"use client";

import { useRouter } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getApiUsersMeOptions,
  postApiAuthLoginMutation,
  postApiAuthLogoutMutation,
  postApiAuthRegisterMutation,
} from "@/api/@tanstack/react-query.gen";
import { RegisterRequestDto } from "@/api";

export function useAuth() {
  const router = useRouter();
  const queryClient = useQueryClient();

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
    await queryClient.invalidateQueries();
  };

  const { mutateAsync: registerMutation } = useMutation({
    ...postApiAuthRegisterMutation(),
    onSuccess: async () => {
      await refetchUser();
      router.push("/events");
    },
  });

  const register = async (userData: RegisterRequestDto) => {
    await registerMutation({ body: userData });
    await queryClient.invalidateQueries();
  };

  const { mutateAsync: logoutMutation } = useMutation({
    ...postApiAuthLogoutMutation(),
  });

  const logout = async () => {
    await logoutMutation({});
    window.location.reload();
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
