import { cn } from "@/lib/utils";

export function Mark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className={cn("h-6 w-6", className)}>
      <rect width="24" height="24" rx="6" fill="currentColor" />
      <path
        d="M8 7v10h8"
        fill="none"
        stroke="white"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function Wordmark({
  className,
  subtitle = false,
  tone = "dark"
}: {
  className?: string;
  subtitle?: boolean;
  tone?: "dark" | "light";
}) {
  const light = tone === "light";
  return (
    <span className={cn("inline-flex items-center gap-2.5", className)}>
      <Mark className={cn("h-7 w-7", light ? "text-white" : "text-primary")} />
      <span className="leading-tight">
        <span
          className={cn(
            "block text-[15px] font-semibold tracking-tight",
            light ? "text-white" : "text-ink"
          )}
        >
          Ledgerline
        </span>
        {subtitle ? (
          <span className={cn("block text-xs", light ? "text-white/70" : "text-muted")}>
            Digital banking
          </span>
        ) : null}
      </span>
    </span>
  );
}
