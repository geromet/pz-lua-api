/*
 * Decompiled with CFR 0.152.
 */
package zombie.combat;

import zombie.UsedFromLua;
import zombie.combat.CombatConfigCategory;

@UsedFromLua
public enum CombatConfigKey {
    BASE_WEAPON_DAMAGE_MULTIPLIER(CombatConfigCategory.GENERAL, 0.3f, 0.0f, 1.0f),
    WEAPON_LEVEL_DAMAGE_MULTIPLIER_INCREMENT(CombatConfigCategory.GENERAL, 0.1f, 0.0f, 1.0f),
    PLAYER_RECEIVED_DAMAGE_MULTIPLIER(CombatConfigCategory.GENERAL, 0.4f, 0.0f, 1.0f),
    NON_PLAYER_RECEIVED_DAMAGE_MULTIPLIER(CombatConfigCategory.GENERAL, 1.5f, 0.0f, 10.0f),
    HEAD_HIT_DAMAGE_SPLIT_MODIFIER(CombatConfigCategory.GENERAL, 3.0f, 0.0f, 10.0f),
    LEG_HIT_DAMAGE_SPLIT_MODIFIER(CombatConfigCategory.GENERAL, 0.05f, 0.0f, 1.0f),
    ADDITIONAL_CRITICAL_HIT_CHANCE_FROM_BEHIND(CombatConfigCategory.GENERAL, 30.0f, 0.0f, 100.0f),
    ADDITIONAL_CRITICAL_HIT_CHANCE_DEFAULT(CombatConfigCategory.GENERAL, 5.0f, 0.0f, 100.0f),
    RECOIL_DELAY(CombatConfigCategory.FIREARM, 10.0f, 0.0f, 100.0f),
    POINT_BLANK_DISTANCE(CombatConfigCategory.FIREARM, 3.5f, 0.0f, 10.0f),
    LOW_LIGHT_THRESHOLD(CombatConfigCategory.FIREARM, 0.75f, 0.0f, 1.0f),
    LOW_LIGHT_TO_HIT_MAXIMUM_PENALTY(CombatConfigCategory.FIREARM, 50.0f, 0.0f, 100.0f),
    POINT_BLANK_TO_HIT_MAXIMUM_BONUS(CombatConfigCategory.FIREARM, 40.0f, 0.0f, 100.0f),
    POINT_BLANK_DROP_OFF_TO_HIT_PENALTY(CombatConfigCategory.FIREARM, 0.7f, 0.0f, 1.0f),
    POST_SHOT_AIMING_DELAY_RECOIL_MODIFIER(CombatConfigCategory.FIREARM, 0.25f, 0.0f, 1.0f),
    POST_SHOT_AIMING_DELAY_AIMING_MODIFIER(CombatConfigCategory.FIREARM, 0.05f, 0.0f, 1.0f),
    OPTIMAL_RANGE_TO_HIT_MAXIMUM_BONUS(CombatConfigCategory.FIREARM, 15.0f, 0.0f, 100.0f),
    OPTIMAL_RANGE_DROP_OFF_TO_HIT_PENALTY(CombatConfigCategory.FIREARM, 4.0f, 0.0f, 10.0f),
    OPTIMAL_RANGE_DROP_OFF_TO_HIT_PENALTY_INCREMENT(CombatConfigCategory.FIREARM, 0.3f, 0.0f, 1.0f),
    MINIMUM_TO_HIT_CHANCE(CombatConfigCategory.FIREARM, 5.0f, 0.0f, 100.0f),
    MAXIMUM_START_TO_HIT_CHANCE(CombatConfigCategory.FIREARM, 95.0f, 0.0f, 100.0f),
    MAXIMUM_TO_HIT_CHANCE(CombatConfigCategory.FIREARM, 100.0f, 0.0f, 100.0f),
    MOVING_TO_HIT_PENALTY(CombatConfigCategory.FIREARM, 5.0f, 0.0f, 100.0f),
    RUNNING_TO_HIT_PENALTY(CombatConfigCategory.FIREARM, 15.0f, 0.0f, 100.0f),
    SPRINTING_TO_HIT_PENALTY(CombatConfigCategory.FIREARM, 25.0f, 0.0f, 100.0f),
    MARKSMAN_TRAIT_TO_HIT_BONUS(CombatConfigCategory.FIREARM, 20.0f, 0.0f, 100.0f),
    ARM_PAIN_TO_HIT_MODIFIER(CombatConfigCategory.FIREARM, 0.1f, 0.0f, 1.0f),
    PANIC_TO_HIT_BASE_PENALTY(CombatConfigCategory.FIREARM, 4.0f, 0.0f, 10.0f),
    PANIC_TO_HIT_DISTANCE_MODIFIER(CombatConfigCategory.FIREARM, 0.5f, 0.0f, 1.0f),
    STRESS_TO_HIT_BASE_PENALTY(CombatConfigCategory.FIREARM, 4.0f, 0.0f, 10.0f),
    STRESS_TO_HIT_DISTANCE_MODIFIER(CombatConfigCategory.FIREARM, 0.5f, 0.0f, 1.0f),
    TIRED_TO_HIT_BASE_PENALTY(CombatConfigCategory.FIREARM, 2.5f, 0.0f, 10.0f),
    ENDURANCE_TO_HIT_BASE_PENALTY(CombatConfigCategory.FIREARM, 2.5f, 0.0f, 10.0f),
    DRUNK_TO_HIT_BASE_PENALTY(CombatConfigCategory.FIREARM, 4.0f, 0.0f, 10.0f),
    DRUNK_TO_HIT_DISTANCE_MODIFIER(CombatConfigCategory.FIREARM, 0.5f, 0.0f, 1.0f),
    WIND_INTENSITY_TO_HIT_PENALTY(CombatConfigCategory.FIREARM, 6.0f, 0.0f, 10.0f),
    WIND_INTENSITY_TO_HIT_AIMING_MODIFIER(CombatConfigCategory.FIREARM, 0.2f, 0.0f, 1.0f),
    WIND_INTENSITY_TO_HIT_MINIMUM_MARKSMAN_MODIFIER(CombatConfigCategory.FIREARM, 0.6f, 0.0f, 1.0f),
    WIND_INTENSITY_TO_HIT_MAXIMUM_MARKSMAN_MODIFIER(CombatConfigCategory.FIREARM, 1.0f, 0.0f, 10.0f),
    RAIN_INTENSITY_TO_HIT_DISTANCE_MODIFIER(CombatConfigCategory.FIREARM, 0.5f, 0.0f, 1.0f),
    FOG_INTENSITY_DISTANCE_MODIFIER(CombatConfigCategory.FIREARM, 10.0f, 0.0f, 100.0f),
    POINT_BLANK_MAXIMUM_DISTANCE_MODIFIER(CombatConfigCategory.FIREARM, 1.0f, 0.0f, 10.0f),
    SIGHTLESS_TO_HIT_BASE_DISTANCE(CombatConfigCategory.FIREARM, 15.0f, 0.0f, 100.0f),
    SIGHTLESS_TO_HIT_PRONE_MODIFIER(CombatConfigCategory.FIREARM, 2.0f, 0.0f, 10.0f),
    SIGHTLESS_AIM_DELAY_TO_HIT_DISTANCE_MODIFIER(CombatConfigCategory.FIREARM, 0.1f, 0.0f, 1.0f),
    PIERCING_BULLET_DAMAGE_REDUCTION(CombatConfigCategory.FIREARM, 5.0f, 0.0f, 100.0f),
    FIREARM_RECOIL_MUSCLE_STRAIN_MODIFIER(CombatConfigCategory.FIREARM, 0.05f, 0.0f, 1.0f),
    DRIVEBY_DOT_OPTIMAL_ANGLE(CombatConfigCategory.FIREARM, -0.7f, -1.0f, 1.0f),
    DRIVEBY_DOT_MAXIMUM_ANGLE(CombatConfigCategory.FIREARM, -0.1f, -1.0f, 1.0f),
    DRIVEBY_DOT_TO_HIT_MAXIMUM_PENALTY(CombatConfigCategory.FIREARM, 40.0f, 0.0f, 100.0f),
    GLOBAL_MELEE_DAMAGE_REDUCTION_MULTIPLIER(CombatConfigCategory.MELEE, 0.15f, 0.0f, 1.0f),
    DAMAGE_PENALTY_ONE_HANDED_TWO_HANDED_WEAPON_MULTIPLIER(CombatConfigCategory.MELEE, 0.5f, 0.0f, 1.0f),
    ENDURANCE_LOSS_TWO_HANDED_PENALTY_DIVISOR(CombatConfigCategory.MELEE, 1.5f, 0.0f, 10.0f),
    ENDURANCE_LOSS_TWO_HANDED_PENALTY_SCALE(CombatConfigCategory.MELEE, 10.0f, 0.0f, 100.0f),
    ENDURANCE_LOSS_FLOOR_SHOVE_MULTIPLIER(CombatConfigCategory.MELEE, 2.0f, 0.0f, 10.0f),
    ENDURANCE_LOSS_CLOSE_KILL_MODIFIER(CombatConfigCategory.MELEE, 0.2f, 0.0f, 1.0f),
    ENDURANCE_LOSS_BASE_SCALE(CombatConfigCategory.MELEE, 0.28f, 0.0f, 1.0f),
    ENDURANCE_LOSS_WEIGHT_MODIFIER(CombatConfigCategory.MELEE, 0.3f, 0.0f, 1.0f),
    ENDURANCE_LOSS_FINAL_MULTIPLIER(CombatConfigCategory.MELEE, 0.04f, 0.0f, 1.0f),
    BALLISTICS_CONTROLLER_DISTANCE_THRESHOLD(CombatConfigCategory.BALLISTICS, 2.75f, 0.0f, 10.0f);

    private final CombatConfigCategory category;
    private final float defaultValue;
    private final float minimum;
    private final float maximum;

    private CombatConfigKey(CombatConfigCategory category, float defaultValue, float minimum, float maximum) {
        this.category = category;
        this.defaultValue = defaultValue;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public CombatConfigCategory getCategory() {
        return this.category;
    }

    public float getDefaultValue() {
        return this.defaultValue;
    }

    public float getMinimum() {
        return this.minimum;
    }

    public float getMaximum() {
        return this.maximum;
    }
}

