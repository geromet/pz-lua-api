/*
 * Decompiled with CFR 0.152.
 */
package zombie.radio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import zombie.GameTime;
import zombie.Lua.LuaEventManager;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoPlayer;
import zombie.characters.Roles;
import zombie.chat.ChatElement;
import zombie.chat.ChatMessage;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.VoiceManagerData;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.inventory.types.Radio;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.radio.GameMode;
import zombie.radio.RadioAPI;
import zombie.radio.RadioData;
import zombie.radio.RadioDebugConsole;
import zombie.radio.StorySounds.SLSoundManager;
import zombie.radio.devices.DeviceData;
import zombie.radio.devices.WaveSignalDevice;
import zombie.radio.media.RecordedMedia;
import zombie.radio.scripting.RadioChannel;
import zombie.radio.scripting.RadioScript;
import zombie.radio.scripting.RadioScriptManager;

@UsedFromLua
public final class ZomboidRadio {
    public static final String SAVE_FILE = "RADIO_SAVE.txt";
    private final ArrayList<WaveSignalDevice> devices = new ArrayList();
    private final ArrayList<WaveSignalDevice> broadcastDevices = new ArrayList();
    private RadioScriptManager scriptManager;
    private int daysSinceStart;
    private int lastRecordedHour;
    private final String[] playerLastLine = new String[4];
    private final Map<Integer, String> channelNames = new HashMap<Integer, String>();
    private final Map<String, Map<Integer, String>> categorizedChannels = new HashMap<String, Map<Integer, String>>();
    private final List<Integer> knownFrequencies = new ArrayList<Integer>();
    private RadioDebugConsole debugConsole;
    private boolean hasRecievedServerData;
    private final SLSoundManager storySoundManager = null;
    private static final String[] staticSounds = new String[]{"<bzzt>", "<fzzt>", "<wzzt>", "<szzt>"};
    public static final boolean DEBUG_MODE = false;
    public static final boolean DEBUG_XML = false;
    public static final boolean DEBUG_SOUND = false;
    public static boolean postRadioSilence;
    public static boolean disableBroadcasting;
    private static ZomboidRadio instance;
    private static RecordedMedia recordedMedia;
    public static boolean louisvilleObfuscation;
    private String lastSaveFile;
    private String lastSaveContent;
    private final HashMap<Integer, FreqListEntry> freqlist = new HashMap();
    private boolean hasAppliedRangeDistortion;
    private final StringBuilder stringBuilder = new StringBuilder();
    private boolean hasAppliedInterference;
    private static final int[] obfuscateChannels;

    public static boolean hasInstance() {
        return instance != null;
    }

    public static ZomboidRadio getInstance() {
        if (instance == null) {
            instance = new ZomboidRadio();
        }
        return instance;
    }

    private ZomboidRadio() {
        this.lastRecordedHour = GameTime.instance.getHour();
        SLSoundManager.debug = false;
        for (int i = 0; i < staticSounds.length; ++i) {
            ChatElement.addNoLogText(staticSounds[i]);
        }
        ChatElement.addNoLogText("~");
        recordedMedia = new RecordedMedia();
    }

    public static boolean isStaticSound(String str) {
        if (str != null) {
            for (String s : staticSounds) {
                if (!str.equals(s)) continue;
                return true;
            }
        }
        return false;
    }

    public RadioScriptManager getScriptManager() {
        return this.scriptManager;
    }

    public int getDaysSinceStart() {
        return this.daysSinceStart;
    }

    public ArrayList<WaveSignalDevice> getDevices() {
        return this.devices;
    }

    public ArrayList<WaveSignalDevice> getBroadcastDevices() {
        return this.broadcastDevices;
    }

    public void setHasRecievedServerData(boolean state) {
        this.hasRecievedServerData = state;
    }

    public void addChannelName(String name, int frequency, String category) {
        this.addChannelName(name, frequency, category, true);
    }

    public void addChannelName(String name, int frequency, String category, boolean overwrite) {
        if (overwrite || !this.channelNames.containsKey(frequency)) {
            if (!this.categorizedChannels.containsKey(category)) {
                this.categorizedChannels.put(category, new HashMap());
            }
            this.categorizedChannels.get(category).put(frequency, name);
            this.channelNames.put(frequency, name);
            this.knownFrequencies.add(frequency);
        }
    }

