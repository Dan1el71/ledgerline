"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { z } from "zod";

import { Field } from "@/components/form";
import { SectionHeader } from "@/components/section-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { customersApi, errorMessage } from "@/lib/api";
import { keys, useCustomer } from "@/lib/hooks";
import { useAuthStore } from "@/store/auth-store";

const schema = z.object({
  phoneNumber: z
    .string()
    .trim()
    .regex(/^\+?[1-9]\d{6,14}$/, "Use an international number with country code"),
  idNumber: z
    .string()
    .trim()
    .regex(/^[A-Za-z0-9][A-Za-z0-9-]{4,18}[A-Za-z0-9]$/, "6-20 letters, digits or hyphens")
    .refine((value) => /\d/.test(value), "Must contain at least one digit"),
  dateOfBirth: z.string().min(1, "Required"),
  address: z.string().trim().min(10, "Enter your full residential address").max(300)
});

type Form = z.infer<typeof schema>;

export default function OnboardingPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const customer = useCustomer();
  const [form, setForm] = useState<Form>({ phoneNumber: "", idNumber: "", dateOfBirth: "", address: "" });
  const [errors, setErrors] = useState<Partial<Record<keyof Form, string>>>({});
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  if (customer.data) {
    return (
      <div className="mx-auto max-w-xl">
        <SectionHeader title="Verification in progress" description="We'll notify you as soon as it's done." />
        <Card>
          <CardContent className="space-y-3 text-sm text-slate">
            <p>
              Your identity status is <strong className="text-ink">{customer.data.kycStatus}</strong>.
              Verification usually takes one business day. You can explore the app meanwhile; opening
              accounts and payments unlock once you&apos;re verified.
            </p>
            <Button onClick={() => router.push("/dashboard")}>Go to home</Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    const parsed = schema.safeParse(form);
    if (!parsed.success || !user) {
      const f = parsed.success ? {} : parsed.error.flatten().fieldErrors;
      setErrors({
        phoneNumber: f.phoneNumber?.[0], idNumber: f.idNumber?.[0],
        dateOfBirth: f.dateOfBirth?.[0], address: f.address?.[0]
      });
      return;
    }
    setErrors({});
    setPending(true);
    setError(null);
    const [firstName, ...rest] = user.fullName.split(" ");
    try {
      await customersApi.create({
        userId: Number(user.id),
        firstName,
        lastName: rest.join(" ") || firstName,
        email: user.email,
        ...parsed.data
      });
      await queryClient.invalidateQueries({ queryKey: keys.me });
    } catch (err) {
      setError(errorMessage(err, "We couldn't save your details."));
    } finally {
      setPending(false);
    }
  };

  const set = (key: keyof Form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((prev) => ({ ...prev, [key]: e.target.value }));

  return (
    <div className="mx-auto max-w-xl">
      <SectionHeader title="Verify your identity" description="Regulations require us to verify your identity before you can use your accounts." />
      <Card>
        <CardContent>
          <form className="space-y-4" onSubmit={submit} noValidate>
            <Field label="Mobile number" error={errors.phoneNumber}>
              <Input inputMode="tel" autoComplete="tel" value={form.phoneNumber} onChange={set("phoneNumber")} />
            </Field>
            <Field label="ID number" error={errors.idNumber}>
              <Input inputMode="numeric" autoComplete="off" value={form.idNumber} onChange={set("idNumber")} />
            </Field>
            <Field label="Date of birth" error={errors.dateOfBirth}>
              <Input type="date" autoComplete="bday" value={form.dateOfBirth} onChange={set("dateOfBirth")} />
            </Field>
            <Field label="Residential address" error={errors.address}>
              <Input autoComplete="street-address" value={form.address} onChange={set("address")} />
            </Field>
            {error ? <p className="text-sm text-danger" role="alert">{error}</p> : null}
            <Button type="submit" className="w-full" disabled={pending}>
              {pending ? "Submitting…" : "Submit for verification"}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
