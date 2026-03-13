/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.WornItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import zombie.UsedFromLua;
import zombie.characters.WornItems.BodyLocationGroup;

@UsedFromLua
public final class BodyLocations {
    private static final List<BodyLocationGroup> groups = new ArrayList<BodyLocationGroup>();

    public static BodyLocationGroup getGroup(String id) {
        for (BodyLocationGroup group : groups) {
            if (!group.getId().equals(id)) continue;
            return group;
        }
        BodyLocationGroup newGroup = new BodyLocationGroup(id);
        groups.add(newGroup);
        return newGroup;
    }

    public static void reset() {
        groups.clear();
    }

    public static List<BodyLocationGroup> getAllGroups() {
        return Collections.unmodifiableList(groups);
    }
}

