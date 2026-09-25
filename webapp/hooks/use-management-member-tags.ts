"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { useT } from "@/lib/i18n/hooks";
import {
  getApiUsersManagerOptions,
  getApiUsersManagerQueryKey,
  postApiUsersManagerTagsMutation,
} from "@/api/@tanstack/react-query.gen";
import type { ErrorWithResponse } from "@/components/init-query-client";

export function useManagementMemberTags() {
  const t = useT();
  const queryClient = useQueryClient();

  const { data: users, isLoading } = useQuery({
    ...getApiUsersManagerOptions(),
  });
  const assignMutation = useMutation({
    ...postApiUsersManagerTagsMutation(),
  });

  const assignTag = async (usernames: string[], tag: string) => {
    const request = assignMutation.mutateAsync({ body: { usernames, tag } });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("management.memberTags.assignSuccess"),
      error: (error: ErrorWithResponse) => ({
        message: t("management.memberTags.assignError"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    const result = await request;
    await queryClient.invalidateQueries({
      queryKey: getApiUsersManagerQueryKey(),
    });
    return result;
  };

  return {
    users: users ?? [],
    isLoading,
    assignTag,
    isAssigning: assignMutation.isPending,
  };
}
