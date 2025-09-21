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