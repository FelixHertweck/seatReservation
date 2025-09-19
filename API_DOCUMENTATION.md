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

## User Management (Admin)

### UserResource

Base path: `/api/users`

---

#### POST /admin

Creates a new user.

-   **Roles:** `ADMIN`
-   **Request Body:** `UserCreationDTO`
-   **Responses:**
    -   `200 OK`: User successfully created. Returns `UserDTO`.
    -   `400 Bad Request`: Invalid data (e.g., duplicate username).
    -   `403 Forbidden`: Access denied (role is not `ADMIN`).

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

---

#### DELETE /admin/{id}

Deletes a user.

-   **Roles:** `ADMIN`
-   **Path Parameter:** `id` (Long) - The user ID.
-   **Responses:**
    -   `200 OK`: User successfully deleted.
    -   `404 Not Found`: User not found.
    -   `403 Forbidden`: Access denied.

---

#### GET /manager

Retrieves a list of all users with limited information.

-   **Roles:** `ADMIN`, `MANAGER`
-   **Responses:**
    -   `200 OK`: Returns a list of `LimitedUserInfoDTO` objects.
    -   `403 Forbidden`: Access denied.

---

#### GET /admin/roles

Retrieves all available user roles.

-   **Roles:** `ADMIN`
-   **Responses:**
    -   `200 OK`: Returns a list of available roles (Strings).
    -   `403 Forbidden`: Access denied.

---

#### GET /admin

Retrieves complete data of all users.

-   **Roles:** `ADMIN`
-   **Responses:**
    -   `200 OK`: Returns a list of `UserDTO` objects.
    -   `403 Forbidden`: Access denied.

---

#### POST /admin/import

Imports a set of users.

-   **Roles:** `ADMIN`
-   **Request Body:** `Set<AdminUserUpdateDTO>`
-   **Responses:**
    -   `200 OK`: Users successfully imported. Returns a list of `UserDTO` objects.
    -   `400 Bad Request`: Invalid format.
    -   `403 Forbidden`: Access denied (role is not `ADMIN`).
    -   `500 Internal Server Error`: Internal server error during import.

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

---

#### GET /me

Retrieves data of the currently logged-in user.

-   **Roles:** `USER`, `ADMIN`, `MANAGER`
-   **Responses:**
    -   `200 OK`: Returns the `UserDTO` object of the current user.

---

## Email Confirmation

### EmailConfirmationResource

Base path: `/api/user`

---

#### GET /confirm-email

Confirms a user's email address.

-   **Roles:** Public
-   **Query Parameters:**
    -   `id` (Long): The confirmation ID.
    -   `token` (String): The confirmation token.
-   **Responses:**
    -   `200 OK`: Email successfully confirmed (returns HTML page).
    -   `400 Bad Request`: Invalid token (returns HTML error page).
    -   `404 Not Found`: Token not found (returns HTML error page).
    -   `410 Gone`: Token expired.

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

#### POST /

Creates a new event location.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Request Body:** `EventLocationRequestDTO`
-   **Responses:**
    -   `200 OK`: Location successfully created. Returns `EventLocationResponseDTO`.
    -   `400 Bad Request`: Invalid input data.

---

#### PUT /{id}

Updates an existing event location.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The event location ID.
-   **Request Body:** `EventLocationRequestDTO`
-   **Responses:**
    -   `200 OK`: Location successfully updated. Returns `EventLocationResponseDTO`.
    -   `404 Not Found`: Location not found or no permission.

---

#### DELETE /{id}

Deletes an event location.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The event location ID.
-   **Responses:**
    -   `200 OK`: Location successfully deleted.
    -   `404 Not Found`: Location not found or no permission.

---

#### POST /import

Creates a new event location along with a list of seats.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Request Body:** `ImportEventLocationDto`
-   **Responses:**
    -   `200 OK`: Location and seats successfully created. Returns `EventLocationResponseDTO`.
    -   `400 Bad Request`: Invalid input data.
---

#### POST /importSeats/{id}

Imports a list of seats to an existing event location.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The event location ID.
-   **Request Body:** `Set<ImportSeatDto>`
-   **Responses:**
    -   `200 OK`: Seats successfully imported. Returns `EventLocationResponseDTO`.
    -   `400 Bad Request`: Invalid input data.
    -   `403 Forbidden`: No permission.
    -   `404 Not Found`: Location not found.

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

#### GET /{id}

Retrieves a specific event by its ID.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The event ID.
-   **Responses:**
    -   `200 OK`: Returns `DetailedEventResponseDTO`.
    -   `403 Forbidden`: No permission.
    -   `404 Not Found`: Event not found.

---

#### POST /

Creates a new event.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Request Body:** `EventRequestDTO`
-   **Responses:**
    -   `200 OK`: Event successfully created. Returns `DetailedEventResponseDTO`.
    -   `400 Bad Request`: Invalid data.
    -   `404 Not Found`: Associated location not found.

---

#### PUT /{id}

Updates an existing event.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The event ID.
-   **Request Body:** `EventRequestDTO`
-   **Responses:**
    -   `200 OK`: Event successfully updated. Returns `DetailedEventResponseDTO`.
    -   `404 Not Found`: Event not found or no permission.

---

#### DELETE /{id}

Deletes an event.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The event ID.
-   **Responses:**
    -   `204 No Content`: Event successfully deleted.
    -   `403 Forbidden`: No permission to delete.
    -   `404 Not Found`: Event not found.

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

