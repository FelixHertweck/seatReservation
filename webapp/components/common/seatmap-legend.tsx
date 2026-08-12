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
  showSelected?: boolean;
  showUserReserved?: boolean;
  userReservedLabel?: string;
  showPending?: boolean;
  showLiveStatus?: boolean;
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
          "flex flex-wrap items-center gap-2 md:gap-4 text-sm border-b pb-2 min-h-[34px]",
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
