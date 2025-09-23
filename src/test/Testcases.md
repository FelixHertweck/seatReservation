# Testfälle

Dies ist eine Übersicht der Testfälle für die Anwendung.

## Email Service

### EmailService

| Testfall | Beschreibung |
| :--- | :--- |
| `sendEmailConfirmation_Success` | Sendet erfolgreich eine E-Mail-Bestätigung an einen Benutzer. |
| `sendEmailConfirmation_IOException` | Simuliert einen Fehler beim Senden der E-Mail-Bestätigung (z.B. durch eine `IOException`). Erwartet, dass die Ausnahme korrekt behandelt wird. |
| `sendEventReminder_Success` | Sendet erfolgreich eine Event-Erinnerungs-E-Mail an einen Benutzer. |
| `sendEventReminder_IOException` | Simuliert einen Fehler beim Senden der Event-Erinnerungs-E-Mail (z.B. durch eine `IOException`). Erwartet, dass die Ausnahme korrekt behandelt wird. |
| `sendEventReservationsCsvToManager_Success` | Sendet erfolgreich eine E-Mail mit CSV-Export der Reservierungen an den Manager. |
| `sendEventReservationsCsvToManager_IOException` | Simuliert einen Fehler beim Senden der E-Mail mit CSV-Export an den Manager (z.B. durch eine `IOException`). Erwartet, dass die Ausnahme korrekt behandelt wird. |

### EmailVerificationCleanupService

| Testfall | Beschreibung |
| :--- | :--- |
| `performManualCleanup_Success` | Führt die manuelle Bereinigung erfolgreich durch und löscht abgelaufene E-Mail-Verifizierungsdatensätze. |
| `performManualCleanup_NoExpiredTokens` | Überprüft, ob keine Datensätze gelöscht werden, wenn keine abgelaufenen Token vorhanden sind. |
| `scheduledCleanup_Success` | Überprüft, ob der geplante Bereinigungsprozess erfolgreich ausgeführt wird. |

### NotificationService

| Testfall | Beschreibung |
| :--- | :--- |
| `sendEventReminders_WithEventsAndReservations_SendsEmails` | Überprüft, dass Event-Erinnerungen erfolgreich an Benutzer mit Reservierungen für morgige Events gesendet werden. |
| `sendEventReminders_WithNoEvents_DoesNotSendEmails` | Überprüft, dass keine E-Mails gesendet werden, wenn keine Events für morgen vorhanden sind. |
| `sendEventReminders_WithEventsButNoReservations_DoesNotSendEmails` | Überprüft, dass keine E-Mails gesendet werden, wenn Events existieren, aber keine Reservierungen vorhanden sind. |
| `sendEventReminders_WithMultipleEvents_ProcessesAllEvents` | Überprüft, dass alle Events für morgen verarbeitet und entsprechende Erinnerungen gesendet werden. |
| `sendEventReminders_WithMultipleReservationsPerUser_GroupsCorrectly` | Überprüft, dass mehrere Reservierungen desselben Benutzers für ein Event korrekt gruppiert werden. |
| `sendEventReminders_WithEmailException_ContinuesProcessing` | Überprüft, dass bei E-Mail-Fehlern die Verarbeitung für andere Benutzer fortgesetzt wird. |
| `sendEventReminders_WithNullUserEmail_SkipsUserGracefully` | Überprüft, dass Benutzer mit null E-Mail-Adressen korrekt übersprungen werden. |
| `sendEventReminders_CalculatesCorrectDateRange` | Überprüft, dass der korrekte Datumsbereich (morgen) für die Event-Suche verwendet wird. |
| `sendEventReminders_WithServiceException_HandlesGracefully` | Überprüft das Verhalten bei Service-Exceptions (z.B. Datenbankfehler). |
| `sendDailyReservationCsvToManagers_WithEventsAndManagers_SendsCsvEmails` | Überprüft, dass CSV-Export-E-Mails erfolgreich an Manager von Events gesendet werden, die heute stattfinden. |
| `sendDailyReservationCsvToManagers_WithNoEvents_DoesNotSendEmails` | Überprüft, dass keine CSV-Export-E-Mails gesendet werden, wenn keine Events für heute vorhanden sind. |
| `sendDailyReservationCsvToManagers_WithEventButNoManager_DoesNotSendEmail` | Überprüft, dass keine E-Mail gesendet wird, wenn ein Event keinen zugewiesenen Manager hat. |
| `sendDailyReservationCsvToManagers_WithMultipleEvents_ProcessesAllEvents` | Überprüft, dass CSV-Exports für alle heutigen Events mit unterschiedlichen Managern gesendet werden. |
| `sendDailyReservationCsvToManagers_WithEmailException_ContinuesProcessing` | Überprüft, dass bei E-Mail-Fehlern die Verarbeitung für andere Manager fortgesetzt wird. |
| `sendDailyReservationCsvToManagers_CalculatesCorrectDateRange` | Überprüft, dass der korrekte Datumsbereich (heute) für die Event-Suche verwendet wird. |
| `sendDailyReservationCsvToManagers_WithServiceException_HandlesGracefully` | Überprüft das Verhalten bei Service-Exceptions während der CSV-Export-Verarbeitung. |

## Security

### AuthService

| Testfall | Beschreibung |
| :--- | :--- |
| `authenticate_Success` | Authentifiziert einen Benutzer erfolgreich mit gültigen Anmeldeinformationen und gibt einen Token zurück. |
| `authenticate_Success_WithEmail` | Authentifiziert einen Benutzer erfolgreich mit gültigen Anmeldeinformationen (E-Mail) und gibt einen Token zurück. |
| `authenticate_AuthenticationFailedException_InvalidUsername` | Versucht, sich mit einem nicht existierenden Benutzernamen zu authentifizieren. Erwartet `AuthenticationFailedException`. |
| `authenticate_AuthenticationFailedException_InvalidPassword` | Versucht, sich mit einem gültigen Benutzernamen, aber einem falschen Passwort zu authentifizieren. Erwartet `AuthenticationFailedException`. |
| `testAuthenticateFailureEmailNotFound` | Versucht, sich mit einer nicht existierenden E-Mail-Adresse zu authentifizieren. Erwartet `AuthenticationFailedException`. |
| `testAuthenticateWithEmailWrongPassword` | Versucht, sich mit einer gültigen E-Mail-Adresse, aber einem falschen Passwort zu authentifizieren. Erwartet `AuthenticationFailedException`. |
| `testAuthenticateWithEmailIdentifier` | Überprüft, dass E-Mail-Adressen korrekt als E-Mails identifiziert werden und die E-Mail-basierte Suche verwendet wird. |
| `testAuthenticateWithEmptyPassword` | Versucht, sich mit einem leeren Passwort zu authentifizieren. Erwartet `AuthenticationFailedException`. |
| `testAuthenticateIdentifierDetection` | Testet die korrekte Erkennung von E-Mail vs. Benutzername als Identifier. |
| `testAuthenticateUsernameIdentification` | Überprüft, dass Benutzernamen ohne @-Symbol korrekt als Benutzernamen erkannt werden. |
| `testAuthenticateWithInvalidHash` | Testet das Verhalten bei ungültigem Passwort-Hash-Format. Erwartet `RuntimeException`. |
| `testAuthenticateSpecialCharactersInPassword` | Testet die Authentifizierung mit Sonderzeichen im Passwort. |

### TokenService

| Testfall | Beschreibung |
| :--- | :--- |
| `generateToken_Success` | Generiert erfolgreich einen JWT-Token für einen gegebenen Benutzer. |
| `generateToken_ValidTokenContent` | Überprüft, ob der generierte Token die korrekten Benutzerinformationen (z.B. Benutzername, Rollen) enthält. |
| `generateToken_TokenExpiration` | Überprüft, ob der generierte Token eine korrekte Ablaufzeit hat. |
| `generateToken_NullEmail_UsesEmptyString` | Überprüft, dass bei einer null E-Mail ein leerer String im Token verwendet wird. |
| `generateToken_EmptyRoles_HandlesCorrectly` | Testet das Verhalten bei einem Benutzer mit leerer Rollen-Liste. |
| `getExpirationMinutes_ReturnsConfiguredValue` | Überprüft, dass die konfigurierte Ablaufzeit korrekt zurückgegeben wird. |
| `createNewJwtCookie_ValidCookie` | Erstellt erfolgreich ein JWT-Cookie mit korrekten Eigenschaften (HttpOnly, Secure, Path, MaxAge). |
| `createNewJwtCookie_DifferentExpirationTime` | Überprüft, dass unterschiedliche Ablaufzeiten korrekt im Cookie-MaxAge widergespiegelt werden. |
| `createNewJwtCookie_EmptyToken` | Testet das Verhalten beim Erstellen eines Cookies mit leerem Token. |
| `createNewJwtCookie_NullToken` | Testet das Verhalten beim Erstellen eines Cookies mit null Token. |
| `generateToken_CustomExpirationTime` | Überprüft, dass benutzerdefinierte Ablaufzeiten (z.B. 24 Stunden) korrekt angewendet werden. |

### AuthResource

| Testfall | Beschreibung |
| :--- | :--- |
| `login_Success` | Sendet eine POST-Anfrage an `/api/auth/login` mit gültigen Anmeldeinformationen. Erwartet einen 200 OK Status und einen JWT-Cookie mit korrekter `Max-Age`. |
| `login_AuthenticationFailedException_InvalidCredentials` | Sendet eine POST-Anfrage an `/api/auth/login` mit ungültigen Anmeldeinformationen. Erwartet einen 401 Unauthorized Status. |
| `login_BadRequest_MissingCredentials` | Sendet eine POST-Anfrage an `/api/auth/login` ohne Benutzernamen oder Passwort. Erwartet einen 400 Bad Request Status. |
| `register_Success` | Sendet eine POST-Anfrage an `/api/auth/register` mit gültigen Registrierungsdaten. Erwartet einen 201 Created Status. |
| `register_Failure_DuplicateUsername` | Sendet eine POST-Anfrage an `/api/auth/register` mit einem bereits existierenden Benutzernamen. Erwartet einen 409 Conflict Status. |
| `register_Failure_InvalidData` | Sendet eine POST-Anfrage an `/api/auth/register` mit ungültigen Daten (z.B. fehlender Benutzername, zu kurzes Passwort). Erwartet einen 400 Bad Request Status. |

