import assert from "node:assert/strict";
import test from "node:test";

import { formatCurrency, formatDateTime, getInitials } from "../lib/format";

const plain = (value: string) => value.replace(/\u00a0/g, " ");

test("formatCurrency renders COP with the currency code and two decimals", () => {
  assert.equal(plain(formatCurrency(5_000_000)), "COP 5,000,000.00");
  assert.equal(plain(formatCurrency(1234.5)), "COP 1,234.50");
  assert.equal(plain(formatCurrency(0)), "COP 0.00");
});

test("formatCurrency follows the requested non-COP currency", () => {
  assert.match(plain(formatCurrency(10, "USD")), /^\$10\.00$/);
});

test("formatDateTime returns a dash for empty values", () => {
  assert.equal(formatDateTime(""), "-");
});

test("formatDateTime returns the original text when the date is invalid", () => {
  assert.equal(formatDateTime("not-a-date"), "not-a-date");
});

test("formatDateTime renders a readable local date and time", () => {
  const formatted = formatDateTime("2026-02-28T12:04:00.000Z");

  assert.match(formatted, /2026/);
  assert.match(formatted, /:04/);
  assert.notEqual(formatted, "2026-02-28T12:04:00.000Z");
});

test("getInitials uses the first two words and falls back to LB", () => {
  assert.equal(getInitials("Ada Lovelace"), "AL");
  assert.equal(getInitials("  Ada   Byron Lovelace "), "AB");
  assert.equal(getInitials("   "), "LB");
});
