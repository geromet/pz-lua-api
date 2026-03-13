/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio;

import java.util.ArrayList;
import java.util.HashMap;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.audio.MusicIntensityEvent;
import zombie.audio.MusicIntensityEvents;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.util.StringUtils;
import zombie.util.Type;

@UsedFromLua
public final class MusicIntensityConfig {
    private static MusicIntensityConfig instance;
    private final ArrayList<Event> events = new ArrayList();
    private final HashMap<String, Event> eventById = new HashMap();

    public static MusicIntensityConfig getInstance() {
        if (instance == null) {
            instance = new MusicIntensityConfig();
        }
        return instance;
    }

    public void initEvents(KahluaTableImpl eventsTable) {
        this.events.clear();
        this.eventById.clear();
        KahluaTableIterator it = eventsTable.iterator();
        while (it.advance()) {
            String key = it.getKey().toString();
            if ("VERSION".equalsIgnoreCase(key)) continue;
            KahluaTableImpl eventTable = (KahluaTableImpl)it.getValue();
            Event event = new Event();
            event.id = StringUtils.discardNullOrWhitespace(eventTable.rawgetStr("id"));
            event.intensity = eventTable.rawgetFloat("intensity");
            event.duration = eventTable.rawgetInt("duration");
            Object value = eventTable.rawget("multiple");
            if (value instanceof Boolean) {
                Boolean b = (Boolean)value;
                event.multiple = b;
            }
            if (event.id == null) continue;
            if (this.eventById.containsKey(event.id)) {
                this.events.remove(this.eventById.get(event.id));
            }
            this.events.add(event);
            this.eventById.put(event.id, event);
        }
    }

    public MusicIntensityEvent triggerEvent(String id, MusicIntensityEvents mie) {
        Event event = this.eventById.get(id);
        if (event == null) {
            return null;
        }
        return mie.addEvent(event.id, event.intensity, event.duration, event.multiple);
    }

    public void checkHealthPanelVisible(IsoGameCharacter character) {
        if (!(character instanceof IsoPlayer)) {
            return;
        }
        IsoPlayer player = (IsoPlayer)character;
        this.checkHealthPanel_SeeBite(player);
    }

    private void checkHealthPanel_SeeBite(IsoPlayer player) {
        Object worldAgeHours = player.getMusicIntensityEventModData("HealthPanel_SeeBite");
        if (worldAgeHours != null) {
            return;
        }
        BodyDamage bodyDamage = player.getBodyDamage();
        boolean bBitten = false;
        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); ++i) {
            if (!bodyDamage.IsBitten(i)) continue;
            bBitten = true;
            break;
        }
        if (!bBitten) {
            return;
        }
        player.setMusicIntensityEventModData("HealthPanel_SeeBite", GameTime.getInstance().getWorldAgeHours());
        player.triggerMusicIntensityEvent("HealthPanel_SeeBite");
    }

    public void restoreToFullHealth(IsoGameCharacter character) {
        IsoPlayer player = Type.tryCastTo(character, IsoPlayer.class);
        if (player == null || !player.hasModData()) {
            return;
        }
        player.setMusicIntensityEventModData("HealthPanel_SeeBite", null);
    }

    private static final class Event {
        String id;
        float intensity;
        long duration;
        boolean multiple = true;

        private Event() {
        }
    }
}

