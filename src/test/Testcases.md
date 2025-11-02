# Test Cases

This is an overview of the test cases for the application.

## Email Service

### EmailService

| Test Case | Description |
| :--- | :--- |
| `sendEmailConfirmation_Success` | Successfully sends an email confirmation to a user. |
| `sendEmailConfirmation_IOException` | Simulates an error while sending the email confirmation (e.g., via an `IOException`). Expects the exception to be handled correctly. |
| `sendEventReminder_Success` | Successfully sends an event reminder email to a user. |
| `sendEventReminder_IOException` | Simulates an error while sending the event reminder email (e.g., via an `IOException`). Expects the exception to be handled correctly. |
| `sendEventReservationsCsvToManager_Success` | Successfully sends an email with a CSV export of reservations to the manager. |
| `sendEventReservationsCsvToManager_IOException` | Simulates an error while sending the email with the CSV export to the manager (e.g., via an `IOException`). Expects the exception to be handled correctly. |

### EmailVerificationCleanupService

| Test Case | Description |
| :--- | :--- |
| `performManualCleanup_Success` | Successfully performs manual cleanup and deletes expired email verification records. |
| `performManualCleanup_NoExpiredTokens` | Checks that no records are deleted when no expired tokens are present. |
| `scheduledCleanup_Success` | Checks if the scheduled cleanup process runs successfully. |

### NotificationService

| Test Case | Description |
| :--- | :--- |
| `sendEventReminders_WithEventsAndReservations_SendsEmails` | Checks that event reminders are successfully sent to users with reservations for tomorrow's events. |
| `sendEventReminders_WithNoEvents_DoesNotSendEmails` | Checks that no emails are sent when there are no events for tomorrow. |
| `sendEventReminders_WithEventsButNoReservations_DoesNotSendEmails` | Checks that no emails are sent when events exist but there are no reservations. |
| `sendEventReminders_WithMultipleEvents_ProcessesAllEvents` | Checks that all events for tomorrow are processed and corresponding reminders are sent. |
| `sendEventReminders_WithMultipleReservationsPerUser_GroupsCorrectly` | Checks that multiple reservations by the same user for one event are grouped correctly. |
| `sendEventReminders_WithEmailException_ContinuesProcessing` | Checks that processing continues for other users in case of email errors. |
| `sendEventReminders_WithNullUserEmail_SkipsUserGracefully` | Checks that users with null email addresses are skipped gracefully. |
| `sendEventReminders_CalculatesCorrectDateRange` | Checks that the correct date range (tomorrow) is used for the event search. |
| `sendEventReminders_WithServiceException_HandlesGracefully` | Checks the behavior in case of service exceptions (e.g., database error). |
| `sendDailyReservationCsvToManagers_WithEventsAndManagers_SendsCsvEmails` | Checks that CSV export emails are successfully sent to managers of events taking place today. |
| `sendDailyReservationCsvToManagers_WithNoEvents_DoesNotSendEmails` | Checks that no CSV export emails are sent when there are no events for today. |
| `sendDailyReservationCsvToManagers_WithEventButNoManager_DoesNotSendEmail` | Checks that no email is sent if an event has no assigned manager. |
| `sendDailyReservationCsvToManagers_WithMultipleEvents_ProcessesAllEvents` | Checks that CSV exports are sent for all of today's events with different managers. |
| `sendDailyReservationCsvToManagers_WithEmailException_ContinuesProcessing` | Checks that processing continues for other managers in case of email errors. |
| `sendDailyReservationCsvToManagers_CalculatesCorrectDateRange` | Checks that the correct date range (today) is used for the event search. |
| `sendDailyReservationCsvToManagers_WithServiceException_HandlesGracefully` | Checks the behavior in case of service exceptions during CSV export processing. |

## Security

### AuthService

| Test Case | Description |
| :--- | :--- |
| `authenticate_Success` | Successfully authenticates a user with valid credentials and returns a token. |
| `authenticate_Success_WithEmail` | Successfully authenticates a user with valid credentials (email) and returns a token. |
| `authenticate_AuthenticationFailedException_InvalidUsername` | Attempts to authenticate with a non-existent username. Expects `AuthenticationFailedException`. |
| `authenticate_AuthenticationFailedException_InvalidPassword` | Attempts to authenticate with a valid username but an incorrect password. Expects `AuthenticationFailedException`. |
| `testAuthenticateFailureEmailNotFound` | Attempts to authenticate with a non-existent email address. Expects `AuthenticationFailedException`. |
| `testAuthenticateWithEmailWrongPassword` | Attempts to authenticate with a valid email address but an incorrect password. Expects `AuthenticationFailedException`. |
| `testAuthenticateWithEmailIdentifier` | Checks that email addresses are correctly identified as emails and that email-based search is used. |
| `testAuthenticateWithEmptyPassword` | Attempts to authenticate with an empty password. Expects `AuthenticationFailedException`. |
| `testAuthenticateIdentifierDetection` | Tests the correct detection of email vs. username as an identifier. |
| `testAuthenticateUsernameIdentification` | Checks that usernames without an @ symbol are correctly identified as usernames. |
| `testAuthenticateWithInvalidHash` | Tests the behavior with an invalid password hash format. Expects `RuntimeException`. |
| `testAuthenticateSpecialCharactersInPassword` | Tests authentication with special characters in the password. |
| `testIsRegistrationEnabled_DefaultTrue` | Checks that registration is enabled by default. |
| `testIsRegistrationEnabled_WhenDisabled` | Checks that the `isRegistrationEnabled()` method correctly returns `false` when registration is disabled. |
| `testRegisterThrowsExceptionWhenRegistrationDisabled` | Attempts to register a new user when registration is disabled. Expects `RegistrationDisabledException`. |

### TokenService

| Test Case | Description |
| :--- | :--- |
| `generateToken_Success` | Successfully generates a JWT token for a given user. |
| `generateToken_ValidTokenContent` | Checks if the generated token contains the correct user information (e.g., username, roles). |
| `generateToken_TokenExpiration` | Checks if the generated token has a correct expiration time. |
| `generateToken_NullEmail_UsesEmptyString` | Checks that an empty string is used in the token for a null email. |
| `generateToken_EmptyRoles_HandlesCorrectly` | Tests the behavior for a user with an empty roles list. |
| `getExpirationMinutes_ReturnsConfiguredValue` | Checks that the configured expiration time is returned correctly. |
| `createNewJwtCookie_ValidCookie` | Successfully creates a JWT cookie with correct properties (HttpOnly, Secure, Path, MaxAge). |
| `createNewJwtCookie_DifferentExpirationTime` | Checks that different expiration times are correctly reflected in the cookie's MaxAge. |
| `createNewJwtCookie_EmptyToken` | Tests the behavior when creating a cookie with an empty token. |
| `createNewJwtCookie_NullToken` | Tests the behavior when creating a cookie with a null token. |
| `generateToken_CustomExpirationTime` | Checks that custom expiration times (e.g., 24 hours) are applied correctly. |
| `generateRefreshToken_Success` | Successfully generates and stores a refresh token for a user. |
| `validateRefreshToken_Success` | Successfully validates a valid refresh token. |
| `validateRefreshToken_ExpiredToken` | Attempts to validate an expired refresh token. Expects `JwtInvalidException`. |
| `validateRefreshToken_InvalidJwt` | Attempts to validate a malformed or invalid JWT. Expects `JwtInvalidException`. |
| `validateRefreshToken_TokenNotFoundInDatabase` | Attempts to validate a token that is not in the database. Expects `JwtInvalidException`. |
| `validateRefreshToken_InvalidTokenValue` | Attempts to validate a token with an incorrect value. Expects `JwtInvalidException`. |
| `createNewRefreshTokenCookie` | Successfully creates a refresh token cookie with correct properties. |
| `createStatusCookie` | Successfully creates a status cookie for refresh token expiration. |
| `logoutAllDevices` | Successfully deletes all refresh tokens for a user. |
| `logoutAllDevices_DoesNotAffectOtherUsers` | Ensures that logging out all devices for one user does not affect other users' tokens. |

### AuthResource

