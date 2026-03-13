/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import java.util.Arrays;
import zombie.UsedFromLua;
import zombie.characters.skills.PerkFactory;
import zombie.debug.DebugLog;
import zombie.scripting.objects.Recipe;

@UsedFromLua
public class MovableRecipe
extends Recipe {
    private boolean isValid;
    private String worldSprite = "";
    private PerkFactory.Perk xpPerk = PerkFactory.Perks.MAX;
    private Recipe.Source primaryTools;
    private Recipe.Source secondaryTools;

    public MovableRecipe() {
        this.animNode = "Disassemble";
        this.removeResultItem = true;
        this.allowDestroyedItem = false;
        this.name = "Disassemble Movable";
    }

    public void setResult(String resultItem, int count) {
        Recipe.Result result = new Recipe.Result();
        result.count = count;
        if (resultItem.contains(".")) {
            result.type = resultItem.split("\\.")[1];
            result.module = resultItem.split("\\.")[0];
        } else {
            DebugLog.log("MovableRecipe invalid result item. item = " + resultItem);
        }
        this.result = result;
    }

    public void setSource(String sourceItem) {
        Recipe.Source source2 = new Recipe.Source();
        source2.getItems().add(sourceItem);
        this.source.add(source2);
    }

    public void setTool(String tools, boolean isPrimary) {
        Recipe.Source source2 = new Recipe.Source();
        source2.keep = true;
        if (tools.contains("/")) {
            tools = tools.replaceFirst("keep ", "").trim();
            source2.getItems().addAll(Arrays.asList(tools.split("/")));
        } else {
            source2.getItems().add(tools);
        }
        if (isPrimary) {
            this.primaryTools = source2;
        } else {
            this.secondaryTools = source2;
        }
        this.source.add(source2);
    }

    public Recipe.Source getPrimaryTools() {
        return this.primaryTools;
    }

    public Recipe.Source getSecondaryTools() {
        return this.secondaryTools;
    }

    public void setRequiredSkill(PerkFactory.Perk perk, int level) {
        Recipe.RequiredSkill skill = new Recipe.RequiredSkill(perk, level);
        this.skillRequired.add(skill);
    }

    public void setXpPerk(PerkFactory.Perk perk) {
        this.xpPerk = perk;
    }

    public PerkFactory.Perk getXpPerk() {
        return this.xpPerk;
    }

    public boolean hasXpPerk() {
        return this.xpPerk != PerkFactory.Perks.MAX;
    }

    public void setTime(float time) {
        this.timeToMake = time;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWorldSprite() {
        return this.worldSprite;
    }

    public void setWorldSprite(String worldSprite) {
        this.worldSprite = worldSprite;
    }

    public boolean isValid() {
        return this.isValid;
    }

    public void setValid(boolean valid) {
        this.isValid = valid;
    }
}

