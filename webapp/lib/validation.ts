/**
 * Client-side mirrors of the backend validation rules, so modals can reject invalid input
 * before a request is sent. Each validator returns an i18n key (under `validation.`) or `null`.
 * The backend stays authoritative; keep these in sync with it.
 */

// AdminUserCreationDto / UserCreationDTO
export const USERNAME_PATTERN = /^[a-zA-Z0-9._-]{3,64}$/;
// AdminUserUpdateDTO
export const EMAIL_PATTERN = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
// PasswordPolicy.MIN_LENGTH
export const PASSWORD_MIN_LENGTH = 8;

export const isBlank = (value: string | null | undefined): boolean =>
  !value || value.trim().length === 0;

export interface EventTimes {
  startTime: string;
  endTime: string;
  bookingStartTime: string;
  bookingDeadline: string;
  reminderSendDate?: string;
}

/** Mirrors EventService.validateEventTiming (all comparisons strict). */
export function validateEventTimes(times: EventTimes): string | null {
  const start = Date.parse(times.startTime);
  const end = Date.parse(times.endTime);
  const bookingStart = Date.parse(times.bookingStartTime);
  const deadline = Date.parse(times.bookingDeadline);
  if ([start, end, bookingStart, deadline].some(Number.isNaN)) {
    return "validation.event.datesRequired";
  }
  if (start >= end) return "validation.event.startBeforeEnd";
  if (deadline >= end) return "validation.event.deadlineBeforeEnd";
  if (bookingStart >= deadline) {
    return "validation.event.bookingStartBeforeDeadline";
  }
  if (times.reminderSendDate) {
    const reminder = Date.parse(times.reminderSendDate);
    if (Number.isNaN(reminder)) return "validation.event.reminderInvalid";
    if (reminder >= start) return "validation.event.reminderBeforeStart";
  }
  return null;
}

export function validateUsername(username: string): string | null {
  return USERNAME_PATTERN.test(username) ? null : "validation.usernameHint";
}

/** Email is optional; when present it must match the backend pattern. */
export function validateOptionalEmail(email: string): string | null {
  const trimmed = email.trim();
  if (!trimmed) return null;
  return EMAIL_PATTERN.test(trimmed) ? null : "validation.emailInvalid";
}