## UserService

### createUser(UserCreationDTO userCreationDTO)

| Testfall | Beschreibung |
| :--- | :--- |
| `createUser_Success_WithEmail` | Erstellt einen neuen Benutzer mit gültigen Daten (Benutzername, Passwort, E-Mail, Vorname, Nachname). Überprüft, ob der Benutzer erfolgreich in der Datenbank gespeichert wird und eine E-Mail-Bestätigung gesendet wird. |
| `createUser_Success_WithoutEmail` | Erstellt einen neuen Benutzer mit gültigen Daten (Benutzername, Passwort, Vorname, Nachname), aber ohne E-Mail. Überprüft, ob der Benutzer erfolgreich in der Datenbank gespeichert wird und keine E-Mail-Bestätigung gesendet wird. |
| `createUser_InvalidUserException_NullDTO` | Versucht, einen Benutzer mit einem `null` `UserCreationDTO` zu erstellen. Erwartet `InvalidUserException`. |
| `createUser_InvalidUserException_EmptyUsername` | Versucht, einen Benutzer mit einem leeren oder nur aus Leerzeichen bestehenden Benutzernamen zu erstellen. Erwartet `InvalidUserException`. |
| `createUser_InvalidUserException_EmptyPassword` | Versucht, einen Benutzer mit einem leeren oder nur aus Leerzeichen bestehenden Passwort zu erstellen. Erwartet `InvalidUserException`. |
| `createUser_DuplicateUserException_ExistingUsername` | Versucht, einen Benutzer mit einem Benutzernamen zu erstellen, der bereits in der Datenbank existiert. Erwartet `DuplicateUserException`. |
| `createUser_Success_WithDuplicateEmail` | Erstellt einen neuen Benutzer mit einer E-Mail-Adresse, die bereits von einem anderen Benutzer verwendet wird. Überprüft, ob der Benutzer erfolgreich erstellt wird und keine `DuplicateUserException` geworfen wird. |
| `createUser_InternalServerErrorException_EmailSendFailure` | Simuliert einen Fehler beim Senden der E-Mail-Bestätigung (z.B. durch eine `IOException` im `EmailService`). Erwartet `InternalServerErrorException`. |
| `createUser_InternalServerErrorException_EmailSendFailure` | Simuliert einen Fehler beim Senden der E-Mail-Bestätigung (z.B. durch eine `IOException` im `EmailService`). Erwartet `InternalServerErrorException`. |

### importUsers(Set<AdminUserCreationDto> adminUserCreationDtos)

| Testfall | Beschreibung |
| :--- | :--- |
| `importUsers_Success` | Importiert erfolgreich mehrere Benutzer. |
| `importUsers_EmptySet` | Versucht, ein leeres Set von Benutzern zu importieren. Erwartet eine leere Liste importierter Benutzer. |
| `importUsers_InvalidUserException` | Versucht, Benutzer mit ungültigen Daten zu importieren (z.B. leerer Benutzername). Erwartet `InvalidUserException`. |
| `importUsers_DuplicateUserException` | Versucht, Benutzer zu importieren, von denen einer bereits existiert. Erwartet `DuplicateUserException`. |
| `importUsers_EmailSendFailure` | Simuliert einen Fehler beim E-Mail-Versand während des Imports. Erwartet `RuntimeException`. |

### updateUser(Long id, AdminUserUpdateDTO user)

| Testfall | Beschreibung |
| :--- | :--- |
| `updateUser_Success_UpdateFirstname` | Aktualisiert erfolgreich den Vornamen eines bestehenden Benutzers (Admin-Funktion). |
| `updateUser_Success_UpdateLastname` | Aktualisiert erfolgreich den Nachnamen eines bestehenden Benutzers (Admin-Funktion). |
| `updateUser_Success_UpdatePassword` | Aktualisiert erfolgreich das Passwort eines bestehenden Benutzers (Admin-Funktion). |
| `updateUser_Success_UpdateRoles` | Aktualisiert erfolgreich die Rollen eines bestehenden Benutzers (Admin-Funktion). |
| `updateUser_Success_NoEmailChange` | Aktualisiert erfolgreich andere Felder eines bestehenden Benutzers (z.B. Vorname, Nachname, Passwort, Rollen), ohne die E-Mail-Adresse zu ändern (Admin-Funktion). Überprüft, ob keine E-Mail-Bestätigung gesendet wird. |
| `updateUser_Success_UpdateEmail` | Aktualisiert erfolgreich die E-Mail-Adresse eines bestehenden Benutzers und überprüft, ob die E-Mail-Verifizierung zurückgesetzt und eine neue Bestätigungs-E-Mail gesendet wird (Admin-Funktion). |
| `updateUser_UserNotFoundException` | Versucht, einen nicht existierenden Benutzer zu aktualisieren (Admin-Funktion). Erwartet `UserNotFoundException`. |
| `updateUser_InvalidUserException_NullDTO` | Versucht, einen Benutzer mit einem `null` `AdminUserUpdateDTO` zu aktualisieren (Admin-Funktion). Erwartet `InvalidUserException`. |
| `updateUser_Success_WithDuplicateEmail` | Aktualisiert die E-Mail-Adresse eines Benutzers auf eine bereits existierende E-Mail-Adresse (Admin-Funktion). Überprüft, ob die Aktualisierung erfolgreich ist und keine `DuplicateUserException` geworfen wird. |
| `updateUser_InternalServerErrorException_EmailSendFailure` | Simuliert einen Fehler beim Senden der E-Mail-Bestätigung nach einer E-Mail-Änderung (Admin-Funktion). Erwartet `InternalServerErrorException`. |

### deleteUser(Long id)

| Testfall | Beschreibung |
| :--- | :--- |
| `deleteUser_Success` | Löscht erfolgreich einen bestehenden Benutzer anhand seiner ID. |
| `deleteUser_UserNotFoundException` | Versucht, einen nicht existierenden Benutzer zu löschen. Erwartet `UserNotFoundException`. |

### getUserById(Long id)

| Testfall | Beschreibung |
| :--- | :--- |
| `getUserById_Success` | Ruft erfolgreich einen bestehenden Benutzer anhand seiner ID ab. |
| `getUserById_UserNotFoundException` | Versucht, einen nicht existierenden Benutzer abzurufen. Erwartet `UserNotFoundException`. |

### getAllUsers()

| Testfall | Beschreibung |
| :--- | :--- |
| `getAllUsers_Success_WithUsers` | Ruft erfolgreich eine Liste aller Benutzer ab, wenn Benutzer vorhanden sind. |
| `getAllUsers_Success_NoUsers` | Ruft erfolgreich eine leere Liste ab, wenn keine Benutzer vorhanden sind. |

### getAvailableRoles()

| Testfall | Beschreibung |
| :--- | :--- |
| `getAvailableRoles_Success` | Ruft erfolgreich eine Liste aller verfügbaren Rollen ab. |


### updateUserProfile(String username, UserProfileUpdateDTO userProfileUpdateDTO)

| Testfall | Beschreibung |
| :--- | :--- |
| `updateUserProfile_Success_UpdateFirstname` | Aktualisiert erfolgreich den Vornamen eines bestehenden Benutzers über seinen Benutzernamen. |
| `updateUserProfile_Success_UpdateLastname` | Aktualisiert erfolgreich den Nachnamen eines bestehenden Benutzers über seinen Benutzernamen. |
| `updateUserProfile_Success_UpdatePassword` | Aktualisiert erfolgreich das Passwort eines bestehenden Benutzers über seinen Benutzernamen. |
| `updateUserProfile_Success_PasswordSaltChangesOnPasswordUpdate` | Überprüft, ob sich das Salt beim Aktualisieren des Passworts eines bestehenden Benutzers über seinen Benutzernamen ändert. |
| `updateUserProfile_Success_UpdateEmail` | Aktualisiert erfolgreich die E-Mail-Adresse eines bestehenden Benutzers über seinen Benutzernamen und überprüft, ob die E-Mail-Verifizierung zurückgesetzt und eine neue Bestätigungs-E-Mail gesendet wird. |
| `updateUserProfile_DoesNotUpdateRoles` | Stellt sicher, dass ein Versuch, die eigenen Rollen über diesen Endpunkt zu aktualisieren, ignoriert wird. |
| `updateUserProfile_UserNotFoundException` | Versucht, das Profil eines nicht existierenden Benutzers zu aktualisieren. Erwartet `UserNotFoundException`. |
| `updateUserProfile_InvalidUserException_NullDTO` | Versucht, ein Benutzerprofil mit einem `null` `UserProfileUpdateDTO` zu aktualisieren. Erwartet `InvalidUserException`. |
| `updateUserProfile_Success_WithDuplicateEmail` | Aktualisiert die E-Mail-Adresse eines Benutzerprofils auf eine bereits existierende E-Mail-Adresse. Überprüft, ob die Aktualisierung erfolgreich ist und keine `DuplicateUserException` geworfen wird. |
| `updateUserProfile_InternalServerErrorException_EmailSendFailure` | Simuliert einen Fehler beim Senden der E-Mail-Bestätigung nach einer E-Mail-Änderung. Erwartet `InternalServerErrorException`. |

### verifyEmailWithCode(String verificationCode)

