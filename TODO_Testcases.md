# Email Service
 
## EmailService
 
*   **sendEmailConfirmation_Success**: Sendet erfolgreich eine E-Mail-Bestätigung an einen Benutzer.
*   **sendEmailConfirmation_IOException**: Simuliert einen Fehler beim Senden der E-Mail-Bestätigung (z.B. durch eine `IOException`). Erwartet, dass die Ausnahme korrekt behandelt wird.
 
## EmailVerificationCleanupService
 
*   **performManualCleanup_Success**: Führt die manuelle Bereinigung erfolgreich durch und löscht abgelaufene E-Mail-Verifizierungsdatensätze.
*   **performManualCleanup_NoExpiredTokens**: Überprüft, ob keine Datensätze gelöscht werden, wenn keine abgelaufenen Token vorhanden sind.
*   **scheduledCleanup_Success**: Überprüft, ob der geplante Bereinigungsprozess erfolgreich ausgeführt wird.
# Security
 
## AuthService
 
*   **authenticate_Success**: Authentifiziert einen Benutzer erfolgreich mit gültigen Anmeldeinformationen und gibt einen Token zurück.
*   **authenticate_AuthenticationFailedException_InvalidUsername**: Versucht, sich mit einem nicht existierenden Benutzernamen zu authentifizieren. Erwartet `AuthenticationFailedException`.
*   **authenticate_AuthenticationFailedException_InvalidPassword**: Versucht, sich mit einem gültigen Benutzernamen, aber einem falschen Passwort zu authentifizieren. Erwartet `AuthenticationFailedException`.
 
## TokenService
 
*   **generateToken_Success**: Generiert erfolgreich einen JWT-Token für einen gegebenen Benutzer.
*   **generateToken_ValidTokenContent**: Überprüft, ob der generierte Token die korrekten Benutzerinformationen (z.B. Benutzername, Rollen) enthält.
*   **generateToken_TokenExpiration**: Überprüft, ob der generierte Token eine korrekte Ablaufzeit hat.
 
## AuthResource
 
*   **login_Success**: Sendet eine POST-Anfrage an `/api/auth/login` mit gültigen Anmeldeinformationen. Erwartet einen 200 OK Status und einen JWT-Token im Antwort-Body.
*   **login_AuthenticationFailedException_InvalidCredentials**: Sendet eine POST-Anfrage an `/api/auth/login` mit ungültigen Anmeldeinformationen. Erwartet einen 403 Forbidden Status.
*   **login_BadRequest_MissingCredentials**: Sendet eine POST-Anfrage an `/api/auth/login` ohne Benutzernamen oder Passwort. Erwartet einen 400 Bad Request Status.

# Testfälle für UserService

## createUser(UserCreationDTO userCreationDTO)

*   **createUser_Success_WithEmail**: Erstellt einen neuen Benutzer mit gültigen Daten (Benutzername, Passwort, E-Mail, Vorname, Nachname). Überprüft, ob der Benutzer erfolgreich in der Datenbank gespeichert wird und eine E-Mail-Bestätigung gesendet wird.
*   **createUser_Success_WithoutEmail**: Erstellt einen neuen Benutzer mit gültigen Daten (Benutzername, Passwort, Vorname, Nachname), aber ohne E-Mail. Überprüft, ob der Benutzer erfolgreich in der Datenbank gespeichert wird und keine E-Mail-Bestätigung gesendet wird.
*   **createUser_InvalidUserException_NullDTO**: Versucht, einen Benutzer mit einem `null` `UserCreationDTO` zu erstellen. Erwartet `InvalidUserException`.
*   **createUser_InvalidUserException_EmptyUsername**: Versucht, einen Benutzer mit einem leeren oder nur aus Leerzeichen bestehenden Benutzernamen zu erstellen. Erwartet `InvalidUserException`.
*   **createUser_InvalidUserException_EmptyPassword**: Versucht, einen Benutzer mit einem leeren oder nur aus Leerzeichen bestehenden Passwort zu erstellen. Erwartet `InvalidUserException`.
*   **createUser_DuplicateUserException_ExistingUsername**: Versucht, einen Benutzer mit einem Benutzernamen zu erstellen, der bereits in der Datenbank existiert. Erwartet `DuplicateUserException`.
*   **createUser_Success_WithDuplicateEmail**: Erstellt einen neuen Benutzer mit einer E-Mail-Adresse, die bereits von einem anderen Benutzer verwendet wird. Überprüft, ob der Benutzer erfolgreich erstellt wird und keine `DuplicateUserException` geworfen wird.
*   **createUser_InternalServerErrorException_EmailSendFailure**: Simuliert einen Fehler beim Senden der E-Mail-Bestätigung (z.B. durch eine `IOException` im `EmailService`). Erwartet `InternalServerErrorException`.

