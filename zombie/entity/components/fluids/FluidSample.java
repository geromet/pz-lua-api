/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.fluids;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidInstance;

@UsedFromLua
public class FluidSample {
    private static final ConcurrentLinkedDeque<FluidSample> pool = new ConcurrentLinkedDeque();
    private final ArrayList<FluidInstance> fluids = new ArrayList();
    private boolean sealed;
    private float amount;

    public static FluidSample Alloc() {
        FluidSample obj = pool.poll();
        if (obj == null) {
            obj = new FluidSample();
        }
        return obj;
    }

    protected static void Release(FluidSample obj) {
        obj.reset();
        assert (!Core.debug || !pool.contains(obj)) : "Object already in pool.";
        pool.offer(obj);
    }

    private FluidSample() {
    }

    public void release() {
        FluidSample.Release(this);
    }

    private void reset() {
        this.sealed = false;
        this.amount = 0.0f;
        if (!this.fluids.isEmpty()) {
            for (int i = 0; i < this.fluids.size(); ++i) {
                FluidInstance.Release(this.fluids.get(i));
            }
            this.fluids.clear();
        }
    }

    public void clear() {
        this.reset();
    }

    protected void addFluid(FluidInstance fluid) {
        if (!this.sealed) {
            this.fluids.add(fluid.copy());
            this.amount += fluid.getAmount();
        } else {
            DebugLog.General.error("FluidSample is sealed");
        }
    }

    protected FluidSample seal() {
        this.sealed = true;
        return this;
    }

    public FluidSample copy() {
        FluidSample copy = FluidSample.Alloc();
        for (int i = 0; i < this.fluids.size(); ++i) {
            copy.fluids.add(this.fluids.get(i).copy());
        }
        copy.sealed = true;
        copy.amount = this.amount;
        return copy;
    }

    public boolean isEmpty() {
        return this.fluids.isEmpty() || this.amount <= 0.0f;
    }

    public boolean isPureFluid() {
        return this.fluids.size() == 1;
    }

    public float getAmount() {
        return this.amount;
    }

    public int size() {
        return this.fluids.size();
    }

    public float getPercentage(int index) {
        if (index >= 0 && index < this.fluids.size()) {
            return this.fluids.get(index).getPercentage();
        }
        DebugLog.General.error("FluidSample index out of bounds");
        return 0.0f;
    }

    public Fluid getFluid(int index) {
        if (index >= 0 && index < this.fluids.size()) {
            return this.fluids.get(index).getFluid();
        }
        DebugLog.General.error("FluidSample index out of bounds");
        return null;
    }

    public FluidInstance getFluidInstance(int index) {
        if (index >= 0 && index < this.fluids.size()) {
            return this.fluids.get(index);
        }
        DebugLog.General.error("FluidSample index out of bounds");
        return null;
    }

    public FluidInstance getFluidInstance(Fluid fluid) {
        for (int i = 0; i < this.fluids.size(); ++i) {
            if (!this.fluids.get(i).getFluid().equals(fluid)) continue;
            return this.fluids.get(i);
        }
        return null;
    }

    public Fluid getPrimaryFluid() {
        if (this.isEmpty()) {
            return null;
        }
        if (this.fluids.size() == 1) {
            return this.fluids.get(0).getFluid();
        }
        FluidInstance primary = null;
        for (int i = 0; i < this.fluids.size(); ++i) {
            FluidInstance test = this.fluids.get(i);
            if (primary != null && !(test.getAmount() > primary.getAmount())) continue;
            primary = test;
        }
        return primary.getFluid();
    }

    public Color getColor() {
        float r = 0.0f;
        float g = 0.0f;
        float b = 0.0f;
        Color color = new Color();
        for (int i = 0; i < this.fluids.size(); ++i) {
            FluidInstance fluid = this.fluids.get(i);
            r += fluid.getColor().r * this.getPercentage(i);
            g += fluid.getColor().g * this.getPercentage(i);
            b += fluid.getColor().b * this.getPercentage(i);
        }
        color.set(r, g, b);
        return color;
    }

    public void scaleToAmount(float amount) {
        for (int i = 0; i < this.fluids.size(); ++i) {
            FluidInstance fluidInstance = this.fluids.get(i);
            fluidInstance.setAmount(amount * fluidInstance.getPercentage());
        }
        this.amount = amount;
    }

    public static FluidSample combine(FluidSample a, FluidSample b) {
        int i;
        FluidSample sample = FluidSample.Alloc();
        for (i = 0; i < a.size(); ++i) {
            sample.addFluid(a.getFluidInstance(i));
        }
        for (i = 0; i < b.size(); ++i) {
            FluidInstance existingFluid = sample.getFluidInstance(b.getFluid(i));
            if (existingFluid != null) {
                existingFluid.setAmount(existingFluid.getAmount() + b.getFluidInstance(i).getAmount());
                sample.amount += b.getFluidInstance(i).getAmount();
                continue;
            }
            sample.addFluid(b.getFluidInstance(i));
        }
        return sample.seal();
    }

    public FluidSample combineWith(FluidSample b) {
        FluidSample combined = FluidSample.combine(this, b);
        this.reset();
        for (int i = 0; i < combined.size(); ++i) {
            this.addFluid(combined.getFluidInstance(i));
        }
        this.amount = combined.getAmount();
        combined.release();
        return this.seal();
    }

    public static void Save(FluidSample fluidSample, ByteBuffer output) throws IOException {
        output.put(fluidSample.sealed ? (byte)1 : 0);
        output.putFloat(fluidSample.amount);
        output.putInt(fluidSample.size());
        for (int i = 0; i < fluidSample.size(); ++i) {
            FluidInstance.save(fluidSample.fluids.get(i), output);
        }
    }

    public static FluidSample Load(ByteBuffer input, int worldVersion) throws IOException {
        return FluidSample.Load(FluidSample.Alloc(), input, worldVersion);
    }

    public static FluidSample Load(FluidSample fluidSample, ByteBuffer input, int worldVersion) throws IOException {
        fluidSample.sealed = input.get() != 0;
        fluidSample.amount = input.getFloat();
        float amount = 0.0f;
        int size = input.getInt();
        for (int i = 0; i < size; ++i) {
            FluidInstance fluidInstance = FluidInstance.load(input, worldVersion);
            fluidSample.fluids.add(fluidInstance);
            amount += fluidInstance.getAmount();
        }
        if (amount != fluidSample.amount) {
            DebugLog.General.warn("Fluids amount mismatch with saved amount, correcting. save=" + fluidSample.amount + ", fluids=" + amount);
            fluidSample.amount = amount;
        }
        return fluidSample;
    }
}

