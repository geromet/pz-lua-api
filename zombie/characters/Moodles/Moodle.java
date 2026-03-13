/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.Moodles;

import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.BodyDamage.Thermoregulator;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.Moodles.MoodleStat;
import zombie.core.Color;
import zombie.iso.weather.Temperature;
import zombie.scripting.objects.MoodleType;
import zombie.ui.MoodlesUI;

@UsedFromLua
public final class Moodle {
    private static final int MinChevrons = 0;
    private static final int MaxChevrons = 3;
    private static final int CantSprintTimerDuration = 300;
    private static final int PainTimerDuration = 120;
    private final MoodleType moodleType;
    private int moodleLevel = MoodleLevel.MinMoodleLevel.ordinal();
    private final IsoGameCharacter isoGameCharacter;
    private int painTimer;
    private Color chevronColor = Color.white;
    private boolean chevronIsUp = true;
    private int chevronCount = 0;
    private static final Color colorNeg = new Color(0.88235295f, 0.15686275f, 0.15686275f);
    private static final Color colorPos = new Color(0.15686275f, 0.88235295f, 0.15686275f);
    private int cantSprintTimer = 300;

    public Moodle(MoodleType moodleType, IsoGameCharacter isoGameCharacter) {
        this.isoGameCharacter = isoGameCharacter;
        this.moodleType = moodleType;
    }

    public MoodleType getMoodleType() {
        return this.moodleType;
    }

    public int getChevronCount() {
        return this.chevronCount;
    }

    public boolean isChevronIsUp() {
        return this.chevronIsUp;
    }

    public Color getChevronColor() {
        return this.chevronColor;
    }

    public boolean chevronDifference(int count, boolean isUp, Color col) {
        return count != this.chevronCount || isUp != this.chevronIsUp || col != this.chevronColor;
    }

    public void setChevron(int count, boolean isUp, Color col) {
        if (count < 0) {
            count = 0;
        }
        if (count > 3) {
            count = 3;
        }
        this.chevronCount = count;
        this.chevronIsUp = isUp;
        this.chevronColor = col != null ? col : Color.white;
    }

    public int getLevel() {
        return this.moodleLevel;
    }

    private boolean updateMoodleLevel(int moodleLevel) {
        if (moodleLevel != this.moodleLevel) {
            this.moodleLevel = Math.max(MoodleLevel.MinMoodleLevel.ordinal(), Math.min(moodleLevel, MoodleLevel.MaxMoodleLevel.ordinal()));
            return true;
        }
        return false;
    }

