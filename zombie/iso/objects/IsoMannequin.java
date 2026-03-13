/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import org.joml.Vector3f;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.WornItems.BodyLocations;
import zombie.characters.WornItems.WornItems;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.Shader;
import zombie.core.properties.IsoObjectChange;
import zombie.core.properties.IsoPropertyType;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AnimNode;
import zombie.core.skinnedmodel.advancedanimation.AnimState;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.advancedanimation.AnimationSet;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugLog;
import zombie.gameStates.GameLoadingState;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Moveable;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SliceY;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderCorpses;
import zombie.iso.fboRenderChunk.FBORenderObjectHighlight;
import zombie.iso.fboRenderChunk.FBORenderShadows;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.zones.Zone;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.MannequinScript;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.ResourceLocation;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public class IsoMannequin
extends IsoObject
implements IHumanVisual {
    private static final ColorInfo inf = new ColorInfo();
    private boolean init;
    private boolean female;
    private boolean zombie;
    private boolean skeleton;
    private String mannequinScriptName;
    private String modelScriptName;
    private String textureName;
    private String animSet;
    private String animState;
    private String pose;
    private String outfit;
    private final HumanVisual humanVisual = new HumanVisual(this);
    private final ItemVisuals itemVisuals = new ItemVisuals();
    private final WornItems wornItems;
    private MannequinScript mannequinScript;
    private ModelScript modelScript;
    private final PerPlayer[] perPlayer = new PerPlayer[4];
    private boolean animate;
    private AnimatedModel animatedModel;
    private Drawer[] drawers;
    private float screenX;
    private float screenY;
    private static final StaticPerPlayer[] staticPerPlayer = new StaticPerPlayer[4];

    public IsoMannequin(IsoCell cell) {
        super(cell);
        this.wornItems = new WornItems(BodyLocations.getGroup("Human"));
        for (int i = 0; i < 4; ++i) {
            this.perPlayer[i] = new PerPlayer();
        }
    }

    public IsoMannequin(IsoCell cell, IsoGridSquare square, IsoSprite sprite) {
        super(cell, square, sprite);
        this.wornItems = new WornItems(BodyLocations.getGroup("Human"));
        for (int i = 0; i < 4; ++i) {
            this.perPlayer[i] = new PerPlayer();
        }
    }

    @Override
    public String getObjectName() {
        return "Mannequin";
    }

    @Override
    public HumanVisual getHumanVisual() {
        return this.humanVisual;
    }

    @Override
    public void getItemVisuals(ItemVisuals itemVisuals) {
        this.wornItems.getItemVisuals(itemVisuals);
    }

    @Override
    public boolean isFemale() {
        return this.female;
    }

    @Override
    public boolean isZombie() {
        return this.zombie;
    }

    @Override
    public boolean isSkeleton() {
        return this.skeleton;
    }

    @Override
    public boolean isItemAllowedInContainer(ItemContainer container, InventoryItem item) {
        if (item instanceof Clothing && item.getBodyLocation() != null) {
            return true;
        }
        return item instanceof InventoryContainer && item.canBeEquipped() != null;
    }

    public String getMannequinScriptName() {
        return this.mannequinScriptName;
    }

    public void setMannequinScriptName(String name) {
        if (StringUtils.isNullOrWhitespace(name)) {
            return;
        }
        if (ScriptManager.instance.getMannequinScript(name) == null) {
            return;
        }
        this.mannequinScriptName = name;
        this.init = true;
        this.mannequinScript = null;
        this.textureName = null;
        this.animSet = null;
        this.animState = null;
        this.pose = null;
        this.outfit = null;
        this.humanVisual.clear();
        this.itemVisuals.clear();
        this.wornItems.clear();
        this.initMannequinScript();
        this.initModelScript();
        if (this.outfit == null) {
            Outfit outfit = OutfitManager.instance.GetRandomNonProfessionalOutfit(this.female);
            this.humanVisual.dressInNamedOutfit(outfit.name, this.itemVisuals);
        } else if (!"none".equalsIgnoreCase(this.outfit)) {
            this.humanVisual.dressInNamedOutfit(this.outfit, this.itemVisuals);
        }
        this.humanVisual.setHairModel("");
        this.humanVisual.setBeardModel("");
        this.createInventory(this.itemVisuals);
        this.validateSkinTexture();
        this.validatePose();
        this.syncModel();
    }

    public String getPose() {
        return this.pose;
    }

    public void setRenderDirection(IsoDirections newDir) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        if (newDir == this.perPlayer[playerIndex].renderDirection) {
            return;
        }
        this.perPlayer[playerIndex].renderDirection = newDir;
        if (PerformanceSettings.fboRenderChunk) {
            this.invalidateRenderChunkLevel(256L);
        }
    }

    public void rotate(IsoDirections newDir) {
        if (newDir == null) {
            return;
        }
        this.dir = newDir;
        for (int i = 0; i < 4; ++i) {
            this.perPlayer[i].atlasTex = null;
        }
        if (GameServer.server) {
            this.sendObjectChange(IsoObjectChange.ROTATE);
        }
        this.invalidateRenderChunkLevel(256L);
    }

    @Override
    public void saveChange(IsoObjectChange change, KahluaTable tbl, ByteBufferWriter bb) {
        if (change == IsoObjectChange.ROTATE) {
            bb.putEnum(this.dir);
        } else {
            super.saveChange(change, tbl, bb);
        }
    }

    @Override
    public void loadChange(IsoObjectChange change, ByteBufferReader bb) {
        if (change == IsoObjectChange.ROTATE) {
            this.rotate(bb.getEnum(IsoDirections.class));
        } else {
            super.loadChange(change, bb);
        }
    }

    public void getVariables(Map<String, String> vars) {
        vars.put("Female", this.female ? "true" : "false");
        vars.put("Pose", this.getPose());
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        block5: {
            super.load(input, worldVersion, isDebugSave);
            this.dir = IsoDirections.fromIndex(input.get());
            this.init = input.get() != 0;
            this.female = input.get() != 0;
            this.zombie = input.get() != 0;
            this.skeleton = input.get() != 0;
            this.mannequinScriptName = GameWindow.ReadString(input);
            this.pose = GameWindow.ReadString(input);
            this.humanVisual.load(input, worldVersion);
            this.textureName = this.humanVisual.getSkinTexture();
            this.wornItems.clear();
            if (this.container == null) {
                this.container = new ItemContainer("mannequin", this.getSquare(), this);
                this.container.setExplored(true);
            }
            this.container.clear();
            if (input.get() != 0) {
                try {
                    this.container.id = input.getInt();
                    ArrayList<InventoryItem> savedItems = this.container.load(input, worldVersion);
                    int wornItemCount = input.get();
                    for (int i = 0; i < wornItemCount; ++i) {
                        ItemBodyLocation itemBodyLocation = ItemBodyLocation.get(ResourceLocation.of(GameWindow.ReadString(input)));
                        short index = input.getShort();
                        if (index < 0 || index >= savedItems.size() || this.wornItems.getBodyLocationGroup().getLocation(itemBodyLocation) == null) continue;
                        this.wornItems.setItem(itemBodyLocation, savedItems.get(index));
                    }
                }
                catch (Exception ex) {
                    if (this.container == null) break block5;
                    DebugLog.log("Failed to stream in container ID: " + this.container.id);
                }
            }
        }
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        ItemContainer container = this.container;
        this.container = null;
        super.save(output, isDebugSave);
        this.container = container;
        output.put((byte)this.dir.ordinal());
        output.put(this.init ? (byte)1 : 0);
        output.put(this.female ? (byte)1 : 0);
        output.put(this.zombie ? (byte)1 : 0);
        output.put(this.skeleton ? (byte)1 : 0);
        GameWindow.WriteString(output, this.mannequinScriptName);
        GameWindow.WriteString(output, this.pose);
        this.humanVisual.save(output);
        if (container != null) {
            output.put((byte)1);
            output.putInt(container.id);
            ArrayList<InventoryItem> savedItems = container.save(output);
            if (this.wornItems.size() > 127) {
                throw new RuntimeException("too many worn items");
            }
            output.put((byte)this.wornItems.size());
            this.wornItems.forEach(wornItem -> {
                GameWindow.WriteString(output, wornItem.getLocation().toString());
                output.putShort((short)savedItems.indexOf(wornItem.getItem()));
            });
        } else {
            output.put((byte)0);
        }
    }

    @Override
    public void saveState(ByteBuffer output) throws IOException {
        if (!this.init) {
            this.initOutfit();
        }
        this.save(output);
    }

    @Override
    public void loadState(ByteBuffer input) throws IOException {
        input.get();
        input.get();
        this.load(input, 244);
        this.initOutfit();
        this.validateSkinTexture();
        this.validatePose();
        this.syncModel();
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        if (this.square != null && this.square.chunk != null && this.square.chunk.jobType == IsoChunk.JobType.SoftReset) {
            return;
        }
        this.initOutfit();
        this.validateSkinTexture();
        this.validatePose();
        this.syncModel();
        if (!FBORenderCell.instance.mannequinList.contains(this)) {
            FBORenderCell.instance.mannequinList.add(this);
        }
    }

    @Override
    public void removeFromWorld() {
        super.removeFromWorld();
        FBORenderCell.instance.mannequinList.remove(this);
    }

    private void initMannequinScript() {
        if (!StringUtils.isNullOrWhitespace(this.mannequinScriptName)) {
            this.mannequinScript = ScriptManager.instance.getMannequinScript(this.mannequinScriptName);
        }
        if (this.mannequinScript == null) {
            this.modelScriptName = this.female ? "FemaleBody" : "MaleBody";
            this.textureName = this.female ? "F_Mannequin_White" : "M_Mannequin_White";
            this.animSet = "mannequin";
            this.animState = this.female ? "female" : "male";
            this.outfit = null;
        } else {
            this.female = this.mannequinScript.isFemale();
            this.modelScriptName = this.mannequinScript.getModelScriptName();
            if (this.textureName == null) {
                this.textureName = this.mannequinScript.getTexture();
            }
            this.animSet = this.mannequinScript.getAnimSet();
            this.animState = this.mannequinScript.getAnimState();
            if (this.pose == null) {
                this.pose = this.mannequinScript.getPose();
            }
            if (this.outfit == null) {
                this.outfit = this.mannequinScript.getOutfit();
            }
        }
    }

    private void initModelScript() {
        if (!StringUtils.isNullOrWhitespace(this.modelScriptName)) {
            this.modelScript = ScriptManager.instance.getModelScript(this.modelScriptName);
        }
    }

    private void validateSkinTexture() {
    }

    private void validatePose() {
        AnimationSet animSet = AnimationSet.GetAnimationSet(this.animSet, false);
        if (animSet == null) {
            DebugLog.General.warn("ERROR: mannequin AnimSet \"%s\" doesn't exist", this.animSet);
            this.pose = "Invalid";
            return;
        }
        AnimState state = animSet.GetState(this.animState);
        if (state == null) {
            DebugLog.General.warn("ERROR: mannequin AnimSet \"%s\" state \"%s\" doesn't exist", this.animSet, this.animState);
            this.pose = "Invalid";
            return;
        }
        for (AnimNode node : state.nodes) {
            if (!node.name.equalsIgnoreCase(this.pose)) continue;
            return;
        }
        if (state.nodes == null) {
            DebugLog.General.warn("ERROR: mannequin AnimSet \"%s\" state \"%s\" node \"%s\" doesn't exist", this.animSet, this.animState, this.pose);
            this.pose = "Invalid";
            return;
        }
        AnimNode node = PZArrayUtil.pickRandom(state.nodes);
        this.pose = node.name;
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoChild, boolean bWallLightingPass, Shader shader) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        this.calcScreenPos(x += 0.5f, y += 0.5f, z);
        this.renderShadow(x, y, z);
        if (this.animate) {
            this.animatedModel.update();
            Drawer drawer = this.drawers[SpriteRenderer.instance.getMainStateIndex()];
            drawer.init(x, y, z);
            SpriteRenderer.instance.drawGeneric(drawer);
            return;
        }
        boolean bHighlighted = FBORenderObjectHighlight.getInstance().shouldRenderObjectHighlight(this);
        if (FBORenderChunkManager.instance.isCaching()) {
            FBORenderChunk renderChunk = this.square.chunk.getRenderLevels(playerIndex).getFBOForLevel(this.square.z, Core.getInstance().getZoom(playerIndex));
            IsoDirections dirOld = this.dir;
            PerPlayer perPlayer = this.perPlayer[playerIndex];
            if (perPlayer.renderDirection != null) {
                this.dir = perPlayer.renderDirection;
                perPlayer.renderDirection = null;
                perPlayer.wasRenderDirection = true;
                perPlayer.atlasTex = null;
            } else if (perPlayer.wasRenderDirection) {
                perPlayer.wasRenderDirection = false;
                perPlayer.atlasTex = null;
            }
            if (FBORenderCell.instance.isBlackedOutBuildingSquare(this.square)) {
                return;
            }
            float oldAlpha = this.getAlpha(playerIndex);
            float alpha = PZMath.min(IsoWorldInventoryObject.getSurfaceAlpha(this.square, this.getZ() - (float)PZMath.fastfloor(this.getZ())), oldAlpha);
            if (alpha == 0.0f) {
                return;
            }
            FBORenderCorpses.getInstance().render(renderChunk.index, this);
            perPlayer.lastRenderDirection = this.dir;
            this.dir = dirOld;
            return;
        }
        if (PerformanceSettings.fboRenderChunk) {
            if (this.animatedModel == null) {
                this.animatedModel = new AnimatedModel();
                this.animatedModel.setAnimate(false);
                this.drawers = new Drawer[3];
                for (int i = 0; i < this.drawers.length; ++i) {
                    this.drawers[i] = new Drawer(this);
                }
            }
            this.animatedModel.setAnimSetName(this.getAnimSetName());
            this.animatedModel.setState(this.getAnimStateName());
            this.animatedModel.setVariable("Female", this.female);
            this.animatedModel.setVariable("Pose", this.getPose());
            IsoDirections dir = this.dir;
            PerPlayer perPlayer = this.perPlayer[playerIndex];
            if (perPlayer.renderDirection != null) {
                dir = perPlayer.renderDirection;
                perPlayer.renderDirection = null;
                perPlayer.wasRenderDirection = true;
                perPlayer.atlasTex = null;
            } else if (perPlayer.wasRenderDirection) {
                perPlayer.wasRenderDirection = false;
                perPlayer.atlasTex = null;
            }
            this.animatedModel.setAngle(dir.ToVector());
            this.animatedModel.setModelData(this.humanVisual, this.itemVisuals);
            this.animatedModel.update();
            Drawer drawer = this.drawers[SpriteRenderer.instance.getMainStateIndex()];
            IsoObject[] objects = this.square.getObjects().getElements();
            for (int i = 0; i < this.square.getObjects().size(); ++i) {
                IsoObject object = objects[i];
                if (!object.isTableSurface()) continue;
                z += (object.getSurfaceOffset() + 1.0f) / 96.0f;
            }
            if (bHighlighted) {
                this.animatedModel.setTint(this.getHighlightColor(playerIndex));
            } else {
                this.animatedModel.setTint(1.0f, 1.0f, 1.0f);
            }
            drawer.init(x, y, z);
            SpriteRenderer.instance.drawGeneric(drawer);
            return;
        }
        IsoDirections dirOld = this.dir;
        PerPlayer perPlayer = this.perPlayer[playerIndex];
        if (perPlayer.renderDirection != null) {
            this.dir = perPlayer.renderDirection;
            perPlayer.renderDirection = null;
            perPlayer.wasRenderDirection = true;
            perPlayer.atlasTex = null;
        } else if (perPlayer.wasRenderDirection) {
            perPlayer.wasRenderDirection = false;
            perPlayer.atlasTex = null;
        }
        if (perPlayer.atlasTex == null) {
            perPlayer.atlasTex = DeadBodyAtlas.instance.getBodyTexture(this);
            DeadBodyAtlas.instance.render();
        }
        this.dir = dirOld;
        if (perPlayer.atlasTex != null) {
            if (bHighlighted) {
                IsoMannequin.inf.r = this.getHighlightColor((int)playerIndex).r;
                IsoMannequin.inf.g = this.getHighlightColor((int)playerIndex).g;
                IsoMannequin.inf.b = this.getHighlightColor((int)playerIndex).b;
                IsoMannequin.inf.a = this.getHighlightColor((int)playerIndex).a;
            } else {
                IsoMannequin.inf.r = col.r;
                IsoMannequin.inf.g = col.g;
                IsoMannequin.inf.b = col.b;
                IsoMannequin.inf.a = col.a;
            }
            col = inf;
            if (!bHighlighted) {
                this.square.interpolateLight(col, x - (float)this.square.getX(), y - (float)this.square.getY());
            }
            IndieGL.disableDepthTest();
            SpriteRenderer.instance.StartShader(0, playerIndex);
            perPlayer.atlasTex.render(x, y, z, (int)this.screenX, (int)this.screenY, col.r, col.g, col.b, this.getAlpha(playerIndex));
            SpriteRenderer.instance.EndShader();
            if (Core.debug) {
                // empty if block
            }
        }
    }

    @Override
    public void renderFxMask(float x, float y, float z, boolean bDoAttached) {
    }

    public boolean shouldRenderEachFrame() {
        return this.animate;
    }

    public void checkRenderDirection(int playerIndex) {
        PerPlayer perPlayer = this.perPlayer[playerIndex];
        if (perPlayer.renderDirection == null && perPlayer.wasRenderDirection) {
            this.invalidateRenderChunkLevel(256L);
        }
    }

    private void calcScreenPos(float x, float y, float z) {
        if (IsoSprite.globalOffsetX == -1.0f) {
            IsoSprite.globalOffsetX = -IsoCamera.frameState.offX;
            IsoSprite.globalOffsetY = -IsoCamera.frameState.offY;
        }
        this.screenX = IsoUtils.XToScreen(x, y, z, 0);
        this.screenY = IsoUtils.YToScreen(x, y, z, 0);
        this.sx = this.screenX;
        this.sy = this.screenY;
        this.screenX = this.sx + IsoSprite.globalOffsetX;
        this.screenY = this.sy + IsoSprite.globalOffsetY;
        IsoObject[] objects = this.square.getObjects().getElements();
        for (int i = 0; i < this.square.getObjects().size(); ++i) {
            IsoObject object = objects[i];
            if (!object.isTableSurface()) continue;
            this.screenY -= (object.getSurfaceOffset() + 1.0f) * (float)Core.tileScale;
        }
    }

    public DeadBodyAtlas.BodyTexture getAtlasTexture() {
        this.offsetY = 0.0f;
        this.offsetX = 0.0f;
        int playerIndex = IsoCamera.frameState.playerIndex;
        PerPlayer perPlayer = this.perPlayer[playerIndex];
        IsoDirections dirOld = this.dir;
        this.dir = perPlayer.lastRenderDirection;
        if (perPlayer.atlasTex == null) {
            perPlayer.atlasTex = DeadBodyAtlas.instance.getBodyTexture(this);
            DeadBodyAtlas.instance.render();
        }
        this.dir = dirOld;
        return perPlayer.atlasTex;
    }

    public void renderShadow(float x, float y, float z) {
        if (FBORenderChunkManager.instance.isCaching()) {
            return;
        }
        float w = 0.45f;
        float fm = 0.4725f;
        float bm = 0.5611f;
        int playerIndex = IsoCamera.frameState.playerIndex;
        ColorInfo lightInfo = this.square.lighting[playerIndex].lightInfo();
        Vector3f forward = BaseVehicle.allocVector3f().set(1.0f, 0.0f, 0.0f);
        if (PerformanceSettings.fboRenderChunk) {
            IsoObject[] objects = this.square.getObjects().getElements();
            for (int i = 0; i < this.square.getObjects().size(); ++i) {
                IsoObject object = objects[i];
                if (!object.isTableSurface()) continue;
                z += (object.getSurfaceOffset() + 2.0f) / 96.0f;
            }
            FBORenderShadows.getInstance().addShadow(x, y, z, forward, 0.45f, 0.4725f, 0.5611f, lightInfo.r, lightInfo.g, lightInfo.a, 0.8f * this.getAlpha(playerIndex), false);
            BaseVehicle.releaseVector3f(forward);
            return;
        }
        IsoDeadBody.renderShadow(x, y, z, forward, 0.45f, 0.4725f, 0.5611f, lightInfo, 0.8f * this.getAlpha(playerIndex));
        BaseVehicle.releaseVector3f(forward);
    }

    private void initOutfit() {
        if (this.init) {
            this.initMannequinScript();
            this.initModelScript();
            return;
        }
        this.init = true;
        this.getPropertiesFromSprite();
        this.getPropertiesFromZone();
        this.initMannequinScript();
        this.initModelScript();
        if (this.outfit == null) {
            Outfit outfit = OutfitManager.instance.GetRandomNonProfessionalOutfit(this.female);
            this.humanVisual.dressInNamedOutfit(outfit.name, this.itemVisuals);
        } else if (!"none".equalsIgnoreCase(this.outfit)) {
            this.humanVisual.dressInNamedOutfit(this.outfit, this.itemVisuals);
        }
        this.humanVisual.setHairModel("");
        this.humanVisual.setBeardModel("");
        this.createInventory(this.itemVisuals);
    }

    private void getPropertiesFromSprite() {
        switch (this.sprite.name) {
            case "location_shop_mall_01_65": {
                this.mannequinScriptName = "FemaleWhite01";
                this.dir = IsoDirections.SE;
                break;
            }
            case "location_shop_mall_01_66": {
                this.mannequinScriptName = "FemaleWhite02";
                this.dir = IsoDirections.S;
                break;
            }
            case "location_shop_mall_01_67": {
                this.mannequinScriptName = "FemaleWhite03";
                this.dir = IsoDirections.SE;
                break;
            }
            case "location_shop_mall_01_68": {
                this.mannequinScriptName = "MaleWhite01";
                this.dir = IsoDirections.SE;
                break;
            }
            case "location_shop_mall_01_69": {
                this.mannequinScriptName = "MaleWhite02";
                this.dir = IsoDirections.S;
                break;
            }
            case "location_shop_mall_01_70": {
                this.mannequinScriptName = "MaleWhite03";
                this.dir = IsoDirections.SE;
                break;
            }
            case "location_shop_mall_01_73": {
                this.mannequinScriptName = "FemaleBlack01";
                this.dir = IsoDirections.SE;
                break;
            }
            case "location_shop_mall_01_74": {
                this.mannequinScriptName = "FemaleBlack02";
                this.dir = IsoDirections.S;
                break;
            }
            case "location_shop_mall_01_75": {
                this.mannequinScriptName = "FemaleBlack03";
                this.dir = IsoDirections.SE;
                break;
            }
            case "location_shop_mall_01_76": {
                this.mannequinScriptName = "MaleBlack01";
                this.dir = IsoDirections.SE;
                break;
            }
            case "location_shop_mall_01_77": {
                this.mannequinScriptName = "MaleBlack02";
                this.dir = IsoDirections.S;
                break;
            }
            case "location_shop_mall_01_78": {
                this.mannequinScriptName = "MaleBlack03";
                this.dir = IsoDirections.SE;
            }
        }
    }

    private void getPropertiesFromZone() {
        if (this.getObjectIndex() == -1) {
            return;
        }
        IsoMetaCell metaCell = IsoWorld.instance.getMetaGrid().getCellData(this.square.x / 256, this.square.y / 256);
        if (metaCell == null || metaCell.mannequinZones == null) {
            return;
        }
        ArrayList<MannequinZone> zones = metaCell.mannequinZones;
        MannequinZone zone = null;
        for (int i = 0; i < zones.size() && !(zone = zones.get(i)).contains(this.square.x, this.square.y, this.square.z); ++i) {
            zone = null;
        }
        if (zone == null) {
            return;
        }
        if (zone.female != -1) {
            boolean bl = this.female = zone.female == 1;
        }
        if (zone.dir != null) {
            this.dir = zone.dir;
        }
        if (zone.mannequinScript != null) {
            this.mannequinScriptName = zone.mannequinScript;
        }
        if (zone.skin != null) {
            this.textureName = zone.skin;
        }
        if (zone.pose != null) {
            this.pose = zone.pose;
        }
        if (zone.outfit != null) {
            this.outfit = zone.outfit;
        }
    }

    private void syncModel() {
        int i;
        this.humanVisual.setForceModelScript(this.modelScriptName);
        switch (this.modelScriptName) {
            case "FemaleBody": {
                this.humanVisual.setForceModel(ModelManager.instance.femaleModel);
                break;
            }
            case "MaleBody": {
                this.humanVisual.setForceModel(ModelManager.instance.maleModel);
                break;
            }
            default: {
                this.humanVisual.setForceModel(ModelManager.instance.getLoadedModel(this.modelScriptName));
            }
        }
        this.humanVisual.setSkinTextureName(this.textureName);
        this.wornItems.getItemVisuals(this.itemVisuals);
        for (i = 0; i < 4; ++i) {
            this.perPlayer[i].atlasTex = null;
        }
        if (this.animate) {
            if (this.animatedModel == null) {
                this.animatedModel = new AnimatedModel();
                this.drawers = new Drawer[3];
                for (i = 0; i < this.drawers.length; ++i) {
                    this.drawers[i] = new Drawer(this);
                }
            }
            this.animatedModel.setAnimSetName(this.getAnimSetName());
            this.animatedModel.setState(this.getAnimStateName());
            this.animatedModel.setVariable("Female", this.female);
            this.animatedModel.setVariable("Pose", this.getPose());
            this.animatedModel.setAngle(this.dir.ToVector());
            this.animatedModel.setModelData(this.humanVisual, this.itemVisuals);
        }
    }

    private void createInventory(ItemVisuals itemVisuals) {
        if (this.container == null) {
            this.container = new ItemContainer("mannequin", this.getSquare(), this);
            this.container.setExplored(true);
        }
        this.container.clear();
        this.wornItems.setFromItemVisuals(itemVisuals);
        this.wornItems.addItemsToItemContainer(this.container);
    }

    public void wearItem(InventoryItem item, IsoGameCharacter chr) {
        if (!this.container.contains(item)) {
            return;
        }
        ItemVisual itemVisual = item.getVisual();
        if (itemVisual == null) {
            return;
        }
        if (item instanceof Clothing && item.getBodyLocation() != null) {
            this.wornItems.setItem(item.getBodyLocation(), item);
        } else if (item instanceof InventoryContainer && item.canBeEquipped() != null) {
            this.wornItems.setItem(item.canBeEquipped(), item);
        } else {
            return;
        }
        if (chr != null) {
            ArrayList<InventoryItem> items = this.container.getItems();
            for (int i = 0; i < items.size(); ++i) {
                InventoryItem item1 = items.get(i);
                if (this.wornItems.contains(item1)) continue;
                this.container.removeItemOnServer(item1);
                this.container.Remove(item1);
                chr.getInventory().AddItem(item1);
                --i;
            }
        }
        this.syncModel();
        this.invalidateRenderChunkLevel(256L);
    }

    public void checkClothing(InventoryItem removedItem) {
        for (int i = 0; i < this.wornItems.size(); ++i) {
            InventoryItem item = this.wornItems.getItemByIndex(i);
            if (this.container != null && this.container.getItems().indexOf(item) != -1) continue;
            this.wornItems.remove(item);
            this.syncModel();
            this.invalidateRenderChunkLevel(256L);
            --i;
        }
    }

    public String getAnimSetName() {
        return this.animSet;
    }

    public String getAnimStateName() {
        return this.animState;
    }

    public void getCustomSettingsFromItem(InventoryItem item) throws IOException {
        if (item instanceof Moveable) {
            ByteBuffer input = item.getByteData();
            if (input == null) {
                return;
            }
            input.rewind();
            int worldVersion = input.getInt();
            input.get();
            input.get();
            this.load(input, worldVersion);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void setCustomSettingsToItem(InventoryItem item) throws IOException {
        if (item instanceof Moveable) {
            Object object = SliceY.SliceBufferLock;
            synchronized (object) {
                ByteBuffer output = SliceY.SliceBuffer;
                output.clear();
                output.putInt(244);
                this.save(output);
                output.flip();
                item.byteData = ByteBuffer.allocate(output.limit());
                item.byteData.put(output);
            }
            if (this.container != null) {
                item.setActualWeight(item.getActualWeight() + this.container.getContentsWeight());
            }
        }
    }

    public static boolean isMannequinSprite(IsoSprite sprite) {
        return "Mannequin".equals(sprite.getProperties().get(IsoPropertyType.CUSTOM_NAME));
    }

    private void resetMannequin() {
        this.init = false;
        this.female = false;
        this.zombie = false;
        this.skeleton = false;
        this.mannequinScriptName = null;
        this.modelScriptName = null;
        this.textureName = null;
        this.animSet = null;
        this.animState = null;
        this.pose = null;
        this.outfit = null;
        this.humanVisual.clear();
        this.itemVisuals.clear();
        this.wornItems.clear();
        this.mannequinScript = null;
        this.modelScript = null;
        this.animate = false;
    }

    public static void renderMoveableItem(Moveable item, int x, int y, int z, IsoDirections dir) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        StaticPerPlayer perPlayer = staticPerPlayer[playerIndex];
        if (perPlayer == null) {
            perPlayer = IsoMannequin.staticPerPlayer[playerIndex] = new StaticPerPlayer(playerIndex);
        }
        perPlayer.renderMoveableItem(item, x, y, z, dir);
    }

    public static void renderMoveableObject(IsoMannequin mannequin, int x, int y, int z, IsoDirections dir) {
        mannequin.setRenderDirection(dir);
    }

    public static IsoDirections getDirectionFromItem(Moveable item, int playerIndex) {
        StaticPerPlayer perPlayer = staticPerPlayer[playerIndex];
        if (perPlayer == null) {
            perPlayer = IsoMannequin.staticPerPlayer[playerIndex] = new StaticPerPlayer(playerIndex);
        }
        return perPlayer.getDirectionFromItem(item);
    }

    public WornItems getWornItems() {
        return this.wornItems;
    }

    private static final class PerPlayer {
        private DeadBodyAtlas.BodyTexture atlasTex;
        IsoDirections renderDirection;
        IsoDirections lastRenderDirection;
        boolean wasRenderDirection;

        private PerPlayer() {
        }
    }

    private final class Drawer
    extends TextureDraw.GenericDrawer {
        private float x;
        private float y;
        private float z;
        private float animPlayerAngle;
        private boolean rendered;
        final /* synthetic */ IsoMannequin this$0;

        private Drawer(IsoMannequin isoMannequin) {
            IsoMannequin isoMannequin2 = isoMannequin;
            Objects.requireNonNull(isoMannequin2);
            this.this$0 = isoMannequin2;
        }

        public void init(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.rendered = false;
            this.this$0.animatedModel.renderMain();
            this.animPlayerAngle = this.this$0.animatedModel.getAnimationPlayer().getRenderedAngle();
        }

        @Override
        public void render() {
            this.this$0.animatedModel.DoRenderToWorld(this.x, this.y, this.z, this.animPlayerAngle);
            this.rendered = true;
        }

        @Override
        public void postRender() {
            this.this$0.animatedModel.postRender(this.rendered);
        }
    }

    public static final class MannequinZone
    extends Zone {
        public int female = -1;
        public IsoDirections dir;
        public String mannequinScript;
        public String pose;
        public String skin;
        public String outfit;

        public MannequinZone(String name, String type, int x, int y, int z, int w, int h, KahluaTable properties) {
            super(name, type, x, y, z, w, h);
            if (properties != null) {
                String s;
                Object o = properties.rawget("Female");
                if (o instanceof Boolean) {
                    int n = this.female = o == Boolean.TRUE ? 1 : 0;
                }
                if ((o = properties.rawget("Direction")) instanceof String) {
                    s = (String)o;
                    this.dir = IsoDirections.valueOf(s);
                }
                if ((o = properties.rawget("Outfit")) instanceof String) {
                    this.outfit = s = (String)o;
                }
                if ((o = properties.rawget("Script")) instanceof String) {
                    this.mannequinScript = s = (String)o;
                }
                if ((o = properties.rawget("Skin")) instanceof String) {
                    this.skin = s = (String)o;
                }
                if ((o = properties.rawget("Pose")) instanceof String) {
                    this.pose = s = (String)o;
                }
            }
        }
    }

    private static final class StaticPerPlayer {
        private final int playerIndex;
        private Moveable moveable;
        private Moveable failedItem;
        private IsoMannequin mannequin;

        private StaticPerPlayer(int playerIndex) {
            this.playerIndex = playerIndex;
        }

        private void renderMoveableItem(Moveable item, int x, int y, int z, IsoDirections dir) {
            if (!this.checkItem(item)) {
                return;
            }
            if (this.moveable != item) {
                this.moveable = item;
                try {
                    this.mannequin.getCustomSettingsFromItem(this.moveable);
                }
                catch (IOException iOException) {
                    // empty catch block
                }
                this.mannequin.initOutfit();
                this.mannequin.validateSkinTexture();
                this.mannequin.validatePose();
                this.mannequin.syncModel();
                this.mannequin.perPlayer[this.playerIndex].atlasTex = null;
            }
            this.mannequin.square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            if (this.mannequin.square == null) {
                return;
            }
            this.mannequin.perPlayer[this.playerIndex].renderDirection = dir;
            inf.set(1.0f, 1.0f, 1.0f, 1.0f);
            this.mannequin.render(x, y, z, inf, false, false, null);
        }

        private IsoDirections getDirectionFromItem(Moveable item) {
            if (!this.checkItem(item)) {
                return IsoDirections.S;
            }
            this.moveable = null;
            try {
                this.mannequin.getCustomSettingsFromItem(item);
                return this.mannequin.getDir();
            }
            catch (Exception exception) {
                return IsoDirections.S;
            }
        }

        private boolean checkItem(Moveable item) {
            if (item == null) {
                return false;
            }
            String spriteName = item.getWorldSprite();
            IsoSprite sprite = IsoSpriteManager.instance.getSprite(spriteName);
            if (sprite == null || !IsoMannequin.isMannequinSprite(sprite)) {
                return false;
            }
            if (item.getByteData() == null) {
                Thread thread2 = Thread.currentThread();
                if (thread2 != GameWindow.gameThread && thread2 != GameLoadingState.loader && thread2 == GameServer.mainThread) {
                    return false;
                }
                if (this.mannequin == null || this.mannequin.getCell() != IsoWorld.instance.currentCell) {
                    this.mannequin = new IsoMannequin(IsoWorld.instance.currentCell);
                }
                if (this.failedItem == item) {
                    return false;
                }
                try {
                    this.mannequin.resetMannequin();
                    this.mannequin.sprite = sprite;
                    this.mannequin.initOutfit();
                    this.mannequin.validateSkinTexture();
                    this.mannequin.validatePose();
                    this.mannequin.syncModel();
                    this.mannequin.setCustomSettingsToItem(item);
                    return true;
                }
                catch (IOException ex) {
                    this.failedItem = item;
                    return false;
                }
            }
            if (this.mannequin == null || this.mannequin.getCell() != IsoWorld.instance.currentCell) {
                this.mannequin = new IsoMannequin(IsoWorld.instance.currentCell);
            }
            return true;
        }
    }
}

