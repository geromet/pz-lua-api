/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.areas;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.SurvivorDesc;
import zombie.core.opengl.RenderSettings;
import zombie.core.random.Rand;
import zombie.inventory.ItemContainer;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.areas.BuildingScore;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.IsoRoomExit;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameServer;
import zombie.scripting.objects.ItemType;

@UsedFromLua
public final class IsoBuilding {
    public Rectangle bounds;
    public final Vector<IsoRoomExit> exits = new Vector();
    public boolean isResidence = true;
    public final ArrayList<ItemContainer> container = new ArrayList();
    public final Vector<IsoRoom> rooms = new Vector();
    public final Vector<IsoWindow> windows = new Vector();
    public int id;
    public static int idCount;
    public int safety;
    public int transparentWalls;
    private boolean isToxic;
    public static float poorBuildingScore;
    public static float goodBuildingScore;
    public int scoreUpdate;
    public BuildingDef def;
    public boolean seenInside;
    public ArrayList<IsoLightSource> lights = new ArrayList();
    static ArrayList<IsoRoom> tempo;
    static ArrayList<ItemContainer> tempContainer;
    static ArrayList<String> randomContainerChoices;
    static ArrayList<IsoWindow> windowchoices;

    public int getRoomsNumber() {
        return this.rooms.size();
    }

    public IsoBuilding() {
        this.id = idCount++;
        this.scoreUpdate = -120 + Rand.Next(120);
    }

    public int getID() {
        return this.id;
    }

    public void TriggerAlarm() {
    }

    public IsoBuilding(IsoCell cell) {
        this.id = idCount++;
        this.scoreUpdate = -120 + Rand.Next(120);
    }

    public boolean ContainsAllItems(Stack<String> items) {
        return false;
    }

    public float ScoreBuildingPersonSpecific(SurvivorDesc desc, boolean bFarGood) {
        float dist;
        float score = 0.0f;
        score += (float)(this.rooms.size() * 5);
        score += (float)(this.exits.size() * 15);
        score -= (float)(this.transparentWalls * 10);
        for (int n = 0; n < this.container.size(); ++n) {
            ItemContainer con = this.container.get(n);
            score += (float)(con.items.size() * 3);
        }
        if (!IsoWorld.instance.currentCell.getBuildingScores().containsKey(this.id)) {
            BuildingScore s = new BuildingScore(this);
            s.building = this;
            IsoWorld.instance.currentCell.getBuildingScores().put(this.id, s);
            this.ScoreBuildingGeneral(s);
        }
        BuildingScore s = IsoWorld.instance.currentCell.getBuildingScores().get(this.id);
        score += (s.defense + s.food + (float)s.size + s.weapons + s.wood) * 10.0f;
        int dx = -10000;
        int dy = -10000;
        if (!this.exits.isEmpty()) {
            IsoRoomExit exit = this.exits.get(0);
            dx = exit.x;
            dy = exit.y;
        }
        if ((dist = IsoUtils.DistanceManhatten(desc.getInstance().getX(), desc.getInstance().getY(), dx, dy)) > 0.0f) {
            score = bFarGood ? (score *= dist * 0.5f) : (score /= dist * 0.5f);
        }
        return score;
    }

    public BuildingDef getDef() {
        return this.def;
    }

    public void update() {
        if (this.exits.isEmpty()) {
            return;
        }
        int safescore = 0;
        int tilecount = 0;
        for (int n = 0; n < this.rooms.size(); ++n) {
            IsoRoom room = this.rooms.get(n);
            if (room.layer != 0) continue;
            for (int z = 0; z < room.tileList.size(); ++z) {
                ++tilecount;
                IsoGridSquare isoGridSquare = room.tileList.get(z);
            }
        }
        if (tilecount == 0) {
            ++tilecount;
        }
        safescore = (int)((float)safescore / (float)tilecount);
        --this.scoreUpdate;
        if (this.scoreUpdate <= 0) {
            BuildingScore score;
            this.scoreUpdate += 120;
            if (IsoWorld.instance.currentCell.getBuildingScores().containsKey(this.id)) {
                score = IsoWorld.instance.currentCell.getBuildingScores().get(this.id);
            } else {
                score = new BuildingScore(this);
                score.building = this;
            }
            score = this.ScoreBuildingGeneral(score);
            score.defense += (float)(safescore * 10);
            this.safety = safescore;
            IsoWorld.instance.currentCell.getBuildingScores().put(this.id, score);
        }
    }

