/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.characters.animals.AnimalCell;
import zombie.characters.animals.AnimalTracks;
import zombie.characters.animals.AnimalTracksDefinitions;
import zombie.characters.animals.AnimalZones;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.MigrationGroupDefinitions;
import zombie.characters.animals.VirtualAnimal;
import zombie.popman.ObjectPool;

@UsedFromLua
public final class AnimalChunk {
    int x;
    int y;
    final ArrayList<VirtualAnimal> animals = new ArrayList();
    public AnimalCell cell;
    public final ArrayList<AnimalTracks> animalTracks = new ArrayList();
    float tracksUpdateTimer;
    static final ObjectPool<AnimalChunk> pool = new ObjectPool<AnimalChunk>(AnimalChunk::new);

    AnimalChunk init(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    void save(ByteBuffer output) throws IOException {
        int i;
        output.putShort((short)this.animals.size());
        for (i = 0; i < this.animals.size(); ++i) {
            VirtualAnimal animal = this.animals.get(i);
            animal.save(output);
        }
        output.putShort((short)this.animalTracks.size());
        for (i = 0; i < this.animalTracks.size(); ++i) {
            AnimalTracks track = this.animalTracks.get(i);
            track.save(output);
        }
    }

    public void updateTracks() {
        if (this.animalTracks.isEmpty()) {
            return;
        }
        if (this.tracksUpdateTimer <= 0.0f) {
            this.tracksUpdateTimer = 5000.0f;
            for (int i = 0; i < this.animalTracks.size(); ++i) {
                AnimalTracks track = this.animalTracks.get(i);
                if (track.getTrackAgeDays() < 4) continue;
                this.animalTracks.remove(i);
                --i;
            }
        }
        this.tracksUpdateTimer -= GameTime.getInstance().getMultiplier();
    }

    void save(ByteBuffer output, ArrayList<VirtualAnimal> realAnimals) throws IOException {
        VirtualAnimal animal;
        int i;
        output.putShort((short)(this.animals.size() + realAnimals.size()));
        for (i = 0; i < this.animals.size(); ++i) {
            animal = this.animals.get(i);
            animal.save(output);
        }
        for (i = 0; i < realAnimals.size(); ++i) {
            animal = realAnimals.get(i);
            animal.save(output);
        }
        output.putShort((short)this.animalTracks.size());
        for (i = 0; i < this.animalTracks.size(); ++i) {
            AnimalTracks track = this.animalTracks.get(i);
            track.save(output);
        }
    }

    void load(ByteBuffer input, int worldVersion) throws IOException {
        int i;
        int count = input.getShort();
        for (i = 0; i < count; ++i) {
            VirtualAnimal animal = new VirtualAnimal();
            animal.load(input, worldVersion);
            MigrationGroupDefinitions.initValueFromDef(animal);
            this.animals.add(animal);
            for (int j = 0; j < animal.animals.size(); ++j) {
                IsoAnimal isoAnimal = animal.animals.get(j);
                if (isoAnimal.attachBackToMother <= 0) continue;
                if (this.cell.animalListToReattach == null) {
                    this.cell.animalListToReattach = new ArrayList();
                }
                this.cell.animalListToReattach.add(isoAnimal);
            }
        }
        count = input.getShort();
        for (i = 0; i < count; ++i) {
            AnimalTracks track = new AnimalTracks();
            track.load(input, worldVersion);
            this.animalTracks.add(track);
        }
        if (!this.animals.isEmpty() || !this.animalTracks.isEmpty()) {
            AnimalZones.addAnimalChunk(this);
        }
    }

    public ArrayList<VirtualAnimal> getVirtualAnimals() {
        return this.animals;
    }

    public ArrayList<AnimalTracks> getAnimalsTracks() {
        return this.animalTracks;
    }

    public void deleteTracks() {
        if (this.animals.isEmpty() && !this.animalTracks.isEmpty()) {
            AnimalZones.removeAnimalChunk(this);
        }
        this.animalTracks.clear();
    }

    public void addTracksStr(VirtualAnimal animal, String trackType) {
        AnimalTracksDefinitions.AnimalTracksType track = AnimalTracksDefinitions.tracksDefinitions.get((Object)animal.migrationGroup).tracks.get(trackType);
        this.addTracks(animal, track);
    }

    public void addTracks(VirtualAnimal animal, AnimalTracksDefinitions.AnimalTracksType trackType) {
        float closestZoneDist = AnimalZones.getClosestZoneDist(animal.x, animal.y);
        if (closestZoneDist == -1.0f || closestZoneDist > 20.0f) {
            return;
        }
        this.animalTracks.add(AnimalTracks.addAnimalTrack(animal, trackType));
        if (this.animals.size() + this.animalTracks.size() == 1) {
            AnimalZones.addAnimalChunk(this);
        }
    }

    public VirtualAnimal findAnimalByID(double id) {
        if (id == 0.0) {
            return null;
        }
        for (int i = 0; i < this.animals.size(); ++i) {
            VirtualAnimal virtualAnimal = this.animals.get(i);
            if (virtualAnimal.id == 0.0 || virtualAnimal.id != id) continue;
            return virtualAnimal;
        }
        return null;
    }

    static AnimalChunk alloc() {
        return pool.alloc();
    }

    void release() {
        if (this.animals.size() + this.animalTracks.size() > 0) {
            AnimalZones.removeAnimalChunk(this);
        }
        this.animals.clear();
        this.animalTracks.clear();
        pool.release(this);
    }
}

