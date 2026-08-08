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
package de.felixhertweck.seatreservation.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Maps a walk-in box office reservation (booked under the shared, passwordless {@code boxoffice}
 * system user, see {@code BoxOfficeService}) to the guest's name, so supervisor-facing views can
 * display who a "boxoffice" reservation belongs to instead of just the shared account.
 *
 * <p>One row per {@link Reservation}: a guest booking multiple seats in one submission creates one
 * row per seat, each carrying the same name.
 */
@Entity
@Table(name = "boxoffice_guest_info")
public class BoxOfficeGuestInfo extends AbstractEntity {

    @OneToOne
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Reservation reservation;

    private String guestName;

    public BoxOfficeGuestInfo() {}

    public BoxOfficeGuestInfo(Reservation reservation, String guestName) {
        this.reservation = reservation;
        this.guestName = guestName;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }
}
