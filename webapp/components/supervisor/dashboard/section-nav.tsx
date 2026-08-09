"use client";

import { useLayoutEffect, useRef, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import type { LucideIcon } from "lucide-react";
import {
  LayoutDashboard,
  Eye,
  LogIn as CheckInIcon,
  Store,
} from "lucide-react";

import { cn } from "@/lib/utils";
import { useT } from "@/lib/i18n/hooks";

interface SectionNavItem {
  href: string;
  // The path segment right after "/supervisor" that identifies this
  // section, or undefined for the overview page itself.
  segment: string | undefined;
  label: string;
  icon: LucideIcon;
}

interface Indicator {
  left: number;
  width: number;
}

export function SectionNav() {
  const t = useT();
  const pathname = usePathname();

  const items: SectionNavItem[] = [
    {
      href: "/supervisor",
      segment: undefined,
      label: t("supervisor.nav.overview"),
      icon: LayoutDashboard,
    },
    {
      href: "/supervisor/checkin",
      segment: "checkin",
      label: t("supervisor.nav.checkin"),
      icon: CheckInIcon,
    },
    {
      href: "/supervisor/liveview",
      segment: "liveview",
      label: t("supervisor.nav.liveview"),
      icon: Eye,
    },
    {
      href: "/supervisor/box-office",
      segment: "box-office",
      label: t("supervisor.nav.boxOffice"),
      icon: Store,
    },
  ];

  // Matched by segment rather than substring, so e.g. a future
  // "/supervisor/checkin-history" route can't be mistaken for
  // "/supervisor/checkin".
  const pathSegments = pathname.split("/").filter(Boolean);
  const supervisorIndex = pathSegments.indexOf("supervisor");
  const activeSegment =
    supervisorIndex >= 0 ? pathSegments[supervisorIndex + 1] : undefined;
  const activeIndex = items.findIndex((item) => item.segment === activeSegment);

  const linkRefs = useRef<(HTMLAnchorElement | null)[]>([]);
  const [indicator, setIndicator] = useState<Indicator | null>(null);

  useLayoutEffect(() => {
    const updateIndicator = () => {
      const activeLink = linkRefs.current[activeIndex];
      if (activeLink) {
        setIndicator({
          left: activeLink.offsetLeft,
          width: activeLink.offsetWidth,
        });
      } else {
        setIndicator(null);
      }
    };

    updateIndicator();
    window.addEventListener("resize", updateIndicator);
    return () => window.removeEventListener("resize", updateIndicator);
  }, [activeIndex]);

  return (
    <nav
      className="relative flex max-w-full items-center gap-1 overflow-x-auto px-4 text-muted-foreground [mask-image:linear-gradient(to_right,transparent,black_16px,black_calc(100%-16px),transparent)] lg:px-6 md:peer-data-[state=collapsed]:px-3 md:peer-data-[state=expanded]:px-6"
      aria-label={t("supervisor.nav.label")}
    >
      {indicator && (
        <div
          aria-hidden="true"
          className="absolute bottom-0 h-0.5 rounded-full bg-primary transition-[left,width] duration-200 ease-out"
          style={{ left: indicator.left, width: indicator.width }}
        />
      )}
      {items.map((item, index) => {
        const isActive = index === activeIndex;

        return (
          <Link
            key={item.href}
            ref={(el) => {
              linkRefs.current[index] = el;
            }}
            href={item.href}
            aria-current={isActive ? "page" : undefined}
            title={item.label}
            className={cn(
              "relative inline-flex items-center gap-2 whitespace-nowrap border-b-2 border-transparent px-2.5 py-2.5 text-sm font-medium transition-colors",
              isActive
                ? "text-foreground"
                : "hover:border-border hover:text-foreground",
            )}
          >
            <item.icon className="h-4 w-4 shrink-0" />
            <span className="hidden sm:inline">{item.label}</span>
          </Link>
        );
      })}
    </nav>
  );
}
