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
  Eye,
  LogIn as CheckInIcon,
} from "lucide-react";
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
import { useProfileUnsavedChanges } from "@/hooks/use-profile-unsaved-changes";

export function AppSidebar() {
  const t = useT();

  const { user, logout, logoutAll } = useAuth();
  const { setOpen, setOpenMobile, isMobile } = useSidebar();
  const { theme, setTheme } = useTheme();
  const router = useRouter();
  const pathname = usePathname();

  const { hasUnsavedChanges, setPendingNavigation, setShowUnsavedDialog } =
    useProfileUnsavedChanges();

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

  useEffect(() => {
    if (isMobile) {
      setOpenMobile(false);
    }
  }, [pathname, isMobile, setOpenMobile]);

  const getMenuItems = () => {
    const baseItems: Array<{
      title: string;
      url: string;
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      icon: any;
      badge: string;
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      subItems?: Array<{ title: string; url: string; icon: any }>;
    }> = [];
    if (user?.roles?.includes("USER")) {
      baseItems.push({
        title: t("sidebar.events"),
        url: "/events",
        icon: CalendarDays,
        badge: "",
      });
      baseItems.push({
        title: t("sidebar.reservations"),
        url: "/reservations",
        icon: BookmarkCheck,
        badge: "",
      });
    }
    baseItems.push({
      title: t("sidebar.profile"),
      url: "/profile",
      icon: Settings,
      badge: "",
    });

    if (
      user?.roles?.includes("SUPERVISOR") ||
      user?.roles?.includes("MANAGER") ||
      user?.roles?.includes("ADMIN")
    ) {
      baseItems.push({
        title: t("sidebar.checkin"),
        url: "/checkin",
        icon: CheckInIcon,
        badge: t("sidebar.supervisor"),
      });
      baseItems.push({
        title: t("sidebar.liveview"),
        url: "/liveview",
        icon: Eye,
        badge: t("sidebar.supervisor"),
      });
    }

    if (user?.roles?.includes("MANAGER") || user?.roles?.includes("ADMIN")) {
      baseItems.push({
        title: t("sidebar.manager"),
        url: "/manager",
        icon: Users,
        badge: t("sidebar.manager"),
      });
    }

    if (user?.roles?.includes("ADMIN")) {
      baseItems.push({
        title: t("sidebar.userManagement"),
        url: "/admin",
        icon: Users,
        badge: t("sidebar.admin"),
      });
    }

    return baseItems;
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
    if (hasUnsavedChanges && pathname.includes("/profile")) {
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

  return (
    <Sidebar
      collapsible="offcanvas"
      className="border-r bg-linear-to-b from-sidebar-background to-sidebar-background/80 backdrop-blur-xs"
    >
      <SidebarMenu>
        <Link
          href="/"
          className={`w-full transition-all duration-500 flex items-center justify-center bg-transparent`}
        >
          <div className="relative w-full h-[100px] flex items-center justify-center">
            <Image
              src="/logo.png"
              alt="Logo"
              fill
              sizes="(max-width: 768px) 100vw, 300px"
              className="object-contain dark:invert"
              priority
            />
          </div>
        </Link>
      </SidebarMenu>
      <div className="border-b border-sidebar-border/50" />

      <SidebarContent className="px-2 py-1 ">
        <SidebarGroup>
          <SidebarGroupLabel className="text-base font-semibold text-sidebar-foreground/70 mb-2 px-2">
            {t("sidebar.navigation")}
          </SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu className="space-y-1">
              {getMenuItems().map((item, index) => (
                <SidebarMenuItem key={item.title}>
                  <SidebarMenuButton
                    asChild
                    tooltip={item.title}
                    className={`hover:bg-sidebar-accent/80 hover:text-sidebar-accent-foreground transition-all duration-300 hover:scale-[1.02] group relative overflow-hidden p-0 ${
                      pathname.includes(item.url)
                        ? "bg-sidebar-accent/40 text-sidebar-accent-foreground"
                        : ""
                    }`}
                    style={{
                      animationDelay: `${index * 100}ms`,
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
                      <div className="flex items-center gap-3 w-full px-3 py-2">
                        <div className="relative">
                          <item.icon
                            className={`group-hover:scale-110 group-hover:rotate-3 transition-all duration-300 ${
                              pathname.includes(item.url) ? "scale-110" : ""
                            }`}
                          />
                          <div
                            className={`absolute inset-0 bg-sidebar-primary/20 rounded-full scale-0 group-hover:scale-150 transition-transform duration-500 opacity-0 group-hover:opacity-100 ${
                              pathname.includes(item.url)
                                ? "scale-125 opacity-100"
                                : ""
                            }`}
                          />
                        </div>
                        <span
                          className={`font-medium ${
                            pathname.includes(item.url) ? "font-semibold" : ""
                          }`}
                        >
                          {item.title}
                        </span>
                        {item.badge && (
                          <Badge
                            variant="secondary"
                            className={`ml-auto text-xs bg-linear-to-r from-sidebar-primary/10 to-sidebar-accent/10 border-sidebar-primary/20 group-hover:scale-105 transition-transform duration-300 ${
                              pathname.includes(item.url) ? "scale-110" : ""
                            }`}
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
                            className="hover:bg-sidebar-accent/50 transition-all duration-300 group p-0"
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
                              className="flex items-center gap-3 w-full px-3 py-2"
                            >
                              <subItem.icon className="h-4 w-4 group-hover:scale-110 transition-transform duration-300" />
                              <span className="text-sm">{subItem.title}</span>
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
      </SidebarContent>

      <SidebarFooter className="border-t border-sidebar-border/50 bg-linear-to-r from-sidebar-background to-sidebar-accent/5 p-2">
        <SidebarMenu>
          <SidebarMenuItem>
            {user ? (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <SidebarMenuButton
                    size="lg"
                    className="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground hover:bg-sidebar-accent/50 transition-all duration-300 hover:scale-[1.02] group"
                  >
                    <Avatar className="h-8 w-8 rounded-lg ring-2 ring-sidebar-primary/20 group-hover:ring-sidebar-primary/40 transition-all duration-300 group-hover:scale-110">
                      <AvatarFallback className="rounded-lg bg-linear-to-br from-sidebar-primary to-sidebar-primary/80 text-sidebar-primary-foreground font-semibold group-hover:rotate-3 transition-transform duration-300">
                        {getUserInitials()}
                      </AvatarFallback>
                    </Avatar>
                    <div className="grid flex-1 text-left text-sm leading-tight">
                      <span className="truncate font-semibold group-hover:text-sidebar-accent-foreground transition-colors duration-300">
                        {user.firstname} {user.lastname}
                      </span>
                      <span className="truncate text-xs text-sidebar-foreground/60 group-hover:text-sidebar-foreground/80 transition-colors duration-300">
                        {user.email}
                      </span>
                    </div>
                    <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
                  </SidebarMenuButton>
                </DropdownMenuTrigger>
                <DropdownMenuContent
                  className="w-(--radix-dropdown-menu-trigger-width) min-w-56 rounded-lg shadow-xl border-sidebar-border/50 bg-sidebar-background/95 backdrop-blur-xs animate-in slide-in-from-bottom-2 duration-300"
                  side="bottom"
                  align="end"
                  sideOffset={4}
                >
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
                  <DropdownMenuItem
                    onClick={() => {
                      void logout();
                    }}
                    className="focus:bg-destructive/10 focus:text-destructive data-[highlighted]:bg-destructive/10 data-[highlighted]:text-destructive transition-all duration-200 cursor-pointer group"
                  >
                    <LogOut className="mr-2 h-4 w-4 group-hover:scale-110 transition-transform duration-200" />
                    {t("sidebar.logout")}
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            ) : (
              <SidebarMenuButton
                asChild
                tooltip={t("sidebar.login")}
                className="hover:bg-sidebar-accent/80 hover:text-sidebar-accent-foreground transition-all duration-300 hover:scale-[1.02] group"
              >
                <Link
                  href="/login"
                  onClick={handleLinkClick}
                  className="flex items-center gap-3"
                >
                  <LogIn className="group-hover:scale-110 group-hover:translate-x-1 transition-all duration-300" />
                  <span className="font-medium">{t("sidebar.login")}</span>
                </Link>
              </SidebarMenuButton>
            )}
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  );
}
