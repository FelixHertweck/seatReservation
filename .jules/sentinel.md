## 2024-05-18 - Fix CSV Injection Bypass with Carriage Return (\r)

**Vulnerability:**
The `ReservationExporter` was vulnerable to a CSV Formula Injection bypass. The protection logic (`isSpreadsheetTrimmable`) stripped leading whitespaces before checking for dangerous formula characters (`=`, `+`, `-`, `@`, `\t`). However, it only ignored `\t` as whitespace, failing to account for `\r` (Carriage Return). Since `\r` is considered whitespace by `Character.isWhitespace(char)` but NOT trimmed by spreadsheet software (like Excel/LibreOffice) when parsing formulas, an attacker could inject `\rcmd` to bypass the security check.

**Learning:**
When preventing CSV Formula Injection, it is critical to align the definition of "trimmable whitespace" exactly with how target spreadsheet applications parse cells. Relying solely on `Character.isWhitespace()` can allow unrecognized trigger characters (like `\r`) to bypass validation while still being executed by the spreadsheet engine.

**Prevention:**
Always explicitly test all potential whitespace and control characters against the CSV sanitization logic. Specifically, exclude both `\t` and `\r` from any trimming logic designed to find the first "meaningful" character of a CSV field, or prepend the quote directly if any dangerous character is present.
