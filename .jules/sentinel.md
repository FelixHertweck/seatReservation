## 2026-08-28 - CSV Injection via Carriage Return
**Vulnerability:** A CSV injection vulnerability existed where malicious input prefixed with a carriage return (`\r`) followed by a formula trigger character (e.g. `=`) bypassed the CSV escaping mechanism.
**Learning:** When trimming leading whitespace to detect formula triggers in CSV data for spreadsheet applications, both tab (`\t`) and carriage return (`\r`) must be explicitly excluded from being stripped, as they are not trimmed by default in certain spreadsheet software and can act as formula triggers or whitespace prefixes to them.
**Prevention:** Always explicitly check for and prevent trimming of `\t` and `\r` when sanitizing CSV field inputs against formula injection.
