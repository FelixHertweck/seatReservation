import { useEffect, useState } from "react";

export function useAuthStatus() {
  const [isLoading, setIsLoading] = useState(false);
  const [isLoggedIn, setIsLoggedIn] = useState<boolean | null>(null);

  const fetchUserStatus = async () => {
    setIsLoading(true);
    const response = await fetch("/api/users/me");
    const errorIs401 = response.status === 401;

    setIsLoggedIn(!errorIs401);
    setIsLoading(false);
  };

  useEffect(() => {
    void fetchUserStatus();
  }, []);

  return { isLoggedIn, isLoading };
}
