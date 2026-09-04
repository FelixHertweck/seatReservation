## 2025-03-05 - CSV Formula Injection via Carriage Return
**Vulnerability:** The CSV exporter failed to recognize Carriage Return (\r) as a formula trigger character because the whitespace trimming logic skipped it and the trigger check missed it.
**Learning:** Spreadsheet software ignores leading whitespace but evaluates formula characters like Tab and Carriage Return. Trimming must explicitly exclude \r, and the character must be handled as a formula trigger.
**Prevention:** Explicitly exclude \t and \r from isWhitespace checks when detecting CSV injection, and test specifically with payloads like `\rcmd` rather than combining with other triggers.
