/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.core.math.PZMath;
import zombie.core.properties.IsoObjectChange;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.iso.LosUtil;
import zombie.iso.Vector2;
import zombie.network.GameServer;
import zombie.util.StringUtils;

@UsedFromLua
public final class AttackState
extends State {
    private static final AttackState INSTANCE = new AttackState();
    public static final State.Param<Boolean> SKIP_TEST_DEFENCE = State.Param.ofBool("skip_test_defence", false);
    private static final String frontStr = "FRONT";
    private static final String backStr = "BEHIND";
    private static final String rightStr = "LEFT";
    private static final String leftStr = "RIGHT";

    public static AttackState instance() {
        return INSTANCE;
    }

    private AttackState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        owner.clear(this);
        owner.set(SKIP_TEST_DEFENCE, false);
        zombie.setAttackOutcome("start");
        owner.clearVariable("AttackDidDamage");
        owner.clearVariable("ZombieBiteDone");
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        boolean slowFactor;
        IsoZombie zomb = (IsoZombie)owner;
        IsoGameCharacter targetChar = (IsoGameCharacter)zomb.target;
        if (targetChar != null && "Chainsaw".equals(targetChar.getVariableString("ZombieHitReaction"))) {
            return;
        }
        String outcome = zomb.getAttackOutcome();
        if ("success".equals(outcome) && owner.getVariableBoolean("bAttack") && owner.isVariable("targethitreaction", "EndDeath")) {
            outcome = "enddeath";
            zomb.setAttackOutcome(outcome);
        }
        if ("success".equals(outcome) && !owner.getVariableBoolean("bAttack") && !owner.getVariableBoolean("AttackDidDamage") && owner.getVariableString("ZombieBiteDone") == null) {
            zomb.setAttackOutcome("interrupted");
        }
        if (targetChar == null || targetChar.isDead()) {
            zomb.setTargetSeenTime(10.0f);
        }
        if (!(targetChar == null || owner.get(SKIP_TEST_DEFENCE).booleanValue() || "started".equals(outcome) || StringUtils.isNullOrEmpty(owner.getVariableString("PlayerHitReaction")))) {
            owner.set(SKIP_TEST_DEFENCE, true);
            targetChar.testDefense(zomb);
        }
        zomb.setShootable(true);
        if (zomb.target != null && !zomb.crawling) {
            if (!"fail".equals(outcome) && !"interrupted".equals(outcome)) {
                zomb.faceThisObject(zomb.target);
            }
            zomb.setOnFloor(false);
        }
        boolean bl = slowFactor = zomb.speedType == 1;
        if (zomb.target != null && slowFactor && ("start".equals(outcome) || "success".equals(outcome))) {
            IsoGameCharacter chr = (IsoGameCharacter)zomb.target;
            float oldSlowFactor = chr.getSlowFactor();
            if (chr.getSlowFactor() <= 0.0f) {
                chr.setSlowTimer(30.0f);
            }
            chr.setSlowTimer(chr.getSlowTimer() + GameTime.instance.getMultiplier());
            if (chr.getSlowTimer() > 60.0f) {
                chr.setSlowTimer(60.0f);
            }
            chr.setSlowFactor(chr.getSlowFactor() + 0.03f);
            if (chr.getSlowFactor() >= 0.5f) {
                chr.setSlowFactor(0.5f);
            }
            if (GameServer.server && oldSlowFactor != chr.getSlowFactor()) {
                GameServer.sendSlowFactor(chr);
            }
        }
        if (zomb.target != null) {
            zomb.target.setTimeSinceZombieAttack(0);
            zomb.target.setLastTargettedBy(zomb);
        }
        if (!zomb.crawling) {
            zomb.setVariable("AttackType", "bite");
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        owner.clearVariable("AttackOutcome");
        owner.clearVariable("AttackType");
        owner.clearVariable("PlayerHitReaction");
        owner.setStateMachineLocked(false);
        if (zombie.target != null && zombie.target.isOnFloor()) {
            zombie.setEatBodyTarget(zombie.target, true);
            zombie.setTarget(null);
        }
        zombie.allowRepathDelay = 0.0f;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoZombie zombie = (IsoZombie)owner;
        if (event.eventName.equalsIgnoreCase("SetAttackOutcome")) {
            if (zombie.getVariableBoolean("bAttack")) {
                zombie.setAttackOutcome("success");
            } else {
                zombie.setAttackOutcome("fail");
            }
        }
        if (event.eventName.equalsIgnoreCase("AttackCollisionCheck") && !zombie.isNoTeeth()) {
            int knifeLvl;
            int chance;
            IsoGameCharacter targetChar = (IsoGameCharacter)zombie.target;
            if (targetChar == null) {
                return;
            }
            targetChar.setHitFromBehind(zombie.isBehind(targetChar));
            String dotSide = targetChar.testDotSide(zombie);
            boolean isFront = dotSide.equals(frontStr);
            if (isFront && !targetChar.isAimAtFloor() && !StringUtils.isNullOrEmpty(targetChar.getVariableString("AttackType"))) {
                return;
            }
            if ("KnifeDeath".equals(targetChar.getVariableString("ZombieHitReaction")) && Rand.NextBool(chance = Math.max(0, 9 - (knifeLvl = targetChar.getPerkLevel(PerkFactory.Perks.SmallBlade) + 1) * 2))) {
                return;
            }
            this.triggerPlayerReaction(owner.getVariableString("PlayerHitReaction"), owner);
            Vector2 hitDir = zombie.getHitDir();
            hitDir.x = zombie.getX();
            hitDir.y = zombie.getY();
            hitDir.x -= targetChar.getX();
            hitDir.y -= targetChar.getY();
            hitDir.normalize();
        }
        if (event.eventName.equalsIgnoreCase("EatBody")) {
            owner.setVariable("EatingStarted", true);
            ((IsoZombie)owner).setEatBodyTarget(((IsoZombie)owner).target, true);
            ((IsoZombie)owner).setTarget(null);
        }
        if (event.eventName.equalsIgnoreCase("SetState")) {
            zombie.parameterZombieState.setState(ParameterZombieState.State.Attack);
        }
    }

    @Override
    public boolean isAttacking(IsoGameCharacter owner) {
        return true;
    }

    private void triggerPlayerReaction(String hitReaction, IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        IsoGameCharacter targetChar = (IsoGameCharacter)zombie.target;
        if (targetChar == null) {
            return;
        }
        if (targetChar instanceof IsoAnimal) {
            zombie.target = null;
            return;
        }
        if (zombie.DistTo(targetChar) > 1.0f && !zombie.crawling) {
            return;
        }
        if ((zombie.isFakeDead() || zombie.crawling) && zombie.DistTo(targetChar) > 1.3f) {
            return;
        }
        if (targetChar.isDead() && !targetChar.getHitReaction().equals("EndDeath") || targetChar.isOnFloor()) {
            zombie.setEatBodyTarget(targetChar, true);
            return;
        }
        if (targetChar.isDead()) {
            return;
        }
        targetChar.setHitFromBehind(zombie.isBehind(targetChar));
        String dotSide = targetChar.testDotSide(zombie);
        boolean isFront = dotSide.equals(frontStr);
        boolean isBack = dotSide.equals(backStr);
        if (dotSide.equals(leftStr)) {
            hitReaction = (String)hitReaction + rightStr;
        }
        if (dotSide.equals(rightStr)) {
            hitReaction = (String)hitReaction + leftStr;
        }
        if (((IsoPlayer)targetChar).isDoShove() && isFront && !targetChar.isAimAtFloor()) {
            return;
        }
        if (((IsoPlayer)targetChar).isDoShove() && !isFront && !isBack && Rand.Next(100) > 75) {
            return;
        }
        if (Math.abs(zombie.getZ() - targetChar.getZ()) >= 0.2f) {
            return;
        }
        LosUtil.TestResults testResults = LosUtil.lineClear(zombie.getCell(), PZMath.fastfloor(zombie.getX()), PZMath.fastfloor(zombie.getY()), PZMath.fastfloor(zombie.getZ()), PZMath.fastfloor(targetChar.getX()), PZMath.fastfloor(targetChar.getY()), PZMath.fastfloor(targetChar.getZ()), false);
        if (testResults == LosUtil.TestResults.Blocked || testResults == LosUtil.TestResults.ClearThroughClosedDoor) {
            return;
        }
        if (targetChar.getSquare().isSomethingTo(zombie.getCurrentSquare())) {
            return;
        }
        targetChar.setAttackedBy(zombie);
        boolean bDamaged = targetChar.getBodyDamage().AddRandomDamageFromZombie(zombie, (String)hitReaction);
        owner.setVariable("AttackDidDamage", bDamaged);
        targetChar.getBodyDamage().Update();
        if (targetChar.isDead()) {
            targetChar.setHealth(0.0f);
            zombie.setEatBodyTarget(targetChar, true);
            zombie.setTarget(null);
        } else if (targetChar.isAsleep()) {
            if (GameServer.server) {
                targetChar.sendObjectChange(IsoObjectChange.WAKE_UP);
            } else {
                targetChar.forceAwake();
            }
        }
    }
}

