// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.android.storage.blob.transfer;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

/**
 * Package private.
 *
 * The Data Access Object exposing operations to store and retrieve upload metadata.
 *
 * @see BlobUploadEntity
 * @see BlockUploadEntity
 */
@Dao
abstract class UploadDao {
    /**
     * Create blob metadata and it's blocks metadata for a file upload.
     *
     * @param blob the blob upload metadata
     * @param blocks the collection of block upload metadata
     * @return the blob upload metadata key (aka uploadId)
     */
    @Transaction
    public long createUploadRecord(BlobUploadEntity blob, List<BlockUploadEntity> blocks) {
        long uploadId = insert(blob);
        for (BlockUploadEntity block : blocks) {
            block.setBlobKey(uploadId);
            insert(block);
        }
        return uploadId;
    }

    /**
     * Get the blob and blocks metadata for a file upload.
     *
     * @param blobKey the blob upload metadata key (aka uploadId)
     * @return an {@link UploadRecord} instance composing blob and blocks metadata
     */
    @Transaction
    @Query("SELECT * FROM blobuploads where `key` = :blobKey limit 1")
    public abstract UploadRecord getUploadRecord(long blobKey);

    /**
     * Get the blob upload metadata for a file upload.
     *
     * @param blobKey the blob upload metadata key (aka uploadId)
     * @return the blob upload metadata
     */
    @Query("SELECT * FROM blobuploads where `key` = :blobKey limit 1")
    public abstract BlobUploadEntity getBlob(long blobKey);

    /**
     * Get the collection of block upload metadata for a file upload.
     *
     * @param blobKey the blob upload metadata key (aka uploadId)
     * @return the collection of block upload metadata
     */
    @Query("SELECT * FROM blockuploads where `blob_key` = :blobKey")
    public abstract List<BlockUploadEntity> getBlocks(long blobKey);

    /**
     * Get the filtered collection of block upload metadata for a file upload.
     *
     * @param blobKey the blob upload metadata key (aka uploadId)
     * @param skipStates the state of the metadata entries to be skipped
     * @return the collection of block upload metadata
     */
    @Query("SELECT * FROM blockuploads where `blob_key` = :blobKey and block_upload_state NOT IN (:skipStates)")
    public abstract List<BlockUploadEntity> getBlocks(long blobKey, List<BlockTransferState> skipStates);

    /**
     * Get the collection of block ids for a file upload.
     *
     * @param blobKey the blob upload metadata key (aka uploadId)
     * @return the base64 block ids
     */
    @Query("SELECT block_id FROM blockuploads where `blob_key` = :blobKey")
    public abstract List<String> getBlockIds(long blobKey);

    /**
     * Insert a blob upload metadata.
     *
     * @param blobUploadEntity the blob upload metadata
     * @return the autogenerated the blob upload metadata key (aka uploadId)
     */
    @Insert
    public abstract Long insert(BlobUploadEntity blobUploadEntity);

    /**
     * Insert a block upload metadata.
     *
     * @param blockUploadEntity the block upload metadata
     */
    @Insert
    public abstract void insert(BlockUploadEntity blockUploadEntity);

    /**
     * Update the upload state in a block upload metadata entity.
     *
     * @param blockKey the block upload metadata entity key
     * @param state the upload state
     */
    @Query("UPDATE blockuploads SET block_upload_state=:state WHERE `key` = :blockKey")
    public abstract void updateBlockState(long blockKey, BlockTransferState state);

    /**
     * Update the upload state field of a blob upload metadata.
     *
     * @param blobKey the blob upload metadata key (aka uploadId)
     * @param state the upload state
     */
    @Query("UPDATE blobuploads SET blob_upload_state=:state WHERE `key` = :blobKey")
    public abstract void updateBlobState(long blobKey, BlobTransferState state);

    /**
     * Update the interrupted state field of a blob upload metadata.
     *
     * @param blobKey the blob upload metadata key (aka uploadId)
     * @param state the interrupted state
     */
    @Query("UPDATE blobuploads SET transfer_interrupt_state=:state WHERE `key` = :blobKey")
    public abstract void updateUploadInterruptState(long blobKey, TransferInterruptState state);

    /**
     * Get the interrupted state from a blob upload metadata.
     *
     * @param blobKey the blob upload metadata key (aka uploadId)
     * @return the interrupted state
     */
    @Query("SELECT transfer_interrupt_state FROM blobuploads where `key` = :blobKey limit 1")
    public abstract TransferInterruptState getTransferInterruptState(long blobKey);

    /**
     * Get the total bytes uploaded so far for a blob upload.
     *
     * @param blobKey the blob upload metadata key (aka uploadId)
     * @return the bytes uploaded
     */
    public long getUploadedBytesCount(long blobKey) {
        return this.getAggregatedBlockSize(blobKey, BlockTransferState.COMPLETED);
    }

    /**
     * Get the sum of size of blocks for a blob upload.
     *
     * @param blobKey the blob upload metadata key (aka uploadId)
     * @param state the state of the blocks to be considered for aggregation
     * @return the aggregated block size
     */
    @Query("SELECT SUM(block_size) FROM blockuploads WHERE blob_key = :blobKey and block_upload_state = :state")
    public abstract long getAggregatedBlockSize(long blobKey, BlockTransferState state);
}
