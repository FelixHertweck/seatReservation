"use client";

import {
  useLayoutEffect,
  useRef,
  useState,
  cloneElement,
  type ReactElement,
  type Ref,
} from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import type { AppIcon, AnimatedIconHandle } from "@/lib/icon-type";

import { LayoutGridIcon } from "@/components/ui/layout-grid";
import { MapPinIcon } from "@/components/ui/map-pin";
import { BookmarkCheckIcon } from "@/components/ui/bookmark-check";
import { TicketIcon } from "@/components/ui/ticket";
import { CalendarDaysIcon } from "@/components/ui/calendar-days";

import { cn } from "@/lib/utils";
import { useT } from "@/lib/i18n/hooks";

interface SectionNavItem {
  href: string;
  // The path segment right after "/management" that identifies this
  // section, or undefined for the overview page itself.
  segment: string | undefined;
  label: string;
  icon: AppIcon;
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
      href: "/management",
      segment: undefined,
      label: t("management.nav.overview"),
      icon: LayoutGridIcon,
    },
    {
      href: "/management/locations",
      segment: "locations",
      label: t("management.nav.locations"),
      icon: MapPinIcon,
    },
    {
      href: "/management/events",
      segment: "events",
      label: t("management.nav.events"),
      icon: CalendarDaysIcon,
    },
    {
      href: "/management/reservations",
      segment: "reservations",
      label: t("management.nav.reservations"),
      icon: BookmarkCheckIcon,
    },
    {
      href: "/management/allowances",
      segment: "allowances",
      label: t("management.nav.allowances"),
      icon: TicketIcon,
    },
  ];

  // Matched by segment rather than substring, so e.g. a future
  // "/management/reservations-archive" route can't be mistaken for
  // "/management/reservations".
  const pathSegments = pathname.split("/").filter(Boolean);
  const managementIndex = pathSegments.indexOf("management");
  const activeSegment =
    managementIndex >= 0 ? pathSegments[managementIndex + 1] : undefined;
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
      aria-label={t("management.nav.label")}
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
        let iconHandle: AnimatedIconHandle | null = null;

        return (
          <Link
            key={item.href}
            ref={(el) => {
              linkRefs.current[index] = el;
            }}
            href={item.href}
            aria-current={isActive ? "page" : undefined}
            title={item.label}
            onMouseEnter={() => iconHandle?.startAnimation()}
            onMouseLeave={() => iconHandle?.stopAnimation()}
            className={cn(
              "relative inline-flex items-center gap-2 whitespace-nowrap border-b-2 border-transparent px-2.5 py-2.5 text-sm font-medium transition-colors",
              isActive
                ? "text-foreground"
                : "hover:border-border hover:text-foreground",
            )}
          >
            {cloneElement(
              <item.icon size={16} className="shrink-0" /> as ReactElement<{
                ref?: Ref<AnimatedIconHandle>;
              }>,
              {
                ref: (handle: AnimatedIconHandle | null) => {
                  iconHandle = handle;
                },
              },
            )}
            <span className="hidden sm:inline">{item.label}</span>
          </Link>
        );
      })}
    </nav>
  );
}
