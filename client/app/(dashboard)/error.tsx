"use client";

import { Button } from "@/components/ui/button";

export default function DashboardError({
  error,
  reset
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="grid min-h-[60vh] place-items-center">
      <div className="max-w-md rounded-lg border border-danger/20 bg-surface p-6 text-center">
        <h2 className="text-xl font-semibold text-ink">Dashboard error</h2>
        <p className="mt-2 text-sm text-slate">{error.message || "Could not load this section."}</p>
        <Button className="mt-5" onClick={reset}>
          Reload section
        </Button>
      </div>
    </div>
  );
}