| Testfall | Beschreibung |
| :--- | :--- |
| `verifyEmailWithCode_Success` | Verifiziert erfolgreich eine E-Mail-Adresse mit einem gültigen 6-stelligen Verifizierungscode. Überprüft, ob der E-Mail-Verifizierungsdatensatz gelöscht und der Benutzer als "E-Mail verifiziert" markiert wird. |
| `verifyEmailWithCode_BadRequestException_NullCode` | Versucht, die E-Mail mit einem `null` Verifizierungscode zu verifizieren. Erwartet `IllegalArgumentException`. |
| `verifyEmailWithCode_BadRequestException_EmptyCode` | Versucht, die E-Mail mit einem leeren Verifizierungscode zu verifizieren. Erwartet `IllegalArgumentException`. |
| `verifyEmailWithCode_BadRequestException_InvalidFormat` | Versucht, die E-Mail mit einem Verifizierungscode zu verifizieren, der nicht dem 6-stelligen Zahlenformat entspricht. Erwartet `IllegalArgumentException`. |
| `verifyEmailWithCode_BadRequestException_CodeNotFound` | Versucht, die E-Mail mit einem Verifizierungscode zu verifizieren, für den kein Verifizierungsdatensatz existiert. Erwartet `IllegalArgumentException`. |
| `verifyEmailWithCode_TokenExpiredException` | Versucht, die E-Mail mit einem abgelaufenen Verifizierungscode zu verifizieren. Erwartet `TokenExpiredException`. |
| `verifyEmailWithCode_FailsWithUsedCode` | Stellt sicher, dass ein bereits verwendeter E-Mail-Verifizierungscode nicht erneut verwendet werden kann. |

## EventService

### createEvent(EventRequestDTO dto, User manager)

| Testfall | Beschreibung |
| :--- | :--- |
| `createEvent_Success` | Erstellt erfolgreich ein neues Event mit gültigen Daten und einem Manager. |
| `createEvent_IllegalArgumentException_LocationNotFound` | Versucht, ein Event mit einer nicht existierenden EventLocation-ID zu erstellen. Erwartet `IllegalArgumentException`. |

### updateEvent(Long id, EventRequestDTO dto, User manager)

| Testfall | Beschreibung |
| :--- | :--- |
| `updateEvent_Success_AsManager` | Aktualisiert erfolgreich ein bestehendes Event als Manager des Events. |
| `updateEvent_Success_AsAdmin` | Aktualisiert erfolgreich ein bestehendes Event als Administrator. |
| `updateEvent_EventNotFoundException` | Versucht, ein nicht existierendes Event zu aktualisieren. Erwartet `EventNotFoundException`. |
| `updateEvent_ForbiddenException_NotManagerOrAdmin` | Versucht, ein Event als Benutzer zu aktualisieren, der weder Manager noch Administrator ist. Erwartet `ForbiddenException`. |
| `updateEvent_IllegalArgumentException_LocationNotFound` | Versucht, ein Event mit einer nicht existierenden EventLocation-ID zu aktualisieren. Erwartet `IllegalArgumentException`. |

### getEventsByCurrentManager(User manager)

| Testfall | Beschreibung |
| :--- | :--- |
| `getEventsByCurrentManager_Success_AsAdmin` | Ruft alle Events als Administrator ab. |
| `getEventsByCurrentManager_Success_AsManager` | Ruft Events ab, die dem aktuellen Manager gehören. |
| `getEventsByCurrentManager_Success_NoEventsForManager` | Ruft eine leere Liste ab, wenn der Manager keine Events verwaltet. |

### setReservationsAllowedForUser(EventUserAllowancesDto dto, User manager)

| Testfall | Beschreibung |
| :--- | :--- |
| `setReservationsAllowedForUser_Success_NewAllowance` | Setzt erfolgreich die erlaubte Anzahl von Reservierungen für einen Benutzer für ein Event (neuer Eintrag). |
| `setReservationsAllowedForUser_Success_UpdateAllowance` | Aktualisiert erfolgreich die erlaubte Anzahl von Reservierungen für einen Benutzer für ein Event (bestehender Eintrag). |
| `setReservationsAllowedForUser_EventNotFoundException` | Versucht, die Reservierungserlaubnis für ein nicht existierendes Event zu setzen. Erwartet `EventNotFoundException`. |
| `setReservationsAllowedForUser_UserNotFoundException` | Versucht, die Reservierungserlaubnis für einen nicht existierenden Benutzer zu setzen. Erwartet `UserNotFoundException`. |
| `setReservationsAllowedForUser_ForbiddenException_NotManagerOrAdmin` | Versucht, die Reservierungserlaubnis als Benutzer zu setzen, der weder Manager noch Administrator des Events ist. Erwartet `ForbiddenException`. |
| `setReservationsAllowedForUser_Success_AsAdmin` | Setzt erfolgreich die erlaubte Anzahl von Reservierungen für einen Benutzer für ein Event durch einen Administrator und überprüft die korrekte Persistierung der Daten. |

### updateReservationAllowance(EventUserAllowanceUpdateDto dto, User manager)

| Testfall | Beschreibung |
| :--- | :--- |
| `updateReservationAllowance_Success_AsManager` | Aktualisiert erfolgreich eine bestehende Reservierungsberechtigung als Manager des Events. |
| `updateReservationAllowance_Success_AsAdmin` | Aktualisiert erfolgreich eine bestehende Reservierungsberechtigung als Administrator. |
| `updateReservationAllowance_EventNotFoundException_AllowanceNotFound` | Versucht, eine nicht existierende Reservierungsberechtigung zu aktualisieren. Erwartet `EventNotFoundException`. |
| `updateReservationAllowance_SecurityException_NotManagerOrAdmin` | Versucht, eine Reservierungsberechtigung als Benutzer zu aktualisieren, der weder Manager noch Administrator des Events ist. Erwartet `SecurityException`. |

### getReservationAllowanceById(Long id, User manager)

| Testfall | Beschreibung |
| :--- | :--- |
| `getReservationAllowanceById_Success_AsManager` | Ruft erfolgreich eine `EventUserAllowance` als der zuständige Manager ab. |
| `getReservationAllowanceById_Success_AsAdmin` | Ruft erfolgreich eine `EventUserAllowance` als Administrator ab. |
| `getReservationAllowanceById_ForbiddenException_NotManagerOrAdmin` | Versucht, eine `EventUserAllowance` als nicht autorisierter Benutzer abzurufen. Erwartet `SecurityException`. |
| `getReservationAllowanceById_EventNotFoundException` | Versucht, eine nicht existierende `EventUserAllowance` abzurufen. Erwartet `EventNotFoundException`. |

## ReservationService

### findAllReservations(User currentUser)

| Testfall | Beschreibung |
| :--- | :--- |
| `findAllReservations_Success_AsAdmin` | Ruft alle Reservierungen als Administrator ab. |
| `findAllReservations_Success_AsManager` | Ruft Reservierungen für Events ab, die der Manager verwalten darf. |
| `findAllReservations_Success_NoAllowedEventsForManager` | Ruft eine leere Liste ab, wenn der Manager keine Events verwalten darf. |
| `findAllReservations_ForbiddenException_OtherRoles` | Versucht, Reservierungen als Benutzer mit einer anderen Rolle abzurufen. Erwartet `ForbiddenException`. |

### findReservationById(Long id, User currentUser)

| Testfall | Beschreibung |
| :--- | :--- |
| `findReservationById_Success_AsAdmin` | Ruft eine Reservierung als Administrator ab. |
| `findReservationById_Success_AsManager` | Ruft eine Reservierung ab, die zu einem Event gehört, das der Manager verwalten darf. |
| `findReservationById_NotFoundException` | Versucht, eine nicht existierende Reservierung abzurufen. Erwartet `NotFoundException`. |
| `findReservationById_ForbiddenException_NotAllowed` | Versucht, eine Reservierung abzurufen, für die der Benutzer keine Berechtigung hat. Erwartet `ForbiddenException`. |

### createReservation(ReservationRequestDTO dto, User currentUser)

| Testfall | Beschreibung |
| :--- | :--- |
| `createReservation_Success_AsAdmin` | Erstellt erfolgreich eine Reservierung als Administrator. |
| `createReservation_Success_AsManager` | Erstellt erfolgreich eine Reservierung als Manager für ein Event, das er verwalten darf. |
| `createReservation_UserNotFoundException_TargetUser` | Versucht, eine Reservierung für einen nicht existierenden Zielbenutzer zu erstellen. Erwartet `UserNotFoundException`. |
| `createReservation_NotFoundException_EventNotFound` | Versucht, eine Reservierung für ein nicht existierendes Event zu erstellen. Erwartet `NotFoundException`. |
| `createReservation_ForbiddenException_NotAllowed` | Versucht, eine Reservierung als Benutzer zu erstellen, der keine Berechtigung hat. Erwartet `ForbiddenException`. |
| `createReservation_NotFoundException_SeatNotFound` | Versucht, eine Reservierung für einen nicht existierenden Sitzplatz zu erstellen. Erwartet `NotFoundException`. |
| `createReservation_BadRequestException_NoAllowance` | Versucht, eine Reservierung zu erstellen, wenn der Benutzer keine Reservierungserlaubnis für das Event hat. Erwartet `BadRequestException`. |
| `createReservation_BadRequestException_AllowanceZero` | Versucht, eine Reservierung zu erstellen, wenn die Reservierungserlaubnis des Benutzers 0 ist. Erwartet `BadRequestException`. |
| `createReservation_Forbidden_AsUser` | Versucht, eine Reservierung als normaler Benutzer zu erstellen, was fehlschlagen sollte. |

### updateReservation(Long id, ReservationRequestDTO dto, User currentUser)

