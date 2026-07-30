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

// Split so PageHeader only depends on the stable setter, not content — avoids a render loop.
const PageHeaderContentContext = createContext<PageHeaderContent | null>(null);
const PageHeaderSetterContext = createContext<SetPageHeaderContent | null>(
  null,
);

export function PageHeaderProvider({ children }: { children: ReactNode }) {
  const [content, setContent] = useState<PageHeaderContent | null>(null);

  return (
    <PageHeaderSetterContext.Provider value={setContent}>
      <PageHeaderContentContext.Provider value={content}>
        {children}
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
    throw new Error("usePageHeaderSetter must be used within a PageHeaderProvider");
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
    <div className="flex w-full flex-1 min-w-0 items-center gap-4">
      <div className="min-w-0 shrink-0">
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
        <div className="ml-auto w-full max-w-xs sm:max-w-sm">
          {content.search}
        </div>
      )}
    </div>
  );
}
