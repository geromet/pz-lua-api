/*
 * Decompiled with CFR 0.152.
 */
package zombie.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.random.Rand;

@UsedFromLua
public final class Colors {
    private static final HashMap<String, ColNfo> infoMap = new HashMap();
    private static final ArrayList<ColNfo> infoList = new ArrayList();
    private static final ArrayList<Color> colors = new ArrayList();
    private static final HashMap<String, Color> colorMap = new HashMap();
    private static final ArrayList<String> colorNames = new ArrayList();
    private static final HashSet<String> colorSet = new HashSet();
    private static final HashMap<String, ColNfo> CB_infoMap = new HashMap();
    private static final ArrayList<Color> CB_colors = new ArrayList();
    private static final HashMap<String, Color> CB_colorMap = new HashMap();
    private static final ArrayList<String> CB_colorNames = new ArrayList();
    private static final HashSet<String> CB_colorSet = new HashSet();
    public static final Color UI_Background = Colors.AddGameColor("UI_Background", new Color(0.0f, 0.0f, 0.0f));
    public static final Color Black = Colors.AddGameColor("Black", new Color(0.0f, 0.0f, 0.0f));
    public static final Color CB_G0_SherwoodGreen = Colors.addColorCB("SherwoodGreen", new Color(0.0f, 0.239f, 0.188f));
    public static final Color CB_G1_PthaloGreen = Colors.addColorCB("PthaloGreen", new Color(0.0f, 0.341f, 0.271f));
    public static final Color CB_G2_TropicalRainForest = Colors.addColorCB("TropicalRainForest", new Color(0.0f, 0.451f, 0.361f));
    public static final Color CB_G3_Observatory = Colors.addColorCB("Observatory", new Color(0.0f, 0.569f, 0.459f));
    public static final Color CB_G4_JungleGreen = Colors.addColorCB("JungleGreen", new Color(0.0f, 0.686f, 0.557f));
    public static final Color CB_G5_Dali = Colors.addColorCB("Dali", new Color(0.0f, 0.796f, 0.655f));
    public static final Color CB_G6_AquaMarine = Colors.addColorCB("AquaMarine", new Color(0.0f, 0.922f, 0.757f));
    public static final Color CB_G7_LightAqua = Colors.addColorCB("LightAqua", new Color(0.525f, 1.0f, 0.871f));
    public static final Color CB_B0_Submerge = Colors.addColorCB("Submerge", new Color(0.0f, 0.188f, 0.435f));
    public static final Color CB_B1_Elvis = Colors.addColorCB("Elvis", new Color(0.0f, 0.282f, 0.62f));
    public static final Color CB_B2_FlatMediumBlue = Colors.addColorCB("FlatMediumBlue", new Color(0.0f, 0.373f, 0.8f));
    public static final Color CB_B3_ClearBlue = Colors.addColorCB("ClearBlue", new Color(0.0f, 0.475f, 0.98f));
    public static final Color CB_B4_Azure = Colors.addColorCB("Azure", new Color(0.0f, 0.624f, 0.98f));
    public static final Color CB_B5_SpiroDiscoBall = Colors.addColorCB("SpiroDiscoBall", new Color(0.0f, 0.761f, 0.976f));
    public static final Color CB_B6_AquaBlue = Colors.addColorCB("AquaBlue", new Color(0.0f, 0.898f, 0.973f));
    public static final Color CB_B7_LightBrilliantCyan = Colors.addColorCB("LightBrilliantCyan", new Color(0.486f, 1.0f, 0.98f));
    public static final Color CB_R0_DeepAmaranth = Colors.addColorCB("DeepAmaranth", new Color(0.373f, 0.0353f, 0.0784f));
    public static final Color CB_R1_HotChile = Colors.addColorCB("HotChile", new Color(0.525f, 0.0314f, 0.11f));
    public static final Color CB_R2_Smashing = Colors.addColorCB("Smashing", new Color(0.698f, 0.0275f, 0.145f));
    public static final Color CB_R3_GeraniumLake = Colors.addColorCB("GeraniumLake", new Color(0.871f, 0.051f, 0.18f));
    public static final Color CB_R4_RedOrange = Colors.addColorCB("RedOrange", new Color(1.0f, 0.259f, 0.208f));
    public static final Color CB_R5_Crusta = Colors.addColorCB("Crusta", new Color(1.0f, 0.529f, 0.208f));
    public static final Color CB_R6_GoldenYellow = Colors.addColorCB("GoldenYellow", new Color(1.0f, 0.725f, 0.208f));
    public static final Color CB_R7_BananaYellow = Colors.addColorCB("BananaYellow", new Color(1.0f, 0.886f, 0.224f));
    public static final Color CB_White = Colors.addColorCB("White", new Color(1.0f, 1.0f, 1.0f));
    public static final Color White = Colors.addColor("White", new Color(1.0f, 1.0f, 1.0f));
    public static final Color Silver = Colors.addColor("Silver", new Color(0.753f, 0.753f, 0.753f));
    public static final Color Gray = Colors.addColor("Gray", new Color(0.502f, 0.502f, 0.502f));
    public static final Color Red = Colors.addColor("Red", new Color(1.0f, 0.0f, 0.0f));
    public static final Color Maroon = Colors.addColor("Maroon", new Color(0.502f, 0.0f, 0.0f));
    public static final Color Yellow = Colors.addColor("Yellow", new Color(1.0f, 1.0f, 0.0f));
    public static final Color Olive = Colors.addColor("Olive", new Color(0.502f, 0.502f, 0.0f));
    public static final Color Lime = Colors.addColor("Lime", new Color(0.0f, 1.0f, 0.0f));
    public static final Color Green = Colors.addColor("Green", new Color(0.0f, 0.502f, 0.0f));
    public static final Color Cyan = Colors.addColor("Cyan", new Color(0.0f, 1.0f, 1.0f));
    public static final Color Teal = Colors.addColor("Teal", new Color(0.0f, 0.502f, 0.502f));
    public static final Color Blue = Colors.addColor("Blue", new Color(0.0f, 0.0f, 1.0f));
    public static final Color Navy = Colors.addColor("Navy", new Color(0.0f, 0.0f, 0.502f));
    public static final Color Magenta = Colors.addColor("Magenta", new Color(1.0f, 0.0f, 1.0f));
    public static final Color Purple = Colors.addColor("Purple", new Color(0.502f, 0.0f, 0.502f));
    public static final Color Orange = Colors.addColor("Orange", new Color(1.0f, 0.647f, 0.0f));
    public static final Color Pink = Colors.addColor("Pink", new Color(1.0f, 0.753f, 0.796f));
    public static final Color Brown = Colors.addColor("Brown", new Color(0.647f, 0.165f, 0.165f));
    public static final Color Gainsboro = Colors.addColor("Gainsboro", new Color(0.863f, 0.863f, 0.863f));
    public static final Color LightGray = Colors.addColor("LightGray", new Color(0.827f, 0.827f, 0.827f));
    public static final Color DarkGray = Colors.addColor("DarkGray", new Color(0.663f, 0.663f, 0.663f));
    public static final Color DimGray = Colors.addColor("DimGray", new Color(0.412f, 0.412f, 0.412f));
    public static final Color LightSlateGray = Colors.addColor("LightSlateGray", new Color(0.467f, 0.533f, 0.6f));
    public static final Color SlateGray = Colors.addColor("SlateGray", new Color(0.439f, 0.502f, 0.565f));
    public static final Color DarkSlateGray = Colors.addColor("DarkSlateGray", new Color(0.184f, 0.31f, 0.31f));
    public static final Color IndianRed = Colors.addColor("IndianRed", new Color(0.804f, 0.361f, 0.361f));
    public static final Color LightCoral = Colors.addColor("LightCoral", new Color(0.941f, 0.502f, 0.502f));
    public static final Color Salmon = Colors.addColor("Salmon", new Color(0.98f, 0.502f, 0.447f));
    public static final Color DarkSalmon = Colors.addColor("DarkSalmon", new Color(0.914f, 0.588f, 0.478f));
    public static final Color LightSalmon = Colors.addColor("LightSalmon", new Color(1.0f, 0.627f, 0.478f));
    public static final Color Crimson = Colors.addColor("Crimson", new Color(0.863f, 0.0784f, 0.235f));
    public static final Color FireBrick = Colors.addColor("FireBrick", new Color(0.698f, 0.133f, 0.133f));
    public static final Color DarkRed = Colors.addColor("DarkRed", new Color(0.545f, 0.0f, 0.0f));
    public static final Color LightPink = Colors.addColor("LightPink", new Color(1.0f, 0.714f, 0.757f));
    public static final Color HotPink = Colors.addColor("HotPink", new Color(1.0f, 0.412f, 0.706f));
    public static final Color DeepPink = Colors.addColor("DeepPink", new Color(1.0f, 0.0784f, 0.576f));
    public static final Color MediumVioletRed = Colors.addColor("MediumVioletRed", new Color(0.78f, 0.0824f, 0.522f));
    public static final Color PaleVioletRed = Colors.addColor("PaleVioletRed", new Color(0.859f, 0.439f, 0.576f));
    public static final Color Coral = Colors.addColor("Coral", new Color(1.0f, 0.498f, 0.314f));
    public static final Color Tomato = Colors.addColor("Tomato", new Color(1.0f, 0.388f, 0.278f));
    public static final Color OrangeRed = Colors.addColor("OrangeRed", new Color(1.0f, 0.271f, 0.0f));
    public static final Color DarkOrange = Colors.addColor("DarkOrange", new Color(1.0f, 0.549f, 0.0f));
    public static final Color Gold = Colors.addColor("Gold", new Color(1.0f, 0.843f, 0.0f));
    public static final Color LightYellow = Colors.addColor("LightYellow", new Color(1.0f, 1.0f, 0.878f));
    public static final Color LemonChiffon = Colors.addColor("LemonChiffon", new Color(1.0f, 0.98f, 0.804f));
    public static final Color LightGoldenrodYellow = Colors.addColor("LightGoldenrodYellow", new Color(0.98f, 0.98f, 0.824f));
    public static final Color PapayaWhip = Colors.addColor("PapayaWhip", new Color(1.0f, 0.937f, 0.835f));
    public static final Color Moccasin = Colors.addColor("Moccasin", new Color(1.0f, 0.894f, 0.71f));
    public static final Color PeachPuff = Colors.addColor("PeachPuff", new Color(1.0f, 0.855f, 0.725f));
    public static final Color PaleGoldenrod = Colors.addColor("PaleGoldenrod", new Color(0.933f, 0.91f, 0.667f));
    public static final Color Khaki = Colors.addColor("Khaki", new Color(0.941f, 0.902f, 0.549f));
    public static final Color DarkKhaki = Colors.addColor("DarkKhaki", new Color(0.741f, 0.718f, 0.42f));
    public static final Color Lavender = Colors.addColor("Lavender", new Color(0.902f, 0.902f, 0.98f));
    public static final Color Thistle = Colors.addColor("Thistle", new Color(0.847f, 0.749f, 0.847f));
    public static final Color Plum = Colors.addColor("Plum", new Color(0.867f, 0.627f, 0.867f));
    public static final Color Violet = Colors.addColor("Violet", new Color(0.933f, 0.51f, 0.933f));
    public static final Color Orchid = Colors.addColor("Orchid", new Color(0.855f, 0.439f, 0.839f));
    public static final Color MediumOrchid = Colors.addColor("MediumOrchid", new Color(0.729f, 0.333f, 0.827f));
    public static final Color MediumPurple = Colors.addColor("MediumPurple", new Color(0.576f, 0.439f, 0.859f));
    public static final Color RebeccaPurple = Colors.addColor("RebeccaPurple", new Color(0.4f, 0.2f, 0.6f));
    public static final Color BlueViolet = Colors.addColor("BlueViolet", new Color(0.541f, 0.169f, 0.886f));
    public static final Color DarkViolet = Colors.addColor("DarkViolet", new Color(0.58f, 0.0f, 0.827f));
    public static final Color DarkOrchid = Colors.addColor("DarkOrchid", new Color(0.6f, 0.196f, 0.8f));
    public static final Color DarkMagenta = Colors.addColor("DarkMagenta", new Color(0.545f, 0.0f, 0.545f));
    public static final Color Indigo = Colors.addColor("Indigo", new Color(0.294f, 0.0f, 0.51f));
    public static final Color SlateBlue = Colors.addColor("SlateBlue", new Color(0.416f, 0.353f, 0.804f));
    public static final Color DarkSlateBlue = Colors.addColor("DarkSlateBlue", new Color(0.282f, 0.239f, 0.545f));
    public static final Color MediumSlateBlue = Colors.addColor("MediumSlateBlue", new Color(0.482f, 0.408f, 0.933f));
    public static final Color GreenYellow = Colors.addColor("GreenYellow", new Color(0.678f, 1.0f, 0.184f));
    public static final Color Chartreuse = Colors.addColor("Chartreuse", new Color(0.498f, 1.0f, 0.0f));
    public static final Color LawnGreen = Colors.addColor("LawnGreen", new Color(0.486f, 0.988f, 0.0f));
    public static final Color LimeGreen = Colors.addColor("LimeGreen", new Color(0.196f, 0.804f, 0.196f));
    public static final Color PaleGreen = Colors.addColor("PaleGreen", new Color(0.596f, 0.984f, 0.596f));
    public static final Color LightGreen = Colors.addColor("LightGreen", new Color(0.565f, 0.933f, 0.565f));
    public static final Color MediumSpringGreen = Colors.addColor("MediumSpringGreen", new Color(0.0f, 0.98f, 0.604f));
    public static final Color SpringGreen = Colors.addColor("SpringGreen", new Color(0.0f, 1.0f, 0.498f));
    public static final Color MediumSeaGreen = Colors.addColor("MediumSeaGreen", new Color(0.235f, 0.702f, 0.443f));
    public static final Color SeaGreen = Colors.addColor("SeaGreen", new Color(0.18f, 0.545f, 0.341f));
    public static final Color ForestGreen = Colors.addColor("ForestGreen", new Color(0.133f, 0.545f, 0.133f));
    public static final Color DarkGreen = Colors.addColor("DarkGreen", new Color(0.0f, 0.392f, 0.0f));
    public static final Color YellowGreen = Colors.addColor("YellowGreen", new Color(0.604f, 0.804f, 0.196f));
    public static final Color OliveDrab = Colors.addColor("OliveDrab", new Color(0.42f, 0.557f, 0.137f));
    public static final Color DarkOliveGreen = Colors.addColor("DarkOliveGreen", new Color(0.333f, 0.42f, 0.184f));
    public static final Color MediumAquamarine = Colors.addColor("MediumAquamarine", new Color(0.4f, 0.804f, 0.667f));
    public static final Color DarkSeaGreen = Colors.addColor("DarkSeaGreen", new Color(0.561f, 0.737f, 0.545f));
    public static final Color LightSeaGreen = Colors.addColor("LightSeaGreen", new Color(0.125f, 0.698f, 0.667f));
    public static final Color DarkCyan = Colors.addColor("DarkCyan", new Color(0.0f, 0.545f, 0.545f));
    public static final Color LightCyan = Colors.addColor("LightCyan", new Color(0.878f, 1.0f, 1.0f));
    public static final Color PaleTurquoise = Colors.addColor("PaleTurquoise", new Color(0.686f, 0.933f, 0.933f));
    public static final Color Aquamarine = Colors.addColor("Aquamarine", new Color(0.498f, 1.0f, 0.831f));
    public static final Color Turquoise = Colors.addColor("Turquoise", new Color(0.251f, 0.878f, 0.816f));
    public static final Color MediumTurquoise = Colors.addColor("MediumTurquoise", new Color(0.282f, 0.82f, 0.8f));
    public static final Color DarkTurquoise = Colors.addColor("DarkTurquoise", new Color(0.0f, 0.808f, 0.82f));
    public static final Color CadetBlue = Colors.addColor("CadetBlue", new Color(0.373f, 0.62f, 0.627f));
    public static final Color SteelBlue = Colors.addColor("SteelBlue", new Color(0.275f, 0.51f, 0.706f));
    public static final Color LightSteelBlue = Colors.addColor("LightSteelBlue", new Color(0.69f, 0.769f, 0.871f));
    public static final Color PowderBlue = Colors.addColor("PowderBlue", new Color(0.69f, 0.878f, 0.902f));
    public static final Color LightBlue = Colors.addColor("LightBlue", new Color(0.678f, 0.847f, 0.902f));
    public static final Color SkyBlue = Colors.addColor("SkyBlue", new Color(0.529f, 0.808f, 0.922f));
    public static final Color LightSkyBlue = Colors.addColor("LightSkyBlue", new Color(0.529f, 0.808f, 0.98f));
    public static final Color DeepSkyBlue = Colors.addColor("DeepSkyBlue", new Color(0.0f, 0.749f, 1.0f));
    public static final Color DodgerBlue = Colors.addColor("DodgerBlue", new Color(0.118f, 0.565f, 1.0f));
    public static final Color CornFlowerBlue = Colors.addColor("CornFlowerBlue", new Color(0.392f, 0.584f, 0.929f));
    public static final Color RoyalBlue = Colors.addColor("RoyalBlue", new Color(0.255f, 0.412f, 0.882f));
    public static final Color MediumBlue = Colors.addColor("MediumBlue", new Color(0.0f, 0.0f, 0.804f));
    public static final Color DarkBlue = Colors.addColor("DarkBlue", new Color(0.0f, 0.0f, 0.545f));
    public static final Color MidnightBlue = Colors.addColor("MidnightBlue", new Color(0.098f, 0.098f, 0.439f));
    public static final Color Cornsilk = Colors.addColor("Cornsilk", new Color(1.0f, 0.973f, 0.863f));
    public static final Color BlanchedAlmond = Colors.addColor("BlanchedAlmond", new Color(1.0f, 0.922f, 0.804f));
    public static final Color Bisque = Colors.addColor("Bisque", new Color(1.0f, 0.894f, 0.769f));
    public static final Color NavajoWhite = Colors.addColor("NavajoWhite", new Color(1.0f, 0.871f, 0.678f));
    public static final Color Wheat = Colors.addColor("Wheat", new Color(0.961f, 0.871f, 0.702f));
    public static final Color BurlyWood = Colors.addColor("BurlyWood", new Color(0.871f, 0.722f, 0.529f));
    public static final Color Tan = Colors.addColor("Tan", new Color(0.824f, 0.706f, 0.549f));
    public static final Color RosyBrown = Colors.addColor("RosyBrown", new Color(0.737f, 0.561f, 0.561f));
    public static final Color SandyBrown = Colors.addColor("SandyBrown", new Color(0.957f, 0.643f, 0.376f));
    public static final Color Goldenrod = Colors.addColor("Goldenrod", new Color(0.855f, 0.647f, 0.125f));
    public static final Color DarkGoldenrod = Colors.addColor("DarkGoldenrod", new Color(0.722f, 0.525f, 0.0431f));
    public static final Color Peru = Colors.addColor("Peru", new Color(0.804f, 0.522f, 0.247f));
    public static final Color Chocolate = Colors.addColor("Chocolate", new Color(0.824f, 0.412f, 0.118f));
    public static final Color SaddleBrown = Colors.addColor("SaddleBrown", new Color(0.545f, 0.271f, 0.0745f));
    public static final Color Sienna = Colors.addColor("Sienna", new Color(0.627f, 0.322f, 0.176f));
    public static final Color Snow = Colors.addColor("Snow", new Color(1.0f, 0.98f, 0.98f));
    public static final Color HoneyDew = Colors.addColor("HoneyDew", new Color(0.941f, 1.0f, 0.941f));
    public static final Color MintCream = Colors.addColor("MintCream", new Color(0.961f, 1.0f, 0.98f));
    public static final Color Azure = Colors.addColor("Azure", new Color(0.941f, 1.0f, 1.0f));
    public static final Color AliceBlue = Colors.addColor("AliceBlue", new Color(0.941f, 0.973f, 1.0f));
    public static final Color GhostWhite = Colors.addColor("GhostWhite", new Color(0.973f, 0.973f, 1.0f));
    public static final Color WhiteSmoke = Colors.addColor("WhiteSmoke", new Color(0.961f, 0.961f, 0.961f));
    public static final Color SeaShell = Colors.addColor("SeaShell", new Color(1.0f, 0.961f, 0.933f));
    public static final Color Beige = Colors.addColor("Beige", new Color(0.961f, 0.961f, 0.863f));
    public static final Color OldLace = Colors.addColor("OldLace", new Color(0.992f, 0.961f, 0.902f));
    public static final Color FloralWhite = Colors.addColor("FloralWhite", new Color(1.0f, 0.98f, 0.941f));
    public static final Color Ivory = Colors.addColor("Ivory", new Color(1.0f, 1.0f, 0.941f));
    public static final Color AntiqueWhite = Colors.addColor("AntiqueWhite", new Color(0.98f, 0.922f, 0.843f));
    public static final Color Linen = Colors.addColor("Linen", new Color(0.98f, 0.941f, 0.902f));
    public static final Color LavenderBlush = Colors.addColor("LavenderBlush", new Color(1.0f, 0.941f, 0.961f));
    public static final Color MistyRose = Colors.addColor("MistyRose", new Color(1.0f, 0.894f, 0.882f));
    public static final Color Grenadine = Colors.addColor("Grenadine", new Color(0.674f, 0.329f, 0.368f));
    public static final Color Cola = Colors.addColor("Cola", new Color(0.235f, 0.184f, 0.137f));
    public static final Color Ginger = Colors.addColor("Ginger", new Color(0.69f, 0.396f, 0.0f));
    public static final Color FruitPunch = Colors.addColor("FruitPunch", new Color(0.807f, 0.239f, 0.282f));
    public static final Color Fuchsia = Magenta;
    public static final Color Aqua = Cyan;