    public void removeChannelName(int frequency) {
        if (this.channelNames.containsKey(frequency)) {
            this.channelNames.remove(frequency);
            for (Map.Entry<String, Map<Integer, String>> entry : this.categorizedChannels.entrySet()) {
                if (!entry.getValue().containsKey(frequency)) continue;
                entry.getValue().remove(frequency);
            }
        }
    }

    public Map<Integer, String> GetChannelList(String category) {
        if (this.categorizedChannels.containsKey(category)) {
            return this.categorizedChannels.get(category);
        }
        return null;
    }

    public String getChannelName(int frequency) {
        if (this.channelNames.containsKey(frequency)) {
            return this.channelNames.get(frequency);
        }
        return null;
    }

    public int getRandomFrequency() {
        return this.getRandomFrequency(88000, 108000);
    }

    public int getRandomFrequency(int rangemin, int rangemax) {
        int freq;
        while (this.knownFrequencies.contains(freq = Rand.Next(rangemin, rangemax) / 200 * 200)) {
        }
        return freq;
    }

    public Map<String, Map<Integer, String>> getFullChannelList() {
        return this.categorizedChannels;
    }

    public void WriteRadioServerDataPacket(ByteBufferWriter bb) {
        bb.putInt(this.categorizedChannels.size());
        for (Map.Entry<String, Map<Integer, String>> entry : this.categorizedChannels.entrySet()) {
            bb.putUTF(entry.getKey());
            bb.putInt(entry.getValue().size());
            for (Map.Entry<Integer, String> entry2 : entry.getValue().entrySet()) {
                bb.putInt(entry2.getKey());
                bb.putUTF(entry2.getValue());
            }
        }
        bb.putBoolean(postRadioSilence);
    }

    public void Init(int savedWorldVersion) {
        postRadioSilence = false;
        boolean success = false;
        boolean bDebugEnabled = DebugLog.isEnabled(DebugType.Radio);
        if (bDebugEnabled) {
            DebugLog.Radio.println();
            DebugLog.Radio.println("################## Radio Init ##################");
        }
        RadioAPI.getInstance();
        recordedMedia.init();
        this.lastRecordedHour = GameTime.instance.getHour();
        GameMode mode = this.getGameMode();
        if (mode == GameMode.Client) {
            GameClient.sendRadioServerDataRequest();
            if (bDebugEnabled) {
                DebugLog.Radio.println("Radio (Client) loaded.");
                DebugLog.Radio.println("################################################");
            }
            this.scriptManager = null;
            return;
        }
        this.scriptManager = RadioScriptManager.getInstance();
        this.scriptManager.init(savedWorldVersion);
        try {
            if (!Core.getInstance().isNoSave()) {
                ZomboidFileSystem.instance.getFileInCurrentSave("radio", "data").mkdirs();
            }
            ArrayList<RadioData> radioDataList = RadioData.fetchAllRadioData();
            for (RadioData radioData : radioDataList) {
                for (RadioChannel channel : radioData.getRadioChannels()) {
                    ZomboidRadio.ObfuscateChannelCheck(channel);
                    RadioChannel found = null;
                    if (this.scriptManager.getChannels().containsKey(channel.GetFrequency())) {
                        found = this.scriptManager.getChannels().get(channel.GetFrequency());
                    }
                    if (found == null || found.getRadioData().isVanilla() && !channel.getRadioData().isVanilla()) {
                        this.scriptManager.AddChannel(channel, true);
                        continue;
                    }
                    if (!bDebugEnabled) continue;
                    DebugLog.Radio.println("Unable to add channel: " + channel.GetName() + ", frequency '" + channel.GetFrequency() + "' taken.");
                }
            }
            LuaEventManager.triggerEvent("OnLoadRadioScripts", this.scriptManager, savedWorldVersion == -1);
            if (savedWorldVersion == -1) {
                if (bDebugEnabled) {
                    DebugLog.Radio.println("Radio setting new game start times");
                }
                SandboxOptions options = SandboxOptions.instance;
                int months = options.timeSinceApo.getValue() - 1;
                if (months < 0) {
                    months = 0;
                }
                if (bDebugEnabled) {
                    DebugLog.log(DebugType.Radio, "Time since the apocalypse: " + options.timeSinceApo.getValue());
                }
                if (months > 0) {
                    this.daysSinceStart = (int)((float)months * 30.5f);
                    if (bDebugEnabled) {
                        DebugLog.Radio.println("Time since the apocalypse in days: " + this.daysSinceStart);
                    }
                    this.scriptManager.simulateScriptsUntil(this.daysSinceStart, true);
                }
                this.checkGameModeSpecificStart();
            } else {
                boolean isLoaded = this.Load();
                if (!isLoaded) {
                    SandboxOptions options = SandboxOptions.instance;
                    int months = options.timeSinceApo.getValue() - 1;
                    if (months < 0) {
                        months = 0;
                    }
                    this.daysSinceStart = (int)((float)months * 30.5f);
                    this.daysSinceStart += GameTime.instance.getNightsSurvived();
                }
                if (this.daysSinceStart > 0) {
                    this.scriptManager.simulateScriptsUntil(this.daysSinceStart, false);
                }
            }
            success = true;
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
        }
        if (!bDebugEnabled) {
            return;
        }
        if (success) {
            DebugLog.Radio.println("Radio loaded.");
        }
        DebugLog.Radio.println("################################################");
        DebugLog.Radio.println();
    }

