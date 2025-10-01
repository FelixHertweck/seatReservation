# Changelog

## 0.0.6 - 2025-10-01

### Added
- **Event Management**: Added booking start time to event management and reservation system.
- **UI/UX**: Prevented closing modals when interacting outside.
- **User Management**: Added ability to add user with email and send verification at user login time.
- **Email System**: Enhanced reservation confirmation email to support additional email addresses and added send confirmation mail also to manager user.
- **Export Functionality**: Updated CSV export format to include seat row and status.
- **Export Functionality**: Added seat data to location export functionality.
- **UI/UX**: Enhanced marker component with dynamic sizing and centering.
- **Email System**: Added BCC support and improved email content formatting in EmailService.
- **Export Functionality**: Implemented PDF export functionality for reservations.

### Fixed
- **Dependencies**: Replaced openpdf dependency with quarkus-openpdf and updated dockerfile to fix native image file error.
- **Event Management**: Validated booking start time before booking deadline.
- **UI/UX**: Updated event card status badge.
- **User Management**: Persisted user before sending confirmation mail to ensure user has ID.
- **Email System**: Mailer now uses email address list instead of single user email.
- **Email System**: Prevented duplicate Bcc addresses in email recipients.
- **Export Functionality**: Reset selected format in export modal to default.
- **Configuration**: Corrected export template path in docker-compose configuration.
- **Logging**: Changed log level from info to debug for reservation export methods.
- **Internationalization**: Translated into English.
- **UI/UX**: Adjusted SVG styling for full width and height rendering.
- **UI/UX**: Don't return blocked seats as reservation of user and updated height of reservation modals.
- **UI/UX**: Integrated reservations into events page to show existing reservations.

### Changed
- **Documentation**: Updated README and SQL import file for clarity and consistency.
- **Email System**: Mailer uses email address list instead of user email.

### Refactored
- **Import Process**: Improved CSV to JSON import process with enhanced error handling and user feedback.
- **File Structure**: Moved CSV importer to maven project root and updated readme.

### Build
- **Dependencies**: Bumped com.github.librepdf:openpdf from 1.3.30 to 3.0.0.
- **Dependencies**: Bumped org.codehaus.mojo:license-maven-plugin.
- **Dependencies**: Bumped quarkus.platform.version from 3.26.4 to 3.28.1.
- **Dependencies**: Bumped com.diffplug.spotless:spotless-maven-plugin.
- **Dependencies**: Bumped react-i18next from 15.7.3 to 16.0.0 in /webapp.
- **Dependencies**: Bumped @types/node from 24.5.2 to 24.6.1 in /webapp.
- **Dependencies**: Bumped org.apache.commons:commons-csv from 1.10.0 to 1.14.1.
- **Dependencies**: Bumped quarkus.platform.version from 3.28.1 to 3.28.2.
- **Dependencies**: Bumped org.codehaus.mojo:exec-maven-plugin.

## 0.0.5 - 2025-09-26

### Added
- **Internationalization**: Added support for event and reservation messages.
- **Event and Reservation Management**: Enhancements in event and reservation management.
- **Secure Cookie Configuration**: Added secure cookie configuration for JWT to support HTTP connections, and updated related tests.
- **Seat Reservation Map Performance**: Updated seat reservation map performance and added missing translations.
- **Metrics Monitoring**: Integrated Micrometer with Prometheus and Grafana for comprehensive metrics monitoring.

### Changed
- **Layout & Styling**: Updated layout and styling in `StartPage` and `SearchAndFilter` components.
- **JVM Metrics**: Removed unused JVM and system metrics binders from application properties.
- **Login Identifier**: Trimmed login identifier for better input handling.
- **Pages**: Split reservations and events pages for improved navigation.

### Refactored
- **Event Handling**: Event reservation now returns ID and an endpoint to request event locations has been added.
- **DTOs**: Refactored Event Location and Marker DTOs.
- **DTOs**: Renamed `DetailedEventResponseDTO` and `DetailedReservationResponseDTO` to `EventResponseDTO` and `ReservationResponseDTO`.
- **DTOs**: Refactored event and reservation DTOs to `UserEventResponseDTO` and `UserReservationResponseDTO`.
- **DTOs**: `DetailedEventResponseDTO` now uses `eventLocationId` instead of `EventLocationResponseDTO`; related components and tests updated accordingly.
- **DTOs**: Refactored event and event location DTOs to separate status from event location.
- **Date Handling**: Refactored date handling in backend and frontend.

