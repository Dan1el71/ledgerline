"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useMemo, useState, type ReactNode } from "react";
import {
  ArrowLeftRight,
  Bell,
  FileText,
  Home,
  ListOrdered,
  LogOut,
  Menu,
  Settings,
  ShieldCheck,
  Sparkles,
  Wallet,
  X
} from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Wordmark } from "@/components/wordmark";
import { authApi } from "@/lib/api";
import { useCustomer, useUnreadCount } from "@/lib/hooks";
import { cn, getInitials } from "@/lib/utils";
import { useAuthStore } from "@/store/auth-store";
import type { UserRole } from "@/types/api";

interface NavigationItem {
  href: string;
  label: string;
  icon: typeof Home;
  roles: UserRole[];
}

const navigation: NavigationItem[] = [
  { href: "/dashboard", label: "Overview", icon: Home, roles: ["CUSTOMER"] },
  { href: "/accounts", label: "Accounts", icon: Wallet, roles: ["CUSTOMER"] },
  { href: "/transfer", label: "Transfer", icon: ArrowLeftRight, roles: ["CUSTOMER"] },
  { href: "/transactions", label: "Transactions", icon: ListOrdered, roles: ["CUSTOMER"] },
  { href: "/statements", label: "Statements", icon: FileText, roles: ["CUSTOMER"] },
  { href: "/assistant", label: "Assistant", icon: Sparkles, roles: ["CUSTOMER"] },
  { href: "/notifications", label: "Notifications", icon: Bell, roles: ["CUSTOMER"] },
  { href: "/admin", label: "Back office", icon: ShieldCheck, roles: ["ADMIN", "SUPPORT"] },
  { href: "/settings", label: "Settings", icon: Settings, roles: ["CUSTOMER", "ADMIN", "SUPPORT"] }
];

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const user = useAuthStore((s) => s.user);
  const hydrated = useAuthStore((s) => s.hydrated);
  const setUser = useAuthStore((s) => s.setUser);
  const [mobileOpen, setMobileOpen] = useState(false);
  const isCustomer = user?.role === "CUSTOMER";
  const customer = useCustomer();
  const unread = useUnreadCount();

  useEffect(() => {
    if (hydrated && !user) {
      router.replace("/login");
    }
  }, [hydrated, router, user]);

  useEffect(() => {
    if (isCustomer && customer.isSuccess && customer.data === null && pathname !== "/onboarding") {
      router.replace("/onboarding");
    }
  }, [customer.data, customer.isSuccess, isCustomer, pathname, router]);

  useEffect(() => setMobileOpen(false), [pathname]);

  const items = useMemo(
    () => navigation.filter((item) => user && item.roles.includes(user.role)),
    [user]
  );

  if (!hydrated || !user) {
    return (
      <div className="flex min-h-screen items-center justify-center" aria-busy="true">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-line border-t-primary" />
      </div>
    );
  }

  const signOut = async () => {
    await authApi.logout().catch(() => null);
    setUser(null);
    router.replace("/login");
  };

  const unreadCount = unread.data?.unread ?? 0;

  return (
    <div className="min-h-screen md:grid md:grid-cols-[260px_1fr]">
      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-30 flex w-[260px] flex-col border-r border-line bg-surface transition-transform md:sticky md:top-0 md:h-screen md:self-start md:translate-x-0",
          mobileOpen ? "translate-x-0" : "-translate-x-full"
        )}
        aria-label="Main navigation"
      >
        <div className="flex h-16 items-center justify-between border-b border-line px-5">
          <Link href={isCustomer ? "/dashboard" : "/admin"} aria-label="Ledgerline home">
            <Wordmark />
          </Link>
          <button
            className="rounded-md p-1 text-slate hover:bg-paper md:hidden"
            onClick={() => setMobileOpen(false)}
            aria-label="Close menu"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-4">
          {items.map((item) => {
            const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
            const Icon = item.icon;
            return (
              <Link
                key={item.href}
                href={item.href}
                aria-current={active ? "page" : undefined}
                className={cn(
                  "flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors",
                  active
                    ? "bg-primary/10 font-medium text-primary"
                    : "text-slate hover:bg-paper hover:text-ink"
                )}
              >
                <Icon className="h-4 w-4 shrink-0" aria-hidden />
                <span className="flex-1">{item.label}</span>
                {item.href === "/notifications" && unreadCount > 0 ? (
                  <span className="rounded-full bg-primary px-2 py-0.5 text-[11px] font-medium text-white">
                    {unreadCount > 99 ? "99+" : unreadCount}
                  </span>
                ) : null}
              </Link>
            );
          })}
        </nav>

        <div className="border-t border-line p-3">
          <div className="flex items-center gap-3 rounded-lg px-2 py-2">
            <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-primary/10 text-xs font-semibold text-primary">
              {getInitials(user.fullName)}
            </span>
            <span className="min-w-0 flex-1">
              <span className="block truncate text-sm font-medium text-ink">{user.fullName}</span>
              <span className="block truncate text-xs text-muted">{user.email}</span>
            </span>
            <button
              onClick={signOut}
              className="rounded-md p-1.5 text-muted transition-colors hover:bg-paper hover:text-ink"
              aria-label="Sign out"
              title="Sign out"
            >
              <LogOut className="h-4 w-4" />
            </button>
          </div>
        </div>
      </aside>

      {mobileOpen ? (
        <button
          onClick={() => setMobileOpen(false)}
          className="fixed inset-0 z-20 bg-ink/30 md:hidden"
          aria-label="Close navigation"
        />
      ) : null}

      <div className="flex min-h-screen min-w-0 flex-col">
        <header className="sticky top-0 z-10 flex h-16 items-center justify-between border-b border-line bg-surface px-4 md:px-8">
          <div className="flex items-center gap-3">
            <button
              className="rounded-md border border-line p-1.5 text-slate md:hidden"
              onClick={() => setMobileOpen(true)}
              aria-label="Open menu"
            >
              <Menu className="h-4 w-4" />
            </button>
            <span className="text-sm font-medium text-ink">
              {user.role === "CUSTOMER" ? "Personal banking" : "Operations"}
            </span>
          </div>
          <div className="flex items-center gap-3">
            {user.role !== "CUSTOMER" ? <Badge variant="soft">{user.role}</Badge> : null}
            <span className="grid h-8 w-8 place-items-center rounded-full bg-paper text-xs font-semibold text-slate">
              {getInitials(user.fullName)}
            </span>
          </div>
        </header>

        <main className="flex-1 px-4 py-8 md:px-8">
          <div className="mx-auto w-full max-w-6xl">{children}</div>
        </main>

        <footer className="border-t border-line px-4 py-5 md:px-8">
          <div className="mx-auto flex w-full max-w-6xl items-center justify-between text-xs text-muted">
            <span>Ledgerline</span>
            <span>All rights reserved</span>
          </div>
        </footer>
      </div>
    </div>
  );
}
