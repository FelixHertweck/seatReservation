"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { useT } from "@/lib/i18n/hooks";
import type { EventLocationRequestDto, EventLocationResponseDto } from "@/api";
import {
  getApiManagerEventlocationsOptions,
  getApiManagerEventlocationsQueryKey,
  postApiManagerEventlocationsMutation,
  putApiManagerEventlocationsByIdMutation,
  deleteApiManagerEventlocationsMutation,
  getApiUsersManagerOptions,
} from "@/api/@tanstack/react-query.gen";
import type { ErrorWithResponse } from "@/components/init-query-client";

export function useManagementLocations() {
  const t = useT();
  const queryClient = useQueryClient();

  const { data: locations, isLoading: locationsLoading } = useQuery({
    ...getApiManagerEventlocationsOptions(),
  });
  const { data: users, isLoading: usersLoading } = useQuery({
    ...getApiUsersManagerOptions(),
  });

  const createLocationMutation = useMutation({
    ...postApiManagerEventlocationsMutation(),
    onSuccess: (data) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerEventlocationsQueryKey() },
        (oldData: EventLocationResponseDto[] | undefined) =>
          oldData ? [...oldData, data] : [data],
      );
    },
  });

  const updateLocationMutation = useMutation({
    ...putApiManagerEventlocationsByIdMutation(),
    onSuccess: (data) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerEventlocationsQueryKey() },
        (oldData: EventLocationResponseDto[] | undefined) =>
          oldData
            ? oldData.map((location) =>
                location.id === data.id ? data : location,
              )
            : [data],
      );
    },
  });

  const deleteLocationMutation = useMutation({
    ...deleteApiManagerEventlocationsMutation(),
    onSuccess: (_, variables) => {
      const idsSet = new Set(variables.query?.ids ?? []);
      queryClient.setQueriesData(
        { queryKey: getApiManagerEventlocationsQueryKey() },
        (oldData: EventLocationResponseDto[] | undefined) =>
          oldData
            ? oldData.filter((location) => !idsSet.has(location.id ?? ""))
            : [],
      );
    },
  });

  // Used both by the plain "New location" step-0 form (meta only, empty
  // markers/areas/seats) and by the JSON-import flow (fully populated
  // payload) - both are the same atomic POST /api/manager/eventlocations.
  const createLocation = async (location: EventLocationRequestDto) => {
    const request = createLocationMutation.mutateAsync({ body: location });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("management.locations.createSuccess"),
      error: (error: ErrorWithResponse) => ({
        message: t("management.locations.createError"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const updateLocation = async (
    id: string,
    location: EventLocationRequestDto,
  ) => {
    const request = updateLocationMutation.mutateAsync({
      path: { id },
      body: location,
    });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("management.locations.updateSuccess"),
      error: (error: ErrorWithResponse) => ({
        message: t("management.locations.updateError"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const deleteLocation = async (ids: string[]) => {
    const request = deleteLocationMutation.mutateAsync({ query: { ids } });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("management.locations.deleteSuccess"),
      error: (error: ErrorWithResponse) => ({
        message: t("management.locations.deleteError"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  return {
    locations: locations ?? [],
    users: users ?? [],
    isLoading: locationsLoading || usersLoading,
    createLocation,
    updateLocation,
    deleteLocation,
  };
}
