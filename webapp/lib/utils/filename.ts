export function sanitizeFileName(name: string | undefined, fallback = "event") {
  if (!name) return fallback;
  // Replace characters that are invalid or problematic in filenames on many OSes
  // (<, >, :, ", /, \\, |, ?, *, and control chars) with a single hyphen.
  let s = name.replace(/[<>:\"/\\|?*\x00-\x1F]/g, "-");
  // Replace multiple whitespace characters with single space and trim
  s = s.replace(/\s+/g, " ").trim();
  // Collapse multiple hyphens
  s = s.replace(/-+/g, "-");
  // Remove leading/trailing spaces and dots which can be problematic on Windows
  s = s.replace(/^[ .]+|[ .]+$/g, "");
  // Limit length to a reasonable number
  if (s.length > 100) s = s.slice(0, 100);
  return s || fallback;
}
