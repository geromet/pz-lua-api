/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.AnimalCell;
import zombie.characters.animals.AnimalManagerWorker;
import zombie.characters.animals.AnimalTracksDefinitions;
import zombie.characters.animals.VirtualAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoAnimalTrack;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

@UsedFromLua
public class AnimalTracks {
    public String animalType;
    public long addedTime;
    public String trackType;
    public IsoDirections dir;
    public int x;
    public int y;
    public int minSkill;
    public boolean addedToWorld;
    private InventoryItem item;

    public static AnimalTracks addAnimalTrack(VirtualAnimal animal, AnimalTracksDefinitions.AnimalTracksType trackType) {
        AnimalTracks track = new AnimalTracks();
        track.animalType = animal.migrationGroup;
        track.trackType = trackType.type;
        if (trackType.needDir) {
            track.dir = IsoDirections.fromAngle(animal.forwardDirection.x, animal.forwardDirection.y);
        }
        track.x = Rand.Next((int)animal.x - 2, (int)animal.x + 2);
        track.y = Rand.Next((int)animal.y - 2, (int)animal.y + 2);
        track.addedTime = GameTime.getInstance().getCalender().getTimeInMillis();
        return track;
    }

    public static AnimalTracks addAnimalTrackAtPos(VirtualAnimal animal, int x, int y, AnimalTracksDefinitions.AnimalTracksType trackType, long timeMinus) {
        AnimalTracks track = new AnimalTracks();
        track.animalType = animal.migrationGroup;
        track.trackType = trackType.type;
        if (trackType.needDir) {
            track.dir = IsoDirections.fromAngle(animal.forwardDirection.x, animal.forwardDirection.y);
        }
        track.x = x;
        track.y = y;
        track.addedTime = GameTime.getInstance().getCalender().getTimeInMillis() - timeMinus;
        return track;
    }

    public boolean canFindTrack(IsoGameCharacter chr) {
        if (this.addedToWorld) {
            return true;
        }
        if (chr == null || chr.getCurrentSquare() == null) {
            return false;
        }
        AnimalTracksDefinitions def = AnimalTracksDefinitions.tracksDefinitions.get(this.animalType);
        if (def == null) {
            return false;
        }
        AnimalTracksDefinitions.AnimalTracksType track = def.tracks.get(this.trackType);
        if (track == null) {
            return false;
        }
        if (chr.getPerkLevel(PerkFactory.Perks.Tracking) >= track.minSkill) {
            float totalChance = def.chanceToFindTrack + track.chanceToFindTrack;
            totalChance /= (float)(chr.getPerkLevel(PerkFactory.Perks.Tracking) + 1) / 0.7f;
            float dist = chr.getCurrentSquare().DistToProper(this.x, this.y);
            if (dist < 20.0f) {
                dist = (20.0f - dist) / 20.0f;
                totalChance /= dist + 2.0f;
            }
            if (dist < 4.0f) {
                totalChance /= 20.0f;
            }
            if (!Rand.NextBool((int)totalChance)) {
                this.addTrackingExp(chr, false);
                return false;
            }
            this.addTrackingExp(chr, true);
            return true;
        }
        return false;
    }

    public void addTrackingExp(IsoGameCharacter chr, boolean success) {
        if (success) {
            if (GameServer.server) {
                GameServer.addXp((IsoPlayer)chr, PerkFactory.Perks.Tracking, Rand.Next(7.0f, 15.0f));
            } else if (!GameClient.client) {
                chr.getXp().AddXP(PerkFactory.Perks.Tracking, Rand.Next(7.0f, 15.0f));
            }
        } else if (GameServer.server) {
            if (Rand.NextBool(10)) {
                GameServer.addXp((IsoPlayer)chr, PerkFactory.Perks.Tracking, Rand.Next(2.0f, 4.0f));
            }
        } else if (!GameClient.client && Rand.NextBool(10)) {
            chr.getXp().AddXP(PerkFactory.Perks.Tracking, Rand.Next(2.0f, 4.0f));
        }
    }

    public static String getTrackStr(String trackType) {
        return Translator.getText("IGUI_AnimalTracks_" + trackType);
    }

