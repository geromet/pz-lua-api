/*
 * Decompiled with CFR 0.152.
 */
package zombie;

import fmod.fmod.FMODFootstep;
import fmod.fmod.FMODManager;
import fmod.fmod.FMODSoundBank;
import fmod.fmod.FMODVoice;
import fmod.fmod.FMOD_STUDIO_EVENT_DESCRIPTION;
import fmod.fmod.FMOD_STUDIO_PLAYBACK_STATE;
import fmod.javafmod;
import fmod.javafmodJNI;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.audio.BaseSoundBank;
import zombie.audio.GameSound;
import zombie.audio.GameSoundClip;
import zombie.characters.IsoPlayer;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.GameSoundScript;
import zombie.util.StringUtils;

@UsedFromLua
public final class GameSounds {
    public static final int VERSION = 1;
    protected static final HashMap<String, GameSound> soundByName = new HashMap();
    protected static final ArrayList<GameSound> sounds = new ArrayList();
    private static final BankPreviewSound previewBank = new BankPreviewSound();
    private static final FilePreviewSound previewFile = new FilePreviewSound();
    public static boolean soundIsPaused;
    private static IPreviewSound previewSound;
    public static final boolean VCA_VOLUME = true;
    private static int missingEventCount;

    public static void addSound(GameSound sound) {
        GameSounds.initClipEvents(sound);
        assert (!sounds.contains(sound));
        int index = sounds.size();
        if (soundByName.containsKey(sound.getName())) {
            for (index = 0; index < sounds.size() && !sounds.get(index).getName().equals(sound.getName()); ++index) {
            }
            sounds.remove(index);
        }
        sounds.add(index, sound);
        soundByName.put(sound.getName(), sound);
    }

    private static void initClipEvents(GameSound sound) {
        if (GameServer.server) {
            return;
        }
        for (GameSoundClip clip : sound.clips) {
            if (clip.event == null || clip.eventDescription != null) continue;
            clip.eventDescription = FMODManager.instance.getEventDescription("event:/" + clip.event);
            if (clip.eventDescription == null) {
                DebugLog.Sound.println("No such FMOD event \"%s\" for GameSound \"%s\"", clip.event, sound.getName());
                ++missingEventCount;
            }
            clip.eventDescriptionMp = FMODManager.instance.getEventDescription("event:/Remote/" + clip.event);
            if (clip.eventDescriptionMp == null) continue;
            DebugLog.Sound.println("MP event %s", clip.eventDescriptionMp.path);
        }
    }

    public static boolean isKnownSound(String name) {
        return soundByName.containsKey(name);
    }

    public static GameSound getSound(String name) {
        return GameSounds.getOrCreateSound(name);
    }

    public static GameSound getOrCreateSound(String name) {
        if (StringUtils.isNullOrEmpty(name)) {
            return null;
        }
        GameSound gameSound = soundByName.get(name);
        if (gameSound == null) {
            DebugLog.Sound.warn("no GameSound called \"" + name + "\", adding a new one");
            gameSound = new GameSound();
            gameSound.name = name;
            gameSound.category = "AUTO";
            GameSoundClip clip = new GameSoundClip(gameSound);
            gameSound.clips.add(clip);
            sounds.add(gameSound);
            soundByName.put(name.replace(".wav", "").replace(".ogg", ""), gameSound);
            if (BaseSoundBank.instance instanceof FMODSoundBank) {
                FMOD_STUDIO_EVENT_DESCRIPTION eventDescription = FMODManager.instance.getEventDescription("event:/" + name);
                if (eventDescription != null) {
                    clip.event = name;
                    clip.eventDescription = eventDescription;
                    clip.eventDescriptionMp = FMODManager.instance.getEventDescription("event:/Remote/" + name);
                } else {
                    long sound;
                    String path = null;
                    if (ZomboidFileSystem.instance.getAbsolutePath("media/sound/" + name + ".ogg") != null) {
                        path = "media/sound/" + name + ".ogg";
                    } else if (ZomboidFileSystem.instance.getAbsolutePath("media/sound/" + name + ".wav") != null) {
                        path = "media/sound/" + name + ".wav";
                    }
                    if (path != null && (sound = FMODManager.instance.loadSound(path)) != 0L) {
                        clip.file = path;
                    }
                }
                if (clip.event == null && clip.file == null) {
                    DebugLog.Sound.warn("couldn't find an FMOD event or .ogg or .wav file for sound \"" + name + "\"");
                }
            }
        }
        return gameSound;
    }

