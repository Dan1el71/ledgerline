package com.ledgerline.shared.exception;

/**
 * The request needs a fresh password confirmation. Mapped to HTTP 428 so the BFF and web client can
 * open the step-up dialog instead of showing a generic authorization error.
 */
public class StepUpRequiredException extends RuntimeException {
    public StepUpRequiredException(String message) {
        super(message);
    }
}
