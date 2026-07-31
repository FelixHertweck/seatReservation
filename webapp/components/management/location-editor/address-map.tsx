"use client";

import { useEffect } from "react";
import { MapContainer, TileLayer, Marker, useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

import { cn } from "@/lib/utils";
import { useTheme } from "next-themes";

// A simple inline-SVG pin instead of Leaflet's default marker images, which
// need bundler-specific asset-path workarounds. `var(--primary)` picks up
// the app's brand color automatically in both themes.
const pinIcon = L.divIcon({
  className: "",
  html: `<svg width="30" height="42" viewBox="0 0 30 42" xmlns="http://www.w3.org/2000/svg">
    <path d="M15 0C6.7 0 0 6.7 0 15c0 10.5 15 27 15 27s15-16.5 15-27c0-8.3-6.7-15-15-15z" fill="var(--primary)" stroke="white" stroke-width="1.5"/>
    <circle cx="15" cy="15" r="5.5" fill="white"/>
  </svg>`,
  iconSize: [30, 42],
  iconAnchor: [15, 42],
  popupAnchor: [0, -38],
});

function Recenter({ lat, lon }: { lat: number; lon: number }) {
  const map = useMap();
  useEffect(() => {
    map.setView([lat, lon], map.getZoom());
  }, [map, lat, lon]);
  return null;
}

interface AddressMapProps {
  lat: number;
  lon: number;
  displayName?: string;
  className?: string;
  // Set false for purely decorative placements (e.g. a card background),
  // where the map must not steal drag/scroll/keyboard input from the page.
  // The attribution control is kept either way - OpenStreetMap's terms
  // require it to stay visible regardless of how the map is used.
  interactive?: boolean;
}

export default function AddressMap({
  lat,
  lon,
  displayName,
  className,
  interactive = true,
}: AddressMapProps) {
  const { resolvedTheme } = useTheme();

  return (
    <MapContainer
      center={[lat, lon]}
      zoom={15}
      scrollWheelZoom={false}
      zoomControl={interactive}
      dragging={interactive}
      doubleClickZoom={interactive}
      touchZoom={interactive}
      boxZoom={interactive}
      keyboard={interactive}
      className={cn(
        "h-full w-full",
        resolvedTheme === "dark" && "leaflet-dark-tiles",
        className,
      )}
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <Marker position={[lat, lon]} icon={pinIcon} title={displayName} />
      <Recenter lat={lat} lon={lon} />
    </MapContainer>
  );
}
