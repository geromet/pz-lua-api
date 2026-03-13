/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.hash.THashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.SystemDisabler;
import zombie.UsedFromLua;
import zombie.core.BoxedStaticValues;
import zombie.core.ImmutableColor;
import zombie.core.math.PZMath;
import zombie.core.physics.Bullet;
import zombie.core.random.Rand;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.IModelAttachmentOwner;
import zombie.scripting.objects.ItemKey;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.VehicleTemplate;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public final class VehicleScript
extends BaseScriptObject
implements IModelAttachmentOwner {
    private String fileName;
    private String name;
    private final ArrayList<Model> models = new ArrayList();
    public final ArrayList<ModelAttachment> attachments = new ArrayList();
    private float mass = 800.0f;
    private final Vector3f centerOfMassOffset = new Vector3f();
    private float engineForce = 3000.0f;
    private float engineIdleSpeed = 750.0f;
    private float steeringIncrement = 0.04f;
    private float steeringClamp = 0.4f;
    private final float steeringClampMax = 0.9f;
    private float wheelFriction = 800.0f;
    private float stoppingMovementForce = 1.0f;
    private float animalTrailerSize;
    private float suspensionStiffness = 20.0f;
    private float suspensionDamping = 2.3f;
    private float suspensionCompression = 4.4f;
    private float suspensionRestLength = 0.6f;
    private float maxSuspensionTravelCm = 500.0f;
    private float rollInfluence = 0.1f;
    private final Vector3f extents = new Vector3f(0.75f, 0.5f, 2.0f);
    private final Vector2f shadowExtents = new Vector2f(0.0f, 0.0f);
    private final Vector2f shadowOffset = new Vector2f(0.0f, 0.0f);
    private boolean hadShadowOExtents;
    private boolean hadShadowOffset;
    private final Vector2f extentsOffset = new Vector2f(0.5f, 0.5f);
    private final Vector3f physicsChassisShape = new Vector3f(0.0f);
    private final ArrayList<PhysicsShape> physicsShapes = new ArrayList();
    private final ArrayList<Wheel> wheels = new ArrayList();
    private final ArrayList<Passenger> passengers = new ArrayList();
    public float maxSpeed = 20.0f;
    private boolean useChassisPhysicsCollision = true;
    public float maxSpeedReverse = 40.0f;
    public boolean isSmallVehicle = true;
    private int frontEndHealth = 100;
    private int rearEndHealth = 100;
    private int storageCapacity = 100;
    private int engineLoudness = 100;
    private int engineQuality = 100;
    private int seats = 2;
    private int mechanicType;
    private int engineRepairLevel;
    private float playerDamageProtection;
    private float forcedHue = -1.0f;
    private float forcedSat = -1.0f;
    private float forcedVal = -1.0f;
    public ImmutableColor leftSirenCol;
    public ImmutableColor rightSirenCol;
    private String engineRpmType = "jeep";
    private float offroadEfficiency = 1.0f;
    private final TFloatArrayList crawlOffsets = new TFloatArrayList();
    private ArrayList<String> zombieType;
    private ArrayList<String> specialKeyRing;
    private boolean notKillCrops;
    private boolean hasLighter = true;
    private String carMechanicsOverlay;
    private String carModelName;
    private int specialLootChance = 8;
    private int specialKeyRingChance;
    private boolean neverSpawnKey;
    public int gearRatioCount = 4;
    public final float[] gearRatio = new float[9];
    private final Skin textures = new Skin();
    private final ArrayList<Skin> skins = new ArrayList();
    private final ArrayList<Area> areas = new ArrayList();
    private final ArrayList<Part> parts = new ArrayList();
    private boolean hasSiren;
    private final LightBar lightbar = new LightBar();
    private final Sounds sound = new Sounds();
    public boolean textureMaskEnable;
    public static final int PHYSICS_SHAPE_BOX = 1;
    public static final int PHYSICS_SHAPE_SPHERE = 2;
    public static final int PHYSICS_SHAPE_MESH = 3;

    public VehicleScript() {
        super(ScriptType.Vehicle);
        this.gearRatio[0] = 7.09f;
        this.gearRatio[1] = 6.44f;
        this.gearRatio[2] = 4.1f;
        this.gearRatio[3] = 2.29f;
        this.gearRatio[4] = 1.47f;
        this.gearRatio[5] = 1.0f;
    }

    @Override
    public void InitLoadPP(String name) {
        super.InitLoadPP(name);
        ScriptManager scriptMgr = ScriptManager.instance;
        this.fileName = scriptMgr.currentFileName;
        this.name = name;
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        this.LoadCommonBlock(block);
        for (ScriptParser.BlockElement element : block.elements) {
            String k;
            String pattern;
            if (element.asValue() != null) {
                int i;
                String[] split;
                String[] ss = element.asValue().string.split("=");
                String k2 = ss[0].trim();
                Iterator<Passenger> v = ss[1].trim();
                if ("extents".equals(k2)) {
                    this.LoadVector3f((String)((Object)v), this.extents);
                    continue;
                }
                if ("shadowExtents".equals(k2)) {
                    this.LoadVector2f((String)((Object)v), this.shadowExtents);
                    this.hadShadowOExtents = true;
                    continue;
                }
                if ("shadowOffset".equals(k2)) {
                    this.LoadVector2f((String)((Object)v), this.shadowOffset);
                    this.hadShadowOffset = true;
                    continue;
                }
                if ("physicsChassisShape".equals(k2)) {
                    this.LoadVector3f((String)((Object)v), this.physicsChassisShape);
                    continue;
                }
                if ("extentsOffset".equals(k2)) {
                    this.LoadVector2f((String)((Object)v), this.extentsOffset);
                    continue;
                }
                if ("mass".equals(k2)) {
                    this.mass = Float.parseFloat((String)((Object)v));
                    continue;
                }
                if ("offRoadEfficiency".equalsIgnoreCase(k2)) {
                    this.offroadEfficiency = Float.parseFloat(v);
                    continue;
                }
                if ("centerOfMassOffset".equals(k2)) {
                    this.LoadVector3f((String)((Object)v), this.centerOfMassOffset);
                    continue;
                }
                if ("engineForce".equals(k2)) {
                    this.engineForce = Float.parseFloat(v);
                    continue;
                }
                if ("engineIdleSpeed".equals(k2)) {
                    this.engineIdleSpeed = Float.parseFloat(v);
                    continue;
                }
                if ("gearRatioCount".equals(k2)) {
                    this.gearRatioCount = Integer.parseInt(v);
                    continue;
                }
                if ("gearRatioR".equals(k2)) {
                    this.gearRatio[0] = Float.parseFloat(v);
                    continue;
                }
                if ("gearRatio1".equals(k2)) {
                    this.gearRatio[1] = Float.parseFloat(v);
                    continue;
                }
                if ("gearRatio2".equals(k2)) {
                    this.gearRatio[2] = Float.parseFloat(v);
                    continue;
                }
                if ("gearRatio3".equals(k2)) {
                    this.gearRatio[3] = Float.parseFloat(v);
                    continue;
                }
                if ("gearRatio4".equals(k2)) {
                    this.gearRatio[4] = Float.parseFloat(v);
                    continue;
                }
                if ("gearRatio5".equals(k2)) {
                    this.gearRatio[5] = Float.parseFloat(v);
                    continue;
                }
                if ("gearRatio6".equals(k2)) {
                    this.gearRatio[6] = Float.parseFloat(v);
                    continue;
                }
                if ("gearRatio7".equals(k2)) {
                    this.gearRatio[7] = Float.parseFloat(v);
                    continue;
                }
                if ("gearRatio8".equals(k2)) {
                    this.gearRatio[8] = Float.parseFloat(v);
                    continue;
                }
                if ("textureMaskEnable".equals(k2)) {
                    this.textureMaskEnable = Boolean.parseBoolean(v);
                    continue;
                }
                if ("textureRust".equals(k2)) {
                    this.textures.textureRust = StringUtils.discardNullOrWhitespace(v);
                    continue;
                }
                if ("textureMask".equals(k2)) {
                    this.textures.textureMask = StringUtils.discardNullOrWhitespace(v);
                    continue;
                }
                if ("textureLights".equals(k2)) {
                    this.textures.textureLights = StringUtils.discardNullOrWhitespace(v);
                    continue;
                }
                if ("textureDamage1Overlay".equals(k2)) {
                    this.textures.textureDamage1Overlay = StringUtils.discardNullOrWhitespace(v);
                    continue;
                }
                if ("textureDamage1Shell".equals(k2)) {
                    this.textures.textureDamage1Shell = StringUtils.discardNullOrWhitespace(v);
                    continue;
                }
                if ("textureDamage2Overlay".equals(k2)) {
                    this.textures.textureDamage2Overlay = StringUtils.discardNullOrWhitespace(v);
                    continue;
                }
                if ("textureDamage2Shell".equals(k2)) {
                    this.textures.textureDamage2Shell = StringUtils.discardNullOrWhitespace(v);
                    continue;
                }
                if ("textureShadow".equals(k2)) {
                    this.textures.textureShadow = StringUtils.discardNullOrWhitespace(v);
                    continue;
                }
                if ("rollInfluence".equals(k2)) {
                    this.rollInfluence = Float.parseFloat(v);
                    continue;
                }
                if ("steeringIncrement".equals(k2)) {
                    this.steeringIncrement = Float.parseFloat(v);
                    continue;
                }
                if ("steeringClamp".equals(k2)) {
                    this.steeringClamp = Float.parseFloat(v);
                    continue;
                }
                if ("suspensionStiffness".equals(k2)) {
                    this.suspensionStiffness = Float.parseFloat(v);
                    continue;
                }
                if ("suspensionDamping".equals(k2)) {
                    this.suspensionDamping = Float.parseFloat(v);
                    continue;
                }
                if ("suspensionCompression".equals(k2)) {
                    this.suspensionCompression = Float.parseFloat(v);
                    continue;
                }
                if ("suspensionRestLength".equals(k2)) {
                    this.suspensionRestLength = Float.parseFloat(v);
                    continue;
                }
                if ("maxSuspensionTravelCm".equals(k2)) {
                    this.maxSuspensionTravelCm = Float.parseFloat(v);
                    continue;
                }
                if ("wheelFriction".equals(k2)) {
                    this.wheelFriction = Float.parseFloat(v);
                    continue;
                }
                if ("stoppingMovementForce".equals(k2)) {
                    this.stoppingMovementForce = Float.parseFloat(v);
                    continue;
                }
                if ("animalTrailerSize".equalsIgnoreCase(k2)) {
                    this.animalTrailerSize = Float.parseFloat(v);
                    continue;
                }
                if ("maxSpeed".equals(k2)) {
                    this.maxSpeed = Float.parseFloat(v);
                    continue;
                }
                if ("maxSpeedReverse".equals(k2)) {
                    this.maxSpeedReverse = Float.parseFloat(v);
                    continue;
                }
                if ("isSmallVehicle".equals(k2)) {
                    this.isSmallVehicle = Boolean.parseBoolean(v);
                    continue;
                }
                if ("frontEndDurability".equals(k2)) {
                    this.frontEndHealth = Integer.parseInt(v);
                    continue;
                }
                if ("rearEndDurability".equals(k2)) {
                    this.rearEndHealth = Integer.parseInt(v);
                    continue;
                }
                if ("storageCapacity".equals(k2)) {
                    this.storageCapacity = Integer.parseInt(v);
                    continue;
                }
                if ("engineLoudness".equals(k2)) {
                    this.engineLoudness = Integer.parseInt(v);
                    continue;
                }
                if ("engineQuality".equals(k2)) {
                    this.engineQuality = Integer.parseInt(v);
                    continue;
                }
                if ("seats".equals(k2)) {
                    this.seats = Integer.parseInt(v);
                    continue;
                }
                if ("hasSiren".equals(k2)) {
                    this.hasSiren = Boolean.parseBoolean(v);
                    continue;
                }
                if ("mechanicType".equals(k2)) {
                    this.mechanicType = Integer.parseInt(v);
                    continue;
                }
                if ("forcedColor".equals(k2)) {
                    String[] hsv = ((String)((Object)v)).split(" ");
                    this.setForcedHue(Float.parseFloat(hsv[0]));
                    this.setForcedSat(Float.parseFloat(hsv[1]));
                    this.setForcedVal(Float.parseFloat(hsv[2]));
                    continue;
                }
                if ("engineRPMType".equals(k2)) {
                    this.engineRpmType = ((String)((Object)v)).trim();
                    continue;
                }
                if ("zombieType".equals(k2)) {
                    this.zombieType = new ArrayList();
                    split = ((String)((Object)v)).split(";");
                    for (i = 0; i < split.length; ++i) {
                        this.zombieType.add(split[i].trim());
                    }
                    continue;
                }
                if ("specialKeyRing".equals(k2)) {
                    this.specialKeyRing = new ArrayList();
                    split = ((String)((Object)v)).split(";");
                    for (i = 0; i < split.length; ++i) {
                        String itemType = split[i].trim();
                        if (StringUtils.isNullOrWhitespace(itemType)) continue;
                        this.specialKeyRing.add(itemType);
                    }
                    continue;
                }
                if ("notKillCrops".equals(k2)) {
                    this.notKillCrops = Boolean.parseBoolean(v);
                    continue;
                }
                if ("hasLighter".equals(k2)) {
                    this.hasLighter = Boolean.parseBoolean(v);
                    continue;
                }
                if ("neverSpawnKey".equals(k2)) {
                    this.neverSpawnKey = Boolean.parseBoolean(v);
                    continue;
                }
                if ("carMechanicsOverlay".equals(k2)) {
                    this.carMechanicsOverlay = ((String)((Object)v)).trim();
                    continue;
                }
                if ("carModelName".equals(k2)) {
                    this.carModelName = ((String)((Object)v)).trim();
                    continue;
                }
                if ("specialLootChance".equals(k2)) {
                    this.specialLootChance = Integer.parseInt(v);
                    continue;
                }
                if ("specialKeyRingChance".equals(k2)) {
                    this.specialKeyRingChance = Integer.parseInt(v);
                    continue;
                }
                if ("template".equals(k2)) {
                    this.LoadTemplate((String)((Object)v));
                    continue;
                }
                if ("template!".equals(k2)) {
                    VehicleTemplate template = ScriptManager.instance.getVehicleTemplate((String)((Object)v));
                    if (template == null) {
                        DebugLog.log("ERROR: template \"" + v + "\" not found in: " + this.getFileName());
                        continue;
                    }
                    this.Load(name, template.body);
                    continue;
                }
                if ("engineRepairLevel".equals(k2)) {
                    this.engineRepairLevel = Integer.parseInt(v);
                    continue;
                }
                if ("playerDamageProtection".equals(k2)) {
                    this.setPlayerDamageProtection(Float.parseFloat(v));
                    continue;
                }
                if (!"useChassisPhysicsCollision".equals(k2)) continue;
                this.useChassisPhysicsCollision = Boolean.parseBoolean(v);
                continue;
            }
            ScriptParser.Block child = element.asBlock();
            if ("area".equals(child.type)) {
                this.LoadArea(child);
                continue;
            }
            if ("attachment".equals(child.type)) {
                this.LoadAttachment(child);
                continue;
            }
            if ("model".equals(child.type)) {
                this.LoadModel(child, this.models);
                continue;
            }
            if ("part".equals(child.type)) {
                if (child.id != null && child.id.contains("*")) {
                    pattern = child.id;
                    for (Part part : this.parts) {
                        if (!this.globMatch(pattern, part.id)) continue;
                        child.id = part.id;
                        this.LoadPart(child);
                    }
                    continue;
                }
                this.LoadPart(child);
                continue;
            }
            if ("passenger".equals(child.type)) {
                if (child.id != null && child.id.contains("*")) {
                    pattern = child.id;
                    for (Passenger pngr : this.passengers) {
                        if (!this.globMatch(pattern, pngr.id)) continue;
                        child.id = pngr.id;
                        this.LoadPassenger(child);
                    }
                    continue;
                }
                this.LoadPassenger(child);
                continue;
            }
            if ("physics".equals(child.type)) {
                PhysicsShape physicsShape = this.LoadPhysicsShape(child);
                if (physicsShape == null || this.physicsShapes.size() >= 10) continue;
                this.physicsShapes.add(physicsShape);
                continue;
            }
            if ("skin".equals(child.type)) {
                Skin skin = this.LoadSkin(child);
                if (StringUtils.isNullOrWhitespace(skin.texture)) continue;
                this.skins.add(skin);
                continue;
            }
            if ("wheel".equals(child.type)) {
                this.LoadWheel(child);
                continue;
            }
            if ("lightbar".equals(child.type)) {
                for (ScriptParser.Value value : child.values) {
                    String[] split;
                    k = value.getKey().trim();
                    String v = value.getValue().trim();
                    if ("soundSiren".equals(k)) {
                        this.lightbar.soundSiren0 = v + "Yelp";
                        this.lightbar.soundSiren1 = v + "Wall";
                        this.lightbar.soundSiren2 = v + "Alarm";
                    }
                    if ("soundSiren0".equals(k)) {
                        this.lightbar.soundSiren0 = v;
                    }
                    if ("soundSiren1".equals(k)) {
                        this.lightbar.soundSiren1 = v;
                    }
                    if ("soundSiren2".equals(k)) {
                        this.lightbar.soundSiren2 = v;
                    }
                    if ("leftCol".equals(k)) {
                        split = v.split(";");
                        this.leftSirenCol = new ImmutableColor(Float.parseFloat(split[0]), Float.parseFloat(split[1]), Float.parseFloat(split[2]));
                    }
                    if ("rightCol".equals(k)) {
                        split = v.split(";");
                        this.rightSirenCol = new ImmutableColor(Float.parseFloat(split[0]), Float.parseFloat(split[1]), Float.parseFloat(split[2]));
                    }
                    this.lightbar.enable = true;
                    if (this.getPartById("lightbar") != null) continue;
                    Part part = new Part();
                    part.id = "lightbar";
                    this.parts.add(part);
                }
                continue;
            }
            if (!"sound".equals(child.type)) continue;
            for (ScriptParser.Value value : child.values) {
                k = value.getKey().trim();
                String v = value.getValue().trim();
                if ("alarm".equals(k)) {
                    ss = v.split("\\s+");
                    if (ss.length > 0) {
                        this.sound.alarm.addAll(Arrays.asList(ss));
                        this.sound.alarmEnable = true;
                    }
                } else if ("alarmLoop".equals(k)) {
                    ss = v.split("\\s+");
                    if (ss.length > 0) {
                        this.sound.alarmLoop.addAll(Arrays.asList(ss));
                        this.sound.alarmEnable = true;
                    }
                } else if ("backSignal".equals(k)) {
                    this.sound.backSignal = StringUtils.discardNullOrWhitespace(v);
                    this.sound.backSignalEnable = this.sound.backSignal != null;
                } else if ("engine".equals(k)) {
                    this.sound.engine = StringUtils.discardNullOrWhitespace(v);
                } else if ("engineStart".equals(k)) {
                    this.sound.engineStart = StringUtils.discardNullOrWhitespace(v);
                } else if ("engineTurnOff".equals(k)) {
                    this.sound.engineTurnOff = StringUtils.discardNullOrWhitespace(v);
                } else if ("handBrake".equals(k)) {
                    this.sound.handBrake = StringUtils.discardNullOrWhitespace(v);
                } else if ("horn".equals(k)) {
                    this.sound.horn = StringUtils.discardNullOrWhitespace(v);
                    this.sound.hornEnable = this.sound.horn != null;
                } else if ("ignitionFail".equals(k)) {
                    this.sound.ignitionFail = StringUtils.discardNullOrWhitespace(v);
                } else if ("ignitionFailNoPower".equals(k)) {
                    this.sound.ignitionFailNoPower = StringUtils.discardNullOrWhitespace(v);
                }
                this.sound.specified.add(k);
            }
        }
    }

    public String getFileName() {
        return this.fileName;
    }

    public void Loaded() {
        int j;
        int i;
        float scale = this.getModelScale();
        this.extents.mul(scale);
        this.maxSuspensionTravelCm *= scale;
        this.suspensionRestLength *= scale;
        this.centerOfMassOffset.mul(scale);
        this.physicsChassisShape.mul(scale);
        if (this.hadShadowOExtents) {
            this.shadowExtents.mul(scale);
        } else {
            this.shadowExtents.set(this.extents.x(), this.extents.z());
        }
        if (this.hadShadowOffset) {
            this.shadowOffset.mul(scale);
        } else {
            this.shadowOffset.set(this.centerOfMassOffset.x(), this.centerOfMassOffset.z());
        }
        for (Model model : this.models) {
            model.offset.mul(scale);
        }
        for (ModelAttachment attachment : this.attachments) {
            attachment.getOffset().mul(scale);
        }
        for (PhysicsShape shape : this.physicsShapes) {
            shape.offset.mul(scale);
            switch (shape.type) {
                case 1: {
                    shape.extents.mul(scale);
                    break;
                }
                case 2: {
                    shape.radius *= scale;
                    break;
                }
                case 3: {
                    shape.extents.mul(scale);
                }
            }
        }
        for (Wheel wheel : this.wheels) {
            wheel.radius *= scale;
            wheel.offset.mul(scale);
        }
        for (Area area : this.areas) {
            area.x *= scale;
            area.y *= scale;
            area.w *= scale;
            area.h *= scale;
        }
        if (this.hasPhysicsChassisShape() && !this.extents.equals(this.physicsChassisShape)) {
            DebugLog.Script.warn("vehicle \"" + this.name + "\" extents != physicsChassisShape");
        }
        for (i = 0; i < this.passengers.size(); ++i) {
            Passenger pngr = this.passengers.get(i);
            for (j = 0; j < pngr.getPositionCount(); ++j) {
                Position posn = pngr.getPosition(j);
                posn.getOffset().mul(scale);
            }
            for (j = 0; j < pngr.switchSeats.size(); ++j) {
                Passenger.SwitchSeat switchSeat = pngr.switchSeats.get(j);
                switchSeat.seat = this.getPassengerIndex(switchSeat.id);
                assert (switchSeat.seat != -1);
            }
        }
        for (i = 0; i < this.parts.size(); ++i) {
            Part part = this.parts.get(i);
            if (part.container != null && part.container.seatId != null && !part.container.seatId.isEmpty()) {
                part.container.seat = this.getPassengerIndex(part.container.seatId);
            }
            if (!part.specificItem || part.itemType == null) continue;
            for (j = 0; j < part.itemType.size(); ++j) {
                part.itemType.set(j, part.itemType.get(j) + this.mechanicType);
            }
        }
        if (!this.sound.alarmEnable.booleanValue() && this.sound.hornEnable) {
            this.sound.alarmEnable = true;
            this.sound.alarm.add(this.sound.horn);
        }
        this.initCrawlOffsets();
        if (this.specialKeyRing != null) {
            for (i = 0; i < this.specialKeyRing.size(); ++i) {
                String itemType = this.specialKeyRing.get(i);
                itemType = ScriptManager.instance.resolveItemType(this.getModule(), itemType);
                this.specialKeyRing.set(i, itemType);
            }
        }
        this.compact();
        if (!GameServer.server) {
            this.toBullet();
        }
    }

    private void compact() {
        this.areas.trimToSize();
        this.crawlOffsets.trimToSize();
        this.attachments.trimToSize();
        this.physicsShapes.trimToSize();
        this.models.trimToSize();
        this.parts.trimToSize();
        for (int i = 0; i < this.parts.size(); ++i) {
            this.parts.get(i).compact();
        }
        this.passengers.trimToSize();
        this.skins.trimToSize();
        if (this.specialKeyRing != null) {
            this.specialKeyRing.trimToSize();
        }
        this.wheels.trimToSize();
    }

    public void toBullet() {
        float[] params = new float[200];
        int n = 0;
        params[n++] = this.getModelScale();
        params[n++] = this.mass;
        params[n++] = this.rollInfluence;
        params[n++] = this.suspensionStiffness;
        params[n++] = this.suspensionCompression;
        params[n++] = this.suspensionDamping;
        params[n++] = this.maxSuspensionTravelCm;
        params[n++] = this.suspensionRestLength;
        params[n++] = SystemDisabler.getdoHighFriction() ? this.wheelFriction * 100.0f : this.wheelFriction;
        params[n++] = this.stoppingMovementForce;
        params[n++] = this.getWheelCount();
        for (int i = 0; i < this.getWheelCount(); ++i) {
            Wheel wheel = this.getWheel(i);
            params[n++] = wheel.front ? 1.0f : 0.0f;
            params[n++] = wheel.offset.x + this.getModel().offset.x - 0.0f * this.centerOfMassOffset.x;
            params[n++] = wheel.offset.y + this.getModel().offset.y - 0.0f * this.centerOfMassOffset.y + 1.0f * this.suspensionRestLength;
            params[n++] = wheel.offset.z + this.getModel().offset.z - 0.0f * this.centerOfMassOffset.z;
            params[n++] = wheel.radius;
        }
        int numShapes = (this.hasPhysicsChassisShape() && this.useChassisPhysicsCollision ? 1 : 0) + this.physicsShapes.size();
        if (numShapes == 0) {
            numShapes = 1;
        }
        params[n++] = numShapes;
        if (this.hasPhysicsChassisShape() && this.useChassisPhysicsCollision) {
            params[n++] = 1.0f;
            params[n++] = this.centerOfMassOffset.x;
            params[n++] = this.centerOfMassOffset.y;
            params[n++] = this.centerOfMassOffset.z;
            params[n++] = this.physicsChassisShape.x;
            params[n++] = this.physicsChassisShape.y;
            params[n++] = this.physicsChassisShape.z;
            params[n++] = 0.0f;
            params[n++] = 0.0f;
            params[n++] = 0.0f;
        } else if (this.physicsShapes.isEmpty()) {
            params[n++] = 1.0f;
            params[n++] = this.centerOfMassOffset.x;
            params[n++] = this.centerOfMassOffset.y;
            params[n++] = this.centerOfMassOffset.z;
            params[n++] = this.extents.x;
            params[n++] = this.extents.y;
            params[n++] = this.extents.z;
            params[n++] = 0.0f;
            params[n++] = 0.0f;
            params[n++] = 0.0f;
        }
        for (int i = 0; i < this.physicsShapes.size(); ++i) {
            PhysicsShape shape = this.physicsShapes.get(i);
            params[n++] = shape.type;
            params[n++] = shape.offset.x;
            params[n++] = shape.offset.y;
            params[n++] = shape.offset.z;
            if (shape.type == 1) {
                params[n++] = shape.extents.x;
                params[n++] = shape.extents.y;
                params[n++] = shape.extents.z;
                params[n++] = shape.rotate.x;
                params[n++] = shape.rotate.y;
                params[n++] = shape.rotate.z;
                continue;
            }
            if (shape.type == 2) {
                params[n++] = shape.radius;
                continue;
            }
            if (shape.type != 3) continue;
        }
        Bullet.defineVehicleScript(this.getFullName(), params);
    }

    private void LoadVector2f(String s, Vector2f v) {
        String[] ss = s.split(" ");
        v.set(Float.parseFloat(ss[0]), Float.parseFloat(ss[1]));
    }

    private void LoadVector3f(String s, Vector3f v) {
        String[] ss = s.split(" ");
        v.set(Float.parseFloat(ss[0]), Float.parseFloat(ss[1]), Float.parseFloat(ss[2]));
    }

    private void LoadVector4f(String s, Vector4f v) {
        String[] ss = s.split(" ");
        v.set(Float.parseFloat(ss[0]), Float.parseFloat(ss[1]), Float.parseFloat(ss[2]), Float.parseFloat(ss[3]));
    }

    private void LoadVector2i(String s, Vector2i v) {
        String[] ss = s.split(" ");
        v.set(Integer.parseInt(ss[0]), Integer.parseInt(ss[1]));
    }

    private ModelAttachment LoadAttachment(ScriptParser.Block block) {
        ModelAttachment attachment = this.getAttachmentById(block.id);
        if (attachment == null) {
            attachment = new ModelAttachment(block.id);
            attachment.setOwner(this);
            this.attachments.add(attachment);
        }
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("bone".equals(k)) {
                attachment.setBone(v);
                continue;
            }
            if ("offset".equals(k)) {
                this.LoadVector3f(v, attachment.getOffset());
                continue;
            }
            if ("rotate".equals(k)) {
                this.LoadVector3f(v, attachment.getRotate());
                continue;
            }
            if ("canAttach".equals(k)) {
                attachment.setCanAttach(new ArrayList<String>(Arrays.asList(v.split(";"))));
                continue;
            }
            if ("zoffset".equals(k)) {
                attachment.setZOffset(Float.parseFloat(v));
                continue;
            }
            if (!"updateconstraint".equals(k)) continue;
            attachment.setUpdateConstraint(Boolean.parseBoolean(v));
        }
        return attachment;
    }

    private Model LoadModel(ScriptParser.Block block, ArrayList<Model> models) {
        Model model = this.getModelById(block.id, models);
        if (model == null) {
            model = new Model();
            model.id = block.id;
            models.add(model);
        }
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("file".equals(k)) {
                model.file = v;
                continue;
            }
            if ("offset".equals(k)) {
                this.LoadVector3f(v, model.offset);
                continue;
            }
            if ("rotate".equals(k)) {
                this.LoadVector3f(v, model.rotate);
                continue;
            }
            if ("scale".equals(k)) {
                model.scale = Float.parseFloat(v);
                continue;
            }
            if ("attachmentParent".equals(k)) {
                model.attachmentNameParent = v.isEmpty() ? null : v;
                continue;
            }
            if ("attachmentSelf".equals(k)) {
                model.attachmentNameSelf = v.isEmpty() ? null : v;
                continue;
            }
            if (!"ignoreVehicleScale".equalsIgnoreCase(k)) continue;
            model.ignoreVehicleScale = Boolean.parseBoolean(v);
        }
        return model;
    }

    private Skin LoadSkin(ScriptParser.Block block) {
        Skin skin = new Skin();
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("texture".equals(k)) {
                skin.texture = StringUtils.discardNullOrWhitespace(v);
                continue;
            }
            if ("textureRust".equals(k)) {
                skin.textureRust = StringUtils.discardNullOrWhitespace(v);
                continue;
            }
            if ("textureMask".equals(k)) {
                skin.textureMask = StringUtils.discardNullOrWhitespace(v);
                continue;
            }
            if ("textureLights".equals(k)) {
                skin.textureLights = StringUtils.discardNullOrWhitespace(v);
                continue;
            }
            if ("textureDamage1Overlay".equals(k)) {
                skin.textureDamage1Overlay = StringUtils.discardNullOrWhitespace(v);
                continue;
            }
            if ("textureDamage1Shell".equals(k)) {
                skin.textureDamage1Shell = StringUtils.discardNullOrWhitespace(v);
                continue;
            }
            if ("textureDamage2Overlay".equals(k)) {
                skin.textureDamage2Overlay = StringUtils.discardNullOrWhitespace(v);
                continue;
            }
            if ("textureDamage2Shell".equals(k)) {
                skin.textureDamage2Shell = StringUtils.discardNullOrWhitespace(v);
                continue;
            }
            if (!"textureShadow".equals(k)) continue;
            skin.textureShadow = StringUtils.discardNullOrWhitespace(v);
        }
        return skin;
    }

    private Wheel LoadWheel(ScriptParser.Block block) {
        Wheel wheel = this.getWheelById(block.id);
        if (wheel == null) {
            wheel = new Wheel();
            wheel.id = block.id;
            this.wheels.add(wheel);
        }
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("model".equals(k)) {
                wheel.model = v;
                continue;
            }
            if ("front".equals(k)) {
                wheel.front = Boolean.parseBoolean(v);
                continue;
            }
            if ("offset".equals(k)) {
                this.LoadVector3f(v, wheel.offset);
                continue;
            }
            if ("radius".equals(k)) {
                wheel.radius = Float.parseFloat(v);
                continue;
            }
            if (!"width".equals(k)) continue;
            wheel.width = Float.parseFloat(v);
        }
        return wheel;
    }

    private Passenger LoadPassenger(ScriptParser.Block block) {
        Passenger pngr = this.getPassengerById(block.id);
        if (pngr == null) {
            pngr = new Passenger();
            pngr.id = block.id;
            this.passengers.add(pngr);
        }
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("area".equals(k)) {
                pngr.area = v;
                continue;
            }
            if ("door".equals(k)) {
                pngr.door = v;
                continue;
            }
            if ("door2".equals(k)) {
                pngr.door2 = v;
                continue;
            }
            if ("hasRoof".equals(k)) {
                pngr.hasRoof = Boolean.parseBoolean(v);
                continue;
            }
            if (!"showPassenger".equals(k)) continue;
            pngr.showPassenger = Boolean.parseBoolean(v);
        }
        for (ScriptParser.Block child : block.children) {
            if ("anim".equals(child.type)) {
                this.LoadAnim(child, pngr.anims);
                continue;
            }
            if ("position".equals(child.type)) {
                this.LoadPosition(child, pngr.positions);
                continue;
            }
            if (!"switchSeat".equals(child.type)) continue;
            this.LoadPassengerSwitchSeat(child, pngr);
        }
        return pngr;
    }

    private Anim LoadAnim(ScriptParser.Block block, ArrayList<Anim> anims) {
        Anim anim = this.getAnimationById(block.id, anims);
        if (anim == null) {
            anim = new Anim();
            anim.id = block.id.intern();
            anims.add(anim);
        }
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("angle".equals(k)) {
                this.LoadVector3f(v, anim.angle);
                continue;
            }
            if ("anim".equals(k)) {
                anim.anim = v;
                continue;
            }
            if ("animate".equals(k)) {
                anim.animate = Boolean.parseBoolean(v);
                continue;
            }
            if ("loop".equals(k)) {
                anim.loop = Boolean.parseBoolean(v);
                continue;
            }
            if ("reverse".equals(k)) {
                anim.reverse = Boolean.parseBoolean(v);
                continue;
            }
            if ("rate".equals(k)) {
                anim.rate = Float.parseFloat(v);
                continue;
            }
            if ("offset".equals(k)) {
                this.LoadVector3f(v, anim.offset);
                continue;
            }
            if (!"sound".equals(k)) continue;
            anim.sound = v;
        }
        return anim;
    }

    private Passenger.SwitchSeat LoadPassengerSwitchSeat(ScriptParser.Block block, Passenger passenger) {
        Passenger.SwitchSeat switchSeat = passenger.getSwitchSeatById(block.id);
        if (block.isEmpty()) {
            if (switchSeat != null) {
                passenger.switchSeats.remove(switchSeat);
            }
            return null;
        }
        if (switchSeat == null) {
            switchSeat = new Passenger.SwitchSeat();
            switchSeat.id = block.id;
            passenger.switchSeats.add(switchSeat);
        }
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("anim".equals(k)) {
                switchSeat.anim = v;
                continue;
            }
            if ("rate".equals(k)) {
                switchSeat.rate = Float.parseFloat(v);
                continue;
            }
            if (!"sound".equals(k)) continue;
            switchSeat.sound = v.isEmpty() ? null : v;
        }
        return switchSeat;
    }

    private Area LoadArea(ScriptParser.Block block) {
        Area area = this.getAreaById(block.id);
        if (area == null) {
            area = new Area();
            area.id = block.id.intern();
            this.areas.add(area);
        }
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if (!"xywh".equals(k)) continue;
            String[] ss2 = v.split(" ");
            area.x = Float.parseFloat(ss2[0]);
            area.y = Float.parseFloat(ss2[1]);
            area.w = Float.parseFloat(ss2[2]);
            area.h = Float.parseFloat(ss2[3]);
        }
        return area;
    }

    private Part LoadPart(ScriptParser.Block block) {
        Part part = this.getPartById(block.id);
        if (part == null) {
            part = new Part();
            part.id = block.id;
            this.parts.add(part);
        }
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim().intern();
            String v = value.getValue().trim().intern();
            if ("area".equals(k)) {
                String string = part.area = v.isEmpty() ? null : v;
            }
            if ("mechanicArea".equals(k)) {
                part.mechanicArea = v;
                continue;
            }
            if ("itemType".equals(k)) {
                String[] split;
                part.itemType = new ArrayList();
                for (String itype : split = v.split(";")) {
                    part.itemType.add(itype.intern());
                }
                continue;
            }
            if ("parent".equals(k)) {
                part.parent = v.isEmpty() ? null : v;
                continue;
            }
            if ("mechanicRequireKey".equals(k)) {
                part.mechanicRequireKey = Boolean.parseBoolean(v);
                continue;
            }
            if ("repairMechanic".equals(k)) {
                part.setRepairMechanic(Boolean.parseBoolean(v));
                continue;
            }
            if ("setAllModelsVisible".equals(k)) {
                part.setAllModelsVisible = Boolean.parseBoolean(v);
                continue;
            }
            if ("wheel".equals(k)) {
                part.wheel = v;
                continue;
            }
            if ("category".equals(k)) {
                part.category = v;
                continue;
            }
            if ("durability".equals(k)) {
                part.durability = Float.parseFloat(v);
                continue;
            }
            if ("specificItem".equals(k)) {
                part.specificItem = Boolean.parseBoolean(v);
                continue;
            }
            if (!"hasLightsRear".equals(k)) continue;
            part.hasLightsRear = Boolean.parseBoolean(v);
        }
        for (ScriptParser.Block child : block.children) {
            if ("anim".equals(child.type)) {
                if (part.anims == null) {
                    part.anims = new ArrayList();
                }
                this.LoadAnim(child, part.anims);
                continue;
            }
            if ("container".equals(child.type)) {
                part.container = this.LoadContainer(child, part.container);
                continue;
            }
            if ("door".equals(child.type)) {
                part.door = this.LoadDoor(child);
                continue;
            }
            if ("lua".equals(child.type)) {
                part.luaFunctions = this.LoadLuaFunctions(child);
                continue;
            }
            if ("model".equals(child.type)) {
                if (part.models == null) {
                    part.models = new ArrayList();
                }
                this.LoadModel(child, part.models);
                continue;
            }
            if ("table".equals(child.type)) {
                KahluaTable kahluaTable;
                KahluaTable o = part.tables == null ? null : part.tables.get(child.id);
                KahluaTable table = this.LoadTable(child, o instanceof KahluaTable ? (kahluaTable = o) : null);
                if (part.tables == null) {
                    part.tables = new THashMap();
                }
                part.tables.put(child.id, table);
                continue;
            }
            if (!"window".equals(child.type)) continue;
            part.window = this.LoadWindow(child);
        }
        return part;
    }

    private PhysicsShape LoadPhysicsShape(ScriptParser.Block block) {
        int type;
        switch (block.id) {
            case "box": {
                type = 1;
                break;
            }
            case "sphere": {
                type = 2;
                break;
            }
            case "mesh": {
                type = 3;
                break;
            }
            default: {
                return null;
            }
        }
        PhysicsShape shape = new PhysicsShape();
        shape.type = type;
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("extents".equalsIgnoreCase(k)) {
                this.LoadVector3f(v, shape.extents);
                continue;
            }
            if ("offset".equalsIgnoreCase(k)) {
                this.LoadVector3f(v, shape.offset);
                continue;
            }
            if ("radius".equalsIgnoreCase(k)) {
                shape.radius = Float.parseFloat(v);
                continue;
            }
            if ("rotate".equalsIgnoreCase(k)) {
                this.LoadVector3f(v, shape.rotate);
                continue;
            }
            if ("physicsShapeScript".equalsIgnoreCase(k)) {
                shape.physicsShapeScript = StringUtils.discardNullOrWhitespace(v);
                continue;
            }
            if (!"scale".equalsIgnoreCase(k)) continue;
            float scale = Float.parseFloat(v);
            shape.extents.set(scale);
        }
        switch (shape.type) {
            case 1: {
                if (!(shape.extents.x() <= 0.0f) && !(shape.extents.y() <= 0.0f) && !(shape.extents.z() <= 0.0f)) break;
                return null;
            }
            case 2: {
                if (!(shape.radius <= 0.0f)) break;
                return null;
            }
            case 3: {
                if (shape.physicsShapeScript == null) {
                    return null;
                }
                if (!(shape.extents.x() <= 0.0f)) break;
                shape.extents.set(1.0f);
            }
        }
        return shape;
    }

    private Door LoadDoor(ScriptParser.Block block) {
        Door door = new Door();
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String string = value.getValue().trim();
        }
        return door;
    }

    private Window LoadWindow(ScriptParser.Block block) {
        Window window = new Window();
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if (!"openable".equals(k)) continue;
            window.openable = Boolean.parseBoolean(v);
        }
        return window;
    }

    private Container LoadContainer(ScriptParser.Block block, Container existing) {
        Container container = existing == null ? new Container() : existing;
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("capacity".equals(k)) {
                container.capacity = Integer.parseInt(v);
                continue;
            }
            if ("conditionAffectsCapacity".equals(k)) {
                container.conditionAffectsCapacity = Boolean.parseBoolean(v);
                continue;
            }
            if ("contentType".equals(k)) {
                container.contentType = v;
                continue;
            }
            if ("seat".equals(k)) {
                container.seatId = v;
                continue;
            }
            if (!"test".equals(k)) continue;
            container.luaTest = v;
        }
        return container;
    }

    private THashMap<String, String> LoadLuaFunctions(ScriptParser.Block block) {
        THashMap<String, String> map = new THashMap<String, String>();
        for (ScriptParser.Value value : block.values) {
            if (value.string.indexOf(61) == -1) {
                throw new RuntimeException("expected \"key = value\", got \"" + value.string.trim() + "\" in " + this.getFullName());
            }
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            map.put(k, v);
        }
        return map;
    }

    private Object checkIntegerKey(Object key) {
        if (!(key instanceof String)) {
            return key;
        }
        String str = (String)key;
        for (int i = 0; i < str.length(); ++i) {
            if (Character.isDigit(str.charAt(i))) continue;
            return str.intern();
        }
        return Double.valueOf(str);
    }

    private KahluaTable LoadTable(ScriptParser.Block block, KahluaTable existing) {
        KahluaTable table = existing == null ? LuaManager.platform.newTable() : existing;
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim().intern();
            if (v.isEmpty()) {
                v = null;
            }
            table.rawset(this.checkIntegerKey(k), (Object)v);
        }
        for (ScriptParser.Block child : block.children) {
            KahluaTable kahluaTable;
            Object o = table.rawget(child.type);
            KahluaTable table2 = this.LoadTable(child, o instanceof KahluaTable ? (kahluaTable = (KahluaTable)o) : null);
            table.rawset(this.checkIntegerKey(child.type), (Object)table2);
        }
        return table;
    }

    private void LoadTemplate(String str) {
        if (str.contains("/")) {
            String[] ss = str.split("/");
            if (ss.length == 0 || ss.length > 3) {
                DebugLog.log("ERROR: template \"" + str + "\"");
                return;
            }
            for (int i = 0; i < ss.length; ++i) {
                ss[i] = ss[i].trim();
                if (!ss[i].isEmpty()) continue;
                DebugLog.log("ERROR: template \"" + str + "\"");
                return;
            }
            String templateName = ss[0];
            VehicleTemplate template = ScriptManager.instance.getVehicleTemplate(templateName);
            if (template == null) {
                DebugLog.log("ERROR: template \"" + str + "\" not found A");
                return;
            }
            VehicleScript script = template.getScript();
            switch (ss[1]) {
                case "area": {
                    if (ss.length == 2) {
                        DebugLog.log("ERROR: template \"" + str + "\"");
                        return;
                    }
                    this.copyAreasFrom(script, ss[2]);
                    break;
                }
                case "part": {
                    if (ss.length == 2) {
                        DebugLog.log("ERROR: template \"" + str + "\"");
                        return;
                    }
                    this.copyPartsFrom(script, ss[2]);
                    break;
                }
                case "passenger": {
                    if (ss.length == 2) {
                        DebugLog.log("ERROR: template \"" + str + "\"");
                        return;
                    }
                    this.copyPassengersFrom(script, ss[2]);
                    break;
                }
                case "wheel": {
                    if (ss.length == 2) {
                        DebugLog.log("ERROR: template \"" + str + "\"");
                        return;
                    }
                    this.copyWheelsFrom(script, ss[2]);
                    break;
                }
                case "physics": {
                    if (ss.length == 2) {
                        DebugLog.log("ERROR: template \"" + str + "\"");
                        return;
                    }
                    this.copyPhysicsFrom(script, ss[2]);
                    break;
                }
                default: {
                    DebugLog.log("ERROR: template \"" + str + "\"");
                    return;
                }
            }
        } else {
            String templateName = str.trim();
            VehicleTemplate template = ScriptManager.instance.getVehicleTemplate(templateName);
            if (template == null) {
                DebugLog.log("ERROR: template \"" + str + "\" not found B");
                return;
            }
            VehicleScript script = template.getScript();
            this.copyAreasFrom(script, "*");
            this.copyPartsFrom(script, "*");
            this.copyPassengersFrom(script, "*");
            this.copySoundFrom(script, "*");
            this.copyWheelsFrom(script, "*");
            this.copyPhysicsFrom(script, "*");
        }
    }

    public void copyAreasFrom(VehicleScript other, String spec) {
        if ("*".equals(spec)) {
            for (int i = 0; i < other.getAreaCount(); ++i) {
                Area otherArea = other.getArea(i);
                int index = this.getIndexOfAreaById(otherArea.id);
                if (index == -1) {
                    this.areas.add(otherArea.makeCopy());
                    continue;
                }
                this.areas.set(index, otherArea.makeCopy());
            }
        } else {
            Area otherArea = other.getAreaById(spec);
            if (otherArea == null) {
                DebugLog.log("ERROR: area \"" + spec + "\" not found");
                return;
            }
            int index = this.getIndexOfAreaById(otherArea.id);
            if (index == -1) {
                this.areas.add(otherArea.makeCopy());
            } else {
                this.areas.set(index, otherArea.makeCopy());
            }
        }
    }

    public void copyPartsFrom(VehicleScript other, String spec) {
        if ("*".equals(spec)) {
            for (int i = 0; i < other.getPartCount(); ++i) {
                Part otherPart = other.getPart(i);
                int index = this.getIndexOfPartById(otherPart.id);
                if (index == -1) {
                    this.parts.add(otherPart.makeCopy());
                    continue;
                }
                this.parts.set(index, otherPart.makeCopy());
            }
        } else {
            Part otherPart = other.getPartById(spec);
            if (otherPart == null) {
                DebugLog.log("ERROR: part \"" + spec + "\" not found");
                return;
            }
            int index = this.getIndexOfPartById(otherPart.id);
            if (index == -1) {
                this.parts.add(otherPart.makeCopy());
            } else {
                this.parts.set(index, otherPart.makeCopy());
            }
        }
    }

    public void copyPhysicsFrom(VehicleScript other, String spec) {
        this.physicsShapes.clear();
        for (int i = 0; i < other.getPhysicsShapeCount(); ++i) {
            this.physicsShapes.add(i, other.getPhysicsShape(i).makeCopy());
        }
        this.useChassisPhysicsCollision = other.useChassisPhysicsCollision();
    }

    public void copyPassengersFrom(VehicleScript other, String spec) {
        if ("*".equals(spec)) {
            for (int i = 0; i < other.getPassengerCount(); ++i) {
                Passenger otherPngr = other.getPassenger(i);
                int index = this.getPassengerIndex(otherPngr.id);
                if (index == -1) {
                    this.passengers.add(otherPngr.makeCopy());
                    continue;
                }
                this.passengers.set(index, otherPngr.makeCopy());
            }
        } else {
            Passenger otherPngr = other.getPassengerById(spec);
            if (otherPngr == null) {
                DebugLog.log("ERROR: passenger \"" + spec + "\" not found");
                return;
            }
            int index = this.getPassengerIndex(otherPngr.id);
            if (index == -1) {
                this.passengers.add(otherPngr.makeCopy());
            } else {
                this.passengers.set(index, otherPngr.makeCopy());
            }
        }
    }

    public void copySoundFrom(VehicleScript other, String spec) {
        if ("*".equals(spec)) {
            Iterator<String> iterator2 = other.sound.specified.iterator();
            while (iterator2.hasNext()) {
                String s;
                switch (s = iterator2.next()) {
                    case "backSignal": {
                        this.sound.backSignal = other.sound.backSignal;
                        this.sound.backSignalEnable = other.sound.backSignalEnable;
                        break;
                    }
                    case "engine": {
                        this.sound.engine = other.sound.engine;
                        break;
                    }
                    case "engineStart": {
                        this.sound.engineStart = other.sound.engineStart;
                        break;
                    }
                    case "engineTurnOff": {
                        this.sound.engineTurnOff = other.sound.engineTurnOff;
                        break;
                    }
                    case "handBrake": {
                        this.sound.handBrake = other.sound.handBrake;
                        break;
                    }
                    case "ignitionFail": {
                        this.sound.ignitionFail = other.sound.ignitionFail;
                        break;
                    }
                    case "ignitionFailNoPower": {
                        this.sound.ignitionFailNoPower = other.sound.ignitionFailNoPower;
                    }
                }
                this.sound.specified.add(s);
            }
        }
    }

    public void copyWheelsFrom(VehicleScript other, String spec) {
        if ("*".equals(spec)) {
            for (int i = 0; i < other.getWheelCount(); ++i) {
                Wheel otherWheel = other.getWheel(i);
                int index = this.getIndexOfWheelById(otherWheel.id);
                if (index == -1) {
                    this.wheels.add(otherWheel.makeCopy());
                    continue;
                }
                this.wheels.set(index, otherWheel.makeCopy());
            }
        } else {
            Wheel otherWheel = other.getWheelById(spec);
            if (otherWheel == null) {
                DebugLog.log("ERROR: wheel \"" + spec + "\" not found");
                return;
            }
            int index = this.getIndexOfWheelById(otherWheel.id);
            if (index == -1) {
                this.wheels.add(otherWheel.makeCopy());
            } else {
                this.wheels.set(index, otherWheel.makeCopy());
            }
        }
    }

    private Position LoadPosition(ScriptParser.Block block, ArrayList<Position> positions) {
        Position position = this.getPositionById(block.id, positions);
        if (block.isEmpty()) {
            if (position != null) {
                positions.remove(position);
            }
            return null;
        }
        if (position == null) {
            position = new Position();
            position.id = block.id.intern();
            positions.add(position);
        }
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("rotate".equals(k)) {
                this.LoadVector3f(v, position.rotate);
                continue;
            }
            if ("offset".equals(k)) {
                this.LoadVector3f(v, position.offset);
                continue;
            }
            if (!"area".equals(k)) continue;
            position.area = v.isEmpty() ? null : v;
        }
        return position;
    }

    private void initCrawlOffsets() {
        int i;
        for (int i2 = 0; i2 < this.getWheelCount(); ++i2) {
            Wheel wheel = this.getWheel(i2);
            if (!wheel.id.contains("Left")) continue;
            this.initCrawlOffsets(wheel);
        }
        float polyLength = this.extents.z + 0.3f;
        for (i = 0; i < this.crawlOffsets.size(); ++i) {
            this.crawlOffsets.set(i, (this.extents.z / 2.0f + 0.15f + this.crawlOffsets.get(i) - this.centerOfMassOffset.z) / polyLength);
        }
        this.crawlOffsets.sort();
        for (i = 0; i < this.crawlOffsets.size(); ++i) {
            float o1 = this.crawlOffsets.get(i);
            for (int j = i + 1; j < this.crawlOffsets.size(); ++j) {
                float o2 = this.crawlOffsets.get(j);
                if (!((o2 - o1) * polyLength < 0.15f)) continue;
                this.crawlOffsets.removeAt(j--);
            }
        }
    }

    private void initCrawlOffsets(Wheel wheel) {
        float radius = 0.3f;
        float modelOffsetZ = this.getModel() == null ? 0.0f : this.getModel().getOffset().z;
        float front = this.centerOfMassOffset.z + this.extents.z / 2.0f;
        float rear = this.centerOfMassOffset.z - this.extents.z / 2.0f;
        for (int i = 0; i < 10; ++i) {
            float zOffset = modelOffsetZ + wheel.offset.z + wheel.radius + 0.3f + 0.3f * (float)i;
            if (zOffset + 0.3f <= front && !this.isOverlappingWheel(zOffset)) {
                this.crawlOffsets.add(zOffset);
            }
            if (!((zOffset = modelOffsetZ + wheel.offset.z - wheel.radius - 0.3f - 0.3f * (float)i) - 0.3f >= rear) || this.isOverlappingWheel(zOffset)) continue;
            this.crawlOffsets.add(zOffset);
        }
    }

    private boolean isOverlappingWheel(float zOffset) {
        float radius = 0.3f;
        float modelOffsetZ = this.getModel() == null ? 0.0f : this.getModel().getOffset().z;
        for (int i = 0; i < this.getWheelCount(); ++i) {
            Wheel wheel = this.getWheel(i);
            if (!wheel.id.contains("Left") || !(Math.abs(modelOffsetZ + wheel.offset.z - zOffset) < (wheel.radius + 0.3f) * 0.99f)) continue;
            return true;
        }
        return false;
    }

    public String getName() {
        return this.name;
    }

    public String getFullName() {
        return this.getModule().getName() + "." + this.getName();
    }

    public String getFullType() {
        return this.getFullName();
    }

    public Model getModel() {
        return this.models.isEmpty() ? null : this.models.get(0);
    }

    public Vector3f getModelOffset() {
        return this.getModel() == null ? null : this.getModel().getOffset();
    }

    public float getModelScale() {
        return this.getModel() == null ? 1.0f : this.getModel().scale;
    }

    public void setModelScale(float scale) {
        Model model = this.getModel();
        if (model != null) {
            float oldScale = model.scale;
            model.scale = 1.0f / oldScale;
            this.Loaded();
            model.scale = PZMath.clamp(scale, 0.01f, 100.0f);
            this.Loaded();
        }
    }

    public int getModelCount() {
        return this.models.size();
    }

    public Model getModelByIndex(int index) {
        return this.models.get(index);
    }

    public Model getModelById(String id, ArrayList<Model> models) {
        for (int i = 0; i < models.size(); ++i) {
            Model model = models.get(i);
            if (StringUtils.isNullOrWhitespace(model.id) && StringUtils.isNullOrWhitespace(id)) {
                return model;
            }
            if (model.id == null || !model.id.equals(id)) continue;
            return model;
        }
        return null;
    }

    public Model getModelById(String id) {
        return this.getModelById(id, this.models);
    }

    public int getAttachmentCount() {
        return this.attachments.size();
    }

    public ModelAttachment getAttachment(int index) {
        return this.attachments.get(index);
    }

    public ModelAttachment getAttachmentById(String id) {
        for (int i = 0; i < this.attachments.size(); ++i) {
            ModelAttachment attachment = this.attachments.get(i);
            if (!attachment.getId().equals(id)) continue;
            return attachment;
        }
        return null;
    }

    public ModelAttachment addAttachment(ModelAttachment attach) {
        attach.setOwner(this);
        this.attachments.add(attach);
        return attach;
    }

    public ModelAttachment removeAttachment(ModelAttachment attach) {
        attach.setOwner(null);
        this.attachments.remove(attach);
        return attach;
    }

    public ModelAttachment addAttachmentAt(int index, ModelAttachment attach) {
        attach.setOwner(this);
        this.attachments.add(index, attach);
        return attach;
    }

    public ModelAttachment removeAttachment(int index) {
        ModelAttachment attachment = this.attachments.remove(index);
        attachment.setOwner(null);
        return attachment;
    }

    @Override
    public void beforeRenameAttachment(ModelAttachment attachment) {
    }

    @Override
    public void afterRenameAttachment(ModelAttachment attachment) {
    }

    public LightBar getLightbar() {
        return this.lightbar;
    }

    public Sounds getSounds() {
        return this.sound;
    }

    public boolean getHasSiren() {
        return this.hasSiren;
    }

    public Vector3f getExtents() {
        return this.extents;
    }

    public Vector3f getPhysicsChassisShape() {
        return this.physicsChassisShape;
    }

    public boolean hasPhysicsChassisShape() {
        return this.physicsChassisShape.lengthSquared() > 0.0f;
    }

    public boolean useChassisPhysicsCollision() {
        return this.useChassisPhysicsCollision;
    }

    public Vector2f getShadowExtents() {
        return this.shadowExtents;
    }

    public Vector2f getShadowOffset() {
        return this.shadowOffset;
    }

    public Vector2f getExtentsOffset() {
        return this.extentsOffset;
    }

    public float getMass() {
        return this.mass;
    }

    public Vector3f getCenterOfMassOffset() {
        return this.centerOfMassOffset;
    }

    public float getEngineForce() {
        return this.engineForce;
    }

    public float getEngineIdleSpeed() {
        return this.engineIdleSpeed;
    }

    public int getEngineQuality() {
        return this.engineQuality;
    }

    public int getEngineLoudness() {
        return this.engineLoudness;
    }

    public float getRollInfluence() {
        return this.rollInfluence;
    }

    public float getSteeringIncrement() {
        return this.steeringIncrement;
    }

    public float getSteeringClamp(float speed) {
        float delta = (speed = Math.abs(speed)) / this.maxSpeed;
        if (delta > 1.0f) {
            delta = 1.0f;
        }
        delta = 1.0f - delta;
        return (0.9f - this.steeringClamp) * delta + this.steeringClamp;
    }

    public float getSuspensionStiffness() {
        return this.suspensionStiffness;
    }

    public float getSuspensionDamping() {
        return this.suspensionDamping;
    }

    public float getSuspensionCompression() {
        return this.suspensionCompression;
    }

    public float getSuspensionRestLength() {
        return this.suspensionRestLength;
    }

    public float getSuspensionTravel() {
        return this.maxSuspensionTravelCm;
    }

    public float getWheelFriction() {
        return this.wheelFriction;
    }

    public int getWheelCount() {
        return this.wheels.size();
    }

    public Wheel getWheel(int index) {
        return this.wheels.get(index);
    }

    public Wheel getWheelById(String id) {
        for (int i = 0; i < this.wheels.size(); ++i) {
            Wheel wheel = this.wheels.get(i);
            if (wheel.id == null || !wheel.id.equals(id)) continue;
            return wheel;
        }
        return null;
    }

    public int getIndexOfWheelById(String id) {
        for (int i = 0; i < this.wheels.size(); ++i) {
            Wheel wheel = this.wheels.get(i);
            if (wheel.id == null || !wheel.id.equals(id)) continue;
            return i;
        }
        return -1;
    }

    public int getPassengerCount() {
        return this.passengers.size();
    }

    public Passenger getPassenger(int index) {
        return this.passengers.get(index);
    }

    public Passenger getPassengerById(String id) {
        for (int i = 0; i < this.passengers.size(); ++i) {
            Passenger passenger = this.passengers.get(i);
            if (passenger.id == null || !passenger.id.equals(id)) continue;
            return passenger;
        }
        return null;
    }

    public int getPassengerIndex(String id) {
        for (int i = 0; i < this.passengers.size(); ++i) {
            Passenger passenger = this.passengers.get(i);
            if (passenger.id == null || !passenger.id.equals(id)) continue;
            return i;
        }
        return -1;
    }

    public int getPhysicsShapeCount() {
        return this.physicsShapes.size();
    }

    public PhysicsShape getPhysicsShape(int index) {
        if (index < 0 || index >= this.physicsShapes.size()) {
            return null;
        }
        return this.physicsShapes.get(index);
    }

    public PhysicsShape addPhysicsShape(String type) {
        Objects.requireNonNull(type);
        PhysicsShape shape = new PhysicsShape();
        shape.type = switch (type) {
            case "box" -> 1;
            case "sphere" -> 2;
            case "mesh" -> 3;
            default -> throw new IllegalArgumentException("invalid vehicle physics shape \"%s\"".formatted(type));
        };
        switch (shape.type) {
            case 1: {
                shape.extents.set(1.0f, 1.0f, 1.0f);
                break;
            }
            case 2: {
                shape.radius = 0.5f;
                break;
            }
            case 3: {
                shape.physicsShapeScript = "Base.XXX";
                shape.extents.set(1.0f);
            }
        }
        this.physicsShapes.add(shape);
        return shape;
    }

    public PhysicsShape removePhysicsShape(int index) {
        return this.physicsShapes.remove(index);
    }

    public int getFrontEndHealth() {
        return this.frontEndHealth;
    }

    public int getRearEndHealth() {
        return this.rearEndHealth;
    }

    public int getStorageCapacity() {
        return this.storageCapacity;
    }

    public Skin getTextures() {
        return this.textures;
    }

    public int getSkinCount() {
        return this.skins.size();
    }

    public Skin getSkin(int index) {
        return this.skins.get(index);
    }

    public int getAreaCount() {
        return this.areas.size();
    }

    public Area getArea(int index) {
        return this.areas.get(index);
    }

    public Area getAreaById(String id) {
        for (int i = 0; i < this.areas.size(); ++i) {
            Area area = this.areas.get(i);
            if (area.id == null || !area.id.equals(id)) continue;
            return area;
        }
        return null;
    }

    public int getIndexOfAreaById(String id) {
        for (int i = 0; i < this.areas.size(); ++i) {
            Area area = this.areas.get(i);
            if (area.id == null || !area.id.equals(id)) continue;
            return i;
        }
        return -1;
    }

    public int getPartCount() {
        return this.parts.size();
    }

    public Part getPart(int index) {
        return this.parts.get(index);
    }

    public Part getPartById(String id) {
        for (int i = 0; i < this.parts.size(); ++i) {
            Part part = this.parts.get(i);
            if (part.id == null || !part.id.equals(id)) continue;
            return part;
        }
        return null;
    }

    public int getIndexOfPartById(String id) {
        for (int i = 0; i < this.parts.size(); ++i) {
            Part part = this.parts.get(i);
            if (part.id == null || !part.id.equals(id)) continue;
            return i;
        }
        return -1;
    }

    private Anim getAnimationById(String id, ArrayList<Anim> anims) {
        for (int i = 0; i < anims.size(); ++i) {
            Anim anim = anims.get(i);
            if (anim.id == null || !anim.id.equals(id)) continue;
            return anim;
        }
        return null;
    }

    private Position getPositionById(String id, ArrayList<Position> positions) {
        for (int i = 0; i < positions.size(); ++i) {
            Position position = positions.get(i);
            if (position.id == null || !position.id.equals(id)) continue;
            return position;
        }
        return null;
    }

    public boolean globMatch(String pattern, String str) {
        Pattern pattern1 = Pattern.compile(pattern.replaceAll("\\*", ".*"));
        return pattern1.matcher(str).matches();
    }

    public int getGearRatioCount() {
        return this.gearRatioCount;
    }

    public int getSeats() {
        return this.seats;
    }

    public void setSeats(int seats) {
        this.seats = seats;
    }

    public int getMechanicType() {
        return this.mechanicType;
    }

    public void setMechanicType(int mechanicType) {
        this.mechanicType = mechanicType;
    }

    public int getEngineRepairLevel() {
        return this.engineRepairLevel;
    }

    public int getHeadlightConfigLevel() {
        return 2;
    }

    public void setEngineRepairLevel(int engineRepairLevel) {
        this.engineRepairLevel = engineRepairLevel;
    }

    public float getPlayerDamageProtection() {
        return this.playerDamageProtection;
    }

    public void setPlayerDamageProtection(float playerDamageProtection) {
        this.playerDamageProtection = playerDamageProtection;
    }

    public float getForcedHue() {
        return this.forcedHue;
    }

    public void setForcedHue(float forcedHue) {
        this.forcedHue = forcedHue;
    }

    public float getForcedSat() {
        return this.forcedSat;
    }

    public void setForcedSat(float forcedSat) {
        this.forcedSat = forcedSat;
    }

    public float getForcedVal() {
        return this.forcedVal;
    }

    public void setForcedVal(float forcedVal) {
        this.forcedVal = forcedVal;
    }

    public String getEngineRPMType() {
        return this.engineRpmType;
    }

    public void setEngineRPMType(String engineRpmType) {
        this.engineRpmType = engineRpmType;
    }

    public float getOffroadEfficiency() {
        return this.offroadEfficiency;
    }

    public void setOffroadEfficiency(float offroadEfficiency) {
        this.offroadEfficiency = offroadEfficiency;
    }

    public TFloatArrayList getCrawlOffsets() {
        return this.crawlOffsets;
    }

    public float getAnimalTrailerSize() {
        return this.animalTrailerSize;
    }

    public ArrayList<String> getZombieType() {
        return this.zombieType;
    }

    public ArrayList<String> getSpecialKeyRing() {
        return this.specialKeyRing;
    }

    public String getRandomZombieType() {
        if (this.getZombieType().isEmpty()) {
            return null;
        }
        return this.getZombieType().get(Rand.Next(this.getZombieType().size()));
    }

    public String getRandomSpecialKeyRing() {
        if (!this.hasSpecialKeyRing()) {
            return ItemKey.Container.KEY_RING.toString();
        }
        return PZArrayUtil.pickRandom(this.getSpecialKeyRing());
    }

    public boolean hasSpecialKeyRing() {
        if (this.getSpecialKeyRing() == null) {
            return false;
        }
        return !this.getSpecialKeyRing().isEmpty();
    }

    public String getFirstZombieType() {
        if (this.getZombieType().isEmpty()) {
            return null;
        }
        return this.getZombieType().get(0);
    }

    public boolean hasZombieType(String outfit) {
        for (int i = 0; i < this.getZombieType().size(); ++i) {
            if (this.getZombieType().get(i) == null || this.getZombieType().get(i) != outfit) continue;
            return true;
        }
        return false;
    }

    public boolean notKillCrops() {
        return this.notKillCrops;
    }

    public boolean hasLighter() {
        return this.hasLighter;
    }

    public String getCarMechanicsOverlay() {
        return this.carMechanicsOverlay;
    }

    public void setCarMechanicsOverlay(String overlay) {
        this.carMechanicsOverlay = overlay;
    }

    public String getCarModelName() {
        return this.carModelName;
    }

    public void setCarModelName(String overlay) {
        this.carModelName = overlay;
    }

    public int getSpecialLootChance() {
        return this.specialLootChance;
    }

    public int getSpecialKeyRingChance() {
        if (!this.hasSpecialKeyRing()) {
            return 0;
        }
        return this.specialKeyRingChance;
    }

    public boolean neverSpawnKey() {
        return this.neverSpawnKey;
    }

    public static final class Skin {
        public String texture;
        public String textureRust;
        public String textureMask;
        public String textureLights;
        public String textureDamage1Overlay;
        public String textureDamage1Shell;
        public String textureDamage2Overlay;
        public String textureDamage2Shell;
        public String textureShadow;
        public Texture textureData;
        public Texture textureDataRust;
        public Texture textureDataMask;
        public Texture textureDataLights;
        public Texture textureDataDamage1Overlay;
        public Texture textureDataDamage1Shell;
        public Texture textureDataDamage2Overlay;
        public Texture textureDataDamage2Shell;
        public Texture textureDataShadow;

        public void copyMissingFrom(Skin other) {
            if (this.textureRust == null) {
                this.textureRust = other.textureRust;
            }
            if (this.textureMask == null) {
                this.textureMask = other.textureMask;
            }
            if (this.textureLights == null) {
                this.textureLights = other.textureLights;
            }
            if (this.textureDamage1Overlay == null) {
                this.textureDamage1Overlay = other.textureDamage1Overlay;
            }
            if (this.textureDamage1Shell == null) {
                this.textureDamage1Shell = other.textureDamage1Shell;
            }
            if (this.textureDamage2Overlay == null) {
                this.textureDamage2Overlay = other.textureDamage2Overlay;
            }
            if (this.textureDamage2Shell == null) {
                this.textureDamage2Shell = other.textureDamage2Shell;
            }
            if (this.textureShadow == null) {
                this.textureShadow = other.textureShadow;
            }
        }
    }

    public static final class LightBar {
        public boolean enable;
        public String soundSiren0 = "";
        public String soundSiren1 = "";
        public String soundSiren2 = "";
    }

    public static final class Sounds {
        public Boolean alarmEnable = false;
        public ArrayList<String> alarm = new ArrayList();
        public ArrayList<String> alarmLoop = new ArrayList();
        public boolean hornEnable;
        public String horn = "";
        public boolean backSignalEnable;
        public String backSignal = "";
        public String engine;
        public String engineStart;
        public String engineTurnOff;
        public String handBrake;
        public String ignitionFail;
        public String ignitionFailNoPower;
        public final HashSet<String> specified = new HashSet();
    }

    @UsedFromLua
    public static final class Area {
        public String id;
        public float x;
        public float y;
        public float w;
        public float h;

        public String getId() {
            return this.id;
        }

        public Double getX() {
            return BoxedStaticValues.toDouble(this.x);
        }

        public Double getY() {
            return BoxedStaticValues.toDouble(this.y);
        }

        public Double getW() {
            return BoxedStaticValues.toDouble(this.w);
        }

        public Double getH() {
            return BoxedStaticValues.toDouble(this.h);
        }

        public void setX(Double d) {
            this.x = d.floatValue();
        }

        public void setY(Double d) {
            this.y = d.floatValue();
        }

        public void setW(Double d) {
            this.w = d.floatValue();
        }

        public void setH(Double d) {
            this.h = d.floatValue();
        }

        private Area makeCopy() {
            Area copy = new Area();
            copy.id = this.id;
            copy.x = this.x;
            copy.y = this.y;
            copy.w = this.w;
            copy.h = this.h;
            return copy;
        }
    }

    @UsedFromLua
    public static final class Model {
        public String id;
        public String file;
        public float scale = 1.0f;
        public final Vector3f offset = new Vector3f();
        public final Vector3f rotate = new Vector3f();
        public String attachmentNameParent;
        public String attachmentNameSelf;
        public boolean ignoreVehicleScale;

        public String getId() {
            return this.id;
        }

        public String getFile() {
            return this.file;
        }

        public float getScale() {
            return this.scale;
        }

        public Vector3f getOffset() {
            return this.offset;
        }

        public Vector3f getRotate() {
            return this.rotate;
        }

        public String getAttachmentNameParent() {
            return this.attachmentNameParent;
        }

        public String getAttachmentNameSelf() {
            return this.attachmentNameSelf;
        }

        Model makeCopy() {
            Model copy = new Model();
            copy.id = this.id;
            copy.file = this.file;
            copy.scale = this.scale;
            copy.offset.set(this.offset);
            copy.rotate.set(this.rotate);
            copy.attachmentNameParent = this.attachmentNameParent;
            copy.attachmentNameSelf = this.attachmentNameSelf;
            copy.ignoreVehicleScale = this.ignoreVehicleScale;
            return copy;
        }
    }

    @UsedFromLua
    public static final class Part {
        public String id = "Unknown";
        public String parent;
        public ArrayList<String> itemType;
        public Container container;
        public String area;
        public String mechanicArea;
        public String wheel;
        public THashMap<String, KahluaTable> tables;
        public THashMap<String, String> luaFunctions;
        public ArrayList<Model> models;
        public boolean setAllModelsVisible = true;
        public Door door;
        public Window window;
        public ArrayList<Anim> anims;
        public String category;
        public boolean specificItem = true;
        public boolean mechanicRequireKey;
        public boolean repairMechanic;
        public boolean hasLightsRear;
        private float durability;

        public boolean isMechanicRequireKey() {
            return this.mechanicRequireKey;
        }

        public void setMechanicRequireKey(boolean mechanicRequireKey) {
            this.mechanicRequireKey = mechanicRequireKey;
        }

        public boolean isRepairMechanic() {
            return this.repairMechanic;
        }

        public void setRepairMechanic(boolean repairMechanic) {
            this.repairMechanic = repairMechanic;
        }

        public String getId() {
            return this.id;
        }

        public int getModelCount() {
            return this.models == null ? 0 : this.models.size();
        }

        public Model getModel(int index) {
            return this.models.get(index);
        }

        public float getDurability() {
            return this.durability;
        }

        public String getMechanicArea() {
            return this.mechanicArea;
        }

        public Anim getAnimById(String id) {
            if (this.anims == null) {
                return null;
            }
            for (int i = 0; i < this.anims.size(); ++i) {
                Anim anim = this.anims.get(i);
                if (!anim.id.equals(id)) continue;
                return anim;
            }
            return null;
        }

        public Model getModelById(String id) {
            if (this.models == null) {
                return null;
            }
            for (int i = 0; i < this.models.size(); ++i) {
                Model model = this.models.get(i);
                if (!model.id.equals(id)) continue;
                return model;
            }
            return null;
        }

        Part makeCopy() {
            Part copy = new Part();
            copy.id = this.id;
            copy.parent = this.parent;
            if (this.itemType != null) {
                copy.itemType = new ArrayList();
                copy.itemType.addAll(this.itemType);
            }
            if (this.container != null) {
                copy.container = this.container.makeCopy();
            }
            copy.area = this.area;
            copy.mechanicArea = this.mechanicArea;
            copy.wheel = this.wheel;
            if (this.tables != null) {
                copy.tables = new THashMap();
                for (Map.Entry<String, KahluaTable> entry : this.tables.entrySet()) {
                    KahluaTable copyOfTable = LuaManager.copyTable(entry.getValue());
                    copy.tables.put(entry.getKey(), copyOfTable);
                }
            }
            if (this.luaFunctions != null) {
                copy.luaFunctions = new THashMap();
                copy.luaFunctions.putAll(this.luaFunctions);
            }
            if (this.models != null) {
                copy.models = new ArrayList();
                for (int i = 0; i < this.models.size(); ++i) {
                    copy.models.add(this.models.get(i).makeCopy());
                }
            }
            copy.setAllModelsVisible = this.setAllModelsVisible;
            if (this.door != null) {
                copy.door = this.door.makeCopy();
            }
            if (this.window != null) {
                copy.window = this.window.makeCopy();
            }
            if (this.anims != null) {
                copy.anims = new ArrayList();
                for (int i = 0; i < this.anims.size(); ++i) {
                    copy.anims.add(this.anims.get(i).makeCopy());
                }
            }
            copy.category = this.category;
            copy.specificItem = this.specificItem;
            copy.mechanicRequireKey = this.mechanicRequireKey;
            copy.repairMechanic = this.repairMechanic;
            copy.hasLightsRear = this.hasLightsRear;
            copy.durability = this.durability;
            return copy;
        }

        void compact() {
            if (this.anims != null) {
                this.anims.trimToSize();
            }
            if (this.itemType != null) {
                this.itemType.trimToSize();
            }
            if (this.luaFunctions != null) {
                this.luaFunctions.trimToSize();
            }
            if (this.models != null) {
                this.models.trimToSize();
            }
            if (this.tables != null) {
                this.tables.trimToSize();
            }
        }
    }

    @UsedFromLua
    public static final class Passenger {
        public String id;
        public final ArrayList<Anim> anims = new ArrayList();
        public final ArrayList<SwitchSeat> switchSeats = new ArrayList();
        public boolean hasRoof = true;
        public boolean showPassenger;
        public String door;
        public String door2;
        public String area;
        public final ArrayList<Position> positions = new ArrayList();

        public String getId() {
            return this.id;
        }

        public Passenger makeCopy() {
            int i;
            Passenger copy = new Passenger();
            copy.id = this.id;
            for (i = 0; i < this.anims.size(); ++i) {
                copy.anims.add(this.anims.get(i).makeCopy());
            }
            for (i = 0; i < this.switchSeats.size(); ++i) {
                copy.switchSeats.add(this.switchSeats.get(i).makeCopy());
            }
            copy.hasRoof = this.hasRoof;
            copy.showPassenger = this.showPassenger;
            copy.door = this.door;
            copy.door2 = this.door2;
            copy.area = this.area;
            for (i = 0; i < this.positions.size(); ++i) {
                copy.positions.add(this.positions.get(i).makeCopy());
            }
            return copy;
        }

        public int getPositionCount() {
            return this.positions.size();
        }

        public Position getPosition(int index) {
            return this.positions.get(index);
        }

        public Position getPositionById(String id) {
            for (int i = 0; i < this.positions.size(); ++i) {
                Position position = this.positions.get(i);
                if (position.id == null || !position.id.equals(id)) continue;
                return position;
            }
            return null;
        }

        public SwitchSeat getSwitchSeatById(String id) {
            for (int i = 0; i < this.switchSeats.size(); ++i) {
                SwitchSeat switchSeat = this.switchSeats.get(i);
                if (switchSeat.id == null || !switchSeat.id.equals(id)) continue;
                return switchSeat;
            }
            return null;
        }

        public static final class SwitchSeat {
            public String id;
            public int seat;
            public String anim;
            public float rate = 1.0f;
            public String sound;

            public String getId() {
                return this.id;
            }

            public SwitchSeat makeCopy() {
                SwitchSeat copy = new SwitchSeat();
                copy.id = this.id;
                copy.seat = this.seat;
                copy.anim = this.anim;
                copy.rate = this.rate;
                copy.sound = this.sound;
                return copy;
            }
        }
    }

    @UsedFromLua
    public static final class PhysicsShape {
        public int type;
        public final Vector3f offset = new Vector3f();
        public final Vector3f rotate = new Vector3f();
        public final Vector3f extents = new Vector3f();
        public float radius;
        public String physicsShapeScript;

        public String getTypeString() {
            return switch (this.type) {
                case 1 -> "box";
                case 2 -> "sphere";
                case 3 -> "mesh";
                default -> throw new RuntimeException("unhandled VehicleScript.PhysicsShape");
            };
        }

        public Vector3f getOffset() {
            return this.offset;
        }

        public Vector3f getExtents() {
            return this.extents;
        }

        public Vector3f getRotate() {
            return this.rotate;
        }

        public float getRadius() {
            return this.radius;
        }

        public void setRadius(float radius) {
            this.radius = PZMath.clamp(radius, 0.05f, 5.0f);
        }

        public String getPhysicsShapeScript() {
            return this.physicsShapeScript;
        }

        public void setPhysicsShapeScript(String scriptId) {
            this.physicsShapeScript = Objects.requireNonNull(scriptId);
        }

        private PhysicsShape makeCopy() {
            PhysicsShape copy = new PhysicsShape();
            copy.type = this.type;
            copy.extents.set(this.extents);
            copy.offset.set(this.offset);
            copy.rotate.set(this.rotate);
            copy.radius = this.radius;
            return copy;
        }
    }

    @UsedFromLua
    public static final class Wheel {
        public String id;
        public String model;
        public boolean front;
        public final Vector3f offset = new Vector3f();
        public float radius = 0.5f;
        public float width = 0.4f;

        public String getId() {
            return this.id;
        }

        public Vector3f getOffset() {
            return this.offset;
        }

        private Wheel makeCopy() {
            Wheel copy = new Wheel();
            copy.id = this.id;
            copy.model = this.model;
            copy.front = this.front;
            copy.offset.set(this.offset);
            copy.radius = this.radius;
            copy.width = this.width;
            return copy;
        }
    }

    @UsedFromLua
    public static final class Position {
        public String id;
        public final Vector3f offset = new Vector3f();
        public final Vector3f rotate = new Vector3f();
        public String area;

        public String getId() {
            return this.id;
        }

        public Vector3f getOffset() {
            return this.offset;
        }

        public Vector3f getRotate() {
            return this.rotate;
        }

        public String getArea() {
            return this.area;
        }

        private Position makeCopy() {
            Position copy = new Position();
            copy.id = this.id;
            copy.offset.set(this.offset);
            copy.rotate.set(this.rotate);
            return copy;
        }
    }

    public static final class Container {
        public int capacity;
        public int seat = -1;
        public String seatId;
        public String luaTest;
        public String contentType;
        public boolean conditionAffectsCapacity;

        Container makeCopy() {
            Container copy = new Container();
            copy.capacity = this.capacity;
            copy.seat = this.seat;
            copy.seatId = this.seatId;
            copy.luaTest = this.luaTest;
            copy.contentType = this.contentType;
            copy.conditionAffectsCapacity = this.conditionAffectsCapacity;
            return copy;
        }
    }

    public static final class Anim {
        public String id;
        public String anim;
        public float rate = 1.0f;
        public boolean animate = true;
        public boolean loop;
        public boolean reverse;
        public final Vector3f offset = new Vector3f();
        public final Vector3f angle = new Vector3f();
        public String sound;

        private Anim makeCopy() {
            Anim copy = new Anim();
            copy.id = this.id;
            copy.anim = this.anim;
            copy.rate = this.rate;
            copy.animate = this.animate;
            copy.loop = this.loop;
            copy.reverse = this.reverse;
            copy.offset.set(this.offset);
            copy.angle.set(this.angle);
            copy.sound = this.sound;
            return copy;
        }
    }

    public static final class Door {
        private Door makeCopy() {
            return new Door();
        }
    }

    public static final class Window {
        public boolean openable;

        private Window makeCopy() {
            Window copy = new Window();
            copy.openable = this.openable;
            return copy;
        }
    }
}