## updateUser(Long id, UserProfileUpdateDTO user)

*   **updateUser_Success_UpdateFirstname**: Aktualisiert erfolgreich den Vornamen eines bestehenden Benutzers.
*   **updateUser_Success_UpdateLastname**: Aktualisiert erfolgreich den Nachnamen eines bestehenden Benutzers.
*   **updateUser_Success_UpdatePassword**: Aktualisiert erfolgreich das Passwort eines bestehenden Benutzers.
*   **updateUser_Success_UpdateRoles**: Aktualisiert erfolgreich die Rollen eines bestehenden Benutzers.
*   **updateUser_Success_NoEmailChange**: Aktualisiert erfolgreich andere Felder eines bestehenden Benutzers (z.B. Vorname, Nachname, Passwort, Rollen), ohne die E-Mail-Adresse zu ändern. Überprüft, ob keine E-Mail-Bestätigung gesendet wird.
*   **updateUser_Success_UpdateEmail**: Aktualisiert erfolgreich die E-Mail-Adresse eines bestehenden Benutzers und überprüft, ob die E-Mail-Verifizierung zurückgesetzt und eine neue Bestätigungs-E-Mail gesendet wird.
*   **updateUser_UserNotFoundException**: Versucht, einen nicht existierenden Benutzer zu aktualisieren. Erwartet `UserNotFoundException`.
*   **updateUser_InvalidUserException_NullDTO**: Versucht, einen Benutzer mit einem `null` `UserProfileUpdateDTO` zu aktualisieren. Erwartet `InvalidUserException`.
*   **updateUser_Success_WithDuplicateEmail**: Aktualisiert die E-Mail-Adresse eines Benutzers auf eine bereits existierende E-Mail-Adresse. Überprüft, ob die Aktualisierung erfolgreich ist und keine `DuplicateUserException` geworfen wird.
*   **updateUser_InternalServerErrorException_EmailSendFailure**: Simuliert einen Fehler beim Senden der E-Mail-Bestätigung nach einer E-Mail-Änderung. Erwartet `InternalServerErrorException`.

## deleteUser(Long id)

*   **deleteUser_Success**: Löscht erfolgreich einen bestehenden Benutzer anhand seiner ID.
*   **deleteUser_UserNotFoundException**: Versucht, einen nicht existierenden Benutzer zu löschen. Erwartet `UserNotFoundException`.

## getUserById(Long id)

*   **getUserById_Success**: Ruft erfolgreich einen bestehenden Benutzer anhand seiner ID ab.
*   **getUserById_UserNotFoundException**: Versucht, einen nicht existierenden Benutzer abzurufen. Erwartet `UserNotFoundException`.

## getAllUsers()

*   **getAllUsers_Success_WithUsers**: Ruft erfolgreich eine Liste aller Benutzer ab, wenn Benutzer vorhanden sind.
*   **getAllUsers_Success_NoUsers**: Ruft erfolgreich eine leere Liste ab, wenn keine Benutzer vorhanden sind.

## getAvailableRoles()

