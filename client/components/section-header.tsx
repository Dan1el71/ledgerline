import type { ReactNode } from "react";

interface SectionHeaderProps {
  title: string;
  description?: string;
  action?: ReactNode;
}

export function SectionHeader({ title, description, action }: SectionHeaderProps) {
  return (
    <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
      <div className="space-y-1.5">
        <h1 className="text-2xl font-semibold text-ink">{title}</h1>
        {description ? (
          <p className="max-w-xl text-sm leading-relaxed text-slate">{description}</p>
        ) : null}
      </div>
      {action}
    </div>
  );
}

