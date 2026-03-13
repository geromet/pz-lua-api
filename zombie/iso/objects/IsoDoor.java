/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.function.Consumer;
import org.joml.Matrix4f;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.Lua.LuaEventManager;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.ai.states.ThumpState;
import zombie.characters.BaseCharacterSoundEmitter;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.Shader;
import zombie.core.properties.IsoObjectChange;
import zombie.core.properties.IsoPropertyType;
import zombie.core.properties.PropertyContainer;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.model.IsoObjectAnimations;
import zombie.core.skinnedmodel.model.IsoObjectModelDrawer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.Key;
import zombie.iso.BuildingDef;
import zombie.iso.ICurtain;
import zombie.iso.IHasHealth;
import zombie.iso.ILockableDoor;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.SpriteModel;
import zombie.iso.Vector2;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderObjectHighlight;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.ObjectRenderEffects;
import zombie.iso.objects.RenderEffectType;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;
import zombie.network.fields.NetObject;
import zombie.pathfind.PolygonalMap2;
import zombie.scripting.objects.SoundKey;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public class IsoDoor
extends IsoObject
implements BarricadeAble,
Thumpable,
IHasHealth,
ILockableDoor,
ICurtain {
    public int health = 500;
    public boolean lockedByKey;
    private boolean haveKey;
    public boolean locked;
    public int maxHealth = 500;
    public int pushedMaxStrength;
    public int pushedStrength;
    public DoorType type = DoorType.WeakWooden;
    private IsoSprite closedSprite;
    public boolean north;
    int gid = -1;
    public boolean open;
    private IsoSprite openSprite;
    private boolean destroyed;
    private boolean hasCurtain;
    private boolean curtainInside;
    private boolean curtainOpen;
    private static final ColorInfo curtainColor = new ColorInfo();
    private short lastPlayerOnlineId = (short)-1;
    private boolean wasTryingToggleLockedDoor;
    private boolean wasTryingToggleBarricadedDoor;
    private static final ColorInfo stCol = new ColorInfo();
    KahluaTable table;
    public static final Vector2 tempo = new Vector2();
    private IsoSpriteInstance curtainN;
    private IsoSpriteInstance curtainS;
    private IsoSpriteInstance curtainW;
    private IsoSpriteInstance curtainE;
    private IsoSpriteInstance curtainNopen;
    private IsoSpriteInstance curtainSopen;
    private IsoSpriteInstance curtainWopen;
    private IsoSpriteInstance curtainEopen;
    private static final int[] DoubleDoorNorthSpriteOffset = new int[]{5, 3, 4, 4};
    private static final int[] DoubleDoorWestSpriteOffset = new int[]{4, 4, 5, 3};
    private static final int[] DoubleDoorNorthClosedXOffset = new int[]{0, 1, 2, 3};
    private static final int[] DoubleDoorNorthOpenXOffset = new int[]{0, 0, 3, 3};
    private static final int[] DoubleDoorNorthClosedYOffset = new int[]{0, 0, 0, 0};
    private static final int[] DoubleDoorNorthOpenYOffset = new int[]{0, 1, 1, 0};
    private static final int[] DoubleDoorWestClosedXOffset = new int[]{0, 0, 0, 0};
    private static final int[] DoubleDoorWestOpenXOffset = new int[]{0, 1, 1, 0};
    private static final int[] DoubleDoorWestClosedYOffset = new int[]{0, -1, -2, -3};
    private static final int[] DoubleDoorWestOpenYOffset = new int[]{0, 0, -3, -3};

    public IsoDoor(IsoCell cell) {
        super(cell);
    }

    @Override
    public String getObjectName() {
        return "Door";
    }

    @Override
    public void render(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (!this.hasCurtain) {
            super.render(x, y, z, info, bDoAttached, bWallLightingPass, shader);
            return;
        }
        this.initCurtainSprites();
        IsoDirections edge = this.getSpriteEdge(false);
        this.prerender(x, y, z, info, bDoAttached, bWallLightingPass, edge);
        super.render(x, y, z, info, bDoAttached, bWallLightingPass, shader);
        this.postrender(x, y, z, info, bDoAttached, bWallLightingPass, edge);
    }

    @Override
    public void renderWallTile(IsoDirections dir, float x, float y, float z, ColorInfo col, boolean bDoAttached, boolean bWallLightingPass, Shader shader, Consumer<TextureDraw> texdModifier) {
        this.sx = 0.0f;
        int ddIndex = IsoDoor.getDoubleDoorIndex(this);
        if (ddIndex != -1) {
            IsoObject master = null;
            if (ddIndex == 2) {
                master = IsoDoor.getDoubleDoorObject(this, 1);
            } else if (ddIndex == 3) {
                master = IsoDoor.getDoubleDoorObject(this, 4);
            }
            if (master != null && master.getSpriteModel() != null) {
                this.updateRenderInfoForObjectPicker(x, y, z, col);
                this.sx = 0.0f;
                return;
            }
        }
        if (!this.hasCurtain) {
            super.renderWallTile(dir, x, y, z, col, bDoAttached, bWallLightingPass, shader, texdModifier);
            return;
        }
        this.initCurtainSprites();
        this.initCurtainColor();
        if (PerformanceSettings.fboRenderChunk && this.getSpriteModel() != null) {
            this.renderCurtainModel(this.isCurtainOpen() ? this.curtainNopen : this.curtainN, this.getX() + 0.5f, this.getY() + 0.5f, this.getZ(), curtainColor);
            super.render(x, y, z, col, bDoAttached, bWallLightingPass, shader);
            return;
        }
        IsoDirections edge = this.getSpriteEdge(false);
        this.prerender(x, y, z, curtainColor, bDoAttached, bWallLightingPass, edge);
        super.renderWallTile(dir, x, y, z, col, bDoAttached, bWallLightingPass, shader, texdModifier);
        this.postrender(x, y, z, curtainColor, bDoAttached, bWallLightingPass, edge);
    }

    private ColorInfo initCurtainColor() {
        if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
            return curtainColor.set(1.0f, 1.0f, 1.0f, 1.0f);
        }
        IsoGridSquare curtainSq = this.getSheetSquare();
        if (curtainSq == null) {
            return curtainColor.set(1.0f, 1.0f, 1.0f, 1.0f);
        }
        ColorInfo lightInfo = curtainSq.getLightInfo(IsoCamera.frameState.playerIndex);
        if (lightInfo == null) {
            return curtainColor.set(1.0f, 1.0f, 1.0f, 1.0f);
        }
        curtainColor.set(lightInfo);
        IsoDirections edge = this.getSpriteEdge(false);
        float interpX = switch (edge) {
            case IsoDirections.N -> 0.5f;
            case IsoDirections.S -> 0.5f;
            case IsoDirections.W -> 0.0f;
            case IsoDirections.E -> 1.0f;
            default -> 0.0f;
        };
        float interpY = switch (edge) {
            case IsoDirections.N -> 0.0f;
            case IsoDirections.S -> 1.0f;
            case IsoDirections.W -> 0.5f;
            case IsoDirections.E -> 1.5f;
            default -> 0.0f;
        };
        curtainSq.interpolateLight(curtainColor, interpX, interpY);
        IsoDoor.curtainColor.a = FBORenderCell.instance.calculateWindowTargetAlpha(IsoCamera.frameState.playerIndex, this, this.getOppositeSquare(), this.getNorth());
        return curtainColor;
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        IsoObjectAnimations.getInstance().addDancingDoor(this);
    }

    @Override
    public void removeFromWorld() {
        super.removeFromWorld();
        IsoObjectAnimations.getInstance().removeDancingDoor(this);
    }

    public void checkKeyHighlight(int playerIndex) {
        Key key = Key.highlightDoor[playerIndex].key;
        if (key != null) {
            boolean seen = this.square.isSeen(playerIndex);
            if (!seen) {
                IsoGridSquare oppositeSq = this.getOppositeSquare();
                boolean bl = seen = oppositeSq != null && oppositeSq.isSeen(playerIndex);
            }
            if (seen) {
                this.checkKeyId();
                if (this.getKeyId() == key.getKeyId()) {
                    this.setHighlighted(playerIndex, true, false);
                }
            }
        }
    }

    private void prerender(float x, float y, float z, ColorInfo col, boolean bDoAttached, boolean bWallLightingPass, IsoDirections edge) {
        if (Core.tileScale == 1) {
            switch (edge) {
                case N: {
                    this.prerender1xN(x, y, z, col, bDoAttached, bWallLightingPass, null);
                    break;
                }
                case S: {
                    this.prerender1xS(x, y, z, col, bDoAttached, bWallLightingPass, null);
                    break;
                }
                case W: {
                    this.prerender1xW(x, y, z, col, bDoAttached, bWallLightingPass, null);
                    break;
                }
                case E: {
                    this.prerender1xE(x, y, z, col, bDoAttached, bWallLightingPass, null);
                }
            }
            return;
        }
        switch (edge) {
            case N: {
                this.prerender2xN(x, y, z, col, bDoAttached, bWallLightingPass, null);
                break;
            }
            case S: {
                this.prerender2xS(x, y, z, col, bDoAttached, bWallLightingPass, null);
                break;
            }
            case W: {
                this.prerender2xW(x, y, z, col, bDoAttached, bWallLightingPass, null);
                break;
            }
            case E: {
                this.prerender2xE(x, y, z, col, bDoAttached, bWallLightingPass, null);
            }
        }
    }

    private void postrender(float x, float y, float z, ColorInfo col, boolean bDoAttached, boolean bWallLightingPass, IsoDirections edge) {
        if (Core.tileScale == 1) {
            switch (edge) {
                case N: {
                    this.postrender1xN(x, y, z, col, bDoAttached, bWallLightingPass, null);
                    break;
                }
                case S: {
                    this.postrender1xS(x, y, z, col, bDoAttached, bWallLightingPass, null);
                    break;
                }
                case W: {
                    this.postrender1xW(x, y, z, col, bDoAttached, bWallLightingPass, null);
                    break;
                }
                case E: {
                    this.postrender1xE(x, y, z, col, bDoAttached, bWallLightingPass, null);
                }
            }
            return;
        }
        switch (edge) {
            case N: {
                this.postrender2xN(x, y, z, col, bDoAttached, bWallLightingPass, null);
                break;
            }
            case S: {
                this.postrender2xS(x, y, z, col, bDoAttached, bWallLightingPass, null);
                break;
            }
            case W: {
                this.postrender2xW(x, y, z, col, bDoAttached, bWallLightingPass, null);
                break;
            }
            case E: {
                this.postrender2xE(x, y, z, col, bDoAttached, bWallLightingPass, null);
            }
        }
    }

    private void prerender1xN(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (this.curtainInside) {
            if (!this.north && this.open) {
                (this.curtainOpen ? this.curtainSopen : this.curtainS).render(null, x, y - 1.0f, z, this.dir, this.offsetX + 3.0f, this.offsetY + (float)(this.curtainOpen ? -14 : -14), info, true);
            }
        } else if (this.north && !this.open) {
            (this.curtainOpen ? this.curtainSopen : this.curtainS).render(null, x, y - 1.0f, z, this.dir, this.offsetX - 1.0f - 1.0f, this.offsetY + -15.0f, info, true);
        }
    }

    private void postrender1xN(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (this.curtainInside) {
            if (this.north && !this.open) {
                (this.curtainOpen ? this.curtainNopen : this.curtainN).render(null, x, y, z, this.dir, this.offsetX - 10.0f - 1.0f, this.offsetY + -10.0f, info, true);
            }
        } else if (!this.north && this.open) {
            (this.curtainOpen ? this.curtainNopen : this.curtainN).render(null, x, y, z, this.dir, this.offsetX - 4.0f, this.offsetY + (float)(this.curtainOpen ? -10 : -10), info, true);
        }
    }

    private void prerender1xS(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        assert (!this.north && this.open);
        if (!this.curtainInside) {
            (this.curtainOpen ? this.curtainSopen : this.curtainS).render(null, x, y, z, this.dir, this.offsetX + (float)((this.curtainOpen ? -14 : -14) / 2), this.offsetY + (float)((this.curtainOpen ? -16 : -16) / 2), info, true);
        }
    }

    private void postrender1xS(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        assert (!this.north && this.open);
        if (this.curtainInside) {
            (this.curtainOpen ? this.curtainNopen : this.curtainN).render(null, x, y + 1.0f, z, this.dir, this.offsetX + (float)((this.curtainOpen ? -28 : -28) / 2), this.offsetY + (float)((this.curtainOpen ? -8 : -8) / 2), info, true);
        }
    }

    private void prerender1xW(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (this.curtainInside) {
            if (this.north && this.open) {
                (this.curtainOpen ? this.curtainEopen : this.curtainE).render(null, x - 1.0f, y, z, this.dir, this.offsetX + (float)(this.curtainOpen ? -16 : -18), this.offsetY + (float)(this.curtainOpen ? -14 : -15), info, true);
            }
            if (!this.north && this.open) {
                (this.curtainOpen ? this.curtainSopen : this.curtainS).render(null, x, y - 1.0f, z, this.dir, this.offsetX + 3.0f, this.offsetY + (float)(this.curtainOpen ? -14 : -14), info, true);
            }
        } else {
            if (this.north && !this.open) {
                (this.curtainOpen ? this.curtainSopen : this.curtainS).render(null, x, y - 1.0f, z, this.dir, this.offsetX - 1.0f - 1.0f, this.offsetY + -15.0f, info, true);
            }
            if (!this.north && !this.open) {
                (this.curtainOpen ? this.curtainEopen : this.curtainE).render(null, x - 1.0f, y, z, this.dir, this.offsetX + (float)(this.curtainOpen ? -12 : -14), this.offsetY + (float)(this.curtainOpen ? -14 : -15), info, true);
            }
        }
    }

    private void postrender1xW(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (this.curtainInside) {
            if (this.north && !this.open) {
                (this.curtainOpen ? this.curtainNopen : this.curtainN).render(null, x, y, z, this.dir, this.offsetX - 10.0f - 1.0f, this.offsetY + -10.0f, info, true);
            }
            if (!this.north && !this.open) {
                (this.curtainOpen ? this.curtainWopen : this.curtainW).render(null, x, y, z, this.dir, this.offsetX - 2.0f - 1.0f, this.offsetY + -10.0f, info, true);
            }
        } else {
            if (this.north && this.open) {
                (this.curtainOpen ? this.curtainWopen : this.curtainW).render(null, x, y, z, this.dir, this.offsetX - 9.0f, this.offsetY + -10.0f, info, true);
            }
            if (!this.north && this.open) {
                (this.curtainOpen ? this.curtainNopen : this.curtainN).render(null, x, y, z, this.dir, this.offsetX - 4.0f, this.offsetY + (float)(this.curtainOpen ? -10 : -10), info, true);
            }
        }
    }

    private void prerender1xE(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        assert (this.north && this.open);
        if (!this.curtainInside) {
            (this.curtainOpen ? this.curtainEopen : this.curtainE).render(null, x, y, z, this.dir, this.offsetX + (float)((this.curtainOpen ? -13 : -18) / 2), this.offsetY + (float)((this.curtainOpen ? -15 : -18) / 2), info, true);
        }
    }

    private void postrender1xE(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        assert (this.north && this.open);
        if (this.curtainInside) {
            (this.curtainOpen ? this.curtainWopen : this.curtainW).render(null, x + 1.0f, y, z, this.dir, this.offsetX + (float)(this.curtainOpen ? false : false), this.offsetY + (float)(this.curtainOpen ? false : false), info, true);
        }
    }

    private void prerender2xN(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (this.curtainInside) {
            if (!this.north && this.open) {
                (this.curtainOpen ? this.curtainSopen : this.curtainS).render(null, x, y - 1.0f, z, this.dir, this.offsetX + 7.0f, this.offsetY + (float)(this.curtainOpen ? -28 : -28), info, false);
            }
        } else if (this.north && !this.open) {
            (this.curtainOpen ? this.curtainSopen : this.curtainS).render(null, x, y - 1.0f, z, this.dir, this.offsetX - 3.0f, this.offsetY + (float)(this.curtainOpen ? -30 : -30), info, false);
        }
    }

    private void postrender2xN(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (this.curtainInside) {
            if (this.north && !this.open) {
                (this.curtainOpen ? this.curtainNopen : this.curtainN).render(null, x, y, z, this.dir, this.offsetX - 20.0f, this.offsetY + (float)(this.curtainOpen ? -20 : -20), info, false);
            }
        } else if (!this.north && this.open) {
            (this.curtainOpen ? this.curtainNopen : this.curtainN).render(null, x, y, z, this.dir, this.offsetX - 8.0f, this.offsetY + (float)(this.curtainOpen ? -20 : -20), info, false);
        }
    }

    private void prerender2xS(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        assert (!this.north && this.open);
        if (!this.curtainInside) {
            (this.curtainOpen ? this.curtainSopen : this.curtainS).render(null, x, y, z, this.dir, this.offsetX + (float)(this.curtainOpen ? -14 : -14), this.offsetY + (float)(this.curtainOpen ? -16 : -16), info, false);
        }
    }

    private void postrender2xS(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        assert (!this.north && this.open);
        if (this.curtainInside) {
            (this.curtainOpen ? this.curtainNopen : this.curtainN).render(null, x, y + 1.0f, z, this.dir, this.offsetX + (float)(this.curtainOpen ? -28 : -28), this.offsetY + (float)(this.curtainOpen ? -8 : -8), info, false);
        }
    }

    private void prerender2xW(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (this.curtainInside) {
            if (this.north && this.open) {
                (this.curtainOpen ? this.curtainEopen : this.curtainE).render(null, x - 1.0f, y, z, this.dir, this.offsetX + (float)(this.curtainOpen ? -32 : -37), this.offsetY + (float)(this.curtainOpen ? -28 : -31), info, false);
            }
        } else if (!this.north && !this.open) {
            (this.curtainOpen ? this.curtainEopen : this.curtainE).render(null, x - 1.0f, y, z, this.dir, this.offsetX + (float)(this.curtainOpen ? -22 : -26), this.offsetY + (float)(this.curtainOpen ? -28 : -31), info, false);
        }
    }

    private void postrender2xW(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (this.curtainInside) {
            if (!this.north && !this.open) {
                (this.curtainOpen ? this.curtainWopen : this.curtainW).render(null, x, y, z, this.dir, this.offsetX - 5.0f, this.offsetY + (float)(this.curtainOpen ? -20 : -20), info, false);
            }
        } else if (this.north && this.open) {
            (this.curtainOpen ? this.curtainWopen : this.curtainW).render(null, x, y, z, this.dir, this.offsetX - 19.0f, this.offsetY + (float)(this.curtainOpen ? -20 : -20), info, false);
        }
    }

    private void prerender2xE(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        assert (this.north && this.open);
        if (!this.curtainInside) {
            (this.curtainOpen ? this.curtainEopen : this.curtainE).render(null, x, y, z, this.dir, this.offsetX + (float)(this.curtainOpen ? -13 : -18), this.offsetY + (float)(this.curtainOpen ? -15 : -18), info, false);
        }
    }

    private void postrender2xE(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        assert (this.north && this.open);
        if (this.curtainInside) {
            (this.curtainOpen ? this.curtainWopen : this.curtainW).render(null, x + 1.0f, y, z, this.dir, this.offsetX + (float)(this.curtainOpen ? false : false), this.offsetY + (float)(this.curtainOpen ? false : false), info, false);
        }
    }

    private void renderCurtainSpriteOrModel(IsoSpriteInstance spriteInstance, float x, float y, float z, IsoDirections dir, float offsetX, float offsetY, ColorInfo col, boolean bDoRenderPrep) {
        if (!this.renderCurtainModel(spriteInstance, this.getX() + 0.5f, this.getY() + 0.5f, z, col)) {
            spriteInstance.render(null, x, y, z, dir, offsetX, offsetY, col, bDoRenderPrep);
        }
    }

    private boolean renderCurtainModel(IsoSpriteInstance spriteInstance, float x, float y, float z, ColorInfo col) {
        boolean bHighlighted;
        if (!PerformanceSettings.fboRenderChunk) {
            return false;
        }
        SpriteModel spriteModel = spriteInstance.getParentSprite().spriteModel;
        if (spriteModel == null) {
            return false;
        }
        float offset = this.sprite.getProperties().isSurfaceOffset() ? (float)this.sprite.getProperties().getSurface() : 0.0f;
        int playerIndex = IsoCamera.frameState.playerIndex;
        ColorInfo colOrig = col;
        float a = col.a;
        float targetAlpha = this.getTargetAlpha(IsoCamera.frameState.playerIndex);
        float alpha = this.getAlpha(IsoCamera.frameState.playerIndex);
        if (alpha < targetAlpha) {
            if ((alpha += IsoSprite.alphaStep) > targetAlpha) {
                alpha = targetAlpha;
            }
        } else if (alpha > targetAlpha && (alpha -= IsoSprite.alphaStep) < targetAlpha) {
            alpha = targetAlpha;
        }
        if (alpha < 0.0f) {
            alpha = 0.0f;
        }
        if (alpha > 1.0f) {
            alpha = 1.0f;
        }
        this.setAlpha(IsoCamera.frameState.playerIndex, alpha);
        col.a *= this.getAlpha(IsoCamera.frameState.playerIndex);
        if (DebugOptions.instance.fboRenderChunk.forceAlphaAndTargetOne.getValue()) {
            col.a = 1.0f;
        }
        if (bHighlighted = FBORenderObjectHighlight.getInstance().shouldRenderObjectHighlight(this)) {
            stCol.set(this.getHighlightColor(playerIndex));
            if (this.isBlink()) {
                IsoDoor.stCol.a *= Core.blinkAlpha;
            }
            IsoDoor.stCol.r = col.r * (1.0f - IsoDoor.stCol.a) + this.getHighlightColor((int)playerIndex).r * IsoDoor.stCol.a;
            IsoDoor.stCol.g = col.g * (1.0f - IsoDoor.stCol.a) + this.getHighlightColor((int)playerIndex).g * IsoDoor.stCol.a;
            IsoDoor.stCol.b = col.b * (1.0f - IsoDoor.stCol.a) + this.getHighlightColor((int)playerIndex).b * IsoDoor.stCol.a;
            IsoDoor.stCol.a = col.a;
            col = stCol;
        }
        if (FBORenderCell.instance.isBlackedOutBuildingSquare(this.getSquare())) {
            float fade = 1.0f - FBORenderCell.instance.getBlackedOutRoomFadeRatio(this.getSquare());
            col = stCol.set(col.r * fade, col.g * fade, col.b * fade, col.a);
        }
        IsoDirections edge = this.getSpriteEdge(true);
        if (IsoBarricade.GetBarricadeOnSquare(this.getSquare(), edge) != null) {
            x += (float)edge.dx() * -0.07f;
            y += (float)edge.dy() * -0.07f;
        }
        AnimationPlayer animationPlayer = IsoObjectAnimations.getInstance().getAnimationPlayer(this);
        float oldScale = spriteModel.scale;
        spriteModel.scale = 1.5f;
        String attachmentName = this.curtainInside ? "curtain1" : "curtain2";
        Matrix4f xfrm = IsoObjectAnimations.getInstance().getAttachmentTransform(this, attachmentName, BaseVehicle.allocMatrix4f());
        ObjectRenderEffects renderEffects = this.getObjectRenderEffectsToApply();
        IsoObjectModelDrawer.RenderStatus renderStatus = renderEffects == null ? IsoObjectModelDrawer.renderMain(spriteModel, x, y, z, col, this.getRenderYOffset() + offset, this.getSpriteModel(), xfrm, false, true) : IsoObjectModelDrawer.renderMain(spriteModel, x + (float)renderEffects.x1 * 1.5f, y + (float)renderEffects.y1 * 1.5f, z, col, this.getRenderYOffset() + offset, this.getSpriteModel(), xfrm, false, true);
        BaseVehicle.releaseMatrix4f(xfrm);
        spriteModel.scale = oldScale;
        colOrig.a = a;
        if (renderStatus == IsoObjectModelDrawer.RenderStatus.Loading && PerformanceSettings.fboRenderChunk && FBORenderChunkManager.instance.isCaching()) {
            FBORenderCell.instance.handleDelayedLoading(this);
            return true;
        }
        return renderStatus == IsoObjectModelDrawer.RenderStatus.Ready;
    }

    public IsoDirections getSpriteEdge(boolean ignoreOpen) {
        if (!this.open || ignoreOpen) {
            return this.north ? IsoDirections.N : IsoDirections.W;
        }
        PropertyContainer properties = this.getProperties();
        if (properties != null && properties.has(IsoPropertyType.GARAGE_DOOR)) {
            return this.north ? IsoDirections.N : IsoDirections.W;
        }
        if (properties != null && properties.has(IsoFlagType.attachedE)) {
            return IsoDirections.E;
        }
        if (properties != null && properties.has(IsoFlagType.attachedS)) {
            return IsoDirections.S;
        }
        return this.north ? IsoDirections.W : IsoDirections.N;
    }

    public IsoDoor(IsoCell cell, IsoGridSquare gridSquare, IsoSprite gid, boolean north) {
        this.open = gid.getProperties().has(IsoFlagType.open);
        this.outlineOnMouseover = true;
        this.pushedStrength = 2500;
        this.pushedMaxStrength = 2500;
        int openSpriteOffset = 2;
        if (gid.getProperties().has(IsoPropertyType.DOUBLE_DOOR)) {
            openSpriteOffset = 4;
        }
        if (gid.getProperties().has(IsoPropertyType.GARAGE_DOOR)) {
            openSpriteOffset = 8;
        }
        this.closedSprite = this.open ? IsoSprite.getSprite(IsoSpriteManager.instance, gid, -openSpriteOffset) : gid;
        this.openSprite = this.open ? gid : IsoSprite.getSprite(IsoSpriteManager.instance, gid, openSpriteOffset);
        this.sprite = this.open ? this.openSprite : this.closedSprite;
        this.square = gridSquare;
        this.north = north;
        switch (this.type.ordinal()) {
            case 0: {
                this.health = 500;
                this.maxHealth = 500;
                break;
            }
            case 1: {
                this.health = 800;
                this.maxHealth = 800;
            }
        }
        if (this.getProperties().has("forceLocked")) {
            this.health = 2000;
            this.maxHealth = 2000;
        }
        if (this.getSprite().getName() != null && this.getSprite().getName().contains("fences")) {
            this.health = 100;
            this.maxHealth = 100;
        }
        int randLock = 69;
        if (SandboxOptions.instance.lockedHouses.getValue() == 1) {
            randLock = -1;
        } else if (SandboxOptions.instance.lockedHouses.getValue() == 2) {
            randLock = 5;
        } else if (SandboxOptions.instance.lockedHouses.getValue() == 3) {
            randLock = 10;
        } else if (SandboxOptions.instance.lockedHouses.getValue() == 4) {
            randLock = 50;
        } else if (SandboxOptions.instance.lockedHouses.getValue() == 5) {
            randLock = 60;
        } else if (SandboxOptions.instance.lockedHouses.getValue() == 6) {
            randLock = 70;
        }
        if (randLock > -1) {
            boolean bl = this.locked = Rand.Next(100) < randLock;
            if (this.locked && Rand.Next(3) == 0) {
                this.lockedByKey = true;
            }
        }
        if (this.getProperties().has("forceLocked")) {
            this.locked = true;
            this.lockedByKey = true;
        }
        if (this.open) {
            this.locked = false;
            this.lockedByKey = false;
        }
    }

    public IsoDoor(IsoCell cell, IsoGridSquare gridSquare, String gid, boolean north) {
        this.outlineOnMouseover = true;
        this.pushedStrength = 2500;
        this.pushedMaxStrength = 2500;
        IsoSprite existing = IsoSpriteManager.instance.namedMap.get(gid);
        if (existing != null && existing.getProperties().has(IsoFlagType.open)) {
            this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, 0);
            this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, -2);
            this.open = true;
        } else {
            this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, 0);
            this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, 2);
            this.open = false;
        }
        IsoSprite validSprite = this.open ? this.openSprite : this.closedSprite;
        String garageDoor = validSprite.getProperties().get(IsoPropertyType.GARAGE_DOOR);
        if (garageDoor != null) {
            int index = Integer.parseInt(garageDoor);
            if (index <= 3) {
                this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, 8);
            } else {
                this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, -8);
            }
        }
        this.sprite = this.open ? this.openSprite : this.closedSprite;
        this.square = gridSquare;
        this.north = north;
        switch (this.type.ordinal()) {
            case 0: {
                this.health = 500;
                this.maxHealth = 500;
                break;
            }
            case 1: {
                this.health = 800;
                this.maxHealth = 800;
            }
        }
        if (this.getSprite().getName() != null && this.getSprite().getName().contains("fences")) {
            this.health = 100;
            this.maxHealth = 100;
        }
    }

    public IsoDoor(IsoCell cell, IsoGridSquare gridSquare, String gid, boolean north, KahluaTable table) {
        this.outlineOnMouseover = true;
        this.pushedStrength = 2500;
        this.pushedMaxStrength = 2500;
        this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, 0);
        this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, 2);
        this.table = table;
        this.sprite = this.closedSprite;
        String garageDoor = this.sprite.getProperties().get(IsoPropertyType.GARAGE_DOOR);
        if (garageDoor != null) {
            int index = Integer.parseInt(garageDoor);
            if (index <= 3) {
                this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, 8);
            } else {
                this.openSprite = this.sprite;
                this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, -8);
            }
        }
        this.square = gridSquare;
        this.north = north;
        switch (this.type.ordinal()) {
            case 0: {
                this.health = 500;
                this.maxHealth = 500;
                break;
            }
            case 1: {
                this.health = 800;
                this.maxHealth = 800;
            }
        }
        if (this.getSprite().getName() != null && this.getSprite().getName().contains("fences")) {
            this.health = 100;
            this.maxHealth = 100;
        }
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        super.load(input, worldVersion, isDebugSave);
        this.open = input.get() != 0;
        this.locked = input.get() != 0;
        this.north = input.get() != 0;
        this.health = input.getInt();
        this.maxHealth = input.getInt();
        this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, input.getInt());
        this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, input.getInt());
        this.outlineOnMouseover = true;
        this.pushedStrength = 2500;
        this.pushedMaxStrength = 2500;
        this.keyId = input.getInt();
        this.lockedByKey = input.get() != 0;
        byte b = input.get();
        if ((b & 1) != 0) {
            this.hasCurtain = true;
            this.curtainOpen = (b & 2) != 0;
            this.curtainInside = (b & 4) != 0;
        }
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        super.save(output, isDebugSave);
        output.put(this.open ? (byte)1 : 0);
        output.put(this.locked ? (byte)1 : 0);
        output.put(this.north ? (byte)1 : 0);
        output.putInt(this.health);
        output.putInt(this.maxHealth);
        output.putInt(this.closedSprite.id);
        output.putInt(this.openSprite.id);
        output.putInt(this.getKeyId());
        output.put(this.isLockedByKey() ? (byte)1 : 0);
        byte b = 0;
        if (this.hasCurtain) {
            b = (byte)(b | 1);
            if (this.curtainOpen) {
                b = (byte)(b | 2);
            }
            if (this.curtainInside) {
                b = (byte)(b | 4);
            }
        }
        output.put(b);
    }

    @Override
    public void saveState(ByteBuffer bb) throws IOException {
        bb.put((byte)(this.open ? 1 : 0));
        bb.put((byte)(this.locked ? 1 : 0));
        bb.put((byte)(this.lockedByKey ? 1 : 0));
    }

    @Override
    public void loadState(ByteBuffer bb) throws IOException {
        boolean lockedByKey;
        boolean open = bb.get() != 0;
        boolean locked = bb.get() != 0;
        boolean bl = lockedByKey = bb.get() != 0;
        if (open != this.open) {
            this.open = open;
            IsoSprite isoSprite = this.sprite = open ? this.openSprite : this.closedSprite;
        }
        if (locked != this.locked) {
            this.locked = locked;
        }
        if (lockedByKey != this.lockedByKey) {
            this.lockedByKey = lockedByKey;
        }
    }

    @Override
    public boolean isDestroyed() {
        return this.destroyed;
    }

    @Override
    public boolean IsOpen() {
        return this.open;
    }

    public boolean IsStrengthenedByPushedItems() {
        return false;
    }

    @Override
    public boolean onMouseLeftClick(int x, int y) {
        return false;
    }

    @Override
    public boolean TestPathfindCollide(IsoMovingObject obj, IsoGridSquare from, IsoGridSquare to) {
        IsoSurvivor isoSurvivor;
        boolean north = this.north;
        if (!this.isBarricaded()) {
            return false;
        }
        if (obj instanceof IsoSurvivor && (isoSurvivor = (IsoSurvivor)obj).getInventory().contains("Hammer")) {
            return false;
        }
        if (this.open) {
            boolean bl = north = !north;
        }
        if (from == this.square) {
            if (north && to.getY() < from.getY()) {
                return true;
            }
            if (!north && to.getX() < from.getX()) {
                return true;
            }
        } else {
            if (north && to.getY() > from.getY()) {
                return true;
            }
            if (!north && to.getX() > from.getX()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean TestCollide(IsoMovingObject obj, IsoGridSquare from, IsoGridSquare to) {
        boolean north = this.north;
        if (this.open) {
            return false;
        }
        if (from == this.square) {
            if (north && to.getY() < from.getY()) {
                if (obj != null) {
                    obj.collideWith(this);
                }
                return true;
            }
            if (!north && to.getX() < from.getX()) {
                if (obj != null) {
                    obj.collideWith(this);
                }
                return true;
            }
        } else {
            if (north && to.getY() > from.getY()) {
                if (obj != null) {
                    obj.collideWith(this);
                }
                return true;
            }
            if (!north && to.getX() > from.getX()) {
                if (obj != null) {
                    obj.collideWith(this);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public IsoObject.VisionResult TestVision(IsoGridSquare from, IsoGridSquare to) {
        boolean doorTrans;
        boolean bl = doorTrans = this.sprite != null && this.sprite.getProperties().has(IsoPropertyType.DOOR_TRANS);
        if (this.sprite != null && this.sprite.getProperties().has(IsoPropertyType.GARAGE_DOOR) && this.open) {
            doorTrans = true;
        }
        if (this.open) {
            doorTrans = true;
        } else if (this.hasCurtain && !this.curtainOpen) {
            doorTrans = false;
        }
        boolean north = this.north;
        if (this.open) {
            boolean bl2 = north = !north;
        }
        if (to.getZ() != from.getZ()) {
            return IsoObject.VisionResult.NoEffect;
        }
        if (from == this.square) {
            if (north && to.getY() < from.getY()) {
                if (doorTrans) {
                    return IsoObject.VisionResult.Unblocked;
                }
                return IsoObject.VisionResult.Blocked;
            }
            if (!north && to.getX() < from.getX()) {
                if (doorTrans) {
                    return IsoObject.VisionResult.Unblocked;
                }
                return IsoObject.VisionResult.Blocked;
            }
        } else {
            if (north && to.getY() > from.getY()) {
                if (doorTrans) {
                    return IsoObject.VisionResult.Unblocked;
                }
                return IsoObject.VisionResult.Blocked;
            }
            if (!north && to.getX() > from.getX()) {
                if (doorTrans) {
                    return IsoObject.VisionResult.Unblocked;
                }
                return IsoObject.VisionResult.Blocked;
            }
        }
        return IsoObject.VisionResult.NoEffect;
    }

    @Override
    public void Thump(IsoMovingObject thumper) {
        if (this.isDestroyed()) {
            return;
        }
        if (thumper instanceof IsoGameCharacter) {
            IsoGameCharacter isoGameCharacter = (IsoGameCharacter)thumper;
            Thumpable thumpable = this.getThumpableFor(isoGameCharacter);
            if (thumpable == null) {
                return;
            }
            if (thumpable != this) {
                thumpable.Thump(thumper);
                return;
            }
        }
        if (thumper instanceof IsoZombie) {
            IsoZombie isoZombie = (IsoZombie)thumper;
            if (!(isoZombie.cognition != 1 || this.open || this.locked && (thumper.getCurrentSquare() == null || thumper.getCurrentSquare().has(IsoFlagType.exterior)))) {
                this.ToggleDoor((IsoGameCharacter)thumper);
                if (this.open) {
                    return;
                }
            }
            if (GameClient.client) {
                if (isoZombie.isLocal()) {
                    GameClient.sendZombieHitThumpable((IsoGameCharacter)thumper, this);
                }
                return;
            }
            int tot = thumper.getSurroundingThumpers();
            int mult = ThumpState.getFastForwardDamageMultiplier();
            int max = isoZombie.strength;
            if (tot >= 2) {
                this.DirtySlice();
                this.Damage(isoZombie.strength * mult);
                if (SandboxOptions.instance.lore.strength.getValue() == 1) {
                    this.Damage(tot * 2 * mult);
                }
            }
            if (Core.gameMode.equals("LastStand")) {
                this.Damage(1 * mult);
            }
            WorldSoundManager.instance.addSound(thumper, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, true, 4.0f, 15.0f);
            this.setRenderEffect(RenderEffectType.Hit_Door, true);
        }
        if (this.health <= 0) {
            if (this.getSquare().getBuilding() != null) {
                this.getSquare().getBuilding().forceAwake();
            }
            this.playDoorSound(((IsoGameCharacter)thumper).getEmitter(), "Break");
            if (GameServer.server) {
                GameServer.PlayWorldSoundServer((IsoGameCharacter)thumper, "BreakDoor", false, thumper.getCurrentSquare(), 0.2f, 20.0f, 1.1f, true);
            }
            WorldSoundManager.instance.addSound(null, this.square.getX(), this.square.getY(), this.square.getZ(), 10, 20, true, 4.0f, 15.0f);
            thumper.setThumpTarget(null);
            if (IsoDoor.destroyDoubleDoor(this)) {
                return;
            }
            if (IsoDoor.destroyGarageDoor(this)) {
                return;
            }
            this.destroy();
        }
    }

    @Override
    public Thumpable getThumpableFor(IsoGameCharacter chr) {
        IsoBarricade barricade = this.getBarricadeForCharacter(chr);
        if (barricade != null) {
            return barricade;
        }
        barricade = this.getBarricadeOppositeCharacter(chr);
        if (barricade != null) {
            return barricade;
        }
        if (this.isDestroyed() || this.IsOpen()) {
            return null;
        }
        return this;
    }

    @Override
    public float getThumpCondition() {
        if (this.getMaxHealth() <= 0) {
            return 0.0f;
        }
        return (float)PZMath.clamp(this.getHealth(), 0, this.getMaxHealth()) / (float)this.getMaxHealth();
    }

    @Override
    public void WeaponHit(IsoGameCharacter owner, HandWeapon weapon) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (GameClient.client) {
            this.setRenderEffect(RenderEffectType.Hit_Door, true);
            return;
        }
        Thumpable thumpable = this.getThumpableFor(owner);
        if (thumpable == null) {
            return;
        }
        if (thumpable instanceof IsoBarricade) {
            thumpable.WeaponHit(owner, weapon);
            return;
        }
        LuaEventManager.triggerEvent("OnWeaponHitThumpable", owner, weapon, this);
        if (this.open) {
            return;
        }
        if (this.isDestroyed()) {
            return;
        }
        int perk = owner.getPerkLevel(PerkFactory.Perks.Strength);
        float mod = 1.0f;
        if (perk == 0) {
            mod = 0.5f;
        } else if (perk == 1) {
            mod = 0.63f;
        } else if (perk == 2) {
            mod = 0.76f;
        } else if (perk == 3) {
            mod = 0.89f;
        } else if (perk == 4) {
            mod = 1.02f;
        }
        if (perk == 6) {
            mod = 1.15f;
        } else if (perk == 7) {
            mod = 1.27f;
        } else if (perk == 8) {
            mod = 1.3f;
        } else if (perk == 9) {
            mod = 1.45f;
        } else if (perk == 10) {
            mod = 1.7f;
        }
        this.Damage((int)((float)weapon.getDoorDamage() * 2.0f * mod));
        this.setRenderEffect(RenderEffectType.Hit_Door, true);
        if (Rand.Next(10) == 0) {
            this.Damage((int)((float)weapon.getDoorDamage() * 6.0f * mod));
        }
        float delta = GameTime.getInstance().getThirtyFPSMultiplier();
        switch (owner.getPerkLevel(PerkFactory.Perks.Fitness)) {
            case 0: {
                owner.exert(0.01f * delta);
                break;
            }
            case 1: {
                owner.exert(0.007f * delta);
                break;
            }
            case 2: {
                owner.exert(0.0065f * delta);
                break;
            }
            case 3: {
                owner.exert(0.006f * delta);
                break;
            }
            case 4: {
                owner.exert(0.005f * delta);
                break;
            }
            case 5: {
                owner.exert(0.004f * delta);
                break;
            }
            case 6: {
                owner.exert(0.0035f * delta);
                break;
            }
            case 7: {
                owner.exert(0.003f * delta);
                break;
            }
            case 8: {
                owner.exert(0.0025f * delta);
                break;
            }
            case 9: {
                owner.exert(0.002f * delta);
            }
        }
        this.DirtySlice();
        if (weapon.getDoorHitSound() != null) {
            if (player != null) {
                player.setMeleeHitSurface(this.getSoundPrefix());
            }
            owner.getEmitter().playSound(weapon.getDoorHitSound(), this);
            if (GameServer.server) {
                GameServer.PlayWorldSoundServer(owner, weapon.getDoorHitSound(), false, this.getSquare(), 1.0f, 20.0f, 2.0f, false);
            }
        }
        WorldSoundManager.instance.addSound(owner, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, false, 0.0f, 15.0f);
        if (!this.IsStrengthenedByPushedItems() && this.health <= 0 || this.IsStrengthenedByPushedItems() && this.health <= -this.pushedMaxStrength) {
            this.playDoorSound(owner.getEmitter(), "Break");
            if (GameServer.server) {
                GameServer.PlayWorldSoundServer(owner, "BreakDoor", false, this.getSquare(), 0.2f, 20.0f, 1.1f, true);
            }
            WorldSoundManager.instance.addSound(owner, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, false, 0.0f, 15.0f);
            if (IsoDoor.destroyDoubleDoor(this)) {
                return;
            }
            if (IsoDoor.destroyGarageDoor(this)) {
                return;
            }
            this.destroy();
            LuaEventManager.triggerEvent("OnContainerUpdate");
        }
    }

    public void destroy() {
        if (this.sprite != null && this.sprite.getProperties().has(IsoPropertyType.GARAGE_DOOR)) {
            this.destroyed = true;
            this.square.transmitRemoveItemFromSquare(this);
            return;
        }
        PropertyContainer props = this.getProperties();
        if (props == null) {
            return;
        }
        String material = props.get("Material");
        String material2 = props.get("Material2");
        String material3 = props.get("Material3");
        if (!(StringUtils.isNullOrEmpty(material) && StringUtils.isNullOrEmpty(material2) && StringUtils.isNullOrEmpty(material3))) {
            this.addItemsFromProperties();
        } else {
            int numPlanks = Rand.Next(2) + 1;
            for (int i = 0; i < numPlanks; ++i) {
                this.square.AddWorldInventoryItem("Base.Plank", 0.0f, 0.0f, 0.0f);
            }
        }
        Object item = InventoryItemFactory.CreateItem("Base.Doorknob");
        ((InventoryItem)item).setKeyId(this.checkKeyId());
        this.square.AddWorldInventoryItem((InventoryItem)item, 0.0f, 0.0f, 0.0f);
        int numHinges = Rand.Next(3);
        for (int i = 0; i < numHinges; ++i) {
            this.square.AddWorldInventoryItem("Base.Hinge", 0.0f, 0.0f, 0.0f);
        }
        if (this.hasCurtain) {
            this.square.AddWorldInventoryItem("Base.Sheet", 0.0f, 0.0f, 0.0f);
        }
        this.destroyed = true;
        this.square.transmitRemoveItemFromSquare(this);
    }

    public IsoGridSquare getOtherSideOfDoor(IsoGameCharacter chr) {
        if (this.north) {
            if (chr.getCurrentSquare().getRoom() == this.square.getRoom()) {
                return IsoWorld.instance.currentCell.getGridSquare(this.square.getX(), this.square.getY() - 1, this.square.getZ());
            }
            return IsoWorld.instance.currentCell.getGridSquare(this.square.getX(), this.square.getY(), this.square.getZ());
        }
        if (chr.getCurrentSquare().getRoom() == this.square.getRoom()) {
            return IsoWorld.instance.currentCell.getGridSquare(this.square.getX() - 1, this.square.getY(), this.square.getZ());
        }
        return IsoWorld.instance.currentCell.getGridSquare(this.square.getX(), this.square.getY(), this.square.getZ());
    }

    @Deprecated
    public boolean isExteriorDoor(IsoGameCharacter chr) {
        return this.isExterior();
    }

    public boolean isExterior() {
        IsoGridSquare sq = this.getSquare();
        IsoGridSquare sqOtherSide = this.getOppositeSquare();
        if (sqOtherSide == null) {
            return false;
        }
        if (sq.has(IsoFlagType.exterior) && sqOtherSide.getBuilding() != null && sqOtherSide.getBuilding().getDef() != null) {
            return true;
        }
        return sq.getBuilding() != null && sq.getBuilding().getDef() != null && sqOtherSide.has(IsoFlagType.exterior);
    }

    @Override
    public boolean isHoppable() {
        if (this.IsOpen()) {
            return false;
        }
        if (this.closedSprite == null) {
            return false;
        }
        PropertyContainer props = this.closedSprite.getProperties();
        return props.has(IsoFlagType.HoppableN) || props.has(IsoFlagType.HoppableW);
    }

    @Override
    public boolean canClimbOver(IsoGameCharacter chr) {
        if (this.square == null) {
            return false;
        }
        if (!this.isHoppable()) {
            return false;
        }
        return chr == null || IsoWindow.canClimbThroughHelper(chr, this.getSquare(), this.getOppositeSquare(), this.north);
    }

    @Override
    public boolean couldBeOpen(IsoGameCharacter chr) {
        if (chr instanceof IsoAnimal) {
            return false;
        }
        if (this.isBarricaded()) {
            return false;
        }
        if (this.isLockedByKey() && chr instanceof IsoPlayer && (chr.getCurrentSquare().has(IsoFlagType.exterior) || this.getProperties().has("forceLocked") || this.getModData().rawget("CustomLock") != null && this.getModData().rawget("CustomLock") instanceof Boolean && ((Boolean)this.getModData().rawget("CustomLock")).booleanValue()) && chr.getInventory().haveThisKeyId(this.getKeyId()) == null) {
            return false;
        }
        if (this.isObstructed()) {
            return false;
        }
        return !"Tutorial".equals(Core.getInstance().getGameMode()) || !this.isLockedByKey();
    }

    public void ToggleDoorActual(IsoGameCharacter chr) {
        boolean wasOpen;
        boolean bUnlock;
        IsoPlayer player = Type.tryCastTo(chr, IsoPlayer.class);
        if (chr instanceof IsoAnimal) {
            return;
        }
        if (GameClient.client || GameServer.server) {
            short onlineID = chr != null ? (short)chr.getOnlineID() : (short)-1;
            this.forEachDoorObject(object -> {
                object.lastPlayerOnlineId = onlineID;
            });
        }
        if (GameServer.server && chr instanceof IsoPlayer && player.getRole().hasCapability(Capability.CanOpenLockedDoors)) {
            this.locked = false;
            this.setLockedByKey(false);
        }
        if (Core.debug && DebugOptions.instance.cheat.door.unlock.getValue()) {
            this.locked = false;
            this.setLockedByKey(false);
        }
        if (this.isHoppable()) {
            this.locked = false;
            this.setLockedByKey(false);
        }
        this.setWasTryingToggleLockedDoor(false);
        this.setWasTryingToggleBarricadedDoor(false);
        if (this.isBarricaded()) {
            if (chr != null) {
                this.TriggerBarricadedDoor(chr);
            }
            this.setWasTryingToggleBarricadedDoor(true);
            this.sync(this.open ? 1 : 0);
            return;
        }
        this.checkKeyId();
        if (this.locked && !this.lockedByKey && this.getKeyId() != -1) {
            this.lockedByKey = true;
        }
        if (chr instanceof IsoPlayer) {
            IsoPlayer isoPlayer = (IsoPlayer)chr;
            if (!this.open) {
                isoPlayer.timeSinceOpenDoor = 0.0f;
            } else {
                isoPlayer.timeSinceCloseDoor = 0.0f;
            }
        }
        this.DirtySlice();
        IsoGridSquare.recalcLightTime = -1.0f;
        ++Core.dirtyGlobalLightsCount;
        GameTime.instance.lightSourceUpdate = 100.0f;
        this.square.InvalidateSpecialObjectPaths();
        if (this.isLockedByKey() && chr != null && chr instanceof IsoPlayer && (chr.getCurrentSquare().has(IsoFlagType.exterior) || this.getProperties().has("forceLocked") || this.getModData().rawget("CustomLock") != null && this.getModData().rawget("CustomLock") instanceof Boolean && ((Boolean)this.getModData().rawget("CustomLock")).booleanValue()) && !this.open) {
            if (chr.getInventory().haveThisKeyId(this.getKeyId()) == null) {
                this.TriggerLockedDoor(chr);
                this.setWasTryingToggleLockedDoor(true);
                this.sync(this.open ? 1 : 0);
                return;
            }
            this.playDoorSound(chr.getEmitter(), "Unlock");
            this.playDoorSound(chr.getEmitter(), "Open");
            this.locked = false;
            this.setLockedByKey(false);
        }
        boolean bl = bUnlock = chr instanceof IsoPlayer && !chr.getCurrentSquare().isOutside();
        if ("Tutorial".equals(Core.getInstance().getGameMode()) && this.isLockedByKey()) {
            bUnlock = false;
        }
        if (chr instanceof IsoPlayer && this.getSprite().getProperties().has(IsoPropertyType.GARAGE_DOOR)) {
            boolean bInteriorSide = this.getSprite().getProperties().has("InteriorSide");
            if (bInteriorSide) {
                bUnlock = this.north ? chr.getY() >= this.getY() : chr.getX() >= this.getX();
            } else {
                boolean bl2 = this.north ? chr.getY() < this.getY() : (bUnlock = chr.getX() < this.getX());
            }
        }
        if (this.locked && !bUnlock && !this.open) {
            this.playDoorSound(chr.getEmitter(), "Locked");
            this.setRenderEffect(RenderEffectType.Hit_Door, true);
            return;
        }
        if (this.getSprite().getProperties().has(IsoPropertyType.DOUBLE_DOOR)) {
            if (IsoDoor.isDoubleDoorObstructed(this)) {
                if (chr != null) {
                    this.playDoorSound(chr.getEmitter(), "Blocked");
                    chr.setHaloNote(Translator.getText("IGUI_PlayerText_DoorBlocked"), 255, 255, 255, 256.0f);
                }
                return;
            }
            wasOpen = this.open;
            IsoDoor.toggleDoubleDoor(this, true);
            if (wasOpen != this.open) {
                this.playDoorSound(chr.getEmitter(), this.open ? "Open" : "Close");
                if (player != null && player.isLocalPlayer()) {
                    player.triggerMusicIntensityEvent(this.open ? "DoorOpen" : "DoorClose");
                }
            }
            return;
        }
        if (this.getSprite().getProperties().has(IsoPropertyType.GARAGE_DOOR)) {
            if (IsoDoor.isGarageDoorObstructed(this)) {
                if (chr != null) {
                    this.playDoorSound(chr.getEmitter(), "Blocked");
                    chr.setHaloNote(Translator.getText("IGUI_PlayerText_DoorBlocked"), 255, 255, 255, 256.0f);
                }
                return;
            }
            wasOpen = this.open;
            IsoDoor.toggleGarageDoor(this, true);
            if (wasOpen != this.open) {
                this.playDoorSound(chr.getEmitter(), this.open ? "Open" : "Close");
            }
            if (player != null && player.isLocalPlayer()) {
                player.triggerMusicIntensityEvent(this.open ? "DoorOpen" : "DoorClose");
            }
            return;
        }
        if (this.isObstructed()) {
            if (chr != null) {
                this.playDoorSound(chr.getEmitter(), "Blocked");
                chr.setHaloNote(Translator.getText("IGUI_PlayerText_DoorBlocked"), 255, 255, 255, 256.0f);
            }
            return;
        }
        this.locked = false;
        this.setLockedByKey(false);
        if (chr instanceof IsoPlayer) {
            for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
                LosUtil.cachecleared[pn] = true;
            }
            IsoGridSquare.setRecalcLightTime(-1.0f);
        }
        this.open = !this.open;
        WeatherFxMask.forceMaskUpdateAll();
        this.sprite = this.closedSprite;
        if (this.open) {
            if (chr != null) {
                this.playDoorSound(chr.getEmitter(), "Open");
            }
            this.sprite = this.openSprite;
        } else if (chr != null) {
            this.playDoorSound(chr.getEmitter(), "Close");
        }
        this.square.RecalcAllWithNeighbours(true);
        this.sync(this.open ? 1 : 0);
        PolygonalMap2.instance.squareChanged(this.square);
        LuaEventManager.triggerEvent("OnContainerUpdate");
        this.invalidateRenderChunkLevel(256L);
        if (player != null && player.isLocalPlayer()) {
            player.triggerMusicIntensityEvent(this.open ? "DoorOpen" : "DoorClose");
        }
        this.PlayAnimation();
    }

    private void setWasTryingToggleLockedDoor(boolean b) {
        this.forEachDoorObject(object -> {
            object.wasTryingToggleLockedDoor = b;
        });
    }

    private void setWasTryingToggleBarricadedDoor(boolean b) {
        this.forEachDoorObject(object -> {
            object.wasTryingToggleBarricadedDoor = b;
        });
    }

    private void PlayAnimation() {
        SpriteModel spriteModel1 = this.getSpriteModel();
        if (spriteModel1 != null && spriteModel1.animationName != null) {
            IsoObjectAnimations.getInstance().addObject(this, spriteModel1, this.open ? "Open" : "Close");
        }
        this.setAnimating(true);
    }

    private void TriggerLockedDoor(IsoGameCharacter chr) {
        this.playDoorSound(chr.getEmitter(), "Locked");
        this.setRenderEffect(RenderEffectType.Hit_Door, true);
    }

    private void TriggerBarricadedDoor(IsoGameCharacter chr) {
        IsoPlayer player;
        this.playDoorSound(chr.getEmitter(), "Blocked");
        if (chr instanceof IsoPlayer && (player = (IsoPlayer)chr).isLocalPlayer()) {
            chr.setHaloNote(Translator.getText("IGUI_PlayerText_DoorBarricaded"), 255, 255, 255, 256.0f);
        }
        this.setRenderEffect(RenderEffectType.Hit_Door, true);
    }

    @Override
    public void syncIsoObjectSend(ByteBufferWriter b) {
        b.putInt(this.square.getX());
        b.putInt(this.square.getY());
        b.putInt(this.square.getZ());
        b.putByte(this.square.getObjects().indexOf(this));
        b.putBoolean(true);
        b.putBoolean(false);
        b.putBoolean(this.open);
        b.putBoolean(this.locked);
        b.putBoolean(this.lockedByKey);
        b.putBoolean(this.wasTryingToggleLockedDoor);
        b.putBoolean(this.wasTryingToggleBarricadedDoor);
        b.putInt(this.health);
        b.putShort(this.lastPlayerOnlineId);
        if (b.putBoolean(this.getSprite().getProperties().has(IsoPropertyType.GARAGE_DOOR))) {
            ArrayList<NetObject> doorParts = new ArrayList<NetObject>();
            doorParts.add(new NetObject().setObject(this));
            IsoObject prev = IsoDoor.getGarageDoorPrev(this);
            while (prev != null) {
                doorParts.add(new NetObject().setObject(prev));
                prev = IsoDoor.getGarageDoorPrev(prev);
            }
            IsoObject next = IsoDoor.getGarageDoorNext(this);
            while (next != null) {
                doorParts.add(new NetObject().setObject(next));
                next = IsoDoor.getGarageDoorNext(next);
            }
            b.putByte(doorParts.size());
            for (NetObject doorPart : doorParts) {
                doorPart.write(b);
            }
        }
    }

    @Override
    public void syncIsoObject(boolean bRemote, byte val, UdpConnection source2, ByteBufferReader bb) {
        if (!GameClient.client && !GameServer.server) {
            return;
        }
        if (this.square == null) {
            System.out.println("ERROR: " + this.getClass().getSimpleName() + " square is null");
            return;
        }
        if (this.getObjectIndex() == -1) {
            System.out.println("ERROR: " + this.getClass().getSimpleName() + " not found on square " + this.square.getX() + "," + this.square.getY() + "," + this.square.getZ());
            return;
        }
        if (!bRemote) {
            if (GameClient.client) {
                this.lastPlayerOnlineId = IsoPlayer.getInstance().getOnlineID();
                ByteBufferWriter b = GameClient.connection.startPacket();
                PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                this.syncIsoObjectSend(b);
                PacketTypes.PacketType.SyncIsoObject.send(GameClient.connection);
            } else {
                for (UdpConnection connection : GameServer.udpEngine.connections) {
                    if (!connection.isFullyConnected() || !connection.isRelevantTo(this.getX(), this.getY())) continue;
                    ByteBufferWriter b = connection.startPacket();
                    PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                    this.syncIsoObjectSend(b);
                    PacketTypes.PacketType.SyncIsoObject.send(connection);
                }
            }
            return;
        }
        if (bb == null) {
            DebugLog.Multiplayer.error("ERROR: " + this.getClass().getSimpleName() + " ByteBuffer is null");
            return;
        }
        boolean bOpen = bb.getBoolean();
        boolean bLocked = bb.getBoolean();
        boolean bLockedByKey = bb.getBoolean();
        boolean bWasTryingToggleLockedDoor = bb.getBoolean();
        boolean bWasTryingToggleBarricadedDoor = bb.getBoolean();
        int health = bb.getInt();
        this.lastPlayerOnlineId = bb.getShort();
        boolean bParts = bb.getBoolean();
        ArrayList<NetObject> doorParts = new ArrayList<NetObject>();
        if (bParts) {
            byte size = bb.getByte();
            for (byte i = 0; i < size; i = (byte)(i + 1)) {
                NetObject doorPart = new NetObject();
                doorPart.parse(bb, null);
                doorParts.add(doorPart);
            }
        }
        if (bRemote) {
            boolean wasLocked = this.locked && !bLocked;
            this.locked = bLocked;
            this.lockedByKey = bLockedByKey;
            this.health = health;
            IsoGameCharacter player = null;
            if (GameClient.client && this.lastPlayerOnlineId != -1 && (player = GameClient.IDToPlayerMap.get(this.lastPlayerOnlineId)) != null) {
                ((IsoPlayer)player).networkAi.setNoCollision(1000L);
            }
            if (wasLocked && player != null) {
                this.playDoorSound(player.getEmitter(), "Unlock");
            }
            if (IsoDoor.getDoubleDoorIndex(this) != -1) {
                if (bOpen != this.open) {
                    IsoDoor.toggleDoubleDoor(this, false);
                    if (player != null) {
                        this.playDoorSound(player.getEmitter(), this.open ? "Open" : "Close");
                        if (((IsoPlayer)player).isLocalPlayer()) {
                            ((IsoPlayer)player).triggerMusicIntensityEvent(this.open ? "DoorOpen" : "DoorClose");
                        }
                    }
                }
            } else if (bOpen != this.open) {
                this.open = bOpen;
                IsoSprite isoSprite = this.sprite = this.open ? this.openSprite : this.closedSprite;
                if (player != null) {
                    this.playDoorSound(player.getEmitter(), this.open ? "Open" : "Close");
                    if (((IsoPlayer)player).isLocalPlayer()) {
                        ((IsoPlayer)player).triggerMusicIntensityEvent(this.open ? "DoorOpen" : "DoorClose");
                    }
                }
                this.PlayAnimation();
            }
            for (NetObject doorPart : doorParts) {
                IsoObject obj = doorPart.getObject();
                if (obj != null && obj.getSquare() != null) {
                    IsoThumpable isoThumpable;
                    IsoThumpable thumpable;
                    IsoDoor isoDoor;
                    IsoDoor door = obj instanceof IsoDoor ? (isoDoor = (IsoDoor)obj) : null;
                    IsoThumpable isoThumpable2 = thumpable = obj instanceof IsoThumpable ? (isoThumpable = (IsoThumpable)obj) : null;
                    if (door != null) {
                        door.open = this.open;
                        door.setLockedByKey(bLockedByKey);
                        door.changeSprite(door);
                    }
                    if (thumpable != null) {
                        thumpable.open = this.open;
                        thumpable.setLockedByKey(bLockedByKey);
                        thumpable.changeSprite(thumpable);
                    }
                    obj.getSquare().RecalcAllWithNeighbours(true);
                    obj.invalidateRenderChunkLevel(256L);
                    PolygonalMap2.instance.squareChanged(obj.getSquare());
                    continue;
                }
                DebugLog.General.error("expected IsoDoor index is invalid " + doorPart.getDescription());
                return;
            }
            if (bWasTryingToggleLockedDoor) {
                if (player != null) {
                    this.TriggerLockedDoor(player);
                }
            } else if (bWasTryingToggleBarricadedDoor && player != null) {
                this.TriggerBarricadedDoor(player);
            }
            if (GameServer.server) {
                for (UdpConnection connection : GameServer.udpEngine.connections) {
                    if (!connection.isFullyConnected() || !connection.isRelevantTo(this.getX(), this.getY()) || source2 != null && connection.getConnectedGUID() == source2.getConnectedGUID()) continue;
                    ByteBufferWriter b = connection.startPacket();
                    PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                    this.syncIsoObjectSend(b);
                    PacketTypes.PacketType.SyncIsoObject.send(connection);
                }
            }
        }
        this.square.InvalidateSpecialObjectPaths();
        this.square.RecalcProperties();
        this.square.RecalcAllWithNeighbours(true);
        for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
            LosUtil.cachecleared[pn] = true;
        }
        IsoGridSquare.setRecalcLightTime(-1.0f);
        GameTime.instance.lightSourceUpdate = 100.0f;
        LuaEventManager.triggerEvent("OnContainerUpdate");
        WeatherFxMask.forceMaskUpdateAll();
        this.flagForHotSave();
    }

    public void ToggleDoor(IsoGameCharacter chr) {
        this.ToggleDoorActual(chr);
    }

    public void ToggleDoorSilent() {
        if (this.isBarricaded()) {
            return;
        }
        this.square.InvalidateSpecialObjectPaths();
        for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
            LosUtil.cachecleared[pn] = true;
        }
        IsoGridSquare.setRecalcLightTime(-1.0f);
        this.open = !this.open;
        this.sprite = this.closedSprite;
        if (this.open) {
            this.sprite = this.openSprite;
        }
    }

    void Damage(int amount) {
        this.DirtySlice();
        this.health -= amount;
        if (GameServer.server) {
            this.sync();
        }
    }

    @Override
    public IsoBarricade getBarricadeOnSameSquare() {
        return IsoBarricade.GetBarricadeOnSquare(this.square, this.north ? IsoDirections.N : IsoDirections.W);
    }

    @Override
    public IsoBarricade getBarricadeOnOppositeSquare() {
        return IsoBarricade.GetBarricadeOnSquare(this.getOppositeSquare(), this.north ? IsoDirections.S : IsoDirections.E);
    }

    @Override
    public boolean isBarricaded() {
        IsoBarricade barricade = this.getBarricadeOnSameSquare();
        if (barricade == null) {
            barricade = this.getBarricadeOnOppositeSquare();
        }
        return barricade != null;
    }

    @Override
    public boolean isBarricadeAllowed() {
        return this.getSprite() != null && !this.getSprite().getProperties().has(IsoPropertyType.DOUBLE_DOOR) && !this.getSprite().getProperties().has(IsoPropertyType.GARAGE_DOOR);
    }

    @Override
    public IsoBarricade getBarricadeForCharacter(IsoGameCharacter chr) {
        return IsoBarricade.GetBarricadeForCharacter(this, chr);
    }

    @Override
    public IsoBarricade getBarricadeOppositeCharacter(IsoGameCharacter chr) {
        return IsoBarricade.GetBarricadeOppositeCharacter(this, chr);
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void setLocked(boolean bLocked) {
        this.locked = bLocked;
    }

    @Override
    public boolean getNorth() {
        return this.north;
    }

    @Override
    public Vector2 getFacingPosition(Vector2 pos) {
        if (this.square == null) {
            return pos.set(0.0f, 0.0f);
        }
        if (this.north) {
            return pos.set(this.getX() + 0.5f, this.getY());
        }
        return pos.set(this.getX(), this.getY() + 0.5f);
    }

    @Override
    public Vector2 getFacingPositionAlt(Vector2 pos) {
        if (this.square == null) {
            return pos.set(0.0f, 0.0f);
        }
        switch (this.getSpriteEdge(false)) {
            case N: {
                return pos.set(this.getX() + 0.5f, this.getY());
            }
            case S: {
                return pos.set(this.getX() + 0.5f, this.getY() + 1.0f);
            }
            case W: {
                return pos.set(this.getX(), this.getY() + 0.5f);
            }
            case E: {
                return pos.set(this.getX() + 1.0f, this.getY() + 0.5f);
            }
        }
        throw new IllegalStateException();
    }

    public void setIsLocked(boolean lock) {
        this.locked = lock;
    }

    public IsoSprite getOpenSprite() {
        return this.openSprite;
    }

    public void setOpenSprite(IsoSprite sprite) {
        this.openSprite = sprite;
    }

    @Override
    public int getKeyId() {
        return this.keyId;
    }

    public void syncDoorKey() {
        ByteBufferWriter b = GameClient.connection.startPacket();
        PacketTypes.PacketType.SyncDoorKey.doPacket(b);
        b.putInt(this.square.getX());
        b.putInt(this.square.getY());
        b.putInt(this.square.getZ());
        int i = this.square.getObjects().indexOf(this);
        if (i == -1) {
            System.out.println("ERROR: Door not found on square " + this.square.getX() + ", " + this.square.getY() + ", " + this.square.getZ());
            GameClient.connection.cancelPacket();
            return;
        }
        b.putByte(i);
        b.putInt(this.getKeyId());
        PacketTypes.PacketType.SyncDoorKey.send(GameClient.connection);
    }

    @Override
    public void setKeyId(int keyId) {
        if (this.keyId != keyId && GameClient.client) {
            this.keyId = keyId;
            this.syncDoorKey();
        } else {
            this.keyId = keyId;
        }
    }

    @Override
    public boolean isLockedByKey() {
        return this.lockedByKey;
    }

    @Override
    public void setLockedByKey(boolean lockedByKey) {
        boolean changed = lockedByKey != this.lockedByKey;
        this.lockedByKey = lockedByKey;
        this.locked = lockedByKey;
        if (changed) {
            if (lockedByKey) {
                this.sync(3);
            } else {
                this.sync(4);
            }
        }
    }

    public boolean haveKey() {
        return this.haveKey;
    }

    public void setHaveKey(boolean haveKey) {
        this.haveKey = haveKey;
        if (GameServer.server) {
            return;
        }
        if (haveKey) {
            this.sync(-1);
        } else {
            this.sync(-2);
        }
    }

    @Override
    public IsoGridSquare getOppositeSquare() {
        if (this.getNorth()) {
            return this.getCell().getGridSquare(this.getX(), this.getY() - 1.0f, this.getZ());
        }
        return this.getCell().getGridSquare(this.getX() - 1.0f, this.getY(), this.getZ());
    }

    public boolean isAdjacentToSquare(IsoGridSquare square2) {
        IsoGridSquare square1 = this.getSquare();
        if (square1 == null || square2 == null) {
            return false;
        }
        boolean bClosed = !this.IsOpen();
        IsoGridSquare nw = square1.getAdjacentSquare(IsoDirections.NW);
        IsoGridSquare n = square1.getAdjacentSquare(IsoDirections.N);
        IsoGridSquare ne = square1.getAdjacentSquare(IsoDirections.NE);
        IsoGridSquare w = square1.getAdjacentSquare(IsoDirections.W);
        IsoGridSquare e = square1.getAdjacentSquare(IsoDirections.E);
        IsoGridSquare sw = square1.getAdjacentSquare(IsoDirections.SW);
        IsoGridSquare s = square1.getAdjacentSquare(IsoDirections.S);
        IsoGridSquare se = square1.getAdjacentSquare(IsoDirections.SE);
        switch (this.getSpriteEdge(false)) {
            case N: {
                if (square2 == nw) {
                    if (nw.isWallTo(n) || nw.isWindowTo(n) || nw.hasDoorOnEdge(IsoDirections.E, false) || n.hasDoorOnEdge(IsoDirections.W, false)) {
                        return false;
                    }
                    if (n.hasDoorOnEdge(IsoDirections.S, false)) {
                        return false;
                    }
                    return !this.IsOpen() || !square1.hasClosedDoorOnEdge(IsoDirections.N);
                }
                if (square2 == n) {
                    if (n.hasDoorOnEdge(IsoDirections.S, false)) {
                        return false;
                    }
                    return !this.IsOpen() || !square1.hasClosedDoorOnEdge(IsoDirections.N);
                }
                if (square2 == ne) {
                    if (ne.isWallTo(n) || ne.isWindowTo(n) || ne.hasDoorOnEdge(IsoDirections.W, false) || n.hasDoorOnEdge(IsoDirections.E, false)) {
                        return false;
                    }
                    if (n.hasDoorOnEdge(IsoDirections.S, false)) {
                        return false;
                    }
                    return !this.IsOpen() || !square1.hasClosedDoorOnEdge(IsoDirections.N);
                }
                if (square2 == w) {
                    if (w.isWallTo(square1) || w.isWindowTo(square1) || w.hasDoorOnEdge(IsoDirections.E, false) || square1.hasDoorOnEdge(IsoDirections.W, false)) {
                        return false;
                    }
                    return !bClosed || !square1.hasOpenDoorOnEdge(IsoDirections.N);
                }
                if (square2 == square1) {
                    return !bClosed || !square1.hasOpenDoorOnEdge(IsoDirections.N);
                }
                if (square2 != e) break;
                if (e.isWallTo(square1) || e.isWindowTo(square1) || e.hasDoorOnEdge(IsoDirections.W, false) || square1.hasDoorOnEdge(IsoDirections.E, false)) {
                    return false;
                }
                return !bClosed || !square1.hasOpenDoorOnEdge(IsoDirections.N);
            }
            case S: {
                if (square2 == w) {
                    if (w.isWallTo(square1) || w.isWindowTo(square1) || w.hasDoorOnEdge(IsoDirections.E, false) || square1.hasDoorOnEdge(IsoDirections.W, false)) {
                        return false;
                    }
                    return !bClosed || !square1.hasOpenDoorOnEdge(IsoDirections.S);
                }
                if (square2 == square1) {
                    return !bClosed || !square1.hasOpenDoorOnEdge(IsoDirections.S);
                }
                if (square2 == e) {
                    if (e.isWallTo(square1) || e.isWindowTo(square1) || e.hasDoorOnEdge(IsoDirections.W, false) || square1.hasDoorOnEdge(IsoDirections.E, false)) {
                        return false;
                    }
                    return !bClosed || !square1.hasOpenDoorOnEdge(IsoDirections.S);
                }
                if (square2 == sw) {
                    if (sw.isWallTo(s) || sw.isWindowTo(s) || sw.hasDoorOnEdge(IsoDirections.E, false) || s.hasDoorOnEdge(IsoDirections.W, false)) {
                        return false;
                    }
                    return !s.hasDoorOnEdge(IsoDirections.N, false);
                }
                if (square2 == s) {
                    return !s.hasDoorOnEdge(IsoDirections.N, false);
                }
                if (square2 != se) break;
                if (se.isWallTo(s) || se.isWindowTo(s) || se.hasDoorOnEdge(IsoDirections.W, false) || s.hasDoorOnEdge(IsoDirections.E, false)) {
                    return false;
                }
                return !s.hasDoorOnEdge(IsoDirections.N, false);
            }
            case W: {
                if (square2 == nw) {
                    if (nw.isWallTo(w) || nw.isWindowTo(w) || nw.hasDoorOnEdge(IsoDirections.S, false) || w.hasDoorOnEdge(IsoDirections.N, false)) {
                        return false;
                    }
                    if (bClosed && w.hasDoorOnEdge(IsoDirections.E, false)) {
                        return false;
                    }
                    return !this.IsOpen() || !square1.hasClosedDoorOnEdge(IsoDirections.W);
                }
                if (square2 == w) {
                    if (bClosed && w.hasDoorOnEdge(IsoDirections.E, false)) {
                        return false;
                    }
                    return !this.IsOpen() || !square1.hasClosedDoorOnEdge(IsoDirections.W);
                }
                if (square2 == sw) {
                    if (sw.isWallTo(w) || sw.isWindowTo(w) || sw.hasDoorOnEdge(IsoDirections.N, false) || w.hasDoorOnEdge(IsoDirections.S, false)) {
                        return false;
                    }
                    if (bClosed && w.hasDoorOnEdge(IsoDirections.E, false)) {
                        return false;
                    }
                    return !this.IsOpen() || !square1.hasClosedDoorOnEdge(IsoDirections.W);
                }
                if (square2 == n) {
                    if (n.isWallTo(square1) || n.isWindowTo(square1) || n.hasDoorOnEdge(IsoDirections.S, false) || square1.hasDoorOnEdge(IsoDirections.N, false)) {
                        return false;
                    }
                    return !bClosed || !square1.hasOpenDoorOnEdge(IsoDirections.W);
                }
                if (square2 == square1) {
                    return !bClosed || !square1.hasOpenDoorOnEdge(IsoDirections.W);
                }
                if (square2 != s) break;
                if (s.isWallTo(square1) || s.isWindowTo(square1) || s.hasDoorOnEdge(IsoDirections.N, false) || square1.hasDoorOnEdge(IsoDirections.S, false)) {
                    return false;
                }
                return !bClosed || !square1.hasOpenDoorOnEdge(IsoDirections.W);
            }
            case E: {
                if (square2 == n) {
                    if (n.isWallTo(square1) || n.isWindowTo(square1) || n.hasDoorOnEdge(IsoDirections.S, false) || square1.hasDoorOnEdge(IsoDirections.N, false)) {
                        return false;
                    }
                    return !bClosed || !square1.hasOpenDoorOnEdge(IsoDirections.E);
                }
                if (square2 == square1) {
                    return !bClosed || !square1.hasOpenDoorOnEdge(IsoDirections.E);
                }
                if (square2 == s) {
                    if (s.isWallTo(square1) || s.isWindowTo(square1) || s.hasDoorOnEdge(IsoDirections.N, false) || square1.hasDoorOnEdge(IsoDirections.S, false)) {
                        return false;
                    }
                    return !bClosed || !square1.hasOpenDoorOnEdge(IsoDirections.E);
                }
                if (square2 == ne) {
                    if (ne.isWallTo(e) || ne.isWindowTo(e) || ne.hasDoorOnEdge(IsoDirections.S, false) || w.hasDoorOnEdge(IsoDirections.N, false)) {
                        return false;
                    }
                    return !e.hasDoorOnEdge(IsoDirections.W, false);
                }
                if (square2 == e) {
                    return !e.hasDoorOnEdge(IsoDirections.W, false);
                }
                if (square2 != se) break;
                if (se.isWallTo(e) || se.isWindowTo(e) || se.hasDoorOnEdge(IsoDirections.N, false) || e.hasDoorOnEdge(IsoDirections.S, false)) {
                    return false;
                }
                return !e.hasDoorOnEdge(IsoDirections.E, false);
            }
            default: {
                return false;
            }
        }
        return false;
    }

    public int checkKeyId() {
        BuildingDef sqOtherSideBuilding;
        if (this.getKeyId() != -1) {
            return this.getKeyId();
        }
        IsoGridSquare sq = this.getSquare();
        IsoGridSquare sqOtherSide = this.getOppositeSquare();
        if (sq == null || sqOtherSide == null) {
            return -1;
        }
        BuildingDef sqBuilding = sq.getBuilding() == null ? null : sq.getBuilding().getDef();
        BuildingDef buildingDef = sqOtherSideBuilding = sqOtherSide.getBuilding() == null ? null : sqOtherSide.getBuilding().getDef();
        if (sqBuilding == null && sqOtherSideBuilding != null) {
            this.setKeyId(sqOtherSideBuilding.getKeyId());
        } else if (sqBuilding != null && sqOtherSideBuilding == null) {
            this.setKeyId(sqBuilding.getKeyId());
        } else if (this.getProperties().has("forceLocked") && sqBuilding != null) {
            this.setKeyId(sqBuilding.getKeyId());
        }
        if (this.locked && !this.lockedByKey && this.getKeyId() != -1) {
            this.lockedByKey = true;
        }
        return this.getKeyId();
    }

    @Override
    public void setHealth(int health) {
        this.health = health;
    }

    private void initCurtainSprites() {
        if (this.curtainN != null) {
            return;
        }
        this.curtainW = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_16").newInstance();
        this.curtainW.setScale(0.8f, 0.8f);
        this.curtainWopen = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_20").newInstance();
        this.curtainWopen.setScale(0.8f, 0.8f);
        this.curtainE = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_17").newInstance();
        this.curtainE.setScale(0.8f, 0.8f);
        this.curtainEopen = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_21").newInstance();
        this.curtainEopen.setScale(0.8f, 0.8f);
        this.curtainN = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_18").newInstance();
        this.curtainN.setScale(0.8f, 0.8f);
        this.curtainNopen = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_22").newInstance();
        this.curtainNopen.setScale(0.8f, 0.8f);
        this.curtainS = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_19").newInstance();
        this.curtainS.setScale(0.8f, 0.8f);
        this.curtainSopen = IsoSpriteManager.instance.getSprite("fixtures_windows_curtains_01_23").newInstance();
        this.curtainSopen.setScale(0.8f, 0.8f);
    }

    @Override
    public boolean canAddCurtain() {
        PropertyContainer props = this.getProperties();
        if (props == null) {
            return false;
        }
        if (props.has(IsoPropertyType.GARAGE_DOOR)) {
            return false;
        }
        if ("SlidingGlassDoor".equalsIgnoreCase(this.getSoundPrefix())) {
            return false;
        }
        return props.has("doorTrans");
    }

    @Override
    public IsoDoor HasCurtains() {
        return this.hasCurtain ? this : null;
    }

    @Override
    public boolean isCurtainOpen() {
        return this.hasCurtain && this.curtainOpen;
    }

    public void setCurtainOpen(boolean open) {
        if (!this.hasCurtain) {
            return;
        }
        this.curtainOpen = open;
        if (!GameServer.server) {
            for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
                LosUtil.cachecleared[pn] = true;
            }
            GameTime.instance.lightSourceUpdate = 100.0f;
            IsoGridSquare.setRecalcLightTime(-1.0f);
            if (this.square != null) {
                this.square.RecalcProperties();
                this.square.RecalcAllWithNeighbours(true);
            }
        }
    }

    public void transmitSetCurtainOpen(boolean open) {
        if (!this.hasCurtain) {
            return;
        }
        if (GameServer.server) {
            this.sendObjectChange(IsoObjectChange.SET_CURTAIN_OPEN, "open", open);
        }
        if (GameClient.client) {
            GameClient.instance.sendClientCommandV(null, "object", "openCloseCurtain", "x", Float.valueOf(this.getX()), "y", Float.valueOf(this.getY()), "z", Float.valueOf(this.getZ()), "index", this.getObjectIndex(), "open", !this.curtainOpen);
        }
    }

    public void toggleCurtain() {
        if (!this.hasCurtain) {
            return;
        }
        if (GameClient.client) {
            this.transmitSetCurtainOpen(!this.isCurtainOpen());
        } else {
            this.setCurtainOpen(!this.isCurtainOpen());
            if (GameServer.server) {
                this.transmitSetCurtainOpen(this.isCurtainOpen());
            }
        }
    }

    public void addSheet(IsoGameCharacter chr) {
        if (this.hasCurtain || chr == null || chr.getCurrentSquare() == null) {
            return;
        }
        IsoGridSquare sqChr = chr.getCurrentSquare();
        IsoGridSquare sqInside = this.getSquare();
        this.addSheet(switch (this.getSpriteEdge(false)) {
            case IsoDirections.N -> this.north == sqChr.getY() >= sqInside.getY();
            case IsoDirections.S -> sqChr.getY() > sqInside.getY();
            case IsoDirections.W -> this.north == sqChr.getX() < sqInside.getX();
            case IsoDirections.E -> sqChr.getX() > sqInside.getX();
            default -> throw new IllegalStateException();
        }, chr);
    }

    public void addSheet(boolean inside, IsoGameCharacter chr) {
        if (this.hasCurtain) {
            return;
        }
        this.hasCurtain = true;
        this.curtainInside = inside;
        this.curtainOpen = true;
        if (!GameClient.client) {
            InventoryItem item = chr.getInventory().FindAndReturn("Sheet");
            chr.getInventory().Remove(item);
            if (GameServer.server) {
                GameServer.sendRemoveItemFromContainer(chr.getInventory(), item);
            }
        }
        if (GameServer.server) {
            this.sendObjectChange(IsoObjectChange.ADD_SHEET, "inside", inside);
        } else if (chr != null) {
            for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
                LosUtil.cachecleared[pn] = true;
            }
            GameTime.instance.lightSourceUpdate = 100.0f;
            IsoGridSquare.setRecalcLightTime(-1.0f);
            if (this.square != null) {
                this.square.RecalcProperties();
            }
        }
    }

    public void removeSheet(IsoGameCharacter chr) {
        if (!this.hasCurtain) {
            return;
        }
        this.hasCurtain = false;
        InventoryItem item = chr.getInventory().AddItem("Base.Sheet");
        if (GameServer.server) {
            GameServer.sendAddItemToContainer(chr.getInventory(), item);
        }
        if (GameServer.server) {
            this.sendObjectChange(IsoObjectChange.REMOVE_SHEET);
        } else if (chr != null) {
            for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
                LosUtil.cachecleared[pn] = true;
            }
            GameTime.instance.lightSourceUpdate = 100.0f;
            IsoGridSquare.setRecalcLightTime(-1.0f);
            if (this.square != null) {
                this.square.RecalcProperties();
            }
        }
    }

    public IsoGridSquare getAddSheetSquare(IsoGameCharacter chr) {
        if (chr == null || chr.getCurrentSquare() == null) {
            return null;
        }
        IsoGridSquare sqChr = chr.getCurrentSquare();
        IsoGridSquare sqInside = this.getSquare();
        switch (this.getSpriteEdge(false)) {
            case N: {
                return sqChr.getY() >= sqInside.getY() ? sqInside : this.getCell().getGridSquare(sqInside.x, sqInside.y - 1, sqInside.z);
            }
            case S: {
                return sqChr.getY() <= sqInside.getY() ? sqInside : this.getCell().getGridSquare(sqInside.x, sqInside.y + 1, sqInside.z);
            }
            case W: {
                return sqChr.getX() >= sqInside.getX() ? sqInside : this.getCell().getGridSquare(sqInside.x - 1, sqInside.y, sqInside.z);
            }
            case E: {
                return sqChr.getX() <= sqInside.getX() ? sqInside : this.getCell().getGridSquare(sqInside.x + 1, sqInside.y, sqInside.z);
            }
        }
        throw new IllegalStateException();
    }

    public IsoGridSquare getSheetSquare() {
        if (!this.hasCurtain) {
            return null;
        }
        switch (this.getSpriteEdge(false)) {
            case N: {
                if (this.open) {
                    return this.curtainInside ? this.getCell().getGridSquare(this.getX(), this.getY() - 1.0f, this.getZ()) : this.getSquare();
                }
                return this.curtainInside ? this.getSquare() : this.getCell().getGridSquare(this.getX(), this.getY() - 1.0f, this.getZ());
            }
            case S: {
                return this.curtainInside ? this.getCell().getGridSquare(this.getX(), this.getY() + 1.0f, this.getZ()) : this.getSquare();
            }
            case W: {
                if (this.open) {
                    return this.curtainInside ? this.getCell().getGridSquare(this.getX() - 1.0f, this.getY(), this.getZ()) : this.getSquare();
                }
                return this.curtainInside ? this.getSquare() : this.getCell().getGridSquare(this.getX() - 1.0f, this.getY(), this.getZ());
            }
            case E: {
                return this.curtainInside ? this.getCell().getGridSquare(this.getX() + 1.0f, this.getY(), this.getZ()) : this.getSquare();
            }
        }
        throw new IllegalStateException();
    }

    @Override
    public int getHealth() {
        return this.health;
    }

    @Override
    public int getMaxHealth() {
        return this.maxHealth;
    }

    public boolean isFacingSheet(IsoGameCharacter chr) {
        if (!this.hasCurtain || chr == null || chr.getCurrentSquare() != this.getSheetSquare()) {
            return false;
        }
        IsoDirections dir = this.curtainInside ? (this.open ? (this.north ? IsoDirections.E : IsoDirections.S) : (this.north ? IsoDirections.N : IsoDirections.W)) : (this.open ? (this.north ? IsoDirections.W : IsoDirections.N) : (this.north ? IsoDirections.S : IsoDirections.E));
        IsoDirections edge = this.getSpriteEdge(false);
        if (edge == IsoDirections.E) {
            IsoDirections isoDirections = dir = this.curtainInside ? IsoDirections.W : IsoDirections.E;
        }
        if (edge == IsoDirections.S) {
            dir = this.curtainInside ? IsoDirections.N : IsoDirections.S;
        }
        return chr.getDir() == dir || chr.getDir() == dir.RotLeft() || chr.getDir() == dir.RotRight();
    }

    @Override
    public void saveChange(IsoObjectChange change, KahluaTable tbl, ByteBufferWriter bb) {
        if (change == IsoObjectChange.ADD_SHEET) {
            Object object;
            if (tbl != null && (object = tbl.rawget("inside")) instanceof Boolean) {
                Boolean isInside = (Boolean)object;
                bb.putBoolean(isInside);
            }
        } else if (change != IsoObjectChange.REMOVE_SHEET) {
            if (change == IsoObjectChange.SET_CURTAIN_OPEN) {
                Object object;
                if (tbl != null && (object = tbl.rawget("open")) instanceof Boolean) {
                    Boolean isOpen = (Boolean)object;
                    bb.putBoolean(isOpen);
                }
            } else {
                super.saveChange(change, tbl, bb);
            }
        }
    }

    @Override
    public void loadChange(IsoObjectChange change, ByteBufferReader bb) {
        if (change == IsoObjectChange.ADD_SHEET) {
            this.addSheet(bb.getBoolean(), null);
        } else if (change == IsoObjectChange.REMOVE_SHEET) {
            this.removeSheet(null);
        } else if (change == IsoObjectChange.SET_CURTAIN_OPEN) {
            this.setCurtainOpen(bb.getBoolean());
        } else {
            super.loadChange(change, bb);
        }
    }

    public void addRandomBarricades() {
        boolean addOpposite;
        IsoBarricade barricade;
        IsoGridSquare outside;
        IsoGridSquare isoGridSquare = outside = this.square.getRoom() == null ? this.square : this.getOppositeSquare();
        if (outside != null && outside.getRoom() == null && (barricade = IsoBarricade.AddBarricadeToObject((BarricadeAble)this, addOpposite = outside != this.square)) != null) {
            int numPlanks = Rand.Next(1, 4);
            for (int b = 0; b < numPlanks; ++b) {
                barricade.addPlank(null, null);
            }
        }
    }

    public boolean isObstructed() {
        return IsoDoor.isDoorObstructed(this);
    }

    public static boolean isDoorObstructed(IsoObject object) {
        IsoThumpable isoThumpable;
        IsoThumpable thump;
        IsoDoor isoDoor;
        IsoDoor door = object instanceof IsoDoor ? (isoDoor = (IsoDoor)object) : null;
        IsoThumpable isoThumpable2 = thump = object instanceof IsoThumpable ? (isoThumpable = (IsoThumpable)object) : null;
        if (door == null && thump == null) {
            return false;
        }
        IsoGridSquare sq = object.getSquare();
        if (sq == null) {
            return false;
        }
        if (sq.isSolid() || sq.isSolidTrans() || sq.has(IsoObjectType.tree)) {
            return true;
        }
        int chunkMinX = (sq.x - 1) / 8;
        int chunkMinY = (sq.y - 1) / 8;
        int chunkMaxX = (int)Math.ceil(((float)sq.x + 1.0f) / 8.0f);
        int chunkMaxY = (int)Math.ceil(((float)sq.y + 1.0f) / 8.0f);
        for (int cy = chunkMinY; cy <= chunkMaxY; ++cy) {
            for (int cx = chunkMinX; cx <= chunkMaxX; ++cx) {
                IsoChunk chunk;
                IsoChunk isoChunk = chunk = GameServer.server ? ServerMap.instance.getChunk(cx, cy) : IsoWorld.instance.currentCell.getChunk(cx, cy);
                if (chunk == null) continue;
                for (int i = 0; i < chunk.vehicles.size(); ++i) {
                    BaseVehicle vehicle = chunk.vehicles.get(i);
                    if (!vehicle.isIntersectingSquareWithShadow(sq.x, sq.y, sq.z)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    public static void toggleDoubleDoor(IsoObject oneOfFour, boolean doSync) {
        SpriteModel spriteModel1;
        IsoThumpable isoThumpable;
        IsoDoor isoDoor;
        int oneOfFourIndex = IsoDoor.getDoubleDoorIndex(oneOfFour);
        if (oneOfFourIndex == -1) {
            return;
        }
        IsoDoor door = oneOfFour instanceof IsoDoor ? (isoDoor = (IsoDoor)oneOfFour) : null;
        IsoThumpable thumpable = oneOfFour instanceof IsoThumpable ? (isoThumpable = (IsoThumpable)oneOfFour) : null;
        boolean north = door == null ? thumpable.north : door.north;
        boolean open = door == null ? thumpable.open : door.open;
        IsoObject object1 = IsoDoor.getDoubleDoorObject(oneOfFour, 1);
        IsoObject object2 = IsoDoor.getDoubleDoorObject(oneOfFour, 2);
        IsoObject object3 = IsoDoor.getDoubleDoorObject(oneOfFour, 3);
        IsoObject object4 = IsoDoor.getDoubleDoorObject(oneOfFour, 4);
        if (object1 != null) {
            IsoDoor.toggleDoubleDoorObject(object1);
        }
        if (object2 != null) {
            IsoDoor.toggleDoubleDoorObject(object2);
        }
        if (object3 != null) {
            IsoDoor.toggleDoubleDoorObject(object3);
        }
        if (object4 != null) {
            IsoDoor.toggleDoubleDoorObject(object4);
        }
        LuaEventManager.triggerEvent("OnContainerUpdate");
        if (doSync) {
            if (object1 != null) {
                object1.sync(open ? 1 : 0);
            } else if (object4 != null) {
                object4.sync(open ? 1 : 0);
            }
        }
        if (object1 != null && (spriteModel1 = object1.getSpriteModel()) != null && spriteModel1.animationName != null) {
            IsoObjectAnimations.getInstance().addObject(object1, spriteModel1, !open ? "Open" : "Close");
            object1.setAnimating(true);
        }
        if (object4 != null && (spriteModel1 = object4.getSpriteModel()) != null && spriteModel1.animationName != null) {
            IsoObjectAnimations.getInstance().addObject(object4, spriteModel1, !open ? "Open" : "Close");
            object4.setAnimating(true);
        }
    }

    private static void toggleDoubleDoorObject(IsoObject oneOfFour) {
        int offset;
        IsoThumpable isoThumpable;
        IsoDoor isoDoor;
        int oneOfFourIndex = IsoDoor.getDoubleDoorIndex(oneOfFour);
        if (oneOfFourIndex == -1) {
            return;
        }
        IsoDoor door = oneOfFour instanceof IsoDoor ? (isoDoor = (IsoDoor)oneOfFour) : null;
        IsoThumpable thumpable = oneOfFour instanceof IsoThumpable ? (isoThumpable = (IsoThumpable)oneOfFour) : null;
        boolean north = door == null ? thumpable.north : door.north;
        boolean open = door == null ? thumpable.open : door.open;
        int keyId = -1;
        if (door != null) {
            door.open = !open;
            door.setLockedByKey(false);
            keyId = door.checkKeyId();
        }
        if (thumpable != null) {
            thumpable.open = !open;
            thumpable.setLockedByKey(false);
            keyId = thumpable.getKeyId();
        }
        IsoSprite sprite = oneOfFour.getSprite();
        int n = offset = north ? DoubleDoorNorthSpriteOffset[oneOfFourIndex - 1] : DoubleDoorWestSpriteOffset[oneOfFourIndex - 1];
        if (open) {
            offset *= -1;
        }
        oneOfFour.sprite = IsoSprite.getSprite(IsoSpriteManager.instance, sprite.getName(), offset);
        oneOfFour.getSquare().RecalcAllWithNeighbours(true);
        if (oneOfFourIndex == 2 || oneOfFourIndex == 3) {
            int[] yOffsetNew;
            int[] xOffsetNew;
            int[] yOffsetCur;
            int[] xOffsetCur;
            IsoGridSquare sq = oneOfFour.getSquare();
            if (north) {
                if (open) {
                    xOffsetCur = DoubleDoorNorthOpenXOffset;
                    yOffsetCur = DoubleDoorNorthOpenYOffset;
                    xOffsetNew = DoubleDoorNorthClosedXOffset;
                    yOffsetNew = DoubleDoorNorthClosedYOffset;
                } else {
                    xOffsetCur = DoubleDoorNorthClosedXOffset;
                    yOffsetCur = DoubleDoorNorthClosedYOffset;
                    xOffsetNew = DoubleDoorNorthOpenXOffset;
                    yOffsetNew = DoubleDoorNorthOpenYOffset;
                }
            } else if (open) {
                xOffsetCur = DoubleDoorWestOpenXOffset;
                yOffsetCur = DoubleDoorWestOpenYOffset;
                xOffsetNew = DoubleDoorWestClosedXOffset;
                yOffsetNew = DoubleDoorWestClosedYOffset;
            } else {
                xOffsetCur = DoubleDoorWestClosedXOffset;
                yOffsetCur = DoubleDoorWestClosedYOffset;
                xOffsetNew = DoubleDoorWestOpenXOffset;
                yOffsetNew = DoubleDoorWestOpenYOffset;
            }
            int firstX = sq.getX() - xOffsetCur[oneOfFourIndex - 1];
            int firstY = sq.getY() - yOffsetCur[oneOfFourIndex - 1];
            int indexX = firstX + xOffsetNew[oneOfFourIndex - 1];
            int indexY = firstY + yOffsetNew[oneOfFourIndex - 1];
            sq.RemoveTileObject(oneOfFour, false);
            PolygonalMap2.instance.squareChanged(sq);
            sq = IsoWorld.instance.currentCell.getGridSquare(indexX, indexY, sq.getZ());
            if (sq == null) {
                return;
            }
            if (thumpable != null) {
                IsoThumpable newDoorB = new IsoThumpable(sq.getCell(), sq, oneOfFour.getSprite().getName(), north, thumpable.getTable());
                newDoorB.setModData(thumpable.getModData());
                newDoorB.setCanBeLockByPadlock(thumpable.canBeLockByPadlock());
                newDoorB.setCanBePlastered(thumpable.canBePlastered());
                newDoorB.setIsHoppable(thumpable.isHoppable());
                newDoorB.setIsDismantable(thumpable.isDismantable());
                newDoorB.setName(thumpable.getName());
                newDoorB.setIsDoor(true);
                newDoorB.setIsThumpable(thumpable.isThumpable());
                newDoorB.setThumpDmg(thumpable.getThumpDmg());
                newDoorB.setThumpSound(thumpable.getThumpSound());
                newDoorB.open = !open;
                newDoorB.keyId = keyId;
                sq.AddSpecialObject(newDoorB);
            } else {
                IsoDoor newDoorB = new IsoDoor(sq.getCell(), sq, oneOfFour.getSprite().getName(), north);
                newDoorB.open = !open;
                newDoorB.keyId = keyId;
                sq.getObjects().add(newDoorB);
                sq.getSpecialObjects().add(newDoorB);
                sq.RecalcProperties();
                sq.invalidateRenderChunkLevel(64L);
            }
            if (!GameClient.client) {
                sq.restackSheetRope();
            }
            PolygonalMap2.instance.squareChanged(sq);
        } else {
            PolygonalMap2.instance.squareChanged(oneOfFour.getSquare());
            oneOfFour.invalidateRenderChunkLevel(256L);
        }
    }

    public static int getDoubleDoorIndex(IsoObject oneOfFour) {
        boolean open;
        IsoThumpable isoThumpable;
        IsoThumpable thump;
        IsoDoor isoDoor;
        if (oneOfFour == null || oneOfFour.getSquare() == null) {
            return -1;
        }
        PropertyContainer props = oneOfFour.getProperties();
        if (props == null || !props.has(IsoPropertyType.DOUBLE_DOOR)) {
            return -1;
        }
        int ddIndex = Integer.parseInt(props.get(IsoPropertyType.DOUBLE_DOOR));
        if (ddIndex < 1 || ddIndex > 8) {
            return -1;
        }
        IsoDoor door = oneOfFour instanceof IsoDoor ? (isoDoor = (IsoDoor)oneOfFour) : null;
        IsoThumpable isoThumpable2 = thump = oneOfFour instanceof IsoThumpable ? (isoThumpable = (IsoThumpable)oneOfFour) : null;
        if (door == null && thump == null) {
            return -1;
        }
        boolean bl = open = door == null ? thump.open : door.open;
        if (open) {
            if (ddIndex >= 5) {
                return ddIndex - 4;
            }
            return -1;
        }
        return ddIndex;
    }

    public static IsoObject getDoubleDoorObject(IsoObject oneOfFour, int index) {
        IsoObject obj;
        int i;
        int[] yOffset;
        int[] xOffset;
        IsoThumpable thumpable;
        IsoDoor isoDoor;
        int oneOfFourIndex = IsoDoor.getDoubleDoorIndex(oneOfFour);
        if (oneOfFourIndex == -1 || index <= 0) {
            return null;
        }
        IsoDoor door = oneOfFour instanceof IsoDoor ? (isoDoor = (IsoDoor)oneOfFour) : null;
        IsoThumpable thump = oneOfFour instanceof IsoThumpable ? (thumpable = (IsoThumpable)oneOfFour) : null;
        boolean north = door == null ? thump.north : door.north;
        boolean open = door == null ? thump.open : door.open;
        IsoGridSquare sq = oneOfFour.getSquare();
        if (north) {
            if (open) {
                xOffset = DoubleDoorNorthOpenXOffset;
                yOffset = DoubleDoorNorthOpenYOffset;
            } else {
                xOffset = DoubleDoorNorthClosedXOffset;
                yOffset = DoubleDoorNorthClosedYOffset;
            }
        } else if (open) {
            xOffset = DoubleDoorWestOpenXOffset;
            yOffset = DoubleDoorWestOpenYOffset;
        } else {
            xOffset = DoubleDoorWestClosedXOffset;
            yOffset = DoubleDoorWestClosedYOffset;
        }
        int firstX = sq.getX() - xOffset[oneOfFourIndex - 1];
        int firstY = sq.getY() - yOffset[oneOfFourIndex - 1];
        int indexX = firstX + xOffset[index - 1];
        int indexY = firstY + yOffset[index - 1];
        sq = IsoWorld.instance.currentCell.getGridSquare(indexX, indexY, sq.getZ());
        if (sq == null) {
            return null;
        }
        ArrayList<IsoObject> specialObjects = sq.getSpecialObjects();
        if (door != null) {
            for (i = 0; i < specialObjects.size(); ++i) {
                obj = specialObjects.get(i);
                if (!(obj instanceof IsoDoor)) continue;
                IsoDoor isoDoor2 = (IsoDoor)obj;
                if (isoDoor2.north != north || IsoDoor.getDoubleDoorIndex(obj) != index) continue;
                return obj;
            }
        }
        if (thump != null) {
            for (i = 0; i < specialObjects.size(); ++i) {
                obj = specialObjects.get(i);
                if (!(obj instanceof IsoThumpable)) continue;
                IsoThumpable isoThumpable = (IsoThumpable)obj;
                if (isoThumpable.north != north || IsoDoor.getDoubleDoorIndex(obj) != index) continue;
                return obj;
            }
        }
        return null;
    }

    public static int getDoubleDoorPartnerIndex(int ddIndex) {
        if (ddIndex >= 5) {
            ddIndex -= 4;
        }
        return switch (ddIndex) {
            case 1 -> 2;
            case 2 -> 1;
            case 3 -> 4;
            case 4 -> 3;
            default -> -1;
        };
    }

    public static boolean isDoubleDoorObstructed(IsoObject oneOfFour) {
        int[] yOffset;
        int[] xOffset;
        IsoThumpable isoThumpable;
        IsoDoor isoDoor;
        int oneOfFourIndex = IsoDoor.getDoubleDoorIndex(oneOfFour);
        if (oneOfFourIndex == -1) {
            return false;
        }
        IsoDoor door = oneOfFour instanceof IsoDoor ? (isoDoor = (IsoDoor)oneOfFour) : null;
        IsoThumpable thump = oneOfFour instanceof IsoThumpable ? (isoThumpable = (IsoThumpable)oneOfFour) : null;
        boolean north = door == null ? thump.north : door.north;
        boolean open = door == null ? thump.open : door.open;
        IsoGridSquare sq = oneOfFour.getSquare();
        if (north) {
            if (open) {
                xOffset = DoubleDoorNorthOpenXOffset;
                yOffset = DoubleDoorNorthOpenYOffset;
            } else {
                xOffset = DoubleDoorNorthClosedXOffset;
                yOffset = DoubleDoorNorthClosedYOffset;
            }
        } else if (open) {
            xOffset = DoubleDoorWestOpenXOffset;
            yOffset = DoubleDoorWestOpenYOffset;
        } else {
            xOffset = DoubleDoorWestClosedXOffset;
            yOffset = DoubleDoorWestClosedYOffset;
        }
        boolean bHasLeft = IsoDoor.getDoubleDoorObject(oneOfFour, 1) != null;
        boolean bHasRight = IsoDoor.getDoubleDoorObject(oneOfFour, 4) != null;
        int firstX = sq.getX() - xOffset[oneOfFourIndex - 1];
        int firstY = sq.getY() - yOffset[oneOfFourIndex - 1];
        int minX = firstX;
        int minY = firstY;
        int maxX = minX;
        int maxY = minY;
        int z = sq.getZ();
        if (north) {
            maxX += 4;
            maxY += 2;
            if (bHasLeft && IsoDoor.hasSomething4x4(firstX, firstY, firstX + 1, firstY + 1, z)) {
                return true;
            }
            if (bHasRight && IsoDoor.hasSomething4x4(firstX + 2, firstY, firstX + 3, firstY + 1, z)) {
                return true;
            }
            if (!bHasLeft) {
                minX += 2;
            }
            if (!bHasRight) {
                maxX -= 2;
            }
        } else {
            maxX += 2;
            maxY = (minY -= 3) + 4;
            if (bHasLeft && IsoDoor.hasSomething4x4(firstX, firstY - 1, firstX + 1, firstY, z)) {
                return true;
            }
            if (bHasRight && IsoDoor.hasSomething4x4(firstX, firstY - 3, firstX + 1, firstY - 2, z)) {
                return true;
            }
            if (!bHasLeft) {
                maxY -= 2;
            }
            if (!bHasRight) {
                minY += 2;
            }
        }
        int chunkMinX = PZMath.fastfloor(((float)minX - 4.0f) / 8.0f);
        int chunkMinY = PZMath.fastfloor(((float)minY - 4.0f) / 8.0f);
        int chunkMaxX = (int)Math.ceil(((float)maxX + 4.0f) / 8.0f);
        int chunkMaxY = (int)Math.ceil(((float)maxY + 4.0f) / 8.0f);
        for (int cy = chunkMinY; cy <= chunkMaxY; ++cy) {
            for (int cx = chunkMinX; cx <= chunkMaxX; ++cx) {
                IsoChunk chunk;
                IsoChunk isoChunk = chunk = GameServer.server ? ServerMap.instance.getChunk(cx, cy) : IsoWorld.instance.currentCell.getChunk(cx, cy);
                if (chunk == null) continue;
                for (int i = 0; i < chunk.vehicles.size(); ++i) {
                    BaseVehicle vehicle = chunk.vehicles.get(i);
                    for (int y = minY; y < maxY; ++y) {
                        for (int x = minX; x < maxX; ++x) {
                            if (!vehicle.isIntersectingSquare(x, y, z)) continue;
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasSolidObjects(IsoGridSquare square1) {
        if (square1 == null) {
            return false;
        }
        return square1.isSolid() || square1.isSolidTrans() || square1.has(IsoObjectType.tree);
    }

    private static boolean isSomethingTo(IsoGridSquare square1, IsoGridSquare square2) {
        if (square1 == null || square2 == null) {
            return false;
        }
        if (square2 == square1.getAdjacentSquare(IsoDirections.E) && (square1.hasDoorOnEdge(IsoDirections.E, false) || square2.hasDoorOnEdge(IsoDirections.W, false) || square2.getProperties().has(IsoFlagType.DoorWallW))) {
            return true;
        }
        if (square2 == square1.getAdjacentSquare(IsoDirections.SE) && (square1.hasDoorOnEdge(IsoDirections.E, false) || square1.hasDoorOnEdge(IsoDirections.S, false) || square2.hasDoorOnEdge(IsoDirections.W, false) || square2.hasDoorOnEdge(IsoDirections.N, false) || square2.getProperties().has(IsoFlagType.DoorWallN) || square2.getProperties().has(IsoFlagType.DoorWallW))) {
            return true;
        }
        if (square2 == square1.getAdjacentSquare(IsoDirections.S) && (square1.hasDoorOnEdge(IsoDirections.S, false) || square2.hasDoorOnEdge(IsoDirections.N, false) || square2.getProperties().has(IsoFlagType.DoorWallN))) {
            return true;
        }
        return square1.isWallTo(square2) || square1.isWindowTo(square2);
    }

    private static boolean hasSomething4x4(int x1, int y1, int x2, int y2, int z) {
        IsoGridSquare square1 = IsoWorld.instance.currentCell.getGridSquare(x1, y1, z);
        IsoGridSquare square2 = IsoWorld.instance.currentCell.getGridSquare(x2, y1, z);
        IsoGridSquare square3 = IsoWorld.instance.currentCell.getGridSquare(x2, y2, z);
        IsoGridSquare square4 = IsoWorld.instance.currentCell.getGridSquare(x1, y2, z);
        if (IsoDoor.hasSolidObjects(square1) || IsoDoor.hasSolidObjects(square2) || IsoDoor.hasSolidObjects(square3) || IsoDoor.hasSolidObjects(square4)) {
            return true;
        }
        if (IsoDoor.isSomethingTo(square1, square2)) {
            return true;
        }
        if (IsoDoor.isSomethingTo(square1, square3)) {
            return true;
        }
        return IsoDoor.isSomethingTo(square1, square4);
    }

    public static boolean destroyDoubleDoor(IsoObject oneOfFour) {
        int ddIndex = IsoDoor.getDoubleDoorIndex(oneOfFour);
        if (ddIndex == -1) {
            return false;
        }
        if (ddIndex == 1 || ddIndex == 4) {
            IsoObject attached = IsoDoor.getDoubleDoorObject(oneOfFour, ddIndex == 1 ? 2 : 3);
            if (attached instanceof IsoDoor) {
                IsoDoor isoDoor = (IsoDoor)attached;
                isoDoor.destroy();
            } else if (attached instanceof IsoThumpable) {
                IsoThumpable isoThumpable = (IsoThumpable)attached;
                isoThumpable.destroy();
            }
        }
        if (oneOfFour instanceof IsoDoor) {
            IsoDoor isoDoor = (IsoDoor)oneOfFour;
            isoDoor.destroy();
        } else if (oneOfFour instanceof IsoThumpable) {
            IsoThumpable isoThumpable = (IsoThumpable)oneOfFour;
            isoThumpable.destroy();
        }
        LuaEventManager.triggerEvent("OnContainerUpdate");
        return true;
    }

    public static int getGarageDoorIndex(IsoObject oneOfThree) {
        boolean open;
        IsoThumpable isoThumpable;
        IsoThumpable thump;
        IsoDoor isoDoor;
        if (oneOfThree == null || oneOfThree.getSquare() == null) {
            return -1;
        }
        PropertyContainer props = oneOfThree.getProperties();
        if (props == null || !props.has(IsoPropertyType.GARAGE_DOOR)) {
            return -1;
        }
        int ddIndex = Integer.parseInt(props.get(IsoPropertyType.GARAGE_DOOR));
        if (ddIndex < 1 || ddIndex > 6) {
            return -1;
        }
        IsoDoor door = oneOfThree instanceof IsoDoor ? (isoDoor = (IsoDoor)oneOfThree) : null;
        IsoThumpable isoThumpable2 = thump = oneOfThree instanceof IsoThumpable ? (isoThumpable = (IsoThumpable)oneOfThree) : null;
        if (door == null && thump == null) {
            return -1;
        }
        boolean bl = open = door == null ? thump.open : door.open;
        if (open) {
            if (ddIndex >= 4) {
                return ddIndex - 3;
            }
            return -1;
        }
        return ddIndex;
    }

    public static IsoObject getGarageDoorPrev(IsoObject oneOfThree) {
        IsoObject obj;
        int i;
        int prevY;
        IsoThumpable isoThumpable;
        IsoDoor isoDoor;
        int oneOfThreeIndex = IsoDoor.getGarageDoorIndex(oneOfThree);
        if (oneOfThreeIndex == -1) {
            return null;
        }
        if (oneOfThreeIndex == 1) {
            return null;
        }
        IsoDoor door = oneOfThree instanceof IsoDoor ? (isoDoor = (IsoDoor)oneOfThree) : null;
        IsoThumpable thump = oneOfThree instanceof IsoThumpable ? (isoThumpable = (IsoThumpable)oneOfThree) : null;
        boolean north = door == null ? thump.north : door.north;
        IsoGridSquare sq = oneOfThree.getSquare();
        int prevX = sq.x - (north ? 1 : 0);
        sq = IsoWorld.instance.currentCell.getGridSquare(prevX, prevY = sq.y + (north ? 0 : 1), sq.getZ());
        if (sq == null) {
            return null;
        }
        ArrayList<IsoObject> specialObjects = sq.getSpecialObjects();
        if (door != null) {
            for (i = 0; i < specialObjects.size(); ++i) {
                obj = specialObjects.get(i);
                if (!(obj instanceof IsoDoor)) continue;
                IsoDoor isoDoor2 = (IsoDoor)obj;
                if (isoDoor2.north != north || IsoDoor.getGarageDoorIndex(obj) > oneOfThreeIndex) continue;
                return obj;
            }
        }
        if (thump != null) {
            for (i = 0; i < specialObjects.size(); ++i) {
                obj = specialObjects.get(i);
                if (!(obj instanceof IsoThumpable)) continue;
                IsoThumpable isoThumpable2 = (IsoThumpable)obj;
                if (isoThumpable2.north != north || IsoDoor.getGarageDoorIndex(obj) > oneOfThreeIndex) continue;
                return obj;
            }
        }
        return null;
    }

    public static IsoObject getGarageDoorNext(IsoObject oneOfThree) {
        IsoObject obj;
        int i;
        int nextY;
        IsoThumpable isoThumpable;
        IsoDoor isoDoor;
        int oneOfThreeIndex = IsoDoor.getGarageDoorIndex(oneOfThree);
        if (oneOfThreeIndex == -1) {
            return null;
        }
        if (oneOfThreeIndex == 3) {
            return null;
        }
        IsoDoor door = oneOfThree instanceof IsoDoor ? (isoDoor = (IsoDoor)oneOfThree) : null;
        IsoThumpable thump = oneOfThree instanceof IsoThumpable ? (isoThumpable = (IsoThumpable)oneOfThree) : null;
        boolean north = door == null ? thump.north : door.north;
        IsoGridSquare sq = oneOfThree.getSquare();
        int nextX = sq.x + (north ? 1 : 0);
        sq = IsoWorld.instance.currentCell.getGridSquare(nextX, nextY = sq.y - (north ? 0 : 1), sq.getZ());
        if (sq == null) {
            return null;
        }
        ArrayList<IsoObject> specialObjects = sq.getSpecialObjects();
        if (door != null) {
            for (i = 0; i < specialObjects.size(); ++i) {
                obj = specialObjects.get(i);
                if (!(obj instanceof IsoDoor)) continue;
                IsoDoor isoDoor2 = (IsoDoor)obj;
                if (isoDoor2.north != north || IsoDoor.getGarageDoorIndex(obj) < oneOfThreeIndex) continue;
                return obj;
            }
        }
        if (thump != null) {
            for (i = 0; i < specialObjects.size(); ++i) {
                obj = specialObjects.get(i);
                if (!(obj instanceof IsoThumpable)) continue;
                IsoThumpable isoThumpable2 = (IsoThumpable)obj;
                if (isoThumpable2.north != north || IsoDoor.getGarageDoorIndex(obj) < oneOfThreeIndex) continue;
                return obj;
            }
        }
        return null;
    }

    public static IsoObject getGarageDoorFirst(IsoObject oneOfThree) {
        int oneOfThreeIndex = IsoDoor.getGarageDoorIndex(oneOfThree);
        if (oneOfThreeIndex == -1) {
            return null;
        }
        if (oneOfThreeIndex == 1) {
            return oneOfThree;
        }
        IsoObject prev = IsoDoor.getGarageDoorPrev(oneOfThree);
        while (prev != null) {
            if (IsoDoor.getGarageDoorIndex(prev) == 1) {
                return prev;
            }
            prev = IsoDoor.getGarageDoorPrev(prev);
        }
        return oneOfThree;
    }

    public void changeSprite(IsoDoor door) {
        door.sprite = door.open ? door.openSprite : door.closedSprite;
    }

    private static void toggleGarageDoorObject(IsoObject oneOfThree) {
        boolean open;
        IsoThumpable isoThumpable;
        IsoDoor isoDoor;
        int oneOfThreeIndex = IsoDoor.getGarageDoorIndex(oneOfThree);
        if (oneOfThreeIndex == -1) {
            return;
        }
        IsoDoor door = oneOfThree instanceof IsoDoor ? (isoDoor = (IsoDoor)oneOfThree) : null;
        IsoThumpable thumpable = oneOfThree instanceof IsoThumpable ? (isoThumpable = (IsoThumpable)oneOfThree) : null;
        boolean bl = open = door == null ? thumpable.open : door.open;
        if (door != null) {
            door.open = !open;
            door.setLockedByKey(false);
            IsoSprite isoSprite = door.sprite = door.open ? door.openSprite : door.closedSprite;
        }
        if (thumpable != null) {
            thumpable.open = !open;
            thumpable.setLockedByKey(false);
            thumpable.sprite = thumpable.open ? thumpable.openSprite : thumpable.closedSprite;
        }
        oneOfThree.getSquare().RecalcAllWithNeighbours(true);
        PolygonalMap2.instance.squareChanged(oneOfThree.getSquare());
        oneOfThree.invalidateRenderChunkLevel(256L);
    }

    public static void toggleGarageDoor(IsoObject oneOfThree, boolean doSync) {
        IsoThumpable isoThumpable;
        IsoDoor isoDoor;
        int oneOfThreeIndex = IsoDoor.getGarageDoorIndex(oneOfThree);
        ArrayList<IsoObject> doorPartsIndexes = new ArrayList<IsoObject>();
        doorPartsIndexes.add(oneOfThree);
        IsoDoor door = oneOfThree instanceof IsoDoor ? (isoDoor = (IsoDoor)oneOfThree) : null;
        IsoThumpable thumpable = oneOfThree instanceof IsoThumpable ? (isoThumpable = (IsoThumpable)oneOfThree) : null;
        IsoDoor.toggleGarageDoorObject(oneOfThree);
        IsoObject prev = IsoDoor.getGarageDoorPrev(oneOfThree);
        while (prev != null) {
            IsoDoor.toggleGarageDoorObject(prev);
            doorPartsIndexes.add(prev);
            prev = IsoDoor.getGarageDoorPrev(prev);
        }
        IsoObject next = IsoDoor.getGarageDoorNext(oneOfThree);
        while (next != null) {
            IsoDoor.toggleGarageDoorObject(next);
            doorPartsIndexes.add(next);
            next = IsoDoor.getGarageDoorNext(next);
        }
        if (GameClient.client || GameServer.server) {
            oneOfThree.sync(door.open ? 1 : 0);
        }
        for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
            LosUtil.cachecleared[pn] = true;
        }
        IsoGridSquare.setRecalcLightTime(-1.0f);
        LuaEventManager.triggerEvent("OnContainerUpdate");
    }

    private static boolean isGarageDoorObstructed(IsoObject oneOfThree) {
        boolean open;
        IsoThumpable isoThumpable;
        IsoDoor isoDoor;
        int oneOfThreeIndex = IsoDoor.getGarageDoorIndex(oneOfThree);
        if (oneOfThreeIndex == -1) {
            return false;
        }
        IsoDoor door = oneOfThree instanceof IsoDoor ? (isoDoor = (IsoDoor)oneOfThree) : null;
        IsoThumpable thumpable = oneOfThree instanceof IsoThumpable ? (isoThumpable = (IsoThumpable)oneOfThree) : null;
        boolean north = door == null ? thumpable.north : door.north;
        boolean bl = open = door == null ? thumpable.open : door.open;
        if (!open) {
            return false;
        }
        int minX = oneOfThree.square.x;
        int minY = oneOfThree.square.y;
        int maxX = minX;
        int maxY = minY;
        if (north) {
            prev = IsoDoor.getGarageDoorPrev(oneOfThree);
            while (prev != null) {
                --minX;
                prev = IsoDoor.getGarageDoorPrev(prev);
            }
            next = IsoDoor.getGarageDoorNext(oneOfThree);
            while (next != null) {
                ++maxX;
                next = IsoDoor.getGarageDoorNext(next);
            }
        } else {
            prev = IsoDoor.getGarageDoorPrev(oneOfThree);
            while (prev != null) {
                ++maxY;
                prev = IsoDoor.getGarageDoorPrev(prev);
            }
            next = IsoDoor.getGarageDoorNext(oneOfThree);
            while (next != null) {
                --minY;
                next = IsoDoor.getGarageDoorNext(next);
            }
        }
        int chunkMinX = PZMath.fastfloor(((float)minX - 4.0f) / 8.0f);
        int chunkMinY = PZMath.fastfloor(((float)minY - 4.0f) / 8.0f);
        int chunkMaxX = (int)Math.ceil(((float)maxX + 4.0f) / 8.0f);
        int chunkMaxY = (int)Math.ceil(((float)maxY + 4.0f) / 8.0f);
        int z = oneOfThree.square.z;
        for (int cy = chunkMinY; cy <= chunkMaxY; ++cy) {
            for (int cx = chunkMinX; cx <= chunkMaxX; ++cx) {
                IsoChunk chunk;
                IsoChunk isoChunk = chunk = GameServer.server ? ServerMap.instance.getChunk(cx, cy) : IsoWorld.instance.currentCell.getChunk(cx, cy);
                if (chunk == null) continue;
                for (int i = 0; i < chunk.vehicles.size(); ++i) {
                    BaseVehicle vehicle = chunk.vehicles.get(i);
                    for (int y = minY; y <= maxY; ++y) {
                        for (int x = minX; x <= maxX; ++x) {
                            if (!vehicle.isIntersectingSquare(x, y, z) || !vehicle.isIntersectingSquare(x - (north ? 0 : 1), y - (north ? 1 : 0), z)) continue;
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean destroyGarageDoor(IsoObject oneOfThree) {
        IsoThumpable isoThumpable;
        IsoDoor isoDoor;
        int gdIndex = IsoDoor.getGarageDoorIndex(oneOfThree);
        if (gdIndex == -1) {
            return false;
        }
        IsoObject prev = IsoDoor.getGarageDoorPrev(oneOfThree);
        while (prev != null) {
            IsoObject prev2 = IsoDoor.getGarageDoorPrev(prev);
            if (prev instanceof IsoDoor) {
                isoDoor = (IsoDoor)prev;
                isoDoor.destroy();
            } else if (prev instanceof IsoThumpable) {
                isoThumpable = (IsoThumpable)prev;
                isoThumpable.destroy();
            }
            prev = prev2;
        }
        IsoObject next = IsoDoor.getGarageDoorNext(oneOfThree);
        while (next != null) {
            IsoObject next2 = IsoDoor.getGarageDoorNext(next);
            if (next instanceof IsoDoor) {
                IsoDoor isoDoor2 = (IsoDoor)next;
                isoDoor2.destroy();
            } else if (next instanceof IsoThumpable) {
                IsoThumpable isoThumpable2 = (IsoThumpable)next;
                isoThumpable2.destroy();
            }
            next = next2;
        }
        if (oneOfThree instanceof IsoDoor) {
            isoDoor = (IsoDoor)oneOfThree;
            isoDoor.destroy();
        } else if (oneOfThree instanceof IsoThumpable) {
            isoThumpable = (IsoThumpable)oneOfThree;
            isoThumpable.destroy();
        }
        LuaEventManager.triggerEvent("OnContainerUpdate");
        return true;
    }

    @Override
    public IsoObject getRenderEffectMaster() {
        int ddIndex = IsoDoor.getDoubleDoorIndex(this);
        if (ddIndex != -1) {
            IsoObject first = null;
            if (ddIndex == 2) {
                first = IsoDoor.getDoubleDoorObject(this, 1);
            } else if (ddIndex == 3) {
                first = IsoDoor.getDoubleDoorObject(this, 4);
            }
            if (first != null) {
                return first;
            }
        } else {
            IsoObject first = IsoDoor.getGarageDoorFirst(this);
            if (first != null) {
                return first;
            }
        }
        return this;
    }

    @Override
    public int getRenderEffectObjectCount() {
        int ddIndex = IsoDoor.getDoubleDoorIndex(this);
        if (ddIndex != -1) {
            return 2;
        }
        int gdIndex = IsoDoor.getGarageDoorIndex(this);
        if (gdIndex != -1) {
            IsoObject object = this;
            int count = 1;
            while ((object = IsoDoor.getGarageDoorPrev(object)) != null) {
                ++count;
            }
            object = this;
            while ((object = IsoDoor.getGarageDoorNext(object)) != null) {
                ++count;
            }
            return count;
        }
        return 1;
    }

    @Override
    public IsoObject getRenderEffectObjectByIndex(int index) {
        int ddIndex = IsoDoor.getDoubleDoorIndex(this);
        if (ddIndex != -1) {
            return IsoDoor.getDoubleDoorObject(this, index);
        }
        int gdIndex = IsoDoor.getGarageDoorIndex(this);
        if (gdIndex != -1) {
            IsoObject object = IsoDoor.getGarageDoorFirst(this);
            for (int count = 0; object != null && count != index; ++count) {
                object = IsoDoor.getGarageDoorNext(object);
            }
            return object;
        }
        return this;
    }

    public String getThumpSound() {
        String soundName;
        if (this.sprite != null && this.sprite.getProperties().has("ThumpSound") && !StringUtils.isNullOrWhitespace(soundName = this.sprite.getProperties().get("ThumpSound"))) {
            return soundName;
        }
        return switch (this.getSoundPrefix()) {
            case "MetalGate" -> SoundKey.ZOMBIE_THUMP_CHAINLINK_FENCE.toString();
            case "MetalPoleGate", "MetalPoleGateDouble" -> SoundKey.ZOMBIE_THUMP_METAL_POLE_FENCE.toString();
            case "GarageDoor" -> SoundKey.ZOMBIE_THUMP_GARAGE_DOOR.toString();
            case "MetalDoor", "PrisonMetalDoor" -> SoundKey.ZOMBIE_THUMP_METAL.toString();
            case "SlidingGlassDoor" -> SoundKey.ZOMBIE_THUMP_WINDOW.toString();
            default -> SoundKey.ZOMBIE_THUMP_GENERIC.toString();
        };
    }

    public String getSoundPrefix() {
        if (this.closedSprite == null) {
            return "WoodDoor";
        }
        PropertyContainer props = this.closedSprite.getProperties();
        if (props.has("DoorSound")) {
            return props.get("DoorSound");
        }
        return "WoodDoor";
    }

    private void playDoorSound(BaseCharacterSoundEmitter emitter, String suffix) {
        emitter.playSoundImpl(this.getSoundPrefix() + suffix, this);
    }

    @Override
    public SpriteModel getSpriteModel() {
        int ddIndex = IsoDoor.getDoubleDoorIndex(this);
        if ((ddIndex == 1 || ddIndex == 4) && IsoDoor.getDoubleDoorObject(this, ddIndex == 1 ? 2 : 3) == null) {
            return null;
        }
        return super.getSpriteModel();
    }

    public void forEachDoorObject(Consumer<IsoDoor> consumer) {
        IsoDoor.forEachDoorObject(this, object -> {
            if (object instanceof IsoDoor) {
                IsoDoor door = (IsoDoor)object;
                consumer.accept(door);
            }
        });
    }

    public static void forEachDoorObject(IsoObject object, Consumer<IsoObject> consumer) {
        if (IsoDoor.getDoubleDoorIndex(object) != -1) {
            for (int ddIndex = 1; ddIndex <= 4; ++ddIndex) {
                IsoObject isoObject = IsoDoor.getDoubleDoorObject(object, ddIndex);
                if (!(isoObject instanceof IsoObject)) continue;
                IsoObject door = isoObject;
                consumer.accept(door);
            }
            return;
        }
        consumer.accept(object);
        if (IsoDoor.getGarageDoorIndex(object) != -1) {
            IsoObject prev = IsoDoor.getGarageDoorPrev(object);
            while (prev != null) {
                consumer.accept(prev);
                prev = IsoDoor.getGarageDoorPrev(prev);
            }
            IsoObject next = IsoDoor.getGarageDoorNext(object);
            while (next != null) {
                consumer.accept(next);
                next = IsoDoor.getGarageDoorNext(next);
            }
        }
    }

    public static enum DoorType {
        WeakWooden,
        StrongWooden;

    }
}

