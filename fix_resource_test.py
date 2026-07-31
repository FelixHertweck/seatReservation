import re

with open("src/test/java/de/felixhertweck/seatreservation/supervisor/resource/CheckInResourceTest.java", "r") as f:
    content = f.read()

# Replace the mocked query logic with the new mocked repository method
new_mock_logic = """        PanacheQuery<Reservation> reservedQueryEvent10 =
                (PanacheQuery<Reservation>) mock(PanacheQuery.class);
        when(reservedQueryEvent10.stream()).thenReturn(Stream.of(res1, res2));
        when(reservationRepository.find("event.id", id(10))).thenReturn(reservedQueryEvent10);
        when(reservationRepository.findDistinctUsernamesByEventIdAndStatusNotBlocked(id(10)))
                .thenReturn(List.of("user1", "user2"));

        PanacheQuery<Reservation> reservedQueryEvent20 =
                (PanacheQuery<Reservation>) mock(PanacheQuery.class);
        when(reservedQueryEvent20.stream()).thenReturn(Stream.empty());
        when(reservationRepository.find("event.id", id(20))).thenReturn(reservedQueryEvent20);
        when(reservationRepository.findDistinctUsernamesByEventIdAndStatusNotBlocked(id(20)))
                .thenReturn(List.empty());"""

content = content.replace(
    """        PanacheQuery<Reservation> reservedQueryEvent10 =
                (PanacheQuery<Reservation>) mock(PanacheQuery.class);
        when(reservedQueryEvent10.stream()).thenReturn(Stream.of(res1, res2));
        when(reservationRepository.find("event.id", id(10))).thenReturn(reservedQueryEvent10);

        PanacheQuery<Reservation> reservedQueryEvent20 =
                (PanacheQuery<Reservation>) mock(PanacheQuery.class);
        when(reservedQueryEvent20.stream()).thenReturn(Stream.empty());
        when(reservationRepository.find("event.id", id(20))).thenReturn(reservedQueryEvent20);""",
    new_mock_logic
)

# wait List.empty() doesn't exist, we want List.of() or Collections.emptyList()
content = content.replace("List.empty()", "List.of()")

with open("src/test/java/de/felixhertweck/seatreservation/supervisor/resource/CheckInResourceTest.java", "w") as f:
    f.write(content)

print("Updated CheckInResourceTest")
