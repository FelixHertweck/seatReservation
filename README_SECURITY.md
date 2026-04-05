# Security Architecture Guide

This project integrates a robust, self-hosted security stack to protect against common attacks, brute forcing, and malicious bots using open-source tools.

## 1. NGINX Rate Limiting
Strict rate limiting is applied to the `/api/auth/login` and `/api/auth/register` routes.
- **Zone configuration**: A 10MB shared memory zone `login_limit` tracks IP addresses (`$binary_remote_addr`).
- **Limits**: We enforce a baseline rate of 5 requests per minute (`rate=5r/m`).
- **Bursts**: A `burst=5` parameter with `nodelay` allows legitimate users to click quickly or login concurrently without waiting, up to 5 times. Anything exceeding this bursts the limit and is rejected with HTTP 503 (or 429 based on config).

## 2. Coraza WAF (OWASP CRS)
We use Coraza, an enterprise-grade WAF that drops-in directly to NGINX.
- **NGINX Module**: Built into the custom NGINX image (`ghcr.io/corazawaf/coraza-nginx`), we load `ngx_http_coraza_module.so`.
- **Ruleset**: The OWASP Core Rule Set (CRS) is enabled to block common vulnerabilities (SQLi, XSS, RFI) and malicious payloads.
- **Action**: When a malicious payload is detected, Coraza immediately interrupts the request, logging the event and returning an HTTP 403 status.

## 3. CrowdSec (Collaborative IPS)
CrowdSec monitors NGINX logs for anomalous behavior (like path traversal, brute forcing) and blocks attacking IPs.
- **Agent**: Runs as a separate container parsing NGINX access logs (`/var/log/nginx`).
- **Bouncer**: The NGINX container has the `crowdsec-nginx-bouncer` installed. NGINX asks CrowdSec on every request if the IP is banned. If it is, the request is dropped.
- **Testing**: To test, you can intentionally trigger 404s or 403s on non-existent paths. CrowdSec will eventually ban your IP. Use `docker exec -it <crowdsec_container> cscli decisions list` to see bans, and `cscli decisions delete -i <your_ip>` to unban yourself.

## 4. Altcha (Proof-of-Work)
Altcha provides a privacy-preserving alternative to CAPTCHA using Proof-of-Work.
- **Challenge**: The frontend requests a challenge from `/api/auth/altcha-challenge`.
- **Validation**: On login or registration, the backend (`AltchaService`) verifies the `altchaPayload` by checking the HMAC SHA-256 signature and recalculating the Proof-of-Work to ensure it matches the challenge requirements.

## How to Test Locally
1. Start the stack using `docker-compose up -d --build`.
2. Try logging in repeatedly with invalid credentials to see the NGINX rate limit in action (you will eventually receive an error).
3. Try sending an SQL injection payload (e.g., `?id=1' OR '1'='1`) to see Coraza intercept it (HTTP 403).
4. Run `docker logs crowdsec` to observe it parsing logs and applying bans based on repeated offenses.
