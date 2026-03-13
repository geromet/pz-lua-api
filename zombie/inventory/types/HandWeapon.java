/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory.types;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import zombie.AttackType;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.SurvivorDesc;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.debug.DebugType;
import zombie.interfaces.IUpdater;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemSpawner;
import zombie.inventory.types.WeaponPart;
import zombie.inventory.types.WeaponType;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector3;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;
import zombie.scripting.objects.ModelKey;
import zombie.scripting.objects.ModelWeaponPart;
import zombie.scripting.objects.MoodleType;
import zombie.scripting.objects.SoundKey;
import zombie.scripting.objects.WeaponCategory;
import zombie.ui.ObjectTooltip;
import zombie.util.StringUtils;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

@UsedFromLua
public final class HandWeapon
extends InventoryItem
implements IUpdater {
    public static final int MAX_ATTACHMENT_COUNT = 100;
    public float weaponLength;
    public float splatSize = 1.0f;
    private int ammoPerShoot = 1;
    private String magazineType;
    private boolean angleFalloff;
    private boolean canBarricade;
    private float doSwingBeforeImpact;
    private String impactSound = "BaseballBatHit";
    private boolean knockBackOnNoDeath = true;
    private float maxAngle = 1.0f;
    private float maxDamage = 1.5f;
    private int maxHitCount = 1000;
    private float maxRange = 1.0f;
    private boolean ranged;
    private float minAngle = 0.5f;
    private float minDamage = 0.4f;
    private float minimumSwingTime = 0.5f;
    private float minRange;
    private float noiseFactor;
    private ItemTag otherHandRequire;
    private boolean otherHandUse;
    private String physicsObject;
    private float pushBackMod = 1.0f;
    private boolean rangeFalloff;
    private int soundRadius;
    private int soundVolume;
    private boolean splatBloodOnNoDeath;
    private int splatNumber = 2;
    private String swingSound = "BaseballBatSwing";
    private float swingTime = 1.0f;
    private float toHitModifier = 1.0f;
    private boolean useEndurance = true;
    private boolean useSelf;
    private String weaponSprite;
    private String originalWeaponSprite;
    private float otherBoost = 1.0f;
    private int doorDamage = 1;
    private String doorHitSound = "BaseballBatHit";
    private int conditionLowerChance = 10;
    private boolean multipleHitConditionAffected = true;
    private boolean shareEndurance = true;
    private boolean alwaysKnockdown;
    private float enduranceMod = 1.0f;
    private float knockdownMod = 1.0f;
    private boolean cantAttackWithLowestEndurance;
    public boolean isAimedFirearm;
    public boolean isAimedHandWeapon;
    public String runAnim = "Run";
    public String idleAnim = "Idle";
    public float hitAngleMod;
    private String subCategory = "";
    private Set<WeaponCategory> weaponCategories;
    private int aimingPerkCritModifier;
    private float aimingPerkRangeModifier;
    private float aimingPerkHitChanceModifier;
    private int hitChance;
    private float aimingPerkMinAngleModifier;
    private int recoilDelay;
    private boolean piercingBullets;
    private float soundGain = 1.0f;
    private final HashMap<String, WeaponPart> attachments = new HashMap();
    private final List<WeaponPart> attachmentList = new ArrayList<WeaponPart>();
    public WeaponPart activeSight;
    private WeaponPart activeLight;
    private int clipSize;
    private int reloadTime;
    private int aimingTime;
    private float minRangeRanged;
    private float minSightRange = 2.0f;
    private float maxSightRange = 6.0f;
    private int treeDamage;
    private String bulletOutSound;
    private String shellFallSound;
    private int triggerExplosionTimer;
    private boolean canBePlaced;
    private int explosionRange;
    private int explosionPower;
    private int fireRange;
    private int fireStartingEnergy;
    private int fireStartingChance;
    private int smokeRange;
    private int noiseRange;
    private float extraDamage;
    private int explosionTimer;
    private int explosionDuration;
    private String placedSprite;
    private boolean canBeReused;
    private int sensorRange;
    private float criticalDamageMultiplier = 2.0f;
    private float baseSpeed = 1.0f;
    private float bloodLevel;
    private String ammoBox;
    private String rackSound;
    private String clickSound = "Stormy9mmClick";
    private boolean containsClip;
    private String weaponReloadType = "handgun";
    private boolean rackAfterShoot;
    private boolean roundChambered;
    private boolean spentRoundChambered;
    private int spentRoundCount;
    private float jamGunChance = 5.0f;
    private int projectileCount = 1;
    private float projectileSpread;
    private float projectileWeightCenter = 1.0f;
    private final float aimingMod = 1.0f;
    private float criticalChance = 20.0f;
    private String hitSound = "BaseballBatHit";
    private boolean isJammed;
    private ArrayList<ModelWeaponPart> modelWeaponPart;
    private boolean haveChamber = true;
    private String bulletName;
    private String damageCategory;
    private boolean damageMakeHole;
    private String hitFloorSound = "BatOnFloor";
    private boolean insertAllBulletsReload;
    private String fireMode;
    private float cyclicRateMultiplier;
    private ArrayList<String> fireModePossibilities;
    private ArrayList<String> weaponSpritesByIndex;
    private IsoGridSquare attackTargetSquare;
    private boolean isMelee = true;
    private boolean isExplosive;
    private static final Comparator<InventoryItem> magazineComparator = Comparator.comparingInt(InventoryItem::getCurrentAmmoCount);
    private ModelKey muzzleFlashModelKey;

    public HandWeapon(String module, String name, String itemType, String texName) {
        super(module, name, itemType, texName);
        this.itemType = ItemType.WEAPON;
    }

    public HandWeapon(String module, String name, String itemType, Item item) {
        super(module, name, itemType, item);
        this.itemType = ItemType.WEAPON;
    }

    @Override
    public String getCategory() {
        if (this.mainCategory != null) {
            return this.mainCategory;
        }
        return "Weapon";
    }

    @Override
    public boolean IsWeapon() {
        return true;
    }

    public float getSplatSize() {
        return this.splatSize;
    }

    @Override
    public float getScore(SurvivorDesc desc) {
        float score = 0.0f;
        if (this.getAmmoType() != null && !this.container.contains(this.getAmmoType().getItemKey())) {
            score -= 100000.0f;
        }
        if (this.condition == 0) {
            score -= 100000.0f;
        }
        score += this.maxDamage * 10.0f;
        score += this.maxAngle * 5.0f;
        score -= this.minimumSwingTime * 0.1f;
        score -= this.swingTime;
        if (desc != null && desc.getInstance().getThreatLevel() <= 2 && this.soundRadius > 5) {
            if (score > 0.0f && (float)this.soundRadius > score) {
                score = 1.0f;
            }
            score -= (float)this.soundRadius;
        }
        return score;
    }

    @Override
    public float getActualWeight() {
        float weight = this.getScriptItem().getActualWeight();
        for (WeaponPart part : this.attachments.values()) {
            weight += this.getWeaponPartWeightModifier(part);
        }
        return weight;
    }

    @Override
    public float getWeight() {
        return this.getActualWeight();
    }

    public float getEffectiveWeight() {
        return Math.max(this.getWeight(), 1.0f);
    }

    @Override
    public float getContentsWeight() {
        Item scriptMag;
        Item scriptAmmo;
        float contentsWeight = 0.0f;
        if (this.haveChamber() && this.isRoundChambered() && this.getAmmoType() != null && (scriptAmmo = ScriptManager.instance.FindItem(this.getAmmoType().getItemKey())) != null) {
            contentsWeight += scriptAmmo.getActualWeight();
        }
        if (this.isContainsClip() && !StringUtils.isNullOrWhitespace(this.getMagazineType()) && (scriptMag = ScriptManager.instance.FindItem(this.getMagazineType())) != null) {
            contentsWeight += scriptMag.getActualWeight();
        }
        return contentsWeight + super.getContentsWeight();
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        float f;
        ObjectTooltip.LayoutItem item;
        float tr = 1.0f;
        float tg = 1.0f;
        float tb = 0.8f;
        float ta = 1.0f;
        ColorInfo g2Bgrad = new ColorInfo();
        if (this.hasSharpness()) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_weapon_Sharpness") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            float f2 = this.getSharpness();
            Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), f2, g2Bgrad);
            item.setProgress(f2, g2Bgrad.getR(), g2Bgrad.getG(), g2Bgrad.getB(), 1.0f);
        }
        item = layout.addItem();
        String text = "Tooltip_weapon_Condition";
        if (this.hasHeadCondition()) {
            text = "Tooltip_weapon_HandleCondition";
        }
        item.setLabel(Translator.getText(text) + ":", 1.0f, 1.0f, 0.8f, 1.0f);
        float f3 = (float)this.getCondition() / (float)this.getConditionMax();
        Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), f3, g2Bgrad);
        item.setProgress(f3, g2Bgrad.getR(), g2Bgrad.getG(), g2Bgrad.getB(), 1.0f);
        if (this.hasHeadCondition()) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_weapon_HeadCondition") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            float f4 = (float)this.getHeadCondition() / (float)this.getHeadConditionMax();
            Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), f4, g2Bgrad);
            item.setProgress(f4, g2Bgrad.getR(), g2Bgrad.getG(), g2Bgrad.getB(), 1.0f);
        }
        if (this.getMaxDamage() > 0.0f) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_weapon_Damage") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            float realDmg = this.getMaxDamage() + this.getMinDamage();
            float maxDmg = 5.0f;
            f = realDmg / 5.0f;
            Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), f, g2Bgrad);
            item.setProgress(f, g2Bgrad.getR(), g2Bgrad.getG(), g2Bgrad.getB(), 1.0f);
        }
        if (this.bloodLevel != 0.0f) {
            ColorInfo b2Ggrad = new ColorInfo();
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_clothing_bloody") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            f3 = this.bloodLevel;
            Core.getInstance().getGoodHighlitedColor().interp(Core.getInstance().getBadHighlitedColor(), f3, b2Ggrad);
            item.setProgress(f3, b2Ggrad.getR(), b2Ggrad.getG(), b2Ggrad.getB(), 1.0f);
        }
        if (this.hasTag(ItemTag.FISHING_ROD)) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_fishing_line_Condition") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            Object lineConditionObj = this.getModData().rawget("fishing_LineCondition");
            f3 = 1.0f;
            if (lineConditionObj == null) {
                this.getModData().rawset("fishing_LineCondition", (Object)1.0);
            } else {
                f3 = (float)((Double)lineConditionObj).doubleValue();
            }
            Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), f3, g2Bgrad);
            item.setProgress(f3, g2Bgrad.getR(), g2Bgrad.getG(), g2Bgrad.getB(), 1.0f);
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_fishing_line") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            Object lineObj = this.getModData().rawget("fishing_LineType");
            if (lineObj == null) {
                this.getModData().rawset("fishing_LineType", (Object)"FishingLine");
                item.setValue(Translator.getText(ScriptManager.instance.FindItem("Base.FishingLine").getDisplayName()), 1.0f, 1.0f, 1.0f, 1.0f);
            } else if (ScriptManager.instance.FindItem("FishingLine") == null || ScriptManager.instance.FindItem((String)lineObj) == null) {
                item.setValue(Translator.getText("Tooltip_fishing_line"), 1.0f, 1.0f, 1.0f, 1.0f);
            } else {
                item.setValue(Translator.getText(ScriptManager.instance.FindItem((String)lineObj).getDisplayName()), 1.0f, 1.0f, 1.0f, 1.0f);
            }
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_fishing_hook") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            Object hookObj = this.getModData().rawget("fishing_HookType");
            if (hookObj == null) {
                this.getModData().rawset("fishing_HookType", (Object)"FishingHook");
                item.setValue(Translator.getText(ScriptManager.instance.FindItem("FishingHook").getDisplayName()), 1.0f, 1.0f, 1.0f, 1.0f);
            } else if (ScriptManager.instance.FindItem((String)hookObj) == null || ScriptManager.instance.FindItem("FishingHook") == null) {
                item.setValue(Translator.getText("Tooltip_fishing_hook"), 1.0f, 1.0f, 1.0f, 1.0f);
            } else {
                item.setValue(Translator.getText(ScriptManager.instance.FindItem((String)hookObj).getDisplayName()), 1.0f, 1.0f, 1.0f, 1.0f);
            }
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_fishing_bait") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            Object lureObj = this.getModData().rawget("fishing_Lure");
            if (lureObj == null) {
                item.setValue(Translator.getText("UI_None"), 1.0f, 0.0f, 0.0f, 1.0f);
            } else if (ScriptManager.instance.FindItem((String)lureObj) == null || Translator.getText("UI_None") == null) {
                item.setValue(Translator.getText("Tooltip_fishing_bait"), 1.0f, 1.0f, 1.0f, 1.0f);
            } else {
                item.setValue(Translator.getText(ScriptManager.instance.FindItem((String)lureObj).getDisplayName()), 1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
        if (this.isRanged()) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_weapon_Range") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            float range = this.getMaxRange(IsoPlayer.getInstance());
            float maxRange = 40.0f;
            f = range / 40.0f;
            Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), f, g2Bgrad);
            item.setProgress(f, g2Bgrad.getR(), g2Bgrad.getG(), g2Bgrad.getB(), 1.0f);
        }
        if (this.isTwoHandWeapon() && !this.isRequiresEquippedBothHands()) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_item_TwoHandWeapon"), 1.0f, 1.0f, 0.8f, 1.0f);
        }
        if (!StringUtils.isNullOrEmpty(this.getFireMode())) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_item_FireMode") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            item.setValue(Translator.getText("ContextMenu_FireMode_" + this.getFireMode()), 1.0f, 1.0f, 1.0f, 1.0f);
        }
        if (this.cantAttackWithLowestEndurance) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_weapon_Unusable_at_max_exertion"), Core.getInstance().getBadHighlitedColor().getR(), Core.getInstance().getBadHighlitedColor().getG(), Core.getInstance().getBadHighlitedColor().getB(), 1.0f);
        }
        if (this.getMaxAmmo() > 0) {
            Object currentAmmo = String.valueOf(this.getCurrentAmmoCount());
            if (this.isRoundChambered()) {
                currentAmmo = (String)currentAmmo + "+1";
            }
            item = layout.addItem();
            if (this.bulletName == null) {
                this.bulletName = this.getMagazineType() != null ? ((InventoryItem)InventoryItemFactory.CreateItem(this.getMagazineType())).getDisplayName() : ((InventoryItem)InventoryItemFactory.CreateItem(this.getAmmoType())).getDisplayName();
            }
            item.setLabel(this.bulletName + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            item.setValue((String)currentAmmo + " / " + this.getMaxAmmo(), 1.0f, 1.0f, 1.0f, 1.0f);
        }
        if (this.isJammed()) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_weapon_Jammed"), Core.getInstance().getBadHighlitedColor().getR(), Core.getInstance().getBadHighlitedColor().getG(), Core.getInstance().getBadHighlitedColor().getB(), 1.0f);
        } else if (this.haveChamber() && !this.isRoundChambered() && this.getCurrentAmmoCount() > 0) {
            item = layout.addItem();
            String xln = this.isSpentRoundChambered() ? "Tooltip_weapon_SpentRoundChambered" : "Tooltip_weapon_NoRoundChambered";
            item.setLabel(Translator.getText(xln), Core.getInstance().getBadHighlitedColor().getR(), Core.getInstance().getBadHighlitedColor().getG(), Core.getInstance().getBadHighlitedColor().getB(), 1.0f);
        } else if (this.getSpentRoundCount() > 0) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_weapon_SpentRounds") + ":", Core.getInstance().getBadHighlitedColor().getR(), Core.getInstance().getBadHighlitedColor().getG(), Core.getInstance().getBadHighlitedColor().getB(), 1.0f);
            item.setValue(this.getSpentRoundCount() + " / " + this.getMaxAmmo(), 1.0f, 1.0f, 1.0f, 1.0f);
        }
        if (!StringUtils.isNullOrEmpty(this.getMagazineType())) {
            if (this.isContainsClip()) {
                item = layout.addItem();
                item.setLabel(Translator.getText("Tooltip_weapon_ContainsClip"), 1.0f, 1.0f, 0.8f, 1.0f);
            } else {
                item = layout.addItem();
                item.setLabel(Translator.getText("Tooltip_weapon_NoClip"), 1.0f, 1.0f, 0.8f, 1.0f);
            }
        }
        for (int i = 0; i < this.attachmentList.size(); ++i) {
            WeaponPart weaponPart = this.attachmentList.get(i);
            ObjectTooltip.LayoutItem weaponPartLayoutItem = layout.addItem();
            weaponPartLayoutItem.setLabel(Translator.getText("Tooltip_weapon_" + weaponPart.getPartType()) + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            weaponPartLayoutItem.setValue(weaponPart.getName(), 1.0f, 1.0f, 1.0f, 1.0f);
            weaponPart.DoBatteryTooltip(tooltipUI, layout);
        }
        if (this.hasTag(ItemTag.NO_MAINTENANCE_XP)) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_weapon_NoMaintenanceXp"), 1.0f, 1.0f, 0.8f, 1.0f);
        }
    }

    public float getDamageMod(IsoGameCharacter chr) {
        int bluntLevel = chr.getPerkLevel(PerkFactory.Perks.Blunt);
        if (this.scriptItem.containsWeaponCategory(WeaponCategory.BLUNT)) {
            if (bluntLevel >= 3 && bluntLevel <= 6) {
                return 1.1f;
            }
            if (bluntLevel >= 7) {
                return 1.2f;
            }
        }
        int axeLevel = chr.getPerkLevel(PerkFactory.Perks.Axe);
        if (this.scriptItem.containsWeaponCategory(WeaponCategory.AXE)) {
            if (axeLevel >= 3 && axeLevel <= 6) {
                return 1.1f;
            }
            if (axeLevel >= 7) {
                return 1.2f;
            }
        }
        int spearLevel = chr.getPerkLevel(PerkFactory.Perks.Spear);
        if (this.scriptItem.containsWeaponCategory(WeaponCategory.SPEAR)) {
            if (spearLevel >= 3 && spearLevel <= 6) {
                return 1.1f;
            }
            if (spearLevel >= 7) {
                return 1.2f;
            }
        }
        return 1.0f;
    }

    public float getRangeMod(IsoGameCharacter chr) {
        int bluntLevel = chr.getPerkLevel(PerkFactory.Perks.Blunt);
        if (this.scriptItem.containsWeaponCategory(WeaponCategory.BLUNT) && bluntLevel >= 7) {
            return 1.2f;
        }
        int axeLevel = chr.getPerkLevel(PerkFactory.Perks.Axe);
        if (this.scriptItem.containsWeaponCategory(WeaponCategory.AXE) && axeLevel >= 7) {
            return 1.2f;
        }
        int spearLevel = chr.getPerkLevel(PerkFactory.Perks.Spear);
        if (this.scriptItem.containsWeaponCategory(WeaponCategory.SPEAR) && spearLevel >= 7) {
            return 1.2f;
        }
        return 1.0f;
    }

    public float getFatigueMod(IsoGameCharacter chr) {
        int bluntLevel = chr.getPerkLevel(PerkFactory.Perks.Blunt);
        if (this.scriptItem.containsWeaponCategory(WeaponCategory.BLUNT) && bluntLevel >= 8) {
            return 0.8f;
        }
        int axeLevel = chr.getPerkLevel(PerkFactory.Perks.Axe);
        if (this.scriptItem.containsWeaponCategory(WeaponCategory.AXE) && axeLevel >= 8) {
            return 0.8f;
        }
        int spearLevel = chr.getPerkLevel(PerkFactory.Perks.Spear);
        if (this.scriptItem.containsWeaponCategory(WeaponCategory.SPEAR) && spearLevel >= 8) {
            return 0.8f;
        }
        return 1.0f;
    }

    public float getKnockbackMod(IsoGameCharacter chr) {
        int axeLevel = chr.getPerkLevel(PerkFactory.Perks.Axe);
        if (this.scriptItem.containsWeaponCategory(WeaponCategory.AXE) && axeLevel >= 6) {
            return 2.0f;
        }
        return 1.0f;
    }

    public float getSpeedMod(IsoGameCharacter chr) {
        if (this.scriptItem.containsWeaponCategory(WeaponCategory.BLUNT)) {
            int bluntLevel = chr.getPerkLevel(PerkFactory.Perks.Blunt);
            if (bluntLevel >= 10) {
                return 0.65f;
            }
            if (bluntLevel >= 9) {
                return 0.68f;
            }
            if (bluntLevel >= 8) {
                return 0.71f;
            }
            if (bluntLevel >= 7) {
                return 0.74f;
            }
            if (bluntLevel >= 6) {
                return 0.77f;
            }
            if (bluntLevel >= 5) {
                return 0.8f;
            }
            if (bluntLevel >= 4) {
                return 0.83f;
            }
            if (bluntLevel >= 3) {
                return 0.86f;
            }
            if (bluntLevel >= 2) {
                return 0.9f;
            }
            if (bluntLevel >= 1) {
                return 0.95f;
            }
        }
        if (this.scriptItem.containsWeaponCategory(WeaponCategory.AXE)) {
            int axeLevel = chr.getPerkLevel(PerkFactory.Perks.Axe);
            float mod = 1.0f;
            if (chr.hasTrait(CharacterTrait.AXEMAN)) {
                mod = 0.95f;
            }
            if (axeLevel >= 10) {
                return 0.65f * mod;
            }
            if (axeLevel >= 9) {
                return 0.68f * mod;
            }
            if (axeLevel >= 8) {
                return 0.71f * mod;
            }
            if (axeLevel >= 7) {
                return 0.74f * mod;
            }
            if (axeLevel >= 6) {
                return 0.77f * mod;
            }
            if (axeLevel >= 5) {
                return 0.8f * mod;
            }
            if (axeLevel >= 4) {
                return 0.83f * mod;
            }
            if (axeLevel >= 3) {
                return 0.86f * mod;
            }
            if (axeLevel >= 2) {
                return 0.9f * mod;
            }
            if (axeLevel >= 1) {
                return 0.95f * mod;
            }
            return 1.0f * mod;
        }
        if (this.scriptItem.containsWeaponCategory(WeaponCategory.SPEAR)) {
            int spearLevel = chr.getPerkLevel(PerkFactory.Perks.Spear);
            if (spearLevel >= 10) {
                return 0.65f;
            }
            if (spearLevel >= 9) {
                return 0.68f;
            }
            if (spearLevel >= 8) {
                return 0.71f;
            }
            if (spearLevel >= 7) {
                return 0.74f;
            }
            if (spearLevel >= 6) {
                return 0.77f;
            }
            if (spearLevel >= 5) {
                return 0.8f;
            }
            if (spearLevel >= 4) {
                return 0.83f;
            }
            if (spearLevel >= 3) {
                return 0.86f;
            }
            if (spearLevel >= 2) {
                return 0.9f;
            }
            if (spearLevel >= 1) {
                return 0.95f;
            }
        }
        return 1.0f;
    }

    public float getToHitMod(IsoGameCharacter chr) {
        int bluntLevel = chr.getPerkLevel(PerkFactory.Perks.Blunt);
        if (this.scriptItem.containsWeaponCategory(WeaponCategory.BLUNT)) {
            if (bluntLevel == 1) {
                return 1.2f;
            }
            if (bluntLevel == 2) {
                return 1.3f;
            }
            if (bluntLevel == 3) {
                return 1.4f;
            }
            if (bluntLevel == 4) {
                return 1.5f;
            }
            if (bluntLevel == 5) {
                return 1.6f;
            }
            if (bluntLevel == 6) {
                return 1.7f;
            }
            if (bluntLevel == 7) {
                return 1.8f;
            }
            if (bluntLevel == 8) {
                return 1.9f;
            }
            if (bluntLevel == 9) {
                return 2.0f;
            }
            if (bluntLevel == 10) {
                return 100.0f;
            }
        }
        int axeLevel = chr.getPerkLevel(PerkFactory.Perks.Axe);
        if (this.scriptItem.containsWeaponCategory(WeaponCategory.AXE)) {
            if (axeLevel == 1) {
                return 1.2f;
            }
            if (axeLevel == 2) {
                return 1.3f;
            }
            if (axeLevel == 3) {
                return 1.4f;
            }
            if (axeLevel == 4) {
                return 1.5f;
            }
            if (axeLevel == 5) {
                return 1.6f;
            }
            if (axeLevel == 6) {
                return 1.7f;
            }
            if (axeLevel == 7) {
                return 1.8f;
            }
            if (axeLevel == 8) {
                return 1.9f;
            }
            if (axeLevel == 9) {
                return 2.0f;
            }
            if (axeLevel == 10) {
                return 100.0f;
            }
        }
        int spearLevel = chr.getPerkLevel(PerkFactory.Perks.Spear);
        if (this.scriptItem.containsWeaponCategory(WeaponCategory.SPEAR)) {
            if (spearLevel == 1) {
                return 1.2f;
            }
            if (spearLevel == 2) {
                return 1.3f;
            }
            if (spearLevel == 3) {
                return 1.4f;
            }
            if (spearLevel == 4) {
                return 1.5f;
            }
            if (spearLevel == 5) {
                return 1.6f;
            }
            if (spearLevel == 6) {
                return 1.7f;
            }
            if (spearLevel == 7) {
                return 1.8f;
            }
            if (spearLevel == 8) {
                return 1.9f;
            }
            if (spearLevel == 9) {
                return 2.0f;
            }
            if (spearLevel == 10) {
                return 100.0f;
            }
        }
        return 1.0f;
    }

    public PerkFactory.Perk getPerk() {
        if (this.weaponCategories.contains(WeaponCategory.AXE)) {
            return PerkFactory.Perks.Axe;
        }
        if (this.weaponCategories.contains(WeaponCategory.LONG_BLADE)) {
            return PerkFactory.Perks.LongBlade;
        }
        if (this.weaponCategories.contains(WeaponCategory.SPEAR)) {
            return PerkFactory.Perks.Spear;
        }
        if (this.weaponCategories.contains(WeaponCategory.SMALL_BLADE)) {
            return PerkFactory.Perks.SmallBlade;
        }
        if (this.weaponCategories.contains(WeaponCategory.SMALL_BLUNT)) {
            return PerkFactory.Perks.SmallBlunt;
        }
        if (WeaponType.getWeaponType(this).isRanged()) {
            return PerkFactory.Perks.Aiming;
        }
        return PerkFactory.Perks.Blunt;
    }

    public float muscleStrainMod(IsoGameCharacter chr) {
        float mod = 1.0f;
        int skill = this.getWeaponSkill(chr);
        mod -= (float)skill * 0.075f;
        return mod *= this.getStrainModifier();
    }

    public int getWeaponSkill(IsoGameCharacter chr) {
        return chr.getPerkLevel(this.getPerk());
    }

    public boolean isAngleFalloff() {
        return this.angleFalloff;
    }

    public void setAngleFalloff(boolean angleFalloff) {
        this.angleFalloff = angleFalloff;
    }

    public boolean isCanBarracade() {
        return this.canBarricade;
    }

    public void setCanBarracade(boolean bCanBarracade) {
        this.canBarricade = bCanBarracade;
    }

    public float getDoSwingBeforeImpact() {
        return this.doSwingBeforeImpact;
    }

    public void setDoSwingBeforeImpact(float doSwingBeforeImpact) {
        this.doSwingBeforeImpact = doSwingBeforeImpact;
    }

    public String getImpactSound() {
        return this.impactSound;
    }

    public void setImpactSound(String impactSound) {
        this.impactSound = impactSound;
    }

    public boolean isKnockBackOnNoDeath() {
        return this.knockBackOnNoDeath;
    }

    public void setKnockBackOnNoDeath(boolean knockBackOnNoDeath) {
        this.knockBackOnNoDeath = knockBackOnNoDeath;
    }

    public float getMaxAngle() {
        return this.maxAngle;
    }

    public void setMaxAngle(float maxAngle) {
        this.maxAngle = maxAngle;
    }

    public float getMaxDamage() {
        float max = this.maxDamage;
        if (this.hasSharpness() && this.maxDamage > this.getMinDamage()) {
            float maxAdd = this.maxDamage - this.getMinDamage();
            max = (maxAdd *= this.getSharpnessMultiplier()) + this.getMinDamage();
        }
        return max;
    }

    public void setMaxDamage(float maxDamage) {
        this.maxDamage = maxDamage;
    }

    public int getMaxHitCount() {
        return this.maxHitCount;
    }

    public void setMaxHitCount(int maxHitCount) {
        this.maxHitCount = maxHitCount;
    }

    public float getMaxRange() {
        return this.maxRange;
    }

    public float getMaxRange(IsoGameCharacter owner) {
        if (this.isRanged()) {
            return this.maxRange + this.getAimingPerkRangeModifier() * ((float)owner.getPerkLevel(PerkFactory.Perks.Aiming) / 2.0f);
        }
        return this.maxRange;
    }

    public void setMaxRange(float maxRange) {
        this.maxRange = maxRange;
    }

    public boolean isRanged() {
        return this.ranged;
    }

    public void setRanged(boolean ranged) {
        this.ranged = ranged;
    }

    public float getMinAngle() {
        return this.minAngle;
    }

    public void setMinAngle(float minAngle) {
        this.minAngle = minAngle;
    }

    public float getMinDamage() {
        return this.minDamage;
    }

    public void setMinDamage(float minDamage) {
        this.minDamage = minDamage;
    }

    public float getMinimumSwingTime() {
        return this.minimumSwingTime;
    }

    public void setMinimumSwingTime(float minimumSwingTime) {
        this.minimumSwingTime = minimumSwingTime;
    }

    public float getMinRange() {
        return this.minRange;
    }

    public void setMinRange(float minRange) {
        this.minRange = minRange;
    }

    public float getNoiseFactor() {
        return this.noiseFactor;
    }

    public void setNoiseFactor(float noiseFactor) {
        this.noiseFactor = noiseFactor;
    }

    public ItemTag getOtherHandRequire() {
        return this.otherHandRequire;
    }

    public void setOtherHandRequire(ItemTag otherHandRequire) {
        this.otherHandRequire = otherHandRequire;
    }

    public boolean isOtherHandUse() {
        return this.otherHandUse;
    }

    public void setOtherHandUse(boolean otherHandUse) {
        this.otherHandUse = otherHandUse;
    }

    public String getPhysicsObject() {
        return this.physicsObject;
    }

    public void setPhysicsObject(String physicsObject) {
        this.physicsObject = physicsObject;
    }

    public float getPushBackMod() {
        return this.pushBackMod;
    }

    public void setPushBackMod(float pushBackMod) {
        this.pushBackMod = pushBackMod;
    }

    public boolean isRangeFalloff() {
        return this.rangeFalloff;
    }

    public void setRangeFalloff(boolean rangeFalloff) {
        this.rangeFalloff = rangeFalloff;
    }

    public int getSoundRadius() {
        return this.soundRadius;
    }

    public void setSoundRadius(int soundRadius) {
        this.soundRadius = soundRadius;
    }

    public int getSoundVolume() {
        return this.soundVolume;
    }

    public void setSoundVolume(int soundVolume) {
        this.soundVolume = soundVolume;
    }

    public boolean isSplatBloodOnNoDeath() {
        return this.splatBloodOnNoDeath;
    }

    public void setSplatBloodOnNoDeath(boolean splatBloodOnNoDeath) {
        this.splatBloodOnNoDeath = splatBloodOnNoDeath;
    }

    public int getSplatNumber() {
        return this.splatNumber;
    }

    public void setSplatNumber(int splatNumber) {
        this.splatNumber = splatNumber;
    }

    public String getSwingSound() {
        return this.swingSound;
    }

    public void setSwingSound(String swingSound) {
        this.swingSound = swingSound;
    }

    public float getSwingTime() {
        return this.swingTime;
    }

    public void setSwingTime(float swingTime) {
        this.swingTime = swingTime;
    }

    public float getToHitModifier() {
        return this.toHitModifier;
    }

    public void setToHitModifier(float toHitModifier) {
        this.toHitModifier = toHitModifier;
    }

    public boolean isUseEndurance() {
        return this.useEndurance;
    }

    public void setUseEndurance(boolean useEndurance) {
        this.useEndurance = useEndurance;
    }

    public boolean isUseSelf() {
        return this.useSelf;
    }

    public void setUseSelf(boolean useSelf) {
        this.useSelf = useSelf;
    }

    public String getWeaponSprite() {
        if (this.getModelIndex() != -1 && this.getWeaponSpritesByIndex() != null) {
            return this.getWeaponSpritesByIndex().get(this.getModelIndex());
        }
        return this.weaponSprite;
    }

    public void setWeaponSprite(String weaponSprite) {
        this.weaponSprite = weaponSprite;
    }

    public float getOtherBoost() {
        return this.otherBoost;
    }

    public void setOtherBoost(float otherBoost) {
        this.otherBoost = otherBoost;
    }

    public int getDoorDamage() {
        if (this.doorDamage < 1) {
            return 0;
        }
        int damage = (int)((float)this.doorDamage * this.getSharpnessMultiplier());
        return Math.max(damage, 1);
    }

    public void setDoorDamage(int doorDamage) {
        this.doorDamage = doorDamage;
    }

    public String getDoorHitSound() {
        return this.doorHitSound;
    }

    public void setDoorHitSound(String doorHitSound) {
        this.doorHitSound = doorHitSound;
    }

    @Override
    public int getConditionLowerChance() {
        return this.conditionLowerChance;
    }

    public void setConditionLowerChance(int conditionLowerChance) {
        this.conditionLowerChance = conditionLowerChance;
    }

    public boolean isMultipleHitConditionAffected() {
        return this.multipleHitConditionAffected;
    }

    public void setMultipleHitConditionAffected(boolean multipleHitConditionAffected) {
        this.multipleHitConditionAffected = multipleHitConditionAffected;
    }

    public boolean isShareEndurance() {
        return this.shareEndurance;
    }

    public void setShareEndurance(boolean shareEndurance) {
        this.shareEndurance = shareEndurance;
    }

    public boolean isAlwaysKnockdown() {
        return this.alwaysKnockdown;
    }

    public void setAlwaysKnockdown(boolean alwaysKnockdown) {
        this.alwaysKnockdown = alwaysKnockdown;
    }

    public float getEnduranceMod() {
        return this.enduranceMod;
    }

    public void setEnduranceMod(float enduranceMod) {
        this.enduranceMod = enduranceMod;
    }

    public float getKnockdownMod() {
        return this.knockdownMod;
    }

    public void setKnockdownMod(float knockdownMod) {
        this.knockdownMod = knockdownMod;
    }

    public boolean isCantAttackWithLowestEndurance() {
        return this.cantAttackWithLowestEndurance;
    }

    public void setCantAttackWithLowestEndurance(boolean cantAttackWithLowestEndurance) {
        this.cantAttackWithLowestEndurance = cantAttackWithLowestEndurance;
    }

    public boolean isAimedFirearm() {
        return this.isAimedFirearm;
    }

    public boolean isAimedHandWeapon() {
        return this.isAimedHandWeapon;
    }

    public int getProjectileCount() {
        return this.projectileCount;
    }

    public void setProjectileCount(int count) {
        this.projectileCount = count;
    }

    public float getProjectileSpread() {
        return this.projectileSpread;
    }

    public void setProjectileSpread(float projectileSpread) {
        this.projectileSpread = projectileSpread;
    }

    public float getProjectileWeightCenter() {
        return this.projectileWeightCenter;
    }

    public void setProjectileWeightCenter(float projectileWeightCenter) {
        this.projectileWeightCenter = projectileWeightCenter;
    }

    public void setMuzzleFlashModelKey(ModelKey muzzleFlashModelKey) {
        this.muzzleFlashModelKey = muzzleFlashModelKey;
    }

    public ModelKey getMuzzleFlashModelKey() {
        return this.muzzleFlashModelKey;
    }

    public float getAimingMod() {
        return 1.0f;
    }

    public boolean isAimed() {
        return this.isAimedFirearm || this.isAimedHandWeapon;
    }

    public void setCriticalChance(float criticalChance) {
        this.criticalChance = criticalChance;
    }

    public float getCriticalChance() {
        if (this.hasSharpness()) {
            return this.criticalChance * this.getSharpness();
        }
        return this.criticalChance;
    }

    public void setSubCategory(String subcategory) {
        this.subCategory = subcategory;
    }

    public String getSubCategory() {
        return this.subCategory;
    }

    public void setZombieHitSound(String hitSound) {
        this.hitSound = hitSound;
    }

    public String getZombieHitSound() {
        return this.hitSound;
    }

    public boolean isOfWeaponCategory(WeaponCategory weaponCategory) {
        return this.weaponCategories.contains(weaponCategory);
    }

    public void setWeaponCategories(Set<WeaponCategory> weaponCategories) {
        this.weaponCategories = weaponCategories;
    }

    public int getAimingPerkCritModifier() {
        return this.aimingPerkCritModifier;
    }

    public void setAimingPerkCritModifier(int aimingPerkCritModifier) {
        this.aimingPerkCritModifier = aimingPerkCritModifier;
    }

    public float getAimingPerkRangeModifier() {
        return this.aimingPerkRangeModifier;
    }

    public void setAimingPerkRangeModifier(float aimingPerkRangeModifier) {
        this.aimingPerkRangeModifier = aimingPerkRangeModifier;
    }

    public int getHitChance() {
        return this.hitChance;
    }

    public void setHitChance(int hitChance) {
        this.hitChance = hitChance;
    }

    public float getAimingPerkHitChanceModifier() {
        return this.aimingPerkHitChanceModifier;
    }

    public void setAimingPerkHitChanceModifier(float aimingPerkHitChanceModifier) {
        this.aimingPerkHitChanceModifier = aimingPerkHitChanceModifier;
    }

    public float getAimingPerkMinAngleModifier() {
        return this.aimingPerkMinAngleModifier;
    }

    public void setAimingPerkMinAngleModifier(float aimingPerkMinAngleModifier) {
        this.aimingPerkMinAngleModifier = aimingPerkMinAngleModifier;
    }

    public int getRecoilDelay() {
        return this.recoilDelay;
    }

    public int getRecoilDelay(IsoGameCharacter owner) {
        return PZMath.max(0, (int)((float)this.recoilDelay * (1.0f - (float)owner.getPerkLevel(PerkFactory.Perks.Aiming) / 40.0f) * (1.0f - (-10.0f + (float)owner.getPerkLevel(PerkFactory.Perks.Strength) * 2.0f) / 40.0f) * (owner.getPrimaryHandItem() == this && owner.getSecondaryHandItem() != this && owner.getSecondaryHandItem() != null ? 1.3f : 1.0f)));
    }

    public void setRecoilDelay(int recoilDelay) {
        this.recoilDelay = recoilDelay;
    }

    public boolean isPiercingBullets() {
        return this.piercingBullets;
    }

    public void setPiercingBullets(boolean piercingBullets) {
        this.piercingBullets = piercingBullets;
    }

    public float getSoundGain() {
        return this.soundGain;
    }

    public void setSoundGain(float soundGain) {
        this.soundGain = soundGain;
    }

    public int getClipSize() {
        return this.clipSize;
    }

    public void setClipSize(int capacity) {
        this.clipSize = capacity;
    }

    @Override
    public void save(ByteBuffer output, boolean net) throws IOException {
        super.save(output, net);
        BitHeaderWrite bits = BitHeader.allocWrite(BitHeader.HeaderSize.Integer, output);
        if (this.maxRange != 1.0f) {
            bits.addFlags(1);
            output.putFloat(this.maxRange);
        }
        if (this.minRangeRanged != 0.0f) {
            bits.addFlags(2);
            output.putFloat(this.minRangeRanged);
        }
        if (this.clipSize != 0) {
            bits.addFlags(4);
            output.putInt(this.clipSize);
        }
        if (this.minDamage != 0.4f) {
            bits.addFlags(8);
            output.putFloat(this.minDamage);
        }
        if (this.maxDamage != 1.5f) {
            bits.addFlags(16);
            output.putFloat(this.maxDamage);
        }
        if (this.recoilDelay != 0) {
            bits.addFlags(32);
            output.putInt(this.recoilDelay);
        }
        if (this.aimingTime != 0) {
            bits.addFlags(64);
            output.putInt(this.aimingTime);
        }
        if (this.reloadTime != 0) {
            bits.addFlags(128);
            output.putInt(this.reloadTime);
        }
        if (this.hitChance != 0) {
            bits.addFlags(256);
            output.putInt(this.hitChance);
        }
        if (this.minAngle != 0.5f) {
            bits.addFlags(512);
            output.putFloat(this.minAngle);
        }
        if (!this.attachmentList.isEmpty()) {
            bits.addFlags(1024);
            output.put((byte)this.attachmentList.size());
            for (int i = 0; i < this.attachmentList.size(); ++i) {
                this.attachmentList.get(i).save(output, net);
            }
        }
        if (this.fireMode != null) {
            bits.addFlags(2048);
            GameWindow.WriteString(output, this.fireMode);
        }
        if (this.cyclicRateMultiplier > 0.0f) {
            bits.addFlags(4096);
            output.putFloat(this.cyclicRateMultiplier);
        }
        if (this.getExplosionTimer() != 0) {
            bits.addFlags(65536);
            output.putInt(this.getExplosionTimer());
        }
        if (this.maxAngle != 1.0f) {
            bits.addFlags(131072);
            output.putFloat(this.maxAngle);
        }
        if (this.bloodLevel != 0.0f) {
            bits.addFlags(262144);
            output.putFloat(this.bloodLevel);
        }
        if (this.containsClip) {
            bits.addFlags(524288);
        }
        if (this.roundChambered) {
            bits.addFlags(0x100000);
        }
        if (this.isJammed) {
            bits.addFlags(0x200000);
        }
        if (!StringUtils.equals(this.weaponSprite, this.getScriptItem().getWeaponSprite())) {
            bits.addFlags(0x400000);
            GameWindow.WriteString(output, this.weaponSprite);
        }
        if (this.minSightRange != 2.0f) {
            bits.addFlags(0x800000);
            output.putFloat(this.minSightRange);
        }
        if (this.maxSightRange != 6.0f) {
            bits.addFlags(0x1000000);
            output.putFloat(this.maxSightRange);
        }
        bits.write();
        bits.release();
    }

    @Override
    public void load(ByteBuffer input, int worldVersion) throws IOException {
        this.clearAllWeaponParts();
        super.load(input, worldVersion);
        this.maxRange = 1.0f;
        this.minRangeRanged = 0.0f;
        this.clipSize = 0;
        this.minDamage = 0.4f;
        this.maxDamage = 1.5f;
        this.recoilDelay = 0;
        this.aimingTime = 0;
        this.reloadTime = 0;
        this.hitChance = 0;
        this.minAngle = 0.5f;
        this.explosionTimer = 0;
        this.maxAngle = 1.0f;
        this.bloodLevel = 0.0f;
        this.containsClip = false;
        this.roundChambered = false;
        this.isJammed = false;
        this.weaponSprite = this.getScriptItem().getWeaponSprite();
        this.isMelee = !this.weaponCategories.isEmpty();
        this.isExplosive = "Explosives".equals(this.scriptItem.displayCategory);
        BitHeaderRead bits = BitHeader.allocRead(BitHeader.HeaderSize.Integer, input);
        if (!bits.equals(0)) {
            WeaponPart weaponPart;
            InventoryItem item;
            byte b;
            byte count;
            if (bits.hasFlags(1)) {
                this.setMaxRange(input.getFloat());
            }
            if (bits.hasFlags(2)) {
                this.setMinRangeRanged(input.getFloat());
            }
            if (bits.hasFlags(4)) {
                this.setClipSize(input.getInt());
            }
            if (bits.hasFlags(8)) {
                this.setMinDamage(input.getFloat());
            }
            if (bits.hasFlags(16)) {
                this.setMaxDamage(input.getFloat());
            }
            if (bits.hasFlags(32)) {
                this.setRecoilDelay(input.getInt());
            }
            if (bits.hasFlags(64)) {
                this.setAimingTime(input.getInt());
            }
            if (bits.hasFlags(128)) {
                this.setReloadTime(input.getInt());
            }
            if (bits.hasFlags(256)) {
                this.setHitChance(input.getInt());
            }
            if (bits.hasFlags(512)) {
                this.setMinAngle(input.getFloat());
            }
            if (bits.hasFlags(1024)) {
                count = input.get();
                for (b = 0; b < count; b = (byte)(b + 1)) {
                    item = InventoryItemFactory.CreateItem(input.getShort());
                    input.get();
                    item.load(input, worldVersion);
                    if (!(item instanceof WeaponPart)) continue;
                    weaponPart = (WeaponPart)item;
                    this.attachWeaponPart(null, weaponPart, false);
                }
            }
            if (bits.hasFlags(2048)) {
                this.setFireMode(GameWindow.ReadString(input));
            }
            if (bits.hasFlags(4096)) {
                this.setCyclicRateMultiplier(input.getFloat());
            }
            if (bits.hasFlags(65536)) {
                this.setExplosionTimer(input.getInt());
            }
            if (bits.hasFlags(131072)) {
                this.setMaxAngle(input.getFloat());
            }
            if (bits.hasFlags(262144)) {
                this.setBloodLevel(input.getFloat());
            }
            this.setContainsClip(bits.hasFlags(524288));
            if (StringUtils.isNullOrWhitespace(this.magazineType)) {
                this.setContainsClip(false);
            }
            this.setRoundChambered(bits.hasFlags(0x100000));
            this.setJammed(bits.hasFlags(0x200000));
            if (bits.hasFlags(0x400000)) {
                this.setWeaponSprite(GameWindow.ReadString(input));
            }
            if (bits.hasFlags(0x800000)) {
                this.setMinSightRange(input.getFloat());
            }
            if (bits.hasFlags(0x1000000)) {
                this.setMaxSightRange(input.getFloat());
            }
            if (bits.hasFlags(0x2000000)) {
                count = input.get();
                for (b = 0; b < count; b = (byte)(b + 1)) {
                    item = InventoryItemFactory.CreateItem(input.getShort());
                    if (!(item instanceof WeaponPart)) continue;
                    weaponPart = (WeaponPart)item;
                    this.attachWeaponPart(null, weaponPart, false);
                }
            }
        }
        bits.release();
    }

    public WeaponPart getActiveLight() {
        return this.activeLight;
    }

    public void setActiveLight(WeaponPart part) {
        this.activeLight = part;
    }

    public WeaponPart getActiveSight() {
        return this.activeSight;
    }

    public void setActiveSight(WeaponPart part) {
        this.activeSight = part;
    }

    public void setMinSightRange(float value) {
        this.minSightRange = value;
    }

    public float getMinSightRange() {
        return this.minSightRange;
    }

    public float getMinSightRange(IsoGameCharacter character) {
        return (this.activeSight != null ? this.activeSight.getMinSightRange() : this.minSightRange) * (1.0f - (float)character.getPerkLevel(PerkFactory.Perks.Aiming) / 30.0f);
    }

    public void setMaxSightRange(float value) {
        this.maxSightRange = value;
    }

    public float getMaxSightRange() {
        return this.maxSightRange;
    }

    public float getMaxSightRange(IsoGameCharacter character) {
        if (character.hasTrait(CharacterTrait.SHORT_SIGHTED) && !character.isWearingGlasses()) {
            return this.getMinSightRange(character);
        }
        return (this.activeSight != null ? this.activeSight.getMaxSightRange() : this.maxSightRange) * (1.0f + (float)character.getPerkLevel(PerkFactory.Perks.Aiming) / 30.0f) * (character.hasTrait(CharacterTrait.EAGLE_EYED) ? 1.2f : 1.0f);
    }

    public float getLowLightBonus() {
        return this.activeSight != null ? this.activeSight.getLowLightBonus() : 0.0f;
    }

    public float getMinRangeRanged() {
        return this.minRangeRanged;
    }

    public void setMinRangeRanged(float minRangeRanged) {
        this.minRangeRanged = minRangeRanged;
    }

    public int getReloadTime() {
        return this.reloadTime;
    }

    public void setReloadTime(int reloadTime) {
        this.reloadTime = reloadTime;
    }

    public int getAimingTime() {
        return this.aimingTime;
    }

    public void setAimingTime(int aimingTime) {
        this.aimingTime = aimingTime;
    }

    public int getTreeDamage() {
        if (this.treeDamage < 1) {
            return 0;
        }
        int damage = (int)((float)this.treeDamage * this.getSharpnessMultiplier());
        return Math.max(damage, 1);
    }

    public void setTreeDamage(int treeDamage) {
        this.treeDamage = treeDamage;
    }

    public String getBulletOutSound() {
        return this.bulletOutSound;
    }

    public void setBulletOutSound(String bulletOutSound) {
        this.bulletOutSound = bulletOutSound;
    }

    public String getShellFallSound() {
        return this.shellFallSound;
    }

    public void setShellFallSound(String shellFallSound) {
        this.shellFallSound = shellFallSound;
    }

    private void addPartToList(String type, ArrayList<WeaponPart> list) {
        WeaponPart part = this.getWeaponPart(type);
        if (part != null) {
            list.add(part);
        }
    }

    public List<WeaponPart> getAllWeaponParts() {
        return this.attachmentList;
    }

    public List<WeaponPart> getAllWeaponParts(List<WeaponPart> result) {
        result.clear();
        result.addAll(this.attachmentList);
        return result;
    }

    public List<WeaponPart> getDetachableWeaponParts(IsoGameCharacter character) {
        return new ArrayList<WeaponPart>(this.attachmentList.stream().filter(weaponPart -> weaponPart.canDetach(character, this)).toList());
    }

    public void clearAllWeaponParts() {
        this.activeLight = null;
        this.activeSight = null;
        this.attachments.clear();
        this.attachmentList.clear();
    }

    public void clearWeaponPart(WeaponPart part) {
        if (part == null) {
            return;
        }
        if (part == this.activeLight) {
            this.activeLight = null;
        }
        if (part == this.activeSight) {
            this.activeSight = null;
        }
        this.attachments.remove(part.getPartType());
        this.attachmentList.remove(part);
    }

    public void clearWeaponPart(String partType) {
        this.clearWeaponPart(this.attachments.get(partType));
    }

    public void setWeaponPart(WeaponPart part) {
        this.setWeaponPart(part.getPartType(), part);
    }

    public void setWeaponPart(String partType, WeaponPart part) {
        if (StringUtils.isNullOrEmpty(partType)) {
            return;
        }
        this.clearWeaponPart(partType);
        if (part != null) {
            if (part.hasTag(ItemTag.OPTICS) && this.activeSight == null) {
                this.activeSight = part;
            }
            if (part.isTorchCone() && this.activeLight == null) {
                this.activeLight = part;
            }
            this.attachments.put(partType, part);
            this.attachmentList.add(part);
        }
    }

    public WeaponPart getWeaponPart(WeaponPart part) {
        return this.attachments.get(part.getPartType());
    }

    public WeaponPart getWeaponPart(String location) {
        return this.attachments.get(location);
    }

    public float getWeaponPartWeightModifier(String type) {
        return this.getWeaponPartWeightModifier(this.getWeaponPart(type));
    }

    public float getWeaponPartWeightModifier(WeaponPart part) {
        return part == null ? 0.0f : part.getWeightModifier();
    }

    public void attachWeaponPart(WeaponPart part) {
        this.attachWeaponPart(null, part, true);
    }

    public void attachWeaponPart(WeaponPart part, boolean doChange) {
        this.attachWeaponPart(null, part, doChange);
    }

    public void attachWeaponPart(IsoGameCharacter character, WeaponPart part) {
        this.attachWeaponPart(character, part, true);
    }

    public void attachWeaponPart(IsoGameCharacter character, WeaponPart part, boolean doChange) {
        if (part == null) {
            return;
        }
        if (this.attachments.containsKey(part.getPartType())) {
            this.detachWeaponPart(character, this.attachments.get(part.getPartType()), doChange);
        }
        this.setWeaponPart(part);
        if (!doChange) {
            return;
        }
        this.setMaxRange(this.getMaxRange() + part.getMaxRange());
        this.setReloadTime(this.getReloadTime() + part.getReloadTime());
        this.setRecoilDelay((int)((float)this.getRecoilDelay() + part.getRecoilDelay()));
        this.setAimingTime(this.getAimingTime() + part.getAimingTime());
        this.setHitChance(this.getHitChance() + part.getHitChance());
        this.setProjectileSpread(this.getProjectileSpread() + part.getSpreadModifier());
        this.setMinDamage(this.getMinDamage() + part.getDamage());
        this.setMaxDamage(this.getMaxDamage() + part.getDamage());
        part.onAttach(character, this);
    }

    public void detachAllWeaponParts() {
        for (WeaponPart p : new ArrayList<WeaponPart>(this.attachmentList)) {
            this.detachWeaponPart(null, p, true);
        }
    }

    public void detachWeaponPart(WeaponPart part) {
        this.detachWeaponPart(null, part, true);
    }

    public void detachWeaponPart(String location) {
        this.detachWeaponPart(null, this.getWeaponPart(location), true);
    }

    public void detachWeaponPart(IsoGameCharacter character, WeaponPart part) {
        this.detachWeaponPart(character, part, true);
    }

    public void detachWeaponPart(IsoGameCharacter character, WeaponPart part, boolean doChange) {
        if (part == null || !this.getAllWeaponParts().contains(part)) {
            return;
        }
        WeaponPart current = this.getWeaponPart(part.getPartType());
        if (current != part) {
            return;
        }
        this.clearWeaponPart(part);
        if (!doChange) {
            return;
        }
        this.setMaxRange(this.getMaxRange() - part.getMaxRange());
        this.setClipSize(this.getClipSize() - part.getClipSize());
        this.setReloadTime(this.getReloadTime() - part.getReloadTime());
        this.setRecoilDelay((int)((float)this.getRecoilDelay() - part.getRecoilDelay()));
        this.setAimingTime(this.getAimingTime() - part.getAimingTime());
        this.setHitChance(this.getHitChance() - part.getHitChance());
        this.setProjectileSpread(this.getProjectileSpread() - part.getSpreadModifier());
        this.setMinDamage(this.getMinDamage() - part.getDamage());
        this.setMaxDamage(this.getMaxDamage() - part.getDamage());
        part.onDetach(character, this);
    }

    public int getTriggerExplosionTimer() {
        return this.triggerExplosionTimer;
    }

    public void setTriggerExplosionTimer(int triggerExplosionTimer) {
        this.triggerExplosionTimer = triggerExplosionTimer;
    }

    public boolean canBePlaced() {
        return this.canBePlaced;
    }

    public void setCanBePlaced(boolean canBePlaced) {
        this.canBePlaced = canBePlaced;
    }

    public int getExplosionRange() {
        return this.explosionRange;
    }

    public void setExplosionRange(int explosionRange) {
        this.explosionRange = explosionRange;
    }

    public int getExplosionPower() {
        return this.explosionPower;
    }

    public void setExplosionPower(int explosionPower) {
        this.explosionPower = explosionPower;
    }

    public int getFireRange() {
        return this.fireRange;
    }

    public void setFireRange(int fireRange) {
        this.fireRange = fireRange;
    }

    public int getSmokeRange() {
        return this.smokeRange;
    }

    public void setSmokeRange(int smokeRange) {
        this.smokeRange = smokeRange;
    }

    public int getFireStartingEnergy() {
        return this.fireStartingEnergy;
    }

    public void setFireStartingEnergy(int fireStartingEnergy) {
        this.fireStartingEnergy = fireStartingEnergy;
    }

    public int getFireStartingChance() {
        return this.fireStartingChance;
    }

    public void setFireStartingChance(int fireStartingChance) {
        this.fireStartingChance = fireStartingChance;
    }

    public int getNoiseRange() {
        return this.noiseRange;
    }

    public void setNoiseRange(int noiseRange) {
        this.noiseRange = noiseRange;
    }

    public int getNoiseDuration() {
        return this.getScriptItem().getNoiseDuration();
    }

    public float getExtraDamage() {
        return this.extraDamage;
    }

    public void setExtraDamage(float extraDamage) {
        this.extraDamage = extraDamage;
    }

    public int getExplosionTimer() {
        return this.explosionTimer;
    }

    public void setExplosionTimer(int explosionTimer) {
        this.explosionTimer = explosionTimer;
    }

    public int getExplosionDuration() {
        return this.explosionDuration;
    }

    public void setExplosionDuration(int seconds) {
        this.explosionDuration = seconds;
    }

    public String getPlacedSprite() {
        return this.placedSprite;
    }

    public void setPlacedSprite(String placedSprite) {
        this.placedSprite = placedSprite;
    }

    public boolean canBeReused() {
        return this.canBeReused;
    }

    public void setCanBeReused(boolean canBeReused) {
        this.canBeReused = canBeReused;
    }

    public int getSensorRange() {
        return this.sensorRange;
    }

    public void setSensorRange(int sensorRange) {
        this.sensorRange = sensorRange;
    }

    public String getRunAnim() {
        return this.runAnim;
    }

    public float getCriticalDamageMultiplier() {
        if (this.hasSharpness()) {
            return this.criticalDamageMultiplier * this.getSharpnessMultiplier();
        }
        return this.criticalDamageMultiplier;
    }

    public void setCriticalDamageMultiplier(float criticalDamageMultiplier) {
        this.criticalDamageMultiplier = criticalDamageMultiplier;
    }

    @Override
    public String getStaticModel() {
        if (this.getModelIndex() != -1 && this.getWeaponSpritesByIndex() != null) {
            return this.getWeaponSpritesByIndex().get(this.getModelIndex());
        }
        return this.staticModel != null ? this.staticModel : this.weaponSprite;
    }

    @Override
    public String getStaticModelException() {
        if (this.hasTag(ItemTag.USE_WORLD_STATIC_MODEL)) {
            return this.getWorldStaticModel();
        }
        return this.getStaticModel();
    }

    public float getBaseSpeed() {
        return this.baseSpeed;
    }

    public void setBaseSpeed(float baseSpeed) {
        this.baseSpeed = baseSpeed;
    }

    @Override
    public float getBloodLevel() {
        return this.bloodLevel;
    }

    @Override
    public void setBloodLevel(float level) {
        this.bloodLevel = Math.max(0.0f, Math.min(1.0f, level));
    }

    public void setWeaponLength(float weaponLength) {
        this.weaponLength = weaponLength;
    }

    public String getAmmoBox() {
        return this.ammoBox;
    }

    public void setAmmoBox(String ammoBox) {
        this.ammoBox = ammoBox;
    }

    public String getMagazineType() {
        return this.magazineType;
    }

    public void setMagazineType(String magazineType) {
        this.magazineType = magazineType;
    }

    public String getEjectAmmoStartSound() {
        return this.getScriptItem().getEjectAmmoStartSound();
    }

    public String getEjectAmmoSound() {
        return this.getScriptItem().getEjectAmmoSound();
    }

    public String getEjectAmmoStopSound() {
        return this.getScriptItem().getEjectAmmoStopSound();
    }

    public String getInsertAmmoStartSound() {
        return this.getScriptItem().getInsertAmmoStartSound();
    }

    public String getInsertAmmoSound() {
        return this.getScriptItem().getInsertAmmoSound();
    }

    public String getInsertAmmoStopSound() {
        return this.getScriptItem().getInsertAmmoStopSound();
    }

    public String getRackSound() {
        return this.rackSound;
    }

    public void setRackSound(String rackSound) {
        this.rackSound = rackSound;
    }

    public boolean isReloadable(IsoGameCharacter owner) {
        return this.isRanged();
    }

    public boolean isContainsClip() {
        return this.containsClip;
    }

    public void setContainsClip(boolean containsClip) {
        this.containsClip = this.usesExternalMagazine() && containsClip;
    }

    public InventoryItem getBestMagazine(IsoGameCharacter owner) {
        if (StringUtils.isNullOrEmpty(this.getMagazineType())) {
            return null;
        }
        return owner.getInventory().getBestTypeRecurse(this.getMagazineType(), magazineComparator);
    }

    public String getWeaponReloadType() {
        return this.weaponReloadType;
    }

    public void setWeaponReloadType(String weaponReloadType) {
        this.weaponReloadType = weaponReloadType;
    }

    public boolean isRackAfterShoot() {
        return this.rackAfterShoot;
    }

    public void setRackAfterShoot(boolean rackAfterShoot) {
        this.rackAfterShoot = rackAfterShoot;
    }

    public boolean isRoundChambered() {
        return this.roundChambered;
    }

    public void setRoundChambered(boolean roundChambered) {
        this.roundChambered = this.haveChamber && roundChambered;
    }

    public boolean isSpentRoundChambered() {
        return this.spentRoundChambered;
    }

    public void setSpentRoundChambered(boolean roundChambered) {
        this.spentRoundChambered = roundChambered;
    }

    public int getSpentRoundCount() {
        return this.spentRoundCount;
    }

    public void setSpentRoundCount(int count) {
        this.spentRoundCount = PZMath.clamp(count, 0, this.getMaxAmmo());
    }

    public boolean isManuallyRemoveSpentRounds() {
        return this.getScriptItem().isManuallyRemoveSpentRounds();
    }

    public int getAmmoPerShoot() {
        return this.ammoPerShoot;
    }

    public void setAmmoPerShoot(int ammoPerShoot) {
        this.ammoPerShoot = ammoPerShoot;
    }

    public float getJamGunChance() {
        return this.jamGunChance;
    }

    public void setJamGunChance(float jamGunChance) {
        this.jamGunChance = jamGunChance;
    }

    public boolean isJammed() {
        return this.isJammed;
    }

    public void setJammed(boolean isJammed) {
        this.isJammed = isJammed;
    }

    public boolean checkJam(IsoPlayer player, boolean racking) {
        float chance = (float)((double)this.jamGunChance * SandboxOptions.instance.firearmJamMultiplier.getValue());
        if (chance == 0.0f) {
            return false;
        }
        boolean limp = !racking && !this.isManuallyRemoveSpentRounds() && (player.getPerkLevel(PerkFactory.Perks.Aiming) < 3 && player.getPerkLevel(PerkFactory.Perks.Strength) < 5 || player.getPerkLevel(PerkFactory.Perks.Aiming) < 5 && player.getPerkLevel(PerkFactory.Perks.Strength) < 3);
        float modifier = 8.0f * ((float)this.getCondition() / (float)this.getConditionMax());
        chance = (chance + (limp ? 0.5f : 0.0f) + (float)(this.getConditionMax() - this.getCondition()) / modifier) * 0.01f;
        float roll = Rand.Next(0.0f, 1.0f);
        DebugType.Combat.debugln("Jam chance: %f, roll: %f, jammed: %s", Float.valueOf(chance), Float.valueOf(roll), roll < chance);
        if (roll < chance) {
            this.setJammed(true);
        }
        return this.isJammed;
    }

    public boolean checkUnJam(IsoPlayer player) {
        float modifier = 8.0f * ((float)this.getCondition() / (float)this.getConditionMax());
        float chance = 8.0f - (float)player.getPerkLevel(PerkFactory.Perks.Aiming) * 0.5f + (float)((player.getMoodleLevel(MoodleType.PANIC) + player.getMoodleLevel(MoodleType.STRESS) + player.getMoodleLevel(MoodleType.DRUNK)) * 3);
        chance -= player.hasTrait(CharacterTrait.DEXTROUS) ? 2.0f : 0.0f;
        chance += player.hasTrait(CharacterTrait.ALL_THUMBS) ? 2.0f : 0.0f;
        chance = PZMath.max(chance, 1.0f);
        chance = 1.0f - (chance + (float)(this.getConditionMax() - this.getCondition()) / modifier) * 0.01f;
        float roll = Rand.Next(0.0f, 1.0f);
        DebugType.Combat.debugln("UnJam chance: %f, roll: %f, unjammed: %s", Float.valueOf(chance), Float.valueOf(roll), roll < chance);
        if (roll < chance) {
            this.setJammed(false);
            if (this.shellFallSound != null) {
                player.getEmitter().playSound(this.shellFallSound);
            }
        }
        return this.isJammed;
    }

    public String getClickSound() {
        return this.clickSound;
    }

    public void setClickSound(String clickSound) {
        this.clickSound = clickSound;
    }

    public ArrayList<ModelWeaponPart> getModelWeaponPart() {
        return this.modelWeaponPart;
    }

    public void setModelWeaponPart(ArrayList<ModelWeaponPart> modelWeaponPart) {
        this.modelWeaponPart = modelWeaponPart;
    }

    public String getOriginalWeaponSprite() {
        return this.originalWeaponSprite;
    }

    public void setOriginalWeaponSprite(String originalWeaponSprite) {
        this.originalWeaponSprite = originalWeaponSprite;
    }

    public boolean haveChamber() {
        return this.haveChamber;
    }

    public void setHaveChamber(boolean haveChamber) {
        this.haveChamber = haveChamber;
    }

    public String getDamageCategory() {
        return this.damageCategory;
    }

    public void setDamageCategory(String damageCategory) {
        this.damageCategory = damageCategory;
    }

    public boolean isDamageMakeHole() {
        return this.damageMakeHole;
    }

    public void setDamageMakeHole(boolean damageMakeHole) {
        this.damageMakeHole = damageMakeHole;
    }

    public String getHitFloorSound() {
        return this.hitFloorSound;
    }

    public void setHitFloorSound(String hitFloorSound) {
        this.hitFloorSound = hitFloorSound;
    }

    public boolean isInsertAllBulletsReload() {
        return this.insertAllBulletsReload;
    }

    public void setInsertAllBulletsReload(boolean insertAllBulletsReload) {
        this.insertAllBulletsReload = insertAllBulletsReload;
    }

    public String getFireMode() {
        return this.fireMode;
    }

    public void setFireMode(String fireMode) {
        this.fireMode = fireMode;
    }

    public boolean isSelectFire() {
        return this.fireModePossibilities != null && this.fireModePossibilities.size() > 1;
    }

    public String cycleFireMode() {
        if (this.fireMode == null || this.fireModePossibilities == null || this.fireModePossibilities.size() <= 1) {
            return this.fireMode;
        }
        this.fireMode = this.fireModePossibilities.get((this.fireModePossibilities.indexOf(this.fireMode) + 1) % this.fireModePossibilities.size());
        return this.fireMode;
    }

    public ArrayList<String> getFireModePossibilities() {
        return this.fireModePossibilities;
    }

    public void setFireModePossibilities(ArrayList<String> fireModePossibilities) {
        this.fireModePossibilities = fireModePossibilities;
    }

    public float getCyclicRateMultiplier() {
        return this.cyclicRateMultiplier;
    }

    public void setCyclicRateMultiplier(float value) {
        this.cyclicRateMultiplier = value;
    }

    public int randomizeBullets() {
        if (!this.isRanged() || Rand.NextBool(4)) {
            return this.getCurrentAmmoCount();
        }
        this.setCurrentAmmoCount(Rand.Next(this.getMaxAmmo() - 2, this.getMaxAmmo()));
        if (!StringUtils.isNullOrEmpty(this.getMagazineType())) {
            this.setContainsClip(true);
        }
        if (this.haveChamber()) {
            this.setRoundChambered(true);
        }
        return this.getCurrentAmmoCount();
    }

    @Override
    public boolean canEmitLight() {
        return this.activeLight != null && this.activeLight.canEmitLight() || super.canEmitLight();
    }

    @Override
    public float getLightStrength() {
        return this.activeLight != null ? this.activeLight.getLightStrength() : super.getLightStrength();
    }

    @Override
    public boolean isTorchCone() {
        return this.activeLight != null && this.activeLight.isTorchCone() || super.isTorchCone();
    }

    @Override
    public float getTorchDot() {
        return this.activeLight != null ? this.activeLight.getTorchDot() : super.getTorchDot();
    }

    @Override
    public int getLightDistance() {
        return this.activeLight != null ? this.activeLight.getLightDistance() : super.getLightDistance();
    }

    @Override
    public boolean canBeActivated() {
        return this.activeLight != null ? this.activeLight.canBeActivated() : super.canBeActivated();
    }

    public float getStopPower() {
        return this.getScriptItem().stopPower;
    }

    public boolean isInstantExplosion() {
        return this.explosionTimer <= 0 && this.sensorRange <= 0 && this.getRemoteControlID() == -1 && !this.canBeRemote();
    }

    public void setWeaponSpritesByIndex(ArrayList<String> weaponSpritesByIndex) {
        this.weaponSpritesByIndex = weaponSpritesByIndex;
    }

    public ArrayList<String> getWeaponSpritesByIndex() {
        return this.weaponSpritesByIndex;
    }

    public boolean usesExternalMagazine() {
        return this.getMagazineType() != null;
    }

    public void inheritAmmunition(HandWeapon other) {
        this.setJammed(other.isJammed());
        if (other.haveChamber() && this.haveChamber()) {
            this.setRoundChambered(other.isRoundChambered());
            this.setSpentRoundChambered(other.isSpentRoundChambered());
        }
        if (other.usesExternalMagazine() && this.usesExternalMagazine()) {
            this.setContainsClip(other.isContainsClip());
        }
        this.setCurrentAmmoCount(other.getCurrentAmmoCount());
        this.setFireMode(other.getFireMode());
    }

    public boolean isBareHands() {
        return this.hasTag(ItemTag.BARE_HANDS) || "BareHands".equals(this.getType());
    }

    @Override
    public void render() {
    }

    @Override
    public void setActivated(boolean activated) {
        if (this.activeLight != null) {
            this.activeLight.setActivated(activated);
        }
        super.setActivated(activated);
    }

    @Override
    public void playActivateSound() {
        if (this.activeLight != null) {
            this.playSoundOnPlayer(SoundKey.FLASHLIGHT_ON);
        }
    }

    @Override
    public void playDeactivateSound() {
        if (this.activeLight != null) {
            this.playSoundOnPlayer(SoundKey.FLASHLIGHT_OFF);
        }
    }

    @Override
    public void update() {
        super.update();
        for (int i = 0; i < this.attachmentList.size(); ++i) {
            this.attachmentList.get(i).update();
        }
        if (this.activeLight != null && this.activeLight.isActivated() != this.isActivated()) {
            super.setActivated(this.activeLight.isActivated());
        }
    }

    public boolean canAttackPierceTransparentWall(IsoGameCharacter isoGameCharacter, HandWeapon handWeapon) {
        if (handWeapon == null) {
            return false;
        }
        if (this.isAimedFirearm()) {
            return true;
        }
        WeaponType weaponType = WeaponType.getWeaponType(handWeapon);
        if (weaponType == WeaponType.SPEAR && !handWeapon.hasTag(ItemTag.NO_FENCE_STAB)) {
            if (isoGameCharacter instanceof IsoPlayer) {
                IsoPlayer isoPlayer = (IsoPlayer)isoGameCharacter;
                isoPlayer.setAttackType(AttackType.SPEAR_STAB);
            }
            return true;
        }
        return weaponType == WeaponType.KNIFE;
    }

    public void randomizeFirearmAsLoot() {
        int ammoBoxChance = 30;
        if (Core.getInstance().getOptionReloadDifficulty() > 1 && !StringUtils.isNullOrEmpty(this.getMagazineType()) && Rand.Next(100) < 90) {
            if (Rand.NextBool(3)) {
                InventoryItem clip = ItemSpawner.spawnItem(this.getMagazineType(), this.container);
                if (Rand.NextBool(5)) {
                    clip.setCurrentAmmoCount(Rand.Next(1, clip.getMaxAmmo()));
                }
                if (!Rand.NextBool(5)) {
                    clip.setCurrentAmmoCount(clip.getMaxAmmo());
                }
            } else {
                if (!StringUtils.isNullOrWhitespace(this.getMagazineType())) {
                    this.setContainsClip(true);
                }
                if (Rand.NextBool(6)) {
                    this.setCurrentAmmoCount(Rand.Next(1, this.getMaxAmmo()));
                } else {
                    ammoBoxChance = Rand.Next(60, 100);
                }
            }
            if (this.haveChamber() & Rand.NextBool(5)) {
                this.setRoundChambered(true);
            }
        }
        if (Core.getInstance().getOptionReloadDifficulty() == 1 || StringUtils.isNullOrEmpty(this.getMagazineType()) && Rand.Next(100) < 30) {
            this.setCurrentAmmoCount(Rand.Next(1, this.getMaxAmmo()));
            if (this.haveChamber()) {
                this.setRoundChambered(true);
            }
        }
        if (this.getContainer() != null && !StringUtils.isNullOrEmpty(this.getAmmoBox())) {
            if (Rand.Next(100) < ammoBoxChance) {
                ItemSpawner.spawnItem(this.getAmmoBox(), this.getContainer());
            } else if (Rand.Next(100) < 50) {
                ItemSpawner.spawnItems(this.getAmmoType().getItemKey(), Rand.Next(1, 5), this.getContainer());
            }
        }
    }

    public void setAttackTargetSquare(IsoGridSquare isoGridSquare) {
        this.attackTargetSquare = isoGridSquare;
    }

    public IsoGridSquare getAttackTargetSquare(Vector3 attackPosition) {
        if (attackPosition != null) {
            attackPosition.set(this.attackTargetSquare.x, this.attackTargetSquare.y, this.attackTargetSquare.z);
        }
        return this.attackTargetSquare;
    }

    public boolean isMelee() {
        return this.isMelee;
    }

    public boolean isExplosive() {
        return this.isExplosive;
    }

    public float getStaggerBackTimeMod(IsoGameCharacter wielder, IsoGameCharacter target) {
        boolean isSmallBladeTargetAZombie = target.isZombie() && this.weaponCategories.contains(WeaponCategory.SMALL_BLADE);
        return isSmallBladeTargetAZombie ? 0.0f : this.pushBackMod * this.getKnockbackMod(wielder) * wielder.getShovingMod();
    }

    @Override
    public void setScriptItem(Item scriptItem) {
        super.setScriptItem(scriptItem);
        this.isMelee = !this.weaponCategories.isEmpty();
        this.isExplosive = "Explosives".equals(this.scriptItem.displayCategory);
    }

    public boolean needToBeClosedOnceReload() {
        return this.getScriptItem().needToBeClosedOnceReload;
    }
}

