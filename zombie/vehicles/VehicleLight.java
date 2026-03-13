/*
 * Decompiled with CFR 0.152.
 */
package zombie.vehicles;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.joml.Vector3f;
import zombie.UsedFromLua;

@UsedFromLua
public final class VehicleLight {
    public boolean active;
    public final Vector3f offset = new Vector3f();
    public float dist = 16.0f;
    public float intensity = 1.0f;
    public float dot = 0.96f;
    public int focusing;
    public float r = 1.0f;
    public float g = 0.8235294f;
    public float b = 0.7058824f;

    public boolean getActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Deprecated
    public int getFocusing() {
        return this.focusing;
    }

    public float getIntensity() {
        return this.intensity;
    }

    @Deprecated
    public float getDistanization() {
        return this.dist;
    }

    @Deprecated
    public boolean canFocusingUp() {
        return this.focusing != 0;
    }

    @Deprecated
    public boolean canFocusingDown() {
        return this.focusing != 1;
    }

    @Deprecated
    public void setFocusingUp() {
        if (this.focusing == 0) {
            return;
        }
        this.focusing = this.focusing < 4 ? 4 : (this.focusing < 10 ? 10 : (this.focusing < 30 ? 30 : (this.focusing < 100 ? 100 : 0)));
    }

    @Deprecated
    public void setFocusingDown() {
        if (this.focusing == 1) {
            return;
        }
        this.focusing = this.focusing == 0 ? 100 : (this.focusing > 30 ? 30 : (this.focusing > 10 ? 10 : (this.focusing > 4 ? 4 : 1)));
    }

    public void save(ByteBuffer output) throws IOException {
        output.put((byte)(this.active ? 1 : 0));
        output.putFloat(this.offset.x);
        output.putFloat(this.offset.y);
        output.putFloat(this.intensity);
        output.putFloat(this.dist);
        output.putInt(this.focusing);
    }

    public void load(ByteBuffer input, int worldVersion) throws IOException {
        this.active = input.get() != 0;
        this.offset.x = input.getFloat();
        this.offset.y = input.getFloat();
        this.intensity = input.getFloat();
        this.dist = input.getFloat();
        this.focusing = input.getInt();
    }
}

