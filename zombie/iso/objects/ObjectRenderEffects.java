/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.random.Rand;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.RenderEffectType;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameServer;

@UsedFromLua
public class ObjectRenderEffects {
    public static final boolean ENABLED = true;
    private static final ArrayDeque<ObjectRenderEffects> pool = new ArrayDeque();
    public double x1;
    public double y1;
    public double x2;
    public double y2;
    public double x3;
    public double y3;
    public double x4;
    public double y4;
    private double tx1;
    private double ty1;
    private double tx2;
    private double ty2;
    private double tx3;
    private double ty3;
    private double tx4;
    private double ty4;
    private double lx1;
    private double ly1;
    private double lx2;
    private double ly2;
    private double lx3;
    private double ly3;
    private double lx4;
    private double ly4;
    private double maxX;
    private double maxY;
    private float curTime;
    private float maxTime;
    private float totalTime;
    private float totalMaxTime;
    private RenderEffectType type;
    private IsoObject parent;
    private boolean finish;
    private boolean isTree;
    private boolean isBig;
    private boolean gust;
    private int windType = 1;
    private static final float T_MOD = 1.0f;
    private static int windCount;
    private static int windCountTree;
    private static final int EFFECTS_COUNT = 15;
    private static final int TYPE_COUNT = 3;
    private static final ObjectRenderEffects[][] WIND_EFFECTS;
    private static final ObjectRenderEffects[][] WIND_EFFECTS_TREES;
    private static final ArrayList<ObjectRenderEffects> DYNAMIC_EFFECTS;
    private static ObjectRenderEffects randomRustle;
    private static float randomRustleTime;
    private static float randomRustleTotalTime;
    private static int randomRustleTarget;
    private static int randomRustleType;

    public static ObjectRenderEffects alloc() {
        return pool.isEmpty() ? new ObjectRenderEffects() : pool.pop();
    }

    public static void release(ObjectRenderEffects o) {
        assert (!pool.contains(o));
        pool.push(o.reset());
    }

    private ObjectRenderEffects() {
    }

    private ObjectRenderEffects reset() {
        this.parent = null;
        this.finish = false;
        this.isBig = false;
        this.isTree = false;
        this.curTime = 0.0f;
        this.maxTime = 0.0f;
        this.totalTime = 0.0f;
        this.totalMaxTime = 0.0f;
        this.x1 = 0.0;
        this.y1 = 0.0;
        this.x2 = 0.0;
        this.y2 = 0.0;
        this.x3 = 0.0;
        this.y3 = 0.0;
        this.x4 = 0.0;
        this.y4 = 0.0;
        this.tx1 = 0.0;
        this.ty1 = 0.0;
        this.tx2 = 0.0;
        this.ty2 = 0.0;
        this.tx3 = 0.0;
        this.ty3 = 0.0;
        this.tx4 = 0.0;
        this.ty4 = 0.0;
        this.swapTargetToLast();
        return this;
    }

    public static ObjectRenderEffects getNew(IsoObject parent, RenderEffectType t, boolean reuseEqualType) {
        return ObjectRenderEffects.getNew(parent, t, reuseEqualType, false);
    }

