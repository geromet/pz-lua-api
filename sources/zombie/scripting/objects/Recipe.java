/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import java.lang.invoke.CallSite;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.characters.skills.PerkFactory;
import zombie.core.Translator;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;

@UsedFromLua
public class Recipe
extends BaseScriptObject {
    private final boolean canBeDoneFromFloor = false;
    public float timeToMake = 0.0f;
    public String sound;
    protected String animNode;
    protected String prop1;
    protected String prop2;
    public final ArrayList<Source> source = new ArrayList();
    public Result result = null;
    public final ArrayList<Result> results = new ArrayList();
    public boolean allowDestroyedItem = false;
    public boolean allowFrozenItem = false;
    public boolean allowRottenItem = false;
    public boolean allowOnlyOne = false;
    public boolean inSameInventory = false;
    public String name = "recipe";
    private String originalname;
    private String requiredNearObject;
    private final String tooltip;
    public ArrayList<RequiredSkill> skillRequired;
    private final boolean needToBeLearn = false;
    protected String category;
    protected boolean removeResultItem;
    private final float heat = 0.0f;
    protected boolean stopOnWalk = true;
    protected boolean stopOnRun = true;
    public boolean hidden;
    private String recipeFileText;
    private final boolean obsolete = false;
    private final boolean requiresWorkstation = false;
    private final float stationMultiplier = 0.25f;
    private final HashMap<LuaCall, String> luaCalls = new HashMap();

    public boolean isRequiresWorkstation() {
        return false;
    }

    public float getStationMultiplier() {
        return 0.25f;
    }

    public Recipe() {
        super(ScriptType.Recipe);
        this.tooltip = null;
        this.setOriginalname("recipe");
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        this.name = Translator.getRecipeName(name);
        this.originalname = name;
        this.recipeFileText = totalFile;
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        this.LoadCommonBlock(block);
        for (ScriptParser.BlockElement element : block.elements) {
            String s;
            if (element.asValue() == null || (s = element.asValue().string).trim().isEmpty()) continue;
            if (s.contains(":")) {
                String[] split = s.split(":");
                String key = split[0].trim();
                String value = split[1].trim();
                if (key.equalsIgnoreCase("Override")) continue;
                DebugLog.General.error("Could not assign [key]: '" + key + "' : '" + value + "'.");
                continue;
            }
            this.DoSource(s.trim());
        }
    }

    public void DoSource(String type) {
        Source source2 = new Source();
        if (type.contains("=")) {
            source2.count = Float.parseFloat(type.split("=")[1].trim());
            type = type.split("=")[0].trim();
        }
        if (type.indexOf("keep") == 0) {
            type = type.replace("keep ", "");
            source2.keep = true;
        }
        if (type.contains(";")) {
            String[] typeUse = type.split(";");
            type = typeUse[0];
            source2.use = Float.parseFloat(typeUse[1]);
        }
        if (type.indexOf("destroy") == 0) {
            type = type.replace("destroy ", "");
            source2.destroy = true;
        }
        if (type.equals("null")) {
            source2.getItems().clear();
            source2.originalItems.clear();
        } else if (type.contains("/")) {
            type = type.replaceFirst("keep ", "").trim();
            source2.getItems().addAll(Arrays.asList(type.split("/")));
            source2.originalItems.addAll(Arrays.asList(type.split("/")));
        } else {
            source2.getItems().add(type);
            source2.originalItems.add(type);
        }
        if (!type.isEmpty()) {
            this.source.add(source2);
        }
    }

    public void DoResult(String type) {
        String[] split;
        Result result = new Result();
        if (type.contains("=")) {
            split = type.split("=");
            type = split[0].trim();
            result.count = Integer.parseInt(split[1].trim());
        }
        if (type.contains(";")) {
            split = type.split(";");
            type = split[0].trim();
            result.drainableCount = Integer.parseInt(split[1].trim());
        }
        if (type.contains(".")) {
            result.type = type.split("\\.")[1];
            result.module = type.split("\\.")[0];
        } else {
            result.type = type;
        }
        if (this.result == null) {
            this.result = result;
        }
        this.results.add(result);
    }

    public int getNumberOfNeededItem() {
        int result = 0;
        for (int i = 0; i < this.getSource().size(); ++i) {
            Source source2 = this.getSource().get(i);
            if (source2.getItems().isEmpty()) continue;
            result = (int)((float)result + source2.getCount());
        }
        return result;
    }

    public ArrayList<String> getRequiredSkills() {
        ArrayList<CallSite> result = null;
        if (this.skillRequired != null) {
            result = new ArrayList<CallSite>();
            for (int i = 0; i < this.skillRequired.size(); ++i) {
                RequiredSkill skill = this.skillRequired.get(i);
                PerkFactory.Perk perk = PerkFactory.getPerk(skill.perk);
                if (perk == null) {
                    result.add((CallSite)((Object)(skill.perk.name + " " + skill.level)));
                    continue;
                }
                String newEntry = perk.name + " " + skill.level;
                result.add((CallSite)((Object)newEntry));
            }
        }
        return result;
    }

    public int getRequiredSkillCount() {
        return this.skillRequired == null ? 0 : this.skillRequired.size();
    }

    public RequiredSkill getRequiredSkill(int index) {
        if (this.skillRequired == null || index < 0 || index >= this.skillRequired.size()) {
            return null;
        }
        return this.skillRequired.get(index);
    }

    public void clearRequiredSkills() {
        if (this.skillRequired == null) {
            return;
        }
        this.skillRequired.clear();
    }

    public void addRequiredSkill(PerkFactory.Perk perk, int level) {
        if (this.skillRequired == null) {
            this.skillRequired = new ArrayList();
        }
        this.skillRequired.add(new RequiredSkill(perk, level));
    }

    public Source findSource(String sourceFullType) {
        for (int i = 0; i < this.source.size(); ++i) {
            Source source2 = this.source.get(i);
            for (int j = 0; j < source2.getItems().size(); ++j) {
                if (!source2.getItems().get(j).equals(sourceFullType)) continue;
                return source2;
            }
        }
        return null;
    }

    public ArrayList<Source> getSource() {
        return this.source;
    }

    public String getOriginalname() {
        return this.originalname;
    }

    public void setOriginalname(String originalname) {
        this.originalname = originalname;
    }

    public String getFullType() {
        return this.getModule().name + "." + this.originalname;
    }

    public String getName() {
        return this.name;
    }

    public float getHeat() {
        return 0.0f;
    }

    public Result getResult() {
        return this.result;
    }

    @Deprecated
    public String getNearItem() {
        return this.requiredNearObject;
    }

    @Deprecated
    public void setNearItem(String nearItem) {
        this.requiredNearObject = nearItem;
    }

    public ArrayList<Result> getResults() {
        return this.results;
    }

    @UsedFromLua
    public static final class Result {
        public String module;
        public String type;
        public int count = 1;
        public int drainableCount;

        public String getType() {
            return this.type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getCount() {
            return this.count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public String getModule() {
            return this.module;
        }

        public void setModule(String module) {
            this.module = module;
        }

        public String getFullType() {
            return this.module + "." + this.type;
        }

        public int getDrainableCount() {
            return this.drainableCount;
        }

        public void setDrainableCount(int count) {
            this.drainableCount = count;
        }
    }

    @UsedFromLua
    public static final class Source {
        public boolean keep;
        private final ArrayList<String> items = new ArrayList();
        private final ArrayList<String> originalItems = new ArrayList();
        public boolean destroy;
        public float count = 1.0f;
        public float use;

        public boolean isDestroy() {
            return this.destroy;
        }

        public void setDestroy(boolean destroy) {
            this.destroy = destroy;
        }

        public boolean isKeep() {
            return this.keep;
        }

        public void setKeep(boolean keep) {
            this.keep = keep;
        }

        public float getCount() {
            return this.count;
        }

        public void setCount(float count) {
            this.count = count;
        }

        public float getUse() {
            return this.use;
        }

        public void setUse(float use) {
            this.use = use;
        }

        public ArrayList<String> getItems() {
            return this.items;
        }

        public ArrayList<String> getOriginalItems() {
            return this.originalItems;
        }

        public String getOnlyItem() {
            if (this.items.size() != 1) {
                throw new RuntimeException("items.size() == " + this.items.size());
            }
            return this.items.get(0);
        }
    }

    @UsedFromLua
    public static final class RequiredSkill {
        private final PerkFactory.Perk perk;
        private final int level;

        public RequiredSkill(PerkFactory.Perk perk, int level) {
            this.perk = perk;
            this.level = level;
        }

        public PerkFactory.Perk getPerk() {
            return this.perk;
        }

        public int getLevel() {
            return this.level;
        }
    }

    public static enum LuaCall {
        LuaAttributes,
        LuaTest,
        LuaCreate,
        LuaGrab,
        LuaCanPerform,
        LuaGiveXP;

    }
}

