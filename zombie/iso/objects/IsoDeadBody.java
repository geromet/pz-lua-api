/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.util.vector.Quaternion;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import zombie.FliesSound;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.SandboxOptions;
import zombie.SharedDescriptors;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.AttachedItems.AttachedItem;
import zombie.characters.AttachedItems.AttachedItems;
import zombie.characters.AttachedItems.AttachedLocationGroup;
import zombie.characters.AttachedItems.AttachedLocations;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.characters.Talker;
import zombie.characters.WornItems.BodyLocationGroup;
import zombie.characters.WornItems.BodyLocations;
import zombie.characters.WornItems.WornItem;
import zombie.characters.WornItems.WornItems;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.Shader;
import zombie.core.physics.Transform;
import zombie.core.properties.IsoObjectChange;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.BaseGrappleable;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.core.skinnedmodel.IGrappleable;
import zombie.core.skinnedmodel.IGrappleableWrapper;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.TwistableBoneTransform;
import zombie.core.skinnedmodel.visual.AnimalVisual;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IAnimalVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.debug.LogSeverity;
import zombie.input.Mouse;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.iso.CorpseCount;
import zombie.iso.IItemProvider;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.Vector2;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderShadows;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.ShadowParams;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerGUI;
import zombie.network.ServerLOS;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.network.fields.IPositional;
import zombie.network.id.IIdentifiable;
import zombie.network.id.ObjectID;
import zombie.network.id.ObjectIDManager;
import zombie.network.id.ObjectIDType;
import zombie.network.packets.INetworkPacket;
import zombie.network.statistics.data.ConnectionQueueStatistic;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ResourceLocation;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class IsoDeadBody
extends IsoMovingObject
implements Talker,
IAnimalVisual,
IHumanVisual,
IIdentifiable,
IGrappleableWrapper,
IItemProvider,
IPositional {
    private static final ArrayList<IIdentifiable> tempBodies = new ArrayList();
    private final ObjectID id = ObjectIDManager.createObjectID(ObjectIDType.DeadBody);
    public static final int MAX_ROT_STAGES = 3;
    public static final int MAX_ROT_STAGES_ANIMALS = 4;
    private static final int VISUAL_TYPE_HUMAN = 0;
    private static final int VISUAL_TYPE_ANIMAL = 1;
    private static final float ZOMBIE_SKELETON_WEIGHT = 7.0f;
    private boolean female;
    private boolean wasZombie;
    private boolean fakeDead;
    private boolean crawling;
    private final Color speakColor;
    private float speakTime;
    private int persistentOutfitId;
    private SurvivorDesc desc;
    private BaseVisual baseVisual;
    private String animalType;
    private float animalSize = 1.0f;
    private WornItems wornItems;
    private AttachedItems attachedItems;
    private float deathTime = -1.0f;
    private float reanimateTime = -1.0f;
    private IsoPlayer player;
    private boolean fallOnFront;
    private boolean killedByFall;
    private boolean wasSkeleton;
    private InventoryItem primaryHandItem;
    private InventoryItem secondaryHandItem;
    private float angle;
    private final Vector2 forwardDirection = new Vector2();
    private int zombieRotStageAtDeath = 1;
    private int animalRotStageAtDeath;
    private short characterOnlineId = (short)-1;
    public String animalAnimSet;
    public float weight;
    public String corpseItem;
    public String customName;
    public String invIcon;
    private final ShadowParams shadowParams = new ShadowParams(0.0f, 0.0f, 0.0f);
    private final BaseGrappleable grappleable;
    public boolean ragdollFall;
    private TwistableBoneTransform[] diedBoneTransforms;
    public AnimationPlayer animationPlayer;
    private boolean invalidateNextRender;
    public String rottenTexture;
    public String skelInvIcon;
    private boolean isOnHook;
    private IsoGameCharacter killedBy;
    private static final ThreadLocal<IsoZombie> tempZombie = new ThreadLocal<IsoZombie>(){

        @Override
        public IsoZombie initialValue() {
            return new IsoZombie(null);
        }
    };
    private static final ColorInfo inf = new ColorInfo();
    private DeadBodyAtlas.BodyTexture atlasTex;
    private static Texture dropShadow;
    private static final float HIT_TEST_WIDTH = 0.3f;
    private static final float HIT_TEST_HEIGHT = 0.9f;
    private static final Quaternionf _rotation;
    private static final Transform _transform;
    private static final Vector3f _UNIT_Z;
    private static final Vector3f _tempVec3f_1;
    private static final Vector3f _tempVec3f_2;
    private float burnTimer;
    public boolean speaking;
    public String sayLine = "";

    @Override
    public ObjectID getObjectID() {
        return this.id;
    }

    public long getObjectIDAsLong() {
        return this.getObjectID().getObjectID();
    }

    public static boolean isDead(short characterOnlineID) {
        float worldAgeHours = (float)GameTime.getInstance().getWorldAgeHours();
        for (IIdentifiable deadBodyObj : ObjectIDType.DeadBody.getObjects()) {
            IsoDeadBody deadBody = (IsoDeadBody)deadBodyObj;
            if (deadBody.characterOnlineId != characterOnlineID || !(worldAgeHours - deadBody.deathTime < 0.1f)) continue;
            return true;
        }
        return false;
    }

    @Override
    public String getObjectName() {
        return "DeadBody";
    }

    public IsoDeadBody(IsoGameCharacter died) {
        this(died, false);
    }

    public IsoDeadBody(IsoGameCharacter died, boolean wasCorpseAlready) {
        this(died, wasCorpseAlready, true);
    }

    public IsoDeadBody(IsoGameCharacter died, boolean wasCorpseAlready, boolean bAddToSquareAndWorld) {
        super(false);
        IsoAnimal isoAnimal;
        IsoZombie zombie = Type.tryCastTo(died, IsoZombie.class);
        this.setFallOnFront(died.isFallOnFront());
        this.setKilledByFall(died.isKilledByFall());
        if (!GameClient.client && !GameServer.server && zombie != null && zombie.crawling) {
            if (!zombie.isReanimate()) {
                this.setFallOnFront(true);
            }
            this.crawling = true;
        }
        this.ragdollFall = died.isRagdollFall();
        IsoGridSquare sq = died.getCurrentSquare();
        if (sq == null && died instanceof IsoAnimal && (isoAnimal = (IsoAnimal)died).getHutch() != null) {
            sq = isoAnimal.getHutch().square;
        }
        if (died.getZ() < -32.0f) {
            DebugType.Death.error("invalid z-coordinate %.2f,%.2f,%.2f", Float.valueOf(died.getX()), Float.valueOf(died.getY()), Float.valueOf(died.getZ()));
            died.setZ(0.0f);
        }
        this.square = sq;
        this.current = sq;
        if (died instanceof IsoPlayer) {
            IsoPlayer player = (IsoPlayer)died;
            if (!died.isAnimal()) {
                player.removeSaveFile();
            }
        }
        if (bAddToSquareAndWorld && sq != null) {
            sq.getStaticMovingObjects().add(this);
        }
        if (died instanceof IsoSurvivor) {
            IsoSurvivor isoSurvivor = (IsoSurvivor)died;
            IsoWorld.instance.totalSurvivorNights += isoSurvivor.nightsSurvived;
            ++IsoWorld.instance.totalSurvivorsDead;
            if (IsoWorld.instance.survivorSurvivalRecord < isoSurvivor.nightsSurvived) {
                IsoWorld.instance.survivorSurvivalRecord = isoSurvivor.nightsSurvived;
            }
        }
        this.female = died.isFemale();
        boolean bl = this.wasZombie = zombie != null;
        if (this.wasZombie) {
            this.fakeDead = zombie.isFakeDead();
            this.wasSkeleton = zombie.isSkeleton();
        }
        this.dir = died.dir;
        this.angle = died.getAnimAngleRadians();
        died.getForwardDirection(this.forwardDirection);
        this.collidable = false;
        this.setX(died.getX());
        this.setY(died.getY());
        this.setZ(died.getZ());
        this.setNextX(this.getX());
        this.setNextY(this.getY());
        this.offsetX = died.offsetX;
        this.offsetY = died.offsetY;
        this.solid = false;
        this.shootable = false;
        this.characterOnlineId = died.getOnlineID();
        this.setKilledBy(died.getAttackedBy());
        this.outlineOnMouseover = true;
        if (died instanceof IsoZombie && died.getDescriptor() != null) {
            this.desc = new SurvivorDesc(died.getDescriptor());
        }
        if (died instanceof IHumanVisual) {
            IHumanVisual iHumanVisual = (IHumanVisual)((Object)died);
            this.baseVisual = new HumanVisual(this);
            this.baseVisual.copyFrom(iHumanVisual.getHumanVisual());
            this.zombieRotStageAtDeath = this.getHumanVisual().zombieRotStage;
            this.setContainer(died.getInventory());
        }
        this.setWornItems(died.getWornItems());
        this.setAttachedItems(died.getAttachedItems());
        if (!(died instanceof IsoAnimal)) {
            died.setInventory(new ItemContainer());
        }
        died.clearWornItems();
        died.clearAttachedItems();
        if (this.container != null && !this.container.explored) {
            IsoZombie isoZombie;
            this.container.setExplored(died instanceof IsoPlayer || died instanceof IsoZombie && (isoZombie = (IsoZombie)died).isReanimatedPlayer());
        }
        boolean wasOnFire = died.isOnFire();
        this.animationPlayer = died.getAnimationPlayer();
        this.diedBoneTransforms = Pool.tryRelease(this.diedBoneTransforms);
        if (died.canUseCurrentPoseForCorpse()) {
            int numberOfBones = this.animationPlayer.getNumBones();
            this.diedBoneTransforms = TwistableBoneTransform.allocArray(numberOfBones);
            for (int boneIndex = 0; boneIndex < numberOfBones; ++boneIndex) {
                this.animationPlayer.getBoneTransformAt(boneIndex, this.diedBoneTransforms[boneIndex]);
            }
        }
        if (died instanceof IsoZombie) {
            this.persistentOutfitId = died.getPersistentOutfitID();
            if (!wasCorpseAlready && !GameServer.server) {
                for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
                    IsoPlayer player = IsoPlayer.players[playerIndex];
                    if (player == null || player.reanimatedCorpse != died) continue;
                    player.reanimatedCorpse = null;
                    player.reanimatedCorpseId = -1;
                }
                if (!GameClient.client && died.emitter != null) {
                    died.emitter.tick();
                }
            }
        } else if (!(died instanceof IsoAnimal)) {
            if (died instanceof IsoSurvivor) {
                this.getCell().getSurvivorList().remove(died);
            }
            this.desc = new SurvivorDesc(died.getDescriptor());
            if (died instanceof IsoPlayer) {
                IsoPlayer isoPlayer = (IsoPlayer)died;
                this.desc.setVoicePrefix(Objects.equals(this.desc.getVoicePrefix(), "VoiceFemale") ? "FemaleZombie" : "MaleZombie");
                if (GameServer.server) {
                    this.player = isoPlayer;
                } else if (!GameClient.client && isoPlayer.isLocalPlayer()) {
                    this.player = isoPlayer;
                }
            }
        }
        LuaManager.copyTable(this.getModData(), died.getModData());
        if (died instanceof IsoAnimal) {
            IsoAnimal isoAnimal2 = (IsoAnimal)died;
            this.setAnimalData(isoAnimal2);
            if (isoAnimal2.hutch == null) {
                isoAnimal2.remove();
            }
        } else {
            died.calculateShadowParams(this.getShadowParams());
            died.removeFromWorld();
            died.removeFromSquare();
        }
        this.sayLine = died.getSayLine();
        this.speakColor = died.getSpeakColour();
        this.speakTime = died.getSpeakTime();
        this.speaking = died.isSpeaking();
        if (wasOnFire) {
            if (bAddToSquareAndWorld && sq != null && !GameClient.client && SandboxOptions.instance.fireSpread.getValue()) {
                IsoFireManager.StartFire(this.getCell(), this.getSquare(), true, 100, 500);
            }
            if (this.container != null) {
                this.container.setExplored(true);
            }
        }
        if (!wasCorpseAlready && !GameServer.server) {
            LuaEventManager.triggerEvent("OnContainerUpdate", this);
        }
        if (bAddToSquareAndWorld && !GameServer.server) {
            LuaEventManager.triggerEvent("OnDeadBodySpawn", this);
        }
        if (died instanceof IsoPlayer) {
            IsoPlayer isoPlayer = (IsoPlayer)died;
            isoPlayer.deathFinished = true;
        }
        this.deathTime = (float)GameTime.getInstance().getWorldAgeHours();
        this.setEatingZombies(died.getEatingZombies());
        if (bAddToSquareAndWorld && !this.wasZombie && !(died instanceof IsoAnimal)) {
            ArrayList<IsoMovingObject> movingObj = new ArrayList<IsoMovingObject>();
            for (int x = -2; x < 2; ++x) {
                for (int y = -2; y < 2; ++y) {
                    IsoGridSquare testSq = sq.getCell().getGridSquare(sq.x + x, sq.y + y, sq.z);
                    if (testSq == null) continue;
                    for (int i = 0; i < testSq.getMovingObjects().size(); ++i) {
                        if (!(testSq.getMovingObjects().get(i) instanceof IsoZombie)) continue;
                        movingObj.add(testSq.getMovingObjects().get(i));
                    }
                }
            }
            for (int i = 0; i < movingObj.size(); ++i) {
                ((IsoZombie)movingObj.get(i)).pathToLocationF(this.getX() + Rand.Next(-0.3f, 0.3f), this.getY() + Rand.Next(-0.3f, 0.3f), this.getZ());
                ((IsoZombie)movingObj.get((int)i)).bodyToEat = this;
            }
        }
        if (bAddToSquareAndWorld) {
            ObjectIDManager.getInstance().addObject(this);
            CorpseCount.instance.corpseAdded(this.getXi(), this.getYi(), this.getZi());
        }
        if (bAddToSquareAndWorld && !GameServer.server) {
            FliesSound.instance.corpseAdded(this.getXi(), this.getYi(), this.getZi());
            this.invalidateRenderChunkLevel(2L);
        }
        this.grappleable = new BaseGrappleable(this);
        DebugType.Death.debugln("Corpse created %s", this.getDescription());
    }

    public IsoDeadBody(IsoCell cell) {
        super(false);
        this.speakColor = Color.white;
        this.solid = false;
        this.shootable = false;
        BodyLocationGroup bodyLocationGroup = BodyLocations.getGroup("Human");
        this.wornItems = new WornItems(bodyLocationGroup);
        AttachedLocationGroup attachedLocationGroup = AttachedLocations.getGroup("Human");
        this.attachedItems = new AttachedItems(attachedLocationGroup);
        this.grappleable = new BaseGrappleable(this);
        DebugType.Death.noise("Corpse created on cell %s", this.getDescription());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{  Name:" + this.getName() + ",  ID:" + this.getID() + ",  wasZombie:" + this.wasZombie + ",  deathTime:" + this.deathTime + " }";
    }

    public BaseVisual getVisual() {
        return this.baseVisual;
    }

    @Override
    public HumanVisual getHumanVisual() {
        return Type.tryCastTo(this.baseVisual, HumanVisual.class);
    }

    @Override
    public AnimalVisual getAnimalVisual() {
        return Type.tryCastTo(this.baseVisual, AnimalVisual.class);
    }

    @Override
    public String getAnimalType() {
        return this.animalType;
    }

    @Override
    public float getAnimalSize() {
        return this.animalSize;
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
        return this.wasZombie;
    }

    @Override
    public boolean isCrawling() {
        return this.crawling;
    }

    public void setCrawling(boolean crawling) {
        this.crawling = crawling;
    }

    public boolean isFakeDead() {
        if (SandboxOptions.instance.lore.disableFakeDead.getValue() == 3) {
            this.fakeDead = false;
        }
        if (this != null && this.getSquare() != null && this.getSquare().HasStairs() && this.fakeDead) {
            this.fakeDead = false;
        }
        return this.fakeDead;
    }

    public void setFakeDead(boolean fakeDead) {
        if (fakeDead && SandboxOptions.instance.lore.disableFakeDead.getValue() == 3) {
            return;
        }
        if (this != null && this.getSquare() != null && this.getSquare().HasStairs()) {
            this.fakeDead = false;
            return;
        }
        this.fakeDead = fakeDead;
    }

    @Override
    public boolean isSkeleton() {
        if (this.animalType != null) {
            String s;
            Object skeleton = this.getModData().rawget("skeleton");
            return skeleton instanceof String && "true".equalsIgnoreCase(s = (String)skeleton);
        }
        return this.wasSkeleton;
    }

    public void setWornItems(WornItems other) {
        for (int i = 0; i < other.size(); ++i) {
            InventoryItem item = other.get(i).getItem();
            if (item != null && item.hasTag(ItemTag.APPLY_OWNER_NAME) && this.getDescriptor() != null) {
                item.nameAfterDescriptor(this.getDescriptor());
                continue;
            }
            if (item == null || !item.hasTag(ItemTag.MONOGRAM_OWNER_NAME) || this.getDescriptor() == null) continue;
            item.monogramAfterDescriptor(this.getDescriptor());
        }
        this.wornItems = new WornItems(other);
    }

    public WornItems getWornItems() {
        return this.wornItems;
    }

    public void setAttachedItems(AttachedItems other) {
        if (other == null) {
            return;
        }
        if (this.container == null) {
            return;
        }
        this.attachedItems = new AttachedItems(other);
        for (int i = 0; i < this.attachedItems.size(); ++i) {
            AttachedItem attachedItem = this.attachedItems.get(i);
            InventoryItem item = attachedItem.getItem();
            if (this.container.contains(item) || GameClient.client || GameServer.server) continue;
            item.setContainer(this.container);
            if (item.hasTag(ItemTag.APPLY_OWNER_NAME) && this.getDescriptor() != null) {
                item.nameAfterDescriptor(this.getDescriptor());
            } else if (item.hasTag(ItemTag.MONOGRAM_OWNER_NAME) && this.getDescriptor() != null) {
                item.monogramAfterDescriptor(this.getDescriptor());
            }
            this.container.getItems().add(item);
        }
    }

    public AttachedItems getAttachedItems() {
        return this.attachedItems;
    }

    public boolean isEquipped(InventoryItem item) {
        return this.isEquippedClothing(item) || this.isHandItem(item);
    }

    public boolean isEquippedClothing(InventoryItem item) {
        return this.wornItems.contains(item);
    }

    public boolean isAttachedItem(InventoryItem item) {
        return this.getAttachedItems().contains(item);
    }

    public boolean isHandItem(InventoryItem item) {
        return this.isPrimaryHandItem(item) || this.isSecondaryHandItem(item);
    }

    public boolean isPrimaryHandItem(InventoryItem item) {
        return item != null && this.getPrimaryHandItem() == item;
    }

    public boolean isSecondaryHandItem(InventoryItem item) {
        return item != null && this.getSecondaryHandItem() == item;
    }

    public float getInventoryWeight() {
        if (this.getContainer() == null) {
            return 0.0f;
        }
        float total = 0.0f;
        ArrayList<InventoryItem> items = this.getContainer().getItems();
        for (int i = 0; i < items.size(); ++i) {
            InventoryItem item = items.get(i);
            if (item.getAttachedSlot() > -1 && !this.isEquipped(item)) {
                total += item.getHotbarEquippedWeight();
                continue;
            }
            if (this.isEquipped(item)) {
                total += item.getEquippedWeight();
                continue;
            }
            total += item.getUnequippedWeight();
        }
        return total;
    }

    @Override
    public InventoryItem getItem() {
        String itemType;
        String string = itemType = this.isFemale() ? "Base.CorpseFemale" : "Base.CorpseMale";
        if (this.isAnimal()) {
            itemType = "Base.CorpseAnimal";
        }
        Object item = InventoryItemFactory.CreateItem(itemType);
        if (this.isAnimal()) {
            ((InventoryItem)item).copyModData(this.getModData());
            ((InventoryItem)item).setIcon(Texture.getSharedTexture(this.invIcon));
            if (this.isAnimalSkeleton()) {
                ((InventoryItem)item).setName(Translator.getText("IGUI_Item_AnimalSkeleton", this.customName));
            } else {
                ((InventoryItem)item).setName(Translator.getText("IGUI_Item_AnimalCorpse", this.customName));
            }
            ((InventoryItem)item).setCustomName(true);
            ((InventoryItem)item).setActualWeight(this.weight);
            ((InventoryItem)item).setWeight(this.weight);
            ((InventoryItem)item).setCustomWeight(true);
            ((InventoryItem)item).setAge(this.getInitialItemAge((InventoryItem)item));
        } else if (this.isSkeleton()) {
            ((InventoryItem)item).setActualWeight(7.0f);
            ((InventoryItem)item).setWeight(7.0f);
            ((InventoryItem)item).setCustomWeight(true);
        }
        ((InventoryItem)item).storeInByteData(this);
        ((InventoryItem)item).deadBodyObject = this;
        if (this.id.getObjectID() != -1L) {
            ((InventoryItem)item).id = this.id.hashCode();
        }
        return item;
    }

    public float getInitialItemAge(InventoryItem item) {
        Double deathAge;
        if (this.getAnimalVisual().animalRotStage == 1) {
            return item.getOffAgeMax();
        }
        Object object = this.getModData().rawget("deathAge");
        if (object instanceof Double && (deathAge = (Double)object) > 24.0) {
            return item.getOffAge();
        }
        return 0.0f;
    }

    public float getDeathTime() {
        return this.deathTime;
    }

    public void setDeathTime(float worldAgeHours) {
        this.deathTime = worldAgeHours;
    }

    private String getDeadAnimalIcon(IsoAnimal dead) {
        if (dead.isBaby()) {
            return dead.getBreed().invIconBabyDead;
        }
        if (dead.isFemale()) {
            return dead.getBreed().invIconFemaleDead;
        }
        return dead.getBreed().invIconMaleDead;
    }

    private IsoSprite loadSprite(ByteBuffer input) {
        String tex = GameWindow.ReadString(input);
        float r = input.getFloat();
        float g = input.getFloat();
        float b = input.getFloat();
        float a = input.getFloat();
        return null;
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        boolean bServer;
        block20: {
            super.load(input, worldVersion, isDebugSave);
            this.female = input.get() != 0;
            boolean bl = this.wasZombie = input.get() != 0;
            if (input.get() != 0) {
                this.animalType = GameWindow.ReadString(input);
                AnimalDefinitions adef = AnimalDefinitions.getDef(this.animalType);
                if (adef != null) {
                    this.animalAnimSet = adef.animset;
                }
                this.animalSize = input.getFloat();
                this.customName = GameWindow.ReadString(input);
                this.corpseItem = GameWindow.ReadString(input);
                this.weight = input.getFloat();
                this.invIcon = GameWindow.ReadString(input);
                this.shadowParams.bm = input.getFloat();
                this.shadowParams.fm = input.getFloat();
                this.shadowParams.w = input.getFloat();
            } else {
                this.shadowParams.set(0.0f, 0.0f, 0.0f);
            }
            if (worldVersion >= 199) {
                this.id.load(input);
            } else {
                input.getShort();
            }
            bServer = input.get() != 0;
            this.persistentOutfitId = input.getInt();
            if (input.get() != 0) {
                this.desc = new SurvivorDesc(true);
                this.desc.load(input, worldVersion, null);
            }
            byte visualType = input.get();
            switch (visualType) {
                case 0: {
                    this.baseVisual = new HumanVisual(this);
                    this.baseVisual.load(input, worldVersion);
                    break;
                }
                case 1: {
                    this.baseVisual = new AnimalVisual(this);
                    this.baseVisual.load(input, worldVersion);
                    break;
                }
                default: {
                    throw new IOException("invalid visualType for corpse");
                }
            }
            if (input.get() != 0) {
                int con = input.getInt();
                try {
                    this.setContainer(new ItemContainer());
                    this.container.id = con;
                    ArrayList<InventoryItem> savedItems = this.container.load(input, worldVersion);
                    int wornItemCount = input.get();
                    for (int i = 0; i < wornItemCount; ++i) {
                        ItemBodyLocation itemBodyLocation = ItemBodyLocation.get(ResourceLocation.of(GameWindow.ReadString(input)));
                        short index = input.getShort();
                        if (index < 0 || index >= savedItems.size() || this.wornItems.getBodyLocationGroup().getLocation(itemBodyLocation) == null) continue;
                        this.wornItems.setItem(itemBodyLocation, savedItems.get(index));
                    }
                    int attachedItemCount = input.get();
                    for (int i = 0; i < attachedItemCount; ++i) {
                        String location = GameWindow.ReadString(input);
                        short index = input.getShort();
                        if (index < 0 || index >= savedItems.size() || this.attachedItems.getGroup().getLocation(location) == null) continue;
                        this.attachedItems.setItem(location, savedItems.get(index));
                    }
                }
                catch (Exception ex) {
                    if (this.container == null) break block20;
                    DebugType.Death.error("Failed to stream in container ID: " + this.container.id);
                }
            }
        }
        this.deathTime = input.getFloat();
        this.reanimateTime = input.getFloat();
        byte flags = input.get();
        this.fallOnFront = (flags & 1) != 0;
        boolean bl = this.killedByFall = (flags & 2) != 0;
        if (bServer && (GameClient.client || GameServer.server && ServerGUI.isCreated())) {
            this.checkClothing(null);
        }
        this.wasSkeleton = input.get() != 0;
        this.angle = input.getFloat();
        this.zombieRotStageAtDeath = input.get() & 0xFF;
        if (worldVersion >= 222) {
            this.animalRotStageAtDeath = input.get() & 0xFF;
        }
        if (worldVersion >= 225) {
            this.rottenTexture = GameWindow.ReadString(input);
            this.skelInvIcon = GameWindow.ReadString(input);
        }
        this.crawling = input.get() != 0;
        this.fakeDead = input.get() != 0;
        boolean bl2 = this.ragdollFall = input.get() != 0;
        if (this.ragdollFall) {
            int boneCount = input.getInt();
            this.diedBoneTransforms = TwistableBoneTransform.allocArray(boneCount);
            for (int i = 0; i < boneCount; ++i) {
                int boneID = input.getInt();
                org.lwjgl.util.vector.Vector3f position = new org.lwjgl.util.vector.Vector3f();
                position.x = input.getFloat();
                position.y = input.getFloat();
                position.z = input.getFloat();
                Quaternion quaternion = new Quaternion();
                quaternion.x = input.getFloat();
                quaternion.y = input.getFloat();
                quaternion.z = input.getFloat();
                quaternion.w = input.getFloat();
                org.lwjgl.util.vector.Vector3f scale = new org.lwjgl.util.vector.Vector3f();
                scale.x = input.getFloat();
                scale.y = input.getFloat();
                scale.z = input.getFloat();
                this.diedBoneTransforms[boneID].set(position, quaternion, scale);
            }
        }
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        super.save(output, isDebugSave);
        output.put(this.female ? (byte)1 : 0);
        output.put(this.wasZombie ? (byte)1 : 0);
        if (this.isAnimal()) {
            output.put((byte)1);
            GameWindow.WriteString(output, this.animalType);
            output.putFloat(this.animalSize);
            GameWindow.WriteString(output, this.customName);
            GameWindow.WriteString(output, this.corpseItem);
            output.putFloat(this.weight);
            GameWindow.WriteString(output, this.invIcon);
            output.putFloat(this.shadowParams.bm);
            output.putFloat(this.shadowParams.fm);
            output.putFloat(this.shadowParams.w);
        } else {
            output.put((byte)0);
        }
        this.getObjectID().save(output);
        if (GameServer.server || GameClient.client) {
            output.put((byte)1);
        } else {
            output.put((byte)0);
        }
        output.putInt(this.persistentOutfitId);
        if (this.desc != null) {
            output.put((byte)1);
            this.desc.save(output);
        } else {
            output.put((byte)0);
        }
        if (this.baseVisual instanceof HumanVisual) {
            output.put((byte)0);
        } else if (this.baseVisual instanceof AnimalVisual) {
            output.put((byte)1);
        } else {
            throw new IllegalStateException("unhandled baseVisual class");
        }
        this.baseVisual.save(output);
        if (this.container != null) {
            output.put((byte)1);
            output.putInt(this.container.id);
            ArrayList<InventoryItem> savedItems = this.container.save(output);
            if (this.wornItems.size() > 127) {
                throw new RuntimeException("too many worn items");
            }
            output.put((byte)this.wornItems.size());
            this.wornItems.forEach(wornItem -> {
                GameWindow.WriteString(output, wornItem.getLocation().toString());
                output.putShort((short)savedItems.indexOf(wornItem.getItem()));
            });
            if (this.attachedItems == null) {
                output.put((byte)0);
            } else {
                if (this.attachedItems.size() > 127) {
                    throw new RuntimeException("too many attached items");
                }
                output.put((byte)this.attachedItems.size());
                this.attachedItems.forEach(attachedItem -> {
                    GameWindow.WriteString(output, attachedItem.getLocation());
                    output.putShort((short)savedItems.indexOf(attachedItem.getItem()));
                });
            }
        } else {
            output.put((byte)0);
        }
        output.putFloat(this.deathTime);
        output.putFloat(this.reanimateTime);
        byte flags = 0;
        if (this.fallOnFront) {
            flags = (byte)(flags | 1);
        }
        if (this.killedByFall) {
            flags = (byte)(flags | 2);
        }
        output.put(flags);
        output.put(this.isSkeleton() ? (byte)1 : 0);
        output.putFloat(this.angle);
        output.put((byte)this.zombieRotStageAtDeath);
        output.put((byte)this.animalRotStageAtDeath);
        GameWindow.WriteString(output, this.rottenTexture);
        GameWindow.WriteString(output, this.skelInvIcon);
        output.put(this.crawling ? (byte)1 : 0);
        output.put(this.fakeDead ? (byte)1 : 0);
        output.put(this.ragdollFall ? (byte)1 : 0);
        if (this.ragdollFall) {
            org.lwjgl.util.vector.Vector3f pos = new org.lwjgl.util.vector.Vector3f();
            Quaternion rot = new Quaternion();
            org.lwjgl.util.vector.Vector3f scale = new org.lwjgl.util.vector.Vector3f();
            int boneCount = PZArrayUtil.lengthOf(this.diedBoneTransforms);
            output.putInt(boneCount);
            for (int boneIdx = 0; boneIdx < boneCount; ++boneIdx) {
                TwistableBoneTransform bone = this.diedBoneTransforms[boneIdx];
                bone.getPRS(pos, rot, scale);
                output.putInt(boneIdx);
                output.putFloat(pos.x);
                output.putFloat(pos.y);
                output.putFloat(pos.z);
                output.putFloat(rot.x);
                output.putFloat(rot.y);
                output.putFloat(rot.z);
                output.putFloat(rot.w);
                output.putFloat(scale.x);
                output.putFloat(scale.y);
                output.putFloat(scale.z);
            }
        }
    }

    @Override
    public void softReset() {
        this.square.RemoveTileObject(this);
    }

    @Override
    public void saveChange(IsoObjectChange change, KahluaTable tbl, ByteBufferWriter bb) {
        if (change == IsoObjectChange.BECOME_SKELETON) {
            bb.putInt(this.getHumanVisual().getSkinTextureIndex());
        } else if (change == IsoObjectChange.ZOMBIE_ROT_STAGE) {
            bb.putInt(this.getHumanVisual().zombieRotStage);
        } else if (change == IsoObjectChange.OBJECT_ID) {
            this.id.save(bb.bb);
        } else {
            super.saveChange(change, tbl, bb);
        }
    }

    @Override
    public void loadChange(IsoObjectChange change, ByteBufferReader bb) {
        if (change == IsoObjectChange.BECOME_SKELETON) {
            int skinTextureIndex = bb.getInt();
            this.getHumanVisual().setBeardModel("");
            this.getHumanVisual().setHairModel("");
            this.getHumanVisual().setSkinTextureIndex(skinTextureIndex);
            this.wasSkeleton = true;
            this.getWornItems().clear();
            this.getAttachedItems().clear();
            this.getContainer().clear();
            this.atlasTex = null;
        } else if (change == IsoObjectChange.ZOMBIE_ROT_STAGE) {
            this.getHumanVisual().zombieRotStage = bb.getInt();
            this.atlasTex = null;
        } else if (change == IsoObjectChange.OBJECT_ID) {
            if (this.id.getObjectID() != -1L) {
                ObjectIDManager.getInstance().remove(this.getObjectID());
            }
            this.id.load(bb.bb);
            ObjectIDManager.getInstance().addObject(this);
        } else {
            super.loadChange(change, bb);
        }
    }

    @Override
    public void renderlast() {
        if (this.speaking) {
            float sx = this.sx;
            float sy = this.sy;
            sx -= IsoCamera.getOffX();
            sy -= IsoCamera.getOffY();
            sx += 8.0f;
            sy += 32.0f;
            if (this.sayLine != null) {
                TextManager.instance.DrawStringCentre(UIFont.Medium, sx, sy, this.sayLine, this.speakColor.r, this.speakColor.g, this.speakColor.b, this.speakColor.a);
            }
        }
    }

    public DeadBodyAtlas.BodyTexture getAtlasTexture() {
        return this.atlasTex;
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoChild, boolean bWallLightingPass, Shader shader) {
        if (!this.getDoRender()) {
            return;
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        this.offsetX = 0.0f;
        this.offsetY = 0.0f;
        boolean highlighted = this.isHighlighted(playerIndex);
        if (ModelManager.instance.debugEnableModels && ModelManager.instance.isCreated()) {
            if (PerformanceSettings.fboRenderChunk && DebugOptions.instance.fboRenderChunk.corpsesInChunkTexture.getValue()) {
                return;
            }
            if (this.atlasTex == null) {
                this.atlasTex = DeadBodyAtlas.instance.getBodyTexture(this);
                DeadBodyAtlas.instance.render();
            }
            if (this.atlasTex != null) {
                if (IsoSprite.globalOffsetX == -1.0f) {
                    IsoSprite.globalOffsetX = -IsoCamera.frameState.offX;
                    IsoSprite.globalOffsetY = -IsoCamera.frameState.offY;
                }
                float ssx = IsoUtils.XToScreen(x, y, z, 0);
                float ssy = IsoUtils.YToScreen(x, y, z, 0);
                float globalOffsetX = IsoSprite.globalOffsetX;
                float globalOffsetY = IsoSprite.globalOffsetY;
                this.sx = ssx;
                this.sy = ssy;
                ssx = this.sx + globalOffsetX;
                ssy = this.sy + globalOffsetY;
                if (PerformanceSettings.fboRenderChunk) {
                    ssx += IsoCamera.cameras[playerIndex].fixJigglyModelsX * IsoCamera.frameState.zoom;
                    ssy += IsoCamera.cameras[playerIndex].fixJigglyModelsY * IsoCamera.frameState.zoom;
                }
                if (Core.tileScale == 1) {
                    // empty if block
                }
                if (highlighted) {
                    IsoDeadBody.inf.r = this.getHighlightColor((int)playerIndex).r;
                    IsoDeadBody.inf.g = this.getHighlightColor((int)playerIndex).g;
                    IsoDeadBody.inf.b = this.getHighlightColor((int)playerIndex).b;
                    IsoDeadBody.inf.a = this.getHighlightColor((int)playerIndex).a;
                } else {
                    IsoDeadBody.inf.r = col.r;
                    IsoDeadBody.inf.g = col.g;
                    IsoDeadBody.inf.b = col.b;
                    IsoDeadBody.inf.a = col.a;
                }
                col = inf;
                if (!highlighted && this.getCurrentSquare() != null) {
                    this.getCurrentSquare().interpolateLight(col, x - (float)this.getCurrentSquare().getX(), y - (float)this.getCurrentSquare().getY());
                }
                if (FBORenderCell.instance.isBlackedOutBuildingSquare(this.getSquare())) {
                    inf.set(0.0f, 0.0f, 0.0f, inf.getA());
                }
                if (GameServer.server && ServerGUI.isCreated()) {
                    inf.set(1.0f, 1.0f, 1.0f, 1.0f);
                }
                if (DebugOptions.instance.fboRenderChunk.nolighting.getValue() && !highlighted) {
                    inf.set(1.0f, 1.0f, 1.0f, 1.0f);
                }
                this.atlasTex.render(x, y, z, ssx, ssy, col.r, col.g, col.b, col.a);
                if (Core.debug && DebugOptions.instance.deadBodyAtlas.render.getValue()) {
                    if (PerformanceSettings.fboRenderChunk) {
                        IndieGL.disableDepthTest();
                        IndieGL.StartShader(0);
                    } else {
                        IndieGL.glBlendFunc(770, 771);
                    }
                    LineDrawer.DrawIsoLine(x - 0.5f, y, z, x + 0.5f, y, z, 1.0f, 1.0f, 1.0f, 0.25f, 1);
                    LineDrawer.DrawIsoLine(x, y - 0.5f, z, x, y + 0.5f, z, 1.0f, 1.0f, 1.0f, 0.25f, 1);
                }
                this.sx = ssx;
                this.sy = ssy;
                if (IsoObjectPicker.Instance.wasDirty) {
                    this.renderObjectPicker(this.getX(), this.getY(), this.getZ(), col);
                }
            }
        }
        if (Core.debug && DebugOptions.instance.deadBodyAtlas.render.getValue()) {
            _rotation.setAngleAxis((double)this.angle + 1.5707963267948966, 0.0, 0.0, 1.0);
            _transform.setRotation(_rotation);
            IsoDeadBody._transform.origin.set(this.getX(), this.getY(), this.getZ());
            Vector3f forward = _tempVec3f_1;
            IsoDeadBody._transform.basis.getColumn(1, forward);
            Vector3f perp = _tempVec3f_2;
            forward.cross(_UNIT_Z, perp);
            float w = 0.3f;
            float h = 0.9f;
            forward.x *= 0.9f;
            forward.y *= 0.9f;
            perp.x *= 0.3f;
            perp.y *= 0.3f;
            float fx = x + forward.x;
            float fy = y + forward.y;
            float bx = x - forward.x;
            float by = y - forward.y;
            float fx1 = fx - perp.x;
            float fx2 = fx + perp.x;
            float bx1 = bx - perp.x;
            float bx2 = bx + perp.x;
            float by1 = by - perp.y;
            float by2 = by + perp.y;
            float fy1 = fy - perp.y;
            float fy2 = fy + perp.y;
            float r = 1.0f;
            float g = 1.0f;
            float b = 1.0f;
            if (this.isMouseOver(Mouse.getX(), Mouse.getY())) {
                DeadBodyAtlas.instance.invalidateBodyTexture(this.atlasTex, this);
                b = 0.0f;
                r = 0.0f;
            }
            LineDrawer.addLine(fx1, fy1, this.getZ(), fx2, fy2, this.getZ(), r, 1.0f, b, null, true);
            LineDrawer.addLine(fx1, fy1, this.getZ(), bx1, by1, this.getZ(), r, 1.0f, b, null, true);
            LineDrawer.addLine(fx2, fy2, this.getZ(), bx2, by2, this.getZ(), r, 1.0f, b, null, true);
            LineDrawer.addLine(bx1, by1, this.getZ(), bx2, by2, this.getZ(), r, 1.0f, b, null, true);
            LineDrawer.addLine(this.getX(), this.getY(), this.getZ(), this.getX(), this.getY(), PZMath.fastfloor(this.getZ()), r, 1.0f, b, null, true);
        }
        if (this.isFakeDead() && DebugOptions.instance.zombieRenderFakeDead.getValue()) {
            float sx = IsoUtils.XToScreen(x, y, z, 0) + IsoSprite.globalOffsetX;
            float sy = IsoUtils.YToScreen(x, y, z, 0) + IsoSprite.globalOffsetY - (float)(16 * Core.tileScale);
            float delay = this.getFakeDeadWakeupHours() - (float)GameTime.getInstance().getWorldAgeHours();
            delay = Math.max(delay, 0.0f);
            TextManager.instance.DrawStringCentre(UIFont.Medium, sx, sy, String.format("FakeDead %.2f", Float.valueOf(delay)), 1.0, 1.0, 1.0, 1.0);
        }
        if (Core.debug) {
            this.renderDebugData();
        }
    }

    public void renderShadow() {
        if (this.invalidateNextRender) {
            this.invalidateCorpse();
            this.invalidateNextRender = false;
        }
        _rotation.setAngleAxis((double)this.angle + 4.71238898038469, 0.0, 0.0, 1.0);
        _transform.setRotation(_rotation);
        IsoDeadBody._transform.origin.set(this.getX(), this.getY(), this.getZ());
        Vector3f forward = _tempVec3f_1;
        IsoDeadBody._transform.basis.getColumn(1, forward);
        ShadowParams shadowParams = this.shadowParams;
        if (!(this.atlasTex == null || this.isAnimal() || !ModelManager.instance.debugEnableModels || !ModelManager.instance.isCreated() || PerformanceSettings.fboRenderChunk && DebugOptions.instance.fboRenderChunk.corpsesInChunkTexture.getValue())) {
            shadowParams = this.atlasTex.getShadowParams();
        }
        float w = 0.45f;
        float fm = 1.4f;
        float bm = 1.125f;
        if (shadowParams.bm > 0.0f) {
            bm = shadowParams.bm * this.animalSize;
        }
        if (shadowParams.fm > 0.0f) {
            fm = shadowParams.fm * this.animalSize;
        }
        if (shadowParams.w > 0.0f) {
            w = shadowParams.w * this.animalSize;
        }
        float skeletonAlpha = this.isSkeleton() ? 0.5f : 1.0f;
        int playerIndex = IsoCamera.frameState.playerIndex;
        ColorInfo lightInfo = this.square.lighting[playerIndex].lightInfo();
        if (PerformanceSettings.fboRenderChunk) {
            FBORenderShadows.getInstance().addShadow(this.getX(), this.getY(), this.getZ(), forward, w, fm, bm, lightInfo.r, lightInfo.g, lightInfo.b, this.getAlpha(playerIndex) * skeletonAlpha, this.isAnimal());
            return;
        }
        IsoDeadBody.renderShadow(this.getX(), this.getY(), this.getZ(), forward, w, fm, bm, lightInfo, this.getAlpha(playerIndex) * skeletonAlpha, this.isAnimal());
    }

    public static void renderShadow(float x, float y, float z, Vector3f forward, float w, float fm, float bm, ColorInfo lightInfo, float alpha) {
        IsoDeadBody.renderShadow(x, y, z, forward, w, fm, bm, lightInfo, alpha, false);
    }

    public static void renderShadow(float x, float y, float z, Vector3f forward, float w, float fm, float bm, ColorInfo lightInfo, float alpha, boolean isAnimal) {
        float shadowAlpha = alpha;
        shadowAlpha *= (lightInfo.r + lightInfo.g + lightInfo.b) / 3.0f;
        shadowAlpha *= 0.66f;
        forward.normalize();
        Vector3f perp = _tempVec3f_2;
        forward.cross(_UNIT_Z, perp);
        if (!isAnimal) {
            w = Math.max(0.65f, w);
            fm = Math.max(fm, 0.65f);
            bm = Math.max(bm, 0.65f);
        }
        perp.x *= w;
        perp.y *= w;
        float fx = x + forward.x * fm;
        float fy = y + forward.y * fm;
        float bx = x - forward.x * bm;
        float by = y - forward.y * bm;
        float fx1 = fx - perp.x;
        float fx2 = fx + perp.x;
        float bx1 = bx - perp.x;
        float bx2 = bx + perp.x;
        float by1 = by - perp.y;
        float by2 = by + perp.y;
        float fy1 = fy - perp.y;
        float fy2 = fy + perp.y;
        float x1 = IsoUtils.XToScreenExact(fx1, fy1, z, 0);
        float y1 = IsoUtils.YToScreenExact(fx1, fy1, z, 0);
        float x2 = IsoUtils.XToScreenExact(fx2, fy2, z, 0);
        float y2 = IsoUtils.YToScreenExact(fx2, fy2, z, 0);
        float x3 = IsoUtils.XToScreenExact(bx2, by2, z, 0);
        float y3 = IsoUtils.YToScreenExact(bx2, by2, z, 0);
        float x4 = IsoUtils.XToScreenExact(bx1, by1, z, 0);
        float y4 = IsoUtils.YToScreenExact(bx1, by1, z, 0);
        if (dropShadow == null) {
            dropShadow = Texture.getSharedTexture("media/textures/NewShadow.png");
        }
        SpriteRenderer.instance.renderPoly(dropShadow, x1, y1, x2, y2, x3, y3, x4, y4, 0.0f, 0.0f, 0.0f, shadowAlpha);
        if (DebugOptions.instance.isoSprite.dropShadowEdges.getValue()) {
            LineDrawer.addLine(fx1, fy1, z, fx2, fy2, z, 1, 1, 1, null);
            LineDrawer.addLine(fx2, fy2, z, bx2, by2, z, 1, 1, 1, null);
            LineDrawer.addLine(bx2, by2, z, bx1, by1, z, 1, 1, 1, null);
            LineDrawer.addLine(bx1, by1, z, fx1, fy1, z, 1, 1, 1, null);
        }
    }

    public ShadowParams getShadowParams() {
        return this.shadowParams;
    }

    @Override
    public void renderObjectPicker(float x, float y, float z, ColorInfo lightInfo) {
        if (this.atlasTex == null) {
            return;
        }
        this.atlasTex.renderObjectPicker(this.sx, this.sy, lightInfo, this.square, this);
    }

    public boolean isMouseOver(float screenX, float screenY) {
        _rotation.setAngleAxis((double)this.angle + 1.5707963267948966, 0.0, 0.0, 1.0);
        _transform.setRotation(_rotation);
        IsoDeadBody._transform.origin.set(this.getX(), this.getY(), this.getZ());
        _transform.inverse();
        Vector3f localPos = _tempVec3f_1.set(IsoUtils.XToIso(screenX, screenY, this.getZ()), IsoUtils.YToIso(screenX, screenY, this.getZ()), this.getZ());
        _transform.transform(localPos);
        return localPos.x >= -0.3f && localPos.y >= -0.9f && localPos.x < 0.3f && localPos.y < 0.9f;
    }

    public Vector2f getGrabHeadPosition(Vector2f out) {
        _rotation.setAngleAxis((double)this.angle + 1.5707963267948966, 0.0, 0.0, 1.0);
        _transform.setRotation(_rotation);
        IsoDeadBody._transform.origin.set(this.getX(), this.getY(), this.getZ());
        Vector3f forward = _tempVec3f_1;
        IsoDeadBody._transform.basis.getColumn(1, forward);
        float h = 0.9f;
        forward.x *= 0.9f;
        forward.y *= 0.9f;
        float fx = this.getX() + forward.x;
        float fy = this.getY() + forward.y;
        float bx = this.getX() - forward.x;
        float by = this.getY() - forward.y;
        return this.isFallOnFront() ? out.set(bx, by) : out.set(fx, fy);
    }

    public Vector2f getGrabLegsPosition(Vector2f out) {
        _rotation.setAngleAxis((double)this.angle + 1.5707963267948966, 0.0, 0.0, 1.0);
        _transform.setRotation(_rotation);
        IsoDeadBody._transform.origin.set(this.getX(), this.getY(), this.getZ());
        Vector3f forward = _tempVec3f_1;
        IsoDeadBody._transform.basis.getColumn(1, forward);
        float h = 0.9f;
        forward.x *= 0.9f;
        forward.y *= 0.9f;
        float fx = this.getX() + forward.x;
        float fy = this.getY() + forward.y;
        float bx = this.getX() - forward.x;
        float by = this.getY() - forward.y;
        return this.isFallOnFront() ? out.set(fx, fy) : out.set(bx, by);
    }

    public void Burn() {
        if (GameClient.client) {
            return;
        }
        if (this.getSquare() != null && this.getSquare().getProperties().has(IsoFlagType.burning)) {
            this.burnTimer += GameTime.instance.getMultipliedSecondsSinceLastUpdate();
        } else {
            return;
        }
        if (this.burnTimer >= 10.0f) {
            ConnectionQueueStatistic.getInstance().burnedCorpsesToday.increase();
            boolean addBurnedTile = true;
            for (int i = 0; i < this.getSquare().getObjects().size(); ++i) {
                IsoObject obj = this.getSquare().getObjects().get(i);
                if (obj.getName() == null || !"burnedCorpse".equals(obj.getName())) continue;
                addBurnedTile = false;
                break;
            }
            if (addBurnedTile) {
                IsoObject burnedCorpse = new IsoObject(this.getSquare(), "floors_burnt_01_" + Rand.Next(1, 3), "burnedCorpse");
                this.getSquare().getObjects().add(burnedCorpse);
                burnedCorpse.transmitCompleteItemToClients();
            }
            if (GameServer.server) {
                INetworkPacket.sendToAll(PacketTypes.PacketType.RemoveCorpseFromMap, this);
            }
            this.getSquare().removeCorpse(this, true);
        }
    }

    @Override
    public void setContainer(ItemContainer container) {
        super.setContainer(container);
        container.type = this.female ? "inventoryfemale" : "inventorymale";
        container.capacity = 12;
        container.sourceGrid = this.square;
    }

    public void checkClothing(InventoryItem removedItem) {
        InventoryItem item;
        int i;
        for (i = 0; i < this.wornItems.size(); ++i) {
            item = this.wornItems.getItemByIndex(i);
            if (this.container != null && this.container.getItems().indexOf(item) != -1) continue;
            this.wornItems.remove(item);
            this.atlasTex = null;
            this.invalidateRenderChunkLevel(2L);
            --i;
        }
        if (removedItem == this.getPrimaryHandItem()) {
            this.setPrimaryHandItem(null);
            this.atlasTex = null;
            this.invalidateRenderChunkLevel(2L);
        }
        if (removedItem == this.getSecondaryHandItem()) {
            this.setSecondaryHandItem(null);
            this.atlasTex = null;
            this.invalidateRenderChunkLevel(2L);
        }
        for (i = 0; i < this.attachedItems.size(); ++i) {
            item = this.attachedItems.getItemByIndex(i);
            if (this.container != null && this.container.getItems().indexOf(item) != -1) continue;
            this.attachedItems.remove(item);
            this.atlasTex = null;
            this.invalidateRenderChunkLevel(2L);
            --i;
        }
    }

    @Override
    public boolean IsSpeaking() {
        return this.speaking;
    }

    @Override
    public void Say(String line) {
        this.speakTime = line.length() * 4;
        if (this.speakTime < 60.0f) {
            this.speakTime = 60.0f;
        }
        this.sayLine = line;
        this.speaking = true;
    }

    @Override
    public String getSayLine() {
        return this.sayLine;
    }

    @Override
    public String getTalkerType() {
        return "Talker";
    }

    @Override
    public void addToWorld() {
        DesignationZoneAnimal dZone;
        super.addToWorld();
        CorpseCount.instance.corpseAdded(this.getXi(), this.getYi(), this.getZi());
        if (!GameServer.server) {
            FliesSound.instance.corpseAdded(this.getXi(), this.getYi(), this.getZi());
        }
        ObjectIDManager.getInstance().addObject(this);
        if (this.isAnimal() && (dZone = DesignationZoneAnimal.getZoneF(this.getX(), this.getY(), this.getZ())) != null) {
            dZone.addCorpse(this);
        }
        if (GameClient.client) {
            return;
        }
        if (this.reanimateTime > 0.0f) {
            this.getCell().addToStaticUpdaterObjectList(this);
            DebugType.Death.debugln("reanimate: addToWorld reanimateTime=" + this.reanimateTime + String.valueOf(this));
        }
        float worldAge = (float)GameTime.getInstance().getWorldAgeHours();
        if (this.deathTime < 0.0f) {
            this.deathTime = worldAge;
        }
        if (this.deathTime > worldAge) {
            this.deathTime = worldAge;
        }
    }

    @Override
    public void removeFromWorld() {
        DesignationZoneAnimal dZone;
        CorpseCount.instance.corpseRemoved(this.getXi(), this.getYi(), this.getZi());
        if (!GameServer.server) {
            FliesSound.instance.corpseRemoved(this.getXi(), this.getYi(), this.getZi());
        }
        if (this.isAnimal() && (dZone = DesignationZoneAnimal.getZoneF(this.getX(), this.getY(), this.getZ())) != null) {
            dZone.removeCorpse(this);
        }
        ObjectIDManager.getInstance().remove(this.id);
        this.diedBoneTransforms = Pool.tryRelease(this.diedBoneTransforms);
        super.removeFromWorld();
    }

    public static void updateBodies() {
        if (GameClient.client) {
            return;
        }
        float hoursForCorpseRemoval = (float)SandboxOptions.instance.hoursForCorpseRemoval.getValue();
        if (hoursForCorpseRemoval <= 0.0f) {
            return;
        }
        float hoursPerRotStage = hoursForCorpseRemoval / 3.0f;
        float worldAge = (float)GameTime.getInstance().getWorldAgeHours();
        tempBodies.clear();
        Collection<IIdentifiable> values2 = ObjectIDType.DeadBody.getObjects();
        for (IIdentifiable ii : values2) {
            tempBodies.add(ii);
        }
        for (int i = 0; i < tempBodies.size(); ++i) {
            IsoDeadBody body = (IsoDeadBody)tempBodies.get(i);
            if (body.getAnimalVisual() != null) {
                hoursPerRotStage = hoursForCorpseRemoval / 4.0f;
                body.updateAnimalRotting(worldAge, hoursPerRotStage);
            }
            if (body.getHumanVisual() == null) continue;
            if (body.deathTime > worldAge) {
                body.deathTime = worldAge;
                body.getHumanVisual().zombieRotStage = body.zombieRotStageAtDeath;
            }
            if (body.updateFakeDead() || !ServerOptions.instance.removePlayerCorpsesOnCorpseRemoval.getValue() && !body.wasZombie) continue;
            int stageOld = body.getHumanVisual().zombieRotStage;
            body.updateRotting(worldAge, hoursPerRotStage);
            if (body.isFakeDead()) {
                // empty if block
            }
            int stageNew = body.getHumanVisual().zombieRotStage;
            float age = worldAge - body.deathTime;
            if (age < hoursForCorpseRemoval + (body.isSkeleton() ? hoursPerRotStage : 0.0f)) continue;
            int stages = (int)(age / hoursPerRotStage);
            DebugType.Death.noise("%s REMOVE %d -> %d age=%.2f stages=%d", body, stageOld, stageNew, Float.valueOf(age), stages);
            if (GameServer.server) {
                INetworkPacket.sendToAll(PacketTypes.PacketType.RemoveCorpseFromMap, body);
            }
            body.removeFromWorld();
            body.removeFromSquare();
        }
    }

    public void changeRotStage(int newStage) {
        if (newStage <= 4) {
            this.getAnimalVisual().animalRotStage = newStage;
            this.animalRotStageAtDeath = newStage;
            this.atlasTex = null;
            this.invalidateCorpse();
            this.invalidateRenderChunkLevel(2L);
        } else {
            this.removeFromWorld();
            this.removeFromSquare();
        }
    }

    private void updateAnimalRotting(float worldAge, float hoursPerRotStage) {
        float age = worldAge - this.deathTime;
        int stages = (int)(age / hoursPerRotStage);
        int newStage = stages + this.animalRotStageAtDeath;
        this.getModData().rawset("deathAge", (Object)age);
        if (stages < 4) {
            newStage = PZMath.clamp(newStage, -1, 4);
        }
        if (newStage != this.getAnimalVisual().animalRotStage) {
            if (newStage <= 4) {
                if (newStage >= 2) {
                    this.getModData().rawset("parts", (Object)false);
                    this.getModData().rawset("skeleton", (Object)true);
                    if (!StringUtils.isNullOrEmpty(this.skelInvIcon)) {
                        this.invIcon = this.skelInvIcon;
                    }
                }
                this.getAnimalVisual().animalRotStage = newStage;
                this.getModData().rawset("animalRotStage", (Object)newStage);
                this.atlasTex = null;
                this.invalidateCorpse();
                this.invalidateRenderChunkLevel(2L);
            } else {
                this.removeFromWorld();
                this.removeFromSquare();
            }
        }
    }

    private void updateRotting(float worldAge, float hoursPerRotStage) {
        if (this.isSkeleton()) {
            return;
        }
        float age = worldAge - this.deathTime;
        int stages = (int)(age / hoursPerRotStage);
        int newStage = this.zombieRotStageAtDeath + stages;
        if (stages < 3) {
            newStage = PZMath.clamp(newStage, 1, 3);
        }
        if (newStage <= 3 && newStage != this.getHumanVisual().zombieRotStage) {
            int decaySteps = newStage - this.getHumanVisual().zombieRotStage;
            DebugType.Death.noise("%s zombieRotStage %d -> %d age=%.2f stages=%d", this, this.getHumanVisual().zombieRotStage, newStage, Float.valueOf(age), stages);
            this.getHumanVisual().zombieRotStage = newStage;
            this.atlasTex = null;
            this.invalidateRenderChunkLevel(2L);
            if (GameServer.server) {
                this.sendObjectChange(IsoObjectChange.ZOMBIE_ROT_STAGE);
            }
            if (Rand.Next(100) == 0 && this.wasZombie && SandboxOptions.instance.lore.disableFakeDead.getValue() == 2) {
                this.setFakeDead(true);
                if (Rand.Next(5) == 0) {
                    this.setCrawling(true);
                }
            }
            String season = ClimateManager.getInstance().getSeasonName();
            if (decaySteps < 1 || "Winter".equals(season)) {
                return;
            }
            if (SandboxOptions.instance.maggotSpawn.getValue() == 3) {
                return;
            }
            int maggotChance = 5;
            if ("Summer".equals(season)) {
                maggotChance = 3;
            }
            for (int i = 0; i < decaySteps; ++i) {
                Object maggot;
                if (this.wasZombie) {
                    Food food;
                    if (Rand.Next(maggotChance) == 0 && (maggot = InventoryItemFactory.CreateItem("Maggots")) != null && this.getContainer() != null) {
                        this.getContainer().addItem((InventoryItem)maggot);
                        if (maggot instanceof Food) {
                            food = (Food)maggot;
                            food.setPoisonPower(5);
                        }
                    }
                    if (Rand.Next(maggotChance * 2) != 0 || SandboxOptions.instance.maggotSpawn.getValue() == 2 || (maggot = InventoryItemFactory.CreateItem("Maggots")) == null || this.getSquare() == null) continue;
                    this.getSquare().AddWorldInventoryItem((InventoryItem)maggot, (float)(Rand.Next(10) / 10), (float)(Rand.Next(10) / 10), 0.0f);
                    if (!(maggot instanceof Food)) continue;
                    food = (Food)maggot;
                    food.setPoisonPower(5);
                    continue;
                }
                if (Rand.Next(maggotChance) == 0 && (maggot = InventoryItemFactory.CreateItem("Maggots")) != null && this.getContainer() != null) {
                    this.getContainer().addItem((InventoryItem)maggot);
                }
                if (Rand.Next(maggotChance * 2) != 0 || SandboxOptions.instance.maggotSpawn.getValue() == 2 || (maggot = InventoryItemFactory.CreateItem("Maggots")) == null || this.getSquare() == null) continue;
                this.getSquare().AddWorldInventoryItem((InventoryItem)maggot, (float)(Rand.Next(10) / 10), (float)(Rand.Next(10) / 10), 0.0f);
            }
            return;
        }
        if (stages == 3 && Rand.NextBool(7)) {
            DebugType.Death.noise("%s zombieRotStage %d -> x age=%.2f stages=%d", this, this.getHumanVisual().zombieRotStage, Float.valueOf(age), stages);
            this.getHumanVisual().setBeardModel("");
            this.getHumanVisual().setHairModel("");
            this.getHumanVisual().setSkinTextureIndex(Rand.Next(1, 3));
            this.wasSkeleton = true;
            this.getWornItems().clear();
            this.getAttachedItems().clear();
            this.getContainer().clear();
            this.atlasTex = null;
            this.invalidateRenderChunkLevel(2L);
            if (GameServer.server) {
                this.sendObjectChange(IsoObjectChange.BECOME_SKELETON);
            }
        }
    }

    private boolean updateFakeDead() {
        if (!this.isFakeDead()) {
            return false;
        }
        if (this.isSkeleton()) {
            return false;
        }
        if ((double)this.getFakeDeadWakeupHours() > GameTime.getInstance().getWorldAgeHours()) {
            return false;
        }
        if (!this.isPlayerNearby()) {
            return false;
        }
        if (SandboxOptions.instance.lore.disableFakeDead.getValue() == 3) {
            return false;
        }
        this.reanimateNow();
        return true;
    }

    private float getFakeDeadWakeupHours() {
        return this.deathTime + 0.5f;
    }

    private boolean isPlayerNearby() {
        if (GameServer.server) {
            for (int i = 0; i < GameServer.Players.size(); ++i) {
                boolean bCanSee;
                IsoPlayer player = GameServer.Players.get(i);
                boolean bl = bCanSee = this.square != null && ServerLOS.instance.isCouldSee(player, this.square);
                if (!this.isPlayerNearby(player, bCanSee)) continue;
                return true;
            }
        } else {
            IsoGridSquare square = this.getSquare();
            for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
                boolean bCanSee;
                IsoPlayer player = IsoPlayer.players[pn];
                boolean bl = bCanSee = square != null && square.isCanSee(pn);
                if (!this.isPlayerNearby(player, bCanSee)) continue;
                return true;
            }
        }
        return false;
    }

    private boolean isPlayerNearby(IsoPlayer player, boolean isCanSee) {
        if (!isCanSee) {
            return false;
        }
        if (player == null || player.isDead()) {
            return false;
        }
        if (player.isGhostMode() || player.isInvisible()) {
            return false;
        }
        if (player.getVehicle() != null) {
            return false;
        }
        float distSq = player.DistToSquared(this);
        return !(distSq < 4.0f) && !(distSq > 16.0f);
    }

    public float getReanimateTime() {
        return this.reanimateTime;
    }

    public void setReanimateTime(float hours) {
        this.reanimateTime = hours;
        if (GameClient.client) {
            return;
        }
        ArrayList<IsoObject> staticUpdaters = IsoWorld.instance.currentCell.getStaticUpdaterObjectList();
        if (this.reanimateTime > 0.0f && !staticUpdaters.contains(this)) {
            staticUpdaters.add(this);
        } else if (this.reanimateTime <= 0.0f && staticUpdaters.contains(this)) {
            staticUpdaters.remove(this);
        }
    }

    private float getReanimateDelay() {
        float min = 0.0f;
        float max = 0.0f;
        switch (SandboxOptions.instance.lore.reanimate.getValue()) {
            case 1: {
                break;
            }
            case 2: {
                max = 0.008333334f;
                break;
            }
            case 3: {
                max = 0.016666668f;
                break;
            }
            case 4: {
                max = 12.0f;
                break;
            }
            case 5: {
                min = 48.0f;
                max = 72.0f;
                break;
            }
            case 6: {
                min = 168.0f;
                max = 336.0f;
            }
        }
        if (Core.tutorial) {
            max = 0.25f;
        }
        if (min == max) {
            return min;
        }
        return Rand.Next(min, max);
    }

    public void reanimateLater() {
        this.setReanimateTime((float)GameTime.getInstance().getWorldAgeHours() + this.getReanimateDelay());
    }

    public void reanimateNow() {
        this.setReanimateTime((float)GameTime.getInstance().getWorldAgeHours());
    }

    @Override
    public void update() {
        float worldAge;
        if (this.current == null) {
            this.current = IsoWorld.instance.currentCell.getGridSquare(this.getX(), this.getY(), PZMath.floor(this.getZ()));
        }
        if (GameClient.client) {
            return;
        }
        if (this.reanimateTime > 0.0f && this.reanimateTime <= (worldAge = (float)GameTime.getInstance().getWorldAgeHours())) {
            this.reanimate();
        }
    }

    @Override
    public void Grappled(IGrappleable grappler, HandWeapon weapon, float grappleEffectiveness, String grappleType) {
        if (grappler == null) {
            DebugType.Grapple.warn("Grappler is null. Nothing to grapple us.");
            return;
        }
        if (grappleEffectiveness < 0.5f) {
            DebugType.Grapple.debugln("Effectiveness insufficient. %f. Rejecting grapple.", Float.valueOf(grappleEffectiveness));
            grappler.RejectGrapple(this);
            return;
        }
        if (!this.canBeGrappled()) {
            DebugType.Grapple.debugln("Cannot grapple. No transition available to grappled state.");
            grappler.RejectGrapple(this);
            return;
        }
        if (this.isFakeDead()) {
            DebugType.Grapple.debugln("Corpse is a fake-dead. Cannot grapple.");
            return;
        }
        IsoZombie reanimatedZombie = this.reanimateZombieForGrapple();
        if (reanimatedZombie == null) {
            DebugType.Grapple.warn("Corpse failed to reanimate. Cannot grapple.");
            return;
        }
        if (this.isSkeleton()) {
            reanimatedZombie.setSkeleton(true);
        }
        reanimatedZombie.getHumanVisual().setSkinTextureIndex(this.getHumanVisual().getSkinTextureIndex());
        DebugType.Grapple.debugln("Accepting grapple by: %s", grappler.getClass().getName());
        reanimatedZombie.Grappled(grappler, weapon, grappleEffectiveness, grappleType);
    }

    private IsoZombie reanimateZombieForGrapple() {
        if (this.isAnimal()) {
            return null;
        }
        DebugType.Grapple.debugln("Reanimating corpse for grappling.");
        IsoZombie reanimatedCorpse = (IsoZombie)this.reanimate();
        if (reanimatedCorpse == null) {
            DebugType.Grapple.warn("Failed to reanimate Zombie for grappling.");
            return null;
        }
        reanimatedCorpse.setName("ReanimatedCorpse_" + String.valueOf(this));
        reanimatedCorpse.setReanimatedForGrappleOnly(true);
        reanimatedCorpse.addFootstepParametersIfNeeded();
        reanimatedCorpse.setFakeDead(false);
        reanimatedCorpse.setCrawler(false);
        reanimatedCorpse.setOnFloor(true);
        reanimatedCorpse.setDoRender(this.getDoRender());
        return reanimatedCorpse;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public IsoGameCharacter reanimate() {
        if (this.isAnimal()) {
            return this.reanimateAnimal();
        }
        ConnectionQueueStatistic.getInstance().zombifiedPlayersToday.increase();
        int onlineID = -1;
        if (GameServer.server && (onlineID = (int)ServerMap.instance.getUniqueZombieId()) == -1) {
            return null;
        }
        SurvivorDesc survivorDesc = new SurvivorDesc();
        survivorDesc.setFemale(this.isFemale());
        if (this.desc != null) {
            survivorDesc.setVoicePrefix(this.desc.getVoicePrefix());
        }
        IsoZombie zombie = new IsoZombie(IsoWorld.instance.currentCell, survivorDesc, -1);
        zombie.setSkeleton(this.isSkeleton());
        zombie.setPersistentOutfitID(this.persistentOutfitId);
        if (this.container == null) {
            this.container = new ItemContainer();
        }
        zombie.setInventory(this.container);
        this.container = null;
        zombie.getHumanVisual().copyFrom(this.getHumanVisual());
        zombie.getWornItems().copyFrom(this.wornItems);
        this.wornItems.clear();
        zombie.getAttachedItems().copyFrom(this.attachedItems);
        this.attachedItems.clear();
        zombie.setX(this.getX());
        zombie.setY(this.getY());
        zombie.setZ(this.getZ());
        zombie.setCurrent(this.getCurrentSquare());
        zombie.setMovingSquareNow();
        zombie.setDir(this.dir);
        LuaManager.copyTable(zombie.getModData(), this.getModData());
        if (zombie.getModData().rawget("_bloodSplatAmount") != null) {
            Double blood = (Double)zombie.getModData().rawget("_bloodSplatAmount");
            zombie.bloodSplatAmount = (int)Math.min(blood, 1000.0);
        }
        zombie.getAnimationPlayer().setTargetAngle(this.angle);
        zombie.getAnimationPlayer().setAngleToTarget();
        zombie.setForwardDirection(Vector2.fromLengthDirection(1.0f, this.angle));
        zombie.setAlphaAndTarget(1.0f);
        Arrays.fill(zombie.isVisibleToPlayer, true);
        zombie.setOnFloor(true);
        zombie.setCrawler(this.crawling);
        zombie.setCanWalk(!this.crawling);
        zombie.walkVariant = "ZombieWalk";
        zombie.DoZombieStats();
        zombie.setFallOnFront(this.isFallOnFront());
        if (SandboxOptions.instance.lore.toughness.getValue() == 1) {
            zombie.setHealth(3.5f + Rand.Next(0.0f, 0.3f));
        }
        if (SandboxOptions.instance.lore.toughness.getValue() == 2) {
            zombie.setHealth(1.8f + Rand.Next(0.0f, 0.3f));
        }
        if (SandboxOptions.instance.lore.toughness.getValue() == 3) {
            zombie.setHealth(0.5f + Rand.Next(0.0f, 0.3f));
        }
        if (GameServer.server) {
            zombie.onlineId = (short)onlineID;
            ServerMap.instance.zombieMap.put(zombie.onlineId, zombie);
        }
        if (this.isFakeDead()) {
            zombie.setWasFakeDead(true);
        } else {
            zombie.setReanimatedPlayer(true);
            zombie.getDescriptor().setID(0);
            SharedDescriptors.createPlayerZombieDescriptor(zombie);
        }
        zombie.setReanimate(this.crawling);
        if (!IsoWorld.instance.currentCell.getZombieList().contains(zombie)) {
            IsoWorld.instance.currentCell.getZombieList().add(zombie);
        }
        if (!IsoWorld.instance.currentCell.getObjectList().contains(zombie) && !IsoWorld.instance.currentCell.getAddList().contains(zombie)) {
            IsoWorld.instance.currentCell.getAddList().add(zombie);
        }
        if (GameServer.server) {
            if (this.player != null) {
                this.player.reanimatedCorpse = zombie;
                this.player.reanimatedCorpseId = zombie.onlineId;
            }
            zombie.networkAi.reanimatedBodyId.set(this.id);
        }
        this.invalidateRenderChunkLevel(128L);
        this.removeFromWorld();
        this.removeFromSquare();
        LuaEventManager.triggerEvent("OnContainerUpdate");
        zombie.setReanimateTimer(0.0f);
        zombie.onWornItemsChanged();
        if (this.player != null) {
            if (GameServer.server) {
                GameServer.sendReanimatedZombieID(this.player, zombie);
            } else if (!GameClient.client && this.player.isLocalPlayer()) {
                this.player.reanimatedCorpse = zombie;
            }
            this.player.setLeaveBodyTimedown(3601.0f);
        }
        zombie.getActionContext().update();
        float fpsMultiplier = GameTime.getInstance().fpsMultiplier;
        GameTime.getInstance().fpsMultiplier = 100.0f;
        try {
            zombie.advancedAnimator.update(GameTime.getInstance().getTimeDelta());
        }
        finally {
            GameTime.getInstance().fpsMultiplier = fpsMultiplier;
        }
        if (this.isFakeDead() && SoundManager.instance.isListenerInRange(this.getX(), this.getY(), 20.0f) && !GameServer.server) {
            zombie.parameterZombieState.setState(ParameterZombieState.State.Reanimate);
        }
        zombie.neverDoneAlpha = false;
        DebugType.Death.debugln("Reanimate: corpse=%s/%d zombie=%d delay=%f", this.getObjectID().getDescription(), this.getCharacterOnlineID(), zombie.getOnlineID(), GameTime.getInstance().getWorldAgeHours() - (double)this.reanimateTime);
        return zombie;
    }

    private IsoAnimal reanimateAnimal() {
        AnimalDefinitions adef = AnimalDefinitions.getDef(this.getAnimalType());
        IsoAnimal animal = new IsoAnimal(this.getCell(), this.getXi(), this.getYi(), this.getZi(), this.getAnimalType(), adef.getBreeds().get(0));
        animal.getAnimalVisual().copyFrom(this.getAnimalVisual());
        animal.getData().setSize(this.getAnimalSize());
        animal.getData().setWeight(this.getWeight());
        animal.setShouldBeSkeleton(this.isSkeleton());
        animal.setX(this.getX());
        animal.setY(this.getY());
        animal.setZ(this.getZ());
        animal.setCurrent(this.getCurrentSquare());
        animal.setMovingSquareNow();
        animal.setDir(this.dir);
        LuaManager.copyTable(animal.getModData(), this.getModData());
        animal.getAnimationPlayer().setTargetAngle(this.angle);
        animal.getAnimationPlayer().setAngleToTarget();
        animal.setForwardDirection(Vector2.fromLengthDirection(1.0f, this.angle));
        animal.setAlphaAndTarget(1.0f);
        animal.addToWorld();
        this.invalidateRenderChunkLevel(128L);
        this.removeFromWorld();
        this.removeFromSquare();
        return animal;
    }

    public static void Reset() {
    }

    @Override
    public void Collision(Vector2 collision, IsoObject object) {
        if (object instanceof BaseVehicle) {
            BaseVehicle vehicle = (BaseVehicle)object;
            float speedCap = 15.0f;
            Vector3f velocity = (Vector3f)BaseVehicle.TL_vector3f_pool.get().alloc();
            Vector3f v = (Vector3f)BaseVehicle.TL_vector3f_pool.get().alloc();
            vehicle.getLinearVelocity(velocity);
            velocity.y = 0.0f;
            v.set(vehicle.getX() - this.getX(), 0.0f, vehicle.getZ() - this.getZ());
            v.normalize();
            velocity.mul(v);
            BaseVehicle.TL_vector3f_pool.get().release(v);
            float speed = velocity.length();
            BaseVehicle.TL_vector3f_pool.get().release(velocity);
            speed = Math.min(speed, 15.0f);
            if (speed < 0.05f) {
                return;
            }
            if (Math.abs(vehicle.getCurrentSpeedKmHour()) > 20.0f) {
                vehicle.applyImpulseFromHitCorpse(this);
            }
        }
    }

    @Override
    public boolean isFallOnFront() {
        return this.fallOnFront;
    }

    @Override
    public void setFallOnFront(boolean fallOnFront) {
        this.fallOnFront = fallOnFront;
    }

    @Override
    public boolean isKilledByFall() {
        return this.killedByFall;
    }

    @Override
    public void setKilledByFall(boolean killedByFall) {
        this.killedByFall = killedByFall;
    }

    public InventoryItem getPrimaryHandItem() {
        return this.primaryHandItem;
    }

    public void setPrimaryHandItem(InventoryItem item) {
        this.primaryHandItem = item;
        this.updateContainerWithHandItems();
    }

    private void updateContainerWithHandItems() {
        if (this.getContainer() != null) {
            if (this.getPrimaryHandItem() != null) {
                this.getContainer().AddItem(this.getPrimaryHandItem());
            }
            if (this.getSecondaryHandItem() != null) {
                this.getContainer().AddItem(this.getSecondaryHandItem());
            }
        }
    }

    public InventoryItem getSecondaryHandItem() {
        return this.secondaryHandItem;
    }

    public void setSecondaryHandItem(InventoryItem item) {
        this.secondaryHandItem = item;
        this.updateContainerWithHandItems();
    }

    public float getAngle() {
        return this.angle;
    }

    public String getOutfitName() {
        if (this.getHumanVisual().getOutfit() != null) {
            return this.getHumanVisual().getOutfit().name;
        }
        return null;
    }

    public String getDescription() {
        return "{ \"IsoDeadBody\" : { \"ObjectID\" : " + this.getObjectID().getDescription() + ", \"characterOnlineID\" : " + this.characterOnlineId + ", \"bFakeDead\" : " + this.fakeDead + ", \"bCrawling\" : " + this.crawling + ", \"fallOnFront\" : " + this.fallOnFront + ", \"x\" : " + this.getX() + ", \"y\" : " + this.getY() + ", \"z\" : " + this.getZ() + ", \"m_angle\" : " + this.angle + ", \"m_persistentOutfitID\" : " + this.persistentOutfitId + " } }";
    }

    public String readInventory(ByteBuffer b) {
        boolean hasInventory;
        String type = GameWindow.ReadString(b);
        if (this.getContainer() == null || this.getWornItems() == null || this.getAttachedItems() == null) {
            return type;
        }
        this.getContainer().clear();
        this.getWornItems().clear();
        this.getAttachedItems().clear();
        boolean bl = hasInventory = b.get() != 0;
        if (hasInventory) {
            try {
                ArrayList<InventoryItem> savedItems = this.getContainer().load(b, IsoWorld.getWorldVersion());
                this.getContainer().capacity = 8;
                int wornItemCount = b.get();
                for (int i = 0; i < wornItemCount; ++i) {
                    ItemBodyLocation itemBodyLocation = ItemBodyLocation.get(ResourceLocation.of(GameWindow.ReadString(b)));
                    short index = b.getShort();
                    if (index < 0 || index >= savedItems.size() || this.getWornItems().getBodyLocationGroup().getLocation(itemBodyLocation) == null) continue;
                    this.getWornItems().setItem(itemBodyLocation, savedItems.get(index));
                }
                int attachedItemsCount = b.get();
                for (int i = 0; i < attachedItemsCount; ++i) {
                    String location = GameWindow.ReadString(b);
                    short index = b.getShort();
                    if (index < 0 || index >= savedItems.size() || this.getAttachedItems().getGroup().getLocation(location) == null) continue;
                    this.getAttachedItems().setItem(location, savedItems.get(index));
                }
            }
            catch (IOException e) {
                DebugType.Death.printException(e, "ReadDeadBodyInventory error for dead body " + this.getCharacterOnlineID(), LogSeverity.Error);
            }
        }
        return type;
    }

    public short getCharacterOnlineID() {
        return this.characterOnlineId;
    }

    public void setCharacterOnlineID(short onlineID) {
        this.characterOnlineId = onlineID;
    }

    public boolean isPlayer() {
        return this.player != null;
    }

    public static void removeDeadBody(ObjectID id) {
        IsoDeadBody deadBody = (IsoDeadBody)id.getObject();
        if (deadBody != null) {
            ObjectIDManager.getInstance().remove(id);
            if (deadBody.getSquare() != null) {
                deadBody.getSquare().removeCorpse(deadBody, true);
            }
        }
    }

    @Override
    public IsoGridSquare getRenderSquare() {
        if (this.getSquare() == null) {
            return null;
        }
        int chunksPerWidth = 8;
        if (PZMath.coordmodulo(this.square.x, 8) == 0 && PZMath.coordmodulo(this.square.y, 8) == 7) {
            return this.square.getAdjacentSquare(IsoDirections.S);
        }
        if (PZMath.coordmodulo(this.square.x, 8) == 7 && PZMath.coordmodulo(this.square.y, 8) == 0) {
            return this.square.getAdjacentSquare(IsoDirections.E);
        }
        return this.getSquare();
    }

    public void renderDebugData() {
        IndieGL.StartShader(0);
        IndieGL.disableDepthTest();
        IndieGL.glBlendFunc(770, 771);
        TextManager.StringDrawer stringDrawer = TextManager.instance::DrawString;
        Color c = Colors.OrangeRed;
        UIFont font = UIFont.Dialogue;
        float sx = IsoUtils.XToScreenExact(this.getX() + 0.4f, this.getY() + 0.4f, this.getZ(), 0);
        float sy = IsoUtils.YToScreenExact(this.getX() + 0.4f, this.getY() - 1.4f, this.getZ(), 0);
        float dy = 0.0f;
        if (DebugOptions.instance.multiplayer.debugFlags.deadBody.enable.getValue()) {
            stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("%s (%s)", this.getObjectID().getObjectID(), this.getCharacterOnlineID()), c.r, c.g, c.b, c.a);
            if (DebugOptions.instance.multiplayer.debugFlags.deadBody.position.getValue()) {
                c = Colors.RosyBrown;
                dy += 4.0f;
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("x=%09.3f", Float.valueOf(this.getX())), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("y=%09.3f", Float.valueOf(this.getY())), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("z=%09.3f", Float.valueOf(this.getZ())), c.r, c.g, c.b, c.a);
            }
        }
    }

    public boolean isAnimal() {
        return !StringUtils.isNullOrEmpty(this.animalType);
    }

    @Override
    public float getWeight() {
        return this.weight;
    }

    public String getCorpseItem() {
        return this.corpseItem;
    }

    public String getCustomName() {
        return this.customName;
    }

    public void setAnimalData(IsoAnimal died) {
        Object functionObj = LuaManager.getFunctionObject("setAnimalBodyData");
        if (functionObj != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObj, died, this.getModData());
        }
        this.animalType = died.getAnimalType();
        this.animalSize = died.getAnimalSize();
        this.baseVisual = new AnimalVisual(this);
        this.baseVisual.copyFrom(died.getAnimalVisual());
        this.animalAnimSet = died.adef.animset;
        this.weight = died.getData().getWeight();
        this.customName = died.getFullName();
        this.deathTime = (float)GameTime.getInstance().getWorldAgeHours();
        this.invIcon = this.getDeadAnimalIcon(died);
        this.rottenTexture = died.getBreed().getRottenTexture();
        this.skelInvIcon = died.getBreed().invIconFemaleSkel;
        if (!died.isFemale()) {
            this.skelInvIcon = died.getBreed().invIconMaleSkel;
        }
        if (died.isBaby()) {
            this.skelInvIcon = died.getBreed().invIconBabySkel;
        }
        this.getShadowParams().set(died.adef.shadoww, died.adef.shadowfm, died.adef.shadowbm);
    }

    public SurvivorDesc getDescriptor() {
        return this.desc;
    }

    @Override
    public Vector2 getAnimForwardDirection(Vector2 forwardDirection) {
        forwardDirection.set(this.forwardDirection);
        return forwardDirection;
    }

    @Override
    public void setForwardDirection(float directionX, float directionY) {
        this.angle = Vector2.getDirection(directionX, directionY);
        this.forwardDirection.set(directionX, directionY);
    }

    @Override
    public boolean isPerformingGrappleAnimation() {
        return false;
    }

    public void setForwardDirectionAngle(float angle) {
        this.angle = angle;
        this.forwardDirection.setDirection(angle);
    }

    @Override
    public IAnimatable getAnimatable() {
        return null;
    }

    @Override
    public IGrappleable getWrappedGrappleable() {
        return this.grappleable;
    }

    public TwistableBoneTransform[] getDiedBoneTransforms() {
        return this.diedBoneTransforms;
    }

    public String getCarcassName() {
        if (!StringUtils.isNullOrEmpty(this.animalType) && !StringUtils.isNullOrEmpty(this.getBreed())) {
            return this.animalType + this.getBreed();
        }
        return "";
    }

    public String getBreed() {
        return (String)this.getModData().rawget("AnimalBreed");
    }

    public boolean hasAnimalParts() {
        if (this.getModData().rawget("parts") != null) {
            return (Boolean)this.getModData().rawget("parts");
        }
        return false;
    }

    public boolean isAnimalSkeleton() {
        return "true".equals(this.getModData().rawget("skeleton")) || ((KahluaTableImpl)this.getModData()).rawgetBool("skeleton");
    }

    public void invalidateCorpse() {
        if (PerformanceSettings.fboRenderChunk && DebugOptions.instance.fboRenderChunk.corpsesInChunkTexture.getValue()) {
            this.invalidateRenderChunkLevel(2L);
        } else {
            this.atlasTex = null;
        }
    }

    public void setInvalidateNextRender(boolean invalidate) {
        this.invalidateNextRender = invalidate;
    }

    public String getInvIcon() {
        return this.invIcon;
    }

    public String getPickUpSound() {
        AnimalDefinitions def = AnimalDefinitions.getDef(this.getAnimalType());
        if (def == null) {
            return "CorpseGrab";
        }
        AnimalBreed breed = def.getBreedByName(this.getBreed());
        if (breed == null) {
            return "CorpseGrab";
        }
        AnimalBreed.Sound sound = breed.getSound("pick_up_corpse");
        if (sound == null || StringUtils.isNullOrWhitespace(sound.soundName)) {
            return "CorpseGrab";
        }
        return sound.soundName;
    }

    public void setOnHook(boolean value) {
        this.isOnHook = value;
    }

    public boolean isOnHook() {
        return this.isOnHook;
    }

    public IsoGameCharacter getKilledBy() {
        return this.killedBy;
    }

    public void setKilledBy(IsoGameCharacter killedBy) {
        this.killedBy = killedBy;
    }

    public static void removeDeadBodies(UdpConnection removeCorpsesConnection) {
        int radius = (IsoChunkMap.chunkGridWidth + 2) * 8;
        ArrayList<IsoDeadBody> deadBodiesToRemove = new ArrayList<IsoDeadBody>();
        for (IsoPlayer player : removeCorpsesConnection.players) {
            if (player == null) continue;
            deadBodiesToRemove.clear();
            for (IIdentifiable identifiable : ObjectIDType.DeadBody.getObjects()) {
                IsoDeadBody isoDeadBody;
                if (!(identifiable instanceof IsoDeadBody) || !(isoDeadBody = (IsoDeadBody)identifiable).isInRange(player, radius)) continue;
                deadBodiesToRemove.add(isoDeadBody);
            }
            for (IsoDeadBody isoDeadBody : deadBodiesToRemove) {
                ObjectIDManager.getInstance().remove(isoDeadBody.getObjectID());
                if (isoDeadBody.getSquare() == null) continue;
                isoDeadBody.getSquare().removeCorpse(isoDeadBody, false);
            }
        }
    }

    public void writeInventory(ByteBufferWriter b) {
        try {
            ArrayList<InventoryItem> savedItems = this.getContainer().save(b.bb);
            WornItems wornItems = this.getWornItems();
            if (wornItems == null) {
                b.putByte(0);
            } else {
                int wornItemCount = wornItems.size();
                b.putByte(wornItemCount);
                for (int i = 0; i < wornItemCount; ++i) {
                    WornItem wornItem = wornItems.get(i);
                    b.putUTF(wornItem.getLocation().toString());
                    b.putShort(savedItems.indexOf(wornItem.getItem()));
                }
            }
            AttachedItems attachedItems = this.getAttachedItems();
            if (attachedItems == null) {
                b.putByte(0);
            } else {
                int attachedItemsCount = attachedItems.size();
                b.putByte(attachedItemsCount);
                for (int i = 0; i < attachedItemsCount; ++i) {
                    AttachedItem attachedItem = attachedItems.get(i);
                    b.putUTF(attachedItem.getLocation());
                    b.putShort(savedItems.indexOf(attachedItem.getItem()));
                }
            }
        }
        catch (IOException e) {
            DebugType.Death.printException(e, "WriteInventory error id=" + this.getCharacterOnlineID(), LogSeverity.Error);
        }
    }

    public InventoryItem becomeCorpseItem() {
        InventoryItem inventoryItem = this.getItem();
        if (inventoryItem == null) {
            return null;
        }
        this.square.removeCorpse(this, true);
        return inventoryItem;
    }

    @Override
    public void setDoRender(boolean doRender) {
        if (this.getDoRender() != doRender) {
            super.setDoRender(doRender);
            this.setInvalidateNextRender(true);
        }
    }

    public boolean canBeGrabbedFrom(float x, float y) {
        IsoCell targetCell = this.getCell();
        IsoGridSquare targetSquare = this.getSquare();
        IsoGridSquare fromSquare = targetCell.getGridSquare(x, y, this.getZ());
        return fromSquare != null && fromSquare.canReachTo(targetSquare);
    }

    static {
        _rotation = new Quaternionf();
        _transform = new Transform();
        _UNIT_Z = new Vector3f(0.0f, 0.0f, 1.0f);
        _tempVec3f_1 = new Vector3f();
        _tempVec3f_2 = new Vector3f();
    }
}

