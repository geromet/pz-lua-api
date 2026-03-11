/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.BodyDamage;

import zombie.UsedFromLua;
import zombie.core.Translator;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.debug.DebugLog;

@UsedFromLua
public enum BodyPartType {
    Hand_L,
    Hand_R,
    ForeArm_L,
    ForeArm_R,
    UpperArm_L,
    UpperArm_R,
    Torso_Upper,
    Torso_Lower,
    Head,
    Neck,
    Groin,
    UpperLeg_L,
    UpperLeg_R,
    LowerLeg_L,
    LowerLeg_R,
    Foot_L,
    Foot_R,
    MAX;


    public static BodyPartType FromIndex(int index) {
        switch (index) {
            case 0: {
                return Hand_L;
            }
            case 1: {
                return Hand_R;
            }
            case 2: {
                return ForeArm_L;
            }
            case 3: {
                return ForeArm_R;
            }
            case 4: {
                return UpperArm_L;
            }
            case 5: {
                return UpperArm_R;
            }
            case 6: {
                return Torso_Upper;
            }
            case 7: {
                return Torso_Lower;
            }
            case 8: {
                return Head;
            }
            case 9: {
                return Neck;
            }
            case 10: {
                return Groin;
            }
            case 11: {
                return UpperLeg_L;
            }
            case 12: {
                return UpperLeg_R;
            }
            case 13: {
                return LowerLeg_L;
            }
            case 14: {
                return LowerLeg_R;
            }
            case 15: {
                return Foot_L;
            }
            case 16: {
                return Foot_R;
            }
        }
        return MAX;
    }

    public int index() {
        return BodyPartType.ToIndex(this);
    }

    public static BodyPartType FromString(String str) {
        if (str.equals("Hand_L")) {
            return Hand_L;
        }
        if (str.equals("Hand_R")) {
            return Hand_R;
        }
        if (str.equals("ForeArm_L")) {
            return ForeArm_L;
        }
        if (str.equals("ForeArm_R")) {
            return ForeArm_R;
        }
        if (str.equals("UpperArm_L")) {
            return UpperArm_L;
        }
        if (str.equals("UpperArm_R")) {
            return UpperArm_R;
        }
        if (str.equals("Torso_Upper")) {
            return Torso_Upper;
        }
        if (str.equals("Torso_Lower")) {
            return Torso_Lower;
        }
        if (str.equals("Head")) {
            return Head;
        }
        if (str.equals("Neck")) {
            return Neck;
        }
        if (str.equals("Groin")) {
            return Groin;
        }
        if (str.equals("UpperLeg_L")) {
            return UpperLeg_L;
        }
        if (str.equals("UpperLeg_R")) {
            return UpperLeg_R;
        }
        if (str.equals("LowerLeg_L")) {
            return LowerLeg_L;
        }
        if (str.equals("LowerLeg_R")) {
            return LowerLeg_R;
        }
        if (str.equals("Foot_L")) {
            return Foot_L;
        }
        if (str.equals("Foot_R")) {
            return Foot_R;
        }
        return MAX;
    }

    public static float getPainModifyer(int index) {
        switch (index) {
            case 0: {
                return 0.5f;
            }
            case 1: {
                return 0.5f;
            }
            case 2: {
                return 0.6f;
            }
            case 3: {
                return 0.6f;
            }
            case 4: {
                return 0.6f;
            }
            case 5: {
                return 0.6f;
            }
            case 6: {
                return 0.7f;
            }
            case 7: {
                return 0.78f;
            }
            case 8: {
                return 0.8f;
            }
            case 9: {
                return 0.8f;
            }
            case 10: {
                return 0.7f;
            }
            case 11: {
                return 0.7f;
            }
            case 12: {
                return 0.7f;
            }
            case 13: {
                return 0.6f;
            }
            case 14: {
                return 0.6f;
            }
            case 15: {
                return 0.5f;
            }
            case 16: {
                return 0.5f;
            }
        }
        return 1.0f;
    }

