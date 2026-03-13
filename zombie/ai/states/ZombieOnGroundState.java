/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.model.Model;
import zombie.iso.Vector3;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.GameClient;
import zombie.network.NetworkVariables;

@UsedFromLua
public final class ZombieOnGroundState
extends State {
    private static final ZombieOnGroundState INSTANCE = new ZombieOnGroundState();
    static Vector3 tempVector = new Vector3();
    static Vector3 tempVectorBonePos = new Vector3();

    public static ZombieOnGroundState instance() {
        return INSTANCE;
    }

    private ZombieOnGroundState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoZombie ownerZombie = (IsoZombie)owner;
        ownerZombie.setCollidable(false);
        if (!ownerZombie.isDead()) {
            ownerZombie.setOnFloor(true);
        }
        if (ownerZombie.isDead() || ownerZombie.isFakeDead()) {
            if (GameClient.client) {
                ownerZombie.networkAi.extraUpdate();
            }
            ownerZombie.die();
            return;
        }
        ownerZombie.setStaggerBack(false);
        ownerZombie.setKnockedDown(false);
        if (ownerZombie.isAlive()) {
            ownerZombie.setHitReaction("");
        }
        ownerZombie.setEatBodyTarget(null, false);
        ownerZombie.setSitAgainstWall(false);
        if (ownerZombie.isBecomeCrawler()) {
            return;
        }
        if (!"Tutorial".equals(Core.gameMode)) {
            ZombieOnGroundState.startReanimateTimer(ownerZombie);
        }
        if (GameClient.client && ownerZombie.isReanimatedPlayer()) {
            IsoDeadBody.removeDeadBody(ownerZombie.networkAi.reanimatedBodyId);
        }
        ownerZombie.parameterZombieState.setState(ParameterZombieState.State.Idle);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        if (zombie.isDead() || zombie.isFakeDead()) {
            if (zombie.isRagdollSimulationActive()) {
                return;
            }
            zombie.die();
            return;
        }
        if (zombie.isReanimatedForGrappleOnly() && !zombie.isBeingGrappled()) {
            if (GameClient.client) {
                zombie.onZombieGrappleEnded();
            } else {
                zombie.die();
            }
            return;
        }
        if (zombie.isBecomeCrawler()) {
            if (zombie.isBeingSteppedOn() || zombie.isUnderVehicle()) {
                return;
            }
            zombie.setCrawler(true);
            zombie.setCanWalk(false);
            zombie.setReanimate(true);
            zombie.setBecomeCrawler(false);
            return;
        }
        if (zombie.hasAnimationPlayer()) {
            zombie.getAnimationPlayer().setTargetToAngle();
        }
        zombie.setReanimateTimer(zombie.getReanimateTimer() - GameTime.getInstance().getThirtyFPSMultiplier());
        if (zombie.getReanimateTimer() <= 2.0f) {
            if (zombie.isBeingSteppedOn() && zombie.getReanimatedPlayer() == null) {
                ZombieOnGroundState.startReanimateTimer(zombie);
            }
            if (!zombie.isLocal() && NetworkVariables.ZombieState.FallDown != zombie.realState && NetworkVariables.ZombieState.OnGround != zombie.realState && NetworkVariables.ZombieState.HitReaction != zombie.realState && NetworkVariables.ZombieState.StaggerBack != zombie.realState) {
                zombie.setReanimateTimer(0.0f);
            }
        }
        if (GameClient.client && zombie.isReanimatedPlayer()) {
            IsoDeadBody.removeDeadBody(zombie.networkAi.reanimatedBodyId);
        }
    }

    public static boolean isCharacterStandingOnOther(IsoGameCharacter chrStanding, IsoGameCharacter chrProne) {
        AnimationPlayer animPlayer = chrProne.getAnimationPlayer();
        int bone = ZombieOnGroundState.DoCollisionBoneCheck(chrStanding, chrProne, animPlayer.getSkinningBoneIndex("Bip01_Spine", -1), 0.32f);
        if (bone == -1) {
            bone = ZombieOnGroundState.DoCollisionBoneCheck(chrStanding, chrProne, animPlayer.getSkinningBoneIndex("Bip01_L_Calf", -1), 0.18f);
        }
        if (bone == -1) {
            bone = ZombieOnGroundState.DoCollisionBoneCheck(chrStanding, chrProne, animPlayer.getSkinningBoneIndex("Bip01_R_Calf", -1), 0.18f);
        }
        if (bone == -1) {
            bone = ZombieOnGroundState.DoCollisionBoneCheck(chrStanding, chrProne, animPlayer.getSkinningBoneIndex("Bip01_Head", -1), 0.28f);
        }
        return bone > -1;
    }

    private static int DoCollisionBoneCheck(IsoGameCharacter chrStanding, IsoGameCharacter chrProne, int bone, float tempoLengthTest) {
        float weaponLength = 0.3f;
        Model.boneToWorldCoords(chrProne, bone, tempVectorBonePos);
        for (int x = 1; x <= 10; ++x) {
            boolean hit;
            float delta = (float)x / 10.0f;
            ZombieOnGroundState.tempVector.x = chrStanding.getX();
            ZombieOnGroundState.tempVector.y = chrStanding.getY();
            ZombieOnGroundState.tempVector.z = chrStanding.getZ();
            ZombieOnGroundState.tempVector.x += chrStanding.getForwardDirectionX() * 0.3f * delta;
            ZombieOnGroundState.tempVector.y += chrStanding.getForwardDirectionY() * 0.3f * delta;
            ZombieOnGroundState.tempVector.x = ZombieOnGroundState.tempVectorBonePos.x - ZombieOnGroundState.tempVector.x;
            ZombieOnGroundState.tempVector.y = ZombieOnGroundState.tempVectorBonePos.y - ZombieOnGroundState.tempVector.y;
            ZombieOnGroundState.tempVector.z = 0.0f;
            boolean bl = hit = tempVector.getLength() < tempoLengthTest;
            if (!hit) continue;
            return bone;
        }
        return -1;
    }

    public static void startReanimateTimer(IsoZombie ownerZombie) {
        ownerZombie.setReanimateTimer(Rand.Next(60) + 30);
    }

    @Override
    public void exit(IsoGameCharacter owner) {
    }
}

