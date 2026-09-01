## 2026-09-01 - CSV Injection via Carriage Return Formula Trigger
**Vulnerability:** The CSV exporter was vulnerable to CSV Formula Injection because it failed to recognize Carriage Return (`\r`) as an execution trigger for spreadsheet applications. It trimmed the whitespace, and evaluated `=cmd` when a cell started with ` \r=cmd`.
**Learning:** Spreadsheet applications (like Excel or LibreOffice) evaluate `\r` alongside `\t` before triggering formulas, therefore we should also exclude `\r` from `isSpreadsheetTrimmable` character set so the formula gets treated as plain string starting with an apostrophe (`'`).
**Prevention:** Exclude all spreadsheet trigger characters (`\t`, `\r`) from the whitespace trimming check before appending the apostrophe (`'`) safeguard in CSV exporters.
