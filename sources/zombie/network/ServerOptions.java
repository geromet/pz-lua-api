/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.config.EnumConfigOption;
import zombie.config.IntegerConfigOption;
import zombie.config.StringConfigOption;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.logger.LoggerManager;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.GameServer;

@UsedFromLua
public class ServerOptions {
    public static final ServerOptions instance = new ServerOptions();
    private final ArrayList<String> publicOptions = new ArrayList();
    public static HashMap<String, String> clientOptionsList;
    public static final int MAX_PORT = 65535;
    private final ArrayList<ServerOption> options = new ArrayList();
    private final HashMap<String, ServerOption> optionByName = new HashMap();
    public BooleanServerOption pvp = new BooleanServerOption(this, "PVP", true);
    public BooleanServerOption pvpLogToolChat = new BooleanServerOption(this, "PVPLogToolChat", true);
    public BooleanServerOption pvpLogToolFile = new BooleanServerOption(this, "PVPLogToolFile", true);
    public BooleanServerOption pauseEmpty = new BooleanServerOption(this, "PauseEmpty", true);
    public BooleanServerOption globalChat = new BooleanServerOption(this, "GlobalChat", true);
    public StringServerOption chatStreams = new StringServerOption(this, "ChatStreams", "s,r,a,w,y,sh,f,all", -1);
    public BooleanServerOption open = new BooleanServerOption(this, "Open", true);
    public TextServerOption serverWelcomeMessage = new TextServerOption(this, "ServerWelcomeMessage", "Welcome to Project Zomboid Multiplayer! <LINE> <LINE> To interact with the Chat panel: press Tab, T, or Enter. <LINE> <LINE> The Tab key will change the target stream of the message. <LINE> <LINE> Global Streams: /all <LINE> Local Streams: /say, /yell <LINE> Special Steams: /whisper, /safehouse, /faction. <LINE> <LINE> Press the Up arrow to cycle through your message history. Click the Gear icon to customize chat. <LINE> <LINE> Happy surviving!", -1);
    public StringServerOption serverImageLoginScreen = new StringServerOption(this, "ServerImageLoginScreen", "", -1);
    public StringServerOption serverImageLoadingScreen = new StringServerOption(this, "ServerImageLoadingScreen", "", -1);
    public StringServerOption serverImageIcon = new StringServerOption(this, "ServerImageIcon", "", -1);
    public BooleanServerOption autoCreateUserInWhiteList = new BooleanServerOption(this, "AutoCreateUserInWhiteList", false);
    public BooleanServerOption displayUserName = new BooleanServerOption(this, "DisplayUserName", true);
    public BooleanServerOption showFirstAndLastName = new BooleanServerOption(this, "ShowFirstAndLastName", false);
    public BooleanServerOption usernameDisguises = new BooleanServerOption(this, "UsernameDisguises", false);
    public BooleanServerOption hideDisguisedUserName = new BooleanServerOption(this, "HideDisguisedUserName", false);
    public BooleanServerOption switchZombiesOwnershipEachUpdate = new BooleanServerOption(this, "SwitchZombiesOwnershipEachUpdate", false);
    public StringServerOption spawnPoint = new StringServerOption(this, "SpawnPoint", "0,0,0", -1);
    public BooleanServerOption safetySystem = new BooleanServerOption(this, "SafetySystem", true);
    public BooleanServerOption showSafety = new BooleanServerOption(this, "ShowSafety", true);
    public IntegerServerOption safetyToggleTimer = new IntegerServerOption(this, "SafetyToggleTimer", 0, 1000, 2);
    public IntegerServerOption safetyCooldownTimer = new IntegerServerOption(this, "SafetyCooldownTimer", 0, 1000, 3);
    public IntegerServerOption safetyDisconnectDelay = new IntegerServerOption(this, "SafetyDisconnectDelay", 0, 60, 60);
    public StringServerOption spawnItems = new StringServerOption(this, "SpawnItems", "", -1);
    public IntegerServerOption defaultPort = new IntegerServerOption(this, "DefaultPort", 0, 65535, 16261);
    public IntegerServerOption udpPort = new IntegerServerOption(this, "UDPPort", 0, 65535, 16262);
    public IntegerServerOption resetId = new IntegerServerOption(this, "ResetID", 0, Integer.MAX_VALUE, Rand.Next(1000000000));
    public StringServerOption mods = new StringServerOption(this, "Mods", "", -1);
    public StringServerOption map = new StringServerOption(this, "Map", "Muldraugh, KY", -1);
    public BooleanServerOption doLuaChecksum = new BooleanServerOption(this, "DoLuaChecksum", true);
    public BooleanServerOption denyLoginOnOverloadedServer = new BooleanServerOption(this, "DenyLoginOnOverloadedServer", true);
    public BooleanServerOption isPublic = new BooleanServerOption(this, "Public", false);
    public StringServerOption publicName = new StringServerOption(this, "PublicName", "My PZ Server", 64);
    public TextServerOption publicDescription = new TextServerOption(this, "PublicDescription", "", 256);
    public IntegerServerOption maxPlayers = new IntegerServerOption(this, "MaxPlayers", 1, 100, 32);
    public IntegerServerOption pingLimit = new IntegerServerOption(this, "PingLimit", 0, Integer.MAX_VALUE, 0);
    public BooleanServerOption safehousePreventsLootRespawn = new BooleanServerOption(this, "SafehousePreventsLootRespawn", true);
    public BooleanServerOption dropOffWhiteListAfterDeath = new BooleanServerOption(this, "DropOffWhiteListAfterDeath", false);
    public BooleanServerOption noFire = new BooleanServerOption(this, "NoFire", false);
    public BooleanServerOption announceDeath = new BooleanServerOption(this, "AnnounceDeath", false);
    public BooleanServerOption announceAnimalDeath = new BooleanServerOption(this, "AnnounceAnimalDeath", false);
    public IntegerServerOption saveWorldEveryMinutes = new IntegerServerOption(this, "SaveWorldEveryMinutes", 0, Integer.MAX_VALUE, 0);
    public BooleanServerOption playerSafehouse = new BooleanServerOption(this, "PlayerSafehouse", false);
    public BooleanServerOption adminSafehouse = new BooleanServerOption(this, "AdminSafehouse", false);
    public BooleanServerOption safehouseAllowTrepass = new BooleanServerOption(this, "SafehouseAllowTrepass", true);
    public BooleanServerOption safehouseAllowFire = new BooleanServerOption(this, "SafehouseAllowFire", true);
    public BooleanServerOption safehouseAllowLoot = new BooleanServerOption(this, "SafehouseAllowLoot", true);
    public BooleanServerOption safehouseAllowRespawn = new BooleanServerOption(this, "SafehouseAllowRespawn", false);
    public IntegerServerOption safehouseDaySurvivedToClaim = new IntegerServerOption(this, "SafehouseDaySurvivedToClaim", 0, Integer.MAX_VALUE, 0);
    public IntegerServerOption safeHouseRemovalTime = new IntegerServerOption(this, "SafeHouseRemovalTime", 0, Integer.MAX_VALUE, 144);
    public BooleanServerOption safehouseAllowNonResidential = new BooleanServerOption(this, "SafehouseAllowNonResidential", false);
    public BooleanServerOption safehouseDisableDisguises = new BooleanServerOption(this, "SafehouseDisableDisguises", true);
    public IntegerServerOption maxSafezoneSize = new IntegerServerOption(this, "MaxSafezoneSize", 0, Integer.MAX_VALUE, 20000);
    public BooleanServerOption allowDestructionBySledgehammer = new BooleanServerOption(this, "AllowDestructionBySledgehammer", true);
    public BooleanServerOption sledgehammerOnlyInSafehouse = new BooleanServerOption(this, "SledgehammerOnlyInSafehouse", false);
    public BooleanServerOption war = new BooleanServerOption(this, "War", true);
    public IntegerServerOption warStartDelay = new IntegerServerOption(this, "WarStartDelay", 60, Integer.MAX_VALUE, 600);
    public IntegerServerOption warDuration = new IntegerServerOption(this, "WarDuration", 60, Integer.MAX_VALUE, 3600);
    public IntegerServerOption warSafehouseHitPoints = new IntegerServerOption(this, "WarSafehouseHitPoints", 0, Integer.MAX_VALUE, 3);
    public StringServerOption serverPlayerId = new StringServerOption(this, "ServerPlayerID", Integer.toString(Rand.Next(Integer.MAX_VALUE)), -1);
    public IntegerServerOption rconPort = new IntegerServerOption(this, "RCONPort", 0, 65535, 27015);
    public StringServerOption rconPassword = new StringServerOption(this, "RCONPassword", "", -1);
    public BooleanServerOption discordEnable = new BooleanServerOption(this, "DiscordEnable", false);
    public StringServerOption discordToken = new StringServerOption(this, "DiscordToken", "", -1);
    public StringServerOption discordChatChannel = new StringServerOption(this, "DiscordChatChannel", "", -1);
    public StringServerOption discordLogChannel = new StringServerOption(this, "DiscordLogChannel", "", -1);
    public StringServerOption discordCommandChannel = new StringServerOption(this, "DiscordCommandChannel", "", -1);
    public StringServerOption webhookAddress = new StringServerOption(this, "WebhookAddress", "", -1);
    public StringServerOption password = new StringServerOption(this, "Password", "", -1);
    public IntegerServerOption maxAccountsPerUser = new IntegerServerOption(this, "MaxAccountsPerUser", 0, Integer.MAX_VALUE, 0);
    public BooleanServerOption allowCoop = new BooleanServerOption(this, "AllowCoop", true);
    public BooleanServerOption sleepAllowed = new BooleanServerOption(this, "SleepAllowed", false);
    public BooleanServerOption sleepNeeded = new BooleanServerOption(this, "SleepNeeded", false);
    public BooleanServerOption knockedDownAllowed = new BooleanServerOption(this, "KnockedDownAllowed", false);
    public BooleanServerOption sneakModeHideFromOtherPlayers = new BooleanServerOption(this, "SneakModeHideFromOtherPlayers", true);
    public BooleanServerOption ultraSpeedDoesnotAffectToAnimals = new BooleanServerOption(this, "UltraSpeedDoesnotAffectToAnimals", false);
    public StringServerOption workshopItems = new StringServerOption(this, "WorkshopItems", "", -1);
    public BooleanServerOption steamScoreboard = new BooleanServerOption(this, "SteamScoreboard", false);
    public BooleanServerOption steamVac = new BooleanServerOption(this, "SteamVAC", true);
    public BooleanServerOption uPnp = new BooleanServerOption(this, "UPnP", true);
    public BooleanServerOption voiceEnable = new BooleanServerOption(this, "VoiceEnable", true);
    public DoubleServerOption voiceMinDistance = new DoubleServerOption(this, "VoiceMinDistance", 0.0, 100000.0, 10.0);
    public DoubleServerOption voiceMaxDistance = new DoubleServerOption(this, "VoiceMaxDistance", 0.0, 100000.0, 100.0);
    public BooleanServerOption voice3d = new BooleanServerOption(this, "Voice3D", true);
    public DoubleServerOption speedLimit = new DoubleServerOption(this, "SpeedLimit", 10.0, 150.0, 70.0);
    public BooleanServerOption loginQueueEnabled = new BooleanServerOption(this, "LoginQueueEnabled", false);
    public IntegerServerOption loginQueueConnectTimeout = new IntegerServerOption(this, "LoginQueueConnectTimeout", 20, 1200, 60);
    public StringServerOption serverBrowserAnnouncedIp = new StringServerOption(this, "server_browser_announced_ip", "", -1);
    public BooleanServerOption playerRespawnWithSelf = new BooleanServerOption(this, "PlayerRespawnWithSelf", false);
    public BooleanServerOption playerRespawnWithOther = new BooleanServerOption(this, "PlayerRespawnWithOther", false);
    public DoubleServerOption fastForwardMultiplier = new DoubleServerOption(this, "FastForwardMultiplier", 1.0, 100.0, 40.0);
    public BooleanServerOption disableSafehouseWhenPlayerConnected = new BooleanServerOption(this, "DisableSafehouseWhenPlayerConnected", false);
    public BooleanServerOption faction = new BooleanServerOption(this, "Faction", true);
    public IntegerServerOption factionDaySurvivedToCreate = new IntegerServerOption(this, "FactionDaySurvivedToCreate", 0, Integer.MAX_VALUE, 0);
    public IntegerServerOption factionPlayersRequiredForTag = new IntegerServerOption(this, "FactionPlayersRequiredForTag", 1, Integer.MAX_VALUE, 1);
    public BooleanServerOption disableRadioStaff = new BooleanServerOption(this, "DisableRadioStaff", false);
    public BooleanServerOption disableRadioAdmin = new BooleanServerOption(this, "DisableRadioAdmin", true);
    public BooleanServerOption disableRadioGm = new BooleanServerOption(this, "DisableRadioGM", true);
    public BooleanServerOption disableRadioOverseer = new BooleanServerOption(this, "DisableRadioOverseer", false);
    public BooleanServerOption disableRadioModerator = new BooleanServerOption(this, "DisableRadioModerator", false);
    public BooleanServerOption disableRadioInvisible = new BooleanServerOption(this, "DisableRadioInvisible", true);
    public StringServerOption clientCommandFilter = new StringServerOption(this, "ClientCommandFilter", "-vehicle.*;+vehicle.damageWindow;+vehicle.fixPart;+vehicle.installPart;+vehicle.uninstallPart", -1);
    public StringServerOption clientActionLogs = new StringServerOption(this, "ClientActionLogs", "ISEnterVehicle;ISExitVehicle;ISTakeEngineParts;", -1);
    public BooleanServerOption perkLogs = new BooleanServerOption(this, "PerkLogs", true);
    public IntegerServerOption itemNumbersLimitPerContainer = new IntegerServerOption(this, "ItemNumbersLimitPerContainer", 0, 9000, 0);
    public IntegerServerOption bloodSplatLifespanDays = new IntegerServerOption(this, "BloodSplatLifespanDays", 0, 365, 0);
    public BooleanServerOption allowNonAsciiUsername = new BooleanServerOption(this, "AllowNonAsciiUsername", false);
    public BooleanServerOption banKickGlobalSound = new BooleanServerOption(this, "BanKickGlobalSound", true);
    public BooleanServerOption removePlayerCorpsesOnCorpseRemoval = new BooleanServerOption(this, "RemovePlayerCorpsesOnCorpseRemoval", false);
    public BooleanServerOption trashDeleteAll = new BooleanServerOption(this, "TrashDeleteAll", false);
    public BooleanServerOption pvpMeleeWhileHitReaction = new BooleanServerOption(this, "PVPMeleeWhileHitReaction", false);
    public BooleanServerOption mouseOverToSeeDisplayName = new BooleanServerOption(this, "MouseOverToSeeDisplayName", true);
    public BooleanServerOption hidePlayersBehindYou = new BooleanServerOption(this, "HidePlayersBehindYou", true);
    public DoubleServerOption pvpMeleeDamageModifier = new DoubleServerOption(this, "PVPMeleeDamageModifier", 0.0, 500.0, 30.0);
    public DoubleServerOption pvpFirearmDamageModifier = new DoubleServerOption(this, "PVPFirearmDamageModifier", 0.0, 500.0, 50.0);
    public DoubleServerOption carEngineAttractionModifier = new DoubleServerOption(this, "CarEngineAttractionModifier", 0.0, 10.0, 0.5);
    public BooleanServerOption playerBumpPlayer = new BooleanServerOption(this, "PlayerBumpPlayer", false);
    public IntegerServerOption mapRemotePlayerVisibility = new IntegerServerOption(this, "MapRemotePlayerVisibility", 1, 3, 1);
    public IntegerServerOption backupsCount = new IntegerServerOption(this, "BackupsCount", 1, 300, 5);
    public BooleanServerOption backupsOnStart = new BooleanServerOption(this, "BackupsOnStart", true);
    public BooleanServerOption backupsOnVersionChange = new BooleanServerOption(this, "BackupsOnVersionChange", true);
    public IntegerServerOption backupsPeriod = new IntegerServerOption(this, "BackupsPeriod", 0, 1500, 0);
    public BooleanServerOption disableVehicleTowing = new BooleanServerOption(this, "DisableVehicleTowing", false);
    public BooleanServerOption disableTrailerTowing = new BooleanServerOption(this, "DisableTrailerTowing", false);
    public BooleanServerOption disableBurntTowing = new BooleanServerOption(this, "DisableBurntTowing", false);
    public StringServerOption badWordListFile = new StringServerOption(this, "BadWordListFile", "", -1);
    public StringServerOption goodWordListFile = new StringServerOption(this, "GoodWordListFile", "", -1);
    public EnumServerOption badWordPolicy = new EnumServerOption(this, "BadWordPolicy", 3, 3);
    public StringServerOption badWordReplacement = new StringServerOption(this, "BadWordReplacement", "[HIDDEN]", 16);
    public EnumServerOption antiCheatSafety = new EnumServerOption(this, "AntiCheatSafety", 4, 4);
    public EnumServerOption antiCheatMovement = new EnumServerOption(this, "AntiCheatMovement", 4, 4);
    public EnumServerOption antiCheatHit = new EnumServerOption(this, "AntiCheatHit", 4, 4);
    public EnumServerOption antiCheatPacket = new EnumServerOption(this, "AntiCheatPacket", 4, 4);
    public EnumServerOption antiCheatPermission = new EnumServerOption(this, "AntiCheatPermission", 4, 4);
    public EnumServerOption antiCheatXp = new EnumServerOption(this, "AntiCheatXP", 4, 4);
    public EnumServerOption antiCheatFire = new EnumServerOption(this, "AntiCheatFire", 4, 4);
    public EnumServerOption antiCheatSafeHouse = new EnumServerOption(this, "AntiCheatSafeHouse", 4, 4);
    public EnumServerOption antiCheatRecipe = new EnumServerOption(this, "AntiCheatRecipe", 4, 4);
    public EnumServerOption antiCheatPlayer = new EnumServerOption(this, "AntiCheatPlayer", 4, 4);
    public EnumServerOption antiCheatChecksum = new EnumServerOption(this, "AntiCheatChecksum", 4, 4);
    public EnumServerOption antiCheatItem = new EnumServerOption(this, "AntiCheatItem", 4, 4);
    public EnumServerOption antiCheatServerCustomization = new EnumServerOption(this, "AntiCheatServerCustomization", 4, 4);
    public IntegerServerOption multiplayerStatisticsPeriod = new IntegerServerOption(this, "MultiplayerStatisticsPeriod", 0, 10, 1);
    public BooleanServerOption disableScoreboard = new BooleanServerOption(this, "DisableScoreboard", false);
    public BooleanServerOption hideAdminsInPlayerList = new BooleanServerOption(this, "HideAdminsInPlayerList", false);
    public StringServerOption seed = new StringServerOption(this, "Seed", GameServer.seed, -1);
    public BooleanServerOption usePhysicsHitReaction = new BooleanServerOption(this, "UsePhysicsHitReaction", false);
    public IntegerServerOption chatMessageCharacterLimit = new IntegerServerOption(this, "ChatMessageCharacterLimit", 64, 1024, 200);
    public IntegerServerOption chatMessageSlowModeTime = new IntegerServerOption(this, "ChatMessageSlowModeTime", 1, 30, 3);
    public static ArrayList<String> cardList;

