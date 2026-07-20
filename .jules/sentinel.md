## 2024-07-20 - Security Enhancement: Enforce SameSite=Strict for Authentication Cookies
**Vulnerability:** Authentication cookies (JWT, Refresh Token) do not enforce `SameSite=Strict`, leaving them potentially susceptible to CSRF attacks if other protections fail.
**Learning:** In Quarkus with Jakarta REST, we need to explicitly configure `sameSite(NewCookie.SameSite.STRICT)` on the `NewCookie.Builder`.
**Prevention:** Always set `SameSite=Strict` for sensitive authentication and session cookies.
