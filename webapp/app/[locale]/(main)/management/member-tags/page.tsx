"use client";

import { useMemo, useState } from "react";

import { useT } from "@/lib/i18n/hooks";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/custom-ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useManagementMemberTags } from "@/hooks/use-management-member-tags";

export default function ManagementMemberTagsPage() {
  const t = useT();
  const { users, isLoading, assignTag, isAssigning } =
    useManagementMemberTags();

  const [rawUsernames, setRawUsernames] = useState("");
  const [tag, setTag] = useState("");

  const usernames = useMemo(
    () =>
      Array.from(
        new Set(
          rawUsernames
            .split(/[\s,;]+/)
            .map((name) => name.trim())
            .filter(Boolean),
        ),
      ),
    [rawUsernames],
  );

  const existingTags = useMemo(
    () => Array.from(new Set(users.flatMap((u) => u.tags ?? []))).sort(),
    [users],
  );

  const trimmedTag = tag.trim();
  const usersByName = useMemo(
    () => new Map(users.map((u) => [u.username ?? "", u])),
    [users],
  );
  const preview = useMemo(() => {
    const matched: string[] = [];
    const alreadyTagged: string[] = [];
    const unknown: string[] = [];
    for (const name of usernames) {
      const user = usersByName.get(name);
      if (!user) unknown.push(name);
      else if (trimmedTag && user.tags?.includes(trimmedTag))
        alreadyTagged.push(name);
      else matched.push(name);
    }
    return { matched, alreadyTagged, unknown };
  }, [usernames, usersByName, trimmedTag]);

  const canSubmit =
    !!trimmedTag && preview.matched.length > 0 && !isAssigning && !isLoading;

  const handleSubmit = async () => {
    await assignTag(usernames, trimmedTag);
    setRawUsernames("");
  };

  return (
    <div className="container mx-auto space-y-6 p-4 sm:p-6">
      <PageHeader
        title={t("management.memberTags.title")}
        description={t("management.memberTags.description")}
      />

      <section className="max-w-2xl space-y-3">
        <h2 className="text-base font-semibold">
          {t("management.memberTags.formTitle")}
        </h2>
        <Card>
          <CardContent className="space-y-6 pt-6">
            <div className="space-y-3">
              <Label htmlFor="member-tag" className="block">
                {t("management.memberTags.tagLabel")}
              </Label>
              <Input
                id="member-tag"
                value={tag}
                onChange={(e) => setTag(e.target.value)}
                placeholder={t("management.memberTags.tagPlaceholder")}
                maxLength={100}
              />
              {existingTags.length > 0 && (
                <div className="space-y-2 pt-1">
                  <p className="text-xs text-muted-foreground">
                    {t("management.memberTags.existingTagsLabel")}
                  </p>
                  <div className="flex max-h-24 flex-wrap gap-1.5 overflow-y-auto">
                    {existingTags.map((existing) => (
                      <button
                        key={existing}
                        type="button"
                        onClick={() => setTag(existing)}
                        className="rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                      >
                        <Badge
                          variant={
                            trimmedTag === existing ? "default" : "secondary"
                          }
                          className="cursor-pointer"
                        >
                          {existing}
                        </Badge>
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <div className="space-y-3">
              <Label htmlFor="member-usernames" className="block">
                {t("management.memberTags.usernamesLabel")}
              </Label>
              <Textarea
                id="member-usernames"
                value={rawUsernames}
                onChange={(e) => setRawUsernames(e.target.value)}
                placeholder={t("management.memberTags.usernamesPlaceholder")}
                rows={8}
              />
            </div>

            {usernames.length > 0 && (
              <div className="space-y-1 text-sm">
                <p>
                  {t("management.memberTags.previewMatched", {
                    count: preview.matched.length,
                  })}
                </p>
                {preview.alreadyTagged.length > 0 && (
                  <p className="text-muted-foreground">
                    {t("management.memberTags.previewAlreadyTagged", {
                      count: preview.alreadyTagged.length,
                    })}
                  </p>
                )}
                {preview.unknown.length > 0 && (
                  <p className="text-destructive">
                    {t("management.memberTags.previewUnknown", {
                      count: preview.unknown.length,
                      names: preview.unknown.join(", "),
                    })}
                  </p>
                )}
              </div>
            )}

            <Button onClick={handleSubmit} disabled={!canSubmit}>
              {t("management.memberTags.submit", {
                count: preview.matched.length,
              })}
            </Button>
          </CardContent>
        </Card>
      </section>
    </div>
  );
}
