/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.properties;

import java.util.Map;
import zombie.UsedFromLua;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public enum IsoObjectChange {
    ENERGY("Energy"),
    LIGHT_RADIUS("LightRadius"),
    CONTAINERS("containers"),
    CONTAINER("container"),
    CONTAINER_CUSTOM_TEMPERATURE("container.customTemperature"),
    NAME("name"),
    REPLACE_WITH("replaceWith"),
    USES_EXTERNAL_WATER_SOURCE("usesExternalWaterSource"),
    EMPTY_TRASH("emptyTrash"),
    SPRITE("sprite"),
    STATE("state"),
    WASHER_STATE("washer.state"),
    DRYER_STATE("dryer.state"),
    MODE("mode"),
    BECOME_SKELETON("becomeSkeleton"),
    ZOMBIE_ROT_STAGE("zombieRotStage"),
    OBJECT_ID("objectID"),
    ADD_SHEET("addSheet"),
    REMOVE_SHEET("removeSheet"),
    SET_CURTAIN_OPEN("setCurtainOpen"),
    ADD_ITEM("addItem"),
    ADD_ITEM_OF_TYPE("addItemOfType"),
    ADD_RANDOM_DAMAGE_FROM_ZOMBIE("AddRandomDamageFromZombie"),
    ADD_ZOMBIE_KILL("AddZombieKill"),
    REMOVE_ITEM("removeItem"),
    REMOVE_ITEM_ID("removeItemID"),
    REMOVE_ITEM_TYPE("removeItemType"),
    REMOVE_ONE_OF("removeOneOf"),
    REANIMATED_ID("reanimatedID"),
    SHOVE("Shove"),
    WAKE_UP("wakeUp"),
    MECHANIC_ACTION_DONE("mechanicActionDone"),
    EXIT_VEHICLE("exitVehicle"),
    STOP_BURNING("StopBurning"),
    VEHICLE_NO_KEY("vehicleNoKey"),
    ROTATE("rotate"),
    LIGHT_SOURCE("lightSource"),
    PAINTABLE("paintable"),
    SWAP_ITEM("swapItem"),
    PLAY_GAIN_EXPERIENCE_LEVEL_SOUND("playGainExperienceLevelSound");

    private static final Map<String, IsoObjectChange> BY_NAME;
    private final String name;

    private IsoObjectChange(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public boolean isProperty(String inObjectChangeName) {
        return StringUtils.equalsIgnoreCase(this.name, inObjectChangeName);
    }

    public String toString() {
        return this.getName();
    }

    public static IsoObjectChange lookup(String name) {
        IsoObjectChange propertyType = BY_NAME.get(name);
        if (propertyType == null) {
            throw new IsoObjectChangeNotFoundException(name);
        }
        return propertyType;
    }

    public static IsoObjectChange lookup(Object obj) {
        if (obj instanceof IsoObjectChange) {
            IsoObjectChange change = (IsoObjectChange)((Object)obj);
            return change;
        }
        return IsoObjectChange.lookup(String.valueOf(obj));
    }

    static {
        BY_NAME = PZArrayUtil.generateNameToEnumLookUpTable(IsoObjectChange.class, IsoObjectChange::getName);
    }

    public static class IsoObjectChangeNotFoundException
    extends RuntimeException {
        public IsoObjectChangeNotFoundException(String inName) {
            super("ObjectChange Name not found: " + inName);
        }
    }
}