    public void AddRoom(IsoRoom room) {
        this.rooms.add(room);
        if (this.bounds == null) {
            this.bounds = (Rectangle)room.bounds.clone();
        }
        if (room != null && room.bounds != null) {
            this.bounds.add(room.bounds);
        }
    }

    public void CalculateExits() {
        for (IsoRoom room : this.rooms) {
            for (IsoRoomExit exit : room.exits) {
                if (exit.to.from != null || room.layer != 0) continue;
                this.exits.add(exit);
            }
        }
    }

    public void CalculateWindows() {
        for (IsoRoom room : this.rooms) {
            for (IsoGridSquare sq : room.tileList) {
                IsoWindow isoWindow;
                IsoObject spec;
                int n;
                boolean bSameBuilding;
                IsoGridSquare s = sq.getCell().getGridSquare(sq.getX(), sq.getY() + 1, sq.getZ());
                IsoGridSquare e = sq.getCell().getGridSquare(sq.getX() + 1, sq.getY(), sq.getZ());
                if (sq.getProperties().has(IsoFlagType.collideN) && sq.getProperties().has(IsoFlagType.transparentN)) {
                    ++room.transparentWalls;
                    ++this.transparentWalls;
                }
                if (sq.getProperties().has(IsoFlagType.collideW) && sq.getProperties().has(IsoFlagType.transparentW)) {
                    ++room.transparentWalls;
                    ++this.transparentWalls;
                }
                if (s != null) {
                    boolean bl = bSameBuilding = s.getRoom() != null;
                    if (s.getRoom() != null && s.getRoom().building != room.building) {
                        bSameBuilding = false;
                    }
                    if (s.getProperties().has(IsoFlagType.collideN) && s.getProperties().has(IsoFlagType.transparentN) && !bSameBuilding) {
                        ++room.transparentWalls;
                        ++this.transparentWalls;
                    }
                }
                if (e != null) {
                    boolean bl = bSameBuilding = e.getRoom() != null;
                    if (e.getRoom() != null && e.getRoom().building != room.building) {
                        bSameBuilding = false;
                    }
                    if (e.getProperties().has(IsoFlagType.collideW) && e.getProperties().has(IsoFlagType.transparentW) && !bSameBuilding) {
                        ++room.transparentWalls;
                        ++this.transparentWalls;
                    }
                }
                for (n = 0; n < sq.getSpecialObjects().size(); ++n) {
                    spec = sq.getSpecialObjects().get(n);
                    if (!(spec instanceof IsoWindow)) continue;
                    isoWindow = (IsoWindow)spec;
                    this.windows.add(isoWindow);
                }
                if (s != null) {
                    for (n = 0; n < s.getSpecialObjects().size(); ++n) {
                        spec = s.getSpecialObjects().get(n);
                        if (!(spec instanceof IsoWindow)) continue;
                        isoWindow = (IsoWindow)spec;
                        this.windows.add(isoWindow);
                    }
                }
                if (e == null) continue;
                for (n = 0; n < e.getSpecialObjects().size(); ++n) {
                    spec = e.getSpecialObjects().get(n);
                    if (!(spec instanceof IsoWindow)) continue;
                    isoWindow = (IsoWindow)spec;
                    this.windows.add(isoWindow);
                }
            }
        }
    }

    public void FillContainers() {
        for (IsoRoom room : this.rooms) {
            if (room.roomDef.contains("shop")) {
                this.isResidence = false;
            }
            for (IsoGridSquare sq : room.tileList) {
                for (int n = 0; n < sq.getObjects().size(); ++n) {
                    IsoObject obj = sq.getObjects().get(n);
                    if (obj.hasWater()) {
                        room.getWaterSources().add(obj);
                    }
                    if (obj.container == null) continue;
                    this.container.add(obj.container);
                    room.containers.add(obj.container);
                }
                if (!sq.getProperties().has(IsoFlagType.bed)) continue;
                room.beds.add(sq);
            }
        }
    }

    public ItemContainer getContainerWith(ItemType itemType) {
        for (IsoRoom room : this.rooms) {
            for (ItemContainer container : room.containers) {
                if (!container.HasType(itemType)) continue;
                return container;
            }
        }
        return null;
    }

