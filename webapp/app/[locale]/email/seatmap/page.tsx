"use client";

import { useEffect, useState, useRef } from "react";
import {
  useSearchParams,
  useParams,
  useRouter,
  usePathname,
} from "next/navigation";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  ZoomIn,
  ZoomOut,
  RotateCcw,
  Download,
  Loader2,
  Languages,
} from "lucide-react";
import { toast } from "@/hooks/use-toast";
import { getApiEmailSeatmap } from "@/api";
import { useT } from "@/lib/i18n/hooks";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

export default function EmailSeatmapPage() {
  const searchParams = useSearchParams();
  const pathname = usePathname();
  const token = searchParams.get("token");
  const params = useParams();
  const router = useRouter();
  const locale = params.locale as string;
  const t = useT();

  const [svgContent, setSvgContent] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [scale, setScale] = useState(() => {
    // Check if mobile device (screen width < 768px)
    if (typeof window !== "undefined") {
      return window.innerWidth < 768 ? 1.2 : 1.8;
    }
    return 2;
  });
  const [position, setPosition] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
  const [lastTouchDistance, setLastTouchDistance] = useState<number | null>(
    null,
  );

  const containerRef = useRef<HTMLDivElement>(null);
  const contentRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!token) {
      setError(t("emailSeatmap.error.noToken"));
      setIsLoading(false);
      return;
    }

    const fetchSeatmap = async () => {
      try {
        const response = await getApiEmailSeatmap({
          query: {
            token: token,
          },
          // Don't send credentials or cookies for this public endpoint
          credentials: "omit",
        });

        if (!response.data) {
          throw new Error(t("emailSeatmap.error.failedToLoad"));
        }

        // Convert Blob to SVG text
        const svg = await response.data.text();
        setSvgContent(svg);
      } catch (err) {
        const errorMessage =
          err instanceof Error
            ? err.message
            : t("emailSeatmap.error.failedToLoad");
        setError(errorMessage);
        toast({
          title: t("emailSeatmap.toast.errorTitle"),
          description: errorMessage,
          variant: "destructive",
        });
      } finally {
        setIsLoading(false);
      }
    };

    fetchSeatmap();
  }, [token, t]);

  const handleZoomIn = () => {
    setScale((prev) => Math.min(prev + 0.25, 5));
  };

  const handleZoomOut = () => {
    setScale((prev) => Math.max(prev - 0.25, 0.25));
  };

  const handleReset = () => {
    const isMobile = typeof window !== "undefined" && window.innerWidth < 768;
    setScale(isMobile ? 1 : 2);
    setPosition({ x: 0, y: 0 });
  };

  const handleDownload = () => {
    if (!svgContent) return;

    const blob = new Blob([svgContent], { type: "image/svg+xml" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "seatmap.svg";
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);

    toast({
      title: t("emailSeatmap.toast.downloadStarted"),
      description: t("emailSeatmap.toast.downloadDescription"),
    });
  };

  const handlePointerDown = (e: React.PointerEvent) => {
    if (e.button !== 0) return; // Only left mouse button
    const target = e.target as HTMLElement;
    target.setPointerCapture(e.pointerId);
    setIsDragging(true);
    setDragStart({ x: e.clientX - position.x, y: e.clientY - position.y });
  };

  const handlePointerMove = (e: React.PointerEvent) => {
    if (!isDragging) return;
    setPosition({
      x: e.clientX - dragStart.x,
      y: e.clientY - dragStart.y,
    });
  };

  const handlePointerUp = (e: React.PointerEvent) => {
    const target = e.target as HTMLElement;
    target.releasePointerCapture(e.pointerId);
    setIsDragging(false);
  };

  const handleWheel = (e: React.WheelEvent) => {
    e.preventDefault();
    const delta = e.deltaY > 0 ? -0.1 : 0.1;
    setScale((prev) => Math.max(0.25, Math.min(5, prev + delta)));
  };

  const handleLanguageChange = (newLocale: string) => {
    const segments = pathname.split("/");
    segments[1] = newLocale;
    const newPath = segments.join("/");
    const search = window.location.search;
    router.push(newPath + search);
  };

  const getTouchDistance = (touches: React.TouchList) => {
    if (touches.length < 2) return 0;
    const touch1 = touches[0];
    const touch2 = touches[1];
    return Math.sqrt(
      Math.pow(touch2.clientX - touch1.clientX, 2) +
        Math.pow(touch2.clientY - touch1.clientY, 2),
    );
  };

  const handleTouchStart = (e: React.TouchEvent) => {
    if (e.touches.length === 1) {
      // Single finger - start panning
      const touch = e.touches[0];
      setIsDragging(true);
      setDragStart({
        x: touch.clientX - position.x,
        y: touch.clientY - position.y,
      });
      setLastTouchDistance(null);
    } else if (e.touches.length === 2) {
      // Two fingers - start pinch zoom
      e.preventDefault(); // Prevent default zoom
      setIsDragging(false);
      setLastTouchDistance(getTouchDistance(e.touches));
    }
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    if (e.touches.length === 1 && isDragging) {
      const touch = e.touches[0];
      setPosition({
        x: touch.clientX - dragStart.x,
        y: touch.clientY - dragStart.y,
      });
    } else if (e.touches.length === 2 && lastTouchDistance) {
      e.preventDefault(); // Prevent default zoom/scroll
      const currentDistance = getTouchDistance(e.touches);
      const scaleChange = currentDistance / lastTouchDistance;

      if (Math.abs(scaleChange - 1.0) > 0.01) {
        setScale((prev) => Math.max(0.25, Math.min(5, prev * scaleChange)));
        setLastTouchDistance(currentDistance);
      }
    }
  };

  const handleTouchEnd = () => {
    setIsDragging(false);
    setLastTouchDistance(null);
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <Card className="w-full max-w-md mx-4">
          <CardContent className="pt-6">
            <div className="flex flex-col items-center gap-4">
              <Loader2 className="h-12 w-12 animate-spin text-primary" />
              <p className="text-lg font-medium">
                {t("emailSeatmap.loading.title")}
              </p>
              <p className="text-sm text-muted-foreground">
                {t("emailSeatmap.loading.subtitle")}
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (error || !svgContent) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background p-4">
        <Card className="w-full max-w-md">
          <CardHeader>
            <CardTitle className="text-destructive">
              {t("emailSeatmap.error.title")}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-muted-foreground mb-4">
              {error || t("emailSeatmap.error.failedToLoad")}
            </p>
            <p className="text-sm text-muted-foreground">
              {t("emailSeatmap.error.invalidToken")}
            </p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="border-b bg-card">
        <div className="container mx-auto px-4 py-4">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div>
              <h1 className="text-2xl font-bold">{t("emailSeatmap.title")}</h1>
              <p className="text-sm text-muted-foreground">
                {t("emailSeatmap.subtitle")}
              </p>
            </div>

            {/* Controls */}
            <div className="flex flex-wrap gap-2 items-center">
              {/* Language Switcher */}
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button
                    variant="outline"
                    size="icon"
                    title="Change language"
                    className="h-9 w-9"
                  >
                    <Languages className="h-4 w-4" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuItem
                    onClick={() => handleLanguageChange("en")}
                    className={locale === "en" ? "bg-accent" : ""}
                  >
                    English
                  </DropdownMenuItem>
                  <DropdownMenuItem
                    onClick={() => handleLanguageChange("de")}
                    className={locale === "de" ? "bg-accent" : ""}
                  >
                    Deutsch
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>

              <Button
                variant="outline"
                size="icon"
                onClick={handleZoomOut}
                disabled={scale <= 0.25}
                title={t("emailSeatmap.controls.zoomOut")}
              >
                <ZoomOut className="h-4 w-4" />
              </Button>

              <Button
                variant="outline"
                size="sm"
                onClick={handleReset}
                title={t("emailSeatmap.controls.reset")}
                className="min-w-[80px]"
              >
                <RotateCcw className="h-4 w-4 mr-2" />
                {Math.round(scale * 100)}%
              </Button>

              <Button
                variant="outline"
                size="icon"
                onClick={handleZoomIn}
                disabled={scale >= 5}
                title={t("emailSeatmap.controls.zoomIn")}
              >
                <ZoomIn className="h-4 w-4" />
              </Button>

              <Button
                variant="default"
                size="sm"
                onClick={handleDownload}
                title={t("emailSeatmap.controls.download")}
              >
                <Download className="h-4 w-4 mr-2" />
                <span className="hidden sm:inline">
                  {t("emailSeatmap.controls.download")}
                </span>
              </Button>
            </div>
          </div>
        </div>
      </header>

      {/* Legend */}
      <div className="border-b bg-card">
        <div className="container mx-auto px-4 py-3">
          <div className="flex flex-wrap gap-4 text-sm">
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 bg-[#2B7FFF] rounded" />
              <span>{t("emailSeatmap.legend.yourSeats")}</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 bg-[#F0B100] rounded" />
              <span>{t("emailSeatmap.legend.otherSeats")}</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 bg-green-500 rounded" />
              <span>{t("emailSeatmap.legend.availableSeats")}</span>
            </div>
          </div>
        </div>
      </div>

      {/* SVG Viewer */}
      <div
        ref={containerRef}
        className="relative flex-1 overflow-hidden"
        style={{ height: "calc(100vh - 180px)", touchAction: "none" }}
        onWheel={handleWheel}
      >
        <div
          ref={contentRef}
          className={`absolute inset-0 flex items-center justify-center ${
            isDragging ? "cursor-grabbing" : "cursor-grab"
          }`}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerLeave={handlePointerUp}
          onTouchStart={handleTouchStart}
          onTouchMove={handleTouchMove}
          onTouchEnd={handleTouchEnd}
        >
          <div
            style={{
              transform: `translate(${position.x}px, ${position.y}px) scale(${scale})`,
              transformOrigin: "center",
              transition: isDragging ? "none" : "transform 0.1s ease-out",
            }}
            className="select-none relative"
          >
            {/* SVG Content */}
            <div
              className="border-1 border-border rounded-lg"
              dangerouslySetInnerHTML={{ __html: svgContent }}
            />

            {/* Stage - Centered at top of SVG */}
            <div
              className="absolute left-1/2 -translate-x-1/2 border-2 border rounded-lg bg-card/90 backdrop-blur-sm"
              style={{
                width: "100px",
                height: "30px",
                top: "-35px",
              }}
            >
              <div className="w-full h-full flex items-center justify-center text-foreground text-md font-medium">
                {t("emailSeatmap.stage")}
              </div>
            </div>
          </div>
        </div>

        {/* Instructions Overlay */}
        <div className="absolute bottom-4 left-1/2 transform -translate-x-1/2 bg-card/95 backdrop-blur-sm border rounded-lg px-4 py-2 shadow-lg">
          <p className="text-xs text-muted-foreground text-center">
            <span className="hidden sm:inline">
              {t("emailSeatmap.instructions.desktop")}
            </span>
            <span className="sm:hidden">
              {t("emailSeatmap.instructions.mobile")}
            </span>
          </p>
        </div>
      </div>
    </div>
  );
}
