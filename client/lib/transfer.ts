/**
 * BFF pre-flight rule. The authoritative check is in transaction-service, which compares the
 * target account's owner with the caller. Requiring confirmation for every customer transfer
 * (whether the body uses targetAccountId or targetAccountNumber) means a client cannot choose the
 * cheaper field to skip the prompt; the server still lets transfers between the customer's own
 * accounts through without a token.
 */
export function needsStepUp(request: {
  method: string;
  path: string;
  role?: string | null;
}): boolean {
  return (
    request.method === "POST" && request.path === "api/v1/transfers" && request.role === "CUSTOMER"
  );
}