    public static String getDisplayName(BodyPartType bpt) {
        if (bpt == Hand_L) {
            return Translator.getText("IGUI_health_Left_Hand");
        }
        if (bpt == Hand_R) {
            return Translator.getText("IGUI_health_Right_Hand");
        }
        if (bpt == ForeArm_L) {
            return Translator.getText("IGUI_health_Left_Forearm");
        }
        if (bpt == ForeArm_R) {
            return Translator.getText("IGUI_health_Right_Forearm");
        }
        if (bpt == UpperArm_L) {
            return Translator.getText("IGUI_health_Left_Upper_Arm");
        }
        if (bpt == UpperArm_R) {
            return Translator.getText("IGUI_health_Right_Upper_Arm");
        }
        if (bpt == Torso_Upper) {
            return Translator.getText("IGUI_health_Upper_Torso");
        }
        if (bpt == Torso_Lower) {
            return Translator.getText("IGUI_health_Lower_Torso");
        }
        if (bpt == Head) {
            return Translator.getText("IGUI_health_Head");
        }
        if (bpt == Neck) {
            return Translator.getText("IGUI_health_Neck");
        }
        if (bpt == Groin) {
            return Translator.getText("IGUI_health_Groin");
        }
        if (bpt == UpperLeg_L) {
            return Translator.getText("IGUI_health_Left_Thigh");
        }
        if (bpt == UpperLeg_R) {
            return Translator.getText("IGUI_health_Right_Thigh");
        }
        if (bpt == LowerLeg_L) {
            return Translator.getText("IGUI_health_Left_Shin");
        }
        if (bpt == LowerLeg_R) {
            return Translator.getText("IGUI_health_Right_Shin");
        }
        if (bpt == Foot_L) {
            return Translator.getText("IGUI_health_Left_Foot");
        }
        if (bpt == Foot_R) {
            return Translator.getText("IGUI_health_Right_Foot");
        }
        return Translator.getText("IGUI_health_Unknown_Body_Part");
    }

    public static int ToIndex(BodyPartType bpt) {
        if (bpt == null) {
            return 0;
        }
        switch (bpt.ordinal()) {
            case 0: {
                return 0;
            }
            case 1: {
                return 1;
            }
            case 2: {
                return 2;
            }
            case 3: {
                return 3;
            }
            case 4: {
                return 4;
            }
            case 5: {
                return 5;
            }
            case 6: {
                return 6;
            }
            case 7: {
                return 7;
            }
            case 8: {
                return 8;
            }
            case 9: {
                return 9;
            }
            case 10: {
                return 10;
            }
            case 11: {
                return 11;
            }
            case 12: {
                return 12;
            }
            case 13: {
                return 13;
            }
            case 14: {
                return 14;
            }
            case 15: {
                return 15;
            }
            case 16: {
                return 16;
            }
            case 17: {
                return 17;
            }
        }
        return 17;
    }

    public static String ToString(BodyPartType bpt) {
        if (bpt == Hand_L) {
            return "Hand_L";
        }
        if (bpt == Hand_R) {
            return "Hand_R";
        }
        if (bpt == ForeArm_L) {
            return "ForeArm_L";
        }
        if (bpt == ForeArm_R) {
            return "ForeArm_R";
        }
        if (bpt == UpperArm_L) {
            return "UpperArm_L";
        }
        if (bpt == UpperArm_R) {
            return "UpperArm_R";
        }
        if (bpt == Torso_Upper) {
            return "Torso_Upper";
        }
        if (bpt == Torso_Lower) {
            return "Torso_Lower";
        }
        if (bpt == Head) {
            return "Head";
        }
        if (bpt == Neck) {
            return "Neck";
        }
        if (bpt == Groin) {
            return "Groin";
        }
        if (bpt == UpperLeg_L) {
            return "UpperLeg_L";
        }
        if (bpt == UpperLeg_R) {
            return "UpperLeg_R";
        }
        if (bpt == LowerLeg_L) {
            return "LowerLeg_L";
        }
        if (bpt == LowerLeg_R) {
            return "LowerLeg_R";
        }
        if (bpt == Foot_L) {
            return "Foot_L";
        }
        if (bpt == Foot_R) {
            return "Foot_R";
        }
        return "Unknown Body Part";
    }

