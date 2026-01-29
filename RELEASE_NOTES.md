# Release 0.0.8

This release introduces comprehensive supervisor and check-in functionality with QR codes, Progressive Web App (PWA) support, enhanced notification system with improved toasts, user management improvements, and various refactorings and security enhancements.

## New Features
- **Supervisor & Check-In**: Added complete supervisor functionality including check-in codes generation, QR code support, and live reservation status tracking.
- **Live View**: Implemented live view functionality to display real-time reservation status with WebSocket support.
- **PWA Support**: Added manifest file for Progressive Web App support, enabling offline functionality and app-like experience.
- **User Management**: Enhanced user deletion with bulk delete support via IDs query parameter; added user list exposure to manager UI.
- **Allowance Management**: New dedicated allowance management interface for supervisors and managers.
- **Registration Control**: Added ability to enable/disable user registration at the application level.
- **Refresh Token**: Implemented refresh token functionality allowing users to logout from all devices simultaneously.
- **QR Code in Emails**: Integrated QR code generation into email service for reservation check-ins.
- **Email Enhancements**: Replaced SVG with PNG in email seatmaps for better compatibility and added interactive webview support.
- **Seat Entrance Field**: Added entrance field to seat management for better seat identification and organization.
- **Sortable Tables**: Implemented sortable tables with truncation feature in management UI.
- **Enter Key Handling**: Added Enter key handling for form submission in modals.
- **Username Validation**: Added username validation hint with tooltip and info icon.

## Fixes
- **DevContainer Setup**: Fixed devcontainer configuration with proper application-devcontainer.yaml and corrected JDBC URL format.
- **Security**: Updated authentication to use Quarkus SecurityIdentity and restricted EventResource & EventLocationResource to USER role.
- **Reservation Code**: Corrected update endpoint authorization to allow only USER role.
- **Email Service**: Transitioned to ReactiveMailer with improved error handling and removed unnecessary transactional annotations from scheduled methods.
- **Token Management**: Delete refresh token from database on logout to prevent unauthorized access.
- **CSV Export**: Updated line terminator to CRLF for RFC compliance.
- **JWT Exceptions**: Enhanced GlobalExceptionHandler to manage JWT invalid exceptions by removing cookies.
- **Allowance Count**: Fixed restoration of allowance count on manager reservation deletion.
- **Exception Handling**: Replaced generic IllegalArgumentException with VerificationCodeNotFoundException and moved ReservationNotFoundException to common.exception package.
- **Email Notifications**: Removed BCC from event reminder emails.
- **Manifest Route**: Ensured manifest route is treated as static for static build.
- **Email Address Skip**: Moved null/empty email address skip log from WARN to DEBUG.

## Refactored
- **Sidebar**: Renamed and refactored to show icons when closed; moved custom-ui components out of ui folder to avoid shadcn conflicts.
- **Component Library**: Updated components to latest shadcn version with improved accessibility and layout adjustments.
- **Type Safety**: Replaced 'any' type with 'unknown' and specific types throughout codebase.
- **Package Structure**: Renamed 'ressource' to 'resource' to fix typo in management package.
- **Token Refresh**: Improved toast scheduling and trigger login handling in InitQueryClient with better error logging (console.warn instead of console.error).
- **Authentication**: Centralized redirect logic with redirectUser utility and updated auth hooks/pages accordingly.
- **Tests**: Updated test files to align with check-in and reservation changes; replaced Long with primitive long for event IDs.
- **User Update Process**: Refactored user and admin update process for better consistency.
- **Import Process**: Improved CSV format configuration to avoid deprecated methods.
- **Toast Notifications**: Replaced custom toast implementation with shadcn Sonner toast and improved logging with toast.promise.
- **OpenAPI Client**: Updated generated OpenAPI API client/types for check-in & supervisor endpoints.

## Style
- **UI/UX**: Updated profile & reservations layouts; enhanced responsive widths in management UI.
- **Components**: Minor accessibility and layout adjustments across webapp components.
- **Management UI**: Adapted management UI for new fields and user list support.

