// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.android.storage.blob.transfer;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

/**
 * Package private.
 *
 * The Data Access Object exposing operations to store and retrieve download metadata.
 *
 * @see BlobDownloadEntity
 */
@Dao
abstract class DownloadDao {
    /**
     * Create blob metadata for a download.
     *
     * @param blob The blob download metadata.
     * @return The blob download metadata key (a.k.a. downloadId).
     */
    @Transaction
    public long createDownloadRecord(BlobDownloadEntity blob) {
        return insert(blob);
    }

    /**
     * Get the blob metadata for a download.
     *
     * @param blobKey The blob download metadata key (a.k.a. downloadId).
     * @return {@link DownloadRecord} instance including blob metadata.
     */
    @Transaction
    @Query("SELECT * FROM blobdownloads where `key` = :blobKey limit 1")
    public abstract DownloadRecord getDownloadRecord(long blobKey);

    /**
     * Get the blob download metadata for a download.
     *
     * @param blobKey The blob download metadata key (a.k.a. downloadId).
     * @return The blob download metadata.
     */
    @Query("SELECT * FROM blobdownloads where `key` = :blobKey limit 1")
    public abstract BlobDownloadEntity getBlob(long blobKey);

    /**
     * Insert a blob download metadata.
     *
     * @param blobDownloadEntity The blob download metadata.
     * @return The autogenerated blob download metadata key (a.k.a. downloadId).
     */
    @Insert
    public abstract Long insert(BlobDownloadEntity blobDownloadEntity);

    /**
     * Update the download state field of a blob download metadata.
     *
     * @param blobKey The blob download metadata key (a.k.a. downloadId).
     * @param totalBytesDownloaded The total bytes downloaded for this blob.
     */
    @Query("UPDATE blobdownloads SET total_bytes_downloaded=:totalBytesDownloaded WHERE `key` = :blobKey")
    public abstract void updateTotalBytesDownloaded(long blobKey, long totalBytesDownloaded);

    /**
     * Update the download state field of a blob download metadata.
     *
     * @param blobKey The blob download metadata key (a.k.a. downloadId).
     * @param state The download state.
     */
    @Query("UPDATE blobdownloads SET blob_download_state=:state WHERE `key` = :blobKey")
    public abstract void updateBlobState(long blobKey, BlobDownloadState state);

    /**
     * Update the interrupted state field of a blob download metadata.
     *
     * @param blobKey The blob download metadata key (a.k.a. downloadId).
     * @param state The interrupted state.
     */
    @Query("UPDATE blobdownloads SET transfer_interrupt_state=:state WHERE `key` = :blobKey")
    public abstract void updateDownloadInterruptState(long blobKey, TransferInterruptState state);

    /**
     * Get the interrupted state from a blob download metadata.
     *
     * @param blobKey The blob download metadata key (a.k.a. downloadId).
     * @return The interrupted state.
     */
    @Query("SELECT transfer_interrupt_state FROM blobdownloads where `key` = :blobKey limit 1")
    public abstract TransferInterruptState getDownloadInterruptState(long blobKey);
}
