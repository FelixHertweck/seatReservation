"use client";

import { useMemo, useState } from "react";
import {
  AlertTriangle,
  ArrowRight,
  Check,
  GitCompare,
  Search,
  X,
} from "lucide-react";

import { Button } from "@/components/custom-ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/custom-ui/dialog";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { useT } from "@/lib/i18n/hooks";
import type {
  UserImportResolutionDto,
  UserImportResolutionResultDto,
} from "@/api";
import {
  buildResolution,
  defaultDecision,
  resolvedValues,
  usernameKey,
  type Decision,
  type FieldChoices,
  type ImportConflict,
  type ResolutionAction,
} from "@/lib/import-conflicts";

interface UserConflictResolverModalProps {
  isOpen: boolean;
  // Only conflicts with an existing user can be resolved here.
  conflicts: ImportConflict[];
  currentUserId?: string;
  onResolve: (
    resolutions: UserImportResolutionDto[],
  ) => Promise<UserImportResolutionResultDto[]>;
  // Called with the usernames that were resolved successfully (or skipped).
  onClose: (resolvedUsernames: string[]) => void;
}

type ChangeKey = "firstname" | "lastname" | "email" | "roles" | "tags";

interface ChangeRow {
  key: ChangeKey;
  label: string;
  existing: string;
  imported: string;
  // true: the import value is applied.
  apply: boolean;
}

const ACTIONS: ResolutionAction[] = ["skip", "update", "replace"];

const list = (values: string[] | undefined) =>
  values && values.length > 0 ? values.join(", ") : "—";

const sameSet = (a: string[] = [], b: string[] = []) =>
  a.length === b.length && a.every((value) => b.includes(value));

