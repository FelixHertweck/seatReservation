import re

with open("src/main/java/de/felixhertweck/seatreservation/management/service/ReservationService.java", "r") as f:
    content = f.read()

pattern = r'''        List<Reservation> deletedReservations = new ArrayList<\>\(\);\n\n        for \(Long id : ids\) \{\n\n            Reservation reservation =\n                    reservationRepository\n                            \.findByIdOptional\(id\)\n                            \.orElseThrow\(\n                                    \(\) -> \{\n                                        LOG\.warnf\(\n                                                "Reservation with ID %d not found for deletion by"\n                                                        \+ " user: %s \(ID: %d\)",\n                                                id, managerUser\.id, managerUser\.getId\(\)\);\n                                        return new ReservationNotFoundException\(\n                                                "Reservation with id " \+ id \+ " not found"\);\n                                    \}\);'''

replacement = '''        List<Reservation> deletedReservations = new ArrayList<>();

        List<Reservation> foundReservations = reservationRepository.find("id in ?1", ids).list();
        Map<Long, Reservation> reservationMap = foundReservations.stream().collect(Collectors.toMap(r -> r.id, r -> r));

        for (Long id : ids) {
            Reservation reservation = reservationMap.get(id);
            if (reservation == null) {
                LOG.warnf("Reservation with ID %d not found for deletion by user: %s (ID: %d)", id, managerUser.id, managerUser.getId());
                throw new ReservationNotFoundException("Reservation with id " + id + " not found");
            }'''

new_content = re.sub(pattern, replacement, content, count=1)

if content != new_content:
    with open("src/main/java/de/felixhertweck/seatreservation/management/service/ReservationService.java", "w") as f:
        f.write(new_content)
    print("Patched successfully")
else:
    print("Pattern not found")
