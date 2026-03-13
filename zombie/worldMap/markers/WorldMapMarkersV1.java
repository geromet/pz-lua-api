/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.markers;

import java.util.ArrayList;
import java.util.Objects;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.markers.WorldMapGridSquareMarker;
import zombie.worldMap.markers.WorldMapMarker;

@UsedFromLua
public class WorldMapMarkersV1 {
    private final UIWorldMap ui;
    private final ArrayList<WorldMapMarkerV1> markers = new ArrayList();

    public WorldMapMarkersV1(UIWorldMap ui) {
        Objects.requireNonNull(ui);
        this.ui = ui;
    }

    public WorldMapGridSquareMarkerV1 addGridSquareMarker(int worldX, int worldY, int radius, float r, float g, float b, float a) {
        WorldMapGridSquareMarker marker = this.ui.getAPIv1().getMarkers().addGridSquareMarker(worldX, worldY, radius, r, g, b, a);
        WorldMapGridSquareMarkerV1 markerV1 = new WorldMapGridSquareMarkerV1(marker);
        this.markers.add(markerV1);
        return markerV1;
    }

    public void removeMarker(WorldMapMarkerV1 marker) {
        if (!this.markers.remove(marker)) {
            return;
        }
        this.ui.getAPIv1().getMarkers().removeMarker(marker.marker);
    }

    public void clear() {
        this.ui.getAPIv1().getMarkers().clear();
        this.markers.clear();
    }

    public static void setExposed(LuaManager.Exposer exposer) {
        exposer.setExposed(WorldMapMarkersV1.class);
        exposer.setExposed(WorldMapMarkerV1.class);
        exposer.setExposed(WorldMapGridSquareMarkerV1.class);
    }

    @UsedFromLua
    public static final class WorldMapGridSquareMarkerV1
    extends WorldMapMarkerV1 {
        final WorldMapGridSquareMarker gridSquareMarker;

        WorldMapGridSquareMarkerV1(WorldMapGridSquareMarker marker) {
            super(marker);
            this.gridSquareMarker = marker;
        }

        public void setBlink(boolean blink) {
            this.gridSquareMarker.setBlink(blink);
        }

        public void setMinScreenRadius(int pixels) {
            this.gridSquareMarker.setMinScreenRadius(pixels);
        }
    }

    @UsedFromLua
    public static class WorldMapMarkerV1 {
        final WorldMapMarker marker;

        WorldMapMarkerV1(WorldMapMarker marker) {
            this.marker = marker;
        }
    }
}

