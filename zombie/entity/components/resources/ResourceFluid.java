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
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidFilter;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceBlueprint;
import zombie.inventory.InventoryItem;
import zombie.network.GameClient;
import zombie.ui.ObjectTooltip;

@DebugClassFields
@UsedFromLua
public class ResourceFluid
extends Resource {
    private final FluidContainer fluidContainer;
    private final FluidFilter fluidFilter = new FluidFilter();

    protected ResourceFluid() {
        this.fluidContainer = FluidContainer.CreateContainer();
    }

    @Override
    void loadBlueprint(ResourceBlueprint bp) {
        super.loadBlueprint(bp);
        this.fluidContainer.setCapacity(bp.getCapacity());
        if (this.getFilterName() != null) {
            this.fluidFilter.setFilterScript(this.getFilterName());
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
        item.setValue((int)(this.getFluidRatio() * 100.0f) + " %", 1.0f, 1.0f, 1.0f, 1.0f);
        if (this.fluidContainer != null) {
            this.fluidContainer.DoTooltip(tooltipUI, layout);
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
    }

    public FluidContainer getFluidContainer() {
        return this.fluidContainer;
    }

    @Override
    public boolean isFull() {
        return this.fluidContainer.isFull();
    }

    @Override
    public boolean isEmpty() {
        return this.fluidContainer.isEmpty();
    }

    @Override
    public float getFluidAmount() {
        return this.fluidContainer.getAmount();
    }

    @Override
    public float getFluidCapacity() {
        return this.fluidContainer.getCapacity();
    }

    @Override
    public float getFreeFluidCapacity() {
        return this.fluidContainer.getFreeCapacity();
    }

    public float getFluidRatio() {
        if (this.getFluidCapacity() <= 0.0f) {
            return 0.0f;
        }
        return PZMath.clamp_01(this.getFluidAmount() / this.getFluidCapacity());
    }

    @Override
    public boolean canDrainToItem(InventoryItem item) {
        FluidContainer fc;
        if (GameClient.client || this.isEmpty()) {
            return false;
        }
        if (item != null && item.getFluidContainer() != null && !(fc = item.getFluidContainer()).isFull()) {
            return FluidContainer.CanTransfer(this.fluidContainer, fc);
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
            FluidContainer.Transfer(this.fluidContainer, item.getFluidContainer());
            this.setDirty();
            return true;
        }
        return false;
    }

    @Override
    public boolean canDrainFromItem(InventoryItem item) {
        FluidContainer fc;
        if (GameClient.client || this.isFull()) {
            return false;
        }
        if (item != null && item.getFluidContainer() != null && !(fc = item.getFluidContainer()).isEmpty()) {
            return FluidContainer.CanTransfer(fc, this.fluidContainer);
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
            FluidContainer.Transfer(item.getFluidContainer(), this.fluidContainer);
            this.setDirty();
            return true;
        }
        return false;
    }

    @Override
    public void tryTransferTo(Resource target) {
        this.tryTransferTo(target, this.getFluidAmount());
    }

    @Override
    public void tryTransferTo(Resource target, float amount) {
        if (this.isEmpty() || target == null || target.isFull()) {
            return;
        }
        if (target instanceof ResourceFluid) {
            ResourceFluid resourceFluid = (ResourceFluid)target;
            this.transferTo(resourceFluid, amount);
        }
    }

    public void transferTo(ResourceFluid target, float transferAmount) {
        if (this.isEmpty() || target == null || target.isFull()) {
            return;
        }
        float amount = PZMath.min(PZMath.min(transferAmount, this.getFluidAmount()), target.getFreeFluidCapacity());
        if (amount <= 0.0f) {
            return;
        }
        if (FluidContainer.CanTransfer(this.fluidContainer, target.fluidContainer)) {
            FluidContainer.Transfer(this.fluidContainer, target.fluidContainer, amount);
            this.setDirty();
        }
    }

    @Override
    public void clear() {
        if (GameClient.client) {
            DebugLog.General.warn("Not allowed on client");
            return;
        }
        this.fluidContainer.Empty();
        this.setDirty();
    }

    @Override
    protected void reset() {
        super.reset();
        this.fluidContainer.Empty();
        this.fluidFilter.setFilterScript(null);
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
        this.fluidContainer.save(output);
    }

    @Override
    public void load(ByteBuffer input, int worldVersion) throws IOException {
        super.load(input, worldVersion);
        if (this.fluidContainer != null && !this.fluidContainer.isEmpty()) {
            this.fluidContainer.Empty();
        }
        this.fluidContainer.load(input, worldVersion);
        if (this.getFilterName() != null) {
            this.fluidFilter.setFilterScript(this.getFilterName());
        } else {
            this.fluidFilter.setFilterScript(null);
        }
    }
}