    public static float getDamageModifyer(int index) {
        switch (index) {
            case 0: {
                return 0.1f;
            }
            case 1: {
                return 0.1f;
            }
            case 2: {
                return 0.2f;
            }
            case 3: {
                return 0.2f;
            }
            case 4: {
                return 0.3f;
            }
            case 5: {
                return 0.3f;
            }
            case 6: {
                return 0.35f;
            }
            case 7: {
                return 0.4f;
            }
            case 8: {
                return 0.6f;
            }
            case 9: {
                return 0.7f;
            }
            case 10: {
                return 0.4f;
            }
            case 11: {
                return 0.3f;
            }
            case 12: {
                return 0.3f;
            }
            case 13: {
                return 0.2f;
            }
            case 14: {
                return 0.2f;
            }
            case 15: {
                return 0.2f;
            }
            case 16: {
                return 0.2f;
            }
        }
        return 1.0f;
    }

    public static float getBleedingTimeModifyer(int index) {
        switch (index) {
            case 0: {
                return 0.2f;
            }
            case 1: {
                return 0.2f;
            }
            case 2: {
                return 0.3f;
            }
            case 3: {
                return 0.3f;
            }
            case 4: {
                return 0.4f;
            }
            case 5: {
                return 0.4f;
            }
            case 6: {
                return 0.5f;
            }
            case 7: {
                return 0.9f;
            }
            case 8: {
                return 1.0f;
            }
            case 9: {
                return 1.5f;
            }
            case 10: {
                return 0.5f;
            }
            case 11: {
                return 0.4f;
            }
            case 12: {
                return 0.4f;
            }
            case 13: {
                return 0.3f;
            }
            case 14: {
                return 0.3f;
            }
            case 15: {
                return 0.2f;
            }
            case 16: {
                return 0.2f;
            }
        }
        return 1.0f;
    }

    public static float GetSkinSurface(BodyPartType bodyPartType) {
        if (bodyPartType == null) {
            return 0.001f;
        }
        switch (bodyPartType.ordinal()) {
            case 6: {
                return 0.18f;
            }
            case 8: {
                return 0.08f;
            }
            case 9: {
                return 0.02f;
            }
            case 7: {
                return 0.12f;
            }
            case 10: {
                return 0.06f;
            }
            case 11: 
            case 12: {
                return 0.09f;
            }
            case 13: 
            case 14: {
                return 0.07f;
            }
            case 15: 
            case 16: {
                return 0.02f;
            }
            case 4: 
            case 5: {
                return 0.045f;
            }
            case 2: 
            case 3: {
                return 0.035f;
            }
            case 0: 
            case 1: {
                return 0.01f;
            }
        }
        DebugLog.log("Warning: couldnt get skinSurface for body part '" + String.valueOf((Object)bodyPartType) + "'.");
        return 0.001f;
    }

    public static float GetDistToCore(BodyPartType bodyPartType) {
        if (bodyPartType == null) {
            return 0.0f;
        }
        switch (bodyPartType.ordinal()) {
            case 6: {
                return 0.0f;
            }
            case 8: {
                return 0.05f;
            }
            case 9: {
                return 0.02f;
            }
            case 7: {
                return 0.05f;
            }
            case 10: {
                return 0.3f;
            }
            case 11: 
            case 12: {
                return 0.45f;
            }
            case 13: 
            case 14: {
                return 0.75f;
            }
            case 15: 
            case 16: {
                return 1.0f;
            }
            case 4: 
            case 5: {
                return 0.3f;
            }
            case 2: 
            case 3: {
                return 0.6f;
            }
            case 0: 
            case 1: {
                return 0.8f;
            }
        }
        DebugLog.log("Warning: couldnt get distToCore for body part '" + String.valueOf((Object)bodyPartType) + "'.");
        return 0.0f;
    }

