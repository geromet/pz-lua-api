/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.skills;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.core.Translator;
import zombie.core.math.PZMath;

@UsedFromLua
public final class PerkFactory {
    public static final ArrayList<Perk> PerkList = new ArrayList();
    private static final HashMap<String, Perk> PerkById = new HashMap();
    private static final HashMap<String, Perk> PerkByName = new HashMap();
    private static final Perk[] PerkByIndex = new Perk[256];
    private static int nextPerkId;
    private static final float PERK_XP_REQ_MULTIPLIER = 1.5f;

    public static String getPerkName(Perk type) {
        return type.getName();
    }

    public static Perk getPerkFromName(String name) {
        return PerkByName.get(name);
    }

    public static Perk getPerk(Perk perk) {
        return perk;
    }

    public static Perk AddPerk(Perk perk, String translation, int xp1, int xp2, int xp3, int xp4, int xp5, int xp6, int xp7, int xp8, int xp9, int xp10) {
        return PerkFactory.AddPerk(perk, translation, Perks.None, xp1, xp2, xp3, xp4, xp5, xp6, xp7, xp8, xp9, xp10, false);
    }

    public static Perk AddPerk(Perk perk, String translation, int xp1, int xp2, int xp3, int xp4, int xp5, int xp6, int xp7, int xp8, int xp9, int xp10, boolean passiv) {
        return PerkFactory.AddPerk(perk, translation, Perks.None, xp1, xp2, xp3, xp4, xp5, xp6, xp7, xp8, xp9, xp10, passiv);
    }

    public static Perk AddPerk(Perk perk, String translation, Perk parent, int xp1, int xp2, int xp3, int xp4, int xp5, int xp6, int xp7, int xp8, int xp9, int xp10) {
        return PerkFactory.AddPerk(perk, translation, parent, xp1, xp2, xp3, xp4, xp5, xp6, xp7, xp8, xp9, xp10, false);
    }

    public static Perk AddPerk(Perk perk, String translation, Perk parent, int xp1, int xp2, int xp3, int xp4, int xp5, int xp6, int xp7, int xp8, int xp9, int xp10, boolean passiv) {
        perk.translation = translation;
        perk.name = Translator.getText("IGUI_perks_" + translation);
        perk.parent = parent;
        perk.passiv = passiv;
        perk.xp1 = (int)((float)xp1 * 1.5f);
        perk.xp2 = (int)((float)xp2 * 1.5f);
        perk.xp3 = (int)((float)xp3 * 1.5f);
        perk.xp4 = (int)((float)xp4 * 1.5f);
        perk.xp5 = (int)((float)xp5 * 1.5f);
        perk.xp6 = (int)((float)xp6 * 1.5f);
        perk.xp7 = (int)((float)xp7 * 1.5f);
        perk.xp8 = (int)((float)xp8 * 1.5f);
        perk.xp9 = (int)((float)xp9 * 1.5f);
        perk.xp10 = (int)((float)xp10 * 1.5f);
        PerkByName.put(perk.getName(), perk);
        PerkList.add(perk);
        return perk;
    }