*   **getAvailableRoles_Success**: Ruft erfolgreich eine Liste aller verfügbaren Rollen ab.

## updateUserProfile(String username, UserProfileUpdateDTO userProfileUpdateDTO)

*   **updateUserProfile_Success_UpdateFirstname**: Aktualisiert erfolgreich den Vornamen eines bestehenden Benutzers über seinen Benutzernamen.
*   **updateUserProfile_Success_UpdateLastname**: Aktualisiert erfolgreich den Nachnamen eines bestehenden Benutzers über seinen Benutzernamen.
*   **updateUserProfile_Success_UpdatePassword**: Aktualisiert erfolgreich das Passwort eines bestehenden Benutzers über seinen Benutzernamen.
*   **updateUserProfile_Success_UpdateEmail**: Aktualisiert erfolgreich die E-Mail-Adresse eines bestehenden Benutzers über seinen Benutzernamen und überprüft, ob die E-Mail-Verifizierung zurückgesetzt und eine neue Bestätigungs-E-Mail gesendet wird.
*   **updateUserProfile_UserNotFoundException**: Versucht, das Profil eines nicht existierenden Benutzers zu aktualisieren. Erwartet `UserNotFoundException`.
*   **updateUserProfile_InvalidUserException_NullDTO**: Versucht, ein Benutzerprofil mit einem `null` `UserProfileUpdateDTO` zu aktualisieren. Erwartet `InvalidUserException`.
*   **updateUserProfile_Success_WithDuplicateEmail**: Aktualisiert die E-Mail-Adresse eines Benutzerprofils auf eine bereits existierende E-Mail-Adresse. Überprüft, ob die Aktualisierung erfolgreich ist und keine `DuplicateUserException` geworfen wird.
*   **updateUserProfile_InternalServerErrorException_EmailSendFailure**: Simuliert einen Fehler beim Senden der E-Mail-Bestätigung nach einer E-Mail-Änderung. Erwartet `InternalServerErrorException`.

## verifyEmail(Long id, String token)

*   **verifyEmail_Success**: Verifiziert erfolgreich eine E-Mail-Adresse mit einer gültigen ID und einem gültigen Token. Überprüft, ob der E-Mail-Verifizierungsdatensatz gelöscht und der Benutzer als "E-Mail verifiziert" markiert wird.
*   **verifyEmail_BadRequestException_NullId**: Versucht, die E-Mail mit einer `null` ID zu verifizieren. Erwartet `BadRequestException`.
*   **verifyEmail_BadRequestException_NullToken**: Versucht, die E-Mail mit einem `null` Token zu verifizieren. Erwartet `BadRequestException`.
*   **verifyEmail_BadRequestException_EmptyToken**: Versucht, die E-Mail mit einem leeren Token zu verifizieren. Erwartet `BadRequestException`.
*   **verifyEmail_NotFoundException_TokenNotFound**: Versucht, die E-Mail mit einer ID zu verifizieren, für die kein Verifizierungsdatensatz existiert. Erwartet `NotFoundException`.
*   **verifyEmail_BadRequestException_InvalidToken**: Versucht, die E-Mail mit einem ungültigen Token zu verifizieren (Token stimmt nicht überein). Erwartet `BadRequestException`.
*   **verifyEmail_TokenExpiredException**: Versucht, die E-Mail mit einem abgelaufenen Token zu verifizieren. Erwartet `TokenExpiredException`.

# EventService

## createEvent(EventRequestDTO dto, User manager)

*   **createEvent_Success**: Erstellt erfolgreich ein neues Event mit gültigen Daten und einem Manager.
*   **createEvent_IllegalArgumentException_LocationNotFound**: Versucht, ein Event mit einer nicht existierenden EventLocation-ID zu erstellen. Erwartet `IllegalArgumentException`.

## updateEvent(Long id, EventRequestDTO dto, User manager)

