import type { ReactNode, SelectHTMLAttributes } from "react";

import { cn } from "@/lib/utils";

export function Field({
  label,
  error,
  hint,
  children
}: {
  label: string;
  error?: string;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <label className="block space-y-1.5 text-sm">
      <span className="block text-sm font-medium text-ink">{label}</span>
      {children}
      {error ? (
        <p className="text-xs text-danger" role="alert">
          {error}
        </p>
      ) : hint ? (
        <p className="text-xs text-muted">{hint}</p>
      ) : null}
    </label>
  );
}

export function Select({ className, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select
      className={cn(
        "h-10 w-full rounded-lg border border-line bg-surface px-3 text-sm text-ink shadow-sm outline-none transition-colors hover:border-slate/40 focus:border-primary focus:ring-2 focus:ring-primary/15 disabled:cursor-not-allowed disabled:bg-paper disabled:opacity-70",
        className
      )}
      {...props}
    />
  );
}

export function EmptyState({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="rounded-xl border border-dashed border-line bg-surface p-8 text-center">
      <p className="text-sm font-medium text-ink">{title}</p>
      {children ? <div className="mt-1 text-sm text-muted">{children}</div> : null}
    </div>
  );
}

export function Amount({
  value,
  direction,
  currency = "USD"
}: {
  value: number;
  direction?: "in" | "out";
  currency?: string;
}) {
  const formatted = new Intl.NumberFormat("en-US", { style: "currency", currency }).format(
    Math.abs(value)
  );
  return (
    <span
      className={cn(
        "tabular-nums font-medium",
        direction === "in" ? "text-emerald-700" : "text-ink"
      )}
    >
      {direction === "in" ? "+" : direction === "out" ? "−" : ""}
      {formatted}
    </span>
  );
}
