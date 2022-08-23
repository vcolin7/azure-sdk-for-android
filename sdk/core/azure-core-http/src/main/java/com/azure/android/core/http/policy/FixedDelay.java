// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.android.core.http.policy;

import com.azure.android.core.http.HttpResponse;
import com.azure.android.core.http.implementation.Util;
import com.azure.android.core.logging.ClientLogger;

import org.threeten.bp.Duration;

/**
 * The retry strategy with fixed backoff delay.
 */
public class FixedDelay implements RetryStrategy {
    private final ClientLogger logger = new ClientLogger(FixedDelay.class);

    private final int maxRetries;
    private final Duration delay;

    /**
     * Creates an instance of {@link FixedDelay}.
     *
     * @param maxRetries The max number of retry attempts that can be made.
     * @param delay The fixed delay duration between retry attempts.
     * @throws IllegalArgumentException If {@code maxRetries} is negative.
     * @throws NullPointerException If {@code delay} is {@code null}.
     */
    public FixedDelay(int maxRetries, Duration delay) {
        if (maxRetries < 0) {
            throw logger.logExceptionAsError(new IllegalArgumentException("'maxRetries' cannot be less than 0."));
        }
        this.maxRetries = maxRetries;
        this.delay = Util.requireNonNull(delay, "'delay' cannot be null.");
    }

    @Override
    public int getMaxRetries() {
        return this.maxRetries;
    }

    @Override
    public Duration calculateRetryDelay(int retryAttempts) {
        return this.delay;
    }
}
