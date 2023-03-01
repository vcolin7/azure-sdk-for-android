// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.android.core.sampleapp;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.azure.android.core.util.events.EventHandler;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private String userId;
    private MessagingClient client;
    private EventHandler<MessagingClient.MessageSentEvent> messageSentHandler;
    private EventHandler<MessagingClient.MessageReceivedEvent> messageReceivedHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        client = new MessagingClient();

        if (userId == null) {
            userId = UUID.randomUUID().toString();
        }

        if (messageSentHandler == null) {
            messageSentHandler = event ->
                Toast.makeText(this, "Sent message with id: " + event.getMessageId(), Toast.LENGTH_SHORT).show();
        }

        if (messageReceivedHandler == null) {
            messageReceivedHandler = (MessagingClient.MessageReceivedEvent event) ->
                Toast.makeText(this, "Received message with id: " + event.getMessageId() + "and contents: "
                    + event.getContents(), Toast.LENGTH_SHORT).show();
        }

        client.addOnMessageSentEventHandler(messageSentHandler);
        client.addOnMessageReceivedEventHandler(messageReceivedHandler);
    }

    public void sendMessage(View view) {
        EditText text = (EditText) findViewById(R.id.editText1);
        String messageContents = text.getText().toString();

        client.sendMessage(new Message(userId, messageContents));
    }

    public void receiveMessages(View view) {
        client.receiveMessages();
    }

    public void addOnMessageSentEventHandler(View view) {
        client.addOnMessageSentEventHandler(messageSentHandler);
    }

    public void removeOnMessageSentEventHandler(View view) {
        client.removeOnMessageSentEventHandler(messageSentHandler);
    }

    public void addOnMessageReceivedEventHandler(View view) {
        client.addOnMessageReceivedEventHandler(messageReceivedHandler);
    }

    public void removeOnMessageReceivedEventHandler(View view) {
        client.removeOnMessageReceivedEventHandler(messageReceivedHandler);
    }
}