| Testfall | Beschreibung |
| :--- | :--- |
| `updateReservation_Success_AsAdmin` | Aktualisiert erfolgreich eine Reservierung als Administrator. |
| `updateReservation_Success_AsManager` | Aktualisiert erfolgreich eine Reservierung als Manager für ein Event, das er verwalten darf. |
| `updateReservation_NotFoundException_ReservationNotFound` | Versucht, eine nicht existierende Reservierung zu aktualisieren. Erwartet `NotFoundException`. |
| `updateReservation_ForbiddenException_NotAllowed` | Versucht, eine Reservierung als Benutzer zu aktualisieren, der keine Berechtigung hat. Erwartet `ForbiddenException`. |
| `updateReservation_NotFoundException_NewEventNotFound` | Versucht, eine Reservierung auf ein nicht existierendes neues Event zu aktualisieren. Erwartet `NotFoundException`. |
| `updateReservation_ForbiddenException_NewEventNotAllowed` | Versucht, eine Reservierung auf ein neues Event zu aktualisieren, für das der Manager keine Berechtigung hat. Erwartet `ForbiddenException`. |
| `updateReservation_NotFoundException_UserNotFound` | Versucht, eine Reservierung mit einem nicht existierenden Benutzer zu aktualisieren. Erwartet `NotFoundException`. |
| `updateReservation_NotFoundException_SeatNotFound` | Versucht, eine Reservierung mit einem nicht existierenden Sitzplatz zu aktualisieren. Erwartet `NotFoundException`. |

### deleteReservation(Long id, User currentUser)

| Testfall | Beschreibung |
| :--- | :--- |
| `deleteReservation_Success_AsAdmin` | Löscht erfolgreich eine Reservierung als Administrator. |
| `deleteReservation_Success_AsManager` | Löscht erfolgreich eine Reservierung als Manager für ein Event, das er verwalten darf. |
| `deleteReservation_NotFoundException` | Versucht, eine nicht existierende Reservierung zu löschen. Erwartet `NotFoundException`. |
| `deleteReservation_ForbiddenException_NotAllowed` | Versucht, eine Reservierung als Benutzer zu löschen, der keine Berechtigung hat. Erwartet `ForbiddenException`. |
| `blockSeats_Success` | Blockiert erfolgreich Sitze für ein Event als Manager. |
| `blockSeats_Forbidden` | Versucht, Sitze als nicht autorisierter Benutzer zu blockieren. Erwartet `SecurityException`. |
| `blockSeats_SeatAlreadyReserved` | Versucht, bereits reservierte oder blockierte Sitze zu blockieren. Erwartet `IllegalStateException`. |

## EventService (reservation package)

### getEventsForCurrentUser(String username)

| Testfall | Beschreibung |
| :--- | :--- |
| `getEventsForCurrentUser_Success` | Ruft erfolgreich Events für den aktuellen Benutzer ab, basierend auf seinen EventUserAllowances. Die Antwort enthält die Anzahl der erlaubten Reservierungen. |
| `getEventsForCurrentUser_UserNotFoundException` | Versucht, Events für einen nicht existierenden Benutzer abzurufen. Erwartet `UserNotFoundException`. |
| `getEventsForCurrentUser_Success_NoEvents` | Ruft eine leere Liste ab, wenn der Benutzer keine EventUserAllowances hat. |

## ReservationService (reservation package)

### findReservationsByUser(User currentUser)

| Testfall | Beschreibung |
| :--- | :--- |
| `findReservationsByUser_Success` | Ruft erfolgreich alle Reservierungen für den aktuellen Benutzer ab. |
| `findReservationsByUser_Success_NoReservations` | Ruft eine leere Liste ab, wenn der Benutzer keine Reservierungen hat. |

### findReservationByIdForUser(Long id, User currentUser)

| Testfall | Beschreibung |
| :--- | :--- |
| `findReservationByIdForUser_Success` | Ruft erfolgreich eine Reservierung für den aktuellen Benutzer anhand der ID ab. |
| `findReservationByIdForUser_NotFoundException` | Versucht, eine nicht existierende Reservierung abzurufen. Erwartet `NotFoundException`. |
| `findReservationByIdForUser_ForbiddenException` | Versucht, eine Reservierung abzurufen, die nicht dem aktuellen Benutzer gehört. Erwartet `ForbiddenException`. |

### createReservationForUser(UserReservationsRequestDTO dto, User currentUser)

| Testfall | Beschreibung |
| :--- | :--- |
| `createReservationForUser_Success` | Erstellt erfolgreich eine oder mehrere Reservierungen für den aktuellen Benutzer. |
| `createReservationForUser_NotFoundException_EventNotFound` | Versucht, eine Reservierung für ein nicht existierendes Event zu erstellen. Erwartet `NotFoundException`. |
| `createReservationForUser_NotFoundException_SeatNotFound` | Versucht, eine Reservierung für einen oder mehrere nicht existierende Sitzplätze zu erstellen. Erwartet `NotFoundException`. |
| `createReservationForUser_ForbiddenException_NoAllowance` | Versucht, eine Reservierung zu erstellen, wenn der Benutzer keine Reservierungserlaubnis für das Event hat. Erwartet `ForbiddenException`. |
| `createReservationForUser_NoSeatsAvailableException_LimitReached` | Versucht, mehr Reservierungen zu erstellen, als die erlaubte Anzahl für den Benutzer. Erwartet `NoSeatsAvailableException`. |
| `createReservationForUser_EventBookingClosedException` | Versucht, eine Reservierung für ein Event zu erstellen, dessen Buchungsfrist abgelaufen ist. Erwartet `EventBookingClosedException`. |
| `createReservationForUser_SeatAlreadyReservedException` | Versucht, eine Reservierung für einen bereits reservierten Sitzplatz zu erstellen. Erwartet `SeatAlreadyReservedException`. |
| `createReservationForUser_IllegalArgumentException_NoSeatIds` | Versucht, eine Reservierung ohne Angabe von Sitzplatz-IDs zu erstellen. |
| `createReservationForUser_IllegalStateException_EmailNotVerified` | Versucht, eine Reservierung mit einem Benutzer zu erstellen, dessen E-Mail-Adresse nicht verifiziert ist. Erwartet `IllegalStateException`. |

### deleteReservationForUser(Long id, User currentUser)

| Testfall | Beschreibung |
| :--- | :--- |
| `deleteReservationForUser_Success` | Löscht erfolgreich eine Reservierung für den aktuellen Benutzer. |
| `deleteReservationForUser_NotFoundException` | Versucht, eine nicht existierende Reservierung zu löschen. Erwartet `NotFoundException`. |
| `deleteReservationForUser_ForbiddenException_NotOwner` | Versucht, eine Reservierung zu löschen, die nicht dem aktuellen Benutzer gehört. Erwartet `ForbiddenException`. |
| `deleteReservationForUser_ForbiddenException_NoAllowance` | Versucht, eine Reservierung zu löschen, wenn der Benutzer keine Reservierungserlaubnis für das Event hat (obwohl er der Besitzer der Reservierung ist). Erwartet `ForbiddenException`. |

## EventLocation Service

### EventLocationService

| Testfall | Beschreibung |
| :--- | :--- |
| `getEventLocationsByCurrentManager_Success_AsAdmin` | Ruft alle EventLocations als Administrator ab. |
| `getEventLocationsByCurrentManager_Success_AsManager` | Ruft EventLocations ab, die dem aktuellen Manager gehören. |
| `getEventLocationsByCurrentManager_Success_NoEventLocationsForManager` | Ruft eine leere Liste ab, wenn der Manager keine EventLocations verwaltet. |
| `createEventLocation_Success` | Erstellt erfolgreich eine neue EventLocation mit gültigen Daten. |
| `createEventLocation_InvalidInput` | Versucht, eine EventLocation mit ungültigen Daten zu erstellen (z.B. leere Felder). |
| `createEventLocation_InvalidInput_NegativeCapacity` | Versucht, eine EventLocation mit negativer Kapazität zu erstellen. |
| `updateEventLocation_Success_AsManager` | Aktualisiert erfolgreich eine bestehende EventLocation als Besitzer. |
| `updateEventLocation_Success_AsAdmin` | Aktualisiert erfolgreich eine bestehende EventLocation als Administrator. |
| `updateEventLocation_NotFound` | Versucht, eine nicht existierende EventLocation zu aktualisieren. |
| `updateEventLocation_ForbiddenException_NotManagerOrAdmin` | Versucht, eine EventLocation zu aktualisieren, ohne die erforderlichen Berechtigungen zu haben. |
| `deleteEventLocation_Success_AsManager` | Löscht erfolgreich eine bestehende EventLocation als Besitzer. |
| `deleteEventLocation_Success_AsAdmin` | Löscht erfolgreich eine bestehende EventLocation als Administrator. |
| `deleteEventLocation_NotFound` | Versucht, eine nicht existierende EventLocation zu löschen. |
| `deleteEventLocation_ForbiddenException_NotManagerOrAdmin` | Versucht, eine EventLocation zu löschen, ohne die erforderlichen Berechtigungen zu haben. |
| `importEventLocation_Success` | Erstellt erfolgreich eine neue EventLocation mit einer Liste von Sitzplätzen. |
| `importSeatsToEventLocation_Success` | Importiert erfolgreich Sitze zu einer bestehenden EventLocation als Manager. |
| `importSeatsToEventLocation_Success_AsAdmin` | Importiert erfolgreich Sitze zu einer bestehenden EventLocation als Administrator. |
| `importSeatsToEventLocation_NotFound` | Versucht, Sitze zu einer nicht existierenden EventLocation zu importieren. |
| `importSeatsToEventLocation_Forbidden` | Versucht, Sitze zu einer EventLocation zu importieren, für die keine Berechtigung besteht. |
| `createEventLocation_WithMarkers_Success` | Erstellt erfolgreich eine neue EventLocation mit Markern. |
| `createEventLocation_WithNullMarkers_Success` | Erstellt erfolgreich eine neue EventLocation mit null-Marker-Liste. |
| `createEventLocation_WithEmptyMarkers_Success` | Erstellt erfolgreich eine neue EventLocation mit leerer Marker-Liste. |
| `updateEventLocation_WithMarkers_Success` | Aktualisiert erfolgreich eine bestehende EventLocation mit neuen Markern. |
| `updateEventLocation_ClearingMarkers_Success` | Aktualisiert erfolgreich eine bestehende EventLocation und löscht alle Marker. |
| `convertToMarkerEntities_ValidInput` | Testet die Konvertierung von Marker-DTOs zu Entitäten mit verschiedenen Grenzwerten. |

