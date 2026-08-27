## 2025-08-27 - Missing Carriage Return (\r) Escape in CSV Export
**Vulnerability:** A CSV injection vulnerability existed because the carriage return character (`\r`) was considered as ordinary whitespace instead of a formula trigger.
**Learning:** Spreadsheet programs like Excel and LibreOffice also process `\r` as a potential formula evaluation trigger or bypass when placed after whitespace.
**Prevention:** Ensure all potential trigger characters, including `\r` and `\t`, are explicitly handled during CSV injection escaping.
