import re

with open("src/main/java/de/felixhertweck/seatreservation/reservation/service/ReservationService.java", "r") as f:
    content = f.read()

pattern = r'''        // Validate the seatIds, ensure they exist\n        List<Seat> seats =\n                dto\.getSeatIds\(\)\.stream\(\)\n                        \.map\(\n                                seatId ->\n                                        seatRepository\n                                                \.findByIdOptional\(seatId\)\n                                                \.orElseThrow\(\n                                                        \(\) -> \{\n                                                            LOG\.warnf\(\n                                                                    "Seat with ID %d not found for"\n                                                                        \+ " reservation creation by"\n                                                                        \+ " user %s\.",\n                                                                    seatId, currentUser\.id\);\n                                                            return new EventNotFoundException\(\n                                                                    "Minimum one seat not"\n                                                                            \+ " found"\);\n                                                        \}\)\)\n                        \.toList\(\);'''

replacement = '''        // Validate the seatIds, ensure they exist
        List<Seat> seats = seatRepository.find("id in ?1", dto.getSeatIds()).list();
        if (seats.size() != dto.getSeatIds().size()) {
            LOG.warnf("Some seats not found for reservation creation by user %s.", currentUser.id);
            throw new EventNotFoundException("Minimum one seat not found");
        }'''

new_content = re.sub(pattern, replacement, content, count=1)

if content != new_content:
    with open("src/main/java/de/felixhertweck/seatreservation/reservation/service/ReservationService.java", "w") as f:
        f.write(new_content)
    print("Patched successfully")
else:
    print("Pattern not found")
