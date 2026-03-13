/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.resources;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceBlueprint;
import zombie.entity.energy.Energy;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.network.GameClient;
import zombie.ui.ObjectTooltip;

@DebugClassFields
@UsedFromLua
public class ResourceEnergy
extends Resource {
    private float storedEnergy;
    private Energy energy;
    private float capacity;

    protected ResourceEnergy() {
    }

    @Override
    void loadBlueprint(ResourceBlueprint bp) {
        super.loadBlueprint(bp);
        this.capacity = bp.getCapacity();
        this.setEnergyType(this.getFilterName());
    }

    private void setEnergyType(String energyType) {
        if (energyType != null) {
            this.energy = Energy.Get(energyType);
            if (this.energy == null) {
                DebugLog.General.warn("Energy not found: " + energyType);
            }
        } else {
            DebugLog.General.warn("Energy Type is null!");
            this.energy = Energy.VoidEnergy;
        }
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        if (layout == null) {
            return;
        }
        super.DoTooltip(tooltipUI, layout);
        ObjectTooltip.LayoutItem item = layout.addItem();
        item.setLabel(Translator.getEntityText("EC_Stored") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
        item.setValue((int)(this.getEnergyRatio() * 100.0f) + " %", 1.0f, 1.0f, 1.0f, 1.0f);
        if (this.energy != null) {
            item = layout.addItem();
            item.setLabel(Translator.getEntityText("EC_Energy") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            item.setValue(this.energy.getDisplayName(), 1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            item = layout.addItem();
            item.setLabel(Translator.getEntityText("EC_Energy_Not_Set"), 1.0f, 1.0f, 0.8f, 1.0f);
        }
        if (Core.debug && DebugOptions.instance.entityDebugUi.getValue()) {
            this.DoDebugTooltip(tooltipUI, layout);
        }
    }

    @Override
    protected void DoDebugTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        if (layout == null || !Core.debug) {
            return;
        }
        super.DoDebugTooltip(tooltipUI, layout);
        float a = 0.7f;
        ObjectTooltip.LayoutItem item = layout.addItem();
        item.setLabel("EnergyStored:", 0.7f, 0.7f, 0.56f, 1.0f);
        item.setValue("" + this.storedEnergy, 0.7f, 0.7f, 0.7f, 1.0f);
        item = layout.addItem();
        item.setLabel("EnergyCapacity:", 0.7f, 0.7f, 0.56f, 1.0f);
        item.setValue("" + this.capacity, 0.7f, 0.7f, 0.7f, 1.0f);
    }

    @Override
    public boolean isFull() {
        return this.storedEnergy >= this.capacity;
    }

    @Override
    public boolean isEmpty() {
        return this.storedEnergy <= 0.0f;
    }

    public Energy getEnergy() {
        return this.energy;
    }

    @Override
    public float getEnergyAmount() {
        return this.storedEnergy;
    }

    @Override
    public float getEnergyCapacity() {
        return this.capacity;
    }

    @Override
    public float getFreeEnergyCapacity() {
        return this.getEnergyCapacity() - this.getEnergyAmount();
    }

    public float getEnergyRatio() {
        if (this.capacity <= 0.0f) {
            return 0.0f;
        }
        return PZMath.clamp_01(this.storedEnergy / this.capacity);
    }

    public boolean setEnergyAmount(float amount) {
        if (GameClient.client) {
            return false;
        }
        if (this.storedEnergy != (amount = PZMath.min(PZMath.max(0.0f, amount), this.capacity))) {
            this.storedEnergy = amount;
            this.setDirty();
            return true;
        }
        return false;
    }

    @Override
    public boolean canDrainToItem(InventoryItem item) {
        DrainableComboItem comboItem;
        if (GameClient.client || this.isEmpty()) {
            return false;
        }
        if (item != null && item instanceof DrainableComboItem && !(comboItem = (DrainableComboItem)item).isFullUses() && comboItem.isEnergy()) {
            return this.getEnergy().equals(comboItem.getEnergy());
        }
        return false;
    }

    @Override
    public boolean drainToItem(InventoryItem item) {
        if (GameClient.client) {
            DebugLog.General.warn("Not allowed on client");
            return false;
        }
        if (this.canDrainToItem(item)) {
            DrainableComboItem comboItem = (DrainableComboItem)item;
            float transfer = PZMath.min(1.0f - comboItem.getCurrentUsesFloat(), this.getEnergyAmount());
            this.setEnergyAmount(this.getEnergyAmount() - transfer);
            int transferint = (int)((float)comboItem.getMaxUses() * transfer);
            comboItem.setCurrentUses(comboItem.getCurrentUses() + transferint);
            this.setDirty();
            return true;
        }
        return false;
    }

    @Override
    public boolean canDrainFromItem(InventoryItem item) {
        DrainableComboItem comboItem;
        if (GameClient.client || this.isFull()) {
            return false;
        }
        if (item != null && item instanceof DrainableComboItem && !(comboItem = (DrainableComboItem)item).isEmptyUses() && comboItem.isEnergy()) {
            return this.getEnergy().equals(comboItem.getEnergy());
        }
        return false;
    }

    @Override
    public boolean drainFromItem(InventoryItem item) {
        if (GameClient.client) {
            DebugLog.General.warn("Not allowed on client");
            return false;
        }
        if (this.canDrainFromItem(item)) {
            DrainableComboItem comboItem = (DrainableComboItem)item;
            float transfer = PZMath.min(this.getFreeEnergyCapacity(), comboItem.getCurrentUsesFloat());
            int transferint = (int)((float)comboItem.getMaxUses() * transfer);
            comboItem.setCurrentUses(comboItem.getCurrentUses() - transferint);
            this.setEnergyAmount(this.getEnergyAmount() + transfer);
            this.setDirty();
            return true;
        }
        return false;
    }

    @Override
    public void tryTransferTo(Resource target) {
        this.tryTransferTo(target, this.getEnergyAmount());
    }

    @Override
    public void tryTransferTo(Resource target, float amount) {
        if (this.isEmpty() || target == null || target.isFull()) {
            return;
        }
        if (target instanceof ResourceEnergy) {
            ResourceEnergy resourceEnergy = (ResourceEnergy)target;
            this.transferTo(resourceEnergy, amount);
        }
    }

    public void transferTo(ResourceEnergy target, float transferAmount) {
        if (this.isEmpty() || target == null || target.isFull()) {
            return;
        }
        float amount = PZMath.min(PZMath.min(transferAmount, this.getEnergyAmount()), target.getFreeEnergyCapacity());
        if (amount <= 0.0f) {
            return;
        }
        this.setEnergyAmount(this.getEnergyAmount() - amount);
        target.setEnergyAmount(target.getEnergyAmount() + amount);
        this.setDirty();
    }

    @Override
    public void clear() {
        if (GameClient.client) {
            DebugLog.General.warn("Not allowed on client");
            return;
        }
        this.storedEnergy = 0.0f;
        this.setDirty();
    }

    @Override
    protected void reset() {
        super.reset();
        this.storedEnergy = 0.0f;
        this.capacity = 0.0f;
        this.energy = null;
    }

    @Override
    public void saveSync(ByteBuffer output) throws IOException {
        this.save(output);
    }

    @Override
    public void loadSync(ByteBuffer input, int worldVersion) throws IOException {
        this.load(input, worldVersion);
    }

    @Override
    public void save(ByteBuffer output) throws IOException {
        super.save(output);
        Energy.saveEnergy(this.energy, output);
        output.putFloat(this.capacity);
        output.putFloat(this.storedEnergy);
    }

    @Override
    public void load(ByteBuffer input, int worldVersion) throws IOException {
        super.load(input, worldVersion);
        this.storedEnergy = 0.0f;
        this.energy = Energy.loadEnergy(input, worldVersion);
        this.capacity = input.getFloat();
        this.storedEnergy = input.getFloat();
    }
}

