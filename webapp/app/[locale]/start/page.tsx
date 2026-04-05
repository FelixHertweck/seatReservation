import { Calendar, ArrowRight } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { createTranslation } from "@/lib/i18n/server";

// GitHub SVG Icon Component
const GithubIcon = ({ className = "h-8 w-8" }: { className?: string }) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width="24"
    height="24"
    viewBox="0 0 24 24"
    fill="currentColor"
    className={className}
  >
    <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v 3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z" />
  </svg>
);

export default async function StartPage({
  params,
}: {
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  const t = await createTranslation(locale);

  return (
    <div className="min-h-screen bg-linear-to-br from-background via-background to-accent/20 relative overflow-hidden">
      <div className="absolute inset-0 pointer-events-none">
        <svg
          className="absolute inset-0 h-full w-full md:object-cover object-center"
          viewBox="0 0 1200 800"
          preserveAspectRatio="xMidYMid slice"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
        >
          {/* Floating circles with bright, visible colors */}
          <circle
            cx="200"
            cy="150"
            r="60"
            fill="#3b82f6"
            fillOpacity="0.4"
            className="animate-pulse"
            style={{ animationDuration: "4s" }}
          />
          <circle
            cx="1000"
            cy="200"
            r="40"
            fill="#8b5cf6"
            fillOpacity="0.5"
            className="animate-pulse"
            style={{ animationDuration: "3s", animationDelay: "1s" }}
          />
          <circle
            cx="150"
            cy="600"
            r="80"
            fill="#06b6d4"
            fillOpacity="0.35"
            className="animate-pulse"
            style={{ animationDuration: "5s", animationDelay: "2s" }}
          />
          <circle
            cx="950"
            cy="500"
            r="50"
            fill="#f59e0b"
            fillOpacity="0.45"
            className="animate-pulse"
            style={{ animationDuration: "4.5s", animationDelay: "3s" }}
          />

          {/* Abstract geometric shapes with bright colors */}
          <path
            d="M800 100 L900 150 L850 250 L750 200 Z"
            fill="#10b981"
            fillOpacity="0.4"
            className="animate-pulse"
            style={{ animationDuration: "6s", animationDelay: "0.5s" }}
          />
          <path
            d="M300 400 L400 350 L450 450 L350 500 Z"
            fill="#ef4444"
            fillOpacity="0.35"
            className="animate-pulse"
            style={{ animationDuration: "4.5s", animationDelay: "1.5s" }}
          />
          <path
            d="M600 600 L700 550 L750 650 L650 700 Z"
            fill="#8b5cf6"
            fillOpacity="0.3"
            className="animate-pulse"
            style={{ animationDuration: "5.5s", animationDelay: "2.5s" }}
          />

          {/* Curved lines with bright colors */}
          <path
            d="M0 300 Q300 250 600 300 T1200 350"
            stroke="#3b82f6"
            strokeOpacity="0.6"
            strokeWidth="3"
            fill="none"
            className="animate-pulse"
            style={{ animationDuration: "8s" }}
          />
          <path
            d="M0 500 Q400 450 800 500 T1200 550"
            stroke="#f59e0b"
            strokeOpacity="0.5"
            strokeWidth="2"
            fill="none"
            className="animate-pulse"
            style={{ animationDuration: "7s", animationDelay: "2s" }}
          />
          <path
            d="M0 200 Q200 150 400 200 T800 250"
            stroke="#10b981"
            strokeOpacity="0.4"
            strokeWidth="2"
            fill="none"
            className="animate-pulse"
            style={{ animationDuration: "9s", animationDelay: "1s" }}
          />

          {/* Highly visible decorative dots */}
          <circle cx="500" cy="180" r="6" fill="#3b82f6" fillOpacity="0.7" />
          <circle cx="700" cy="320" r="5" fill="#f59e0b" fillOpacity="0.8" />
          <circle cx="900" cy="450" r="7" fill="#10b981" fillOpacity="0.6" />
          <circle cx="250" cy="350" r="5" fill="#8b5cf6" fillOpacity="0.75" />
          <circle cx="450" cy="500" r="4" fill="#ef4444" fillOpacity="0.7" />
          <circle cx="750" cy="150" r="6" fill="#06b6d4" fillOpacity="0.65" />

          {/* Enhanced grid pattern with better visibility */}
          <defs>
            <pattern
              id="grid"
              width="100"
              height="100"
              patternUnits="userSpaceOnUse"
            >
              <path
                d="M 100 0 L 0 0 0 100"
                fill="none"
                stroke="#3b82f6"
                strokeOpacity="0.15"
                strokeWidth="1"
              />
            </pattern>
          </defs>
          <rect width="100%" height="100%" fill="url(#grid)" />

          {/* Bright triangular elements */}
          <polygon
            points="400,250 420,280 380,280"
            fill="#f59e0b"
            fillOpacity="0.5"
            className="animate-pulse"
            style={{ animationDuration: "3.5s", animationDelay: "4s" }}
          />
          <polygon
            points="850,350 870,380 830,380"
            fill="#10b981"
            fillOpacity="0.45"
            className="animate-pulse"
            style={{ animationDuration: "4s", animationDelay: "2.5s" }}
          />
        </svg>
      </div>

      {/* Navigation */}
      <nav className="border-b bg-background/80 backdrop-blur-xs sticky top-0 z-50 relative">
        <div className="container mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="flex aspect-square size-8 items-center justify-center rounded-lg bg-linear-to-br from-primary to-primary/80 text-primary-foreground shadow-lg">
                <Calendar className="size-4" />
              </div>
              <span className="text-xl font-bold">
                {t("startPage.eventManagerTitle")}
              </span>
            </div>
            <div className="flex items-center gap-4">
              <Link href={`/${locale}/login`}>
                <Button variant="ghost">{t("startPage.signInButton")}</Button>
              </Link>
              <Link href={`/${locale}/register`}>
                <Button>{t("startPage.registerButton")}</Button>
              </Link>
            </div>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="container mx-auto px-4 py-20 text-center flex flex-col items-center justify-center min-h-[calc(100vh-140px)] relative z-10">
        <div className="max-w-3xl mx-auto animate-in fade-in slide-in-from-bottom duration-1000">
          <h1
            className="text-4xl md:text-6xl font-bold mb-6 bg-linear-to-r from-foreground to-foreground/70 bg-clip-text text-transparent animate-in slide-in-from-bottom duration-700"
            style={{ animationDelay: "300ms" }}
          >
            {t("startPage.heroTitle")}
          </h1>
          <p
            className="text-xl text-muted-foreground mb-8 max-w-2xl mx-auto animate-in slide-in-from-bottom duration-700"
            style={{ animationDelay: "400ms" }}
          >
            {t("startPage.heroDescription")}
          </p>
          <div
            className="flex flex-col sm:flex-row gap-4 justify-center animate-in slide-in-from-bottom duration-700"
            style={{ animationDelay: "500ms" }}
          >
            <Link href={`/${locale}/login`}>
              <Button
                size="lg"
                className="group hover:scale-105 transition-all duration-300"
              >
                {t("startPage.getStartedButton")}
                <ArrowRight className="ml-2 h-4 w-4 group-hover:translate-x-1 transition-transform duration-300" />
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t bg-accent/5 relative z-10">
        <div className="container mx-auto px-4 pt-12 pb-4">
          <div className="flex flex-col items-center justify-center text-center">
            <Link
              href="https://github.com/FelixHertweck/seatReservation"
              target="_blank"
              rel="noopener noreferrer"
              className="group flex items-center gap-4 p-6 rounded-xl bg-linear-to-r from-accent/10 to-accent/5 hover:from-accent/20 hover:to-accent/10 transition-all duration-300 hover:scale-105 border border-accent/20 hover:border-accent/40"
            >
              <div className="p-3 rounded-full bg-linear-to-br from-primary to-primary/80 text-primary-foreground shadow-lg group-hover:shadow-xl transition-shadow duration-300">
                <GithubIcon className="h-8 w-8" />
              </div>
              <div className="text-left">
                <h3 className="text-2xl font-bold text-foreground group-hover:text-primary transition-colors duration-300">
                  {t("startPage.githubProjectLink")}
                </h3>
                <p className="text-muted-foreground group-hover:text-foreground/80 transition-colors duration-300">
                  View source code and contribute
                </p>
              </div>
            </Link>
            <div className="flex gap-6 text-sm mt-4">
              <Link
                href={`/${locale}/legal-notice`}
                className="text-muted-foreground hover:text-foreground transition-colors"
              >
                {t("footer.legalNotice")}
              </Link>
              <Link
                href={`/${locale}/privacy`}
                className="text-muted-foreground hover:text-foreground transition-colors"
              >
                {t("footer.privacyPolicy")}
              </Link>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
