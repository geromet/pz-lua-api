/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals;

import java.util.ArrayList;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.characters.animals.AnimalPart;

@UsedFromLua
public class AnimalPartsDefinitions {
    public static String getLeather(String animalType) {
        KahluaTableImpl animalDef = AnimalPartsDefinitions.getAnimalDef(animalType);
        if (animalDef == null) {
            return null;
        }
        return animalDef.rawgetStr("leather");
    }

    public static ArrayList<AnimalPart> getAllPartsDef(String animalType) {
        KahluaTableImpl animalDef = AnimalPartsDefinitions.getAnimalDef(animalType);
        if (animalDef == null) {
            return new ArrayList<AnimalPart>();
        }
        return AnimalPartsDefinitions.getDef(animalDef, "parts");
    }

    public static ArrayList<AnimalPart> getAllBonesDef(String animalType) {
        KahluaTableImpl animalDef = AnimalPartsDefinitions.getAnimalDef(animalType);
        if (animalDef == null) {
            return new ArrayList<AnimalPart>();
        }
        return AnimalPartsDefinitions.getDef(animalDef, "bones");
    }

    public static ArrayList<AnimalPart> getDef(KahluaTableImpl def, String type) {
        ArrayList<AnimalPart> result = new ArrayList<AnimalPart>();
        KahluaTableImpl parts = (KahluaTableImpl)def.rawget(type);
        if (parts == null) {
            return result;
        }
        KahluaTableIterator allParts = parts.iterator();
        while (allParts.advance()) {
            KahluaTableImpl partDef = (KahluaTableImpl)allParts.getValue();
            AnimalPart part = new AnimalPart(partDef);
            result.add(part);
        }
        return result;
    }

    public static KahluaTableImpl getAnimalDef(String animalType) {
        KahluaTableImpl definitions = (KahluaTableImpl)LuaManager.env.rawget("AnimalPartsDefinitions");
        if (definitions == null) {
            return null;
        }
        KahluaTableImpl animals = (KahluaTableImpl)definitions.rawget("animals");
        if (animals == null) {
            return null;
        }
        KahluaTableIterator iterator2 = animals.iterator();
        while (iterator2.advance()) {
            String type = iterator2.getKey().toString();
            if (!animalType.equals(type)) continue;
            return (KahluaTableImpl)iterator2.getValue();
        }
        return null;
    }
}