    public static void init() {
        Perks.None.parent = Perks.None;
        Perks.MAX.parent = Perks.None;
        PerkFactory.AddPerk(Perks.Combat, "CombatMelee", 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Axe, "Axe", Perks.Combat, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Blunt, "Blunt", Perks.Combat, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.SmallBlunt, "SmallBlunt", Perks.Combat, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.LongBlade, "LongBlade", Perks.Combat, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.SmallBlade, "SmallBlade", Perks.Combat, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Spear, "Spear", Perks.Combat, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Maintenance, "Maintenance", Perks.Combat, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Firearm, "CombatFirearms", 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Aiming, "Aiming", Perks.Firearm, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Reloading, "Reloading", Perks.Firearm, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Crafting, "Crafting", 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Woodwork, "Carpentry", Perks.Crafting, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Carving, "Carving", Perks.Crafting, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Cooking, "Cooking", Perks.Crafting, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Electricity, "Electricity", Perks.Crafting, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Doctor, "Doctor", Perks.Survivalist, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Glassmaking, "Glassmaking", Perks.Crafting, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.FlintKnapping, "FlintKnapping", Perks.Crafting, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Masonry, "Masonry", Perks.Crafting, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Blacksmith, "Blacksmith", Perks.Crafting, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Mechanics, "Mechanics", Perks.Crafting, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Pottery, "Pottery", Perks.Crafting, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Tailoring, "Tailoring", Perks.Crafting, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.MetalWelding, "MetalWelding", Perks.Crafting, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Survivalist, "Survivalist", 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Fishing, "Fishing", Perks.Survivalist, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.PlantScavenging, "Foraging", Perks.Survivalist, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Tracking, "Tracking", Perks.Survivalist, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Trapping, "Trapping", Perks.Survivalist, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.PhysicalCategory, "PhysicalCategory", 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Fitness, "Fitness", Perks.PhysicalCategory, 1000, 2000, 4000, 6000, 12000, 20000, 40000, 60000, 80000, 100000, true);
        PerkFactory.AddPerk(Perks.Strength, "Strength", Perks.PhysicalCategory, 1000, 2000, 4000, 6000, 12000, 20000, 40000, 60000, 80000, 100000, true);
        PerkFactory.AddPerk(Perks.Agility, "Agility", 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Lightfoot, "Lightfooted", Perks.PhysicalCategory, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Nimble, "Nimble", Perks.PhysicalCategory, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Sprinting, "Sprinting", Perks.PhysicalCategory, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Sneak, "Sneaking", Perks.PhysicalCategory, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.FarmingCategory, "FarmingCategory", 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Farming, "Farming", Perks.FarmingCategory, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Husbandry, "Husbandry", Perks.FarmingCategory, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
        PerkFactory.AddPerk(Perks.Butchering, "Butchering", Perks.FarmingCategory, 50, 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000);
    }

    public static void initTranslations() {
        PerkByName.clear();
        for (Perk perk : PerkList) {
            perk.name = Translator.getText("IGUI_perks_" + perk.translation);
            PerkByName.put(perk.name, perk);
        }
    }

    public static void Reset() {
        nextPerkId = 0;
        for (int i = PerkByIndex.length - 1; i >= 0; --i) {
            Perk perk = PerkByIndex[i];
            if (perk == null) continue;
            if (perk.isCustom()) {
                PerkList.remove(perk);
                PerkById.remove(perk.getId());
                PerkByName.remove(perk.getName());
                PerkFactory.PerkByIndex[perk.index] = null;
                continue;
            }
            if (perk == Perks.MAX || nextPerkId != 0) continue;
            nextPerkId = i + 1;
        }
        Perks.MAX.index = nextPerkId;
    }

    @UsedFromLua
    public static final class Perk {
        private final String id;
        private int index;
        private boolean custom;
        public String translation;
        public String name;
        public boolean passiv;
        public int xp1;
        public int xp2;
        public int xp3;
        public int xp4;
        public int xp5;
        public int xp6;
        public int xp7;
        public int xp8;
        public int xp9;
        public int xp10;
        public Perk parent = Perks.None;

        public Perk(String id) {
            this.id = id;
            this.index = nextPerkId++;
            this.translation = id;
            this.name = id;
            PerkById.put(id, this);
            PerkFactory.PerkByIndex[this.index] = this;
            if (Perks.MAX != null) {
                Perks.MAX.index = PZMath.max(Perks.MAX.index, this.index + 1);
            }
        }

        public Perk(String id, Perk parent) {
            this(id);
            this.parent = parent;
        }

        public String getId() {
            return this.id;
        }

        public int index() {
            return this.index;
        }

        public void setCustom() {
            this.custom = true;
        }

        public boolean isCustom() {
            return this.custom;
        }

        public boolean isPassiv() {
            return this.passiv;
        }

        public Perk getParent() {
            return this.parent;
        }

        public String getName() {
            return this.name;
        }

        public Perk getType() {
            return this;
        }

        public int getXp1() {
            return this.xp1;
        }

        public int getXp2() {
            return this.xp2;
        }

        public int getXp3() {
            return this.xp3;
        }

        public int getXp4() {
            return this.xp4;
        }

        public int getXp5() {
            return this.xp5;
        }

        public int getXp6() {
            return this.xp6;
        }

        public int getXp7() {
            return this.xp7;
        }

        public int getXp8() {
            return this.xp8;
        }

        public int getXp9() {
            return this.xp9;
        }

        public int getXp10() {
            return this.xp10;
        }

        public float getXpForLevel(int level) {
            if (level == 1) {
                return this.xp1;
            }
            if (level == 2) {
                return this.xp2;
            }
            if (level == 3) {
                return this.xp3;
            }
            if (level == 4) {
                return this.xp4;
            }
            if (level == 5) {
                return this.xp5;
            }
            if (level == 6) {
                return this.xp6;
            }
            if (level == 7) {
                return this.xp7;
            }
            if (level == 8) {
                return this.xp8;
            }
            if (level == 9) {
                return this.xp9;
            }
            if (level == 10) {
                return this.xp10;
            }
            return -1.0f;
        }

        public float getTotalXpForLevel(int level) {
            int total = 0;
            for (int i = 1; i <= level; ++i) {
                float xp = this.getXpForLevel(i);
                if (xp == -1.0f) continue;
                total = (int)((float)total + xp);
            }
            return total;
        }

        public String toString() {
            return this.id;
        }
    }

    @UsedFromLua
    public static final class Perks {
        public static final Perk None = new Perk("None");
        public static final Perk Agility = new Perk("Agility");
        public static final Perk Cooking = new Perk("Cooking");
        public static final Perk Melee = new Perk("Melee");
        public static final Perk Crafting = new Perk("Crafting");
        public static final Perk Fitness = new Perk("Fitness");
        public static final Perk Strength = new Perk("Strength");
        public static final Perk Blunt = new Perk("Blunt");
        public static final Perk Axe = new Perk("Axe");
        public static final Perk Lightfoot = new Perk("Lightfoot");
        public static final Perk Nimble = new Perk("Nimble");
        public static final Perk Sprinting = new Perk("Sprinting");
        public static final Perk Sneak = new Perk("Sneak");
        public static final Perk Woodwork = new Perk("Woodwork");
        public static final Perk Aiming = new Perk("Aiming");
        public static final Perk Reloading = new Perk("Reloading");
        public static final Perk Farming = new Perk("Farming");
        public static final Perk Survivalist = new Perk("Survivalist");
        public static final Perk Fishing = new Perk("Fishing");
        public static final Perk Trapping = new Perk("Trapping");
        public static final Perk Passiv = new Perk("Passiv");
        public static final Perk Firearm = new Perk("Firearm");
        public static final Perk PlantScavenging = new Perk("PlantScavenging");
        public static final Perk Doctor = new Perk("Doctor");
        public static final Perk Electricity = new Perk("Electricity");
        public static final Perk Blacksmith = new Perk("Blacksmith");
        public static final Perk MetalWelding = new Perk("MetalWelding");
        public static final Perk Melting = new Perk("Melting");
        public static final Perk Mechanics = new Perk("Mechanics");
        public static final Perk Spear = new Perk("Spear");
        public static final Perk Maintenance = new Perk("Maintenance");
        public static final Perk SmallBlade = new Perk("SmallBlade");
        public static final Perk LongBlade = new Perk("LongBlade");
        public static final Perk SmallBlunt = new Perk("SmallBlunt");
        public static final Perk Combat = new Perk("Combat");
        public static final Perk Tailoring = new Perk("Tailoring");
        public static final Perk Tracking = new Perk("Tracking");
        public static final Perk Husbandry = new Perk("Husbandry");
        public static final Perk FlintKnapping = new Perk("FlintKnapping");
        public static final Perk Masonry = new Perk("Masonry");
        public static final Perk Pottery = new Perk("Pottery");
        public static final Perk Carving = new Perk("Carving");
        public static final Perk Butchering = new Perk("Butchering");
        public static final Perk Glassmaking = new Perk("Glassmaking");
        public static final Perk FarmingCategory = new Perk("FarmingCategory");
        public static final Perk PhysicalCategory = new Perk("PhysicalCategory");
        public static final Perk MAX = new Perk("MAX");

        public static int getMaxIndex() {
            return MAX.index();
        }

        public static Perk fromIndex(int value) {
            if (value < 0 || value > nextPerkId) {
                return null;
            }
            return PerkByIndex[value];
        }

        public static Perk FromString(String id) {
            return PerkById.getOrDefault(id, MAX);
        }
    }
}