export function UserConflictResolverModal({
  isOpen,
  conflicts,
  currentUserId,
  onResolve,
  onClose,
}: UserConflictResolverModalProps) {
  const t = useT();

  const [decisions, setDecisions] = useState<Record<string, Decision>>(() =>
    Object.fromEntries(
      conflicts.map((c) => [usernameKey(c.user.username), defaultDecision(c)]),
    ),
  );
  const [focusedKey, setFocusedKey] = useState(
    conflicts.length > 0 ? usernameKey(conflicts[0].user.username) : "",
  );
  const [checkedKeys, setCheckedKeys] = useState<Set<string>>(new Set());
  const [search, setSearch] = useState("");
  const [isApplying, setIsApplying] = useState(false);
  const [results, setResults] = useState<
    UserImportResolutionResultDto[] | null
  >(null);

  const keyOf = (conflict: ImportConflict) =>
    usernameKey(conflict.user.username);
  const canReplace = (conflict: ImportConflict) =>
    !!conflict.existing?.id && conflict.existing.id !== currentUserId;
  const decisionOf = (conflict: ImportConflict): Decision =>
    decisions[keyOf(conflict)] ?? defaultDecision(conflict);
  const actionLabel = (action: ResolutionAction) =>
    t(`userConflictResolver.action.${action}`);

  const visibleConflicts = useMemo(() => {
    const query = search.trim().toLowerCase();
    return conflicts.filter(
      (c) =>
        !query ||
        c.user.username.toLowerCase().includes(query) ||
        `${c.user.firstname} ${c.user.lastname}`.toLowerCase().includes(query),
    );
  }, [conflicts, search]);

  const focused = conflicts.find((c) => keyOf(c) === focusedKey);

  const counts = useMemo(() => {
    const result = { update: 0, replace: 0, skip: 0 };
    for (const c of conflicts) {
      result[decisions[usernameKey(c.user.username)]?.action ?? "update"] += 1;
    }
    return result;
  }, [conflicts, decisions]);

  const allVisibleChecked =
    visibleConflicts.length > 0 &&
    visibleConflicts.every((c) => checkedKeys.has(keyOf(c)));

  const toggleAllVisible = () =>
    setCheckedKeys((prev) => {
      const next = new Set(prev);
      for (const c of visibleConflicts) {
        if (allVisibleChecked) next.delete(keyOf(c));
        else next.add(keyOf(c));
      }
      return next;
    });

  const toggleChecked = (conflict: ImportConflict) =>
    setCheckedKeys((prev) => {
      const next = new Set(prev);
      if (next.has(keyOf(conflict))) next.delete(keyOf(conflict));
      else next.add(keyOf(conflict));
      return next;
    });

  const setAction = (targets: ImportConflict[], action: ResolutionAction) =>
    setDecisions((prev) => {
      const next = { ...prev };
      for (const c of targets) {
        if (action === "replace" && !canReplace(c)) continue;
        next[keyOf(c)] = { ...decisionOf(c), action };
      }
      return next;
    });

  const setField = <K extends keyof FieldChoices>(
    conflict: ImportConflict,
    field: K,
    value: FieldChoices[K],
  ) => {
    const current = decisionOf(conflict);
    setDecisions((prev) => ({
      ...prev,
      [keyOf(conflict)]: {
        ...current,
        fields: { ...current.fields, [field]: value },
      },
    }));
  };

  const changeRows = (conflict: ImportConflict): ChangeRow[] => {
    const existing = conflict.existing;
    const imported = conflict.user;
    if (!existing) return [];
    const fields = decisionOf(conflict).fields;
    const rows: ChangeRow[] = [];

    if ((existing.firstname ?? "") !== imported.firstname) {
      rows.push({
        key: "firstname",
        label: t("userConflictResolver.field.firstname"),
        existing: existing.firstname ?? "—",
        imported: imported.firstname,
        apply: fields.firstname === "import",
      });
    }
    if ((existing.lastname ?? "") !== imported.lastname) {
      rows.push({
        key: "lastname",
        label: t("userConflictResolver.field.lastname"),
        existing: existing.lastname ?? "—",
        imported: imported.lastname,
        apply: fields.lastname === "import",
      });
    }
    if ((existing.email ?? "") !== (imported.email?.trim() ?? "")) {
      rows.push({
        key: "email",
        label: t("userConflictResolver.field.email"),
        existing: existing.email ?? "—",
        imported: imported.email?.trim() || "—",
        apply: fields.email === "import",
      });
    }
    if (!sameSet(existing.roles, imported.roles)) {
      rows.push({
        key: "roles",
        label: t("userConflictResolver.field.roles"),
        existing: list(existing.roles),
        imported: list(imported.roles),
        apply: fields.roles === "import",
      });
    }
    // Tags are only ever added, so a row is only shown when the import has new ones.
    const newTags = (imported.tags ?? []).filter(
      (tag) => !(existing.tags ?? []).includes(tag),
    );
    if (newTags.length > 0) {
      rows.push({
        key: "tags",
        label: t("userConflictResolver.field.tags"),
        existing: list(existing.tags),
        imported: list(newTags),
        apply: fields.tags === "merge",
      });
    }
    return rows;
  };

  const toggleRow = (conflict: ImportConflict, row: ChangeRow) => {
    if (row.key === "tags") {
      setField(conflict, "tags", row.apply ? "existing" : "merge");
    } else {
      setField(conflict, row.key, row.apply ? "existing" : "import");
    }
  };

  const removesAdmin = (conflict: ImportConflict) =>
    decisionOf(conflict).action === "update" &&
    !!conflict.existing?.roles?.includes("ADMIN") &&
    !resolvedValues(
      conflict.existing,
      conflict.user,
      decisionOf(conflict).fields,
    ).roles.includes("ADMIN");

  const handleApply = async () => {
    if (
      counts.replace > 0 &&
      !confirm(
        t("userConflictResolver.confirmReplace", { count: counts.replace }),
      )
    ) {
      return;
    }
    const resolutions = conflicts
      .map((c) => buildResolution(c, decisionOf(c)))
      .filter((r): r is UserImportResolutionDto => r !== null);

    setIsApplying(true);
    try {
      setResults(resolutions.length > 0 ? await onResolve(resolutions) : []);
    } catch {
      // The error is already surfaced by the toast of the caller.
    } finally {
      setIsApplying(false);
    }
  };

  const handleClose = () => {
    if (!results) {
      onClose([]);
      return;
    }
    const succeededIds = new Set(
      results.filter((r) => r.success).map((r) => r.existingUserId),
    );
    onClose(
      conflicts
        .filter(
          (c) =>
            decisionOf(c).action === "skip" ||
            (c.existing?.id && succeededIds.has(c.existing.id)),
        )
        .map((c) => c.user.username),
    );
  };

  const canApply = !isApplying && conflicts.length > 0;

  const checkedConflicts = conflicts.filter((c) => checkedKeys.has(keyOf(c)));

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && handleClose()}>
      <DialogContent
        className="sm:max-w-5xl sm:max-h-[90vh] sm:overflow-y-auto"
        onInteractOutside={(e) => e.preventDefault()}
      >
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <GitCompare className="h-5 w-5" />
            {t("userConflictResolver.title", { count: conflicts.length })}
          </DialogTitle>
          <DialogDescription>
            {t("userConflictResolver.description")}
          </DialogDescription>
        </DialogHeader>

        {results ? (
          <div className="space-y-4">
            <p className="text-sm">
              {t("userConflictResolver.resultSummary", {
                succeeded: results.filter((r) => r.success).length,
                failed: results.filter((r) => !r.success).length,
              })}
            </p>
            <div className="max-h-80 space-y-1 overflow-y-auto">
              {results.map((r) => {
                const conflict = conflicts.find(
                  (c) => c.existing?.id === r.existingUserId,
                );
                return (
                  <div
                    key={r.existingUserId}
                    className="flex items-center gap-2 rounded-md border px-3 py-2 text-sm"
                  >
                    {r.success ? (
                      <Check className="h-4 w-4 text-green-600" />
                    ) : (
                      <X className="h-4 w-4 text-destructive" />
                    )}
                    <span className="font-medium">
                      {conflict?.user.username ??
                        r.username ??
                        r.existingUserId}
                    </span>
                    {!r.success && (
                      <span className="text-muted-foreground">{r.message}</span>
                    )}
                  </div>
                );
              })}
            </div>
            <div className="flex justify-end">
              <Button type="button" onClick={handleClose}>
                {t("userConflictResolver.done")}
              </Button>
            </div>
          </div>
        ) : (
          <div className="mt-2 space-y-8">
            <div className="grid grid-cols-1 gap-8 md:grid-cols-[340px_1fr]">
              {/* Left: selectable list with bulk actions */}
              <div className="space-y-4">
                <div className="relative">
                  <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
                  <Input
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    placeholder={t("userConflictResolver.search")}
                    className="pl-8"
                  />
                </div>

                <div className="flex items-center justify-between gap-2">
                  <label className="flex items-center gap-3 text-sm">
                    <Checkbox
                      checked={allVisibleChecked}
                      onCheckedChange={toggleAllVisible}
                    />
                    {t("userConflictResolver.selectAll")}
                  </label>
                  <span className="text-xs text-muted-foreground">
                    {t("userConflictResolver.selectedCount", {
                      count: checkedKeys.size,
                    })}
                  </span>
                </div>

                <div className="grid grid-cols-3 gap-3">
                  {ACTIONS.map((action) => (
                    <Button
                      key={action}
                      type="button"
                      variant="outline"
                      disabled={checkedConflicts.length === 0}
                      onClick={() => setAction(checkedConflicts, action)}
                      className="px-2"
                    >
                      {actionLabel(action)}
                    </Button>
                  ))}
                </div>

                <div className="max-h-[46vh] space-y-3 overflow-y-auto pr-2">
                  {visibleConflicts.map((c) => {
                    const key = keyOf(c);
                    const action = decisionOf(c).action;
                    return (
                      <div
                        key={key}
                        role="button"
                        tabIndex={0}
                        onClick={() => setFocusedKey(key)}
                        onKeyDown={(e) => {
                          // Ignore keys pressed on the nested checkbox.
                          if (e.target !== e.currentTarget) return;
                          if (e.key === "Enter" || e.key === " ") {
                            e.preventDefault();
                            setFocusedKey(key);
                          }
                        }}
                        className={cn(
                          "flex cursor-pointer items-center gap-4 rounded-md border px-4 py-3 text-sm hover:bg-muted",
                          key === focusedKey && "border-primary bg-muted",
                        )}
                      >
                        <span onClick={(e) => e.stopPropagation()}>
                          <Checkbox
                            checked={checkedKeys.has(key)}
                            onCheckedChange={() => toggleChecked(c)}
                            aria-label={c.user.username}
                          />
                        </span>
                        <span className="min-w-0 flex-1">
                          <span className="block truncate font-medium">
                            {c.user.username}
                          </span>
                          <span className="block truncate text-xs text-muted-foreground">
                            {c.user.firstname} {c.user.lastname}
                          </span>
                        </span>
                        <Badge
                          variant={
                            action === "replace"
                              ? "destructive"
                              : action === "skip"
                                ? "outline"
                                : "secondary"
                          }
                          className="shrink-0 text-[10px]"
                        >
                          {actionLabel(action)}
                        </Badge>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Right: what happens with the focused user */}
              {focused && (
                <div className="space-y-6">
                  <div className="space-y-1">
                    <p className="font-semibold">{focused.user.username}</p>
                    <p className="text-sm text-muted-foreground">
                      {focused.user.firstname} {focused.user.lastname}
                    </p>
                  </div>

                  <div className="grid grid-cols-3 gap-3">
                    {ACTIONS.map((action) => {
                      const active = decisionOf(focused).action === action;
                      const disabled =
                        action === "replace" && !canReplace(focused);
                      return (
                        <Button
                          key={action}
                          type="button"
                          variant={
                            active
                              ? action === "replace"
                                ? "destructive"
                                : "default"
                              : "outline"
                          }
                          disabled={disabled}
                          title={
                            disabled
                              ? t("userConflictResolver.replaceOwnAccount")
                              : undefined
                          }
                          onClick={() => setAction([focused], action)}
                        >
                          {actionLabel(action)}
                        </Button>
                      );
                    })}
                  </div>

                  {decisionOf(focused).action === "skip" && (
                    <p className="text-sm text-muted-foreground">
                      {t("userConflictResolver.skipInfo")}
                    </p>
                  )}

                  {decisionOf(focused).action === "replace" && (
                    <div className="flex gap-3 rounded-md border border-destructive/50 bg-destructive/10 p-4 text-sm">
                      <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-destructive" />
                      <span>{t("userConflictResolver.replaceWarning")}</span>
                    </div>
                  )}

                  {decisionOf(focused).action === "update" && (
                    <div className="space-y-5">
                      <p className="text-sm text-muted-foreground">
                        {t("userConflictResolver.whatToApply")}
                      </p>

                      {changeRows(focused).length === 0 ? (
                        <p className="text-sm">
                          {t("userConflictResolver.noDifferences")}
                        </p>
                      ) : (
                        <div className="divide-y rounded-md border">
                          {changeRows(focused).map((row) => (
                            <label
                              key={row.key}
                              className="flex cursor-pointer items-center gap-4 px-4 py-4 text-sm"
                            >
                              <Checkbox
                                checked={row.apply}
                                onCheckedChange={() => toggleRow(focused, row)}
                              />
                              <span className="w-24 shrink-0 font-medium">
                                {row.label}
                              </span>
                              <span
                                className={cn(
                                  "min-w-0 flex-1 break-words",
                                  row.apply && "text-muted-foreground",
                                )}
                              >
                                {row.existing}
                              </span>
                              <ArrowRight className="h-4 w-4 shrink-0 text-muted-foreground" />
                              <span
                                className={cn(
                                  "min-w-0 flex-1 break-words",
                                  !row.apply && "text-muted-foreground",
                                )}
                              >
                                {row.imported}
                              </span>
                            </label>
                          ))}
                        </div>
                      )}

                      <label className="flex cursor-pointer items-center gap-3 text-sm">
                        <Checkbox
                          checked={
                            decisionOf(focused).fields.password === "import"
                          }
                          onCheckedChange={(checked) =>
                            setField(
                              focused,
                              "password",
                              checked === true ? "import" : "keep",
                            )
                          }
                        />
                        {t("userConflictResolver.setPassword")}
                      </label>

                      {removesAdmin(focused) && (
                        <div className="flex gap-3 rounded-md border border-destructive/50 bg-destructive/10 p-4 text-sm">
                          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-destructive" />
                          <span>
                            {t("userConflictResolver.removesAdminWarning")}
                          </span>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )}
            </div>

            <div className="space-y-5 border-t pt-6">
              <p className="text-sm">
                {t("userConflictResolver.summary", {
                  updateCount: counts.update,
                  replaceCount: counts.replace,
                  skipCount: counts.skip,
                })}
              </p>
              <div className="flex justify-end gap-3">
                <Button type="button" variant="outline" onClick={handleClose}>
                  {t("common.cancel")}
                </Button>
                <Button
                  type="button"
                  onClick={handleApply}
                  isLoading={isApplying}
                  disabled={!canApply}
                >
                  {t("userConflictResolver.apply")}
                </Button>
              </div>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
