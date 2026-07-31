import re

# Update CheckInServiceTest
with open("src/test/java/de/felixhertweck/seatreservation/supervisor/service/CheckInServiceTest.java", "r") as f:
    content = f.read()

# Replace the mocked query logic with the new mocked repository method
new_mock_logic = """    @Test
    void testGetUsernamesWithReservations_AdminAllowed() {
        UUID eventId = id(10);
        User admin = new User();
        admin.id = id(2);
        admin.setRoles(Set.of(Roles.ADMIN));

        when(reservationRepository.findDistinctUsernamesByEventIdAndStatusNotBlocked(eventId))
                .thenReturn(List.of("user1"));

        List<String> usernames = checkInService.getUsernamesWithReservations(auth(admin), eventId);
        assertEquals(1, usernames.size());
    }"""

content = re.sub(
    r"    @Test\s+void testGetUsernamesWithReservations_AdminAllowed\(\) \{.*?(?=    @Test\s+void testGetAllEventsForSupervisor_filtersProperly\(\) \{)",
    new_mock_logic + "\n\n",
    content,
    flags=re.DOTALL
)

with open("src/test/java/de/felixhertweck/seatreservation/supervisor/service/CheckInServiceTest.java", "w") as f:
    f.write(content)

print("Updated CheckInServiceTest")
