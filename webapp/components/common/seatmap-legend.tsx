import { useT } from "@/lib/i18n/hooks";
import type { AreaDto } from "@/api";
import { getAreaColor } from "@/lib/areaColors";
import { cn } from "@/lib/utils";
import {
  SEAT_STATUS_BG,
  SEAT_STATUS_LABEL_KEY,
  type SeatVisualStatus,
} from "@/lib/seatStatusStyles";

interface SeatmapLegendProps {
  areas?: AreaDto[];
  // Every swatch below is gated by whether its category can structurally occur for the data
  // this particular page shows - NOT by whether it currently happens to occur. A category
  // that could appear (e.g. "Blocked" on a location with no blocked seats yet) still needs
  // to show, so viewers learn what the color means before they ever encounter it; only
  // categories that can never occur here (e.g. "Pending" on a page whose data never carries
  // cart-hold info) should be omitted.

  // Blue "Selected" - shown on any map where the viewer can mark seats as selected/chosen,
  // whether by clicking the map itself or via an external control (e.g. checkboxes).
  showSelected?: boolean;
  // Yellow "My reservation" - highlights seats already reserved by a specific user.
  showUserReserved?: boolean;
  // Overrides the label of the "my reserved" swatch. Useful when the yellow
  // status doesn't mean "reserved by me" (e.g. the manager's reservation
  // form, where it highlights seats already reserved by the user being
  // booked for).
  userReservedLabel?: string;
  // Amber "Selected by another user" - only possible for data sourced from the cart-aware
  // user booking endpoints (event-reservation-modal, reservation-modal); never appears for
  // supervisor or management data, which don't track Redis cart holds at all.
  showPending?: boolean;
  // Checked-in/Cancelled/No-show - only possible when the underlying data is
  // SupervisorSeatStatusDto (box office, live view); management/booking data never carries
  // a liveStatus field, so these can never occur there.
  showLiveStatus?: boolean;
  // "card": boxed, vertical list (liveview sidebar). "bar": compact, wraps horizontally (dialogs).
  layout?: "card" | "bar";
  className?: string;
}

function LegendSwatch({
  color,
  label,
  bar,
}: {
  color: string;
  label: string;
  bar: boolean;
}) {
  return (
    <div className="flex items-center gap-2">
      <div className={cn("w-4 h-4 rounded", color)} />
      <span className={bar ? "text-sm" : undefined}>{label}</span>
    </div>
  );
}

export default function SeatmapLegend({
  areas = [],
  showSelected = false,
  showUserReserved = false,
  userReservedLabel,
  showPending = false,
  showLiveStatus = false,
  layout = "card",
  className,
}: Readonly<SeatmapLegendProps>) {
  const t = useT();
  const bar = layout === "bar";

  const swatch = (status: SeatVisualStatus, label?: string) => (
    <LegendSwatch
      key={status}
      color={SEAT_STATUS_BG[status]}
      label={label ?? t(SEAT_STATUS_LABEL_KEY[status])}
      bar={bar}
    />
  );

  const swatches = (
    <>
      {/* Available/Reserved/Blocked are structurally possible on every seatmap. */}
      {swatch("AVAILABLE")}
      {showSelected && swatch("SELECTED")}
      {showUserReserved && swatch("USER_RESERVED", userReservedLabel)}
      {swatch("RESERVED")}
      {swatch("BLOCKED")}
      {showPending && swatch("PENDING")}
      {showLiveStatus && swatch("CHECKED_IN")}
      {showLiveStatus && swatch("CANCELLED")}
      {showLiveStatus && swatch("NO_SHOW")}
    </>
  );

  const areaSwatches = areas.map((area, index) => {
    const color = getAreaColor(index);
    return (
      <div key={area.name ?? index} className="flex items-center gap-2">
        <div
          className={cn(
            "w-4 h-4 rounded-sm border-2 border-dashed",
            color.fill,
            color.border,
          )}
        ></div>
        <span className={bar ? undefined : "text-sm"}>{area.name}</span>
      </div>
    );
  });

  if (bar) {
    return (
      <div
        className={cn(
          "flex flex-wrap gap-2 md:gap-4 text-sm border-b pb-1",
          className,
        )}
      >
        {swatches}
        {areas.length > 0 && (
          <>
            <div className="w-px self-stretch bg-border hidden sm:block" />
            {areaSwatches}
          </>
        )}
      </div>
    );
  }

  return (
    <div className={cn("p-4 border rounded-lg bg-card", className)}>
      <h3 className="text-lg font-bold mb-4">{t("liveview.legend.title")}</h3>
      <div className="space-y-3">{swatches}</div>

      {areas.length > 0 && (
        <>
          <div className="my-4 border-t" />
          <div className="space-y-3">{areaSwatches}</div>
        </>
      )}
    </div>
  );
}
