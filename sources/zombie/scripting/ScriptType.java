/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;
import zombie.UsedFromLua;

@UsedFromLua
public enum ScriptType {
    EntityComponent("entityComponent"),
    VehicleTemplate(true, "vehicle"),
    EntityTemplate(true, "entity"),
    Item("item"),
    Recipe("recipe"),
    UniqueRecipe("uniquerecipe"),
    EvolvedRecipe("evolvedrecipe"),
    Fixing("fixing"),
    AnimationMesh("animationsMesh"),
    Mannequin("mannequin"),
    Model("model"),
    SpriteModel("spriteModel"),
    Sound("sound"),
    SoundTimeline("soundTimeline"),
    Vehicle("vehicle"),
    RuntimeAnimation("animation"),
    VehicleEngineRPM("vehicleEngineRPM"),
    ItemConfig("itemConfig"),
    Entity("entity"),
    XuiLayout("xuiLayout"),
    XuiStyle("xuiStyle"),
    XuiDefaultStyle("xuiDefaultStyle"),
    XuiColor("xuiGlobalColors"),
    XuiSkin("xuiSkin"),
    XuiConfig("xuiConfig"),
    ItemFilter("itemFilter"),
    CraftRecipe("craftRecipe"),
    FluidFilter("fluidFilter"),
    StringList("stringList"),
    EnergyDefinition("energy"),
    FluidDefinition("fluid"),
    PhysicsShape("physicsShape"),
    TimedAction("timedAction"),
    Ragdoll("ragdoll"),
    PhysicsHitReaction("physicsHitReaction"),
    Clock("clock"),
    CharacterTraitDefinition("character_trait_definition"),
    CharacterProfessionDefinition("character_profession_definition");

    private static final ArrayList<ScriptType> sortedList;
    private static final Comparator<ScriptType> typeComparator;
    private final boolean isTemplate;
    private final String scriptTag;
    private final boolean isCritical = false;
    private Set<Flags> flags;
    private boolean verbose;

    private ScriptType(String scriptTag) {
        this(false, scriptTag);
    }

    private ScriptType(boolean isTemplate, String scriptTag) {
        this.isTemplate = isTemplate;
        this.scriptTag = scriptTag;
    }

    public boolean isTemplate() {
        return this.isTemplate;
    }

    public boolean isCritical() {
        return false;
    }

    public String getScriptTag() {
        return this.scriptTag;
    }

    public boolean hasFlag(Flags flag) {
        return this.flags.contains((Object)flag);
    }

    public boolean hasFlags(EnumSet<Flags> flags) {
        return this.flags.containsAll(flags);
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    public void setVerbose(boolean b) {
        this.verbose = b;
    }

    public static ArrayList<ScriptType> GetEnumListLua() {
        return sortedList;
    }

    static {
        typeComparator = (object1, object2) -> {
            if (object1.isTemplate && !object2.isTemplate) {
                return 1;
            }
            if (!object1.isTemplate && object2.isTemplate) {
                return -1;
            }
            return object1.toString().compareTo(object2.toString());
        };
        EnumSet<Flags[]> defaultFlags = EnumSet.of(Flags.Clear, new Flags[]{Flags.CacheFullType, Flags.ResetExisting, Flags.RemoveLoadError, Flags.SeekImports, Flags.AllowNewScriptDiscoveryOnReload});
        EnumSet<Flags[]> dictionaryFlags = EnumSet.copyOf(defaultFlags);
        dictionaryFlags.remove((Object)Flags.AllowNewScriptDiscoveryOnReload);
        ScriptType.Item.flags = Collections.unmodifiableSet(dictionaryFlags);
        ScriptType.Entity.flags = Collections.unmodifiableSet(dictionaryFlags);
        EnumSet<Flags[]> vehicleFlags = EnumSet.copyOf(defaultFlags);
        vehicleFlags.remove((Object)Flags.ResetExisting);
        vehicleFlags.add((Flags[])Flags.NewInstanceOnReload);
        ScriptType.Vehicle.flags = Collections.unmodifiableSet(vehicleFlags);
        ScriptType.XuiSkin.flags = Collections.unmodifiableSet(EnumSet.of(Flags.Clear, Flags.CacheFullType, Flags.RemoveLoadError, Flags.SeekImports, Flags.ResetOnceOnReload));
        XuiSkin.setVerbose(true);
        ArrayList<ScriptType> list = new ArrayList<ScriptType>();
        for (ScriptType type : ScriptType.values()) {
            if (type.flags == null) {
                type.flags = Collections.unmodifiableSet(defaultFlags);
            }
            if (type == EntityComponent) continue;
            list.add(type);
        }
        list.sort(typeComparator);
        sortedList = list;
    }

    public static enum Flags {
        Clear,
        FromList,
        CacheFullType,
        ResetExisting,
        RemoveLoadError,
        SeekImports,
        ResetOnceOnReload,
        AllowNewScriptDiscoveryOnReload,
        NewInstanceOnReload;

    }
}

