## 2023-11-20 - Fix CSV Injection in Exporter
**Vulnerability:** The CSV exporter was vulnerable to CSV injection because `isSpreadsheetTrimmable` didn't consider `\r` as whitespace, allowing payloads like `\rcmd` to bypass the leading character check.
**Learning:** Spreadsheets trim `\r` (carriage return) as well as `\t` when evaluating formula triggers, so we must manually skip it before checking if a cell value starts with an operator like `=`.
**Prevention:** Always explicitly define `\r` in spreadsheet trimmable lists, alongside `\t` and other whitespace characters when avoiding formula injection.