    public IsoRoom getRandomRoom() {
        if (this.rooms.isEmpty()) {
            return null;
        }
        return this.rooms.get(Rand.Next(this.rooms.size()));
    }

    private BuildingScore ScoreBuildingGeneral(BuildingScore score) {
        score.food = 0.0f;
        score.defense = 0.0f;
        score.weapons = 0.0f;
        score.wood = 0.0f;
        score.building = this;
        score.size = 0;
        score.defense += (float)((this.exits.size() - 1) * 140);
        score.defense -= (float)(this.transparentWalls * 40);
        score.size = this.rooms.size() * 10;
        score.size += this.container.size() * 10;
        return score;
    }

    public IsoGridSquare getFreeTile() {
        IsoRoom room;
        IsoGridSquare sq;
        while ((sq = (room = this.rooms.get(Rand.Next(this.rooms.size()))).getFreeTile()) == null) {
        }
        return sq;
    }

    public boolean hasWater() {
        Iterator<IsoRoom> it = this.rooms.iterator();
        while (it != null && it.hasNext()) {
            IsoRoom r = it.next();
            if (r.waterSources.isEmpty()) continue;
            IsoObject waterSource = null;
            for (int i = 0; i < r.waterSources.size(); ++i) {
                if (!r.waterSources.get(i).hasWater()) continue;
                waterSource = r.waterSources.get(i);
                break;
            }
            if (waterSource == null) continue;
            return true;
        }
        return false;
    }

    public void CreateFrom(BuildingDef building, IsoMetaCell metaCell) {
        for (int n = 0; n < building.rooms.size(); ++n) {
            RoomDef roomDef = building.rooms.get(n);
            IsoRoom r = IsoWorld.instance.getMetaGrid().getRoomByID(roomDef.id);
            if (r == null) continue;
            r.building = this;
            if (this.rooms.contains(r)) continue;
            this.rooms.add(r);
        }
    }

    public void setAllExplored(boolean b) {
        this.def.alarmed = false;
        for (int n = 0; n < this.rooms.size(); ++n) {
            IsoRoom r = this.rooms.get(n);
            r.def.setExplored(b);
            for (int x = r.def.getX(); x <= r.def.getX2(); ++x) {
                for (int y = r.def.getY(); y <= r.def.getY2(); ++y) {
                    IsoGridSquare g = IsoWorld.instance.currentCell.getGridSquare(x, y, r.def.level);
                    if (g == null) continue;
                    g.setHourSeenToCurrent();
                }
            }
        }
    }

    public boolean isAllExplored() {
        for (int n = 0; n < this.rooms.size(); ++n) {
            IsoRoom r = this.rooms.get(n);
            if (r.def.explored) continue;
            return false;
        }
        return true;
    }

    public void addWindow(IsoWindow obj, boolean bOtherTile, IsoGridSquare from, IsoBuilding building) {
        this.windows.add(obj);
        IsoGridSquare squareToAdd = bOtherTile ? obj.square : from;
        if (squareToAdd == null) {
            return;
        }
        if (squareToAdd.getRoom() != null) {
            return;
        }
        float r = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.7f;
        float g = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.7f;
        float b = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.7f;
        int radius = 7;
        IsoLightSource ls = new IsoLightSource(squareToAdd.getX(), squareToAdd.getY(), squareToAdd.getZ(), r, g, b, 7, building);
        this.lights.add(ls);
        IsoWorld.instance.currentCell.getLamppostPositions().add(ls);
    }

    public void addWindow(IsoWindow obj, boolean bOtherTile) {
        this.addWindow(obj, bOtherTile, obj.square, null);
    }

    public void addDoor(IsoDoor obj, boolean bOtherTile, IsoGridSquare from, IsoBuilding building) {
        IsoGridSquare squareToAdd = bOtherTile ? obj.square : from;
        if (squareToAdd == null) {
            return;
        }
        if (squareToAdd.getRoom() != null) {
            return;
        }
        float r = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.7f;
        float g = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.7f;
        float b = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.7f;
        int radius = 7;
        IsoLightSource ls = new IsoLightSource(squareToAdd.getX(), squareToAdd.getY(), squareToAdd.getZ(), r, g, b, 7, building);
        this.lights.add(ls);
        IsoWorld.instance.currentCell.getLamppostPositions().add(ls);
    }