    private static void loadNonBankSounds() {
        if (!(BaseSoundBank.instance instanceof FMODSoundBank)) {
            return;
        }
        for (GameSound sound : sounds) {
            for (GameSoundClip clip : sound.clips) {
                if (clip.getFile() != null && !clip.getFile().isEmpty()) continue;
            }
        }
    }

    public static void ScriptsLoaded() {
        Object scriptSound;
        ArrayList<GameSoundScript> scriptSounds = ScriptManager.instance.getAllGameSounds();
        for (int i = 0; i < scriptSounds.size(); ++i) {
            scriptSound = scriptSounds.get(i);
            if (((GameSoundScript)scriptSound).gameSound.clips.isEmpty()) continue;
            GameSounds.addSound(((GameSoundScript)scriptSound).gameSound);
        }
        scriptSounds.clear();
        GameSounds.loadNonBankSounds();
        GameSounds.loadINI();
        if (Core.debug && (scriptSound = BaseSoundBank.instance) instanceof FMODSoundBank) {
            FMODSoundBank bank = (FMODSoundBank)scriptSound;
            HashSet<String> usedEvents = new HashSet<String>();
            for (GameSound gameSound : sounds) {
                for (GameSoundClip clip : gameSound.clips) {
                    if (clip.getEvent() == null || clip.getEvent().isEmpty()) continue;
                    usedEvents.add(clip.getEvent());
                }
            }
            for (FMODFootstep footstep2 : bank.footstepMap.values()) {
                usedEvents.add(footstep2.wood);
                usedEvents.add(footstep2.concrete);
                usedEvents.add(footstep2.grass);
                usedEvents.add(footstep2.upstairs);
                usedEvents.add(footstep2.woodCreak);
            }
            for (FMODVoice voice : bank.voiceMap.values()) {
                usedEvents.add(voice.sound);
            }
            ArrayList<String> unusedEvents = new ArrayList<String>();
            long[] bankList = new long[32];
            long[] eventDescList = new long[1024];
            int bankCount = javafmodJNI.FMOD_Studio_System_GetBankList(bankList);
            for (int i = 0; i < bankCount; ++i) {
                int eventDescCount = javafmodJNI.FMOD_Studio_Bank_GetEventList(bankList[i], eventDescList);
                for (int j = 0; j < eventDescCount; ++j) {
                    try {
                        String name = javafmodJNI.FMOD_Studio_EventDescription_GetPath(eventDescList[j]);
                        name = name.replace("event:/", "");
                        if (usedEvents.contains(name)) continue;
                        unusedEvents.add(name);
                        continue;
                    }
                    catch (Exception e) {
                        DebugLog.Sound.warn("FMOD cannot get path for " + eventDescList[j] + " event");
                    }
                }
            }
            unusedEvents.sort(String::compareTo);
            if (DebugLog.isEnabled(DebugType.Sound)) {
                for (String event : unusedEvents) {
                    DebugLog.Sound.warn("FMOD event \"%s\" not used by any GameSound", event);
                }
            } else {
                DebugLog.Sound.warn("FMOD %s missing events", missingEventCount);
                DebugLog.Sound.warn("FMOD %s events not used by any GameSound", unusedEvents.size());
                DebugLog.Sound.warn("FMOD [Turn on DebugType.Sound for detailed lists of missing and unused events]", unusedEvents.size());
            }
        }
    }

    public static void OnReloadSound(GameSoundScript scriptSound) {
        if (sounds.contains(scriptSound.gameSound)) {
            GameSounds.initClipEvents(scriptSound.gameSound);
            return;
        }
        if (scriptSound.gameSound.clips.isEmpty()) {
            return;
        }
        GameSounds.addSound(scriptSound.gameSound);
    }

    public static ArrayList<String> getCategories() {
        HashSet<String> categories = new HashSet<String>();
        for (GameSound sound : sounds) {
            categories.add(sound.getCategory());
        }
        ArrayList<String> sorted2 = new ArrayList<String>(categories);
        Collections.sort(sorted2);
        return sorted2;
    }

    public static ArrayList<GameSound> getSoundsInCategory(String category) {
        ArrayList<GameSound> result = new ArrayList<GameSound>();
        for (GameSound sound : sounds) {
            if (!sound.getCategory().equals(category)) continue;
            result.add(sound);
        }
        return result;
    }

