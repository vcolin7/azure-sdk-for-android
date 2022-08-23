// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.android.core.http.policy;

import static com.azure.android.core.http.implementation.Util.isNullOrEmpty;

import com.azure.android.core.http.HttpHeaders;
import com.azure.android.core.http.HttpPipelinePolicy;
import com.azure.android.core.http.HttpPipelinePolicyChain;
import com.azure.android.core.http.HttpResponse;
import com.azure.android.core.http.NextPolicyCallback;
import com.azure.android.core.http.PolicyCompleter;
import com.azure.android.core.http.implementation.Util;
import com.azure.android.core.util.Supplier;

import org.threeten.bp.Duration;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.temporal.ChronoUnit;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * A pipeline policy that retries when a recoverable HTTP error or exception occurs.
 */
public class RetryPolicy implements HttpPipelinePolicy {
    private final RetryStrategy retryStrategy;
    private final String retryAfterHeader;
    private final ChronoUnit retryAfterTimeUnit;

    /**
     * Creates {@link RetryPolicy} using {@link ExponentialBackoff#ExponentialBackoff()} as the {@link RetryStrategy}.
     */
    public RetryPolicy() {
        this(new ExponentialBackoff(), null, null);
    }

    /**
     * Creates {@link RetryPolicy} using {@link ExponentialBackoff#ExponentialBackoff()} as the {@link RetryStrategy}
     * and uses {@code retryAfterHeader} to look up the wait period in the returned {@link HttpResponse} to calculate
     * the retry delay when a recoverable HTTP error is returned.
     *
     * @param retryAfterHeader The HTTP header, such as {@code Retry-After} or {@code x-ms-retry-after-ms}, to lookup
     * for the retry delay. If the value is null, {@link RetryStrategy#calculateRetryDelay(int)} will compute the delay
     * and ignore the delay provided in response header.
     * @param retryAfterTimeUnit The time unit to use when applying the retry delay. Null is valid if, and only if,
     * {@code retryAfterHeader} is null.
     *
     * @throws NullPointerException When {@code retryAfterTimeUnit} is null and {@code retryAfterHeader} is not null.
     */
    public RetryPolicy(String retryAfterHeader, ChronoUnit retryAfterTimeUnit) {
        this(new ExponentialBackoff(), retryAfterHeader, retryAfterTimeUnit);
    }

    /**
     * Creates {@link RetryPolicy} with the provided {@link RetryStrategy} and default {@link ExponentialBackoff} as
     * {@link RetryStrategy}. It will use provided {@code retryAfterHeader} in {@link HttpResponse} headers for
     * calculating retry delay.
     *
     * @param retryStrategy The {@link RetryStrategy} used for retries.
     * @param retryAfterHeader The HTTP header, such as 'Retry-After' or 'x-ms-retry-after-ms', to lookup for the retry
     * delay. If the value is null, {@link RetryPolicy} will use the retry strategy to compute the delay and ignore the
     * delay provided in response header.
     * @param retryAfterTimeUnit The time unit to use when applying the retry delay. null is valid if, and only if,
     * {@code retryAfterHeader} is null.
     *
     * @throws NullPointerException If {@code retryStrategy} is null or when {@code retryAfterTimeUnit} is null and
     * {@code retryAfterHeader} is not null.
     */
    public RetryPolicy(RetryStrategy retryStrategy, String retryAfterHeader, ChronoUnit retryAfterTimeUnit) {
        this.retryStrategy = Util.requireNonNull(retryStrategy, "'retryStrategy' cannot be null.");
        this.retryAfterHeader = retryAfterHeader;
        this.retryAfterTimeUnit = retryAfterTimeUnit;

        if (!isNullOrEmpty(retryAfterHeader)) {
            Util.requireNonNull(retryAfterTimeUnit, "'retryAfterTimeUnit' cannot be null.");
        }
    }

    /**
     * Creates a {@link RetryPolicy} with the provided {@link RetryStrategy}.
     *
     * @param retryStrategy The {@link RetryStrategy} used for retries.
     * @throws NullPointerException If {@code retryStrategy} is null.
     */
    public RetryPolicy(RetryStrategy retryStrategy) {
        this(retryStrategy, null, null);
    }

