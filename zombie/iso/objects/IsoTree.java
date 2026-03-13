/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import fmod.fmod.FMODManager;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.lwjgl.opengl.GL20;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.Lua.LuaEventManager;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.parameters.ParameterMeleeHitSurface;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.inventory.types.HandWeapon;
import zombie.iso.CellLoader;
import zombie.iso.IHasHealth;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.ObjectCache;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.fboRenderChunk.FBORenderObjectHighlight;
import zombie.iso.fboRenderChunk.FBORenderTrees;
import zombie.iso.objects.RenderEffectType;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.objects.CharacterProfession;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.ItemKey;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.WeaponCategory;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public class IsoTree
extends IsoObject
implements IHasHealth {
    private static final IsoGameCharacter.Location[] s_chopTreeLocation = new IsoGameCharacter.Location[4];
    private static final ArrayList<IsoGridSquare> s_chopTreeIndicators = new ArrayList();
    private static IsoTree chopTreeHighlighted;
    public float fadeAlpha = 1.0f;
    private static final int MAX_SIZE = 6;
    private int logYield = 1;
    private int damage = 500;
    public int size = 4;
    public boolean renderFlag;
    public boolean wasFaded;
    public boolean useTreeShader;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static IsoTree getNew() {
        ObjectCache<IsoTree> objectCache = CellLoader.isoTreeCache;
        synchronized (objectCache) {
            IsoTree o = CellLoader.isoTreeCache.pop();
            if (o == null) {
                return new IsoTree();
            }
            o.sx = 0.0f;
            return o;
        }
    }

    public IsoTree() {
    }

    public IsoTree(IsoCell cell) {
        super(cell);
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        super.save(output, isDebugSave);
        output.put((byte)this.logYield);
        output.put((byte)(this.damage / 10));
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        super.load(input, worldVersion, isDebugSave);
        this.logYield = input.get();
        this.damage = input.get() * 10;
        if (this.sprite != null && this.sprite.getProperties().get("tree") != null) {
            this.size = Integer.parseInt(this.sprite.getProperties().get("tree"));
            if (this.size < 1) {
                this.size = 1;
            }
            if (this.size > 6) {
                this.size = 6;
            }
        }
    }

    @Override
    protected void checkMoveWithWind() {
        this.checkMoveWithWind(true);
    }

    public IsoTree(IsoGridSquare sq, String gid) {
        super(sq, gid, false);
        this.initTree();
    }

    public IsoTree(IsoGridSquare sq, IsoSprite gid) {
        super(sq.getCell(), sq, gid);
        this.initTree();
    }

    public void initTree() {
        this.setType(IsoObjectType.tree);
        if (this.sprite.getProperties().get("tree") != null) {
            this.size = Integer.parseInt(this.sprite.getProperties().get("tree"));
            if (this.size < 1) {
                this.size = 1;
            }
            if (this.size > 6) {
                this.size = 6;
            }
        } else {
            this.size = 4;
        }
        switch (this.size) {
            case 1: 
            case 2: {
                this.logYield = 1;
                break;
            }
            case 3: {
                this.logYield = 2;
                break;
            }
            case 4: {
                this.logYield = 3;
                break;
            }
            case 5: {
                this.logYield = 4;
                break;
            }
            case 6: {
                this.logYield = 5;
            }
        }
        this.damage = (this.logYield - 1) * 80;
        this.damage = Math.max(this.damage, 40);
    }

    @Override
    public String getObjectName() {
        return "Tree";
    }

    @Override
    public void Damage(float amount) {
        float dmg = amount * 0.05f;
        this.damage = (int)((float)this.damage - dmg);
        if (this.damage <= 0) {
            this.toppleTree();
        }
    }

    @Override
    public void HitByVehicle(BaseVehicle vehicle, float amount) {
        BaseSoundEmitter emitter = IsoWorld.instance.getFreeEmitter((float)this.square.x + 0.5f, (float)this.square.y + 0.5f, this.square.z);
        long soundRef = emitter.playSound("VehicleHitTree");
        emitter.setParameterValue(soundRef, FMODManager.instance.getParameterDescription("VehicleSpeed"), vehicle.getCurrentSpeedKmHour());
        WorldSoundManager.instance.addSound(null, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, true, 4.0f, 15.0f);
        this.Damage(this.damage);
    }

    public void WeaponHitEffects(IsoGameCharacter owner, HandWeapon weapon) {
        if (owner instanceof IsoPlayer) {
            IsoPlayer isoPlayer = (IsoPlayer)owner;
            isoPlayer.setMeleeHitSurface(ParameterMeleeHitSurface.Material.Tree);
            if (weapon != null) {
                owner.getEmitter().playSound(weapon.getZombieHitSound());
            }
        } else {
            owner.getEmitter().playSound("ChopTree");
        }
        WorldSoundManager.instance.addSound(null, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, false, 4.0f, 15.0f);
        this.setRenderEffect(RenderEffectType.Hit_Tree_Shudder, true);
    }

    @Override
    public void WeaponHit(IsoGameCharacter owner, HandWeapon weapon) {
        if (!GameClient.client && weapon != null) {
            int skill = owner.getWeaponLevel(weapon);
            weapon.checkSyncItemFields(weapon.damageCheck(skill, weapon.hasTag(ItemTag.CHOP_TREE) ? 2.0f : 1.0f, true, true, owner));
        }
        if (!GameServer.server) {
            this.WeaponHitEffects(owner, weapon);
        }
        float dmg = weapon.getTreeDamage();
        if (owner.hasTrait(CharacterTrait.AXEMAN) && weapon.isOfWeaponCategory(WeaponCategory.AXE)) {
            dmg *= 1.5f;
        }
        this.damage = (int)((float)this.damage - dmg);
        if (this.damage <= 0) {
            this.toppleTree(owner);
        }
    }

    @Override
    public void setHealth(int health) {
        this.damage = Math.max(health, 0);
    }

    @Override
    public int getHealth() {
        return this.damage;
    }

    @Override
    public int getMaxHealth() {
        int maxHealth = (this.logYield - 1) * 80;
        maxHealth = Math.max(maxHealth, 40);
        return maxHealth;
    }

    public int getSize() {
        return this.size;
    }

    public float getSlowFactor(IsoMovingObject chr) {
        float mod = 1.0f;
        if (chr instanceof IsoGameCharacter) {
            IsoGameCharacter isoGameCharacter = (IsoGameCharacter)chr;
            if (isoGameCharacter.getDescriptor().isCharacterProfession(CharacterProfession.PARK_RANGER)) {
                mod = 1.5f;
            }
            if (isoGameCharacter.getDescriptor().isCharacterProfession(CharacterProfession.LUMBERJACK)) {
                mod = 1.2f;
            }
        }
        if (this.size == 1 || this.size == 2) {
            return 0.8f * mod;
        }
        if (this.size == 3 || this.size == 4) {
            return 0.5f * mod;
        }
        return 0.3f * mod;
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        boolean bUseStencil;
        if (this.isHighlighted()) {
            if (this.square != null) {
                chopTreeHighlighted = this;
            }
            return;
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        boolean bl = bUseStencil = !(!this.renderFlag && !(this.fadeAlpha < this.getTargetAlpha(playerIndex)) || PerformanceSettings.fboRenderChunk && !FBORenderCell.instance.renderTranslucentOnly);
        if (PerformanceSettings.fboRenderChunk && FBORenderTrees.current != null) {
            FBORenderTrees renderTrees = FBORenderTrees.current;
            Texture texture = this.getSprite().getTextureForCurrentFrame(this.getDir(), this);
            Texture texture2 = null;
            if (this.attachedAnimSprite != null && !this.attachedAnimSprite.isEmpty()) {
                IsoSpriteInstance spriteInstance = (IsoSpriteInstance)this.attachedAnimSprite.get(0);
                texture2 = spriteInstance.parentSprite.getTextureForCurrentFrame(this.getDir(), this);
            }
            if (bUseStencil) {
                float maxAlpha;
                float minAlpha;
                boolean bPlayerInside;
                float alphaStep = 0.044999998f * GameTime.getInstance().getThirtyFPSMultiplier();
                boolean bl2 = bPlayerInside = IsoCamera.frameState.camCharacterSquare != null && !IsoCamera.frameState.camCharacterSquare.has(IsoFlagType.exterior);
                float f = DebugOptions.instance.terrain.renderTiles.forceFullAlpha.getValue() ? 1.0f : (minAlpha = bPlayerInside ? 0.05f : 0.25f);
                if (this.renderFlag && this.fadeAlpha > minAlpha) {
                    this.fadeAlpha -= alphaStep;
                    if (this.fadeAlpha < minAlpha) {
                        this.fadeAlpha = minAlpha;
                    }
                }
                if (!this.renderFlag && this.fadeAlpha < (maxAlpha = this.getTargetAlpha(playerIndex))) {
                    this.fadeAlpha += alphaStep;
                    if (this.fadeAlpha > maxAlpha) {
                        this.fadeAlpha = maxAlpha;
                    }
                }
            }
            renderTrees.addTree(texture, texture2, x, y, z, col.r, col.g, col.b, col.a, this.getObjectRenderEffectsToApply(), bUseStencil, this.fadeAlpha);
        } else if (bUseStencil) {
            float maxAlpha;
            float minAlpha;
            IndieGL.enableStencilTest();
            IndieGL.glBlendFunc(770, 771);
            boolean bPlayerInside = IsoCamera.frameState.camCharacterSquare != null && !IsoCamera.frameState.camCharacterSquare.has(IsoFlagType.exterior);
            IndieGL.glStencilFunc(517, 128, 128);
            this.renderInner(x, y, z, col, bDoAttached, false);
            float alphaStep = 0.044999998f * GameTime.getInstance().getThirtyFPSMultiplier();
            float f = minAlpha = bPlayerInside ? 0.05f : 0.25f;
            if (this.renderFlag && this.fadeAlpha > minAlpha) {
                this.fadeAlpha -= alphaStep;
                if (this.fadeAlpha < minAlpha) {
                    this.fadeAlpha = minAlpha;
                }
            }
            if (!this.renderFlag && this.fadeAlpha < (maxAlpha = this.getTargetAlpha(playerIndex))) {
                this.fadeAlpha += alphaStep;
                if (this.fadeAlpha > maxAlpha) {
                    this.fadeAlpha = maxAlpha;
                }
            }
            float a = this.getAlpha(playerIndex);
            float ta = this.getTargetAlpha(playerIndex);
            this.setAlphaAndTarget(playerIndex, this.fadeAlpha);
            IndieGL.glStencilFunc(514, 128, 128);
            this.renderInner(x, y, z, col, true, false);
            this.setAlpha(playerIndex, a);
            this.setTargetAlpha(playerIndex, ta);
            if (TreeShader.instance.StartShader()) {
                TreeShader.instance.setOutlineColor(0.1f, 0.1f, 0.1f, bPlayerInside && this.fadeAlpha < 0.5f ? this.fadeAlpha : 1.0f - this.fadeAlpha);
                this.renderInner(x, y, z, col, true, true);
                IndieGL.EndShader();
            }
            IndieGL.glStencilFunc(519, 255, 255);
            IndieGL.glDefaultBlendFunc();
        } else {
            this.renderInner(x, y, z, col, bDoAttached, false);
        }
        if (!PerformanceSettings.fboRenderChunk) {
            this.checkChopTreeIndicator();
        }
    }

    private void renderInner(float x, float y, float z, ColorInfo col, boolean bDoAttached, boolean bShader) {
        Texture texture;
        if (this.sprite != null && this.sprite.name != null && this.sprite.name.contains("JUMBO")) {
            loffsetX = this.offsetX;
            loffsetY = this.offsetY;
            this.offsetX = 384 * Core.tileScale / 2 - 96 * Core.tileScale;
            this.offsetY = 256 * Core.tileScale - 32 * Core.tileScale;
            if (this.offsetX != loffsetX || this.offsetY != loffsetY) {
                this.sx = 0.0f;
            }
        } else {
            loffsetX = this.offsetX;
            loffsetY = this.offsetY;
            this.offsetX = 32 * Core.tileScale;
            this.offsetY = 96 * Core.tileScale;
            if (this.offsetX != loffsetX || this.offsetY != loffsetY) {
                this.sx = 0.0f;
            }
        }
        if (bShader && this.sprite != null && (texture = this.sprite.getTextureForCurrentFrame(this.dir, this)) != null) {
            TreeShader.instance.setStepSize(0.25f, texture.getWidth(), texture.getHeight());
        }
        boolean wasRenderFlag = this.renderFlag;
        if (!bShader) {
            this.renderFlag = false;
        }
        this.useTreeShader = bShader;
        super.render(x, y, z, col, false, false, null);
        if (this.attachedAnimSprite != null) {
            int n = this.attachedAnimSprite.size();
            for (int i = 0; i < n; ++i) {
                IsoSpriteInstance s = (IsoSpriteInstance)this.attachedAnimSprite.get(i);
                int playerIndex = IsoCamera.frameState.playerIndex;
                float fa = this.getTargetAlpha(playerIndex);
                this.setTargetAlpha(playerIndex, 1.0f);
                s.render(this, x, y, z, this.dir, this.offsetX, this.offsetY, this.isHighlighted(playerIndex) ? this.getHighlightColor(playerIndex) : col);
                this.setTargetAlpha(playerIndex, fa);
                s.update();
            }
        }
        this.renderFlag = wasRenderFlag;
    }

    @Override
    protected boolean isUpdateAlphaDuringRender() {
        return false;
    }

    @Override
    public void setSprite(IsoSprite sprite) {
        super.setSprite(sprite);
        this.initTree();
    }

    @Override
    public boolean isMaskClicked(int x, int y, boolean flip) {
        if (super.isMaskClicked(x, y, flip)) {
            return true;
        }
        if (this.attachedAnimSprite == null) {
            return false;
        }
        for (int i = 0; i < this.attachedAnimSprite.size(); ++i) {
            if (!((IsoSpriteInstance)this.attachedAnimSprite.get((int)i)).parentSprite.isMaskClicked(this.dir, x, y, flip)) continue;
            return true;
        }
        return false;
    }

    public static void setChopTreeCursorLocation(int playerIndex, int x, int y, int z) {
        if (s_chopTreeLocation[playerIndex] == null) {
            IsoTree.s_chopTreeLocation[playerIndex] = new IsoGameCharacter.Location(-1, -1, -1);
        }
        IsoGameCharacter.Location location = s_chopTreeLocation[playerIndex];
        location.x = x;
        location.y = y;
        location.z = z;
    }

    public void checkChopTreeIndicator() {
        if (this.isHighlighted()) {
            return;
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoGameCharacter.Location location = s_chopTreeLocation[playerIndex];
        if (location == null || location.x == -1 || this.square == null) {
            return;
        }
        if (this.getCell().getDrag(playerIndex) == null) {
            location.x = -1;
            return;
        }
        if (IsoUtils.DistanceToSquared((float)this.square.x + 0.5f, (float)this.square.y + 0.5f, (float)location.x + 0.5f, (float)location.y + 0.5f) < 12.25f) {
            s_chopTreeIndicators.add(this.square);
        }
    }

    public static void checkChopTreeIndicators(int playerIndex) {
        IsoGameCharacter.Location location = s_chopTreeLocation[playerIndex];
        if (location == null || location.x == -1) {
            return;
        }
        if (IsoWorld.instance.currentCell.getDrag(playerIndex) == null) {
            location.x = -1;
            return;
        }
        int chunkMinX = PZMath.fastfloor(((float)location.x - 4.0f) / 8.0f) - 1;
        int chunkMinY = PZMath.fastfloor(((float)location.y - 4.0f) / 8.0f) - 1;
        int chunkMaxX = (int)Math.ceil(((float)location.x + 4.0f) / 8.0f) + 1;
        int chunkMaxY = (int)Math.ceil(((float)location.y + 4.0f) / 8.0f) + 1;
        IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(playerIndex);
        if (chunkMap.ignore) {
            return;
        }
        for (int cy = chunkMinY; cy < chunkMaxY; ++cy) {
            for (int cx = chunkMinX; cx < chunkMaxX; ++cx) {
                FBORenderLevels renderLevels;
                IsoChunk chunk = chunkMap.getChunkForGridSquare(cx * 8, cy * 8);
                if (chunk == null || !chunk.loaded || !chunk.IsOnScreen(true) || !(renderLevels = chunk.getRenderLevels(playerIndex)).isOnScreen(0)) continue;
                ArrayList<IsoGridSquare> trees = renderLevels.treeSquares;
                for (int i = 0; i < trees.size(); ++i) {
                    IsoGridSquare square = trees.get(i);
                    IsoTree tree = square.getTree();
                    if (tree == null || tree.isHighlighted() || !(IsoUtils.DistanceToSquared((float)square.x + 0.5f, (float)square.y + 0.5f, (float)location.x + 0.5f, (float)location.y + 0.5f) < 12.25f)) continue;
                    s_chopTreeIndicators.add(square);
                }
            }
        }
    }

    public static void renderChopTreeIndicators() {
        if (!s_chopTreeIndicators.isEmpty()) {
            if (PerformanceSettings.fboRenderChunk) {
                IndieGL.disableDepthTest();
            }
            PZArrayUtil.forEach(s_chopTreeIndicators, IsoTree::renderChopTreeIndicator);
            s_chopTreeIndicators.clear();
        }
        if (chopTreeHighlighted != null) {
            IsoTree tree = chopTreeHighlighted;
            chopTreeHighlighted = null;
            if (PerformanceSettings.fboRenderChunk) {
                FBORenderObjectHighlight.getInstance().setRenderingGhostTile(true);
            }
            int playerIndex = IsoCamera.frameState.playerIndex;
            tree.renderInner(tree.square.x, tree.square.y, tree.square.z, tree.getHighlightColor(playerIndex), false, false);
            if (PerformanceSettings.fboRenderChunk) {
                FBORenderObjectHighlight.getInstance().setRenderingGhostTile(false);
            }
        }
    }

    private static void renderChopTreeIndicator(IsoGridSquare square) {
        Texture tex = Texture.getSharedTexture("media/ui/chop_tree.png");
        if (tex == null || !tex.isReady()) {
            return;
        }
        float x = square.x;
        float y = square.y;
        float z = square.z;
        float sx = IsoUtils.XToScreen(x, y, z, 0) + IsoSprite.globalOffsetX;
        float sy = IsoUtils.YToScreen(x, y, z, 0) + IsoSprite.globalOffsetY;
        IndieGL.StartShader(0);
        SpriteRenderer.instance.render(tex, sx -= (float)(32 * Core.tileScale), sy -= (float)(96 * Core.tileScale), 64 * Core.tileScale, 128 * Core.tileScale, 0.0f, 0.5f, 0.0f, 0.75f, null);
    }

    @Override
    public IsoGridSquare getRenderSquare() {
        if (this.getSquare() == null) {
            return null;
        }
        Texture texture = this.getSprite().getTextureForCurrentFrame(this.getDir(), this);
        if (texture != null && texture.getName() != null && texture.getName().contains("JUMBO")) {
            int chunksPerWidth = 8;
            if (PZMath.coordmodulo(this.square.x, 8) == 0 && PZMath.coordmodulo(this.square.y, 8) == 7) {
                return this.square.getAdjacentSquare(IsoDirections.S);
            }
            if (PZMath.coordmodulo(this.square.x, 8) == 7 && PZMath.coordmodulo(this.square.y, 8) == 0) {
                return this.square.getAdjacentSquare(IsoDirections.E);
            }
        }
        return this.getSquare();
    }

    public void dropWood() {
        int i;
        String name = this.getSprite().getName();
        boolean acorn = name != null && (name.toLowerCase().contains("oak") || name.equals("vegetation_trees_01_13") || name.equals("vegetation_trees_01_14") || name.equals("vegetation_trees_01_15"));
        boolean pinecone = name != null && (name.toLowerCase().contains("pine") || name.equals("vegetation_trees_01_08") || name.equals("vegetation_trees_01_09") || name.equals("vegetation_trees_01_010") || name.equals("vegetation_trees_01_011"));
        boolean holly = name != null && name.toLowerCase().contains("holly") && ("Autumn".equals(ClimateManager.getInstance().getSeasonName()) || "Winter".equals(ClimateManager.getInstance().getSeasonName()));
        int numPlanks = this.logYield;
        int roll = Math.min(6 - this.logYield, 4);
        if (numPlanks == 1) {
            this.square.AddWorldInventoryItem(ItemKey.Weapon.TREE_BRANCH_2, 0.0f, 0.0f, 0.0f);
        }
        if (numPlanks == 2) {
            this.square.AddWorldInventoryItem(ItemKey.Weapon.SAPLING, 0.0f, 0.0f, 0.0f);
            this.square.AddWorldInventoryItem(ItemKey.Normal.LOG, 0.0f, 0.0f, 0.0f);
        }
        if (numPlanks > 2) {
            for (i = 0; i < numPlanks - 1; ++i) {
                this.square.AddWorldInventoryItem(ItemKey.Normal.LOG, 0.0f, 0.0f, 0.0f);
                if (i > 2 && Rand.NextBool(roll)) {
                    this.square.AddWorldInventoryItem(ItemKey.Weapon.LARGE_BRANCH, 0.0f, 0.0f, 0.0f);
                }
                if (i > 2 && Rand.NextBool(roll)) {
                    this.square.AddWorldInventoryItem(ItemKey.Weapon.SAPLING, 0.0f, 0.0f, 0.0f);
                }
                if (acorn && Rand.NextBool(roll * 2)) {
                    this.square.AddWorldInventoryItem(ItemKey.Food.ACORN, 0.0f, 0.0f, 0.0f, false);
                }
                if (pinecone && Rand.NextBool(roll)) {
                    this.square.AddWorldInventoryItem(ItemKey.Normal.PINECONE, 0.0f, 0.0f, 0.0f);
                }
                if (!holly || !Rand.NextBool(roll * 2)) continue;
                this.square.AddWorldInventoryItem(ItemKey.Food.HOLLY_BERRY, 0.0f, 0.0f, 0.0f, false);
            }
        }
        for (i = 0; i < numPlanks; ++i) {
            if (Rand.NextBool(roll)) {
                this.square.AddWorldInventoryItem(ItemKey.Weapon.TREE_BRANCH_2, 0.0f, 0.0f, 0.0f);
            }
            if (!Rand.NextBool(roll)) continue;
            this.square.AddWorldInventoryItem(ItemKey.Normal.TWIGS, 0.0f, 0.0f, 0.0f);
        }
        this.square.AddWorldInventoryItem(ItemKey.Normal.SPLINTERS, 0.0f, 0.0f, 0.0f);
    }

    public void toppleTree() {
        this.toppleTree(null);
    }

    public void toppleTree(IsoGameCharacter owner) {
        if (GameClient.client) {
            return;
        }
        this.square.transmitRemoveItemFromSquare(this);
        if (GameServer.server) {
            GameServer.PlayWorldSoundServer("FallingTree", this.square, 70.0f, -1);
            this.dropWood();
            this.reset();
            CellLoader.isoTreeCache.push(this);
            return;
        }
        if (owner != null) {
            owner.getEmitter().playSound("FallingTree");
        }
        this.square.RecalcAllWithNeighbours(true);
        this.dropWood();
        this.reset();
        CellLoader.isoTreeCache.push(this);
        for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
            LosUtil.cachecleared[pn] = true;
        }
        IsoGridSquare.setRecalcLightTime(-1.0f);
        GameTime.instance.lightSourceUpdate = 100.0f;
        LuaEventManager.triggerEvent("OnContainerUpdate");
    }

    public int getLogYield() {
        return this.logYield;
    }

    public static class TreeShader {
        public static final TreeShader instance = new TreeShader();
        private ShaderProgram shaderProgram;
        private int stepSize;
        private int outlineColor;
        private int chunkDepth;
        private int zDepth;

        public void initShader() {
            this.shaderProgram = ShaderProgram.createShaderProgram("tree", false, false, true);
            if (this.shaderProgram.isCompiled()) {
                this.stepSize = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "stepSize");
                this.outlineColor = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "outlineColor");
                this.chunkDepth = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "chunkDepth");
                this.zDepth = GL20.glGetUniformLocation(this.shaderProgram.getShaderID(), "zDepth");
                ShaderHelper.glUseProgramObjectARB(this.shaderProgram.getShaderID());
                GL20.glUniform2f(this.stepSize, 0.001f, 0.001f);
                ShaderHelper.glUseProgramObjectARB(0);
            }
        }

        public void setOutlineColor(float r, float g, float b, float a) {
            SpriteRenderer.instance.ShaderUpdate4f(this.shaderProgram.getShaderID(), this.outlineColor, r, g, b, a);
        }

        public void setStepSize(float stepSize, int texWidth, int texHeight) {
            SpriteRenderer.instance.ShaderUpdate2f(this.shaderProgram.getShaderID(), this.stepSize, stepSize / (float)texWidth, stepSize / (float)texHeight);
        }

        public void setDepth(float chunkDepth, float zDepth) {
            SpriteRenderer.instance.ShaderUpdate1f(this.shaderProgram.getShaderID(), this.chunkDepth, chunkDepth);
            SpriteRenderer.instance.ShaderUpdate1f(this.shaderProgram.getShaderID(), this.zDepth, zDepth);
        }

        public boolean StartShader() {
            if (this.shaderProgram == null) {
                RenderThread.invokeOnRenderContext(this::initShader);
            }
            if (this.shaderProgram.isCompiled()) {
                IndieGL.StartShader(this.shaderProgram.getShaderID(), 0);
                return true;
            }
            return false;
        }
    }
}

