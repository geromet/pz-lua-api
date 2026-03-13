/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import fmod.fmod.Audio;
import java.util.ArrayList;
import java.util.Stack;
import zombie.GameTime;
import zombie.Lua.LuaEventManager;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.PerformanceSettings;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoRainSplash;
import zombie.iso.objects.IsoRaindrop;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameServer;

@UsedFromLua
public class RainManager {
    private static boolean isRaining;
    public static int numActiveRainSplashes;
    public static int numActiveRaindrops;
    public static int maxRainSplashObjects;
    public static int maxRaindropObjects;
    public static float rainSplashAnimDelay;
    public static int addNewSplashesDelay;
    public static int addNewSplashesTimer;
    public static float raindropGravity;
    public static float gravModMin;
    public static float gravModMax;
    public static float raindropStartDistance;
    public static IsoGridSquare[] playerLocation;
    public static IsoGridSquare[] playerOldLocation;
    public static boolean playerMoved;
    public static int rainRadius;
    public static Audio rainAmbient;
    public static Audio thunderAmbient;
    public static ColorInfo rainSplashTintMod;
    public static ColorInfo raindropTintMod;
    public static ColorInfo darkRaindropTintMod;
    public static ArrayList<IsoRainSplash> rainSplashStack;
    public static ArrayList<IsoRaindrop> raindropStack;
    public static Stack<IsoRainSplash> rainSplashReuseStack;
    public static Stack<IsoRaindrop> raindropReuseStack;
    private static float rainChangeTimer;
    private static float rainChangeRate;
    private static final float RainChangeRateMin = 0.006f;
    private static final float RainChangeRateMax = 0.01f;
    public static float rainIntensity;
    public static float rainDesiredIntensity;
    private static int randRain;
    public static int randRainMin;
    public static int randRainMax;
    private static boolean stopRain;
    static Audio outsideAmbient;
    static Audio outsideNightAmbient;
    static ColorInfo adjustedRainSplashTintMod;

    public static void reset() {
        rainSplashStack.clear();
        raindropStack.clear();
        raindropReuseStack.clear();
        rainSplashReuseStack.clear();
        numActiveRainSplashes = 0;
        numActiveRaindrops = 0;
        for (int i = 0; i < 4; ++i) {
            RainManager.playerLocation[i] = null;
            RainManager.playerOldLocation[i] = null;
        }
        rainAmbient = null;
        thunderAmbient = null;
        isRaining = false;
        stopRain = false;
    }

    public static void AddRaindrop(IsoRaindrop newRaindrop) {
        if (numActiveRaindrops < maxRaindropObjects) {
            raindropStack.add(newRaindrop);
            ++numActiveRaindrops;
        } else {
            IsoRaindrop oldestRaindropObject = null;
            int oldestAge = -1;
            for (int i = 0; i < raindropStack.size(); ++i) {
                if (RainManager.raindropStack.get((int)i).life <= oldestAge) continue;
                oldestAge = RainManager.raindropStack.get((int)i).life;
                oldestRaindropObject = raindropStack.get(i);
            }
            if (oldestRaindropObject != null) {
                RainManager.RemoveRaindrop(oldestRaindropObject);
                raindropStack.add(newRaindrop);
                ++numActiveRaindrops;
            }
        }
    }

    public static void AddRainSplash(IsoRainSplash newRainSplash) {
        if (numActiveRainSplashes < maxRainSplashObjects) {
            rainSplashStack.add(newRainSplash);
            ++numActiveRainSplashes;
        } else {
            IsoRainSplash oldestRainSplashObject = null;
            int oldestAge = -1;
            for (int i = 0; i < rainSplashStack.size(); ++i) {
                if (RainManager.rainSplashStack.get((int)i).age <= oldestAge) continue;
                oldestAge = RainManager.rainSplashStack.get((int)i).age;
                oldestRainSplashObject = rainSplashStack.get(i);
            }
            RainManager.RemoveRainSplash(oldestRainSplashObject);
            rainSplashStack.add(newRainSplash);
            ++numActiveRainSplashes;
        }
    }

