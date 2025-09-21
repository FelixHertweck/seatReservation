# Changelog

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
