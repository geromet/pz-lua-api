/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.BodyDamage;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.BodyDamage.Metabolics;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.random.Rand;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.SyncPlayerStatsPacket;
import zombie.scripting.objects.CharacterProfession;
import zombie.scripting.objects.MoodleType;

@UsedFromLua
public final class Fitness {
    private IsoGameCharacter parent;
    private HashMap<String, Float> regularityMap = new HashMap();
    private int fitnessLvl;
    private int strLvl;
    private final HashMap<String, Integer> stiffnessTimerMap = new HashMap();
    private final HashMap<String, Float> stiffnessIncMap = new HashMap();
    private final ArrayList<String> bodypartToIncStiffness = new ArrayList();
    private final HashMap<String, FitnessExercise> exercises = new HashMap();
    private final HashMap<String, Long> exeTimer = new HashMap();
    private int lastUpdate = -1;
    private FitnessExercise currentExe;
    private static final int HOURS_FOR_STIFFNESS = 12;
    private static final float BASE_STIFFNESS_INC = 0.5f;
    private static final float BASE_ENDURANCE_RED = 0.015f;
    private static final float BASE_REGULARITY_INC = 0.08f;
    private static final float BASE_REGULARITY_DEC = 0.002f;
    private static final float BASE_PAIN_INC = 2.5f;

    public Fitness(IsoGameCharacter parent) {
        this.setParent(parent);
    }

    public void update() {
        int i;
        Number number;
        int currentMin = GameTime.getInstance().getMinutes() / 10;
        if (this.lastUpdate == -1) {
            this.lastUpdate = currentMin;
        }
        if (currentMin == this.lastUpdate) {
            return;
        }
        this.lastUpdate = currentMin;
        ArrayList<String> toremove = new ArrayList<String>();
        this.decreaseRegularity();
        for (String currentBodypart : this.stiffnessTimerMap.keySet()) {
            Integer timer;
            number = timer = this.stiffnessTimerMap.get(currentBodypart);
            timer = timer - 1;
            if (timer <= 0) {
                toremove.add(currentBodypart);
                this.bodypartToIncStiffness.add(currentBodypart);
                continue;
            }
            this.stiffnessTimerMap.put(currentBodypart, timer);
        }
        for (i = 0; i < toremove.size(); ++i) {
            this.stiffnessTimerMap.remove(toremove.get(i));
        }
        for (i = 0; i < this.bodypartToIncStiffness.size(); ++i) {
            String bodypartType = this.bodypartToIncStiffness.get(i);
            Float stiffnessLeft = this.stiffnessIncMap.get(bodypartType);
            if (stiffnessLeft == null) {
                return;
            }
            number = stiffnessLeft;
            stiffnessLeft = Float.valueOf(stiffnessLeft.floatValue() - 1.0f);
            this.increasePain(bodypartType);
            if (stiffnessLeft.floatValue() <= 0.0f) {
                this.bodypartToIncStiffness.remove(i);
                this.stiffnessIncMap.remove(bodypartType);
                --i;
                continue;
            }
            this.stiffnessIncMap.put(bodypartType, stiffnessLeft);
        }
    }

    private void decreaseRegularity() {
        for (String currentExe : this.regularityMap.keySet()) {
            if (!this.exeTimer.containsKey(currentExe) || GameTime.getInstance().getCalender().getTimeInMillis() - this.exeTimer.get(currentExe) <= 86400000L) continue;
            float currentValue = this.regularityMap.get(currentExe).floatValue();
            this.regularityMap.put(currentExe, Float.valueOf(currentValue -= 0.002f));
        }
    }

