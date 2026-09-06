## 2025-02-24 - CSV Formula Injection Bypass via Carriage Return
**Vulnerability:** A CSV Formula Injection vulnerability could be triggered by prepending a carriage return (`\r`) to formula trigger characters like `=`, bypassing the whitespace trimming logic because `\r` is considered whitespace but acts as an execution trigger in spreadsheets.
**Learning:** `Character.isWhitespace()` evaluates to true for `\r`, causing the mitigation check to skip it. When validating CSV inputs, dangerous characters that double as whitespace (like `\t` and `\r`) must be explicitly excluded from trimming loops so they can be identified and neutralized.
**Prevention:** Explicitly exclude both tab (`\t`) and carriage return (`\r`) when manually trimming whitespace to detect CSV Formula Injection triggers.
