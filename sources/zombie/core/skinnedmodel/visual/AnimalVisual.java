/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.visual;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.IsoAnimal;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.skinnedmodel.visual.IAnimalVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoDeadBody;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ModelScript;
import zombie.util.StringUtils;
import zombie.util.Type;

@UsedFromLua
public class AnimalVisual
extends BaseVisual {
    private final IAnimalVisual owner;
    private String skinTextureName;
    public int animalRotStage = -1;

    public AnimalVisual(IAnimalVisual owner) {
        this.owner = owner;
    }

    @Override
    public void save(ByteBuffer output) throws IOException {
        GameWindow.WriteString(output, this.skinTextureName);
        output.put((byte)this.animalRotStage);
    }

    @Override
    public void load(ByteBuffer input, int worldVersion) throws IOException {
        this.skinTextureName = GameWindow.ReadString(input);
        this.animalRotStage = input.get();
    }

    @Override
    public Model getModel() {
        IAnimalVisual corpse;
        IsoAnimal animal = this.getIsoAnimal();
        if (animal != null) {
            return this.getModelTest(animal);
        }
        if (this.isSkeleton() || this.animalRotStage > 1) {
            AnimalDefinitions adef = AnimalDefinitions.getDef(this.owner.getAnimalType());
            IAnimalVisual iAnimalVisual = this.owner;
            if (iAnimalVisual instanceof IsoDeadBody) {
                corpse = (IsoDeadBody)iAnimalVisual;
                if (adef.bodyModelSkelNoHead != null && ((IsoObject)((Object)corpse)).getModData() != null && ((KahluaTableImpl)((IsoObject)((Object)corpse)).getModData()).rawgetBool("headless")) {
                    this.skinTextureName = adef.textureSkeleton;
                    return adef.bodyModelSkelNoHead;
                }
            }
            if (adef.bodyModelSkel != null) {
                this.skinTextureName = adef.textureSkeleton;
                return adef.bodyModelSkel;
            }
        }
        if ((corpse = this.owner) instanceof IsoDeadBody) {
            IsoDeadBody corpse2 = (IsoDeadBody)corpse;
            AnimalDefinitions adef = AnimalDefinitions.getDef(this.owner.getAnimalType());
            if (this.animalRotStage == 1 && !StringUtils.isNullOrEmpty(corpse2.rottenTexture)) {
                this.skinTextureName = corpse2.rottenTexture;
            }
            if (adef.bodyModelFleece != null && ((KahluaTableImpl)corpse2.getModData()).rawgetBool("shouldBeBodyFleece")) {
                return adef.bodyModelFleece;
            }
            if (!StringUtils.isNullOrEmpty(adef.textureSkinned) && ((KahluaTableImpl)corpse2.getModData()).rawgetBool("skinned")) {
                this.skinTextureName = adef.textureSkinned;
            }
            if (adef.bodyModelHeadless != null && corpse2.getModData() != null && ((KahluaTableImpl)corpse2.getModData()).rawgetBool("headless")) {
                return adef.bodyModelHeadless;
            }
        }
        return AnimalDefinitions.getDef((String)this.owner.getAnimalType()).bodyModel;
    }

    public Model getModelTest(IsoAnimal animal) {
        if (animal.shouldBeSkeleton()) {
            if (animal.adef.bodyModelSkelNoHead != null && animal.getModData() != null && ((KahluaTableImpl)animal.getModData()).rawgetBool("headless")) {
                this.skinTextureName = AnimalDefinitions.getDef((String)this.owner.getAnimalType()).textureSkeleton;
                return AnimalDefinitions.getDef((String)this.owner.getAnimalType()).bodyModelSkelNoHead;
            }
            this.skinTextureName = AnimalDefinitions.getDef((String)this.owner.getAnimalType()).textureSkeleton;
            return AnimalDefinitions.getDef((String)this.owner.getAnimalType()).bodyModelSkel;
        }
        if (!StringUtils.isNullOrEmpty(AnimalDefinitions.getDef((String)this.owner.getAnimalType()).textureSkinned) && ((KahluaTableImpl)animal.getModData()).rawgetBool("skinned")) {
            this.skinTextureName = AnimalDefinitions.getDef((String)this.owner.getAnimalType()).textureSkinned;
        }
        if (animal.adef.bodyModelHeadless != null && animal.getModData() != null && ((KahluaTableImpl)animal.getModData()).rawgetBool("headless")) {
            return animal.adef.bodyModelHeadless;
        }
        if (StringUtils.isNullOrEmpty(animal.getBreed().woolType)) {
            return animal.adef.bodyModel;
        }
        if (animal.getData().getWoolQuantity() >= animal.getData().getMaxWool() / 2.0f && animal.adef.bodyModelFleece != null) {
            return animal.adef.bodyModelFleece;
        }
        if (((KahluaTableImpl)animal.getModData()).rawgetBool("shouldBeBodyFleece") && animal.adef.bodyModelFleece != null) {
            return animal.adef.bodyModelFleece;
        }
        return animal.adef.bodyModel;
    }

    @Override
    public ModelScript getModelScript() {
        IsoAnimal animal = this.getIsoAnimal();
        if (animal == null) {
            AnimalDefinitions adef = AnimalDefinitions.getDef(this.owner.getAnimalType());
            if (this.isSkeleton() && adef.bodyModelSkel != null) {
                this.skinTextureName = adef.textureSkeleton;
                return ScriptManager.instance.getModelScript(adef.bodyModelSkelStr);
            }
            IAnimalVisual iAnimalVisual = this.owner;
            if (iAnimalVisual instanceof IsoDeadBody) {
                IsoDeadBody corpse = (IsoDeadBody)iAnimalVisual;
                if (!StringUtils.isNullOrEmpty(adef.textureSkinned) && ((KahluaTableImpl)corpse.getModData()).rawgetBool("skinned")) {
                    this.skinTextureName = adef.textureSkinned;
                }
                if (!StringUtils.isNullOrEmpty(adef.bodyModelHeadlessStr) && ((KahluaTableImpl)corpse.getModData()).rawgetBool("headless")) {
                    return ScriptManager.instance.getModelScript(adef.bodyModelHeadlessStr);
                }
                if (!StringUtils.isNullOrEmpty(adef.bodyModelFleeceStr) && ((KahluaTableImpl)corpse.getModData()).rawgetBool("shouldBeBodyFleece")) {
                    return ScriptManager.instance.getModelScript(adef.bodyModelFleeceStr);
                }
            }
            return ScriptManager.instance.getModelScript(adef.bodyModelStr);
        }
        if (!StringUtils.isNullOrEmpty(animal.adef.textureSkinned) && ((KahluaTableImpl)animal.getModData()).rawgetBool("skinned")) {
            this.skinTextureName = animal.adef.textureSkinned;
        }
        if (!StringUtils.isNullOrEmpty(animal.adef.bodyModelHeadlessStr) && animal.getModData() != null && ((KahluaTableImpl)animal.getModData()).rawgetBool("headless")) {
            return ScriptManager.instance.getModelScript(animal.adef.bodyModelHeadlessStr);
        }
        if (StringUtils.isNullOrEmpty(animal.getBreed().woolType)) {
            return ScriptManager.instance.getModelScript(animal.adef.bodyModelStr);
        }
        if (animal.getData().getWoolQuantity() >= animal.getData().getMaxWool() / 2.0f && animal.adef.bodyModelFleeceStr != null) {
            return ScriptManager.instance.getModelScript(animal.adef.bodyModelFleeceStr);
        }
        return ScriptManager.instance.getModelScript(animal.adef.bodyModelStr);
    }

    @Override
    public void dressInNamedOutfit(String outfitName, ItemVisuals itemVisuals) {
        itemVisuals.clear();
    }

    public String getAnimalType() {
        return this.owner.getAnimalType();
    }

    public float getAnimalSize() {
        return this.owner.getAnimalSize();
    }

    public IsoAnimal getIsoAnimal() {
        IAnimalVisual iAnimalVisual = this.owner;
        if (iAnimalVisual instanceof IsoAnimal) {
            IsoAnimal animal = (IsoAnimal)iAnimalVisual;
            return animal;
        }
        AnimatedModel animatedModel = Type.tryCastTo(this.owner, AnimatedModel.class);
        if (animatedModel != null && animatedModel.getCharacter() instanceof IsoAnimal) {
            return (IsoAnimal)animatedModel.getCharacter();
        }
        return null;
    }

    public String getSkinTexture() {
        AnimalDefinitions adef = AnimalDefinitions.getDef(this.owner.getAnimalType());
        if (this.animalRotStage > -1 && adef != null) {
            switch (this.animalRotStage) {
                case 1: {
                    if (!StringUtils.isNullOrEmpty(adef.textureRotten)) {
                        return adef.textureRotten;
                    }
                    return this.skinTextureName;
                }
                case 2: {
                    if (!StringUtils.isNullOrEmpty(adef.textureSkeletonBloody)) {
                        return adef.textureSkeletonBloody;
                    }
                    return this.skinTextureName;
                }
                case 3: {
                    if (!StringUtils.isNullOrEmpty(adef.textureSkeleton)) {
                        return adef.textureSkeleton;
                    }
                    return this.skinTextureName;
                }
            }
        }
        return this.skinTextureName;
    }

    public void setSkinTextureName(String textureName) {
        this.skinTextureName = textureName;
    }

    public boolean isSkeleton() {
        return this.owner != null && this.owner.isSkeleton();
    }

    @Override
    public void clear() {
        this.skinTextureName = null;
        this.animalRotStage = -1;
    }

    @Override
    public void copyFrom(BaseVisual baseVisual) {
        if (baseVisual == null) {
            this.clear();
            return;
        }
        if (!(baseVisual instanceof AnimalVisual)) {
            throw new IllegalArgumentException("expected AnimalVisual, got " + String.valueOf(baseVisual));
        }
        AnimalVisual other = (AnimalVisual)baseVisual;
        this.skinTextureName = other.skinTextureName;
        this.animalRotStage = other.animalRotStage;
    }
}

