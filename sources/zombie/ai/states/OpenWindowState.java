/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.ai.states.SmashWindowState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.properties.IsoPropertyType;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.debug.DebugOptions;
import zombie.iso.IsoDirections;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoWindow;
import zombie.scripting.objects.CharacterTrait;

@UsedFromLua
public final class OpenWindowState
extends State {
    private static final OpenWindowState INSTANCE = new OpenWindowState();
    public static final State.Param<IsoWindow> WINDOW = State.Param.of("window", IsoWindow.class);

    public static OpenWindowState instance() {
        return INSTANCE;
    }

    private OpenWindowState() {
        super(true, false, true, true);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setIgnoreMovement(true);
        owner.setHideWeaponModel(true);
        IsoWindow window = owner.get(WINDOW);
        if (Core.debug && DebugOptions.instance.cheat.window.unlock.getValue() && window.getSprite() != null && !window.getSprite().getProperties().has(IsoPropertyType.WINDOW_LOCKED)) {
            window.setIsLocked(false);
            window.setPermaLocked(false);
        }
        if (window.isNorth()) {
            if ((float)window.getSquare().getY() < owner.getY()) {
                owner.setDir(IsoDirections.N);
            } else {
                owner.setDir(IsoDirections.S);
            }
        } else if ((float)window.getSquare().getX() < owner.getX()) {
            owner.setDir(IsoDirections.W);
        } else {
            owner.setDir(IsoDirections.E);
        }
        owner.setVariable("bOpenWindow", true);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        if (!owner.getVariableBoolean("bOpenWindow")) {
            return;
        }
        IsoPlayer player = (IsoPlayer)owner;
        if (player.pressedMovement(false) || player.pressedCancelAction()) {
            owner.setVariable("bOpenWindow", false);
            return;
        }
        IsoWindow window = owner.get(WINDOW);
        if (window == null || window.getObjectIndex() == -1) {
            owner.setVariable("bOpenWindow", false);
            return;
        }
        if (player.contextPanic > 5.0f) {
            player.contextPanic = 0.0f;
            owner.setVariable("bOpenWindow", false);
            owner.smashWindow(window);
            owner.set(SmashWindowState.CLIMB_THROUGH_WINDOW, true);
            return;
        }
        player.setCollidable(true);
        player.updateLOS();
        if (window.isNorth()) {
            if ((float)window.getSquare().getY() < owner.getY()) {
                owner.setDir(IsoDirections.N);
            } else {
                owner.setDir(IsoDirections.S);
            }
        } else if ((float)window.getSquare().getX() < owner.getX()) {
            owner.setDir(IsoDirections.W);
        } else {
            owner.setDir(IsoDirections.E);
        }
        if (Core.tutorial) {
            if (owner.getX() != window.getX() + 0.5f && window.isNorth()) {
                this.slideX(owner, window.getX() + 0.5f);
            }
            if (owner.getY() != window.getY() + 0.5f && !window.isNorth()) {
                this.slideY(owner, window.getY() + 0.5f);
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setIgnoreMovement(false);
        owner.clearVariable("bOpenWindow");
        owner.clearVariable("OpenWindowOutcome");
        owner.clearVariable("StopAfterAnimLooped");
        owner.setHideWeaponModel(false);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (!owner.getVariableBoolean("bOpenWindow")) {
            return;
        }
        IsoWindow window = owner.get(WINDOW);
        if (window == null) {
            owner.setVariable("bOpenWindow", false);
            return;
        }
        if (event.eventName.equalsIgnoreCase("WindowAnimLooped")) {
            if ("start".equalsIgnoreCase(event.parameterValue)) {
                if (!(window.isPermaLocked() || window.isLocked() && owner.getCurrentSquare().has(IsoFlagType.exterior))) {
                    owner.setVariable("OpenWindowOutcome", "success");
                } else {
                    owner.setVariable("OpenWindowOutcome", "struggle");
                }
                return;
            }
            if (event.parameterValue.equalsIgnoreCase(owner.getVariableString("StopAfterAnimLooped"))) {
                owner.setVariable("bOpenWindow", false);
            }
        }
        if (event.eventName.equalsIgnoreCase("WindowOpenAttempt")) {
            this.onAttemptFinished(owner, window);
        } else if (event.eventName.equalsIgnoreCase("WindowOpenSuccess")) {
            this.onSuccess(owner, window);
        } else if (event.eventName.equalsIgnoreCase("WindowStruggleSound") && "struggle".equals(owner.getVariableString("OpenWindowOutcome"))) {
            owner.playSound("WindowIsLocked");
        }
    }

    @Override
    public boolean isDoingActionThatCanBeCancelled() {
        return true;
    }

    private void onAttemptFinished(IsoGameCharacter owner, IsoWindow window) {
        this.exert(owner);
        if (window.isPermaLocked()) {
            if (!owner.getEmitter().isPlaying("WindowIsLocked")) {
                // empty if block
            }
            owner.setVariable("OpenWindowOutcome", "fail");
            owner.setVariable("StopAfterAnimLooped", "fail");
            return;
        }
        int basePermaLockChance = 10;
        if (owner.hasTrait(CharacterTrait.BURGLAR)) {
            basePermaLockChance = 5;
        }
        if (window.isLocked() && owner.getCurrentSquare().has(IsoFlagType.exterior)) {
            if (Rand.Next(100) < basePermaLockChance) {
                owner.getEmitter().playSound("BreakLockOnWindow", window);
                window.setPermaLocked(true);
                window.sync();
                owner.set(WINDOW, null);
                owner.setVariable("OpenWindowOutcome", "fail");
                owner.setVariable("StopAfterAnimLooped", "fail");
                return;
            }
            boolean bSuccess = false;
            if (owner.getPerkLevel(PerkFactory.Perks.Strength) > 7 && Rand.Next(100) < 20) {
                bSuccess = true;
            } else if (owner.getPerkLevel(PerkFactory.Perks.Strength) > 5 && Rand.Next(100) < 10) {
                bSuccess = true;
            } else if (owner.getPerkLevel(PerkFactory.Perks.Strength) > 3 && Rand.Next(100) < 6) {
                bSuccess = true;
            } else if (owner.getPerkLevel(PerkFactory.Perks.Strength) > 1 && Rand.Next(100) < 4) {
                bSuccess = true;
            } else if (Rand.Next(100) <= 1) {
                bSuccess = true;
            }
            if (bSuccess) {
                owner.setVariable("OpenWindowOutcome", "success");
            }
        } else {
            owner.setVariable("OpenWindowOutcome", "success");
        }
    }

    private void onSuccess(IsoGameCharacter owner, IsoWindow window) {
        owner.setVariable("StopAfterAnimLooped", "success");
        ((IsoPlayer)owner).contextPanic = 0.0f;
        if (window.getObjectIndex() != -1 && !window.IsOpen() && ((IsoPlayer)owner).isLocalPlayer()) {
            window.ToggleWindow(owner);
        }
    }

    private void exert(IsoGameCharacter owner) {
        switch (owner.getPerkLevel(PerkFactory.Perks.Fitness)) {
            case 0: {
                owner.exert(0.011f);
                break;
            }
            case 1: {
                owner.exert(0.01f);
                break;
            }
            case 2: {
                owner.exert(0.009f);
                break;
            }
            case 3: {
                owner.exert(0.008f);
                break;
            }
            case 4: {
                owner.exert(0.007f);
                break;
            }
            case 5: {
                owner.exert(0.006f);
                break;
            }
            case 6: {
                owner.exert(0.005f);
                break;
            }
            case 7: {
                owner.exert(0.004f);
                break;
            }
            case 8: {
                owner.exert(0.003f);
                break;
            }
            case 9: {
                owner.exert(0.0025f);
                break;
            }
            case 10: {
                owner.exert(0.002f);
                break;
            }
            default: {
                owner.exert(0.012f);
            }
        }
    }

    private void slideX(IsoGameCharacter owner, float x) {
        float dx = 0.05f * GameTime.getInstance().getThirtyFPSMultiplier();
        dx = x > owner.getX() ? Math.min(dx, x - owner.getX()) : Math.max(-dx, x - owner.getX());
        owner.setX(owner.getX() + dx);
        owner.setNextX(owner.getX());
    }

    private void slideY(IsoGameCharacter owner, float y) {
        float dy = 0.05f * GameTime.getInstance().getThirtyFPSMultiplier();
        dy = y > owner.getY() ? Math.min(dy, y - owner.getY()) : Math.max(-dy, y - owner.getY());
        owner.setY(owner.getY() + dy);
        owner.setNextY(owner.getY());
    }

    public void setParams(IsoGameCharacter owner, IsoWindow window) {
        owner.clear(this);
        owner.set(WINDOW, window);
    }
}

