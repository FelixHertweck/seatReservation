import { Calendar, Github, ArrowRight } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { createTranslation } from "@/lib/i18n/server";

export default async function StartPage({
  params,
}: {
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  const t = await createTranslation(locale);

  return (
    <div className="min-h-screen bg-gradient-to-br from-background via-background to-accent/20">
      {/* Navigation */}
      <nav className="border-b bg-background/80 backdrop-blur-sm sticky top-0 z-50">
        <div className="container mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="flex aspect-square size-8 items-center justify-center rounded-lg bg-gradient-to-br from-primary to-primary/80 text-primary-foreground shadow-lg">
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
                <Button>{t("startPage.getStartedButton")}</Button>
              </Link>
            </div>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="container mx-auto px-4 py-20 text-center flex flex-col items-center justify-center min-h-[calc(100vh-140px)]">
        <div className="max-w-3xl mx-auto animate-in fade-in slide-in-from-bottom duration-1000">
          <h1
            className="text-4xl md:text-6xl font-bold mb-6 bg-gradient-to-r from-foreground to-foreground/70 bg-clip-text text-transparent animate-in slide-in-from-bottom duration-700"
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
            <Link href={`/${locale}/register`}>
              <Button
                size="lg"
                className="group hover:scale-105 transition-all duration-300"
              >
                {t("startPage.getStartedButton")}
                <ArrowRight className="ml-2 h-4 w-4 group-hover:translate-x-1 transition-transform duration-300" />
              </Button>
            </Link>
            <Link href={`/${locale}/login`}>
              <Button
                variant="outline"
                size="lg"
                className="hover:scale-105 transition-all duration-300 bg-transparent"
              >
                {t("startPage.signInButton")}
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t bg-accent/5">
        <div className="container mx-auto px-4 py-12">
          <div className="grid md:grid-cols-4 gap-8">
            <div className="col-span-2">
              <div className="flex items-center gap-2 mb-4">
                <div className="flex aspect-square size-8 items-center justify-center rounded-lg bg-gradient-to-br from-primary to-primary/80 text-primary-foreground shadow-lg">
                  <Calendar className="size-4" />
                </div>
                <span className="text-xl font-bold">
                  {t("startPage.eventManagerTitle")}
                </span>
              </div>
              <p className="text-muted-foreground mb-4 max-w-md">
                {t("startPage.footerDescription")}
              </p>
            </div>
            <div>
              <h3 className="font-semibold mb-4">
                {t("startPage.productTitle")}
              </h3>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li>
                  <Link
                    href={`/${locale}/events`}
                    className="hover:text-foreground transition-colors"
                  >
                    {t("startPage.eventsLink")}
                  </Link>
                </li>
                <li>
                  <Link
                    href={`/${locale}/reservations`}
                    className="hover:text-foreground transition-colors"
                  >
                    {t("startPage.reservationsLink")}
                  </Link>
                </li>
                <li>
                  <Link
                    href={`/${locale}/manager`}
                    className="hover:text-foreground transition-colors"
                  >
                    {t("startPage.managementLink")}
                  </Link>
                </li>
                <li>
                  <Link
                    href={`/${locale}/admin`}
                    className="hover:text-foreground transition-colors"
                  >
                    {t("startPage.administrationLink")}
                  </Link>
                </li>
              </ul>
            </div>
            <div>
              <h3 className="font-semibold mb-4">
                {t("startPage.resourcesTitle")}
              </h3>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li>
                  <Link
                    href="#"
                    className="hover:text-foreground transition-colors"
                  >
                    {t("startPage.aboutLink")}
                  </Link>
                </li>
                <li>
                  <Link
                    href="#"
                    className="hover:text-foreground transition-colors"
                  >
                    {t("startPage.contactLink")}
                  </Link>
                </li>
                <li>
                  <Link
                    href="https://github.com/FelixHertweck/seatReservation"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="hover:text-foreground transition-colors flex items-center gap-2"
                  >
                    <Github className="h-4 w-4" />
                    {t("startPage.githubProjectLink")}
                  </Link>
                </li>
              </ul>
            </div>
          </div>
          <div className="border-t mt-8 pt-8 text-center text-sm text-muted-foreground">
            <p>{t("startPage.copyright")}</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
