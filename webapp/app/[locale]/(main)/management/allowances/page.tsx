"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { ArrowUp, Plus, Trash2, Users } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import EventSelector from "@/components/common/supervisor/event-selector";
import { AllowanceFormModal } from "@/components/management/allowance-form-modal";
import { AllowancesTable } from "@/components/management/allowances/allowances-table";
import { useManagementAllowances } from "@/hooks/use-management-allowances";
import type { EventUserAllowancesDto } from "@/api";

export default function ManagementAllowancesPage() {
  const t = useT();
  const router = useRouter();
  const searchParams = useSearchParams();
  const eventId = searchParams.get("eventId");

  const {
    events,
    users,
    allowances,
    isLoading,
    isAllowancesLoading,
    grantAllowances,
    updateAllowance,
    deleteAllowances,
  } = useManagementAllowances(eventId);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [pendingCounts, setPendingCounts] = useState<Record<string, number>>(
    {},
  );
  const debounceTimers = useRef<Map<string, ReturnType<typeof setTimeout>>>(
    new Map(),
  );

  useEffect(() => {
    const timers = debounceTimers.current;
    return () => {
      timers.forEach((timer) => clearTimeout(timer));
    };
  }, []);

  const usersWithoutAllowance = useMemo(() => {
    const covered = new Set(allowances.map((a) => a.userId));
    return users.filter((u) => u.id && !covered.has(u.id));
  }, [allowances, users]);

  const handleEventSelect = (id: string) => {
    router.push(`/management/allowances?eventId=${id}`);
    setSelectedIds(new Set());
  };

  const handleDeleteSelected = async () => {
    if (selectedIds.size === 0) return;
    if (
      confirm(
        t("management.allowances.deleteConfirm", { count: selectedIds.size }),
      )
    ) {
      await deleteAllowances([...selectedIds]);
      setSelectedIds(new Set());
    }
  };

  const handleDeleteAllowance = async (allowance: EventUserAllowancesDto) => {
    if (
      allowance.id &&
      confirm(t("management.allowances.deleteConfirm", { count: 1 }))
    ) {
      await deleteAllowances([allowance.id]);
      setSelectedIds((prev) => {
        const next = new Set(prev);
        next.delete(allowance.id!);
        return next;
      });
    }
  };

  const handleChangeCount = (
    allowance: EventUserAllowancesDto,
    delta: number,
  ) => {
    const { id, eventId: allowanceEventId, userId } = allowance;
    if (!id || !allowanceEventId || !userId) return;

    setPendingCounts((prev) => {
      const current = prev[id] ?? allowance.reservationsAllowedCount ?? 0;
      const next = Math.max(0, current + delta);

      const existingTimer = debounceTimers.current.get(id);
      if (existingTimer) clearTimeout(existingTimer);

      const timer = setTimeout(() => {
        debounceTimers.current.delete(id);
        updateAllowance({
          id,
          eventId: allowanceEventId,
          userId,
          reservationsAllowedCount: next,
        });
        setPendingCounts((p) => {
          const rest = { ...p };
          delete rest[id];
          return rest;
        });
      }, 1000);
      debounceTimers.current.set(id, timer);

      return { ...prev, [id]: next };
    });
  };

  const displayAllowances = useMemo(
    () =>
      allowances.map((a) =>
        a.id && pendingCounts[a.id] !== undefined
          ? { ...a, reservationsAllowedCount: pendingCounts[a.id] }
          : a,
      ),
    [allowances, pendingCounts],
  );

  const toggleAll = () => {
    if (selectedIds.size === allowances.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(allowances.map((a) => a.id ?? "")));
    }
  };

  const totalGranted = displayAllowances.reduce(
    (sum, a) => sum + (a.reservationsAllowedCount ?? 0),
    0,
  );

  return (
    <div className="container mx-auto p-4 sm:p-6">
      <PageHeader
        title={t("management.allowances.title")}
        description={t("management.allowances.description")}
        search={
          <EventSelector
            events={events}
            isLoadingEvents={isLoading}
            selectedEventId={eventId}
            onEventSelect={handleEventSelect}
          />
        }
      />

      {!eventId ? (
        <Card>
          <CardContent className="flex flex-col items-center gap-2 py-12 text-center text-muted-foreground">
            <ArrowUp className="h-5 w-5" />
            {t("management.allowances.selectEventPrompt")}
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          <Card>
            <CardContent className="flex flex-wrap items-center gap-4 py-4 text-sm text-muted-foreground">
              <div className="flex items-center gap-2">
                <Users className="h-4 w-4" />
                {t("management.allowances.coverage", {
                  withAllowance: allowances.length,
                  total: users.length,
                })}
              </div>
              <div>
                {t("management.allowances.totalGranted", {
                  count: totalGranted,
                })}
              </div>
            </CardContent>
          </Card>

          <div className="flex flex-wrap justify-end gap-2">
            {selectedIds.size > 0 && (
              <Button
                variant="destructive"
                size="sm"
                onClick={handleDeleteSelected}
              >
                <Trash2 className="h-4 w-4" />
                {selectedIds.size}
              </Button>
            )}
            <Button size="sm" onClick={() => setIsModalOpen(true)}>
              <Plus className="h-4 w-4" />
              {t("management.allowances.grantButton")}
            </Button>
          </div>

          <Card>
            <CardContent className="p-0">
              <AllowancesTable
                allowances={displayAllowances}
                users={users}
                isLoading={isAllowancesLoading}
                selectedIds={selectedIds}
                onSelectedIdsChange={setSelectedIds}
                onToggleAll={toggleAll}
                onChangeCount={handleChangeCount}
                onDeleteAllowance={handleDeleteAllowance}
              />
            </CardContent>
          </Card>
        </div>
      )}

      {isModalOpen && (
        <AllowanceFormModal
          allowance={eventId ? ({ eventId } as EventUserAllowancesDto) : null}
          users={users}
          events={events}
          isCreating
          hideEventSelector
          usersWithoutAllowance={usersWithoutAllowance}
          onSubmit={async (data) => {
            await grantAllowances(
              data as Parameters<typeof grantAllowances>[0],
            );
            setIsModalOpen(false);
          }}
          onClose={() => setIsModalOpen(false)}
        />
      )}
    </div>
  );
}
