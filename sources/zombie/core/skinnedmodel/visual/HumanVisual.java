/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.visual;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.HairOutfitDefinitions;
import zombie.characters.SurvivorDesc;
import zombie.characters.WornItems.BodyLocation;
import zombie.characters.WornItems.BodyLocationGroup;
import zombie.characters.WornItems.BodyLocations;
import zombie.core.ImmutableColor;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.CharacterMask;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.population.BeardStyles;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.ClothingItemReference;
import zombie.core.skinnedmodel.population.DefaultClothing;
import zombie.core.skinnedmodel.population.HairStyles;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.core.skinnedmodel.population.PopTemplateManager;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.iso.IsoWorld;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ModelScript;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public class HumanVisual
extends BaseVisual {
    private final IHumanVisual owner;
    private ImmutableColor skinColor = ImmutableColor.white;
    private int skinTexture = -1;
    protected String skinTextureName;
    public int zombieRotStage = -1;
    private ImmutableColor hairColor;
    private ImmutableColor beardColor;
    private ImmutableColor naturalHairColor;
    private ImmutableColor naturalBeardColor;
    private String hairModel;
    private String beardModel;
    private int bodyHair = -1;
    private final byte[] blood = new byte[BloodBodyPartType.MAX.index()];
    private final byte[] dirt = new byte[BloodBodyPartType.MAX.index()];
    private final byte[] holes = new byte[BloodBodyPartType.MAX.index()];
    private final ItemVisuals bodyVisuals = new ItemVisuals();
    private Outfit outfit;
    private String nonAttachedHair;
    private Model forceModel;
    private String forceModelScript;
    private static final List<ItemBodyLocation> itemVisualLocations = new ArrayList<ItemBodyLocation>();
    private static final int LASTSTAND_VERSION1 = 1;
    private static final int LASTSTAND_VERSION = 1;

    public HumanVisual(IHumanVisual owner) {
        this.owner = owner;
        Arrays.fill(this.blood, (byte)0);
        Arrays.fill(this.dirt, (byte)0);
        Arrays.fill(this.holes, (byte)0);
    }

    public boolean isFemale() {
        return this.owner.isFemale();
    }

    public boolean isZombie() {
        return this.owner.isZombie();
    }

    public boolean isSkeleton() {
        return this.owner.isSkeleton();
    }

    public void setSkinColor(ImmutableColor color) {
        this.skinColor = color;
    }

    public ImmutableColor getSkinColor() {
        if (this.skinColor == null) {
            this.skinColor = new ImmutableColor(SurvivorDesc.getRandomSkinColor());
        }
        return this.skinColor;
    }

    public void setBodyHairIndex(int index) {
        this.bodyHair = index;
    }

    public int getBodyHairIndex() {
        return this.bodyHair;
    }

    public void setSkinTextureIndex(int index) {
        this.skinTexture = index;
    }

    public int getSkinTextureIndex() {
        return this.skinTexture;
    }

    public void setSkinTextureName(String textureName) {
        this.skinTextureName = textureName;
    }

    public float lerp(float start, float end, float delta) {
        if (delta < 0.0f) {
            delta = 0.0f;
        }
        if (delta >= 1.0f) {
            delta = 1.0f;
        }
        float amount = end - start;
        float result = amount * delta;
        return start + result;
    }

    public int pickRandomZombieRotStage() {
        int daysSurvived = Math.max((int)IsoWorld.instance.getWorldAgeDays(), 0);
        float firstDayThreshold = 20.0f;
        float secondDayThreshold = 90.0f;
        float stage1ChanceFirstDay = 100.0f;
        float stage1ChanceSecondDay = 20.0f;
        float stage2ChanceFirstDay = 10.0f;
        float stage2ChanceSecondDay = 30.0f;
        if (daysSurvived >= 180) {
            stage1ChanceSecondDay = 0.0f;
            stage2ChanceSecondDay = 10.0f;
        }
        float daySinceThreshold = (float)daysSurvived - 20.0f;
        float delta = daySinceThreshold / 70.0f;
        float chanceStage1 = this.lerp(100.0f, stage1ChanceSecondDay, delta);
        float chanceStage2 = this.lerp(10.0f, stage2ChanceSecondDay, delta);
        float roll = OutfitRNG.Next(100);
        if (roll < chanceStage1) {
            return 1;
        }
        if (roll < chanceStage2 + chanceStage1) {
            return 2;
        }
        return 3;
    }

    public String getSkinTexture() {
        ArrayList<String> textures;
        if (this.skinTextureName != null) {
            return this.skinTextureName;
        }
        String bodyHair = "";
        ArrayList<String> arrayList = textures = this.owner.isFemale() ? PopTemplateManager.instance.femaleSkins : PopTemplateManager.instance.maleSkins;
        if (this.owner.isZombie() && this.owner.isSkeleton()) {
            textures = this.owner.isFemale() ? PopTemplateManager.instance.skeletonFemaleSkinsZombie : PopTemplateManager.instance.skeletonMaleSkinsZombie;
        } else if (this.owner.isZombie()) {
            if (this.zombieRotStage < 1 || this.zombieRotStage > 3) {
                this.zombieRotStage = this.pickRandomZombieRotStage();
            }
            switch (this.zombieRotStage) {
                case 1: {
                    textures = this.owner.isFemale() ? PopTemplateManager.instance.femaleSkinsZombie1 : PopTemplateManager.instance.maleSkinsZombie1;
                    break;
                }
                case 2: {
                    textures = this.owner.isFemale() ? PopTemplateManager.instance.femaleSkinsZombie2 : PopTemplateManager.instance.maleSkinsZombie2;
                    break;
                }
                case 3: {
                    textures = this.owner.isFemale() ? PopTemplateManager.instance.femaleSkinsZombie3 : PopTemplateManager.instance.maleSkinsZombie3;
                }
            }
        } else if (!this.owner.isFemale()) {
            String string = bodyHair = !this.owner.isZombie() && this.bodyHair >= 0 ? "a" : "";
        }
        if (this.skinTexture == textures.size()) {
            --this.skinTexture;
        } else if (this.skinTexture < 0 || this.skinTexture > textures.size()) {
            this.skinTexture = OutfitRNG.Next(textures.size());
        }
        return textures.get(this.skinTexture) + bodyHair;
    }

    public void setHairColor(ImmutableColor color) {
        if (this.beardColor == null) {
            this.beardColor = new ImmutableColor(this.hairColor);
        }
        this.hairColor = color;
    }

    public ImmutableColor getHairColor() {
        if (this.hairColor == null) {
            this.hairColor = HairOutfitDefinitions.instance.getRandomHaircutColor(this.outfit != null ? this.outfit.name : null);
        }
        return this.hairColor;
    }

    public void setBeardColor(ImmutableColor color) {
        this.beardColor = color;
    }

    public ImmutableColor getBeardColor() {
        if (this.beardColor == null) {
            this.beardColor = this.getHairColor();
        }
        return this.beardColor;
    }

    public void setNaturalHairColor(ImmutableColor color) {
        this.naturalHairColor = color;
    }

    public ImmutableColor getNaturalHairColor() {
        if (this.naturalHairColor == null) {
            this.naturalHairColor = this.getHairColor();
        }
        return this.naturalHairColor;
    }

    public void setNaturalBeardColor(ImmutableColor color) {
        this.naturalBeardColor = color;
    }

    public ImmutableColor getNaturalBeardColor() {
        if (this.naturalBeardColor == null) {
            this.naturalBeardColor = this.getNaturalHairColor();
        }
        return this.naturalBeardColor;
    }

    public void setHairModel(String model) {
        this.hairModel = model;
    }

    public String getHairModel() {
        if (this.owner.isFemale()) {
            if (HairStyles.instance.FindFemaleStyle(this.hairModel) == null) {
                this.hairModel = HairStyles.instance.getRandomFemaleStyle(this.outfit != null ? this.outfit.name : null);
            }
        } else if (HairStyles.instance.FindMaleStyle(this.hairModel) == null) {
            this.hairModel = HairStyles.instance.getRandomMaleStyle(this.outfit != null ? this.outfit.name : null);
        }
        return this.hairModel;
    }

    public void setBeardModel(String model) {
        this.beardModel = model;
    }

    public String getBeardModel() {
        if (this.owner.isFemale()) {
            this.beardModel = null;
        } else if (BeardStyles.instance.FindStyle(this.beardModel) == null) {
            this.beardModel = BeardStyles.instance.getRandomStyle(this.outfit != null ? this.outfit.name : null);
        }
        return this.beardModel;
    }

    public void setBlood(BloodBodyPartType bodyPartType, float amount) {
        amount = Math.max(0.0f, Math.min(1.0f, amount));
        this.blood[bodyPartType.index()] = (byte)(amount * 255.0f);
    }

    public float getBlood(BloodBodyPartType bodyPartType) {
        return (float)(this.blood[bodyPartType.index()] & 0xFF) / 255.0f;
    }

    public void setDirt(BloodBodyPartType bodyPartType, float amount) {
        amount = Math.max(0.0f, Math.min(1.0f, amount));
        this.dirt[bodyPartType.index()] = (byte)(amount * 255.0f);
    }

    public float getDirt(BloodBodyPartType bodyPartType) {
        return (float)(this.dirt[bodyPartType.index()] & 0xFF) / 255.0f;
    }

    public void setHole(BloodBodyPartType bodyPartType) {
        this.holes[bodyPartType.index()] = -1;
    }

    public float getHole(BloodBodyPartType bodyPartType) {
        return (float)(this.holes[bodyPartType.index()] & 0xFF) / 255.0f;
    }

    public void removeBlood() {
        Arrays.fill(this.blood, (byte)0);
    }

    public void removeDirt() {
        Arrays.fill(this.dirt, (byte)0);
    }

    public void randomBlood() {
        for (int i = 0; i < BloodBodyPartType.MAX.index(); ++i) {
            this.setBlood(BloodBodyPartType.FromIndex(i), OutfitRNG.Next(0.0f, 1.0f));
        }
    }

    public void randomDirt() {
        for (int i = 0; i < BloodBodyPartType.MAX.index(); ++i) {
            this.setDirt(BloodBodyPartType.FromIndex(i), OutfitRNG.Next(0.0f, 1.0f));
        }
    }

    public float getTotalBlood() {
        float total = 0.0f;
        for (int i = 0; i < this.blood.length; ++i) {
            total += (float)(this.blood[i] & 0xFF) / 255.0f;
        }
        return total;
    }

    @Override
    public void clear() {
        this.skinColor = ImmutableColor.white;
        this.skinTexture = -1;
        this.skinTextureName = null;
        this.zombieRotStage = -1;
        this.hairColor = null;
        this.beardColor = null;
        this.naturalHairColor = null;
        this.naturalBeardColor = null;
        this.hairModel = null;
        this.nonAttachedHair = null;
        this.beardModel = null;
        this.bodyHair = -1;
        Arrays.fill(this.blood, (byte)0);
        Arrays.fill(this.dirt, (byte)0);
        Arrays.fill(this.holes, (byte)0);
        this.bodyVisuals.clear();
        this.forceModel = null;
        this.forceModelScript = null;
    }

    @Override
    public void copyFrom(BaseVisual baseVisual) {
        if (baseVisual == null) {
            this.clear();
            return;
        }
        if (!(baseVisual instanceof HumanVisual)) {
            throw new IllegalArgumentException("expected HumanVisual, got " + String.valueOf(baseVisual));
        }
        HumanVisual other = (HumanVisual)baseVisual;
        other.getHairColor();
        other.getNaturalHairColor();
        other.getNaturalBeardColor();
        other.getHairModel();
        other.getBeardModel();
        other.getSkinTexture();
        this.skinColor = other.skinColor;
        this.skinTexture = other.skinTexture;
        this.skinTextureName = other.skinTextureName;
        this.zombieRotStage = other.zombieRotStage;
        this.hairColor = other.hairColor;
        this.beardColor = other.beardColor;
        this.naturalHairColor = other.naturalHairColor;
        this.naturalBeardColor = other.naturalBeardColor;
        this.hairModel = other.hairModel;
        this.nonAttachedHair = other.nonAttachedHair;
        this.beardModel = other.beardModel;
        this.bodyHair = other.bodyHair;
        this.outfit = other.outfit;
        System.arraycopy(other.blood, 0, this.blood, 0, this.blood.length);
        System.arraycopy(other.dirt, 0, this.dirt, 0, this.dirt.length);
        System.arraycopy(other.holes, 0, this.holes, 0, this.holes.length);
        this.bodyVisuals.clear();
        PZArrayUtil.addAll(this.bodyVisuals, other.bodyVisuals);
        this.forceModel = other.forceModel;
        this.forceModelScript = other.forceModelScript;
    }

    @Override
    public void save(ByteBuffer output) throws IOException {
        int i;
        byte flags1 = 0;
        if (this.hairColor != null) {
            flags1 = (byte)(flags1 | 4);
        }
        if (this.beardColor != null) {
            flags1 = (byte)(flags1 | 2);
        }
        if (this.skinColor != null) {
            flags1 = (byte)(flags1 | 8);
        }
        if (this.beardModel != null) {
            flags1 = (byte)(flags1 | 0x10);
        }
        if (this.hairModel != null) {
            flags1 = (byte)(flags1 | 0x20);
        }
        if (this.skinTextureName != null) {
            flags1 = (byte)(flags1 | 0x40);
        }
        output.put(flags1);
        if (this.hairColor != null) {
            output.put(this.hairColor.getRedByte());
            output.put(this.hairColor.getGreenByte());
            output.put(this.hairColor.getBlueByte());
        }
        if (this.beardColor != null) {
            output.put(this.beardColor.getRedByte());
            output.put(this.beardColor.getGreenByte());
            output.put(this.beardColor.getBlueByte());
        }
        if (this.skinColor != null) {
            output.put(this.skinColor.getRedByte());
            output.put(this.skinColor.getGreenByte());
            output.put(this.skinColor.getBlueByte());
        }
        output.put((byte)this.bodyHair);
        output.put((byte)this.skinTexture);
        output.put((byte)this.zombieRotStage);
        if (this.skinTextureName != null) {
            GameWindow.WriteString(output, this.skinTextureName);
        }
        if (this.beardModel != null) {
            GameWindow.WriteString(output, this.beardModel);
        }
        if (this.hairModel != null) {
            GameWindow.WriteString(output, this.hairModel);
        }
        output.put((byte)this.blood.length);
        for (i = 0; i < this.blood.length; ++i) {
            output.put(this.blood[i]);
        }
        output.put((byte)this.dirt.length);
        for (i = 0; i < this.dirt.length; ++i) {
            output.put(this.dirt[i]);
        }
        output.put((byte)this.holes.length);
        for (i = 0; i < this.holes.length; ++i) {
            output.put(this.holes[i]);
        }
        output.put((byte)this.bodyVisuals.size());
        for (i = 0; i < this.bodyVisuals.size(); ++i) {
            ItemVisual itemVisual = (ItemVisual)this.bodyVisuals.get(i);
            itemVisual.save(output);
        }
        GameWindow.WriteString(output, this.getNonAttachedHair());
        byte flags2 = 0;
        if (this.naturalHairColor != null) {
            flags2 = (byte)(flags2 | 4);
        }
        if (this.naturalBeardColor != null) {
            flags2 = (byte)(flags2 | 2);
        }
        output.put(flags2);
        if (this.naturalHairColor != null) {
            output.put(this.naturalHairColor.getRedByte());
            output.put(this.naturalHairColor.getGreenByte());
            output.put(this.naturalHairColor.getBlueByte());
        }
        if (this.naturalBeardColor != null) {
            output.put(this.naturalBeardColor.getRedByte());
            output.put(this.naturalBeardColor.getGreenByte());
            output.put(this.naturalBeardColor.getBlueByte());
        }
    }

    @Override
    public void load(ByteBuffer input, int worldversion) throws IOException {
        int b;
        int g;
        int r;
        byte amount;
        int i;
        int b2;
        int g2;
        int r2;
        this.clear();
        int flags1 = input.get() & 0xFF;
        if ((flags1 & 4) != 0) {
            r2 = input.get() & 0xFF;
            g2 = input.get() & 0xFF;
            b2 = input.get() & 0xFF;
            this.hairColor = new ImmutableColor(r2, g2, b2);
        }
        if ((flags1 & 2) != 0) {
            r2 = input.get() & 0xFF;
            g2 = input.get() & 0xFF;
            b2 = input.get() & 0xFF;
            this.beardColor = new ImmutableColor(r2, g2, b2);
        }
        if ((flags1 & 8) != 0) {
            r2 = input.get() & 0xFF;
            g2 = input.get() & 0xFF;
            b2 = input.get() & 0xFF;
            this.skinColor = new ImmutableColor(r2, g2, b2);
        }
        this.bodyHair = input.get();
        this.skinTexture = input.get();
        this.zombieRotStage = input.get();
        if ((flags1 & 0x40) != 0) {
            this.skinTextureName = GameWindow.ReadString(input);
        }
        if ((flags1 & 0x10) != 0) {
            this.beardModel = GameWindow.ReadString(input);
        }
        if ((flags1 & 0x20) != 0) {
            this.hairModel = GameWindow.ReadString(input);
        }
        int count = input.get();
        for (i = 0; i < count; ++i) {
            amount = input.get();
            if (i >= this.blood.length) continue;
            this.blood[i] = amount;
        }
        count = input.get();
        for (i = 0; i < count; ++i) {
            amount = input.get();
            if (i >= this.dirt.length) continue;
            this.dirt[i] = amount;
        }
        count = input.get();
        for (i = 0; i < count; ++i) {
            amount = input.get();
            if (i >= this.holes.length) continue;
            this.holes[i] = amount;
        }
        count = input.get();
        for (i = 0; i < count; ++i) {
            ItemVisual itemVisual = new ItemVisual();
            itemVisual.load(input, worldversion);
            this.bodyVisuals.add(itemVisual);
        }
        this.setNonAttachedHair(GameWindow.ReadString(input));
        int flags2 = input.get() & 0xFF;
        if ((flags2 & 4) != 0) {
            r = input.get() & 0xFF;
            g = input.get() & 0xFF;
            b = input.get() & 0xFF;
            this.naturalHairColor = new ImmutableColor(r, g, b);
        }
        if ((flags2 & 2) != 0) {
            r = input.get() & 0xFF;
            g = input.get() & 0xFF;
            b = input.get() & 0xFF;
            this.naturalBeardColor = new ImmutableColor(r, g, b);
        }
    }

    @Override
    public Model getModel() {
        if (this.forceModel != null) {
            return this.forceModel;
        }
        if (this.isSkeleton()) {
            return this.isFemale() ? ModelManager.instance.skeletonFemaleModel : ModelManager.instance.skeletonMaleModel;
        }
        return this.isFemale() ? ModelManager.instance.femaleModel : ModelManager.instance.maleModel;
    }

    @Override
    public ModelScript getModelScript() {
        if (this.forceModelScript != null) {
            return ScriptManager.instance.getModelScript(this.forceModelScript);
        }
        return ScriptManager.instance.getModelScript(this.isFemale() ? "FemaleBody" : "MaleBody");
    }

    public static CharacterMask GetMask(ItemVisuals itemVisuals) {
        CharacterMask mask = new CharacterMask();
        for (int i = itemVisuals.size() - 1; i >= 0; --i) {
            ((ItemVisual)itemVisuals.get(i)).getClothingItemCombinedMask(mask);
        }
        return mask;
    }

    public void synchWithOutfit(Outfit outfit) {
        if (outfit == null) {
            return;
        }
        this.beardColor = this.hairColor = outfit.randomData.hairColor;
        this.hairModel = this.owner.isFemale() ? outfit.randomData.femaleHairName : outfit.randomData.maleHairName;
        this.beardModel = this.owner.isFemale() ? null : outfit.randomData.beardName;
        this.getSkinTexture();
    }

    @Override
    public void dressInNamedOutfit(String outfitName, ItemVisuals itemVisuals) {
        this.dressInNamedOutfit(outfitName, itemVisuals, true);
    }

    public void dressInNamedOutfit(String outfitName, ItemVisuals itemVisuals, boolean clear) {
        Outfit outfitSource;
        if (clear) {
            itemVisuals.clear();
        }
        if (StringUtils.isNullOrWhitespace(outfitName)) {
            return;
        }
        Outfit outfit = outfitSource = this.owner.isFemale() ? OutfitManager.instance.FindFemaleOutfit(outfitName) : OutfitManager.instance.FindMaleOutfit(outfitName);
        if (outfitSource == null) {
            return;
        }
        Outfit outfit2 = outfitSource.clone();
        outfit2.Randomize();
        this.dressInOutfit(outfit2, itemVisuals);
    }

    public void dressInClothingItem(String itemGUID, ItemVisuals itemVisuals) {
        this.dressInClothingItem(itemGUID, itemVisuals, true);
    }

    public void dressInClothingItem(String itemGUID, ItemVisuals itemVisuals, boolean clearCurrentVisuals) {
        ClothingItem item;
        if (clearCurrentVisuals) {
            this.clear();
            itemVisuals.clear();
        }
        if ((item = OutfitManager.instance.getClothingItem(itemGUID)) == null) {
            return;
        }
        Outfit outfit = new Outfit();
        ClothingItemReference itemRef = new ClothingItemReference();
        itemRef.itemGuid = itemGUID;
        outfit.items.add(itemRef);
        outfit.pants = false;
        outfit.top = false;
        outfit.Randomize();
        this.dressInOutfit(outfit, itemVisuals);
    }

    private void dressInOutfit(Outfit outfit, ItemVisuals itemVisuals) {
        String clothingName;
        this.setOutfit(outfit);
        this.getItemVisualLocations(itemVisuals, itemVisualLocations);
        if (outfit.pants) {
            clothingName = outfit.allowPantsHue ? DefaultClothing.instance.pickPantsHue() : (outfit.allowPantsTint ? DefaultClothing.instance.pickPantsTint() : DefaultClothing.instance.pickPantsTexture());
            this.addClothingItem(itemVisuals, itemVisualLocations, clothingName, null);
        }
        if (outfit.top && outfit.randomData.hasTop) {
            clothingName = outfit.randomData.hasTshirt ? (outfit.randomData.hasTshirtDecal && outfit.GetMask().isTorsoVisible() && outfit.allowTshirtDecal ? (outfit.allowTopTint ? DefaultClothing.instance.pickTShirtDecalTint() : DefaultClothing.instance.pickTShirtDecalTexture()) : (outfit.allowTopTint ? DefaultClothing.instance.pickTShirtTint() : DefaultClothing.instance.pickTShirtTexture())) : (outfit.allowTopTint ? DefaultClothing.instance.pickVestTint() : DefaultClothing.instance.pickVestTexture());
            this.addClothingItem(itemVisuals, itemVisualLocations, clothingName, null);
        }
        for (int i = 0; i < outfit.items.size(); ++i) {
            ItemVisual visual;
            ClothingItemReference itemRef = outfit.items.get(i);
            ClothingItem clothingItem = itemRef.getClothingItem();
            if (clothingItem == null || !clothingItem.isReady() || (visual = this.addClothingItem(itemVisuals, itemVisualLocations, clothingItem.mame, itemRef)) == null || clothingItem.getSpawnWith().isEmpty()) continue;
            for (int j = 0; j < clothingItem.getSpawnWith().size(); ++j) {
                ItemVisual spawnWithVisual;
                String spawnString;
                ClothingItem spawnWithItem;
                String modID = outfit.modId;
                if (modID == null) {
                    modID = "game";
                }
                if ((spawnWithItem = OutfitManager.instance.getClothingItem(spawnString = modID + "-" + clothingItem.getSpawnWith().get(j))) == null || spawnWithItem == null || !spawnWithItem.isReady() || (spawnWithVisual = this.addClothingItem(itemVisuals, itemVisualLocations, spawnWithItem.mame, null)) == null) continue;
                spawnWithVisual.copyVisualFrom(visual);
            }
        }
        outfit.pants = false;
        outfit.top = false;
        outfit.randomData.topTexture = null;
        outfit.randomData.pantsTexture = null;
    }

    public ItemVisuals getBodyVisuals() {
        return this.bodyVisuals;
    }

    public ItemVisual addBodyVisual(String clothingItemName) {
        return this.addBodyVisualFromClothingItemName(clothingItemName);
    }

    public ItemVisual addBodyVisualFromItemType(String itemType) {
        Item scriptItem = ScriptManager.instance.getItem(itemType);
        if (scriptItem == null || StringUtils.isNullOrWhitespace(scriptItem.getClothingItem())) {
            return null;
        }
        return this.addBodyVisualFromClothingItemName(scriptItem.getClothingItem());
    }

    public ItemVisual addBodyVisualFromClothingItemName(String clothingItemName) {
        if (StringUtils.isNullOrWhitespace(clothingItemName)) {
            return null;
        }
        Item scriptItem = ScriptManager.instance.getItemForClothingItem(clothingItemName);
        if (scriptItem == null) {
            return null;
        }
        ClothingItem clothingItem = scriptItem.getClothingItemAsset();
        if (clothingItem == null) {
            return null;
        }
        for (int j = 0; j < this.bodyVisuals.size(); ++j) {
            if (!((ItemVisual)this.bodyVisuals.get(j)).getClothingItemName().equals(clothingItemName)) continue;
            return null;
        }
        ClothingItemReference itemRef = new ClothingItemReference();
        itemRef.itemGuid = clothingItem.guid;
        itemRef.randomize();
        ItemVisual itemVisual = new ItemVisual();
        itemVisual.setItemType(scriptItem.getFullName());
        itemVisual.synchWithOutfit(itemRef);
        this.bodyVisuals.add(itemVisual);
        return itemVisual;
    }

    public ItemVisual removeBodyVisualFromItemType(String itemType) {
        for (int i = 0; i < this.bodyVisuals.size(); ++i) {
            ItemVisual itemVisual = (ItemVisual)this.bodyVisuals.get(i);
            if (!itemVisual.getItemType().equals(itemType)) continue;
            this.bodyVisuals.remove(i);
            return itemVisual;
        }
        return null;
    }

    public boolean hasBodyVisualFromItemType(String itemType) {
        for (int i = 0; i < this.bodyVisuals.size(); ++i) {
            ItemVisual itemVisual = (ItemVisual)this.bodyVisuals.get(i);
            if (!itemVisual.getItemType().equals(itemType)) continue;
            return true;
        }
        return false;
    }

    private void getItemVisualLocations(ItemVisuals itemVisuals, List<ItemBodyLocation> itemVisualLocations) {
        itemVisualLocations.clear();
        for (int i = 0; i < itemVisuals.size(); ++i) {
            ItemVisual itemVisual = (ItemVisual)itemVisuals.get(i);
            Item scriptItem = itemVisual.getScriptItem();
            if (scriptItem == null) {
                itemVisualLocations.add(null);
                continue;
            }
            ItemBodyLocation locationId = scriptItem.getBodyLocation();
            if (locationId == null) {
                locationId = scriptItem.canBeEquipped;
            }
            itemVisualLocations.add(locationId);
        }
    }

    public ItemVisual addClothingItem(ItemVisuals itemVisuals, Item scriptItem) {
        if (scriptItem == null) {
            return null;
        }
        ClothingItem clothingItem = scriptItem.getClothingItemAsset();
        if (clothingItem == null) {
            return null;
        }
        if (!clothingItem.isReady()) {
            return null;
        }
        this.getItemVisualLocations(itemVisuals, itemVisualLocations);
        return this.addClothingItem(itemVisuals, itemVisualLocations, clothingItem.mame, null);
    }

    public ItemVisual addClothingItem(ItemVisuals itemVisuals, ClothingItem clothingItem) {
        if (clothingItem == null) {
            return null;
        }
        if (!clothingItem.isReady()) {
            return null;
        }
        this.getItemVisualLocations(itemVisuals, itemVisualLocations);
        return this.addClothingItem(itemVisuals, itemVisualLocations, clothingItem.mame, null);
    }

    private ItemVisual addClothingItem(ItemVisuals itemVisuals, List<ItemBodyLocation> itemVisualLocations, String clothingName, ClothingItemReference itemRef) {
        int index;
        assert (itemVisuals.size() == itemVisualLocations.size());
        if (itemRef != null && !itemRef.randomData.active) {
            return null;
        }
        if (StringUtils.isNullOrWhitespace(clothingName)) {
            return null;
        }
        Item scriptItem = ScriptManager.instance.getItemForClothingItem(clothingName);
        if (scriptItem == null) {
            DebugType.Clothing.warn("Could not find item type for %s", clothingName);
            return null;
        }
        ClothingItem clothingItem = scriptItem.getClothingItemAsset();
        if (clothingItem == null) {
            return null;
        }
        if (!clothingItem.isReady()) {
            return null;
        }
        ItemBodyLocation locationId = scriptItem.getBodyLocation();
        if (locationId == null) {
            locationId = scriptItem.canBeEquipped;
        }
        if (locationId == null) {
            return null;
        }
        if (itemRef == null) {
            itemRef = new ClothingItemReference();
            itemRef.itemGuid = clothingItem.guid;
            itemRef.randomize();
        }
        if (!itemRef.randomData.active) {
            return null;
        }
        BodyLocationGroup bodyLocationGroup = BodyLocations.getGroup("Human");
        BodyLocation location = bodyLocationGroup.getLocation(locationId);
        if (location == null) {
            DebugLog.General.error("The game can't found location '" + String.valueOf(locationId) + "' for the item '" + scriptItem.name + "'");
            return null;
        }
        if (!location.isMultiItem() && (index = itemVisualLocations.indexOf(locationId)) != -1) {
            itemVisuals.remove(index);
            itemVisualLocations.remove(index);
        }
        for (int i = 0; i < itemVisuals.size(); ++i) {
            if (!bodyLocationGroup.isExclusive(locationId, itemVisualLocations.get(i))) continue;
            itemVisuals.remove(i);
            itemVisualLocations.remove(i);
            --i;
        }
        assert (itemVisuals.size() == itemVisualLocations.size());
        int locationIndex = bodyLocationGroup.indexOf(locationId);
        int insertAt = itemVisuals.size();
        for (int i = 0; i < itemVisuals.size(); ++i) {
            if (bodyLocationGroup.indexOf(itemVisualLocations.get(i)) <= locationIndex) continue;
            insertAt = i;
            break;
        }
        ItemVisual itemVisual = new ItemVisual();
        itemVisual.setItemType(scriptItem.getFullName());
        itemVisual.synchWithOutfit(itemRef);
        itemVisuals.add(insertAt, itemVisual);
        itemVisualLocations.add(insertAt, locationId);
        return itemVisual;
    }

    public Outfit getOutfit() {
        return this.outfit;
    }

    public void setOutfit(Outfit outfit) {
        this.outfit = outfit;
    }

    public String getNonAttachedHair() {
        return this.nonAttachedHair;
    }

    public void setNonAttachedHair(String nonAttachedHair) {
        if (StringUtils.isNullOrWhitespace(nonAttachedHair)) {
            nonAttachedHair = null;
        }
        this.nonAttachedHair = nonAttachedHair;
    }

    public void setForceModel(Model model) {
        this.forceModel = model;
    }

    public void setForceModelScript(String modelScript) {
        this.forceModelScript = modelScript;
    }

    private static StringBuilder toString(ImmutableColor color, StringBuilder sb) {
        sb.append(color.getRedByte() & 0xFF);
        sb.append(",");
        sb.append(color.getGreenByte() & 0xFF);
        sb.append(",");
        sb.append(color.getBlueByte() & 0xFF);
        return sb;
    }

    private static ImmutableColor colorFromString(String str) {
        String[] ss = str.split(",");
        if (ss.length == 3) {
            try {
                int r = Integer.parseInt(ss[0]);
                int g = Integer.parseInt(ss[1]);
                int b = Integer.parseInt(ss[2]);
                return new ImmutableColor((float)r / 255.0f, (float)g / 255.0f, (float)b / 255.0f);
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        return null;
    }

    public String getLastStandString() {
        StringBuilder sb = new StringBuilder();
        sb.append("version=");
        sb.append(1);
        sb.append(";");
        if (this.getHairColor() != null) {
            sb.append("hairColor=");
            HumanVisual.toString(this.getHairColor(), sb);
            sb.append(";");
        }
        if (this.getBeardColor() != null) {
            sb.append("beardColor=");
            HumanVisual.toString(this.getBeardColor(), sb);
            sb.append(";");
        }
        if (this.getNaturalHairColor() != null) {
            sb.append("naturalHairColor=");
            HumanVisual.toString(this.getNaturalHairColor(), sb);
            sb.append(";");
        }
        if (this.getNaturalBeardColor() != null) {
            sb.append("naturalBeardColor=");
            HumanVisual.toString(this.getNaturalBeardColor(), sb);
            sb.append(";");
        }
        if (this.getSkinColor() != null) {
            sb.append("skinColor=");
            HumanVisual.toString(this.getSkinColor(), sb);
            sb.append(";");
        }
        sb.append("bodyHair=");
        sb.append(this.getBodyHairIndex());
        sb.append(";");
        sb.append("skinTexture=");
        sb.append(this.getSkinTextureIndex());
        sb.append(";");
        if (this.getSkinTexture() != null) {
            sb.append("skinTextureName=");
            sb.append(this.getSkinTexture());
            sb.append(";");
        }
        if (this.getHairModel() != null) {
            sb.append("hairModel=");
            sb.append(this.getHairModel());
            sb.append(";");
        }
        if (this.getBeardModel() != null) {
            sb.append("beardModel=");
            sb.append(this.getBeardModel());
            sb.append(";");
        }
        return sb.toString();
    }

    public boolean loadLastStandString(String saveStr) {
        if (StringUtils.isNullOrWhitespace(saveStr = saveStr.trim()) || !saveStr.startsWith("version=")) {
            return false;
        }
        String[] ss = saveStr.split(";");
        block30: for (int i = 0; i < ss.length; ++i) {
            int p = ss[i].indexOf(61);
            if (p == -1) continue;
            String key = ss[i].substring(0, p).trim();
            String value = ss[i].substring(p + 1).trim();
            switch (key) {
                case "version": {
                    int version = Integer.parseInt(value);
                    if (version >= 1 && version <= 1) continue block30;
                    return false;
                }
                case "beardColor": {
                    ImmutableColor color = HumanVisual.colorFromString(value);
                    if (color == null) continue block30;
                    this.setBeardColor(color);
                    continue block30;
                }
                case "naturalBeardColor": {
                    ImmutableColor color = HumanVisual.colorFromString(value);
                    if (color == null) continue block30;
                    this.setNaturalBeardColor(color);
                    continue block30;
                }
                case "beardModel": {
                    this.setBeardModel(value);
                    continue block30;
                }
                case "bodyHair": {
                    try {
                        this.setBodyHairIndex(Integer.parseInt(value));
                    }
                    catch (NumberFormatException color) {}
                    continue block30;
                }
                case "hairColor": {
                    ImmutableColor color = HumanVisual.colorFromString(value);
                    if (color == null) continue block30;
                    this.setHairColor(color);
                    continue block30;
                }
                case "naturalHairColor": {
                    ImmutableColor color = HumanVisual.colorFromString(value);
                    if (color == null) continue block30;
                    this.setNaturalHairColor(color);
                    continue block30;
                }
                case "hairModel": {
                    this.setHairModel(value);
                    continue block30;
                }
                case "skinColor": {
                    ImmutableColor color = HumanVisual.colorFromString(value);
                    if (color == null) continue block30;
                    this.setSkinColor(color);
                    continue block30;
                }
                case "skinTexture": {
                    try {
                        this.setSkinTextureIndex(Integer.parseInt(value));
                    }
                    catch (NumberFormatException numberFormatException) {}
                    continue block30;
                }
                case "skinTextureName": {
                    this.setSkinTextureName(value);
                }
            }
        }
        return true;
    }
}

