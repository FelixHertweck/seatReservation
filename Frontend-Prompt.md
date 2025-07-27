**Anweisung für das LLM zur Frontend-Generierung**

**Frontend Aufbau:**
Das Frontend soll folgende Seiten haben:
Die Seite soll eine Sidebar links enthalten. Diese soll als Hauptnavigation dienen. Basierend auf den aktuellen Rollen solle für normale Nutzer nur Events, Meine Reservierungen und Profil angezeigt werden. Für Manager zusätzlich Manager und für Admins zusätzlich User Management.
Außerdem soll unten links der Name des Nutzer sowie seine Email Adresse stehen. Wenn er drauf klick soll er ein Dropdown Menu bekommen bei dem er sich ausloggen kann.
Wenn ein Nutzer nicht angemeldet ist. Soll er unten links die Möglichkeit haben sich anzumelden. und auf die /Login Seite weitergeleitet werden.
Die Sidebar soll folgendes Interface haben:
- user: UserDto
- Logout: () => void

**Login Seite.** Diese soll folgendem Beispiel entsprechen: https://ui.shadcn.com/blocks/login#login-04
Außerdem soll es eine Registrierung Seite geben. Die soll ähnlich wie die Login Seite aussehen nur entsprechend auf das Registrieren eines neuen Benutzers ausgerichtet sein
- Login: (username: string, Password: string) => Promise<void>

**Registrierungs Seite.** Diese soll ähnlich wie die Login Seite aussehen nur entsprechend auf das Registrieren eines neuen Benutzers ausgerichtet sein. Dabei soll es auch eine Möglichkeit geben sich als Admin zu registrieren.
- Register: (user: UserCreationDto) => Promise<void>

**Profil Seite.** Diese Seite soll es Nutzern ermöglichen ihre eigenen Nutzer Angaben einzusehen. Auch sollen sie dort ihre Angaben sowie Passwörter Updaten können.
- user: UserDto
- update: (id:bigint, user: UserProfileUpdateDto) => Promise<void>

**Auf der Meine Reservierungsseite** sollen Nutzer ihre bereits getätigten Reservierungen sehen können. Dazu sollen sie wie auf der Event Übersicht auch hier die Events sehen können. Es soll eine Möglichkeit geben diese zu Filtern und zu durchsuchen. Außerdem soll dabei stehen wie viel Sitze sie reserviert haben. Wenn sie diese dann öffnen um ihre reservierten Sitzplätze zu sehen sollen die Sitzplätze gerendert werden. Diese enthalten x und y Koordinaten. Diese geben die Position im Raum an. Dabei ist beispielsweise x=1 und y=1 und Platz x=2 y=1 neben dem ersten und x=1 y=2 hinter dem ersten. Dabei kann es beispielsweise auch frei Slot geben die einen Gang darstellen sollen. Das heißt x und y ist nicht zwingend die genaue Position sondern eher die Position zu den anderen Sitzplätze. Die Karte soll auch auf kleinen Geräten responsiv sein. Nutzer sollen auf der Seite auch die Möglichkeit haben bestehende Sitzplätze zu löschen. Zeige diese also irgendwie noch mit der Sitzplatznummer an und biete eine Möglichkeit diese zu löschen.
- events: Array<EventResponseDto>
- reservations: Array<ReservationResponseDto>
- deleteReservation(id: bigint) => Promise<void>

**Auf der Events Seite** sollen in so Karten die Events angezeigt werden. Dabei sollen alle Relevanten Infos wie Name Beschreibung Start Endzeit sowie Buchungsende angezeigt werden. Außerdem soll eine Batch angezeigt werden ob noch Sitzplätze gebucht werden können. Biete dem Nutzer auch eine Möglichkeit die Events zu Filtern so wie nach den Events zu suchen. Wenn man darauf klickt und eine Reservierung vorgenommen werden soll, wird wie bei der Seite meine Reservierungen die Sitzplätze gerendert werden. Dabei sollen bereits reservierte Sitzplätze sowie blockierte Sitzplätze anders Farbig angezeigt werden. Freie Sitzplätze sollen angeklickt werden können und in einer Art Warenkorb mit der Sitzplatznummer angezeigt werden. Dabei soll auch angezeigt werden wie viele von den freien Sitzplätzen noch verfügbar ist. Man soll auch nicht mehr als die Verfügbaren Sitzplätze auswählen können. Des weiteren soll es einen Bestätigungsbutton geben der dann die Sitzplätze reserviert.
- events: Array<EventResponseDto>
- createReservation: () => Promise<void>


