"use client";

import { useState, useEffect, useCallback } from "react";
import { useQuery, useMutation } from "@tanstack/react-query";
import { getApiPushSubscriptionsVapidPublicKeyOptions } from "@/api/@tanstack/react-query.gen";
import {
  postApiPushSubscriptions,
  deleteApiPushSubscriptions,
} from "@/api/sdk.gen";
import { useAuth } from "@/hooks/use-auth";

function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");
  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}

function arrayBufferToBase64Url(buffer: ArrayBuffer | null): string {
  if (!buffer) return "";
  const bytes = new Uint8Array(buffer);
  let binary = "";
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return window
    .btoa(binary)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

export function useWebPush() {
  const { user } = useAuth();
  const isSupported =
    typeof window !== "undefined" &&
    "serviceWorker" in navigator &&
    "PushManager" in window &&
    "Notification" in window;

  const [permission, setPermission] = useState<NotificationPermission>(() => {
    return typeof window !== "undefined" && "Notification" in window
      ? Notification.permission
      : "default";
  });
  const [subscription, setSubscription] = useState<PushSubscription | null>(
    null,
  );
  const [isLoading, setIsLoading] = useState<boolean>(isSupported);

  const { data: vapidData } = useQuery({
    ...getApiPushSubscriptionsVapidPublicKeyOptions(),
    enabled: !!user,
  });

  const registerMutation = useMutation({
    mutationFn: (sub: PushSubscription) => {
      const p256dh = arrayBufferToBase64Url(sub.getKey("p256dh"));
      const auth = arrayBufferToBase64Url(sub.getKey("auth"));
      return postApiPushSubscriptions({
        body: {
          endpoint: sub.endpoint,
          p256dh,
          auth,
        },
      });
    },
  });

  const unregisterMutation = useMutation({
    mutationFn: (endpoint: string) =>
      deleteApiPushSubscriptions({ query: { endpoint } }),
  });

  useEffect(() => {
    if (!isSupported) {
      return;
    }

    navigator.serviceWorker
      .register("/sw.js")
      .then((reg) => reg.pushManager.getSubscription())
      .then((sub) => {
        setSubscription(sub);
        setIsLoading(false);
      })
      .catch((err) => {
        console.error("Service worker registration error:", err);
        setIsLoading(false);
      });
  }, [isSupported]);

  const subscribe = useCallback(async () => {
    if (!isSupported) {
      throw new Error("Web Push is not supported in this browser.");
    }

    const vapidPublicKey = (vapidData as { publicKey?: string })?.publicKey;
    if (!vapidPublicKey) {
      throw new Error("VAPID public key is not available from server.");
    }

    const perm = await Notification.requestPermission();
    setPermission(perm);

    if (perm !== "granted") {
      throw new Error("Notification permission was denied.");
    }

    const reg = await navigator.serviceWorker.ready;
    const applicationServerKey = urlBase64ToUint8Array(vapidPublicKey);

    const sub = await reg.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: applicationServerKey as unknown as BufferSource,
    });

    await registerMutation.mutateAsync(sub);
    setSubscription(sub);
    return sub;
  }, [isSupported, vapidData, registerMutation]);

  const unsubscribe = useCallback(async () => {
    if (!subscription) return;

    const endpoint = subscription.endpoint;
    await subscription.unsubscribe();
    await unregisterMutation.mutateAsync(endpoint);
    setSubscription(null);
  }, [subscription, unregisterMutation]);

  return {
    isSupported,
    permission,
    isSubscribed: !!subscription,
    isLoading:
      isLoading || registerMutation.isPending || unregisterMutation.isPending,
    subscribe,
    unsubscribe,
  };
}
