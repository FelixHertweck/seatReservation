"use client";

import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import Link from "next/link";

type PageHeaderContent = {
  title: ReactNode;
  description?: ReactNode;
  search?: ReactNode;
};

type SetPageHeaderContent = (content: PageHeaderContent | null) => void;
type SetPageHeaderNav = (nav: ReactNode | null) => void;

// Split so PageHeader only depends on the stable setter, not content — avoids a render loop.
const PageHeaderContentContext = createContext<PageHeaderContent | null>(null);
const PageHeaderSetterContext = createContext<SetPageHeaderContent | null>(
  null,
);

// A separate slot for a section-level sub-nav (e.g. the management tab
// strip), published by a layout rather than a page. Kept independent from
// PageHeaderContent so it isn't cleared every time a nested page's own
// <PageHeader> unmounts/remounts during navigation.
const PageHeaderNavContext = createContext<ReactNode | null>(null);
const PageHeaderNavSetterContext = createContext<SetPageHeaderNav | null>(null);

export function PageHeaderProvider({ children }: { children: ReactNode }) {
  const [content, setContent] = useState<PageHeaderContent | null>(null);
  const [nav, setNav] = useState<ReactNode | null>(null);

  return (
    <PageHeaderSetterContext.Provider value={setContent}>
      <PageHeaderContentContext.Provider value={content}>
        <PageHeaderNavSetterContext.Provider value={setNav}>
          <PageHeaderNavContext.Provider value={nav}>
            {children}
          </PageHeaderNavContext.Provider>
        </PageHeaderNavSetterContext.Provider>
      </PageHeaderContentContext.Provider>
    </PageHeaderSetterContext.Provider>
  );
}

export function usePageHeaderContent() {
  return useContext(PageHeaderContentContext);
}

function usePageHeaderSetter() {
  const setContent = useContext(PageHeaderSetterContext);
  if (!setContent) {
    throw new Error(
      "usePageHeaderSetter must be used within a PageHeaderProvider",
    );
  }
  return setContent;
}

// Lets a page publish its title/description/search bar into the shared
// layout header. Runs on every render (no dependency array) so the header
// always reflects the latest closures (e.g. an onSearch callback bound to
// page-local state), and clears itself on unmount.
export function PageHeader({
  title,
  description,
  search,
}: {
  title: ReactNode;
  description?: ReactNode;
  search?: ReactNode;
}) {
  const setContent = usePageHeaderSetter();

  useEffect(() => {
    setContent({ title, description, search });
    return () => setContent(null);
  });

  return null;
}

function usePageHeaderNavSetter() {
  const setNav = useContext(PageHeaderNavSetterContext);
  if (!setNav) {
    throw new Error(
      "usePageHeaderNavSetter must be used within a PageHeaderProvider",
    );
  }
  return setNav;
}

// Lets a section layout (not an individual page) publish a sub-nav into the
// shared header, e.g. the management tab strip. Runs once on mount/unmount
// rather than every render since it's owned by a layout that outlives the
// pages navigated between it.
export function PageHeaderNav({ children }: { children: ReactNode }) {
  const setNav = usePageHeaderNavSetter();

  useEffect(() => {
    setNav(children);
    return () => setNav(null);
  }, [children, setNav]);

  return null;
}

// Renders whatever the active layout published via <PageHeaderNav>, as a
// second row in the shared header. Null when no layout has published one.
export function PageHeaderNavSlot() {
  return useContext(PageHeaderNavContext);
}

// Renders in the shared layout header: whatever the active page published via
// <PageHeader>, or the app brand link when no page has published anything.
export function PageHeaderSlot() {
  const content = usePageHeaderContent();

  if (!content) {
    return (
      <div className="w-full flex-1">
        <h1 className="text-lg font-semibold md:text-xl">
          <Link href="/" className="bg-transparent">
            Seat Reservation
          </Link>
        </h1>
      </div>
    );
  }

  return (
    <div className="group relative flex w-full flex-1 min-w-0 items-center gap-4 max-sm:flex-wrap">
      <div className="min-w-0 flex-1 max-sm:pr-24 max-sm:group-has-[[data-placeholder]]:pr-0">
        <h1 className="truncate text-lg font-semibold md:text-xl">
          {content.title}
        </h1>
        {content.description && (
          <p className="hidden truncate text-xs text-muted-foreground sm:block">
            {content.description}
          </p>
        )}
      </div>
      {content.search && (
        <div
          className={
            // Default: a compact search-bar slot, icon-sized on mobile until
            // focused (used by e.g. SearchAndFilter's text input).
            "absolute right-0 top-1/2 z-20 w-10 -translate-y-1/2 overflow-hidden rounded-md transition-[width] duration-200 ease-out max-sm:right-2 max-sm:has-[button]:w-[5.75rem] max-sm:has-[input:focus]:w-56! max-sm:has-[input:focus]:bg-background " +
            // A select/combobox (e.g. EventSelector) can't shrink to an icon
            // and stay usable while it still shows its placeholder, so on
            // mobile it drops out of the absolute corner and becomes its own
            // full-width row below the title, where there's room to read it.
            // Once a value is picked, data-placeholder disappears and it
            // falls back to the compact has-[button] corner sizing above.
            "max-sm:has-[[data-placeholder]]:static max-sm:has-[[data-placeholder]]:top-auto max-sm:has-[[data-placeholder]]:z-auto max-sm:has-[[data-placeholder]]:order-3 max-sm:has-[[data-placeholder]]:mt-1 max-sm:has-[[data-placeholder]]:w-full max-sm:has-[[data-placeholder]]:translate-y-0 max-sm:has-[[data-placeholder]]:overflow-visible " +
            "sm:static sm:top-auto sm:z-auto sm:ml-auto sm:w-full sm:max-w-sm sm:translate-y-0 sm:overflow-visible sm:transition-none"
          }
        >
          {content.search}
        </div>
      )}
    </div>
  );
}