    private void checkGameModeSpecificStart() {
        block5: {
            block4: {
                if (!Core.gameMode.equals("Initial Infection")) break block4;
                for (Map.Entry<Integer, RadioChannel> entry : this.scriptManager.getChannels().entrySet()) {
                    RadioScript initInfectionScript = entry.getValue().getRadioScript("init_infection");
                    if (initInfectionScript != null) {
                        initInfectionScript.clearExitOptions();
                        initInfectionScript.AddExitOption(entry.getValue().getCurrentScript().GetName(), 100, 0);
                        entry.getValue().setActiveScript("init_infection", this.daysSinceStart);
                        continue;
                    }
                    entry.getValue().getCurrentScript().setStartDayStamp(this.daysSinceStart + 1);
                }
                break block5;
            }
            if (!Core.gameMode.equals("Six Months Later")) break block5;
            for (Map.Entry<Integer, RadioChannel> entry : this.scriptManager.getChannels().entrySet()) {
                if (entry.getValue().GetName().equals("Classified M1A1")) {
                    entry.getValue().setActiveScript("numbers", this.daysSinceStart);
                    continue;
                }
                if (!entry.getValue().GetName().equals("NNR Radio")) continue;
                entry.getValue().setActiveScript("pastor", this.daysSinceStart);
            }
        }
    }

