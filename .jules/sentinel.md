## 2025-03-05 - CSV Injection bypass via Carriage Return (\r)
**Vulnerability:** CSV Injection bypass in ReservationExporter.java using Carriage Return (\r).
**Learning:** Character.isWhitespace('\r') returns true, meaning custom trim methods for CSV injection may inadvertently strip \r and miss it as a formula trigger. Additionally, \r itself acts as a formula trigger in some spreadsheet software and must be explicitly escaped.
**Prevention:** Explicitly exclude \r (along with \t) when manually trimming whitespace to detect CSV Formula Injection triggers, and ensure \r is included in the list of triggers that require escaping.
