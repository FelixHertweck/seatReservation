"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useT } from "@/lib/i18n/hooks";
import {
  type EventResponseDto,
  type EventRequestDto,
  type EventLocationResponseDto,
  type SeatDto,
  type ReservationResponseDto,
  type EventUserAllowancesDto,
  type EventLocationRequestDto,
  type SeatRequestDto,
  type ReservationRequestDto,
  type BlockSeatsRequestDto,
  type EventUserAllowancesCreateDto,
  type EventUserAllowanceUpdateDto,
  type ImportEventLocationDto,
  type ImportSeatDto,
  getApiManagerReservationsExportByEventIdCsv,
  getApiManagerReservationsExportByEventIdPdf,
} from "@/api";
import {
  getApiManagerEventsOptions,
  postApiManagerEventsMutation,
  putApiManagerEventsByIdMutation,
  deleteApiManagerEventsMutation,
  getApiManagerEventlocationsOptions,
  postApiManagerEventlocationsMutation,
  putApiManagerEventlocationsByIdMutation,
  deleteApiManagerEventlocationsMutation,
  getApiManagerSeatsOptions,
  postApiManagerSeatsMutation,
  putApiManagerSeatsByIdMutation,
  deleteApiManagerSeatsMutation,
  getApiManagerReservationsOptions,
  postApiManagerReservationsMutation,
  deleteApiManagerReservationsMutation,
  postApiManagerReservationsBlockMutation,
  getApiManagerReservationAllowanceOptions,
  postApiManagerReservationAllowanceMutation,
  putApiManagerReservationAllowanceMutation,
  deleteApiManagerReservationAllowanceMutation,
  getApiManagerEventsQueryKey,
  getApiManagerEventlocationsQueryKey,
  getApiManagerSeatsQueryKey,
  getApiManagerReservationsQueryKey,
  getApiManagerReservationAllowanceQueryKey,
  getApiUsersManagerOptions,
  postApiManagerEventlocationsImportMutation,
  postApiManagerEventlocationsImportByIdMutation,
} from "@/api/@tanstack/react-query.gen";
import type { EventManagementProps } from "@/components/management/event-management";
import type { LocationManagementProps } from "@/components/management/location-management";
import type { ReservationAllowanceManagementProps } from "@/components/management/allowance-management";
import type { ReservationManagementProps } from "@/components/management/reservation-management";
import type { SeatManagementProps } from "@/components/management/seat-management";
import { ErrorWithResponse } from "@/components/init-query-client";

interface UseManagerReturn {
  isLoading: boolean;
  events: EventManagementProps;
  locations: LocationManagementProps;
  seats: SeatManagementProps;
  reservations: ReservationManagementProps;
  reservationAllowance: ReservationAllowanceManagementProps;
}

