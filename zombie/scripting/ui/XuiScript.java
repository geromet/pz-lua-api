/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.ui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptParser;
import zombie.scripting.ui.TextAlign;
import zombie.scripting.ui.VectorPosAlign;
import zombie.scripting.ui.XuiAutoApply;
import zombie.scripting.ui.XuiManager;
import zombie.scripting.ui.XuiReference;
import zombie.scripting.ui.XuiScriptType;
import zombie.scripting.ui.XuiSkin;
import zombie.scripting.ui.XuiTableScript;
import zombie.scripting.ui.XuiVarType;
import zombie.ui.UIFont;

@UsedFromLua
public class XuiScript {
    private static final String xui_prefix = "xui_";
    protected HashMap<String, XuiVar<?, ?>> varsMap = new HashMap();
    protected ArrayList<XuiVar<?, ?>> vars = new ArrayList();
    protected final ArrayList<XuiScript> children = new ArrayList();
    protected XuiSkin xuiSkin;
    protected final boolean readAltKeys;
    protected final XuiScriptType scriptType;
    protected final String xuiLayoutName;
    private XuiScript defaultStyle;
    private XuiScript style;
    public final String xuiUuid;
    public final XuiString xuiKey;
    public final XuiString xuiLuaClass;
    public final XuiString xuiStyle;
    public final XuiString xuiCustomDebug;
    public final XuiUnit x;
    public final XuiUnit y;
    public final XuiUnit width;
    public final XuiUnit height;
    public final XuiVector vector;
    public final XuiVectorPosAlign posAlign;
    public final XuiFloat minimumWidth;
    public final XuiFloat minimumHeight;
    public final XuiFloat maximumWidth;
    public final XuiFloat maximumHeight;
    public final XuiUnit paddingTop;
    public final XuiUnit paddingRight;
    public final XuiUnit paddingBottom;
    public final XuiUnit paddingLeft;
    public final XuiSpacing padding;
    public final XuiUnit marginTop;
    public final XuiUnit marginRight;
    public final XuiUnit marginBottom;
    public final XuiUnit marginLeft;
    public final XuiSpacing margin;
    public final XuiTranslateString title;
    public final XuiTranslateString name;
    public final XuiFontType font;
    public final XuiFontType font2;
    public final XuiFontType font3;
    public final XuiTexture icon;
    public final XuiUnit iconX;
    public final XuiUnit iconY;
    public final XuiUnit iconWidth;
    public final XuiUnit iconHeight;
    public final XuiVector iconVector;
    public final XuiTexture image;
    public final XuiUnit imageX;
    public final XuiUnit imageY;
    public final XuiUnit imageWidth;
    public final XuiUnit imageHeight;
    public final XuiVector imageVector;
    public final XuiBoolean anchorLeft;
    public final XuiBoolean anchorRight;
    public final XuiBoolean anchorTop;
    public final XuiBoolean anchorBottom;
    public final XuiStringList animationList;
    public final XuiFloat animationTime;
    public final XuiTexture textureBackground;
    public final XuiTexture texture;
    public final XuiTexture textureOverride;
    public final XuiTexture tickTexture;
    public final XuiColor textColor;
    public final XuiColor backgroundColor;
    public final XuiColor backgroundColorMouseOver;
    public final XuiColor borderColor;
    public final XuiColor textureColor;
    public final XuiColor choicesColor;
    public final XuiColor gridColor;
    public final XuiBoolean displayBackground;
    public final XuiBoolean background;
    public final XuiBoolean drawGrid;
    public final XuiBoolean drawBackground;
    public final XuiBoolean drawBorder;
    public final XuiTranslateString tooltip;
    public final XuiColor hsbFactor;
    public final XuiBoolean moveWithMouse;
    public final XuiBoolean mouseOver;
    public final XuiTranslateString mouseOverText;
    public final XuiTextAlign textAlign;
    public final XuiBoolean doHighlight;
    public final XuiColor backgroundColorHl;
    public final XuiColor borderColorHl;
    public final XuiBoolean doValidHighlight;
    public final XuiColor backgroundColorHlVal;
    public final XuiColor borderColorHlVal;
    public final XuiBoolean doInvalidHighlight;
    public final XuiColor backgroundColorHlInv;
    public final XuiColor borderColorHlInv;
    public final XuiBoolean storeItem;
    public final XuiBoolean doBackDropTex;
    public final XuiColor backDropTexCol;
    public final XuiBoolean doToolTip;
    public final XuiBoolean mouseEnabled;
    public final XuiBoolean allowDropAlways;
    public final XuiTranslateString toolTipTextItem;
    public final XuiTranslateString toolTipTextLocked;
    public final XuiColor backgroundEmpty;
    public final XuiColor backgroundHover;
    public final XuiColor borderInput;
    public final XuiColor borderOutput;
    public final XuiColor borderValid;
    public final XuiColor borderInvalid;
    public final XuiColor borderLocked;
    public final XuiBoolean doBorderLocked;
    public final XuiBoolean pin;
    public final XuiBoolean resizable;
    public final XuiBoolean enableHeader;
    public final XuiFloat scaledWidth;
    public final XuiFloat scaledHeight;

    public XuiScript(String xuiLayoutName, boolean readAltKeys, String xuiLuaClass) {
        this(xuiLayoutName, readAltKeys, xuiLuaClass, XuiScriptType.Layout);
    }

