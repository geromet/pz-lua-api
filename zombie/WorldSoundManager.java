/*
 * Decompiled with CFR 0.152.
 */
package zombie;

import java.util.ArrayList;
import java.util.Stack;
import zombie.Lua.LuaEventManager;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.FishSchoolManager;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoClothingDryer;
import zombie.iso.objects.IsoClothingWasher;
import zombie.iso.objects.IsoCombinationWasherDryer;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoJukebox;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoStove;
import zombie.iso.objects.IsoTelevision;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerGUI;
import zombie.popman.MPDebugInfo;
import zombie.popman.ZombiePopulationManager;

@UsedFromLua
public final class WorldSoundManager {
    public static final WorldSoundManager instance = new WorldSoundManager();
    public final ArrayList<WorldSound> soundList = new ArrayList();
    private final Stack<WorldSound> freeSounds = new Stack();
    private static final ResultBiggestSound resultBiggestSound = new ResultBiggestSound();

    public void init(IsoCell cell) {
    }

    public void initFrame() {
    }

    public void KillCell() {
        for (WorldSound sound : this.soundList) {
            sound.source = null;
        }
        this.freeSounds.addAll(this.soundList);
        this.soundList.clear();
    }

    public WorldSound getNew() {
        if (this.freeSounds.isEmpty()) {
            return new WorldSound();
        }
        return this.freeSounds.pop();
    }

    public WorldSound addSound(Object source2, int x, int y, int z, int radius, int volume) {
        return this.addSound(source2, x, y, z, radius, volume, false, 0.0f, 1.0f);
    }

    public WorldSound addSound(Object source2, int x, int y, int z, int radius, int volume, boolean stressHumans) {
        return this.addSound(source2, x, y, z, radius, volume, stressHumans, 0.0f, 1.0f);
    }

    public WorldSound addSound(Object source2, int x, int y, int z, int radius, int volume, boolean stressHumans, float zombieIgnoreDist, float stressMod) {
        return this.addSound(source2, x, y, z, radius, volume, stressHumans, zombieIgnoreDist, stressMod, false, true, false, false, false);
    }

    public WorldSound addSoundRepeating(Object source2, int x, int y, int z, int radius, int volume, boolean stressHumans, float zombieIgnoreDist, float stressMod) {
        return this.addSound(source2, x, y, z, radius, volume, stressHumans, zombieIgnoreDist, stressMod, false, true, false, true, false);
    }

    public WorldSound addSound(Object source2, int x, int y, int z, int radius, int volume, boolean stressHumans, float zombieIgnoreDist, float stressMod, boolean sourceIsZombie, boolean doSend, boolean remote) {
        return this.addSound(source2, x, y, z, radius, volume, stressHumans, zombieIgnoreDist, stressMod, sourceIsZombie, doSend, remote, false, false);
    }

