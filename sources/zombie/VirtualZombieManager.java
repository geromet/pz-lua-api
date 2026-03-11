/*
 * Decompiled with CFR 0.152.
 */
package zombie;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import zombie.AmbientStreamManager;
import zombie.Lua.LuaEventManager;
import zombie.PersistentOutfits;
import zombie.ReanimatedPlayers;
import zombie.SandboxOptions;
import zombie.SystemDisabler;
import zombie.UsedFromLua;
import zombie.ZombieSpawnRecorder;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.ZombiesZoneDefinition;
import zombie.characters.action.ActionGroup;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.BuildingDef;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaChunk;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.Vector2;
import zombie.iso.areas.IsoRoom;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoFireManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.pathfind.PolygonalMap2;
import zombie.popman.NetworkZombieSimulator;
import zombie.popman.ZombiePopulationManager;
import zombie.scripting.objects.ItemKey;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class VirtualZombieManager {
    private final ArrayDeque<IsoZombie> reusableZombies = new ArrayDeque();
    private final HashSet<IsoZombie> reusableZombieSet = new HashSet();
    private final ArrayList<IsoZombie> reusedThisFrame = new ArrayList();
    private final ArrayList<IsoZombie> recentlyRemoved = new ArrayList();
    public static VirtualZombieManager instance = new VirtualZombieManager();
    public int maxRealZombies = 1;
    private final ArrayList<IsoZombie> tempZombies = new ArrayList();
    public final ArrayList<IsoGridSquare> choices = new ArrayList();
    private final ArrayList<IsoGridSquare> bestchoices = new ArrayList();
    HandWeapon w;
    private static final int BLOCKED_N = 1;
    private static final int BLOCKED_S = 2;
    private static final int BLOCKED_W = 4;
    private static final int BLOCKED_E = 8;
    private static final int NO_SQUARE_N = 16;
    private static final int NO_SQUARE_S = 32;
    private static final int NO_SQUARE_W = 64;
    private static final int NO_SQUARE_E = 128;

    public float getKeySpawnChanceD100() {
        float chance = (float)SandboxOptions.getInstance().keyLootNew.getValue() * 50.0f;
        if (chance > 90.0f) {
            chance = 90.0f;
        }
        return chance;
    }

    public boolean removeZombieFromWorld(IsoZombie z) {
        boolean b = z.getCurrentSquare() != null;
        z.getEmitter().unregister();
        z.removeFromWorld();
        z.removeFromSquare();
        return b;
    }

    private void reuseZombie(IsoZombie z) {
        if (z == null) {
            return;
        }
        assert (!IsoWorld.instance.currentCell.getObjectList().contains(z));
        assert (!IsoWorld.instance.currentCell.getZombieList().contains(z));
        assert (z.getCurrentSquare() == null || !z.getCurrentSquare().getMovingObjects().contains(z));
        if (this.isReused(z)) {
            return;
        }
        NetworkZombieSimulator.getInstance().remove(z);
        z.resetForReuse();
        this.addToReusable(z);
    }

    public void addToReusable(IsoZombie z) {
        if (z != null && !this.reusableZombieSet.contains(z)) {
            this.reusableZombies.addLast(z);
            this.reusableZombieSet.add(z);
        }
    }

    public boolean isReused(IsoZombie z) {
        return this.reusableZombieSet.contains(z);
    }

    public void init() {
        if (GameClient.client) {
            return;
        }
        if (IsoWorld.getZombiesDisabled()) {
            return;
        }
        for (int n = 0; n < this.maxRealZombies; ++n) {
            IsoZombie zombie = new IsoZombie(IsoWorld.instance.currentCell);
            zombie.getEmitter().unregister();
            this.addToReusable(zombie);
        }
    }

    public void Reset() {
        for (IsoZombie zombie : this.reusedThisFrame) {
            if (zombie.vocalEvent != 0L) {
                zombie.getEmitter().stopSoundLocal(zombie.vocalEvent);
                zombie.vocalEvent = 0L;
            }
            zombie.getAdvancedAnimator().reset();
            zombie.releaseAnimationPlayer();
        }
        this.bestchoices.clear();
        this.choices.clear();
        this.recentlyRemoved.clear();
        this.reusableZombies.clear();
        this.reusableZombieSet.clear();
        this.reusedThisFrame.clear();
    }

    public void update() {
        IsoZombie z;
        int i;
        long currentMS = System.currentTimeMillis();
        for (i = this.recentlyRemoved.size() - 1; i >= 0; --i) {
            IsoZombie zombie = this.recentlyRemoved.get(i);
            zombie.updateEmitter();
            if (currentMS - zombie.removedFromWorldMs <= 5000L) continue;
            if (zombie.vocalEvent != 0L) {
                zombie.getEmitter().stopSoundLocal(zombie.vocalEvent);
                zombie.vocalEvent = 0L;
            }
            zombie.getEmitter().stopAll();
            this.recentlyRemoved.remove(i);
            this.reusedThisFrame.add(zombie);
        }
        if (GameClient.client || GameServer.server) {
            for (i = 0; i < this.reusedThisFrame.size(); ++i) {
                z = this.reusedThisFrame.get(i);
                this.reuseZombie(z);
            }
            this.reusedThisFrame.clear();
            return;
        }
        for (int f = 0; f < IsoWorld.instance.currentCell.getZombieList().size(); ++f) {
            z = IsoWorld.instance.currentCell.getZombieList().get(f);
            if (z.keepItReal || z.getCurrentSquare() != null) continue;
            z.removeFromWorld();
            z.removeFromSquare();
            assert (this.reusedThisFrame.contains(z));
            assert (!IsoWorld.instance.currentCell.getZombieList().contains(z));
            --f;
        }
        for (i = 0; i < this.reusedThisFrame.size(); ++i) {
            z = this.reusedThisFrame.get(i);
            this.reuseZombie(z);
        }
        this.reusedThisFrame.clear();
    }

    public IsoZombie createRealZombieAlways(IsoDirections dir, boolean bDead) {
        return this.createRealZombieAlways(dir, bDead, 0);
    }

    public IsoZombie createRealZombieAlways(int descriptorId, IsoDirections dir, boolean bDead) {
        int outfitID = PersistentOutfits.instance.getOutfit(descriptorId);
        return this.createRealZombieAlways(dir, bDead, outfitID);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public IsoZombie createRealZombieAlways(IsoDirections dir, boolean bDead, int outfitID) {
        IsoZombie zombie;
        if (!SystemDisabler.doZombieCreation) {
            return null;
        }
        if (this.choices == null || this.choices.isEmpty()) {
            return null;
        }
        IsoGridSquare choice = this.choices.get(Rand.Next(this.choices.size()));
        if (choice == null) {
            return null;
        }
        if (choice.isWaterSquare()) {
            return null;
        }
        if (this.w == null) {
            this.w = (HandWeapon)InventoryItemFactory.CreateItem("Base.Axe");
        }
        if ((GameServer.server || GameClient.client) && outfitID == 0) {
            outfitID = ZombiesZoneDefinition.pickPersistentOutfit(choice);
        }
        if (this.reusableZombies.isEmpty()) {
            zombie = new IsoZombie(IsoWorld.instance.currentCell);
            zombie.dressInRandomOutfit = outfitID == 0;
            zombie.setPersistentOutfitID(outfitID);
            IsoWorld.instance.currentCell.getObjectList().add(zombie);
        } else {
            zombie = this.reusableZombies.removeFirst();
            this.reusableZombieSet.remove(zombie);
            zombie.getHumanVisual().clear();
            zombie.clearAttachedItems();
            zombie.clearItemsToSpawnAtDeath();
            zombie.dressInRandomOutfit = outfitID == 0;
            zombie.setPersistentOutfitID(outfitID);
            zombie.setSitAgainstWall(false);
            zombie.setOnDeathDone(false);
            zombie.setOnKillDone(false);
            zombie.setDoDeathSound(true);
            zombie.setKilledByFall(false);
            zombie.setHitTime(0);
            zombie.setFallOnFront(false);
            zombie.setFakeDead(false);
            zombie.setReanimatedPlayer(false);
            zombie.setStateMachineLocked(false);
            zombie.setDoRender(true);
            Vector2 temp = zombie.dir.ToVector();
            temp.x += (float)Rand.Next(200) / 100.0f - 0.5f;
            temp.y += (float)Rand.Next(200) / 100.0f - 0.5f;
            temp.normalize();
            zombie.setForwardDirection(temp);
            IsoWorld.instance.currentCell.getObjectList().add(zombie);
            zombie.walkVariant = "ZombieWalk";
            zombie.DoZombieStats();
            if (zombie.isOnFire()) {
                IsoFireManager.RemoveBurningCharacter(zombie);
                zombie.setOnFire(false);
            }
            if (zombie.attachedAnimSprite != null) {
                zombie.attachedAnimSprite.clear();
            }
            zombie.thumpFlag = 0;
            zombie.thumpSent = false;
            zombie.soundSourceTarget = null;
            zombie.soundAttract = 0.0f;
            zombie.soundAttractTimeout = 0.0f;
            zombie.bodyToEat = null;
            zombie.eatBodyTarget = null;
            zombie.atlasTex = null;
            zombie.clearVariables();
            zombie.setStaggerBack(false);
            zombie.setKnockedDown(false);
            zombie.setKnifeDeath(false);
            zombie.setJawStabAttach(false);
            zombie.setCrawler(false);
            zombie.initializeStates();
            zombie.getActionContext().setGroup(ActionGroup.getActionGroup("zombie"));
            zombie.advancedAnimator.OnAnimDataChanged(false);
            zombie.setDefaultState();
            zombie.getAnimationPlayer().resetBoneModelTransforms();
        }
        zombie.dir = dir;
        zombie.setForwardDirection(zombie.dir.ToVector());
        zombie.getInventory().setExplored(false);
        if (bDead) {
            zombie.dressInRandomOutfit = true;
        }
        zombie.target = null;
        zombie.timeSinceSeenFlesh = 100000.0f;
        if (!zombie.isFakeDead()) {
            if (SandboxOptions.instance.lore.toughness.getValue() == 1) {
                zombie.setHealth(3.5f + Rand.Next(0.0f, 0.3f));
            }
            if (SandboxOptions.instance.lore.toughness.getValue() == 2) {
                zombie.setHealth(1.5f + Rand.Next(0.0f, 0.3f));
            }
            if (SandboxOptions.instance.lore.toughness.getValue() == 3) {
                zombie.setHealth(0.5f + Rand.Next(0.0f, 0.3f));
            }
            if (SandboxOptions.instance.lore.toughness.getValue() == 4) {
                zombie.setHealth(Rand.Next(0.5f, 3.5f) + Rand.Next(0.0f, 0.3f));
            }
        } else {
            zombie.setHealth(0.5f + Rand.Next(0.0f, 0.3f));
        }
        float specX = Rand.Next(0, 1000);
        float specY = Rand.Next(0, 1000);
        specX /= 1000.0f;
        specY /= 1000.0f;
        zombie.setCurrent(choice);
        zombie.setMovingSquareNow();
        zombie.setX(specX += (float)choice.getX());
        zombie.setY(specY += (float)choice.getY());
        zombie.setZ(choice.getZ());
        if ((GameClient.client || GameServer.server) && zombie.networkAi != null) {
            zombie.networkAi.reset();
            zombie.getPathFindBehavior2().reset();
        }
        if (bDead) {
            zombie.setDir(IsoDirections.getRandom());
            zombie.setForwardDirection(zombie.dir.ToVector());
            zombie.setFakeDead(false);
            zombie.setHealth(0.0f);
            zombie.DoZombieInventory();
            new IsoDeadBody(zombie, true);
            return zombie;
        }
        LuaEventManager.triggerEvent("OnZombieCreate", zombie);
        ArrayList<IsoZombie> arrayList = IsoWorld.instance.currentCell.getZombieList();
        synchronized (arrayList) {
            int roll;
            zombie.getEmitter().register();
            IsoWorld.instance.currentCell.getZombieList().add(zombie);
            if (GameClient.client) {
                zombie.remote = true;
            }
            if (GameServer.server) {
                zombie.onlineId = ServerMap.instance.getUniqueZombieId();
                if (zombie.onlineId == -1) {
                    IsoWorld.instance.currentCell.getZombieList().remove(zombie);
                    IsoWorld.instance.currentCell.getObjectList().remove(zombie);
                    this.reusedThisFrame.add(zombie);
                    return null;
                }
                ServerMap.instance.zombieMap.put(zombie.onlineId, zombie);
            }
            if ((float)(roll = Math.min(Rand.Next(100), Rand.Next(100))) < this.getKeySpawnChanceD100()) {
                this.checkAndSpawnZombieForBuildingKey(zombie);
            }
            return zombie;
        }
    }

    private IsoGridSquare pickEatingZombieSquare(float bodyX, float bodyY, float zombieX, float zombieY, int z) {
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(zombieX, zombieY, (double)z);
        if (square == null || !this.canSpawnAt(square.x, square.y, square.z) || square.HasStairs()) {
            return null;
        }
        if (PolygonalMap2.instance.lineClearCollide(bodyX, bodyY, zombieX, zombieY, z, null, false, true)) {
            return null;
        }
        return square;
    }

    public void createEatingZombies(IsoDeadBody target, int nb) {
        if (IsoWorld.getZombiesDisabled()) {
            return;
        }
        for (int i = 0; i < nb; ++i) {
            float zombieX = target.getX();
            float zombieY = target.getY();
            switch (i) {
                case 0: {
                    zombieX -= 0.5f;
                    break;
                }
                case 1: {
                    zombieX += 0.5f;
                    break;
                }
                case 2: {
                    zombieY -= 0.5f;
                    break;
                }
                case 3: {
                    zombieY += 0.5f;
                }
            }
            IsoGridSquare square = this.pickEatingZombieSquare(target.getX(), target.getY(), zombieX, zombieY, PZMath.fastfloor(target.getZ()));
            if (square == null) continue;
            this.choices.clear();
            this.choices.add(square);
            IsoZombie zombie = this.createRealZombieAlways(IsoDirections.NW, false);
            if (zombie == null) continue;
            ZombieSpawnRecorder.instance.record(zombie, "createEatingZombies");
            zombie.dressInRandomOutfit = true;
            zombie.setX(zombieX);
            zombie.setY(zombieY);
            zombie.setZ(target.getZ());
            zombie.faceLocationF(target.getX(), target.getY());
            zombie.setEatBodyTarget(target, true);
        }
    }

    private IsoZombie createRealZombie(IsoDirections dir, boolean bDead) {
        if (GameClient.client) {
            return null;
        }
        return this.createRealZombieAlways(dir, bDead);
    }

    public void AddBloodToMap(int nSize, IsoChunk chk) {
        for (int n = 0; n < nSize; ++n) {
            IsoGridSquare sq;
            int timeout2 = 0;
            do {
                int x = Rand.Next(10);
                int y = Rand.Next(10);
                sq = chk.getGridSquare(x, y, 0);
            } while (++timeout2 < 100 && (sq == null || !sq.isFree(false)));
            if (sq == null) continue;
            int amount = 5;
            if (Rand.Next(10) == 0) {
                amount = 10;
            }
            if (Rand.Next(40) == 0) {
                amount = 20;
            }
            for (int m = 0; m < amount; ++m) {
                float rx = (float)Rand.Next(3000) / 1000.0f;
                float ry = (float)Rand.Next(3000) / 1000.0f;
                chk.addBloodSplat((float)sq.getX() + (rx -= 1.5f), (float)sq.getY() + (ry -= 1.5f), sq.getZ(), Rand.Next(12) + 8);
            }
        }
    }

    public boolean shouldSpawnZombiesOnLevel(int level) {
        if (GameServer.server) {
            ArrayList<IsoPlayer> players = GameServer.getPlayers();
            for (int i = 0; i < players.size(); ++i) {
                IsoPlayer player = players.get(i);
                if (PZMath.abs(level - PZMath.fastfloor(player.getZ())) > 1) continue;
                return true;
            }
            return false;
        }
        if (GameClient.client) {
            return false;
        }
        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
            IsoPlayer player = IsoPlayer.players[playerIndex];
            if (player == null || PZMath.abs(level - PZMath.fastfloor(player.getZ())) > 1) continue;
            return true;
        }
        return false;
    }

    public ArrayList<IsoZombie> addZombiesToMap(int nSize, RoomDef room) {
        return this.addZombiesToMap(nSize, room, true);
    }

    public ArrayList<IsoZombie> addZombiesToMap(int nSize, RoomDef room, boolean bAllowDead) {
        int n;
        ArrayList<IsoZombie> result = new ArrayList<IsoZombie>();
        if ("Tutorial".equals(Core.gameMode)) {
            return result;
        }
        if (IsoWorld.getZombiesDisabled()) {
            return result;
        }
        this.choices.clear();
        this.bestchoices.clear();
        for (n = 0; n < room.rects.size(); ++n) {
            int z = room.level;
            RoomDef.RoomRect r = room.rects.get(n);
            for (int x = r.x; x < r.getX2(); ++x) {
                for (int y = r.y; y < r.getY2(); ++y) {
                    IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
                    if (sq == null || !this.canSpawnAt(x, y, z)) continue;
                    this.choices.add(sq);
                    boolean seen = false;
                    for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
                        if (IsoPlayer.players[playerIndex] == null || !sq.isSeen(playerIndex)) continue;
                        seen = true;
                    }
                    if (seen) continue;
                    this.bestchoices.add(sq);
                }
            }
        }
        nSize = Math.min(nSize, this.choices.size());
        if (!this.bestchoices.isEmpty()) {
            this.choices.addAll(this.bestchoices);
            this.choices.addAll(this.bestchoices);
        }
        for (n = 0; n < nSize; ++n) {
            if (!this.choices.isEmpty()) {
                room.building.alarmed = false;
                int randomDeadZed = 4;
                IsoZombie z = this.createRealZombie(IsoDirections.getRandom(), bAllowDead ? Rand.Next(4) == 0 : false);
                if (z == null || z.getSquare() == null) continue;
                if (!GameServer.server) {
                    z.dressInRandomOutfit = true;
                }
                z.setX((float)PZMath.fastfloor(z.getX()) + (float)Rand.Next(2, 8) / 10.0f);
                z.setY((float)PZMath.fastfloor(z.getY()) + (float)Rand.Next(2, 8) / 10.0f);
                this.choices.remove(z.getSquare());
                this.choices.remove(z.getSquare());
                this.choices.remove(z.getSquare());
                result.add(z);
                continue;
            }
            System.out.println("No choices for zombie.");
        }
        this.bestchoices.clear();
        this.choices.clear();
        return result;
    }

    public void tryAddIndoorZombies(RoomDef room, boolean bAllowDead) {
    }

    private void addIndoorZombies(int nSize, RoomDef room, boolean bAllowDead) {
        int n;
        this.choices.clear();
        this.bestchoices.clear();
        for (n = 0; n < room.rects.size(); ++n) {
            int z = room.level;
            RoomDef.RoomRect r = room.rects.get(n);
            for (int x = r.x; x < r.getX2(); ++x) {
                for (int y = r.y; y < r.getY2(); ++y) {
                    IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
                    if (sq == null || !this.canSpawnAt(x, y, z)) continue;
                    this.choices.add(sq);
                }
            }
        }
        nSize = Math.min(nSize, this.choices.size());
        if (!this.bestchoices.isEmpty()) {
            this.choices.addAll(this.bestchoices);
            this.choices.addAll(this.bestchoices);
        }
        for (n = 0; n < nSize; ++n) {
            if (!this.choices.isEmpty()) {
                room.building.alarmed = false;
                int randomDeadZed = 4;
                IsoZombie z = this.createRealZombie(IsoDirections.getRandom(), bAllowDead ? Rand.Next(4) == 0 : false);
                if (z == null || z.getSquare() == null) continue;
                ZombieSpawnRecorder.instance.record(z, "addIndoorZombies");
                z.indoorZombie = true;
                z.setX((float)PZMath.fastfloor(z.getX()) + (float)Rand.Next(2, 8) / 10.0f);
                z.setY((float)PZMath.fastfloor(z.getY()) + (float)Rand.Next(2, 8) / 10.0f);
                this.choices.remove(z.getSquare());
                this.choices.remove(z.getSquare());
                this.choices.remove(z.getSquare());
                continue;
            }
            System.out.println("No choices for zombie.");
        }
        this.bestchoices.clear();
        this.choices.clear();
    }

    public void addIndoorZombiesToChunk(IsoChunk chunk, IsoRoom room, int zombieCountForRoom, ArrayList<IsoZombie> zombies) {
        int i;
        if (zombieCountForRoom <= 0) {
            return;
        }
        float areaPercent = room.getRoomDef().getAreaOverlapping(chunk);
        int count = (int)Math.ceil((float)zombieCountForRoom * areaPercent);
        if (count <= 0) {
            return;
        }
        int chunksPerWidth = 8;
        this.choices.clear();
        int z = room.def.level;
        for (i = 0; i < room.rects.size(); ++i) {
            RoomDef.RoomRect roomRect = room.rects.get(i);
            int x1 = Math.max(chunk.wx * 8, roomRect.x);
            int y1 = Math.max(chunk.wy * 8, roomRect.y);
            int x2 = Math.min((chunk.wx + 1) * 8, roomRect.x + roomRect.w);
            int y2 = Math.min((chunk.wy + 1) * 8, roomRect.y + roomRect.h);
            for (int x = x1; x < x2; ++x) {
                for (int y = y1; y < y2; ++y) {
                    IsoGridSquare sq = chunk.getGridSquare(x - chunk.wx * 8, y - chunk.wy * 8, z);
                    if (sq == null || !this.canSpawnAt(x, y, z)) continue;
                    this.choices.add(sq);
                }
            }
        }
        if (this.choices.isEmpty()) {
            return;
        }
        room.def.building.alarmed = false;
        count = Math.min(count, this.choices.size());
        for (i = 0; i < count; ++i) {
            IsoZombie zombie = this.createRealZombie(IsoDirections.getRandom(), false);
            if (zombie == null || zombie.getSquare() == null) continue;
            if (!GameServer.server) {
                zombie.dressInRandomOutfit = true;
            }
            zombie.setX((float)PZMath.fastfloor(zombie.getX()) + (float)Rand.Next(2, 8) / 10.0f);
            zombie.setY((float)PZMath.fastfloor(zombie.getY()) + (float)Rand.Next(2, 8) / 10.0f);
            this.choices.remove(zombie.getSquare());
            zombies.add(zombie);
        }
        this.choices.clear();
    }

    public void addIndoorZombiesToChunk(IsoChunk chunk, IsoRoom room) {
        if (room.def.spawnCount == -1) {
            room.def.spawnCount = this.getZombieCountForRoom(room);
        }
        this.tempZombies.clear();
        this.addIndoorZombiesToChunk(chunk, room, room.def.spawnCount, this.tempZombies);
        ZombieSpawnRecorder.instance.record(this.tempZombies, "addIndoorZombiesToChunk");
    }

    public void addDeadZombiesToMap(int nSize, RoomDef room) {
        int n;
        this.choices.clear();
        this.bestchoices.clear();
        for (n = 0; n < room.rects.size(); ++n) {
            int z = room.level;
            RoomDef.RoomRect r = room.rects.get(n);
            for (int x = r.x; x < r.getX2(); ++x) {
                for (int y = r.y; y < r.getY2(); ++y) {
                    IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
                    if (sq == null || !sq.isFree(false)) continue;
                    this.choices.add(sq);
                    if (GameServer.server) continue;
                    boolean seen = false;
                    for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
                        if (IsoPlayer.players[playerIndex] == null || !sq.isSeen(playerIndex)) continue;
                        seen = true;
                    }
                    if (seen) continue;
                    this.bestchoices.add(sq);
                }
            }
        }
        nSize = Math.min(nSize, this.choices.size());
        if (!this.bestchoices.isEmpty()) {
            this.choices.addAll(this.bestchoices);
            this.choices.addAll(this.bestchoices);
        }
        for (n = 0; n < nSize; ++n) {
            if (this.choices.isEmpty()) continue;
            this.createRealZombie(IsoDirections.getRandom(), true);
        }
        this.bestchoices.clear();
        this.choices.clear();
    }

    public void RemoveZombie(IsoZombie obj) {
        if (obj.isReanimatedPlayer()) {
            if (obj.vocalEvent != 0L) {
                obj.getEmitter().stopSoundLocal(obj.vocalEvent);
                obj.vocalEvent = 0L;
            }
            ReanimatedPlayers.instance.removeReanimatedPlayerFromWorld(obj);
            return;
        }
        if (obj.isDead()) {
            if (!this.recentlyRemoved.contains(obj)) {
                obj.removedFromWorldMs = System.currentTimeMillis();
                this.recentlyRemoved.add(obj);
            }
        } else if (!this.reusedThisFrame.contains(obj)) {
            this.reusedThisFrame.add(obj);
        }
    }

    public void createHordeFromTo(float spawnX, float spawnY, float targetX, float targetY, int count) {
        ZombiePopulationManager.instance.createHordeFromTo(PZMath.fastfloor(spawnX), PZMath.fastfloor(spawnY), PZMath.fastfloor(targetX), PZMath.fastfloor(targetY), count);
    }

    public IsoZombie createRealZombie(float x, float y, float z) {
        this.choices.clear();
        this.choices.add(IsoWorld.instance.currentCell.getGridSquare(x, y, z));
        if (!this.choices.isEmpty()) {
            return this.createRealZombie(IsoDirections.getRandom(), true);
        }
        return null;
    }

    public IsoZombie createRealZombieNow(float x, float y, float z) {
        this.choices.clear();
        IsoGridSquare c = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        if (c == null) {
            return null;
        }
        this.choices.add(c);
        if (!this.choices.isEmpty()) {
            return this.createRealZombie(IsoDirections.getRandom(), false);
        }
        return null;
    }

    private int getZombieCountForRoom(IsoRoom room) {
        if (IsoWorld.getZombiesDisabled()) {
            return 0;
        }
        if (GameClient.client) {
            return 0;
        }
        if (Core.lastStand) {
            return 0;
        }
        if (room.def != null && room.def.isEmptyOutside()) {
            return 0;
        }
        int ra = 7;
        if (SandboxOptions.instance.zombies.getValue() == 1) {
            ra = 3;
        } else if (SandboxOptions.instance.zombies.getValue() == 2) {
            ra = 4;
        } else if (SandboxOptions.instance.zombies.getValue() == 3) {
            ra = 6;
        } else if (SandboxOptions.instance.zombies.getValue() == 5) {
            ra = 15;
        }
        float zombieDensity = 0.0f;
        IsoMetaChunk metaChunk = IsoWorld.instance.getMetaChunk(room.def.x / 8, room.def.y / 8);
        if (metaChunk != null && (zombieDensity = metaChunk.getLootZombieIntensity()) > 4.0f) {
            ra = (int)((float)ra - (zombieDensity / 2.0f - 2.0f));
        }
        if (room.def.getArea() > 100) {
            ra -= 2;
        }
        ra = Math.max(2, ra);
        if (room.getBuilding() != null) {
            int roomArea = room.def.getArea();
            if (room.getBuilding().getRoomsNumber() > 100 && roomArea >= 20) {
                int number = room.getBuilding().getRoomsNumber() - 95;
                if (number > 20) {
                    number = 20;
                }
                if (SandboxOptions.instance.zombies.getValue() == 1) {
                    number += 10;
                } else if (SandboxOptions.instance.zombies.getValue() == 2) {
                    number += 7;
                } else if (SandboxOptions.instance.zombies.getValue() == 3) {
                    number += 5;
                } else if (SandboxOptions.instance.zombies.getValue() == 5) {
                    number -= 10;
                }
                if (roomArea < 30) {
                    number -= 6;
                }
                if (roomArea < 50) {
                    number -= 10;
                }
                if (roomArea < 70) {
                    number -= 13;
                }
                return Rand.Next(number, number + 10);
            }
        }
        if (Rand.Next(ra) == 0) {
            int baseNbr = 1;
            baseNbr = (int)((float)baseNbr + (zombieDensity / 2.0f - 2.0f));
            if (room.def.getArea() < 30) {
                baseNbr -= 4;
            }
            if (room.def.getArea() > 85) {
                baseNbr += 2;
            }
            if (room.getBuilding().getRoomsNumber() < 7) {
                baseNbr -= 2;
            }
            if (SandboxOptions.instance.zombies.getValue() == 1) {
                baseNbr += 3;
            } else if (SandboxOptions.instance.zombies.getValue() == 2) {
                baseNbr += 2;
            } else if (SandboxOptions.instance.zombies.getValue() == 3) {
                ++baseNbr;
            } else if (SandboxOptions.instance.zombies.getValue() == 5) {
                baseNbr -= 2;
            }
            baseNbr = Math.max(0, baseNbr);
            baseNbr = Math.min(7, baseNbr);
            return Rand.Next(baseNbr, baseNbr + 2);
        }
        return 0;
    }

    public void roomSpotted(IsoRoom room) {
        if (GameClient.client) {
            return;
        }
        room.def.forEachChunk((roomDef, chunk) -> chunk.addSpawnedRoom(roomDef.id));
        if (room.def.spawnCount == -1) {
            room.def.spawnCount = this.getZombieCountForRoom(room);
        }
        if (room.def.spawnCount <= 0) {
            return;
        }
        if (room.getBuilding().getDef().isFullyStreamedIn()) {
            ArrayList<IsoZombie> zombies = this.addZombiesToMap(room.def.spawnCount, room.def, false);
            ZombieSpawnRecorder.instance.record(zombies, "roomSpotted");
        } else {
            this.tempZombies.clear();
            room.def.forEachChunk((roomDef, chunk) -> this.addIndoorZombiesToChunk((IsoChunk)chunk, room, room.def.spawnCount, this.tempZombies));
            ZombieSpawnRecorder.instance.record(this.tempZombies, "roomSpotted");
        }
    }

    private int getBlockedBits(IsoGridSquare sq) {
        int bits = 0;
        if (sq == null) {
            return bits;
        }
        if (sq.getAdjacentSquare(IsoDirections.N) == null) {
            bits |= 0x10;
        } else if (IsoGridSquare.getMatrixBit(sq.pathMatrix, 1, 0, 1)) {
            bits |= 1;
        }
        if (sq.getAdjacentSquare(IsoDirections.S) == null) {
            bits |= 0x20;
        } else if (IsoGridSquare.getMatrixBit(sq.pathMatrix, 1, 2, 1)) {
            bits |= 2;
        }
        if (sq.getAdjacentSquare(IsoDirections.W) == null) {
            bits |= 0x40;
        } else if (IsoGridSquare.getMatrixBit(sq.pathMatrix, 0, 1, 1)) {
            bits |= 4;
        }
        if (sq.getAdjacentSquare(IsoDirections.E) == null) {
            bits |= 0x80;
        } else if (IsoGridSquare.getMatrixBit(sq.pathMatrix, 2, 1, 1)) {
            bits |= 8;
        }
        return bits;
    }

    private boolean isBlockedInAllDirections(int x, int y, int z) {
        IsoGridSquare sq;
        IsoGridSquare isoGridSquare = sq = GameServer.server ? ServerMap.instance.getGridSquare(x, y, z) : IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        if (sq == null) {
            return false;
        }
        boolean blockedN = IsoGridSquare.getMatrixBit(sq.pathMatrix, 1, 0, 1) && sq.getAdjacentSquare(IsoDirections.N) != null;
        boolean blockedS = IsoGridSquare.getMatrixBit(sq.pathMatrix, 1, 2, 1) && sq.getAdjacentSquare(IsoDirections.S) != null;
        boolean blockedW = IsoGridSquare.getMatrixBit(sq.pathMatrix, 0, 1, 1) && sq.getAdjacentSquare(IsoDirections.W) != null;
        boolean blockedE = IsoGridSquare.getMatrixBit(sq.pathMatrix, 2, 1, 1) && sq.getAdjacentSquare(IsoDirections.E) != null;
        return blockedN && blockedS && blockedW && blockedE;
    }

    private boolean canPathOnlyN(IsoGridSquare sq) {
        int bits;
        while (((bits = this.getBlockedBits(sq)) & 0xC) == 12) {
            if ((bits & 0x10) != 0) {
                return false;
            }
            if ((bits & 1) != 0) {
                return true;
            }
            sq = sq.getAdjacentSquare(IsoDirections.N);
        }
        return false;
    }

    private boolean canPathOnlyS(IsoGridSquare sq) {
        int bits;
        while (((bits = this.getBlockedBits(sq)) & 0xC) == 12) {
            if ((bits & 0x20) != 0) {
                return false;
            }
            if ((bits & 2) != 0) {
                return true;
            }
            sq = sq.getAdjacentSquare(IsoDirections.S);
        }
        return false;
    }

    private boolean canPathOnlyW(IsoGridSquare sq) {
        int bits;
        while (((bits = this.getBlockedBits(sq)) & 3) == 3) {
            if ((bits & 0x40) != 0) {
                return false;
            }
            if ((bits & 4) != 0) {
                return true;
            }
            sq = sq.getAdjacentSquare(IsoDirections.W);
        }
        return false;
    }

    private boolean canPathOnlyE(IsoGridSquare sq) {
        int bits;
        while (((bits = this.getBlockedBits(sq)) & 3) == 3) {
            if ((bits & 0x80) != 0) {
                return false;
            }
            if ((bits & 8) != 0) {
                return true;
            }
            sq = sq.getAdjacentSquare(IsoDirections.E);
        }
        return false;
    }

    public boolean canSpawnAt(int x, int y, int z) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        if (sq == null || !sq.isFree(false)) {
            return false;
        }
        int bits = this.getBlockedBits(sq);
        if (bits == 15) {
            return false;
        }
        if ((bits & 3) == 3 && this.canPathOnlyW(sq) && this.canPathOnlyE(sq)) {
            return false;
        }
        return (bits & 0xC) != 12 || !this.canPathOnlyN(sq) || !this.canPathOnlyS(sq);
    }

    public int reusableZombiesSize() {
        return this.reusableZombies.size();
    }

    public boolean checkZombieKeyForBuilding(String outfitName, IsoGridSquare square) {
        if (outfitName == null) {
            return false;
        }
        if (square == null) {
            return false;
        }
        if (square.getBuilding() == null) {
            return false;
        }
        if (outfitName.contains("Survivalist") || outfitName.equals("Security")) {
            return true;
        }
        if (outfitName.equals("Young") || outfitName.equals("Student")) {
            return square.getBuilding().getRandomRoom("bedroom") != null && square.getBuilding().getRandomRoom("livingroom") != null && square.getBuilding().getRandomRoom("kitchen") != null;
        }
        if (outfitName.contains("Generic") || outfitName.contains("Dress")) {
            return square.getBuilding().getRandomRoom("bedroom") != null && square.getBuilding().getRandomRoom("livingroom") != null && square.getBuilding().getRandomRoom("kitchen") != null;
        }
        if ((outfitName.contains("Army") || outfitName.contains("Ghillie")) && (square.getBuilding().getRandomRoom("armyhanger") != null || square.getBuilding().getRandomRoom("armystorage") != null)) {
            return true;
        }
        if (outfitName.contains("Police") && square.getBuilding().getRandomRoom("policestorage") != null) {
            return true;
        }
        if (outfitName.contains("PrisonGuard") && square.getBuilding().getRandomRoom("prisoncells") != null) {
            return true;
        }
        if ((outfitName.contains("AmbulanceDriver") || outfitName.contains("Doctor") || outfitName.contains("Nurse") || outfitName.contains("Pharmacist")) && square.getBuilding().getRandomRoom("hospitalroom") != null) {
            return true;
        }
        if (outfitName.contains("Fireman") || outfitName.contains("Police") || outfitName.contains("Ranger") || outfitName.contains("AmbulanceDriver") || outfitName.contains("Ghillie") || outfitName.contains("PrisonGuard") || outfitName.contains("Tourist")) {
            return false;
        }
        if (outfitName.contains("Army")) {
            return square.getBuilding().getRandomRoom("bedroom") != null;
        }
        if (square.getBuilding().getRandomRoom("prisoncells") != null || square.getBuilding().getRandomRoom("policestorage") != null) {
            return false;
        }
        if (square.getBuilding().getRandomRoom("bedroom") != null && square.getBuilding().getRandomRoom("livingroom") != null && square.getBuilding().getRandomRoom("kitchen") != null) {
            return true;
        }
        if (square.getBuilding().getRandomRoom("storageunit") != null) {
            return true;
        }
        if (outfitName.contains("Trader") && square.getBuilding().getRandomRoom("bank") != null) {
            return true;
        }
        if ((outfitName.contains("OfficeWorker") || outfitName.contains("Trader")) && (square.getBuilding().getRandomRoom("cardealershipoffice") != null || square.getBuilding().getRandomRoom("office") != null)) {
            return true;
        }
        if (outfitName.contains("Priest") && square.getBuilding().getRandomRoom("church") != null) {
            return true;
        }
        if (outfitName.contains("Teacher") && square.getBuilding().getRandomRoom("classroom") != null) {
            return true;
        }
        if ((outfitName.contains("ConstructionWorker") || outfitName.contains("Foreman") || outfitName.contains("Mechanic") || outfitName.contains("MetalWorker")) && square.getBuilding().getRandomRoom("construction") != null) {
            return true;
        }
        if ((outfitName.contains("Biker") || outfitName.contains("Punk") || outfitName.contains("Redneck") || outfitName.contains("Thug") || outfitName.contains("Veteran")) && (square.getBuilding().getRandomRoom("druglab") != null || square.getBuilding().getRandomRoom("drugshack") != null)) {
            return true;
        }
        if (outfitName.contains("Farmer") && (square.getBuilding().getRandomRoom("farmstorage") != null || square.getBuilding().getRandomRoom("producestorage") != null)) {
            return true;
        }
        if (outfitName.contains("Fireman") && square.getBuilding().getRandomRoom("firestorage") != null) {
            return true;
        }
        if (outfitName.contains("Fossoil") && square.getBuilding().getRandomRoom("fossoil") != null) {
            return true;
        }
        if ((outfitName.contains("Gas2Go") || outfitName.contains("ThunderGas")) && square.getBuilding().getRandomRoom("gasstore") != null) {
            return true;
        }
        if ((outfitName.contains("GigaMart") || outfitName.contains("Cook_Generic")) && square.getBuilding().getRandomRoom("gigamart") != null) {
            return true;
        }
        if ((outfitName.contains("McCoys") || outfitName.contains("Foreman")) && (square.getBuilding().getRandomRoom("loggingfactory") != null || square.getBuilding().getRandomRoom("loggingwarehouse") != null)) {
            return true;
        }
        if ((outfitName.contains("Mechanic") || outfitName.contains("MetalWorker")) && square.getBuilding().getRandomRoom("mechanic") != null) {
            return true;
        }
        if ((outfitName.contains("Doctor") || outfitName.contains("Nurse")) && square.getBuilding().getRandomRoom("medical") != null) {
            return true;
        }
        if (outfitName.contains("Doctor") && square.getBuilding().getRandomRoom("morgue") != null) {
            return true;
        }
        if ((outfitName.contains("Generic") || outfitName.contains("Dress") || outfitName.contains("Golfer") || outfitName.contains("Classy") || outfitName.contains("Tourist")) && square.getBuilding().getRandomRoom("motelroom") != null) {
            return true;
        }
        if ((outfitName.contains("Waiter_PileOCrepe") || outfitName.contains("Chef")) && square.getBuilding().getRandomRoom("pileocrepe") != null) {
            return true;
        }
        if ((outfitName.contains("Waiter_PizzaWhirled") || outfitName.contains("Cook_Generic")) && square.getBuilding().getRandomRoom("pizzawhirled") != null) {
            return true;
        }
        if (outfitName.contains("Pharmacist") && square.getBuilding().getRandomRoom("pharmacy") != null) {
            return true;
        }
        if (outfitName.contains("Postal") && square.getBuilding().getRandomRoom("post") != null) {
            return true;
        }
        if ((outfitName.contains("Waiter_Restaurant") || outfitName.contains("Cook_Generic")) && square.getBuilding().getRandomRoom("chineserestaurant") != null && square.getBuilding().getRandomRoom("italianrestaurant") != null && square.getBuilding().getRandomRoom("restaurant") != null) {
            return true;
        }
        if (outfitName.contains("Spiffo") && square.getBuilding().getRandomRoom("spiffoskitchen") != null) {
            return true;
        }
        if (outfitName.contains("WaiterStripper") && square.getBuilding().getRandomRoom("stripclub") != null) {
            return true;
        }
        if (outfitName.contains("Cook_Generic") && (square.getBuilding().getRandomRoom("bakerykitchen") != null || square.getBuilding().getRandomRoom("burgerkitchen") != null || square.getBuilding().getRandomRoom("cafekitchen") != null || square.getBuilding().getRandomRoom("cafeteriakitchen") != null || square.getBuilding().getRandomRoom("chinesekitchen") != null || square.getBuilding().getRandomRoom("deepfry_kitchen") != null || square.getBuilding().getRandomRoom("deepfry_kitchen") != null || square.getBuilding().getRandomRoom("dinerkitchen") != null || square.getBuilding().getRandomRoom("donut_kitchen") != null || square.getBuilding().getRandomRoom("fishchipskitchen") != null || square.getBuilding().getRandomRoom("gigamartkitchen") != null || square.getBuilding().getRandomRoom("icecreamkitchen") != null || square.getBuilding().getRandomRoom("italiankitchen") != null || square.getBuilding().getRandomRoom("jayschicken_kitchen") != null || square.getBuilding().getRandomRoom("restaurantkitchen") != null || square.getBuilding().getRandomRoom("italiankitchen") != null || square.getBuilding().getRandomRoom("jayschicken_kitchen") != null || square.getBuilding().getRandomRoom("kitchen_crepe") != null || square.getBuilding().getRandomRoom("mexicankitchen") != null || square.getBuilding().getRandomRoom("pizzakitchen") != null || square.getBuilding().getRandomRoom("restaurantkitchen") != null || square.getBuilding().getRandomRoom("seafoodkitchen") != null || square.getBuilding().getRandomRoom("sushikitchen") != null)) {
            return true;
        }
        if ((outfitName.contains("ConstructionWorker") || outfitName.contains("Foreman") || outfitName.contains("Mechanic") || outfitName.contains("MetalWorker")) && (square.getBuilding().getRandomRoom("batfactory") != null || square.getBuilding().getRandomRoom("batteryfactory") != null || square.getBuilding().getRandomRoom("brewery") != null || square.getBuilding().getRandomRoom("cabinetfactory") != null || square.getBuilding().getRandomRoom("dogfoodfactory") != null || square.getBuilding().getRandomRoom("factory") != null || square.getBuilding().getRandomRoom("fryshipping") != null || square.getBuilding().getRandomRoom("metalshop") != null || square.getBuilding().getRandomRoom("radiofactory") != null || square.getBuilding().getRandomRoom("warehouse") != null || square.getBuilding().getRandomRoom("warehouse") != null)) {
            return true;
        }
        return true;
    }

    public boolean spawnBuildingKeyOnZombie(IsoZombie zombie) {
        IsoGridSquare square = zombie.getSquare();
        if (square != null && square.getBuilding() != null && square.getBuilding().getDef() != null) {
            return this.spawnBuildingKeyOnZombie(zombie, square.getBuilding().getDef());
        }
        return false;
    }

    public boolean spawnBuildingKeyOnZombie(IsoZombie zombie, BuildingDef def) {
        if ((float)Rand.Next(100) >= 1.0f * this.getKeySpawnChanceD100()) {
            String keyType = "Base.Key1";
            Object key = InventoryItemFactory.CreateItem("Base.Key1");
            if (key != null) {
                ((InventoryItem)key).setKeyId(def.getKeyId());
                ItemPickerJava.KeyNamer.nameKey(key, def.getFreeSquareInRoom());
                zombie.addItemToSpawnAtDeath((InventoryItem)key);
                return true;
            }
        } else {
            Object keyringItem = InventoryItemFactory.CreateItem(ItemKey.Container.KEY_RING);
            if (keyringItem instanceof InventoryContainer) {
                InventoryContainer keyring = (InventoryContainer)keyringItem;
                String keyType = "Base.Key1";
                InventoryItem key = keyring.getInventory().AddItem("Base.Key1");
                if (key != null) {
                    ArrayList<BaseVehicle> vehicles;
                    ItemPickerJava.KeyNamer.nameKey(key, def.getFreeSquareInRoom());
                    key.setKeyId(def.getKeyId());
                    if ((float)Rand.Next(100) < 1.0f * this.getKeySpawnChanceD100() && !(vehicles = IsoWorld.instance.currentCell.getVehicles()).isEmpty()) {
                        boolean isGood;
                        BaseVehicle vehicle = zombie.getNearVehicle();
                        boolean bl = isGood = vehicle != null && !vehicle.isPreviouslyMoved() && !vehicle.getKeySpawned() && vehicle.checkZombieKeyForVehicle(zombie, vehicle.getScriptName());
                        if (!isGood) {
                            vehicle = vehicles.get(Rand.Next(vehicles.size()));
                        }
                        boolean bl2 = isGood = vehicle != null && !vehicle.getScript().neverSpawnKey() && !vehicle.isPreviouslyMoved() && !vehicle.getKeySpawned() && vehicle.checkZombieKeyForVehicle(zombie, vehicle.getScriptName());
                        if (isGood) {
                            InventoryItem key2 = vehicle.createVehicleKey();
                            vehicle.keySpawned = 1;
                            vehicle.setPreviouslyMoved(true);
                            vehicle.keyNamerVehicle(key);
                            keyring.getInventory().AddItem(key2);
                        }
                    }
                    zombie.addItemToSpawnAtDeath((InventoryItem)keyringItem);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkAndSpawnZombieForBuildingKey(IsoZombie zombie) {
        return this.checkAndSpawnZombieForBuildingKey(zombie, false);
    }

    public boolean checkAndSpawnZombieForBuildingKey(IsoZombie zombie, boolean bandits) {
        BuildingDef def;
        boolean key = zombie.shouldZombieHaveKey(bandits);
        if (!key) {
            return false;
        }
        IsoGridSquare square = null;
        boolean outside = false;
        if (zombie.getBuilding() != null && zombie.getBuilding().getDef() != null && zombie.getBuilding().getDef().getKeyId() != -1) {
            def = zombie.getBuilding().getDef();
            square = zombie.getSquare();
        } else {
            float py;
            float px = zombie.getX();
            def = AmbientStreamManager.getNearestBuilding(px, py = zombie.getY());
            if (def != null && !def.isAllExplored()) {
                outside = true;
                square = def.getFreeSquareInRoom();
            }
        }
        if (square != null && this.checkZombieKeyForBuilding(zombie.getOutfitName(), square)) {
            String roomName = null;
            boolean isKey = true;
            if (!outside && zombie.getSquare().getRoom() != null) {
                roomName = zombie.getSquare().getRoom().getName();
            }
            if (roomName != null) {
                String outfitName = zombie.getOutfitName();
                if (roomName.equals("cells") || roomName.equals("prisoncells") && !outfitName.contains("Police") && !outfitName.equals("PrisonGuard")) {
                    isKey = false;
                }
            }
            if (isKey) {
                return this.spawnBuildingKeyOnZombie(zombie, def);
            }
        }
        return false;
    }

    private static float doKeySandboxSettings(int value) {
        switch (value) {
            case 1: {
                return 0.0f;
            }
            case 2: {
                return 0.05f;
            }
            case 3: {
                return 0.2f;
            }
            case 4: {
                return 0.6f;
            }
            case 5: {
                return 1.0f;
            }
            case 6: {
                return 2.0f;
            }
            case 7: {
                return 2.4f;
            }
        }
        return 0.6f;
    }
}

