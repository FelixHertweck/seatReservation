"use client";

import { useLayoutEffect, useRef, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  CheckCheck,
  Check,
  Trash2,
  ExternalLink,
  Sparkles,
  Bell,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { useT } from "@/lib/i18n/hooks";
import { PageHeader } from "@/components/page-header";
import { cn } from "@/lib/utils";
import {
  getApiNotificationsOptions,
  getApiNotificationsQueryKey,
  getApiNotificationsUnreadCountQueryKey,
} from "@/api/@tanstack/react-query.gen";
import { deleteApiNotificationsById } from "@/api/sdk.gen";
import { UserNotificationDto } from "@/api/types.gen";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { toast } from "sonner";
import { useNotificationMutations } from "@/hooks/use-notification-mutations";
import {
  formatRelativeTime,
  getCategoryIcon,
  getCategoryColorClasses,
} from "@/lib/notifications";

function getPriorityBadge(
  priority: string | undefined,
  t: ReturnType<typeof useT>,
) {
  switch (priority) {
    case "URGENT":
      return (
        <Badge
          variant="destructive"
          className="text-[10px] uppercase font-bold px-1.5 py-0"
        >
          {t("notifications.priority.URGENT")}
        </Badge>
      );
    case "HIGH":
      return (
        <Badge className="bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20 text-[10px] uppercase font-bold px-1.5 py-0">
          {t("notifications.priority.HIGH")}
        </Badge>
      );
    default:
      return null;
  }
}

interface Indicator {
  left: number;
  width: number;
}

export default function NotificationsPage() {
  const t = useT();
  const router = useRouter();
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState<string>("all");

  const tabItems = [
    { value: "all", label: t("notifications.tabs.all"), icon: Bell },
    { value: "unread", label: t("notifications.tabs.unread"), icon: Sparkles },
  ];

  const activeIndex = tabItems.findIndex((item) => item.value === activeTab);
  const tabRefs = useRef<(HTMLButtonElement | null)[]>([]);
  const [indicator, setIndicator] = useState<Indicator | null>(null);

  useLayoutEffect(() => {
    const updateIndicator = () => {
      const activeBtn = tabRefs.current[activeIndex];
      if (activeBtn) {
        setIndicator({
          left: activeBtn.offsetLeft,
          width: activeBtn.offsetWidth,
        });
      } else {
        setIndicator(null);
      }
    };

    updateIndicator();
    window.addEventListener("resize", updateIndicator);
    return () => window.removeEventListener("resize", updateIndicator);
  }, [activeIndex]);

  const unreadOnlyFilter = activeTab === "unread" ? true : undefined;

  const { data, isLoading } = useQuery({
    ...getApiNotificationsOptions({
      query: {
        unreadOnly: unreadOnlyFilter,
        size: 50,
      },
    }),
  });

  const { markReadMutation, markAllReadMutation } = useNotificationMutations();

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteApiNotificationsById({ path: { id } }),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: getApiNotificationsQueryKey(),
      });
      void queryClient.invalidateQueries({
        queryKey: getApiNotificationsUnreadCountQueryKey(),
      });
      toast.success(t("notifications.delete"));
    },
  });

  const notifications: UserNotificationDto[] = data?.items ?? [];
  const hasUnread = notifications.some((item) => !item.isRead);

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      <PageHeader
        title={t("notifications.title")}
        description={t("notifications.description")}
        actions={
          hasUnread && (
            <Button
              variant="outline"
              size="sm"
              onClick={() =>
                markAllReadMutation.mutate(undefined, {
                  onSuccess: () =>
                    toast.success(t("notifications.markAllAsRead")),
                })
              }
              disabled={markAllReadMutation.isPending}
              className="gap-2 text-xs hover:bg-accent/50"
            >
              <CheckCheck className="h-4 w-4" />
              {t("notifications.markAllAsRead")}
            </Button>
          )
        }
      />

      <div className="relative flex max-w-full items-center gap-1 border-b border-border/60 overflow-x-auto text-muted-foreground">
        {indicator && (
          <div
            aria-hidden="true"
            className="absolute bottom-0 h-0.5 rounded-full bg-primary transition-[left,width] duration-200 ease-out"
            style={{ left: indicator.left, width: indicator.width }}
          />
        )}
        {tabItems.map((item, index) => {
          const isActive = item.value === activeTab;
          const Icon = item.icon;
          return (
            <button
              key={item.value}
              ref={(el) => {
                tabRefs.current[index] = el;
              }}
              onClick={() => setActiveTab(item.value)}
              className={cn(
                "relative inline-flex items-center gap-2 whitespace-nowrap border-b-2 border-transparent px-3 py-2.5 text-sm font-medium transition-colors",
                isActive
                  ? "text-foreground font-semibold"
                  : "hover:border-border hover:text-foreground",
              )}
            >
              <Icon className="h-4 w-4 shrink-0" />
              <span>{item.label}</span>
            </button>
          );
        })}
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <Card key={i} className="border border-border/40">
              <CardContent className="p-4 flex gap-4 items-center">
                <Skeleton className="h-10 w-10 rounded-xl" />
                <div className="space-y-2 flex-1">
                  <Skeleton className="h-4 w-1/3" />
                  <Skeleton className="h-3 w-2/3" />
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      ) : notifications.length === 0 ? (
        <Card className="border border-dashed border-border/60 bg-muted/20 py-12 text-center">
          <CardContent className="flex flex-col items-center justify-center gap-3">
            <div className="h-12 w-12 rounded-2xl bg-muted/60 flex items-center justify-center text-muted-foreground/60">
              <Sparkles className="h-6 w-6" />
            </div>
            <div>
              <h3 className="font-semibold text-base text-foreground">
                {t("notifications.emptyTitle")}
              </h3>
              <p className="text-xs text-muted-foreground mt-1 max-w-sm">
                {t("notifications.emptyDescription")}
              </p>
            </div>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          {notifications.map((item) => {
            const CategoryIcon = getCategoryIcon(item.category);
            return (
              <Card
                key={item.id}
                className={`group border transition-all duration-200 hover:shadow-md ${
                  !item.isRead
                    ? "border-primary/30 bg-primary/[0.02] dark:bg-primary/[0.04]"
                    : "border-border/50 bg-card hover:border-border"
                }`}
              >
                <CardContent className="p-4 sm:p-5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                  <div className="flex items-start gap-4 min-w-0 flex-1">
                    <div
                      className={`mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl transition-transform group-hover:scale-105 ${getCategoryColorClasses(item.category)}`}
                    >
                      <CategoryIcon className="h-5 w-5" />
                    </div>

                    <div className="space-y-1 min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <h4
                          className={`text-sm font-semibold truncate ${
                            !item.isRead
                              ? "text-foreground"
                              : "text-muted-foreground"
                          }`}
                        >
                          {item.title}
                        </h4>
                        {!item.isRead && (
                          <Badge className="bg-primary text-primary-foreground text-[10px] px-1.5 py-0 h-4">
                            {t("notifications.newBadge")}
                          </Badge>
                        )}
                        {getPriorityBadge(item.priority, t)}
                      </div>

                      <p className="text-xs text-muted-foreground leading-relaxed">
                        {item.message}
                      </p>

                      <p className="text-[11px] text-muted-foreground/70 pt-0.5">
                        {formatRelativeTime(item.createdAt, t)}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 shrink-0 w-full sm:w-auto justify-end border-t sm:border-t-0 pt-3 sm:pt-0 border-border/40">
                    {item.actionLabel && (
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() => {
                          if (!item.isRead && item.id)
                            markReadMutation.mutate(item.id);
                          if (item.actionUrl) router.push(item.actionUrl);
                        }}
                        className="gap-1.5 text-xs font-medium hover:scale-[1.02] transition-transform"
                      >
                        {item.actionLabel}
                        <ExternalLink className="h-3.5 w-3.5" />
                      </Button>
                    )}

                    {!item.isRead && (
                      <Button
                        size="icon"
                        variant="ghost"
                        onClick={() =>
                          item.id && markReadMutation.mutate(item.id)
                        }
                        disabled={markReadMutation.isPending}
                        title={t("notifications.markAsRead")}
                        className="h-8 w-8 text-muted-foreground hover:text-foreground"
                      >
                        <Check className="h-4 w-4" />
                      </Button>
                    )}

                    <Button
                      size="icon"
                      variant="ghost"
                      onClick={() => item.id && deleteMutation.mutate(item.id)}
                      disabled={deleteMutation.isPending}
                      title={t("notifications.delete")}
                      className="h-8 w-8 text-muted-foreground hover:text-destructive hover:bg-destructive/10"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