| Test Case | Description |
| :--- | :--- |
| `login_Success` | Sends a POST request to `/api/auth/login` with valid credentials. Expects a 200 OK status and JWT, refresh token, and expiration cookies. |
| `login_AuthenticationFailedException_InvalidCredentials` | Sends a POST request to `/api/auth/login` with invalid credentials. Expects a 401 Unauthorized status. |
| `login_BadRequest_MissingCredentials` | Sends a POST request to `/api/auth/login` without a username or password. Expects a 400 Bad Request status. |
| `register_Success` | Sends a POST request to `/api/auth/register` with valid registration data. Expects a 200 OK status and JWT, refresh token, and expiration cookies. |
| `register_Failure_DuplicateUsername` | Sends a POST request to `/api/auth/register` with an already existing username. Expects a 409 Conflict status. |
| `register_Failure_InvalidData` | Sends a POST request to `/api/auth/register` with invalid data (e.g., missing username, too short password). Expects a 400 Bad Request status. |
| `refreshToken_Success` | Successfully refreshes the JWT using a valid refresh token. |
| `refreshToken_InvalidToken` | Attempts to refresh the JWT with an invalid refresh token. Expects 401 Unauthorized. |
| `refreshToken_MissingToken` | Attempts to refresh the JWT without a refresh token. Expects 401 Unauthorized. |
| `refreshToken_EmptyToken` | Attempts to refresh the JWT with an empty refresh token. Expects 401 Unauthorized. |
| `refreshToken_ServiceThrowsException` | Simulates an internal server error during token refresh. Expects 500 Internal Server Error. |
| `logout_Success` | Successfully logs out the current device by clearing cookies. |
| `logout_NoRefreshTokenCookie` | Ensures the logout endpoint works correctly even if no refresh token cookie is present. |
| `logoutAllDevices_Success` | Successfully logs out from all devices by invalidating all refresh tokens. |
| `logoutAllDevices_WithoutAuth_Unauthorized` | Attempts to log out from all devices without authentication. Expects 401 Unauthorized. |
| `logoutAllDevices_WithInvalidToken_Unauthorized` | Attempts to log out from all devices with an invalid JWT. Expects 401 Unauthorized. |
| `testGetRegistrationStatus_RegistrationEnabled` | Sends a GET request to `/api/auth/registration-status` when registration is enabled. Expects a 200 OK status with `enabled: true`. |
| `testGetRegistrationStatus_RegistrationDisabled` | Sends a GET request to `/api/auth/registration-status` when registration is disabled. Expects a 200 OK status with `enabled: false`. |
| `testGetRegistrationStatus_IsPublicEndpoint` | Verifies that the `/api/auth/registration-status` endpoint is public and does not require authentication. |
| `testRegisterWhenDisabled_Returns403Forbidden` | Attempts to register a new user when registration is disabled. Expects a 403 Forbidden status. |
| `testGetRegistrationStatus_RegistrationEnabledByDefault` | Verifies that registration status is `true` (enabled) by default in test configuration. |

## UserService

### createUser(UserCreationDTO userCreationDTO)

| Test Case | Description |
| :--- | :--- |
| `createUser_Success_WithEmail` | Creates a new user with valid data (username, password, email, first name, last name) and `sendEmailVerification` set to `true`. Checks if the user is successfully saved in the database and an email confirmation is sent. |
| `createUser_Success_WithoutEmail` | Creates a new user with valid data (username, password, first name, last name), but without an email. Checks if the user is successfully saved in the database and no email confirmation is sent. |
| `createUser_Success_WithEmail_NoVerificationSent` | Creates a new user with valid data (username, password, email, first name, last name) and `sendEmailVerification` set to `false`. Checks if the user is successfully saved in the database and no email confirmation is sent. |
| `createUser_InvalidUserException_NullDTO` | Attempts to create a user with a `null` `UserCreationDTO`. Expects `InvalidUserException`. |
| `createUser_InvalidUserException_EmptyUsername` | Attempts to create a user with an empty or whitespace-only username. Expects `InvalidUserException`. |
| `createUser_InvalidUserException_EmptyPassword` | Attempts to create a user with an empty or whitespace-only password. Expects `InvalidUserException`. |
| `createUser_DuplicateUserException_ExistingUsername` | Attempts to create a user with a username that already exists in the database. Expects `DuplicateUserException`. |
| `createUser_Success_WithDuplicateEmail` | Creates a new user with an email address that is already used by another user. Checks if the user is created successfully and no `DuplicateUserException` is thrown. |
| `createUser_InternalServerErrorException_EmailSendFailure` | Simulates an error while sending the email confirmation (e.g., via an `IOException` in `EmailService`). Expects `InternalServerErrorException`. |

### importUsers(Set<AdminUserCreationDto> adminUserCreationDtos)

| Test Case | Description |
| :--- | :--- |
| `importUsers_Success` | Successfully imports multiple users. |
| `importUsers_EmptySet` | Attempts to import an empty set of users. Expects an empty list of imported users. |
| `importUsers_InvalidUserException` | Attempts to import users with invalid data (e.g., empty username). Expects `InvalidUserException`. |
| `importUsers_DuplicateUserException` | Attempts to import users, one of whom already exists. Expects `DuplicateUserException`. |
| `importUsers_EmailSendFailure` | Simulates an email sending failure during import. Checks that the import still proceeds and does not throw a `RuntimeException`. |

### updateUser(Long id, AdminUserUpdateDTO user)

| Test Case | Description |
| :--- | :--- |
| `updateUser_Success_UpdateFirstname` | Successfully updates the first name of an existing user (admin function). |
| `updateUser_Success_UpdateLastname` | Successfully updates the last name of an existing user (admin function). |
| `updateUser_Success_UpdatePassword` | Successfully updates the password of an existing user (admin function). |
| `updateUser_Success_UpdateRoles` | Successfully updates the roles of an existing user (admin function). |
| `updateUser_Success_NoEmailChange` | Successfully updates other fields of an existing user (e.g., first name, last name, password, roles) without changing the email address (admin function). Checks that no email confirmation is sent. |
| `updateUser_Success_UpdateEmail` | Successfully updates the email address of an existing user and checks if the email verification is reset and a new confirmation email is sent (admin function). |
| `updateUser_Success_UpdateEmail_NoVerificationSent` | Successfully updates the email address of an existing user without sending a verification email (admin function). |
| `updateUser_Success_NoEmailChange_VerificationSentTrue` | Successfully updates other fields of an existing user without changing the email address, even if `sendEmailVerification` is true (admin function). Checks that no email confirmation is sent. |
| `updateUser_UserNotFoundException` | Attempts to update a non-existent user (admin function). Expects `UserNotFoundException`. |
| `updateUser_InvalidUserException_NullDTO` | Attempts to update a user with a `null` `AdminUserUpdateDTO` (admin function). Expects `InvalidUserException`. |
| `updateUser_Success_WithDuplicateEmail` | Updates a user's email address to an already existing email address (admin function). Checks if the update is successful and no `DuplicateUserException` is thrown. |
| `updateUser_InternalServerErrorException_EmailSendFailure` | Simulates an error while sending the email confirmation after an email change (admin function). Expects `InternalServerErrorException`. |

### deleteUser(Long id)

| Test Case | Description |
| :--- | :--- |
| `deleteUser_Success` | Successfully deletes an existing user by their ID. |
| `deleteUser_UserNotFoundException` | Attempts to delete a non-existent user. Expects `UserNotFoundException`. |

### getUserById(Long id)

| Test Case | Description |
| :--- | :--- |
| `getUserById_Success` | Successfully retrieves an existing user by their ID. |
| `getUserById_UserNotFoundException` | Attempts to retrieve a non-existent user. Expects `UserNotFoundException`. |

### getAllUsers()

| Test Case | Description |
| :--- | :--- |
| `getAllUsers_Success_WithUsers` | Successfully retrieves a list of all users when users are present. |
| `getAllUsers_Success_NoUsers` | Successfully retrieves an empty list when no users are present. |

### getAvailableRoles()

| Test Case | Description |
| :--- | :--- |
| `getAvailableRoles_Success` | Successfully retrieves a list of all available roles. |

### updateUserProfile(String username, UserProfileUpdateDTO userProfileUpdateDTO)

| Test Case | Description |
| :--- | :--- |
| `updateUserProfile_Success_UpdateFirstname` | Successfully updates the first name of an existing user via their username. |
| `updateUserProfile_Success_UpdateLastname` | Successfully updates the last name of an existing user via their username. |
| `updateUserProfile_Success_UpdatePassword` | Successfully updates the password of an existing user via their username. |
| `updateUserProfile_Success_PasswordSaltChangesOnPasswordUpdate` | Checks if the salt changes when updating the password of an existing user via their username. |
| `updateUserProfile_Success_UpdateEmail` | Successfully updates the email address of an existing user via their username and checks if the email verification is reset and a new confirmation email is sent. |
| `updateUserProfile_DoesNotUpdateRoles` | Ensures that an attempt to update one's own roles via this endpoint is ignored. |
| `updateUserProfile_UserNotFoundException` | Attempts to update the profile of a non-existent user. Expects `UserNotFoundException`. |
| `updateUserProfile_InvalidUserException_NullDTO` | Attempts to update a user profile with a `null` `UserProfileUpdateDTO`. Expects `InvalidUserException`. |
| `updateUserProfile_Success_WithDuplicateEmail` | Updates a user profile's email address to an already existing email address. Checks if the update is successful and no `DuplicateUserException` is thrown. |
| `updateUserProfile_InternalServerErrorException_EmailSendFailure` | Simulates an error while sending the email confirmation after an email change. Expects `InternalServerErrorException`. |

### verifyEmailWithCode(String verificationCode)

| Test Case | Description |
| :--- | :--- |
| `verifyEmailWithCode_Success` | Successfully verifies an email address with a valid 6-digit verification code. Checks if the email verification record is deleted and the user is marked as "email verified". |
| `verifyEmailWithCode_BadRequestException_NullCode` | Attempts to verify the email with a `null` verification code. Expects `IllegalArgumentException`. |
| `verifyEmailWithCode_BadRequestException_EmptyCode` | Attempts to verify the email with an empty verification code. Expects `IllegalArgumentException`. |
| `verifyEmailWithCode_BadRequestException_InvalidFormat` | Attempts to verify the email with a verification code that does not match the 6-digit number format. Expects `IllegalArgumentException`. |
| `verifyEmailWithCode_BadRequestException_CodeNotFound` | Attempts to verify the email with a verification code for which no verification record exists. Expects `IllegalArgumentException`. |
| `verifyEmailWithCode_TokenExpiredException` | Attempts to verify the email with an expired verification code. Expects `TokenExpiredException`. |
| `verifyEmailWithCode_FailsWithUsedCode` | Ensures that an already used email verification code cannot be used again. |

