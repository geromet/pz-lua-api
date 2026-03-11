/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import java.util.ArrayList;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.AlarmClock;
import zombie.inventory.types.AlarmClockClothing;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.scripting.objects.ItemTag;
import zombie.ui.UIElement;
import zombie.ui.UIManager;

@UsedFromLua
public final class Clock
extends UIElement {
    private boolean largeTextures;
    private Texture background;
    private Texture[] digitsLarge;
    private Texture[] digitsSmall;
    private Texture colon;
    private Texture slash;
    private Texture minus;
    private Texture dot;
    private Texture tempC;
    private Texture tempF;
    private Texture tempE;
    private Texture texAm;
    private Texture texPm;
    private Texture alarmOn;
    private Texture alarmRinging;
    private final Color displayColour = new Color(100, 200, 210, 255);
    private final Color ghostColour = new Color(40, 40, 40, 128);
    private int uxOriginal;
    private int uyOriginal;
    private int largeDigitSpacing;
    private int smallDigitSpacing;
    private int colonSpacing;
    private int ampmSpacing;
    private int alarmBellSpacing;
    private int decimalSpacing;
    private int degreeSpacing;
    private int slashSpacing;
    private int tempDateSpacing;
    private int dateOffset;
    private int minusOffset;
    private int amVerticalSpacing;
    private int pmVerticalSpacing;
    private int alarmBellVerticalSpacing;
    private int displayVerticalSpacing;
    private int decimalVerticalSpacing;
    private boolean digital;
    private boolean isAlarmSet;
    private boolean isAlarmRinging;
    private IsoPlayer clockPlayer;
    public static Clock instance;

    public Clock(int x, int y) {
        this.x = x;
        this.y = y;
        instance = this;
    }

    @Override
    public void render() {
        if (!this.visible) {
            return;
        }
        this.assignTextures(Core.getInstance().getOptionClockSize() == 2);
        this.DrawTexture(this.background, 0.0, 0.0, 0.75);
        this.renderDisplay(true, this.ghostColour);
        this.renderDisplay(false, this.displayColour);
        super.render();
    }

    private void renderDisplay(boolean ghostDisplay, Color color) {
        int ux = this.uxOriginal;
        int uy = this.uyOriginal;
        for (int i = 0; i < 4; ++i) {
            int[] timeDigits = this.timeDigits();
            if (ghostDisplay) {
                this.DrawTextureCol(this.digitsLarge[8], ux, uy, color);
            } else {
                this.DrawTextureCol(this.digitsLarge[timeDigits[i]], ux, uy, color);
            }
            ux += this.digitsLarge[0].getWidth();
            if (i == 1) {
                this.DrawTextureCol(this.colon, ux += this.colonSpacing, uy, color);
                ux += this.colon.getWidth() + this.colonSpacing;
                continue;
            }
            if (i >= 3) continue;
            ux += this.largeDigitSpacing;
        }
        ux += this.ampmSpacing;
        if (!Core.getInstance().getOptionClock24Hour() || ghostDisplay) {
            if (ghostDisplay) {
                this.DrawTextureCol(this.texAm, ux, uy + this.amVerticalSpacing, color);
                this.DrawTextureCol(this.texPm, ux, uy + this.pmVerticalSpacing, color);
            } else if (GameTime.getInstance().getTimeOfDay() < 12.0f) {
                this.DrawTextureCol(this.texAm, ux, uy + this.amVerticalSpacing, color);
            } else {
                this.DrawTextureCol(this.texPm, ux, uy + this.pmVerticalSpacing, color);
            }
        }
        if (this.isAlarmRinging || ghostDisplay) {
            this.DrawTextureCol(this.alarmRinging, ux + this.texAm.getWidth() + this.alarmBellSpacing, uy + this.alarmBellVerticalSpacing, color);
        } else if (this.isAlarmSet) {
            this.DrawTextureCol(this.alarmOn, ux + this.texAm.getWidth() + this.alarmBellSpacing, uy + this.alarmBellVerticalSpacing, color);
        }
        if (this.digital || ghostDisplay) {
            ux = this.uxOriginal;
            uy += this.digitsLarge[0].getHeight() + this.displayVerticalSpacing;
            if (this.clockPlayer != null) {
                int[] tempDigits = this.tempDigits();
                if (tempDigits[0] == 1 || ghostDisplay) {
                    this.DrawTextureCol(this.minus, ux, uy, color);
                }
                ux += this.minusOffset;
                if (tempDigits[1] == 1 || ghostDisplay) {
                    this.DrawTextureCol(this.digitsSmall[1], ux, uy, color);
                }
                ux += this.digitsSmall[0].getWidth() + this.smallDigitSpacing;
                for (int i = 2; i < 5; ++i) {
                    if (ghostDisplay) {
                        this.DrawTextureCol(this.digitsSmall[8], ux, uy, color);
                    } else {
                        this.DrawTextureCol(this.digitsSmall[tempDigits[i]], ux, uy, color);
                    }
                    ux += this.digitsSmall[0].getWidth();
                    if (i == 3) {
                        this.DrawTextureCol(this.dot, ux += this.decimalSpacing, uy + this.decimalVerticalSpacing, color);
                        ux += this.dot.getWidth() + this.decimalSpacing;
                        continue;
                    }
                    if (i >= 4) continue;
                    ux += this.smallDigitSpacing;
                }
                this.DrawTextureCol(this.dot, ux += this.degreeSpacing, uy, color);
                ux += this.dot.getWidth() + this.degreeSpacing;
                if (ghostDisplay) {
                    this.DrawTextureCol(this.tempE, ux, uy, color);
                } else if (tempDigits[5] == 0) {
                    this.DrawTextureCol(this.tempC, ux, uy, color);
                } else {
                    this.DrawTextureCol(this.tempF, ux, uy, color);
                }
                ux += this.digitsSmall[0].getWidth() + this.tempDateSpacing;
            } else {
                ux += this.dateOffset;
            }
            int[] dateDigits = this.dateDigits();
            for (int i = 0; i < 4; ++i) {
                if (ghostDisplay) {
                    this.DrawTextureCol(this.digitsSmall[8], ux, uy, color);
                } else {
                    this.DrawTextureCol(this.digitsSmall[dateDigits[i]], ux, uy, color);
                }
                ux += this.digitsSmall[0].getWidth();
                if (i == 1) {
                    this.DrawTextureCol(this.slash, ux += this.slashSpacing, uy, color);
                    ux += this.slash.getWidth() + this.slashSpacing;
                    continue;
                }
                if (i >= 3) continue;
                ux += this.smallDigitSpacing;
            }
        }
    }

    private void assignTextures(boolean largeTextures) {
        if (this.digitsLarge != null && this.largeTextures == largeTextures) {
            return;
        }
        this.largeTextures = largeTextures;
        this.background = largeTextures ? Texture.getSharedTexture("media/ui/ClockAssets/ClockLargeBackground.png") : Texture.getSharedTexture("media/ui/ClockAssets/ClockSmallBackground.png");
        String largeTex = "Medium";
        String smallTex = "Small";
        if (largeTextures) {
            largeTex = "Large";
            smallTex = "Medium";
            this.assignLargeOffsets();
        } else {
            this.assignSmallOffsets();
        }
        if (this.digitsLarge == null) {
            this.digitsLarge = new Texture[10];
            this.digitsSmall = new Texture[10];
        }
        for (int n = 0; n < 10; ++n) {
            this.digitsLarge[n] = Texture.getSharedTexture("media/ui/ClockAssets/ClockDigits" + largeTex + n + ".png");
            this.digitsSmall[n] = Texture.getSharedTexture("media/ui/ClockAssets/ClockDigits" + smallTex + n + ".png");
        }
        this.colon = Texture.getSharedTexture("media/ui/ClockAssets/ClockDivide" + largeTex + ".png");
        this.slash = Texture.getSharedTexture("media/ui/ClockAssets/DateDivide" + smallTex + ".png");
        this.minus = Texture.getSharedTexture("media/ui/ClockAssets/ClockDigits" + smallTex + "Minus.png");
        this.dot = Texture.getSharedTexture("media/ui/ClockAssets/ClockDigits" + smallTex + "Dot.png");
        this.tempC = Texture.getSharedTexture("media/ui/ClockAssets/ClockDigits" + smallTex + "C.png");
        this.tempF = Texture.getSharedTexture("media/ui/ClockAssets/ClockDigits" + smallTex + "F.png");
        this.tempE = Texture.getSharedTexture("media/ui/ClockAssets/ClockDigits" + smallTex + "E.png");
        this.texAm = Texture.getSharedTexture("media/ui/ClockAssets/ClockAm" + largeTex + ".png");
        this.texPm = Texture.getSharedTexture("media/ui/ClockAssets/ClockPm" + largeTex + ".png");
        this.alarmOn = Texture.getSharedTexture("media/ui/ClockAssets/ClockAlarm" + largeTex + "Set.png");
        this.alarmRinging = Texture.getSharedTexture("media/ui/ClockAssets/ClockAlarm" + largeTex + "Sound.png");
    }

    private void assignSmallOffsets() {
        this.uxOriginal = 3;
        this.uyOriginal = 3;
        this.largeDigitSpacing = 1;
        this.smallDigitSpacing = 1;
        this.colonSpacing = 1;
        this.ampmSpacing = 1;
        this.alarmBellSpacing = 1;
        this.decimalSpacing = 1;
        this.degreeSpacing = 1;
        this.slashSpacing = 1;
        this.tempDateSpacing = 5;
        this.dateOffset = 33;
        this.minusOffset = 0;
        this.amVerticalSpacing = 7;
        this.pmVerticalSpacing = 12;
        this.alarmBellVerticalSpacing = 1;
        this.displayVerticalSpacing = 2;
        this.decimalVerticalSpacing = 6;
    }

    private void assignLargeOffsets() {
        this.uxOriginal = 3;
        this.uyOriginal = 3;
        this.largeDigitSpacing = 2;
        this.smallDigitSpacing = 1;
        this.colonSpacing = 3;
        this.ampmSpacing = 3;
        this.alarmBellSpacing = 5;
        this.decimalSpacing = 2;
        this.degreeSpacing = 2;
        this.slashSpacing = 2;
        this.tempDateSpacing = 8;
        this.dateOffset = 65;
        this.minusOffset = -2;
        this.amVerticalSpacing = 15;
        this.pmVerticalSpacing = 25;
        this.alarmBellVerticalSpacing = 1;
        this.displayVerticalSpacing = 5;
        this.decimalVerticalSpacing = 15;
    }

    private int[] timeDigits() {
        float time = GameTime.getInstance().getTimeOfDay();
        if (GameClient.client && GameClient.fastForward) {
            time = GameTime.getInstance().serverTimeOfDay;
        }
        if (!Core.getInstance().getOptionClock24Hour()) {
            if (time >= 13.0f) {
                time -= 12.0f;
            }
            if (time < 1.0f) {
                time += 12.0f;
            }
        }
        int hours = (int)time;
        float minutes = (time - (float)((int)time)) * 60.0f;
        int hourTens = hours / 10;
        int hourUnit = hours % 10;
        int minTens = (int)(minutes / 10.0f);
        return new int[]{hourTens, hourUnit, minTens, 0};
    }

    private int[] dateDigits() {
        int dayTens = (GameTime.getInstance().getDay() + 1) / 10;
        int dayUnit = (GameTime.getInstance().getDay() + 1) % 10;
        int monthTens = (GameTime.getInstance().getMonth() + 1) / 10;
        int monthUnit = (GameTime.getInstance().getMonth() + 1) % 10;
        if (Core.getInstance().getOptionClockFormat() == 1) {
            return new int[]{monthTens, monthUnit, dayTens, dayUnit};
        }
        return new int[]{dayTens, dayUnit, monthTens, monthUnit};
    }

    private int[] tempDigits() {
        float temperature = ClimateManager.getInstance().getAirTemperatureForCharacter(this.clockPlayer, false);
        int negative = 0;
        int fahrenheit = 0;
        if (!Core.getInstance().getOptionTemperatureDisplayCelsius()) {
            temperature = temperature * 1.8f + 32.0f;
            fahrenheit = 1;
        }
        if (temperature < 0.0f) {
            negative = 1;
            temperature *= -1.0f;
        }
        int tempHund = (int)temperature / 100;
        int tempTens = (int)(temperature % 100.0f) / 10;
        int tempUnit = (int)temperature % 10;
        int tempDeci = (int)(temperature * 10.0f) % 10;
        return new int[]{negative, tempHund, tempTens, tempUnit, tempDeci, fahrenheit};
    }

    public void resize() {
        this.visible = false;
        this.digital = false;
        this.clockPlayer = null;
        this.isAlarmSet = false;
        this.isAlarmRinging = false;
        if (IsoPlayer.getInstance() != null) {
            for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player == null || player.isDead()) continue;
                for (int j = 0; j < player.getWornItems().size(); ++j) {
                    InventoryItem item = player.getWornItems().getItemByIndex(j);
                    if (!(item instanceof AlarmClock) && !(item instanceof AlarmClockClothing)) continue;
                    this.visible = UIManager.visibleAllUi;
                    this.digital |= item.hasTag(ItemTag.DIGITAL);
                    if (item instanceof AlarmClock) {
                        AlarmClock alarmClock = (AlarmClock)item;
                        if (alarmClock.isAlarmSet()) {
                            this.isAlarmSet = true;
                        }
                        if (alarmClock.isRinging()) {
                            this.isAlarmRinging = true;
                        }
                    } else {
                        if (((AlarmClockClothing)item).isAlarmSet()) {
                            this.isAlarmSet = true;
                        }
                        if (((AlarmClockClothing)item).isRinging()) {
                            this.isAlarmRinging = true;
                        }
                    }
                    this.clockPlayer = player;
                }
                if (this.clockPlayer != null) break;
                ArrayList<InventoryItem> items = player.getInventory().getItems();
                for (int j = 0; j < items.size(); ++j) {
                    InventoryItem item = items.get(j);
                    if (!(item instanceof AlarmClock) && !(item instanceof AlarmClockClothing) || !item.isWorn() && !item.isEquipped()) continue;
                    this.visible = UIManager.visibleAllUi;
                    this.digital |= item.hasTag(ItemTag.DIGITAL);
                    if (item instanceof AlarmClock) {
                        AlarmClock alarmClock = (AlarmClock)item;
                        if (alarmClock.isAlarmSet()) {
                            this.isAlarmSet = true;
                        }
                        if (alarmClock.isRinging()) {
                            this.isAlarmRinging = true;
                        }
                    } else {
                        if (((AlarmClockClothing)item).isAlarmSet()) {
                            this.isAlarmSet = true;
                        }
                        if (((AlarmClockClothing)item).isRinging()) {
                            this.isAlarmRinging = true;
                        }
                    }
                    this.clockPlayer = player;
                }
            }
        }
        if (DebugOptions.instance.cheat.clock.visible.getValue()) {
            this.digital = true;
            this.visible = UIManager.visibleAllUi;
        }
        if (this.background == null) {
            this.background = Core.getInstance().getOptionClockSize() == 2 ? Texture.getSharedTexture("media/ui/ClockAssets/ClockLargeBackground.png") : Texture.getSharedTexture("media/ui/ClockAssets/ClockSmallBackground.png");
        }
        this.setHeight(this.background.getHeight());
        this.setWidth(this.background.getWidth());
    }

    public boolean isDateVisible() {
        return this.visible && this.digital;
    }

    @Override
    public Boolean onMouseDown(double x, double y) {
        block20: {
            block21: {
                block19: {
                    if (!this.isVisible().booleanValue()) {
                        return false;
                    }
                    if (!this.isAlarmRinging) break block19;
                    if (IsoPlayer.getInstance() == null) break block20;
                    for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                        AlarmClockClothing alarmClockClothing;
                        AlarmClock alarmClock;
                        int j;
                        IsoPlayer player = IsoPlayer.players[i];
                        if (player == null || player.isDead()) continue;
                        for (j = 0; j < player.getWornItems().size(); ++j) {
                            InventoryItem item = player.getWornItems().getItemByIndex(j);
                            if (item instanceof AlarmClock) {
                                alarmClock = (AlarmClock)item;
                                alarmClock.stopRinging();
                                continue;
                            }
                            if (!(item instanceof AlarmClockClothing)) continue;
                            alarmClockClothing = (AlarmClockClothing)item;
                            alarmClockClothing.stopRinging();
                        }
                        for (j = 0; j < player.getInventory().getItems().size(); ++j) {
                            InventoryItem inventoryItem = player.getInventory().getItems().get(j);
                            if (inventoryItem instanceof AlarmClock) {
                                alarmClock = (AlarmClock)inventoryItem;
                                alarmClock.stopRinging();
                                continue;
                            }
                            if (!(inventoryItem instanceof AlarmClockClothing)) continue;
                            alarmClockClothing = (AlarmClockClothing)inventoryItem;
                            alarmClockClothing.stopRinging();
                        }
                    }
                    break block20;
                }
                if (!this.isAlarmSet) break block21;
                if (IsoPlayer.getInstance() == null) break block20;
                for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                    AlarmClock alarmClock;
                    int j;
                    IsoPlayer player = IsoPlayer.players[i];
                    if (player == null || player.isDead()) continue;
                    for (j = 0; j < player.getWornItems().size(); ++j) {
                        AlarmClockClothing alarmClockClothing;
                        InventoryItem item = player.getWornItems().getItemByIndex(j);
                        if (item instanceof AlarmClock && (alarmClock = (AlarmClock)item).isAlarmSet()) {
                            alarmClock.setAlarmSet(false);
                            continue;
                        }
                        if (!(item instanceof AlarmClockClothing) || !(alarmClockClothing = (AlarmClockClothing)item).isAlarmSet()) continue;
                        alarmClockClothing.setAlarmSet(false);
                    }
                    for (j = 0; j < player.getInventory().getItems().size(); ++j) {
                        AlarmClockClothing alarmClockClothing;
                        InventoryItem inventoryItem = player.getInventory().getItems().get(j);
                        if (inventoryItem instanceof AlarmClockClothing && (alarmClockClothing = (AlarmClockClothing)inventoryItem).isAlarmSet()) {
                            alarmClockClothing.setAlarmSet(false);
                        }
                        if (!(inventoryItem instanceof AlarmClock) || !(alarmClock = (AlarmClock)inventoryItem).isAlarmSet()) continue;
                        alarmClock.setAlarmSet(false);
                    }
                }
                break block20;
            }
            if (IsoPlayer.getInstance() != null) {
                for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                    AlarmClockClothing alarmClockClothing;
                    AlarmClock alarmClock;
                    int j;
                    IsoPlayer player = IsoPlayer.players[i];
                    if (player == null || player.isDead()) continue;
                    for (j = 0; j < player.getWornItems().size(); ++j) {
                        InventoryItem item = player.getWornItems().getItemByIndex(j);
                        if (item instanceof AlarmClock && (alarmClock = (AlarmClock)item).isDigital() && !alarmClock.isAlarmSet()) {
                            alarmClock.setAlarmSet(true);
                            if (this.isAlarmSet) {
                                return true;
                            }
                        }
                        if (!(item instanceof AlarmClockClothing) || !(alarmClockClothing = (AlarmClockClothing)item).isDigital() || alarmClockClothing.isAlarmSet()) continue;
                        alarmClockClothing.setAlarmSet(true);
                        if (!this.isAlarmSet) continue;
                        return true;
                    }
                    for (j = 0; j < player.getInventory().getItems().size(); ++j) {
                        InventoryItem inventoryItem = player.getInventory().getItems().get(j);
                        if (inventoryItem instanceof AlarmClock && (alarmClock = (AlarmClock)inventoryItem).isDigital() && !alarmClock.isAlarmSet()) {
                            alarmClock.setAlarmSet(true);
                            if (this.isAlarmSet) {
                                return true;
                            }
                        }
                        if (!(inventoryItem instanceof AlarmClockClothing) || !(alarmClockClothing = (AlarmClockClothing)inventoryItem).isDigital() || alarmClockClothing.isAlarmSet()) continue;
                        alarmClockClothing.setAlarmSet(true);
                        if (!this.isAlarmSet) continue;
                        return true;
                    }
                }
            }
        }
        return true;
    }
}

