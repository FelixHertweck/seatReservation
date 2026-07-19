import re

with open("src/main/java/de/felixhertweck/seatreservation/reservation/service/ReservationService.java", "r") as f:
    content = f.read()

pattern = r'''        // Group reservations by event to handle allowance updates and email confirmations correctly\n        Map<Long, List<Reservation>> reservationMap =\n                reservations\.stream\(\)\.collect\(Collectors\.groupingBy\(r -> r\.getEvent\(\)\.id\)\);'''

replacement = '''        // Group reservations by event to handle allowance updates and email confirmations correctly
        Map<Long, List<Reservation>> groupedReservationMap =
                reservations.stream().collect(Collectors.groupingBy(r -> r.getEvent().id));'''

new_content = re.sub(pattern, replacement, content, count=1)

pattern2 = r'''        for \(Map\.Entry<Long, List<Reservation>> entry : reservationMap\.entrySet\(\)\) \{'''

replacement2 = '''        for (Map.Entry<Long, List<Reservation>> entry : groupedReservationMap.entrySet()) {'''

new_content = re.sub(pattern2, replacement2, new_content, count=1)

if content != new_content:
    with open("src/main/java/de/felixhertweck/seatreservation/reservation/service/ReservationService.java", "w") as f:
        f.write(new_content)
    print("Patched groupedReservationMap successfully")
else:
    print("Pattern groupedReservationMap not found")
