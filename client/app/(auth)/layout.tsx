import Link from "next/link";
import type { ReactNode } from "react";

import { Wordmark } from "@/components/wordmark";

export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col bg-paper">
      <header className="flex h-16 items-center border-b border-line bg-surface px-6">
        <Link href="/" aria-label="Ledgerline">
          <Wordmark subtitle />
        </Link>
      </header>

      <main className="flex flex-1 items-center justify-center px-4 py-10">
        <div className="w-full max-w-md">
          <div className="rounded-2xl border border-line bg-surface p-8 shadow-[0_1px_2px_rgba(16,24,40,0.04)]">
            {children}
          </div>
          <p className="mt-6 text-center text-xs text-muted">
            Ledgerline · Digital banking
          </p>
        </div>
      </main>
    </div>
  );
}