    public XuiScript(String xuiLayoutName, boolean readAltKeys, String xuiLuaClass, XuiScriptType type) {
        this.xuiLayoutName = xuiLayoutName;
        this.readAltKeys = readAltKeys;
        this.scriptType = type;
        this.xuiUuid = UUID.randomUUID().toString();
        int order = 9000;
        this.xuiLuaClass = this.addVar(new XuiString(this, "xuiLuaClass", "ISUIElement"));
        if (xuiLuaClass != null) {
            this.xuiLuaClass.setValue(xuiLuaClass);
        }
        this.xuiLuaClass.setAutoApplyMode(XuiAutoApply.Forbidden);
        this.xuiLuaClass.setIgnoreStyling(true);
        --order;
        order = this.xuiLuaClass.setUiOrder(order);
        this.xuiKey = this.addVar(new XuiString(this, "xuiKey", UUID.randomUUID().toString()));
        this.xuiKey.setAutoApplyMode(XuiAutoApply.Forbidden);
        --order;
        order = this.xuiKey.setUiOrder(order);
        this.xuiStyle = this.addVar(new XuiString(this, "xuiStyle"));
        this.xuiStyle.setAutoApplyMode(XuiAutoApply.Forbidden);
        this.xuiStyle.setScriptLoadEnabled(false);
        this.xuiStyle.setIgnoreStyling(true);
        --order;
        order = this.xuiStyle.setUiOrder(order);
        this.xuiCustomDebug = this.addVar(new XuiString(this, "xuiCustomDebug"));
        this.xuiCustomDebug.setAutoApplyMode(XuiAutoApply.Forbidden);
        this.xuiCustomDebug.setIgnoreStyling(true);
        --order;
        order = this.xuiCustomDebug.setUiOrder(order);
        this.x = this.addVar(new XuiUnit(this, "x", 0.0f));
        this.x.setAutoApplyMode(XuiAutoApply.Forbidden);
        --order;
        order = this.x.setUiOrder(order);
        this.y = this.addVar(new XuiUnit(this, "y", 0.0f));
        this.y.setAutoApplyMode(XuiAutoApply.Forbidden);
        --order;
        order = this.y.setUiOrder(order);
        this.width = this.addVar(new XuiUnit(this, "width", 0.0f));
        this.width.setAutoApplyMode(XuiAutoApply.No);
        --order;
        order = this.width.setUiOrder(order);
        this.height = this.addVar(new XuiUnit(this, "height", 0.0f));
        this.height.setAutoApplyMode(XuiAutoApply.No);
        --order;
        order = this.height.setUiOrder(order);
        this.vector = this.addVar(new XuiVector(this, "vector", this.x, this.y, this.width, this.height));
        --order;
        order = this.vector.setUiOrder(order);
        this.posAlign = this.addVar(new XuiVectorPosAlign(this, "vectorPosAlign", VectorPosAlign.TopLeft));
        this.posAlign.setAutoApplyMode(XuiAutoApply.Always);
        --order;
        order = this.posAlign.setUiOrder(order);
        this.textAlign = this.addVar(new XuiTextAlign(this, "textAlign"));
        this.textAlign.setAutoApplyMode(XuiAutoApply.No);
        this.minimumWidth = this.addVar(new XuiFloat(this, "minimumWidth"));
        this.minimumHeight = this.addVar(new XuiFloat(this, "minimumHeight"));
        this.maximumWidth = this.addVar(new XuiFloat(this, "maximumWidth"));
        this.maximumHeight = this.addVar(new XuiFloat(this, "maximumHeight"));
        this.scaledWidth = this.addVar(new XuiFloat(this, "scaledWidth"));
        this.scaledHeight = this.addVar(new XuiFloat(this, "scaledHeight"));
        this.addVar(new XuiUnit(this, "maximumHeightPercent", -1.0f));
        this.paddingTop = this.addVar(new XuiUnit(this, "paddingTop", 0.0f));
        this.paddingRight = this.addVar(new XuiUnit(this, "paddingRight", 0.0f));
        this.paddingBottom = this.addVar(new XuiUnit(this, "paddingBottom", 0.0f));
        this.paddingLeft = this.addVar(new XuiUnit(this, "paddingLeft", 0.0f));
        this.padding = this.addVar(new XuiSpacing(this, "padding", this.paddingTop, this.paddingRight, this.paddingBottom, this.paddingLeft));
        this.marginTop = this.addVar(new XuiUnit(this, "marginTop", 0.0f));
        this.marginRight = this.addVar(new XuiUnit(this, "marginRight", 0.0f));
        this.marginBottom = this.addVar(new XuiUnit(this, "marginBottom", 0.0f));
        this.marginLeft = this.addVar(new XuiUnit(this, "marginLeft", 0.0f));
        this.margin = this.addVar(new XuiSpacing(this, "margin", this.marginTop, this.marginRight, this.marginBottom, this.marginLeft));
        this.icon = this.addVar(new XuiTexture(this, "icon"));
        this.iconX = this.addVar(new XuiUnit(this, "icon_x", 0.0f));
        this.iconX.setAutoApplyMode(XuiAutoApply.No);
        this.iconY = this.addVar(new XuiUnit(this, "icon_y", 0.0f));
        this.iconY.setAutoApplyMode(XuiAutoApply.No);
        this.iconWidth = this.addVar(new XuiUnit(this, "icon_width", 0.0f));
        this.iconWidth.setAutoApplyMode(XuiAutoApply.No);
        this.iconHeight = this.addVar(new XuiUnit(this, "icon_height", 0.0f));
        this.iconHeight.setAutoApplyMode(XuiAutoApply.No);
        this.iconVector = this.addVar(new XuiVector(this, "icon_vector", this.iconX, this.iconY, this.iconWidth, this.iconHeight));
        this.image = this.addVar(new XuiTexture(this, "image"));
        this.imageX = this.addVar(new XuiUnit(this, "image_x", 0.0f));
        this.imageX.setAutoApplyMode(XuiAutoApply.No);
        this.imageY = this.addVar(new XuiUnit(this, "image_y", 0.0f));
        this.imageY.setAutoApplyMode(XuiAutoApply.No);
        this.imageWidth = this.addVar(new XuiUnit(this, "image_width", 0.0f));
        this.imageWidth.setAutoApplyMode(XuiAutoApply.No);
        this.imageHeight = this.addVar(new XuiUnit(this, "image_height", 0.0f));
        this.imageHeight.setAutoApplyMode(XuiAutoApply.No);
        this.imageVector = this.addVar(new XuiVector(this, "image_vector", this.imageX, this.imageY, this.imageWidth, this.imageHeight));
        this.anchorLeft = this.addVar(new XuiBoolean(this, "anchorLeft"));
        this.anchorRight = this.addVar(new XuiBoolean(this, "anchorRight"));
        this.anchorTop = this.addVar(new XuiBoolean(this, "anchorTop"));
        this.anchorBottom = this.addVar(new XuiBoolean(this, "anchorBottom"));
        this.animationList = this.addVar(new XuiStringList(this, "animationList"));
        this.addVar(new XuiFloat(this, "r", 1.0f));
        this.addVar(new XuiFloat(this, "g", 1.0f));
        this.addVar(new XuiFloat(this, "b", 1.0f));
        this.addVar(new XuiFloat(this, "a", 1.0f));
        this.addVar(new XuiFloat(this, "textR", 1.0f));
        this.addVar(new XuiFloat(this, "textG", 1.0f));
        this.addVar(new XuiFloat(this, "textB", 1.0f));
        this.animationTime = this.addVar(new XuiFloat(this, "animationTime", 1.0f));
        this.addVar(new XuiFloat(this, "boxSize", 16.0f));
        this.addVar(new XuiFloat(this, "bubblesAlpha", 1.0f));
        this.addVar(new XuiFloat(this, "contentTransparency", 1.0f));
        this.addVar(new XuiFloat(this, "currentValue", 1.0f));
        this.addVar(new XuiFloat(this, "differenceAlpha", 1.0f));
        this.addVar(new XuiFloat(this, "gradientAlpha", 1.0f));
        this.addVar(new XuiFloat(this, "itemGap", 4.0f));
        this.addVar(new XuiFloat(this, "itemheight", 30.0f));
        this.addVar(new XuiFloat(this, "itemHgt", 30.0f));
        this.addVar(new XuiFloat(this, "itemPadY", 10.0f));
        this.addVar(new XuiFloat(this, "ledBlinkSpeed", 0.0f));
        this.addVar(new XuiFloat(this, "leftMargin", 0.0f));
        this.addVar(new XuiFloat(this, "minValue", 0.0f));
        this.addVar(new XuiInteger(this, "maxLength", 1));
        this.addVar(new XuiInteger(this, "maxLines", 1));
        this.addVar(new XuiFloat(this, "maxValue", 0.0f));
        this.addVar(new XuiFloat(this, "scrollX", 0.0f));
        this.addVar(new XuiFloat(this, "shiftValue", 0.0f));
        this.addVar(new XuiFloat(this, "stepValue", 0.0f));
        this.addVar(new XuiFloat(this, "tabHeight", 0.0f));
        this.addVar(new XuiFloat(this, "tabPadX", 20.0f));
        this.addVar(new XuiFloat(this, "tabTransparency", 1.0f));
        this.addVar(new XuiFloat(this, "textTransparency", 1.0f));
        this.addVar(new XuiFloat(this, "textGap", 4.0f));
        this.addVar(new XuiFloat(this, "triangleWidth", 1.0f));
        this.addVar(new XuiFontType(this, "defaultFont", UIFont.NewSmall));
        this.font = this.addVar(new XuiFontType(this, "font", UIFont.Small));
        this.font2 = this.addVar(new XuiFontType(this, "font2", UIFont.Small));
        this.font3 = this.addVar(new XuiFontType(this, "font3", UIFont.Small));
        this.addVar(new XuiFontType(this, "titleFont", UIFont.Small));
        this.addVar(new XuiTexture(this, "bubblesTex"));
        this.addVar(new XuiTexture(this, "closeButtonTexture"));
        this.addVar(new XuiTexture(this, "collapseButtonTexture"));
        this.addVar(new XuiTexture(this, "gradientTex"));
        this.addVar(new XuiTexture(this, "infoBtn"));
        this.addVar(new XuiTexture(this, "invbasic"));
        this.addVar(new XuiTexture(this, "lcdback"));
        this.addVar(new XuiTexture(this, "lcdfont"));
        this.addVar(new XuiTexture(this, "ledBackTexture"));
        this.addVar(new XuiTexture(this, "ledTexture"));
        this.addVar(new XuiTexture(this, "resizeimage"));
        this.addVar(new XuiTexture(this, "pinButtonTexture"));
        this.addVar(new XuiTexture(this, "progressTexture"));
        this.addVar(new XuiTexture(this, "statusbarbkg"));
        this.texture = this.addVar(new XuiTexture(this, "texture"));
        this.textureBackground = this.addVar(new XuiTexture(this, "textureBackground"));
        this.addVar(new XuiTexture(this, "texBtnLeft"));
        this.addVar(new XuiTexture(this, "texBtnRight"));
        this.textureOverride = this.addVar(new XuiTexture(this, "textureOverride"));
        this.tickTexture = this.addVar(new XuiTexture(this, "tickTexture"));
        this.addVar(new XuiTexture(this, "titlebarbkg"));
        this.addVar(new XuiColor(this, "altBgColor"));
        this.backDropTexCol = this.addVar(new XuiColor(this, "backDropTexCol"));
        this.backgroundColor = this.addVar(new XuiColor(this, "backgroundColor"));
        this.backgroundColorHl = this.addVar(new XuiColor(this, "backgroundColorHL"));
        this.backgroundColorHlInv = this.addVar(new XuiColor(this, "backgroundColorHLInv"));
        this.backgroundColorHlVal = this.addVar(new XuiColor(this, "backgroundColorHLVal"));
        this.backgroundColorMouseOver = this.addVar(new XuiColor(this, "backgroundColorMouseOver"));
        this.backgroundEmpty = this.addVar(new XuiColor(this, "backgroundEmpty"));
        this.backgroundHover = this.addVar(new XuiColor(this, "backgroundHover"));
        this.borderColor = this.addVar(new XuiColor(this, "borderColor"));
        this.borderColorHl = this.addVar(new XuiColor(this, "borderColorHL"));
        this.borderColorHlInv = this.addVar(new XuiColor(this, "borderColorHLInv "));
        this.borderColorHlVal = this.addVar(new XuiColor(this, "borderColorHLVal"));
        this.borderInput = this.addVar(new XuiColor(this, "borderInput"));
        this.borderInvalid = this.addVar(new XuiColor(this, "borderInvalid"));
        this.borderLocked = this.addVar(new XuiColor(this, "borderLocked"));
        this.borderOutput = this.addVar(new XuiColor(this, "borderOutput"));
        this.borderValid = this.addVar(new XuiColor(this, "borderValid"));
        this.addVar(new XuiColor(this, "buttonColor"));
        this.addVar(new XuiColor(this, "buttonMouseOverColor"));
        this.choicesColor = this.addVar(new XuiColor(this, "choicesColor"));
        this.addVar(new XuiColor(this, "detailInnerColor"));
        this.addVar(new XuiColor(this, "greyCol"));
        this.gridColor = this.addVar(new XuiColor(this, "gridColor"));
        this.hsbFactor = this.addVar(new XuiColor(this, "hsbFactor"));
        this.hsbFactor.setAutoApplyMode(XuiAutoApply.No);
        this.addVar(new XuiColor(this, "ledCol"));
        this.addVar(new XuiColor(this, "ledColor"));
        this.addVar(new XuiColor(this, "ledColOff"));
        this.addVar(new XuiColor(this, "ledTextColor"));
        this.addVar(new XuiColor(this, "listHeaderColor"));
        this.addVar(new XuiColor(this, "progressColor"));
        this.addVar(new XuiColor(this, "sliderColor"));
        this.addVar(new XuiColor(this, "sliderBarBorderColor"));
        this.addVar(new XuiColor(this, "sliderBarColor"));
        this.addVar(new XuiColor(this, "sliderBorderColor"));
        this.addVar(new XuiColor(this, "sliderMouseOverColor"));
        this.textColor = this.addVar(new XuiColor(this, "textColor"));
        this.addVar(new XuiColor(this, "textBackColor"));
        this.textureColor = this.addVar(new XuiColor(this, "textureColor"));
        this.addVar(new XuiColor(this, "widgetTextureColor"));
        this.addVar(new XuiBoolean(this, "allowDraggingTabs"));
        this.allowDropAlways = this.addVar(new XuiBoolean(this, "allowDropAlways"));
        this.addVar(new XuiBoolean(this, "allowTornOffTabs"));
        this.addVar(new XuiBoolean(this, "autoScale"));
        this.addVar(new XuiBoolean(this, "autosetheight"));
        this.background = this.addVar(new XuiBoolean(this, "background"));
        this.addVar(new XuiBoolean(this, "center"));
        this.addVar(new XuiBoolean(this, "centerTabs"));
        this.addVar(new XuiBoolean(this, "clearStentil"));
        this.addVar(new XuiBoolean(this, "clip"));
        this.displayBackground = this.addVar(new XuiBoolean(this, "displayBackground"));
        this.doBackDropTex = this.addVar(new XuiBoolean(this, "doBackDropTex"));
        this.doBorderLocked = this.addVar(new XuiBoolean(this, "doBorderLocked"));
        this.addVar(new XuiBoolean(this, "doButtons"));
        this.doHighlight = this.addVar(new XuiBoolean(this, "doHighlight"));
        this.doInvalidHighlight = this.addVar(new XuiBoolean(this, "doInvalidHighlight"));
        this.addVar(new XuiBoolean(this, "doLedBlink"));
        this.addVar(new XuiBoolean(this, "doScroll"));
        this.addVar(new XuiBoolean(this, "doTextBackdrop"));
        this.doToolTip = this.addVar(new XuiBoolean(this, "doToolTip"));
        this.doValidHighlight = this.addVar(new XuiBoolean(this, "doValidHighlight"));
        this.addVar(new XuiBoolean(this, "dragInside"));
        this.addVar(new XuiBoolean(this, "drawFrame"));
        this.drawGrid = this.addVar(new XuiBoolean(this, "drawGrid"));
        this.drawBackground = this.addVar(new XuiBoolean(this, "drawBackground"));
        this.drawBorder = this.addVar(new XuiBoolean(this, "drawBorder"));
        this.addVar(new XuiBoolean(this, "drawMeasures"));
        this.addVar(new XuiBoolean(this, "editable"));
        this.addVar(new XuiBoolean(this, "enable"));
        this.enableHeader = this.addVar(new XuiBoolean(this, "enableHeader"));
        this.addVar(new XuiBoolean(this, "equalTabWidth"));
        this.addVar(new XuiBoolean(this, "keeplog"));
        this.addVar(new XuiBoolean(this, "isOn", false));
        this.addVar(new XuiBoolean(this, "isVertical", false));
        this.addVar(new XuiBoolean(this, "ledIsOn", false));
        this.addVar(new XuiBoolean(this, "left", false));
        this.moveWithMouse = this.addVar(new XuiBoolean(this, "moveWithMouse", false));
        this.mouseEnabled = this.addVar(new XuiBoolean(this, "mouseEnabled"));
        this.mouseOver = this.addVar(new XuiBoolean(this, "mouseover", false));
        this.pin = this.addVar(new XuiBoolean(this, "pin"));
        this.resizable = this.addVar(new XuiBoolean(this, "resizable"));
        this.storeItem = this.addVar(new XuiBoolean(this, "storeItem"));
        this.addVar(new XuiTranslateString(this, "description"));
        this.addVar(new XuiTranslateString(this, "footNote"));
        this.mouseOverText = this.addVar(new XuiTranslateString(this, "mouseovertext"));
        this.name = this.addVar(new XuiTranslateString(this, "name", "NAME_NOT_SET"));
        this.title = this.addVar(new XuiTranslateString(this, "title", "TITLE_NOT_SET"));
        this.tooltip = this.addVar(new XuiTranslateString(this, "tooltip"));
        this.addVar(new XuiTranslateString(this, "toolTipText"));
        this.toolTipTextItem = this.addVar(new XuiTranslateString(this, "toolTipTextItem"));
        this.toolTipTextLocked = this.addVar(new XuiTranslateString(this, "toolTipTextLocked"));
        this.addVar(new XuiTranslateString(this, "translation"));
    }