## Build
- **Dependencies**: Bumped quarkus.platform.version from 3.28.2 to 3.31.1.
- **Dependencies**: Bumped postgres from 17-alpine to 18-alpine.
- **Dependencies**: Bumped prom/prometheus from v3.6.0 to v3.9.1.
- **Dependencies**: Bumped grafana/grafana from 12.0 to 12.3.
- **Dependencies**: Bumped @types/node from 24.10.0 to 25.1.0 in /webapp.
- **Dependencies**: Bumped @hey-api/openapi-ts from 0.86.1 to 0.90.10 in /webapp.
- **Dependencies**: Bumped i18next from 25.5.2 to 25.8.0 in /webapp.
- **Dependencies**: Bumped lucide-react from 0.546.0 to 0.563.0 in /webapp.
- **Dependencies**: Bumped next from 15.5.0 to 16.1.1 in /webapp.
- **Dependencies**: Bumped node from 24-alpine to 25-alpine in /webapp.
- **Dependencies**: Bumped react-hook-form from 7.64.0 to 7.66.0 in /webapp.
- **Dependencies**: Bumped react-i18next from 16.0.0 to 16.5.0 in /webapp.
- **Dependencies**: Bumped prettier from 3.6.2 to 3.8.1 in /webapp.
- **Dependencies**: Bumped eslint from 9.37.0 to 9.39.0 in /webapp.
- **Dependencies**: Bumped com.googlecode.owasp-java-html-sanitizer for improved security.
- **Dependencies**: Bumped ubi9/openjdk-21 from 1.23 to 1.24 in /src/main/docker.
- **Dependencies**: Bumped io.quarkiverse.openpdf:quarkus-openpdf.
- **Dependencies**: Bumped org.apache.xmlgraphics batik libraries (codec and transcoder).
- **Dependencies**: Bumped com.google.zxing.version from 3.5.3 to 3.5.4 for QR code support.
- **Dependencies**: Bumped tailwind-merge from 3.3.1 to 3.4.0 in /webapp.
- **Dependencies**: Bumped org.codehaus.mojo:exec-maven-plugin and license-maven-plugin.
- **Dependencies**: Bumped com.diffplug.spotless:spotless-maven-plugin.
- **Dependencies**: Bumped org.jacoco:jacoco-maven-plugin from 0.8.13 to 0.8.14.
- **Dependencies**: Bumped actions dependencies (checkout, upload-artifact, download-artifact, setup-node, sigstore/cosign-installer).
- **Dependencies**: Added ZXing, websockets and jsr310 dependencies for check-in and QR code support.
- **Dependencies**: Added sanitizeFileName utility for safe file downloads.
- **Dependabot Configuration**: Added docker-compose support to dependabot configuration.
- **Workflow Permissions**: Updated permissions in workflow files for cleanup and build processes.

## Additional Changes
- **Email Reminders**: Implemented configurable reminder email send date per event with programmatic scheduling.
- **Documentation**: Updated OpenAPI specification and generated TypeScript types for new check-in and supervisor endpoints.
- **Internationalization**: Added check-in & liveview translations and misc translation improvements.
- **API Documentation**: Added @ApiResponse annotations and missing javadoc.
- **Configuration**: Updated application.yaml and import data; improved configuration setup for different environments including devcontainer support.
- **Bug Fixes**: Enhanced error handling and response body validation in email tests.
- **Scheduled Tasks**: Implemented scheduled cleanup for expired email verifications and refresh tokens.

---

# Release 0.0.7

This release focuses on improving the user experience by enhancing deletion functionalities, fixing several bugs related to event and reservation management, and refining the overall UI responsiveness.

## New Features
- **Reservation & Manager**: Enhanced deletions of reservation and manager objects with multi-select support, making it easier to manage multiple entries at once.

