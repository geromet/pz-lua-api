/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.iso.IsoDirections;
import zombie.iso.Vector2;

@UsedFromLua
public final class CrawlingZombieTurnState
extends State {
    private static final CrawlingZombieTurnState INSTANCE = new CrawlingZombieTurnState();
    private static final Vector2 tempVector2_1 = new Vector2();
    private static final Vector2 tempVector2_2 = new Vector2();

    public static CrawlingZombieTurnState instance() {
        return INSTANCE;
    }

    private CrawlingZombieTurnState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        ((IsoZombie)owner).allowRepathDelay = 0.0f;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (event.eventName.equalsIgnoreCase("TurnSome")) {
            Vector2 startDir = tempVector2_1.set(owner.dir.ToVector());
            Vector2 endDir = "left".equalsIgnoreCase(event.parameterValue) ? owner.dir.RotLeft().ToVector() : owner.dir.RotRight().ToVector();
            Vector2 v = PZMath.lerp(tempVector2_2, startDir, endDir, event.timePc);
            owner.setForwardDirection(v);
            return;
        }
        if (event.eventName.equalsIgnoreCase("TurnComplete")) {
            owner.dir = "left".equalsIgnoreCase(event.parameterValue) ? owner.dir.RotLeft() : owner.dir.RotRight();
            owner.setForwardDirectionFromIsoDirection();
        }
    }

    public static boolean calculateDir(IsoGameCharacter owner, IsoDirections targetDir) {
        if (targetDir.ordinal() > owner.dir.ordinal()) {
            return targetDir.ordinal() - owner.dir.ordinal() <= 4;
        }
        return targetDir.ordinal() - owner.dir.ordinal() < -4;
    }
}

