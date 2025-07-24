"use client";

import type React from "react";

import { useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/hooks/use-auth";
import { useRouter } from "next/navigation";

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isLoadingForm, setIsLoadingForm] = useState(false);
  const { user, isLoggedIn, login, logout } = useAuth();
  const router = useRouter();
  const [currentlyLoggingIn, setCurrentlyLoggingIn] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoadingForm(true);
    try {
      setCurrentlyLoggingIn(true);
      await login(username, password);
      setCurrentlyLoggingIn(false);
    } finally {
      setIsLoadingForm(false);
    }
  };

  const handleContinue = () => {
    router.push("/events");
  };

  const handleLogout = async () => {
    await logout();
  };

  if (isLoggedIn && !currentlyLoggingIn) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
        <Card className="w-full max-w-md">
          <CardHeader className="space-y-1">
            <CardTitle className="text-2xl font-bold">
              Willkommen zurück!
            </CardTitle>
            <CardDescription>Sie sind bereits angemeldet.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <Button onClick={handleContinue} className="w-full">
              Weiter mit angemeldetem Nutzer ({user?.username})
            </Button>
            <Button onClick={handleLogout} variant="outline" className="w-full">
              Logout
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1">
          <CardTitle className="text-2xl font-bold">Anmelden</CardTitle>
          <CardDescription>
            Geben Sie Ihre Anmeldedaten ein, um auf Ihr Konto zuzugreifen
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="username">Benutzername</Label>
              <Input
                id="username"
                type="text"
                placeholder="Geben Sie Ihren Benutzernamen ein"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">Passwort</Label>
              <Input
                id="password"
                type="password"
                placeholder="Geben Sie Ihr Passwort ein"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            <Button type="submit" className="w-full" disabled={isLoadingForm}>
              {isLoadingForm ? "Melde mich an..." : "Anmelden"}
            </Button>
          </form>
          <div className="mt-4 text-center text-sm">
            {"Sie haben noch kein Konto? "}
            <Link href="/register" className="text-primary hover:underline">
              Registrieren
            </Link>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