## EventService

### createEvent(EventRequestDTO dto, User manager)

| Test Case | Description |
| :--- | :--- |
| `createEvent_Success` | Successfully creates a new event with valid data and a manager. |
| `createEvent_IllegalArgumentException_LocationNotFound` | Attempts to create an event with a non-existent EventLocation ID. Expects `IllegalArgumentException`. |

### updateEvent(Long id, EventRequestDTO dto, User manager)

| Test Case | Description |
| :--- | :--- |
| `updateEvent_Success_AsManager` | Successfully updates an existing event as the event's manager. |
| `updateEvent_Success_AsAdmin` | Successfully updates an existing event as an administrator. |
| `updateEvent_EventNotFoundException` | Attempts to update a non-existent event. Expects `EventNotFoundException`. |
| `updateEvent_ForbiddenException_NotManagerOrAdmin` | Attempts to update an event as a user who is neither a manager nor an administrator. Expects `ForbiddenException`. |
| `updateEvent_IllegalArgumentException_LocationNotFound` | Attempts to update an event with a non-existent EventLocation ID. Expects `IllegalArgumentException`. |

### getEventsByCurrentManager(User manager)

| Test Case | Description |
| :--- | :--- |
| `getEventsByCurrentManager_Success_AsAdmin` | Retrieves all events as an administrator. |
| `getEventsByCurrentManager_Success_AsManager` | Retrieves events belonging to the current manager. |
| `getEventsByCurrentManager_Success_NoEventsForManager` | Retrieves an empty list if the manager manages no events. |

### setReservationsAllowedForUser(EventUserAllowancesDto dto, User manager)

| Test Case | Description |
| :--- | :--- |
| `setReservationsAllowedForUser_Success_NewAllowance` | Successfully sets the allowed number of reservations for a user for an event (new entry). |
| `setReservationsAllowedForUser_Success_UpdateAllowance` | Successfully updates the allowed number of reservations for a user for an event (existing entry). |
| `setReservationsAllowedForUser_EventNotFoundException` | Attempts to set reservation allowance for a non-existent event. Expects `EventNotFoundException`. |
| `setReservationsAllowedForUser_UserNotFoundException` | Attempts to set reservation allowance for a non-existent user. Expects `UserNotFoundException`. |
| `setReservationsAllowedForUser_ForbiddenException_NotManagerOrAdmin` | Attempts to set reservation allowance as a user who is neither a manager nor an administrator of the event. Expects `ForbiddenException`. |
| `setReservationsAllowedForUser_Success_AsAdmin` | Successfully sets the allowed number of reservations for a user for an event by an administrator and checks for correct data persistence. |

### updateReservationAllowance(EventUserAllowanceUpdateDto dto, User manager)

| Test Case | Description |
| :--- | :--- |
| `updateReservationAllowance_Success_AsManager` | Successfully updates an existing reservation allowance as the event's manager. |
| `updateReservationAllowance_Success_AsAdmin` | Successfully updates an existing reservation allowance as an administrator. |
| `updateReservationAllowance_EventNotFoundException_AllowanceNotFound` | Attempts to update a non-existent reservation allowance. Expects `EventNotFoundException`. |
| `updateReservationAllowance_SecurityException_NotManagerOrAdmin` | Attempts to update a reservation allowance as a user who is neither a manager nor an administrator of the event. Expects `SecurityException`. |

### getReservationAllowanceById(Long id, User manager)

| Test Case | Description |
| :--- | :--- |
| `getReservationAllowanceById_Success_AsManager` | Successfully retrieves an `EventUserAllowance` as the responsible manager. |
| `getReservationAllowanceById_Success_AsAdmin` | Successfully retrieves an `EventUserAllowance` as an administrator. |
| `getReservationAllowanceById_ForbiddenException_NotManagerOrAdmin` | Attempts to retrieve an `EventUserAllowance` as an unauthorized user. Expects `SecurityException`. |
| `getReservationAllowanceById_EventNotFoundException` | Attempts to retrieve a non-existent `EventUserAllowance`. Expects `EventNotFoundException`. |

## ReservationService

### findAllReservations(User currentUser)

| Test Case | Description |
| :--- | :--- |
| `findAllReservations_Success_AsAdmin` | Retrieves all reservations as an administrator. |
| `findAllReservations_Success_AsManager` | Retrieves reservations for events that the manager is allowed to manage. |
| `findAllReservations_Success_NoAllowedEventsForManager` | Retrieves an empty list if the manager is not allowed to manage any events. |
| `findAllReservations_ForbiddenException_OtherRoles` | Attempts to retrieve reservations as a user with a different role. Expects `ForbiddenException`. |

### findReservationById(Long id, User currentUser)

| Test Case | Description |
| :--- | :--- |
| `findReservationById_Success_AsAdmin` | Retrieves a reservation as an administrator. |
| `findReservationById_Success_AsManager` | Retrieves a reservation belonging to an event that the manager is allowed to manage. |
| `findReservationById_NotFoundException` | Attempts to retrieve a non-existent reservation. Expects `NotFoundException`. |
| `findReservationById_ForbiddenException_NotAllowed` | Attempts to retrieve a reservation for which the user has no permission. Expects `ForbiddenException`. |

### createReservation(ReservationRequestDTO dto, User currentUser)

| Test Case | Description |
| :--- | :--- |
| `createReservation_Success_AsAdmin` | Successfully creates a reservation as an administrator. |
| `createReservation_Success_AsManager` | Successfully creates a reservation as a manager for an event they are allowed to manage. |
| `createReservation_UserNotFoundException_TargetUser` | Attempts to create a reservation for a non-existent target user. Expects `UserNotFoundException`. |
| `createReservation_NotFoundException_EventNotFound` | Attempts to create a reservation for a non-existent event. Expects `NotFoundException`. |
| `createReservation_ForbiddenException_NotAllowed` | Attempts to create a reservation as a user who has no permission. Expects `ForbiddenException`. |
| `createReservation_NotFoundException_SeatNotFound` | Attempts to create a reservation for a non-existent seat. Expects `NotFoundException`. |
| `createReservation_BadRequestException_NoAllowance` | Attempts to create a reservation when the user has no reservation allowance for the event. Expects `BadRequestException`. |
| `createReservation_BadRequestException_AllowanceZero` | Attempts to create a reservation when the user's reservation allowance is 0. Expects `BadRequestException`. |
| `createReservation_Forbidden_AsUser` | Attempts to create a reservation as a regular user, which should fail. |

### updateReservation(Long id, ReservationRequestDTO dto, User currentUser)

| Test Case | Description |
| :--- | :--- |
| `updateReservation_Success_AsAdmin` | Successfully updates a reservation as an administrator. |
| `updateReservation_Success_AsManager` | Successfully updates a reservation as a manager for an event they are allowed to manage. |
| `updateReservation_NotFoundException_ReservationNotFound` | Attempts to update a non-existent reservation. Expects `NotFoundException`. |
| `updateReservation_ForbiddenException_NotAllowed` | Attempts to update a reservation as a user who has no permission. Expects `ForbiddenException`. |
| `updateReservation_NotFoundException_NewEventNotFound` | Attempts to update a reservation to a non-existent new event. Expects `NotFoundException`. |
| `updateReservation_ForbiddenException_NewEventNotAllowed` | Attempts to update a reservation to a new event for which the manager has no permission. Expects `ForbiddenException`. |
| `updateReservation_NotFoundException_UserNotFound` | Attempts to update a reservation with a non-existent user. Expects `NotFoundException`. |
| `updateReservation_NotFoundException_SeatNotFound` | Attempts to update a reservation with a non-existent seat. Expects `NotFoundException`. |

### deleteReservation(Long id, User currentUser)

| Test Case | Description |
| :--- | :--- |
| `deleteReservation_Success_AsAdmin` | Successfully deletes a reservation as an administrator. Verifies that no allowance exists and no error is thrown. |
| `deleteReservation_Success_AsManager` | Successfully deletes a reservation as a manager for an event they are allowed to manage. Verifies that no allowance exists and no error is thrown. |
| `deleteReservation_Forbidden` | Attempts to delete a reservation as a user who has no permission. Expects `SecurityException`. |
| `deleteReservation_Success_WithAllowanceIncrement` | Successfully deletes a reservation and increments the user's allowance count from 0 to 1 when an allowance exists. |
| `deleteReservation_Success_NoAllowanceExists` | Successfully deletes a reservation when no allowance exists for the user and event. Verifies no error is thrown and allowance is not persisted. |
| `deleteReservation_Success_BlockedReservation_NoAllowanceIncrement` | Successfully deletes a blocked reservation and verifies that the allowance count is not incremented. |
| `deleteReservation_Success_MultipleReservations_WithAllowanceIncrement` | Successfully deletes multiple reservations for the same user and event, incrementing the allowance count for each reservation. |
| `deleteReservation_Success_MixedStatus_OnlyReservedIncrementsAllowance` | Successfully deletes both reserved and blocked reservations, verifying that only reserved reservations increment the allowance count. |
| `deleteReservation_Success_DifferentAllowanceCounts` | Successfully deletes a reservation with a non-zero starting allowance count (5) and verifies it increments to 6. |
| `blockSeats_Success` | Successfully blocks seats for an event as a manager. |
| `blockSeats_Forbidden` | Attempts to block seats as an unauthorized user. Expects `SecurityException`. |
| `blockSeats_SeatAlreadyReserved` | Attempts to block already reserved or blocked seats. Expects `IllegalStateException`. |

