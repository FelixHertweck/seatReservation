"use client";

import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import Link from "next/link";
import { NotificationBell } from "@/components/notifications/notification-bell";

type PageHeaderContent = {
  title: ReactNode;
  description?: ReactNode;
  actions?: ReactNode;
  search?: ReactNode;
};

type SetPageHeaderContent = (content: PageHeaderContent | null) => void;
type SetPageHeaderNav = (nav: ReactNode | null) => void;

const PageHeaderContentContext = createContext<PageHeaderContent | null>(null);
const PageHeaderSetterContext = createContext<SetPageHeaderContent | null>(
  null,
);

const PageHeaderNavContext = createContext<ReactNode | null>(null);
const PageHeaderNavSetterContext = createContext<SetPageHeaderNav | null>(null);

export function PageHeaderProvider({
  children,
}: Readonly<{ children: ReactNode }>) {
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

export function PageHeader({
  title,
  description,
  actions,
  search,
}: Readonly<{
  title: ReactNode;
  description?: ReactNode;
  actions?: ReactNode;
  search?: ReactNode;
}>) {
  const setContent = usePageHeaderSetter();

  useEffect(() => {
    setContent({ title, description, actions, search });
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

export function PageHeaderNav({ children }: Readonly<{ children: ReactNode }>) {
  const setNav = usePageHeaderNavSetter();

  useEffect(() => {
    setNav(children);
    return () => setNav(null);
  }, [children, setNav]);

  return null;
}

export function PageHeaderNavSlot() {
  return useContext(PageHeaderNavContext);
}

export function PageHeaderSlot() {
  const content = usePageHeaderContent();

  if (!content) {
    return (
      <div className="flex w-full flex-1 items-center justify-between">
        <h1 className="text-lg font-semibold md:text-xl">
          <Link href="/" className="bg-transparent">
            Seat Reservation
          </Link>
        </h1>
        <NotificationBell />
      </div>
    );
  }

  const hasControls = Boolean(content.search || content.actions);

  return (
    <div className="flex w-full flex-1 min-w-0 flex-wrap items-center gap-2 sm:gap-4">
      <div className={`min-w-0 flex-1 ${hasControls ? "max-sm:w-full" : ""}`}>
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
            "order-2 flex shrink-0 items-center overflow-hidden rounded-md transition-[width] duration-200 ease-out max-sm:w-10 max-sm:has-[button]:w-[5.75rem] max-sm:has-[input:focus]:w-56! max-sm:has-[input:focus]:bg-background " +
            "max-sm:has-[[data-placeholder]]:order-1 max-sm:has-[[data-placeholder]]:w-full max-sm:has-[[data-placeholder]]:overflow-visible " +
            "sm:order-none sm:ml-auto sm:w-full sm:max-w-sm sm:overflow-visible sm:transition-none"
          }
        >
          {content.search}
        </div>
      )}
      <div
        className={
          "order-3 flex shrink-0 items-center gap-2 max-sm:ml-auto sm:order-none sm:flex-nowrap" +
          (!content.search ? " sm:ml-auto" : "")
        }
      >
        {content.actions}
        <NotificationBell />
      </div>
    </div>
  );
}
