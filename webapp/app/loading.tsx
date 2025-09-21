import { BouncingDotsLoader } from "@/components/ui/bouncing-dots-loader";

export default function Loading() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <BouncingDotsLoader />
    </div>
  );
}
