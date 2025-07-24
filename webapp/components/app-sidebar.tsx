"use client";

import {
  Calendar,
  Settings,
  Users,
  UserCheck,
  LogOut,
  LogIn,
  Sparkles,
  Sun,
  Moon,
  Monitor,
} from "lucide-react";
import Link from "next/link";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
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
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/hooks/use-auth";
import { useEffect } from "react";
import { useTheme } from "next-themes";
import { useRouter } from "next/navigation";

export function AppSidebar() {
  const { user, logout } = useAuth();
  const { setOpen, isMobile } = useSidebar();
  const { theme, setTheme } = useTheme();
  const router = useRouter();

  // Auto-close sidebar on mobile when screen becomes small
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth < 768) {
        // md breakpoint
        setOpen(false);
      }
    };

    // Initial check
    handleResize();

    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, [setOpen]);

  const getMenuItems = () => {
    const baseItems = [
      { title: "Events", url: "/events", icon: Calendar, badge: "" },
      {
        title: "My Reservations",
        url: "/reservations",
        icon: UserCheck,
        badge: "",
      },
      { title: "Profile", url: "/profile", icon: Settings, badge: "" },
    ];

    if (user?.roles?.includes("MANAGER")) {
      baseItems.push({
        title: "Manager",
        url: "/manager",
        icon: Users,
        badge: "Manager",
      });
    }

    if (user?.roles?.includes("ADMIN")) {
      baseItems.push({
        title: "User Management",
        url: "/admin",
        icon: Users,
        badge: "Admin",
      });
    }

    return baseItems;
  };

  const getUserInitials = () => {
    if (!user?.firstName || !user?.lastName) return "U";
    return `${user.firstName[0]}${user.lastName[0]}`;
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
        return "Light";
      case "dark":
        return "Dark";
      case "system":
      default:
        return "System";
    }
  };

  const handleLinkClick = () => {
    // Close sidebar on mobile when a link is clicked
    if (isMobile) {
      setOpen(false);
    }
  };

  return (
    <Sidebar
      collapsible="offcanvas"
      className="border-r bg-gradient-to-b from-sidebar-background to-sidebar-background/80 backdrop-blur-sm"
    >
      <SidebarHeader className="border-b border-sidebar-border/50 bg-gradient-to-r from-sidebar-primary/5 to-sidebar-accent/5">
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton
              size="lg"
              className="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground hover:bg-sidebar-accent/50 transition-all duration-300 hover:scale-[1.02] group"
              onClick={() => {
                router.push("/");
              }}
            >
              <div className="flex aspect-square size-8 items-center justify-center rounded-lg bg-gradient-to-br from-sidebar-primary to-sidebar-primary/80 text-sidebar-primary-foreground shadow-lg group-hover:shadow-xl transition-all duration-300 group-hover:scale-110">
                <Calendar className="size-4 group-hover:rotate-12 transition-transform duration-300" />
              </div>
              <div className="grid flex-1 text-left text-sm leading-tight">
                <span className="truncate font-semibold bg-gradient-to-r from-sidebar-foreground to-sidebar-foreground/80 bg-clip-text">
                  Event Manager
                </span>
                <span className="truncate text-xs text-sidebar-foreground/60">
                  Management System
                </span>
              </div>
              <Sparkles className="size-3 text-sidebar-primary opacity-60 group-hover:opacity-100 transition-opacity duration-300" />
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>

      <SidebarContent className="px-2 py-4">
        <SidebarGroup>
          <SidebarGroupLabel className="text-xs font-semibold text-sidebar-foreground/70 mb-2 px-2">
            Navigation
          </SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu className="space-y-1">
              {getMenuItems().map((item, index) => (
                <SidebarMenuItem key={item.title}>
                  <SidebarMenuButton
                    asChild
                    tooltip={item.title}
                    className="hover:bg-sidebar-accent/80 hover:text-sidebar-accent-foreground transition-all duration-300 hover:scale-[1.02] hover:translate-x-1 group relative overflow-hidden"
                    style={{
                      animationDelay: `${index * 100}ms`,
                    }}
                  >
                    <Link
                      href={item.url}
                      onClick={handleLinkClick}
                      className="flex items-center gap-3"
                    >
                      <div className="relative">
                        <item.icon className="group-hover:scale-110 group-hover:rotate-3 transition-all duration-300" />
                        <div className="absolute inset-0 bg-sidebar-primary/20 rounded-full scale-0 group-hover:scale-150 transition-transform duration-500 opacity-0 group-hover:opacity-100" />
                      </div>
                      <span className="font-medium">{item.title}</span>
                      {item.badge && (
                        <Badge
                          variant="secondary"
                          className="ml-auto text-xs bg-gradient-to-r from-sidebar-primary/10 to-sidebar-accent/10 border-sidebar-primary/20 group-hover:scale-105 transition-transform duration-300"
                        >
                          {item.badge}
                        </Badge>
                      )}
                      <div className="absolute inset-0 bg-gradient-to-r from-transparent via-sidebar-primary/5 to-transparent translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-700" />
                    </Link>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>

      <SidebarFooter className="border-t border-sidebar-border/50 bg-gradient-to-r from-sidebar-background to-sidebar-accent/5 p-2">
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
                      <AvatarFallback className="rounded-lg bg-gradient-to-br from-sidebar-primary to-sidebar-primary/80 text-sidebar-primary-foreground font-semibold group-hover:rotate-3 transition-transform duration-300">
                        {getUserInitials()}
                      </AvatarFallback>
                    </Avatar>
                    <div className="grid flex-1 text-left text-sm leading-tight">
                      <span className="truncate font-semibold group-hover:text-sidebar-accent-foreground transition-colors duration-300">
                        {user.firstName} {user.lastName}
                      </span>
                      <span className="truncate text-xs text-sidebar-foreground/60 group-hover:text-sidebar-foreground/80 transition-colors duration-300">
                        {user.email}
                      </span>
                    </div>
                    <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
                  </SidebarMenuButton>
                </DropdownMenuTrigger>
                <DropdownMenuContent
                  className="w-[--radix-dropdown-menu-trigger-width] min-w-56 rounded-lg shadow-xl border-sidebar-border/50 bg-sidebar-background/95 backdrop-blur-sm animate-in slide-in-from-bottom-2 duration-300"
                  side="bottom"
                  align="end"
                  sideOffset={4}
                >
                  <div className="px-2 py-1.5 text-sm font-semibold text-sidebar-foreground">
                    Theme
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
                  <DropdownMenuItem
                    onClick={() => {
                      void logout();
                    }}
                    className="hover:bg-destructive/10 hover:text-destructive transition-all duration-200 cursor-pointer group"
                  >
                    <LogOut className="mr-2 h-4 w-4 group-hover:scale-110 transition-transform duration-200" />
                    Logout
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            ) : (
              <SidebarMenuButton
                asChild
                tooltip="Login"
                className="hover:bg-sidebar-accent/80 hover:text-sidebar-accent-foreground transition-all duration-300 hover:scale-[1.02] group"
              >
                <Link
                  href="/login"
                  onClick={handleLinkClick}
                  className="flex items-center gap-3"
                >
                  <LogIn className="group-hover:scale-110 group-hover:translate-x-1 transition-all duration-300" />
                  <span className="font-medium">Login</span>
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
