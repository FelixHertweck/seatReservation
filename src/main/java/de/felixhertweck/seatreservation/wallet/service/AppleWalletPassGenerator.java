/*
 * #%L
 * seat-reservation
 * %%
 * Copyright (C) 2025 Felix Hertweck
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package de.felixhertweck.seatreservation.wallet.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import jakarta.enterprise.context.ApplicationScoped;

import de.felixhertweck.seatreservation.wallet.dto.WalletPassData;
import de.felixhertweck.seatreservation.wallet.dto.WalletPassResponseDTO;
import de.felixhertweck.seatreservation.wallet.dto.WalletProvider;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/** Generates an Apple Wallet {@code .pkpass} archive with optional PKCS#7 digital signature. */
/** This is not tested as a payed Apple Developer Account is required. */
@ApplicationScoped
public class AppleWalletPassGenerator extends AbstractPkpassGenerator {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static final Logger LOG = Logger.getLogger(AppleWalletPassGenerator.class);

    @ConfigProperty(
            name = "wallet.apple.pass-type-identifier",
            defaultValue = "pass.de.felixhertweck.seatreservation")
    String passTypeIdentifier;

    @ConfigProperty(name = "wallet.apple.team-id", defaultValue = "ABC1234567")
    String teamId;

    /** Path to the PKCS#12 (.p12) file exported from Keychain Access with the private key. */
    @ConfigProperty(name = "wallet.apple.certificate-path", defaultValue = "keys/pass.p12")
    String certificatePath;

    /** Password for the .p12 file. May be empty for password-less keystores. */
    @ConfigProperty(name = "wallet.apple.certificate-password")
    java.util.Optional<String> certificatePassword;

    /**
     * Path to the Apple WWDR Intermediate Certificate (.pem or .cer), available from
     * developer.apple.com/certificationauthority.
     */
    @ConfigProperty(name = "wallet.apple.wwdr-certificate-path", defaultValue = "keys/wwdr.pem")
    String wwdrCertificatePath;

    @Override
    public WalletProvider getProvider() {
        return WalletProvider.APPLE;
    }

    @Override
    public WalletPassResponseDTO generatePass(WalletPassData data) {
        return generatePass(java.util.List.of(data));
    }

    @Override
    public WalletPassResponseDTO generatePass(java.util.List<WalletPassData> allSeatReservations) {
        if (allSeatReservations == null || allSeatReservations.isEmpty()) {
            throw new IllegalArgumentException("No reservations provided");
        }
        WalletPassData first = allSeatReservations.get(0);

        if (allSeatReservations.size() == 1) {
            LOG.debugf(
                    "Generating Apple Wallet Pass (.pkpass) for single reservation ID: %s",
                    first.reservationId());
            try {
                byte[] passJsonBytes = buildPassJson(first, passTypeIdentifier, teamId);
                byte[] manifestBytes = buildManifest(Map.of("pass.json", passJsonBytes));
                byte[] signatureBytes = signIfPossible(manifestBytes);
                byte[] pkpass = buildZip(passJsonBytes, manifestBytes, signatureBytes);
                String filename = String.format("ticket_%s.pkpass", first.reservationId());
                return WalletPassResponseDTO.forApple(pkpass, filename);
            } catch (Exception e) {
                LOG.errorf(
                        e,
                        "Error generating Apple Wallet PKPass for reservation ID %s",
                        first.reservationId());
                throw new RuntimeException("Failed to generate Apple Wallet Pass", e);
            }
        } else {
            LOG.debugf(
                    "Generating Apple Wallet PKPasses Bundle (.pkpasses) for %d seat(s), event ID:"
                            + " %s",
                    allSeatReservations.size(), first.eventId());
            try {
                Map<String, byte[]> bundleEntries = new HashMap<>();
                for (int i = 0; i < allSeatReservations.size(); i++) {
                    WalletPassData seatData = allSeatReservations.get(i);
                    byte[] passJsonBytes = buildPassJson(seatData, passTypeIdentifier, teamId);
                    byte[] manifestBytes = buildManifest(Map.of("pass.json", passJsonBytes));
                    byte[] signatureBytes = signIfPossible(manifestBytes);
                    byte[] singlePkpass = buildZip(passJsonBytes, manifestBytes, signatureBytes);
                    bundleEntries.put(
                            String.format("pass_%d_%s.pkpass", i + 1, seatData.reservationId()),
                            singlePkpass);
                }
                byte[] bundleBytes = buildPkpassesBundle(bundleEntries);
                String filename = String.format("tickets_%s.pkpasses", first.eventId());
                return WalletPassResponseDTO.forAppleBundle(bundleBytes, filename);
            } catch (Exception e) {
                LOG.errorf(
                        e,
                        "Error generating Apple Wallet PKPasses Bundle for event ID %s",
                        first.eventId());
                throw new RuntimeException("Failed to generate Apple Wallet Pass Bundle", e);
            }
        }
    }

