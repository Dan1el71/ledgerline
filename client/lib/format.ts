/**
 * English UI for the Colombian market. This locale keeps dates and grouping in English while
 * Intl still renders COP explicitly (e.g. "COP 5,000,000.00") instead of an ambiguous "$".
 */
export const APP_LOCALE = "en-CO";

export const formatCurrency = (amount: number, currency = "COP"): string => {
  return new Intl.NumberFormat(APP_LOCALE, {
    style: "currency",
    currency,
    maximumFractionDigits: 2,
    minimumFractionDigits: 2
  }).format(amount ?? 0);
};

const dateTimeFormatter = new Intl.DateTimeFormat(APP_LOCALE, {
  day: "2-digit",
  month: "short",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
  hour12: false
});

export const formatDateTime = (value: string): string => {
  if (!value) {
    return "-";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return dateTimeFormatter.format(date);
};

export const getInitials = (fullName: string): string => {
  const chunks = fullName.trim().split(/\s+/).filter(Boolean);

  return (
    chunks
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() ?? "")
      .join("") || "LB"
  );
};
