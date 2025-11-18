import { cn } from "@/lib/utils";

interface BouncingDotsLoaderProps {
  className?: string;
  dotColor?: string;
}

export function BouncingDotsLoader({
  className,
  dotColor = "bg-primary",
}: BouncingDotsLoaderProps) {
  return (
    <div
      className={cn("flex items-center justify-center space-x-2", className)}
    >
      <div
        className={cn("w-4 h-4 rounded-full animate-bounce-dot", dotColor)}
        style={{ animationDelay: "0s" }}
      />
      <div
        className={cn("w-4 h-4 rounded-full animate-bounce-dot", dotColor)}
        style={{ animationDelay: "0.2s" }}
      />
      <div
        className={cn("w-4 h-4 rounded-full animate-bounce-dot", dotColor)}
        style={{ animationDelay: "0.4s" }}
      />
    </div>
  );
}