    public void Save() throws FileNotFoundException, IOException {
        File path;
        if (Core.getInstance().isNoSave()) {
            return;
        }
        GameMode mode = this.getGameMode();
        if ((mode == GameMode.Server || mode == GameMode.SinglePlayer) && this.scriptManager != null && (path = ZomboidFileSystem.instance.getFileInCurrentSave("radio", "data")).exists() && path.isDirectory()) {
            String content;
            String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("radio", "data", SAVE_FILE);
            try (StringWriter w = new StringWriter(1024);){
                w.write("DaysSinceStart = " + this.daysSinceStart + System.lineSeparator());
                w.write("LvObfuscation = " + louisvilleObfuscation + System.lineSeparator());
                this.scriptManager.Save(w);
                content = w.toString();
            }
            catch (IOException ex) {
                ExceptionLogger.logException(ex);
                return;
            }
            if (fileName.equals(this.lastSaveFile) && content.equals(this.lastSaveContent)) {
                return;
            }
            this.lastSaveFile = fileName;
            this.lastSaveContent = content;
            File f = new File(fileName);
            if (DebugLog.isEnabled(DebugType.Radio)) {
                DebugLog.Radio.println("Saving radio: " + fileName);
            }
            try (FileWriter w = new FileWriter(f, false);){
                w.write(content);
            }
            catch (Exception ex) {
                ExceptionLogger.logException(ex);
            }
        }
        if (recordedMedia != null) {
            try {
                recordedMedia.save();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean Load() throws FileNotFoundException, IOException {
        boolean result = false;
        GameMode mode = this.getGameMode();
        if (mode == GameMode.Server || mode == GameMode.SinglePlayer) {
            for (Map.Entry<Integer, RadioChannel> entry : this.scriptManager.getChannels().entrySet()) {
                entry.getValue().setActiveScriptNull();
            }
            ArrayList<String> channelLines = new ArrayList<String>();
            String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("radio", "data", SAVE_FILE);
            File file = new File(fileName);
            if (!file.exists()) {
                return false;
            }
            if (DebugLog.isEnabled(DebugType.Radio)) {
                DebugLog.log(DebugType.Radio, "Loading radio save:" + fileName);
            }
            try (FileReader fr = new FileReader(file);
                 BufferedReader r = new BufferedReader(fr);){
                String line;
                while ((line = r.readLine()) != null) {
                    if ((line = line.trim()).startsWith("DaysSinceStart") || line.startsWith("LvObfuscation")) {
                        String[] s;
                        if (line.startsWith("DaysSinceStart")) {
                            s = line.split("=");
                            this.daysSinceStart = Integer.parseInt(s[1].trim());
                        }
                        if (!line.startsWith("LvObfuscation")) continue;
                        s = line.split("=");
                        louisvilleObfuscation = Boolean.parseBoolean(s[1].trim());
                        continue;
                    }
                    channelLines.add(line);
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
            try {
                DebugLog.log("Radio Loading channels...");
                this.scriptManager.Load(channelLines);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                boolean bl = false;
                return bl;
            }
            finally {
                result = true;
            }
        }
        return result;
    }

    public void Reset() {
        instance = null;
        if (this.scriptManager != null) {
            this.scriptManager.reset();
        }
    }

    public void UpdateScripts(int hour, int mins) {
        GameMode mode = this.getGameMode();
        if (mode == GameMode.Server || mode == GameMode.SinglePlayer) {
            if (hour == 0 && this.lastRecordedHour != 0) {
                ++this.daysSinceStart;
            }
            this.lastRecordedHour = hour;
            if (this.scriptManager != null) {
                this.scriptManager.UpdateScripts(this.daysSinceStart, hour, mins);
            }
            try {
                this.Save();
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        if (mode == GameMode.Client || mode == GameMode.SinglePlayer) {
            for (int i = 0; i < this.devices.size(); ++i) {
                WaveSignalDevice device = this.devices.get(i);
                if (!device.getDeviceData().getIsTurnedOn() || !device.HasPlayerInRange()) continue;
                device.getDeviceData().TriggerPlayerListening(true);
            }
        }
        if (mode == GameMode.Client && !this.hasRecievedServerData) {
            GameClient.sendRadioServerDataRequest();
        }
    }

    public void render() {
        GameMode mode = this.getGameMode();
        if (mode != GameMode.Server && this.storySoundManager != null) {
            this.storySoundManager.render();
        }
    }

    private void addFrequencyListEntry(boolean isinvitem, DeviceData devicedata, int x, int y) {
        if (devicedata == null) {
            return;
        }
        if (!this.freqlist.containsKey(devicedata.getChannel())) {
            this.freqlist.put(devicedata.getChannel(), new FreqListEntry(isinvitem, devicedata, x, y));
        } else if (this.freqlist.get((Object)Integer.valueOf((int)devicedata.getChannel())).deviceData.getTransmitRange() < devicedata.getTransmitRange()) {
            FreqListEntry fe = this.freqlist.get(devicedata.getChannel());
            fe.isInvItem = isinvitem;
            fe.deviceData = devicedata;
            fe.sourceX = x;
            fe.sourceY = y;
        }
    }

    public void update() {
        this.LouisvilleObfuscationCheck();
        GameMode mode = this.getGameMode();
        if (!(mode != GameMode.Server && mode != GameMode.SinglePlayer || this.daysSinceStart <= 14 || postRadioSilence)) {
            postRadioSilence = true;
            if (GameServer.server) {
                GameServer.sendRadioPostSilence();
            }
        }
        if (mode != GameMode.Server && this.storySoundManager != null) {
            this.storySoundManager.update(this.daysSinceStart, GameTime.instance.getHour(), GameTime.instance.getMinutes());
        }
        if ((mode == GameMode.Server || mode == GameMode.SinglePlayer) && this.scriptManager != null) {
            this.scriptManager.update();
        }
        if (mode == GameMode.SinglePlayer || mode == GameMode.Client) {
            for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                String lastChatMessage;
                IsoPlayer player = IsoPlayer.players[i];
                if (player == null || player.getLastSpokenLine() == null || this.playerLastLine[i] != null && this.playerLastLine[i].equals(player.getLastSpokenLine())) continue;
                this.playerLastLine[i] = lastChatMessage = player.getLastSpokenLine();
                if (mode == GameMode.Client && ((player.role == Roles.getDefaultForAdmin() || player.role == Roles.getDefaultForGM() || player.role == Roles.getDefaultForModerator()) && (ServerOptions.instance.disableRadioStaff.getValue() || ServerOptions.instance.disableRadioAdmin.getValue() && player.role == Roles.getDefaultForAdmin() || ServerOptions.instance.disableRadioGm.getValue() && player.role == Roles.getDefaultForGM() || ServerOptions.instance.disableRadioOverseer.getValue() && player.role == Roles.getDefaultForOverseer() || ServerOptions.instance.disableRadioModerator.getValue() && player.role == Roles.getDefaultForModerator()) || ServerOptions.instance.disableRadioInvisible.getValue() && player.isInvisible())) continue;
                this.freqlist.clear();
                if (!GameClient.client && !GameServer.server) {
                    for (int index = 0; index < IsoPlayer.numPlayers; ++index) {
                        this.checkPlayerForDevice(IsoPlayer.players[index], player);
                    }
                } else if (GameClient.client) {
                    ArrayList<IsoPlayer> players = GameClient.instance.getPlayers();
                    for (int j = 0; j < players.size(); ++j) {
                        this.checkPlayerForDevice((IsoPlayer)players.get(j), player);
                    }
                }
                for (WaveSignalDevice device : this.broadcastDevices) {
                    if (device == null || device.getDeviceData() == null || !device.getDeviceData().getIsTurnedOn() || !device.getDeviceData().getIsTwoWay() || !device.HasPlayerInRange() || device.getDeviceData().getMicIsMuted() || this.GetDistance(PZMath.fastfloor(player.getX()), PZMath.fastfloor(player.getY()), PZMath.fastfloor(device.getX()), PZMath.fastfloor(device.getY())) >= device.getDeviceData().getMicRange()) continue;
                    this.addFrequencyListEntry(true, device.getDeviceData(), PZMath.fastfloor(device.getX()), PZMath.fastfloor(device.getY()));
                }
                if (this.freqlist.isEmpty()) continue;
                Color col = player.getSpeakColour();
                for (Map.Entry<Integer, FreqListEntry> entry : this.freqlist.entrySet()) {
                    FreqListEntry d = entry.getValue();
                    this.SendTransmission(d.sourceX, d.sourceY, entry.getKey(), this.playerLastLine[i], null, null, col.r, col.g, col.b, d.deviceData.getTransmitRange(), false);
                }
            }
        }
    }

    private void checkPlayerForDevice(IsoPlayer plr, IsoPlayer selfPlayer) {
        Radio radio;
        boolean playerIsSelf;
        boolean bl = playerIsSelf = plr == selfPlayer;
        if (plr != null && (radio = plr.getEquipedRadio()) != null && radio.getDeviceData() != null && radio.getDeviceData().getIsPortable() && radio.getDeviceData().getIsTwoWay() && radio.getDeviceData().getIsTurnedOn() && !radio.getDeviceData().getMicIsMuted() && (playerIsSelf || this.GetDistance(PZMath.fastfloor(selfPlayer.getX()), PZMath.fastfloor(selfPlayer.getY()), PZMath.fastfloor(plr.getX()), PZMath.fastfloor(plr.getY())) < radio.getDeviceData().getMicRange())) {
            this.addFrequencyListEntry(true, radio.getDeviceData(), PZMath.fastfloor(plr.getX()), PZMath.fastfloor(plr.getY()));
        }
    }

    private boolean DeviceInRange(int dx, int dy, int sx, int sy, int ss) {
        return dx > sx - ss && dx < sx + ss && dy > sy - ss && dy < sy + ss && Math.sqrt(Math.pow(dx - sx, 2.0) + Math.pow(dy - sy, 2.0)) < (double)ss;
    }

    private int GetDistance(int dx, int dy, int sx, int sy) {
        return (int)Math.sqrt(Math.pow(dx - sx, 2.0) + Math.pow(dy - sy, 2.0));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void DistributeToPlayerOnClient(IsoPlayer player, int sourceX, int sourceY, int channel, String msg, String guid, String codes, float r, float g, float b, int signalStrength, boolean isTV) {
        if (player != null && player.getOnlineID() != -1) {
            VoiceManagerData myRadioData = VoiceManagerData.get(player.getOnlineID());
            ArrayList<VoiceManagerData.RadioData> arrayList = myRadioData.radioData;
            synchronized (arrayList) {
                for (VoiceManagerData.RadioData radio : myRadioData.radioData) {
                    if (!radio.isReceivingAvailable(channel)) continue;
                    this.DistributeToPlayerInternal(radio.getDeviceData().getParent(), player, sourceX, sourceY, msg, guid, codes, r, g, b, signalStrength);
                }
            }
        }
    }

    private void DistributeToPlayer(IsoPlayer player, int sourceX, int sourceY, int channel, String msg, String guid, String codes, float r, float g, float b, int signalStrength, boolean isTV) {
        Radio radio;
        if (player != null && (radio = player.getEquipedRadio()) != null && radio.getDeviceData() != null && radio.getDeviceData().getIsPortable() && radio.getDeviceData().getIsTurnedOn() && radio.getDeviceData().getChannel() == channel) {
            if (radio.getDeviceData().getDeviceVolume() <= 0.0f) {
                return;
            }
            if (radio.getDeviceData().isPlayingMedia() || radio.getDeviceData().isNoTransmit()) {
                return;
            }
            this.DistributeToPlayerInternal(radio, player, sourceX, sourceY, msg, guid, codes, r, g, b, signalStrength);
        }
    }

    private void DistributeToPlayerInternal(WaveSignalDevice radio, IsoPlayer player, int sourceX, int sourceY, String msg, String guid, String codes, float r, float g, float b, int signalStrength) {
        boolean pass = false;
        int dist = -1;
        if (signalStrength < 0) {
            pass = true;
        } else {
            dist = this.GetDistance((int)player.getX(), (int)player.getY(), sourceX, sourceY);
            if (dist > 3 && dist < signalStrength) {
                pass = true;
            }
        }
        if (pass) {
            if (signalStrength > 0) {
                this.hasAppliedRangeDistortion = false;
                msg = this.doDeviceRangeDistortion(msg, signalStrength, dist);
            }
            if (!this.hasAppliedRangeDistortion) {
                radio.AddDeviceText(player, msg, r, g, b, guid, codes, dist);
            } else {
                radio.AddDeviceText(msg, 0.5f, 0.5f, 0.5f, guid, codes, dist);
            }
        }
    }

    public void DistributeTransmission(int sourceX, int sourceY, int channel, String msg, String guid, String codes, float r, float g, float b, int signalStrength, boolean isTV) {
        if (!isTV) {
            if (!GameClient.client && !GameServer.server) {
                for (int index = 0; index < IsoPlayer.numPlayers; ++index) {
                    this.DistributeToPlayer(IsoPlayer.players[index], sourceX, sourceY, channel, msg, guid, codes, r, g, b, signalStrength, isTV);
                }
            } else if (GameClient.client) {
                for (IsoPlayer player : IsoPlayer.players) {
                    this.DistributeToPlayerOnClient(player, sourceX, sourceY, channel, msg, guid, codes, r, g, b, signalStrength, isTV);
                }
                return;
            }
        }
        if (this.devices.isEmpty()) {
            return;
        }
        for (int i = 0; i < this.devices.size(); ++i) {
            WaveSignalDevice device = this.devices.get(i);
            if (device == null || device.getDeviceData() == null || !device.getDeviceData().getIsTurnedOn() || isTV != device.getDeviceData().getIsTelevision()) continue;
            if (device.getDeviceData().isPlayingMedia() || device.getDeviceData().isNoTransmit()) {
                return;
            }
            if (channel != device.getDeviceData().getChannel()) continue;
            boolean pass = false;
            if (signalStrength == -1) {
                pass = true;
            } else if (sourceX != PZMath.fastfloor(device.getX()) && sourceY != PZMath.fastfloor(device.getY())) {
                pass = true;
            }
            if (!pass) continue;
            int dist = -1;
            if (signalStrength > 0) {
                this.hasAppliedRangeDistortion = false;
                dist = this.GetDistance(PZMath.fastfloor(device.getX()), PZMath.fastfloor(device.getY()), sourceX, sourceY);
                msg = this.doDeviceRangeDistortion(msg, signalStrength, dist);
            }
            if (!this.hasAppliedRangeDistortion) {
                if (GameServer.server) {
                    if (!(device.getDeviceData().getDeviceVolume() > 0.0f) || codes == null) continue;
                    LuaEventManager.triggerEvent("OnDeviceText", guid, codes, Float.valueOf(device.getX()), Float.valueOf(device.getY()), Float.valueOf(device.getZ()), msg, device);
                    continue;
                }
                device.AddDeviceText(msg, r, g, b, guid, codes, dist);
                continue;
            }
            if (GameServer.server) {
                if (!(device.getDeviceData().getDeviceVolume() > 0.0f) || codes == null) continue;
                LuaEventManager.triggerEvent("OnDeviceText", guid, codes, Float.valueOf(device.getX()), Float.valueOf(device.getY()), Float.valueOf(device.getZ()), msg, device);
                continue;
            }
            device.AddDeviceText(msg, 0.5f, 0.5f, 0.5f, guid, codes, dist);
        }
    }

    private String doDeviceRangeDistortion(String msg, int signalStrength, int dist) {
        float distortRange = (float)signalStrength * 0.9f;
        if (distortRange < (float)signalStrength && (float)dist > distortRange) {
            float scambleIntensity = 100.0f * (((float)dist - distortRange) / ((float)signalStrength - distortRange));
            msg = this.scrambleString(msg, (int)scambleIntensity, false);
            this.hasAppliedRangeDistortion = true;
        }
        return msg;
    }

    public GameMode getGameMode() {
        if (!GameClient.client && !GameServer.server) {
            return GameMode.SinglePlayer;
        }
        if (GameServer.server) {
            return GameMode.Server;
        }
        return GameMode.Client;
    }

    public String getRandomBzztFzzt() {
        int r = Rand.Next(staticSounds.length);
        return staticSounds[r];
    }

    private String applyWeatherInterference(String msg, int signalStrength) {
        if (ClimateManager.getInstance().getWeatherInterference() <= 0.0f) {
            return msg;
        }
        int intensity = (int)(ClimateManager.getInstance().getWeatherInterference() * 100.0f);
        return this.scrambleString(msg, intensity, signalStrength == -1);
    }

    private String scrambleString(String msg, int intensity, boolean ignoreBBcode) {
        return this.scrambleString(msg, intensity, ignoreBBcode, null);
    }

    public String scrambleString(String msg, int intensity, boolean ignoreBBcode, String customScramble) {
        this.hasAppliedInterference = false;
        StringBuilder newMsg = this.stringBuilder;
        newMsg.setLength(0);
        if (intensity <= 0) {
            return msg;
        }
        if (intensity >= 100) {
            return customScramble != null ? customScramble : this.getRandomBzztFzzt();
        }
        this.hasAppliedInterference = true;
        if (ignoreBBcode) {
            char[] chars = msg.toCharArray();
            boolean scrmbl = false;
            boolean hasOpened = false;
            Object word = "";
            for (int i = 0; i < chars.length; ++i) {
                char c = chars[i];
                if (hasOpened) {
                    word = (String)word + c;
                    if (c != ']') continue;
                    newMsg.append((String)word);
                    word = "";
                    hasOpened = false;
                    continue;
                }
                if (c != '[' && (!Character.isWhitespace(c) || i <= 0 || Character.isWhitespace(chars[i - 1]))) {
                    word = (String)word + c;
                    continue;
                }
                int r = Rand.Next(100);
                if (r > intensity) {
                    newMsg.append((String)word).append(" ");
                    scrmbl = false;
                } else if (!scrmbl) {
                    newMsg.append(customScramble != null ? customScramble : this.getRandomBzztFzzt()).append(" ");
                    scrmbl = true;
                }
                if (c == '[') {
                    word = "[";
                    hasOpened = true;
                    continue;
                }
                word = "";
            }
            if (word != null && !((String)word).isEmpty()) {
                newMsg.append((String)word);
            }
        } else {
            boolean scrmbl = false;
            String[] words = msg.split("\\s+");
            for (int i = 0; i < words.length; ++i) {
                String word = words[i];
                int r = Rand.Next(100);
                if (r > intensity) {
                    newMsg.append(word).append(" ");
                    scrmbl = false;
                    continue;
                }
                if (scrmbl) continue;
                newMsg.append(customScramble != null ? customScramble : this.getRandomBzztFzzt()).append(" ");
                scrmbl = true;
            }
        }
        return newMsg.toString();
    }

    public void SendTransmission(int sourceX, int sourceY, ChatMessage msg, int signalStrength) {
        Color color = msg.getTextColor();
        int channel = msg.getRadioChannel();
        this.SendTransmission(sourceX, sourceY, channel, msg.getText(), null, null, color.r, color.g, color.b, signalStrength, false);
    }

    public void SendTransmission(int sourceX, int sourceY, int channel, String msg, String guid, String codes, float r, float g, float b, int signalStrength, boolean isTV) {
        this.SendTransmission(-1L, sourceX, sourceY, channel, msg, guid, codes, r, g, b, signalStrength, isTV);
    }

    public void SendTransmission(long source2, int sourceX, int sourceY, int channel, String msg, String guid, String codes, float r, float g, float b, int signalStrength, boolean isTV) {
        GameMode mode = this.getGameMode();
        if (!(isTV || mode != GameMode.Server && mode != GameMode.SinglePlayer)) {
            this.hasAppliedInterference = false;
            msg = this.applyWeatherInterference(msg, signalStrength);
            if (this.hasAppliedInterference) {
                r = 0.5f;
                g = 0.5f;
                b = 0.5f;
                codes = "";
            }
        }
        if (mode == GameMode.SinglePlayer) {
            this.DistributeTransmission(sourceX, sourceY, channel, msg, guid, codes, r, g, b, signalStrength, isTV);
        } else if (mode == GameMode.Server) {
            this.DistributeTransmission(sourceX, sourceY, channel, msg, guid, codes, r, g, b, signalStrength, isTV);
            GameServer.sendIsoWaveSignal(source2, sourceX, sourceY, channel, msg, guid, codes, r, g, b, signalStrength, isTV);
        } else if (mode == GameMode.Client) {
            GameClient.sendIsoWaveSignal(sourceX, sourceY, channel, msg, guid, codes, r, g, b, signalStrength, isTV);
        }
    }

    public void PlayerListensChannel(int channel, boolean listenmode, boolean isTV) {
        GameMode mode = this.getGameMode();
        if (mode == GameMode.SinglePlayer || mode == GameMode.Server) {
            if (this.scriptManager != null) {
                this.scriptManager.PlayerListensChannel(channel, listenmode, isTV);
            }
        } else if (mode == GameMode.Client) {
            GameClient.sendPlayerListensChannel(channel, listenmode, isTV);
        }
    }

    public void RegisterDevice(WaveSignalDevice device) {
        if (device == null) {
            return;
        }
        if (!this.devices.contains(device)) {
            this.devices.add(device);
        }
        if (!GameServer.server && device.getDeviceData().getIsTwoWay() && !this.broadcastDevices.contains(device)) {
            this.broadcastDevices.add(device);
        }
    }

    public void UnRegisterDevice(WaveSignalDevice device) {
        if (device == null) {
            return;
        }
        if (this.devices.contains(device)) {
            this.devices.remove(device);
        }
        if (!GameServer.server && device.getDeviceData().getIsTwoWay() && this.broadcastDevices.contains(device)) {
            this.broadcastDevices.remove(device);
        }
    }

    public Object clone() {
        return null;
    }

    public String computerize(String str) {
        StringBuilder sb = this.stringBuilder;
        sb.setLength(0);
        for (char c : str.toCharArray()) {
            if (Character.isLetter(c)) {
                sb.append(Rand.NextBool(2) ? Character.toLowerCase(c) : Character.toUpperCase(c));
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public RecordedMedia getRecordedMedia() {
        return recordedMedia;
    }

    public void setDisableBroadcasting(boolean b) {
        disableBroadcasting = b;
    }

    public boolean getDisableBroadcasting() {
        return disableBroadcasting;
    }

    public void setDisableMediaLineLearning(boolean b) {
        RecordedMedia.disableLineLearning = b;
    }

    public boolean getDisableMediaLineLearning() {
        return RecordedMedia.disableLineLearning;
    }

    private void LouisvilleObfuscationCheck() {
        if (GameClient.client || GameServer.server) {
            return;
        }
        IsoPlayer player = IsoPlayer.getInstance();
        if (player != null && player.getY() < 3550.0f) {
            louisvilleObfuscation = true;
        }
    }

    public static void ObfuscateChannelCheck(RadioChannel channel) {
        if (!channel.isVanilla()) {
            return;
        }
        int freq = channel.GetFrequency();
        for (int i = 0; i < obfuscateChannels.length; ++i) {
            if (freq != obfuscateChannels[i]) continue;
            channel.setLouisvilleObfuscate(true);
        }
    }

    static {
        obfuscateChannels = new int[]{200, 201, 204, 93200, 98000, 101200};
    }

    private static final class FreqListEntry {
        public boolean isInvItem;
        public DeviceData deviceData;
        public int sourceX;
        public int sourceY;

        public FreqListEntry(boolean isinvitem, DeviceData devicedata, int x, int y) {
            this.isInvItem = isinvitem;
            this.deviceData = devicedata;
            this.sourceX = x;
            this.sourceY = y;
        }
    }
}