    private static Color addColor(String name, Color color) {
        ColNfo nfo = new ColNfo(name, color, ColorSet.Standard);
        infoMap.put(name, nfo);
        infoList.add(nfo);
        colors.add(color);
        colorMap.put(name.toLowerCase(), color);
        colorNames.add(name);
        colorSet.add(name.toLowerCase());
        return color;
    }

    public static Color AddGameColor(String name, Color color) {
        ColNfo nfo;
        if (infoMap.containsKey(name)) {
            nfo = infoMap.get(name);
            if (nfo.colorSet == ColorSet.Game) {
                nfo.color.set(color);
                return color;
            }
        }
        nfo = new ColNfo(name, color, ColorSet.Game);
        infoMap.put(name, nfo);
        infoList.add(nfo);
        colorMap.put(name.toLowerCase(), color);
        colorNames.add(name);
        colorSet.add(name.toLowerCase());
        return color;
    }

    public static ColNfo GetColorInfo(String name) {
        return infoMap.get(name);
    }

    public static String getNameFromColor(Color color) {
        for (ColNfo col : infoMap.values()) {
            if (col.color != color) continue;
            return col.name;
        }
        return "";
    }

    public static Color GetRandomColor() {
        return colors.get(Rand.Next(0, colors.size() - 1));
    }