    /**
     * Get an instance of {@link RetryPolicy} that uses fixed backoff delay retry strategy.
     *
     * @param maxRetries The maximum number of times to retry.
     * @param delay The fixed backoff delay applied before every retry.
     *
     * @return The retry policy.
     */
    public static RetryPolicy withFixedDelay(int maxRetries, Duration delay) {
        return new RetryPolicy(new FixedDelay(maxRetries, delay));
    }

    /**
     * Get an instance of {@link RetryPolicy} that uses a default full jitter backoff
     * retry strategy.
     *
     * <p>
     * The retry strategy by default retries maximum 3 times, uses 800 milliseconds as
     * the default base delay and uses 8 seconds as default maximum backoff delay before a retry.
     *
     * @return The retry policy.
     */
    public static RetryPolicy withExponentialBackoff() {
        return new RetryPolicy(new ExponentialBackoff());
    }

    /**
     * Get an instance of {@link RetryPolicy} that uses full jitter backoff retry strategy.
     *
     * @param maxRetries The maximum number of times to retry.
     * @param baseDelay The delay used as the coefficient for backoffs, also baseDelay will be the first backoff delay.
     * @param maxDelay The maximum backoff delay before a retry.
     *
     * @return The retry interceptor.
     */
    public static RetryPolicy withExponentialBackoff(int maxRetries, Duration baseDelay, Duration maxDelay) {
        return new RetryPolicy(new ExponentialBackoff(maxRetries, baseDelay, maxDelay));
    }

    /**
     * Intercepts any exception in the pipeline or the HTTP response error and, if recoverable, retries sending
     * the request.
     *
     * @param chain Provides access to the request to send.
     */
    @Override
    public void process(HttpPipelinePolicyChain chain) {
        if (chain.getCancellationToken().isCancellationRequested()) {
            chain.completedError(new IOException("Canceled."));

            return;
        }

        chain.processNextPolicy(chain.getRequest(), new NextPolicyCallback() {
            @Override
            public PolicyCompleter.CompletionState onSuccess(HttpResponse response, PolicyCompleter completer) {
                return retryIfRequired(chain, response, null, completer, 0);
            }

            @Override
            public PolicyCompleter.CompletionState onError(Throwable error, PolicyCompleter completer) {
                return retryIfRequired(chain, null, error, completer, 0);
            }
        });
    }

    private PolicyCompleter.CompletionState retryIfRequired(HttpPipelinePolicyChain chain, HttpResponse response,
                                                            Throwable error, PolicyCompleter completer,
                                                            final int retryAttempts) {
        if (chain.getCancellationToken().isCancellationRequested()) {
            if (response != null) {
                response.close();
            }

            return completer.completedError(new IOException("Canceled."));
        }

        if (!shouldRetry(response, retryAttempts)) {
            if (response != null) {
                return completer.completed(response);
            } else {
                if (retryAttempts >= this.retryStrategy.getMaxRetries()) {
                    final RuntimeException maxRetriedError = new RuntimeException(String.format(Locale.getDefault(),
                        "The max retries (%d times) for the service call is exceeded.",
                        this.retryStrategy.getMaxRetries()));
                    if (error != null) {
                        maxRetriedError.addSuppressed(error);
                    }

                    return completer.completedError(maxRetriedError);
                } else {
                    return completer.completedError(error);
                }
            }
        } else {
            Duration delay = null;
            Throwable userError = null;

            try {
                delay = calculateRetryDelay(response, null, retryAttempts);
            } catch (Throwable e) {
                userError = e;
            } finally {
                if (response != null) {
                    response.close();
                }
            }

            if (userError != null) {
                return completer.completedError(userError);
            } else {
                chain.processNextPolicy(chain.getRequest(), new NextPolicyCallback() {
                    @Override
                    public PolicyCompleter.CompletionState onSuccess(HttpResponse response, PolicyCompleter completer) {
                        return retryIfRequired(chain, response, null, completer, retryAttempts + 1);
                    }

                    @Override
                    public PolicyCompleter.CompletionState onError(Throwable error, PolicyCompleter completer) {
                        return retryIfRequired(chain, null, error, completer, retryAttempts + 1);
                    }
                }, delay.toMillis(), TimeUnit.MILLISECONDS);

                return completer.defer();
            }
        }
    }

