# Email Templates

This directory contains HTML templates for emails sent by the Seat Reservation System.

## Available Templates

- `email-confirmation.html`: Template for email confirmation messages

## How to Use

The email templates can be used through the `EmailService` class, which provides methods for sending templated emails:

1. `sendEmailConfirmation(String recipient, String userName, String confirmationLink)`: Sends an email confirmation message using the `email-confirmation.html` template.
2. `sendTemplatedEmail(String recipient, String subject, String templateName, Map<String, String> placeholders)`: Sends a generic templated email using any template in this directory.

## Placeholders

Templates use placeholders in the format `{placeholderName}` that are replaced with actual values when the email is sent. Common placeholders include:

- `{userName}`: The name of the recipient
- `{confirmationLink}`: The link to confirm the email address
- `{currentYear}`: The current year (automatically added if not provided)

## Testing Email Confirmation Flow

To test the email confirmation flow:

1. Send a POST request to `/api/email/send-confirmation` with a JSON body containing:
   ```json
   {
     "email": "user@example.com",
     "name": "John Doe"
   }
   ```

2. The system will generate a confirmation token and send an email with a confirmation link.

3. When the user clicks the confirmation link, they will be directed to `/api/user/confirm-email?token=<token>`, which will validate the token and display a success or error page.

## Adding New Templates

To add a new email template:

1. Create a new HTML file in this directory
2. Use placeholders in the format `{placeholderName}` for dynamic content
3. Use the `sendTemplatedEmail` method in `EmailService` to send emails using the new template

## Styling Guidelines

Email templates should follow these styling guidelines:

- Use inline CSS for compatibility with email clients
- Keep the design simple and responsive
- Use a consistent color scheme and typography
- Include both text and HTML versions when possible
- Test in multiple email clients before deploying