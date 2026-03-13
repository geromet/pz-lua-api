/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;
import zombie.debug.options.OptionGroup;

public class Model
extends OptionGroup {
    public final RenderOG render = this.newOptionGroup(new RenderOG());
    public final BooleanDebugOption forceSkeleton = this.newOption("Force.Skeleton", false);

    public static final class RenderOG
    extends OptionGroup {
        public final BooleanDebugOption limitTextureSize = this.newOption("LimitTextureSize", true);
        public final BooleanDebugOption attachments = this.newOption("Attachments", false);
        public final BooleanDebugOption axis = this.newOption("Axis", false);
        public final BooleanDebugOption bones = this.newOption("Bones", false);
        public final BooleanDebugOption bounds = this.newOption("Bounds", false);
        public final BooleanDebugOption forceAlphaOne = this.newDebugOnlyOption("ForceAlphaOne", false);
        public final BooleanDebugOption lights = this.newOption("Lights", false);
        public final BooleanDebugOption muzzleFlash = this.newOption("MuzzleFlash", false);
        public final BooleanDebugOption skipVehicles = this.newOption("SkipVehicles", false);
        public final BooleanDebugOption weaponHitPoint = this.newOption("WeaponHitPoint", false);
        public final BooleanDebugOption wireframe = this.newOption("Wireframe", false);
    }
}