## EventService (reservation package)

### getEventsForCurrentUser(String username)

| Test Case | Description |
| :--- | :--- |
| `getEventsForCurrentUser_Success` | Successfully retrieves events for the current user based on their EventUserAllowances. The response includes the number of allowed reservations. |
| `getEventsForCurrentUser_UserNotFoundException` | Attempts to retrieve events for a non-existent user. Expects `UserNotFoundException`. |
| `getEventsForCurrentUser_Success_NoEvents` | Retrieves an empty list if the user has no EventUserAllowances. |

## ReservationService (reservation package)

### findReservationsByUser(User currentUser)

| Test Case | Description |
| :--- | :--- |
| `findReservationsByUser_Success` | Successfully retrieves all reservations for the current user. |
| `findReservationsByUser_Success_NoReservations` | Retrieves an empty list if the user has no reservations. |

### findReservationByIdForUser(Long id, User currentUser)

| Test Case | Description |
| :--- | :--- |
| `findReservationByIdForUser_Success` | Successfully retrieves a reservation for the current user by ID. |
| `findReservationByIdForUser_NotFoundException` | Attempts to retrieve a non-existent reservation. Expects `NotFoundException`. |
| `findReservationByIdForUser_ForbiddenException` | Attempts to retrieve a reservation that does not belong to the current user. Expects `ForbiddenException`. |

### createReservationForUser(UserReservationsRequestDTO dto, User currentUser)

| Test Case | Description |
| :--- | :--- |
| `createReservationForUser_Success` | Successfully creates one or more reservations for the current user. |
| `createReservationForUser_NotFoundException_EventNotFound` | Attempts to create a reservation for a non-existent event. Expects `NotFoundException`. |
| `createReservationForUser_NotFoundException_SeatNotFound` | Attempts to create a reservation for one or more non-existent seats. Expects `NotFoundException`. |
| `createReservationForUser_ForbiddenException_NoAllowance` | Attempts to create a reservation when the user has no reservation allowance for the event. Expects `ForbiddenException`. |
| `createReservationForUser_NoSeatsAvailableException_LimitReached` | Attempts to create more reservations than the allowed number for the user. Expects `NoSeatsAvailableException`. |
| `createReservationForUser_EventBookingClosedException_BookingDeadlinePassed` | Attempts to create a reservation for an event whose booking deadline has passed. Expects `EventBookingClosedException`. |
| `createReservationForUser_EventBookingClosedException_BookingNotStarted` | Attempts to create a reservation for an event whose booking start time has not yet been reached. Expects `EventBookingClosedException`. |
| `createReservationForUser_SeatAlreadyReservedException` | Attempts to create a reservation for an already reserved seat. Expects `SeatAlreadyReservedException`. |
| `createReservationForUser_IllegalArgumentException_NoSeatIds` | Attempts to create a reservation without specifying seat IDs. |
| `createReservationForUser_IllegalStateException_EmailNotVerified` | Attempts to create a reservation with a user whose email address is not verified. Expects `IllegalStateException`. |

### deleteReservationForUser(List<Long> ids, User currentUser)

| Test Case | Description |
| :--- | :--- |
| `deleteReservationForUser_Success` | Successfully deletes multiple reservations for the current user. |
| `deleteReservationForUser_NotFoundException` | Attempts to delete non-existent reservations. Expects `NotFoundException`. |
| `deleteReservationForUser_ForbiddenException_NotOwner` | Attempts to delete reservations that do not belong to the current user. Expects `ForbiddenException`. |
| `deleteReservationForUser_ForbiddenException_NoAllowance` | Attempts to delete reservations when the user has no reservation allowance for the event. Expects `ForbiddenException`. |

## EventLocation Service

### EventLocationService (Manager/Admin)

| Test Case | Description |
| :--- | :--- |
| `getEventLocationsByCurrentManager_Success_AsAdmin` | Retrieves all EventLocations as an administrator. |
| `getEventLocationsByCurrentManager_Success_AsManager` | Retrieves EventLocations belonging to the current manager. |
| `getEventLocationsByCurrentManager_Success_NoEventLocationsForManager` | Retrieves an empty list if the manager manages no EventLocations. |
| `createEventLocation_Success` | Successfully creates a new EventLocation with valid data. |
| `createEventLocation_InvalidInput` | Attempts to create an EventLocation with invalid data (e.g., empty fields). |
| `createEventLocation_InvalidInput_NegativeCapacity` | Attempts to create an EventLocation with negative capacity. |
| `updateEventLocation_Success_AsManager` | Successfully updates an existing EventLocation as the owner. |
| `updateEventLocation_Success_AsAdmin` | Successfully updates an existing EventLocation as an administrator. |
| `updateEventLocation_NotFound` | Attempts to update a non-existent EventLocation. |
| `updateEventLocation_ForbiddenException_NotManagerOrAdmin` | Attempts to update an EventLocation without the required permissions. |
| `deleteEventLocation_Success_AsManager` | Successfully deletes an existing EventLocation as the owner. |
| `deleteEventLocation_Success_AsAdmin` | Successfully deletes an existing EventLocation as an administrator. |
| `deleteEventLocation_NotFound` | Attempts to delete a non-existent EventLocation. |
| `deleteEventLocation_ForbiddenException_NotManagerOrAdmin` | Attempts to delete an EventLocation without the required permissions. |
| `importEventLocation_Success` | Successfully creates a new EventLocation with a list of seats. |
| `importSeatsToEventLocation_Success` | Successfully imports seats to an existing EventLocation as a manager. |
| `importSeatsToEventLocation_Success_AsAdmin` | Successfully imports seats to an existing EventLocation as an administrator. |
| `importSeatsToEventLocation_NotFound` | Attempts to import seats to a non-existent EventLocation. |
| `importSeatsToEventLocation_Forbidden` | Attempts to import seats to an EventLocation for which there is no permission. |
| `createEventLocation_WithMarkers_Success` | Successfully creates a new EventLocation with markers. |
| `createEventLocation_WithNullMarkers_Success` | Successfully creates a new EventLocation with a null marker list. |
| `createEventLocation_WithEmptyMarkers_Success` | Successfully creates a new EventLocation with an empty marker list. |
| `updateEventLocation_WithMarkers_Success` | Successfully updates an existing EventLocation with new markers. |
| `updateEventLocation_ClearingMarkers_Success` | Successfully updates an existing EventLocation and deletes all markers. |
| `convertToMarkerEntities_ValidInput` | Tests the conversion of marker DTOs to entities with various limits. |

### EventLocationService (User)

| Test Case | Description |
| :--- | :--- |
| `getLocationsForCurrentUser_Success_FromAllowanceAndReservation` | Successfully retrieves event locations for the current user based on permissions and reservations. |
| `getLocationsForCurrentUser_Deduplicates_Locations` | Ensures that duplicate event locations from permissions and reservations are correctly deduplicated. |
| `getLocationsForCurrentUser_Empty` | Retrieves an empty list if the user has no event locations via permissions or reservations. |
| `getLocationsForCurrentUser_UserNotFound` | Attempts to retrieve event locations for a non-existent user. Expects `UserNotFoundException`. |
| `getLocationsForCurrentUser_Success_OnlyFromAllowance` | Successfully retrieves event locations for the current user when only permissions are present. |
| `getLocationsForCurrentUser_Success_OnlyFromReservation` | Successfully retrieves event locations for the current user when only reservations are present. |
| `getLocationsForCurrentUser_NoAllowanceNoReservation` | Retrieves an empty list if the user has neither permissions nor reservations. |
| `getLocationsForCurrentUser_OneLocationWithAllowance_OneLocationWithReservation` | Successfully retrieves event locations when one location is assigned via permission and another via reservation. |
| `getLocationsForCurrentUser_TwoDifferentLocations_OneAllowanceOneReservation` | Successfully retrieves event locations when two different locations are assigned via permission and reservation. |
| `getLocationsForCurrentUser_OneLocationTwoEvents_OneAllowanceOneReservation` | Successfully retrieves a single event location when it is assigned via two different events (one with permission, one with reservation). |

### EventLocationResource (User)

| Test Case | Description |
| :--- | :--- |
| `getLocations_ReturnsLocationsForUser` | Successfully retrieves event locations for the authenticated user. |
| `getLocations_NoAuth_ReturnsUnauthorized` | Attempts to retrieve event locations without authentication. Expects `401 Unauthorized`. |
| `getLocations_UserWithNoLocations_ReturnsEmptyList` | Retrieves an empty list if the authenticated user has no event locations. |
| `getLocations_DeduplicatesLocations_FromAllowanceAndReservation` | Ensures that event locations are correctly deduplicated when they are accessible via both permissions and reservations. |

## EventLocationMarker Tests

### EventLocationMarker