*   **updateEvent_Success_AsManager**: Aktualisiert erfolgreich ein bestehendes Event als Manager des Events.
*   **updateEvent_Success_AsAdmin**: Aktualisiert erfolgreich ein bestehendes Event als Administrator.
*   **updateEvent_EventNotFoundException**: Versucht, ein nicht existierendes Event zu aktualisieren. Erwartet `EventNotFoundException`.
*   **updateEvent_ForbiddenException_NotManagerOrAdmin**: Versucht, ein Event als Benutzer zu aktualisieren, der weder Manager noch Administrator ist. Erwartet `ForbiddenException`.
*   **updateEvent_IllegalArgumentException_LocationNotFound**: Versucht, ein Event mit einer nicht existierenden EventLocation-ID zu aktualisieren. Erwartet `IllegalArgumentException`.

## getEventsByCurrentManager(User manager)

*   **getEventsByCurrentManager_Success_AsAdmin**: Ruft alle Events als Administrator ab.
*   **getEventsByCurrentManager_Success_AsManager**: Ruft Events ab, die dem aktuellen Manager gehören.
*   **getEventsByCurrentManager_Success_NoEventsForManager**: Ruft eine leere Liste ab, wenn der Manager keine Events verwaltet.

## setReservationsAllowedForUser(EventUserAllowancesDto dto, User manager)

*   **setReservationsAllowedForUser_Success_NewAllowance**: Setzt erfolgreich die erlaubte Anzahl von Reservierungen für einen Benutzer für ein Event (neuer Eintrag).
*   **setReservationsAllowedForUser_Success_UpdateAllowance**: Aktualisiert erfolgreich die erlaubte Anzahl von Reservierungen für einen Benutzer für ein Event (bestehender Eintrag).
*   **setReservationsAllowedForUser_EventNotFoundException**: Versucht, die Reservierungserlaubnis für ein nicht existierendes Event zu setzen. Erwartet `EventNotFoundException`.
*   **setReservationsAllowedForUser_UserNotFoundException**: Versucht, die Reservierungserlaubnis für einen nicht existierenden Benutzer zu setzen. Erwartet `UserNotFoundException`.
*   **setReservationsAllowedForUser_ForbiddenException_NotManagerOrAdmin**: Versucht, die Reservierungserlaubnis als Benutzer zu setzen, der weder Manager noch Administrator des Events ist. Erwartet `ForbiddenException`.

# ReservationService

## findAllReservations(User currentUser)

*   **findAllReservations_Success_AsAdmin**: Ruft alle Reservierungen als Administrator ab.
*   **findAllReservations_Success_AsManager**: Ruft Reservierungen für Events ab, die der Manager verwalten darf.
*   **findAllReservations_Success_NoAllowedEventsForManager**: Ruft eine leere Liste ab, wenn der Manager keine Events verwalten darf.
*   **findAllReservations_ForbiddenException_OtherRoles**: Versucht, Reservierungen als Benutzer mit einer anderen Rolle abzurufen. Erwartet `ForbiddenException`.

## findReservationById(Long id, User currentUser)

*   **findReservationById_Success_AsAdmin**: Ruft eine Reservierung als Administrator ab.
*   **findReservationById_Success_AsManager**: Ruft eine Reservierung ab, die zu einem Event gehört, das der Manager verwalten darf.
*   **findReservationById_NotFoundException**: Versucht, eine nicht existierende Reservierung abzurufen. Erwartet `NotFoundException`.
*   **findReservationById_ForbiddenException_NotAllowed**: Versucht, eine Reservierung abzurufen, für die der Benutzer keine Berechtigung hat. Erwartet `ForbiddenException`.

## createReservation(ReservationRequestDTO dto, User currentUser)

