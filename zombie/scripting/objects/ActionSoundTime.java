/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import zombie.UsedFromLua;

@UsedFromLua
public enum ActionSoundTime {
    ACTION_START("action_start"),
    ANIMATION_EVENT("animation_event"),
    ANIMATION_START("animation_start");

    private final String id;

    private ActionSoundTime(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }

    public static ActionSoundTime fromValue(String value) {
        for (ActionSoundTime actionSoundTime : ActionSoundTime.values()) {
            if (!actionSoundTime.id.equals(value)) continue;
            return actionSoundTime;
        }
        throw new IllegalArgumentException("No enum constant for value: " + value);
    }
}

