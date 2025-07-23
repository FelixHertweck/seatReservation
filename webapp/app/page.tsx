import { redirect } from "next/navigation";

export default function HomePage() {
  // Redirect to events page as default
  redirect("/events");
}