    public boolean Update() {
        IsoGameCharacter isoGameCharacter;
        int moodleLevelToSet = MoodleLevel.MinMoodleLevel.ordinal();
        if (this.isoGameCharacter.isDead() && this.moodleType != MoodleType.DEAD && this.moodleType != MoodleType.ZOMBIE) {
            return this.updateMoodleLevel(moodleLevelToSet);
        }
        if (this.moodleType == MoodleType.CANT_SPRINT && ((IsoPlayer)this.isoGameCharacter).moodleCantSprint) {
            moodleLevelToSet = MoodleLevel.LowMoodleLevel.ordinal();
            --this.cantSprintTimer;
            MoodlesUI.getInstance().wiggle(MoodleType.CANT_SPRINT);
            if (this.cantSprintTimer == 0) {
                moodleLevelToSet = MoodleLevel.MinMoodleLevel.ordinal();
                this.cantSprintTimer = 300;
                ((IsoPlayer)this.isoGameCharacter).moodleCantSprint = false;
            }
        }
        if (this.moodleType == MoodleType.ENDURANCE && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0f) {
            float endurance = this.isoGameCharacter.getStats().get(CharacterStat.ENDURANCE);
            moodleLevelToSet = endurance > MoodleStat.ENDURANCE.getMinimumThreshold() ? MoodleLevel.MinMoodleLevel.ordinal() : (endurance > MoodleStat.ENDURANCE.getLowestThreshold() ? MoodleLevel.LowMoodleLevel.ordinal() : (endurance > MoodleStat.ENDURANCE.getModerateThreshold() ? MoodleLevel.ModerateMoodleLevel.ordinal() : (endurance > MoodleStat.ENDURANCE.getHighestThreshold() ? MoodleLevel.HighMoodleLevel.ordinal() : MoodleLevel.MaxMoodleLevel.ordinal())));
        }
        if (this.moodleType == MoodleType.ANGRY) {
            float anger = this.isoGameCharacter.getStats().get(CharacterStat.ANGER);
            if (anger > MoodleStat.ANGRY.getMaximumThreshold()) {
                moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
            } else if (anger > MoodleStat.ANGRY.getHighestThreshold()) {
                moodleLevelToSet = MoodleLevel.HighMoodleLevel.ordinal();
            } else if (anger > MoodleStat.ANGRY.getModerateThreshold()) {
                moodleLevelToSet = MoodleLevel.ModerateMoodleLevel.ordinal();
            } else if (anger > MoodleStat.ANGRY.getLowestThreshold()) {
                moodleLevelToSet = MoodleLevel.LowMoodleLevel.ordinal();
            }
        }
        if (this.moodleType == MoodleType.TIRED && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0f) {
            float fatigue = this.isoGameCharacter.getStats().get(CharacterStat.FATIGUE);
            if (fatigue > MoodleStat.TIRED.getLowestThreshold()) {
                moodleLevelToSet = MoodleLevel.LowMoodleLevel.ordinal();
            }
            if (fatigue > MoodleStat.TIRED.getModerateThreshold()) {
                moodleLevelToSet = MoodleLevel.ModerateMoodleLevel.ordinal();
            }
            if (fatigue > MoodleStat.TIRED.getHighestThreshold()) {
                moodleLevelToSet = MoodleLevel.HighMoodleLevel.ordinal();
            }
            if (fatigue > MoodleStat.TIRED.getMaximumThreshold()) {
                moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
            }
        }
        if (this.moodleType == MoodleType.HUNGRY && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0f) {
            float hunger = this.isoGameCharacter.getStats().get(CharacterStat.HUNGER);
            if (hunger > MoodleStat.HUNGRY.getLowestThreshold()) {
                moodleLevelToSet = MoodleLevel.LowMoodleLevel.ordinal();
            }
            if (hunger > MoodleStat.HUNGRY.getModerateThreshold()) {
                moodleLevelToSet = MoodleLevel.ModerateMoodleLevel.ordinal();
            }
            if (hunger > MoodleStat.HUNGRY.getHighestThreshold()) {
                moodleLevelToSet = MoodleLevel.HighMoodleLevel.ordinal();
            }
            if (hunger > MoodleStat.HUNGRY.getMaximumThreshold()) {
                moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
            }
        }
        if (this.moodleType == MoodleType.PANIC && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0f) {
            float panic = this.isoGameCharacter.getStats().get(CharacterStat.PANIC);
            if (panic > MoodleStat.PANIC.getLowestThreshold()) {
                moodleLevelToSet = MoodleLevel.LowMoodleLevel.ordinal();
            }
            if (panic > MoodleStat.PANIC.getModerateThreshold()) {
                moodleLevelToSet = MoodleLevel.ModerateMoodleLevel.ordinal();
            }
            if (panic > MoodleStat.PANIC.getHighestThreshold()) {
                moodleLevelToSet = MoodleLevel.HighMoodleLevel.ordinal();
            }
            if (panic > MoodleStat.PANIC.getMaximumThreshold()) {
                moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
            }
        }
        if (this.moodleType == MoodleType.SICK && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0f) {
            float sickness = this.isoGameCharacter.getBodyDamage().getApparentInfectionLevel() / 100.0f + this.isoGameCharacter.getStats().get(CharacterStat.SICKNESS);
            if (sickness > MoodleStat.SICK.getLowestThreshold()) {
                moodleLevelToSet = MoodleLevel.LowMoodleLevel.ordinal();
            }
            if (sickness > MoodleStat.SICK.getModerateThreshold()) {
                moodleLevelToSet = MoodleLevel.ModerateMoodleLevel.ordinal();
            }
            if (sickness > MoodleStat.SICK.getHighestThreshold()) {
                moodleLevelToSet = MoodleLevel.HighMoodleLevel.ordinal();
            }
            if (sickness > MoodleStat.SICK.getMaximumThreshold()) {
                moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
            }
        }
        if (this.moodleType == MoodleType.BORED && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0f) {
            float boredom = this.isoGameCharacter.getStats().get(CharacterStat.BOREDOM);
            if (boredom > MoodleStat.BORED.getLowestThreshold()) {
                moodleLevelToSet = MoodleLevel.LowMoodleLevel.ordinal();
            }
            if (boredom > MoodleStat.BORED.getModerateThreshold()) {
                moodleLevelToSet = MoodleLevel.ModerateMoodleLevel.ordinal();
            }
            if (boredom > MoodleStat.BORED.getHighestThreshold()) {
                moodleLevelToSet = MoodleLevel.HighMoodleLevel.ordinal();
            }
            if (boredom > MoodleStat.BORED.getMaximumThreshold()) {
                moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
            }
        }
        if (this.moodleType == MoodleType.UNHAPPY && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0f) {
            float unhappinessLevel = this.isoGameCharacter.getStats().get(CharacterStat.UNHAPPINESS);
            if (unhappinessLevel > MoodleStat.UNHAPPY.getLowestThreshold()) {
                moodleLevelToSet = MoodleLevel.LowMoodleLevel.ordinal();
            }
            if (unhappinessLevel > MoodleStat.UNHAPPY.getModerateThreshold()) {
                moodleLevelToSet = MoodleLevel.ModerateMoodleLevel.ordinal();
            }
            if (unhappinessLevel > MoodleStat.UNHAPPY.getHighestThreshold()) {
                moodleLevelToSet = MoodleLevel.HighMoodleLevel.ordinal();
            }
            if (unhappinessLevel > MoodleStat.UNHAPPY.getMaximumThreshold()) {
                moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
            }
        }
        if (this.moodleType == MoodleType.STRESS) {
            float effectiveStress = this.isoGameCharacter.getStats().getNicotineStress();
            if (effectiveStress > MoodleStat.STRESS.getMaximumThreshold()) {
                moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
            } else if (effectiveStress > MoodleStat.STRESS.getHighestThreshold()) {
                moodleLevelToSet = MoodleLevel.HighMoodleLevel.ordinal();
            } else if (effectiveStress > MoodleStat.STRESS.getModerateThreshold()) {
                moodleLevelToSet = MoodleLevel.ModerateMoodleLevel.ordinal();
            } else if (effectiveStress > MoodleStat.STRESS.getLowestThreshold()) {
                moodleLevelToSet = MoodleLevel.LowMoodleLevel.ordinal();
            }
        }
        if (this.moodleType == MoodleType.THIRST) {
            if (this.isoGameCharacter.getStats().get(CharacterStat.THIRST) > MoodleStat.THIRST.getLowestThreshold()) {
                moodleLevelToSet = MoodleLevel.LowMoodleLevel.ordinal();
            }
            if (this.isoGameCharacter.getStats().get(CharacterStat.THIRST) > MoodleStat.THIRST.getModerateThreshold()) {
                moodleLevelToSet = MoodleLevel.ModerateMoodleLevel.ordinal();
            }
            if (this.isoGameCharacter.getStats().get(CharacterStat.THIRST) > MoodleStat.THIRST.getHighestThreshold()) {
                moodleLevelToSet = MoodleLevel.HighMoodleLevel.ordinal();
            }
            if (this.isoGameCharacter.getStats().get(CharacterStat.THIRST) > MoodleStat.THIRST.getMaximumThreshold()) {
                moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
            }
        }
        if (this.moodleType == MoodleType.BLEEDING && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0f) {
            moodleLevelToSet = this.isoGameCharacter.getBodyDamage().getNumPartsBleeding();
            if (this.isoGameCharacter.getBodyDamage().isNeckBleeding()) {
                moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
            }
            if (moodleLevelToSet > MoodleLevel.MaxMoodleLevel.ordinal()) {
                moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
            }
        }
        if (this.moodleType == MoodleType.WET && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0f) {
            float wetness = this.isoGameCharacter.getStats().get(CharacterStat.WETNESS);
            if (wetness > MoodleStat.WET.getLowestThreshold()) {
                moodleLevelToSet = MoodleLevel.LowMoodleLevel.ordinal();
            }
            if (wetness > MoodleStat.WET.getModerateThreshold()) {
                moodleLevelToSet = MoodleLevel.ModerateMoodleLevel.ordinal();
            }
            if (wetness > MoodleStat.WET.getHighestThreshold()) {
                moodleLevelToSet = MoodleLevel.HighMoodleLevel.ordinal();
            }
            if (wetness > MoodleStat.WET.getMaximumThreshold()) {
                moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
            }
        }
        if (this.moodleType == MoodleType.HAS_A_COLD && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0f) {
            if (this.isoGameCharacter.getBodyDamage().getColdStrength() > MoodleStat.HAS_A_COLD.getLowestThreshold()) {
                moodleLevelToSet = MoodleLevel.LowMoodleLevel.ordinal();
            }
            if (this.isoGameCharacter.getBodyDamage().getColdStrength() > MoodleStat.HAS_A_COLD.getModerateThreshold()) {
                moodleLevelToSet = MoodleLevel.ModerateMoodleLevel.ordinal();
            }
            if (this.isoGameCharacter.getBodyDamage().getColdStrength() > MoodleStat.HAS_A_COLD.getHighestThreshold()) {
                moodleLevelToSet = MoodleLevel.HighMoodleLevel.ordinal();
            }
            if (this.isoGameCharacter.getBodyDamage().getColdStrength() > MoodleStat.HAS_A_COLD.getMaximumThreshold()) {
                moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
            }
        }
        if (this.moodleType == MoodleType.INJURED && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0f) {
            if (100.0f - this.isoGameCharacter.getBodyDamage().getHealth() > MoodleStat.INJURED.getLowestThreshold()) {
                moodleLevelToSet = MoodleLevel.LowMoodleLevel.ordinal();
            }
            if (100.0f - this.isoGameCharacter.getBodyDamage().getHealth() > MoodleStat.INJURED.getModerateThreshold()) {
                moodleLevelToSet = MoodleLevel.ModerateMoodleLevel.ordinal();
            }
            if (100.0f - this.isoGameCharacter.getBodyDamage().getHealth() > MoodleStat.INJURED.getHighestThreshold()) {
                moodleLevelToSet = MoodleLevel.HighMoodleLevel.ordinal();
            }
            if (100.0f - this.isoGameCharacter.getBodyDamage().getHealth() > MoodleStat.INJURED.getMaximumThreshold()) {
                moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
            }
        }
        if (this.moodleType == MoodleType.PAIN) {
            IsoPlayer player;
            IsoGameCharacter isoGameCharacter2;
            ++this.painTimer;
            if (this.painTimer < 120) {
                return false;
            }
            this.painTimer = 0;
            if (this.isoGameCharacter.getBodyDamage().getHealth() != 0.0f) {
                float pain = this.isoGameCharacter.getStats().get(CharacterStat.PAIN);
                if (pain > MoodleStat.PAIN.getLowestThreshold()) {
                    moodleLevelToSet = MoodleLevel.LowMoodleLevel.ordinal();
                }
                if (pain > MoodleStat.PAIN.getModerateThreshold()) {
                    moodleLevelToSet = MoodleLevel.ModerateMoodleLevel.ordinal();
                }
                if (pain > MoodleStat.PAIN.getHighestThreshold()) {
                    moodleLevelToSet = MoodleLevel.HighMoodleLevel.ordinal();
                }
                if (pain > MoodleStat.PAIN.getMaximumThreshold()) {
                    moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
                }
            }
            if (moodleLevelToSet != this.getLevel() && moodleLevelToSet >= MoodleLevel.HighMoodleLevel.ordinal() && this.getLevel() < MoodleLevel.HighMoodleLevel.ordinal() && (isoGameCharacter2 = this.isoGameCharacter) instanceof IsoPlayer && (player = (IsoPlayer)isoGameCharacter2).isLocalPlayer()) {
                player.playerVoiceSound("PainMoodle");
            }
        }
        if (this.moodleType == MoodleType.HEAVY_LOAD) {
            float weight = this.isoGameCharacter.getInventory().getCapacityWeight();
            float maxWeight = this.isoGameCharacter.getMaxWeight();
            float ratio = weight / maxWeight;
            if (this.isoGameCharacter.getBodyDamage().getHealth() != 0.0f) {
                if (ratio >= MoodleStat.HEAVY_LOAD.getMaximumThreshold()) {
                    moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
                } else if (ratio >= MoodleStat.HEAVY_LOAD.getHighestThreshold()) {
                    moodleLevelToSet = MoodleLevel.HighMoodleLevel.ordinal();
                } else if (ratio >= MoodleStat.HEAVY_LOAD.getModerateThreshold()) {
                    moodleLevelToSet = MoodleLevel.ModerateMoodleLevel.ordinal();
                } else if (ratio > MoodleStat.HEAVY_LOAD.getLowestThreshold()) {
                    moodleLevelToSet = MoodleLevel.LowMoodleLevel.ordinal();
                }
            }
        }
        if (this.moodleType == MoodleType.DRUNK && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0f) {
            float intoxication = this.isoGameCharacter.getStats().get(CharacterStat.INTOXICATION);
            if (intoxication > MoodleStat.DRUNK.getLowestThreshold()) {
                moodleLevelToSet = MoodleLevel.LowMoodleLevel.ordinal();
            }
            if (intoxication > MoodleStat.DRUNK.getModerateThreshold()) {
                moodleLevelToSet = MoodleLevel.ModerateMoodleLevel.ordinal();
            }
            if (intoxication > MoodleStat.DRUNK.getHighestThreshold()) {
                moodleLevelToSet = MoodleLevel.HighMoodleLevel.ordinal();
            }
            if (intoxication > MoodleStat.DRUNK.getMaximumThreshold()) {
                moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
            }
        }
        if (this.moodleType == MoodleType.DEAD && this.isoGameCharacter.isDead()) {
            moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
            if (!this.isoGameCharacter.getBodyDamage().IsFakeInfected() && this.isoGameCharacter.getStats().get(CharacterStat.ZOMBIE_INFECTION) >= 0.001f) {
                moodleLevelToSet = MoodleLevel.MinMoodleLevel.ordinal();
            }
        }
        if (this.moodleType == MoodleType.ZOMBIE && this.isoGameCharacter.isDead() && !this.isoGameCharacter.getBodyDamage().IsFakeInfected() && this.isoGameCharacter.getStats().get(CharacterStat.ZOMBIE_INFECTION) >= 0.001f) {
            moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
        }
        if (this.moodleType == MoodleType.FOOD_EATEN && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0f) {
            if (this.isoGameCharacter.getBodyDamage().getHealthFromFoodTimer() > 0.0f) {
                moodleLevelToSet = MoodleLevel.LowMoodleLevel.ordinal();
            }
            if (this.isoGameCharacter.getBodyDamage().getHealthFromFoodTimer() > (float)this.isoGameCharacter.getBodyDamage().getStandardHealthFromFoodTime()) {
                moodleLevelToSet = MoodleLevel.ModerateMoodleLevel.ordinal();
            }
            if (this.isoGameCharacter.getBodyDamage().getHealthFromFoodTimer() > (float)this.isoGameCharacter.getBodyDamage().getStandardHealthFromFoodTime() * 2.0f) {
                moodleLevelToSet = MoodleLevel.HighMoodleLevel.ordinal();
            }
            if (this.isoGameCharacter.getBodyDamage().getHealthFromFoodTimer() > (float)this.isoGameCharacter.getBodyDamage().getStandardHealthFromFoodTime() * 3.0f) {
                moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
            }
        }
        int chevCount = this.chevronCount;
        boolean chevIsUp = this.chevronIsUp;
        Color chevCol = this.chevronColor;
        float temperature = this.isoGameCharacter.getStats().get(CharacterStat.TEMPERATURE);
        if ((this.moodleType == MoodleType.HYPERTHERMIA || this.moodleType == MoodleType.HYPOTHERMIA) && this.isoGameCharacter instanceof IsoPlayer) {
            if (temperature < 36.5f || temperature > 37.5f) {
                Thermoregulator thermos = this.isoGameCharacter.getBodyDamage().getThermoregulator();
                if (thermos == null) {
                    chevCount = 0;
                } else {
                    chevIsUp = thermos.thermalChevronUp();
                    chevCount = thermos.thermalChevronCount();
                }
            } else {
                chevCount = 0;
            }
        }
        if (this.moodleType == MoodleType.HYPERTHERMIA) {
            if (chevCount > 0) {
                Color color = chevCol = chevIsUp ? colorNeg : colorPos;
            }
            if (temperature != 0.0f) {
                if (temperature > 37.5f) {
                    moodleLevelToSet = MoodleLevel.LowMoodleLevel.ordinal();
                }
                if (temperature > 39.0f) {
                    moodleLevelToSet = MoodleLevel.ModerateMoodleLevel.ordinal();
                }
                if (temperature > 40.0f) {
                    moodleLevelToSet = MoodleLevel.HighMoodleLevel.ordinal();
                }
                if (temperature > 41.0f) {
                    moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
                }
            }
            if (moodleLevelToSet != this.getLevel() || moodleLevelToSet > MoodleLevel.MinMoodleLevel.ordinal() && this.chevronDifference(chevCount, chevIsUp, chevCol)) {
                this.setChevron(chevCount, chevIsUp, chevCol);
            }
        }
        if (this.moodleType == MoodleType.HYPOTHERMIA) {
            if (chevCount > 0) {
                Color color = chevCol = chevIsUp ? colorPos : colorNeg;
            }
            if (temperature != 0.0f) {
                if (temperature < 36.5f && this.isoGameCharacter.getStats().get(CharacterStat.INTOXICATION) <= MoodleStat.HYPOTHERMIA.getLowestThreshold()) {
                    moodleLevelToSet = MoodleLevel.LowMoodleLevel.ordinal();
                }
                if (temperature < 35.0f && this.isoGameCharacter.getStats().get(CharacterStat.INTOXICATION) <= MoodleStat.HYPOTHERMIA.getModerateThreshold()) {
                    moodleLevelToSet = MoodleLevel.ModerateMoodleLevel.ordinal();
                }
                if (temperature < 30.0f) {
                    moodleLevelToSet = MoodleLevel.HighMoodleLevel.ordinal();
                }
                if (temperature < 25.0f) {
                    moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
                }
            }
            if (moodleLevelToSet != this.getLevel() || moodleLevelToSet > MoodleLevel.MinMoodleLevel.ordinal() && this.chevronDifference(chevCount, chevIsUp, chevCol)) {
                this.setChevron(chevCount, chevIsUp, chevCol);
            }
        }
        if (this.moodleType == MoodleType.WINDCHILL && (isoGameCharacter = this.isoGameCharacter) instanceof IsoPlayer) {
            IsoPlayer isoPlayer = (IsoPlayer)isoGameCharacter;
            float windChillAmount = Temperature.getWindChillAmountForPlayer(isoPlayer);
            if (windChillAmount > MoodleStat.WINDCHILL.getHighestThreshold()) {
                moodleLevelToSet = MoodleLevel.LowMoodleLevel.ordinal();
            }
            if (windChillAmount > MoodleStat.WINDCHILL.getModerateThreshold()) {
                moodleLevelToSet = MoodleLevel.ModerateMoodleLevel.ordinal();
            }
            if (windChillAmount > MoodleStat.WINDCHILL.getHighestThreshold()) {
                moodleLevelToSet = MoodleLevel.HighMoodleLevel.ordinal();
            }
            if (windChillAmount > MoodleStat.WINDCHILL.getMaximumThreshold()) {
                moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
            }
        }
        if (this.moodleType == MoodleType.UNCOMFORTABLE && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0f) {
            float discomfort = this.isoGameCharacter.getStats().get(CharacterStat.DISCOMFORT);
            if (discomfort >= MoodleStat.UNCOMFORTABLE.getMaximumThreshold()) {
                moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
            } else if (discomfort >= MoodleStat.UNCOMFORTABLE.getHighestThreshold()) {
                moodleLevelToSet = MoodleLevel.HighMoodleLevel.ordinal();
            } else if (discomfort >= MoodleStat.UNCOMFORTABLE.getModerateThreshold()) {
                moodleLevelToSet = MoodleLevel.ModerateMoodleLevel.ordinal();
            } else if (discomfort >= MoodleStat.UNCOMFORTABLE.getLowestThreshold()) {
                moodleLevelToSet = MoodleLevel.LowMoodleLevel.ordinal();
            }
        }
        if (this.moodleType == MoodleType.NOXIOUS_SMELL && this.isoGameCharacter.getBodyDamage() != null && this.isoGameCharacter.getBodyDamage().getHealth() != 0.0f) {
            float rate;
            if (this.isoGameCharacter.getCurrentBuilding() != null && this.isoGameCharacter.getCurrentBuilding().isToxic() && !this.isoGameCharacter.isProtectedFromToxic(false)) {
                moodleLevelToSet = MoodleLevel.MaxMoodleLevel.ordinal();
            } else if (SandboxOptions.instance.decayingCorpseHealthImpact.getValue() != 1 && (rate = this.isoGameCharacter.getCorpseSicknessRate()) > 0.0f) {
                moodleLevelToSet = rate > MoodleStat.NOXIOUS_SMELL.getHighestThreshold() ? MoodleLevel.HighMoodleLevel.ordinal() : (rate > MoodleStat.NOXIOUS_SMELL.getModerateThreshold() ? MoodleLevel.ModerateMoodleLevel.ordinal() : MoodleLevel.LowMoodleLevel.ordinal());
            }
        }
        return this.updateMoodleLevel(moodleLevelToSet);
    }

    public static enum MoodleLevel {
        MinMoodleLevel,
        LowMoodleLevel,
        ModerateMoodleLevel,
        HighMoodleLevel,
        MaxMoodleLevel;

    }
}

