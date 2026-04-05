FROM ghcr.io/corazawaf/coraza-nginx:latest

# Install tools
RUN apt-get update && \
    apt-get install -y curl gnupg2 apt-transport-https

# Add Crowdsec repository and install NGINX bouncer
RUN curl -s https://packagecloud.io/install/repositories/crowdsec/crowdsec/script.deb.sh | sh && \
    apt-get update && apt-get install -y crowdsec-nginx-bouncer && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# The bouncer config typically lives at /etc/crowdsec/bouncers/crowdsec-nginx-bouncer.conf
# We must edit it or inject the API key at runtime.
