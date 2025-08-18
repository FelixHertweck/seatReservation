"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import type {
  DetailedEventResponseDto,
  EventRequestDto,
  EventLocationResponseDto,
  SeatResponseDto,
  DetailedReservationResponseDto,
  EventUserAllowancesDto,
  EventLocationRequestDto,
  SeatRequestDto,
  ReservationRequestDto,
  BlockSeatsRequestDto,
  EventUserAllowancesCreateDto, // Import new create DTO
  EventUserAllowanceUpdateDto, // Import new update DTO
  UserDto,
  ImportEventLocationDto,
  ImportSeatDto, // Import UserDto for use in useManager return type
} from "@/api";
import {
  getApiManagerEventsOptions,
  postApiManagerEventsMutation,
  putApiManagerEventsByIdMutation,
  deleteApiManagerEventsByIdMutation,
  getApiManagerEventlocationsOptions,
  postApiManagerEventlocationsMutation,
  putApiManagerEventlocationsByIdMutation,
  deleteApiManagerEventlocationsByIdMutation,
  getApiManagerSeatsOptions,
  postApiManagerSeatsMutation,
  putApiManagerSeatsByIdMutation,
  deleteApiManagerSeatsByIdMutation,
  getApiManagerReservationsOptions,
  postApiManagerReservationsMutation,
  deleteApiManagerReservationsByIdMutation,
  postApiManagerReservationsBlockMutation,
  getApiManagerReservationAllowanceOptions,
  postApiManagerReservationAllowanceMutation,
  putApiManagerReservationAllowanceMutation, // Import new update mutation
  deleteApiManagerReservationAllowanceByIdMutation,
  getApiManagerEventsQueryKey,
  getApiManagerEventlocationsQueryKey,
  getApiManagerSeatsQueryKey,
  getApiManagerReservationsQueryKey,
  getApiManagerReservationAllowanceQueryKey,
  getApiUsersManagerOptions,
  postApiManagerEventlocationsImportMutation,
  postApiManagerEventlocationsImportByIdMutation,
} from "@/api/@tanstack/react-query.gen";
import type { EventManagementProps } from "@/components/manager/event-management";
import type { LocationManagementProps } from "@/components/manager/location-management";
import type { ReservationAllowanceManagementProps } from "@/components/manager/reservation-allowance-management";
import type { ReservationManagementProps } from "@/components/manager/reservation-management";
import type { SeatManagementProps } from "@/components/manager/seat-management";

interface UseManagerReturn {
  isLoading: boolean;
  events: EventManagementProps;
  locations: LocationManagementProps;
  seats: SeatManagementProps;
  reservations: ReservationManagementProps;
  reservationAllowance: ReservationAllowanceManagementProps;
}

export function useManager(): UseManagerReturn {
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
        (oldData: DetailedEventResponseDto[] | undefined) => {
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
        (oldData: DetailedEventResponseDto[] | undefined) => {
          return oldData
            ? oldData.map((event) => (event.id === data.id ? data : event))
            : [data];
        },
      );
    },
  });

  const deleteEventMutation = useMutation({
    ...deleteApiManagerEventsByIdMutation(),
    onSuccess: (_, variables) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerEventsQueryKey() },
        (oldData: DetailedEventResponseDto[] | undefined) => {
          return oldData
            ? oldData.filter((event) => event.id !== variables.path.id)
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
    ...deleteApiManagerEventlocationsByIdMutation(),
    onSuccess: (_, variables) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerEventlocationsQueryKey() },
        (oldData: EventLocationResponseDto[] | undefined) => {
          return oldData
            ? oldData.filter((location) => location.id !== variables.path.id)
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
        (oldData: SeatResponseDto[] | undefined) => {
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
        (oldData: SeatResponseDto[] | undefined) => {
          return oldData
            ? oldData.map((seat) => (seat.id === data.id ? data : seat))
            : [data];
        },
      );
    },
  });

  const deleteSeatMutation = useMutation({
    ...deleteApiManagerSeatsByIdMutation(),
    onSuccess: (_, variables) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerSeatsQueryKey() },
        (oldData: SeatResponseDto[] | undefined) => {
          return oldData
            ? oldData.filter((seat) => seat.id !== variables.path.id)
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
        (oldData: DetailedReservationResponseDto[] | undefined) => {
          return oldData ? [...oldData, ...data] : [...data];
        },
      );
      eventsRefetch();
    },
  });

  const deleteReservationMutation = useMutation({
    ...deleteApiManagerReservationsByIdMutation(),
    onSuccess: (_, variables) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerReservationsQueryKey() },
        (oldData: DetailedReservationResponseDto[] | undefined) => {
          return oldData
            ? oldData.filter(
                (reservation) => reservation.id !== variables.path.id,
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
        (oldData: DetailedReservationResponseDto[] | undefined) => {
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
    ...deleteApiManagerReservationAllowanceByIdMutation(),
    onSuccess: (_, variables) => {
      queryClient.setQueriesData(
        { queryKey: getApiManagerReservationAllowanceQueryKey() },
        (oldData: EventUserAllowancesDto[] | undefined) => {
          return oldData
            ? oldData.filter((allowance) => allowance.id !== variables.path.id)
            : [];
        },
      );
    },
  });

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
      createEvent: (event: EventRequestDto) =>
        createEventMutation.mutateAsync({ body: event }),
      updateEvent: (id: bigint, event: EventRequestDto) =>
        updateEventMutation.mutateAsync({ path: { id }, body: event }),
      deleteEvent: (id: bigint) =>
        deleteEventMutation.mutateAsync({ path: { id } }),
    },
    locations: {
      locations: locations ?? [],
      createLocation: (location: EventLocationRequestDto) =>
        createLocationMutation.mutateAsync({ body: location }),
      updateLocation: (id: bigint, location: EventLocationRequestDto) =>
        updateLocationMutation.mutateAsync({ path: { id }, body: location }),
      deleteLocation: (id: bigint) =>
        deleteLocationMutation.mutateAsync({ path: { id } }),
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
      deleteSeat: (id: bigint) =>
        deleteSeatMutation.mutateAsync({ path: { id } }),
    },
    reservations: {
      users: user ?? [],
      events: events ?? [],
      seats: seats ?? [],
      reservations: reservations ?? [],
      createReservation: (reservation: ReservationRequestDto) =>
        createReservationMutation.mutateAsync({ body: reservation }),
      deleteReservation: (id: bigint) =>
        deleteReservationMutation.mutateAsync({ path: { id } }),
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
      deleteReservationAllowance: (id: bigint) =>
        deleteReservationAllowanceMutation.mutateAsync({ path: { id } }),
    },
  };
}
