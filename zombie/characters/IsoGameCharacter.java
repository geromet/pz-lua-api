/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import fmod.fmod.IFMODParameterUpdater;
import gnu.trove.map.hash.THashMap;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Stack;
import org.joml.GeometryUtils;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.util.vector.Quaternion;
import se.krka.kahlua.vm.KahluaTable;
import zombie.AmbientStreamManager;
import zombie.CombatManager;
import zombie.DebugFileWatcher;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaHookManager;
import zombie.Lua.LuaManager;
import zombie.PersistentOutfits;
import zombie.PredicatedFileWatcher;
import zombie.SandboxOptions;
import zombie.SystemDisabler;
import zombie.UpdateSchedulerSimulationLevel;
import zombie.UsedFromLua;
import zombie.VirtualZombieManager;
import zombie.WorldSoundManager;
import zombie.ZomboidFileSystem;
import zombie.ZomboidGlobals;
import zombie.ai.GameCharacterAIBrain;
import zombie.ai.IStateCharacter;
import zombie.ai.MapKnowledge;
import zombie.ai.State;
import zombie.ai.StateMachine;
import zombie.ai.astar.AStarPathFinder;
import zombie.ai.astar.AStarPathFinderResult;
import zombie.ai.sadisticAIDirector.SleepingEventData;
import zombie.ai.states.AttackNetworkState;
import zombie.ai.states.AttackState;
import zombie.ai.states.BumpedState;
import zombie.ai.states.ClimbDownSheetRopeState;
import zombie.ai.states.ClimbOverFenceState;
import zombie.ai.states.ClimbOverWallState;
import zombie.ai.states.ClimbSheetRopeState;
import zombie.ai.states.ClimbThroughWindowPositioningParams;
import zombie.ai.states.ClimbThroughWindowState;
import zombie.ai.states.CloseWindowState;
import zombie.ai.states.CollideWithWallState;
import zombie.ai.states.FakeDeadZombieState;
import zombie.ai.states.FishingState;
import zombie.ai.states.GrappledThrownIntoContainerState;
import zombie.ai.states.GrappledThrownOutWindowState;
import zombie.ai.states.GrappledThrownOverFenceState;
import zombie.ai.states.IdleState;
import zombie.ai.states.LungeNetworkState;
import zombie.ai.states.LungeState;
import zombie.ai.states.OpenWindowState;
import zombie.ai.states.PathFindState;
import zombie.ai.states.PlayerDraggingCorpse;
import zombie.ai.states.PlayerEmoteState;
import zombie.ai.states.PlayerGetUpState;
import zombie.ai.states.PlayerHitReactionPVPState;
import zombie.ai.states.PlayerHitReactionState;
import zombie.ai.states.SmashWindowState;
import zombie.ai.states.StaggerBackState;
import zombie.ai.states.StateManager;
import zombie.ai.states.SwipeStatePlayer;
import zombie.ai.states.ThumpState;
import zombie.ai.states.WalkTowardState;
import zombie.ai.states.ZombieFallDownState;
import zombie.ai.states.ZombieFallingState;
import zombie.ai.states.ZombieHitReactionState;
import zombie.ai.states.ZombieOnGroundState;
import zombie.ai.states.animals.AnimalIdleState;
import zombie.ai.states.animals.AnimalWalkState;
import zombie.audio.FMODParameter;
import zombie.audio.FMODParameterList;
import zombie.audio.GameSoundClip;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.BloodClothingType;
import zombie.characters.AnimStateTriggerXmlFile;
import zombie.characters.AttachedItems.AttachedItems;
import zombie.characters.AttachedItems.AttachedLocationGroup;
import zombie.characters.AttachedItems.AttachedLocations;
import zombie.characters.BaseCharacterSoundEmitter;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartLast;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.BodyDamage.Metabolics;
import zombie.characters.BodyDamage.Nutrition;
import zombie.characters.Capability;
import zombie.characters.CharacterSoundEmitter;
import zombie.characters.CharacterStat;
import zombie.characters.CharacterTimedActions.BaseAction;
import zombie.characters.CharacterTimedActions.LuaTimedActionNew;
import zombie.characters.CheatType;
import zombie.characters.ClothingWetness;
import zombie.characters.ClothingWetnessSync;
import zombie.characters.DummyCharacterSoundEmitter;
import zombie.characters.Faction;
import zombie.characters.FallDamage;
import zombie.characters.FallSeverity;
import zombie.characters.FallingConstants;
import zombie.characters.FallingWhileInjured;
import zombie.characters.HaloTextHelper;
import zombie.characters.HitReactionNetworkAI;
import zombie.characters.ILuaGameCharacter;
import zombie.characters.ILuaVariableSource;
import zombie.characters.IsoAIModule;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.Moodles.Moodles;
import zombie.characters.MoveDeltaModifiers;
import zombie.characters.NetworkCharacter;
import zombie.characters.NetworkCharacterAI;
import zombie.characters.PlayerCheats;
import zombie.characters.Role;
import zombie.characters.Safety;
import zombie.characters.Side;
import zombie.characters.Stats;
import zombie.characters.SurvivorDesc;
import zombie.characters.SurvivorFactory;
import zombie.characters.Talker;
import zombie.characters.TriggerXmlFile;
import zombie.characters.VisibilityData;
import zombie.characters.WornItems.BodyLocationGroup;
import zombie.characters.WornItems.BodyLocations;
import zombie.characters.WornItems.WornItem;
import zombie.characters.WornItems.WornItems;
import zombie.characters.ZombiesZoneDefinition;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionState;
import zombie.characters.action.ActionStateSnapshot;
import zombie.characters.action.IActionStateChanged;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.professions.CharacterProfessionDefinition;
import zombie.characters.skills.PerkFactory;
import zombie.characters.traits.CharacterTraitDefinition;
import zombie.characters.traits.CharacterTraits;
import zombie.chat.ChatElement;
import zombie.chat.ChatElementOwner;
import zombie.chat.ChatManager;
import zombie.chat.ChatMessage;
import zombie.combat.CombatConfigKey;
import zombie.core.BoxedStaticValues;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.RenderSettings;
import zombie.core.opengl.Shader;
import zombie.core.physics.BallisticsController;
import zombie.core.physics.BallisticsTarget;
import zombie.core.physics.RagdollController;
import zombie.core.physics.RagdollStateData;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.properties.IsoObjectChange;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.BaseGrappleable;
import zombie.core.skinnedmodel.HelperFunctions;
import zombie.core.skinnedmodel.IGrappleable;
import zombie.core.skinnedmodel.IGrappleableWrapper;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AdvancedAnimator;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.AnimNode;
import zombie.core.skinnedmodel.advancedanimation.AnimState;
import zombie.core.skinnedmodel.advancedanimation.AnimationSet;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandle;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandlePool;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandles;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableReference;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSource;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableType;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableMap;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableRegistry;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.core.skinnedmodel.advancedanimation.LiveAnimNode;
import zombie.core.skinnedmodel.advancedanimation.debug.AnimatorDebugMonitor;
import zombie.core.skinnedmodel.advancedanimation.events.AnimEventBroadcaster;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventCallback;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventWrappedBroadcaster;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.AnimationMultiTrack;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.AnimatorsBoneTransform;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.model.ModelInstanceTextureCreator;
import zombie.core.skinnedmodel.model.SkeletonBone;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.population.BeardStyle;
import zombie.core.skinnedmodel.population.BeardStyles;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.ClothingItemReference;
import zombie.core.skinnedmodel.population.HairStyle;
import zombie.core.skinnedmodel.population.HairStyles;
import zombie.core.skinnedmodel.population.IClothingItemListener;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.ColorInfo;
import zombie.core.utils.UpdateLimit;
import zombie.core.znet.SteamGameServer;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.debug.LogSeverity;
import zombie.entity.ComponentType;
import zombie.entity.components.fluids.FluidCategory;
import zombie.entity.components.fluids.FluidConsume;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidType;
import zombie.gameStates.IngameState;
import zombie.input.AimingMode;
import zombie.input.Mouse;
import zombie.interfaces.IUpdater;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.AnimalInventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Literature;
import zombie.inventory.types.MapItem;
import zombie.inventory.types.Radio;
import zombie.inventory.types.WeaponType;
import zombie.iso.BentFences;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoPhysicsObject;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoRoofFixer;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.LosUtil;
import zombie.iso.RoomDef;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.Vector2;
import zombie.iso.Vector2ObjectPool;
import zombie.iso.Vector3;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.areas.SafeHouse;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderShadows;
import zombie.iso.objects.IsoBall;
import zombie.iso.objects.IsoBulletTracerEffects;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoFallingClothing;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.IsoMolotovCocktail;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.objects.IsoZombieGiblets;
import zombie.iso.objects.RainManager;
import zombie.iso.objects.ShadowParams;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.NetworkVariables;
import zombie.network.PVPLogTool;
import zombie.network.PacketTypes;
import zombie.network.ServerGUI;
import zombie.network.ServerLOS;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.network.ServerWorldDatabase;
import zombie.network.anticheats.AntiCheatXPUpdate;
import zombie.network.chat.ChatServer;
import zombie.network.chat.ChatType;
import zombie.network.fields.hit.AttackVars;
import zombie.network.fields.hit.HitInfo;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.SyncPlayerStatsPacket;
import zombie.network.packets.VariableSyncPacket;
import zombie.pathfind.Path;
import zombie.pathfind.PathFindBehavior2;
import zombie.pathfind.PolygonalMap2;
import zombie.popman.ObjectPool;
import zombie.profanity.ProfanityFilter;
import zombie.radio.ZomboidRadio;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ItemKey;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.MoodleType;
import zombie.scripting.objects.Recipe;
import zombie.scripting.objects.ResourceLocation;
import zombie.scripting.objects.VehicleScript;
import zombie.scripting.objects.WeaponCategory;
import zombie.statistics.StatisticCategory;
import zombie.statistics.StatisticType;
import zombie.statistics.StatisticsManager;
import zombie.ui.TextDrawObject;
import zombie.ui.TextManager;
import zombie.ui.TutorialManager;
import zombie.ui.UIFont;
import zombie.ui.UIManager;
import zombie.util.AutoCloseablePool;
import zombie.util.FrameDelay;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.lambda.Invokers;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleLight;
import zombie.vehicles.VehiclePart;

@UsedFromLua
public abstract class IsoGameCharacter
extends IsoMovingObject
implements Talker,
ChatElementOwner,
IAnimatable,
IAnimationVariableMap,
IAnimationVariableRegistry,
IClothingItemListener,
IActionStateChanged,
IAnimEventCallback,
IAnimEventWrappedBroadcaster,
IFMODParameterUpdater,
IGrappleableWrapper,
ILuaVariableSource,
ILuaGameCharacter,
IStateCharacter {
    private static final int CorpseBodyWeight = 20;
    private static final float BaseMuscleStrainMultiplier = 2.5f;
    private static final float ZombieAttackingClimbPenalty = 25.0f;
    private static final float ZombieNearbyClimbPenalty = 7.0f;
    public static final int GlovesStrengthBonus = 1;
    public static final int AwkwardGlovesStrengthDivisor = 2;
    private static final ItemVisuals tempItemVisuals = new ItemVisuals();
    public IsoAIModule ai = new IsoAIModule(this);
    private final float extraLungeRange = 0.2f;
    private boolean ignoreAimingInput;
    private boolean headLookAround;
    private static final float maxHeadLookAngle = 0.348f;
    private float headLookHorizontal;
    private float headLookVertical;
    private boolean doDeathSound = true;
    private boolean canShout = true;
    public boolean doDirtBloodEtc = true;
    private static int instanceId;
    public static final int RENDER_OFFSET_X = 1;
    public static final int RENDER_OFFSET_Y = -89;
    public static final float s_maxPossibleTwist = 70.0f;
    private static final Bandages s_bandages;
    private static final HashMap<Integer, SurvivorDesc> SurvivorMap;
    private static final int[] LevelUpLevels;
    protected static final Vector2 tempo;
    protected static final Vector3 tempo3;
    protected static final ColorInfo inf;
    public long vocalEvent;
    public long removedFromWorldMs;
    private boolean isAddedToModelManager;
    private boolean autoWalk;
    private final Vector2 autoWalkDirection = new Vector2();
    private boolean sneaking;
    private float sneakLimpSpeedScale = 1.0f;
    private static final float m_sneakLimpSpeed = 0.6f;
    private static final float m_sneakLowLimpSpeed = 0.45f;
    protected static final Vector2 tempo2;
    private static final Vector2 tempVector2_1;
    private static final Vector2 tempVector2_2;
    private static String sleepText;
    protected final ArrayList<InventoryItem> savedInventoryItems = new ArrayList();
    private final String instancename;
    public final ArrayList<String> amputations = new ArrayList();
    public ModelInstance hair;
    public ModelInstance beard;
    public ModelInstance primaryHandModel;
    public ModelInstance secondaryHandModel;
    private final ActionContext actionContext = new ActionContext(this);
    public final BaseCharacterSoundEmitter emitter;
    private final FMODParameterList fmodParameters = new FMODParameterList();
    private final AnimationVariableSource gameVariables = new AnimationVariableSource();
    private AnimationVariableSource playbackGameVariables;
    private boolean running;
    private boolean sprinting;
    private boolean avoidDamage;
    public boolean callOut;
    public IsoGameCharacter reanimatedCorpse;
    public int reanimatedCorpseId = -1;
    private AnimationPlayer animPlayer;
    private boolean deferredMovementEnabled = true;
    public final AdvancedAnimator advancedAnimator;
    public final Map<Class<?>, Map<State.Param<?>, Object>> stateMachineParams = new IdentityHashMap();
    private boolean isCrit;
    private boolean knockedDown;
    public int bumpNbr;
    private final ArrayList<PerkInfo> perkList = new ArrayList();
    protected final Vector2 forwardDirection = new Vector2();
    private float targetVerticalAimAngleDegrees;
    private float currentVerticalAimAngleDegrees;
    public boolean asleep;
    public boolean isResting;
    public boolean blockTurning;
    public float speedMod = 1.0f;
    public IsoSprite legsSprite;
    private boolean female = true;
    public float knockbackAttackMod = 1.0f;
    private boolean animal;
    public final boolean[] isVisibleToPlayer = new boolean[4];
    public float savedVehicleX;
    public float savedVehicleY;
    public short savedVehicleSeat = (short)-1;
    public boolean savedVehicleRunning;
    private static final float RecoilDelayDecrease = 0.625f;
    protected static final float BeenMovingForIncrease = 1.25f;
    protected static final float BeenMovingForDecrease = 0.625f;
    private IsoGameCharacter followingTarget;
    private final ArrayList<IsoMovingObject> localList = new ArrayList();
    private final ArrayList<IsoMovingObject> localNeutralList = new ArrayList();
    private final ArrayList<IsoMovingObject> localGroupList = new ArrayList();
    private final ArrayList<IsoMovingObject> localRelevantEnemyList = new ArrayList();
    private float dangerLevels;
    private static final Vector2 tempVector2;
    private float leaveBodyTimedown;
    protected boolean allowConversation = true;
    private float reanimateTimer;
    private int reanimAnimFrame;
    private int reanimAnimDelay;
    private boolean reanim;
    private boolean visibleToNpcs = true;
    private int dieCount;
    private float llx;
    private float lly;
    private float llz;
    protected int remoteId = -1;
    protected int numSurvivorsInVicinity;
    private float levelUpMultiplier = 2.5f;
    protected XP xp;
    private int lastLocalEnemies;
    private final ArrayList<IsoMovingObject> veryCloseEnemyList = new ArrayList();
    private final HashMap<String, Location> lastKnownLocation = new HashMap();
    protected IsoGameCharacter attackedBy;
    protected boolean damagedByVehicle;
    protected boolean ignoreStaggerBack;
    private int timeThumping;
    private int patienceMax = 150;
    private int patienceMin = 20;
    private int patience = 20;
    protected final Stack<BaseAction> characterActions = new Stack();
    private int zombieKills;
    private int survivorKills;
    private int lastZombieKills;
    protected float forceWakeUpTime = -1.0f;
    private float fullSpeedMod = 1.0f;
    protected float runSpeedModifier = 1.0f;
    private float walkSpeedModifier = 1.0f;
    private float combatSpeedModifier = 1.0f;
    private float clothingDiscomfortModifier;
    private boolean rangedWeaponEmpty;
    public final ArrayList<InventoryContainer> bagsWorn = new ArrayList();
    protected boolean forceWakeUp;
    protected final BodyDamage bodyDamage;
    private BodyDamage bodyDamageRemote;
    private State defaultState;
    protected WornItems wornItems;
    protected AttachedItems attachedItems;
    protected ClothingWetness clothingWetness;
    protected ClothingWetnessSync clothingWetnessSync;
    protected SurvivorDesc descriptor;
    private final Stack<IsoBuilding> familiarBuildings = new Stack();
    protected final AStarPathFinderResult finder = new AStarPathFinderResult();
    private float fireKillRate = 0.0038f;
    private int fireSpreadProbability = 6;
    protected float health = 1.0f;
    protected boolean dead;
    protected boolean kill;
    private boolean wornClothingCanRagdoll = true;
    private boolean isEditingRagdoll;
    private boolean ragdollFall;
    private boolean vehicleCollision;
    protected boolean playingDeathSound;
    private boolean deathDragDown;
    protected String hurtSound = "MaleZombieHurt";
    protected ItemContainer inventory = new ItemContainer();
    protected InventoryItem leftHandItem;
    protected boolean handItemShouldSendToClients;
    private int nextWander = 200;
    private boolean onFire;
    private int pathIndex;
    protected InventoryItem rightHandItem;
    protected Color speakColour = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    protected float slowFactor;
    protected float slowTimer;
    protected boolean useParts;
    protected boolean speaking;
    private float speakTime;
    private float staggerTimeMod = 1.0f;
    protected final StateMachine stateMachine;
    protected final Moodles moodles;
    protected final Stats stats = new Stats();
    private final Stack<String> usedItemsOn = new Stack();
    protected HandWeapon useHandWeapon;
    protected IsoGridSquare attackTargetSquare;
    private float bloodImpactX;
    private float bloodImpactY;
    private float bloodImpactZ;
    private IsoSprite bloodSplat;
    private boolean onBed;
    private final Vector2 moveForwardVec = new Vector2();
    protected boolean pathing;
    protected ChatElement chatElement;
    private final Stack<IsoGameCharacter> localEnemyList = new Stack();
    protected final Stack<IsoGameCharacter> enemyList = new Stack();
    protected final CharacterTraits characterTraits = new CharacterTraits();
    private int maxWeight = 8;
    private int maxWeightBase = 8;
    private float sleepingTabletEffect;
    private float sleepingTabletDelta = 1.0f;
    private float betaEffect;
    private float betaDelta;
    private float depressEffect;
    private float depressDelta;
    private float depressFirstTakeTime = -1.0f;
    private float painEffect;
    private float painDelta;
    private boolean doDefer = true;
    private float haloDispTime = 128.0f;
    protected TextDrawObject userName;
    private TextDrawObject haloNote;
    private static final String nameCarKeySuffix = " [img=media/ui/CarKey.png";
    private static final String voiceSuffix = "[img=media/ui/voiceon.png] ";
    private static final String voiceMuteSuffix = "[img=media/ui/voicemuted.png] ";
    protected IsoPlayer isoPlayer;
    private boolean hasInitTextObjects;
    private boolean canSeeCurrent;
    private boolean drawUserName;
    private final Location lastHeardSound = new Location(-1, -1, -1);
    protected boolean climbing;
    private boolean lastCollidedW;
    private boolean lastCollidedN;
    protected float fallTime;
    protected float lastFallSpeed;
    protected boolean falling;
    protected boolean isOnGround = true;
    protected BaseVehicle vehicle;
    boolean isNpc;
    private long lastBump;
    private IsoGameCharacter bumpedChr;
    private int age = 25;
    private int lastHitCount;
    private final Safety safety = new Safety(this);
    private float meleeDelay;
    private float recoilDelay;
    private float beenMovingFor;
    private float beenSprintingFor;
    private float aimingDelay;
    private String clickSound;
    private float reduceInfectionPower;
    private final List<String> knownRecipes = new ArrayList<String>();
    private final HashSet<String> knownMediaLines = new HashSet();
    private int lastHourSleeped;
    protected float timeOfSleep;
    protected float delayToActuallySleep;
    private String bedType = "averageBed";
    private IsoObject bed;
    private boolean isReading;
    private float timeSinceLastSmoke;
    private ChatMessage lastChatMessage;
    private String lastSpokenLine;
    public PlayerCheats cheats = new PlayerCheats();
    private boolean showAdminTag = true;
    private long isAnimForecasted;
    private boolean fallOnFront;
    private boolean killedByFall;
    private boolean hitFromBehind;
    private String hitReaction = "";
    private String bumpType = "";
    private boolean isBumpDone;
    private boolean bumpFall;
    private boolean bumpStaggered;
    private String bumpFallType = "";
    private boolean animationFinishing;
    private int sleepSpeechCnt;
    private Radio equipedRadio;
    private InventoryItem leftHandCache;
    private InventoryItem rightHandCache;
    private InventoryItem backCache;
    private final ArrayList<ReadBook> readBooks = new ArrayList();
    public final LightInfo lightInfo = new LightInfo();
    private final LightInfo lightInfo2 = new LightInfo();
    private Path path2;
    private final MapKnowledge mapKnowledge = new MapKnowledge();
    protected final AttackVars attackVars = new AttackVars();
    private final PZArrayList<HitInfo> hitInfoList = new PZArrayList<HitInfo>(HitInfo.class, 8);
    private final PathFindBehavior2 pfb2 = new PathFindBehavior2(this);
    private final InventoryItem[] cacheEquiped = new InventoryItem[2];
    private boolean aimAtFloor;
    private float aimAtFloorTargetDistance;
    protected int persistentOutfitId = 0;
    protected boolean persistentOutfitInit;
    private boolean updateModelTextures;
    private ModelInstanceTextureCreator textureCreator;
    public boolean updateEquippedTextures;
    private final ArrayList<ModelInstance> readyModelData = new ArrayList();
    private boolean isSitOnFurniture;
    private IsoObject sitOnFurnitureObject;
    private IsoDirections sitOnFurnitureDirection;
    private boolean sitOnGround;
    private boolean ignoreMovement;
    private boolean hideWeaponModel;
    private boolean hideEquippedHandL;
    private boolean hideEquippedHandR;
    private boolean isAiming;
    private float beardGrowTiming = -1.0f;
    private float hairGrowTiming = -1.0f;
    private float moveDelta = 1.0f;
    protected float turnDeltaNormal = 1.0f;
    protected float turnDeltaRunning = 0.8f;
    protected float turnDeltaSprinting = 0.75f;
    private float maxTwist = 15.0f;
    private boolean isMoving;
    private boolean isTurning;
    private boolean isTurningAround;
    private float initialTurningAroundTarget;
    private boolean isTurning90;
    private boolean invincible;
    private float lungeFallTimer;
    private SleepingEventData sleepingEventData;
    private static final int HAIR_GROW_TIME_DAYS = 20;
    private static final int BEARD_GROW_TIME_DAYS = 5;
    public float realx;
    public float realy;
    public byte realz;
    public NetworkVariables.ZombieState realState = NetworkVariables.ZombieState.Idle;
    public String overridePrimaryHandModel;
    public String overrideSecondaryHandModel;
    public boolean forceNullOverride;
    protected final UpdateLimit ulBeatenVehicle = new UpdateLimit(200L);
    private float momentumScalar;
    private final HashMap<String, State> aiStateMap = new HashMap();
    private boolean isPerformingAttackAnim;
    private boolean isPerformingShoveAnim;
    private boolean isPerformingStompAnim;
    private float wornItemsVisionModifier = 1.0f;
    private float wornItemsHearingModifier = 1.0f;
    private float corpseSicknessRate;
    private float blurFactor;
    private float blurFactorTarget;
    public boolean usernameDisguised;
    private float climbRopeTime;
    @Deprecated
    public ArrayList<Integer> invRadioFreq = new ArrayList();
    private final PredicatedFileWatcher animStateTriggerWatcher;
    private boolean debugVariablesRegistered;
    private float effectiveEdibleBuffTimer;
    private final HashMap<String, Integer> readLiterature = new HashMap();
    private final HashSet<String> readPrintMedia = new HashSet();
    private IsoGameCharacter lastHitCharacter;
    private BallisticsController ballisticsController;
    private BallisticsTarget ballisticsTarget;
    private final BaseGrappleable grappleable;
    private boolean isAnimatingBackwards;
    private float animationTimeScale = 1.0f;
    private boolean animationUpdatingThisFrame = true;
    private final FrameDelay animationInvisibleFrameDelay = new FrameDelay(4);
    public long lastAnimalPet;
    private final AnimEventBroadcaster animEventBroadcaster = new AnimEventBroadcaster();
    public IsoGameCharacter vbdebugHitTarget;
    private String hitDirEnum = "FRONT";
    private boolean isGrappleThrowOutWindow;
    private boolean isGrappleThrowOverFence;
    private boolean isGrappleThrowIntoContainer;
    private boolean shoveStompAnim;
    private static final float maxStrafeSpeed = 0.48f;
    private static final Vector3f tempVector3f00;
    private static final Vector3f tempVector3f01;
    private static final float CombatSpeedBase = 0.8f;
    private static final float HeavyTwoHandedWeaponModifier = 1.2f;
    private float idleSquareTime;
    private final List<String> concurrentActionList = new ArrayList<String>(List.of("OpenDoor", "CloseDoor", "ClimbThroughWindow", "ClimbOverFence", "OpenHutch", "CloseCurtain", "OpenCurtain", "CloseWindow", "OpenWindow"));
    private float shadowFm;
    private float shadowBm;
    private long shadowTick = -1L;
    private float lastFitnessValue = CharacterStat.FITNESS.getDefaultValue();
    public final NetworkCharacter networkCharacter = new NetworkCharacter();
    private AimingMode aimingMode = AimingMode.REGULAR;
    private final Recoil recoil = new Recoil();
    private static final double meleeWeaponMuscleStrainAdjustment = 0.65;
    private boolean usePhysicHitReaction;
    private ClimbSheetRopeState.ClimbData climbData;
    private final FallDamage fallDamage = new FallDamage();
    private static final AnimEvent s_turn180StartedEvent;
    private static final AnimEvent s_turn180TargetChangedEvent;
    private final LC_slideAwayFromWalls slideAwayFromWalls = new LC_slideAwayFromWalls();
    private static final ArrayList<IsoMovingObject> movingStatic;
    final PerformanceProfileProbe postUpdateInternal = new PerformanceProfileProbe("IsoGameCharacter.postUpdate");
    final PerformanceProfileProbe updateInternal = new PerformanceProfileProbe("IsoGameCharacter.update");
    private static final Vector3 tempVectorBonePos;

    public IsoGameCharacter(IsoCell cell, float x, float y, float z) {
        super(null, false);
        this.grappleable = new BaseGrappleable(this);
        this.getWrappedGrappleable().setOnGrappledBeginCallback(this::onGrappleBegin);
        this.getWrappedGrappleable().setOnGrappledEndCallback(this::onGrappleEnded);
        if (!GameServer.server || !(this instanceof IsoZombie)) {
            this.registerVariableCallbacks();
            this.registerAnimEventCallbacks();
        }
        this.instancename = this.getClass().getSimpleName() + instanceId;
        ++instanceId;
        this.emitter = !(this instanceof IsoSurvivor) ? (Core.soundDisabled || GameServer.server ? new DummyCharacterSoundEmitter(this) : new CharacterSoundEmitter(this)) : null;
        if (x != 0.0f || y != 0.0f || z != 0.0f) {
            if (this.getCell().isSafeToAdd()) {
                this.getCell().getObjectList().add(this);
            } else {
                this.getCell().getAddList().add(this);
            }
        }
        if (this.def == null) {
            this.def = IsoSpriteInstance.get(this.sprite);
        }
        if (this instanceof IsoPlayer && !(this instanceof IsoAnimal)) {
            this.bodyDamage = new BodyDamage(this);
            this.moodles = new Moodles(this);
            this.xp = new XP(this, this);
        } else {
            this.bodyDamage = this instanceof IsoAnimal ? new BodyDamage(this) : null;
            this.moodles = null;
            this.xp = null;
        }
        this.patience = Rand.Next(this.patienceMin, this.patienceMax);
        this.setX(x + 0.5f);
        this.setY(y + 0.5f);
        this.setZ(z);
        this.setScriptNextX(this.setLastX(this.setNextX(x)));
        this.setScriptNextY(this.setLastY(this.setNextY(y)));
        if (cell != null) {
            this.current = this.getCell().getGridSquare(PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z));
        }
        this.offsetY = 0.0f;
        this.offsetX = 0.0f;
        this.stateMachine = new StateMachine(this);
        this.setDefaultState(IdleState.instance());
        this.inventory.parent = this;
        this.inventory.setExplored(true);
        this.chatElement = new ChatElement(this, 1, "character");
        this.advancedAnimator = new AdvancedAnimator();
        this.advancedAnimator.init(this);
        this.advancedAnimator.animCallbackHandlers.add(this);
        this.advancedAnimator.setAnimSet(AnimationSet.GetAnimationSet(this.GetAnimSetName(), false));
        this.actionContext.onStateChanged.add(this);
        this.animStateTriggerWatcher = new PredicatedFileWatcher(ZomboidFileSystem.instance.getMessagingDirSub("Trigger_SetAnimState.xml"), AnimStateTriggerXmlFile.class, this::onTrigger_setAnimStateToTriggerFile);
    }

    private void registerVariableCallbacks() {
        this.setVariable("isRendered", true, this::getDoRender, this::setDoRender, (IAnimationVariableSource owner) -> "If this character is allowed to be drawn to screen. Typically a corpse being dragged out of a container is not drawn until it has exited it.");
        this.setVariable("stateeventdelaytimer", this::getStateEventDelayTimer, (IAnimationVariableSource owner) -> "Generic State Event timer countdown, in frames at 60fps. Typically started by an ai.State that expects to do something for this amount of game frames.");
        this.setVariable("hashitreaction", this::hasHitReaction, (IAnimationVariableSource owner) -> "If this character has been attacked, the type of reaction to the hit is stored here.");
        this.setVariable("hitreaction", this::getHitReaction, this::setHitReaction, (IAnimationVariableSource owner) -> "Does this character have a hit reaction. If they have not been attacked and hit, this will return FALSE.");
        this.setVariable("collidetype", this::getCollideType, this::setCollideType, (IAnimationVariableSource owner) -> "The character has collided with something. This is the type of collision. The only known value is 'wall'.");
        this.setVariable("footInjuryType", this::getFootInjuryType, (IAnimationVariableSource owner) -> "The type of unhealed foot injury this character currently has, if any.<br />Known values:<br /> leftlight,<br /> rightlight,<br /> leftheavy,<br /> rightheavy");
        this.setVariable("bumptype", this::getBumpType, this::setBumpType, (IAnimationVariableSource owner) -> "If the character has been bumped, this is the kind of bump they've sustained.<br />Known values:<br /> stagger,<br /> left,<br /> trippingFromSprint");
        this.setVariable("onbed", this::isOnBed, this::setOnBed, (IAnimationVariableSource owner) -> "Is this character lying on a bed.");
        this.setVariable("sittingonfurniture", this::isSittingOnFurniture, this::setSittingOnFurniture, (IAnimationVariableSource owner) -> "Is this character sitting on furniture. Eg a chair.");
        this.setVariable("sitonground", this::isSitOnGround, this::setSitOnGround, (IAnimationVariableSource owner) -> "Is this character sitting on the ground.");
        this.setVariable("canclimbdownrope", this::canClimbDownSheetRopeInCurrentSquare, (IAnimationVariableSource owner) -> "Can the character climb down a sheet rope, from where they are currently standing.");
        this.setVariable("frombehind", this::isHitFromBehind, this::setHitFromBehind, (IAnimationVariableSource owner) -> "Has this character been attacked and hit from behind.");
        this.setVariable("fallonfront", this::isFallOnFront, this::setFallOnFront, (IAnimationVariableSource owner) -> "This character is falling- or has fallen down on their belly.");
        this.setVariable("killedbyfall", this::isKilledByFall, this::setKilledByFall, (IAnimationVariableSource owner) -> "Has this character been killed by a fall, caused by impact to the ground.");
        this.setVariable("intrees", this::isInTreesNoBush, (IAnimationVariableSource owner) -> "Is this character in trees, but not in a bush.");
        this.setVariable("bumped", this::isBumped, (IAnimationVariableSource owner) -> "This character has been bumped.");
        this.setVariable("BumpDone", false, this::isBumpDone, this::setBumpDone, (IAnimationVariableSource owner) -> "This character has finished reacting to getting bumped.");
        this.setVariable("BumpFall", false, this::isBumpFall, this::setBumpFall, (IAnimationVariableSource owner) -> "This character has been bumped, hard enough to fall.");
        this.setVariable("BumpFallType", "", this::getBumpFallType, this::setBumpFallType, (IAnimationVariableSource owner) -> "If this character has been numped hard enough to fall down, this is the type of fall.<br />Known values:<br /> pushedBehind,<br /> pushedFront");
        this.setVariable("BumpStaggered", false, this::isBumpStaggered, this::setBumpStaggered, (IAnimationVariableSource owner) -> "This character has been bumped, hard enough to be staggered.");
        this.setVariable("bonfloor", this::isOnFloor, this::setOnFloor, (IAnimationVariableSource owner) -> "This character is lying on the floor.");
        this.setVariable("isProne", this::isProne, (IAnimationVariableSource owner) -> "This character is currently prone, for the purposes of targeting and selection of appropriate attack. Determined by their internal logic. Usually, but not always, isProne = bOnFloor");
        this.setVariable("isGettingUp", this::isGettingUp, (IAnimationVariableSource owner) -> "This character is currently getting up from a prone position.");
        this.setVariable("rangedweaponempty", this::isRangedWeaponEmpty, this::setRangedWeaponEmpty, (IAnimationVariableSource owner) -> "This character's equipped weapon is out of ammo.");
        this.setVariable("footInjury", this::hasFootInjury, (IAnimationVariableSource owner) -> "Does this character have an unhealed injury to their foot.");
        this.setVariable("ChopTreeSpeed", 1.0f, this::getChopTreeSpeed, (IAnimationVariableSource owner) -> "The speed of chopping trees. Multiplier: 0.0 - 1.0");
        this.setVariable("MoveDelta", 1.0f, this::getMoveDelta, this::setMoveDelta, (IAnimationVariableSource owner) -> "The movement speed. Multiplier: 0.0 - 1.0");
        this.setVariable("TurnDelta", 1.0f, this::getTurnDelta, this::setTurnDelta, (IAnimationVariableSource owner) -> "The turn speed. Multiplier: 0.0 - 1.0");
        this.setVariable("angle", this::getDirectionAngle, this::setDirectionAngle, (IAnimationVariableSource owner) -> "The character's current direction of travel. In degrees: -180 to 180.");
        this.setVariable("animAngle", this::getAnimAngle, (IAnimationVariableSource owner) -> "The character's current direction of travel, as is tweened by its AnimationPlayer. In degrees: -180 to 180.");
        this.setVariable("twist", this::getTwist, (IAnimationVariableSource owner) -> "The character's current twist about the waist. In degrees.");
        this.setVariable("targetTwist", this::getTargetTwist, (IAnimationVariableSource owner) -> "The character's target twist about the waist. In degrees. The character tries to twist this far but may or may not achieve it.");
        this.setVariable("maxTwist", this.maxTwist, this::getMaxTwist, this::setMaxTwist, (IAnimationVariableSource owner) -> "The character's maximum twist about the waist. In degrees. The character will not twist further than this limit.");
        this.setVariable("shoulderTwist", this::getShoulderTwist, (IAnimationVariableSource owner) -> "The character's twist about the shoulders. In degrees.");
        this.setVariable("excessTwist", this::getExcessTwist, (IAnimationVariableSource owner) -> "The character's excess twist. This is the difference between maxTwist and twist.");
        this.setVariable("numTwistBones", this::getNumTwistBones, (IAnimationVariableSource owner) -> "The number of bones the character uses to twist about their waist.");
        this.setVariable("angleStepDelta", this::getAnimAngleStepDelta, (IAnimationVariableSource owner) -> "The character's rate of turn per frame.");
        this.setVariable("angleTwistDelta", this::getAnimAngleTwistDelta, (IAnimationVariableSource owner) -> "The character's rate of twist per frame.");
        this.setVariable("isTurning", false, this::isTurning, this::setTurning, (IAnimationVariableSource owner) -> "The character is currently turning.");
        this.setVariable("isTurning90", false, this::isTurning90, this::setTurning90, (IAnimationVariableSource owner) -> "The character is turning by a right angle.");
        this.setVariable("isTurningAround", false, this::isTurningAround, this::setTurningAround, (IAnimationVariableSource owner) -> "The character is turning around, by more than 90 degrees.");
        this.setVariable("bMoving", false, this::isMoving, this::setMoving, (IAnimationVariableSource owner) -> "The character is moving.");
        this.setVariable("beenMovingFor", this::getBeenMovingFor, (IAnimationVariableSource owner) -> "The amount of time the character has been moving for.");
        this.setVariable("previousState", this::getPreviousActionContextStateName, (IAnimationVariableSource owner) -> "The character's previous state.");
        this.setVariable("momentumScalar", this::getMomentumScalar, this::setMomentumScalar, (IAnimationVariableSource owner) -> "The amount of linear momentum the character has. Scalar: 0.0 - 1.0");
        this.setVariable("hasTimedActions", this::hasTimedActions, (IAnimationVariableSource owner) -> "The character is performing a timed action. Or has a timed action to do.");
        this.setVariable("isOverEncumbered", this::isOverEncumbered, (IAnimationVariableSource owner) -> "Is the carrying too much weight.");
        if (DebugOptions.instance.character.debug.registerDebugVariables.getValue()) {
            this.registerDebugGameVariables();
        }
        this.setVariable("CriticalHit", this::isCriticalHit, this::setCriticalHit, (IAnimationVariableSource owner) -> "Has the character been attacked by a critical hit.");
        this.setVariable("bKnockedDown", this::isKnockedDown, this::setKnockedDown, (IAnimationVariableSource owner) -> "Has the character been hit hard enough to be knocked down.");
        this.setVariable("bfalling", this::isFalling, (IAnimationVariableSource owner) -> "The character is currently falling.");
        this.setVariable("bdead", this::isDead, (IAnimationVariableSource owner) -> "Is the character dead.");
        this.setVariable("fallTime", this::getFallTime, (IAnimationVariableSource owner) -> "How long has the character been falling.");
        this.setVariable("fallSpeedSeverity", FallSeverity.class, this::getFallSpeedSeverity, (IAnimationVariableSource owner) -> "How fast are we currenly falling. How sever would be the impact, if we were to impact the ground. None, Light, Heavy, Severe, Lethal");
        this.setVariable("bGetUpFromKnees", false);
        this.setVariable("bGetUpFromProne", false);
        this.setVariable("aim", this::isAiming, (IAnimationVariableSource owner) -> "Is the character aiming.");
        this.setVariable("bAimAtfloor", this::isAimAtFloor, (IAnimationVariableSource owner) -> "Is the character aiming at the floor.");
        this.setVariable("aimAtFloorAmount", this::getAimAtFloorAmount, (IAnimationVariableSource owner) -> "The character is aiming at the floor by this amount. Scalar: Horizontal: 0.0 to Down: 1.0");
        this.setVariable("verticalAimAngle", this::getCurrentVerticalAimAngle, (IAnimationVariableSource owner) -> "The vertical aim angle. In degrees: -90 (down) to 90 (up)");
        this.setVariable("AttackAnim", this::isPerformingAttackAnimation, this::setPerformingAttackAnimation, (IAnimationVariableSource owner) -> "Is the character performing an attack animation.");
        this.setVariable("ShoveAnim", this::isPerformingShoveAnimation, this::setPerformingShoveAnimation, (IAnimationVariableSource owner) -> "Is the character performing a shove animation.");
        this.setVariable("StompAnim", this::isPerformingStompAnimation, this::setPerformingStompAnimation, (IAnimationVariableSource owner) -> "Is the character performing a stomp animation.");
        this.setVariable("isStompAnim", this::isShoveStompAnim, this::setShoveStompAnim, (IAnimationVariableSource owner) -> "Is the character's current Shove actually a stomp.");
        this.setVariable("PerformingHostileAnim", this::isPerformingHostileAnimation, (IAnimationVariableSource owner) -> "Is the character performing a hostile animation - an attack, a shove, or a stomp.");
        this.setVariable("FireMode", this::getFireMode, (IAnimationVariableSource owner) -> "The character's equipped weapon's firing mode. Single or Auto.");
        this.setVariable("ShoutType", this::getShoutType, (IAnimationVariableSource owner) -> "The character's 'shout' type. If they are making a noise with an equipped item. Can be: BlowWhistle_primary, BlowWhistle_secondary, BlowHarmonica_primary, BlowHarmonica_secondary");
        this.setVariable("ShoutItemModel", this::getShoutItemModel, (IAnimationVariableSource owner) -> "The equipped item used for the 'shout'. Usually a Whistle or a Harmonica.");
        this.setVariable("isAnimatingBackwards", this::isAnimatingBackwards, this::setAnimatingBackwards, (IAnimationVariableSource owner) -> "Is the character animating backwards. Eg. when dragging a corpse backwards.");
        BaseGrappleable.RegisterGrappleVariables(this.getGameVariablesInternal(), this);
        this.setVariable("GrappleThrowOutWindow", this::isGrappleThrowOutWindow, this::setGrappleThrowOutWindow, (IAnimationVariableSource owner) -> "The character is dragging a corpse and wants to throw them through a window.");
        this.setVariable("GrappleThrowOverFence", this::isGrappleThrowOverFence, this::setGrappleThrowOverFence, (IAnimationVariableSource owner) -> "The character is dragging a corpse and wants to throw them over a fence.");
        this.setVariable("GrappleThrowIntoContainer", this::isGrappleThrowIntoContainer, this::setGrappleThrowIntoContainer, (IAnimationVariableSource owner) -> "The character is dragging a corpse and wants to throw them into a container.");
        this.setVariable("canRagdoll", this::canRagdoll, (IAnimationVariableSource owner) -> "The character can become a ragdoll.");
        this.setVariable("isEditingRagdoll", this::isEditingRagdoll, this::setEditingRagdoll, (IAnimationVariableSource owner) -> "The character is actively editing their ragdoll.");
        this.setVariable("isRagdoll", this::isRagdoll, (IAnimationVariableSource owner) -> "The character is currently ragdolling.");
        this.setVariable("isSimulationActive", this::isRagdollSimulationActive, (IAnimationVariableSource owner) -> "The character is currently ragdolling and their ragdoll simulation is actively running. Once they've settled down on the ground, this becomes FALSE.");
        this.setVariable("isUpright", this::isUpright, (IAnimationVariableSource owner) -> "The character is currently standing upright.");
        this.setVariable("isOnBack", this::isOnBack, (IAnimationVariableSource owner) -> "The character is lying on their back.");
        this.setVariable("isRagdollFall", this::isRagdollFall, this::setRagdollFall, (IAnimationVariableSource owner) -> "The character is falling down, using their ragdoll.");
        this.setVariable("isVehicleCollision", this::isVehicleCollision, this::setVehicleCollision, (IAnimationVariableSource owner) -> "The character has been hit by a car.");
        this.setVariable("usePhysicHitReaction", this::usePhysicHitReaction, this::setUsePhysicHitReaction, (IAnimationVariableSource owner) -> "The character has been hit, and should use their ragdoll physics to react to it.");
        this.setVariable("useRagdollVehicleCollision", this::useRagdollVehicleCollision, (IAnimationVariableSource owner) -> "The character has been hit by a car, and should use their ragdoll physics to react to it.");
        this.setVariable("bHeadLookAround", this::isHeadLookAround, (IAnimationVariableSource owner) -> "The character is looking around.");
        this.setVariable("lookHorizontal", this::getHeadLookHorizontal, (IAnimationVariableSource owner) -> "The character's horizontal head look amount. -1 to 1");
        this.setVariable("lookVertical", this::getHeadLookVertical, (IAnimationVariableSource owner) -> "The character's vertical head look amount. -1 to 1");
        this.setVariable("hitforce", this::getHitForce, (IAnimationVariableSource owner) -> "The character has been hit, by this amount of force.");
        this.setVariable("hitDir", this::getHitDirEnum, (IAnimationVariableSource owner) -> "The direction the character has been hit from.<br />Known values:<br /> FRONT,<br /> BEHIND,<br /> LEFT,<br /> RIGHT.");
        this.setVariable("hitDir.x", () -> this.getHitDir().x, (IAnimationVariableSource owner) -> "The direction the character has been hit from. Along the x-axis.");
        this.setVariable("hitDir.y", () -> this.getHitDir().y, (IAnimationVariableSource owner) -> "The direction the character has been hit from. Along the y-axis.");
        this.setVariable("recoilVarX", this::getRecoilVarX, this::setRecoilVarX, (IAnimationVariableSource owner) -> "The character has fired a weapon, this is the amount of recoil they're currently experiencing, along the x-axis.");
        this.setVariable("recoilVarY", this::getRecoilVarY, this::setRecoilVarY, (IAnimationVariableSource owner) -> "The character has fired a weapon, this is the amount of recoil they're currently experiencing, along the y-axis.");
        this.setVariable("hideEquippedHandL", this::isHideEquippedHandL, this::setHideEquippedHandL, (IAnimationVariableSource owner) -> "The character will have any item in their left hand hidden from rendering.");
        this.setVariable("hideEquippedHandR", this::isHideEquippedHandR, this::setHideEquippedHandR, (IAnimationVariableSource owner) -> "The character will have any item in their right hand hidden from rendering.");
        this.setVariable("aimingMode", AimingMode.class, this::getAimingMode, (IAnimationVariableSource owner) -> "Get the character's aiming mode. REGULAR means it's free-aiming, TARGET_FOUND means it's spotted a target.");
        this.fallDamage.registerVariableCallbacks(this);
    }

    public final boolean isFalling() {
        return (this.falling || this.lastFallSpeed > FallingConstants.isFallingThreshold) && this.getZ() > (float)this.getMinFloorZ();
    }

    private int getMinFloorZ() {
        int minLevel = 0;
        IsoChunk chunk = this.getChunk();
        if (chunk != null) {
            minLevel = chunk.getMinLevel();
        }
        return minLevel;
    }

    private void registerAnimEventCallbacks() {
        this.addAnimEventListener(this::OnAnimEvent_SetVariable);
        this.addAnimEventListener("ClearVariable", this::OnAnimEvent_ClearVariable);
        this.addAnimEventListener("PlaySound", this::OnAnimEvent_PlaySound);
        this.addAnimEventListener("PlaySoundNoBlend", this::OnAnimEvent_PlaySoundNoBlend);
        this.addAnimEventListener("Footstep", this::OnAnimEvent_Footstep);
        this.addAnimEventListener("DamageWhileInTrees", this::OnAnimEvent_DamageWhileInTrees);
        this.addAnimEventListener("TurnAround", this::OnAnimEvent_TurnAround);
        this.addAnimEventListener("TurnAround_FlipSkeleton", this::OnAnimEvent_TurnAroundFlipSkeleton);
        this.addAnimEventListener("SetSharedGrappleType", this::OnAnimEvent_SetSharedGrappleType);
        this.addAnimEventListener("GrapplerLetGo", this::OnAnimEvent_GrapplerLetGo);
        this.addAnimEventListener("FallOnFront", this::OnAnimEvent_FallOnFront);
        this.addAnimEventListener("SetOnFloor", this::OnAnimEvent_SetOnFloor);
        this.addAnimEventListener("SetKnockedDown", this::OnAnimEvent_SetKnockedDown);
        this.addAnimEventListener("IsAlmostUp", this::OnAnimEvent_IsAlmostUp);
        this.addAnimEventListener("KilledByAttacker", this::OnAnimEvent_KilledByAttacker);
    }

    private void OnAnimEvent_GrapplerLetGo(IsoGameCharacter owner, String grappleResult) {
        if (GameServer.server) {
            DebugLog.Grapple.println("GrapplerLetGo.");
        }
        LuaEventManager.triggerEvent("GrapplerLetGo", owner, grappleResult);
        owner.LetGoOfGrappled(grappleResult);
    }

    private void OnAnimEvent_FallOnFront(IsoGameCharacter owner, boolean fallOnFront) {
        owner.setFallOnFront(fallOnFront);
    }

    private void OnAnimEvent_SetOnFloor(IsoGameCharacter owner, boolean onFloor) {
        owner.setOnFloor(onFloor);
    }

    private void OnAnimEvent_SetKnockedDown(IsoGameCharacter owner, boolean knockedDown) {
        owner.setKnockedDown(knockedDown);
    }

    protected void OnAnimEvent_IsAlmostUp(IsoGameCharacter owner) {
        owner.setOnFloor(false);
        owner.setKnockedDown(false);
        owner.setSitOnGround(false);
    }

    protected void OnAnimEvent_KilledByAttacker(IsoGameCharacter owner) {
        owner.Kill(this.getAttackedBy());
    }

    public boolean isShoveStompAnim() {
        return this.shoveStompAnim;
    }

    public void setShoveStompAnim(boolean val) {
        this.shoveStompAnim = val;
    }

    private void onGrappleBegin() {
        IGrappleable grappledBy = this.getGrappledBy();
        IAnimatable grappledByAnimatable = Type.tryCastTo(grappledBy, IAnimatable.class);
        if (grappledByAnimatable != null && grappledByAnimatable.isAnimationRecorderActive()) {
            this.setAnimRecorderActive(true, true);
        }
    }

    private void onGrappleEnded() {
        this.setGrappleThrowOutWindow(false);
    }

    public boolean canUseCurrentPoseForCorpse() {
        if (GameClient.client || GameServer.server) {
            return false;
        }
        if (this.isSceneCulled()) {
            return false;
        }
        if (!this.hasActiveModel()) {
            return false;
        }
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        if (animationPlayer == null) {
            return false;
        }
        if (this.isRagdollSimulationActive()) {
            return true;
        }
        return !animationPlayer.isBoneTransformsNeedFirstFrame();
    }

    public float getRecoilVarX() {
        return this.recoil.recoilVarX;
    }

    public void setRecoilVarX(float recoilVarX) {
        this.recoil.recoilVarX = recoilVarX;
    }

    public float getRecoilVarY() {
        return this.recoil.recoilVarY;
    }

    public void setRecoilVarY(float recoilVarY) {
        this.recoil.recoilVarY = recoilVarY;
    }

    public void setGrappleThrowOutWindow(boolean newValue) {
        this.isGrappleThrowOutWindow = newValue;
    }

    public boolean isGrappleThrowOutWindow() {
        return this.isGrappleThrowOutWindow;
    }

    public void setGrappleThrowOverFence(boolean newValue) {
        this.isGrappleThrowOverFence = newValue;
    }

    public boolean isGrappleThrowOverFence() {
        return this.isGrappleThrowOverFence;
    }

    public void setGrappleThrowIntoContainer(boolean newValue) {
        this.isGrappleThrowIntoContainer = newValue;
    }

    public boolean isGrappleThrowIntoContainer() {
        return this.isGrappleThrowIntoContainer;
    }

    public void updateRecoilVar() {
        this.setRecoilVarY(0.0f);
        this.setRecoilVarX(0.0f + (float)this.getPerkLevel(PerkFactory.Perks.Aiming) / 10.0f);
    }

    private void registerDebugGameVariables() {
        int maxTracks = 9;
        int maxLayers = 2;
        for (int layerIdx = 0; layerIdx < 2; ++layerIdx) {
            for (int trackIdx = 0; trackIdx < 9; ++trackIdx) {
                this.dbgRegisterAnimTrackVariable(layerIdx, trackIdx);
            }
        }
        this.setVariable("dbg.anm.dx", () -> this.getDeferredMovement((Vector2)IsoGameCharacter.tempo).x / GameTime.instance.getMultiplier(), (IAnimationVariableSource owner) -> "The current animation's deferred motion's x value.");
        this.setVariable("dbg.anm.dy", () -> this.getDeferredMovement((Vector2)IsoGameCharacter.tempo).y / GameTime.instance.getMultiplier(), (IAnimationVariableSource owner) -> "The current animation's deferred motion's y value.");
        this.setVariable("dbg.anm.da", () -> this.getDeferredAngleDelta() / GameTime.instance.getMultiplier(), (IAnimationVariableSource owner) -> "The current animation's deferred rotation's angle value.");
        this.setVariable("dbg.anm.daw", this::getDeferredRotationWeight, (IAnimationVariableSource owner) -> "The current animation's deferred rotation weight value.");
        this.setVariable("dbg.forward", () -> this.getForwardDirectionX() + "; " + this.getForwardDirectionY(), (IAnimationVariableSource owner) -> "The current forward direction vector. (x, y)");
        this.setVariable("dbg.anm.blend.fbx_x", () -> DebugOptions.instance.animation.blendUseFbx.getValue() ? 1.0f : 0.0f, (IAnimationVariableSource owner) -> "The current setting of the BlendUseFbx flag.");
        this.setVariable("dbg.lastFallSpeed", this::getLastFallSpeed, (IAnimationVariableSource owner) -> "The speed at which the character hit the ground.");
        this.fallDamage.registerDebugGameVariables(this);
        this.debugVariablesRegistered = true;
    }

    private void dbgRegisterAnimTrackVariable(int layerIdx, int trackIdx) {
        this.setVariable(String.format("dbg.anm.track%d%d", layerIdx, trackIdx), () -> this.dbgGetAnimTrackName(layerIdx, trackIdx), (IAnimationVariableSource owner) -> "The current animation track at index:" + trackIdx + " layer " + layerIdx);
        this.setVariable(String.format("dbg.anm.t.track%d%d", layerIdx, trackIdx), () -> this.dbgGetAnimTrackTime(layerIdx, trackIdx), (IAnimationVariableSource owner) -> "The current animation track time at index:" + trackIdx + " layer " + layerIdx);
        this.setVariable(String.format("dbg.anm.w.track%d%d", layerIdx, trackIdx), () -> this.dbgGetAnimTrackWeight(layerIdx, trackIdx), (IAnimationVariableSource owner) -> "The current animationt rack weight at index:" + trackIdx + " layer " + layerIdx);
    }

    public void setVehicleHitLocation(BaseVehicle vehicle) {
    }

    public float getMomentumScalar() {
        return this.momentumScalar;
    }

    public void setMomentumScalar(float val) {
        this.momentumScalar = val;
    }

    public Vector2 getDeferredMovement(Vector2 result) {
        return this.getDeferredMovement(result, false);
    }

    protected Vector2 getDeferredMovement(Vector2 result, boolean reset) {
        if (!this.hasAnimationPlayer()) {
            result.set(0.0f, 0.0f);
            return result;
        }
        this.animPlayer.getDeferredMovement(result, reset);
        return result;
    }

    public Vector3 getDeferredMovementFromRagdoll(Vector3 result) {
        if (!this.hasAnimationPlayer()) {
            result.set(0.0f, 0.0f, 0.0f);
            return result;
        }
        return this.animPlayer.getDeferredMovementFromRagdoll(result);
    }

    public float getDeferredAngleDelta() {
        if (this.animPlayer == null) {
            return 0.0f;
        }
        return this.animPlayer.getDeferredAngleDelta() * 57.295776f;
    }

    public float getDeferredRotationWeight() {
        if (this.animPlayer == null) {
            return 0.0f;
        }
        return this.animPlayer.getDeferredRotationWeight();
    }

    @Override
    public Vector3f getTargetGrapplePos(Vector3f result) {
        if (this.animPlayer == null) {
            result.set(0.0f, 0.0f, 0.0f);
            return result;
        }
        return this.animPlayer.getTargetGrapplePos(result);
    }

    @Override
    public Vector3 getTargetGrapplePos(Vector3 result) {
        if (this.animPlayer == null) {
            result.set(0.0f, 0.0f, 0.0f);
            return result;
        }
        return this.animPlayer.getTargetGrapplePos(result);
    }

    @Override
    public void setTargetGrapplePos(float x, float y, float z) {
        if (this.animPlayer != null) {
            this.animPlayer.setTargetGrapplePos(x, y, z);
        }
    }

    @Override
    public Vector2 getTargetGrappleRotation(Vector2 result) {
        if (this.animPlayer == null) {
            result.set(1.0f, 0.0f);
            return result;
        }
        return this.animPlayer.getTargetGrappleRotation(result);
    }

    public boolean isStrafing() {
        if (this.getPath2() != null && this.pfb2.isStrafing()) {
            return true;
        }
        return this.isAiming();
    }

    public AnimationTrack dbgGetAnimTrack(int layerIdx, int trackIdx) {
        if (this.animPlayer == null) {
            return null;
        }
        AnimationPlayer animPlayer = this.animPlayer;
        AnimationMultiTrack multiTrack = animPlayer.getMultiTrack();
        List<AnimationTrack> tracks = multiTrack.getTracks();
        AnimationTrack foundTrack = null;
        int currentLayerTrackIdx = 0;
        int count = tracks.size();
        for (int i = 0; i < count; ++i) {
            AnimationTrack track = tracks.get(i);
            int trackLayer = track.getLayerIdx();
            if (trackLayer != layerIdx) continue;
            if (currentLayerTrackIdx == trackIdx) {
                foundTrack = track;
                break;
            }
            ++currentLayerTrackIdx;
        }
        return foundTrack;
    }

    public String dbgGetAnimTrackName(int layerIdx, int trackIdx) {
        AnimationTrack track = this.dbgGetAnimTrack(layerIdx, trackIdx);
        return track != null ? track.getName() : "";
    }

    public float dbgGetAnimTrackTime(int layerIdx, int trackIdx) {
        AnimationTrack track = this.dbgGetAnimTrack(layerIdx, trackIdx);
        return track != null ? track.getCurrentTrackTime() : 0.0f;
    }

    public float dbgGetAnimTrackWeight(int layerIdx, int trackIdx) {
        AnimationTrack track = this.dbgGetAnimTrack(layerIdx, trackIdx);
        return track != null ? track.getBlendWeight() : 0.0f;
    }

    public float getTwist() {
        if (this.animPlayer != null) {
            return 57.295776f * this.animPlayer.getTwistAngle();
        }
        return 0.0f;
    }

    public float getShoulderTwist() {
        if (this.animPlayer != null) {
            return 57.295776f * this.animPlayer.getShoulderTwistAngle();
        }
        return 0.0f;
    }

    public float getMaxTwist() {
        return this.maxTwist;
    }

    public void setMaxTwist(float degrees) {
        this.maxTwist = degrees;
    }

    public float getExcessTwist() {
        if (this.animPlayer != null) {
            return 57.295776f * this.animPlayer.getExcessTwistAngle();
        }
        return 0.0f;
    }

    public int getNumTwistBones() {
        if (this.animPlayer != null) {
            return this.animPlayer.getNumTwistBones();
        }
        return 0;
    }

    public float getAbsoluteExcessTwist() {
        return Math.abs(this.getExcessTwist());
    }

    public float getAnimAngleTwistDelta() {
        return this.animPlayer != null ? this.animPlayer.angleTwistDelta : 0.0f;
    }

    public float getAnimAngleStepDelta() {
        return this.animPlayer != null ? this.animPlayer.angleStepDelta : 0.0f;
    }

    public float getTargetTwist() {
        return this.animPlayer != null ? 57.295776f * this.animPlayer.getTargetTwistAngle() : 0.0f;
    }

    @Override
    public boolean isRangedWeaponEmpty() {
        return this.rangedWeaponEmpty;
    }

    @Override
    public void setRangedWeaponEmpty(boolean val) {
        this.rangedWeaponEmpty = val;
    }

    public boolean hasFootInjury() {
        return !StringUtils.isNullOrWhitespace(this.getFootInjuryType());
    }

    public boolean isInTrees2(boolean ignoreBush) {
        if (this.isCurrentState(BumpedState.instance())) {
            return false;
        }
        IsoGridSquare currentSquare = this.getCurrentSquare();
        if (currentSquare != null) {
            IsoTree tree;
            if (currentSquare.has(IsoObjectType.tree) && ((tree = currentSquare.getTree()) == null || ignoreBush && tree.getSize() > 2 || !ignoreBush)) {
                return true;
            }
            String movement = currentSquare.getProperties().get("Movement");
            if ("HedgeLow".equalsIgnoreCase(movement) || "HedgeHigh".equalsIgnoreCase(movement)) {
                return true;
            }
            return !ignoreBush && currentSquare.hasBush();
        }
        return false;
    }

    public boolean isInTreesNoBush() {
        return this.isInTrees2(true);
    }

    public boolean isInTrees() {
        return this.isInTrees2(false);
    }

    public static HashMap<Integer, SurvivorDesc> getSurvivorMap() {
        return SurvivorMap;
    }

    public static int[] getLevelUpLevels() {
        return LevelUpLevels;
    }

    public static Vector2 getTempo() {
        return tempo;
    }

    public static Vector2 getTempo2() {
        return tempo2;
    }

    public static ColorInfo getInf() {
        return inf;
    }

    public boolean getIsNPC() {
        return this.isNpc;
    }

    public void setIsNPC(boolean isAI) {
        this.isNpc = isAI;
    }

    @Override
    public BaseCharacterSoundEmitter getEmitter() {
        return this.emitter;
    }

    public void updateEmitter() {
        this.getFMODParameters().update();
        if (!IsoWorld.instance.emitterUpdate && !this.emitter.hasSoundsToStart()) {
            return;
        }
        if (this.isZombie() && this.isProne()) {
            CombatManager.getBoneWorldPos(this, "Bip01_Head", tempVectorBonePos);
            this.emitter.set(IsoGameCharacter.tempVectorBonePos.x, IsoGameCharacter.tempVectorBonePos.y, this.getZ());
            this.emitter.tick();
            return;
        }
        this.emitter.set(this.getX(), this.getY(), this.getZ());
        this.emitter.tick();
    }

    protected void doDeferredMovement() {
        if (!this.hasAnimationPlayer()) {
            return;
        }
        if (GameClient.client && HitReactionNetworkAI.isEnabled(this) && this.getHitReactionNetworkAI() != null) {
            if (this.getHitReactionNetworkAI().isStarted()) {
                this.getHitReactionNetworkAI().move();
                this.animPlayer.resetDeferredMovementAccum();
                return;
            }
            if (this.isDead() && this.getHitReactionNetworkAI().isDoSkipMovement()) {
                this.animPlayer.resetDeferredMovementAccum();
                return;
            }
        }
        if (!GameServer.server && !this.isAnimationUpdatingThisFrame()) {
            return;
        }
        Vector2 dMovement = tempo;
        this.getDeferredMovement(dMovement, true);
        if (this.getPath2() != null && !this.isCurrentState(ClimbOverFenceState.instance()) && !this.isCurrentState(ClimbThroughWindowState.instance())) {
            if (this.isCurrentState(WalkTowardState.instance()) || this.isCurrentState(AnimalWalkState.instance()) && !this.getNetworkCharacterAI().usePathFind) {
                DebugLog.General.warn("WalkTowardState but path2 != null");
                this.setPath2(null);
            }
            return;
        }
        if (this.isCurrentState(WalkTowardState.instance())) {
            Vector2 targetVec = BaseVehicle.allocVector2();
            targetVec.x = this.getPathFindBehavior2().getTargetX();
            targetVec.y = this.getPathFindBehavior2().getTargetY();
            targetVec.x -= this.getX();
            targetVec.y -= this.getY();
            if (targetVec.getLengthSquared() < dMovement.getLengthSquared()) {
                dMovement.setLength(targetVec.getLength());
            }
            BaseVehicle.releaseVector2(targetVec);
        }
        if (GameClient.client) {
            if (this instanceof IsoZombie && ((IsoZombie)this).isRemoteZombie()) {
                if (this.getCurrentState() != ClimbOverFenceState.instance() && this.getCurrentState() != ClimbThroughWindowState.instance() && this.getCurrentState() != ClimbOverWallState.instance() && this.getCurrentState() != StaggerBackState.instance() && this.getCurrentState() != ZombieHitReactionState.instance() && this.getCurrentState() != ZombieFallDownState.instance() && this.getCurrentState() != ZombieFallingState.instance() && this.getCurrentState() != ZombieOnGroundState.instance() && this.getCurrentState() != AttackNetworkState.instance()) {
                    this.animPlayer.resetDeferredMovementAccum();
                    return;
                }
            } else if (this instanceof IsoAnimal && !((IsoAnimal)this).isLocalPlayer() ? !this.isCurrentState(AnimalIdleState.instance()) && !((IsoAnimal)this).isHappy() : this instanceof IsoPlayer && !((IsoPlayer)this).isLocalPlayer() && !this.isCurrentState(CollideWithWallState.instance()) && !this.isCurrentState(PlayerGetUpState.instance()) && !this.isCurrentState(BumpedState.instance())) {
                return;
            }
        }
        if (this.isGrappling() || this.isBeingGrappled()) {
            Vector3 grappleOffset = new Vector3();
            this.getGrappleOffset(grappleOffset);
            dMovement.x += grappleOffset.x;
            dMovement.y += grappleOffset.y;
        }
        if (GameClient.client && this instanceof IsoZombie && this.isCurrentState(StaggerBackState.instance())) {
            float len = dMovement.getLength();
            dMovement.set(this.getHitDir());
            dMovement.setLength(len);
        }
        if (this.isDeferredMovementEnabled()) {
            if (this.isAnimationRecorderActive()) {
                this.setVariable("deferredMovement.x", dMovement.x);
                this.setVariable("deferredMovement.y", dMovement.y);
            }
            this.MoveUnmodded(dMovement);
        } else if (this.isAnimationRecorderActive()) {
            this.setVariable("deferredMovement.x", 0.0f);
            this.setVariable("deferredMovement.y", 0.0f);
        }
    }

    public void doDeferredMovementFromRagdoll(Vector3 dMovement) {
        if (!this.isRagdoll()) {
            return;
        }
        if (!this.hasAnimationPlayer()) {
            return;
        }
        if (!GameServer.server && !this.isAnimationUpdatingThisFrame()) {
            return;
        }
        this.moveUnmoddedInternal(dMovement.x, dMovement.y);
        this.setX(this.getNextX());
        this.setY(this.getNextY());
        this.setZ(this.getZ() + dMovement.z);
        if (this.isAnimationRecorderActive()) {
            this.setVariable("deferredMovement_Ragdoll.x", dMovement.x);
            this.setVariable("deferredMovement_Ragdoll.y", dMovement.y);
            this.setVariable("deferredMovement_Ragdoll.z", dMovement.z);
        }
    }

    @Override
    public ActionContext getActionContext() {
        return this.actionContext;
    }

    public String getPreviousActionContextStateName() {
        ActionContext context = this.getActionContext();
        return context == null ? "" : context.peekPreviousStateName();
    }

    public String getCurrentActionContextStateName() {
        ActionContext context = this.getActionContext();
        if (context == null || context.getCurrentState() == null) {
            return "";
        }
        return context.getCurrentStateName();
    }

    @Override
    public boolean hasAnimationPlayer() {
        return this.animPlayer != null;
    }

    @Override
    public AnimationPlayer getAnimationPlayer() {
        Model model = ModelManager.instance.getBodyModel(this);
        boolean hasTracks = false;
        if (this.animPlayer != null && this.animPlayer.getModel() != model) {
            hasTracks = this.animPlayer.getMultiTrack().getTrackCount() > 0;
            this.animPlayer = Pool.tryRelease(this.animPlayer);
        }
        if (this.animPlayer == null) {
            this.animPlayer = AnimationPlayer.alloc(model);
            this.onAnimPlayerCreated(this.animPlayer);
            if (hasTracks) {
                this.getAdvancedAnimator().OnAnimDataChanged(false);
            }
        }
        return this.animPlayer;
    }

    public void releaseAnimationPlayer() {
        this.animPlayer = Pool.tryRelease(this.animPlayer);
    }

    protected void onAnimPlayerCreated(AnimationPlayer animationPlayer) {
        animationPlayer.setIsoGameCharacter(this);
        animationPlayer.setRecorder(this.getAnimationRecorder());
        animationPlayer.setTwistBones("Bip01_Pelvis", "Bip01_Spine", "Bip01_Spine1", "Bip01_Neck", "Bip01_Head");
        animationPlayer.setCounterRotationBone("Bip01");
    }

    @Override
    public AdvancedAnimator getAdvancedAnimator() {
        return this.advancedAnimator;
    }

    @Override
    public ModelInstance getModelInstance() {
        if (this.legsSprite == null) {
            return null;
        }
        if (this.legsSprite.modelSlot == null) {
            return null;
        }
        return this.legsSprite.modelSlot.model;
    }

    public String getCurrentStateName() {
        return this.stateMachine.getCurrent() == null ? null : this.stateMachine.getCurrent().getName();
    }

    public String getPreviousStateName() {
        return this.stateMachine.getPrevious() == null ? null : this.stateMachine.getPrevious().getName();
    }

    public String getAnimationDebug() {
        if (this.advancedAnimator != null) {
            return this.instancename + "\n" + this.advancedAnimator.GetDebug();
        }
        return this.instancename + "\n - No Animator";
    }

    public String getStatisticsDebug() {
        return "Statistics" + StatisticsManager.getInstance().getAllStatisticsDebug();
    }

    @Override
    public String getTalkerType() {
        return this.chatElement.getTalkerType();
    }

    public void spinToZeroAllAnimNodes() {
        AdvancedAnimator aa = this.getAdvancedAnimator();
        AnimLayer rootLayer = aa.getRootLayer();
        List<LiveAnimNode> liveAnimNodes = rootLayer.getLiveAnimNodes();
        for (int i = 0; i < liveAnimNodes.size(); ++i) {
            LiveAnimNode animNode = liveAnimNodes.get(i);
            animNode.stopTransitionIn();
            animNode.setWeightsToZero();
        }
    }

    public boolean isAnimForecasted() {
        return System.currentTimeMillis() < this.isAnimForecasted;
    }

    public void setAnimForecasted(int timeMs) {
        this.isAnimForecasted = System.currentTimeMillis() + (long)timeMs;
    }

    @Override
    public void resetModel() {
        ModelManager.instance.Reset(this);
    }

    @Override
    public void resetModelNextFrame() {
        ModelManager.instance.ResetNextFrame(this);
    }

    protected void onTrigger_setClothingToXmlTriggerFile(TriggerXmlFile triggerXml) {
        OutfitManager.Reload();
        if (!StringUtils.isNullOrWhitespace(triggerXml.outfitName)) {
            String outfitName = triggerXml.outfitName;
            DebugType.Clothing.debugln("Desired outfit name: %s", outfitName);
            Outfit desiredOutfitSource = triggerXml.isMale ? OutfitManager.instance.FindMaleOutfit(outfitName) : OutfitManager.instance.FindFemaleOutfit(outfitName);
            if (desiredOutfitSource == null) {
                DebugType.Clothing.error("Could not find outfit: %s", outfitName);
                return;
            }
            if (this.female == triggerXml.isMale && this instanceof IHumanVisual) {
                ((IHumanVisual)((Object)this)).getHumanVisual().clear();
            }
            boolean bl = this.female = !triggerXml.isMale;
            if (this.descriptor != null) {
                this.descriptor.setFemale(this.female);
            }
            this.dressInNamedOutfit(desiredOutfitSource.name);
            this.advancedAnimator.OnAnimDataChanged(false);
            if (this instanceof IsoPlayer) {
                LuaEventManager.triggerEvent("OnClothingUpdated", this);
            }
        } else if (!StringUtils.isNullOrWhitespace(triggerXml.clothingItemGuid)) {
            boolean foundItem;
            String gameModID = "game";
            String itemGUID = "game-" + triggerXml.clothingItemGuid;
            boolean bl = foundItem = OutfitManager.instance.getClothingItem(itemGUID) != null;
            if (!foundItem) {
                for (String modID : ZomboidFileSystem.instance.getModIDs()) {
                    itemGUID = modID + "-" + triggerXml.clothingItemGuid;
                    if (OutfitManager.instance.getClothingItem(itemGUID) == null) continue;
                    foundItem = true;
                    break;
                }
            }
            if (foundItem) {
                this.dressInClothingItem(itemGUID);
                if (this instanceof IsoPlayer) {
                    LuaEventManager.triggerEvent("OnClothingUpdated", this);
                }
            }
        }
        ModelManager.instance.Reset(this);
    }

    protected void onTrigger_setAnimStateToTriggerFile(AnimStateTriggerXmlFile triggerXml) {
        String animSetName = this.GetAnimSetName();
        if (!StringUtils.equalsIgnoreCase(animSetName, triggerXml.animSet)) {
            this.setVariable("dbgForceAnim", false);
            this.restoreAnimatorStateToActionContext();
            return;
        }
        DebugOptions.instance.animation.animLayer.allowAnimNodeOverride.setValue(triggerXml.forceAnim);
        if (this.advancedAnimator.containsState(triggerXml.stateName)) {
            this.setVariable("dbgForceAnim", triggerXml.forceAnim);
            this.setVariable("dbgForceAnimStateName", triggerXml.stateName);
            this.setVariable("dbgForceAnimNodeName", triggerXml.nodeName);
            this.setVariable("dbgForceAnimScalars", triggerXml.setScalarValues);
            this.setVariable("dbgForceScalar", triggerXml.scalarValue);
            this.setVariable("dbgForceScalar2", triggerXml.scalarValue2);
            this.advancedAnimator.setState(triggerXml.stateName);
        } else {
            DebugType.Animation.error("State not found: %s", triggerXml.stateName);
            this.restoreAnimatorStateToActionContext();
        }
    }

    private void restoreAnimatorStateToActionContext() {
        if (this.actionContext.getCurrentState() != null) {
            this.advancedAnimator.setState(this.actionContext.getCurrentStateName(), PZArrayUtil.listConvert(this.actionContext.getChildStates(), state -> state.getName()));
        }
    }

    @Override
    public void clothingItemChanged(String itemGuid) {
        if (this.wornItems != null) {
            for (int i = 0; i < this.wornItems.size(); ++i) {
                InventoryItem item = this.wornItems.getItemByIndex(i);
                ClothingItem clothingItem = item.getClothingItem();
                if (clothingItem == null || !clothingItem.isReady() || !clothingItem.guid.equals(itemGuid)) continue;
                ClothingItemReference itemRef = new ClothingItemReference();
                itemRef.itemGuid = itemGuid;
                itemRef.randomize();
                item.getVisual().synchWithOutfit(itemRef);
                item.synchWithVisual();
                this.resetModelNextFrame();
            }
        }
    }

    public void reloadOutfit() {
        ModelManager.instance.Reset(this);
    }

    @Override
    public void setSceneCulled(boolean isCulled) {
        super.setSceneCulled(isCulled);
        try {
            if (this.isSceneCulled()) {
                ModelManager.instance.Remove(this);
            } else {
                ModelManager.instance.Add(this);
            }
        }
        catch (Exception ex) {
            System.err.println("Error in IsoGameCharacter.setSceneCulled(" + isCulled + "):");
            ExceptionLogger.logException(ex);
            ModelManager.instance.Remove(this);
            this.legsSprite.modelSlot = null;
        }
    }

    public void setAddedToModelManager(ModelManager modelManager, boolean isAdded) {
        if (this.isAddedToModelManager == isAdded) {
            return;
        }
        this.isAddedToModelManager = isAdded;
        if (isAdded) {
            this.restoreAnimatorStateToActionContext();
            DebugFileWatcher.instance.add(this.animStateTriggerWatcher);
            OutfitManager.instance.addClothingItemListener(this);
        } else {
            DebugFileWatcher.instance.remove(this.animStateTriggerWatcher);
            OutfitManager.instance.removeClothingItemListener(this);
        }
    }

    public boolean isAddedToModelManager() {
        return this.isAddedToModelManager;
    }

    public void dressInRandomOutfit() {
        DebugType.Clothing.println("IsoGameCharacter.dressInRandomOutfit>");
        Outfit randomOutfitSource = OutfitManager.instance.GetRandomOutfit(this.isFemale());
        if (randomOutfitSource != null) {
            this.dressInNamedOutfit(randomOutfitSource.name);
        }
    }

    public void dressInRandomNonSillyOutfit() {
        DebugType.Clothing.println("IsoGameCharacter.dressInRandomOutfit>");
        Outfit randomOutfitSource = OutfitManager.instance.GetRandomNonSillyOutfit(this.isFemale());
        if (randomOutfitSource != null) {
            this.dressInNamedOutfit(randomOutfitSource.name);
        }
    }

    @Override
    public void dressInNamedOutfit(String outfitName) {
    }

    @Override
    public void dressInPersistentOutfit(String outfitName) {
        if (this.isZombie()) {
            this.getDescriptor().setForename(SurvivorFactory.getRandomForename(this.isFemale()));
        }
        int outfitID = PersistentOutfits.instance.pickOutfit(outfitName, this.isFemale());
        this.dressInPersistentOutfitID(outfitID);
    }

    @Override
    public void dressInPersistentOutfitID(int outfitID) {
    }

    @Override
    public String getOutfitName() {
        if (this instanceof IHumanVisual) {
            HumanVisual humanVisual = ((IHumanVisual)((Object)this)).getHumanVisual();
            Outfit outfit = humanVisual.getOutfit();
            return outfit == null ? null : outfit.name;
        }
        return null;
    }

    public void dressInClothingItem(String itemGUID) {
    }

    public Outfit getRandomDefaultOutfit() {
        IsoGridSquare square = this.getCurrentSquare();
        IsoRoom room = square == null ? null : square.getRoom();
        String roomName = room == null ? null : room.getName();
        return ZombiesZoneDefinition.getRandomDefaultOutfit(this.isFemale(), roomName);
    }

    public ModelInstance getModel() {
        if (this.legsSprite != null && this.legsSprite.modelSlot != null) {
            return this.legsSprite.modelSlot.model;
        }
        return null;
    }

    public boolean hasActiveModel() {
        return this.legsSprite != null && this.legsSprite.hasActiveModel();
    }

    @Override
    public boolean hasItems(String type, int count) {
        int total = this.inventory.getItemCount(type);
        return count <= total;
    }

    public int getLevelUpLevels(int level) {
        if (LevelUpLevels.length <= level) {
            return LevelUpLevels[LevelUpLevels.length - 1];
        }
        return LevelUpLevels[level];
    }

    public int getLevelMaxForXp() {
        return LevelUpLevels.length;
    }

    @Override
    public int getXpForLevel(int level) {
        if (level < LevelUpLevels.length) {
            return (int)((float)LevelUpLevels[level] * this.levelUpMultiplier);
        }
        return (int)((float)(LevelUpLevels[LevelUpLevels.length - 1] + (level - LevelUpLevels.length + 1) * 400) * this.levelUpMultiplier);
    }

    public void DoDeath(HandWeapon weapon, IsoGameCharacter wielder) {
        this.DoDeath(weapon, wielder, true);
    }

    public void DoDeath(HandWeapon weapon, IsoGameCharacter wielder, boolean isGory) {
        this.OnDeath();
        if (this.getAttackedBy() instanceof IsoPlayer && GameServer.server && this instanceof IsoPlayer) {
            PVPLogTool.logKill((IsoPlayer)this.getAttackedBy(), (IsoPlayer)this);
        } else {
            if (GameServer.server && this instanceof IsoPlayer) {
                LoggerManager.getLogger("user").write("user " + ((IsoPlayer)this).username + " died at " + LoggerManager.getPlayerCoords(this) + " (non pvp)");
            }
            if (ServerOptions.instance.announceDeath.getValue() && !this.isAnimal() && this instanceof IsoPlayer && GameServer.server) {
                ChatServer.getInstance().sendMessageToServerChat(((IsoPlayer)this).username + " is dead.");
            }
        }
        if (GameServer.server && ServerOptions.instance.dropOffWhiteListAfterDeath.getValue() && this instanceof IsoPlayer && !this.isAnimal() && !((IsoPlayer)this).getRole().hasCapability(Capability.CanAlwaysJoinServer)) {
            try {
                ServerWorldDatabase.instance.removeUser(((IsoPlayer)this).getUsername(), GameServer.serverName);
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }
        this.doDeathSplatterAndSounds(weapon, wielder, isGory);
    }

    /*
     * Enabled aggressive block sorting
     */
    public void doDeathSplatterAndSounds(HandWeapon weapon, IsoGameCharacter wielder, boolean isGory) {
        if (!this.onDeath_ShouldDoSplatterAndSounds(weapon, wielder, isGory)) {
            return;
        }
        if (this.isDoDeathSound()) {
            this.playDeadSound();
        }
        this.setDoDeathSound(false);
        if (!this.isDead()) {
            return;
        }
        float dz = 0.5f;
        if (this.isZombie() && (((IsoZombie)this).crawling || this.getCurrentState() == ZombieOnGroundState.instance())) {
            dz = 0.2f;
        }
        if (GameServer.server && isGory) {
            boolean isRadial = this.isOnFloor() && wielder instanceof IsoPlayer && weapon != null && "BareHands".equals(weapon.getType());
            GameServer.sendBloodSplatter(weapon, this.getX(), this.getY(), this.getZ() + dz, this.getHitDir(), this.isCloseKilled(), isRadial);
        }
        if (weapon != null && SandboxOptions.instance.bloodLevel.getValue() > 1 && isGory) {
            int spn = weapon.getSplatNumber();
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
                    break;
                }
            }
            for (int n = 0; n < spn; ++n) {
                this.splatBlood(3, 0.3f);
            }
        }
        if (weapon != null && SandboxOptions.instance.bloodLevel.getValue() > 1 && isGory) {
            this.splatBloodFloorBig();
        }
        if (wielder != null && wielder.xp != null) {
            wielder.xp.AddXP(weapon, 3);
        }
        if (SandboxOptions.instance.bloodLevel.getValue() > 1 && this.isOnFloor() && wielder instanceof IsoPlayer) {
            IsoPlayer player = (IsoPlayer)wielder;
            if (weapon == player.bareHands && isGory) {
                this.playBloodSplatterSound();
                int sx = -1;
                while (true) {
                    if (sx > 1) {
                        new IsoZombieGiblets(IsoZombieGiblets.GibletType.Eye, this.getCell(), this.getX(), this.getY(), this.getZ() + dz, this.getHitDir().x * 0.8f, this.getHitDir().y * 0.8f);
                        return;
                    }
                    for (int sy = -1; sy <= 1; ++sy) {
                        if (sx == 0 && sy == 0) continue;
                        new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + dz, (float)sx * Rand.Next(0.25f, 0.5f), (float)sy * Rand.Next(0.25f, 0.5f));
                    }
                    ++sx;
                }
            }
        }
        if (SandboxOptions.instance.bloodLevel.getValue() <= 1) return;
        if (!isGory) return;
        this.playBloodSplatterSound();
        new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + dz, this.getHitDir().x * 1.5f, this.getHitDir().y * 1.5f);
        IsoGameCharacter.tempo.x = this.getHitDir().x;
        IsoGameCharacter.tempo.y = this.getHitDir().y;
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
                break;
            }
        }
        int i = 0;
        while (i < nbRepeat) {
            if (Rand.Next(this.isCloseKilled() ? 8 : rand) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + dz, this.getHitDir().x * 1.5f, this.getHitDir().y * 1.5f);
            }
            if (Rand.Next(this.isCloseKilled() ? 8 : rand) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + dz, this.getHitDir().x * 1.5f, this.getHitDir().y * 1.5f);
            }
            if (Rand.Next(this.isCloseKilled() ? 8 : rand) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + dz, this.getHitDir().x * 1.8f, this.getHitDir().y * 1.8f);
            }
            if (Rand.Next(this.isCloseKilled() ? 8 : rand) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + dz, this.getHitDir().x * 1.9f, this.getHitDir().y * 1.9f);
            }
            if (Rand.Next(this.isCloseKilled() ? 4 : rand2) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + dz, this.getHitDir().x * 3.5f, this.getHitDir().y * 3.5f);
            }
            if (Rand.Next(this.isCloseKilled() ? 4 : rand2) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + dz, this.getHitDir().x * 3.8f, this.getHitDir().y * 3.8f);
            }
            if (Rand.Next(this.isCloseKilled() ? 4 : rand2) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + dz, this.getHitDir().x * 3.9f, this.getHitDir().y * 3.9f);
            }
            if (Rand.Next(this.isCloseKilled() ? 4 : rand2) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + dz, this.getHitDir().x * 1.5f, this.getHitDir().y * 1.5f);
            }
            if (Rand.Next(this.isCloseKilled() ? 4 : rand2) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + dz, this.getHitDir().x * 3.8f, this.getHitDir().y * 3.8f);
            }
            if (Rand.Next(this.isCloseKilled() ? 4 : rand2) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + dz, this.getHitDir().x * 3.9f, this.getHitDir().y * 3.9f);
            }
            if (Rand.Next(this.isCloseKilled() ? 9 : 6) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.Eye, this.getCell(), this.getX(), this.getY(), this.getZ() + dz, this.getHitDir().x * 0.8f, this.getHitDir().y * 0.8f);
            }
            ++i;
        }
    }

    public boolean onDeath_ShouldDoSplatterAndSounds(HandWeapon weapon, IsoGameCharacter wielder, boolean isGory) {
        return true;
    }

    protected boolean TestIfSeen(int playerIndex, IsoPlayer player) {
        float angle;
        IsoZombie zombie;
        IsoGameCharacter isoGameCharacter;
        boolean canSee;
        if (player == null || this == player) {
            return false;
        }
        float dist = this.DistToProper(player);
        if (dist > GameTime.getInstance().getViewDist()) {
            return false;
        }
        boolean couldSee = GameServer.server ? ServerLOS.instance.isCouldSee(player, this.current) : this.current.isCouldSee(playerIndex);
        boolean bl = canSee = GameServer.server ? couldSee : this.current.isCanSee(playerIndex);
        if (!canSee && (isoGameCharacter = this) instanceof IsoZombie && (zombie = (IsoZombie)isoGameCharacter).canSeeHeadSquare(player)) {
            canSee = true;
        }
        if (!canSee && couldSee) {
            boolean bl2 = canSee = dist < player.getSeeNearbyCharacterDistance();
        }
        if (!canSee) {
            return false;
        }
        ColorInfo lightInfo = this.getCurrentSquare().lighting[playerIndex].lightInfo();
        if (lightInfo == null) {
            return false;
        }
        float delta = (lightInfo.r + lightInfo.g + lightInfo.b) / 3.0f;
        if (dist < player.getSeeNearbyCharacterDistance() && player.getCurrentSquare() != null && (lightInfo = player.getCurrentSquare().lighting[playerIndex].lightInfo()) != null) {
            delta = PZMath.max(delta, (lightInfo.r + lightInfo.g + lightInfo.b) / 3.0f);
        }
        if (delta > 0.6f) {
            delta = 1.0f;
        }
        float delta2 = 1.0f - dist / GameTime.getInstance().getViewDist();
        if (delta == 1.0f && delta2 > 0.3f) {
            delta2 = 1.0f;
        }
        if ((angle = player.getDotWithForwardDirection(this.getX(), this.getY())) < 0.5f) {
            angle = 0.5f;
        }
        if ((delta *= angle) < 0.0f) {
            delta = 0.0f;
        }
        if (dist <= 1.0f) {
            delta2 = 1.0f;
            delta *= 2.0f;
        }
        delta *= delta2;
        return (delta *= 100.0f) > 0.025f;
    }

    public void clearFallDamage() {
        this.fallDamage.reset();
    }

    public float getImpactIsoSpeed() {
        return this.fallDamage.getImpactIsoSpeed();
    }

    public void DoLand(float impactIsoSpeed) {
        if (this.isClimbing() || this.isRagdollFall()) {
            this.fallDamage.reset();
            return;
        }
        boolean isFall = FallingConstants.isFall(impactIsoSpeed);
        if (!isFall) {
            return;
        }
        this.fallDamage.setLandingImpact(impactIsoSpeed);
        DebugType.FallDamage.debugln("DoLand %s speed: %s u/s %f m/s, %f km/h", new Object[]{FallingConstants.getFallSeverity(impactIsoSpeed), Float.valueOf(impactIsoSpeed), Float.valueOf(impactIsoSpeed * 2.44949f), Float.valueOf(impactIsoSpeed * 3.6f * 2.44949f)});
        this.handleLandingImpact(this.fallDamage);
    }

    protected void handleLandingImpact(FallDamage fallDamage) {
        boolean injury;
        boolean fatal;
        float healthReductionRaw;
        boolean isDamagingFall = fallDamage.isDamagingFall();
        boolean isMoreThanLightFall = fallDamage.isMoreThanLightFall();
        boolean isMoreThanHardFall = fallDamage.isMoreThanHardFall();
        boolean isLethalFall = fallDamage.isLethalFall();
        boolean bWasAlive = this.isAlive();
        if (!isDamagingFall || this.isClimbing()) {
            return;
        }
        boolean unscratch = Rand.NextBool(80 - this.getPerkLevel(PerkFactory.Perks.Nimble));
        float damageAlpha = fallDamage.getImpactIsoSpeed() / FallingConstants.hardFallThreshold;
        float healthReduction = healthReductionRaw = PZMath.lerpFunc_EaseOutQuad(damageAlpha);
        healthReduction *= 115.0f;
        float randomizer = Rand.Next(0.5f, 1.0f);
        healthReduction *= randomizer;
        float weightMultiplier = this.getInventory().getCapacityWeight() / this.getInventory().getMaxWeight();
        weightMultiplier = Math.min(1.8f, weightMultiplier);
        healthReduction *= weightMultiplier;
        if (this.getCurrentSquare().getFloor() != null && this.getCurrentSquare().getFloor().getSprite().getName() != null && this.getCurrentSquare().getFloor().getSprite().getName().startsWith("blends_natural")) {
            healthReduction *= 0.8f;
            if (!unscratch) {
                unscratch = Rand.NextBool(65 - this.getPerkLevel(PerkFactory.Perks.Nimble));
            }
        }
        if (this.characterTraits.get(CharacterTrait.OBESE) || this.characterTraits.get(CharacterTrait.EMACIATED)) {
            healthReduction *= 1.4f;
        } else if (this.characterTraits.get(CharacterTrait.OVERWEIGHT) || this.characterTraits.get(CharacterTrait.VERY_UNDERWEIGHT)) {
            healthReduction *= 1.2f;
        }
        healthReduction *= Math.max(0.1f, 1.0f - (float)this.getPerkLevel(PerkFactory.Perks.Fitness) * 0.05f);
        healthReduction *= Math.max(0.1f, 1.0f - (float)this.getPerkLevel(PerkFactory.Perks.Nimble) * 0.05f);
        boolean bl = fatal = isLethalFall && !unscratch;
        if (fatal) {
            healthReduction = 1000.0f;
        } else if (isLethalFall) {
            healthReduction *= 0.05f;
        } else if (unscratch) {
            healthReduction = 0.0f;
        }
        if (isMoreThanLightFall) {
            this.fallenOnKnees(true);
            this.dropHandItems();
        } else {
            this.helmetFall(false);
        }
        BodyDamage bodyDamage = this.getBodyDamage();
        for (BodyPart bodyPart : bodyDamage.getBodyParts()) {
            healthReduction *= FallingWhileInjured.getDamageMultiplier(bodyPart);
        }
        if (healthReduction <= 0.0f) {
            return;
        }
        bodyDamage.ReduceGeneralHealth(healthReduction);
        if (healthReduction > 0.0f) {
            DebugType.FallDamage.debugln("Impact health reduction: %f. isAlive: %s", Float.valueOf(healthReduction), !this.isDead());
            bodyDamage.Update();
            this.setKilledByFall(bWasAlive && this.isDead());
        }
        LuaEventManager.triggerEvent("OnPlayerGetDamage", this, "FALLDOWN", Float.valueOf(healthReduction));
        if (healthReduction > 5.0f && this.isAlive()) {
            this.playPainVoicesFromFallDamage(fallDamage);
        }
        boolean bl2 = injury = isDamagingFall && (float)Rand.Next(100) < healthReduction;
        if (injury) {
            int rand = (int)(healthReductionRaw * 55.0f);
            if (this.getInventory().getMaxWeight() - this.getInventory().getCapacityWeight() < 2.0f) {
                rand = (int)((float)rand + this.getInventory().getCapacityWeight() / this.getInventory().getMaxWeight() * 20.0f);
            }
            if (this.characterTraits.get(CharacterTrait.OBESE) || this.characterTraits.get(CharacterTrait.EMACIATED)) {
                rand += 20;
            } else if (this.characterTraits.get(CharacterTrait.OVERWEIGHT) || this.characterTraits.get(CharacterTrait.VERY_UNDERWEIGHT)) {
                rand += 10;
            }
            if (this.getPerkLevel(PerkFactory.Perks.Fitness) > 4) {
                rand = (int)((double)rand - (double)(this.getPerkLevel(PerkFactory.Perks.Fitness) - 4) * 1.5);
            }
            rand = (int)((double)rand - (double)this.getPerkLevel(PerkFactory.Perks.Nimble) * 1.5);
            BodyPartType bodyPartType = isMoreThanHardFall || isMoreThanLightFall && Rand.NextBool(2) ? BodyPartType.FromIndex(Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.Foot_R) + 1)) : BodyPartType.FromIndex(Rand.Next(BodyPartType.ToIndex(BodyPartType.UpperLeg_L), BodyPartType.ToIndex(BodyPartType.Foot_R) + 1));
            if (Rand.Next(100) < rand && SandboxOptions.instance.boneFracture.getValue()) {
                DebugType.FallDamage.debugln("Impact fracture likely: %s. rand: %d", new Object[]{bodyPartType, rand});
                bodyDamage.getBodyPart(bodyPartType).generateFractureNew(Rand.Next(50, 80));
            } else if (Rand.Next(100) < rand + 10) {
                DebugType.FallDamage.debugln("Impact deep wound likely: %s. rand: %d", new Object[]{bodyPartType, rand});
                bodyDamage.getBodyPart(bodyPartType).generateDeepWound();
            } else {
                DebugType.FallDamage.debugln("Impact stiffness likely: %s. rand: %d", new Object[]{bodyPartType, rand});
                bodyDamage.getBodyPart(bodyPartType).setStiffness(100.0f);
            }
            if (isMoreThanHardFall) {
                bodyPartType = BodyPartType.FromIndex(Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.Foot_R) + 1));
                if (Rand.Next(100) < rand && SandboxOptions.instance.boneFracture.getValue()) {
                    DebugType.FallDamage.debugln("Severe Impact. Fracture likely: %s. rand: %d", new Object[]{bodyPartType, rand});
                    bodyDamage.getBodyPart(bodyPartType).generateFractureNew(Rand.Next(50, 80));
                } else if (Rand.Next(100) < rand + 10) {
                    DebugType.FallDamage.debugln("Severe Impact. Deep wound likely: %s. rand: %d", new Object[]{bodyPartType, rand});
                    bodyDamage.getBodyPart(bodyPartType).generateDeepWound();
                } else {
                    DebugType.FallDamage.debugln("Severe Impact. Stiffness likely: %s. rand: %d", new Object[]{bodyPartType, rand});
                    bodyDamage.getBodyPart(bodyPartType).setStiffness(100.0f);
                }
            }
        }
        if (fatal) {
            this.splatBloodFloorBig();
            this.splatBloodFloorBig();
            this.splatBloodFloorBig();
            this.splatBloodFloorBig();
        }
    }

    protected void playPainVoicesFromFallDamage(FallDamage fallDamage) {
    }

    public <T> PZArrayList<ItemContainer> getContextWorldContainers(T paramToCompare, Invokers.Params2.Boolean.ICallback<T, ItemContainer> isValidPredicate) {
        PZArrayList<ItemContainer> containerList = new PZArrayList<ItemContainer>(ItemContainer.class, 10);
        return this.getContextWorldContainers(paramToCompare, isValidPredicate, containerList);
    }

    public <T> PZArrayList<ItemContainer> getContextWorldContainers(T paramToCompare, Invokers.Params2.Boolean.ICallback<T, ItemContainer> isValidPredicate, PZArrayList<ItemContainer> containerList) {
        this.square.getAllContainers(paramToCompare, isValidPredicate, containerList);
        IsoDirections forwardDir = this.getForwardMovementIsoDirection();
        this.square.getAllContainersFromAdjacentSquare(forwardDir, paramToCompare, isValidPredicate, containerList);
        IsoDirections forwardDirL = forwardDir.RotLeft();
        this.square.getAllContainersFromAdjacentSquare(forwardDirL, paramToCompare, isValidPredicate, containerList);
        IsoDirections forwardDirR = forwardDir.RotRight();
        this.square.getAllContainersFromAdjacentSquare(forwardDirR, paramToCompare, isValidPredicate, containerList);
        IsoDirections forwardDirLL = forwardDirL.RotLeft();
        this.square.getAllContainersFromAdjacentSquare(forwardDirLL, paramToCompare, isValidPredicate, containerList);
        IsoDirections forwardDirRR = forwardDirR.RotRight();
        this.square.getAllContainersFromAdjacentSquare(forwardDirRR, paramToCompare, isValidPredicate, containerList);
        return containerList;
    }

    public <T> PZArrayList<ItemContainer> getContextWorldContainersInObjects(IsoObject[] contextObjects, T paramToCompare, Invokers.Params2.Boolean.ICallback<T, ItemContainer> isValidPredicate, PZArrayList<ItemContainer> containerList) {
        DebugType.General.noise("Getting ContextWorld Containers");
        if (contextObjects == null) {
            return containerList;
        }
        for (int i = 0; i < contextObjects.length; ++i) {
            IsoObject obj = contextObjects[i];
            if (obj == null) continue;
            if (obj == this) {
                DebugType.General.noise("Found self in context list, searching surrounding area...");
                this.getContextWorldContainers(paramToCompare, isValidPredicate, containerList);
                continue;
            }
            obj.getContainers(paramToCompare, isValidPredicate, containerList);
        }
        return containerList;
    }

    public PZArrayList<ItemContainer> getContextWorldSuitableContainersToDropCorpseInObjects(IsoObject[] contextObjects) {
        return this.getContextWorldContainersInObjects(contextObjects, this, IsoGameCharacter::canDropCorpseInto, new PZArrayList<ItemContainer>(ItemContainer.class, 10));
    }

    public PZArrayList<ItemContainer> getSuitableContainersToDropCorpseInSquare(IsoGridSquare square) {
        return this.getSuitableContainersToDropCorpseInSquare(square, new PZArrayList<ItemContainer>(ItemContainer.class, 10));
    }

    public PZArrayList<ItemContainer> getSuitableContainersToDropCorpseInSquare(IsoGridSquare square, PZArrayList<ItemContainer> foundContainers) {
        if (square == null) {
            return foundContainers;
        }
        if (this.square == square) {
            return this.getSuitableContainersToDropCorpse(foundContainers);
        }
        return square.getAllContainers(this, IsoGameCharacter::canDropCorpseInto, foundContainers);
    }

    public PZArrayList<ItemContainer> getSuitableContainersToDropCorpse() {
        return this.getSuitableContainersToDropCorpse(new PZArrayList<ItemContainer>(ItemContainer.class, 10));
    }

    public PZArrayList<ItemContainer> getSuitableContainersToDropCorpse(PZArrayList<ItemContainer> foundContainers) {
        this.getContextWorldContainers(this, IsoGameCharacter::canDropCorpseInto, foundContainers);
        return foundContainers;
    }

    public PZArrayList<ItemContainer> getContextWorldContainersWithHumanCorpse(IsoObject[] contextObjects) {
        return this.getContextWorldContainersInObjects(contextObjects, this, IsoGameCharacter::canGrabCorpseFrom, new PZArrayList<ItemContainer>(ItemContainer.class, 10));
    }

    public PZArrayList<ItemContainer> getSuitableContainersWithHumanCorpseInSquare(IsoGridSquare square) {
        return this.getSuitableContainersWithHumanCorpseInSquare(square, new PZArrayList<ItemContainer>(ItemContainer.class, 10));
    }

    public PZArrayList<ItemContainer> getSuitableContainersWithHumanCorpseInSquare(IsoGridSquare square, PZArrayList<ItemContainer> foundContainers) {
        return square.getAllContainers(this, IsoGameCharacter::canGrabCorpseFrom, foundContainers);
    }

    public static boolean canDropCorpseInto(IsoGameCharacter chr, ItemContainer container) {
        if (chr == null) {
            return false;
        }
        if (container == null) {
            return false;
        }
        return container.canHumanCorpseFit();
    }

    public static boolean canGrabCorpseFrom(IsoGameCharacter chr, ItemContainer container) {
        if (chr == null) {
            return false;
        }
        if (container == null) {
            return false;
        }
        return container.containsHumanCorpse();
    }

    public boolean canAccessContainer(ItemContainer container) {
        if (container.doesVehicleDoorNeedOpening()) {
            return container.canCharacterOpenVehicleDoor(this);
        }
        return true;
    }

    public String getContainerToolTip(ItemContainer container) {
        if (container.doesVehicleDoorNeedOpening()) {
            if (!container.canCharacterOpenVehicleDoor(this)) {
                return "IGUI_Tooltip_VehicleDoorLocked";
            }
            return "IGUI_Tooltip_DoorClosed";
        }
        return "";
    }

    public IsoGameCharacter getFollowingTarget() {
        return this.followingTarget;
    }

    public void setFollowingTarget(IsoGameCharacter followingTarget) {
        this.followingTarget = followingTarget;
    }

    public ArrayList<IsoMovingObject> getLocalList() {
        return this.localList;
    }

    public ArrayList<IsoMovingObject> getLocalNeutralList() {
        return this.localNeutralList;
    }

    public ArrayList<IsoMovingObject> getLocalGroupList() {
        return this.localGroupList;
    }

    public ArrayList<IsoMovingObject> getLocalRelevantEnemyList() {
        return this.localRelevantEnemyList;
    }

    public float getDangerLevels() {
        return this.dangerLevels;
    }

    public void setDangerLevels(float dangerLevels) {
        this.dangerLevels = dangerLevels;
    }

    public ArrayList<PerkInfo> getPerkList() {
        return this.perkList;
    }

    public float getLeaveBodyTimedown() {
        return this.leaveBodyTimedown;
    }

    public void setLeaveBodyTimedown(float leaveBodyTimedown) {
        this.leaveBodyTimedown = leaveBodyTimedown;
    }

    public boolean isAllowConversation() {
        return this.allowConversation;
    }

    public void setAllowConversation(boolean allowConversation) {
        this.allowConversation = allowConversation;
    }

    public float getReanimateTimer() {
        return this.reanimateTimer;
    }

    public void setReanimateTimer(float reanimateTimer) {
        this.reanimateTimer = reanimateTimer;
    }

    public int getReanimAnimFrame() {
        return this.reanimAnimFrame;
    }

    public void setReanimAnimFrame(int reanimAnimFrame) {
        this.reanimAnimFrame = reanimAnimFrame;
    }

    public int getReanimAnimDelay() {
        return this.reanimAnimDelay;
    }

    public void setReanimAnimDelay(int reanimAnimDelay) {
        this.reanimAnimDelay = reanimAnimDelay;
    }

    public boolean isReanim() {
        return this.reanim;
    }

    public void setReanim(boolean reanim) {
        this.reanim = reanim;
    }

    public boolean isVisibleToNPCs() {
        return this.visibleToNpcs;
    }

    public void setVisibleToNPCs(boolean visibleToNpcs) {
        this.visibleToNpcs = visibleToNpcs;
    }

    public int getDieCount() {
        return this.dieCount;
    }

    public void setDieCount(int dieCount) {
        this.dieCount = dieCount;
    }

    public float getLlx() {
        return this.llx;
    }

    public void setLlx(float llx) {
        this.llx = llx;
    }

    public float getLly() {
        return this.lly;
    }

    public void setLly(float lly) {
        this.lly = lly;
    }

    public float getLlz() {
        return this.llz;
    }

    public void setLlz(float llz) {
        this.llz = llz;
    }

    public int getRemoteID() {
        return this.remoteId;
    }

    public void setRemoteID(int remoteId) {
        this.remoteId = remoteId;
    }

    public int getNumSurvivorsInVicinity() {
        return this.numSurvivorsInVicinity;
    }

    public void setNumSurvivorsInVicinity(int numSurvivorsInVicinity) {
        this.numSurvivorsInVicinity = numSurvivorsInVicinity;
    }

    public float getLevelUpMultiplier() {
        return this.levelUpMultiplier;
    }

    public void setLevelUpMultiplier(float levelUpMultiplier) {
        this.levelUpMultiplier = levelUpMultiplier;
    }

    @Override
    public XP getXp() {
        return this.xp;
    }

    @Deprecated
    public void setXp(XP xp) {
        this.xp = xp;
    }

    public int getLastLocalEnemies() {
        return this.lastLocalEnemies;
    }

    public void setLastLocalEnemies(int lastLocalEnemies) {
        this.lastLocalEnemies = lastLocalEnemies;
    }

    public ArrayList<IsoMovingObject> getVeryCloseEnemyList() {
        return this.veryCloseEnemyList;
    }

    public HashMap<String, Location> getLastKnownLocation() {
        return this.lastKnownLocation;
    }

    public IsoGameCharacter getAttackedBy() {
        return this.attackedBy;
    }

    public void setAttackedBy(IsoGameCharacter attackedBy) {
        this.attackedBy = attackedBy;
    }

    public boolean isIgnoreStaggerBack() {
        return this.ignoreStaggerBack;
    }

    public void setIgnoreStaggerBack(boolean ignoreStaggerBack) {
        this.ignoreStaggerBack = ignoreStaggerBack;
    }

    public int getTimeThumping() {
        return this.timeThumping;
    }

    public void setTimeThumping(int timeThumping) {
        this.timeThumping = timeThumping;
    }

    public int getPatienceMax() {
        return this.patienceMax;
    }

    public void setPatienceMax(int patienceMax) {
        this.patienceMax = patienceMax;
    }

    public int getPatienceMin() {
        return this.patienceMin;
    }

    public void setPatienceMin(int patienceMin) {
        this.patienceMin = patienceMin;
    }

    public int getPatience() {
        return this.patience;
    }

    public void setPatience(int patience) {
        this.patience = patience;
    }

    @Override
    public Stack<BaseAction> getCharacterActions() {
        return this.characterActions;
    }

    public boolean hasTimedActions() {
        return !this.characterActions.isEmpty() || this.getVariableBoolean("IsPerformingAnAction");
    }

    public boolean isCurrentActionPathfinding() {
        return this.checkCurrentAction(BaseAction::isPathfinding);
    }

    public boolean isCurrentActionAllowedWhileDraggingCorpses() {
        return this.checkCurrentAction(BaseAction::isAllowedWhileDraggingCorpses);
    }

    public boolean checkCurrentAction(Invokers.Params1.Boolean.ICallback<BaseAction> checkPredicate) {
        if (this.characterActions.isEmpty()) {
            return false;
        }
        BaseAction action = (BaseAction)this.characterActions.get(0);
        return action != null && checkPredicate.accept(action);
    }

    public boolean isImpactFromBehind(Vector2 impactDir) {
        return this.isImpactFromBehind(impactDir.x, impactDir.y);
    }

    public boolean isImpactFromBehind(float impactDirX, float impactDirY) {
        return IsoGameCharacter.isImpactFromBehind(this.forwardDirection.x, this.forwardDirection.y, impactDirX, impactDirY);
    }

    public static boolean isImpactFromBehind(float chrForwardX, float chrForwardY, float impactDirX, float impactDirY) {
        float impactDirNY;
        float impactDirLenSq = impactDirX * impactDirX + impactDirY * impactDirY;
        if (impactDirLenSq < 1.0E-7f) {
            return false;
        }
        float maxFromBehindDot = 0.5f;
        if (impactDirLenSq >= 0.999f && impactDirLenSq <= 0.999f) {
            float dot = chrForwardX * impactDirX + chrForwardY * impactDirY;
            return dot >= 0.5f;
        }
        float impactDirLen = PZMath.sqrt(impactDirLenSq);
        float impactDirNX = impactDirX / impactDirLen;
        float dotN = chrForwardX * impactDirNX + chrForwardY * (impactDirNY = impactDirY / impactDirLen);
        return dotN >= 0.5f;
    }

    @Deprecated
    public Vector2 getForwardDirection() {
        return this.forwardDirection;
    }

    public float getForwardDirectionX() {
        return this.forwardDirection.x;
    }

    public float getForwardDirectionY() {
        return this.forwardDirection.y;
    }

    public Vector2 getForwardDirection(Vector2 forwardDirection) {
        forwardDirection.x = this.forwardDirection.x;
        forwardDirection.y = this.forwardDirection.y;
        return forwardDirection;
    }

    public void setForwardDirection(Vector2 dir) {
        if (dir == null) {
            return;
        }
        this.setForwardDirection(dir.x, dir.y);
    }

    @Override
    public void setTargetAndCurrentDirection(float directionX, float directionY) {
        this.setForwardDirection(directionX, directionY);
        if (this.hasAnimationPlayer()) {
            this.getAnimationPlayer().setTargetAndCurrentDirection(directionX, directionY);
        }
    }

    @Override
    public void setForwardDirection(float directionX, float directionY) {
        this.forwardDirection.x = directionX;
        this.forwardDirection.y = directionY;
        float forwardDirectionLength = this.forwardDirection.normalize();
        this.dir = IsoDirections.fromAngle(directionX, directionY);
        if (PZMath.equal(forwardDirectionLength, 0.0f)) {
            throw new IllegalStateException("Forward Direction cannot be zero length vector.");
        }
    }

    public void zeroForwardDirectionX() {
        this.setForwardDirection(0.0f, 1.0f);
    }

    public void zeroForwardDirectionY() {
        this.setForwardDirection(1.0f, 0.0f);
    }

    public float getDirectionAngleRadians() {
        return this.forwardDirection.getDirection();
    }

    public float getDirectionAngle() {
        return 57.295776f * this.getDirectionAngleRadians();
    }

    public void setDirectionAngle(float angleDegrees) {
        float angleRads = (float)Math.PI / 180 * angleDegrees;
        float x = (float)Math.cos(angleRads);
        float y = (float)Math.sin(angleRads);
        this.setForwardDirection(x, y);
    }

    public float getAnimAngle() {
        if (this.animPlayer == null || !this.animPlayer.isReady() || this.animPlayer.isBoneTransformsNeedFirstFrame()) {
            return this.getDirectionAngle();
        }
        return 57.295776f * this.animPlayer.getAngle();
    }

    public float getAnimAngleRadians() {
        if (this.animPlayer == null || !this.animPlayer.isReady() || this.animPlayer.isBoneTransformsNeedFirstFrame()) {
            return this.forwardDirection.getDirection();
        }
        return this.animPlayer.getAngle();
    }

    @Deprecated
    public Vector2 getAnimVector(Vector2 animForwardDirection) {
        return this.getAnimForwardDirection(animForwardDirection);
    }

    @Override
    public Vector2 getAnimForwardDirection(Vector2 forwardDirection) {
        return forwardDirection.setLengthAndDirection(this.getAnimAngleRadians(), 1.0f);
    }

    public float getLookAngleRadians() {
        if (this.animPlayer == null || !this.animPlayer.isReady()) {
            return this.getDirectionAngleRadians();
        }
        float angle = this.animPlayer.getAngle() + this.animPlayer.getTwistAngle();
        if (this.isHeadLookAround()) {
            angle += this.getHeadLookHorizontal();
        }
        return angle;
    }

    public Vector2 getLookVector(Vector2 vector2) {
        return vector2.setLengthAndDirection(this.getLookAngleRadians(), 1.0f);
    }

    public float getLookDirectionX() {
        Vector2 lookVector = BaseVehicle.allocVector2();
        float x = this.getLookVector((Vector2)lookVector).x;
        BaseVehicle.releaseVector2(lookVector);
        return x;
    }

    public float getLookDirectionY() {
        Vector2 lookVector = BaseVehicle.allocVector2();
        float y = this.getLookVector((Vector2)lookVector).y;
        BaseVehicle.releaseVector2(lookVector);
        return y;
    }

    public boolean isAnimatingBackwards() {
        return this.isAnimatingBackwards;
    }

    @Override
    public IsoDirections getForwardMovementIsoDirection() {
        if (this.isAnimatingBackwards()) {
            this.getForwardIsoDirection().Rot180();
        }
        return this.getForwardIsoDirection();
    }

    public void setAnimatingBackwards(boolean isAnimatingBackwards) {
        this.isAnimatingBackwards = isAnimatingBackwards;
    }

    public boolean isDraggingCorpse() {
        if (!this.isGrappling()) {
            return false;
        }
        IGrappleable grappledTarget = this.getGrapplingTarget();
        if (!(grappledTarget instanceof IsoZombie)) {
            return false;
        }
        IsoZombie grappledZombie = (IsoZombie)grappledTarget;
        return grappledZombie.isReanimatedForGrappleOnly();
    }

    public UdpConnection getOwner() {
        return null;
    }

    public void setOwner(UdpConnection connection) {
    }

    public IsoPlayer getOwnerPlayer() {
        return null;
    }

    public void setOwnerPlayer(IsoPlayer player) {
    }

    public float getDotWithForwardDirection(Vector3 bonePos) {
        return this.getDotWithForwardDirection(bonePos.x, bonePos.y);
    }

    public float getDotWithForwardDirection(float targetX, float targetY) {
        Vector2 vectorToTarget = L_getDotWithForwardDirection.v1.set(targetX - this.getX(), targetY - this.getY());
        vectorToTarget.normalize();
        Vector2 forward = this.getLookVector(L_getDotWithForwardDirection.v2);
        forward.normalize();
        return vectorToTarget.dot(forward);
    }

    @Override
    public boolean isAsleep() {
        return this.asleep;
    }

    @Override
    public void setAsleep(boolean asleep) {
        this.asleep = asleep;
    }

    @Override
    public boolean isResting() {
        return this.isResting;
    }

    @Override
    public void setIsResting(boolean isResting) {
        this.isResting = isResting;
    }

    @Override
    public int getZombieKills() {
        return this.zombieKills;
    }

    public void setZombieKills(int zombieKills) {
        this.zombieKills = zombieKills;
        if (GameServer.server && this instanceof IsoPlayer) {
            SteamGameServer.UpdatePlayer((IsoPlayer)this);
        }
    }

    public int getLastZombieKills() {
        return this.lastZombieKills;
    }

    public void setLastZombieKills(int lastZombieKills) {
        this.lastZombieKills = lastZombieKills;
    }

    public float getForceWakeUpTime() {
        return this.forceWakeUpTime;
    }

    @Override
    public void setForceWakeUpTime(float forceWakeUpTime) {
        this.forceWakeUpTime = forceWakeUpTime;
    }

    public void forceAwake() {
        if (this.isAsleep()) {
            this.forceWakeUp = true;
        }
    }

    @Override
    public BodyDamage getBodyDamage() {
        return this.bodyDamage;
    }

    @Override
    public BodyDamage getBodyDamageRemote() {
        if (this.bodyDamageRemote == null) {
            this.bodyDamageRemote = new BodyDamage(this);
        }
        return this.bodyDamageRemote;
    }

    public void resetBodyDamageRemote() {
        this.bodyDamageRemote = null;
    }

    public State getDefaultState() {
        return this.defaultState;
    }

    public void setDefaultState(State defaultState) {
        this.defaultState = defaultState;
    }

    @Override
    public SurvivorDesc getDescriptor() {
        return this.descriptor;
    }

    @Override
    public void setDescriptor(SurvivorDesc descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public String getFullName() {
        if (this.descriptor != null) {
            return this.descriptor.getForename() + " " + this.descriptor.getSurname();
        }
        return "Bob Smith";
    }

    @Override
    public BaseVisual getVisual() {
        throw new RuntimeException("subclasses must implement this");
    }

    public ItemVisuals getItemVisuals() {
        throw new RuntimeException("subclasses must implement this");
    }

    public void getItemVisuals(ItemVisuals itemVisuals) {
        this.getWornItems().getItemVisuals(itemVisuals);
    }

    public boolean isUsingWornItems() {
        return this.wornItems != null;
    }

    public Stack<IsoBuilding> getFamiliarBuildings() {
        return this.familiarBuildings;
    }

    public AStarPathFinderResult getFinder() {
        return this.finder;
    }

    public float getFireKillRate() {
        return this.fireKillRate;
    }

    public void setFireKillRate(float fireKillRate) {
        this.fireKillRate = fireKillRate;
    }

    public int getFireSpreadProbability() {
        return this.fireSpreadProbability;
    }

    public void setFireSpreadProbability(int fireSpreadProbability) {
        this.fireSpreadProbability = fireSpreadProbability;
    }

    @Override
    public float getHealth() {
        return this.health;
    }

    @Override
    public void setHealth(float health) {
        if (health == 0.0f && this.isInvulnerable()) {
            return;
        }
        this.health = health;
    }

    @Override
    public boolean isOnDeathDone() {
        return this.dead;
    }

    @Override
    public void setOnDeathDone(boolean done) {
        this.dead = done;
    }

    @Override
    public boolean isOnKillDone() {
        return this.kill;
    }

    @Override
    public void setOnKillDone(boolean done) {
        this.kill = done;
    }

    @Override
    public boolean isDeathDragDown() {
        return this.deathDragDown;
    }

    @Override
    public void setDeathDragDown(boolean dragDown) {
        this.deathDragDown = dragDown;
    }

    @Override
    public boolean isPlayingDeathSound() {
        return this.playingDeathSound;
    }

    @Override
    public void setPlayingDeathSound(boolean playing) {
        this.playingDeathSound = playing;
    }

    public String getHurtSound() {
        return this.hurtSound;
    }

    public void setHurtSound(String hurtSound) {
        this.hurtSound = hurtSound;
    }

    @Deprecated
    public boolean isIgnoreMovementForDirection() {
        return false;
    }

    @Override
    public ItemContainer getInventory() {
        return this.inventory;
    }

    public void setInventory(ItemContainer inventory) {
        inventory.parent = this;
        this.inventory = inventory;
        this.inventory.setExplored(true);
    }

    public boolean isPrimaryEquipped(String item) {
        if (this.leftHandItem == null) {
            return false;
        }
        return this.leftHandItem.getFullType().equals(item) || this.leftHandItem.getType().equals(item);
    }

    @Override
    public InventoryItem getPrimaryHandItem() {
        return this.leftHandItem;
    }

    @Override
    public void setPrimaryHandItem(InventoryItem leftHandItem) {
        BallisticsController ballisticsController;
        if (this.leftHandItem == leftHandItem) {
            return;
        }
        if (leftHandItem == null && this.getPrimaryHandItem() instanceof AnimalInventoryItem) {
            ((AnimalInventoryItem)this.getPrimaryHandItem()).getAnimal().heldBy = null;
        }
        if (this instanceof IsoPlayer && leftHandItem == null && !((IsoPlayer)this).getAttachedAnimals().isEmpty() && this.getPrimaryHandItem() != null && this.getPrimaryHandItem().getType().equalsIgnoreCase("Rope")) {
            ((IsoPlayer)this).removeAllAttachedAnimals();
        }
        if (leftHandItem == this.getSecondaryHandItem()) {
            this.setEquipParent(this.leftHandItem, leftHandItem, false);
        } else {
            this.setEquipParent(this.leftHandItem, leftHandItem);
        }
        if (leftHandItem instanceof HandWeapon) {
            HandWeapon handWeapon = (HandWeapon)leftHandItem;
            this.setUseHandWeapon(handWeapon);
        } else {
            this.setUseHandWeapon(null);
        }
        if ((this.leftHandItem instanceof HandWeapon || leftHandItem instanceof HandWeapon) && (ballisticsController = this.getBallisticsController()) != null) {
            ballisticsController.clearCacheTargets();
        }
        this.leftHandItem = leftHandItem;
        this.handItemShouldSendToClients = true;
        LuaEventManager.triggerEvent("OnEquipPrimary", this, leftHandItem);
        this.resetEquippedHandsModels();
        this.setVariable("Weapon", WeaponType.getWeaponType(this).getType());
        if (leftHandItem instanceof AnimalInventoryItem) {
            AnimalInventoryItem animalInventoryItem = (AnimalInventoryItem)leftHandItem;
            if (this instanceof IsoPlayer) {
                animalInventoryItem.getAnimal().heldBy = (IsoPlayer)this;
            }
        }
    }

    public HandWeapon getAttackingWeapon() {
        return this.getUseHandWeapon();
    }

    protected void setEquipParent(InventoryItem handItem, InventoryItem newHandItem) {
        this.setEquipParent(handItem, newHandItem, true);
    }

    protected void setEquipParent(InventoryItem handItem, InventoryItem newHandItem, boolean register) {
        if (handItem != null) {
            handItem.setEquipParent(null, register);
        }
        if (newHandItem != null) {
            newHandItem.setEquipParent(this, register);
        }
    }

    public void initWornItems(String bodyLocationGroupName) {
        BodyLocationGroup bodyLocationGroup = BodyLocations.getGroup(bodyLocationGroupName);
        this.wornItems = new WornItems(bodyLocationGroup);
        this.onWornItemsChanged();
    }

    @Override
    public WornItems getWornItems() {
        return this.wornItems;
    }

    @Override
    public void setWornItems(WornItems other) {
        this.wornItems = new WornItems(other);
        this.onWornItemsChanged();
    }

    @Override
    public InventoryItem getWornItem(ItemBodyLocation itemBodyLocation) {
        return this.wornItems.getItem(itemBodyLocation);
    }

    @Override
    public void setWornItem(ItemBodyLocation location, InventoryItem item) {
        this.setWornItem(location, item, true);
    }

    public void setWornItem(ItemBodyLocation location, InventoryItem item, boolean forceDropTooHeavy) {
        InventoryItem itemCur = this.wornItems.getItem(location);
        if (item == itemCur) {
            return;
        }
        IsoCell cell = IsoWorld.instance.currentCell;
        if (itemCur != null && cell != null) {
            cell.addToProcessItemsRemove(itemCur);
        }
        this.wornItems.setItem(location, item);
        if (item != null && cell != null) {
            if (item.getContainer() != null) {
                item.getContainer().parent = this;
            }
            cell.addToProcessItems(item);
        }
        if (forceDropTooHeavy && itemCur != null && this instanceof IsoPlayer && !this.getInventory().hasRoomFor(this, itemCur)) {
            IsoGridSquare sq = this.getCurrentSquare();
            sq = this.getSolidFloorAt(sq.x, sq.y, sq.z);
            if (sq != null) {
                float dropX = Rand.Next(0.1f, 0.9f);
                float dropY = Rand.Next(0.1f, 0.9f);
                float dropZ = sq.getApparentZ(dropX, dropY) - (float)sq.getZ();
                sq.AddWorldInventoryItem(itemCur, dropX, dropY, dropZ);
                this.getInventory().Remove(itemCur);
                if (GameServer.server) {
                    GameServer.sendRemoveItemFromContainer(this.getInventory(), itemCur);
                }
            }
        }
        if (this.isoPlayer != null && this.isoPlayer.getHumanVisual().getHairModel().contains("Mohawk") && (Objects.equals(location, ItemBodyLocation.HAT) || Objects.equals(location, ItemBodyLocation.FULL_HAT))) {
            this.isoPlayer.getHumanVisual().setHairModel("MohawkFlat");
            this.resetModel();
        }
        this.resetModelNextFrame();
        if (this.clothingWetness != null) {
            this.clothingWetness.changed = true;
        }
        if (this instanceof IsoPlayer) {
            if (GameServer.server) {
                INetworkPacket.sendToAll(PacketTypes.PacketType.SyncClothing, this);
                INetworkPacket.sendToAll(PacketTypes.PacketType.SyncVisuals, this);
            } else if (GameClient.client && GameClient.connection.isReady()) {
                INetworkPacket.send(PacketTypes.PacketType.SyncClothing, this);
            }
        }
        this.onWornItemsChanged();
    }

    @Override
    public void removeWornItem(InventoryItem item) {
        this.removeWornItem(item, true);
    }

    @Override
    public void removeWornItem(InventoryItem item, boolean forceDropTooHeavy) {
        this.setWornItem(this.wornItems.getLocation(item), null, forceDropTooHeavy);
    }

    @Override
    public void clearWornItems() {
        if (this.wornItems == null) {
            return;
        }
        this.wornItems.clear();
        if (this.clothingWetness != null) {
            this.clothingWetness.changed = true;
        }
        this.onWornItemsChanged();
    }

    @Override
    public BodyLocationGroup getBodyLocationGroup() {
        if (this.wornItems == null) {
            return null;
        }
        return this.wornItems.getBodyLocationGroup();
    }

    public void onWornItemsChanged() {
        boolean clothingCanRagdoll;
        boolean bl = clothingCanRagdoll = !this.hasWornTag(ItemTag.NO_RAGDOLL);
        if (this.wornClothingCanRagdoll != clothingCanRagdoll) {
            this.wornClothingCanRagdoll = clothingCanRagdoll;
            DebugLog.General.debugln("%s worn items changed. %s.", this.getName(), clothingCanRagdoll ? "Character's clothes can now ragdoll." : "Character's clothing prevent ragdolling.");
        }
        if (GameServer.server) {
            this.OnClothingUpdated();
        }
    }

    public void initAttachedItems(String groupName) {
        AttachedLocationGroup group = AttachedLocations.getGroup(groupName);
        this.attachedItems = new AttachedItems(group);
    }

    @Override
    public AttachedItems getAttachedItems() {
        return this.attachedItems;
    }

    @Override
    public void setAttachedItems(AttachedItems other) {
        this.attachedItems = new AttachedItems(other);
    }

    @Override
    public InventoryItem getAttachedItem(String location) {
        return this.attachedItems.getItem(location);
    }

    @Override
    public void setAttachedItem(String location, InventoryItem item) {
        InventoryItem itemCur = this.attachedItems.getItem(location);
        IsoCell cell = IsoWorld.instance.currentCell;
        if (itemCur != null && cell != null) {
            cell.addToProcessItemsRemove(itemCur);
        }
        this.attachedItems.setItem(location, item);
        if (item != null && cell != null) {
            InventoryContainer invContainer = Type.tryCastTo(item, InventoryContainer.class);
            if (invContainer != null && invContainer.getInventory() != null) {
                invContainer.getInventory().parent = this;
            }
            cell.addToProcessItems(item);
        }
        this.resetEquippedHandsModels();
        IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
        if (GameClient.client && player != null && player.isLocalPlayer() && !"bowtie".equals(location) && !"head_hat".equals(location)) {
            GameClient.instance.sendAttachedItem(player, location, item);
        }
        if (!GameServer.server && player != null && player.isLocalPlayer()) {
            LuaEventManager.triggerEvent("OnClothingUpdated", this);
        }
    }

    @Override
    public void removeAttachedItem(InventoryItem item) {
        String location = this.attachedItems.getLocation(item);
        if (location == null) {
            return;
        }
        this.setAttachedItem(location, null);
    }

    @Override
    public void clearAttachedItems() {
        if (this.attachedItems == null) {
            return;
        }
        this.attachedItems.clear();
    }

    @Override
    public AttachedLocationGroup getAttachedLocationGroup() {
        if (this.attachedItems == null) {
            return null;
        }
        return this.attachedItems.getGroup();
    }

    public ClothingWetness getClothingWetness() {
        return this.clothingWetness;
    }

    public ClothingWetnessSync getClothingWetnessSync() {
        return this.clothingWetnessSync;
    }

    public InventoryItem getClothingItem_Head() {
        return this.getWornItem(ItemBodyLocation.HAT);
    }

    @Override
    public void setClothingItem_Head(InventoryItem item) {
        this.setWornItem(ItemBodyLocation.HAT, item);
    }

    public InventoryItem getClothingItem_Torso() {
        return this.getWornItem(ItemBodyLocation.TSHIRT);
    }

    @Override
    public void setClothingItem_Torso(InventoryItem item) {
        this.setWornItem(ItemBodyLocation.TSHIRT, item);
    }

    public InventoryItem getClothingItem_Back() {
        return this.getWornItem(ItemBodyLocation.BACK);
    }

    @Override
    public void setClothingItem_Back(InventoryItem item) {
        this.setWornItem(ItemBodyLocation.BACK, item);
    }

    public InventoryItem getClothingItem_Hands() {
        return this.getWornItem(ItemBodyLocation.HANDS);
    }

    @Override
    public void setClothingItem_Hands(InventoryItem item) {
        this.setWornItem(ItemBodyLocation.HANDS, item);
    }

    public InventoryItem getClothingItem_Legs() {
        return this.getWornItem(ItemBodyLocation.PANTS);
    }

    @Override
    public void setClothingItem_Legs(InventoryItem item) {
        this.setWornItem(ItemBodyLocation.PANTS, item);
    }

    public InventoryItem getClothingItem_Feet() {
        return this.getWornItem(ItemBodyLocation.SHOES);
    }

    @Override
    public void setClothingItem_Feet(InventoryItem item) {
        this.setWornItem(ItemBodyLocation.SHOES, item);
    }

    public int getNextWander() {
        return this.nextWander;
    }

    public void setNextWander(int nextWander) {
        this.nextWander = nextWander;
    }

    @Override
    public boolean isOnFire() {
        return this.onFire;
    }

    public void setOnFire(boolean onFire) {
        this.onFire = onFire;
        if (GameServer.server) {
            if (onFire) {
                IsoFireManager.addCharacterOnFire(this);
            } else {
                IsoFireManager.deleteCharacterOnFire(this);
            }
        }
    }

    @Override
    public void removeFromWorld() {
        if (GameServer.server) {
            IsoFireManager.deleteCharacterOnFire(this);
        }
        super.removeFromWorld();
        this.releaseRagdollController();
        this.releaseBallisticsController();
        this.releaseBallisticsTarget();
        this.closeAnimationRecorder();
        if (GameClient.client && !this.isLocal()) {
            this.getNetworkCharacterAI().resetState();
        }
    }

    public int getPathIndex() {
        return this.pathIndex;
    }

    public void setPathIndex(int pathIndex) {
        this.pathIndex = pathIndex;
    }

    public int getPathTargetX() {
        return PZMath.fastfloor(this.getPathFindBehavior2().getTargetX());
    }

    public int getPathTargetY() {
        return PZMath.fastfloor(this.getPathFindBehavior2().getTargetY());
    }

    public int getPathTargetZ() {
        return PZMath.fastfloor(this.getPathFindBehavior2().getTargetZ());
    }

    @Override
    public InventoryItem getSecondaryHandItem() {
        return this.rightHandItem;
    }

    @Override
    public void setSecondaryHandItem(InventoryItem rightHandItem) {
        if (this.rightHandItem == rightHandItem) {
            return;
        }
        if (rightHandItem == this.getPrimaryHandItem()) {
            this.setEquipParent(this.rightHandItem, rightHandItem, false);
        } else {
            this.setEquipParent(this.rightHandItem, rightHandItem);
        }
        this.rightHandItem = rightHandItem;
        this.handItemShouldSendToClients = true;
        LuaEventManager.triggerEvent("OnEquipSecondary", this, rightHandItem);
        this.resetEquippedHandsModels();
        this.setVariable("Weapon", WeaponType.getWeaponType(this).getType());
    }

    @Override
    public boolean isHandItem(InventoryItem item) {
        return this.isPrimaryHandItem(item) || this.isSecondaryHandItem(item);
    }

    @Override
    public boolean isPrimaryHandItem(InventoryItem item) {
        return item != null && this.getPrimaryHandItem() == item;
    }

    @Override
    public boolean isSecondaryHandItem(InventoryItem item) {
        return item != null && this.getSecondaryHandItem() == item;
    }

    @Override
    public boolean isItemInBothHands(InventoryItem item) {
        return this.isPrimaryHandItem(item) && this.isSecondaryHandItem(item);
    }

    @Override
    public boolean removeFromHands(InventoryItem item) {
        boolean addToWorld = true;
        if (this.isPrimaryHandItem(item)) {
            this.setPrimaryHandItem(null);
        }
        if (this.isSecondaryHandItem(item)) {
            this.setSecondaryHandItem(null);
        }
        return true;
    }

    public Color getSpeakColour() {
        return this.speakColour;
    }

    public void setSpeakColour(Color speakColour) {
        this.speakColour = speakColour;
    }

    @Override
    public void setSpeakColourInfo(ColorInfo info) {
        this.speakColour = new Color(info.r, info.g, info.b, 1.0f);
    }

    public float getSlowFactor() {
        return this.slowFactor;
    }

    public void setSlowFactor(float slowFactor) {
        this.slowFactor = slowFactor;
    }

    public float getSlowTimer() {
        return this.slowTimer;
    }

    public void setSlowTimer(float slowTimer) {
        this.slowTimer = slowTimer;
    }

    public boolean isbUseParts() {
        return this.useParts;
    }

    public void setbUseParts(boolean useParts) {
        this.useParts = useParts;
    }

    @Override
    public boolean isSpeaking() {
        return this.IsSpeaking();
    }

    public void setSpeaking(boolean speaking) {
        this.speaking = speaking;
    }

    public float getSpeakTime() {
        return this.speakTime;
    }

    public void setSpeakTime(int speakTime) {
        this.speakTime = speakTime;
    }

    public float getSpeedMod() {
        return this.speedMod;
    }

    public void setSpeedMod(float speedMod) {
        this.speedMod = speedMod;
    }

    public float getStaggerTimeMod() {
        return this.staggerTimeMod;
    }

    public void setStaggerTimeMod(float staggerTimeMod) {
        this.staggerTimeMod = staggerTimeMod;
    }

    public StateMachine getStateMachine() {
        return this.stateMachine;
    }

    @Override
    public Moodles getMoodles() {
        return this.moodles;
    }

    @Override
    public Stats getStats() {
        return this.stats;
    }

    public Stack<String> getUsedItemsOn() {
        return this.usedItemsOn;
    }

    public HandWeapon getUseHandWeapon() {
        return this.useHandWeapon;
    }

    public void setUseHandWeapon(HandWeapon useHandWeapon) {
        this.useHandWeapon = useHandWeapon;
    }

    public IsoSprite getLegsSprite() {
        return this.legsSprite;
    }

    public void setLegsSprite(IsoSprite legsSprite) {
        this.legsSprite = legsSprite;
    }

    public IsoGridSquare getAttackTargetSquare() {
        return this.attackTargetSquare;
    }

    public void setAttackTargetSquare(IsoGridSquare attackTargetSquare) {
        this.attackTargetSquare = attackTargetSquare;
    }

    public float getBloodImpactX() {
        return this.bloodImpactX;
    }

    public void setBloodImpactX(float bloodImpactX) {
        this.bloodImpactX = bloodImpactX;
    }

    public float getBloodImpactY() {
        return this.bloodImpactY;
    }

    public void setBloodImpactY(float bloodImpactY) {
        this.bloodImpactY = bloodImpactY;
    }

    public float getBloodImpactZ() {
        return this.bloodImpactZ;
    }

    public void setBloodImpactZ(float bloodImpactZ) {
        this.bloodImpactZ = bloodImpactZ;
    }

    public IsoSprite getBloodSplat() {
        return this.bloodSplat;
    }

    public void setBloodSplat(IsoSprite bloodSplat) {
        this.bloodSplat = bloodSplat;
    }

    @Deprecated
    public boolean isbOnBed() {
        return this.onBed;
    }

    @Deprecated
    public void setbOnBed(boolean onBed) {
        this.onBed = onBed;
    }

    public boolean isOnBed() {
        return this.onBed;
    }

    public void setOnBed(boolean bOnBed) {
        this.onBed = bOnBed;
    }

    public Vector2 getMoveForwardVec() {
        return this.moveForwardVec;
    }

    public void setMoveForwardVec(Vector2 moveForwardVec) {
        this.moveForwardVec.set(moveForwardVec);
    }

    public boolean isPathing() {
        return this.pathing;
    }

    public void setPathing(boolean pathing) {
        this.pathing = pathing;
    }

    public Stack<IsoGameCharacter> getLocalEnemyList() {
        return this.localEnemyList;
    }

    public Stack<IsoGameCharacter> getEnemyList() {
        return this.enemyList;
    }

    @Override
    public CharacterTraits getCharacterTraits() {
        return this.characterTraits;
    }

    @Override
    public int getMaxWeight() {
        return this.maxWeight;
    }

    public void setMaxWeight(int maxWeight) {
        this.maxWeight = maxWeight;
    }

    public int getMaxWeightBase() {
        return this.maxWeightBase;
    }

    public void setMaxWeightBase(int maxWeightBase) {
        this.maxWeightBase = maxWeightBase;
    }

    public float getSleepingTabletDelta() {
        return this.sleepingTabletDelta;
    }

    public void setSleepingTabletDelta(float sleepingTabletDelta) {
        this.sleepingTabletDelta = sleepingTabletDelta;
    }

    public float getBetaEffect() {
        return this.betaEffect;
    }

    public void setBetaEffect(float betaEffect) {
        this.betaEffect = betaEffect;
    }

    public float getDepressEffect() {
        return this.depressEffect;
    }

    public void setDepressEffect(float depressEffect) {
        this.depressEffect = depressEffect;
    }

    @Override
    public float getSleepingTabletEffect() {
        return this.sleepingTabletEffect;
    }

    @Override
    public void setSleepingTabletEffect(float sleepingTabletEffect) {
        this.sleepingTabletEffect = sleepingTabletEffect;
    }

    public float getBetaDelta() {
        return this.betaDelta;
    }

    public void setBetaDelta(float betaDelta) {
        this.betaDelta = betaDelta;
    }

    public float getDepressDelta() {
        return this.depressDelta;
    }

    public void setDepressDelta(float depressDelta) {
        this.depressDelta = depressDelta;
    }

    public float getPainEffect() {
        return this.painEffect;
    }

    public void setPainEffect(float painEffect) {
        this.painEffect = painEffect;
    }

    public float getPainDelta() {
        return this.painDelta;
    }

    public void setPainDelta(float painDelta) {
        this.painDelta = painDelta;
    }

    public boolean isbDoDefer() {
        return this.doDefer;
    }

    public void setbDoDefer(boolean doDefer) {
        this.doDefer = doDefer;
    }

    public Location getLastHeardSound() {
        return this.lastHeardSound;
    }

    public void setLastHeardSound(int x, int y, int z) {
        this.lastHeardSound.x = x;
        this.lastHeardSound.y = y;
        this.lastHeardSound.z = z;
    }

    public boolean isClimbing() {
        return this.climbing;
    }

    public void setbClimbing(boolean climbing) {
        this.climbing = climbing;
    }

    public boolean isLastCollidedW() {
        return this.lastCollidedW;
    }

    public void setLastCollidedW(boolean lastCollidedW) {
        this.lastCollidedW = lastCollidedW;
    }

    public boolean isLastCollidedN() {
        return this.lastCollidedN;
    }

    public void setLastCollidedN(boolean lastCollidedN) {
        this.lastCollidedN = lastCollidedN;
    }

    public float getFallTime() {
        return this.fallTime;
    }

    public FallSeverity getFallSpeedSeverity() {
        return FallingConstants.getFallSeverity(this.lastFallSpeed);
    }

    public void setFallTime(float fallTime) {
        this.fallTime = fallTime;
    }

    public float getLastFallSpeed() {
        return this.lastFallSpeed;
    }

    public void setLastFallSpeed(float lastFallSpeed) {
        this.lastFallSpeed = lastFallSpeed;
    }

    public boolean isbFalling() {
        return this.falling && !this.ragdollFall;
    }

    public void setbFalling(boolean falling) {
        this.falling = falling;
    }

    public BuildingDef getCurrentBuildingDef() {
        return this.getCurrentSquare() == null ? null : this.getCurrentSquare().getBuildingDef();
    }

    public RoomDef getCurrentRoomDef() {
        return this.getCurrentSquare() == null ? null : this.getCurrentSquare().getRoomDef();
    }

    public float getTorchStrength() {
        return 0.0f;
    }

    @Override
    public AnimEventBroadcaster getAnimEventBroadcaster() {
        return this.animEventBroadcaster;
    }

    @Override
    public void OnAnimEvent(AnimLayer sender, AnimationTrack track, AnimEvent event) {
        if (event.eventName == null) {
            return;
        }
        this.animEvent(this, sender, track, event);
        if (Core.debug && DebugOptions.instance.animation.animLayer.allowAnimNodeOverride.getValue()) {
            IsoGameCharacter.dbgOnGlobalAnimEvent(this, sender, track, event);
        }
        int layerIdx = AnimLayer.getDepth(sender);
        String stateName = AnimLayer.getCurrentStateName(sender);
        DebugType.ActionSystemEvents.trace("%s.animEvent: %s(%s) time=%f", stateName, event.eventName, event.parameterValue, Float.valueOf(event.timePc));
        this.actionContext.reportEvent(stateName, event.eventName);
        this.stateMachine.stateAnimEvent(layerIdx, sender, track, event);
    }

    private static void dbgOnGlobalAnimEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (!Core.debug) {
            return;
        }
        SwipeStatePlayer.dbgOnGlobalAnimEvent(owner, layer, track, event);
    }

    private void OnAnimEvent_SetVariable(IsoGameCharacter owner, AnimationVariableReference animReference, String variableValue) {
        DebugType.Animation.trace("SetVariable(%s, %s)", animReference, variableValue);
        animReference.setVariable((IAnimationVariableSource)owner, variableValue);
    }

    private void OnAnimEvent_ClearVariable(IsoGameCharacter owner, String variableName) {
        AnimationVariableReference animReference = AnimationVariableReference.fromRawVariableName(variableName);
        animReference.clearVariable(owner);
    }

    private void OnAnimEvent_PlaySound(IsoGameCharacter owner, String file) {
        owner.getEmitter().playSoundImpl(file, this);
    }

    private void OnAnimEvent_PlaySoundNoBlend(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (layer.isBlendingIn() || layer.isBlendingOut()) {
            return;
        }
        owner.getEmitter().playSoundImpl(event.parameterValue, this);
    }

    private void OnAnimEvent_Footstep(IsoGameCharacter owner, String type) {
        owner.DoFootstepSound(type);
    }

    private void OnAnimEvent_DamageWhileInTrees(IsoGameCharacter owner) {
        owner.damageWhileInTrees();
    }

    private void OnAnimEvent_TurnAround(IsoGameCharacter owner, boolean instant) {
        float newDirectionX = -owner.getForwardDirectionX();
        float newDirectionY = -owner.getForwardDirectionY();
        if (instant) {
            owner.setTargetAndCurrentDirection(newDirectionX, newDirectionY);
        } else {
            owner.setForwardDirection(newDirectionX, newDirectionY);
        }
    }

    private void OnAnimEvent_TurnAroundFlipSkeleton(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, String boneName) {
        float newDirectionX = -owner.getForwardDirectionX();
        float newDirectionY = -owner.getForwardDirectionY();
        owner.setTargetAndCurrentDirection(newDirectionX, newDirectionY);
        AnimationPlayer animationPlayer = owner.getAnimationPlayer();
        if (animationPlayer == null) {
            return;
        }
        int boneIndex = SkeletonBone.getBoneOrdinal(boneName);
        if (boneIndex == SkeletonBone.None.ordinal()) {
            DebugType.Animation.warn("Bone not found: %s", boneName);
            return;
        }
        Quaternion rotation = HelperFunctions.allocQuaternion();
        HelperFunctions.setFromAxisAngle(0.0f, 1.0f, 0.0f, (float)Math.PI, rotation);
        track.setBonePoseAdjustment(boneIndex, new org.lwjgl.util.vector.Vector3f(0.0f, 0.0f, 0.0f), rotation, new org.lwjgl.util.vector.Vector3f(1.0f, 1.0f, 1.0f));
        HelperFunctions.setFromAxisAngle(0.0f, 1.0f, 0.0f, (float)Math.PI, rotation);
        animationPlayer.transformRootChildBones(boneName, rotation);
        HelperFunctions.releaseQuaternion(rotation);
    }

    private void OnAnimEvent_SetSharedGrappleType(IsoGameCharacter owner, String sharedGrappleType) {
        owner.setSharedGrappleType(sharedGrappleType);
    }

    public void onRagdollSimulationStarted() {
        float collisionAvoidanceRadius = 0.5f;
        this.slideAwayFromWalls(0.5f, true, true);
    }

    private void damageWhileInTrees() {
        if (GameClient.client || this.isZombie() || "Tutorial".equals(Core.gameMode)) {
            return;
        }
        int rand = 50;
        int part = Rand.Next(0, BodyPartType.ToIndex(BodyPartType.MAX));
        if (this.isRunning()) {
            rand = 30;
        }
        if (this.characterTraits.get(CharacterTrait.OUTDOORSMAN)) {
            rand += 50;
        }
        if (Rand.NextBool(rand += (int)this.getBodyPartClothingDefense(part, false, false))) {
            this.addHole(BloodBodyPartType.FromIndex(part));
            rand = 6;
            if (this.characterTraits.get(CharacterTrait.THICK_SKINNED)) {
                rand += 7;
            }
            if (this.characterTraits.get(CharacterTrait.THIN_SKINNED)) {
                rand -= 3;
            }
            if (Rand.NextBool(rand) && (int)this.getBodyPartClothingDefense(part, false, false) < 100) {
                BodyPart bodyPart = this.getBodyDamage().getBodyParts().get(part);
                bodyPart.setScratched(true, true);
                IsoGameCharacter isoGameCharacter = this;
                if (isoGameCharacter instanceof IsoPlayer) {
                    IsoPlayer player = (IsoPlayer)isoGameCharacter;
                    if (GameServer.server) {
                        player.getNetworkCharacterAI().syncDamage();
                        INetworkPacket.sendToRelative(PacketTypes.PacketType.PlaySound, this.getXi(), (float)this.getYi(), "PainFromScratch", (byte)2, this);
                    } else {
                        player.playerVoiceSound("PainFromScratch");
                    }
                }
            }
        }
    }

    @Override
    public float getHammerSoundMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Woodwork);
        if (level == 2) {
            return 0.8f;
        }
        if (level == 3) {
            return 0.6f;
        }
        if (level == 4) {
            return 0.4f;
        }
        if (level >= 5) {
            return 0.4f;
        }
        return 1.0f;
    }

    @Override
    public float getWeldingSoundMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.MetalWelding);
        if (level == 2) {
            return 0.8f;
        }
        if (level == 3) {
            return 0.6f;
        }
        if (level == 4) {
            return 0.4f;
        }
        if (level >= 5) {
            return 0.4f;
        }
        return 1.0f;
    }

    public float getBarricadeTimeMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Woodwork);
        if (level == 1) {
            return 0.8f;
        }
        if (level == 2) {
            return 0.7f;
        }
        if (level == 3) {
            return 0.62f;
        }
        if (level == 4) {
            return 0.56f;
        }
        if (level == 5) {
            return 0.5f;
        }
        if (level == 6) {
            return 0.42f;
        }
        if (level == 7) {
            return 0.36f;
        }
        if (level == 8) {
            return 0.3f;
        }
        if (level == 9) {
            return 0.26f;
        }
        if (level == 10) {
            return 0.2f;
        }
        return 0.7f;
    }

    public float getMetalBarricadeStrengthMod() {
        switch (this.getPerkLevel(PerkFactory.Perks.MetalWelding)) {
            case 2: {
                return 1.1f;
            }
            case 3: {
                return 1.14f;
            }
            case 4: {
                return 1.18f;
            }
            case 5: {
                return 1.22f;
            }
            case 6: {
                return 1.26f;
            }
            case 7: {
                return 1.3f;
            }
            case 8: {
                return 1.34f;
            }
            case 9: {
                return 1.4f;
            }
            case 10: {
                return 1.5f;
            }
        }
        int level = this.getPerkLevel(PerkFactory.Perks.Woodwork);
        if (level == 2) {
            return 1.1f;
        }
        if (level == 3) {
            return 1.14f;
        }
        if (level == 4) {
            return 1.18f;
        }
        if (level == 5) {
            return 1.22f;
        }
        if (level == 6) {
            return 1.26f;
        }
        if (level == 7) {
            return 1.3f;
        }
        if (level == 8) {
            return 1.34f;
        }
        if (level == 9) {
            return 1.4f;
        }
        if (level == 10) {
            return 1.5f;
        }
        return 1.0f;
    }

    public float getBarricadeStrengthMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Woodwork);
        if (level == 2) {
            return 1.1f;
        }
        if (level == 3) {
            return 1.14f;
        }
        if (level == 4) {
            return 1.18f;
        }
        if (level == 5) {
            return 1.22f;
        }
        if (level == 6) {
            return 1.26f;
        }
        if (level == 7) {
            return 1.3f;
        }
        if (level == 8) {
            return 1.34f;
        }
        if (level == 9) {
            return 1.4f;
        }
        if (level == 10) {
            return 1.5f;
        }
        return 1.0f;
    }

    public float getSneakSpotMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Sneak);
        float result = 0.95f;
        if (level == 1) {
            result = 0.9f;
        }
        if (level == 2) {
            result = 0.8f;
        }
        if (level == 3) {
            result = 0.75f;
        }
        if (level == 4) {
            result = 0.7f;
        }
        if (level == 5) {
            result = 0.65f;
        }
        if (level == 6) {
            result = 0.6f;
        }
        if (level == 7) {
            result = 0.55f;
        }
        if (level == 8) {
            result = 0.5f;
        }
        if (level == 9) {
            result = 0.45f;
        }
        if (level == 10) {
            result = 0.4f;
        }
        return result *= 1.2f;
    }

    public float getNimbleMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Nimble);
        if (level == 1) {
            return 1.1f;
        }
        if (level == 2) {
            return 1.14f;
        }
        if (level == 3) {
            return 1.18f;
        }
        if (level == 4) {
            return 1.22f;
        }
        if (level == 5) {
            return 1.26f;
        }
        if (level == 6) {
            return 1.3f;
        }
        if (level == 7) {
            return 1.34f;
        }
        if (level == 8) {
            return 1.38f;
        }
        if (level == 9) {
            return 1.42f;
        }
        if (level == 10) {
            return 1.5f;
        }
        return 1.0f;
    }

    @Override
    public float getFatigueMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Fitness);
        if (level == 1) {
            return 0.95f;
        }
        if (level == 2) {
            return 0.92f;
        }
        if (level == 3) {
            return 0.89f;
        }
        if (level == 4) {
            return 0.87f;
        }
        if (level == 5) {
            return 0.85f;
        }
        if (level == 6) {
            return 0.83f;
        }
        if (level == 7) {
            return 0.81f;
        }
        if (level == 8) {
            return 0.79f;
        }
        if (level == 9) {
            return 0.77f;
        }
        if (level == 10) {
            return 0.75f;
        }
        return 1.0f;
    }

    public float getLightfootMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Lightfoot);
        if (level == 1) {
            return 0.9f;
        }
        if (level == 2) {
            return 0.79f;
        }
        if (level == 3) {
            return 0.71f;
        }
        if (level == 4) {
            return 0.65f;
        }
        if (level == 5) {
            return 0.59f;
        }
        if (level == 6) {
            return 0.52f;
        }
        if (level == 7) {
            return 0.45f;
        }
        if (level == 8) {
            return 0.37f;
        }
        if (level == 9) {
            return 0.3f;
        }
        if (level == 10) {
            return 0.2f;
        }
        return 0.99f;
    }

    public float getPacingMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Fitness);
        if (level == 1) {
            return 0.8f;
        }
        if (level == 2) {
            return 0.75f;
        }
        if (level == 3) {
            return 0.7f;
        }
        if (level == 4) {
            return 0.65f;
        }
        if (level == 5) {
            return 0.6f;
        }
        if (level == 6) {
            return 0.57f;
        }
        if (level == 7) {
            return 0.53f;
        }
        if (level == 8) {
            return 0.49f;
        }
        if (level == 9) {
            return 0.46f;
        }
        if (level == 10) {
            return 0.43f;
        }
        return 0.9f;
    }

    public float getHyperthermiaMod() {
        float delta = 1.0f;
        if (this.getMoodles().getMoodleLevel(MoodleType.HYPERTHERMIA) > 1 && this.getMoodles().getMoodleLevel(MoodleType.HYPERTHERMIA) == 4) {
            delta = 2.0f;
        }
        return delta;
    }

    public float getHittingMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Strength);
        if (level == 1) {
            return 0.8f;
        }
        if (level == 2) {
            return 0.85f;
        }
        if (level == 3) {
            return 0.9f;
        }
        if (level == 4) {
            return 0.95f;
        }
        if (level == 5) {
            return 1.0f;
        }
        if (level == 6) {
            return 1.05f;
        }
        if (level == 7) {
            return 1.1f;
        }
        if (level == 8) {
            return 1.15f;
        }
        if (level == 9) {
            return 1.2f;
        }
        if (level == 10) {
            return 1.25f;
        }
        return 0.75f;
    }

    public float getShovingMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Strength);
        if (level == 1) {
            return 0.8f;
        }
        if (level == 2) {
            return 0.85f;
        }
        if (level == 3) {
            return 0.9f;
        }
        if (level == 4) {
            return 0.95f;
        }
        if (level == 5) {
            return 1.0f;
        }
        if (level == 6) {
            return 1.05f;
        }
        if (level == 7) {
            return 1.1f;
        }
        if (level == 8) {
            return 1.15f;
        }
        if (level == 9) {
            return 1.2f;
        }
        if (level == 10) {
            return 1.25f;
        }
        return 0.75f;
    }

    public float getRecoveryMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Fitness);
        float mod = 0.0f;
        if (level == 0) {
            mod = 0.7f;
        }
        if (level == 1) {
            mod = 0.8f;
        }
        if (level == 2) {
            mod = 0.9f;
        }
        if (level == 3) {
            mod = 1.0f;
        }
        if (level == 4) {
            mod = 1.1f;
        }
        if (level == 5) {
            mod = 1.2f;
        }
        if (level == 6) {
            mod = 1.3f;
        }
        if (level == 7) {
            mod = 1.4f;
        }
        if (level == 8) {
            mod = 1.5f;
        }
        if (level == 9) {
            mod = 1.55f;
        }
        if (level == 10) {
            mod = 1.6f;
        }
        if (this.characterTraits.get(CharacterTrait.OBESE)) {
            mod *= 0.4f;
        }
        if (this.characterTraits.get(CharacterTrait.OVERWEIGHT)) {
            mod *= 0.7f;
        }
        if (this.characterTraits.get(CharacterTrait.VERY_UNDERWEIGHT)) {
            mod *= 0.7f;
        }
        if (this.characterTraits.get(CharacterTrait.EMACIATED)) {
            mod *= 0.3f;
        }
        if (this instanceof IsoPlayer) {
            if (((IsoPlayer)this).getNutrition().getLipids() < -1500.0f) {
                mod *= 0.2f;
            } else if (((IsoPlayer)this).getNutrition().getLipids() < -1000.0f) {
                mod *= 0.5f;
            }
            if (((IsoPlayer)this).getNutrition().getProteins() < -1500.0f) {
                mod *= 0.2f;
            } else if (((IsoPlayer)this).getNutrition().getProteins() < -1000.0f) {
                mod *= 0.5f;
            }
        }
        return mod;
    }

    public float getWeightMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Strength);
        if (level == 1) {
            return 0.9f;
        }
        if (level == 2) {
            return 1.07f;
        }
        if (level == 3) {
            return 1.24f;
        }
        if (level == 4) {
            return 1.41f;
        }
        if (level == 5) {
            return 1.58f;
        }
        if (level == 6) {
            return 1.75f;
        }
        if (level == 7) {
            return 1.92f;
        }
        if (level == 8) {
            return 2.09f;
        }
        if (level == 9) {
            return 2.26f;
        }
        if (level == 10) {
            return 2.5f;
        }
        return 0.8f;
    }

    public int getHitChancesMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Aiming);
        if (level == 1) {
            return 1;
        }
        if (level == 2) {
            return 1;
        }
        if (level == 3) {
            return 2;
        }
        if (level == 4) {
            return 2;
        }
        if (level == 5) {
            return 3;
        }
        if (level == 6) {
            return 3;
        }
        if (level == 7) {
            return 4;
        }
        if (level == 8) {
            return 4;
        }
        if (level == 9) {
            return 5;
        }
        if (level == 10) {
            return 5;
        }
        return 1;
    }

    public float getSprintMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Sprinting);
        if (level == 1) {
            return 1.1f;
        }
        if (level == 2) {
            return 1.15f;
        }
        if (level == 3) {
            return 1.2f;
        }
        if (level == 4) {
            return 1.25f;
        }
        if (level == 5) {
            return 1.3f;
        }
        if (level == 6) {
            return 1.35f;
        }
        if (level == 7) {
            return 1.4f;
        }
        if (level == 8) {
            return 1.45f;
        }
        if (level == 9) {
            return 1.5f;
        }
        if (level == 10) {
            return 1.6f;
        }
        return 0.9f;
    }

    @Override
    public int getPerkLevel(PerkFactory.Perk perks) {
        PerkInfo info = this.getPerkInfo(perks);
        if (info != null) {
            return info.level;
        }
        return 0;
    }

    @Override
    public void setPerkLevelDebug(PerkFactory.Perk perks, int level) {
        PerkInfo info = this.getPerkInfo(perks);
        if (info != null) {
            info.level = level;
        } else {
            info = new PerkInfo(this);
            info.perk = perks;
            info.level = level;
            this.perkList.add(info);
        }
        if (GameClient.client && this instanceof IsoPlayer) {
            GameClient.sendPerks((IsoPlayer)this);
        }
    }

    @Override
    public void LoseLevel(PerkFactory.Perk perk) {
        PerkInfo info = this.getPerkInfo(perk);
        if (info != null) {
            --info.level;
            if (info.level < 0) {
                info.level = 0;
            }
            LuaEventManager.triggerEvent("LevelPerk", this, perk, info.level, false);
            if (perk == PerkFactory.Perks.Sneak && GameClient.client && this instanceof IsoPlayer) {
                GameClient.sendPerks((IsoPlayer)this);
            }
            return;
        }
        LuaEventManager.triggerEvent("LevelPerk", this, perk, 0, false);
    }

    @Override
    public void LevelPerk(PerkFactory.Perk perk, boolean removePick) {
        Objects.requireNonNull(perk, "perk is null");
        if (perk == PerkFactory.Perks.MAX) {
            throw new IllegalArgumentException("perk == Perks.MAX");
        }
        IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
        PerkInfo info = this.getPerkInfo(perk);
        if (info != null) {
            ++info.level;
            if (player != null && !"Tutorial".equals(Core.gameMode) && this.getHoursSurvived() > 0.016666666666666666) {
                HaloTextHelper.addTextWithArrow(player, "+1 " + perk.getName(), "[br/]", true, HaloTextHelper.getGoodColor());
            }
            if (info.level > 10) {
                info.level = 10;
            }
            if (Core.debug && info.perk == PerkFactory.Perks.Fitness) {
                player.getStats().set(CharacterStat.FITNESS, (float)info.level / 5.0f - 1.0f);
            }
            LuaEventManager.triggerEventGarbage("LevelPerk", this, perk, info.level, true);
            if (GameClient.client && player != null) {
                GameClient.sendPerks(player);
            }
            return;
        }
        info = new PerkInfo(this);
        info.perk = perk;
        info.level = 1;
        this.perkList.add(info);
        if (player != null && !"Tutorial".equals(Core.gameMode) && this.getHoursSurvived() > 0.016666666666666666) {
            HaloTextHelper.addTextWithArrow(player, "+1 " + perk.getName(), "[br/]", true, HaloTextHelper.getGoodColor());
        }
        LuaEventManager.triggerEvent("LevelPerk", this, perk, info.level, true);
    }

    @Override
    public void LevelPerk(PerkFactory.Perk perk) {
        this.LevelPerk(perk, true);
    }

    public void level0(PerkFactory.Perk perk) {
        PerkInfo info = this.getPerkInfo(perk);
        if (info != null) {
            info.level = 0;
        }
    }

    public Location getLastKnownLocationOf(String character) {
        if (this.lastKnownLocation.containsKey(character)) {
            return this.lastKnownLocation.get(character);
        }
        return null;
    }

    @Override
    public void ReadLiterature(Literature literature) {
        this.stats.add(CharacterStat.STRESS, literature.getStressChange());
        this.getBodyDamage().JustReadSomething(literature);
        if (literature.getLearnedRecipes() != null) {
            for (int i = 0; i < literature.getLearnedRecipes().size(); ++i) {
                if (this.getKnownRecipes().contains(literature.getLearnedRecipes().get(i))) continue;
                this.learnRecipe(literature.getLearnedRecipes().get(i));
            }
        }
        if (literature.hasTag(ItemTag.CONSUME_ON_READ)) {
            literature.Use();
        }
    }

    public void OnDeath() {
        LuaEventManager.triggerEvent("OnCharacterDeath", this);
    }

    public void splatBloodFloorBig() {
        if (this.getCurrentSquare() != null && this.getCurrentSquare().getChunk() != null) {
            this.getCurrentSquare().getChunk().addBloodSplat(this.getX(), this.getY(), this.getZ(), Rand.Next(20));
        }
    }

    public void splatBloodFloor() {
        if (this.getCurrentSquare() == null) {
            return;
        }
        if (this.getCurrentSquare().getChunk() == null) {
            return;
        }
        if (this.isDead() && Rand.Next(10) == 0) {
            this.getCurrentSquare().getChunk().addBloodSplat(this.getX(), this.getY(), this.getZ(), Rand.Next(20));
        }
        if (Rand.Next(14) == 0) {
            this.getCurrentSquare().getChunk().addBloodSplat(this.getX(), this.getY(), this.getZ(), Rand.Next(8));
        }
        if (Rand.Next(50) == 0) {
            this.getCurrentSquare().getChunk().addBloodSplat(this.getX(), this.getY(), this.getZ(), Rand.Next(20));
        }
    }

    public int getThreatLevel() {
        int total = this.localRelevantEnemyList.size();
        if ((total += this.veryCloseEnemyList.size() * 10) > 20) {
            return 3;
        }
        if (total > 10) {
            return 2;
        }
        if (total > 0) {
            return 1;
        }
        return 0;
    }

    public boolean isDead() {
        return this.health <= 0.0f || this.getBodyDamage() != null && this.getBodyDamage().getHealth() <= 0.0f;
    }

    public boolean isAlive() {
        return !this.isDead();
    }

    public boolean isEditingRagdoll() {
        return this.isEditingRagdoll;
    }

    public void setEditingRagdoll(boolean value) {
        this.isEditingRagdoll = value;
    }

    public boolean isRagdoll() {
        return this.hasAnimationPlayer() && this.getAnimationPlayer().isRagdolling();
    }

    public boolean isFullyRagdolling() {
        return this.hasAnimationPlayer() && this.getAnimationPlayer().isFullyRagdolling();
    }

    public void setRagdollFall(boolean value) {
        this.ragdollFall = value;
    }

    public boolean isRagdollFall() {
        return this.ragdollFall;
    }

    public boolean isVehicleCollision() {
        return this.vehicleCollision;
    }

    public void setVehicleCollision(boolean value) {
        this.vehicleCollision = value;
    }

    public boolean useRagdollVehicleCollision() {
        return this.canRagdoll() && this.vehicleCollision;
    }

    public boolean isUpright() {
        if (this.canRagdoll()) {
            if (this.getRagdollController() == null) {
                return false;
            }
            return this.getRagdollController().isUpright();
        }
        return false;
    }

    public boolean isOnBack() {
        if (this.getRagdollController() == null) {
            return false;
        }
        return this.getRagdollController().isOnBack();
    }

    public boolean usePhysicHitReaction() {
        return this.usePhysicHitReaction;
    }

    public void setUsePhysicHitReaction(boolean usePhysicHitReaction) {
        this.usePhysicHitReaction = usePhysicHitReaction;
    }

    public boolean isRagdollSimulationActive() {
        return this.hasAnimationPlayer() && this.getAnimationPlayer().isRagdollSimulationActive() && this.canRagdoll();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void Seen(Stack<IsoMovingObject> seenList) {
        ArrayList<IsoMovingObject> arrayList = this.localList;
        synchronized (arrayList) {
            this.localList.clear();
            this.localList.addAll(seenList);
        }
    }

    public boolean CanSee(IsoMovingObject obj) {
        return this.CanSee((IsoObject)obj);
    }

    public boolean CanSee(IsoObject obj) {
        return LosUtil.lineClear(this.getCell(), PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), PZMath.fastfloor(obj.getX()), PZMath.fastfloor(obj.getY()), PZMath.fastfloor(obj.getZ()), false) != LosUtil.TestResults.Blocked;
    }

    public IsoGridSquare getLowDangerInVicinity(int attempts, int range) {
        float highscore = -1000000.0f;
        IsoGridSquare chosen = null;
        for (int n = 0; n < attempts; ++n) {
            float score = 0.0f;
            int randx = Rand.Next(-range, range);
            int randy = Rand.Next(-range, range);
            IsoGridSquare sq = this.getCell().getGridSquare(PZMath.fastfloor(this.getX()) + randx, PZMath.fastfloor(this.getY()) + randy, PZMath.fastfloor(this.getZ()));
            if (sq == null || !sq.isFree(true)) continue;
            float total = sq.getMovingObjects().size();
            if (sq.getE() != null) {
                total += (float)sq.getE().getMovingObjects().size();
            }
            if (sq.getS() != null) {
                total += (float)sq.getS().getMovingObjects().size();
            }
            if (sq.getW() != null) {
                total += (float)sq.getW().getMovingObjects().size();
            }
            if (sq.getN() != null) {
                total += (float)sq.getN().getMovingObjects().size();
            }
            if (!((score -= total * 1000.0f) > highscore)) continue;
            highscore = score;
            chosen = sq;
        }
        return chosen;
    }

    @Override
    public boolean hasEquipped(String string) {
        if (string.contains(".")) {
            string = string.split("\\.")[1];
        }
        if (this.leftHandItem != null && this.leftHandItem.getType().equals(string)) {
            return true;
        }
        return this.rightHandItem != null && this.rightHandItem.getType().equals(string);
    }

    @Override
    public boolean hasEquippedTag(ItemTag itemTag) {
        if (this.leftHandItem != null && this.leftHandItem.hasTag(itemTag)) {
            return true;
        }
        return this.rightHandItem != null && this.rightHandItem.hasTag(itemTag);
    }

    @Override
    public boolean hasWornTag(ItemTag itemTag) {
        for (int i = 0; i < this.getWornItems().size(); ++i) {
            InventoryItem item = this.getWornItems().getItemByIndex(i);
            if (!item.hasTag(itemTag)) continue;
            return true;
        }
        return false;
    }

    @Override
    public void setForwardIsoDirection(IsoDirections directions) {
        this.dir = directions;
        this.setForwardDirectionFromIsoDirection();
    }

    public void setForwardDirectionFromIsoDirection() {
        this.getVectorFromDirection(tempVector2_2);
        this.setForwardDirection(tempVector2_2);
    }

    public void setForwardDirectionFromAnimAngle() {
        this.setDirectionAngle(this.getAnimAngle());
    }

    public void Callout(boolean doAnim) {
        if (!this.isCanShout()) {
            return;
        }
        this.Callout();
        if (doAnim) {
            this.playEmote("shout");
        }
    }

    @Override
    public void Callout() {
        String text = "";
        InventoryItem item = this.getPrimaryHandItem();
        boolean bMegaphone = item != null && item.hasTag(ItemTag.MEGAPHONE);
        int radius = bMegaphone ? 90 : 30;
        IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
        if (Core.getInstance().getGameMode().equals("Tutorial")) {
            text = Translator.getText("IGUI_PlayerText_CalloutTutorial");
            if (player != null) {
                player.transmitPlayerVoiceSound("ShoutHey");
            }
        } else if (this.isSneaking()) {
            radius = bMegaphone ? 18 : 6;
            switch (Rand.Next(3)) {
                case 0: {
                    text = Translator.getText("IGUI_PlayerText_Callout1Sneak");
                    if (player == null) break;
                    player.transmitPlayerVoiceSound(bMegaphone ? "WhisperMegaphonePsst" : "WhisperPsst");
                    break;
                }
                case 1: {
                    text = Translator.getText("IGUI_PlayerText_Callout2Sneak");
                    if (player == null) break;
                    player.transmitPlayerVoiceSound(bMegaphone ? "WhisperMegaphonePsst" : "WhisperPsst");
                    break;
                }
                case 2: {
                    text = Translator.getText("IGUI_PlayerText_Callout3Sneak");
                    if (player == null) break;
                    player.transmitPlayerVoiceSound(bMegaphone ? "WhisperMegaphoneHey" : "WhisperHey");
                }
            }
        } else {
            InventoryItem shoutItem = null;
            if (this.getPrimaryHandItem() != null && this.getPrimaryHandItem().getShoutType() != null) {
                shoutItem = this.getPrimaryHandItem();
            } else if (this.getSecondaryHandItem() != null && this.getSecondaryHandItem().getShoutType() != null) {
                shoutItem = this.getSecondaryHandItem();
            } else if (this.getWornItems() != null) {
                for (int i = 0; i < this.getWornItems().size(); ++i) {
                    if (this.getWornItems().get(i).getItem() == null || this.getWornItems().get(i).getItem().getShoutType() == null) continue;
                    shoutItem = this.getWornItems().get(i).getItem();
                    break;
                }
            }
            if (shoutItem != null) {
                this.playSound(shoutItem.getShoutType());
                radius = (int)((float)radius * shoutItem.getShoutMultiplier());
            } else {
                switch (Rand.Next(3)) {
                    case 0: {
                        String string = Translator.getText("IGUI_PlayerText_Callout1New");
                        break;
                    }
                    case 1: {
                        String string = Translator.getText("IGUI_PlayerText_Callout2New");
                        break;
                    }
                    case 2: {
                        String string = Translator.getText("IGUI_PlayerText_Callout3New");
                        break;
                    }
                    default: {
                        String string = text = text;
                    }
                }
                if (player != null) {
                    player.transmitPlayerVoiceSound(bMegaphone ? "ShoutMegaphoneHey" : "ShoutHey");
                }
            }
        }
        WorldSoundManager.instance.addSound(this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), radius, radius, false, 0.0f, 1.0f, false, true, false, false, true);
        this.SayShout(text);
        this.callOut = true;
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        int i;
        boolean onFire;
        super.load(input, worldVersion, isDebugSave);
        this.setForwardDirectionFromIsoDirection();
        if (input.get() != 0) {
            this.descriptor = new SurvivorDesc(true);
            this.descriptor.load(input, worldVersion, this);
            this.female = this.descriptor.isFemale();
        }
        this.getVisual().load(input, worldVersion);
        ArrayList<InventoryItem> savedItems = this.inventory.load(input, worldVersion);
        this.savedInventoryItems.clear();
        this.savedInventoryItems.addAll(savedItems);
        this.asleep = input.get() != 0;
        this.forceWakeUpTime = input.getFloat();
        if (!this.isZombie()) {
            this.stats.load(input, worldVersion);
            this.getBodyDamage().load(input, worldVersion);
            this.xp.load(input, worldVersion);
            ArrayList<InventoryItem> items = this.inventory.includingObsoleteItems;
            int n = input.getInt();
            if (n >= 0 && n < items.size()) {
                this.leftHandItem = items.get(n);
            }
            if ((n = input.getInt()) >= 0 && n < items.size()) {
                this.rightHandItem = items.get(n);
            }
            this.setEquipParent(null, this.leftHandItem);
            if (this.rightHandItem == this.leftHandItem) {
                this.setEquipParent(null, this.rightHandItem, false);
            } else {
                this.setEquipParent(null, this.rightHandItem);
            }
            InventoryItem inventoryItem = this.leftHandItem;
            if (inventoryItem instanceof HandWeapon) {
                HandWeapon handWeapon = (HandWeapon)inventoryItem;
                this.setUseHandWeapon(handWeapon);
            } else {
                this.setUseHandWeapon(null);
            }
        }
        boolean bl = onFire = input.get() != 0;
        if (onFire) {
            this.SetOnFire();
        }
        this.depressEffect = input.getFloat();
        this.depressFirstTakeTime = input.getFloat();
        this.betaEffect = input.getFloat();
        this.betaDelta = input.getFloat();
        this.painEffect = input.getFloat();
        this.painDelta = input.getFloat();
        this.sleepingTabletEffect = input.getFloat();
        this.sleepingTabletDelta = input.getFloat();
        int numBooks = input.getInt();
        for (int i2 = 0; i2 < numBooks; ++i2) {
            ReadBook read = new ReadBook();
            read.fullType = GameWindow.ReadString(input);
            read.alreadyReadPages = input.getInt();
            this.readBooks.add(read);
        }
        this.reduceInfectionPower = input.getFloat();
        int numrecipes = input.getInt();
        for (int i3 = 0; i3 < numrecipes; ++i3) {
            this.knownRecipes.add(GameWindow.ReadString(input));
        }
        this.lastHourSleeped = input.getInt();
        this.timeSinceLastSmoke = input.getFloat();
        this.beardGrowTiming = input.getFloat();
        this.hairGrowTiming = input.getFloat();
        this.setUnlimitedCarry(input.get() != 0);
        this.setBuildCheat(input.get() != 0);
        this.setHealthCheat(input.get() != 0);
        this.setMechanicsCheat(input.get() != 0);
        this.setMovablesCheat(input.get() != 0);
        this.setFarmingCheat(input.get() != 0);
        if (worldVersion >= 202) {
            this.setFishingCheat(input.get() != 0);
        }
        if (worldVersion >= 217) {
            this.setCanUseBrushTool(input.get() != 0);
            this.setFastMoveCheat(input.get() != 0);
        }
        this.setTimedActionInstantCheat(input.get() != 0);
        this.setUnlimitedEndurance(input.get() != 0);
        if (worldVersion >= 230) {
            this.setUnlimitedAmmo(input.get() != 0);
            this.setKnowAllRecipes(input.get() != 0);
        }
        this.setSneaking(input.get() != 0);
        this.setDeathDragDown(input.get() != 0);
        int size = input.getInt();
        for (i = 0; i < size; ++i) {
            String title = GameWindow.ReadString(input);
            int day = input.getInt();
            this.addReadLiterature(title, day);
        }
        if (worldVersion >= 222) {
            this.readPrintMedia.clear();
            size = input.getInt();
            for (i = 0; i < size; ++i) {
                String mediaId = GameWindow.ReadString(input);
                this.readPrintMedia.add(mediaId);
            }
        }
        this.lastAnimalPet = input.getLong();
        if (worldVersion >= 231) {
            this.getCheats().load(input, worldVersion, isDebugSave);
        }
    }

    @Override
    public String getDescription(String separatorStr) {
        String result = this.getClass().getSimpleName() + " [" + separatorStr;
        result = result + "isDead=" + this.isDead() + " | " + separatorStr;
        result = result + super.getDescription(separatorStr + "    ") + " | " + separatorStr;
        result = result + "inventory=";
        for (int i = 0; i < this.inventory.items.size() - 1; ++i) {
            result = result + String.valueOf(this.inventory.items.get(i)) + ", ";
        }
        result = result + " ] ";
        return result;
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        int i;
        DebugType.Saving.trace("Saving: %s", this);
        super.save(output, isDebugSave);
        if (this.descriptor == null) {
            output.put((byte)0);
        } else {
            output.put((byte)1);
            this.descriptor.save(output);
        }
        this.getVisual().save(output);
        ArrayList<InventoryItem> savedItems = this.inventory.save(output, this);
        this.savedInventoryItems.clear();
        this.savedInventoryItems.addAll(savedItems);
        output.put(this.asleep ? (byte)1 : 0);
        output.putFloat(this.forceWakeUpTime);
        if (!this.isZombie()) {
            this.stats.save(output);
            this.getBodyDamage().save(output);
            this.xp.save(output);
            if (this.leftHandItem != null) {
                output.putInt(this.inventory.getItems().indexOf(this.leftHandItem));
            } else {
                output.putInt(-1);
            }
            if (this.rightHandItem != null) {
                output.putInt(this.inventory.getItems().indexOf(this.rightHandItem));
            } else {
                output.putInt(-1);
            }
        }
        output.put(this.onFire ? (byte)1 : 0);
        output.putFloat(this.depressEffect);
        output.putFloat(this.depressFirstTakeTime);
        output.putFloat(this.betaEffect);
        output.putFloat(this.betaDelta);
        output.putFloat(this.painEffect);
        output.putFloat(this.painDelta);
        output.putFloat(this.sleepingTabletEffect);
        output.putFloat(this.sleepingTabletDelta);
        output.putInt(this.readBooks.size());
        for (i = 0; i < this.readBooks.size(); ++i) {
            ReadBook read = this.readBooks.get(i);
            GameWindow.WriteString(output, read.fullType);
            output.putInt(read.alreadyReadPages);
        }
        output.putFloat(this.reduceInfectionPower);
        output.putInt(this.knownRecipes.size());
        for (i = 0; i < this.knownRecipes.size(); ++i) {
            String recipe = this.knownRecipes.get(i);
            GameWindow.WriteString(output, recipe);
        }
        output.putInt(this.lastHourSleeped);
        output.putFloat(this.timeSinceLastSmoke);
        output.putFloat(this.beardGrowTiming);
        output.putFloat(this.hairGrowTiming);
        output.put(this.isUnlimitedCarry() ? (byte)1 : 0);
        output.put(this.isBuildCheat() ? (byte)1 : 0);
        output.put(this.isHealthCheat() ? (byte)1 : 0);
        output.put(this.isMechanicsCheat() ? (byte)1 : 0);
        output.put(this.isMovablesCheat() ? (byte)1 : 0);
        output.put(this.isFarmingCheat() ? (byte)1 : 0);
        output.put(this.isFishingCheat() ? (byte)1 : 0);
        output.put(this.isCanUseBrushTool() ? (byte)1 : 0);
        output.put(this.isFastMoveCheat() ? (byte)1 : 0);
        output.put(this.isTimedActionInstantCheat() ? (byte)1 : 0);
        output.put(this.isUnlimitedEndurance() ? (byte)1 : 0);
        output.put(this.isUnlimitedAmmo() ? (byte)1 : 0);
        output.put(this.isKnowAllRecipes() ? (byte)1 : 0);
        output.put(this.isSneaking() ? (byte)1 : 0);
        output.put(this.isDeathDragDown() ? (byte)1 : 0);
        output.putInt(this.readLiterature.size());
        for (Map.Entry<String, Integer> entry : this.getReadLiterature().entrySet()) {
            GameWindow.WriteString(output, entry.getKey());
            output.putInt(entry.getValue());
        }
        output.putInt(this.readPrintMedia.size());
        for (String mediaId : this.readPrintMedia) {
            GameWindow.WriteString(output, mediaId);
        }
        output.putLong(this.lastAnimalPet);
        this.getCheats().save(output, isDebugSave);
    }

    public ChatElement getChatElement() {
        return this.chatElement;
    }

    @Override
    public void StartAction(BaseAction act) {
        this.characterActions.clear();
        this.characterActions.push(act);
        if (act.valid()) {
            act.waitToStart();
        }
    }

    public void QueueAction(BaseAction act) {
    }

    @Override
    public void StopAllActionQueue() {
        if (this.characterActions.isEmpty()) {
            return;
        }
        BaseAction act = (BaseAction)this.characterActions.get(0);
        if (act.started) {
            act.stop();
        }
        this.characterActions.clear();
        if (this == IsoPlayer.players[0] || this == IsoPlayer.players[1] || this == IsoPlayer.players[2] || this == IsoPlayer.players[3]) {
            UIManager.getProgressBar(((IsoPlayer)this).getPlayerNum()).setValue(0.0f);
        }
    }

    public void StopAllActionQueueRunning() {
        if (this.characterActions.isEmpty()) {
            return;
        }
        BaseAction act = (BaseAction)this.characterActions.get(0);
        if (!act.stopOnRun) {
            return;
        }
        if (act.started) {
            act.stop();
        }
        this.characterActions.clear();
        if (this == IsoPlayer.players[0] || this == IsoPlayer.players[1] || this == IsoPlayer.players[2] || this == IsoPlayer.players[3]) {
            UIManager.getProgressBar(((IsoPlayer)this).getPlayerNum()).setValue(0.0f);
        }
    }

    public void StopAllActionQueueAiming() {
        if (this.characterActions.isEmpty()) {
            return;
        }
        BaseAction act = (BaseAction)this.characterActions.get(0);
        if (!act.stopOnAim) {
            return;
        }
        if (act.started) {
            act.stop();
        }
        this.characterActions.clear();
        if (this == IsoPlayer.players[0] || this == IsoPlayer.players[1] || this == IsoPlayer.players[2] || this == IsoPlayer.players[3]) {
            UIManager.getProgressBar(((IsoPlayer)this).getPlayerNum()).setValue(0.0f);
        }
    }

    public void StopAllActionQueueWalking() {
        if (this.characterActions.isEmpty()) {
            return;
        }
        BaseAction act = (BaseAction)this.characterActions.get(0);
        if (!act.stopOnWalk) {
            return;
        }
        if (act.started) {
            act.stop();
        }
        this.characterActions.clear();
        if (this == IsoPlayer.players[0] || this == IsoPlayer.players[1] || this == IsoPlayer.players[2] || this == IsoPlayer.players[3]) {
            UIManager.getProgressBar(((IsoPlayer)this).getPlayerNum()).setValue(0.0f);
        }
    }

    @Override
    public String GetAnimSetName() {
        return "Base";
    }

    public void SleepingTablet(float sleepingTabletDelta) {
        this.sleepingTabletEffect = 6600.0f;
        this.sleepingTabletDelta += sleepingTabletDelta;
    }

    public void BetaBlockers(float delta) {
        this.betaEffect = 6600.0f;
        this.betaDelta += delta;
    }

    public void BetaAntiDepress(float delta) {
        if (this.depressEffect == 0.0f) {
            this.depressFirstTakeTime = 10000.0f;
        }
        this.depressEffect = 6600.0f;
        this.depressDelta += delta;
    }

    public void PainMeds(float delta) {
        this.painEffect = 5400.0f;
        this.painDelta += delta;
    }

    @Override
    public void initSpritePartsEmpty() {
        this.InitSpriteParts(this.descriptor);
    }

    public void InitSpriteParts(SurvivorDesc desc) {
        this.sprite.disposeAnimation();
        this.legsSprite = this.sprite;
        this.legsSprite.name = desc.getTorso();
        this.useParts = true;
    }

    @Override
    public boolean hasTrait(CharacterTrait characterTrait) {
        return this.characterTraits.get(characterTrait);
    }

    public void ApplyInBedOffset(boolean apply2) {
        if (apply2) {
            if (!this.onBed) {
                this.offsetX -= 20.0f;
                this.offsetY += 21.0f;
                this.onBed = true;
            }
        } else if (this.onBed) {
            this.offsetX += 20.0f;
            this.offsetY -= 21.0f;
            this.onBed = false;
        }
    }

    @Override
    public void Dressup(SurvivorDesc desc) {
        if (this.isZombie()) {
            return;
        }
        if (this.wornItems == null) {
            return;
        }
        ItemVisuals itemVisuals = new ItemVisuals();
        desc.getItemVisuals(itemVisuals);
        this.wornItems.setFromItemVisuals(itemVisuals);
        this.wornItems.addItemsToItemContainer(this.inventory);
        desc.getWornItems().clear();
        this.onWornItemsChanged();
    }

    public void setPathSpeed(float speed) {
    }

    @Override
    public void PlayAnim(String string) {
    }

    @Override
    public void PlayAnimWithSpeed(String string, float framesSpeedPerFrame) {
    }

    @Override
    public void PlayAnimUnlooped(String string) {
    }

    public void DirectionFromVector(Vector2 vecA) {
        this.dir = IsoDirections.fromAngle(vecA);
    }

    public void DoFootstepSound(String type) {
        float volume = switch (type) {
            case "sneak_walk" -> 0.2f;
            case "sneak_run", "walk" -> 0.5f;
            case "strafe" -> {
                if (this.sneaking) {
                    yield 0.2f;
                }
                yield 0.3f;
            }
            case "run" -> 1.3f;
            case "sprint" -> 1.8f;
            default -> 1.0f;
        };
        this.DoFootstepSound(volume);
    }

    public void DoFootstepSound(float volume) {
        IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
        if (player != null && player.isGhostMode() && !DebugOptions.instance.character.debug.playSoundWhenInvisible.getValue()) {
            return;
        }
        if (this.getCurrentSquare() == null) {
            return;
        }
        if (volume <= 0.0f) {
            return;
        }
        float parameterVolume = volume;
        volume *= 1.4f;
        if (this.characterTraits.get(CharacterTrait.GRACEFUL)) {
            volume *= 0.6f;
        }
        if (this.characterTraits.get(CharacterTrait.CLUMSY)) {
            volume *= 1.2f;
        }
        if (this.getWornItem(ItemBodyLocation.SHOES) == null) {
            volume *= 0.5f;
        }
        volume *= this.getLightfootMod();
        volume *= 2.0f - this.getNimbleMod();
        if (this.sneaking) {
            volume *= this.getSneakSpotMod();
        }
        if (volume > 0.0f) {
            this.emitter.playFootsteps("HumanFootstepsCombined", parameterVolume);
            if (player != null && player.isGhostMode()) {
                return;
            }
            int rad = (int)Math.ceil(volume * 10.0f);
            if (this.sneaking) {
                rad = Math.max(1, rad);
            }
            if (this.getCurrentSquare().getRoom() != null) {
                rad = (int)((float)rad * 0.5f);
            }
            int rand = 2;
            if (this.sneaking) {
                rand = Math.min(12, 4 + this.getPerkLevel(PerkFactory.Perks.Lightfoot));
            }
            if (Rand.Next(rand) == 0) {
                WorldSoundManager.instance.addSound(this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), rad, rad, false, 0.0f, 1.0f, false, false, false);
            }
        }
    }

    @Override
    public boolean Eat(InventoryItem info, float percentage) {
        return this.Eat(info, percentage, false);
    }

    public boolean EatOnClient(InventoryItem info, float percentage) {
        Object functionObj;
        if (!(info instanceof Food)) {
            return false;
        }
        Food food = (Food)info;
        if (food.getOnEat() != null && (functionObj = LuaManager.getFunctionObject(food.getOnEat())) != null) {
            LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, info, this, BoxedStaticValues.toDouble(percentage));
        }
        return true;
    }

    @Override
    public boolean Eat(InventoryItem info, float percentage, boolean useUtensil) {
        Object functionObj;
        if (!(info instanceof Food)) {
            return false;
        }
        Food food = (Food)info;
        float originalPercent = percentage = PZMath.clamp(percentage, 0.0f, 1.0f);
        if (food.getBaseHunger() != 0.0f && food.getHungChange() != 0.0f) {
            float hungChange = food.getBaseHunger() * percentage;
            float usedPercent = hungChange / food.getHungChange();
            percentage = usedPercent = PZMath.clamp(usedPercent, 0.0f, 1.0f);
        }
        if (food.getHungChange() < 0.0f && food.getHungChange() * (1.0f - percentage) > -0.01f) {
            percentage = 1.0f;
        }
        if (food.getHungChange() == 0.0f && food.getThirstChange() < 0.0f && food.getThirstChange() * (1.0f - percentage) > -0.01f) {
            percentage = 1.0f;
        }
        this.stats.add(CharacterStat.THIRST, food.getThirstChange() * percentage);
        this.stats.add(CharacterStat.HUNGER, food.getHungerChange() * percentage);
        this.stats.add(CharacterStat.ENDURANCE, food.getEnduranceChange() * percentage);
        this.stats.add(CharacterStat.STRESS, food.getStressChange() * percentage);
        this.stats.add(CharacterStat.FATIGUE, food.getFatigueChange() * percentage);
        IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
        if (player != null && !food.isBurnt()) {
            Nutrition nutrition = player.getNutrition();
            nutrition.setCalories(nutrition.getCalories() + food.getCalories() * percentage);
            nutrition.setCarbohydrates(nutrition.getCarbohydrates() + food.getCarbohydrates() * percentage);
            nutrition.setProteins(nutrition.getProteins() + food.getProteins() * percentage);
            nutrition.setLipids(nutrition.getLipids() + food.getLipids() * percentage);
        } else if (player != null && food.isBurnt()) {
            Nutrition nutrition = player.getNutrition();
            nutrition.setCalories(nutrition.getCalories() + food.getCalories() * percentage / 5.0f);
            nutrition.setCarbohydrates(nutrition.getCarbohydrates() + food.getCarbohydrates() * percentage / 5.0f);
            nutrition.setProteins(nutrition.getProteins() + food.getProteins() * percentage / 5.0f);
            nutrition.setLipids(nutrition.getLipids() + food.getLipids() * percentage / 5.0f);
        }
        this.getBodyDamage().setPainReduction(this.getBodyDamage().getPainReduction() + food.getPainReduction() * percentage);
        this.getBodyDamage().setColdReduction(this.getBodyDamage().getColdReduction() + (float)food.getFluReduction() * percentage);
        if (this.stats.isAboveMinimum(CharacterStat.FOOD_SICKNESS) && food.getFoodSicknessChange() < 0 && this.effectiveEdibleBuffTimer <= 0.0f) {
            float sicknessReductionAmount = (float)Math.abs(food.getFoodSicknessChange()) * percentage;
            this.stats.remove(CharacterStat.FOOD_SICKNESS, sicknessReductionAmount);
            this.stats.remove(CharacterStat.POISON, sicknessReductionAmount);
            this.effectiveEdibleBuffTimer = this.characterTraits.get(CharacterTrait.IRON_GUT) ? Rand.Next(80.0f, 150.0f) : (this.characterTraits.get(CharacterTrait.WEAK_STOMACH) ? Rand.Next(200.0f, 280.0f) : Rand.Next(120.0f, 230.0f));
        }
        this.getBodyDamage().JustAteFood(food, percentage, useUtensil);
        if (GameServer.server && this instanceof IsoPlayer) {
            INetworkPacket.send((IsoPlayer)this, PacketTypes.PacketType.SyncPlayerStats, this, SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.THIRST) + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.HUNGER) + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.ENDURANCE) + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.STRESS) + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.FATIGUE) + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.PAIN));
            GameServer.sendSyncPlayerFields((IsoPlayer)this, (byte)8);
            INetworkPacket.send((IsoPlayer)this, PacketTypes.PacketType.EatFood, this, food, Float.valueOf(percentage));
        }
        if (food.getOnEat() != null && (functionObj = LuaManager.getFunctionObject(food.getOnEat())) != null) {
            LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, info, this, BoxedStaticValues.toDouble(percentage));
        }
        if (percentage == 1.0f) {
            food.setHungChange(0.0f);
            food.UseAndSync();
        } else {
            float hungChange = food.getHungChange();
            float thirstChange = food.getThirstChange();
            food.multiplyFoodValues(1.0f - percentage);
            if (hungChange == 0.0f && thirstChange < 0.0f && food.getThirstChange() > -0.01f) {
                food.setHungChange(0.0f);
                food.UseAndSync();
                return true;
            }
            float emptyItemWeight = 0.0f;
            if (food.isCustomWeight()) {
                Item emptyItem;
                String fullType = food.getReplaceOnUseFullType();
                Item item = emptyItem = fullType == null ? null : ScriptManager.instance.getItem(fullType);
                if (emptyItem != null) {
                    emptyItemWeight = emptyItem.getActualWeight();
                }
                food.setWeight(food.getWeight() - emptyItemWeight - originalPercent * (food.getWeight() - emptyItemWeight) + emptyItemWeight);
            }
            food.syncItemFields();
        }
        return true;
    }

    @Override
    public boolean Eat(InventoryItem info) {
        return this.Eat(info, 1.0f);
    }

    @Override
    public boolean DrinkFluid(InventoryItem info, float percentage) {
        return this.DrinkFluid(info, percentage, false);
    }

    @Override
    public boolean DrinkFluid(InventoryItem info, float percentage, boolean useUtensil) {
        if (!info.hasComponent(ComponentType.FluidContainer)) {
            return false;
        }
        FluidContainer fluidCont = info.getFluidContainer();
        return this.DrinkFluid(fluidCont, percentage, useUtensil);
    }

    @Override
    public boolean DrinkFluid(FluidContainer fluidCont, float percentage) {
        return this.DrinkFluid(fluidCont, percentage, false);
    }

    @Override
    public boolean DrinkFluid(FluidContainer fluidCont, float percentage, boolean useUtensil) {
        IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
        if (player != null) {
            Nutrition nutrition = player.getNutrition();
            nutrition.setCalories(nutrition.getCalories() + fluidCont.getProperties().getCalories() * percentage);
            nutrition.setCarbohydrates(nutrition.getCarbohydrates() + fluidCont.getProperties().getCarbohydrates() * percentage);
            nutrition.setProteins(nutrition.getProteins() + fluidCont.getProperties().getProteins() * percentage);
            nutrition.setLipids(nutrition.getLipids() + fluidCont.getProperties().getLipids() * percentage);
        }
        boolean isTaintedWater = fluidCont.isTainted();
        boolean isBleach = fluidCont.getPrimaryFluid().getFluidType() == FluidType.Bleach;
        FluidConsume consume = fluidCont.removeFluid(fluidCont.getAmount() * percentage, true);
        this.stats.add(CharacterStat.THIRST, consume.getThirstChange());
        this.stats.add(CharacterStat.HUNGER, consume.getHungerChange());
        this.stats.add(CharacterStat.ENDURANCE, consume.getEnduranceChange());
        this.stats.add(CharacterStat.STRESS, consume.getStressChange());
        this.stats.add(CharacterStat.FATIGUE, consume.getFatigueChange());
        this.stats.add(CharacterStat.BOREDOM, consume.getUnhappyChange());
        this.stats.add(CharacterStat.UNHAPPINESS, consume.getUnhappyChange());
        float hungerChange = Math.abs(consume.getHungerChange());
        this.getBodyDamage().setHealthFromFoodTimer((int)(this.getBodyDamage().getHealthFromFoodTimer() + hungerChange * 13000.0f));
        if (consume.getAlcohol() > 0.0f) {
            this.getBodyDamage().JustDrankBoozeFluid(consume.getAlcohol());
        }
        this.getBodyDamage().setPainReduction(this.getBodyDamage().getPainReduction() + consume.getPainReduction());
        this.getBodyDamage().setColdReduction(this.getBodyDamage().getColdReduction() + consume.getFluReduction());
        float poisonModified = consume.getPoison();
        if (isTaintedWater) {
            poisonModified *= 0.75f;
        }
        if (this.characterTraits.get(CharacterTrait.IRON_GUT)) {
            if (isTaintedWater) {
                poisonModified = 0.0f;
            } else if (!isBleach) {
                poisonModified /= 2.0f;
            }
        }
        if (this.characterTraits.get(CharacterTrait.WEAK_STOMACH)) {
            poisonModified = isTaintedWater ? (poisonModified *= 1.2f) : (poisonModified *= 2.0f);
        }
        this.stats.add(CharacterStat.POISON, poisonModified);
        if (this.stats.isAboveMinimum(CharacterStat.FOOD_SICKNESS) && (float)consume.getFoodSicknessChange() > 0.0f && this.effectiveEdibleBuffTimer <= 0.0f) {
            this.stats.remove(CharacterStat.FOOD_SICKNESS, consume.getFoodSicknessChange());
            this.stats.remove(CharacterStat.POISON, consume.getFoodSicknessChange());
            this.effectiveEdibleBuffTimer = this.characterTraits.get(CharacterTrait.IRON_GUT) ? Rand.Next(80.0f, 150.0f) : (this.characterTraits.get(CharacterTrait.WEAK_STOMACH) ? Rand.Next(200.0f, 280.0f) : Rand.Next(120.0f, 230.0f));
        }
        if (GameServer.server && this instanceof IsoPlayer) {
            INetworkPacket.send((IsoPlayer)this, PacketTypes.PacketType.SyncPlayerStats, this, SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.INTOXICATION) + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.THIRST) + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.HUNGER) + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.ENDURANCE) + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.STRESS) + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.FATIGUE) + SyncPlayerStatsPacket.getBitMaskForStat(CharacterStat.BOREDOM));
        }
        return true;
    }

    @Override
    public boolean DrinkFluid(InventoryItem info) {
        return this.DrinkFluid(info, 1.0f);
    }

    public void FireCheck() {
        if (this.onFire) {
            return;
        }
        if (GameClient.client && this instanceof IsoPlayer) {
            return;
        }
        if (GameClient.client && this.isZombie() && this instanceof IsoZombie && ((IsoZombie)this).isRemoteZombie()) {
            return;
        }
        if (this.isZombie() && VirtualZombieManager.instance.isReused((IsoZombie)this)) {
            DebugLog.log(DebugType.Zombie, "FireCheck running on REUSABLE ZOMBIE - IGNORED " + String.valueOf(this));
            return;
        }
        if (this.getVehicle() != null) {
            return;
        }
        if (this.square != null && this.square.getProperties().has(IsoFlagType.burning) && (!GameClient.client || this.isZombie() && this.isLocal())) {
            if (this instanceof IsoPlayer && Rand.Next(Rand.AdjustForFramerate(70)) == 0 || this.isZombie() || this instanceof IsoAnimal) {
                this.SetOnFire();
            } else {
                if (!(this instanceof IsoPlayer)) {
                    float damage = this.fireKillRate * GameTime.instance.getMultiplier() / 2.0f;
                    CombatManager.getInstance().applyDamage(this, damage);
                    this.setAttackedBy(null);
                } else {
                    float dpm = this.fireKillRate * GameTime.instance.getThirtyFPSMultiplier() * 60.0f / 2.0f;
                    this.getBodyDamage().ReduceGeneralHealth(dpm);
                    LuaEventManager.triggerEvent("OnPlayerGetDamage", this, "FIRE", Float.valueOf(dpm));
                    this.getBodyDamage().OnFire(true);
                    this.forceAwake();
                }
                if (this.isDead()) {
                    IsoFireManager.RemoveBurningCharacter(this);
                    if (this.isZombie()) {
                        LuaEventManager.triggerEvent("OnZombieDead", this);
                        if (GameClient.client) {
                            this.setAttackedBy(IsoWorld.instance.currentCell.getFakeZombieForHit());
                        }
                    }
                }
            }
        }
    }

    public String getPrimaryHandType() {
        if (this.leftHandItem == null) {
            return null;
        }
        return this.leftHandItem.getType();
    }

    @Override
    public float getGlobalMovementMod(boolean bDoNoises) {
        if (this.getCurrentState() == ClimbOverFenceState.instance() || this.getCurrentState() == ClimbThroughWindowState.instance() || this.getCurrentState() == ClimbOverWallState.instance()) {
            return 1.0f;
        }
        return super.getGlobalMovementMod(bDoNoises);
    }

    public float getMovementSpeed() {
        IsoGameCharacter.tempo2.x = this.getX() - this.getLastX();
        IsoGameCharacter.tempo2.y = this.getY() - this.getLastY();
        return tempo2.getLength();
    }

    public String getSecondaryHandType() {
        if (this.rightHandItem == null) {
            return null;
        }
        return this.rightHandItem.getType();
    }

    public boolean HasItem(String string) {
        if (string == null) {
            return true;
        }
        return string.equals(this.getSecondaryHandType()) || string.equals(this.getPrimaryHandType()) || this.inventory.contains(string);
    }

    @Override
    public void changeState(State state) {
        this.stateMachine.changeState(state, null, false);
    }

    @Override
    public State getCurrentState() {
        return this.stateMachine.getCurrent();
    }

    @Override
    public boolean isCurrentState(State state) {
        if (this.stateMachine.isSubstate(state)) {
            return true;
        }
        return this.stateMachine.getCurrent() == state;
    }

    public boolean isCurrentGameClientState(State state) {
        if (!GameClient.client) {
            return false;
        }
        if (!this.isLocal()) {
            return false;
        }
        return this.isCurrentState(state);
    }

    public <T> void set(State.Param<T> state, T value) {
        state.set(this, value);
    }

    public <T> T get(State.Param<T> state) {
        return state.get(this);
    }

    public <T> T get(State.Param<T> state, T defaultT) {
        return (T)state.get(this, () -> defaultT);
    }

    public <T> T remove(State.Param<T> state) {
        return state.remove(this);
    }

    public void clear(State state) {
        this.clear(state.getClass());
    }

    public void clear(Class<? extends State> clazz) {
        this.getStateMachineParams(clazz).clear();
    }

    public Map<State.Param<?>, Object> getStateMachineParams(Class<?> clazz) {
        return this.stateMachineParams.computeIfAbsent(clazz, k -> new IdentityHashMap());
    }

    public void setStateMachineLocked(boolean val) {
        this.stateMachine.setLocked(val);
    }

    @Override
    public float Hit(HandWeapon weapon, IsoGameCharacter wielder, float damageSplit, boolean bIgnoreDamage, float modDelta) {
        return this.Hit(weapon, wielder, damageSplit, bIgnoreDamage, modDelta, false);
    }

    @Override
    public float Hit(HandWeapon weapon, IsoGameCharacter wielder, float damageSplit, boolean bIgnoreDamage, float modDelta, boolean bRemote) {
        IsoPlayer player;
        if (wielder == null || weapon == null) {
            return 0.0f;
        }
        if (weapon.isMelee() && !bIgnoreDamage && this.isZombie()) {
            IsoZombie zed = (IsoZombie)this;
            zed.setHitTime(zed.getHitTime() + 1);
            if (zed.getHitTime() >= 4 && !bRemote) {
                damageSplit *= ((float)zed.getHitTime() - 2.0f) * 1.5f;
            }
        }
        if (wielder instanceof IsoPlayer && (player = (IsoPlayer)wielder).isDoShove() && !wielder.isAimAtFloor()) {
            bIgnoreDamage = true;
            modDelta *= 1.5f;
        }
        LuaEventManager.triggerEvent("OnWeaponHitCharacter", wielder, this, weapon, Float.valueOf(damageSplit));
        LuaEventManager.triggerEvent("OnPlayerGetDamage", this, "WEAPONHIT", Float.valueOf(damageSplit));
        if (LuaHookManager.TriggerHook("WeaponHitCharacter", wielder, this, weapon, Float.valueOf(damageSplit))) {
            return 0.0f;
        }
        if (this.avoidDamage) {
            this.avoidDamage = false;
            return 0.0f;
        }
        if (this.noDamage) {
            bIgnoreDamage = true;
            this.noDamage = false;
        }
        if (this instanceof IsoSurvivor && !this.enemyList.contains(wielder)) {
            this.enemyList.add(wielder);
        }
        this.staggerTimeMod = weapon.getStaggerBackTimeMod(wielder, this);
        wielder.addWorldSoundUnlessInvisible(5, 1, false);
        this.calculateHitDirection(weapon, wielder);
        this.setAttackedBy(wielder);
        float damage = bRemote ? damageSplit : this.processHitDamage(weapon, wielder, damageSplit, bIgnoreDamage, modDelta);
        this.hitConsequences(weapon, wielder, bIgnoreDamage, damage, bRemote);
        return damage;
    }

    private void calculateHitDirection(HandWeapon handWeapon, IsoGameCharacter wielder) {
        this.hitDir.x = this.getX();
        this.hitDir.y = this.getY();
        float x = wielder.getX();
        float y = wielder.getY();
        IsoGridSquare attackTargetSquare = handWeapon.getAttackTargetSquare(null);
        if (attackTargetSquare != null) {
            x = attackTargetSquare.getX();
            y = attackTargetSquare.getY();
            if (DebugOptions.instance.character.debug.render.explosionHitDirection.getValue()) {
                LineDrawer.addAlphaDecayingIsoCircle(x, y, wielder.getZ(), 0.25f, 16, 0.0f, 0.0f, 1.0f, 1.0f);
            }
        }
        if (DebugOptions.instance.character.debug.render.explosionHitDirection.getValue()) {
            LineDrawer.addAlphaDecayingLine(x, y, wielder.getZ(), this.getX(), this.getY(), this.getZ(), 1.0f, 0.0f, 0.0f, 1.0f);
        }
        this.hitDir.x -= x;
        this.hitDir.y -= y;
        this.getHitDir().normalize();
        this.hitDir.x *= handWeapon.getPushBackMod();
        this.hitDir.y *= handWeapon.getPushBackMod();
        this.hitDir.rotate(handWeapon.hitAngleMod);
    }

    private float processInstantExplosionHitDamage(HandWeapon weapon, IsoGameCharacter wielder, float damage, boolean bIgnoreDamage, float modDelta) {
        return damage * modDelta;
    }

    public float processHitDamage(HandWeapon weapon, IsoGameCharacter wielder, float damageSplit, boolean bIgnoreDamage, float modDelta) {
        IsoPlayer isoPlayer;
        float f;
        if (weapon.isExplosive()) {
            return this.processInstantExplosionHitDamage(weapon, wielder, damageSplit, bIgnoreDamage, modDelta);
        }
        float damage = damageSplit;
        float dmgTest = damage *= modDelta;
        if (bIgnoreDamage) {
            dmgTest /= 2.7f;
        }
        if ((f = dmgTest * wielder.getShovingMod()) > 1.0f) {
            f = 1.0f;
        }
        this.setHitForce(f);
        if (wielder.characterTraits.get(CharacterTrait.STRONG) && !weapon.isRanged()) {
            this.setHitForce(this.getHitForce() * 1.4f);
        }
        if (wielder.characterTraits.get(CharacterTrait.WEAK) && !weapon.isRanged()) {
            this.setHitForce(this.getHitForce() * 0.6f);
        }
        float del = wielder.stats.get(CharacterStat.ENDURANCE);
        if ((del *= wielder.knockbackAttackMod) < 0.5f) {
            if ((del *= 1.3f) < 0.4f) {
                del = 0.4f;
            }
            this.setHitForce(this.getHitForce() * del);
        }
        if (wielder instanceof IsoPlayer && !bIgnoreDamage) {
            this.setHitForce(this.getHitForce() * 2.0f);
        }
        if (wielder instanceof IsoPlayer && !(isoPlayer = (IsoPlayer)wielder).isDoShove() && !bIgnoreDamage) {
            Vector2 oPos = tempVector2_1.set(this.getX(), this.getY());
            Vector2 tPos = tempVector2_2.set(wielder.getX(), wielder.getY());
            oPos.x -= tPos.x;
            oPos.y -= tPos.y;
            Vector2 dir = this.getVectorFromDirection(tempVector2_2);
            oPos.normalize();
            float dot = oPos.dot(dir);
            if (dot > -0.3f) {
                damage *= 1.5f;
            }
        }
        damage = CombatManager.getInstance().applyPlayerReceivedDamageModifier(this, damage);
        damage = CombatManager.getInstance().applyWeaponLevelDamageModifier(wielder, damage);
        if (wielder instanceof IsoPlayer) {
            IsoPlayer player = (IsoPlayer)wielder;
            if (wielder.isAimAtFloor() && !bIgnoreDamage && !player.isDoShove() && !weapon.isRanged()) {
                damage *= Math.max(5.0f, weapon.getCriticalDamageMultiplier());
            }
        }
        if (wielder.isCriticalHit() && !bIgnoreDamage) {
            damage *= Math.max(2.0f, weapon.getCriticalDamageMultiplier());
        }
        damage = CombatManager.getInstance().applyOneHandedDamagePenalty(wielder, weapon, damage);
        return damage;
    }

    public void hitConsequences(HandWeapon weapon, IsoGameCharacter wielder, boolean bIgnoreDamage, float damage, boolean bRemote) {
        if (!bIgnoreDamage) {
            damage = CombatManager.getInstance().applyGlobalDamageReductionMultipliers(weapon, damage);
            CombatManager.getInstance().applyDamage(this, damage);
        }
        if (this.isDead()) {
            if (!this.isOnKillDone()) {
                if (this instanceof IsoZombie) {
                    wielder.setZombieKills(wielder.getZombieKills() + 1);
                }
                if (GameServer.server) {
                    this.die();
                } else if (!GameClient.client) {
                    this.Kill(weapon, wielder);
                }
            }
            return;
        }
        if (weapon.isSplatBloodOnNoDeath()) {
            this.splatBlood(2, 0.2f);
        }
        if (weapon.isRanged()) {
            return;
        }
        if (weapon.isKnockBackOnNoDeath()) {
            if (GameServer.server) {
                if (wielder.xp != null) {
                    GameServer.addXp((IsoPlayer)wielder, PerkFactory.Perks.Strength, 2.0f);
                }
            } else if (!GameClient.client && wielder.xp != null) {
                wielder.xp.AddXP(PerkFactory.Perks.Strength, 2.0f);
            }
        }
    }

    public boolean IsAttackRange(float x, float y, float z) {
        InventoryItem attackItem;
        float maxrange = 1.0f;
        float minrange = 0.0f;
        if (this.leftHandItem != null && (attackItem = this.leftHandItem) instanceof HandWeapon) {
            HandWeapon handWeapon = (HandWeapon)attackItem;
            maxrange = handWeapon.getMaxRange(this);
            minrange = handWeapon.getMinRange();
            maxrange *= ((HandWeapon)this.leftHandItem).getRangeMod(this);
        }
        if (Math.abs(z - this.getZ()) > 0.3f) {
            return false;
        }
        float dist = IsoUtils.DistanceTo(x, y, this.getX(), this.getY());
        return dist < maxrange && dist > minrange;
    }

    public boolean isMeleeAttackRange(HandWeapon handWeapon, IsoMovingObject isoMovingObject, Vector3 bonePos) {
        if (handWeapon == null || handWeapon.isRanged()) {
            return false;
        }
        float deltaZ = Math.abs(isoMovingObject.getZ() - this.getZ());
        if (deltaZ >= 0.5f) {
            return false;
        }
        float range = handWeapon.getMaxRange(this);
        range *= handWeapon.getRangeMod(this);
        float distSq = IsoUtils.DistanceToSquared(this.getX(), this.getY(), bonePos.x, bonePos.y);
        IsoZombie zombie = Type.tryCastTo(isoMovingObject, IsoZombie.class);
        if (zombie != null && distSq < 4.0f && zombie.target == this && (zombie.isCurrentState(LungeState.instance()) || zombie.isCurrentState(LungeNetworkState.instance()))) {
            range += 0.2f;
        }
        return distSq < range * range;
    }

    @Override
    public boolean IsSpeaking() {
        return this.chatElement.IsSpeaking();
    }

    public boolean IsSpeakingNPC() {
        return this.chatElement.IsSpeakingNPC();
    }

    public void MoveForward(float dist, float x, float y, float soundDelta) {
        if (this.isCurrentState(SwipeStatePlayer.instance())) {
            return;
        }
        this.reqMovement.x = x;
        this.reqMovement.y = y;
        this.reqMovement.normalize();
        float mult = GameTime.instance.getMultiplier();
        this.setNextX(this.getNextX() + x * dist * mult);
        this.setNextY(this.getNextY() + y * dist * mult);
        this.DoFootstepSound(dist);
    }

    protected boolean CanUsePathfindState() {
        return !GameServer.server;
    }

    protected void pathToAux(float x, float y, float z) {
        boolean bLineClear = true;
        if (PZMath.fastfloor(z) == PZMath.fastfloor(this.getZ()) && IsoUtils.DistanceManhatten(x, y, this.getX(), this.getY()) <= 30.0f) {
            IsoChunk chunk;
            int chunkX = PZMath.fastfloor(x) / 8;
            int chunkY = PZMath.fastfloor(y) / 8;
            IsoChunk isoChunk = chunk = GameServer.server ? ServerMap.instance.getChunk(chunkX, chunkY) : IsoWorld.instance.currentCell.getChunkForGridSquare(PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z));
            if (chunk != null) {
                int flags = 1;
                if (this instanceof IsoAnimal) {
                    flags &= 0xFFFFFFFE;
                }
                flags |= 2;
                if (!this.isZombie()) {
                    flags |= 4;
                }
                boolean bl = bLineClear = !PolygonalMap2.instance.lineClearCollide(this.getX(), this.getY(), x, y, PZMath.fastfloor(z), this.getPathFindBehavior2().getTargetChar(), flags);
            }
        }
        if (bLineClear && this.current != null && this.current.HasStairs() && !this.current.isSameStaircase(PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z))) {
            bLineClear = false;
        }
        if (bLineClear) {
            if (this.CanUsePathfindState()) {
                this.setVariable("bPathfind", false);
            }
            this.setMoving(true);
        } else {
            if (this.CanUsePathfindState()) {
                this.setVariable("bPathfind", true);
            }
            this.setMoving(false);
        }
    }

    public void pathToCharacter(IsoGameCharacter target) {
        this.getPathFindBehavior2().pathToCharacter(target);
        this.pathToAux(target.getX(), target.getY(), target.getZ());
    }

    @Override
    public void pathToLocation(int x, int y, int z) {
        this.getPathFindBehavior2().pathToLocation(x, y, z);
        this.pathToAux((float)x + 0.5f, (float)y + 0.5f, z);
    }

    @Override
    public void pathToLocationF(float x, float y, float z) {
        this.getPathFindBehavior2().pathToLocationF(x, y, z);
        this.pathToAux(x, y, z);
    }

    public void pathToSound(int x, int y, int z) {
        this.getPathFindBehavior2().pathToSound(x, y, z);
        this.pathToAux((float)x + 0.5f, (float)y + 0.5f, z);
    }

    public boolean CanAttack() {
        if (this.isPerformingAttackAnimation() || this.getVariableBoolean("IsRacking") || this.getVariableBoolean("IsUnloading") || !StringUtils.isNullOrEmpty(this.getVariableString("RackWeapon"))) {
            return false;
        }
        if (GameClient.client && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
            if (this.isCurrentState(PlayerHitReactionState.instance())) {
                return false;
            }
            if (this.isCurrentState(PlayerHitReactionPVPState.instance()) && !ServerOptions.instance.pvpMeleeWhileHitReaction.getValue()) {
                return false;
            }
        }
        if (this.isSitOnGround()) {
            return false;
        }
        InventoryItem attackItem = this.leftHandItem;
        if (attackItem instanceof HandWeapon) {
            HandWeapon handWeapon = (HandWeapon)attackItem;
            if (attackItem.getSwingAnim() != null) {
                this.setUseHandWeapon(handWeapon);
            }
        }
        if (this.useHandWeapon == null) {
            return true;
        }
        if (this.useHandWeapon.getCondition() <= 0) {
            this.setUseHandWeapon(null);
            if (this.rightHandItem == this.leftHandItem) {
                this.setSecondaryHandItem(null);
            }
            this.setPrimaryHandItem(null);
            if (this.getInventory() != null) {
                this.getInventory().setDrawDirty(true);
            }
            return false;
        }
        if (!this.isWeaponReady()) {
            return false;
        }
        return !this.useHandWeapon.isCantAttackWithLowestEndurance() || this.isEnduranceSufficientForAction();
    }

    @Override
    public boolean isEnduranceSufficientForAction() {
        return !this.moodles.isMaxMoodleLevel(MoodleType.ENDURANCE);
    }

    public void ReduceHealthWhenBurning() {
        if (!this.onFire) {
            return;
        }
        if (this.isGodMod()) {
            this.StopBurning();
            return;
        }
        if (GameClient.client && this.isZombie() && this instanceof IsoZombie && ((IsoZombie)this).isRemoteZombie()) {
            return;
        }
        if (GameClient.client && this instanceof IsoPlayer && ((IsoPlayer)this).remote) {
            return;
        }
        if (this.isAlive()) {
            if (!(this instanceof IsoPlayer) || this instanceof IsoAnimal) {
                if (this.isZombie()) {
                    float damage = this.fireKillRate / 20.0f * GameTime.instance.getMultiplier();
                    CombatManager.getInstance().applyDamage(this, damage);
                    this.setAttackedBy(null);
                } else {
                    float damage = this.fireKillRate * GameTime.instance.getMultiplier();
                    if (this instanceof IsoAnimal) {
                        damage -= this.fireKillRate / 10.0f * GameTime.instance.getMultiplier();
                    }
                    CombatManager.getInstance().applyDamage(this, damage);
                }
            } else {
                float dpm = this.fireKillRate * GameTime.instance.getThirtyFPSMultiplier() * 60.0f;
                this.getBodyDamage().ReduceGeneralHealth(dpm);
                LuaEventManager.triggerEvent("OnPlayerGetDamage", this, "FIRE", Float.valueOf(dpm));
                this.getBodyDamage().OnFire(true);
            }
            if (this.isDead()) {
                IsoFireManager.RemoveBurningCharacter(this);
                if (this.isZombie()) {
                    LuaEventManager.triggerEvent("OnZombieDead", this);
                    if (GameClient.client) {
                        this.setAttackedBy(IsoWorld.instance.currentCell.getFakeZombieForHit());
                    }
                }
            }
        }
        if (this instanceof IsoPlayer && !(this instanceof IsoAnimal) && Rand.Next(Rand.AdjustForFramerate(((IsoPlayer)this).IsRunning() ? 150 : 400)) == 0) {
            this.StopBurning();
        }
    }

    @Deprecated
    public void DrawSneezeText() {
        if (this.getBodyDamage().IsSneezingCoughing() > 0) {
            IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
            String sneezeText = null;
            if (this.getBodyDamage().IsSneezingCoughing() == 1) {
                sneezeText = Translator.getText("IGUI_PlayerText_Sneeze");
                if (player != null) {
                    player.playerVoiceSound("SneezeHeavy");
                }
            }
            if (this.getBodyDamage().IsSneezingCoughing() == 2) {
                sneezeText = Translator.getText("IGUI_PlayerText_Cough");
                if (player != null) {
                    player.playerVoiceSound("Cough");
                }
            }
            if (this.getBodyDamage().IsSneezingCoughing() == 3) {
                sneezeText = Translator.getText("IGUI_PlayerText_SneezeMuffled");
                if (player != null) {
                    player.playerVoiceSound("SneezeLight");
                }
            }
            if (this.getBodyDamage().IsSneezingCoughing() == 4) {
                sneezeText = Translator.getText("IGUI_PlayerText_CoughMuffled");
                if (player != null) {
                    player.playerVoiceSound("MuffledCough");
                }
            }
            float sx = this.sx;
            float sy = this.sy;
            sx = (int)sx;
            sy = (int)sy;
            sx -= (float)((int)IsoCamera.getOffX());
            sy -= (float)((int)IsoCamera.getOffY());
            sy -= 48.0f;
            if (sneezeText != null) {
                TextManager.instance.DrawStringCentre(UIFont.Dialogue, (int)sx, (int)sy, sneezeText, this.speakColour.r, this.speakColour.g, this.speakColour.b, this.speakColour.a);
            }
        }
    }

    @Override
    public IsoSpriteInstance getSpriteDef() {
        if (this.def == null) {
            this.def = new IsoSpriteInstance();
        }
        return this.def;
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoChild, boolean bWallLightingPass, Shader shader) {
        int n;
        IsoGridSquare currentSquare;
        IsoGridSquare above;
        if (!this.getDoRender()) {
            return;
        }
        if (this.isAlphaAndTargetZero()) {
            return;
        }
        if (this.isSeatedInVehicle() && !this.getVehicle().showPassenger(this)) {
            return;
        }
        if (this.isSpriteInvisible()) {
            return;
        }
        if (this.isAlphaZero()) {
            return;
        }
        if (!this.useParts && this.def == null) {
            this.def = new IsoSpriteInstance(this.sprite);
        }
        IndieGL.glDepthMask(true);
        if (!PerformanceSettings.fboRenderChunk && this.doDefer && z - (float)PZMath.fastfloor(z) > 0.2f && (above = this.getCell().getGridSquare(PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z) + 1)) != null) {
            above.addDeferredCharacter(this);
        }
        if ((currentSquare = this.getCurrentSquare()) != null) {
            currentSquare.interpolateLight(inf, x - (float)currentSquare.getX(), y - (float)currentSquare.getY());
        } else {
            IsoGameCharacter.inf.r = col.r;
            IsoGameCharacter.inf.g = col.g;
            IsoGameCharacter.inf.b = col.b;
            IsoGameCharacter.inf.a = col.a;
        }
        if (Core.debug && DebugOptions.instance.pathfindRenderWaiting.getValue() && this.hasActiveModel()) {
            if (this.getCurrentState() == PathFindState.instance() && this.finder.progress == AStarPathFinder.PathFindProgress.notyetfound) {
                this.legsSprite.modelSlot.model.tintR = 1.0f;
                this.legsSprite.modelSlot.model.tintG = 0.0f;
                this.legsSprite.modelSlot.model.tintB = 0.0f;
            } else {
                this.legsSprite.modelSlot.model.tintR = 1.0f;
                this.legsSprite.modelSlot.model.tintG = 1.0f;
                this.legsSprite.modelSlot.model.tintB = 1.0f;
            }
        }
        if (this.dir == null) {
            this.dir = IsoDirections.N;
        }
        lastRenderedRendered = lastRendered;
        lastRendered = this;
        this.checkUpdateModelTextures();
        float scale = Core.tileScale;
        float offsetX = this.offsetX + 1.0f * scale;
        float offsetY = this.offsetY + -89.0f * scale;
        if (this.sprite != null) {
            this.def.setScale(scale, scale);
            if (!this.useParts) {
                this.sprite.render(this.def, this, x, y, z, this.dir, offsetX, offsetY, inf, true);
            } else if (this.legsSprite.hasActiveModel()) {
                this.legsSprite.renderActiveModel();
            } else if (!this.renderTextureInsteadOfModel(x, y)) {
                this.def.flip = false;
                IsoGameCharacter.inf.r = 1.0f;
                IsoGameCharacter.inf.g = 1.0f;
                IsoGameCharacter.inf.b = 1.0f;
                IsoGameCharacter.inf.a = this.def.alpha * 0.4f;
                this.legsSprite.renderCurrentAnim(this.def, this, x, y, z, this.dir, offsetX, offsetY, inf, false, null);
            }
        }
        if (this.attachedAnimSprite != null) {
            if (PerformanceSettings.fboRenderChunk) {
                FBORenderCell.instance.renderTranslucentOnly = true;
            }
            for (n = 0; n < this.attachedAnimSprite.size(); ++n) {
                IsoSpriteInstance spr = (IsoSpriteInstance)this.attachedAnimSprite.get(n);
                spr.update();
                float fa = IsoGameCharacter.inf.a;
                IsoGameCharacter.inf.a = spr.alpha;
                spr.SetTargetAlpha(this.getTargetAlpha());
                if (this.isOnFire()) {
                    spr.getParentSprite().soffX = (short)(this.offsetX + 1.0f * scale);
                    float sprX = this.getX();
                    float sprY = this.getY();
                    int offY = 40;
                    if (this.hasAnimationPlayer()) {
                        CombatManager.getBoneWorldPos(this, "Bip01_Spine1", tempVectorBonePos);
                        sprX = IsoGameCharacter.tempVectorBonePos.x;
                        sprY = IsoGameCharacter.tempVectorBonePos.y;
                        offY = (int)((IsoGameCharacter.tempVectorBonePos.z - this.getZ()) * 96.0f + (float)(this.isProne() ? 0 : 10));
                    }
                    spr.getParentSprite().soffY = (short)(this.offsetY + -89.0f * scale - (float)offY * scale);
                    float dxy = this.isProne() ? 0.25f : 0.5f;
                    float dz = 0.0f;
                    inf.set(1.0f, 1.0f, 1.0f, IsoGameCharacter.inf.a);
                    spr.render(this, sprX + dxy, sprY + dxy, z + 0.0f, IsoDirections.N, offsetX, offsetY, inf);
                } else {
                    spr.render(this, x, y, z, this.dir, offsetX, offsetY, inf);
                }
                IsoGameCharacter.inf.a = fa;
            }
            if (PerformanceSettings.fboRenderChunk) {
                FBORenderCell.instance.renderTranslucentOnly = false;
            }
        }
        for (n = 0; n < this.inventory.items.size(); ++n) {
            InventoryItem item = this.inventory.items.get(n);
            if (!(item instanceof IUpdater)) continue;
            IUpdater iUpdater = (IUpdater)((Object)item);
            iUpdater.render();
        }
        if (this.canRagdoll() && this.getRagdollController() != null) {
            this.getRagdollController().debugRender();
        }
        if (this.ballisticsController != null) {
            this.ballisticsController.debugRender();
        }
        if (this.ballisticsTarget != null) {
            this.ballisticsTarget.debugRender();
        }
    }

    public void renderServerGUI() {
        if (this instanceof IsoPlayer) {
            this.setSceneCulled(false);
        }
        if (this.updateModelTextures && this.hasActiveModel()) {
            this.updateModelTextures = false;
            this.textureCreator = ModelInstanceTextureCreator.alloc();
            this.textureCreator.init(this);
        }
        float scale = Core.tileScale;
        float offsetX = this.offsetX + 1.0f * scale;
        float offsetY = this.offsetY + -89.0f * scale;
        if (this.sprite != null) {
            this.def.setScale(scale, scale);
            IsoGameCharacter.inf.r = 1.0f;
            IsoGameCharacter.inf.g = 1.0f;
            IsoGameCharacter.inf.b = 1.0f;
            IsoGameCharacter.inf.a = this.def.alpha * 0.4f;
            if (!this.isbUseParts()) {
                this.sprite.render(this.def, this, this.getX(), this.getY(), this.getZ(), this.dir, offsetX, offsetY, inf, true);
            } else {
                this.def.flip = false;
                this.legsSprite.render(this.def, this, this.getX(), this.getY(), this.getZ(), this.dir, offsetX, offsetY, inf, true);
            }
        }
        if (Core.debug && this.hasActiveModel()) {
            if (this instanceof IsoZombie) {
                int sx = (int)IsoUtils.XToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
                int sy = (int)IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
                TextManager.instance.DrawString(sx, sy, "ID: " + this.getOnlineID());
                TextManager.instance.DrawString(sx, sy + 10, "State: " + this.getCurrentStateName());
                TextManager.instance.DrawString(sx, sy + 20, "Health: " + this.getHealth());
            }
            float maxRange = 2.0f;
            Vector2 dir = tempo;
            this.getDeferredMovement(dir);
            this.drawDirectionLine(dir, 1000.0f * dir.getLength() / GameTime.instance.getMultiplier() * 2.0f, 1.0f, 0.5f, 0.5f);
        }
    }

    @Override
    protected float getAlphaUpdateRateMul() {
        float mul = super.getAlphaUpdateRateMul();
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        if (isoGameCharacter.characterTraits.get(CharacterTrait.SHORT_SIGHTED)) {
            mul /= 2.0f;
        }
        if (isoGameCharacter.characterTraits.get(CharacterTrait.EAGLE_EYED)) {
            mul *= 1.5f;
        }
        return mul;
    }

    @Override
    protected boolean isUpdateAlphaDuringRender() {
        return false;
    }

    public boolean isSeatedInVehicle() {
        return this.vehicle != null && this.vehicle.getSeat(this) != -1;
    }

    @Override
    public void renderObjectPicker(float x, float y, float z, ColorInfo lightInfo) {
        if (!this.useParts) {
            this.sprite.renderObjectPicker(this.def, this, this.dir);
        } else {
            this.legsSprite.renderObjectPicker(this.def, this, this.dir);
        }
    }

    private static Vector2 closestpointonline(double lx1, double ly1, double lx2, double ly2, double x0, double y0, Vector2 out) {
        double cy;
        double cx;
        double a1 = ly2 - ly1;
        double b1 = lx1 - lx2;
        double c1 = (ly2 - ly1) * lx1 + (lx1 - lx2) * ly1;
        double c2 = -b1 * x0 + a1 * y0;
        double det = a1 * a1 - -b1 * b1;
        if (det != 0.0) {
            cx = (a1 * c1 - b1 * c2) / det;
            cy = (a1 * c2 - -b1 * c1) / det;
        } else {
            cx = x0;
            cy = y0;
        }
        return out.set((float)cx, (float)cy);
    }

    public ShadowParams calculateShadowParams(ShadowParams sp) {
        float f;
        if (!this.hasAnimationPlayer()) {
            return sp.set(0.45f, 1.4f, 1.125f);
        }
        IsoGameCharacter isoGameCharacter = this;
        if (isoGameCharacter instanceof IsoAnimal) {
            IsoAnimal animal = (IsoAnimal)isoGameCharacter;
            f = animal.getAnimalSize();
        } else {
            f = 1.0f;
        }
        float animalSize = f;
        return IsoGameCharacter.calculateShadowParams(this.getAnimationPlayer(), animalSize, false, sp);
    }

    public static ShadowParams calculateShadowParams(AnimationPlayer animationPlayer, float animalSize, boolean bRagdoll, ShadowParams sp) {
        float w = 0.45f;
        float fm = 1.4f;
        float bm = 1.125f;
        if (animationPlayer != null && animationPlayer.isReady()) {
            float x = 0.0f;
            float y = 0.0f;
            float z = 0.0f;
            Vector3 v = L_renderShadow.vector3;
            Model.boneToWorldCoords(animationPlayer, 0.0f, 0.0f, 0.0f, animalSize, animationPlayer.getSkinningBoneIndex("Bip01_Head", -1), v);
            float p1x = v.x;
            float p1y = v.y;
            Model.boneToWorldCoords(animationPlayer, 0.0f, 0.0f, 0.0f, animalSize, animationPlayer.getSkinningBoneIndex("Bip01_L_Foot", -1), v);
            float p2x = v.x;
            float p2y = v.y;
            Model.boneToWorldCoords(animationPlayer, 0.0f, 0.0f, 0.0f, animalSize, animationPlayer.getSkinningBoneIndex("Bip01_R_Foot", -1), v);
            float p3x = v.x;
            float p3y = v.y;
            if (bRagdoll) {
                Model.boneToWorldCoords(animationPlayer, 0.0f, 0.0f, 0.0f, animalSize, animationPlayer.getSkinningBoneIndex("Bip01_Pelvis", -1), v);
                p1x -= v.x;
                p1y -= v.y;
                p2x -= v.x;
                p2y -= v.y;
                p3x -= v.x;
                p3y -= v.y;
            }
            Vector3f vClosest = L_renderShadow.vector3f;
            float fLen = 0.0f;
            float bLen = 0.0f;
            Vector3f forward = L_renderShadow.forward;
            Vector2 forward2 = L_renderShadow.vector2_1.setLengthAndDirection(animationPlayer.getAngle(), 1.0f);
            forward.set(forward2.x, forward2.y, 0.0f);
            Vector2 closest = IsoGameCharacter.closestpointonline(0.0, 0.0, 0.0f + forward.x, 0.0f + forward.y, p1x, p1y, L_renderShadow.vector2_2);
            float cx = closest.x;
            float cy = closest.y;
            float cLen = closest.set(cx - 0.0f, cy - 0.0f).getLength();
            if (cLen > 0.001f) {
                vClosest.set(cx - 0.0f, cy - 0.0f, 0.0f).normalize();
                if (forward.dot(vClosest) > 0.0f) {
                    fLen = Math.max(fLen, cLen);
                } else {
                    bLen = Math.max(bLen, cLen);
                }
            }
            if ((cLen = (closest = IsoGameCharacter.closestpointonline(0.0, 0.0, 0.0f + forward.x, 0.0f + forward.y, p2x, p2y, L_renderShadow.vector2_2)).set((cx = closest.x) - 0.0f, (cy = closest.y) - 0.0f).getLength()) > 0.001f) {
                vClosest.set(cx - 0.0f, cy - 0.0f, 0.0f).normalize();
                if (forward.dot(vClosest) > 0.0f) {
                    fLen = Math.max(fLen, cLen);
                } else {
                    bLen = Math.max(bLen, cLen);
                }
            }
            if ((cLen = (closest = IsoGameCharacter.closestpointonline(0.0, 0.0, 0.0f + forward.x, 0.0f + forward.y, p3x, p3y, L_renderShadow.vector2_2)).set((cx = closest.x) - 0.0f, (cy = closest.y) - 0.0f).getLength()) > 0.001f) {
                vClosest.set(cx - 0.0f, cy - 0.0f, 0.0f).normalize();
                if (forward.dot(vClosest) > 0.0f) {
                    fLen = Math.max(fLen, cLen);
                } else {
                    bLen = Math.max(bLen, cLen);
                }
            }
            fm = (fLen + 0.35f) * 1.35f;
            bm = (bLen + 0.35f) * 1.35f;
        }
        return sp.set(0.45f, fm, bm);
    }

    public void renderShadow(float x, float y, float z) {
        RagdollStateData ragdollStateData;
        IsoZombie zombie;
        if (!Core.getInstance().isDisplayPlayerModel() && !this.isAnimal() && !this.isZombie() && this.isLocal()) {
            return;
        }
        if (this.isAlphaAndTargetZero()) {
            return;
        }
        if (this.isSeatedInVehicle()) {
            return;
        }
        IsoGridSquare currentSquare = this.getCurrentSquare();
        if (currentSquare == null) {
            return;
        }
        float heightAboveFloor = this.getHeightAboveFloor();
        if (heightAboveFloor > 0.5f) {
            return;
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        ShadowParams shadowParams = this.calculateShadowParams(L_renderShadow.shadowParams);
        float w = shadowParams.w;
        float fm = shadowParams.fm;
        float bm = shadowParams.bm;
        float alpha = this.getAlpha(playerIndex);
        IsoGameCharacter isoGameCharacter = this;
        if (isoGameCharacter instanceof IsoZombie && (zombie = (IsoZombie)isoGameCharacter).isSkeleton()) {
            alpha *= 0.5f;
        }
        if (heightAboveFloor > 0.0f) {
            alpha *= 1.0f - heightAboveFloor / 0.5f;
        }
        if (this.hasActiveModel() && this.hasAnimationPlayer() && this.getAnimationPlayer().isReady()) {
            float mult = 0.1f * GameTime.getInstance().getThirtyFPSMultiplier();
            mult = PZMath.clamp(mult, 0.0f, 1.0f);
            if (this.shadowTick != IngameState.instance.numberTicks - 1L) {
                this.shadowFm = fm;
                this.shadowBm = bm;
            }
            this.shadowTick = IngameState.instance.numberTicks;
            fm = this.shadowFm = PZMath.lerp(this.shadowFm, fm, mult);
            bm = this.shadowBm = PZMath.lerp(this.shadowBm, bm, mult);
        } else if (this.isZombie() && this.isCurrentState(FakeDeadZombieState.instance())) {
            alpha = 1.0f;
        } else if (this.isSceneCulled()) {
            return;
        }
        Vector2 animVector = this.getAnimVector(L_renderShadow.vector2_1);
        Vector3f forward = L_renderShadow.forward.set(animVector.x, animVector.y, 0.0f);
        if (this.getRagdollController() != null && (ragdollStateData = this.getRagdollController().getRagdollStateData()) != null && ragdollStateData.isCalculated) {
            forward.x = ragdollStateData.simulationDirection.x;
            forward.y = ragdollStateData.simulationDirection.y;
        }
        ColorInfo lightInfo = currentSquare.lighting[playerIndex].lightInfo();
        if (PerformanceSettings.fboRenderChunk) {
            FBORenderShadows.getInstance().addShadow(x, y, z - heightAboveFloor, forward, w, fm, bm, lightInfo.r, lightInfo.g, lightInfo.b, alpha, false);
            return;
        }
        IsoDeadBody.renderShadow(x, y, z - heightAboveFloor, forward, w, fm, bm, lightInfo, alpha);
    }

    public void checkUpdateModelTextures() {
        if (this.updateModelTextures && this.hasActiveModel()) {
            this.updateModelTextures = false;
            this.textureCreator = ModelInstanceTextureCreator.alloc();
            this.textureCreator.init(this);
        }
        if (this.updateEquippedTextures && this.hasActiveModel()) {
            this.updateEquippedTextures = false;
            if (this.primaryHandModel != null && this.primaryHandModel.getTextureInitializer() != null) {
                this.primaryHandModel.getTextureInitializer().setDirty();
            }
            if (this.secondaryHandModel != null && this.secondaryHandModel.getTextureInitializer() != null) {
                this.secondaryHandModel.getTextureInitializer().setDirty();
            }
        }
    }

    @Override
    public boolean isMaskClicked(int x, int y, boolean flip) {
        if (this.sprite == null) {
            return false;
        }
        if (!this.useParts) {
            return super.isMaskClicked(x, y, flip);
        }
        return this.legsSprite.isMaskClicked(this.dir, x, y, flip);
    }

    @Override
    public void setHaloNote(String str) {
        this.setHaloNote(str, this.haloDispTime);
    }

    @Override
    public void setHaloNote(String str, float dispTime) {
        this.setHaloNote(str, 0, 255, 0, dispTime);
    }

    @Override
    public void setHaloNote(String str, int r, int g, int b, float dispTime) {
        if (this.haloNote != null && str != null) {
            this.haloDispTime = dispTime;
            this.haloNote.setDefaultColors(r, g, b);
            this.haloNote.ReadString(str);
            this.haloNote.setInternalTickClock(this.haloDispTime);
        }
    }

    public float getHaloTimerCount() {
        if (this.haloNote != null) {
            return this.haloNote.getInternalClock();
        }
        return 0.0f;
    }

    public void DoSneezeText() {
        if (this.getBodyDamage() == null) {
            return;
        }
        if (this.getBodyDamage().IsSneezingCoughing() > 0) {
            IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
            String sneezeText = null;
            int sneezeVar = 0;
            if (this.getBodyDamage().IsSneezingCoughing() == 1) {
                sneezeText = Translator.getText("IGUI_PlayerText_Sneeze");
                sneezeVar = Rand.Next(2) + 1;
                this.setVariable("Ext", "Sneeze" + sneezeVar);
                if (player != null) {
                    player.playerVoiceSound("SneezeHeavy");
                }
            }
            if (this.getBodyDamage().IsSneezingCoughing() == 2) {
                sneezeText = Translator.getText("IGUI_PlayerText_Cough");
                this.setVariable("Ext", "Cough");
                if (player != null) {
                    player.playerVoiceSound("Cough");
                }
            }
            if (this.getBodyDamage().IsSneezingCoughing() == 3) {
                sneezeText = Translator.getText("IGUI_PlayerText_SneezeMuffled");
                sneezeVar = Rand.Next(2) + 1;
                this.setVariable("Ext", "Sneeze" + sneezeVar);
                if (player != null) {
                    player.playerVoiceSound("SneezeLight");
                }
            }
            if (this.getBodyDamage().IsSneezingCoughing() == 4) {
                sneezeText = Translator.getText("IGUI_PlayerText_CoughMuffled");
                this.setVariable("Ext", "Cough");
                if (player != null) {
                    player.playerVoiceSound("MuffledCough");
                }
            }
            if (sneezeText != null) {
                this.Say(sneezeText);
                this.reportEvent("EventDoExt");
                if (GameClient.client && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
                    GameClient.sendSneezingCoughing((IsoPlayer)this, this.getBodyDamage().IsSneezingCoughing(), (byte)sneezeVar);
                }
            }
        }
    }

    @Override
    public String getSayLine() {
        return this.chatElement.getSayLine();
    }

    public void setSayLine(String sayLine) {
        this.Say(sayLine);
    }

    public ChatMessage getLastChatMessage() {
        return this.lastChatMessage;
    }

    public void setLastChatMessage(ChatMessage lastChatMessage) {
        this.lastChatMessage = lastChatMessage;
    }

    public String getLastSpokenLine() {
        return this.lastSpokenLine;
    }

    public void setLastSpokenLine(String line) {
        this.lastSpokenLine = line;
    }

    protected void doSleepSpeech() {
        ++this.sleepSpeechCnt;
        if ((float)this.sleepSpeechCnt > (float)(250 * PerformanceSettings.getLockFPS()) / 30.0f) {
            this.sleepSpeechCnt = 0;
            if (sleepText == null) {
                sleepText = "ZzzZZZzzzz";
                ChatElement.addNoLogText(sleepText);
            }
            this.SayWhisper(sleepText);
        }
    }

    public void SayDebug(String text) {
        this.chatElement.SayDebug(0, text);
    }

    public void SayDebug(int n, String text) {
        this.chatElement.SayDebug(n, text);
    }

    public int getMaxChatLines() {
        return this.chatElement.getMaxChatLines();
    }

    @Override
    public void Say(String line) {
        if (this.isZombie()) {
            return;
        }
        this.ProcessSay(line, this.speakColour.r, this.speakColour.g, this.speakColour.b, 30.0f, 0, "default");
    }

    @Override
    public void Say(String line, float r, float g, float b, UIFont font, float baseRange, String customTag) {
        this.ProcessSay(line, r, g, b, baseRange, 0, customTag);
    }

    public void SayWhisper(String line) {
        this.ProcessSay(line, this.speakColour.r, this.speakColour.g, this.speakColour.b, 10.0f, 0, "whisper");
    }

    public void SayShout(String line) {
        this.ProcessSay(line, this.speakColour.r, this.speakColour.g, this.speakColour.b, 60.0f, 0, "shout");
    }

    public void SayRadio(String line, float r, float g, float b, UIFont font, float baseRange, int channel, String customTag) {
        this.ProcessSay(line, r, g, b, baseRange, channel, customTag);
    }

    private void ProcessSay(String line, float r, float g, float b, float baseRange, int channel, String customTag) {
        if (!this.allowConversation) {
            return;
        }
        if (TutorialManager.instance.profanityFilter) {
            line = ProfanityFilter.getInstance().filterString(line);
        }
        if (customTag.equals("default")) {
            ChatManager.getInstance().showInfoMessage(((IsoPlayer)this).getUsername(), line);
            this.lastSpokenLine = line;
        } else if (customTag.equals("whisper")) {
            this.lastSpokenLine = line;
        } else if (customTag.equals("shout")) {
            ChatManager.getInstance().sendMessageToChat(((IsoPlayer)this).getUsername(), ChatType.shout, line);
            this.lastSpokenLine = line;
        } else if (customTag.equals("radio")) {
            UIFont font = UIFont.Medium;
            boolean bbcode = true;
            boolean img = true;
            boolean icons = true;
            boolean colors = false;
            boolean fonts = false;
            boolean equalizeHeights = true;
            this.chatElement.addChatLine(line, r, g, b, font, baseRange, customTag, true, true, true, false, false, true);
            if (ZomboidRadio.isStaticSound(line)) {
                ChatManager.getInstance().showStaticRadioSound(line);
            } else {
                ChatManager.getInstance().showRadioMessage(line, channel);
            }
        }
    }

    public void addLineChatElement(String line) {
        this.addLineChatElement(line, 1.0f, 1.0f, 1.0f);
    }

    public void addLineChatElement(String line, float r, float g, float b) {
        this.addLineChatElement(line, r, g, b, UIFont.Dialogue, 30.0f, "default");
    }

    public void addLineChatElement(String line, float r, float g, float b, UIFont font, float baseRange, String customTag) {
        this.addLineChatElement(line, r, g, b, font, baseRange, customTag, false, false, false, false, false, true);
    }

    public void addLineChatElement(String line, float r, float g, float b, UIFont font, float baseRange, String customTag, boolean bbcode, boolean img, boolean icons, boolean colors, boolean fonts, boolean equalizeHeights) {
        this.chatElement.addChatLine(line, r, g, b, font, baseRange, customTag, bbcode, img, icons, colors, fonts, equalizeHeights);
    }

    protected boolean playerIsSelf() {
        return IsoPlayer.getInstance() == this;
    }

    public int getUserNameHeight() {
        if (!GameClient.client) {
            return 0;
        }
        if (this.userName != null) {
            return this.userName.getHeight();
        }
        return 0;
    }

    protected void initTextObjects() {
        this.hasInitTextObjects = true;
        if (this instanceof IsoPlayer && !(this instanceof IsoAnimal)) {
            this.chatElement.setMaxChatLines(5);
            if (IsoPlayer.getInstance() != null) {
                DebugLog.DetailedInfo.trace("FirstNAME:" + IsoPlayer.getInstance().username);
            }
            this.isoPlayer = (IsoPlayer)this;
            if (this.isoPlayer.username != null) {
                this.userName = new TextDrawObject();
                this.userName.setAllowAnyImage(true);
                this.userName.setDefaultFont(UIFont.Small);
                this.userName.setDefaultColors(255, 255, 255, 255);
                this.updateUserName();
            }
            if (this.haloNote == null) {
                this.haloNote = new TextDrawObject();
                this.haloNote.setDefaultFont(UIFont.Small);
                this.haloNote.setDefaultColors(0, 255, 0);
                this.haloNote.setDrawBackground(true);
                this.haloNote.setAllowImages(true);
                this.haloNote.setAllowAnyImage(true);
                this.haloNote.setOutlineColors(0.0f, 0.0f, 0.0f, 0.33f);
            }
        }
    }

    /*
     * Unable to fully structure code
     */
    protected void updateUserName() {
        block18: {
            if (this.userName == null || this.isoPlayer == null) break block18;
            this.isoPlayer.resetDisplayName();
            nameStr = this.isoPlayer.getDisplayName();
            if (!(this == IsoPlayer.getInstance() || !this.isInvisible() || IsoPlayer.getInstance() == null || IsoPlayer.getInstance().role == null || IsoPlayer.getInstance().role.hasCapability(Capability.CanSeePlayersStats) || Core.debug && DebugOptions.instance.cheat.player.seeEveryone.getValue())) {
                this.userName.ReadString("");
                return;
            }
            fact = Faction.getPlayerFaction(this.isoPlayer);
            if (fact != null) {
                if (this.isoPlayer.showTag || this.isoPlayer == IsoPlayer.getInstance() || Faction.getPlayerFaction(IsoPlayer.getInstance()) == fact) {
                    this.isoPlayer.tagPrefix = fact.getTag();
                    if (fact.getTagColor() != null) {
                        this.isoPlayer.setTagColor(fact.getTagColor());
                    }
                } else {
                    this.isoPlayer.tagPrefix = "";
                }
            } else {
                this.isoPlayer.tagPrefix = "";
            }
            isoGameCharacter = IsoCamera.getCameraCharacter();
            v0 = showMyUsername = this.isoPlayer != null && this.isoPlayer.remote != false || Core.getInstance().isShowYourUsername() != false;
            if (!GameClient.client || !(isoGameCharacter instanceof IsoPlayer)) ** GOTO lbl-1000
            player = (IsoPlayer)isoGameCharacter;
            if (player.role != null && player.role.hasCapability(Capability.CanSeePlayersStats)) {
                v1 = true;
            } else lbl-1000:
            // 2 sources

            {
                v1 = false;
            }
            bViewerIsAdmin = v1;
            v2 = canSeeAll = isoGameCharacter instanceof IsoPlayer != false && (player = (IsoPlayer)isoGameCharacter).canSeeAll() != false;
            if (!(ServerOptions.instance.displayUserName.getValue() || ServerOptions.instance.showFirstAndLastName.getValue() || canSeeAll)) {
                showMyUsername = false;
            }
            if (!showMyUsername) {
                nameStr = "";
            }
            if (showMyUsername && this.isoPlayer.tagPrefix != null && !this.isoPlayer.tagPrefix.isEmpty() && (!this.isDisguised() || bViewerIsAdmin)) {
                nameStr = "[col=" + (int)(this.isoPlayer.getTagColor().r * 255.0f) + "," + (int)(this.isoPlayer.getTagColor().g * 255.0f) + "," + (int)(this.isoPlayer.getTagColor().b * 255.0f) + "][" + this.isoPlayer.tagPrefix + "][/] " + (String)nameStr;
            }
            if (showMyUsername && this.isoPlayer.role != null && this.isoPlayer.role.hasCapability(Capability.CanSeePlayersStats) && this.isoPlayer.isShowAdminTag()) {
                nameStr = String.format("[col=%d,%d,%d]%s[/] ", new Object[]{(int)(this.isoPlayer.role.getColor().getR() * 255.0f), (int)(this.isoPlayer.role.getColor().getG() * 255.0f), (int)(this.isoPlayer.role.getColor().getB() * 255.0f), this.isoPlayer.role.getName()}) + (String)nameStr;
            }
            if (showMyUsername && this.checkPVP()) {
                namePvpSuffix1 = " [img=media/ui/Skull1.png]";
                if (this.isoPlayer.getSafety().getToggle() == 0.0f) {
                    namePvpSuffix1 = " [img=media/ui/Skull2.png]";
                }
                nameStr = (String)nameStr + namePvpSuffix1;
            }
            if (this.isoPlayer.isSpeek && !this.isoPlayer.isVoiceMute) {
                nameStr = "[img=media/ui/voiceon.png] " + (String)nameStr;
            }
            if (this.isoPlayer.isVoiceMute) {
                nameStr = "[img=media/ui/voicemuted.png] " + (String)nameStr;
            }
            v3 = vehicle = isoGameCharacter == this.isoPlayer ? this.isoPlayer.getNearVehicle() : null;
            if (this.getVehicle() == null && vehicle != null && (this.isoPlayer.getInventory().haveThisKeyId(vehicle.getKeyId()) != null || vehicle.isHotwired() || SandboxOptions.getInstance().vehicleEasyUse.getValue())) {
                newC = Color.HSBtoRGB(vehicle.colorHue, vehicle.colorSaturation * 0.5f, vehicle.colorValue);
                nameStr = " [img=media/ui/CarKey.png," + newC.getRedByte() + "," + newC.getGreenByte() + "," + newC.getBlueByte() + "]" + (String)nameStr;
            }
            if (!nameStr.equals(this.userName.getOriginal())) {
                this.userName.ReadString((String)nameStr);
            }
        }
    }

    private boolean checkPVP() {
        if (this.isoPlayer.getSafety().isEnabled()) {
            return false;
        }
        if (!ServerOptions.instance.showSafety.getValue()) {
            return false;
        }
        if (NonPvpZone.getNonPvpZone(PZMath.fastfloor(this.isoPlayer.getX()), PZMath.fastfloor(this.isoPlayer.getY())) != null) {
            return false;
        }
        if (IsoPlayer.getInstance() != this.isoPlayer && Faction.isInSameFaction(IsoPlayer.getInstance(), this.isoPlayer)) {
            return this.isoPlayer.isFactionPvp();
        }
        return true;
    }

    public void updateTextObjects() {
        if (GameServer.server) {
            return;
        }
        if (!this.hasInitTextObjects) {
            this.initTextObjects();
        }
        if (!this.speaking) {
            this.DoSneezeText();
            if (this.isAsleep() && this.getCurrentSquare() != null && this.getCurrentSquare().getCanSee(0)) {
                this.doSleepSpeech();
            }
        }
        if (this.isoPlayer != null) {
            this.radioEquipedCheck();
        }
        this.speaking = false;
        this.drawUserName = false;
        this.canSeeCurrent = false;
        if (this.haloNote != null && this.haloNote.getInternalClock() > 0.0f) {
            this.haloNote.updateInternalTickClock();
        }
        this.legsSprite.PlayAnim("ZombieWalk1");
        this.chatElement.update();
        this.speaking = this.chatElement.IsSpeaking();
        if (!this.speaking || this.isDead()) {
            this.speaking = false;
            this.callOut = false;
        }
    }

    /*
     * Unable to fully structure code
     */
    @Override
    public void renderlast() {
        block29: {
            super.renderlast();
            playerIndex = IsoCamera.frameState.playerIndex;
            renderX = this.getX();
            renderY = this.getY();
            if (this.sx == 0.0f && this.def != null) {
                this.sx = IsoUtils.XToScreen(renderX + this.def.offX, renderY + this.def.offY, this.getZ() + this.def.offZ, 0);
                this.sy = IsoUtils.YToScreen(renderX + this.def.offX, renderY + this.def.offY, this.getZ() + this.def.offZ, 0);
                this.sx -= this.offsetX - 8.0f;
                this.sy -= this.offsetY - 60.0f;
            }
            if ((!this.hasInitTextObjects || this.isoPlayer == null) && !this.chatElement.getHasChatToDisplay()) break block29;
            sx = IsoUtils.XToScreen(renderX, renderY, this.getZ(), 0);
            sy = IsoUtils.YToScreen(renderX, renderY, this.getZ(), 0);
            sx = sx - IsoCamera.getOffX() - this.offsetX;
            sy = sy - IsoCamera.getOffY() - this.offsetY;
            sy -= (float)(128 / (2 / Core.tileScale));
            zoom = Core.getInstance().getZoom(playerIndex);
            sx /= zoom;
            sy /= zoom;
            sx += IsoCamera.cameras[IsoCamera.frameState.playerIndex].fixJigglyModelsX;
            sy += IsoCamera.cameras[IsoCamera.frameState.playerIndex].fixJigglyModelsY;
            this.canSeeCurrent = true;
            this.drawUserName = false;
            if (this.isoPlayer != null && (this == IsoCamera.frameState.camCharacter || this.getCurrentSquare() != null && this.getCurrentSquare().getCanSee(playerIndex)) || IsoPlayer.getInstance().canSeeAll()) {
                if (this == IsoPlayer.getInstance()) {
                    this.canSeeCurrent = true;
                }
                if (GameClient.client && this.userName != null && !(this instanceof IsoAnimal)) {
                    this.drawUserName = false;
                    if (ServerOptions.getInstance().mouseOverToSeeDisplayName.getValue() && this != IsoPlayer.getInstance() && !IsoPlayer.getInstance().canSeeAll()) {
                        object = IsoObjectPicker.Instance.ContextPick(Mouse.getXA(), Mouse.getYA());
                        if (object != null && object.tile != null) {
                            block0: for (x = object.tile.square.getX() - 1; x < object.tile.square.getX() + 2; ++x) {
                                for (y = object.tile.square.getY() - 1; y < object.tile.square.getY() + 2; ++y) {
                                    sq = IsoCell.getInstance().getGridSquare(x, y, object.tile.square.getZ());
                                    if (sq != null) {
                                        for (i = 0; i < sq.getMovingObjects().size(); ++i) {
                                            obj = sq.getMovingObjects().get(i);
                                            if (this != obj || !(obj instanceof IsoPlayer) || (isoPlayer = (IsoPlayer)obj).getTargetAlpha(isoPlayer.playerIndex) != 1.0f) continue;
                                            this.drawUserName = true;
                                            break;
                                        }
                                        if (this.drawUserName) continue block0;
                                    }
                                    if (this.drawUserName) continue block0;
                                }
                            }
                        }
                    } else {
                        this.drawUserName = true;
                    }
                    if (this.drawUserName) {
                        this.updateUserName();
                    }
                }
                if (!GameClient.client && this.isoPlayer != null && !this.isAnimal() && this.isoPlayer.getVehicle() == null) {
                    nameStr = "";
                    vehicle = this.isoPlayer.getNearVehicle();
                    if (this.getVehicle() == null && vehicle != null && vehicle.getPartById("Engine") != null && (this.isoPlayer.getInventory().haveThisKeyId(vehicle.getKeyId()) != null || vehicle.isHotwired() || SandboxOptions.getInstance().vehicleEasyUse.getValue()) && UIManager.visibleAllUi) {
                        newC = Color.HSBtoRGB(vehicle.colorHue, vehicle.colorSaturation * 0.5f, vehicle.colorValue, L_renderLast.color);
                        nameStr = " [img=media/ui/CarKey.png," + newC.getRedByte() + "," + newC.getGreenByte() + "," + newC.getBlueByte() + "]";
                    }
                    if (!nameStr.isEmpty()) {
                        this.userName.ReadString((String)nameStr);
                        this.drawUserName = true;
                    }
                }
            }
            if (this.isoPlayer != null && this.hasInitTextObjects && (this.playerIsSelf() || this.canSeeCurrent)) {
                if (this.canSeeCurrent && this.drawUserName) {
                    this.userName.AddBatchedDraw((int)sx, (int)(sy -= (float)this.userName.getHeight()), true);
                }
                if (this.playerIsSelf() && (bar = UIManager.getProgressBar(playerIndex)) != null && bar.isVisible().booleanValue()) {
                    sy -= (float)(bar.getHeight().intValue() + 2);
                }
                if (this.playerIsSelf() && this.haloNote != null && this.haloNote.getInternalClock() > 0.0f) {
                    alp = this.haloNote.getInternalClock() / (this.haloDispTime / 4.0f);
                    alp = PZMath.min(alp, 1.0f);
                    this.haloNote.AddBatchedDraw((int)sx, (int)(sy -= (float)(this.haloNote.getHeight() + 2)), true, alp);
                }
            }
            ignoreRadioLines = false;
            if (IsoPlayer.getInstance() != this && this.equipedRadio != null && this.equipedRadio.getDeviceData() != null && this.equipedRadio.getDeviceData().getHeadphoneType() >= 0) {
                ignoreRadioLines = true;
            }
            if (this.equipedRadio != null && this.equipedRadio.getDeviceData() != null && !this.equipedRadio.getDeviceData().getIsTurnedOn()) {
                ignoreRadioLines = true;
            }
            isoGameCharacter = IsoCamera.getCameraCharacter();
            if (!GameClient.client || !(isoGameCharacter instanceof IsoPlayer)) ** GOTO lbl-1000
            player = (IsoPlayer)isoGameCharacter;
            if (player.role.hasCapability(Capability.CanSeePlayersStats)) {
                v0 = true;
            } else lbl-1000:
            // 2 sources

            {
                v0 = bViewerIsAdmin = false;
            }
            if (!this.isInvisible() || this == IsoCamera.frameState.camCharacter || bViewerIsAdmin) {
                this.chatElement.renderBatched(IsoPlayer.getPlayerIndex(), (int)sx, (int)sy, ignoreRadioLines);
            }
        }
        if (this instanceof IsoPlayer) {
            IsoBulletTracerEffects.getInstance().render();
        }
        if (this.inventory != null) {
            for (n = 0; n < this.inventory.items.size(); ++n) {
                item = this.inventory.items.get(n);
                if (!(item instanceof IUpdater)) continue;
                iUpdater = (IUpdater)item;
                iUpdater.renderlast();
            }
        }
        if (this.getIsNPC() && this.ai.brain != null) {
            this.ai.brain.renderlast();
        }
        if (Core.debug) {
            this.debugRenderLast();
        }
    }

    private void debugRenderLast() {
        Cloneable dir;
        float maxRange = 2.0f;
        if (DebugOptions.instance.character.debug.render.angle.getValue() && this.hasActiveModel()) {
            dir = tempo;
            ((Vector2)dir).set(this.dir.ToVector());
            this.drawDirectionLine((Vector2)dir, 2.4f, 0.0f, 1.0f, 0.0f);
            ((Vector2)dir).setLengthAndDirection(this.getLookAngleRadians(), 1.0f);
            this.drawDirectionLine((Vector2)dir, 2.0f, 1.0f, 1.0f, 1.0f);
            ((Vector2)dir).setLengthAndDirection(this.getAnimAngleRadians(), 1.0f);
            this.drawDirectionLine((Vector2)dir, 2.0f, 1.0f, 1.0f, 0.0f);
            float angle = this.getDirectionAngleRadians();
            ((Vector2)dir).setLengthAndDirection(angle, 1.0f);
            this.drawDirectionLine((Vector2)dir, 2.0f, 0.0f, 0.0f, 1.0f);
        }
        if (DebugOptions.instance.character.debug.render.deferredMovement.getValue() && this.hasActiveModel()) {
            dir = tempo;
            this.getDeferredMovement((Vector2)dir);
            this.drawDirectionLine((Vector2)dir, 1000.0f * ((Vector2)dir).getLength() / GameTime.instance.getMultiplier() * 2.0f, 1.0f, 0.5f, 0.5f);
        }
        if (DebugOptions.instance.character.debug.render.deferredMovement.getValue() && this.hasActiveModel()) {
            dir = tempo3;
            this.getDeferredMovementFromRagdoll((Vector3)dir);
            this.drawDirectionLine((Vector3)dir, 1000.0f * ((Vector3)dir).getLength() / GameTime.instance.getMultiplier() * 2.0f, 0.0f, 1.0f, 0.5f);
        }
        if (DebugOptions.instance.character.debug.render.deferredAngles.getValue() && this.hasActiveModel()) {
            dir = tempo;
            this.getDeferredMovement((Vector2)dir);
            this.drawDirectionLine((Vector2)dir, 1000.0f * ((Vector2)dir).getLength() / GameTime.instance.getMultiplier() * 2.0f, 1.0f, 0.5f, 0.5f);
        }
        if (DebugOptions.instance.character.debug.render.aimCone.getValue()) {
            this.debugAim();
        }
        if (DebugOptions.instance.character.debug.render.testDotSide.getValue()) {
            this.debugTestDotSide();
        }
        if (DebugOptions.instance.character.debug.render.vision.getValue()) {
            this.debugVision();
        }
        if (DebugOptions.instance.character.debug.render.climbRope.getValue()) {
            ClimbSheetRopeState.instance().debug(this);
        }
        if (this.getNetworkCharacterAI().getBooleanDebugOptions().enable.getValue() || GameServer.server && GameServer.guiCommandline) {
            this.renderDebugData();
        }
        if (DebugOptions.instance.pathfindRenderPath.getValue() && this.pfb2 != null) {
            this.pfb2.render();
        }
        if (DebugOptions.instance.collideWithObstacles.render.radius.getValue()) {
            float radius = 0.3f;
            float r = 1.0f;
            float g = 1.0f;
            float b = 1.0f;
            if (!this.isCollidable()) {
                b = 0.0f;
            }
            if (PZMath.fastfloor(this.getZ()) != PZMath.fastfloor(IsoCamera.frameState.camCharacterZ)) {
                b = 0.5f;
                g = 0.5f;
                r = 0.5f;
            }
            LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), 0.3f, 16, r, g, b, 1.0f);
        }
        if (DebugOptions.instance.animation.debug.getValue() && this.hasActiveModel() && !(this instanceof IsoAnimal)) {
            IndieGL.glBlendFunc(770, 771);
            IndieGL.disableDepthTest();
            IndieGL.StartShader(0);
            int sx = (int)IsoUtils.XToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
            int sy = (int)IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ() + 1.0f, 0);
            TextManager.instance.DrawString(UIFont.Dialogue, sx, sy, 1.0, this.getAnimationDebug(), 1.0, 1.0, 1.0, 1.0);
        }
        if (DebugOptions.instance.statistics.displayAllDebugStatistics.getValue() && this instanceof IsoPlayer) {
            IndieGL.disableDepthTest();
            IndieGL.StartShader(0);
            int sx = (int)IsoUtils.XToScreenExact(this.getX() + 1.0f, this.getY(), this.getZ(), 0);
            int sy = (int)IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ() + 1.0f, 0);
            TextManager.instance.DrawString(UIFont.Dialogue, sx, sy, this.getStatisticsDebug(), 0.0, 1.0, 0.0, 1.0);
        }
        if (DebugOptions.instance.character.debug.render.carStopDebug.getValue() && this.hasActiveModel()) {
            Vector2 dir2 = tempo;
            if (this.vehicle != null && this.vbdebugHitTarget != null) {
                float maxAngle = 0.3f;
                float maxLength = 6.0f;
                Vector2 carSpeed = this.calcCarSpeedVector();
                boolean movingBackward = this.carMovingBackward(carSpeed);
                Vector2 offset = this.calcCarPositionOffset(movingBackward);
                Vector2 vectorCarToPlayer = this.calcCarToPlayerVector(this.vbdebugHitTarget, offset);
                carSpeed = this.calcCarSpeedVector(offset);
                float lengthMultiplier = this.calcLengthMultiplier(carSpeed, movingBackward);
                float angleMultiplier = this.calcConeAngleMultiplier(this.vbdebugHitTarget, movingBackward);
                float angleOffset = this.calcConeAngleOffset(this.vbdebugHitTarget, movingBackward);
                maxAngle *= angleMultiplier;
                float angle = carSpeed.getDirection();
                dir2.setLengthAndDirection(angle -= angleOffset, maxLength += lengthMultiplier);
                Vector2 startPosition = new Vector2();
                startPosition.x = this.vehicle.getX() + offset.x;
                startPosition.y = this.vehicle.getY() + offset.y;
                this.drawLine(startPosition, dir2, 2.0f, 1.0f, 0.0f, 1.0f);
                float length = (float)Math.cos(maxAngle) * maxLength;
                dir2.setLengthAndDirection(angle + maxAngle, length);
                this.drawLine(startPosition, dir2, 2.0f, 1.0f, 0.0f, 0.0f);
                dir2.setLengthAndDirection(angle - maxAngle, length);
                this.drawLine(startPosition, dir2, 2.0f, 1.0f, 0.0f, 0.0f);
                angle = vectorCarToPlayer.getDirection();
                dir2.setLengthAndDirection(angle, vectorCarToPlayer.getLength() / 2.0f);
                this.drawLine(startPosition, dir2, 2.0f, 0.0f, 0.0f, 1.0f);
            }
        }
        if (DebugOptions.instance.character.debug.render.aimVector.getValue() && this.ballisticsController != null) {
            this.ballisticsController.renderlast();
        }
    }

    public void drawLine(Vector2 startPos, Vector2 dir, float length, float r, float g, float b) {
        float x2 = startPos.x + dir.x * length;
        float y2 = startPos.y + dir.y * length;
        float sx = IsoUtils.XToScreenExact(startPos.x, startPos.y, this.getZ(), 0);
        float sy = IsoUtils.YToScreenExact(startPos.x, startPos.y, this.getZ(), 0);
        float sx2 = IsoUtils.XToScreenExact(x2, y2, this.getZ(), 0);
        float sy2 = IsoUtils.YToScreenExact(x2, y2, this.getZ(), 0);
        LineDrawer.drawLine(sx, sy, sx2, sy2, r, g, b, 0.5f, 1);
    }

    public Vector2 calcCarForwardVector() {
        Vector3f forward = this.vehicle.getForwardVector(BaseVehicle.allocVector3f());
        Vector2 forward2 = new Vector2().set(forward.x, forward.z);
        BaseVehicle.releaseVector3f(forward);
        return forward2;
    }

    public boolean carMovingBackward(Vector2 carSpeed) {
        float sx = carSpeed.x;
        float sy = carSpeed.y;
        carSpeed.normalize();
        boolean bBackward = this.calcCarForwardVector().dot(carSpeed) < 0.0f;
        carSpeed.set(sx, sy);
        return bBackward;
    }

    public Vector2 calcCarPositionOffset(boolean movingBackward) {
        Vector2 offset;
        if (!movingBackward) {
            offset = this.calcCarForwardVector().setLength(1.0f);
            offset.x *= -1.0f;
            offset.y *= -1.0f;
        } else {
            offset = this.calcCarForwardVector().setLength(2.0f);
        }
        return offset;
    }

    public float calcLengthMultiplier(Vector2 carSpeed, boolean movingBackward) {
        float multiplier = movingBackward ? carSpeed.getLength() : carSpeed.getLength();
        if (multiplier < 1.0f) {
            multiplier = 1.0f;
        }
        return multiplier;
    }

    public Vector2 calcCarSpeedVector(Vector2 offset) {
        Vector2 carSpeed = this.calcCarSpeedVector();
        carSpeed.x -= offset.x;
        carSpeed.y -= offset.y;
        return carSpeed;
    }

    public Vector2 calcCarSpeedVector() {
        Vector2 carSpeed = new Vector2();
        Vector3f linearVelocity = this.vehicle.getLinearVelocity(BaseVehicle.allocVector3f());
        carSpeed.x = linearVelocity.x;
        carSpeed.y = linearVelocity.z;
        BaseVehicle.releaseVector3f(linearVelocity);
        return carSpeed;
    }

    public Vector2 calcCarToPlayerVector(IsoGameCharacter target, Vector2 offset) {
        Vector2 vectorCarToPlayer = new Vector2();
        vectorCarToPlayer.x = target.getX() - this.vehicle.getX();
        vectorCarToPlayer.y = target.getY() - this.vehicle.getY();
        vectorCarToPlayer.x -= offset.x;
        vectorCarToPlayer.y -= offset.y;
        return vectorCarToPlayer;
    }

    public Vector2 calcCarToPlayerVector(IsoGameCharacter target) {
        Vector2 vectorCarToPlayer = new Vector2();
        vectorCarToPlayer.x = target.getX() - this.vehicle.getX();
        vectorCarToPlayer.y = target.getY() - this.vehicle.getY();
        return vectorCarToPlayer;
    }

    public float calcConeAngleOffset(IsoGameCharacter target, boolean movingBackward) {
        float angleOffset = 0.0f;
        if (movingBackward && (this.vehicle.getCurrentSteering() > 0.1f || this.vehicle.getCurrentSteering() < -0.1f)) {
            angleOffset = this.vehicle.getCurrentSteering() * 0.3f;
        }
        if (!movingBackward && (this.vehicle.getCurrentSteering() > 0.1f || this.vehicle.getCurrentSteering() < -0.1f)) {
            angleOffset = this.vehicle.getCurrentSteering() * 0.3f;
        }
        return angleOffset;
    }

    public float calcConeAngleMultiplier(IsoGameCharacter target, boolean movingBackward) {
        float angleMultiplier = 0.0f;
        if (movingBackward && (this.vehicle.getCurrentSteering() > 0.1f || this.vehicle.getCurrentSteering() < -0.1f)) {
            angleMultiplier = this.vehicle.getCurrentSteering() * 3.0f;
        }
        if (!movingBackward && (this.vehicle.getCurrentSteering() > 0.1f || this.vehicle.getCurrentSteering() < -0.1f)) {
            angleMultiplier = this.vehicle.getCurrentSteering() * 2.0f;
        }
        if (this.vehicle.getCurrentSteering() < 0.0f) {
            angleMultiplier *= -1.0f;
        }
        if (angleMultiplier < 1.0f) {
            angleMultiplier = 1.0f;
        }
        return angleMultiplier;
    }

    protected boolean renderTextureInsteadOfModel(float x, float y) {
        return false;
    }

    public void drawDirectionLine(Vector2 dir, float length, float r, float g, float b) {
        float x2 = this.getX() + dir.x * length;
        float y2 = this.getY() + dir.y * length;
        float sx = IsoUtils.XToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
        float sy = IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
        float sx2 = IsoUtils.XToScreenExact(x2, y2, this.getZ(), 0);
        float sy2 = IsoUtils.YToScreenExact(x2, y2, this.getZ(), 0);
        SpriteRenderer.instance.StartShader(0, IsoCamera.frameState.playerIndex);
        IndieGL.disableDepthTest();
        LineDrawer.drawLine(sx, sy, sx2, sy2, r, g, b, 0.5f, 1);
    }

    public void drawDirectionLine(Vector3 dir, float length, float r, float g, float b) {
        float x2 = this.getX() + dir.x * length;
        float y2 = this.getY() + dir.y * length;
        float z2 = this.getZ() + dir.z * length;
        float sx = IsoUtils.XToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
        float sy = IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
        float sz = IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
        float sx2 = IsoUtils.XToScreenExact(x2, y2, z2, 0);
        float sy2 = IsoUtils.YToScreenExact(x2, y2, z2, 0);
        float sz2 = IsoUtils.YToScreenExact(x2, y2, z2, 0);
        SpriteRenderer.instance.StartShader(0, IsoCamera.frameState.playerIndex);
        IndieGL.disableDepthTest();
        LineDrawer.drawLine(sx, sy, sx2, sy2, r, g, b, 0.5f, 1);
    }

    public void drawDebugTextBelow(String text) {
        int boxWidth = TextManager.instance.MeasureStringX(UIFont.Small, text) + 32;
        int fontHeight = TextManager.instance.MeasureStringY(UIFont.Small, text);
        int boxHeight = (int)Math.ceil((double)fontHeight * 1.25);
        float sx = IsoUtils.XToScreenExact(this.getX() + 0.25f, this.getY() + 0.25f, this.getZ(), 0);
        float sy = IsoUtils.YToScreenExact(this.getX() + 0.25f, this.getY() + 0.25f, this.getZ(), 0);
        IndieGL.glBlendFunc(770, 771);
        SpriteRenderer.instance.StartShader(0, IsoCamera.frameState.playerIndex);
        SpriteRenderer.instance.renderi(null, (int)(sx - (float)(boxWidth / 2)), (int)(sy - (float)((boxHeight - fontHeight) / 2)), boxWidth, boxHeight, 0.0f, 0.0f, 0.0f, 0.5f, null);
        TextManager.instance.DrawStringCentre(UIFont.Small, sx, sy, text, 1.0, 1.0, 1.0, 1.0);
        SpriteRenderer.instance.EndShader();
    }

    public Radio getEquipedRadio() {
        return this.equipedRadio;
    }

    private void radioEquipedCheck() {
        Radio radio;
        InventoryItem inventoryItem;
        if (this.leftHandItem != this.leftHandCache) {
            this.leftHandCache = this.leftHandItem;
            if (this.leftHandItem != null && (this.equipedRadio == null || this.equipedRadio != this.rightHandItem) && (inventoryItem = this.leftHandItem) instanceof Radio) {
                this.equipedRadio = radio = (Radio)inventoryItem;
            } else if (this.equipedRadio != null && this.equipedRadio != this.rightHandItem && this.equipedRadio != this.getClothingItem_Back()) {
                if (this.equipedRadio.getDeviceData() != null) {
                    this.equipedRadio.getDeviceData().cleanSoundsAndEmitter();
                }
                this.equipedRadio = null;
            }
        }
        if (this.rightHandItem != this.rightHandCache) {
            this.rightHandCache = this.rightHandItem;
            if (this.rightHandItem != null && (inventoryItem = this.rightHandItem) instanceof Radio) {
                this.equipedRadio = radio = (Radio)inventoryItem;
            } else if (this.equipedRadio != null && this.equipedRadio != this.leftHandItem && this.equipedRadio != this.getClothingItem_Back()) {
                if (this.equipedRadio.getDeviceData() != null) {
                    this.equipedRadio.getDeviceData().cleanSoundsAndEmitter();
                }
                this.equipedRadio = null;
            }
        }
        if (this.getClothingItem_Back() != this.backCache) {
            this.backCache = this.getClothingItem_Back();
            if (this.getClothingItem_Back() != null && this.getClothingItem_Back() instanceof Radio) {
                this.equipedRadio = (Radio)this.getClothingItem_Back();
            } else if (this.equipedRadio != null && this.equipedRadio != this.leftHandItem && this.equipedRadio != this.rightHandItem) {
                if (this.equipedRadio.getDeviceData() != null) {
                    this.equipedRadio.getDeviceData().cleanSoundsAndEmitter();
                }
                this.equipedRadio = null;
            }
        }
    }

    private void debugAim() {
        IsoGameCharacter isoGameCharacter = this;
        if (!(isoGameCharacter instanceof IsoPlayer)) {
            return;
        }
        IsoPlayer player = (IsoPlayer)isoGameCharacter;
        if (!player.isAiming()) {
            return;
        }
        HandWeapon weapon = Type.tryCastTo(this.getPrimaryHandItem(), HandWeapon.class);
        if (weapon == null) {
            weapon = player.bareHands;
        }
        float maxRange = weapon.getMaxRange(player) * weapon.getRangeMod(player);
        float minRange = weapon.getMinRange();
        float direction = this.getLookAngleRadians();
        IndieGL.disableDepthTest();
        IndieGL.StartShader(0);
        if (this.ballisticsController != null && weapon.isRanged()) {
            boolean thickness = true;
            float targetRectAlpha = 1.0f;
            Color targetRectColor = Color.magenta;
            Vector3 firingStart = new Vector3();
            Vector3 firingDirection = new Vector3();
            Vector3 firingEnd = new Vector3();
            this.ballisticsController.calculateMuzzlePosition(firingStart, firingDirection);
            firingDirection.normalize();
            firingEnd.set(firingStart.x + firingDirection.x * maxRange, firingStart.y + firingDirection.y * maxRange, firingStart.z + firingDirection.z * maxRange);
            float height = -0.45f;
            GeometryUtils.computePerpendicularVectors(firingDirection.x, firingDirection.y, firingDirection.z, tempVector3f00, tempVector3f01);
            tempVector3f00.mul(CombatManager.getInstance().getCombatConfig().get(CombatConfigKey.BALLISTICS_CONTROLLER_DISTANCE_THRESHOLD));
            float zHeight = -0.45f;
            for (int i = 0; i < 4; ++i) {
                LineDrawer.DrawIsoLine(firingStart.x + IsoGameCharacter.tempVector3f00.x, firingStart.y + IsoGameCharacter.tempVector3f00.y, firingStart.z + IsoGameCharacter.tempVector3f00.z - zHeight, firingEnd.x + IsoGameCharacter.tempVector3f00.x, firingEnd.y + IsoGameCharacter.tempVector3f00.y, firingEnd.z + IsoGameCharacter.tempVector3f00.z - zHeight, targetRectColor.r, targetRectColor.g, targetRectColor.b, 1.0f, 1);
                LineDrawer.DrawIsoLine(firingStart.x - IsoGameCharacter.tempVector3f00.x, firingStart.y - IsoGameCharacter.tempVector3f00.y, firingStart.z - IsoGameCharacter.tempVector3f00.z - zHeight, firingEnd.x - IsoGameCharacter.tempVector3f00.x, firingEnd.y - IsoGameCharacter.tempVector3f00.y, firingEnd.z - IsoGameCharacter.tempVector3f00.z - zHeight, targetRectColor.r, targetRectColor.g, targetRectColor.b, 1.0f, 1);
                LineDrawer.DrawIsoLine(firingStart.x - IsoGameCharacter.tempVector3f00.x, firingStart.y - IsoGameCharacter.tempVector3f00.y, firingStart.z - IsoGameCharacter.tempVector3f00.z - zHeight, firingStart.x + IsoGameCharacter.tempVector3f00.x, firingStart.y + IsoGameCharacter.tempVector3f00.y, firingStart.z + IsoGameCharacter.tempVector3f00.z - zHeight, targetRectColor.r, targetRectColor.g, targetRectColor.b, 1.0f, 1);
                LineDrawer.DrawIsoLine(firingEnd.x - IsoGameCharacter.tempVector3f00.x, firingEnd.y - IsoGameCharacter.tempVector3f00.y, firingEnd.z - IsoGameCharacter.tempVector3f00.z - zHeight, firingEnd.x + IsoGameCharacter.tempVector3f00.x, firingEnd.y + IsoGameCharacter.tempVector3f00.y, firingEnd.z + IsoGameCharacter.tempVector3f00.z - zHeight, targetRectColor.r, targetRectColor.g, targetRectColor.b, 1.0f, 1);
                zHeight -= -0.29999998f;
            }
        } else {
            HitInfo hitInfo;
            float minAngle = weapon.getMinAngle();
            LineDrawer.drawDirectionLine(this.getX(), this.getY(), this.getZ(), maxRange, direction, 1.0f, 1.0f, 1.0f, 0.5f, 1);
            LineDrawer.drawDotLines(this.getX(), this.getY(), this.getZ(), maxRange, direction, minAngle -= weapon.getAimingPerkMinAngleModifier() * ((float)this.getPerkLevel(PerkFactory.Perks.Aiming) / 2.0f), 1.0f, 1.0f, 1.0f, 0.5f, 1);
            LineDrawer.drawArc(this.getX(), this.getY(), this.getZ(), minRange, direction, minAngle, 6, 1.0f, 1.0f, 1.0f, 0.5f);
            if (minRange != maxRange) {
                LineDrawer.drawArc(this.getX(), this.getY(), this.getZ(), maxRange, direction, minAngle, 6, 1.0f, 1.0f, 1.0f, 0.5f);
            }
            LineDrawer.drawArc(this.getX(), this.getY(), this.getZ(), maxRange + 0.2f, direction, minAngle, 6, 0.75f, 0.75f, 0.75f, 0.5f);
            float ignoreProneRange = Core.getInstance().getIgnoreProneZombieRange();
            if (ignoreProneRange > 0.0f) {
                LineDrawer.drawArc(this.getX(), this.getY(), this.getZ(), ignoreProneRange, direction, 0.0f, 12, 0.0f, 0.0f, 1.0f, 0.25f);
                LineDrawer.drawDotLines(this.getX(), this.getY(), this.getZ(), ignoreProneRange, direction, 0.0f, 0.0f, 0.0f, 1.0f, 0.25f, 1);
            }
            if (this.attackVars.targetOnGround.getObject() != null) {
                if (!this.attackVars.targetsProne.isEmpty() && (hitInfo = this.attackVars.targetsProne.get(0)) != null) {
                    LineDrawer.DrawIsoCircle(hitInfo.x, hitInfo.y, hitInfo.z, 0.1f, 8, 1.0f, 1.0f, 0.0f, 1.0f);
                }
            } else if (!this.attackVars.targetsStanding.isEmpty() && (hitInfo = this.attackVars.targetsStanding.get(0)) != null) {
                LineDrawer.DrawIsoCircle(hitInfo.x, hitInfo.y, hitInfo.z, 0.1f, 8, 1.0f, 1.0f, 0.0f, 1.0f);
            }
            for (int i = 0; i < this.hitInfoList.size(); ++i) {
                HitInfo hitInfo2 = this.hitInfoList.get(i);
                IsoMovingObject obj = hitInfo2.getObject();
                if (obj != null) {
                    int chance = hitInfo2.chance;
                    float r = 1.0f - (float)chance / 100.0f;
                    float g = 1.0f - r;
                    float scale = Math.max(0.2f, (float)chance / 100.0f) / 2.0f;
                    float sx = IsoUtils.XToScreenExact(obj.getX() - scale, obj.getY() + scale, obj.getZ(), 0);
                    float sy = IsoUtils.YToScreenExact(obj.getX() - scale, obj.getY() + scale, obj.getZ(), 0);
                    float sx2 = IsoUtils.XToScreenExact(obj.getX() - scale, obj.getY() - scale, obj.getZ(), 0);
                    float sy2 = IsoUtils.YToScreenExact(obj.getX() - scale, obj.getY() - scale, obj.getZ(), 0);
                    float sx3 = IsoUtils.XToScreenExact(obj.getX() + scale, obj.getY() - scale, obj.getZ(), 0);
                    float sy3 = IsoUtils.YToScreenExact(obj.getX() + scale, obj.getY() - scale, obj.getZ(), 0);
                    float sx4 = IsoUtils.XToScreenExact(obj.getX() + scale, obj.getY() + scale, obj.getZ(), 0);
                    float sy4 = IsoUtils.YToScreenExact(obj.getX() + scale, obj.getY() + scale, obj.getZ(), 0);
                    SpriteRenderer.instance.renderPoly(sx, sy, sx2, sy2, sx3, sy3, sx4, sy4, r, g, 0.0f, 0.5f);
                    UIFont font = UIFont.Dialogue;
                    TextManager.instance.DrawStringCentre(font, sx4, sy4, String.valueOf(hitInfo2.dot), 1.0, 1.0, 1.0, 1.0);
                    TextManager.instance.DrawStringCentre(font, sx4, sy4 + (float)TextManager.instance.getFontHeight(font), hitInfo2.chance + "%", 1.0, 1.0, 1.0, 1.0);
                    r = 1.0f;
                    g = 1.0f;
                    float b = 1.0f;
                    float dist = PZMath.sqrt(hitInfo2.distSq);
                    if (dist < weapon.getMinRange()) {
                        b = 0.0f;
                        r = 0.0f;
                    }
                    TextManager.instance.DrawStringCentre(font, sx4, sy4 + (float)(TextManager.instance.getFontHeight(font) * 2), "DIST: " + dist, r, g, b, 1.0);
                }
                if (hitInfo2.window.getObject() == null) continue;
                hitInfo2.window.getObject().setHighlighted(true);
            }
        }
    }

    private void debugTestDotSide() {
        if (this != IsoPlayer.getInstance()) {
            return;
        }
        float direction = this.getLookAngleRadians();
        float radius = 2.0f;
        float dot = 0.7f;
        LineDrawer.drawDotLines(this.getX(), this.getY(), this.getZ(), 2.0f, direction, dot, 1.0f, 1.0f, 1.0f, 0.5f, 1);
        dot = -0.5f;
        LineDrawer.drawDotLines(this.getX(), this.getY(), this.getZ(), 2.0f, direction, dot, 1.0f, 1.0f, 1.0f, 0.5f, 1);
        LineDrawer.drawArc(this.getX(), this.getY(), this.getZ(), 2.0f, direction, -1.0f, 16, 1.0f, 1.0f, 1.0f, 0.5f);
        ArrayList<IsoZombie> zombies = this.getCell().getZombieList();
        for (int i = 0; i < zombies.size(); ++i) {
            IsoMovingObject obj = zombies.get(i);
            if (!(this.DistToSquared(obj) < 4.0f)) continue;
            LineDrawer.DrawIsoCircle(obj.getX(), obj.getY(), obj.getZ(), 0.3f, 1.0f, 1.0f, 1.0f, 1.0f);
            float scale = 0.2f;
            float sx4 = IsoUtils.XToScreenExact(obj.getX() + 0.2f, obj.getY() + 0.2f, obj.getZ(), 0);
            float sy4 = IsoUtils.YToScreenExact(obj.getX() + 0.2f, obj.getY() + 0.2f, obj.getZ(), 0);
            UIFont font = UIFont.DebugConsole;
            int fontHgt = TextManager.instance.getFontHeight(font);
            TextManager.instance.DrawStringCentre(font, sx4, sy4 + (float)fontHgt, "SIDE: " + this.testDotSide(obj), 1.0, 1.0, 1.0, 1.0);
            Vector2 v1 = this.getLookVector(tempo2);
            Vector2 v2 = tempo.set(obj.getX() - this.getX(), obj.getY() - this.getY());
            v2.normalize();
            float radians = PZMath.wrap(v2.getDirection() - v1.getDirection(), 0.0f, (float)Math.PI * 2);
            TextManager.instance.DrawStringCentre(font, sx4, sy4 + (float)(fontHgt * 2), "ANGLE (0-360): " + PZMath.radToDeg(radians), 1.0, 1.0, 1.0, 1.0);
            radians = (float)Math.acos(this.getDotWithForwardDirection(obj.getX(), obj.getY()));
            TextManager.instance.DrawStringCentre(font, sx4, sy4 + (float)(fontHgt * 3), "ANGLE (0-180): " + PZMath.radToDeg(radians), 1.0, 1.0, 1.0, 1.0);
        }
    }

    private void debugVision() {
        if (this != IsoPlayer.getInstance()) {
            return;
        }
        float cone = LightingJNI.calculateVisionCone(this);
        LineDrawer.drawDotLines(this.getX(), this.getY(), this.getZ(), GameTime.getInstance().getViewDist(), this.getLookAngleRadians(), -cone, 1.0f, 1.0f, 1.0f, 0.5f, 1);
        LineDrawer.drawArc(this.getX(), this.getY(), this.getZ(), GameTime.getInstance().getViewDist(), this.getLookAngleRadians(), -cone, 16, 1.0f, 1.0f, 1.0f, 0.5f);
        float rearZombieDistance = LightingJNI.calculateRearZombieDistance(this);
        LineDrawer.drawArc(this.getX(), this.getY(), this.getZ(), rearZombieDistance, this.getLookAngleRadians(), -1.0f, 32, 1.0f, 1.0f, 1.0f, 0.5f);
    }

    public void setDefaultState() {
        this.stateMachine.changeState(this.defaultState, null, false);
    }

    public void SetOnFire() {
        if (this.onFire) {
            return;
        }
        this.setOnFire(true);
        float scale = (float)Core.tileScale / 2.0f;
        this.AttachAnim("Fire", "01", 30, 0.5f, (int)(-(this.offsetX + 1.0f * scale)) + 8 - Rand.Next(16), (int)(-(this.offsetY + -89.0f * scale)) + (int)((float)(10 + Rand.Next(20)) * scale), true, 0, false, 0.7f, IsoFireManager.FIRE_TINT_MOD);
        IsoFireManager.AddBurningCharacter(this);
        int partIndex = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.MAX));
        if (this instanceof IsoPlayer) {
            this.getBodyDamage().getBodyParts().get(partIndex).setBurned();
        }
        if (scale == 2.0f) {
            int n = this.attachedAnimSprite.size() - 1;
            ((IsoSpriteInstance)this.attachedAnimSprite.get(n)).setScale(scale, scale);
        }
        if (!this.getEmitter().isPlaying("BurningFlesh")) {
            this.getEmitter().playSoundImpl("BurningFlesh", this);
        }
    }

    @Override
    public void StopBurning() {
        if (!this.onFire) {
            return;
        }
        IsoFireManager.RemoveBurningCharacter(this);
        this.setOnFire(false);
        if (this.attachedAnimSprite != null) {
            this.attachedAnimSprite.clear();
        }
        this.getEmitter().stopOrTriggerSoundByName("BurningFlesh");
    }

    public void SpreadFireMP() {
        if (!(this.onFire && GameServer.server && SandboxOptions.instance.fireSpread.getValue())) {
            return;
        }
        IsoGridSquare sq = ServerMap.instance.getGridSquare(this.getXi(), this.getYi(), this.getZi());
        if (sq != null && !sq.getProperties().has(IsoFlagType.burning) && Rand.Next(Rand.AdjustForFramerate(3000)) < this.fireSpreadProbability) {
            IsoFireManager.StartFire(this.getCell(), sq, false, 80);
        }
    }

    public void SpreadFire() {
        if (!this.onFire || GameServer.server || GameClient.client || !SandboxOptions.instance.fireSpread.getValue()) {
            return;
        }
        if (this.square != null && !this.square.getProperties().has(IsoFlagType.burning) && Rand.Next(Rand.AdjustForFramerate(3000)) < this.fireSpreadProbability) {
            IsoFireManager.StartFire(this.getCell(), this.square, false, 80);
        }
    }

    public void Throw(HandWeapon weapon) {
        String physicsObject;
        float y;
        if (this instanceof IsoPlayer && (((IsoPlayer)this).getJoypadBind() != -1 || this.attackTargetSquare == null)) {
            Vector2 throwVec = this.getForwardDirection(tempo);
            throwVec.setLength(weapon.getMaxRange());
            this.attackTargetSquare = this.getCell().getGridSquare(this.getX() + throwVec.getX(), this.getY() + throwVec.getY(), this.getZ());
            if (this.attackTargetSquare == null) {
                this.attackTargetSquare = this.getCell().getGridSquare(this.getX() + throwVec.getX(), this.getY() + throwVec.getY(), 0.0);
            }
        }
        weapon.setAttackTargetSquare(this.attackTargetSquare);
        float x = (float)this.attackTargetSquare.getX() - this.getX();
        if (x > 0.0f) {
            if ((float)this.attackTargetSquare.getX() - this.getX() > weapon.getMaxRange()) {
                x = weapon.getMaxRange();
            }
        } else if ((float)this.attackTargetSquare.getX() - this.getX() < -weapon.getMaxRange()) {
            x = -weapon.getMaxRange();
        }
        if ((y = (float)this.attackTargetSquare.getY() - this.getY()) > 0.0f) {
            if ((float)this.attackTargetSquare.getY() - this.getY() > weapon.getMaxRange()) {
                y = weapon.getMaxRange();
            }
        } else if ((float)this.attackTargetSquare.getY() - this.getY() < -weapon.getMaxRange()) {
            y = -weapon.getMaxRange();
        }
        if ((physicsObject = weapon.getPhysicsObject()) != null) {
            IsoPhysicsObject isoPhysicsObject = physicsObject.equals(ItemKey.Normal.BALL.toString()) ? new IsoBall(this.getCell(), this.getX(), this.getY(), this.getZ() + 0.6f, x * 0.4f, y * 0.4f, weapon, this) : new IsoMolotovCocktail(this.getCell(), this.getX(), this.getY(), this.getZ() + 0.6f, x * 0.4f, y * 0.4f, weapon, this);
        }
        if (!GameClient.client || this.isLocal()) {
            IsoGridSquare sq = this.getCurrentSquare();
            INetworkPacket.send(PacketTypes.PacketType.PlayerDropHeldItems, this, sq.x, sq.y, sq.z, false, true);
        }
        if (this instanceof IsoPlayer) {
            ((IsoPlayer)this).setAttackAnimThrowTimer(0L);
        }
    }

    public boolean helmetFall(boolean hitHead) {
        if (GameClient.client) {
            return false;
        }
        if (GameServer.server) {
            return GameServer.helmetFall(this, hitHead);
        }
        IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
        boolean removed = false;
        IsoZombie zombie = Type.tryCastTo(this, IsoZombie.class);
        if (zombie != null && !zombie.isUsingWornItems()) {
            this.getItemVisuals(tempItemVisuals);
            for (int i = 0; i < tempItemVisuals.size(); ++i) {
                Object item;
                ItemVisual itemVisual = (ItemVisual)tempItemVisuals.get(i);
                Item scriptItem = itemVisual.getScriptItem();
                if (scriptItem == null || !scriptItem.isItemType(ItemType.CLOTHING) || scriptItem.getChanceToFall() <= 0) continue;
                int chanceToFall = scriptItem.getChanceToFall();
                if (hitHead) {
                    chanceToFall += 40;
                }
                if (Rand.Next(100) <= chanceToFall || (item = InventoryItemFactory.CreateItem(scriptItem.getFullName())) == null) continue;
                if (((InventoryItem)item).getVisual() != null) {
                    ((InventoryItem)item).getVisual().copyFrom(itemVisual);
                    ((InventoryItem)item).synchWithVisual();
                }
                IsoFallingClothing clot = new IsoFallingClothing(this.getCell(), this.getX(), this.getY(), PZMath.min(this.getZ() + 0.4f, (float)PZMath.fastfloor(this.getZ()) + 0.95f), 0.2f, 0.2f, (InventoryItem)item);
                tempItemVisuals.remove(i--);
                zombie.itemVisuals.clear();
                zombie.itemVisuals.addAll(tempItemVisuals);
                this.resetModelNextFrame();
                this.onWornItemsChanged();
                removed = true;
            }
        } else if (this.getWornItems() != null && !this.getWornItems().isEmpty()) {
            for (int i = 0; i < this.getWornItems().size(); ++i) {
                WornItem wornItem = this.getWornItems().get(i);
                InventoryItem item = wornItem.getItem();
                if (!(item instanceof Clothing)) continue;
                Clothing clothing = (Clothing)item;
                int chanceToFall = clothing.getChanceToFall();
                if (hitHead) {
                    chanceToFall += 40;
                }
                if (clothing.getChanceToFall() <= 0 || Rand.Next(100) > chanceToFall) continue;
                new IsoFallingClothing(this.getCell(), this.getX(), this.getY(), PZMath.min(this.getZ() + 0.4f, this.getZ() + 0.95f), Rand.Next(-0.2f, 0.2f), Rand.Next(-0.2f, 0.2f), item);
                this.getInventory().Remove(item);
                this.getWornItems().remove(item);
                this.resetModelNextFrame();
                this.onWornItemsChanged();
                removed = true;
                if (!GameClient.client || player == null || !player.isLocalPlayer()) continue;
                INetworkPacket.send(PacketTypes.PacketType.SyncClothing, player);
            }
        }
        if (removed && player != null && player.isLocalPlayer()) {
            LuaEventManager.triggerEvent("OnClothingUpdated", this);
        }
        if (removed && this.isZombie()) {
            PersistentOutfits.instance.setFallenHat(this, true);
        }
        return removed;
    }

    @Override
    public void smashCarWindow(VehiclePart part) {
        this.clear(SmashWindowState.class);
        this.set(SmashWindowState.VEHICLE_WINDOW, part.getWindow());
        this.set(SmashWindowState.VEHICLE, part.getVehicle());
        this.set(SmashWindowState.VEHICLE_PART, part);
        this.actionContext.reportEvent("EventSmashWindow");
    }

    @Override
    public void smashWindow(IsoWindow w) {
        if (!w.isInvincible()) {
            this.clear(SmashWindowState.class);
            this.set(SmashWindowState.ISO_WINDOW, w);
            this.actionContext.reportEvent("EventSmashWindow");
        }
    }

    @Override
    public void openWindow(IsoWindow w) {
        if (!w.isInvincible()) {
            OpenWindowState.instance().setParams(this, w);
            this.actionContext.reportEvent("EventOpenWindow");
        }
    }

    @Override
    public void closeWindow(IsoWindow w) {
        if (!w.isInvincible()) {
            this.clear(CloseWindowState.class);
            this.set(CloseWindowState.ISO_WINDOW, w);
            this.actionContext.reportEvent("EventCloseWindow");
        }
    }

    @Override
    public void climbThroughWindow(IsoWindow w) {
        if (w.canClimbThrough(this)) {
            this.dropHeavyItems();
            float ox = this.getX() - (float)PZMath.fastfloor(this.getX());
            float oy = this.getY() - (float)PZMath.fastfloor(this.getY());
            int xModifier = 0;
            int yModifier = 0;
            if (w.getX() > this.getX() && !w.isNorth()) {
                xModifier = -1;
            }
            if (w.getY() > this.getY() && w.isNorth()) {
                yModifier = -1;
            }
            this.setX(w.getX() + ox + (float)xModifier);
            this.setY(w.getY() + oy + (float)yModifier);
            ClimbThroughWindowState.instance().setParams(this, w);
            this.actionContext.reportEvent("EventClimbWindow");
        }
    }

    @Override
    public void climbThroughWindow(IsoWindow w, Integer startingFrame) {
        if (w.canClimbThrough(this)) {
            this.dropHeavyItems();
            ClimbThroughWindowState.instance().setParams(this, w);
            this.actionContext.reportEvent("EventClimbWindow");
        }
    }

    public boolean isClosingWindow(IsoWindow window) {
        if (window == null) {
            return false;
        }
        if (!this.isCurrentState(CloseWindowState.instance())) {
            return false;
        }
        return CloseWindowState.instance().getWindow(this) == window;
    }

    public boolean isClimbingThroughWindow(IsoWindow window) {
        if (window == null) {
            return false;
        }
        if (!this.isCurrentState(ClimbThroughWindowState.instance())) {
            return false;
        }
        if (!this.getVariableBoolean("BlockWindow")) {
            return false;
        }
        return ClimbThroughWindowState.instance().getWindow(this) == window;
    }

    @Override
    public void climbThroughWindowFrame(IsoWindowFrame windowFrame) {
        if (windowFrame.canClimbThrough(this)) {
            this.dropHeavyItems();
            ClimbThroughWindowState.instance().setParams(this, windowFrame);
            this.actionContext.reportEvent("EventClimbWindow");
        }
    }

    @Override
    public void climbSheetRope() {
        if (!this.canClimbSheetRope(this.current)) {
            return;
        }
        this.dropHeavyItems();
        this.clear(ClimbSheetRopeState.class);
        this.actionContext.reportEvent("EventClimbRope");
    }

    @Override
    public void climbDownSheetRope() {
        if (!this.canClimbDownSheetRope(this.current)) {
            return;
        }
        this.dropHeavyItems();
        this.clear(ClimbDownSheetRopeState.class);
        this.actionContext.reportEvent("EventClimbDownRope");
    }

    @Override
    public boolean canClimbSheetRope(IsoGridSquare sq) {
        if (sq == null) {
            return false;
        }
        int startZ = sq.getZ();
        while (sq != null) {
            if (!IsoWindow.isSheetRopeHere(sq)) {
                return false;
            }
            if (!IsoWindow.canClimbHere(sq)) {
                return false;
            }
            if (sq.TreatAsSolidFloor() && sq.getZ() > startZ) {
                return false;
            }
            if (IsoWindow.isTopOfSheetRopeHere(sq)) {
                return true;
            }
            sq = this.getCell().getGridSquare((double)sq.getX(), (double)sq.getY(), (float)sq.getZ() + 1.0f);
        }
        return false;
    }

    @Override
    public boolean canClimbDownSheetRopeInCurrentSquare() {
        return this.canClimbDownSheetRope(this.current);
    }

    @Override
    public boolean canClimbDownSheetRope(IsoGridSquare sq) {
        if (sq == null) {
            return false;
        }
        int startZ = sq.getZ();
        while (sq != null) {
            if (!IsoWindow.isSheetRopeHere(sq)) {
                return false;
            }
            if (!IsoWindow.canClimbHere(sq)) {
                return false;
            }
            if (sq.TreatAsSolidFloor()) {
                return sq.getZ() < startZ;
            }
            sq = this.getCell().getGridSquare((double)sq.getX(), (double)sq.getY(), (float)sq.getZ() - 1.0f);
        }
        return false;
    }

    @Override
    public void climbThroughWindow(IsoThumpable w) {
        if (w.canClimbThrough(this)) {
            this.dropHeavyItems();
            float ox = this.getX() - (float)PZMath.fastfloor(this.getX());
            float oy = this.getY() - (float)PZMath.fastfloor(this.getY());
            int xModifier = 0;
            int yModifier = 0;
            if (w.getX() > this.getX() && !w.north) {
                xModifier = -1;
            }
            if (w.getY() > this.getY() && w.north) {
                yModifier = -1;
            }
            this.setX(w.getX() + ox + (float)xModifier);
            this.setY(w.getY() + oy + (float)yModifier);
            ClimbThroughWindowState.instance().setParams(this, w);
            this.actionContext.reportEvent("EventClimbWindow");
        }
    }

    @Override
    public void climbThroughWindow(IsoThumpable w, Integer startingFrame) {
        if (w.canClimbThrough(this)) {
            this.dropHeavyItems();
            ClimbThroughWindowState.instance().setParams(this, w);
            this.actionContext.reportEvent("EventClimbWindow");
        }
    }

    @Override
    public void climbOverFence(IsoDirections dir) {
        if (this.current == null) {
            return;
        }
        IsoGridSquare oppositeSq = this.current.getAdjacentSquare(dir);
        if (!IsoWindow.canClimbThroughHelper(this, this.current, oppositeSq, dir == IsoDirections.N || dir == IsoDirections.S)) {
            return;
        }
        if (!this.current.isPlayerAbleToHopWallTo(dir, oppositeSq)) {
            return;
        }
        BentFences.getInstance().checkDamageHoppableFence(this, this.current, oppositeSq);
        ClimbOverFenceState.instance().setParams(this, dir);
        this.actionContext.reportEvent("EventClimbFence");
    }

    @Override
    public boolean isAboveTopOfStairs() {
        if (this.getZ() == 0.0f || this.getZ() - (float)PZMath.fastfloor(this.getZ()) > 0.01f || this.current != null && this.current.TreatAsSolidFloor()) {
            return false;
        }
        IsoGridSquare sq = this.getCell().getGridSquare(this.getX(), this.getY(), this.getZ() - 1.0f);
        return sq != null && (sq.has(IsoObjectType.stairsTN) || sq.has(IsoObjectType.stairsTW));
    }

    public void throwGrappledTargetOutWindow(IsoObject windowObject) {
        DebugType.Grapple.debugln("Attempting to throw out window: %s", windowObject);
        if (!this.isGrappling()) {
            DebugLog.Grapple.warn("Not currently grapling anything. Nothing to throw out the window, windowObject:%s", windowObject);
            return;
        }
        IGrappleable thrownTarget = this.getGrapplingTarget();
        if (!(thrownTarget instanceof IsoGameCharacter)) {
            return;
        }
        IsoGameCharacter thrownCharacter = (IsoGameCharacter)thrownTarget;
        ClimbThroughWindowPositioningParams params = ClimbThroughWindowPositioningParams.alloc();
        ClimbThroughWindowState.getClimbThroughWindowPositioningParams(this, windowObject, params);
        if (!params.canClimb) {
            DebugType.Grapple.error("Cannot climb through, cannot throw out through.");
            params.release();
            return;
        }
        this.setDoGrapple(false);
        this.setDoGrappleLetGo();
        this.setDir(params.climbDir.Rot180());
        ClimbThroughWindowState.slideCharacterToWindowOpening(this, params);
        GrappledThrownOutWindowState.instance().setParams(thrownCharacter, params.windowObject);
        this.set(PlayerDraggingCorpse.THROWN_OUT_WINDOW_OBJ, params.windowObject);
        this.actionContext.reportEvent("GrappleThrowOutWindow");
        this.setGrappleThrowOutWindow(true);
        params.release();
    }

    public void throwGrappledOverFence(IsoObject hoppableObject, IsoDirections dir) {
        DebugType.Grapple.debugln("Attempting to throw over fence: %s", hoppableObject);
        if (!this.isGrappling()) {
            DebugLog.Grapple.warn("Not currently grapling anything. Nothing to throw over fence: %s", hoppableObject);
            return;
        }
        IGrappleable thrownTarget = this.getGrapplingTarget();
        if (!(thrownTarget instanceof IsoGameCharacter)) {
            return;
        }
        IsoGameCharacter thrownCharacter = (IsoGameCharacter)thrownTarget;
        if (this.current == null || dir == null) {
            return;
        }
        IsoGridSquare oppositeSq = this.current.getAdjacentSquare(dir);
        if (!IsoWindow.canClimbThroughHelper(this, this.current, oppositeSq, dir == IsoDirections.N || dir == IsoDirections.S)) {
            return;
        }
        this.setDoGrapple(false);
        this.setDoGrappleLetGo();
        this.setDir(dir);
        GrappledThrownOverFenceState.instance().setParams(thrownCharacter, dir);
        this.set(PlayerDraggingCorpse.THROWN_OVER_FENCE_DIR, dir);
        this.actionContext.reportEvent("GrappleThrowOverFence");
        this.setGrappleThrowOverFence(true);
    }

    public void throwGrappledIntoInventory(ItemContainer targetContainer) {
        DebugType.Grapple.debugln("Attempting to throw into inventory: %s", targetContainer);
        if (!this.isGrappling()) {
            DebugLog.Grapple.warn("Not currently grapling anything. Nothing to throw into inventory: %s", targetContainer);
            return;
        }
        IGrappleable thrownTarget = this.getGrapplingTarget();
        if (!(thrownTarget instanceof IsoGameCharacter)) {
            return;
        }
        IsoGameCharacter thrownCharacter = (IsoGameCharacter)thrownTarget;
        if (this.current == null) {
            return;
        }
        try (AutoCloseablePool pool = AutoCloseablePool.alloc();){
            Vector2 containerPos = targetContainer.getWorldPosition(pool.allocVector2());
            Vector2 pos = this.getPosition(pool.allocVector2());
            Vector2 forwardDir = pool.allocVector2();
            forwardDir.set(containerPos.x - pos.x, containerPos.y - pos.y);
            if (this.isAnimatingBackwards()) {
                forwardDir.scale(-1.0f);
            }
            this.setForwardDirection(forwardDir);
        }
        this.setDoGrapple(false);
        this.setDoGrappleLetGo();
        GrappledThrownIntoContainerState.instance().setParams(thrownCharacter, targetContainer);
        this.set(PlayerDraggingCorpse.THROWN_INTO_CONTAINER_OBJ, targetContainer);
        this.actionContext.reportEvent("GrappleThrowIntoContainer");
        this.setGrappleThrowIntoContainer(true);
    }

    public void pickUpCorpseItem(InventoryItem item) {
        if (this.isGrappling()) {
            DebugType.Grapple.warn("Cannot pick up a corpse when already grappling.");
            return;
        }
        DebugType.Grapple.debugln("Attempting to grab corpse item: %s", item);
        IsoGridSquare square = this.getCurrentSquare();
        IsoDeadBody deadBody = square.tryAddCorpseToWorld(item, this.getX() - (float)square.x, this.getY() - (float)square.y, false);
        if (deadBody == null) {
            DebugType.Grapple.warn("Failed to spawn IsoDeadBody from item: %s", item);
            return;
        }
        deadBody.setFallOnFront(false);
        this.pickUpCorpse(deadBody, "PickUpCorpseItem");
        if (!this.isGrappling()) {
            deadBody.setDoRender(true);
        }
    }

    public void pickUpCorpse(IsoDeadBody body, String dragType) {
        DebugType.Grapple.debugln("Attempting to pick up corpse: %s", body);
        if (body == null) {
            DebugType.Grapple.error("Body is null.");
            return;
        }
        this.setDoGrapple(true);
        float grappleEffectiveness = this.calculateGrappleEffectivenessFromTraits();
        body.Grappled(this, this.getAttackingWeapon(), grappleEffectiveness, dragType);
        this.setDoGrapple(false);
    }

    public float calculateGrappleEffectivenessFromTraits() {
        float grappleEffectiveness = 1.0f;
        if (this.characterTraits.get(CharacterTrait.VERY_UNDERWEIGHT)) {
            grappleEffectiveness *= 0.8f;
        }
        if (this.characterTraits.get(CharacterTrait.EMACIATED)) {
            grappleEffectiveness *= 0.6f;
        }
        if (this.characterTraits.get(CharacterTrait.OVERWEIGHT)) {
            grappleEffectiveness *= 1.1f;
        }
        if (this.characterTraits.get(CharacterTrait.OBESE)) {
            grappleEffectiveness *= 1.05f;
        }
        if (this.characterTraits.get(CharacterTrait.STRONG)) {
            grappleEffectiveness *= 1.25f;
        }
        if (this.characterTraits.get(CharacterTrait.ATHLETIC)) {
            grappleEffectiveness *= 1.25f;
        }
        if (this.characterTraits.get(CharacterTrait.BRAVE)) {
            grappleEffectiveness *= 1.1f;
        }
        if (this.characterTraits.get(CharacterTrait.COWARDLY)) {
            grappleEffectiveness *= 0.9f;
        }
        if (this.characterTraits.get(CharacterTrait.SPEED_DEMON)) {
            grappleEffectiveness *= 1.15f;
        }
        return grappleEffectiveness;
    }

    @Override
    public void preupdate() {
        super.preupdate();
        this.updateAnimationTimeDelta();
        if (!this.debugVariablesRegistered && DebugOptions.instance.character.debug.registerDebugVariables.getValue()) {
            this.registerDebugGameVariables();
        }
        if (GameServer.server && this.handItemShouldSendToClients) {
            INetworkPacket.send((IsoPlayer)this, PacketTypes.PacketType.Equip, this);
            this.handItemShouldSendToClients = false;
        }
    }

    private void updateAnimationTimeDelta() {
        this.animationTimeScale = 1.0f;
        this.animationUpdatingThisFrame = true;
        if (GameTime.getInstance().perObjectMultiplier > 1.0f || !DebugOptions.instance.zombieAnimationDelay.getValue()) {
            return;
        }
        float maxAlpha = 0.0f;
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            IsoPlayer player = IsoPlayer.players[i];
            if (player == null) continue;
            maxAlpha = PZMath.max(maxAlpha, this.getAlpha(i));
        }
        if (maxAlpha < 0.03f) {
            this.animationUpdatingThisFrame = this.animationInvisibleFrameDelay.update();
            if (this.animationUpdatingThisFrame) {
                this.animationTimeScale = GameClient.client && this instanceof IsoZombie && ((IsoZombie)this).isRemoteZombie() ? 1.0f : (float)(this.animationInvisibleFrameDelay.delay + 1);
            }
        }
    }

    public void updateHandEquips() {
        if (GameServer.server) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.Equip, this.getX(), this.getY(), this);
        } else {
            INetworkPacket.send(PacketTypes.PacketType.Equip, this);
        }
        this.handItemShouldSendToClients = false;
    }

    @Override
    public void update() {
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = this.updateInternal.profile();){
            this.updateInternal();
        }
    }

    @Override
    public boolean isPushedByForSeparate(IsoMovingObject other) {
        if (other instanceof IGrappleable) {
            IGrappleable otherGrappleable = (IGrappleable)((Object)other);
            if (this.isGrapplingTarget(otherGrappleable)) {
                return false;
            }
            if (this.isBeingGrappledBy(otherGrappleable)) {
                return false;
            }
        }
        return super.isPushedByForSeparate(other);
    }

    @Override
    protected void slideAwayFromWalls(float radius, boolean instant, boolean includePolyCollisions) {
        if (!DebugOptions.instance.collideWithObstacles.debug.slideAwayFromWalls.getValue()) {
            return;
        }
        if (DebugOptions.instance.collideWithObstacles.render.radius.getValue()) {
            LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), radius, 16, 1.0f, 1.0f, 0.0f, 1.0f);
        }
        Vector2f newPos = this.slideAwayFromWalls.vector2f;
        boolean hasCollided = false;
        if (includePolyCollisions) {
            hasCollided = PolygonalMap2.instance.resolveCollision(this, radius, newPos);
        }
        boolean bl = hasCollided = this.resolveCollisionWithNeighboringSquares(radius, newPos) || hasCollided;
        if (!hasCollided) {
            return;
        }
        if (DebugOptions.instance.collideWithObstacles.render.radius.getValue()) {
            LineDrawer.DrawIsoCircle(newPos.x, newPos.y, this.getZ(), radius, 16, 0.5f, 0.0f, 0.5f, 1.0f);
        }
        this.slideAwayToCollisionPos(newPos.x, newPos.y, instant);
    }

    private boolean resolveCollisionWithNeighboringSquares(float inRadius, Vector2f newPos) {
        float currentX = this.getX();
        float currentY = this.getY();
        float currentZ = this.getZ();
        float nextX = this.getNextX();
        float nextY = this.getNextY();
        int currentSquareX = PZMath.fastfloor(currentX);
        int currentSquareY = PZMath.fastfloor(currentY);
        int currentSquareZ = PZMath.fastfloor(currentZ);
        float squareCenterX = 0.5f + (float)currentSquareX;
        float squareCenterY = 0.5f + (float)currentSquareY;
        float sqcToNextX = nextX - squareCenterX;
        float sqcToNextY = nextY - squareCenterY;
        int sqcToNextXsign = PZMath.sign(sqcToNextX);
        int sqcToNextYsign = PZMath.sign(sqcToNextY);
        float x2 = nextX + inRadius * (float)sqcToNextXsign;
        float y2 = nextY + inRadius * (float)sqcToNextYsign;
        int nextSquareX = PZMath.fastfloor(x2);
        int nextSquareY = PZMath.fastfloor(y2);
        if (nextSquareX == currentSquareX && nextSquareY == currentSquareY) {
            return false;
        }
        IsoGridSquare currentSquare = this.getSquare();
        if (currentSquare == null || currentSquareX != currentSquare.x || currentSquareY != currentSquare.y) {
            currentSquare = this.getCell().getGridSquare(currentSquareX, currentSquareY, currentSquareZ);
        }
        if (currentSquare == null) {
            return false;
        }
        boolean collided = false;
        newPos.x = nextX;
        newPos.y = nextY;
        if (sqcToNextXsign != 0 && currentSquare.testCollideAdjacent(this, sqcToNextXsign, 0, 0)) {
            collided = true;
            if (sqcToNextXsign == -1) {
                newPos.x = (float)currentSquareX + inRadius;
            } else if (sqcToNextXsign == 1) {
                newPos.x = (float)currentSquareX + 1.0f - inRadius;
            }
        }
        if (sqcToNextYsign != 0 && currentSquare.testCollideAdjacent(this, 0, sqcToNextYsign, 0)) {
            collided = true;
            if (sqcToNextYsign == -1) {
                newPos.y = (float)currentSquareY + inRadius;
            } else if (sqcToNextYsign == 1) {
                newPos.y = (float)currentSquareY + 1.0f - inRadius;
            }
        }
        return collided;
    }

    @Override
    public void setHitDir(Vector2 hitDir) {
        super.setHitDir(hitDir);
        this.setHitDirEnum(this.determineHitDirEnum(hitDir));
    }

    @Override
    public UpdateSchedulerSimulationLevel getMinimumSimulationLevel() {
        if (this.isDead() || this.isRagdollSimulationActive() || this.isFalling()) {
            return UpdateSchedulerSimulationLevel.FULL;
        }
        return super.getMinimumSimulationLevel();
    }

    private void setHitDirEnum(String hitDirEnum) {
        this.hitDirEnum = hitDirEnum;
    }

    public String getHitDirEnum() {
        return this.hitDirEnum;
    }

    private String determineHitDirEnum(Vector2 hitDir) {
        Vector2 lookVector = this.getLookVector(l_testDotSide.v1);
        Vector2 hitDirNormalized = l_testDotSide.v3.set(hitDir);
        hitDirNormalized.normalize();
        float dotHitDirToForward = Vector2.dot(hitDirNormalized.x, hitDirNormalized.y, lookVector.x, lookVector.y);
        if (dotHitDirToForward < -0.5f) {
            return "FRONT";
        }
        if (dotHitDirToForward > 0.5f) {
            return "BEHIND";
        }
        float crossZ = hitDirNormalized.x * lookVector.y - hitDirNormalized.y * lookVector.x;
        if (crossZ > 0.0f) {
            return "RIGHT";
        }
        return "LEFT";
    }

    private void updateInternal() {
        boolean inVehicle;
        if (this.current == null) {
            return;
        }
        this.updateAlpha();
        if (!this.isAnimal()) {
            this.updateBallisticsTarget();
        }
        if (this.isNpc) {
            this.ai.update();
        }
        if (this.sprite != null) {
            this.legsSprite = this.sprite;
        }
        if (!(!this.isGrappling() || this.isPerformingAnyGrappleAnimation() || this.isInGrapplerState() || GameClient.client || GameServer.server)) {
            this.LetGoOfGrappled("Aborted");
        }
        if (this.isDead() && (this.current == null || !this.current.getMovingObjects().contains(this))) {
            if (GameServer.server) {
                this.die();
            }
            return;
        }
        this.checkSCBADrain();
        if (this.getBodyDamage() != null && this.getCurrentBuilding() != null && this.getCurrentBuilding().isToxic() && !this.isProtectedFromToxic(true)) {
            float fatigue = this.getStats().get(CharacterStat.FATIGUE);
            float mult = GameTime.getInstance().getThirtyFPSMultiplier();
            if (fatigue < 1.0f) {
                this.getStats().add(CharacterStat.FATIGUE, 1.0E-4f * mult);
            }
            if (fatigue > 0.8f) {
                this.getBodyDamage().getBodyPart(BodyPartType.Head).ReduceHealth(0.1f * mult);
            }
            this.getBodyDamage().getBodyPart(BodyPartType.Torso_Upper).ReduceHealth(0.1f * mult);
        }
        if (this.lungeFallTimer > 0.0f) {
            this.lungeFallTimer -= GameTime.getInstance().getThirtyFPSMultiplier();
        }
        if (this.getMeleeDelay() > 0.0f) {
            this.setMeleeDelay(this.getMeleeDelay() - 0.625f * GameTime.getInstance().getMultiplier());
        }
        if (this.getRecoilDelay() > 0.0f) {
            this.setRecoilDelay(this.getRecoilDelay() - 0.625f * GameTime.getInstance().getMultiplier());
        }
        this.sx = 0.0f;
        this.sy = 0.0f;
        if (this.current.getRoom() != null && this.current.getRoom().building.def.isAlarmed() && (!this.isZombie() || Core.tutorial) && !GameClient.client) {
            boolean isInvisible = false;
            if (this instanceof IsoPlayer && (this.isInvisible() || ((IsoPlayer)this).isGhostMode())) {
                isInvisible = true;
            }
            if (!isInvisible && !this.isAnimal()) {
                AmbientStreamManager.instance.doAlarm(this.current.getRoom().def);
            }
        }
        if (!GameServer.server) {
            this.updateSeenVisibility();
        }
        this.llx = this.getLastX();
        this.lly = this.getLastY();
        this.updateMovementStatistics();
        if (this.getClimbRopeTime() != 0.0f && !this.isClimbingRope()) {
            this.setClimbRopeTime(0.0f);
        }
        this.setLastX(this.getX());
        this.setLastY(this.getY());
        this.setLastZ(this.getZ());
        this.updateBeardAndHair();
        this.updateFalling();
        if (this.descriptor != null) {
            this.descriptor.setInstance(this);
        }
        boolean bl = inVehicle = this.vehicle != null;
        if (GameClient.client && !this.isZombie() && this.moodles != null) {
            this.moodles.Update();
        }
        if (!GameClient.client && !this.isZombie()) {
            if (this.characterTraits.get(CharacterTrait.AGORAPHOBIC) && !this.getCurrentSquare().isInARoom()) {
                this.stats.add(CharacterStat.PANIC, 0.5f * GameTime.getInstance().getThirtyFPSMultiplier());
            }
            if (this.characterTraits.get(CharacterTrait.CLAUSTROPHOBIC) && (this.getCurrentSquare().isInARoom() || inVehicle)) {
                int n;
                int n2 = n = inVehicle ? 60 : this.getCurrentSquare().getRoomSize();
                if (n > 0 && n < 70) {
                    float del = PZMath.max(0.0f, 1.0f - (float)n / 70.0f);
                    float panicInc = 0.6f * del * GameTime.getInstance().getThirtyFPSMultiplier();
                    this.stats.add(CharacterStat.PANIC, panicInc);
                }
            }
            if (this.getBodyDamage().getNumPartsBleeding() > 0) {
                float del = (1.0f - this.getBodyDamage().getOverallBodyHealth() / 100.0f) * (float)this.getBodyDamage().getNumPartsBleeding();
                this.stats.add(CharacterStat.PANIC, (this.characterTraits.get(CharacterTrait.HEMOPHOBIC) ? 0.4f : 0.2f) * del * GameTime.getInstance().getThirtyFPSMultiplier());
            }
            if (this.moodles != null) {
                this.moodles.Update();
            }
            if (this.asleep) {
                this.betaEffect = 0.0f;
                this.sleepingTabletEffect = 0.0f;
                this.StopAllActionQueue();
            }
            if (this.betaEffect > 0.0f) {
                this.betaEffect -= GameTime.getInstance().getThirtyFPSMultiplier();
                this.stats.remove(CharacterStat.PANIC, 0.6f * GameTime.getInstance().getThirtyFPSMultiplier());
            } else {
                this.betaDelta = 0.0f;
            }
            if (this.depressFirstTakeTime > 0.0f || this.depressEffect > 0.0f) {
                this.depressFirstTakeTime -= GameTime.getInstance().getThirtyFPSMultiplier();
                if (this.depressFirstTakeTime < 0.0f) {
                    this.depressFirstTakeTime = -1.0f;
                    this.depressEffect -= GameTime.getInstance().getThirtyFPSMultiplier();
                    this.stats.remove(CharacterStat.UNHAPPINESS, 0.03f * GameTime.getInstance().getThirtyFPSMultiplier());
                }
            }
            if (this.depressEffect < 0.0f) {
                this.depressEffect = 0.0f;
            }
            if (this.sleepingTabletEffect > 0.0f) {
                this.sleepingTabletEffect -= GameTime.getInstance().getThirtyFPSMultiplier();
                this.stats.add(CharacterStat.FATIGUE, 0.0016666667f * this.sleepingTabletDelta * GameTime.getInstance().getThirtyFPSMultiplier());
            } else {
                this.sleepingTabletDelta = 0.0f;
            }
            if (this.moodles != null) {
                int panic = this.moodles.getMoodleLevel(MoodleType.PANIC);
                if (panic == 2) {
                    this.stats.remove(CharacterStat.SANITY, 3.2E-7f);
                } else if (panic == 3) {
                    this.stats.remove(CharacterStat.SANITY, 4.8000004E-7f);
                } else if (panic == 4) {
                    this.stats.remove(CharacterStat.SANITY, 8.0E-7f);
                } else if (panic == 0) {
                    this.stats.add(CharacterStat.SANITY, 1.0E-7f);
                }
                int fatigue = this.moodles.getMoodleLevel(MoodleType.TIRED);
                if (fatigue == 4) {
                    this.stats.remove(CharacterStat.SANITY, 2.0E-6f);
                }
            }
        }
        if (!this.characterActions.isEmpty()) {
            BaseAction act = (BaseAction)this.characterActions.get(0);
            boolean valid = act.valid();
            if (valid && !act.started) {
                act.waitToStart();
            } else if (valid && !act.finished() && !act.forceComplete && !act.forceStop) {
                act.update();
            }
            if (!valid || act.finished() || act.forceComplete || act.forceStop) {
                if (act.finished() || act.forceComplete) {
                    act.perform();
                    if (!GameClient.client) {
                        act.complete();
                    }
                    valid = true;
                }
                if ((act.finished() || act.forceComplete) && !act.loopAction || act.forceStop || !valid) {
                    if (act.started && (act.forceStop || !valid)) {
                        act.stop();
                    }
                    this.characterActions.removeElement(act);
                    if (this == IsoPlayer.players[0] || this == IsoPlayer.players[1] || this == IsoPlayer.players[2] || this == IsoPlayer.players[3]) {
                        UIManager.getProgressBar(((IsoPlayer)this).getPlayerNum()).setValue(0.0f);
                    }
                }
                act.forceComplete = false;
            }
            for (int a = 0; a < this.enemyList.size(); ++a) {
                IsoGameCharacter b = (IsoGameCharacter)this.enemyList.get(a);
                if (!b.isDead()) continue;
                this.enemyList.remove(b);
                --a;
            }
        }
        if (SystemDisabler.doCharacterStats && this.getBodyDamage() != null) {
            this.getBodyDamage().Update();
            this.updateBandages();
        }
        if (SystemDisabler.doCharacterStats) {
            this.calculateStats();
        }
        if (this.asleep && this instanceof IsoPlayer && !(this instanceof IsoAnimal)) {
            ((IsoPlayer)this).processWakingUp();
        }
        this.updateIdleSquareTime();
        this.moveForwardVec.x = 0.0f;
        this.moveForwardVec.y = 0.0f;
        if (!this.asleep || !(this instanceof IsoPlayer)) {
            this.setLastX(this.getX());
            this.setLastY(this.getY());
            this.setLastZ(this.getZ());
            this.square = this.getCurrentSquare();
            if (this.sprite != null) {
                if (!this.useParts) {
                    this.sprite.update(this.def);
                } else {
                    this.legsSprite.update(this.def);
                }
            }
            this.setStateEventDelayTimer(this.getStateEventDelayTimer() - GameTime.getInstance().getThirtyFPSMultiplier());
        }
        this.stateMachine.update();
        if (this.isZombie() && VirtualZombieManager.instance.isReused((IsoZombie)this)) {
            DebugLog.log(DebugType.Zombie, "Zombie added to ReusableZombies after stateMachine.update - RETURNING " + String.valueOf(this));
            return;
        }
        if (this instanceof IsoPlayer) {
            this.ensureOnTile();
        }
        if ((this instanceof IsoPlayer || this instanceof IsoSurvivor) && this.remoteId == -1 && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
            RainManager.SetPlayerLocation(((IsoPlayer)this).getPlayerNum(), this.getCurrentSquare());
        }
        this.FireCheck();
        this.SpreadFire();
        this.ReduceHealthWhenBurning();
        this.updateTextObjects();
        if (this.stateMachine.getCurrent() == StaggerBackState.instance()) {
            if (this.getStateEventDelayTimer() > 20.0f) {
                this.bloodImpactX = this.getX();
                this.bloodImpactY = this.getY();
                this.bloodImpactZ = this.getZ();
            }
        } else {
            this.bloodImpactX = this.getX();
            this.bloodImpactY = this.getY();
            this.bloodImpactZ = this.getZ();
        }
        if (!this.isZombie()) {
            this.recursiveItemUpdater(this.inventory);
        }
        this.lastZombieKills = this.zombieKills;
        if (this.attachedAnimSprite != null) {
            int n = this.attachedAnimSprite.size();
            for (int i = 0; i < n; ++i) {
                IsoSpriteInstance s = (IsoSpriteInstance)this.attachedAnimSprite.get(i);
                IsoSprite sp = s.parentSprite;
                s.update();
                if (!sp.hasAnimation()) continue;
                s.frame += s.animFrameIncrease * (GameTime.instance.getMultipliedSecondsSinceLastUpdate() * 60.0f);
                if ((int)s.frame < sp.currentAnim.frames.size() || !sp.loop || !s.looped) continue;
                s.frame = 0.0f;
            }
        }
        if (this.isGodMod()) {
            this.getStats().reset(CharacterStat.FATIGUE);
            this.getStats().reset(CharacterStat.ENDURANCE);
            this.getStats().reset(CharacterStat.TEMPERATURE);
            this.getStats().reset(CharacterStat.HUNGER);
            if (this instanceof IsoPlayer) {
                ((IsoPlayer)this).resetSleepingPillsTaken();
            }
        }
        this.updateMovementMomentum();
        if (this.effectiveEdibleBuffTimer > 0.0f) {
            this.effectiveEdibleBuffTimer -= GameTime.getInstance().getMultiplier() * 0.015f;
            if (this.effectiveEdibleBuffTimer < 0.0f) {
                this.effectiveEdibleBuffTimer = 0.0f;
            }
        }
        if (!GameServer.server || GameClient.client || !GameClient.client) {
            this.updateDirt();
        }
        if (this.useHandWeapon != null && this.useHandWeapon.isAimedFirearm() && this.isAiming()) {
            if (this instanceof IsoPlayer && this.isLocal()) {
                ((IsoPlayer)this).setAngleFromAim();
            }
            this.updateBallistics();
        }
    }

    private boolean isInGrapplerState() {
        if (this.actionContext == null) {
            return false;
        }
        ActionState currentState = this.actionContext.getCurrentState();
        if (currentState == null) {
            return false;
        }
        return currentState.isGrapplerState();
    }

    private void updateSeenVisibility() {
        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
            this.updateSeenVisibility(playerIndex);
        }
    }

    private void updateSeenVisibility(int playerIndex) {
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player == null) {
            return;
        }
        this.isVisibleToPlayer[playerIndex] = this.TestIfSeen(playerIndex, player);
        if (this.isVisibleToPlayer[playerIndex]) {
            return;
        }
        if (this instanceof IsoPlayer) {
            return;
        }
        if (player.isSeeEveryone()) {
            return;
        }
        this.setTargetAlpha(playerIndex, 0.0f);
    }

    private void recursiveItemUpdater(ItemContainer container) {
        for (int m = 0; m < container.items.size(); ++m) {
            InventoryItem item = container.items.get(m);
            if (item instanceof InventoryContainer) {
                InventoryContainer inventoryContainer = (InventoryContainer)item;
                this.recursiveItemUpdater(inventoryContainer);
            }
            if (!(item instanceof IUpdater)) continue;
            item.update();
        }
    }

    private void recursiveItemUpdater(InventoryContainer container) {
        for (int m = 0; m < container.getInventory().getItems().size(); ++m) {
            InventoryItem item = container.getInventory().getItems().get(m);
            if (item instanceof InventoryContainer) {
                InventoryContainer inventoryContainer = (InventoryContainer)item;
                this.recursiveItemUpdater(inventoryContainer);
            }
            if (!(item instanceof IUpdater)) continue;
            item.update();
        }
    }

    private void updateDirt() {
        IsoPlayer player;
        float temperature;
        if (this.isZombie() || this.getBodyDamage() == null) {
            return;
        }
        int dirtNbr = 0;
        if (this.isRunning() && Rand.NextBool(Rand.AdjustForFramerate(3500))) {
            dirtNbr = 1;
        }
        if (this.isSprinting() && Rand.NextBool(Rand.AdjustForFramerate(2500))) {
            dirtNbr += Rand.Next(1, 3);
        }
        if ((temperature = this.stats.get(CharacterStat.TEMPERATURE)) > 37.0f && Rand.NextBool(Rand.AdjustForFramerate(5000))) {
            ++dirtNbr;
        }
        if (temperature > 38.0f && Rand.NextBool(Rand.AdjustForFramerate(3000))) {
            ++dirtNbr;
        }
        if ((player = Type.tryCastTo(this, IsoPlayer.class)) != null && player.isPlayerMoving() || player == null && this.isMoving()) {
            boolean bMuddyFloor;
            float puddle = this.square == null ? 0.0f : this.square.getPuddlesInGround();
            boolean bl = bMuddyFloor = this.square != null && this.isOutside() && this.square.hasNaturalFloor() && IsoPuddles.getInstance().getWetGroundFinalValue() > 0.5f;
            if (puddle > 0.09f && Rand.NextBool(Rand.AdjustForFramerate(1500))) {
                ++dirtNbr;
            } else if (bMuddyFloor && Rand.NextBool(Rand.AdjustForFramerate(3000))) {
                ++dirtNbr;
            }
            if (dirtNbr > 0) {
                this.addDirt(null, dirtNbr, true);
            }
            dirtNbr = 0;
            if (puddle > 0.09f && Rand.NextBool(Rand.AdjustForFramerate(1500))) {
                ++dirtNbr;
            } else if (bMuddyFloor && Rand.NextBool(Rand.AdjustForFramerate(3000))) {
                ++dirtNbr;
            }
            if (this.isInTrees() && Rand.NextBool(Rand.AdjustForFramerate(1500))) {
                ++dirtNbr;
            }
            if (dirtNbr > 0) {
                this.addDirt(null, dirtNbr, false);
            }
        }
    }

    protected void updateMovementMomentum() {
        float dt = GameTime.instance.getTimeDelta();
        if (this.isPlayerMoving() && !this.isAiming()) {
            float timeToFullMomentum = 0.55f;
            float time = this.momentumScalar * 0.55f;
            if (time >= 0.55f) {
                this.momentumScalar = 1.0f;
                return;
            }
            float newTime = time + dt;
            float alpha = newTime / 0.55f;
            this.momentumScalar = PZMath.clamp(alpha, 0.0f, 1.0f);
        } else {
            float timeToZeroMomentum = 0.25f;
            float time = (1.0f - this.momentumScalar) * 0.25f;
            if (time >= 0.25f) {
                this.momentumScalar = 0.0f;
                return;
            }
            float newTime = time + dt;
            float alpha = newTime / 0.25f;
            float clampedAlpha = PZMath.clamp(alpha, 0.0f, 1.0f);
            this.momentumScalar = 1.0f - clampedAlpha;
        }
    }

    @Override
    public double getHoursSurvived() {
        return GameTime.instance.getWorldAgeHours();
    }

    private void updateBeardAndHair() {
        int i;
        ArrayList<Object> allStyles;
        int level;
        Object currentStyle;
        if (this.isZombie() || this.isAnimal()) {
            return;
        }
        if (this instanceof IsoPlayer && !((IsoPlayer)this).isLocalPlayer()) {
            return;
        }
        float hoursSurvived = (float)this.getHoursSurvived();
        if (this.beardGrowTiming < 0.0f || this.beardGrowTiming > hoursSurvived) {
            this.beardGrowTiming = hoursSurvived;
        }
        if (this.hairGrowTiming < 0.0f || this.hairGrowTiming > hoursSurvived) {
            this.hairGrowTiming = hoursSurvived;
        }
        boolean canSleep = !GameClient.client && !GameServer.server || ServerOptions.instance.sleepAllowed.getValue() && ServerOptions.instance.sleepNeeded.getValue();
        boolean updated = false;
        if ((this.isAsleep() || !canSleep) && hoursSurvived - this.beardGrowTiming > 120.0f) {
            this.beardGrowTiming = hoursSurvived;
            currentStyle = BeardStyles.instance.FindStyle(((HumanVisual)this.getVisual()).getBeardModel());
            level = 1;
            if (currentStyle != null) {
                level = ((BeardStyle)currentStyle).level;
            }
            allStyles = BeardStyles.instance.getAllStyles();
            for (i = 0; i < allStyles.size(); ++i) {
                if (!allStyles.get((int)i).growReference || ((BeardStyle)allStyles.get((int)i)).level != level + 1) continue;
                ((HumanVisual)this.getVisual()).setBeardModel(((BeardStyle)allStyles.get((int)i)).name);
                updated = true;
                break;
            }
        }
        if ((this.isAsleep() || !canSleep) && hoursSurvived - this.hairGrowTiming > 480.0f) {
            this.hairGrowTiming = hoursSurvived;
            currentStyle = HairStyles.instance.FindMaleStyle(((HumanVisual)this.getVisual()).getHairModel());
            if (this.isFemale()) {
                currentStyle = HairStyles.instance.FindFemaleStyle(((HumanVisual)this.getVisual()).getHairModel());
            }
            level = 1;
            if (currentStyle != null) {
                level = ((HairStyle)currentStyle).level;
            }
            allStyles = HairStyles.instance.maleStyles;
            if (this.isFemale()) {
                allStyles = HairStyles.instance.femaleStyles;
            }
            for (i = 0; i < allStyles.size(); ++i) {
                HairStyle style = (HairStyle)allStyles.get(i);
                if (!style.growReference || style.level != level + 1) continue;
                ((HumanVisual)this.getVisual()).setHairModel(style.name);
                ((HumanVisual)this.getVisual()).setNonAttachedHair(null);
                updated = true;
                break;
            }
        }
        if (updated) {
            this.resetModelNextFrame();
            LuaEventManager.triggerEvent("OnClothingUpdated", this);
            if (GameClient.client) {
                GameClient.instance.sendVisual((IsoPlayer)this);
            }
        }
    }

    private void updateFalling() {
        if (this instanceof IsoPlayer && !this.isClimbing()) {
            IsoRoofFixer.FixRoofsAt(this.current);
        }
        if (!this.shouldBeFalling()) {
            this.setFallTime(0.0f);
            this.lastFallSpeed = 0.0f;
            this.falling = false;
            this.isOnGround = true;
            return;
        }
        float dt = GameTime.getInstance().getTimeDelta();
        if (GameServer.server) {
            dt *= 0.16f;
        }
        float fallSpeedDelta = 5.0010414f * dt;
        float fallSpeed = this.lastFallSpeed + fallSpeedDelta;
        float fallDelta = this.lastFallSpeed * dt + 2.5005207f * dt * dt;
        float newZ = this.getZ() - fallDelta;
        float heightAboveFloor = this.getHeightAboveFloor();
        float floorZ = this.getZ() - heightAboveFloor;
        if (newZ < floorZ) {
            this.setZ(floorZ);
            float impactFraction = heightAboveFloor / fallDelta;
            float impactSpeed = this.lastFallSpeed + fallSpeedDelta * impactFraction;
            this.fallTime = impactSpeed / 5.0010414f;
            this.DoLand(impactSpeed);
            this.setFallTime(0.0f);
            this.lastFallSpeed = 0.0f;
            this.falling = false;
            this.isOnGround = true;
        } else {
            this.setZ(newZ);
            this.fallTime = fallSpeed / 5.0010414f;
            this.lastFallSpeed = fallSpeed;
            this.falling = FallingConstants.isFall(fallSpeed);
            this.isOnGround = false;
        }
        this.llz = this.getLastZ();
    }

    @Override
    public boolean shouldSnapZToCurrentSquare() {
        return this.isOnGround && !this.isFalling() && !this.shouldBeFalling() && !this.isRagdoll();
    }

    public boolean shouldBeFalling() {
        IGrappleable iGrappleable;
        if (this instanceof IsoAnimal && ((IsoAnimal)this).isOnHook()) {
            return false;
        }
        if (this.isSeatedInVehicle()) {
            return false;
        }
        if (this.isClimbing()) {
            return false;
        }
        if (this.isRagdollFall()) {
            return false;
        }
        if (this.isCurrentState(ClimbOverFenceState.instance())) {
            return false;
        }
        if (this.isCurrentState(ClimbThroughWindowState.instance())) {
            return false;
        }
        if (this.isBeingGrappled() && (iGrappleable = this.getGrappledBy()) instanceof IsoGameCharacter) {
            IsoGameCharacter grappler = (IsoGameCharacter)iGrappleable;
            return grappler.isGrappleThrowOutWindow() || grappler.isGrappleThrowOverFence();
        }
        return true;
    }

    public float getHeightAboveFloor() {
        float apparentZ;
        if (this.current == null) {
            return 1.0f;
        }
        if (this.current.HasStairs()) {
            apparentZ = this.current.getApparentZ(this.getX() - (float)PZMath.fastfloor(this.getX()), this.getY() - (float)PZMath.fastfloor(this.getY()));
            if (this.getZ() >= apparentZ) {
                return this.getZ() - apparentZ;
            }
        }
        if (this.current.hasSlopedSurface()) {
            apparentZ = this.current.getApparentZ(this.getX() - (float)PZMath.fastfloor(this.getX()), this.getY() - (float)PZMath.fastfloor(this.getY()));
            if (this.getZ() >= apparentZ) {
                return this.getZ() - apparentZ;
            }
        }
        if (this.current.TreatAsSolidFloor()) {
            return this.getZ() - (float)this.current.getZ();
        }
        if (this.current.chunk == null) {
            return this.getZ();
        }
        if (this.current.z == this.current.chunk.minLevel) {
            return this.getZ();
        }
        for (int i = this.current.z; i >= this.current.chunk.minLevel; --i) {
            IsoGridSquare below = this.getCell().getGridSquare(this.current.x, this.current.y, i);
            if (below == null) continue;
            if (below.HasStairs()) {
                float apparentZ2 = below.getApparentZ(this.getX() - (float)PZMath.fastfloor(this.getX()), this.getY() - (float)PZMath.fastfloor(this.getY()));
                return this.getZ() - apparentZ2;
            }
            if (below.hasSlopedSurface()) {
                float apparentZ3 = below.getApparentZ(this.getX() - (float)PZMath.fastfloor(this.getX()), this.getY() - (float)PZMath.fastfloor(this.getY()));
                return this.getZ() - apparentZ3;
            }
            if (!below.TreatAsSolidFloor()) continue;
            return this.getZ() - (float)below.getZ();
        }
        return 1.0f;
    }

    protected void updateMovementRates() {
    }

    protected float calculateIdleSpeed() {
        float result = 0.01f;
        return result += (float)this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) * 2.5f / 10.0f;
    }

    public float calculateBaseSpeed() {
        float result = 0.8f;
        float bagRunSpeedModifier = 1.0f;
        if (this.getMoodles() != null) {
            result -= (float)this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) * 0.15f;
            result -= (float)this.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) * 0.15f;
        }
        if (this.getMoodles().getMoodleLevel(MoodleType.PANIC) >= 3 && this.characterTraits.get(CharacterTrait.ADRENALINE_JUNKIE)) {
            int mul = this.getMoodles().getMoodleLevel(MoodleType.PANIC) + 1;
            result += (float)mul / 20.0f;
        }
        for (int i = BodyPartType.ToIndex(BodyPartType.Torso_Upper); i < BodyPartType.ToIndex(BodyPartType.Neck) + 1; ++i) {
            BodyPart part = this.getBodyDamage().getBodyPart(BodyPartType.FromIndex(i));
            if (part.HasInjury()) {
                result -= 0.1f;
            }
            if (!part.bandaged()) continue;
            result += 0.05f;
        }
        BodyPart part = this.getBodyDamage().getBodyPart(BodyPartType.UpperLeg_L);
        if (part.getAdditionalPain(true) > 20.0f) {
            result -= (part.getAdditionalPain(true) - 20.0f) / 100.0f;
        }
        for (int i = 0; i < this.bagsWorn.size(); ++i) {
            InventoryContainer bag = this.bagsWorn.get(i);
            bagRunSpeedModifier += this.calcRunSpeedModByBag(bag);
        }
        if (this.getPrimaryHandItem() != null && this.getPrimaryHandItem() instanceof InventoryContainer) {
            bagRunSpeedModifier += this.calcRunSpeedModByBag((InventoryContainer)this.getPrimaryHandItem());
        }
        if (this.getSecondaryHandItem() != null && this.getSecondaryHandItem() instanceof InventoryContainer) {
            bagRunSpeedModifier += this.calcRunSpeedModByBag((InventoryContainer)this.getSecondaryHandItem());
        }
        if (this.isOutside()) {
            if (this.getCurrentSquare().hasNaturalFloor()) {
                result -= IsoPuddles.getInstance().getWetGroundFinalValue() * 0.25f;
            }
            if (this.getCurrentSquare().hasSand()) {
                result -= 0.05f;
            }
        }
        this.fullSpeedMod = this.runSpeedModifier + (bagRunSpeedModifier - 1.0f);
        return result * (1.0f - Math.abs(1.0f - this.fullSpeedMod) / 2.0f);
    }

    private float calcRunSpeedModByClothing() {
        float result = 0.0f;
        int count = 0;
        for (int i = 0; i < this.wornItems.size(); ++i) {
            Clothing clothing;
            InventoryItem item = this.wornItems.getItemByIndex(i);
            if (!(item instanceof Clothing) || (clothing = (Clothing)item).getRunSpeedModifier() == 1.0f) continue;
            result += clothing.getRunSpeedModifier();
            ++count;
        }
        if (result == 0.0f && count == 0) {
            result = 1.0f;
            count = 1;
        }
        if (this.getWornItem(ItemBodyLocation.SHOES) == null) {
            result *= 0.8f;
        }
        return result / (float)count;
    }

    private float calcRunSpeedModByBag(InventoryContainer bag) {
        float runBagMod = bag.getScriptItem().runSpeedModifier - 1.0f;
        float deltaWeight = bag.getContentsWeight() / (float)bag.getEffectiveCapacity(this);
        return runBagMod *= 1.0f + deltaWeight / 2.0f;
    }

    public float calculateCombatSpeed() {
        float combatSpeed = 0.8f;
        InventoryItem weapon = null;
        if (this.getPrimaryHandItem() != null && this.getPrimaryHandItem() instanceof HandWeapon) {
            weapon = (HandWeapon)this.getPrimaryHandItem();
            combatSpeed *= ((HandWeapon)this.getPrimaryHandItem()).getBaseSpeed();
        }
        WeaponType weaponType = WeaponType.getWeaponType(this);
        if (weapon != null && weapon.isTwoHandWeapon() && this.getSecondaryHandItem() != weapon) {
            combatSpeed *= 0.77f;
        }
        if (weapon != null && ((HandWeapon)weapon).isOfWeaponCategory(WeaponCategory.AXE)) {
            combatSpeed *= this.getChopTreeSpeed();
        }
        combatSpeed -= (float)this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) * 0.07f;
        combatSpeed -= (float)this.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) * 0.07f;
        combatSpeed += (float)this.getWeaponLevel() * 0.03f;
        combatSpeed += (float)this.getPerkLevel(PerkFactory.Perks.Fitness) * 0.02f;
        if (this.getSecondaryHandItem() != null && this.getSecondaryHandItem() instanceof InventoryContainer) {
            combatSpeed *= 0.95f;
        }
        combatSpeed *= Rand.Next(1.1f, 1.2f);
        combatSpeed *= this.combatSpeedModifier;
        combatSpeed *= this.getArmsInjurySpeedModifier();
        if (this.getBodyDamage() != null && this.getBodyDamage().getThermoregulator() != null) {
            combatSpeed *= this.getBodyDamage().getThermoregulator().getCombatModifier();
        }
        combatSpeed = Math.min(1.6f, combatSpeed);
        combatSpeed = Math.max(0.8f, combatSpeed);
        if (weapon != null && weapon.isTwoHandWeapon() && weaponType == WeaponType.HEAVY) {
            combatSpeed *= 1.2f;
        }
        return combatSpeed;
    }

    private float getArmsInjurySpeedModifier() {
        float speed = 1.0f;
        BodyPart bodyPart = this.getBodyDamage().getBodyPart(BodyPartType.Hand_R);
        float modifier = this.calculateInjurySpeed(bodyPart, true);
        if (modifier > 0.0f) {
            speed -= modifier;
        }
        if ((modifier = this.calculateInjurySpeed(bodyPart = this.getBodyDamage().getBodyPart(BodyPartType.ForeArm_R), true)) > 0.0f) {
            speed -= modifier;
        }
        if ((modifier = this.calculateInjurySpeed(bodyPart = this.getBodyDamage().getBodyPart(BodyPartType.UpperArm_R), true)) > 0.0f) {
            speed -= modifier;
        }
        return speed;
    }

    private float getFootInjurySpeedModifier() {
        boolean left = true;
        float leftInjuries = 0.0f;
        float rightInjuries = 0.0f;
        for (int i = BodyPartType.ToIndex(BodyPartType.Groin); i < BodyPartType.ToIndex(BodyPartType.MAX); ++i) {
            float speed = this.calculateInjurySpeed(this.getBodyDamage().getBodyPart(BodyPartType.FromIndex(i)), false);
            if (left) {
                leftInjuries += speed;
            } else {
                rightInjuries += speed;
            }
            left = !left;
        }
        if (leftInjuries > rightInjuries) {
            return -(leftInjuries + rightInjuries);
        }
        return leftInjuries + rightInjuries;
    }

    private float calculateInjurySpeed(BodyPart bodyPart, boolean doPain) {
        float scratchModifier = bodyPart.getScratchSpeedModifier();
        float cutModifier = bodyPart.getCutSpeedModifier();
        float burnModifier = bodyPart.getBurnSpeedModifier();
        float deepWoundModifier = bodyPart.getDeepWoundSpeedModifier();
        float modifier = 0.0f;
        if ((bodyPart.getType() == BodyPartType.Foot_L || bodyPart.getType() == BodyPartType.Foot_R) && (bodyPart.getBurnTime() > 5.0f || bodyPart.getStitchTime() > 0.0f || bodyPart.getBiteTime() > 0.0f || bodyPart.deepWounded() || bodyPart.isSplint() || bodyPart.getFractureTime() > 0.0f || bodyPart.haveGlass())) {
            modifier = 1.0f;
            if (bodyPart.getStitchTime() > 0.0f) {
                modifier = 0.7f;
            }
            if (bodyPart.bandaged()) {
                modifier *= 0.7f;
            }
            if (bodyPart.getFractureTime() > 0.0f) {
                modifier = this.calcFractureInjurySpeed(bodyPart);
            }
        }
        if (bodyPart.haveBullet()) {
            return 1.0f;
        }
        if (bodyPart.getScratchTime() > 2.0f || bodyPart.getCutTime() > 5.0f || bodyPart.getBurnTime() > 0.0f || bodyPart.getDeepWoundTime() > 0.0f || bodyPart.isSplint() || bodyPart.getFractureTime() > 0.0f || bodyPart.getBiteTime() > 0.0f) {
            modifier += bodyPart.getScratchTime() / scratchModifier + bodyPart.getCutTime() / cutModifier + bodyPart.getBurnTime() / burnModifier + bodyPart.getDeepWoundTime() / deepWoundModifier;
            modifier += bodyPart.getBiteTime() / 20.0f;
            if (bodyPart.bandaged()) {
                modifier /= 2.0f;
            }
            if (bodyPart.getFractureTime() > 0.0f) {
                modifier = this.calcFractureInjurySpeed(bodyPart);
            }
        }
        if (doPain && bodyPart.getPain() > 20.0f) {
            modifier += bodyPart.getPain() / 10.0f;
        }
        return modifier;
    }

    private float calcFractureInjurySpeed(BodyPart bodyPart) {
        float result = 0.4f;
        if (bodyPart.getFractureTime() > 10.0f) {
            result = 0.7f;
        }
        if (bodyPart.getFractureTime() > 20.0f) {
            result = 1.0f;
        }
        if (bodyPart.getSplintFactor() > 0.0f) {
            result -= 0.2f;
            result -= Math.min(bodyPart.getSplintFactor() / 10.0f, 0.8f);
        }
        return Math.max(0.0f, result);
    }

    protected void calculateWalkSpeed() {
        IsoTree tree;
        IsoGridSquare currentSquare;
        if (this instanceof IsoPlayer && GameClient.client) {
            return;
        }
        if (this instanceof IsoPlayer && !((IsoPlayer)this).getAttachedAnimals().isEmpty()) {
            float injurySpeed = this.getFootInjurySpeedModifier();
            this.setVariable("WalkInjury", injurySpeed);
            this.setVariable("WalkSpeed", 0.0f);
            return;
        }
        float sneakLimpSpeedScale = 1.0f;
        float injurySpeed = this.getFootInjurySpeedModifier();
        this.setVariable("WalkInjury", injurySpeed);
        float walkSpeed = this.calculateBaseSpeed();
        if (this.running || this.sprinting) {
            walkSpeed -= 0.15f;
            walkSpeed *= this.fullSpeedMod;
            walkSpeed += (float)this.getPerkLevel(PerkFactory.Perks.Sprinting) / 20.0f;
            walkSpeed -= Math.abs(injurySpeed / 1.5f);
            if ("Tutorial".equals(Core.gameMode)) {
                walkSpeed = Math.max(1.0f, walkSpeed);
            }
        } else if (Math.abs(injurySpeed) > 0.1f) {
            boolean bIsSneaking = this.isSneaking();
            boolean bNearWallCrouching = this.getVariable("nearWallCrouching").getValueBool();
            if (bIsSneaking) {
                sneakLimpSpeedScale = bNearWallCrouching ? 0.45f : 0.6f;
            }
        } else {
            walkSpeed *= this.walkSpeedModifier;
        }
        this.setSneakLimpSpeedScale(sneakLimpSpeedScale);
        if (this.getSlowFactor() > 0.0f) {
            walkSpeed *= 0.05f;
        }
        walkSpeed = Math.min(1.0f, walkSpeed);
        if (this.getBodyDamage() != null && this.getBodyDamage().getThermoregulator() != null) {
            walkSpeed *= this.getBodyDamage().getThermoregulator().getMovementModifier();
        }
        if (this.isAiming()) {
            float strafeSpeed = Math.min(0.9f + (float)this.getPerkLevel(PerkFactory.Perks.Nimble) / 10.0f, 1.5f);
            float walkSpeedModifier = Math.min(walkSpeed * 2.5f, 1.0f);
            strafeSpeed *= walkSpeedModifier;
            strafeSpeed = Math.max(strafeSpeed, 0.48f);
            this.setVariable("StrafeSpeed", strafeSpeed * 0.8f);
        }
        if (this.isInTreesNoBush() && (currentSquare = this.getCurrentSquare()) != null && currentSquare.has(IsoObjectType.tree) && (tree = currentSquare.getTree()) != null) {
            walkSpeed *= tree.getSlowFactor(this);
        }
        this.setVariable("WalkSpeed", walkSpeed);
    }

    public void updateSpeedModifiers() {
        this.runSpeedModifier = 1.0f;
        this.walkSpeedModifier = 1.0f;
        this.combatSpeedModifier = 1.0f;
        this.bagsWorn.clear();
        for (int i = 0; i < this.getWornItems().size(); ++i) {
            InventoryItem item = this.getWornItems().getItemByIndex(i);
            if (item instanceof Clothing) {
                Clothing clothing = (Clothing)item;
                this.combatSpeedModifier += clothing.getCombatSpeedModifier() - 1.0f;
            }
            if (!(item instanceof InventoryContainer)) continue;
            InventoryContainer bag = (InventoryContainer)item;
            this.combatSpeedModifier += bag.getScriptItem().combatSpeedModifier - 1.0f;
            this.bagsWorn.add(bag);
        }
        InventoryItem item = this.getWornItems().getItem(ItemBodyLocation.SHOES);
        if (item == null || item.getCondition() == 0) {
            this.runSpeedModifier *= 0.85f;
            this.walkSpeedModifier *= 0.85f;
        }
    }

    public void updateDiscomfortModifiers() {
        this.clothingDiscomfortModifier = 0.0f;
        for (int i = 0; i < this.getWornItems().size(); ++i) {
            InventoryItem item = this.getWornItems().getItemByIndex(i);
            if (item instanceof Clothing) {
                Clothing clothing = (Clothing)item;
                this.clothingDiscomfortModifier += clothing.getDiscomfortModifier();
            }
            if (!(item instanceof InventoryContainer)) continue;
            InventoryContainer bag = (InventoryContainer)item;
            this.clothingDiscomfortModifier += bag.getScriptItem().discomfortModifier;
        }
        this.clothingDiscomfortModifier = Math.max(this.clothingDiscomfortModifier, 0.0f);
    }

    public void DoFloorSplat(IsoGridSquare sq, String id, boolean bFlip, float offZ, float alpha) {
        if (sq == null) {
            return;
        }
        sq.DirtySlice();
        IsoObject best = null;
        for (int i = 0; i < sq.getObjects().size(); ++i) {
            IsoObject obj = sq.getObjects().get(i);
            if (obj.sprite == null || !obj.sprite.getProperties().has(IsoFlagType.solidfloor) || best != null) continue;
            best = obj;
        }
        if (best != null && best.sprite != null && (best.sprite.getProperties().has(IsoFlagType.vegitation) || best.sprite.getProperties().has(IsoFlagType.solidfloor))) {
            IsoSprite spr1 = IsoSprite.getSprite(IsoSpriteManager.instance, id, 0);
            if (spr1 == null) {
                return;
            }
            if (best.attachedAnimSprite.size() > 7) {
                return;
            }
            IsoSpriteInstance spr = IsoSpriteInstance.get(spr1);
            best.attachedAnimSprite.add(spr);
            best.attachedAnimSprite.get((int)(best.attachedAnimSprite.size() - 1)).flip = bFlip;
            best.attachedAnimSprite.get((int)(best.attachedAnimSprite.size() - 1)).tintr = 0.5f + (float)Rand.Next(100) / 2000.0f;
            best.attachedAnimSprite.get((int)(best.attachedAnimSprite.size() - 1)).tintg = 0.7f + (float)Rand.Next(300) / 1000.0f;
            best.attachedAnimSprite.get((int)(best.attachedAnimSprite.size() - 1)).tintb = 0.7f + (float)Rand.Next(300) / 1000.0f;
            best.attachedAnimSprite.get(best.attachedAnimSprite.size() - 1).SetAlpha(0.4f * alpha * 0.6f);
            best.attachedAnimSprite.get(best.attachedAnimSprite.size() - 1).SetTargetAlpha(0.4f * alpha * 0.6f);
            best.attachedAnimSprite.get((int)(best.attachedAnimSprite.size() - 1)).offZ = -offZ;
            best.attachedAnimSprite.get((int)(best.attachedAnimSprite.size() - 1)).offX = 0.0f;
        }
    }

    void DoSplat(IsoGridSquare sq, String id, boolean bFlip, IsoFlagType prop, float offX, float offZ, float alpha) {
        if (sq == null) {
            return;
        }
        sq.DoSplat(id, bFlip, prop, offX, offZ, alpha);
    }

    @Override
    public boolean onMouseLeftClick(int x, int y) {
        if (IsoCamera.getCameraCharacter() != IsoPlayer.getInstance() && Core.debug) {
            IsoCamera.setCameraCharacter(this);
        }
        return super.onMouseLeftClick(x, y);
    }

    protected void calculateStats() {
        if (this.isAnimal()) {
            return;
        }
        if (!(!GameServer.server || ServerOptions.instance.sleepAllowed.getValue() && ServerOptions.instance.sleepNeeded.getValue())) {
            this.stats.reset(CharacterStat.FATIGUE);
        }
        if (LuaHookManager.TriggerHook("CalculateStats", this)) {
            return;
        }
        this.updateEndurance();
        this.updateTripping();
        this.updateThirst();
        this.updateStress();
        this.updateStats_WakeState();
        this.updateMorale();
        this.updateFitness();
    }

    protected void updateStats_WakeState() {
        if (this.isAnimal()) {
            return;
        }
        if (GameServer.server || !GameClient.client && IsoPlayer.getInstance() == this) {
            if (this.asleep) {
                this.updateStats_Sleeping();
            } else {
                this.updateStats_Awake();
            }
        }
    }

    protected void updateStats_Sleeping() {
    }

    protected void updateStats_Awake() {
        this.stats.remove(CharacterStat.STRESS, (float)(ZomboidGlobals.stressReduction * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay()));
        float fatiguemod = 1.0f - this.stats.get(CharacterStat.ENDURANCE);
        if (fatiguemod < 0.3f) {
            fatiguemod = 0.3f;
        }
        float mul = 1.0f;
        if (this.characterTraits.get(CharacterTrait.NEEDS_LESS_SLEEP)) {
            mul = 0.7f;
        }
        if (this.characterTraits.get(CharacterTrait.NEEDS_MORE_SLEEP)) {
            mul = 1.3f;
        }
        float mod = 1.0f;
        if (this.isSitOnGround() || this.isSittingOnFurniture() || this.isResting()) {
            mod = 1.5f;
        }
        this.stats.add(CharacterStat.FATIGUE, (float)(ZomboidGlobals.fatigueIncrease * SandboxOptions.instance.getStatsDecreaseMultiplier() * (double)fatiguemod * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay() * (double)mul * this.getFatiqueMultiplier() / (double)mod));
        float hungerMult = this.getAppetiteMultiplier();
        if (this instanceof IsoPlayer && ((IsoPlayer)this).IsRunning() && this.isPlayerMoving() || this.isCurrentState(SwipeStatePlayer.instance())) {
            if (this.moodles.getMoodleLevel(MoodleType.FOOD_EATEN) == 0) {
                this.stats.add(CharacterStat.HUNGER, (float)(ZomboidGlobals.hungerIncreaseWhenExercise / 3.0 * SandboxOptions.instance.getStatsDecreaseMultiplier() * (double)hungerMult * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay() * this.getHungerMultiplier()));
            } else {
                this.stats.add(CharacterStat.HUNGER, (float)(ZomboidGlobals.hungerIncreaseWhenExercise * SandboxOptions.instance.getStatsDecreaseMultiplier() * (double)hungerMult * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay() * this.getHungerMultiplier()));
            }
        } else if (this.moodles.getMoodleLevel(MoodleType.FOOD_EATEN) == 0) {
            this.stats.add(CharacterStat.HUNGER, (float)(ZomboidGlobals.hungerIncrease * SandboxOptions.instance.getStatsDecreaseMultiplier() * (double)hungerMult * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay() * this.getHungerMultiplier()));
        } else {
            this.stats.add(CharacterStat.HUNGER, (float)(ZomboidGlobals.hungerIncreaseWhenWellFed * SandboxOptions.instance.getStatsDecreaseMultiplier() * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay() * this.getHungerMultiplier()));
        }
        this.updateIdleSquareTime();
        if (this.isInCombat()) {
            this.stats.reset(CharacterStat.IDLENESS);
        } else if (this.isCurrentlyIdle() && this.getCurrentSquare() != null) {
            if (this.getCurrentSquare() == this.getLastSquare() && this.getIdleSquareTime() >= 1800.0f) {
                this.stats.set(CharacterStat.IDLENESS, (float)((double)this.stats.get(CharacterStat.IDLENESS) + ZomboidGlobals.idleIncreaseRate * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay()));
            }
            if (this.getCurrentSquare().isInARoom()) {
                this.stats.set(CharacterStat.IDLENESS, (float)((double)this.stats.get(CharacterStat.IDLENESS) + ZomboidGlobals.idleIncreaseRate / 3.0 * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay()));
            }
        } else if (!this.isSittingOnFurniture() && !this.isSitOnGround()) {
            this.stats.set(CharacterStat.IDLENESS, (float)((double)this.stats.get(CharacterStat.IDLENESS) - ZomboidGlobals.idleDecreaseRate * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay()));
        }
    }

    private void updateMorale() {
        float mod = 1.0f - this.stats.getNicotineStress() - 0.5f;
        if ((mod *= 1.0E-4f) > 0.0f) {
            mod += 0.5f;
        }
        this.stats.add(CharacterStat.MORALE, PZMath.clamp(mod, 0.0f, 1.0f));
    }

    private void updateFitness() {
        this.stats.set(CharacterStat.FITNESS, (float)this.getPerkLevel(PerkFactory.Perks.Fitness) / 5.0f - 1.0f);
    }

    private void updateTripping() {
        if (this.stats.isTripping()) {
            this.stats.addTrippingRotAngle(0.06f);
        }
    }

    protected float getAppetiteMultiplier() {
        float hungerMult = 1.0f - this.stats.get(CharacterStat.HUNGER);
        if (this.characterTraits.get(CharacterTrait.HEARTY_APPETITE)) {
            hungerMult *= 1.5f;
        }
        if (this.characterTraits.get(CharacterTrait.LIGHT_EATER)) {
            hungerMult *= 0.75f;
        }
        return hungerMult;
    }

    private void updateStress() {
        if (this.isAnimal()) {
            return;
        }
        if (!this.hasTrait(CharacterTrait.DEAF)) {
            this.stats.add(CharacterStat.STRESS, (float)((double)WorldSoundManager.instance.getStressFromSounds(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ())) * ZomboidGlobals.stressFromSoundsMultiplier));
        }
        if (this.getBodyDamage().getNumPartsBitten() > 0) {
            this.stats.add(CharacterStat.STRESS, (float)(ZomboidGlobals.stressFromBiteOrScratch * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay()));
        }
        if (this.getBodyDamage().getNumPartsScratched() > 0) {
            this.stats.add(CharacterStat.STRESS, (float)(ZomboidGlobals.stressFromBiteOrScratch * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay()));
        }
        if (this.getBodyDamage().IsInfected() || this.getBodyDamage().IsFakeInfected()) {
            this.stats.add(CharacterStat.STRESS, (float)(ZomboidGlobals.stressFromBiteOrScratch * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay()));
        }
        if (this.characterTraits.get(CharacterTrait.HEMOPHOBIC)) {
            this.stats.add(CharacterStat.STRESS, (float)((double)this.getTotalBlood() * ZomboidGlobals.stressFromHemophobic * (double)(GameTime.instance.getMultiplier() / 0.8f) * (double)GameTime.instance.getDeltaMinutesPerDay()));
        }
        this.stats.remove(CharacterStat.ANGER, (float)(ZomboidGlobals.angerDecrease * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay()));
    }

    private void updateEndurance() {
        this.stats.setLastEndurance(this.stats.get(CharacterStat.ENDURANCE));
        if (this.isUnlimitedEndurance()) {
            this.stats.reset(CharacterStat.ENDURANCE);
        }
    }

    private void updateThirst() {
        float traitMod = 1.0f;
        if (this.characterTraits.get(CharacterTrait.HIGH_THIRST)) {
            traitMod *= 2.0f;
        }
        if (this.characterTraits.get(CharacterTrait.LOW_THIRST)) {
            traitMod *= 0.5f;
        }
        if (!(!GameServer.server && (GameClient.client || IsoPlayer.getInstance() != this) || this.isoPlayer != null && this.isoPlayer.isGhostMode())) {
            if (this.asleep) {
                this.stats.add(CharacterStat.THIRST, (float)(ZomboidGlobals.thirstSleepingIncrease * SandboxOptions.instance.getStatsDecreaseMultiplier() * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay() * (double)traitMod));
            } else {
                this.stats.add(CharacterStat.THIRST, (float)(ZomboidGlobals.thirstIncrease * SandboxOptions.instance.getStatsDecreaseMultiplier() * (double)GameTime.instance.getMultiplier() * this.getRunningThirstReduction() * (double)GameTime.instance.getDeltaMinutesPerDay() * (double)traitMod * this.getThirstMultiplier()));
            }
        }
        this.autoDrink();
    }

    private double getRunningThirstReduction() {
        if (this == IsoPlayer.getInstance() && IsoPlayer.getInstance().IsRunning()) {
            return 1.2;
        }
        return 1.0;
    }

    public void faceDirection(IsoDirections dir) {
        this.dir = dir;
        this.setForwardDirectionFromIsoDirection();
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        if (animationPlayer != null && animationPlayer.isReady()) {
            animationPlayer.updateForwardDirection(this);
        }
    }

    public void faceLocation(float x, float y) {
        IsoGameCharacter.tempo.x = x + 0.5f;
        IsoGameCharacter.tempo.y = y + 0.5f;
        IsoGameCharacter.tempo.x -= this.getX();
        IsoGameCharacter.tempo.y -= this.getY();
        this.DirectionFromVector(tempo);
        this.setForwardDirectionFromIsoDirection();
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        if (animationPlayer != null && animationPlayer.isReady()) {
            animationPlayer.updateForwardDirection(this);
        }
    }

    public void faceLocationF(float x, float y) {
        IsoGameCharacter.tempo.x = x;
        IsoGameCharacter.tempo.y = y;
        IsoGameCharacter.tempo.x -= this.getX();
        IsoGameCharacter.tempo.y -= this.getY();
        if (tempo.getLengthSquared() == 0.0f) {
            return;
        }
        this.DirectionFromVector(tempo);
        tempo.normalize();
        this.setForwardDirection(IsoGameCharacter.tempo.x, IsoGameCharacter.tempo.y);
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        if (animationPlayer != null && animationPlayer.isReady()) {
            animationPlayer.updateForwardDirection(this);
        }
    }

    public boolean isFacingLocation(float x, float y, float dot) {
        Vector2 v1 = BaseVehicle.allocVector2().set(x - this.getX(), y - this.getY());
        v1.normalize();
        Vector2 v2 = this.getLookVector(BaseVehicle.allocVector2());
        float dot2 = v1.dot(v2);
        BaseVehicle.releaseVector2(v1);
        BaseVehicle.releaseVector2(v2);
        return dot2 >= dot;
    }

    public boolean isFacingObject(IsoObject object, float dot) {
        Vector2 facingPos = BaseVehicle.allocVector2();
        object.getFacingPosition(facingPos);
        boolean facing = this.isFacingLocation(facingPos.x, facingPos.y, dot);
        BaseVehicle.releaseVector2(facingPos);
        return facing;
    }

    public void splatBlood(int dist, float alpha) {
        if (this.getCurrentSquare() == null) {
            return;
        }
        this.getCurrentSquare().splatBlood(dist, alpha);
    }

    @Override
    public boolean isOutside() {
        if (this.getCurrentSquare() == null) {
            return false;
        }
        return this.getCurrentSquare().isOutside();
    }

    @Override
    public boolean isFemale() {
        return this.female;
    }

    @Override
    public void setFemale(boolean isFemale) {
        this.female = isFemale;
    }

    @Override
    public boolean isZombie() {
        return false;
    }

    @Override
    public int getLastHitCount() {
        return this.lastHitCount;
    }

    @Override
    public void setLastHitCount(int hitCount) {
        this.lastHitCount = hitCount;
    }

    public int getSurvivorKills() {
        return this.survivorKills;
    }

    public void setSurvivorKills(int survivorKills) {
        this.survivorKills = survivorKills;
    }

    public int getAge() {
        return this.age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void exert(float f) {
        if (this.characterTraits.get(CharacterTrait.JOGGER)) {
            f *= 0.9f;
        }
        this.stats.remove(CharacterStat.ENDURANCE, f);
    }

    @Override
    public PerkInfo getPerkInfo(PerkFactory.Perk perk) {
        for (int n = 0; n < this.perkList.size(); ++n) {
            PerkInfo info = this.perkList.get(n);
            if (info.perk != perk) continue;
            return info;
        }
        return null;
    }

    @Override
    public boolean isEquipped(InventoryItem item) {
        return this.isEquippedClothing(item) || this.isHandItem(item);
    }

    @Override
    public boolean isEquippedClothing(InventoryItem item) {
        return this.wornItems.contains(item);
    }

    @Override
    public boolean isAttachedItem(InventoryItem item) {
        return this.getAttachedItems().contains(item);
    }

    @Override
    public void faceThisObject(IsoObject object) {
        if (object == null) {
            return;
        }
        Vector2 facingPosition = tempo;
        BaseVehicle objVehicle = Type.tryCastTo(object, BaseVehicle.class);
        BarricadeAble barricadeAble = Type.tryCastTo(object, BarricadeAble.class);
        if (objVehicle != null) {
            objVehicle.getFacingPosition(this, facingPosition);
            facingPosition.x -= this.getX();
            facingPosition.y -= this.getY();
            this.DirectionFromVector(facingPosition);
            facingPosition.normalize();
            this.setForwardDirection(facingPosition.x, facingPosition.y);
        } else if (barricadeAble != null && this.current == barricadeAble.getSquare()) {
            this.dir = barricadeAble.getNorth() ? IsoDirections.N : IsoDirections.W;
            this.setForwardDirectionFromIsoDirection();
        } else if (barricadeAble != null && this.current == barricadeAble.getOppositeSquare()) {
            this.dir = barricadeAble.getNorth() ? IsoDirections.S : IsoDirections.E;
            this.setForwardDirectionFromIsoDirection();
        } else {
            object.getFacingPosition(facingPosition);
            facingPosition.x -= this.getX();
            facingPosition.y -= this.getY();
            facingPosition.normalize();
            this.DirectionFromVector(facingPosition);
            this.setForwardDirection(facingPosition.x, facingPosition.y);
        }
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        if (animationPlayer != null && animationPlayer.isReady()) {
            animationPlayer.updateForwardDirection(this);
        }
    }

    @Override
    public void facePosition(int x, int y) {
        IsoGameCharacter.tempo.x = x;
        IsoGameCharacter.tempo.y = y;
        IsoGameCharacter.tempo.x -= this.getX();
        IsoGameCharacter.tempo.y -= this.getY();
        this.DirectionFromVector(tempo);
        this.setForwardDirectionFromIsoDirection();
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        if (animationPlayer != null && animationPlayer.isReady()) {
            animationPlayer.updateForwardDirection(this);
        }
    }

    @Override
    public void faceThisObjectAlt(IsoObject object) {
        if (object == null) {
            return;
        }
        object.getFacingPositionAlt(tempo);
        IsoGameCharacter.tempo.x -= this.getX();
        IsoGameCharacter.tempo.y -= this.getY();
        this.DirectionFromVector(tempo);
        this.setForwardDirectionFromIsoDirection();
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        if (animationPlayer != null && animationPlayer.isReady()) {
            animationPlayer.updateForwardDirection(this);
        }
    }

    public void setAnimated(boolean b) {
        this.legsSprite.animate = true;
    }

    public long playHurtSound() {
        if (GameServer.server) {
            return 0L;
        }
        return this.getEmitter().playVocals(this.getHurtSound());
    }

    public void playDeadSound() {
        if (GameServer.server) {
            return;
        }
        if (this instanceof IsoAnimal) {
            return;
        }
        if (this.isCloseKilled()) {
            this.getEmitter().playSoundImpl("HeadStab", this);
        } else if (this.isKilledBySlicingWeapon()) {
            this.getEmitter().playSoundImpl("HeadSlice", this);
        } else if (this instanceof IsoZombie) {
            this.getEmitter().playSoundImpl("HeadSmash", this);
        } else {
            IsoGameCharacter isoGameCharacter = this;
            if (isoGameCharacter instanceof IsoPlayer) {
                IsoPlayer player = (IsoPlayer)isoGameCharacter;
                if (!this.isDeathDragDown() && !this.isKilledByFall()) {
                    player.playerVoiceSound("DeathAlone");
                }
            }
        }
        if (this.isZombie()) {
            ((IsoZombie)this).parameterZombieState.setState(ParameterZombieState.State.Death);
        }
    }

    @Override
    public void saveChange(IsoObjectChange change, KahluaTable tbl, ByteBufferWriter bb) {
        super.saveChange(change, tbl, bb);
        if (change == IsoObjectChange.ADD_ITEM) {
            DebugLog.General.warn("The addItem change type in the IsoGameCharacter.saveChange function  was disabled. The server should create item and sent it using the sendAddItemToContainer function.");
        } else if (change == IsoObjectChange.ADD_ITEM_OF_TYPE) {
            DebugLog.General.warn("The addItemOfType change type in the IsoGameCharacter.saveChange function  was disabled. The server should create item and sent it using the sendAddItemToContainer function.");
        } else if (change == IsoObjectChange.ADD_RANDOM_DAMAGE_FROM_ZOMBIE) {
            if (tbl != null && tbl.rawget("zombie") instanceof Double) {
                bb.putShort(((Double)tbl.rawget("zombie")).shortValue());
            }
        } else if (change != IsoObjectChange.ADD_ZOMBIE_KILL) {
            if (change == IsoObjectChange.REMOVE_ITEM) {
                DebugLog.General.warn("The removeItem change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function.");
            } else if (change == IsoObjectChange.REMOVE_ITEM_ID) {
                DebugLog.General.warn("The removeItemID change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function.");
            } else if (change == IsoObjectChange.REMOVE_ITEM_TYPE) {
                DebugLog.General.warn("The removeItemType change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function.");
            } else if (change == IsoObjectChange.REMOVE_ONE_OF) {
                DebugLog.General.warn("The removeOneOf change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function.");
            } else if (change == IsoObjectChange.REANIMATED_ID) {
                if (tbl != null && tbl.rawget("ID") instanceof Double) {
                    int id = ((Double)tbl.rawget("ID")).intValue();
                    bb.putInt(id);
                }
            } else if (change == IsoObjectChange.SHOVE) {
                if (tbl != null && tbl.rawget("hitDirX") instanceof Double && tbl.rawget("hitDirY") instanceof Double && tbl.rawget("force") instanceof Double) {
                    bb.putFloat(((Double)tbl.rawget("hitDirX")).floatValue());
                    bb.putFloat(((Double)tbl.rawget("hitDirY")).floatValue());
                    bb.putFloat(((Double)tbl.rawget("force")).floatValue());
                }
            } else if (change != IsoObjectChange.WAKE_UP && change == IsoObjectChange.MECHANIC_ACTION_DONE && tbl != null) {
                bb.putBoolean((Boolean)tbl.rawget("success"));
            }
        }
    }

    @Override
    public void loadChange(IsoObjectChange change, ByteBufferReader bb) {
        super.loadChange(change, bb);
        if (change == IsoObjectChange.ADD_ITEM) {
            DebugLog.General.warn("The addItem change type in the IsoGameCharacter.saveChange function  was disabled. The server should create item and sent it using the sendAddItemToContainer function.");
        } else if (change == IsoObjectChange.ADD_ITEM_OF_TYPE) {
            DebugLog.General.warn("The addItemOfType change type in the IsoGameCharacter.saveChange function  was disabled. The server should create item and sent it using the sendAddItemToContainer function.");
        } else if (change == IsoObjectChange.ADD_RANDOM_DAMAGE_FROM_ZOMBIE) {
            short id = bb.getShort();
            IsoZombie zombie = GameClient.getZombie(id);
            if (zombie != null && !this.isDead()) {
                this.getBodyDamage().AddRandomDamageFromZombie(zombie, null);
                this.getBodyDamage().Update();
                if (this.isDead()) {
                    if (this.isFemale()) {
                        zombie.getEmitter().playSound("FemaleBeingEatenDeath");
                    } else {
                        zombie.getEmitter().playSound("MaleBeingEatenDeath");
                    }
                }
            }
        } else if (change == IsoObjectChange.ADD_ZOMBIE_KILL) {
            this.setZombieKills(this.getZombieKills() + 1);
        } else if (change == IsoObjectChange.EXIT_VEHICLE) {
            BaseVehicle vehicle = this.getVehicle();
            if (vehicle != null) {
                vehicle.exit(this);
                this.setVehicle(null);
            }
        } else if (change == IsoObjectChange.REMOVE_ITEM) {
            DebugLog.General.warn("The removeItem change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function.");
        } else if (change == IsoObjectChange.REMOVE_ITEM_ID) {
            DebugLog.General.warn("The removeItemID change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function.");
        } else if (change == IsoObjectChange.REMOVE_ITEM_TYPE) {
            DebugLog.General.warn("The removeItemType change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function.");
        } else if (change == IsoObjectChange.REMOVE_ONE_OF) {
            DebugLog.General.warn("The removeOneOf change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function.");
        } else if (change == IsoObjectChange.REANIMATED_ID) {
            this.reanimatedCorpseId = bb.getInt();
        } else if (change != IsoObjectChange.SHOVE) {
            if (change == IsoObjectChange.STOP_BURNING) {
                this.StopBurning();
            } else if (change == IsoObjectChange.WAKE_UP) {
                if (this.isAsleep()) {
                    this.asleep = false;
                    this.forceWakeUpTime = -1.0f;
                    TutorialManager.instance.stealControl = false;
                    if (this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
                        UIManager.setFadeBeforeUI(((IsoPlayer)this).getPlayerNum(), true);
                        UIManager.FadeIn(((IsoPlayer)this).getPlayerNum(), 2.0);
                        GameClient.instance.sendPlayer((IsoPlayer)this);
                    }
                }
            } else if (change == IsoObjectChange.MECHANIC_ACTION_DONE) {
                boolean success = bb.getBoolean();
                LuaEventManager.triggerEvent("OnMechanicActionDone", this, success);
            } else if (change == IsoObjectChange.VEHICLE_NO_KEY) {
                this.SayDebug(" [img=media/ui/CarKey_none.png]");
            }
        }
    }

    @Override
    public int getAlreadyReadPages(String fullType) {
        for (int i = 0; i < this.readBooks.size(); ++i) {
            ReadBook read = this.readBooks.get(i);
            if (!read.fullType.equals(fullType)) continue;
            return read.alreadyReadPages;
        }
        return 0;
    }

    @Override
    public void setAlreadyReadPages(String fullType, int pages) {
        for (int i = 0; i < this.readBooks.size(); ++i) {
            ReadBook read = this.readBooks.get(i);
            if (!read.fullType.equals(fullType)) continue;
            read.alreadyReadPages = pages;
            return;
        }
        ReadBook read = new ReadBook();
        read.fullType = fullType;
        read.alreadyReadPages = pages;
        this.readBooks.add(read);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void updateLightInfo() {
        if (!GameServer.server) {
            return;
        }
        if (this.isZombie()) {
            return;
        }
        LightInfo lightInfo = this.lightInfo;
        synchronized (lightInfo) {
            this.lightInfo.square = this.getMovingSquare();
            if (this.lightInfo.square == null) {
                this.lightInfo.square = this.getCell().getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()));
            }
            if (this.reanimatedCorpse != null) {
                this.lightInfo.square = this.getCell().getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()));
            }
            this.lightInfo.x = this.getX();
            this.lightInfo.y = this.getY();
            this.lightInfo.z = this.getZ();
            this.lightInfo.angleX = this.getForwardDirectionX();
            this.lightInfo.angleY = this.getForwardDirectionY();
            this.lightInfo.torches.clear();
            this.lightInfo.night = GameTime.getInstance().getNight();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public LightInfo initLightInfo2() {
        LightInfo lightInfo = this.lightInfo;
        synchronized (lightInfo) {
            for (int i = 0; i < this.lightInfo2.torches.size(); ++i) {
                TorchInfo.release(this.lightInfo2.torches.get(i));
            }
            this.lightInfo2.initFrom(this.lightInfo);
        }
        return this.lightInfo2;
    }

    public LightInfo getLightInfo2() {
        return this.lightInfo2;
    }

    @Override
    public void postupdate() {
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = this.postUpdateInternal.profile();){
            this.postUpdateInternal();
        }
    }

    @Override
    public float getAnimationTimeDelta() {
        return GameTime.instance.getTimeDelta() * this.animationTimeScale;
    }

    public void updateForServerGui() {
        AnimationPlayer animPlayer = this.getAnimationPlayer();
        float animDeltaT = this.getAnimationTimeDelta();
        this.advancedAnimator.update(animDeltaT);
        if (!this.hasActiveModel()) {
            this.updateAnimPlayer(animPlayer);
        } else {
            this.updateModelSlot();
        }
        this.updateLightInfo();
    }

    private void postUpdateInternal() {
        super.postupdate();
        this.postUpdateAnimating();
        this.clearHitInfo();
        this.clearAttackVars();
        if (this.ballisticsController != null) {
            this.ballisticsController.postUpdate();
        }
    }

    private void postUpdateAnimating() {
        if (!GameServer.server && !this.isAnimationUpdatingThisFrame()) {
            return;
        }
        AnimationPlayer animPlayer = this.getAnimationPlayer();
        animPlayer.updateForwardDirection(this);
        animPlayer.updateVerticalAimAngle(this);
        boolean shouldBeTurning = this.shouldBeTurning();
        this.setTurning(shouldBeTurning);
        boolean shouldBeTurning912 = this.shouldBeTurning90();
        this.setTurning90(shouldBeTurning912);
        boolean shouldBeTurningAround = this.shouldBeTurningAround();
        this.setTurningAround(shouldBeTurningAround);
        this.actionContext.update();
        if (GameClient.client) {
            this.getNetworkCharacterAI().postUpdate();
        }
        if (this.getCurrentSquare() != null) {
            float animDeltaT = this.getAnimationTimeDelta();
            this.advancedAnimator.update(animDeltaT);
        }
        this.actionContext.clearEvent("ActiveAnimFinished");
        this.actionContext.clearEvent("ActiveAnimFinishing");
        this.actionContext.clearEvent("ActiveAnimLooped");
        GameProfiler profiler = GameProfiler.getInstance();
        try (GameProfiler.ProfileArea shouldBeTurning912 = profiler.profile("Deltas");){
            this.applyDeltas(animPlayer);
        }
        if (!this.hasActiveModel()) {
            shouldBeTurning912 = profiler.profile("Anim Player");
            try {
                this.updateAnimPlayer(animPlayer);
            }
            finally {
                if (shouldBeTurning912 != null) {
                    shouldBeTurning912.close();
                }
            }
        }
        shouldBeTurning912 = profiler.profile("Model Slot");
        try {
            this.updateModelSlot();
        }
        finally {
            if (shouldBeTurning912 != null) {
                shouldBeTurning912.close();
            }
        }
        this.updateLightInfo();
        if (this.isAnimationRecorderActive()) {
            for (int i = 0; i < animPlayer.getNumTwistBones(); ++i) {
                AnimatorsBoneTransform twistableBone = animPlayer.getTwistBoneAt(i);
                this.setVariable("twistBone_" + i + "_Name", animPlayer.getTwistBoneNameAt(i));
                this.setVariable("twistBone_" + i + "_Twist", 57.295776f * twistableBone.twist);
                this.setVariable("twistBone_" + i + "_BlendWeight", twistableBone.blendWeight);
            }
            this.getAnimationRecorder().logVariables(this);
        }
        if (this.animationFinishing) {
            this.actionContext.reportEvent("ActiveAnimFinishing");
            this.animationFinishing = false;
        }
    }

    public boolean isAnimationUpdatingThisFrame() {
        return this.animationUpdatingThisFrame;
    }

    private void clearHitInfo() {
        CombatManager.getInstance().hitInfoPool.release((List<HitInfo>)this.hitInfoList);
        this.hitInfoList.clear();
    }

    private void clearAttackVars() {
        this.attackVars.clear();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void updateAnimPlayer(AnimationPlayer animPlayer) {
        animPlayer.updateBones = false;
        boolean interpolateAnims = PerformanceSettings.interpolateAnims;
        PerformanceSettings.interpolateAnims = false;
        try {
            animPlayer.updateForwardDirection(this);
            float animDeltaT = this.getAnimationTimeDelta();
            animPlayer.Update(animDeltaT);
        }
        catch (Throwable t) {
            ExceptionLogger.logException(t);
        }
        finally {
            animPlayer.updateBones = true;
            PerformanceSettings.interpolateAnims = interpolateAnims;
        }
    }

    private void updateModelSlot() {
        try {
            ModelManager.ModelSlot modelSlot = this.legsSprite.modelSlot;
            float animDeltaT = this.getAnimationTimeDelta();
            modelSlot.Update(animDeltaT);
        }
        catch (Throwable t) {
            ExceptionLogger.logException(t);
        }
    }

    private void applyDeltas(AnimationPlayer animPlayer) {
        State currentState;
        MoveDeltaModifiers deltas = L_postUpdate.moveDeltas;
        deltas.moveDelta = this.getMoveDelta();
        deltas.turnDelta = this.getTurnDelta();
        boolean hasPath = this.hasPath();
        boolean isPlayer = this instanceof IsoPlayer;
        if (isPlayer && hasPath && this.isRunning()) {
            deltas.turnDelta = Math.max(deltas.turnDelta, 2.0f);
        }
        if ((currentState = this.getCurrentState()) != null) {
            currentState.getDeltaModifiers(this, deltas);
        }
        if (this.hasPath() && this.getPathFindBehavior2().isTurningToObstacle()) {
            deltas.setMaxTurnDelta(2.0f);
        }
        this.getCurrentTimedActionDeltaModifiers(deltas);
        if (deltas.twistDelta == -1.0f) {
            deltas.twistDelta = deltas.turnDelta * 1.8f;
        }
        if (!this.isTurning() && !GameServer.server) {
            deltas.turnDelta = 0.0f;
        }
        float movementTurnMultiplier = Math.max(1.0f - deltas.moveDelta / 2.0f, 0.0f);
        animPlayer.angleStepDelta = movementTurnMultiplier * deltas.turnDelta;
        animPlayer.angleTwistDelta = movementTurnMultiplier * deltas.twistDelta;
        animPlayer.setMaxTwistAngle((float)Math.PI / 180 * this.getMaxTwist());
    }

    private void getCurrentTimedActionDeltaModifiers(MoveDeltaModifiers deltas) {
        if (this.getCharacterActions().isEmpty()) {
            return;
        }
        BaseAction action = (BaseAction)this.getCharacterActions().get(0);
        if (action == null) {
            return;
        }
        if (action.finished()) {
            return;
        }
        action.getDeltaModifiers(deltas);
    }

    public boolean shouldBeTurning() {
        boolean isTwisting = this.isTwisting();
        if (this.isZombie() && this.getCurrentState() == ZombieFallDownState.instance()) {
            return false;
        }
        if (this.blockTurning) {
            return false;
        }
        if (this.isBehaviourMoving()) {
            return isTwisting;
        }
        if (this.isPlayerMoving()) {
            return isTwisting;
        }
        if (this.isAttacking()) {
            return !this.aimAtFloor;
        }
        float absExcessTwist = this.getAbsoluteExcessTwist();
        if (absExcessTwist > 1.0f) {
            return true;
        }
        if (this.isTurning()) {
            return isTwisting;
        }
        return false;
    }

    public boolean shouldBeTurning90() {
        if (!this.isTurning()) {
            return false;
        }
        if (this.isTurning90()) {
            return true;
        }
        float targetTwist = this.getTargetTwist();
        float targetTwistAbs = Math.abs(targetTwist);
        return targetTwistAbs > 65.0f;
    }

    public boolean shouldBeTurningAround() {
        if (!this.isTurning()) {
            return false;
        }
        float targetTwist = this.getTargetTwist();
        float targetTwistAbs = Math.abs(targetTwist);
        if (this.isTurningAround()) {
            return targetTwistAbs > 45.0f;
        }
        return targetTwistAbs > 110.0f;
    }

    public boolean isTurning() {
        return this.isTurning;
    }

    private void setTurning(boolean isTurning) {
        this.isTurning = isTurning;
    }

    public boolean isTurningAround() {
        return this.isTurningAround;
    }

    private void setTurningAround(boolean isTurningAround) {
        float angleDiff;
        boolean isDifferent = this.isTurningAround != isTurningAround;
        boolean wasTurningAround = this.isTurningAround;
        if (!isDifferent && !wasTurningAround) {
            return;
        }
        this.isTurningAround = isTurningAround;
        float previousTargetAngle = this.initialTurningAroundTarget;
        float currentTargetAngle = this.getDirectionAngle();
        if (isDifferent && isTurningAround) {
            this.invokeGlobalAnimEvent(s_turn180StartedEvent);
            this.initialTurningAroundTarget = currentTargetAngle;
        }
        if (!isDifferent && wasTurningAround && PZMath.abs(angleDiff = PZMath.getClosestAngleDegrees(previousTargetAngle, currentTargetAngle)) > 90.0f) {
            this.invokeGlobalAnimEvent(s_turn180TargetChangedEvent);
            this.isTurningAround = false;
        }
    }

    private void invokeGlobalAnimEvent(AnimEvent globalEvent) {
        AdvancedAnimator advancedAnimator = this.getAdvancedAnimator();
        if (advancedAnimator == null) {
            return;
        }
        advancedAnimator.invokeGlobalAnimEvent(globalEvent);
    }

    public boolean isTurning90() {
        return this.isTurning90;
    }

    private void setTurning90(boolean is) {
        this.isTurning90 = is;
    }

    public boolean hasPath() {
        return this.getPath2() != null;
    }

    @Override
    public float getMeleeDelay() {
        return this.meleeDelay;
    }

    @Override
    public void setMeleeDelay(float delay) {
        this.meleeDelay = Math.max(delay, 0.0f);
    }

    @Override
    public float getRecoilDelay() {
        return this.recoilDelay;
    }

    @Override
    public void setRecoilDelay(float recoilDelay) {
        this.recoilDelay = PZMath.max(0.0f, recoilDelay);
    }

    public float getAimingDelay() {
        return this.aimingDelay;
    }

    public void setAimingDelay(float aimingDelay) {
        this.aimingDelay = aimingDelay;
    }

    public void resetAimingDelay() {
        if (!(this.getPrimaryHandItem() instanceof HandWeapon)) {
            this.aimingDelay = 0.0f;
        } else {
            this.aimingDelay = ((HandWeapon)this.getPrimaryHandItem()).getAimingTime();
            this.aimingDelay *= this.characterTraits.get(CharacterTrait.DEXTROUS) ? 0.8f : (this.characterTraits.get(CharacterTrait.ALL_THUMBS) ? 1.2f : 1.0f);
            this.aimingDelay *= this.getVehicle() != null ? 1.5f : 1.0f;
        }
    }

    public void updateAimingDelay() {
        float mod = 0.0f;
        if (this.getPrimaryHandItem() instanceof HandWeapon) {
            mod = (float)((HandWeapon)this.getPrimaryHandItem()).getRecoilDelay(this) * ((float)this.getPerkLevel(PerkFactory.Perks.Aiming) / 30.0f);
        }
        if (this.isAiming() && this.getRecoilDelay() <= 0.0f + mod && !this.getVariableBoolean("isracking")) {
            this.aimingDelay = PZMath.max(this.aimingDelay - 0.625f * GameTime.getInstance().getMultiplier() * (1.0f + 0.05f * (float)this.getPerkLevel(PerkFactory.Perks.Aiming) + (this.characterTraits.get(CharacterTrait.MARKSMAN) ? 0.1f : 0.0f)), 0.0f);
        } else if (!this.isAiming()) {
            this.resetAimingDelay();
        }
    }

    public float getBeenMovingFor() {
        return this.beenMovingFor;
    }

    public void setBeenMovingFor(float beenMovingFor) {
        this.beenMovingFor = PZMath.clamp(beenMovingFor, 0.0f, 70.0f);
    }

    public String getClickSound() {
        return this.clickSound;
    }

    public void setClickSound(String clickSound) {
        this.clickSound = clickSound;
    }

    public int getMeleeCombatMod() {
        int level = this.getWeaponLevel();
        if (level == 1) {
            return -2;
        }
        if (level == 2) {
            return 0;
        }
        if (level == 3) {
            return 1;
        }
        if (level == 4) {
            return 2;
        }
        if (level == 5) {
            return 3;
        }
        if (level == 6) {
            return 4;
        }
        if (level == 7) {
            return 5;
        }
        if (level == 8) {
            return 5;
        }
        if (level == 9) {
            return 6;
        }
        if (level >= 10) {
            return 7;
        }
        return -5;
    }

    @Override
    public int getWeaponLevel() {
        return this.getWeaponLevel(null);
    }

    @Override
    public int getWeaponLevel(HandWeapon weapon) {
        WeaponType weaponType = WeaponType.getWeaponType(this);
        if (weapon != null) {
            weaponType = WeaponType.getWeaponType(this);
        }
        if (weapon == null) {
            weapon = Type.tryCastTo(this.getPrimaryHandItem(), HandWeapon.class);
        }
        int level = -1;
        if (weaponType != null && weaponType != WeaponType.UNARMED && weapon != null) {
            if (((HandWeapon)this.getPrimaryHandItem()).isOfWeaponCategory(WeaponCategory.AXE)) {
                level = this.getPerkLevel(PerkFactory.Perks.Axe);
            }
            if (((HandWeapon)this.getPrimaryHandItem()).isOfWeaponCategory(WeaponCategory.SPEAR)) {
                level += this.getPerkLevel(PerkFactory.Perks.Spear);
            }
            if (((HandWeapon)this.getPrimaryHandItem()).isOfWeaponCategory(WeaponCategory.SMALL_BLADE)) {
                level += this.getPerkLevel(PerkFactory.Perks.SmallBlade);
            }
            if (((HandWeapon)this.getPrimaryHandItem()).isOfWeaponCategory(WeaponCategory.LONG_BLADE)) {
                level += this.getPerkLevel(PerkFactory.Perks.LongBlade);
            }
            if (((HandWeapon)this.getPrimaryHandItem()).isOfWeaponCategory(WeaponCategory.BLUNT)) {
                level += this.getPerkLevel(PerkFactory.Perks.Blunt);
            }
            if (((HandWeapon)this.getPrimaryHandItem()).isOfWeaponCategory(WeaponCategory.SMALL_BLUNT)) {
                level += this.getPerkLevel(PerkFactory.Perks.SmallBlunt);
            }
        }
        if (level > 10) {
            level = 10;
        }
        if (level == -1) {
            return 0;
        }
        return level;
    }

    @Override
    public int getMaintenanceMod() {
        int level = this.getPerkLevel(PerkFactory.Perks.Maintenance);
        return level += this.getWeaponLevel() / 2;
    }

    @Override
    public BaseVehicle getVehicle() {
        return this.vehicle;
    }

    @Override
    public void setVehicle(BaseVehicle v) {
        this.vehicle = v;
    }

    public boolean isUnderVehicle() {
        return this.isUnderVehicleRadius(0.3f);
    }

    public boolean isUnderVehicleRadius(float radius) {
        int chunkMinX = (PZMath.fastfloor(this.getX()) - 4) / 8;
        int chunkMinY = (PZMath.fastfloor(this.getY()) - 4) / 8;
        int chunkMaxX = (int)Math.ceil((this.getX() + 4.0f) / 8.0f);
        int chunkMaxY = (int)Math.ceil((this.getY() + 4.0f) / 8.0f);
        Vector2 vector2 = (Vector2)Vector2ObjectPool.get().alloc();
        for (int y = chunkMinY; y < chunkMaxY; ++y) {
            for (int x = chunkMinX; x < chunkMaxX; ++x) {
                IsoChunk chunk;
                IsoChunk isoChunk = chunk = GameServer.server ? ServerMap.instance.getChunk(x, y) : IsoWorld.instance.currentCell.getChunkForGridSquare(x * 8, y * 8, 0);
                if (chunk == null) continue;
                for (int i = 0; i < chunk.vehicles.size(); ++i) {
                    BaseVehicle vehicle = chunk.vehicles.get(i);
                    Vector2 v = vehicle.testCollisionWithCharacter(this, radius, vector2);
                    if (v == null || v.x == -1.0f) continue;
                    Vector2ObjectPool.get().release(vector2);
                    return true;
                }
            }
        }
        Vector2ObjectPool.get().release(vector2);
        return false;
    }

    public boolean isBeingSteppedOn() {
        if (!this.isOnFloor()) {
            return false;
        }
        for (int dy = -1; dy <= 1; ++dy) {
            for (int dx = -1; dx <= 1; ++dx) {
                IsoGridSquare square = this.getCell().getGridSquare(PZMath.fastfloor(this.getX()) + dx, PZMath.fastfloor(this.getY()) + dy, PZMath.fastfloor(this.getZ()));
                if (square == null) continue;
                ArrayList<IsoMovingObject> objects = square.getMovingObjects();
                for (int i = 0; i < objects.size(); ++i) {
                    IsoGameCharacter chr;
                    IsoMovingObject obj = objects.get(i);
                    if (obj == this || !(obj instanceof IsoGameCharacter) || (chr = (IsoGameCharacter)obj).getVehicle() != null || obj.isOnFloor() || !ZombieOnGroundState.isCharacterStandingOnOther(chr, this)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public float getReduceInfectionPower() {
        return this.reduceInfectionPower;
    }

    @Override
    public void setReduceInfectionPower(float reduceInfectionPower) {
        this.reduceInfectionPower = reduceInfectionPower;
    }

    @Override
    public float getInventoryWeight() {
        if (this.getInventory() == null) {
            return 0.0f;
        }
        float total = 0.0f;
        ArrayList<InventoryItem> items = this.getInventory().getItems();
        for (int i = 0; i < items.size(); ++i) {
            InventoryItem item = items.get(i);
            if (item.getAttachedSlot() > -1 && !this.isEquipped(item)) {
                total += item.getHotbarEquippedWeight();
                continue;
            }
            if (this.isEquipped(item) || item.isFakeEquipped(this)) {
                total += item.getEquippedWeight();
                continue;
            }
            total += item.getUnequippedWeight();
        }
        return total;
    }

    public void dropHandItems() {
        IsoPlayer player;
        if ("Tutorial".equals(Core.gameMode)) {
            return;
        }
        IsoGameCharacter isoGameCharacter = this;
        if (isoGameCharacter instanceof IsoPlayer && !(player = (IsoPlayer)isoGameCharacter).isLocalPlayer()) {
            return;
        }
        this.dropHeavyItems();
        IsoGridSquare sq = this.getCurrentSquare();
        if (sq == null) {
            return;
        }
        InventoryItem item1 = this.getPrimaryHandItem();
        InventoryItem item2 = this.getSecondaryHandItem();
        if (item1 == null && item2 == null) {
            return;
        }
        sq = this.getSolidFloorAt(sq.x, sq.y, sq.z);
        if (sq == null) {
            return;
        }
        if (item1 != null) {
            this.setPrimaryHandItem(null);
            if (!GameClient.client) {
                this.getInventory().DoRemoveItem(item1);
                float dropX = Rand.Next(0.1f, 0.9f);
                float dropY = Rand.Next(0.1f, 0.9f);
                float dropZ = sq.getApparentZ(dropX, dropY) - (float)sq.getZ();
                sq.AddWorldInventoryItem(item1, dropX, dropY, dropZ);
                LuaEventManager.triggerEvent("OnContainerUpdate");
            }
            LuaEventManager.triggerEvent("onItemFall", item1);
            this.playDropItemSound(item1);
        }
        if (item2 != null) {
            boolean inBothHand;
            this.setSecondaryHandItem(null);
            boolean bl = inBothHand = item2 == item1;
            if (!inBothHand) {
                if (!GameClient.client) {
                    this.getInventory().DoRemoveItem(item2);
                    float dropX = Rand.Next(0.1f, 0.9f);
                    float dropY = Rand.Next(0.1f, 0.9f);
                    float dropZ = sq.getApparentZ(dropX, dropY) - (float)sq.getZ();
                    sq.AddWorldInventoryItem(item2, dropX, dropY, dropZ);
                    LuaEventManager.triggerEvent("OnContainerUpdate");
                }
                LuaEventManager.triggerEvent("onItemFall", item2);
                this.playDropItemSound(item2);
            }
        }
        if (GameClient.client && this.isLocal()) {
            INetworkPacket.send(PacketTypes.PacketType.PlayerDropHeldItems, this, sq.x, sq.y, sq.z, false);
            INetworkPacket.send(PacketTypes.PacketType.Equip, this);
        }
    }

    public void dropHeldItems(int x, int y, int z, boolean heavy, boolean isThrow) {
        boolean drop1;
        if (!GameServer.server) {
            return;
        }
        InventoryItem item1 = this.getPrimaryHandItem();
        InventoryItem item2 = this.getSecondaryHandItem();
        if (item1 == null && item2 == null) {
            return;
        }
        IsoGridSquare sq = this.getSolidFloorAt(x, y, z);
        if (sq == null) {
            return;
        }
        boolean bl = heavy ? this.isHeavyItem(item1) : (drop1 = item1 != null);
        if (drop1) {
            this.setPrimaryHandItem(null);
            this.getInventory().DoRemoveItem(item1);
            GameServer.sendRemoveItemFromContainer(this.getInventory(), item1);
            if (!isThrow) {
                float dropX = Rand.Next(0.1f, 0.9f);
                float dropY = Rand.Next(0.1f, 0.9f);
                float dropZ = sq.getApparentZ(dropX, dropY) - (float)sq.getZ();
                sq.AddWorldInventoryItem(item1, dropX, dropY, dropZ);
            }
        }
        if (!isThrow) {
            boolean drop2;
            boolean bl2 = heavy ? this.isHeavyItem(item2) : (drop2 = item2 != null);
            if (drop2) {
                boolean inBothHand;
                this.setSecondaryHandItem(null);
                boolean bl3 = inBothHand = item2 == item1;
                if (!inBothHand) {
                    this.getInventory().DoRemoveItem(item2);
                    GameServer.sendRemoveItemFromContainer(this.getInventory(), item2);
                    float dropX = Rand.Next(0.1f, 0.9f);
                    float dropY = Rand.Next(0.1f, 0.9f);
                    float dropZ = sq.getApparentZ(dropX, dropY) - (float)sq.getZ();
                    sq.AddWorldInventoryItem(item2, dropX, dropY, dropZ);
                }
            }
        }
    }

    public boolean shouldBecomeZombieAfterDeath() {
        return switch (SandboxOptions.instance.lore.transmission.getValue()) {
            case 1 -> {
                if (!this.getBodyDamage().IsFakeInfected() && this.stats.get(CharacterStat.ZOMBIE_INFECTION) >= 0.001f) {
                    yield true;
                }
                yield false;
            }
            case 2 -> {
                if (!this.getBodyDamage().IsFakeInfected() && this.stats.get(CharacterStat.ZOMBIE_INFECTION) >= 0.001f) {
                    yield true;
                }
                yield false;
            }
            case 3 -> true;
            case 4 -> false;
            default -> false;
        };
    }

    @Override
    public void modifyTraitXPBoost(CharacterTrait characterTrait, boolean isRemovingTrait) {
        this.modifyTraitXPBoost(CharacterTraitDefinition.getCharacterTraitDefinition(characterTrait), isRemovingTrait);
    }

    @Override
    public void modifyTraitXPBoost(CharacterTraitDefinition trait, boolean isRemovingTrait) {
        if (trait == null) {
            return;
        }
        HashMap<PerkFactory.Perk, Integer> playerXPBoostMap = this.getDescriptor().getXPBoostMap();
        HashMap<PerkFactory.Perk, Integer> xpBoosts = trait.getXpBoosts();
        if (xpBoosts == null) {
            return;
        }
        for (Map.Entry entry : xpBoosts.entrySet()) {
            PerkFactory.Perk perkType = (PerkFactory.Perk)entry.getKey();
            int currentValue = 0;
            if (playerXPBoostMap.containsKey(perkType)) {
                currentValue = playerXPBoostMap.get(perkType);
            }
            playerXPBoostMap.put(perkType, currentValue + (isRemovingTrait ? -((Integer)entry.getValue()).intValue() : (Integer)entry.getValue()));
        }
    }

    public void applyTraits(List<CharacterTrait> luaTraits) {
        PerkFactory.Perk perkType;
        if (luaTraits == null) {
            return;
        }
        HashMap<PerkFactory.Perk, Integer> levels = new HashMap<PerkFactory.Perk, Integer>();
        levels.put(PerkFactory.Perks.Fitness, 5);
        levels.put(PerkFactory.Perks.Strength, 5);
        for (CharacterTrait characterTrait : luaTraits) {
            HashMap<PerkFactory.Perk, Integer> xpBoostMap;
            if (characterTrait == null) continue;
            this.characterTraits.set(characterTrait, true);
            CharacterTraitDefinition characterTraitDefinition = CharacterTraitDefinition.getCharacterTraitDefinition(characterTrait);
            if (characterTraitDefinition == null || (xpBoostMap = characterTraitDefinition.getXpBoosts()) == null) continue;
            for (Map.Entry entry : xpBoostMap.entrySet()) {
                PerkFactory.Perk perkType2 = (PerkFactory.Perk)entry.getKey();
                int level = (Integer)entry.getValue();
                if (levels.containsKey(perkType2)) {
                    level += ((Integer)levels.get(perkType2)).intValue();
                }
                levels.put(perkType2, level);
            }
        }
        if (this instanceof IsoPlayer) {
            ((IsoPlayer)this).getNutrition().applyWeightFromTraits();
        }
        HashMap<PerkFactory.Perk, Integer> xpBoostMap = this.getDescriptor().getXPBoostMap();
        for (Map.Entry<PerkFactory.Perk, Integer> entry : xpBoostMap.entrySet()) {
            perkType = entry.getKey();
            int level = entry.getValue();
            if (levels.containsKey(perkType)) {
                level += ((Integer)levels.get(perkType)).intValue();
            }
            levels.put(perkType, level);
        }
        for (Map.Entry<PerkFactory.Perk, Integer> entry : levels.entrySet()) {
            perkType = entry.getKey();
            int level = entry.getValue();
            level = Math.max(0, level);
            level = Math.min(10, level);
            this.getDescriptor().getXPBoostMap().put(perkType, Math.min(3, level));
            for (int i = 0; i < level; ++i) {
                this.LevelPerk(perkType);
            }
            this.getXp().setXPToLevel(perkType, this.getPerkLevel(perkType));
        }
    }

    public void applyProfessionRecipes() {
        CharacterProfessionDefinition characterProfessionDefinition = CharacterProfessionDefinition.getCharacterProfessionDefinition(this.getDescriptor().getCharacterProfession());
        this.knownRecipes.addAll(characterProfessionDefinition.getGrantedRecipes());
    }

    public void applyCharacterTraitsRecipes() {
        Map<CharacterTrait, Boolean> traits = this.characterTraits.getTraits();
        for (Map.Entry<CharacterTrait, Boolean> entry : traits.entrySet()) {
            CharacterTraitDefinition trait;
            if (!entry.getValue().booleanValue() || (trait = CharacterTraitDefinition.getCharacterTraitDefinition(entry.getKey())) == null) continue;
            this.knownRecipes.addAll(trait.getGrantedRecipes());
        }
    }

    public InventoryItem createKeyRing() {
        return this.createKeyRing(ItemKey.Container.KEY_RING);
    }

    public InventoryItem createKeyRing(ItemKey itemKey) {
        RoomDef roomDef;
        Object keyringItem = this.getInventory().addItem(itemKey);
        InventoryContainer keyring = (InventoryContainer)keyringItem;
        keyring.setName(Translator.getText("IGUI_KeyRingName", this.getDescriptor().getForename(), this.getDescriptor().getSurname()));
        if (Rand.Next(100) < 40 && (roomDef = IsoWorld.instance.metaGrid.getRoomAt(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()))) != null && roomDef.getBuilding() != null) {
            String keyType = "Base.Key1";
            InventoryItem key = keyring.getInventory().AddItem("Base.Key1");
            key.setKeyId(roomDef.getBuilding().getKeyId());
        }
        return keyringItem;
    }

    public void autoDrink() {
        IsoPlayer player;
        IsoGameCharacter isoGameCharacter;
        if (GameClient.client) {
            return;
        }
        if (GameServer.server && (isoGameCharacter = this) instanceof IsoPlayer && !(player = (IsoPlayer)isoGameCharacter).getAutoDrink()) {
            return;
        }
        if (!Core.getInstance().getOptionAutoDrink()) {
            return;
        }
        if (this.isAsleep() || this.isPerformingGrappleAnimation() || this.isKnockedDown() || this.isbFalling() || this.isAiming() || this.isClimbing()) {
            return;
        }
        if (LuaHookManager.TriggerHook("AutoDrink", this)) {
            return;
        }
        if (this.stats.get(CharacterStat.THIRST) <= 0.1f) {
            return;
        }
        InventoryItem drinkFrom = this.getWaterSource(this.getInventory().getItems());
        if (drinkFrom != null && drinkFrom.hasComponent(ComponentType.FluidContainer)) {
            IsoGameCharacter isoGameCharacter2;
            float amountNeeded = this.stats.get(CharacterStat.THIRST) * 2.0f;
            float amount = Math.min(drinkFrom.getFluidContainer().getAmount(), amountNeeded);
            float percentage = amount / drinkFrom.getFluidContainer().getAmount();
            this.DrinkFluid(drinkFrom, percentage, false);
            if (GameServer.server && (isoGameCharacter2 = this) instanceof IsoPlayer) {
                IsoPlayer player2 = (IsoPlayer)isoGameCharacter2;
                INetworkPacket.send(player2, PacketTypes.PacketType.SyncItemFields, player2, drinkFrom);
            }
        }
    }

    public InventoryItem getWaterSource(ArrayList<InventoryItem> items) {
        InventoryItem drinkFrom = null;
        for (int n = 0; n < items.size(); ++n) {
            InventoryItem item = items.get(n);
            boolean validItem = false;
            boolean drink = true;
            if (item.isWaterSource()) {
                validItem = true;
                boolean bl = drink = !item.getFluidContainer().isCategory(FluidCategory.Hazardous) || !SandboxOptions.instance.enableTaintedWaterText.getValue();
            }
            if (!validItem || !drink) continue;
            if (item.hasComponent(ComponentType.FluidContainer) && (double)item.getFluidContainer().getAmount() >= 0.12) {
                drinkFrom = item;
                break;
            }
            if (item instanceof InventoryContainer) continue;
            drinkFrom = item;
            break;
        }
        return drinkFrom;
    }

    @Override
    public List<String> getKnownRecipes() {
        return this.knownRecipes;
    }

    @Override
    public boolean isRecipeKnown(Recipe recipe) {
        if (this.isKnowAllRecipes() || SandboxOptions.instance.seeNotLearntRecipe.getValue()) {
            return true;
        }
        return this.getKnownRecipes().contains(recipe.getOriginalname());
    }

    public boolean isRecipeKnown(CraftRecipe recipe) {
        return this.isRecipeKnown(recipe, false);
    }

    public boolean isRecipeKnown(CraftRecipe recipe, boolean ignoreSandbox) {
        if (!ignoreSandbox && SandboxOptions.instance.seeNotLearntRecipe.getValue() || this.isKnowAllRecipes()) {
            return true;
        }
        return !recipe.needToBeLearn() || this.getKnownRecipes().contains(recipe.getName()) || this.getKnownRecipes().contains(recipe.getMetaRecipe()) || this.getKnownRecipes().contains(recipe.getTranslationName());
    }

    @Override
    public boolean isRecipeKnown(String name) {
        return this.isRecipeKnown(name, false);
    }

    public boolean isRecipeKnown(String name, boolean ignoreSandbox) {
        Recipe recipe = ScriptManager.instance.getRecipe(name);
        if (recipe == null) {
            if (!ignoreSandbox && SandboxOptions.instance.seeNotLearntRecipe.getValue() || this.isKnowAllRecipes()) {
                return true;
            }
            return this.getKnownRecipes().contains(name);
        }
        return this.isRecipeKnown(recipe);
    }

    public boolean isRecipeActuallyKnown(CraftRecipe recipe) {
        return this.isRecipeKnown(recipe, true);
    }

    public boolean isRecipeActuallyKnown(String name) {
        return this.isRecipeKnown(name, true);
    }

    public boolean learnRecipe(String name) {
        return this.learnRecipe(name, true);
    }

    public boolean learnRecipe(String name, boolean checkMetaRecipe) {
        if (!this.isRecipeKnown(name, true)) {
            this.getKnownRecipes().add(name);
            if (checkMetaRecipe) {
                ScriptManager.instance.checkMetaRecipe(this, name);
            }
            return true;
        }
        return false;
    }

    @Override
    public void addKnownMediaLine(String guid) {
        if (StringUtils.isNullOrWhitespace(guid)) {
            return;
        }
        this.knownMediaLines.add(guid.trim());
    }

    @Override
    public void removeKnownMediaLine(String guid) {
        if (StringUtils.isNullOrWhitespace(guid)) {
            return;
        }
        this.knownMediaLines.remove(guid.trim());
    }

    @Override
    public void clearKnownMediaLines() {
        this.knownMediaLines.clear();
    }

    @Override
    public boolean isKnownMediaLine(String guid) {
        if (StringUtils.isNullOrWhitespace(guid)) {
            return false;
        }
        return this.knownMediaLines.contains(guid.trim());
    }

    protected void saveKnownMediaLines(ByteBuffer bb) {
        bb.putShort((short)this.knownMediaLines.size());
        for (String guid : this.knownMediaLines) {
            GameWindow.WriteString(bb, guid);
        }
    }

    protected void loadKnownMediaLines(ByteBuffer bb, int worldVersion) {
        this.knownMediaLines.clear();
        int count = bb.getShort();
        for (int i = 0; i < count; ++i) {
            String guid = GameWindow.ReadString(bb);
            this.knownMediaLines.add(guid);
        }
    }

    @Override
    public boolean isMoving() {
        if (this instanceof IsoPlayer && !((IsoPlayer)this).isAttackAnimThrowTimeOut()) {
            return false;
        }
        return this.isMoving;
    }

    public boolean isBehaviourMoving() {
        State currentState = this.getCurrentState();
        return currentState != null && currentState.isMoving(this);
    }

    public boolean isPlayerMoving() {
        return false;
    }

    public void setMoving(boolean val) {
        this.isMoving = val;
        if (GameClient.client && this instanceof IsoPlayer && ((IsoPlayer)this).remote) {
            ((IsoPlayer)this).isPlayerMoving = val;
            ((IsoPlayer)this).setJustMoved(val);
        }
    }

    private boolean isFacingNorthWesterly() {
        return this.dir == IsoDirections.W || this.dir == IsoDirections.NW || this.dir == IsoDirections.N || this.dir == IsoDirections.NE;
    }

    public boolean isAttacking() {
        return false;
    }

    public boolean isZombieAttacking() {
        return false;
    }

    public boolean isZombieAttacking(IsoMovingObject other) {
        return false;
    }

    private boolean isZombieThumping() {
        if (this.isZombie()) {
            return this.getCurrentState() == ThumpState.instance();
        }
        return false;
    }

    public int compareMovePriority(IsoGameCharacter other) {
        if (other == null) {
            return 1;
        }
        if (this.isZombieThumping() && !other.isZombieThumping()) {
            return 1;
        }
        if (!this.isZombieThumping() && other.isZombieThumping()) {
            return -1;
        }
        if (other instanceof IsoPlayer) {
            if (GameClient.client && this.isZombieAttacking(other)) {
                return -1;
            }
            return 0;
        }
        if (this.isZombieAttacking() && !other.isZombieAttacking()) {
            return 1;
        }
        if (!this.isZombieAttacking() && other.isZombieAttacking()) {
            return -1;
        }
        if (this.isBehaviourMoving() && !other.isBehaviourMoving()) {
            return 1;
        }
        if (!this.isBehaviourMoving() && other.isBehaviourMoving()) {
            return -1;
        }
        if (this.isFacingNorthWesterly() && !other.isFacingNorthWesterly()) {
            return 1;
        }
        if (!this.isFacingNorthWesterly() && other.isFacingNorthWesterly()) {
            return -1;
        }
        return 0;
    }

    @Override
    public long playSound(String file) {
        return this.getEmitter().playSound(file);
    }

    @Override
    public long playSoundLocal(String file) {
        return this.getEmitter().playSoundImpl(file, null);
    }

    @Override
    public void stopOrTriggerSound(long eventInstance) {
        this.getEmitter().stopOrTriggerSound(eventInstance);
    }

    public long playDropItemSound(InventoryItem item) {
        if (item == null) {
            return 0L;
        }
        String sound = item.getDropSound();
        if (sound == null && item instanceof InventoryContainer) {
            sound = "DropBag";
        }
        if (sound == null) {
            return 0L;
        }
        return this.playSound(sound);
    }

    public long playWeaponHitArmourSound(int partIndex, boolean bullet) {
        this.getItemVisuals(tempItemVisuals);
        for (int i = tempItemVisuals.size() - 1; i >= 0; --i) {
            ArrayList<BloodBodyPartType> coveredParts;
            ArrayList<BloodClothingType> types;
            String sound;
            ItemVisual itemVisual = (ItemVisual)tempItemVisuals.get(i);
            Item scriptItem = itemVisual.getScriptItem();
            if (scriptItem == null) continue;
            String string = sound = bullet ? scriptItem.getBulletHitArmourSound() : scriptItem.getWeaponHitArmourSound();
            if (sound == null || (types = scriptItem.getBloodClothingType()) == null || (coveredParts = BloodClothingType.getCoveredParts(types)) == null) continue;
            for (int j = 0; j < coveredParts.size(); ++j) {
                if (coveredParts.get(j).index() != partIndex) continue;
                if (GameServer.server) {
                    INetworkPacket.sendToRelative(PacketTypes.PacketType.PlaySound, this.getXi(), (float)this.getYi(), sound, (byte)0, this);
                    return 0L;
                }
                return this.playSoundLocal(sound);
            }
        }
        return 0L;
    }

    @Override
    public void addWorldSoundUnlessInvisible(int radius, int volume, boolean bStressHumans) {
        if (this.isInvisible()) {
            return;
        }
        WorldSoundManager.instance.addSound(this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), radius, volume, bStressHumans);
    }

    @Override
    public boolean isKnownPoison(InventoryItem item) {
        if (item.hasTag(ItemTag.NO_DETECT)) {
            return false;
        }
        if (item.hasTag(ItemTag.SHOW_POISON)) {
            return true;
        }
        if (item instanceof Food) {
            Food food = (Food)item;
            if (food.getPoisonPower() <= 0) {
                return false;
            }
            if (food.getHerbalistType() != null && !food.getHerbalistType().isEmpty()) {
                return this.isRecipeActuallyKnown("Herbalist");
            }
            if (food.getPoisonDetectionLevel() >= 0 && this.getPerkLevel(PerkFactory.Perks.Cooking) >= 10 - food.getPoisonDetectionLevel()) {
                return true;
            }
            return this.getFullName().equals(food.getModData().rawget("addedPoisonBy"));
        }
        return false;
    }

    @Override
    public boolean isKnownPoison(Item item) {
        if (item.hasTag(ItemTag.SHOW_POISON)) {
            return true;
        }
        if (item.isItemType(ItemType.FOOD)) {
            if (item.getPoisonPower() <= 0.0f) {
                return false;
            }
            if (item.getHerbalistType() != null && !item.getHerbalistType().isEmpty()) {
                return this.isRecipeActuallyKnown("Herbalist");
            }
            if (item.getPoisonDetectionLevel() >= 0 && this.getPerkLevel(PerkFactory.Perks.Cooking) >= 10 - item.getPoisonDetectionLevel()) {
                return true;
            }
            return item.getPoisonDetectionLevel() >= 0;
        }
        return false;
    }

    @Override
    public int getLastHourSleeped() {
        return this.lastHourSleeped;
    }

    @Override
    public void setLastHourSleeped(int lastHourSleeped) {
        this.lastHourSleeped = lastHourSleeped;
    }

    @Override
    public void setTimeOfSleep(float timeOfSleep) {
        this.timeOfSleep = timeOfSleep;
    }

    public void setDelayToSleep(float delay) {
        this.delayToActuallySleep = delay;
    }

    @Override
    public String getBedType() {
        return this.bedType;
    }

    @Override
    public void setBedType(String bedType) {
        this.bedType = bedType;
    }

    public void enterVehicle(BaseVehicle v, int seat, Vector3f offset) {
        if (this.vehicle != null) {
            this.vehicle.exit(this);
        }
        if (v != null) {
            v.enter(seat, this, offset);
        }
    }

    @Override
    public float Hit(BaseVehicle vehicle, float speed, boolean isHitFromBehind, float hitDirX, float hitDirY, boolean pushedBack, float collisionPosOnVehicleX, float collisionPosOnVehicleY) {
        Vector2 impactPosOnVehicle = (Vector2)Vector2ObjectPool.get().alloc();
        impactPosOnVehicle.set(collisionPosOnVehicleX, collisionPosOnVehicleY);
        boolean isCollisionPosValid = collisionPosOnVehicleX != 0.0f && collisionPosOnVehicleY != 0.0f;
        float damage = this.onHitByVehicle(vehicle, speed, this.getHitDir().set(hitDirX, hitDirY), impactPosOnVehicle, pushedBack && isCollisionPosValid);
        Vector2ObjectPool.get().release(impactPosOnVehicle);
        return damage;
    }

    @Override
    public Path getPath2() {
        return this.path2;
    }

    @Override
    public void setPath2(Path path) {
        this.path2 = path;
    }

    @Override
    public PathFindBehavior2 getPathFindBehavior2() {
        return this.pfb2;
    }

    public MapKnowledge getMapKnowledge() {
        return this.mapKnowledge;
    }

    @Override
    public IsoObject getBed() {
        return this.bed;
    }

    @Override
    public void setBed(IsoObject bed) {
        this.bed = bed;
    }

    public boolean avoidDamage() {
        return this.avoidDamage;
    }

    public void setAvoidDamage(boolean avoid) {
        this.avoidDamage = avoid;
    }

    @Override
    public boolean isReading() {
        return this.isReading;
    }

    @Override
    public void setReading(boolean isReading) {
        this.isReading = isReading;
    }

    @Override
    public float getTimeSinceLastSmoke() {
        return this.timeSinceLastSmoke;
    }

    @Override
    public void setTimeSinceLastSmoke(float timeSinceLastSmoke) {
        this.timeSinceLastSmoke = PZMath.clamp(timeSinceLastSmoke, 0.0f, 10.0f);
    }

    @Override
    public boolean isInvisible() {
        return this.getCheats().isSet(CheatType.INVISIBLE);
    }

    @Override
    public void setInvisible(boolean b) {
        if (!Role.hasCapability(this, Capability.ToggleInvisibleHimself)) {
            this.getCheats().set(CheatType.INVISIBLE, false);
            return;
        }
        this.getCheats().set(CheatType.INVISIBLE, b);
    }

    public void setInvisible(boolean b, boolean isForced) {
        if (!isForced) {
            this.setInvisible(b);
            return;
        }
        this.getCheats().set(CheatType.INVISIBLE, b);
    }

    public boolean isCanUseBrushTool() {
        return this.getCheats().isSet(CheatType.BRUSH_TOOL);
    }

    public void setCanUseBrushTool(boolean b) {
        if (!Role.hasCapability(this, Capability.UseBrushToolManager)) {
            this.getCheats().set(CheatType.BRUSH_TOOL, false);
            return;
        }
        this.getCheats().set(CheatType.BRUSH_TOOL, b);
    }

    public boolean canUseLootTool() {
        return this.getCheats().isSet(CheatType.LOOT_TOOL);
    }

    public void setCanUseLootTool(boolean b) {
        if (!Role.hasCapability(this, Capability.UseLootTool)) {
            this.getCheats().set(CheatType.LOOT_TOOL, false);
            return;
        }
        this.getCheats().set(CheatType.LOOT_TOOL, b);
    }

    public boolean canUseDebugContextMenu() {
        return this.getCheats().isSet(CheatType.DEBUG_CONTEXT_MENU);
    }

    public void setCanUseDebugContextMenu(boolean b) {
        if (!Role.hasCapability(this, Capability.UseDebugContextMenu)) {
            this.getCheats().set(CheatType.DEBUG_CONTEXT_MENU, false);
            return;
        }
        this.getCheats().set(CheatType.DEBUG_CONTEXT_MENU, b);
    }

    @Override
    public boolean isDriving() {
        return this.getVehicle() != null && this.getVehicle().getDriver() == this && this.getVehicle().getController() != null && !this.getVehicle().isStopped();
    }

    @Override
    public boolean isInARoom() {
        return this.square != null && this.square.isInARoom();
    }

    @Override
    public boolean isGodMod() {
        return this.getCheats().isSet(CheatType.GOD_MODE);
    }

    public boolean isInvulnerable() {
        return this.getCheats().isSet(CheatType.GOD_MODE);
    }

    public void setInvulnerable(boolean invulnerable) {
        this.getCheats().set(CheatType.GOD_MODE, invulnerable);
    }

    public void setZombiesDontAttack(boolean b) {
        if (!Role.hasCapability(this, Capability.UseZombieDontAttackCheat)) {
            this.getCheats().set(CheatType.ZOMBIES_DONT_ATTACK, false);
            return;
        }
        this.getCheats().set(CheatType.ZOMBIES_DONT_ATTACK, b);
    }

    public boolean isZombiesDontAttack() {
        return this.getCheats().isSet(CheatType.ZOMBIES_DONT_ATTACK);
    }

    public void setGodMod(boolean b, boolean isForced) {
        if (!isForced) {
            this.setGodMod(b);
            return;
        }
        if (!this.isDead()) {
            this.getCheats().set(CheatType.GOD_MODE, b);
        }
    }

    @Override
    public void setGodMod(boolean b) {
        if (!Role.hasCapability(this, Capability.ToggleGodModHimself)) {
            this.getCheats().set(CheatType.GOD_MODE, false);
            return;
        }
        if (!this.isDead()) {
            this.getCheats().set(CheatType.GOD_MODE, b);
        }
    }

    @Override
    public boolean isUnlimitedCarry() {
        return this.getCheats().isSet(CheatType.UNLIMITED_CARRY);
    }

    @Override
    public void setUnlimitedCarry(boolean unlimitedCarry) {
        if (!Role.hasCapability(this, Capability.ToggleUnlimitedCarry)) {
            this.getCheats().set(CheatType.UNLIMITED_CARRY, false);
            return;
        }
        this.getCheats().set(CheatType.UNLIMITED_CARRY, unlimitedCarry);
    }

    @Override
    public boolean isBuildCheat() {
        return this.getCheats().isSet(CheatType.BUILD);
    }

    @Override
    public void setBuildCheat(boolean buildCheat) {
        if (!Role.hasCapability(this, Capability.UseBuildCheat)) {
            this.getCheats().set(CheatType.BUILD, false);
            return;
        }
        this.getCheats().set(CheatType.BUILD, buildCheat);
    }

    @Override
    public boolean isFarmingCheat() {
        return this.getCheats().isSet(CheatType.FARMING);
    }

    @Override
    public void setFarmingCheat(boolean b) {
        if (!Role.hasCapability(this, Capability.UseFarmingCheat)) {
            this.getCheats().set(CheatType.FARMING, false);
            return;
        }
        this.getCheats().set(CheatType.FARMING, b);
    }

    @Override
    public boolean isFishingCheat() {
        return this.getCheats().isSet(CheatType.FISHING);
    }

    @Override
    public void setFishingCheat(boolean b) {
        if (!Role.hasCapability(this, Capability.UseFishingCheat)) {
            this.getCheats().set(CheatType.FISHING, false);
            return;
        }
        this.getCheats().set(CheatType.FISHING, b);
    }

    @Override
    public boolean isHealthCheat() {
        return this.getCheats().isSet(CheatType.HEALTH);
    }

    @Override
    public void setHealthCheat(boolean healthCheat) {
        if (!Role.hasCapability(this, Capability.UseHealthCheat)) {
            this.getCheats().set(CheatType.HEALTH, false);
            return;
        }
        this.getCheats().set(CheatType.HEALTH, healthCheat);
    }

    @Override
    public boolean isMechanicsCheat() {
        return this.getCheats().isSet(CheatType.MECHANICS);
    }

    @Override
    public void setMechanicsCheat(boolean mechanicsCheat) {
        if (!Role.hasCapability(this, Capability.UseMechanicsCheat)) {
            this.getCheats().set(CheatType.MECHANICS, false);
            return;
        }
        this.getCheats().set(CheatType.MECHANICS, mechanicsCheat);
    }

    public boolean isFastMoveCheat() {
        return this.getCheats().isSet(CheatType.FAST_MOVE);
    }

    public void setFastMoveCheat(boolean b) {
        if (!Role.hasCapability(this, Capability.UseFastMoveCheat)) {
            this.getCheats().set(CheatType.FAST_MOVE, false);
            return;
        }
        this.getCheats().set(CheatType.FAST_MOVE, b);
    }

    @Override
    public boolean isMovablesCheat() {
        return this.getCheats().isSet(CheatType.MOVABLES);
    }

    @Override
    public void setMovablesCheat(boolean b) {
        if (!Role.hasCapability(this, Capability.UseMovablesCheat)) {
            this.getCheats().set(CheatType.MOVABLES, false);
            return;
        }
        this.getCheats().set(CheatType.MOVABLES, b);
    }

    @Override
    public boolean isAnimalCheat() {
        return this.getCheats().isSet(CheatType.ANIMAL);
    }

    @Override
    public void setAnimalCheat(boolean b) {
        if (!Role.hasCapability(this, Capability.AnimalCheats)) {
            this.getCheats().set(CheatType.ANIMAL, false);
            return;
        }
        this.getCheats().set(CheatType.ANIMAL, b);
    }

    @Override
    public boolean isAnimalExtraValuesCheat() {
        return this.getCheats().isSet(CheatType.ANIMAL_EXTRA_VALUES);
    }

    @Override
    public void setAnimalExtraValuesCheat(boolean b) {
        if (!Role.hasCapability(this, Capability.AnimalCheats)) {
            this.getCheats().set(CheatType.ANIMAL_EXTRA_VALUES, false);
            return;
        }
        this.getCheats().set(CheatType.ANIMAL_EXTRA_VALUES, b);
    }

    @Override
    public boolean isTimedActionInstantCheat() {
        return this.getCheats().isSet(CheatType.TIMED_ACTION_INSTANT);
    }

    @Override
    public void setTimedActionInstantCheat(boolean b) {
        if (!Role.hasCapability(this, Capability.UseTimedActionInstantCheat)) {
            this.getCheats().set(CheatType.TIMED_ACTION_INSTANT, false);
            return;
        }
        this.getCheats().set(CheatType.TIMED_ACTION_INSTANT, b);
    }

    @Override
    public boolean isTimedActionInstant() {
        if (Core.debug && DebugOptions.instance.cheat.timedAction.instant.getValue()) {
            return true;
        }
        return this.isTimedActionInstantCheat();
    }

    @Override
    public boolean isShowAdminTag() {
        return this.showAdminTag;
    }

    @Override
    public void setShowAdminTag(boolean showAdminTag) {
        this.showAdminTag = showAdminTag;
    }

    @Override
    public Iterable<IAnimationVariableSlot> getGameVariables() {
        ActionContext actionContext = this.getActionContext();
        if (actionContext == null || !actionContext.hasStateVariables()) {
            return this.getGameVariablesInternal().getGameVariables();
        }
        return () -> new Iterator<IAnimationVariableSlot>(this){
            private final Iterator<AnimationVariableHandle> iterator;
            private IAnimationVariableSlot nextSlot;
            final /* synthetic */ IsoGameCharacter this$0;
            {
                IsoGameCharacter isoGameCharacter = this$0;
                Objects.requireNonNull(isoGameCharacter);
                this.this$0 = isoGameCharacter;
                this.iterator = AnimationVariableHandlePool.all().iterator();
                this.nextSlot = this.findNextSlot();
            }

            @Override
            public boolean hasNext() {
                return this.nextSlot != null;
            }

            @Override
            public IAnimationVariableSlot next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                }
                IAnimationVariableSlot currentSlot = this.nextSlot;
                this.nextSlot = this.findNextSlot();
                return currentSlot;
            }

            private IAnimationVariableSlot findNextSlot() {
                IAnimationVariableSlot nextSlot = null;
                while (this.iterator.hasNext()) {
                    AnimationVariableHandle nextHandle = this.iterator.next();
                    IAnimationVariableSlot slot = this.this$0.getVariable(nextHandle);
                    if (slot == null) continue;
                    nextSlot = slot;
                    break;
                }
                return nextSlot;
            }
        };
    }

    @Override
    public IAnimationVariableSlot getVariable(AnimationVariableHandle handle) {
        IAnimationVariableSlot actionSlot;
        ActionContext actionContext = this.getActionContext();
        if (actionContext != null && (actionSlot = actionContext.getVariable(handle)) != null) {
            return actionSlot;
        }
        return this.getGameVariablesInternal().getVariable(handle);
    }

    @Override
    public void setVariable(IAnimationVariableSlot var) {
        if (GameServer.server && this instanceof IsoZombie) {
            return;
        }
        this.getGameVariablesInternal().setVariable(var);
    }

    @Override
    public IAnimationVariableSlot setVariable(String key, String value) {
        if (GameServer.server && this instanceof IsoZombie) {
            return null;
        }
        if (GameClient.client && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer() && VariableSyncPacket.syncedVariables.contains(key)) {
            INetworkPacket.send(PacketTypes.PacketType.VariableSync, this, key, value);
        }
        return this.getGameVariablesInternal().setVariable(key, value);
    }

    @Override
    public IAnimationVariableSlot setVariable(String key, boolean value) {
        if (GameServer.server && this instanceof IsoZombie) {
            return null;
        }
        if (GameClient.client && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer() && VariableSyncPacket.syncedVariables.contains(key)) {
            INetworkPacket.send(PacketTypes.PacketType.VariableSync, this, key, value);
        }
        return this.getGameVariablesInternal().setVariable(key, value);
    }

    @Override
    public IAnimationVariableSlot setVariable(String key, float value) {
        if (GameServer.server && this instanceof IsoZombie) {
            return null;
        }
        if (GameClient.client && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer() && VariableSyncPacket.syncedVariables.contains(key)) {
            INetworkPacket.send(PacketTypes.PacketType.VariableSync, this, key, Float.valueOf(value));
        }
        return this.getGameVariablesInternal().setVariable(key, value);
    }

    @Override
    public <EnumType extends Enum<EnumType>> IAnimationVariableSlot setVariableEnum(String key, EnumType value) {
        if (GameClient.client && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer() && VariableSyncPacket.syncedVariables.contains(key)) {
            INetworkPacket.send(PacketTypes.PacketType.VariableSync, this, key, value);
        }
        return this.getGameVariablesInternal().setVariableEnum(key, value);
    }

    @Override
    public IAnimationVariableSlot setVariable(AnimationVariableHandle handle, boolean value) {
        String key = handle.getVariableName();
        if (GameServer.server && this instanceof IsoZombie) {
            return null;
        }
        if (GameClient.client && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer() && VariableSyncPacket.syncedVariables.contains(key)) {
            INetworkPacket.send(PacketTypes.PacketType.VariableSync, this, key, value);
        }
        return this.getGameVariablesInternal().setVariable(handle, value);
    }

    @Override
    public void clearVariable(String key) {
        this.getGameVariablesInternal().clearVariable(key);
    }

    @Override
    public void clearVariables() {
        this.getGameVariablesInternal().clearVariables();
    }

    private String getFootInjuryType() {
        if (!(this instanceof IsoPlayer)) {
            return "";
        }
        BodyPart footL = this.getBodyDamage().getBodyPart(BodyPartType.Foot_L);
        BodyPart footR = this.getBodyDamage().getBodyPart(BodyPartType.Foot_R);
        if (!this.running) {
            if (footL.haveBullet() || footL.getBurnTime() > 5.0f || footL.bitten() || footL.deepWounded() || footL.isSplint() || footL.getFractureTime() > 0.0f || footL.haveGlass()) {
                return "leftheavy";
            }
            if (footR.haveBullet() || footR.getBurnTime() > 5.0f || footR.bitten() || footR.deepWounded() || footR.isSplint() || footR.getFractureTime() > 0.0f || footR.haveGlass()) {
                return "rightheavy";
            }
        }
        if (footL.getScratchTime() > 5.0f || footL.getCutTime() > 7.0f || footL.getBurnTime() > 0.0f) {
            return "leftlight";
        }
        if (footR.getScratchTime() > 5.0f || footR.getCutTime() > 7.0f || footR.getBurnTime() > 0.0f) {
            return "rightlight";
        }
        return "";
    }

    @Override
    public IAnimationVariableSource getSubVariableSource(String subVariableSourceName) {
        if (subVariableSourceName.equals("GrappledTarget")) {
            return IGrappleable.getAnimatable(this.getGrapplingTarget());
        }
        if (subVariableSourceName.equals("GrappledBy")) {
            return IGrappleable.getAnimatable(this.getGrappledBy());
        }
        return null;
    }

    @Override
    public AnimationVariableSource getGameVariablesInternal() {
        if (this.playbackGameVariables != null) {
            return this.playbackGameVariables;
        }
        return this.gameVariables;
    }

    public AnimationVariableSource startPlaybackGameVariables() {
        if (this.playbackGameVariables != null) {
            DebugLog.General.error("Error! PlaybackGameVariables is already active.");
            return this.playbackGameVariables;
        }
        AnimationVariableSource playbackVars = new AnimationVariableSource();
        block6: for (IAnimationVariableSlot var : this.getGameVariables()) {
            AnimationVariableType varType = var.getType();
            switch (varType) {
                case String: {
                    playbackVars.setVariable(var.getKey(), var.getValueString());
                    continue block6;
                }
                case Float: {
                    playbackVars.setVariable(var.getKey(), var.getValueFloat());
                    continue block6;
                }
                case Boolean: {
                    playbackVars.setVariable(var.getKey(), var.getValueBool());
                    continue block6;
                }
                case Void: {
                    continue block6;
                }
            }
            DebugLog.General.error("Error! Variable type not handled: %s", varType.toString());
        }
        this.playbackGameVariables = playbackVars;
        return this.playbackGameVariables;
    }

    public void endPlaybackGameVariables(AnimationVariableSource playbackVars) {
        if (this.playbackGameVariables != playbackVars) {
            DebugLog.General.error("Error! Playback GameVariables do not match.");
        }
        this.playbackGameVariables = null;
    }

    public void playbackSetCurrentStateSnapshot(ActionStateSnapshot snapshot) {
        if (this.actionContext == null) {
            return;
        }
        this.actionContext.setPlaybackStateSnapshot(snapshot);
    }

    public ActionStateSnapshot playbackRecordCurrentStateSnapshot() {
        if (this.actionContext == null) {
            return null;
        }
        return this.actionContext.getPlaybackStateSnapshot();
    }

    @Override
    public String GetVariable(String key) {
        return this.getVariableString(key);
    }

    @Override
    public void SetVariable(String key, String value) {
        this.setVariable(key, value);
    }

    @Override
    public void ClearVariable(String key) {
        this.clearVariable(key);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void actionStateChanged(ActionContext sender) {
        for (int ii = 0; ii < L_actionStateChanged.stateNames.size(); ++ii) {
            DebugType.AnimationDetailed.debugln("************* stateNames: %s", L_actionStateChanged.stateNames.get(ii));
        }
        ArrayList<String> stateNames = L_actionStateChanged.stateNames;
        PZArrayUtil.listConvert(sender.getChildStates(), stateNames, state -> state.getName());
        for (int ii = 0; ii < L_actionStateChanged.stateNames.size(); ++ii) {
            DebugType.AnimationDetailed.debugln("************* stateNames: %s", L_actionStateChanged.stateNames.get(ii));
        }
        this.advancedAnimator.setState(sender.getCurrentStateName(), stateNames);
        try {
            ++this.stateMachine.activeStateChanged;
            State aiState = this.tryGetAIState(sender.getCurrentStateName());
            if (aiState == null) {
                aiState = this.defaultState;
            }
            ArrayList<State> childStates = L_actionStateChanged.states;
            PZArrayUtil.listConvert(sender.getChildStates(), childStates, this.aiStateMap, (state2, lLookup) -> (State)lLookup.get(state2.getName().toLowerCase()));
            this.stateMachine.changeState(aiState, childStates, false);
        }
        finally {
            --this.stateMachine.activeStateChanged;
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

    public boolean isHitFromBehind() {
        return this.hitFromBehind;
    }

    public void setHitFromBehind(boolean hitFromBehind) {
        this.hitFromBehind = hitFromBehind;
    }

    public boolean isKilledBySlicingWeapon() {
        if (this.damagedByVehicle) {
            return false;
        }
        IsoGameCharacter killer = this.getAttackedBy();
        if (killer == null) {
            return false;
        }
        HandWeapon weapon = killer.getAttackingWeapon();
        if (weapon == null) {
            return false;
        }
        return weapon.isOfWeaponCategory(WeaponCategory.LONG_BLADE);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean testCollideWithVehicles(BaseVehicle vehicle, BaseVehicle.HitVars hitVars) {
        if (!this.isAlive() && this.isProne()) {
            return false;
        }
        if (this.getVehicle() != null) {
            return false;
        }
        if (this.isProne()) {
            int numWheelsHit;
            IsoGameCharacter driver = vehicle.getDriverRegardlessOfTow();
            if (driver == null) {
                return false;
            }
            Vector2 impactPosOnVehicle = (Vector2)Vector2ObjectPool.get().alloc();
            int n = numWheelsHit = vehicle.isEngineRunning() ? vehicle.testCollisionWithProneCharacter(this, true, impactPosOnVehicle) : 0;
            if (numWheelsHit > 0) {
                if (!this.emitter.isPlaying(this.getHurtSound()) && this.isStanding()) {
                    this.playHurtSound();
                }
                float damageToPedestrian = 0.0f;
                this.attackedBy = driver;
                try {
                    this.damagedByVehicle = true;
                    damageToPedestrian = vehicle.hitCharacter(this, impactPosOnVehicle, false);
                }
                finally {
                    this.damagedByVehicle = false;
                }
                if (!GameServer.server && !GameClient.client && this.isDead()) {
                    this.Kill(driver);
                }
                if (hitVars != null) {
                    hitVars.isPushedBack = false;
                    hitVars.damageToPedestrian = damageToPedestrian;
                    hitVars.collision.set(impactPosOnVehicle);
                    hitVars.calc(this, vehicle);
                }
                return true;
            }
        }
        if (vehicle.shouldCollideWithCharacters()) {
            Vector2 impactPosOnVehicle = (Vector2)Vector2ObjectPool.get().alloc();
            if (vehicle.testCollisionWithCharacter(this, 0.3f, impactPosOnVehicle) != null) {
                float damageToPedestrian = 0.0f;
                boolean shouldPushBack = this.shouldBePushedBackByVehicleHit();
                try {
                    this.damagedByVehicle = true;
                    damageToPedestrian = vehicle.hitCharacter(this, impactPosOnVehicle, shouldPushBack);
                }
                finally {
                    this.damagedByVehicle = false;
                }
                if (hitVars != null) {
                    hitVars.isPushedBack = shouldPushBack;
                    hitVars.damageToPedestrian = damageToPedestrian;
                    hitVars.collision.set(impactPosOnVehicle);
                    hitVars.calc(this, vehicle);
                }
                Vector2ObjectPool.get().release(impactPosOnVehicle);
                return true;
            }
            Vector2ObjectPool.get().release(impactPosOnVehicle);
        }
        return false;
    }

    public boolean shouldBePushedBackByVehicleHit() {
        if (this.isProne()) {
            return false;
        }
        if (this.isGettingUp()) {
            return false;
        }
        return !this.isRagdollSimulationActive();
    }

    public float onHitByVehicle(BaseVehicle vehicle, float impactSpeed, Vector2 hitDir, Vector2 impactPosOnVehicle, boolean pushedBack) {
        boolean isHitFromBehind = this.isImpactFromBehind(hitDir);
        if (GameClient.client || GameServer.server) {
            this.setAttackedBy(GameServer.IDToPlayerMap.get(vehicle.getNetPlayerId()));
        } else {
            this.onHitByVehicleDriver(vehicle.getCurrentOrLastKnownDriver());
        }
        this.setHitDir(hitDir);
        this.setHitFromBehind(isHitFromBehind);
        this.setHitForce(impactSpeed * 0.15f);
        this.setVehicleCollision(true);
        float damage = this.onHitByVehicleApplyDamage(vehicle, impactSpeed, pushedBack);
        if (pushedBack) {
            if (!this.isRagdollSimulationActive() && impactPosOnVehicle != null) {
                this.slideAwayToCollisionPos(impactPosOnVehicle.x, impactPosOnVehicle.y, false);
            }
            float collisionAvoidanceRadius = 0.25f;
            this.slideAwayFromWalls(0.25f, false, true);
        }
        if (this.isProne() || this.isGettingUp()) {
            this.setHitReaction("Floor");
            return damage;
        }
        this.postHitByVehicleUpdateStance(impactSpeed, true);
        return damage;
    }

    public float onHitByVehicleApplyDamage(BaseVehicle vehicle, float impactSpeed, boolean pushedBack) {
        boolean isBeingCrushed;
        float damageFromImpact;
        if (!this.isAlive()) {
            return 0.0f;
        }
        float damage = damageFromImpact = this.calculateDamageFromVehicleImpact(impactSpeed);
        boolean isCrushed = this.isProne();
        boolean isFirstImpact = !vehicle.isPersistentContact(this);
        boolean isBeingPushed = pushedBack && !isFirstImpact;
        boolean bl = isBeingCrushed = isCrushed && !isFirstImpact;
        if (!isFirstImpact) {
            float sequentialDamageModifier = 1.0f;
            float damageFromBeingPushed = 0.01f;
            float damageFromBeingCrushed = 0.2f;
            if (isBeingPushed) {
                sequentialDamageModifier = 0.01f;
            } else if (isBeingCrushed) {
                sequentialDamageModifier = 0.2f;
            }
            damage = damageFromImpact * sequentialDamageModifier * GameTime.getInstance().getMultiplier();
        }
        if (this.isLocal() && !this.isGodMod()) {
            boolean wasDead = this.isDead();
            this.applyDamageFromVehicleHit(impactSpeed, damage);
            boolean isDead = this.isDead();
            if (isDead && !wasDead) {
                if (this.attackedBy != null) {
                    this.attackedBy.setZombieKills(this.attackedBy.getZombieKills() + 1);
                }
                this.Kill(this.attackedBy);
            }
        }
        if (this.isAlive()) {
            DebugType.VehicleHit.trace("%s %s Speed; %.4f, Damage: %.4f, Heath: %.4f", this.getClass().getSimpleName(), isCrushed ? "crushed" : (isBeingPushed ? "push" : "hit"), Float.valueOf(impactSpeed), Float.valueOf(damage), Float.valueOf(this.getHealth()));
        } else {
            DebugType.VehicleHit.debugln("%s %s Killed. Speed; %.4f, Damage: %.4f, Heath: %.4f", this.getClass().getSimpleName(), isCrushed ? "crushed" : (isBeingPushed ? "push" : "hit"), Float.valueOf(impactSpeed), Float.valueOf(damage), Float.valueOf(this.getHealth()));
        }
        return damageFromImpact;
    }

    protected void onHitByVehicleDriver(IsoGameCharacter vehicleDriver) {
        this.attackedBy = vehicleDriver;
        if (this.attackedBy != null) {
            this.attackedBy.setUseHandWeapon(null);
        }
    }

    public void applyDamageFromVehicleHit(float vehicleSpeed, float damage) {
        DebugType.VehicleHit.debugln("Character vehicle hit speed: %f, damage: %f. Zed: %s", Float.valueOf(vehicleSpeed), Float.valueOf(damage), this);
        if (!GameServer.server) {
            boolean addBlood;
            float minDamageForBlood = 0.05f;
            boolean bl = addBlood = damage > 0.05f;
            if (addBlood) {
                this.addBloodFromVehicleImpact(vehicleSpeed);
            }
        }
        CombatManager.getInstance().applyDamage(this, damage);
    }

    protected float calculateDamageFromVehicleImpact(float impactSpeed) {
        if (this.isProne()) {
            return this.calculateDamageFromVehicleRunOver(impactSpeed);
        }
        float impactSpeedNoDamage = 3.0f;
        float impactSpeedMaxDamage = 20.0f;
        float maxDamage = 1.5f;
        float impactDamageAlpha = (impactSpeed - 3.0f) / 17.0f;
        if (impactDamageAlpha < 0.0f) {
            return 0.0f;
        }
        return 1.5f * PZMath.lerpFunc_EaseOutQuad(impactDamageAlpha);
    }

    protected float calculateDamageFromVehicleRunOver(float impactSpeed) {
        float maxDamage = 0.5f;
        float impactSpeedNoDamage = 0.2f;
        float impactSpeedMaxDamage = 10.0f;
        float impactDamageAlpha = (impactSpeed - 0.2f) / 9.8f;
        if (impactDamageAlpha < 0.0f) {
            return 0.0f;
        }
        return 0.5f * PZMath.lerpFunc_EaseOutQuad(impactDamageAlpha);
    }

    protected void postHitByVehicleUpdateStance(float speed, boolean knockDownAllowed) {
        if (GameServer.server) {
            return;
        }
        float minSpeedToPossibleKnockdown = 3.5f;
        float maxSpeedToPossibleKnockdown = 14.5f;
        boolean knockedDown = this.isKnockedDown();
        if (knockDownAllowed && !knockedDown && speed > 3.5f) {
            boolean shouldKnockDown;
            float knockDownRandAlpha = (speed - 3.5f) / 11.0f;
            float knockDownRandf = PZMath.lerpFunc_EaseInQuad(knockDownRandAlpha);
            int knockDownRand = (int)(knockDownRandf * 100.0f);
            boolean bl = shouldKnockDown = knockDownRandAlpha > 1.0f || Rand.Next(100) <= knockDownRand;
            if (shouldKnockDown) {
                knockedDown = true;
            }
        }
        this.setKnockedDown(knockedDown);
    }

    @Override
    public void reportEvent(String name) {
        this.actionContext.reportEvent(name);
    }

    @Override
    public void StartTimedActionAnim(String event) {
        this.StartTimedActionAnim(event, null);
    }

    @Override
    public void StartTimedActionAnim(String event, String type) {
        this.reportEvent(event);
        if (type != null) {
            this.setVariable("TimedActionType", type);
        }
        this.resetModelNextFrame();
    }

    @Override
    public void StopTimedActionAnim() {
        this.clearVariable("TimedActionType");
        this.reportEvent("Event_TA_Exit");
        this.resetModelNextFrame();
    }

    public boolean hasHitReaction() {
        return !StringUtils.isNullOrEmpty(this.getHitReaction());
    }

    public String getHitReaction() {
        return this.hitReaction;
    }

    public void setHitReaction(String hitReaction) {
        if (!StringUtils.equals(this.hitReaction, hitReaction)) {
            this.hitReaction = hitReaction;
        }
    }

    public void CacheEquipped() {
        this.cacheEquiped[0] = this.getPrimaryHandItem();
        this.cacheEquiped[1] = this.getSecondaryHandItem();
    }

    public InventoryItem GetPrimaryEquippedCache() {
        return this.cacheEquiped[0] != null && this.inventory.contains(this.cacheEquiped[0]) ? this.cacheEquiped[0] : null;
    }

    public InventoryItem GetSecondaryEquippedCache() {
        return this.cacheEquiped[1] != null && this.inventory.contains(this.cacheEquiped[1]) ? this.cacheEquiped[1] : null;
    }

    public void ClearEquippedCache() {
        this.cacheEquiped[0] = null;
        this.cacheEquiped[1] = null;
    }

    public boolean isObjectBehind(IsoObject obj) {
        Vector2 oPos = tempVector2_1.set(obj.getX(), obj.getY());
        Vector2 tPos = tempVector2_2.set(this.getX(), this.getY());
        tPos.x -= oPos.x;
        tPos.y -= oPos.y;
        Vector2 dir = this.getForwardDirection();
        tPos.normalize();
        dir.normalize();
        float dot = tPos.dot(dir);
        return dot > 0.6f;
    }

    public boolean isBehind(IsoGameCharacter chr) {
        Vector2 oPos = tempVector2_1.set(this.getX(), this.getY());
        Vector2 tPos = tempVector2_2.set(chr.getX(), chr.getY());
        tPos.x -= oPos.x;
        tPos.y -= oPos.y;
        Vector2 dir = chr.getForwardDirection();
        tPos.normalize();
        dir.normalize();
        float dot = tPos.dot(dir);
        return dot > 0.6f;
    }

    public void resetEquippedHandsModels() {
        if (GameServer.server && !ServerGUI.isCreated()) {
            return;
        }
        if (!this.hasActiveModel()) {
            return;
        }
        ModelManager.instance.ResetEquippedNextFrame(this);
    }

    @Override
    public AnimatorDebugMonitor getDebugMonitor() {
        return this.advancedAnimator.getDebugMonitor();
    }

    @Override
    public void setDebugMonitor(AnimatorDebugMonitor monitor) {
        this.advancedAnimator.setDebugMonitor(monitor);
    }

    public boolean isAimAtFloor() {
        return this.aimAtFloor;
    }

    public void setAimAtFloor(boolean aimAtFloor) {
        this.setAimAtFloor(aimAtFloor, 0.0f);
    }

    public void setAimAtFloor(boolean aimAtFloor, float targetDistance) {
        this.aimAtFloor = aimAtFloor;
        this.aimAtFloorTargetDistance = targetDistance;
    }

    public float aimAtFloorTargetDistance() {
        return this.aimAtFloorTargetDistance;
    }

    public float getAimAtFloorAmount() {
        float aimAngle = this.getCurrentVerticalAimAngle();
        if (PZMath.equal(aimAngle, 0.0f, 0.1f)) {
            return 0.0f;
        }
        float aimAngleFrac = -aimAngle / 90.0f;
        return PZMath.clamp(aimAngleFrac, 0.0f, 1.0f);
    }

    public float getCurrentVerticalAimAngle() {
        return this.currentVerticalAimAngleDegrees;
    }

    public void setCurrentVerticalAimAngle(float verticalAimAngleDegrees) {
        this.currentVerticalAimAngleDegrees = verticalAimAngleDegrees;
    }

    public void setTargetVerticalAimAngle(float verticalAimAngleDegrees) {
        float wrappedAngle = PZMath.wrap(verticalAimAngleDegrees, -180.0f, 180.0f);
        float clampedAngle = wrappedAngle < -90.0f ? -180.0f - wrappedAngle : (wrappedAngle > 90.0f ? 180.0f - wrappedAngle : wrappedAngle);
        this.targetVerticalAimAngleDegrees = clampedAngle;
    }

    public float getTargetVerticalAimAngle() {
        return this.targetVerticalAimAngleDegrees;
    }

    public boolean isDeferredMovementEnabled() {
        return this.deferredMovementEnabled;
    }

    public void setDeferredMovementEnabled(boolean deferredMovementEnabled) {
        this.deferredMovementEnabled = deferredMovementEnabled;
    }

    public String testDotSide(IsoMovingObject target) {
        return this.testDotSideEnum(target).name();
    }

    public Side testDotSideEnum(IsoMovingObject target) {
        float x2;
        float py;
        float y1;
        float y2;
        float x1;
        Vector2 zombieLookVector = this.getLookVector(l_testDotSide.v1);
        Vector2 zombiePos = l_testDotSide.v2.set(this.getX(), this.getY());
        Vector2 zombieToPlayer = l_testDotSide.v3.set(target.getX() - zombiePos.x, target.getY() - zombiePos.y);
        zombieToPlayer.normalize();
        float dotZombieToPlayer = Vector2.dot(zombieToPlayer.x, zombieToPlayer.y, zombieLookVector.x, zombieLookVector.y);
        if ((double)dotZombieToPlayer > 0.7) {
            return Side.FRONT;
        }
        if (dotZombieToPlayer < 0.0f && (double)dotZombieToPlayer < -0.5) {
            return Side.BEHIND;
        }
        float px = target.getX();
        float d = (px - (x1 = zombiePos.x)) * ((y2 = zombiePos.y + zombieLookVector.y) - (y1 = zombiePos.y)) - ((py = target.getY()) - y1) * ((x2 = zombiePos.x + zombieLookVector.x) - x1);
        if (d > 0.0f) {
            return Side.RIGHT;
        }
        return Side.LEFT;
    }

    public void addBasicPatch(BloodBodyPartType part) {
        if (!(this instanceof IHumanVisual)) {
            return;
        }
        IsoGameCharacter isoGameCharacter = this;
        if (isoGameCharacter instanceof IsoPlayer) {
            IsoPlayer player = (IsoPlayer)isoGameCharacter;
            if (GameServer.server || GameClient.client && !player.isLocalPlayer()) {
                return;
            }
        }
        if (part == null) {
            part = BloodBodyPartType.FromIndex(Rand.Next(0, BloodBodyPartType.MAX.index()));
        }
        HumanVisual humanVisual = ((IHumanVisual)((Object)this)).getHumanVisual();
        this.getItemVisuals(tempItemVisuals);
        BloodClothingType.addBasicPatch(part, humanVisual, tempItemVisuals);
        this.updateModelTextures = true;
        this.updateEquippedTextures = true;
        if (!GameServer.server && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
            LuaEventManager.triggerEvent("OnClothingUpdated", this);
            if (GameClient.client) {
                INetworkPacket.send(PacketTypes.PacketType.SyncClothing, this);
                INetworkPacket.send(PacketTypes.PacketType.SyncVisuals, this);
            }
        }
    }

    @Override
    public boolean addHole(BloodBodyPartType part) {
        return this.addHole(part, false);
    }

    public boolean addHole(BloodBodyPartType part, boolean allLayers) {
        if (!(this instanceof IHumanVisual)) {
            return false;
        }
        IsoGameCharacter isoGameCharacter = this;
        if (isoGameCharacter instanceof IsoPlayer) {
            IsoPlayer player = (IsoPlayer)isoGameCharacter;
            if (GameServer.server || GameClient.client && !player.isLocalPlayer()) {
                return false;
            }
        }
        if (part == null) {
            part = BloodBodyPartType.FromIndex(OutfitRNG.Next(0, BloodBodyPartType.MAX.index()));
        }
        HumanVisual humanVisual = ((IHumanVisual)((Object)this)).getHumanVisual();
        this.getItemVisuals(tempItemVisuals);
        boolean addedHole = BloodClothingType.addHole(part, humanVisual, tempItemVisuals, allLayers);
        this.updateModelTextures = true;
        if (!GameServer.server && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
            LuaEventManager.triggerEvent("OnClothingUpdated", this);
            if (GameClient.client) {
                INetworkPacket.send(PacketTypes.PacketType.SyncClothing, this);
                INetworkPacket.send(PacketTypes.PacketType.SyncVisuals, this);
            }
        }
        return addedHole;
    }

    public void addDirt(BloodBodyPartType part, Integer nbr, boolean allLayers) {
        if (this instanceof IsoAnimal) {
            return;
        }
        IsoGameCharacter isoGameCharacter = this;
        if (isoGameCharacter instanceof IsoPlayer) {
            IsoPlayer player = (IsoPlayer)isoGameCharacter;
            if (GameServer.server || GameClient.client && !player.isLocalPlayer()) {
                return;
            }
        }
        HumanVisual humanVisual = ((IHumanVisual)((Object)this)).getHumanVisual();
        if (nbr == null) {
            nbr = OutfitRNG.Next(5, 10);
        }
        boolean randomPart = false;
        if (part == null) {
            randomPart = true;
        }
        this.getItemVisuals(tempItemVisuals);
        for (int i = 0; i < nbr; ++i) {
            if (randomPart) {
                part = BloodBodyPartType.FromIndex(OutfitRNG.Next(0, BloodBodyPartType.MAX.index()));
            }
            BloodClothingType.addDirt(part, humanVisual, tempItemVisuals, allLayers);
        }
        this.updateModelTextures = true;
        if (!GameServer.server && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
            LuaEventManager.triggerEvent("OnClothingUpdated", this);
            if (GameClient.client) {
                INetworkPacket.send(PacketTypes.PacketType.SyncClothing, this);
                INetworkPacket.send(PacketTypes.PacketType.SyncVisuals, this);
            }
        }
    }

    public void addLotsOfDirt(BloodBodyPartType part, Integer nbr, boolean allLayers) {
        if (this instanceof IsoAnimal) {
            return;
        }
        IsoGameCharacter isoGameCharacter = this;
        if (isoGameCharacter instanceof IsoPlayer) {
            IsoPlayer player = (IsoPlayer)isoGameCharacter;
            if (GameServer.server || GameClient.client && !player.isLocalPlayer()) {
                return;
            }
        }
        HumanVisual humanVisual = ((IHumanVisual)((Object)this)).getHumanVisual();
        if (nbr == null) {
            nbr = OutfitRNG.Next(5, 10);
        }
        boolean randomPart = false;
        if (part == null) {
            randomPart = true;
        }
        this.getItemVisuals(tempItemVisuals);
        for (int i = 0; i < nbr; ++i) {
            if (randomPart) {
                part = BloodBodyPartType.FromIndex(OutfitRNG.Next(0, BloodBodyPartType.MAX.index()));
            }
            BloodClothingType.addDirt(part, Rand.Next(0.01f, 1.0f), humanVisual, tempItemVisuals, allLayers);
        }
        this.updateModelTextures = true;
        if (!GameServer.server && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
            LuaEventManager.triggerEvent("OnClothingUpdated", this);
            if (GameClient.client) {
                INetworkPacket.send(PacketTypes.PacketType.SyncClothing, this);
                INetworkPacket.send(PacketTypes.PacketType.SyncVisuals, this);
            }
        }
    }

    @Override
    public void addBlood(BloodBodyPartType part, boolean scratched, boolean bitten, boolean allLayers) {
        if (this instanceof IsoAnimal) {
            return;
        }
        IsoGameCharacter isoGameCharacter = this;
        if (isoGameCharacter instanceof IsoPlayer) {
            IsoPlayer player = (IsoPlayer)isoGameCharacter;
            if (GameServer.server || GameClient.client && !player.isLocalPlayer()) {
                return;
            }
        }
        HumanVisual humanVisual = ((IHumanVisual)((Object)this)).getHumanVisual();
        int nbr = 1;
        boolean randomPart = false;
        if (part == null) {
            randomPart = true;
        }
        if (this.getPrimaryHandItem() instanceof HandWeapon) {
            nbr = ((HandWeapon)this.getPrimaryHandItem()).getSplatNumber();
            if (OutfitRNG.Next(15) < this.getWeaponLevel()) {
                --nbr;
            }
        }
        if (bitten) {
            nbr = 20;
        }
        if (scratched) {
            nbr = 5;
        }
        if (this.isZombie()) {
            nbr += 8;
        }
        this.getItemVisuals(tempItemVisuals);
        for (int i = 0; i < nbr; ++i) {
            if (randomPart) {
                HandWeapon weapon;
                InventoryItem inventoryItem;
                part = BloodBodyPartType.FromIndex(OutfitRNG.Next(0, BloodBodyPartType.MAX.index()));
                if (this.getPrimaryHandItem() != null && (inventoryItem = this.getPrimaryHandItem()) instanceof HandWeapon && (weapon = (HandWeapon)inventoryItem).getBloodLevel() < 1.0f) {
                    float bloodLevel = weapon.getBloodLevel() + 0.02f;
                    weapon.setBloodLevel(bloodLevel);
                    this.updateEquippedTextures = true;
                }
            }
            BloodClothingType.addBlood(part, humanVisual, (ArrayList<ItemVisual>)tempItemVisuals, allLayers);
        }
        this.updateModelTextures = true;
        if (!GameServer.server && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
            LuaEventManager.triggerEvent("OnClothingUpdated", this);
            if (GameClient.client) {
                INetworkPacket.send(PacketTypes.PacketType.SyncClothing, this);
                INetworkPacket.send(PacketTypes.PacketType.SyncVisuals, this);
            }
        }
    }

    private boolean bodyPartHasTag(Integer part, ItemTag itemTag) {
        this.getItemVisuals(tempItemVisuals);
        for (int i = tempItemVisuals.size() - 1; i >= 0; --i) {
            InventoryItem item;
            ArrayList<BloodBodyPartType> coveredParts;
            ArrayList<BloodClothingType> types;
            ItemVisual itemVisual = (ItemVisual)tempItemVisuals.get(i);
            Item scriptItem = itemVisual.getScriptItem();
            if (scriptItem == null || (types = scriptItem.getBloodClothingType()) == null || (coveredParts = BloodClothingType.getCoveredParts(types)) == null || (item = itemVisual.getInventoryItem()) == null && (item = InventoryItemFactory.CreateItem(itemVisual.getItemType())) == null) continue;
            for (int j = 0; j < coveredParts.size(); ++j) {
                if (!(item instanceof Clothing) || coveredParts.get(j).index() != part.intValue() || itemVisual.getHole(coveredParts.get(j)) != 0.0f || !item.hasTag(itemTag)) continue;
                return true;
            }
        }
        return false;
    }

    public boolean bodyPartIsSpiked(Integer part) {
        return this.bodyPartHasTag(part, ItemTag.SPIKED);
    }

    public boolean bodyPartIsSpikedBehind(Integer part) {
        return this.bodyPartHasTag(part, ItemTag.SPIKED_BEHIND);
    }

    public float getBodyPartClothingDefense(Integer part, boolean bite, boolean bullet) {
        float result = 0.0f;
        this.getItemVisuals(tempItemVisuals);
        block0: for (int i = tempItemVisuals.size() - 1; i >= 0; --i) {
            InventoryItem item;
            ArrayList<BloodBodyPartType> coveredParts;
            ArrayList<BloodClothingType> types;
            ItemVisual itemVisual = (ItemVisual)tempItemVisuals.get(i);
            Item scriptItem = itemVisual.getScriptItem();
            if (scriptItem == null || (types = scriptItem.getBloodClothingType()) == null || (coveredParts = BloodClothingType.getCoveredParts(types)) == null || (item = itemVisual.getInventoryItem()) == null && (item = InventoryItemFactory.CreateItem(itemVisual.getItemType())) == null) continue;
            for (int j = 0; j < coveredParts.size(); ++j) {
                if (!(item instanceof Clothing)) continue;
                Clothing clothing = (Clothing)item;
                if (coveredParts.get(j).index() != part.intValue() || itemVisual.getHole(coveredParts.get(j)) != 0.0f) continue;
                result += clothing.getDefForPart(coveredParts.get(j), bite, bullet);
                continue block0;
            }
        }
        result = Math.min(100.0f, result);
        return result;
    }

    @Override
    public boolean isBumped() {
        return !StringUtils.isNullOrWhitespace(this.getBumpType());
    }

    public boolean isBumpDone() {
        return this.isBumpDone;
    }

    public void setBumpDone(boolean val) {
        this.isBumpDone = val;
    }

    public boolean isBumpFall() {
        return this.bumpFall;
    }

    public void setBumpFall(boolean val) {
        this.bumpFall = val;
    }

    public boolean isBumpStaggered() {
        return this.bumpStaggered;
    }

    public void setBumpStaggered(boolean val) {
        this.bumpStaggered = val;
    }

    @Override
    public String getBumpType() {
        return this.bumpType;
    }

    public void setBumpType(String bumpType) {
        if (StringUtils.equalsIgnoreCase(this.bumpType, bumpType)) {
            this.bumpType = bumpType;
            return;
        }
        boolean wasBumped = this.isBumped();
        this.bumpType = bumpType;
        boolean isBumped = this.isBumped();
        if (isBumped != wasBumped) {
            this.setBumpStaggered(isBumped);
        }
    }

    public String getBumpFallType() {
        return this.bumpFallType;
    }

    public void setBumpFallType(String val) {
        this.bumpFallType = val;
    }

    public IsoGameCharacter getBumpedChr() {
        return this.bumpedChr;
    }

    public void setBumpedChr(IsoGameCharacter bumpedChr) {
        this.bumpedChr = bumpedChr;
    }

    public long getLastBump() {
        return this.lastBump;
    }

    public void setLastBump(long lastBump) {
        this.lastBump = lastBump;
    }

    public void postAnimationFinishing() {
        this.animationFinishing = true;
    }

    public boolean isSitOnGround() {
        return this.sitOnGround;
    }

    public void setSitOnGround(boolean sitOnGround) {
        this.sitOnGround = sitOnGround;
    }

    public boolean isSittingOnFurniture() {
        return this.isSitOnFurniture;
    }

    public void setSittingOnFurniture(boolean isSittingOnFurniture) {
        this.isSitOnFurniture = isSittingOnFurniture;
    }

    public IsoObject getSitOnFurnitureObject() {
        return this.sitOnFurnitureObject;
    }

    public void setSitOnFurnitureObject(IsoObject object) {
        this.sitOnFurnitureObject = object;
    }

    public IsoDirections getSitOnFurnitureDirection() {
        return this.sitOnFurnitureDirection;
    }

    public void setSitOnFurnitureDirection(IsoDirections dir) {
        this.sitOnFurnitureDirection = dir;
    }

    public boolean isSitOnFurnitureObject(IsoObject object) {
        IsoObject sitOnObject = this.getSitOnFurnitureObject();
        if (sitOnObject == null) {
            return false;
        }
        if (object == sitOnObject) {
            return true;
        }
        return sitOnObject.isConnectedSpriteGridObject(object);
    }

    @Override
    public boolean shouldIgnoreCollisionWithSquare(IsoGridSquare square) {
        if (this.getSitOnFurnitureObject() != null && this.getSitOnFurnitureObject().getSquare() == square) {
            return true;
        }
        return this.hasPath() && this.getPathFindBehavior2().shouldIgnoreCollisionWithSquare(square);
    }

    public boolean canStandAt(float x, float y, float z) {
        int flags = 17;
        boolean bCloseToWalls = false;
        return PolygonalMap2.instance.canStandAt(x, y, PZMath.fastfloor(z), null, flags);
    }

    protected void clearAIStateMap() {
        this.aiStateMap.clear();
    }

    protected void registerAIState(String name, State aiState) {
        this.aiStateMap.put(name.toLowerCase(Locale.ENGLISH), aiState);
    }

    public State tryGetAIState(String stateName) {
        return this.aiStateMap.get(stateName.toLowerCase(Locale.ENGLISH));
    }

    public boolean isRunning() {
        if (this.getMoodles() != null && this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) >= 3) {
            return false;
        }
        return this.running;
    }

    public void setRunning(boolean bRunning) {
        this.running = bRunning;
    }

    public boolean isSprinting() {
        if (this.sprinting && !this.canSprint()) {
            return false;
        }
        return this.sprinting;
    }

    public void setSprinting(boolean bSprinting) {
        this.sprinting = bSprinting;
    }

    public boolean canSprint() {
        if (this instanceof IsoPlayer && !((IsoPlayer)this).isAllowSprint()) {
            return false;
        }
        if ("Tutorial".equals(Core.gameMode)) {
            return true;
        }
        InventoryItem item = this.getPrimaryHandItem();
        if (item != null && item.isEquippedNoSprint()) {
            return false;
        }
        item = this.getSecondaryHandItem();
        if (item != null && item.isEquippedNoSprint()) {
            return false;
        }
        return this.getMoodles() == null || this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) < 2;
    }

    public void postUpdateModelTextures() {
        this.updateModelTextures = true;
    }

    public ModelInstanceTextureCreator getTextureCreator() {
        return this.textureCreator;
    }

    public void setTextureCreator(ModelInstanceTextureCreator textureCreator) {
        this.textureCreator = textureCreator;
    }

    public void postUpdateEquippedTextures() {
        this.updateEquippedTextures = true;
    }

    public ArrayList<ModelInstance> getReadyModelData() {
        return this.readyModelData;
    }

    public boolean getIgnoreMovement() {
        return this.ignoreMovement;
    }

    public void setIgnoreMovement(boolean ignoreMovement) {
        if (this instanceof IsoPlayer && ignoreMovement) {
            ((IsoPlayer)this).networkAi.needToUpdate();
        }
        this.ignoreMovement = ignoreMovement;
    }

    public boolean isAutoWalk() {
        return this.autoWalk;
    }

    public void setAutoWalk(boolean b) {
        this.autoWalk = b;
    }

    public void setAutoWalkDirection(Vector2 v) {
        this.autoWalkDirection.set(v);
        this.autoWalkDirection.normalize();
    }

    public Vector2 getAutoWalkDirection(Vector2 out) {
        out.set(this.autoWalkDirection);
        return out;
    }

    public boolean isSneaking() {
        return this.sneaking;
    }

    public void setSneaking(boolean bSneaking) {
        this.sneaking = bSneaking;
    }

    public float getSneakLimpSpeedScale() {
        return this.sneakLimpSpeedScale;
    }

    public void setSneakLimpSpeedScale(float sneakLimpSpeedScale) {
        this.sneakLimpSpeedScale = sneakLimpSpeedScale;
    }

    public GameCharacterAIBrain getGameCharacterAIBrain() {
        return this.ai.brain;
    }

    public float getMoveDelta() {
        return this.moveDelta;
    }

    public void setMoveDelta(float moveDelta) {
        this.moveDelta = moveDelta;
    }

    public float getTurnDelta() {
        if (this.isSprinting()) {
            return this.turnDeltaSprinting;
        }
        if (this.isRunning()) {
            return this.turnDeltaRunning;
        }
        return this.turnDeltaNormal;
    }

    public void setTurnDelta(float turnDelta) {
        this.turnDeltaNormal = turnDelta;
    }

    public float getChopTreeSpeed() {
        return this.characterTraits.get(CharacterTrait.AXEMAN) ? 1.0f : 0.8f;
    }

    public boolean testDefense(IsoZombie zomb) {
        if (!this.testDotSide(zomb).equals("FRONT") || zomb.crawling || this.getSurroundingAttackingZombies() > 3) {
            return false;
        }
        int defendChance = 0;
        if ("KnifeDeath".equals(this.getVariableString("ZombieHitReaction"))) {
            defendChance += 30;
        }
        defendChance += this.getWeaponLevel() * 3;
        defendChance += this.getPerkLevel(PerkFactory.Perks.Fitness) * 2;
        defendChance += this.getPerkLevel(PerkFactory.Perks.Strength) * 2;
        defendChance -= this.getSurroundingAttackingZombies() * 5;
        defendChance -= this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) * 2;
        defendChance -= this.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) * 2;
        defendChance -= this.getMoodles().getMoodleLevel(MoodleType.TIRED) * 3;
        defendChance -= this.getMoodles().getMoodleLevel(MoodleType.DRUNK) * 2;
        if (SandboxOptions.instance.lore.strength.getValue() == 1) {
            defendChance -= 7;
        }
        if (SandboxOptions.instance.lore.strength.getValue() == 3) {
            defendChance += 7;
        }
        if (Rand.Next(100) < defendChance) {
            this.setAttackedBy(zomb);
            this.setHitReaction(zomb.getVariableString("PlayerHitReaction") + "Defended");
            return true;
        }
        return false;
    }

    public int getSurroundingAttackingZombies() {
        return this.getSurroundingAttackingZombies(false);
    }

    public int getSurroundingAttackingZombies(boolean includeCrawlers) {
        movingStatic.clear();
        IsoGridSquare sq = this.getCurrentSquare();
        if (sq == null) {
            return 0;
        }
        movingStatic.addAll(sq.getMovingObjects());
        if (sq.n != null) {
            movingStatic.addAll(sq.n.getMovingObjects());
        }
        if (sq.s != null) {
            movingStatic.addAll(sq.s.getMovingObjects());
        }
        if (sq.e != null) {
            movingStatic.addAll(sq.e.getMovingObjects());
        }
        if (sq.w != null) {
            movingStatic.addAll(sq.w.getMovingObjects());
        }
        if (sq.nw != null) {
            movingStatic.addAll(sq.nw.getMovingObjects());
        }
        if (sq.sw != null) {
            movingStatic.addAll(sq.sw.getMovingObjects());
        }
        if (sq.se != null) {
            movingStatic.addAll(sq.se.getMovingObjects());
        }
        if (sq.ne != null) {
            movingStatic.addAll(sq.ne.getMovingObjects());
        }
        int count = 0;
        for (int i = 0; i < movingStatic.size(); ++i) {
            IsoZombie zombie = Type.tryCastTo(movingStatic.get(i), IsoZombie.class);
            if (zombie == null || zombie.target != this || this.DistToSquared(zombie) >= 0.80999994f || zombie.isCrawling() && !includeCrawlers || !zombie.isCurrentState(AttackState.instance()) && !zombie.isCurrentState(AttackNetworkState.instance()) && !zombie.isCurrentState(LungeState.instance()) && !zombie.isCurrentState(LungeNetworkState.instance())) continue;
            ++count;
        }
        return count;
    }

    public boolean checkIsNearVehicle() {
        for (int i = 0; i < IsoWorld.instance.currentCell.getVehicles().size(); ++i) {
            BaseVehicle vehicle = IsoWorld.instance.currentCell.getVehicles().get(i);
            if (!(vehicle.DistTo(this) < 3.5f)) continue;
            if (this.sneaking) {
                this.setVariable("nearWallCrouching", true);
            }
            return true;
        }
        return false;
    }

    public float checkIsNearWall() {
        float result2;
        float result;
        if (!this.sneaking || this.getCurrentSquare() == null) {
            this.setVariable("nearWallCrouching", false);
            return 0.0f;
        }
        IsoGridSquare nSq = this.getCurrentSquare().getAdjacentSquare(IsoDirections.N);
        IsoGridSquare sSq = this.getCurrentSquare().getAdjacentSquare(IsoDirections.S);
        IsoGridSquare eSq = this.getCurrentSquare().getAdjacentSquare(IsoDirections.E);
        IsoGridSquare wSq = this.getCurrentSquare().getAdjacentSquare(IsoDirections.W);
        if (nSq != null && (result = nSq.getGridSneakModifier(true)) > 1.0f) {
            this.setVariable("nearWallCrouching", true);
            return result;
        }
        if (sSq != null) {
            result = sSq.getGridSneakModifier(false);
            result2 = sSq.getGridSneakModifier(true);
            if (result > 1.0f || result2 > 1.0f) {
                this.setVariable("nearWallCrouching", true);
                return result > 1.0f ? result : result2;
            }
        }
        if (eSq != null) {
            result = eSq.getGridSneakModifier(false);
            result2 = eSq.getGridSneakModifier(true);
            if (result > 1.0f || result2 > 1.0f) {
                this.setVariable("nearWallCrouching", true);
                return result > 1.0f ? result : result2;
            }
        }
        if (wSq != null) {
            result = wSq.getGridSneakModifier(false);
            result2 = wSq.getGridSneakModifier(true);
            if (result > 1.0f || result2 > 1.0f) {
                this.setVariable("nearWallCrouching", true);
                return result > 1.0f ? result : result2;
            }
        }
        if ((result = this.getCurrentSquare().getGridSneakModifier(false)) > 1.0f) {
            this.setVariable("nearWallCrouching", true);
            return result;
        }
        if (this instanceof IsoPlayer && ((IsoPlayer)this).isNearVehicle().booleanValue()) {
            this.setVariable("nearWallCrouching", true);
            return 6.0f;
        }
        this.setVariable("nearWallCrouching", false);
        return 0.0f;
    }

    public float getBeenSprintingFor() {
        return this.beenSprintingFor;
    }

    public void setBeenSprintingFor(float beenSprintingFor) {
        if (beenSprintingFor < 0.0f) {
            beenSprintingFor = 0.0f;
        }
        if (beenSprintingFor > 100.0f) {
            beenSprintingFor = 100.0f;
        }
        this.beenSprintingFor = beenSprintingFor;
    }

    public boolean isHideWeaponModel() {
        return this.hideWeaponModel;
    }

    public void setHideWeaponModel(boolean hideWeaponModel) {
        if (this.hideWeaponModel != hideWeaponModel) {
            this.hideWeaponModel = hideWeaponModel;
            this.resetEquippedHandsModels();
        }
    }

    public boolean isHideEquippedHandL() {
        return this.hideEquippedHandL;
    }

    public void setHideEquippedHandL(boolean hideEquippedHandL) {
        if (this.hideEquippedHandL != hideEquippedHandL) {
            this.hideEquippedHandL = hideEquippedHandL;
            this.resetEquippedHandsModels();
        }
    }

    public boolean isHideEquippedHandR() {
        return this.hideEquippedHandR;
    }

    public void setHideEquippedHandR(boolean hideEquippedHandR) {
        if (this.hideEquippedHandR != hideEquippedHandR) {
            this.hideEquippedHandR = hideEquippedHandR;
            this.resetEquippedHandsModels();
        }
    }

    public void setIsAiming(boolean isAiming) {
        this.isAiming = isAiming;
    }

    public void setFireMode(String fireMode) {
    }

    public String getFireMode() {
        String string;
        InventoryItem inventoryItem = this.leftHandItem;
        if (inventoryItem instanceof HandWeapon) {
            HandWeapon weapon = (HandWeapon)inventoryItem;
            string = weapon.getFireMode();
        } else {
            string = "";
        }
        return string;
    }

    @Override
    public boolean isAiming() {
        if (this.isNpc) {
            return this.NPCGetAiming();
        }
        if (this.isPerformingHostileAnimation()) {
            return true;
        }
        if (this.isIgnoringAimingInput()) {
            return false;
        }
        return this.isAiming;
    }

    @Override
    public boolean isTwisting() {
        float twist = this.getTargetTwist();
        float absTwist = PZMath.abs(twist);
        return absTwist > 1.0f;
    }

    @Override
    public boolean allowsTwist() {
        return false;
    }

    public float getShoulderTwistWeight() {
        if (this.isAiming()) {
            return 1.0f;
        }
        if (this.isSneaking()) {
            return 0.6f;
        }
        return 0.75f;
    }

    @Override
    public void resetBeardGrowingTime() {
        this.beardGrowTiming = (float)this.getHoursSurvived();
        if (GameClient.client && this instanceof IsoPlayer) {
            GameClient.instance.sendVisual((IsoPlayer)this);
        }
    }

    @Override
    public void resetHairGrowingTime() {
        this.hairGrowTiming = (float)this.getHoursSurvived();
        if (GameClient.client && this instanceof IsoPlayer) {
            GameClient.instance.sendVisual((IsoPlayer)this);
        }
    }

    public void fallenOnKnees() {
        this.fallenOnKnees(false);
    }

    public void fallenOnKnees(boolean hardFall) {
        if (GameClient.client) {
            return;
        }
        if (this.isGodMod()) {
            return;
        }
        this.helmetFall(hardFall);
        BloodBodyPartType part = BloodBodyPartType.FromIndex(Rand.Next(BloodBodyPartType.Hand_L.index(), BloodBodyPartType.Torso_Upper.index()));
        if (Rand.NextBool(2)) {
            part = BloodBodyPartType.FromIndex(Rand.Next(BloodBodyPartType.UpperLeg_L.index(), BloodBodyPartType.Back.index()));
        }
        for (int i = 0; i < 4; ++i) {
            BloodBodyPartType part2 = BloodBodyPartType.FromIndex(Rand.Next(BloodBodyPartType.Hand_L.index(), BloodBodyPartType.Torso_Upper.index()));
            if (Rand.NextBool(2)) {
                part2 = BloodBodyPartType.FromIndex(Rand.Next(BloodBodyPartType.UpperLeg_L.index(), BloodBodyPartType.Back.index()));
            }
            this.addDirt(part2, Rand.Next(2, 6), false);
        }
        if (DebugOptions.instance.character.debug.alwaysTripOverFence.getValue()) {
            this.dropHandItems();
        }
        if (!Rand.NextBool(4 + this.getPerkLevel(PerkFactory.Perks.Nimble))) {
            return;
        }
        if (Rand.NextBool(4)) {
            this.dropHandItems();
        }
        this.addHole(part);
        BodyPart bodyPart = this.getBodyDamage().getBodyPart(BodyPartType.FromIndex(part.index()));
        float defense = this.getBodyPartClothingDefense(part.index(), false, false);
        if ((float)Rand.Next(100) >= defense) {
            this.addBlood(part, true, false, false);
            if (bodyPart.scratched()) {
                bodyPart.generateDeepWound();
                IsoGameCharacter isoGameCharacter = this;
                if (isoGameCharacter instanceof IsoPlayer) {
                    IsoPlayer player = (IsoPlayer)isoGameCharacter;
                    if (GameServer.server) {
                        player.getNetworkCharacterAI().syncDamage();
                        INetworkPacket.sendToRelative(PacketTypes.PacketType.PlaySound, this.getXi(), (float)this.getYi(), "PainFromLacerate", (byte)2, this);
                    } else {
                        player.playerVoiceSound("PainFromLacerate");
                    }
                }
            } else {
                bodyPart.setScratched(true, true);
                IsoGameCharacter isoGameCharacter = this;
                if (isoGameCharacter instanceof IsoPlayer) {
                    IsoPlayer player = (IsoPlayer)isoGameCharacter;
                    if (GameServer.server) {
                        player.getNetworkCharacterAI().syncDamage();
                        INetworkPacket.sendToRelative(PacketTypes.PacketType.PlaySound, this.getXi(), (float)this.getYi(), "PainFromScratch", (byte)2, this);
                    } else {
                        player.playerVoiceSound("PainFromScratch");
                    }
                }
            }
        }
    }

    public void addVisualDamage(String itemType) {
        this.addBodyVisualFromItemType("Base." + itemType);
    }

    public ItemVisual addBodyVisualFromItemType(String itemType) {
        IsoGameCharacter isoGameCharacter = this;
        if (!(isoGameCharacter instanceof IHumanVisual)) {
            return null;
        }
        IHumanVisual iHumanVisual = (IHumanVisual)((Object)isoGameCharacter);
        Item scriptItem = ScriptManager.instance.getItem(itemType);
        if (scriptItem == null) {
            return null;
        }
        ClothingItem clothingItem = scriptItem.getClothingItemAsset();
        if (clothingItem == null) {
            return null;
        }
        ClothingItemReference itemRef = new ClothingItemReference();
        itemRef.itemGuid = clothingItem.guid;
        itemRef.randomize();
        ItemVisual itemVisual = new ItemVisual();
        itemVisual.setItemType(itemType);
        itemVisual.synchWithOutfit(itemRef);
        if (!this.isDuplicateBodyVisual(itemVisual)) {
            ItemVisuals itemVisuals = iHumanVisual.getHumanVisual().getBodyVisuals();
            itemVisuals.add(itemVisual);
            return itemVisual;
        }
        return null;
    }

    protected boolean isDuplicateBodyVisual(ItemVisual itemVisual) {
        IsoGameCharacter isoGameCharacter = this;
        if (!(isoGameCharacter instanceof IHumanVisual)) {
            return false;
        }
        IHumanVisual iHumanVisual = (IHumanVisual)((Object)isoGameCharacter);
        ItemVisuals itemVisuals = iHumanVisual.getHumanVisual().getBodyVisuals();
        for (int i = 0; i < itemVisuals.size(); ++i) {
            ItemVisual itemVisual2 = (ItemVisual)itemVisuals.get(i);
            if (!itemVisual.getClothingItemName().equals(itemVisual2.getClothingItemName()) || itemVisual.getTextureChoice() != itemVisual2.getTextureChoice() || itemVisual.getBaseTexture() != itemVisual2.getBaseTexture()) continue;
            return true;
        }
        return false;
    }

    public boolean isCriticalHit() {
        return this.isCrit;
    }

    public void setCriticalHit(boolean isCrit) {
        this.isCrit = isCrit;
    }

    public float getRunSpeedModifier() {
        return this.runSpeedModifier;
    }

    public boolean isNPC() {
        return this.isNpc;
    }

    public void setNPC(boolean newvalue) {
        this.ai.setNPC(newvalue);
        this.isNpc = newvalue;
    }

    public void NPCSetRunning(boolean newvalue) {
        this.ai.brain.humanControlVars.running = newvalue;
    }

    public boolean NPCGetRunning() {
        return this.ai.brain.humanControlVars.running;
    }

    public void NPCSetJustMoved(boolean newvalue) {
        this.ai.brain.humanControlVars.justMoved = newvalue;
    }

    public void NPCSetAiming(boolean isAiming) {
        this.ai.brain.humanControlVars.aiming = isAiming;
    }

    public boolean NPCGetAiming() {
        return this.ai.brain.humanControlVars.aiming;
    }

    public void NPCSetAttack(boolean newvalue) {
        this.ai.brain.humanControlVars.initiateAttack = newvalue;
    }

    public void NPCSetMelee(boolean newvalue) {
        this.ai.brain.humanControlVars.melee = newvalue;
    }

    public void setMetabolicTarget(Metabolics m) {
        if (m != null) {
            this.setMetabolicTarget(m.getMet());
        }
    }

    public void setMetabolicTarget(float target) {
        if (this.getBodyDamage() != null && this.getBodyDamage().getThermoregulator() != null) {
            this.getBodyDamage().getThermoregulator().setMetabolicTarget(target);
        }
    }

    public double getThirstMultiplier() {
        if (this.getBodyDamage() != null && this.getBodyDamage().getThermoregulator() != null) {
            return this.getBodyDamage().getThermoregulator().getFluidsMultiplier();
        }
        return 1.0;
    }

    public double getHungerMultiplier() {
        return 1.0;
    }

    public double getFatiqueMultiplier() {
        if (this.getBodyDamage() != null && this.getBodyDamage().getThermoregulator() != null) {
            return this.getBodyDamage().getThermoregulator().getFatigueMultiplier();
        }
        return 1.0;
    }

    public float getTimedActionTimeModifier() {
        return 1.0f;
    }

    public boolean addHoleFromZombieAttacks(BloodBodyPartType part, boolean scratch) {
        InventoryItem inventoryItem;
        this.getItemVisuals(tempItemVisuals);
        ItemVisual itemHit = null;
        for (int i = tempItemVisuals.size() - 1; i >= 0; --i) {
            ArrayList<BloodClothingType> types;
            ItemVisual itemVisual = (ItemVisual)tempItemVisuals.get(i);
            Item scriptItem = itemVisual.getScriptItem();
            if (scriptItem == null || (types = scriptItem.getBloodClothingType()) == null) continue;
            ArrayList<BloodBodyPartType> coveredParts = BloodClothingType.getCoveredParts(types);
            for (int j = 0; j < coveredParts.size(); ++j) {
                BloodBodyPartType bloodClothingType = coveredParts.get(j);
                if (part != bloodClothingType) continue;
                itemHit = itemVisual;
                break;
            }
            if (itemHit != null) break;
        }
        float baseDef = 0.0f;
        boolean result = false;
        if (itemHit != null && itemHit.getInventoryItem() != null && (inventoryItem = itemHit.getInventoryItem()) instanceof Clothing) {
            Clothing clothing = (Clothing)inventoryItem;
            baseDef = Math.max(30.0f, 100.0f - clothing.getDefForPart(part, !scratch, false) / 1.5f);
        }
        if ((float)Rand.Next(100) < baseDef) {
            boolean addedHole = this.addHole(part);
            if (addedHole) {
                this.getEmitter().playSoundImpl("ZombieRipClothing", null);
            }
            result = true;
        }
        return result;
    }

    protected void updateBandages() {
        if (this instanceof IsoAnimal) {
            return;
        }
        s_bandages.update(this);
    }

    public float getTotalBlood() {
        float result = 0.0f;
        if (this.getWornItems() == null) {
            return result;
        }
        for (int i = 0; i < this.getWornItems().size(); ++i) {
            InventoryItem item = this.getWornItems().get(i).getItem();
            if (!(item instanceof Clothing)) continue;
            Clothing clothing = (Clothing)item;
            result += clothing.getBloodlevel();
        }
        if (this.getPrimaryHandItem() != null && !this.getWornItems().contains(this.getPrimaryHandItem())) {
            result += this.getPrimaryHandItem().getBloodLevelAdjustedHigh();
        }
        if (this.getSecondaryHandItem() != null && this.getPrimaryHandItem() != this.getSecondaryHandItem() && !this.getWornItems().contains(this.getSecondaryHandItem())) {
            result += this.getSecondaryHandItem().getBloodLevelAdjustedHigh();
        }
        return result += ((HumanVisual)this.getVisual()).getTotalBlood();
    }

    public void attackFromWindowsLunge(IsoZombie zombie) {
        if (this.lungeFallTimer > 0.0f || PZMath.fastfloor(this.getZ()) != PZMath.fastfloor(zombie.getZ()) || zombie.isDead() || this.getCurrentSquare() == null || this.getCurrentSquare().isDoorBlockedTo(zombie.getCurrentSquare()) || this.getCurrentSquare().isWallTo(zombie.getCurrentSquare()) || this.getCurrentSquare().isWindowTo(zombie.getCurrentSquare())) {
            return;
        }
        if (this.getVehicle() != null) {
            return;
        }
        boolean hit = this.DoSwingCollisionBoneCheck(zombie, zombie.getAnimationPlayer().getSkinningBoneIndex("Bip01_R_Hand", -1), 1.0f);
        if (!hit) {
            return;
        }
        zombie.playSound("ZombieCrawlLungeHit");
        this.lungeFallTimer = 200.0f;
        this.setIsAiming(false);
        boolean fall = false;
        int fallChance = 30;
        fallChance += this.getMoodles().getMoodleLevel(MoodleType.DRUNK) * 3;
        fallChance += this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) * 3;
        fallChance += this.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) * 5;
        fallChance -= this.getPerkLevel(PerkFactory.Perks.Fitness) * 2;
        fallChance -= this.getPerkLevel(PerkFactory.Perks.Nimble);
        BodyPart part = this.getBodyDamage().getBodyPart(BodyPartType.Torso_Lower);
        if (part.getAdditionalPain(true) > 20.0f) {
            fallChance = (int)((float)fallChance + (part.getAdditionalPain(true) - 20.0f) / 10.0f);
        }
        if (this.characterTraits.get(CharacterTrait.CLUMSY)) {
            fallChance += 10;
        }
        if (this.characterTraits.get(CharacterTrait.GRACEFUL)) {
            fallChance -= 10;
        }
        if (this.characterTraits.get(CharacterTrait.VERY_UNDERWEIGHT)) {
            fallChance += 20;
        }
        if (this.characterTraits.get(CharacterTrait.VERY_UNDERWEIGHT)) {
            fallChance += 10;
        }
        if (this.characterTraits.get(CharacterTrait.OBESE)) {
            fallChance -= 10;
        }
        if (this.characterTraits.get(CharacterTrait.OVERWEIGHT)) {
            fallChance -= 5;
        }
        fallChance = Math.max(5, fallChance);
        this.clearVariable("BumpFallType");
        this.setBumpType("stagger");
        if (Rand.Next(100) < fallChance) {
            fall = true;
        }
        this.setBumpDone(false);
        this.setBumpFall(fall);
        if (zombie.isBehind(this)) {
            this.setBumpFallType("pushedBehind");
        } else {
            this.setBumpFallType("pushedFront");
        }
        this.actionContext.reportEvent("wasBumped");
    }

    public boolean DoSwingCollisionBoneCheck(IsoGameCharacter zombie, int bone, float tempoLengthTest) {
        Model.boneToWorldCoords(zombie, bone, tempVectorBonePos);
        float distSq = IsoUtils.DistanceToSquared(IsoGameCharacter.tempVectorBonePos.x, IsoGameCharacter.tempVectorBonePos.y, this.getX(), this.getY());
        return distSq < tempoLengthTest * tempoLengthTest;
    }

    public boolean isInvincible() {
        return this.invincible;
    }

    public void setInvincible(boolean invincible) {
        if (!Role.hasCapability(this, Capability.ToggleInvincibleHimself)) {
            this.invincible = false;
            return;
        }
        this.invincible = invincible;
    }

    public BaseVehicle getNearVehicle() {
        if (this.getVehicle() != null) {
            return null;
        }
        int chunkMinX = (PZMath.fastfloor(this.getX()) - 4) / 8 - 1;
        int chunkMinY = (PZMath.fastfloor(this.getY()) - 4) / 8 - 1;
        int chunkMaxX = (int)Math.ceil((this.getX() + 4.0f) / 8.0f) + 1;
        int chunkMaxY = (int)Math.ceil((this.getY() + 4.0f) / 8.0f) + 1;
        for (int cy = chunkMinY; cy < chunkMaxY; ++cy) {
            for (int cx = chunkMinX; cx < chunkMaxX; ++cx) {
                IsoChunk chunk;
                IsoChunk isoChunk = chunk = GameServer.server ? ServerMap.instance.getChunk(cx, cy) : IsoWorld.instance.currentCell.getChunk(cx, cy);
                if (chunk == null) continue;
                for (int i = 0; i < chunk.vehicles.size(); ++i) {
                    BaseVehicle vehicle = chunk.vehicles.get(i);
                    if (vehicle.getScript() == null || PZMath.fastfloor(this.getZ()) != PZMath.fastfloor(vehicle.getZ()) || this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer() && vehicle.getTargetAlpha(((IsoPlayer)this).playerIndex) == 0.0f || this.DistToSquared(vehicle) >= 16.0f) continue;
                    return vehicle;
                }
            }
        }
        return null;
    }

    public boolean isNearSirenVehicle() {
        if (this.getVehicle() != null) {
            return false;
        }
        int chunkMinX = (PZMath.fastfloor(this.getX()) - 5) / 8 - 1;
        int chunkMinY = (PZMath.fastfloor(this.getY()) - 5) / 8 - 1;
        int chunkMaxX = (int)Math.ceil((this.getX() + 5.0f) / 8.0f) + 1;
        int chunkMaxY = (int)Math.ceil((this.getY() + 5.0f) / 8.0f) + 1;
        for (int cy = chunkMinY; cy < chunkMaxY; ++cy) {
            for (int cx = chunkMinX; cx < chunkMaxX; ++cx) {
                IsoChunk chunk;
                IsoChunk isoChunk = chunk = GameServer.server ? ServerMap.instance.getChunk(cx, cy) : IsoWorld.instance.currentCell.getChunk(cx, cy);
                if (chunk == null) continue;
                for (int i = 0; i < chunk.vehicles.size(); ++i) {
                    BaseVehicle vehicle = chunk.vehicles.get(i);
                    if (vehicle.getScript() == null || PZMath.fastfloor(this.getZ()) != PZMath.fastfloor(vehicle.getZ()) || this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer() && vehicle.getTargetAlpha(((IsoPlayer)this).playerIndex) == 0.0f || this.DistToSquared(PZMath.fastfloor(vehicle.getX()), PZMath.fastfloor(vehicle.getY())) >= 25.0f || !vehicle.isSirening()) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private IsoGridSquare getSolidFloorAt(int x, int y, int z) {
        while (z >= 0) {
            IsoGridSquare sq = this.getCell().getGridSquare(x, y, z);
            if (sq != null && sq.TreatAsSolidFloor()) {
                return sq;
            }
            --z;
        }
        return null;
    }

    public void dropHeavyItems() {
        IsoGridSquare sq = this.getCurrentSquare();
        if (sq == null) {
            return;
        }
        InventoryItem item1 = this.getPrimaryHandItem();
        InventoryItem item2 = this.getSecondaryHandItem();
        if (item1 == null && item2 == null) {
            return;
        }
        sq = this.getSolidFloorAt(sq.x, sq.y, sq.z);
        if (sq == null) {
            return;
        }
        if (this.isHeavyItem(item1)) {
            this.setPrimaryHandItem(null);
            if (!GameClient.client) {
                this.getInventory().DoRemoveItem(item1);
                float dropX = Rand.Next(0.1f, 0.9f);
                float dropY = Rand.Next(0.1f, 0.9f);
                float dropZ = sq.getApparentZ(dropX, dropY) - (float)sq.getZ();
                sq.AddWorldInventoryItem(item1, dropX, dropY, dropZ);
                LuaEventManager.triggerEvent("OnContainerUpdate");
            }
            LuaEventManager.triggerEvent("onItemFall", item1);
            this.playDropItemSound(item1);
        }
        if (this.isHeavyItem(item2)) {
            boolean inBothHand;
            this.setSecondaryHandItem(null);
            boolean bl = inBothHand = item1 == item2;
            if (!inBothHand) {
                if (!GameClient.client) {
                    this.getInventory().DoRemoveItem(item2);
                    float dropX = Rand.Next(0.1f, 0.9f);
                    float dropY = Rand.Next(0.1f, 0.9f);
                    float dropZ = sq.getApparentZ(dropX, dropY) - (float)sq.getZ();
                    sq.AddWorldInventoryItem(item2, dropX, dropY, dropZ);
                    LuaEventManager.triggerEvent("OnContainerUpdate");
                }
                LuaEventManager.triggerEvent("onItemFall", item2);
                this.playDropItemSound(item2);
            }
        }
        if (GameClient.client && this.isLocal()) {
            INetworkPacket.send(PacketTypes.PacketType.PlayerDropHeldItems, this, sq.x, sq.y, sq.z, true);
            INetworkPacket.send(PacketTypes.PacketType.Equip, this);
        }
    }

    public boolean isHeavyItem(InventoryItem item) {
        if (item == null) {
            return false;
        }
        if (item instanceof InventoryContainer) {
            return true;
        }
        if (item.hasTag(ItemTag.HEAVY_ITEM)) {
            return true;
        }
        if ("CorpseMale".equals(item.getType()) || "CorpseFemale".equals(item.getType()) || "Animal".equals(item.getType()) || "CorpseAnimal".equals(item.getType())) {
            return true;
        }
        return item.getType().equals("Generator");
    }

    public boolean isCanShout() {
        return this.canShout;
    }

    public void setCanShout(boolean canShout) {
        this.canShout = canShout;
    }

    public boolean isKnowAllRecipes() {
        return this.getCheats().isSet(CheatType.KNOW_ALL_RECIPES);
    }

    public void setKnowAllRecipes(boolean knowAllRecipes) {
        if (!Role.hasCapability(this, Capability.ToggleKnowAllRecipes)) {
            this.getCheats().set(CheatType.KNOW_ALL_RECIPES, false);
            return;
        }
        this.getCheats().set(CheatType.KNOW_ALL_RECIPES, knowAllRecipes);
    }

    public boolean isUnlimitedAmmo() {
        return this.getCheats().isSet(CheatType.UNLIMITED_AMMO);
    }

    public void setUnlimitedAmmo(boolean unlimitedAmmo) {
        if (!Role.hasCapability(this, Capability.ToggleUnlimitedAmmo)) {
            this.getCheats().set(CheatType.UNLIMITED_AMMO, false);
            return;
        }
        this.getCheats().set(CheatType.UNLIMITED_AMMO, unlimitedAmmo);
    }

    public boolean isUnlimitedEndurance() {
        return this.getCheats().isSet(CheatType.UNLIMITED_ENDURANCE);
    }

    public void setUnlimitedEndurance(boolean unlimitedEndurance) {
        if (!Role.hasCapability(this, Capability.ToggleUnlimitedEndurance)) {
            this.getCheats().set(CheatType.UNLIMITED_ENDURANCE, false);
            return;
        }
        this.getCheats().set(CheatType.UNLIMITED_ENDURANCE, unlimitedEndurance);
    }

    private void addActiveLightItem(InventoryItem item, ArrayList<InventoryItem> items) {
        if (item != null && item.isEmittingLight() && !items.contains(item)) {
            items.add(item);
        }
    }

    public ArrayList<InventoryItem> getActiveLightItems(ArrayList<InventoryItem> items) {
        this.addActiveLightItem(this.getSecondaryHandItem(), items);
        this.addActiveLightItem(this.getPrimaryHandItem(), items);
        AttachedItems attachedItems = this.getAttachedItems();
        for (int i = 0; i < attachedItems.size(); ++i) {
            InventoryItem item = attachedItems.getItemByIndex(i);
            this.addActiveLightItem(item, items);
        }
        return items;
    }

    public SleepingEventData getOrCreateSleepingEventData() {
        if (this.sleepingEventData == null) {
            this.sleepingEventData = new SleepingEventData();
        }
        return this.sleepingEventData;
    }

    public void playEmote(String emote) {
        this.setVariable("emote", emote);
        this.setVariable("EmotePlaying", true);
        this.actionContext.reportEvent("EventEmote");
        if (GameClient.client) {
            StateManager.enterState(this, PlayerEmoteState.instance());
        }
    }

    public String getAnimationStateName() {
        return this.advancedAnimator.getCurrentStateName();
    }

    public String getActionStateName() {
        return this.actionContext.getCurrentStateName();
    }

    public boolean shouldWaitToStartTimedAction() {
        if (this.isSitOnGround()) {
            if (this.getCurrentState().equals(FishingState.instance())) {
                return false;
            }
            AdvancedAnimator aa = this.getAdvancedAnimator();
            if (aa.getRootLayer() == null) {
                return false;
            }
            if (aa.animSet == null || !aa.animSet.containsState("sitonground")) {
                return false;
            }
            AnimState as = aa.animSet.GetState("sitonground");
            if (!PZArrayUtil.contains(as.nodes, node -> "sit_action".equalsIgnoreCase(node.name))) {
                return false;
            }
            LiveAnimNode lan0 = PZArrayUtil.find(aa.getRootLayer().getLiveAnimNodes(), lan -> lan.isActive() && "sit_action".equalsIgnoreCase(lan.getName()));
            return lan0 == null || !lan0.isMainAnimActive();
        }
        return false;
    }

    public void setPersistentOutfitID(int outfitID) {
        this.setPersistentOutfitID(outfitID, false);
    }

    public void setPersistentOutfitID(int outfitID, boolean init) {
        this.persistentOutfitId = outfitID;
        this.persistentOutfitInit = init;
    }

    public int getPersistentOutfitID() {
        return this.persistentOutfitId;
    }

    public boolean isPersistentOutfitInit() {
        return this.persistentOutfitInit;
    }

    @Override
    public boolean isDoingActionThatCanBeCancelled() {
        return false;
    }

    public boolean isDoDeathSound() {
        return this.doDeathSound;
    }

    public void setDoDeathSound(boolean doDeathSound) {
        this.doDeathSound = doDeathSound;
    }

    @Override
    public boolean isKilledByFall() {
        return this.killedByFall;
    }

    @Override
    public void setKilledByFall(boolean killedByFall) {
        this.killedByFall = killedByFall;
    }

    public void updateEquippedRadioFreq() {
        int i;
        this.invRadioFreq.clear();
        for (i = 0; i < this.getInventory().getItems().size(); ++i) {
            Radio radio;
            InventoryItem item = this.getInventory().getItems().get(i);
            if (!(item instanceof Radio) || (radio = (Radio)item).getDeviceData() == null || !radio.getDeviceData().getIsTurnedOn() || radio.getDeviceData().getMicIsMuted() || this.invRadioFreq.contains(radio.getDeviceData().getChannel())) continue;
            this.invRadioFreq.add(radio.getDeviceData().getChannel());
        }
        for (i = 0; i < this.invRadioFreq.size(); ++i) {
            System.out.println(this.invRadioFreq.get(i));
        }
        if (this instanceof IsoPlayer && GameClient.client) {
            GameClient.sendEquippedRadioFreq((IsoPlayer)this);
        }
    }

    public void updateEquippedItemSounds() {
        WornItems wornItems1;
        if (this.leftHandItem != null) {
            this.leftHandItem.updateEquippedAndActivatedSound();
        }
        if (this.rightHandItem != null) {
            this.rightHandItem.updateEquippedAndActivatedSound();
        }
        if ((wornItems1 = this.getWornItems()) == null) {
            return;
        }
        for (int i = 0; i < wornItems1.size(); ++i) {
            InventoryItem item = wornItems1.getItemByIndex(i);
            item.updateEquippedAndActivatedSound();
        }
    }

    @Override
    public FMODParameterList getFMODParameters() {
        return this.fmodParameters;
    }

    @Override
    public void startEvent(long eventInstance, GameSoundClip clip, BitSet parameterSet) {
        FMODParameterList myParameters = this.getFMODParameters();
        ArrayList<FMOD_STUDIO_PARAMETER_DESCRIPTION> eventParameters = clip.eventDescription.parameters;
        for (int i = 0; i < eventParameters.size(); ++i) {
            FMODParameter fmodParameter;
            FMOD_STUDIO_PARAMETER_DESCRIPTION eventParameter = eventParameters.get(i);
            if (parameterSet.get(eventParameter.globalIndex) || (fmodParameter = myParameters.get(eventParameter)) == null) continue;
            fmodParameter.startEventInstance(eventInstance);
        }
    }

    @Override
    public void updateEvent(long eventInstance, GameSoundClip clip) {
    }

    @Override
    public void stopEvent(long eventInstance, GameSoundClip clip, BitSet parameterSet) {
        FMODParameterList myParameters = this.getFMODParameters();
        ArrayList<FMOD_STUDIO_PARAMETER_DESCRIPTION> eventParameters = clip.eventDescription.parameters;
        for (int i = 0; i < eventParameters.size(); ++i) {
            FMODParameter fmodParameter;
            FMOD_STUDIO_PARAMETER_DESCRIPTION eventParameter = eventParameters.get(i);
            if (parameterSet.get(eventParameter.globalIndex) || (fmodParameter = myParameters.get(eventParameter)) == null) continue;
            fmodParameter.stopEventInstance(eventInstance);
        }
    }

    public void playBloodSplatterSound() {
        this.getEmitter().playSoundImpl("BloodSplatter", this);
    }

    public void setIgnoreAimingInput(boolean b) {
        this.ignoreAimingInput = b;
    }

    public boolean isIgnoringAimingInput() {
        return this.ignoreAimingInput;
    }

    public void setHeadLookAround(boolean b) {
        this.headLookAround = b;
    }

    public boolean isHeadLookAround() {
        return this.headLookAround;
    }

    public void setHeadLookAroundDirection(float lookHorizontal, float lookVertical) {
        this.headLookHorizontal = PZMath.clamp(lookHorizontal, -1.0f, 1.0f);
        this.headLookVertical = PZMath.clamp(lookVertical, -1.0f, 1.0f);
    }

    public float getHeadLookHorizontal() {
        return this.headLookHorizontal;
    }

    public float getHeadLookVertical() {
        return this.headLookVertical;
    }

    public float getHeadLookAngleMax() {
        return 0.348f;
    }

    public void addBloodFromVehicleImpact(float speed) {
        if ((float)Rand.Next(10) > speed) {
            return;
        }
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
            this.playBloodSplatterSound();
        }
    }

    public boolean isKnockedDown() {
        return this.knockedDown;
    }

    public void setKnockedDown(boolean knockedDown) {
        this.knockedDown = knockedDown;
    }

    public boolean isStaggerBack() {
        return false;
    }

    public void readInventory(ByteBufferReader b) {
        try {
            ArrayList<InventoryItem> savedItems = this.getInventory().load(b.bb, IsoWorld.getWorldVersion());
            int wornItemCount = b.getByte();
            for (int i = 0; i < wornItemCount; ++i) {
                ItemBodyLocation itemBodyLocation = ItemBodyLocation.get(ResourceLocation.of(b.getUTF()));
                short index = b.getShort();
                if (index < 0 || index >= savedItems.size() || this.getBodyLocationGroup().getLocation(itemBodyLocation) == null) continue;
                this.getWornItems().setItem(itemBodyLocation, savedItems.get(index));
                savedItems.get(index).synchWithVisual();
            }
            int attachedItemsCount = b.getByte();
            for (int i = 0; i < attachedItemsCount; ++i) {
                String location = b.getUTF();
                short index = b.getShort();
                if (index < 0 || index >= savedItems.size() || this.getAttachedLocationGroup().getLocation(location) == null) continue;
                this.getAttachedItems().setItem(location, savedItems.get(index));
            }
        }
        catch (IOException e) {
            DebugLog.Multiplayer.printException(e, "ReadInventory error id=" + this.getOnlineID(), LogSeverity.Error);
        }
    }

    public void Kill(HandWeapon handWeapon, IsoGameCharacter killer) {
        this.setAttackedBy(killer);
        this.setHealth(0.0f);
        this.setOnKillDone(true);
    }

    public void Kill(IsoGameCharacter killer) {
        this.setAttackedBy(killer);
        this.setHealth(0.0f);
        if (this.getBodyDamage() != null) {
            this.getBodyDamage().setOverallBodyHealth(0.0f);
        }
        this.setOnKillDone(true);
    }

    public void die() {
        if (this.isOnDeathDone()) {
            return;
        }
        if (GameClient.client) {
            this.getNetworkCharacterAI().becomeCorpse();
        } else {
            this.becomeCorpse();
        }
    }

    public IsoDeadBody becomeCorpse() {
        this.Kill(this.getAttackedBy());
        this.setOnDeathDone(true);
        if (this.isBeingGrappled()) {
            IGrappleable grappledBy = this.getGrappledBy();
            if (grappledBy != null) {
                grappledBy.LetGoOfGrappled("GrappledDied");
            }
        } else if (this.isGrappling()) {
            this.LetGoOfGrappled("GrapplerDied");
        }
        return null;
    }

    public InventoryItem becomeCorpseItem(ItemContainer placeInContainer) {
        boolean canHumanCorpseFit = placeInContainer.canHumanCorpseFit();
        if (!canHumanCorpseFit) {
            DebugType.General.warn("Character's corpse cannot fit in this container: '%s'. Remaining storage space: %f. Required: %f. Character: '%s'", placeInContainer, Float.valueOf(placeInContainer.getAvailableWeightCapacity()), IsoGameCharacter.getWeightAsCorpse(), this);
            return null;
        }
        IsoDeadBody deadBody = this.becomeCorpse();
        if (deadBody == null) {
            DebugType.General.warn("Failed to become an IsoDeadBody.");
            return null;
        }
        InventoryItem inventoryItem = deadBody.becomeCorpseItem();
        if (inventoryItem == null) {
            DebugType.General.warn("Failed to spawn corpse item: %s", deadBody);
            return null;
        }
        if (!placeInContainer.canItemFit(inventoryItem)) {
            DebugType.General.error("Item cannot fit in inventory. Item: '%s', Inventory: '%s'. Remaining storage space: %f. Required: %f", inventoryItem.getFullType(), placeInContainer.getType(), Float.valueOf(placeInContainer.getAvailableWeightCapacity()), Float.valueOf(inventoryItem.getUnequippedWeight()));
            return null;
        }
        placeInContainer.addItem(inventoryItem);
        return inventoryItem;
    }

    public float getMass() {
        return 100.0f;
    }

    public static int getWeightAsCorpse() {
        return 20;
    }

    public HitReactionNetworkAI getHitReactionNetworkAI() {
        return null;
    }

    public NetworkCharacterAI getNetworkCharacterAI() {
        return null;
    }

    public boolean wasLocal() {
        return this.getNetworkCharacterAI() == null || this.getNetworkCharacterAI().wasLocal();
    }

    public boolean isLocal() {
        return !GameClient.client && !GameServer.server;
    }

    public boolean isNetworkVehicleCollisionActive(BaseVehicle testVehicle) {
        return testVehicle != null && !this.isLocal();
    }

    public void doNetworkHitByVehicle(BaseVehicle baseVehicle, BaseVehicle.HitVars hitVars) {
        if (GameClient.client) {
            IsoPlayer driver = GameClient.IDToPlayerMap.get(baseVehicle.getNetPlayerId());
            if (driver == null) {
                return;
            }
            if (driver.isLocal()) {
                GameClient.sendVehicleHit(driver, this, baseVehicle, hitVars.damageToPedestrian, hitVars.isTargetHitFromBehind, hitVars.vehicleDamage, hitVars.hitSpeed, hitVars.isVehicleHitFromFront);
            } else {
                this.getNetworkCharacterAI().hitByVehicle();
            }
        }
    }

    public boolean isSkipResolveCollision() {
        return this.isBeingGrappled();
    }

    public boolean isPerformingAttackAnimation() {
        return this.isPerformingAttackAnim;
    }

    public void setPerformingAttackAnimation(boolean attackAnim) {
        this.isPerformingAttackAnim = attackAnim;
    }

    public boolean isPerformingShoveAnimation() {
        return this.isPerformingShoveAnim;
    }

    public void setPerformingShoveAnimation(boolean shoveAnim) {
        this.isPerformingShoveAnim = shoveAnim;
    }

    public boolean isPerformingStompAnimation() {
        return this.isPerformingStompAnim;
    }

    public void setPerformingStompAnimation(boolean stompAnim) {
        this.isPerformingStompAnim = stompAnim;
    }

    public boolean isPerformingHostileAnimation() {
        return this.isPerformingAttackAnimation() || this.isPerformingShoveAnimation() || this.isPerformingStompAnimation() || this.isPerformingGrappleGrabAnimation();
    }

    public Float getNextAnimationTranslationLength() {
        ActionContext actionContext = this.getActionContext();
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        AdvancedAnimator advancedAnimator = this.getAdvancedAnimator();
        if (actionContext == null || animationPlayer == null || advancedAnimator == null) {
            return null;
        }
        ActionState actionState = actionContext.peekNextState();
        if (actionState == null || StringUtils.isNullOrEmpty(actionState.getName())) {
            return null;
        }
        AnimationSet animSet = advancedAnimator.animSet;
        AnimState animState = animSet.GetState(actionState.getName());
        SkinningData skinningData = animationPlayer.getSkinningData();
        ArrayList<AnimNode> nodes = new ArrayList<AnimNode>();
        animState.getAnimNodes(this, nodes);
        for (AnimNode node : nodes) {
            AnimationClip clip;
            if (StringUtils.isNullOrEmpty(node.animName) || (clip = skinningData.animationClips.get(node.animName)) == null) continue;
            float length = clip.getTranslationLength(node.deferredBoneAxis);
            float time = AdvancedAnimator.motionScale * clip.getDuration();
            return Float.valueOf(length * time);
        }
        return null;
    }

    public Float calcHitDir(IsoGameCharacter wielder, HandWeapon weapon, Vector2 out) {
        Float length = this.getNextAnimationTranslationLength();
        out.set(this.getX() - wielder.getX(), this.getY() - wielder.getY()).normalize();
        if (length == null) {
            out.setLength(this.getHitForce() * 0.1f);
            out.scale(weapon.getPushBackMod());
            out.rotate(weapon.hitAngleMod);
        } else {
            out.scale(length.floatValue());
        }
        return null;
    }

    public void calcHitDir(Vector2 out) {
        out.set(this.getHitDir());
        out.setLength(this.getHitForce());
    }

    @Override
    public Safety getSafety() {
        return this.safety;
    }

    @Override
    public void setSafety(Safety safety) {
        this.safety.copyFrom(safety);
    }

    public void burnCorpse(IsoDeadBody corpse) {
        if (GameClient.client) {
            INetworkPacket.send(PacketTypes.PacketType.BurnCorpse, this, corpse.getObjectID());
            return;
        }
        IsoFireManager.StartFire(corpse.getCell(), corpse.getSquare(), true, 100, 700);
    }

    public void setIsAnimal(boolean v) {
        this.animal = v;
    }

    public boolean isAnimal() {
        return this.animal;
    }

    public boolean isAnimalRunningToDeathPosition() {
        return false;
    }

    @Override
    public float getPerkToUnit(PerkFactory.Perk perk) {
        int perkLevel = this.getPerkLevel(perk);
        if (perkLevel == 10) {
            return 1.0f;
        }
        float xpNextLevel = perk.getXpForLevel(perkLevel + 1);
        float xp = this.getXp().getXP(perk);
        float subtractXp = 0.0f;
        if (perkLevel > 0) {
            subtractXp = perk.getTotalXpForLevel(perkLevel);
        }
        float perkProgress = (float)perkLevel * 0.1f + (xp -= subtractXp) / xpNextLevel * 0.1f;
        return PZMath.clamp(perkProgress, 0.0f, 1.0f);
    }

    @Override
    public HashMap<String, Integer> getReadLiterature() {
        return this.readLiterature;
    }

    @Override
    public boolean isLiteratureRead(String name) {
        HashMap<String, Integer> list = this.getReadLiterature();
        if (list.containsKey(name)) {
            int dayRead = list.get(name);
            int currentDay = GameTime.getInstance().getNightsSurvived();
            return currentDay < dayRead + SandboxOptions.getInstance().literatureCooldown.getValue();
        }
        return false;
    }

    @Override
    public void addReadLiterature(String name) {
        if (this.isLiteratureRead(name)) {
            return;
        }
        HashMap<String, Integer> list = this.getReadLiterature();
        int day = GameTime.getInstance().getNightsSurvived();
        list.put(name, day);
    }

    @Override
    public void addReadLiterature(String name, int day) {
        if (this.isLiteratureRead(name)) {
            return;
        }
        HashMap<String, Integer> list = this.getReadLiterature();
        list.put(name, day);
    }

    @Override
    public void addReadPrintMedia(String mediaId) {
        if (StringUtils.isNullOrWhitespace(mediaId)) {
            return;
        }
        if (this.readPrintMedia.contains(mediaId)) {
            return;
        }
        this.readPrintMedia.add(mediaId);
    }

    @Override
    public boolean isPrintMediaRead(String mediaId) {
        return this.readPrintMedia.contains(mediaId);
    }

    @Override
    public HashSet<String> getReadPrintMedia() {
        return this.readPrintMedia;
    }

    @Override
    public boolean hasReadMap(InventoryItem item) {
        if (item instanceof MapItem) {
            MapItem map = (MapItem)item;
            return this.isPrintMediaRead(map.getMediaId());
        }
        return false;
    }

    @Override
    public void addReadMap(InventoryItem item) {
        if (item instanceof MapItem) {
            MapItem map = (MapItem)item;
            this.addReadPrintMedia(map.getMediaId());
        }
    }

    public void setMusicIntensityEventModData(String key, Object value) {
        KahluaTable table;
        if (StringUtils.isNullOrWhitespace(key)) {
            return;
        }
        KahluaTable kahluaTable = table = this.hasModData() ? Type.tryCastTo(this.getModData().rawget("MusicIntensityEvent"), KahluaTable.class) : null;
        if (table == null && value == null) {
            return;
        }
        if (table == null) {
            table = LuaManager.platform.newTable();
            this.getModData().rawset("MusicIntensityEvent", (Object)table);
        }
        table.rawset(key, value);
    }

    public Object getMusicIntensityEventModData(String key) {
        if (this.hasModData()) {
            Object object = this.getModData().rawget("MusicIntensityEvent");
            if (!(object instanceof KahluaTable)) {
                return null;
            }
            KahluaTable table = (KahluaTable)object;
            return table.rawget(key);
        }
        return null;
    }

    public boolean isWearingTag(ItemTag itemTag) {
        for (int i = 0; i < this.getWornItems().size(); ++i) {
            InventoryItem item = this.getWornItems().get(i).getItem();
            if (!item.hasTag(itemTag)) continue;
            return true;
        }
        return false;
    }

    public float getCorpseSicknessDefense() {
        return this.getCorpseSicknessDefense(0.0f, false);
    }

    public float getCorpseSicknessDefense(float rate) {
        return this.getCorpseSicknessDefense(rate, true);
    }

    public float getCorpseSicknessDefense(float rate, boolean drain) {
        float defense = 0.0f;
        for (int i = 0; i < this.getWornItems().size(); ++i) {
            InventoryItem item = this.getWornItems().getItemByIndex(i);
            if (item instanceof Clothing) {
                Clothing clothing = (Clothing)item;
                if (clothing.hasTag(ItemTag.SCBA) && clothing.isActivated() && clothing.hasTank() && clothing.getUsedDelta() > 0.0f) {
                    return 100.0f;
                }
                if (clothing.getCorpseSicknessDefense() > defense) {
                    defense = clothing.getCorpseSicknessDefense();
                }
                if ((clothing.hasTag(ItemTag.GAS_MASK) || clothing.hasTag(ItemTag.RESPIRATOR)) && clothing.hasFilter() && clothing.getUsedDelta() > 0.0f) {
                    defense = 100.0f;
                    if (drain && rate > 0.0f) {
                        clothing.drainGasMask(rate);
                    }
                }
            }
            if (!(defense >= 100.0f)) continue;
            return 100.0f;
        }
        return defense;
    }

    public boolean isProtectedFromToxic() {
        return this.isProtectedFromToxic(false);
    }

    public boolean isProtectedFromToxic(boolean drain) {
        for (int i = 0; i < this.getWornItems().size(); ++i) {
            InventoryItem item = this.getWornItems().getItemByIndex(i);
            if (!(item instanceof Clothing)) continue;
            Clothing clothing = (Clothing)item;
            if (clothing.hasTag(ItemTag.SCBA) && clothing.isActivated() && clothing.hasTank() && clothing.getUsedDelta() > 0.0f) {
                return true;
            }
            if (!clothing.hasTag(ItemTag.GAS_MASK) && !clothing.hasTag(ItemTag.RESPIRATOR) || !clothing.hasFilter() || !(clothing.getUsedDelta() > 0.0f)) continue;
            if (drain) {
                clothing.drainGasMask(0.01f);
            }
            return true;
        }
        return false;
    }

    private void checkSCBADrain() {
        for (int i = 0; i < this.getWornItems().size(); ++i) {
            InventoryItem item = this.getWornItems().getItemByIndex(i);
            if (!(item instanceof Clothing)) continue;
            Clothing clothing = (Clothing)item;
            if (clothing.hasTag(ItemTag.SCBA) && clothing.isActivated() && clothing.hasTank() && clothing.getUsedDelta() > 0.0f) {
                clothing.drainSCBA();
                return;
            }
            if (!clothing.hasTag(ItemTag.SCBA) || !clothing.isActivated() || clothing.hasTank() && !(clothing.getUsedDelta() <= 0.0f)) continue;
            clothing.setActivated(false);
            return;
        }
    }

    public boolean isOverEncumbered() {
        float maxWeight;
        float weight = this.getInventory().getCapacityWeight();
        return weight / (maxWeight = (float)this.getMaxWeight()) > 1.0f;
    }

    public void updateWornItemsVisionModifier() {
        float mod = 1.0f;
        if (this instanceof IsoZombie) {
            this.getItemVisuals(tempItemVisuals);
            if (tempItemVisuals != null) {
                for (int i = tempItemVisuals.size() - 1; i >= 0; --i) {
                    Item scriptItem;
                    ItemVisual itemVisual = (ItemVisual)tempItemVisuals.get(i);
                    if (itemVisual == null || (scriptItem = itemVisual.getScriptItem()) == null || scriptItem.getVisionModifier() == 1.0f || !(scriptItem.getVisionModifier() > 0.0f)) continue;
                    mod /= scriptItem.getVisionModifier();
                }
            }
        } else if (this.getWornItems() != null) {
            for (int i = 0; i < this.getWornItems().size(); ++i) {
                InventoryItem item = this.getWornItems().getItemByIndex(i);
                if (item == null || item.getVisionModifier() == 1.0f || !(item.getVisionModifier() > 0.0f)) continue;
                mod /= item.getVisionModifier();
            }
        }
        this.wornItemsVisionModifier = mod;
    }

    public float getWornItemsVisionModifier() {
        return this.wornItemsVisionModifier;
    }

    public float getWornItemsVisionMultiplier() {
        return 1.0f / this.getWornItemsVisionModifier();
    }

    public void updateWornItemsHearingModifier() {
        float mod = 1.0f;
        if (this instanceof IsoZombie) {
            this.getItemVisuals(tempItemVisuals);
            for (int i = tempItemVisuals.size() - 1; i >= 0; --i) {
                ItemVisual itemVisual = (ItemVisual)tempItemVisuals.get(i);
                Item scriptItem = itemVisual.getScriptItem();
                if (scriptItem == null || scriptItem.getHearingModifier() == 1.0f || !(scriptItem.getHearingModifier() > 0.0f)) continue;
                mod /= scriptItem.getHearingModifier();
            }
        } else {
            for (int i = 0; i < this.getWornItems().size(); ++i) {
                InventoryItem item = this.getWornItems().getItemByIndex(i);
                if (item == null || item.getHearingModifier() == 1.0f || !(item.getHearingModifier() > 0.0f)) continue;
                mod /= item.getHearingModifier();
            }
        }
        this.wornItemsHearingModifier = mod;
    }

    public float getWornItemsHearingModifier() {
        return this.wornItemsHearingModifier;
    }

    public float getWornItemsHearingMultiplier() {
        return 1.0f / this.getWornItemsHearingModifier();
    }

    public float getHearDistanceModifier() {
        float dist = 1.0f;
        if (this.characterTraits.get(CharacterTrait.HARD_OF_HEARING)) {
            dist *= 4.5f;
        }
        return dist *= this.getWornItemsHearingModifier();
    }

    public float getWeatherHearingMultiplier() {
        float resultMod = 1.0f;
        resultMod -= ClimateManager.getInstance().getRainIntensity() * 0.33f;
        return resultMod -= ClimateManager.getInstance().getFogIntensity() * 0.1f;
    }

    public float getSeeNearbyCharacterDistance() {
        return (3.5f - this.stats.get(CharacterStat.FATIGUE) - this.stats.get(CharacterStat.INTOXICATION) * 0.01f) * this.getWornItemsVisionMultiplier();
    }

    public void setLastHitCharacter(IsoGameCharacter character) {
        this.lastHitCharacter = character;
    }

    public IsoGameCharacter getLastHitCharacter() {
        return this.lastHitCharacter;
    }

    public void triggerCough() {
        this.setVariable("Ext", "Cough");
        this.Say(Translator.getText("IGUI_PlayerText_Cough"));
        this.reportEvent("EventDoExt");
        WorldSoundManager.instance.addSound(this, this.getXi(), this.getYi(), this.getZi(), 35, 40, true);
    }

    public boolean hasDirtyClothing(Integer part) {
        this.getItemVisuals(tempItemVisuals);
        for (int i = tempItemVisuals.size() - 1; i >= 0; --i) {
            InventoryItem item;
            ArrayList<BloodBodyPartType> coveredParts;
            ArrayList<BloodClothingType> types;
            ItemVisual itemVisual = (ItemVisual)tempItemVisuals.get(i);
            Item scriptItem = itemVisual.getScriptItem();
            if (scriptItem == null || (types = scriptItem.getBloodClothingType()) == null || (coveredParts = BloodClothingType.getCoveredParts(types)) == null || (item = itemVisual.getInventoryItem()) == null && (item = InventoryItemFactory.CreateItem(itemVisual.getItemType())) == null) continue;
            for (int j = 0; j < coveredParts.size(); ++j) {
                if (!(item instanceof Clothing) || coveredParts.get(j).index() != part.intValue() || !(itemVisual.getDirt(coveredParts.get(j)) > 0.15f)) continue;
                return true;
            }
        }
        return false;
    }

    public boolean hasBloodyClothing(Integer part) {
        this.getItemVisuals(tempItemVisuals);
        for (int i = tempItemVisuals.size() - 1; i >= 0; --i) {
            InventoryItem item;
            ArrayList<BloodBodyPartType> coveredParts;
            ArrayList<BloodClothingType> types;
            ItemVisual itemVisual = (ItemVisual)tempItemVisuals.get(i);
            Item scriptItem = itemVisual.getScriptItem();
            if (scriptItem == null || (types = scriptItem.getBloodClothingType()) == null || (coveredParts = BloodClothingType.getCoveredParts(types)) == null || (item = itemVisual.getInventoryItem()) == null && (item = InventoryItemFactory.CreateItem(itemVisual.getItemType())) == null) continue;
            for (int j = 0; j < coveredParts.size(); ++j) {
                if (!(item instanceof Clothing) || coveredParts.get(j).index() != part.intValue() || !(itemVisual.getBlood(coveredParts.get(j)) > 0.25f)) continue;
                return true;
            }
        }
        return false;
    }

    @Override
    public IAnimatable getAnimatable() {
        return this;
    }

    @Override
    public IGrappleable getGrappleable() {
        return this;
    }

    @Override
    public BaseGrappleable getWrappedGrappleable() {
        return this.grappleable;
    }

    @Override
    public boolean canBeGrappled() {
        if (this.isBeingGrappled()) {
            return false;
        }
        return this.canTransitionToState("grappled");
    }

    @Override
    public boolean isPerformingGrappleAnimation() {
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        if (animationPlayer == null || !animationPlayer.isReady()) {
            return false;
        }
        List<AnimationTrack> tracks = animationPlayer.getMultiTrack().getTracks();
        for (int i = 0; i < tracks.size(); ++i) {
            AnimationTrack track = tracks.get(i);
            if (!track.isGrappler()) continue;
            return true;
        }
        return false;
    }

    public String getShoutType() {
        if (this.getPrimaryHandItem() != null && this.getPrimaryHandItem().getShoutType() != null) {
            return this.getPrimaryHandItem().getShoutType() + "_primary";
        }
        if (this.getSecondaryHandItem() != null && this.getSecondaryHandItem().getShoutType() != null) {
            return this.getSecondaryHandItem().getShoutType() + "_secondary";
        }
        if (this.getWornItems() != null) {
            for (int i = 0; i < this.getWornItems().size(); ++i) {
                if (this.getWornItems().get(i).getItem() == null || this.getWornItems().get(i).getItem().getShoutType() == null) continue;
                return this.getWornItems().get(i).getItem().getShoutType() + "_secondary";
            }
        }
        return null;
    }

    public String getShoutItemModel() {
        if (this.getPrimaryHandItem() != null && this.getPrimaryHandItem().getShoutType() != null) {
            return this.getPrimaryHandItem().getStaticModel();
        }
        if (this.getSecondaryHandItem() != null && this.getSecondaryHandItem().getShoutType() != null) {
            return this.getSecondaryHandItem().getStaticModel();
        }
        return null;
    }

    public boolean isWearingGlasses() {
        InventoryItem wornItem = this.getWornItem(ItemBodyLocation.EYES);
        return this.isWearingVisualAid() || wornItem != null && wornItem.isVisualAid();
    }

    public boolean isWearingVisualAid() {
        InventoryItem wornItem;
        if (this.getWornItems() != null && !this.getWornItems().isEmpty()) {
            for (int i = 0; i < this.getWornItems().size(); ++i) {
                WornItem wornItem2 = this.getWornItems().get(i);
                InventoryItem characterItem = wornItem2.getItem();
                if (!characterItem.isVisualAid()) continue;
                return true;
            }
        }
        return (wornItem = this.getWornItem(ItemBodyLocation.EYES)) != null && wornItem.isVisualAid();
    }

    public float getClothingDiscomfortModifier() {
        return this.clothingDiscomfortModifier;
    }

    public float getVehicleDiscomfortModifier() {
        if (this.getVehicle() == null) {
            return 0.0f;
        }
        return (float)((double)this.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) * ZomboidGlobals.vehicleDiscomfortWhenOverEncumbered);
    }

    public void updateVisionEffectTargets() {
        this.blurFactor = PZMath.lerp(this.blurFactor, this.blurFactorTarget, this.blurFactor < this.blurFactorTarget ? 0.1f : 0.01f);
    }

    public void updateVisionEffects() {
        boolean isShortSighted = this.characterTraits.get(CharacterTrait.SHORT_SIGHTED);
        boolean isWearingGlasses = this.isWearingGlasses();
        this.blurFactorTarget = !isWearingGlasses && isShortSighted || isWearingGlasses && !isShortSighted ? 1.0f : 0.0f;
    }

    public float getBlurFactor() {
        return this.blurFactor;
    }

    public boolean isDisguised() {
        return this.usernameDisguised;
    }

    public void updateDisguisedState() {
        if (GameClient.client || GameServer.server) {
            boolean isUsernameDisguised = this.usernameDisguised;
            this.usernameDisguised = false;
            if (!ServerOptions.instance.usernameDisguises.getValue() && !ServerOptions.getInstance().hideDisguisedUserName.getValue()) {
                return;
            }
            IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
            if (player == null) {
                return;
            }
            SafeHouse safe = SafeHouse.isSafeHouse(this.getCurrentSquare(), null, false);
            if (safe == null || !ServerOptions.instance.safehouseDisableDisguises.getValue() || player.role.hasCapability(Capability.CanGoInsideSafehouses)) {
                HashSet<ItemTag> testItemTags = new HashSet<ItemTag>();
                this.getItemVisuals(tempItemVisuals);
                if (tempItemVisuals != null) {
                    for (int i = tempItemVisuals.size() - 1; i >= 0; --i) {
                        Item scriptItem;
                        ItemVisual itemVisual = (ItemVisual)tempItemVisuals.get(i);
                        if (itemVisual == null || (scriptItem = itemVisual.getScriptItem()) == null) continue;
                        testItemTags.addAll(scriptItem.getTags());
                    }
                } else if (this.getWornItems() != null) {
                    for (int i = 0; i < this.getWornItems().size(); ++i) {
                        Item scriptItem;
                        InventoryItem item = this.getWornItems().getItemByIndex(i);
                        if (item == null || (scriptItem = item.getScriptItem()) == null) continue;
                        testItemTags.addAll(scriptItem.getTags());
                    }
                }
                if (!testItemTags.isEmpty() && (testItemTags.contains(ItemTag.IS_DISGUISE) || testItemTags.contains(ItemTag.IS_LOWER_DISGUISE) && testItemTags.contains(ItemTag.IS_UPPER_DISGUISE))) {
                    this.usernameDisguised = true;
                }
                if (isUsernameDisguised || this.usernameDisguised) {
                    player.setDisplayName(null);
                }
            }
        }
    }

    public void OnClothingUpdated() {
        this.updateSpeedModifiers();
        if (this instanceof IsoPlayer) {
            this.updateVisionEffects();
            this.updateDiscomfortModifiers();
        }
        this.updateWornItemsVisionModifier();
        this.updateWornItemsHearingModifier();
    }

    public void OnEquipmentUpdated() {
    }

    private void renderDebugData() {
        Color c;
        IndieGL.StartShader(0);
        IndieGL.disableDepthTest();
        IndieGL.glBlendFunc(770, 771);
        IsoZombie isoZombie = Type.tryCastTo(this, IsoZombie.class);
        IsoPlayer isoPlayer = Type.tryCastTo(this, IsoPlayer.class);
        IsoAnimal isoAnimal = Type.tryCastTo(this, IsoAnimal.class);
        TextManager.StringDrawer stringDrawer = TextManager.instance::DrawString;
        Color ownerColor = Colors.Chartreuse;
        if (this.isDead()) {
            ownerColor = Colors.Yellow;
        } else if (!this.isLocal()) {
            ownerColor = Colors.OrangeRed;
        }
        UIFont font = UIFont.Dialogue;
        float sx = IsoUtils.XToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
        float sy = IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
        float dy = 0.0f;
        if (isoPlayer != null && isoPlayer.getRole() != null && isoAnimal == null) {
            stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("%d %.03f", this.getOnlineID(), Float.valueOf(isoZombie != null || isoAnimal != null ? this.getHealth() : this.getBodyDamage().getOverallBodyHealth())), ownerColor.r, ownerColor.g, ownerColor.b, ownerColor.a);
        } else {
            stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("%d", this.getOnlineID()), ownerColor.r, ownerColor.g, ownerColor.b, ownerColor.a);
        }
        if (this.getNetworkCharacterAI().getBooleanDebugOptions().position.getValue()) {
            Object object;
            c = Colors.RosyBrown;
            dy += 4.0f;
            stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("x=%09.3f", Float.valueOf(this.getX())), c.r, c.g, c.b, c.a);
            stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("y=%09.3f", Float.valueOf(this.getY())), c.r, c.g, c.b, c.a);
            stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("z=%09.3f", Float.valueOf(this.getZ())), c.r, c.g, c.b, c.a);
            if (this.getHitReactionNetworkAI().isSetup()) {
                LineDrawer.DrawIsoLine(this.getHitReactionNetworkAI().startPosition.x, this.getHitReactionNetworkAI().startPosition.y, this.getZ(), this.getHitReactionNetworkAI().finalPosition.x, this.getHitReactionNetworkAI().finalPosition.y, this.getZ(), Colors.Salmon.r, Colors.Salmon.g, Colors.Salmon.b, Colors.Salmon.a, 1);
                LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), 0.2f, 16, c.r, c.g, c.b, c.a);
                LineDrawer.DrawIsoCircle(this.getHitReactionNetworkAI().finalPosition.x, this.getHitReactionNetworkAI().finalPosition.y, this.getZ(), 0.2f, 16, Colors.Salmon.r, Colors.Salmon.g, Colors.Salmon.b, Colors.Salmon.a);
            }
            if ((object = this.getPrimaryHandItem()) instanceof HandWeapon) {
                HandWeapon handWeapon = (HandWeapon)object;
                LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), handWeapon.getMaxRange(this), 16, c.r, c.g, c.b, c.a);
            }
        }
        if (this.getNetworkCharacterAI().getBooleanDebugOptions().prediction.getValue()) {
            c = Colors.Magenta;
            dy += 4.0f;
            if (isoZombie != null) {
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), "Prediction: " + this.getNetworkCharacterAI().predictionType, c.r, c.g, c.b, c.a);
                LineDrawer.DrawIsoCircle(this.realx, this.realy, this.getZ(), 0.35f, 16, Colors.Blue.r, Colors.Blue.g, Colors.Blue.b, Colors.Blue.a);
                if (isoZombie.networkAi.debugInterfaceActive) {
                    LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), 0.4f, 4, Colors.NavajoWhite.r, Colors.NavajoWhite.g, Colors.NavajoWhite.b, Colors.NavajoWhite.a);
                } else if (this.isLocal()) {
                    LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), 0.3f, 3, Colors.Magenta.r, Colors.Magenta.g, Colors.Magenta.b, Colors.Magenta.a);
                } else {
                    LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), 0.3f, 5, Colors.Magenta.r, Colors.Magenta.g, Colors.Magenta.b, Colors.Magenta.a);
                }
                if (GameClient.client) {
                    LineDrawer.DrawIsoTransform(this.getNetworkCharacterAI().targetX, this.getNetworkCharacterAI().targetY, this.getZ(), 1.0f, 0.0f, 0.4f, 16, Colors.LimeGreen.r, Colors.LimeGreen.g, Colors.LimeGreen.b, Colors.LimeGreen.a, 1);
                    LineDrawer.DrawIsoLine(this.getX(), this.getY(), this.getZ(), this.getNetworkCharacterAI().targetX, this.getNetworkCharacterAI().targetY, this.getZ(), Colors.LimeGreen.r, Colors.LimeGreen.g, Colors.LimeGreen.b, Colors.LimeGreen.a, 1);
                    if (IsoUtils.DistanceToSquared(this.getX(), this.getY(), this.realx, this.realy) > 4.5f) {
                        LineDrawer.DrawIsoLine(this.realx, this.realy, this.getZ(), this.getX(), this.getY(), this.getZ(), Colors.Magenta.r, Colors.Magenta.g, Colors.Magenta.b, Colors.Magenta.a, 1);
                    } else {
                        LineDrawer.DrawIsoLine(this.realx, this.realy, this.getZ(), this.getX(), this.getY(), this.getZ(), Colors.Blue.r, Colors.Blue.g, Colors.Blue.b, Colors.Blue.a, 1);
                    }
                }
            } else if (isoPlayer != null && !this.isLocal()) {
                LineDrawer.DrawIsoLine(this.realx, this.realy, this.getZ(), this.getX(), this.getY(), this.getZ(), Colors.Blue.r, Colors.Blue.g, Colors.Blue.b, Colors.Blue.a, 1);
                LineDrawer.DrawIsoLine(this.getX(), this.getY(), this.getZ(), IsoGameCharacter.tempo.x, IsoGameCharacter.tempo.y, this.getZ(), Colors.LimeGreen.r, Colors.LimeGreen.g, Colors.LimeGreen.b, Colors.LimeGreen.a, 1);
                LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), 0.3f, 16, Colors.OrangeRed.r, Colors.OrangeRed.g, Colors.OrangeRed.b, Colors.OrangeRed.a);
                LineDrawer.DrawIsoCircle(this.realx, this.realy, this.getZ(), 0.35f, 16, Colors.Blue.r, Colors.Blue.g, Colors.Blue.b, Colors.Blue.a);
                tempo.set(this.getNetworkCharacterAI().targetX, this.getNetworkCharacterAI().targetY);
                LineDrawer.DrawIsoCircle(IsoGameCharacter.tempo.x, IsoGameCharacter.tempo.y, this.getZ(), 0.25f, 16, Colors.LimeGreen.r, Colors.LimeGreen.g, Colors.LimeGreen.b, Colors.LimeGreen.a);
                float tx = IsoUtils.XToScreenExact(this.getX() - 0.1f, this.getY() - 0.6f, this.getZ(), 0);
                float ty = IsoUtils.YToScreenExact(this.getX() - 0.1f, this.getY() - 0.6f, this.getZ(), 0);
                stringDrawer.draw(font, tx, ty, "local", Colors.OrangeRed.r, Colors.OrangeRed.g, Colors.OrangeRed.b, Colors.OrangeRed.a);
                tx = IsoUtils.XToScreenExact(this.realx - 1.1f, this.realy + 0.3f, this.getZ(), 0);
                ty = IsoUtils.YToScreenExact(this.realx - 1.1f, this.realy + 0.3f, this.getZ(), 0);
                stringDrawer.draw(font, tx, ty, "remote", Colors.Blue.r, Colors.Blue.g, Colors.Blue.b, Colors.Blue.a);
                tx = IsoUtils.XToScreenExact(IsoGameCharacter.tempo.x - 0.3f, IsoGameCharacter.tempo.y - 0.6f, this.getZ(), 0);
                ty = IsoUtils.YToScreenExact(IsoGameCharacter.tempo.x - 0.3f, IsoGameCharacter.tempo.y - 0.6f, this.getZ(), 0);
                stringDrawer.draw(font, tx, ty, "target", Colors.LimeGreen.r, Colors.LimeGreen.g, Colors.LimeGreen.b, Colors.LimeGreen.a);
            }
        }
        if (this.getNetworkCharacterAI().getBooleanDebugOptions().variables.getValue()) {
            c = Colors.MediumPurple;
            dy += 4.0f;
            stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("FallTime: %.03f", Float.valueOf(this.getFallTime())), c.r, c.g, c.b, c.a);
            stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("Reanimate: %.03f", Float.valueOf(this.getReanimateTimer())), c.r, c.g, c.b, c.a);
            if (isoPlayer != null && isoAnimal == null) {
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("SneakLimpSpeedScale: %s", Float.valueOf(isoPlayer.getSneakLimpSpeedScale())), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("IdleSpeed: %s , targetDist: %s ", isoPlayer.getVariableString("IdleSpeed"), isoPlayer.getVariableString("targetDist")), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("WalkInjury: %s , WalkSpeed: %s", isoPlayer.getVariableString("WalkInjury"), isoPlayer.getVariableString("WalkSpeed")), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("DeltaX: %s , DeltaY: %s", isoPlayer.getVariableString("DeltaX"), isoPlayer.getVariableString("DeltaY")), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("AttackVariationX: %s , AttackVariationY: %s", isoPlayer.getVariableString("AttackVariationX"), isoPlayer.getVariableString("AttackVariationY")), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("autoShootVarX: %s , autoShootVarY: %s", isoPlayer.getVariableString("autoShootVarX"), isoPlayer.getVariableString("autoShootVarY")), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("recoilVarX: %s , recoilVarY: %s", isoPlayer.getVariableString("recoilVarX"), isoPlayer.getVariableString("recoilVarY")), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("ShoveAimX: %s , ShoveAimY: %s", isoPlayer.getVariableString("ShoveAimX"), isoPlayer.getVariableString("ShoveAimY")), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("ForwardDirection: %f", Float.valueOf(isoPlayer.getDirectionAngleRadians())), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("FishingStage: %s", isoPlayer.getVariableString("FishingStage")), c.r, c.g, c.b, c.a);
            }
            if (isoAnimal != null) {
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("Stress:%.02f", Float.valueOf(isoAnimal.getStress())), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("Milk: %.02f", Float.valueOf(isoAnimal.getData().getMilkQuantity())), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("Acceptance:%.02f", Float.valueOf(isoAnimal.getAcceptanceLevel(IsoPlayer.getInstance()))), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("AlertX:%.02f", Float.valueOf(isoAnimal.getVariableFloat("AlertX", 0.0f))), c.r, c.g, c.b, c.a);
            }
            if (isoZombie != null) {
                IsoMovingObject ty = isoZombie.target;
                if (ty instanceof IsoPlayer) {
                    IsoPlayer player = (IsoPlayer)ty;
                    stringDrawer.draw(font, sx, sy + (dy += 14.0f), "Target: " + player.username, c.r, c.g, c.b, c.a);
                } else if (isoZombie.target != null) {
                    stringDrawer.draw(font, sx, sy + (dy += 14.0f), "Target: " + String.valueOf(isoZombie.target), c.r, c.g, c.b, c.a);
                } else {
                    stringDrawer.draw(font, sx, sy + (dy += 14.0f), "Target x=" + this.getPathTargetX(), c.r, c.g, c.b, c.a);
                    stringDrawer.draw(font, sx, sy + (dy += 14.0f), "Target y=" + this.getPathTargetY(), c.r, c.g, c.b, c.a);
                }
            }
        }
        if (this.getNetworkCharacterAI().getBooleanDebugOptions().state.getValue()) {
            c = Colors.LightBlue;
            dy += 4.0f;
            String subStateName = "";
            if (this.getStateMachine().getSubStateCount() > 0 && this.getStateMachine().getSubStateAt(0) != null) {
                subStateName = this.getStateMachine().getSubStateAt(0).getName();
            }
            stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("Class state: %s ( %s )", this.getCurrentStateName(), subStateName), c.r, c.g, c.b, c.a);
            String childStateName = "";
            if (!this.getActionContext().getChildStates().isEmpty() && this.getActionContext().getChildStateAt(0) != null) {
                childStateName = this.getActionContext().getChildStateAt(0).getName();
            }
            stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("Actions state: %s ( %s )", this.getCurrentActionContextStateName(), childStateName), c.r, c.g, c.b, c.a);
            if (this.characterActions != null) {
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("Actions: %d", this.characterActions.size()), c.r, c.g, c.b, c.a);
                for (BaseAction baseAction : this.characterActions) {
                    if (!(baseAction instanceof LuaTimedActionNew)) continue;
                    LuaTimedActionNew luaTimedActionNew = (LuaTimedActionNew)baseAction;
                    stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("Action: %s", luaTimedActionNew.getMetaType()), c.r, c.g, c.b, c.a);
                }
            }
            stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("Network state enter: %s ( %s )", this.getNetworkCharacterAI().getState().getEnterStateName(), this.getNetworkCharacterAI().getState().getEnterSubStateName()), c.r, c.g, c.b, c.a);
            stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("Network state exit: %s ( %s )", this.getNetworkCharacterAI().getState().getExitStateName(), this.getNetworkCharacterAI().getState().getExitSubStateName()), c.r, c.g, c.b, c.a);
            if (this.getNetworkCharacterAI().getState().getEnterState() != null) {
                LineDrawer.DrawIsoCircle(this.getNetworkCharacterAI().getState().getEnterState().getX(), this.getNetworkCharacterAI().getState().getEnterState().getY(), this.getNetworkCharacterAI().getState().getEnterState().getZ(), 0.2f, 16, c.r, c.g, c.b, c.a);
            }
            stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("Real state: %s", new Object[]{this.realState}), c.r, c.g, c.b, c.a);
        }
        if (this.getNetworkCharacterAI().getBooleanDebugOptions().stateVariables.getValue()) {
            c = Colors.LightGreen;
            dy += 4.0f;
            stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("isHitFromBehind: %b / %b", this.isHitFromBehind(), this.getVariableBoolean("frombehind")), c.r, c.g, c.b, c.a);
            stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("bKnockedDown: %b / %b", this.isKnockedDown(), this.getVariableBoolean("bknockeddown")), c.r, c.g, c.b, c.a);
            stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("isFallOnFront: %b / %b", this.isFallOnFront(), this.getVariableBoolean("fallonfront")), c.r, c.g, c.b, c.a);
            stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("isOnFloor: %b / %b", this.isOnFloor(), this.getVariableBoolean("bonfloor")), c.r, c.g, c.b, c.a);
            stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("isSitOnGround: %b / %b", this.isSitOnGround(), this.getVariableBoolean("sitonground")), c.r, c.g, c.b, c.a);
            stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("isDead: %b / %b", this.isDead(), this.getVariableBoolean("bdead")), c.r, c.g, c.b, c.a);
            stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("isAiming: %b / %b", this.isAiming(), this.getVariableBoolean("aim")), c.r, c.g, c.b, c.a);
            if (isoZombie != null) {
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("bThump: %b", this.getVariableString("bThump")), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("ThumpType: %s", this.getVariableString("ThumpType")), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("onknees: %b", this.getVariableBoolean("onknees")), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("isCanWalk: %b", isoZombie.isCanWalk()), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("isCrawling: %b", isoZombie.isCrawling()), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("isBecomeCrawler: %b", isoZombie.isBecomeCrawler()), c.r, c.g, c.b, c.a);
            } else {
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("isBumped: %b / %s", this.isBumped(), this.getBumpType()), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("bMoving: %b / %s", this.isMoving(), this.getVariableBoolean("bMoving")), c.r, c.g, c.b, c.a);
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("bPathfind: %s", this.getVariableBoolean("bPathfind")), c.r, c.g, c.b, c.a);
                if (isoAnimal != null) {
                    stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("isAlerted: %b", isoAnimal.isAlerted()), c.r, c.g, c.b, c.a);
                    stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("animalSpeed: %f", Float.valueOf(this.getVariableFloat("animalSpeed", -1.0f))), c.r, c.g, c.b, c.a);
                    stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("animalRunning: %s", this.getVariableBoolean(AnimationVariableHandles.animalRunning)), c.r, c.g, c.b, c.a);
                } else if (isoPlayer != null) {
                    stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("isGrappling: %b", this.isGrappling()), c.r, c.g, c.b, c.a);
                    stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("isDoGrapple: %b", this.isDoGrapple()), c.r, c.g, c.b, c.a);
                    stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("isDoContinueGrapple: %b", this.isDoContinueGrapple()), c.r, c.g, c.b, c.a);
                    stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("IsPerformingAnAction: %b", this.getVariableString("IsPerformingAnAction")), c.r, c.g, c.b, c.a);
                    stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("initiateAttack: %b", isoPlayer.isInitiateAttack()), c.r, c.g, c.b, c.a);
                    stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("rangedWeapon: %b", this.getVariableBoolean("rangedWeapon")), c.r, c.g, c.b, c.a);
                    stringDrawer.draw(font, sx, sy + (dy += 14.0f), String.format("bDoShove: %b", isoPlayer.isDoShove()), c.r, c.g, c.b, c.a);
                }
            }
        }
        if (this.getNetworkCharacterAI().getBooleanDebugOptions().enable.getValue() && this.getNetworkCharacterAI().getBooleanDebugOptions().animation.getValue()) {
            c = Colors.YellowGreen;
            dy += 4.0f;
            if (this.advancedAnimator.getRootLayer() != null) {
                stringDrawer.draw(font, sx, sy + (dy += 14.0f), "State: " + this.advancedAnimator.getCurrentStateName(), c.r, c.g, c.b, c.a);
                AnimationPlayer animationPlayer = this.getAnimationPlayer();
                if (animationPlayer != null) {
                    for (AnimationTrack track : animationPlayer.getMultiTrack().getTracks()) {
                        stringDrawer.draw(font, sx, sy + (dy += 14.0f), "Clip: " + track.currentClip.name, c.r, c.g, c.b, c.a);
                    }
                }
            }
        }
    }

    public float getCorpseSicknessRate() {
        return this.corpseSicknessRate;
    }

    public void setCorpseSicknessRate(float rate) {
        this.corpseSicknessRate = Math.max(0.0f, rate);
    }

    public void spikePartIndex(int bodyPartIndex) {
        DebugType.Combat.debugln("%s got spiked in %s", this, BodyPartType.getDisplayName(BodyPartType.FromIndex(bodyPartIndex)));
        HandWeapon weapon = (HandWeapon)InventoryItemFactory.CreateItem("Base.IcePick");
        if (this.getBodyDamage() == null) {
            this.splatBloodFloorBig();
            this.getEmitter().playSoundImpl(weapon.getZombieHitSound(), null);
            this.Hit(weapon, IsoWorld.instance.currentCell.getFakeZombieForHit(), 0.0f, false, 0.0f);
            return;
        }
        if (this instanceof IsoAnimal) {
            this.splatBloodFloorBig();
            this.getEmitter().playSoundImpl(weapon.getZombieHitSound(), null);
            this.Hit(weapon, IsoWorld.instance.currentCell.getFakeZombieForHit(), 0.0f, false, 0.0f);
        } else {
            this.getBodyDamage().DamageFromWeapon(weapon, bodyPartIndex);
        }
    }

    public void spikePart(BodyPartType partType) {
        int index = BodyPartType.ToIndex(partType);
        this.spikePartIndex(index);
    }

    public IsoGameCharacter getReanimatedCorpse() {
        return this.reanimatedCorpse;
    }

    public void applyDamage(float damageAmount) {
        this.health -= damageAmount;
        if (this.health < 0.0f) {
            this.health = 0.0f;
        }
    }

    public boolean canRagdoll() {
        if (GameClient.client || GameServer.server) {
            return false;
        }
        if (DebugOptions.instance.animation.disableRagdolls.getValue()) {
            return false;
        }
        if (!Core.getInstance().getOptionUsePhysicsHitReaction()) {
            return false;
        }
        if (GameServer.server && !ServerOptions.getInstance().usePhysicsHitReaction.getValue()) {
            return false;
        }
        if (this.getRagdollController() == null && RagdollController.getNumberOfActiveSimulations() >= Core.getInstance().getMaxActiveRagdolls()) {
            return false;
        }
        if (!this.wornClothingCanRagdoll) {
            return false;
        }
        return this.canCurrentStateRagdoll();
    }

    public RagdollController getRagdollController() {
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        if (animationPlayer == null) {
            return null;
        }
        return animationPlayer.getRagdollController();
    }

    public void releaseRagdollController() {
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        if (animationPlayer == null) {
            return;
        }
        animationPlayer.releaseRagdollController();
    }

    public BallisticsController getBallisticsController() {
        return this.ballisticsController;
    }

    public void updateBallistics() {
        if (GameServer.server) {
            return;
        }
        if (this.ballisticsController == null) {
            this.ballisticsController = BallisticsController.alloc();
            this.ballisticsController.setIsoGameCharacter(this);
        }
        this.ballisticsController.update();
    }

    public void releaseBallisticsController() {
        if (this.ballisticsController != null) {
            this.ballisticsController.releaseController();
            this.ballisticsController = null;
        }
    }

    public BallisticsTarget getBallisticsTarget() {
        return this.ballisticsTarget;
    }

    public BallisticsTarget ensureExistsBallisticsTarget(IsoGameCharacter isoGameCharacter) {
        if (isoGameCharacter == null) {
            return null;
        }
        if (this.ballisticsTarget == null) {
            this.ballisticsTarget = BallisticsTarget.alloc(isoGameCharacter);
        }
        this.ballisticsTarget.setIsoGameCharacter(isoGameCharacter);
        return this.ballisticsTarget;
    }

    private void updateBallisticsTarget() {
        if (this.ballisticsTarget == null) {
            return;
        }
        boolean releaseBallisticsTarget = this.ballisticsTarget.update();
        if (releaseBallisticsTarget) {
            this.releaseBallisticsTarget();
        }
    }

    public void releaseBallisticsTarget() {
        if (this.ballisticsTarget != null) {
            this.ballisticsTarget.releaseTarget();
            this.ballisticsTarget = null;
        }
    }

    public boolean canReachTo(IsoGridSquare square) {
        return this.getSquare().canReachTo(square);
    }

    public boolean canUseAsGenericCraftingSurface(IsoObject object) {
        return object.isGenericCraftingSurface() && this.getSquare().canReachTo(object.getSquare());
    }

    public PZArrayList<HitInfo> getHitInfoList() {
        return this.hitInfoList;
    }

    public AimingMode getAimingMode() {
        return this.aimingMode;
    }

    public void updateAimingMode() {
        this.aimingMode = this.isAiming() ? (this.getHitInfoList().isEmpty() ? AimingMode.REGULAR : AimingMode.TARGET_FOUND) : AimingMode.NOT_AIMING;
    }

    public AttackVars getAttackVars() {
        return this.attackVars;
    }

    public void addCombatMuscleStrain(InventoryItem weapon) {
        this.addCombatMuscleStrain(weapon, 1);
    }

    public void addCombatMuscleStrain(InventoryItem weapon, int hitCount) {
        this.addCombatMuscleStrain(weapon, hitCount, 1.0f);
    }

    public void addCombatMuscleStrain(InventoryItem item, int hitCount, float multiplier) {
        if (this.isDoStomp()) {
            float val = 0.3f;
            float strengthMod = (float)(15 - this.getPerkLevel(PerkFactory.Perks.Strength)) / 10.0f;
            this.addRightLegMuscleStrain(val *= strengthMod);
            return;
        }
        if (this.isShoving()) {
            float val = 0.15f;
            float strengthMod = (float)(15 - this.getPerkLevel(PerkFactory.Perks.Strength)) / 10.0f;
            this.addBothArmMuscleStrain(val *= strengthMod);
            return;
        }
        HandWeapon weapon = null;
        if (item instanceof HandWeapon) {
            HandWeapon handWeapon;
            weapon = handWeapon = (HandWeapon)item;
        }
        if (weapon != null && weapon.isAimedFirearm()) {
            float value = (float)weapon.getRecoilDelay(this) * CombatManager.getInstance().getCombatConfig().get(CombatConfigKey.FIREARM_RECOIL_MUSCLE_STRAIN_MODIFIER) * weapon.muscleStrainMod(this) * ((float)(15 - this.getPerkLevel(PerkFactory.Perks.Strength)) / 10.0f);
            if ("Auto".equalsIgnoreCase(weapon.getFireMode())) {
                value *= 0.5f;
            }
            if ((value *= (float)SandboxOptions.instance.muscleStrainFactor.getValue()) == 0.0f) {
                return;
            }
            this.addStiffness(BodyPartType.Hand_R, value);
            this.addStiffness(BodyPartType.ForeArm_R, value);
            if (this.getSecondaryHandItem() == weapon) {
                this.addStiffness(BodyPartType.UpperArm_R, value);
                this.addStiffness(BodyPartType.Hand_L, value * 0.1f);
                this.addStiffness(BodyPartType.ForeArm_L, value * 0.1f);
            }
            return;
        }
        if (!this.isActuallyAttackingWithMeleeWeapon() || item == null) {
            return;
        }
        if (SandboxOptions.instance.muscleStrainFactor.getValue() > 0.0 && weapon != null && weapon.isUseEndurance() && WeaponType.getWeaponType(this) != WeaponType.UNARMED && !weapon.isRanged()) {
            if (hitCount <= 0) {
                hitCount = 1;
            }
            if (multiplier <= 0.0f) {
                multiplier = 1.0f;
            }
            boolean twoHandedWeapon = weapon.isTwoHandWeapon();
            boolean twoHandedUsedOneHand = weapon.isTwoHandWeapon() && (this.getPrimaryHandItem() != weapon || this.getSecondaryHandItem() != weapon);
            float enduranceTwoHandsWeaponModifier = 0.0f;
            if (twoHandedUsedOneHand) {
                enduranceTwoHandsWeaponModifier = weapon.getWeight() / 1.5f / 10.0f;
                twoHandedWeapon = false;
            }
            float val = (weapon.getWeight() * 0.15f * weapon.getEnduranceMod() * 0.3f + enduranceTwoHandsWeaponModifier) * 4.0f;
            float mod = 1.0f;
            val *= (mod *= (float)(hitCount + 1));
            float strengthMod = (float)(15 - this.getPerkLevel(PerkFactory.Perks.Strength)) / 10.0f;
            val *= strengthMod;
            val *= weapon.muscleStrainMod(this);
            val *= multiplier;
            if (twoHandedWeapon) {
                val *= 0.5f;
            }
            val = (float)((double)val * 0.65);
            this.addArmMuscleStrain(val);
            if (twoHandedWeapon) {
                this.addLeftArmMuscleStrain(val);
            }
        } else if (SandboxOptions.instance.muscleStrainFactor.getValue() > 0.0 && weapon == null && item != null) {
            float val = item.getWeight() * 0.15f * 0.3f * 4.0f;
            float strengthMod = (float)(15 - this.getPerkLevel(PerkFactory.Perks.Strength)) / 10.0f;
            val *= strengthMod;
            val = (float)((double)val * 0.65);
            this.addArmMuscleStrain(val);
        }
    }

    public void addRightLegMuscleStrain(float painfactor) {
        if (SandboxOptions.instance.muscleStrainFactor.getValue() > 0.0) {
            painfactor *= 2.5f;
            painfactor = (float)((double)painfactor * SandboxOptions.instance.muscleStrainFactor.getValue());
            this.addStiffness(BodyPartType.UpperLeg_R, painfactor);
            this.addStiffness(BodyPartType.LowerLeg_R, painfactor);
            this.addStiffness(BodyPartType.Foot_R, painfactor);
        }
    }

    public void addBackMuscleStrain(float painfactor) {
        if (SandboxOptions.instance.muscleStrainFactor.getValue() > 0.0) {
            painfactor *= 2.5f;
            painfactor = (float)((double)painfactor * SandboxOptions.instance.muscleStrainFactor.getValue());
            this.addStiffness(BodyPartType.Torso_Upper, painfactor);
            this.addStiffness(BodyPartType.Torso_Lower, painfactor);
        }
    }

    public void addNeckMuscleStrain(float painfactor) {
        if (SandboxOptions.instance.muscleStrainFactor.getValue() > 0.0) {
            painfactor *= 2.5f;
            painfactor = (float)((double)painfactor * SandboxOptions.instance.muscleStrainFactor.getValue());
            this.addStiffness(BodyPartType.Neck, painfactor);
        }
    }

    public void addArmMuscleStrain(float painfactor) {
        if (SandboxOptions.instance.muscleStrainFactor.getValue() > 0.0) {
            painfactor *= 2.5f;
            painfactor = (float)((double)painfactor * SandboxOptions.instance.muscleStrainFactor.getValue());
            this.addStiffness(BodyPartType.Hand_R, painfactor);
            this.addStiffness(BodyPartType.ForeArm_R, painfactor);
            this.addStiffness(BodyPartType.UpperArm_R, painfactor);
        }
    }

    public void addLeftArmMuscleStrain(float painfactor) {
        if (SandboxOptions.instance.muscleStrainFactor.getValue() > 0.0) {
            painfactor *= 2.5f;
            painfactor = (float)((double)painfactor * SandboxOptions.instance.muscleStrainFactor.getValue());
            this.addStiffness(BodyPartType.Hand_L, painfactor);
            this.addStiffness(BodyPartType.ForeArm_L, painfactor);
            this.addStiffness(BodyPartType.UpperArm_L, painfactor);
        }
    }

    public void addBothArmMuscleStrain(float painfactor) {
        this.addArmMuscleStrain(painfactor);
        this.addLeftArmMuscleStrain(painfactor);
    }

    public void addStiffness(BodyPartType partType, float stiffness) {
        BodyPart part = this.getBodyDamage().getBodyPart(partType);
        part.addStiffness(stiffness);
    }

    public int getClimbingFailChanceInt() {
        return (int)this.getClimbingFailChanceFloat();
    }

    public float getClimbingFailChanceFloat() {
        float failChance = 0.0f;
        failChance += (float)this.getPerkLevel(PerkFactory.Perks.Fitness) * 2.0f;
        failChance += (float)this.getPerkLevel(PerkFactory.Perks.Strength) * 2.0f;
        failChance += (float)this.getPerkLevel(PerkFactory.Perks.Nimble) * 2.0f;
        failChance += (float)this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) * -5.0f;
        failChance += (float)this.getMoodles().getMoodleLevel(MoodleType.DRUNK) * -8.0f;
        failChance += (float)this.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) * -8.0f;
        failChance += (float)this.getMoodles().getMoodleLevel(MoodleType.PAIN) * -5.0f;
        if (this.characterTraits.get(CharacterTrait.OBESE)) {
            failChance += -25.0f;
        } else if (this.characterTraits.get(CharacterTrait.OVERWEIGHT)) {
            failChance += -15.0f;
        }
        if (this.characterTraits.get(CharacterTrait.CLUMSY)) {
            failChance /= 2.0f;
        }
        if (this.isWearingAwkwardGloves()) {
            failChance /= 2.0f;
        } else if (!this.isWearingAwkwardGloves() && this.isWearingGloves()) {
            failChance += 4.0f;
        }
        if (this.characterTraits.get(CharacterTrait.ALL_THUMBS)) {
            failChance += -4.0f;
        } else if (this.characterTraits.get(CharacterTrait.DEXTROUS)) {
            failChance += 4.0f;
        }
        if (this.characterTraits.get(CharacterTrait.BURGLAR)) {
            failChance += 4.0f;
        }
        if (this.characterTraits.get(CharacterTrait.GYMNAST)) {
            failChance += 4.0f;
        }
        failChance += this.nearbyZombieClimbPenalty();
        failChance = Math.max(0.0f, failChance);
        failChance = (int)Math.sqrt(failChance);
        return failChance;
    }

    public float nearbyZombieClimbPenalty() {
        float penaltyChance = 0.0f;
        IsoGridSquare current = this.getCurrentSquare();
        if (current == null) {
            return penaltyChance;
        }
        for (int i = 0; i < current.getMovingObjects().size(); ++i) {
            IsoMovingObject mov = current.getMovingObjects().get(i);
            if (!(mov instanceof IsoZombie)) continue;
            IsoZombie isoZombie = (IsoZombie)mov;
            if (isoZombie.target == this && isoZombie.getCurrentState() == AttackState.instance()) {
                penaltyChance += 25.0f;
                continue;
            }
            penaltyChance += 7.0f;
        }
        return penaltyChance;
    }

    public boolean isClimbingRope() {
        return this.getCurrentState().equals(ClimbSheetRopeState.instance()) || this.getCurrentState().equals(ClimbDownSheetRopeState.instance());
    }

    public void fallFromRope() {
        if (!this.isClimbingRope()) {
            return;
        }
        this.setCollidable(true);
        this.setbClimbing(false);
        this.setbFalling(true);
        this.clearVariable("ClimbRope");
        this.setLlz(this.getZ());
    }

    public boolean isWearingGloves() {
        return this.getWornItem(ItemBodyLocation.HANDS) != null;
    }

    public boolean isWearingAwkwardGloves() {
        return this.getWornItem(ItemBodyLocation.HANDS) != null && this.getWornItem(ItemBodyLocation.HANDS).hasTag(ItemTag.AWKWARD_GLOVES);
    }

    public float getClimbRopeSpeed(boolean down) {
        int effectiveStrength = Math.max(this.getPerkLevel(PerkFactory.Perks.Strength), this.getPerkLevel(PerkFactory.Perks.Fitness)) - (this.getMoodles().getMoodleLevel(MoodleType.DRUNK) + this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) + this.getMoodles().getMoodleLevel(MoodleType.PAIN));
        if (!down) {
            effectiveStrength -= this.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD);
            if (this.characterTraits.get(CharacterTrait.OBESE)) {
                effectiveStrength -= 2;
            } else if (this.characterTraits.get(CharacterTrait.OVERWEIGHT)) {
                --effectiveStrength;
            }
        }
        if (this.characterTraits.get(CharacterTrait.ALL_THUMBS)) {
            --effectiveStrength;
        } else if (this.characterTraits.get(CharacterTrait.DEXTROUS)) {
            ++effectiveStrength;
        }
        if (this.characterTraits.get(CharacterTrait.BURGLAR)) {
            ++effectiveStrength;
        }
        if (this.characterTraits.get(CharacterTrait.GYMNAST)) {
            ++effectiveStrength;
        }
        if (this.isWearingAwkwardGloves()) {
            effectiveStrength /= 2;
        } else if (this.isWearingGloves()) {
            ++effectiveStrength;
        }
        effectiveStrength = Math.max(0, effectiveStrength);
        effectiveStrength = Math.min(10, effectiveStrength);
        float climbSpeed = 0.16f;
        switch (effectiveStrength) {
            case 0: {
                climbSpeed -= 0.12f;
                break;
            }
            case 1: {
                climbSpeed -= 0.11f;
                break;
            }
            case 2: {
                climbSpeed -= 0.1f;
                break;
            }
            case 3: {
                climbSpeed -= 0.09f;
                break;
            }
            case 6: {
                climbSpeed += 0.02f;
                break;
            }
            case 7: {
                climbSpeed += 0.05f;
                break;
            }
            case 8: {
                climbSpeed += 0.07f;
                break;
            }
            case 9: {
                climbSpeed += 0.09f;
                break;
            }
            case 10: {
                climbSpeed += 0.12f;
            }
        }
        return climbSpeed *= 0.5f;
    }

    public void setClimbRopeTime(float time) {
        this.climbRopeTime = time;
    }

    public float getClimbRopeTime() {
        return this.climbRopeTime;
    }

    public boolean hasAwkwardHands() {
        return this.isWearingAwkwardGloves() || this.characterTraits.get(CharacterTrait.ALL_THUMBS);
    }

    private boolean forbidConcurrentAction(String action) {
        if (this.concurrentActionList.contains(action)) {
            return false;
        }
        return this.isDoingActionThatCanBeCancelled();
    }

    @Override
    public void triggerContextualAction(String action) {
        if (this.forbidConcurrentAction(action)) {
            return;
        }
        LuaHookManager.TriggerHook("ContextualAction", action, this);
    }

    @Override
    public void triggerContextualAction(String action, Object param1) {
        if (this.forbidConcurrentAction(action)) {
            return;
        }
        LuaHookManager.TriggerHook("ContextualAction", action, this, param1);
    }

    @Override
    public void triggerContextualAction(String action, Object param1, Object param2) {
        if (this.forbidConcurrentAction(action)) {
            return;
        }
        LuaHookManager.TriggerHook("ContextualAction", action, this, param1, param2);
    }

    @Override
    public void triggerContextualAction(String action, Object param1, Object param2, Object param3) {
        if (this.forbidConcurrentAction(action)) {
            return;
        }
        LuaHookManager.TriggerHook("ContextualAction", action, this, param1, param2, param3);
    }

    @Override
    public void triggerContextualAction(String action, Object param1, Object param2, Object param3, Object param4) {
        if (this.forbidConcurrentAction(action)) {
            return;
        }
        LuaHookManager.TriggerHook("ContextualAction", action, this, param1, param2, param3, param4);
    }

    public boolean isActuallyAttackingWithMeleeWeapon() {
        if (this.getPrimaryHandItem() == null) {
            return false;
        }
        if (!(this.getPrimaryHandItem() instanceof HandWeapon)) {
            return false;
        }
        if (this.getUseHandWeapon() == null) {
            return false;
        }
        HandWeapon weapon = this.getUseHandWeapon();
        if (weapon.isBareHands()) {
            return false;
        }
        if (weapon.isRanged()) {
            return false;
        }
        if (this.isShoving()) {
            return false;
        }
        return !this.isDoStomp();
    }

    public boolean isDoStomp() {
        return false;
    }

    public boolean isShoving() {
        return false;
    }

    public void teleportTo(int newX, int newY) {
        this.teleportTo(newX, newY, 0);
    }

    public void teleportTo(float newX, float newY) {
        this.teleportTo((int)newX, (int)newY, 0);
    }

    public void teleportTo(float newX, float newY, int newZ) {
        this.teleportTo(PZMath.fastfloor(newX), PZMath.fastfloor(newY), newZ);
    }

    public void teleportTo(int newX, int newY, int newZ) {
        this.ensureNotInVehicle();
        newZ = Math.max(-32, newZ);
        newZ = Math.min(31, newZ);
        this.setX(newX);
        this.setY(newY);
        this.setZ(newZ);
        this.setLastX(newX);
        this.setLastY(newY);
        this.ensureOnTile();
    }

    public void ensureNotInVehicle() {
        if (this.getVehicle() != null) {
            this.getVehicle().exit(this);
            LuaEventManager.triggerEvent("OnExitVehicle", this);
        }
    }

    public void forgetRecipes() {
        this.getKnownRecipes().clear();
    }

    protected boolean isHandModelOverriddenByCurrentCharacterAction() {
        BaseAction action;
        BaseAction baseAction = action = this.characterActions.isEmpty() ? null : (BaseAction)this.characterActions.get(0);
        if (action == null) {
            return false;
        }
        return action.overrideHandModels;
    }

    protected boolean isPrimaryHandModelReady() {
        return this.primaryHandModel != null && this.primaryHandModel.model != null && this.primaryHandModel.model.isReady();
    }

    private boolean isRangedWeaponReady() {
        if (this.useHandWeapon != null && this.useHandWeapon.isAimedFirearm() && this.isPrimaryHandModelReady() && this.primaryHandModel.modelScript != null) {
            ModelAttachment attachment = this.primaryHandModel.modelScript.getAttachmentById("muzzle");
            return attachment != null;
        }
        return true;
    }

    public boolean isWeaponReady() {
        return !this.isHandModelOverriddenByCurrentCharacterAction() && this.isPrimaryHandModelReady() && this.isRangedWeaponReady();
    }

    public void climbThroughWindow(IsoObject isoObject) {
        if (isoObject instanceof IsoWindow) {
            IsoWindow isoWindow = (IsoWindow)isoObject;
            this.climbThroughWindow(isoWindow);
        } else if (isoObject instanceof IsoThumpable) {
            IsoThumpable isoThumpable = (IsoThumpable)isoObject;
            this.climbThroughWindow(isoThumpable);
        }
    }

    public ClimbSheetRopeState.ClimbData getClimbData() {
        return this.climbData;
    }

    public void setClimbData(ClimbSheetRopeState.ClimbData climbData) {
        this.climbData = climbData;
    }

    public float getIdleSquareTime() {
        return this.idleSquareTime;
    }

    private void updateIdleSquareTime() {
        if (this.getCurrentSquare() == this.getLastSquare()) {
            if (this.idleSquareTime <= 3600.0f) {
                this.idleSquareTime += 1.0f * GameTime.instance.getMultiplier() * GameTime.instance.getDeltaMinutesPerDay();
            }
        } else {
            this.idleSquareTime = 0.0f;
        }
    }

    public boolean isCurrentlyIdle() {
        IsoGameCharacter isoGameCharacter = this;
        if (isoGameCharacter instanceof IsoPlayer) {
            IsoPlayer player = (IsoPlayer)isoGameCharacter;
            if (player.isPlayerMoving() && (player.isWalking() || player.isRunning() || player.isSprinting())) {
                return false;
            }
            if (this.isAsleep()) {
                return false;
            }
            if ((this.isSittingOnFurniture() || this.isSitOnGround()) && this.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) >= 1) {
                return false;
            }
            if (this.isReading()) {
                return false;
            }
            if (!this.characterActions.isEmpty()) {
                return false;
            }
            if (this.getMoodles().getMoodleLevel(MoodleType.PANIC) > 1) {
                return false;
            }
            if (this.isInCombat()) {
                return false;
            }
            if (GameServer.server) {
                if (player.networkAi.getState().getEnterState() != null && !IdleState.instance().equals(player.networkAi.getState().getEnterState().getState())) {
                    return false;
                }
                if (player.networkAi.getState().getExitState() != null && !IdleState.instance().equals(player.networkAi.getState().getExitState().getState())) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    public boolean isCurrentlyBusy() {
        IsoPlayer player = Type.tryCastTo(this, IsoPlayer.class);
        if (player != null) {
            if (this.isAsleep()) {
                return true;
            }
            if (this.isReading()) {
                return true;
            }
            if (!this.characterActions.isEmpty() && !((BaseAction)this.characterActions.get(0)).isPathfinding()) {
                return true;
            }
            return this.isInCombat();
        }
        return true;
    }

    private boolean isInCombat() {
        return this.stats.getNumVeryCloseZombies() > 0 || this.stats.getNumChasingZombies() >= 3;
    }

    private void updateMovementStatistics() {
        if (this.isoPlayer == null) {
            return;
        }
        tempVector2_1.set(this.getLastX(), this.getLastY());
        tempVector2_2.set(this.getX(), this.getY());
        float distanceTraveled = tempVector2_1.distanceTo(tempVector2_2);
        if (this.isSprinting()) {
            StatisticsManager.getInstance().incrementStatistic(StatisticType.Player, StatisticCategory.Travel, "Distance Sprinted", distanceTraveled);
        } else if (this.isRunning()) {
            StatisticsManager.getInstance().incrementStatistic(StatisticType.Player, StatisticCategory.Travel, "Distance Ran", distanceTraveled);
        } else if (this.isDriving()) {
            StatisticsManager.getInstance().incrementStatistic(StatisticType.Player, StatisticCategory.Travel, "Distance Driven", distanceTraveled);
        } else if (this.isoPlayer.isPlayerMoving) {
            StatisticsManager.getInstance().incrementStatistic(StatisticType.Player, StatisticCategory.Travel, "Distance Walked", distanceTraveled);
        }
        if (distanceTraveled != 0.0f) {
            StatisticsManager.getInstance().setStatistic(StatisticType.Player, StatisticCategory.Travel, "Travel Speed", distanceTraveled * GameWindow.averageFPS);
        }
    }

    @Override
    public void flagForHotSave() {
    }

    public ArrayList<ItemContainer> getContainers() {
        ArrayList list = null;
        ArrayList list2 = null;
        for (int i = 0; i < list.size(); ++i) {
            boolean locked;
            ItemContainer cont = (ItemContainer)list.get(i);
            boolean bl = locked = cont.getParent() != null && cont.getParent() instanceof IsoThumpable && ((IsoThumpable)cont.getParent()).isLockedToCharacter(this);
            if (locked) continue;
            list2.add(cont);
        }
        return list2;
    }

    public boolean hasRecipeAtHand(CraftRecipe recipe) {
        if (recipe.getMetaRecipe() != null && this.getInventory().hasRecipe(recipe.getMetaRecipe(), this, true)) {
            return true;
        }
        return this.getInventory().hasRecipe(recipe.getName(), this, true);
    }

    public PlayerCheats getCheats() {
        return this.cheats;
    }

    public VisibilityData calculateVisibilityData() {
        float currentFatigue = this.stats.get(CharacterStat.FATIGUE);
        float fatigue = Math.max(0.0f, currentFatigue - 0.6f) * 2.5f;
        float cone = -0.2f - fatigue;
        float noiseDistance = 2.0f;
        if (fatigue >= 1.0f) {
            cone -= 0.2f;
        }
        cone -= this.stats.get(CharacterStat.INTOXICATION) * 0.002f * (float)(this.moodles.getMoodleLevel(MoodleType.DRUNK) >= 2 ? 1 : 0);
        if (this.moodles.getMoodleLevel(MoodleType.PANIC) == 4) {
            cone -= 0.2f;
        }
        if (this.characterTraits.get(CharacterTrait.EAGLE_EYED)) {
            cone += 0.2f;
        }
        if (this instanceof IsoPlayer && this.getVehicle() != null) {
            cone = 1.0f;
        }
        if (this.characterTraits.get(CharacterTrait.HARD_OF_HEARING)) {
            noiseDistance -= 1.0f;
        }
        if (this.characterTraits.get(CharacterTrait.KEEN_HEARING)) {
            noiseDistance += 3.0f;
        }
        float baseAmbient = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex());
        return new VisibilityData(fatigue, noiseDistance *= this.getWornItemsHearingMultiplier(), cone, baseAmbient);
    }

    public boolean hasFullInventory() {
        return this.getInventory().isFull(this);
    }

    public float getFreeInventoryCapacity() {
        return this.getInventory().getFreeCapacity(this);
    }

    static {
        s_bandages = new Bandages();
        SurvivorMap = new HashMap();
        LevelUpLevels = new int[]{25, 75, 150, 225, 300, 400, 500, 600, 700, 800, 900, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 2400, 2600, 2800, 3000, 3200, 3400, 3600, 3800, 4000, 4400, 4800, 5200, 5600, 6000};
        tempo = new Vector2();
        tempo3 = new Vector3();
        inf = new ColorInfo();
        tempo2 = new Vector2();
        tempVector2_1 = new Vector2();
        tempVector2_2 = new Vector2();
        tempVector2 = new Vector2();
        tempVector3f00 = new Vector3f();
        tempVector3f01 = new Vector3f();
        s_turn180StartedEvent = new AnimEvent();
        IsoGameCharacter.s_turn180StartedEvent.time = AnimEvent.AnimEventTime.END;
        IsoGameCharacter.s_turn180StartedEvent.eventName = "Turn180Started";
        s_turn180TargetChangedEvent = new AnimEvent();
        IsoGameCharacter.s_turn180TargetChangedEvent.time = AnimEvent.AnimEventTime.END;
        IsoGameCharacter.s_turn180TargetChangedEvent.eventName = "Turn180TargetChanged";
        movingStatic = new ArrayList();
        tempVectorBonePos = new Vector3();
    }

    @UsedFromLua
    public static class Location {
        public int x;
        public int y;
        public int z;

        public Location() {
        }

        public Location(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Location set(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        public int getX() {
            return this.x;
        }

        public int getY() {
            return this.y;
        }

        public int getZ() {
            return this.z;
        }

        public boolean equals(int x, int y, int z) {
            return this.x == x && this.y == y && this.z == z;
        }

        public boolean equals(Object other) {
            if (other instanceof Location) {
                Location location = (Location)other;
                return this.x == location.x && this.y == location.y && this.z == location.z;
            }
            return false;
        }
    }

    public static class LightInfo {
        public IsoGridSquare square;
        public float x;
        public float y;
        public float z;
        public float angleX;
        public float angleY;
        public ArrayList<TorchInfo> torches = new ArrayList();
        public long time;
        public float night;
        public float rmod;
        public float gmod;
        public float bmod;

        public void initFrom(LightInfo other) {
            this.square = other.square;
            this.x = other.x;
            this.y = other.y;
            this.z = other.z;
            this.angleX = other.angleX;
            this.angleY = other.angleY;
            this.torches.clear();
            this.torches.addAll(other.torches);
            this.time = (long)((double)System.nanoTime() / 1000000.0);
            this.night = other.night;
            this.rmod = other.rmod;
            this.gmod = other.gmod;
            this.bmod = other.bmod;
        }
    }

    private static class Recoil {
        private float recoilVarX;
        private float recoilVarY;

        private Recoil() {
        }
    }

    private static final class LC_slideAwayFromWalls {
        public final Vector2f vector2f = new Vector2f();

        private LC_slideAwayFromWalls() {
        }
    }

    @UsedFromLua
    public class XP
    implements AntiCheatXPUpdate.IAntiCheatUpdate {
        public int level;
        public int lastlevel;
        public float totalXp;
        public HashMap<PerkFactory.Perk, Float> xpMap;
        public HashMap<PerkFactory.Perk, XPMultiplier> xpMapMultiplier;
        private final IsoGameCharacter chr;
        private static final long XP_INTERVAL = 60000L;
        private final UpdateLimit ulInterval;
        private float sum;
        final /* synthetic */ IsoGameCharacter this$0;

        public XP(IsoGameCharacter this$0, IsoGameCharacter chr) {
            IsoGameCharacter isoGameCharacter = this$0;
            Objects.requireNonNull(isoGameCharacter);
            this.this$0 = isoGameCharacter;
            this.xpMap = new HashMap();
            this.xpMapMultiplier = new HashMap();
            this.ulInterval = new UpdateLimit(60000L);
            this.chr = chr;
        }

        @Override
        public boolean intervalCheck() {
            return this.ulInterval.Check();
        }

        @Override
        public float getGrowthRate() {
            this.ulInterval.Reset(60000L);
            float sum = 0.0f;
            for (Float value : this.xpMap.values()) {
                sum += value.floatValue();
            }
            float rate = sum - this.sum;
            this.sum = sum;
            return rate;
        }

        @Override
        public float getMultiplier() {
            double multiplier = 0.0;
            if (SandboxOptions.instance.multipliersConfig.xpMultiplierGlobalToggle.getValue()) {
                multiplier = SandboxOptions.instance.multipliersConfig.xpMultiplierGlobal.getValue();
            } else {
                int optionCount = 0;
                for (int i = 0; i < this.this$0.getPerkList().size(); ++i) {
                    String optionName = "MultiplierConfig." + String.valueOf(this.this$0.getPerkList().get(i));
                    if (SandboxOptions.instance.getOptionByName(optionName) == null) continue;
                    ++optionCount;
                    multiplier += Double.parseDouble(SandboxOptions.instance.getOptionByName(optionName).asConfigOption().getValueAsString());
                }
                multiplier /= (double)optionCount;
            }
            return (float)multiplier;
        }

        public void addXpMultiplier(PerkFactory.Perk perks, float multiplier, int minLevel, int maxLevel) {
            XPMultiplier xpMultiplier = this.xpMapMultiplier.get(perks);
            if (xpMultiplier == null) {
                xpMultiplier = new XPMultiplier();
            }
            xpMultiplier.multiplier = multiplier;
            xpMultiplier.minLevel = minLevel;
            xpMultiplier.maxLevel = maxLevel;
            this.xpMapMultiplier.put(perks, xpMultiplier);
        }

        public HashMap<PerkFactory.Perk, XPMultiplier> getMultiplierMap() {
            return this.xpMapMultiplier;
        }

        public float getMultiplier(PerkFactory.Perk perk) {
            XPMultiplier xpMultiplier = this.xpMapMultiplier.get(perk);
            if (xpMultiplier == null) {
                return 0.0f;
            }
            return xpMultiplier.multiplier;
        }

        public int getPerkBoost(PerkFactory.Perk type) {
            if (this.this$0.getDescriptor().getXPBoostMap().get(type) != null) {
                return this.this$0.getDescriptor().getXPBoostMap().get(type);
            }
            return 0;
        }

        public void setPerkBoost(PerkFactory.Perk perk, int level) {
            if (perk == null || perk == PerkFactory.Perks.None || perk == PerkFactory.Perks.MAX) {
                return;
            }
            if ((level = PZMath.clamp(level, 0, 10)) == 0) {
                this.this$0.getDescriptor().getXPBoostMap().remove(perk);
                return;
            }
            this.this$0.getDescriptor().getXPBoostMap().put(perk, level);
        }

        public int getLevel() {
            return this.level;
        }

        public void setLevel(int newlevel) {
            this.level = newlevel;
        }

        public float getTotalXp() {
            return this.totalXp;
        }

        public void AddXP(PerkFactory.Perk type, float amount) {
            IsoPlayer player;
            IsoGameCharacter isoGameCharacter = this.chr;
            if (isoGameCharacter instanceof IsoPlayer && (player = (IsoPlayer)isoGameCharacter).isLocalPlayer()) {
                this.AddXP(type, amount, true, true, false);
            }
        }

        public void AddXPHaloText(PerkFactory.Perk type, float amount) {
            IsoPlayer player;
            IsoGameCharacter isoGameCharacter = this.chr;
            if (isoGameCharacter instanceof IsoPlayer && (player = (IsoPlayer)isoGameCharacter).isLocalPlayer()) {
                this.AddXP(type, amount, true, true, false, true);
            }
        }

        public void AddXP(PerkFactory.Perk type, float amount, boolean noMultiplier) {
            IsoPlayer player;
            IsoGameCharacter isoGameCharacter = this.chr;
            if (isoGameCharacter instanceof IsoPlayer && (player = (IsoPlayer)isoGameCharacter).isLocalPlayer()) {
                this.AddXP(type, amount, true, !noMultiplier, false, false);
            }
        }

        public void AddXP(PerkFactory.Perk type, float amount, boolean noMultiplier, boolean haloText) {
            IsoPlayer player;
            IsoGameCharacter isoGameCharacter = this.chr;
            if (isoGameCharacter instanceof IsoPlayer && (player = (IsoPlayer)isoGameCharacter).isLocalPlayer()) {
                this.AddXP(type, amount, true, !noMultiplier, false, haloText);
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public void AddXPNoMultiplier(PerkFactory.Perk type, float amount) {
            XPMultiplier xpMultiplier = this.getMultiplierMap().remove(type);
            try {
                this.AddXP(type, amount);
            }
            finally {
                if (xpMultiplier != null) {
                    this.getMultiplierMap().put(type, xpMultiplier);
                }
            }
        }

        public void AddXP(PerkFactory.Perk type, float amount, boolean callLua, boolean doXPBoost, boolean remote) {
            this.AddXP(type, amount, callLua, doXPBoost, remote, false);
        }

        public void AddXP(PerkFactory.Perk type, float amount, boolean callLua, boolean doXPBoost, boolean remote, boolean haloText) {
            IsoGameCharacter isoGameCharacter;
            float newXP;
            IsoPlayer isoPlayer;
            Object info;
            if (this.this$0.isAsleep()) {
                return;
            }
            Object perk = null;
            for (int n = 0; n < PerkFactory.PerkList.size(); ++n) {
                info = PerkFactory.PerkList.get(n);
                if (((PerkFactory.Perk)info).getType() != type) continue;
                perk = info;
                break;
            }
            if (((PerkFactory.Perk)perk).getType() == PerkFactory.Perks.Fitness && (info = this.chr) instanceof IsoPlayer && !(isoPlayer = (IsoPlayer)info).getNutrition().canAddFitnessXp()) {
                return;
            }
            if (((PerkFactory.Perk)perk).getType() == PerkFactory.Perks.Strength && (info = this.chr) instanceof IsoPlayer) {
                IsoPlayer isoPlayer2 = (IsoPlayer)info;
                if (isoPlayer2.getNutrition().getProteins() > 50.0f && isoPlayer2.getNutrition().getProteins() < 300.0f) {
                    amount *= 1.5f;
                }
                if (isoPlayer2.getNutrition().getProteins() < -300.0f) {
                    amount *= 0.7f;
                }
            }
            float oldXP = this.getXP(type);
            float maxXP = ((PerkFactory.Perk)perk).getTotalXpForLevel(10);
            if (amount >= 0.0f && oldXP >= maxXP) {
                return;
            }
            float mod = 1.0f;
            if (doXPBoost) {
                boolean bDoneIt = false;
                for (Map.Entry<PerkFactory.Perk, Integer> entry : this.this$0.getDescriptor().getXPBoostMap().entrySet()) {
                    if (entry.getKey() != ((PerkFactory.Perk)perk).getType()) continue;
                    bDoneIt = true;
                    if (entry.getValue() == 0 && !this.isSkillExcludedFromSpeedReduction(entry.getKey())) {
                        mod *= 0.25f;
                        continue;
                    }
                    if (entry.getValue() == 1 && entry.getKey() == PerkFactory.Perks.Sprinting) {
                        mod *= 1.25f;
                        continue;
                    }
                    if (entry.getValue() == 1) {
                        mod *= 1.0f;
                        continue;
                    }
                    if (entry.getValue() == 2 && !this.isSkillExcludedFromSpeedIncrease(entry.getKey())) {
                        mod *= 1.33f;
                        continue;
                    }
                    if (entry.getValue() < 3 || this.isSkillExcludedFromSpeedIncrease(entry.getKey())) continue;
                    mod *= 1.66f;
                }
                if (!bDoneIt && !this.isSkillExcludedFromSpeedReduction(((PerkFactory.Perk)perk).getType())) {
                    mod = 0.25f;
                }
                if (this.this$0.characterTraits.get(CharacterTrait.FAST_LEARNER) && !this.isSkillExcludedFromSpeedIncrease(((PerkFactory.Perk)perk).getType())) {
                    mod *= 1.3f;
                }
                if (this.this$0.characterTraits.get(CharacterTrait.SLOW_LEARNER) && !this.isSkillExcludedFromSpeedReduction(((PerkFactory.Perk)perk).getType())) {
                    mod *= 0.7f;
                }
                if (this.this$0.characterTraits.get(CharacterTrait.PACIFIST)) {
                    if (((PerkFactory.Perk)perk).getType() == PerkFactory.Perks.SmallBlade || ((PerkFactory.Perk)perk).getType() == PerkFactory.Perks.LongBlade || ((PerkFactory.Perk)perk).getType() == PerkFactory.Perks.SmallBlunt || ((PerkFactory.Perk)perk).getType() == PerkFactory.Perks.Spear || ((PerkFactory.Perk)perk).getType() == PerkFactory.Perks.Blunt || ((PerkFactory.Perk)perk).getType() == PerkFactory.Perks.Axe) {
                        mod *= 0.75f;
                    } else if (((PerkFactory.Perk)perk).getType() == PerkFactory.Perks.Aiming) {
                        mod *= 0.75f;
                    }
                }
                if (this.this$0.characterTraits.get(CharacterTrait.CRAFTY) && ((PerkFactory.Perk)perk).getParent() != null && ((PerkFactory.Perk)perk).getParent() == PerkFactory.Perks.Crafting) {
                    mod *= 1.3f;
                }
                amount *= mod;
                float multiplier = this.getMultiplier(type);
                if (multiplier > 1.0f) {
                    amount *= multiplier;
                }
                amount = SandboxOptions.instance.multipliersConfig.xpMultiplierGlobalToggle.getValue() ? (amount *= (float)SandboxOptions.instance.multipliersConfig.xpMultiplierGlobal.getValue()) : (amount *= Float.parseFloat(SandboxOptions.instance.getOptionByName("MultiplierConfig." + String.valueOf(((PerkFactory.Perk)perk).getType())).asConfigOption().getValueAsString()));
            }
            if ((newXP = oldXP + amount) < 0.0f) {
                newXP = 0.0f;
                amount = -oldXP;
            }
            if (newXP > maxXP) {
                newXP = maxXP;
                amount = newXP - oldXP;
            }
            this.xpMap.put(type, Float.valueOf(newXP));
            XPMultiplier xpMultiplier = this.getMultiplierMap().get(perk);
            if (xpMultiplier != null) {
                float xpMin = ((PerkFactory.Perk)perk).getTotalXpForLevel(xpMultiplier.minLevel - 1);
                float xpMax = ((PerkFactory.Perk)perk).getTotalXpForLevel(xpMultiplier.maxLevel);
                if (oldXP >= xpMin && newXP < xpMin || oldXP < xpMax && newXP >= xpMax) {
                    this.getMultiplierMap().remove(perk);
                }
            }
            float xpForNextLevel = ((PerkFactory.Perk)perk).getTotalXpForLevel(this.chr.getPerkLevel((PerkFactory.Perk)perk) + 1);
            while (oldXP < xpForNextLevel && newXP >= xpForNextLevel) {
                this.this$0.LevelPerk(type);
                IsoGameCharacter isoGameCharacter2 = this.chr;
                if (isoGameCharacter2 instanceof IsoPlayer) {
                    IsoPlayer player = (IsoPlayer)isoGameCharacter2;
                    if (perk != PerkFactory.Perks.Strength && perk != PerkFactory.Perks.Fitness || this.chr.getPerkLevel((PerkFactory.Perk)perk) != 10) {
                        if (GameServer.server) {
                            this.this$0.sendObjectChange(IsoObjectChange.PLAY_GAIN_EXPERIENCE_LEVEL_SOUND);
                        } else {
                            player.playGainExperienceLevelSound();
                        }
                    }
                }
                if (this.chr.getPerkLevel((PerkFactory.Perk)perk) >= 10) break;
                xpForNextLevel = ((PerkFactory.Perk)perk).getTotalXpForLevel(this.chr.getPerkLevel((PerkFactory.Perk)perk) + 1);
            }
            float xpForThisLevel = ((PerkFactory.Perk)perk).getTotalXpForLevel(this.chr.getPerkLevel((PerkFactory.Perk)perk));
            while (oldXP >= xpForThisLevel && newXP < xpForThisLevel) {
                this.this$0.LoseLevel((PerkFactory.Perk)perk);
                if (this.chr.getPerkLevel((PerkFactory.Perk)perk) >= 10) break;
                xpForThisLevel = ((PerkFactory.Perk)perk).getTotalXpForLevel(this.chr.getPerkLevel((PerkFactory.Perk)perk));
            }
            if ((isoGameCharacter = this.chr) instanceof IsoPlayer) {
                IsoPlayer player = (IsoPlayer)isoGameCharacter;
                if (perk == PerkFactory.Perks.Fitness) {
                    player.getStats().set(CharacterStat.FITNESS, (float)player.getPerkLevel((PerkFactory.Perk)perk) / 5.0f - 1.0f);
                }
            }
            if (!(this.chr instanceof IsoPlayer)) {
                haloText = false;
            }
            float newXPTotal = this.getXP(type);
            if (haloText && newXPTotal > oldXP) {
                float haloAward = newXP - oldXP;
                if ((haloAward = (float)Math.round(haloAward * 10.0f) / 10.0f) > 0.0f) {
                    HaloTextHelper.addGoodText((IsoPlayer)this.chr, Translator.getText(((PerkFactory.Perk)perk).getName()) + " XP: " + haloAward, "[br/]");
                }
            }
            if (!GameClient.client) {
                LuaEventManager.triggerEventGarbage("AddXP", this.chr, type, Float.valueOf(amount));
            }
        }

        private boolean isSkillExcludedFromSpeedReduction(PerkFactory.Perk key) {
            if (key == PerkFactory.Perks.Sprinting) {
                return true;
            }
            if (key == PerkFactory.Perks.Fitness) {
                return true;
            }
            return key == PerkFactory.Perks.Strength;
        }

        private boolean isSkillExcludedFromSpeedIncrease(PerkFactory.Perk key) {
            if (key == PerkFactory.Perks.Fitness) {
                return true;
            }
            return key == PerkFactory.Perks.Strength;
        }

        public float getXP(PerkFactory.Perk type) {
            if (this.xpMap.containsKey(type)) {
                return this.xpMap.get(type).floatValue();
            }
            return 0.0f;
        }

        @Deprecated
        public void AddXP(HandWeapon weapon, int amount) {
        }

        public void setTotalXP(float xp) {
            this.totalXp = xp;
        }

        private void savePerk(ByteBuffer output, PerkFactory.Perk perk) throws IOException {
            GameWindow.WriteString(output, perk == null ? "" : perk.getId());
        }

        private PerkFactory.Perk loadPerk(ByteBuffer input, int worldVersion) throws IOException {
            String perkName = GameWindow.ReadString(input);
            PerkFactory.Perk perk = PerkFactory.Perks.FromString(perkName);
            return perk == PerkFactory.Perks.MAX ? null : perk;
        }

        public void load(ByteBuffer input, int worldVersion) throws IOException {
            this.chr.characterTraits.load(input);
            this.totalXp = input.getFloat();
            this.level = input.getInt();
            this.lastlevel = input.getInt();
            this.xpMap.clear();
            int x = input.getInt();
            for (int n = 0; n < x; ++n) {
                PerkFactory.Perk perk = this.loadPerk(input, worldVersion);
                float xp = input.getFloat();
                if (perk == null) continue;
                this.xpMap.put(perk, Float.valueOf(xp));
            }
            this.this$0.perkList.clear();
            int nperks = input.getInt();
            for (int n = 0; n < nperks; ++n) {
                PerkFactory.Perk p = this.loadPerk(input, worldVersion);
                int level = input.getInt();
                if (p == null) continue;
                PerkInfo info = new PerkInfo(this.this$0);
                info.perk = p;
                info.level = level;
                this.this$0.perkList.add(info);
            }
            int x2 = input.getInt();
            for (int n = 0; n < x2; ++n) {
                PerkFactory.Perk perks = this.loadPerk(input, worldVersion);
                float multiplier = input.getFloat();
                byte minLevel = input.get();
                byte maxLevel = input.get();
                if (perks == null) continue;
                this.addXpMultiplier(perks, multiplier, minLevel, maxLevel);
            }
            if (this.totalXp > (float)this.this$0.getXpForLevel(this.getLevel() + 1)) {
                this.setTotalXP(this.chr.getXpForLevel(this.getLevel()));
            }
            this.getGrowthRate();
        }

        public void save(ByteBuffer output) throws IOException {
            this.chr.characterTraits.save(output);
            output.putFloat(this.totalXp);
            output.putInt(this.level);
            output.putInt(this.lastlevel);
            output.putInt(this.xpMap.size());
            for (Map.Entry<PerkFactory.Perk, Float> entry : this.xpMap.entrySet()) {
                this.savePerk(output, entry.getKey());
                output.putFloat(entry.getValue().floatValue());
            }
            output.putInt(this.this$0.perkList.size());
            for (int n = 0; n < this.this$0.perkList.size(); ++n) {
                PerkInfo perkInfo = this.this$0.perkList.get(n);
                this.savePerk(output, perkInfo.perk);
                output.putInt(perkInfo.level);
            }
            output.putInt(this.xpMapMultiplier.size());
            for (Map.Entry<PerkFactory.Perk, XPMultiplier> entry : this.xpMapMultiplier.entrySet()) {
                this.savePerk(output, entry.getKey());
                output.putFloat(entry.getValue().multiplier);
                output.put((byte)entry.getValue().minLevel);
                output.put((byte)entry.getValue().maxLevel);
            }
        }

        public void setXPToLevel(PerkFactory.Perk key, int perkLevel) {
            PerkFactory.Perk perk = null;
            for (int n = 0; n < PerkFactory.PerkList.size(); ++n) {
                PerkFactory.Perk info = PerkFactory.PerkList.get(n);
                if (info.getType() != key) continue;
                perk = info;
                break;
            }
            if (perk != null) {
                this.xpMap.put(key, Float.valueOf(perk.getTotalXpForLevel(perkLevel)));
                if (Core.debug && perk == PerkFactory.Perks.Fitness) {
                    this.this$0.getStats().set(CharacterStat.FITNESS, (float)perkLevel / 5.0f - 1.0f);
                }
            }
        }
    }

    private static final class L_getDotWithForwardDirection {
        private static final Vector2 v1 = new Vector2();
        private static final Vector2 v2 = new Vector2();

        private L_getDotWithForwardDirection() {
        }
    }

    @UsedFromLua
    public class PerkInfo {
        public int level;
        public PerkFactory.Perk perk;

        public PerkInfo(IsoGameCharacter this$0) {
            Objects.requireNonNull(this$0);
        }

        public int getLevel() {
            return this.level;
        }
    }

    private static class ReadBook {
        private String fullType;
        private int alreadyReadPages;

        private ReadBook() {
        }
    }

    private static final class L_renderShadow {
        static final ShadowParams shadowParams = new ShadowParams(1.0f, 1.0f, 1.0f);
        static final Vector2 vector2_1 = new Vector2();
        static final Vector2 vector2_2 = new Vector2();
        static final Vector3f forward = new Vector3f();
        static final Vector3 vector3 = new Vector3();
        static final Vector3f vector3f = new Vector3f();

        private L_renderShadow() {
        }
    }

    private static final class L_renderLast {
        private static final Color color = new Color();

        private L_renderLast() {
        }
    }

    protected static final class l_testDotSide {
        private static final Vector2 v1 = new Vector2();
        private static final Vector2 v2 = new Vector2();
        private static final Vector2 v3 = new Vector2();

        protected l_testDotSide() {
        }
    }

    public static class TorchInfo {
        private static final ObjectPool<TorchInfo> TorchInfoPool = new ObjectPool<TorchInfo>(TorchInfo::new);
        private static final Vector3f tempVector3f = new Vector3f();
        public int id;
        public float x;
        public float y;
        public float z;
        public float r;
        public float g;
        public float b;
        public float angleX;
        public float angleY;
        public float dist;
        public float strength;
        public boolean cone;
        public float dot;
        public int focusing;

        public static TorchInfo alloc() {
            return TorchInfoPool.alloc();
        }

        public static void release(TorchInfo info) {
            TorchInfoPool.release(info);
        }

        public TorchInfo set(IsoPlayer p, InventoryItem item) {
            this.x = p.getX();
            this.y = p.getY();
            this.z = p.getZ();
            this.r = 1.0f;
            this.g = 0.8235294f;
            this.b = 0.7058824f;
            Vector2 lookVector = p.getLookVector(tempVector2);
            this.angleX = lookVector.x;
            this.angleY = lookVector.y;
            this.dist = item.getLightDistance();
            this.strength = item.getLightStrength();
            this.cone = item.isTorchCone();
            this.dot = item.getTorchDot();
            this.focusing = 0;
            return this;
        }

        public TorchInfo set(VehiclePart part) {
            BaseVehicle vehicle = part.getVehicle();
            VehicleLight light = part.getLight();
            VehicleScript script = vehicle.getScript();
            Vector3f vec = tempVector3f;
            vec.set(light.offset.x * script.getExtents().x / 2.0f, 0.0f, light.offset.y * script.getExtents().z / 2.0f);
            vehicle.getWorldPos(vec, vec);
            this.x = vec.x;
            this.y = vec.y;
            this.z = vec.z;
            this.r = light.r;
            this.g = light.g;
            this.b = light.b;
            vec = vehicle.getForwardVector(vec);
            if (part.getId().contains("Rear")) {
                vec.negate();
            }
            this.angleX = vec.x;
            this.angleY = vec.z;
            this.dist = part.getLightDistance();
            this.strength = part.getLightIntensity();
            this.cone = true;
            this.dot = light.dot;
            this.focusing = (int)part.getLightFocusing();
            return this;
        }
    }

    private static class L_postUpdate {
        static final MoveDeltaModifiers moveDeltas = new MoveDeltaModifiers();

        private L_postUpdate() {
        }
    }

    private static final class L_actionStateChanged {
        private static final ArrayList<String> stateNames = new ArrayList();
        private static final ArrayList<State> states = new ArrayList();

        private L_actionStateChanged() {
        }
    }

    private static final class Bandages {
        private final HashMap<String, String> bandageTypeMap = new HashMap();
        private final THashMap<String, InventoryItem> itemMap = new THashMap();

        private Bandages() {
        }

        private String getBloodBandageType(String type) {
            Object typeBlood = this.bandageTypeMap.get(type);
            if (typeBlood == null) {
                typeBlood = type + "_Blood";
                this.bandageTypeMap.put(type, (String)typeBlood);
            }
            return typeBlood;
        }

        private void update(IsoGameCharacter chr) {
            int i;
            if (GameServer.server) {
                return;
            }
            BodyDamage bodyDamage = chr.getBodyDamage();
            WornItems wornItems = chr.getWornItems();
            if (bodyDamage == null || wornItems == null) {
                return;
            }
            assert (!(chr instanceof IsoZombie));
            this.itemMap.clear();
            for (i = 0; i < wornItems.size(); ++i) {
                InventoryItem item = wornItems.getItemByIndex(i);
                if (item == null) continue;
                this.itemMap.put(item.getFullType(), item);
            }
            for (i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); ++i) {
                String woundType;
                BodyPart bodyPart = bodyDamage.getBodyPart(BodyPartType.FromIndex(i));
                BodyPartLast bodyPartLastState = bodyDamage.getBodyPartsLastState(BodyPartType.FromIndex(i));
                String bandageType = bodyPart.getType().getBandageModel();
                if (StringUtils.isNullOrWhitespace(bandageType)) continue;
                String bandageTypeBlood = this.getBloodBandageType(bandageType);
                if (bodyPart.bandaged() && !chr.getInventory().contains(bandageType)) {
                    if (bodyPart.isBandageDirty()) {
                        this.addBandageModel(chr, bandageTypeBlood);
                    } else {
                        this.addBandageModel(chr, bandageType);
                    }
                }
                if (bodyPart.bandaged() != bodyPartLastState.bandaged() || bodyPart.isBandageDirty() != bodyPartLastState.isBandageDirty()) {
                    if (bodyPart.bandaged()) {
                        if (bodyPart.isBandageDirty()) {
                            this.removeBandageModel(chr, bandageType);
                            this.addBandageModel(chr, bandageTypeBlood);
                        } else {
                            this.removeBandageModel(chr, bandageTypeBlood);
                            this.addBandageModel(chr, bandageType);
                        }
                    } else {
                        this.removeBandageModel(chr, bandageType);
                        this.removeBandageModel(chr, bandageTypeBlood);
                    }
                }
                if (bodyPart.bitten() != bodyPartLastState.bitten()) {
                    if (bodyPart.bitten()) {
                        woundType = bodyPart.getType().getBiteWoundModel(chr.isFemale());
                        if (StringUtils.isNullOrWhitespace(woundType)) continue;
                        this.addBandageModel(chr, woundType);
                    } else {
                        this.removeBandageModel(chr, bodyPart.getType().getBiteWoundModel(chr.isFemale()));
                    }
                }
                if (bodyPart.scratched() != bodyPartLastState.scratched()) {
                    if (bodyPart.scratched()) {
                        woundType = bodyPart.getType().getScratchWoundModel(chr.isFemale());
                        if (StringUtils.isNullOrWhitespace(woundType)) continue;
                        this.addBandageModel(chr, woundType);
                    } else {
                        this.removeBandageModel(chr, bodyPart.getType().getScratchWoundModel(chr.isFemale()));
                    }
                }
                if (bodyPart.isCut() == bodyPartLastState.isCut()) continue;
                if (bodyPart.isCut()) {
                    woundType = bodyPart.getType().getCutWoundModel(chr.isFemale());
                    if (StringUtils.isNullOrWhitespace(woundType)) continue;
                    this.addBandageModel(chr, woundType);
                    continue;
                }
                this.removeBandageModel(chr, bodyPart.getType().getCutWoundModel(chr.isFemale()));
            }
        }

        private void addBandageModel(IsoGameCharacter chr, String type) {
            if (this.itemMap.containsKey(type)) {
                return;
            }
            Object item = InventoryItemFactory.CreateItem(type);
            if (!(item instanceof Clothing)) {
                return;
            }
            Clothing bandageItem = (Clothing)item;
            chr.getInventory().addItem(bandageItem);
            chr.setWornItem(bandageItem.getBodyLocation(), bandageItem);
            chr.resetModelNextFrame();
        }

        private void removeBandageModel(IsoGameCharacter chr, String bandageType) {
            InventoryItem item = this.itemMap.get(bandageType);
            if (item == null) {
                return;
            }
            chr.getWornItems().remove(item);
            chr.getInventory().Remove(item);
            chr.resetModelNextFrame();
            chr.onWornItemsChanged();
        }
    }

    public static class XPMultiplier {
        public float multiplier;
        public int minLevel;
        public int maxLevel;
    }

    public static enum BodyLocation {
        Head,
        Leg,
        Arm,
        Chest,
        Stomach,
        Foot,
        Hand;

    }
}