## EventLocationMarker Tests

### EventLocationMarker

| Testfall | Beschreibung |
| :--- | :--- |
| `testDefaultConstructor` | Überprüft die korrekte Initialisierung mit dem Default-Konstruktor. |
| `testParameterizedConstructor` | Überprüft die korrekte Initialisierung mit dem parametrisierten Konstruktor. |
| `testSettersAndGetters` | Testet alle Setter und Getter Methoden. |
| `testEquals_SameObject` | Überprüft equals() mit demselben Objekt. |
| `testEquals_EqualObjects` | Überprüft equals() mit identischen Objekten. |
| `testEquals_DifferentLabel` | Überprüft equals() mit unterschiedlichen Labels. |
| `testEquals_DifferentXCoordinate` | Überprüft equals() mit unterschiedlichen X-Koordinaten. |
| `testEquals_DifferentYCoordinate` | Überprüft equals() mit unterschiedlichen Y-Koordinaten. |
| `testEquals_NullObject` | Überprüft equals() mit null-Objekt. |
| `testEquals_DifferentClass` | Überprüft equals() mit anderem Klassentyp. |
| `testEquals_NullValues` | Überprüft equals() mit null-Werten in beiden Objekten. |
| `testEquals_MixedNullValues` | Überprüft equals() mit gemischten null-Werten. |
| `testHashCode_EqualObjects` | Überprüft konsistente hashCode() Werte für gleiche Objekte. |
| `testHashCode_DifferentObjects` | Überprüft unterschiedliche hashCode() Werte für unterschiedliche Objekte. |
| `testHashCode_NullValues` | Überprüft hashCode() mit null-Werten. |
| `testToString` | Überprüft die toString() Ausgabe mit normalen Werten. |
| `testToString_NullValues` | Überprüft die toString() Ausgabe mit null-Werten. |
| `testCoordinatesBoundaries` | Testet Grenzwerte für Koordinaten (Integer.MAX_VALUE, Integer.MIN_VALUE). |
| `testEmptyLabel` | Testet Verhalten mit leerem Label. |

### EventLocationMakerRequestDTO

| Testfall | Beschreibung |
| :--- | :--- |
| `testDefaultConstructor` | Überprüft die korrekte Initialisierung mit dem Default-Konstruktor. |
| `testParameterizedConstructor` | Überprüft die korrekte Initialisierung mit dem parametrisierten Konstruktor. |
| `testSettersAndGetters` | Testet alle Setter und Getter Methoden. |
| `testSettersWithNullValues` | Testet Setter mit null-Werten. |
| `testWithBoundaryValues` | Testet Grenzwerte für Koordinaten. |
| `testWithZeroCoordinates` | Testet Verhalten mit Null-Koordinaten. |
| `testWithNegativeCoordinates` | Testet Verhalten mit negativen Koordinaten. |
| `testLongLabel` | Testet Verhalten mit sehr langem Label. |
| `testSetterChaining` | Testet unabhängiges Funktionieren der Setter. |
| `testOverwriteValues` | Testet das Überschreiben von Werten. |

### EventLocationMakerDTO

| Testfall | Beschreibung |
| :--- | :--- |
| `testConstructorWithMarkerEntity` | Überprüft Konstruktor mit EventLocationMarker-Entität. |
| `testDirectConstructor` | Überprüft direkten Konstruktor mit Parametern. |
| `testWithZeroCoordinates` | Testet Verhalten mit Null-Koordinaten. |
| `testWithNegativeCoordinates` | Testet Verhalten mit negativen Koordinaten. |
| `testWithBoundaryValues` | Testet Grenzwerte für Koordinaten. |
| `testWithNullLabel` | Testet Verhalten mit null-Label. |
| `testWithEmptyLabel` | Testet Verhalten mit leerem Label. |
| `testWithLongLabel` | Testet Verhalten mit sehr langem Label. |
| `testRecordEquality` | Testet Record-Gleichheit und Unterschiede. |
| `testRecordHashCode` | Testet Record-HashCode-Konsistenz. |
| `testRecordToString` | Testet Record-toString-Methode. |
| `testConversionConsistency` | Testet Konsistenz bei Konvertierung von Entity zu DTO. |
| `testNullCoordinatesInEntity` | Testet Verhalten bei null-Koordinaten in Entity (NullPointerException erwartet). |

## Seat Service

### SeatService

| Testfall | Beschreibung |
| :--- | :--- |
| `createSeat_Success` | Erstellt erfolgreich einen neuen Sitzplatz mit gültigen Daten. |
| `createSeat_Success_AsManager` | Erstellt erfolgreich einen neuen Sitzplatz als Manager. |
| `createSeat_Success_AsAdmin` | Erstellt erfolgreich einen neuen Sitzplatz als Admin. |
| `createSeat_ForbiddenException_NotManagerOfLocation` | Versucht, einen Sitzplatz für eine Location zu erstellen, die einem nicht gehört. |
| `createSeat_InvalidInput` | Versucht, einen Sitzplatz mit ungültigen Daten zu erstellen. |
| `findAllSeatsForManager_Success_AsAdmin` | Ruft alle Sitzplätze als Administrator ab. |
| `findAllSeatsForManager_Success_AsManager` | Ruft Sitzplätze ab, die dem aktuellen Manager gehören. |
| `findAllSeatsForManager_Success_NoSeatsForManager` | Ruft eine leere Liste ab, wenn der Manager keine Sitzplätze verwaltet. |
| `findSeatByIdForManager_Success_AsAdmin` | Ruft einen Sitzplatz als Administrator ab. |
| `findSeatByIdForManager_Success_AsManager` | Ruft einen Sitzplatz ab, der dem aktuellen Manager gehört. |
| `findSeatByIdForManager_NotFound` | Versucht, einen nicht existierenden Sitzplatz abzurufen. |
| `findSeatByIdForManager_ForbiddenException` | Versucht, einen Sitzplatz abzurufen, für den keine Berechtigung besteht. |
| `updateSeat_Success_AsManager` | Aktualisiert erfolgreich einen bestehenden Sitzplatz als Manager. |
| `updateSeat_Success_AsAdmin` | Aktualisiert erfolgreich einen bestehenden Sitzplatz als Admin. |
| `updateSeat_NotFound` | Versucht, einen nicht existierenden Sitzplatz zu aktualisieren. |
| `updateSeat_InvalidInput` | Versucht, einen Sitzplatz mit ungültigen Daten zu aktualisieren. |
| `updateSeat_ForbiddenException_NotManagerOfSeatLocation` | Versucht, einen Sitzplatz zu aktualisieren, der zu einer fremden Location gehört. |
| `updateSeat_ForbiddenException_NotManagerOfNewLocation` | Versucht, einen Sitzplatz zu einer fremden Location zu verschieben. |
| `deleteSeat_Success_AsManager` | Löscht erfolgreich einen bestehenden Sitzplatz als Manager. |
| `deleteSeat_Success_AsAdmin` | Löscht erfolgreich einen bestehenden Sitzplatz als Admin. |
| `deleteSeat_NotFound` | Versucht, einen nicht existierenden Sitzplatz zu löschen. |
| `deleteSeat_ForbiddenException_NotManager` | Versucht, einen Sitzplatz zu löschen, für den keine Berechtigung besteht. |
| `findSeatEntityById_Success` | Ruft eine Sitzplatz-Entität erfolgreich ab. |
| `findSeatEntityById_ForbiddenException` | Versucht, eine Sitzplatz-Entität abzurufen, für die der Benutzer keine Berechtigung hat. |

## GlobalExceptionHandler

### GlobalExceptionHandler

| Testfall | Beschreibung |
| :--- | :--- |
| `testUserNotFoundException` | Testet die Behandlung von `UserNotFoundException` und erwartet HTTP-Status 404 (Not Found). |
| `testEventNotFoundException` | Testet die Behandlung von `EventNotFoundException` und erwartet HTTP-Status 404 (Not Found). |
| `testSeatNotFoundException` | Testet die Behandlung von `SeatNotFoundException` und erwartet HTTP-Status 404 (Not Found). |
| `testReservationNotFoundException` | Testet die Behandlung von `ReservationNotFoundException` und erwartet HTTP-Status 404 (Not Found). |
| `testEventLocationNotFoundException` | Testet die Behandlung von `EventLocationNotFoundException` und erwartet HTTP-Status 404 (Not Found). |
| `testDuplicateUserException` | Testet die Behandlung von `DuplicateUserException` und erwartet HTTP-Status 409 (Conflict). |
| `testSeatAlreadyReservedException` | Testet die Behandlung von `SeatAlreadyReservedException` und erwartet HTTP-Status 409 (Conflict). |
| `testAuthenticationFailedException` | Testet die Behandlung von `AuthenticationFailedException` und erwartet HTTP-Status 401 (Unauthorized). |
| `testTokenExpiredException` | Testet die Behandlung von `TokenExpiredException` und erwartet HTTP-Status 401 (Unauthorized). |
| `testInvalidUserException` | Testet die Behandlung von `InvalidUserException` und erwartet HTTP-Status 400 (Bad Request). |
| `testEventBookingClosedException` | Testet die Behandlung von `EventBookingClosedException` und erwartet HTTP-Status 400 (Bad Request). |
| `testNoSeatsAvailableException` | Testet die Behandlung von `NoSeatsAvailableException` und erwartet HTTP-Status 400 (Bad Request). |
| `testGenericException` | Testet die Behandlung von generischen `RuntimeException` und erwartet HTTP-Status 500 (Internal Server Error) mit der ursprünglichen Fehlermeldung. |
| `testNullPointerException` | Testet die Behandlung von `NullPointerException` und erwartet HTTP-Status 500 (Internal Server Error). |
| `testExceptionWithNullMessage` | Testet die Behandlung von Exceptions mit null-Nachricht und erwartet HTTP-Status 500 (Internal Server Error). |
| `testExceptionWithEmptyMessage` | Testet die Behandlung von Exceptions mit leerer Nachricht und erwartet HTTP-Status 500 (Internal Server Error). |

