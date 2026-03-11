/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory.types;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoWorld;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemType;
import zombie.ui.ObjectTooltip;
import zombie.ui.UIFont;
import zombie.util.StringUtils;

@UsedFromLua
public class AnimalInventoryItem
extends InventoryItem {
    private IsoAnimal animal;
    private String animalName;

    public AnimalInventoryItem(String module, String name, String type, String tex) {
        super(module, name, type, tex);
        this.itemType = ItemType.ANIMAL;
    }

    public AnimalInventoryItem(String module, String name, String type, Item item) {
        super(module, name, type, item);
        this.itemType = ItemType.ANIMAL;
    }

    @Override
    public void update() {
        if (this.getContainer() == null) {
            return;
        }
        if (this.animal != null) {
            this.animal.container = this.getContainer();
            this.animal.square = null;
            this.animal.setCurrent(null);
            this.animal.update();
        }
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        tooltipUI.render();
        UIFont font = tooltipUI.getFont();
        int lineSpacing = tooltipUI.getLineSpacing();
        int y = 5;
        ObjectTooltip.LayoutItem item = layout.addItem();
        item.setLabel(Translator.getText("IGUI_AnimalType") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
        item.setValue(Translator.getText("IGUI_AnimalType_" + this.animal.getAnimalType()), 1.0f, 1.0f, 1.0f, 1.0f);
        item = layout.addItem();
        item.setLabel(Translator.getText("UI_characreation_gender") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
        String text = Translator.getText("IGUI_Animal_Female");
        if (!this.animal.isFemale()) {
            text = Translator.getText("IGUI_Animal_Male");
        }
        item.setValue(text, 1.0f, 1.0f, 1.0f, 1.0f);
        item = layout.addItem();
        item.setLabel(Translator.getText("IGUI_char_Age") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
        item.setValue(this.animal.getAgeText(Core.getInstance().animalCheat, IsoPlayer.getInstance().getPerkLevel(PerkFactory.Perks.Husbandry)), 1.0f, 1.0f, 1.0f, 1.0f);
        item = layout.addItem();
        item.setLabel(Translator.getText("IGUI_Animal_Appearance") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
        item.setValue(this.animal.getAppearanceText(Core.getInstance().animalCheat), 1.0f, 1.0f, 1.0f, 1.0f);
        item = layout.addItem();
        item.setLabel(Translator.getText("IGUI_XP_Health") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
        item.setValue(this.animal.getHealthText(Core.getInstance().animalCheat, IsoPlayer.getInstance().getPerkLevel(PerkFactory.Perks.Husbandry)), 1.0f, 1.0f, 1.0f, 1.0f);
        if (Core.getInstance().animalCheat) {
            item = layout.addItem();
            item.setLabel("[DEBUG] Stress:", 1.0f, 1.0f, 0.8f, 1.0f);
            item.setValue("" + Math.round(this.animal.getStress()), 1.0f, 1.0f, 1.0f, 1.0f);
            if (this.animal.heldBy != null) {
                item = layout.addItem();
                item.setLabel("[DEBUG] Acceptance:", 1.0f, 1.0f, 0.8f, 1.0f);
                item.setValue("" + Math.round(this.animal.getAcceptanceLevel(this.animal.heldBy)), 1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
    }

    @Override
    public boolean finishupdate() {
        return false;
    }

    public void initAnimalData() {
        this.animalName = !StringUtils.isNullOrEmpty(this.animal.getCustomName()) ? this.animal.getCustomName() : Translator.getText("IGUI_Breed_" + this.animal.getBreed().getName()) + " " + Translator.getText("IGUI_AnimalType_" + this.animal.getAnimalType());
        this.setName(this.animalName);
        this.setWeight(this.animal.adef.baseEncumbrance * this.animal.getAnimalSize());
        this.setActualWeight(this.getWeight());
        String icon = this.animal.getInventoryIconTextureName();
        if (!StringUtils.isNullOrEmpty(icon)) {
            this.setIcon(Texture.getSharedTexture(icon));
        }
        if (this.animal.mother != null) {
            this.animal.attachBackToMother = this.animal.mother.animalId;
        }
    }

    public IsoAnimal getAnimal() {
        return this.animal;
    }

    public void setAnimal(IsoAnimal animal) {
        this.animal = animal;
        this.animal.setItemID(this.id);
        this.initAnimalData();
    }

    @Override
    public void save(ByteBuffer output, boolean net) throws IOException {
        super.save(output, net);
        this.animal.save(output, net, false);
    }

    @Override
    public void load(ByteBuffer input, int worldVersion) throws IOException {
        super.load(input, worldVersion);
        this.animal = new IsoAnimal(IsoWorld.instance.getCell());
        this.animal.load(input, worldVersion, false);
    }

    @Override
    public String getCategory() {
        return "Animal";
    }

    @Override
    public boolean shouldUpdateInWorld() {
        return true;
    }
}