    public WorldSound addSound(Object source2, int x, int y, int z, int radius, int volume, boolean stressHumans, float zombieIgnoreDist, float stressMod, boolean sourceIsZombie, boolean doSend, boolean remote, boolean repeating, boolean stressAnimals) {
        short flags = 4;
        if (stressAnimals) {
            flags = (short)(flags | 1);
        }
        if (stressHumans) {
            flags = (short)(flags | 2);
        }
        return this.addSound(source2, x, y, z, radius, volume, zombieIgnoreDist, stressMod, sourceIsZombie, doSend, remote, repeating, flags);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public WorldSound addSound(Object source2, int x, int y, int z, int radius, int volume, float zombieIgnoreDist, float stressMod, boolean sourceIsZombie, boolean doSend, boolean remote, boolean repeating, short flags) {
        WorldSound s;
        if (radius <= 0) {
            return null;
        }
        ArrayList<WorldSound> arrayList = this.soundList;
        synchronized (arrayList) {
            s = this.getNew().init(source2, x, y, z, radius, volume, zombieIgnoreDist, stressMod, flags);
            s.repeating = repeating;
            if (source2 == null) {
                s.sourceIsZombie = sourceIsZombie;
            }
            if (!GameServer.server) {
                int hearing = SandboxOptions.instance.lore.hearing.getValue();
                if (hearing == 4) {
                    hearing = 1;
                }
                if (hearing == 5) {
                    hearing = 2;
                }
                int radiusMax = (int)PZMath.ceil((float)radius * this.getHearingMultiplier(hearing));
                int chunkMinX = (x - radiusMax) / 8;
                int chunkMinY = (y - radiusMax) / 8;
                int chunkMaxX = (int)Math.ceil(((float)x + (float)radiusMax) / 8.0f);
                int chunkMaxY = (int)Math.ceil(((float)y + (float)radiusMax) / 8.0f);
                for (int xx = chunkMinX; xx < chunkMaxX; ++xx) {
                    for (int yy = chunkMinY; yy < chunkMaxY; ++yy) {
                        IsoChunk c = IsoWorld.instance.currentCell.getChunk(xx, yy);
                        if (c == null) continue;
                        c.soundList.add(s);
                    }
                }
            }
            this.soundList.add(s);
            ZombiePopulationManager.instance.addWorldSound(s, doSend);
        }
        if (doSend) {
            if (GameClient.client) {
                GameClient.instance.sendWorldSound(s);
            } else if (GameServer.server) {
                GameServer.sendWorldSound(s, null);
            }
        }
        if (Core.debug && GameClient.client) {
            MPDebugInfo.AddDebugSound(s);
        }
        return s;
    }

    public WorldSound addSoundRepeating(Object source2, int x, int y, int z, int radius, int volume, boolean stressHumans, boolean stressAnimals) {
        return this.addSound(source2, x, y, z, radius, volume, stressHumans, 0.0f, 1.0f, false, true, false, true, stressAnimals);
    }

    public WorldSound addSoundRepeating(Object source2, int x, int y, int z, int radius, int volume, boolean stressHumans) {
        return this.addSoundRepeating(source2, x, y, z, radius, volume, stressHumans, 0.0f, 1.0f);
    }

    public WorldSound addSoundRepeating(Object source2, int x, int y, int z, int radius, int volume, short flags) {
        boolean sourceIsZombie = false;
        boolean doSend = true;
        boolean remote = false;
        boolean repeating = true;
        return this.addSound(source2, x, y, z, radius, volume, 0.0f, 1.0f, false, true, false, true, flags);
    }

    public WorldSound getSoundZomb(IsoZombie zom) {
        if (zom.soundSourceTarget == null) {
            return null;
        }
        if (zom.getCurrentSquare() == null) {
            return null;
        }
        IsoChunk chunk = zom.getCurrentSquare().chunk;
        ArrayList<WorldSound> soundList = chunk == null || GameServer.server ? this.soundList : chunk.soundList;
        for (int n = 0; n < soundList.size(); ++n) {
            WorldSound sound = soundList.get(n);
            if (zom.soundSourceTarget != sound.source || !sound.stressZombies) continue;
            return sound;
        }
        return null;
    }

    public WorldSound getSoundAnimal(IsoAnimal animal) {
        if (animal.getCurrentSquare() == null) {
            return null;
        }
        IsoChunk chunk = animal.getCurrentSquare().chunk;
        ArrayList<WorldSound> soundList = chunk == null || GameServer.server ? this.soundList : chunk.soundList;
        for (int n = 0; n < soundList.size(); ++n) {
            WorldSound sound = soundList.get(n);
            if (!sound.stresshumans && !sound.stressAnimals) continue;
            return sound;
        }
        return null;
    }

    public ResultBiggestSound getBiggestSoundZomb(int x, int y, int z, boolean ignoreBySameType, IsoZombie zom) {
        float largestSound = -1000000.0f;
        WorldSound largest = null;
        IsoChunk chunk = null;
        if (zom != null) {
            if (zom.getCurrentSquare() == null) {
                return resultBiggestSound.init(null, 0.0f);
            }
            chunk = zom.getCurrentSquare().chunk;
        }
        ArrayList<WorldSound> soundList = chunk == null || GameServer.server ? this.soundList : chunk.soundList;
        for (int n = 0; n < soundList.size(); ++n) {
            float tot;
            float radius;
            float dist;
            WorldSound sound = soundList.get(n);
            if (sound == null || !sound.stressZombies || sound.radius == 0 || (dist = IsoUtils.DistanceToSquared(x, y, z * 3, sound.x, sound.y, sound.z * 3)) > (radius = (float)sound.radius * this.getHearingMultiplier(zom)) * radius || dist < sound.zombieIgnoreDist * sound.zombieIgnoreDist && z == sound.z || ignoreBySameType && sound.sourceIsZombie) continue;
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(sound.x, sound.y, sound.z);
            IsoGridSquare sq2 = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            float delta = dist / (radius * radius);
            if (sq != null && sq2 != null && sq.getRoom() != sq2.getRoom()) {
                delta *= 1.2f;
                if (sq2.getRoom() == null || sq.getRoom() == null) {
                    delta *= 1.4f;
                }
            }
            if ((delta = 1.0f - delta) <= 0.0f) continue;
            if (delta > 1.0f) {
                delta = 1.0f;
            }
            if (!((tot = (float)sound.volume * delta) > largestSound)) continue;
            largestSound = tot;
            largest = sound;
        }
        return resultBiggestSound.init(largest, largestSound);
    }

    public float getSoundAttract(WorldSound sound, IsoZombie zom) {
        float radius;
        if (sound == null) {
            return 0.0f;
        }
        if (sound.radius == 0) {
            return 0.0f;
        }
        float dist = IsoUtils.DistanceToSquared(zom.getX(), zom.getY(), zom.getZ() * 3.0f, sound.x, sound.y, sound.z * 3);
        if (dist > (radius = (float)sound.radius * this.getHearingMultiplier(zom)) * radius) {
            return 0.0f;
        }
        if (dist < sound.zombieIgnoreDist * sound.zombieIgnoreDist && zom.getZ() == (float)sound.z) {
            return 0.0f;
        }
        if (sound.sourceIsZombie) {
            return 0.0f;
        }
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(sound.x, sound.y, sound.z);
        IsoGridSquare sq2 = IsoWorld.instance.currentCell.getGridSquare(zom.getX(), zom.getY(), zom.getZ());
        float delta = dist / (radius * radius);
        if (sq != null && sq2 != null && sq.getRoom() != sq2.getRoom()) {
            delta *= 1.2f;
            if (sq2.getRoom() == null || sq.getRoom() == null) {
                delta *= 1.4f;
            }
        }
        if ((delta = 1.0f - delta) <= 0.0f) {
            return 0.0f;
        }
        if (delta > 1.0f) {
            delta = 1.0f;
        }
        return (float)sound.volume * delta;
    }

    public float getSoundAttractAnimal(WorldSound sound, IsoAnimal animal) {
        if (sound == null) {
            return 0.0f;
        }
        if (sound.radius == 0) {
            return 0.0f;
        }
        float dist = IsoUtils.DistanceTo(animal.getX(), animal.getY(), animal.getZ() * 3.0f, sound.x, sound.y, sound.z * 3);
        float radiusBonus = 1.0f;
        if (animal.isWild()) {
            radiusBonus = 3.0f;
        }
        if (dist > (float)sound.radius * radiusBonus) {
            return 0.0f;
        }
        if (dist < sound.zombieIgnoreDist * sound.zombieIgnoreDist && animal.getZ() == (float)sound.z) {
            return 0.0f;
        }
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(sound.x, sound.y, sound.z);
        IsoGridSquare sq2 = IsoWorld.instance.currentCell.getGridSquare(animal.getX(), animal.getY(), animal.getZ());
        float delta = dist / (float)(sound.radius * sound.radius);
        if (sq != null && sq2 != null && sq.getRoom() != sq2.getRoom()) {
            delta *= 1.2f;
            if (sq2.getRoom() == null || sq.getRoom() == null) {
                delta *= 1.4f;
            }
        }
        if ((delta = 1.0f - delta) <= 0.0f) {
            return 0.0f;
        }
        if (delta > 1.0f) {
            delta = 1.0f;
        }
        return (float)sound.volume * delta;
    }

    public float getStressFromSounds(int x, int y, int z) {
        float ret = 0.0f;
        for (int i = 0; i < this.soundList.size(); ++i) {
            WorldSound sound = this.soundList.get(i);
            if (!sound.stresshumans || sound.radius == 0) continue;
            float dist = IsoUtils.DistanceManhatten(x, y, sound.x, sound.y);
            float delta = dist / (float)sound.radius;
            if ((delta = 1.0f - delta) <= 0.0f) continue;
            if (delta > 1.0f) {
                delta = 1.0f;
            }
            float tot = delta * sound.stressMod;
            ret += tot;
        }
        return ret;
    }

    public void update() {
        if (!GameServer.server) {
            for (int n = 0; n < IsoPlayer.numPlayers; ++n) {
                IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[n];
                if (chunkMap.ignore) continue;
                for (int y = 0; y < IsoChunkMap.chunkGridWidth; ++y) {
                    for (int x = 0; x < IsoChunkMap.chunkGridWidth; ++x) {
                        IsoChunk chunk = chunkMap.getChunk(x, y);
                        if (chunk == null) continue;
                        chunk.updateSounds();
                    }
                }
            }
        }
        int s = this.soundList.size();
        for (int n = 0; n < s; ++n) {
            WorldSound sound = this.soundList.get(n);
            if (sound == null || sound.life <= 0) {
                this.soundList.remove(n);
                this.freeSounds.push(sound);
                --n;
                --s;
                continue;
            }
            --sound.life;
        }
    }

    public void render() {
        if (!Core.debug || !DebugOptions.instance.worldSoundRender.getValue()) {
            return;
        }
        if (GameClient.client) {
            return;
        }
        if (GameServer.server && !ServerGUI.isCreated()) {
            return;
        }
        int hearing = SandboxOptions.instance.lore.hearing.getValue();
        if (hearing == 4) {
            hearing = 2;
        }
        if (hearing == 5) {
            hearing = 2;
        }
        float radiusMultiplier = this.getHearingMultiplier(hearing);
        for (int i = 0; i < this.soundList.size(); ++i) {
            WorldSound sound = this.soundList.get(i);
            float radius = (float)sound.radius * radiusMultiplier;
            int segments = 32;
            LineDrawer.DrawIsoCircle(sound.x, sound.y, sound.z, radius, 32, 1.0f, 1.0f, 1.0f, 1.0f);
        }
        if (GameServer.server) {
            return;
        }
        IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(0);
        if (chunkMap == null || chunkMap.ignore) {
            return;
        }
        for (int y = 0; y < IsoChunkMap.chunkGridWidth; ++y) {
            for (int x = 0; x < IsoChunkMap.chunkGridWidth; ++x) {
                IsoChunk chunk = chunkMap.getChunk(x, y);
                if (chunk == null) continue;
                for (int i = 0; i < chunk.soundList.size(); ++i) {
                    WorldSound sound = chunk.soundList.get(i);
                    float radius = (float)sound.radius * radiusMultiplier;
                    int segments = 32;
                    LineDrawer.DrawIsoCircle(sound.x, sound.y, sound.z, radius, 32, 0.0f, 1.0f, 1.0f, 1.0f);
                    int chunksPerWidth = 8;
                    float left = (float)(chunk.wx * 8) + 0.1f;
                    float top = (float)(chunk.wy * 8) + 0.1f;
                    float right = (float)((chunk.wx + 1) * 8) - 0.1f;
                    float bottom = (float)((chunk.wy + 1) * 8) - 0.1f;
                    LineDrawer.DrawIsoRect(left, top, right - left, bottom - top, sound.z, 0.0f, 1.0f, 1.0f);
                }
            }
        }
    }

    public float getHearingMultiplier(IsoZombie zombie) {
        if (zombie == null) {
            return this.getHearingMultiplier(2);
        }
        return this.getHearingMultiplier(zombie.hearing) * zombie.getWornItemsHearingMultiplier() * zombie.getWeatherHearingMultiplier();
    }

    public float getHearingMultiplier(int hearing) {
        if (hearing == 1) {
            return 3.0f;
        }
        if (hearing == 3) {
            return 0.45f;
        }
        return 1.0f;
    }

    @UsedFromLua
    public static final class WorldSound {
        public Object source;
        public int life = 1;
        public int radius;
        public boolean stresshumans;
        public boolean stressZombies;
        public boolean stressAnimals;
        public int volume;
        public int x;
        public int y;
        public int z;
        public float zombieIgnoreDist;
        public boolean sourceIsZombie;
        public boolean sourceIsPlayer;
        public boolean sourceIsPlayerBase;
        public float stressMod = 1.0f;
        public boolean repeating;

        private boolean isSourceIsPlayerBase(Object source2) {
            return source2 instanceof IsoGenerator || source2 instanceof IsoJukebox || source2 instanceof IsoTelevision || source2 instanceof IsoRadio || source2 instanceof IsoStove || source2 instanceof IsoClothingWasher || source2 instanceof IsoClothingDryer || source2 instanceof IsoCombinationWasherDryer;
        }

        public WorldSound init(Object source2, int x, int y, int z, int radius, int volume) {
            return this.init(source2, x, y, z, radius, volume, false, 0.0f, 1.0f);
        }

        public WorldSound init(Object source2, int x, int y, int z, int radius, int volume, boolean stresshumans) {
            return this.init(source2, x, y, z, radius, volume, stresshumans, 0.0f, 1.0f);
        }

        public WorldSound init(Object source2, int x, int y, int z, int radius, int volume, boolean stresshumans, float zombieIgnoreDist, float stressMod) {
            short flags = 4;
            if (stresshumans) {
                flags = (short)(flags | 2);
            }
            return this.init(source2, x, y, z, radius, volume, zombieIgnoreDist, stressMod, flags);
        }

        public WorldSound init(Object source2, int x, int y, int z, int radius, int volume, float zombieIgnoreDist, float stressMod, short flags) {
            this.source = source2;
            this.life = 16;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.volume = volume;
            this.stresshumans = (flags & 2) != 0;
            this.stressAnimals = (flags & 1) != 0;
            this.stressZombies = (flags & 4) != 0;
            this.zombieIgnoreDist = zombieIgnoreDist;
            this.stressMod = stressMod;
            this.sourceIsPlayer = source2 instanceof IsoPlayer;
            this.sourceIsPlayerBase = this.isSourceIsPlayerBase(source2);
            this.sourceIsZombie = source2 instanceof IsoZombie;
            this.repeating = false;
            LuaEventManager.triggerEvent("OnWorldSound", x, y, z, radius, volume, source2);
            if (!GameClient.client) {
                FishSchoolManager.getInstance().addSoundNoise(x, y, radius / 6);
            }
            return this;
        }

        public WorldSound init(boolean sourceIsZombie, int x, int y, int z, int radius, int volume, boolean stressHumans, float zombieIgnoreDist, float stressMod) {
            WorldSound sound = this.init(null, x, y, z, radius, volume, stressHumans, zombieIgnoreDist, stressMod);
            sound.sourceIsZombie = sourceIsZombie;
            return sound;
        }
    }

    public static final class ResultBiggestSound {
        public WorldSound sound;
        public float attract;

        public ResultBiggestSound init(WorldSound sound, float attract) {
            this.sound = sound;
            this.attract = attract;
            return this;
        }
    }
}