    public static Color GetColorFromIndex(int index) {
        return colors.get(index);
    }

    public static String GetColorNameFromIndex(int index) {
        return colorNames.get(index);
    }

    public static int GetColorsCount() {
        return colors.size();
    }

    public static Color GetColorByName(String name) {
        return colorMap.get(name.toLowerCase());
    }

    public static ArrayList<String> GetColorNames() {
        return colorNames;
    }

    public static boolean ColorExists(String name) {
        return colorSet.contains(name.toLowerCase());
    }

    public static Color CB_GetRandomColor() {
        return CB_colors.get(Rand.Next(0, CB_colors.size() - 1));
    }

    public static Color CB_GetColorFromIndex(int index) {
        return CB_colors.get(index);
    }

    public static String CB_GetColorNameFromIndex(int index) {
        return CB_colorNames.get(index);
    }

    public static int CB_GetColorsCount() {
        return CB_colors.size();
    }

    public static Color CB_GetColorByName(String name) {
        return CB_colorMap.get(name.toLowerCase());
    }

    public static ArrayList<String> CB_GetColorNames() {
        return CB_colorNames;
    }

    public static boolean CB_ColorExists(String name) {
        return CB_colorSet.contains(name.toLowerCase());
    }

    private static Color addColorCB(String name, Color color) {
        ColNfo nfo = new ColNfo(name, color, ColorSet.ColorBlind);
        infoList.add(nfo);
        CB_infoMap.put(name, nfo);
        CB_colors.add(color);
        CB_colorMap.put(name.toLowerCase(), color);
        CB_colorNames.add(name);
        CB_colorSet.add(name.toLowerCase());
        return color;
    }

