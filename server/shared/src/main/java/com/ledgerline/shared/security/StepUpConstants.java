package com.ledgerline.shared.security;

/** Contract for the short-lived re-authentication token used for sensitive money movements. */
public final class StepUpConstants {

    private StepUpConstants() {}

    /** Audience that must reject step-up tokens minted for another service. */
    public static final String AUDIENCE = "transaction-service";

    /** Value of the {@code scope} claim; an access token must never be accepted. */
    public static final String SCOPE = "step-up";

    public static final String CLAIM_SCOPE = "scope";
    public static final String CLAIM_TYPE = "type";

    /** Maximum accepted lifetime. auth-service mints exactly this TTL. */
    public static final long TTL_SECONDS = 5 * 60;
}