    public static void AddSplashes() {
        if (addNewSplashesTimer > 0) {
            --addNewSplashesTimer;
        } else {
            int i;
            addNewSplashesTimer = (int)((float)addNewSplashesDelay * ((float)PerformanceSettings.getLockFPS() / 30.0f));
            if (!stopRain) {
                if (playerMoved) {
                    for (i = rainSplashStack.size() - 1; i >= 0; --i) {
                        IsoRainSplash rainsplash = rainSplashStack.get(i);
                        if (RainManager.inBounds(rainsplash.square)) continue;
                        RainManager.RemoveRainSplash(rainsplash);
                    }
                    for (i = raindropStack.size() - 1; i >= 0; --i) {
                        IsoRaindrop raindrop = raindropStack.get(i);
                        if (RainManager.inBounds(raindrop.square)) continue;
                        RainManager.RemoveRaindrop(raindrop);
                    }
                }
                int numPlayers = 0;
                for (int i2 = 0; i2 < IsoPlayer.numPlayers; ++i2) {
                    if (IsoPlayer.players[i2] == null) continue;
                    ++numPlayers;
                }
                int area = rainRadius * 2 * rainRadius * 2;
                int count = area / (randRain + 1);
                count = Math.min(maxRainSplashObjects, count);
                while (numActiveRainSplashes > count * numPlayers) {
                    RainManager.RemoveRainSplash(rainSplashStack.get(0));
                }
                while (numActiveRaindrops > count * numPlayers) {
                    RainManager.RemoveRaindrop(raindropStack.get(0));
                }
                IsoCell cell = IsoWorld.instance.currentCell;
                for (int p = 0; p < IsoPlayer.numPlayers; ++p) {
                    if (IsoPlayer.players[p] == null || playerLocation[p] == null) continue;
                    for (int i3 = 0; i3 < count; ++i3) {
                        int x = Rand.Next(-rainRadius, rainRadius);
                        int y = Rand.Next(-rainRadius, rainRadius);
                        IsoGridSquare newSquare = cell.getGridSquare(playerLocation[p].getX() + x, playerLocation[p].getY() + y, 0);
                        if (newSquare == null || !newSquare.isSeen(p) || newSquare.getProperties().has(IsoFlagType.vegitation) || !newSquare.getProperties().has(IsoFlagType.exterior)) continue;
                        RainManager.StartRainSplash(cell, newSquare, true);
                    }
                }
            }
            playerMoved = false;
            if (!stopRain) {
                if (--randRain < randRainMin) {
                    randRain = randRainMin;
                }
            } else if ((randRain = (int)((float)randRain - 1.0f * GameTime.instance.getMultiplier())) < randRainMin) {
                RainManager.removeAll();
                randRain = randRainMin;
            } else {
                for (i = rainSplashStack.size() - 1; i >= 0; --i) {
                    if (Rand.Next(randRain) != 0) continue;
                    IsoRainSplash rainsplash = rainSplashStack.get(i);
                    RainManager.RemoveRainSplash(rainsplash);
                }
                for (i = raindropStack.size() - 1; i >= 0; --i) {
                    if (Rand.Next(randRain) != 0) continue;
                    IsoRaindrop raindrop = raindropStack.get(i);
                    RainManager.RemoveRaindrop(raindrop);
                }
            }
        }
    }

    public static void RemoveRaindrop(IsoRaindrop dyingRaindrop) {
        if (dyingRaindrop.square != null) {
            dyingRaindrop.square.getProperties().unset(IsoFlagType.HasRaindrop);
            dyingRaindrop.square.setRainDrop(null);
            dyingRaindrop.square = null;
        }
        raindropStack.remove(dyingRaindrop);
        --numActiveRaindrops;
        raindropReuseStack.push(dyingRaindrop);
    }

    public static void RemoveRainSplash(IsoRainSplash dyingRainSplash) {
        if (dyingRainSplash.square != null) {
            dyingRainSplash.square.getProperties().unset(IsoFlagType.HasRainSplashes);
            dyingRainSplash.square.setRainSplash(null);
            dyingRainSplash.square = null;
        }
        rainSplashStack.remove(dyingRainSplash);
        --numActiveRainSplashes;
        rainSplashReuseStack.push(dyingRainSplash);
    }

    public static void SetPlayerLocation(int playerIndex, IsoGridSquare playerCurrentSquare) {
        RainManager.playerOldLocation[playerIndex] = playerLocation[playerIndex];
        RainManager.playerLocation[playerIndex] = playerCurrentSquare;
        if (playerOldLocation[playerIndex] != playerLocation[playerIndex]) {
            playerMoved = true;
        }
    }

    public static Boolean isRaining() {
        return ClimateManager.getInstance().isRaining();
    }

    public static void stopRaining() {
        stopRain = true;
        randRain = randRainMax;
        rainDesiredIntensity = 0.0f;
        if (GameServer.server) {
            GameServer.stopRain();
        }
        LuaEventManager.triggerEvent("OnRainStop");
    }

    public static void startRaining() {
    }

