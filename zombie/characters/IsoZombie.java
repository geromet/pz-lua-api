/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import org.joml.Vector3f;
import zombie.CombatManager;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.Lua.LuaEventManager;
import zombie.PersistentOutfits;
import zombie.SandboxOptions;
import zombie.SharedDescriptors;
import zombie.SoundManager;
import zombie.SystemDisabler;
import zombie.UsedFromLua;
import zombie.VirtualZombieManager;
import zombie.WorldSoundManager;
import zombie.ai.State;
import zombie.ai.ZombieGroupManager;
import zombie.ai.astar.AStarPathFinder;
import zombie.ai.astar.Mover;
import zombie.ai.states.AttackNetworkState;
import zombie.ai.states.AttackState;
import zombie.ai.states.BumpedState;
import zombie.ai.states.BurntToDeath;
import zombie.ai.states.ClimbOverFenceState;
import zombie.ai.states.ClimbOverWallState;
import zombie.ai.states.ClimbThroughWindowPositioningParams;
import zombie.ai.states.ClimbThroughWindowState;
import zombie.ai.states.CrawlingZombieTurnState;
import zombie.ai.states.FakeDeadAttackState;
import zombie.ai.states.FakeDeadZombieState;
import zombie.ai.states.GrappledThrownIntoContainerState;
import zombie.ai.states.GrappledThrownOutWindowState;
import zombie.ai.states.GrappledThrownOverFenceState;
import zombie.ai.states.IdleState;
import zombie.ai.states.LungeNetworkState;
import zombie.ai.states.LungeState;
import zombie.ai.states.PathFindState;
import zombie.ai.states.PlayerHitReactionState;
import zombie.ai.states.StaggerBackState;
import zombie.ai.states.ThumpState;
import zombie.ai.states.VehicleCollisionMinorStaggerState;
import zombie.ai.states.VehicleCollisionOnGroundState;
import zombie.ai.states.VehicleCollisionState;
import zombie.ai.states.WalkTowardNetworkState;
import zombie.ai.states.WalkTowardState;
import zombie.ai.states.ZombieEatBodyState;
import zombie.ai.states.ZombieFaceTargetState;
import zombie.ai.states.ZombieFallDownState;
import zombie.ai.states.ZombieFallingState;
import zombie.ai.states.ZombieGenericState;
import zombie.ai.states.ZombieGetDownState;
import zombie.ai.states.ZombieGetUpFromCrawlState;
import zombie.ai.states.ZombieGetUpState;
import zombie.ai.states.ZombieHitReactionState;
import zombie.ai.states.ZombieIdleState;
import zombie.ai.states.ZombieOnGroundState;
import zombie.ai.states.ZombieRagdollOnGroundState;
import zombie.ai.states.ZombieReanimateState;
import zombie.ai.states.ZombieSittingState;
import zombie.ai.states.ZombieTurnAlerted;
import zombie.audio.parameters.ParameterCharacterInside;
import zombie.audio.parameters.ParameterCharacterMovementSpeed;
import zombie.audio.parameters.ParameterCharacterOnFire;
import zombie.audio.parameters.ParameterFootstepMaterial;
import zombie.audio.parameters.ParameterFootstepMaterial2;
import zombie.audio.parameters.ParameterPlayerDistance;
import zombie.audio.parameters.ParameterShoeType;
import zombie.audio.parameters.ParameterVehicleHitLocation;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.AttachedItems.AttachedItem;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.FallDamage;
import zombie.characters.FallingConstants;
import zombie.characters.HitReactionNetworkAI;
import zombie.characters.Imposter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.NetworkCharacterAI;
import zombie.characters.NetworkZombieAI;
import zombie.characters.SurvivorDesc;
import zombie.characters.SurvivorFactory;
import zombie.characters.UnderwearDefinition;
import zombie.characters.ZombieFootstepManager;
import zombie.characters.ZombieGroup;
import zombie.characters.ZombieVocalsManager;
import zombie.characters.ZombiesZoneDefinition;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionGroup;
import zombie.characters.skills.PerkFactory;
import zombie.core.BoxedStaticValues;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.math.interpolators.LerpType;
import zombie.core.opengl.RenderSettings;
import zombie.core.opengl.Shader;
import zombie.core.physics.RagdollController;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.sharedskele.SharedSkeleAnimationRepository;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.utils.BooleanGrid;
import zombie.core.utils.OnceEvery;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemSpawner;
import zombie.inventory.types.HandWeapon;
import zombie.iso.BentFences;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.LosUtil;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.objects.IsoZombieGiblets;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerLOS;
import zombie.network.ServerMap;
import zombie.network.packets.character.ZombiePacket;
import zombie.network.statistics.data.ConnectionQueueStatistic;
import zombie.network.statistics.data.GameStatistic;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.nativeCode.PathfindNative;
import zombie.popman.NetworkZombieManager;
import zombie.popman.NetworkZombieSimulator;
import zombie.popman.ZombieCountOptimiser;
import zombie.popman.ZombieStateFlags;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;
import zombie.scripting.objects.ResourceLocation;
import zombie.statistics.StatisticCategory;
import zombie.statistics.StatisticType;
import zombie.statistics.StatisticsManager;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.AttackVehicleState;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

