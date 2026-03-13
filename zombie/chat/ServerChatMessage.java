/*
 * Decompiled with CFR 0.152.
 */
package zombie.chat;

import zombie.UsedFromLua;
import zombie.chat.ChatBase;
import zombie.chat.ChatMessage;

@UsedFromLua
public class ServerChatMessage
extends ChatMessage {
    public ServerChatMessage(ChatBase chat, String text) {
        super(chat, text);
        super.setAuthor("Server");
        this.setServerAuthor(true);
    }

    @Override
    public void setAuthor(String author) {
        throw new UnsupportedOperationException();
    }
}