    public ServerOptions() {
        this.publicOptions.clear();
        this.publicOptions.addAll(this.optionByName.keySet());
        this.publicOptions.remove("Password");
        this.publicOptions.remove("RCONPort");
        this.publicOptions.remove("RCONPassword");
        this.publicOptions.remove(this.discordToken.getName());
        this.publicOptions.remove(this.discordChatChannel.getName());
        this.publicOptions.remove(this.discordLogChannel.getName());
        this.publicOptions.remove(this.discordCommandChannel.getName());
        Collections.sort(this.publicOptions);
    }

    private void initOptions() {
        ServerOptions.initClientCommandsHelp();
        for (ServerOption option : this.options) {
            option.asConfigOption().resetToDefault();
        }
    }

    public ArrayList<String> getPublicOptions() {
        return this.publicOptions;
    }

    public ArrayList<ServerOption> getOptions() {
        return this.options;
    }

    public static void initClientCommandsHelp() {
        clientOptionsList = new HashMap();
        clientOptionsList.put("help", Translator.getText("UI_ServerOptionDesc_Help"));
        clientOptionsList.put("changepwd", Translator.getText("UI_ServerOptionDesc_ChangePwd"));
        clientOptionsList.put("roll", Translator.getText("UI_ServerOptionDesc_Roll"));
        clientOptionsList.put("card", Translator.getText("UI_ServerOptionDesc_Card"));
        clientOptionsList.put("safehouse", Translator.getText("UI_ServerOptionDesc_SafeHouse"));
    }

