## 2026-09-13 - CSV Injection via Carriage Return

**Vulnerability:** The CSV exporter failed to recognize Carriage Return (\r) as a formula trigger and incorrectly treated it as trimmable whitespace, allowing CSV injection payloads starting with \r to bypass escaping.
**Learning:** When mitigating CSV injection, Character.isWhitespace() is dangerous because it returns true for \t and \r, which are actually formula triggers in some spreadsheet software and thus must be excluded from trimming logic.
**Prevention:** Explicitly exclude \t and \r from whitespace trimming loops, and ensure they trigger the prepending of an apostrophe (') in manual CSV formatting.
