import re

with open("src/main/java/de/felixhertweck/seatreservation/management/service/ReservationService.java", "r") as f:
    content = f.read()

pattern = r'''        List<Seat> seats =\n                request\.getSeatIds\(\)\.stream\(\)\n                        \.map\(\n                                seatId ->\n                                        seatRepository\n                                                \.findByIdOptional\(seatId\)\n                                                \.orElseThrow\(\n                                                        \(\) -> \{\n                                                            LOG\.warnf\(\n                                                                    "Seat with ID %d not found for"\n                                                                        \+ " reservation creation in"\n                                                                        \+ " event ID %d\.",\n                                                                    seatId, eventId\);\n                                                            return new IllegalArgumentException\(\n                                                                    "Seat with id "\n                                                                            \+ seatId\n                                                                            \+ " not found"\);\n                                                        \}\)\)\n                        \.toList\(\);'''

replacement = '''        List<Seat> seats = seatRepository.find("id in ?1", request.getSeatIds()).list();
        if (seats.size() != request.getSeatIds().size()) {
            LOG.warnf("Some seats not found for reservation creation in event ID %d.", eventId);
            throw new IllegalArgumentException("Seat not found");
        }'''

new_content = re.sub(pattern, replacement, content, count=1)

if content != new_content:
    with open("src/main/java/de/felixhertweck/seatreservation/management/service/ReservationService.java", "w") as f:
        f.write(new_content)
    print("Patched create reservation successfully")
else:
    print("Pattern create reservation not found")
