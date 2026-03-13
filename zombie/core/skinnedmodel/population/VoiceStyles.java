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
import zombie.core.logger.ExceptionLogger;
import zombie.core.skinnedmodel.population.VoiceStyle;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.gameStates.ChooseGameInfo;

@XmlRootElement(name="voiceStyles")
@UsedFromLua
public class VoiceStyles {
    @XmlElement(name="style")
    public final ArrayList<VoiceStyle> styles = new ArrayList();
    @XmlTransient
    public static VoiceStyles instance;

    public static void init() {
        instance = VoiceStyles.Parse(ZomboidFileSystem.instance.base.canonicalFile.getAbsolutePath() + File.separator + ZomboidFileSystem.processFilePath("media/voiceStyles/voiceStyles.xml", File.separatorChar));
        if (instance == null) {
            return;
        }
        VoiceStyles.instance.styles.add(0, new VoiceStyle());
        for (String modID : ZomboidFileSystem.instance.getModIDs()) {
            int index;
            VoiceStyle style;
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
            if (mod == null) continue;
            String modDir = mod.getVersionDir();
            VoiceStyles voiceStyles = VoiceStyles.Parse(modDir + File.separator + ZomboidFileSystem.processFilePath("media/voiceStyles/voiceStyles.xml", File.separatorChar));
            if (voiceStyles != null) {
                for (VoiceStyle styleMod : voiceStyles.styles) {
                    style = instance.FindStyle(styleMod.prefix);
                    if (style == null) {
                        VoiceStyles.instance.styles.add(styleMod);
                        continue;
                    }
                    if (DebugLog.isEnabled(DebugType.Sound)) {
                        DebugLog.Sound.println("mod \"%s\" overrides voice \"%s\"", modID, styleMod.prefix);
                    }
                    index = VoiceStyles.instance.styles.indexOf(style);
                    VoiceStyles.instance.styles.set(index, styleMod);
                }
                continue;
            }
            modDir = mod.getCommonDir();
            voiceStyles = VoiceStyles.Parse(modDir + File.separator + ZomboidFileSystem.processFilePath("media/voiceStyles/voiceStyles.xml", File.separatorChar));
            if (voiceStyles == null) continue;
            for (VoiceStyle styleMod : voiceStyles.styles) {
                style = instance.FindStyle(styleMod.prefix);
                if (style == null) {
                    VoiceStyles.instance.styles.add(styleMod);
                    continue;
                }
                if (DebugLog.isEnabled(DebugType.Sound)) {
                    DebugLog.Sound.println("mod \"%s\" overrides voice \"%s\"", modID, styleMod.prefix);
                }
                index = VoiceStyles.instance.styles.indexOf(style);
                VoiceStyles.instance.styles.set(index, styleMod);
            }
        }
    }

    public static void Reset() {
        if (instance == null) {
            return;
        }
        VoiceStyles.instance.styles.clear();
        instance = null;
    }

    public static VoiceStyles Parse(String filename) {
        try {
            return VoiceStyles.parse(filename);
        }
        catch (FileNotFoundException fileNotFoundException) {
        }
        catch (IOException | JAXBException e) {
            ExceptionLogger.logException(e);
        }
        return null;
    }

    public static VoiceStyles parse(String filename) throws JAXBException, IOException {
        try (FileInputStream adrFile = new FileInputStream(filename);){
            JAXBContext ctx = JAXBContext.newInstance(VoiceStyles.class);
            Unmarshaller um = ctx.createUnmarshaller();
            VoiceStyles voiceStyles = (VoiceStyles)um.unmarshal(adrFile);
            return voiceStyles;
        }
    }

    public VoiceStyle FindStyle(String name) {
        for (int i = 0; i < this.styles.size(); ++i) {
            VoiceStyle style = this.styles.get(i);
            if (!style.prefix.equalsIgnoreCase(name)) continue;
            return style;
        }
        return null;
    }

    public VoiceStyles getInstance() {
        return instance;
    }

    public ArrayList<VoiceStyle> getAllStyles() {
        return this.styles;
    }
}

