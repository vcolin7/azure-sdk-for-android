// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.android.core.util;

/**
 * A type representing a supplier of results.
 */
@FunctionalInterface
public interface Supplier<R> {
    /**
     * Gets a result.
     *
     * @return A result
     */
    R get();
}
