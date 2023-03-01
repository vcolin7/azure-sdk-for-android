// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.android.core.sampleapp;

import com.azure.android.core.util.events.EventHandler;
import com.azure.android.core.util.events.EventHandlerCollection;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MessagingClient {
    static final String MESSAGE_RECEIVED = "MessageReceived";
    static final String MESSAGE_SENT = "MessageSent";

    private final List<Message> sentMessages;

    public static class MessageSentEvent {
        private String messageId;
        private String userId;
        private Date timestamp;

        public String getMessageId() {
            return messageId;
        }

        public MessageSentEvent setMessageId(String messageId) {
            this.messageId = messageId;

            return this;
        }

        public String getUserId() {
            return userId;
        }

        public MessageSentEvent setUserId(String userId) {
            this.userId = userId;

            return this;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public MessageSentEvent setTimestamp(Date timestamp) {
            this.timestamp = timestamp;

            return this;
        }
    }

    public static class MessageReceivedEvent {
        private String messageId;
        private String userId;
        private Date timestamp;
        private String contents;

        public String getMessageId() {
            return messageId;
        }

        public MessageReceivedEvent setMessageId(String messageId) {
            this.messageId = messageId;

            return this;
        }

        public String getUserId() {
            return userId;
        }

        public MessageReceivedEvent setUserId(String userId) {
            this.userId = userId;

            return this;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public MessageReceivedEvent setTimestamp(Date timestamp) {
            this.timestamp = timestamp;

            return this;
        }

        public String getContents() {
            return contents;
        }

        public MessageReceivedEvent setContents(String contents) {
            this.contents = contents;

            return this;
        }
    }

    public final EventHandlerCollection eventHandlerCollection;

    public MessagingClient() {
        this.eventHandlerCollection = new EventHandlerCollection();
        this.sentMessages = new ArrayList<>();
    }

    public void addOnMessageSentEventHandler(EventHandler<MessageSentEvent> handler) {
        eventHandlerCollection.addEventHandler(MESSAGE_SENT, handler);
    }

    public void addOnMessageReceivedEventHandler(EventHandler<MessageReceivedEvent> handler) {
        eventHandlerCollection.addEventHandler(MESSAGE_RECEIVED, handler);
    }

    public void removeOnMessageSentEventHandler(EventHandler<MessageSentEvent> handler) {
        eventHandlerCollection.removeEventHandler(MESSAGE_SENT, handler);
    }

    public void removeOnMessageReceivedEventHandler(EventHandler<MessageReceivedEvent> handler) {
        eventHandlerCollection.removeEventHandler(MESSAGE_RECEIVED, handler);
    }

    public void sendMessage(Message message) {
        sentMessages.add(message.setTimestamp(new Date())); // Sending message

        MessageSentEvent someEvent = new MessageSentEvent()
            .setMessageId(message.getId())
            .setTimestamp(message.getTimestamp())
            .setUserId(message.getUserId());

        eventHandlerCollection.fireEvent(MESSAGE_SENT, someEvent);
    }

    public void receiveMessages() {
        for (Message message : sentMessages) {
            MessageReceivedEvent messageReceivedEvent = new MessageReceivedEvent()
                .setMessageId(message.getId())
                .setTimestamp(message.getTimestamp())
                .setUserId(message.getUserId())
                .setContents(message.getContents());

            eventHandlerCollection.fireEvent(MESSAGE_RECEIVED, messageReceivedEvent);
        }
    }
}