| Test Case | Description |
| :--- | :--- |
| `testDefaultConstructor` | Checks for correct initialization with the default constructor. |
| `testParameterizedConstructor` | Checks for correct initialization with the parameterized constructor. |
| `testSettersAndGetters` | Tests all setter and getter methods. |
| `testEquals_SameObject` | Checks equals() with the same object. |
| `testEquals_EqualObjects` | Checks equals() with identical objects. |
| `testEquals_DifferentLabel` | Checks equals() with different labels. |
| `testEquals_DifferentXCoordinate` | Checks equals() with different X coordinates. |
| `testEquals_DifferentYCoordinate` | Checks equals() with different Y coordinates. |
| `testEquals_NullObject` | Checks equals() with a null object. |
| `testEquals_DifferentClass` | Checks equals() with a different class type. |
| `testEquals_NullValues` | Checks equals() with null values in both objects. |
| `testEquals_MixedNullValues` | Checks equals() with mixed null values. |
| `testHashCode_EqualObjects` | Checks for consistent hashCode() values for equal objects. |
| `testHashCode_DifferentObjects` | Checks for different hashCode() values for different objects. |
| `testHashCode_NullValues` | Checks hashCode() with null values. |
| `testToString` | Checks the toString() output with normal values. |
| `testToString_NullValues` | Checks the toString() output with null values. |
| `testCoordinatesBoundaries` | Tests boundary values for coordinates (Integer.MAX_VALUE, Integer.MIN_VALUE). |
| `testEmptyLabel` | Tests behavior with an empty label. |

### MakerRequestDTO

| Test Case | Description |
| :--- | :--- |
| `testDefaultConstructor` | Checks for correct initialization with the default constructor. |
| `testParameterizedConstructor` | Checks for correct initialization with the parameterized constructor. |
| `testSettersAndGetters` | Tests all setter and getter methods. |
| `testSettersWithNullValues` | Tests setters with null values. |
| `testWithBoundaryValues` | Tests boundary values for coordinates. |
| `testWithZeroCoordinates` | Tests behavior with zero coordinates. |
| `testWithNegativeCoordinates` | Tests behavior with negative coordinates. |
| `testLongLabel` | Tests behavior with a very long label. |
| `testSetterChaining` | Tests that setters function independently. |
| `testOverwriteValues` | Tests overwriting values. |

### EventLocationMakerDTO

| Test Case | Description |
| :--- | :--- |
| `testConstructorWithMarkerEntity` | Checks constructor with EventLocationMarker entity. |
| `testDirectConstructor` | Checks direct constructor with parameters. |
| `testWithZeroCoordinates` | Tests behavior with zero coordinates. |
| `testWithNegativeCoordinates` | Tests behavior with negative coordinates. |
| `testWithBoundaryValues` | Tests boundary values for coordinates. |
| `testWithNullLabel` | Tests behavior with a null label. |
| `testWithEmptyLabel` | Tests behavior with an empty label. |
| `testWithLongLabel` | Tests behavior with a very long label. |
| `testRecordEquality` | Tests record equality and differences. |
| `testRecordHashCode` | Tests record hashCode consistency. |
| `testRecordToString` | Tests record toString method. |
| `testConversionConsistency` | Tests consistency in conversion from Entity to DTO. |
| `testNullCoordinatesInEntity` | Tests behavior with null coordinates in Entity (NullPointerException expected). |

## Seat Service

### SeatService

| Test Case | Description |
| :--- | :--- |
| `createSeat_Success` | Successfully creates a new seat with valid data. |
| `createSeat_Success_AsManager` | Successfully creates a new seat as a manager. |
| `createSeat_Success_AsAdmin` | Successfully creates a new seat as an admin. |
| `createSeat_ForbiddenException_NotManagerOfLocation` | Attempts to create a seat for a location that one does not own. |
| `createSeat_InvalidInput` | Attempts to create a seat with invalid data. |
| `findAllSeatsForManager_Success_AsAdmin` | Retrieves all seats as an administrator. |
| `findAllSeatsForManager_Success_AsManager` | Retrieves seats belonging to the current manager. |
| `findAllSeatsForManager_Success_NoSeatsForManager` | Retrieves an empty list if the manager manages no seats. |
| `findSeatByIdForManager_Success_AsAdmin` | Retrieves a seat as an administrator. |
| `findSeatByIdForManager_Success_AsManager` | Retrieves a seat belonging to the current manager. |
| `findSeatByIdForManager_NotFound` | Attempts to retrieve a non-existent seat. |
| `findSeatByIdForManager_ForbiddenException` | Attempts to retrieve a seat for which there is no permission. |
| `updateSeat_Success_AsManager` | Successfully updates an existing seat as a manager. |
| `updateSeat_Success_AsAdmin` | Successfully updates an existing seat as an admin. |
| `updateSeat_NotFound` | Attempts to update a non-existent seat. |
| `updateSeat_InvalidInput` | Attempts to update a seat with invalid data. |
| `updateSeat_ForbiddenException_NotManagerOfSeatLocation` | Attempts to update a seat belonging to a foreign location. |
| `updateSeat_ForbiddenException_NotManagerOfNewLocation` | Attempts to move a seat to a foreign location. |
| `deleteSeat_Success_AsManager` | Successfully deletes an existing seat as a manager. |
| `deleteSeat_Success_AsAdmin` | Successfully deletes an existing seat as an admin. |
| `deleteSeat_NotFound` | Attempts to delete a non-existent seat. |
| `deleteSeat_ForbiddenException_NotManager` | Attempts to delete a seat for which there is no permission. |
| `findSeatEntityById_Success` | Successfully retrieves a seat entity. |
| `findSeatEntityById_ForbiddenException` | Attempts to retrieve a seat entity for which the user has no permission. |

## GlobalExceptionHandler

### GlobalExceptionHandler

| Test Case | Description |
| :--- | :--- |
| `testUserNotFoundException` | Tests the handling of `UserNotFoundException` and expects HTTP status 404 (Not Found). |
| `testEventNotFoundException` | Tests the handling of `EventNotFoundException` and expects HTTP status 404 (Not Found). |
| `testSeatNotFoundException` | Tests the handling of `SeatNotFoundException` and expects HTTP status 404 (Not Found). |
| `testReservationNotFoundException` | Tests the handling of `ReservationNotFoundException` and expects HTTP status 404 (Not Found). |
| `testEventLocationNotFoundException` | Tests the handling of `EventLocationNotFoundException` and expects HTTP status 404 (Not Found). |
| `testDuplicateUserException` | Tests the handling of `DuplicateUserException` and expects HTTP status 409 (Conflict). |
| `testSeatAlreadyReservedException` | Tests the handling of `SeatAlreadyReservedException` and expects HTTP status 409 (Conflict). |
| `testAuthenticationFailedException` | Tests the handling of `AuthenticationFailedException` and expects HTTP status 401 (Unauthorized). |
| `testJwtInvalidException` | Tests the handling of `JwtInvalidException` and expects HTTP status 401 (Unauthorized). |
| `testVerifyTokenExpiredException` | Tests the handling of `VerifyTokenExpiredException` and expects HTTP status 410 (Gone). |
| `testInvalidUserException` | Tests the handling of `InvalidUserException` and expects HTTP status 400 (Bad Request). |
| `testEventBookingClosedException` | Tests the handling of `EventBookingClosedException` and expects HTTP status 400 (Bad Request). |
| `testNoSeatsAvailableException` | Tests the handling of `NoSeatsAvailableException` and expects HTTP status 400 (Bad Request). |
| `testVerificationCodeNotFoundException` | Tests the handling of `VerificationCodeNotFoundException` and expects HTTP status 400 (Bad Request). |
| `testGenericException` | Tests the handling of generic `RuntimeException` and expects HTTP status 500 (Internal Server Error) with the original error message. |
| `testNullPointerException` | Tests the handling of `NullPointerException` and expects HTTP status 500 (Internal Server Error). |
| `testExceptionWithNullMessage` | Tests the handling of exceptions with a null message and expects HTTP status 500 (Internal Server Error). |
| `testExceptionWithEmptyMessage` | Tests the handling of exceptions with an empty message and expects HTTP status 500 (Internal Server Error). |

**Important Changes:**
- `EventBookingClosedException` and `NoSeatsAvailableException` now return HTTP status 400 (Bad Request) instead of 406 (Not Acceptable)
- Generic exceptions return the original error message without the additional "An unexpected error occurred: " prefix
- `ErrorResponseDTO` has been extended with a `getMessage()` method for better compatibility

## HttpForwardFilter

### HttpForwardFilter

| Test Case | Description |
| :--- | :--- |
| `doFilter_ForwardToRootPath` | Checks if the filter forwards requests that do not start with `/api` or `/q` and have a 404 status to the root path `/`. |
| `doFilter_NoForwardForApiOrQuarkusPath` | Checks if the filter does not forward requests that start with `/api` or `/q`. |
| `doFilter_NoForwardForNon404Status` | Checks if the filter does not forward requests with a status other than 404. |

# Resource Tests

This document describes the tests for the REST resources.

## Event Management

### EventLocationResource

Base Path: `/api/manager/eventlocations`

Roles: `MANAGER`, `ADMIN`

---

#### GET /

Retrieves all event locations for the current manager.

**Description:**

This test checks if a manager or administrator can retrieve a list of their event locations.

**Test Cases:**

*   **Success:**
    *   A manager with associated event locations retrieves the list and receives a `200 OK` status with the correct data.
    *   A manager without event locations retrieves the list and receives a `200 OK` status with an empty list.
*   **Failure:**
    *   An unauthenticated user attempts to access the endpoint and receives a `401 Unauthorized` status.
    *   A user with a different role (e.g., `USER`) attempts to access the endpoint and receives a `403 Forbidden` status.

---

#### POST /

Creates a new event location.

**Description:**

This test ensures that a manager or administrator can create a new event location.

**Test Cases:**

*   **Success:**
    *   A manager sends valid data and successfully creates a new event location. A `200 OK` status is returned with the data of the created location.
