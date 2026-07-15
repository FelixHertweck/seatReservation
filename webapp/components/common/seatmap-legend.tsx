import { useT } from "@/lib/i18n/hooks";
import type { AreaDto } from "@/api";
import { getAreaColor } from "@/lib/areaColors";
import { cn } from "@/lib/utils";

interface SeatmapLegendProps {
  areas?: AreaDto[];
  // "supervisor": read-only liveview coloring (checked-in/cancelled/no-show tracked separately,
  // so red always means "reserved" consistently with the "selection" variant below).
  // "selection": interactive seat picking (selected + optionally the user's own reservation).
  variant?: "supervisor" | "selection";
  showUserReserved?: boolean;
  // Overrides the label of the "my reserved" swatch. Useful when the yellow
  // status doesn't mean "reserved by me" (e.g. the manager's reservation
  // form, where it highlights seats already reserved by the user being
  // booked for).
  userReservedLabel?: string;
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
  variant = "supervisor",
  showUserReserved = true,
  userReservedLabel,
  layout = "card",
  className,
}: SeatmapLegendProps) {
  const t = useT();
  const bar = layout === "bar";

  const swatches = (
    <>
      <LegendSwatch
        color="bg-green-500"
        label={t("seatStatus.available")}
        bar={bar}
      />
      {variant === "selection" && (
        <LegendSwatch
          color="bg-blue-500"
          label={t("seatStatus.selected")}
          bar={bar}
        />
      )}
      {variant === "selection" && showUserReserved && (
        <LegendSwatch
          color="bg-yellow-500"
          label={userReservedLabel ?? t("seatStatus.myReserved")}
          bar={bar}
        />
      )}
      <LegendSwatch
        color="bg-red-500"
        label={t("seatStatus.reserved")}
        bar={bar}
      />
      <LegendSwatch
        color="bg-gray-500"
        label={t("seatStatus.blocked")}
        bar={bar}
      />
      {variant === "supervisor" && (
        <>
          <LegendSwatch
            color="bg-yellow-300"
            label={t("seatStatus.checkedIn")}
            bar={bar}
          />
          <LegendSwatch
            color="bg-violet-500"
            label={t("seatStatus.cancelled")}
            bar={bar}
          />
          <LegendSwatch
            color="bg-orange-500"
            label={t("seatStatus.noShow")}
            bar={bar}
          />
        </>
      )}
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