## Fixes
- **Reservation**: Ensured that reservation update notifications are only sent when the status is "reserved", reducing unnecessary notifications.
- **Event Management**: The booking start time is now correctly updated when an event is modified.
- **Export**: The CSV export for reservations now correctly includes the seat row and reservation status in the header.
- **Authentication**: Updated the authentication system to handle cases where user email addresses may not be unique.
- **Reservations**: Corrected an issue with handling selected reservation locations.
- **Email**: Prevented duplicate email addresses in email recipients and set the default BCC address to null to avoid unintended recipients.

## Refactored
- **Profile**: The email verification process on the user profile page has been improved with clearer buttons and better styling on mobile devices.
- **Manager**: Components in the manager section have been updated for better responsiveness and a more consistent user interface.
- **Frontend**: Various styling improvements have been made across the frontend for a better visual experience.

## Style
- **Admin**: The admin components are now more responsive and have improved styling.
- **SVG**: The styling for SVGs has been improved for better responsiveness and rendering.

## Build
- **Dependencies**: Bumped `@hey-api/openapi-ts` from 0.84.1 to 0.85.0 in /webapp.
- **Dependencies**: Bumped `org.codehaus.mojo:exec-maven-plugin`.
- **Dependencies**: Bumped `@types/node` from 24.6.1 to 24.7.0 in /webapp.
- **Dependencies**: Bumped `react-dom` and `@types/react-dom` in /webapp.
- **Dependencies**: Bumped `react-hook-form` from 7.63.0 to 7.64.0 in /webapp.
- **Dependencies**: Bumped `eslint` from 9.36.0 to 9.37.0 in /webapp.

---
# Release 0.0.6

This release introduces several new features, fixes, and improvements across the application, focusing on enhanced event and user management, improved export functionalities, and better UI/UX.

## New Features
- **Event Management**: Added booking start time to event management and reservation system, allowing for more precise event scheduling.
- **UI/UX**: Implemented a feature to prevent modals from closing when interacting outside, improving user experience during data entry.
- **User Management**: Added the ability to add users with an email address and send verification at user login time, streamlining the onboarding process.
- **Email System**: Enhanced reservation confirmation emails to support additional email addresses and included the option to send confirmation mails to manager users.
- **Export Functionality**: Updated the CSV export format to include seat row and status, providing more comprehensive data for analysis.
- **Export Functionality**: Added seat data to the location export functionality, offering a complete overview of event location layouts.
- **UI/UX**: Enhanced the marker component with dynamic sizing and centering, improving visual clarity on seat maps.
- **Email System**: Implemented BCC support and improved email content formatting in the EmailService for better communication.
- **Export Functionality**: Introduced PDF export functionality for reservations, providing a professional and printable format for records.

## Fixes
- **Dependencies**: Replaced the openpdf dependency with quarkus-openpdf and updated the dockerfile to resolve native image file errors, ensuring smoother deployments.
- **Event Management**: Validated booking start time before the booking deadline, preventing invalid event configurations.
- **UI/UX**: Updated the event card status badge for accurate and timely display of event statuses.
- **User Management**: Ensured users are persisted before sending confirmation emails to guarantee a valid user ID is available.
- **Email System**: The mailer now uses an email address list instead of a single user email, enhancing flexibility in recipient management.
- **Email System**: Prevented duplicate Bcc addresses in email recipients, ensuring clean and efficient email delivery.
- **Export Functionality**: Reset the selected format in the export modal to default, improving consistency.
- **Configuration**: Corrected the export template path in the docker-compose configuration, resolving export issues.
- **Logging**: Changed the log level from info to debug for reservation export methods, reducing log verbosity in production.
- **Internationalization**: Translated into English.
- **UI/UX**: Adjusted SVG styling for full width and height rendering.
- **UI/UX**: Prevented blocked seats from being returned as user reservations and updated the height of reservation modals.
- **UI/UX**: Integrated reservations into the events page to show existing reservations.

## Changes
- **Documentation**: Updated README and SQL import file for clarity and consistency.
- **Email System**: Mailer uses an email address list instead of a user email.

