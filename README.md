# Seat Reservation System

A comprehensive system for managing seat reservations for events. The system consists of a Quarkus backend and a Next.js frontend, and ships with a ready-to-run Docker Compose stack including reverse proxy and monitoring.

## Overview

There is a [DeepWiki](https://deepwiki.com/FelixHertweck/seatReservation), check it out for documentation.

This application provides a robust solution for managing event seat reservations. It allows users to browse events, reserve seats, and receive email notifications. The system supports different user roles with varying levels of access and capabilities, ensuring secure and efficient management of events and reservations.

## Features

-   **Event & seat management** with interactive seating plans.
-   **Reservations** with per-user allowances and manager oversight.
-   **Role-based access** (Standard User, Manager, Admin) secured with JWT.
-   **Transactional email delivery** via a persistent outbox with automatic retries and dead-lettering (see [Email Delivery](#email-delivery-transactional-outbox)).
-   **Customizable email templates** (Qute HTML) and **PDF export** of reservations.
-   **Bulk user import** from CSV.
-   **Monitoring** out of the box with Prometheus and Grafana.
-   **Cloud-native backend** with JVM and native (GraalVM) build options.

## Architecture

The system is built with a clear separation of concerns:

-   **Reverse Proxy:** **nginx** sits in front of the stack, routing traffic to the frontend, the backend API (`/api`, `/q`), and Grafana (`/grafana`).
-   **Backend:** Developed using **Quarkus**, a cloud-native Java framework, providing high performance and a small memory footprint. It handles all business logic, data persistence, and API endpoints.
-   **Frontend:** A modern web application built with **Next.js**, a React framework, offering a responsive and interactive user interface. It consumes data from the backend API.
-   **Database:** **PostgreSQL** is used as the primary data store, ensuring reliable and scalable data management for events, seats, and user information.
-   **Security:** Implemented with **JWT (JSON Web Tokens)** for authentication and authorization, securing access to various functionalities based on user roles.
-   **Email Services:** Integrated for sending automated notifications, such as reservation confirmations and event reminders, delivered asynchronously through a transactional outbox and configured via environment variables.
-   **Monitoring:** **Prometheus** scrapes the backend's metrics and **Grafana** visualizes them via a provisioned dashboard.

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

## Deployment with Docker Compose

The fastest way to run the full system (reverse proxy, frontend, backend, database, and monitoring) is the provided [`docker-compose.yml`](docker-compose.yml). It uses the pre-built images published to GitHub Container Registry, so no local build is required.

### Prerequisites

-   [Docker](https://docs.docker.com/get-docker/) and Docker Compose.
-   Generated JWT keys (see [JWT Key Generation](#jwt-key-generation)).
-   A configured `.env` file (see [Environment Variables](#environment-variables)).

### Starting the stack

```shell script
# 1. Generate JWT keys (once)
mkdir -p keys && openssl genpkey -algorithm RSA -out keys/privateKey.pem -pkeyopt rsa_keygen_bits:2048 && openssl rsa -pubout -in keys/privateKey.pem -out keys/publicKey.pem

# 2. Create and edit your environment file
cp .env.example .env

# 3. Start everything in the background
docker compose up -d
```

> **_NOTE:_** The backend container runs as `${UID}:${GID}` so it can read the mounted `keys/` directory. Export these before starting if they are not already set: `export UID=$(id -u) GID=$(id -g)`.

### Services and access

Only **nginx** is published to the host (port `80`); all other services communicate over the internal `app-network`.

| Service | Purpose | Access |
|---------|---------|--------|
| `nginx` | Reverse proxy / entry point | `http://localhost` |
| `frontend` | Next.js web UI | via nginx `/` |
| `backend` | Quarkus API | via nginx `/api`, `/q` |
| `db` | PostgreSQL 18 | internal only |
| `prometheus` | Metrics collection | internal only |
| `grafana` | Dashboards | `http://localhost/grafana` (default login `admin` / `admin`) |

To expose individual services directly (e.g. the backend on `8080` or the database on `5432`) for debugging, uncomment the corresponding `ports:` blocks in [`docker-compose.yml`](docker-compose.yml).

### Using the native backend image

For a smaller footprint and faster startup, switch the backend to the native image by uncommenting the native image line in the `backend` service:

```yaml
image: ghcr.io/felixhertweck/seatreservation-backend-native:latest
```

### Building the images locally

The Dockerfiles live in [`src/main/docker/`](src/main/docker/) (`Dockerfile.jvm`, `Dockerfile.native`) for the backend and [`webapp/Dockerfile`](webapp/Dockerfile) for the frontend, in case you want to build the images yourself instead of pulling from GHCR.

### Updating & Database Backups

The backend applies database schema migrations automatically on startup (via Flyway). These migrations are not automatically reversible, so back up the database before pulling a new backend image:

```shell script
docker compose exec -T db pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" > backup-$(date +%Y%m%d-%H%M%S).sql
```

To restore a backup if something goes wrong:

```shell script
docker compose exec -T db psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" < backup-YYYYMMDD-HHMMSS.sql
```

## Initial Setup

Before running the application (locally or via Docker), ensure the following setup steps are completed:

### JWT Key Generation

Execute the following command to generate the necessary keys for JWT authentication:

```shell script
mkdir -p keys && openssl genpkey -algorithm RSA -out keys/privateKey.pem -pkeyopt rsa_keygen_bits:2048 && openssl rsa -pubout -in keys/privateKey.pem -out keys/publicKey.pem
```

### Automatic Admin User Creation

Upon initial startup, the application automatically checks for the existence of an 'admin' user. If no user with the username 'admin' is found, a new admin account will be created during the application's startup phase. The credentials for this automatically created user are:

-   **Username:** `admin`
-   **Email:** `admin@localhost`
-   **Password:** A randomly generated password. This password will be logged to the console (STDOUT) during the application's startup.
-   **Roles:** `ADMIN`

**Make sure to change the default credentials before deploying the application**

## Environment Variables

The application is configured via environment variables. For local runs and Docker Compose, these are loaded from a `.env` file in the project root. Copy the template to get started:

```shell script
cp .env.example .env
```

Then edit the values:

| Variable | Description | Example |
|----------|-------------|---------|
| `MAIL_HOST` | SMTP server host | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP server port | `465` (SSL) |
| `MAIL_USERNAME` | SMTP username / address | `you@example.com` |
| `MAIL_PASSWORD` | SMTP password or app password | `••••••••` |
| `MAIL_FROM` | The "from" address for sent emails | `noreply@example.com` |
| `MAIL_TLS` | Set to `true` if the SMTP server requires TLS | `false` |
| `MAIL_START_TLS` | STARTTLS mode (`REQUIRED`, `OPTIONAL`, `DISABLED`) | `REQUIRED` |
| `EMAIL_BCC_ADDRESS` | Optional BCC address for all outgoing emails (leave empty to disable) | |
| `APP_URL` | Public base URL of the deployment (used for links in emails and the Grafana root URL) | `http://localhost:8080` |
| `POSTGRES_USER` | Database user (Docker deployment) | `postgres` |
| `POSTGRES_PASSWORD` | Database password (Docker deployment) | `postgres_password` |
| `POSTGRES_DB` | Database name (Docker deployment) | `seatReservation` |

## Email Delivery (Transactional Outbox)

Outgoing emails are not sent inline. Instead they are written to a persistent **outbox** and delivered asynchronously by a background dispatcher. This makes email delivery reliable and resilient to temporary mail-server outages:

-   When a business action triggers an email, the message is persisted as an `OutboundEmail` in the **same database transaction** as the change that caused it. If that transaction rolls back, no orphan email is queued; once it commits, delivery is guaranteed to be attempted.
-   A scheduled dispatcher drains the queue, hands messages to the SMTP server, and records the outcome. Message lifecycle: `PENDING → SENDING → SENT` (or `FAILED`).
-   Failed sends are retried with **exponential back-off**. After the configured number of attempts a message becomes a `FAILED` dead letter.
-   Delivered and failed messages are cleaned up automatically after a retention period.

The behaviour is configurable via `email.queue.*` in [`application.yaml`](src/main/resources/application.yaml):

| Setting | Default | Description |
|---------|---------|-------------|
| `poll-interval` | `30s` | How often the dispatcher drains the queue |
| `batch-size` | `20` | Max number of mails handled per drain cycle |
| `max-attempts` | `5` | Delivery attempts before a mail becomes a `FAILED` dead letter |
| `retry-backoff-seconds` | `60` | Base back-off; doubles per attempt (60s, 120s, 240s, …) |
| `max-backoff-seconds` | `3600` | Upper bound for the back-off delay |
| `sending-timeout-seconds` | `300` | After this, a mail stuck in `SENDING` is requeued |
| `retention-days` | `30` | How long delivered/failed mails are kept before cleanup |

## Monitoring

The Docker Compose stack includes a monitoring setup:

-   The backend exposes Prometheus metrics at `/q/metrics/prometheus`.
-   **Prometheus** ([`prometheus/prometheus.yml`](prometheus/prometheus.yml)) scrapes the backend and stores the metrics.
-   **Grafana** ([`grafana/`](grafana/)) is provisioned with the Prometheus data source and a Quarkus dashboard, reachable at `http://localhost/grafana` (default login `admin` / `admin` — change this before deploying).

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

## Templates

### Email Templates

The application renders emails from customizable [Qute](https://quarkus.io/guides/qute) HTML templates located in [`src/main/resources/templates/email/`](src/main/resources/templates/email/):

-   `email-confirmation.html`
-   `password-changed.html`
-   `reservation-confirmation.html`
-   `reservation-update-confirmation.html`
-   `event-reminder.html`
-   `manager-reservation-export.html`

Edit these files directly to adjust the look and content of outgoing emails, keeping the existing `{placeholder}` expressions intact. Subject lines and small text snippets are configured under `email.header.*` and related keys in [`application.yaml`](src/main/resources/application.yaml).

#### Overriding Templates in a Deployment

Instead of editing the bundled files (which requires rebuilding the image), you can supply your own templates from an external directory via `email.template.override-dir` (env var `EMAIL_TEMPLATE_OVERRIDE_DIR`). This is picked up on application start and takes precedence over the bundled templates.

To customize a template:

1.   Create a directory, e.g. `./email-templates/email/`.
2.   Add an `.html` file named after the template you want to replace, e.g. `password-changed.html`. It only needs to contain the templates you actually want to change — anything missing falls back to the bundled version.
3.   Write it as a normal, self-contained HTML file (Qute `{placeholder}`, `{#if}`, `{#for}` syntax is supported), using the same `{placeholder}` names as the original.
4.   Point the app at the directory:
     -   Locally: set `EMAIL_TEMPLATE_OVERRIDE_DIR=/path/to/email-templates` in `.env`.
     -   Docker Compose: mount the directory into the container and set the env var — see the commented `email-templates` volume and `EMAIL_TEMPLATE_OVERRIDE_DIR` entry in [`docker-compose.yml`](docker-compose.yml).
5.   Restart the application to pick up the changes.

### PDF Export Templates

The application supports exporting reservations as PDF documents, utilizing predefined PDF templates with AcroForm fields. There are two distinct templates used based on the reservation status:

-   **`/export-template/reserved.pdf`**: Used for reservations with the status `RESERVED`.
    -   **Required Form Fields:**
        -   `reservedUntil`: The date until which the seat is reserved.
        -   `userName`: The full name of the user who made the reservation.
        -   `seatInfo`: Information about the seat (e.g., "A1 (Row 1)").

-   **`/export-template/blocked.pdf`**: Used for reservations with the status `BLOCKED`.
    -   **Required Form Fields:**
        -   `seatInfo`: Information about the seat (e.g., "A1 (Row 1)").

If these template files are not found at the specified paths, the system will generate a standard PDF layout with basic reservation information.

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

### Backend code quality checks
```shell script
# Spotless checks and formatting for the backend
./mvnw spotless:check
./mvnw spotless:apply

# License checks and generation for the backend
./mvnw license:check-file-header
./mvnw license:update-file-header
```

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


## License
This project is licensed under the Apache License, Version 2.0 - see the [LICENSE](LICENSE) file for details.

### Check licenses of dependencies:


```shell script
# Backend
mvn license:add-third-party

# Frontend
cd webapp/
npx license-checker --json --production
npx license-checker --onlyAllow "MIT;Apache-2.0;BSD-3-Clause"
```
