// Stable color assignment for seat map areas, distinct from the seat-status
// colors (green/red/gray/blue/yellow/violet) so zones never look like a
// reservation state. Index is the position of the area in the `areas` array
// returned by the API, which is stable across renders.
//
// `hex` mirrors the Tailwind utility classes (`swatch`/`fill`/`border`) as a
// raw color value for contexts that can't use Tailwind classes, such as SVG
// `fill`/`stroke` attributes when rendering a custom area boundary polygon.
const AREA_PALETTE = [
  {
    swatch: "bg-amber-400",
    fill: "bg-amber-400/10",
    border: "border-amber-400/60",
    text: "text-amber-700 dark:text-amber-300",
    hex: "#fbbf24",
  },
  {
    swatch: "bg-cyan-400",
    fill: "bg-cyan-400/10",
    border: "border-cyan-400/60",
    text: "text-cyan-700 dark:text-cyan-300",
    hex: "#22d3ee",
  },
  {
    swatch: "bg-indigo-400",
    fill: "bg-indigo-400/10",
    border: "border-indigo-400/60",
    text: "text-indigo-700 dark:text-indigo-300",
    hex: "#818cf8",
  },
  {
    swatch: "bg-pink-400",
    fill: "bg-pink-400/10",
    border: "border-pink-400/60",
    text: "text-pink-700 dark:text-pink-300",
    hex: "#f472b6",
  },
  {
    swatch: "bg-orange-400",
    fill: "bg-orange-400/10",
    border: "border-orange-400/60",
    text: "text-orange-700 dark:text-orange-300",
    hex: "#fb923c",
  },
  {
    swatch: "bg-teal-400",
    fill: "bg-teal-400/10",
    border: "border-teal-400/60",
    text: "text-teal-700 dark:text-teal-300",
    hex: "#2dd4bf",
  },
] as const;

export type AreaColor = (typeof AREA_PALETTE)[number];

export function getAreaColor(index: number): AreaColor {
  return AREA_PALETTE[index % AREA_PALETTE.length];
}
