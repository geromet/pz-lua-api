/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.CharacterTimedActions.BaseAction;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.util.Type;

@UsedFromLua
public final class PlayerHitReactionState
extends State {
    private static final PlayerHitReactionState INSTANCE = new PlayerHitReactionState();

    public static PlayerHitReactionState instance() {
        return INSTANCE;
    }

    private PlayerHitReactionState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setIgnoreMovement(true);
        if (!owner.getCharacterActions().isEmpty()) {
            ((BaseAction)owner.getCharacterActions().get(0)).forceStop();
        }
        owner.setIsAiming(false);
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setIgnoreMovement(false);
        owner.setHitReaction("");
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (owner.getAttackedBy() == null || !(owner.getAttackedBy() instanceof IsoZombie)) {
            DebugLog.log("PlayerHitReactionState.animEvent (" + event.eventName + ") zombie is null");
            return;
        }
        if (event.eventName.equalsIgnoreCase("PushAwayZombie")) {
            owner.getAttackedBy().setHitForce(0.03f);
            ((IsoZombie)owner.getAttackedBy()).setPlayerAttackPosition(null);
            ((IsoZombie)owner.getAttackedBy()).setStaggerBack(true);
        }
        if (event.eventName.equalsIgnoreCase("Defend")) {
            owner.getAttackedBy().setHitReaction("BiteDefended");
            if (GameClient.client) {
                GameClient.sendZombieHit((IsoZombie)owner.getAttackedBy(), player, false, null);
            }
        }
        if (event.eventName.equalsIgnoreCase("DeathSound")) {
            if (owner.isPlayingDeathSound()) {
                return;
            }
            owner.setPlayingDeathSound(true);
            if (player == null) {
                Object sound = "Male";
                if (owner.isFemale()) {
                    sound = "Female";
                }
                sound = (String)sound + "BeingEatenDeath";
                owner.playSound((String)sound);
            } else {
                player.playerVoiceSound("DeathEaten");
            }
        }
        if (event.eventName.equalsIgnoreCase("Death")) {
            owner.setOnFloor(true);
            if (!GameServer.server) {
                owner.Kill(owner.getAttackedBy());
            }
        }
    }
}

