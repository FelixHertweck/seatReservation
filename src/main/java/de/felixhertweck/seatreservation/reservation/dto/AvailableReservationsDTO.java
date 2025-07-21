package de.felixhertweck.seatreservation.reservation.dto;

public class AvailableReservationsDTO {
    private int availableReservations;

    public AvailableReservationsDTO() {}

    public AvailableReservationsDTO(int availableReservations) {
        this.availableReservations = availableReservations;
    }

    public int getAvailableReservations() {
        return availableReservations;
    }

    public void setAvailableReservations(int availableReservations) {
        this.availableReservations = availableReservations;
    }
}
