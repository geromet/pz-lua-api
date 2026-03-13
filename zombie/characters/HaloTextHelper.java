/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@UsedFromLua
public class HaloTextHelper {
    public static final ColorRGB COLOR_WHITE = new ColorRGB(255, 255, 255);
    public static final ColorRGB COLOR_GREEN = new ColorRGB(137, 232, 148);
    public static final ColorRGB COLOR_RED = new ColorRGB(255, 105, 97);
    private static final String[] queuedLines = new String[4];
    private static final String[] currentLines = new String[4];
    private static boolean ignoreOverheadCheckOnce;

    public static ColorRGB getColorWhite() {
        return COLOR_WHITE;
    }

    public static ColorRGB getColorGreen() {
        return COLOR_GREEN;
    }

    public static ColorRGB getColorRed() {
        return COLOR_RED;
    }

    public static void forceNextAddText() {
        ignoreOverheadCheckOnce = true;
    }

    public static void addTextWithArrow(IsoPlayer player, String text, String separator, boolean arrowIsUp, ColorRGB color) {
        HaloTextHelper.addTextWithArrow(player, text, separator, arrowIsUp, color.r, color.g, color.b, color.r, color.g, color.b);
    }

    public static void addTextWithArrow(IsoPlayer player, String text, String separator, boolean arrowIsUp, int r, int g, int b) {
        HaloTextHelper.addTextWithArrow(player, text, separator, arrowIsUp, r, g, b, r, g, b);
    }

    public static void addTextWithArrow(IsoPlayer player, String text, String separator, boolean arrowIsUp, ColorRGB color, ColorRGB arrowColor) {
        HaloTextHelper.addTextWithArrow(player, text, separator, arrowIsUp, color.r, color.g, color.b, arrowColor.r, arrowColor.g, arrowColor.b);
    }

    public static void addTextWithArrow(IsoPlayer player, String text, String separator, boolean arrowIsUp, int r, int g, int b, int aR, int aG, int aB) {
        HaloTextHelper.addText(player, "[col=" + r + "," + g + "," + b + "]" + text + "[/] [img=media/ui/" + (arrowIsUp ? "ArrowUp.png" : "ArrowDown.png") + "," + aR + "," + aG + "," + aB + "]", separator);
    }

    public static void addTextWithArrow(IsoPlayer player, String text, boolean arrowIsUp, ColorRGB color) {
        HaloTextHelper.addTextWithArrow(player, text, "[col=175,175,175], [/]", arrowIsUp, color.r, color.g, color.b, color.r, color.g, color.b);
    }

    public static void addTextWithArrow(IsoPlayer player, String text, boolean arrowIsUp, int r, int g, int b) {
        HaloTextHelper.addTextWithArrow(player, text, "[col=175,175,175], [/]", arrowIsUp, r, g, b, r, g, b);
    }

    public static void addTextWithArrow(IsoPlayer player, String text, boolean arrowIsUp, ColorRGB color, ColorRGB arrowColor) {
        HaloTextHelper.addTextWithArrow(player, text, "[col=175,175,175], [/]", arrowIsUp, color.r, color.g, color.b, arrowColor.r, arrowColor.g, arrowColor.b);
    }

    public static void addTextWithArrow(IsoPlayer player, String text, boolean arrowIsUp, int r, int g, int b, int aR, int aG, int aB) {
        HaloTextHelper.addText(player, "[col=" + r + "," + g + "," + b + "]" + text + "[/] [img=media/ui/" + (arrowIsUp ? "ArrowUp.png" : "ArrowDown.png") + "," + aR + "," + aG + "," + aB + "]", "[col=175,175,175], [/]");
    }

    public static void addText(IsoPlayer player, String text, String seperator, ColorRGB color) {
        HaloTextHelper.addText(player, text, seperator, color.r, color.g, color.b);
    }

    public static void addText(IsoPlayer player, String text, String separator, int r, int g, int b) {
        HaloTextHelper.addText(player, "[col=" + r + "," + g + "," + b + "]" + text + "[/]", separator);
    }

    public static ColorRGB getGoodColor() {
        int r = (int)Core.getInstance().getGoodHighlitedColor().getR() * 255;
        int g = (int)Core.getInstance().getGoodHighlitedColor().getG() * 255;
        int b = (int)Core.getInstance().getGoodHighlitedColor().getB() * 255;
        return new ColorRGB(r, g, b);
    }

    public static ColorRGB getBadColor() {
        int r = (int)Core.getInstance().getBadHighlitedColor().getR() * 255;
        int g = (int)Core.getInstance().getBadHighlitedColor().getG() * 255;
        int b = (int)Core.getInstance().getBadHighlitedColor().getB() * 255;
        return new ColorRGB(r, g, b);
    }

    public static void addGoodText(IsoPlayer player, String text) {
        HaloTextHelper.addGoodText(player, text, "[col=175,175,175], [/]");
    }

    public static void addGoodText(IsoPlayer player, String text, String separator) {
        HaloTextHelper.addText(player, text, separator, HaloTextHelper.getGoodColor());
    }

    public static void addBadText(IsoPlayer player, String text) {
        HaloTextHelper.addBadText(player, text, "[col=175,175,175], [/]");
    }

    public static void addBadText(IsoPlayer player, String text, String separator) {
        HaloTextHelper.addText(player, text, separator, HaloTextHelper.getBadColor());
    }

    public static void addText(IsoPlayer player, String text) {
        HaloTextHelper.addText(player, text, "[col=175,175,175], [/]");
    }

    public static void addText(IsoPlayer player, String text, String separator) {
        int num = player.getPlayerNum();
        if (HaloTextHelper.overheadContains(num, text)) {
            return;
        }
        Object haloStr = queuedLines[num];
        if (haloStr == null) {
            haloStr = text;
        } else {
            if (((String)haloStr).contains(text)) {
                return;
            }
            haloStr = (String)haloStr + separator + text;
        }
        HaloTextHelper.queuedLines[num] = haloStr;
        if (GameServer.server) {
            INetworkPacket.send(player, PacketTypes.PacketType.HaloText, player, text, separator);
        }
    }

    private static boolean overheadContains(int num, String text) {
        if (ignoreOverheadCheckOnce) {
            ignoreOverheadCheckOnce = false;
            return false;
        }
        return currentLines[num] != null && currentLines[num].contains(text);
    }

    public static void update() {
        for (int i = 0; i < 4; ++i) {
            IsoPlayer plr = IsoPlayer.players[i];
            if (plr != null) {
                if (currentLines[i] != null && plr.getHaloTimerCount() <= 0.2f * GameTime.getInstance().getMultiplier()) {
                    HaloTextHelper.currentLines[i] = null;
                }
                if (queuedLines[i] == null || !(plr.getHaloTimerCount() <= 0.2f * GameTime.getInstance().getMultiplier())) continue;
                plr.setHaloNote(queuedLines[i]);
                HaloTextHelper.currentLines[i] = queuedLines[i];
                HaloTextHelper.queuedLines[i] = null;
                continue;
            }
            if (queuedLines[i] != null) {
                HaloTextHelper.queuedLines[i] = null;
            }
            if (currentLines[i] == null) continue;
            HaloTextHelper.currentLines[i] = null;
        }
    }

    @UsedFromLua
    public static class ColorRGB {
        public int r;
        public int g;
        public int b;
        public int a = 255;

        public ColorRGB(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }
}