    private void increasePain(String bodypartType) {
        BodyPart part;
        int i;
        if ("arms".equals(bodypartType)) {
            for (i = BodyPartType.ForeArm_L.index(); i < BodyPartType.UpperArm_R.index() + 1; ++i) {
                part = this.parent.getBodyDamage().getBodyPart(BodyPartType.FromIndex(i));
                part.setStiffness(part.getStiffness() + 2.5f);
            }
        }
        if ("legs".equals(bodypartType)) {
            for (i = BodyPartType.UpperLeg_L.index(); i < BodyPartType.LowerLeg_R.index() + 1; ++i) {
                part = this.parent.getBodyDamage().getBodyPart(BodyPartType.FromIndex(i));
                part.setStiffness(part.getStiffness() + 2.5f);
            }
        }
        if ("chest".equals(bodypartType)) {
            BodyPart part2 = this.parent.getBodyDamage().getBodyPart(BodyPartType.Torso_Upper);
            part2.setStiffness(part2.getStiffness() + 2.5f);
        }
        if ("abs".equals(bodypartType)) {
            BodyPart part3 = this.parent.getBodyDamage().getBodyPart(BodyPartType.Torso_Lower);
            part3.setStiffness(part3.getStiffness() + 2.5f);
        }
    }

    public void setCurrentExercise(String type) {
        this.currentExe = this.exercises.get(type);
    }

    public void exerciseRepeat() {
        IsoGameCharacter isoGameCharacter;
        this.fitnessLvl = this.parent.getPerkLevel(PerkFactory.Perks.Fitness);
        this.strLvl = this.parent.getPerkLevel(PerkFactory.Perks.Strength);
        this.incRegularity();
        this.reduceEndurance();
        this.incFutureStiffness();
        this.incStats();
        this.updateExeTimer();
        if (GameServer.server && (isoGameCharacter = this.parent) instanceof IsoPlayer) {
            IsoPlayer isoPlayer = (IsoPlayer)isoGameCharacter;
            GameServer.sendSyncPlayerFields(isoPlayer, (byte)32);
        }
    }

    private void updateExeTimer() {
        this.exeTimer.put(this.currentExe.type, GameTime.getInstance().getCalender().getTimeInMillis());
    }

    public void incRegularity() {
        float baseInc = 0.08f;
        int logMod = 4;
        double incLog = Math.log((float)this.fitnessLvl / 5.0f + 4.0f);
        baseInc = (float)((double)baseInc * (Math.log(5.0) / incLog));
        Float result = this.regularityMap.get(this.currentExe.type);
        if (result == null) {
            result = Float.valueOf(0.0f);
        }
        result = Float.valueOf(result.floatValue() + baseInc);
        result = Float.valueOf(Math.min(Math.max(result.floatValue(), 0.0f), 100.0f));
        this.regularityMap.put(this.currentExe.type, result);
    }

