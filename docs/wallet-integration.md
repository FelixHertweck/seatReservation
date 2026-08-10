# Digital Wallet Integration Guide (Google Wallet, Apple Wallet & Generic PKPASS)

This guide describes how to configure Google Wallet, Apple Wallet, and Generic PKPASS passes in **SeatReservation** for dynamic ticket generation.

---

## 1. Architecture Overview

The application implements a generic, extensible backend design (`AbstractWalletPassGenerator`, `AbstractPkpassGenerator`, `GoogleWalletPassGenerator`, `AppleWalletPassGenerator`, `GenericPkpassGenerator`, `WalletPassService`) that generates wallet passes dynamically per reservation without requiring manual per-event setup in developer consoles.

```
                           ┌─────────────────────────┐
                           │   Reservation Entity    │
                           └────────────┬────────────┘
                                        │
                                        ▼
                           ┌─────────────────────────┐
                           │    WalletPassService    │
                           └────────────┬────────────┘
                                        │
             ┌──────────────────────────┼──────────────────────────┐
             ▼                          ▼                          ▼
  ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐
  │GoogleWalletGenerator│    │ AppleWalletGenerator│    │GenericPkpassGeneratr│
  └──────────┬──────────┘    └──────────┬──────────┘    └──────────┬──────────┘
             │                          │                          │
             │                          └────────────┬─────────────┘
             ▼                                       ▼
  Google Wallet JWT ("savetowallet")           PKPASS / PKPASSES Archive
                                            (.pkpass / .pkpasses Bundle)
```

---

## 2. Google Wallet Setup Guide

