import { useT } from "@/lib/i18n/hooks";
import type { AreaDto } from "@/api";
import { getAreaColor } from "@/lib/areaColors";
import { cn } from "@/lib/utils";

interface SeatmapLegendProps {
  areas?: AreaDto[];
}

export default function SeatmapLegend({ areas = [] }: SeatmapLegendProps) {
  const t = useT();
  return (
    <div className="p-4 border rounded-lg bg-card">
      <h3 className="text-lg font-bold mb-4">{t("liveview.legend.title")}</h3>
      <div className="space-y-3">
        <div className="flex items-center gap-3">
          <div className="w-4 h-4 bg-green-500 rounded"></div>
          <span className="text-sm">{t("seatStatus.available")}</span>
        </div>
        <div className="flex items-center gap-3">
          <div className="w-4 h-4 bg-gray-500 rounded"></div>
          <span className="text-sm">{t("seatStatus.blocked")}</span>
        </div>
        <div className="flex items-center gap-3">
          <div className="w-4 h-4 bg-red-500 rounded"></div>
          <span className="text-sm">{t("seatStatus.noShow")}</span>
        </div>
        <div className="flex items-center gap-3">
          <div className="w-4 h-4 bg-violet-500 rounded"></div>
          <span className="text-sm">{t("seatStatus.cancelled")}</span>
        </div>
        <div className="flex items-center gap-3">
          <div className="w-4 h-4 bg-yellow-300 rounded"></div>
          <span className="text-sm">{t("seatStatus.checkedIn")}</span>
        </div>
      </div>

      {areas.length > 0 && (
        <>
          <div className="my-4 border-t" />
          <div className="space-y-3">
            {areas.map((area, index) => {
              const color = getAreaColor(index);
              return (
                <div
                  key={area.name ?? index}
                  className="flex items-center gap-3"
                >
                  <div
                    className={cn(
                      "w-4 h-4 rounded-sm border-2 border-dashed",
                      color.fill,
                      color.border,
                    )}
                  ></div>
                  <span className="text-sm">{area.name}</span>
                </div>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
}
