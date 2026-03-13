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
import zombie.core.skinnedmodel.population.BeardStyle;
import zombie.debug.DebugType;
import zombie.gameStates.ChooseGameInfo;

@XmlRootElement(name="beardStyles")
@UsedFromLua
public class BeardStyles {
    @XmlElement(name="style")
    public final ArrayList<BeardStyle> styles = new ArrayList();
    @XmlTransient
    public static BeardStyles instance;

    public static void init() {
        instance = BeardStyles.Parse(ZomboidFileSystem.instance.base.canonicalFile.getAbsolutePath() + File.separator + ZomboidFileSystem.processFilePath("media/hairStyles/beardStyles.xml", File.separatorChar));
        if (instance == null) {
            return;
        }
        BeardStyles.instance.styles.add(0, new BeardStyle());
        for (String modID : ZomboidFileSystem.instance.getModIDs()) {
            int index;
            BeardStyle style;
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
            if (mod == null) continue;
            String modDir = mod.getVersionDir();
            BeardStyles beardStyles = BeardStyles.Parse(modDir + File.separator + ZomboidFileSystem.processFilePath("media/hairStyles/beardStyles.xml", File.separatorChar));
            if (beardStyles != null) {
                for (BeardStyle styleMod : beardStyles.styles) {
                    style = instance.FindStyle(styleMod.name);
                    if (style == null) {
                        BeardStyles.instance.styles.add(styleMod);
                        continue;
                    }
                    DebugType.Clothing.println("mod \"%s\" overrides beard \"%s\"", modID, styleMod.name);
                    index = BeardStyles.instance.styles.indexOf(style);
                    BeardStyles.instance.styles.set(index, styleMod);
                }
                continue;
            }
            modDir = mod.getCommonDir();
            beardStyles = BeardStyles.Parse(modDir + File.separator + ZomboidFileSystem.processFilePath("media/hairStyles/beardStyles.xml", File.separatorChar));
            if (beardStyles == null) continue;
            for (BeardStyle styleMod : beardStyles.styles) {
                style = instance.FindStyle(styleMod.name);
                if (style == null) {
                    BeardStyles.instance.styles.add(styleMod);
                    continue;
                }
                DebugType.Clothing.println("mod \"%s\" overrides beard \"%s\"", modID, styleMod.name);
                index = BeardStyles.instance.styles.indexOf(style);
                BeardStyles.instance.styles.set(index, styleMod);
            }
        }
    }

    public static void Reset() {
        if (instance == null) {
            return;
        }
        BeardStyles.instance.styles.clear();
        instance = null;
    }

    public static BeardStyles Parse(String filename) {
        try {
            return BeardStyles.parse(filename);
        }
        catch (FileNotFoundException fileNotFoundException) {
        }
        catch (IOException | JAXBException e) {
            ExceptionLogger.logException(e);
        }
        return null;
    }

    public static BeardStyles parse(String filename) throws JAXBException, IOException {
        try (FileInputStream adrFile = new FileInputStream(filename);){
            JAXBContext ctx = JAXBContext.newInstance(BeardStyles.class);
            Unmarshaller um = ctx.createUnmarshaller();
            BeardStyles beardStyles = (BeardStyles)um.unmarshal(adrFile);
            return beardStyles;
        }
    }

    public BeardStyle FindStyle(String name) {
        for (int i = 0; i < this.styles.size(); ++i) {
            BeardStyle style = this.styles.get(i);
            if (!style.name.equalsIgnoreCase(name)) continue;
            return style;
        }
        return null;
    }

    public String getRandomStyle(String outfitName) {
        return HairOutfitDefinitions.instance.getRandomBeard(outfitName, this.styles);
    }

    public BeardStyles getInstance() {
        return instance;
    }

    public ArrayList<BeardStyle> getAllStyles() {
        return this.styles;
    }
}

