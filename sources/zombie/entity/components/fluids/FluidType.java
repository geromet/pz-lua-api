/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.fluids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.Translator;

@UsedFromLua
public enum FluidType {
    Water(1),
    Petrol(2),
    RubbingAlcohol(3),
    TaintedWater(4),
    Beer(5),
    Whiskey(6),
    SodaPop(7),
    Coffee(8),
    Tea(9),
    Wine(10),
    Bleach(11),
    Blood(12),
    Honey(14),
    Mead(15),
    Acid(16),
    SpiffoJuice(17),
    SecretFlavoring(18),
    CarbonatedWater(19),
    CowMilk(20),
    SheepMilk(21),
    CleaningLiquid(22),
    AnimalBlood(23),
    AnimalGrease(24),
    Dye(64),
    HairDye(65),
    Paint(66),
    PoisonWeak(70),
    PoisonNormal(71),
    PoisonStrong(72),
    PoisonPotent(73),
    AnimalMilk(74),
    Modded(127),
    None(-1);

    private static final HashSet<String> fluidNames;
    private static final HashMap<Byte, FluidType> fluidIdMap;
    private static final HashMap<String, FluidType> fluidNameMap;
    private final byte id;
    private String lowerCache;

    private FluidType(byte typeID) {
        this.id = typeID;
    }

    public byte getId() {
        return this.id;
    }

    public String toStringLower() {
        if (this.lowerCache != null) {
            return this.lowerCache;
        }
        this.lowerCache = this.toString().toLowerCase();
        return this.lowerCache;
    }

    public static boolean containsNameLowercase(String name) {
        return fluidNames.contains(name.toLowerCase());
    }

    public static FluidType FromId(byte id) {
        return fluidIdMap.get(id);
    }

    public static FluidType FromNameLower(String name) {
        return fluidNameMap.get(name.toLowerCase());
    }

    public static ArrayList<String> getAllFluidName() {
        ArrayList<String> result = new ArrayList<String>(fluidNames);
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    public String getDisplayName() {
        return Translator.getText("Fluid_Name_" + String.valueOf((Object)this));
    }

    static {
        fluidNames = new HashSet();
        fluidIdMap = new HashMap();
        fluidNameMap = new HashMap();
        for (FluidType type : FluidType.values()) {
            if (Core.debug && fluidIdMap.containsKey(type.id)) {
                throw new IllegalStateException("ID duplicate in FluidType");
            }
            fluidNames.add(type.toString().toLowerCase());
            fluidNameMap.put(type.toString().toLowerCase(), type);
            fluidIdMap.put(type.id, type);
        }
    }
}