    public static void loadINI() {
        ConfigFile configFile = new ConfigFile();
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "sounds.ini";
        if (!configFile.read(fileName)) {
            return;
        }
        if (configFile.getVersion() > 1) {
            return;
        }
        for (ConfigOption option : configFile.getOptions()) {
            GameSound gameSound = soundByName.get(option.getName());
            if (gameSound == null) continue;
            gameSound.setUserVolume(PZMath.tryParseFloat(option.getValueAsString(), 1.0f));
        }
    }

    public static void saveINI() {
        ArrayList<DoubleConfigOption> options = new ArrayList<DoubleConfigOption>();
        for (GameSound gameSound : sounds) {
            DoubleConfigOption option = new DoubleConfigOption(gameSound.getName(), 0.0, 2.0, 0.0);
            option.setValue(gameSound.getUserVolume());
            options.add(option);
        }
        ConfigFile configFile = new ConfigFile();
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "sounds.ini";
        if (!configFile.write(fileName, 1, options)) {
            return;
        }
        options.clear();
    }

    public static void previewSound(String name) {
        if (Core.soundDisabled) {
            DebugLog.Sound.printf("sound is disabled, not playing " + name, new Object[0]);
            return;
        }
        if (!GameSounds.isKnownSound(name)) {
            DebugLog.Sound.warn("sound is not known, not playing " + name);
            return;
        }
        GameSound gameSound = GameSounds.getSound(name);
        if (gameSound == null) {
            DebugLog.Sound.warn("no such GameSound " + name);
            return;
        }
        GameSoundClip clip = gameSound.getRandomClip();
        if (clip == null) {
            DebugLog.Sound.warn("GameSound.clips is empty");
            return;
        }
        if (soundIsPaused) {
            if (!GameClient.client) {
                long channelGroup = javafmod.FMOD_System_GetMasterChannelGroup();
                javafmod.FMOD_ChannelGroup_SetVolume(channelGroup, 1.0f);
            }
            soundIsPaused = false;
        }
        if (previewSound != null) {
            previewSound.stop();
        }
        if (clip.getEvent() != null) {
            if (previewBank.play(clip)) {
                previewSound = previewBank;
            }
        } else if (clip.getFile() != null && previewFile.play(clip)) {
            previewSound = previewFile;
        }
    }

    public static void stopPreview() {
        if (previewSound == null) {
            return;
        }
        previewSound.stop();
        previewSound = null;
    }

    public static boolean isPreviewPlaying() {
        if (previewSound == null) {
            return false;
        }
        if (previewSound.update()) {
            previewSound = null;
            return false;
        }
        return previewSound.isPlaying();
    }

