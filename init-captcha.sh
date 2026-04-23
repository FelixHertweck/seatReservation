#!/bin/bash
# Script to help initialize CAPTCHA keys for Cap

echo "Cap (CAPTCHA) Initialization Helper"
echo "-----------------------------------"
echo "Cap standalone needs to be started to generate a site key and secret key."
echo ""
echo "1. First, make sure you have generated a secure password for CAP_ADMIN_KEY in your .env file."
echo "2. Start the Cap container by running: docker compose up -d cap valkey"
echo "3. Open your browser and go to: http://localhost:3000 (or the port mapped to cap)"
echo "4. Log in using your CAP_ADMIN_KEY."
echo "5. In the Cap dashboard, create a new site key."
echo "6. Copy the generated Site Key and Secret Key."
echo "7. Update your .env file with these values:"
echo "   CAP_SITE_KEY=<your-site-key>"
echo "   CAP_SECRET_KEY=<your-secret-key>"
echo "   NEXT_PUBLIC_CAP_SITE_KEY=<your-site-key>"
echo "8. Restart your application: docker compose down && docker compose up -d"
echo ""
echo "Note: Make sure your Nginx proxy maps /captcha/ to the Cap container properly."

chmod +x init-captcha.sh
