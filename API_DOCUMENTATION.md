# API Documentation

This is the API documentation for the seat reservation system.

## Table of Contents

- [Authentication](#authentication)
- [User Management (Admin)](#user-management-admin)
- [User Profile (User)](#user-profile-user)
- [Email Confirmation](#email-confirmation)
- [Email Seat Map](#email-seat-map)
- [Event Locations (Manager/Admin)](#event-locations-manageradmin)
- [Events (Manager/Admin)](#events-manageradmin)
- [Seats (Manager/Admin)](#seats-manageradmin)
- [Reservations (Manager/Admin)](#reservations-manageradmin)
- [Event Locations (User)](#event-locations-user)
- [Events (User)](#events-user)
- [Reservations (User)](#reservations-user)

---

## Authentication

### AuthResource

Base path: `/api/auth`

---

#### POST /login

Authenticates a user and sets a JWT cookie.

-   **Roles:** Public
-   **Request Body:** `LoginRequestDTO`
-   **Responses:**
    -   `200 OK`: Successful login. Sets a `jwt` cookie.
    -   `401 Unauthorized`: Invalid credentials or email not confirmed.

---

#### POST /register

Registers a new user and sets a JWT cookie.

-   **Roles:** Public
-   **Request Body:** `RegisterRequestDTO`
-   **Responses:**
    -   `200 OK`: Successful registration. Sets a `jwt` cookie and triggers an email verification flow.
    -   `400 Bad Request`: Invalid data (e.g., missing fields, password too short, invalid email format, username does not match pattern).
    -   `409 Conflict`: User with this username already exists.
    -   `403 Forbidden`: User registration is currently disabled.

**Example Error Response (403 Forbidden):**
```json
{
  "message": "User registration is currently disabled"
}
```

**Example Error Response (409 Conflict):**
```json
{
  "message": "User with username 'john_doe' already exists."
}
```

---

#### POST /logout

Logs out the current user by clearing the JWT cookie.

-   **Roles:** Authenticated
-   **Responses:**
    -   `200 OK`: Successful logout. Clears the `jwt` cookie.

---

#### GET /registration-status

Retrieves the current registration status of the application.

-   **Roles:** Public
-   **Response Body:** `RegistrationStatusDTO`
-   **Responses:**
    -   `200 OK`: Registration status retrieved successfully. Returns `RegistrationStatusDTO` with `enabled` flag.

**Example Response:**
```json
{
  "enabled": true
}
```

**Description:** 
This endpoint allows clients to check whether user registration is currently enabled or disabled in the application. The registration status is controlled by the configuration property `registration.enabled` (default: `true`). When registration is disabled, attempts to call the `/register` endpoint will return a `403 Forbidden` status.

---

#### POST /logoutAllDevices

Logs out the current user from all devices by invalidating all refresh tokens.

-   **Roles:** Authenticated
-   **Responses:**
    -   `200 OK`: Successfully logged out from all devices. Clears all authentication cookies.

---

#### POST /refresh

Refreshes the JWT token using a valid refresh token.

-   **Roles:** Public (but requires valid refresh token cookie)
-   **Request Cookie:** `refreshToken`
-   **Responses:**
    -   `200 OK`: Token successfully refreshed. Returns new JWT and refresh token cookies.
    -   `401 Unauthorized`: Invalid or missing refresh token.

---

## User Management (Admin)

### UserResource

Base path: `/api/users`

---

#### POST /admin/import

Imports a set of users.

-   **Roles:** `ADMIN`
-   **Request Body:** `Set<AdminUserCreationDto>`
-   **Responses:**
    -   `200 OK`: Users successfully imported. Returns a list of `UserDTO` objects.
    -   `400 Bad Request`: Invalid format.
    -   `403 Forbidden`: Access denied (role is not `ADMIN`).
    -   `401 Unauthorized`: Not authenticated.

---

#### POST /admin

Creates a new user.

-   **Roles:** `ADMIN`
-   **Request Body:** `AdminUserCreationDto` (now includes `sendEmailVerification` field)
-   **Responses:**
-       `201 Created`: User successfully created. Returns `UserDTO`.
-       `400 Bad Request`: Invalid data (e.g., duplicate username).
-       `403 Forbidden`: Access denied (role is not `ADMIN`).
-       `409 Conflict`: User with this username already exists.
    -   `401 Unauthorized`: Not authenticated.

---

#### PUT /admin/{id}

Updates an existing user.

-   **Roles:** `ADMIN`
-   **Path Parameter:** `id` (Long) - The user ID.
-   **Request Body:** `AdminUserUpdateDTO`
-   **Responses:**
    -   `200 OK`: User successfully updated. Returns `UserDTO`.
    -   `404 Not Found`: User not found.
    -   `403 Forbidden`: Access denied.
    -   `401 Unauthorized`: Not authenticated.
    -   `409 Conflict`: User with this username already exists.

---

#### DELETE /admin/{id}

Deletes a user.

-   **Roles:** `ADMIN`
-   **Path Parameter:** `id` (Long) - The user ID.
-   **Responses:**
    -   `200 OK`: User successfully deleted.
    -   `404 Not Found`: User not found.
    -   `403 Forbidden`: Access denied.
    -   `401 Unauthorized`: Not authenticated.

---

#### GET /manager

Retrieves a list of all users with limited information.

-   **Roles:** `ADMIN`, `MANAGER`
-   **Responses:**
    -   `200 OK`: Returns a list of `LimitedUserInfoDTO` objects.
    -   `403 Forbidden`: Access denied.
    -   `401 Unauthorized`: Not authenticated.

---

#### GET /roles

Retrieves all available user roles.

-   **Roles:** `USER`, `ADMIN`, `MANAGER`
-   **Responses:**
    -   `200 OK`: Returns a list of available roles (Strings).
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.

---

#### GET /admin

Retrieves complete data of all users.

-   **Roles:** `ADMIN`
-   **Responses:**
    -   `200 OK`: Returns a list of `UserDTO` objects.
    -   `403 Forbidden`: Access denied.
    -   `401 Unauthorized`: Not authenticated.

---

## User Profile (User)

### UserResource

Base path: `/api/users`

---

#### PUT /me

Updates the profile of the currently logged-in user.

-   **Roles:** `USER`
-   **Request Body:** `UserProfileUpdateDTO`
-   **Responses:**
    -   `200 OK`: Profile successfully updated. Returns `UserDTO`.
    -   `400 Bad Request`: Invalid data.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
    -   `409 Conflict`: User with this username already exists.

---

#### GET /me

Retrieves data of the currently logged-in user.

-   **Roles:** `USER`, `ADMIN`, `MANAGER`
-   **Responses:**
    -   `200 OK`: Returns the `UserDTO` object of the current user.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.

---

## Email Confirmation

### EmailConfirmationResource

Base path: `/api/user`

---

#### POST /resend-email-confirmation

Resends the email confirmation for the authenticated user and extends the token's lifetime.

-   **Roles:** Authenticated
-   **Responses:**
    -   `204 No Content`: Email confirmation resent successfully.
    -   `401 Unauthorized`: Not authenticated.
    -   `404 Not Found`: User not found.
    -   `500 Internal Server Error`: Internal server error.

---

#### POST /verify-email-code

Verifies a user's email address using a 6-digit verification code.

-   **Roles:** Public
-   **Request Body:** `VerifyEmailCodeRequestDto`
-   **Responses:**
    -   `200 OK`: Email verified successfully. Returns `VerifyEmailCodeResponseDto`.
    -   `400 Bad Request`: Invalid verification code.
    -   `410 Gone`: Verification code expired.
    -   `500 Internal Server Error`: Internal server error.

---

## Email Seat Map

### EmailSeatMapResource

Base path: `/api/email/seatmap`

---

#### GET /

Retrieves the seat map as SVG for a given email token. This endpoint is used to display the personalized seat map from email links.

-   **Roles:** Public
-   **Query Parameters:**
    -   `token` (string, required): The unique token from the email link
-   **Produces:** `image/svg+xml`
-   **Responses:**
    -   `200 OK`: Returns the seat map as an SVG image. The SVG highlights the user's newly reserved seats in blue, previously reserved seats in orange, and available seats in gray.
    -   `404 Not Found`: Token not found, invalid, or expired.

**Description:**  
When a user makes a reservation, they receive an email containing a personalized seat map. The email includes a PNG image and a link to this endpoint. The token is valid for 30 days (configurable via `email.seatmap.token.expiration.days`). The SVG dynamically adjusts its size based on the seat layout and is rendered at high resolution (144 DPI) for optimal quality.

**Color Coding:**
- **Blue (#2B7FFF)**: Newly reserved seats
- **Orange (#F0B100)**: Previously reserved seats by the same user
- **Gray (#CCCCCC)**: Available seats

**Example Request:**
```
GET /api/email/seatmap?token=123e4567-e89b-12d3-a456-426614174000
```

---

## Event Locations (Manager/Admin)

### EventLocationResource

Base path: `/api/manager/eventlocations`

---

#### GET /

Retrieves all event locations for the current manager.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Returns a list of `EventLocationResponseDTO` objects.

---
 
#### GET /
 
Retrieves all event locations for the current manager.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Returns a list of `EventLocationResponseDTO` objects.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
 
---
 
#### POST /
 
Creates a new event location.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Request Body:** `EventLocationRequestDTO`
-   **Responses:**
    -   `200 OK`: Location successfully created. Returns `EventLocationResponseDTO`.
    -   `400 Bad Request`: Invalid input data.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
    -   `409 Conflict`: Event location with this name already exists.
 
---
 
#### PUT /{id}
 
Updates an existing event location.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The event location ID.
-   **Request Body:** `EventLocationRequestDTO`
-   **Responses:**
    -   `200 OK`: Location successfully updated. Returns `EventLocationResponseDTO`.
    -   `404 Not Found`: Location not found or no permission.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
    -   `409 Conflict`: Event location with this name already exists.
 
---
 
#### DELETE /
 
Deletes one or more event locations.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Query Parameter:** `ids` (List<Long>) - The event location IDs to delete.
-   **Responses:**
    -   `200 OK`: Location(s) successfully deleted.
    -   `204 No Content`: Location(s) successfully deleted.
    -   `404 Not Found`: One or more locations not found or no permission.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
 
---
 
#### POST /import
 
Creates a new event location along with a list of seats.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Request Body:** `ImportEventLocationDto`
-   **Responses:**
    -   `200 OK`: Location and seats successfully created. Returns `EventLocationResponseDTO`.
    -   `400 Bad Request`: Invalid input data.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
    -   `409 Conflict`: Event location with this name already exists.
 
---
 
#### POST /import/{id}
 
Imports a list of seats to an existing event location.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The event location ID.
-   **Request Body:** `Set<ImportSeatDto>`
-   **Responses:**
    -   `200 OK`: Seats successfully imported. Returns `EventLocationResponseDTO`.
    -   `400 Bad Request`: Invalid input data.
    -   `403 Forbidden`: No permission.
    -   `404 Not Found`: Location not found.
    -   `401 Unauthorized`: Not authenticated.
 
---

## Events (Manager/Admin)

### EventResource

Base path: `/api/manager/events`

---

#### GET /

Retrieves all events managed by the current manager.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Returns a list of `DetailedUserEventResponseDTO` objects.

---
 
#### GET /
 
Retrieves all events managed by the current manager.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Returns a list of `DetailedUserEventResponseDTO` objects.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
 
---
 
#### GET /{id}
 
Retrieves a specific event by its ID.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The event ID.
-   **Responses:**
    -   `200 OK`: Returns `DetailedUserEventResponseDTO`.
    -   `403 Forbidden`: No permission.
    -   `404 Not Found`: Event not found.
    -   `401 Unauthorized`: Not authenticated.
 
---
 
#### POST /
 
Creates a new event.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Request Body:** `EventRequestDTO`
-   **Responses:**
    -   `200 OK`: Event successfully created. Returns `DetailedUserEventResponseDTO`.
    -   `400 Bad Request`: Invalid data.
    -   `404 Not Found`: Associated location not found.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
    -   `409 Conflict`: Event with this name already exists in this event location.
 
---
 
#### PUT /{id}
 
Updates an existing event.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The event ID.
-   **Request Body:** `EventRequestDTO`
-   **Responses:**
    -   `200 OK`: Event successfully updated. Returns `DetailedUserEventResponseDTO`.
    -   `404 Not Found`: Event not found or no permission.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
    -   `409 Conflict`: Event with this name already exists in this event location.
 
---
 
#### DELETE /
 
Deletes one or more events.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Query Parameter:** `ids` (List<Long>) - The event IDs to delete.
-   **Responses:**
    -   `204 No Content`: Event(s) successfully deleted.
    -   `403 Forbidden`: No permission to delete.
    -   `404 Not Found`: One or more events not found.
    -   `401 Unauthorized`: Not authenticated.
 
---

## Seats (Manager/Admin)

### SeatResource

Base path: `/api/manager/seats`

---

#### GET /

Retrieves all seats belonging to the current manager's locations.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Returns a list of `SeatDTO` objects.

---
 
#### GET /
 
Retrieves all seats belonging to the current manager's locations.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Returns a list of `SeatDTO` objects.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
 
---
 
#### POST /
 
Creates a new seat.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Request Body:** `SeatRequestDTO`
-   **Responses:**
    -   `200 OK`: Seat successfully created. Returns `SeatDTO`.
    -   `404 Not Found`: Associated location not found or no permission.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
    -   `409 Conflict`: Seat with this row and number already exists in this event location.
 
---
 
#### GET /{id}
 
Retrieves a specific seat by its ID.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The seat ID.
-   **Responses:**
    -   `200 OK`: Returns `SeatDTO`.
    -   `404 Not Found`: Seat not found or no permission.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
 
---
 
#### PUT /{id}
 
Updates a seat.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The seat ID.
-   **Request Body:** `SeatRequestDTO`
-   **Responses:**
    -   `200 OK`: Seat successfully updated. Returns `SeatDTO`.
    -   `404 Not Found`: Seat or location not found or no permission.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
    -   `409 Conflict`: Seat with this row and number already exists in this event location.
 
---
 
#### DELETE /
 
Deletes one or more seats.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Query Parameter:** `ids` (List<Long>) - The seat IDs to delete.
-   **Responses:**
    -   `200 OK`: Seat(s) successfully deleted.
    -   `204 No Content`: Seat(s) deleted successfully.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
    -   `404 Not Found`: One or more seats not found for the current manager.
 
---

## Reservation Permissions (Manager/Admin)

### EventUserReservationAllowance

Base path: `/api/manager/reservationAllowance`

---

#### POST /

Sets or updates the number of allowed reservations for a user for an event.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Request Body:** `EventUserAllowancesDto`
-   **Responses:**
    -   `200 OK`: Permission successfully set/updated. Returns `EventUserAllowancesDto`.
    -   `403 Forbidden`: No permission.
    -   `404 Not Found`: Event or user not found.

---
 
#### POST /
 
Sets or updates the number of allowed reservations for a user for an event.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Request Body:** `EventUserAllowancesCreateDto`
-   **Responses:**
    -   `200 OK`: Permission successfully set/updated. Returns `Set<EventUserAllowancesDto>`.
    -   `403 Forbidden`: No permission.
    -   `404 Not Found`: Event or user not found.
    -   `401 Unauthorized`: Not authenticated.
    -   `409 Conflict`: Allowance already exists for this user and event.
 
---
 
#### PUT /
 
Updates an existing reservation permission.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Request Body:** `EventUserAllowanceUpdateDto`
-   **Responses:**
    -   `200 OK`: Permission successfully updated. Returns `EventUserAllowancesDto`.
    -   `403 Forbidden`: No permission.
    -   `404 Not Found`: Permission not found.
    -   `401 Unauthorized`: Not authenticated.
    -   `409 Conflict`: Allowance already exists for this user and event.
 
---
 
#### GET /{id}
 
Retrieves a specific reservation permission by its ID.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The permission ID.
-   **Responses:**
    -   `200 OK`: Returns `EventUserAllowancesDto`.
    -   `403 Forbidden`: No permission.
    -   `404 Not Found`: Permission not found.
    -   `401 Unauthorized`: Not authenticated.
 
---
 
#### GET /
 
Retrieves all reservation permissions.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Returns a list of `EventUserAllowancesDto`.
    -   `403 Forbidden`: No permission.
    -   `401 Unauthorized`: Not authenticated.
 
---
 
#### GET /event/{eventId}
 
Retrieves all reservation permissions for a specific event.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `eventId` (Long) - The event ID.
-   **Responses:**
    -   `200 OK`: Returns a list of `EventUserAllowancesDto`.
    -   `403 Forbidden`: No permission.
    -   `404 Not Found`: Event not found.
    -   `401 Unauthorized`: Not authenticated.
 
---
 
#### DELETE /
 
Deletes one or more reservation permissions.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Query Parameter:** `ids` (List<Long>) - The permission IDs to delete.
-   **Responses:**
    -   `204 No Content`: Permission(s) successfully deleted.
    -   `403 Forbidden`: No permission.
    -   `404 Not Found`: One or more permissions not found.
    -   `401 Unauthorized`: Not authenticated.
 
---

## Reservations (Manager/Admin)

### ReservationResource

Base path: `/api/manager/reservations`

---

#### GET /

Retrieves all reservations for the current manager's events.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Returns a list of `DetailedUserReservationResponseDTO` objects.

---
 
#### GET /
 
Retrieves all reservations for the current manager's events.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Returns a list of `DetailedUserReservationResponseDTO` objects.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
 
---
 
#### GET /{id}
 
Retrieves a specific reservation by its ID.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The reservation ID.
-   **Responses:**
    -   `200 OK`: Returns `DetailedUserReservationResponseDTO`.
    -   `404 Not Found`: Reservation not found or no permission.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
 
---
 
#### GET /event/{id}
 
Retrieves all reservations for a specific event.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The event ID.
-   **Responses:**
    -   `200 OK`: Returns a list of `DetailedUserReservationResponseDTO` objects.
    -   `403 Forbidden`: No permission for this event.
    -   `401 Unauthorized`: Not authenticated.
 
---
 
#### POST /
 
Creates a new reservation.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Request Body:** `ReservationRequestDTO`
-   **Responses:**
    -   `200 OK`: Reservation successfully created. Returns `Set<DetailedUserReservationResponseDTO>`.
    -   `404 Not Found`: User, event, or seat not found.
    -   `409 Conflict`: Seat already reserved or event booking closed.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: No permission.
 
---
 
#### PUT /{id}
 
Updates an existing reservation.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The reservation ID.
-   **Request Body:** `ReservationRequestDTO`
-   **Responses:**
    -   `200 OK`: Reservation successfully updated. Returns `DetailedUserReservationResponseDTO`.
    -   `404 Not Found`: Reservation, event, or user not found or no permission.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
 
---
 
#### DELETE /
 
Deletes one or more reservations.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Query Parameter:** `ids` (List<Long>) - The reservation IDs to delete.
-   **Responses:**
    -   `204 No Content`: Reservation(s) successfully deleted.
    -   `404 Not Found`: One or more reservations not found or no permission.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
 
---
 
#### POST /block
 
Blocks any number of seats for an event.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Request Body:** `BlockSeatsRequestDTO`
-   **Responses:**
    -   `200 OK`: Seats successfully blocked. Returns `Set<DetailedUserReservationResponseDTO>`.
    -   `204 No Content`: Seats successfully blocked.
    -   `403 Forbidden`: No permission.
    -   `404 Not Found`: Event or seat not found.
    -   `409 Conflict`: Seat already reserved or blocked.
    -   `401 Unauthorized`: Not authenticated.
 
---

#### GET /export/{eventId}/csv

Exports all reservations for a specific event as a CSV file.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `eventId` (Long) - The event ID.
-   **Produces:** `text/csv`
-   **Responses:**
    -   `200 OK`: CSV file successfully exported.
    -   `403 Forbidden`: No permission (user is not the event manager or not an admin).
    -   `404 Not Found`: Event not found.
    -   `500 Internal Server Error`: Internal server error during export.

---

#### GET /export/{eventId}/pdf

Exports all reservations for a specific event as a PDF file.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `eventId` (Long) - The event ID.
-   **Produces:** `application/pdf`
-   **Responses:**
    -   `200 OK`: PDF file successfully exported.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: No permission (user is not the event manager or not an admin).
    -   `404 Not Found`: Event not found.
    -   `500 Internal Server Error`: Internal server error during export.

---
 
## Event Locations (User)

### EventLocationResource

Base path: `/api/user/locations`

---

#### GET /

Retrieves all event locations for which the current user has permissions or reservations.

-   **Roles:** `USER`, `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Returns a list of `UserEventLocationResponseDTO` objects.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.

---

## Events (User)
 
### EventResource
 
Base path: `/api/user/events`
 
---
 
#### GET /
 
Retrieves all events for which the current user has permission.
 
-   **Roles:** `USER`, `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Returns a list of `UserEventResponseDTO` objects, including the number of allowed reservations.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
 
---

## Reservations (User)

### ReservationResource

Base path: `/api/user/reservations`

---

#### GET /

Retrieves all reservations of the current user.

-   **Roles:** `USER`
-   **Responses:**
    -   `200 OK`: Returns a list of `UserReservationResponseDTO` objects.

---
 
#### GET /
 
Retrieves all reservations of the current user.
 
-   **Roles:** `USER`, `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Returns a list of `UserReservationResponseDTO` objects.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
 
---
 
#### GET /{id}
 
Retrieves a specific reservation of the current user.
 
-   **Roles:** `USER`, `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The reservation ID.
-   **Responses:**
    -   `200 OK`: Returns `UserReservationResponseDTO`.
    -   `404 Not Found`: Reservation does not belong to the user or does not exist.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
 
---
 
#### POST /
 
Creates one or more new reservations for the current user.
 
-   **Roles:** `USER`, `MANAGER`, `ADMIN`
-   **Request Body:** `UserReservationsRequestDTO`
-   **Responses:**
    -   `200 OK`: Reservation(s) successfully created. Returns a list of `UserReservationResponseDTO`.
    -   `400 Bad Request`: Invalid request (e.g., more seats than allowed).
    -   `403 Forbidden`: No permission for the event.
    -   `404 Not Found`: Event or seat not found.
    -   `409 Conflict`: Seat already reserved.
    -   `401 Unauthorized`: Not authenticated.
 
---
 
#### DELETE /
 
Deletes one or more reservations of the current user.
 
-   **Roles:** `USER`, `MANAGER`, `ADMIN`
-   **Query Parameter:** `ids` (List<Long>) - The reservation IDs to delete.
-   **Responses:**
    -   `204 No Content`: Reservation(s) successfully deleted.
    -   `404 Not Found`: One or more reservations do not belong to the user or do not exist.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.

---

## Data Transfer Objects (DTOs)

### RegistrationStatusDTO

Represents the current registration status of the application.

**Fields:**
- `enabled` (boolean): Indicates whether user registration is currently enabled (`true`) or disabled (`false`).

**Example:**
```json
{
  "enabled": true
}
```

**Usage:** Returned by the `GET /api/auth/registration-status` endpoint. Clients can use this information to display or hide the registration form in the UI based on the application's current registration settings.

---

## Configuration

### Registration Settings

The registration feature can be controlled via the following configuration property:

**Property:** `registration.enabled`
**Type:** `boolean`
**Default Value:** `true`
**Environment Variable:** `REGISTRATION_ENABLED`

**Description:**
When set to `true`, new users can register via the `POST /api/auth/register` endpoint. When set to `false`, registration is disabled and any attempt to register will result in a `403 Forbidden` response with the message "User registration is currently disabled".

**Example Configuration (application.yaml):**
```yaml
registration:
  enabled: false
```

**Use Cases:**
- Disable registration temporarily for maintenance
- Disable registration when using an external authentication provider
- Restrict new user sign-ups to admin-created accounts only

---

## Error Handling

### RegistrationDisabledException

**HTTP Status Code:** `403 Forbidden`

**Message:** "User registration is currently disabled"

**Description:** This exception is thrown when a client attempts to register a new user via the `POST /api/auth/register` endpoint while registration is disabled in the configuration.

**Response Example:**
```json
{
  "message": "User registration is currently disabled"
}
```

**When Encountered:**
- Check the application's configuration to verify the `registration.enabled` setting
- If registration should be enabled, update the configuration and restart the application
- If registration should remain disabled, contact the application administrator