### Step 1: Google Pay & Wallet Console Setup
1. Go to the [Google Pay & Wallet Console](https://pay.google.com/business/console).
2. Create or select a Google Cloud Project.
3. Apply for **Google Wallet API access** (Passes API).
4. Retrieve your **Issuer ID** (a 19-digit numerical string, e.g. `3388000000001234567`).
5. **Enable Google Wallet API in Google Cloud:** Navigate to the [Google Cloud API Library: Google Wallet API](https://console.cloud.google.com/apis/library/walletobjects.googleapis.com) and click **Enable** for your project *(required for REST API event synchronization and class updates)*.

### Step 2: Create Service Account & Download Private Key
1. Open the [Google Cloud Console Credentials Page](https://console.cloud.google.com/apis/credentials).
2. Create a **Service Account** (e.g. `wallet-sa@your-project.iam.gserviceaccount.com`).
3. In Google Pay & Wallet Console, grant your Service Account **Editor / Dev access** to your Issuer ID.
4. Create a **JSON key** for the Service Account and download it.
5. Extract the `private_key` field from the downloaded JSON into a standalone PEM file:
   ```bash
   # Using jq and the downloaded key.json:
   cat key.json | jq -r '.private_key' > keys/google-wallet-sa.pem
   ```
6. Place the PEM file at `keys/google-wallet-sa.pem` (gitignored with the rest of `keys/`).

> **Note on EventTicketClass & EventTicketObject:**
> The Java backend automatically generates both `EventTicketClass` (keyed by event ID) and `EventTicketObject` (keyed by reservation ID) on-the-fly inside the "Save to Google Wallet" JWT payload. No manual class creation in the Google Pay Console is required. When an event's details or its event location (venue name/address) are edited, the backend automatically synchronizes changes across all affected passes by upserting (PATCH with fallback to POST insert) the `EventTicketClass` on the Google Wallet REST API.

> **Important:** The Google Wallet JWT **must** be signed with the Service Account private key.
> The application's own JWT signing key is a completely different key — using it produces an `INVALID_SIGNATURE` error when Google tries to validate the pass.

---

## 3. Apple Wallet & Generic PKPASS Setup Guide

> [!WARNING]
> **Apple Wallet Testing Status / Paid Developer Account Requirement:**
> The signed Apple Wallet integration (`.pkpass` generation and PKCS#7 signing) is **untested end-to-end**, as generating valid Pass Signing Certificates requires an active paid Apple Developer Account ($99/year).

### Generic PKPASS Generator (Out-of-the-Box)
To allow pass downloads without requiring an Apple Developer Account, **SeatReservation** includes `GenericPkpassGenerator` (`GENERIC_PKPASS` provider).
It creates unsigned `.pkpass` archives or `.pkpasses` bundles that can be opened by third-party Android and iOS wallet apps or imported into Apple Wallet (where allowed).

### Single Pass vs. Multi-Seat Bundles (.pkpasses)
When a user books multiple seats for the same event:
- **Single Seat**: Generates a `.pkpass` file (`Content-Type: application/vnd.apple.pkpass`).
- **Multiple Seats**: Generates a `.pkpasses` bundle archive (`Content-Type: application/vnd.apple.pkpasses`) containing individual `.pkpass` cards for each seat. Opening a `.pkpasses` bundle prompts Wallet apps to import all ticket cards in a single step.

### Step 1: Apple Developer Portal Setup (Optional for Signed Passes)
1. Log in to [Apple Developer Portal](https://developer.apple.com).
2. Go to **Identifiers -> Pass Type IDs** and register a Pass Type ID (e.g., `pass.de.felixhertweck.seatreservation`).
3. Create a **Pass Signing Certificate** associated with this Pass Type ID.
4. Download the WWDR (Apple Worldwide Developer Relations) Intermediate Certificate and your Pass Certificate.

### Step 2: PKPass Archive Structure
Apple Passes are compressed `.pkpass` ZIP archives containing:
- `pass.json`: Ticket layout, seat details, primary/secondary/auxiliary/back fields, location address, and barcode dictionary.
- `logo.png`, `icon.png`: Visual assets.
- `manifest.json`: SHA-1 hashes of all archive files.
- `signature`: PKCS#7 signature created with your Pass Signing Certificate (omitted for generic unsigned passes).

---

## 4. Application Configuration

Add the following environment variables or configure `application.yaml`:

```yaml
wallet:
  google:
    enabled: false
    issuer-id: "${GOOGLE_WALLET_ISSUER_ID:3388000000001234567}"
    class-id: "${GOOGLE_WALLET_CLASS_ID:seat_reservation_event_ticket}"
    service-account-email: "${GOOGLE_WALLET_SA_EMAIL:wallet-sa@project.iam.gserviceaccount.com}"
    # PEM-encoded RSA private key of the Google Service Account (extracted from downloaded JSON).
    service-account-key-path: "${GOOGLE_WALLET_SA_KEY_PATH:keys/google-wallet-sa.pem}"
    default-language: "${GOOGLE_WALLET_DEFAULT_LANGUAGE:en-US}"
    # Review status: UNDER_REVIEW (default for Demo/Dev mode) or APPROVED (for approved production accounts)
    review-status: "${GOOGLE_WALLET_REVIEW_STATUS:UNDER_REVIEW}"
    logo-uri: "${GOOGLE_WALLET_LOGO_URI:https://raw.githubusercontent.com/FelixHertweck/seatReservation/master/webapp/public/logo.png}"
  apple:
    enabled: false
    pass-type-identifier: "${APPLE_WALLET_PASS_TYPE_ID:pass.de.felixhertweck.seatreservation}"
    team-id: "${APPLE_WALLET_TEAM_ID:ABC1234567}"
    # PKCS#12 file exported from Keychain Access including the private key.
    certificate-path: "${APPLE_WALLET_CERT_PATH:keys/pass.p12}"
    certificate-password: "${APPLE_WALLET_CERT_PASSWORD:}"
    # Apple WWDR Intermediate Certificate (.pem / .cer from developer.apple.com).
    wwdr-certificate-path: "${APPLE_WALLET_WWDR_PATH:keys/wwdr.pem}"
  generic:
    enabled: true
    pass-type-identifier: "${GENERIC_WALLET_PASS_TYPE_ID:pass.de.felixhertweck.seatreservation}"
    team-id: "${GENERIC_WALLET_TEAM_ID:GENERIC123}"
```

### Apple Pass Certificate Placement
Place your downloaded certificates in the `keys/` directory (matching JWT & TOTP key security patterns):
- **Pass Signing Certificate (`.p12`)**: `keys/pass.p12` (Exported from Keychain Access with private key).
- **WWDR Intermediate Certificate (`.pem` / `.cer`)**: `keys/wwdr.pem` (Downloaded from Apple Developer Portal).

*Note: In Docker environments, mount these files read-only under `keys/` via Secrets or configure the paths using `APPLE_WALLET_CERT_PATH` and `APPLE_WALLET_WWDR_PATH`.*

---

## 5. REST API Endpoints

- **GET** `/api/user/wallet/config`
  Returns active wallet feature flags:
  ```json
  {
    "googleEnabled": false,
    "appleEnabled": false,
    "genericEnabled": true
  }
  ```

- **GET** `/api/user/reservations/{id}/wallet/GENERIC_PKPASS`
  Returns binary stream with `Content-Type: application/vnd.apple.pkpass` (single seat) or `application/vnd.apple.pkpasses` (multiple seats bundle).

- **GET** `/api/user/reservations/{id}/wallet/APPLE`
  Returns binary stream with `Content-Type: application/vnd.apple.pkpass` or `application/vnd.apple.pkpasses`.

- **GET** `/api/user/reservations/{id}/wallet/GOOGLE`
  Returns JSON:
  ```json
  {
    "provider": "GOOGLE",
    "url": "https://pay.google.com/gp/v/save/<JWT_TOKEN>",
    "contentType": "application/json"
  }
  ```

---

## 6. Trademarks & Disclaimers

- **Google Wallet** is a trademark of Google LLC.
- **Apple Wallet** and **PassKit** are trademarks of Apple Inc.
