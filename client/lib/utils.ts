import type { ClassValue } from "clsx";
import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export { APP_LOCALE, formatCurrency, formatDateTime, getInitials } from "./format";

export const cn = (...inputs: ClassValue[]): string => twMerge(clsx(inputs));
