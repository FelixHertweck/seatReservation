# API-Dokumentation

Dies ist die API-Dokumentation für das Sitzplatzreservierungssystem.

## Inhaltsverzeichnis

- [Authentifizierung](#authentifizierung)
- [Benutzerverwaltung (Admin)](#benutzerverwaltung-admin)
- [Benutzerprofil (Benutzer)](#benutzerprofil-benutzer)
- [E-Mail-Bestätigung](#e-mail-bestätigung)
- [Event-Locations (Manager/Admin)](#event-locations-manageradmin)
- [Events (Manager/Admin)](#events-manageradmin)
- [Sitzplätze (Manager/Admin)](#sitzplätze-manageradmin)
- [Reservierungen (Manager/Admin)](#reservierungen-manageradmin)
- [Events (Benutzer)](#events-benutzer)
- [Reservierungen (Benutzer)](#reservierungen-benutzer)

---

## Authentifizierung

### AuthResource

Basispfad: `/api/auth`

---

#### POST /login

Authentifiziert einen Benutzer und setzt einen JWT-Cookie.

-   **Rollen:** Öffentlich
-   **Request Body:** `LoginRequestDTO`
    ```json
    {
      "username": "string",
      "password": "string"
    }
    ```
-   **Responses:**
    -   `200 OK`: Erfolgreiche Anmeldung. Setzt einen `jwt`-Cookie.
    -   `401 Unauthorized`: Ungültige Anmeldeinformationen oder E-Mail nicht bestätigt.

---

## Benutzerverwaltung (Admin)

### UserResource

Basispfad: `/api/users`

---

#### POST /admin

Erstellt einen neuen Benutzer.

-   **Rollen:** `ADMIN`
-   **Request Body:** `UserCreationDTO`
-   **Responses:**
    -   `200 OK`: Benutzer erfolgreich erstellt. Gibt `UserDTO` zurück.
    -   `400 Bad Request`: Ungültige Daten (z.B. doppelter Benutzername).
    -   `403 Forbidden`: Zugriff verweigert (Rolle ist nicht `ADMIN`).

---

#### PUT /admin/{id}

Aktualisiert einen bestehenden Benutzer.

-   **Rollen:** `ADMIN`
-   **Path Parameter:** `id` (Long) - Die ID des Benutzers.
-   **Request Body:** `AdminUserUpdateDTO`
-   **Responses:**
    -   `200 OK`: Benutzer erfolgreich aktualisiert. Gibt `UserDTO` zurück.
    -   `404 Not Found`: Benutzer nicht gefunden.
    -   `403 Forbidden`: Zugriff verweigert.

---

#### DELETE /admin/{id}

Löscht einen Benutzer.

-   **Rollen:** `ADMIN`
-   **Path Parameter:** `id` (Long) - Die ID des Benutzers.
-   **Responses:**
    -   `200 OK`: Benutzer erfolgreich gelöscht.
    -   `404 Not Found`: Benutzer nicht gefunden.
    -   `403 Forbidden`: Zugriff verweigert.

---

#### GET /manager

Ruft eine Liste aller Benutzer mit eingeschränkten Informationen ab.

-   **Rollen:** `ADMIN`, `MANAGER`
-   **Responses:**
    -   `200 OK`: Gibt eine Liste von `LimitedUserInfoDTO` Objekten zurück.
    -   `403 Forbidden`: Zugriff verweigert.

---

#### GET /admin/roles

Ruft alle verfügbaren Benutzerrollen ab.

-   **Rollen:** `ADMIN`
-   **Responses:**
    -   `200 OK`: Gibt eine Liste der verfügbaren Rollen (Strings) zurück.
    -   `403 Forbidden`: Zugriff verweigert.

---

#### GET /admin

Ruft die vollständigen Daten aller Benutzer ab.

-   **Rollen:** `ADMIN`
-   **Responses:**
    -   `200 OK`: Gibt eine Liste von `UserDTO` Objekten zurück.
    -   `403 Forbidden`: Zugriff verweigert.

---

## Benutzerprofil (Benutzer)

### UserResource

Basispfad: `/api/users`

---

#### PUT /me

Aktualisiert das Profil des aktuell angemeldeten Benutzers.

-   **Rollen:** `USER`
-   **Request Body:** `UserProfileUpdateDTO`
-   **Responses:**
    -   `200 OK`: Profil erfolgreich aktualisiert. Gibt `UserDTO` zurück.
    -   `400 Bad Request`: Ungültige Daten.
    -   `401 Unauthorized`: Nicht authentifiziert.

---

#### GET /me

Ruft die Daten des aktuell angemeldeten Benutzers ab.

-   **Rollen:** `USER`, `ADMIN`, `MANAGER`
-   **Responses:**
    -   `200 OK`: Gibt das `UserDTO` Objekt des aktuellen Benutzers zurück.

---

## E-Mail-Bestätigung

### EmailConfirmationResource

Basispfad: `/api/user`

---

#### GET /confirm-email

Bestätigt die E-Mail-Adresse eines Benutzers.

-   **Rollen:** Öffentlich
-   **Query Parameter:**
    -   `id` (Long): Die Bestätigungs-ID.
    -   `token` (String): Das Bestätigungs-Token.
-   **Responses:**
    -   `200 OK`: E-Mail erfolgreich bestätigt (gibt HTML-Seite zurück).
    -   `400 Bad Request`: Ungültiger Token (gibt HTML-Fehlerseite zurück).
    -   `404 Not Found`: Token nicht gefunden (gibt HTML-Fehlerseite zurück).
    -   `410 Gone`: Token abgelaufen.

---

## Event-Locations (Manager/Admin)

### EventLocationResource

Basispfad: `/api/manager/eventlocations`

---

#### GET /

Ruft alle Event-Locations für den aktuellen Manager ab.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Gibt eine Liste von `EventLocationResponseDTO` Objekten zurück.

---

#### POST /

Erstellt eine neue Event-Location.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Request Body:** `EventLocationRequestDTO`
-   **Responses:**
    -   `200 OK`: Location erfolgreich erstellt. Gibt `EventLocationResponseDTO` zurück.
    -   `400 Bad Request`: Ungültige Eingabedaten.

---

#### PUT /{id}

Aktualisiert eine bestehende Event-Location.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - Die ID der Event-Location.
-   **Request Body:** `EventLocationRequestDTO`
-   **Responses:**
    -   `200 OK`: Location erfolgreich aktualisiert. Gibt `EventLocationResponseDTO` zurück.
    -   `404 Not Found`: Location nicht gefunden oder keine Berechtigung.

---

#### DELETE /{id}

Löscht eine Event-Location.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - Die ID der Event-Location.
-   **Responses:**
    -   `200 OK`: Location erfolgreich gelöscht.
    -   `404 Not Found`: Location nicht gefunden oder keine Berechtigung.

---

#### POST /import

Erstellt eine neue Event-Location zusammen mit einer Liste von Sitzplätzen.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Request Body:** `ImportEventLocationDto`
    ```json
    {
      "name": "string",
      "address": "string",
      "capacity": "integer",
      "seats": [
        {
          "seatNumber": "string",
          "xCoordinate": "integer",
          "yCoordinate": "integer"
        }
      ]
    }
    ```
-   **Responses:**
    -   `200 OK`: Location und Sitzplätze erfolgreich erstellt. Gibt `EventLocationResponseDTO` zurück.
    -   `400 Bad Request`: Ungültige Eingabedaten.
---

#### POST /importSeats/{id}

Importiert eine Liste von Sitzplätzen zu einer bestehenden Event-Location.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - Die ID der Event-Location.
-   **Request Body:** `Set<ImportSeatDto>`
    ```json
    [
      {
        "seatNumber": "string",
        "xCoordinate": "integer",
        "yCoordinate": "integer"
      }
    ]
    ```
-   **Responses:**
    -   `200 OK`: Sitze erfolgreich importiert. Gibt `EventLocationResponseDTO` zurück.
    -   `400 Bad Request`: Ungültige Eingabedaten.
    -   `403 Forbidden`: Keine Berechtigung.
    -   `404 Not Found`: Location nicht gefunden.

---

## Events (Manager/Admin)

### EventResource

Basispfad: `/api/manager/events`

---

#### GET /

Ruft alle Events ab, die vom aktuellen Manager verwaltet werden.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Gibt eine Liste von `DetailedEventResponseDTO` Objekten zurück.

---

#### GET /{id}

Ruft ein bestimmtes Event anhand seiner ID ab.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - Die ID des Events.
-   **Responses:**
    -   `200 OK`: Gibt `DetailedEventResponseDTO` zurück.
    -   `403 Forbidden`: Keine Berechtigung.
    -   `404 Not Found`: Event nicht gefunden.

---

#### POST /

Erstellt ein neues Event.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Request Body:** `EventRequestDTO`
-   **Responses:**
    -   `200 OK`: Event erfolgreich erstellt. Gibt `DetailedEventResponseDTO` zurück.
    -   `400 Bad Request`: Ungültige Daten.
    -   `404 Not Found`: Zugehörige Location nicht gefunden.

---

#### PUT /{id}

Aktualisiert ein bestehendes Event.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - Die ID des Events.
-   **Request Body:** `EventRequestDTO`
-   **Responses:**
    -   `200 OK`: Event erfolgreich aktualisiert. Gibt `DetailedEventResponseDTO` zurück.
    -   `404 Not Found`: Event nicht gefunden oder keine Berechtigung.

---

#### DELETE /{id}

Löscht ein Event.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - Die ID des Events.
-   **Responses:**
    -   `204 No Content`: Event erfolgreich gelöscht.
    -   `403 Forbidden`: Keine Berechtigung zum Löschen.
    -   `404 Not Found`: Event nicht gefunden.

---

## Sitzplätze (Manager/Admin)

### SeatResource

Basispfad: `/api/manager/seats`

---

#### GET /

Ruft alle Sitzplätze ab, die zu den Locations des aktuellen Managers gehören.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Gibt eine Liste von `SeatResponseDTO` Objekten zurück.

---

#### POST /

Erstellt einen neuen Sitzplatz.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Request Body:** `SeatRequestDTO`
-   **Responses:**
    -   `200 OK`: Sitzplatz erfolgreich erstellt. Gibt `SeatResponseDTO` zurück.
    -   `404 Not Found`: Zugehörige Location nicht gefunden oder keine Berechtigung.

---

#### GET /{id}

Ruft einen bestimmten Sitzplatz anhand seiner ID ab.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - Die ID des Sitzplatzes.
-   **Responses:**
    -   `200 OK`: Gibt `SeatResponseDTO` zurück.
    -   `404 Not Found`: Sitzplatz nicht gefunden oder keine Berechtigung.

---

#### PUT /{id}

Aktualisiert einen Sitzplatz.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - Die ID des Sitzplatzes.
-   **Request Body:** `SeatRequestDTO`
-   **Responses:**
    -   `200 OK`: Sitzplatz erfolgreich aktualisiert. Gibt `SeatResponseDTO` zurück.
    -   `404 Not Found`: Sitzplatz oder Location nicht gefunden oder keine Berechtigung.
---

## Reservierungsberechtigungen (Manager/Admin)

### EventUserReservationAllowance

Basispfad: `/api/manager/reservationAllowance`

---

#### POST /

Setzt oder aktualisiert die Anzahl der erlaubten Reservierungen für einen Benutzer für ein Event.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Request Body:** `EventUserAllowancesDto`
-   **Responses:**
    -   `200 OK`: Berechtigung erfolgreich gesetzt/aktualisiert. Gibt `EventUserAllowancesDto` zurück.
    -   `403 Forbidden`: Keine Berechtigung.
    -   `404 Not Found`: Event oder Benutzer nicht gefunden.

---

#### GET /{id}

Ruft eine spezifische Reservierungsberechtigung anhand ihrer ID ab.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - Die ID der Berechtigung.
-   **Responses:**
    -   `200 OK`: Gibt `EventUserAllowancesDto` zurück.
    -   `403 Forbidden`: Keine Berechtigung.
    -   `404 Not Found`: Berechtigung nicht gefunden.

---

#### GET /

Ruft alle Reservierungsberechtigungen ab.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Gibt eine Liste von `EventUserAllowancesDto` zurück.
    -   `403 Forbidden`: Keine Berechtigung.

---

#### GET /event/{eventId}

Ruft alle Reservierungsberechtigungen für ein bestimmtes Event ab.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `eventId` (Long) - Die ID des Events.
-   **Responses:**
    -   `200 OK`: Gibt eine Liste von `EventUserAllowancesDto` zurück.
    -   `403 Forbidden`: Keine Berechtigung.
    -   `404 Not Found`: Event nicht gefunden.

---

#### DELETE /{id}

Löscht eine Reservierungsberechtigung.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - Die ID der Berechtigung.
-   **Responses:**
    -   `204 No Content`: Berechtigung erfolgreich gelöscht.
    -   `403 Forbidden`: Keine Berechtigung.
    -   `404 Not Found`: Berechtigung nicht gefunden.

#### PUT /

Aktualisiert eine bestehende Reservierungsberechtigung.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Request Body:** `EventUserAllowanceUpdateDto`
-   **Responses:**
    -   `200 OK`: Berechtigung erfolgreich aktualisiert. Gibt `EventUserAllowancesDto` zurück.
    -   `403 Forbidden`: Keine Berechtigung.
    -   `404 Not Found`: Berechtigung nicht gefunden.

---

## Reservierungen (Manager/Admin)

### ReservationResource

Basispfad: `/api/manager/reservations`

---

#### GET /

Ruft alle Reservierungen für die Events des aktuellen Managers ab.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Gibt eine Liste von `DetailedReservationResponseDTO` Objekten zurück.

---

#### GET /{id}

Ruft eine bestimmte Reservierung anhand ihrer ID ab.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - Die ID der Reservierung.
-   **Responses:**
    -   `200 OK`: Gibt `DetailedReservationResponseDTO` zurück.
    -   `404 Not Found`: Reservierung nicht gefunden oder keine Berechtigung.

---

#### GET /event/{id}

Ruft alle Reservierungen für ein bestimmtes Event ab.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - Die ID des Events.
-   **Responses:**
    -   `200 OK`: Gibt eine Liste von `DetailedReservationResponseDTO` Objekten zurück.
    -   `403 Forbidden`: Keine Berechtigung für dieses Event.

---

#### POST /

Erstellt eine neue Reservierung.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Request Body:** `ReservationRequestDTO`
-   **Responses:**
    -   `200 OK`: Reservierung erfolgreich erstellt. Gibt `DetailedReservationResponseDTO` zurück.
    -   `404 Not Found`: Benutzer, Event oder Sitzplatz nicht gefunden.
    -   `409 Conflict`: Sitzplatz bereits reserviert.

---

#### PUT /{id}

Aktualisiert eine bestehende Reservierung.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - Die ID der Reservierung.
-   **Request Body:** `ReservationRequestDTO`
-   **Responses:**
    -   `200 OK`: Reservierung erfolgreich aktualisiert. Gibt `DetailedReservationResponseDTO` zurück.
    -   `404 Not Found`: Reservierung, Event oder Benutzer nicht gefunden oder keine Berechtigung.

---

#### DELETE /{id}

Löscht eine Reservierung.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - Die ID der Reservierung.
-   **Responses:**
    -   `200 OK`: Reservierung erfolgreich gelöscht.
    -   `404 Not Found`: Reservierung nicht gefunden oder keine Berechtigung.

---

#### POST /block

Blockiert eine beliebige Anzahl von Sitzen für ein Event.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Request Body:** `BlockSeatsRequestDTO`
    ```json
    {
      "eventId": "long",
      "seatIds": ["long"]
    }
    ```
-   **Responses:**
    -   `204 No Content`: Sitze erfolgreich blockiert.
    -   `403 Forbidden`: Keine Berechtigung.
    -   `404 Not Found`: Event oder Sitzplatz nicht gefunden.
    -   `409 Conflict`: Sitzplatz bereits reserviert oder blockiert.

---

#### GET /export/{eventId}/csv

Exportiert alle Reservierungen für ein bestimmtes Event als CSV-Datei.

-   **Rollen:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `eventId` (Long) - Die ID des Events.
-   **Produces:** `text/csv`
-   **Responses:**
    -   `200 OK`: CSV-Datei erfolgreich exportiert.
    -   `403 Forbidden`: Keine Berechtigung (Benutzer ist nicht der Manager des Events oder kein Admin).
    -   `404 Not Found`: Event nicht gefunden.
    -   `500 Internal Server Error`: Interner Serverfehler beim Export.

---

## Events (Benutzer)

### EventResource

Basispfad: `/api/user/events`

---

#### GET /

Ruft alle Events ab, für die der aktuelle Benutzer eine Berechtigung hat.

-   **Rollen:** `USER`
-   **Responses:**
    -   `200 OK`: Gibt eine Liste von `EventResponseDTO` Objekten zurück, inklusive der Anzahl erlaubter Reservierungen.

---

## Reservierungen (Benutzer)

### ReservationResource

Basispfad: `/api/user/reservations`

---

#### GET /

Ruft alle Reservierungen des aktuellen Benutzers ab.

-   **Rollen:** `USER`
-   **Responses:**
    -   `200 OK`: Gibt eine Liste von `ReservationResponseDTO` Objekten zurück.

---

#### GET /{id}

Ruft eine bestimmte Reservierung des aktuellen Benutzers ab.

-   **Rollen:** `USER`
-   **Path Parameter:** `id` (Long) - Die ID der Reservierung.
-   **Responses:**
    -   `200 OK`: Gibt `ReservationResponseDTO` zurück.
    -   `404 Not Found`: Reservierung gehört nicht dem Benutzer oder existiert nicht.

---

#### POST /

Erstellt eine oder mehrere neue Reservierungen für den aktuellen Benutzer.

-   **Rollen:** `USER`
-   **Request Body:** `ReservationsRequestCreateDTO`
-   **Responses:**
    -   `200 OK`: Reservierung(en) erfolgreich erstellt. Gibt eine Liste von `ReservationResponseDTO` zurück.
    -   `400 Bad Request`: Ungültige Anfrage (z.B. mehr Plätze als erlaubt).
    -   `403 Forbidden`: Keine Berechtigung für das Event.
    -   `404 Not Found`: Event oder Sitzplatz nicht gefunden.
    -   `409 Conflict`: Sitzplatz bereits reserviert.

---

#### DELETE /{id}

Löscht eine Reservierung des aktuellen Benutzers.

-   **Rollen:** `USER`
-   **Path Parameter:** `id` (Long) - Die ID der Reservierung.
-   **Responses:**
    -   `204 No Content`: Reservierung erfolgreich gelöscht.
    -   `404 Not Found`: Reservierung gehört nicht dem Benutzer oder existiert nicht.