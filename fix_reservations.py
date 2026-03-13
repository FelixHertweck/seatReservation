import re

filepath = 'src/main/java/de/felixhertweck/seatreservation/reservation/service/ReservationService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    'LOG.debugf(\n                "Persisted %d new reservations for user ID: %d and event ID: %d.",\n                newReservations.size(), currentUser.id, event.id);',
    'LOG.infof(\n                "Persisted %d new reservations for user ID: %d and event ID: %d.",\n                newReservations.size(), currentUser.id, event.id);\n        LOG.debugf(\n                "Persisted %d new reservations for user ID: %d and event ID: %d.",\n                newReservations.size(), currentUser.id, event.id);'
)

# For deletion
content = content.replace(
    'reservationRepository.delete(reservation);\n                                LOG.debugf(\n                                        "Deleted reservation with ID %d for user ID: %d.",\n                                        reservation.id, currentUser.id);',
    'reservationRepository.delete(reservation);\n                                LOG.infof("Deleted reservation with ID %d for user ID: %d.", reservation.id, currentUser.id);\n                                LOG.debugf(\n                                        "Deleted reservation with ID %d for user ID: %d.",\n                                        reservation.id, currentUser.id);'
)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
