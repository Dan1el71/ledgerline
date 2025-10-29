import assert from "node:assert/strict";
import test from "node:test";

import { needsStepUp } from "../lib/transfer";

test("every customer transfer asks for step-up, regardless of the target field", () => {
  assert.equal(needsStepUp({ method: "POST", path: "api/v1/transfers", role: "CUSTOMER" }), true);
});

test("staff transfers and non-transfer requests do not ask for step-up", () => {
  assert.equal(needsStepUp({ method: "POST", path: "api/v1/transfers", role: "ADMIN" }), false);
  assert.equal(needsStepUp({ method: "GET", path: "api/v1/transfers", role: "CUSTOMER" }), false);
  assert.equal(
    needsStepUp({ method: "POST", path: "api/v1/transfers/123", role: "CUSTOMER" }),
    false
  );
});