    @UsedFromLua
    public static class ColNfo {
        private final ColorSet colorSet;
        private final String name;
        private final String hex;
        private final Color color;
        private final float r;
        private final float g;
        private final float b;
        private final int rInt;
        private final int gInt;
        private final int bInt;

        public ColNfo(String name, Color c, ColorSet colorSet) {
            this.colorSet = colorSet;
            this.name = name;
            this.color = c;
            this.r = c.r;
            this.g = c.g;
            this.b = c.b;
            this.rInt = (int)this.r * 255;
            this.gInt = (int)this.g * 255;
            this.bInt = (int)this.b * 255;
            this.hex = String.format("#%02x%02x%02x", this.rInt, this.gInt, this.bInt);
        }

        public ColorSet getColorSet() {
            return this.colorSet;
        }

        public int getColorSetIndex() {
            return this.colorSet.index;
        }

        public String getName() {
            return this.name;
        }

        public String getHex() {
            return this.hex;
        }

        public Color getColor() {
            return this.color;
        }

        public float getR() {
            return this.r;
        }

        public float getG() {
            return this.g;
        }

        public float getB() {
            return this.b;
        }

        public int getRInt() {
            return this.rInt;
        }

        public int getGInt() {
            return this.gInt;
        }

        public int getBInt() {
            return this.bInt;
        }
    }

    @UsedFromLua
    public static enum ColorSet {
        Game(0),
        Standard(1),
        ColorBlind(2);

        final int index;

        private ColorSet(int index) {
            this.index = index;
        }

        public int getIndex() {
            return this.index;
        }
    }
}

