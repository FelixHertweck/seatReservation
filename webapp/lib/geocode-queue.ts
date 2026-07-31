export interface GeocodeResult {
  lat: number;
  lon: number;
  displayName: string;
}

interface NominatimResult {
  lat: string;
  lon: string;
  display_name: string;
}

// Nominatim caps requests at 1/sec; this shared cache + dispatch queue
// keeps all callers under that limit.
const MIN_DISPATCH_INTERVAL_MS = 1100;

const cache = new Map<string, GeocodeResult | null>();
const inFlight = new Map<string, Promise<GeocodeResult | null>>();

let queueTail: Promise<void> = Promise.resolve();
let lastDispatch = 0;

function fetchGeocode(
  query: string,
  signal: AbortSignal,
): Promise<GeocodeResult | null> {
  const url = `https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=${encodeURIComponent(query)}`;
  return fetch(url, {
    signal,
    headers: { Accept: "application/json" },
  })
    .then((response) => {
      if (!response.ok) throw new Error("geocode request failed");
      return response.json() as Promise<NominatimResult[]>;
    })
    .then((data) => {
      if (data.length === 0) return null;
      return {
        lat: Number.parseFloat(data[0].lat),
        lon: Number.parseFloat(data[0].lon),
        displayName: data[0].display_name,
      };
    });
}

export function geocode(
  query: string,
  signal: AbortSignal,
): Promise<GeocodeResult | null> {
  if (cache.has(query)) return Promise.resolve(cache.get(query) ?? null);

  const existing = inFlight.get(query);
  if (existing) return existing;

  const promise = new Promise<GeocodeResult | null>((resolve, reject) => {
    // Chained onto queueTail so requests dispatch one at a time, spaced by
    // MIN_DISPATCH_INTERVAL_MS.
    queueTail = queueTail.then(async () => {
      try {
        if (signal.aborted) {
          reject(new DOMException("Aborted", "AbortError"));
          return;
        }
        const wait = Math.max(
          0,
          MIN_DISPATCH_INTERVAL_MS - (Date.now() - lastDispatch),
        );
        if (wait > 0) await new Promise((r) => setTimeout(r, wait));
        lastDispatch = Date.now();

        const result = await fetchGeocode(query, signal);
        cache.set(query, result);
        resolve(result);
      } catch (err) {
        reject(err);
      }
    });
  });

  inFlight.set(query, promise);
  promise.finally(() => inFlight.delete(query));
  return promise;
}