export function useManager(): UseManagerReturn {
  const t = useT();

  const queryClient = useQueryClient();

  // User
  const { data: user, isLoading: userIsLoading } = useQuery({
    ...getApiUsersManagerOptions(),
  });

  // Event management
  const {
    data: events,
    isLoading: eventsIsLoading,
    refetch: eventsRefetch,
  } = useQuery({
    ...getApiManagerEventsOptions(),
  });

  const createEventMutation = useMutation({
    ...postApiManagerEventsMutation(),
    onSuccess: (data) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerEventsQueryKey() },
        (oldData: EventResponseDto[] | undefined) => {
          return oldData ? [...oldData, data] : [data];
        },
      );
    },
  });

  const updateEventMutation = useMutation({
    ...putApiManagerEventsByIdMutation(),
    onSuccess: (data) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerEventsQueryKey() },
        (oldData: EventResponseDto[] | undefined) => {
          return oldData
            ? oldData.map((event) => (event.id === data.id ? data : event))
            : [data];
        },
      );
    },
  });

  const deleteEventMutation = useMutation({
    ...deleteApiManagerEventsMutation(),
    onSuccess: (_, variables) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerEventsQueryKey() },
        (oldData: EventResponseDto[] | undefined) => {
          const idsSet = createIdsSet(variables.query);
          return oldData
            ? oldData.filter((event) => !idsSet.has(event.id ?? BigInt(-1)))
            : [];
        },
      );
    },
  });

  // Location management
  const { data: locations, isLoading: locationsIsLoading } = useQuery({
    ...getApiManagerEventlocationsOptions(),
  });

  const createLocationMutation = useMutation({
    ...postApiManagerEventlocationsMutation(),
    onSuccess: (data) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerEventlocationsQueryKey() },
        (oldData: EventLocationResponseDto[] | undefined) => {
          return oldData ? [...oldData, data] : [data];
        },
      );
    },
  });

  const updateLocationMutation = useMutation({
    ...putApiManagerEventlocationsByIdMutation(),
    onSuccess: (data) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerEventlocationsQueryKey() },
        (oldData: EventLocationResponseDto[] | undefined) => {
          return oldData
            ? oldData.map((location) =>
                location.id === data.id ? data : location,
              )
            : [data];
        },
      );
    },
  });

  const deleteLocationMutation = useMutation({
    ...deleteApiManagerEventlocationsMutation(),
    onSuccess: (_, variables) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerEventlocationsQueryKey() },
        (oldData: EventLocationResponseDto[] | undefined) => {
          const idsSet = createIdsSet(variables.query);
          return oldData
            ? oldData.filter(
                (location) => !idsSet.has(location.id ?? BigInt(-1)),
              )
            : [];
        },
      );
    },
  });

  const importLocationWithSeatsMutation = useMutation({
    ...postApiManagerEventlocationsImportMutation(),
    onSuccess: (data) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerEventlocationsQueryKey() },
        (oldData: EventLocationResponseDto[] | undefined) => {
          return oldData ? [...oldData, data] : [data];
        },
      );
    },
  });

  const importSeatsMutation = useMutation({
    ...postApiManagerEventlocationsImportByIdMutation(),
    onSuccess: (data) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerEventlocationsQueryKey() },
        (oldData: EventLocationResponseDto[] | undefined) => {
          return oldData
            ? oldData.map((location) =>
                location.id === data.id ? data : location,
              )
            : [data];
        },
      );
    },
  });

  // Seat management
  const { data: seats, isLoading: seatsIsLoading } = useQuery({
    ...getApiManagerSeatsOptions(),
  });

  const createSeatMutation = useMutation({
    ...postApiManagerSeatsMutation(),
    onSuccess: (data) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerSeatsQueryKey() },
        (oldData: SeatDto[] | undefined) => {
          return oldData ? [...oldData, data] : [data];
        },
      );
      eventsRefetch();
    },
  });

  const updateSeatMutation = useMutation({
    ...putApiManagerSeatsByIdMutation(),
    onSuccess: (data) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerSeatsQueryKey() },
        (oldData: SeatDto[] | undefined) => {
          return oldData
            ? oldData.map((seat) => (seat.id === data.id ? data : seat))
            : [data];
        },
      );
    },
  });

  const deleteSeatMutation = useMutation({
    ...deleteApiManagerSeatsMutation(),
    onSuccess: (_, variables) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerSeatsQueryKey() },
        (oldData: SeatDto[] | undefined) => {
          const idsSet = createIdsSet(variables.query);
          return oldData
            ? oldData.filter((seat) => !idsSet.has(seat.id ?? BigInt(-1)))
            : [];
        },
      );
    },
  });

  // Reservation management
  const { data: reservations, isLoading: reservationsIsLoading } = useQuery({
    ...getApiManagerReservationsOptions(),
  });

  const createReservationMutation = useMutation({
    ...postApiManagerReservationsMutation(),
    onSuccess: (data) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerReservationsQueryKey() },
        (oldData: ReservationResponseDto[] | undefined) => {
          return oldData ? [...oldData, ...data] : [...data];
        },
      );
      eventsRefetch();
    },
  });

  const deleteReservationMutation = useMutation({
    ...deleteApiManagerReservationsMutation(),
    onSuccess: (_, variables) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerReservationsQueryKey() },
        (oldData: ReservationResponseDto[] | undefined) => {
          const idsSet = createIdsSet(variables.query);
          return oldData
            ? oldData.filter(
                (reservation) => !idsSet.has(reservation.id ?? BigInt(-1)),
              )
            : [];
        },
      );
    },
  });

  const blockSeatsMutation = useMutation({
    ...postApiManagerReservationsBlockMutation(),
    onSuccess: (data) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerReservationsQueryKey() },
        (oldData: ReservationResponseDto[] | undefined) => {
          return oldData ? [...oldData, ...data] : [...data];
        },
      );
      eventsRefetch();
    },
  });

  // Reservation allowance management
  const {
    data: reservationAllowance,
    isLoading: reservationAllowanceIsLoading,
  } = useQuery({
    ...getApiManagerReservationAllowanceOptions(),
  });

  const createReservationAllowanceMutation = useMutation({
    ...postApiManagerReservationAllowanceMutation(),
    onSuccess: (data) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerReservationAllowanceQueryKey() },
        (oldData: EventUserAllowancesDto[] | undefined) => {
          return oldData ? [...oldData, ...data] : [...data];
        },
      );
    },
  });

  const updateReservationAllowanceMutation = useMutation({
    ...putApiManagerReservationAllowanceMutation(),
    onSuccess: (data) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerReservationAllowanceQueryKey() },
        (oldData: EventUserAllowancesDto[] | undefined) => {
          return oldData
            ? oldData.map((allowance) =>
                allowance.id === data.id ? data : allowance,
              )
            : [data];
        },
      );
    },
  });

  const deleteReservationAllowanceMutation = useMutation({
    ...deleteApiManagerReservationAllowanceMutation(),
    onSuccess: (_, variables) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerReservationAllowanceQueryKey() },
        (oldData: EventUserAllowancesDto[] | undefined) => {
          const idsSet = createIdsSet(variables.query);
          return oldData
            ? oldData.filter(
                (allowance) => !idsSet.has(allowance.id ?? BigInt(-1)),
              )
            : [];
        },
      );
    },
  });

  const exportReservationsToCsv = async (eventId: bigint): Promise<Blob> => {
    const response = await getApiManagerReservationsExportByEventIdCsv({
      path: { eventId: BigInt(eventId) },
    });

    return new Blob([response.data as string], {
      type: "text/csv;charset=utf-8;",
    });
  };

  const exportReservationsToPDF = async (eventId: bigint): Promise<Blob> => {
    const response = await getApiManagerReservationsExportByEventIdPdf({
      path: { eventId: BigInt(eventId) },
    });

    return new Blob([response.data as File], {
      type: "application/pdf",
    });
  };

  const handleCreateEvent = async (event: EventRequestDto) => {
    const request = createEventMutation.mutateAsync({ body: event });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("manager.event.create.success.title"),

      error: (error: ErrorWithResponse) => ({
        message: t("manager.event.create.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const handleUpdateEvent = async (id: bigint, event: EventRequestDto) => {
    const request = updateEventMutation.mutateAsync({
      path: { id },
      body: event,
    });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("manager.event.update.success.title"),
      error: (error: ErrorWithResponse) => ({
        message: t("manager.event.update.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const handleDeleteEvent = async (ids: bigint[]) => {
    const request = deleteEventMutation.mutateAsync({ query: { ids } });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("manager.event.delete.success.title"),
      error: (error: ErrorWithResponse) => ({
        message: t("manager.event.delete.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const handleCreateLocation = async (location: EventLocationRequestDto) => {
    const request = createLocationMutation.mutateAsync({ body: location });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("manager.location.create.success.title"),
      error: (error: ErrorWithResponse) => ({
        message: t("manager.location.create.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const handleUpdateLocation = async (
    id: bigint,
    location: EventLocationRequestDto,
  ) => {
    const request = updateLocationMutation.mutateAsync({
      path: { id },
      body: location,
    });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("manager.location.update.success.title"),
      error: (error: ErrorWithResponse) => ({
        message: t("manager.location.update.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const handleDeleteLocation = async (ids: bigint[]) => {
    const request = deleteLocationMutation.mutateAsync({ query: { ids } });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("manager.location.delete.success.title"),
      error: (error: ErrorWithResponse) => ({
        message: t("manager.location.delete.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const handleImportLocationWithSeats = async (
    location: ImportEventLocationDto,
  ) => {
    const request = importLocationWithSeatsMutation.mutateAsync({
      body: location,
    });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("manager.location.importWithSeats.success.title"),
      error: (error: ErrorWithResponse) => ({
        message: t("manager.location.importWithSeats.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const handleImportSeats = async (
    seatsToImport: ImportSeatDto[],
    locationId: string,
  ) => {
    const request = importSeatsMutation.mutateAsync({
      body: seatsToImport,
      path: { id: BigInt(locationId) },
    });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("manager.location.importSeats.success.title"),
      error: (error: ErrorWithResponse) => ({
        message: t("manager.location.importSeats.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const handleCreateSeat = async (seat: SeatRequestDto) => {
    const request = createSeatMutation.mutateAsync({ body: seat });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("manager.seat.create.success.title"),
      error: (error: ErrorWithResponse) => ({
        message: t("manager.seat.create.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const handleUpdateSeat = async (id: bigint, seat: SeatRequestDto) => {
    const request = updateSeatMutation.mutateAsync({
      path: { id },
      body: seat,
    });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("manager.seat.update.success.title"),
      error: (error: ErrorWithResponse) => ({
        message: t("manager.seat.update.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const handleDeleteSeat = async (ids: bigint[]) => {
    const request = deleteSeatMutation.mutateAsync({ query: { ids } });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("manager.seat.delete.success.title"),
      error: (error: ErrorWithResponse) => ({
        message: t("manager.seat.delete.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const handleCreateReservation = async (
    reservation: ReservationRequestDto,
  ) => {
    const request = createReservationMutation.mutateAsync({
      body: reservation,
    });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("manager.reservation.create.success.title"),
      error: (error: ErrorWithResponse) => ({
        message: t("manager.reservation.create.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const handleDeleteReservation = async (ids: bigint[]) => {
    const request = deleteReservationMutation.mutateAsync({ query: { ids } });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("manager.reservation.delete.success.title"),
      error: (error: ErrorWithResponse) => ({
        message: t("manager.reservation.delete.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const handleBlockSeats = async (seatsToBlock: BlockSeatsRequestDto) => {
    const request = blockSeatsMutation.mutateAsync({ body: seatsToBlock });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("manager.reservation.blockSeats.success.title"),
      error: (error: ErrorWithResponse) => ({
        message: t("manager.reservation.blockSeats.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const handleCreateReservationAllowance = async (
    allowance: EventUserAllowancesCreateDto,
  ) => {
    const request = createReservationAllowanceMutation.mutateAsync({
      body: allowance,
    });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("manager.reservationAllowance.create.success.title"),
      error: (error: ErrorWithResponse) => ({
        message: t("manager.reservationAllowance.create.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const handleUpdateReservationAllowance = async (
    allowance: EventUserAllowanceUpdateDto,
  ) => {
    const request = updateReservationAllowanceMutation.mutateAsync({
      body: allowance,
    });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("manager.reservationAllowance.update.success.title"),
      error: (error: ErrorWithResponse) => ({
        message: t("manager.reservationAllowance.update.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const handleDeleteReservationAllowance = async (ids: bigint[]) => {
    const request = deleteReservationAllowanceMutation.mutateAsync({
      query: { ids },
    });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("manager.reservationAllowance.delete.success.title"),
      error: (error: ErrorWithResponse) => ({
        message: t("manager.reservationAllowance.delete.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const isLoading =
    eventsIsLoading ||
    locationsIsLoading ||
    seatsIsLoading ||
    reservationsIsLoading ||
    reservationAllowanceIsLoading ||
    userIsLoading;

  return {
    isLoading,
    events: {
      allLocations: locations ?? [],
      events: events ?? [],
      users: user ?? [],
      createEvent: handleCreateEvent,
      updateEvent: handleUpdateEvent,
      deleteEvent: handleDeleteEvent,
    },
    locations: {
      locations: locations ?? [],
      seats: seats ?? [],
      createLocation: handleCreateLocation,
      updateLocation: handleUpdateLocation,
      deleteLocation: handleDeleteLocation,
      importLocationWithSeats: handleImportLocationWithSeats,
      importSeats: handleImportSeats,
    },
    seats: {
      locations: locations ?? [],
      seats: seats ?? [],
      createSeat: handleCreateSeat,
      updateSeat: handleUpdateSeat,
      deleteSeat: handleDeleteSeat,
    },
    reservations: {
      users: user ?? [],
      seats: seats ?? [],
      events: events ?? [],
      locations: locations ?? [],
      reservations: reservations ?? [],
      exportCSV: exportReservationsToCsv,
      exportPDF: exportReservationsToPDF,
      createReservation: handleCreateReservation,
      deleteReservation: handleDeleteReservation,
      blockSeats: handleBlockSeats,
    },
    reservationAllowance: {
      events: events ?? [],
      users: user ?? [],
      allowances: reservationAllowance ?? [],
      createReservationAllowance: handleCreateReservationAllowance,
      updateReservationAllowance: handleUpdateReservationAllowance,
      deleteReservationAllowance: handleDeleteReservationAllowance,
    },
  };
}

function createIdsSet(query: { ids?: bigint[] } | undefined): Set<bigint> {
  return new Set(query?.ids ?? []);
}
