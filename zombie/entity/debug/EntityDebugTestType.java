/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.debug;

import java.util.ArrayList;
import java.util.Arrays;
import zombie.UsedFromLua;

@UsedFromLua
public enum EntityDebugTestType {
    BaseTest;

    private static final ArrayList<EntityDebugTestType> typeList;

    public static ArrayList<EntityDebugTestType> getValueList() {
        return typeList;
    }

    static {
        typeList = new ArrayList();
        typeList.addAll(Arrays.asList(EntityDebugTestType.values()));
    }
}

