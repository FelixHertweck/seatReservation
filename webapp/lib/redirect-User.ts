import type { AppRouterInstance } from "next/dist/shared/lib/app-router-context.shared-runtime";
import { isValidRedirectUrlEncoded } from "@/lib/utils";
import { UserDto } from "@/api";

export function redirectUser(
  router: AppRouterInstance,
  locale: string,
  currentUser: UserDto | undefined,
  returnToUrl?: string | null,
) {
  if (returnToUrl && isValidRedirectUrlEncoded(returnToUrl)) {
    router.push(decodeURIComponent(returnToUrl));
  } else if (currentUser?.roles?.includes("USER")) {
    router.push(`/${locale}/events`);
  } else if (currentUser?.roles?.includes("SUPERVISOR")) {
    router.push(`/${locale}/liveview`);
  } else if (currentUser?.roles?.includes("MANAGER")) {
    router.push(`/${locale}/manager`);
  } else if (currentUser?.roles?.includes("ADMIN")) {
    router.push(`/${locale}/admin`);
  }
}