    /**
     * Determines the delay duration that should be waited before retrying.
     *
     * @param response The HTTP response.
     * @param exception The pipeline exception, if any.
     * @param retryAttempts The number of retry attempts so far made.
     *
     * @return The delay duration.
     */
    public Duration calculateRetryDelay(HttpResponse response, Throwable exception, int retryAttempts) {
        if (exception == null) {
            final int code = response.getStatusCode();

            if (code == 429) {
                // Too Many Requests.
                // https://docs.microsoft.com/en-us/rest/api/cosmos-db/common-cosmosdb-rest-response-headers
                final String retryAfterHeader = response.getHeaderValue("x-ms-retry-after-ms");
                if (retryAfterHeader != null) {
                    return Duration.of(Integer.parseInt(retryAfterHeader), ChronoUnit.MILLIS);
                }
            }

            if (code == 429 || code == 503) {
                // Too Many Requests OR Service Unavailable
                // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Retry-After
                final String retryAfterHeader = response.getHeaderValue("Retry-After");
                if (retryAfterHeader != null) {
                    try {
                        return Duration.between(OffsetDateTime.now(),
                            OffsetDateTime.parse(retryAfterHeader, DateTimeFormatter.RFC_1123_DATE_TIME));
                    } catch (Exception ignored) {
                        return Duration.of(Integer.parseInt(retryAfterHeader), ChronoUnit.SECONDS);
                    }
                }
            }

        }

        return this.retryStrategy.calculateRetryDelay(retryAttempts);
    }

    /**
     * Determines any retry should be performed.
     *
     * @param response The HTTP response.
     * @param retryAttempts The number of retry attempts made so far.
     *
     * @return True to retry, false to exit retry loop.
     */
    private boolean shouldRetry(HttpResponse response, int retryAttempts) {
        return retryAttempts < retryStrategy.getMaxRetries() && retryStrategy.shouldRetry(response);
    }

    private boolean shouldRetryException(Throwable throwable, int tryCount) {
        return tryCount < retryStrategy.getMaxRetries() && retryStrategy.shouldRetryException(throwable);
    }

    /*
     * Determines the delay duration that should be waited before retrying.
     */
    static Duration determineDelayDuration(HttpResponse response, int tryCount, RetryStrategy retryStrategy,
                                           String retryAfterHeader, ChronoUnit retryAfterTimeUnit) {
        // If the retry after header hasn't been configured, attempt to look up the well-known headers.
        if (isNullOrEmpty(retryAfterHeader)) {
            return getWellKnownRetryDelay(response.getHeaders(), tryCount, retryStrategy, OffsetDateTime::now);
        }

        String retryHeaderValue = response.getHeaderValue(retryAfterHeader);

        // Retry header is missing or empty, return the default delay duration.
        if (isNullOrEmpty(retryHeaderValue)) {
            return retryStrategy.calculateRetryDelay(tryCount);
        }

        // Use the response delay duration, the server returned it for a reason.
        return Duration.of(Integer.parseInt(retryHeaderValue), retryAfterTimeUnit);
    }

    /*
     * Determines the delay duration that should be waited before retrying using the well-known retry headers.
     */
    static Duration getWellKnownRetryDelay(HttpHeaders responseHeaders, int tryCount, RetryStrategy retryStrategy,
                                           Supplier<OffsetDateTime> nowSupplier) {
        Duration retryDelay = Util.getRetryAfterFromHeaders(responseHeaders, nowSupplier);

        if (retryDelay != null) {
            return retryDelay;
        }

        // None of the well-known headers have been found, return the default delay duration.
        return retryStrategy.calculateRetryDelay(tryCount);
    }
}
