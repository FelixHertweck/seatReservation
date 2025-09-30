# Seat Reservation System

A comprehensive system for managing seat reservations for events. The system consists of a Quarkus backend and a Next.js frontend.

## Overview

There is a [DeepWiki](https://deepwiki.com/FelixHertweck/seatReservation), check it out for documentation.

After initial start there is a user account with username admin and password admin. Make sure to change the default credentials before deploying the application!

This application provides a robust solution for managing event seat reservations. It allows users to browse events, reserve seats, and receive email notifications. The system supports different user roles with varying levels of access and capabilities, ensuring secure and efficient management of events and reservations.

## Architecture

The system is built with a clear separation of concerns:

-   **Backend:** Developed using **Quarkus**, a cloud-native Java framework, providing high performance and a small memory footprint. It handles all business logic, data persistence, and API endpoints.
-   **Frontend:** A modern web application built with **Next.js**, a React framework, offering a responsive and interactive user interface. It consumes data from the backend API.
-   **Database:** **PostgreSQL** is used as the primary data store, ensuring reliable and scalable data management for events, seats, and user information.
-   **Security:** Implemented with **JWT (JSON Web Tokens)** for authentication and authorization, securing access to various functionalities based on user roles.
-   **Email Services:** Integrated for sending automated notifications, such as reservation confirmations and event reminders, configured via environment variables.

## User Roles and Permissions

The system defines different user roles to manage access and functionalities:

### Standard User

-   Can view available events.
-   Can reserve seats for events.
-   Receives email confirmations for reservations.
-   Can view their own past and upcoming reservations.

### Manager

Managers have elevated privileges, allowing them to manage events and reservations within their scope.
-   **Event Management:** Can create, update, and delete events.
-   **Seat Management:** Can define and modify seating plans for events.
-   **Reservation Oversight:** Can view and manage all reservations for events they manage.
-   **User Allowances:** Can set specific allowances for users regarding event access or reservation limits.

### Admin

Admins have full control over the system, including user management and system-wide configurations.
-   **Full Event and Reservation Management:** All capabilities of a Manager, but across all events.
-   **User Management:** Can create, update, and delete user accounts.
-   **Role Assignment:** Can assign and modify user roles (Standard User, Manager, Admin).
-   **System Configuration:** Access to system-wide settings and configurations.

## Build Status
- Frontend 
[![Build Status](https://github.com/FelixHertweck/SeatReservation/actions/workflows/frontend.yml/badge.svg?branch=main)](https://github.com/FelixHertweck/SeatReservation/actions/workflows/frontend.yml)

- Backend:  [![Build Status](https://github.com/FelixHertweck/SeatReservation/actions/workflows/backend.yml/badge.svg?branch=main)](https://github.com/FelixHertweck/SeatReservation/actions/workflows/backend.yml)
- Backend Native: [![Build Status](https://github.com/FelixHertweck/SeatReservation/actions/workflows/backend-native.yml/badge.svg?branch=main)](https://github.com/FelixHertweck/SeatReservation/actions/workflows/backend-native.yml)

## Initial Setup

Before running the application, ensure the following setup steps are completed:

### JWT Key Generation

Execute the following command to generate the necessary keys for JWT authentication:

```shell script
mkdir -p keys && openssl genpkey -algorithm RSA -out keys/privateKey.pem -pkeyopt rsa_keygen_bits:2048 && openssl rsa -pubout -in keys/privateKey.pem -out keys/publicKey.pem
```

### Automatic Admin User Creation

Upon initial startup, the application automatically checks for the existence of an 'admin' user. If no user with the username 'admin' is found, a new admin account will be created with the following default credentials:

-   **Username:** `admin`
-   **Email:** `admin@example.com`
-   **Password:** `admin` 
-   **Roles:** `ADMIN`

**Make sure to change the default credentials before deploying the application**

## Environment Variables

The application requires certain environment variables to be set for proper functionality, especially for email services. These variables are loaded from a `.env` file in the project root.

### Setting up your environment

1.  Copy the `.env.example` file to create your own `.env` file:
    ```shell script
    cp .env.example .env
    ```

2.  Edit the `.env` file and replace the placeholder values with your actual configuration:
    ```
    mail-host=your-mail-host         # e.g., smtp.gmail.com
    mail-port=your-mail-port         # e.g., 465 for SSL
    mail-username=your-mail-username # Your email username/address
    mail-password=your-mail-password # Your email password or app password
    mail-from=your-mail-from-address # The "from" address for sent emails
    ```

## User Import

### CSV → AdminUserCreationDto Importer

This small Java program reads an `input.csv` and writes the data as JSON to `output.json`.  
Each line of the CSV is translated into an `AdminUserCreationDto` object.

#### Running the Importer

To run the importer, execute the following command from the project root:

```shell script
mvn exec:java@import
```

The `input.csv` file must be placed in the project root directory. The generated `output.json` can then be imported via the admin interface.

#### Structure of `input.csv`

The file **must** be in the project root directory.  
Columns are separated by `,` (comma).

| First Name | Last Name | Password |
|------------|-----------|----------|
| Max        | Mustermann | secret123 |
| Anna       | Müller     | password |

**Example `input.csv`:**
```
Max,Mustermann,secret123
Anna,Müller,password
```

## Backend (Quarkus)

### Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_** Quarkus ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

### Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory. Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

### Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/security-jpa-quickstart-1.0.0-SNAPSHOT-runner`

To learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Frontend (Next.js)

The frontend is located in the `webapp/` directory.

### Installing Dependencies

Navigate into the `webapp/` directory and install the dependencies:

```shell script
cd webapp/
npm install
```

### Available Scripts

From the `webapp/` directory, you can run the following scripts:

-   **`npm run dev`**: Starts the application in development mode.
-   **`npm run build`**: Builds the application for production.
-   **`npm run start`**: Starts the production server.
-   **`npm run lint`**: Runs ESLint and automatically fixes issues.
-   **`npm run lint:check`**: Runs ESLint without fixing issues.
-   **`npm run format`**: Formats the code with Prettier and writes changes.
-   **`npm run format:check`**: Checks code formatting with Prettier.
-   **`npm run generate:openapi-file`**: Generates the OpenAPI file from the backend.
-   **`npm run generate:api-client`**: Generates the API client based on the OpenAPI file.
-   **`npm run generate:api`**: Executes `generate:openapi-file` and `generate:api-client`.