*   **createReservation_Success_AsAdmin**: Erstellt erfolgreich eine Reservierung als Administrator.
*   **createReservation_Success_AsManager**: Erstellt erfolgreich eine Reservierung als Manager für ein Event, das er verwalten darf.
*   **createReservation_UserNotFoundException_TargetUser**: Versucht, eine Reservierung für einen nicht existierenden Zielbenutzer zu erstellen. Erwartet `UserNotFoundException`.
*   **createReservation_NotFoundException_EventNotFound**: Versucht, eine Reservierung für ein nicht existierendes Event zu erstellen. Erwartet `NotFoundException`.
*   **createReservation_ForbiddenException_NotAllowed**: Versucht, eine Reservierung als Benutzer zu erstellen, der keine Berechtigung hat. Erwartet `ForbiddenException`.
*   **createReservation_NotFoundException_SeatNotFound**: Versucht, eine Reservierung für einen nicht existierenden Sitzplatz zu erstellen. Erwartet `NotFoundException`.
*   **createReservation_BadRequestException_NoAllowance**: Versucht, eine Reservierung zu erstellen, wenn der Benutzer keine Reservierungserlaubnis für das Event hat. Erwartet `BadRequestException`.
*   **createReservation_BadRequestException_AllowanceZero**: Versucht, eine Reservierung zu erstellen, wenn die Reservierungserlaubnis des Benutzers 0 ist. Erwartet `BadRequestException`.

## updateReservation(Long id, ReservationRequestDTO dto, User currentUser)

*   **updateReservation_Success_AsAdmin**: Aktualisiert erfolgreich eine Reservierung als Administrator.
*   **updateReservation_Success_AsManager**: Aktualisiert erfolgreich eine Reservierung als Manager für ein Event, das er verwalten darf.
*   **updateReservation_NotFoundException_ReservationNotFound**: Versucht, eine nicht existierende Reservierung zu aktualisieren. Erwartet `NotFoundException`.
*   **updateReservation_ForbiddenException_NotAllowed**: Versucht, eine Reservierung als Benutzer zu aktualisieren, der keine Berechtigung hat. Erwartet `ForbiddenException`.
*   **updateReservation_NotFoundException_NewEventNotFound**: Versucht, eine Reservierung auf ein nicht existierendes neues Event zu aktualisieren. Erwartet `NotFoundException`.
*   **updateReservation_ForbiddenException_NewEventNotAllowed**: Versucht, eine Reservierung auf ein neues Event zu aktualisieren, für das der Manager keine Berechtigung hat. Erwartet `ForbiddenException`.
*   **updateReservation_NotFoundException_UserNotFound**: Versucht, eine Reservierung mit einem nicht existierenden Benutzer zu aktualisieren. Erwartet `NotFoundException`.
*   **updateReservation_NotFoundException_SeatNotFound**: Versucht, eine Reservierung mit einem nicht existierenden Sitzplatz zu aktualisieren. Erwartet `NotFoundException`.

## deleteReservation(Long id, User currentUser)

*   **deleteReservation_Success_AsAdmin**: Löscht erfolgreich eine Reservierung als Administrator.
*   **deleteReservation_Success_AsManager**: Löscht erfolgreich eine Reservierung als Manager für ein Event, das er verwalten darf.
*   **deleteReservation_NotFoundException**: Versucht, eine nicht existierende Reservierung zu löschen. Erwartet `NotFoundException`.
*   **deleteReservation_ForbiddenException_NotAllowed**: Versucht, eine Reservierung als Benutzer zu löschen, der keine Berechtigung hat. Erwartet `ForbiddenException`.



# EventService (reservation package)

## getEventsForCurrentUser(String username)

*   **getEventsForCurrentUser_Success**: Ruft erfolgreich Events für den aktuellen Benutzer ab, basierend auf seinen EventUserAllowances.
*   **getEventsForCurrentUser_UserNotFoundException**: Versucht, Events für einen nicht existierenden Benutzer abzurufen. Erwartet `UserNotFoundException`.
*   **getEventsForCurrentUser_Success_NoEvents**: Ruft eine leere Liste ab, wenn der Benutzer keine EventUserAllowances hat.

