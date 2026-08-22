## 2026-08-22 - CSV Injection via Tab Whitespace Bypass
**Vulnerability:** A CSV injection vulnerability existed because `Character.isWhitespace('\t')` evaluated to true. This allowed malicious payloads starting with leading spaces followed by a tab (e.g., `  \t=cmd|...`) to bypass the formula trigger check when exporting CSVs.
**Learning:** `Character.isWhitespace()` and `Character.isSpaceChar()` cover characters like `\t`, which can still trigger formulas when opened in spreadsheet applications, bypassing checks that simply look past standard whitespaces for trigger characters.
**Prevention:** Explicitly exclude `\t` from whitespace evaluation in CSV parsing logic, and ensure `\t` is checked alongside typical formula triggers (`=`, `+`, `-`, `@`) as a malicious prefix.