    public String getXuiUUID() {
        return this.xuiUuid;
    }

    public String getXuiKey() {
        return (String)this.xuiKey.value();
    }

    public XuiScript setXuiKey(String xuiKey) {
        this.xuiKey.setValue(xuiKey);
        return this;
    }

    public String getXuiLuaClass() {
        return (String)this.xuiLuaClass.value();
    }

    public XuiScript setXuiLuaClass(String xuiLuaClass) {
        this.xuiLuaClass.setValue(xuiLuaClass);
        return this;
    }

    public String getXuiStyle() {
        return (String)this.xuiStyle.value();
    }

    public XuiScript setXuiStyle(String xuiStyle) {
        this.xuiStyle.setValue(xuiStyle);
        return this;
    }

    public String getXuiCustomDebug() {
        return (String)this.xuiCustomDebug.value();
    }

    public XuiVector getVector() {
        return this.vector;
    }

    public XuiSpacing getPadding() {
        return this.padding;
    }

    public XuiSpacing getMargin() {
        return this.margin;
    }

    public XuiVectorPosAlign getPosAlign() {
        return this.posAlign;
    }

    public XuiFloat getMinimumWidth() {
        return this.minimumWidth;
    }

    public XuiFloat getMinimumHeight() {
        return this.minimumHeight;
    }