### Build
- **Maven Plugins**: Updated `maven-compiler-plugin` and `license-maven-plugin`.
- **Frontend Dependencies**: Updated `@hey-api/openapi-ts` and `@tanstack/react-query` in `webapp`.

### Chore
- **CI/CD Trigger**: Adjusted trigger for `backend-native.yml`, now runs only on tags (later changed back to every push on `main`).

## 0.0.4 - 2025-09-21

### Added
- **Seat Row Attribute**: The `ImportSeatDto` DTO and related services now support the new `seatRow` attribute for seat row specification during import.
- **API & Webapp**: The new `seatRow` field is included in the OpenAPI specification and the generated TypeScript types.

### Improved
- **Tests**: Tests for EventLocationService and EventLocationResource have been updated to cover the new `seatRow` attribute.

### Changed
- **Logging**: Info logs for email confirmations and password change notifications have been removed to reduce log verbosity.

## 0.0.3 - 2025-09-21

### Added
- **Event Location Markers**: Added functionality to support markers at import of event location for better visual orientation
- **Reflection Configuration**: Registered DTOs for reflection to support native image builds with GraalVM
- **Error Translations**: Added error title and description for invalid login credentials in German and English translations

### Improved
- **Native Image Support**: Enhanced system compatibility for native compilation by properly configuring reflection for all DTOs
- **User Feedback**: Better error handling and localized messages for authentication failures
- **Import Functionality**: Event location import now supports marker placement for enhanced user experience

### Fixed
- **Login Error Handling**: Proper error messages are now displayed when invalid credentials are provided
- **Native Build Compatibility**: Resolved issues with DTO serialization in native image builds
- **Logging**: Changed log levels from INFO to DEBUG to reduce verbose logging

## 0.0.2 - 2025-09-21

### Added
- **CSV Export**: Daily CSV export of reservations for event managers
- **Email Verification**: 6-digit code email verification functionality
- **Event Location Markers**: Support for visual markers on event location maps
- **Seat Row Attribute**: Added seat row information to components and database
- **Booking Deadline**: Added booking deadline column to event management
- **Unit Tests**: Enhanced test coverage for better code quality

### Improved
- **Mobile UX**: Fixed automatic capitalization on Android login/register forms with proper `autoCapitalize="none"` and `autoComplete="username"` attributes
- **Responsive Layout**: Comprehensive mobile layout improvements
  - Optimized space utilization with reduced padding and margins
  - Enhanced grid layouts with responsive breakpoints (`sm:grid-cols-2 lg:grid-cols-3`)
  - Improved card spacing and typography for mobile devices
- **Performance**: Optimized seat map rendering with throttling and memoization
- **UI/UX Enhancements**:
  - Fixed legend overflow in reservation modals with `flex-wrap`
  - Enhanced loading animations with skeleton components
  - Made availability status more prominent in forms
  - Improved sidebar behavior to close on navigation
- **Email System**: 
  - Enhanced email templates with conditional headers
  - Skip sending emails for localhost/empty addresses
  - Email verification with automatic PostgreSQL cleanup triggers

### Fixed
- **SVG Rendering**: Changed seat number text color from white to black for better visibility on light backgrounds
- **Dependabot**: Added missing schedule configuration for Docker images in webapp directory
- **Documentation**: Translated API documentation to English
- **Docker**: Updated Dockerfile to use `/deployments/` directory for consistency
- **Spelling**: Corrected 'Address' spelling in email validation methods
- **Code Quality**: Cleaned up unused imports and updated ESLint configuration

### Security
- **Password Generation**: Implemented random password generation for admin users instead of hard-coded passwords

### Refactored
- **Package Structure**: Renamed `eventManagement` and `manager` packages to `management`
- **Logging**: Changed log levels from INFO to DEBUG to reduce verbose logging
- **Code Organization**: 
  - Moved shared exceptions to common package
  - Removed duplicate DTOs
  - Optimized seat DTO creation with reservation status mapping
  - Moved export endpoints to management package

### Dependencies
- **Node.js**: Updated base Docker image from Node 22-alpine to 24-alpine for webapp
- **Backend**: Updated Quarkus platform from 3.26.3 to 3.26.4
- **Frontend**: Updated React Query, react-day-picker, and @types/node dependencies
- **Testing**: Updated Mockito from 5.19.0 to 5.20.0
- **GitHub Actions**: Updated cosign-installer and other workflow dependencies

## 0.0.1 - 2025-09-15

### Changed
- Initial release with version 0.0.1.
- Implemented seat reservation functionality, user management, and event management features.
- Added HTML sanitization for security.
- Integrated email verification and scheduling for cleanup.
- Developed a responsive web application with internationalization support.
