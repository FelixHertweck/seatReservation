# Release 0.0.1

This is the first official release of the Seat Reservation application.

## Highlights

- **Seat Reservation Functionality:** Users can reserve seats for various events.
- **User Management:** Comprehensive features for managing user accounts.
- **Event Management:** Administrators can create and manage events and venues.
- **Security:** Implementation of HTML sanitization and email verification.
- **Internationalization:** Support for multiple languages in the web application.

## Getting Started

To deploy the application with Docker, follow these steps:

1.  **Generate JWT Keys:**
    ```shell script
    mkdir -p keys && openssl genpkey -algorithm RSA -out keys/privateKey.pem -pkeyopt rsa_keygen_bits:2048 && openssl rsa -pubout -in keys/privateKey.pem -out keys/publicKey.pem
    ```
2.  **Set up Environment Variables:**
    Copy the `.env.example` file and rename it to `.env`. Edit the `.env` file with your actual configurations, especially for email services.
    ```shell script
    cp .env.example .env
    ```
3.  **Start Docker Compose:**
    ```shell script
    docker compose up -d
    ```

### Admin User Importer

The `importer/` folder contains a small Java program that reads an `input.csv` and writes admin user data as JSON to `output.json`. This is useful for bulk creation of admin users. For more details, refer to the [importer/README.md](importer/README.md).

For more information, please refer to the [README.md](README.md).