## Refactored
- **Import Process**: Improved the CSV to JSON import process with enhanced error handling and user feedback.
- **File Structure**: Moved the CSV importer to the Maven project root and updated the readme for better organization.

## Build
- **Dependencies**: Bumped `com.github.librepdf:openpdf` from 1.3.30 to 3.0.0.
- **Dependencies**: Bumped `org.codehaus.mojo:license-maven-plugin`.
- **Dependencies**: Bumped `quarkus.platform.version` from 3.26.4 to 3.28.1.
- **Dependencies**: Bumped `com.diffplug.spotless:spotless-maven-plugin`.
- **Dependencies**: Bumped `react-i18next` from 15.7.3 to 16.0.0 in `/webapp`.
- **Dependencies**: Bumped `@types/node` from 24.5.2 to 24.6.1 in `/webapp`.
- **Dependencies**: Bumped `org.apache.commons:commons-csv` from 1.10.0 to 1.14.1.
- **Dependencies**: Bumped `quarkus.platform.version` from 3.28.1 to 3.28.2.
- **Dependencies**: Bumped `org.codehaus.mojo:exec-maven-plugin`.

---
# Release 0.0.5

This release introduces internationalization support for event and reservation messages, enhances event and reservation management, adds secure cookie configuration for JWT, improves seat reservation map performance, and includes various refactorings and updates.

## New Features
- **Internationalization**: The application now supports internationalization for event and reservation messages, allowing for better adaptation to different languages.
- **Event and Reservation Management**: General improvements have been made to event and reservation management to enhance usability and functionality.
- **Secure Cookie Configuration**: Added secure cookie configuration for JWT to support HTTP connections, with updated related tests.
- **Seat Reservation Map Performance**: Updated seat reservation map performance and added missing translations.
- **Metrics Monitoring**: Integrated Micrometer with Prometheus and Grafana to provide comprehensive metrics monitoring for application performance and health.

## Changes
- **UI/UX**: The layout and styling of the `StartPage` and `SearchAndFilter` components have been updated to provide a more modern appearance and improved user experience.
- **Application Properties**: Removed unused JVM and system metrics binders from application properties.
- **Login Handling**: Trimmed login identifier for better input handling.
- **Page Structure**: Split reservations and events pages for improved navigation.

## Technical Improvements
- **Refactoring**:
    - Event handling has been refactored, so event reservations now return an ID, and a new endpoint for requesting event locations has been added.
    - Event Location and Marker DTOs have been revised.
    - `DetailedEventResponseDTO` and `DetailedReservationResponseDTO` have been renamed to `EventResponseDTO` and `ReservationResponseDTO` to improve consistency.
    - Event and reservation DTOs have been refactored into `UserEventResponseDTO` and `UserReservationResponseDTO`.
    - `DetailedEventResponseDTO` now uses `eventLocationId` instead of `EventLocationResponseDTO`, and related components and tests have been updated accordingly.
    - Event and event location DTOs have been revised to separate status from event location.
    - Date handling in the backend and frontend has been refactored to ensure more consistent and robust handling of dates.
- **Build & Dependencies**:
    - The Maven plugins `maven-compiler-plugin` and `license-maven-plugin` have been updated.
    - Frontend dependencies such as `@hey-api/openapi-ts` and `@tanstack/react-query` in the `webapp` have been updated.
- **CI/CD**: The trigger for `backend-native.yml` has been adjusted to reduce build load by running only on tags (later changed back to every push on `main`).

---

# Release 0.0.4

This release extends the seat import functionality with the new `seatRow` attribute and improves test coverage as well as API documentation.

## New Features

- **Seat Row Import**: When importing seats, the seat row (`seatRow`) can now be specified. This makes seat management and assignment easier.
- **API Extension**: The OpenAPI specification and the generated types in the webapp now include the new `seatRow` field.

## Improvements

- **Test Coverage**: Tests for EventLocationService and EventLocationResource have been updated to cover the new functionality.

## Changes

- **Logging**: Reduced log output by removing info logs for email confirmations and password changes.

# Release 0.0.3