## getAvailableSeatsForCurrentUser(Long eventId, String username)

*   **getAvailableSeatsForCurrentUser_Success**: Ruft erfolgreich die Anzahl der verfügbaren Sitze für den aktuellen Benutzer für ein bestimmtes Event ab.
*   **getAvailableSeatsForCurrentUser_UserNotFoundException**: Versucht, verfügbare Sitze für einen nicht existierenden Benutzer abzurufen. Erwartet `UserNotFoundException`.
*   **getAvailableSeatsForCurrentUser_NotFoundException_EventNotFound**: Versucht, verfügbare Sitze für ein nicht existierendes Event abzurufen. Erwartet `NotFoundException`.
*   **getAvailableSeatsForCurrentUser_ForbiddenException_NoAccess**: Versucht, verfügbare Sitze für ein Event abzurufen, für das der Benutzer keine Berechtigung hat. Erwartet `ForbiddenException`.

# ReservationService (reservation package)

## findReservationsByUser(User currentUser)

*   **findReservationsByUser_Success**: Ruft erfolgreich alle Reservierungen für den aktuellen Benutzer ab.
*   **findReservationsByUser_Success_NoReservations**: Ruft eine leere Liste ab, wenn der Benutzer keine Reservierungen hat.

## findReservationByIdForUser(Long id, User currentUser)

*   **findReservationByIdForUser_Success**: Ruft erfolgreich eine Reservierung für den aktuellen Benutzer anhand der ID ab.
*   **findReservationByIdForUser_NotFoundException**: Versucht, eine nicht existierende Reservierung abzurufen. Erwartet `NotFoundException`.
*   **findReservationByIdForUser_ForbiddenException**: Versucht, eine Reservierung abzurufen, die nicht dem aktuellen Benutzer gehört. Erwartet `ForbiddenException`.

## createReservationForUser(ReservationsRequestCreateDTO dto, User currentUser)

*   **createReservationForUser_Success**: Erstellt erfolgreich eine oder mehrere Reservierungen für den aktuellen Benutzer.
*   **createReservationForUser_NotFoundException_EventNotFound**: Versucht, eine Reservierung für ein nicht existierendes Event zu erstellen. Erwartet `NotFoundException`.
*   **createReservationForUser_NotFoundException_SeatNotFound**: Versucht, eine Reservierung für einen oder mehrere nicht existierende Sitzplätze zu erstellen. Erwartet `NotFoundException`.
*   **createReservationForUser_ForbiddenException_NoAllowance**: Versucht, eine Reservierung zu erstellen, wenn der Benutzer keine Reservierungserlaubnis für das Event hat. Erwartet `ForbiddenException`.
*   **createReservationForUser_NoSeatsAvailableException_LimitReached**: Versucht, mehr Reservierungen zu erstellen, als die erlaubte Anzahl für den Benutzer. Erwartet `NoSeatsAvailableException`.
*   **createReservationForUser_EventBookingClosedException**: Versucht, eine Reservierung für ein Event zu erstellen, dessen Buchungsfrist abgelaufen ist. Erwartet `EventBookingClosedException`.
*   **createReservationForUser_SeatAlreadyReservedException**: Versucht, eine Reservierung für einen bereits reservierten Sitzplatz zu erstellen. Erwartet `SeatAlreadyReservedException`.

## deleteReservationForUser(Long id, User currentUser)

*   **deleteReservationForUser_Success**: Löscht erfolgreich eine Reservierung für den aktuellen Benutzer.
*   **deleteReservationForUser_NotFoundException**: Versucht, eine nicht existierende Reservierung zu löschen. Erwartet `NotFoundException`.
*   **deleteReservationForUser_ForbiddenException_NotOwner**: Versucht, eine Reservierung zu löschen, die nicht dem aktuellen Benutzer gehört. Erwartet `ForbiddenException`.
*   **deleteReservationForUser_ForbiddenException_NoAllowance**: Versucht, eine Reservierung zu löschen, wenn der Benutzer keine Reservierungserlaubnis für das Event hat (obwohl er der Besitzer der Reservierung ist). Erwartet `ForbiddenException`.


