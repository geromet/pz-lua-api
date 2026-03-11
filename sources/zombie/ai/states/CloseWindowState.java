/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.debug.DebugOptions;
import zombie.iso.IsoDirections;
import zombie.iso.objects.IsoWindow;
import zombie.scripting.objects.MoodleType;

@UsedFromLua
public final class CloseWindowState
extends State {
    private static final CloseWindowState INSTANCE = new CloseWindowState();
    public static final State.Param<IsoWindow> ISO_WINDOW = State.Param.of("iso_window", IsoWindow.class);

    public static CloseWindowState instance() {
        return INSTANCE;
    }

    private CloseWindowState() {
        super(true, false, true, true);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setIgnoreMovement(true);
        owner.setHideWeaponModel(true);
        IsoWindow window = owner.get(ISO_WINDOW);
        if (Core.debug && DebugOptions.instance.cheat.window.unlock.getValue()) {
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
        owner.setVariable("bCloseWindow", true);
        owner.clearVariable("BlockWindow");
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        if (!owner.getVariableBoolean("bCloseWindow")) {
            return;
        }
        IsoPlayer player = (IsoPlayer)owner;
        if (player.pressedMovement(false) || player.pressedCancelAction()) {
            owner.setVariable("bCloseWindow", false);
            return;
        }
        IsoWindow isoWindow = owner.get(ISO_WINDOW);
        if (!(isoWindow instanceof IsoWindow)) {
            owner.setVariable("bCloseWindow", false);
            return;
        }
        IsoWindow window = isoWindow;
        if (window == null || window.getObjectIndex() == -1) {
            owner.setVariable("bCloseWindow", false);
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
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.clearVariable("BlockWindow");
        owner.clearVariable("bCloseWindow");
        owner.clearVariable("CloseWindowOutcome");
        owner.clearVariable("StopAfterAnimLooped");
        owner.setIgnoreMovement(false);
        owner.setHideWeaponModel(false);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (!owner.getVariableBoolean("bCloseWindow")) {
            return;
        }
        IsoWindow isoWindow = owner.get(ISO_WINDOW);
        if (!(isoWindow instanceof IsoWindow)) {
            owner.setVariable("bCloseWindow", false);
            return;
        }
        IsoWindow window = isoWindow;
        if (event.eventName.equalsIgnoreCase("WindowAnimLooped")) {
            if ("start".equalsIgnoreCase(event.parameterValue)) {
                int randStruggle = Math.max(5 - owner.getMoodles().getMoodleLevel(MoodleType.PANIC), 1);
                if (window.isPermaLocked() || window.getFirstCharacterClimbingThrough() != null) {
                    owner.setVariable("CloseWindowOutcome", "struggle");
                } else {
                    owner.setVariable("CloseWindowOutcome", "success");
                }
                return;
            }
            if (event.parameterValue.equalsIgnoreCase(owner.getVariableString("StopAfterAnimLooped"))) {
                owner.setVariable("bCloseWindow", false);
            }
        }
        if (event.eventName.equalsIgnoreCase("WindowCloseAttempt")) {
            this.onAttemptFinished(owner, window);
        } else if (event.eventName.equalsIgnoreCase("WindowCloseSuccess")) {
            this.onSuccess(owner, window);
        }
    }

    @Override
    public boolean isDoingActionThatCanBeCancelled() {
        return true;
    }

    private void onAttemptFinished(IsoGameCharacter owner, IsoWindow window) {
        this.exert(owner);
        if (window.isPermaLocked()) {
            owner.getEmitter().playSound("WindowIsLocked", window);
            owner.setVariable("CloseWindowOutcome", "fail");
            owner.setVariable("StopAfterAnimLooped", "fail");
        } else {
            int randStruggle = Math.max(5 - owner.getMoodles().getMoodleLevel(MoodleType.PANIC), 3);
            if (window.isPermaLocked() || window.getFirstCharacterClimbingThrough() != null) {
                owner.setVariable("CloseWindowOutcome", "struggle");
            } else {
                owner.setVariable("CloseWindowOutcome", "success");
            }
        }
    }

    private void onSuccess(IsoGameCharacter owner, IsoWindow window) {
        owner.setVariable("StopAfterAnimLooped", "success");
        ((IsoPlayer)owner).contextPanic = 0.0f;
        if (window.getObjectIndex() != -1 && window.IsOpen() && ((IsoPlayer)owner).isLocalPlayer()) {
            window.ToggleWindow(owner);
        }
    }

    private void exert(IsoGameCharacter owner) {
        float delta = GameTime.getInstance().getThirtyFPSMultiplier();
        switch (owner.getPerkLevel(PerkFactory.Perks.Fitness)) {
            case 1: {
                owner.exert(0.01f * delta);
                break;
            }
            case 2: {
                owner.exert(0.009f * delta);
                break;
            }
            case 3: {
                owner.exert(0.008f * delta);
                break;
            }
            case 4: {
                owner.exert(0.007f * delta);
                break;
            }
            case 5: {
                owner.exert(0.006f * delta);
                break;
            }
            case 6: {
                owner.exert(0.005f * delta);
                break;
            }
            case 7: {
                owner.exert(0.004f * delta);
                break;
            }
            case 8: {
                owner.exert(0.003f * delta);
                break;
            }
            case 9: {
                owner.exert(0.0025f * delta);
                break;
            }
            case 10: {
                owner.exert(0.002f * delta);
            }
        }
    }

    public IsoWindow getWindow(IsoGameCharacter owner) {
        if (!owner.isCurrentState(this)) {
            return null;
        }
        return owner.get(ISO_WINDOW);
    }
}

