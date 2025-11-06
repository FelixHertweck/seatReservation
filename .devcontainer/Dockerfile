# Use the official Microsoft Java devcontainer image with Java 21 and Node.js
FROM mcr.microsoft.com/devcontainers/java:1-21-bullseye

# Install Node.js 20.x (LTS)
RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && apt-get clean -y && rm -rf /var/lib/apt/lists/*

# Verify installations
RUN java -version && node --version && npm --version

# Install global npm packages
RUN npm install -g npm@latest

# Set the default user
USER vscode

# Install SDKMAN and Maven
RUN curl -s "https://get.sdkman.io" | bash \
    && bash -c "source $HOME/.sdkman/bin/sdkman-init.sh \
    && sdk install maven"

# Add SDKMAN to bashrc for interactive shells
RUN echo 'source "$HOME/.sdkman/bin/sdkman-init.sh"' >> ~/.bashrc
