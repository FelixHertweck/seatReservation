# Development Container

This directory contains the configuration for a development container that provides a consistent development environment for the Seat Reservation System.

## What's Included

The development container includes:

- **Java 21** - Required for the Quarkus backend
- **Node.js 20.x (LTS)** - Required for the Next.js frontend
- **Maven** - Build tool for the backend (via SDKMAN)
- **PostgreSQL 17** - Database (running in a separate container)
- **Docker-in-Docker** - For building and running containers
- **Git** - Version control

## VS Code Extensions

The following extensions are automatically installed:

### Java Development
- Red Hat Java Extension Pack
- Quarkus Tools
- Maven for Java

### JavaScript/TypeScript Development
- ESLint
- Prettier

### Container Development
- Docker

### General Development
- GitLens
- EditorConfig

## Getting Started

### Prerequisites

- [Visual Studio Code](https://code.visualstudio.com/)
- [Dev Containers extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers)
- [Docker Desktop](https://www.docker.com/products/docker-desktop)

### Opening the Project

1. Install the prerequisites above
2. Clone this repository
3. Open the repository folder in VS Code
4. When prompted, click "Reopen in Container" (or use Command Palette: `Dev Containers: Reopen in Container`)
5. Wait for the container to build and the post-create script to complete

### What Happens on Container Creation

The `post-create.sh` script automatically:

1. Generates JWT keys (if not already present)
2. Creates a `.env` file from `.env.example` (if not already present)
3. Downloads backend dependencies
4. Installs frontend dependencies

### Running the Application

After the container is created, you can run:

**Backend (Quarkus):**
```bash
./mvnw quarkus:dev
```

**Frontend (Next.js):**
```bash
cd webapp
npm run dev
```

### Accessing Services

The following ports are automatically forwarded to your local machine:

- **Backend API**: http://localhost:8080
- **Frontend**: http://localhost:3000
- **PostgreSQL**: localhost:5432
  - Database: `seatReservation`
  - User: `postgres`
  - Password: `postgres`

### Environment Configuration

Don't forget to update the `.env` file with your configuration, especially:

- Email settings (for notifications)
- Application URL
- Any other environment-specific settings

## Troubleshooting

### Container won't build

- Ensure Docker Desktop is running
- Try rebuilding the container: Command Palette → `Dev Containers: Rebuild Container`

### PostgreSQL connection issues

- The database container starts automatically with the dev container
- Connection string: `jdbc:postgresql://localhost:5432/seatReservation`
- Credentials are in the `.env` file

### Maven or npm commands not found

- The post-create script should install these automatically
- If issues persist, rebuild the container

## Additional Information

For more information about development containers, see:
- [VS Code Dev Containers documentation](https://code.visualstudio.com/docs/devcontainers/containers)
- [Dev Container specification](https://containers.dev/)
