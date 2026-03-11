/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.population;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.HairOutfitDefinitions;
import zombie.core.logger.ExceptionLogger;
import zombie.core.skinnedmodel.population.HairStyle;
import zombie.debug.DebugType;
import zombie.gameStates.ChooseGameInfo;

@XmlRootElement(name="hairStyles")
@UsedFromLua
public class HairStyles {
    @XmlElement(name="male")
    public final ArrayList<HairStyle> maleStyles = new ArrayList();
    @XmlElement(name="female")
    public final ArrayList<HairStyle> femaleStyles = new ArrayList();
    @XmlTransient
    public static HairStyles instance;

    public static void init() {
        instance = HairStyles.Parse(ZomboidFileSystem.instance.base.canonicalFile.getAbsolutePath() + File.separator + ZomboidFileSystem.processFilePath("media/hairStyles/hairStyles.xml", File.separatorChar));
        if (instance == null) {
            return;
        }
        for (String modID : ZomboidFileSystem.instance.getModIDs()) {
            int index;
            HairStyle style;
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
            if (mod == null) continue;
            String modDir = mod.getVersionDir();
            HairStyles hairStyles = HairStyles.Parse(modDir + File.separator + ZomboidFileSystem.processFilePath("media/hairStyles/hairStyles.xml", File.separatorChar));
            if (hairStyles != null) {
                for (HairStyle styleMod : hairStyles.femaleStyles) {
                    style = instance.FindFemaleStyle(styleMod.name);
                    if (style == null) {
                        HairStyles.instance.femaleStyles.add(styleMod);
                        continue;
                    }
                    DebugType.Clothing.println("mod \"%s\" overrides hair \"%s\"", modID, styleMod.name);
                    index = HairStyles.instance.femaleStyles.indexOf(style);
                    HairStyles.instance.femaleStyles.set(index, styleMod);
                }
                for (HairStyle styleMod : hairStyles.maleStyles) {
                    style = instance.FindMaleStyle(styleMod.name);
                    if (style == null) {
                        HairStyles.instance.maleStyles.add(styleMod);
                        continue;
                    }
                    DebugType.Clothing.println("mod \"%s\" overrides hair \"%s\"", modID, styleMod.name);
                    index = HairStyles.instance.maleStyles.indexOf(style);
                    HairStyles.instance.maleStyles.set(index, styleMod);
                }
                continue;
            }
            modDir = mod.getCommonDir();
            hairStyles = HairStyles.Parse(modDir + File.separator + ZomboidFileSystem.processFilePath("media/hairStyles/hairStyles.xml", File.separatorChar));
            if (hairStyles == null) continue;
            for (HairStyle styleMod : hairStyles.femaleStyles) {
                style = instance.FindFemaleStyle(styleMod.name);
                if (style == null) {
                    HairStyles.instance.femaleStyles.add(styleMod);
                    continue;
                }
                DebugType.Clothing.println("mod \"%s\" overrides hair \"%s\"", modID, styleMod.name);
                index = HairStyles.instance.femaleStyles.indexOf(style);
                HairStyles.instance.femaleStyles.set(index, styleMod);
            }
            for (HairStyle styleMod : hairStyles.maleStyles) {
                style = instance.FindMaleStyle(styleMod.name);
                if (style == null) {
                    HairStyles.instance.maleStyles.add(styleMod);
                    continue;
                }
                DebugType.Clothing.println("mod \"%s\" overrides hair \"%s\"", modID, styleMod.name);
                index = HairStyles.instance.maleStyles.indexOf(style);
                HairStyles.instance.maleStyles.set(index, styleMod);
            }
        }
    }

    public static void Reset() {
        if (instance == null) {
            return;
        }
        HairStyles.instance.femaleStyles.clear();
        HairStyles.instance.maleStyles.clear();
        instance = null;
    }

    public static HairStyles Parse(String filename) {
        try {
            return HairStyles.parse(filename);
        }
        catch (FileNotFoundException fileNotFoundException) {
        }
        catch (IOException | JAXBException e) {
            ExceptionLogger.logException(e);
        }
        return null;
    }

    public static HairStyles parse(String filename) throws JAXBException, IOException {
        try (FileInputStream adrFile = new FileInputStream(filename);){
            JAXBContext ctx = JAXBContext.newInstance(HairStyles.class);
            Unmarshaller um = ctx.createUnmarshaller();
            HairStyles hairStyles = (HairStyles)um.unmarshal(adrFile);
            return hairStyles;
        }
    }

    public HairStyle FindMaleStyle(String name) {
        return this.FindStyle(this.maleStyles, name);
    }

    public HairStyle FindFemaleStyle(String name) {
        return this.FindStyle(this.femaleStyles, name);
    }

    private HairStyle FindStyle(ArrayList<HairStyle> list, String name) {
        for (int i = 0; i < list.size(); ++i) {
            HairStyle style = list.get(i);
            if (style.name.equalsIgnoreCase(name)) {
                return style;
            }
            if (!"".equals(name) || !style.name.equalsIgnoreCase("bald")) continue;
            return style;
        }
        return null;
    }

    public String getRandomMaleStyle(String outfitName) {
        return HairOutfitDefinitions.instance.getRandomMaleHaircut(outfitName, this.maleStyles);
    }

    public String getRandomFemaleStyle(String outfitName) {
        return HairOutfitDefinitions.instance.getRandomFemaleHaircut(outfitName, this.femaleStyles);
    }

    public HairStyle getAlternateForHat(HairStyle style, String category) {
        if ("nohair".equalsIgnoreCase(category) || "nohairnobeard".equalsIgnoreCase(category)) {
            return null;
        }
        if (this.femaleStyles.contains(style)) {
            return this.FindFemaleStyle(style.getAlternate(category));
        }
        if (this.maleStyles.contains(style)) {
            return this.FindMaleStyle(style.getAlternate(category));
        }
        return style;
    }

    public ArrayList<HairStyle> getAllMaleStyles() {
        return this.maleStyles;
    }

    public ArrayList<HairStyle> getAllFemaleStyles() {
        return this.femaleStyles;
    }
}

