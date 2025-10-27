# Classes Needing Additional Unit Tests

Based on the current JaCoCo coverage report, the following classes have low or no test coverage and would benefit from additional unit tests. The focus should be on increasing coverage for critical logic, edge cases, and error handling. Below is a prioritized list with suggestions for meaningful tests:

## 2. **NotificationService** (`de.felixhertweck.seatreservation.email`)
- **Current coverage:** 0% (202 instructions missed)
- **Suggestions:**
  - Test sending notifications for different scenarios (success, failure)
  - Test exception handling and logging

## 3. **ReservationExporter** (`de.felixhertweck.seatreservation.utils`)
- **Current coverage:** Low (370 missed, 218 covered)
- **Suggestions:**
  - Test export logic for various reservation data
  - Test file output and error handling

## 4. **SvgRenderer** (`de.felixhertweck.seatreservation.utils`)
- **Current coverage:** Low (142 missed, 213 covered)
- **Suggestions:**
  - Test SVG generation for different seat layouts
  - Test handling of invalid input data

## 5. **AdminUserInitializer** (`de.felixhertweck.seatreservation`)
- **Current coverage:** Low (72 missed, 20 covered)
- **Suggestions:**
  - Test initialization logic with/without existing admin users
  - Test error scenarios

## 6. **EmailService** (`de.felixhertweck.seatreservation.email`)
- **Current coverage:** Partial (395 missed, 1082 covered)
- **Suggestions:**
  - Test email sending for all supported templates
  - Test error handling (SMTP failures, invalid addresses)

## 7. **UserService** (`de.felixhertweck.seatreservation.userManagment.service`)
- **Current coverage:** Partial (130 missed, 873 covered)
- **Suggestions:**
  - Test user creation, update, and deletion
  - Test edge cases (duplicate users, invalid data)

## 8. **EventReservationAllowanceService, EventLocationService, EventService, SeatService, ReservationService** (`de.felixhertweck.seatreservation.management.service`)
- **Current coverage:** Partial, but many instructions missed
- **Suggestions:**
  - Test main business logic and edge cases
  - Test error handling and validation

## 9. **Entities (e.g., EventLocation, Reservation, User, EventUserAllowance, etc.)**
- **Current coverage:** Many missed instructions
- **Suggestions:**
  - Test entity methods (e.g., equals, hashCode, toString)
  - Test validation logic if present

## 10. **Other Utility Classes (e.g., RandomUUIDString, SeatStatusDTO, ErrorResponseDto)**
- **Current coverage:** 0% or very low
- **Suggestions:**
  - Test all public methods
  - Test edge cases and invalid input

---

**General Recommendations:**
- Focus on classes with 0% or very low coverage first.
- Add tests for error handling, edge cases, and business-critical logic.
- Consider using parameterized tests for DTOs and utility classes.
- Review coverage after adding tests to ensure improvements.