    public static float GetUmbrellaMod(BodyPartType bodyPartType) {
        if (bodyPartType == null) {
            return 1.0f;
        }
        switch (bodyPartType.ordinal()) {
            case 6: {
                return 0.2f;
            }
            case 8: {
                return 0.05f;
            }
            case 9: {
                return 0.1f;
            }
            case 7: {
                return 0.25f;
            }
            case 10: {
                return 0.3f;
            }
            case 11: 
            case 12: {
                return 0.55f;
            }
            case 13: 
            case 14: {
                return 0.75f;
            }
            case 15: 
            case 16: {
                return 1.0f;
            }
            case 4: 
            case 5: {
                return 0.25f;
            }
            case 2: 
            case 3: {
                return 0.3f;
            }
            case 0: 
            case 1: {
                return 0.35f;
            }
        }
        return 1.0f;
    }

    public static float GetMaxActionPenalty(BodyPartType bodyPartType) {
        if (bodyPartType == null) {
            return 0.0f;
        }
        switch (bodyPartType.ordinal()) {
            case 6: {
                return 0.2f;
            }
            case 8: {
                return 0.4f;
            }
            case 9: {
                return 0.05f;
            }
            case 7: {
                return 0.1f;
            }
            case 10: {
                return 0.05f;
            }
            case 11: 
            case 12: {
                return 0.1f;
            }
            case 13: 
            case 14: {
                return 0.1f;
            }
            case 15: 
            case 16: {
                return 0.1f;
            }
            case 4: 
            case 5: {
                return 0.4f;
            }
            case 2: 
            case 3: {
                return 0.6f;
            }
            case 0: 
            case 1: {
                return 1.0f;
            }
        }
        DebugLog.log("Warning: couldnt get maxActionPenalty for body part '" + String.valueOf((Object)bodyPartType) + "'.");
        return 0.0f;
    }

    public static float GetMaxMovementPenalty(BodyPartType bodyPartType) {
        if (bodyPartType == null) {
            return 0.0f;
        }
        switch (bodyPartType.ordinal()) {
            case 6: {
                return 0.05f;
            }
            case 8: {
                return 0.25f;
            }
            case 9: {
                return 0.05f;
            }
            case 7: {
                return 0.05f;
            }
            case 10: {
                return 0.15f;
            }
            case 11: 
            case 12: {
                return 0.4f;
            }
            case 13: 
            case 14: {
                return 0.6f;
            }
            case 15: 
            case 16: {
                return 1.0f;
            }
            case 4: 
            case 5: {
                return 0.1f;
            }
            case 2: 
            case 3: {
                return 0.1f;
            }
            case 0: 
            case 1: {
                return 0.05f;
            }
        }
        DebugLog.log("Warning: couldnt get maxMovementPenalty for body part '" + String.valueOf((Object)bodyPartType) + "'.");
        return 0.0f;
    }

    public String getBandageModel() {
        switch (this.ordinal()) {
            case 6: {
                return "Base.Bandage_Chest";
            }
            case 8: {
                return "Base.Bandage_Head";
            }
            case 9: {
                return "Base.Bandage_Neck";
            }
            case 7: {
                return "Base.Bandage_Abdomen";
            }
            case 10: {
                return "Base.Bandage_Groin";
            }
            case 11: {
                return "Base.Bandage_LeftUpperLeg";
            }
            case 12: {
                return "Base.Bandage_RightUpperLeg";
            }
            case 13: {
                return "Base.Bandage_LeftLowerLeg";
            }
            case 14: {
                return "Base.Bandage_RightLowerLeg";
            }
            case 15: {
                return "Base.Bandage_LeftFoot";
            }
            case 16: {
                return "Base.Bandage_RightFoot";
            }
            case 4: {
                return "Base.Bandage_LeftUpperArm";
            }
            case 5: {
                return "Base.Bandage_RightUpperArm";
            }
            case 2: {
                return "Base.Bandage_LeftLowerArm";
            }
            case 3: {
                return "Base.Bandage_RightLowerArm";
            }
            case 0: {
                return "Base.Bandage_LeftHand";
            }
            case 1: {
                return "Base.Bandage_RightHand";
            }
        }
        return null;
    }

