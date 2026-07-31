"use client";

import dynamic from "next/dynamic";

import { useGeocode } from "@/hooks/use-geocode";

const AddressMap = dynamic(
  () => import("@/components/management/location-editor/address-map"),
  { ssr: false },
);

interface LocationCardMapBackgroundProps {
  address: string;
}

// Grows the container past the card's bottom edge to shift Leaflet's
// centered marker further down; the overflow is clipped by `overflow-hidden`.
const MARKER_DOWNWARD_OFFSET_PX = 56;

// Decorative map preview filling a location card behind its text content.
// Renders nothing until geocoding resolves, so a slow/unfound address just
// leaves the card's plain background in place instead of showing a
// placeholder box.
export function LocationCardMapBackground({
  address,
}: LocationCardMapBackgroundProps) {
  const { result: geocoded } = useGeocode(address);

  if (!geocoded) return null;

  return (
    <div
      className="pointer-events-none absolute inset-x-0 top-0 z-0"
      style={{ height: `calc(100% + ${MARKER_DOWNWARD_OFFSET_PX}px)` }}
    >
      {/* Dimming is scoped to `.leaflet-tile-pane` (see globals.css) rather
          than opacity/filter here, which would trap the attribution
          control's z-index below the overlay. */}
      <AddressMap
        lat={geocoded.lat}
        lon={geocoded.lon}
        interactive={false}
        className="location-card-map"
      />
      {/* Fades from nearly transparent at the top, so the map stays
          visible behind the title, toward a more opaque floor at the
          bottom for legible contrast behind the footer buttons.
          `color-mix()` against `var(--card)` looked right on paper but
          silently failed to resolve in testing, leaving `background`
          invalid (i.e. fully transparent) - plain `rgb(.. / alpha)`
          matching `--card`'s actual light/dark values is far more
          broadly supported. */}
      <div className="absolute inset-0 location-card-map-fade" />
    </div>
  );
}
