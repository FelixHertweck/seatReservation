"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getApiNotificationsQueryKey,
  getApiNotificationsUnreadCountQueryKey,
} from "@/api/@tanstack/react-query.gen";
import {
  patchApiNotificationsByIdRead,
  patchApiNotificationsReadAll,
} from "@/api/sdk.gen";

/**
 * Mark-as-read/mark-all-as-read mutations, shared between the notification bell and the
 * notifications page so both invalidate the same query keys and can't drift out of sync.
 */
export function useNotificationMutations() {
  const queryClient = useQueryClient();

  const invalidateNotifications = () => {
    void queryClient.invalidateQueries({
      queryKey: getApiNotificationsQueryKey(),
    });
    void queryClient.invalidateQueries({
      queryKey: getApiNotificationsUnreadCountQueryKey(),
    });
  };

  const markReadMutation = useMutation({
    mutationFn: (id: string) => patchApiNotificationsByIdRead({ path: { id } }),
    onSuccess: invalidateNotifications,
  });

  const markAllReadMutation = useMutation({
    mutationFn: () => patchApiNotificationsReadAll(),
    onSuccess: invalidateNotifications,
  });

  return { markReadMutation, markAllReadMutation };
}
