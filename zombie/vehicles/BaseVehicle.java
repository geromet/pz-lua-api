/*
 * Decompiled with CFR 0.152.
 */
package zombie.vehicles;

import fmod.fmod.FMODManager;
import fmod.fmod.FMODSoundEmitter;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import fmod.fmod.IFMODParameterUpdater;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import zombie.AmbientStreamManager;
import zombie.CombatManager;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.SystemDisabler;
import zombie.UpdateSchedulerSimulationLevel;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.ai.states.animals.AnimalFalldownState;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.DummySoundEmitter;
import zombie.audio.FMODParameter;
import zombie.audio.FMODParameterList;
import zombie.audio.GameSoundClip;
import zombie.audio.parameters.ParameterVehicleBrake;
import zombie.audio.parameters.ParameterVehicleEngineCondition;
import zombie.audio.parameters.ParameterVehicleGear;
import zombie.audio.parameters.ParameterVehicleHitLocation;
import zombie.audio.parameters.ParameterVehicleLoad;
import zombie.audio.parameters.ParameterVehicleRPM;
import zombie.audio.parameters.ParameterVehicleRoadMaterial;
import zombie.audio.parameters.ParameterVehicleSkid;
import zombie.audio.parameters.ParameterVehicleSpeed;
import zombie.audio.parameters.ParameterVehicleSteer;
import zombie.audio.parameters.ParameterVehicleTireMissing;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.Side;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.Shader;
import zombie.core.physics.BallisticsController;
import zombie.core.physics.Bullet;
import zombie.core.physics.CarController;
import zombie.core.physics.PhysicsDebugRenderer;
import zombie.core.physics.RagdollController;
import zombie.core.physics.Transform;
import zombie.core.physics.WorldSimulation;
import zombie.core.properties.IsoObjectChange;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationMultiTrack;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.debug.AnimationPlayerRecorder;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.model.VehicleModelInstance;
import zombie.core.skinnedmodel.model.VehicleSubModelInstance;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureID;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.input.GameKeyboard;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.AnimalInventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Key;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.Vector2;
import zombie.iso.Vector2ObjectPool;
import zombie.iso.Vector3;
import zombie.iso.areas.SafeHouse;
import zombie.iso.fboRenderChunk.FBORenderShadows;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.RainManager;
import zombie.iso.objects.RenderEffectType;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.weather.ClimateManager;
import zombie.network.ClientServerMap;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.PassengerMap;
import zombie.network.ServerOptions;
import zombie.network.fields.IPositional;
import zombie.network.packets.INetworkPacket;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.VehiclePoly;
import zombie.pathfind.nativeCode.PathfindNative;
import zombie.popman.ObjectPool;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.popman.animal.AnimalSynchronizationManager;
import zombie.radio.ZomboidRadio;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.ItemKey;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.SoundKey;
import zombie.scripting.objects.VehiclePartModel;
import zombie.scripting.objects.VehicleScript;
import zombie.statistics.StatisticCategory;
import zombie.statistics.StatisticType;
import zombie.statistics.StatisticsManager;
import zombie.ui.TextManager;
import zombie.ui.UIManager;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.lambda.Invokers;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.LightbarLightsMode;
import zombie.vehicles.LightbarSirenMode;
import zombie.vehicles.PolyPolyIntersect;
import zombie.vehicles.QuadranglesIntersection;
import zombie.vehicles.SurroundVehicle;
import zombie.vehicles.TransmissionNumber;
import zombie.vehicles.VehicleDoor;
import zombie.vehicles.VehicleEngineRPM;
import zombie.vehicles.VehicleIDMap;
import zombie.vehicles.VehicleInterpolation;
import zombie.vehicles.VehicleInterpolationData;
import zombie.vehicles.VehicleLight;
import zombie.vehicles.VehicleManager;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehiclePedestrianContactTracking;
import zombie.vehicles.VehicleType;
import zombie.vehicles.VehicleWindow;
import zombie.vehicles.VehiclesDB2;

@UsedFromLua
public final class BaseVehicle
extends IsoMovingObject
implements Thumpable,
IFMODParameterUpdater,
IPositional {
    public static final int MASK1_FRONT = 0;
    public static final int MASK1_REAR = 4;
    public static final int MASK1_DOOR_RIGHT_FRONT = 8;
    public static final int MASK1_DOOR_RIGHT_REAR = 12;
    public static final int MASK1_DOOR_LEFT_FRONT = 1;
    public static final int MASK1_DOOR_LEFT_REAR = 5;
    public static final int MASK1_WINDOW_RIGHT_FRONT = 9;
    public static final int MASK1_WINDOW_RIGHT_REAR = 13;
    public static final int MASK1_WINDOW_LEFT_FRONT = 2;
    public static final int MASK1_WINDOW_LEFT_REAR = 6;
    public static final int MASK1_WINDOW_FRONT = 10;
    public static final int MASK1_WINDOW_REAR = 14;
    public static final int MASK1_GUARD_RIGHT_FRONT = 3;
    public static final int MASK1_GUARD_RIGHT_REAR = 7;
    public static final int MASK1_GUARD_LEFT_FRONT = 11;
    public static final int MASK1_GUARD_LEFT_REAR = 15;
    public static final int MASK2_ROOF = 0;
    public static final int MASK2_LIGHT_RIGHT_FRONT = 4;
    public static final int MASK2_LIGHT_LEFT_FRONT = 8;
    public static final int MASK2_LIGHT_RIGHT_REAR = 12;
    public static final int MASK2_LIGHT_LEFT_REAR = 1;
    public static final int MASK2_BRAKE_RIGHT = 5;
    public static final int MASK2_BRAKE_LEFT = 9;
    public static final int MASK2_LIGHTBAR_RIGHT = 13;
    public static final int MASK2_LIGHTBAR_LEFT = 2;
    public static final int MASK2_HOOD = 6;
    public static final int MASK2_BOOT = 10;
    public static final float PHYSICS_Z_SCALE = 0.8164967f;
    public static final float RADIUS = 0.3f;
    public static final float PLUS_RADIUS = 0.15f;
    public static final int FADE_DISTANCE = 15;
    public static final int RANDOMIZE_CONTAINER_CHANCE = 100;
    public static final byte noAuthorization = -1;
    private static final Vector3f _UNIT_Y = new Vector3f(0.0f, 1.0f, 0.0f);
    private static final VehiclePoly tempPoly = new VehiclePoly();
    public static final boolean YURI_FORCE_FIELD = false;
    public static boolean renderToTexture;
    public static float centerOfMassMagic;
    private static final float[] wheelParams;
    private static final float[] physicsParams;
    private final float forcedFriction = -1.0f;
    public static Texture vehicleShadow;
    private static final ColorInfo inf;
    private static final float[] lowRiderParam;
    private final VehicleImpulse impulseFromServer = new VehicleImpulse();
    private final VehicleImpulse[] impulsesFromSquishedBodies = new VehicleImpulse[4];
    private final ArrayList<VehicleImpulse> impulsesFromHitObjects = new ArrayList();
    private final int netPlayerTimeoutMax = 30;
    public final ArrayList<ModelInfo> models = new ArrayList();
    public IsoChunk chunk;
    public boolean polyDirty = true;
    private boolean polyGarageCheck = true;
    private float radiusReductionInGarage;
    public short vehicleId = (short)-1;
    public int sqlId = -1;
    public boolean serverRemovedFromWorld;
    public VehicleInterpolation interpolation;
    public boolean waitFullUpdate;
    public float throttle;
    public double engineSpeed;
    public TransmissionNumber transmissionNumber;
    public final UpdateLimit transmissionChangeTime = new UpdateLimit(1000L);
    public boolean hasExtendOffset = true;
    public boolean hasExtendOffsetExiting;
    public float savedPhysicsZ = Float.NaN;
    public final Quaternionf savedRot = new Quaternionf();
    public final Transform jniTransform = new Transform();
    private float jniSpeed;
    public boolean jniIsCollide;
    public final Vector3f jniLinearVelocity = new Vector3f();
    private final Vector3f lastLinearVelocity = new Vector3f();
    public Authorization netPlayerAuthorization = Authorization.Server;
    public short netPlayerId = (short)-1;
    public int netPlayerTimeout;
    public int authSimulationHash;
    public long authSimulationTime;
    public int frontEndDurability = 100;
    public int rearEndDurability = 100;
    public float rust;
    public float colorHue;
    public float colorSaturation;
    public float colorValue;
    public int currentFrontEndDurability = 100;
    public int currentRearEndDurability = 100;
    public float collideX = -1.0f;
    public float collideY = -1.0f;
    public final VehiclePoly shadowCoord = new VehiclePoly();
    public engineStateTypes engineState = engineStateTypes.Idle;
    public long engineLastUpdateStateTime;
    public static final int MAX_WHEELS = 4;
    public static final int PHYSICS_PARAM_COUNT = 27;
    public final WheelInfo[] wheelInfo = new WheelInfo[4];
    public boolean skidding;
    public long skidSound;
    public long ramSound;
    public long ramSoundTime;
    private VehicleEngineRPM vehicleEngineRpm;
    public final long[] newEngineSoundId = new long[8];
    private long combinedEngineSound;
    public int engineSoundIndex;
    public BaseSoundEmitter alarmEmitter;
    public BaseSoundEmitter hornEmitter;
    public float startTime;
    public boolean headlightsOn;
    public boolean stoplightsOn;
    public boolean windowLightsOn;
    public boolean soundAlarmOn;
    public boolean soundHornOn;
    public boolean soundBackMoveOn;
    public boolean previouslyEntered;
    public boolean previouslyMoved;
    public final LightbarLightsMode lightbarLightsMode = new LightbarLightsMode();
    public final LightbarSirenMode lightbarSirenMode = new LightbarSirenMode();
    private final IsoLightSource leftLight1 = new IsoLightSource(0, 0, 0, 0.0f, 0.0f, 1.0f, 8);
    private final IsoLightSource leftLight2 = new IsoLightSource(0, 0, 0, 0.0f, 0.0f, 1.0f, 8);
    private final IsoLightSource rightLight1 = new IsoLightSource(0, 0, 0, 1.0f, 0.0f, 0.0f, 8);
    private final IsoLightSource rightLight2 = new IsoLightSource(0, 0, 0, 1.0f, 0.0f, 0.0f, 8);
    private int leftLightIndex = -1;
    private int rightLightIndex = -1;
    public final ServerVehicleState[] connectionState = new ServerVehicleState[512];
    private Passenger[] passengers = new Passenger[1];
    private String scriptName;
    protected VehicleScript script;
    protected final ArrayList<VehiclePart> parts = new ArrayList();
    private VehiclePart battery;
    protected int engineQuality;
    protected int engineLoudness;
    protected int enginePower;
    private long engineCheckTime;
    private final ArrayList<VehiclePart> lights = new ArrayList();
    private boolean createdModel;
    private int skinIndex = -1;
    protected CarController physics;
    private boolean created;
    private final VehiclePoly poly = new VehiclePoly();
    private final VehiclePoly polyPlusRadius = new VehiclePoly();
    protected boolean doDamageOverlay;
    private boolean loaded;
    public short updateFlags;
    private long updateLockTimeout;
    private final UpdateLimit limitPhysicSend = new UpdateLimit(300L);
    private Vector2 limitPhysicPositionSent;
    protected final UpdateLimit limitPhysicValid = new UpdateLimit(1000L);
    private final UpdateLimit limitCrash = new UpdateLimit(600L);
    public boolean addedToWorld;
    private boolean removedFromWorld;
    private float polyPlusRadiusMinX = -123.0f;
    private float polyPlusRadiusMinY;
    private float polyPlusRadiusMaxX;
    private float polyPlusRadiusMaxY;
    private float maxSpeed;
    private boolean keyIsOnDoor;
    private boolean hotwired;
    private boolean hotwiredBroken;
    private boolean keysInIgnition;
    public ItemContainer ignitionSwitch = new ItemContainer();
    public int keysContainerId = -1;
    private long soundAlarm = -1L;
    private long soundHorn = -1L;
    private long soundScrapePastPlant = -1L;
    private boolean hittingPlant;
    private long soundBackMoveSignal = -1L;
    public long soundSirenSignal = -1L;
    public long doorAlarmSound;
    private boolean handBrakeActive;
    private long handBrakeSound;
    private final HashMap<String, String> choosenParts = new HashMap();
    private String type = "";
    private String respawnZone;
    private float mass;
    private float initialMass;
    private float brakingForce;
    private float baseQuality;
    private float currentSteering;
    private boolean isBraking;
    private int mechanicalId;
    private boolean needPartsUpdate;
    private boolean alarmed;
    private double alarmStartTime;
    private float alarmAccumulator;
    private String chosenAlarmSound;
    private double sirenStartTime;
    private boolean mechanicUiOpen;
    private boolean isGoodCar;
    private InventoryItem currentKey;
    private boolean doColor = true;
    private float breakingSlowFactor;
    private final ArrayList<IsoObject> breakingObjectsList = new ArrayList();
    private final UpdateLimit limitUpdate = new UpdateLimit(333L);
    public byte keySpawned;
    public final Matrix4f vehicleTransform = new Matrix4f();
    public final Matrix4f renderTransform = new Matrix4f();
    private BaseSoundEmitter emitter;
    private float brakeBetweenUpdatesSpeed;
    public long physicActiveCheck = -1L;
    public long constraintChangedTime = -1L;
    private AnimationPlayer animPlayer;
    public String specificDistributionId;
    private boolean addThumpWorldSound;
    private final SurroundVehicle surroundVehicle = new SurroundVehicle(this);
    private boolean regulator;
    private float regulatorSpeed;
    private static final HashMap<String, Integer> s_PartToMaskMap;
    private static final Byte BYTE_ZERO;
    private final HashMap<String, Byte> bloodIntensity = new HashMap();
    private boolean optionBloodDecals;
    private BaseVehicle vehicleTowing;
    private BaseVehicle vehicleTowedBy;
    public int constraintTowing = -1;
    private int vehicleTowingId = -1;
    private int vehicleTowedById = -1;
    private String towAttachmentSelf;
    private String towAttachmentOther;
    private float rowConstraintZOffset;
    private final ParameterVehicleBrake parameterVehicleBrake = new ParameterVehicleBrake(this);
    private final ParameterVehicleEngineCondition parameterVehicleEngineCondition = new ParameterVehicleEngineCondition(this);
    private final ParameterVehicleGear parameterVehicleGear = new ParameterVehicleGear(this);
    private final ParameterVehicleLoad parameterVehicleLoad = new ParameterVehicleLoad(this);
    private final ParameterVehicleRoadMaterial parameterVehicleRoadMaterial = new ParameterVehicleRoadMaterial(this);
    private final ParameterVehicleRPM parameterVehicleRpm = new ParameterVehicleRPM(this);
    private final ParameterVehicleSkid parameterVehicleSkid = new ParameterVehicleSkid(this);
    private final ParameterVehicleSpeed parameterVehicleSpeed = new ParameterVehicleSpeed(this);
    private final ParameterVehicleSteer parameterVehicleSteer = new ParameterVehicleSteer(this);
    private final ParameterVehicleTireMissing parameterVehicleTireMissing = new ParameterVehicleTireMissing(this);
    private final FMODParameterList fmodParameters = new FMODParameterList();
    public boolean isActive;
    public boolean isStatic;
    private final UpdateLimit physicReliableLimit = new UpdateLimit(500L);
    public boolean isReliable;
    public ArrayList<IsoAnimal> animals = new ArrayList();
    private float totalAnimalSize;
    private final float keySpawnChancedD100;
    public float timeSinceLastAuth;
    private final UpdateLimit updateAnimal;
    private final HitVars hitVars;
    private long zombieHitTimestamp;
    private int createPhysicsRecursion;
    public static final ThreadLocal<TransformPool> TL_transform_pool;
    public static final ThreadLocal<Vector3ObjectPool> TL_vector3_pool;
    public static final ThreadLocal<Vector2fObjectPool> TL_vector2f_pool;
    public static final ThreadLocal<Vector3fObjectPool> TL_vector3f_pool;
    public static final ThreadLocal<Vector4fObjectPool> TL_vector4f_pool;
    public static final ThreadLocal<Matrix4fObjectPool> TL_matrix4f_pool;
    public static final ThreadLocal<QuaternionfObjectPool> TL_quaternionf_pool;
    private IsoGameCharacter lastDrivenBy;
    private IsoGameCharacter lastDamagedBy;
    private final VehiclePedestrianContactTracking pedestrianContacts;

    public int getSqlId() {
        return this.sqlId;
    }

    public static Matrix4f allocMatrix4f() {
        return (Matrix4f)TL_matrix4f_pool.get().alloc();
    }

    public static void releaseMatrix4f(Matrix4f v) {
        TL_matrix4f_pool.get().release(v);
    }

    public static Quaternionf allocQuaternionf() {
        return (Quaternionf)TL_quaternionf_pool.get().alloc();
    }

    public static void releaseQuaternionf(Quaternionf q) {
        TL_quaternionf_pool.get().release(q);
    }

    public static Transform allocTransform() {
        return (Transform)TL_transform_pool.get().alloc();
    }

    public static void releaseTransform(Transform t) {
        TL_transform_pool.get().release(t);
    }

    public static Vector2 allocVector2() {
        return (Vector2)Vector2ObjectPool.get().alloc();
    }

    public static void releaseVector2(Vector2 v) {
        Vector2ObjectPool.get().release(v);
    }

    public static Vector3 allocVector3() {
        return (Vector3)TL_vector3_pool.get().alloc();
    }

    public static void releaseVector3(Vector3 v) {
        TL_vector3_pool.get().release(v);
    }

    public static Vector2f allocVector2f() {
        return (Vector2f)TL_vector2f_pool.get().alloc();
    }

    public static void releaseVector2f(Vector2f vector2f) {
        TL_vector2f_pool.get().release(vector2f);
    }

    public static Vector3f allocVector3f() {
        return (Vector3f)TL_vector3f_pool.get().alloc();
    }

    public static void releaseVector4f(Vector4f vector4f) {
        TL_vector4f_pool.get().release(vector4f);
    }

    public static Vector4f allocVector4f() {
        return (Vector4f)TL_vector4f_pool.get().alloc();
    }

    public static void releaseVector3f(Vector3f vector3f) {
        TL_vector3f_pool.get().release(vector3f);
    }

    public BaseVehicle(IsoCell cell) {
        super(false);
        int i;
        this.keySpawnChancedD100 = (float)SandboxOptions.getInstance().keyLootNew.getValue() * 25.0f;
        this.timeSinceLastAuth = 30.0f;
        this.updateAnimal = new UpdateLimit(2100L);
        this.hitVars = new HitVars();
        this.pedestrianContacts = new VehiclePedestrianContactTracking();
        this.setCollidable(false);
        this.respawnZone = "";
        this.scriptName = "Base.PickUpTruck";
        this.passengers[0] = new Passenger();
        this.waitFullUpdate = false;
        this.savedRot.w = 1.0f;
        for (i = 0; i < this.wheelInfo.length; ++i) {
            this.wheelInfo[i] = new WheelInfo();
        }
        if (GameClient.client) {
            this.interpolation = new VehicleInterpolation();
        }
        this.setKeyId(Rand.Next(100000000));
        this.engineSpeed = 0.0;
        this.transmissionNumber = TransmissionNumber.N;
        this.rust = Rand.Next(0, 2);
        this.jniIsCollide = false;
        for (i = 0; i < 4; ++i) {
            BaseVehicle.lowRiderParam[i] = 0.0f;
        }
        this.fmodParameters.add(this.parameterVehicleBrake);
        this.fmodParameters.add(this.parameterVehicleEngineCondition);
        this.fmodParameters.add(this.parameterVehicleGear);
        this.fmodParameters.add(this.parameterVehicleLoad);
        this.fmodParameters.add(this.parameterVehicleRpm);
        this.fmodParameters.add(this.parameterVehicleRoadMaterial);
        this.fmodParameters.add(this.parameterVehicleSkid);
        this.fmodParameters.add(this.parameterVehicleSpeed);
        this.fmodParameters.add(this.parameterVehicleSteer);
        this.fmodParameters.add(this.parameterVehicleTireMissing);
    }

    public static void LoadAllVehicleTextures() {
        DebugLog.Vehicle.println("BaseVehicle.LoadAllVehicleTextures...");
        ArrayList<VehicleScript> scripts = ScriptManager.instance.getAllVehicleScripts();
        for (VehicleScript script : scripts) {
            BaseVehicle.LoadVehicleTextures(script);
        }
    }

    public static void LoadVehicleTextures(VehicleScript script) {
        if (SystemDisabler.doVehiclesWithoutTextures) {
            VehicleScript.Skin skin = script.getSkin(0);
            skin.textureData = BaseVehicle.LoadVehicleTexture(skin.texture);
            skin.textureDataMask = BaseVehicle.LoadVehicleTexture("vehicles_placeholder_mask");
            skin.textureDataDamage1Overlay = BaseVehicle.LoadVehicleTexture("vehicles_placeholder_damage1overlay");
            skin.textureDataDamage1Shell = BaseVehicle.LoadVehicleTexture("vehicles_placeholder_damage1shell");
            skin.textureDataDamage2Overlay = BaseVehicle.LoadVehicleTexture("vehicles_placeholder_damage2overlay");
            skin.textureDataDamage2Shell = BaseVehicle.LoadVehicleTexture("vehicles_placeholder_damage2shell");
            skin.textureDataLights = BaseVehicle.LoadVehicleTexture("vehicles_placeholder_lights");
            skin.textureDataRust = BaseVehicle.LoadVehicleTexture("vehicles_placeholder_rust");
        } else {
            for (int i = 0; i < script.getSkinCount(); ++i) {
                VehicleScript.Skin skin = script.getSkin(i);
                skin.copyMissingFrom(script.getTextures());
                BaseVehicle.LoadVehicleTextures(skin);
            }
        }
    }

    private static void LoadVehicleTextures(VehicleScript.Skin skin) {
        skin.textureData = BaseVehicle.LoadVehicleTexture(skin.texture);
        if (skin.textureMask != null) {
            int flags = 0;
            skin.textureDataMask = BaseVehicle.LoadVehicleTexture(skin.textureMask, flags |= 0x100);
        }
        skin.textureDataDamage1Overlay = BaseVehicle.LoadVehicleTexture(skin.textureDamage1Overlay);
        skin.textureDataDamage1Shell = BaseVehicle.LoadVehicleTexture(skin.textureDamage1Shell);
        skin.textureDataDamage2Overlay = BaseVehicle.LoadVehicleTexture(skin.textureDamage2Overlay);
        skin.textureDataDamage2Shell = BaseVehicle.LoadVehicleTexture(skin.textureDamage2Shell);
        skin.textureDataLights = BaseVehicle.LoadVehicleTexture(skin.textureLights);
        skin.textureDataRust = BaseVehicle.LoadVehicleTexture(skin.textureRust);
        skin.textureDataShadow = BaseVehicle.LoadVehicleTexture(skin.textureShadow);
    }

    public static Texture LoadVehicleTexture(String name) {
        int flags = 0;
        flags |= TextureID.useCompression ? 4 : 0;
        return BaseVehicle.LoadVehicleTexture(name, flags |= 0x100);
    }

    public static Texture LoadVehicleTexture(String name, int flags) {
        if (StringUtils.isNullOrWhitespace(name)) {
            return null;
        }
        return Texture.getSharedTexture("media/textures/" + name + ".png", flags);
    }

    public void setNetPlayerAuthorization(Authorization netPlayerAuthorization, int netPlayerId) {
        this.netPlayerAuthorization = netPlayerAuthorization;
        this.timeSinceLastAuth = 10.0f;
        this.netPlayerId = (short)netPlayerId;
        int n = this.netPlayerTimeout = netPlayerId == -1 ? 0 : 30;
        if (GameClient.client) {
            boolean isLocal = Authorization.Local == netPlayerAuthorization || Authorization.LocalCollide == netPlayerAuthorization;
            this.setPhysicsActive(isLocal);
        }
        DebugLog.Vehicle.trace("vid%s=%d pid=%d %s", this.getVehicleTowing() != null ? "-a" : (this.getVehicleTowedBy() != null ? "-b" : ""), this.getId(), netPlayerId, netPlayerAuthorization.name());
    }

    public boolean isNetPlayerAuthorization(Authorization netPlayerAuthorization) {
        return this.netPlayerAuthorization == netPlayerAuthorization;
    }

    public boolean isNetPlayerId(short netPlayerId) {
        return this.netPlayerId == netPlayerId;
    }

    public short getNetPlayerId() {
        return this.netPlayerId;
    }

    public String getAuthorizationDescription() {
        return String.format("vid:%s(%d) pid:(%d) auth=%s static=%b active=%b", this.scriptName, this.vehicleId, this.netPlayerId, this.netPlayerAuthorization.name(), this.isStatic, this.isActive);
    }

    public static float getFakeSpeedModifier() {
        if (GameClient.client || GameServer.server) {
            float limit = (float)ServerOptions.instance.speedLimit.getValue();
            return 120.0f / Math.min(limit, 120.0f);
        }
        return 1.0f;
    }

    public boolean isLocalPhysicSim() {
        if (GameServer.server) {
            return this.isNetPlayerAuthorization(Authorization.Server);
        }
        return this.isNetPlayerAuthorization(Authorization.LocalCollide) || this.isNetPlayerAuthorization(Authorization.Local);
    }

    public void addImpulse(Vector3f impulse, Vector3f relPos) {
        if (!this.impulseFromServer.enable) {
            this.impulseFromServer.enable = true;
            this.impulseFromServer.impulse.set(impulse);
            this.impulseFromServer.relPos.set(relPos);
        } else if (this.impulseFromServer.impulse.length() < impulse.length()) {
            this.impulseFromServer.impulse.set(impulse);
            this.impulseFromServer.relPos.set(relPos);
            this.impulseFromServer.enable = false;
            this.impulseFromServer.release();
        }
    }

    public double getEngineSpeed() {
        return this.engineSpeed;
    }

    public String getTransmissionNumberLetter() {
        return this.transmissionNumber.getString();
    }

    public int getTransmissionNumber() {
        return this.transmissionNumber.getIndex();
    }

    public void setClientForce(float force) {
        this.physics.clientForce = force;
    }

    public float getClientForce() {
        return this.physics.clientForce;
    }

    public float getForce() {
        return this.physics.engineForce - this.physics.brakingForce;
    }

    private void doVehicleColor() {
        if (!this.isDoColor()) {
            this.colorSaturation = 0.1f;
            this.colorValue = 0.9f;
            return;
        }
        this.colorHue = Rand.Next(0.0f, 0.0f);
        this.colorSaturation = 0.5f;
        this.colorValue = Rand.Next(0.3f, 0.6f);
        int rng = Rand.Next(100);
        if (rng < 20) {
            this.colorHue = Rand.Next(0.0f, 0.03f);
            this.colorSaturation = Rand.Next(0.85f, 1.0f);
            this.colorValue = Rand.Next(0.55f, 0.85f);
        } else if (rng < 32) {
            this.colorHue = Rand.Next(0.55f, 0.61f);
            this.colorSaturation = Rand.Next(0.85f, 1.0f);
            this.colorValue = Rand.Next(0.65f, 0.75f);
        } else if (rng < 67) {
            this.colorHue = 0.15f;
            this.colorSaturation = Rand.Next(0.0f, 0.1f);
            this.colorValue = Rand.Next(0.7f, 0.8f);
        } else if (rng < 89) {
            this.colorHue = Rand.Next(0.0f, 1.0f);
            this.colorSaturation = Rand.Next(0.0f, 0.1f);
            this.colorValue = Rand.Next(0.1f, 0.25f);
        } else {
            this.colorHue = Rand.Next(0.0f, 1.0f);
            this.colorSaturation = Rand.Next(0.6f, 0.75f);
            this.colorValue = Rand.Next(0.3f, 0.7f);
        }
        if (this.getScript() != null) {
            if (this.getScript().getForcedHue() > -1.0f) {
                this.colorHue = this.getScript().getForcedHue();
            }
            if (this.getScript().getForcedSat() > -1.0f) {
                this.colorSaturation = this.getScript().getForcedSat();
            }
            if (this.getScript().getForcedVal() > -1.0f) {
                this.colorValue = this.getScript().getForcedVal();
            }
        }
    }

    @Override
    public String getObjectName() {
        return "Vehicle";
    }

    public void createPhysics() {
        this.createPhysics(false);
    }

    public void createPhysics(boolean spawnSwap) {
        if (!GameClient.client && this.vehicleId == -1) {
            this.vehicleId = VehicleIDMap.instance.allocateID();
            if (GameServer.server) {
                VehicleManager.instance.registerVehicle(this);
            } else {
                VehicleIDMap.instance.put(this.vehicleId, this);
            }
        }
        if (this.script == null) {
            this.setScript(this.scriptName);
        }
        try {
            ++this.createPhysicsRecursion;
            if (this.createPhysicsRecursion == 1) {
                if (!spawnSwap) {
                    LuaEventManager.triggerEvent("OnSpawnVehicleStart", this);
                }
                if (this.physics != null) {
                    return;
                }
            }
        }
        finally {
            --this.createPhysicsRecursion;
        }
        if (this.script == null) {
            return;
        }
        if (this.skinIndex == -1) {
            this.setSkinIndex(Rand.Next(this.getSkinCount()));
        }
        if (!GameServer.server) {
            WorldSimulation.instance.create();
        }
        this.jniTransform.origin.set(this.getX() - WorldSimulation.instance.offsetX, Float.isNaN(this.savedPhysicsZ) ? this.getZ() : this.savedPhysicsZ, this.getY() - WorldSimulation.instance.offsetY);
        this.physics = new CarController(this);
        if (!GameServer.server) {
            this.setPhysicsActive(!this.isNetPlayerAuthorization(Authorization.Remote));
        }
        this.savedPhysicsZ = Float.NaN;
        if (!this.created) {
            this.created = true;
            int recentlySurvivorVehiclesChance = 30;
            if (SandboxOptions.getInstance().recentlySurvivorVehicles.getValue() == 1) {
                recentlySurvivorVehiclesChance = 0;
            }
            if (SandboxOptions.getInstance().recentlySurvivorVehicles.getValue() == 2) {
                recentlySurvivorVehiclesChance = 10;
            }
            if (SandboxOptions.getInstance().recentlySurvivorVehicles.getValue() == 3) {
                recentlySurvivorVehiclesChance = 30;
            }
            if (SandboxOptions.getInstance().recentlySurvivorVehicles.getValue() == 4) {
                recentlySurvivorVehiclesChance = 50;
            }
            if (Rand.Next(100) < recentlySurvivorVehiclesChance) {
                this.setGoodCar(true);
            }
        }
        this.createParts();
        this.initParts();
        if (!this.createdModel) {
            ModelManager.instance.addVehicle(this);
            this.createdModel = true;
        }
        this.updateTransform();
        this.lights.clear();
        for (int i = 0; i < this.parts.size(); ++i) {
            VehiclePart part = this.parts.get(i);
            if (part.getLight() == null) continue;
            this.lights.add(part);
        }
        this.setMaxSpeed(this.getScript().maxSpeed);
        this.setInitialMass(this.getScript().getMass());
        if (!this.getCell().getVehicles().contains(this) && !this.getCell().addVehicles.contains(this)) {
            this.getCell().addVehicles.add(this);
        }
        this.square = this.getCell().getGridSquare(this.getX(), this.getY(), this.getZ());
        if (!this.shouldNotHaveLoot()) {
            this.randomizeContainers();
        }
        if (this.engineState == engineStateTypes.Running) {
            this.engineDoRunning();
        }
        this.updateTotalMass();
        this.doDamageOverlay = true;
        this.updatePartStats();
        this.mechanicalId = Rand.Next(100000);
        LuaEventManager.triggerEvent("OnSpawnVehicleEnd", this);
    }

    public boolean isPreviouslyEntered() {
        return this.previouslyEntered;
    }

    public void setPreviouslyEntered(boolean bool) {
        this.previouslyEntered = bool;
    }

    public boolean isPreviouslyMoved() {
        return this.previouslyMoved;
    }

    public void setPreviouslyMoved(boolean bool) {
        this.previouslyMoved = bool;
    }

    public boolean getKeySpawned() {
        return this.keySpawned != 0;
    }

    private InventoryContainer tryCreateKeyRing() {
        InventoryContainer keyRing;
        Object keyringItem;
        String keyRingType = ItemKey.Container.KEY_RING.toString();
        if (this.getScript().hasSpecialKeyRing() && (float)Rand.Next(100) < 1.0f * this.getSpecialKeyRingChance()) {
            keyRingType = this.getScript().getRandomSpecialKeyRing();
        }
        if ((keyringItem = InventoryItemFactory.CreateItem(keyRingType)) instanceof InventoryContainer && (keyRing = (InventoryContainer)keyringItem).getInventory() != null) {
            this.keyNamerVehicle(keyRing);
            return keyRing;
        }
        return null;
    }

    private InventoryItem tryCreateBuildingKey(BuildingDef buildingDef) {
        if (buildingDef == null || buildingDef.getKeyId() == -1) {
            return null;
        }
        ItemKey keyType = ItemKey.Key.KEY_1;
        Object key = InventoryItemFactory.CreateItem(keyType);
        if (key == null) {
            return null;
        }
        ((InventoryItem)key).setKeyId(buildingDef.getKeyId());
        IsoGridSquare square = buildingDef.getFreeSquareInRoom();
        if (square != null) {
            ItemPickerJava.KeyNamer.nameKey(key, square);
        }
        return key;
    }

    private InventoryItem randomlyAddNearestBuildingKeyToContainer(ItemContainer container) {
        float py;
        if ((float)Rand.Next(100) >= 1.0f * this.keySpawnChancedD100) {
            return null;
        }
        float px = this.getX();
        BuildingDef buildingDef = AmbientStreamManager.getNearestBuilding(px, py = this.getY());
        InventoryItem houseKey = this.tryCreateBuildingKey(buildingDef);
        if (houseKey != null) {
            container.AddItem(houseKey);
            return houseKey;
        }
        return null;
    }

    public void putKeyToZombie(IsoZombie zombie) {
        if (!zombie.shouldZombieHaveKey(true)) {
            return;
        }
        if (!this.checkZombieKeyForVehicle(zombie)) {
            return;
        }
        InventoryItem key = this.createVehicleKey();
        if (key == null) {
            return;
        }
        this.keySpawned = 1;
        if ((float)Rand.Next(100) >= 1.0f * this.keySpawnChancedD100) {
            zombie.getInventory().AddItem(key);
        } else {
            InventoryContainer keyRing = this.tryCreateKeyRing();
            if (keyRing == null) {
                zombie.getInventory().AddItem(key);
                return;
            }
            keyRing.getInventory().AddItem(key);
            this.randomlyAddNearestBuildingKeyToContainer(keyRing.getInventory());
            zombie.getInventory().AddItem(keyRing);
        }
    }

    public void putKeyToContainer(ItemContainer container, IsoGridSquare sq, IsoObject obj) {
        InventoryItem key = this.createVehicleKey();
        if (key == null) {
            return;
        }
        this.keySpawned = 1;
        if ((float)Rand.Next(100) >= 1.0f * this.keySpawnChancedD100) {
            container.AddItem(key);
            this.putKeyToContainerServer(key, sq, obj);
        } else {
            InventoryItem houseKey;
            InventoryContainer keyRing = this.tryCreateKeyRing();
            if (keyRing == null) {
                container.AddItem(key);
                this.putKeyToContainerServer(key, sq, obj);
                return;
            }
            keyRing.getInventory().AddItem(key);
            if (sq.getBuilding() != null && sq.getBuilding().getDef() != null && sq.getBuilding().getDef().getKeyId() != -1 && Rand.Next(10) != 0 && (houseKey = this.tryCreateBuildingKey(sq.getBuilding().getDef())) != null) {
                keyRing.getInventory().AddItem(houseKey);
            }
            container.AddItem(keyRing);
            this.putKeyToContainerServer(keyRing, sq, obj);
        }
    }

    public void putKeyToContainerServer(InventoryItem item, IsoGridSquare sq, IsoObject obj) {
        if (GameServer.server) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AddInventoryItemToContainer, obj.square.x, (float)obj.square.y, this.container, item);
        }
    }

    public void putKeyToWorld(IsoGridSquare sq) {
        InventoryItem key = this.createVehicleKey();
        if (key == null) {
            return;
        }
        this.keySpawned = 1;
        if ((float)Rand.Next(100) >= 1.0f * this.keySpawnChancedD100) {
            sq.AddWorldInventoryItem(key, 0.0f, 0.0f, 0.0f);
        } else {
            InventoryContainer keyRing = this.tryCreateKeyRing();
            if (keyRing == null) {
                sq.AddWorldInventoryItem(key, 0.0f, 0.0f, 0.0f);
                return;
            }
            keyRing.getInventory().AddItem(key);
            this.randomlyAddNearestBuildingKeyToContainer(keyRing.getInventory());
            sq.AddWorldInventoryItem(keyRing, 0.0f, 0.0f, 0.0f);
        }
    }

    public void addKeyToWorld() {
        this.addKeyToWorld(false);
    }

    public void addKeyToWorld(boolean crashed) {
        IsoGridSquare sq;
        if (this.isPreviouslyEntered() || this.isPreviouslyMoved() || this.isHotwired() || this.isBurnt()) {
            return;
        }
        if (this.isInTrafficJam()) {
            crashed = true;
        }
        if (!this.checkIfGoodVehicleForKey()) {
            return;
        }
        if (this.getScriptName().contains("Burnt") || this.getScriptName().equals("Trailer") || this.getScriptName().equals("TrailerAdvert")) {
            return;
        }
        if (!this.getScriptName().contains("Smashed") && this.haveOneDoorUnlocked()) {
            if ((float)Rand.Next(100) < 1.0f * this.keySpawnChancedD100) {
                this.keysInIgnition = true;
                this.currentKey = this.createVehicleKey();
                this.ignitionSwitch.addItem(this.currentKey);
                this.keySpawned = 1;
                return;
            }
            if ((float)Rand.Next(100) < 1.0f * this.keySpawnChancedD100) {
                this.addKeyToGloveBox();
                return;
            }
        }
        if ((sq = this.getCell().getGridSquare(this.getX(), this.getY(), this.getZ())) != null) {
            boolean bl = this.addKeyToSquare(sq, crashed || this.isBurntOrSmashed());
        }
    }

    public void addKeyToGloveBox() {
        if (this.keySpawned != 0) {
            return;
        }
        VehiclePart glovebox = this.getPartById("GloveBox");
        if (glovebox == null) {
            return;
        }
        InventoryItem key = this.createVehicleKey();
        if (key == null) {
            return;
        }
        this.keySpawned = 1;
        if ((float)Rand.Next(100) >= 1.0f * this.keySpawnChancedD100) {
            glovebox.container.addItem(key);
            this.randomlyAddNearestBuildingKeyToContainer(glovebox.container);
        } else {
            InventoryContainer keyRing = this.tryCreateKeyRing();
            if (keyRing == null) {
                glovebox.container.addItem(key);
                this.randomlyAddNearestBuildingKeyToContainer(glovebox.container);
                return;
            }
            keyRing.getInventory().AddItem(key);
            this.randomlyAddNearestBuildingKeyToContainer(keyRing.getInventory());
            glovebox.container.addItem(keyRing);
        }
    }

    public void addBuildingKeyToGloveBox(IsoGridSquare square) {
        VehiclePart glovebox = this.getPartById("GloveBox");
        if (glovebox == null) {
            return;
        }
        if (square.getBuilding() == null || square.getBuilding().getDef() == null) {
            return;
        }
        BuildingDef buildingDef = square.getBuilding().getDef();
        InventoryItem houseKey = this.tryCreateBuildingKey(buildingDef);
        if (houseKey == null) {
            return;
        }
        if ((float)Rand.Next(100) >= 1.0f * this.keySpawnChancedD100) {
            glovebox.container.AddItem(houseKey);
        } else {
            InventoryContainer keyRing = this.tryCreateKeyRing();
            if (keyRing == null) {
                glovebox.container.AddItem(houseKey);
                return;
            }
            keyRing.getInventory().AddItem(houseKey);
            glovebox.container.addItem(keyRing);
        }
    }

    public InventoryItem createVehicleKey() {
        Object item = InventoryItemFactory.CreateItem(ItemKey.Key.CAR_KEY);
        if (item == null) {
            return null;
        }
        ((InventoryItem)item).setKeyId(this.getKeyId());
        BaseVehicle.keyNamerVehicle(item, this);
        Color newC = Color.HSBtoRGB(this.colorHue, this.colorSaturation * 0.5f, this.colorValue);
        ((InventoryItem)item).setColor(newC);
        ((InventoryItem)item).setCustomColor(true);
        return item;
    }

    public boolean addKeyToSquare(IsoGridSquare sq) {
        return this.addKeyToSquare(sq, false);
    }

    public boolean addKeyToSquare(IsoGridSquare sq, boolean crashed) {
        boolean isKeyIssued = false;
        for (int z = 0; z < 3; ++z) {
            int x2;
            if (Rand.Next(2) == 0) {
                for (x2 = sq.getX() - 10; x2 < sq.getX() + 10; ++x2) {
                    isKeyIssued = this.addKeyToSquare2(sq, x2, crashed);
                    if (!isKeyIssued) continue;
                    return true;
                }
                continue;
            }
            for (x2 = sq.getX() + 10; x2 > sq.getX() - 10; --x2) {
                isKeyIssued = this.addKeyToSquare2(sq, x2, crashed);
                if (!isKeyIssued) continue;
                return true;
            }
        }
        if ((float)Rand.Next(100) < 1.0f * this.keySpawnChancedD100) {
            for (int i = 0; i < 100; ++i) {
                int x = sq.getX() - 10 + Rand.Next(20);
                int y = sq.getY() - 10 + Rand.Next(20);
                IsoGridSquare square = IsoWorld.instance.getCell().getGridSquare((double)x, (double)y, this.getZ());
                if (square == null || square.isSolid() || square.isSolidTrans() || square.HasTree()) continue;
                this.putKeyToWorld(square);
                isKeyIssued = true;
                return isKeyIssued;
            }
        }
        return isKeyIssued;
    }

    public boolean addKeyToSquare2(IsoGridSquare sq, int x2) {
        return this.addKeyToSquare2(sq, x2, false);
    }

    public boolean addKeyToSquare2(IsoGridSquare sq, int x2, boolean crashed) {
        boolean isKeyIssued = false;
        if (Rand.Next(100) < 50) {
            for (int y = sq.getY() - 10; y < sq.getY() + 10; ++y) {
                IsoGridSquare square = IsoWorld.instance.getCell().getGridSquare((double)x2, (double)y, this.getZ());
                if (square == null || !(isKeyIssued = this.checkSquareForVehicleKeySpot(square, crashed))) continue;
                return true;
            }
        } else {
            for (int y = sq.getY() + 10; y > sq.getY() - 10; --y) {
                IsoGridSquare square = IsoWorld.instance.getCell().getGridSquare((double)x2, (double)y, this.getZ());
                if (square == null || !(isKeyIssued = this.checkSquareForVehicleKeySpot(square, crashed))) continue;
                return true;
            }
        }
        return isKeyIssued;
    }

    public void toggleLockedDoor(VehiclePart part, IsoGameCharacter chr, boolean locked) {
        if (locked) {
            if (!this.canLockDoor(part, chr)) {
                return;
            }
            part.getDoor().setLocked(true);
        } else {
            if (!this.canUnlockDoor(part, chr)) {
                return;
            }
            part.getDoor().setLocked(false);
        }
    }

    public boolean canLockDoor(VehiclePart part, IsoGameCharacter chr) {
        VehicleWindow window;
        if (part == null) {
            return false;
        }
        if (chr == null) {
            return false;
        }
        VehicleDoor door = part.getDoor();
        if (door == null) {
            return false;
        }
        if (door.lockBroken) {
            return false;
        }
        if (door.locked) {
            return false;
        }
        if (this.getSeat(chr) != -1) {
            return true;
        }
        if (chr.getInventory().haveThisKeyId(this.getKeyId()) != null) {
            return true;
        }
        VehiclePart windowPart = part.getChildWindow();
        if (windowPart != null && windowPart.getInventoryItem() == null) {
            return true;
        }
        VehicleWindow vehicleWindow = window = windowPart == null ? null : windowPart.getWindow();
        return window != null && (window.isOpen() || window.isDestroyed());
    }

    public boolean canUnlockDoor(VehiclePart part, IsoGameCharacter chr) {
        VehicleWindow window;
        if (part == null) {
            return false;
        }
        if (chr == null) {
            return false;
        }
        VehicleDoor door = part.getDoor();
        if (door == null) {
            return false;
        }
        if (door.lockBroken) {
            return false;
        }
        if (!door.locked) {
            return false;
        }
        if (this.getSeat(chr) != -1) {
            return true;
        }
        if (chr.getInventory().haveThisKeyId(this.getKeyId()) != null) {
            return true;
        }
        VehiclePart windowPart = part.getChildWindow();
        if (windowPart != null && windowPart.getInventoryItem() == null) {
            return true;
        }
        VehicleWindow vehicleWindow = window = windowPart == null ? null : windowPart.getWindow();
        return window != null && (window.isOpen() || window.isDestroyed());
    }

    public boolean canOpenDoor(VehiclePart part, IsoGameCharacter chr) {
        VehicleWindow window;
        if (part == null) {
            return false;
        }
        if (chr == null) {
            return false;
        }
        VehicleDoor door = part.getDoor();
        if (door == null) {
            return false;
        }
        if (door.lockBroken) {
            return false;
        }
        if (!door.locked) {
            return true;
        }
        if (this.getSeat(chr) != -1) {
            return true;
        }
        if (chr.getInventory().haveThisKeyId(this.getKeyId()) != null) {
            return true;
        }
        VehiclePart windowPart = part.getChildWindow();
        if (windowPart != null && windowPart.getInventoryItem() == null) {
            return true;
        }
        VehicleWindow vehicleWindow = window = windowPart == null ? null : windowPart.getWindow();
        return window != null && (window.isOpen() || window.isDestroyed());
    }

    private void initParts() {
        for (int i = 0; i < this.parts.size(); ++i) {
            VehiclePart part = this.parts.get(i);
            String functionName = part.getLuaFunction("init");
            if (functionName == null) continue;
            this.callLuaVoid(functionName, this, part);
        }
    }

    public void setGeneralPartCondition(float baseQuality, float chanceToSpawnDamaged) {
        for (int i = 0; i < this.parts.size(); ++i) {
            VehiclePart part = this.parts.get(i);
            part.setGeneralCondition(null, baseQuality, chanceToSpawnDamaged);
        }
    }

    private void createParts() {
        for (int i = 0; i < this.parts.size(); ++i) {
            VehiclePart part = this.parts.get(i);
            ArrayList<String> itemType = part.getItemType();
            if (part.created && itemType != null && !itemType.isEmpty() && part.getInventoryItem() == null && part.getTable("install") == null) {
                part.created = false;
            } else if ((itemType == null || itemType.isEmpty()) && part.getInventoryItem() != null) {
                part.item = null;
            }
            if (part.created) continue;
            part.created = true;
            String functionName = part.getLuaFunction("create");
            if (functionName == null) {
                part.setRandomCondition(null);
                continue;
            }
            this.callLuaVoid(functionName, this, part);
            if (part.getCondition() != -1) continue;
            part.setRandomCondition(null);
        }
        if (this.hasLightbar() && this.getScript().rightSirenCol != null && this.getScript().leftSirenCol != null) {
            this.leftLight1.r = this.leftLight2.r = this.getScript().leftSirenCol.r;
            this.leftLight1.g = this.leftLight2.g = this.getScript().leftSirenCol.g;
            this.leftLight1.b = this.leftLight2.b = this.getScript().leftSirenCol.b;
            this.rightLight1.r = this.rightLight2.r = this.getScript().rightSirenCol.r;
            this.rightLight1.g = this.rightLight2.g = this.getScript().rightSirenCol.g;
            this.rightLight1.b = this.rightLight2.b = this.getScript().rightSirenCol.b;
        }
    }

    public CarController getController() {
        return this.physics;
    }

    public SurroundVehicle getSurroundVehicle() {
        return this.surroundVehicle;
    }

    public int getSkinCount() {
        return this.script.getSkinCount();
    }

    public int getSkinIndex() {
        return this.skinIndex;
    }

    public void setSkinIndex(int index) {
        if (index < 0 || index > this.getSkinCount()) {
            return;
        }
        this.skinIndex = index;
    }

    public void updateSkin() {
        if (this.sprite == null || this.sprite.modelSlot == null || this.sprite.modelSlot.model == null) {
            return;
        }
        VehicleModelInstance inst = (VehicleModelInstance)this.sprite.modelSlot.model;
        VehicleScript.Skin skin = this.script.getTextures();
        VehicleScript script = this.getScript();
        if (this.getSkinIndex() >= 0 && this.getSkinIndex() < script.getSkinCount()) {
            skin = script.getSkin(this.getSkinIndex());
        }
        inst.LoadTexture(skin.texture);
        inst.tex = skin.textureData;
        inst.textureMask = skin.textureDataMask;
        inst.textureDamage1Overlay = skin.textureDataDamage1Overlay;
        inst.textureDamage1Shell = skin.textureDataDamage1Shell;
        inst.textureDamage2Overlay = skin.textureDataDamage2Overlay;
        inst.textureDamage2Shell = skin.textureDataDamage2Shell;
        inst.textureLights = skin.textureDataLights;
        inst.textureRust = skin.textureDataRust;
        if (inst.tex != null) {
            inst.tex.bindAlways = true;
        } else {
            DebugType.Animation.error("texture not found:", this.getSkin());
        }
    }

    public Texture getShadowTexture() {
        if (this.getScript() != null) {
            VehicleScript.Skin skin = this.getScript().getTextures();
            if (this.getSkinIndex() >= 0 && this.getSkinIndex() < this.getScript().getSkinCount()) {
                skin = this.getScript().getSkin(this.getSkinIndex());
            }
            if (skin.textureDataShadow != null) {
                return skin.textureDataShadow;
            }
        }
        if (vehicleShadow == null) {
            int flags = 0;
            vehicleShadow = Texture.getSharedTexture("media/vehicleShadow.png", flags |= TextureID.useCompression ? 4 : 0);
        }
        return vehicleShadow;
    }

    public VehicleScript getScript() {
        return this.script;
    }

    public void setScript(String name) {
        int i;
        ArrayList<VehicleScript> scripts;
        if (StringUtils.isNullOrWhitespace(name)) {
            return;
        }
        this.scriptName = name;
        boolean hadScript = this.script != null;
        this.script = ScriptManager.instance.getVehicle(this.scriptName);
        if (this.script == null && !(scripts = ScriptManager.instance.getAllVehicleScripts()).isEmpty()) {
            boolean isBurnt;
            ArrayList<VehicleScript> scriptsBurnt = new ArrayList<VehicleScript>();
            for (i = 0; i < scripts.size(); ++i) {
                VehicleScript script1 = scripts.get(i);
                if (script1.getWheelCount() != 0) continue;
                scriptsBurnt.add(script1);
                scripts.remove(i--);
            }
            boolean bl = isBurnt = this.loaded && this.parts.isEmpty() || this.scriptName.contains("Burnt");
            if (isBurnt && !scriptsBurnt.isEmpty()) {
                this.script = (VehicleScript)scriptsBurnt.get(Rand.Next(scriptsBurnt.size()));
            } else if (!scripts.isEmpty()) {
                this.script = scripts.get(Rand.Next(scripts.size()));
            }
            if (this.script != null) {
                this.scriptName = this.script.getFullName();
            }
        }
        this.battery = null;
        this.models.clear();
        if (this.script != null) {
            VehiclePart part;
            this.scriptName = this.script.getFullName();
            Passenger[] oldPassengers = this.passengers;
            this.passengers = new Passenger[this.script.getPassengerCount()];
            for (int i2 = 0; i2 < this.passengers.length; ++i2) {
                this.passengers[i2] = i2 < oldPassengers.length ? oldPassengers[i2] : new Passenger();
            }
            ArrayList<VehiclePart> oldParts = new ArrayList<VehiclePart>(this.parts);
            this.parts.clear();
            for (i = 0; i < this.script.getPartCount(); ++i) {
                VehicleScript.Part scriptPart = this.script.getPart(i);
                VehiclePart part2 = null;
                for (int j = 0; j < oldParts.size(); ++j) {
                    VehiclePart oldPart = oldParts.get(j);
                    if (oldPart.getScriptPart() != null && scriptPart.id.equals(oldPart.getScriptPart().id)) {
                        part2 = oldPart;
                        break;
                    }
                    if (oldPart.partId == null || !scriptPart.id.equals(oldPart.partId)) continue;
                    part2 = oldPart;
                    break;
                }
                if (part2 == null) {
                    part2 = new VehiclePart(this);
                }
                part2.setScriptPart(scriptPart);
                part2.category = scriptPart.category;
                part2.specificItem = scriptPart.specificItem;
                part2.setDurability(scriptPart.getDurability());
                if (scriptPart.container == null || scriptPart.container.contentType != null) {
                    part2.setItemContainer(null);
                } else {
                    if (part2.getItemContainer() == null) {
                        ItemContainer container = new ItemContainer(scriptPart.id, null, this);
                        part2.setItemContainer(container);
                        container.id = 0;
                    }
                    part2.getItemContainer().capacity = scriptPart.container.capacity;
                }
                if (scriptPart.door == null) {
                    part2.door = null;
                } else if (part2.door == null) {
                    part2.door = new VehicleDoor(part2);
                    part2.door.init(scriptPart.door);
                }
                if (scriptPart.window == null) {
                    part2.window = null;
                } else if (part2.window == null) {
                    part2.window = new VehicleWindow(part2);
                    part2.window.init(scriptPart.window);
                } else {
                    part2.window.openable = scriptPart.window.openable;
                }
                part2.parent = null;
                if (part2.children != null) {
                    part2.children.clear();
                }
                this.parts.add(part2);
                if (!"Battery".equals(part2.getId())) continue;
                this.battery = part2;
            }
            for (i = 0; i < this.script.getPartCount(); ++i) {
                part = this.parts.get(i);
                VehicleScript.Part scriptPart = part.getScriptPart();
                if (scriptPart.parent == null) continue;
                part.parent = this.getPartById(scriptPart.parent);
                if (part.parent == null) continue;
                part.parent.addChild(part);
            }
            if (!hadScript && !this.loaded) {
                this.rearEndDurability = 99999;
                this.frontEndDurability = 99999;
            }
            this.frontEndDurability = Math.min(this.frontEndDurability, this.script.getFrontEndHealth());
            this.rearEndDurability = Math.min(this.rearEndDurability, this.script.getRearEndHealth());
            this.currentFrontEndDurability = this.frontEndDurability;
            this.currentRearEndDurability = this.rearEndDurability;
            for (i = 0; i < this.script.getPartCount(); ++i) {
                part = this.parts.get(i);
                part.setInventoryItem(part.item);
            }
        }
        if (!this.loaded || this.colorHue == 0.0f && this.colorSaturation == 0.0f && this.colorValue == 0.0f) {
            this.doVehicleColor();
        }
        this.surroundVehicle.reset();
    }

    @Override
    public String getScriptName() {
        return this.scriptName;
    }

    public void setScriptName(String name) {
        assert (name == null || name.contains("."));
        this.scriptName = name;
    }

    public void setScript() {
        this.setScript(this.scriptName);
    }

    public void scriptReloaded() {
        this.scriptReloaded(false);
    }

    public void scriptReloaded(boolean spawnSwap) {
        int i;
        if (this.physics != null) {
            Transform xfrm = BaseVehicle.allocTransform();
            xfrm.setIdentity();
            this.getWorldTransform(xfrm);
            xfrm.basis.getUnnormalizedRotation(this.savedRot);
            BaseVehicle.releaseTransform(xfrm);
            this.breakConstraint(false, false);
            Bullet.removeVehicle(this.vehicleId);
            this.physics = null;
        }
        if (this.createdModel) {
            ModelManager.instance.Remove(this);
            this.createdModel = false;
        }
        this.vehicleEngineRpm = null;
        for (i = 0; i < this.parts.size(); ++i) {
            VehiclePart part = this.parts.get(i);
            part.setInventoryItem(null);
            part.created = false;
        }
        this.setScript(this.scriptName);
        this.createPhysics(spawnSwap);
        if (this.script != null) {
            for (i = 0; i < this.passengers.length; ++i) {
                VehicleScript.Position pp;
                Passenger passenger = this.passengers[i];
                if (passenger == null || passenger.character == null || (pp = this.getPassengerPosition(i, "inside")) == null) continue;
                passenger.offset.set(pp.offset);
            }
        }
        this.polyDirty = true;
        if (this.isEngineRunning()) {
            this.engineDoShuttingDown();
            this.engineState = engineStateTypes.Idle;
        }
        if (this.addedToWorld) {
            if (PathfindNative.useNativeCode) {
                PathfindNative.instance.removeVehicle(this);
                PathfindNative.instance.addVehicle(this);
            } else {
                PolygonalMap2.instance.removeVehicleFromWorld(this);
                PolygonalMap2.instance.addVehicleToWorld(this);
            }
        }
    }

    public String getSkin() {
        if (this.script == null || this.script.getSkinCount() == 0) {
            return "BOGUS";
        }
        if (this.skinIndex < 0 || this.skinIndex >= this.script.getSkinCount()) {
            this.skinIndex = Rand.Next(this.script.getSkinCount());
        }
        return this.script.getSkin((int)this.skinIndex).texture;
    }

    public ModelInfo setModelVisible(VehiclePart part, VehicleScript.Model scriptModel, boolean visible) {
        ModelInfo info;
        for (int i = 0; i < this.models.size(); ++i) {
            info = this.models.get(i);
            if (info.part != part || info.scriptModel != scriptModel) continue;
            if (visible) {
                return info;
            }
            if (info.animPlayer != null) {
                info.animPlayer = Pool.tryRelease(info.animPlayer);
            }
            this.models.remove(i);
            if (this.createdModel) {
                ModelManager.instance.Remove(this);
                ModelManager.instance.addVehicle(this);
            }
            part.updateFlags = (short)(part.updateFlags | 0x40);
            this.updateFlags = (short)(this.updateFlags | 0x40);
            return null;
        }
        if (visible) {
            String modelScriptName = this.getModelScriptNameForPart(part, scriptModel);
            if (modelScriptName == null) {
                return null;
            }
            info = new ModelInfo();
            info.part = part;
            info.scriptModel = scriptModel;
            info.modelScript = ScriptManager.instance.getModelScript(modelScriptName);
            info.wheelIndex = part.getWheelIndex();
            this.models.add(info);
            if (this.createdModel) {
                ModelManager.instance.Remove(this);
                ModelManager.instance.addVehicle(this);
            }
            part.updateFlags = (short)(part.updateFlags | 0x40);
            this.updateFlags = (short)(this.updateFlags | 0x40);
            return info;
        }
        return null;
    }

    private String getModelScriptNameForPart(VehiclePart part, VehicleScript.Model scriptModel) {
        String modelScriptName = scriptModel.file;
        if (modelScriptName == null) {
            Object item = part.getInventoryItem();
            if (item == null) {
                return null;
            }
            ArrayList<VehiclePartModel> vehiclePartModels = ((InventoryItem)item).getScriptItem().getVehiclePartModels();
            if (vehiclePartModels == null || vehiclePartModels.isEmpty()) {
                return null;
            }
            for (int i = 0; i < vehiclePartModels.size(); ++i) {
                VehiclePartModel vehiclePartModel = vehiclePartModels.get(i);
                if (!vehiclePartModel.partId.equalsIgnoreCase(part.getId()) || !vehiclePartModel.partModelId.equalsIgnoreCase(scriptModel.getId())) continue;
                modelScriptName = vehiclePartModel.modelId;
                break;
            }
        }
        return modelScriptName;
    }

    private ModelInfo getModelInfoForPart(VehiclePart part) {
        for (int i = 0; i < this.models.size(); ++i) {
            ModelInfo modelInfo = this.models.get(i);
            if (modelInfo.part != part) continue;
            return modelInfo;
        }
        return null;
    }

    private VehicleScript.Passenger getScriptPassenger(int seat) {
        if (this.getScript() == null) {
            return null;
        }
        if (seat < 0 || seat >= this.getScript().getPassengerCount()) {
            return null;
        }
        return this.getScript().getPassenger(seat);
    }

    public int getMaxPassengers() {
        return this.passengers.length;
    }

    public boolean setPassenger(int seat, IsoGameCharacter chr, Vector3f offset) {
        if (seat < 0 || seat >= this.passengers.length) {
            return false;
        }
        if (seat == 0) {
            this.setNeedPartsUpdate(true);
        }
        this.passengers[seat].character = chr;
        this.passengers[seat].offset.set(offset);
        this.updateLastKnwnDriver();
        return true;
    }

    private void updateLastKnwnDriver() {
        IsoGameCharacter currentDriver = this.getDriverRegardlessOfTow();
        if (currentDriver != null) {
            this.lastDrivenBy = currentDriver;
        }
    }

    public boolean clearPassenger(int seat) {
        if (seat >= 0 && seat < this.passengers.length) {
            this.passengers[seat].character = null;
            this.passengers[seat].offset.set(0.0f, 0.0f, 0.0f);
            return true;
        }
        return false;
    }

    public boolean hasPassenger() {
        for (int i = 0; i < this.getMaxPassengers(); ++i) {
            Passenger passenger = this.getPassenger(i);
            if (passenger == null || passenger.character == null) continue;
            return true;
        }
        return false;
    }

    public Passenger getPassenger(int seat) {
        if (seat >= 0 && seat < this.passengers.length) {
            return this.passengers[seat];
        }
        return null;
    }

    public IsoGameCharacter getCharacter(int seat) {
        Passenger passenger = this.getPassenger(seat);
        if (passenger != null) {
            return passenger.character;
        }
        return null;
    }

    public int getSeat(IsoGameCharacter chr) {
        for (int i = 0; i < this.getMaxPassengers(); ++i) {
            if (this.getCharacter(i) != chr) continue;
            return i;
        }
        return -1;
    }

    public boolean isDriver(IsoGameCharacter chr) {
        return this.getSeat(chr) == 0;
    }

    public Vector3f getWorldPos(Vector3f localPos, Vector3f worldPos, VehicleScript script) {
        return this.getWorldPos(localPos.x, localPos.y, localPos.z, worldPos, script);
    }

    public Vector3f getWorldPos(float localX, float localY, float localZ, Vector3f worldPos, VehicleScript script) {
        Transform xfrm = this.getWorldTransform(BaseVehicle.allocTransform());
        xfrm.origin.set(0.0f, 0.0f, 0.0f);
        worldPos.set(localX, localY, localZ);
        xfrm.transform(worldPos);
        BaseVehicle.releaseTransform(xfrm);
        float physX = this.jniTransform.origin.x + WorldSimulation.instance.offsetX;
        float physY = this.jniTransform.origin.z + WorldSimulation.instance.offsetY;
        float physZ = this.jniTransform.origin.y / 2.44949f;
        worldPos.set(physX + worldPos.x, physY + worldPos.z, physZ + worldPos.y);
        return worldPos;
    }

    public Vector3f getWorldPos(Vector3f localPos, Vector3f worldPos) {
        return this.getWorldPos(localPos.x, localPos.y, localPos.z, worldPos, this.getScript());
    }

    public Vector3f getWorldPos(float localX, float localY, float localZ, Vector3f worldPos) {
        return this.getWorldPos(localX, localY, localZ, worldPos, this.getScript());
    }

    public Vector3f getLocalPos(Vector3f worldPos, Vector3f localPos) {
        return this.getLocalPos(worldPos.x, worldPos.y, worldPos.z, localPos);
    }

    public Vector3f getLocalPos(float worldX, float worldY, float worldZ, Vector3f localPos) {
        Transform xfrm = this.getWorldTransform(BaseVehicle.allocTransform());
        xfrm.inverse();
        localPos.set(worldX - WorldSimulation.instance.offsetX, 0.0f, worldY - WorldSimulation.instance.offsetY);
        xfrm.transform(localPos);
        BaseVehicle.releaseTransform(xfrm);
        return localPos;
    }

    public Vector3f getPassengerLocalPos(int seat, Vector3f v) {
        Passenger passenger = this.getPassenger(seat);
        if (passenger == null) {
            return null;
        }
        return v.set(this.script.getModel().getOffset()).add(passenger.offset);
    }

    public Vector3f getPassengerWorldPos(int seat, Vector3f out) {
        Passenger passenger = this.getPassenger(seat);
        if (passenger == null) {
            return null;
        }
        return this.getPassengerPositionWorldPos(passenger.offset.x, passenger.offset.y, passenger.offset.z, out);
    }

    public Vector3f getPassengerPositionWorldPos(VehicleScript.Position posn, Vector3f out) {
        return this.getPassengerPositionWorldPos(posn.offset.x, posn.offset.y, posn.offset.z, out);
    }

    public Vector3f getPassengerPositionWorldPos(float x, float y, float z, Vector3f out) {
        out.set(this.script.getModel().offset);
        out.add(x, y, z);
        this.getWorldPos(out.x, out.y, out.z, out);
        out.z = PZMath.fastfloor(this.getZ());
        return out;
    }

    public VehicleScript.Anim getPassengerAnim(int seat, String id) {
        VehicleScript.Passenger pngr = this.getScriptPassenger(seat);
        if (pngr == null) {
            return null;
        }
        for (int i = 0; i < pngr.anims.size(); ++i) {
            VehicleScript.Anim anim = pngr.anims.get(i);
            if (!id.equals(anim.id)) continue;
            return anim;
        }
        return null;
    }

    public VehicleScript.Position getPassengerPosition(int seat, String id) {
        VehicleScript.Passenger pngr = this.getScriptPassenger(seat);
        if (pngr == null) {
            return null;
        }
        return pngr.getPositionById(id);
    }

    public VehiclePart getPassengerDoor(int seat) {
        VehicleScript.Passenger pngr = this.getScriptPassenger(seat);
        if (pngr == null) {
            return null;
        }
        return this.getPartById(pngr.door);
    }

    public VehiclePart getPassengerDoor2(int seat) {
        VehicleScript.Passenger pngr = this.getScriptPassenger(seat);
        if (pngr == null) {
            return null;
        }
        return this.getPartById(pngr.door2);
    }

    public boolean isPositionOnLeftOrRight(float x, float y) {
        Vector3f v = (Vector3f)TL_vector3f_pool.get().alloc();
        this.getLocalPos(x, y, 0.0f, v);
        x = v.x;
        TL_vector3f_pool.get().release(v);
        Vector3f ext = this.script.getExtents();
        Vector3f com = this.script.getCenterOfMassOffset();
        float xMin = com.x - ext.x / 2.0f;
        float xMax = com.x + ext.x / 2.0f;
        return x < xMin * 0.98f || x > xMax * 0.98f;
    }

    public boolean haveOneDoorUnlocked() {
        for (int i = 0; i < this.getPartCount(); ++i) {
            VehiclePart part = this.getPartByIndex(i);
            if (part.getDoor() == null || !part.getId().contains("Left") && !part.getId().contains("Right") || part.getDoor().isLocked() && !part.getDoor().isOpen()) continue;
            return true;
        }
        return false;
    }

    public String getPassengerArea(int seat) {
        VehicleScript.Passenger pngr = this.getScriptPassenger(seat);
        if (pngr == null) {
            return null;
        }
        return pngr.area;
    }

    public void playPassengerAnim(int seat, String animId) {
        IsoGameCharacter chr = this.getCharacter(seat);
        this.playPassengerAnim(seat, animId, chr);
    }

    public void playPassengerAnim(int seat, String animId, IsoGameCharacter chr) {
        if (chr == null) {
            return;
        }
        VehicleScript.Anim anim = this.getPassengerAnim(seat, animId);
        if (anim == null) {
            return;
        }
        this.playCharacterAnim(chr, anim, true);
    }

    public void playPassengerSound(int seat, String animId) {
        VehicleScript.Anim anim = this.getPassengerAnim(seat, animId);
        if (anim == null || anim.sound == null) {
            return;
        }
        this.playSound(anim.sound);
    }

    public void playPartAnim(VehiclePart part, String animId) {
        AnimationTrack track;
        if (!this.parts.contains(part)) {
            return;
        }
        VehicleScript.Anim anim = part.getAnimById(animId);
        if (anim == null || StringUtils.isNullOrWhitespace(anim.anim)) {
            return;
        }
        ModelInfo modelInfo = this.getModelInfoForPart(part);
        if (modelInfo == null) {
            return;
        }
        AnimationPlayer animPlayer = modelInfo.getAnimationPlayer();
        if (animPlayer == null || !animPlayer.isReady()) {
            return;
        }
        if (animPlayer.getMultiTrack().getIndexOfTrack(modelInfo.track) != -1) {
            animPlayer.getMultiTrack().removeTrack(modelInfo.track);
        }
        modelInfo.track = null;
        SkinningData skinningData = animPlayer.getSkinningData();
        if (skinningData != null && !skinningData.animationClips.containsKey(anim.anim)) {
            return;
        }
        modelInfo.track = track = animPlayer.play(anim.anim, anim.loop);
        if (track != null) {
            track.setBlendWeight(1.0f);
            track.setSpeedDelta(anim.rate);
            track.isPlaying = anim.animate;
            track.reverse = anim.reverse;
            if (!modelInfo.modelScript.boneWeights.isEmpty()) {
                track.setBoneWeights(modelInfo.modelScript.boneWeights);
                track.initBoneWeights(skinningData);
            }
            if (part.getWindow() != null) {
                track.setCurrentTimeValue(track.getDuration() * part.getWindow().getOpenDelta());
            }
        }
    }

    public void playActorAnim(VehiclePart part, String animId, IsoGameCharacter chr) {
        if (chr == null) {
            return;
        }
        if (!this.parts.contains(part)) {
            return;
        }
        VehicleScript.Anim anim = part.getAnimById("Actor" + animId);
        if (anim == null) {
            return;
        }
        this.playCharacterAnim(chr, anim, !"EngineDoor".equals(part.getId()));
    }

    private void playCharacterAnim(IsoGameCharacter chr, VehicleScript.Anim anim, boolean snapDirection) {
        chr.PlayAnimUnlooped(anim.anim);
        chr.getSpriteDef().setFrameSpeedPerFrame(anim.rate);
        chr.getLegsSprite().animate = true;
        Vector3f angle = this.getForwardVector((Vector3f)TL_vector3f_pool.get().alloc());
        if (anim.angle.lengthSquared() != 0.0f) {
            Matrix4f m4 = (Matrix4f)TL_matrix4f_pool.get().alloc();
            m4.rotationXYZ((float)Math.toRadians(anim.angle.x), (float)Math.toRadians(anim.angle.y), (float)Math.toRadians(anim.angle.z));
            Quaternionf q = BaseVehicle.allocQuaternionf();
            angle.rotate(m4.getNormalizedRotation(q));
            BaseVehicle.releaseQuaternionf(q);
            TL_matrix4f_pool.get().release(m4);
        }
        Vector2 vector2 = (Vector2)Vector2ObjectPool.get().alloc();
        vector2.set(angle.x, angle.z);
        chr.DirectionFromVector(vector2);
        Vector2ObjectPool.get().release(vector2);
        chr.setForwardDirection(angle.x, angle.z);
        if (chr.getAnimationPlayer() != null) {
            chr.getAnimationPlayer().setTargetAngle(chr.getDirectionAngleRadians());
            if (snapDirection) {
                chr.getAnimationPlayer().setAngleToTarget();
            }
        }
        TL_vector3f_pool.get().release(angle);
    }

    public void playPartSound(VehiclePart part, IsoPlayer player, String animId) {
        if (!this.parts.contains(part)) {
            return;
        }
        VehicleScript.Anim anim = part.getAnimById(animId);
        if (anim == null || anim.sound == null) {
            return;
        }
        this.getEmitter().playSound(anim.sound, player);
    }

    public void setCharacterPosition(IsoGameCharacter chr, int seat, String positionId) {
        IsoPlayer isoPlayer;
        VehicleScript.Passenger pngr = this.getScriptPassenger(seat);
        if (pngr == null) {
            return;
        }
        VehicleScript.Position position = pngr.getPositionById(positionId);
        if (position == null) {
            return;
        }
        if (this.getCharacter(seat) == chr) {
            this.passengers[seat].offset.set(position.offset);
        } else {
            Vector3f worldPos = (Vector3f)TL_vector3f_pool.get().alloc();
            if (position.area == null) {
                this.getPassengerPositionWorldPos(position, worldPos);
            } else {
                VehicleScript.Area area = this.script.getAreaById(position.area);
                Vector2 vector2 = (Vector2)Vector2ObjectPool.get().alloc();
                Vector2 areaPos = this.areaPositionWorld4PlayerInteract(area, vector2);
                worldPos.x = areaPos.x;
                worldPos.y = areaPos.y;
                worldPos.z = PZMath.fastfloor(this.getZ());
                Vector2ObjectPool.get().release(vector2);
            }
            chr.setX(worldPos.x);
            chr.setY(worldPos.y);
            chr.setZ(worldPos.z);
            TL_vector3f_pool.get().release(worldPos);
        }
        if (chr instanceof IsoPlayer && (isoPlayer = (IsoPlayer)chr).isLocalPlayer()) {
            isoPlayer.dirtyRecalcGridStackTime = 10.0f;
        }
    }

    public void transmitCharacterPosition(int seat, String positionId) {
        if (GameClient.client) {
            INetworkPacket.send(PacketTypes.PacketType.VehiclePassengerPosition, this, seat, positionId);
        }
    }

    public void setCharacterPositionToAnim(IsoGameCharacter chr, int seat, String animId) {
        VehicleScript.Anim anim = this.getPassengerAnim(seat, animId);
        if (anim == null) {
            return;
        }
        if (this.getCharacter(seat) == chr) {
            this.passengers[seat].offset.set(anim.offset);
        } else {
            Vector3f worldPos = this.getWorldPos(anim.offset, (Vector3f)TL_vector3f_pool.get().alloc());
            chr.setX(worldPos.x);
            chr.setY(worldPos.y);
            chr.setZ(0.0f);
            TL_vector3f_pool.get().release(worldPos);
        }
    }

    public int getPassengerSwitchSeatCount(int seat) {
        VehicleScript.Passenger pngr = this.getScriptPassenger(seat);
        if (pngr == null) {
            return -1;
        }
        return pngr.switchSeats.size();
    }

    public VehicleScript.Passenger.SwitchSeat getPassengerSwitchSeat(int seat, int index) {
        VehicleScript.Passenger pngr = this.getScriptPassenger(seat);
        if (pngr == null) {
            return null;
        }
        if (index < 0 || index >= pngr.switchSeats.size()) {
            return null;
        }
        return pngr.switchSeats.get(index);
    }

    private VehicleScript.Passenger.SwitchSeat getSwitchSeat(int seatFrom, int seatTo) {
        VehicleScript.Passenger pngr = this.getScriptPassenger(seatFrom);
        if (pngr == null) {
            return null;
        }
        for (int i = 0; i < pngr.switchSeats.size(); ++i) {
            VehicleScript.Passenger.SwitchSeat switchSeat = pngr.switchSeats.get(i);
            if (switchSeat.seat != seatTo || this.getPartForSeatContainer(seatTo) == null || this.getPartForSeatContainer(seatTo).getInventoryItem() == null) continue;
            return switchSeat;
        }
        return null;
    }

    public String getSwitchSeatAnimName(int seatFrom, int seatTo) {
        VehicleScript.Passenger.SwitchSeat switchSeat = this.getSwitchSeat(seatFrom, seatTo);
        if (switchSeat == null) {
            return null;
        }
        return switchSeat.anim;
    }

    public float getSwitchSeatAnimRate(int seatFrom, int seatTo) {
        VehicleScript.Passenger.SwitchSeat switchSeat = this.getSwitchSeat(seatFrom, seatTo);
        if (switchSeat == null) {
            return 0.0f;
        }
        return switchSeat.rate;
    }

    public String getSwitchSeatSound(int seatFrom, int seatTo) {
        VehicleScript.Passenger.SwitchSeat switchSeat = this.getSwitchSeat(seatFrom, seatTo);
        if (switchSeat == null) {
            return null;
        }
        return switchSeat.sound;
    }

    public boolean canSwitchSeat(int seatFrom, int seatTo) {
        VehicleScript.Passenger.SwitchSeat switchSeat = this.getSwitchSeat(seatFrom, seatTo);
        return switchSeat != null;
    }

    public void switchSeat(IsoGameCharacter chr, int seatTo) {
        int seatFrom = this.getSeat(chr);
        if (seatFrom == -1) {
            return;
        }
        this.clearPassenger(seatFrom);
        VehicleScript.Position posInside = this.getPassengerPosition(seatTo, "inside");
        if (posInside == null) {
            Vector3f v = (Vector3f)TL_vector3f_pool.get().alloc();
            v.set(0.0f, 0.0f, 0.0f);
            this.setPassenger(seatTo, chr, v);
            TL_vector3f_pool.get().release(v);
        } else {
            this.setPassenger(seatTo, chr, posInside.offset);
        }
    }

    public void playSwitchSeatAnim(int seatFrom, int seatTo) {
        IsoGameCharacter chr = this.getCharacter(seatFrom);
        if (chr == null) {
            return;
        }
        VehicleScript.Passenger.SwitchSeat switchSeat = this.getSwitchSeat(seatFrom, seatTo);
        if (switchSeat == null) {
            return;
        }
        chr.PlayAnimUnlooped(switchSeat.anim);
        chr.getSpriteDef().setFrameSpeedPerFrame(switchSeat.rate);
        chr.getLegsSprite().animate = true;
    }

    public boolean isSeatOccupied(int seat) {
        return this.isSeatHoldingItems(seat) || this.getCharacter(seat) != null;
    }

    public boolean isSeatInstalled(int seat) {
        VehiclePart part = this.getPartForSeatContainer(seat);
        return part != null && part.getInventoryItem() != null;
    }

    public boolean isSeatHoldingItems(int seat) {
        return this.isSeatHoldingItems(this.getPartForSeatContainer(seat));
    }

    public boolean isSeatHoldingItems(VehiclePart seat) {
        if (seat == null || seat.getItemContainer() == null || seat.getItemContainer().isEmpty()) {
            return false;
        }
        return seat.getItemContainer().getContentsWeight() * 4.0f > (float)seat.getItemContainer().getCapacity();
    }

    public ArrayList<VehiclePart> getAllSeatParts() {
        return this.getAllSeatParts(new ArrayList<VehiclePart>(this.passengers.length));
    }

    public ArrayList<VehiclePart> getAllSeatParts(ArrayList<VehiclePart> results) {
        results.clear();
        results.ensureCapacity(this.passengers.length);
        for (int i = 0; i < this.passengers.length; ++i) {
            results.add(null);
        }
        for (int i = 0; i < this.parts.size(); ++i) {
            VehiclePart part = this.parts.get(i);
            int seatNumber = part.getContainerSeatNumber();
            if (seatNumber < 0) continue;
            results.set(seatNumber, part);
        }
        return results;
    }

    public boolean isPointLeftOfCenter(float x, float y) {
        Vector3f forward = this.getForwardVector(BaseVehicle.allocVector3f());
        boolean bLeft = PZMath.isLeft(this.getX(), this.getY(), this.getX() + forward.x, this.getY() + forward.z, x, y) < 0.0f;
        BaseVehicle.releaseVector3f(forward);
        return bLeft;
    }

    public int getBestSeat(IsoGameCharacter chr) {
        return -1;
    }

    public float getEnterSeatDistance(int seat, float x, float y) {
        VehicleScript.Position posOutside = this.getPassengerPosition(seat, "outside");
        if (posOutside != null) {
            Vector3f pos = this.getPassengerPositionWorldPos(posOutside, BaseVehicle.allocVector3f());
            float distSq = IsoUtils.DistanceToSquared(x, y, pos.x, pos.y);
            BaseVehicle.releaseVector3f(pos);
            return distSq;
        }
        posOutside = this.getPassengerPosition(seat, "outside2");
        if (posOutside != null) {
            Vector3f pos = this.getPassengerPositionWorldPos(posOutside, BaseVehicle.allocVector3f());
            float distSq = IsoUtils.DistanceToSquared(x, y, pos.x, pos.y);
            BaseVehicle.releaseVector3f(pos);
            return distSq;
        }
        return -1.0f;
    }

    public void updateHasExtendOffsetForExit(IsoGameCharacter chr) {
        this.hasExtendOffsetExiting = true;
        this.updateHasExtendOffset(chr);
        this.getPoly();
    }

    public void updateHasExtendOffsetForExitEnd(IsoGameCharacter chr) {
        this.hasExtendOffsetExiting = false;
        this.updateHasExtendOffset(chr);
        this.getPoly();
    }

    public void updateHasExtendOffset(IsoGameCharacter chr) {
        this.hasExtendOffset = false;
        this.hasExtendOffsetExiting = false;
    }

    public VehiclePart getUseablePart(IsoGameCharacter chr) {
        return this.getUseablePart(chr, true);
    }

    public VehiclePart getUseablePart(IsoGameCharacter chr, boolean checkDir) {
        if (chr.getVehicle() != null) {
            return null;
        }
        if (PZMath.fastfloor(this.getZ()) != PZMath.fastfloor(chr.getZ())) {
            return null;
        }
        if (chr.DistTo(this) > 6.0f) {
            return null;
        }
        VehicleScript script = this.getScript();
        if (script == null) {
            return null;
        }
        Vector3f ext = script.getExtents();
        Vector3f com = script.getCenterOfMassOffset();
        float minY = com.z - ext.z / 2.0f;
        float maxY = com.z + ext.z / 2.0f;
        Vector2 vector2 = (Vector2)Vector2ObjectPool.get().alloc();
        Vector3f vecTest = (Vector3f)TL_vector3f_pool.get().alloc();
        for (int i = 0; i < this.parts.size(); ++i) {
            Vector2 center;
            VehicleScript.Area area;
            String func;
            VehiclePart part = this.parts.get(i);
            if (part.getArea() == null || !this.isInArea(part.getArea(), chr) || (func = part.getLuaFunction("use")) == null || func.equals("") || (area = script.getAreaById(part.getArea())) == null || (center = this.areaPositionLocal(area, vector2)) == null) continue;
            float testX = 0.0f;
            float testY = 0.0f;
            float testZ = 0.0f;
            if (center.y >= maxY || center.y <= minY) {
                testX = center.x;
            } else {
                testZ = center.y;
            }
            if (!checkDir) {
                return part;
            }
            this.getWorldPos(testX, 0.0f, testZ, vecTest);
            Vector2 vecTo = vector2;
            vecTo.set(vecTest.x - chr.getX(), vecTest.y - chr.getY());
            vecTo.normalize();
            float dot = vecTo.dot(chr.getForwardDirection());
            if (!(dot > 0.5f) || PolygonalMap2.instance.lineClearCollide(chr.getX(), chr.getY(), vecTest.x, vecTest.y, PZMath.fastfloor(chr.getZ()), this, false, true)) break;
            Vector2ObjectPool.get().release(vector2);
            TL_vector3f_pool.get().release(vecTest);
            return part;
        }
        Vector2ObjectPool.get().release(vector2);
        TL_vector3f_pool.get().release(vecTest);
        return null;
    }

    public float distanceToManhatten(float x, float y) {
        return IsoUtils.DistanceManhatten(this.getX(), this.getY(), x, y);
    }

    public VehiclePart getClosestWindow(IsoGameCharacter chr) {
        if (chr == null) {
            return null;
        }
        float chrX = chr.getX();
        float chrY = chr.getY();
        float chrZ = chr.getZ();
        float forwardDirectionX = chr.getForwardDirectionX();
        float forwardDirectionY = chr.getForwardDirectionY();
        return this.getClosestWindow(chrX, chrY, chrZ, forwardDirectionX, forwardDirectionY);
    }

    private @Nullable VehiclePart getClosestWindow(float chrX, float chrY, float chrZ, float forwardDirectionX, float forwardDirectionY) {
        if (PZMath.fastfloor(this.getZ()) != PZMath.fastfloor(chrZ)) {
            return null;
        }
        if (this.distanceToManhatten(chrX, chrY) > 5.0f) {
            return null;
        }
        Vector3f ext = this.script.getExtents();
        Vector3f com = this.script.getCenterOfMassOffset();
        float minY = com.z - ext.z / 2.0f;
        float maxY = com.z + ext.z / 2.0f;
        Vector2 vecTo = (Vector2)Vector2ObjectPool.get().alloc();
        Vector3f vecTest = (Vector3f)TL_vector3f_pool.get().alloc();
        for (int i = 0; i < this.parts.size(); ++i) {
            VehicleScript.Area area;
            VehiclePart part = this.parts.get(i);
            if (part.getWindow() == null || part.getArea() == null || !this.isInArea(part.getArea(), chrX, chrY) || (area = this.script.getAreaById(part.getArea())) == null) continue;
            if (area.y >= maxY || area.y <= minY) {
                vecTest.set(area.x, 0.0f, 0.0f);
            } else {
                vecTest.set(0.0f, 0.0f, area.y);
            }
            this.getWorldPos(vecTest, vecTest);
            vecTo.set(vecTest.x - chrX, vecTest.y - chrY);
            vecTo.normalize();
            float dot = vecTo.dot(forwardDirectionX, forwardDirectionY);
            if (!(dot > 0.5f)) break;
            Vector2ObjectPool.get().release(vecTo);
            TL_vector3f_pool.get().release(vecTest);
            return part;
        }
        Vector2ObjectPool.get().release(vecTo);
        TL_vector3f_pool.get().release(vecTest);
        return null;
    }

    public Vector2 getFacingPosition(IsoGameCharacter chr, Vector2 out) {
        return this.getFacingPosition(chr.getX(), chr.getY(), chr.getZ(), out);
    }

    private Vector2 getFacingPosition(float worldX, float worldY, float worldZ, Vector2 worldFacingPos) {
        Vector3f chrPos = this.getLocalPos(worldX, worldY, worldZ, (Vector3f)TL_vector3f_pool.get().alloc());
        Vector3f ext = this.script.getExtents();
        Vector3f com = this.script.getCenterOfMassOffset();
        float xMin = com.x - ext.x / 2.0f;
        float xMax = com.x + ext.x / 2.0f;
        float yMin = com.z - ext.z / 2.0f;
        float yMax = com.z + ext.z / 2.0f;
        float localX = 0.0f;
        float localY = 0.0f;
        if (chrPos.x <= 0.0f && chrPos.z >= yMin && chrPos.z <= yMax) {
            localY = chrPos.z;
        } else if (chrPos.x > 0.0f && chrPos.z >= yMin && chrPos.z <= yMax) {
            localY = chrPos.z;
        } else if (chrPos.z <= 0.0f && chrPos.x >= xMin && chrPos.x <= xMax) {
            localX = chrPos.x;
        } else if (chrPos.z > 0.0f && chrPos.x >= xMin && chrPos.x <= xMax) {
            localX = chrPos.x;
        }
        this.getWorldPos(localX, 0.0f, localY, chrPos);
        worldFacingPos.set(chrPos.x, chrPos.y);
        TL_vector3f_pool.get().release(chrPos);
        return worldFacingPos;
    }

    public boolean enter(int seat, IsoGameCharacter chr, Vector3f offset) {
        if (!GameClient.client) {
            VehiclesDB2.instance.updateVehicleAndTrailer(this);
        }
        if (chr == null) {
            return false;
        }
        if (chr.getVehicle() != null && !chr.getVehicle().exit(chr)) {
            return false;
        }
        if (this.setPassenger(seat, chr, offset)) {
            IsoPlayer isoPlayer;
            chr.setVehicle(this);
            chr.setCollidable(false);
            if (GameClient.client) {
                INetworkPacket.send(PacketTypes.PacketType.VehicleEnter, this, chr, seat);
            }
            if (chr instanceof IsoPlayer && (isoPlayer = (IsoPlayer)chr).isLocalPlayer()) {
                isoPlayer.dirtyRecalcGridStackTime = 10.0f;
            }
            return true;
        }
        return false;
    }

    public boolean enter(int seat, IsoGameCharacter chr) {
        if (this.getPartForSeatContainer(seat) == null || this.getPartForSeatContainer(seat).getInventoryItem() == null) {
            return false;
        }
        VehicleScript.Position position = this.getPassengerPosition(seat, "outside");
        if (position != null) {
            return this.enter(seat, chr, position.offset);
        }
        return false;
    }

    public boolean enterRSync(int seat, IsoGameCharacter chr, BaseVehicle v) {
        if (chr == null) {
            return false;
        }
        VehicleScript.Position position = this.getPassengerPosition(seat, "inside");
        if (position != null) {
            if (this.setPassenger(seat, chr, position.offset)) {
                IsoPlayer player;
                chr.setVehicle(v);
                chr.setCollidable(false);
                if (GameClient.client && chr instanceof IsoPlayer && (player = (IsoPlayer)chr).isLocalPlayer()) {
                    LuaEventManager.triggerEvent("OnEnterVehicle", player);
                    LuaEventManager.triggerEvent("OnContainerUpdate");
                }
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean exit(IsoGameCharacter chr) {
        if (!GameClient.client) {
            VehiclesDB2.instance.updateVehicleAndTrailer(this);
        }
        if (chr == null) {
            return false;
        }
        int seat = this.getSeat(chr);
        if (seat == -1) {
            return false;
        }
        if (this.clearPassenger(seat)) {
            chr.setVehicle(null);
            chr.savedVehicleSeat = (short)-1;
            chr.setCollidable(true);
            if (GameClient.client) {
                INetworkPacket.send(PacketTypes.PacketType.VehicleExit, this, chr, seat);
            }
            if (this.getDriver() == null && this.soundHornOn) {
                this.onHornStop();
            }
            this.polyGarageCheck = true;
            this.polyDirty = true;
            return true;
        }
        return false;
    }

    public boolean exitRSync(IsoGameCharacter chr) {
        if (chr == null) {
            return false;
        }
        int seat = this.getSeat(chr);
        if (seat == -1) {
            return false;
        }
        if (this.clearPassenger(seat)) {
            chr.setVehicle(null);
            chr.setCollidable(true);
            this.setCharacterPosition(chr, seat, "outside");
            if (GameClient.client) {
                LuaEventManager.triggerEvent("OnContainerUpdate");
            }
            return true;
        }
        return false;
    }

    public boolean hasRoof(int seat) {
        VehicleScript.Passenger pngr = this.getScriptPassenger(seat);
        if (pngr == null) {
            return false;
        }
        return pngr.hasRoof;
    }

    public boolean showPassenger(int seat) {
        VehicleScript.Passenger pngr = this.getScriptPassenger(seat);
        if (pngr == null) {
            return false;
        }
        return pngr.showPassenger;
    }

    public boolean showPassenger(IsoGameCharacter chr) {
        int seat = this.getSeat(chr);
        return this.showPassenger(seat);
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        float origX = this.getX();
        float origY = this.getY();
        if (this.square != null) {
            float d = 5.0E-4f;
            this.setX(PZMath.clamp(this.getX(), (float)this.square.x + 5.0E-4f, (float)this.square.x + 1.0f - 5.0E-4f));
            this.setY(PZMath.clamp(this.getY(), (float)this.square.y + 5.0E-4f, (float)this.square.y + 1.0f - 5.0E-4f));
        }
        super.save(output, isDebugSave);
        this.setX(origX);
        this.setY(origY);
        Quaternionf q = this.savedRot;
        Transform xfrm = this.getWorldTransform(BaseVehicle.allocTransform());
        output.putFloat(xfrm.origin.y);
        xfrm.getRotation(q);
        BaseVehicle.releaseTransform(xfrm);
        output.putFloat(q.x);
        output.putFloat(q.y);
        output.putFloat(q.z);
        output.putFloat(q.w);
        GameWindow.WriteString(output, this.scriptName);
        output.putInt(this.skinIndex);
        output.put((byte)(this.isEngineRunning() ? 1 : 0));
        output.putInt(this.frontEndDurability);
        output.putInt(this.rearEndDurability);
        output.putInt(this.currentFrontEndDurability);
        output.putInt(this.currentRearEndDurability);
        output.putInt(this.engineLoudness);
        output.putInt(this.engineQuality);
        output.putInt(this.keyId);
        output.put(this.keySpawned);
        output.put((byte)(this.headlightsOn ? 1 : 0));
        output.put((byte)(this.created ? 1 : 0));
        output.put((byte)(this.soundHornOn ? 1 : 0));
        output.put((byte)(this.soundBackMoveOn ? 1 : 0));
        output.put((byte)this.lightbarLightsMode.get());
        output.put((byte)this.lightbarSirenMode.get());
        output.putShort((short)this.parts.size());
        for (int i = 0; i < this.parts.size(); ++i) {
            VehiclePart part = this.parts.get(i);
            part.save(output);
        }
        output.put((byte)(this.keyIsOnDoor ? 1 : 0));
        output.put((byte)(this.hotwired ? 1 : 0));
        output.put((byte)(this.hotwiredBroken ? 1 : 0));
        output.put((byte)(this.keysInIgnition ? 1 : 0));
        output.putFloat(this.rust);
        output.putFloat(this.colorHue);
        output.putFloat(this.colorSaturation);
        output.putFloat(this.colorValue);
        output.putInt(this.enginePower);
        output.putShort(this.vehicleId);
        output.putInt(this.mechanicalId);
        output.put((byte)(this.alarmed ? 1 : 0));
        output.putDouble(this.alarmStartTime);
        GameWindow.WriteString(output, this.chosenAlarmSound);
        output.putDouble(this.sirenStartTime);
        if (this.getCurrentKey() != null) {
            output.put((byte)1);
            this.getCurrentKey().saveWithSize(output, false);
        } else {
            output.put((byte)0);
        }
        output.put((byte)this.bloodIntensity.size());
        for (Map.Entry<String, Byte> entry : this.bloodIntensity.entrySet()) {
            GameWindow.WriteString(output, entry.getKey());
            output.put(entry.getValue());
        }
        if (this.vehicleTowingId != -1) {
            output.put((byte)1);
            output.putInt(this.vehicleTowingId);
            GameWindow.WriteString(output, this.towAttachmentSelf);
            GameWindow.WriteString(output, this.towAttachmentOther);
            output.putFloat(this.rowConstraintZOffset);
        } else {
            output.put((byte)0);
        }
        output.putFloat(this.getRegulatorSpeed());
        output.put((byte)(this.previouslyEntered ? 1 : 0));
        output.put((byte)(this.previouslyMoved ? 1 : 0));
        int pos = output.position();
        output.putInt(0);
        int posStart = output.position();
        if (this.animals.isEmpty()) {
            output.put((byte)0);
        } else {
            output.put((byte)1);
            output.putInt(this.animals.size());
            for (int i = 0; i < this.animals.size(); ++i) {
                this.animals.get(i).save(output, isDebugSave, false);
            }
        }
        int posEnd = output.position();
        output.position(pos);
        output.putInt(posEnd - posStart);
        output.position(posEnd);
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        Object part;
        boolean isEngineRunning;
        super.load(input, worldVersion, isDebugSave);
        float physicsZ = input.getFloat();
        int level = PZMath.fastfloor(this.getZ());
        float f = 2.44949f;
        this.savedPhysicsZ = PZMath.clamp(physicsZ, (float)level * 2.44949f, ((float)level + 0.995f) * 2.44949f);
        float x = input.getFloat();
        float y = input.getFloat();
        float z = input.getFloat();
        float w = input.getFloat();
        this.savedRot.set(x, y, z, w);
        this.jniTransform.origin.set(this.getX() - WorldSimulation.instance.offsetX, Float.isNaN(this.savedPhysicsZ) ? this.getZ() : this.savedPhysicsZ, this.getY() - WorldSimulation.instance.offsetY);
        this.jniTransform.setRotation(this.savedRot);
        this.scriptName = GameWindow.ReadString(input);
        this.skinIndex = input.getInt();
        boolean bl = isEngineRunning = input.get() != 0;
        if (isEngineRunning) {
            this.engineState = engineStateTypes.Running;
        }
        this.frontEndDurability = input.getInt();
        this.rearEndDurability = input.getInt();
        this.currentFrontEndDurability = input.getInt();
        this.currentRearEndDurability = input.getInt();
        this.engineLoudness = input.getInt();
        this.engineQuality = input.getInt();
        this.engineQuality = PZMath.clamp(this.engineQuality, 0, 100);
        this.keyId = input.getInt();
        this.keySpawned = input.get();
        this.headlightsOn = input.get() != 0;
        this.created = input.get() != 0;
        this.soundHornOn = input.get() != 0;
        this.soundBackMoveOn = input.get() != 0;
        this.lightbarLightsMode.set(input.get());
        this.lightbarSirenMode.set(input.get());
        int partCount = input.getShort();
        for (int i = 0; i < partCount; ++i) {
            part = new VehiclePart(this);
            ((VehiclePart)part).load(input, worldVersion);
            this.parts.add((VehiclePart)part);
        }
        this.keyIsOnDoor = input.get() != 0;
        this.hotwired = input.get() != 0;
        this.hotwiredBroken = input.get() != 0;
        this.keysInIgnition = input.get() != 0;
        this.rust = input.getFloat();
        this.colorHue = input.getFloat();
        this.colorSaturation = input.getFloat();
        this.colorValue = input.getFloat();
        this.enginePower = input.getInt();
        short vehicleId = input.getShort();
        if (worldVersion < 229) {
            part = GameWindow.ReadString(input);
        }
        this.mechanicalId = input.getInt();
        boolean bl2 = this.alarmed = input.get() != 0;
        if (worldVersion >= 229) {
            this.alarmStartTime = input.getDouble();
            this.chosenAlarmSound = StringUtils.discardNullOrWhitespace(GameWindow.ReadString(input));
        }
        this.sirenStartTime = input.getDouble();
        if (input.get() != 0) {
            InventoryItem key = null;
            try {
                key = InventoryItem.loadItem(input, worldVersion);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            if (key != null) {
                this.setCurrentKey(key);
            }
        }
        int count = input.get();
        for (int i = 0; i < count; ++i) {
            String id = GameWindow.ReadString(input);
            byte intensity = input.get();
            this.bloodIntensity.put(id, intensity);
        }
        if (input.get() != 0) {
            this.vehicleTowingId = input.getInt();
            this.towAttachmentSelf = GameWindow.ReadString(input);
            this.towAttachmentOther = GameWindow.ReadString(input);
            this.rowConstraintZOffset = input.getFloat();
        }
        this.setRegulatorSpeed(input.getFloat());
        boolean bl3 = this.previouslyEntered = input.get() != 0;
        if (worldVersion >= 196) {
            boolean bl4 = this.previouslyMoved = input.get() != 0;
        }
        if (worldVersion >= 212) {
            int bufferSize = input.getInt();
            if (GameClient.client) {
                input.position(input.position() + bufferSize);
            } else if (input.get() != 0) {
                int size = input.getInt();
                for (int i = 0; i < size; ++i) {
                    IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell());
                    animal.load(input, worldVersion, isDebugSave);
                    this.addAnimalInTrailer(animal);
                }
            }
        } else if (input.get() != 0) {
            int size = input.getInt();
            for (int i = 0; i < size; ++i) {
                IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell());
                animal.load(input, worldVersion, isDebugSave);
                this.addAnimalInTrailer(animal);
            }
        }
        this.loaded = true;
    }

    @Override
    public void softReset() {
        this.keySpawned = 0;
        this.keyIsOnDoor = false;
        this.keysInIgnition = false;
        this.ignitionSwitch.removeAllItems();
        this.currentKey = null;
        this.previouslyEntered = false;
        this.previouslyMoved = false;
        this.engineState = engineStateTypes.Idle;
        this.randomizeContainers();
    }

    public void trySpawnKey() {
        this.trySpawnKey(false);
    }

    public void trySpawnKey(boolean crashed) {
        int chance;
        if (GameClient.client) {
            return;
        }
        if (this.script == null || this.script.neverSpawnKey()) {
            return;
        }
        if (this.keySpawned == 1) {
            return;
        }
        if (SandboxOptions.getInstance().vehicleEasyUse.getValue()) {
            this.addKeyToGloveBox();
            return;
        }
        if (this.isPreviouslyEntered() || this.isPreviouslyMoved() || this.isHotwired() || this.isBurnt()) {
            return;
        }
        VehicleType type = VehicleType.getTypeFromName(this.getVehicleType());
        int n = chance = type == null ? 70 : type.getChanceToSpawnKey();
        if (Rand.Next(100) <= chance) {
            this.addKeyToWorld(crashed);
        }
    }

    public boolean shouldCollideWithCharacters() {
        if (this.vehicleTowedBy != null && this.vehicleTowedBy != this) {
            return this.vehicleTowedBy.shouldCollideWithCharacters();
        }
        float speed = this.getSpeed2D();
        return this.isEngineRunning() ? speed > 0.05f : speed > 1.0f;
    }

    public boolean shouldCollideWithObjects() {
        if (this.vehicleTowedBy != null && this.vehicleTowedBy != this) {
            return this.vehicleTowedBy.shouldCollideWithObjects();
        }
        return this.isEngineRunning();
    }

    public void breakingObjects() {
        boolean bCollideWithCharacters = this.shouldCollideWithCharacters();
        boolean bCollideWithObjects = this.shouldCollideWithObjects();
        if (!bCollideWithCharacters && !bCollideWithObjects) {
            return;
        }
        Vector3f ext = this.script.getExtents();
        Vector2 vector2 = (Vector2)Vector2ObjectPool.get().alloc();
        float radius = Math.max(ext.x / 2.0f, ext.z / 2.0f) + 0.3f + 1.0f;
        int radiusSq = (int)Math.ceil(radius);
        for (int yy = -radiusSq; yy < radiusSq; ++yy) {
            for (int xx = -radiusSq; xx < radiusSq; ++xx) {
                IsoObject object;
                int i;
                IsoGridSquare sq = this.getCell().getGridSquare(this.getX() + (float)xx, this.getY() + (float)yy, this.getZ());
                if (sq == null) continue;
                if (bCollideWithObjects) {
                    for (i = 0; i < sq.getObjects().size(); ++i) {
                        object = sq.getObjects().get(i);
                        if (object instanceof IsoWorldInventoryObject) continue;
                        Vector2 collision = null;
                        if (this.breakingObjectsList.contains(object) || object == null || object.getProperties() == null) continue;
                        if (object.getProperties().has("CarSlowFactor")) {
                            collision = this.testCollisionWithObject(object, 0.3f, vector2);
                        }
                        if (collision != null) {
                            this.breakingObjectsList.add(object);
                            if (!GameClient.client) {
                                object.Collision(collision, this);
                            }
                        }
                        if (object.getProperties().has("HitByCar")) {
                            collision = this.testCollisionWithObject(object, 0.3f, vector2);
                        }
                        if (collision != null && !GameClient.client) {
                            object.Collision(collision, this);
                        }
                        this.checkCollisionWithPlant(sq, object, vector2);
                    }
                }
                if (bCollideWithCharacters) {
                    for (i = 0; i < sq.getMovingObjects().size(); ++i) {
                        object = sq.getMovingObjects().get(i);
                        if (object instanceof IsoZombie) {
                            IsoZombie zombie = (IsoZombie)object;
                            if (zombie.isProne()) {
                                this.testCollisionWithProneCharacter(zombie, false, null);
                            }
                            zombie.setVehicle4TestCollision(this);
                        }
                        if (object instanceof IsoAnimal) {
                            IsoAnimal animal = (IsoAnimal)object;
                            animal.setVehicle4TestCollision(this);
                        }
                        if (!(object instanceof IsoPlayer)) continue;
                        IsoPlayer player = (IsoPlayer)object;
                        if (object == this.getDriver()) continue;
                        player.setVehicle4TestCollision(this);
                    }
                }
                if (!bCollideWithObjects) continue;
                for (i = 0; i < sq.getStaticMovingObjects().size(); ++i) {
                    object = sq.getStaticMovingObjects().get(i);
                    if (!(object instanceof IsoDeadBody)) continue;
                    IsoDeadBody body = (IsoDeadBody)object;
                    int n = this.testCollisionWithCorpse(body, true);
                }
            }
        }
        float slowFactor = -999.0f;
        for (int i = 0; i < this.breakingObjectsList.size(); ++i) {
            IsoObject object = this.breakingObjectsList.get(i);
            Vector2 collision = this.testCollisionWithObject(object, 1.0f, vector2);
            if (collision == null || !object.getSquare().getObjects().contains(object)) {
                this.breakingObjectsList.remove(object);
                object.UnCollision(this);
                continue;
            }
            if (!(slowFactor < object.GetVehicleSlowFactor(this))) continue;
            slowFactor = object.GetVehicleSlowFactor(this);
        }
        this.breakingSlowFactor = slowFactor != -999.0f ? PZMath.clamp(slowFactor, 0.0f, 34.0f) : 0.0f;
        Vector2ObjectPool.get().release(vector2);
    }

    private void updateVelocityMultiplier() {
        if (this.physics == null || this.getScript() == null) {
            return;
        }
        Vector3f velocity = this.getLinearVelocity((Vector3f)TL_vector3f_pool.get().alloc());
        velocity.y = 0.0f;
        float speed = velocity.length();
        float maxSpeed = 100000.0f;
        float multiplier = 1.0f;
        if (this.getScript().getWheelCount() > 0) {
            if (speed > 0.0f && speed > 34.0f - this.breakingSlowFactor) {
                maxSpeed = 34.0f - this.breakingSlowFactor;
                multiplier = (34.0f - this.breakingSlowFactor) / speed;
            }
        } else if (this.getVehicleTowedBy() == null) {
            maxSpeed = 0.0f;
            multiplier = 0.1f;
        }
        Bullet.setVehicleVelocityMultiplier(this.vehicleId, maxSpeed, multiplier);
        TL_vector3f_pool.get().release(velocity);
    }

    private void playScrapePastPlantSound(IsoGridSquare sq) {
        if (this.emitter != null && !this.emitter.isPlaying(this.soundScrapePastPlant)) {
            this.emitter.setPos((float)sq.x + 0.5f, (float)sq.y + 0.5f, sq.z);
            this.soundScrapePastPlant = this.emitter.playSoundImpl("VehicleScrapePastPlant", sq);
        }
        this.hittingPlant = true;
    }

    private void checkCollisionWithPlant(IsoGridSquare sq, IsoObject object, Vector2 vector2) {
        boolean bPlantExcludingGrass;
        if (object.sprite == null) {
            return;
        }
        IsoTree tree = Type.tryCastTo(object, IsoTree.class);
        String tilesetName = object.sprite.tilesetName;
        boolean bl = bPlantExcludingGrass = "d_generic_1".equalsIgnoreCase(tilesetName) || "d_plants_1".equalsIgnoreCase(tilesetName);
        if (tree == null && !object.isBush() && !bPlantExcludingGrass) {
            return;
        }
        float currentSpeed = this.getCurrentAbsoluteSpeedKmHour();
        if (currentSpeed <= 1.0f) {
            return;
        }
        Vector2 collision = this.testCollisionWithObject(object, 0.3f, vector2);
        if (collision == null) {
            return;
        }
        if (tree != null && tree.getSize() == 1) {
            this.applyImpulseFromHitPlant(object, 0.025f);
            this.playScrapePastPlantSound(sq);
            return;
        }
        if (object.isBush() && this.soundScrapePastPlant == -1L && this.emitter != null && !this.emitter.isPlaying("VehicleHitHedge")) {
            this.emitter.playSoundImpl("VehicleHitHedge", (IsoObject)null);
        }
        if (this.isPositionOnLeftOrRight(collision.x, collision.y)) {
            if (!bPlantExcludingGrass) {
                this.applyImpulseFromHitPlant(object, 0.025f);
            }
            this.playScrapePastPlantSound(sq);
            return;
        }
        if (currentSpeed < 10.0f) {
            if (!bPlantExcludingGrass) {
                this.applyImpulseFromHitPlant(object, 0.025f);
            }
            this.playScrapePastPlantSound(sq);
            return;
        }
        if (!bPlantExcludingGrass) {
            this.applyImpulseFromHitPlant(object, 0.1f);
        }
        this.playScrapePastPlantSound(sq);
    }

    private void updateScrapPastPlantSound() {
        if (this.soundScrapePastPlant == -1L) {
            return;
        }
        if (!(this.isEngineRunning() && this.hittingPlant || this.emitter == null)) {
            this.emitter.stopOrTriggerSound(this.soundScrapePastPlant);
            this.soundScrapePastPlant = -1L;
        }
    }

    public void damageObjects(float damage) {
        if (!this.isEngineRunning()) {
            return;
        }
        Vector3f ext = this.script.getExtents();
        Vector2 vector2 = (Vector2)Vector2ObjectPool.get().alloc();
        float radius = Math.max(ext.x / 2.0f, ext.z / 2.0f) + 0.3f + 1.0f;
        int radiusSq = (int)Math.ceil(radius);
        for (int yy = -radiusSq; yy < radiusSq; ++yy) {
            for (int xx = -radiusSq; xx < radiusSq; ++xx) {
                IsoGridSquare sq = this.getCell().getGridSquare(this.getX() + (float)xx, this.getY() + (float)yy, this.getZ());
                if (sq == null) continue;
                for (int i = 0; i < sq.getObjects().size(); ++i) {
                    IsoGridSquare sq2;
                    IsoObject object = sq.getObjects().get(i);
                    Vector2 collision = null;
                    if (object instanceof IsoTree && (collision = this.testCollisionWithObject(object, 2.0f, vector2)) != null) {
                        object.setRenderEffect(RenderEffectType.Hit_Tree_Shudder);
                    }
                    if (collision == null && object instanceof IsoWindow) {
                        collision = this.testCollisionWithObject(object, 1.0f, vector2);
                    }
                    if (collision == null && object.sprite != null && (object.sprite.getProperties().has("HitByCar") || object.sprite.getProperties().has("CarSlowFactor"))) {
                        collision = this.testCollisionWithObject(object, 1.0f, vector2);
                    }
                    if (collision == null && (sq2 = this.getCell().getGridSquare(this.getX() + (float)xx, this.getY() + (float)yy, 1.0)) != null && sq2.has(IsoObjectType.lightswitch)) {
                        collision = this.testCollisionWithObject(object, 1.0f, vector2);
                    }
                    if (collision == null && (sq2 = this.getCell().getGridSquare(this.getX() + (float)xx, this.getY() + (float)yy, 0.0)) != null && sq2.has(IsoObjectType.lightswitch)) {
                        collision = this.testCollisionWithObject(object, 1.0f, vector2);
                    }
                    if (collision == null) continue;
                    object.Hit(collision, this, damage);
                }
            }
        }
        ArrayList<BaseVehicle> vehicles = IsoWorld.instance.currentCell.getVehicles();
        for (int i = 0; i < vehicles.size(); ++i) {
            BaseVehicle vehicle = vehicles.get(i);
            if (vehicle == this || !this.testCollisionWithVehicle(vehicle)) continue;
            vehicle.lastDamagedBy = this.getDriverRegardlessOfTow();
        }
        Vector2ObjectPool.get().release(vector2);
    }

    @Override
    public void update() {
        int i;
        VehiclePart gasTank;
        IsoGridSquare sq;
        int i2;
        if (this.removedFromWorld) {
            return;
        }
        if (!this.getCell().vehicles.contains(this)) {
            this.getCell().getRemoveList().add(this);
            return;
        }
        if (this.chunk != null) {
            if (!this.chunk.vehicles.contains(this)) {
                if (GameClient.client) {
                    VehicleManager.instance.sendVehicleRequest(this.vehicleId, (short)2);
                }
            } else if (!GameServer.server && this.chunk.refs.isEmpty()) {
                this.removeFromWorld();
                return;
            }
        }
        super.update();
        if (this.timeSinceLastAuth > 0.0f) {
            this.timeSinceLastAuth -= 1.0f;
        }
        if (!GameClient.client) {
            for (i = this.getAnimals().size() - 1; i >= 0; --i) {
                animal = this.getAnimals().get(i);
                if (animal == null) continue;
                animal.setX(this.getX());
                animal.setY(this.getY());
                animal.setZ(PZMath.fastfloor(this.getZ()));
                animal.update();
                if (!this.getAnimals().contains(animal)) continue;
                if (!animal.isDead()) {
                    animal.updateVocalProperties();
                }
                this.setNeedPartsUpdate(true);
                if (!GameServer.server || !this.updateAnimal.Check()) continue;
                animal.networkAi.setAnimalPacket(null);
                AnimalSynchronizationManager.getInstance().setSendToClients(animal.onlineId);
            }
        } else {
            for (i = 0; i < this.getAnimals().size(); ++i) {
                animal = this.getAnimals().get(i);
                animal.setX(this.getX());
                animal.setY(this.getY());
                animal.setZ(PZMath.fastfloor(this.getZ()));
                AnimalInstanceManager.getInstance().update(animal);
            }
        }
        if (GameClient.client || GameServer.server) {
            this.isReliable = this.physicReliableLimit.Check();
        }
        if (GameClient.client && this.hasAuthorization(GameClient.connection)) {
            this.updatePhysicsNetwork();
        }
        if (this.getVehicleTowing() != null && this.getDriver() != null && this.getVehicleTowing().scriptName.equals("Base.Trailer")) {
            VehiclePart trailer = this.getVehicleTowing().getPartById("TrailerTrunk");
            if (this.getCurrentSpeedKmHour() > 30.0f && (float)trailer.getCondition() < 50.0f && !trailer.container.items.isEmpty()) {
                ArrayList<InventoryItem> heavyItems = new ArrayList<InventoryItem>();
                for (i2 = 0; i2 < trailer.container.items.size(); ++i2) {
                    if (!(trailer.container.items.get(i2).getWeight() >= 3.5f)) continue;
                    heavyItems.add(trailer.container.items.get(i2));
                }
                if (!heavyItems.isEmpty()) {
                    int t = trailer.getCondition();
                    int s = 0;
                    int w = 0;
                    for (int i3 = 0; i3 < this.getVehicleTowing().parts.size(); ++i3) {
                        VehiclePart part = this.getVehicleTowing().getPartByIndex(i3);
                        if (part == null || part.item == null) continue;
                        if (part.partId != null && part.partId.contains("Suspension")) {
                            s += part.getCondition();
                            continue;
                        }
                        if (part.partId == null || !part.partId.contains("Tire")) continue;
                        w += part.getCondition();
                    }
                    float r = this.parameterVehicleSteer.getCurrentValue();
                    int dropChance = (int)(Math.pow(100 - t * 2, 2.0) * 0.3 * (1.0 + (double)(100 - s / 2) * 0.005) * (1.0 + (double)(100 - s / 2) * 0.005) * (double)(1.0f + r / 3.0f));
                    if (Rand.Next(0, Math.max(10000 - dropChance, 1)) == 0) {
                        InventoryItem droppedItem = (InventoryItem)heavyItems.get(Rand.Next(0, heavyItems.size()));
                        droppedItem.setCondition(droppedItem.getCondition() - droppedItem.getConditionMax() / 10, false);
                        trailer.getSquare().AddWorldInventoryItem(droppedItem, Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
                        trailer.container.items.remove(droppedItem);
                        trailer.getSquare().playSound("thumpa2");
                    }
                }
            }
        }
        if (this.physics != null && this.vehicleTowingId != -1 && this.vehicleTowing == null) {
            this.tryReconnectToTowedVehicle();
        }
        boolean bTowed = false;
        boolean bTowing = false;
        if (this.getVehicleTowedBy() != null && this.getVehicleTowedBy().getController() != null) {
            bTowed = this.getVehicleTowedBy() != null && this.getVehicleTowedBy().getController().isEnable;
            boolean bl = bTowing = this.getVehicleTowing() != null && this.getVehicleTowing().getDriver() != null;
        }
        if (this.physics != null) {
            boolean bUpdatePhysics = this.getDriver() != null || bTowed || bTowing;
            long currentTimeMS = System.currentTimeMillis();
            if (this.constraintChangedTime != -1L) {
                if (this.constraintChangedTime + 3500L < currentTimeMS) {
                    this.constraintChangedTime = -1L;
                    if (!bUpdatePhysics && this.physicActiveCheck < currentTimeMS) {
                        this.setPhysicsActive(false);
                    }
                }
            } else {
                if (this.physicActiveCheck != -1L && (bUpdatePhysics || !this.physics.isEnable)) {
                    this.physicActiveCheck = -1L;
                }
                if (!bUpdatePhysics && this.physics.isEnable && this.physicActiveCheck != -1L && this.physicActiveCheck < currentTimeMS) {
                    this.physicActiveCheck = -1L;
                    this.setPhysicsActive(false);
                }
            }
            if (this.getVehicleTowedBy() != null && this.getScript().getWheelCount() > 0) {
                this.physics.updateTrailer();
            } else if (this.getDriver() == null && !GameServer.server) {
                this.physics.checkShouldBeActive();
            }
            this.doAlarm();
            VehicleImpulse impulse = this.impulseFromServer;
            if (!GameServer.server && impulse != null && impulse.enable) {
                impulse.enable = false;
                float fpsScale = 1.0f;
                Bullet.applyCentralForceToVehicle(this.vehicleId, impulse.impulse.x * 1.0f, impulse.impulse.y * 1.0f, impulse.impulse.z * 1.0f);
                Vector3f torque = impulse.relPos.cross(impulse.impulse, (Vector3f)TL_vector3f_pool.get().alloc());
                Bullet.applyTorqueToVehicle(this.vehicleId, torque.x * 1.0f, torque.y * 1.0f, torque.z * 1.0f);
                TL_vector3f_pool.get().release(torque);
            }
            int baseCheckTime = 1000;
            if (System.currentTimeMillis() - this.engineCheckTime > 1000L && !GameClient.client) {
                VehiclePart engine;
                this.engineCheckTime = System.currentTimeMillis();
                if (!GameClient.client) {
                    if (this.engineState != engineStateTypes.Idle) {
                        int newEngineLoudness = (int)((double)this.engineLoudness * this.engineSpeed / 2500.0);
                        double maxSpeed = Math.min(this.getEngineSpeed(), 2000.0);
                        newEngineLoudness = (int)((double)newEngineLoudness * (1.0 + maxSpeed / 4000.0));
                        int baseRand = 120;
                        if (GameServer.server) {
                            baseRand = (int)((double)baseRand * ServerOptions.getInstance().carEngineAttractionModifier.getValue());
                            newEngineLoudness = (int)((double)newEngineLoudness * ServerOptions.getInstance().carEngineAttractionModifier.getValue());
                        }
                        if (Rand.Next((int)((float)baseRand * GameTime.instance.getInvMultiplier())) == 0) {
                            WorldSoundManager.instance.addSoundRepeating(this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), Math.max(8, newEngineLoudness), Math.max(6, newEngineLoudness / 3), false, true);
                        }
                        if (Rand.Next((int)((float)(baseRand - 85) * GameTime.instance.getInvMultiplier())) == 0) {
                            WorldSoundManager.instance.addSoundRepeating(this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), Math.max(8, newEngineLoudness / 2), Math.max(6, newEngineLoudness / 3), false, true);
                        }
                        if (Rand.Next((int)((float)(baseRand - 110) * GameTime.instance.getInvMultiplier())) == 0) {
                            WorldSoundManager.instance.addSoundRepeating(this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), Math.max(8, newEngineLoudness / 4), Math.max(6, newEngineLoudness / 3), false, true);
                        }
                        WorldSoundManager.instance.addSoundRepeating(this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), Math.max(8, newEngineLoudness / 6), Math.max(6, newEngineLoudness / 3), false, true);
                        boolean flags = true;
                        WorldSoundManager.instance.addSoundRepeating((Object)this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), Math.max(80, newEngineLoudness / 6), Math.max(6, newEngineLoudness / 3), (short)1);
                    }
                    if (this.lightbarSirenMode.isEnable() && this.getBatteryCharge() > 0.0f && SandboxOptions.instance.sirenEffectsZombies.getValue()) {
                        WorldSoundManager.instance.addSoundRepeating(this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), 100, 60, false, true);
                    }
                }
                if (this.engineState == engineStateTypes.Running && !this.isEngineWorking()) {
                    String sound = "VehicleEngineFailureDamage";
                    VehiclePart gasTank2 = this.getPartById("GasTank");
                    if (gasTank2 != null && gasTank2.getContainerContentAmount() <= 0.0f) {
                        sound = "VehicleRunningOutOfGas";
                    }
                    this.shutOff(sound);
                    this.checkVehicleFailsToStartWithZombiesTargeting();
                }
                if (this.engineState == engineStateTypes.Running && (engine = this.getPartById("Engine")) != null && engine.getCondition() < 50 && Rand.Next(Rand.AdjustForFramerate(engine.getCondition() * 12)) == 0) {
                    this.shutOff("VehicleEngineFailureDamage");
                    this.checkVehicleFailsToStartWithZombiesTargeting();
                }
                if (this.engineState == engineStateTypes.Starting) {
                    this.updateEngineStarting();
                }
                if (this.engineState == engineStateTypes.RetryingStarting && System.currentTimeMillis() - this.engineLastUpdateStateTime > 10L) {
                    this.engineDoStarting();
                }
                if (this.engineState == engineStateTypes.StartingSuccess && System.currentTimeMillis() - this.engineLastUpdateStateTime > 500L) {
                    this.engineDoRunning();
                }
                if (this.engineState == engineStateTypes.StartingFailed && System.currentTimeMillis() - this.engineLastUpdateStateTime > 500L) {
                    this.engineDoIdle();
                }
                if (this.engineState == engineStateTypes.StartingFailedNoPower && System.currentTimeMillis() - this.engineLastUpdateStateTime > 500L) {
                    this.engineDoIdle();
                }
                if (this.engineState == engineStateTypes.Stalling && System.currentTimeMillis() - this.engineLastUpdateStateTime > 3000L) {
                    this.engineDoIdle();
                }
                if (this.engineState == engineStateTypes.ShutingDown && System.currentTimeMillis() - this.engineLastUpdateStateTime > 2000L) {
                    this.engineDoIdle();
                }
            }
            if (this.getDriver() == null && !bTowed) {
                this.getController().park();
            }
            this.setX(this.jniTransform.origin.x + WorldSimulation.instance.offsetX);
            this.setY(this.jniTransform.origin.z + WorldSimulation.instance.offsetY);
            this.setZ(0.0f);
            int zi = PZMath.fastfloor(this.jniTransform.origin.y / 2.44949f + 0.05f);
            IsoGridSquare square = this.getCell().getGridSquare(this.getX(), this.getY(), (double)zi);
            IsoGridSquare below = this.getCell().getGridSquare(this.getX(), this.getY(), (double)(zi - 1));
            if (square != null && (square.getFloor() != null || below != null && below.getFloor() != null)) {
                this.setZ(zi);
            }
            if ((sq = this.getCell().getGridSquare(this.getX(), this.getY(), this.getZ())) == null && !this.chunk.refs.isEmpty()) {
                float d = 5.0E-4f;
                int minX = this.chunk.wx * 8;
                int minY = this.chunk.wy * 8;
                int maxX = minX + 8;
                int maxY = minY + 8;
                this.setX(Math.max(this.getX(), (float)minX + 5.0E-4f));
                this.setX(Math.min(this.getX(), (float)maxX - 5.0E-4f));
                this.setY(Math.max(this.getY(), (float)minY + 5.0E-4f));
                this.setY(Math.min(this.getY(), (float)maxY - 5.0E-4f));
                this.setZ(0.2f);
                Transform t = BaseVehicle.allocTransform();
                Transform t1 = BaseVehicle.allocTransform();
                this.getWorldTransform(t);
                t1.basis.set(t.basis);
                t1.origin.set(this.getX() - WorldSimulation.instance.offsetX, this.getZ(), this.getY() - WorldSimulation.instance.offsetY);
                this.setWorldTransform(t1);
                BaseVehicle.releaseTransform(t);
                BaseVehicle.releaseTransform(t1);
                this.current = this.getCell().getGridSquare(this.getX(), this.getY(), PZMath.floor(this.getZ()));
            }
            if (this.current == null || this.current.chunk == null) {
                boolean d = false;
            } else if (this.current.getChunk() != this.chunk) {
                assert (this.chunk.vehicles.contains(this));
                this.chunk.vehicles.remove(this);
                this.chunk = this.current.getChunk();
                assert (!this.chunk.vehicles.contains(this));
                this.chunk.vehicles.add(this);
                IsoChunk.addFromCheckedVehicles(this);
            }
            this.updateTransform();
            Vector3f currentVelocity = BaseVehicle.allocVector3f().set(this.jniLinearVelocity);
            if (this.jniIsCollide && this.limitCrash.Check()) {
                this.jniIsCollide = false;
                this.limitCrash.Reset();
                Vector3f velocityChange = BaseVehicle.allocVector3f();
                velocityChange.set(currentVelocity).sub(this.lastLinearVelocity);
                velocityChange.y = 0.0f;
                float delta = velocityChange.length();
                float deltaLimit = 6.0f;
                if (currentVelocity.lengthSquared() > this.lastLinearVelocity.lengthSquared() && delta > 6.0f) {
                    DebugLog.Vehicle.trace("Vehicle vid=%d got sharp speed increase delta=%f", this.vehicleId, Float.valueOf(delta));
                    delta = 6.0f;
                }
                if (delta > 1.0f) {
                    if (this.lastLinearVelocity.length() < 6.0f) {
                        delta /= 3.0f;
                    }
                    DebugLog.Vehicle.trace("Vehicle vid=%d crash delta=%f", this.vehicleId, Float.valueOf(delta));
                    Vector3f forward = this.getForwardVector(BaseVehicle.allocVector3f());
                    float dot = velocityChange.dot(forward);
                    BaseVehicle.releaseVector3f(forward);
                    this.crash(delta * 3.0f, dot < 0.0f);
                    this.damageObjects(delta * 30.0f);
                }
                BaseVehicle.releaseVector3f(velocityChange);
            }
            this.lastLinearVelocity.set(currentVelocity);
            BaseVehicle.releaseVector3f(currentVelocity);
        }
        if (this.soundAlarmOn && this.alarmEmitter != null) {
            this.alarmEmitter.setPos(this.getX(), this.getY(), this.getZ());
        }
        if (this.soundHornOn && this.hornEmitter != null) {
            this.hornEmitter.setPos(this.getX(), this.getY(), this.getZ());
        }
        for (i2 = 0; i2 < this.impulsesFromSquishedBodies.length; ++i2) {
            VehicleImpulse impulse = this.impulsesFromSquishedBodies[i2];
            if (impulse == null) continue;
            impulse.enable = false;
        }
        this.updateSounds();
        this.hittingPlant = false;
        this.breakingObjects();
        this.updateScrapPastPlantSound();
        if (this.addThumpWorldSound) {
            this.addThumpWorldSound = false;
            WorldSoundManager.instance.addSound(this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), 20, 20, true);
        }
        if (this.script.getLightbar().enable && this.lightbarLightsMode.isEnable() && this.getBatteryCharge() > 0.0f) {
            this.lightbarLightsMode.update();
        }
        this.updateWorldLights();
        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
            IsoPlayer player;
            if (this.current == null || !this.couldSeeIntersectedSquare(playerIndex)) {
                this.setTargetAlpha(playerIndex, 0.0f);
            }
            if ((player = IsoPlayer.players[playerIndex]) == null || !(this.DistToSquared(player) < 225.0f)) continue;
            this.setTargetAlpha(playerIndex, 1.0f);
        }
        for (i2 = 0; i2 < this.getScript().getPassengerCount(); ++i2) {
            IsoGameCharacter w = this.getCharacter(i2);
            if (!(w instanceof IsoGameCharacter)) continue;
            IsoGameCharacter chr = w;
            Vector3f worldPos = this.getPassengerWorldPos(i2, BaseVehicle.allocVector3f());
            chr.setX(worldPos.x);
            chr.setY(worldPos.y);
            chr.setZ(worldPos.z);
            BaseVehicle.releaseVector3f(worldPos);
        }
        VehiclePart lightbar = this.getPartById("lightbar");
        if (lightbar != null && this.lightbarLightsMode.isEnable() && lightbar.getCondition() == 0 && !GameClient.client) {
            this.setLightbarLightsMode(0);
        }
        if (lightbar != null && this.lightbarSirenMode.isEnable() && lightbar.getCondition() == 0 && !GameClient.client) {
            this.setLightbarSirenMode(0);
        }
        if (this.needPartsUpdate() || this.isMechanicUIOpen() || this.alarmStartTime > 0.0) {
            this.updateParts();
        } else {
            this.drainBatteryUpdateHack();
        }
        if (this.engineState == engineStateTypes.Running || bTowed) {
            this.updateBulletStats();
        }
        if (this.doDamageOverlay) {
            this.doDamageOverlay = false;
            this.doDamageOverlay();
        }
        if (GameClient.client) {
            this.checkPhysicsValidWithServer();
        }
        if ((gasTank = this.getPartById("GasTank")) != null && gasTank.getContainerContentAmount() > (float)gasTank.getContainerCapacity()) {
            gasTank.setContainerContentAmount(gasTank.getContainerCapacity());
        }
        boolean bHasPassengers = false;
        for (i = 0; i < this.getMaxPassengers(); ++i) {
            Passenger pngr = this.getPassenger(i);
            if (pngr.character == null) continue;
            bHasPassengers = true;
            break;
        }
        if (bHasPassengers) {
            this.surroundVehicle.update();
        }
        if (!this.notKillCrops() && this.getSquare() != null) {
            for (i = -1; i < 1; ++i) {
                for (int j = -1; j < 1; ++j) {
                    sq = IsoWorld.instance.currentCell.getGridSquare(this.getSquare().getX() + i, this.getSquare().getY() + j, this.getSquare().getZ());
                    if (sq == null) continue;
                    sq.checkForIntersectingCrops(this);
                }
            }
        }
        if (!GameServer.server) {
            if (this.physics != null) {
                Bullet.setVehicleMass(this.vehicleId, this.getFudgedMass());
            }
            this.updateVelocityMultiplier();
        }
    }

    @Override
    public UpdateSchedulerSimulationLevel getMinimumSimulationLevel() {
        return UpdateSchedulerSimulationLevel.FULL;
    }

    private void updateEngineStarting() {
        if (this.getBatteryCharge() <= 0.1f) {
            this.engineDoStartingFailedNoPower();
            return;
        }
        VehiclePart gasTank = this.getPartById("GasTank");
        if (gasTank != null && gasTank.getContainerContentAmount() <= 0.0f) {
            this.engineDoStartingFailed("VehicleRunningOutOfGas");
            return;
        }
        int weatherAffect = 0;
        float airTemp = ClimateManager.getInstance().getAirTemperatureForSquare(this.getSquare());
        if (this.engineQuality < 65 && airTemp <= 2.0f) {
            weatherAffect = Math.min((2 - (int)airTemp) * 2, 30);
        }
        if (!SandboxOptions.instance.vehicleEasyUse.getValue() && this.engineQuality < 100 && Rand.Next(this.engineQuality + 50 - weatherAffect) <= 30) {
            this.engineDoStartingFailed("VehicleEngineFailureDamage");
            return;
        }
        if (Rand.Next(this.engineQuality) != 0) {
            this.engineDoStartingSuccess();
        } else {
            this.engineDoRetryingStarting();
        }
    }

    public void applyAccumulatedImpulsesFromHitObjectsToPhysics() {
        if (this.impulsesFromHitObjects.isEmpty()) {
            return;
        }
        if (GameClient.client && !this.hasAuthorization(GameClient.connection) || GameServer.server) {
            int count = this.impulsesFromHitObjects.size();
            for (int i = 0; i < count; ++i) {
                VehicleImpulse impulse = this.impulsesFromHitObjects.get(i);
                impulse.release();
                impulse.enable = false;
            }
            this.impulsesFromHitObjects.clear();
            return;
        }
        Vector3f force = ((Vector3f)TL_vector3f_pool.get().alloc()).set(0.0f, 0.0f, 0.0f);
        Vector3f torque = ((Vector3f)TL_vector3f_pool.get().alloc()).set(0.0f, 0.0f, 0.0f);
        Vector3f cross = ((Vector3f)TL_vector3f_pool.get().alloc()).set(0.0f, 0.0f, 0.0f);
        int count = this.impulsesFromHitObjects.size();
        for (int i = 0; i < count; ++i) {
            VehicleImpulse impulse = this.impulsesFromHitObjects.get(i);
            force.add(impulse.impulse);
            torque.add(impulse.relPos.cross(impulse.impulse, cross));
            impulse.release();
            impulse.enable = false;
        }
        this.impulsesFromHitObjects.clear();
        float limit = 1500.0f * this.getFudgedMass();
        if (force.lengthSquared() > limit * limit) {
            float limitScalar = limit / force.length();
            force.mul(limitScalar);
            torque.mul(limitScalar);
        }
        float fpsScale = 30.0f;
        Bullet.applyCentralForceToVehicle(this.vehicleId, force.x * 30.0f, force.y * 30.0f, force.z * 30.0f);
        Bullet.applyTorqueToVehicle(this.vehicleId, torque.x * 30.0f, torque.y * 30.0f, torque.z * 30.0f);
        TL_vector3f_pool.get().release(force);
        TL_vector3f_pool.get().release(torque);
        TL_vector3f_pool.get().release(cross);
    }

    public void applyAllImpulsesFromProneCharacters() {
        if (GameClient.client && !this.hasAuthorization(GameClient.connection) || GameServer.server) {
            return;
        }
        boolean hasImpulse = PZArrayUtil.contains(this.impulsesFromSquishedBodies, impulse -> impulse != null && impulse.enable);
        if (!hasImpulse) {
            return;
        }
        Vector3f force = ((Vector3f)TL_vector3f_pool.get().alloc()).set(0.0f, 0.0f, 0.0f);
        Vector3f torque = ((Vector3f)TL_vector3f_pool.get().alloc()).set(0.0f, 0.0f, 0.0f);
        Vector3f cross = (Vector3f)TL_vector3f_pool.get().alloc();
        for (int i = 0; i < this.impulsesFromSquishedBodies.length; ++i) {
            VehicleImpulse impulse2 = this.impulsesFromSquishedBodies[i];
            if (impulse2 == null || !impulse2.enable || impulse2.applied) continue;
            force.add(impulse2.impulse);
            torque.add(impulse2.relPos.cross(impulse2.impulse, cross));
            impulse2.applied = true;
        }
        if (force.lengthSquared() > 0.0f) {
            float limit = this.getFudgedMass() * 0.15f;
            if (force.lengthSquared() > limit * limit) {
                force.mul(limit / force.length());
            }
            float fpsScale = 30.0f;
            Bullet.applyCentralForceToVehicle(this.vehicleId, force.x * 30.0f, force.y * 30.0f, force.z * 30.0f);
            Bullet.applyTorqueToVehicle(this.vehicleId, torque.x * 30.0f, torque.y * 30.0f, torque.z * 30.0f);
        }
        TL_vector3f_pool.get().release(force);
        TL_vector3f_pool.get().release(torque);
        TL_vector3f_pool.get().release(cross);
    }

    public float getFudgedMass() {
        if (this.getScriptName().contains("Trailer")) {
            return this.getMass();
        }
        BaseVehicle vehicleA = this.getVehicleTowedBy();
        if (vehicleA != null && vehicleA.getDriver() != null && vehicleA.isEngineRunning()) {
            float mass = Math.max(250.0f, vehicleA.getMass() / 3.7f);
            if (this.getScript().getWheelCount() == 0) {
                mass = Math.min(mass, 200.0f);
            }
            return mass;
        }
        return this.getMass();
    }

    private boolean isNullChunk(int wx, int wy) {
        if (!IsoWorld.instance.getMetaGrid().isValidChunk(wx, wy)) {
            return false;
        }
        if (GameClient.client && !ClientServerMap.isChunkLoaded(wx, wy)) {
            return true;
        }
        if (GameClient.client && !PassengerMap.isChunkLoaded(this, wx, wy)) {
            return true;
        }
        return this.getCell().getChunk(wx, wy) == null;
    }

    public boolean isInvalidChunkAround() {
        Vector3f velocity = this.getLinearVelocity(BaseVehicle.allocVector3f());
        float absX = Math.abs(velocity.x);
        float absY = Math.abs(velocity.z);
        boolean moveW = velocity.x < 0.0f && absX > absY;
        boolean moveE = velocity.x > 0.0f && absX > absY;
        boolean moveN = velocity.z < 0.0f && absY > absX;
        boolean moveS = velocity.z > 0.0f && absY > absX;
        BaseVehicle.releaseVector3f(velocity);
        return this.isInvalidChunkAround(moveW, moveE, moveN, moveS);
    }

    public boolean isInvalidChunkAhead() {
        Vector3f angle = this.getForwardVector(BaseVehicle.allocVector3f());
        boolean moveW = angle.x < -0.5f;
        boolean moveS = angle.z > 0.5f;
        boolean moveE = angle.x > 0.5f;
        boolean moveN = angle.z < -0.5f;
        BaseVehicle.releaseVector3f(angle);
        return this.isInvalidChunkAround(moveW, moveE, moveN, moveS);
    }

    public boolean isInvalidChunkBehind() {
        Vector3f angle = this.getForwardVector(BaseVehicle.allocVector3f());
        boolean moveE = angle.x < -0.5f;
        boolean moveN = angle.z > 0.5f;
        boolean moveW = angle.x > 0.5f;
        boolean moveS = angle.z < -0.5f;
        BaseVehicle.releaseVector3f(angle);
        return this.isInvalidChunkAround(moveW, moveE, moveN, moveS);
    }

    public boolean isInvalidChunkAround(boolean moveW, boolean moveE, boolean moveN, boolean moveS) {
        if (IsoChunkMap.chunkGridWidth > 7) {
            if (moveE && (this.isNullChunk(this.chunk.wx + 1, this.chunk.wy) || this.isNullChunk(this.chunk.wx + 2, this.chunk.wy))) {
                return true;
            }
            if (moveW && (this.isNullChunk(this.chunk.wx - 1, this.chunk.wy) || this.isNullChunk(this.chunk.wx - 2, this.chunk.wy))) {
                return true;
            }
            if (moveS && (this.isNullChunk(this.chunk.wx, this.chunk.wy + 1) || this.isNullChunk(this.chunk.wx, this.chunk.wy + 2))) {
                return true;
            }
            if (moveN && (this.isNullChunk(this.chunk.wx, this.chunk.wy - 1) || this.isNullChunk(this.chunk.wx, this.chunk.wy - 2))) {
                return true;
            }
        } else if (IsoChunkMap.chunkGridWidth > 4) {
            if (moveE && this.isNullChunk(this.chunk.wx + 1, this.chunk.wy)) {
                return true;
            }
            if (moveW && this.isNullChunk(this.chunk.wx - 1, this.chunk.wy)) {
                return true;
            }
            if (moveS && this.isNullChunk(this.chunk.wx, this.chunk.wy + 1)) {
                return true;
            }
            if (moveN && this.isNullChunk(this.chunk.wx, this.chunk.wy - 1)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void postupdate() {
        this.current = this.findCurrentGridSquare();
        this.setMovingSquare(this.current);
        this.square = this.current;
        if (this.sprite.hasActiveModel()) {
            this.updateAnimationPlayer(this.getAnimationPlayer(), null);
            for (int i = 0; i < this.models.size(); ++i) {
                ModelInfo modelInfo = this.models.get(i);
                this.updateAnimationPlayer(modelInfo.getAnimationPlayer(), modelInfo.part);
            }
        }
        this.pedestrianContacts.postUpdate(this, (vehicle, contacts) -> vehicle.applyDamageFromHitCharacters((VehiclePedestrianContactTracking)contacts));
        if (this.isAnimationRecorderActive()) {
            Object vehicleStateStr = this.isPhysicsActive() ? "Physics" : "NonPhysics";
            if (this.isAtRest()) {
                vehicleStateStr = (String)vehicleStateStr + "AtRest";
            }
            String vehicleStaticStateStr = this.isStatic ? "Static" : (this.isActive ? "Active" : "Inactive");
            AnimationPlayerRecorder animationRecorder = this.getAnimationRecorder();
            animationRecorder.logActionState("Vehicle", (String)vehicleStateStr);
            animationRecorder.logAIState(vehicleStaticStateStr);
            animationRecorder.logVariable("isStatic", this.isStatic);
            animationRecorder.logVariable("isActive", this.isActive);
            animationRecorder.logVariable("isPhysicsActive", this.isPhysicsActive());
            animationRecorder.logVariable("isInCell", this.getCell().vehicles.contains(this));
            animationRecorder.logVariable("isAtRest", this.isAtRest());
            Vector3f linearVelocity = this.getLinearVelocity(new Vector3f());
            animationRecorder.logVariable("velocity.x", linearVelocity.x);
            animationRecorder.logVariable("velocity.y", linearVelocity.y);
            animationRecorder.logVariable("velocity.z", linearVelocity.z);
            if (this.physics != null) {
                animationRecorder.logVariable("clientForce", this.physics.clientForce);
                animationRecorder.logVariable("engineForce", this.physics.engineForce);
                animationRecorder.logVariable("brakingForce", this.physics.brakingForce);
                animationRecorder.logVariable("vehicleSteering", this.physics.getVehicleSteering());
                animationRecorder.logVariable("isGas", this.physics.isGas());
                animationRecorder.logVariable("isGasR", this.physics.isGasR());
                animationRecorder.logVariable("isBreak", this.physics.isBreak());
                animationRecorder.logVariable("acceleratorOn", this.physics.acceleratorOn);
                animationRecorder.logVariable("brakeOn", this.physics.brakeOn);
                animationRecorder.logVariable("speed", this.physics.speed);
            }
        }
    }

    @Override
    public boolean shouldSnapZToCurrentSquare() {
        return !this.isPhysicsActive();
    }

    private void applyDamageFromHitCharacters(VehiclePedestrianContactTracking pedestrianContacts) {
        ArrayList<IsoGameCharacter> pedestrianHitsCausingDamage = pedestrianContacts.getHitsCausingDamage();
        int totalDmgFront = 0;
        int totalDmgBack = 0;
        for (int i = 0; i < pedestrianHitsCausingDamage.size(); ++i) {
            IsoGameCharacter chr = pedestrianHitsCausingDamage.get(i);
            boolean isInFront = this.isCharacterInFront(chr);
            int dmg = this.calculateDamageWithCharacter(chr);
            if (isInFront) {
                totalDmgFront += dmg;
                continue;
            }
            totalDmgBack += dmg;
        }
        if (totalDmgFront != 0 || totalDmgBack != 0) {
            if (GameClient.client) {
                GameClient.instance.sendClientCommandV(null, "vehicle", "damageFromHitChr", "vehicle", this.getId(), "dmgFront", totalDmgFront, "dmgBack", totalDmgBack);
                return;
            }
            this.damageFromHitChr(totalDmgFront, totalDmgBack);
        }
    }

    public void damageFromHitChr(int dmgFront, int dmgBack) {
        if (dmgFront > 0) {
            DebugType.VehicleHit.debugln("Vehicle receives damage from pedestrian hits on the Front: %d", dmgFront);
            this.addDamageFrontHitAChr(dmgFront);
        }
        if (dmgBack > 0) {
            DebugType.VehicleHit.debugln("Vehicle receives damage from pedestrian hits on the Back: %d", dmgBack);
            this.addDamageRearHitAChr(dmgBack);
        }
    }

    private void updateAnimationPlayer(AnimationPlayer animPlayer, VehiclePart part) {
        VehicleWindow window;
        if (animPlayer == null || !animPlayer.isReady()) {
            return;
        }
        AnimationMultiTrack multiTrack = animPlayer.getMultiTrack();
        float del = 0.016666668f;
        del *= 0.8f;
        animPlayer.Update(del *= GameTime.instance.getUnmoddedMultiplier());
        for (int i = 0; i < multiTrack.getTrackCount(); ++i) {
            AnimationTrack track = multiTrack.getTracks().get(i);
            if (!track.isPlaying || !track.isFinished()) continue;
            multiTrack.removeTrackAt(i);
            --i;
        }
        if (part == null) {
            return;
        }
        ModelInfo modelInfo = this.getModelInfoForPart(part);
        if (modelInfo.track != null && multiTrack.getIndexOfTrack(modelInfo.track) == -1) {
            modelInfo.track = null;
        }
        if (modelInfo.track != null) {
            VehicleWindow window2 = part.getWindow();
            if (window2 != null) {
                AnimationTrack track = modelInfo.track;
                track.setCurrentTimeValue(track.getDuration() * window2.getOpenDelta());
            }
            return;
        }
        VehicleDoor door = part.getDoor();
        if (door != null) {
            this.playPartAnim(part, door.isOpen() ? "Opened" : "Closed");
        }
        if ((window = part.getWindow()) != null) {
            this.playPartAnim(part, "ClosedToOpen");
        }
    }

    public void authorizationClientCollide(IsoPlayer driver) {
        if (driver != null && this.getDriver() == null) {
            this.setNetPlayerAuthorization(Authorization.LocalCollide, driver.getOnlineID());
            this.authSimulationTime = System.currentTimeMillis();
            this.interpolation.clear();
            if (this.getVehicleTowing() != null) {
                this.getVehicleTowing().setNetPlayerAuthorization(Authorization.LocalCollide, driver.getOnlineID());
                this.getVehicleTowing().authSimulationTime = System.currentTimeMillis();
                this.getVehicleTowing().interpolation.clear();
            } else if (this.getVehicleTowedBy() != null) {
                this.getVehicleTowedBy().setNetPlayerAuthorization(Authorization.LocalCollide, driver.getOnlineID());
                this.getVehicleTowedBy().authSimulationTime = System.currentTimeMillis();
                this.getVehicleTowedBy().interpolation.clear();
            }
        }
    }

    public void authorizationServerCollide(short playerId, boolean isCollide) {
        if (this.isNetPlayerAuthorization(Authorization.Local)) {
            return;
        }
        if (isCollide) {
            this.setNetPlayerAuthorization(Authorization.LocalCollide, playerId);
            if (this.getVehicleTowing() != null) {
                this.getVehicleTowing().setNetPlayerAuthorization(Authorization.LocalCollide, playerId);
            } else if (this.getVehicleTowedBy() != null) {
                this.getVehicleTowedBy().setNetPlayerAuthorization(Authorization.LocalCollide, playerId);
            }
        } else {
            Authorization auth = playerId == -1 ? Authorization.Server : Authorization.Local;
            this.setNetPlayerAuthorization(auth, playerId);
            if (this.getVehicleTowing() != null) {
                this.getVehicleTowing().setNetPlayerAuthorization(auth, playerId);
            } else if (this.getVehicleTowedBy() != null) {
                this.getVehicleTowedBy().setNetPlayerAuthorization(auth, playerId);
            }
        }
    }

    public void authorizationServerOnSeat(IsoPlayer player, boolean enter) {
        BaseVehicle vehicleA = this.getVehicleTowing();
        BaseVehicle vehicleB = this.getVehicleTowedBy();
        if (this.isNetPlayerId((short)-1) && enter) {
            if (vehicleA != null && vehicleA.getDriver() == null) {
                this.addPointConstraint(null, vehicleA, this.getTowAttachmentSelf(), vehicleA.getTowAttachmentSelf());
            } else if (vehicleB != null && vehicleB.getDriver() == null) {
                this.addPointConstraint(null, vehicleB, this.getTowAttachmentSelf(), vehicleB.getTowAttachmentSelf());
            } else {
                this.setNetPlayerAuthorization(Authorization.Local, player.getOnlineID());
            }
        } else if (this.isNetPlayerId(player.getOnlineID()) && !enter) {
            if (vehicleA != null && vehicleA.getDriver() != null) {
                vehicleA.addPointConstraint(null, this, vehicleA.getTowAttachmentSelf(), this.getTowAttachmentSelf());
            } else if (vehicleB != null && vehicleB.getDriver() != null) {
                vehicleB.addPointConstraint(null, this, vehicleB.getTowAttachmentSelf(), this.getTowAttachmentSelf());
            } else {
                this.setNetPlayerAuthorization(Authorization.Server, -1);
                if (vehicleA != null) {
                    vehicleA.setNetPlayerAuthorization(Authorization.Server, -1);
                } else if (vehicleB != null) {
                    vehicleB.setNetPlayerAuthorization(Authorization.Server, -1);
                }
            }
        }
    }

    public boolean hasAuthorization(UdpConnection connection) {
        if (this.isNetPlayerId((short)-1) || connection == null) {
            return false;
        }
        if (GameServer.server) {
            for (int i = 0; i < connection.players.length; ++i) {
                if (connection.players[i] == null || !this.isNetPlayerId(connection.players[i].onlineId)) continue;
                return true;
            }
            return false;
        }
        return this.isNetPlayerId(IsoPlayer.getInstance().getOnlineID());
    }

    public void netPlayerFromServerUpdate(Authorization authorization, short authorizationPlayer) {
        if (this.isNetPlayerAuthorization(authorization) && this.isNetPlayerId(authorizationPlayer)) {
            return;
        }
        if (Authorization.Local == authorization) {
            if (IsoPlayer.getLocalPlayerByOnlineID(authorizationPlayer) != null) {
                this.setNetPlayerAuthorization(Authorization.Local, authorizationPlayer);
            } else {
                this.setNetPlayerAuthorization(Authorization.Remote, authorizationPlayer);
            }
        } else if (Authorization.LocalCollide == authorization) {
            if (IsoPlayer.getLocalPlayerByOnlineID(authorizationPlayer) != null) {
                this.setNetPlayerAuthorization(Authorization.LocalCollide, authorizationPlayer);
            } else {
                this.setNetPlayerAuthorization(Authorization.RemoteCollide, authorizationPlayer);
            }
        } else {
            this.setNetPlayerAuthorization(Authorization.Server, -1);
        }
    }

    public Transform getWorldTransform(Transform out) {
        out.set(this.jniTransform);
        return out;
    }

    public void setWorldTransform(Transform in) {
        this.jniTransform.set(in);
        Quaternionf rotation = BaseVehicle.allocQuaternionf();
        in.getRotation(rotation);
        if (!GameServer.server) {
            Bullet.teleportVehicle(this.vehicleId, in.origin.x + WorldSimulation.instance.offsetX, in.origin.z + WorldSimulation.instance.offsetY, in.origin.y, rotation.x, rotation.y, rotation.z, rotation.w);
        }
        BaseVehicle.releaseQuaternionf(rotation);
    }

    public void flipUpright() {
        Transform xfrm = BaseVehicle.allocTransform();
        xfrm.set(this.jniTransform);
        Quaternionf rotation = BaseVehicle.allocQuaternionf();
        rotation.setAngleAxis(0.0f, BaseVehicle._UNIT_Y.x, BaseVehicle._UNIT_Y.y, BaseVehicle._UNIT_Y.z);
        xfrm.setRotation(rotation);
        BaseVehicle.releaseQuaternionf(rotation);
        this.setWorldTransform(xfrm);
        BaseVehicle.releaseTransform(xfrm);
    }

    public void setAngles(float degreesX, float degreesY, float degreesZ) {
        if ((int)degreesX == (int)this.getAngleX() && (int)degreesY == (int)this.getAngleY() && degreesZ == (float)((int)this.getAngleZ())) {
            return;
        }
        this.polyDirty = true;
        float radiansX = degreesX * ((float)Math.PI / 180);
        float radiansY = degreesY * ((float)Math.PI / 180);
        float radiansZ = degreesZ * ((float)Math.PI / 180);
        Quaternionf q = BaseVehicle.allocQuaternionf();
        q.rotationXYZ(radiansX, radiansY, radiansZ);
        Transform xfrm = BaseVehicle.allocTransform();
        xfrm.set(this.jniTransform);
        xfrm.setRotation(q);
        BaseVehicle.releaseQuaternionf(q);
        this.setWorldTransform(xfrm);
        BaseVehicle.releaseTransform(xfrm);
    }

    public float getAngleX() {
        Vector3f v = (Vector3f)TL_vector3f_pool.get().alloc();
        Quaternionf q = BaseVehicle.allocQuaternionf();
        this.jniTransform.getRotation(q).getEulerAnglesXYZ(v);
        BaseVehicle.releaseQuaternionf(q);
        float angle = v.x * 57.295776f;
        TL_vector3f_pool.get().release(v);
        return angle;
    }

    public float getAngleY() {
        Vector3f v = (Vector3f)TL_vector3f_pool.get().alloc();
        Quaternionf q = BaseVehicle.allocQuaternionf();
        this.jniTransform.getRotation(q).getEulerAnglesXYZ(v);
        BaseVehicle.releaseQuaternionf(q);
        float angle = v.y * 57.295776f;
        TL_vector3f_pool.get().release(v);
        return angle;
    }

    public float getAngleZ() {
        Vector3f v = (Vector3f)TL_vector3f_pool.get().alloc();
        Quaternionf q = BaseVehicle.allocQuaternionf();
        this.jniTransform.getRotation(q).getEulerAnglesXYZ(v);
        BaseVehicle.releaseQuaternionf(q);
        float angle = v.z * 57.295776f;
        TL_vector3f_pool.get().release(v);
        return angle;
    }

    public void setDebugZ(float z) {
        Transform xfrm = BaseVehicle.allocTransform();
        xfrm.set(this.jniTransform);
        int zi = PZMath.fastfloor(this.jniTransform.origin.y / 2.44949f);
        xfrm.origin.y = ((float)zi + PZMath.clamp(z, 0.0f, 0.99f)) * 3.0f * 0.8164967f;
        this.setWorldTransform(xfrm);
        BaseVehicle.releaseTransform(xfrm);
    }

    public void setPhysicsActive(boolean active) {
        BaseVehicle towedVehicle;
        if (this.physics == null || active == this.physics.isEnable) {
            return;
        }
        this.physics.isEnable = active;
        if (!GameServer.server) {
            if (this.isStatic != !active) {
                Bullet.setVehicleStatic(this, !active);
            }
            if (this.isActive != active) {
                Bullet.setVehicleActive(this, active);
            }
        }
        if (active) {
            this.physicActiveCheck = System.currentTimeMillis() + 3000L;
        }
        if ((towedVehicle = this.getVehicleTowing()) != null) {
            towedVehicle.setPhysicsActive(active);
        }
    }

    public boolean isPhysicsActive() {
        return this.physics != null && this.physics.isEnable;
    }

    public float getDebugZ() {
        return this.jniTransform.origin.y / 2.44949f;
    }

    public VehiclePoly getPoly() {
        if (this.polyDirty) {
            if (this.polyGarageCheck && this.square != null) {
                this.radiusReductionInGarage = this.square.getRoom() != null && this.square.getRoom().roomDef != null && this.square.getRoom().roomDef.contains("garagestorage") ? -0.3f : 0.0f;
                this.polyGarageCheck = false;
            }
            this.poly.init(this, 0.0f);
            this.polyPlusRadius.init(this, 0.15f + this.radiusReductionInGarage);
            this.polyDirty = false;
            this.polyPlusRadiusMinX = -123.0f;
            this.initShadowPoly();
        }
        return this.poly;
    }

    public VehiclePoly getPolyPlusRadius() {
        if (this.polyDirty) {
            if (this.polyGarageCheck && this.square != null) {
                this.radiusReductionInGarage = this.square.getRoom() != null && this.square.getRoom().roomDef != null && this.square.getRoom().roomDef.contains("garagestorage") ? -0.3f : 0.0f;
                this.polyGarageCheck = false;
            }
            this.poly.init(this, 0.0f);
            this.polyPlusRadius.init(this, 0.15f + this.radiusReductionInGarage);
            this.polyDirty = false;
            this.polyPlusRadiusMinX = -123.0f;
            this.initShadowPoly();
        }
        return this.polyPlusRadius;
    }

    private void initShadowPoly() {
        Transform xfrm = this.getWorldTransform(BaseVehicle.allocTransform());
        Quaternionf q = xfrm.getRotation(BaseVehicle.allocQuaternionf());
        BaseVehicle.releaseTransform(xfrm);
        Vector2f ext = this.script.getShadowExtents();
        Vector2f off = this.script.getShadowOffset();
        float width = ext.x / 2.0f;
        float length = ext.y / 2.0f;
        Vector3f v = BaseVehicle.allocVector3f();
        if (q.x < 0.0f) {
            this.getWorldPos(off.x - width, 0.0f, off.y + length, v);
            this.shadowCoord.x1 = v.x;
            this.shadowCoord.y1 = v.y;
            this.getWorldPos(off.x + width, 0.0f, off.y + length, v);
            this.shadowCoord.x2 = v.x;
            this.shadowCoord.y2 = v.y;
            this.getWorldPos(off.x + width, 0.0f, off.y - length, v);
            this.shadowCoord.x3 = v.x;
            this.shadowCoord.y3 = v.y;
            this.getWorldPos(off.x - width, 0.0f, off.y - length, v);
            this.shadowCoord.x4 = v.x;
            this.shadowCoord.y4 = v.y;
        } else {
            this.getWorldPos(off.x - width, 0.0f, off.y + length, v);
            this.shadowCoord.x1 = v.x;
            this.shadowCoord.y1 = v.y;
            this.getWorldPos(off.x + width, 0.0f, off.y + length, v);
            this.shadowCoord.x2 = v.x;
            this.shadowCoord.y2 = v.y;
            this.getWorldPos(off.x + width, 0.0f, off.y - length, v);
            this.shadowCoord.x3 = v.x;
            this.shadowCoord.y3 = v.y;
            this.getWorldPos(off.x - width, 0.0f, off.y - length, v);
            this.shadowCoord.x4 = v.x;
            this.shadowCoord.y4 = v.y;
        }
        BaseVehicle.releaseVector3f(v);
        BaseVehicle.releaseQuaternionf(q);
    }

    private void initPolyPlusRadiusBounds() {
        if (this.polyPlusRadiusMinX != -123.0f) {
            return;
        }
        VehiclePoly poly = this.getPolyPlusRadius();
        Vector3f localPos = (Vector3f)TL_vector3f_pool.get().alloc();
        Vector3f v = this.getLocalPos(poly.x1, poly.y1, poly.z, localPos);
        float x1 = (float)PZMath.fastfloor(v.x * 100.0f) / 100.0f;
        float y1 = (float)PZMath.fastfloor(v.z * 100.0f) / 100.0f;
        v = this.getLocalPos(poly.x2, poly.y2, poly.z, localPos);
        float x2 = (float)PZMath.fastfloor(v.x * 100.0f) / 100.0f;
        float y2 = (float)PZMath.fastfloor(v.z * 100.0f) / 100.0f;
        v = this.getLocalPos(poly.x3, poly.y3, poly.z, localPos);
        float x3 = (float)PZMath.fastfloor(v.x * 100.0f) / 100.0f;
        float y3 = (float)PZMath.fastfloor(v.z * 100.0f) / 100.0f;
        v = this.getLocalPos(poly.x4, poly.y4, poly.z, localPos);
        float x4 = (float)PZMath.fastfloor(v.x * 100.0f) / 100.0f;
        float y4 = (float)PZMath.fastfloor(v.z * 100.0f) / 100.0f;
        this.polyPlusRadiusMinX = Math.min(x1, Math.min(x2, Math.min(x3, x4)));
        this.polyPlusRadiusMaxX = Math.max(x1, Math.max(x2, Math.max(x3, x4)));
        this.polyPlusRadiusMinY = Math.min(y1, Math.min(y2, Math.min(y3, y4)));
        this.polyPlusRadiusMaxY = Math.max(y1, Math.max(y2, Math.max(y3, y4)));
        TL_vector3f_pool.get().release(localPos);
    }

    public Vector3f getForwardVector(Vector3f out) {
        int forwardAxis = 2;
        return this.jniTransform.basis.getColumn(2, out);
    }

    public Vector3f getUpVector(Vector3f out) {
        boolean axis = true;
        return this.jniTransform.basis.getColumn(1, out);
    }

    public float getUpVectorDot() {
        Vector3f up = this.getUpVector((Vector3f)TL_vector3f_pool.get().alloc());
        float dot = up.dot(_UNIT_Y);
        TL_vector3f_pool.get().release(up);
        return dot;
    }

    public boolean isStopped() {
        return this.getCurrentAbsoluteSpeedKmHour() < 0.8f && !this.getController().isGasPedalPressed();
    }

    public void setSpeedKmHour(float speedKmHour) {
        this.jniSpeed = speedKmHour;
    }

    public float getCurrentSpeedKmHour() {
        if (this.physics != null && !this.physics.isEnable) {
            this.jniSpeed = 0.0f;
            return 0.0f;
        }
        return this.jniSpeed;
    }

    public float getCurrentAbsoluteSpeedKmHour() {
        return PZMath.abs(this.getCurrentSpeedKmHour());
    }

    public Vector3f getLinearVelocity(Vector3f out) {
        return out.set(this.jniLinearVelocity);
    }

    public float getSpeed2D() {
        float vx = this.jniLinearVelocity.x;
        float vy = this.jniLinearVelocity.z;
        return (float)Math.sqrt(vx * vx + vy * vy);
    }

    public boolean isAtRest() {
        if (this.physics == null) {
            return true;
        }
        if (!this.impulsesFromHitObjects.isEmpty()) {
            return false;
        }
        if (this.impulseFromServer.enable) {
            return false;
        }
        if (Math.abs(this.physics.engineForce) >= 0.01f) {
            return false;
        }
        if (this.getSpeed2D() >= 0.02f) {
            return false;
        }
        float velocityVertical = this.jniLinearVelocity.y;
        if (Math.abs(velocityVertical) >= 0.5f) {
            return false;
        }
        IsoGridSquare ourSquare = this.getSquare();
        if (!ourSquare.hasFloor()) {
            return false;
        }
        float z = this.jniTransform.origin.y / 2.44949f;
        float heightAboveGround = z - (float)ourSquare.z;
        return !(heightAboveGround > 0.2f);
    }

    protected void updateTransform() {
        if (this.sprite.modelSlot == null) {
            return;
        }
        float scale = this.getScript().getModelScale();
        float scale2 = 1.0f;
        if (this.sprite.modelSlot != null && this.sprite.modelSlot.model.scale != 1.0f) {
            scale2 = this.sprite.modelSlot.model.scale;
        }
        Quaternionf chassisRot = (Quaternionf)TL_quaternionf_pool.get().alloc();
        Quaternionf modelRotQ = (Quaternionf)TL_quaternionf_pool.get().alloc();
        Matrix4f matrix4f = (Matrix4f)TL_matrix4f_pool.get().alloc();
        int rightToLeftHand = -1;
        Transform chassisTrans = this.getWorldTransform(BaseVehicle.allocTransform());
        chassisTrans.getRotation(chassisRot);
        BaseVehicle.releaseTransform(chassisTrans);
        chassisRot.y *= -1.0f;
        chassisRot.z *= -1.0f;
        Matrix4f chassisRotM = chassisRot.get(matrix4f);
        float scaleInvertX = 1.0f;
        if (this.sprite.modelSlot.model.modelScript != null) {
            scaleInvertX = this.sprite.modelSlot.model.modelScript.invertX ? -1.0f : 1.0f;
        }
        Vector3f modelOffset = this.script.getModel().getOffset();
        Vector3f modelRotate = this.getScript().getModel().getRotate();
        modelRotQ.rotationXYZ(modelRotate.x * ((float)Math.PI / 180), modelRotate.y * ((float)Math.PI / 180), modelRotate.z * ((float)Math.PI / 180));
        this.renderTransform.translationRotateScale(modelOffset.x * -1.0f, modelOffset.y, modelOffset.z, modelRotQ.x, modelRotQ.y, modelRotQ.z, modelRotQ.w, scale * scale2 * scaleInvertX, scale * scale2, scale * scale2);
        chassisRotM.mul(this.renderTransform, this.renderTransform);
        this.vehicleTransform.translationRotateScale(modelOffset.x * -1.0f, modelOffset.y, modelOffset.z, 0.0f, 0.0f, 0.0f, 1.0f, scale);
        chassisRotM.mul(this.vehicleTransform, this.vehicleTransform);
        for (int i = 0; i < this.models.size(); ++i) {
            float corpseOffset;
            ModelInfo modelInfo = this.models.get(i);
            VehicleScript.Model scriptModel = modelInfo.scriptModel;
            modelOffset = scriptModel.getOffset();
            modelRotate = scriptModel.getRotate();
            float scale1 = scriptModel.scale;
            scale2 = 1.0f;
            float scaleInvertX2 = 1.0f;
            if (modelInfo.modelScript != null) {
                scale2 = modelInfo.modelScript.scale;
                scaleInvertX2 = modelInfo.modelScript.invertX ? -1.0f : 1.0f;
            }
            int rotateYZ = 1;
            if (modelInfo.wheelIndex == -1) {
                rotateYZ = -1;
            }
            modelRotQ.rotationXYZ(modelRotate.x * ((float)Math.PI / 180), modelRotate.y * ((float)Math.PI / 180) * (float)rotateYZ, modelRotate.z * ((float)Math.PI / 180) * (float)rotateYZ);
            if (modelInfo.wheelIndex == -1) {
                if (modelInfo.part != null && modelInfo.part.scriptPart != null && modelInfo.part.scriptPart.parent != null && scriptModel.attachmentNameParent != null) {
                    ModelInfo parentModelInfo = this.getModelInfoForPart(modelInfo.part.getParent());
                    Matrix4f attachmentXfrm = (Matrix4f)TL_matrix4f_pool.get().alloc();
                    this.initTransform(parentModelInfo.modelInstance, parentModelInfo.modelScript, modelInfo.modelScript, scriptModel.attachmentNameParent, scriptModel.attachmentNameSelf, attachmentXfrm);
                    Model parentModel = parentModelInfo.modelInstance.model;
                    ModelInstanceRenderData.preMultiplyMeshTransform(attachmentXfrm, parentModel.mesh);
                    parentModelInfo.renderTransform.mul(attachmentXfrm, modelInfo.renderTransform);
                    boolean bIgnoreVehicleScale = scriptModel.ignoreVehicleScale;
                    float scale3 = bIgnoreVehicleScale ? 1.5f / this.getScript().getModelScale() : 1.0f;
                    modelInfo.renderTransform.scale(scale1 * scale2 * scale3);
                    TL_matrix4f_pool.get().release(attachmentXfrm);
                    continue;
                }
                modelInfo.renderTransform.translationRotateScale(modelOffset.x * -1.0f, modelOffset.y, modelOffset.z, modelRotQ.x, modelRotQ.y, modelRotQ.z, modelRotQ.w, scale1 * scale2 * scaleInvertX2, scale1 * scale2, scale1 * scale2);
                this.vehicleTransform.mul(modelInfo.renderTransform, modelInfo.renderTransform);
                continue;
            }
            WheelInfo wheelInfo = this.wheelInfo[modelInfo.wheelIndex];
            float steering = wheelInfo.steering;
            float rotate = wheelInfo.rotation;
            VehicleScript.Wheel scriptWheel = this.getScript().getWheel(modelInfo.wheelIndex);
            VehicleImpulse impulse = modelInfo.wheelIndex < this.impulsesFromSquishedBodies.length ? this.impulsesFromSquishedBodies[modelInfo.wheelIndex] : null;
            float f = corpseOffset = impulse != null && impulse.enable ? 0.05f : 0.0f;
            if (wheelInfo.suspensionLength == 0.0f) {
                matrix4f.translation(scriptWheel.offset.x / scale * -1.0f, scriptWheel.offset.y / scale, scriptWheel.offset.z / scale);
            } else {
                matrix4f.translation(scriptWheel.offset.x / scale * -1.0f, (scriptWheel.offset.y + this.script.getSuspensionRestLength() - wheelInfo.suspensionLength) / scale + corpseOffset * 0.5f, scriptWheel.offset.z / scale);
            }
            modelInfo.renderTransform.identity();
            modelInfo.renderTransform.mul(matrix4f);
            modelInfo.renderTransform.rotateY(steering * -1.0f);
            modelInfo.renderTransform.rotateX(rotate);
            matrix4f.translationRotateScale(modelOffset.x * -1.0f, modelOffset.y, modelOffset.z, modelRotQ.x, modelRotQ.y, modelRotQ.z, modelRotQ.w, scale1 * scale2 * scaleInvertX2, scale1 * scale2, scale1 * scale2);
            modelInfo.renderTransform.mul(matrix4f);
            this.vehicleTransform.mul(modelInfo.renderTransform, modelInfo.renderTransform);
        }
        TL_matrix4f_pool.get().release(matrix4f);
        TL_quaternionf_pool.get().release(chassisRot);
        TL_quaternionf_pool.get().release(modelRotQ);
    }

    private void initTransform(ModelInstance parentModelInstance, ModelScript parentModelScript, ModelScript modelScript, String attachmentNameParent, String attachmentNameSelf, Matrix4f transform) {
        ModelAttachment selfAttachment;
        transform.identity();
        Matrix4f attachmentXfrm = (Matrix4f)TL_matrix4f_pool.get().alloc();
        ModelAttachment parentAttachment = parentModelScript.getAttachmentById(attachmentNameParent);
        if (parentAttachment == null) {
            parentAttachment = this.getScript().getAttachmentById(attachmentNameParent);
        }
        if (parentAttachment != null) {
            ModelInstanceRenderData.makeBoneTransform(parentModelInstance.animPlayer, parentAttachment.getBone(), transform);
            transform.scale(1.0f / parentModelScript.scale);
            ModelInstanceRenderData.makeAttachmentTransform(parentAttachment, attachmentXfrm);
            transform.mul(attachmentXfrm);
        }
        if ((selfAttachment = modelScript.getAttachmentById(attachmentNameSelf)) != null) {
            ModelInstanceRenderData.makeAttachmentTransform(selfAttachment, attachmentXfrm);
            if (ModelInstanceRenderData.invertAttachmentSelfTransform) {
                attachmentXfrm.invert();
            }
            transform.mul(attachmentXfrm);
        }
        TL_matrix4f_pool.get().release(attachmentXfrm);
    }

    public void updatePhysics() {
        this.physics.update();
    }

    public void updatePhysicsNetwork() {
        if (this.limitPhysicSend.Check()) {
            INetworkPacket.send(this.isReliable ? PacketTypes.PacketType.VehiclePhysicsReliable : PacketTypes.PacketType.VehiclePhysicsUnreliable, this);
            if (this.limitPhysicPositionSent == null) {
                this.limitPhysicPositionSent = new Vector2();
            } else if (IsoUtils.DistanceToSquared(this.limitPhysicPositionSent.x, this.limitPhysicPositionSent.y, this.getX(), this.getY()) > 0.001f) {
                this.limitPhysicSend.setUpdatePeriod(150L);
            } else {
                this.limitPhysicSend.setSmoothUpdatePeriod(300L);
            }
            this.limitPhysicPositionSent.set(this.getX(), this.getY());
        }
    }

    public void checkPhysicsValidWithServer() {
        float delta = 0.05f;
        if (this.limitPhysicValid.Check() && Bullet.getOwnVehiclePhysics(this.vehicleId, physicsParams) == 0) {
            float diffX = Math.abs(physicsParams[0] - this.getX());
            float diffY = Math.abs(physicsParams[1] - this.getY());
            if (diffX > 0.05f || diffY > 0.05f) {
                VehicleManager.instance.sendVehicleRequest(this.vehicleId, (short)2);
                DebugLog.Vehicle.trace("diff-x=%f diff-y=%f delta=%f", Float.valueOf(diffX), Float.valueOf(diffY), Float.valueOf(0.05f));
            }
        }
    }

    public void updateControls() {
        if (this.getController() == null) {
            return;
        }
        if (!this.isOperational()) {
            return;
        }
        IsoPlayer player = Type.tryCastTo(this.getDriver(), IsoPlayer.class);
        if (player != null && player.isBlockMovement()) {
            return;
        }
        this.getController().updateControls();
    }

    public boolean isKeyboardControlled() {
        IsoGameCharacter chr = this.getCharacter(0);
        return chr != null && chr == IsoPlayer.players[0] && this.getVehicleTowedBy() == null;
    }

    public int getJoypad() {
        IsoGameCharacter chr = this.getCharacter(0);
        if (chr != null && chr instanceof IsoPlayer) {
            IsoPlayer isoPlayer = (IsoPlayer)chr;
            return isoPlayer.joypadBind;
        }
        return -1;
    }

    @Override
    public void Damage(float amount) {
        this.crash(amount, true);
    }

    @Override
    public void HitByVehicle(BaseVehicle vehicle, float amount) {
        this.crash(amount, true);
    }

    public void crash(float delta, boolean front) {
        IsoPlayer driver;
        StatisticsManager.getInstance().incrementStatistic(StatisticType.Player, StatisticCategory.Vehicle, "Vehicle Crash Counter", 1.0f);
        if (GameClient.client) {
            SoundManager.instance.PlayWorldSound(this.getCrashSound(delta), this.square, 1.0f, 20.0f, 1.0f, true);
            GameClient.instance.sendClientCommandV(null, "vehicle", "crash", "vehicle", this.getId(), "amount", Float.valueOf(delta), "front", front);
            return;
        }
        float modifier = 1.3f;
        float dmg = delta;
        switch (SandboxOptions.instance.carDamageOnImpact.getValue()) {
            case 1: {
                modifier = 1.9f;
                break;
            }
            case 2: {
                modifier = 1.6f;
                break;
            }
            case 4: {
                modifier = 1.1f;
                break;
            }
            case 5: {
                modifier = 0.9f;
            }
        }
        delta = Math.abs(delta) / modifier;
        if (front) {
            this.addDamageFront((int)delta);
        } else {
            this.addDamageRear((int)Math.abs(delta / modifier));
        }
        this.damagePlayers(Math.abs(dmg));
        SoundManager.instance.PlayWorldSound(this.getCrashSound(dmg), this.square, 1.0f, 20.0f, 1.0f, true);
        if (this.getVehicleTowing() != null) {
            this.getVehicleTowing().crash(delta, front);
        }
        if (this.getAnimals() != null && !this.getAnimals().isEmpty()) {
            for (int i = 0; i < this.getAnimals().size(); ++i) {
                this.getAnimals().get(i).carCrash(delta, front);
            }
        }
        if ((driver = Type.tryCastTo(this.getDriverRegardlessOfTow(), IsoPlayer.class)) != null && driver.isLocalPlayer()) {
            driver.triggerMusicIntensityEvent("VehicleCrash");
        }
    }

    private String getCrashSound(float dmg) {
        if (dmg < 5.0f) {
            return "VehicleCrash1";
        }
        if (dmg < 30.0f) {
            return "VehicleCrash2";
        }
        return "VehicleCrash";
    }

    public void addDamageFrontHitAChr(int dmg) {
        if (this.isDriverGodMode()) {
            return;
        }
        if (dmg < 4 && Rand.NextBool(7)) {
            return;
        }
        VehiclePart part = this.getPartById("EngineDoor");
        if (part != null && part.getInventoryItem() != null) {
            part.damage(Rand.Next(Math.max(1, dmg - 10), dmg + 3));
        }
        if (part != null && (part.getCondition() <= 0 || part.getInventoryItem() == null) && Rand.NextBool(4) && (part = this.getPartById("Engine")) != null) {
            part.damage(Rand.Next(2, 4));
        }
        if (dmg > 12 && (part = this.getPartById("Windshield")) != null && part.getInventoryItem() != null) {
            part.damage(Rand.Next(Math.max(1, dmg - 10), dmg + 3));
        }
        if (Rand.Next(5) < dmg && (part = Rand.NextBool(2) ? this.getPartById("TireFrontLeft") : this.getPartById("TireFrontRight")) != null && part.getInventoryItem() != null) {
            part.damage(Rand.Next(1, 3));
        }
        if (Rand.Next(7) < dmg) {
            this.damageHeadlight("HeadlightLeft", Rand.Next(1, 4));
        }
        if (Rand.Next(7) < dmg) {
            this.damageHeadlight("HeadlightRight", Rand.Next(1, 4));
        }
        float intensity = this.getBloodIntensity("Front");
        this.setBloodIntensity("Front", intensity + 0.01f);
    }

    public void addDamageRearHitAChr(int dmg) {
        if (this.isDriverGodMode()) {
            return;
        }
        if (dmg < 4 && Rand.NextBool(7)) {
            return;
        }
        VehiclePart part = this.getPartById("TruckBed");
        if (part != null && part.getInventoryItem() != null) {
            part.setCondition(part.getCondition() - Rand.Next(Math.max(1, dmg - 10), dmg + 3));
            part.doInventoryItemStats((InventoryItem)part.getInventoryItem(), 0);
            this.transmitPartCondition(part);
            this.transmitPartItem(part);
        }
        if ((part = this.getPartById("DoorRear")) != null && part.getInventoryItem() != null) {
            part.damage(Rand.Next(Math.max(1, dmg - 10), dmg + 3));
        }
        if ((part = this.getPartById("TrunkDoor")) != null && part.getInventoryItem() != null) {
            part.damage(Rand.Next(Math.max(1, dmg - 10), dmg + 3));
        }
        if (dmg > 12 && (part = this.getPartById("WindshieldRear")) != null && part.getInventoryItem() != null) {
            part.damage(dmg);
        }
        if (Rand.Next(5) < dmg && (part = Rand.NextBool(2) ? this.getPartById("TireRearLeft") : this.getPartById("TireRearRight")) != null && part.getInventoryItem() != null) {
            part.damage(Rand.Next(1, 3));
        }
        if (Rand.Next(7) < dmg) {
            this.damageHeadlight("HeadlightRearLeft", Rand.Next(1, 4));
        }
        if (Rand.Next(7) < dmg) {
            this.damageHeadlight("HeadlightRearRight", Rand.Next(1, 4));
        }
        if (Rand.Next(6) < dmg && (part = this.getPartById("GasTank")) != null && part.getInventoryItem() != null) {
            part.damage(Rand.Next(1, 3));
        }
        float intensity = this.getBloodIntensity("Rear");
        this.setBloodIntensity("Rear", intensity + 0.01f);
    }

    private void addDamageFront(int dmg) {
        if (this.isDriverGodMode()) {
            return;
        }
        this.currentFrontEndDurability -= dmg;
        VehiclePart part = this.getPartById("EngineDoor");
        if (part != null && part.getInventoryItem() != null) {
            part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
        }
        if ((part == null || part.getInventoryItem() == null || part.getCondition() < 25) && (part = this.getPartById("Engine")) != null) {
            part.damage(Rand.Next(Math.max(1, dmg - 3), dmg + 3));
        }
        if ((part = this.getPartById("Windshield")) != null && part.getInventoryItem() != null) {
            part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
        }
        if (Rand.Next(4) == 0) {
            part = this.getPartById("DoorFrontLeft");
            if (part != null && part.getInventoryItem() != null) {
                part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
            }
            if ((part = this.getPartById("WindowFrontLeft")) != null && part.getInventoryItem() != null) {
                part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
            }
        }
        if (Rand.Next(4) == 0) {
            part = this.getPartById("DoorFrontRight");
            if (part != null && part.getInventoryItem() != null) {
                part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
            }
            if ((part = this.getPartById("WindowFrontRight")) != null && part.getInventoryItem() != null) {
                part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
            }
        }
        if (Rand.Next(20) < dmg) {
            this.damageHeadlight("HeadlightLeft", dmg);
        }
        if (Rand.Next(20) < dmg) {
            this.damageHeadlight("HeadlightRight", dmg);
        }
    }

    private void addDamageRear(int dmg) {
        if (this.isDriverGodMode()) {
            return;
        }
        this.currentRearEndDurability -= dmg;
        VehiclePart part = this.getPartById("TruckBed");
        if (part != null && part.getInventoryItem() != null) {
            part.setCondition(part.getCondition() - Rand.Next(Math.max(1, dmg - 5), dmg + 5));
            part.doInventoryItemStats((InventoryItem)part.getInventoryItem(), 0);
            this.transmitPartCondition(part);
            this.transmitPartItem(part);
        }
        if ((part = this.getPartById("DoorRear")) != null && part.getInventoryItem() != null) {
            part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
        }
        if ((part = this.getPartById("TrunkDoor")) != null && part.getInventoryItem() != null) {
            part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
        }
        if ((part = this.getPartById("WindshieldRear")) != null && part.getInventoryItem() != null) {
            part.damage(dmg);
        }
        if (Rand.Next(4) == 0) {
            part = this.getPartById("DoorRearLeft");
            if (part != null && part.getInventoryItem() != null) {
                part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
            }
            if ((part = this.getPartById("WindowRearLeft")) != null && part.getInventoryItem() != null) {
                part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
            }
        }
        if (Rand.Next(4) == 0) {
            part = this.getPartById("DoorRearRight");
            if (part != null && part.getInventoryItem() != null) {
                part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
            }
            if ((part = this.getPartById("WindowRearRight")) != null && part.getInventoryItem() != null) {
                part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
            }
        }
        if (Rand.Next(20) < dmg) {
            this.damageHeadlight("HeadlightRearLeft", dmg);
        }
        if (Rand.Next(20) < dmg) {
            this.damageHeadlight("HeadlightRearRight", dmg);
        }
        if (Rand.Next(20) < dmg && (part = this.getPartById("Muffler")) != null && part.getInventoryItem() != null) {
            part.damage(Rand.Next(Math.max(1, dmg - 5), dmg + 5));
        }
    }

    private void damageHeadlight(String partId, int dmg) {
        if (this.isDriverGodMode()) {
            return;
        }
        VehiclePart part = this.getPartById(partId);
        if (part != null && part.getInventoryItem() != null) {
            part.damage(dmg);
            if (part.getCondition() <= 0) {
                part.setInventoryItem(null);
                this.transmitPartItem(part);
            }
        }
    }

    private float clamp(float f1, float min, float max) {
        if (f1 < min) {
            f1 = min;
        }
        if (f1 > max) {
            f1 = max;
        }
        return f1;
    }

    private double getClosestPointOnEdge(float px, float py, float x1, float y1, float x2, float y2, double closestDistSq, Vector2f out) {
        float ox = out.x;
        float oy = out.y;
        double distSq = PZMath.closestPointOnLineSegment(x1, y1, x2, y2, px, py, 0.0, out);
        if (distSq < closestDistSq) {
            return distSq;
        }
        out.set(ox, oy);
        return closestDistSq;
    }

    public float getClosestPointOnExtents(float x, float y, Vector2f closest) {
        if (this.getScript() == null) {
            closest.set(x, y);
            return 0.0f;
        }
        Vector3f pos = BaseVehicle.allocVector3f();
        this.getLocalPos(x, y, 0.0f, pos);
        Vector3f ext = this.script.getExtents();
        Vector3f com = this.script.getCenterOfMassOffset();
        float xMin = com.x - ext.x / 2.0f;
        float xMax = com.x + ext.x / 2.0f;
        float yMin = com.z - ext.z / 2.0f;
        float yMax = com.z + ext.z / 2.0f;
        float lx = pos.x;
        float ly = pos.z;
        double distSq = 3.4028234663852886E38;
        distSq = this.getClosestPointOnEdge(lx, ly, xMin, yMin, xMax, yMin, distSq, closest);
        distSq = this.getClosestPointOnEdge(lx, ly, xMax, yMin, xMax, yMax, distSq, closest);
        distSq = this.getClosestPointOnEdge(lx, ly, xMin, yMax, xMax, yMax, distSq, closest);
        distSq = this.getClosestPointOnEdge(lx, ly, xMin, yMin, xMin, yMax, distSq, closest);
        this.getWorldPos(closest.x, 0.0f, closest.y, pos);
        closest.x = pos.x;
        closest.y = pos.y;
        BaseVehicle.releaseVector3f(pos);
        return (float)distSq;
    }

    public float getClosestPointOnPoly(float x, float y, Vector2f closest) {
        if (this.getScript() == null) {
            closest.set(x, y);
            return 0.0f;
        }
        VehiclePoly poly1 = this.getPoly();
        double distSq = 3.4028234663852886E38;
        distSq = this.getClosestPointOnEdge(x, y, poly1.x1, poly1.y1, poly1.x2, poly1.y2, distSq, closest);
        distSq = this.getClosestPointOnEdge(x, y, poly1.x2, poly1.y2, poly1.x3, poly1.y3, distSq, closest);
        distSq = this.getClosestPointOnEdge(x, y, poly1.x3, poly1.y3, poly1.x4, poly1.y4, distSq, closest);
        distSq = this.getClosestPointOnEdge(x, y, poly1.x4, poly1.y4, poly1.x1, poly1.y1, distSq, closest);
        return (float)distSq;
    }

    public float getClosestPointOnPoly(BaseVehicle other, Vector2f pointSelf, Vector2f pointOther) {
        if (this.getScript() == null && other.getScript() == null) {
            pointSelf.set(this.getX(), this.getY());
            pointOther.set(other.getX(), other.getY());
            return IsoUtils.DistanceToSquared(pointSelf.x, pointSelf.y, pointOther.x, pointOther.y);
        }
        if (this.getScript() == null) {
            pointSelf.set(this.getX(), this.getY());
            return other.getClosestPointOnPoly(this.getX(), this.getY(), pointOther);
        }
        if (other.getScript() == null) {
            pointOther.set(other.getX(), other.getY());
            return this.getClosestPointOnPoly(other.getX(), other.getY(), pointSelf);
        }
        VehiclePoly poly1 = this.getPoly();
        VehiclePoly poly2 = other.getPoly();
        Vector2[] pts1 = poly1.borders;
        Vector2[] pts2 = poly2.borders;
        Vector2f p1 = BaseVehicle.allocVector2f();
        Vector2f p2 = BaseVehicle.allocVector2f();
        double closestDistSq = Double.MAX_VALUE;
        for (int i = 0; i < 4; ++i) {
            Vector2 pt1 = pts1[i];
            Vector2 pt2 = pts1[(i + 1) % 4];
            for (int j = 0; j < 4; ++j) {
                Vector2 pt3 = pts2[j];
                Vector2 pt4 = pts2[(j + 1) % 4];
                double distSq = PZMath.closestPointsOnLineSegments(pt1.x, pt1.y, pt2.x, pt2.y, pt3.x, pt3.y, pt4.x, pt4.y, p1, p2);
                if (!(distSq < closestDistSq)) continue;
                closestDistSq = distSq;
                pointSelf.set(p1);
                pointOther.set(p2);
            }
        }
        BaseVehicle.releaseVector2f(p1);
        BaseVehicle.releaseVector2f(p2);
        return (float)closestDistSq;
    }

    public boolean intersectLineWithExtents(float x1, float y1, float x2, float y2, float adjust, Vector2f intersection) {
        float distSq;
        Vector3f p1 = this.getLocalPos(x1, y1, this.getZ(), BaseVehicle.allocVector3f());
        Vector3f p2 = this.getLocalPos(x2, y2, this.getZ(), BaseVehicle.allocVector3f());
        float lx1 = p1.x;
        float ly1 = p1.z;
        float lx2 = p2.x;
        float ly2 = p2.z;
        Vector3f ext = this.script.getExtents();
        Vector3f com = this.script.getCenterOfMassOffset();
        float xMin = com.x - ext.x / 2.0f - adjust;
        float xMax = com.x + ext.x / 2.0f + adjust;
        float yMin = com.z - ext.z / 2.0f - adjust;
        float yMax = com.z + ext.z / 2.0f + adjust;
        float closestDistSq = Float.MAX_VALUE;
        float closestX = 0.0f;
        float closestY = 0.0f;
        if (PZMath.intersectLineSegments(lx1, ly1, lx2, ly2, xMin, yMin, xMax, yMin, intersection) && (distSq = IsoUtils.DistanceToSquared(lx1, ly1, intersection.x, intersection.y)) < closestDistSq) {
            closestX = intersection.x;
            closestY = intersection.y;
            closestDistSq = distSq;
        }
        if (PZMath.intersectLineSegments(lx1, ly1, lx2, ly2, xMax, yMin, xMax, yMax, intersection) && (distSq = IsoUtils.DistanceToSquared(lx1, ly1, intersection.x, intersection.y)) < closestDistSq) {
            closestX = intersection.x;
            closestY = intersection.y;
            closestDistSq = distSq;
        }
        if (PZMath.intersectLineSegments(lx1, ly1, lx2, ly2, xMin, yMax, xMax, yMax, intersection) && (distSq = IsoUtils.DistanceToSquared(lx1, ly1, intersection.x, intersection.y)) < closestDistSq) {
            closestX = intersection.x;
            closestY = intersection.y;
            closestDistSq = distSq;
        }
        if (PZMath.intersectLineSegments(lx1, ly1, lx2, ly2, xMin, yMin, xMin, yMax, intersection) && (distSq = IsoUtils.DistanceToSquared(lx1, ly1, intersection.x, intersection.y)) < closestDistSq) {
            closestX = intersection.x;
            closestY = intersection.y;
            closestDistSq = distSq;
        }
        if (closestDistSq < Float.MAX_VALUE) {
            this.getWorldPos(closestX, 0.0f, closestY, p1);
            intersection.set(p1.x, p1.y);
            BaseVehicle.releaseVector3f(p1);
            BaseVehicle.releaseVector3f(p2);
            return true;
        }
        BaseVehicle.releaseVector3f(p1);
        BaseVehicle.releaseVector3f(p2);
        return false;
    }

    public boolean intersectLineWithPoly(float x1, float y1, float x2, float y2, Vector2f intersection) {
        VehiclePoly poly1 = this.getPoly();
        float closestDistSq = Float.MAX_VALUE;
        float closestX = 0.0f;
        float closestY = 0.0f;
        for (int i = 0; i < 4; ++i) {
            float distSq;
            Vector2 p1 = poly1.borders[i];
            Vector2 p2 = poly1.borders[(i + 1) % 4];
            if (!PZMath.intersectLineSegments(x1, y1, x2, y2, p1.x, p1.y, p2.x, p2.y, intersection) || !((distSq = IsoUtils.DistanceToSquared(x1, y1, intersection.x, intersection.y)) < closestDistSq)) continue;
            closestX = intersection.x;
            closestY = intersection.y;
            closestDistSq = distSq;
        }
        if (closestDistSq < Float.MAX_VALUE) {
            Vector3f p1 = this.getWorldPos(closestX, 0.0f, closestY, BaseVehicle.allocVector3f());
            intersection.set(p1.x, p1.y);
            BaseVehicle.releaseVector3f(p1);
            return true;
        }
        return false;
    }

    public boolean isCharacterAdjacentTo(IsoGameCharacter chr) {
        if (PZMath.fastfloor(chr.getZ()) != PZMath.fastfloor(this.getZ())) {
            return false;
        }
        Transform xfrm = this.getWorldTransform(BaseVehicle.allocTransform());
        xfrm.inverse();
        Vector3f circle = (Vector3f)TL_vector3f_pool.get().alloc();
        circle.set(chr.getX() - WorldSimulation.instance.offsetX, 0.0f, chr.getY() - WorldSimulation.instance.offsetY);
        xfrm.transform(circle);
        BaseVehicle.releaseTransform(xfrm);
        Vector3f ext = this.script.getExtents();
        Vector3f com = this.script.getCenterOfMassOffset();
        float xMin = com.x - ext.x / 2.0f;
        float xMax = com.x + ext.x / 2.0f;
        float yMin = com.z - ext.z / 2.0f;
        float yMax = com.z + ext.z / 2.0f;
        float adjacentDist = 0.5f;
        if (circle.x >= xMin - 0.5f && circle.x < xMax + 0.5f && circle.z >= yMin - 0.5f && circle.z < yMax + 0.5f) {
            TL_vector3f_pool.get().release(circle);
            return true;
        }
        TL_vector3f_pool.get().release(circle);
        return false;
    }

    public Vector2 testCollisionWithCharacter(IsoGameCharacter chr, float circleRadius, Vector2 outCollisionPos) {
        if (this.physics == null) {
            return null;
        }
        Vector3f ext = this.script.getExtents();
        Vector3f com = this.script.getCenterOfMassOffset();
        if (this.DistToProper(chr) > Math.max(ext.x / 2.0f, ext.z / 2.0f) + circleRadius + 1.0f) {
            return null;
        }
        Vector3f circle = (Vector3f)TL_vector3f_pool.get().alloc();
        this.getLocalPos(chr.getNextX(), chr.getNextY(), 0.0f, circle);
        float xMin = com.x - ext.x / 2.0f;
        float xMax = com.x + ext.x / 2.0f;
        float yMin = com.z - ext.z / 2.0f;
        float yMax = com.z + ext.z / 2.0f;
        if (circle.x > xMin && circle.x < xMax && circle.z > yMin && circle.z < yMax) {
            float dw = circle.x - xMin;
            float de = xMax - circle.x;
            float dn = circle.z - yMin;
            float ds = yMax - circle.z;
            Vector3f v = (Vector3f)TL_vector3f_pool.get().alloc();
            if (dw < de && dw < dn && dw < ds) {
                v.set(xMin - circleRadius - 0.015f, 0.0f, circle.z);
            } else if (de < dw && de < dn && de < ds) {
                v.set(xMax + circleRadius + 0.015f, 0.0f, circle.z);
            } else if (dn < dw && dn < de && dn < ds) {
                v.set(circle.x, 0.0f, yMin - circleRadius - 0.015f);
            } else if (ds < dw && ds < de && ds < dn) {
                v.set(circle.x, 0.0f, yMax + circleRadius + 0.015f);
            }
            TL_vector3f_pool.get().release(circle);
            Transform xfrm = this.getWorldTransform(BaseVehicle.allocTransform());
            xfrm.origin.set(0.0f, 0.0f, 0.0f);
            xfrm.transform(v);
            BaseVehicle.releaseTransform(xfrm);
            v.x += this.getX();
            v.z += this.getY();
            this.collideX = v.x;
            this.collideY = v.z;
            outCollisionPos.set(v.x, v.z);
            TL_vector3f_pool.get().release(v);
            return outCollisionPos;
        }
        float closestX = this.clamp(circle.x, xMin, xMax);
        float closestY = this.clamp(circle.z, yMin, yMax);
        float distanceX = circle.x - closestX;
        float distanceY = circle.z - closestY;
        TL_vector3f_pool.get().release(circle);
        float distanceSquared = distanceX * distanceX + distanceY * distanceY;
        if (distanceSquared < circleRadius * circleRadius) {
            if (distanceX == 0.0f && distanceY == 0.0f) {
                return outCollisionPos.set(-1.0f, -1.0f);
            }
            Vector3f v = (Vector3f)TL_vector3f_pool.get().alloc();
            v.set(distanceX, 0.0f, distanceY);
            v.normalize();
            v.mul(circleRadius + 0.015f);
            v.x += closestX;
            v.z += closestY;
            Transform xfrm = this.getWorldTransform(BaseVehicle.allocTransform());
            xfrm.origin.set(0.0f, 0.0f, 0.0f);
            xfrm.transform(v);
            BaseVehicle.releaseTransform(xfrm);
            v.x += this.getX();
            v.z += this.getY();
            this.collideX = v.x;
            this.collideY = v.z;
            outCollisionPos.set(v.x, v.z);
            TL_vector3f_pool.get().release(v);
            return outCollisionPos;
        }
        return null;
    }

    public int testCollisionWithProneCharacter(IsoGameCharacter chr, boolean doSound, Vector2 outImpactPosOnVehicle) {
        Vector2 animVector = chr.getAnimVector((Vector2)Vector2ObjectPool.get().alloc());
        int result = this.testCollisionWithProneCharacter(chr, animVector.x, animVector.y, doSound, outImpactPosOnVehicle);
        Vector2ObjectPool.get().release(animVector);
        return result;
    }

    public int testCollisionWithCorpse(IsoDeadBody body, boolean doSound) {
        float angleX = (float)Math.cos(body.getAngle());
        float angleY = (float)Math.sin(body.getAngle());
        return this.testCollisionWithProneCharacter(body, angleX, angleY, doSound, null);
    }

    public int testCollisionWithProneCharacter(IsoMovingObject chr, float angleX, float angleY, boolean doSound, Vector2 outImpactPosOnVehicle) {
        if (this.physics == null) {
            return 0;
        }
        if (GameServer.server) {
            return 0;
        }
        Vector3f ext = this.script.getExtents();
        if (this.DistToProper(chr) > Math.max(ext.x / 2.0f, ext.z / 2.0f) + 0.3f + 1.0f) {
            return 0;
        }
        float currentAbsoluteSpeedKmHour = this.getCurrentAbsoluteSpeedKmHour();
        if (currentAbsoluteSpeedKmHour < 3.0f) {
            return 0;
        }
        float angle = 0.65f;
        if (!(chr instanceof IsoDeadBody) || chr.getModData() == null || chr.getModData().rawget("corpseLength") != null) {
            // empty if block
        }
        float headX = chr.getX() + angleX * 0.65f;
        float headY = chr.getY() + angleY * 0.65f;
        float feetX = chr.getX() - angleX * 0.65f;
        float feetY = chr.getY() - angleY * 0.65f;
        int numWheelsHit = 0;
        Vector3f worldPos = (Vector3f)TL_vector3f_pool.get().alloc();
        Vector3f wheelPos = (Vector3f)TL_vector3f_pool.get().alloc();
        for (int i = 0; i < this.script.getWheelCount(); ++i) {
            float closestY;
            float closestX;
            VehicleScript.Wheel scriptWheel = this.script.getWheel(i);
            boolean onGround = true;
            for (int j = 0; j < this.models.size(); ++j) {
                ModelInfo modelInfo = this.models.get(j);
                if (modelInfo.wheelIndex != i) continue;
                this.getWorldPos(scriptWheel.offset.x, scriptWheel.offset.y - this.wheelInfo[i].suspensionLength, scriptWheel.offset.z, worldPos);
                if (!(worldPos.z > this.script.getWheel((int)i).radius + 0.05f)) break;
                onGround = false;
                break;
            }
            if (!onGround) continue;
            this.getWorldPos(scriptWheel.offset.x, scriptWheel.offset.y, scriptWheel.offset.z, wheelPos);
            float x1 = feetX;
            float y1 = feetY;
            float x2 = headX;
            float y2 = headY;
            float x3 = wheelPos.x;
            float y3 = wheelPos.y;
            double u = (double)((x3 - x1) * (x2 - x1) + (y3 - y1) * (y2 - y1)) / (Math.pow(x2 - x1, 2.0) + Math.pow(y2 - y1, 2.0));
            if (u <= 0.0) {
                closestX = x1;
                closestY = y1;
            } else if (u >= 1.0) {
                closestX = x2;
                closestY = y2;
            } else {
                closestX = x1 + (x2 - x1) * (float)u;
                closestY = y1 + (y2 - y1) * (float)u;
            }
            if (IsoUtils.DistanceToSquared(wheelPos.x, wheelPos.y, closestX, closestY) > scriptWheel.radius * scriptWheel.radius) continue;
            if (doSound && currentAbsoluteSpeedKmHour > 10.0f) {
                if (GameServer.server && chr instanceof IsoZombie) {
                    IsoZombie isoZombie = (IsoZombie)chr;
                    isoZombie.setThumpFlag(1);
                } else {
                    SoundManager.instance.PlayWorldSound("VehicleRunOverBody", chr.getCurrentSquare(), 0.0f, 20.0f, 0.9f, true);
                }
                doSound = false;
            }
            if (i >= this.impulsesFromSquishedBodies.length) continue;
            if (this.impulsesFromSquishedBodies[i] == null) {
                this.impulsesFromSquishedBodies[i] = new VehicleImpulse();
            }
            this.impulsesFromSquishedBodies[i].impulse.set(0.0f, 1.0f, 0.0f);
            float speedMult = Math.max(currentAbsoluteSpeedKmHour, 10.0f) / 10.0f;
            float corpseSizeMul = 1.0f;
            if (chr instanceof IsoDeadBody && chr.getModData() != null && chr.getModData().rawget("corpseSize") != null) {
                corpseSizeMul = ((KahluaTableImpl)chr.getModData()).rawgetFloat("corpseSize");
            }
            this.impulsesFromSquishedBodies[i].impulse.mul(0.065f * this.getFudgedMass() * speedMult * corpseSizeMul);
            this.impulsesFromSquishedBodies[i].relPos.set(wheelPos.x - this.getX(), 0.0f, wheelPos.y - this.getY());
            this.impulsesFromSquishedBodies[i].enable = true;
            this.impulsesFromSquishedBodies[i].applied = false;
            ++numWheelsHit;
            if (outImpactPosOnVehicle == null) continue;
            outImpactPosOnVehicle.set(wheelPos.x, wheelPos.y);
        }
        TL_vector3f_pool.get().release(worldPos);
        TL_vector3f_pool.get().release(wheelPos);
        return numWheelsHit;
    }

    public Vector2 testCollisionWithObject(IsoObject obj, float circleRadius, Vector2 out) {
        if (this.physics == null) {
            return null;
        }
        if (obj.square == null) {
            return null;
        }
        float objX = this.getObjectX(obj);
        float objY = this.getObjectY(obj);
        Vector3f ext = this.script.getExtents();
        Vector3f com = this.script.getCenterOfMassOffset();
        float selfRadius = Math.max(ext.x / 2.0f, ext.z / 2.0f) + circleRadius + 1.0f;
        if (this.DistToSquared(objX, objY) > selfRadius * selfRadius) {
            return null;
        }
        Vector3f circle = (Vector3f)TL_vector3f_pool.get().alloc();
        this.getLocalPos(objX, objY, 0.0f, circle);
        float xMin = com.x - ext.x / 2.0f;
        float xMax = com.x + ext.x / 2.0f;
        float yMin = com.z - ext.z / 2.0f;
        float yMax = com.z + ext.z / 2.0f;
        if (circle.x > xMin && circle.x < xMax && circle.z > yMin && circle.z < yMax) {
            float dw = circle.x - xMin;
            float de = xMax - circle.x;
            float dn = circle.z - yMin;
            float ds = yMax - circle.z;
            Vector3f v = (Vector3f)TL_vector3f_pool.get().alloc();
            if (dw < de && dw < dn && dw < ds) {
                v.set(xMin - circleRadius - 0.015f, 0.0f, circle.z);
            } else if (de < dw && de < dn && de < ds) {
                v.set(xMax + circleRadius + 0.015f, 0.0f, circle.z);
            } else if (dn < dw && dn < de && dn < ds) {
                v.set(circle.x, 0.0f, yMin - circleRadius - 0.015f);
            } else if (ds < dw && ds < de && ds < dn) {
                v.set(circle.x, 0.0f, yMax + circleRadius + 0.015f);
            }
            TL_vector3f_pool.get().release(circle);
            Transform xfrm = this.getWorldTransform(BaseVehicle.allocTransform());
            xfrm.origin.set(0.0f, 0.0f, 0.0f);
            xfrm.transform(v);
            BaseVehicle.releaseTransform(xfrm);
            v.x += this.getX();
            v.z += this.getY();
            this.collideX = v.x;
            this.collideY = v.z;
            out.set(v.x, v.z);
            TL_vector3f_pool.get().release(v);
            return out;
        }
        float closestX = this.clamp(circle.x, xMin, xMax);
        float closestY = this.clamp(circle.z, yMin, yMax);
        float distanceX = circle.x - closestX;
        float distanceY = circle.z - closestY;
        TL_vector3f_pool.get().release(circle);
        float distanceSquared = distanceX * distanceX + distanceY * distanceY;
        if (distanceSquared < circleRadius * circleRadius) {
            if (distanceX == 0.0f && distanceY == 0.0f) {
                return out.set(-1.0f, -1.0f);
            }
            Vector3f v = (Vector3f)TL_vector3f_pool.get().alloc();
            v.set(distanceX, 0.0f, distanceY);
            v.normalize();
            v.mul(circleRadius + 0.015f);
            v.x += closestX;
            v.z += closestY;
            Transform xfrm = this.getWorldTransform(BaseVehicle.allocTransform());
            xfrm.origin.set(0.0f, 0.0f, 0.0f);
            xfrm.transform(v);
            BaseVehicle.releaseTransform(xfrm);
            v.x += this.getX();
            v.z += this.getY();
            this.collideX = v.x;
            this.collideY = v.z;
            out.set(v.x, v.z);
            TL_vector3f_pool.get().release(v);
            return out;
        }
        return null;
    }

    public boolean testCollisionWithVehicle(BaseVehicle obj) {
        VehicleScript objScript;
        VehicleScript thisScript = this.script;
        if (thisScript == null) {
            thisScript = ScriptManager.instance.getVehicle(this.scriptName);
        }
        if ((objScript = obj.script) == null) {
            objScript = ScriptManager.instance.getVehicle(obj.scriptName);
        }
        if (thisScript == null || objScript == null) {
            return false;
        }
        Vector2[] testVecs1 = L_testCollisionWithVehicle.testVecs1;
        Vector2[] testVecs2 = L_testCollisionWithVehicle.testVecs2;
        if (testVecs1[0] == null) {
            for (int i = 0; i < testVecs1.length; ++i) {
                testVecs1[i] = new Vector2();
                testVecs2[i] = new Vector2();
            }
        }
        Vector3f extThis = thisScript.getExtents();
        Vector3f comThis = thisScript.getCenterOfMassOffset();
        Vector3f extOther = objScript.getExtents();
        Vector3f comOther = objScript.getCenterOfMassOffset();
        Vector3f worldPos = L_testCollisionWithVehicle.worldPos;
        float scale = 0.5f;
        this.getWorldPos(comThis.x + extThis.x * 0.5f, 0.0f, comThis.z + extThis.z * 0.5f, worldPos, thisScript);
        testVecs1[0].set(worldPos.x, worldPos.y);
        this.getWorldPos(comThis.x - extThis.x * 0.5f, 0.0f, comThis.z + extThis.z * 0.5f, worldPos, thisScript);
        testVecs1[1].set(worldPos.x, worldPos.y);
        this.getWorldPos(comThis.x - extThis.x * 0.5f, 0.0f, comThis.z - extThis.z * 0.5f, worldPos, thisScript);
        testVecs1[2].set(worldPos.x, worldPos.y);
        this.getWorldPos(comThis.x + extThis.x * 0.5f, 0.0f, comThis.z - extThis.z * 0.5f, worldPos, thisScript);
        testVecs1[3].set(worldPos.x, worldPos.y);
        obj.getWorldPos(comOther.x + extOther.x * 0.5f, 0.0f, comOther.z + extOther.z * 0.5f, worldPos, objScript);
        testVecs2[0].set(worldPos.x, worldPos.y);
        obj.getWorldPos(comOther.x - extOther.x * 0.5f, 0.0f, comOther.z + extOther.z * 0.5f, worldPos, objScript);
        testVecs2[1].set(worldPos.x, worldPos.y);
        obj.getWorldPos(comOther.x - extOther.x * 0.5f, 0.0f, comOther.z - extOther.z * 0.5f, worldPos, objScript);
        testVecs2[2].set(worldPos.x, worldPos.y);
        obj.getWorldPos(comOther.x + extOther.x * 0.5f, 0.0f, comOther.z - extOther.z * 0.5f, worldPos, objScript);
        testVecs2[3].set(worldPos.x, worldPos.y);
        return QuadranglesIntersection.IsQuadranglesAreIntersected(testVecs1, testVecs2);
    }

    private float getObjectX(IsoObject obj) {
        if (obj instanceof IsoMovingObject) {
            return obj.getX();
        }
        return (float)obj.getSquare().getX() + 0.5f;
    }

    private float getObjectY(IsoObject obj) {
        if (obj instanceof IsoMovingObject) {
            return obj.getY();
        }
        return (float)obj.getSquare().getY() + 0.5f;
    }

    public void applyImpulseFromHitObject(IsoObject obj, float mul) {
        float objX = this.getObjectX(obj);
        float objY = this.getObjectY(obj);
        VehicleImpulse impulse = VehicleImpulse.alloc();
        impulse.impulse.set(this.getX() - objX, 0.0f, this.getY() - objY);
        impulse.impulse.normalize();
        impulse.impulse.mul(mul);
        impulse.relPos.set(objX - this.getX(), 0.0f, objY - this.getY());
        this.impulsesFromHitObjects.add(impulse);
        this.setPhysicsActive(true);
    }

    public void applyImpulseFromHitPedestrian(IsoGameCharacter chr) {
        Vector3f velocityNormal = this.getLinearVelocity((Vector3f)TL_vector3f_pool.get().alloc());
        velocityNormal.y = 0.0f;
        float vehicleSpeed = velocityNormal.length();
        if (vehicleSpeed < 1.0E-5f) {
            return;
        }
        velocityNormal.mul(1.0f / vehicleSpeed);
        float objX = chr.getX();
        float objY = chr.getY();
        float x = this.getX();
        float y = this.getY();
        float posToCharX = objX - x;
        float posToCharY = objY - y;
        float posToCharLenSq = posToCharX * posToCharX + posToCharY * posToCharY;
        if (PZMath.abs(posToCharLenSq) < 1.0E-5f) {
            return;
        }
        float posToCharLen = PZMath.sqrt(posToCharLenSq);
        float impulseX = -posToCharX / posToCharLen;
        float impulseY = -posToCharY / posToCharLen;
        float dot = velocityNormal.dot(impulseX, 0.0f, impulseY);
        TL_vector3f_pool.get().release(velocityNormal);
        if (dot > 0.0f) {
            return;
        }
        float vehicleMass = this.getFudgedMass();
        float characterMass = chr.getMass();
        float characterImpactScalar = chr.isProne() ? 0.2f : 0.8f;
        float impulseStrength = -dot * characterMass * characterImpactScalar * vehicleSpeed;
        VehicleImpulse impulse = VehicleImpulse.alloc();
        impulse.impulse.set(impulseX, 0.0f, impulseY);
        impulse.impulse.mul(impulseStrength);
        impulse.relPos.set(posToCharX, 0.0f, posToCharY);
        this.impulsesFromHitObjects.add(impulse);
    }

    public void applyImpulseFromHitPlant(IsoObject obj, float mul) {
        float objX = this.getObjectX(obj);
        float objY = this.getObjectY(obj);
        VehicleImpulse impulse = VehicleImpulse.alloc();
        this.getLinearVelocity(impulse.impulse);
        impulse.impulse.mul(-mul * this.getFudgedMass());
        impulse.relPos.set(objX - this.getX(), 0.0f, objY - this.getY());
        this.impulsesFromHitObjects.add(impulse);
        this.setPhysicsActive(true);
    }

    public void applyImpulseGeneric(float fromX, float fromY, float fromZ, float impulseDirX, float impulseDirY, float impulseDirZ, float impulseStrength) {
        VehicleImpulse impulse = VehicleImpulse.alloc();
        impulse.impulse.set(impulseDirX, impulseDirZ, impulseDirY);
        impulse.impulse.normalize();
        impulse.impulse.mul(impulseStrength);
        impulse.relPos.set(fromX - this.getX(), fromZ - this.getZ(), fromY - this.getY());
        this.impulsesFromHitObjects.add(impulse);
        this.setPhysicsActive(true);
        DebugType.General.println("applyImpulseGeneric! from(%f, %f, %f). relPos(%f, %f, %f). isActive: %s, isStatic: %s", Float.valueOf(fromX), Float.valueOf(fromY), Float.valueOf(fromZ), Float.valueOf(impulse.relPos.x), Float.valueOf(impulse.relPos.y), Float.valueOf(impulse.relPos.z), this.isActive ? "true" : "false", this.isStatic ? "true" : "false");
    }

    public float hitCharacter(IsoGameCharacter chr, Vector2 impactPosOnVehicle, boolean pushedBack) {
        if (!chr.canBeHitByVehicle(this)) {
            return 0.0f;
        }
        if (Math.abs(chr.getX() - this.getX()) < 0.01f || Math.abs(chr.getY() - this.getY()) < 0.01f) {
            return 0.0f;
        }
        float speedCap = 15.0f;
        Vector3f velocity = this.getLinearVelocity((Vector3f)TL_vector3f_pool.get().alloc());
        velocity.y = 0.0f;
        float speed = velocity.length();
        speed = Math.min(speed, 15.0f);
        TL_vector3f_pool.get().release(velocity);
        if (speed < 0.05f) {
            return 0.0f;
        }
        Vector3f impulse = (Vector3f)TL_vector3f_pool.get().alloc();
        impulse.set(this.getX() - chr.getX(), 0.0f, this.getY() - chr.getY());
        impulse.normalize();
        if (chr.canSlowDownVehicleWhenHit(this)) {
            this.applyImpulseFromHitPedestrian(chr);
        }
        impulse.normalize();
        impulse.mul(3.0f * speed / 15.0f);
        Vector2 vector2 = (Vector2)Vector2ObjectPool.get().alloc();
        float hitSpeed = speed + this.physics.clientForce / this.getFudgedMass();
        chr.setVehicleHitLocation(this);
        this.playSoundVehicleHitCharracter(chr);
        float damageCausedToPedestrian = chr.onHitByVehicle(this, hitSpeed, vector2.set(-impulse.x, -impulse.z), impactPosOnVehicle, pushedBack);
        this.triggerMusicIntensityEventVehicleHitCharacter();
        Vector2ObjectPool.get().release(vector2);
        TL_vector3f_pool.get().release(impulse);
        this.onHitCharacterIncrementHitCounts(chr);
        return damageCausedToPedestrian;
    }

    private void triggerMusicIntensityEventVehicleHitCharacter() {
        if (GameServer.server) {
            return;
        }
        IsoPlayer driver = Type.tryCastTo(this.getDriverRegardlessOfTow(), IsoPlayer.class);
        if (driver != null && driver.isLocalPlayer()) {
            driver.triggerMusicIntensityEvent("VehicleHitCharacter");
        }
    }

    private void playSoundVehicleHitCharracter(IsoGameCharacter chr) {
        if (GameServer.server) {
            return;
        }
        BaseSoundEmitter emitter = IsoWorld.instance.getFreeEmitter(chr.getX(), chr.getY(), chr.getZ());
        long soundRef = emitter.playSound("VehicleHitCharacter");
        emitter.setParameterValue(soundRef, FMODManager.instance.getParameterDescription("VehicleHitLocation"), ParameterVehicleHitLocation.calculateLocation(this, chr.getX(), chr.getY(), chr.getZ()).getValue());
        emitter.setParameterValue(soundRef, FMODManager.instance.getParameterDescription("VehicleSpeed"), this.getCurrentSpeedKmHour());
    }

    private void onHitCharacterIncrementHitCounts(IsoGameCharacter chr) {
        this.pedestrianContacts.onCharacterHit(chr);
    }

    public boolean isPersistentContact(IsoGameCharacter chr) {
        return this.pedestrianContacts.isPersistentContact(chr);
    }

    private boolean isCharacterInFront(IsoGameCharacter chr) {
        Vector3f pos = (Vector3f)TL_vector3f_pool.get().alloc();
        this.getLocalPos(chr.getX(), chr.getY(), chr.getZ(), pos);
        boolean isInFront = pos.z > 0.0f;
        TL_vector3f_pool.get().release(pos);
        return isInFront;
    }

    public void hitCharacter(IsoAnimal chr) {
        if (chr.getCurrentState() == AnimalFalldownState.instance()) {
            return;
        }
        if (Math.abs(chr.getX() - this.getX()) < 0.01f || Math.abs(chr.getY() - this.getY()) < 0.01f) {
            return;
        }
        float speedCap = 15.0f;
        Vector3f velocity = this.getLinearVelocity((Vector3f)TL_vector3f_pool.get().alloc());
        velocity.y = 0.0f;
        float speed = velocity.length();
        if ((speed = Math.min(speed, 15.0f)) < 0.05f) {
            TL_vector3f_pool.get().release(velocity);
            return;
        }
        Vector3f impulse = (Vector3f)TL_vector3f_pool.get().alloc();
        impulse.set(this.getX() - chr.getX(), 0.0f, this.getY() - chr.getY());
        impulse.normalize();
        velocity.normalize();
        float dot = velocity.dot(impulse);
        TL_vector3f_pool.get().release(velocity);
        if (dot < 0.0f && !GameServer.server) {
            this.applyImpulseFromHitObject(chr, this.getFudgedMass() * 7.0f * speed / 15.0f * Math.abs(dot));
        }
        impulse.normalize();
        impulse.mul(3.0f * speed / 15.0f);
        Vector2 vector2 = (Vector2)Vector2ObjectPool.get().alloc();
        float hitSpeed = speed + this.physics.clientForce / this.getFudgedMass();
        chr.setVehicleHitLocation(this);
        this.playSoundVehicleHitCharracter(chr);
        chr.Hit(this, hitSpeed, dot > 0.0f, vector2.set(-impulse.x, -impulse.z));
        this.triggerMusicIntensityEventVehicleHitCharacter();
        Vector2ObjectPool.get().release(vector2);
        TL_vector3f_pool.get().release(impulse);
        this.onHitCharacterIncrementHitCounts(chr);
    }

    public int calculateDamageWithCharacter(IsoGameCharacter chr) {
        float minSpeedToDamage = 5.0f;
        float speedAtMaxDamage = 160.0f;
        float dmgMultiplier = 60.0f;
        float currentAbsoluteSpeedKmHour = this.getCurrentAbsoluteSpeedKmHour();
        if (currentAbsoluteSpeedKmHour < 5.0f) {
            return 0;
        }
        if (chr instanceof IsoAnimal) {
            IsoAnimal animal = (IsoAnimal)chr;
            dmgMultiplier *= Math.min(animal.getData().getWeight() / 50.0f, 1.5f);
        }
        float dmgAlpha = (currentAbsoluteSpeedKmHour - 5.0f) / 155.0f;
        float dmgAlphaClamped = PZMath.clamp(dmgAlpha, 0.0f, 1.0f);
        float dmg = dmgMultiplier * PZMath.lerpFunc_EaseOutQuad(dmgAlphaClamped);
        return PZMath.roundToInt(dmg);
    }

    public boolean blocked(int x, int y, int z) {
        if (this.removedFromWorld || this.current == null) {
            return false;
        }
        if (this.getController() == null) {
            return false;
        }
        if (z != PZMath.fastfloor(this.getZ())) {
            return false;
        }
        if (IsoUtils.DistanceTo2D((float)x + 0.5f, (float)y + 0.5f, this.getX(), this.getY()) > 5.0f) {
            return false;
        }
        float circleRadius = 0.3f;
        Transform xfrm = BaseVehicle.allocTransform();
        this.getWorldTransform(xfrm);
        xfrm.inverse();
        Vector3f circle = (Vector3f)TL_vector3f_pool.get().alloc();
        circle.set((float)x + 0.5f - WorldSimulation.instance.offsetX, 0.0f, (float)y + 0.5f - WorldSimulation.instance.offsetY);
        xfrm.transform(circle);
        BaseVehicle.releaseTransform(xfrm);
        Vector3f ext = this.script.getExtents();
        Vector3f com = this.script.getCenterOfMassOffset();
        float closestX = this.clamp(circle.x, com.x - ext.x / 2.0f, com.x + ext.x / 2.0f);
        float closestY = this.clamp(circle.z, com.z - ext.z / 2.0f, com.z + ext.z / 2.0f);
        float distanceX = circle.x - closestX;
        float distanceY = circle.z - closestY;
        TL_vector3f_pool.get().release(circle);
        float distanceSquared = distanceX * distanceX + distanceY * distanceY;
        return distanceSquared < 0.09f;
    }

    public boolean isIntersectingSquare(int x, int y, int z) {
        if (z != PZMath.fastfloor(this.getZ())) {
            return false;
        }
        if (this.removedFromWorld || this.current == null || this.getController() == null) {
            return false;
        }
        BaseVehicle.tempPoly.x1 = BaseVehicle.tempPoly.x4 = (float)x;
        BaseVehicle.tempPoly.y1 = BaseVehicle.tempPoly.y2 = (float)y;
        BaseVehicle.tempPoly.x2 = BaseVehicle.tempPoly.x3 = (float)x + 1.0f;
        BaseVehicle.tempPoly.y3 = BaseVehicle.tempPoly.y4 = (float)y + 1.0f;
        return PolyPolyIntersect.intersects(tempPoly, this.getPoly());
    }

    public boolean isIntersectingSquare(IsoGridSquare sq) {
        return this.isIntersectingSquare(sq.getX(), sq.getY(), sq.getZ());
    }

    public boolean isIntersectingSquareWithShadow(int x, int y, int z) {
        if (z != PZMath.fastfloor(this.getZ())) {
            return false;
        }
        if (this.removedFromWorld || this.current == null || this.getController() == null) {
            return false;
        }
        BaseVehicle.tempPoly.x1 = BaseVehicle.tempPoly.x4 = (float)x;
        BaseVehicle.tempPoly.y1 = BaseVehicle.tempPoly.y2 = (float)y;
        BaseVehicle.tempPoly.x2 = BaseVehicle.tempPoly.x3 = (float)x + 1.0f;
        BaseVehicle.tempPoly.y3 = BaseVehicle.tempPoly.y4 = (float)y + 1.0f;
        return PolyPolyIntersect.intersects(tempPoly, this.shadowCoord);
    }

    public boolean circleIntersects(float x, float y, float z, float radius) {
        if (this.getController() == null) {
            return false;
        }
        if (PZMath.fastfloor(z) != PZMath.fastfloor(this.getZ())) {
            return false;
        }
        if (IsoUtils.DistanceTo2D(x, y, this.getX(), this.getY()) > 5.0f) {
            return false;
        }
        Vector3f ext = this.script.getExtents();
        Vector3f com = this.script.getCenterOfMassOffset();
        Vector3f circle = (Vector3f)TL_vector3f_pool.get().alloc();
        this.getLocalPos(x, y, z, circle);
        float xMin = com.x - ext.x / 2.0f;
        float xMax = com.x + ext.x / 2.0f;
        float yMin = com.z - ext.z / 2.0f;
        float yMax = com.z + ext.z / 2.0f;
        if (circle.x > xMin && circle.x < xMax && circle.z > yMin && circle.z < yMax) {
            return true;
        }
        float closestX = this.clamp(circle.x, xMin, xMax);
        float closestY = this.clamp(circle.z, yMin, yMax);
        float distanceX = circle.x - closestX;
        float distanceY = circle.z - closestY;
        TL_vector3f_pool.get().release(circle);
        float distanceSquared = distanceX * distanceX + distanceY * distanceY;
        return distanceSquared < radius * radius;
    }

    public void updateLights() {
        boolean stoplightsOn;
        VehicleModelInstance inst = (VehicleModelInstance)this.sprite.modelSlot.model;
        inst.textureRustA = this.rust;
        if (this.script.getWheelCount() == 0) {
            inst.textureRustA = 0.0f;
        }
        inst.painColor.x = this.colorHue;
        inst.painColor.y = this.colorSaturation;
        inst.painColor.z = this.colorValue;
        boolean windowFront = false;
        boolean windowRear = false;
        boolean windowFrontLeft = false;
        boolean windowMiddleLeft = false;
        boolean windowRearLeft = false;
        boolean windowFrontRight = false;
        boolean windowMiddleRight = false;
        boolean windowRearRight = false;
        if (this.windowLightsOn) {
            VehiclePart part = this.getPartById("Windshield");
            windowFront = part != null && part.getInventoryItem() != null;
            part = this.getPartById("WindshieldRear");
            windowRear = part != null && part.getInventoryItem() != null;
            part = this.getPartById("WindowFrontLeft");
            windowFrontLeft = part != null && part.getInventoryItem() != null;
            part = this.getPartById("WindowMiddleLeft");
            windowMiddleLeft = part != null && part.getInventoryItem() != null;
            part = this.getPartById("WindowRearLeft");
            windowRearLeft = part != null && part.getInventoryItem() != null;
            part = this.getPartById("WindowFrontRight");
            windowFrontRight = part != null && part.getInventoryItem() != null;
            part = this.getPartById("WindowMiddleRight");
            windowMiddleRight = part != null && part.getInventoryItem() != null;
            part = this.getPartById("WindowRearRight");
            windowRearRight = part != null && part.getInventoryItem() != null;
        }
        inst.textureLightsEnables1[10] = windowFront ? 1.0f : 0.0f;
        inst.textureLightsEnables1[14] = windowRear ? 1.0f : 0.0f;
        inst.textureLightsEnables1[2] = windowFrontLeft ? 1.0f : 0.0f;
        inst.textureLightsEnables1[6] = windowMiddleLeft | windowRearLeft ? 1.0f : 0.0f;
        inst.textureLightsEnables1[9] = windowFrontRight ? 1.0f : 0.0f;
        inst.textureLightsEnables1[13] = windowMiddleRight | windowRearRight ? 1.0f : 0.0f;
        boolean headlightLeftSet = false;
        boolean headlightRightSet = false;
        boolean headlightRearRightSet = false;
        boolean headlightRearLeftSet = false;
        if (this.headlightsOn && this.getBatteryCharge() > 0.0f) {
            VehiclePart part = this.getPartById("HeadlightLeft");
            if (part != null && part.getInventoryItem() != null) {
                headlightLeftSet = true;
            }
            if ((part = this.getPartById("HeadlightRight")) != null && part.getInventoryItem() != null) {
                headlightRightSet = true;
            }
            if ((part = this.getPartById("HeadlightRearLeft")) != null && part.getInventoryItem() != null) {
                headlightRearLeftSet = true;
            }
            if ((part = this.getPartById("HeadlightRearRight")) != null && part.getInventoryItem() != null) {
                headlightRearRightSet = true;
            }
        }
        inst.textureLightsEnables2[4] = headlightRightSet ? 1.0f : 0.0f;
        inst.textureLightsEnables2[8] = headlightLeftSet ? 1.0f : 0.0f;
        inst.textureLightsEnables2[12] = headlightRearRightSet ? 1.0f : 0.0f;
        inst.textureLightsEnables2[1] = headlightRearLeftSet ? 1.0f : 0.0f;
        boolean bl = stoplightsOn = this.stoplightsOn && this.getBatteryCharge() > 0.0f;
        if (this.scriptName.contains("Trailer") && this.vehicleTowedBy != null && this.vehicleTowedBy.stoplightsOn && this.vehicleTowedBy.getBatteryCharge() > 0.0f) {
            stoplightsOn = true;
        }
        if (stoplightsOn) {
            inst.textureLightsEnables2[5] = 1.0f;
            inst.textureLightsEnables2[9] = 1.0f;
        } else {
            inst.textureLightsEnables2[5] = 0.0f;
            inst.textureLightsEnables2[9] = 0.0f;
        }
        if (this.script.getLightbar().enable) {
            if (this.lightbarLightsMode.isEnable() && this.getBatteryCharge() > 0.0f) {
                switch (this.lightbarLightsMode.getLightTexIndex()) {
                    case 0: {
                        inst.textureLightsEnables2[13] = 0.0f;
                        inst.textureLightsEnables2[2] = 0.0f;
                        break;
                    }
                    case 1: {
                        inst.textureLightsEnables2[13] = 0.0f;
                        inst.textureLightsEnables2[2] = 1.0f;
                        break;
                    }
                    case 2: {
                        inst.textureLightsEnables2[13] = 1.0f;
                        inst.textureLightsEnables2[2] = 0.0f;
                        break;
                    }
                    default: {
                        inst.textureLightsEnables2[13] = 0.0f;
                        inst.textureLightsEnables2[2] = 0.0f;
                        break;
                    }
                }
            } else {
                inst.textureLightsEnables2[13] = 0.0f;
                inst.textureLightsEnables2[2] = 0.0f;
            }
        }
        if (DebugOptions.instance.vehicleCycleColor.getValue()) {
            float c = System.currentTimeMillis() % 2000L;
            float c2 = System.currentTimeMillis() % 7000L;
            float c3 = System.currentTimeMillis() % 11000L;
            inst.painColor.x = c / 2000.0f;
            inst.painColor.y = c2 / 7000.0f;
            inst.painColor.z = c3 / 11000.0f;
        }
        if (DebugOptions.instance.vehicleRenderBlood0.getValue()) {
            Arrays.fill(inst.matrixBlood1Enables1, 0.0f);
            Arrays.fill(inst.matrixBlood1Enables2, 0.0f);
            Arrays.fill(inst.matrixBlood2Enables1, 0.0f);
            Arrays.fill(inst.matrixBlood2Enables2, 0.0f);
        }
        if (DebugOptions.instance.vehicleRenderBlood50.getValue()) {
            Arrays.fill(inst.matrixBlood1Enables1, 0.5f);
            Arrays.fill(inst.matrixBlood1Enables2, 0.5f);
            Arrays.fill(inst.matrixBlood2Enables1, 1.0f);
            Arrays.fill(inst.matrixBlood2Enables2, 1.0f);
        }
        if (DebugOptions.instance.vehicleRenderBlood100.getValue()) {
            Arrays.fill(inst.matrixBlood1Enables1, 1.0f);
            Arrays.fill(inst.matrixBlood1Enables2, 1.0f);
            Arrays.fill(inst.matrixBlood2Enables1, 1.0f);
            Arrays.fill(inst.matrixBlood2Enables2, 1.0f);
        }
        if (DebugOptions.instance.vehicleRenderDamage0.getValue()) {
            Arrays.fill(inst.textureDamage1Enables1, 0.0f);
            Arrays.fill(inst.textureDamage1Enables2, 0.0f);
            Arrays.fill(inst.textureDamage2Enables1, 0.0f);
            Arrays.fill(inst.textureDamage2Enables2, 0.0f);
        }
        if (DebugOptions.instance.vehicleRenderDamage1.getValue()) {
            Arrays.fill(inst.textureDamage1Enables1, 1.0f);
            Arrays.fill(inst.textureDamage1Enables2, 1.0f);
            Arrays.fill(inst.textureDamage2Enables1, 0.0f);
            Arrays.fill(inst.textureDamage2Enables2, 0.0f);
        }
        if (DebugOptions.instance.vehicleRenderDamage2.getValue()) {
            Arrays.fill(inst.textureDamage1Enables1, 0.0f);
            Arrays.fill(inst.textureDamage1Enables2, 0.0f);
            Arrays.fill(inst.textureDamage2Enables1, 1.0f);
            Arrays.fill(inst.textureDamage2Enables2, 1.0f);
        }
        if (DebugOptions.instance.vehicleRenderRust0.getValue()) {
            inst.textureRustA = 0.0f;
        }
        if (DebugOptions.instance.vehicleRenderRust50.getValue()) {
            inst.textureRustA = 0.5f;
        }
        if (DebugOptions.instance.vehicleRenderRust100.getValue()) {
            inst.textureRustA = 1.0f;
        }
        inst.refBody = 0.3f;
        inst.refWindows = 0.4f;
        if (this.rust > 0.8f) {
            inst.refBody = 0.1f;
            inst.refWindows = 0.2f;
        }
    }

    private void updateWorldLights() {
        if (!this.script.getLightbar().enable) {
            this.removeWorldLights();
            return;
        }
        if (!this.lightbarLightsMode.isEnable() || this.getBatteryCharge() <= 0.0f) {
            this.removeWorldLights();
            return;
        }
        if (this.lightbarLightsMode.getLightTexIndex() == 0) {
            this.removeWorldLights();
            return;
        }
        this.rightLight2.radius = 8;
        this.rightLight1.radius = 8;
        this.leftLight2.radius = 8;
        this.leftLight1.radius = 8;
        if (this.lightbarLightsMode.getLightTexIndex() == 1) {
            IsoLightSource light;
            Vector3f pos = this.getWorldPos(0.4f, 0.0f, 0.0f, (Vector3f)TL_vector3f_pool.get().alloc());
            int lx = PZMath.fastfloor(pos.x);
            int ly = PZMath.fastfloor(pos.y);
            int lz = PZMath.fastfloor(this.getZ());
            TL_vector3f_pool.get().release(pos);
            int oldLeftLightIndex = this.leftLightIndex;
            if (oldLeftLightIndex == 1 && this.leftLight1.x == lx && this.leftLight1.y == ly && this.leftLight1.z == lz) {
                return;
            }
            if (oldLeftLightIndex == 2 && this.leftLight2.x == lx && this.leftLight2.y == ly && this.leftLight2.z == lz) {
                return;
            }
            this.removeWorldLights();
            if (oldLeftLightIndex == 1) {
                light = this.leftLight2;
                this.leftLightIndex = 2;
            } else {
                light = this.leftLight1;
                this.leftLightIndex = 1;
            }
            light.life = -1;
            light.x = lx;
            light.y = ly;
            light.z = lz;
            IsoWorld.instance.currentCell.addLamppost(light);
        } else {
            IsoLightSource light;
            Vector3f pos = this.getWorldPos(-0.4f, 0.0f, 0.0f, (Vector3f)TL_vector3f_pool.get().alloc());
            int lx = PZMath.fastfloor(pos.x);
            int ly = PZMath.fastfloor(pos.y);
            int lz = PZMath.fastfloor(this.getZ());
            TL_vector3f_pool.get().release(pos);
            int oldRightLightIndex = this.rightLightIndex;
            if (oldRightLightIndex == 1 && this.rightLight1.x == lx && this.rightLight1.y == ly && this.rightLight1.z == lz) {
                return;
            }
            if (oldRightLightIndex == 2 && this.rightLight2.x == lx && this.rightLight2.y == ly && this.rightLight2.z == lz) {
                return;
            }
            this.removeWorldLights();
            if (oldRightLightIndex == 1) {
                light = this.rightLight2;
                this.rightLightIndex = 2;
            } else {
                light = this.rightLight1;
                this.rightLightIndex = 1;
            }
            light.life = -1;
            light.x = lx;
            light.y = ly;
            light.z = lz;
            IsoWorld.instance.currentCell.addLamppost(light);
        }
    }

    public void fixLightbarModelLighting(IsoLightSource ls, Vector3f lightPos) {
        if (ls == this.leftLight1 || ls == this.leftLight2) {
            lightPos.set(1.0f, 0.0f, 0.0f);
        } else if (ls == this.rightLight1 || ls == this.rightLight2) {
            lightPos.set(-1.0f, 0.0f, 0.0f);
        }
    }

    private void removeWorldLights() {
        if (this.leftLightIndex == 1) {
            IsoWorld.instance.currentCell.removeLamppost(this.leftLight1);
            this.leftLightIndex = -1;
        }
        if (this.leftLightIndex == 2) {
            IsoWorld.instance.currentCell.removeLamppost(this.leftLight2);
            this.leftLightIndex = -1;
        }
        if (this.rightLightIndex == 1) {
            IsoWorld.instance.currentCell.removeLamppost(this.rightLight1);
            this.rightLightIndex = -1;
        }
        if (this.rightLightIndex == 2) {
            IsoWorld.instance.currentCell.removeLamppost(this.rightLight2);
            this.rightLightIndex = -1;
        }
    }

    public void doDamageOverlay() {
        if (this.sprite.modelSlot == null) {
            return;
        }
        this.doDoorDamage();
        this.doWindowDamage();
        this.doOtherBodyWorkDamage();
        this.doBloodOverlay();
    }

    private void checkDamage(VehiclePart part, int matrixName, boolean doBlack) {
        if (doBlack && part != null && part.getId().startsWith("Window") && part.getScriptModelById("Default") != null) {
            doBlack = false;
        }
        VehicleModelInstance inst = (VehicleModelInstance)this.sprite.modelSlot.model;
        try {
            inst.textureDamage1Enables1[matrixName] = 0.0f;
            inst.textureDamage2Enables1[matrixName] = 0.0f;
            inst.textureUninstall1[matrixName] = 0.0f;
            if (part != null && part.getInventoryItem() != null) {
                if (((InventoryItem)part.getInventoryItem()).getCondition() < 60 && ((InventoryItem)part.getInventoryItem()).getCondition() >= 40) {
                    inst.textureDamage1Enables1[matrixName] = 1.0f;
                }
                if (((InventoryItem)part.getInventoryItem()).getCondition() < 40) {
                    inst.textureDamage2Enables1[matrixName] = 1.0f;
                }
                if (part.window != null && part.window.isOpen() && doBlack) {
                    inst.textureUninstall1[matrixName] = 1.0f;
                }
            } else if (part != null && doBlack) {
                inst.textureUninstall1[matrixName] = 1.0f;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkDamage2(VehiclePart part, int matrixName, boolean doBlack) {
        VehicleModelInstance inst = (VehicleModelInstance)this.sprite.modelSlot.model;
        try {
            inst.textureDamage1Enables2[matrixName] = 0.0f;
            inst.textureDamage2Enables2[matrixName] = 0.0f;
            inst.textureUninstall2[matrixName] = 0.0f;
            if (part != null && part.getInventoryItem() != null) {
                if (((InventoryItem)part.getInventoryItem()).getCondition() < 60 && ((InventoryItem)part.getInventoryItem()).getCondition() >= 40) {
                    inst.textureDamage1Enables2[matrixName] = 1.0f;
                }
                if (((InventoryItem)part.getInventoryItem()).getCondition() < 40) {
                    inst.textureDamage2Enables2[matrixName] = 1.0f;
                }
                if (part.window != null && part.window.isOpen() && doBlack) {
                    inst.textureUninstall2[matrixName] = 1.0f;
                }
            } else if (part != null && doBlack) {
                inst.textureUninstall2[matrixName] = 1.0f;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkUninstall2(VehiclePart part, int matrixName) {
        VehicleModelInstance inst = (VehicleModelInstance)this.sprite.modelSlot.model;
        try {
            inst.textureUninstall2[matrixName] = 0.0f;
            if (part != null && part.getInventoryItem() == null) {
                inst.textureUninstall2[matrixName] = 1.0f;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doOtherBodyWorkDamage() {
        this.checkDamage(this.getPartById("EngineDoor"), 0, false);
        this.checkDamage(this.getPartById("EngineDoor"), 3, false);
        this.checkDamage(this.getPartById("EngineDoor"), 11, false);
        this.checkDamage2(this.getPartById("EngineDoor"), 6, true);
        this.checkDamage(this.getPartById("TruckBed"), 4, false);
        this.checkDamage(this.getPartById("TruckBed"), 7, false);
        this.checkDamage(this.getPartById("TruckBed"), 15, false);
        VehiclePart backDoor = this.getPartById("TrunkDoor");
        if (backDoor != null) {
            this.checkDamage2(backDoor, 10, true);
            if (backDoor.scriptPart.hasLightsRear) {
                this.checkUninstall2(backDoor, 12);
                this.checkUninstall2(backDoor, 1);
                this.checkUninstall2(backDoor, 5);
                this.checkUninstall2(backDoor, 9);
            }
        } else {
            backDoor = this.getPartById("DoorRear");
            if (backDoor != null) {
                this.checkDamage2(backDoor, 10, true);
                if (backDoor.scriptPart.hasLightsRear) {
                    this.checkUninstall2(backDoor, 12);
                    this.checkUninstall2(backDoor, 1);
                    this.checkUninstall2(backDoor, 5);
                    this.checkUninstall2(backDoor, 9);
                }
            }
        }
    }

    private void doWindowDamage() {
        this.checkDamage(this.getPartById("WindowFrontLeft"), 2, true);
        this.checkDamage(this.getPartById("WindowFrontRight"), 9, true);
        VehiclePart backDoor = this.getPartById("WindowRearLeft");
        if (backDoor != null) {
            this.checkDamage(backDoor, 6, true);
        } else {
            backDoor = this.getPartById("WindowMiddleLeft");
            if (backDoor != null) {
                this.checkDamage(backDoor, 6, true);
            }
        }
        backDoor = this.getPartById("WindowRearRight");
        if (backDoor != null) {
            this.checkDamage(backDoor, 13, true);
        } else {
            backDoor = this.getPartById("WindowMiddleRight");
            if (backDoor != null) {
                this.checkDamage(backDoor, 13, true);
            }
        }
        this.checkDamage(this.getPartById("Windshield"), 10, true);
        this.checkDamage(this.getPartById("WindshieldRear"), 14, true);
    }

    private void doDoorDamage() {
        this.checkDamage(this.getPartById("DoorFrontLeft"), 1, true);
        this.checkDamage(this.getPartById("DoorFrontRight"), 8, true);
        VehiclePart backDoor = this.getPartById("DoorRearLeft");
        if (backDoor != null) {
            this.checkDamage(backDoor, 5, true);
        } else {
            backDoor = this.getPartById("DoorMiddleLeft");
            if (backDoor != null) {
                this.checkDamage(backDoor, 5, true);
            }
        }
        backDoor = this.getPartById("DoorRearRight");
        if (backDoor != null) {
            this.checkDamage(backDoor, 12, true);
        } else {
            backDoor = this.getPartById("DoorMiddleRight");
            if (backDoor != null) {
                this.checkDamage(backDoor, 12, true);
            }
        }
    }

    public float getBloodIntensity(String id) {
        return (float)(this.bloodIntensity.getOrDefault(id, BYTE_ZERO) & 0xFF) / 100.0f;
    }

    public void setBloodIntensity(String id, float intensity) {
        byte intensity2 = (byte)(PZMath.clamp(intensity, 0.0f, 1.0f) * 100.0f);
        if (this.bloodIntensity.containsKey(id) && intensity2 == this.bloodIntensity.get(id)) {
            return;
        }
        this.bloodIntensity.put(id, intensity2);
        this.doBloodOverlay();
        this.transmitBlood();
    }

    public void transmitBlood() {
        if (!GameServer.server) {
            return;
        }
        this.updateFlags = (short)(this.updateFlags | 0x1000);
    }

    public void doBloodOverlay() {
        if (this.sprite.modelSlot == null) {
            return;
        }
        VehicleModelInstance inst = (VehicleModelInstance)this.sprite.modelSlot.model;
        Arrays.fill(inst.matrixBlood1Enables1, 0.0f);
        Arrays.fill(inst.matrixBlood1Enables2, 0.0f);
        Arrays.fill(inst.matrixBlood2Enables1, 0.0f);
        Arrays.fill(inst.matrixBlood2Enables2, 0.0f);
        if (Core.getInstance().getOptionBloodDecals() == 0) {
            return;
        }
        this.doBloodOverlayFront(inst.matrixBlood1Enables1, inst.matrixBlood1Enables2, this.getBloodIntensity("Front"));
        this.doBloodOverlayRear(inst.matrixBlood1Enables1, inst.matrixBlood1Enables2, this.getBloodIntensity("Rear"));
        this.doBloodOverlayLeft(inst.matrixBlood1Enables1, inst.matrixBlood1Enables2, this.getBloodIntensity("Left"));
        this.doBloodOverlayRight(inst.matrixBlood1Enables1, inst.matrixBlood1Enables2, this.getBloodIntensity("Right"));
        for (Map.Entry<String, Byte> entry : this.bloodIntensity.entrySet()) {
            Integer mask = s_PartToMaskMap.get(entry.getKey());
            if (mask == null) continue;
            inst.matrixBlood1Enables1[mask.intValue()] = (float)(entry.getValue() & 0xFF) / 100.0f;
        }
        this.doBloodOverlayAux(inst.matrixBlood2Enables1, inst.matrixBlood2Enables2, 1.0f);
    }

    private void doBloodOverlayAux(float[] matrix1, float[] matrix2, float intensity) {
        matrix1[0] = intensity;
        matrix2[6] = intensity;
        matrix2[4] = intensity;
        matrix2[8] = intensity;
        matrix1[4] = intensity;
        matrix1[7] = intensity;
        matrix1[15] = intensity;
        matrix2[10] = intensity;
        matrix2[12] = intensity;
        matrix2[1] = intensity;
        matrix2[5] = intensity;
        matrix2[9] = intensity;
        matrix1[3] = intensity;
        matrix1[8] = intensity;
        matrix1[12] = intensity;
        matrix1[11] = intensity;
        matrix1[1] = intensity;
        matrix1[5] = intensity;
        matrix2[0] = intensity;
        matrix1[10] = intensity;
        matrix1[14] = intensity;
        matrix1[9] = intensity;
        matrix1[13] = intensity;
        matrix1[2] = intensity;
        matrix1[6] = intensity;
    }

    private void doBloodOverlayFront(float[] matrix1, float[] matrix2, float intensity) {
        matrix1[0] = intensity;
        matrix2[6] = intensity;
        matrix2[4] = intensity;
        matrix2[8] = intensity;
        matrix1[10] = intensity;
    }

    private void doBloodOverlayRear(float[] matrix1, float[] matrix2, float intensity) {
        matrix1[4] = intensity;
        matrix2[10] = intensity;
        matrix2[12] = intensity;
        matrix2[1] = intensity;
        matrix2[5] = intensity;
        matrix2[9] = intensity;
        matrix1[14] = intensity;
    }

    private void doBloodOverlayLeft(float[] matrix1, float[] matrix2, float intensity) {
        matrix1[11] = intensity;
        matrix1[1] = intensity;
        matrix1[5] = intensity;
        matrix1[15] = intensity;
        matrix1[2] = intensity;
        matrix1[6] = intensity;
    }

    private void doBloodOverlayRight(float[] matrix1, float[] matrix2, float intensity) {
        matrix1[3] = intensity;
        matrix1[8] = intensity;
        matrix1[12] = intensity;
        matrix1[7] = intensity;
        matrix1[9] = intensity;
        matrix1[13] = intensity;
    }

    @Override
    public boolean isOnScreen() {
        if (super.isOnScreen()) {
            return true;
        }
        if (this.physics == null) {
            return false;
        }
        if (this.script == null) {
            return false;
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        if (this.polyDirty) {
            this.getPoly();
        }
        float x1 = IsoUtils.XToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0f, 0);
        float y1 = IsoUtils.YToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0f, 0);
        float x2 = IsoUtils.XToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0f, 0);
        float y2 = IsoUtils.YToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0f, 0);
        float x3 = IsoUtils.XToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0f, 0);
        float y3 = IsoUtils.YToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0f, 0);
        float x4 = IsoUtils.XToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0f, 0);
        float y4 = IsoUtils.YToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0f, 0);
        float dz = (this.script.getCenterOfMassOffset().y + this.script.getExtents().y) / 0.8164967f * 24.0f * (float)Core.tileScale;
        float zoom = Core.getInstance().getZoom(playerIndex);
        float minX = PZMath.min(x1, x2, x3, x4) / zoom;
        float maxX = PZMath.max(x1, x2, x3, x4) / zoom;
        float minY = PZMath.min(y1, y2, y3, y4) / zoom;
        float maxY = PZMath.max(y1, y2, y3, y4) / zoom;
        if (minX < (float)(IsoCamera.getScreenLeft(playerIndex) + IsoCamera.getScreenWidth(playerIndex)) && maxX > (float)IsoCamera.getScreenLeft(playerIndex) && minY < (float)(IsoCamera.getScreenTop(playerIndex) + IsoCamera.getScreenHeight(playerIndex)) && maxY > (float)IsoCamera.getScreenTop(playerIndex)) {
            return true;
        }
        minX = PZMath.min(x1, x2, x3, x4) / zoom;
        maxX = PZMath.max(x1, x2, x3, x4) / zoom;
        minY = PZMath.min(y1 -= dz, y2 -= dz, y3 -= dz, y4 -= dz) / zoom;
        maxY = PZMath.max(y1, y2, y3, y4) / zoom;
        return minX < (float)(IsoCamera.getScreenLeft(playerIndex) + IsoCamera.getScreenWidth(playerIndex)) && maxX > (float)IsoCamera.getScreenLeft(playerIndex) && minY < (float)(IsoCamera.getScreenTop(playerIndex) + IsoCamera.getScreenHeight(playerIndex)) && maxY > (float)IsoCamera.getScreenTop(playerIndex);
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        boolean bForceSeen;
        if (this.script == null) {
            return;
        }
        if (this.physics != null) {
            this.physics.debug();
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        boolean bl = bForceSeen = isoGameCharacter != null && isoGameCharacter.getVehicle() == this;
        if (isoGameCharacter != null && isoGameCharacter.getVehicle() != null && isoGameCharacter.getVehicle().getVehicleTowing() == this) {
            bForceSeen = true;
        }
        if (!bForceSeen && !this.square.lighting[playerIndex].bSeen()) {
            return;
        }
        if (bForceSeen || this.couldSeeIntersectedSquare(playerIndex)) {
            this.setTargetAlpha(playerIndex, 1.0f);
        } else {
            this.setTargetAlpha(playerIndex, 0.0f);
        }
        if (this.sprite.hasActiveModel()) {
            boolean showBloodDecals;
            this.updateLights();
            boolean bl2 = showBloodDecals = Core.getInstance().getOptionBloodDecals() != 0;
            if (this.optionBloodDecals != showBloodDecals) {
                this.optionBloodDecals = showBloodDecals;
                this.doBloodOverlay();
            }
            if (col == null) {
                inf.set(1.0f, 1.0f, 1.0f, 1.0f);
            } else {
                BaseVehicle.inf.a = col.a = this.getAlpha(playerIndex);
                BaseVehicle.inf.r = col.r;
                BaseVehicle.inf.g = col.g;
                BaseVehicle.inf.b = col.b;
            }
            this.sprite.renderVehicle(this.def, this, x, y, 0.0f, 0.0f, 0.0f, inf, true);
        }
        this.updateAlpha(playerIndex);
        if (Core.debug && DebugOptions.instance.vehicleRenderArea.getValue()) {
            this.renderAreas();
        }
        if (Core.debug && DebugOptions.instance.vehicleRenderAttackPositions.getValue()) {
            this.surroundVehicle.render();
        }
        if (Core.debug && DebugOptions.instance.vehicleRenderExit.getValue()) {
            this.renderExits();
        }
        if (Core.debug && DebugOptions.instance.vehicleRenderIntersectedSquares.getValue()) {
            this.renderIntersectedSquares();
        }
        if (Core.debug && DebugOptions.instance.vehicleRenderAuthorizations.getValue()) {
            this.renderAuthorizations();
        }
        if (Core.debug && DebugOptions.instance.vehicleRenderInterpolateBuffer.getValue()) {
            this.renderInterpolateBuffer();
        }
        if (DebugOptions.instance.vehicleRenderTrailerPositions.getValue()) {
            this.renderTrailerPositions();
        }
        this.renderUsableArea();
    }

    @Override
    public void renderlast() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        for (int i = 0; i < this.parts.size(); ++i) {
            VehiclePart part = this.parts.get(i);
            if (part.chatElement == null || !part.chatElement.getHasChatToDisplay()) continue;
            if (part.getDeviceData() != null && !part.getDeviceData().getIsTurnedOn()) {
                part.chatElement.clear(playerIndex);
                continue;
            }
            float sx = IsoUtils.XToScreen(this.getX(), this.getY(), this.getZ(), 0);
            float sy = IsoUtils.YToScreen(this.getX(), this.getY(), this.getZ(), 0);
            sx = sx - IsoCamera.getOffX() - this.offsetX;
            sy = sy - IsoCamera.getOffY() - this.offsetY;
            sx += (float)(32 * Core.tileScale);
            sy += (float)(20 * Core.tileScale);
            sx /= Core.getInstance().getZoom(playerIndex);
            sy /= Core.getInstance().getZoom(playerIndex);
            part.chatElement.renderBatched(playerIndex, (int)(sx += IsoCamera.cameras[IsoCamera.frameState.playerIndex].fixJigglyModelsX), (int)(sy += IsoCamera.cameras[IsoCamera.frameState.playerIndex].fixJigglyModelsY));
        }
    }

    public void renderShadow() {
        if (this.physics == null) {
            return;
        }
        if (this.script == null) {
            return;
        }
        if (this.square == null) {
            return;
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        if (!this.square.lighting[playerIndex].bSeen()) {
            return;
        }
        if (this.square.lighting[playerIndex].bCouldSee()) {
            this.setTargetAlpha(playerIndex, 1.0f);
        } else {
            this.setTargetAlpha(playerIndex, 0.0f);
        }
        Texture vehicleShadow = this.getShadowTexture();
        if (vehicleShadow != null && vehicleShadow.isReady() && this.getCurrentSquare() != null) {
            float shadowAlpha = 0.6f * this.getAlpha(playerIndex);
            ColorInfo lightInfo = this.getCurrentSquare().lighting[playerIndex].lightInfo();
            shadowAlpha *= (lightInfo.r + lightInfo.g + lightInfo.b) / 3.0f;
            if (this.polyDirty) {
                this.getPoly();
            }
            if (PerformanceSettings.fboRenderChunk) {
                float shadowZ = PZMath.fastfloor(this.getZ());
                if (this.current != null && this.current.hasSlopedSurface()) {
                    shadowZ = this.current.getApparentZ(this.getX() % 1.0f, this.getY() % 1.0f);
                }
                FBORenderShadows.getInstance().addShadow(this.getX(), this.getY(), shadowZ, this.shadowCoord.x2, this.shadowCoord.y2, this.shadowCoord.x1, this.shadowCoord.y1, this.shadowCoord.x4, this.shadowCoord.y4, this.shadowCoord.x3, this.shadowCoord.y3, 1.0f, 1.0f, 1.0f, 0.8f * shadowAlpha, vehicleShadow, true);
                return;
            }
            SpriteRenderer.instance.renderPoly(vehicleShadow, (int)IsoUtils.XToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0f, 0), (int)IsoUtils.YToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0f, 0), (int)IsoUtils.XToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0f, 0), (int)IsoUtils.YToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0f, 0), (int)IsoUtils.XToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0f, 0), (int)IsoUtils.YToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0f, 0), (int)IsoUtils.XToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0f, 0), (int)IsoUtils.YToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0f, 0), 1.0f, 1.0f, 1.0f, 0.8f * shadowAlpha);
        }
    }

    public boolean isEnterBlocked(IsoGameCharacter chr, int seat) {
        return this.isExitBlocked(chr, seat);
    }

    public boolean isExitBlocked(int seat) {
        VehicleScript.Position posInside = this.getPassengerPosition(seat, "inside");
        VehicleScript.Position posOutside = this.getPassengerPosition(seat, "outside");
        if (posInside == null || posOutside == null) {
            return true;
        }
        Vector3f exitPos = this.getPassengerPositionWorldPos(posOutside, (Vector3f)TL_vector3f_pool.get().alloc());
        if (posOutside.area != null) {
            Vector2 vector2 = (Vector2)Vector2ObjectPool.get().alloc();
            VehicleScript.Area area = this.script.getAreaById(posOutside.area);
            Vector2 areaPos = this.areaPositionWorld4PlayerInteract(area, vector2);
            exitPos.x = areaPos.x;
            exitPos.y = areaPos.y;
            Vector2ObjectPool.get().release(vector2);
        }
        exitPos.z = 0.0f;
        Vector3f seatedPos = this.getPassengerPositionWorldPos(posInside, (Vector3f)TL_vector3f_pool.get().alloc());
        boolean blocked = PolygonalMap2.instance.lineClearCollide(seatedPos.x, seatedPos.y, exitPos.x, exitPos.y, PZMath.fastfloor(this.getZ()), this, false, false);
        TL_vector3f_pool.get().release(exitPos);
        TL_vector3f_pool.get().release(seatedPos);
        return blocked;
    }

    public boolean isExitBlocked(IsoGameCharacter chr, int seat) {
        IsoPlayer isoPlayer;
        IsoGridSquare sq;
        VehicleScript.Position posInside = this.getPassengerPosition(seat, "inside");
        VehicleScript.Position posOutside = this.getPassengerPosition(seat, "outside");
        if (posInside == null || posOutside == null) {
            return true;
        }
        Vector3f exitPos = this.getPassengerPositionWorldPos(posOutside, (Vector3f)TL_vector3f_pool.get().alloc());
        if (posOutside.area != null) {
            Vector2 vector2 = (Vector2)Vector2ObjectPool.get().alloc();
            VehicleScript.Area area = this.script.getAreaById(posOutside.area);
            Vector2 areaPos = this.areaPositionWorld4PlayerInteract(area, vector2);
            exitPos.x = areaPos.x;
            exitPos.y = areaPos.y;
            Vector2ObjectPool.get().release(vector2);
        }
        exitPos.z = 0.0f;
        Vector3f seatedPos = this.getPassengerPositionWorldPos(posInside, (Vector3f)TL_vector3f_pool.get().alloc());
        boolean blocked = PolygonalMap2.instance.lineClearCollide(seatedPos.x, seatedPos.y, exitPos.x, exitPos.y, this.getZi(), this, false, false);
        TL_vector3f_pool.get().release(exitPos);
        TL_vector3f_pool.get().release(seatedPos);
        if (!blocked && GameClient.client && (sq = IsoWorld.instance.currentCell.getGridSquare(exitPos.x, exitPos.y, exitPos.z)) != null && chr instanceof IsoPlayer && !SafeHouse.isPlayerAllowedOnSquare(isoPlayer = (IsoPlayer)chr, sq)) {
            blocked = true;
        }
        return blocked;
    }

    public boolean isPassengerUseDoor2(IsoGameCharacter chr, int seat) {
        VehicleScript.Position posn = this.getPassengerPosition(seat, "outside2");
        if (posn != null) {
            Vector3f worldPos = this.getPassengerPositionWorldPos(posn, (Vector3f)TL_vector3f_pool.get().alloc());
            worldPos.sub(chr.getX(), chr.getY(), chr.getZ());
            float length = worldPos.length();
            TL_vector3f_pool.get().release(worldPos);
            if (length < 2.0f) {
                return true;
            }
        }
        return false;
    }

    public boolean isEnterBlocked2(IsoGameCharacter chr, int seat) {
        return this.isExitBlocked2(seat);
    }

    public boolean isExitBlocked2(int seat) {
        IsoPlayer isoPlayer;
        VehicleScript.Position posInside = this.getPassengerPosition(seat, "inside");
        VehicleScript.Position posOutside = this.getPassengerPosition(seat, "outside2");
        if (posInside == null || posOutside == null) {
            return true;
        }
        Vector3f exitPos = this.getPassengerPositionWorldPos(posOutside, (Vector3f)TL_vector3f_pool.get().alloc());
        exitPos.z = 0.0f;
        Vector3f seatedPos = this.getPassengerPositionWorldPos(posInside, (Vector3f)TL_vector3f_pool.get().alloc());
        boolean blocked = PolygonalMap2.instance.lineClearCollide(seatedPos.x, seatedPos.y, exitPos.x, exitPos.y, PZMath.fastfloor(this.getZ()), this, false, false);
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(exitPos.x, exitPos.y, exitPos.z);
        IsoGameCharacter chr = this.getCharacter(seat);
        if (chr instanceof IsoPlayer && !SafeHouse.isPlayerAllowedOnSquare(isoPlayer = (IsoPlayer)chr, sq)) {
            blocked = true;
        }
        TL_vector3f_pool.get().release(exitPos);
        TL_vector3f_pool.get().release(seatedPos);
        return blocked;
    }

    private void renderExits() {
        int scale = Core.tileScale;
        Vector3f exitPos = (Vector3f)TL_vector3f_pool.get().alloc();
        Vector3f seatedPos = (Vector3f)TL_vector3f_pool.get().alloc();
        for (int seat = 0; seat < this.getMaxPassengers(); ++seat) {
            VehicleScript.Position posInside = this.getPassengerPosition(seat, "inside");
            VehicleScript.Position posOutside = this.getPassengerPosition(seat, "outside");
            if (posInside == null || posOutside == null) continue;
            float radius = 0.3f;
            this.getPassengerPositionWorldPos(posOutside, exitPos);
            this.getPassengerPositionWorldPos(posInside, seatedPos);
            int x1 = PZMath.fastfloor(exitPos.x - 0.3f);
            int x2 = PZMath.fastfloor(exitPos.x + 0.3f);
            int y1 = PZMath.fastfloor(exitPos.y - 0.3f);
            int y2 = PZMath.fastfloor(exitPos.y + 0.3f);
            for (int y = y1; y <= y2; ++y) {
                for (int x = x1; x <= x2; ++x) {
                    float sx = IsoUtils.XToScreenExact(x, y + 1, PZMath.fastfloor(this.getZ()), 0);
                    float sy = IsoUtils.YToScreenExact(x, y + 1, PZMath.fastfloor(this.getZ()), 0);
                    if (PerformanceSettings.fboRenderChunk) {
                        sx += IsoCamera.cameras[IsoCamera.frameState.playerIndex].fixJigglyModelsX * IsoCamera.frameState.zoom;
                        sy += IsoCamera.cameras[IsoCamera.frameState.playerIndex].fixJigglyModelsY * IsoCamera.frameState.zoom;
                    }
                    IndieGL.glBlendFunc(770, 771);
                    SpriteRenderer.instance.renderPoly(sx, sy, sx + (float)(32 * scale), sy - (float)(16 * scale), sx + (float)(64 * scale), sy, sx + (float)(32 * scale), sy + (float)(16 * scale), 1.0f, 1.0f, 1.0f, 0.5f);
                }
            }
            float r = 1.0f;
            float g = 1.0f;
            float b = 1.0f;
            if (this.isExitBlocked(seat)) {
                b = 0.0f;
                g = 0.0f;
            }
            this.getController().drawCircle(seatedPos.x, seatedPos.y, 0.3f, 0.0f, 0.0f, 1.0f, 1.0f);
            this.getController().drawCircle(exitPos.x, exitPos.y, 0.3f, 1.0f, g, b, 1.0f);
        }
        TL_vector3f_pool.get().release(exitPos);
        TL_vector3f_pool.get().release(seatedPos);
    }

    private Vector2 areaPositionLocal(VehicleScript.Area area) {
        return this.areaPositionLocal(area, new Vector2());
    }

    private Vector2 areaPositionLocal(VehicleScript.Area area, Vector2 out) {
        Vector2 center = this.areaPositionWorld(area, out);
        Vector3f vec = (Vector3f)TL_vector3f_pool.get().alloc();
        this.getLocalPos(center.x, center.y, 0.0f, vec);
        center.set(vec.x, vec.z);
        TL_vector3f_pool.get().release(vec);
        return center;
    }

    public Vector2 areaPositionWorld(VehicleScript.Area area) {
        return this.areaPositionWorld(area, new Vector2());
    }

    public Vector2 areaPositionWorld(VehicleScript.Area area, Vector2 out) {
        if (area == null) {
            return null;
        }
        Vector3f worldPos = this.getWorldPos(area.x, 0.0f, area.y, (Vector3f)TL_vector3f_pool.get().alloc());
        out.set(worldPos.x, worldPos.y);
        TL_vector3f_pool.get().release(worldPos);
        return out;
    }

    public Vector2 areaPositionWorld4PlayerInteract(VehicleScript.Area area) {
        return this.areaPositionWorld4PlayerInteract(area, new Vector2());
    }

    public Vector2 areaPositionWorld4PlayerInteract(VehicleScript.Area area, Vector2 out) {
        Vector3f ext = this.script.getExtents();
        Vector3f com = this.script.getCenterOfMassOffset();
        Vector2 p = this.areaPositionWorld(area, out);
        Vector3f vec = this.getLocalPos(p.x, p.y, 0.0f, (Vector3f)TL_vector3f_pool.get().alloc());
        if (area.x > com.x + ext.x / 2.0f || area.x < com.x - ext.x / 2.0f) {
            vec.x = area.x > 0.0f ? (vec.x -= area.w * 0.3f) : (vec.x += area.w * 0.3f);
        } else {
            vec.z = area.y > 0.0f ? (vec.z -= area.h * 0.3f) : (vec.z += area.h * 0.3f);
        }
        this.getWorldPos(vec, vec);
        out.set(vec.x, vec.y);
        TL_vector3f_pool.get().release(vec);
        return out;
    }

    private void renderAreas() {
        if (this.getScript() == null) {
            return;
        }
        Vector3f forward = this.getForwardVector((Vector3f)TL_vector3f_pool.get().alloc());
        Vector2 vector2 = (Vector2)Vector2ObjectPool.get().alloc();
        for (int i = 0; i < this.parts.size(); ++i) {
            Vector2 center;
            VehicleScript.Area area;
            VehiclePart part = this.parts.get(i);
            if (part.getArea() == null || (area = this.getScript().getAreaById(part.getArea())) == null || (center = this.areaPositionWorld(area, vector2)) == null) continue;
            boolean inArea = this.isInArea(area.id, IsoPlayer.getInstance());
            this.getController().drawRect(forward, center.x - WorldSimulation.instance.offsetX, center.y - WorldSimulation.instance.offsetY, area.w, area.h / 2.0f, inArea ? 0.0f : 0.65f, inArea ? 1.0f : 0.65f, inArea ? 1.0f : 0.65f);
            center = this.areaPositionWorld4PlayerInteract(area, vector2);
            this.getController().drawRect(forward, center.x - WorldSimulation.instance.offsetX, center.y - WorldSimulation.instance.offsetY, 0.1f, 0.1f, 1.0f, 0.0f, 0.0f);
        }
        TL_vector3f_pool.get().release(forward);
        Vector2ObjectPool.get().release(vector2);
        LineDrawer.drawLine(IsoUtils.XToScreenExact(this.poly.x1, this.poly.y1, 0.0f, 0), IsoUtils.YToScreenExact(this.poly.x1, this.poly.y1, 0.0f, 0), IsoUtils.XToScreenExact(this.poly.x2, this.poly.y2, 0.0f, 0), IsoUtils.YToScreenExact(this.poly.x2, this.poly.y2, 0.0f, 0), 1.0f, 0.5f, 0.5f, 1.0f, 0);
        LineDrawer.drawLine(IsoUtils.XToScreenExact(this.poly.x2, this.poly.y2, 0.0f, 0), IsoUtils.YToScreenExact(this.poly.x2, this.poly.y2, 0.0f, 0), IsoUtils.XToScreenExact(this.poly.x3, this.poly.y3, 0.0f, 0), IsoUtils.YToScreenExact(this.poly.x3, this.poly.y3, 0.0f, 0), 1.0f, 0.5f, 0.5f, 1.0f, 0);
        LineDrawer.drawLine(IsoUtils.XToScreenExact(this.poly.x3, this.poly.y3, 0.0f, 0), IsoUtils.YToScreenExact(this.poly.x3, this.poly.y3, 0.0f, 0), IsoUtils.XToScreenExact(this.poly.x4, this.poly.y4, 0.0f, 0), IsoUtils.YToScreenExact(this.poly.x4, this.poly.y4, 0.0f, 0), 1.0f, 0.5f, 0.5f, 1.0f, 0);
        LineDrawer.drawLine(IsoUtils.XToScreenExact(this.poly.x4, this.poly.y4, 0.0f, 0), IsoUtils.YToScreenExact(this.poly.x4, this.poly.y4, 0.0f, 0), IsoUtils.XToScreenExact(this.poly.x1, this.poly.y1, 0.0f, 0), IsoUtils.YToScreenExact(this.poly.x1, this.poly.y1, 0.0f, 0), 1.0f, 0.5f, 0.5f, 1.0f, 0);
        LineDrawer.drawLine(IsoUtils.XToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0f, 0), IsoUtils.YToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0f, 0), IsoUtils.XToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0f, 0), IsoUtils.YToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0f, 0), 0.5f, 1.0f, 0.5f, 1.0f, 0);
        LineDrawer.drawLine(IsoUtils.XToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0f, 0), IsoUtils.YToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0f, 0), IsoUtils.XToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0f, 0), IsoUtils.YToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0f, 0), 0.5f, 1.0f, 0.5f, 1.0f, 0);
        LineDrawer.drawLine(IsoUtils.XToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0f, 0), IsoUtils.YToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0f, 0), IsoUtils.XToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0f, 0), IsoUtils.YToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0f, 0), 0.5f, 1.0f, 0.5f, 1.0f, 0);
        LineDrawer.drawLine(IsoUtils.XToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0f, 0), IsoUtils.YToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0f, 0), IsoUtils.XToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0f, 0), IsoUtils.YToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0f, 0), 0.5f, 1.0f, 0.5f, 1.0f, 0);
    }

    private void renderInterpolateBuffer() {
        if (this.netPlayerAuthorization != Authorization.Remote) {
            return;
        }
        float grx = IsoUtils.XToScreenExact(this.getX(), this.getY(), 0.0f, 0);
        float gry = IsoUtils.YToScreenExact(this.getX(), this.getY(), 0.0f, 0);
        float x = grx - 310.0f;
        float y = gry + 22.0f;
        float w = 300.0f;
        float h = 150.0f;
        float positionScale = 4.0f;
        Color colorBorder = Color.lightGray;
        Color dataCurTime = Color.green;
        Color dataCurShiftTime = Color.cyan;
        Color yellow = Color.yellow;
        Color blue = Color.blue;
        Color red = Color.red;
        LineDrawer.drawLine(x, y, x + 300.0f, y, colorBorder.r, colorBorder.g, colorBorder.b, colorBorder.a, 1);
        LineDrawer.drawLine(x, y + 150.0f, x + 300.0f, y + 150.0f, colorBorder.r, colorBorder.g, colorBorder.b, colorBorder.a, 1);
        long t = GameTime.getServerTimeMills();
        long tStart = t - 150L - (long)this.interpolation.history;
        long tEnd = t + 150L;
        this.renderInterpolateBuffer_drawVertLine(tStart, colorBorder, x, y, 300.0f, 150.0f, tStart, tEnd, true);
        this.renderInterpolateBuffer_drawVertLine(tEnd, colorBorder, x, y, 300.0f, 150.0f, tStart, tEnd, true);
        this.renderInterpolateBuffer_drawVertLine(t, dataCurTime, x, y, 300.0f, 150.0f, tStart, tEnd, true);
        this.renderInterpolateBuffer_drawVertLine(t - (long)this.interpolation.delay, dataCurShiftTime, x, y, 300.0f, 150.0f, tStart, tEnd, true);
        this.renderInterpolateBuffer_drawPoint(t - (long)this.interpolation.delay, this.getX(), blue, 5, x, y, 300.0f, 150.0f, tStart, tEnd, this.getX() - 4.0f, this.getX() + 4.0f);
        this.renderInterpolateBuffer_drawPoint(t - (long)this.interpolation.delay, this.getY(), red, 5, x, y, 300.0f, 150.0f, tStart, tEnd, this.getY() - 4.0f, this.getY() + 4.0f);
        long prevT = 0L;
        float prevX = Float.NaN;
        float prevY = Float.NaN;
        VehicleInterpolationData temp = new VehicleInterpolationData();
        temp.time = t - (long)this.interpolation.delay;
        VehicleInterpolationData higher = this.interpolation.buffer.higher(temp);
        VehicleInterpolationData lower = this.interpolation.buffer.floor(temp);
        for (VehicleInterpolationData data : this.interpolation.buffer) {
            boolean parity = (data.hashCode() & 1) == 0;
            this.renderInterpolateBuffer_drawVertLine(data.time, yellow, x, y, 300.0f, 150.0f, tStart, tEnd, parity);
            if (data == higher) {
                this.renderInterpolateBuffer_drawTextHL(data.time, "H", dataCurShiftTime, x, y, 300.0f, 150.0f, tStart, tEnd);
            }
            if (data == lower) {
                this.renderInterpolateBuffer_drawTextHL(data.time, "L", dataCurShiftTime, x, y, 300.0f, 150.0f, tStart, tEnd);
            }
            this.renderInterpolateBuffer_drawPoint(data.time, data.x, blue, 5, x, y, 300.0f, 150.0f, tStart, tEnd, this.getX() - 4.0f, this.getX() + 4.0f);
            this.renderInterpolateBuffer_drawPoint(data.time, data.y, red, 5, x, y, 300.0f, 150.0f, tStart, tEnd, this.getY() - 4.0f, this.getY() + 4.0f);
            if (!Float.isNaN(prevX)) {
                this.renderInterpolateBuffer_drawLine(prevT, prevX, data.time, data.x, blue, x, y, 300.0f, 150.0f, tStart, tEnd, this.getX() - 4.0f, this.getX() + 4.0f);
                this.renderInterpolateBuffer_drawLine(prevT, prevY, data.time, data.y, red, x, y, 300.0f, 150.0f, tStart, tEnd, this.getY() - 4.0f, this.getY() + 4.0f);
            }
            prevT = data.time;
            prevX = data.x;
            prevY = data.y;
        }
        float[] physicsBuf = new float[27];
        float[] engineSoundBuf = new float[2];
        boolean ret = this.interpolation.interpolationDataGet(physicsBuf, engineSoundBuf, t - (long)this.interpolation.delay);
        TextManager.instance.DrawString(x, y + 150.0f + 20.0f, String.format("interpolationDataGet=%s", ret ? "True" : "False"), dataCurShiftTime.r, dataCurShiftTime.g, dataCurShiftTime.b, dataCurShiftTime.a);
        TextManager.instance.DrawString(x, y + 150.0f + 30.0f, String.format("buffer.size=%d buffering=%s", this.interpolation.buffer.size(), String.valueOf(this.interpolation.buffering)), dataCurShiftTime.r, dataCurShiftTime.g, dataCurShiftTime.b, dataCurShiftTime.a);
        TextManager.instance.DrawString(x, y + 150.0f + 40.0f, String.format("delayTarget=%d", this.interpolation.delayTarget), dataCurShiftTime.r, dataCurShiftTime.g, dataCurShiftTime.b, dataCurShiftTime.a);
        if (this.interpolation.buffer.size() >= 2) {
            TextManager.instance.DrawString(x, y + 150.0f + 50.0f, String.format("last=%d first=%d", this.interpolation.buffer.last().time, this.interpolation.buffer.first().time), dataCurShiftTime.r, dataCurShiftTime.g, dataCurShiftTime.b, dataCurShiftTime.a);
            TextManager.instance.DrawString(x, y + 150.0f + 60.0f, String.format("(last-first).time=%d delay=%d", this.interpolation.buffer.last().time - this.interpolation.buffer.first().time, this.interpolation.delay), dataCurShiftTime.r, dataCurShiftTime.g, dataCurShiftTime.b, dataCurShiftTime.a);
        }
    }

    private void renderInterpolateBuffer_drawTextHL(long x1, String text, Color col, float x, float y, float w, float h, long start, long end) {
        float tM = w / (float)(end - start);
        float tempX = (float)(x1 - start) * tM;
        TextManager.instance.DrawString(tempX + x, y, text, col.r, col.g, col.b, col.a);
    }

    private void renderInterpolateBuffer_drawVertLine(long x1, Color col, float x, float y, float w, float h, long start, long end, boolean drawParity) {
        float tM = w / (float)(end - start);
        float tempX = (float)(x1 - start) * tM;
        LineDrawer.drawLine(tempX + x, y, tempX + x, y + h, col.r, col.g, col.b, col.a, 1);
        TextManager.instance.DrawString(tempX + x, y + h + (drawParity ? 0.0f : 10.0f), String.format("%.1f", Float.valueOf((float)(x1 - x1 / 100000L * 100000L) / 1000.0f)), col.r, col.g, col.b, col.a);
    }

    private void renderInterpolateBuffer_drawLine(long x1, float y1, long x2, float y2, Color col, float x, float y, float w, float h, long start, long end, float starty, float endy) {
        float tM = w / (float)(end - start);
        float tempX = (float)(x1 - start) * tM;
        float tempX2 = (float)(x2 - start) * tM;
        float tMy = h / (endy - starty);
        float tempY = (y1 - starty) * tMy;
        float tempY2 = (y2 - starty) * tMy;
        LineDrawer.drawLine(tempX + x, tempY + y, tempX2 + x, tempY2 + y, col.r, col.g, col.b, col.a, 1);
    }

    private void renderInterpolateBuffer_drawPoint(long x1, float y1, Color col, int radius, float x, float y, float w, float h, long start, long end, float starty, float endy) {
        float tM = w / (float)(end - start);
        float tempX = (float)(x1 - start) * tM;
        float tMy = h / (endy - starty);
        float tempY = (y1 - starty) * tMy;
        LineDrawer.drawCircle(tempX + x, tempY + y, radius, 10, col.r, col.g, col.b);
    }

    private void renderAuthorizations() {
        float r = 0.3f;
        float g = 0.3f;
        float b = 0.3f;
        float a = 0.5f;
        switch (this.netPlayerAuthorization.ordinal()) {
            case 0: {
                r = 1.0f;
                break;
            }
            case 1: {
                b = 1.0f;
                break;
            }
            case 3: {
                g = 1.0f;
                break;
            }
            case 4: {
                g = 1.0f;
                r = 1.0f;
                break;
            }
            case 2: {
                b = 1.0f;
                r = 1.0f;
            }
        }
        LineDrawer.drawLine(IsoUtils.XToScreenExact(this.poly.x1, this.poly.y1, 0.0f, 0), IsoUtils.YToScreenExact(this.poly.x1, this.poly.y1, 0.0f, 0), IsoUtils.XToScreenExact(this.poly.x2, this.poly.y2, 0.0f, 0), IsoUtils.YToScreenExact(this.poly.x2, this.poly.y2, 0.0f, 0), r, g, b, a, 1);
        LineDrawer.drawLine(IsoUtils.XToScreenExact(this.poly.x2, this.poly.y2, 0.0f, 0), IsoUtils.YToScreenExact(this.poly.x2, this.poly.y2, 0.0f, 0), IsoUtils.XToScreenExact(this.poly.x3, this.poly.y3, 0.0f, 0), IsoUtils.YToScreenExact(this.poly.x3, this.poly.y3, 0.0f, 0), r, g, b, a, 1);
        LineDrawer.drawLine(IsoUtils.XToScreenExact(this.poly.x3, this.poly.y3, 0.0f, 0), IsoUtils.YToScreenExact(this.poly.x3, this.poly.y3, 0.0f, 0), IsoUtils.XToScreenExact(this.poly.x4, this.poly.y4, 0.0f, 0), IsoUtils.YToScreenExact(this.poly.x4, this.poly.y4, 0.0f, 0), r, g, b, a, 1);
        LineDrawer.drawLine(IsoUtils.XToScreenExact(this.poly.x4, this.poly.y4, 0.0f, 0), IsoUtils.YToScreenExact(this.poly.x4, this.poly.y4, 0.0f, 0), IsoUtils.XToScreenExact(this.poly.x1, this.poly.y1, 0.0f, 0), IsoUtils.YToScreenExact(this.poly.x1, this.poly.y1, 0.0f, 0), r, g, b, a, 1);
        float distance = 0.0f;
        if (this.getVehicleTowing() != null) {
            Vector3fObjectPool pool = TL_vector3f_pool.get();
            Vector3f v1t = (Vector3f)pool.alloc();
            Vector3f worldA = this.getTowingWorldPos(this.getTowAttachmentSelf(), v1t);
            Vector3f v2t = (Vector3f)pool.alloc();
            Vector3f worldB = this.getVehicleTowing().getTowingWorldPos(this.getVehicleTowing().getTowAttachmentSelf(), v2t);
            if (worldA != null && worldB != null) {
                LineDrawer.DrawIsoLine(worldA.x, worldA.y, worldA.z, worldB.x, worldB.y, worldB.z, r, g, b, a, 1);
                LineDrawer.DrawIsoCircle(worldA.x, worldA.y, worldA.z, 0.2f, 16, r, g, b, a);
                distance = IsoUtils.DistanceTo(worldA.x, worldA.y, worldA.z, worldB.x, worldB.y, worldB.z);
            }
            pool.release(v1t);
            pool.release(v2t);
        }
        r = 1.0f;
        g = 1.0f;
        b = 0.75f;
        a = 1.0f;
        float dy = 10.0f;
        float sx = IsoUtils.XToScreenExact(this.getX(), this.getY(), 0.0f, 0);
        float sy = IsoUtils.YToScreenExact(this.getX(), this.getY(), 0.0f, 0);
        IsoPlayer owner = GameClient.IDToPlayerMap.get(this.netPlayerId);
        String player = (owner == null ? "@server" : owner.getUsername()) + " ( " + this.netPlayerId + " )";
        TextManager.instance.DrawString(sx, sy + (dy += 12.0f), "VID: " + this.getScriptName() + " ( " + this.getId() + " )", r, g, b, a);
        TextManager.instance.DrawString(sx, sy + (dy += 12.0f), "PID: " + player, r, g, b, a);
        TextManager.instance.DrawString(sx, sy + (dy += 12.0f), "Auth: " + this.netPlayerAuthorization.name(), r, g, b, a);
        TextManager.instance.DrawString(sx, sy + (dy += 12.0f), "Static/active: " + this.isStatic + "/" + this.isActive, r, g, b, a);
        TextManager.instance.DrawString(sx, sy + (dy += 12.0f), "x=" + this.getX() + " / y=" + this.getY(), r, g, b, a);
        TextManager.instance.DrawString(sx, sy + (dy += 14.0f), String.format("Passengers: %d/%d", Arrays.stream(this.passengers).filter(p -> p.character != null).count(), this.passengers.length), r, g, b, a);
        TextManager.instance.DrawString(sx, sy + (dy += 12.0f), String.format("Speed: %s%.3f kmph", this.getCurrentSpeedKmHour() >= 0.0f ? "+" : "", Float.valueOf(this.getCurrentSpeedKmHour())), r, g, b, a);
        TextManager.instance.DrawString(sx, sy + (dy += 12.0f), String.format("Engine speed: %.3f", this.engineSpeed), r, g, b, a);
        TextManager.instance.DrawString(sx, sy + (dy += 12.0f), String.format("Mass: %.3f/%.3f", Float.valueOf(this.getMass()), Float.valueOf(this.getFudgedMass())), r, g, b, a);
        if (distance > 1.5f) {
            g = 0.75f;
        }
        if (this.getVehicleTowing() != null) {
            TextManager.instance.DrawString(sx, sy + (dy += 14.0f), "Towing: " + this.getVehicleTowing().getId(), r, g, b, a);
            TextManager.instance.DrawString(sx, sy + (dy += 12.0f), String.format("Distance: %.3f", Float.valueOf(distance)), r, g, b, a);
        }
        if (this.getVehicleTowedBy() != null) {
            TextManager.instance.DrawString(sx, sy + (dy += 14.0f), "TowedBy: " + this.getVehicleTowedBy().getId(), r, g, b, a);
            TextManager.instance.DrawString(sx, sy + (dy += 12.0f), String.format("Distance: %.3f", Float.valueOf(distance)), r, g, b, a);
        }
    }

    private void renderUsableArea() {
        int seat;
        if (this.getScript() == null || !UIManager.visibleAllUi) {
            return;
        }
        if (this.getAlpha(IsoPlayer.getPlayerIndex()) == 0.0f) {
            return;
        }
        IsoPlayer chr = IsoPlayer.getInstance();
        VehiclePart part = this.getUseablePart(chr);
        boolean bBestSeat = false;
        if (part == null && this == chr.getUseableVehicle() && (seat = this.getBestSeat(chr)) != -1) {
            part = this.getPassengerDoor(seat);
            bBestSeat = true;
        }
        if (part == null) {
            return;
        }
        VehicleScript.Area area = this.getScript().getAreaById(part.getArea());
        if (area == null) {
            return;
        }
        Vector2 vector2 = (Vector2)Vector2ObjectPool.get().alloc();
        Vector2 center = this.areaPositionWorld(area, vector2);
        if (center == null) {
            Vector2ObjectPool.get().release(vector2);
            return;
        }
        Vector3f forward = this.getForwardVector((Vector3f)TL_vector3f_pool.get().alloc());
        float r = Core.getInstance().getGoodHighlitedColor().getR();
        float g = Core.getInstance().getGoodHighlitedColor().getG();
        float b = Core.getInstance().getGoodHighlitedColor().getB();
        if (bBestSeat) {
            r *= 0.6666667f;
            g *= 0.6666667f;
            b *= 0.6666667f;
        }
        this.getController().drawRect(forward, center.x - WorldSimulation.instance.offsetX, center.y - WorldSimulation.instance.offsetY, area.w, area.h / 2.0f, r, g, b);
        forward.x *= area.h / this.script.getModelScale();
        forward.z *= area.h / this.script.getModelScale();
        if (part.getDoor() != null && (part.getId().contains("Left") || part.getId().contains("Right"))) {
            if (part.getId().contains("Front")) {
                this.getController().drawRect(forward, center.x - WorldSimulation.instance.offsetX + forward.x * area.h / 2.0f, center.y - WorldSimulation.instance.offsetY + forward.z * area.h / 2.0f, area.w, area.h / 8.0f, r, g, b);
            } else if (part.getId().contains("Rear")) {
                this.getController().drawRect(forward, center.x - WorldSimulation.instance.offsetX - forward.x * area.h / 2.0f, center.y - WorldSimulation.instance.offsetY - forward.z * area.h / 2.0f, area.w, area.h / 8.0f, r, g, b);
            }
        }
        Vector2ObjectPool.get().release(vector2);
        TL_vector3f_pool.get().release(forward);
    }

    private boolean couldSeeIntersectedSquare(int playerIndex) {
        VehiclePoly poly = this.getPoly();
        float minX = Math.min(poly.x1, Math.min(poly.x2, Math.min(poly.x3, poly.x4)));
        float minY = Math.min(poly.y1, Math.min(poly.y2, Math.min(poly.y3, poly.y4)));
        float maxX = Math.max(poly.x1, Math.max(poly.x2, Math.max(poly.x3, poly.x4)));
        float maxY = Math.max(poly.y1, Math.max(poly.y2, Math.max(poly.y3, poly.y4)));
        int z = PZMath.fastfloor(this.getZ());
        for (int y = PZMath.fastfloor(minY); y < (int)Math.ceil(maxY); ++y) {
            for (int x = PZMath.fastfloor(minX); x < (int)Math.ceil(maxX); ++x) {
                IsoGridSquare square = this.getCell().getGridSquare(x, y, z);
                if (square == null || !square.isCouldSee(playerIndex) || !this.isIntersectingSquare(x, y, z)) continue;
                return true;
            }
        }
        return false;
    }

    private void renderIntersectedSquares() {
        VehiclePoly poly = this.getPoly();
        float minX = Math.min(poly.x1, Math.min(poly.x2, Math.min(poly.x3, poly.x4)));
        float minY = Math.min(poly.y1, Math.min(poly.y2, Math.min(poly.y3, poly.y4)));
        float maxX = Math.max(poly.x1, Math.max(poly.x2, Math.max(poly.x3, poly.x4)));
        float maxY = Math.max(poly.y1, Math.max(poly.y2, Math.max(poly.y3, poly.y4)));
        for (int y = PZMath.fastfloor(minY); y < (int)Math.ceil(maxY); ++y) {
            for (int x = PZMath.fastfloor(minX); x < (int)Math.ceil(maxX); ++x) {
                if (!this.isIntersectingSquare(x, y, PZMath.fastfloor(this.getZ()))) continue;
                LineDrawer.addRect(x, y, PZMath.fastfloor(this.getZ()), 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
    }

    private void renderTrailerPositions() {
        boolean blocked;
        Vector3f v2;
        if (this.script == null || this.physics == null) {
            return;
        }
        Vector3f v1 = (Vector3f)TL_vector3f_pool.get().alloc();
        Vector3f v3 = (Vector3f)TL_vector3f_pool.get().alloc();
        Vector3f vt = this.getTowingWorldPos("trailer", v3);
        if (vt != null) {
            this.physics.drawCircle(vt.x, vt.y, 0.3f, 1.0f, 1.0f, 1.0f, 1.0f);
        }
        if ((v2 = this.getPlayerTrailerLocalPos("trailer", false, v1)) != null) {
            this.getWorldPos(v2, v2);
            blocked = PolygonalMap2.instance.lineClearCollide(v3.x, v3.y, v2.x, v2.y, PZMath.fastfloor(this.getZ()), this, false, false);
            this.physics.drawCircle(v2.x, v2.y, 0.3f, 1.0f, blocked ? 0.0f : 1.0f, blocked ? 0.0f : 1.0f, 1.0f);
            if (blocked) {
                LineDrawer.addLine(v2.x, v2.y, 0.0f, v3.x, v3.y, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f);
            }
        }
        if ((v2 = this.getPlayerTrailerLocalPos("trailer", true, v1)) != null) {
            this.getWorldPos(v2, v2);
            blocked = PolygonalMap2.instance.lineClearCollide(v3.x, v3.y, v2.x, v2.y, PZMath.fastfloor(this.getZ()), this, false, false);
            this.physics.drawCircle(v2.x, v2.y, 0.3f, 1.0f, blocked ? 0.0f : 1.0f, blocked ? 0.0f : 1.0f, 1.0f);
            if (blocked) {
                LineDrawer.addLine(v2.x, v2.y, 0.0f, v3.x, v3.y, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f);
            }
        }
        TL_vector3f_pool.get().release(v1);
        TL_vector3f_pool.get().release(v3);
    }

    public void getWheelForwardVector(int wheelIndex, Vector3f out) {
        WheelInfo wheelInfo = this.wheelInfo[wheelIndex];
        Matrix4f m = (Matrix4f)TL_matrix4f_pool.get().alloc();
        m.rotationY(wheelInfo.steering);
        Matrix4f chassisRotMatrix = this.jniTransform.getMatrix((Matrix4f)TL_matrix4f_pool.get().alloc());
        chassisRotMatrix.setTranslation(0.0f, 0.0f, 0.0f);
        m.mul(chassisRotMatrix, m);
        TL_matrix4f_pool.get().release(chassisRotMatrix);
        TL_matrix4f_pool.get().release(m);
        Vector4f forward = BaseVehicle.allocVector4f();
        m.getColumn(2, forward);
        out.set(forward.x, 0.0f, forward.z);
        BaseVehicle.releaseVector4f(forward);
    }

    public void tryStartEngine(boolean haveKey) {
        if (this.getDriver() != null && this.getDriver() instanceof IsoPlayer && ((IsoPlayer)this.getDriver()).isBlockMovement()) {
            return;
        }
        VehiclePart part = this.getPartById("Engine");
        if (part == null || part.getCondition() <= 0) {
            if (part != null) {
                this.engineDoStartingFailed("VehicleEngineFailureDamage");
            }
            return;
        }
        if (this.getEngineQuality() <= 0) {
            this.engineDoStartingFailed("VehicleEngineFailureDamage");
            return;
        }
        if (this.engineState != engineStateTypes.Idle) {
            return;
        }
        DrainableComboItem batteryItem = (DrainableComboItem)this.getBattery().getInventoryItem();
        if (batteryItem == null) {
            return;
        }
        float currentCharge = batteryItem.getCurrentUsesFloat();
        batteryItem.setCurrentUsesFloat(PZMath.clamp(currentCharge - 0.025f, 0.0f, 1.0f));
        if (currentCharge <= 0.1f) {
            this.engineDoStartingFailedNoPower();
            return;
        }
        if (Core.debug && DebugOptions.instance.cheat.vehicle.startWithoutKey.getValue() || SandboxOptions.instance.vehicleEasyUse.getValue() || this.isKeysInIgnition() || haveKey || this.isHotwired()) {
            this.engineDoStarting();
        } else if (GameServer.server) {
            this.getDriver().sendObjectChange(IsoObjectChange.VEHICLE_NO_KEY);
        } else {
            this.getDriver().SayDebug(" [img=media/ui/CarKey_none.png]");
            this.checkVehicleFailsToStartWithZombiesTargeting();
        }
    }

    public void tryStartEngine() {
        this.tryStartEngine(false);
    }

    public void engineDoIdle() {
        this.engineState = engineStateTypes.Idle;
        this.engineLastUpdateStateTime = System.currentTimeMillis();
        this.transmitEngine();
    }

    public void engineDoStarting() {
        this.engineState = engineStateTypes.Starting;
        this.engineLastUpdateStateTime = System.currentTimeMillis();
        this.transmitEngine();
        this.setKeysInIgnition(true);
        this.setPreviouslyMoved(true);
    }

    public boolean isStarting() {
        return this.engineState == engineStateTypes.Starting || this.engineState == engineStateTypes.StartingFailed || this.engineState == engineStateTypes.StartingSuccess || this.engineState == engineStateTypes.StartingFailedNoPower;
    }

    private String getEngineSound() {
        if (this.getScript() != null && this.getScript().getSounds().engine != null) {
            return this.getScript().getSounds().engine;
        }
        return "VehicleEngineDefault";
    }

    private String getEngineStartSound() {
        if (this.getScript() != null && this.getScript().getSounds().engineStart != null) {
            return this.getScript().getSounds().engineStart;
        }
        return "VehicleStarted";
    }

    private String getEngineTurnOffSound() {
        if (this.getScript() != null && this.getScript().getSounds().engineTurnOff != null) {
            return this.getScript().getSounds().engineTurnOff;
        }
        return "VehicleTurnedOff";
    }

    private String getHandBrakeSound() {
        if (this.getScript() != null && this.getScript().getSounds().handBrake != null) {
            return this.getScript().getSounds().handBrake;
        }
        return "VehicleHandBrake";
    }

    private String getIgnitionFailSound() {
        if (this.getScript() != null && this.getScript().getSounds().ignitionFail != null) {
            return this.getScript().getSounds().ignitionFail;
        }
        return "VehicleFailingToStart";
    }

    private String getIgnitionFailNoPowerSound() {
        if (this.getScript() != null && this.getScript().getSounds().ignitionFailNoPower != null) {
            return this.getScript().getSounds().ignitionFailNoPower;
        }
        return SoundKey.VEHICLE_IGNITION_FAIL_DEFAULT.toString();
    }

    public void engineDoRetryingStarting() {
        this.getEmitter().stopSoundByName(this.getIgnitionFailSound());
        this.getEmitter().playSoundImpl(this.getIgnitionFailSound(), (IsoObject)null);
        this.engineState = engineStateTypes.RetryingStarting;
        this.engineLastUpdateStateTime = System.currentTimeMillis();
        this.transmitEngine();
        this.checkVehicleFailsToStartWithZombiesTargeting();
    }

    public void engineDoStartingSuccess() {
        this.getEmitter().stopSoundByName(this.getIgnitionFailSound());
        this.engineState = engineStateTypes.StartingSuccess;
        this.engineLastUpdateStateTime = System.currentTimeMillis();
        if (this.getEngineStartSound().equals(this.getEngineSound())) {
            if (!this.getEmitter().isPlaying(this.combinedEngineSound)) {
                this.combinedEngineSound = this.emitter.playSoundImpl(this.getEngineSound(), (IsoObject)null);
            }
        } else {
            this.getEmitter().playSoundImpl(this.getEngineStartSound(), (IsoObject)null);
        }
        this.transmitEngine();
        this.setKeysInIgnition(true);
        this.checkVehicleStartsWithZombiesTargeting();
    }

    public void engineDoStartingFailed() {
        this.engineDoStartingFailed(this.getIgnitionFailSound());
    }

    public void engineDoStartingFailed(String sound) {
        this.getEmitter().stopSoundByName(sound);
        this.getEmitter().playSoundImpl(sound, (IsoObject)null);
        this.stopEngineSounds();
        this.engineState = engineStateTypes.StartingFailed;
        this.engineLastUpdateStateTime = System.currentTimeMillis();
        this.transmitEngine();
        this.checkVehicleFailsToStartWithZombiesTargeting();
    }

    public void engineDoStartingFailedNoPower() {
        this.getEmitter().stopSoundByName(this.getIgnitionFailNoPowerSound());
        this.getEmitter().playSoundImpl(this.getIgnitionFailNoPowerSound(), (IsoObject)null);
        this.stopEngineSounds();
        this.engineState = engineStateTypes.StartingFailedNoPower;
        this.engineLastUpdateStateTime = System.currentTimeMillis();
        this.transmitEngine();
        this.checkVehicleFailsToStartWithZombiesTargeting();
    }

    public void engineDoRunning() {
        this.setNeedPartsUpdate(true);
        this.engineState = engineStateTypes.Running;
        this.engineLastUpdateStateTime = System.currentTimeMillis();
        this.transmitEngine();
    }

    public void engineDoStalling() {
        this.getEmitter().playSoundImpl("VehicleRunningOutOfGas", (IsoObject)null);
        this.engineState = engineStateTypes.Stalling;
        this.engineLastUpdateStateTime = System.currentTimeMillis();
        this.stopEngineSounds();
        this.engineSoundIndex = 0;
        this.transmitEngine();
        this.checkVehicleFailsToStartWithZombiesTargeting();
        if (!Core.getInstance().getOptionLeaveKeyInIgnition()) {
            this.setKeysInIgnition(false);
        }
    }

    public void engineDoShuttingDown() {
        this.engineDoShuttingDown(this.getEngineTurnOffSound());
    }

    public void engineDoShuttingDown(String sound) {
        VehiclePart heater;
        if (!StringUtils.equals(sound, this.getEngineSound())) {
            this.getEmitter().playSoundImpl(sound, (IsoObject)null);
        }
        this.stopEngineSounds();
        this.engineSoundIndex = 0;
        this.engineState = engineStateTypes.ShutingDown;
        this.engineLastUpdateStateTime = System.currentTimeMillis();
        this.transmitEngine();
        if (!Core.getInstance().getOptionLeaveKeyInIgnition()) {
            this.setKeysInIgnition(false);
        }
        if ((heater = this.getHeater()) != null) {
            heater.getModData().rawset("active", (Object)false);
        }
    }

    public void shutOff() {
        this.shutOff(this.getEngineTurnOffSound());
    }

    public void shutOff(String sound) {
        if (this.getPartById("GasTank").getContainerContentAmount() == 0.0f) {
            this.engineDoStalling();
            return;
        }
        this.engineDoShuttingDown(sound);
    }

    public void resumeRunningAfterLoad() {
        if (GameClient.client) {
            IsoGameCharacter driver = this.getDriver();
            if (driver == null) {
                return;
            }
            Boolean haveKey = this.getDriver().getInventory().haveThisKeyId(this.getKeyId()) != null ? Boolean.TRUE : Boolean.FALSE;
            GameClient.instance.sendClientCommandV((IsoPlayer)this.getDriver(), "vehicle", "startEngine", "haveKey", haveKey);
            return;
        }
        if (!this.isEngineWorking()) {
            return;
        }
        this.getEmitter();
        this.engineDoStartingSuccess();
    }

    public boolean isEngineStarted() {
        return this.engineState == engineStateTypes.Starting || this.engineState == engineStateTypes.StartingFailed || this.engineState == engineStateTypes.StartingSuccess || this.engineState == engineStateTypes.RetryingStarting;
    }

    public boolean isEngineRunning() {
        return this.engineState == engineStateTypes.Running;
    }

    public boolean isEngineWorking() {
        for (int i = 0; i < this.parts.size(); ++i) {
            VehiclePart part = this.parts.get(i);
            String functionName = part.getLuaFunction("checkEngine");
            if (functionName == null || Boolean.TRUE.equals(this.callLuaBoolean(functionName, this, part))) continue;
            return false;
        }
        return true;
    }

    public boolean isOperational() {
        for (int i = 0; i < this.parts.size(); ++i) {
            VehiclePart part = this.parts.get(i);
            String functionName = part.getLuaFunction("checkOperate");
            if (functionName == null || Boolean.TRUE.equals(this.callLuaBoolean(functionName, this, part))) continue;
            return false;
        }
        return true;
    }

    public boolean isDriveable() {
        if (!this.isEngineWorking()) {
            return false;
        }
        return this.isOperational();
    }

    public BaseSoundEmitter getEmitter() {
        if (this.emitter == null) {
            if (Core.soundDisabled || GameServer.server) {
                this.emitter = new DummySoundEmitter();
            } else {
                FMODSoundEmitter emitter1 = new FMODSoundEmitter();
                emitter1.parameterUpdater = this;
                this.emitter = emitter1;
            }
        }
        return this.emitter;
    }

    public long playSoundImpl(String file, IsoObject parent) {
        return this.getEmitter().playSoundImpl(file, parent);
    }

    public int stopSound(long channel) {
        return this.getEmitter().stopSound(channel);
    }

    public void playSound(String sound) {
        this.getEmitter().playSound(sound);
    }

    public void updateSounds() {
        if (!GameServer.server) {
            if (this.getBatteryCharge() > 0.0f) {
                if (this.lightbarSirenMode.isEnable() && this.soundSirenSignal == -1L) {
                    this.setLightbarSirenMode(this.lightbarSirenMode.get());
                }
            } else if (this.soundSirenSignal != -1L) {
                this.getEmitter().stopSound(this.soundSirenSignal);
                this.soundSirenSignal = -1L;
            }
        }
        IsoPlayer closestListener = null;
        float closestListenerDistSq = Float.MAX_VALUE;
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            IsoPlayer chr = IsoPlayer.players[i];
            if (chr == null || chr.getCurrentSquare() == null) continue;
            float px = chr.getX();
            float py = chr.getY();
            float dist = IsoUtils.DistanceToSquared(px, py, this.getX(), this.getY());
            dist *= chr.getHearDistanceModifier();
            if (chr.hasTrait(CharacterTrait.DEAF)) {
                dist = Float.MAX_VALUE;
            }
            if (!(dist < closestListenerDistSq)) continue;
            closestListener = chr;
            closestListenerDistSq = dist;
        }
        if (closestListener == null) {
            if (this.emitter != null) {
                this.emitter.setPos(this.getX(), this.getY(), this.getZ());
                if (!this.emitter.isEmpty()) {
                    this.emitter.tick();
                }
            }
            return;
        }
        if (!GameServer.server) {
            float rainIntensity;
            float f = rainIntensity = ClimateManager.getInstance().isRaining() ? ClimateManager.getInstance().getPrecipitationIntensity() : 0.0f;
            if (this.getSquare() != null && this.getSquare().isInARoom()) {
                rainIntensity = 0.0f;
            }
            if (this.getEmitter().isPlaying("VehicleAmbiance")) {
                if (rainIntensity == 0.0f) {
                    this.getEmitter().stopOrTriggerSoundByName("VehicleAmbiance");
                }
            } else if (rainIntensity > 0.0f && closestListenerDistSq < 100.0f) {
                this.emitter.playAmbientLoopedImpl("VehicleAmbiance");
            }
            if (closestListenerDistSq > 1200.0f) {
                this.stopEngineSounds();
                if (this.emitter != null && !this.emitter.isEmpty()) {
                    this.emitter.setPos(this.getX(), this.getY(), this.getZ());
                    this.emitter.tick();
                }
                return;
            }
            for (int i = 0; i < this.newEngineSoundId.length; ++i) {
                if (this.newEngineSoundId[i] == 0L) continue;
                this.getEmitter().setVolume(this.newEngineSoundId[i], 1.0f - closestListenerDistSq / 1200.0f);
            }
        }
        if (this.getController() == null) {
            return;
        }
        if (GameServer.server) {
            return;
        }
        if (this.emitter == null) {
            if (this.engineState != engineStateTypes.Running) {
                return;
            }
            this.getEmitter();
        }
        boolean isAnyListenerInside = this.isAnyListenerInside();
        if (this.startTime <= 0.0f && this.engineState == engineStateTypes.Running && !this.getEmitter().isPlaying(this.combinedEngineSound)) {
            this.combinedEngineSound = this.emitter.playSoundImpl(this.getEngineSound(), (IsoObject)null);
        }
        boolean skidding = false;
        if (!GameClient.client || this.isLocalPhysicSim()) {
            for (int i = 0; i < this.script.getWheelCount(); ++i) {
                if (!(this.wheelInfo[i].skidInfo < 0.15f)) continue;
                skidding = true;
                break;
            }
        }
        if (this.getDriver() == null) {
            skidding = false;
        }
        if (skidding != this.skidding) {
            if (skidding) {
                this.skidSound = this.getEmitter().playSoundImpl("VehicleSkid", (IsoObject)null);
            } else if (this.skidSound != 0L) {
                this.emitter.stopSound(this.skidSound);
                this.skidSound = 0L;
            }
            this.skidding = skidding;
        }
        if (this.soundBackMoveSignal != -1L && this.emitter != null) {
            this.emitter.set3D(this.soundBackMoveSignal, !isAnyListenerInside);
        }
        if (this.soundHorn != -1L && this.emitter != null) {
            this.emitter.set3D(this.soundHorn, !isAnyListenerInside);
        }
        if (this.soundSirenSignal != -1L && this.emitter != null) {
            this.emitter.set3D(this.soundSirenSignal, !isAnyListenerInside);
        }
        this.updateDoorAlarmSound();
        this.updateHandBrakeSound();
        if (!(this.emitter == null || this.engineState == engineStateTypes.Idle && this.emitter.isEmpty())) {
            this.getFMODParameters().update();
            this.emitter.setPos(this.getX(), this.getY(), this.getZ());
            this.emitter.tick();
        }
    }

    private void updateDoorAlarmSound() {
        if (this.emitter == null) {
            return;
        }
        boolean bShouldPlay = false;
        if (this.isEngineRunning()) {
            for (int i = 0; i < this.getMaxPassengers(); ++i) {
                VehiclePart doorPart = this.getPassengerDoor(i);
                if (doorPart == null || doorPart.isInventoryItemUninstalled() || !doorPart.getDoor().isOpen()) continue;
                bShouldPlay = true;
                break;
            }
        }
        if (bShouldPlay) {
            if (!this.emitter.isPlaying(this.doorAlarmSound)) {
                this.doorAlarmSound = this.emitter.playSoundImpl("VehicleDoorAlarm", (IsoObject)null);
            }
        } else if (this.emitter.isPlaying(this.doorAlarmSound)) {
            this.emitter.stopSound(this.doorAlarmSound);
            this.doorAlarmSound = 0L;
        }
    }

    private void updateHandBrakeSound() {
        if (this.emitter == null) {
            return;
        }
        String soundName = this.getHandBrakeSound();
        boolean bShouldPlay = false;
        if (soundName != null && this.isEngineRunning() && this.getDriver() != null && this.getController() != null && this.getController().clientControls.brake) {
            bShouldPlay = true;
        }
        if (bShouldPlay) {
            if (!this.handBrakeActive) {
                this.handBrakeActive = true;
                if (!this.emitter.isPlaying(this.handBrakeSound)) {
                    this.handBrakeSound = this.emitter.playSoundImpl(soundName, (IsoObject)null);
                }
            }
        } else if (this.handBrakeActive) {
            this.handBrakeActive = false;
            if (this.emitter.isPlaying(this.handBrakeSound)) {
                this.emitter.stopOrTriggerSound(this.handBrakeSound);
            }
            this.handBrakeSound = 0L;
        }
    }

    private boolean updatePart(VehiclePart part) {
        String functionName;
        part.updateSignalDevice();
        VehicleLight light = part.getLight();
        if (light != null && part.getId().contains("Headlight")) {
            part.setLightActive(this.getHeadlightsOn() && part.getInventoryItem() != null && this.getBatteryCharge() > 0.0f);
        }
        if ((functionName = part.getLuaFunction("update")) == null) {
            return false;
        }
        float worldAgeHours = (float)GameTime.getInstance().getWorldAgeHours();
        if (part.getLastUpdated() < 0.0f) {
            part.setLastUpdated(worldAgeHours);
        } else if (part.getLastUpdated() > worldAgeHours) {
            part.setLastUpdated(worldAgeHours);
        }
        float elapsedHours = worldAgeHours - part.getLastUpdated();
        if ((int)(elapsedHours * 60.0f) > 0) {
            part.setLastUpdated(worldAgeHours);
            this.callLuaVoid(functionName, this, part, elapsedHours * 60.0f);
            return true;
        }
        return false;
    }

    public void updateParts() {
        if (GameClient.client) {
            for (int i = 0; i < this.getPartCount(); ++i) {
                VehiclePart part = this.getPartByIndex(i);
                part.updateSignalDevice();
            }
            return;
        }
        boolean didUpdate = false;
        for (int i = 0; i < this.getPartCount(); ++i) {
            VehiclePart part = this.getPartByIndex(i);
            if (this.updatePart(part) && !didUpdate) {
                didUpdate = true;
            }
            if (i != this.getPartCount() - 1 || !didUpdate) continue;
            this.brakeBetweenUpdatesSpeed = 0.0f;
        }
    }

    public void drainBatteryUpdateHack() {
        boolean engineRunning = this.isEngineRunning();
        if (engineRunning) {
            return;
        }
        for (int i = 0; i < this.parts.size(); ++i) {
            VehiclePart part = this.parts.get(i);
            if (part.getDeviceData() != null && part.getDeviceData().getIsTurnedOn()) {
                this.updatePart(part);
                continue;
            }
            if (part.getLight() == null || !part.getLight().getActive()) continue;
            this.updatePart(part);
        }
        if (this.hasLightbar() && (this.lightbarLightsMode.isEnable() || this.lightbarSirenMode.isEnable()) && this.getBattery() != null) {
            this.updatePart(this.getBattery());
        }
    }

    public boolean getHeadlightsOn() {
        return this.headlightsOn;
    }

    public void setHeadlightsOn(boolean on) {
        if (this.headlightsOn == on) {
            return;
        }
        this.headlightsOn = on;
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 8);
        } else {
            this.playSound(this.headlightsOn ? "VehicleHeadlightsOn" : "VehicleHeadlightsOff");
        }
    }

    public boolean getWindowLightsOn() {
        return this.windowLightsOn;
    }

    public void setWindowLightsOn(boolean on) {
        this.windowLightsOn = on;
    }

    public boolean getHeadlightCanEmmitLight() {
        if (this.getBatteryCharge() <= 0.0f) {
            return false;
        }
        VehiclePart part = this.getPartById("HeadlightLeft");
        if (part != null && part.getInventoryItem() != null) {
            return true;
        }
        part = this.getPartById("HeadlightRight");
        return part != null && part.getInventoryItem() != null;
    }

    public boolean getStoplightsOn() {
        return this.stoplightsOn;
    }

    public void setStoplightsOn(boolean on) {
        if (this.stoplightsOn == on) {
            return;
        }
        this.stoplightsOn = on;
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 8);
        }
    }

    public boolean hasHeadlights() {
        return this.getLightCount() > 0;
    }

    @Override
    public void addToWorld() {
        this.addToWorld(false);
    }

    public void addToWorld(boolean crashed) {
        if (this.addedToWorld) {
            DebugLog.Vehicle.error("added vehicle twice " + String.valueOf(this) + " id=" + this.vehicleId);
            return;
        }
        if (Core.debug) {
            this.setDebugPhysicsRender(true);
        }
        VehiclesDB2.instance.setVehicleLoaded(this);
        this.addedToWorld = true;
        this.removedFromWorld = false;
        super.addToWorld();
        this.createPhysics();
        for (int i = 0; i < this.parts.size(); ++i) {
            VehiclePart part = this.parts.get(i);
            if (part.getItemContainer() != null) {
                part.getItemContainer().addItemsToProcessItems();
            }
            if (part.getDeviceData() == null) continue;
            ZomboidRadio.getInstance().RegisterDevice(part);
        }
        if (this.lightbarSirenMode.isEnable()) {
            this.setLightbarSirenMode(this.lightbarSirenMode.get());
            if (this.sirenStartTime <= 0.0) {
                this.sirenStartTime = GameTime.instance.getWorldAgeHours();
            }
        }
        if (this.chunk != null && this.chunk.jobType != IsoChunk.JobType.SoftReset) {
            if (PathfindNative.useNativeCode) {
                PathfindNative.instance.addVehicle(this);
            } else {
                PolygonalMap2.instance.addVehicleToWorld(this);
            }
        }
        if (this.engineState != engineStateTypes.Idle) {
            double d = this.engineSpeed = this.getScript() == null ? 1000.0 : (double)this.getScript().getEngineIdleSpeed();
        }
        if (!(this.chunk == null || this.chunk.jobType == IsoChunk.JobType.SoftReset || this.isPreviouslyEntered() || this.isPreviouslyMoved() || this.isHotwired() || this.isBurnt())) {
            try {
                this.trySpawnKey(crashed);
            }
            catch (Exception ex) {
                ExceptionLogger.logException(ex);
            }
        }
        if (this.emitter != null) {
            SoundManager.instance.registerEmitter(this.emitter);
        }
    }

    @Override
    public void removeFromWorld() {
        int i;
        this.breakConstraint(false, false);
        VehiclesDB2.instance.setVehicleUnloaded(this);
        for (i = 0; i < this.passengers.length; ++i) {
            if (this.getPassenger((int)i).character == null) continue;
            for (int k = 0; k < 4; ++k) {
                if (this.getPassenger((int)i).character != IsoPlayer.players[k]) continue;
                return;
            }
        }
        IsoChunk.removeFromCheckedVehicles(this);
        DebugLog.Vehicle.trace("BaseVehicle.removeFromWorld() %s id=%d", this, this.vehicleId);
        if (this.removedFromWorld) {
            return;
        }
        if (!this.addedToWorld) {
            DebugLog.Vehicle.debugln("ERROR: removing vehicle but addedToWorld=false %s id=%d", this, this.vehicleId);
        }
        if (Core.debug) {
            this.setDebugPhysicsRender(false);
        }
        this.removedFromWorld = true;
        this.addedToWorld = false;
        for (i = 0; i < this.parts.size(); ++i) {
            VehiclePart part = this.parts.get(i);
            if (part.getItemContainer() != null) {
                part.getItemContainer().removeItemsFromProcessItems();
            }
            if (part.getDeviceData() == null) continue;
            part.getDeviceData().cleanSoundsAndEmitter();
            ZomboidRadio.getInstance().UnRegisterDevice(part);
        }
        if (this.emitter != null) {
            this.emitter.stopAll();
            SoundManager.instance.unregisterEmitter(this.emitter);
            this.emitter = null;
        }
        if (this.hornEmitter != null && this.soundHorn != -1L) {
            this.hornEmitter.stopAll();
            this.hornEmitter = null;
            this.soundHorn = -1L;
        }
        if (this.alarmEmitter != null && this.soundAlarm != -1L) {
            this.alarmEmitter.stopAll();
            this.alarmEmitter = null;
            this.soundAlarm = -1L;
        }
        if (this.createdModel) {
            ModelManager.instance.Remove(this);
            this.createdModel = false;
        }
        this.releaseAnimationPlayers();
        if (this.getController() != null) {
            if (!GameServer.server) {
                Bullet.removeVehicle(this.vehicleId);
            }
            this.physics = null;
        }
        if (GameServer.server || GameClient.client) {
            VehicleManager.instance.removeFromWorld(this);
        } else if (this.vehicleId != -1) {
            VehicleIDMap.instance.remove(this.vehicleId);
        }
        IsoWorld.instance.currentCell.addVehicles.remove(this);
        IsoWorld.instance.currentCell.vehicles.remove(this);
        if (PathfindNative.useNativeCode) {
            PathfindNative.instance.removeVehicle(this);
        } else {
            PolygonalMap2.instance.removeVehicleFromWorld(this);
        }
        if (GameClient.client) {
            this.chunk.vehicles.remove(this);
        }
        this.surroundVehicle.reset();
        this.removeWorldLights();
        for (IsoAnimal a : this.animals) {
            a.delete();
        }
        super.removeFromWorld();
    }

    public void permanentlyRemove() {
        for (int i = 0; i < this.getMaxPassengers(); ++i) {
            IsoGameCharacter chr = this.getCharacter(i);
            if (chr == null) continue;
            if (GameServer.server) {
                chr.sendObjectChange(IsoObjectChange.EXIT_VEHICLE);
            }
            this.exit(chr);
        }
        this.breakConstraint(true, false);
        this.removeFromWorld();
        this.removeFromSquare();
        if (this.chunk != null) {
            this.chunk.vehicles.remove(this);
        }
        VehiclesDB2.instance.removeVehicle(this);
    }

    public VehiclePart getBattery() {
        return this.battery;
    }

    public void setEngineFeature(int quality, int loudness, int engineForce) {
        this.engineQuality = PZMath.clamp(quality, 0, 100);
        this.engineLoudness = (int)((float)loudness / 2.7f);
        this.enginePower = engineForce;
    }

    public int getEngineQuality() {
        return this.engineQuality;
    }

    public int getEngineLoudness() {
        return this.engineLoudness;
    }

    public int getEnginePower() {
        return this.enginePower;
    }

    public float getBatteryCharge() {
        VehiclePart battery = this.getBattery();
        if (battery != null && battery.getInventoryItem() instanceof DrainableComboItem) {
            return ((InventoryItem)battery.getInventoryItem()).getCurrentUsesFloat();
        }
        return 0.0f;
    }

    public int getPartCount() {
        return this.parts.size();
    }

    public VehiclePart getPartByIndex(int index) {
        if (index < 0 || index >= this.parts.size()) {
            return null;
        }
        return this.parts.get(index);
    }

    public VehiclePart getPartByPartId(zombie.scripting.objects.VehiclePart id) {
        if (id == null) {
            return null;
        }
        return this.getPartById(id.toString());
    }

    public VehiclePart getPartById(String id) {
        if (id == null) {
            return null;
        }
        for (int i = 0; i < this.parts.size(); ++i) {
            VehiclePart part = this.parts.get(i);
            VehicleScript.Part scriptPart = part.getScriptPart();
            if (scriptPart == null || !id.equals(scriptPart.id)) continue;
            return part;
        }
        return null;
    }

    public int getPartIndex(String id) {
        if (id == null) {
            return -1;
        }
        for (int i = 0; i < this.parts.size(); ++i) {
            VehicleScript.Part scriptPart = this.parts.get(i).getScriptPart();
            if (scriptPart == null || !id.equals(scriptPart.id)) continue;
            return i;
        }
        return -1;
    }

    public int getNumberOfPartsWithContainers() {
        if (this.getScript() == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < this.getScript().getPartCount(); ++i) {
            if (this.getScript().getPart((int)i).container == null) continue;
            ++count;
        }
        return count;
    }

    public VehiclePart getTrunkDoorPart() {
        return this.getPartByPartId(zombie.scripting.objects.VehiclePart.TRUNK_DOOR);
    }

    public VehiclePart getTrunkPart() {
        VehiclePart truckBedPart = this.getPartByPartId(zombie.scripting.objects.VehiclePart.TRUCK_BED);
        if (truckBedPart != null) {
            return truckBedPart;
        }
        return this.getPartByPartId(zombie.scripting.objects.VehiclePart.TRUCK_BED_OPEN);
    }

    public VehiclePart getTrailerTrunkPart() {
        return this.getPartByPartId(zombie.scripting.objects.VehiclePart.TRAILER_TRUNK);
    }

    public VehiclePart getPartForSeatContainer(int seat) {
        if (this.getScript() == null || seat < 0 || seat >= this.getMaxPassengers()) {
            return null;
        }
        for (int i = 0; i < this.getPartCount(); ++i) {
            VehiclePart part = this.getPartByIndex(i);
            if (part.getContainerSeatNumber() != seat) continue;
            return part;
        }
        return null;
    }

    public <T> PZArrayList<ItemContainer> getVehicleItemContainers(T paramToCompare, Invokers.Params2.Boolean.ICallback<T, ItemContainer> isValidPredicate) {
        PZArrayList<ItemContainer> containerList = new PZArrayList<ItemContainer>(ItemContainer.class, 10);
        return this.getVehicleItemContainers(paramToCompare, isValidPredicate, containerList);
    }

    public <T> PZArrayList<ItemContainer> getVehicleItemContainers(T paramToCompare, Invokers.Params2.Boolean.ICallback<T, ItemContainer> isValidPredicate, PZArrayList<ItemContainer> containerList) {
        for (int i = 0; i < this.parts.size(); ++i) {
            boolean canStore;
            VehiclePart part = this.parts.get(i);
            ItemContainer partContainer = part.getItemContainer();
            if (partContainer == null || !(canStore = isValidPredicate.accept(paramToCompare, partContainer))) continue;
            containerList.addUniqueReference(partContainer);
        }
        return containerList;
    }

    public void transmitPartCondition(VehiclePart part) {
        if (!GameServer.server) {
            return;
        }
        if (!this.parts.contains(part)) {
            return;
        }
        part.updateFlags = (short)(part.updateFlags | 0x800);
        this.updateFlags = (short)(this.updateFlags | 0x800);
    }

    public void transmitPartItem(VehiclePart part) {
        if (!GameServer.server) {
            return;
        }
        if (!this.parts.contains(part)) {
            return;
        }
        part.updateFlags = (short)(part.updateFlags | 0x80);
        this.updateFlags = (short)(this.updateFlags | 0x80);
    }

    public void transmitPartModData(VehiclePart part) {
        if (!GameServer.server) {
            return;
        }
        if (!this.parts.contains(part)) {
            return;
        }
        part.updateFlags = (short)(part.updateFlags | 0x10);
        this.updateFlags = (short)(this.updateFlags | 0x10);
    }

    public void transmitPartUsedDelta(VehiclePart part) {
        if (!GameServer.server) {
            return;
        }
        if (!this.parts.contains(part)) {
            return;
        }
        if (!(part.getInventoryItem() instanceof DrainableComboItem)) {
            return;
        }
        part.updateFlags = (short)(part.updateFlags | 0x20);
        this.updateFlags = (short)(this.updateFlags | 0x20);
    }

    public void transmitPartDoor(VehiclePart part) {
        if (!GameServer.server) {
            return;
        }
        if (!this.parts.contains(part)) {
            return;
        }
        if (part.getDoor() == null) {
            return;
        }
        part.updateFlags = (short)(part.updateFlags | 0x200);
        this.updateFlags = (short)(this.updateFlags | 0x200);
    }

    public void transmitPartWindow(VehiclePart part) {
        if (!GameServer.server) {
            return;
        }
        if (!this.parts.contains(part)) {
            return;
        }
        if (part.getWindow() == null) {
            return;
        }
        part.updateFlags = (short)(part.updateFlags | 0x100);
        this.updateFlags = (short)(this.updateFlags | 0x100);
    }

    public int getLightCount() {
        return this.lights.size();
    }

    public VehiclePart getLightByIndex(int index) {
        if (index < 0 || index >= this.lights.size()) {
            return null;
        }
        return this.lights.get(index);
    }

    public String getZone() {
        return this.respawnZone;
    }

    public void setZone(String name) {
        this.respawnZone = name;
    }

    public boolean isInArea(String areaId, IsoGameCharacter chr) {
        if (chr == null) {
            return false;
        }
        float chrX = chr.getX();
        float chrY = chr.getY();
        return this.isInArea(areaId, chrX, chrY);
    }

    private boolean isInArea(String areaId, float chrX, float chrY) {
        if (areaId == null || this.getScript() == null) {
            return false;
        }
        VehicleScript.Area area = this.getScript().getAreaById(areaId);
        if (area == null) {
            return false;
        }
        Vector2 vector2 = (Vector2)Vector2ObjectPool.get().alloc();
        Vector2 center = this.areaPositionLocal(area, vector2);
        if (center == null) {
            Vector2ObjectPool.get().release(vector2);
            return false;
        }
        Vector3f localPos = (Vector3f)TL_vector3f_pool.get().alloc();
        this.getLocalPos(chrX, chrY, this.getZ(), localPos);
        float minX = center.x - area.w / 2.0f;
        float minY = center.y - area.h / 2.0f;
        float maxX = center.x + area.w / 2.0f;
        float maxY = center.y + area.h / 2.0f;
        Vector2ObjectPool.get().release(vector2);
        boolean inside = localPos.x >= minX && localPos.x < maxX && localPos.z >= minY && localPos.z < maxY;
        TL_vector3f_pool.get().release(localPos);
        return inside;
    }

    public float getAreaDist(String areaId, float x, float y, float z) {
        if (areaId == null || this.getScript() == null) {
            return 999.0f;
        }
        VehicleScript.Area area = this.getScript().getAreaById(areaId);
        if (area != null) {
            Vector3f localPos = this.getLocalPos(x, y, z, (Vector3f)TL_vector3f_pool.get().alloc());
            float minX = Math.abs(area.x - area.w / 2.0f);
            float minY = Math.abs(area.y - area.h / 2.0f);
            float maxX = Math.abs(area.x + area.w / 2.0f);
            float maxY = Math.abs(area.y + area.h / 2.0f);
            float result = Math.abs(localPos.x + minX) + Math.abs(localPos.z + minY);
            TL_vector3f_pool.get().release(localPos);
            return result;
        }
        return 999.0f;
    }

    public float getAreaDist(String areaId, IsoGameCharacter chr) {
        if (areaId == null || this.getScript() == null) {
            return 999.0f;
        }
        VehicleScript.Area area = this.getScript().getAreaById(areaId);
        if (area != null) {
            Vector3f localPos = this.getLocalPos(chr.getX(), chr.getY(), this.getZ(), (Vector3f)TL_vector3f_pool.get().alloc());
            float minX = Math.abs(area.x - area.w / 2.0f);
            float minY = Math.abs(area.y - area.h / 2.0f);
            float result = Math.abs(localPos.x + minX) + Math.abs(localPos.z + minY);
            TL_vector3f_pool.get().release(localPos);
            return result;
        }
        return 999.0f;
    }

    public Vector2 getAreaCenter(String areaId) {
        return this.getAreaCenter(areaId, new Vector2());
    }

    public Vector2 getAreaCenter(String areaId, Vector2 out) {
        if (areaId == null || this.getScript() == null) {
            return null;
        }
        VehicleScript.Area area = this.getScript().getAreaById(areaId);
        if (area == null) {
            return null;
        }
        return this.areaPositionWorld(area, out);
    }

    public Vector2 getAreaFacingPosition(String areaId, Vector2 out) {
        Vector2 areaCenter = this.getAreaCenter(areaId, out);
        if (areaCenter == null) {
            return null;
        }
        float areaZ = this.getZ();
        return this.getFacingPosition(areaCenter.x, areaCenter.y, areaZ, out);
    }

    public boolean isInBounds(float worldX, float worldY) {
        return this.getPoly().containsPoint(worldX, worldY);
    }

    public boolean canAccessContainer(int partIndex, IsoGameCharacter chr) {
        VehiclePart part = this.getPartByIndex(partIndex);
        if (part == null) {
            return false;
        }
        VehicleScript.Part scriptPart = part.getScriptPart();
        if (scriptPart == null) {
            return false;
        }
        if (scriptPart.container == null) {
            return false;
        }
        if (part.isInventoryItemUninstalled() && scriptPart.container.capacity == 0) {
            return false;
        }
        if (scriptPart.container.luaTest == null || scriptPart.container.luaTest.isEmpty()) {
            return true;
        }
        return Boolean.TRUE.equals(this.callLuaBoolean(scriptPart.container.luaTest, this, part, chr));
    }

    public boolean canInstallPart(IsoGameCharacter chr, VehiclePart part) {
        if (!this.parts.contains(part)) {
            return false;
        }
        KahluaTable install = part.getTable("install");
        if (install == null || !(install.rawget("test") instanceof String)) {
            return false;
        }
        return Boolean.TRUE.equals(this.callLuaBoolean((String)install.rawget("test"), this, part, chr));
    }

    public boolean canUninstallPart(IsoGameCharacter chr, VehiclePart part) {
        if (!this.parts.contains(part)) {
            return false;
        }
        KahluaTable uninstall = part.getTable("uninstall");
        if (uninstall == null || !(uninstall.rawget("test") instanceof String)) {
            return false;
        }
        return Boolean.TRUE.equals(this.callLuaBoolean((String)uninstall.rawget("test"), this, part, chr));
    }

    private void callLuaVoid(String functionName, Object arg1, Object arg2) {
        Object functionObj = LuaManager.getFunctionObject(functionName);
        if (functionObj == null) {
            return;
        }
        LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObj, arg1, arg2);
    }

    private void callLuaVoid(String functionName, Object arg1) {
        Object functionObj = LuaManager.getFunctionObject(functionName);
        if (functionObj == null) {
            return;
        }
        LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObj, arg1);
    }

    private void callLuaVoid(String functionName, Object arg1, Object arg2, Object arg3) {
        Object functionObj = LuaManager.getFunctionObject(functionName);
        if (functionObj == null) {
            return;
        }
        LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObj, arg1, arg2, arg3);
    }

    private Boolean callLuaBoolean(String functionName, Object arg, Object arg2) {
        Object functionObj = LuaManager.getFunctionObject(functionName);
        if (functionObj == null) {
            return null;
        }
        return LuaManager.caller.protectedCallBoolean(LuaManager.thread, functionObj, arg, arg2);
    }

    private Boolean callLuaBoolean(String functionName, Object arg, Object arg2, Object arg3) {
        Object functionObj = LuaManager.getFunctionObject(functionName);
        if (functionObj == null) {
            return null;
        }
        return LuaManager.caller.protectedCallBoolean(LuaManager.thread, functionObj, arg, arg2, arg3);
    }

    public short getId() {
        return this.vehicleId;
    }

    public void setTireInflation(int wheelIndex, float inflation) {
        Bullet.setTireInflation(this.vehicleId, wheelIndex, inflation);
    }

    public void setTireRemoved(int wheelIndex, boolean removed) {
        if (!GameServer.server) {
            Bullet.setTireRemoved(this.vehicleId, wheelIndex, removed);
        }
    }

    public Vector3f chooseBestAttackPosition(IsoGameCharacter target, IsoGameCharacter attacker, Vector3f worldPos) {
        if (attacker instanceof IsoAnimal) {
            return null;
        }
        Vector2f v0 = BaseVehicle.allocVector2f();
        Vector2f v = target.getVehicle().getSurroundVehicle().getPositionForZombie((IsoZombie)attacker, v0);
        float vx = v0.x;
        float vy = v0.y;
        BaseVehicle.releaseVector2f(v0);
        if (v != null) {
            return worldPos.set(vx, vy, this.getZ());
        }
        return null;
    }

    public MinMaxPosition getMinMaxPosition() {
        MinMaxPosition res = new MinMaxPosition();
        float x = this.getX();
        float y = this.getY();
        Vector3f ext = this.getScript().getExtents();
        float l = ext.x;
        float w = ext.z;
        IsoDirections dir = this.getDir();
        switch (dir) {
            case E: 
            case W: {
                res.minX = x - l / 2.0f;
                res.maxX = x + l / 2.0f;
                res.minY = y - w / 2.0f;
                res.maxY = y + w / 2.0f;
                break;
            }
            case N: 
            case S: {
                res.minX = x - w / 2.0f;
                res.maxX = x + w / 2.0f;
                res.minY = y - l / 2.0f;
                res.maxY = y + l / 2.0f;
                break;
            }
            default: {
                return null;
            }
        }
        return res;
    }

    public String getVehicleType() {
        return this.type;
    }

    public void setVehicleType(String type) {
        this.type = type;
    }

    public float getMaxSpeed() {
        return this.maxSpeed;
    }

    public void setMaxSpeed(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public void lockServerUpdate(long lockTimeMs) {
        this.updateLockTimeout = System.currentTimeMillis() + lockTimeMs;
    }

    public void changeTransmission(TransmissionNumber newTransmission) {
        this.transmissionNumber = newTransmission;
    }

    public void tryHotwire(int electricityLevel) {
        int engineQuality = Math.max(100 - this.getEngineQuality(), 5);
        engineQuality = Math.min(engineQuality, 50);
        int skillModifier = electricityLevel * 4;
        int chance = engineQuality + skillModifier;
        boolean sync = false;
        String sound = "VehicleHotwireFail";
        if (Rand.Next(100) <= 11 - electricityLevel && this.alarmed) {
            this.triggerAlarm();
        }
        if (Rand.Next(100) <= chance) {
            this.setHotwired(true);
            sync = true;
            sound = "VehicleHotwireSuccess";
        } else if (Rand.Next(100) <= 10 - electricityLevel) {
            this.setHotwiredBroken(true);
            sync = true;
        }
        if (GameServer.server) {
            LuaManager.GlobalObject.playServerSound(sound, this.square);
        } else if (this.getDriver() != null) {
            this.getDriver().getEmitter().playSound(sound);
        }
        if (sync && GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 0x1000);
        }
    }

    public void cheatHotwire(boolean hotwired, boolean broken) {
        if (hotwired != this.hotwired || broken != this.hotwiredBroken) {
            this.hotwired = hotwired;
            this.hotwiredBroken = broken;
            if (GameServer.server) {
                this.updateFlags = (short)(this.updateFlags | 0x1000);
            }
        }
    }

    public boolean isKeyIsOnDoor() {
        return this.keyIsOnDoor;
    }

    public void setKeyIsOnDoor(boolean keyIsOnDoor) {
        this.keyIsOnDoor = keyIsOnDoor;
    }

    public boolean isHotwired() {
        return this.hotwired;
    }

    public void setHotwired(boolean hotwired) {
        this.hotwired = hotwired;
    }

    public boolean isHotwiredBroken() {
        return this.hotwiredBroken;
    }

    public void setHotwiredBroken(boolean hotwiredBroken) {
        this.hotwiredBroken = hotwiredBroken;
    }

    public IsoGameCharacter getDriver() {
        Passenger passenger = this.getPassenger(0);
        return passenger == null ? null : passenger.character;
    }

    public IsoGameCharacter getDriverRegardlessOfTow() {
        IsoGameCharacter driver = this.getDriver();
        if (driver != null) {
            return driver;
        }
        return this.vehicleTowedBy != null ? this.vehicleTowedBy.getDriver() : null;
    }

    public boolean isKeysInIgnition() {
        return !this.ignitionSwitch.getItems().isEmpty();
    }

    public void setKeysInIgnition(boolean keysOnContact) {
        IsoPlayer isoPlayer;
        IsoGameCharacter driver = this.getDriver();
        if (driver == null) {
            return;
        }
        this.setAlarmed(false);
        if (!(!GameClient.client || driver instanceof IsoPlayer && (isoPlayer = (IsoPlayer)driver).isLocalPlayer())) {
            return;
        }
        if (!this.isHotwired()) {
            InventoryItem key;
            if (!GameServer.server && keysOnContact && !this.isKeysInIgnition()) {
                key = this.getDriver().getInventory().haveThisKeyId(this.getKeyId());
                int containerID = -1;
                if (key != null) {
                    if (key.getContainer() != null) {
                        InventoryItem keyRing = key.getContainer().getContainingItem();
                        if (keyRing instanceof InventoryContainer && (keyRing.hasTag(ItemTag.KEY_RING) || "KeyRing".equals(keyRing.getType()))) {
                            key.getModData().rawset("keyRing", (Object)keyRing.getID());
                            containerID = key.getContainer().getContainingItem().getID();
                        } else if (key.hasModData()) {
                            key.getModData().rawset("keyRing", null);
                        }
                    }
                    this.keysInIgnition = keysOnContact;
                    if (GameClient.client) {
                        GameClient.instance.sendClientCommandV((IsoPlayer)this.getDriver(), "vehicle", "putKeyInIgnition", "key", key, "container", containerID);
                    }
                    if (key.getContainer() != null) {
                        key.getContainer().DoRemoveItem(key);
                    }
                    this.ignitionSwitch.addItem(key);
                    this.keysContainerId = containerID;
                }
            }
            if (!keysOnContact && this.isKeysInIgnition() && !GameServer.server) {
                if (this.currentKey == null) {
                    this.currentKey = this.createVehicleKey();
                }
                key = this.ignitionSwitch.getItems().get(0);
                ItemContainer container = this.getDriver().getInventory();
                if (this.keysContainerId != -1) {
                    InventoryContainer keyRingContainer = (InventoryContainer)this.getDriver().getInventory().getItemWithID(this.keysContainerId);
                    InventoryItem keyRingItem = this.getDriver().getInventory().getItemWithID(this.keysContainerId);
                    if (keyRingItem != null && keyRingContainer != null) {
                        container = keyRingContainer.getInventory();
                        key.getModData().rawset("keyRing", (Object)keyRingItem.getID());
                    }
                }
                container.addItem(key);
                this.ignitionSwitch.removeAllItems();
                this.setCurrentKey(null);
                this.keysInIgnition = keysOnContact;
                if (GameClient.client) {
                    GameClient.instance.sendClientCommand((IsoPlayer)this.getDriver(), "vehicle", "removeKeyFromIgnition", null);
                }
            }
        }
    }

    public void putKeyInIgnition(InventoryItem key, int containerID) {
        if (!GameServer.server) {
            return;
        }
        if (!(key instanceof Key)) {
            return;
        }
        if (this.isKeysInIgnition()) {
            return;
        }
        this.keysInIgnition = true;
        this.keyIsOnDoor = false;
        if (key != null) {
            ItemContainer container = this.getDriver().getInventory();
            if (containerID != 1) {
                InventoryContainer keyRingContainer = (InventoryContainer)this.getDriver().getInventory().getItemWithID(containerID);
                InventoryItem keyRingItem = this.getDriver().getInventory().getItemWithID(containerID);
                if (keyRingItem != null && keyRingContainer != null) {
                    container = keyRingContainer.getInventory();
                    key.getModData().rawset("keyRing", (Object)keyRingItem.getID());
                }
            } else {
                key.getModData().rawset("keyRing", null);
            }
            container.DoRemoveItem(this.getDriver().getInventory().haveThisKeyId(key.getKeyId()));
            this.ignitionSwitch.addItem(key);
            this.keysContainerId = containerID;
        }
        this.currentKey = key;
        this.updateFlags = (short)(this.updateFlags | 0x1000);
        VehicleManager.instance.serverUpdate();
    }

    public void removeKeyFromIgnition() {
        if (!GameServer.server) {
            return;
        }
        if (!this.isKeysInIgnition()) {
            return;
        }
        this.keysInIgnition = false;
        InventoryItem key = this.ignitionSwitch.getItems().get(0);
        ItemContainer container = this.getDriver().getInventory();
        if (this.keysContainerId != -1) {
            InventoryContainer keyRingContainer = (InventoryContainer)this.getDriver().getInventory().getItemWithID(this.keysContainerId);
            InventoryItem keyRingItem = this.getDriver().getInventory().getItemWithID(this.keysContainerId);
            if (keyRingItem != null && keyRingContainer != null) {
                container = keyRingContainer.getInventory();
                key.getModData().rawset("keyRing", (Object)keyRingItem.getID());
            }
        } else {
            key.getModData().rawset("keyRing", null);
        }
        container.addItem(key);
        this.ignitionSwitch.removeAllItems();
        this.keysContainerId = -1;
        this.currentKey = null;
        this.updateFlags = (short)(this.updateFlags | 0x1000);
        VehicleManager.instance.serverUpdate();
    }

    public void putKeyOnDoor(InventoryItem key) {
        if (!GameServer.server) {
            return;
        }
        if (!(key instanceof Key)) {
            return;
        }
        if (this.keyIsOnDoor) {
            return;
        }
        this.keyIsOnDoor = true;
        this.keysInIgnition = false;
        this.currentKey = key;
        this.updateFlags = (short)(this.updateFlags | 0x1000);
    }

    public void removeKeyFromDoor() {
        if (!GameServer.server) {
            return;
        }
        if (!this.keyIsOnDoor) {
            return;
        }
        this.keyIsOnDoor = false;
        this.currentKey = null;
        this.updateFlags = (short)(this.updateFlags | 0x1000);
    }

    public void syncKeyInIgnition(boolean inIgnition, boolean onDoor, InventoryItem key) {
        if (!GameClient.client) {
            return;
        }
        this.keysInIgnition = inIgnition;
        this.keyIsOnDoor = onDoor;
        this.currentKey = key;
    }

    private void randomizeContainers() {
        int i;
        if (GameClient.client) {
            return;
        }
        boolean doSpecific = true;
        String scriptType = this.getScriptName().substring(this.getScriptName().indexOf(46) + 1);
        ItemPickerJava.VehicleDistribution distrib = ItemPickerJava.VehicleDistributions.get(scriptType + this.getSkinIndex());
        if (distrib != null) {
            doSpecific = false;
        } else {
            distrib = ItemPickerJava.VehicleDistributions.get(scriptType);
        }
        if (distrib == null) {
            for (int i2 = 0; i2 < this.parts.size(); ++i2) {
                VehiclePart part = this.parts.get(i2);
                if (part.getItemContainer() == null) continue;
                if (Core.debug) {
                    DebugLog.Vehicle.debugln("VEHICLE MISSING CONT DISTRIBUTION: " + scriptType);
                }
                return;
            }
            return;
        }
        int specialLootChance = 8;
        if (this.getScript() == null) {
            this.setScript();
        }
        if (this.getScript().getSpecialLootChance() > 0) {
            specialLootChance = this.getScript().getSpecialLootChance();
        }
        ItemPickerJava.ItemPickerRoom contDistrib = doSpecific && Rand.Next(100) <= specialLootChance && !distrib.specific.isEmpty() ? PZArrayUtil.pickRandom(distrib.specific) : distrib.normal;
        if (!StringUtils.isNullOrWhitespace(this.specificDistributionId)) {
            for (i = 0; i < distrib.specific.size(); ++i) {
                ItemPickerJava.ItemPickerRoom room = distrib.specific.get(i);
                if (!this.specificDistributionId.equals(room.specificId)) continue;
                contDistrib = room;
                break;
            }
        }
        for (i = 0; i < this.parts.size(); ++i) {
            VehiclePart part = this.parts.get(i);
            if (part.getItemContainer() == null) continue;
            if (GameServer.server && GameServer.softReset) {
                part.getItemContainer().setExplored(false);
            }
            if (part.getItemContainer().explored) continue;
            part.getItemContainer().clear();
            if (Rand.Next(100) <= 100) {
                this.randomizeContainer(part, contDistrib);
            }
            part.getItemContainer().setExplored(true);
        }
    }

    private void randomizeContainers(ItemPickerJava.ItemPickerRoom contDistrib) {
        for (int i = 0; i < this.parts.size(); ++i) {
            VehiclePart part = this.parts.get(i);
            if (part.getItemContainer() == null) continue;
            if (GameServer.server && GameServer.softReset) {
                part.getItemContainer().setExplored(false);
            }
            if (part.getItemContainer().explored) continue;
            part.getItemContainer().clear();
            if (Rand.Next(100) <= 100) {
                this.randomizeContainer(part, contDistrib);
            }
            part.getItemContainer().setExplored(true);
        }
    }

    private void randomizeContainer(VehiclePart part, ItemPickerJava.ItemPickerRoom contDistrib) {
        if (GameClient.client) {
            return;
        }
        if (contDistrib == null) {
            return;
        }
        if (!part.getId().contains("Seat") && !contDistrib.containers.containsKey(part.getId())) {
            DebugLog.Vehicle.debugln("NO CONT DISTRIB FOR PART: " + part.getId() + " CAR: " + this.getScriptName().replaceFirst("Base.", ""));
        }
        ItemPickerJava.fillContainerType(contDistrib, part.getItemContainer(), "", null);
        String scriptType = this.getScriptName().substring(this.getScriptName().indexOf(46) + 1);
        LuaEventManager.triggerEvent("OnFillContainer", scriptType, part.getItemContainer().getType(), part.getItemContainer());
    }

    public boolean hasAlarm() {
        return this.script.getSounds().alarmEnable;
    }

    public boolean hasHorn() {
        return this.script.getSounds().hornEnable;
    }

    public boolean hasLightbar() {
        return this.script.getLightbar().enable;
    }

    public void setChosenAlarmSound(String soundName) {
        this.chosenAlarmSound = StringUtils.discardNullOrWhitespace(soundName);
    }

    public void chooseAlarmSound() {
        if (this.script == null) {
            return;
        }
        VehicleScript.Sounds sounds = this.script.getSounds();
        if (!sounds.alarmEnable.booleanValue()) {
            return;
        }
        if (this.chosenAlarmSound == null || !sounds.alarm.contains(this.chosenAlarmSound) && !sounds.alarmLoop.contains(this.chosenAlarmSound)) {
            ArrayList<String> choices = new ArrayList<String>();
            choices.addAll(sounds.alarm);
            choices.addAll(sounds.alarmLoop);
            this.chosenAlarmSound = (String)PZArrayUtil.pickRandom(choices);
        }
    }

    public void onAlarmStart() {
        this.soundAlarmOn = true;
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 0x400);
            if (this.script.getSounds().alarmEnable.booleanValue()) {
                WorldSoundManager.instance.addSound(this, this.getXi(), this.getYi(), this.getZi(), 150, 150, false, 0.0f, 1.0f, false, true, false, false, true);
            }
            return;
        }
        if (this.soundAlarm != -1L) {
            this.alarmEmitter.stopSound(this.soundAlarm);
        }
        if (this.script.getSounds().alarmEnable.booleanValue()) {
            IsoGameCharacter driver;
            IsoPlayer player;
            this.alarmEmitter = IsoWorld.instance.getFreeEmitter(this.getX(), this.getY(), this.getZi());
            this.chooseAlarmSound();
            this.soundAlarm = this.alarmEmitter.playSoundImpl(this.chosenAlarmSound, (IsoObject)null);
            this.alarmEmitter.set3D(this.soundAlarm, !this.isAnyListenerInside());
            this.alarmEmitter.setVolume(this.soundAlarm, 1.0f);
            this.alarmEmitter.setPitch(this.soundAlarm, 1.0f);
            if (!GameClient.client) {
                WorldSoundManager.instance.addSound(this, this.getXi(), this.getYi(), this.getZi(), 150, 150, false, 0.0f, 1.0f, false, true, false, false, true);
            }
            if ((player = Type.tryCastTo(driver = this.getDriver(), IsoPlayer.class)) != null && player.isLocalPlayer()) {
                player.triggerMusicIntensityEvent("VehicleHorn");
            }
        }
    }

    public void onAlarmStop() {
        this.soundAlarmOn = false;
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 0x400);
            return;
        }
        if (this.script.getSounds().alarmEnable.booleanValue() && this.soundAlarm != -1L) {
            this.alarmEmitter.stopSound(this.soundAlarm);
            this.soundAlarm = -1L;
        }
    }

    public void onHornStart() {
        this.soundHornOn = true;
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 0x400);
            if (this.script.getSounds().hornEnable) {
                WorldSoundManager.instance.addSound(this, this.getXi(), this.getYi(), this.getZi(), 150, 150, false, 0.0f, 1.0f, false, true, false, false, true);
            }
            return;
        }
        if (this.soundHorn != -1L) {
            this.hornEmitter.stopSound(this.soundHorn);
        }
        if (this.script.getSounds().hornEnable) {
            IsoGameCharacter driver;
            IsoPlayer player;
            this.hornEmitter = IsoWorld.instance.getFreeEmitter(this.getX(), this.getY(), this.getZi());
            this.soundHorn = this.hornEmitter.playSoundLoopedImpl(this.script.getSounds().horn);
            this.hornEmitter.set3D(this.soundHorn, !this.isAnyListenerInside());
            this.hornEmitter.setVolume(this.soundHorn, 1.0f);
            this.hornEmitter.setPitch(this.soundHorn, 1.0f);
            if (!GameClient.client) {
                WorldSoundManager.instance.addSound(this, this.getXi(), this.getYi(), this.getZi(), 150, 150, false, 0.0f, 1.0f, false, true, false, false, true);
            }
            if ((player = Type.tryCastTo(driver = this.getDriver(), IsoPlayer.class)) != null && player.isLocalPlayer()) {
                player.triggerMusicIntensityEvent("VehicleHorn");
            }
        }
    }

    public void onHornStop() {
        this.soundHornOn = false;
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 0x400);
            return;
        }
        if (this.script.getSounds().hornEnable && this.soundHorn != -1L) {
            this.hornEmitter.stopSound(this.soundHorn);
            this.soundHorn = -1L;
        }
    }

    public boolean hasBackSignal() {
        return this.script != null && this.script.getSounds().backSignalEnable;
    }

    public boolean isBackSignalEmitting() {
        return this.soundBackMoveSignal != -1L;
    }

    public void onBackMoveSignalStart() {
        this.soundBackMoveOn = true;
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 0x400);
            return;
        }
        if (this.soundBackMoveSignal != -1L) {
            this.emitter.stopSound(this.soundBackMoveSignal);
        }
        if (this.script.getSounds().backSignalEnable) {
            this.soundBackMoveSignal = this.emitter.playSoundLoopedImpl(this.script.getSounds().backSignal);
            this.emitter.set3D(this.soundBackMoveSignal, !this.isAnyListenerInside());
        }
    }

    public void onBackMoveSignalStop() {
        this.soundBackMoveOn = false;
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 0x400);
            return;
        }
        if (this.script.getSounds().backSignalEnable && this.soundBackMoveSignal != -1L) {
            this.emitter.stopSound(this.soundBackMoveSignal);
            this.soundBackMoveSignal = -1L;
        }
    }

    public int getLightbarLightsMode() {
        return this.lightbarLightsMode.get();
    }

    public void setLightbarLightsMode(int mode) {
        this.lightbarLightsMode.set(mode);
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 0x400);
        }
    }

    public int getLightbarSirenMode() {
        return this.lightbarSirenMode.get();
    }

    public void setLightbarSirenMode(int mode) {
        if (this.soundSirenSignal != -1L) {
            this.getEmitter().stopSound(this.soundSirenSignal);
            this.soundSirenSignal = -1L;
        }
        this.lightbarSirenMode.set(mode);
        if (GameServer.server) {
            this.updateFlags = (short)(this.updateFlags | 0x400);
            return;
        }
        if (this.lightbarSirenMode.isEnable() && this.getBatteryCharge() > 0.0f) {
            this.soundSirenSignal = this.getEmitter().playSoundLoopedImpl(this.lightbarSirenMode.getSoundName(this.script.getLightbar()));
            this.getEmitter().set3D(this.soundSirenSignal, !this.isAnyListenerInside());
        }
    }

    public HashMap<String, String> getChoosenParts() {
        return this.choosenParts;
    }

    public float getMass() {
        float tempMass = this.mass;
        if (tempMass < 0.0f) {
            tempMass = 1.0f;
        }
        return tempMass;
    }

    public void setMass(float mass) {
        this.mass = mass;
    }

    public float getInitialMass() {
        return this.initialMass;
    }

    public void setInitialMass(float initialMass) {
        this.initialMass = initialMass;
    }

    public void updateTotalMass() {
        float plusMass = 0.0f;
        for (int i = 0; i < this.parts.size(); ++i) {
            VehiclePart part = this.parts.get(i);
            if (part.getItemContainer() != null) {
                plusMass += part.getItemContainer().getCapacityWeight();
            }
            if (part.getInventoryItem() == null) continue;
            plusMass += ((InventoryItem)part.getInventoryItem()).getWeight();
        }
        this.setMass(Math.round(this.getInitialMass() + plusMass));
        if (this.physics != null && !GameServer.server) {
            Bullet.setVehicleMass(this.vehicleId, this.getMass());
        }
    }

    public float getBrakingForce() {
        return this.brakingForce;
    }

    public void setBrakingForce(float brakingForce) {
        this.brakingForce = brakingForce;
    }

    public float getBaseQuality() {
        return this.baseQuality;
    }

    public void setBaseQuality(float baseQuality) {
        this.baseQuality = baseQuality;
    }

    public float getCurrentSteering() {
        return this.currentSteering;
    }

    public void setCurrentSteering(float currentSteering) {
        this.currentSteering = currentSteering;
    }

    public boolean isDoingOffroad() {
        if (this.getCurrentSquare() == null) {
            return false;
        }
        IsoObject floor = this.getCurrentSquare().getFloor();
        if (floor == null || floor.getSprite() == null) {
            return false;
        }
        String spriteName = floor.getSprite().getName();
        if (spriteName == null) {
            return false;
        }
        return !spriteName.contains("carpentry_02") && !spriteName.contains("blends_street") && !spriteName.contains("floors_exterior_street");
    }

    public boolean isBraking() {
        return this.isBraking;
    }

    public void setBraking(boolean isBraking) {
        this.isBraking = isBraking;
        if (isBraking && this.brakeBetweenUpdatesSpeed == 0.0f) {
            this.brakeBetweenUpdatesSpeed = this.getCurrentAbsoluteSpeedKmHour();
        }
    }

    public void updatePartStats() {
        this.setBrakingForce(0.0f);
        this.engineLoudness = (int)((double)this.getScript().getEngineLoudness() * SandboxOptions.instance.zombieAttractionMultiplier.getValue() / 2.0);
        boolean foundMuffler = false;
        for (int i = 0; i < this.getPartCount(); ++i) {
            VehiclePart part = this.getPartByIndex(i);
            if (part.getInventoryItem() == null) continue;
            if (((InventoryItem)part.getInventoryItem()).getBrakeForce() > 0.0f) {
                float newBrakeForce = VehiclePart.getNumberByCondition(((InventoryItem)part.getInventoryItem()).getBrakeForce(), ((InventoryItem)part.getInventoryItem()).getCondition(), 5.0f);
                newBrakeForce += newBrakeForce / 50.0f * (float)part.getMechanicSkillInstaller();
                this.setBrakingForce(this.getBrakingForce() + newBrakeForce);
            }
            if (((InventoryItem)part.getInventoryItem()).getWheelFriction() > 0.0f) {
                part.setWheelFriction(0.0f);
                float friction = VehiclePart.getNumberByCondition(((InventoryItem)part.getInventoryItem()).getWheelFriction(), ((InventoryItem)part.getInventoryItem()).getCondition(), 0.2f);
                friction += 0.1f * (float)part.getMechanicSkillInstaller();
                friction = Math.min(2.3f, friction);
                part.setWheelFriction(friction);
            }
            if (((InventoryItem)part.getInventoryItem()).getSuspensionCompression() > 0.0f) {
                part.setSuspensionCompression(VehiclePart.getNumberByCondition(((InventoryItem)part.getInventoryItem()).getSuspensionCompression(), ((InventoryItem)part.getInventoryItem()).getCondition(), 0.6f));
                part.setSuspensionDamping(VehiclePart.getNumberByCondition(((InventoryItem)part.getInventoryItem()).getSuspensionDamping(), ((InventoryItem)part.getInventoryItem()).getCondition(), 0.6f));
            }
            if (((InventoryItem)part.getInventoryItem()).getEngineLoudness() > 0.0f) {
                part.setEngineLoudness(VehiclePart.getNumberByCondition(((InventoryItem)part.getInventoryItem()).getEngineLoudness(), ((InventoryItem)part.getInventoryItem()).getCondition(), 10.0f));
                this.engineLoudness = (int)((float)this.engineLoudness * (1.0f + (100.0f - part.getEngineLoudness()) / 100.0f));
                foundMuffler = true;
            }
            if (!(((InventoryItem)part.getInventoryItem()).getDurability() > 0.0f)) continue;
            part.setDurability(((InventoryItem)part.getInventoryItem()).getDurability());
        }
        if (!foundMuffler) {
            this.engineLoudness *= 2;
        }
    }

    public void transmitEngine() {
        if (!GameServer.server) {
            return;
        }
        this.updateFlags = (short)(this.updateFlags | 4);
    }

    public void setRust(float rust) {
        this.rust = PZMath.clamp(rust, 0.0f, 1.0f);
    }

    public float getRust() {
        return this.rust;
    }

    public void transmitRust() {
        if (!GameServer.server) {
            return;
        }
        this.updateFlags = (short)(this.updateFlags | 0x1000);
    }

    public void transmitColorHSV() {
        if (!GameServer.server) {
            return;
        }
        this.updateFlags = (short)(this.updateFlags | 0x1000);
    }

    public void transmitSkinIndex() {
        if (!GameServer.server) {
            return;
        }
        this.updateFlags = (short)(this.updateFlags | 0x1000);
    }

    public void updateBulletStats() {
        double susp;
        if (this.getScriptName().contains("Burnt") || !WorldSimulation.instance.created) {
            return;
        }
        float[] data = wheelParams;
        double period = 2.4;
        int chanceofbump = 100;
        float frictionMul = 1.0f;
        float currentAbsoluteSpeedKmHour = this.getCurrentAbsoluteSpeedKmHour();
        if (this.isInForest() && this.isDoingOffroad() && currentAbsoluteSpeedKmHour > 1.0f) {
            susp = Rand.Next(0.04f, 0.13f);
            frictionMul = 0.7f;
            chanceofbump = 15;
        } else if (this.isDoingOffroad() && currentAbsoluteSpeedKmHour > 1.0f) {
            chanceofbump = 25;
            susp = Rand.Next(0.02f, 0.1f);
            frictionMul = 0.7f;
        } else {
            susp = currentAbsoluteSpeedKmHour > 1.0f && Rand.Next(100) < 10 ? (double)Rand.Next(0.01f, 0.05f) : 0.0;
        }
        if (RainManager.isRaining().booleanValue()) {
            frictionMul -= 0.3f;
        }
        Vector3f worldPos = (Vector3f)TL_vector3f_pool.get().alloc();
        for (int i = 0; i < this.script.getWheelCount(); ++i) {
            this.updateBulletStatsWheel(i, data, worldPos, frictionMul, chanceofbump, 2.4, susp);
        }
        TL_vector3f_pool.get().release(worldPos);
        if (SystemDisabler.getdoVehicleLowRider() && this.isKeyboardControlled()) {
            float lowRiderLevel = 0.25f;
            float lowRiderK = 1.0f;
            lowRiderParam[0] = GameKeyboard.isKeyDown(79) ? lowRiderParam[0] + (0.25f - lowRiderParam[0]) * 1.0f : lowRiderParam[0] + (0.0f - lowRiderParam[0]) * 0.05f;
            lowRiderParam[1] = GameKeyboard.isKeyDown(80) ? lowRiderParam[1] + (0.25f - lowRiderParam[1]) * 1.0f : lowRiderParam[1] + (0.0f - lowRiderParam[1]) * 0.05f;
            lowRiderParam[2] = GameKeyboard.isKeyDown(75) ? lowRiderParam[2] + (0.25f - lowRiderParam[2]) * 1.0f : lowRiderParam[2] + (0.0f - lowRiderParam[2]) * 0.05f;
            lowRiderParam[3] = GameKeyboard.isKeyDown(76) ? lowRiderParam[3] + (0.25f - lowRiderParam[3]) * 1.0f : lowRiderParam[3] + (0.0f - lowRiderParam[3]) * 0.05f;
            data[5] = lowRiderParam[0];
            data[11] = lowRiderParam[1];
            data[17] = lowRiderParam[2];
            data[23] = lowRiderParam[3];
        }
        Bullet.setVehicleParams(this.vehicleId, data);
    }

    private void updateBulletStatsWheel(int wheelIndex, float[] data, Vector3f worldPos, float frictionMul, int chanceofbump, double period, double susp) {
        int offset = wheelIndex * 6;
        VehicleScript.Wheel scriptWheel = this.script.getWheel(wheelIndex);
        Vector3f wheelPos = this.getWorldPos(scriptWheel.offset.x, scriptWheel.offset.y, scriptWheel.offset.z, worldPos);
        VehiclePart part = this.getPartById("Tire" + scriptWheel.getId());
        VehiclePart partSuspension = this.getPartById("Suspension" + scriptWheel.getId());
        if (part != null && part.getInventoryItem() != null) {
            data[offset + 0] = 1.0f;
            data[offset + 1] = Math.min(part.getContainerContentAmount() / (float)(part.getContainerCapacity() - 10), 1.0f);
            data[offset + 2] = frictionMul * part.getWheelFriction();
            if (partSuspension != null && partSuspension.getInventoryItem() != null) {
                data[offset + 3] = partSuspension.getSuspensionDamping();
                data[offset + 4] = partSuspension.getSuspensionCompression();
            } else {
                data[offset + 3] = 0.1f;
                data[offset + 4] = 0.1f;
            }
            data[offset + 5] = chanceofbump > 0 && Rand.Next(chanceofbump) == 0 ? (float)(Math.sin(period * (double)wheelPos.x()) * Math.sin(period * (double)wheelPos.y()) * susp) : 0.0f;
        } else {
            data[offset + 0] = 0.0f;
            data[offset + 1] = 30.0f;
            data[offset + 2] = 0.0f;
            data[offset + 3] = 2.88f;
            data[offset + 4] = 3.83f;
            data[offset + 5] = Rand.Next(chanceofbump) == 0 ? (float)(Math.sin(period * (double)wheelPos.x()) * Math.sin(period * (double)wheelPos.y()) * susp) : 0.0f;
        }
    }

    public void setActiveInBullet(boolean active) {
    }

    public boolean areAllDoorsLocked() {
        for (int seat = 0; seat < this.getMaxPassengers(); ++seat) {
            VehiclePart part = this.getPassengerDoor(seat);
            if (part == null || part.getDoor() == null || part.getDoor().isLocked()) continue;
            return false;
        }
        return true;
    }

    public boolean isAnyDoorLocked() {
        for (int seat = 0; seat < this.getMaxPassengers(); ++seat) {
            VehiclePart part = this.getPassengerDoor(seat);
            if (part == null || part.getDoor() == null || !part.getDoor().isLocked()) continue;
            return true;
        }
        return false;
    }

    public float getRemainingFuelPercentage() {
        VehiclePart gasTank = this.getPartById("GasTank");
        if (gasTank == null) {
            return 0.0f;
        }
        return gasTank.getContainerContentAmount() / (float)gasTank.getContainerCapacity() * 100.0f;
    }

    public int getMechanicalID() {
        return this.mechanicalId;
    }

    public void setMechanicalID(int mechanicalId) {
        this.mechanicalId = mechanicalId;
    }

    public boolean needPartsUpdate() {
        return this.needPartsUpdate;
    }

    public void setNeedPartsUpdate(boolean needPartsUpdate) {
        this.needPartsUpdate = needPartsUpdate;
    }

    public VehiclePart getHeater() {
        return this.getPartById("Heater");
    }

    public int windowsOpen() {
        int result = 0;
        for (int i = 0; i < this.getPartCount(); ++i) {
            VehiclePart part = this.getPartByIndex(i);
            if (part.window == null || !part.window.open) continue;
            ++result;
        }
        return result;
    }

    public boolean isAlarmed() {
        return this.alarmed;
    }

    public void setAlarmed(boolean alarmed) {
        this.alarmed = alarmed;
        if (alarmed) {
            this.setPreviouslyEntered(false);
        }
    }

    public void triggerAlarm() {
        if (!this.alarmed || this.previouslyEntered) {
            this.alarmed = false;
            return;
        }
        this.alarmed = false;
        this.alarmStartTime = GameTime.getInstance().getWorldAgeHours();
        this.alarmAccumulator = 0.0f;
        this.chooseAlarmSound();
        VehicleScript.Sounds sounds = this.script.getSounds();
        boolean bLooping = sounds.alarmLoop.contains(this.chosenAlarmSound);
        if (bLooping) {
            this.onAlarmStart();
        }
    }

    private void doAlarm() {
        if (this.alarmStartTime > 0.0) {
            if (this.getBatteryCharge() <= 0.0f) {
                if (this.soundAlarmOn) {
                    this.onAlarmStop();
                }
                this.alarmStartTime = 0.0;
                return;
            }
            double worldAge = GameTime.getInstance().getWorldAgeHours();
            if (this.alarmStartTime > worldAge) {
                this.alarmStartTime = worldAge;
            }
            if (worldAge >= this.alarmStartTime + 0.66 * (double)GameTime.getInstance().getDeltaMinutesPerDay()) {
                this.onAlarmStop();
                this.setHeadlightsOn(false);
                this.alarmStartTime = 0.0;
                return;
            }
            this.chooseAlarmSound();
            VehicleScript.Sounds sounds = this.script.getSounds();
            boolean bLooping = sounds.alarmLoop.contains(this.chosenAlarmSound);
            if (bLooping && (this.alarmEmitter == null || !this.alarmEmitter.isPlaying(this.soundAlarm))) {
                this.onAlarmStart();
            }
            this.alarmAccumulator += GameTime.instance.getThirtyFPSMultiplier();
            int t = (int)this.alarmAccumulator / 24;
            if (!this.headlightsOn && t % 2 == 0) {
                if (!bLooping) {
                    this.onAlarmStart();
                }
                this.setHeadlightsOn(true);
            }
            if (this.headlightsOn && t % 2 == 1) {
                if (!bLooping) {
                    this.onAlarmStop();
                }
                this.setHeadlightsOn(false);
            }
            this.checkMusicIntensityEvent_AlarmNearby();
        }
    }

    private void checkMusicIntensityEvent_AlarmNearby() {
        if (GameServer.server) {
            return;
        }
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            float distSq;
            IsoPlayer player = IsoPlayer.players[i];
            if (player == null || player.hasTrait(CharacterTrait.DEAF) || player.isDead() || (distSq = IsoUtils.DistanceToSquared(this.getX(), this.getY(), player.getX(), player.getY())) > 2500.0f) continue;
            player.triggerMusicIntensityEvent("AlarmNearby");
            break;
        }
    }

    public boolean isMechanicUIOpen() {
        return this.mechanicUiOpen;
    }

    public void setMechanicUIOpen(boolean mechanicUiOpen) {
        this.mechanicUiOpen = mechanicUiOpen;
    }

    public void damagePlayers(float damage) {
        if (!SandboxOptions.instance.playerDamageFromCrash.getValue()) {
            return;
        }
        if (GameClient.client) {
            return;
        }
        for (int i = 0; i < this.passengers.length; ++i) {
            IsoGameCharacter chr;
            if (this.getPassenger((int)i).character == null || (chr = this.getPassenger((int)i).character).isGodMod() || GameClient.client) continue;
            this.addRandomDamageFromCrash(chr, damage);
            LuaEventManager.triggerEvent("OnPlayerGetDamage", chr, "CARCRASHDAMAGE", Float.valueOf(damage));
            if (!GameServer.server || !(chr instanceof IsoPlayer)) continue;
            IsoPlayer player = (IsoPlayer)chr;
            INetworkPacket.send(player, PacketTypes.PacketType.PlayerDamage, player);
        }
    }

    public void addRandomDamageFromCrash(IsoGameCharacter chr, float damage) {
        int damagedBodyPartNum = 1;
        if (damage > 40.0f) {
            damagedBodyPartNum = Rand.Next(1, 3);
        }
        if (damage > 70.0f) {
            damagedBodyPartNum = Rand.Next(2, 4);
        }
        int brokenGlass = 0;
        for (int i = 0; i < chr.getVehicle().getPartCount(); ++i) {
            VehiclePart part = chr.getVehicle().getPartByIndex(i);
            if (part.window == null || part.getCondition() >= 15) continue;
            ++brokenGlass;
        }
        for (int j = 0; j < damagedBodyPartNum; ++j) {
            int bodyPart = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.MAX));
            BodyPart part = chr.getBodyDamage().getBodyPart(BodyPartType.FromIndex(bodyPart));
            float realDamage = Math.max(Rand.Next(damage - 15.0f, damage), 5.0f);
            if (chr.hasTrait(CharacterTrait.FAST_HEALER)) {
                realDamage *= 0.8f;
            } else if (chr.hasTrait(CharacterTrait.SLOW_HEALER)) {
                realDamage *= 1.2f;
            }
            switch (SandboxOptions.instance.injurySeverity.getValue()) {
                case 1: {
                    realDamage *= 0.5f;
                    break;
                }
                case 3: {
                    realDamage *= 1.5f;
                }
            }
            realDamage *= this.getScript().getPlayerDamageProtection();
            part.AddDamage(realDamage *= 0.9f);
            if (realDamage > 40.0f && Rand.Next(12) == 0) {
                part.generateDeepWound();
            } else if (realDamage > 50.0f && Rand.Next(10) == 0 && SandboxOptions.instance.boneFracture.getValue()) {
                if (part.getType() == BodyPartType.Neck || part.getType() == BodyPartType.Groin) {
                    part.generateDeepWound();
                } else {
                    part.generateFracture(Rand.Next(Rand.Next(10.0f, realDamage + 10.0f), Rand.Next(realDamage + 20.0f, realDamage + 30.0f)));
                }
            }
            if (!(realDamage > 30.0f) || Rand.Next(12 - brokenGlass) != 0) continue;
            part = chr.getBodyDamage().setScratchedWindow();
            if (Rand.Next(5) != 0) continue;
            part.generateDeepWound();
            part.setHaveGlass(true);
        }
    }

    public boolean isTrunkLocked() {
        VehiclePart trunk = this.getPartById("TrunkDoor");
        if (trunk == null) {
            trunk = this.getPartById("DoorRear");
        }
        if (trunk != null && trunk.getDoor() != null && trunk.getInventoryItem() != null) {
            return trunk.getDoor().isLocked();
        }
        return false;
    }

    public void setTrunkLocked(boolean locked) {
        VehiclePart trunk = this.getPartById("TrunkDoor");
        if (trunk == null) {
            trunk = this.getPartById("DoorRear");
        }
        if (trunk != null && trunk.getDoor() != null && trunk.getInventoryItem() != null) {
            trunk.getDoor().setLocked(locked);
            if (GameServer.server) {
                this.transmitPartDoor(trunk);
            }
        }
    }

    public VehiclePart getNearestBodyworkPart(IsoGameCharacter chr) {
        for (int i = 0; i < this.getPartCount(); ++i) {
            VehiclePart part = this.getPartByIndex(i);
            if (!"door".equals(part.getCategory()) && !"bodywork".equals(part.getCategory()) || !this.isInArea(part.getArea(), chr) || part.getCondition() <= 0) continue;
            return part;
        }
        return null;
    }

    public double getSirenStartTime() {
        return this.sirenStartTime;
    }

    public void setSirenStartTime(double worldAgeHours) {
        this.sirenStartTime = worldAgeHours;
    }

    public boolean sirenShutoffTimeExpired() {
        double shutoffHours = SandboxOptions.instance.sirenShutoffHours.getValue();
        if (shutoffHours <= 0.0) {
            return false;
        }
        double worldAge = GameTime.instance.getWorldAgeHours();
        if (this.sirenStartTime > worldAge) {
            this.sirenStartTime = worldAge;
        }
        return this.sirenStartTime + shutoffHours < worldAge;
    }

    public void repair() {
        for (int i = 0; i < this.getPartCount(); ++i) {
            VehiclePart part = this.getPartByIndex(i);
            part.repair();
        }
        this.rust = 0.0f;
        this.transmitRust();
        this.bloodIntensity.clear();
        this.transmitBlood();
        this.doBloodOverlay();
    }

    public boolean isAnyListenerInside() {
        for (int seat = 0; seat < this.getMaxPassengers(); ++seat) {
            IsoPlayer isoPlayer;
            IsoGameCharacter chr = this.getCharacter(seat);
            if (!(chr instanceof IsoPlayer) || !(isoPlayer = (IsoPlayer)chr).isLocalPlayer() || chr.hasTrait(CharacterTrait.DEAF)) continue;
            return true;
        }
        return false;
    }

    public boolean couldCrawlerAttackPassenger(IsoGameCharacter chr) {
        int seat = this.getSeat(chr);
        if (seat == -1) {
            return false;
        }
        return false;
    }

    public boolean isGoodCar() {
        return this.isGoodCar;
    }

    public void setGoodCar(boolean isGoodCar) {
        this.isGoodCar = isGoodCar;
    }

    public InventoryItem getCurrentKey() {
        if (!this.ignitionSwitch.getItems().isEmpty()) {
            return this.ignitionSwitch.getItems().get(0);
        }
        return null;
    }

    public void setCurrentKey(InventoryItem currentKey) {
        this.currentKey = currentKey;
        this.ignitionSwitch.addItem(currentKey);
    }

    public boolean isInForest() {
        return this.getSquare() != null && this.getSquare().getZone() != null && ("Forest".equals(this.getSquare().getZone().getType()) || "DeepForest".equals(this.getSquare().getZone().getType()) || "FarmLand".equals(this.getSquare().getZone().getType()));
    }

    public boolean shouldNotHaveLoot() {
        if (this.getSquare() != null && IsoWorld.instance.metaGrid.getVehicleZoneAt(this.getSquare().getX(), this.getSquare().getY(), this.getSquare().getZ()) != null && Objects.equals(Objects.requireNonNull(IsoWorld.instance.metaGrid.getVehicleZoneAt((int)this.getSquare().getX(), (int)this.getSquare().getY(), (int)this.getSquare().getZ())).name, "junkyard")) {
            return true;
        }
        return this.getSquare() != null && IsoWorld.instance.metaGrid.getVehicleZoneAt(this.getSquare().getX(), this.getSquare().getY(), this.getSquare().getZ()) != null && Objects.equals(Objects.requireNonNull(IsoWorld.instance.metaGrid.getVehicleZoneAt((int)this.getSquare().getX(), (int)this.getSquare().getY(), (int)this.getSquare().getZ())).name, "luxuryDealership");
    }

    public boolean isInTrafficJam() {
        if (this.getSquare() == null) {
            return false;
        }
        if (IsoWorld.instance.metaGrid.getVehicleZoneAt(this.getSquare().getX(), this.getSquare().getY(), this.getSquare().getZ()) == null) {
            return false;
        }
        String name = Objects.requireNonNull(IsoWorld.instance.metaGrid.getVehicleZoneAt((int)this.getSquare().getX(), (int)this.getSquare().getY(), (int)this.getSquare().getZ())).name;
        if (name == null) {
            return false;
        }
        return name.contains("trafficjam") || name.contains("burnt");
    }

    public float getOffroadEfficiency() {
        if (this.isInForest()) {
            return this.script.getOffroadEfficiency() * 1.5f;
        }
        return this.script.getOffroadEfficiency() * 2.0f;
    }

    public void applyImpulseFromHitCorpse(IsoDeadBody chr) {
        float speedCap = 22.0f;
        Vector3f velocity = this.getLinearVelocity((Vector3f)TL_vector3f_pool.get().alloc());
        velocity.y = 0.0f;
        Vector3f v = (Vector3f)TL_vector3f_pool.get().alloc();
        v.set(this.getX() - chr.getX(), 0.0f, this.getZ() - chr.getY());
        v.normalize();
        velocity.mul(v);
        TL_vector3f_pool.get().release(v);
        float speed = velocity.length();
        speed = Math.min(speed, 22.0f);
        if (speed < 0.05f) {
            TL_vector3f_pool.get().release(velocity);
            return;
        }
        if (!GameServer.server) {
            SoundManager.instance.PlayWorldSound(SoundKey.ZOMBIE_THUMP_GENERIC.toString(), chr.square, 0.0f, 20.0f, 0.9f, true);
        }
        Vector3f impulse = (Vector3f)TL_vector3f_pool.get().alloc();
        impulse.set(this.getX() - chr.getX(), 0.0f, this.getY() - chr.getY());
        impulse.normalize();
        velocity.normalize();
        float dot = velocity.dot(impulse);
        TL_vector3f_pool.get().release(velocity);
        TL_vector3f_pool.get().release(impulse);
        this.applyImpulseFromHitObject(chr, this.getFudgedMass() * 3.0f * speed / 22.0f * Math.abs(dot));
    }

    public boolean isDoColor() {
        return this.doColor;
    }

    public void setDoColor(boolean doColor) {
        this.doColor = doColor;
    }

    public float getBrakeSpeedBetweenUpdate() {
        return this.brakeBetweenUpdatesSpeed;
    }

    @Override
    public IsoGridSquare getSquare() {
        return this.getCell().getGridSquare(this.getX(), this.getY(), this.getZ());
    }

    public void setColor(float value, float saturation, float hue) {
        this.colorValue = value;
        this.colorSaturation = saturation;
        this.colorHue = hue;
    }

    public void setColorHSV(float hue, float saturation, float value) {
        this.colorHue = hue;
        this.colorSaturation = saturation;
        this.colorValue = value;
    }

    public float getColorHue() {
        return this.colorHue;
    }

    public float getColorSaturation() {
        return this.colorSaturation;
    }

    public float getColorValue() {
        return this.colorValue;
    }

    public boolean isRemovedFromWorld() {
        return this.removedFromWorld;
    }

    public float getInsideTemperature() {
        VehiclePart part = this.getPartById("PassengerCompartment");
        float retval = 0.0f;
        if (part != null && part.getModData() != null) {
            if (part.getModData().rawget("temperature") != null) {
                retval += ((Double)part.getModData().rawget("temperature")).floatValue();
            }
            if (part.getModData().rawget("windowtemperature") != null) {
                retval += ((Double)part.getModData().rawget("windowtemperature")).floatValue();
            }
        }
        return retval;
    }

    public AnimationPlayer getAnimationPlayer() {
        String modelName = this.getScript().getModel().file;
        Model model = ModelManager.instance.getLoadedModel(modelName);
        if (model == null || model.isStatic) {
            return null;
        }
        if (this.animPlayer != null && this.animPlayer.getModel() != model) {
            this.animPlayer = Pool.tryRelease(this.animPlayer);
        }
        if (this.animPlayer == null) {
            this.animPlayer = AnimationPlayer.alloc(model);
        }
        return this.animPlayer;
    }

    public void releaseAnimationPlayers() {
        this.animPlayer = Pool.tryRelease(this.animPlayer);
        PZArrayUtil.forEach(this.models, ModelInfo::releaseAnimationPlayer);
    }

    public void setAddThumpWorldSound(boolean add) {
        this.addThumpWorldSound = add;
    }

    public void createImpulse(Vector3f vec) {
    }

    @Override
    public void Thump(IsoMovingObject thumper) {
        VehiclePart part;
        VehiclePart lightbar = this.getPartById("lightbar");
        if (lightbar == null) {
            return;
        }
        if (lightbar.getCondition() <= 0) {
            thumper.setThumpTarget(null);
        }
        if ((part = this.getUseablePart((IsoGameCharacter)thumper)) != null) {
            part.setCondition(part.getCondition() - Rand.Next(1, 5));
        }
        lightbar.setCondition(lightbar.getCondition() - Rand.Next(1, 5));
    }

    @Override
    public void WeaponHit(IsoGameCharacter chr, HandWeapon weapon) {
    }

    @Override
    public Thumpable getThumpableFor(IsoGameCharacter chr) {
        return null;
    }

    @Override
    public float getThumpCondition() {
        return 1.0f;
    }

    public boolean isRegulator() {
        return this.regulator;
    }

    public void setRegulator(boolean regulator) {
        this.regulator = regulator;
    }

    public float getRegulatorSpeed() {
        return this.regulatorSpeed;
    }

    public void setRegulatorSpeed(float regulatorSpeed) {
        this.regulatorSpeed = regulatorSpeed;
    }

    public float getCurrentSpeedForRegulator() {
        return (float)Math.max(5.0 * Math.floor(this.getCurrentSpeedKmHour() / 5.0f), 5.0);
    }

    public void setVehicleTowing(BaseVehicle vehicleB, String attachmentA, String attachmentB) {
        if (vehicleB != this) {
            this.vehicleTowing = vehicleB;
            this.vehicleTowingId = this.vehicleTowing == null ? -1 : this.vehicleTowing.getSqlId();
            this.towAttachmentSelf = attachmentA;
            this.towAttachmentOther = attachmentB;
            this.rowConstraintZOffset = 0.0f;
        }
    }

    public void setVehicleTowedBy(BaseVehicle vehicleA, String attachmentA, String attachmentB) {
        if (vehicleA != this) {
            this.vehicleTowedBy = vehicleA;
            this.vehicleTowedById = this.vehicleTowedBy == null ? -1 : this.vehicleTowedBy.getSqlId();
            this.towAttachmentSelf = attachmentB;
            this.towAttachmentOther = attachmentA;
            this.rowConstraintZOffset = 0.0f;
        }
    }

    public BaseVehicle getVehicleTowing() {
        return this.vehicleTowing;
    }

    public BaseVehicle getVehicleTowedBy() {
        return this.vehicleTowedBy;
    }

    public boolean attachmentExist(String attachmentName) {
        VehicleScript script = this.getScript();
        if (script == null) {
            return false;
        }
        ModelAttachment attach = script.getAttachmentById(attachmentName);
        return attach != null;
    }

    public Vector3f getAttachmentLocalPos(String attachmentName, Vector3f v) {
        VehicleScript script = this.getScript();
        if (script == null) {
            return null;
        }
        ModelAttachment attach = script.getAttachmentById(attachmentName);
        if (attach == null) {
            return null;
        }
        v.set(attach.getOffset());
        if (script.getModel() == null) {
            return v;
        }
        return v.add(script.getModel().getOffset());
    }

    public Vector3f getAttachmentWorldPos(String attachmentName, Vector3f v) {
        return (v = this.getAttachmentLocalPos(attachmentName, v)) == null ? null : this.getWorldPos(v, v);
    }

    public void setForceBrake() {
        this.getController().clientControls.forceBrake = System.currentTimeMillis();
    }

    public Vector3f getTowingLocalPos(String attachmentName, Vector3f v) {
        return this.getAttachmentLocalPos(attachmentName, v);
    }

    public Vector3f getTowedByLocalPos(String attachmentName, Vector3f v) {
        return this.getAttachmentLocalPos(attachmentName, v);
    }

    public Vector3f getTowingWorldPos(String attachmentName, Vector3f v) {
        return (v = this.getTowingLocalPos(attachmentName, v)) == null ? null : this.getWorldPos(v, v);
    }

    public Vector3f getTowedByWorldPos(String attachmentName, Vector3f v) {
        return (v = this.getTowedByLocalPos(attachmentName, v)) == null ? null : this.getWorldPos(v, v);
    }

    public Vector3f getPlayerTrailerLocalPos(String attachmentName, boolean left, Vector3f v) {
        ModelAttachment attach = this.getScript().getAttachmentById(attachmentName);
        if (attach == null) {
            return null;
        }
        Vector3f ext = this.getScript().getExtents();
        Vector3f com = this.getScript().getCenterOfMassOffset();
        float x = com.x + ext.x / 2.0f + 0.3f + 0.05f;
        if (!left) {
            x *= -1.0f;
        }
        if (attach.getOffset().z > 0.0f) {
            return v.set(x, 0.0f, com.z + ext.z / 2.0f + 0.3f + 0.05f);
        }
        return v.set(x, 0.0f, com.z - (ext.z / 2.0f + 0.3f + 0.05f));
    }

    public Vector3f getPlayerTrailerWorldPos(String attachmentName, boolean left, Vector3f v) {
        if ((v = this.getPlayerTrailerLocalPos(attachmentName, left, v)) == null) {
            return null;
        }
        this.getWorldPos(v, v);
        v.z = PZMath.fastfloor(this.getZ());
        Vector3f v2 = this.getTowingWorldPos(attachmentName, (Vector3f)TL_vector3f_pool.get().alloc());
        boolean blocked = PolygonalMap2.instance.lineClearCollide(v.x, v.y, v2.x, v2.y, PZMath.fastfloor(this.getZ()), this, false, false);
        TL_vector3f_pool.get().release(v2);
        if (blocked) {
            return null;
        }
        return v;
    }

    private void drawTowingRope() {
        BaseVehicle vehicleB = this.getVehicleTowing();
        if (vehicleB == null) {
            return;
        }
        Vector3fObjectPool pool = TL_vector3f_pool.get();
        Vector3f v2 = this.getAttachmentWorldPos("trailerfront", (Vector3f)pool.alloc());
        ModelAttachment attach = this.script.getAttachmentById("trailerfront");
        if (attach != null) {
            v2.set(attach.getOffset());
        }
        Vector2 tempVector2 = new Vector2();
        tempVector2.x = vehicleB.getX();
        tempVector2.y = vehicleB.getY();
        tempVector2.x -= this.getX();
        tempVector2.y -= this.getY();
        tempVector2.setLength(2.0f);
        this.drawDirectionLine(tempVector2, tempVector2.getLength(), 1.0f, 0.5f, 0.5f);
    }

    public void drawDirectionLine(Vector2 dir, float length, float r, float g, float b) {
        float x2 = this.getX() + dir.x * length;
        float y2 = this.getY() + dir.y * length;
        float sx = IsoUtils.XToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
        float sy = IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
        float sx2 = IsoUtils.XToScreenExact(x2, y2, this.getZ(), 0);
        float sy2 = IsoUtils.YToScreenExact(x2, y2, this.getZ(), 0);
        LineDrawer.drawLine(sx, sy, sx2, sy2, r, g, b, 0.5f, 1);
    }

    public void addPointConstraint(IsoPlayer player, BaseVehicle vehicleB, String attachmentA, String attachmentB) {
        if (this != vehicleB) {
            this.addPointConstraint(player, vehicleB, attachmentA, attachmentB, false);
        }
    }

    public void addPointConstraint(IsoPlayer player, BaseVehicle vehicleB, String attachmentA, String attachmentB, Boolean remote) {
        this.setPreviouslyMoved(true);
        if (vehicleB == null || player != null && (IsoUtils.DistanceToSquared(player.getX(), player.getY(), this.getX(), this.getY()) > 100.0f || IsoUtils.DistanceToSquared(player.getX(), player.getY(), vehicleB.getX(), vehicleB.getY()) > 100.0f)) {
            DebugLog.Vehicle.warn("The " + player.getUsername() + " user attached vehicles at a long distance");
        }
        BaseVehicle vehicleA = this;
        vehicleA.breakConstraint(true, remote);
        vehicleB.breakConstraint(true, remote);
        Vector3fObjectPool pool = TL_vector3f_pool.get();
        Vector3f v1 = vehicleA.getTowingLocalPos(attachmentA, (Vector3f)pool.alloc());
        Vector3f v2 = vehicleB.getTowedByLocalPos(attachmentB, (Vector3f)pool.alloc());
        if (v1 == null || v2 == null) {
            if (v1 != null) {
                pool.release(v1);
            }
            if (v2 != null) {
                pool.release(v2);
            }
            return;
        }
        if (!GameServer.server) {
            vehicleA.constraintTowing = vehicleA.getScriptName().contains("Trailer") || vehicleB.getScriptName().contains("Trailer") ? Bullet.addPointConstraint(vehicleA.vehicleId, vehicleB.vehicleId, v1.x, v1.y, v1.z, v2.x, v2.y, v2.z) : Bullet.addRopeConstraint(vehicleA.vehicleId, vehicleB.vehicleId, v1.x, v1.y, v1.z, v2.x, v2.y, v2.z, 1.5f);
        }
        vehicleB.constraintTowing = vehicleA.constraintTowing;
        vehicleA.setVehicleTowing(vehicleB, attachmentA, attachmentB);
        vehicleB.setVehicleTowedBy(vehicleA, attachmentA, attachmentB);
        pool.release(v1);
        pool.release(v2);
        vehicleA.constraintChanged();
        vehicleB.constraintChanged();
        if (GameServer.server && player != null && vehicleA.netPlayerAuthorization == Authorization.Server && vehicleB.netPlayerAuthorization == Authorization.Server) {
            vehicleA.setNetPlayerAuthorization(Authorization.LocalCollide, player.onlineId);
            vehicleA.authSimulationTime = System.currentTimeMillis();
            vehicleB.setNetPlayerAuthorization(Authorization.LocalCollide, player.onlineId);
            vehicleB.authSimulationTime = System.currentTimeMillis();
        }
        if (GameServer.server && !remote.booleanValue()) {
            VehicleManager.instance.attachTowing(vehicleA, vehicleB, attachmentA, attachmentB);
        }
    }

    public void authorizationChanged(IsoGameCharacter character) {
        if (character != null) {
            this.setNetPlayerAuthorization(Authorization.Local, character.getOnlineID());
        } else {
            this.setNetPlayerAuthorization(Authorization.Server, -1);
        }
    }

    public void constraintChanged() {
        long currentTimeMS = System.currentTimeMillis();
        this.setPhysicsActive(true);
        this.constraintChangedTime = currentTimeMS;
        if (GameServer.server) {
            if (this.getVehicleTowing() != null) {
                this.authorizationChanged(this.getDriver());
                this.getVehicleTowing().authorizationChanged(this.getDriver());
            } else if (this.getVehicleTowedBy() != null) {
                this.authorizationChanged(this.getVehicleTowedBy().getDriver());
                this.getVehicleTowedBy().authorizationChanged(this.getVehicleTowedBy().getDriver());
            } else {
                this.authorizationChanged(this.getDriver());
            }
        }
    }

    public void breakConstraint(boolean forgetID, boolean remote) {
        if (!GameServer.server && this.constraintTowing == -1) {
            return;
        }
        if (!GameServer.server) {
            Bullet.removeConstraint(this.constraintTowing);
        }
        this.constraintTowing = -1;
        if (this.vehicleTowing != null) {
            if (GameServer.server && !remote) {
                VehicleManager.instance.detachTowing(this, this.vehicleTowing);
            }
            this.vehicleTowing.vehicleTowedBy = null;
            this.vehicleTowing.constraintTowing = -1;
            if (forgetID) {
                this.vehicleTowingId = -1;
                this.vehicleTowing.vehicleTowedById = -1;
            }
            this.vehicleTowing.constraintChanged();
            this.vehicleTowing = null;
        }
        if (this.vehicleTowedBy != null) {
            if (GameServer.server && !remote) {
                VehicleManager.instance.detachTowing(this.vehicleTowedBy, this);
            }
            this.vehicleTowedBy.vehicleTowing = null;
            this.vehicleTowedBy.constraintTowing = -1;
            if (forgetID) {
                this.vehicleTowedBy.vehicleTowingId = -1;
                this.vehicleTowedById = -1;
            }
            this.vehicleTowedBy.constraintChanged();
            this.vehicleTowedBy = null;
        }
        this.constraintChanged();
    }

    public boolean canAttachTrailer(BaseVehicle vehicleB, String attachmentA, String attachmentB) {
        return this.canAttachTrailer(vehicleB, attachmentA, attachmentB, false);
    }

    public boolean canAttachTrailer(BaseVehicle vehicleB, String attachmentA, String attachmentB, boolean reconnect) {
        boolean isTrailer;
        BaseVehicle vehicleA = this;
        if (vehicleA == vehicleB || vehicleA.physics == null || vehicleA.constraintTowing != -1) {
            return false;
        }
        if (vehicleB == null || vehicleB.physics == null || vehicleB.constraintTowing != -1) {
            return false;
        }
        Vector3fObjectPool pool = TL_vector3f_pool.get();
        Vector3f v1 = vehicleA.getTowingWorldPos(attachmentA, (Vector3f)pool.alloc());
        Vector3f v2 = vehicleB.getTowedByWorldPos(attachmentB, (Vector3f)pool.alloc());
        if (v1 == null || v2 == null) {
            return false;
        }
        float distSq = IsoUtils.DistanceToSquared(v1.x, v1.y, 0.0f, v2.x, v2.y, 0.0f);
        pool.release(v1);
        pool.release(v2);
        ModelAttachment attachA = vehicleA.script.getAttachmentById(attachmentA);
        ModelAttachment attachB = vehicleB.script.getAttachmentById(attachmentB);
        if (attachA != null && attachA.getCanAttach() != null && !attachA.getCanAttach().contains(attachmentB)) {
            return false;
        }
        if (attachB != null && attachB.getCanAttach() != null && !attachB.getCanAttach().contains(attachmentA)) {
            return false;
        }
        boolean bl = isTrailer = vehicleA.getScriptName().contains("Trailer") || vehicleB.getScriptName().contains("Trailer");
        return distSq < (reconnect ? 10.0f : (isTrailer ? 1.0f : 4.0f));
    }

    private void tryReconnectToTowedVehicle() {
        if (GameClient.client) {
            short towedID = VehicleManager.instance.getTowedVehicleID(this.vehicleId);
            if (towedID == -1) {
                return;
            }
            BaseVehicle vehicleToTow = VehicleManager.instance.getVehicleByID(towedID);
            if (vehicleToTow == null || vehicleToTow == this) {
                return;
            }
            if (!this.canAttachTrailer(vehicleToTow, this.towAttachmentSelf, this.towAttachmentOther, true)) {
                return;
            }
            this.addPointConstraint(null, vehicleToTow, this.towAttachmentSelf, this.towAttachmentOther, true);
            return;
        }
        if (this.vehicleTowing != null) {
            return;
        }
        if (this.vehicleTowingId == -1) {
            return;
        }
        BaseVehicle vehicleToTow = null;
        ArrayList<BaseVehicle> vehicles = IsoWorld.instance.currentCell.getVehicles();
        for (int i = 0; i < vehicles.size(); ++i) {
            BaseVehicle vehicle = vehicles.get(i);
            if (vehicle.getSqlId() != this.vehicleTowingId) continue;
            vehicleToTow = vehicle;
            break;
        }
        if (vehicleToTow == null || vehicleToTow == this) {
            return;
        }
        if (!this.canAttachTrailer(vehicleToTow, this.towAttachmentSelf, this.towAttachmentOther, true)) {
            return;
        }
        this.addPointConstraint(null, vehicleToTow, this.towAttachmentSelf, this.towAttachmentOther, false);
    }

    public void positionTrailer(BaseVehicle trailer) {
        if (trailer == null) {
            return;
        }
        Vector3fObjectPool pool = TL_vector3f_pool.get();
        Vector3f v1 = this.getTowingWorldPos("trailer", (Vector3f)pool.alloc());
        Vector3f v2 = trailer.getTowedByWorldPos("trailer", (Vector3f)pool.alloc());
        if (v1 == null || v2 == null) {
            return;
        }
        v2.sub(trailer.getX(), trailer.getY(), trailer.getZ());
        v1.sub(v2);
        Transform xfrm = trailer.getWorldTransform(BaseVehicle.allocTransform());
        xfrm.origin.set(v1.x - WorldSimulation.instance.offsetX, trailer.jniTransform.origin.y, v1.y - WorldSimulation.instance.offsetY);
        trailer.setWorldTransform(xfrm);
        BaseVehicle.releaseTransform(xfrm);
        trailer.setX(v1.x);
        trailer.setLastX(v1.x);
        trailer.setY(v1.y);
        trailer.setLastY(v1.y);
        trailer.setCurrentSquareFromPosition(v1.x, v1.y, 0.0f);
        this.addPointConstraint(null, trailer, "trailer", "trailer");
        pool.release(v1);
        pool.release(v2);
    }

    public String getTowAttachmentSelf() {
        return this.towAttachmentSelf;
    }

    public String getTowAttachmentOther() {
        return this.towAttachmentOther;
    }

    public VehicleEngineRPM getVehicleEngineRPM() {
        if (this.vehicleEngineRpm == null) {
            this.vehicleEngineRpm = ScriptManager.instance.getVehicleEngineRPM(this.getScript().getEngineRPMType());
            if (this.vehicleEngineRpm == null) {
                DebugLog.Vehicle.warn("unknown vehicleEngineRPM \"%s\"", this.getScript().getEngineRPMType());
                this.vehicleEngineRpm = new VehicleEngineRPM();
            }
        }
        return this.vehicleEngineRpm;
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

    private void stopEngineSounds() {
        if (this.emitter == null) {
            return;
        }
        for (int i = 0; i < this.newEngineSoundId.length; ++i) {
            if (this.newEngineSoundId[i] == 0L) continue;
            this.getEmitter().stopSound(this.newEngineSoundId[i]);
            this.newEngineSoundId[i] = 0L;
        }
        if (this.combinedEngineSound != 0L) {
            if (this.getEmitter().hasSustainPoints(this.combinedEngineSound)) {
                this.getEmitter().triggerCue(this.combinedEngineSound);
            } else {
                this.getEmitter().stopSound(this.combinedEngineSound);
            }
            this.combinedEngineSound = 0L;
        }
    }

    public BaseVehicle setSmashed(String location) {
        return this.setSmashed(location, false);
    }

    public BaseVehicle setSmashed(String location, boolean flipped) {
        KahluaTableImpl car;
        KahluaTableImpl cars;
        String newScript = null;
        Integer newSkinIndex = null;
        KahluaTableImpl def = (KahluaTableImpl)LuaManager.env.rawget("SmashedCarDefinitions");
        if (def != null && (cars = (KahluaTableImpl)def.rawget("cars")) != null && (car = (KahluaTableImpl)cars.rawget(this.getScriptName())) != null) {
            newScript = car.rawgetStr(location.toLowerCase());
            newSkinIndex = car.rawgetInt("skin");
            if (newSkinIndex == -1) {
                newSkinIndex = this.getSkinIndex();
            }
        }
        int tKeyId = this.getKeyId();
        if (newScript != null) {
            VehiclePart gloveBox;
            this.removeFromWorld();
            this.permanentlyRemove();
            BaseVehicle v = new BaseVehicle(IsoWorld.instance.currentCell);
            v.setScriptName(newScript);
            v.setScript();
            v.setSkinIndex(newSkinIndex);
            v.setX(this.getX());
            v.setY(this.getY());
            v.setZ(this.getZ());
            v.setDir(this.getDir());
            v.savedRot.set(this.savedRot);
            v.savedPhysicsZ = this.savedPhysicsZ;
            if (flipped) {
                float ry = this.getAngleY();
                v.savedRot.rotationXYZ(0.0f, ry * ((float)Math.PI / 180), (float)Math.PI);
            }
            v.jniTransform.setRotation(v.savedRot);
            if (IsoChunk.doSpawnedVehiclesInInvalidPosition(v)) {
                v.setSquare(this.square);
                v.square.chunk.vehicles.add(v);
                v.chunk = v.square.chunk;
                v.addToWorld();
                VehiclesDB2.instance.addVehicle(v);
            }
            v.setGeneralPartCondition(0.5f, 60.0f);
            VehiclePart part = v.getPartById("Engine");
            if (part != null) {
                part.setCondition(0);
            }
            if ((gloveBox = v.getPartById("GloveBox")) != null) {
                gloveBox.setInventoryItem(null);
                gloveBox.setCondition(0);
            }
            v.engineQuality = 0;
            v.setKeyId(tKeyId);
            return v;
        }
        return this;
    }

    public boolean isCollided(IsoGameCharacter character) {
        if (GameClient.client && this.getDriver() != null && !this.getDriver().isLocal()) {
            return true;
        }
        Vector2 v = this.testCollisionWithCharacter(character, 0.20000002f, this.hitVars.collision);
        return v != null && v.x != -1.0f;
    }

    public HitVars checkNetworkCollision(IsoGameCharacter target) {
        boolean hit = target.testCollideWithVehicles(this, this.hitVars);
        if (hit) {
            if (target.causesDamageToVehicleWhenHit(this)) {
                this.hitVars.vehicleSpeed = this.getCurrentSpeedKmHour() / 5.0f;
                int dmg = this.calculateDamageWithCharacter(target);
                if (dmg > 0) {
                    boolean isInFront = this.hitVars.isVehicleHitFromFront;
                    DebugType.VehicleHit.trace("Damage car %s damage=%d", isInFront ? "front" : "rear", dmg);
                }
                this.hitVars.vehicleDamage = dmg;
            }
            return this.hitVars;
        }
        return null;
    }

    public void onHitLandmine(IsoGridSquare square) {
        this.applyImpulseGeneric(square.getX(), square.getY(), square.getZ(), 0.0f, 0.0f, 1.0f, this.getFudgedMass() * 500.0f);
        DebugType.General.println("Hit a land mine! %f, %f, %f. isActive: %s, isStatic: %s", Float.valueOf(square.getX()), Float.valueOf(square.getY()), Float.valueOf(square.getZ()), this.isActive ? "true" : "false", this.isStatic ? "true" : "false");
    }

    public void onJump() {
        this.applyImpulseGeneric(this.getX(), this.getY(), this.getZ(), 0.0f, 0.0f, 1.0f, this.getFudgedMass() * 500.0f);
        DebugType.General.println("Jump! isActive: %s, isStatic: %s", this.isActive ? "true" : "false", this.isStatic ? "true" : "false");
    }

    public boolean updateNetworkHitByVehicle(IsoGameCharacter target) {
        if (!target.isNetworkVehicleCollisionActive(this) || !this.shouldCollideWithCharacters()) {
            return false;
        }
        HitVars hitVars = this.checkNetworkCollision(target);
        if (hitVars == null) {
            return false;
        }
        target.doNetworkHitByVehicle(this, hitVars);
        return true;
    }

    public float getAnimalTrailerSize() {
        return this.getScript().getAnimalTrailerSize();
    }

    public ArrayList<IsoAnimal> getAnimals() {
        return this.animals;
    }

    public void addAnimalFromHandsInTrailer(IsoAnimal animal, IsoPlayer player) {
        this.animals.add(animal);
        animal.setVehicle(this);
        AnimalInventoryItem item = player.getInventory().getAnimalInventoryItem(animal);
        if (item != null) {
            player.getInventory().Remove(item);
        } else {
            DebugLog.Animal.error("Animal not found: id=%d/%d", animal.getAnimalID(), animal.getOnlineID());
        }
        player.setPrimaryHandItem(null);
        player.setSecondaryHandItem(null);
        this.recalcAnimalSize();
    }

    public void addAnimalFromHandsInTrailer(IsoDeadBody body, IsoPlayer player) {
        IsoAnimal animal = IsoAnimal.createAnimalFromCorpse(body);
        animal.setHealth(0.0f);
        InventoryItem item = player.getPrimaryHandItem();
        player.getInventory().Remove(item);
        player.setPrimaryHandItem(null);
        player.setSecondaryHandItem(null);
        this.addAnimalInTrailer(animal);
    }

    public void addAnimalInTrailer(IsoDeadBody body) {
        IsoAnimal animal = IsoAnimal.createAnimalFromCorpse(body);
        animal.setHealth(0.0f);
        this.addAnimalInTrailer(animal);
        body.getSquare().removeCorpse(body, false);
        body.invalidateCorpse();
    }

    public void addAnimalInTrailer(IsoAnimal animal) {
        this.animals.add(animal);
        if (animal.mother != null) {
            animal.attachBackToMother = animal.mother.animalId;
        }
        animal.setVehicle(this);
        if (animal.getData().getAttachedPlayer() != null) {
            animal.getData().getAttachedPlayer().removeAttachedAnimal(animal);
            animal.getData().setAttachedPlayer(null);
        }
        animal.removeFromWorld();
        animal.removeFromSquare();
        animal.setX(this.getX());
        animal.setY(this.getY());
        this.recalcAnimalSize();
    }

    private void recalcAnimalSize() {
        this.totalAnimalSize = 0.0f;
        for (int i = 0; i < this.animals.size(); ++i) {
            this.totalAnimalSize += this.animals.get(i).getAnimalTrailerSize();
        }
    }

    public IsoObject removeAnimalFromTrailer(IsoAnimal animal) {
        IsoMovingObject toReturn = null;
        for (int i = 0; i < this.animals.size(); ++i) {
            IsoAnimal animalTest = this.animals.get(i);
            if (animalTest != animal) continue;
            this.animals.remove(animalTest);
            Vector2 vec = this.getAreaCenter("AnimalEntry");
            IsoAnimal newAnimal = new IsoAnimal(this.getSquare().getCell(), PZMath.fastfloor(vec.x), PZMath.fastfloor(vec.y), this.getSquare().z, animal.getAnimalType(), animal.getBreed());
            newAnimal.copyFrom(animal);
            AnimalInstanceManager.getInstance().remove(animal);
            AnimalInstanceManager.getInstance().add(newAnimal, animal.getOnlineID());
            newAnimal.attachBackToMotherTimer = 10000.0f;
            toReturn = newAnimal;
            if (animal.getHealth() != 0.0f) continue;
            IsoDeadBody body = new IsoDeadBody(newAnimal);
            if (newAnimal.getSquare() != null) {
                newAnimal.getSquare().addCorpse(body, false);
                body.invalidateCorpse();
            }
            toReturn = body;
        }
        this.recalcAnimalSize();
        return toReturn;
    }

    public void replaceGrownAnimalInTrailer(IsoAnimal current, IsoAnimal grown) {
        if (current == null || grown == null || current == grown || this.animals.contains(grown)) {
            return;
        }
        for (int i = 0; i < this.animals.size(); ++i) {
            IsoAnimal animalTest = this.animals.get(i);
            if (animalTest != current) continue;
            this.animals.set(i, grown);
            break;
        }
        this.recalcAnimalSize();
    }

    public float getCurrentTotalAnimalSize() {
        return this.totalAnimalSize;
    }

    public void setCurrentTotalAnimalSize(float totalAnimalSize) {
        this.totalAnimalSize = totalAnimalSize;
    }

    public void keyNamerVehicle(InventoryItem item) {
        BaseVehicle.keyNamerVehicle(item, this);
    }

    public static void keyNamerVehicle(InventoryItem item, BaseVehicle vehicle) {
        if (item == null || vehicle == null) {
            return;
        }
        if (vehicle.getSquare() != null) {
            item.setOrigin(vehicle.getSquare());
        }
        if (!item.getType().equals("KeyRing") && !item.hasTag(ItemTag.KEY_RING)) {
            String carName = vehicle.getScript().getName();
            if (vehicle.getScript().getCarModelName() != null) {
                carName = vehicle.getScript().getCarModelName();
            }
            item.setName(Translator.getText(item.getScriptItem().getDisplayName()) + " - " + Translator.getText("IGUI_VehicleName" + carName));
        }
    }

    public boolean checkZombieKeyForVehicle(IsoZombie zombie) {
        return this.checkZombieKeyForVehicle(zombie, this.getScriptName());
    }

    public boolean checkZombieKeyForVehicle(IsoZombie zombie, String vehicleType) {
        if (vehicleType.contains("Burnt") || vehicleType.equals("Trailer") || vehicleType.equals("TrailerAdvert")) {
            return false;
        }
        String outfitName = zombie.getOutfitName();
        if (outfitName == null) {
            return false;
        }
        if (this.getZombieType() != null && this.hasZombieType(outfitName)) {
            return true;
        }
        if (outfitName.contains("Survivalist")) {
            return true;
        }
        if (this.getZombieType() != null) {
            return false;
        }
        if (this.checkForSpecialMatchOne("Fire", vehicleType, outfitName)) {
            return this.checkForSpecialMatchTwo("Fire", vehicleType, outfitName);
        }
        if (this.checkForSpecialMatchOne("Police", vehicleType, outfitName)) {
            return this.checkForSpecialMatchTwo("Police", vehicleType, outfitName);
        }
        if (this.checkForSpecialMatchOne("Spiffo", vehicleType, outfitName)) {
            return this.checkForSpecialMatchTwo("Spiffo", vehicleType, outfitName);
        }
        if (vehicleType.contains("Ranger") || vehicleType.contains("Lights") && this.getSkinIndex() == 0) {
            return outfitName.contains("Ranger");
        }
        if (vehicleType.contains("Lights") && this.getSkinIndex() == 1 || vehicleType.contains("VanSpecial") && this.getSkinIndex() == 0 || vehicleType.contains("Fossoil")) {
            return outfitName.contains("ConstructionWorker") || outfitName.contains("Fossoil") || outfitName.contains("Foreman") || outfitName.contains("Mechanic") || outfitName.contains("MetalWorker");
        }
        if (outfitName.contains("Postal")) {
            return vehicleType.contains("Mail") || vehicleType.contains("VanSpecial") && this.getSkinIndex() == 2;
        }
        if (vehicleType.contains("Mccoy") || vehicleType.contains("VanSpecial") && this.getSkinIndex() == 1) {
            return outfitName.contains("Foreman") || outfitName.contains("Mccoy");
        }
        if (vehicleType.contains("Taxi")) {
            return outfitName.contains("Generic");
        }
        if (outfitName.contains("Cook") || outfitName.contains("Security") || outfitName.contains("Waiter")) {
            return vehicleType.contains("Normal") || vehicleType.contains("Small");
        }
        if (outfitName.contains("Farmer") || outfitName.contains("Fisherman") || outfitName.contains("Hunter")) {
            return vehicleType.contains("Pickup") || vehicleType.contains("OffRoad") || vehicleType.contains("SUV") || vehicleType.equals("Trailer_Horsebox") || vehicleType.equals("Trailer_Livestock");
        }
        if (outfitName.contains("Teacher")) {
            return vehicleType.contains("Normal") || vehicleType.contains("Small") || vehicleType.contains("StationWagon") || vehicleType.contains("SUV");
        }
        if (!(!outfitName.contains("Young") && !outfitName.contains("Student") || vehicleType.contains("Small") && Rand.Next(2) == 0)) {
            return false;
        }
        if (vehicleType.contains("Luxury") || vehicleType.contains("Modern") || vehicleType.contains("SUV")) {
            return outfitName.contains("Classy") || outfitName.contains("Doctor") || outfitName.contains("Dress") || outfitName.contains("Generic") || outfitName.contains("Golfer") || outfitName.contains("OfficeWorker") || outfitName.contains("Foreman") || outfitName.contains("Priest") || outfitName.contains("Thug") || outfitName.contains("Trader") || outfitName.contains("FitnessInstructor");
        }
        if (vehicleType.contains("Sports")) {
            return outfitName.contains("Classy") || outfitName.contains("Doctor") || outfitName.contains("Dress") || outfitName.contains("Generic") || outfitName.contains("Golfer") || outfitName.contains("OfficeWorker") || outfitName.contains("Trader") || outfitName.contains("Bandit") || outfitName.contains("Biker") || outfitName.contains("Redneck") || outfitName.contains("Veteran") || outfitName.contains("Thug") || outfitName.contains("Foreman");
        }
        if ((vehicleType.contains("Small") || vehicleType.contains("StationWagon")) && (outfitName.contains("Foreman") || outfitName.contains("Classy") || outfitName.contains("Doctor") || outfitName.contains("Golfer") || outfitName.contains("Trader") || outfitName.contains("Biker"))) {
            return false;
        }
        if ((vehicleType.contains("Pickup") || vehicleType.contains("Van")) && (outfitName.contains("Classy") || outfitName.contains("Doctor") || outfitName.contains("Golfer") || outfitName.contains("Trader"))) {
            return false;
        }
        if (outfitName.contains("ConstructionWorker") || outfitName.contains("Fossoil")) {
            return vehicleType.contains("Pickup") || vehicleType.contains("Offroad");
        }
        if (vehicleType.contains("OffRoad")) {
            return outfitName.contains("Classy") || outfitName.contains("Doctor") || outfitName.contains("Generic") || outfitName.contains("Golfer") || outfitName.contains("Foreman") || outfitName.contains("Trader") || outfitName.contains("Biker") || outfitName.contains("Redneck");
        }
        if (outfitName.contains("Redneck") || outfitName.contains("Thug") || outfitName.contains("Veteran")) {
            return vehicleType.contains("Normal") || vehicleType.contains("Pickup") || vehicleType.contains("Offroad") || vehicleType.contains("Small");
        }
        if (outfitName.contains("Biker")) {
            return vehicleType.contains("Normal") || vehicleType.contains("Pickup") || vehicleType.contains("Offroad");
        }
        return true;
    }

    public boolean checkForSpecialMatchOne(String one, String two, String three) {
        return two.contains(one) || three.contains(one);
    }

    public boolean checkForSpecialMatchTwo(String one, String two, String three) {
        return two.contains(one) && three.contains(one);
    }

    public boolean checkIfGoodVehicleForKey() {
        return !this.getScriptName().contains("Burnt");
    }

    public boolean trySpawnVehicleKeyOnZombie(IsoZombie zombie) {
        boolean randomKeyRing;
        if (!zombie.shouldZombieHaveKey(true) || !this.checkZombieKeyForVehicle(zombie)) {
            return false;
        }
        InventoryItem key = this.createVehicleKey();
        if (key == null) {
            return false;
        }
        this.keySpawned = 1;
        boolean bl = randomKeyRing = this.getScript().hasSpecialKeyRing() && this.getSpecialKeyRingChance() > 0.0f && (float)Rand.Next(100) < this.getSpecialKeyRingChance();
        if (!randomKeyRing && Rand.Next(2) == 0) {
            zombie.addItemToSpawnAtDeath(key);
            return true;
        }
        InventoryContainer keyRing = this.tryCreateKeyRing();
        if (keyRing == null) {
            zombie.addItemToSpawnAtDeath(key);
            return true;
        }
        keyRing.getInventory().AddItem(key);
        this.randomlyAddNearestBuildingKeyToContainer(keyRing.getInventory());
        zombie.addItemToSpawnAtDeath(keyRing);
        return true;
    }

    public boolean trySpawnVehicleKeyInObject(IsoObject obj) {
        if (obj.container != null && !obj.container.isExplored() && !obj.container.isShop() && obj.container.getFirstTagRecurse(ItemTag.CAR_KEY) == null && (obj.container.type.equals("counter") || obj.container.type.equals("officedrawers") || obj.container.type.equals("shelves") || obj.container.type.equals("desk") || obj.container.type.equals("filingcabinet") || obj.container.type.equals("locker") || obj.container.type.equals("metal_shelves") || obj.container.type.equals("tent") || obj.container.type.equals("shelter") || obj.container.type.equals("sidetable") || obj.container.type.equals("plankstash") || obj.container.type.equals("wardrobe") || obj.container.type.equals("dresser"))) {
            this.putKeyToContainer(obj.container, this.square, obj);
            if ((float)Rand.Next(100) < 1.0f * this.keySpawnChancedD100 && this.square.getBuilding() != null && this.square.getBuilding().getDef() != null && this.square.getBuilding().getDef().getKeyId() != -1) {
                this.addBuildingKeyToGloveBox(this.square);
            }
            return true;
        }
        return false;
    }

    public boolean checkSquareForVehicleKeySpot(IsoGridSquare square) {
        return this.checkSquareForVehicleKeySpot(square, false);
    }

    public boolean checkSquareForVehicleKeySpot(IsoGridSquare square, boolean crashed) {
        boolean keyInSquare = false;
        if (square == null) {
            return keyInSquare;
        }
        keyInSquare = !this.isBurntOrSmashed() && !crashed ? this.checkSquareForVehicleKeySpotContainer(square) || this.checkSquareForVehicleKeySpotZombie(square) : this.checkSquareForVehicleKeySpotZombie(square);
        return keyInSquare;
    }

    public boolean checkSquareForVehicleKeySpotContainer(IsoGridSquare square) {
        boolean keyInSquare = false;
        if (square != null && !square.isShop() && !this.isBurntOrSmashed()) {
            for (int n = 0; n < square.getObjects().size(); ++n) {
                IsoObject obj = square.getObjects().get(n);
                keyInSquare = this.trySpawnVehicleKeyInObject(obj);
                if (!keyInSquare) continue;
                return true;
            }
        }
        return keyInSquare;
    }

    public boolean checkSquareForVehicleKeySpotZombie(IsoGridSquare square) {
        boolean keyInSquare = false;
        if (square != null) {
            for (int i = 0; i < square.getMovingObjects().size(); ++i) {
                IsoZombie zombie;
                IsoMovingObject isoMovingObject = square.getMovingObjects().get(i);
                if (!(isoMovingObject instanceof IsoZombie) || !(zombie = (IsoZombie)isoMovingObject).shouldZombieHaveKey(true) || !(keyInSquare = this.trySpawnVehicleKeyOnZombie(zombie))) continue;
                return true;
            }
        }
        return keyInSquare;
    }

    private static float doKeySandboxSettings(int value) {
        return switch (value) {
            case 1 -> 0.0f;
            case 2 -> 0.05f;
            case 3 -> 0.2f;
            case 4 -> 0.6f;
            case 5 -> 1.0f;
            case 6 -> 2.0f;
            case 7 -> 2.4f;
            default -> 0.6f;
        };
    }

    public void forceVehicleDistribution(String distribution) {
        ItemPickerJava.VehicleDistribution distro = ItemPickerJava.VehicleDistributions.get(distribution);
        ItemPickerJava.ItemPickerRoom distro2 = distro.normal;
        for (int i = 0; i < this.getPartCount(); ++i) {
            VehiclePart part = this.getPartByIndex(i);
            if (part.getItemContainer() == null) continue;
            if (GameServer.server && GameServer.softReset) {
                part.getItemContainer().setExplored(false);
            }
            if (part.getItemContainer().explored) continue;
            part.getItemContainer().clear();
            this.randomizeContainer(part, distro2);
            part.getItemContainer().setExplored(true);
        }
    }

    public boolean canLightSmoke(IsoGameCharacter chr) {
        if (chr == null) {
            return false;
        }
        if (!this.hasLighter()) {
            return false;
        }
        if (this.getBatteryCharge() <= 0.0f) {
            return false;
        }
        return this.getSeat(chr) <= 1;
    }

    private void checkVehicleFailsToStartWithZombiesTargeting() {
        if (GameServer.server) {
            return;
        }
        for (int i = 0; i < this.getMaxPassengers(); ++i) {
            Passenger passenger = this.getPassenger(i);
            IsoPlayer player = Type.tryCastTo(passenger.character, IsoPlayer.class);
            if (player == null || !player.isLocalPlayer()) continue;
            int numZombies = player.getStats().musicZombiesTargetingNearbyMoving;
            if ((numZombies += player.getStats().musicZombiesTargetingNearbyNotMoving) <= 0) continue;
            player.triggerMusicIntensityEvent("VehicleFailsToStartWithZombiesTargeting");
        }
    }

    private void checkVehicleStartsWithZombiesTargeting() {
        if (GameServer.server) {
            return;
        }
        for (int i = 0; i < this.getMaxPassengers(); ++i) {
            Passenger passenger = this.getPassenger(i);
            IsoPlayer player = Type.tryCastTo(passenger.character, IsoPlayer.class);
            if (player == null || !player.isLocalPlayer()) continue;
            int numZombies = player.getStats().musicZombiesTargetingNearbyMoving;
            if ((numZombies += player.getStats().musicZombiesTargetingNearbyNotMoving) <= 0) continue;
            player.triggerMusicIntensityEvent("VehicleStartsWithZombiesTargeting");
        }
    }

    public ArrayList<String> getZombieType() {
        return this.script.getZombieType();
    }

    public String getRandomZombieType() {
        return this.script.getRandomZombieType();
    }

    public boolean hasZombieType(String outfit) {
        return this.script.hasZombieType(outfit);
    }

    public String getFirstZombieType() {
        return this.script.getFirstZombieType();
    }

    public boolean notKillCrops() {
        return this.script.notKillCrops();
    }

    public boolean hasLighter() {
        return this.script.hasLighter();
    }

    public boolean leftSideFuel() {
        VehicleScript.Area area = this.getScript().getAreaById("GasTank");
        return area != null && !(area.x < 0.0f);
    }

    public boolean rightSideFuel() {
        VehicleScript.Area area = this.getScript().getAreaById("GasTank");
        return area != null && !(area.x > 0.0f);
    }

    public boolean isCreated() {
        return this.created;
    }

    public float getTotalContainerItemWeight() {
        float totalContainerItemWeight = 0.0f;
        for (int i = 0; i < this.parts.size(); ++i) {
            VehiclePart part = this.parts.get(i);
            if (part.getItemContainer() == null) continue;
            totalContainerItemWeight += part.getItemContainer().getCapacityWeight();
        }
        return totalContainerItemWeight;
    }

    public boolean isSirening() {
        return this.hasLightbar() && this.lightbarSirenMode.get() > 0;
    }

    private boolean isDriverGodMode() {
        IsoGameCharacter isoGameCharacter = this.getDriverRegardlessOfTow();
        return isoGameCharacter != null && isoGameCharacter.isGodMod();
    }

    public Vector3f getIntersectPoint(Vector3f start, Vector3f end, Vector3f result) {
        float x = this.script.getExtents().x * 0.5f + this.script.getCenterOfMassOffset().x();
        float y = this.script.getExtents().y * 0.5f + this.script.getCenterOfMassOffset().y();
        float z = this.script.getExtents().z * 0.5f + this.script.getCenterOfMassOffset().z();
        Vector3f extents = BaseVehicle.allocVector3f().set(x, y, z);
        Vector3f localStart = this.getLocalPos(start, BaseVehicle.allocVector3f());
        Vector3f localEnd = this.getLocalPos(end, BaseVehicle.allocVector3f());
        Vector3f intersect = this.getIntersectPoint(localStart, localEnd, extents, result);
        BaseVehicle.releaseVector3f(localStart);
        BaseVehicle.releaseVector3f(localEnd);
        BaseVehicle.releaseVector3f(extents);
        if (intersect == null) {
            return null;
        }
        return this.getWorldPos(intersect, intersect);
    }

    private Vector3f getIntersectPoint(Vector3f start, Vector3f end, Vector3f extents, Vector3f result) {
        Vector3f max = BaseVehicle.allocVector3f().set(extents);
        Vector3f min = BaseVehicle.allocVector3f().set(extents).mul(-1.0f);
        float tmin = 0.0f;
        float tmax = 1.0f;
        Vector3f direction = BaseVehicle.allocVector3f().set(end).sub(start);
        for (int i = 0; i < 3; ++i) {
            float startCoord = start.get(i);
            float minCoord = min.get(i);
            float maxCoord = max.get(i);
            float directionCoord = direction.get(i);
            if ((double)Math.abs(directionCoord) < 1.0E-6) {
                if (!(startCoord < minCoord) && !(startCoord > maxCoord)) continue;
                BaseVehicle.releaseVector3f(min);
                BaseVehicle.releaseVector3f(max);
                BaseVehicle.releaseVector3f(direction);
                return null;
            }
            float t1 = (minCoord - startCoord) / directionCoord;
            float t2 = (maxCoord - startCoord) / directionCoord;
            if (t1 > t2) {
                float temp = t1;
                t1 = t2;
                t2 = temp;
            }
            if (!((tmin = Math.max(tmin, t1)) > (tmax = Math.min(tmax, t2)))) continue;
            BaseVehicle.releaseVector3f(min);
            BaseVehicle.releaseVector3f(max);
            BaseVehicle.releaseVector3f(direction);
            return null;
        }
        result.set(start).add(direction.mul(tmin));
        BaseVehicle.releaseVector3f(min);
        BaseVehicle.releaseVector3f(max);
        BaseVehicle.releaseVector3f(direction);
        return result;
    }

    public VehiclePart getNearestVehiclePart(float x, float y, float z, boolean useDestroyed) {
        Vector3f worldPosition = (Vector3f)TL_vector3f_pool.get().alloc();
        Vector3f areaWorldPosition = (Vector3f)TL_vector3f_pool.get().alloc();
        worldPosition.set(x, y, z);
        VehiclePart nearestVehiclePart = null;
        float minDistance = Float.MAX_VALUE;
        for (int i = 0; i < this.script.getAreaCount(); ++i) {
            VehicleScript.Area area = this.script.getArea(i);
            String id = area.getId();
            VehiclePart vehiclePart = this.getPartById(id);
            if (vehiclePart == null || !useDestroyed && vehiclePart.condition == 0 || !this.isInArea(id, worldPosition)) continue;
            this.getWorldPos(area.x, area.y, z, areaWorldPosition);
            float distance = worldPosition.distance(areaWorldPosition);
            if (!(distance < minDistance)) continue;
            minDistance = distance;
            nearestVehiclePart = vehiclePart;
        }
        TL_vector3f_pool.get().release(areaWorldPosition);
        TL_vector3f_pool.get().release(worldPosition);
        return nearestVehiclePart;
    }

    public boolean isInArea(String areaId, Vector3f chr) {
        if (areaId == null || this.getScript() == null) {
            return false;
        }
        VehicleScript.Area area = this.getScript().getAreaById(areaId);
        if (area == null) {
            return false;
        }
        Vector2 vector2 = (Vector2)Vector2ObjectPool.get().alloc();
        Vector2 center = this.areaPositionLocal(area, vector2);
        if (center == null) {
            Vector2ObjectPool.get().release(vector2);
            return false;
        }
        Vector3f localPos = (Vector3f)TL_vector3f_pool.get().alloc();
        this.getLocalPos(chr.x, chr.y, this.getZ(), localPos);
        float minX = center.x - (area.w + 0.01f) * 0.5f;
        float minY = center.y - (area.h + 0.01f) * 0.5f;
        float maxX = center.x + (area.w + 0.01f) * 0.5f;
        float maxY = center.y + (area.h + 0.01f) * 0.5f;
        boolean inside = localPos.x >= minX && localPos.x < maxX && localPos.z >= minY && localPos.z < maxY;
        Vector2ObjectPool.get().release(vector2);
        TL_vector3f_pool.get().release(localPos);
        return inside;
    }

    private boolean processRangeHit(IsoGameCharacter isoGameCharacter, HandWeapon weapon, float damage) {
        float range = weapon.getMaxRange(isoGameCharacter);
        Vector3f start = BaseVehicle.allocVector3f();
        Vector3f end = BaseVehicle.allocVector3f();
        float renderedAngle = isoGameCharacter.getLookAngleRadians();
        Vector3f directionVector = BaseVehicle.allocVector3f();
        directionVector.set((float)Math.cos(renderedAngle), (float)Math.sin(renderedAngle), 0.0f);
        directionVector.normalize();
        if (GameServer.server) {
            start.set(isoGameCharacter.getX(), isoGameCharacter.getY(), isoGameCharacter.getZ());
        } else {
            BallisticsController ballisticsController = isoGameCharacter.getBallisticsController();
            Vector3 muzzlePosition = ballisticsController.getMuzzlePosition();
            start.set(muzzlePosition.x, muzzlePosition.y, muzzlePosition.z);
        }
        end.set(start.x() + directionVector.x() * range, start.y() + directionVector.y() * range, start.z() + directionVector.z() * range);
        Vector3f intersect = BaseVehicle.allocVector3f();
        boolean bIntersected = this.getIntersectPoint(start, end, intersect) != null;
        BaseVehicle.releaseVector3f(start);
        BaseVehicle.releaseVector3f(end);
        BaseVehicle.releaseVector3f(directionVector);
        BaseVehicle.releaseVector3f(intersect);
        if (!bIntersected) {
            return false;
        }
        VehiclePart vehiclePart = this.getPartByDirection(isoGameCharacter.getX(), isoGameCharacter.getY(), isoGameCharacter.getZ());
        if (vehiclePart == null) {
            return false;
        }
        this.applyDamageToPart(isoGameCharacter, weapon, vehiclePart, damage);
        return true;
    }

    private boolean processMeleeHit(IsoGameCharacter isoGameCharacter, HandWeapon weapon, float damage) {
        VehiclePart vehiclePart;
        if (isoGameCharacter instanceof IsoPlayer) {
            IsoPlayer isoPlayer = (IsoPlayer)isoGameCharacter;
            isoPlayer.setVehicleHitLocation(this);
        }
        if ((vehiclePart = this.getNearestBodyworkPart(isoGameCharacter)) == null) {
            return false;
        }
        this.applyDamageToPart(isoGameCharacter, weapon, vehiclePart, damage);
        return true;
    }

    private void applyDamageToPart(IsoGameCharacter isoGameCharacter, HandWeapon weapon, VehiclePart vehiclePart, float damage) {
        if (vehiclePart != null) {
            float calculatedDamage;
            VehicleWindow window = vehiclePart.getWindow();
            for (int i = 0; i < vehiclePart.getChildCount(); ++i) {
                VehiclePart child = vehiclePart.getChild(i);
                if (child == null || child.getWindow() == null) continue;
                window = child.getWindow();
                break;
            }
            if (vehiclePart.light != null) {
                calculatedDamage = CombatManager.getInstance().calculateDamageToVehicle(isoGameCharacter, vehiclePart.getDurability(), damage, weapon.getDoorDamage());
                vehiclePart.setCondition(vehiclePart.getCondition() - (int)calculatedDamage);
                if (GameServer.server) {
                    this.transmitPartItem(vehiclePart);
                    GameServer.PlayWorldSoundServer(isoGameCharacter, "HitVehicleWindowWithWeapon", false, vehiclePart.getSquare(), 0.2f, 10.0f, 1.1f, true);
                } else if (!GameClient.client) {
                    isoGameCharacter.playSound("HitVehicleWindowWithWeapon");
                }
            } else if (window != null && window.isHittable()) {
                calculatedDamage = CombatManager.getInstance().calculateDamageToVehicle(isoGameCharacter, vehiclePart.getDurability(), damage, weapon.getDoorDamage());
                window.damage((int)calculatedDamage);
                if (GameServer.server) {
                    this.transmitPartWindow(vehiclePart);
                    GameServer.PlayWorldSoundServer(isoGameCharacter, "HitVehicleWindowWithWeapon", false, vehiclePart.getSquare(), 0.2f, 10.0f, 1.1f, true);
                } else if (!GameClient.client) {
                    isoGameCharacter.playSound("HitVehicleWindowWithWeapon");
                }
            } else {
                calculatedDamage = CombatManager.getInstance().calculateDamageToVehicle(isoGameCharacter, vehiclePart.getDurability(), damage, weapon.getDoorDamage());
                vehiclePart.setCondition(vehiclePart.getCondition() - (int)calculatedDamage);
                if (GameServer.server) {
                    this.transmitPartItem(vehiclePart);
                    GameServer.PlayWorldSoundServer(isoGameCharacter, "HitVehiclePartWithWeapon", false, vehiclePart.getSquare(), 0.2f, 10.0f, 1.1f, true);
                } else if (!GameClient.client) {
                    isoGameCharacter.playSound("HitVehiclePartWithWeapon");
                }
            }
            vehiclePart.updateFlags = (short)(vehiclePart.updateFlags | 0x800);
            this.updateFlags = (short)(this.updateFlags | 0x800);
            DebugType.Combat.debugln("VehiclePart = %s : durability = %f : damage = %f : conditionalDamage = %f", vehiclePart.getId(), Float.valueOf(vehiclePart.getDurability()), Float.valueOf(damage), Float.valueOf(calculatedDamage));
        }
    }

    public boolean processHit(IsoGameCharacter isoGameCharacter, HandWeapon weapon, float damage) {
        if (weapon.isRanged()) {
            return this.processRangeHit(isoGameCharacter, weapon, damage);
        }
        return this.processMeleeHit(isoGameCharacter, weapon, damage);
    }

    private VehiclePart getPartByDirection(float x, float y, float z) {
        Vector3f v = (Vector3f)TL_vector3f_pool.get().alloc();
        this.getLocalPos(x, y, z, v);
        x = v.x;
        z = v.z;
        TL_vector3f_pool.get().release(v);
        Vector3f extents = this.script.getExtents();
        Vector3f centerOfMassOffset = this.script.getCenterOfMassOffset();
        float xMin = centerOfMassOffset.x - extents.x * 0.5f;
        float xMax = centerOfMassOffset.x + extents.x * 0.5f;
        float yMin = centerOfMassOffset.z - extents.z * 0.5f;
        float yMax = centerOfMassOffset.z + extents.z * 0.5f;
        if (x < xMin * 0.98f) {
            return this.getWeightedRandomSidePart("Right");
        }
        if (x > xMax * 0.98f) {
            return this.getWeightedRandomSidePart("Left");
        }
        if (z < yMin * 0.98f) {
            return this.getWeightedRandomRearPart();
        }
        if (z > yMax * 0.98f) {
            return this.getWeightedRandomFrontPart();
        }
        return this.getAnyRandomPart();
    }

    private void buildVehiclePartList(String partId, float weight, ArrayList<WeightedVehiclePart> weightedVehiclePartArrayList) {
        WeightedVehiclePart weightedVehiclePart = new WeightedVehiclePart(this);
        weightedVehiclePart.vehiclePart = this.getPartById(partId);
        weightedVehiclePart.weight = weight;
        if (weightedVehiclePart.vehiclePart == null) {
            return;
        }
        if (weightedVehiclePart.vehiclePart.condition == 0) {
            return;
        }
        weightedVehiclePartArrayList.add(weightedVehiclePart);
    }

    private VehiclePart getAnyRandomPart() {
        ArrayList<VehiclePart> activeVehiclePartList = new ArrayList<VehiclePart>();
        for (int i = 0; i < this.getPartCount(); ++i) {
            VehiclePart vehiclePart = this.getPartByIndex(i);
            if (vehiclePart == null || vehiclePart.condition == 0) continue;
            activeVehiclePartList.add(vehiclePart);
        }
        if (!activeVehiclePartList.isEmpty()) {
            return (VehiclePart)activeVehiclePartList.get(Rand.Next(0, activeVehiclePartList.size()));
        }
        return null;
    }

    private boolean isGasTakeSide(String side) {
        if (side.contains("Left") && this.leftSideFuel()) {
            return true;
        }
        return side.contains("Right") && this.rightSideFuel();
    }

    private VehiclePart getWeightedRandomSidePart(String side) {
        ArrayList<WeightedVehiclePart> activeVehiclePartList = new ArrayList<WeightedVehiclePart>();
        for (int i = 0; i < this.getPartCount(); ++i) {
            VehiclePart part = this.getPartByIndex(i);
            if (!part.getId().contains(side) && !this.isGasTakeSide(side)) continue;
            this.buildVehiclePartList(part.getId(), 1.0f, activeVehiclePartList);
        }
        if (side.equals("Right")) {
            this.buildVehiclePartList("WindowRearRight", 19.0f, activeVehiclePartList);
            this.buildVehiclePartList("WindowFrontRight", 19.0f, activeVehiclePartList);
            this.buildVehiclePartList("TireRearRight", 19.0f, activeVehiclePartList);
            this.buildVehiclePartList("TireFrontRight", 19.0f, activeVehiclePartList);
            this.buildVehiclePartList("HeadlightRight", 19.0f, activeVehiclePartList);
            this.buildVehiclePartList("DoorFrontRight", 19.0f, activeVehiclePartList);
            this.buildVehiclePartList("DoorRearRight", 19.0f, activeVehiclePartList);
            this.buildVehiclePartList("HeadlightRearRight", 19.0f, activeVehiclePartList);
        } else {
            this.buildVehiclePartList("WindowRearLeft", 19.0f, activeVehiclePartList);
            this.buildVehiclePartList("WindowFrontLeft", 19.0f, activeVehiclePartList);
            this.buildVehiclePartList("TireRearLeft", 19.0f, activeVehiclePartList);
            this.buildVehiclePartList("TireFrontLeft", 19.0f, activeVehiclePartList);
            this.buildVehiclePartList("HeadlightLeft", 19.0f, activeVehiclePartList);
            this.buildVehiclePartList("DoorFrontLeft", 19.0f, activeVehiclePartList);
            this.buildVehiclePartList("DoorRearLeft", 19.0f, activeVehiclePartList);
            this.buildVehiclePartList("HeadlightRearLeft", 19.0f, activeVehiclePartList);
        }
        this.buildVehiclePartList("GasTank", 9.0f, activeVehiclePartList);
        this.buildVehiclePartList("lightbar", 20.0f, activeVehiclePartList);
        if (!activeVehiclePartList.isEmpty()) {
            return this.getWeightedRandomPart(activeVehiclePartList);
        }
        return null;
    }

    private VehiclePart getWeightedRandomFrontPart() {
        ArrayList<WeightedVehiclePart> activeVehiclePartList = new ArrayList<WeightedVehiclePart>();
        for (int i = 0; i < this.getPartCount(); ++i) {
            VehiclePart part = this.getPartByIndex(i);
            if (!part.getId().contains("Front")) continue;
            this.buildVehiclePartList(part.getId(), 1.0f, activeVehiclePartList);
        }
        this.buildVehiclePartList("HeadlightRight", 20.0f, activeVehiclePartList);
        this.buildVehiclePartList("HeadlightLeft", 20.0f, activeVehiclePartList);
        this.buildVehiclePartList("TireFrontRight", 19.0f, activeVehiclePartList);
        this.buildVehiclePartList("TireFrontLeft", 19.0f, activeVehiclePartList);
        this.buildVehiclePartList("Engine", 5.0f, activeVehiclePartList);
        this.buildVehiclePartList("EngineDoor", 10.0f, activeVehiclePartList);
        this.buildVehiclePartList("Battery", 5.0f, activeVehiclePartList);
        this.buildVehiclePartList("Windshield", 20.0f, activeVehiclePartList);
        this.buildVehiclePartList("Heater", 2.0f, activeVehiclePartList);
        this.buildVehiclePartList("Hood", 10.0f, activeVehiclePartList);
        this.buildVehiclePartList("Radio", 0.5f, activeVehiclePartList);
        this.buildVehiclePartList("GloveBox", 0.5f, activeVehiclePartList);
        this.buildVehiclePartList("lightbar", 20.0f, activeVehiclePartList);
        if (!activeVehiclePartList.isEmpty()) {
            return this.getWeightedRandomPart(activeVehiclePartList);
        }
        return null;
    }

    private VehiclePart getWeightedRandomRearPart() {
        ArrayList<WeightedVehiclePart> activeVehiclePartList = new ArrayList<WeightedVehiclePart>();
        for (int i = 0; i < this.getPartCount(); ++i) {
            VehiclePart part = this.getPartByIndex(i);
            if (!part.getId().contains("Rear")) continue;
            this.buildVehiclePartList(part.getId(), 1.0f, activeVehiclePartList);
        }
        this.buildVehiclePartList("HeadlightRearRight", 19.0f, activeVehiclePartList);
        this.buildVehiclePartList("HeadlightRearLeft", 19.0f, activeVehiclePartList);
        this.buildVehiclePartList("TireRearRight", 19.0f, activeVehiclePartList);
        this.buildVehiclePartList("TireRearLeft", 19.0f, activeVehiclePartList);
        this.buildVehiclePartList("WindshieldRear", 19.0f, activeVehiclePartList);
        this.buildVehiclePartList("TruckBed", 5.0f, activeVehiclePartList);
        this.buildVehiclePartList("TrunkDoor", 10.0f, activeVehiclePartList);
        this.buildVehiclePartList("DoorRear", 9.0f, activeVehiclePartList);
        this.buildVehiclePartList("Muffler", 0.5f, activeVehiclePartList);
        this.buildVehiclePartList("lightbar", 20.0f, activeVehiclePartList);
        if (!activeVehiclePartList.isEmpty()) {
            return this.getWeightedRandomPart(activeVehiclePartList);
        }
        return null;
    }

    private VehiclePart getWeightedRandomPart(ArrayList<WeightedVehiclePart> weightedVehiclePartList) {
        float totalWeight = 0.0f;
        for (WeightedVehiclePart weightedVehiclePart : weightedVehiclePartList) {
            totalWeight += weightedVehiclePart.weight;
        }
        float randomValue = Rand.Next(0.0f, 1.0f) * totalWeight;
        for (WeightedVehiclePart weightedVehiclePart : weightedVehiclePartList) {
            if (!((randomValue -= weightedVehiclePart.weight) <= 0.0f)) continue;
            return weightedVehiclePart.vehiclePart;
        }
        return null;
    }

    public boolean canAddAnimalInTrailer(IsoAnimal animal) {
        return this.getAnimalTrailerSize() >= this.getCurrentTotalAnimalSize() + animal.getAnimalTrailerSize();
    }

    public boolean canAddAnimalInTrailer(IsoDeadBody animal) {
        return this.getAnimalTrailerSize() >= this.getCurrentTotalAnimalSize() + ((KahluaTableImpl)animal.getModData()).rawgetFloat("animalTrailerSize");
    }

    public boolean isBurnt() {
        return this.getScriptName().contains("Burnt");
    }

    public boolean isSmashed() {
        return this.getScriptName().contains("Smashed");
    }

    public boolean isBurntOrSmashed() {
        return this.isBurnt() || this.isSmashed();
    }

    public float getSpecialKeyRingChance() {
        if (!this.getScript().hasSpecialKeyRing()) {
            return this.keySpawnChancedD100;
        }
        return Math.max(this.keySpawnChancedD100, (float)this.getScript().getSpecialKeyRingChance());
    }

    public boolean hasLiveBattery() {
        return this.getPartById("Battery") != null && this.getPartById("Battery").getInventoryItem() != null && this.getBatteryCharge() > 0.0f;
    }

    public void setDebugPhysicsRender(boolean addedToWorld) {
        if (addedToWorld) {
            PhysicsDebugRenderer.addVehicleRender(this);
        } else {
            PhysicsDebugRenderer.removeVehicleRender(this);
        }
    }

    public boolean testTouchingVehicle(IsoGameCharacter isoGameCharacter, RagdollController ragdollController) {
        if (isoGameCharacter == null) {
            return false;
        }
        if (Math.abs(this.getX() - isoGameCharacter.getX()) < 0.01f || Math.abs(this.getY() - isoGameCharacter.getY()) < 0.01f) {
            return false;
        }
        return !(ragdollController.getPelvisPositionZ() < this.getZ() + 0.25f);
    }

    public IsoGameCharacter getCurrentOrLastKnownDriver() {
        IsoGameCharacter isoGameCharacter = this.getDriverRegardlessOfTow();
        return isoGameCharacter != null ? isoGameCharacter : this.lastDrivenBy;
    }

    public IsoGridSquare getSquareForArea(String areaId) {
        Vector2 areaCenter = this.getAreaCenter(areaId);
        if (areaCenter == null) {
            return this.getSquare();
        }
        return this.getCell().getGridSquare(areaCenter.getX(), areaCenter.getY(), (double)PZMath.fastfloor(this.getZ()));
    }

    public void partsClear() {
        this.parts.clear();
    }

    static {
        centerOfMassMagic = 0.7f;
        wheelParams = new float[24];
        physicsParams = new float[27];
        inf = new ColorInfo();
        lowRiderParam = new float[4];
        s_PartToMaskMap = new HashMap();
        BYTE_ZERO = 0;
        TL_transform_pool = ThreadLocal.withInitial(TransformPool::new);
        TL_vector3_pool = ThreadLocal.withInitial(Vector3ObjectPool::new);
        TL_vector2f_pool = ThreadLocal.withInitial(Vector2fObjectPool::new);
        TL_vector3f_pool = ThreadLocal.withInitial(Vector3fObjectPool::new);
        TL_vector4f_pool = ThreadLocal.withInitial(Vector4fObjectPool::new);
        TL_matrix4f_pool = ThreadLocal.withInitial(Matrix4fObjectPool::new);
        TL_quaternionf_pool = ThreadLocal.withInitial(QuaternionfObjectPool::new);
    }

    public static final class Matrix4fObjectPool
    extends ObjectPool<Matrix4f> {
        private int allocated;

        private Matrix4fObjectPool() {
            super(Matrix4f::new);
        }

        @Override
        protected Matrix4f makeObject() {
            ++this.allocated;
            return (Matrix4f)super.makeObject();
        }
    }

    public static final class QuaternionfObjectPool
    extends ObjectPool<Quaternionf> {
        private int allocated;

        private QuaternionfObjectPool() {
            super(Quaternionf::new);
        }

        @Override
        protected Quaternionf makeObject() {
            ++this.allocated;
            return (Quaternionf)super.makeObject();
        }
    }

    public static final class TransformPool
    extends ObjectPool<Transform> {
        private int allocated;

        private TransformPool() {
            super(Transform::new);
        }

        @Override
        protected Transform makeObject() {
            ++this.allocated;
            return (Transform)super.makeObject();
        }
    }

    public static final class Vector3ObjectPool
    extends ObjectPool<Vector3> {
        private int allocated;

        private Vector3ObjectPool() {
            super(Vector3::new);
        }

        @Override
        protected Vector3 makeObject() {
            ++this.allocated;
            return (Vector3)super.makeObject();
        }
    }

    public static final class Vector2fObjectPool
    extends ObjectPool<Vector2f> {
        private int allocated;

        private Vector2fObjectPool() {
            super(Vector2f::new);
        }

        @Override
        protected Vector2f makeObject() {
            ++this.allocated;
            return (Vector2f)super.makeObject();
        }
    }

    public static final class Vector3fObjectPool
    extends ObjectPool<Vector3f> {
        private int allocated;

        private Vector3fObjectPool() {
            super(Vector3f::new);
        }

        @Override
        protected Vector3f makeObject() {
            ++this.allocated;
            return (Vector3f)super.makeObject();
        }
    }

    public static final class Vector4fObjectPool
    extends ObjectPool<Vector4f> {
        private int allocated;

        private Vector4fObjectPool() {
            super(Vector4f::new);
        }

        @Override
        protected Vector4f makeObject() {
            ++this.allocated;
            return (Vector4f)super.makeObject();
        }
    }

    private static final class VehicleImpulse {
        private static final ArrayDeque<VehicleImpulse> pool = new ArrayDeque();
        private final Vector3f impulse = new Vector3f();
        private final Vector3f relPos = new Vector3f();
        private boolean enable;
        private boolean applied;

        private VehicleImpulse() {
        }

        private static VehicleImpulse alloc() {
            return pool.isEmpty() ? new VehicleImpulse() : pool.pop();
        }

        private void release() {
            pool.push(this);
        }
    }

    public static enum Authorization {
        Server,
        LocalCollide,
        RemoteCollide,
        Local,
        Remote;

    }

    public static enum engineStateTypes {
        Idle,
        Starting,
        RetryingStarting,
        StartingSuccess,
        StartingFailed,
        Running,
        Stalling,
        ShutingDown,
        StartingFailedNoPower;

        public static final engineStateTypes[] Values;

        static {
            Values = engineStateTypes.values();
        }
    }

    public static final class WheelInfo {
        public float steering;
        public float rotation;
        public float skidInfo;
        public float suspensionLength;
    }

    public static final class ServerVehicleState {
        public float x = -1.0f;
        public float y;
        public float z;
        public Quaternionf orient = new Quaternionf();
        public short flags = 0;
        public Authorization netPlayerAuthorization = Authorization.Server;
        public short netPlayerId;

        public void setAuthorization(BaseVehicle vehicle) {
            this.netPlayerAuthorization = vehicle.netPlayerAuthorization;
            this.netPlayerId = vehicle.netPlayerId;
        }

        public boolean shouldSend(BaseVehicle vehicle) {
            if (vehicle.getController() == null) {
                return false;
            }
            if (vehicle.updateLockTimeout > System.currentTimeMillis()) {
                return false;
            }
            this.flags = (short)(this.flags & 1);
            if (!vehicle.isNetPlayerAuthorization(this.netPlayerAuthorization) || !vehicle.isNetPlayerId(this.netPlayerId)) {
                this.flags = (short)(this.flags | 0x2000);
            }
            this.flags = (short)(this.flags | vehicle.updateFlags);
            return this.flags != 0;
        }
    }

    public static final class Passenger {
        public IsoGameCharacter character;
        private final Vector3f offset = new Vector3f();
    }

    public static class HitVars {
        private static final float speedCap = 10.0f;
        private final Vector3f velocity = new Vector3f();
        public final Vector2 collision = new Vector2();
        public boolean isPushedBack;
        public float damageToPedestrian;
        private float dot;
        protected float vehicleImpulse;
        protected float vehicleSpeed;
        public final Vector3f targetImpulse = new Vector3f();
        public boolean isVehicleHitFromFront;
        public boolean isTargetHitFromBehind;
        public Side hitFromSide;
        public int vehicleDamage;
        public float hitSpeed;

        public void calc(IsoGameCharacter target, BaseVehicle vehicle) {
            vehicle.getLinearVelocity(this.velocity);
            this.velocity.y = 0.0f;
            if (target instanceof IsoZombie) {
                this.vehicleSpeed = Math.min(this.velocity.length(), 10.0f);
                this.hitSpeed = this.vehicleSpeed + vehicle.getClientForce() / vehicle.getFudgedMass();
            } else {
                this.vehicleSpeed = (float)Math.sqrt(this.velocity.x * this.velocity.x + this.velocity.z * this.velocity.z);
                this.hitSpeed = target.isOnFloor() ? Math.max(this.vehicleSpeed * 6.0f, 5.0f) : Math.max(this.vehicleSpeed * 2.0f, 5.0f);
            }
            this.targetImpulse.set(vehicle.getX() - target.getX(), 0.0f, vehicle.getY() - target.getY());
            this.targetImpulse.normalize();
            this.velocity.normalize();
            this.dot = this.velocity.dot(this.targetImpulse);
            this.targetImpulse.normalize();
            this.targetImpulse.mul(3.0f * this.vehicleSpeed / 10.0f);
            this.targetImpulse.set(this.targetImpulse.x, this.targetImpulse.y, this.targetImpulse.z);
            this.vehicleImpulse = vehicle.getFudgedMass() * 7.0f * this.vehicleSpeed / 10.0f * Math.abs(this.dot);
            this.hitFromSide = target.testDotSideEnum(vehicle);
            this.isVehicleHitFromFront = this.hitFromSide == Side.FRONT;
            this.isTargetHitFromBehind = this.hitFromSide == Side.BEHIND;
        }
    }

    public static final class ModelInfo {
        public VehiclePart part;
        public VehicleScript.Model scriptModel;
        public ModelScript modelScript;
        public int wheelIndex;
        public final Matrix4f renderTransform = new Matrix4f();
        public VehicleSubModelInstance modelInstance;
        public AnimationPlayer animPlayer;
        public AnimationTrack track;

        public AnimationPlayer getAnimationPlayer() {
            ModelInfo modelInfoParent;
            if (this.part != null && this.part.getParent() != null && (modelInfoParent = this.part.getVehicle().getModelInfoForPart(this.part.getParent())) != null) {
                return modelInfoParent.getAnimationPlayer();
            }
            String modelName = this.scriptModel.file;
            Model model = ModelManager.instance.getLoadedModel(modelName);
            if (model == null || model.isStatic) {
                return null;
            }
            if (this.animPlayer != null && this.animPlayer.getModel() != model) {
                this.animPlayer = Pool.tryRelease(this.animPlayer);
            }
            if (this.animPlayer == null) {
                this.animPlayer = AnimationPlayer.alloc(model);
            }
            return this.animPlayer;
        }

        public void releaseAnimationPlayer() {
            this.animPlayer = Pool.tryRelease(this.animPlayer);
        }
    }

    public static class UpdateFlags {
        public static final short Full = 1;
        public static final short PositionOrientation = 2;
        public static final short Engine = 4;
        public static final short Lights = 8;
        public static final short PartModData = 16;
        public static final short PartUsedDelta = 32;
        public static final short PartModels = 64;
        public static final short PartItem = 128;
        public static final short PartWindow = 256;
        public static final short PartDoor = 512;
        public static final short Sounds = 1024;
        public static final short PartCondition = 2048;
        public static final short UpdateCarProperties = 4096;
        public static final short Authorization = 8192;
        public static final short Passengers = 16384;
        public static final short AllPartFlags = 3056;
    }

    private static final class L_testCollisionWithVehicle {
        private static final Vector2[] testVecs1 = new Vector2[4];
        private static final Vector2[] testVecs2 = new Vector2[4];
        private static final Vector3f worldPos = new Vector3f();

        private L_testCollisionWithVehicle() {
        }
    }

    public static final class MinMaxPosition {
        public float minX;
        public float maxX;
        public float minY;
        public float maxY;
    }

    private class WeightedVehiclePart {
        public VehiclePart vehiclePart;
        public float weight;

        private WeightedVehiclePart(BaseVehicle baseVehicle) {
            Objects.requireNonNull(baseVehicle);
            this.weight = 1.0f;
        }
    }
}