    public XuiTranslateString getTitle() {
        return this.title;
    }

    public XuiTranslateString getName() {
        return this.name;
    }

    public XuiFontType getFont() {
        return this.font;
    }

    public XuiFontType getFont2() {
        return this.font2;
    }

    public XuiFontType getFont3() {
        return this.font3;
    }

    public XuiTexture getIcon() {
        return this.icon;
    }

    public XuiVector getIconVector() {
        return this.iconVector;
    }

    public XuiBoolean getAnchorLeft() {
        return this.anchorLeft;
    }

    public XuiBoolean getAnchorRight() {
        return this.anchorRight;
    }

    public XuiBoolean getAnchorTop() {
        return this.anchorTop;
    }

    public XuiBoolean getAnchorBottom() {
        return this.anchorBottom;
    }

    public XuiStringList getAnimationList() {
        return this.animationList;
    }

    public XuiFloat getAnimationTime() {
        return this.animationTime;
    }

    public XuiTexture getTextureBackground() {
        return this.textureBackground;
    }

    public XuiTexture getTexture() {
        return this.texture;
    }

    public XuiTexture getTextureOverride() {
        return this.textureOverride;
    }

    public XuiTexture getTickTexture() {
        return this.tickTexture;
    }

    public XuiColor getTextColor() {
        return this.textColor;
    }

    public XuiColor getBackgroundColor() {
        return this.backgroundColor;
    }

    public XuiColor getBackgroundColorMouseOver() {
        return this.backgroundColorMouseOver;
    }

    public XuiColor getBorderColor() {
        return this.borderColor;
    }

    public XuiColor getTextureColor() {
        return this.textureColor;
    }

    public XuiColor getChoicesColor() {
        return this.choicesColor;
    }

    public XuiColor getGridColor() {
        return this.gridColor;
    }

    public XuiBoolean getDisplayBackground() {
        return this.displayBackground;
    }

    public XuiBoolean getBackground() {
        return this.background;
    }

    public XuiBoolean getDrawGrid() {
        return this.drawGrid;
    }

    public XuiBoolean getDrawBackground() {
        return this.drawBackground;
    }

    public XuiBoolean getDrawBorder() {
        return this.drawBorder;
    }

    public XuiTranslateString getTooltip() {
        return this.tooltip;
    }

    public XuiTranslateString getMouseOverText() {
        return this.mouseOverText;
    }

    public XuiColor getHsbFactor() {
        return this.hsbFactor;
    }

    public XuiBoolean getMoveWithMouse() {
        return this.moveWithMouse;
    }

    public XuiTextAlign getTextAlign() {
        return this.textAlign;
    }

    public XuiBoolean getDoHighlight() {
        return this.doHighlight;
    }

    public XuiColor getBackgroundColorHL() {
        return this.backgroundColorHl;
    }

    public XuiColor getBorderColorHL() {
        return this.borderColorHl;
    }

    public XuiBoolean getDoValidHighlight() {
        return this.doValidHighlight;
    }

    public XuiColor getBackgroundColorHLVal() {
        return this.backgroundColorHlVal;
    }

    public XuiColor getBorderColorHLVal() {
        return this.borderColorHlVal;
    }

    public XuiBoolean getDoInvalidHighlight() {
        return this.doInvalidHighlight;
    }

    public XuiColor getBackgroundColorHLInv() {
        return this.backgroundColorHlInv;
    }

    public XuiColor getBorderColorHLInv() {
        return this.borderColorHlInv;
    }

    public XuiBoolean getStoreItem() {
        return this.storeItem;
    }

    public XuiBoolean getDoBackDropTex() {
        return this.doBackDropTex;
    }

    public XuiColor getBackDropTexCol() {
        return this.backDropTexCol;
    }

    public XuiBoolean getDoToolTip() {
        return this.doToolTip;
    }

    public XuiBoolean getMouseEnabled() {
        return this.mouseEnabled;
    }

    public XuiBoolean getAllowDropAlways() {
        return this.allowDropAlways;
    }

    public XuiTranslateString getToolTipTextItem() {
        return this.toolTipTextItem;
    }

    public XuiTranslateString getToolTipTextLocked() {
        return this.toolTipTextLocked;
    }

    public XuiColor getBackgroundEmpty() {
        return this.backgroundEmpty;
    }

