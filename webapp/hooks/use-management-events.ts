"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { useT } from "@/lib/i18n/hooks";
import type { EventRequestDto, EventResponseDto } from "@/api";
import {
  getApiManagerEventsOptions,
  getApiManagerEventsQueryKey,
  postApiManagerEventsMutation,
  putApiManagerEventsByIdMutation,
  deleteApiManagerEventsMutation,
  getApiManagerEventlocationsOptions,
  getApiUsersManagerOptions,
} from "@/api/@tanstack/react-query.gen";
import type { ErrorWithResponse } from "@/components/init-query-client";

export function useManagementEvents() {
  const t = useT();
  const queryClient = useQueryClient();

  const { data: events, isLoading: eventsLoading } = useQuery({
    ...getApiManagerEventsOptions(),
  });
  const { data: locations, isLoading: locationsLoading } = useQuery({
    ...getApiManagerEventlocationsOptions(),
  });
  const { data: users, isLoading: usersLoading } = useQuery({
    ...getApiUsersManagerOptions(),
  });

  const createMutation = useMutation({
    ...postApiManagerEventsMutation(),
    onSuccess: (data) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerEventsQueryKey() },
        (oldData: EventResponseDto[] | undefined) =>
          oldData ? [...oldData, data] : [data],
      );
    },
  });

  const updateMutation = useMutation({
    ...putApiManagerEventsByIdMutation(),
    onSuccess: (data) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerEventsQueryKey() },
        (oldData: EventResponseDto[] | undefined) =>
          oldData
            ? oldData.map((event) => (event.id === data.id ? data : event))
            : [data],
      );
    },
  });

  const deleteMutation = useMutation({
    ...deleteApiManagerEventsMutation(),
    onSuccess: (_, variables) => {
      const idsSet = new Set(variables.query?.ids ?? []);
      queryClient.setQueriesData(
        { queryKey: getApiManagerEventsQueryKey() },
        (oldData: EventResponseDto[] | undefined) =>
          oldData ? oldData.filter((event) => !idsSet.has(event.id ?? "")) : [],
      );
    },
  });

  const createEvent = async (event: EventRequestDto) => {
    const request = createMutation.mutateAsync({ body: event });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("management.events.createSuccess"),
      error: (error: ErrorWithResponse) => ({
        message: t("management.events.createError"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const updateEvent = async (id: string, event: EventRequestDto) => {
    const request = updateMutation.mutateAsync({ path: { id }, body: event });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("management.events.updateSuccess"),
      error: (error: ErrorWithResponse) => ({
        message: t("management.events.updateError"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const deleteEvent = async (ids: string[]) => {
    const request = deleteMutation.mutateAsync({ query: { ids } });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("management.events.deleteSuccess"),
      error: (error: ErrorWithResponse) => ({
        message: t("management.events.deleteError"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  return {
    events: events ?? [],
    locations: locations ?? [],
    users: users ?? [],
    isLoading: eventsLoading || locationsLoading || usersLoading,
    createEvent,
    updateEvent,
    deleteEvent,
  };
}
