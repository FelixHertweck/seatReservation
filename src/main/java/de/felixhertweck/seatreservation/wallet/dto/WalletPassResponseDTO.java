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
package de.felixhertweck.seatreservation.wallet.dto;

import java.util.Arrays;
import java.util.Objects;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record WalletPassResponseDTO(
        WalletProvider provider, String url, byte[] content, String contentType, String filename) {

    public static WalletPassResponseDTO forGoogle(String saveUrl) {
        return new WalletPassResponseDTO(
                WalletProvider.GOOGLE, saveUrl, null, "application/json", null);
    }

    public static WalletPassResponseDTO forApple(byte[] pkpassBytes, String filename) {
        return new WalletPassResponseDTO(
                WalletProvider.APPLE, null, pkpassBytes, "application/vnd.apple.pkpass", filename);
    }

    public static WalletPassResponseDTO forAppleBundle(byte[] pkpassesBytes, String filename) {
        return new WalletPassResponseDTO(
                WalletProvider.APPLE,
                null,
                pkpassesBytes,
                "application/vnd.apple.pkpasses",
                filename);
    }

    public static WalletPassResponseDTO forGenericPkpass(byte[] pkpassBytes, String filename) {
        return new WalletPassResponseDTO(
                WalletProvider.GENERIC_PKPASS,
                null,
                pkpassBytes,
                "application/vnd.apple.pkpass",
                filename);
    }

    public static WalletPassResponseDTO forGenericPkpassBundle(
            byte[] pkpassesBytes, String filename) {
        return new WalletPassResponseDTO(
                WalletProvider.GENERIC_PKPASS,
                null,
                pkpassesBytes,
                "application/vnd.apple.pkpasses",
                filename);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WalletPassResponseDTO that = (WalletPassResponseDTO) o;
        return provider == that.provider
                && Objects.equals(url, that.url)
                && Arrays.equals(content, that.content)
                && Objects.equals(contentType, that.contentType)
                && Objects.equals(filename, that.filename);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(provider, url, contentType, filename);
        result = 31 * result + Arrays.hashCode(content);
        return result;
    }

    @Override
    public String toString() {
        return "WalletPassResponseDTO["
                + "provider="
                + provider
                + ", url="
                + url
                + ", content="
                + (content != null ? "[" + content.length + " bytes]" : "null")
                + ", contentType="
                + contentType
                + ", filename="
                + filename
                + "]";
    }
}