    public String getBiteWoundModel(boolean female) {
        String gender = "Female";
        if (!female) {
            gender = "Male";
        }
        switch (this.ordinal()) {
            case 6: {
                return "Base.Wound_Chest_Bite_" + gender;
            }
            case 8: {
                return "Base.Wound_Neck_Bite_" + gender;
            }
            case 9: {
                return "Base.Wound_Neck_Bite_" + gender;
            }
            case 7: {
                return "Base.Wound_Abdomen_Bite_" + gender;
            }
            case 10: {
                return "Base.Wound_Groin_Bite_" + gender;
            }
            case 11: {
                return null;
            }
            case 12: {
                return null;
            }
            case 13: {
                return null;
            }
            case 14: {
                return null;
            }
            case 15: {
                return null;
            }
            case 16: {
                return null;
            }
            case 4: {
                return "Base.Wound_LUArm_Bite_" + gender;
            }
            case 5: {
                return "Base.Wound_RUArm_Bite_" + gender;
            }
            case 2: {
                return "Base.Wound_LForearm_Bite_" + gender;
            }
            case 3: {
                return "Base.Wound_RForearm_Bite_" + gender;
            }
            case 0: {
                return "Base.Wound_LHand_Bite_" + gender;
            }
            case 1: {
                return "Base.Wound_RHand_Bite_" + gender;
            }
        }
        return null;
    }

    public String getScratchWoundModel(boolean female) {
        String gender = "Female";
        if (!female) {
            gender = "Male";
        }
        switch (this.ordinal()) {
            case 6: {
                return "Base.Wound_Chest_Scratch_" + gender;
            }
            case 8: {
                return "Base.Wound_Neck_Scratch_" + gender;
            }
            case 9: {
                return "Base.Wound_Neck_Scratch_" + gender;
            }
            case 7: {
                return "Base.Wound_Abdomen_Scratch_" + gender;
            }
            case 10: {
                return "Base.Wound_Groin_Scratch_" + gender;
            }
            case 11: {
                return null;
            }
            case 12: {
                return null;
            }
            case 13: {
                return null;
            }
            case 14: {
                return null;
            }
            case 15: {
                return null;
            }
            case 16: {
                return null;
            }
            case 4: {
                return "Base.Wound_LUArm_Scratch_" + gender;
            }
            case 5: {
                return "Base.Wound_RUArm_Scratch_" + gender;
            }
            case 2: {
                return "Base.Wound_LForearm_Scratch_" + gender;
            }
            case 3: {
                return "Base.Wound_RForearm_Scratch_" + gender;
            }
            case 0: {
                return "Base.Wound_LHand_Scratch_" + gender;
            }
            case 1: {
                return "Base.Wound_RHand_Scratch_" + gender;
            }
        }
        return null;
    }

    public String getCutWoundModel(boolean female) {
        String gender = "Female";
        if (!female) {
            gender = "Male";
        }
        switch (this.ordinal()) {
            case 6: {
                return "Base.Wound_Chest_Laceration_" + gender;
            }
            case 8: {
                return "Base.Wound_Neck_Laceration_" + gender;
            }
            case 9: {
                return "Base.Wound_Neck_Laceration_" + gender;
            }
            case 7: {
                return "Base.Wound_Abdomen_Laceration_" + gender;
            }
            case 10: {
                return "Base.Wound_Groin_Laceration_" + gender;
            }
            case 11: {
                return null;
            }
            case 12: {
                return null;
            }
            case 13: {
                return null;
            }
            case 14: {
                return null;
            }
            case 15: {
                return null;
            }
            case 16: {
                return null;
            }
            case 4: {
                return "Base.Wound_LUArm_Laceration_" + gender;
            }
            case 5: {
                return "Base.Wound_RUArm_Laceration_" + gender;
            }
            case 2: {
                return "Base.Wound_LForearm_Laceration_" + gender;
            }
            case 3: {
                return "Base.Wound_RForearm_Laceration_" + gender;
            }
            case 0: {
                return "Base.Wound_LHand_Laceration_" + gender;
            }
            case 1: {
                return "Base.Wound_RHand_Laceration_" + gender;
            }
        }
        return null;
    }

    public static BodyPartType getRandom() {
        return BodyPartType.FromIndex(OutfitRNG.Next(0, MAX.index()));
    }
}

