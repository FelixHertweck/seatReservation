# Cross-Domain Service Coupling – Refactoring zu CDI Events

Dieses Dokument beschreibt alle bekannten Stellen im Projekt, an denen Services direkt in eine fremde Domäne injiziert werden, obwohl eine lose Kopplung über CDI Events (`jakarta.enterprise.event.Event`) sauberer wäre.

## Warum CDI Events?

| Direkte Injection (Status quo) | CDI Events (Ziel) |
|---|---|
| Aufrufer importiert Klassen der Ziel-Domäne | Aufrufer kennt nur sein eigenes Event-POJO |
| Neue Integrations-Empfänger erfordern Änderungen am Aufrufer | Neuer Empfänger: einfach `@Observes` hinzufügen, Aufrufer bleibt unberührt |
| Direkte Abhängigkeitskette erschwert Unit-Tests | Empfänger einzeln testbar, kein Mocking fremder Services nötig |
| Zirkuläre Abhängigkeiten möglich | Strukturell unmöglich |

CDI Events in Quarkus werden mit `@Inject Event<MyEvent> event; event.fire(new MyEvent(...))` ausgelöst. Für asynchrone Ausführung (z.B. externe API-Calls) `event.fireAsync(...)` verwenden.

---

## Bekannte Cross-Domain-Kopplungen

### 🔴 Priorität Hoch

#### 1. `management.EventService` → `email.NotificationService`
- **Datei**: `management/service/EventService.java`
- **Aufrufe**: `notificationService.scheduleEventReminder(event)`, `notificationService.cancelEventReminder(...)`
- **Ziel-Events**:
  - `EventCreatedEvent(Event event)`
  - `EventUpdatedEvent(Event event)` ← wird auch für Google Wallet Sync genutzt (siehe unten)
  - `EventDeletedEvent(UUID eventId)`

#### 2. `reservation.ReservationService` → `email.EmailService`
- **Datei**: `reservation/service/ReservationService.java`
- **Aufrufe**: Bestätigungs-E-Mail beim Erstellen/Stornieren von Reservierungen
- **Ziel-Events**:
  - `ReservationCreatedEvent(Reservation reservation, User user)`
  - `ReservationCancelledEvent(Reservation reservation, User user)`

#### 3. `supervisor.BoxOfficeService` → `email.EmailService` — ⚠️ kein CDI-Event geeignet
- **Datei**: `supervisor/service/BoxOfficeService.java`
- **Aufrufe**: `emailService.sendBoxOfficeConfirmation(...)`
- **Warum kein Event**: `sendBoxOfficeConfirmation(...)` liefert `BoxOfficeConfirmationContent` (u.a.
  `displayHtml()`) zurück, das `BoxOfficeService` synchron in dieselbe HTTP-Response einbaut (Druck-Beleg
  für den Schalter). `event.fire()`/`fireAsync()` sind aber grundsätzlich `void` bzw. liefern keinen
  Rückgabewert eines Observers zurück – das Pattern passt nur für Fire-and-forget-Seiteneffekte.
  Ein Split (Rendering direkt aufrufen + Versand per Event) würde QR-Code-Generierung und
  Seatmap-Token-Erzeugung doppelt ausführen (u.a. doppelte `EmailSeatMapToken`-Zeilen in der DB) – das
  Risiko wiegt den Kopplungs-Vorteil hier nicht auf. Bleibt bewusst als direkte Injection bestehen.

#### 4. `email.EmailSeatMapResource` → `wallet.WalletPassService` — ⚠️ kein CDI-Event geeignet
- **Datei**: `email/resource/EmailSeatMapResource.java`
- **Aufrufe**: `walletPassService.generatePass(...)` für Apple/Google-Wallet-Download
- **Warum kein Event**: Der Resource-Endpunkt braucht das generierte `WalletPassResponseDTO` synchron,
  um die HTTP-Response (Datei-Download bzw. Redirect) zu bauen. Gleiches Problem wie bei Punkt 3 – ein
  CDI-Event ohne Rückgabewert kann das nicht abbilden. Der allgemeine Hinweis "Resource-Klassen sollten
  keine fremden Domain-Services kennen" bleibt gültig, ist hier aber eher ein Fall für eine dünne
  Fassade *innerhalb* der `email`-Domäne (die intern `WalletPassService` injiziert) als für CDI Events.

### 🟢 Akzeptabel (gleiche Domäne, kein Refactoring nötig)

#### 5. `supervisor.BoxOfficeService` → `supervisor.LiveViewService`
- Gleiche `supervisor`-Domäne, direkte Kopplung ist hier vertretbar.

#### 6. `supervisor.CheckInService` → `supervisor.LiveViewService`
- Gleiche `supervisor`-Domäne, direkte Kopplung ist hier vertretbar.

---

## Ziel-Paketstruktur für CDI Events

```
de.felixhertweck.seatreservation.common.events
├── EventCreatedEvent.java
├── EventUpdatedEvent.java
├── EventDeletedEvent.java
├── ReservationCreatedEvent.java
├── ReservationCancelledEvent.java
└── BoxOfficeReservationCreatedEvent.java
```

Alle Event-POJOs sind einfache `record`s ohne Business-Logik:

```java
// Beispiel
public record EventUpdatedEvent(UUID eventId, String eventName, String locationName,
                                 String locationAddress, Instant startTime, Instant endTime) {}
```

---

## Migrations-Anleitung (pro Kopplung)

1. **Event-POJO erstellen** in `common.events`
2. **`event.fire(new XyzEvent(...))`** im Aufrufer ersetzen (`@Inject Event<XyzEvent>`)
3. **`@Observes` im Empfänger** hinzufügen (bisherige Logik bleibt gleich)
4. Direkte `@Inject`-Abhängigkeit im Aufrufer **entfernen**
5. Tests anpassen

> **Hinweis**: Bei externen API-Calls (z.B. Google Wallet PATCH) `event.fireAsync()` verwenden, damit die REST-Antwort nicht auf den externen Call wartet.

> **Tipp**: Schrittweise Migration möglich – jede Kopplung ist unabhängig migrierbar ohne andere zu beeinflussen.

---

## Status

| Kopplung | Status |
|---|---|
| `management.EventService` → `wallet` (Google Wallet Sync) | ✅ Erledigt – CDI Event implementiert |
| `management.EventService` → `email.NotificationService` | ✅ Erledigt – `EventCreatedEvent`/`EventUpdatedEvent`/`EventDeletedEvent` |
| `reservation.ReservationService` → `email.EmailService` | ✅ Erledigt – `ReservationCreatedEvent`/`ReservationCancelledEvent` (synchrones `fire()`) |
| `supervisor.BoxOfficeService` → `email.EmailService` | ❌ Bewusst nicht migriert – Rückgabewert wird synchron für die HTTP-Response gebraucht, siehe Punkt 3 |
| `email.EmailSeatMapResource` → `wallet.WalletPassService` | ❌ Bewusst nicht migriert – Rückgabewert wird synchron für die HTTP-Response gebraucht, siehe Punkt 4 |
