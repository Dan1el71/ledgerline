"use client";

import Link from "next/link";
import { ArrowLeftRight, Bell, ChevronRight, FileText, Wallet } from "lucide-react";

import { Amount, EmptyState } from "@/components/form";
import { SectionHeader } from "@/components/section-header";
import { TransactionList } from "@/components/transaction-list";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useMyAccounts, useMyTransactions } from "@/lib/hooks";
import { useAuthStore } from "@/store/auth-store";

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user);
  const accounts = useMyAccounts();
  const transactions = useMyTransactions({ page: 0, size: 6 });
  const verified = accounts.customer?.kycStatus === "VERIFIED";
  const list = accounts.data ?? [];
  const total = list.filter((a) => a.status !== "CLOSED").reduce((sum, a) => sum + Number(a.balance), 0);
  const ids = new Set(list.map((a) => a.id));

  return (
    <div className="space-y-6">
      <SectionHeader
        title="Overview"
        description={`Welcome back, ${user?.fullName.split(" ")[0] ?? "there"}.`}
        action={
          verified ? (
            <Link href="/transfer">
              <Button className="gap-2"><ArrowLeftRight className="h-4 w-4" /> Pay or transfer</Button>
            </Link>
          ) : null
        }
      />

      {accounts.customer && !verified ? (
        <Card className="border-primary/20 bg-primary/5">
          <CardContent className="text-sm text-slate">
            Your identity verification is <strong className="text-ink">{accounts.customer.kycStatus.toLowerCase()}</strong>.
            You&apos;ll be able to open accounts and make payments once it&apos;s approved.
          </CardContent>
        </Card>
      ) : null}

      <div className="grid gap-6 md:grid-cols-3">
        <Card className="self-start">
          <p className="text-sm text-muted">Total balance</p>
          <div className="mt-2">
            {accounts.isLoading ? (
              <Skeleton className="h-10 w-40" />
            ) : (
              <p className="num text-3xl font-semibold tracking-tight text-ink">
                <Amount value={total} />
              </p>
            )}
          </div>
          <p className="mt-3 text-xs text-muted">
            {list.length} account{list.length === 1 ? "" : "s"} in USD
          </p>
        </Card>

        <Card className="md:col-span-2">
          <CardHeader>
            <CardTitle>Your accounts</CardTitle>
            <Link href="/accounts" className="text-xs font-medium text-primary hover:underline">
              View all
            </Link>
          </CardHeader>
          <CardContent className="space-y-2">
            {accounts.isLoading ? (
              <Skeleton className="h-20 w-full" />
            ) : list.length === 0 ? (
              <EmptyState title="No accounts yet">
                {verified ? (
                  <Link href="/accounts" className="text-primary">
                    Open your first account
                  </Link>
                ) : null}
              </EmptyState>
            ) : (
              list.map((a) => (
                <Link
                  key={a.id}
                  href={`/accounts/${a.id}`}
                  className="flex items-center justify-between rounded-lg border border-line px-4 py-3 hover:bg-paper"
                >
                  <div>
                    <p className="text-sm font-medium text-ink">
                      {a.accountType === "CHECKING" ? "Cheque" : "Savings"}
                    </p>
                    <p className="text-xs text-muted">•••• {a.accountNumber.slice(-4)}</p>
                  </div>
                  <div className="flex items-center gap-2 text-right">
                    <Amount value={Number(a.balance)} />
                    {a.status !== "ACTIVE" ? <Badge variant="danger">{a.status}</Badge> : null}
                  </div>
                </Link>
              ))
            )}
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-6 md:grid-cols-3">
        <Card className="md:col-span-2">
          <CardHeader>
            <CardTitle>Recent activity</CardTitle>
            <Link href="/transactions" className="text-xs font-medium text-primary hover:underline">
              View all
            </Link>
          </CardHeader>
          <CardContent>
            {transactions.isLoading ? (
              <Skeleton className="h-40 w-full" />
            ) : (
              <TransactionList transactions={transactions.data?.content ?? []} myAccountIds={ids} />
            )}
          </CardContent>
        </Card>
        <Card className="self-start">
          <CardHeader>
            <CardTitle>Quick actions</CardTitle>
          </CardHeader>
          <CardContent className="space-y-1">
            {[
              { href: "/transfer", label: "Transfer money", icon: ArrowLeftRight },
              { href: "/statements", label: "View statements", icon: FileText },
              { href: "/accounts", label: "Manage accounts", icon: Wallet },
              { href: "/notifications", label: "Notifications", icon: Bell }
            ].map((action) => {
              const Icon = action.icon;
              return (
                <Link
                  key={action.href}
                  href={action.href}
                  className="flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-slate hover:bg-paper hover:text-ink"
                >
                  <Icon className="h-4 w-4" aria-hidden />
                  <span className="flex-1">{action.label}</span>
                  <ChevronRight className="h-4 w-4 text-muted" aria-hidden />
                </Link>
              );
            })}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