**Wichtige Änderungen:**
- `EventBookingClosedException` und `NoSeatsAvailableException` geben jetzt HTTP-Status 400 (Bad Request) statt 406 (Not Acceptable) zurück
- Generische Exceptions geben die ursprüngliche Fehlermeldung ohne zusätzliches "An unexpected error occurred: " Prefix zurück
- `ErrorResponseDTO` wurde um eine `getMessage()`-Methode erweitert für bessere Kompatibilität

## HttpForwardFilter

### HttpForwardFilter

| Testfall | Beschreibung |
| :--- | :--- |
| `doFilter_ForwardToRootPath` | Überprüft, ob der Filter Anfragen, die nicht mit `/api` oder `/q` beginnen und einen 404-Status haben, an den Root-Pfad `/` weiterleitet. |
| `doFilter_NoForwardForApiOrQuarkusPath` | Überprüft, ob der Filter Anfragen, die mit `/api` oder `/q` beginnen, nicht weiterleitet. |
| `doFilter_NoForwardForNon404Status` | Überprüft, ob der Filter Anfragen mit einem Status ungleich 404 nicht weiterleitet. |

# Resource Tests

Dieses Dokument beschreibt die Tests für die REST-Ressourcen.

## Event Management

### EventLocationResource

Basispfad: `/api/manager/eventlocations`

Rollen: `MANAGER`, `ADMIN`

---

#### GET /

Ruft alle Event-Locations für den aktuellen Manager ab.

**Beschreibung:**

Dieser Test überprüft, ob ein Manager oder Administrator eine Liste seiner Event-Locations abrufen kann.

**Testfälle:**

*   **Erfolg:**
    *   Ein Manager mit zugeordneten Event-Locations ruft die Liste ab und erhält einen `200 OK`-Status mit den korrekten Daten.
    *   Ein Manager ohne Event-Locations ruft die Liste ab und erhält einen `200 OK`-Status mit einer leeren Liste.
*   **Fehler:**
    *   Ein nicht authentifizierter Benutzer versucht, auf den Endpunkt zuzugreifen, und erhält einen `401 Unauthorized`-Status.
    *   Ein Benutzer mit einer anderen Rolle (z. B. `USER`) versucht, auf den Endpunkt zuzugreifen, und erhält einen `403 Forbidden`-Status.

---

#### POST /

Erstellt eine neue Event-Location.

**Beschreibung:**

Dieser Test stellt sicher, dass ein Manager oder Administrator eine neue Event-Location erstellen kann.

**Testfälle:**

*   **Erfolg:**
    *   Ein Manager sendet gültige Daten und erstellt erfolgreich eine neue Event-Location. Der Status `200 OK` wird mit den Daten der erstellten Location zurückgegeben.
*   **Fehler:**
    *   Ein Manager sendet ungültige Daten (z. B. fehlender Name) und erhält einen `400 Bad Request`-Status.
    *   Ein nicht authentifizierter Benutzer versucht, eine Location zu erstellen, und erhält `401 Unauthorized`.
    *   Ein Benutzer mit der Rolle `USER` versucht, eine Location zu erstellen, und erhält `403 Forbidden`.
---

#### POST /import

Erstellt eine neue Event-Location mit Sitzplätzen.

**Beschreibung:**

Dieser Test stellt sicher, dass ein Manager oder Administrator eine neue Event-Location zusammen mit einer Liste von Sitzplätzen importieren kann.

**Testfälle:**

*   **Erfolg:**
    *   Ein Manager sendet gültige Daten und importiert erfolgreich eine neue Event-Location und die zugehörigen Sitzplätze. Der Status `200 OK` wird mit den Daten der importierten Location zurückgegeben.
*   **Fehler:**
    *   Ein Manager sendet ungültige Daten (z. B. fehlender Name in der Location oder fehlende Sitzplatznummer) und erhält einen `400 Bad Request`-Status.
    *   Ein nicht authentifizierter Benutzer versucht, eine Location zu erstellen, und erhält `401 Unauthorized`.
    *   Ein Benutzer mit der Rolle `USER` versucht, eine Location zu erstellen, und erhält `403 Forbidden`.

---

#### PUT /{id}

Aktualisiert eine bestehende Event-Location.

**Beschreibung:**

Dieser Test überprüft die Aktualisierungsfunktion für eine Event-Location.

**Testfälle:**

*   **Erfolg:**
    *   Ein Manager aktualisiert eine seiner Event-Locations mit gültigen Daten und erhält `200 OK` mit den aktualisierten Daten.
*   **Fehler:**
    *   Ein Manager versucht, eine Location mit ungültigen Daten zu aktualisieren, und erhält `400 Bad Request`.
    *   Ein Manager versucht, eine Location zu aktualisieren, die ihm nicht gehört, und erhält einen `404 Not Found`- oder `403 Forbidden`-Status.
    *   Ein Manager versucht, eine nicht existierende Location zu aktualisieren, und erhält `404 Not Found`.
    *   Ein nicht authentifizierter Benutzer versucht, eine Location zu aktualisieren, und erhält `401 Unauthorized`.

---

#### DELETE /{id}

Löscht eine Event-Location.

**Beschreibung:**

Dieser Test stellt sicher, dass ein Manager oder Administrator eine seiner Event-Locations löschen kann.

**Testfälle:**

*   **Erfolg:**
    *   Ein Manager löscht erfolgreich eine seiner Event-Locations und erhält den Status `200 OK`.
*   **Fehler:**
    *   Ein Manager versucht, eine Location zu löschen, die ihm nicht gehört, und erhält `404 Not Found` oder `403 Forbidden`.
    *   Ein Manager versucht, eine nicht existierende Location zu löschen, und erhält `404 Not Found`.
    *   Ein nicht authentifizierter Benutzer versucht, eine Location zu löschen, und erhält `401 Unauthorized`.

## Reservation

### EventResource

Basispfad: `/api/user/events`

Rolle: `USER`

---

#### GET /


---

### EventResource (Manager)

Basispfad: `/api/manager/events`

Rollen: `MANAGER`, `ADMIN`

---

#### POST /

Erstellt ein neues Event.

**Beschreibung:**

Dieser Test stellt sicher, dass ein Manager oder Admin ein neues Event für eine seiner Event-Locations erstellen kann.

**Testfälle:**

*   **Erfolg:**
    *   Ein Manager sendet gültige Event-Daten und erstellt erfolgreich ein neues Event. Er erhält `200 OK` mit den detaillierten Daten des Events.
*   **Fehler:**
    *   Ein Manager versucht, ein Event für eine Location zu erstellen, die ihm nicht gehört, und erhält `404 Not Found`.
    *   Ungültige Daten (z. B. Startdatum nach Enddatum) führen zu `400 Bad Request`.
    *   Ein nicht autorisierter Benutzer (z. B. `USER`) erhält `403 Forbidden`.

---

#### PUT /{id}

Aktualisiert ein bestehendes Event.

**Beschreibung:**

Dieser Test überprüft die Aktualisierung eines Events durch einen Manager oder Admin.

**Testfälle:**

*   **Erfolg:**
    *   Ein Manager aktualisiert ein Event, das er verwaltet, mit gültigen Daten und erhält `200 OK`.
*   **Fehler:**
    *   Der Versuch, ein Event zu aktualisieren, das nicht existiert, führt zu `404 Not Found`.
    *   Der Versuch, ein Event zu aktualisieren, das einem anderen Manager gehört, führt zu `404 Not Found`.
    *   Ungültige Daten führen zu `400 Bad Request`.

---

#### GET /

Ruft alle Events ab, die vom aktuellen Manager verwaltet werden.

**Beschreibung:**

Dieser Test stellt sicher, dass ein Manager oder Admin eine Liste seiner eigenen Events abrufen kann.

**Testfälle:**

*   **Erfolg:**
    *   Ein Manager ruft seine Event-Liste ab und erhält `200 OK` mit den Daten.
*   **Fehler:**
    *   Ein nicht autorisierter Benutzer erhält `403 Forbidden`.

---

#### GET /{id}

Ruft ein bestimmtes Event anhand seiner ID ab.

**Beschreibung:**

Dieser Test stellt sicher, dass ein Manager oder Administrator ein bestimmtes Event anhand seiner ID abrufen kann, sofern er dazu berechtigt ist.

**Testfälle:**

*   **Erfolg:**
    *   Ein Manager ruft ein Event ab, das er verwaltet, und erhält `200 OK` mit den Event-Daten.
    *   Ein Administrator ruft ein Event ab, das er nicht verwaltet, und erhält `200 OK` mit den Event-Daten.
*   **Fehler:**
    *   Ein Manager versucht, ein Event abzurufen, das einem anderen Manager gehört, und erhält `403 Forbidden`.
    *   Ein nicht existierendes Event wird angefragt, und es wird `404 Not Found` zurückgegeben.
    *   Ein nicht authentifizierter Benutzer versucht, auf den Endpunkt zuzugreifen, und erhält `401 Unauthorized`.

---

#### DELETE /{id}

Löscht ein Event und alle zugehörigen Daten.

**Beschreibung:**

Dieser Test stellt sicher, dass ein Manager oder Administrator ein Event löschen kann. Das Löschen eines Events sollte auch alle zugehörigen `EventUserAllowance`-Einträge und Reservierungen entfernen (Cascade-Delete).

**Testfälle:**

*   **Erfolg:**
    *   Ein Manager löscht erfolgreich ein Event, das er verwaltet, und erhält den Status `204 No Content`.
    *   Ein Administrator löscht erfolgreich ein Event, das er nicht verwaltet, und erhält den Status `204 No Content`.
    *   Nach dem Löschen des Events wird überprüft, ob auch die zugehörigen `EventUserAllowance`-Einträge gelöscht wurden.
