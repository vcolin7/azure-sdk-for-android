// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.android.core.sampleapp;

import java.util.Date;
import java.util.UUID;

public class Message {
    private String id;
    private String userId;
    private Date timestamp;
    private String contents;

    public Message(String userId, String contents) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.contents = contents;
    }

    public String getId() {
        return id;
    }

    public Message setId(String id) {
        this.id = id;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public Message setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public Message setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public String getContents() {
        return contents;
    }

    public Message setContents(String contents) {
        this.contents = contents;
        return this;
    }
}