    public XuiColor getBackgroundHover() {
        return this.backgroundHover;
    }

    public XuiColor getBorderInput() {
        return this.borderInput;
    }

    public XuiColor getBorderOutput() {
        return this.borderOutput;
    }

    public XuiColor getBorderValid() {
        return this.borderValid;
    }

    public XuiColor getBorderInvalid() {
        return this.borderInvalid;
    }

    public XuiColor getBorderLocked() {
        return this.borderLocked;
    }

    public XuiBoolean getDoBorderLocked() {
        return this.doBorderLocked;
    }

    public String getXuiLayoutName() {
        if (this.xuiLayoutName != null) {
            return this.xuiLayoutName;
        }
        return "null";
    }

    public String toString() {
        String orig = super.toString();
        String config = this.getXuiLayoutName();
        String type = this.scriptType != null ? this.scriptType.toString() : "null";
        String luaClass = this.xuiLuaClass != null && this.xuiLuaClass.value() != null ? (String)this.xuiLuaClass.value() : "null";
        String key = this.xuiKey != null && this.xuiKey.value() != null ? (String)this.xuiKey.value() : "null";
        return "XuiScript [config=" + config + ", type=" + type + ", class=" + luaClass + ", key=" + key + ", u=" + orig + "]";
    }

    protected void logWithInfo(String s) {
        DebugLog.General.debugln(s);
        this.logInfo();
    }

    protected void warnWithInfo(String s) {
        DebugLog.General.debugln(s);
        this.logInfo();
    }

    protected void errorWithInfo(String s) {
        DebugLog.General.error(s);
        this.logInfo();
    }

    private void logInfo() {
        DebugLog.log(this.toString());
    }

    public XuiScript getStyle() {
        return this.style;
    }

    public void setStyle(XuiScript style) {
        if (style != null && !style.isStyle()) {
            this.errorWithInfo("XuiScript is not a style.");
            DebugLog.log("StyleScript = " + String.valueOf(style));
            return;
        }
        if (style != null && this.style == style) {
            return;
        }
        this.style = style;
        for (XuiVar<?, ?> var : this.vars) {
            if (style != null) {
                var.style = style.getVar(var.getScriptKey());
                continue;
            }
            var.style = null;
        }
    }

    public XuiScript getDefaultStyle() {
        return this.defaultStyle;
    }

    public void setDefaultStyle(XuiScript defaultStyle) {
        if (defaultStyle != null && !defaultStyle.isDefaultStyle()) {
            this.errorWithInfo("XuiScript is not style.");
            DebugLog.log("StyleScript = " + String.valueOf(defaultStyle));
            return;
        }
        if (defaultStyle != null && this.defaultStyle == defaultStyle) {
            return;
        }
        this.defaultStyle = defaultStyle;
        for (XuiVar<?, ?> var : this.vars) {
            if (defaultStyle != null) {
                var.defaultStyle = defaultStyle.getVar(var.getScriptKey());
                continue;
            }
            var.defaultStyle = null;
        }
    }

    public boolean isLayout() {
        return this.scriptType == XuiScriptType.Layout;
    }

    public boolean isAnyStyle() {
        return this.scriptType == XuiScriptType.Style || this.scriptType == XuiScriptType.DefaultStyle;
    }

    public boolean isStyle() {
        return this.scriptType == XuiScriptType.Style;
    }

    public boolean isDefaultStyle() {
        return this.scriptType == XuiScriptType.DefaultStyle;
    }

    public XuiScriptType getScriptType() {
        return this.scriptType;
    }

    protected <T extends XuiVar<?, ?>> T addVar(T var) {
        if (this.varsMap.containsKey(var.getScriptKey())) {
            this.logInfo();
            throw new RuntimeException("Double script key");
        }
        this.vars.add(var);
        this.varsMap.put(var.getScriptKey(), var);
        return var;
    }

    public XuiVar<?, ?> getVar(String key) {
        return this.varsMap.get(key);
    }

    public ArrayList<XuiVar<?, ?>> getVars() {
        return this.vars;
    }

    public void addChild(XuiScript child) {
        this.children.add(child);
    }

    public ArrayList<XuiScript> getChildren() {
        return this.children;
    }

