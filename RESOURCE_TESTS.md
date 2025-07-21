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
Ruft alle Events ab, für die der aktuelle Benutzer eine Berechtigung hat.

**Beschreibung:**

Dieser Test stellt sicher, dass ein Benutzer eine Liste der für ihn verfügbaren Events abrufen kann.

**Testfälle:**

*   **Erfolg:**
    *   Ein authentifizierter Benutzer mit Berechtigungen für Events ruft die Liste ab und erhält `200 OK` mit den Event-Daten.
    *   Ein authentifizierter Benutzer ohne Berechtigungen für Events ruft die Liste ab und erhält `200 OK` mit einer leeren Liste.
*   **Fehler:**
    *   Ein nicht authentifizierter Benutzer versucht, auf den Endpunkt zuzugreifen, und erhält `401 Unauthorized`.

---

#### GET /available-seats/{id}

Ruft die Anzahl der verfügbaren Plätze für ein bestimmtes Event ab.

**Beschreibung:**

Dieser Test überprüft, ob die korrekte Anzahl der verbleibenden Plätze für ein Event zurückgegeben wird, für das der Benutzer eine Berechtigung hat.

**Testfälle:**

*   **Erfolg:**
    *   Ein Benutzer fragt die verfügbaren Plätze für ein Event an, für das er eine Berechtigung hat, und erhält `200 OK` mit einer Ganzzahl.
*   **Fehler:**
    *   Ein Benutzer fragt die Plätze für ein Event an, für das er keine Berechtigung hat, und erhält `404 Not Found`.
    *   Ein Benutzer fragt die Plätze für ein nicht existierendes Event an und erhält `404 Not Found`.
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
    *   Ein registrierter und bestätigter Benutzer sendet gültige Anmeldeinformationen (Benutzername/E-Mail und Passwort) und erhält einen `200 OK`-Status mit einem JWT-Token.
*   **Fehler:**
    *   Ein Benutzer sendet ungültige Anmeldeinformationen (falscher Benutzername oder falsches Passwort) und erhält einen `401 Unauthorized`-Status.
    *   Ein Benutzer, dessen E-Mail-Adresse noch nicht bestätigt wurde, versucht sich anzumelden und erhält `401 Unauthorized`.
    *   Die Anfrage hat ein ungültiges Format (z. B. fehlendes Passwort) und erhält einen `400 Bad Request`-Status.