    public static void fix3DListenerPosition(boolean inMenu) {
        if (Core.soundDisabled) {
            return;
        }
        if (inMenu) {
            javafmod.FMOD_Studio_Listener3D(0, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f, 1.0f);
        } else {
            for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player == null || player.hasTrait(CharacterTrait.DEAF)) continue;
                javafmod.FMOD_Studio_Listener3D(i, player.getX(), player.getY(), player.getZ() * 3.0f, 0.0f, 0.0f, 0.0f, -1.0f / (float)Math.sqrt(2.0), -1.0f / (float)Math.sqrt(2.0), 0.0f, 0.0f, 0.0f, 1.0f);
            }
        }
    }

    public static void Reset() {
        sounds.clear();
        soundByName.clear();
        if (previewSound != null) {
            previewSound.stop();
            previewSound = null;
        }
    }

    private static interface IPreviewSound {
        public boolean play(GameSoundClip var1);

        public boolean isPlaying();

        public boolean update();

        public void stop();
    }

    private static final class BankPreviewSound
    implements IPreviewSound {
        long instance;
        GameSoundClip clip;
        float effectiveGain;

        private BankPreviewSound() {
        }

        @Override
        public boolean play(GameSoundClip clip) {
            if (clip.eventDescription == null) {
                DebugLog.Sound.error("failed to get event " + clip.getEvent());
                return false;
            }
            this.instance = javafmod.FMOD_Studio_System_CreateEventInstance(clip.eventDescription.address);
            if (this.instance < 0L) {
                DebugLog.Sound.error("failed to create EventInstance: error=" + this.instance);
                this.instance = 0L;
                return false;
            }
            this.clip = clip;
            this.effectiveGain = clip.getEffectiveVolumeInMenu();
            javafmod.FMOD_Studio_EventInstance_SetVolume(this.instance, this.effectiveGain);
            javafmod.FMOD_Studio_EventInstance_SetParameterByName(this.instance, "Occlusion", 0.0f);
            javafmod.FMOD_Studio_StartEvent(this.instance);
            if (clip.gameSound.master == GameSound.MasterVolume.Music) {
                javafmod.FMOD_Studio_EventInstance_SetParameterByName(this.instance, "Volume", 10.0f);
            }
            return true;
        }

        @Override
        public boolean isPlaying() {
            if (this.instance == 0L) {
                return false;
            }
            int state = javafmod.FMOD_Studio_GetPlaybackState(this.instance);
            if (state == FMOD_STUDIO_PLAYBACK_STATE.FMOD_STUDIO_PLAYBACK_STOPPING.index) {
                return true;
            }
            return state != FMOD_STUDIO_PLAYBACK_STATE.FMOD_STUDIO_PLAYBACK_STOPPED.index;
        }

        @Override
        public boolean update() {
            if (this.instance == 0L) {
                return false;
            }
            int state = javafmod.FMOD_Studio_GetPlaybackState(this.instance);
            if (state == FMOD_STUDIO_PLAYBACK_STATE.FMOD_STUDIO_PLAYBACK_STOPPING.index) {
                return false;
            }
            if (state == FMOD_STUDIO_PLAYBACK_STATE.FMOD_STUDIO_PLAYBACK_STOPPED.index) {
                javafmod.FMOD_Studio_ReleaseEventInstance(this.instance);
                this.instance = 0L;
                this.clip = null;
                return true;
            }
            float targetGain = this.clip.getEffectiveVolumeInMenu();
            if (this.effectiveGain != targetGain) {
                this.effectiveGain = targetGain;
                javafmod.FMOD_Studio_EventInstance_SetVolume(this.instance, this.effectiveGain);
            }
            return false;
        }

        @Override
        public void stop() {
            if (this.instance == 0L) {
                return;
            }
            javafmod.FMOD_Studio_EventInstance_Stop(this.instance, false);
            javafmod.FMOD_Studio_ReleaseEventInstance(this.instance);
            this.instance = 0L;
            this.clip = null;
        }
    }

    private static final class FilePreviewSound
    implements IPreviewSound {
        long channel;
        GameSoundClip clip;
        float effectiveGain;

        private FilePreviewSound() {
        }

        @Override
        public boolean play(GameSoundClip clip) {
            GameSound gameSound = clip.gameSound;
            long sound = FMODManager.instance.loadSound(clip.getFile(), gameSound.isLooped());
            if (sound == 0L) {
                return false;
            }
            this.channel = javafmod.FMOD_System_PlaySound(sound, true);
            this.clip = clip;
            this.effectiveGain = clip.getEffectiveVolumeInMenu();
            javafmod.FMOD_Channel_SetVolume(this.channel, this.effectiveGain);
            javafmod.FMOD_Channel_SetPitch(this.channel, clip.pitch);
            if (gameSound.isLooped()) {
                javafmod.FMOD_Channel_SetMode(this.channel, 2L);
            }
            javafmod.FMOD_Channel_SetPaused(this.channel, false);
            return true;
        }

        @Override
        public boolean isPlaying() {
            if (this.channel == 0L) {
                return false;
            }
            return javafmod.FMOD_Channel_IsPlaying(this.channel);
        }

        @Override
        public boolean update() {
            if (this.channel == 0L) {
                return false;
            }
            if (!javafmod.FMOD_Channel_IsPlaying(this.channel)) {
                this.channel = 0L;
                this.clip = null;
                return true;
            }
            float targetGain = this.clip.getEffectiveVolumeInMenu();
            if (this.effectiveGain != targetGain) {
                this.effectiveGain = targetGain;
                javafmod.FMOD_Channel_SetVolume(this.channel, this.effectiveGain);
            }
            return false;
        }

        @Override
        public void stop() {
            if (this.channel == 0L) {
                return;
            }
            javafmod.FMOD_Channel_Stop(this.channel);
            this.channel = 0L;
            this.clip = null;
        }
    }
}

