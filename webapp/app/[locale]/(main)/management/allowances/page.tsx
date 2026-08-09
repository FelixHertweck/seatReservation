"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { ArrowUp, Plus, Trash2 } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/custom-ui/button";
import EventSelector from "@/components/common/supervisor/event-selector";
import { OverflowActionBar } from "@/components/common/overflow-action-bar";
import { SearchAndFilter } from "@/components/common/search-and-filter";
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
    capacity,
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
  const [searchQuery, setSearchQuery] = useState("");
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

  const filteredAllowances = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) return displayAllowances;
    return displayAllowances.filter((a) => {
      const username =
        users.find((u) => u.id === a.userId)?.username?.toLowerCase() ?? "";
      return username.includes(query);
    });
  }, [displayAllowances, users, searchQuery]);

  const toggleAll = () => {
    if (selectedIds.size === filteredAllowances.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(filteredAllowances.map((a) => a.id ?? "")));
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
                          icon: <Trash2 className="h-4 w-4" />,
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
                <Plus className="h-4 w-4" />
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
            <ArrowUp className="h-5 w-5" />
            {t("management.allowances.selectEventPrompt")}
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          <Card>
            <CardContent className="flex flex-wrap items-center justify-between gap-4 py-4 text-sm text-muted-foreground">
              <div>
                {t("management.allowances.totalGranted", {
                  count: totalGranted,
                })}
                {" · "}
                {t("management.allowances.locationCapacity", {
                  count: capacity,
                })}
              </div>
              <SearchAndFilter
                onSearch={setSearchQuery}
                onFilter={() => {}}
                filterOptions={[]}
                className="w-full sm:w-64"
              />
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-0">
              <AllowancesTable
                allowances={filteredAllowances}
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
