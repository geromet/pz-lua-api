/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals.datas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.util.StringUtils;

@UsedFromLua
public class AnimalBreed {
    public String name;
    public ArrayList<String> texture;
    public String textureMale;
    public String textureBaby;
    public int minWeightBonus;
    public int maxWeightBonus;
    public String milkType;
    public String woolType;
    public HashMap<String, ForcedGenes> forcedGenes;
    public String invIconMale;
    public String invIconFemale;
    public String invIconBaby;
    public String invIconMaleDead;
    public String invIconFemaleDead;
    public String invIconBabyDead;
    public String invIconMaleSkel;
    public String invIconFemaleSkel;
    public String invIconBabySkel;
    public String leather;
    public String headItem;
    public String featherItem;
    public int maxFeather;
    private final HashMap<String, Sound> sounds = new HashMap();
    public String rottenTexture;

    public String getName() {
        return this.name;
    }

    public String getMilkType() {
        return this.milkType;
    }

    public void loadForcedGenes(KahluaTableImpl def) {
        this.forcedGenes = new HashMap();
        KahluaTableIterator it = def.iterator();
        while (it.advance()) {
            ForcedGenes gene = new ForcedGenes();
            gene.name = it.getKey().toString().toLowerCase();
            KahluaTableIterator it2 = ((KahluaTableImpl)it.getValue()).iterator();
            while (it2.advance()) {
                String key = it2.getKey().toString();
                String valueStr = it2.getValue().toString();
                if ("minValue".equalsIgnoreCase(key)) {
                    gene.minValue = Float.parseFloat(valueStr);
                }
                if (!"maxValue".equalsIgnoreCase(key)) continue;
                gene.maxValue = Float.parseFloat(valueStr);
            }
            this.forcedGenes.put(gene.name, gene);
        }
    }

    public void loadSounds(KahluaTableImpl soundsTable) {
        this.sounds.clear();
        KahluaTableIterator it = soundsTable.iterator();
        while (it.advance()) {
            Object object = it.getValue();
            if (!(object instanceof KahluaTableImpl)) continue;
            KahluaTableImpl soundTable = (KahluaTableImpl)object;
            Sound sound = new Sound();
            sound.id = it.getKey().toString().toLowerCase(Locale.ENGLISH);
            sound.soundName = soundTable.getString("name");
            sound.intervalMin = soundTable.rawgetInt("intervalMin");
            sound.intervalMax = soundTable.rawgetInt("intervalMax");
            sound.slot = StringUtils.discardNullOrWhitespace(soundTable.getString("slot"));
            sound.priority = PZMath.max(soundTable.rawgetInt("priority"), 0);
            this.sounds.put(sound.id, sound);
        }
    }

    public Sound getSound(String id) {
        if (StringUtils.isNullOrWhitespace(id)) {
            return null;
        }
        return this.sounds.get(id.toLowerCase(Locale.ENGLISH));
    }

    public boolean isSoundDefined(String id) {
        return this.getSound(id) != null;
    }

    public boolean isSoundUndefined(String id) {
        return this.getSound(id) == null;
    }

    public String getFeatherItem() {
        return this.featherItem;
    }

    public String getWoolType() {
        return this.woolType;
    }

    public String getRottenTexture() {
        return this.rottenTexture;
    }

    public static final class ForcedGenes {
        public String name;
        public float minValue;
        public float maxValue;
    }

    public static final class Sound {
        public String id;
        public String soundName;
        public int intervalMin = -1;
        public int intervalMax = -1;
        public String slot;
        public int priority;

        public boolean isIntervalValid() {
            return this.intervalMin >= 0 && this.intervalMin <= this.intervalMax;
        }
    }
}

