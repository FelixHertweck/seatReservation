# API Documentation

This is the API documentation for the seat reservation system.

## Table of Contents

- [Authentication](#authentication)
- [User Management (Admin)](#user-management-admin)
- [User Profile (User)](#user-profile-user)
- [Email Confirmation](#email-confirmation)
- [Event Locations (Manager/Admin)](#event-locations-manageradmin)
- [Events (Manager/Admin)](#events-manageradmin)
- [Seats (Manager/Admin)](#seats-manageradmin)
- [Reservations (Manager/Admin)](#reservations-manageradmin)
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
    -   `200 OK`: Successful registration. Sets a `jwt` cookie.
    -   `409 Conflict`: User with this username already exists.

---

#### POST /logout

Logs out the current user by clearing the JWT cookie.

-   **Roles:** Authenticated
-   **Responses:**
    -   `200 OK`: Successful logout. Clears the `jwt` cookie.

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
-   **Request Body:** `AdminUserCreationDto`
-   **Responses:**
    -   `201 Created`: User successfully created. Returns `UserDTO`.
    -   `400 Bad Request`: Invalid data (e.g., duplicate username).
    -   `403 Forbidden`: Access denied (role is not `ADMIN`).
    -   `409 Conflict`: User with this username already exists.
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
 
#### DELETE /{id}
 
Deletes an event location.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The event location ID.
-   **Responses:**
    -   `200 OK`: Location successfully deleted.
    -   `404 Not Found`: Location not found or no permission.
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
    -   `200 OK`: Returns a list of `DetailedEventResponseDTO` objects.

---
 
#### GET /
 
Retrieves all events managed by the current manager.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Returns a list of `DetailedEventResponseDTO` objects.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
 
---
 
#### GET /{id}
 
Retrieves a specific event by its ID.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The event ID.
-   **Responses:**
    -   `200 OK`: Returns `DetailedEventResponseDTO`.
    -   `403 Forbidden`: No permission.
    -   `404 Not Found`: Event not found.
    -   `401 Unauthorized`: Not authenticated.
 
---
 
#### POST /
 
Creates a new event.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Request Body:** `EventRequestDTO`
-   **Responses:**
    -   `200 OK`: Event successfully created. Returns `DetailedEventResponseDTO`.
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
    -   `200 OK`: Event successfully updated. Returns `DetailedEventResponseDTO`.
    -   `404 Not Found`: Event not found or no permission.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
    -   `409 Conflict`: Event with this name already exists in this event location.
 
---
 
#### DELETE /{id}
 
Deletes an event.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The event ID.
-   **Responses:**
    -   `204 No Content`: Event successfully deleted.
    -   `403 Forbidden`: No permission to delete.
    -   `404 Not Found`: Event not found.
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
 
#### DELETE /{id}
 
Deletes a seat.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The seat ID.
-   **Responses:**
    -   `200 OK`: Seat successfully deleted.
    -   `204 No Content`: Seat deleted successfully.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
    -   `404 Not Found`: Seat with specified ID not found for the current manager.
 
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
 
#### DELETE /{id}
 
Deletes a reservation permission.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The permission ID.
-   **Responses:**
    -   `204 No Content`: Permission successfully deleted.
    -   `403 Forbidden`: No permission.
    -   `404 Not Found`: Permission not found.
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
    -   `200 OK`: Returns a list of `DetailedReservationResponseDTO` objects.

---
 
#### GET /
 
Retrieves all reservations for the current manager's events.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Returns a list of `DetailedReservationResponseDTO` objects.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
 
---
 
#### GET /{id}
 
Retrieves a specific reservation by its ID.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The reservation ID.
-   **Responses:**
    -   `200 OK`: Returns `DetailedReservationResponseDTO`.
    -   `404 Not Found`: Reservation not found or no permission.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
 
---
 
#### GET /event/{id}
 
Retrieves all reservations for a specific event.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The event ID.
-   **Responses:**
    -   `200 OK`: Returns a list of `DetailedReservationResponseDTO` objects.
    -   `403 Forbidden`: No permission for this event.
    -   `401 Unauthorized`: Not authenticated.
 
---
 
#### POST /
 
Creates a new reservation.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Request Body:** `ReservationRequestDTO`
-   **Responses:**
    -   `200 OK`: Reservation successfully created. Returns `Set<DetailedReservationResponseDTO>`.
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
    -   `200 OK`: Reservation successfully updated. Returns `DetailedReservationResponseDTO`.
    -   `404 Not Found`: Reservation, event, or user not found or no permission.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
 
---
 
#### DELETE /{id}
 
Deletes a reservation.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The reservation ID.
-   **Responses:**
    -   `204 No Content`: Reservation successfully deleted.
    -   `404 Not Found`: Reservation not found or no permission.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
 
---
 
#### POST /block
 
Blocks any number of seats for an event.
 
-   **Roles:** `MANAGER`, `ADMIN`
-   **Request Body:** `BlockSeatsRequestDTO`
-   **Responses:**
    -   `200 OK`: Seats successfully blocked. Returns `Set<DetailedReservationResponseDTO>`.
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
 
## Events (User)
 
### EventResource
 
Base path: `/api/user/events`
 
---
 
#### GET /
 
Retrieves all events for which the current user has permission.
 
-   **Roles:** `USER`, `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Returns a list of `EventResponseDTO` objects, including the number of allowed reservations.
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
    -   `200 OK`: Returns a list of `ReservationResponseDTO` objects.

---
 
#### GET /
 
Retrieves all reservations of the current user.
 
-   **Roles:** `USER`, `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Returns a list of `ReservationResponseDTO` objects.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
 
---
 
#### GET /{id}
 
Retrieves a specific reservation of the current user.
 
-   **Roles:** `USER`, `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The reservation ID.
-   **Responses:**
    -   `200 OK`: Returns `ReservationResponseDTO`.
    -   `404 Not Found`: Reservation does not belong to the user or does not exist.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.
 
---
 
#### POST /
 
Creates one or more new reservations for the current user.
 
-   **Roles:** `USER`, `MANAGER`, `ADMIN`
-   **Request Body:** `ReservationsRequestDTO`
-   **Responses:**
    -   `200 OK`: Reservation(s) successfully created. Returns a list of `ReservationResponseDTO`.
    -   `400 Bad Request`: Invalid request (e.g., more seats than allowed).
    -   `403 Forbidden`: No permission for the event.
    -   `404 Not Found`: Event or seat not found.
    -   `409 Conflict`: Seat already reserved.
    -   `401 Unauthorized`: Not authenticated.
 
---
 
#### DELETE /{id}
 
Deletes a reservation of the current user.
 
-   **Roles:** `USER`, `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The reservation ID.
-   **Responses:**
    -   `204 No Content`: Reservation successfully deleted.
    -   `404 Not Found`: Reservation does not belong to the user or does not exist.
    -   `401 Unauthorized`: Not authenticated.
    -   `403 Forbidden`: Access denied.