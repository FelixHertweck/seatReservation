## 2025-09-07 - CSV Injection Bypass via Carriage Return (\r)
**Vulnerability:** The CSV exporter properly escaped common formula trigger characters (`=`, `+`, `-`, `@`, `\t`) and ignored standard whitespace, but did not handle `\r` (carriage return).
**Learning:** `\r` is treated as whitespace by Java's `Character.isWhitespace()`, but spreadsheet software does NOT trim `\r` and recognizes it as a trigger for formulas, allowing CSV formula injection payloads (e.g. `\r=cmd`) to bypass the filter.
**Prevention:** Always explicitly check for and exclude both `\t` and `\r` in CSV formula sanitization loops, and treat both as formula triggers when they are the first non-trimmed character in a field.
