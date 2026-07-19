import re

with open("src/main/java/de/felixhertweck/seatreservation/management/service/ReservationService.java", "r") as f:
    content = f.read()

pattern = r'''        for \(Long dtoSeatId : dto\.getSeatIds\(\)\) \{\n            Seat seat =\n                    seatRepository\n                            \.findByIdOptional\(dtoSeatId\)\n                            \.orElseThrow\(\n                                    \(\) -> \{\n                                        LOG\.warnf\(\n                                                "Seat with ID %d not found for reservation"\n                                                        \+ " creation\.",\n                                                dtoSeatId\);\n                                        return new IllegalArgumentException\(\n                                                "Seat with id " \+ dtoSeatId \+ " not found"\);\n                                    \}\);'''

replacement = '''        List<Seat> seats = seatRepository.find("id in ?1", dto.getSeatIds()).list();
        Map<Long, Seat> seatMap = seats.stream().collect(Collectors.toMap(s -> s.id, s -> s));

        for (Long dtoSeatId : dto.getSeatIds()) {
            Seat seat = seatMap.get(dtoSeatId);
            if (seat == null) {
                LOG.warnf("Seat with ID %d not found for reservation creation.", dtoSeatId);
                throw new IllegalArgumentException("Seat with id " + dtoSeatId + " not found");
            }'''

new_content = re.sub(pattern, replacement, content, count=1)

if content != new_content:
    with open("src/main/java/de/felixhertweck/seatreservation/management/service/ReservationService.java", "w") as f:
        f.write(new_content)
    print("Patched create reservations successfully")
else:
    print("Pattern create reservations not found")
