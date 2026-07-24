"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useT } from "@/lib/i18n/hooks";
import {
  postApiUserSeatcartByEventIdBySeatIdMutation,
  deleteApiUserSeatcartByEventIdBySeatIdMutation,
  getApiUserEventsQueryKey,
} from "@/api/@tanstack/react-query.gen";
import type { SeatCartEntryDto } from "@/api";
import { ErrorWithResponse } from "@/components/init-query-client";

interface UseSeatCartReturn {
  addSeatToCart: (eventId: string, seatId: string) => Promise<SeatCartEntryDto>;
  removeSeatFromCart: (eventId: string, seatId: string) => Promise<void>;
}

export function useSeatCart(): UseSeatCartReturn {
  const t = useT();
  const queryClient = useQueryClient();

  const addMutation = useMutation({
    ...postApiUserSeatcartByEventIdBySeatIdMutation(),
  });
  const removeMutation = useMutation({
    ...deleteApiUserSeatcartByEventIdBySeatIdMutation(),
  });

  const addSeatToCart = async (
    eventId: string,
    seatId: string,
  ): Promise<SeatCartEntryDto> => {
    try {
      return await addMutation.mutateAsync({ path: { eventId, seatId } });
    } catch (error) {
      // Someone else already holds this seat (already reserved, blocked, or in their
      // own cart) - refresh once so the seat map reflects the current state instead
      // of letting the user keep retrying against stale data.
      queryClient.invalidateQueries({ queryKey: getApiUserEventsQueryKey() });
      if ((error as ErrorWithResponse)?.response?.status === 409) {
        toast.error(t("eventReservationModal.cart.conflict.title"), {
          description: t("eventReservationModal.cart.conflict.description"),
        });
      }
      throw error;
    }
  };

  const removeSeatFromCart = async (
    eventId: string,
    seatId: string,
  ): Promise<void> => {
    await removeMutation.mutateAsync({ path: { eventId, seatId } });
  };

  return { addSeatToCart, removeSeatFromCart };
}
