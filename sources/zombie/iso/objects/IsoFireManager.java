/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import fmod.fmod.FMODSoundEmitter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Stack;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.DummySoundEmitter;
import zombie.audio.parameters.ParameterFireSize;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.debug.DebugLog;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoFire;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public class IsoFireManager {
    private static final Stack<IsoFire> updateStack = new Stack();
    private static final HashSet<IsoGameCharacter> charactersOnFire = new HashSet();
    public static double redOscilator;
    public static double greenOscilator;
    public static double blueOscilator;
    public static double redOscilatorRate;
    public static double greenOscilatorRate;
    public static double blueOscilatorRate;
    public static double redOscilatorVal;
    public static double greenOscilatorVal;
    public static double blueOscilatorVal;
    public static double oscilatorSpeedScalar;
    public static double oscilatorEffectScalar;
    public static int maxFireObjects;
    public static int fireRecalcDelay;
    public static int fireRecalc;
    public static boolean lightCalcFromBurningCharacters;
    public static float fireAlpha;
    public static float smokeAlpha;
    @UsedFromLua
    public static final float FIRE_ANIM_DELAY = 0.5f;
    public static float smokeAnimDelay;
    @UsedFromLua
    public static final ColorInfo FIRE_TINT_MOD;
    public static ColorInfo smokeTintMod;
    public static final ArrayList<IsoFire> FireStack;
    public static final ArrayList<IsoGameCharacter> CharactersOnFire_Stack;
    private static final FireSounds fireSounds;

    public static void Add(IsoFire newFire) {
        if (FireStack.contains(newFire)) {
            System.out.println("IsoFireManager.Add already added fire, ignoring");
            return;
        }
        if (FireStack.size() < maxFireObjects) {
            FireStack.add(newFire);
        } else {
            IsoObject oldestFireObject = null;
            int oldestAge = 0;
            for (int i = 0; i < FireStack.size(); ++i) {
                if (IsoFireManager.FireStack.get((int)i).age <= oldestAge) continue;
                oldestAge = IsoFireManager.FireStack.get((int)i).age;
                oldestFireObject = FireStack.get(i);
            }
            if (oldestFireObject != null && ((IsoFire)oldestFireObject).square != null) {
                ((IsoFire)oldestFireObject).square.getProperties().unset(IsoFlagType.burning);
                ((IsoFire)oldestFireObject).square.getProperties().unset(IsoFlagType.smoke);
                oldestFireObject.RemoveAttachedAnims();
                ((IsoFire)oldestFireObject).removeFromWorld();
                oldestFireObject.removeFromSquare();
            }
            FireStack.add(newFire);
        }
    }

    public static void AddBurningCharacter(IsoGameCharacter burningCharacter) {
        for (int i = 0; i < CharactersOnFire_Stack.size(); ++i) {
            if (CharactersOnFire_Stack.get(i) != burningCharacter) continue;
            return;
        }
        CharactersOnFire_Stack.add(burningCharacter);
    }

    public static void Fire_LightCalc(IsoGridSquare fireSquare, IsoGridSquare testSquare, int playerIndex) {
        if (testSquare == null || fireSquare == null) {
            return;
        }
        int dist = 0;
        int fireAreaOfEffect = 8;
        dist += Math.abs(testSquare.getX() - fireSquare.getX());
        dist += Math.abs(testSquare.getY() - fireSquare.getY());
        if ((dist += Math.abs(testSquare.getZ() - fireSquare.getZ())) <= 8) {
            float lightInfluenceR = 0.024875f * (float)(8 - dist);
            float lightInfluenceG = lightInfluenceR * 0.6f;
            float lightInfluenceB = lightInfluenceR * 0.4f;
            if (testSquare.getLightInfluenceR() == null) {
                testSquare.setLightInfluenceR(new ArrayList<Float>());
            }
            testSquare.getLightInfluenceR().add(Float.valueOf(lightInfluenceR));
            if (testSquare.getLightInfluenceG() == null) {
                testSquare.setLightInfluenceG(new ArrayList<Float>());
            }
            testSquare.getLightInfluenceG().add(Float.valueOf(lightInfluenceG));
            if (testSquare.getLightInfluenceB() == null) {
                testSquare.setLightInfluenceB(new ArrayList<Float>());
            }
            testSquare.getLightInfluenceB().add(Float.valueOf(lightInfluenceB));
            ColorInfo lightInfo = testSquare.lighting[playerIndex].lightInfo();
            lightInfo.r += lightInfluenceR;
            lightInfo.g += lightInfluenceG;
            lightInfo.b += lightInfluenceB;
            if (lightInfo.r > 1.0f) {
                lightInfo.r = 1.0f;
            }
            if (lightInfo.g > 1.0f) {
                lightInfo.g = 1.0f;
            }
            if (lightInfo.b > 1.0f) {
                lightInfo.b = 1.0f;
            }
        }
    }

    public static void LightTileWithFire(IsoGridSquare testSquare) {
    }

    public static void explode(IsoCell cell, IsoGridSquare gridSquare, int power) {
        if (gridSquare == null) {
            return;
        }
        fireRecalc = 1;
        for (int x = -2; x <= 2; ++x) {
            for (int y = -2; y <= 2; ++y) {
                for (int z = 0; z <= 1; ++z) {
                    IsoGridSquare surround = cell.getGridSquare(gridSquare.getX() + x, gridSquare.getY() + y, gridSquare.getZ() + z);
                    if (surround == null || Rand.Next(100) >= power || !IsoFire.CanAddFire(surround, true)) continue;
                    IsoFireManager.StartFire(cell, surround, true, Rand.Next(100, 250 + power));
                    surround.BurnWalls(true);
                }
            }
        }
    }

    @Deprecated
    public static void MolotovSmash(IsoCell cell, IsoGridSquare gridSquare) {
    }

    public static void Remove(IsoFire dyingFire) {
        if (!FireStack.contains(dyingFire)) {
            System.out.println("IsoFireManager.Remove unknown fire, ignoring");
            return;
        }
        FireStack.remove(dyingFire);
    }

    public static void RemoveBurningCharacter(IsoGameCharacter burningCharacter) {
        CharactersOnFire_Stack.remove(burningCharacter);
    }

    public static void StartFire(IsoCell cell, IsoGridSquare gridSquare, boolean igniteOnAny, int fireStartingEnergy, int life) {
        if (fireStartingEnergy <= 0) {
            return;
        }
        if (gridSquare.getFloor() != null && gridSquare.getFloor().getSprite() != null) {
            fireStartingEnergy -= gridSquare.getFloor().getSprite().firerequirement;
        }
        if (fireStartingEnergy < 5) {
            fireStartingEnergy = 5;
        }
        if (!IsoFire.CanAddFire(gridSquare, igniteOnAny)) {
            return;
        }
        if (GameClient.client) {
            DebugLog.General.warn("The StartFire function was called on Client");
            return;
        }
        if (GameServer.server) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.StartFire, gridSquare.getX(), (float)gridSquare.getY(), gridSquare, igniteOnAny, fireStartingEnergy, life, false);
        }
        IsoFire newFire = new IsoFire(cell, gridSquare, igniteOnAny, fireStartingEnergy, life);
        IsoFireManager.Add(newFire);
        gridSquare.getObjects().add(newFire);
        if (Rand.Next(5) == 0) {
            WorldSoundManager.instance.addSound(newFire, gridSquare.getX(), gridSquare.getY(), gridSquare.getZ(), 20, 20);
        }
        if (PerformanceSettings.fboRenderChunk) {
            gridSquare.invalidateRenderChunkLevel(64L);
        }
    }

    public static void StartSmoke(IsoCell cell, IsoGridSquare gridSquare, boolean igniteOnAny, int fireStartingEnergy, int life) {
        if (!IsoFire.CanAddSmoke(gridSquare, igniteOnAny)) {
            return;
        }
        if (GameClient.client) {
            ByteBufferWriter b = GameClient.connection.startPacket();
            PacketTypes.PacketType.StartFire.doPacket(b);
            b.putInt(gridSquare.getX());
            b.putInt(gridSquare.getY());
            b.putInt(gridSquare.getZ());
            b.putInt(fireStartingEnergy);
            b.putBoolean(igniteOnAny);
            b.putInt(life);
            b.putBoolean(true);
            PacketTypes.PacketType.StartFire.send(GameClient.connection);
            return;
        }
        if (GameServer.server) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.StartFire, gridSquare.getX(), (float)gridSquare.getY(), gridSquare, igniteOnAny, fireStartingEnergy, life, true);
            return;
        }
        IsoFire newFire = new IsoFire(cell, gridSquare, igniteOnAny, fireStartingEnergy, life, true);
        IsoFireManager.Add(newFire);
        gridSquare.getObjects().add(newFire);
        if (PerformanceSettings.fboRenderChunk) {
            gridSquare.invalidateRenderChunkLevel(64L);
        }
    }

    public static void StartFire(IsoCell cell, IsoGridSquare gridSquare, boolean igniteOnAny, int fireStartingEnergy) {
        if (GameClient.client) {
            return;
        }
        IsoFireManager.StartFire(cell, gridSquare, igniteOnAny, fireStartingEnergy, 0);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void addCharacterOnFire(IsoGameCharacter character) {
        HashSet<IsoGameCharacter> hashSet = charactersOnFire;
        synchronized (hashSet) {
            charactersOnFire.add(character);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void deleteCharacterOnFire(IsoGameCharacter character) {
        HashSet<IsoGameCharacter> hashSet = charactersOnFire;
        synchronized (hashSet) {
            charactersOnFire.remove(character);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void Update() {
        HashSet<IsoGameCharacter> hashSet = charactersOnFire;
        synchronized (hashSet) {
            charactersOnFire.forEach(IsoGameCharacter::SpreadFireMP);
        }
        redOscilatorVal = Math.sin(redOscilator += blueOscilatorRate * oscilatorSpeedScalar);
        greenOscilatorVal = Math.sin(greenOscilator += blueOscilatorRate * oscilatorSpeedScalar);
        blueOscilatorVal = Math.sin(blueOscilator += blueOscilatorRate * oscilatorSpeedScalar);
        redOscilatorVal = (redOscilatorVal + 1.0) / 2.0;
        greenOscilatorVal = (greenOscilatorVal + 1.0) / 2.0;
        blueOscilatorVal = (blueOscilatorVal + 1.0) / 2.0;
        redOscilatorVal *= oscilatorEffectScalar;
        greenOscilatorVal *= oscilatorEffectScalar;
        blueOscilatorVal *= oscilatorEffectScalar;
        updateStack.clear();
        updateStack.addAll(FireStack);
        for (int i = 0; i < updateStack.size(); ++i) {
            IsoFire fire = (IsoFire)updateStack.get(i);
            if (fire.getObjectIndex() == -1 || !FireStack.contains(fire)) continue;
            fire.update();
        }
        if (--fireRecalc < 0) {
            fireRecalc = fireRecalcDelay;
        }
        fireSounds.update();
    }

    public static void updateSound(IsoFire fire) {
        fireSounds.addFire(fire);
    }

    public static void stopSound(IsoFire fire) {
        fireSounds.removeFire(fire);
    }

    public static void RemoveAllOn(IsoGridSquare sq) {
        for (int n = FireStack.size() - 1; n >= 0; --n) {
            IsoFire fire = FireStack.get(n);
            if (fire.square != sq) continue;
            fire.extinctFire();
        }
    }

    public static void Reset() {
        FireStack.clear();
        CharactersOnFire_Stack.clear();
        fireSounds.Reset();
    }

    static {
        redOscilatorRate = 0.1f;
        greenOscilatorRate = 0.13f;
        blueOscilatorRate = 0.0876f;
        oscilatorSpeedScalar = 15.6f;
        oscilatorEffectScalar = 0.0039f;
        maxFireObjects = 75;
        fireRecalc = fireRecalcDelay = 25;
        fireAlpha = 1.0f;
        smokeAlpha = 0.3f;
        smokeAnimDelay = 0.5f;
        FIRE_TINT_MOD = new ColorInfo(1.0f, 1.0f, 1.0f, 1.0f);
        smokeTintMod = new ColorInfo(0.5f, 0.5f, 0.5f, 1.0f);
        FireStack = new ArrayList();
        CharactersOnFire_Stack = new ArrayList();
        fireSounds = new FireSounds(20);
    }

    private static final class FireSounds {
        private final ArrayList<IsoFire> fires = new ArrayList();
        private final Slot[] slots;
        private final Comparator<IsoFire> comp = new Comparator<IsoFire>(this){
            final /* synthetic */ FireSounds this$0;
            {
                FireSounds fireSounds = this$0;
                Objects.requireNonNull(fireSounds);
                this.this$0 = fireSounds;
            }

            @Override
            public int compare(IsoFire a, IsoFire b) {
                float bScore;
                float aScore = this.this$0.getClosestListener((float)a.square.x + 0.5f, (float)a.square.y + 0.5f, a.square.z);
                if (aScore > (bScore = this.this$0.getClosestListener((float)b.square.x + 0.5f, (float)b.square.y + 0.5f, b.square.z))) {
                    return 1;
                }
                if (aScore < bScore) {
                    return -1;
                }
                return 0;
            }
        };

        private FireSounds(int numSlots) {
            this.slots = PZArrayUtil.newInstance(Slot.class, numSlots, Slot::new);
        }

        private void addFire(IsoFire fire) {
            if (!this.fires.contains(fire)) {
                this.fires.add(fire);
            }
        }

        private void removeFire(IsoFire fire) {
            this.fires.remove(fire);
        }

        private void update() {
            int j;
            IsoFire fire;
            int i;
            if (GameServer.server) {
                return;
            }
            for (int i2 = 0; i2 < this.slots.length; ++i2) {
                this.slots[i2].playing = false;
            }
            if (this.fires.isEmpty()) {
                this.stopNotPlaying();
                return;
            }
            Collections.sort(this.fires, this.comp);
            int count = Math.min(this.fires.size(), this.slots.length);
            for (i = 0; i < count; ++i) {
                fire = this.fires.get(i);
                if (!this.shouldPlay(fire) || (j = this.getExistingSlot(fire)) == -1) continue;
                this.slots[j].playSound(fire);
            }
            for (i = 0; i < count; ++i) {
                fire = this.fires.get(i);
                if (!this.shouldPlay(fire) || (j = this.getExistingSlot(fire)) != -1) continue;
                j = this.getFreeSlot();
                this.slots[j].playSound(fire);
            }
            this.stopNotPlaying();
            this.fires.clear();
        }

        private float getClosestListener(float soundX, float soundY, float soundZ) {
            float minDist = Float.MAX_VALUE;
            for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                IsoPlayer chr = IsoPlayer.players[i];
                if (chr == null || chr.getCurrentSquare() == null) continue;
                float px = chr.getX();
                float py = chr.getY();
                float pz = chr.getZ();
                float distSq = IsoUtils.DistanceToSquared(px, py, pz * 3.0f, soundX, soundY, soundZ * 3.0f);
                if (!((distSq *= PZMath.pow(chr.getHearDistanceModifier(), 2.0f)) < minDist)) continue;
                minDist = distSq;
            }
            return minDist;
        }

        private boolean shouldPlay(IsoFire fire) {
            if (fire == null) {
                return false;
            }
            if (fire.getObjectIndex() == -1) {
                return false;
            }
            if (fire.smoke) {
                return true;
            }
            return fire.lifeStage <= 4;
        }

        private int getExistingSlot(IsoFire fire) {
            for (int i = 0; i < this.slots.length; ++i) {
                if (this.slots[i].fire != fire) continue;
                return i;
            }
            return -1;
        }

        private int getFreeSlot() {
            for (int i = 0; i < this.slots.length; ++i) {
                if (this.slots[i].playing) continue;
                return i;
            }
            return -1;
        }

        private void stopNotPlaying() {
            for (int i = 0; i < this.slots.length; ++i) {
                Slot slot = this.slots[i];
                if (slot.playing) continue;
                slot.stopPlaying();
                slot.fire = null;
            }
        }

        private void Reset() {
            for (int i = 0; i < this.slots.length; ++i) {
                this.slots[i].stopPlaying();
                this.slots[i].fire = null;
                this.slots[i].playing = false;
            }
        }

        private static final class Slot {
            private IsoFire fire;
            private BaseSoundEmitter emitter;
            private final ParameterFireSize parameterFireSize = new ParameterFireSize();
            private long instance;
            private boolean playing;

            private Slot() {
            }

            private void playSound(IsoFire fire) {
                if (this.emitter == null) {
                    if (Core.soundDisabled) {
                        this.emitter = new DummySoundEmitter();
                    } else {
                        FMODSoundEmitter emitter1 = new FMODSoundEmitter();
                        emitter1.addParameter(this.parameterFireSize);
                        this.emitter = emitter1;
                    }
                }
                this.emitter.setPos((float)fire.square.x + 0.5f, (float)fire.square.y + 0.5f, fire.square.z);
                int parameter = switch (fire.lifeStage) {
                    case 0, 1 -> 0;
                    case 2, 4 -> 1;
                    case 3 -> 2;
                    default -> 0;
                };
                this.parameterFireSize.setSize(parameter);
                if (fire.isCampfire()) {
                    if (!this.emitter.isPlaying("CampfireRunning")) {
                        this.instance = this.emitter.playSoundImpl("CampfireRunning", (IsoObject)null);
                    }
                } else if (!fire.smoke && !this.emitter.isPlaying("Fire")) {
                    this.instance = this.emitter.playSoundImpl("Fire", (IsoObject)null);
                }
                this.fire = fire;
                this.playing = true;
                this.emitter.tick();
            }

            void stopPlaying() {
                if (this.emitter == null || this.instance == 0L) {
                    if (this.emitter != null && !this.emitter.isEmpty()) {
                        this.emitter.tick();
                    }
                    return;
                }
                if (this.emitter.hasSustainPoints(this.instance)) {
                    this.emitter.triggerCue(this.instance);
                    this.instance = 0L;
                    return;
                }
                this.emitter.stopAll();
                this.instance = 0L;
            }
        }
    }
}

