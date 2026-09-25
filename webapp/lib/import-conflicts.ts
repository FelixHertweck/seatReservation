import type {
  AdminUserCreationDto,
  UserDto,
  UserImportResolutionDto,
} from "@/api";

// Usernames that are reserved for system accounts (mirrors the backend).
const RESERVED_USERNAMES = new Set(["boxoffice"]);

export type ImportConflictReason =
  "USERNAME_EXISTS" | "DUPLICATE_IN_BATCH" | "RESERVED_USERNAME";

export interface ImportConflict {
  user: AdminUserCreationDto;
  reason: ImportConflictReason;
  // Set for USERNAME_EXISTS: the user that already has this username.
  existing?: UserDto;
}

export interface ImportConflictResult {
  clean: AdminUserCreationDto[];
  conflicts: ImportConflict[];
}

export const usernameKey = (username: string | undefined): string =>
  (username ?? "").trim().toLowerCase();

// Splits the users to import into those that can be created right away and
// those that conflict. Usernames are compared case-insensitively; for
// duplicates inside the import the first entry wins.
export function detectImportConflicts(
  users: AdminUserCreationDto[],
  existingUsers: UserDto[],
): ImportConflictResult {
  const existingByKey = new Map(
    existingUsers.map((user) => [usernameKey(user.username), user]),
  );
  const seen = new Set<string>();
  const clean: AdminUserCreationDto[] = [];
  const conflicts: ImportConflict[] = [];

  for (const user of users) {
    const key = usernameKey(user.username);
    if (RESERVED_USERNAMES.has(key)) {
      conflicts.push({ user, reason: "RESERVED_USERNAME" });
    } else if (seen.has(key)) {
      conflicts.push({ user, reason: "DUPLICATE_IN_BATCH" });
    } else if (existingByKey.has(key)) {
      seen.add(key);
      conflicts.push({
        user,
        reason: "USERNAME_EXISTS",
        existing: existingByKey.get(key),
      });
    } else {
      seen.add(key);
      clean.push(user);
    }
  }
  return { clean, conflicts };
}

export type ResolutionAction = "skip" | "update" | "replace";
export type FieldChoice = "existing" | "import";
export type TagChoice = FieldChoice | "merge";

export interface FieldChoices {
  firstname: FieldChoice;
  lastname: FieldChoice;
  email: FieldChoice;
  emailVerified: FieldChoice;
  roles: FieldChoice;
  tags: TagChoice;
  password: "keep" | "import";
}

export interface Decision {
  action: ResolutionAction;
  fields: FieldChoices;
}

// Safe defaults: take names and (non-empty) email from the import, keep the
// existing roles and verification state, merge tags, never touch the password.
export function defaultDecision(conflict: ImportConflict): Decision {
  return {
    action: "update",
    fields: {
      firstname: "import",
      lastname: "import",
      email: conflict.user.email?.trim() ? "import" : "existing",
      emailVerified: "existing",
      roles: "existing",
      tags: "merge",
      password: "keep",
    },
  };
}

export interface ResolvedValues {
  firstname: string;
  lastname: string;
  email?: string;
  emailVerified: boolean;
  roles: string[];
  tags: string[];
  // Only set when the password of the import is applied.
  password?: string;
}

// The values the existing user ends up with when the given choices are applied.
export function resolvedValues(
  existing: UserDto,
  imported: AdminUserCreationDto,
  fields: FieldChoices,
): ResolvedValues {
  const pick = <T>(choice: FieldChoice, fromExisting: T, fromImport: T): T =>
    choice === "import" ? fromImport : fromExisting;
  const existingTags = existing.tags ?? [];
  const importedTags = imported.tags ?? [];
  const tags =
    fields.tags === "merge"
      ? Array.from(new Set([...existingTags, ...importedTags]))
      : fields.tags === "import"
        ? importedTags
        : existingTags;

  return {
    firstname: pick(
      fields.firstname,
      existing.firstname ?? "",
      imported.firstname,
    ),
    lastname: pick(fields.lastname, existing.lastname ?? "", imported.lastname),
    email: pick(
      fields.email,
      existing.email,
      imported.email?.trim() || undefined,
    ),
    emailVerified: pick(
      fields.emailVerified,
      existing.emailVerified ?? false,
      imported.emailVerified,
    ),
    roles: pick(fields.roles, existing.roles ?? [], imported.roles),
    tags,
    ...(fields.password === "import" ? { password: imported.password } : {}),
  };
}

// Builds the request for one conflict; skipped conflicts produce none.
export function buildResolution(
  conflict: ImportConflict,
  decision: Decision,
): UserImportResolutionDto | null {
  const existingId = conflict.existing?.id;
  if (!existingId || decision.action === "skip") return null;

  if (decision.action === "replace") {
    return {
      action: "REPLACE",
      existingUserId: existingId,
      replacement: conflict.user,
    };
  }

  const values = resolvedValues(
    conflict.existing as UserDto,
    conflict.user,
    decision.fields,
  );
  return {
    action: "UPDATE",
    existingUserId: existingId,
    update: { ...values, sendEmailVerification: false },
  };
}