    public void addDoor(IsoDoor obj, boolean bOtherTile) {
        this.addDoor(obj, bOtherTile, obj.square, null);
    }

    public boolean isResidential() {
        return this.getDef().isResidential();
    }

    public boolean containsRoom(String room) {
        for (int n = 0; n < this.rooms.size(); ++n) {
            if (!room.equals(this.rooms.get(n).getName())) continue;
            return true;
        }
        return false;
    }

    public IsoRoom getRandomRoom(String room) {
        tempo.clear();
        for (int n = 0; n < this.rooms.size(); ++n) {
            if (!room.equals(this.rooms.get(n).getName())) continue;
            tempo.add(this.rooms.get(n));
        }
        if (tempo.isEmpty()) {
            return null;
        }
        return tempo.get(Rand.Next(tempo.size()));
    }

    public boolean hasRoom(String room) {
        for (int n = 0; n < this.rooms.size(); ++n) {
            if (!room.equals(this.rooms.get(n).getName())) continue;
            return true;
        }
        return false;
    }

    public ItemContainer getRandomContainer(String type) {
        int n;
        randomContainerChoices.clear();
        String[] choices = null;
        if (type != null) {
            choices = type.split(",");
        }
        if (choices != null) {
            for (n = 0; n < choices.length; ++n) {
                randomContainerChoices.add(choices[n]);
            }
        }
        tempContainer.clear();
        for (n = 0; n < this.rooms.size(); ++n) {
            IsoRoom room = this.rooms.get(n);
            for (int m = 0; m < room.containers.size(); ++m) {
                ItemContainer c = room.containers.get(m);
                if (type != null && !randomContainerChoices.contains(c.getType())) continue;
                tempContainer.add(c);
            }
        }
        if (tempContainer.isEmpty()) {
            return null;
        }
        return tempContainer.get(Rand.Next(tempContainer.size()));
    }

    public ItemContainer getRandomContainerSingle(String type) {
        tempContainer.clear();
        for (int n = 0; n < this.rooms.size(); ++n) {
            IsoRoom room = this.rooms.get(n);
            for (int m = 0; m < room.containers.size(); ++m) {
                ItemContainer c = room.containers.get(m);
                if (type != null && !type.equals(c.getType())) continue;
                tempContainer.add(c);
            }
        }
        if (tempContainer.isEmpty()) {
            return null;
        }
        return tempContainer.get(Rand.Next(tempContainer.size()));
    }

    public IsoWindow getRandomFirstFloorWindow() {
        windowchoices.clear();
        windowchoices.addAll(this.windows);
        for (int n = 0; n < windowchoices.size(); ++n) {
            if (!(windowchoices.get(n).getZ() > 0.0f)) continue;
            windowchoices.remove(n);
        }
        if (!windowchoices.isEmpty()) {
            return windowchoices.get(Rand.Next(windowchoices.size()));
        }
        return null;
    }

    public boolean isToxic() {
        return this.isToxic;
    }

    public void setToxic(boolean isToxic) {
        this.isToxic = isToxic;
        if (GameServer.server) {
            GameServer.sendToxicBuilding(this.getDef().getX() + this.getDef().getW() / 2, this.getDef().getY() + this.getDef().getH() / 2, isToxic);
        }
    }

    public void forceAwake() {
        for (int x = this.def.getX(); x <= this.def.getX2(); ++x) {
            for (int y = this.def.getY(); y <= this.def.getY2(); ++y) {
                for (int z = 0; z <= 4; ++z) {
                    IsoGridSquare g = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
                    if (g == null) continue;
                    for (int i = 0; i < g.getMovingObjects().size(); ++i) {
                        if (!(g.getMovingObjects().get(i) instanceof IsoGameCharacter)) continue;
                        ((IsoGameCharacter)g.getMovingObjects().get(i)).forceAwake();
                    }
                }
            }
        }
    }

    public boolean hasBasement() {
        return false;
    }

    public boolean isEntirelyEmptyOutside() {
        return this.getDef() != null && this.getDef().isEntirelyEmptyOutside();
    }

    static {
        poorBuildingScore = 10.0f;
        goodBuildingScore = 100.0f;
        tempo = new ArrayList();
        tempContainer = new ArrayList();
        randomContainerChoices = new ArrayList();
        windowchoices = new ArrayList();
    }
}