@UsedFromLua
public final class IsoZombie
extends IsoGameCharacter
implements IHumanVisual {
    private static final int s_saveFormatVersion = 1;
    public static final byte SPEED_SPRINTER = 1;
    public static final byte SPEED_FAST_SHAMBLER = 2;
    public static final byte SPEED_SHAMBLER = 3;
    public static final byte SPEED_RANDOM = 4;
    public static final byte HEARING_PINPOINT = 1;
    public static final byte HEARING_NORMAL = 2;
    public static final byte HEARING_POOR = 3;
    public static final byte HEARING_RANDOM = 4;
    public static final byte HEARING_NORMAL_OR_POOR = 5;
    public static final byte THUMP_FLAG_GENERIC = 1;
    public static final byte THUMP_FLAG_WINDOW_EXTRA = 2;
    public static final byte THUMP_FLAG_WINDOW = 3;
    public static final byte THUMP_FLAG_METAL = 4;
    public static final byte THUMP_FLAG_GARAGE_DOOR = 5;
    public static final byte THUMP_FLAG_CHAINLINK_FENCE = 6;
    public static final byte THUMP_FLAG_METAL_POLE_GATE = 7;
    public static final byte THUMP_FLAG_WOOD = 8;
    private static final ArrayList<IsoDeadBody> tempBodies = new ArrayList();
    private boolean alwaysKnockedDown;
    private boolean onlyJawStab;
    private boolean forceEatingAnimation;
    private boolean noTeeth;
    public static final int AllowRepathDelayMax = 120;
    public static final boolean SPRINTER_FIXES = true;
    public int lastTargetSeenX = -1;
    public int lastTargetSeenY = -1;
    public int lastTargetSeenZ = -1;
    public boolean ghost;
    public float lungeTimer;
    public long lungeSoundTime;
    public IsoMovingObject target;
    public float timeSinceSeenFlesh = 100000.0f;
    private float targetSeenTime;
    private boolean canSeeTarget;
    public int followCount;
    public int zombieId;
    private float bonusSpotTime;
    public boolean staggerBack;
    private boolean knifeDeath;
    private boolean jawStabAttach;
    private boolean becomeCrawler;
    private boolean fakeDead;
    private boolean forceFakeDead;
    private boolean wasFakeDead;
    private boolean reanimate;
    public DeadBodyAtlas.BodyTexture atlasTex;
    private boolean reanimatedPlayer;
    public boolean indoorZombie;
    public int thumpFlag;
    public boolean thumpSent;
    private float thumpCondition = 1.0f;
    public static final float EAT_BODY_DIST = 1.0f;
    public static final float EAT_BODY_TIME = 3600.0f;
    public static final float LUNGE_TIME = 180.0f;
    public static final float CRAWLER_DAMAGE_DOT = 0.9f;
    public static final float CRAWLER_DAMAGE_RANGE = 1.5f;
    private boolean useless;
    public int speedType = -1;
    public ZombieGroup group;
    public boolean inactive;
    public int strength = -1;
    public int cognition = -1;
    public int memory = -1;
    public int sight = -1;
    public int hearing = -1;
    private ArrayList<InventoryItem> itemsToSpawnAtDeath;
    private int voiceChoice = -1;
    private float soundReactDelay;
    private final IsoGameCharacter.Location delayedSound = new IsoGameCharacter.Location(-1, -1, -1);
    private boolean soundSourceRepeating;
    private boolean soundSourceIsPlayer;
    private boolean soundSourceIsPlayerBase;
    public Object soundSourceTarget;
    public float soundAttract;
    public float soundAttractTimeout;
    public boolean alerted;
    private String walkType;
    private float footstepVolume = 1.0f;
    private SharedDescriptors.Descriptor sharedDesc;
    public boolean dressInRandomOutfit;
    public String pendingOutfitName;
    private final HumanVisual humanVisual = new HumanVisual(this);
    private int crawlerType;
    private String playerAttackPosition;
    private float eatSpeed = 1.0f;
    private boolean sitAgainstWall;
    private static final int CHECK_FOR_CORPSE_TIMER_MAX = 10000;
    private float checkForCorpseTimer = 10000.0f;
    public IsoDeadBody bodyToEat;
    public IsoMovingObject eatBodyTarget;
    private int hitTime;
    private int thumpTimer;
    private boolean hitLegsWhileOnFloor;
    public boolean collideWhileHit = true;
    private float characterTextureAnimTime;
    private float characterTextureAnimDuration = 1.0f;
    public int lastPlayerHit = -1;
    public static final float VISION_RADIUS_MAX = 20.0f;
    public static final float VISION_RADIUS_MIN = 10.0f;
    public float visionRadiusResult = 20.0f;
    public static final float VISION_FOG_PENALTY_MAX = 7.0f;
    public static final float VISION_RAIN_PENALTY_MAX = 2.5f;
    public static final float VISION_DARKNESS_PENALTY_MAX = 5.0f;
    public static final int HEARING_UNSEEN_OFFSET_MIN = 2;
    public static final int HEARING_UNSEEN_OFFSET_HEAVY_RAIN = 5;
    public static final int HEARING_UNSEEN_OFFSET_MAX = 10;
    protected final ItemVisuals itemVisuals = new ItemVisuals();
    private int hitHeadWhileOnFloor;
    private boolean attackDidDamage;
    private String attackOutcome = "";
    private boolean reanimatedForGrappleOnly;
    public Imposter imposter = new Imposter();
    public IsoMovingObject spottedLast;
    private BaseVehicle vehicle4testCollision;
    private int spotSoundDelay;
    public float movex;
    public float movey;
    private int stepFrameLast = -1;
    private final OnceEvery networkUpdate = new OnceEvery(1.0f);
    public short lastRemoteUpdate;
    public short onlineId = (short)-1;
    public String spriteName = "BobZ";
    public static final int PALETTE_COUNT = 3;
    public final Vector2 vectorToTarget = new Vector2();
    public float allowRepathDelay;
    public boolean keepItReal;
    private boolean isSkeleton;
    public final ParameterCharacterInside parameterCharacterInside = new ParameterCharacterInside(this);
    private final ParameterCharacterMovementSpeed parameterCharacterMovementSpeed = new ParameterCharacterMovementSpeed(this);
    public final ParameterCharacterOnFire parameterCharacterOnFire = new ParameterCharacterOnFire(this);
    private final ParameterFootstepMaterial parameterFootstepMaterial = new ParameterFootstepMaterial(this);
    private final ParameterFootstepMaterial2 parameterFootstepMaterial2 = new ParameterFootstepMaterial2(this);
    public final ParameterPlayerDistance parameterPlayerDistance = new ParameterPlayerDistance(this);
    private final ParameterShoeType parameterShoeType = new ParameterShoeType(this);
    private final ParameterVehicleHitLocation parameterVehicleHitLocation = new ParameterVehicleHitLocation();
    public final ParameterZombieState parameterZombieState = new ParameterZombieState(this);
    public boolean scratch;
    public boolean laceration;
    public final NetworkZombieAI networkAi;
    private UdpConnection authOwner;
    private IsoPlayer authOwnerPlayer;
    public ZombiePacket zombiePacket = new ZombiePacket();
    public boolean zombiePacketUpdated;
    public long lastChangeOwner = -1L;
    public int bloodSplatAmount = 1000;
    private static final Vector2 lastPosition = new Vector2();
    private static final Vector2 currentPosition = new Vector2();
    public BodyPartType lastHitPart;
    public float timeSinceRespondToSound = 1000000.0f;
    private static final SharedSkeleAnimationRepository m_sharedSkeleRepo = new SharedSkeleAnimationRepository();
    public String walkVariantUse;
    public String walkVariant = "ZombieWalk";
    public boolean lunger;
    public boolean running;
    public boolean crawling;
    private boolean canCrawlUnderVehicle = true;
    private boolean canWalk = true;
    public boolean remote;
    private static final FloodFill floodFill = new FloodFill();
    public boolean immortalTutorialZombie;
    private final int palette;
    private final Aggro[] aggroList = new Aggro[4];
    private float unbalancedLevel;
    private static final Map<String, String> temporaryMapCloseSneakBonusDir = Map.ofEntries(Map.entry("trashcontainers_01_08", "NWSE"), Map.entry("trashcontainers_01_09", "NWSE"), Map.entry("trashcontainers_01_10", "NWSE"), Map.entry("trashcontainers_01_11", "NWSE"), Map.entry("trashcontainers_01_12", "NWSE"), Map.entry("trashcontainers_01_13", "NWSE"), Map.entry("trashcontainers_01_14", "NWSE"), Map.entry("trashcontainers_01_15", "NWSE"), Map.entry("f_bushes_1_96", "NWSE"), Map.entry("f_bushes_1_97", "NWSE"), Map.entry("f_bushes_1_98", "NWSE"), Map.entry("f_bushes_1_99", "NWSE"), Map.entry("f_bushes_1_100", "NWSE"), Map.entry("f_bushes_1_101", "NWSE"), Map.entry("f_bushes_1_102", "NWSE"), Map.entry("f_bushes_1_103", "NWSE"), Map.entry("f_bushes_1_104", "NWSE"), Map.entry("f_bushes_1_105", "NWSE"), Map.entry("f_bushes_1_106", "NWSE"), Map.entry("f_bushes_1_107", "NWSE"), Map.entry("f_bushes_1_108", "NWSE"), Map.entry("f_bushes_1_109", "NWSE"), Map.entry("f_bushes_1_110", "NWSE"), Map.entry("f_bushes_1_111", "NWSE"), Map.entry("f_bushes_2_0", "NWSE"), Map.entry("f_bushes_2_1", "NWSE"), Map.entry("f_bushes_2_2", "NWSE"), Map.entry("f_bushes_2_3", "NWSE"), Map.entry("f_bushes_2_4", "NWSE"), Map.entry("f_bushes_2_5", "NWSE"), Map.entry("f_bushes_2_6", "NWSE"), Map.entry("f_bushes_2_7", "NWSE"), Map.entry("f_bushes_2_10", "NWSE"), Map.entry("f_bushes_2_11", "NWSE"), Map.entry("f_bushes_2_12", "NWSE"), Map.entry("f_bushes_2_13", "NWSE"), Map.entry("f_bushes_2_14", "NWSE"), Map.entry("f_bushes_2_15", "NWSE"), Map.entry("f_bushes_2_16", "NWSE"), Map.entry("f_bushes_2_17", "NWSE"), Map.entry("vegetation_ornamental_01_0", "NWSE"), Map.entry("vegetation_ornamental_01_1", "NWSE"), Map.entry("vegetation_ornamental_01_2", "NWSE"), Map.entry("vegetation_ornamental_01_3", "NWSE"), Map.entry("vegetation_ornamental_01_4", "NWSE"), Map.entry("vegetation_ornamental_01_5", "NWSE"), Map.entry("vegetation_ornamental_01_6", "NWSE"), Map.entry("vegetation_ornamental_01_7", "NWSE"), Map.entry("vegetation_ornamental_01_10", "NWSE"), Map.entry("vegetation_ornamental_01_11", "NWSE"), Map.entry("vegetation_ornamental_01_12", "NWSE"), Map.entry("vegetation_ornamental_01_13", "NWSE"), Map.entry("fencing_01_96", "NWSE"), Map.entry("fencing_01_98", "NWSE"), Map.entry("fencing_01_100", "NWSE"), Map.entry("fencing_01_102", "NWSE"), Map.entry("fencing_01_32", "N"), Map.entry("fencing_01_33", "N"), Map.entry("fencing_01_34", "W"), Map.entry("fencing_01_35", "W"), Map.entry("fencing_01_36", "NW"), Map.entry("fencing_01_4", "W"), Map.entry("fencing_01_5", "N"), Map.entry("fencing_01_6", "NW"), Map.entry("carpentry_02_40", "W"), Map.entry("carpentry_02_41", "N"), Map.entry("carpentry_02_42", "NW"), Map.entry("carpentry_02_44", "W"), Map.entry("carpentry_02_45", "N"), Map.entry("carpentry_02_46", "NW"), Map.entry("carpentry_02_48", "W"), Map.entry("carpentry_02_49", "N"), Map.entry("carpentry_02_50", "NW"), Map.entry("fixtures_doors_fences_01_104", "W"), Map.entry("fixtures_doors_fences_01_105", "W"), Map.entry("fixtures_doors_fences_01_96", "W"), Map.entry("fixtures_doors_fences_01_97", "W"), Map.entry("fixtures_doors_fences_01_48", "W"), Map.entry("fixtures_doors_fences_01_49", "W"), Map.entry("fixtures_doors_fences_01_56", "W"), Map.entry("fixtures_doors_fences_01_57", "W"), Map.entry("fixtures_doors_fences_01_98", "N"), Map.entry("fixtures_doors_fences_01_99", "N"), Map.entry("fixtures_doors_fences_01_106", "N"), Map.entry("fixtures_doors_fences_01_107", "N"), Map.entry("fixtures_doors_fences_01_50", "N"), Map.entry("fixtures_doors_fences_01_51", "N"), Map.entry("fixtures_doors_fences_01_58", "N"), Map.entry("fixtures_doors_fences_01_59", "N"), Map.entry("fixtures_doors_fences_01_8", "W"), Map.entry("fixtures_doors_fences_01_9", "N"), Map.entry("fixtures_doors_fences_01_12", "W"), Map.entry("fixtures_doors_fences_01_13", "N"), Map.entry("fixtures_doors_fences_01_4", "W"), Map.entry("fixtures_doors_fences_01_5", "N"));
    private static final Map<String, Float> temporaryMapCloseSneakBonusValue = Map.ofEntries(Map.entry("trashcontainers_01_08", Float.valueOf(100.0f)), Map.entry("trashcontainers_01_09", Float.valueOf(100.0f)), Map.entry("trashcontainers_01_10", Float.valueOf(100.0f)), Map.entry("trashcontainers_01_11", Float.valueOf(100.0f)), Map.entry("trashcontainers_01_12", Float.valueOf(100.0f)), Map.entry("trashcontainers_01_13", Float.valueOf(100.0f)), Map.entry("trashcontainers_01_14", Float.valueOf(100.0f)), Map.entry("trashcontainers_01_15", Float.valueOf(100.0f)), Map.entry("f_bushes_1_96", Float.valueOf(100.0f)), Map.entry("f_bushes_1_97", Float.valueOf(100.0f)), Map.entry("f_bushes_1_98", Float.valueOf(100.0f)), Map.entry("f_bushes_1_99", Float.valueOf(100.0f)), Map.entry("f_bushes_1_100", Float.valueOf(100.0f)), Map.entry("f_bushes_1_101", Float.valueOf(100.0f)), Map.entry("f_bushes_1_102", Float.valueOf(100.0f)), Map.entry("f_bushes_1_103", Float.valueOf(100.0f)), Map.entry("f_bushes_1_104", Float.valueOf(100.0f)), Map.entry("f_bushes_1_105", Float.valueOf(100.0f)), Map.entry("f_bushes_1_106", Float.valueOf(100.0f)), Map.entry("f_bushes_1_107", Float.valueOf(100.0f)), Map.entry("f_bushes_1_108", Float.valueOf(100.0f)), Map.entry("f_bushes_1_109", Float.valueOf(100.0f)), Map.entry("f_bushes_1_110", Float.valueOf(100.0f)), Map.entry("f_bushes_1_111", Float.valueOf(100.0f)), Map.entry("f_bushes_2_0", Float.valueOf(100.0f)), Map.entry("f_bushes_2_1", Float.valueOf(100.0f)), Map.entry("f_bushes_2_2", Float.valueOf(100.0f)), Map.entry("f_bushes_2_3", Float.valueOf(100.0f)), Map.entry("f_bushes_2_4", Float.valueOf(100.0f)), Map.entry("f_bushes_2_5", Float.valueOf(100.0f)), Map.entry("f_bushes_2_6", Float.valueOf(100.0f)), Map.entry("f_bushes_2_7", Float.valueOf(100.0f)), Map.entry("f_bushes_2_10", Float.valueOf(100.0f)), Map.entry("f_bushes_2_11", Float.valueOf(100.0f)), Map.entry("f_bushes_2_12", Float.valueOf(100.0f)), Map.entry("f_bushes_2_13", Float.valueOf(100.0f)), Map.entry("f_bushes_2_14", Float.valueOf(100.0f)), Map.entry("f_bushes_2_15", Float.valueOf(100.0f)), Map.entry("f_bushes_2_16", Float.valueOf(100.0f)), Map.entry("f_bushes_2_17", Float.valueOf(100.0f)), Map.entry("vegetation_ornamental_01_0", Float.valueOf(100.0f)), Map.entry("vegetation_ornamental_01_1", Float.valueOf(100.0f)), Map.entry("vegetation_ornamental_01_2", Float.valueOf(100.0f)), Map.entry("vegetation_ornamental_01_3", Float.valueOf(100.0f)), Map.entry("vegetation_ornamental_01_4", Float.valueOf(100.0f)), Map.entry("vegetation_ornamental_01_5", Float.valueOf(100.0f)), Map.entry("vegetation_ornamental_01_6", Float.valueOf(100.0f)), Map.entry("vegetation_ornamental_01_7", Float.valueOf(100.0f)), Map.entry("vegetation_ornamental_01_10", Float.valueOf(100.0f)), Map.entry("vegetation_ornamental_01_11", Float.valueOf(100.0f)), Map.entry("vegetation_ornamental_01_12", Float.valueOf(100.0f)), Map.entry("vegetation_ornamental_01_13", Float.valueOf(100.0f)), Map.entry("fencing_01_96", Float.valueOf(100.0f)), Map.entry("fencing_01_98", Float.valueOf(100.0f)), Map.entry("fencing_01_100", Float.valueOf(100.0f)), Map.entry("fencing_01_102", Float.valueOf(100.0f)), Map.entry("fencing_01_32", Float.valueOf(100.0f)), Map.entry("fencing_01_33", Float.valueOf(100.0f)), Map.entry("fencing_01_34", Float.valueOf(100.0f)), Map.entry("fencing_01_35", Float.valueOf(100.0f)), Map.entry("fencing_01_36", Float.valueOf(100.0f)), Map.entry("fencing_01_4", Float.valueOf(60.0f)), Map.entry("fencing_01_5", Float.valueOf(60.0f)), Map.entry("fencing_01_6", Float.valueOf(60.0f)), Map.entry("carpentry_02_40", Float.valueOf(100.0f)), Map.entry("carpentry_02_41", Float.valueOf(100.0f)), Map.entry("carpentry_02_42", Float.valueOf(100.0f)), Map.entry("carpentry_02_44", Float.valueOf(100.0f)), Map.entry("carpentry_02_45", Float.valueOf(100.0f)), Map.entry("carpentry_02_46", Float.valueOf(100.0f)), Map.entry("carpentry_02_48", Float.valueOf(100.0f)), Map.entry("carpentry_02_49", Float.valueOf(100.0f)), Map.entry("carpentry_02_50", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_104", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_105", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_96", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_97", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_48", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_49", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_56", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_57", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_98", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_99", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_106", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_107", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_50", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_51", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_58", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_59", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_8", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_9", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_12", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_13", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_4", Float.valueOf(100.0f)), Map.entry("fixtures_doors_fences_01_5", Float.valueOf(100.0f)));

    public IsoZombie(IsoCell cell) {
        this(cell, null, -1);
    }

    public IsoZombie(IsoCell cell, SurvivorDesc desc, int palette) {
        super(cell, 0.0f, 0.0f, 0.0f);
        this.getWrappedGrappleable().setOnGrappledEndCallback(this::onZombieGrappleEnded);
        if (!GameServer.server) {
            this.registerVariableCallbacks();
        }
        this.health = 1.8f + Rand.Next(0.0f, 0.3f);
        this.weight = 0.7f;
        this.dir = IsoDirections.getRandom();
        this.humanVisual.randomBlood();
        if (desc != null) {
            this.descriptor = desc;
            this.palette = palette;
        } else {
            this.descriptor = SurvivorFactory.CreateSurvivor();
            this.palette = Rand.Next(3) + 1;
        }
        this.setFemale(this.descriptor.isFemale());
        if (!this.descriptor.getVoicePrefix().contains("Zombie")) {
            this.descriptor.setVoicePrefix(Objects.equals(this.descriptor.getVoicePrefix(), "VoiceFemale") ? "FemaleZombie" : "MaleZombie");
        }
        String string = this.spriteName = this.isFemale() ? "KateZ" : "BobZ";
        if (this.palette != 1) {
            this.spriteName = this.spriteName + this.palette;
        }
        this.InitSpritePartsZombie();
        this.sprite.def.tintr = 0.95f + (float)Rand.Next(5) / 100.0f;
        this.sprite.def.tintg = 0.95f + (float)Rand.Next(5) / 100.0f;
        this.sprite.def.tintb = 0.95f + (float)Rand.Next(5) / 100.0f;
        this.setDefaultState(ZombieGenericState.instance());
        this.setFakeDead(false);
        this.DoZombieStats();
        this.width = 0.3f;
        this.setAlphaAndTarget(0.0f);
        this.finder.maxSearchDistance = 20;
        this.hurtSound = this.descriptor.getVoicePrefix() + "Hurt";
        this.initializeStates();
        this.getActionContext().setGroup(ActionGroup.getActionGroup("zombie"));
        this.initWornItems("Human");
        this.initAttachedItems("Human");
        this.networkAi = new NetworkZombieAI(this);
        this.clearAggroList();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{  Name:" + this.getName() + ",  ID:" + this.getID() + "  StateFlags:" + String.valueOf(ZombieStateFlags.fromZombie(this)) + "}";
    }

    @Override
    public String getObjectName() {
        return "Zombie";
    }

    @Override
    public short getOnlineID() {
        return this.onlineId;
    }

    public boolean isRemoteZombie() {
        return this.authOwner == null;
    }

    @Override
    public UdpConnection getOwner() {
        return this.authOwner;
    }

    @Override
    public void setOwner(UdpConnection connection) {
        this.authOwner = connection;
    }

    @Override
    public IsoPlayer getOwnerPlayer() {
        return this.authOwnerPlayer;
    }

    @Override
    public void setOwnerPlayer(IsoPlayer player) {
        this.authOwnerPlayer = player;
    }

    public void setVehicle4TestCollision(BaseVehicle vehicle) {
        this.vehicle4testCollision = vehicle;
    }

    public void initializeStates() {
        this.clearAIStateMap();
        this.registerAIState("idle", ZombieIdleState.instance());
        this.registerAIState("attack-network", AttackNetworkState.instance());
        this.registerAIState("attackvehicle-network", IdleState.instance());
        this.registerAIState("fakedead-attack-network", IdleState.instance());
        this.registerAIState("lunge-network", LungeNetworkState.instance());
        this.registerAIState("walktoward-network", WalkTowardNetworkState.instance());
        this.registerAIState("corpseThrownOutWindow-onback", GrappledThrownOutWindowState.instance());
        this.registerAIState("corpseThrownOutWindow-onfront", GrappledThrownOutWindowState.instance());
        this.registerAIState("corpseThrownOverFence-onback", GrappledThrownOverFenceState.instance());
        this.registerAIState("corpseThrownoverFence-onfront", GrappledThrownOverFenceState.instance());
        this.registerAIState("corpseThrownIntoContainer-onback", GrappledThrownIntoContainerState.instance());
        this.registerAIState("corpseThrownIntoContainer-onfront", GrappledThrownIntoContainerState.instance());
        if (this.crawling) {
            this.registerAIState("attack", AttackState.instance());
            this.registerAIState("fakedead", FakeDeadZombieState.instance());
            this.registerAIState("fakedead-attack", FakeDeadAttackState.instance());
            this.registerAIState("getup", ZombieGetUpFromCrawlState.instance());
            this.registerAIState("hitreaction", ZombieHitReactionState.instance());
            this.registerAIState("hitreaction-fencewindow", ZombieHitReactionState.instance());
            this.registerAIState("hitreaction-knockeddown", ZombieHitReactionState.instance());
            this.registerAIState("hitreaction-ragdoll", ZombieHitReactionState.instance());
            this.registerAIState("hitreaction-shothead-bwd", ZombieFallDownState.instance());
            this.registerAIState("hitreaction-shothead-bwd-ragdoll", ZombieFallDownState.instance());
            this.registerAIState("hitreaction-shothead-fwd", ZombieFallDownState.instance());
            this.registerAIState("hitreaction-shothead-fwd-ragdoll", ZombieFallDownState.instance());
            this.registerAIState("hitreaction-shothead-fwd02", ZombieFallDownState.instance());
            this.registerAIState("hitreaction-shothead-fwd02-ragdoll", ZombieFallDownState.instance());
            this.registerAIState("hitreaction-onfloor", ZombieHitReactionState.instance());
            this.registerAIState("hitreaction-gettingup", ZombieHitReactionState.instance());
            this.registerAIState("hitreaction-whilesitting", ZombieHitReactionState.instance());
            this.registerAIState("hitreaction-hit", ZombieHitReactionState.instance());
            this.registerAIState("onground", ZombieOnGroundState.instance());
            this.registerAIState("falldown-ragdoll", ZombieRagdollOnGroundState.instance());
            this.registerAIState("pathfind", PathFindState.instance());
            this.registerAIState("reanimate", ZombieReanimateState.instance());
            this.registerAIState("staggerback", StaggerBackState.instance());
            this.registerAIState("staggerback-knockeddown", StaggerBackState.instance());
            this.registerAIState("staggerback-ragdoll", StaggerBackState.instance());
            this.registerAIState("staggerback-knockeddown-ragdoll", StaggerBackState.instance());
            this.registerAIState("vehicleCollision-staggerback", StaggerBackState.instance());
            this.registerAIState("thump", ThumpState.instance());
            this.registerAIState("turn", CrawlingZombieTurnState.instance());
            this.registerAIState("walktoward", WalkTowardState.instance());
        } else {
            this.registerAIState("attack", AttackState.instance());
            this.registerAIState("attackvehicle", AttackVehicleState.instance());
            this.registerAIState("bumped", BumpedState.instance());
            this.registerAIState("climbfence", ClimbOverFenceState.instance());
            this.registerAIState("climbwindow", ClimbThroughWindowState.instance());
            this.registerAIState("eatbody", ZombieEatBodyState.instance());
            this.registerAIState("falldown", ZombieFallDownState.instance());
            this.registerAIState("vehicleCollision-falldown", ZombieFallDownState.instance());
            this.registerAIState("vehicleCollision-ragdoll", ZombieFallDownState.instance());
            this.registerAIState("falldown-eatingbody", ZombieFallDownState.instance());
            this.registerAIState("falldown-knifedeath", ZombieFallDownState.instance());
            this.registerAIState("falldown-knifedeath-ragdoll", ZombieFallDownState.instance());
            this.registerAIState("falldown-onknees", ZombieFallDownState.instance());
            this.registerAIState("falldown-whilesitting", ZombieFallDownState.instance());
            this.registerAIState("falldown-headleft", ZombieFallDownState.instance());
            this.registerAIState("falldown-headright", ZombieFallDownState.instance());
            this.registerAIState("falldown-headtop", ZombieFallDownState.instance());
            this.registerAIState("falldown-speardeath", ZombieFallDownState.instance());
            this.registerAIState("falldown-speardeath-ragdoll", ZombieFallDownState.instance());
            this.registerAIState("falldown-speardeath1", ZombieFallDownState.instance());
            this.registerAIState("falldown-speardeath2", ZombieFallDownState.instance());
            this.registerAIState("falldown-uppercut", ZombieFallDownState.instance());
            this.registerAIState("falldown-ragdoll", ZombieFallDownState.instance());
            this.registerAIState("falling", ZombieFallingState.instance());
            this.registerAIState("face-target", ZombieFaceTargetState.instance());
            this.registerAIState("fakedead", FakeDeadZombieState.instance());
            this.registerAIState("fakedead-attack", FakeDeadAttackState.instance());
            this.registerAIState("getdown", ZombieGetDownState.instance());
            this.registerAIState("getup", ZombieGetUpState.instance());
            this.registerAIState("getup-fromOnBack", ZombieGetUpState.instance());
            this.registerAIState("getup-fromOnFront", ZombieGetUpState.instance());
            this.registerAIState("getup-fromSitting", ZombieGetUpState.instance());
            this.registerAIState("hitreaction", ZombieHitReactionState.instance());
            this.registerAIState("hitreaction-fencewindow", ZombieHitReactionState.instance());
            this.registerAIState("hitreaction-knockeddown", ZombieHitReactionState.instance());
            this.registerAIState("hitreaction-ragdoll", ZombieHitReactionState.instance());
            this.registerAIState("hitreaction-shothead-bwd", ZombieFallDownState.instance());
            this.registerAIState("hitreaction-shothead-bwd-ragdoll", ZombieFallDownState.instance());
            this.registerAIState("hitreaction-shothead-fwd", ZombieFallDownState.instance());
            this.registerAIState("hitreaction-shothead-fwd-ragdoll", ZombieFallDownState.instance());
            this.registerAIState("hitreaction-shothead-fwd02", ZombieFallDownState.instance());
            this.registerAIState("hitreaction-shothead-fwd02-ragdoll", ZombieFallDownState.instance());
            this.registerAIState("hitreaction-onfloor", ZombieHitReactionState.instance());
            this.registerAIState("hitreaction-gettingup", ZombieHitReactionState.instance());
            this.registerAIState("hitreaction-whilesitting", ZombieHitReactionState.instance());
            this.registerAIState("hitreaction-hit", ZombieHitReactionState.instance());
            this.registerAIState("idle", ZombieIdleState.instance());
            this.registerAIState("lunge", LungeState.instance());
            this.registerAIState("onground", ZombieOnGroundState.instance());
            this.registerAIState("falldown-ragdoll", ZombieFallDownState.instance());
            this.registerAIState("onground-ragdoll", ZombieRagdollOnGroundState.instance());
            this.registerAIState("pathfind", PathFindState.instance());
            this.registerAIState("sitting", ZombieSittingState.instance());
            this.registerAIState("staggerback", StaggerBackState.instance());
            this.registerAIState("staggerback-knockeddown", StaggerBackState.instance());
            this.registerAIState("staggerback-ragdoll", StaggerBackState.instance());
            this.registerAIState("staggerback-knockeddown-ragdoll", StaggerBackState.instance());
            this.registerAIState("knockeddown-headLeft", ZombieFallDownState.instance());
            this.registerAIState("knockeddown-headRight", ZombieFallDownState.instance());
            this.registerAIState("knockeddown-headTop", ZombieFallDownState.instance());
            this.registerAIState("knockeddown-shotChestL", ZombieFallDownState.instance());
            this.registerAIState("knockeddown-shotChestR", ZombieFallDownState.instance());
            this.registerAIState("knockeddown-shotLegL", ZombieFallDownState.instance());
            this.registerAIState("knockeddown-shotLegR", ZombieFallDownState.instance());
            this.registerAIState("knockeddown-shotShoulderL", ZombieFallDownState.instance());
            this.registerAIState("knockeddown-shotShoulderR", ZombieFallDownState.instance());
            this.registerAIState("knockeddown-uppercut", ZombieFallDownState.instance());
            this.registerAIState("vehicleCollision", VehicleCollisionState.instance());
            this.registerAIState("vehicleCollision-staggerback", StaggerBackState.instance());
            this.registerAIState("vehicleCollision-bumped", VehicleCollisionMinorStaggerState.instance());
            this.registerAIState("vehicleCollision-onground", VehicleCollisionOnGroundState.instance());
            this.registerAIState("vehicleCollision-onground-dead", VehicleCollisionOnGroundState.instance());
            this.registerAIState("thump", ThumpState.instance());
            this.registerAIState("turnalerted", ZombieTurnAlerted.instance());
            this.registerAIState("walktoward", WalkTowardState.instance());
            this.registerAIState("grappled", ZombieGenericState.instance());
        }
    }

    private void registerVariableCallbacks() {
        this.setVariable("bClient", () -> GameClient.client && this.isRemoteZombie(), (IAnimationVariableSource owner) -> "Is this multiplayer Zed a remote Zombie.");
        this.setVariable("bMultiplayer", () -> GameClient.client || GameServer.server, (IAnimationVariableSource owner) -> "Global. Is this a multiplayer session.");
        this.setVariable("bMovingNetwork", () -> !(!this.isLocal() && this.isBumped() || !(IsoUtils.DistanceManhatten(this.networkAi.targetX, this.networkAi.targetY, this.getX(), this.getY()) > 0.5f) && this.getZ() == (float)this.networkAi.targetZ), (IAnimationVariableSource owner) -> "Is this network Zed moving. <br />returns FALSE if bumped and non-local..");
        this.setVariable("hitHeadType", this::getHitHeadWhileOnFloor, (IAnimationVariableSource owner) -> "Returns the hitHead while on the floor. Typically 0-1.");
        this.setVariable("realState", this::getRealState, (IAnimationVariableSource owner) -> "Network. Returns the Zed's network realState.toString");
        this.setVariable("bIsReanimatedForGrappleOnly", this::isReanimatedForGrappleOnly, (IAnimationVariableSource owner) -> "Is this Zed just a reanimated corpse for the purposes of being dragged around.");
        this.setVariable("battack", this::getShouldAttack, (IAnimationVariableSource owner) -> "Returns TRUE if this Zed is intending an attack. getShouldAttack");
        this.setVariable("isFacingTarget", this::isFacingTarget, (IAnimationVariableSource owner) -> "Returns TRUE if this Zed has a target and is currently facing its target.");
        this.setVariable("targetSeenTime", this::getTargetSeenTime, (IAnimationVariableSource owner) -> "Amount of time, in seconds. How long has this Zed been looking at its current target.");
        this.setVariable("battackvehicle", () -> {
            IsoPlayer player;
            if (this.getVariableBoolean("bPathfind")) {
                return false;
            }
            if (this.isMoving()) {
                return false;
            }
            if (this.target == null) {
                return false;
            }
            if (Math.abs(this.target.getZ() - this.getZ()) >= 0.8f) {
                return false;
            }
            IsoMovingObject patt0$temp = this.target;
            if (patt0$temp instanceof IsoPlayer && (player = (IsoPlayer)patt0$temp).isGhostMode()) {
                return false;
            }
            IsoMovingObject patt1$temp = this.target;
            if (patt1$temp instanceof IsoGameCharacter) {
                IsoGameCharacter isoGameCharacter = (IsoGameCharacter)patt1$temp;
                BaseVehicle vehicle = isoGameCharacter.getVehicle();
                return vehicle != null && vehicle.isCharacterAdjacentTo(this);
            }
            return false;
        }, (IAnimationVariableSource owner) -> "Is this Zed targeting a vehicle.");
        this.setVariable("beatbodytarget", () -> {
            if (this.isForceEatingAnimation()) {
                return true;
            }
            if (!GameServer.server) {
                this.updateEatBodyTarget();
            }
            return this.getEatBodyTarget() != null;
        }, (IAnimationVariableSource owner) -> "Is this Zed targeting a body for eating.");
        this.setVariable("bbecomecrawler", this::isBecomeCrawler, this::setBecomeCrawler, (IAnimationVariableSource owner) -> "Is this Zed about to become a crawler.");
        this.setVariable("bfakedead", () -> this.fakeDead, (IAnimationVariableSource owner) -> "Is this Zed pretending to be dead.");
        this.setVariable("bHasTarget", () -> {
            IsoMovingObject patt0$temp = this.target;
            if (patt0$temp instanceof IsoGameCharacter) {
                IsoGameCharacter isoGameCharacter = (IsoGameCharacter)patt0$temp;
                if (isoGameCharacter.reanimatedCorpse != null) {
                    this.setTarget(null);
                }
            }
            return this.target != null;
        }, (IAnimationVariableSource owner) -> "Does this Zed have a target.");
        this.setVariable("bCanSeeTarget", () -> this.canSeeTarget, (IAnimationVariableSource owner) -> "Can this Zed see its target.");
        this.setVariable("shouldSprint", () -> {
            IsoMovingObject patt0$temp = this.target;
            if (patt0$temp instanceof IsoGameCharacter) {
                IsoGameCharacter isoGameCharacter = (IsoGameCharacter)patt0$temp;
                if (isoGameCharacter.reanimatedCorpse != null) {
                    this.setTarget(null);
                }
            }
            return this.target != null || this.soundSourceTarget != null && !(this.soundSourceTarget instanceof IsoZombie);
        }, (IAnimationVariableSource owner) -> "This Zed intends to sprint.");
        this.setVariable("bknockeddown", this::isKnockedDown, (IAnimationVariableSource owner) -> "Has this Zed been knocked down. Set when a bump or attack is sufficient to knock them off their feet.");
        this.setVariable("blunge", () -> {
            IsoGameCharacter isoGameCharacter;
            IsoMovingObject patt2$temp;
            IsoPlayer player;
            IsoMovingObject patt1$temp;
            if (this.target == null) {
                return false;
            }
            if (PZMath.fastfloor(this.getZ()) != PZMath.fastfloor(this.target.getZ())) {
                return false;
            }
            IsoMovingObject patt0$temp = this.target;
            if (patt0$temp instanceof IsoGameCharacter) {
                IsoGameCharacter gameCharacter = (IsoGameCharacter)patt0$temp;
                if (gameCharacter.getVehicle() != null) {
                    return false;
                }
                if (gameCharacter.reanimatedCorpse != null) {
                    return false;
                }
            }
            if ((patt1$temp = this.target) instanceof IsoPlayer && (player = (IsoPlayer)patt1$temp).isGhostMode()) {
                this.setTarget(null);
                return false;
            }
            IsoGridSquare currentSquare = this.getCurrentSquare();
            IsoGridSquare targetCurrentSquare = this.target.getCurrentSquare();
            if (targetCurrentSquare != null && targetCurrentSquare.isSomethingTo(currentSquare) && this.getThumpTarget() != null && !BentFences.getInstance().isUnbentObject(this.target)) {
                return false;
            }
            if (this.isSideOfStaircaseBetweenSelfAndTarget()) {
                this.lungeTimer = 0.0f;
                return false;
            }
            if (this.isCurrentState(ZombieTurnAlerted.instance()) && !this.isFacingTarget()) {
                return false;
            }
            float vecToTargetLength = this.vectorToTarget.getLength();
            if (!(!(vecToTargetLength > 3.5f) || vecToTargetLength <= 4.0f && (patt2$temp = this.target) instanceof IsoGameCharacter && (isoGameCharacter = (IsoGameCharacter)patt2$temp).getVehicle() != null)) {
                return false;
            }
            return !PolygonalMap2.instance.lineClearCollide(this.getX(), this.getY(), this.target.getX(), this.target.getY(), PZMath.fastfloor(this.getZ()), this.target, false, true);
        }, (IAnimationVariableSource owner) -> "This Zed intends to lunge.");
        this.setVariable("bstoplunging", () -> {
            if (this.target == null) {
                return true;
            }
            if (this.lungeTimer > 0.0f) {
                return false;
            }
            float distanceToTarget = this.vectorToTarget.getLength() - this.getWidth() + this.target.getWidth();
            if (this.lungeTimer <= 0.0f && distanceToTarget >= 0.8f) {
                return true;
            }
            if (this.isSideOfStaircaseBetweenSelfAndTarget()) {
                return true;
            }
            return false;
        }, (IAnimationVariableSource owner) -> "This Zed intends to stop lunging.");
        this.setVariable("bpassengerexposed", () -> AttackVehicleState.instance().isPassengerExposed(this), (IAnimationVariableSource owner) -> "When attacking a vehicle, is the target inside the vehicle exposed to attack.");
        this.setVariable("bistargetissmallvehicle", () -> {
            IsoPlayer player;
            IsoMovingObject patt0$temp;
            if (this.target != null && (patt0$temp = this.target) instanceof IsoPlayer && (player = (IsoPlayer)patt0$temp).getVehicle() != null) {
                return player.getVehicle().getScript().isSmallVehicle;
            }
            return true;
        }, (IAnimationVariableSource owner) -> "When this Zed's target is a character inside a vehicle, is that vehicle a small vehicle.");
        this.setVariable("breanimate", this::isReanimate, this::setReanimate, (IAnimationVariableSource owner) -> "Typically comes from being dead or on the ground. This Zed intends to reanimate from a dead corpse, or when it becomes a crawler.");
        this.setVariable("bstaggerback", this::isStaggerBack, (IAnimationVariableSource owner) -> "Is this Zed staggered backwards.");
        this.setVariable("unbalancedLevel", this::getUnbalancedLevel, this::setUnbalancedLevel, (IAnimationVariableSource owner) -> "The level that this Zed is currently unbalanced, by being staggered back etc.");
        this.setVariable("bthump", () -> {
            Thumpable patt0$temp = this.getThumpTarget();
            if (patt0$temp instanceof IsoObject) {
                IsoObject obj = (IsoObject)patt0$temp;
                if (!(this.getThumpTarget() instanceof BaseVehicle) && (obj.getSquare() == null || this.DistToSquared(obj.getX() + 0.5f, obj.getY() + 0.5f) > 9.0f)) {
                    this.setThumpTarget(null);
                }
            }
            if (this.getThumpTimer() > 0) {
                this.setThumpTarget(null);
            }
            return this.getThumpTarget() != null;
        }, (IAnimationVariableSource owner) -> "Is this Zed attacking something that makes a thumping sound. Such as a Door.");
        this.setVariable("bundervehicle", this::isUnderVehicle, (IAnimationVariableSource owner) -> "Is this Zed currently under a vehicle.");
        this.setVariable("bBeingSteppedOn", this::isBeingSteppedOn, (IAnimationVariableSource owner) -> "Is this Zed currently being stepped on.");
        this.setVariable("distancetotarget", () -> {
            if (this.target == null) {
                return -1.0f;
            }
            return this.vectorToTarget.getLength() - this.getWidth() + this.target.getWidth();
        }, (IAnimationVariableSource owner) -> "How far away is this Zed's target, if it has one. Otherwise, it is -1.");
        this.setVariable("lasttargetseen", () -> this.lastTargetSeenX != -1, (IAnimationVariableSource owner) -> "Does this Zed remember its target's position.");
        this.setVariable("lungetimer", () -> this.lungeTimer, (IAnimationVariableSource owner) -> "Lunge timer countdown, in frames at 30fps. Starts at 180.0. Spins to Zero.");
        this.setVariable("reanimatetimer", this::getReanimateTimer, (IAnimationVariableSource owner) -> "Reanimate timer countdown, in frames at 30fps.");
        this.setVariable("turndirection", () -> {
            if (this.getPath2() != null) {
                return "";
            }
            if (this.target == null || this.vectorToTarget.getLength() == 0.0f) {
                if (this.isCurrentState(WalkTowardState.instance())) {
                    WalkTowardState.instance().calculateTargetLocation(this, tempo);
                    IsoZombie.tempo.x -= this.getX();
                    IsoZombie.tempo.y -= this.getY();
                    IsoDirections targetDir = IsoDirections.fromAngle(tempo);
                    if (this.dir == targetDir) {
                        return "";
                    }
                    boolean left = CrawlingZombieTurnState.calculateDir(this, targetDir);
                    return left ? "left" : "right";
                }
                if (this.isCurrentState(PathFindState.instance())) {
                    // empty if block
                }
                return "";
            }
            if (GameClient.client && this.isRemoteZombie()) {
                tempo.set(this.networkAi.targetX - this.getX(), this.networkAi.targetY - this.getY());
            } else {
                tempo.set(this.vectorToTarget);
            }
            IsoDirections targetDir = IsoDirections.fromAngle(tempo);
            if (this.dir == targetDir) {
                return "";
            }
            boolean left = CrawlingZombieTurnState.calculateDir(this, targetDir);
            return left ? "left" : "right";
        }, (IAnimationVariableSource owner) -> "Is this Zed wanting to turn towards its target. Either Left or Right. If it is pathfinding, or has no target, returns blank \"\"");
        this.setVariable("alerted", () -> this.alerted, (IAnimationVariableSource owner) -> "Has this Zed been alerted to something.");
        this.setVariable("zombiewalktype", () -> this.walkType, (IAnimationVariableSource owner) -> "This Zed's walking type. slow1-3 if it's a shambler, or sprint1-5 if it's a sprinter.");
        this.setVariable("crawlertype", () -> this.crawlerType, (IAnimationVariableSource owner) -> "This Zed's crawler type. Typically 1 or 2. Or 0 if not set, or not a crawler.");
        this.setVariable("bGetUpFromCrawl", this::shouldGetUpFromCrawl, (IAnimationVariableSource owner) -> "Should this Zed get up from crawling.");
        this.setVariable("playerattackposition", this::getPlayerAttackPosition, (IAnimationVariableSource owner) -> "From which direction is this Zed attacked from. FRONT, BEHIND, LEFT, or RIGHT. If not attacked, returns blank \"\"");
        this.setVariable("eatspeed", () -> this.eatSpeed, (IAnimationVariableSource owner) -> "When eating a body. The eating speed multiplier. 0.0 - 1.0");
        this.setVariable("issitting", this::isSitAgainstWall, (IAnimationVariableSource owner) -> "Is this Zed currently sitting.");
        this.setVariable("bKnifeDeath", this::isKnifeDeath, this::setKnifeDeath, (IAnimationVariableSource owner) -> "Has this Zed been attacked and killed by a knife.");
        this.setVariable("bJawStabAttach", this::isJawStabAttach, this::setJawStabAttach, (IAnimationVariableSource owner) -> "Was this Zed attacked and stabbed in the jaw.");
        this.setVariable("bPathFindPrediction", () -> 2 == this.networkAi.predictionType, (IAnimationVariableSource owner) -> "Network. The type of pathfinding prediction. One of: <br />  Moving, <br />  Static, <br />  Thump, <br />  Climb, <br />  Lunge, <br />  LungeHalf, <br />  Walk, <br />  WalkHalf, <br />  PathFind.");
        this.setVariable("bCrawling", this::isCrawling, this::setCrawler, (IAnimationVariableSource owner) -> "Is this Zed a crawler.");
        this.setVariable("AttackDidDamage", this::getAttackDidDamage, this::setAttackDidDamage, (IAnimationVariableSource owner) -> "Network. Did an attack do damage.");
        this.setVariable("AttackOutcome", this::getAttackOutcome, this::setAttackOutcome, (IAnimationVariableSource owner) -> "The outcome of an attack. One of: <br />  start, <br />  interrupted, <br />  fail, <br />  success");
    }

    @Override
    protected void OnAnimEvent_IsAlmostUp(IsoGameCharacter owner) {
        super.OnAnimEvent_IsAlmostUp(owner);
        if (owner == this) {
            this.setSitAgainstWall(false);
        }
    }

    private boolean isIdleOrStaggering() {
        return this.isCurrentState(StaggerBackState.instance()) || this.isCurrentState(ZombieIdleState.instance());
    }

    @Override
    protected boolean shouldSlideHeadAwayFromWalls() {
        if ((!this.isOnFloor() && !this.isKnockedDown() || this.isSitAgainstWall()) && !this.isIdleOrStaggering()) {
            return false;
        }
        if (this.isCrawling() && (this.getPath2() != null || this.isMoving())) {
            return false;
        }
        return !this.isCurrentState(ClimbOverFenceState.instance()) && !this.isCurrentState(ClimbThroughWindowState.instance());
    }

    @Override
    protected void slideHeadAwayFromWalls(boolean instant) {
        if (this.current == null) {
            return;
        }
        if (!this.shouldSlideHeadAwayFromWalls()) {
            return;
        }
        float collisionAvoidanceRadius = 0.15f;
        this.slideAwayFromWalls(0.15f, instant, false);
    }

    public float getUnbalancedLevel() {
        return this.unbalancedLevel;
    }

    public void setUnbalancedLevel(float unbalancedLevel) {
        this.unbalancedLevel = unbalancedLevel;
    }

    private boolean getShouldAttack() {
        IsoPlayer player;
        IsoMovingObject vehicle;
        IsoGameCharacter gameCharacter;
        IsoMovingObject isoMovingObject;
        if (SystemDisabler.zombiesDontAttack) {
            return false;
        }
        if (this.isReanimatedForGrappleOnly()) {
            return false;
        }
        if (this.target == null || (isoMovingObject = this.target) instanceof IsoGameCharacter && (gameCharacter = (IsoGameCharacter)isoMovingObject).isZombiesDontAttack()) {
            return false;
        }
        isoMovingObject = this.target;
        if (isoMovingObject instanceof IsoGameCharacter) {
            IsoGameCharacter isoGameCharacter = (IsoGameCharacter)isoMovingObject;
            if (this.target.isOnFloor() && isoGameCharacter.getCurrentState() != BumpedState.instance()) {
                this.setTarget(null);
                return false;
            }
            vehicle = isoGameCharacter.getVehicle();
            if (vehicle != null) {
                return false;
            }
            if (isoGameCharacter.reanimatedCorpse != null) {
                return false;
            }
            if (isoGameCharacter.getStateMachine().getCurrent() == ClimbOverWallState.instance()) {
                return false;
            }
        }
        if (this.reanimate) {
            return false;
        }
        if (Math.abs(this.target.getZ() - this.getZ()) >= 0.2f) {
            return false;
        }
        vehicle = this.target;
        if (vehicle instanceof IsoPlayer && (player = (IsoPlayer)vehicle).isGhostMode()) {
            return false;
        }
        if (this.fakeDead) {
            return !this.isUnderVehicle() && this.DistTo(this.target) < 1.3f;
        }
        if (this.crawling) {
            return !this.isUnderVehicle() && this.DistTo(this.target) < 1.3f;
        }
        IsoGridSquare currentSquare = this.getCurrentSquare();
        IsoGridSquare targetCurrentSquare = this.target.getCurrentSquare();
        if (currentSquare != null && currentSquare.isSomethingTo(targetCurrentSquare) && !BentFences.getInstance().isUnbentObject(this.target)) {
            return false;
        }
        float attackRange = this.crawling ? 1.4f : 0.72f;
        float len = this.vectorToTarget.getLength();
        return len <= attackRange;
    }

    @Override
    public void actionStateChanged(ActionContext sender) {
        super.actionStateChanged(sender);
        if (this.networkAi != null && GameServer.server) {
            this.networkAi.extraUpdate();
        }
    }

    @Override
    protected void onAnimPlayerCreated(AnimationPlayer animationPlayer) {
        super.onAnimPlayerCreated(animationPlayer);
        animationPlayer.setSharedAnimRepo(m_sharedSkeleRepo);
    }

    @Override
    public String GetAnimSetName() {
        return this.crawling ? "zombie-crawler" : "zombie";
    }

    public void InitSpritePartsZombie() {
        SurvivorDesc d = this.descriptor;
        this.InitSpritePartsZombie(d);
    }

    public void InitSpritePartsZombie(SurvivorDesc desc) {
        this.sprite.disposeAnimation();
        this.legsSprite = this.sprite;
        this.legsSprite.name = desc.getTorso();
        this.zombieId = Rand.Next(10000);
        this.useParts = true;
    }

    @Override
    public void pathToCharacter(IsoGameCharacter target) {
        if (this.allowRepathDelay > 0.0f && (this.isCurrentState(PathFindState.instance()) || this.isCurrentState(WalkTowardState.instance()) || this.isCurrentState(WalkTowardNetworkState.instance()))) {
            return;
        }
        super.pathToCharacter(target);
    }

    @Override
    public void pathToLocationF(float x, float y, float z) {
        if (this.allowRepathDelay > 0.0f && (this.isCurrentState(PathFindState.instance()) || this.isCurrentState(WalkTowardState.instance()) || this.isCurrentState(WalkTowardNetworkState.instance()))) {
            return;
        }
        super.pathToLocationF(x, y, z);
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        super.load(input, worldVersion, isDebugSave);
        this.walkVariant = "ZombieWalk";
        this.spriteName = "BobZ";
        if (this.palette != 1) {
            this.spriteName = this.spriteName + this.palette;
        }
        SurvivorDesc desc = this.descriptor;
        this.setFemale(desc.isFemale());
        if (this.isFemale()) {
            this.spriteName = this.palette == 1 ? "KateZ" : "KateZ" + this.palette;
        }
        this.hurtSound = desc.getVoicePrefix() + "Hurt";
        this.InitSpritePartsZombie(desc);
        this.sprite.def.tintr = 0.95f + (float)Rand.Next(5) / 100.0f;
        this.sprite.def.tintg = 0.95f + (float)Rand.Next(5) / 100.0f;
        this.sprite.def.tintb = 0.95f + (float)Rand.Next(5) / 100.0f;
        this.setDefaultState(ZombieIdleState.instance());
        this.DoZombieStats();
        int loadedFileVersion = input.getInt();
        this.setWidth(0.3f);
        this.timeSinceSeenFlesh = input.getInt();
        this.setAlpha(0.0f);
        int stateFlags = input.getInt();
        if (loadedFileVersion == 0) {
            DebugLog.Zombie.debugln("Loading Zombie isFakeDead state:%d", stateFlags);
            this.setFakeDead(stateFlags == 1);
        } else if (loadedFileVersion >= 1) {
            ZombieStateFlags state = ZombieStateFlags.fromInt(stateFlags);
            DebugLog.Zombie.debugln("Loading Zombie state flags:%s", state);
            if (state.isFakeDead()) {
                this.setFakeDead(true);
            } else if (state.isCrawling()) {
                this.setCrawler(true);
                this.setCanWalk(state.isCanWalk());
                this.setOnFloor(true);
                this.setFallOnFront(true);
                this.walkVariant = "ZombieWalk";
                this.DoZombieStats();
            }
            if (state.isInitialized()) {
                this.setCanCrawlUnderVehicle(state.isCanCrawlUnderVehicle());
            }
            this.setReanimatedForGrappleOnly(state.isReanimatedForGrappleOnly());
        }
        ArrayList items = this.savedInventoryItems;
        int wornItemCount = input.get();
        for (int i = 0; i < wornItemCount; ++i) {
            ItemBodyLocation itemBodyLocation = ItemBodyLocation.get(ResourceLocation.of(GameWindow.ReadString(input)));
            short index = input.getShort();
            if (index < 0 || index >= items.size() || this.wornItems.getBodyLocationGroup().getLocation(itemBodyLocation) == null) continue;
            this.wornItems.setItem(itemBodyLocation, (InventoryItem)items.get(index));
        }
        this.setStateMachineLocked(false);
        this.setDefaultState();
        this.getCell().getZombieList().add(this);
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        DebugType.Saving.trace("Saving: %s", this);
        super.save(output, isDebugSave);
        output.putInt(1);
        output.putInt((int)this.timeSinceSeenFlesh);
        ZombieStateFlags state = ZombieStateFlags.fromZombie(this);
        output.putInt(state.asInt());
        if (this.wornItems.size() > 127) {
            throw new RuntimeException("too many worn items");
        }
        output.put((byte)this.wornItems.size());
        this.wornItems.forEach(wornItem -> {
            GameWindow.WriteString(output, wornItem.getLocation().toString());
            output.putShort((short)this.savedInventoryItems.indexOf(wornItem.getItem()));
        });
    }

    @Override
    public void collideWith(IsoObject obj) {
        if (this.ghost || obj == null) {
            return;
        }
        if (obj.rerouteCollide != null) {
            obj = this.rerouteCollide;
        }
        State state = this.getCurrentState();
        boolean bMoving = this.isCurrentState(PathFindState.instance()) || this.isCurrentState(LungeState.instance()) || this.isCurrentState(LungeNetworkState.instance()) || this.isCurrentState(WalkTowardState.instance()) || this.isCurrentState(WalkTowardNetworkState.instance());
        IsoWindow window = Type.tryCastTo(obj, IsoWindow.class);
        if (window != null && window.canClimbThrough(this) && bMoving) {
            if (!this.isFacingObject(window, 0.8f)) {
                super.collideWith(obj);
                return;
            }
            if (state != PathFindState.instance() && !this.crawling) {
                this.climbThroughWindow(window);
            }
        } else {
            IsoThumpable isoThumpable;
            IsoWindowFrame windowFrame;
            if (bMoving && !this.crawling && obj instanceof IsoWindowFrame && (windowFrame = (IsoWindowFrame)obj).canClimbThrough(this)) {
                if (!this.isFacingObject(windowFrame, 0.8f)) {
                    super.collideWith(obj);
                    return;
                }
                if (state != PathFindState.instance()) {
                    this.climbThroughWindowFrame(windowFrame);
                }
                return;
            }
            if (obj instanceof IsoThumpable && (isoThumpable = (IsoThumpable)obj).canClimbThrough(this) && bMoving) {
                if (state != PathFindState.instance() && !this.crawling) {
                    this.climbThroughWindow(isoThumpable);
                }
            } else if (!(obj instanceof IsoDoor && obj.isHoppable() || obj == null || obj.getThumpableFor(this) == null || !bMoving)) {
                boolean chasingSound;
                boolean bl = chasingSound = (this.isCurrentState(PathFindState.instance()) || this.isCurrentState(WalkTowardState.instance()) || this.isCurrentState(WalkTowardNetworkState.instance())) && this.getPathFindBehavior2().isGoalSound();
                if (SandboxOptions.instance.lore.thumpNoChasing.getValue() || this.target != null || chasingSound) {
                    if (obj instanceof IsoThumpable && !SandboxOptions.instance.lore.thumpOnConstruction.getValue()) {
                        return;
                    }
                    Thumpable thumpie = obj;
                    if (obj instanceof IsoWindow && obj.getThumpableFor(this) != null && obj.isDestroyed()) {
                        thumpie = obj.getThumpableFor(this);
                    }
                    this.setThumpTarget(thumpie);
                } else {
                    this.setVariable("bPathfind", false);
                    this.setVariable("bMoving", false);
                }
                this.setPath2(null);
            }
        }
        super.collideWith(obj);
    }

    @Override
    public float Hit(HandWeapon weapon, IsoGameCharacter wielder, float damageSplit, boolean bIgnoreDamage, float modDelta, boolean bRemote) {
        if ((Core.tutorial || Core.debug) && this.immortalTutorialZombie) {
            return 0.0f;
        }
        BodyPartType bodyPart = BodyPartType.FromIndex(Rand.Next(BodyPartType.ToIndex(BodyPartType.Torso_Upper), BodyPartType.ToIndex(BodyPartType.Torso_Lower) + 1));
        if (Rand.NextBool(7)) {
            bodyPart = BodyPartType.Head;
        }
        if (wielder.isCriticalHit() && Rand.NextBool(3)) {
            bodyPart = BodyPartType.Head;
        }
        LuaEventManager.triggerEvent("OnHitZombie", this, wielder, (Object)bodyPart, weapon);
        this.lastHitPart = bodyPart;
        wielder.setLastHitCharacter(this);
        float damage = super.Hit(weapon, wielder, damageSplit, bIgnoreDamage, modDelta, bRemote);
        if (GameServer.server && !this.isRemoteZombie()) {
            this.addAggro(wielder, damage);
        }
        this.timeSinceSeenFlesh = 0.0f;
        if (!this.isDead() && !this.isOnFloor() && !bIgnoreDamage && weapon != null && wielder instanceof IsoPlayer && this.DistToProper(wielder) <= 0.9f && (this.isCurrentState(AttackState.instance()) || this.isCurrentState(AttackNetworkState.instance()) || this.isCurrentState(LungeState.instance()) || this.isCurrentState(LungeNetworkState.instance()))) {
            this.setHitForce(0.5f);
            this.changeState(StaggerBackState.instance());
        }
        if (GameServer.server || GameClient.client && this.isDead()) {
            this.lastPlayerHit = wielder.getOnlineID();
        }
        return damage;
    }

    public void onMouseLeftClick() {
        if (IsoPlayer.getInstance() == null || IsoPlayer.getInstance().isAiming()) {
            return;
        }
        if (IsoPlayer.getInstance().IsAttackRange(this.getX(), this.getY(), this.getZ())) {
            Vector2 vec = new Vector2(this.getX(), this.getY());
            vec.x -= IsoPlayer.getInstance().getX();
            vec.y -= IsoPlayer.getInstance().getY();
            vec.normalize();
            IsoPlayer.getInstance().DirectionFromVector(vec);
            IsoPlayer.getInstance().AttemptAttack();
        }
    }

    public void onZombieGrappleEnded() {
        if (this.isReanimatedForGrappleOnly()) {
            DebugLog.Grapple.debugln("Reanimated Corpse Dropped.");
            if (GameClient.client) {
                this.setOnFloor(true);
                this.setHealth(0.0f);
            } else if (GameServer.server) {
                this.die();
            }
        }
    }

    private void renderAtlasTexture(float x, float y, float z) {
        if (this.atlasTex == null) {
            return;
        }
        if (IsoSprite.globalOffsetX == -1.0f) {
            IsoSprite.globalOffsetX = -IsoCamera.frameState.offX;
            IsoSprite.globalOffsetY = -IsoCamera.frameState.offY;
        }
        float ssx = IsoUtils.XToScreen(x, y, z, 0);
        float ssy = IsoUtils.YToScreen(x, y, z, 0);
        this.sx = ssx;
        this.sy = ssy;
        ssx = this.sx + IsoSprite.globalOffsetX;
        ssy = this.sy + IsoSprite.globalOffsetY;
        ColorInfo col = inf.set(1.0f, 1.0f, 1.0f, 1.0f);
        if (this.getCurrentSquare() != null) {
            this.getCurrentSquare().interpolateLight(col, x - (float)this.getCurrentSquare().getX(), y - (float)this.getCurrentSquare().getY());
        }
        this.atlasTex.render(x, y, z, (int)ssx, (int)ssy, col.r, col.g, col.b, col.a);
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoChild, boolean bWallLightingPass, Shader shader) {
        if (this.getCurrentState() == FakeDeadZombieState.instance()) {
            if (this.dressInRandomOutfit) {
                ModelManager.instance.dressInRandomOutfit(this);
            }
            if (this.atlasTex == null) {
                if (this.hasAnimationPlayer()) {
                    this.getAnimationPlayer().Update(0.0f);
                }
                this.atlasTex = DeadBodyAtlas.instance.getBodyTexture(this);
                DeadBodyAtlas.instance.render();
            }
            if (this.atlasTex != null) {
                this.renderAtlasTexture(x, y, z);
            }
            return;
        }
        if (this.atlasTex != null) {
            this.atlasTex = null;
        }
        if (IsoCamera.getCameraCharacter() != IsoPlayer.getInstance()) {
            this.setAlphaAndTarget(1.0f);
        }
        super.render(x, y, z, col, bDoChild, bWallLightingPass, shader);
    }

    @Override
    public void renderlast() {
        super.renderlast();
        if (DebugOptions.instance.zombieRenderCanCrawlUnderVehicle.getValue() && this.isCanCrawlUnderVehicle()) {
            this.renderTextureOverHead("media/ui/FavoriteStar.png");
        }
        if (DebugOptions.instance.zombieRenderMemory.getValue()) {
            String texName = this.target == null ? "media/ui/Moodles/Moodle_Icon_Bored.png" : (this.bonusSpotTime == 0.0f ? "media/ui/Moodles/Moodle_Icon_Angry.png" : "media/ui/Moodles/Moodle_Icon_Zombie.png");
            this.renderTextureOverHead(texName);
            int sx1 = (int)IsoUtils.XToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
            int sy1 = (int)IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
            int fontHgt = TextManager.instance.getFontFromEnum(UIFont.Small).getLineHeight();
            TextManager.instance.DrawString(sx1, sy1 += fontHgt, "AllowRepathDelay : " + this.allowRepathDelay);
            TextManager.instance.DrawString(sx1, sy1 += fontHgt, "BonusSpotTime : " + this.bonusSpotTime);
            TextManager.instance.DrawString(sx1, sy1 += fontHgt, "TimeSinceSeenFlesh : " + this.timeSinceSeenFlesh);
        }
        if (DebugOptions.instance.zombieRenderViewDistance.getValue()) {
            if (this.target == null) {
                this.updateVisionRadius();
                LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), this.visionRadiusResult, 32, 1.0f, 1.0f, 1.0f, 0.3f);
            } else if (this.bonusSpotTime == 0.0f) {
                LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), this.visionRadiusResult, 32, 1.0f, 1.0f, 0.0f, 0.3f);
            } else {
                LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), this.visionRadiusResult, 32, 1.0f, 0.0f, 0.0f, 0.3f);
            }
        }
    }

    @Override
    protected boolean renderTextureInsteadOfModel(float x, float y) {
        boolean bMoving = this.isCurrentState(WalkTowardState.instance()) || this.isCurrentState(PathFindState.instance());
        String animSet = "zombie";
        String stateName = bMoving ? "walktoward" : "idle";
        int frames = 4;
        int frame = (int)(this.characterTextureAnimTime / this.characterTextureAnimDuration * 4.0f);
        float trackTime = (bMoving ? 0.67f : 1.0f) * ((float)frame / 4.0f);
        DeadBodyAtlas.BodyTexture tex = DeadBodyAtlas.instance.getBodyTexture(this.isFemale(), "zombie", stateName, this.getDir(), frame, trackTime);
        if (tex != null) {
            float sx = IsoUtils.XToScreen(x, y, this.getZ(), 0);
            float sy = IsoUtils.YToScreen(x, y, this.getZ(), 0);
            sx -= IsoCamera.getOffX();
            sy -= IsoCamera.getOffY();
            int playerIndex = IsoCamera.frameState.playerIndex;
            if (PerformanceSettings.fboRenderChunk) {
                IndieGL.enableDepthTest();
                IndieGL.glBlendFunc(770, 771);
                IndieGL.glDepthFunc(515);
                IndieGL.StartShader(0);
                TextureDraw.nextZ = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)(x + 0.3f), (float)(y + 0.3f), (float)(this.getZ() + 0.3f)).depthStart * 2.0f - 1.0f;
            }
            tex.render(x, y, this.getZ(), sx, sy, 0.0f, 0.0f, 0.0f, this.getAlpha(playerIndex));
        }
        if (DebugOptions.instance.character.debug.render.angle.getValue()) {
            tempo.set(this.dir.ToVector());
            this.drawDirectionLine(tempo, 1.2f, 0.0f, 1.0f, 0.0f);
        }
        return true;
    }

    private void renderTextureOverHead(String textureName) {
        float renderX = this.getX();
        float renderY = this.getY();
        float sx = IsoUtils.XToScreen(renderX, renderY, this.getZ(), 0);
        float sy = IsoUtils.YToScreen(renderX, renderY, this.getZ(), 0);
        sx = sx - IsoCamera.getOffX() - this.offsetX;
        sy = sy - IsoCamera.getOffY() - this.offsetY;
        sy -= 128.0f / (2.0f / (float)Core.tileScale);
        Texture tex = Texture.getSharedTexture(textureName);
        float zoom = Core.getInstance().getZoom(IsoCamera.frameState.playerIndex);
        zoom = Math.max(zoom, 1.0f);
        int texW = (int)((float)tex.getWidth() * zoom);
        int texH = (int)((float)tex.getHeight() * zoom);
        tex.render((float)((int)sx) - (float)texW / 2.0f, (int)sy - texH, texW, texH);
    }

    @Override
    protected void updateAlpha(int playerIndex, float mul, float div) {
        if (this.isFakeDead()) {
            this.setAlphaAndTarget(1.0f);
            return;
        }
        super.updateAlpha(playerIndex, mul, div);
    }

    public boolean isRespondingToPlayerSound() {
        return this.timeSinceRespondToSound < 1000.0f && (this.soundSourceIsPlayer || this.soundSourceIsPlayerBase);
    }

    public boolean isMovingToPlayerSound() {
        if ((this.getCurrentState() == PathFindState.instance() || this.getCurrentState() == WalkTowardState.instance()) && this.getPathFindBehavior2().isGoalSound()) {
            return this.soundSourceIsPlayer || this.soundSourceIsPlayerBase;
        }
        return false;
    }

    public void RespondToSound() {
        float dist;
        float dist2;
        if (this.ghost) {
            return;
        }
        if (this.isUseless()) {
            return;
        }
        if (GameServer.server) {
            return;
        }
        if (GameClient.client && this.isRemoteZombie()) {
            return;
        }
        if ((this.getCurrentState() == PathFindState.instance() || this.getCurrentState() == WalkTowardState.instance()) && this.getPathFindBehavior2().isGoalSound() && PZMath.fastfloor(this.getZ()) == this.getPathTargetZ() && this.soundSourceRepeating && (dist2 = this.DistToSquared(this.getPathTargetX(), this.getPathTargetY())) < 16.0f && LosUtil.lineClear(this.getCell(), PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), this.getPathTargetX(), this.getPathTargetY(), PZMath.fastfloor(this.getZ()), false) != LosUtil.TestResults.Blocked && !this.isNearSirenVehicle()) {
            this.setVariable("bPathfind", false);
            this.setVariable("bMoving", false);
            this.setPath2(null);
        }
        if (this.soundReactDelay > 0.0f) {
            this.soundReactDelay -= GameTime.getInstance().getThirtyFPSMultiplier();
            if (this.soundReactDelay < 0.0f) {
                this.soundReactDelay = 0.0f;
            }
            if (this.soundReactDelay > 0.0f) {
                return;
            }
        }
        float tmpSoundAttract = 0.0f;
        Object tmpSoundSourceTarget = null;
        WorldSoundManager.WorldSound sound = WorldSoundManager.instance.getSoundZomb(this);
        float attract = WorldSoundManager.instance.getSoundAttract(sound, this);
        if (attract <= 0.0f) {
            sound = null;
        }
        if (sound != null) {
            tmpSoundAttract = attract;
            tmpSoundSourceTarget = sound.source;
            this.soundAttract = tmpSoundAttract;
            this.soundAttractTimeout = 60.0f;
        } else if (this.soundAttractTimeout > 0.0f) {
            this.soundAttractTimeout -= GameTime.getInstance().getThirtyFPSMultiplier();
            if (this.soundAttractTimeout < 0.0f) {
                this.soundAttractTimeout = 0.0f;
            }
        }
        WorldSoundManager.ResultBiggestSound soundB = WorldSoundManager.instance.getBiggestSoundZomb(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), true, this);
        if (soundB.sound != null && (this.soundAttractTimeout == 0.0f || this.soundAttract * 2.0f < soundB.attract)) {
            sound = soundB.sound;
            tmpSoundAttract = soundB.attract;
            tmpSoundSourceTarget = sound.source;
        }
        if (sound != null && sound.repeating && sound.z == PZMath.fastfloor(this.getZ()) && (dist = this.DistToSquared(sound.x, sound.y)) < 25.0f && LosUtil.lineClear(this.getCell(), PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), sound.x, sound.y, PZMath.fastfloor(this.getZ()), false) != LosUtil.TestResults.Blocked) {
            sound = null;
        }
        if (this.shouldStopThumpingToRespondToSound(sound)) {
            this.setThumpTarget(null);
        }
        if (sound != null) {
            this.soundAttract = tmpSoundAttract;
            this.soundSourceTarget = tmpSoundSourceTarget;
            this.soundReactDelay = Rand.Next(0, 16);
            this.delayedSound.x = sound.x;
            this.delayedSound.y = sound.y;
            this.delayedSound.z = sound.z;
            this.soundSourceRepeating = sound.repeating;
            this.soundSourceIsPlayer = sound.sourceIsPlayer;
            this.soundSourceIsPlayerBase = sound.sourceIsPlayerBase;
        }
        if (this.delayedSound.x != -1 && this.soundReactDelay == 0.0f) {
            int soundX = this.delayedSound.x;
            int soundY = this.delayedSound.y;
            int soundZ = this.delayedSound.z;
            this.delayedSound.x = -1;
            float dist3 = IsoUtils.DistanceManhatten(this.getX(), this.getY(), soundX, soundY) / 2.5f;
            soundX += Rand.Next((int)(-dist3), (int)dist3);
            soundY += Rand.Next((int)(-dist3), (int)dist3);
            if ((this.getCurrentState() == PathFindState.instance() || this.getCurrentState() == WalkTowardState.instance()) && (this.getPathFindBehavior2().isGoalLocation() || this.getPathFindBehavior2().isGoalSound())) {
                if (!IsoUtils.isSimilarDirection(this, soundX, soundY, this.getPathFindBehavior2().getTargetX(), this.getPathFindBehavior2().getTargetY(), 0.5f)) {
                    this.setTurnAlertedValues(soundX, soundY);
                    this.pathToSound(soundX, soundY, soundZ);
                    this.setLastHeardSound(this.getPathTargetX(), this.getPathTargetY(), this.getPathTargetZ());
                    this.allowRepathDelay = 120.0f;
                    this.timeSinceRespondToSound = 0.0f;
                }
                return;
            }
            if (this.timeSinceRespondToSound < 60.0f) {
                return;
            }
            if (!IsoUtils.isSimilarDirection(this, soundX, soundY, this.getX() + this.getForwardDirectionX(), this.getY() + this.getForwardDirectionY(), 0.5f)) {
                this.setTurnAlertedValues(soundX, soundY);
            }
            if (LosUtil.lineClear(this.getCell(), this.getXi(), this.getYi(), this.getZi(), soundX, soundY, soundZ, false) != LosUtil.TestResults.Blocked) {
                this.pathToSound(soundX, soundY, soundZ);
                this.setLastHeardSound(this.getPathTargetX(), this.getPathTargetY(), this.getPathTargetZ());
            } else {
                int offsetHearing = 2;
                if (ClimateManager.getInstance().getRainIntensity() > 0.5f) {
                    offsetHearing = 5;
                }
                if (SandboxOptions.instance.lore.hearing.getValue() == 1) {
                    offsetHearing -= 2;
                }
                if (SandboxOptions.instance.lore.hearing.getValue() == 3) {
                    offsetHearing += 2;
                }
                offsetHearing = Math.max(Math.min(10, offsetHearing), 2);
                int heardX = -offsetHearing + (int)(Math.random() * (double)(offsetHearing * 2 + 1));
                int heardY = -offsetHearing + (int)(Math.random() * (double)(offsetHearing * 2 + 1));
                this.pathToSound(soundX + heardX, soundY + heardY, soundZ);
                this.setLastHeardSound(this.getPathTargetX() + heardX, this.getPathTargetY() + heardY, this.getPathTargetZ());
            }
            this.allowRepathDelay = 120.0f;
            this.timeSinceRespondToSound = 0.0f;
        }
    }

    private boolean shouldStopThumpingToRespondToSound(WorldSoundManager.WorldSound sound) {
        if (sound == null) {
            return false;
        }
        if (sound.sourceIsZombie) {
            return false;
        }
        if (sound.radius < 10) {
            return false;
        }
        if (!this.isCurrentState(ThumpState.instance())) {
            return false;
        }
        IsoMovingObject isoMovingObject = this.getTarget();
        if (isoMovingObject instanceof IsoGameCharacter) {
            IsoGameCharacter targetChr = (IsoGameCharacter)isoMovingObject;
            if (this.isTargetLocationKnown() && this.getDotWithForwardDirection(targetChr.getX(), targetChr.getY()) > 0.0f) {
                return false;
            }
            if (!this.isTargetLocationKnown() && this.getDotWithForwardDirection(this.lastTargetSeenX, this.lastTargetSeenY) > 0.0f) {
                return false;
            }
        }
        return this.getDotWithForwardDirection(sound.x, sound.y) < 0.0f;
    }

    public void setTurnAlertedValues(int soundX, int soundY) {
        Vector2 dirOfSound = new Vector2(this.getX() - ((float)soundX + 0.5f), this.getY() - ((float)soundY + 0.5f));
        float radDirOfSound = dirOfSound.getDirectionNeg();
        radDirOfSound = radDirOfSound < 0.0f ? Math.abs(radDirOfSound) : (float)(Math.PI * 2 - (double)radDirOfSound);
        double degreeOfSound = Math.toDegrees(radDirOfSound);
        Vector2 zombieAngle = new Vector2(this.getDir().Rot180().ToVector().x, this.getDir().Rot180().ToVector().y);
        zombieAngle.normalize();
        float radDirOfZombie = zombieAngle.getDirectionNeg();
        radDirOfZombie = radDirOfZombie < 0.0f ? Math.abs(radDirOfZombie) : (float)Math.PI * 2 - radDirOfZombie;
        double degreeOfZombie = Math.toDegrees(radDirOfZombie);
        if ((int)degreeOfZombie == 360) {
            degreeOfZombie = 0.0;
        }
        if ((int)degreeOfSound == 360) {
            degreeOfSound = 0.0;
        }
        String trueDegree = "0";
        if (degreeOfSound > degreeOfZombie) {
            int sumUpDegree = (int)(degreeOfSound - degreeOfZombie);
            if (sumUpDegree > 350 || sumUpDegree <= 35) {
                trueDegree = "45R";
            }
            if (sumUpDegree > 35 && sumUpDegree <= 80) {
                trueDegree = "90R";
            }
            if (sumUpDegree > 80 && sumUpDegree <= 125) {
                trueDegree = "135R";
            }
            if (sumUpDegree > 125 && sumUpDegree <= 170) {
                trueDegree = "180R";
            }
            if (sumUpDegree > 170 && sumUpDegree < 215) {
                trueDegree = "180L";
            }
            if (sumUpDegree >= 215 && sumUpDegree < 260) {
                trueDegree = "135L";
            }
            if (sumUpDegree >= 260 && sumUpDegree < 305) {
                trueDegree = "90L";
            }
            if (sumUpDegree >= 305 && sumUpDegree < 350) {
                trueDegree = "45L";
            }
        } else {
            int sumUpDegree = (int)(degreeOfZombie - degreeOfSound);
            if (sumUpDegree > 10 && sumUpDegree <= 55) {
                trueDegree = "45L";
            }
            if (sumUpDegree > 55 && sumUpDegree <= 100) {
                trueDegree = "90L";
            }
            if (sumUpDegree > 100 && sumUpDegree <= 145) {
                trueDegree = "135L";
            }
            if (sumUpDegree > 145 && sumUpDegree <= 190) {
                trueDegree = "180L";
            }
            if (sumUpDegree > 190 && sumUpDegree < 235) {
                trueDegree = "180R";
            }
            if (sumUpDegree >= 235 && sumUpDegree < 280) {
                trueDegree = "135R";
            }
            if (sumUpDegree >= 280 && sumUpDegree < 325) {
                trueDegree = "90R";
            }
            if (sumUpDegree >= 325 || sumUpDegree < 10) {
                trueDegree = "45R";
            }
        }
        this.setVariable("turnalertedvalue", trueDegree);
        ZombieTurnAlerted.instance().setParams((IsoGameCharacter)this, dirOfSound.set((float)soundX + 0.5f - this.getX(), (float)soundY + 0.5f - this.getY()).getDirection());
        this.alerted = true;
        this.networkAi.extraUpdate();
    }

    public boolean getAttackDidDamage() {
        return this.attackDidDamage;
    }

    public void setAttackDidDamage(boolean attackDidDamage) {
        this.attackDidDamage = attackDidDamage;
    }

    public String getAttackOutcome() {
        return this.attackOutcome;
    }

    public void setAttackOutcome(String attackOutcome) {
        this.attackOutcome = attackOutcome;
    }

    public void setReanimatedForGrappleOnly(boolean val) {
        this.reanimatedForGrappleOnly = val;
    }

    public boolean isReanimatedForGrappleOnly() {
        return this.reanimatedForGrappleOnly;
    }

    public void clearAggroList() {
        try {
            Arrays.fill(this.aggroList, null);
        }
        catch (Exception e) {
            return;
        }
    }

    private void processAggroList() {
        try {
            for (int i = 0; i < this.aggroList.length; ++i) {
                if (this.aggroList[i] == null || !(this.aggroList[i].getAggro() <= 0.0f)) continue;
                this.aggroList[i] = null;
                return;
            }
        }
        catch (Exception e) {
            return;
        }
    }

    public void addAggro(IsoMovingObject other, float damage) {
        try {
            int i;
            if (this.aggroList[0] == null) {
                this.aggroList[0] = new Aggro(other, damage);
                return;
            }
            for (i = 0; i < this.aggroList.length; ++i) {
                if (this.aggroList[i] == null || this.aggroList[i].obj != other) continue;
                this.aggroList[i].addDamage(damage);
                return;
            }
            for (i = 0; i < this.aggroList.length; ++i) {
                if (this.aggroList[i] != null) continue;
                this.aggroList[i] = new Aggro(other, damage);
                return;
            }
        }
        catch (Exception e) {
            return;
        }
    }

    public boolean isLeadAggro(IsoMovingObject other) {
        try {
            if (this.aggroList[0] == null) {
                return false;
            }
            this.processAggroList();
            if (this.aggroList[0] == null) {
                return false;
            }
            IsoMovingObject lead = this.aggroList[0].obj;
            float leadAggro = this.aggroList[0].getAggro();
            for (int i = 1; i < this.aggroList.length; ++i) {
                if (this.aggroList[i] == null) continue;
                if (leadAggro >= 1.0f && this.aggroList[i].getAggro() >= 1.0f) {
                    return false;
                }
                if (this.aggroList[i] == null || !(leadAggro < this.aggroList[i].getAggro())) continue;
                lead = this.aggroList[i].obj;
                leadAggro = this.aggroList[i].getAggro();
            }
            return other == lead && leadAggro == 1.0f;
        }
        catch (Exception e) {
            return false;
        }
    }

    private Float closeSneakBonusCoeff(IsoGridSquare sq, IsoDirections dir) {
        if (sq == null) {
            return null;
        }
        PZArrayList<IsoObject> list = sq.getObjects();
        for (int i = 0; i < list.size(); ++i) {
            IsoSprite sprite = list.get(i).getSprite();
            if (sprite == null || sprite.getName() == null || !temporaryMapCloseSneakBonusDir.containsKey(sprite.getName()) || !temporaryMapCloseSneakBonusValue.containsKey(sprite.getName())) continue;
            try {
                if (dir == IsoDirections.N && temporaryMapCloseSneakBonusDir.get(sprite.getName()).contains("N")) {
                    return Float.valueOf(temporaryMapCloseSneakBonusValue.get(sprite.getName()).floatValue() / 100.0f);
                }
                if (dir == IsoDirections.S && temporaryMapCloseSneakBonusDir.get(sprite.getName()).contains("S")) {
                    return Float.valueOf(temporaryMapCloseSneakBonusValue.get(sprite.getName()).floatValue() / 100.0f);
                }
                if (dir == IsoDirections.W && temporaryMapCloseSneakBonusDir.get(sprite.getName()).contains("W")) {
                    return Float.valueOf(temporaryMapCloseSneakBonusValue.get(sprite.getName()).floatValue() / 100.0f);
                }
                if (dir != IsoDirections.E || !temporaryMapCloseSneakBonusDir.get(sprite.getName()).contains("E")) continue;
                return Float.valueOf(temporaryMapCloseSneakBonusValue.get(sprite.getName()).floatValue() / 100.0f);
            }
            catch (Exception e) {
                DebugLog.General.println("Problem with tile property CloseSneakBonus" + sprite.getName());
            }
        }
        return null;
    }

    private float getObstacleMod(IsoGridSquare sq, IsoDirections dir) {
        IsoDirections nextDir;
        Float currentSquareCoeff = this.closeSneakBonusCoeff(sq, dir);
        if (currentSquareCoeff != null) {
            return 1.0f - currentSquareCoeff.floatValue();
        }
        IsoGridSquare nextSq = sq.getAdjacentSquare(dir);
        Float nextSquareCoeff = this.closeSneakBonusCoeff(nextSq, nextDir = dir == IsoDirections.S ? IsoDirections.N : (dir == IsoDirections.E ? IsoDirections.W : dir));
        if (nextSquareCoeff != null) {
            return 1.0f - nextSquareCoeff.floatValue();
        }
        return 1.0f;
    }

    public void spottedNew(IsoMovingObject other, boolean bForced) {
        IsoGameCharacter gameCharacter;
        IsoMovingObject isoMovingObject;
        float distCurrent;
        float distOther;
        IsoMovingObject isoMovingObject2;
        if (GameClient.client && this.isRemoteZombie()) {
            if (this.getTarget() != null) {
                this.vectorToTarget.x = this.getTarget().getX();
                this.vectorToTarget.y = this.getTarget().getY();
                this.vectorToTarget.x -= this.getX();
                this.vectorToTarget.y -= this.getY();
                if (this.isCurrentState(LungeNetworkState.instance())) {
                    Vector2 temp = new Vector2();
                    temp.x = this.vectorToTarget.x;
                    temp.y = this.vectorToTarget.y;
                    temp.normalize();
                    this.DirectionFromVector(temp);
                    this.setForwardDirectionFromIsoDirection();
                    this.setForwardDirection(temp);
                }
            }
            return;
        }
        if (this.getCurrentSquare() == null) {
            return;
        }
        if (other.getCurrentSquare() == null) {
            return;
        }
        if (GameClient.client && !GameClient.connection.isReady()) {
            this.setTarget(null);
            this.spottedLast = null;
            return;
        }
        if (this.isReanimatedForGrappleOnly()) {
            this.setTarget(null);
            this.spottedLast = null;
            return;
        }
        if (this.isUseless()) {
            this.setTarget(null);
            this.spottedLast = null;
            return;
        }
        if (this.getCurrentSquare().getProperties().has(IsoFlagType.smoke)) {
            this.setTarget(null);
            this.spottedLast = null;
            return;
        }
        IsoGameCharacter otherCharacter = Type.tryCastTo(other, IsoGameCharacter.class);
        if (otherCharacter == null || otherCharacter.isDead()) {
            return;
        }
        IsoPlayer otherPlayer = Type.tryCastTo(other, IsoPlayer.class);
        if (otherPlayer != null && otherPlayer.isGhostMode()) {
            return;
        }
        if ((GameClient.client || GameServer.server) && otherPlayer != null && otherPlayer.networkAi.isDisconnected()) {
            return;
        }
        if (this.getCurrentSquare() == null) {
            this.ensureOnTile();
        }
        if (other.getCurrentSquare() == null) {
            other.ensureOnTile();
        }
        float chance = 100.0f;
        int playerIndex = otherPlayer != null && !GameServer.server ? otherPlayer.playerIndex : 0;
        float lightData = (other.getCurrentSquare().lighting[playerIndex].lightInfo().r + other.getCurrentSquare().lighting[playerIndex].lightInfo().g + other.getCurrentSquare().lighting[playerIndex].lightInfo().b) / 3.0f;
        float lightMod = lightData = Math.max(0.0f, Math.min(1.0f, lightData));
        chance *= lightMod;
        if (other.getCurrentSquare().getZ() != this.current.getZ()) {
            int dif = Math.abs(other.getCurrentSquare().getZ() - this.current.getZ()) * 5 + 1;
            chance /= (float)dif;
        }
        Vector2 thisToOther = IsoGameCharacter.tempo;
        thisToOther.x = other.getX();
        thisToOther.y = other.getY();
        thisToOther.x -= this.getX();
        thisToOther.y -= this.getY();
        float viewDist = GameTime.getInstance().getViewDist();
        if (thisToOther.getLength() > viewDist) {
            return;
        }
        if (GameServer.server) {
            this.indoorZombie = false;
        }
        if (thisToOther.getLength() < viewDist) {
            viewDist = thisToOther.getLength();
        }
        if ((viewDist *= 1.1f) > GameTime.getInstance().getViewDistMax()) {
            viewDist = GameTime.getInstance().getViewDistMax();
        }
        thisToOther.normalize();
        Vector2 thisForward = this.getLookVector(tempo2);
        float cosAngle = thisForward.dot(thisToOther);
        this.updateVisionRadius();
        if (this.DistTo(other) > this.visionRadiusResult) {
            chance = 0.0f;
        }
        if (viewDist > 0.5f) {
            chance = cosAngle < -0.4f ? 0.0f : (cosAngle < -0.2f ? (chance /= 8.0f) : (cosAngle < -0.0f ? (chance /= 4.0f) : (cosAngle < 0.2f ? (chance /= 2.0f) : (cosAngle <= 0.4f ? (chance *= 2.0f) : (cosAngle <= 0.6f ? (chance *= 8.0f) : (cosAngle <= 0.8f ? (chance *= 16.0f) : (chance *= 32.0f)))))));
        }
        if (chance > 0.0f && (isoMovingObject2 = this.target) instanceof IsoPlayer) {
            IsoPlayer player = (IsoPlayer)isoMovingObject2;
            if (!GameServer.server && player.remoteId == -1 && this.current.isCanSee(player.playerIndex)) {
                player.targetedByZombie = true;
                player.lastTargeted = 0.0f;
            }
        }
        float movementData = other.getMovementLastFrame().getLength();
        float movementMod = 0.8f;
        if (movementData == 0.5f) {
            movementMod = 1.0f;
        } else if (movementData == 1.0f) {
            movementMod = 1.5f;
        } else if (movementData == 1.5f) {
            movementMod = 2.0f;
        }
        chance *= movementMod;
        if (viewDist < 5.0f && (!otherCharacter.isRunning() && !otherCharacter.isSneaking() && !otherCharacter.isAiming() || otherCharacter.isRunning())) {
            chance *= 3.0f;
        }
        float sneakingMod = otherCharacter.getSneakSpotMod();
        if (otherPlayer != null && !otherPlayer.isSneaking()) {
            sneakingMod = 1.0f;
        }
        chance *= sneakingMod;
        if (this.spottedLast != other || this.timeSinceSeenFlesh < 120.0f) {
            // empty if block
        }
        if (this.target != other && this.target != null && (distOther = IsoUtils.DistanceManhatten(this.getX(), this.getY(), other.getX(), other.getY())) > (distCurrent = IsoUtils.DistanceManhatten(this.getX(), this.getY(), this.target.getX(), this.target.getY()))) {
            return;
        }
        if (bForced) {
            chance = 1000000.0f;
        }
        if (this.bonusSpotTime > 0.0f) {
            chance *= 5.0f;
        }
        if (this.sight == 1) {
            chance *= 2.5f;
        }
        if (this.sight == 3) {
            chance *= 0.45f;
        }
        if (this.inactive) {
            chance *= 0.25f;
        }
        float traitMod = 1.0f;
        if (otherPlayer != null && otherPlayer.hasTrait(CharacterTrait.INCONSPICUOUS)) {
            traitMod = 0.8f;
        }
        if (otherPlayer != null && otherPlayer.hasTrait(CharacterTrait.CONSPICUOUS)) {
            traitMod = 1.2f;
        }
        chance *= traitMod;
        float shelterMod = 1.0f;
        if (this.getCurrentSquare() != other.getCurrentSquare() && otherPlayer != null && otherPlayer.isSneaking()) {
            int yDiff;
            int xDiff = Math.abs(this.getCurrentSquare().getX() - other.getCurrentSquare().getX());
            shelterMod = xDiff > (yDiff = Math.abs(this.getCurrentSquare().getY() - other.getCurrentSquare().getY())) ? (this.getCurrentSquare().getX() - other.getCurrentSquare().getX() > 0 ? this.getObstacleMod(other.getCurrentSquare(), IsoDirections.E) : this.getObstacleMod(other.getCurrentSquare(), IsoDirections.W)) : (this.getCurrentSquare().getY() - other.getCurrentSquare().getY() > 0 ? this.getObstacleMod(other.getCurrentSquare(), IsoDirections.S) : this.getObstacleMod(other.getCurrentSquare(), IsoDirections.N));
        }
        chance *= shelterMod;
        boolean intersectVision = false;
        Vector3f start = BaseVehicle.allocVector3f();
        Vector3f end = BaseVehicle.allocVector3f();
        Vector3f intersect1 = BaseVehicle.allocVector3f();
        for (int i = 0; i < IsoWorld.instance.currentCell.getVehicles().size(); ++i) {
            BaseVehicle vehicle = IsoWorld.instance.currentCell.getVehicles().get(i);
            start.set(otherCharacter.getX(), otherCharacter.getY(), otherCharacter.getZ() + 0.1f);
            end.set(this.getX(), this.getY(), this.getZ() + 0.1f);
            Vector3f intersect = vehicle.getIntersectPoint(start, end, intersect1);
            if (intersect == null) continue;
            intersectVision = true;
            break;
        }
        BaseVehicle.releaseVector3f(start);
        BaseVehicle.releaseVector3f(end);
        BaseVehicle.releaseVector3f(intersect1);
        thisToOther.x = other.getX();
        thisToOther.y = other.getY();
        thisToOther.x -= this.getX();
        thisToOther.y -= this.getY();
        float carObstacleMod = intersectVision && otherPlayer.getVehicle() == null ? (thisToOther.getLength() < 1.5f ? 0.5f : 0.0f) : 1.0f;
        chance *= carObstacleMod;
        chance /= this.getWornItemsVisionModifier();
        if (this.getEatBodyTarget() != null && chance > 0.0f) {
            chance *= 0.5f;
        }
        chance = PZMath.fastfloor(chance);
        boolean success = false;
        chance = Math.min(chance, 400.0f);
        chance /= 400.0f;
        chance = Math.max(0.0f, chance);
        chance = Math.min(1.0f, chance);
        float mp = GameTime.instance.getMultiplier();
        chance = (float)(1.0 - Math.pow(1.0f - chance, mp));
        chance *= 100.0f;
        if ((float)Rand.Next(10000) / 100.0f < chance) {
            success = true;
        }
        if ((GameClient.client || GameServer.server) && !NetworkZombieManager.canSpotted(this) && other != this.target) {
            return;
        }
        if (!success) {
            if (chance > 20.0f && otherPlayer != null && viewDist < 15.0f) {
                otherPlayer.couldBeSeenThisFrame = true;
            }
            boolean canAddXPForPerk = otherPlayer != null && !otherPlayer.isbCouldBeSeenThisFrame() && !otherPlayer.isbSeenThisFrame() && otherPlayer.isSneaking() && otherPlayer.isJustMoved();
            float randomMultiplier = 1100.0f;
            if (GameServer.debug) {
                randomMultiplier = 100.0f;
            }
            if (canAddXPForPerk && (float)Rand.Next((int)(randomMultiplier * GameTime.instance.getInvMultiplier())) == 0.0f) {
                if (GameServer.server) {
                    GameServer.addXp((IsoPlayer)other, PerkFactory.Perks.Sneak, 1.0f);
                } else if (!GameClient.client) {
                    otherPlayer.getXp().AddXP(PerkFactory.Perks.Sneak, 1.0f);
                }
            }
            if (canAddXPForPerk && (float)Rand.Next((int)(randomMultiplier * GameTime.instance.getInvMultiplier())) == 0.0f) {
                if (GameServer.server) {
                    GameServer.addXp((IsoPlayer)other, PerkFactory.Perks.Lightfoot, 1.0f);
                } else if (!GameClient.client) {
                    otherPlayer.getXp().AddXP(PerkFactory.Perks.Lightfoot, 1.0f);
                }
            }
            return;
        }
        if (otherPlayer != null) {
            otherPlayer.setbSeenThisFrame(true);
        }
        if (!bForced) {
            this.bonusSpotTime = 720.0f;
        }
        this.lastTargetSeenX = PZMath.fastfloor(other.getX());
        this.lastTargetSeenY = PZMath.fastfloor(other.getY());
        this.lastTargetSeenZ = PZMath.fastfloor(other.getZ());
        if (this.stateMachine.getCurrent() == StaggerBackState.instance()) {
            return;
        }
        if (this.target != other) {
            this.targetSeenTime = 0.0f;
            if (GameServer.server && !this.isRemoteZombie()) {
                this.addAggro(other, 1.0f);
            }
        }
        this.setTarget(other);
        this.vectorToTarget.x = other.getX();
        this.vectorToTarget.y = other.getY();
        this.vectorToTarget.x -= this.getX();
        this.vectorToTarget.y -= this.getY();
        float vecToTargetLength = this.vectorToTarget.getLength();
        if (!bForced) {
            this.timeSinceSeenFlesh = 0.0f;
            this.targetSeenTime += GameTime.getInstance().getRealworldSecondsSinceLastUpdate();
            this.canSeeTarget = this.isTargetVisible();
        }
        if (this.target == this.spottedLast && this.getCurrentState() == LungeState.instance() && this.lungeTimer > 0.0f) {
            return;
        }
        if (this.target == this.spottedLast && this.getCurrentState() == AttackVehicleState.instance()) {
            return;
        }
        if (PZMath.fastfloor(this.getZ()) == PZMath.fastfloor(this.target.getZ()) && (vecToTargetLength <= 3.5f || (isoMovingObject = this.target) instanceof IsoGameCharacter && (gameCharacter = (IsoGameCharacter)isoMovingObject).getVehicle() != null && vecToTargetLength <= 4.0f) && this.getStateEventDelayTimer() <= 0.0f && !PolygonalMap2.instance.lineClearCollide(this.getX(), this.getY(), other.getX(), other.getY(), PZMath.fastfloor(this.getZ()), other)) {
            this.setTarget(other);
            if (this.getCurrentState() == LungeState.instance()) {
                return;
            }
        }
        this.spottedLast = other;
        if (!this.ghost && !this.getCurrentSquare().getProperties().has(IsoFlagType.smoke)) {
            IsoGameCharacter isoGameCharacter;
            this.setTarget(other);
            if (this.allowRepathDelay > 0.0f) {
                return;
            }
            isoMovingObject = this.target;
            if (isoMovingObject instanceof IsoGameCharacter && (isoGameCharacter = (IsoGameCharacter)isoMovingObject).getVehicle() != null) {
                if ((this.getCurrentState() == PathFindState.instance() || this.getCurrentState() == WalkTowardState.instance()) && this.getPathFindBehavior2().getTargetChar() == this.target) {
                    return;
                }
                if (this.getCurrentState() == AttackVehicleState.instance()) {
                    return;
                }
                BaseVehicle vehicle = isoGameCharacter.getVehicle();
                if (Math.abs(vehicle.getCurrentSpeedKmHour()) > 0.8f && this.DistToSquared(vehicle) <= 16.0f) {
                    return;
                }
                this.pathToCharacter(isoGameCharacter);
                this.allowRepathDelay = 10.0f;
                return;
            }
            this.pathToCharacter(otherCharacter);
            if (Rand.Next(5) == 0) {
                this.spotSoundDelay = 200;
            }
            this.allowRepathDelay = 480.0f;
        }
    }

    public void spottedOld(IsoMovingObject other, boolean bForced) {
        IsoGameCharacter gameCharacter;
        IsoMovingObject sneakTileBonus2;
        float distCurrent;
        float distOther;
        IsoMovingObject isoMovingObject;
        if (GameClient.client && this.isRemoteZombie()) {
            if (this.getTarget() != null) {
                this.vectorToTarget.x = this.getTarget().getX();
                this.vectorToTarget.y = this.getTarget().getY();
                this.vectorToTarget.x -= this.getX();
                this.vectorToTarget.y -= this.getY();
                if (this.isCurrentState(LungeNetworkState.instance())) {
                    Vector2 temp = new Vector2();
                    temp.x = this.vectorToTarget.x;
                    temp.y = this.vectorToTarget.y;
                    temp.normalize();
                    this.DirectionFromVector(temp);
                    this.setForwardDirectionFromIsoDirection();
                    this.setForwardDirection(temp);
                }
            }
            return;
        }
        if (this.getCurrentSquare() == null) {
            return;
        }
        if (other.getCurrentSquare() == null) {
            return;
        }
        if (GameClient.client && !GameClient.connection.isReady()) {
            this.setTarget(null);
            this.spottedLast = null;
            return;
        }
        if (this.isReanimatedForGrappleOnly()) {
            this.setTarget(null);
            this.spottedLast = null;
            return;
        }
        if (this.isUseless()) {
            this.setTarget(null);
            this.spottedLast = null;
            return;
        }
        if (this.getCurrentSquare().getProperties().has(IsoFlagType.smoke)) {
            this.setTarget(null);
            this.spottedLast = null;
            return;
        }
        IsoGameCharacter otherCharacter = Type.tryCastTo(other, IsoGameCharacter.class);
        if (otherCharacter == null || otherCharacter.isDead()) {
            return;
        }
        IsoPlayer otherPlayer = Type.tryCastTo(other, IsoPlayer.class);
        if (otherPlayer != null && otherPlayer.isGhostMode()) {
            return;
        }
        if ((GameClient.client || GameServer.server) && otherPlayer != null && otherPlayer.networkAi.isDisconnected()) {
            return;
        }
        if (this.getCurrentSquare() == null) {
            this.ensureOnTile();
        }
        if (other.getCurrentSquare() == null) {
            other.ensureOnTile();
        }
        float chance = 200.0f;
        int playerIndex = otherPlayer != null && !GameServer.server ? otherPlayer.playerIndex : 0;
        float delta = (other.getCurrentSquare().lighting[playerIndex].lightInfo().r + other.getCurrentSquare().lighting[playerIndex].lightInfo().g + other.getCurrentSquare().lighting[playerIndex].lightInfo().b) / 3.0f;
        float delta2 = RenderSettings.getInstance().getAmbientForPlayer(playerIndex);
        float mdelta = (this.getCurrentSquare().lighting[playerIndex].lightInfo().r + this.getCurrentSquare().lighting[playerIndex].lightInfo().g + this.getCurrentSquare().lighting[playerIndex].lightInfo().b) / 3.0f;
        mdelta = mdelta * mdelta * mdelta;
        if (delta > 1.0f) {
            delta = 1.0f;
        }
        if (delta < 0.0f) {
            delta = 0.0f;
        }
        if (mdelta > 1.0f) {
            mdelta = 1.0f;
        }
        if (mdelta < 0.0f) {
            mdelta = 0.0f;
        }
        float difdelta = 1.0f - (delta - mdelta);
        if (delta < 0.2f) {
            delta = 0.2f;
        }
        if (delta2 < 0.2f) {
            delta2 = 0.2f;
        }
        if (other.getCurrentSquare().getRoom() != this.getCurrentSquare().getRoom()) {
            chance = 50.0f;
            if (other.getCurrentSquare().getRoom() != null && this.getCurrentSquare().getRoom() == null || other.getCurrentSquare().getRoom() == null && this.getCurrentSquare().getRoom() != null) {
                chance = 20.0f;
                if (otherCharacter.isAiming() || otherCharacter.isSneaking()) {
                    chance = delta < 0.4f ? 0.0f : 10.0f;
                } else if (other.getMovementLastFrame().getLength() <= 0.04f && delta < 0.4f) {
                    chance = 10.0f;
                }
            }
        }
        Vector2 thisToOther = IsoGameCharacter.tempo;
        thisToOther.x = other.getX();
        thisToOther.y = other.getY();
        thisToOther.x -= this.getX();
        thisToOther.y -= this.getY();
        if (other.getCurrentSquare().getZ() != this.current.getZ()) {
            int dif = Math.abs(other.getCurrentSquare().getZ() - this.current.getZ()) * 5;
            chance /= (float)(++dif);
        }
        float viewDist = GameTime.getInstance().getViewDist();
        if (thisToOther.getLength() > viewDist) {
            return;
        }
        if (GameServer.server) {
            this.indoorZombie = false;
        }
        if (thisToOther.getLength() < viewDist) {
            viewDist = thisToOther.getLength();
        }
        if ((viewDist *= 1.1f) > GameTime.getInstance().getViewDistMax()) {
            viewDist = GameTime.getInstance().getViewDistMax();
        }
        thisToOther.normalize();
        Vector2 thisForward = this.getLookVector(tempo2);
        float cosAngle = thisForward.dot(thisToOther);
        this.updateVisionRadius();
        if (this.DistTo(other) > this.visionRadiusResult) {
            chance -= 10000.0f;
        }
        if (viewDist > 0.5f) {
            chance = cosAngle < -0.4f ? 0.0f : (cosAngle < -0.2f ? (chance /= 8.0f) : (cosAngle < -0.0f ? (chance /= 4.0f) : (cosAngle < 0.2f ? (chance /= 2.0f) : (cosAngle <= 0.4f ? (chance *= 2.0f) : (cosAngle <= 0.6f ? (chance *= 8.0f) : (cosAngle <= 0.8f ? (chance *= 16.0f) : (chance *= 32.0f)))))));
        }
        if (chance > 0.0f && (isoMovingObject = this.target) instanceof IsoPlayer) {
            IsoPlayer player = (IsoPlayer)isoMovingObject;
            if (!GameServer.server && player.remoteId == -1 && this.current.isCanSee(player.playerIndex)) {
                player.targetedByZombie = true;
                player.lastTargeted = 0.0f;
            }
        }
        chance *= difdelta;
        int dif = PZMath.fastfloor(other.getZ()) - PZMath.fastfloor(this.getZ());
        if (dif >= 1) {
            chance /= (float)dif * 3.0f;
        }
        float distAlpha = PZMath.clamp(viewDist / GameTime.getInstance().getViewDist(), 0.0f, 1.0f);
        chance *= 1.0f - distAlpha;
        chance *= 1.0f - distAlpha;
        chance *= 1.0f - distAlpha;
        float closeDelta = PZMath.clamp(viewDist / 10.0f, 0.0f, 1.0f);
        chance *= 1.0f + (1.0f - closeDelta) * 10.0f;
        float movement = other.getMovementLastFrame().getLength();
        if (otherCharacter == null) {
            return;
        }
        if (otherCharacter.getTorchStrength() > 0.0f) {
            chance *= 3.0f;
        }
        if (movement < 0.01f) {
            chance *= 0.5f;
        } else if (otherCharacter.isSneaking()) {
            chance *= 0.4f;
        } else if (otherCharacter.isAiming()) {
            chance *= 0.75f;
        } else if (movement < 0.06f) {
            chance *= 0.8f;
        } else if (movement >= 0.06f) {
            chance *= 2.4f;
        }
        if (this.eatBodyTarget != null) {
            chance *= 0.6f;
        }
        if (viewDist < 5.0f && (!otherCharacter.isRunning() && !otherCharacter.isSneaking() && !otherCharacter.isAiming() || otherCharacter.isRunning())) {
            chance *= 3.0f;
        }
        if (this.spottedLast == other && this.timeSinceSeenFlesh < 120.0f) {
            chance = 1000.0f;
        }
        chance *= otherCharacter.getSneakSpotMod();
        chance *= delta2;
        if (this.target != other && this.target != null && (distOther = IsoUtils.DistanceManhatten(this.getX(), this.getY(), other.getX(), other.getY())) > (distCurrent = IsoUtils.DistanceManhatten(this.getX(), this.getY(), this.target.getX(), this.target.getY()))) {
            return;
        }
        chance *= 0.3f;
        if (bForced) {
            chance = 1000000.0f;
        }
        if (this.bonusSpotTime > 0.0f) {
            chance = 1000000.0f;
        }
        chance *= 1.2f;
        if (this.sight == 1) {
            chance *= 2.5f;
        }
        if (this.sight == 3) {
            chance *= 0.45f;
        }
        if (this.inactive) {
            chance *= 0.25f;
        }
        chance *= 0.25f;
        if (otherPlayer != null && otherPlayer.hasTrait(CharacterTrait.INCONSPICUOUS)) {
            chance *= 0.5f;
        }
        if (otherPlayer != null && otherPlayer.hasTrait(CharacterTrait.CONSPICUOUS)) {
            chance *= 2.0f;
        }
        chance *= 1.6f;
        if (this.getCurrentSquare() != other.getCurrentSquare() && otherPlayer != null && otherPlayer.isSneaking()) {
            IsoGridSquare squareTest;
            int yDiff;
            IsoGridSquare solidSquareTest = null;
            int xDiff = Math.abs(this.getCurrentSquare().getX() - other.getCurrentSquare().getX());
            if (xDiff > (yDiff = Math.abs(this.getCurrentSquare().getY() - other.getCurrentSquare().getY()))) {
                if (this.getCurrentSquare().getX() - other.getCurrentSquare().getX() > 0) {
                    squareTest = other.getCurrentSquare().getAdjacentSquare(IsoDirections.E);
                } else {
                    squareTest = other.getCurrentSquare();
                    solidSquareTest = other.getCurrentSquare().getAdjacentSquare(IsoDirections.W);
                }
            } else if (this.getCurrentSquare().getY() - other.getCurrentSquare().getY() > 0) {
                squareTest = other.getCurrentSquare().getAdjacentSquare(IsoDirections.S);
            } else {
                squareTest = other.getCurrentSquare();
                solidSquareTest = other.getCurrentSquare().getAdjacentSquare(IsoDirections.N);
            }
            if (squareTest != null && otherCharacter != null) {
                float sneakTileBonus2 = otherCharacter.checkIsNearWall();
                if (sneakTileBonus2 == 1.0f && solidSquareTest != null) {
                    sneakTileBonus2 = solidSquareTest.getGridSneakModifier(true);
                }
                if (sneakTileBonus2 > 1.0f) {
                    float distToTile = other.DistTo(squareTest.x, squareTest.y);
                    if (distToTile > 1.0f) {
                        sneakTileBonus2 /= distToTile;
                    }
                    chance /= sneakTileBonus2;
                }
            }
        }
        chance /= this.getWornItemsVisionModifier();
        if (this.getEatBodyTarget() != null && chance > 0.0f) {
            chance *= 0.5f;
        }
        chance = PZMath.fastfloor(chance);
        boolean success = false;
        chance = Math.min(chance, 400.0f);
        chance /= 400.0f;
        chance = Math.max(0.0f, chance);
        chance = Math.min(1.0f, chance);
        float mp = GameTime.instance.getMultiplier();
        chance = (float)(1.0 - Math.pow(1.0f - chance, mp));
        chance *= 100.0f;
        if ((float)Rand.Next(10000) / 100.0f < chance) {
            success = true;
        }
        if ((GameClient.client || GameServer.server) && !NetworkZombieManager.canSpotted(this) && other != this.target) {
            return;
        }
        if (!success) {
            if (chance > 20.0f && otherPlayer != null && viewDist < 15.0f) {
                otherPlayer.couldBeSeenThisFrame = true;
            }
            boolean canAddXPForPerk = otherPlayer != null && !otherPlayer.isbCouldBeSeenThisFrame() && !otherPlayer.isbSeenThisFrame() && otherPlayer.isSneaking() && otherPlayer.isJustMoved();
            float randomMultiplier = 1100.0f;
            if (GameServer.debug) {
                randomMultiplier = 100.0f;
            }
            if (canAddXPForPerk && (float)Rand.Next((int)(randomMultiplier * GameTime.instance.getInvMultiplier())) == 0.0f) {
                if (GameServer.server) {
                    GameServer.addXp((IsoPlayer)other, PerkFactory.Perks.Sneak, 1.0f);
                } else if (!GameClient.client) {
                    otherPlayer.getXp().AddXP(PerkFactory.Perks.Sneak, 1.0f);
                }
            }
            if (canAddXPForPerk && (float)Rand.Next((int)(randomMultiplier * GameTime.instance.getInvMultiplier())) == 0.0f) {
                if (GameServer.server) {
                    GameServer.addXp((IsoPlayer)other, PerkFactory.Perks.Lightfoot, 1.0f);
                } else if (!GameClient.client) {
                    otherPlayer.getXp().AddXP(PerkFactory.Perks.Lightfoot, 1.0f);
                }
            }
            return;
        }
        if (otherPlayer != null) {
            otherPlayer.setbSeenThisFrame(true);
        }
        if (!bForced) {
            this.bonusSpotTime = 120.0f;
        }
        this.lastTargetSeenX = PZMath.fastfloor(other.getX());
        this.lastTargetSeenY = PZMath.fastfloor(other.getY());
        this.lastTargetSeenZ = PZMath.fastfloor(other.getZ());
        if (this.stateMachine.getCurrent() == StaggerBackState.instance()) {
            return;
        }
        if (this.target != other) {
            this.targetSeenTime = 0.0f;
            if (GameServer.server && !this.isRemoteZombie()) {
                this.addAggro(other, 1.0f);
            }
        }
        this.setTarget(other);
        this.vectorToTarget.x = other.getX();
        this.vectorToTarget.y = other.getY();
        this.vectorToTarget.x -= this.getX();
        this.vectorToTarget.y -= this.getY();
        float vecToTargetLength = this.vectorToTarget.getLength();
        if (!bForced) {
            this.timeSinceSeenFlesh = 0.0f;
            this.targetSeenTime += GameTime.getInstance().getRealworldSecondsSinceLastUpdate();
        }
        if (this.target == this.spottedLast && this.getCurrentState() == LungeState.instance() && this.lungeTimer > 0.0f) {
            return;
        }
        if (this.target == this.spottedLast && this.getCurrentState() == AttackVehicleState.instance()) {
            return;
        }
        if (PZMath.fastfloor(this.getZ()) == PZMath.fastfloor(this.target.getZ()) && (vecToTargetLength <= 3.5f || (sneakTileBonus2 = this.target) instanceof IsoGameCharacter && (gameCharacter = (IsoGameCharacter)sneakTileBonus2).getVehicle() != null && vecToTargetLength <= 4.0f) && this.getStateEventDelayTimer() <= 0.0f && !PolygonalMap2.instance.lineClearCollide(this.getX(), this.getY(), other.getX(), other.getY(), PZMath.fastfloor(this.getZ()), other)) {
            this.setTarget(other);
            if (this.getCurrentState() == LungeState.instance()) {
                return;
            }
        }
        this.spottedLast = other;
        if (!this.ghost && !this.getCurrentSquare().getProperties().has(IsoFlagType.smoke)) {
            IsoGameCharacter isoGameCharacter;
            this.setTarget(other);
            if (this.allowRepathDelay > 0.0f) {
                return;
            }
            IsoMovingObject sneakTileBonus2 = this.target;
            if (sneakTileBonus2 instanceof IsoGameCharacter && (isoGameCharacter = (IsoGameCharacter)sneakTileBonus2).getVehicle() != null) {
                if ((this.getCurrentState() == PathFindState.instance() || this.getCurrentState() == WalkTowardState.instance()) && this.getPathFindBehavior2().getTargetChar() == this.target) {
                    return;
                }
                if (this.getCurrentState() == AttackVehicleState.instance()) {
                    return;
                }
                BaseVehicle vehicle = isoGameCharacter.getVehicle();
                if (Math.abs(vehicle.getCurrentSpeedKmHour()) > 0.8f && this.DistToSquared(vehicle) <= 16.0f) {
                    return;
                }
                this.pathToCharacter(isoGameCharacter);
                this.allowRepathDelay = 10.0f;
                return;
            }
            this.pathToCharacter(otherCharacter);
            if (Rand.Next(5) == 0) {
                this.spotSoundDelay = 200;
            }
            this.allowRepathDelay = 480.0f;
        }
    }

    @Override
    public void spotted(IsoMovingObject other, boolean bForced) {
        if (SandboxOptions.instance.lore.spottedLogic.getValue()) {
            this.spottedNew(other, bForced);
        } else {
            this.spottedOld(other, bForced);
        }
    }

    @Override
    public void Move(Vector2 dir) {
        if (GameClient.client && this.authOwner == null) {
            return;
        }
        this.setNextX(this.getNextX() + dir.x * GameTime.instance.getMultiplier());
        this.setNextY(this.getNextY() + dir.y * GameTime.instance.getMultiplier());
        this.movex = dir.x;
        this.movey = dir.y;
    }

    @Override
    public void MoveUnmodded(Vector2 dir) {
        if (this.speedType == 1 && (this.isCurrentState(LungeState.instance()) || this.isCurrentState(LungeNetworkState.instance()) || this.isCurrentState(AttackState.instance()) || this.isCurrentState(AttackNetworkState.instance()) || this.isCurrentState(StaggerBackState.instance()) || this.isCurrentState(ZombieHitReactionState.instance())) && this.target instanceof IsoGameCharacter) {
            float dx = this.target.getNextX() - this.getX();
            float dy = this.target.getNextY() - this.getY();
            float dist = (float)Math.sqrt(dx * dx + dy * dy);
            dist -= this.getWidth() + this.target.getWidth() - 0.1f;
            dist = Math.max(0.0f, dist);
            if (dir.getLength() > dist) {
                dir.setLength(dist);
            }
        }
        if (GameClient.client && this.isRemoteZombie()) {
            float dist = IsoUtils.DistanceTo(this.realx, this.realy, this.networkAi.targetX, this.networkAi.targetY);
            float movingScale = 0.5f + IsoUtils.smoothstep(0.5f, 1.5f, IsoUtils.DistanceTo(this.getX(), this.getY(), this.networkAi.targetX, this.networkAi.targetY) / dist);
            dir.setLength(dir.getLength() * movingScale);
            if (this.networkAi.wasSeparated) {
                Vector2 diff = new Vector2(this.getNextX() - this.realx, this.getNextY() - this.realy);
                diff.setLength(diff.getLength() / 16.0f);
                this.setNextX(this.getNextX() - diff.x);
                this.setNextY(this.getNextY() - diff.y);
            }
        }
        super.MoveUnmodded(dir);
    }

    public boolean canBeDeletedUnnoticed(float minDistance) {
        if (!GameClient.client) {
            return false;
        }
        float nearestDistance = Float.POSITIVE_INFINITY;
        ArrayList<IsoPlayer> players = GameClient.instance.getPlayers();
        for (int i = 0; i < players.size(); ++i) {
            float cone;
            IsoPlayer player = players.get(i);
            float dotZombieToPlayer = player.getDotWithForwardDirection(this.getX(), this.getY());
            if (dotZombieToPlayer > -(cone = LightingJNI.calculateVisionCone(player) + 0.2f)) {
                return false;
            }
            float distance = IsoUtils.DistanceToSquared(this.getX(), this.getY(), player.getX(), player.getY());
            if (!(distance < nearestDistance)) continue;
            nearestDistance = distance;
        }
        return nearestDistance > minDistance * minDistance;
    }

    @Override
    public void DoFootstepSound(String type) {
        ParameterCharacterMovementSpeed.MovementType movementType = ParameterCharacterMovementSpeed.MovementType.Walk;
        float speed = 0.5f;
        switch (type) {
            case "sneak_walk": {
                speed = 0.25f;
                movementType = ParameterCharacterMovementSpeed.MovementType.SneakWalk;
                break;
            }
            case "sneak_run": {
                speed = 0.25f;
                movementType = ParameterCharacterMovementSpeed.MovementType.SneakRun;
                break;
            }
            case "strafe": {
                speed = 0.5f;
                movementType = ParameterCharacterMovementSpeed.MovementType.Strafe;
                break;
            }
            case "walk": {
                speed = 0.5f;
                movementType = ParameterCharacterMovementSpeed.MovementType.Walk;
                break;
            }
            case "run": {
                speed = 0.75f;
                movementType = ParameterCharacterMovementSpeed.MovementType.Run;
                break;
            }
            case "sprint": {
                speed = 1.0f;
                movementType = ParameterCharacterMovementSpeed.MovementType.Sprint;
            }
        }
        this.addFootstepParametersIfNeeded();
        this.parameterCharacterMovementSpeed.setMovementType(movementType);
        this.DoFootstepSound(speed);
    }

    public void addFootstepParametersIfNeeded() {
        if (!GameServer.server && !this.getFMODParameters().parameterList.contains(this.parameterCharacterMovementSpeed)) {
            this.getFMODParameters().add(this.parameterCharacterMovementSpeed);
            this.getFMODParameters().add(this.parameterFootstepMaterial);
            this.getFMODParameters().add(this.parameterFootstepMaterial2);
            this.getFMODParameters().add(this.parameterShoeType);
        }
    }

    @Override
    public void DoFootstepSound(float volume) {
        if (GameServer.server) {
            return;
        }
        if (volume <= 0.0f) {
            return;
        }
        if (this.getCurrentSquare() == null) {
            return;
        }
        if (GameClient.client && this.authOwner == null) {
            if (this.def != null && this.sprite != null && this.sprite.currentAnim != null && (this.sprite.currentAnim.name.contains("Run") || this.sprite.currentAnim.name.contains("Walk"))) {
                boolean bFootDown;
                int frame = (int)this.def.frame;
                if (frame >= 0 && frame < 5) {
                    bFootDown = this.stepFrameLast < 0 || this.stepFrameLast > 5;
                } else {
                    boolean bl = bFootDown = this.stepFrameLast < 5;
                }
                if (bFootDown) {
                    for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                        IsoPlayer player = IsoPlayer.players[i];
                        if (player == null || !(player.DistToSquared(this) < 225.0f)) continue;
                        ZombieFootstepManager.instance.addCharacter(this);
                        break;
                    }
                }
                this.stepFrameLast = frame;
            } else {
                this.stepFrameLast = -1;
            }
            return;
        }
        boolean listener = SoundManager.instance.isListenerInRange(this.getX(), this.getY(), 15.0f);
        if (listener) {
            this.footstepVolume = volume;
            ZombieFootstepManager.instance.addCharacter(this);
        }
    }

    @Override
    public void preupdate() {
        if (GameServer.server && this.thumpSent) {
            this.thumpFlag = 0;
            this.thumpSent = false;
        }
        this.followCount = 0;
        if (GameClient.client) {
            HitReactionNetworkAI hitReaction;
            if (this.networkAi.isVehicleHitTimeout()) {
                this.networkAi.hitByVehicle();
            }
            if (this.networkAi.isDeadBodyTimeout()) {
                this.networkAi.becomeCorpse();
            }
            if (!this.isLocal()) {
                this.networkAi.preupdate();
            } else if (this.isKnockedDown() && !this.isOnFloor() && (hitReaction = this.getHitReactionNetworkAI()).isSetup() && !hitReaction.isStarted()) {
                hitReaction.start();
            }
        }
        super.preupdate();
    }

    @Override
    public void postupdate() {
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = s_performance.postUpdate.profile();){
            this.postUpdateInternal();
        }
    }

    private void postUpdateInternal() {
        IsoPlayer player;
        IsoMovingObject isoMovingObject = this.target;
        if (isoMovingObject instanceof IsoPlayer) {
            IsoPlayer isoPlayer = (IsoPlayer)isoMovingObject;
            ++isoPlayer.getStats().numChasingZombies;
        }
        super.postupdate();
        if (this.unbalancedLevel > 0.0f) {
            float rateOfRegainBalance = 0.5f;
            this.unbalancedLevel = PZMath.clamp(PZMath.lerp(this.unbalancedLevel, 0.0f, 0.5f * GameTime.getInstance().getMultiplier(), LerpType.EaseOutQuad), 0.0f, 1.0f);
        }
        if (this.isReanimatedForGrappleOnly() && this.isBeingGrappled() && this.getGrappledBy().isMoving() && this.bloodSplatAmount > 0) {
            int corpseBloodSplatterAmount = 100;
            float invBleedChance = 2.0f * GameTime.instance.getInvMultiplier();
            if ((float)Rand.Next((int)invBleedChance) < invBleedChance * 0.3f) {
                DebugLog.Zombie.trace("Dragged corpse. Bleeding on the floor 1.");
                this.splatBloodFloor();
                this.bloodSplatAmount -= 2;
            }
            if (Rand.Next((int)invBleedChance) == 0) {
                DebugLog.Zombie.trace("Dragged corpse. Bleeding on the floor 2.");
                this.splatBloodFloor();
                this.bloodSplatAmount -= 2;
            }
            this.getModData().rawset("_bloodSplatAmount", (Object)BoxedStaticValues.toDouble(this.bloodSplatAmount));
        }
        if (!(this.current != null || GameClient.client && this.authOwner == null)) {
            this.removeFromWorld();
            this.removeFromSquare();
        }
        if (!GameServer.server && (player = this.getReanimatedPlayer()) != null) {
            player.setX(this.getX());
            player.setY(this.getY());
            player.setZ(this.getZ());
            player.setDir(this.getDir());
            player.setForwardDirection(this.getForwardDirection());
            AnimationPlayer animPlr1 = this.getAnimationPlayer();
            AnimationPlayer animPlr2 = player.getAnimationPlayer();
            if (animPlr1 != null && animPlr1.isReady() && animPlr2 != null && animPlr2.isReady()) {
                animPlr2.setTargetAngle(animPlr1.getAngle());
                animPlr2.setAngleToTarget();
            }
            player.setCurrentSquareFromPosition();
            player.updateLightInfo();
            if (player.soundListener != null) {
                player.soundListener.setPos(player.getX(), player.getY(), player.getZ());
                player.soundListener.tick();
            }
            IsoPlayer oldinst = IsoPlayer.getInstance();
            IsoPlayer.setInstance(player);
            player.updateLOS();
            IsoPlayer.setInstance(oldinst);
            if (GameClient.client && this.authOwner == null && this.networkUpdate.Check()) {
                GameClient.instance.sendPlayer(player);
            }
            player.dirtyRecalcGridStackTime = 2.0f;
        }
        this.canSeeTarget = this.isTargetVisible();
        if (this.targetSeenTime > 0.0f && !this.canSeeTarget) {
            this.targetSeenTime = 0.0f;
        }
    }

    @Override
    protected void handleLandingImpact(FallDamage fallDamage) {
        boolean isMoreThanLightFall = fallDamage.isMoreThanLightFall();
        boolean isDamagingFall = fallDamage.isDamagingFall();
        boolean isMoreThanHardFall = fallDamage.isMoreThanHardFall();
        if (!isDamagingFall || this.isClimbing()) {
            return;
        }
        this.hitDir.y = 0.0f;
        this.hitDir.x = 0.0f;
        this.playHurtSound();
        float damageAlpha = fallDamage.getImpactIsoSpeed() / FallingConstants.zombieLethalFallThreshold;
        float damageAdjust = damageAlpha * Rand.Next(-0.25f, 0.25f);
        float impactDamage = PZMath.lerpFunc_EaseOutQuad(1.05f * damageAlpha);
        float damage = (impactDamage + damageAdjust) * (float)SandboxOptions.instance.lore.zombiesFallDamage.getValue();
        CombatManager.getInstance().applyDamage(this, damage);
        this.setAttackedBy(null);
        this.helmetFall(isMoreThanLightFall);
        if (isMoreThanLightFall) {
            if (isMoreThanHardFall && Rand.Next(0.0f, 1.0f) < damage) {
                if (this.isAlive()) {
                    this.setBecomeCrawler(true);
                    this.setCanWalk(false);
                    this.setCrawlerType(1);
                }
                this.setFallOnFront(true);
                this.playSound("FirstAidFracture");
                this.splatBloodFloorBig();
            }
            if (this.isAlive() && !this.isBecomeCrawler() && Rand.Next(0.0f, 1.0f) > 0.5f) {
                this.setFakeDead(true);
                this.setFallOnFront(true);
            }
            this.splatBloodFloorBig();
            this.playSound("LandHeavy");
        } else {
            this.playSound("LandLight");
        }
        DebugType.General.debugln("Damage incurred by fall: %f. isAlive: %s", Float.valueOf(damage), !this.isDead());
    }

    @Override
    public boolean isSolidForSeparate() {
        if (this.getCurrentState() == FakeDeadZombieState.instance() || this.getCurrentState() == ZombieFallDownState.instance() || this.getCurrentState() == ZombieOnGroundState.instance() || this.getCurrentState() == ZombieGetUpState.instance() || this.getCurrentState() == ZombieHitReactionState.instance() && this.speedType != 1) {
            return false;
        }
        if (this.isSitAgainstWall()) {
            return false;
        }
        if (this.isRagdoll()) {
            return false;
        }
        return super.isSolidForSeparate();
    }

    @Override
    public boolean isPushableForSeparate() {
        if (this.getCurrentState() == ThumpState.instance() || this.getCurrentState() == AttackState.instance() || this.getCurrentState() == AttackVehicleState.instance() || this.getCurrentState() == ZombieEatBodyState.instance() || this.getCurrentState() == ZombieFaceTargetState.instance()) {
            return false;
        }
        if (this.isSitAgainstWall()) {
            return false;
        }
        return super.isPushableForSeparate();
    }

    @Override
    public boolean isPushedByForSeparate(IsoMovingObject other) {
        IsoZombie isoZombie;
        if (other instanceof IsoZombie && (isoZombie = (IsoZombie)other).getCurrentState() == ZombieHitReactionState.instance() && !isoZombie.collideWhileHit) {
            return false;
        }
        if (this.getCurrentState() == ZombieHitReactionState.instance() && !this.collideWhileHit) {
            return false;
        }
        if (GameClient.client && other instanceof IsoZombie && !NetworkZombieSimulator.getInstance().isZombieSimulated(this.getOnlineID())) {
            return false;
        }
        return super.isPushedByForSeparate(other);
    }

    @Override
    public void update() {
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = s_performance.update.profile();){
            this.updateInternal();
        }
    }

    private void updateActiveState() {
        this.makeInactive(GameTime.getInstance().isZombieInactivityPhase());
    }

    private void updateInternal() {
        IsoMovingObject isoMovingObject;
        BaseVehicle nearVehicle;
        if (Core.debug && this.getOutfitName() != null && this.getOutfitName().contains("Debug")) {
            String outfitName = this.getOutfitName();
            if (outfitName.contains("Immortal")) {
                this.setImmortalTutorialZombie(true);
            }
            if (outfitName.contains("Useless")) {
                this.setUseless(true);
            }
        }
        this.updateActiveState();
        if (GameClient.client && !this.isRemoteZombie()) {
            ZombieCountOptimiser.incrementZombie(this);
            GameStatistic.getInstance().zombiesUpdated.increase();
        } else if (GameServer.server) {
            GameStatistic.getInstance().zombiesUpdated.increase();
        }
        if (this.crawling) {
            if (this.getActionContext().getGroup() != ActionGroup.getActionGroup("zombie-crawler")) {
                this.advancedAnimator.OnAnimDataChanged(false);
                this.initializeStates();
                this.getActionContext().setGroup(ActionGroup.getActionGroup("zombie-crawler"));
            }
        } else if (this.getActionContext().getGroup() != ActionGroup.getActionGroup("zombie")) {
            this.advancedAnimator.OnAnimDataChanged(false);
            this.initializeStates();
            this.getActionContext().setGroup(ActionGroup.getActionGroup("zombie"));
        }
        if (this.getThumpTimer() > 0) {
            --this.thumpTimer;
        }
        if ((nearVehicle = this.getNearVehicle()) != null) {
            VehiclePart part;
            if (this.target == null && nearVehicle.isSirening() && (part = nearVehicle.getUseablePart(this, false)) != null && part.getSquare().DistTo(this) < 0.7f) {
                this.setThumpTarget(nearVehicle);
            }
            if (nearVehicle.isAlarmed() && !nearVehicle.isPreviouslyEntered() && Rand.Next(10000) < 1) {
                nearVehicle.triggerAlarm();
            }
        }
        this.doDeferredMovement();
        this.updateEmitter();
        if (this.spotSoundDelay > 0) {
            --this.spotSoundDelay;
        }
        if (GameClient.client && this.authOwner == null) {
            if (this.lastRemoteUpdate > 800 && this.legsSprite.hasAnimation() && (this.legsSprite.currentAnim.name.equals("ZombieDeath") || this.legsSprite.currentAnim.name.equals("ZombieStaggerBack") || this.legsSprite.currentAnim.name.equals("ZombieGetUp"))) {
                DebugLog.log(DebugType.Zombie, "removing stale zombie 800 id=" + this.onlineId);
                VirtualZombieManager.instance.removeZombieFromWorld(this);
                return;
            }
            if (GameClient.fastForward) {
                VirtualZombieManager.instance.removeZombieFromWorld(this);
                return;
            }
        }
        if (GameClient.client && this.authOwner == null && this.lastRemoteUpdate < 2000 && this.lastRemoteUpdate + 1000 / PerformanceSettings.getLockFPS() > 2000) {
            DebugLog.log(DebugType.Zombie, "lastRemoteUpdate 2000+ id=" + this.onlineId);
        }
        this.lastRemoteUpdate = (short)(this.lastRemoteUpdate + (short)(1000 / PerformanceSettings.getLockFPS()));
        if (GameClient.client && this.authOwner == null && (!this.remote || this.lastRemoteUpdate > 5000)) {
            DebugLog.log(DebugType.Zombie, "removing stale zombie 5000 id=" + this.onlineId);
            DebugLog.log("Zombie: removing stale zombie 5000 id=" + this.onlineId);
            VirtualZombieManager.instance.removeZombieFromWorld(this);
            return;
        }
        this.sprite = this.legsSprite;
        if (this.sprite == null) {
            return;
        }
        this.updateCharacterTextureAnimTime();
        if (GameServer.server && this.indoorZombie) {
            super.update();
            return;
        }
        this.bonusSpotTime = PZMath.clamp(this.bonusSpotTime - GameTime.instance.getMultiplier(), 0.0f, Float.MAX_VALUE);
        this.timeSinceSeenFlesh = PZMath.clamp(this.timeSinceSeenFlesh + GameTime.instance.getMultiplier(), 0.0f, Float.MAX_VALUE);
        this.checkZombieEntersPlayerBuilding();
        this.updateMovementStatistics();
        this.setCollidable(true);
        LuaEventManager.triggerEvent("OnZombieUpdate", this);
        if (Core.lastStand && this.getStateMachine().getCurrent() != ThumpState.instance() && this.getStateMachine().getCurrent() != AttackState.instance() && this.timeSinceSeenFlesh > 120.0f && Rand.Next(36000) == 0) {
            IsoPlayer lowest = null;
            float lowestDst = 1000000.0f;
            for (int n = 0; n < IsoPlayer.numPlayers; ++n) {
                if (IsoPlayer.players[n] == null || !(IsoPlayer.players[n].DistTo(this) < lowestDst) || IsoPlayer.players[n].isDead()) continue;
                lowestDst = IsoPlayer.players[n].DistTo(this);
                lowest = IsoPlayer.players[n];
            }
            if (lowest != null) {
                this.allowRepathDelay = -1.0f;
                this.pathToCharacter(lowest);
            }
            return;
        }
        if (this.isRagdoll()) {
            RagdollController ragdollController = this.getRagdollController();
            ragdollController.vehicleCollision(this, this.vehicle4testCollision);
        }
        if (!this.isLocal()) {
            this.setVehicleCollision(this.vehicle4testCollision != null && this.vehicle4testCollision.updateNetworkHitByVehicle(this));
        } else {
            this.setVehicleCollision(this.vehicle4testCollision != null && this.testCollideWithVehicles(this.vehicle4testCollision, null));
        }
        this.vehicle4testCollision = null;
        if (this.bonusSpotTime > 0.0f && this.spottedLast != null && !((IsoGameCharacter)this.spottedLast).isDead()) {
            this.spotted(this.spottedLast, true);
        }
        if (GameServer.server && this.getStateMachine().getCurrent() == BurntToDeath.instance()) {
            DebugLog.log(DebugType.Zombie, "Zombie is burning " + this.onlineId);
        }
        super.update();
        if (VirtualZombieManager.instance.isReused(this)) {
            DebugLog.log(DebugType.Zombie, "Zombie added to ReusableZombies after super.update - RETURNING " + String.valueOf(this));
            return;
        }
        if (this.getStateMachine().getCurrent() == ClimbThroughWindowState.instance() || this.getStateMachine().getCurrent() == ClimbOverFenceState.instance() || this.getStateMachine().getCurrent() == CrawlingZombieTurnState.instance()) {
            return;
        }
        this.ensureOnTile();
        State cur = this.stateMachine.getCurrent();
        if (cur == StaggerBackState.instance() || cur == BurntToDeath.instance() || cur == FakeDeadZombieState.instance() || cur == ZombieFallDownState.instance() || cur == ZombieOnGroundState.instance() || cur == ZombieHitReactionState.instance() || cur == ZombieGetUpState.instance()) {
            return;
        }
        if (GameServer.server && this.onlineId == -1) {
            this.onlineId = ServerMap.instance.getUniqueZombieId();
        } else if (cur == PathFindState.instance() && this.finder.progress == AStarPathFinder.PathFindProgress.notyetfound) {
            if (this.crawling) {
                this.PlayAnim("ZombieCrawl");
                this.def.animFrameIncrease = 0.0f;
            } else {
                this.PlayAnim("ZombieIdle");
                this.def.animFrameIncrease = 0.08f + (float)Rand.Next(1000) / 8000.0f;
                this.def.animFrameIncrease *= 0.5f;
            }
        } else if (cur != AttackState.instance() && cur != AttackVehicleState.instance() && (this.getNextX() != this.getX() || this.getNextY() != this.getY())) {
            if (this.walkVariantUse == null || cur != LungeState.instance() && cur != LungeNetworkState.instance()) {
                this.walkVariantUse = this.walkVariant;
            }
            if (this.crawling) {
                this.walkVariantUse = "ZombieCrawl";
            }
            if (cur != ZombieIdleState.instance() && cur != StaggerBackState.instance() && cur != ThumpState.instance() && cur != FakeDeadZombieState.instance()) {
                if (this.running) {
                    this.PlayAnim("Run");
                    this.def.setFrameSpeedPerFrame(0.33f);
                } else {
                    this.PlayAnim(this.walkVariantUse);
                    this.def.setFrameSpeedPerFrame(0.26f);
                    this.def.animFrameIncrease *= this.speedMod;
                }
                this.setShootable(true);
            }
        }
        this.shootable = true;
        this.solid = true;
        this.tryThump(null);
        this.damageSheetRope();
        this.allowRepathDelay = PZMath.clamp(this.allowRepathDelay - GameTime.instance.getMultiplier(), 0.0f, Float.MAX_VALUE);
        if (this.timeSinceSeenFlesh > (float)this.memory && this.target != null) {
            this.setTarget(null);
        }
        if ((isoMovingObject = this.target) instanceof IsoGameCharacter) {
            IsoGameCharacter isoGameCharacter = (IsoGameCharacter)isoMovingObject;
            if (isoGameCharacter.reanimatedCorpse != null) {
                this.setTarget(null);
            }
        }
        if (this.target != null) {
            this.vectorToTarget.x = this.target.getX();
            this.vectorToTarget.y = this.target.getY();
            this.vectorToTarget.x -= this.getX();
            this.vectorToTarget.y -= this.getY();
            this.updateZombieTripping();
        }
        if (this.getSquare() != null && this.getSquare().hasFarmingPlant()) {
            int chance = (int)(100.0f / GameTime.getInstance().getTrueMultiplier());
            if (Rand.NextBool(chance = Math.max(1, chance))) {
                this.getSquare().destroyFarmingPlant();
            }
        }
        if (this.getSquare() != null && this.getSquare().hasLitCampfire() && this.isMoving()) {
            int chance = (int)(10000.0f / GameTime.getInstance().getTrueMultiplier());
            if (Rand.NextBool(chance = Math.max(1, chance))) {
                this.getSquare().putOutCampfire();
            }
        }
        if (this.getCurrentState() != PathFindState.instance() && this.getCurrentState() != WalkTowardState.instance() && this.getCurrentState() != ClimbThroughWindowState.instance()) {
            this.setLastHeardSound(-1, -1, -1);
        }
        if (this.timeSinceSeenFlesh > 240.0f && this.timeSinceRespondToSound > 5.0f) {
            this.RespondToSound();
        }
        this.timeSinceRespondToSound += GameTime.getInstance().getThirtyFPSMultiplier();
        this.networkAi.wasSeparated = false;
        this.separate();
        this.updateSearchForCorpse();
        if (this.timeSinceSeenFlesh > 2000.0f && this.timeSinceRespondToSound > 2000.0f) {
            ZombieGroupManager.instance.update(this);
        }
    }

    @Override
    protected void calculateStats() {
    }

    private void updateZombieTripping() {
        if (this.speedType == 1 && StringUtils.isNullOrEmpty(this.getBumpType()) && this.target != null && Rand.NextBool(Rand.AdjustForFramerate(750)) && !this.isRemoteZombie()) {
            this.setBumpType("trippingFromSprint");
        }
    }

    public int getVoiceChoice() {
        if (this.voiceChoice == -1) {
            this.voiceChoice = Rand.Next(this.speedType == 1 ? 2 : 3) + 1;
        }
        return this.voiceChoice;
    }

    public String getVoiceSoundName() {
        String part1 = this.getDescriptor().getVoicePrefix();
        String part2 = this.speedType == 1 ? "Sprinter" : "";
        String part3 = "Voice";
        String part4 = switch (this.getVoiceChoice()) {
            case 2 -> "B";
            case 3 -> "C";
            default -> "A";
        };
        return part1 + part2 + "Voice" + part4;
    }

    public String getBiteSoundName() {
        String part1 = this.getDescriptor().getVoicePrefix();
        String part2 = this.speedType == 1 ? "Sprinter" : "";
        String part3 = "Bite";
        String part4 = switch (this.getVoiceChoice()) {
            case 2 -> "B";
            case 3 -> "C";
            default -> "A";
        };
        return part1 + part2 + "Bite" + part4;
    }

    public void updateVocalProperties() {
        boolean bHasFunctioningVoice;
        if (GameServer.server) {
            return;
        }
        boolean bListenerInRange = SoundManager.instance.isListenerInRange(this.getX(), this.getY(), 20.0f);
        if (this.vocalEvent != 0L && !this.getEmitter().isPlaying(this.vocalEvent)) {
            this.vocalEvent = 0L;
        }
        boolean bl = bHasFunctioningVoice = !this.isDead() && !this.isReanimatedForGrappleOnly();
        if (bHasFunctioningVoice && !this.isFakeDead() && bListenerInRange) {
            ZombieVocalsManager.instance.addCharacter(this);
        }
        if (this.vocalEvent != 0L && bHasFunctioningVoice && this.isFakeDead() && this.getEmitter().isPlaying(this.vocalEvent)) {
            this.getEmitter().stopSoundLocal(this.vocalEvent);
            this.vocalEvent = 0L;
        }
    }

    @Override
    public void setVehicleHitLocation(BaseVehicle vehicle) {
        if (!this.getFMODParameters().parameterList.contains(this.parameterVehicleHitLocation)) {
            this.getFMODParameters().add(this.parameterVehicleHitLocation);
        }
        ParameterVehicleHitLocation.HitLocation location = ParameterVehicleHitLocation.calculateLocation(vehicle, this.getX(), this.getY(), this.getZ());
        this.parameterVehicleHitLocation.setLocation(location);
    }

    private void updateSearchForCorpse() {
        if (this.crawling || this.target != null || this.eatBodyTarget != null) {
            this.checkForCorpseTimer = 10000.0f;
            this.bodyToEat = null;
            return;
        }
        if (this.bodyToEat != null) {
            if (this.bodyToEat.getStaticMovingObjectIndex() == -1) {
                this.bodyToEat = null;
            } else if (!this.isEatingOther(this.bodyToEat) && this.bodyToEat.getEatingZombies().size() >= 3) {
                this.bodyToEat = null;
            }
        }
        if (this.bodyToEat == null) {
            this.checkForCorpseTimer -= GameTime.getInstance().getThirtyFPSMultiplier();
            if (this.checkForCorpseTimer <= 0.0f) {
                this.checkForCorpseTimer = 10000.0f;
                tempBodies.clear();
                for (int x = -10; x < 10; ++x) {
                    for (int y = -10; y < 10; ++y) {
                        IsoDeadBody corpse;
                        IsoGridSquare sq = this.getCell().getGridSquare(this.getX() + (float)x, this.getY() + (float)y, this.getZ());
                        if (sq == null || (corpse = sq.getDeadBody()) == null || corpse.isSkeleton() || corpse.isZombie() || corpse.getEatingZombies().size() >= 3 || corpse.isAnimal() || corpse.isAnimalSkeleton() || PolygonalMap2.instance.lineClearCollide(this.getX(), this.getY(), corpse.getX(), corpse.getY(), PZMath.fastfloor(this.getZ()), null, false, true)) continue;
                        tempBodies.add(corpse);
                    }
                }
                if (!tempBodies.isEmpty()) {
                    this.bodyToEat = PZArrayUtil.pickRandom(tempBodies);
                    tempBodies.clear();
                }
            }
        }
        if (this.bodyToEat == null || !this.isCurrentState(ZombieIdleState.instance())) {
            return;
        }
        if (this.DistToSquared(this.bodyToEat) > 1.0f) {
            Vector2 v = tempo.set(this.getX() - this.bodyToEat.getX(), this.getY() - this.bodyToEat.getY());
            v.setLength(0.5f);
            this.pathToLocationF(this.bodyToEat.getX() + v.x, this.bodyToEat.getY() + v.y, this.bodyToEat.getZ());
        }
    }

    private void damageSheetRope() {
        IsoObject sheetRope;
        if (Rand.Next(30) == 0 && this.current != null && (this.current.has(IsoFlagType.climbSheetN) || this.current.has(IsoFlagType.climbSheetE) || this.current.has(IsoFlagType.climbSheetS) || this.current.has(IsoFlagType.climbSheetW)) && (sheetRope = this.current.getSheetRope()) != null) {
            sheetRope.sheetRopeHealth -= (float)Rand.Next(5, 15);
            if (sheetRope.sheetRopeHealth < 40.0f) {
                this.current.damageSpriteSheetRopeFromBottom(null, this.current.has(IsoFlagType.climbSheetN) || this.current.has(IsoFlagType.climbSheetS));
                this.current.RecalcProperties();
            }
            if (sheetRope.sheetRopeHealth <= 0.0f) {
                this.current.removeSheetRopeFromBottom(null, this.current.has(IsoFlagType.climbSheetN) || this.current.has(IsoFlagType.climbSheetS));
            }
        }
    }

    public void getZombieWalkTowardSpeed(float speed, float dist, Vector2 temp) {
        float distMod = dist / 24.0f;
        if (distMod < 1.0f) {
            distMod = 1.0f;
        }
        if (distMod > 1.3f) {
            distMod = 1.3f;
        }
        temp.setLength((speed * this.getSpeedMod() + 0.006f) * distMod);
        if (SandboxOptions.instance.lore.speed.getValue() == 1 && !this.inactive || this.speedType == 1) {
            temp.setLength(0.08f);
            this.running = true;
        }
        if (temp.getLength() > dist) {
            temp.setLength(dist);
        }
    }

    public void getZombieLungeSpeed() {
        this.running = SandboxOptions.instance.lore.speed.getValue() == 1 && !this.inactive || this.speedType == 1;
    }

    public boolean tryThump(IsoGridSquare square) {
        boolean bMoving;
        if (this.ghost) {
            return false;
        }
        if (this.crawling) {
            return false;
        }
        boolean bl = bMoving = this.isCurrentState(PathFindState.instance()) || this.isCurrentState(LungeState.instance()) || this.isCurrentState(LungeNetworkState.instance()) || this.isCurrentState(WalkTowardState.instance()) || this.isCurrentState(WalkTowardNetworkState.instance());
        if (!bMoving) {
            return false;
        }
        IsoGridSquare feeler = square != null ? square : this.getFeelerTile(this.getFeelersize());
        if (feeler == null || this.current == null) {
            return false;
        }
        IsoObject obj = this.current.testCollideSpecialObjects(feeler);
        IsoDoor door = Type.tryCastTo(obj, IsoDoor.class);
        IsoThumpable thumpable = Type.tryCastTo(obj, IsoThumpable.class);
        IsoWindow window = Type.tryCastTo(obj, IsoWindow.class);
        IsoWindowFrame windowFrame = Type.tryCastTo(obj, IsoWindowFrame.class);
        if (window != null && window.canClimbThrough(this)) {
            if (!this.isFacingObject(window, 0.8f)) {
                return false;
            }
            this.climbThroughWindow(window);
            return true;
        }
        if (windowFrame != null && windowFrame.canClimbThrough(this)) {
            if (!this.isFacingObject(windowFrame, 0.8f)) {
                return false;
            }
            this.climbThroughWindowFrame(windowFrame);
            return true;
        }
        if (door != null && door.isHoppable()) {
            if (!this.isFacingObject(door, 0.8f)) {
                return false;
            }
            IsoDirections dir = IsoDirections.fromAngle(door.getX() - (float)this.current.getX(), door.getY() - (float)this.current.getY());
            this.climbOverFence(dir);
            return true;
        }
        if (thumpable != null && thumpable.canClimbThrough(this)) {
            this.climbThroughWindow(thumpable);
            return true;
        }
        if (thumpable != null && thumpable.getThumpableFor(this) != null || window != null && window.getThumpableFor(this) != null || windowFrame != null && windowFrame.getThumpableFor(this) != null || door != null && door.getThumpableFor(this) != null) {
            int difX = feeler.getX() - this.current.getX();
            int difY = feeler.getY() - this.current.getY();
            int absDifX = Math.abs(difX);
            int absDifY = Math.abs(difY);
            IsoDirections dir = IsoDirections.N;
            if (difX < 0 && absDifX > absDifY) {
                dir = IsoDirections.S;
            }
            if (difX < 0 && absDifX <= absDifY) {
                dir = IsoDirections.SW;
            }
            if (difX > 0 && absDifX > absDifY) {
                dir = IsoDirections.W;
            }
            if (difX > 0 && absDifX <= absDifY) {
                dir = IsoDirections.SE;
            }
            if (difY < 0 && absDifX < absDifY) {
                dir = IsoDirections.N;
            }
            if (difY < 0 && absDifX >= absDifY) {
                dir = IsoDirections.NW;
            }
            if (difY > 0 && absDifX < absDifY) {
                dir = IsoDirections.E;
            }
            if (difY > 0 && absDifX >= absDifY) {
                dir = IsoDirections.NE;
            }
            if (this.getDir() == dir) {
                boolean chasingSound;
                boolean bl2 = chasingSound = this.getPathFindBehavior2().isGoalSound() && (this.isCurrentState(PathFindState.instance()) || this.isCurrentState(WalkTowardState.instance()) || this.isCurrentState(WalkTowardNetworkState.instance()));
                if (SandboxOptions.instance.lore.thumpNoChasing.getValue() || this.target != null || chasingSound) {
                    if (window != null && window.getThumpableFor(this) != null) {
                        obj = (IsoObject)window.getThumpableFor(this);
                    }
                    this.setThumpTarget(obj);
                    this.setPath2(null);
                }
            }
            return true;
        }
        return false;
    }

    public void Wander() {
        this.changeState(ZombieIdleState.instance());
    }

    public void DoZombieInventory() {
        this.DoZombieInventory(false);
    }

    public void DoCorpseInventory() {
        this.DoZombieInventory(true);
    }

    private void DoZombieInventory(boolean bRandomCorpse) {
        int i;
        if (this.isReanimatedPlayer() || this.wasFakeDead()) {
            return;
        }
        if (GameServer.server && !this.persistentOutfitInit) {
            this.dressInPersistentOutfitID(this.persistentOutfitId);
        }
        this.getInventory().removeAllItems();
        this.getInventory().setSourceGrid(this.getCurrentSquare());
        this.wornItems.setFromItemVisuals(this.itemVisuals);
        this.wornItems.addItemsToItemContainer(this.getInventory());
        for (i = 0; i < this.attachedItems.size(); ++i) {
            AttachedItem attachedItem = this.attachedItems.get(i);
            InventoryItem item = attachedItem.getItem();
            if (this.getInventory().contains(item)) continue;
            if (item.hasTag(ItemTag.APPLY_OWNER_NAME) && this.getDescriptor() != null) {
                item.nameAfterDescriptor(this.getDescriptor());
            } else if (item.hasTag(ItemTag.MONOGRAM_OWNER_NAME) && this.getDescriptor() != null) {
                item.monogramAfterDescriptor(this.getDescriptor());
            }
            item.setContainer(this.getInventory());
            this.getInventory().getItems().add(item);
        }
        if (this.itemsToSpawnAtDeath != null && !this.itemsToSpawnAtDeath.isEmpty()) {
            for (i = 0; i < this.itemsToSpawnAtDeath.size(); ++i) {
                InventoryItem spawnItem = this.itemsToSpawnAtDeath.get(i);
                if (spawnItem.hasTag(ItemTag.APPLY_OWNER_NAME) && this.getDescriptor() != null) {
                    spawnItem.nameAfterDescriptor(this.getDescriptor());
                } else if (spawnItem.hasTag(ItemTag.MONOGRAM_OWNER_NAME) && this.getDescriptor() != null) {
                    spawnItem.monogramAfterDescriptor(this.getDescriptor());
                }
                ItemSpawner.spawnItem(this.itemsToSpawnAtDeath.get(i), this.inventory);
            }
            this.itemsToSpawnAtDeath.clear();
        }
    }

    public void DoZombieStats() {
        if (SandboxOptions.instance.lore.cognition.getValue() == 1) {
            this.cognition = 1;
        }
        if (SandboxOptions.instance.lore.cognition.getValue() == 4) {
            this.cognition = Rand.Next(100) < SandboxOptions.instance.lore.doorOpeningPercentage.getValue() ? 1 : 0;
        }
        if (this.strength == -1 && SandboxOptions.instance.lore.strength.getValue() == 1) {
            this.strength = 5;
        }
        if (this.strength == -1 && SandboxOptions.instance.lore.strength.getValue() == 2) {
            this.strength = 3;
        }
        if (this.strength == -1 && SandboxOptions.instance.lore.strength.getValue() == 3) {
            this.strength = 1;
        }
        if (this.strength == -1 && SandboxOptions.instance.lore.strength.getValue() == 4) {
            this.strength = Rand.Next(1, 5);
        }
        int memorySetting = SandboxOptions.instance.lore.memory.getValue();
        int randomMemory = -1;
        if (memorySetting == 5) {
            randomMemory = Rand.Next(4);
        } else if (memorySetting == 6) {
            randomMemory = Rand.Next(3) + 1;
        }
        if (this.memory == -1 && memorySetting == 1 || randomMemory == 0) {
            this.memory = 1250;
        }
        if (this.memory == -1 && memorySetting == 2 || randomMemory == 1) {
            this.memory = 800;
        }
        if (this.memory == -1 && memorySetting == 3 || randomMemory == 2) {
            this.memory = 500;
        }
        if (this.memory == -1 && memorySetting == 4 || randomMemory == 3) {
            this.memory = 25;
        }
        this.sight = SandboxOptions.instance.lore.sight.getValue() == 4 ? Rand.Next(3) + 1 : (SandboxOptions.instance.lore.sight.getValue() == 5 ? Rand.Next(2) + 2 : SandboxOptions.instance.lore.sight.getValue());
        this.hearing = SandboxOptions.instance.lore.hearing.getValue() == 4 ? Rand.Next(3) + 1 : (SandboxOptions.instance.lore.hearing.getValue() == 5 ? Rand.Next(2) + 2 : SandboxOptions.instance.lore.hearing.getValue());
        this.doZombieSpeed();
        this.initCanCrawlUnderVehicle();
    }

    public void setWalkType(String walkType) {
        this.walkType = walkType;
    }

    public void DoZombieSpeeds(float spMod) {
        if (this.crawling) {
            this.speedMod = spMod;
            this.def.animFrameIncrease *= 0.8f;
        } else if (Rand.Next(3) != 0 || SandboxOptions.instance.lore.speed.getValue() == 3) {
            this.speedMod = spMod;
            this.speedMod += (float)Rand.Next(1500) / 10000.0f;
            this.walkVariant = this.walkVariant + "1";
            this.def.setFrameSpeedPerFrame(0.24f);
            this.def.animFrameIncrease *= this.speedMod;
        } else if (SandboxOptions.instance.lore.speed.getValue() != 3) {
            this.lunger = true;
            this.speedMod = spMod;
            this.walkVariant = this.walkVariant + "2";
            this.def.setFrameSpeedPerFrame(0.24f);
            this.def.animFrameIncrease *= this.speedMod;
        }
    }

    public boolean isFakeDead() {
        if (SandboxOptions.instance.lore.disableFakeDead.getValue() == 3) {
            this.fakeDead = false;
        }
        if (this.getSquare() != null && this.getSquare().HasStairs() && this.fakeDead) {
            this.fakeDead = false;
        }
        return this.fakeDead;
    }

    public void setFakeDead(boolean bFakeDead) {
        if (this.getSquare() != null && this.getSquare().HasStairs()) {
            this.fakeDead = false;
            return;
        }
        if (bFakeDead && Rand.Next(2) == 0) {
            this.setCrawlerType(2);
        }
        this.fakeDead = bFakeDead;
    }

    public boolean isForceFakeDead() {
        return this.forceFakeDead;
    }

    public void setForceFakeDead(boolean bForceFakeDead) {
        this.forceFakeDead = bForceFakeDead;
    }

    @Override
    public float onHitByVehicle(BaseVehicle vehicle, float impactSpeed, Vector2 hitDir, Vector2 impactPosOnVehicle, boolean pushedBack) {
        if ((this.isProne() || this.isGettingUp()) && this.isFakeDead()) {
            this.setFakeDead(false);
        }
        return super.onHitByVehicle(vehicle, impactSpeed, hitDir, impactPosOnVehicle, pushedBack);
    }

    @Override
    protected void onHitByVehicleDriver(IsoGameCharacter vehicleDriver) {
        super.onHitByVehicleDriver(vehicleDriver);
        if (vehicleDriver != null) {
            this.setTarget(this.attackedBy);
        }
    }

    @Override
    protected void postHitByVehicleUpdateStance(float speed, boolean knockDownAllowed) {
        if (GameServer.server) {
            return;
        }
        boolean staggerBack = this.isStaggerBack();
        boolean knockedDown = this.isKnockedDown();
        boolean becomeCrawler = this.isBecomeCrawler();
        float unbalancedLevel = this.getUnbalancedLevel();
        float unbalancedSpeedAlpha = PZMath.clamp(unbalancedLevel, 0.0f, 1.0f);
        float minSpeedToGuaranteedStagger = PZMath.lerp(2.0f, 0.1f, unbalancedSpeedAlpha);
        float minSpeedToPossibleKnockdown = PZMath.lerp(5.0f, 0.1f, unbalancedSpeedAlpha);
        float maxSpeedToPossibleKnockdown = PZMath.lerp(25.0f, 15.0f, unbalancedSpeedAlpha);
        float minSpeedToPossibleCrawler = this.isProne() ? 6.75f : 2.0f;
        float maxSpeedToPossibleCrawler = 20.5f;
        if (!staggerBack) {
            if (speed > minSpeedToGuaranteedStagger) {
                staggerBack = true;
            } else {
                boolean shouldStagger;
                float staggerRandAlpha = speed / minSpeedToGuaranteedStagger;
                float staggerRandf = PZMath.lerpFunc_EaseOutQuad(staggerRandAlpha);
                int staggerRand = (int)(staggerRandf * 100.0f);
                boolean bl = shouldStagger = staggerRandAlpha > 1.0f || Rand.Next(100) <= staggerRand;
                if (shouldStagger) {
                    staggerBack = true;
                }
            }
        }
        if (staggerBack) {
            unbalancedLevel = PZMath.lerp(unbalancedLevel, 1.0f, 0.35f * GameTime.getInstance().getMultiplier());
            unbalancedLevel = PZMath.clamp(unbalancedLevel, 0.35f, 1.0f);
        }
        if (knockDownAllowed && !knockedDown && speed > minSpeedToPossibleKnockdown) {
            boolean shouldKnockDown;
            float randMultiplier = PZMath.lerp(1.0f, 10.0f, unbalancedLevel);
            float knockDownRandAlpha = randMultiplier * (speed - minSpeedToPossibleKnockdown) / (maxSpeedToPossibleKnockdown - minSpeedToPossibleKnockdown);
            float knockDownRandf = PZMath.lerpFunc_EaseInQuad(knockDownRandAlpha);
            int knockDownRand = (int)(knockDownRandf * 100.0f);
            boolean bl = shouldKnockDown = knockDownRandAlpha > 1.0f || Rand.Next(100) <= knockDownRand;
            if (shouldKnockDown) {
                knockedDown = true;
            }
        }
        if (!becomeCrawler && knockedDown && speed > minSpeedToPossibleCrawler) {
            boolean shouldBecomeCrawler;
            float becomeCrawlerRandAlpha = (speed - minSpeedToPossibleCrawler) / (20.5f - minSpeedToPossibleCrawler);
            float becomeCrawlerRandf = PZMath.lerpFunc_EaseOutQuad(becomeCrawlerRandAlpha);
            int becomeCrawlerRand = (int)(becomeCrawlerRandf * 100.0f);
            boolean bl = shouldBecomeCrawler = becomeCrawlerRandAlpha > 1.0f || Rand.Next(100) <= becomeCrawlerRand;
            if (shouldBecomeCrawler) {
                becomeCrawler = true;
            }
        }
        this.setStaggerBack(staggerBack);
        this.setKnockedDown(knockedDown);
        this.setBecomeCrawler(becomeCrawler);
        this.setUnbalancedLevel(unbalancedLevel);
        unbalancedLevel = PZMath.lerp(unbalancedLevel, 1.0f, 0.1f * GameTime.getInstance().getMultiplier());
        unbalancedLevel = PZMath.clamp(unbalancedLevel, 0.2f, 1.0f);
    }

    @Override
    public void addBloodFromVehicleImpact(float speed) {
        if ((float)Rand.Next(10) > speed) {
            return;
        }
        float dz = 0.6f;
        if (SandboxOptions.instance.bloodLevel.getValue() > 1) {
            int spn = Rand.Next(4, 10);
            if (spn < 1) {
                spn = 1;
            }
            if (Core.lastStand) {
                spn *= 3;
            }
            switch (SandboxOptions.instance.bloodLevel.getValue()) {
                case 2: {
                    spn /= 2;
                    break;
                }
                case 4: {
                    spn *= 2;
                    break;
                }
                case 5: {
                    spn *= 5;
                }
            }
            for (int n = 0; n < spn; ++n) {
                this.splatBlood(2, 0.3f);
            }
        }
        if (SandboxOptions.instance.bloodLevel.getValue() > 1) {
            this.splatBloodFloorBig();
        }
        if (SandboxOptions.instance.bloodLevel.getValue() > 1) {
            this.playBloodSplatterSound();
            new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + 0.6f, this.getHitDir().x * 1.5f, this.getHitDir().y * 1.5f);
            IsoZombie.tempo.x = this.getHitDir().x;
            IsoZombie.tempo.y = this.getHitDir().y;
            int rand = 3;
            int rand2 = 0;
            int nbRepeat = 1;
            switch (SandboxOptions.instance.bloodLevel.getValue()) {
                case 1: {
                    nbRepeat = 0;
                    break;
                }
                case 2: {
                    nbRepeat = 1;
                    rand = 5;
                    rand2 = 2;
                    break;
                }
                case 4: {
                    nbRepeat = 3;
                    rand = 2;
                    break;
                }
                case 5: {
                    nbRepeat = 10;
                    rand = 0;
                }
            }
            for (int i = 0; i < nbRepeat; ++i) {
                if (Rand.Next(this.isCloseKilled() ? 8 : rand) == 0) {
                    new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + 0.6f, this.getHitDir().x * 1.5f, this.getHitDir().y * 1.5f);
                }
                if (Rand.Next(this.isCloseKilled() ? 8 : rand) == 0) {
                    new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + 0.6f, this.getHitDir().x * 1.8f, this.getHitDir().y * 1.8f);
                }
                if (Rand.Next(this.isCloseKilled() ? 8 : rand) == 0) {
                    new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + 0.6f, this.getHitDir().x * 1.9f, this.getHitDir().y * 1.9f);
                }
                if (Rand.Next(this.isCloseKilled() ? 4 : rand2) == 0) {
                    new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + 0.6f, this.getHitDir().x * 3.9f, this.getHitDir().y * 3.9f);
                }
                if (Rand.Next(this.isCloseKilled() ? 4 : rand2) == 0) {
                    new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + 0.6f, this.getHitDir().x * 3.8f, this.getHitDir().y * 3.8f);
                }
                if (Rand.Next(this.isCloseKilled() ? 9 : 6) != 0) continue;
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.Eye, this.getCell(), this.getX(), this.getY(), this.getZ() + 0.6f, this.getHitDir().x * 0.8f, this.getHitDir().y * 0.8f);
            }
        }
    }

    @Override
    public void hitConsequences(HandWeapon weapon, IsoGameCharacter wielder, boolean bIgnoreDamage, float damage, boolean bRemote) {
        IsoPlayer player;
        if (this.isOnlyJawStab() && !this.isCloseKilled()) {
            return;
        }
        super.hitConsequences(weapon, wielder, bIgnoreDamage, damage, bRemote);
        if (Core.debug) {
            boolean isCriticalHit = wielder.isCriticalHit();
            String hitType = isCriticalHit ? "got critically hit for" : "got hit for";
            DebugType.Combat.debugln("Zombie #%d %s %f", this.getID(), hitType, Float.valueOf(damage));
        }
        this.getActionContext().reportEvent("wasHit");
        if (!bRemote) {
            CombatManager.getInstance().processHit(weapon, wielder, this);
        }
        if (!GameClient.client || this.target == null || wielder == this.target || !(IsoUtils.DistanceToSquared(this.getX(), this.getY(), this.target.getX(), this.target.getY()) < 10.0f)) {
            this.setTarget(wielder);
        }
        if (!GameServer.server && !GameClient.client || GameClient.client && wielder instanceof IsoPlayer && (player = (IsoPlayer)wielder).isLocalPlayer() && !this.isRemoteZombie()) {
            this.setKnockedDown(wielder.isCriticalHit() || this.isOnFloor() || this.isAlwaysKnockedDown());
        }
        this.checkClimbOverFenceHit();
        this.checkClimbThroughWindowHit();
        if (this.shouldBecomeCrawler(wielder)) {
            this.setBecomeCrawler(true);
        }
    }

    @Override
    public long playHurtSound() {
        this.parameterZombieState.setState(ParameterZombieState.State.Hit);
        return 0L;
    }

    private void checkClimbOverFenceHit() {
        if (this.isOnFloor()) {
            return;
        }
        ClimbOverFenceState climbOverFenceState = ClimbOverFenceState.instance();
        if (!this.isCurrentState(climbOverFenceState) || !this.getVariableBoolean("ClimbFenceStarted") || this.isVariable("ClimbFenceOutcome", "fall") || this.getVariableBoolean("ClimbFenceFlopped")) {
            return;
        }
        int endX = this.get(ClimbOverFenceState.END_X);
        int endY = this.get(ClimbOverFenceState.END_Y);
        this.climbFenceWindowHit(endX, endY);
    }

    private void checkClimbThroughWindowHit() {
        if (this.isOnFloor()) {
            return;
        }
        if (!this.isCurrentState(ClimbThroughWindowState.instance()) || !this.getVariableBoolean("ClimbWindowStarted") || this.isVariable("ClimbWindowOutcome", "fall") || this.getVariableBoolean("ClimbWindowFlopped")) {
            return;
        }
        ClimbThroughWindowPositioningParams params = ClimbThroughWindowState.instance().getPositioningParams(this);
        if (params == null) {
            return;
        }
        this.climbFenceWindowHit(params.endX, params.endY);
    }

    private void climbFenceWindowHit(int endX, int endY) {
        if (this.getDir() == IsoDirections.W) {
            this.setX((float)endX + 0.9f);
            this.setLastX(this.getX());
        } else if (this.getDir() == IsoDirections.E) {
            this.setX((float)endX + 0.1f);
            this.setLastX(this.getX());
        } else if (this.getDir() == IsoDirections.N) {
            this.setY((float)endY + 0.9f);
            this.setLastY(this.getY());
        } else if (this.getDir() == IsoDirections.S) {
            this.setY((float)endY + 0.1f);
            this.setLastY(this.getY());
        }
        this.setStaggerBack(false);
        this.setKnockedDown(true);
        this.setOnFloor(true);
        this.setFallOnFront(true);
        this.setHitReaction("FenceWindow");
    }

    private boolean shouldBecomeCrawler(IsoGameCharacter attacker) {
        if (this.isBecomeCrawler()) {
            return true;
        }
        if (this.isCrawling()) {
            return false;
        }
        if (Core.lastStand) {
            return false;
        }
        if (this.isDead()) {
            return false;
        }
        if (this.isCloseKilled()) {
            return false;
        }
        IsoPlayer player = Type.tryCastTo(attacker, IsoPlayer.class);
        if (player != null && !player.isAimAtFloor() && player.isDoShove()) {
            return false;
        }
        int chance = 30;
        if (player != null && player.isAimAtFloor() && player.isDoShove()) {
            chance = this.isHitLegsWhileOnFloor() ? 7 : 15;
        }
        return Rand.NextBool(chance);
    }

    @Override
    public void removeFromWorld() {
        this.getEmitter().stopOrTriggerSoundByName("BurningFlesh");
        this.clearAggroList();
        VirtualZombieManager.instance.RemoveZombie(this);
        this.setPath2(null);
        if (PathfindNative.useNativeCode) {
            PathfindNative.instance.cancelRequest(this);
        } else {
            PolygonalMap2.instance.cancelRequest(this);
        }
        if (this.getFinder().progress != AStarPathFinder.PathFindProgress.notrunning && this.getFinder().progress != AStarPathFinder.PathFindProgress.found) {
            this.getFinder().progress = AStarPathFinder.PathFindProgress.notrunning;
        }
        if (this.group != null) {
            this.group.remove(this);
            this.group = null;
        }
        if (GameServer.server && this.onlineId != -1) {
            ServerMap.instance.zombieMap.remove(this.onlineId);
            this.onlineId = (short)-1;
        }
        if (GameClient.client) {
            GameClient.instance.removeZombieFromCache(this);
        }
        this.getCell().getZombieList().remove(this);
        if (GameServer.server) {
            if (this.authOwner != null || this.authOwnerPlayer != null) {
                NetworkZombieManager.getInstance().moveZombie(this, null, null);
            }
            this.zombiePacketUpdated = false;
        }
        this.imposter.destroy();
        super.removeFromWorld();
    }

    public void resetForReuse() {
        this.setCrawler(false);
        this.initializeStates();
        this.getActionContext().setGroup(ActionGroup.getActionGroup("zombie"));
        this.advancedAnimator.OnAnimDataChanged(false);
        this.setStateMachineLocked(false);
        this.setDefaultState();
        if (this.vocalEvent != 0L) {
            this.getEmitter().stopSoundLocal(this.vocalEvent);
            this.vocalEvent = 0L;
        }
        this.parameterZombieState.setState(ParameterZombieState.State.Idle);
        this.setSceneCulled(true);
        this.releaseAnimationPlayer();
        Arrays.fill(this.isVisibleToPlayer, false);
        this.setCurrent(null);
        this.setLast(null);
        this.setOnFloor(false);
        this.setCanWalk(true);
        this.setFallOnFront(false);
        this.setHitTime(0);
        this.strength = -1;
        this.setImmortalTutorialZombie(false);
        this.setOnlyJawStab(false);
        this.setAlwaysKnockedDown(false);
        this.setForceEatingAnimation(false);
        this.setNoTeeth(false);
        this.cognition = -1;
        this.speedType = -1;
        this.bodyToEat = null;
        this.checkForCorpseTimer = 10000.0f;
        this.clearAttachedItems();
        this.target = null;
        this.setEatBodyTarget(null, false);
        this.setSkeleton(false);
        this.setReanimatedPlayer(false);
        this.setBecomeCrawler(false);
        this.setWasFakeDead(false);
        this.setKnifeDeath(false);
        this.setJawStabAttach(false);
        this.setReanimate(false);
        this.DoZombieStats();
        this.alerted = false;
        this.bonusSpotTime = 0.0f;
        this.timeSinceSeenFlesh = 100000.0f;
        this.soundReactDelay = 0.0f;
        this.delayedSound.z = -1;
        this.delayedSound.y = -1;
        this.delayedSound.x = -1;
        this.soundSourceIsPlayerBase = false;
        this.soundSourceIsPlayer = false;
        this.soundSourceRepeating = false;
        this.soundSourceTarget = null;
        this.soundAttract = 0.0f;
        this.soundAttractTimeout = 0.0f;
        if (SandboxOptions.instance.lore.toughness.getValue() == 1) {
            this.setHealth(3.5f + Rand.Next(0.0f, 0.3f));
        }
        if (SandboxOptions.instance.lore.toughness.getValue() == 2) {
            this.setHealth(1.8f + Rand.Next(0.0f, 0.3f));
        }
        if (SandboxOptions.instance.lore.toughness.getValue() == 3) {
            this.setHealth(0.5f + Rand.Next(0.0f, 0.3f));
        }
        if (SandboxOptions.instance.lore.toughness.getValue() == 4) {
            this.setHealth(Rand.Next(0.5f, 3.5f) + Rand.Next(0.0f, 0.3f));
        }
        this.setCollidable(true);
        this.setShootable(true);
        if (this.isOnFire()) {
            IsoFireManager.RemoveBurningCharacter(this);
            this.setOnFire(false);
        }
        if (this.attachedAnimSprite != null) {
            this.attachedAnimSprite.clear();
        }
        this.onlineId = (short)-1;
        this.indoorZombie = false;
        this.setVehicle4TestCollision(null);
        this.clearItemsToSpawnAtDeath();
        this.persistentOutfitId = 0;
        this.persistentOutfitInit = false;
        this.sharedDesc = null;
        this.imposter.destroy();
        if (this.hasModData()) {
            this.getModData().wipe();
        }
        this.setInvulnerable(false);
        this.getWrappedGrappleable().resetGrappleStateToDefault("");
        this.unbalancedLevel = 0.0f;
    }

    public boolean wasFakeDead() {
        return this.wasFakeDead;
    }

    public void setWasFakeDead(boolean wasFakeDead) {
        this.wasFakeDead = wasFakeDead;
    }

    public void setCrawler(boolean crawling) {
        this.crawling = crawling;
    }

    public boolean isBecomeCrawler() {
        return this.becomeCrawler;
    }

    public void setBecomeCrawler(boolean crawler) {
        if (!this.isInvulnerable()) {
            this.becomeCrawler = crawler;
        }
    }

    public boolean isReanimate() {
        return this.reanimate;
    }

    public void setReanimate(boolean reanimate) {
        this.reanimate = reanimate;
    }

    public boolean isReanimatedPlayer() {
        return this.reanimatedPlayer;
    }

    public void setReanimatedPlayer(boolean reanimated) {
        this.reanimatedPlayer = reanimated;
    }

    public IsoPlayer getReanimatedPlayer() {
        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
            IsoPlayer player = IsoPlayer.players[playerIndex];
            if (player == null || player.reanimatedCorpse != this) continue;
            return player;
        }
        return null;
    }

    public void setFemaleEtc(boolean female) {
        this.setFemale(female);
        if (this.getDescriptor() != null) {
            this.getDescriptor().setFemale(female);
            this.getDescriptor().setVoicePrefix(female ? "FemaleZombie" : "MaleZombie");
        }
        this.spriteName = female ? "KateZ" : "BobZ";
        this.hurtSound = this.getDescriptor().getVoicePrefix() + "Hurt";
        if (this.vocalEvent != 0L) {
            String soundName = this.getVoiceSoundName();
            if (this.getEmitter() != null && !this.getEmitter().isPlaying(soundName)) {
                this.getEmitter().stopSoundLocal(this.vocalEvent);
                this.vocalEvent = 0L;
            }
        }
    }

    public void addRandomBloodDirtHolesEtc() {
        BloodBodyPartType part;
        int i;
        float damageFactor;
        if (!this.isSkeleton()) {
            this.addRandomVisualDamages();
            this.addRandomVisualBandages();
        }
        if ((damageFactor = (float)SandboxOptions.instance.clothingDegradation.getValue()) == 1.0f) {
            return;
        }
        damageFactor = (damageFactor - 1.0f) / 2.0f;
        boolean allLayers = (damageFactor *= damageFactor) >= 1.0f;
        this.addBlood(null, false, true, false);
        int worldAgeMonths = (int)(IsoWorld.instance.getWorldAgeDays() / 30.0f * damageFactor);
        this.addLotsOfDirt(null, (int)((float)(Rand.Next(5, 20) + Math.min(worldAgeMonths, 24)) * damageFactor), allLayers);
        int rand = Math.max(8 - worldAgeMonths, 0);
        int rolls = PZMath.clamp(worldAgeMonths, 5, 10);
        rolls *= (int)damageFactor;
        for (i = 0; i < rolls; ++i) {
            if (OutfitRNG.NextBool(rand)) {
                this.addBlood(null, false, true, false);
            }
            if (!OutfitRNG.NextBool(rand / 2)) continue;
            this.addLotsOfDirt(null, null, allLayers);
        }
        rolls = PZMath.clamp(worldAgeMonths, 8, 16);
        rolls *= (int)damageFactor;
        rand /= 2;
        for (i = 0; i < rolls; ++i) {
            if (!OutfitRNG.NextBool(rand)) continue;
            part = BloodBodyPartType.FromIndex(OutfitRNG.Next(0, BloodBodyPartType.MAX.index()));
            this.addHole(part);
            this.addBlood(part, true, false, false);
        }
        for (i = 0; i < rolls; ++i) {
            if (!OutfitRNG.NextBool(rand)) continue;
            part = BloodBodyPartType.FromIndex(OutfitRNG.Next(0, BloodBodyPartType.MAX.index()));
            this.addHole(part);
            this.addLotsOfDirt(part, 1, allLayers);
        }
    }

    public void useDescriptor(SharedDescriptors.Descriptor sharedDesc) {
        this.getHumanVisual().clear();
        this.itemVisuals.clear();
        this.persistentOutfitId = sharedDesc == null ? 0 : sharedDesc.getPersistentOutfitID();
        this.persistentOutfitInit = true;
        this.sharedDesc = sharedDesc;
        if (sharedDesc == null) {
            return;
        }
        this.setFemaleEtc(sharedDesc.isFemale());
        this.getHumanVisual().copyFrom(sharedDesc.getHumanVisual());
        this.getWornItems().setFromItemVisuals(sharedDesc.itemVisuals);
        this.onWornItemsChanged();
    }

    public SharedDescriptors.Descriptor getSharedDescriptor() {
        return this.sharedDesc;
    }

    public int getSharedDescriptorID() {
        return this.getPersistentOutfitID();
    }

    public int getScreenProperX(int playerIndex) {
        return (int)(IsoUtils.XToScreen(this.getX(), this.getY(), this.getZ(), 0) - IsoCamera.cameras[playerIndex].getOffX());
    }

    public int getScreenProperY(int playerIndex) {
        return (int)(IsoUtils.YToScreen(this.getX(), this.getY(), this.getZ(), 0) - IsoCamera.cameras[playerIndex].getOffY());
    }

    @Override
    public BaseVisual getVisual() {
        return this.humanVisual;
    }

    @Override
    public HumanVisual getHumanVisual() {
        return this.humanVisual;
    }

    @Override
    public ItemVisuals getItemVisuals() {
        this.getItemVisuals(this.itemVisuals);
        return this.itemVisuals;
    }

    @Override
    public void getItemVisuals(ItemVisuals itemVisuals) {
        if (this.isUsingWornItems()) {
            this.getWornItems().getItemVisuals(itemVisuals);
        } else if (itemVisuals != this.itemVisuals) {
            itemVisuals.clear();
            PZArrayUtil.addAll(itemVisuals, this.itemVisuals);
        }
    }

    @Override
    public boolean isUsingWornItems() {
        return this.isOnKillDone() || this.isOnDeathDone() || this.isReanimatedPlayer() || this.wasFakeDead();
    }

    public void setAsSurvivor() {
        String outfitName = "Survivalist";
        switch (Rand.Next(5)) {
            case 1: {
                outfitName = "Survivalist02";
                break;
            }
            case 2: {
                outfitName = "Survivalist03";
                break;
            }
            case 3: {
                outfitName = "Survivalist04";
                break;
            }
            case 4: {
                outfitName = "Survivalist05";
            }
        }
        this.dressInPersistentOutfit(outfitName);
    }

    @Override
    public void dressInRandomOutfit() {
        ZombiesZoneDefinition.dressInRandomOutfit(this);
    }

    @Override
    public void dressInNamedOutfit(String outfitName) {
        Outfit outfitSource;
        this.wornItems.clear();
        this.getHumanVisual().clear();
        this.itemVisuals.clear();
        Outfit outfit = outfitSource = this.isFemale() ? OutfitManager.instance.FindFemaleOutfit(outfitName) : OutfitManager.instance.FindMaleOutfit(outfitName);
        if (outfitSource == null) {
            return;
        }
        if (outfitSource.isEmpty()) {
            outfitSource.loadItems();
            this.pendingOutfitName = outfitName;
            return;
        }
        UnderwearDefinition.addRandomUnderwear(this);
        this.getDescriptor().setForename(SurvivorFactory.getRandomForename(this.isFemale()));
        this.getHumanVisual().dressInNamedOutfit(outfitName, this.itemVisuals, false);
        this.getHumanVisual().synchWithOutfit(this.getHumanVisual().getOutfit());
        this.onWornItemsChanged();
    }

    @Override
    public void dressInPersistentOutfitID(int outfitID) {
        this.getHumanVisual().clear();
        this.itemVisuals.clear();
        this.persistentOutfitId = outfitID;
        this.persistentOutfitInit = true;
        if (outfitID == 0) {
            return;
        }
        this.dressInRandomOutfit = false;
        PersistentOutfits.instance.dressInOutfit(this, outfitID);
        this.onWornItemsChanged();
    }

    @Override
    public void dressInClothingItem(String itemGUID) {
        this.wornItems.clear();
        this.getHumanVisual().dressInClothingItem(itemGUID, this.itemVisuals);
        this.onWornItemsChanged();
    }

    @Override
    public boolean onDeath_ShouldDoSplatterAndSounds(HandWeapon weapon, IsoGameCharacter wielder, boolean isGory) {
        if (this.isReanimatedForGrappleOnly()) {
            return false;
        }
        return super.onDeath_ShouldDoSplatterAndSounds(weapon, wielder, isGory);
    }

    @Override
    public void onWornItemsChanged() {
        super.onWornItemsChanged();
        this.parameterShoeType.setShoeType(null);
    }

    @Override
    public void clothingItemChanged(String itemGuid) {
        super.clothingItemChanged(itemGuid);
        if (!StringUtils.isNullOrWhitespace(this.pendingOutfitName)) {
            Outfit outfitSource;
            Outfit outfit = outfitSource = this.isFemale() ? OutfitManager.instance.FindFemaleOutfit(this.pendingOutfitName) : OutfitManager.instance.FindMaleOutfit(this.pendingOutfitName);
            if (outfitSource != null && !outfitSource.isEmpty()) {
                this.dressInNamedOutfit(this.pendingOutfitName);
                this.pendingOutfitName = null;
                this.resetModelNextFrame();
            }
        }
    }

    public boolean WanderFromWindow() {
        if (this.getCurrentSquare() == null) {
            return false;
        }
        FloodFill ff = floodFill;
        ff.calculate(this, this.getCurrentSquare());
        IsoGridSquare sq = ff.choose();
        ff.reset();
        if (sq != null) {
            this.pathToLocation(sq.getX(), sq.getY(), sq.getZ());
            return true;
        }
        return false;
    }

    public boolean isUseless() {
        return this.useless;
    }

    public void setUseless(boolean useless) {
        this.useless = useless;
    }

    public void setImmortalTutorialZombie(boolean immortal) {
        this.immortalTutorialZombie = immortal;
    }

    public boolean isTargetInCone(float dist, float dot) {
        if (this.target == null) {
            return false;
        }
        tempo.set(this.target.getX() - this.getX(), this.target.getY() - this.getY());
        float length = tempo.getLength();
        if (length == 0.0f) {
            return true;
        }
        if (length > dist) {
            return false;
        }
        tempo.normalize();
        this.getVectorFromDirection(tempo2);
        float facing = tempo.dot(tempo2);
        return facing >= dot;
    }

    @Override
    public boolean isCrawling() {
        return this.crawling;
    }

    public boolean isCanCrawlUnderVehicle() {
        return this.canCrawlUnderVehicle;
    }

    public void setCanCrawlUnderVehicle(boolean b) {
        this.canCrawlUnderVehicle = b;
    }

    public boolean isCanWalk() {
        return this.canWalk;
    }

    public void setCanWalk(boolean bCanStand) {
        this.canWalk = bCanStand;
    }

    public void initCanCrawlUnderVehicle() {
        int chance = 100;
        switch (SandboxOptions.instance.lore.crawlUnderVehicle.getValue()) {
            case 1: {
                chance = 0;
                break;
            }
            case 2: {
                chance = 5;
                break;
            }
            case 3: {
                chance = 10;
                break;
            }
            case 4: {
                chance = 25;
                break;
            }
            case 5: {
                chance = 50;
                break;
            }
            case 6: {
                chance = 75;
                break;
            }
            case 7: {
                chance = 100;
            }
        }
        this.setCanCrawlUnderVehicle(Rand.Next(100) < chance);
    }

    public boolean shouldGetUpFromCrawl() {
        float targetY;
        float targetX;
        if (this.isCurrentState(ZombieGetUpFromCrawlState.instance())) {
            return true;
        }
        if (this.isCurrentState(ZombieGetUpState.instance())) {
            return this.stateMachine.getPrevious() == ZombieGetUpFromCrawlState.instance();
        }
        if (!this.isCrawling()) {
            return false;
        }
        if (!this.isCanWalk()) {
            return false;
        }
        if (this.isCurrentState(PathFindState.instance())) {
            if (this.stateMachine.getPrevious() == ZombieGetDownState.instance() && ZombieGetDownState.instance().isNearStartXY(this)) {
                return false;
            }
            return this.getPathFindBehavior2().shouldGetUpFromCrawl();
        }
        if (this.isCurrentState(WalkTowardState.instance()) && this.DistToSquared(targetX = this.getPathFindBehavior2().getTargetX(), targetY = this.getPathFindBehavior2().getTargetY()) > 0.010000001f && PolygonalMap2.instance.lineClearCollide(this.getX(), this.getY(), targetX, targetY, PZMath.fastfloor(this.getZ()), null)) {
            return false;
        }
        if (this.isCurrentState(ZombieGetDownState.instance())) {
            return false;
        }
        return PolygonalMap2.instance.canStandAt(this.getX(), this.getY(), this.getZi(), null, false, true);
    }

    public void toggleCrawling() {
        boolean bCanCrawlUnderVehicle = this.canCrawlUnderVehicle;
        if (this.crawling) {
            this.setCrawler(false);
            this.setKnockedDown(false);
            this.setStaggerBack(false);
            this.setFallOnFront(false);
            this.setOnFloor(false);
            this.DoZombieStats();
        } else {
            this.setCrawler(true);
            this.setOnFloor(true);
            this.DoZombieStats();
            this.walkVariant = "ZombieWalk";
        }
        this.canCrawlUnderVehicle = bCanCrawlUnderVehicle;
    }

    public void knockDown(boolean hitFromBehind) {
        this.setKnockedDown(true);
        this.setStaggerBack(true);
        this.setHitReaction("");
        this.setPlayerAttackPosition(hitFromBehind ? "BEHIND" : null);
        this.setHitForce(1.0f);
        this.reportEvent("wasHit");
    }

    public void addItemToSpawnAtDeath(InventoryItem item) {
        if (item == null) {
            return;
        }
        if (this.itemsToSpawnAtDeath == null) {
            this.itemsToSpawnAtDeath = new ArrayList();
        }
        if (this.itemsToSpawnAtDeath.contains(item)) {
            return;
        }
        this.itemsToSpawnAtDeath.add(item);
    }

    public void clearItemsToSpawnAtDeath() {
        if (this.itemsToSpawnAtDeath != null) {
            this.itemsToSpawnAtDeath.clear();
        }
    }

    public IsoMovingObject getEatBodyTarget() {
        return this.eatBodyTarget;
    }

    public float getEatSpeed() {
        return this.eatSpeed;
    }

    public void setEatBodyTarget(IsoMovingObject target, boolean force) {
        this.setEatBodyTarget(target, force, Rand.Next(0.64f, 0.96f));
    }

    public void setEatBodyTarget(IsoMovingObject target, boolean force, float eatSpeed) {
        if (target == this.eatBodyTarget) {
            return;
        }
        if (!force && target != null && target.getEatingZombies().size() >= 3) {
            return;
        }
        if (this.eatBodyTarget != null) {
            this.eatBodyTarget.getEatingZombies().remove(this);
        }
        this.eatBodyTarget = target;
        if (target == null) {
            return;
        }
        this.eatBodyTarget.getEatingZombies().add(this);
        this.eatSpeed = eatSpeed;
    }

    private void updateEatBodyTarget() {
        IsoPlayer player;
        if (this.bodyToEat != null && this.isCurrentState(ZombieIdleState.instance()) && this.DistToSquared(this.bodyToEat) <= 1.0f && PZMath.fastfloor(this.getZ()) == PZMath.fastfloor(this.bodyToEat.getZ())) {
            this.setEatBodyTarget(this.bodyToEat, false);
            this.bodyToEat = null;
        }
        if (this.eatBodyTarget == null) {
            return;
        }
        if (this.eatBodyTarget instanceof IsoDeadBody && this.eatBodyTarget.getStaticMovingObjectIndex() == -1) {
            this.setEatBodyTarget(null, false);
        }
        if (this.target != null && !this.target.isOnFloor() && this.target != this.eatBodyTarget) {
            this.setEatBodyTarget(null, false);
        }
        if ((player = Type.tryCastTo(this.eatBodyTarget, IsoPlayer.class)) != null && player.reanimatedCorpse != null) {
            this.setEatBodyTarget(null, false);
        }
        if (player != null && player.isAlive() && !player.isOnFloor() && !player.isCurrentState(PlayerHitReactionState.instance())) {
            this.setEatBodyTarget(null, false);
        }
        if (!this.isCurrentState(ZombieEatBodyState.instance()) && this.eatBodyTarget != null && this.DistToSquared(this.eatBodyTarget) > 1.0f) {
            this.setEatBodyTarget(null, false);
        }
        if (this.eatBodyTarget != null && this.eatBodyTarget.getSquare() != null && this.current != null && this.current.isSomethingTo(this.eatBodyTarget.getSquare())) {
            this.setEatBodyTarget(null, false);
        }
    }

    private void updateCharacterTextureAnimTime() {
        boolean bMoving = this.isCurrentState(WalkTowardState.instance()) || this.isCurrentState(PathFindState.instance());
        this.characterTextureAnimDuration = bMoving ? 0.67f : 2.0f;
        this.characterTextureAnimTime += GameTime.getInstance().getTimeDelta();
        if (this.characterTextureAnimTime > this.characterTextureAnimDuration) {
            this.characterTextureAnimTime %= this.characterTextureAnimDuration;
        }
    }

    public int getCrawlerType() {
        return this.crawlerType;
    }

    public void setCrawlerType(int crawlerType) {
        this.crawlerType = crawlerType;
    }

    public void addRandomVisualBandages() {
        if ("Tutorial".equals(Core.getInstance().getGameMode())) {
            return;
        }
        for (int i = 0; i < 5; ++i) {
            if (OutfitRNG.Next(10) != 0) continue;
            BodyPartType bodyPart = BodyPartType.getRandom();
            String itemType = bodyPart.getBandageModel() + "_Blood";
            this.addBodyVisualFromItemType(itemType);
        }
    }

    public void addVisualBandage(BodyPartType bodyPart, boolean bloody) {
        String itemType = bodyPart.getBandageModel() + (bloody ? "_Blood" : "");
        this.addBodyVisualFromItemType(itemType);
    }

    public void addRandomVisualDamages() {
        for (int i = 0; i < 5; ++i) {
            if (OutfitRNG.Next(5) != 0) continue;
            String visualDmg = OutfitRNG.pickRandom(ScriptManager.instance.getZedDmgMap());
            this.addBodyVisualFromItemType("Base." + visualDmg);
        }
    }

    public String getPlayerAttackPosition() {
        return this.playerAttackPosition;
    }

    public void setPlayerAttackPosition(String playerAttackPosition) {
        this.playerAttackPosition = playerAttackPosition;
    }

    public boolean isSitAgainstWall() {
        return this.sitAgainstWall;
    }

    public void setSitAgainstWall(boolean sitAgainstWall) {
        this.sitAgainstWall = sitAgainstWall;
        this.networkAi.extraUpdate();
    }

    @Override
    public boolean isSkeleton() {
        if (Core.debug && DebugOptions.instance.model.forceSkeleton.getValue()) {
            this.getHumanVisual().setSkinTextureIndex(2);
            return true;
        }
        return this.isSkeleton;
    }

    @Override
    public boolean isZombie() {
        return true;
    }

    public void setSkeleton(boolean isSkeleton) {
        this.isSkeleton = isSkeleton;
        if (isSkeleton) {
            this.getHumanVisual().setHairModel("");
            this.getHumanVisual().setBeardModel("");
            ModelManager.instance.Reset(this);
        }
    }

    public int getHitTime() {
        return this.hitTime;
    }

    public void setHitTime(int hitTime) {
        this.hitTime = hitTime;
    }

    public int getThumpTimer() {
        return this.thumpTimer;
    }

    public void setThumpTimer(int thumpTimer) {
        this.thumpTimer = thumpTimer;
    }

    public IsoMovingObject getTarget() {
        return this.target;
    }

    public void setTargetSeenTime(float seconds) {
        this.targetSeenTime = seconds;
    }

    public float getTargetSeenTime() {
        return this.targetSeenTime;
    }

    public boolean isTargetVisible() {
        IsoPlayer player = Type.tryCastTo(this.target, IsoPlayer.class);
        if (player == null || this.getCurrentSquare() == null) {
            return false;
        }
        return GameServer.server ? ServerLOS.instance.isCouldSee(player, this.getCurrentSquare()) : this.getCurrentSquare().isCouldSee(player.getPlayerNum());
    }

    @Override
    public float getTurnDelta() {
        return this.turnDeltaNormal;
    }

    @Override
    public boolean isAttacking() {
        return this.isZombieAttacking();
    }

    @Override
    public boolean isZombieAttacking() {
        State currentState = this.getCurrentState();
        return currentState != null && currentState.isAttacking(this);
    }

    @Override
    public boolean isZombieAttacking(IsoMovingObject other) {
        if (GameClient.client && this.authOwner == null) {
            return this.legsSprite != null && this.legsSprite.currentAnim != null && "ZombieBite".equals(this.legsSprite.currentAnim.name);
        }
        return other == this.target && this.isCurrentState(AttackState.instance());
    }

    public int getHitHeadWhileOnFloor() {
        return this.hitHeadWhileOnFloor;
    }

    public String getRealState() {
        return this.realState.toString();
    }

    public void setHitHeadWhileOnFloor(int hitHeadWhileOnFloor) {
        this.hitHeadWhileOnFloor = hitHeadWhileOnFloor;
        this.networkAi.extraUpdate();
    }

    public boolean isHitLegsWhileOnFloor() {
        return this.hitLegsWhileOnFloor;
    }

    public void setHitLegsWhileOnFloor(boolean hitLegsWhileOnFloor) {
        this.hitLegsWhileOnFloor = hitLegsWhileOnFloor;
    }

    public void makeInactive(boolean binactive) {
        if (binactive == this.inactive) {
            return;
        }
        if (binactive) {
            this.walkType = Integer.toString(Rand.Next(3) + 1);
            this.walkType = "slow" + this.walkType;
            this.running = false;
            this.inactive = true;
            this.speedType = 3;
        } else {
            this.speedType = -1;
            this.inactive = false;
            this.DoZombieStats();
        }
    }

    public float getFootstepVolume() {
        return this.footstepVolume;
    }

    public boolean isFacingTarget() {
        if (this.target == null) {
            return false;
        }
        if (GameClient.client && !this.isLocal() && this.isBumped()) {
            return false;
        }
        tempo.set(this.target.getX() - this.getX(), this.target.getY() - this.getY()).normalize();
        if (tempo.getLength() == 0.0f) {
            return true;
        }
        this.getLookVector(tempo2);
        float dot = Vector2.dot(IsoZombie.tempo.x, IsoZombie.tempo.y, IsoZombie.tempo2.x, IsoZombie.tempo2.y);
        return dot >= 0.8f;
    }

    public boolean isTargetLocationKnown() {
        if (this.target == null) {
            return false;
        }
        if (this.bonusSpotTime > 0.0f) {
            return true;
        }
        return this.timeSinceSeenFlesh < 1.0f;
    }

    protected int getSandboxMemoryDuration() {
        int sandbox = SandboxOptions.instance.lore.memory.getValue();
        int memory = 160;
        if (this.inactive) {
            memory = 5;
        } else if (sandbox == 1) {
            memory = 250;
        } else if (sandbox == 3) {
            memory = 100;
        } else if (sandbox == 4) {
            memory = 5;
        }
        return memory *= 5;
    }

    public boolean shouldDoFenceLunge() {
        if (!SandboxOptions.instance.lore.zombiesFenceLunge.getValue()) {
            return false;
        }
        if (Rand.NextBool(3)) {
            return false;
        }
        IsoGameCharacter character = Type.tryCastTo(this.target, IsoGameCharacter.class);
        if (character == null || PZMath.fastfloor(character.getZ()) != PZMath.fastfloor(this.getZ())) {
            return false;
        }
        if (character.getVehicle() != null) {
            return false;
        }
        return this.DistTo(character) < 3.9f;
    }

    @Override
    public boolean isProne() {
        if (this.isOnFloor()) {
            return true;
        }
        if (this.isCrawling()) {
            return true;
        }
        if (this.isCurrentState(ZombieEatBodyState.instance())) {
            return true;
        }
        if (this.isCurrentState(FakeDeadZombieState.instance())) {
            return true;
        }
        if (this.isCurrentState(ZombieOnGroundState.instance())) {
            return true;
        }
        return this.isSitAgainstWall();
    }

    @Override
    public boolean isGettingUp() {
        return this.getCurrentState() == ZombieGetUpState.instance();
    }

    public void setTarget(IsoMovingObject t) {
        if (this.target == t) {
            return;
        }
        this.target = t;
        this.networkAi.extraUpdate();
    }

    public boolean isAlwaysKnockedDown() {
        return this.alwaysKnockedDown;
    }

    public void setAlwaysKnockedDown(boolean alwaysKnockedDown) {
        this.alwaysKnockedDown = alwaysKnockedDown;
    }

    public void setDressInRandomOutfit(boolean dressInRandom) {
        this.dressInRandomOutfit = dressInRandom;
    }

    public void setBodyToEat(IsoDeadBody body) {
        this.bodyToEat = body;
    }

    public boolean isForceEatingAnimation() {
        return this.forceEatingAnimation;
    }

    public void setForceEatingAnimation(boolean forceEatingAnimation) {
        this.forceEatingAnimation = forceEatingAnimation;
    }

    public boolean isOnlyJawStab() {
        return this.onlyJawStab;
    }

    public void setOnlyJawStab(boolean onlyJawStab) {
        this.onlyJawStab = onlyJawStab;
    }

    public boolean isNoTeeth() {
        return this.noTeeth;
    }

    public boolean cantBite() {
        if (this.getWornItem(ItemBodyLocation.MASK) != null && !this.getWornItem(ItemBodyLocation.MASK).hasTag(ItemTag.CAN_EAT)) {
            return true;
        }
        if (this.getWornItem(ItemBodyLocation.MASK_EYES) != null && !this.getWornItem(ItemBodyLocation.MASK_EYES).hasTag(ItemTag.CAN_EAT)) {
            return true;
        }
        if (this.getWornItem(ItemBodyLocation.MASK_FULL) != null && !this.getWornItem(ItemBodyLocation.MASK_FULL).hasTag(ItemTag.CAN_EAT)) {
            return true;
        }
        if (this.getWornItem(ItemBodyLocation.FULL_HAT) != null && !this.getWornItem(ItemBodyLocation.FULL_HAT).hasTag(ItemTag.CAN_EAT)) {
            return true;
        }
        if (this.getWornItem(ItemBodyLocation.FULL_SUIT_HEAD) != null && !this.getWornItem(ItemBodyLocation.FULL_SUIT_HEAD).hasTag(ItemTag.CAN_EAT)) {
            return true;
        }
        ItemVisuals tempItemVisuals = new ItemVisuals();
        this.getItemVisuals(tempItemVisuals);
        for (int i = 0; i < tempItemVisuals.size(); ++i) {
            ItemVisual itemVisual = (ItemVisual)tempItemVisuals.get(i);
            Item scriptItem = itemVisual.getScriptItem();
            if (scriptItem == null || !scriptItem.isItemType(ItemType.CLOTHING)) continue;
            if (scriptItem.isBodyLocation(ItemBodyLocation.MASK) && !scriptItem.hasTag(ItemTag.CAN_EAT)) {
                return true;
            }
            if (scriptItem.isBodyLocation(ItemBodyLocation.MASK_EYES) && !scriptItem.hasTag(ItemTag.CAN_EAT)) {
                return true;
            }
            if (scriptItem.isBodyLocation(ItemBodyLocation.MASK_FULL) && !scriptItem.hasTag(ItemTag.CAN_EAT)) {
                return true;
            }
            if (scriptItem.isBodyLocation(ItemBodyLocation.FULL_HAT) && !scriptItem.hasTag(ItemTag.CAN_EAT)) {
                return true;
            }
            if (!scriptItem.isBodyLocation(ItemBodyLocation.FULL_SUIT_HEAD) || scriptItem.hasTag(ItemTag.CAN_EAT)) continue;
            return true;
        }
        return this.isNoTeeth();
    }

    public void setNoTeeth(boolean noTeeth) {
        this.noTeeth = noTeeth;
    }

    public void setThumpFlag(int v) {
        if (this.thumpFlag == v) {
            return;
        }
        this.thumpFlag = v;
        this.networkAi.extraUpdate();
    }

    public void setThumpCondition(float condition) {
        this.thumpCondition = PZMath.clamp_01(condition);
    }

    public void setThumpCondition(int condition, int maxCondition) {
        if (maxCondition <= 0) {
            this.thumpCondition = 0.0f;
            return;
        }
        condition = PZMath.clamp(condition, 0, maxCondition);
        this.thumpCondition = (float)condition / (float)maxCondition;
    }

    @Override
    public float getThumpCondition() {
        return this.thumpCondition;
    }

    @Override
    public boolean isStaggerBack() {
        return this.staggerBack;
    }

    public void setStaggerBack(boolean bStaggerBack) {
        this.staggerBack = bStaggerBack;
    }

    public boolean isKnifeDeath() {
        return this.knifeDeath;
    }

    public void setKnifeDeath(boolean bKnifeDeath) {
        this.knifeDeath = bKnifeDeath;
    }

    public boolean isJawStabAttach() {
        return this.jawStabAttach;
    }

    public void setJawStabAttach(boolean bJawStabAttach) {
        this.jawStabAttach = bJawStabAttach;
    }

    public void Kill(HandWeapon handWeapon, IsoGameCharacter killer, boolean bGory) {
        if (this.isOnKillDone()) {
            return;
        }
        if (GameServer.server) {
            ConnectionQueueStatistic.getInstance().zombiesKilledToday.increase();
            if (this.isOnFire()) {
                ConnectionQueueStatistic.getInstance().zombiesKilledByFireToday.increase();
            }
        }
        super.Kill(handWeapon, killer);
        if (!GameClient.client) {
            this.DoZombieInventory();
        }
        LuaEventManager.triggerEvent("OnZombieDead", this);
        if (killer == null) {
            this.DoDeath(null, null, bGory);
        } else if (handWeapon != null) {
            this.DoDeath(handWeapon, killer, bGory);
        } else if (killer.getPrimaryHandItem() instanceof HandWeapon) {
            this.DoDeath((HandWeapon)killer.getPrimaryHandItem(), killer, bGory);
        } else {
            this.DoDeath(this.getUseHandWeapon(), killer, bGory);
        }
    }

    public void Kill(IsoGameCharacter killer, boolean bGory) {
        if (this.isOnKillDone()) {
            return;
        }
        if (GameServer.server) {
            ConnectionQueueStatistic.getInstance().zombiesKilledToday.increase();
            if (this.isOnFire()) {
                ConnectionQueueStatistic.getInstance().zombiesKilledByFireToday.increase();
            }
        }
        if (!GameClient.client) {
            this.DoZombieInventory();
        }
        super.Kill(killer);
        LuaEventManager.triggerEvent("OnZombieDead", this);
        if (killer == null) {
            this.DoDeath(null, null, bGory);
        } else if (killer.getPrimaryHandItem() instanceof HandWeapon) {
            this.DoDeath((HandWeapon)killer.getPrimaryHandItem(), killer, bGory);
        } else {
            this.DoDeath(this.getUseHandWeapon(), killer, bGory);
        }
    }

    @Override
    public void Kill(HandWeapon handWeapon, IsoGameCharacter killer) {
        this.Kill(handWeapon, killer, true);
    }

    @Override
    public void Kill(IsoGameCharacter killer) {
        this.Kill(killer, true);
    }

    @Override
    public IsoDeadBody becomeCorpse() {
        if (this.isOnDeathDone()) {
            return null;
        }
        super.becomeCorpse();
        IsoDeadBody body = new IsoDeadBody(this);
        if (GameServer.server) {
            GameServer.sendCharacterDeath(body);
        }
        return body;
    }

    public IsoDeadBody becomeCorpseSilently() {
        if (this.isOnDeathDone()) {
            return null;
        }
        super.becomeCorpse();
        return new IsoDeadBody(this);
    }

    @Override
    public HitReactionNetworkAI getHitReactionNetworkAI() {
        return this.networkAi.hitReaction;
    }

    @Override
    public NetworkCharacterAI getNetworkCharacterAI() {
        return this.networkAi;
    }

    @Override
    public boolean isLocal() {
        return super.isLocal() || !this.isRemoteZombie();
    }

    @Override
    public boolean isSkipResolveCollision() {
        return super.isSkipResolveCollision() || this.isCurrentState(ZombieHitReactionState.instance()) || this.isCurrentState(ZombieFallDownState.instance()) || this.isCurrentState(ZombieOnGroundState.instance()) || this.isCurrentState(StaggerBackState.instance());
    }

    private void updateVisionRadius() {
        ClimateManager cm = ClimateManager.getInstance();
        float rainPenalty = cm.getRainIntensity() * 2.5f;
        float fogPenalty = cm.getFogIntensity() * 7.0f;
        IsoPlayer character = Type.tryCastTo(this.target, IsoPlayer.class);
        float darknessPenalty = character != null && character.getCurrentSquare() != null ? (1.0f - this.target.getCurrentSquare().getLightLevel(character.getPlayerNum())) * 5.0f : (1.0f - Math.min(cm.getDayLightStrength(), cm.getAmbient())) * 5.0f;
        float visionRadiusAdjusted = this.getVisionRadiusAdjusted(darknessPenalty, rainPenalty, fogPenalty);
        this.visionRadiusResult = PZMath.clamp(visionRadiusAdjusted, 10.0f, 20.0f);
    }

    private float getVisionRadiusAdjusted(float darknessPenalty, float rainPenalty, float fogPenalty) {
        float visionRadiusAdjusted = 20.0f - Math.max(darknessPenalty, rainPenalty + fogPenalty);
        if (this.sight == 1 || SandboxOptions.instance.lore.sight.getValue() == 1) {
            visionRadiusAdjusted *= 1.75f;
        }
        if (this.sight == 3 || SandboxOptions.instance.lore.sight.getValue() == 3) {
            visionRadiusAdjusted *= 0.35f;
        }
        visionRadiusAdjusted /= this.getWornItemsVisionModifier();
        if (this.getEatBodyTarget() != null) {
            visionRadiusAdjusted *= 0.5f;
        }
        return visionRadiusAdjusted;
    }

    public boolean shouldZombieHaveKey(boolean allowBandits) {
        String outfitName = this.getOutfitName();
        boolean isKey = true;
        if (this.isSkeleton() && this.getHumanVisual() != null) {
            return false;
        }
        if (this.getOutfitName() == null) {
            return isKey;
        }
        if (outfitName.contains("Inmate")) {
            isKey = false;
        } else if (outfitName.contains("Backpacker")) {
            isKey = false;
        } else if (outfitName.contains("Evacuee") && !allowBandits) {
            isKey = false;
        } else if (outfitName.contains("Stripper")) {
            isKey = false;
        } else if (outfitName.contains("Naked")) {
            isKey = false;
        } else if (outfitName.equals("Bandit") && !allowBandits) {
            isKey = false;
        } else if (outfitName.equals("Bathrobe")) {
            isKey = false;
        } else if (outfitName.equals("Bedroom")) {
            isKey = false;
        } else if (outfitName.equals("Hobbo")) {
            isKey = false;
        } else if (outfitName.equals("HockeyPyscho")) {
            isKey = false;
        } else if (outfitName.equals("HospitalPatient")) {
            isKey = false;
        } else if (outfitName.equals("Swimmer")) {
            isKey = false;
        }
        return isKey;
    }

    private void checkZombieEntersPlayerBuilding() {
        IsoPlayer player = Type.tryCastTo(this.target, IsoPlayer.class);
        if (player == null || !player.isLocalPlayer()) {
            return;
        }
        if (this.getCurrentSquare() == null || !this.getCurrentSquare().isCanSee(player.playerIndex)) {
            return;
        }
        IsoBuilding building = this.getCurrentBuilding();
        if (building == null) {
            return;
        }
        IsoBuilding playerBuilding = player.getCurrentBuilding();
        if (building != playerBuilding) {
            return;
        }
        Object object = this.getMusicIntensityEventModData("ZombieEntersPlayerBuilding");
        if (object == null) {
            this.setMusicIntensityEventModData("ZombieEntersPlayerBuilding", GameTime.getInstance().getWorldAgeHours());
            player.triggerMusicIntensityEvent("ZombieEntersPlayerBuilding");
        }
    }

    public void doZombieSpeed() {
        this.doZombieSpeedInternal(this.speedType);
    }

    public void doZombieSpeed(int zombieSpeed) {
        this.doZombieSpeedInternal(zombieSpeed);
    }

    private void doZombieSpeedInternal(int zombieSpeed) {
        zombieSpeed = this.determineZombieSpeed(zombieSpeed);
        if (this.crawling) {
            this.doCrawlerSpeed(zombieSpeed);
        } else if (SandboxOptions.instance.lore.speed.getValue() == 3 || zombieSpeed == 3) {
            this.doShambler();
        } else if (Rand.Next(3) != 0) {
            this.doFakeShambler(zombieSpeed);
        } else if (SandboxOptions.instance.lore.speed.getValue() == 2 || zombieSpeed == 2) {
            this.doFastShambler();
        } else if (SandboxOptions.instance.lore.speed.getValue() == 1 || zombieSpeed == 1) {
            this.doSprinter();
        }
    }

    private void doCrawlerSpeed(int zombieSpeed) {
        this.speedMod = 0.3f;
        this.speedMod += (float)Rand.Next(1500) / 10000.0f;
        this.def.animFrameIncrease *= 0.8f;
        this.doZombieSpeedInternal2(zombieSpeed);
    }

    private void doSprinter() {
        this.lunger = true;
        this.speedMod = 0.85f;
        this.walkVariant = "ZombieWalk2";
        this.speedMod += (float)Rand.Next(1500) / 10000.0f;
        this.def.setFrameSpeedPerFrame(0.24f);
        this.def.animFrameIncrease *= this.speedMod;
        this.speedType = 1;
        this.doZombieSpeedInternal2();
    }

    private void doFastShambler() {
        this.lunger = true;
        this.speedMod = 0.85f;
        this.walkVariant = "ZombieWalk2";
        this.speedMod += (float)Rand.Next(1500) / 10000.0f;
        this.def.setFrameSpeedPerFrame(0.24f);
        this.def.animFrameIncrease *= this.speedMod;
        this.speedType = 2;
        this.doZombieSpeedInternal2();
    }

    private void doFakeShambler() {
        this.speedMod = 0.55f;
        this.speedMod += (float)Rand.Next(1500) / 10000.0f;
        this.walkVariant = "ZombieWalk1";
        this.def.setFrameSpeedPerFrame(0.24f);
        this.def.animFrameIncrease *= this.speedMod;
        this.doZombieSpeedInternal2();
    }

    private void doFakeShambler(int zombieSpeed) {
        this.speedMod = 0.55f;
        this.speedMod += (float)Rand.Next(1500) / 10000.0f;
        this.walkVariant = "ZombieWalk1";
        this.def.setFrameSpeedPerFrame(0.24f);
        this.def.animFrameIncrease *= this.speedMod;
        this.doZombieSpeedInternal2(zombieSpeed);
    }

    private void doShambler() {
        this.speedMod = 0.55f;
        this.speedMod += (float)Rand.Next(1500) / 10000.0f;
        this.walkVariant = "ZombieWalk1";
        this.def.setFrameSpeedPerFrame(0.24f);
        this.def.animFrameIncrease *= this.speedMod;
        this.speedType = 3;
        this.doZombieSpeedInternal2();
    }

    private void doZombieSpeedInternal2() {
        this.doZombieSpeedInternal2(this.speedType);
    }

    private void doZombieSpeedInternal2(int zombieSpeed) {
        if (zombieSpeed == 3) {
            this.walkType = Integer.toString(Rand.Next(3) + 1);
            this.walkType = "slow" + this.walkType;
        } else {
            this.walkType = Integer.toString(Rand.Next(5) + 1);
        }
        if (zombieSpeed == 1) {
            this.setTurnDelta(1.0f);
            this.walkType = "sprint" + this.walkType;
        }
        this.speedType = zombieSpeed;
    }

    private int determineZombieSpeed(int zombieSpeed) {
        if (zombieSpeed != -1) {
            return zombieSpeed;
        }
        zombieSpeed = SandboxOptions.instance.lore.speed.getValue();
        if (this.inactive) {
            zombieSpeed = 3;
        } else if (zombieSpeed == 4) {
            zombieSpeed = Rand.Next(100) < SandboxOptions.instance.lore.sprinterPercentage.getValue() ? 1 : Rand.Next(2) + 2;
        }
        return zombieSpeed;
    }

    public String getLastHitPart() {
        if (this.lastHitPart == null) {
            return null;
        }
        return this.lastHitPart.toString();
    }

    public boolean shouldDressInRandomOutfit() {
        return this.dressInRandomOutfit;
    }

    @Override
    public String getOutfitName() {
        if (this.shouldDressInRandomOutfit()) {
            ModelManager.instance.dressInRandomOutfit(this);
        }
        if (this.getHumanVisual() != null) {
            HumanVisual humanVisual = this.getHumanVisual();
            Outfit outfit = humanVisual.getOutfit();
            return outfit == null ? null : outfit.name;
        }
        return null;
    }

    public IsoGridSquare getHeadSquare(IsoPlayer player) {
        if (!this.isCurrentState(ClimbThroughWindowState.instance())) {
            return null;
        }
        if (!this.hasAnimationPlayer()) {
            return null;
        }
        if (this.DistToSquared(player) > 10.0f) {
            return null;
        }
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        if (!animationPlayer.isReady()) {
            return null;
        }
        int playerIndex = player.getIndex();
        float x = this.getX();
        float y = this.getY();
        float z = this.getZ();
        Vector3 v = new Vector3();
        float animalSize = 1.0f;
        Model.boneToWorldCoords(animationPlayer, x, y, z, 1.0f, animationPlayer.getSkinningBoneIndex("Bip01_Head", -1), v);
        return this.getCell().getChunkMap(playerIndex).getGridSquare(PZMath.fastfloor(v.x), PZMath.fastfloor(v.y), PZMath.fastfloor(this.getZ()));
    }

    public boolean couldSeeHeadSquare(IsoPlayer player) {
        int playerIndex = player.getIndex();
        IsoGridSquare headSquare = this.getHeadSquare(player);
        return headSquare != null && headSquare.isCouldSee(playerIndex);
    }

    public boolean canSeeHeadSquare(IsoPlayer player) {
        int playerIndex = player.getIndex();
        IsoGridSquare headSquare = this.getHeadSquare(player);
        return headSquare != null && headSquare.isCanSee(playerIndex);
    }

    private boolean isSideOfStaircaseBetweenSelfAndTarget() {
        if (this.target == null) {
            return false;
        }
        IsoGridSquare currentSquare = this.getCurrentSquare();
        if (currentSquare == null) {
            return false;
        }
        IsoGridSquare targetCurrentSquare = this.target.getCurrentSquare();
        if (targetCurrentSquare == null) {
            return false;
        }
        if ((currentSquare.getX() == targetCurrentSquare.getX() - 1 || currentSquare.getX() == targetCurrentSquare.getX() + 1) && (currentSquare.HasStairsNorth() || targetCurrentSquare.HasStairsNorth())) {
            return true;
        }
        return !(currentSquare.getY() != targetCurrentSquare.getY() - 1 && currentSquare.getY() != targetCurrentSquare.getY() + 1 || !currentSquare.HasStairsWest() && !targetCurrentSquare.HasStairsWest());
    }

    private void updateMovementStatistics() {
        lastPosition.set(this.getLastX(), this.getLastY());
        currentPosition.set(this.getX(), this.getY());
        float distanceTraveled = PZMath.clamp(lastPosition.distanceTo(currentPosition), 0.0f, 0.5f);
        String statisticName = "Zombie's Distance Walked";
        if (this.crawling) {
            statisticName = "Zombie's Distance Crawled";
        } else if (this.running) {
            statisticName = "Zombie's Distance Ran";
        }
        StatisticsManager.getInstance().incrementStatistic(StatisticType.Zombie, StatisticCategory.Travel, statisticName, distanceTraveled);
    }

    private static class Aggro {
        private final IsoMovingObject obj;
        private float damage;
        private long lastDamage;

        public Aggro(IsoMovingObject obj, float damage) {
            this.obj = obj;
            this.damage = damage;
            this.lastDamage = System.currentTimeMillis();
        }

        public void addDamage(float damage) {
            this.damage += damage;
            this.lastDamage = System.currentTimeMillis();
        }

        public float getAggro() {
            float dt = System.currentTimeMillis() - this.lastDamage;
            float aggro = Math.min(1.0f, Math.max(0.0f, (10000.0f - dt) / 5000.0f));
            aggro = Math.min(1.0f, Math.max(0.0f, aggro * this.damage * 0.5f));
            return aggro;
        }
    }

    private static class s_performance {
        private static final PerformanceProfileProbe update = new PerformanceProfileProbe("IsoZombie.update");
        private static final PerformanceProfileProbe postUpdate = new PerformanceProfileProbe("IsoZombie.postUpdate");

        private s_performance() {
        }
    }

    private static final class FloodFill {
        private IsoGridSquare start;
        private static final int FLOOD_SIZE = 11;
        private final BooleanGrid visited = new BooleanGrid(11, 11);
        private final Stack<IsoGridSquare> stack = new Stack();
        private IsoBuilding building;
        private Mover mover;
        private final ArrayList<IsoGridSquare> choices = new ArrayList(121);

        private FloodFill() {
        }

        private void calculate(Mover mv, IsoGridSquare sq) {
            this.start = sq;
            this.mover = mv;
            if (this.start.getRoom() != null) {
                this.building = this.start.getRoom().getBuilding();
            }
            this.push(this.start.getX(), this.start.getY());
            while ((sq = this.pop()) != null) {
                int x = sq.getX();
                int y1 = sq.getY();
                while (this.shouldVisit(x, y1, x, y1 - 1)) {
                    --y1;
                }
                boolean spanRight = false;
                boolean spanLeft = false;
                do {
                    this.visited.setValue(this.gridX(x), this.gridY(y1), true);
                    IsoGridSquare sq2 = IsoWorld.instance.currentCell.getGridSquare(x, y1, this.start.getZ());
                    if (sq2 != null) {
                        this.choices.add(sq2);
                    }
                    if (!spanLeft && this.shouldVisit(x, y1, x - 1, y1)) {
                        this.push(x - 1, y1);
                        spanLeft = true;
                    } else if (spanLeft && !this.shouldVisit(x, y1, x - 1, y1)) {
                        spanLeft = false;
                    } else if (spanLeft && !this.shouldVisit(x - 1, y1, x - 1, y1 - 1)) {
                        this.push(x - 1, y1);
                    }
                    if (!spanRight && this.shouldVisit(x, y1, x + 1, y1)) {
                        this.push(x + 1, y1);
                        spanRight = true;
                        continue;
                    }
                    if (spanRight && !this.shouldVisit(x, y1, x + 1, y1)) {
                        spanRight = false;
                        continue;
                    }
                    if (!spanRight || this.shouldVisit(x + 1, y1, x + 1, y1 - 1)) continue;
                    this.push(x + 1, y1);
                } while (this.shouldVisit(x, ++y1 - 1, x, y1));
            }
        }

        private boolean shouldVisit(int x1, int y1, int x2, int y2) {
            if (this.gridX(x2) >= 11 || this.gridX(x2) < 0) {
                return false;
            }
            if (this.gridY(y2) >= 11 || this.gridY(y2) < 0) {
                return false;
            }
            if (this.visited.getValue(this.gridX(x2), this.gridY(y2))) {
                return false;
            }
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x2, y2, this.start.getZ());
            if (sq == null) {
                return false;
            }
            if (sq.has(IsoObjectType.stairsBN) || sq.has(IsoObjectType.stairsMN) || sq.has(IsoObjectType.stairsTN)) {
                return false;
            }
            if (sq.has(IsoObjectType.stairsBW) || sq.has(IsoObjectType.stairsMW) || sq.has(IsoObjectType.stairsTW)) {
                return false;
            }
            if (sq.getRoom() != null && this.building == null) {
                return false;
            }
            if (sq.getRoom() == null && this.building != null) {
                return false;
            }
            return !IsoWorld.instance.currentCell.blocked(this.mover, x2, y2, this.start.getZ(), x1, y1, this.start.getZ());
        }

        private void push(int x, int y) {
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, this.start.getZ());
            this.stack.push(sq);
        }

        private IsoGridSquare pop() {
            return this.stack.isEmpty() ? null : this.stack.pop();
        }

        private int gridX(int x) {
            return x - (this.start.getX() - 5);
        }

        private int gridY(int y) {
            return y - (this.start.getY() - 5);
        }

        private int gridX(IsoGridSquare sq) {
            return sq.getX() - (this.start.getX() - 5);
        }

        private int gridY(IsoGridSquare sq) {
            return sq.getY() - (this.start.getY() - 5);
        }

        private IsoGridSquare choose() {
            if (this.choices.isEmpty()) {
                return null;
            }
            int n = Rand.Next(this.choices.size());
            return this.choices.get(n);
        }

        private void reset() {
            this.building = null;
            this.choices.clear();
            this.stack.clear();
            this.visited.clear();
        }
    }

    public static enum ZombieSound {
        Burned(10),
        DeadCloseKilled(10),
        DeadNotCloseKilled(10),
        Hurt(10),
        Idle(15),
        Lunge(40),
        MAX(-1);

        private final int radius;
        private static final ZombieSound[] values;

        private ZombieSound(int radius) {
            this.radius = radius;
        }

        public int radius() {
            return this.radius;
        }

        public static ZombieSound fromIndex(int index) {
            return index >= 0 && index < values.length ? values[index] : MAX;
        }

        static {
            values = ZombieSound.values();
        }
    }
}

