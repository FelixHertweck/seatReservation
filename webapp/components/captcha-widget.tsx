"use client";

import { useEffect, useState } from "react";

export function CaptchaWidget() {
  const [siteKey, setSiteKey] = useState("your_site_key");

  useEffect(() => {
    // Add event listener to capture the captcha token
    const handleCaptchaChange = (e: Event) => {
      const customEvent = e as CustomEvent;
      if (customEvent.detail && customEvent.detail.token) {
        window.sessionStorage.setItem("captchaToken", customEvent.detail.token);
      }
    };

    document.addEventListener("cap-token", handleCaptchaChange);

    return () => {
      document.removeEventListener("cap-token", handleCaptchaChange);
    };
  }, []);

  useEffect(() => {
    // Optionally fetch site key from env if passed through Next.js
    const envSiteKey = process.env.NEXT_PUBLIC_CAP_SITE_KEY;
    if (envSiteKey) {
      setSiteKey(envSiteKey);
    }
  }, []);

  return (
    <div className="flex justify-center my-4">
      {/* @ts-ignore */}
      <cap-widget data-cap-api-endpoint={`/captcha/${siteKey}/`}></cap-widget>
    </div>
  );
}
