/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import gnu.trove.map.hash.THashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.UsedFromLua;
import zombie.core.textures.Texture;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoFeedingTrough;
import zombie.iso.objects.IsoStove;
import zombie.network.GameServer;
import zombie.util.LocationRNG;
import zombie.util.StringUtils;

@UsedFromLua
public class ContainerOverlays {
    public static final ContainerOverlays instance = new ContainerOverlays();
    private static final ArrayList<ContainerOverlayEntry> tempEntries = new ArrayList();
    private final THashMap<String, ContainerOverlay> overlayMap = new THashMap();
    private final HashMap<String, ArrayList<String>> overlayNameToUnderlyingName = new HashMap();

    private void parseContainerOverlayMapV0(KahluaTableImpl overlayMapTable) {
        for (Map.Entry<Object, Object> overlaySet : overlayMapTable.delegate.entrySet()) {
            String key = overlaySet.getKey().toString();
            ContainerOverlay o = new ContainerOverlay();
            o.name = key;
            this.overlayMap.put(o.name, o);
            KahluaTableImpl entries = (KahluaTableImpl)overlaySet.getValue();
            for (Map.Entry<Object, Object> entrySet : entries.delegate.entrySet()) {
                String room = entrySet.getKey().toString();
                KahluaTableImpl tiles = (KahluaTableImpl)entrySet.getValue();
                String a = null;
                if (tiles.delegate.containsKey(1.0)) {
                    a = tiles.rawget(1.0).toString();
                }
                String b = null;
                if (tiles.delegate.containsKey(2.0)) {
                    b = tiles.rawget(2.0).toString();
                }
                ContainerOverlayEntry e = new ContainerOverlayEntry();
                e.manyItems = a;
                e.fewItems = b;
                e.room = room;
                o.entries.add(e);
            }
        }
    }

    private void parseContainerOverlayMapV1(KahluaTableImpl overlaysTable) {
        KahluaTableIterator overlaysIter = overlaysTable.iterator();
        while (overlaysIter.advance()) {
            String key = overlaysIter.getKey().toString();
            if ("VERSION".equalsIgnoreCase(key)) continue;
            ContainerOverlay o = new ContainerOverlay();
            o.name = key;
            KahluaTableImpl overlayTable = (KahluaTableImpl)overlaysIter.getValue();
            KahluaTableIterator roomIter = overlayTable.iterator();
            while (roomIter.advance()) {
                KahluaTableImpl roomTable = (KahluaTableImpl)roomIter.getValue();
                String room = roomTable.rawgetStr("name");
                KahluaTableImpl tileTable = (KahluaTableImpl)roomTable.rawget("tiles");
                ContainerOverlayEntry e = new ContainerOverlayEntry();
                e.manyItems = (String)tileTable.rawget(1);
                e.fewItems = (String)tileTable.rawget(2);
                if (StringUtils.isNullOrWhitespace(e.manyItems) || "none".equalsIgnoreCase(e.manyItems)) {
                    e.manyItems = null;
                }
                if (StringUtils.isNullOrWhitespace(e.fewItems) || "none".equalsIgnoreCase(e.fewItems)) {
                    e.fewItems = null;
                }
                e.room = room;
                o.entries.add(e);
            }
            this.overlayMap.put(o.name, o);
            for (ContainerOverlayEntry e : o.entries) {
                ArrayList<String> underlyingNames;
                String overlayName = e.fewItems;
                if (overlayName != null) {
                    underlyingNames = this.overlayNameToUnderlyingName.get(overlayName);
                    if (underlyingNames == null) {
                        underlyingNames = new ArrayList();
                        this.overlayNameToUnderlyingName.put(overlayName, underlyingNames);
                    }
                    if (!underlyingNames.contains(o.name)) {
                        underlyingNames.add(o.name);
                    }
                }
                if ((overlayName = e.manyItems) == null) continue;
                underlyingNames = this.overlayNameToUnderlyingName.get(overlayName);
                if (underlyingNames == null) {
                    underlyingNames = new ArrayList();
                    this.overlayNameToUnderlyingName.put(overlayName, underlyingNames);
                }
                if (underlyingNames.contains(o.name)) continue;
                underlyingNames.add(o.name);
            }
        }
    }

    public void addOverlays(KahluaTableImpl overlayMap) {
        int version = overlayMap.rawgetInt("VERSION");
        if (version == -1) {
            this.parseContainerOverlayMapV0(overlayMap);
        } else if (version == 1) {
            this.parseContainerOverlayMapV1(overlayMap);
        } else {
            throw new RuntimeException("unknown overlayMap.VERSION " + version);
        }
    }

    public boolean hasOverlays(IsoObject obj) {
        return obj != null && obj.sprite != null && obj.sprite.name != null && this.overlayMap.containsKey(obj.sprite.name);
    }

    public ArrayList<String> getUnderlyingSpriteNames(String overlayName) {
        return this.overlayNameToUnderlyingName.get(overlayName);
    }

    public void updateContainerOverlaySprite(IsoObject obj) {
        ContainerOverlay overlay;
        if (obj == null) {
            return;
        }
        if (obj instanceof IsoStove) {
            return;
        }
        if (obj instanceof IsoFeedingTrough) {
            return;
        }
        IsoGridSquare sq = obj.getSquare();
        if (sq == null) {
            return;
        }
        String overlaySpriteName = null;
        ItemContainer container = obj.getContainer();
        if (obj.sprite != null && obj.sprite.name != null && container != null && container.getItems() != null && !container.isEmpty() && (overlay = this.overlayMap.get(obj.sprite.name)) != null) {
            ContainerOverlayEntry entry;
            String room = "other";
            if (sq.getRoom() != null) {
                room = sq.getRoom().getName();
            }
            if ((entry = overlay.pickRandom(room, sq.x, sq.y, sq.z)) == null) {
                entry = overlay.pickRandom("other", sq.x, sq.y, sq.z);
            }
            if (entry != null) {
                overlaySpriteName = entry.manyItems;
                if (entry.fewItems != null && container.getItems().size() < 7) {
                    overlaySpriteName = entry.fewItems;
                }
            }
        }
        if (!StringUtils.isNullOrWhitespace(overlaySpriteName) && !GameServer.server && Texture.getSharedTexture(overlaySpriteName) == null) {
            overlaySpriteName = null;
        }
        obj.setOverlaySprite(overlaySpriteName);
    }

    public void Reset() {
        this.overlayMap.clear();
        this.overlayNameToUnderlyingName.clear();
    }

    private static final class ContainerOverlay {
        public String name;
        public final ArrayList<ContainerOverlayEntry> entries = new ArrayList();

        private ContainerOverlay() {
        }

        public void getEntries(String room, ArrayList<ContainerOverlayEntry> out) {
            out.clear();
            for (int i = 0; i < this.entries.size(); ++i) {
                ContainerOverlayEntry entry = this.entries.get(i);
                if (!entry.room.equalsIgnoreCase(room)) continue;
                out.add(entry);
            }
        }

        public ContainerOverlayEntry pickRandom(String room, int x, int y, int z) {
            this.getEntries(room, tempEntries);
            if (tempEntries.isEmpty()) {
                return null;
            }
            int n = LocationRNG.instance.nextInt(tempEntries.size(), x, y, z);
            return tempEntries.get(n);
        }
    }

    private static final class ContainerOverlayEntry {
        public String room;
        public String manyItems;
        public String fewItems;

        private ContainerOverlayEntry() {
        }
    }
}

