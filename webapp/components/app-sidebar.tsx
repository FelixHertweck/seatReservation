"use client";

import {
  Calendar,
  Settings,
  Users,
  LogOut,
  LogIn,
  Sun,
  Moon,
  Monitor,
  Globe,
} from "lucide-react";
import Link from "next/link";
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
} from "@/components/ui/sidebar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/hooks/use-auth";
import { useEffect, useState } from "react";
import { useTheme } from "next-themes";
import { useT } from "@/lib/i18n/hooks";
import { languages } from "@/lib/i18n/config";

export function AppSidebar() {
  const t = useT();

  const { user, logout } = useAuth();
  const { setOpen, isMobile } = useSidebar();
  const { theme, setTheme, resolvedTheme } = useTheme();
  const router = useRouter();
  const pathname = usePathname();

  const [showUnsavedDialog, setShowUnsavedDialog] = useState(false);
  const [pendingNavigation, setPendingNavigation] = useState<string | null>(
    null,
  );

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

  const getMenuItems = () => {
    const baseItems = [
      { title: t("sidebar.events"), url: "/events", icon: Calendar, badge: "" },
      {
        title: t("sidebar.profile"),
        url: "/profile",
        icon: Settings,
        badge: "",
      },
    ];

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

  const checkUnsavedChanges = () => {
    return (window as any).__profileHasUnsavedChanges || false;
  };

  const handleNavigation = (url: string) => {
    const hasUnsavedChanges = checkUnsavedChanges();

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

  const handleDiscardChanges = () => {
    if (pendingNavigation) {
      router.push(pendingNavigation);
      if (isMobile) {
        setOpen(false);
      }
    }
    setShowUnsavedDialog(false);
    setPendingNavigation(null);
  };

  const handleSaveAndNavigate = () => {
    // Trigger form submission on profile page
    const form = document.querySelector("form") as HTMLFormElement;
    if (form) {
      form.requestSubmit();
      // Wait a bit for the form to submit, then navigate
      setTimeout(() => {
        if (pendingNavigation) {
          router.push(pendingNavigation);
          if (isMobile) {
            setOpen(false);
          }
        }
        setShowUnsavedDialog(false);
        setPendingNavigation(null);
      }, 500);
    }
  };

  const handleLinkClick = () => {
    if (isMobile) {
      setOpen(false);
    }
  };

  return (
    <>
      <Sidebar
        collapsible="offcanvas"
        className="border-r bg-linear-to-b from-sidebar-background to-sidebar-background/80 backdrop-blur-xs"
      >
        <SidebarMenu>
          <Link
            href="/"
            className={`w-full transition-all duration-500 flex items-center justify-center bg-transparent`}
          >
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src="/logo.png"
              alt="Logo"
              className={`h-auto w-full max-h-[100px] bg-transparent object-contain ${resolvedTheme === "dark" ? "invert" : ""}`}
            />
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
                      tooltip={item.title}
                      className="hover:bg-sidebar-accent/80 hover:text-sidebar-accent-foreground transition-all duration-300 hover:scale-[1.02] group relative overflow-hidden"
                      style={{
                        animationDelay: `${index * 100}ms`,
                      }}
                      onClick={() => handleNavigation(item.url)}
                    >
                      <div className="flex items-center gap-3 w-full">
                        <div className="relative">
                          <item.icon className="group-hover:scale-110 group-hover:rotate-3 transition-all duration-300" />
                          <div className="absolute inset-0 bg-sidebar-primary/20 rounded-full scale-0 group-hover:scale-150 transition-transform duration-500 opacity-0 group-hover:opacity-100" />
                        </div>
                        <span className="font-medium">{item.title}</span>
                        {item.badge && (
                          <Badge
                            variant="secondary"
                            className="ml-auto text-xs bg-linear-to-r from-sidebar-primary/10 to-sidebar-accent/10 border-sidebar-primary/20 group-hover:scale-105 transition-transform duration-300"
                          >
                            {item.badge}
                          </Badge>
                        )}
                        <div className="absolute inset-0 bg-linear-to-r from-transparent via-sidebar-primary/5 to-transparent -translate-x-full group-hover:translate-x-full transition-transform duration-700" />
                      </div>
                    </SidebarMenuButton>
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
                        void logout();
                      }}
                      className="hover:bg-destructive/10 hover:text-destructive transition-all duration-200 cursor-pointer group"
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

      <AlertDialog open={showUnsavedDialog} onOpenChange={setShowUnsavedDialog}>
        <AlertDialogContent className="border-border shadow-lg">
          <AlertDialogHeader>
            <AlertDialogTitle className="text-foreground">
              {t("sidebar.unsavedChangesTitle")}
            </AlertDialogTitle>
            <AlertDialogDescription className="text-muted-foreground">
              {t("sidebar.unsavedChangesDescription")}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel
              onClick={() => setShowUnsavedDialog(false)}
              className="bg-secondary text-secondary-foreground hover:bg-secondary/80"
            >
              {t("sidebar.cancel")}
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDiscardChanges}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {t("sidebar.discardChanges")}
            </AlertDialogAction>
            <AlertDialogAction
              onClick={handleSaveAndNavigate}
              className="bg-primary text-primary-foreground hover:bg-primary/90"
            >
              {t("sidebar.saveAndContinue")}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