    public void init() {
        File serverOptsFile;
        this.initOptions();
        File serverFolder = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "Server");
        if (!serverFolder.exists()) {
            serverFolder.mkdirs();
        }
        if ((serverOptsFile = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "Server" + File.separator + GameServer.serverName + ".ini")).exists()) {
            try {
                Core.getInstance().loadOptions();
            }
            catch (IOException e) {
                DebugLog.General.printException(e, "Can't load server options", LogSeverity.Error);
            }
            if (this.loadServerTextFile(GameServer.serverName)) {
                this.saveServerTextFile(GameServer.serverName);
            }
        } else {
            this.saveServerTextFile(GameServer.serverName);
        }
        ServerOptions.tryInitSpawnRegionsFile();
        LoggerManager.init();
    }

    public void resetRegionFile() {
        File file = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "Server" + File.separator + GameServer.serverName + "_spawnregions.lua");
        file.delete();
        ServerOptions.tryInitSpawnRegionsFile();
    }

    private static void tryInitSpawnRegionsFile() {
        try {
            File spawnPointsFile;
            File spawnRegionsFile = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "Server" + File.separator + GameServer.serverName + "_spawnregions.lua");
            if (!spawnRegionsFile.exists()) {
                DebugLog.DetailedInfo.trace("creating server spawnregions file \"" + spawnRegionsFile.getPath() + "\"");
                spawnRegionsFile.createNewFile();
                FileWriter fw = new FileWriter(spawnRegionsFile);
                fw.write("function SpawnRegions()" + System.lineSeparator());
                fw.write("\treturn {" + System.lineSeparator());
                fw.write("\t\t{ name = \"Muldraugh, KY\", file = \"media/maps/Muldraugh, KY/spawnpoints.lua\" }," + System.lineSeparator());
                fw.write("\t\t{ name = \"West Point, KY\", file = \"media/maps/West Point, KY/spawnpoints.lua\" }," + System.lineSeparator());
                fw.write("\t\t{ name = \"Rosewood, KY\", file = \"media/maps/Rosewood, KY/spawnpoints.lua\" }," + System.lineSeparator());
                fw.write("\t\t{ name = \"Riverside, KY\", file = \"media/maps/Riverside, KY/spawnpoints.lua\" }," + System.lineSeparator());
                fw.write("\t\t-- Uncomment the line below to add a custom spawnpoint for this server." + System.lineSeparator());
                fw.write("--\t\t{ name = \"Twiggy's Bar\", serverfile = \"" + GameServer.serverName + "_spawnpoints.lua\" }," + System.lineSeparator());
                fw.write("\t}" + System.lineSeparator());
                fw.write("end" + System.lineSeparator());
                fw.close();
            }
            if (!(spawnPointsFile = new File(spawnRegionsFile.getParent() + File.separator + GameServer.serverName + "_spawnpoints.lua")).exists()) {
                DebugLog.DetailedInfo.trace("creating server spawnpoints file \"" + spawnRegionsFile.getPath() + "\"");
                spawnPointsFile.createNewFile();
                FileWriter fw = new FileWriter(spawnPointsFile);
                fw.write("function SpawnPoints()" + System.lineSeparator());
                fw.write("\treturn {" + System.lineSeparator());
                fw.write("\t\tunemployed = {" + System.lineSeparator());
                fw.write("\t\t\t{ worldX = 40, worldY = 22, posX = 67, posY = 201 }" + System.lineSeparator());
                fw.write("\t\t}" + System.lineSeparator());
                fw.write("\t}" + System.lineSeparator());
                fw.write("end" + System.lineSeparator());
                fw.close();
            }
        }
        catch (Exception e) {
            DebugLog.General.printException(e, "Can't initialize spawn regions or spawn points", LogSeverity.Error);
        }
    }

    public String getOption(String key) {
        ServerOption option = this.getOptionByName(key);
        return option == null ? null : option.asConfigOption().getValueAsString();
    }

    public Boolean getBoolean(String key) {
        ServerOption option = this.getOptionByName(key);
        if (option instanceof BooleanServerOption) {
            BooleanServerOption booleanServerOption = (BooleanServerOption)option;
            return (Boolean)booleanServerOption.getValueAsObject();
        }
        return null;
    }

    public Float getFloat(String key) {
        ServerOption option = this.getOptionByName(key);
        if (option instanceof DoubleServerOption) {
            DoubleServerOption doubleServerOption = (DoubleServerOption)option;
            return Float.valueOf((float)doubleServerOption.getValue());
        }
        return null;
    }

    public Double getDouble(String key) {
        ServerOption option = this.getOptionByName(key);
        if (option instanceof DoubleServerOption) {
            DoubleServerOption doubleServerOption = (DoubleServerOption)option;
            return doubleServerOption.getValue();
        }
        return null;
    }

    public Integer getInteger(String key) {
        ServerOption option = this.getOptionByName(key);
        if (option instanceof IntegerServerOption) {
            IntegerServerOption integerServerOption = (IntegerServerOption)option;
            return integerServerOption.getValue();
        }
        return null;
    }

    public void putOption(String key, String value) {
        ServerOption option = this.getOptionByName(key);
        if (option != null) {
            option.asConfigOption().parse(value);
        }
    }

    public void putSaveOption(String key, String value) {
        this.putOption(key, value);
        this.saveServerTextFile(GameServer.serverName);
    }

    public String changeOption(String key, String value) {
        ServerOption option = this.getOptionByName(key);
        if (option == null) {
            return "Option " + key + " doesn't exist.";
        }
        option.asConfigOption().parse(value);
        if (!this.saveServerTextFile(GameServer.serverName)) {
            return "An error as occured.";
        }
        return "Option : " + key + " is now : " + option.asConfigOption().getValueAsString();
    }

    public static ServerOptions getInstance() {
        return instance;
    }

    public static ArrayList<String> getClientCommandList(boolean doLine) {
        String carriageReturn = " <LINE> ";
        if (!doLine) {
            carriageReturn = "\n";
        }
        if (clientOptionsList == null) {
            ServerOptions.initClientCommandsHelp();
        }
        ArrayList<String> result = new ArrayList<String>();
        Iterator<String> it = clientOptionsList.keySet().iterator();
        result.add("List of commands : " + carriageReturn);
        while (it.hasNext()) {
            String key = it.next();
            result.add("* " + key + " : " + clientOptionsList.get(key) + (it.hasNext() ? carriageReturn : ""));
        }
        return result;
    }

    public static String getRandomCard() {
        if (cardList == null) {
            cardList = new ArrayList();
            cardList.add("the Ace of Clubs");
            cardList.add("a Two of Clubs");
            cardList.add("a Three of Clubs");
            cardList.add("a Four of Clubs");
            cardList.add("a Five of Clubs");
            cardList.add("a Six of Clubs");
            cardList.add("a Seven of Clubs");
            cardList.add("an Eight of Clubs");
            cardList.add("a Nine of Clubs");
            cardList.add("a Ten of Clubs");
            cardList.add("the Jack of Clubs");
            cardList.add("the Queen of Clubs");
            cardList.add("the King of Clubs");
            cardList.add("the Ace of Diamonds");
            cardList.add("a Two of Diamonds");
            cardList.add("a Three of Diamonds");
            cardList.add("a Four of Diamonds");
            cardList.add("a Five of Diamonds");
            cardList.add("a Six of Diamonds");
            cardList.add("a Seven of Diamonds");
            cardList.add("an Eight of Diamonds");
            cardList.add("a Nine of Diamonds");
            cardList.add("a Ten of Diamonds");
            cardList.add("the Jack of Diamonds");
            cardList.add("the Queen of Diamonds");
            cardList.add("the King of Diamonds");
            cardList.add("the Ace of Hearts");
            cardList.add("a Two of Hearts");
            cardList.add("a Three of Hearts");
            cardList.add("a Four of Hearts");
            cardList.add("a Five of Hearts");
            cardList.add("a Six of Hearts");
            cardList.add("a Seven of Hearts");
            cardList.add("an Eight of Hearts");
            cardList.add("a Nine of Hearts");
            cardList.add("a Ten of Hearts");
            cardList.add("the Jack of Hearts");
            cardList.add("the Queen of Hearts");
            cardList.add("the King of Hearts");
            cardList.add("the Ace of Spades");
            cardList.add("a Two of Spades");
            cardList.add("a Three of Spades");
            cardList.add("a Four of Spades");
            cardList.add("a Five of Spades");
            cardList.add("a Six of Spades");
            cardList.add("a Seven of Spades");
            cardList.add("an Eight of Spades");
            cardList.add("a Nine of Spades");
            cardList.add("a Ten of Spades");
            cardList.add("the Jack of Spades");
            cardList.add("the Queen of Spades");
            cardList.add("the King of Spades");
        }
        return cardList.get(Rand.Next(cardList.size()));
    }

    public void addOption(ServerOption option) {
        if (this.optionByName.containsKey(option.asConfigOption().getName())) {
            throw new IllegalArgumentException();
        }
        this.options.add(option);
        this.optionByName.put(option.asConfigOption().getName(), option);
    }

    public int getNumOptions() {
        return this.options.size();
    }

    public ServerOption getOptionByIndex(int index) {
        return this.options.get(index);
    }

    public ServerOption getOptionByName(String name) {
        return this.optionByName.get(name);
    }

    public boolean loadServerTextFile(String serverName) {
        ConfigFile configFile = new ConfigFile();
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "Server" + File.separator + serverName + ".ini";
        if (configFile.read(fileName)) {
            for (ConfigOption configOption : configFile.getOptions()) {
                ServerOption option = this.optionByName.get(configOption.getName());
                if (option == null) continue;
                option.asConfigOption().parse(configOption.getValueAsString());
            }
            return true;
        }
        return false;
    }

    public boolean saveServerTextFile(String serverName) {
        ConfigFile configFile = new ConfigFile();
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "Server" + File.separator + serverName + ".ini";
        ArrayList<ConfigOption> configOptions = new ArrayList<ConfigOption>();
        for (ServerOption option : this.options) {
            configOptions.add(option.asConfigOption());
        }
        return configFile.write(fileName, 0, configOptions);
    }

    public int getMaxPlayers() {
        return Math.min(100, ServerOptions.getInstance().maxPlayers.getValue());
    }

    @UsedFromLua
    public static class BooleanServerOption
    extends BooleanConfigOption
    implements ServerOption {
        public BooleanServerOption(ServerOptions owner, String name, boolean defaultValue) {
            super(name, defaultValue);
            owner.addOption(this);
        }

        @Override
        public ConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getTooltip() {
            return Translator.getTextOrNull("UI_ServerOption_" + this.name + "_tooltip");
        }
    }

    @UsedFromLua
    public static class StringServerOption
    extends StringConfigOption
    implements ServerOption {
        public StringServerOption(ServerOptions owner, String name, String defaultValue, int maxLength) {
            super(name, defaultValue, maxLength);
            owner.addOption(this);
        }

        @Override
        public ConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getTooltip() {
            return Translator.getTextOrNull("UI_ServerOption_" + this.name + "_tooltip");
        }
    }

    @UsedFromLua
    public static class TextServerOption
    extends StringConfigOption
    implements ServerOption {
        public TextServerOption(ServerOptions owner, String name, String defaultValue, int maxLength) {
            super(name, defaultValue, maxLength);
            owner.addOption(this);
        }

        @Override
        public String getType() {
            return "text";
        }

        @Override
        public ConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getTooltip() {
            return Translator.getTextOrNull("UI_ServerOption_" + this.name + "_tooltip");
        }
    }

    @UsedFromLua
    public static class IntegerServerOption
    extends IntegerConfigOption
    implements ServerOption {
        public IntegerServerOption(ServerOptions owner, String name, int min, int max, int defaultValue) {
            super(name, min, max, defaultValue);
            owner.addOption(this);
        }

        @Override
        public ConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getTooltip() {
            String s1 = Translator.getTextOrNull("UI_ServerOption_" + this.name + "_tooltip");
            String s2 = Translator.getText("Sandbox_MinMaxDefault", this.min, this.max, this.defaultValue);
            if (s1 == null) {
                return s2;
            }
            if (s2 == null) {
                return s1;
            }
            return s1 + "\\n" + s2;
        }
    }

    @UsedFromLua
    public static class DoubleServerOption
    extends DoubleConfigOption
    implements ServerOption {
        public DoubleServerOption(ServerOptions owner, String name, double min, double max, double defaultValue) {
            super(name, min, max, defaultValue);
            owner.addOption(this);
        }

        @Override
        public ConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getTooltip() {
            String s1 = Translator.getTextOrNull("UI_ServerOption_" + this.name + "_tooltip");
            String s2 = Translator.getText("Sandbox_MinMaxDefault", String.format("%.02f", this.min), String.format("%.02f", this.max), String.format("%.02f", this.defaultValue));
            if (s1 == null) {
                return s2;
            }
            if (s2 == null) {
                return s1;
            }
            return s1 + "\\n" + s2;
        }
    }

    @UsedFromLua
    public static class EnumServerOption
    extends EnumConfigOption
    implements ServerOption {
        public EnumServerOption(ServerOptions owner, String name, int numValues, int defaultValue) {
            super(name, numValues, defaultValue);
            owner.addOption(this);
        }

        @Override
        public ConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getTooltip() {
            return Translator.getTextOrNull("UI_ServerOption_" + this.name + "_tooltip");
        }

        public String getValueTranslationByIndex(int index) {
            if (index < 1 || index > this.getNumValues()) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return Translator.getTextOrNull("UI_ServerOption_AntiCheat_option" + index);
        }
    }

    public static interface ServerOption {
        public ConfigOption asConfigOption();

        public String getTooltip();
    }
}

