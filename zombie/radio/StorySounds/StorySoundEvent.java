/*
 * Decompiled with CFR 0.152.
 */
package zombie.radio.StorySounds;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.radio.StorySounds.EventSound;

@UsedFromLua
public final class StorySoundEvent {
    protected String name;
    protected ArrayList<EventSound> eventSounds = new ArrayList();

    public StorySoundEvent() {
        this("Unnamed");
    }

    public StorySoundEvent(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<EventSound> getEventSounds() {
        return this.eventSounds;
    }

    public void setEventSounds(ArrayList<EventSound> eventSounds) {
        this.eventSounds = eventSounds;
    }
}

