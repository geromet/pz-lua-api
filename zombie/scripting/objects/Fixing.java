/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;

@UsedFromLua
public final class Fixing
extends BaseScriptObject {
    private String name;
    private ArrayList<String> require;
    private final LinkedList<Fixer> fixers = new LinkedList();
    private Fixer globalItem;
    private float conditionModifier = 1.0f;
    private static final PredicateRequired s_PredicateRequired = new PredicateRequired();
    private static final ArrayList<InventoryItem> s_InventoryItems = new ArrayList();

    public Fixing() {
        super(ScriptType.Fixing);
    }

    @Override
    public void Load(String name, String body) {
        String[] waypoint = body.split("[{}]");
        String[] coords = waypoint[1].split(",");
        this.Load(name, coords);
    }

    private void Load(String name, String[] strArray) {
        this.setName(name);
        for (int i = 0; i < strArray.length; ++i) {
            if (strArray[i].trim().isEmpty() || !strArray[i].contains("=")) continue;
            String[] split = strArray[i].split("=", 2);
            String key = split[0].trim();
            String value = split[1].trim();
            if (key.equals("Require")) {
                List<String> list = Arrays.asList(value.split(";"));
                for (int j = 0; j < list.size(); ++j) {
                    this.addRequiredItem(list.get(j).trim());
                }
                continue;
            }
            if (key.equals("Fixer")) {
                if (value.contains(";")) {
                    LinkedList<FixerSkill> finalList = new LinkedList<FixerSkill>();
                    List<String> skillList = Arrays.asList(value.split(";"));
                    for (int j = 1; j < skillList.size(); ++j) {
                        String[] ss = skillList.get(j).trim().split("=");
                        finalList.add(new FixerSkill(ss[0].trim(), Integer.parseInt(ss[1].trim())));
                    }
                    if (value.split(";")[0].trim().contains("=")) {
                        String[] ss = value.split(";")[0].trim().split("=");
                        this.fixers.add(new Fixer(ss[0], finalList, Integer.parseInt(ss[1])));
                        continue;
                    }
                    this.fixers.add(new Fixer(value.split(";")[0].trim(), finalList, 1));
                    continue;
                }
                if (value.contains("=")) {
                    this.fixers.add(new Fixer(value.split("=")[0], null, Integer.parseInt(value.split("=")[1])));
                    continue;
                }
                this.fixers.add(new Fixer(value, null, 1));
                continue;
            }
            if (key.equals("GlobalItem")) {
                if (value.contains("=")) {
                    this.setGlobalItem(new Fixer(value.split("=")[0], null, Integer.parseInt(value.split("=")[1])));
                    continue;
                }
                this.setGlobalItem(new Fixer(value, null, 1));
                continue;
            }
            if (!key.equals("ConditionModifier")) continue;
            this.setConditionModifier(Float.parseFloat(value.trim()));
        }
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getRequiredItem() {
        return this.require;
    }

    public void addRequiredItem(String require) {
        if (this.require == null) {
            this.require = new ArrayList();
        }
        this.require.add(require);
    }

    public LinkedList<Fixer> getFixers() {
        return this.fixers;
    }

    public Fixer usedInFixer(InventoryItem itemType, IsoGameCharacter chr) {
        for (int j = 0; j < this.getFixers().size(); ++j) {
            Fixer fixer = this.getFixers().get(j);
            if (!fixer.getFixerName().equals(itemType.getFullType())) continue;
            if (itemType instanceof DrainableComboItem) {
                DrainableComboItem item = (DrainableComboItem)itemType;
                if (item.getCurrentUsesFloat() < 1.0f) {
                    if (item.getCurrentUses() < fixer.getNumberOfUse()) continue;
                    return fixer;
                }
                return fixer;
            }
            if (chr.getInventory().getCountTypeRecurse(fixer.getFixerName()) < fixer.getNumberOfUse()) continue;
            return fixer;
        }
        return null;
    }

    public InventoryItem haveGlobalItem(IsoGameCharacter chr) {
        s_InventoryItems.clear();
        ArrayList<InventoryItem> items = this.getRequiredFixerItems(chr, this.getGlobalItem(), null, s_InventoryItems);
        return items == null ? null : items.get(0);
    }

    public InventoryItem haveThisFixer(IsoGameCharacter chr, Fixer fixer, InventoryItem brokenObject) {
        s_InventoryItems.clear();
        ArrayList<InventoryItem> items = this.getRequiredFixerItems(chr, fixer, brokenObject, s_InventoryItems);
        return items == null ? null : items.get(0);
    }

    public int countUses(IsoGameCharacter chr, Fixer fixer, InventoryItem brokenObject) {
        s_InventoryItems.clear();
        Fixing.s_PredicateRequired.uses = 0;
        this.getRequiredFixerItems(chr, fixer, brokenObject, s_InventoryItems);
        return Fixing.s_PredicateRequired.uses;
    }

    private static int countUses(InventoryItem item) {
        if (item instanceof DrainableComboItem) {
            DrainableComboItem drainable = (DrainableComboItem)item;
            return drainable.getCurrentUses();
        }
        return 1;
    }

    public ArrayList<InventoryItem> getRequiredFixerItems(IsoGameCharacter chr, Fixer fixer, InventoryItem brokenItem, ArrayList<InventoryItem> items) {
        if (fixer == null) {
            return null;
        }
        assert (Thread.currentThread() == GameWindow.gameThread);
        PredicateRequired predicate = s_PredicateRequired;
        predicate.fixer = fixer;
        predicate.brokenItem = brokenItem;
        predicate.uses = 0;
        chr.getInventory().getAllRecurse(predicate, items);
        return predicate.uses >= fixer.getNumberOfUse() ? items : null;
    }

    public ArrayList<InventoryItem> getRequiredItems(IsoGameCharacter chr, Fixer fixer, InventoryItem brokenItem) {
        ArrayList<InventoryItem> items = new ArrayList<InventoryItem>();
        if (this.getRequiredFixerItems(chr, fixer, brokenItem, items) == null) {
            items.clear();
            return null;
        }
        if (this.getGlobalItem() != null && this.getRequiredFixerItems(chr, this.getGlobalItem(), brokenItem, items) == null) {
            items.clear();
            return null;
        }
        return items;
    }

    public Fixer getGlobalItem() {
        return this.globalItem;
    }

    public void setGlobalItem(Fixer globalItem) {
        this.globalItem = globalItem;
    }

    public float getConditionModifier() {
        return this.conditionModifier;
    }

    public void setConditionModifier(float conditionModifier) {
        this.conditionModifier = conditionModifier;
    }

    @UsedFromLua
    public static final class FixerSkill {
        private final String skillName;
        private final int skillLvl;

        public FixerSkill(String skillName, int skillLvl) {
            this.skillName = skillName;
            this.skillLvl = skillLvl;
        }

        public String getSkillName() {
            return this.skillName;
        }

        public int getSkillLevel() {
            return this.skillLvl;
        }
    }

    @UsedFromLua
    public static final class Fixer {
        private final String fixerName;
        private final LinkedList<FixerSkill> skills;
        private final int numberOfUse;

        public Fixer(String name, LinkedList<FixerSkill> skills, int numberOfUse) {
            this.fixerName = name;
            this.skills = skills;
            this.numberOfUse = numberOfUse;
        }

        public String getFixerName() {
            return this.fixerName;
        }

        public LinkedList<FixerSkill> getFixerSkills() {
            return this.skills;
        }

        public int getNumberOfUse() {
            return this.numberOfUse;
        }
    }

    private static final class PredicateRequired
    implements Predicate<InventoryItem> {
        Fixer fixer;
        InventoryItem brokenItem;
        int uses;

        private PredicateRequired() {
        }

        @Override
        public boolean test(InventoryItem item) {
            if (this.uses >= this.fixer.getNumberOfUse()) {
                return false;
            }
            if (item == this.brokenItem) {
                return false;
            }
            if (!this.fixer.getFixerName().equals(item.getFullType())) {
                return false;
            }
            int itemUses = Fixing.countUses(item);
            if (itemUses > 0) {
                this.uses += itemUses;
                return true;
            }
            return false;
        }
    }
}

