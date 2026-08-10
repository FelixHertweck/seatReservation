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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import de.felixhertweck.seatreservation.common.exception.ReservationNotFoundException;
import de.felixhertweck.seatreservation.model.entity.Event;
import de.felixhertweck.seatreservation.model.entity.EventLocation;
import de.felixhertweck.seatreservation.model.entity.Reservation;
import de.felixhertweck.seatreservation.model.entity.Seat;
import de.felixhertweck.seatreservation.model.entity.User;
import de.felixhertweck.seatreservation.model.repository.ReservationRepository;
import de.felixhertweck.seatreservation.wallet.dto.WalletPassData;
import de.felixhertweck.seatreservation.wallet.dto.WalletPassResponseDTO;
import de.felixhertweck.seatreservation.wallet.dto.WalletProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class WalletPassService {

    private static final Logger LOG = Logger.getLogger(WalletPassService.class);

    private final Map<WalletProvider, AbstractWalletPassGenerator> generators;

    @Inject ReservationRepository reservationRepository;

    @Inject
    public WalletPassService(Instance<AbstractWalletPassGenerator> generatorInstances) {
        this.generators =
                generatorInstances.stream()
                        .collect(
                                Collectors.toMap(
                                        AbstractWalletPassGenerator::getProvider,
                                        Function.identity()));
    }

    @ConfigProperty(name = "wallet.google.enabled", defaultValue = "false")
    boolean googleWalletEnabled;

    @ConfigProperty(name = "wallet.apple.enabled", defaultValue = "false")
    boolean appleWalletEnabled;

    @ConfigProperty(name = "wallet.generic.enabled", defaultValue = "true")
    boolean genericWalletEnabled;

    public boolean isGoogleWalletEnabled() {
        return googleWalletEnabled;
    }

    public boolean isAppleWalletEnabled() {
        return appleWalletEnabled;
    }

    public boolean isGenericWalletEnabled() {
        return genericWalletEnabled;
    }

    public boolean isProviderEnabled(WalletProvider provider) {
        if (provider == WalletProvider.GOOGLE) return googleWalletEnabled;
        if (provider == WalletProvider.APPLE) return appleWalletEnabled;
        if (provider == WalletProvider.GENERIC_PKPASS) return genericWalletEnabled;
        return false;
    }

    @Transactional
    public WalletPassResponseDTO generatePass(
            UUID reservationId, User currentUser, WalletProvider provider)
            throws ReservationNotFoundException, SecurityException {
        if (!isProviderEnabled(provider)) {
            throw new IllegalStateException("Wallet provider " + provider + " is disabled");
        }

        LOG.debugf(
                "Generating wallet pass for provider %s, reservation ID: %s, user: %s",
                provider, reservationId, currentUser.id);

        Reservation reservation =
                reservationRepository
                        .findByIdOptional(reservationId)
                        .orElseThrow(
                                () ->
                                        new ReservationNotFoundException(
                                                "Reservation not found with ID: " + reservationId));

        if (!reservation.getUser().equals(currentUser)) {
            LOG.warnf(
                    "User ID: %s attempted to access wallet pass for reservation %s belonging to"
                            + " user ID: %s",
                    currentUser.id, reservationId, reservation.getUser().id);
            throw new SecurityException("You are not allowed to access this reservation");
        }

        AbstractWalletPassGenerator generator = generators.get(provider);
        if (generator == null) {
            throw new IllegalArgumentException("Unsupported wallet provider: " + provider);
        }

        // Load ALL active reservations for this user+event so the generator can bundle
        // all seats into a single pass entry. The ownership check above on the requested
        // reservationId already ensures the user can access this event.
        Event event = reservation.getEvent();
        List<WalletPassData> allSeatPassData =
                reservationRepository.findByUserAndEvent(currentUser, event).stream()
                        .map(this::mapToPassData)
                        .toList();

        return generator.generatePass(allSeatPassData);
    }

    private WalletPassData mapToPassData(Reservation reservation) {
        Event event = reservation.getEvent();
        EventLocation location = event != null ? event.getEventLocation() : null;
        Seat seat = reservation.getSeat();
        User user = reservation.getUser();

        String seatLabel = buildSeatLabel(seat);
        String checkInToken =
                reservation.getCheckInToken() != null
                        ? reservation.getCheckInToken().getToken()
                        : "";
        String qrCodePayload =
                String.format("%s;%s;%s", user.id, event != null ? event.id : "", checkInToken);
        String ticketHolderName = buildTicketHolderName(user);

        return new WalletPassData(
                reservation.id,
                event != null ? event.id : null,
                event != null ? event.getName() : "Event",
                event != null ? event.getDescription() : "",
                event != null ? event.getStartTime() : null,
                event != null ? event.getEndTime() : null,
                location != null ? location.getName() : "",
                location != null ? location.getAddress() : "",
                seatLabel,
                seat != null && seat.getArea() != null ? seat.getArea().getName() : null,
                seat != null ? seat.getSeatRow() : null,
                seat != null ? seat.getSeatNumber() : null,
                user.id,
                ticketHolderName,
                user.getEmail(),
                checkInToken,
                qrCodePayload);
    }

    private String buildSeatLabel(Seat seat) {
        if (seat == null) return "";
        return String.format("Reihe %s, Platz %s", seat.getSeatRow(), seat.getSeatNumber());
    }

    private String buildTicketHolderName(User user) {
        String firstName = user.getFirstname();
        String lastName = user.getLastname();
        boolean hasRealName =
                (firstName != null && !firstName.isBlank())
                        || (lastName != null && !lastName.isBlank());
        if (!hasRealName) {
            return user.getUsername();
        }
        String fullName =
                java.util.stream.Stream.of(firstName, lastName)
                        .filter(s -> s != null && !s.isBlank())
                        .collect(Collectors.joining(" "));
        return String.format("%s (%s)", fullName, user.getUsername());
    }
}
