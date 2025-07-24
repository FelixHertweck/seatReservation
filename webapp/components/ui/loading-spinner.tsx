import { cn } from "@/lib/utils"

interface LoadingSpinnerProps {
    size?: "sm" | "md" | "lg"
    className?: string
}

export function LoadingSpinner({ size = "md", className }: LoadingSpinnerProps) {
    const sizeClasses = {
        sm: "w-4 h-4",
        md: "w-6 h-6",
        lg: "w-8 h-8",
    }

    return (
        <div className={cn("flex items-center justify-center", className)}>
            <div className={cn("animate-spin rounded-full border-2 border-primary/20 border-t-primary", sizeClasses[size])} />
        </div>
    )
}
