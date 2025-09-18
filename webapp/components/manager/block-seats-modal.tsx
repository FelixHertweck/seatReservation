"use client";

import type React from "react";

import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { SeatMap } from "@/components/common/seat-map";
import type {
  BlockSeatsRequestDto,
  ManagerEventResponseDto,
  SeatDto,
} from "@/api";
import { useT } from "@/lib/i18n/hooks";

interface BlockSeatsModalProps {
  events: ManagerEventResponseDto[];
  onSubmit: (blockData: BlockSeatsRequestDto) => Promise<void>;
  onClose: () => void;
}

export function BlockSeatsModal({
  events,
  onSubmit,
  onClose,
}: BlockSeatsModalProps) {
  const t = useT();

  const [formData, setFormData] = useState({
    eventId: "",
    seatIds: [] as string[],
  });
  const [selectedSeats, setSelectedSeats] = useState<SeatDto[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const selectedEvent = events?.find(
    (loc) => loc.id?.toString() === formData.eventId,
  );
  const availableSeats: SeatDto[] = selectedEvent?.eventLocation?.seats ?? [];

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.eventId) return;

    setIsLoading(true);

    try {
      const blockData: BlockSeatsRequestDto = {
        eventId: BigInt(selectedEvent?.id || 0),
        seatIds: selectedSeats.map((seat) => seat.id!),
      };
      await onSubmit(blockData);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSeatSelect = (seat: SeatDto) => {
    setSelectedSeats((prev) => {
      const isSelected = prev.some((s) => s.id === seat.id);
      if (isSelected) {
        return prev.filter((s) => s.id !== seat.id);
      } else {
        return [...prev, seat];
      }
    });
  };

  const handleEventChange = (value: string) => {
    setFormData((prev) => ({ ...prev, eventId: value }));
    setSelectedSeats([]);
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-6xl max-h-[90vh] h-[85vh] flex flex-col animate-in fade-in zoom-in duration-300">
        <DialogHeader className="animate-in slide-in-from-top duration-300">
          <DialogTitle className="text-xl font-bold">
            {t("blockSeatsModal.title")}
          </DialogTitle>
          <DialogDescription>
            {t("blockSeatsModal.description")}
          </DialogDescription>
        </DialogHeader>

        <form
          onSubmit={handleSubmit}
          className="flex-1 flex flex-col space-y-4 animate-in slide-in-from-bottom duration-500 min-h-0"
        >
          <div className="space-y-2">
            <Label htmlFor="event">{t("blockSeatsModal.eventLabel")} *</Label>
            <Select
              value={formData.eventId}
              onValueChange={handleEventChange}
              required
            >
              <SelectTrigger>
                <SelectValue
                  placeholder={t("blockSeatsModal.selectEventPlaceholder")}
                />
              </SelectTrigger>
              <SelectContent>
                {events?.map((event) => (
                  <SelectItem
                    key={event.id?.toString()}
                    value={event.id?.toString() ?? "unknown"}
                  >
                    {event.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex gap-4 text-sm">
            <div className="flex items-center gap-2 animate-in slide-in-from-left duration-300">
              <div className="w-4 h-4 bg-green-500 dark:bg-green-400 rounded transition-all duration-300 hover:scale-110"></div>
              <span>{t("blockSeatsModal.available")}</span>
            </div>
            <div
              className="flex items-center gap-2 animate-in slide-in-from-left duration-300"
              style={{ animationDelay: "100ms" }}
            >
              <div className="w-4 h-4 bg-blue-500 dark:bg-blue-400 rounded transition-all duration-300 hover:scale-110"></div>
              <span>{t("blockSeatsModal.selectedToBlock")}</span>
            </div>
            <div
              className="flex items-center gap-2 animate-in slide-in-from-left duration-300"
              style={{ animationDelay: "200ms" }}
            >
              <div className="w-4 h-4 bg-red-500 dark:bg-red-400 rounded transition-all duration-300 hover:scale-110"></div>
              <span>{t("blockSeatsModal.reserved")}</span>
            </div>
            <div
              className="flex items-center gap-2 animate-in slide-in-from-left duration-300"
              style={{ animationDelay: "300ms" }}
            >
              <div className="w-4 h-4 bg-gray-500 dark:bg-gray-400 rounded transition-all duration-300 hover:scale-110"></div>
              <span>{t("blockSeatsModal.alreadyBlocked")}</span>
            </div>
          </div>

          {formData.eventId && availableSeats.length > 0 && (
            <div className="flex-1 min-h-0">
              <SeatMap
                seats={availableSeats}
                selectedSeats={selectedSeats}
                onSeatSelect={handleSeatSelect}
              />
            </div>
          )}

          {selectedSeats.length > 0 && (
            <div className="space-y-2 animate-in slide-in-from-bottom duration-300">
              <h4 className="font-medium">
                {t("blockSeatsModal.seatsToBlockTitle")}
              </h4>
              <div className="flex flex-wrap gap-2">
                {selectedSeats.map((seat, index) => (
                  <Badge
                    key={seat.id?.toString()}
                    variant="outline"
                    className="animate-in zoom-in duration-300 hover:scale-105 transition-transform bg-orange-100 dark:bg-orange-900 border-orange-300 dark:border-orange-700"
                    style={{ animationDelay: `${index * 50}ms` }}
                  >
                    {seat.seatNumber}
                  </Badge>
                ))}
              </div>
            </div>
          )}

          <div className="flex justify-end gap-2 animate-in slide-in-from-bottom duration-300">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              className="hover:scale-[1.02] transition-all duration-300 active:scale-[0.98] bg-transparent"
            >
              {t("blockSeatsModal.cancelButton")}
            </Button>
            <Button
              type="submit"
              disabled={
                isLoading || selectedSeats.length === 0 || !formData.eventId
              }
              className="hover:scale-[1.02] transition-all duration-300 active:scale-[0.98]"
            >
              {isLoading
                ? t("blockSeatsModal.blockingButton")
                : selectedSeats.length === 1
                  ? t("blockSeatsModal.blockSeatButton")
                  : t("blockSeatsModal.blockSeatsButton", {
                      count: selectedSeats.length,
                    })}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