**Auf der Admin Seite** soll das Komplette UserManagment passieren. das heißt es sollen alle vorhanden Nutzer angezeigt werden in einer Liste. Diese soll durchsuchbar und Filterbar sein. Man soll neue Nutzer hinzufügen können. auch sollen vorhandene Nutzer bearbeitbar und löschbar sein. Beispielsweise sollen nur Rollenauswählbar sein die existieren
Hier ist das Interface für die Admin Seite:
- users: Array<UserDto>
- availableRoles: Array<string>
- createUser: (user: UserCreationDto) => Promise<void>
- updateUser: (id: bigint, user: AdminUserUpdateDto) => Promise<void>
- deleteUser: (id: bigint) => Promise<void>


**Auf der Managment Seite** sollen Eventlocations Events und Sitze angezeigt werden und bearbeitet werden können. Diese sollen in einer Liste angezeigt werden und bearbeitet werden können. Es sollen über Formulare neue Elemente hinzugefügt werden können. Auchen sollen die Elemente über Formulare updatebar sein. Beachte dass wenn beispielsweise eine EventId mitgeschickt werden soll, dass dann auch nur vorhandene Events angezeigt werden. Auch soll ein Nutzer nicht direkt ids auswählen können sondern wenn möglich Namen angezeigt bekommen.
Erstelle außerdem einen Input von JSON der an den Endpoint createEventLocationWithSeats geht. Dabei soll der Nutzer eine JSON Datei für eine Eventlocation eingeben und das dann hinzugefügt werden.
AUch soll die Admin Seite noch einen weiteren Tab mit Reservierungen angezeigt werden. Diese sollen durchsucht und auch Filterbar sein insbesondere für ein Event. AUch sollen neue Reservierung über die Managment Reservation Endpoint erstellt werden können. Desweiteren sollen Reservierungen gelöscht und geupdatet werden können. Auch hier soll es Formulare dafür geben.
Hier das Interface für die Management Seite:
Event:
- events: Array<DetailedEventResponseDto>
- createEvent: (event: EventRequestDto) => Promise<DetailedEventResponseDto>
- updateEvent: (id: bigint, event: EventRequestDto) => Promise<DetailedEventResponseDto>
- deleteEvent: (id: bigint) => Promise<void>

Locations: 
- locations: Array<EventLocationResponseDto>
- createLocation: (location: EventLocationRequestDto) => Promise<EventLocationResponseDto>
- updateLocation: (id: bigint, location: EventLocationRequestDto) => Promise<EventLocationResponseDto>
- deleteLocation: (id: bigint) => Promise<void>
- registerLocationWithSeats: (data: EventLocationRegistrationDto) => Promise<void>

Seats:
- seats: Array<SeatResponseDto>
- createSeat: (seat: SeatRequestDto) => Promise<SeatResponseDto>
- updateSeat: (id: bigint, seat: SeatRequestDto) => Promise<SeatResponseDto>
- deleteSeat: (id: bigint) => Promise<void>

Reservation Allowance:
- reservationAllowance: Array<EventUserAllowancesDto>
- getReservationAllowanceByEventId: (eventId: bigint) => Promise<EventUserAllowancesDto[]>
- createReservationAllowance: (allowance: EventUserAllowancesDto) => Promise<void>
- deleteReservationAllowance: (id: bigint) => Promise<void>

Reservations:
- reservations: Array<DetailedReservationResponseDto>
- createReservation: (reservation: ReservationRequestDto) => Promise<DeleteApiManagerReservationsByIdData>
- updateReservation: (id: bigint, reservation: ReservationRequestDto) => Promise<DetailedReservationResponseDto>
- deleteReservation: (id: bigint) => Promise<void>
- blockSeats: (request: BlockSeatsRequestDto) => Promise<void>


**Anbei findest du eine Dokumentation über die Endpoints. Außerdem findest du die im Frontend befindliche API Client. Hier findest du auch eine Erklärung für die Endpoints angepasst auf das was ich dir oben beschrieben habe:**

### API-Endpunkt-Dokumentation

Hier ist eine Zusammenfassung der verfügbaren Backend-Endpunkte, die du für die Implementierung des Frontends verwenden sollst.

