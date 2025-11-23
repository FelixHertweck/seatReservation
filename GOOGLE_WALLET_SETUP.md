# Google Wallet Integration Setup

This document describes how to set up the Google Wallet integration for the Seat Reservation System.

## Prerequisites

1.  A Google Cloud Project.
2.  Access to the [Google Pay & Wallet Console](https://pay.google.com/business/console/).

## Steps

### 1. Create a Google Wallet Issuer Account

1.  Go to the [Google Pay & Wallet Console](https://pay.google.com/business/console/).
2.  Create a new Issuer Account.
3.  Note down your **Issuer ID**. You will need this for the configuration.

### 2. Create a Service Account

1.  Go to the [Google Cloud Console](https://console.cloud.google.com/).
2.  Select your project.
3.  Go to **IAM & Admin** > **Service Accounts**.
4.  Create a new Service Account (e.g., `seat-reservation-wallet`).
5.  Note down the **Service Account Email** (e.g., `seat-reservation-wallet@your-project.iam.gserviceaccount.com`).
6.  Create a key for this service account:
    - Click on the service account.
    - Go to the **Keys** tab.
    - Click **Add Key** > **Create new key**.
    - Select **JSON** (recommended for general use) or **P12**.
    - **IMPORTANT**: For this application, we need the **Private Key in PKCS#8 PEM format**.
    - If you download the JSON key, extract the `private_key` field. It should look like `-----BEGIN PRIVATE KEY-----...-----END PRIVATE KEY-----`.
    - Save this private key to `keys/privatKey-google-wallet.pem` in your project root.

### 3. Grant Access to the Service Account

1.  Go back to the [Google Pay & Wallet Console](https://pay.google.com/business/console/).
2.  Go to **Users**.
3.  Click **Invite a user**.
4.  Enter the email address of the Service Account you created.
5.  Grant **Developer** or **Admin** access.

### 4. Configure the Application

Update your `application.yaml` (or use environment variables) with the Issuer ID and Service Account Email:

```yaml
email:
  google:
    wallet:
      issuer-id: "YOUR_ISSUER_ID"
      service-account-email: "YOUR_SERVICE_ACCOUNT_EMAIL"
      key:
        location: keys/privatKey-google-wallet.pem
```

_Note: You do NOT need to create a Class ID manually. The application will automatically generate a unique Class for each Event._

## Testing

1.  Start the application.
2.  Make a reservation.
3.  Check the confirmation email.
4.  Click the "Add to Google Wallet" button.
5.  It should redirect you to a Google Pay save URL.

## Troubleshooting

- **400 Bad Request**: Check if the JSON Web Token (JWT) is correctly signed and structured.
- **401 Unauthorized**: Check if the Service Account has access to the Issuer Account.
- **Pass not saved**: Check if the Service Account Email matches the `iss` claim in the JWT.
