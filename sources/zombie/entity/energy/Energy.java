/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.energy;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.debug.DebugType;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.energy.EnergyType;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.EnergyDefinitionScript;

@DebugClassFields
@UsedFromLua
public class Energy {
    private static boolean hasInitialized;
    private static final HashMap<EnergyType, Energy> energyEnumMap;
    private static final HashMap<String, Energy> energyStringMap;
    private static final HashMap<String, Energy> cacheStringMap;
    private static final ArrayList<Energy> allEnergies;
    public static final Energy Electric;
    public static final Energy Mechanical;
    public static final Energy Thermal;
    public static final Energy Steam;
    public static final Energy VoidEnergy;
    private EnergyDefinitionScript script;
    private final EnergyType energyType;
    private final String energyTypeString;
    private final Color color = new Color(1.0f, 1.0f, 1.0f, 1.0f);

    private static Energy addEnergy(EnergyType type) {
        if (energyEnumMap.containsKey((Object)type)) {
            throw new RuntimeException("Energy defined twice: " + String.valueOf((Object)type));
        }
        Energy energy = new Energy(type);
        energyEnumMap.put(type, energy);
        return energy;
    }

    public static Energy Get(EnergyType type) {
        if (Core.debug && !hasInitialized) {
            throw new RuntimeException("Energies have not yet been initialized!");
        }
        return energyEnumMap.get((Object)type);
    }

    public static Energy Get(String name) {
        if (Core.debug && !hasInitialized) {
            throw new RuntimeException("Energies have not yet been initialized!");
        }
        return energyStringMap.get(name);
    }

    public static ArrayList<Energy> getAllEnergies() {
        if (Core.debug && !hasInitialized) {
            throw new RuntimeException("Energies have not yet been initialized!");
        }
        return allEnergies;
    }

    public static void Init(ScriptLoadMode loadMode) throws Exception {
        DebugType.Energy.println("*************************************");
        DebugType.Energy.println("* Energy: initialize Energies.      *");
        DebugType.Energy.println("*************************************");
        ArrayList<EnergyDefinitionScript> scripts = ScriptManager.instance.getAllEnergyDefinitionScripts();
        cacheStringMap.clear();
        allEnergies.clear();
        if (loadMode == ScriptLoadMode.Reload) {
            cacheStringMap.putAll(energyStringMap);
            energyStringMap.clear();
        }
        for (EnergyDefinitionScript energyDefinitionScript : scripts) {
            Energy energy;
            if (energyDefinitionScript.getEnergyType() == EnergyType.Modded) {
                DebugType.Energy.println(energyDefinitionScript.getModID() + " = " + energyDefinitionScript.getEnergyTypeString());
                energy = cacheStringMap.get(energyDefinitionScript.getEnergyTypeString());
                if (energy == null) {
                    energy = new Energy(energyDefinitionScript.getEnergyTypeString());
                }
                energy.setScript(energyDefinitionScript);
                energyStringMap.put(energyDefinitionScript.getEnergyTypeString(), energy);
                allEnergies.add(energy);
                continue;
            }
            DebugType.Energy.println(energyDefinitionScript.getModID() + " = " + String.valueOf((Object)energyDefinitionScript.getEnergyType()));
            energy = energyEnumMap.get((Object)energyDefinitionScript.getEnergyType());
            if (energy == null) {
                if (!Core.debug) continue;
                throw new Exception("Energy not found: " + String.valueOf((Object)energyDefinitionScript.getEnergyType()));
            }
            energy.setScript(energyDefinitionScript);
            allEnergies.add(energy);
        }
        for (Map.Entry entry : energyEnumMap.entrySet()) {
            if (Core.debug && ((Energy)entry.getValue()).script == null) {
                throw new Exception("Energy has no script set: " + String.valueOf(entry.getKey()));
            }
            energyStringMap.put(((EnergyType)((Object)entry.getKey())).toString(), (Energy)entry.getValue());
        }
        cacheStringMap.clear();
        hasInitialized = true;
        DebugType.Energy.println("*************************************");
    }

    public static void PreReloadScripts() {
        hasInitialized = false;
    }

    public static void Reset() {
        energyStringMap.clear();
        hasInitialized = false;
    }

    public static void saveEnergy(Energy energy, ByteBuffer output) {
        output.put(energy != null ? (byte)1 : 0);
        if (energy == null) {
            return;
        }
        if (energy.energyType == EnergyType.Modded) {
            output.put((byte)1);
            GameWindow.WriteString(output, energy.energyTypeString);
        } else {
            output.put((byte)0);
            output.put(energy.energyType.getId());
        }
    }

    public static Energy loadEnergy(ByteBuffer input, int worldVersion) {
        Energy energy;
        if (input.get() == 0) {
            return null;
        }
        if (input.get() != 0) {
            String energyTypeString = GameWindow.ReadString(input);
            energy = Energy.Get(energyTypeString);
        } else {
            EnergyType energyType = EnergyType.FromId(input.get());
            energy = Energy.Get(energyType);
        }
        return energy;
    }

    private Energy(EnergyType energyType) {
        this.energyType = energyType;
        this.energyTypeString = null;
    }

    private Energy(String energyTypeString) {
        this.energyType = EnergyType.Modded;
        this.energyTypeString = Objects.requireNonNull(energyTypeString);
    }

    private void setScript(EnergyDefinitionScript script) {
        this.script = Objects.requireNonNull(script);
        this.color.set(script.getColor());
    }

    public boolean isVanilla() {
        return this.script != null && this.script.isVanilla();
    }

    public String getDisplayName() {
        return this.script != null ? this.script.getDisplayName() : Translator.getEntityText("EC_Energy");
    }

    public Color getColor() {
        return this.color;
    }

    public Texture getIconTexture() {
        if (this.script != null) {
            return this.script.getIconTexture();
        }
        return EnergyDefinitionScript.getDefaultIconTexture();
    }

    public Texture getHorizontalBarTexture() {
        if (this.script != null) {
            return this.script.getHorizontalBarTexture();
        }
        return EnergyDefinitionScript.getDefaultHorizontalBarTexture();
    }

    public Texture getVerticalBarTexture() {
        if (this.script != null) {
            return this.script.getVerticalBarTexture();
        }
        return EnergyDefinitionScript.getDefaultVerticalBarTexture();
    }

    public String getEnergyTypeString() {
        if (this.energyType == EnergyType.Modded) {
            return this.energyTypeString;
        }
        return this.energyType.toString();
    }

    static {
        energyEnumMap = new HashMap();
        energyStringMap = new HashMap();
        cacheStringMap = new HashMap();
        allEnergies = new ArrayList();
        Electric = Energy.addEnergy(EnergyType.Electric);
        Mechanical = Energy.addEnergy(EnergyType.Mechanical);
        Thermal = Energy.addEnergy(EnergyType.Thermal);
        Steam = Energy.addEnergy(EnergyType.Steam);
        VoidEnergy = Energy.addEnergy(EnergyType.VoidEnergy);
    }
}

