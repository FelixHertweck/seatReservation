import type { UserDto } from "@/api";

type EmailStatus = Pick<UserDto, "email" | "emailVerified">;

/**
 * True if the account has an email address and it has been verified.
 * Mirrors `User#hasVerifiedEmail` in the backend.
 */
export function hasVerifiedEmail(
  user: EmailStatus | undefined | null,
): boolean {
  return !!user?.email && !!user.emailVerified;
}

/**
 * True for accounts an admin deliberately created and verified without an
 * email. Mirrors `User#isVerifiedWithoutEmail` in the backend.
 */
export function isVerifiedWithoutEmail(
  user: EmailStatus | undefined | null,
): boolean {
  return !!user && !user.email && !!user.emailVerified;
}