*   **Failure:**
    *   A manager sends invalid data (e.g., missing name) and receives a `400 Bad Request` status.
    *   An unauthenticated user attempts to create a location and receives `401 Unauthorized`.
    *   A user with the `USER` role attempts to create a location and receives `403 Forbidden`.
---

#### POST /import

Creates a new event location with seats.

**Description:**

This test ensures that a manager or administrator can import a new event location along with a list of seats.

**Test Cases:**

*   **Success:**
    *   A manager sends valid data and successfully imports a new event location and its associated seats. A `200 OK` status is returned with the data of the imported location.
*   **Failure:**
    *   A manager sends invalid data (e.g., missing name in the location or missing seat number) and receives a `400 Bad Request` status.
    *   An unauthenticated user attempts to create a location and receives `401 Unauthorized`.
    *   A user with the `USER` role attempts to create a location and receives `403 Forbidden`.

---

#### PUT /{id}

Updates an existing event location.

**Description:**

This test checks the update functionality for an event location.

**Test Cases:**

*   **Success:**
    *   A manager updates one of their event locations with valid data and receives `200 OK` with the updated data.
*   **Failure:**
    *   A manager attempts to update a location with invalid data and receives `400 Bad Request`.
    *   A manager attempts to update a location that does not belong to them and receives a `404 Not Found` or `403 Forbidden` status.
    *   A manager attempts to update a non-existent location and receives `404 Not Found`.
    *   An unauthenticated user attempts to update a location and receives `401 Unauthorized`.

---

#### DELETE /{id}

Deletes an event location.

**Description:**

This test ensures that a manager or administrator can delete one of their event locations.

**Test Cases:**

*   **Success:**
    *   A manager successfully deletes one of their event locations and receives a `200 OK` status.
*   **Failure:**
    *   A manager attempts to delete a location that does not belong to them and receives `404 Not Found` or `403 Forbidden`.
    *   A manager attempts to delete a non-existent location and receives `404 Not Found`.
    *   An unauthenticated user attempts to delete a location and receives `401 Unauthorized`.

## Reservation

### EventResource

Base Path: `/api/user/events`

Role: `USER`

---

#### GET /


---

### EventResource (Manager)

Base Path: `/api/manager/events`

Roles: `MANAGER`, `ADMIN`

---

#### POST /

Creates a new event.

**Description:**

This test ensures that a manager or admin can create a new event for one of their event locations.

**Test Cases:**

*   **Success:**
    *   A manager sends valid event data and successfully creates a new event. They receive `200 OK` with the detailed data of the event.
*   **Failure:**
    *   A manager attempts to create an event for a location that does not belong to them and receives `404 Not Found`.
    *   Invalid data (e.g., start date after end date) results in `400 Bad Request`.
    *   An unauthorized user (e.g., `USER`) receives `403 Forbidden`.

---

#### PUT /{id}

Updates an existing event.

**Description:**

This test checks the update of an event by a manager or admin.

**Test Cases:**

*   **Success:**
    *   A manager updates an event they manage with valid data and receives `200 OK`.
*   **Failure:**
    *   Attempting to update an event that does not exist results in `404 Not Found`.
    *   Attempting to update an event belonging to another manager results in `404 Not Found`.
    *   Invalid data results in `400 Bad Request`.

---

#### GET /

Retrieves all events managed by the current manager.

**Description:**

This test ensures that a manager or admin can retrieve a list of their own events.

**Test Cases:**

*   **Success:**
    *   A manager retrieves their event list and receives `200 OK` with the data.
*   **Failure:**
    *   An unauthorized user receives `403 Forbidden`.

---

#### GET /{id}

Retrieves a specific event by its ID.

**Description:**

This test ensures that a manager or administrator can retrieve a specific event by its ID, provided they are authorized.

**Test Cases:**

*   **Success:**
    *   A manager retrieves an event they manage and receives `200 OK` with the event data.
    *   An administrator retrieves an event they do not manage and receives `200 OK` with the event data.
*   **Failure:**
    *   A manager attempts to retrieve an event belonging to another manager and receives `403 Forbidden`.
    *   A non-existent event is requested, and `404 Not Found` is returned.
    *   An unauthenticated user attempts to access the endpoint and receives `401 Unauthorized`.

---

#### DELETE /{id}

Deletes an event and all associated data.

**Description:**

This test ensures that a manager or administrator can delete an event. Deleting an event should also remove all associated `EventUserAllowance` entries and reservations (cascade delete).

**Test Cases:**

*   **Success:**
    *   A manager successfully deletes an event they manage and receives a `204 No Content` status.
    *   An administrator successfully deletes an event they do not manage and receives a `204 No Content` status.
    *   After deleting the event, it is verified that the associated `EventUserAllowance` entries have also been deleted.
*   **Failure:**
    *   A manager attempts to delete an event that does not belong to them and receives a `403 Forbidden` status.
    *   A manager attempts to delete a non-existent event and receives a `404 Not Found` status.
    *   An unauthenticated user attempts to delete an event and receives a `401 Unauthorized` status.
    *   A user with the `USER` role attempts to delete an event and receives a `403 Forbidden` status.

---

### ReservationResource (Manager)

Base Path: `/api/manager/reservations`

Roles: `MANAGER`, `ADMIN`

---

#### GET /

Retrieves all reservations for the current manager's events.

**Description:**

This test ensures that a manager can view all reservations for their events.

**Test Cases:**

*   **Success:**
    *   A manager retrieves the list and receives `200 OK` with all relevant reservations.
*   **Failure:**
    *   An unauthorized user receives `403 Forbidden`.

---

#### GET /{id}

Retrieves a specific reservation by its ID.

**Description:**

This test checks if a manager can view a specific reservation, provided it belongs to one of their events.

**Test Cases:**

*   **Success:**
    *   A manager retrieves a reservation belonging to one of their events and receives `200 OK`.
*   **Failure:**
    *   Attempting to retrieve a non-existent reservation results in `404 Not Found`.
    *   Attempting to retrieve a reservation belonging to another manager's event results in `404 Not Found`.

---

#### POST /

Creates a new reservation (as a manager).

**Description:**

This test allows a manager to manually create a reservation for a user.

**Test Cases:**

*   **Success:**
    *   A manager creates a valid reservation for a user and receives `200 OK`.
*   **Failure:**
    *   Invalid data (e.g., non-existent user or seat) results in `404 Not Found`.
    *   Attempting to book an already reserved seat results in `409 Conflict`.

---

#### PUT /{id}

Updates an existing reservation (as a manager).

**Description:**

This test checks the update of a reservation by a manager.

**Test Cases:**

*   **Success:**
    *   A manager updates a reservation with valid data and receives `200 OK`.
*   **Failure:**
    *   Attempting to update a reservation that does not belong to one of their events results in `404 Not Found`.

---

#### DELETE /{id}

Deletes a reservation (as a manager).

**Description:**

This test ensures that a manager can delete a reservation for one of their events.

**Test Cases:**

*   **Success:**
    *   A manager deletes a reservation and receives `200 OK`.
*   **Failure:**
    *   Attempting to delete a reservation that does not belong to one of their events results in `404 Not Found`.

---

#### GET /event/{id}

Retrieves all reservations for a specific event.

**Description:**

This test ensures that a manager can view all reservations for a specific event they manage.

**Test Cases:**

*   **Success:**
    *   A manager retrieves the list of reservations for an event they manage and receives `200 OK` with the correct data.
*   **Failure:**
    *   A manager attempts to retrieve reservations for an event they do not manage and receives a `403 Forbidden` status.
    *   A manager attempts to retrieve reservations for a non-existent event and receives a `400 Bad Request` status.
    *   An unauthenticated user attempts to access the endpoint and receives a `401 Unauthorized` status.
    *   A user with a different role (e.g., `USER`) attempts to access the endpoint and receives a `403 Forbidden` status.

---

### SeatResource (Manager)

Base Path: `/api/manager/seats`

Roles: `MANAGER`, `ADMIN`

---

#### POST /

Creates a new seat for an event location.

**Description:**

This test ensures that a manager can add new seats to one of their locations.

**Test Cases:**

*   **Success:**
    *   A manager adds a new seat to one of their locations and receives `200 OK`.
*   **Failure:**
    *   Attempting to add a seat to a location belonging to another manager results in `404 Not Found`.

---

#### GET /

Retrieves all seats belonging to the current manager's locations.

**Description:**

This test ensures that a manager can retrieve a list of all their seats.

**Test Cases:**

*   **Success:**
    *   A manager retrieves the list of their seats and receives `200 OK`.
*   **Failure:**
    *   An unauthorized user receives `403 Forbidden`.

---

#### GET /{id}

Retrieves a specific seat by its ID.

**Description:**

This test checks if a manager can view a specific seat, provided it belongs to one of their locations.

**Test Cases:**

*   **Success:**
    *   A manager retrieves one of their seats and receives `200 OK`.
*   **Failure:**
    *   Attempting to retrieve a seat belonging to another manager's location results in `404 Not Found`.

---

#### PUT /{id}

Updates a seat.

**Description:**

This test checks the update of a seat by a manager.

**Test Cases:**

*   **Success:**
    *   A manager updates one of their seats and receives `200 OK`.
*   **Failure:**
    *   Attempting to update a seat that does not belong to their locations results in `404 Not Found`.

---

#### DELETE /{id}

Deletes a seat.

**Description:**

This test ensures that a manager can remove a seat from one of their locations.

**Test Cases:**

