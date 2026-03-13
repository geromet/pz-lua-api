/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.traits;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.ResourceLocation;

@UsedFromLua
public final class CharacterTraits {
    private final Map<CharacterTrait, Boolean> traits = new LinkedHashMap<CharacterTrait, Boolean>();
    private final List<CharacterTrait> knownTraits = new ArrayList<CharacterTrait>();
    private static final float EmaciatedDamageDealtReductionModifier = 0.4f;
    private static final float VeryUnderweightDamageDealtReductionModifier = 0.6f;
    private static final float UnderweightDamageDealtReductionModifier = 0.8f;
    private static final float TraitDamageDealtReductionDefaultModifier = 1.0f;
    public static final int ObeseStrengthPenalty = 2;
    public static final int OverweightStrengthPenalty = 1;
    public static final int AllThumbsStrengthPenalty = 1;
    public static final int DextrousStrengthBonus = 1;
    public static final int BurglarStrengthBonus = 1;
    public static final int GymnastStrengthBonus = 1;
    private static final float AsthmaticEnduranceLossModifier = 1.2f;
    private static final float TraitEnduranceLossDefaultModifier = 1.0f;
    private static final float OutdoorsmanWeatherPenaltyModifier = 1.0f;
    private static final float TraitWeatherPenaltyDefaultModifier = 1.5f;
    public static final float ObeseClimbingPenalty = -25.0f;
    public static final float OverweightClimbingPenalty = -15.0f;
    public static final float ClumsyClimbingPenaltyDivisor = 2.0f;
    public static final float AwkwardGlovesClimbingPenaltyDivisor = 2.0f;
    public static final float RegularGlovesClimbingBonus = 4.0f;
    public static final float PerkClimbingBonusMultiplier = 2.0f;
    public static final float EnduranceClimbingPenaltyMultiplier = -5.0f;
    public static final float DrunkClimbingPenaltyMultiplier = -8.0f;
    public static final float HeavyLoadClimbingPenaltyMultiplier = -8.0f;
    public static final float PainClimbingPenaltyMultiplier = -5.0f;
    public static final float AllThumbsClimbingPenalty = -4.0f;
    public static final float DextrousClimbingBonus = 4.0f;
    public static final float BurglarClimbingBonus = 4.0f;
    public static final float GymnastClimbingBonus = 4.0f;
    public static final float HealthReductionMultiplierModerate = 1.2f;
    public static final float HealthReductionMultiplierSevere = 1.4f;

    public CharacterTraits() {
        for (ResourceLocation key : Registries.CHARACTER_TRAIT.keys()) {
            this.traits.put(Registries.CHARACTER_TRAIT.get(key), false);
        }
    }

    public boolean get(CharacterTrait characterTrait) {
        return this.traits.get(characterTrait);
    }

    public boolean set(CharacterTrait characterTrait, boolean value) {
        boolean oldValue = this.get(characterTrait);
        this.traits.put(characterTrait, value);
        if (value) {
            this.knownTraits.add(characterTrait);
        } else {
            this.knownTraits.remove(characterTrait);
        }
        return value != oldValue;
    }

    public void add(CharacterTrait characterTrait) {
        this.set(characterTrait, true);
    }

    public void remove(CharacterTrait characterTrait) {
        this.set(characterTrait, false);
    }

    public void load(ByteBuffer input) throws IOException {
        this.reset();
        int numberOfKnownTraits = input.getInt();
        for (int i = 0; i < numberOfKnownTraits; ++i) {
            CharacterTrait characterTrait = CharacterTrait.get(ResourceLocation.of(GameWindow.ReadString(input)));
            this.add(characterTrait);
        }
    }

    public void save(ByteBuffer output) throws IOException {
        int numberOfKnownTraits = this.knownTraits.size();
        output.putInt(numberOfKnownTraits);
        for (CharacterTrait characterTrait : this.knownTraits) {
            GameWindow.WriteString(output, Registries.CHARACTER_TRAIT.getLocation(characterTrait).toString());
        }
    }

    public void write(ByteBufferWriter output) {
        int numberOfKnownTraits = this.knownTraits.size();
        output.putInt(numberOfKnownTraits);
        for (CharacterTrait characterTrait : this.knownTraits) {
            output.putUTF(Registries.CHARACTER_TRAIT.getLocation(characterTrait).toString());
        }
    }

    public void read(ByteBufferReader input) {
        int numberOfKnownTraits = input.getInt();
        for (int i = 0; i < numberOfKnownTraits; ++i) {
            CharacterTrait characterTrait = CharacterTrait.get(ResourceLocation.of(input.getUTF()));
            this.add(characterTrait);
        }
    }

    public Map<CharacterTrait, Boolean> getTraits() {
        return this.traits;
    }

    public List<CharacterTrait> getKnownTraits() {
        return new ArrayList<CharacterTrait>(this.knownTraits);
    }

    public float getTraitDamageDealtReductionModifier() {
        float damageReductionModifier = 1.0f;
        if (this.traits.get(CharacterTrait.UNDERWEIGHT).booleanValue()) {
            damageReductionModifier *= 0.8f;
        }
        if (this.traits.get(CharacterTrait.VERY_UNDERWEIGHT).booleanValue()) {
            damageReductionModifier *= 0.6f;
        }
        if (this.traits.get(CharacterTrait.EMACIATED).booleanValue()) {
            damageReductionModifier *= 0.4f;
        }
        return damageReductionModifier;
    }

    public float getTraitEnduranceLossModifier() {
        return this.traits.get(CharacterTrait.ASTHMATIC) != false ? 1.2f : 1.0f;
    }

    public float getTraitWeatherPenaltyModifier() {
        return this.traits.get(CharacterTrait.OUTDOORSMAN) != false ? 1.0f : 1.5f;
    }

    private void reset() {
        this.knownTraits.clear();
        for (Map.Entry<CharacterTrait, Boolean> entry : this.traits.entrySet()) {
            entry.setValue(false);
        }
    }
}