# EventLocation Service

## EventLocationService

*   **getEventLocationsByCurrentManager_Success_AsAdmin**: Ruft alle EventLocations als Administrator ab.
*   **getEventLocationsByCurrentManager_Success_AsManager**: Ruft EventLocations ab, die dem aktuellen Manager gehören.
*   **getEventLocationsByCurrentManager_Success_NoEventLocationsForManager**: Ruft eine leere Liste ab, wenn der Manager keine EventLocations verwaltet.
*   **createEventLocation_Success**: Erstellt erfolgreich eine neue EventLocation mit gültigen Daten.
*   **createEventLocation_InvalidInput**: Versucht, eine EventLocation mit ungültigen Daten zu erstellen (z.B. leere Felder).
*   **updateEventLocation_Success**: Aktualisiert erfolgreich eine bestehende EventLocation.
*   **updateEventLocation_NotFound**: Versucht, eine nicht existierende EventLocation zu aktualisieren.
*   **deleteEventLocation_Success**: Löscht erfolgreich eine bestehende EventLocation.
*   **deleteEventLocation_NotFound**: Versucht, eine nicht existierende EventLocation zu löschen.

# Seat Service

## SeatService

*   **createSeat_Success**: Erstellt erfolgreich einen neuen Sitzplatz mit gültigen Daten.
*   **createSeat_InvalidInput**: Versucht, einen Sitzplatz mit ungültigen Daten zu erstellen.
*   **findAllSeatsForManager_Success_AsAdmin**: Ruft alle Sitzplätze als Administrator ab.
*   **findAllSeatsForManager_Success_AsManager**: Ruft Sitzplätze ab, die dem aktuellen Manager gehören.
*   **findAllSeatsForManager_Success_NoSeatsForManager**: Ruft eine leere Liste ab, wenn der Manager keine Sitzplätze verwaltet.
*   **findSeatByIdForManager_Success_AsAdmin**: Ruft einen Sitzplatz als Administrator ab.
*   **findSeatByIdForManager_Success_AsManager**: Ruft einen Sitzplatz ab, der dem aktuellen Manager gehört.
*   **findSeatByIdForManager_NotFound**: Versucht, einen nicht existierenden Sitzplatz abzurufen.
*   **updateSeat_Success**: Aktualisiert erfolgreich einen bestehenden Sitzplatz.
*   **updateSeat_NotFound**: Versucht, einen nicht existierenden Sitzplatz zu aktualisieren.
*   **updateSeat_InvalidInput**: Versucht, einen Sitzplatz mit ungültigen Daten zu aktualisieren.
*   **deleteSeat_Success**: Löscht erfolgreich einen bestehenden Sitzplatz.
*   **deleteSeat_NotFound**: Versucht, einen nicht existierenden Sitzplatz zu löschen.
*   **findSeatEntityById_Success**: Ruft eine Sitzplatz-Entität erfolgreich ab.
*   **findSeatEntityById_ForbiddenException**: Versucht, eine Sitzplatz-Entität abzurufen, für die der Benutzer keine Berechtigung hat.

# HttpForwardFilter

## HttpForwardFilter

*   **doFilter_ForwardToRootPath**: Überprüft, ob der Filter Anfragen, die nicht mit `/api` oder `/q` beginnen und einen 404-Status haben, an den Root-Pfad `/` weiterleitet.
*   **doFilter_NoForwardForApiOrQuarkusPath**: Überprüft, ob der Filter Anfragen, die mit `/api` oder `/q` beginnen, nicht weiterleitet.
*   **doFilter_NoForwardForNon404Status**: Überprüft, ob der Filter Anfragen mit einem Status ungleich 404 nicht weiterleitet.
