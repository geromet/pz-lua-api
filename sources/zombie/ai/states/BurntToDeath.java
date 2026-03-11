/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.iso.IsoDirections;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.ui.TutorialManager;

@UsedFromLua
public final class BurntToDeath
extends State {
    private static final BurntToDeath INSTANCE = new BurntToDeath();

    public static BurntToDeath instance() {
        return INSTANCE;
    }

    private BurntToDeath() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        if (owner instanceof IsoSurvivor) {
            owner.getDescriptor().setDead(true);
        }
        if (!(owner instanceof IsoZombie)) {
            owner.PlayAnimUnlooped("Die");
        } else {
            owner.PlayAnimUnlooped("ZombieDeath");
        }
        owner.def.animFrameIncrease = 0.25f;
        owner.setStateMachineLocked(true);
        String t = owner.getDescriptor().getVoicePrefix() + "Death";
        owner.getEmitter().playVocals(t);
        if (GameServer.server && owner instanceof IsoZombie) {
            IsoZombie isoZombie = (IsoZombie)owner;
            GameServer.sendZombieSound(IsoZombie.ZombieSound.Burned, isoZombie);
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        if ((int)owner.def.frame == owner.sprite.currentAnim.frames.size() - 1) {
            if (owner == TutorialManager.instance.wife) {
                owner.dir = IsoDirections.S;
            }
            owner.RemoveAttachedAnims();
            if (!GameClient.client) {
                IsoDeadBody isoDeadBody = new IsoDeadBody(owner);
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
    }
}

