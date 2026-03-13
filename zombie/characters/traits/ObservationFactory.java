/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.traits;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.interfaces.IListBoxItem;

@UsedFromLua
public final class ObservationFactory {
    public static HashMap<String, Observation> observationMap = new HashMap();

    public static void init() {
    }

    public static void setMutualExclusive(String a, String b) {
        ObservationFactory.observationMap.get((Object)a).mutuallyExclusive.add(b);
        ObservationFactory.observationMap.get((Object)b).mutuallyExclusive.add(a);
    }

    public static void addObservation(String type, String name, String desc) {
        observationMap.put(type, new Observation(type, name, desc));
    }

    public static Observation getObservation(String name) {
        if (observationMap.containsKey(name)) {
            return observationMap.get(name);
        }
        return null;
    }

    @UsedFromLua
    public static class Observation
    implements IListBoxItem {
        private String traitId;
        private String name;
        private String description;
        public ArrayList<String> mutuallyExclusive = new ArrayList(0);

        public Observation(String tr, String name, String desc) {
            this.setTraitID(tr);
            this.setName(name);
            this.setDescription(desc);
        }

        @Override
        public String getLabel() {
            return this.getName();
        }

        @Override
        public String getLeftLabel() {
            return this.getName();
        }

        @Override
        public String getRightLabel() {
            return null;
        }

        public String getDescription() {
            return this.description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getTraitID() {
            return this.traitId;
        }

        public void setTraitID(String traitId) {
            this.traitId = traitId;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}

