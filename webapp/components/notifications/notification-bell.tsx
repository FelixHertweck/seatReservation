"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  Bell,
  BellRing,
  BellOff,
  CheckCircle2,
  CheckCheck,
  ChevronRight,
  Loader2,
} from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/use-auth";
import { useT } from "@/lib/i18n/hooks";
import { useWebPush } from "@/hooks/use-web-push";
import { useNotificationMutations } from "@/hooks/use-notification-mutations";
import {
  formatRelativeTime,
  getCategoryIcon,
  getCategoryColorClasses,
} from "@/lib/notifications";
import { toast } from "sonner";
import {
  getApiNotificationsUnreadCountOptions,
  getApiNotificationsOptions,
} from "@/api/@tanstack/react-query.gen";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { UserNotificationDto } from "@/api/types.gen";

// Push delivery (added alongside this component) already drives real-time updates; this interval
// is just a low-frequency fallback in case a push was missed (browser closed, permission denied).
const FALLBACK_POLL_INTERVAL_MS = 120_000;

export function NotificationBell() {
  const t = useT();
  const { user } = useAuth();
  const router = useRouter();
  const {
    isSupported: isPushSupported,
    permission: pushPermission,
    isSubscribed: isPushSubscribed,
    isLoading: isPushLoading,
    subscribe: subscribeToPush,
    unsubscribe: unsubscribeFromPush,
  } = useWebPush();

  const [open, setOpen] = useState(false);
  const [isPushToggling, setIsPushToggling] = useState(false);
  const isPushPending = isPushLoading || isPushToggling;

  const { data: unreadData } = useQuery({
    ...getApiNotificationsUnreadCountOptions(),
    enabled: !!user,
    refetchInterval: FALLBACK_POLL_INTERVAL_MS,
    refetchIntervalInBackground: false,
  });

  const { data: notificationsData, refetch: refetchNotifications } = useQuery({
    ...getApiNotificationsOptions({ query: { size: 5 } }),
    enabled: !!user,
    refetchInterval: FALLBACK_POLL_INTERVAL_MS,
    refetchIntervalInBackground: false,
  });

  const { markReadMutation, markAllReadMutation } = useNotificationMutations();

  if (!user) {
    return null;
  }

  const unreadCount = unreadData?.unreadCount
    ? Number(unreadData.unreadCount)
    : 0;
  const notifications: UserNotificationDto[] = notificationsData?.items ?? [];

  const handleNotificationClick = (item: UserNotificationDto) => {
    if (!item.isRead && item.id) {
      markReadMutation.mutate(item.id);
    }
    if (item.actionUrl) {
      router.push(item.actionUrl);
    }
  };

  const handleTogglePush = async () => {
    setIsPushToggling(true);
    try {
      if (isPushSubscribed) {
        await unsubscribeFromPush();
        toast.info(t("notifications.pushBanner.disabledToast"));
      } else {
        await subscribeToPush();
        toast.success(t("notifications.pushBanner.enabledBadge"));
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      toast.error(msg);
    } finally {
      setIsPushToggling(false);
    }
  };

  const getPushBannerIcon = () => {
    if (isPushSubscribed) {
      return (
        <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600 dark:text-emerald-400" />
      );
    }
    if (pushPermission === "denied") {
      return <BellOff className="h-4 w-4 shrink-0 text-destructive" />;
    }
    return <BellRing className="h-4 w-4 shrink-0 text-muted-foreground" />;
  };

  const getPushBannerButtonLabel = () => {
    if (isPushPending) return t("notifications.pushBanner.loadingButton");
    if (isPushSubscribed) return t("notifications.pushBanner.disableButton");
    return t("notifications.pushBanner.enableButton");
  };

  return (
    <DropdownMenu
      open={open}
      onOpenChange={(nextOpen) => {
        setOpen(nextOpen);
        if (nextOpen) {
          void refetchNotifications();
        }
      }}
    >
      <DropdownMenuTrigger asChild>
        <Button
          variant="outline"
          size="icon"
          className="relative h-10 w-10 shrink-0"
          aria-label={t("notifications.title")}
        >
          <Bell className="h-4 w-4" />
          {unreadCount > 0 && (
            <span className="absolute -top-1 -right-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-destructive px-1 text-[10px] font-bold text-destructive-foreground ring-2 ring-background">
              {unreadCount > 99 ? "99+" : unreadCount}
            </span>
          )}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        align="end"
        collisionPadding={12}
        className="w-[calc(100vw-1.5rem)] max-w-96 rounded-xl border border-border/60 bg-popover/95 p-0 shadow-2xl backdrop-blur-md animate-in fade-in-80 zoom-in-95 duration-200"
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b border-border/50 px-4 py-3 bg-muted/30">
          <div className="flex items-center gap-2">
            <h3 className="font-semibold text-sm text-foreground">
              {t("notifications.title")}
            </h3>
            {unreadCount > 0 && (
              <Badge
                variant="secondary"
                className="bg-primary/10 text-primary border-primary/20 text-xs"
              >
                {t("notifications.unreadCountBadge", { count: unreadCount })}
              </Badge>
            )}
          </div>
          {unreadCount > 0 && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => markAllReadMutation.mutate()}
              disabled={markAllReadMutation.isPending}
              className="h-7 text-xs text-muted-foreground hover:text-foreground hover:bg-accent/50 gap-1 px-2"
            >
              <CheckCheck className="h-3.5 w-3.5" />
              {t("notifications.markAllAsRead")}
            </Button>
          )}
        </div>

        {/* List of items */}
        <div className="max-h-[360px] overflow-y-auto divide-y divide-border/30">
          {notifications.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-10 px-4 text-center">
              <p className="text-sm font-medium text-muted-foreground">
                {t("notifications.emptyTitle")}
              </p>
              <p className="text-xs text-muted-foreground/70 mt-1">
                {t("notifications.emptyDescription")}
              </p>
            </div>
          ) : (
            notifications.map((item) => {
              const CategoryIcon = getCategoryIcon(item.category);
              return (
                <DropdownMenuItem
                  key={item.id}
                  onClick={() => handleNotificationClick(item)}
                  className={`flex items-start gap-3 p-3.5 cursor-pointer transition-colors duration-150 ${
                    !item.isRead
                      ? "bg-accent/40 hover:bg-accent/70"
                      : "hover:bg-accent/30"
                  }`}
                >
                  <div
                    className={`mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg ${getCategoryColorClasses(item.category)}`}
                  >
                    <CategoryIcon className="h-4 w-4" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between gap-1">
                      <p
                        className={`text-xs font-semibold truncate ${
                          !item.isRead
                            ? "text-foreground"
                            : "text-muted-foreground"
                        }`}
                      >
                        {item.title}
                      </p>
                      <span className="text-[10px] text-muted-foreground shrink-0">
                        {formatRelativeTime(item.createdAt, t)}
                      </span>
                    </div>
                    <p className="text-xs text-muted-foreground line-clamp-2 mt-0.5 leading-snug">
                      {item.message}
                    </p>
                    {item.actionLabel && (
                      <span className="inline-flex items-center gap-1 text-[11px] font-medium text-primary mt-1 hover:underline">
                        {item.actionLabel}
                        <ChevronRight className="h-3 w-3" />
                      </span>
                    )}
                  </div>
                  {!item.isRead && (
                    <span className="h-2 w-2 rounded-full bg-primary shrink-0 mt-1.5" />
                  )}
                </DropdownMenuItem>
              );
            })
          )}
        </div>

        {/* Push notifications toggle */}
        {isPushSupported && (
          <div className="flex items-center justify-between gap-3 border-t border-border/50 px-4 py-2.5 bg-muted/20">
            <div className="flex items-center gap-2 min-w-0">
              {getPushBannerIcon()}
              <span
                className="truncate text-xs text-muted-foreground"
                title={
                  pushPermission === "denied"
                    ? t("notifications.pushBanner.disabledNotice")
                    : t("notifications.pushBanner.title")
                }
              >
                {t("notifications.pushBanner.label")}
              </span>
            </div>
            {pushPermission === "denied" ? (
              <span className="shrink-0 text-[11px] text-muted-foreground">
                {t("notifications.pushBanner.blockedBadge")}
              </span>
            ) : (
              <Button
                size="sm"
                variant={isPushSubscribed ? "outline" : "default"}
                onClick={handleTogglePush}
                disabled={isPushPending}
                className="h-7 shrink-0 text-xs gap-1.5"
              >
                {isPushPending && <Loader2 className="h-3 w-3 animate-spin" />}
                <span>{getPushBannerButtonLabel()}</span>
              </Button>
            )}
          </div>
        )}

        {/* Footer */}
        <div className="border-t border-border/50 p-2 text-center bg-muted/20">
          <Button
            asChild
            variant="ghost"
            size="sm"
            className="w-full text-xs font-medium text-primary hover:bg-accent/50"
          >
            <Link href="/notifications">{t("notifications.viewAll")}</Link>
          </Button>
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
