/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import zombie.UsedFromLua;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;

@UsedFromLua
public class DebugCSVExportFirearms {
    public static void doCSV() throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter("FirearmsCSV.txt"));){
            writer.println("MODULE&TYPE,DISPLAY_NAME,MIN_DAMAGE,MAX_DAMAGE,MIN_RANGE,MAX_RANGE,MIN_SIGHT_RANGE,MAX_SIGHT_RANGE,WEIGHT,AIMING_PERK_CRIT_MODIFIER,AIMING_PERK_HIT_CHANCE_MODIFIER,AIMING_PERK_MIN_ANGLE_MODIFIER,AIMING_PERK_RANGE_MODIFER,AIMING_TIME,CONDITION_LOWER_CHANCE,CONDITION_MAX,CRIT_DAMAGE_MULTIPLIER,CRITICAL_CHANCE,CYCLIC_RATE_MODIFIER,HIT_CHANCE,JAM_GUN_CHANCE,KNOCK_BACK_ON_NO_DEATH,KNOCKDOWN_MOD,MAX_AMMO,MAX_HIT_COUNT,MINIMUM_SWING_TIME,PIERCING_BULLETS,PROJECTILE_COUNT,PROJECTILE_SPREAD,PROJECTILE_WEIGHT_CENTER,PUSH_BACK_MOD,RECOIL_DELAY,RELOAD_TIME,SOUND_GAIN,SOUND_RADIUS,SOUND_VOLUME,SPLAT_NUMBER,STOP_POWER,SWING_TIME");
            for (Item item : ScriptManager.instance.getAllItems()) {
                if (!item.isAimedFirearm) continue;
                writer.println("%s.%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s".formatted(item.getModuleName(), item.getName(), item.getDisplayName(), Float.valueOf(item.minDamage), Float.valueOf(item.maxDamage), Float.valueOf(item.minRange), Float.valueOf(item.maxRange), Float.valueOf(item.minSightRange), Float.valueOf(item.maxSightRange), Float.valueOf(item.getActualWeight()), item.aimingPerkCritModifier, Float.valueOf(item.aimingPerkHitChanceModifier), Float.valueOf(item.aimingPerkMinAngleModifier), Float.valueOf(item.aimingPerkRangeModifier), item.aimingTime, item.conditionLowerChance, item.conditionMax, Float.valueOf(item.critDmgMultiplier), Float.valueOf(item.criticalChance), Float.valueOf(item.cyclicRateMultiplier), item.hitChance, Float.valueOf(item.jamGunChance), item.knockBackOnNoDeath, Float.valueOf(item.knockdownMod), item.maxAmmo, item.maxHitCount, Float.valueOf(item.minimumSwingTime), item.piercingBullets, item.projectileCount, Float.valueOf(item.projectileSpread), Float.valueOf(item.projectileWeightCenter), Float.valueOf(item.pushBackMod), item.recoilDelay, item.reloadTime, Float.valueOf(item.soundGain), item.soundRadius, item.soundVolume, item.splatNumber, Float.valueOf(item.stopPower), Float.valueOf(item.swingTime)));
            }
        }
    }
}

