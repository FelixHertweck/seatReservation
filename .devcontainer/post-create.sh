#!/bin/bash
set -e

echo "Running post-create setup..."

# Navigate to workspace
cd /workspaces/seatReservation

# Generate JWT keys if they don't exist
if [ ! -d "keys" ]; then
    echo "Generating JWT keys..."
    mkdir -p keys
    openssl genpkey -algorithm RSA -out keys/privateKey.pem -pkeyopt rsa_keygen_bits:2048
    openssl rsa -pubout -in keys/privateKey.pem -out keys/publicKey.pem
    echo "JWT keys generated successfully."
else
    echo "JWT keys already exist."
fi

# Copy .env.example to .env if .env doesn't exist
if [ ! -f ".env" ]; then
    echo "Creating .env file from .env.example..."
    cp .env.example .env
    echo ".env file created. Please update it with your configuration."
else
    echo ".env file already exists."
fi

# Install backend dependencies (Maven will download dependencies)
echo "Installing backend dependencies..."
./mvnw dependency:resolve -DskipTests

# Install frontend dependencies
echo "Installing frontend dependencies..."
cd webapp
npm install
cd ..

echo "Post-create setup completed successfully!"
echo ""
echo "Next steps:"
echo "1. Update the .env file with your configuration"
echo "2. Run the backend: ./mvnw quarkus:dev"
echo "3. Run the frontend: cd webapp && npm run dev"
echo ""
echo "Ports:"
echo "- Backend: http://localhost:8080"
echo "- Frontend: http://localhost:3000"
echo "- PostgreSQL: localhost:5432"