    public static ObjectRenderEffects getNew(IsoObject parent, RenderEffectType t, boolean reuseEqualType, boolean dontAdd) {
        if (GameServer.server) {
            return null;
        }
        if (t == RenderEffectType.Hit_Door && !Core.getInstance().getOptionDoDoorSpriteEffects()) {
            return null;
        }
        ObjectRenderEffects e = null;
        try {
            boolean reusing = false;
            if (reuseEqualType && parent != null && parent.getObjectRenderEffects() != null && parent.getObjectRenderEffects().type == t) {
                e = parent.getObjectRenderEffects();
                reusing = true;
            } else {
                e = ObjectRenderEffects.alloc();
            }
            e.type = t;
            e.parent = parent;
            e.finish = false;
            e.isBig = false;
            e.totalTime = 0.0f;
            switch (t) {
                case Hit_Tree_Shudder: {
                    e.totalMaxTime = Rand.Next(45.0f, 60.0f) * 1.0f;
                    break;
                }
                case Vegetation_Rustle: {
                    e.totalMaxTime = Rand.Next(45.0f, 60.0f) * 1.0f;
                    if (parent == null || !(parent instanceof IsoTree)) break;
                    IsoTree isoTree = (IsoTree)parent;
                    e.isTree = true;
                    e.isBig = isoTree.size > 4;
                    break;
                }
                case Hit_Door: {
                    e.totalMaxTime = Rand.Next(15.0f, 30.0f) * 1.0f;
                }
            }
            if (!reusing && parent != null && parent.getWindRenderEffects() != null && Core.getInstance().getOptionDoWindSpriteEffects()) {
                e.copyMainFromOther(parent.getWindRenderEffects());
            }
            if (!reusing && !dontAdd) {
                DYNAMIC_EFFECTS.add(e);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return e;
    }

    public static ObjectRenderEffects getNextWindEffect(int windType, boolean isTreeLike) {
        int id = windType - 1;
        if (id < 0 || id >= 3) {
            return null;
        }
        if (isTreeLike) {
            if (++windCountTree >= 15) {
                windCountTree = 0;
            }
            return WIND_EFFECTS_TREES[id][windCountTree];
        }
        if (++windCount >= 15) {
            windCount = 0;
        }
        return WIND_EFFECTS[id][windCount];
    }

    public static void init() {
        if (GameServer.server) {
            return;
        }
        for (int id = 0; id < 3; ++id) {
            ObjectRenderEffects r;
            int i;
            for (i = 0; i < 15; ++i) {
                r = new ObjectRenderEffects();
                r.windType = id + 1;
                ObjectRenderEffects.WIND_EFFECTS[id][i] = r;
            }
            for (i = 0; i < 15; ++i) {
                r = new ObjectRenderEffects();
                r.isTree = true;
                r.windType = id + 1;
                ObjectRenderEffects.WIND_EFFECTS_TREES[id][i] = r;
            }
        }
        DYNAMIC_EFFECTS.clear();
        windCount = 0;
        windCountTree = 0;
        randomRustle = null;
        randomRustleTime = 0.0f;
        randomRustleTotalTime = 0.0f;
        randomRustleTarget = 0;
    }

    public boolean update() {
        this.curTime += 1.0f * GameTime.getInstance().getMultiplier();
        this.totalTime += 1.0f * GameTime.getInstance().getMultiplier();
        if (this.curTime > this.maxTime) {
            if (this.finish) {
                return false;
            }
            this.curTime = 0.0f;
            this.swapTargetToLast();
            float t = ClimateManager.clamp01(this.totalTime / this.totalMaxTime);
            float ti = 1.0f - t;
            switch (this.type) {
                case Hit_Tree_Shudder: {
                    double x;
                    if (this.totalTime > this.totalMaxTime) {
                        this.maxTime = 10.0f;
                        this.tx1 = 0.0;
                        this.tx2 = 0.0;
                        this.finish = true;
                        break;
                    }
                    this.maxTime = (3.0f + 15.0f * t) * 1.0f;
                    this.tx1 = x = this.isBig ? (double)Rand.Next(-0.01f + -0.08f * ti, 0.01f + 0.08f * ti) : (double)Rand.Next(-0.02f + -0.16f * ti, 0.02f + 0.16f * ti);
                    this.tx2 = x;
                    break;
                }
                case Vegetation_Rustle: {
                    double y;
                    if (this.totalTime > this.totalMaxTime) {
                        this.maxTime = 3.0f;
                        this.tx1 = 0.0;
                        this.tx2 = 0.0;
                        this.finish = true;
                        break;
                    }
                    this.maxTime = (2.0f + 6.0f * t) * 1.0f;
                    double x = this.isBig ? (double)Rand.Next(-0.00625f, 0.00625f) : (double)Rand.Next(-0.015f, 0.015f);
                    double d = y = this.isBig ? (double)Rand.Next(-0.00625f, 0.00625f) : (double)Rand.Next(-0.015f, 0.015f);
                    if (ClimateManager.getWindTickFinal() < 0.15) {
                        x *= 0.6;
                        y *= 0.6;
                    }
                    this.tx1 = x;
                    this.ty1 = y;
                    this.tx2 = x;
                    this.ty2 = y;
                    break;
                }
                case Hit_Door: {
                    if (this.totalTime > this.totalMaxTime) {
                        this.maxTime = 3.0f;
                        this.tx1 = 0.0;
                        this.tx2 = 0.0;
                        this.finish = true;
                        break;
                    }
                    this.maxTime = (1.0f + 2.0f * t) * 1.0f;
                    double x = Rand.Next(-0.005f, 0.005f);
                    double y = Rand.Next(-0.0075f, 0.0075f);
                    this.tx1 = x;
                    this.ty1 = y;
                    this.tx2 = x;
                    this.ty2 = y;
                    this.tx3 = x;
                    this.ty3 = y;
                    this.tx4 = x;
                    this.ty4 = y;
                    break;
                }
                default: {
                    this.finish = true;
                }
            }
        }
        this.lerpAll(this.curTime / this.maxTime);
        if (this.parent != null && this.parent.getWindRenderEffects() != null && Core.getInstance().getOptionDoWindSpriteEffects()) {
            this.add(this.parent.getWindRenderEffects());
        }
        return true;
    }

    private void update(float wind, float angle) {
        this.curTime += 1.0f * GameTime.getInstance().getMultiplier();
        if (this.curTime >= this.maxTime) {
            this.swapTargetToLast();
            if (this.isTree) {
                double x;
                float skew = 0.0f;
                float skewY = 0.04f;
                if (this.windType == 1) {
                    skew = 0.6f;
                    wind = wind <= 0.08f ? 0.0f : (wind - 0.08f) / 0.92f;
                } else if (this.windType == 2) {
                    skew = 0.3f;
                    skewY = 0.06f;
                    wind = wind <= 0.15f ? 0.0f : (wind - 0.15f) / 0.85f;
                } else if (this.windType == 3) {
                    skew = 0.15f;
                    wind = wind <= 0.3f ? 0.0f : (wind - 0.3f) / 0.7f;
                }
                float windInv = ClimateManager.clamp01(1.0f - wind);
                this.curTime = 0.0f;
                this.maxTime = Rand.Next(20.0f + 100.0f * windInv, 70.0f + 200.0f * windInv) * 1.0f;
                if (wind <= 0.01f || !Core.getInstance().getOptionDoWindSpriteEffects()) {
                    this.tx1 = 0.0;
                    this.tx2 = 0.0;
                    this.ty1 = 0.0;
                    this.ty2 = 0.0;
                    return;
                }
                float windShakeMod = 0.6f * wind + 0.4f * (wind * wind);
                if (this.gust) {
                    x = Rand.Next(-0.1f + 0.6f * wind, 1.0f) * angle;
                    if (Rand.Next(0.0f, 1.0f) > Rand.Next(0.0f, 0.75f * wind)) {
                        this.gust = false;
                    }
                } else {
                    x = Rand.Next(-0.1f, 0.2f) * angle;
                    this.gust = true;
                }
                this.tx1 = x *= (double)(skew * windShakeMod);
                this.tx2 = x;
                double y = Rand.Next(-1.0f, 1.0f);
                this.ty1 = y *= 0.01 + (double)(skewY * windShakeMod);
                y = Rand.Next(-1.0f, 1.0f);
                this.ty2 = y *= 0.01 + (double)(skewY * windShakeMod);
            } else {
                double x;
                float skew = 0.0f;
                if (this.windType == 1) {
                    skew = 0.575f;
                    wind = wind <= 0.02f ? 0.0f : (wind - 0.02f) / 0.98f;
                } else if (this.windType == 2) {
                    skew = 0.375f;
                    wind = wind <= 0.2f ? 0.0f : (wind - 0.2f) / 0.8f;
                } else if (this.windType == 3) {
                    skew = 0.175f;
                    wind = wind <= 0.6f ? 0.0f : (wind - 0.6f) / 0.4f;
                }
                float windInv = ClimateManager.clamp01(1.0f - wind);
                this.curTime = 0.0f;
                this.maxTime = Rand.Next(20.0f + 50.0f * windInv, 60.0f + 100.0f * windInv) * 1.0f;
                if (wind <= 0.05f || !Core.getInstance().getOptionDoWindSpriteEffects()) {
                    this.tx1 = 0.0;
                    this.tx2 = 0.0;
                    this.ty1 = 0.0;
                    this.ty2 = 0.0;
                    return;
                }
                float windShakeMod = 0.55f * wind + 0.45f * (wind * wind);
                if (this.gust) {
                    x = Rand.Next(-0.1f + 0.9f * wind, 1.0f) * angle;
                    if (Rand.Next(0.0f, 1.0f) > Rand.Next(0.0f, 0.95f * wind)) {
                        this.gust = false;
                    }
                } else {
                    x = Rand.Next(-0.1f, 0.2f) * angle;
                    this.gust = true;
                }
                this.tx1 = x *= (double)(0.025f + skew * windShakeMod);
                this.tx2 = x;
                if (wind > 0.5f) {
                    double y = Rand.Next(-1.0f, 1.0f);
                    this.ty1 = y *= (double)(0.05f * windShakeMod);
                    y = Rand.Next(-1.0f, 1.0f);
                    this.ty2 = y *= (double)(0.05f * windShakeMod);
                } else {
                    this.ty1 = 0.0;
                    this.ty2 = 0.0;
                }
            }
        } else {
            this.lerpAll(this.curTime / this.maxTime);
        }
    }

    private void updateOLD(float wind, float angle) {
        this.curTime += 1.0f * GameTime.getInstance().getMultiplier();
        if (this.curTime >= this.maxTime) {
            this.curTime = 0.0f;
            float windInv = ClimateManager.clamp01(1.0f - wind);
            this.maxTime = Rand.Next(20.0f + 100.0f * windInv, 70.0f + 200.0f * windInv) * 1.0f;
            this.swapTargetToLast();
            float skew = wind;
            wind = ClimateManager.clamp01(wind * 1.25f);
            double x = Rand.Next(-0.65f, 0.65f);
            x += (double)(skew * angle * 0.7f);
            this.tx1 = x *= (double)(0.4f * wind);
            this.tx2 = x;
            double y = Rand.Next(-1.0f, 1.0f);
            this.ty1 = y *= (double)(0.05f * wind);
            y = Rand.Next(-1.0f, 1.0f);
            this.ty2 = y *= (double)(0.05f * wind);
        } else {
            this.lerpAll(this.curTime / this.maxTime);
        }
    }

    private void lerpAll(float t) {
        this.x1 = ClimateManager.clerp(t, (float)this.lx1, (float)this.tx1);
        this.y1 = ClimateManager.clerp(t, (float)this.ly1, (float)this.ty1);
        this.x2 = ClimateManager.clerp(t, (float)this.lx2, (float)this.tx2);
        this.y2 = ClimateManager.clerp(t, (float)this.ly2, (float)this.ty2);
        this.x3 = ClimateManager.clerp(t, (float)this.lx3, (float)this.tx3);
        this.y3 = ClimateManager.clerp(t, (float)this.ly3, (float)this.ty3);
        this.x4 = ClimateManager.clerp(t, (float)this.lx4, (float)this.tx4);
        this.y4 = ClimateManager.clerp(t, (float)this.ly4, (float)this.ty4);
    }

    private void swapTargetToLast() {
        this.lx1 = this.tx1;
        this.ly1 = this.ty1;
        this.lx2 = this.tx2;
        this.ly2 = this.ty2;
        this.lx3 = this.tx3;
        this.ly3 = this.ty3;
        this.lx4 = this.tx4;
        this.ly4 = this.ty4;
    }

    public void copyMainFromOther(ObjectRenderEffects other) {
        this.x1 = other.x1;
        this.y1 = other.y1;
        this.x2 = other.x2;
        this.y2 = other.y2;
        this.x3 = other.x3;
        this.y3 = other.y3;
        this.x4 = other.x4;
        this.y4 = other.y4;
    }

    public void add(ObjectRenderEffects other) {
        this.x1 += other.x1;
        this.y1 += other.y1;
        this.x2 += other.x2;
        this.y2 += other.y2;
        this.x3 += other.x3;
        this.y3 += other.y3;
        this.x4 += other.x4;
        this.y4 += other.y4;
    }

    public static void updateStatic() {
        if (GameServer.server) {
            return;
        }
        try {
            float wind = (float)ClimateManager.getWindTickFinal();
            float angle = ClimateManager.getInstance().getWindAngleIntensity();
            angle = angle < 0.0f ? -1.0f : 1.0f;
            for (int id = 0; id < 3; ++id) {
                ObjectRenderEffects r;
                int i;
                for (i = 0; i < 15; ++i) {
                    r = WIND_EFFECTS[id][i];
                    r.update(wind, angle);
                }
                for (i = 0; i < 15; ++i) {
                    r = WIND_EFFECTS_TREES[id][i];
                    r.update(wind, angle);
                }
            }
            if ((randomRustleTime += 1.0f * GameTime.getInstance().getMultiplier()) > randomRustleTotalTime && randomRustle == null) {
                float windInv = 1.0f - wind;
                randomRustle = ObjectRenderEffects.getNew(null, RenderEffectType.Vegetation_Rustle, false, true);
                ObjectRenderEffects.randomRustle.isBig = false;
                if (wind > 0.45f && Rand.Next(0.0f, 1.0f) < Rand.Next(0.0f, 0.8f * wind)) {
                    ObjectRenderEffects.randomRustle.isBig = true;
                }
                randomRustleType = Rand.Next(3);
                randomRustleTarget = Rand.Next(15);
                randomRustleTime = 0.0f;
                randomRustleTotalTime = Rand.Next(400.0f + 400.0f * windInv, 1200.0f + 3200.0f * windInv);
            }
            if (randomRustle != null) {
                if (!randomRustle.update()) {
                    ObjectRenderEffects.release(randomRustle);
                    randomRustle = null;
                } else {
                    ObjectRenderEffects o = WIND_EFFECTS_TREES[randomRustleType][randomRustleTarget];
                    o.add(randomRustle);
                }
            }
            for (int i = DYNAMIC_EFFECTS.size() - 1; i >= 0; --i) {
                ObjectRenderEffects o = DYNAMIC_EFFECTS.get(i);
                if (o.update()) continue;
                if (o.parent != null) {
                    o.parent.removeRenderEffect(o);
                }
                DYNAMIC_EFFECTS.remove(i);
                ObjectRenderEffects.release(o);
            }
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
    }

    static {
        WIND_EFFECTS = new ObjectRenderEffects[3][15];
        WIND_EFFECTS_TREES = new ObjectRenderEffects[3][15];
        DYNAMIC_EFFECTS = new ArrayList();
    }
}

