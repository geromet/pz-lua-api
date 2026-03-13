/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;
import zombie.debug.options.OptionGroup;

public class CollideWithObstacles
extends OptionGroup {
    public final DebugOG debug = this.newOptionGroup(new DebugOG());
    public final RenderOG render = this.newOptionGroup(new RenderOG());

    public static final class DebugOG
    extends OptionGroup {
        public final BooleanDebugOption slideAwayFromWalls = this.newDebugOnlyOption("SlideAwayFromWalls", true);
    }

    public static final class RenderOG
    extends OptionGroup {
        public final BooleanDebugOption radius = this.newDebugOnlyOption("Radius", false);
        public final BooleanDebugOption obstacles = this.newDebugOnlyOption("Obstacles", false);
        public final BooleanDebugOption normals = this.newDebugOnlyOption("Normals", false);
    }
}