This patch release adds important functionality for event location management and improves system compatibility for native image builds.

## New Features

### Event Location Management
- **Event Location Markers Support**: Enhanced event location import functionality now supports markers that can be placed on location maps to provide visual orientation points for users
- **Improved User Experience**: Login error handling with proper localized error messages for invalid credentials

### System Improvements
- **Native Image Compatibility**: Registered all DTOs for reflection to ensure proper functionality when building native images with GraalVM
- **Error Handling**: Added proper error titles and descriptions for login credential validation failures
- **Internationalization**: Enhanced translation support for error messages in both German and English

## Technical Enhancements

### Reflection Configuration
- Comprehensive DTO registration for native image builds ensures all data transfer objects work correctly in compiled native executables
- Improved build reliability for production deployments using native compilation

### Localization
- Added missing error message translations for authentication failures
- Improved user feedback with clear, localized error descriptions

### Logging
- Enhanced logging to reduce amount of info logs by changing log levels from INFO to DEBUG in various parts of the application

---

# Release 0.0.2

This major release brings significant enhancements to the Seat Reservation application, focusing on mobile user experience, new administrative features, and improved system reliability.

## Major New Features

### Administrative Tools
- **CSV Export**: Event managers can now export daily reservation data as CSV files for reporting and analysis
- **Email Verification**: Implemented secure 6-digit code email verification system with automatic cleanup
- **Enhanced Admin User Management**: Random password generation for security instead of hard-coded passwords

### Event & Location Management
- **Event Location Markers**: Visual markers can now be placed on event location maps for better orientation
- **Seat Row Information**: Added seat row attributes throughout the system for better seat identification
- **Booking Deadlines**: Event managers can set booking deadlines with proper validation and display

## User Experience Improvements

### Mobile Optimization
- **Fixed Android Auto-Capitalization**: Login and registration forms properly handle username input without unwanted auto-capitalization
- **Optimized Mobile Layout**: Dramatically improved space utilization with reduced padding, responsive grid layouts, and mobile-first design
- **Responsive Typography**: Better text sizing and spacing optimized for mobile screens
- **Performance**: Seat map rendering optimized with throttling and memoization for smoother interactions

### UI/UX Enhancements
- **Loading Experience**: Enhanced with skeleton loading animations across the application
- **Legend Accessibility**: Fixed overflow issues in reservation modals - legends now properly wrap on small screens
- **Enhanced Readability**: Made availability status more prominent in reservation forms
- **Smart Navigation**: Sidebar now automatically closes when navigating to improve mobile experience
- **Email Templates**: Improved email formatting with conditional headers and better structure

## Technical Improvements

### System Reliability
- **Enhanced Test Coverage**: Added comprehensive unit tests for better code quality and reliability
- **Visual Improvements**: Fixed seat number visibility by changing text color from white to black in SVG seat maps
- **Logging Optimization**: Reduced verbose logging by changing levels from INFO to DEBUG
- **Database Efficiency**: Implemented PostgreSQL triggers for automatic cleanup processes
- **Code Quality**: Cleaned up unused imports, updated ESLint configuration, and improved code organization

### Architecture & Security
- **Package Restructuring**: Reorganized codebase with cleaner separation of concerns (renamed eventManagement to management)
- **Email System**: Skip sending emails to localhost/empty addresses, reducing development noise
- **Docker Optimization**: Updated configuration for consistency with standard deployment practices
- **API Documentation**: Translated to English for better international accessibility

### Dependencies & Maintenance
- **Runtime Updates**: Upgraded Node.js base image from 22-alpine to 24-alpine for improved performance and security
- **Framework Updates**: Updated Quarkus platform to 3.26.4 with latest security patches
- **Frontend Dependencies**: Updated React Query, react-day-picker, and TypeScript definitions
- **Testing Framework**: Updated Mockito to 5.20.0 for improved test reliability
- **CI/CD**: Enhanced GitHub Actions workflows with updated dependencies
- **Dependabot**: Fixed missing Docker image monitoring for webapp directory

---

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