    public static void StartRaindrop(IsoCell cell, IsoGridSquare gridSquare, boolean canSee) {
        if (!gridSquare.getProperties().has(IsoFlagType.HasRaindrop)) {
            if (!raindropReuseStack.isEmpty()) {
                if (canSee) {
                    if (gridSquare.getRainDrop() != null) {
                        return;
                    }
                    IsoRaindrop newRaindrop = raindropReuseStack.pop();
                    newRaindrop.Reset(gridSquare, canSee);
                    gridSquare.setRainDrop(newRaindrop);
                }
            } else if (canSee) {
                if (gridSquare.getRainDrop() != null) {
                    return;
                }
                IsoRaindrop newRaindrop = new IsoRaindrop(cell, gridSquare, canSee);
                gridSquare.setRainDrop(newRaindrop);
            }
        }
    }

    public static void StartRainSplash(IsoCell cell, IsoGridSquare gridSquare, boolean canSee) {
    }

    public static void Update() {
        isRaining = ClimateManager.getInstance().isRaining();
        rainIntensity = isRaining ? ClimateManager.getInstance().getPrecipitationIntensity() : 0.0f;
    }

    public static void UpdateServer() {
    }

    public static void setRandRainMax(int pRandRainMax) {
        randRain = randRainMax = pRandRainMax;
    }

    public static void setRandRainMin(int pRandRainMin) {
        randRainMin = pRandRainMin;
    }

    public static boolean inBounds(IsoGridSquare sq) {
        if (sq == null) {
            return false;
        }
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            IsoPlayer player = IsoPlayer.players[i];
            if (player == null || playerLocation[i] == null) continue;
            if (sq.getX() < playerLocation[i].getX() - rainRadius || sq.getX() >= playerLocation[i].getX() + rainRadius) {
                return true;
            }
            if (sq.getY() >= playerLocation[i].getY() - rainRadius && sq.getY() < playerLocation[i].getY() + rainRadius) continue;
            return true;
        }
        return false;
    }

    public static void RemoveAllOn(IsoGridSquare sq) {
        if (sq.getRainDrop() != null) {
            RainManager.RemoveRaindrop(sq.getRainDrop());
        }
        if (sq.getRainSplash() != null) {
            RainManager.RemoveRainSplash(sq.getRainSplash());
        }
    }

    public static float getRainIntensity() {
        return ClimateManager.getInstance().getPrecipitationIntensity();
    }

    private static void removeAll() {
        int i;
        for (i = rainSplashStack.size() - 1; i >= 0; --i) {
            IsoRainSplash rainsplash = rainSplashStack.get(i);
            RainManager.RemoveRainSplash(rainsplash);
        }
        for (i = raindropStack.size() - 1; i >= 0; --i) {
            IsoRaindrop raindrop = raindropStack.get(i);
            RainManager.RemoveRaindrop(raindrop);
        }
        raindropStack.clear();
        rainSplashStack.clear();
        numActiveRainSplashes = 0;
        numActiveRaindrops = 0;
    }

    private static boolean interruptSleep(IsoPlayer ply) {
        IsoObject bed;
        return ply.isAsleep() && ply.isOutside() && ply.getBed() != null && !ply.getBed().isTent() && ((bed = ply.getBed()).getCell().getGridSquare(bed.getX(), bed.getY(), bed.getZ() + 1.0f) == null || bed.getCell().getGridSquare(bed.getX(), bed.getY(), bed.getZ() + 1.0f).getFloor() == null);
    }

    static {
        maxRainSplashObjects = 500;
        maxRaindropObjects = 500;
        rainSplashAnimDelay = 0.2f;
        addNewSplashesTimer = addNewSplashesDelay = 30;
        raindropGravity = 0.065f;
        gravModMin = 0.28f;
        gravModMax = 0.5f;
        raindropStartDistance = 850.0f;
        playerLocation = new IsoGridSquare[4];
        playerOldLocation = new IsoGridSquare[4];
        playerMoved = true;
        rainRadius = 18;
        rainSplashTintMod = new ColorInfo(0.8f, 0.9f, 1.0f, 0.3f);
        raindropTintMod = new ColorInfo(0.8f, 0.9f, 1.0f, 0.3f);
        darkRaindropTintMod = new ColorInfo(0.8f, 0.9f, 1.0f, 0.3f);
        rainSplashStack = new ArrayList(1600);
        raindropStack = new ArrayList(1600);
        rainSplashReuseStack = new Stack();
        raindropReuseStack = new Stack();
        rainChangeTimer = 1.0f;
        rainChangeRate = 0.01f;
        rainIntensity = 1.0f;
        rainDesiredIntensity = 1.0f;
        adjustedRainSplashTintMod = new ColorInfo();
    }
}

