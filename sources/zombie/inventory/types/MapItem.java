/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory.types;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.logger.ExceptionLogger;
import zombie.inventory.InventoryItem;
import zombie.iso.SaveBufferMap;
import zombie.iso.SliceY;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.util.ByteBufferPooledObject;
import zombie.worldMap.symbols.WorldMapSymbols;

@UsedFromLua
public class MapItem
extends InventoryItem {
    public static MapItem worldMapInstance;
    private static final byte[] FILE_MAGIC;
    private String mapId;
    private final WorldMapSymbols symbols = new WorldMapSymbols();
    private boolean defaultAnnotationsLoaded;

    public static MapItem getSingleton() {
        if (worldMapInstance == null) {
            Item scriptItem = ScriptManager.instance.FindItem("Base.Map");
            if (scriptItem == null) {
                return null;
            }
            worldMapInstance = new MapItem("Base", "World Map", "WorldMap", scriptItem);
        }
        return worldMapInstance;
    }

    public static void SaveWorldMap() {
        if (worldMapInstance == null) {
            return;
        }
        try {
            ByteBuffer out = SliceY.SliceBuffer;
            out.clear();
            out.put(FILE_MAGIC);
            out.putInt(244);
            worldMapInstance.getSymbols().save(out);
            File file = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("map_symbols.bin"));
            try (FileOutputStream fos = new FileOutputStream(file);
                 BufferedOutputStream bos = new BufferedOutputStream(fos);){
                bos.write(out.array(), 0, out.position());
            }
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void SaveWorldMapToBufferMap(SaveBufferMap bufferMap) {
        if (worldMapInstance == null) {
            return;
        }
        Object object = SliceY.SliceBufferLock;
        synchronized (object) {
            try {
                SliceY.SliceBuffer.clear();
                SliceY.SliceBuffer.put(FILE_MAGIC);
                SliceY.SliceBuffer.putInt(244);
                worldMapInstance.getSymbols().save(SliceY.SliceBuffer);
                String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("map_symbols.bin");
                ByteBufferPooledObject buffer = bufferMap.allocate(SliceY.SliceBuffer.position());
                buffer.put(SliceY.SliceBuffer.array(), 0, SliceY.SliceBuffer.position());
                bufferMap.put(fileName, buffer);
            }
            catch (Exception ex) {
                ExceptionLogger.logException(ex);
            }
        }
    }

    public static void LoadWorldMap() {
        if (MapItem.getSingleton() == null) {
            return;
        }
        File file = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("map_symbols.bin"));
        try (FileInputStream fis2 = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis2);){
            ByteBuffer in = SliceY.SliceBuffer;
            in.clear();
            int numBytes = bis.read(in.array());
            in.limit(numBytes);
            byte[] magic = new byte[4];
            in.get(magic);
            if (!Arrays.equals(magic, FILE_MAGIC)) {
                throw new IOException(file.getAbsolutePath() + " does not appear to be map_symbols.bin");
            }
            int worldVersion = in.getInt();
            MapItem.getSingleton().getSymbols().load(in, worldVersion);
        }
        catch (FileNotFoundException fis2) {
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
    }

    public static void Reset() {
        if (worldMapInstance == null) {
            return;
        }
        worldMapInstance.getSymbols().clear();
        worldMapInstance = null;
    }

    public MapItem(String module, String name, String type, String tex) {
        super(module, name, type, tex);
    }

    public MapItem(String module, String name, String type, Item item) {
        super(module, name, type, item);
    }

    @Override
    public boolean IsMap() {
        return true;
    }

    public void setMapID(String mapID) {
        this.mapId = mapID;
    }

    public String getMapID() {
        return this.mapId;
    }

    public WorldMapSymbols getSymbols() {
        return this.symbols;
    }

    @Override
    public void save(ByteBuffer output, boolean net) throws IOException {
        super.save(output, net);
        GameWindow.WriteString(output, this.mapId);
        this.symbols.save(output);
    }

    @Override
    public void load(ByteBuffer input, int worldVersion) throws IOException {
        super.load(input, worldVersion);
        this.mapId = GameWindow.ReadString(input);
        this.symbols.load(input, worldVersion);
    }

    public boolean checkDefaultAnnotationsLoaded() {
        if (this.defaultAnnotationsLoaded) {
            return false;
        }
        this.defaultAnnotationsLoaded = true;
        return true;
    }

    public void clearDefaultAnnotations() {
        this.defaultAnnotationsLoaded = false;
        this.symbols.clearDefaultAnnotations();
    }

    public String getMediaId() {
        return this.getStashMap() != null ? this.getStashMap() : this.getMapID();
    }

    static {
        FILE_MAGIC = new byte[]{87, 77, 83, 89};
    }
}

