import type { HTMLAttributes } from "react";

import { cn } from "@/lib/utils";

type BadgeVariant = "default" | "soft" | "success" | "danger";

const variantClasses: Record<BadgeVariant, string> = {
  default: "bg-primary/10 text-primary",
  soft: "bg-paper text-slate ring-1 ring-inset ring-line",
  success: "bg-emerald-50 text-emerald-700",
  danger: "bg-red-50 text-red-700"
};

interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: BadgeVariant;
}

export function Badge({ className, variant = "default", ...props }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium",
        variantClasses[variant],
        className
      )}
      {...props}
    />
  );
}
