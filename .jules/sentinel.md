## 2025-02-28 - Fix CSV Injection vulnerability via unescaped carriage returns
**Vulnerability:** A CSV injection vulnerability existed because the `\r` character was being improperly treated as spreadsheet-trimmable whitespace, bypassing the formula injection check.
**Learning:** Spreadsheet applications might process `\r` differently than normal whitespace when parsing leading characters. Blindly relying on standard `Character.isWhitespace(c)` checks might incorrectly trim such characters out and expose formula injection vectors.
**Prevention:** Ensure explicit exception rules exist for all formula trigger characters, such as `\r` and `\t`, preventing them from being mistakenly trimmed before evaluating CSV payloads.