    private byte[] signIfPossible(byte[] manifestBytes) {
        if (certificatePath == null || !Files.exists(Path.of(certificatePath))) {
            LOG.info(
                    "Apple Pass signing certificate file not found. Generating unsigned PKPASS"
                            + " archive.");
            return null;
        }
        try {
            return sign(manifestBytes);
        } catch (Exception e) {
            LOG.warnf(
                    "Could not sign Apple Wallet pass (certificate missing or invalid: %s)."
                            + " Generating unsigned PKPASS archive.",
                    e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // signature – detached PKCS#7/CMS, SHA256withRSA, no SMIME capabilities
    // -------------------------------------------------------------------------

    private byte[] sign(byte[] manifestBytes)
            throws java.security.GeneralSecurityException,
                    IOException,
                    org.bouncycastle.cms.CMSException,
                    org.bouncycastle.operator.OperatorCreationException {
        // Load the Pass Signing Certificate + private key from the .p12
        KeyStore p12 = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);
        try (InputStream is = new FileInputStream(certificatePath)) {
            p12.load(is, certificatePassword.orElse("").toCharArray());
        }

        PrivateKey signingKey = null;
        X509Certificate signingCert = null;
        Enumeration<String> aliases = p12.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (p12.isKeyEntry(alias)) {
                signingKey =
                        (PrivateKey)
                                p12.getKey(alias, certificatePassword.orElse("").toCharArray());
                signingCert = (X509Certificate) p12.getCertificate(alias);
                break;
            }
        }
        if (signingKey == null || signingCert == null) {
            throw new IllegalStateException(
                    "No private key entry found in Apple Pass certificate file: "
                            + certificatePath);
        }

        // Load WWDR intermediate certificate
        X509Certificate wwdrCert = loadWwdrCertificate();

        // Build the CMS signer
        ContentSigner contentSigner =
                new JcaContentSignerBuilder("SHA256withRSA")
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .build(signingKey);

        DigestCalculatorProvider digestCalcProvider =
                new JcaDigestCalculatorProviderBuilder()
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .build();

        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        gen.addSignerInfoGenerator(
                new JcaSignerInfoGeneratorBuilder(digestCalcProvider)
                        .build(contentSigner, signingCert));

        // Include the full chain (signing cert + WWDR) so Apple can verify without network
        Collection<Certificate> certChain = new ArrayList<>();
        certChain.add(signingCert);
        certChain.add(wwdrCert);
        gen.addCertificates(new JcaCertStore(certChain));

        // Detached signature (manifest bytes not embedded in the signature blob)
        CMSSignedData signedData = gen.generate(new CMSProcessableByteArray(manifestBytes), false);
        return signedData.getEncoded();
    }

    private X509Certificate loadWwdrCertificate()
            throws IOException, java.security.cert.CertificateException {
        byte[] certBytes = Files.readAllBytes(Path.of(wwdrCertificatePath));
        try {
            // Try PEM first
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            try (InputStream is = new java.io.ByteArrayInputStream(certBytes)) {
                return (X509Certificate) cf.generateCertificate(is);
            }
        } catch (Exception e) {
            throw new UncheckedIOException(
                    new IOException(
                            "Failed to load WWDR certificate from " + wwdrCertificatePath, e));
        }
    }
}
