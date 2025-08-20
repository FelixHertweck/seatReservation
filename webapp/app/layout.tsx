import type React from "react";
import "./globals.css";
import type { Metadata } from "next";
import { fallbackLng } from "@/lib/i18n/config";

export const metadata: Metadata = {
  title: {
    default: "SeatReservation - Event Seat Booking System",
    template: "%s | SeatReservation",
  },
  description:
    "Book seats for events easily with our modern seat reservation system. Secure, fast, and user-friendly event booking platform.",
  keywords: [
    "seat reservation",
    "event booking",
    "ticket booking",
    "event seats",
    "reservation system",
    "event management",
  ],
  authors: [
    { name: "Felix Hertweck", url: "https://github.com/FelixHertweck" },
  ],
  creator: "Felix Hertweck",
  publisher: "SeatReservation",
  formatDetection: {
    email: false,
    address: false,
    telephone: false,
  },
  metadataBase: new URL(
    process.env.NEXT_PUBLIC_APP_URL || "http://localhost:3000",
  ),
  alternates: {
    canonical: "/",
  },
  openGraph: {
    type: "website",
    locale: "en_US",
    url: "/",
    title: "SeatReservation - Event Seat Booking System",
    description:
      "Book seats for events easily with our modern seat reservation system. Secure, fast, and user-friendly event booking platform.",
    siteName: "SeatReservation",
    images: [
      {
        url: "/logo.png",
        width: 1200,
        height: 691,
        alt: "SeatReservation Logo",
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: "SeatReservation - Event Seat Booking System",
    description:
      "Book seats for events easily with our modern seat reservation system. Secure, fast, and user-friendly event booking platform.",
    images: ["/logo.png"],
    creator: "@felixhertweck",
  },
  robots: {
    index: false,
    follow: false,
    googleBot: {
      index: false,
      follow: false,
      "max-video-preview": -1,
      "max-image-preview": "large",
      "max-snippet": -1,
    },
  },
  icons: {
    icon: [
      { url: "/favicon-16x16.png", sizes: "16x16", type: "image/png" },
      { url: "/favicon-32x32.png", sizes: "32x32", type: "image/png" },
    ],
    apple: [
      { url: "/apple-touch-icon.png", sizes: "180x180", type: "image/png" },
    ],
  },
  manifest: "/site.webmanifest",
  other: {
    "theme-color": "#ffffff",
    "color-scheme": "light dark",
    "mobile-web-app-capable": "yes",
    "apple-mobile-web-app-capable": "yes",
    "apple-mobile-web-app-status-bar-style": "default",
  },
};

export default async function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const lng = fallbackLng;

  return (
    <html lang={lng}>
      <body>{children}</body>
    </html>
  );
}