*   **Fehler:**
    *   Ein Manager versucht, ein Event zu löschen, das ihm nicht gehört, und erhält einen `403 Forbidden`-Status.
    *   Ein Manager versucht, ein nicht existierendes Event zu löschen, und erhält einen `404 Not Found`-Status.
    *   Ein nicht authentifizierter Benutzer versucht, ein Event zu löschen, und erhält einen `401 Unauthorized`-Status.
    *   Ein Benutzer mit der Rolle `USER` versucht, ein Event zu löschen, und erhält einen `403 Forbidden`-Status.

---

### ReservationResource (Manager)

Basispfad: `/api/manager/reservations`

Rollen: `MANAGER`, `ADMIN`

---

#### GET /

Ruft alle Reservierungen für die Events des aktuellen Managers ab.

**Beschreibung:**

Dieser Test stellt sicher, dass ein Manager alle Reservierungen für seine Events einsehen kann.

**Testfälle:**

*   **Erfolg:**
    *   Ein Manager ruft die Liste ab und erhält `200 OK` mit allen relevanten Reservierungen.
*   **Fehler:**
    *   Ein nicht autorisierter Benutzer erhält `403 Forbidden`.

---

#### GET /{id}

Ruft eine bestimmte Reservierung anhand ihrer ID ab.

**Beschreibung:**

Dieser Test überprüft, ob ein Manager eine bestimmte Reservierung einsehen kann, sofern sie zu einem seiner Events gehört.

**Testfälle:**

*   **Erfolg:**
    *   Ein Manager ruft eine Reservierung ab, die zu einem seiner Events gehört, und erhält `200 OK`.
*   **Fehler:**
    *   Der Versuch, eine nicht existierende Reservierung abzurufen, führt zu `404 Not Found`.
    *   Der Versuch, eine Reservierung abzurufen, die zu einem Event eines anderen Managers gehört, führt zu `404 Not Found`.

---

#### POST /

Erstellt eine neue Reservierung (als Manager).

**Beschreibung:**

Dieser Test ermöglicht es einem Manager, manuell eine Reservierung für einen Benutzer zu erstellen.

**Testfälle:**

*   **Erfolg:**
    *   Ein Manager erstellt eine gültige Reservierung für einen Benutzer und erhält `200 OK`.
*   **Fehler:**
    *   Ungültige Daten (z. B. nicht existierender Benutzer oder Platz) führen zu `404 Not Found`.
    *   Der Versuch, einen bereits reservierten Platz zu buchen, führt zu `409 Conflict`.

---

#### PUT /{id}

Aktualisiert eine bestehende Reservierung (als Manager).

**Beschreibung:**

Dieser Test überprüft die Aktualisierung einer Reservierung durch einen Manager.

**Testfälle:**

*   **Erfolg:**
    *   Ein Manager aktualisiert eine Reservierung mit gültigen Daten und erhält `200 OK`.
*   **Fehler:**
    *   Der Versuch, eine Reservierung zu aktualisieren, die nicht zu einem seiner Events gehört, führt zu `404 Not Found`.

---

#### DELETE /{id}

Löscht eine Reservierung (als Manager).

**Beschreibung:**

Dieser Test stellt sicher, dass ein Manager eine Reservierung für eines seiner Events löschen kann.

**Testfälle:**

*   **Erfolg:**
    *   Ein Manager löscht eine Reservierung und erhält `200 OK`.
*   **Fehler:**
    *   Der Versuch, eine Reservierung zu löschen, die nicht zu einem seiner Events gehört, führt zu `404 Not Found`.

---

#### GET /event/{id}

Ruft alle Reservierungen für ein bestimmtes Event ab.

**Beschreibung:**

Dieser Test stellt sicher, dass ein Manager alle Reservierungen für ein bestimmtes Event einsehen kann, das er verwaltet.

**Testfälle:**

*   **Erfolg:**
    *   Ein Manager ruft die Liste der Reservierungen für ein von ihm verwaltetes Event ab und erhält `200 OK` mit den korrekten Daten.
*   **Fehler:**
    *   Ein Manager versucht, Reservierungen für ein Event abzurufen, das er nicht verwaltet, und erhält einen `403 Forbidden`-Status.
    *   Ein Manager versucht, Reservierungen für ein nicht existierendes Event abzurufen, und erhält einen `400 Bad Request`-Status.
    *   Ein nicht authentifizierter Benutzer versucht, auf den Endpunkt zuzugreifen, und erhält einen `401 Unauthorized`-Status.
    *   Ein Benutzer mit einer anderen Rolle (z. B. `USER`) versucht, auf den Endpunkt zuzugreifen, und erhält einen `403 Forbidden`-Status.

---

### SeatResource (Manager)

Basispfad: `/api/manager/seats`

Rollen: `MANAGER`, `ADMIN`

---

#### POST /

Erstellt einen neuen Sitzplatz für eine Event-Location.

**Beschreibung:**

Dieser Test stellt sicher, dass ein Manager neue Sitzplätze zu einer seiner Locations hinzufügen kann.

**Testfälle:**

*   **Erfolg:**
    *   Ein Manager fügt einen neuen Sitzplatz zu einer seiner Locations hinzu und erhält `200 OK`.
*   **Fehler:**
    *   Der Versuch, einen Sitzplatz zu einer Location hinzuzufügen, die einem anderen Manager gehört, führt zu `404 Not Found`.

---

#### GET /

Ruft alle Sitzplätze ab, die zu den Locations des aktuellen Managers gehören.

**Beschreibung:**

Dieser Test stellt sicher, dass ein Manager eine Liste aller seiner Sitzplätze abrufen kann.

**Testfälle:**

*   **Erfolg:**
    *   Ein Manager ruft die Liste seiner Sitzplätze ab und erhält `200 OK`.
*   **Fehler:**
    *   Ein nicht autorisierter Benutzer erhält `403 Forbidden`.

---

#### GET /{id}

Ruft einen bestimmten Sitzplatz anhand seiner ID ab.

**Beschreibung:**

Dieser Test überprüft, ob ein Manager einen bestimmten Sitzplatz einsehen kann, sofern er zu einer seiner Locations gehört.

**Testfälle:**

*   **Erfolg:**
    *   Ein Manager ruft einen seiner Sitzplätze ab und erhält `200 OK`.
*   **Fehler:**
    *   Der Versuch, einen Sitzplatz abzurufen, der zu einer Location eines anderen Managers gehört, führt zu `404 Not Found`.

---

#### PUT /{id}

Aktualisiert einen Sitzplatz.

**Beschreibung:**

Dieser Test überprüft die Aktualisierung eines Sitzplatzes durch einen Manager.

**Testfälle:**

*   **Erfolg:**
    *   Ein Manager aktualisiert einen seiner Sitzplätze und erhält `200 OK`.
*   **Fehler:**
    *   Der Versuch, einen Sitzplatz zu aktualisieren, der nicht zu seinen Locations gehört, führt zu `404 Not Found`.

---

#### DELETE /{id}

Löscht einen Sitzplatz.

**Beschreibung:**

Dieser Test stellt sicher, dass ein Manager einen Sitzplatz aus einer seiner Locations entfernen kann.

**Testfälle:**

*   **Erfolg:**
    *   Ein Manager löscht einen seiner Sitzplätze und erhält `200 OK`.
*   **Fehler:**
    *   Der Versuch, einen Sitzplatz zu löschen, der nicht zu seinen Locations gehört, führt zu `404 Not Found`.
    *   Der Versuch, einen Sitzplatz zu löschen, für den bereits eine Reservierung besteht, führt zu `409 Conflict`.
Ruft alle Events ab, für die der aktuelle Benutzer eine Berechtigung hat. Die Antwort enthält auch die Anzahl der erlaubten Reservierungen für jedes Event.

**Beschreibung:**

Dieser Test stellt sicher, dass ein Benutzer eine Liste der für ihn verfügbaren Events abrufen kann.

**Testfälle:**

*   **Erfolg:**
    *   Ein authentifizierter Benutzer mit Berechtigungen für Events ruft die Liste ab und erhält `200 OK` mit den Event-Daten, einschließlich der erlaubten Reservierungen.
    *   Ein authentifizierter Benutzer ohne Berechtigungen für Events ruft die Liste ab und erhält `200 OK` mit einer leeren Liste.
*   **Fehler:**
    *   Ein nicht authentifizierter Benutzer versucht, auf den Endpunkt zuzugreifen, und erhält `401 Unauthorized`.

---

### ReservationResource

Basispfad: `/api/user/reservations`

Rolle: `USER`

---

#### GET /

Ruft alle Reservierungen des aktuellen Benutzers ab.

**Beschreibung:**

Dieser Test stellt sicher, dass ein Benutzer seine eigenen Reservierungen abrufen kann.

**Testfälle:**

*   **Erfolg:**
    *   Ein Benutzer mit Reservierungen ruft die Liste ab und erhält `200 OK` mit seinen Reservierungsdaten.
    *   Ein Benutzer ohne Reservierungen ruft die Liste ab und erhält `200 OK` mit einer leeren Liste.
*   **Fehler:**
    *   Ein nicht authentifizierter Benutzer versucht, auf den Endpunkt zuzugreifen, und erhält `401 Unauthorized`.

---

#### GET /{id}

Ruft eine bestimmte Reservierung des aktuellen Benutzers ab.

**Beschreibung:**

Dieser Test überprüft, ob ein Benutzer eine einzelne seiner Reservierungen anhand der ID abrufen kann.

**Testfälle:**

*   **Erfolg:**
    *   Ein Benutzer ruft eine seiner eigenen Reservierungen ab und erhält `200 OK` mit den Reservierungsdaten.
*   **Fehler:**
    *   Ein Benutzer versucht, eine Reservierung abzurufen, die ihm nicht gehört, und erhält `404 Not Found`.
    *   Ein Benutzer versucht, eine nicht existierende Reservierung abzurufen, und erhält `404 Not Found`.
    *   Ein nicht authentifizierter Benutzer versucht, auf den Endpunkt zuzugreifen, und erhält `401 Unauthorized`.

---

#### POST /

Erstellt eine oder mehrere neue Reservierungen für den aktuellen Benutzer.

