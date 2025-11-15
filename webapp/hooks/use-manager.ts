"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/hooks/use-toast";
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

interface UseManagerReturn {
  isLoading: boolean;
  events: EventManagementProps;
  locations: LocationManagementProps;
  seats: SeatManagementProps;
  reservations: ReservationManagementProps;
  reservationAllowance: ReservationAllowanceManagementProps;
}

function createIdsSet(query: { ids?: bigint[] } | undefined): Set<bigint> {
  return new Set(query?.ids ?? []);
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
      toast({
        title: t("manager.event.create.success.title"),
        description: t("manager.event.create.success.description"),
      });
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
      toast({
        title: t("manager.event.update.success.title"),
        description: t("manager.event.update.success.description"),
      });
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
      toast({
        title: t("manager.event.delete.success.title"),
        description: t("manager.event.delete.success.description"),
      });
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
      toast({
        title: t("manager.location.create.success.title"),
        description: t("manager.location.create.success.description"),
      });
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
      toast({
        title: t("manager.location.update.success.title"),
        description: t("manager.location.update.success.description"),
      });
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
      toast({
        title: t("manager.location.delete.success.title"),
        description: t("manager.location.delete.success.description"),
      });
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
      toast({
        title: t("manager.location.importWithSeats.success.title"),
        description: t("manager.location.importWithSeats.success.description"),
      });
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
      toast({
        title: t("manager.location.importSeats.success.title"),
        description: t("manager.location.importSeats.success.description"),
      });
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
      toast({
        title: t("manager.seat.create.success.title"),
        description: t("manager.seat.create.success.description"),
      });
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
      toast({
        title: t("manager.seat.update.success.title"),
        description: t("manager.seat.update.success.description"),
      });
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
      toast({
        title: t("manager.seat.delete.success.title"),
        description: t("manager.seat.delete.success.description"),
      });
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
      toast({
        title: t("manager.reservation.create.success.title"),
        description: t("manager.reservation.create.success.description"),
      });
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
      toast({
        title: t("manager.reservation.delete.success.title"),
        description: t("manager.reservation.delete.success.description"),
      });
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
      toast({
        title: t("manager.reservation.blockSeats.success.title"),
        description: t("manager.reservation.blockSeats.success.description"),
      });
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
      toast({
        title: t("manager.reservationAllowance.create.success.title"),
        description: t(
          "manager.reservationAllowance.create.success.description",
        ),
      });
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
      toast({
        title: t("manager.reservationAllowance.update.success.title"),
        description: t(
          "manager.reservationAllowance.update.success.description",
        ),
      });
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
      toast({
        title: t("manager.reservationAllowance.delete.success.title"),
        description: t(
          "manager.reservationAllowance.delete.success.description",
        ),
      });
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
      createEvent: (event: EventRequestDto) =>
        createEventMutation.mutateAsync({ body: event }),
      updateEvent: (id: bigint, event: EventRequestDto) =>
        updateEventMutation.mutateAsync({ path: { id }, body: event }),
      deleteEvent: (ids: bigint[]) =>
        deleteEventMutation.mutateAsync({ query: { ids } }),
    },
    locations: {
      locations: locations ?? [],
      seats: seats ?? [],
      createLocation: (location: EventLocationRequestDto) =>
        createLocationMutation.mutateAsync({ body: location }),
      updateLocation: (id: bigint, location: EventLocationRequestDto) =>
        updateLocationMutation.mutateAsync({ path: { id }, body: location }),
      deleteLocation: (ids: bigint[]) =>
        deleteLocationMutation.mutateAsync({ query: { ids } }),
      importLocationWithSeats: (location: ImportEventLocationDto) =>
        importLocationWithSeatsMutation.mutateAsync({ body: location }),
      importSeats: (seats: ImportSeatDto[], locationId: string) =>
        importSeatsMutation.mutateAsync({
          body: seats,
          path: { id: BigInt(locationId) },
        }),
    },
    seats: {
      locations: locations ?? [],
      seats: seats ?? [],
      createSeat: (seat: SeatRequestDto) =>
        createSeatMutation.mutateAsync({ body: seat }),
      updateSeat: (id: bigint, seat: SeatRequestDto) =>
        updateSeatMutation.mutateAsync({ path: { id }, body: seat }),
      deleteSeat: (ids: bigint[]) =>
        deleteSeatMutation.mutateAsync({ query: { ids } }),
    },
    reservations: {
      users: user ?? [],
      seats: seats ?? [],
      events: events ?? [],
      locations: locations ?? [],
      reservations: reservations ?? [],
      exportCSV: exportReservationsToCsv,
      exportPDF: exportReservationsToPDF,
      createReservation: (reservation: ReservationRequestDto) =>
        createReservationMutation.mutateAsync({ body: reservation }),
      deleteReservation: (ids: bigint[]) =>
        deleteReservationMutation.mutateAsync({ query: { ids } }),
      blockSeats: (seats: BlockSeatsRequestDto) =>
        blockSeatsMutation.mutateAsync({ body: seats }),
    },
    reservationAllowance: {
      events: events ?? [],
      users: user ?? [],
      allowances: reservationAllowance ?? [],
      createReservationAllowance: (allowance: EventUserAllowancesCreateDto) =>
        createReservationAllowanceMutation.mutateAsync({ body: allowance }),
      updateReservationAllowance: (allowance: EventUserAllowanceUpdateDto) =>
        updateReservationAllowanceMutation.mutateAsync({ body: allowance }),
      deleteReservationAllowance: (ids: bigint[]) =>
        deleteReservationAllowanceMutation.mutateAsync({ query: { ids } }),
    },
  };
}
