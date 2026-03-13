/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderSettings;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.objects.IsoLightSwitch;

@UsedFromLua
public class IsoLightSource {
    public static int nextId = 1;
    public int id;
    public int x;
    public int y;
    public int z;
    public float r;
    public float g;
    public float b;
    public float rJni;
    public float gJni;
    public float bJni;
    public int radius;
    public boolean active;
    public boolean wasActive;
    public boolean activeJni;
    public int life = -1;
    public int startlife = -1;
    public IsoBuilding localToBuilding;
    public boolean hydroPowered;
    public ArrayList<IsoLightSwitch> switches = new ArrayList(0);
    public IsoChunk chunk;
    public Object lightMap;

    public IsoLightSource(int x, int y, int z, float r, float g, float b, int radius) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
        this.radius = radius;
        this.active = true;
    }

    public IsoLightSource(int x, int y, int z, float r, float g, float b, int radius, IsoBuilding building) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
        this.radius = radius;
        this.active = true;
        this.localToBuilding = building;
    }

    public IsoLightSource(int x, int y, int z, float r, float g, float b, int radius, int life) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
        this.radius = radius;
        this.active = true;
        this.startlife = this.life = life;
    }

    @Deprecated
    public void update() {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
        if (!(!this.hydroPowered || sq.hasGridPower() || sq != null && sq.haveElectricity())) {
            this.active = false;
            return;
        }
        if (!this.active) {
            return;
        }
        if (this.localToBuilding != null) {
            this.r = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.7f;
            this.g = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.7f;
            this.b = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.7f;
        }
        if (this.life > 0) {
            --this.life;
        }
        if (this.localToBuilding != null && sq != null) {
            this.r = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.8f * IsoGridSquare.rmod * 0.7f;
            this.g = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.8f * IsoGridSquare.gmod * 0.7f;
            this.b = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.8f * IsoGridSquare.bmod * 0.7f;
        }
        for (int xx = this.x - this.radius; xx < this.x + this.radius; ++xx) {
            for (int yy = this.y - this.radius; yy < this.y + this.radius; ++yy) {
                for (int zz = 0; zz < 8; ++zz) {
                    float dist;
                    sq = IsoWorld.instance.currentCell.getGridSquare(xx, yy, zz);
                    if (sq == null || this.localToBuilding != null && this.localToBuilding != sq.getBuilding()) continue;
                    LosUtil.TestResults test = LosUtil.lineClear(sq.getCell(), PZMath.fastfloor(this.x), PZMath.fastfloor(this.y), this.z, sq.getX(), sq.getY(), sq.getZ(), false);
                    if ((sq.getX() != this.x || sq.getY() != this.y || sq.getZ() != this.z) && test == LosUtil.TestResults.Blocked || (dist = Math.abs(sq.getZ() - this.z) <= 1 ? IsoUtils.DistanceTo(this.x, this.y, 0.0f, sq.getX(), sq.getY(), 0.0f) : IsoUtils.DistanceTo(this.x, this.y, this.z, sq.getX(), sq.getY(), sq.getZ())) > (float)this.radius) continue;
                    float del = dist / (float)this.radius;
                    del = 1.0f - del;
                    del *= del;
                    if (this.life > -1) {
                        del *= (float)this.life / (float)this.startlife;
                    }
                    float totR = del * this.r * 2.0f;
                    float totG = del * this.g * 2.0f;
                    float totB = del * this.b * 2.0f;
                    sq.setLampostTotalR(sq.getLampostTotalR() + totR);
                    sq.setLampostTotalG(sq.getLampostTotalG() + totG);
                    sq.setLampostTotalB(sq.getLampostTotalB() + totB);
                }
            }
        }
    }

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return this.z;
    }

    public void setZ(int z) {
        z = Math.max(-32, z);
        this.z = z = Math.min(31, z);
    }

    public float getR() {
        return this.r;
    }

    public void setR(float r) {
        this.r = r;
    }

    public float getG() {
        return this.g;
    }

    public void setG(float g) {
        this.g = g;
    }

    public float getB() {
        return this.b;
    }

    public void setB(float b) {
        this.b = b;
    }

    public int getRadius() {
        return this.radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean bActive) {
        this.active = bActive;
    }

    public boolean wasActive() {
        return this.wasActive;
    }

    public void setWasActive(boolean bWasActive) {
        this.wasActive = bWasActive;
    }

    public ArrayList<IsoLightSwitch> getSwitches() {
        return this.switches;
    }

    public void setSwitches(ArrayList<IsoLightSwitch> switches) {
        this.switches = switches;
    }

    public void clearInfluence() {
        for (int xx = this.x - this.radius; xx < this.x + this.radius; ++xx) {
            for (int yy = this.y - this.radius; yy < this.y + this.radius; ++yy) {
                for (int zz = 0; zz < 8; ++zz) {
                    IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(xx, yy, zz);
                    if (sq == null) continue;
                    sq.setLampostTotalR(0.0f);
                    sq.setLampostTotalG(0.0f);
                    sq.setLampostTotalB(0.0f);
                }
            }
        }
    }

    public boolean isInBounds(int minX, int minY, int maxX, int maxY) {
        return this.x >= minX && this.x < maxX && this.y >= minY && this.y < maxY;
    }

    public boolean isInBounds() {
        IsoChunkMap[] chunkMap = IsoWorld.instance.currentCell.chunkMap;
        for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
            int maxY;
            if (chunkMap[pn].ignore) continue;
            int minX = chunkMap[pn].getWorldXMinTiles();
            int maxX = chunkMap[pn].getWorldXMaxTiles();
            int minY = chunkMap[pn].getWorldYMinTiles();
            if (!this.isInBounds(minX, minY, maxX, maxY = chunkMap[pn].getWorldYMaxTiles())) continue;
            return true;
        }
        return false;
    }

    public boolean isHydroPowered() {
        return this.hydroPowered;
    }

    public IsoBuilding getLocalToBuilding() {
        return this.localToBuilding;
    }
}