*   **Success:**
    *   A manager deletes one of their seats and receives `200 OK`.
*   **Failure:**
    *   Attempting to delete a seat that does not belong to their locations results in `404 Not Found`.
    *   Attempting to delete a seat for which a reservation already exists results in `409 Conflict`.
Retrieves all events for which the current user has permission. The response also includes the number of allowed reservations for each event.

**Description:**

This test ensures that a user can retrieve a list of available events for them.

**Test Cases:**

*   **Success:**
    *   An authenticated user with permissions for events retrieves the list and receives `200 OK` with the event data, including the allowed reservations.
    *   An authenticated user without permissions for events retrieves the list and receives `200 OK` with an empty list.
*   **Failure:**
    *   An unauthenticated user attempts to access the endpoint and receives `401 Unauthorized`.

---

### ReservationResource

Base Path: `/api/user/reservations`

Role: `USER`

---

#### GET /

Retrieves all reservations of the current user.

**Description:**

This test ensures that a user can retrieve their own reservations.

**Test Cases:**

*   **Success:**
    *   A user with reservations retrieves the list and receives `200 OK` with their reservation data.
    *   A user without reservations retrieves the list and receives `200 OK` with an empty list.
*   **Failure:**
    *   An unauthenticated user attempts to access the endpoint and receives `401 Unauthorized`.

---

#### GET /{id}

Retrieves a specific reservation of the current user.

**Description:**

This test checks if a user can retrieve one of their single reservations by ID.

**Test Cases:**

*   **Success:**
    *   A user retrieves one of their own reservations and receives `200 OK` with the reservation data.
*   **Failure:**
    *   A user attempts to retrieve a reservation that does not belong to them and receives `404 Not Found`.
    *   A user attempts to retrieve a non-existent reservation and receives `404 Not Found`.
    *   An unauthenticated user attempts to access the endpoint and receives `401 Unauthorized`.

---

#### POST /

Creates one or more new reservations for the current user.

**Description:**

This test ensures that a user can create new reservations for an event for which they are authorized.

**Test Cases:**

*   **Success:**
    *   A user sends a valid request to create reservations and receives `200 OK` with a list of the created reservations.
*   **Failure:**
    *   A user sends an invalid request (e.g., for an event for which they have no permission, or for already reserved seats) and receives an appropriate error status (`400 Bad Request`, `404 Not Found`, `409 Conflict`).
    *   A user attempts to reserve more seats than they are allowed and receives `400 Bad Request`.
    *   An unauthenticated user attempts to create a reservation and receives `401 Unauthorized`.

---

#### DELETE /{id}

Deletes a reservation of the current user.

**Description:**

This test checks if a user can delete one of their own reservations.

**Test Cases:**

*   **Success:**
    *   A user deletes one of their reservations and receives `200 OK`.
*   **Failure:**
    *   A user attempts to delete a reservation that does not belong to them and receives `404 Not Found`.
    *   A user attempts to delete a non-existent reservation and receives `404 Not Found`.
    *   An unauthenticated user attempts to delete a reservation and receives `401 Unauthorized`.

## User Management

### EmailConfirmationResource

Base Path: `/api/user`

Role: Public (no authentication required)

---

#### GET /confirm-email

Confirms a user's email address.

**Description:**

This test checks the email confirmation process via a token. The endpoint returns an HTML page.

**Test Cases:**

*   **Success:**
    *   A valid confirmation link (with ID and token) is used and the email address is successfully confirmed. The user sees a success page.
*   **Failure:**
    *   An invalid token is used, and the user sees an error page with a `400 Bad Request` status.
    *   A link with a non-existent ID is used, and the user sees an error page with a `404 Not Found` status.
    *   An expired token is used, and the user sees an error page with a `410 Gone` status.

---

### UserResource

Base Path: `/api/users`

Roles: `ADMIN`, `MANAGER`, `USER`

---

#### POST /admin/import

Imports a set of users (for admins only).

**Description:**

This test ensures that only administrators can import users in bulk.

**Test Cases:**

*   **Success:**
    *   An admin sends a valid set of user data and successfully imports the users. They receive `200 OK` with the data of the imported users.
*   **Failure:**
    *   An admin sends invalid data (e.g., empty usernames or duplicate users) and receives `400 Bad Request` or `409 Conflict`.
    *   A user with the `MANAGER` or `USER` role attempts to import users and receives `403 Forbidden`.
    *   An unauthenticated user attempts to access the endpoint and receives `401 Unauthorized`.

---

#### POST /admin

Creates a new user (for admins only).

**Description:**

This test ensures that only administrators can create new users.

**Test Cases:**

*   **Success:**
    *   An admin sends valid user data and successfully creates a new user. They receive `200 OK` with the data of the new user.
*   **Failure:**
    *   An admin sends invalid data (e.g., duplicate email) and receives `400 Bad Request`.
    *   A user with the `MANAGER` or `USER` role attempts to create a user and receives `403 Forbidden`.
    *   An unauthenticated user attempts to access the endpoint and receives `401 Unauthorized`.

---

#### PUT /admin/{id}

Updates a user (for admins only).

**Description:**

This test checks if an admin can update the data of any user.

**Test Cases:**

*   **Success:**
    *   An admin updates the data of an existing user and receives `200 OK` with the updated data.
*   **Failure:**
    *   An admin attempts to update a non-existent user and receives `404 Not Found`.
    *   A user with the `MANAGER` or `USER` role attempts to update another user and receives `403 Forbidden`.

---

#### DELETE /admin/{id}

Deletes a user (for admins only).

**Description:**

This test ensures that only administrators can delete users.

**Test Cases:**

*   **Success:**
    *   An admin deletes an existing user and receives `200 OK`.
*   **Failure:**
    *   An admin attempts to delete a non-existent user and receives `404 Not Found`.
    *   A user with the `MANAGER` or `USER` role attempts to delete a user and receives `403 Forbidden`.

---

#### GET /manager

Retrieves a list of all users with limited information (for admins and managers).

**Description:**

This test checks if admins and managers receive a list of all users.

**Test Cases:**

*   **Success:**
    *   An admin or manager retrieves the user list and receives `200 OK` with a list of `LimitedUserInfoDTO` objects.
*   **Failure:**
    *   A user with the `USER` role attempts to access the endpoint and receives `403 Forbidden`.
    *   An unauthenticated user attempts to access the endpoint and receives `401 Unauthorized`.

---

#### GET /admin/roles

Retrieves all available user roles (for admins only).

**Description:**

This test ensures that only admins can retrieve the list of available roles.

**Test Cases:**

*   **Success:**
    *   An admin retrieves the list of roles and receives `200 OK` with a list of strings.
*   **Failure:**
    *   A user with a different role attempts to access the endpoint and receives `403 Forbidden`.

---

#### GET /admin/{id}

Retrieves the full data of a specific user (for admins only).

**Description:**

This test checks if an admin can retrieve the full details of a user by their ID.

**Test Cases:**

*   **Success:**
    *   An admin retrieves the data of an existing user and receives `200 OK` with the `UserDTO` object.
*   **Failure:**
    *   An admin attempts to retrieve a non-existent user and receives `404 Not Found`.
    *   A user with a different role attempts to access the endpoint and receives `403 Forbidden`.

---

#### PUT /me

Updates the profile of the currently logged-in user.

**Description:**

This test ensures that an authenticated user can update their own profile.

**Test Cases:**

*   **Success:**
    *   An authenticated user updates their own profile with valid data and receives `200 OK` with the updated data.
*   **Failure:**
    *   A user sends invalid data and receives `400 Bad Request`.
    *   An unauthenticated user attempts to access the endpoint and receives `401 Unauthorized`.

## Security

### AuthResource

Base Path: `/api/auth`

Role: Public

---

#### POST /login

Authenticates a user and returns a JWT token.

**Description:**

This test checks the login process.

**Test Cases:**

*   **Success:**
    *   A registered and confirmed user sends valid credentials (username/email and password) and receives a `200 OK` status with a JWT cookie.
*   **Failure:**
    *   A user sends invalid credentials (wrong username or password) and receives a `401 Unauthorized` status.
    *   A user whose email address has not yet been confirmed attempts to log in and receives a `401 Unauthorized` status.
    *   The request has an invalid format (e.g., missing password) and receives a `400 Bad Request` status.

## Frontend Internationalization (i18n)

This section describes test cases for the internationalization of the user interface, particularly the correct display of texts based on the selected language and the handling of singular/plural forms.

### EventsSubPage (webapp/components/events/events-page.tsx)

| Test Case | Description |
| :--- | :--- |
| `displayNoEventsAvailable` | Checks if the text "No events available" (or the corresponding translation) is displayed correctly when no events are available. |
| `displayTryAgainOrCheckSearch` | Checks if the text "Try again or check your search" (or the corresponding translation) is displayed correctly when no events were found. |

### ReservationFormModal (webapp/components/management/reservation-form-modal.tsx)

| Test Case | Description |
| :--- | :--- |
| `displaySingleSeatSelected` | Checks if the text "1 seat selected" (or the corresponding translation) is displayed correctly when exactly one seat is selected. |
| `displayMultipleSeatsSelected` | Checks if the text "{{count}} seats selected" (or the corresponding translation) is displayed correctly when multiple seats are selected, with the correct count. |

### ReservationCard (webapp/components/reservations/reservation-card.tsx)

