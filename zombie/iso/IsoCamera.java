/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.characters.IsoDummyCameraCharacter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.iso.IsoGridSquare;
import zombie.iso.PlayerCamera;
import zombie.iso.areas.IsoRoom;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.ui.MoodlesUI;
import zombie.ui.UIManager;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public class IsoCamera {
    public static final FrameState frameState = new FrameState();
    public static final PlayerCamera[] cameras = new PlayerCamera[4];
    private static IsoGameCharacter isoCameraGameCharacter;
    private static final int TargetTileX = 0;
    private static int targetTileY;
    public static int playerOffsetX;
    public static int playerOffsetY;

    public static void init() {
        playerOffsetY = -56 / (2 / Core.tileScale);
    }

    public static void update() {
        int playerIndex = IsoPlayer.getPlayerIndex();
        cameras[playerIndex].update();
    }

    public static void updateAll() {
        for (int pn = 0; pn < 4; ++pn) {
            IsoPlayer player = IsoPlayer.players[pn];
            if (player == null) continue;
            IsoCamera.setCameraCharacter(player);
            cameras[pn].update();
        }
    }

    public static void SetCharacterToFollow(IsoGameCharacter isoGameCharacter) {
        IsoPlayer isoPlayer;
        IsoGameCharacter isoGameCharacter2;
        if (!GameClient.client && !GameServer.server && (isoGameCharacter2 = (isoCameraGameCharacter = isoGameCharacter)) instanceof IsoPlayer && (isoPlayer = (IsoPlayer)isoGameCharacter2).isLocalPlayer() && UIManager.getMoodleUI(isoPlayer.getPlayerNum()) != null) {
            int pn = isoPlayer.getPlayerNum();
            UIManager.getUI().remove(UIManager.getMoodleUI(pn));
            UIManager.setMoodleUI(pn, new MoodlesUI());
            UIManager.getMoodleUI(pn).setCharacter(isoCameraGameCharacter);
            UIManager.getUI().add(UIManager.getMoodleUI(pn));
        }
    }

    public static float getRightClickOffX() {
        return (int)IsoCamera.cameras[IsoPlayer.getPlayerIndex()].rightClickX;
    }

    public static float getRightClickOffY() {
        return (int)IsoCamera.cameras[IsoPlayer.getPlayerIndex()].rightClickY;
    }

    public static float getOffX() {
        return cameras[IsoPlayer.getPlayerIndex()].getOffX();
    }

    public static float getTOffX() {
        return cameras[IsoPlayer.getPlayerIndex()].getTOffX();
    }

    public static void setOffX(float aOffX) {
        IsoCamera.cameras[IsoPlayer.getPlayerIndex()].offX = aOffX;
    }

    public static float getOffY() {
        return cameras[IsoPlayer.getPlayerIndex()].getOffY();
    }

    public static float getTOffY() {
        return cameras[IsoPlayer.getPlayerIndex()].getTOffY();
    }

    public static void setOffY(float aOffY) {
        IsoCamera.cameras[IsoPlayer.getPlayerIndex()].offY = aOffY;
    }

    public static float getLastOffX() {
        return cameras[IsoPlayer.getPlayerIndex()].getLastOffX();
    }

    public static void setLastOffX(float aLastOffX) {
        IsoCamera.cameras[IsoPlayer.getPlayerIndex()].lastOffX = aLastOffX;
    }

    public static float getLastOffY() {
        return cameras[IsoPlayer.getPlayerIndex()].getLastOffY();
    }

    public static void setLastOffY(float aLastOffY) {
        IsoCamera.cameras[IsoPlayer.getPlayerIndex()].lastOffY = aLastOffY;
    }

    public static IsoGameCharacter getCameraCharacter() {
        return isoCameraGameCharacter;
    }

    public static float getCameraCharacterZ() {
        return isoCameraGameCharacter.getZ();
    }

    public static boolean setCameraCharacter(IsoGameCharacter isoGameCharacter) {
        if (isoGameCharacter instanceof IsoDummyCameraCharacter) {
            return false;
        }
        isoCameraGameCharacter = isoGameCharacter;
        return true;
    }

    public static void clearCameraCharacter() {
        isoCameraGameCharacter = null;
    }

    public static int getTargetTileY() {
        return targetTileY;
    }

    public static void setTargetTileY(int aTargetTileY) {
        targetTileY = aTargetTileY;
    }

    public static int getScreenLeft(int playerIndex) {
        if (playerIndex == 1 || playerIndex == 3) {
            return Core.getInstance().getScreenWidth() / 2;
        }
        return 0;
    }

    public static int getScreenWidth(int playerIndex) {
        if (IsoPlayer.numPlayers > 1) {
            return Core.getInstance().getScreenWidth() / 2;
        }
        return Core.getInstance().getScreenWidth();
    }

    public static int getScreenTop(int playerIndex) {
        if (playerIndex == 2 || playerIndex == 3) {
            return Core.getInstance().getScreenHeight() / 2;
        }
        return 0;
    }

    public static int getScreenHeight(int playerIndex) {
        if (IsoPlayer.numPlayers > 2) {
            return Core.getInstance().getScreenHeight() / 2;
        }
        return Core.getInstance().getScreenHeight();
    }

    public static int getOffscreenLeft(int playerIndex) {
        if (playerIndex == 1 || playerIndex == 3) {
            return Core.getInstance().getScreenWidth() / 2;
        }
        return 0;
    }

    public static int getOffscreenWidth(int playerIndex) {
        return Core.getInstance().getOffscreenWidth(playerIndex);
    }

    public static int getOffscreenTop(int playerIndex) {
        if (playerIndex >= 2) {
            return Core.getInstance().getScreenHeight() / 2;
        }
        return 0;
    }

    public static int getOffscreenHeight(int playerIndex) {
        return Core.getInstance().getOffscreenHeight(playerIndex);
    }

    static {
        for (int i = 0; i < cameras.length; ++i) {
            IsoCamera.cameras[i] = new PlayerCamera(i);
        }
        playerOffsetY = -56 / (2 / Core.tileScale);
    }

    public static final class FrameState {
        public int frameCount;
        public float unPausedAccumulator;
        public boolean paused;
        public int playerIndex;
        public float camCharacterX;
        public float camCharacterY;
        public float camCharacterZ;
        public IsoGameCharacter camCharacter;
        public IsoGridSquare camCharacterSquare;
        public IsoRoom camCharacterRoom;
        public float offX;
        public float offY;
        public int offscreenWidth;
        public int offscreenHeight;
        public float zoom;

        public void set(int playerIndex) {
            this.paused = GameTime.isGamePaused();
            this.playerIndex = playerIndex;
            this.camCharacter = IsoPlayer.players[playerIndex];
            this.camCharacterX = this.camCharacter.getX();
            this.camCharacterY = this.camCharacter.getY();
            this.camCharacterZ = this.calculateCameraZ(this.camCharacter);
            this.camCharacterSquare = this.camCharacter.getCurrentSquare();
            this.camCharacterRoom = this.camCharacterSquare == null ? null : this.camCharacterSquare.getRoom();
            this.offX = IsoCamera.getOffX();
            this.offY = IsoCamera.getOffY();
            this.offscreenWidth = IsoCamera.getOffscreenWidth(playerIndex);
            this.offscreenHeight = IsoCamera.getOffscreenHeight(playerIndex);
            this.zoom = Core.getInstance().getZoom(playerIndex);
        }

        public float calculateCameraZ(IsoGameCharacter character) {
            if (character == null) {
                return 0.0f;
            }
            BaseVehicle vehicle = character.getVehicle();
            if (vehicle == null) {
                return character.getZ();
            }
            return vehicle.jniTransform.origin.y / 2.44949f;
        }

        public void updateUnPausedAccumulator() {
            if (GameTime.isGamePaused()) {
                return;
            }
            this.unPausedAccumulator += GameTime.getInstance().getMultiplier();
            if (Float.compare(this.unPausedAccumulator, Float.MAX_VALUE) >= 0) {
                this.unPausedAccumulator = 0.0f;
            }
        }
    }
}

