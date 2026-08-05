"use client";

import { DeleteIcon } from "@/components/ui/delete";
import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { ArrowUpIcon } from "@/components/ui/arrow-up";
import { PlusIcon } from "@/components/ui/plus";

import { useT } from "@/lib/i18n/hooks";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/custom-ui/button";
import EventSelector from "@/components/common/supervisor/event-selector";
import { OverflowActionBar } from "@/components/common/overflow-action-bar";
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
  const [isDeletingSelected, setIsDeletingSelected] = useState(false);
  const [deletingAllowanceId, setDeletingAllowanceId] = useState<string | null>(
    null,
  );
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
      setIsDeletingSelected(true);
      try {
        await deleteAllowances([...selectedIds]);
        setSelectedIds(new Set());
      } finally {
        setIsDeletingSelected(false);
      }
    }
  };

  const handleDeleteAllowance = async (allowance: EventUserAllowancesDto) => {
    if (
      allowance.id &&
      confirm(t("management.allowances.deleteConfirm", { count: 1 }))
    ) {
      setDeletingAllowanceId(allowance.id);
      try {
        await deleteAllowances([allowance.id]);
        setSelectedIds((prev) => {
          const next = new Set(prev);
          next.delete(allowance.id!);
          return next;
        });
      } finally {
        setDeletingAllowanceId(null);
      }
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

  const modalAllowance = useMemo(
    () => (eventId ? ({ eventId } as EventUserAllowancesDto) : null),
    [eventId],
  );

  return (
    <div className="container mx-auto p-4 sm:p-6">
      <PageHeader
        title={t("management.allowances.title")}
        description={t("management.allowances.description")}
        actions={
          eventId ? (
            <>
              <OverflowActionBar
                actions={
                  selectedIds.size > 0
                    ? [
                        {
                          key: "delete",
                          label: t("management.allowances.deleteSelected", {
                            count: selectedIds.size,
                          }),
                          icon: <DeleteIcon size={16} />,
                          onClick: handleDeleteSelected,
                          variant: "destructive" as const,
                          isLoading: isDeletingSelected,
                        },
                      ]
                    : []
                }
              />
              <Button
                onClick={() => setIsModalOpen(true)}
                aria-label={t("management.allowances.grantButton")}
              >
                <PlusIcon size={16} />
                <span className="hidden sm:inline">
                  {t("management.allowances.grantButton")}
                </span>
              </Button>
            </>
          ) : undefined
        }
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
            <ArrowUpIcon size={20} />
            {t("management.allowances.selectEventPrompt")}
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          <Card>
            <CardContent className="flex flex-wrap items-center gap-4 py-4 text-sm text-muted-foreground">
              <div>
                {t("management.allowances.totalGranted", {
                  count: totalGranted,
                })}
              </div>
            </CardContent>
          </Card>

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
                deletingId={deletingAllowanceId}
              />
            </CardContent>
          </Card>
        </div>
      )}

      {isModalOpen && (
        <AllowanceFormModal
          allowance={modalAllowance}
          users={users}
          events={events}
          isCreating
          hideEventSelector
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
