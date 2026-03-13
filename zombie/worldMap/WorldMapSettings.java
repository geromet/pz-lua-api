/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap;

import java.util.ArrayList;
import java.util.Objects;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.core.Core;

@UsedFromLua
public final class WorldMapSettings {
    public static final int VERSION1 = 1;
    public static final int VERSION = 1;
    private static WorldMapSettings instance;
    final ArrayList<ConfigOption> options = new ArrayList();
    final WorldMap mWorldMap = new WorldMap(this);
    final MiniMap mMiniMap = new MiniMap(this);
    private int readVersion;

    public static WorldMapSettings getInstance() {
        if (instance == null) {
            instance = new WorldMapSettings();
            instance.load();
        }
        return instance;
    }

    private BooleanConfigOption newOption(String name, boolean defaultValue) {
        BooleanConfigOption option = new BooleanConfigOption(name, defaultValue);
        this.options.add(option);
        return option;
    }

    private DoubleConfigOption newOption(String name, double min, double max, double defaultValue) {
        DoubleConfigOption option = new DoubleConfigOption(name, min, max, defaultValue);
        this.options.add(option);
        return option;
    }

    public ConfigOption getOptionByName(String name) {
        for (int i = 0; i < this.options.size(); ++i) {
            ConfigOption setting = this.options.get(i);
            if (!setting.getName().equals(name)) continue;
            return setting;
        }
        return null;
    }

    public int getOptionCount() {
        return this.options.size();
    }

    public ConfigOption getOptionByIndex(int index) {
        return this.options.get(index);
    }

    public void setBoolean(String name, boolean value) {
        ConfigOption setting = this.getOptionByName(name);
        if (setting instanceof BooleanConfigOption) {
            BooleanConfigOption booleanConfigOption = (BooleanConfigOption)setting;
            booleanConfigOption.setValue(value);
        }
    }

    public boolean getBoolean(String name) {
        ConfigOption setting = this.getOptionByName(name);
        if (setting instanceof BooleanConfigOption) {
            BooleanConfigOption booleanConfigOption = (BooleanConfigOption)setting;
            return booleanConfigOption.getValue();
        }
        return false;
    }

    public void setDouble(String name, double value) {
        ConfigOption setting = this.getOptionByName(name);
        if (setting instanceof DoubleConfigOption) {
            DoubleConfigOption doubleConfigOption = (DoubleConfigOption)setting;
            doubleConfigOption.setValue(value);
        }
    }

    public double getDouble(String name, double defaultValue) {
        ConfigOption setting = this.getOptionByName(name);
        if (setting instanceof DoubleConfigOption) {
            DoubleConfigOption doubleConfigOption = (DoubleConfigOption)setting;
            return doubleConfigOption.getValue();
        }
        return defaultValue;
    }

    public int getFileVersion() {
        return this.readVersion;
    }

    public void save() {
        if (Core.getInstance().isNoSave()) {
            return;
        }
        String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("InGameMap.ini");
        ConfigFile configFile = new ConfigFile();
        configFile.write(fileName, 1, this.options);
        this.readVersion = 1;
    }

    public void load() {
        this.readVersion = 0;
        ConfigFile configFile = new ConfigFile();
        String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("InGameMap.ini");
        if (!configFile.read(fileName)) {
            return;
        }
        this.readVersion = configFile.getVersion();
        if (this.readVersion < 1 || this.readVersion > 1) {
            return;
        }
        for (int i = 0; i < configFile.getOptions().size(); ++i) {
            ConfigOption configOption = configFile.getOptions().get(i);
            try {
                ConfigOption myOption = this.getOptionByName(configOption.getName());
                if (myOption == null) continue;
                myOption.parse(configOption.getValueAsString());
                continue;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    public static void Reset() {
        if (instance == null) {
            return;
        }
        WorldMapSettings.instance.options.clear();
        instance = null;
    }

    public final class WorldMap {
        public DoubleConfigOption centerX;
        public DoubleConfigOption centerY;
        public BooleanConfigOption highlightStreet;
        public BooleanConfigOption isometric;
        public BooleanConfigOption largeStreetLabel;
        public BooleanConfigOption placeNames;
        public BooleanConfigOption players;
        public BooleanConfigOption showPrintMedia;
        public BooleanConfigOption showStreetNames;
        public BooleanConfigOption showSymbolsUi;
        public BooleanConfigOption symbols;
        public BooleanConfigOption terrainImage;
        public DoubleConfigOption zoom;
        final /* synthetic */ WorldMapSettings this$0;

        public WorldMap(WorldMapSettings this$0) {
            WorldMapSettings worldMapSettings = this$0;
            Objects.requireNonNull(worldMapSettings);
            this.this$0 = worldMapSettings;
            this.centerX = this.this$0.newOption("WorldMap.CenterX", -1.7976931348623157E308, Double.MAX_VALUE, 0.0);
            this.centerY = this.this$0.newOption("WorldMap.CenterY", -1.7976931348623157E308, Double.MAX_VALUE, 0.0);
            this.highlightStreet = this.this$0.newOption("WorldMap.HighlightStreet", true);
            this.isometric = this.this$0.newOption("WorldMap.Isometric", true);
            this.largeStreetLabel = this.this$0.newOption("WorldMap.LargeStreetLabel", true);
            this.placeNames = this.this$0.newOption("WorldMap.PlaceNames", true);
            this.players = this.this$0.newOption("WorldMap.Players", true);
            this.showPrintMedia = this.this$0.newOption("WorldMap.ShowPrintMedia", false);
            this.showStreetNames = this.this$0.newOption("WorldMap.ShowStreetNames", true);
            this.showSymbolsUi = this.this$0.newOption("WorldMap.ShowSymbolsUI", true);
            this.symbols = this.this$0.newOption("WorldMap.Symbols", true);
            this.terrainImage = this.this$0.newOption("WorldMap.TerrainImage", false);
            this.zoom = this.this$0.newOption("WorldMap.Zoom", 0.0, 24.0, 0.0);
        }
    }

    public class MiniMap {
        public DoubleConfigOption zoom;
        public BooleanConfigOption isometric;
        public BooleanConfigOption showSymbols;
        public BooleanConfigOption startVisible;
        public BooleanConfigOption terrainImage;
        final /* synthetic */ WorldMapSettings this$0;

        public MiniMap(WorldMapSettings this$0) {
            WorldMapSettings worldMapSettings = this$0;
            Objects.requireNonNull(worldMapSettings);
            this.this$0 = worldMapSettings;
            this.zoom = this.this$0.newOption("MiniMap.Zoom", 0.0, 24.0, 19.0);
            this.isometric = this.this$0.newOption("MiniMap.Isometric", true);
            this.showSymbols = this.this$0.newOption("MiniMap.ShowSymbols", false);
            this.startVisible = this.this$0.newOption("MiniMap.StartVisible", true);
            this.terrainImage = this.this$0.newOption("MiniMap.TerrainImage", false);
        }
    }
}