    public void reduceEndurance() {
        IsoGameCharacter isoGameCharacter;
        float baseRed = 0.015f;
        Float reg = this.regularityMap.get(this.currentExe.type);
        if (reg == null) {
            reg = Float.valueOf(0.0f);
        }
        int logMod = 50;
        double incLog = Math.log(reg.floatValue() / 50.0f + 50.0f);
        baseRed = (float)((double)baseRed * (incLog / Math.log(51.0)));
        if (this.currentExe.metabolics == Metabolics.FitnessHeavy) {
            baseRed *= 1.3f;
        }
        baseRed *= (float)(1 + this.parent.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) / 3);
        if (!GameClient.client) {
            this.parent.getStats().remove(CharacterStat.ENDURANCE, baseRed);
        }
        if (GameServer.server && (isoGameCharacter = this.parent) instanceof IsoPlayer) {
            IsoPlayer isoPlayer = (IsoPlayer)isoGameCharacter;
            INetworkPacket.send(isoPlayer, PacketTypes.PacketType.SyncPlayerStats, this.parent, SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.ENDURANCE));
        }
    }

    public void incFutureStiffness() {
        Float reg = this.regularityMap.get(this.currentExe.type);
        if (reg == null) {
            reg = Float.valueOf(0.0f);
        }
        for (int i = 0; i < this.currentExe.stiffnessInc.size(); ++i) {
            Float currentStiffnessInc;
            float baseRed = 0.5f;
            String bodyPart = this.currentExe.stiffnessInc.get(i);
            if (!this.stiffnessTimerMap.containsKey(bodyPart) && !this.bodypartToIncStiffness.contains(bodyPart)) {
                this.stiffnessTimerMap.put(bodyPart, 72);
            }
            if ((currentStiffnessInc = this.stiffnessIncMap.get(bodyPart)) == null) {
                currentStiffnessInc = Float.valueOf(0.0f);
            }
            baseRed *= (120.0f - reg.floatValue()) / 170.0f;
            if (this.currentExe.metabolics == Metabolics.FitnessHeavy) {
                baseRed *= 1.3f;
            }
            currentStiffnessInc = Float.valueOf(currentStiffnessInc.floatValue() + (baseRed *= (float)(1 + this.parent.getMoodles().getMoodleLevel(MoodleType.TIRED) / 3)));
            currentStiffnessInc = Float.valueOf(Math.min(currentStiffnessInc.floatValue(), 150.0f));
            this.stiffnessIncMap.put(bodyPart, currentStiffnessInc);
        }
    }

    public void incStats() {
        float str = 0.0f;
        float fit = 0.0f;
        for (int i = 0; i < this.currentExe.stiffnessInc.size(); ++i) {
            String bodypart = this.currentExe.stiffnessInc.get(i);
            if ("arms".equals(bodypart)) {
                str += 4.0f;
            }
            if ("chest".equals(bodypart)) {
                str += 2.0f;
            }
            if ("legs".equals(bodypart)) {
                fit += 4.0f;
            }
            if (!"abs".equals(bodypart)) continue;
            fit += 2.0f;
        }
        if (this.strLvl > 5) {
            str *= (float)(1 + (this.strLvl - 5) / 10);
        }
        if (this.fitnessLvl > 5) {
            fit *= (float)(1 + (this.fitnessLvl - 5) / 10);
        }
        str *= this.currentExe.xpModifier;
        fit *= this.currentExe.xpModifier;
        if (GameServer.server) {
            IsoGameCharacter isoGameCharacter = this.parent;
            if (isoGameCharacter instanceof IsoPlayer) {
                IsoPlayer isoPlayer = (IsoPlayer)isoGameCharacter;
                GameServer.addXp(isoPlayer, PerkFactory.Perks.Strength, (int)str);
                GameServer.addXp(isoPlayer, PerkFactory.Perks.Fitness, (int)fit);
            }
        } else if (!GameClient.client) {
            this.parent.getXp().AddXP(PerkFactory.Perks.Strength, str);
            this.parent.getXp().AddXP(PerkFactory.Perks.Fitness, fit);
        }
    }

    public void resetValues() {
        this.stiffnessIncMap.clear();
        this.stiffnessTimerMap.clear();
        this.regularityMap.clear();
    }

    public void removeStiffnessValue(String type) {
        this.stiffnessIncMap.remove(type);
        this.stiffnessTimerMap.remove(type);
    }

    public void save(ByteBuffer output) {
        output.putInt(this.stiffnessIncMap.size());
        for (String str : this.stiffnessIncMap.keySet()) {
            GameWindow.WriteString(output, str);
            output.putFloat(this.stiffnessIncMap.get(str).floatValue());
        }
        output.putInt(this.stiffnessTimerMap.size());
        for (String str : this.stiffnessTimerMap.keySet()) {
            GameWindow.WriteString(output, str);
            output.putInt(this.stiffnessTimerMap.get(str));
        }
        output.putInt(this.regularityMap.size());
        for (String str : this.regularityMap.keySet()) {
            GameWindow.WriteString(output, str);
            output.putFloat(this.regularityMap.get(str).floatValue());
        }
        output.putInt(this.bodypartToIncStiffness.size());
        for (int i = 0; i < this.bodypartToIncStiffness.size(); ++i) {
            GameWindow.WriteString(output, this.bodypartToIncStiffness.get(i));
        }
        output.putInt(this.exeTimer.size());
        for (String str : this.exeTimer.keySet()) {
            GameWindow.WriteString(output, str);
            output.putLong(this.exeTimer.get(str));
        }
    }

    public void load(ByteBuffer input, int worldVersion) {
        int i;
        int size = input.getInt();
        if (size > 0) {
            for (i = 0; i < size; ++i) {
                this.stiffnessIncMap.put(GameWindow.ReadString(input), Float.valueOf(input.getFloat()));
            }
        }
        if ((size = input.getInt()) > 0) {
            for (i = 0; i < size; ++i) {
                this.stiffnessTimerMap.put(GameWindow.ReadString(input), input.getInt());
            }
        }
        if ((size = input.getInt()) > 0) {
            for (i = 0; i < size; ++i) {
                this.regularityMap.put(GameWindow.ReadString(input), Float.valueOf(input.getFloat()));
            }
        }
        if ((size = input.getInt()) > 0) {
            for (i = 0; i < size; ++i) {
                this.bodypartToIncStiffness.add(GameWindow.ReadString(input));
            }
        }
        if ((size = input.getInt()) > 0) {
            for (i = 0; i < size; ++i) {
                this.exeTimer.put(GameWindow.ReadString(input), input.getLong());
            }
        }
    }

    public boolean onGoingStiffness() {
        return !this.bodypartToIncStiffness.isEmpty();
    }

    public int getCurrentExeStiffnessTimer(String type) {
        return this.stiffnessTimerMap.get(type = type.split(",")[0]) != null ? this.stiffnessTimerMap.get(type) : 0;
    }

    public float getCurrentExeStiffnessInc(String type) {
        return this.stiffnessIncMap.get(type = type.split(",")[0]) != null ? this.stiffnessIncMap.get(type).floatValue() : 0.0f;
    }

    public IsoGameCharacter getParent() {
        return this.parent;
    }

    public void setParent(IsoGameCharacter parent) {
        this.parent = parent;
    }

    public float getRegularity(String type) {
        Float result = this.regularityMap.get(type);
        if (result == null) {
            result = Float.valueOf(0.0f);
        }
        return result.floatValue();
    }

    public HashMap<String, Float> getRegularityMap() {
        return this.regularityMap;
    }

    public void setRegularityMap(HashMap<String, Float> regularityMap) {
        this.regularityMap = regularityMap;
    }

    public void init() {
        if (!this.exercises.isEmpty()) {
            return;
        }
        KahluaTableImpl exercisesType = (KahluaTableImpl)LuaManager.env.rawget("FitnessExercises");
        if (exercisesType == null) {
            return;
        }
        KahluaTableImpl exercisesList = (KahluaTableImpl)exercisesType.rawget("exercisesType");
        if (exercisesList == null) {
            return;
        }
        for (Map.Entry<Object, Object> objectObjectEntry : exercisesList.delegate.entrySet()) {
            this.exercises.put((String)objectObjectEntry.getKey(), new FitnessExercise((KahluaTableImpl)objectObjectEntry.getValue()));
        }
        this.initRegularityMapProfession();
    }

    public void initRegularityMapProfession() {
        if (!this.regularityMap.isEmpty()) {
            return;
        }
        boolean fireman = false;
        boolean fitnessinstructor = false;
        boolean securityguard = false;
        if (this.parent.getDescriptor().isCharacterProfession(CharacterProfession.FITNESS_INSTRUCTOR)) {
            fitnessinstructor = true;
        }
        if (this.parent.getDescriptor().isCharacterProfession(CharacterProfession.FIRE_OFFICER)) {
            fireman = true;
        }
        if (this.parent.getDescriptor().isCharacterProfession(CharacterProfession.SECURITY_GUARD)) {
            securityguard = true;
        }
        if (!(fireman || fitnessinstructor || securityguard)) {
            return;
        }
        for (String s : this.exercises.keySet()) {
            float reg = Rand.Next(7, 12);
            if (fireman) {
                reg = Rand.Next(10, 20);
            } else if (fitnessinstructor) {
                reg = Rand.Next(40, 60);
            }
            this.regularityMap.put(s, Float.valueOf(reg));
        }
    }

    public static final class FitnessExercise {
        String type;
        Metabolics metabolics;
        ArrayList<String> stiffnessInc;
        float xpModifier = 1.0f;

        public FitnessExercise(KahluaTableImpl exeDatas) {
            this.type = exeDatas.rawgetStr("type");
            this.metabolics = (Metabolics)((Object)exeDatas.rawget("metabolics"));
            this.stiffnessInc = new ArrayList<String>(Arrays.asList(exeDatas.rawgetStr("stiffness").split(",")));
            if (exeDatas.rawgetFloat("xpMod") > 0.0f) {
                this.xpModifier = exeDatas.rawgetFloat("xpMod");
            }
        }
    }
}

