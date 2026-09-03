## 2026-09-03 - CSV Injection fix for carriage return
**Vulnerability:** CSV injection (formula injection) vulnerability due to unescaped carriage return (`\r`) trigger characters.
**Learning:** Spreadsheet applications treat `\r` as a valid formula trigger, and simple whitespace trimming may incorrectly skip it or fail to escape it if it's not explicitly defined as a non-trimmable trigger character.
**Prevention:** Always explicitly exclude all spreadsheet formula triggers (including `\r` and `\t`) from whitespace trimming logic and ensure they are prepended with an escape character (like `'`) during CSV generation.
