/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals;

import java.util.ArrayList;
import java.util.HashMap;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;

@UsedFromLua
public class AnimalGenomeDefinitions {
    public String name;
    public float currentValue;
    public HashMap<String, Float> ratios;
    public float minValue = 0.2f;
    public float maxValue = 0.6f;
    public static HashMap<String, AnimalGenomeDefinitions> fullGenomeDef;
    public static ArrayList<String> geneticDisorder;
    public boolean forcedValues;

    public static void loadGenomeDefinition() {
        String name;
        fullGenomeDef = new HashMap();
        KahluaTableImpl definitions = (KahluaTableImpl)LuaManager.env.rawget("AnimalGenomeDefinitions");
        if (definitions == null) {
            return;
        }
        KahluaTableImpl genes = (KahluaTableImpl)definitions.rawget("genes");
        KahluaTableIterator iterator2 = genes.iterator();
        while (iterator2.advance()) {
            AnimalGenomeDefinitions def = new AnimalGenomeDefinitions();
            def.name = name = iterator2.getKey().toString().toLowerCase();
            KahluaTableIterator it2 = ((KahluaTableImpl)iterator2.getValue()).iterator();
            while (it2.advance()) {
                String key = (String)it2.getKey();
                Object value = it2.getValue();
                String valueStr = value.toString().trim();
                if ("minValue".equalsIgnoreCase(key)) {
                    def.minValue = Float.parseFloat(valueStr);
                }
                if ("maxValue".equalsIgnoreCase(key)) {
                    def.maxValue = Float.parseFloat(valueStr);
                }
                if ("forcedValues".equalsIgnoreCase(key)) {
                    def.forcedValues = Boolean.parseBoolean(valueStr);
                }
                if (!"ratio".equalsIgnoreCase(key)) continue;
                def.loadRatio((KahluaTableImpl)value);
            }
            fullGenomeDef.put(name, def);
        }
        KahluaTableImpl disorder = (KahluaTableImpl)definitions.rawget("geneticDisorder");
        geneticDisorder = new ArrayList();
        iterator2 = disorder.iterator();
        while (iterator2.advance()) {
            name = iterator2.getKey().toString().toLowerCase();
            if (geneticDisorder.contains(name)) continue;
            geneticDisorder.add(name);
        }
    }

    private void loadRatio(KahluaTableImpl def) {
        this.ratios = new HashMap();
        KahluaTableIterator it = def.iterator();
        while (it.advance()) {
            String name = it.getKey().toString().toLowerCase();
            String valueStr = it.getValue().toString().trim();
            this.ratios.put(name, Float.valueOf(Float.parseFloat(valueStr)));
        }
    }

    public static ArrayList<String> getGeneticDisorderList() {
        return geneticDisorder;
    }
}

