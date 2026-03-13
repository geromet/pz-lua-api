/*
 * Decompiled with CFR 0.152.
 */
package zombie.radio.StorySounds;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.radio.StorySounds.DataPoint;
import zombie.radio.StorySounds.StorySound;

@UsedFromLua
public final class EventSound {
    protected String name;
    protected Color color = new Color(1.0f, 1.0f, 1.0f);
    protected ArrayList<DataPoint> dataPoints = new ArrayList();
    protected ArrayList<StorySound> storySounds = new ArrayList();

    public EventSound() {
        this("Unnamed");
    }

    public EventSound(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public ArrayList<DataPoint> getDataPoints() {
        return this.dataPoints;
    }

    public void setDataPoints(ArrayList<DataPoint> dataPoints) {
        this.dataPoints = dataPoints;
    }

    public ArrayList<StorySound> getStorySounds() {
        return this.storySounds;
    }

    public void setStorySounds(ArrayList<StorySound> storySounds) {
        this.storySounds = storySounds;
    }
}

