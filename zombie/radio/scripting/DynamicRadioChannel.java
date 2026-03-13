/*
 * Decompiled with CFR 0.152.
 */
package zombie.radio.scripting;

import zombie.UsedFromLua;
import zombie.radio.ChannelCategory;
import zombie.radio.scripting.RadioChannel;

@UsedFromLua
public final class DynamicRadioChannel
extends RadioChannel {
    public DynamicRadioChannel(String n, int freq, ChannelCategory c) {
        super(n, freq, c);
    }

    public DynamicRadioChannel(String n, int freq, ChannelCategory c, String guid) {
        super(n, freq, c, guid);
    }

    @Override
    public void LoadAiringBroadcast(String guid, int line) {
    }
}