**Beschreibung:**

Dieser Test stellt sicher, dass ein Benutzer neue Reservierungen für ein Event erstellen kann, für das er berechtigt ist.

**Testfälle:**

*   **Erfolg:**
    *   Ein Benutzer sendet eine gültige Anfrage zur Erstellung von Reservierungen und erhält `200 OK` mit einer Liste der erstellten Reservierungen.
*   **Fehler:**
    *   Ein Benutzer sendet eine ungültige Anfrage (z. B. für ein Event, für das er keine Berechtigung hat, oder für bereits reservierte Plätze) und erhält einen entsprechenden Fehlerstatus (`400 Bad Request`, `404 Not Found`, `409 Conflict`).
    *   Ein Benutzer versucht, mehr Plätze zu reservieren, als ihm erlaubt sind, und erhält `400 Bad Request`.
    *   Ein nicht authentifizierter Benutzer versucht, eine Reservierung zu erstellen, und erhält `401 Unauthorized`.

---

#### DELETE /{id}

Löscht eine Reservierung des aktuellen Benutzers.

**Beschreibung:**

Dieser Test überprüft, ob ein Benutzer eine seiner eigenen Reservierungen löschen kann.

**Testfälle:**

*   **Erfolg:**
    *   Ein Benutzer löscht eine seiner Reservierungen und erhält `200 OK`.
*   **Fehler:**
    *   Ein Benutzer versucht, eine Reservierung zu löschen, die ihm nicht gehört, und erhält `404 Not Found`.
    *   Ein Benutzer versucht, eine nicht existierende Reservierung zu löschen, und erhält `404 Not Found`.
    *   Ein nicht authentifizierter Benutzer versucht, eine Reservierung zu löschen, und erhält `401 Unauthorized`.

## User Management

### EmailConfirmationResource

Basispfad: `/api/user`

Rolle: Öffentlich (keine Authentifizierung erforderlich)

---

#### GET /confirm-email

Bestätigt die E-Mail-Adresse eines Benutzers.

**Beschreibung:**

Dieser Test überprüft den E-Mail-Bestätigungsprozess über einen Token. Der Endpunkt gibt eine HTML-Seite zurück.

**Testfälle:**

*   **Erfolg:**
    *   Ein gültiger Bestätigungslink (mit ID und Token) wird verwendet und die E-Mail-Adresse wird erfolgreich bestätigt. Der Benutzer sieht eine Erfolgsseite.
*   **Fehler:**
    *   Es wird ein ungültiger Token verwendet, und der Benutzer sieht eine Fehlerseite mit dem Status `400 Bad Request`.
    *   Es wird ein Link mit einer nicht existierenden ID verwendet, und der Benutzer sieht eine Fehlerseite mit dem Status `404 Not Found`.
    *   Es wird ein abgelaufener Token verwendet, und der Benutzer sieht eine Fehlerseite mit dem Status `410 Gone`.

---

### UserResource

Basispfad: `/api/users`

Rollen: `ADMIN`, `MANAGER`, `USER`

---

#### POST /admin/import

Importiert eine Menge von Benutzern (nur für Admins).

**Beschreibung:**

Dieser Test stellt sicher, dass nur Administratoren Benutzer in großen Mengen importieren können.

**Testfälle:**

*   **Erfolg:**
    *   Ein Admin sendet ein gültiges Set von Benutzerdaten und importiert erfolgreich die Benutzer. Er erhält `200 OK` mit den Daten der importierten Benutzer.
*   **Fehler:**
    *   Ein Admin sendet ungültige Daten (z.B. leere Benutzernamen oder doppelte Benutzer) und erhält `400 Bad Request` oder `409 Conflict`.
    *   Ein Benutzer mit der Rolle `MANAGER` oder `USER` versucht, Benutzer zu importieren, und erhält `403 Forbidden`.
    *   Ein nicht authentifizierter Benutzer versucht, auf den Endpunkt zuzugreifen, und erhält `401 Unauthorized`.

---

#### POST /admin

Erstellt einen neuen Benutzer (nur für Admins).

**Beschreibung:**

Dieser Test stellt sicher, dass nur Administratoren neue Benutzer anlegen können.

**Testfälle:**

*   **Erfolg:**
    *   Ein Admin sendet gültige Benutzerdaten und erstellt erfolgreich einen neuen Benutzer. Er erhält `200 OK` mit den Daten des neuen Benutzers.
*   **Fehler:**
    *   Ein Admin sendet ungültige Daten (z. B. doppelte E-Mail) und erhält `400 Bad Request`.
    *   Ein Benutzer mit der Rolle `MANAGER` oder `USER` versucht, einen Benutzer zu erstellen, und erhält `403 Forbidden`.
    *   Ein nicht authentifizierter Benutzer versucht, auf den Endpunkt zuzugreifen, und erhält `401 Unauthorized`.

---

#### PUT /admin/{id}

Aktualisiert einen Benutzer (nur für Admins).

**Beschreibung:**

Dieser Test überprüft, ob ein Admin die Daten eines beliebigen Benutzers aktualisieren kann.

**Testfälle:**

*   **Erfolg:**
    *   Ein Admin aktualisiert die Daten eines bestehenden Benutzers und erhält `200 OK` mit den aktualisierten Daten.
*   **Fehler:**
    *   Ein Admin versucht, einen nicht existierenden Benutzer zu aktualisieren, und erhält `404 Not Found`.
    *   Ein Benutzer mit der Rolle `MANAGER` oder `USER` versucht, einen anderen Benutzer zu aktualisieren, und erhält `403 Forbidden`.

---

#### DELETE /admin/{id}

Löscht einen Benutzer (nur für Admins).

**Beschreibung:**

Dieser Test stellt sicher, dass nur Administratoren Benutzer löschen können.

**Testfälle:**

*   **Erfolg:**
    *   Ein Admin löscht einen bestehenden Benutzer und erhält `200 OK`.
*   **Fehler:**
    *   Ein Admin versucht, einen nicht existierenden Benutzer zu löschen, und erhält `404 Not Found`.
    *   Ein Benutzer mit der Rolle `MANAGER` oder `USER` versucht, einen Benutzer zu löschen, und erhält `403 Forbidden`.

---

#### GET /manager

Ruft eine Liste aller Benutzer mit eingeschränkten Informationen ab (für Admins und Manager).

**Beschreibung:**

Dieser Test überprüft, ob Admins und Manager eine Liste aller Benutzer erhalten.

**Testfälle:**

*   **Erfolg:**
    *   Ein Admin oder Manager ruft die Benutzerliste ab und erhält `200 OK` mit einer Liste von `LimitedUserInfoDTO`-Objekten.
*   **Fehler:**
    *   Ein Benutzer mit der Rolle `USER` versucht, auf den Endpunkt zuzugreifen, und erhält `403 Forbidden`.
    *   Ein nicht authentifizierter Benutzer versucht, auf den Endpunkt zuzugreifen, und erhält `401 Unauthorized`.

---

#### GET /admin/roles

Ruft alle verfügbaren Benutzerrollen ab (nur für Admins).

**Beschreibung:**

Dieser Test stellt sicher, dass nur Admins die Liste der verfügbaren Rollen abrufen können.

**Testfälle:**

*   **Erfolg:**
    *   Ein Admin ruft die Liste der Rollen ab und erhält `200 OK` mit einer Liste von Strings.
*   **Fehler:**
    *   Ein Benutzer mit einer anderen Rolle versucht, auf den Endpunkt zuzugreifen, und erhält `403 Forbidden`.

---

#### GET /admin/{id}

Ruft die vollständigen Daten eines bestimmten Benutzers ab (nur für Admins).

**Beschreibung:**

Dieser Test überprüft, ob ein Admin die vollständigen Details eines Benutzers anhand seiner ID abrufen kann.

**Testfälle:**

*   **Erfolg:**
    *   Ein Admin ruft die Daten eines bestehenden Benutzers ab und erhält `200 OK` mit dem `UserDTO`-Objekt.
*   **Fehler:**
    *   Ein Admin versucht, einen nicht existierenden Benutzer abzurufen, und erhält `404 Not Found`.
    *   Ein Benutzer mit einer anderen Rolle versucht, auf den Endpunkt zuzugreifen, und erhält `403 Forbidden`.

---

#### PUT /me

Aktualisiert das Profil des aktuell angemeldeten Benutzers.

**Beschreibung:**

Dieser Test stellt sicher, dass ein authentifizierter Benutzer sein eigenes Profil aktualisieren kann.

**Testfälle:**

*   **Erfolg:**
    *   Ein authentifizierter Benutzer aktualisiert sein eigenes Profil mit gültigen Daten und erhält `200 OK` mit den aktualisierten Daten.
*   **Fehler:**
    *   Ein Benutzer sendet ungültige Daten und erhält `400 Bad Request`.
    *   Ein nicht authentifizierter Benutzer versucht, auf den Endpunkt zuzugreifen, und erhält `401 Unauthorized`.

## Security

### AuthResource

Basispfad: `/api/auth`

Rolle: Öffentlich

---

#### POST /login

Authentifiziert einen Benutzer und gibt einen JWT-Token zurück.

**Beschreibung:**

Dieser Test überprüft den Anmeldevorgang.

**Testfälle:**

*   **Erfolg:**
    *   Ein registrierter und bestätigter Benutzer sendet gültige Anmeldeinformationen (Benutzername/E-Mail und Passwort) und erhält einen `200 OK`-Status mit einem JWT-Cookie.
*   **Fehler:**
    *   Ein Benutzer sendet ungültige Anmeldeinformationen (falscher Benutzername oder falsches Passwort) und erhält einen `401 Unauthorized`-Status.
    *   Ein Benutzer, dessen E-Mail-Adresse noch nicht bestätigt wurde, versucht sich anzumelden und erhält `401 Unauthorized`-Status.
    *   Die Anfrage hat ein ungültiges Format (z. B. fehlendes Passwort) und erhält einen `400 Bad Request`-Status.