#### POST /

Creates a new seat.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Request Body:** `SeatRequestDTO`
-   **Responses:**
    -   `200 OK`: Seat successfully created. Returns `SeatDTO`.
    -   `404 Not Found`: Associated location not found or no permission.

---

#### GET /{id}

Retrieves a specific seat by its ID.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The seat ID.
-   **Responses:**
    -   `200 OK`: Returns `SeatDTO`.
    -   `404 Not Found`: Seat not found or no permission.

---

#### PUT /{id}

Updates a seat.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The seat ID.
-   **Request Body:** `SeatRequestDTO`
-   **Responses:**
    -   `200 OK`: Seat successfully updated. Returns `SeatDTO`.
    -   `404 Not Found`: Seat or location not found or no permission.
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

#### GET /{id}

Retrieves a specific reservation permission by its ID.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The permission ID.
-   **Responses:**
    -   `200 OK`: Returns `EventUserAllowancesDto`.
    -   `403 Forbidden`: No permission.
    -   `404 Not Found`: Permission not found.

---

#### GET /

Retrieves all reservation permissions.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Responses:**
    -   `200 OK`: Returns a list of `EventUserAllowancesDto`.
    -   `403 Forbidden`: No permission.

---

#### GET /event/{eventId}

Retrieves all reservation permissions for a specific event.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `eventId` (Long) - The event ID.
-   **Responses:**
    -   `200 OK`: Returns a list of `EventUserAllowancesDto`.
    -   `403 Forbidden`: No permission.
    -   `404 Not Found`: Event not found.

---

#### DELETE /{id}

Deletes a reservation permission.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The permission ID.
-   **Responses:**
    -   `204 No Content`: Permission successfully deleted.
    -   `403 Forbidden`: No permission.
    -   `404 Not Found`: Permission not found.

#### PUT /

Updates an existing reservation permission.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Request Body:** `EventUserAllowanceUpdateDto`
-   **Responses:**
    -   `200 OK`: Permission successfully updated. Returns `EventUserAllowancesDto`.
    -   `403 Forbidden`: No permission.
    -   `404 Not Found`: Permission not found.

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

#### GET /{id}

Retrieves a specific reservation by its ID.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The reservation ID.
-   **Responses:**
    -   `200 OK`: Returns `DetailedReservationResponseDTO`.
    -   `404 Not Found`: Reservation not found or no permission.

---

#### GET /event/{id}

Retrieves all reservations for a specific event.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The event ID.
-   **Responses:**
    -   `200 OK`: Returns a list of `DetailedReservationResponseDTO` objects.
    -   `403 Forbidden`: No permission for this event.

---

#### POST /

Creates a new reservation.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Request Body:** `ReservationRequestDTO`
-   **Responses:**
    -   `200 OK`: Reservation successfully created. Returns `DetailedReservationResponseDTO`.
    -   `404 Not Found`: User, event, or seat not found.
    -   `409 Conflict`: Seat already reserved.

---

#### PUT /{id}

Updates an existing reservation.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The reservation ID.
-   **Request Body:** `ReservationRequestDTO`
-   **Responses:**
    -   `200 OK`: Reservation successfully updated. Returns `DetailedReservationResponseDTO`.
    -   `404 Not Found`: Reservation, event, or user not found or no permission.

---

#### DELETE /{id}

Deletes a reservation.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Path Parameter:** `id` (Long) - The reservation ID.
-   **Responses:**
    -   `200 OK`: Reservation successfully deleted.
    -   `404 Not Found`: Reservation not found or no permission.

---

#### POST /block

Blocks any number of seats for an event.

-   **Roles:** `MANAGER`, `ADMIN`
-   **Request Body:** `BlockSeatsRequestDTO`
-   **Responses:**
    -   `204 No Content`: Seats successfully blocked.
    -   `403 Forbidden`: No permission.
    -   `404 Not Found`: Event or seat not found.
    -   `409 Conflict`: Seat already reserved or blocked.

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

-   **Roles:** `USER`
-   **Responses:**
    -   `200 OK`: Returns a list of `EventResponseDTO` objects, including the number of allowed reservations.

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

#### GET /{id}

Retrieves a specific reservation of the current user.

-   **Roles:** `USER`
-   **Path Parameter:** `id` (Long) - The reservation ID.
-   **Responses:**
    -   `200 OK`: Returns `ReservationResponseDTO`.
    -   `404 Not Found`: Reservation does not belong to the user or does not exist.

---

#### POST /

Creates one or more new reservations for the current user.

-   **Roles:** `USER`
-   **Request Body:** `ReservationsRequestCreateDTO`
-   **Responses:**
    -   `200 OK`: Reservation(s) successfully created. Returns a list of `ReservationResponseDTO`.
    -   `400 Bad Request`: Invalid request (e.g., more seats than allowed).
    -   `403 Forbidden`: No permission for the event.
    -   `404 Not Found`: Event or seat not found.
    -   `409 Conflict`: Seat already reserved.

---

#### DELETE /{id}

Deletes a reservation of the current user.

-   **Roles:** `USER`
-   **Path Parameter:** `id` (Long) - The reservation ID.
-   **Responses:**
    -   `204 No Content`: Reservation successfully deleted.
    -   `404 Not Found`: Reservation does not belong to the user or does not exist.