#### Authentifizierung (`/api/auth`)
*   **`POST /login`**: Authentifiziert einen Benutzer.
    *   **Request:** `LoginRequestDTO` (`username`, `password`)
    *   **Response:** `LoginResponseDTO` (`token`)
    *   **Zugriff:** Öffentlich

#### Benutzerverwaltung (`/api/users`, `/api/user`)
*   **`GET /me`**: Ruft die Daten des angemeldeten Benutzers ab.
    *   **Response:** `UserDTO`
    *   **Zugriff:** `USER`, `MANAGER`, `ADMIN`
*   **`PUT /me`**: Aktualisiert das Profil des angemeldeten Benutzers.
    *   **Request:** `UserProfileUpdateDTO`
    *   **Response:** `UserDTO`
    *   **Zugriff:** `USER`
*   **`GET /admin/{id}`**: Ruft einen Benutzer für Admins ab.
    *   **Response:** `UserDTO`
    *   **Zugriff:** `ADMIN`
*   **`POST /admin`**: Erstellt einen neuen Benutzer.
    *   **Request:** `UserCreationDTO`
    *   **Response:** `UserDTO`
    *   **Zugriff:** `ADMIN`
*   **`PUT /admin/{id}`**: Aktualisiert einen Benutzer.
    *   **Request:** `AdminUserUpdateDTO`
    *   **Response:** `UserDTO`
    *   **Zugriff:** `ADMIN`
*   **`DELETE /admin/{id}`**: Löscht einen Benutzer.
    *   **Zugriff:** `ADMIN`
*   **`GET /manager`**: Ruft eine Liste aller Benutzer für Manager ab.
    *   **Response:** `List<LimitedUserInfoDTO>`
    *   **Zugriff:** `MANAGER`, `ADMIN`
*   **`GET /roles`**: Ruft alle verfügbaren Benutzerrollen ab.
    *   **Response:** `List<String>`
    *   **Zugriff:** `USER`, `MANAGER`, `ADMIN`

#### Events & Reservierungen (Benutzer) (`/api/user`)
*   **`GET /events`**: Ruft alle verfügbaren Events für den Benutzer ab.
    *   **Response:** `List<EventResponseDTO>`
    *   **Zugriff:** `USER`
*   **`GET /reservations`**: Ruft alle Reservierungen des Benutzers ab.
    *   **Response:** `List<ReservationResponseDTO>`
    *   **Zugriff:** `USER`
*   **`POST /reservations`**: Erstellt neue Reservierungen.
    *   **Request:** `ReservationsRequestCreateDTO`
    *   **Response:** `List<ReservationResponseDTO>`
    *   **Zugriff:** `USER`
*   **`DELETE /reservations/{id}`**: Löscht eine Reservierung.
    *   **Zugriff:** `USER`

#### Management (`/api/manager`)
*   **Reservierungen:**
    *   **`GET /reservations`**: Alle Reservierungen abrufen.
    *   **`POST /reservations`**: Reservierung erstellen (`ReservationRequestDTO`).
    *   **`PUT /reservations/{id}`**: Reservierung aktualisieren (`ReservationRequestDTO`).
    *   **`DELETE /reservations/{id}`**: Reservierung löschen.
    *   **`POST /reservations/block`**: Sitzplätze blockieren (`BlockSeatsRequestDTO`).
*   **Sitze:**
    *   **`GET /seats`**: Alle Sitze abrufen.
    *   **`POST /seats`**: Sitz erstellen (`SeatRequestDTO`).
    *   **`PUT /seats/{id}`**: Sitz aktualisieren (`SeatRequestDTO`).
    *   **`DELETE /seats/{id}`**: Sitz löschen.
*   **Event-Locations und Events werden über ähnliche Endpunkte unter `/api/manager/eventlocations` und `/api/manager/events` verwaltet.**


**Im Allgemeinen soll die Anwendung sehr modern sein und leicht bedienbar für Nutzer sein. Für eine gute Maintananability sollte möglichst gut der Code strukturiert sein. Verwende also Interfaces und trenne die Methoden und Komponenten sinnvoll. Trenne außerdem die aussehens Komponenten und Businesslogik**
**Implementiere die Styled Komponenten, so dass die Anwendung modern aussieht. Verwende dafür die Shadcn UI Komponenten. Du sollst keine API Anfragen einbauen sondern nur die Interfaces und Methoden. Implementiere für jede Seite interfaces wie oben beschrieben. Verwende diese Daten um die Sachen darzustellen.**