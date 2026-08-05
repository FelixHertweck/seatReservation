"use client";

import {
  CalendarDays,
  BookmarkCheck,
  Settings,
  Users,
  LogOut,
  LogIn,
  Sun,
  Moon,
  Monitor,
  Globe,
  UserLock,
  UserRound,
  ChevronRight,
  Eye,
  LogIn as CheckInIcon,
  LucideIcon,
  LayoutDashboard,
  MapPinned,
  Ticket,
} from "@/components/icons";
import Link from "next/link";
import Image from "next/image";
import { useRouter, usePathname } from "next/navigation";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarRail,
  useSidebar,
  SidebarMenuSub,
  SidebarMenuSubItem,
  SidebarMenuSubButton,
} from "@/components/ui/sidebar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/hooks/use-auth";
import { useEffect } from "react";
import { useTheme } from "next-themes";
import { useT } from "@/lib/i18n/hooks";
import { languages } from "@/lib/i18n/config";
import { useUnsavedChanges } from "@/hooks/use-unsaved-changes";

export function AppSidebar() {
  const t = useT();

  const { user, logout, logoutAll } = useAuth();
  const { state, setOpen, setOpenMobile, isMobile, openMobile } = useSidebar();
  const isCollapsed = state === "collapsed";
  const { theme, setTheme } = useTheme();
  const router = useRouter();
  const pathname = usePathname();

  const { hasUnsavedChanges, setPendingNavigation, setShowUnsavedDialog } =
    useUnsavedChanges();

  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth < 768) {
        setOpen(false);
      }
    };

    handleResize();

    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, [setOpen]);

  // If the sidebar is collapsed and the mobile overlay isn't open, show the small
  // favicon. Otherwise (sidebar expanded or mobile overlay open) show the
  // large logo.
  const showFavicon = isCollapsed && !openMobile;
  const showLargeLogo = !isCollapsed || openMobile;

  useEffect(() => {
    if (isMobile) {
      setOpenMobile(false);
    }
  }, [pathname, isMobile, setOpenMobile]);

  type MenuItem = {
    title: string;
    url: string;
    icon: LucideIcon;
    badge: string;
    subItems?: Array<{ title: string; url: string; icon: LucideIcon }>;
  };

  const getMenuGroups = () => {
    const generalItems: MenuItem[] = [];
    if (user?.roles?.includes("USER")) {
      generalItems.push({
        title: t("sidebar.events"),
        url: "/events",
        icon: CalendarDays,
        badge: "",
        subItems: [
          {
            title: t("sidebar.eventsBrowse"),
            url: "/events",
            icon: CalendarDays,
          },
          {
            title: t("sidebar.reservations"),
            url: "/events/reservations",
            icon: BookmarkCheck,
          },
        ],
      });
    }

    const supervisionItems: MenuItem[] = [];
    if (
      user?.roles?.includes("SUPERVISOR") ||
      user?.roles?.includes("MANAGER") ||
      user?.roles?.includes("ADMIN")
    ) {
      supervisionItems.push({
        title: t("sidebar.checkin"),
        url: "/checkin",
        icon: CheckInIcon,
        badge: t("sidebar.supervisor"),
      });
      supervisionItems.push({
        title: t("sidebar.liveview"),
        url: "/liveview",
        icon: Eye,
        badge: t("sidebar.supervisor"),
      });
    }

    const managementItems: MenuItem[] = [];
    if (user?.roles?.includes("MANAGER") || user?.roles?.includes("ADMIN")) {
      managementItems.push({
        title: t("sidebar.management"),
        url: "/management",
        icon: LayoutDashboard,
        badge: t("sidebar.manager"),
        subItems: [
          {
            title: t("sidebar.managementLocations"),
            url: "/management/locations",
            icon: MapPinned,
          },
          {
            title: t("sidebar.managementEvents"),
            url: "/management/events",
            icon: CalendarDays,
          },
          {
            title: t("sidebar.managementReservations"),
            url: "/management/reservations",
            icon: BookmarkCheck,
          },
          {
            title: t("sidebar.managementAllowances"),
            url: "/management/allowances",
            icon: Ticket,
          },
        ],
      });
    }

    if (user?.roles?.includes("ADMIN")) {
      managementItems.push({
        title: t("sidebar.userManagement"),
        url: "/admin",
        icon: Users,
        badge: t("sidebar.admin"),
      });
    }

    return [
      { label: t("sidebar.groupGeneral"), items: generalItems },
      { label: t("sidebar.groupSupervision"), items: supervisionItems },
      { label: t("sidebar.groupManagement"), items: managementItems },
    ].filter((group) => group.items.length > 0);
  };

  const getUserInitials = () => {
    if (!user?.firstname || !user?.lastname) return "U";
    return `${user.firstname[0]}${user.lastname[0]}`;
  };

  const getThemeIcon = (themeValue: string) => {
    switch (themeValue) {
      case "light":
        return Sun;
      case "dark":
        return Moon;
      case "system":
      default:
        return Monitor;
    }
  };

  const getThemeLabel = (themeValue: string) => {
    switch (themeValue) {
      case "light":
        return t("sidebar.light");
      case "dark":
        return t("sidebar.dark");
      case "system":
      default:
        return t("sidebar.system");
    }
  };

  const getCurrentLanguage = () => {
    const segments = pathname.split("/");
    return segments[1] || "en";
  };

  // pathname without the locale prefix, e.g. "/de/management/locations" -> "/management/locations"
  const currentPath = `/${pathname.split("/").slice(2).join("/")}`;

  const isPathActive = (url: string) =>
    currentPath === url || currentPath.startsWith(`${url}/`);

  // Sub-items are siblings, not a nested hierarchy, so an exact match avoids
  // e.g. "/events" (Browse) staying highlighted while on "/events/reservations".
  const isSubItemActive = (url: string) => currentPath === url;

  const getLanguageLabel = (lang: string) => {
    switch (lang) {
      case "en":
        return t("sidebar.english");
      case "de":
        return t("sidebar.german");
      default:
        return lang.toUpperCase();
    }
  };

  const switchLanguage = (newLang: string) => {
    const segments = pathname.split("/");
    segments[1] = newLang;
    const newPath = segments.join("/");
    router.push(newPath);
  };

  const handleNavigation = (url: string) => {
    if (hasUnsavedChanges) {
      setPendingNavigation(url);
      setShowUnsavedDialog(true);
      return;
    }

    // Proceed with normal navigation
    router.push(url);
    if (isMobile) {
      setOpen(false);
    }
  };

  const handleLinkClick = () => {
    if (isMobile) {
      setOpen(false);
    }
  };

  const renderSettingsMenuItems = () => (
    <>
      <div className="px-2 py-1.5 text-sm font-semibold text-sidebar-foreground">
        {t("sidebar.theme")}
      </div>
      {["light", "dark", "system"].map((themeOption) => {
        const ThemeIcon = getThemeIcon(themeOption);
        return (
          <DropdownMenuItem
            key={themeOption}
            onClick={() => setTheme(themeOption)}
            className={`hover:bg-sidebar-accent/50 transition-all duration-200 cursor-pointer group ${
              theme === themeOption ? "bg-sidebar-accent/30" : ""
            }`}
          >
            <ThemeIcon className="mr-2 h-4 w-4 group-hover:scale-110 transition-transform duration-200" />
            {getThemeLabel(themeOption)}
            {theme === themeOption && (
              <div className="ml-auto w-2 h-2 rounded-full bg-sidebar-primary animate-pulse" />
            )}
          </DropdownMenuItem>
        );
      })}
      <DropdownMenuSeparator />
      <div className="px-2 py-1.5 text-sm font-semibold text-sidebar-foreground">
        {t("sidebar.language")}
      </div>
      {languages.map((lang) => {
        const isCurrentLanguage = getCurrentLanguage() === lang;
        return (
          <DropdownMenuItem
            key={lang}
            onClick={() => switchLanguage(lang)}
            className={`hover:bg-sidebar-accent/50 transition-all duration-200 cursor-pointer group ${
              isCurrentLanguage ? "bg-sidebar-accent/30" : ""
            }`}
          >
            <Globe className="mr-2 h-4 w-4 group-hover:scale-110 transition-transform duration-200" />
            {getLanguageLabel(lang)}
            {isCurrentLanguage && (
              <div className="ml-auto w-2 h-2 rounded-full bg-sidebar-primary animate-pulse" />
            )}
          </DropdownMenuItem>
        );
      })}
      <DropdownMenuSeparator />
      <DropdownMenuItem
        onClick={() => {
          void logoutAll();
        }}
        className="focus:bg-destructive/10 focus:text-destructive data-[highlighted]:bg-destructive/10 data-[highlighted]:text-destructive transition-all duration-200 cursor-pointer group"
      >
        <UserLock className="mr-2 h-4 w-4 group-hover:scale-110 transition-transform duration-200" />
        {t("sidebar.logoutAll")}
      </DropdownMenuItem>
    </>
  );

  return (
    <Sidebar
      collapsible="icon"
      className="border-r bg-linear-to-b from-sidebar-background to-sidebar-background/80 backdrop-blur-xs"
      style={{
        width: isCollapsed ? "3rem" : "16rem",
        minWidth: isCollapsed ? "3rem" : "16rem",
        maxWidth: isCollapsed ? "3rem" : "16rem",
        height: "100vh",
        transition: "width 520ms cubic-bezier(0.2,0.9,0.2,1)",
        overflow: "hidden",
      }}
    >
      <SidebarMenu>
        <Link
          href="/"
          className={`w-full transition-all duration-500 flex items-center justify-center bg-transparent`}
        >
          <div
            className="relative w-full h-full flex items-center justify-center"
            style={{
              height: showLargeLogo ? (openMobile ? "82px" : "102px") : "59px",
              transition: "height 520ms cubic-bezier(0.2,0.9,0.2,1)",
            }}
          >
            {/* favicon (visible when collapsed and mobile overlay closed) */}
            <div
              aria-hidden={!showFavicon}
              style={{
                position: "absolute",
                inset: 0,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                transition:
                  "opacity 420ms ease, transform 520ms cubic-bezier(0.2,0.9,0.2,1)",
                opacity: showFavicon ? 1 : 0,
                transform: showFavicon
                  ? "scale(1) translateY(0)"
                  : "scale(0.9) translateY(-6px)",
                pointerEvents: "none",
              }}
            >
              <Image
                src="/favicon-32x32.png"
                alt="Logo"
                width={32}
                height={32}
                className="object-contain dark:invert"
                priority
              />
            </div>

            {/* large logo (visible when expanded or when mobile overlay open) */}
            <div
              aria-hidden={!showLargeLogo}
              style={{
                position: "absolute",
                inset: 0,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                transition:
                  "opacity 480ms cubic-bezier(0.2,0.9,0.2,1), transform 520ms",
                opacity: showLargeLogo ? 1 : 0,
                transform: showLargeLogo
                  ? "scale(1) translateY(0)"
                  : "scale(0.95) translateY(6px)",
              }}
            >
              <div className="relative w-full h-full flex items-center justify-center">
                <Image
                  src="/logo.png"
                  alt="Logo"
                  fill
                  sizes="(max-width: 768px) 100vw, 300px"
                  className="object-contain dark:invert"
                  priority
                />
              </div>
            </div>
          </div>
        </Link>
      </SidebarMenu>
      <div className="border-b border-sidebar-border/50" />

      <SidebarContent className="px-2 py-1 group-data-[collapsible=icon]:px-0 group-data-[collapsible=icon]:py-0">
        {getMenuGroups().map((group) => (
          <SidebarGroup key={group.label}>
            <SidebarGroupLabel className="text-base font-semibold text-sidebar-foreground/70 mb-2 px-2 group-data-[collapsible=icon]:hidden">
              {group.label}
            </SidebarGroupLabel>
            <SidebarGroupContent className="group-data-[collapsible=icon]:!p-0">
              <SidebarMenu className="space-y-1 group-data-[collapsible=icon]:space-y-1">
                {group.items.map((item, index) => (
                  <SidebarMenuItem key={item.title}>
                    <SidebarMenuButton
                      asChild
                      tooltip={item.title}
                      className={`hover:bg-sidebar-accent/80 hover:text-sidebar-accent-foreground transition-all duration-300 hover:scale-[1.02] group relative overflow-hidden p-0 group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:px-0 ${
                        isPathActive(item.url)
                          ? "bg-sidebar-accent/40 text-sidebar-accent-foreground"
                          : ""
                      }`}
                      style={{
                        animationDelay: `${index * 140}ms`,
                      }}
                    >
                      <Link
                        href={item.url}
                        onClick={(e) => {
                          if (e.button === 0 && !e.metaKey && !e.ctrlKey) {
                            e.preventDefault();
                            handleNavigation(item.url);
                          }
                        }}
                        onContextMenu={(e) => {
                          e.stopPropagation();
                        }}
                      >
                        <div className="flex items-center gap-3 w-full px-3 py-2 group-data-[collapsible=icon]:gap-0 group-data-[collapsible=icon]:px-0 group-data-[collapsible=icon]:py-3 group-data-[collapsible=icon]:justify-center">
                          <div className="relative">
                            <item.icon
                              className={`group-hover:scale-110 group-hover:rotate-3 transition-all duration-300 ${
                                isPathActive(item.url) ? "scale-110" : ""
                              }`}
                            />
                            <div
                              className={`absolute inset-0 bg-sidebar-primary/20 rounded-full scale-0 group-hover:scale-150 transition-transform duration-500 opacity-0 group-hover:opacity-100 ${
                                isPathActive(item.url)
                                  ? "scale-125 opacity-100"
                                  : ""
                              }`}
                            />
                          </div>
                          <span
                            className={`font-medium ${
                              isPathActive(item.url) ? "font-semibold" : ""
                            } group-data-[collapsible=icon]:hidden`}
                          >
                            {item.title}
                          </span>
                          {item.badge && (
                            <Badge
                              variant="secondary"
                              className={`ml-auto text-xs bg-linear-to-r from-sidebar-primary/10 to-sidebar-accent/10 border-sidebar-primary/20 group-hover:scale-105 transition-transform duration-300 ${
                                isPathActive(item.url) ? "scale-110" : ""
                              } group-data-[collapsible=icon]:hidden`}
                            >
                              {item.badge}
                            </Badge>
                          )}
                          <div className="absolute inset-0 bg-linear-to-r from-transparent via-sidebar-primary/5 to-transparent -translate-x-full group-hover:translate-x-full transition-transform duration-700" />
                        </div>
                      </Link>
                    </SidebarMenuButton>
                    {item.subItems && item.subItems.length > 0 && (
                      <SidebarMenuSub className="ml-0 border-l border-sidebar-border/50 ml-4">
                        {item.subItems.map((subItem) => (
                          <SidebarMenuSubItem key={subItem.title}>
                            <SidebarMenuSubButton
                              asChild
                              className={`hover:bg-sidebar-accent/50 transition-all duration-300 group p-0 ${
                                isSubItemActive(subItem.url)
                                  ? "bg-sidebar-accent/40 text-sidebar-accent-foreground"
                                  : ""
                              }`}
                            >
                              <Link
                                href={subItem.url}
                                onClick={(e) => {
                                  if (
                                    e.button === 0 &&
                                    !e.metaKey &&
                                    !e.ctrlKey
                                  ) {
                                    e.preventDefault();
                                    handleNavigation(subItem.url);
                                  }
                                }}
                                onContextMenu={(e) => {
                                  e.stopPropagation();
                                }}
                                className="flex items-center gap-3 w-full px-3 py-2 group-data-[collapsible=icon]:gap-0 group-data-[collapsible=icon]:px-0 group-data-[collapsible=icon]:justify-center"
                              >
                                <subItem.icon
                                  className={`h-4 w-4 group-hover:scale-110 transition-transform duration-300 ${
                                    isSubItemActive(subItem.url)
                                      ? "scale-110"
                                      : ""
                                  }`}
                                />
                                <span
                                  className={`text-sm group-data-[collapsible=icon]:hidden ${
                                    isSubItemActive(subItem.url)
                                      ? "font-semibold"
                                      : ""
                                  }`}
                                >
                                  {subItem.title}
                                </span>
                              </Link>
                            </SidebarMenuSubButton>
                          </SidebarMenuSubItem>
                        ))}
                      </SidebarMenuSub>
                    )}
                  </SidebarMenuItem>
                ))}
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        ))}
      </SidebarContent>

      <SidebarFooter className="border-t border-sidebar-border/50 bg-linear-to-r from-sidebar-background to-sidebar-accent/5 p-2 group-data-[collapsible=icon]:p-0">
        {user ? (
          showLargeLogo ? (
            <div className="rounded-xl border border-sidebar-border/50 bg-sidebar-accent/5 overflow-hidden">
              <div className="flex items-center gap-3 px-3 pt-3 pb-2">
                <Avatar className="h-8 w-8 rounded-lg ring-2 ring-sidebar-primary/20">
                  <AvatarFallback className="rounded-lg bg-linear-to-br from-sidebar-primary to-sidebar-primary/80 text-sidebar-primary-foreground font-semibold">
                    {getUserInitials()}
                  </AvatarFallback>
                </Avatar>
                <div className="grid flex-1 min-w-0 text-left text-sm leading-tight">
                  <span className="truncate font-semibold">
                    {user.firstname} {user.lastname}
                  </span>
                  <span className="truncate text-xs text-sidebar-foreground/60">
                    {user.email}
                  </span>
                </div>
              </div>

              <button
                type="button"
                onClick={() => handleNavigation("/profile")}
                className="flex w-full items-center gap-2 px-3 py-2 text-sm font-medium hover:bg-sidebar-accent/50 transition-colors duration-200"
              >
                <UserRound className="h-4 w-4 text-sidebar-primary" />
                {t("sidebar.viewProfile")}
                <ChevronRight className="ml-auto h-4 w-4 text-sidebar-foreground/50" />
              </button>

              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <button
                    type="button"
                    className="flex w-full items-center gap-2 border-t border-sidebar-border/50 px-3 py-2 text-sm font-medium hover:bg-sidebar-accent/50 transition-colors duration-200"
                  >
                    <Settings className="h-4 w-4 text-sidebar-primary" />
                    {t("sidebar.settings")}
                    <ChevronRight className="ml-auto h-4 w-4 text-sidebar-foreground/50" />
                  </button>
                </DropdownMenuTrigger>
                <DropdownMenuContent
                  className="min-w-56 rounded-lg shadow-xl border-sidebar-border/50 bg-sidebar animate-in slide-in-from-bottom-2 duration-300"
                  side={isMobile ? "top" : "right"}
                  align={isMobile ? "center" : "end"}
                  sideOffset={8}
                >
                  {renderSettingsMenuItems()}
                </DropdownMenuContent>
              </DropdownMenu>

              <button
                type="button"
                onClick={() => {
                  void logout();
                }}
                className="flex w-full items-center gap-2 border-t border-sidebar-border/50 px-3 py-2 text-sm font-medium text-destructive hover:bg-destructive/10 transition-colors duration-200"
              >
                <LogOut className="h-4 w-4" />
                {t("sidebar.logout")}
              </button>
            </div>
          ) : (
            <SidebarMenu className="gap-1">
              <SidebarMenuItem>
                <SidebarMenuButton
                  size="lg"
                  className="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground hover:bg-sidebar-accent/50 transition-all duration-300 hover:scale-[1.02] group group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:items-center group-data-[collapsible=icon]:px-0 group-data-[collapsible=icon]:mx-auto"
                >
                  <Avatar className="h-6 w-6 rounded-lg ring-2 ring-sidebar-primary/20 group-hover:ring-sidebar-primary/40 transition-all duration-300 group-hover:scale-110">
                    <AvatarFallback className="rounded-lg bg-linear-to-br from-sidebar-primary to-sidebar-primary/80 text-sidebar-primary-foreground font-semibold group-hover:rotate-3 transition-transform duration-300">
                      {getUserInitials()}
                    </AvatarFallback>
                  </Avatar>
                </SidebarMenuButton>
              </SidebarMenuItem>
              <SidebarMenuItem>
                <SidebarMenuButton
                  tooltip={t("sidebar.viewProfile")}
                  onClick={() => handleNavigation("/profile")}
                  className="hover:bg-sidebar-accent/50 transition-all duration-300 hover:scale-[1.02] group group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:items-center group-data-[collapsible=icon]:px-0 group-data-[collapsible=icon]:mx-auto"
                >
                  <UserRound className="group-hover:scale-110 transition-transform duration-300" />
                </SidebarMenuButton>
              </SidebarMenuItem>

              <SidebarMenuItem>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <SidebarMenuButton
                      tooltip={t("sidebar.settings")}
                      onClick={() => renderSettingsMenuItems()}
                      className="hover:bg-sidebar-accent/50 transition-all duration-300 hover:scale-[1.02] group group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:items-center group-data-[collapsible=icon]:px-0 group-data-[collapsible=icon]:mx-auto"
                    >
                      <Settings className="group-hover:scale-110 transition-transform duration-300" />
                    </SidebarMenuButton>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent
                    className="w-56 rounded-lg shadow-xl border-sidebar-border/50 bg-sidebar animate-in slide-in-from-bottom-2 duration-300"
                    side="right"
                    align="end"
                    sideOffset={4}
                  >
                    {renderSettingsMenuItems()}
                  </DropdownMenuContent>
                </DropdownMenu>
              </SidebarMenuItem>

              <SidebarMenuItem>
                <SidebarMenuButton
                  tooltip={t("sidebar.logout")}
                  onClick={() => {
                    void logout();
                  }}
                  className="text-destructive hover:bg-destructive/10 hover:text-destructive transition-all duration-300 hover:scale-[1.02] group group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:items-center group-data-[collapsible=icon]:px-0 group-data-[collapsible=icon]:mx-auto"
                >
                  <LogOut className="group-hover:scale-110 transition-transform duration-300" />
                </SidebarMenuButton>
              </SidebarMenuItem>
            </SidebarMenu>
          )
        ) : (
          <SidebarMenu>
            <SidebarMenuItem>
              <SidebarMenuButton
                asChild
                tooltip={t("sidebar.login")}
                className="hover:bg-sidebar-accent/80 hover:text-sidebar-accent-foreground transition-all duration-300 hover:scale-[1.02] group"
              >
                <Link
                  href="/login"
                  onClick={handleLinkClick}
                  className="flex items-center gap-3 group-data-[collapsible=icon]:justify-center"
                >
                  <LogIn className="group-hover:scale-110 group-hover:translate-x-1 transition-all duration-300" />
                  <span className="font-medium group-data-[collapsible=icon]:hidden">
                    {t("sidebar.login")}
                  </span>
                </Link>
              </SidebarMenuButton>
            </SidebarMenuItem>
          </SidebarMenu>
        )}
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  );
}
