/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Locale;
import zombie.UsedFromLua;
import zombie.core.Translator;
import zombie.core.properties.PropertyContainer;
import zombie.iso.IsoWorld;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;

@UsedFromLua
public class DebugCSVExportMoveableTiles {
    public static void doCSV() throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter("MoveableTilesCSV.txt"));){
            writer.println("TILE ID,UNTRANSLATED TILE NAME,TRANSLATED TILE NAME,ENCUMBRANCE");
            ArrayList<String> allTileNames = IsoWorld.instance.getAllTilesName();
            String previousName = null;
            for (String tileSheet : allTileNames) {
                for (int r = 1; r < 256; ++r) {
                    for (int c = 1; c < 8; ++c) {
                        PropertyContainer props;
                        String tileName = tileSheet + "_" + (c - 1 + (r - 1) * 8);
                        IsoSprite sprite = IsoSpriteManager.instance.getSprite(tileName);
                        if (sprite == null || (props = sprite.getProperties()) == null || !props.has("IsMoveAble")) continue;
                        String untranslatedName = "";
                        String translatedName = "";
                        if (props.has("CustomName")) {
                            untranslatedName = props.get("CustomName");
                            if (props.has("GroupName")) {
                                untranslatedName = String.format(Locale.ENGLISH, "%s %s", props.get("GroupName"), untranslatedName);
                            }
                            if (untranslatedName != null) {
                                translatedName = Translator.getMoveableDisplayName(untranslatedName);
                            }
                        }
                        if (!untranslatedName.equals(previousName)) {
                            String pickUpWeight = props.has("PickUpWeight") ? String.valueOf(Float.parseFloat(props.get("PickUpWeight")) / 10.0f) : "UNDEFINED";
                            writer.println("%s,%s,%s,%s".formatted(tileName, untranslatedName, translatedName, pickUpWeight));
                        }
                        previousName = untranslatedName;
                    }
                }
            }
        }
    }
}

