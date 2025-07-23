"use client";

import { useRouter } from "next/navigation";
import { useQuery, useMutation } from "@tanstack/react-query";
import {
  getApiUsersMeOptions,
  postApiAuthLoginMutation,
} from "@/api/@tanstack/react-query.gen";
import type { UserCreationDto } from "@/api";

export function useAuth() {
  const router = useRouter();

  const {
    data: user,
    isLoading: isLoading,
    isSuccess,
    refetch: refetchUser,
  } = useQuery(getApiUsersMeOptions());

  const { mutateAsync: loginMutation } = useMutation({
    ...postApiAuthLoginMutation(),
    onSuccess: async () => {
      await refetchUser();
      router.push("/events");
    },
  });

  const login = async (username: string, password: string) => {
    await loginMutation({ body: { username, password } });
  };

  const register = async (userData: UserCreationDto) => {
    console.log("Need to implement register logic");
    //TODO: Implement registration logic
    router.push("/events");
  };

  const logout = () => {
    router.push("/login");
  };

  return {
    user,
    isLoggedIn: isSuccess,
    isLoading,
    login,
    register,
    logout,
  };
}
