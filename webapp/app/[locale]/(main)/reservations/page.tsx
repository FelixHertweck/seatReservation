"use client";

import { useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";

// Old bookmarked/shared links pointed at "/reservations?id=<eventId>".
// The reservations view now lives as a tab under "/events" - forward
// visitors there, mapping the old "id" param to the new "eventId" one.
export default function LegacyReservationsRedirect() {
  const router = useRouter();
  const searchParams = useSearchParams();

  useEffect(() => {
    const params = new URLSearchParams(searchParams.toString());
    const legacyEventId = params.get("id");
    if (legacyEventId) {
      params.delete("id");
      params.set("eventId", legacyEventId);
    }
    const query = params.toString();
    const target = query
      ? `/events/reservations?${query}`
      : "/events/reservations";
    router.replace(target);
  }, [router, searchParams]);

  return null;
}