    public static ArrayList<AnimalTracks> getAndFindNearestTracks(IsoGameCharacter character) {
        ArrayList<AnimalTracks> result = AnimalTracks.getNearestTracks((int)character.getX(), (int)character.getY(), 20 + character.getPerkLevel(PerkFactory.Perks.Tracking) * 2);
        if (result == null) {
            return null;
        }
        for (AnimalTracks track : result) {
            if (!track.canFindTrack(character)) continue;
            if (track.isItem()) {
                track.addItemToWorld();
            } else {
                track.addToWorld();
            }
            if (!GameServer.server) continue;
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalTracks, track.x, (float)track.y, character, track, track.getItem());
        }
        return result;
    }

    public static ArrayList<AnimalTracks> getNearestTracks(int x, int y, int radius) {
        ArrayList<AnimalTracks> result = new ArrayList<AnimalTracks>();
        AnimalCell cell = AnimalManagerWorker.getInstance().getCellFromSquarePos(PZMath.fastfloor(x), PZMath.fastfloor(y));
        if (cell == null) {
            return null;
        }
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, 0);
        if (sq == null) {
            return null;
        }
        AnimalManagerWorker.getInstance().loadIfNeeded(cell);
        for (int i = 0; i < cell.chunks.length; ++i) {
            if (cell.chunks[i] == null || cell.chunks[i].animalTracks.isEmpty()) continue;
            for (int j = 0; j < cell.chunks[i].animalTracks.size(); ++j) {
                AnimalTracks trackTest = cell.chunks[i].animalTracks.get(j);
                if (!(sq.DistToProper(trackTest.x, trackTest.y) <= (float)radius)) continue;
                result.add(trackTest);
            }
        }
        return result;
    }

    public void save(ByteBuffer output) throws IOException {
        GameWindow.WriteString(output, this.animalType);
        GameWindow.WriteString(output, this.trackType);
        output.putInt(this.x);
        output.putInt(this.y);
        if (this.dir != null) {
            output.put((byte)1);
            output.putInt(this.dir.ordinal());
        } else {
            output.put((byte)0);
        }
        output.putLong(this.addedTime);
        output.put(this.addedToWorld ? (byte)1 : 0);
    }

    public void load(ByteBuffer input, int worldVersion) throws IOException {
        this.animalType = GameWindow.ReadString(input);
        this.trackType = GameWindow.ReadString(input);
        this.x = input.getInt();
        this.y = input.getInt();
        if (input.get() != 0) {
            this.dir = IsoDirections.fromIndex(input.getInt());
        }
        this.addedTime = input.getLong();
        this.addedToWorld = input.get() != 0;
    }

    public String getTrackType() {
        return this.trackType;
    }

    public String getTrackAge(IsoGameCharacter chr) {
        return PZMath.fastfloor((GameTime.getInstance().getCalender().getTimeInMillis() - this.addedTime) / 60000L) + " mins ago";
    }

    public IsoDirections getDir() {
        return this.dir;
    }

    public int getMinSkill() {
        return this.minSkill;
    }

    public String getTrackItem() {
        AnimalTracksDefinitions.AnimalTracksType track = AnimalTracksDefinitions.getTrackType(this.animalType, this.trackType);
        if (track == null) {
            return null;
        }
        return track.item;
    }

    public String getTrackSprite() {
        AnimalTracksDefinitions.AnimalTracksType track = AnimalTracksDefinitions.getTrackType(this.animalType, this.trackType);
        if (track == null) {
            return null;
        }
        if (track.sprites != null && this.dir != null) {
            return track.sprites.get((Object)this.dir);
        }
        if (!StringUtils.isNullOrEmpty(track.sprite)) {
            return track.sprite;
        }
        DebugLog.Animal.debugln("Couldn't find sprite for track " + this.trackType + " for animal " + this.animalType);
        return null;
    }

    public boolean isAddedToWorld() {
        return this.addedToWorld;
    }

    public void setAddedToWorld(boolean b) {
        this.addedToWorld = b;
    }

    public InventoryItem addItemToWorld() {
        if (this.addedToWorld) {
            return this.getItem();
        }
        this.addedToWorld = true;
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, 0);
        if (sq != null) {
            String item = this.getTrackItem();
            if (StringUtils.isNullOrEmpty(item)) {
                return null;
            }
            this.setItem(sq.AddWorldInventoryItem((InventoryItem)InventoryItemFactory.CreateItem(item), Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f));
            this.getItem().setAnimalTracks(this);
            return this.getItem();
        }
        return null;
    }

    public ArrayList<IsoAnimalTrack> getAllIsoTracks() {
        if (this.getSquare() == null) {
            return null;
        }
        ArrayList<IsoAnimalTrack> result = new ArrayList<IsoAnimalTrack>();
        for (int x2 = this.x - 5; x2 < this.x + 6; ++x2) {
            for (int y2 = this.y - 5; y2 < this.y + 6; ++y2) {
                IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x2, y2, this.getSquare().z);
                if (sq == null) continue;
                result.add(sq.getAnimalTrack());
            }
        }
        return result;
    }

    public ArrayList<IsoAnimalTrack> addToWorld() {
        if (this.addedToWorld) {
            return this.getAllIsoTracks();
        }
        this.addedToWorld = true;
        ArrayList<IsoAnimalTrack> result = new ArrayList<IsoAnimalTrack>();
        if (this.dir != null) {
            int x1 = 0;
            int x2 = 0;
            int y1 = 0;
            int y2 = 0;
            if (this.dir == IsoDirections.N || this.dir == IsoDirections.S) {
                x1 = this.x - 1;
                x2 = this.x + 1;
                y1 = this.y - 5;
                y2 = this.y + 5;
            }
            if (this.dir == IsoDirections.W || this.dir == IsoDirections.E) {
                x1 = this.x - 1;
                x2 = this.x + 1;
                y1 = this.y - 5;
                y2 = this.y + 5;
            }
            if (this.dir == IsoDirections.NE || this.dir == IsoDirections.SE) {
                y1 = this.y - 4;
                y2 = this.y + 4;
                x1 = this.x - 2;
                x2 = this.x + 2;
            }
            if (this.dir == IsoDirections.NW || this.dir == IsoDirections.SW) {
                x1 = this.x - 4;
                x2 = this.x + 4;
                y1 = this.y - 2;
                y2 = this.y + 2;
            }
            int added = 0;
            for (int x3 = x1; x3 < x2 + 1; ++x3) {
                for (int y3 = y1; y3 < y2 + 1; ++y3) {
                    String sprite;
                    IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x3, y3, 0);
                    if (sq == null || (sprite = this.getTrackSprite()) == null || added != 0 && !Rand.NextBool(6)) continue;
                    ++added;
                    result.add(new IsoAnimalTrack(sq, sprite, this));
                    if (!GameServer.server) continue;
                    INetworkPacket.sendToRelative(PacketTypes.PacketType.AddTrack, sq.x, (float)sq.y, sq, this);
                }
            }
        } else {
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, 0);
            if (sq != null) {
                String sprite = this.getTrackSprite();
                if (sprite != null) {
                    result.add(new IsoAnimalTrack(sq, sprite, this));
                }
                if (GameServer.server) {
                    INetworkPacket.sendToRelative(PacketTypes.PacketType.AddTrack, sq.x, (float)sq.y, sq, this);
                }
            }
        }
        return result;
    }

    public IsoAnimalTrack getIsoAnimalTrack() {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, 0);
        if (sq == null) {
            return null;
        }
        for (int i = 0; i < sq.getSpecialObjects().size(); ++i) {
            if (!(sq.getSpecialObjects().get(i) instanceof IsoAnimalTrack)) continue;
            return (IsoAnimalTrack)sq.getSpecialObjects().get(i);
        }
        return null;
    }

    public String getFreshnessString(int trackingLevel) {
        int hour = this.getTrackHours();
        if (trackingLevel > 6) {
            if (hour < 12) {
                return Translator.getText("IGUI_AnimalTracks_Time_12Hours");
            }
            if (hour < 24) {
                return Translator.getText("IGUI_AnimalTracks_Time_24Hours");
            }
            if (hour < 48) {
                return Translator.getText("IGUI_AnimalTracks_Time_2Days");
            }
            if (hour < 72) {
                return Translator.getText("IGUI_AnimalTracks_Time_3Days");
            }
            if (hour < 96) {
                return Translator.getText("IGUI_AnimalTracks_Time_4Days");
            }
            return Translator.getText("IGUI_AnimalTracks_Time_5Days");
        }
        if (trackingLevel > 3) {
            if (hour < 24) {
                return Translator.getText("IGUI_AnimalTracks_Time_VeryRecent");
            }
            if (hour < 48) {
                return Translator.getText("IGUI_AnimalTracks_Time_Recent");
            }
            if (hour < 72) {
                return Translator.getText("IGUI_AnimalTracks_Time_SomeDays");
            }
            return Translator.getText("IGUI_AnimalTracks_Time_Old");
        }
        if (hour < 24) {
            return Translator.getText("IGUI_AnimalTracks_Time_Recent");
        }
        if (hour < 72) {
            return Translator.getText("IGUI_AnimalTracks_Time_SomeDays");
        }
        return Translator.getText("IGUI_AnimalTracks_Time_Old");
    }

    public int getTrackAgeDays() {
        return PZMath.fastfloor((GameTime.getInstance().getCalender().getTimeInMillis() - this.addedTime) / 60000L) / 1440;
    }

    public int getTrackHours() {
        return PZMath.fastfloor((GameTime.getInstance().getCalender().getTimeInMillis() - this.addedTime) / 60000L) / 60;
    }

    public boolean isItem() {
        AnimalTracksDefinitions.AnimalTracksType track = AnimalTracksDefinitions.getTrackType(this.animalType, this.trackType);
        if (track == null) {
            return false;
        }
        return !StringUtils.isNullOrEmpty(track.item);
    }

    public IsoGridSquare getSquare() {
        return IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, 0);
    }

    public String getTimestamp() {
        return "Fresh";
    }

    public String getAnimalType() {
        return this.animalType;
    }

    public InventoryItem getItem() {
        return this.item;
    }

    public void setItem(InventoryItem item) {
        this.item = item;
    }
}

