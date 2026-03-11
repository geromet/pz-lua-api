/*
 * Decompiled with CFR 0.152.
 */
package zombie.core;

import zombie.UsedFromLua;

@UsedFromLua
public final class Language {
    private final String name;
    private final String text;
    private final String base;
    private final boolean azerty;

    Language(String name, String text, String base, boolean azerty) {
        this.name = name;
        this.text = text;
        this.base = base;
        this.azerty = azerty;
    }

    public String name() {
        return this.name;
    }

    public String text() {
        return this.text;
    }

    public String base() {
        return this.base;
    }

    public boolean isAzerty() {
        return this.azerty;
    }

    public String toString() {
        return this.name;
    }
}