| Test Case | Description |
| :--- | :--- |
| `displayViewSeatButtonSingle` | Checks if the text "View seat" (or the corresponding translation) is displayed correctly on the button when only one reservation is present. |
| `displayViewSeatButtonMultiple` | Checks if the text "View seats" (or the corresponding translation) is displayed correctly on the button when multiple reservations are present. |

### ReservationsSubPage (webapp/components/reservations/reservation-page.tsx)

| Test Case | Description |
| :--- | :--- |
| `displayNoReservationsYet` | Checks if the text "No reservations yet" (or the corresponding translation) is displayed correctly when no reservations are present. |
| `displaySwitchToAvailableEvents` | Checks if the text "Switch to available events" (or the corresponding translation) is displayed correctly when no reservations are present. |

### Translation Files (webapp/locales/de/translation.json, webapp/locales/en/translation.json)

| Test Case | Description |
| :--- | :--- |
| `verifyNewTranslationKeysExist` | Checks if all new translation keys (`viewSeatMultipleButton`, `seatSelected`, `multipleSeatsSelected`, `eventsPage.noEventsAvailable`, `eventsPage.tryAgainOrCheckSearch`, `eventsPage.noReservationsYet`, `eventsPage.switchToAvailableEvents`) are present in the German and English `translation.json` files. |
| `verifyPluralizationLogic` | Checks if the pluralization logic for `seatsSelected` and `viewSeatButton` is applied correctly in the components, based on the translation files. |

## Utils

### ReservationExporter

| Test Case | Description |
| :--- | :--- |
| `exportReservationsToPdf_createsNonEmptyPdf` | Checks if exporting a single reservation creates a non-empty PDF file. |
| `exportReservationsToPdf_multipleReservations_createsMultiPagePdf` | Checks if exporting multiple reservations creates a multi-page PDF file. |
| `exportReservationsToPdf_emptyList_createsValidPdf` | Checks if exporting an empty list of reservations creates a valid, empty PDF file. |
| `exportReservationsToCsv_createsCsvWithHeaderAndRows` | Checks if exporting a single reservation creates a CSV file with a header and one data row. |
| `exportReservationsToCsv_emptyList_createsCsvWithHeaderOnly` | Checks if exporting an empty list of reservations creates a CSV file with only the header. |
| `exportReservationsToCsv_multipleReservations_createsCsvWithMultipleRows` | Checks if exporting multiple reservations creates a CSV file with a header and multiple data rows. |
| `exportReservationsToPdf_withBlockedReservation_createsValidPdf` | Checks if a valid PDF file is created for a blocked reservation, using the correct template or default layout. |
| `exportReservationsToPdf_withMixedStatus_createsValidPdf` | Checks if a single, multi-page PDF file is correctly created for a list of reservations with mixed statuses (RESERVED and BLOCKED). |

### exportReservationsToPdf(Long eventId, User currentUser)

| Test Case | Description |
| :--- | :--- |
| `exportReservationsToPdf_Forbidden` | Attempts to export reservations as an unauthorized user. Expects `SecurityException`. |
| `exportReservationsToPdf_EventNotFound` | Attempts to export reservations for a non-existent event. Expects `EventNotFoundException`. |

## Utility Classes

### HtmlSanitizerUtils

| Test Case | Description |
| :--- | :--- |
| `sanitize_ReturnsNullWhenInputIsNull` | Checks that `sanitize()` returns `null` when the input is `null`. |
| `sanitize_ReturnsEmptyStringWhenInputIsEmpty` | Checks that `sanitize()` returns an empty string when the input is empty. |
| `sanitize_RemovesScriptTags` | Verifies that `<script>` tags and their content are removed from the input. |
| `sanitize_RemovesOnEventHandlers` | Checks that event handlers like `onclick` are removed from HTML elements. |
| `sanitize_RemovesIframeTags` | Verifies that `<iframe>` tags and their content are removed from the input. |
| `sanitize_RemovesAnchorTagsButAllowsMailtoProtocol` | Checks that anchor tags are removed but text content is preserved. |
| `sanitize_RemovesHttpLinks` | Verifies that HTTP links are removed from the input. |
| `sanitize_RemovesHttpsLinks` | Verifies that HTTPS links are removed from the input. |
| `sanitize_PreservesPlainText` | Checks that plain text without HTML is preserved as is. |
| `sanitize_HandlesMultipleXssAttempts` | Tests that multiple XSS attack vectors are properly sanitized. |
| `sanitize_RemovesStyleTags` | Verifies that `<style>` tags and their content are removed. |
| `sanitize_RemovesEmbedTags` | Checks that `<embed>` tags are removed from the input. |
| `sanitize_RemovesObjectTags` | Verifies that `<object>` tags are removed from the input. |
| `sanitize_HandlesNestedTags` | Tests that nested malicious tags are properly sanitized. |
| `sanitize_HandlesMalformedHtml` | Checks that malformed HTML is handled correctly without throwing exceptions. |
| `sanitize_RemovesJavascriptProtocol` | Verifies that `javascript:` protocol in links is removed. |
| `sanitize_RemovesDataProtocol` | Checks that `data:` protocol in links is removed. |

### SecurityUtils

| Test Case | Description |
| :--- | :--- |
| `getSecureRandom_ReturnsNonNull` | Checks that `getSecureRandom()` returns a non-null `SecureRandom` instance. |
| `getSecureRandom_ReturnsSameInstance` | Verifies that `getSecureRandom()` returns the same instance on consecutive calls (singleton pattern). |
| `generateRandomBytes_ReturnsCorrectLength` | Checks that the generated byte array has the requested length. |
| `generateRandomBytes_ReturnsNonNullArray` | Verifies that `generateRandomBytes()` returns a non-null array. |
| `generateRandomBytes_GeneratesRandomValues` | Tests that consecutive calls generate different random values. |
| `generateRandomBytes_HandlesDifferentLengths` | Checks that the method handles different array lengths correctly (1, 16, 256 bytes). |
| `generateRandomBytes_HandlesZeroLength` | Verifies that a zero-length array can be generated. |
| `nextInt_ReturnsValueWithinBound` | Checks that generated integers are within the specified bound [0, bound). |
| `nextInt_GeneratesVariedValues` | Verifies that the method generates a good distribution of random values. |
| `nextInt_HandlesSmallBound` | Tests that small bounds (e.g., 2) are handled correctly. |
| `nextLong_GeneratesDifferentValues` | Checks that consecutive calls generate different long values. |
| `nextLong_GeneratesVariedValues` | Verifies that the method generates a good distribution of random long values. |
| `constructor_ThrowsException` | Tests that the utility class constructor throws an `UnsupportedOperationException` when instantiation is attempted. |

## Initialization

### AdminUserInitializer

| Test Case | Description |
| :--- | :--- |
| `onStart_WithNoExistingAdminUser_CreatesAdminUser` | Checks that an admin user is created on application startup when no admin user exists. Verifies that the user has username "admin", email "admin@localhost", firstname "System", lastname "Admin", and a 12-character random password. |
| `onStart_WithExistingAdminUser_SkipsCreation` | Verifies that no admin user is created if one already exists in the database. |

## DTOs

### RegistrationStatusDTO

| Test Case | Description |
| :--- | :--- |
| `testConstructor_WithEnabledTrue` | Creates a `RegistrationStatusDTO` with `enabled=true` and verifies it is correctly set. |
| `testConstructor_WithEnabledFalse` | Creates a `RegistrationStatusDTO` with `enabled=false` and verifies it is correctly set. |
| `testDefaultConstructor` | Creates a `RegistrationStatusDTO` using the default constructor and verifies the object is not null. |
| `testSetEnabled_ToTrue` | Creates a `RegistrationStatusDTO` with `enabled=false`, sets it to `true`, and verifies the change. |
| `testSetEnabled_ToFalse` | Creates a `RegistrationStatusDTO` with `enabled=true`, sets it to `false`, and verifies the change. |
| `testGetEnabled` | Creates a `RegistrationStatusDTO`, sets `enabled=true`, and verifies `isEnabled()` returns the correct value. |
| `testMultipleStateChanges` | Performs multiple state changes on a `RegistrationStatusDTO` to verify that the enabled flag can be toggled correctly. |

## Exceptions

### RegistrationDisabledException

| Test Case | Description |
| :--- | :--- |
| `testExceptionCreation_WithMessage` | Creates a `RegistrationDisabledException` with a message and verifies it is stored correctly. |
| `testExceptionIsRuntimeException` | Verifies that `RegistrationDisabledException` extends `RuntimeException`. |
| `testExceptionThrowingAndCatching` | Tests throwing and catching `RegistrationDisabledException` by the specific exception type. |
| `testExceptionCatchAsRuntimeException` | Tests that `RegistrationDisabledException` can be caught as a `RuntimeException`. |
| `testExceptionWithEmptyMessage` | Creates a `RegistrationDisabledException` with an empty message and verifies it is handled correctly. |
| `testExceptionWithNullMessage` | Creates a `RegistrationDisabledException` with a null message and verifies it is handled correctly. |
| `testExceptionStackTrace` | Verifies that `RegistrationDisabledException` has a proper stack trace. |

## Global Exception Handler

### GlobalExceptionHandler

| Test Case | Description |
| :--- | :--- |
| `testRegistrationDisabledException` | Tests that `RegistrationDisabledException` is mapped to HTTP 403 Forbidden status with correct error response. |
| `testRegistrationDisabledExceptionWithDifferentMessage` | Tests that `RegistrationDisabledException` with a custom message is properly mapped to HTTP 403 Forbidden with the correct message. |