    public static String ReadLuaClassValue(ScriptParser.Block block) {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (key.isEmpty() || val.isEmpty() || !key.equalsIgnoreCase("xuiLuaClass")) continue;
            return val;
        }
        return null;
    }

    public static XuiScript CreateScriptForClass(String xuiLayoutName, String luaClass, boolean readAltKeys, XuiScriptType scriptType) {
        if (luaClass != null) {
            switch (luaClass) {
                case "ISXuiTableLayout": {
                    return new XuiTableScript(xuiLayoutName, readAltKeys, scriptType);
                }
                case "Reference": {
                    return new XuiReference(xuiLayoutName, readAltKeys);
                }
            }
            return new XuiScript(xuiLayoutName, readAltKeys, luaClass, scriptType);
        }
        return new XuiScript(xuiLayoutName, readAltKeys, luaClass, scriptType);
    }

    public void Load(ScriptParser.Block block) {
        String val;
        String key;
        if (this.isLayout()) {
            for (ScriptParser.Value value : block.values) {
                key = value.getKey().trim();
                val = value.getValue().trim();
                if (key.isEmpty() || val.isEmpty()) continue;
                if (this.xuiStyle.acceptsKey(key)) {
                    this.xuiStyle.fromString(val);
                    continue;
                }
                if (!this.xuiLuaClass.acceptsKey(key)) continue;
                if (!this.xuiLuaClass.isValueSet()) {
                    this.xuiLuaClass.fromString(val);
                    continue;
                }
                this.warnWithInfo("LuaClass defined in script but already set in constructor, class: " + (String)this.xuiLuaClass.value());
            }
            XuiScript style = XuiManager.GetStyle((String)this.xuiStyle.value());
            if (style != null) {
                this.setStyle(style);
            }
            this.tryToSetDefaultStyle();
        }
        for (ScriptParser.Value value : block.values) {
            key = value.getKey().trim();
            val = value.getValue().trim();
            if (key.isEmpty() || val.isEmpty()) continue;
            this.loadVar(key, val);
        }
        for (ScriptParser.Block child : block.children) {
            if (!this.isLayout() || !child.type.equalsIgnoreCase("xui")) continue;
            XuiScript script = XuiScript.CreateScriptForClass(this.xuiLayoutName, child.id, this.readAltKeys, this.scriptType);
            script.Load(child);
            this.children.add(script);
        }
        this.postLoad();
    }

    public boolean loadVar(String key, String val) {
        return this.loadVar(key, val, true);
    }

    public boolean loadVar(String key, String val, boolean allowNull) {
        for (int i = 0; i < this.vars.size(); ++i) {
            if (!this.vars.get(i).isScriptLoadEnabled()) continue;
            if (val != null && this.vars.get(i).acceptsKey(key)) {
                return this.vars.get(i).load(key, val);
            }
            if (val != null || !allowNull || !this.vars.get(i).acceptsKey(key)) continue;
            this.vars.get(i).setValue(null);
            return true;
        }
        return false;
    }

    protected void tryToSetDefaultStyle() {
        XuiScript def;
        if (this.isLayout() && this.xuiLuaClass.value() != null && (def = XuiManager.GetDefaultStyle((String)this.xuiLuaClass.value())) != null) {
            this.setDefaultStyle(def);
        }
    }

    protected void postLoad() {
        if (this.xuiLuaClass.value() != null) {
            if (this.backgroundColor.valueSet) {
                if ((((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISPanel") || ((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISCollapsableWindow") || ((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISCollapsableWindowJoypad")) && !this.background.valueSet) {
                    this.background.setValue(true);
                }
                if ((((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISXuiTableLayout") || ((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISXuiTableLayoutCell") || ((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISXuiTableLayoutRow") || ((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISXuiTableLayoutColumn")) && !this.drawBackground.valueSet) {
                    this.drawBackground.setValue(true);
                }
            }
            if (this.borderColor.valueSet) {
                if (((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISPanel") && !this.background.valueSet) {
                    this.background.setValue(true);
                }
                if ((((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISXuiTableLayout") || ((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISXuiTableLayoutCell") || ((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISXuiTableLayoutRow") || ((String)this.xuiLuaClass.value()).equalsIgnoreCase("ISXuiTableLayoutColumn")) && !this.drawBorder.valueSet) {
                    this.drawBorder.setValue(true);
                }
            }
        }
    }

    @UsedFromLua
    public static class XuiString
    extends XuiVar<String, XuiString> {
        protected XuiString(XuiScript parent, String key) {
            super(XuiVarType.String, parent, key);
        }

        protected XuiString(XuiScript parent, String key, String defaultVal) {
            super(XuiVarType.String, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            this.setValue(val);
        }
    }

    @UsedFromLua
    public static abstract class XuiVar<T, C extends XuiVar<?, ?>> {
        private int uiOrder = 1000;
        protected final XuiVarType type;
        protected final XuiScript parent;
        protected XuiVar<T, C> style;
        protected XuiVar<T, C> defaultStyle;
        protected boolean valueSet;
        private boolean scriptLoadEnabled = true;
        protected XuiAutoApply autoApply = XuiAutoApply.IfSet;
        protected T defaultValue;
        protected T value;
        protected final String luaTableKey;
        private boolean ignoreStyling;

        protected XuiVar(XuiVarType type, XuiScript parent, String key) {
            this(type, parent, key, null);
        }

        protected XuiVar(XuiVarType type, XuiScript parent, String key, T defaultVal) {
            this.type = Objects.requireNonNull(type);
            this.parent = Objects.requireNonNull(parent);
            this.luaTableKey = Objects.requireNonNull(key);
            this.defaultValue = defaultVal;
        }

        public XuiVarType getType() {
            return this.type;
        }

        public int setUiOrder(int order) {
            this.uiOrder = order;
            return this.uiOrder;
        }

        public int getUiOrder() {
            return this.uiOrder;
        }

        public XuiVar<T, C> getStyle() {
            return this.style;
        }

        public XuiVar<T, C> getDefaultStyle() {
            return this.defaultStyle;
        }

        public boolean isStyle() {
            return this.parent.isAnyStyle();
        }

        public void setScriptLoadEnabled(boolean b) {
            this.scriptLoadEnabled = b;
        }

        public boolean isScriptLoadEnabled() {
            return this.scriptLoadEnabled;
        }

        protected void setIgnoreStyling(boolean b) {
            this.ignoreStyling = b;
        }

        public boolean isIgnoreStyling() {
            return this.ignoreStyling;
        }

        protected void setDefaultValue(T value) {
            this.defaultValue = value;
        }

        protected T getDefaultValue() {
            return this.defaultValue;
        }

        public void setValue(T value) {
            this.value = value;
            this.valueSet = true;
        }

        public void setAutoApplyMode(XuiAutoApply autoApplyMode) {
            this.autoApply = autoApplyMode;
        }

        public XuiAutoApply getAutoApplyMode() {
            return this.autoApply;
        }

        public String getLuaTableKey() {
            return this.luaTableKey;
        }

        protected String getScriptKey() {
            return this.luaTableKey;
        }

        public boolean isValueSet() {
            if (this.parent.isLayout() && !this.valueSet && !this.ignoreStyling) {
                if (this.style != null && this.style.isValueSet()) {
                    return true;
                }
                if (this.defaultStyle != null && this.defaultStyle.isValueSet()) {
                    return true;
                }
            }
            return this.valueSet;
        }

        public XuiScriptType getValueType() {
            if (this.parent.isLayout() && !this.valueSet && !this.ignoreStyling) {
                if (this.style != null && this.style.isValueSet()) {
                    return XuiScriptType.Style;
                }
                if (this.defaultStyle != null && this.defaultStyle.isValueSet()) {
                    return XuiScriptType.DefaultStyle;
                }
            }
            return this.parent.getScriptType();
        }

        public T value() {
            if (this.parent.isLayout() && !this.valueSet && !this.ignoreStyling) {
                if (this.style != null && this.style.isValueSet()) {
                    return this.style.value();
                }
                if (this.defaultStyle != null && this.defaultStyle.isValueSet()) {
                    return this.defaultStyle.value();
                }
            }
            return this.valueSet ? this.value : this.defaultValue;
        }

        public String getValueString() {
            return this.value() != null ? this.value().toString() : "null";
        }

        protected boolean acceptsKey(String key) {
            return this.luaTableKey.equalsIgnoreCase(key);
        }

        protected abstract void fromString(String var1);

        protected boolean load(String key, String val) {
            if (this.acceptsKey(key)) {
                this.fromString(val);
                return true;
            }
            return false;
        }
    }

    @UsedFromLua
    public static class XuiUnit
    extends XuiVar<Float, XuiUnit> {
        protected boolean isPercent;

        protected XuiUnit(XuiScript parent, String key) {
            super(XuiVarType.Unit, parent, key, Float.valueOf(0.0f));
        }

        protected XuiUnit(XuiScript parent, String key, float defaultVal) {
            super(XuiVarType.Unit, parent, key, Float.valueOf(defaultVal));
        }

        @Override
        protected void fromString(String val) {
            try {
                this.isPercent = this.isPercent(val);
                this.setValue(Float.valueOf(this.getNum(val)));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void setValue(float val, boolean isPercent) {
            this.isPercent = isPercent;
            this.setValue(Float.valueOf(val));
        }

        public boolean isPercent() {
            if (this.parent.isLayout() && !this.valueSet && !this.isIgnoreStyling()) {
                if (this.style != null && this.style.isValueSet()) {
                    return ((XuiUnit)this.style).isPercent;
                }
                if (this.defaultStyle != null && this.defaultStyle.isValueSet()) {
                    return ((XuiUnit)this.defaultStyle).isPercent;
                }
            }
            return this.isPercent;
        }

        private boolean isPercent(String s) {
            return s.endsWith("%");
        }

        private float getNum(String s) {
            try {
                boolean isPercent = this.isPercent(s);
                String ss = isPercent ? s.substring(0, s.length() - 1) : s;
                ss = ss.trim();
                float f = Float.parseFloat(ss);
                if (isPercent) {
                    f /= 100.0f;
                }
                return f;
            }
            catch (Exception e) {
                this.parent.logInfo();
                e.printStackTrace();
                return 0.0f;
            }
        }

        @Override
        public String getValueString() {
            return String.valueOf(this.value() != null ? (Serializable)this.value() : "null") + (this.isPercent() ? "%" : "");
        }
    }

    @UsedFromLua
    public static class XuiVector
    extends XuiVar<Float, XuiVector> {
        private final XuiUnit x;
        private final XuiUnit y;
        private final XuiUnit w;
        private final XuiUnit h;

        public XuiVector(XuiScript parent, String key, XuiUnit x, XuiUnit y, XuiUnit w, XuiUnit h) {
            super(XuiVarType.Vector, parent, key, Float.valueOf(0.0f));
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.setIgnoreStyling(true);
        }

        @Override
        protected void fromString(String val) {
            throw new RuntimeException("Not implemented for UIVector!");
        }

        @Override
        protected boolean load(String key, String val) {
            try {
                if (this.acceptsKey(key)) {
                    String[] split = val.split(":");
                    block8: for (int i = 0; i < split.length; ++i) {
                        String s = split[i].trim();
                        switch (i) {
                            case 0: {
                                this.x.fromString(s);
                                continue block8;
                            }
                            case 1: {
                                this.y.fromString(s);
                                continue block8;
                            }
                            case 2: {
                                this.w.fromString(s);
                                continue block8;
                            }
                            case 3: {
                                this.h.fromString(s);
                            }
                        }
                    }
                    return true;
                }
            }
            catch (Exception e) {
                this.parent.logInfo();
                e.printStackTrace();
            }
            return false;
        }

        public float getX() {
            return ((Float)this.x.value()).floatValue();
        }

        public float getY() {
            return ((Float)this.y.value()).floatValue();
        }

        public float getWidth() {
            return ((Float)this.w.value()).floatValue();
        }

        public float getHeight() {
            return ((Float)this.h.value()).floatValue();
        }

        public float getW() {
            return ((Float)this.w.value()).floatValue();
        }

        public float getH() {
            return ((Float)this.h.value()).floatValue();
        }

        public boolean isxPercent() {
            return this.x.isPercent();
        }

        public boolean isyPercent() {
            return this.y.isPercent();
        }

        public boolean iswPercent() {
            return this.w.isPercent();
        }

        public boolean ishPercent() {
            return this.h.isPercent();
        }

        @Override
        public boolean isValueSet() {
            return this.x.isValueSet() || this.y.isValueSet() || this.w.isValueSet() || this.h.isValueSet();
        }

        @Override
        public String getValueString() {
            return this.x.getValueString() + ", " + this.y.getValueString() + ", " + this.w.getValueString() + ", " + this.h.getValueString();
        }
    }

    @UsedFromLua
    public static class XuiVectorPosAlign
    extends XuiVar<VectorPosAlign, XuiVectorPosAlign> {
        protected XuiVectorPosAlign(XuiScript parent, String key) {
            super(XuiVarType.VectorPosAlign, parent, key, VectorPosAlign.None);
        }

        protected XuiVectorPosAlign(XuiScript parent, String key, VectorPosAlign defaultVal) {
            super(XuiVarType.VectorPosAlign, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                this.setValue(VectorPosAlign.valueOf(val));
            }
            catch (Exception e) {
                this.parent.logInfo();
                e.printStackTrace();
            }
        }
    }

    @UsedFromLua
    public static class XuiTextAlign
    extends XuiVar<TextAlign, XuiTextAlign> {
        protected XuiTextAlign(XuiScript parent, String key) {
            super(XuiVarType.TextAlign, parent, key, TextAlign.Left);
        }

        protected XuiTextAlign(XuiScript parent, String key, TextAlign defaultVal) {
            super(XuiVarType.TextAlign, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                this.setValue(TextAlign.valueOf(val));
            }
            catch (Exception e) {
                this.parent.logInfo();
                e.printStackTrace();
            }
        }
    }

    @UsedFromLua
    public static class XuiFloat
    extends XuiVar<Float, XuiFloat> {
        protected XuiFloat(XuiScript parent, String key) {
            super(XuiVarType.Float, parent, key, Float.valueOf(0.0f));
        }

        protected XuiFloat(XuiScript parent, String key, float defaultVal) {
            super(XuiVarType.Float, parent, key, Float.valueOf(defaultVal));
        }

        @Override
        protected void fromString(String val) {
            try {
                this.setValue(Float.valueOf(Float.parseFloat(val)));
            }
            catch (Exception e) {
                this.parent.logInfo();
                e.printStackTrace();
            }
        }
    }

    @UsedFromLua
    public static class XuiSpacing
    extends XuiVar<Float, XuiSpacing> {
        private final XuiUnit top;
        private final XuiUnit right;
        private final XuiUnit bottom;
        private final XuiUnit left;

        public XuiSpacing(XuiScript parent, String key, XuiUnit top, XuiUnit right, XuiUnit bottom, XuiUnit left) {
            super(XuiVarType.Vector, parent, key, Float.valueOf(0.0f));
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.left = left;
            this.setIgnoreStyling(true);
            this.setAutoApplyMode(XuiAutoApply.Forbidden);
            top.setAutoApplyMode(XuiAutoApply.No);
            right.setAutoApplyMode(XuiAutoApply.No);
            bottom.setAutoApplyMode(XuiAutoApply.No);
            left.setAutoApplyMode(XuiAutoApply.No);
        }

        @Override
        protected void fromString(String val) {
            throw new RuntimeException("Not implemented for XuiSpacing!");
        }

        @Override
        protected boolean load(String key, String val) {
            try {
                if (this.acceptsKey(key)) {
                    String[] split = val.split(":");
                    block8: for (int i = 0; i < split.length; ++i) {
                        String s = split[i].trim();
                        switch (i) {
                            case 0: {
                                if (split.length == 1) {
                                    this.top.fromString(s);
                                    this.right.fromString(s);
                                    this.bottom.fromString(s);
                                    this.left.fromString(s);
                                    continue block8;
                                }
                                if (split.length == 2) {
                                    this.top.fromString(s);
                                    this.bottom.fromString(s);
                                    continue block8;
                                }
                                this.top.fromString(s);
                                continue block8;
                            }
                            case 1: {
                                if (split.length == 2 || split.length == 3) {
                                    this.right.fromString(s);
                                    this.left.fromString(s);
                                    continue block8;
                                }
                                this.right.fromString(s);
                                continue block8;
                            }
                            case 2: {
                                this.bottom.fromString(s);
                                continue block8;
                            }
                            case 3: {
                                this.left.fromString(s);
                            }
                        }
                    }
                    return true;
                }
            }
            catch (Exception e) {
                this.parent.logInfo();
                e.printStackTrace();
            }
            return false;
        }

        public float getTop() {
            return ((Float)this.top.value()).floatValue();
        }

        public float getRight() {
            return ((Float)this.right.value()).floatValue();
        }

        public float getBottom() {
            return ((Float)this.bottom.value()).floatValue();
        }

        public float getLeft() {
            return ((Float)this.left.value()).floatValue();
        }

        public boolean isTopPercent() {
            return this.top.isPercent();
        }

        public boolean isRightPercent() {
            return this.right.isPercent();
        }

        public boolean isBottomPercent() {
            return this.bottom.isPercent();
        }

        public boolean isLeftPercent() {
            return this.left.isPercent();
        }

        @Override
        public boolean isValueSet() {
            return this.top.isValueSet() || this.right.isValueSet() || this.bottom.isValueSet() || this.left.isValueSet();
        }

        @Override
        public String getValueString() {
            return this.top.getValueString() + ", " + this.right.getValueString() + ", " + this.bottom.getValueString() + ", " + this.left.getValueString();
        }
    }

    @UsedFromLua
    public static class XuiTexture
    extends XuiVar<String, XuiTexture> {
        protected XuiTexture(XuiScript parent, String key) {
            super(XuiVarType.Texture, parent, key);
        }

        protected XuiTexture(XuiScript parent, String key, String defaultVal) {
            super(XuiVarType.Texture, parent, key, defaultVal);
        }

        public Texture getTexture() {
            if (this.value() != null) {
                Texture tex = Texture.getSharedTexture((String)this.value());
                if (tex != null) {
                    return tex;
                }
                if (Core.debug) {
                    DebugLog.General.warn("Could not find texture for: " + (String)this.value());
                }
            }
            return null;
        }

        @Override
        protected void fromString(String val) {
            this.setValue(val);
        }
    }

    @UsedFromLua
    public static class XuiBoolean
    extends XuiVar<Boolean, XuiBoolean> {
        protected XuiBoolean(XuiScript parent, String key) {
            super(XuiVarType.Boolean, parent, key, false);
        }

        protected XuiBoolean(XuiScript parent, String key, boolean defaultVal) {
            super(XuiVarType.Boolean, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                this.setValue(Boolean.parseBoolean(val));
            }
            catch (Exception e) {
                this.parent.logInfo();
                e.printStackTrace();
            }
        }
    }

    @UsedFromLua
    public static class XuiStringList
    extends XuiVar<ArrayList<String>, XuiStringList> {
        protected XuiStringList(XuiScript parent, String key) {
            super(XuiVarType.StringList, parent, key, new ArrayList());
        }

        protected XuiStringList(XuiScript parent, String key, ArrayList<String> defaultVal) {
            super(XuiVarType.StringList, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                String[] split = val.split(":");
                ArrayList<String> list = new ArrayList<String>(split.length);
                for (int i = 0; i < split.length; ++i) {
                    list.add(split[i].trim());
                }
                this.setValue(list);
            }
            catch (Exception e) {
                this.parent.logInfo();
                e.printStackTrace();
            }
        }
    }

    @UsedFromLua
    public static class XuiInteger
    extends XuiVar<Integer, XuiInteger> {
        protected XuiInteger(XuiScript parent, String key) {
            super(XuiVarType.Integer, parent, key, 0);
        }

        protected XuiInteger(XuiScript parent, String key, int defaultVal) {
            super(XuiVarType.Integer, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                this.setValue(Integer.parseInt(val));
            }
            catch (Exception e) {
                this.parent.logInfo();
                e.printStackTrace();
            }
        }
    }

    @UsedFromLua
    public static class XuiFontType
    extends XuiVar<UIFont, XuiFontType> {
        protected XuiFontType(XuiScript parent, String key) {
            super(XuiVarType.FontType, parent, key, UIFont.Small);
        }

        protected XuiFontType(XuiScript parent, String key, UIFont defaultVal) {
            super(XuiVarType.FontType, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                if (val.startsWith("UIFont.")) {
                    val = val.substring(val.indexOf(".") + 1);
                }
                this.setValue(UIFont.valueOf(val));
            }
            catch (Exception e) {
                this.parent.logInfo();
                e.printStackTrace();
            }
        }
    }

    @UsedFromLua
    public static class XuiColor
    extends XuiVar<Color, XuiColor> {
        protected XuiColor(XuiScript parent, String key) {
            super(XuiVarType.Color, parent, key);
        }

        protected XuiColor(XuiScript parent, String key, Color defaultVal) {
            super(XuiVarType.Color, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                Color color = null;
                if (this.parent.xuiSkin != null) {
                    color = this.parent.xuiSkin.color(val);
                }
                if (color == null) {
                    color = Colors.GetColorByName(val);
                }
                if (color == null && val.contains(":")) {
                    color = new Color();
                    String[] split = val.split(":");
                    if (split.length < 3) {
                        this.parent.errorWithInfo("Warning color has <3 values. color: " + val);
                    }
                    if (split.length > 1 && split[0].trim().equalsIgnoreCase("rgb")) {
                        block14: for (int i = 1; i < split.length; ++i) {
                            switch (i) {
                                case 1: {
                                    color.r = Float.parseFloat(split[i].trim()) / 255.0f;
                                    continue block14;
                                }
                                case 2: {
                                    color.g = Float.parseFloat(split[i].trim()) / 255.0f;
                                    continue block14;
                                }
                                case 3: {
                                    color.b = Float.parseFloat(split[i].trim()) / 255.0f;
                                    continue block14;
                                }
                                case 4: {
                                    color.a = Float.parseFloat(split[i].trim()) / 255.0f;
                                }
                            }
                        }
                    } else {
                        block15: for (int i = 0; i < split.length; ++i) {
                            switch (i) {
                                case 0: {
                                    color.r = Float.parseFloat(split[i].trim());
                                    continue block15;
                                }
                                case 1: {
                                    color.g = Float.parseFloat(split[i].trim());
                                    continue block15;
                                }
                                case 2: {
                                    color.b = Float.parseFloat(split[i].trim());
                                    continue block15;
                                }
                                case 3: {
                                    color.a = Float.parseFloat(split[i].trim());
                                }
                            }
                        }
                    }
                }
                if (color == null) {
                    throw new Exception("Could not read color: " + val);
                }
                this.setValue(color);
            }
            catch (Exception e) {
                this.parent.logInfo();
                e.printStackTrace();
            }
        }

        public float getR() {
            return this.value() != null ? ((Color)this.value()).r : 1.0f;
        }

        public float getG() {
            return this.value() != null ? ((Color)this.value()).g : 1.0f;
        }

        public float getB() {
            return this.value() != null ? ((Color)this.value()).b : 1.0f;
        }

        public float getA() {
            return this.value() != null ? ((Color)this.value()).a : 1.0f;
        }

        @Override
        public String getValueString() {
            return this.getR() + ", " + this.getG() + ", " + this.getB() + ", " + this.getA();
        }
    }

    @UsedFromLua
    public static class XuiTranslateString
    extends XuiVar<String, XuiTranslateString> {
        protected XuiTranslateString(XuiScript parent, String key) {
            super(XuiVarType.TranslateString, parent, key);
        }

        protected XuiTranslateString(XuiScript parent, String key, String defaultVal) {
            super(XuiVarType.TranslateString, parent, key, defaultVal);
        }

        @Override
        public String value() {
            if (super.value() == null) {
                return null;
            }
            return Translator.getText((String)super.value());
        }

        @Override
        protected void fromString(String val) {
            this.setValue(val);
        }

        @Override
        public String getValueString() {
            return super.value() != null ? (String)super.value() : "null";
        }
    }

    @UsedFromLua
    public static class XuiDouble
    extends XuiVar<Double, XuiDouble> {
        protected XuiDouble(XuiScript parent, String key) {
            super(XuiVarType.Double, parent, key, 0.0);
        }

        protected XuiDouble(XuiScript parent, String key, double defaultVal) {
            super(XuiVarType.Double, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                this.setValue(Double.parseDouble(val));
            }
            catch (Exception e) {
                this.parent.logInfo();
                e.printStackTrace();
            }
        }
    }

    @UsedFromLua
    public static class XuiFunction
    extends XuiVar<String, XuiFunction> {
        protected XuiFunction(XuiScript parent, String key) {
            super(XuiVarType.Function, parent, key);
        }

        protected XuiFunction(XuiScript parent, String key, String defaultVal) {
            super(XuiVarType.Function, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            this.setValue(val);
        }
    }
}

