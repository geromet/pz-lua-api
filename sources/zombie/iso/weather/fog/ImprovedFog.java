/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.weather.fog;

import org.joml.Vector2i;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.input.GameKeyboard;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.PlayerCamera;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.fog.FogShader;
import zombie.iso.weather.fog.ImprovedFogDrawer;
import zombie.iso.weather.fx.SteppedUpdateFloat;

@UsedFromLua
public class ImprovedFog {
    public static final int MAX_FOG_Z = 1;
    private static final RectangleIterator rectangleIter = new RectangleIterator();
    private static final Vector2i rectangleMatrixPos = new Vector2i();
    private static IsoChunkMap chunkMap;
    private static int minY;
    private static int maxY;
    private static int minX;
    private static int maxX;
    private static int zLayer;
    private static final Vector2i lastIterPos;
    private static final FogRectangle fogRectangle;
    private static boolean drawingThisLayer;
    private static float zoom;
    private static int playerIndex;
    private static int playerRow;
    private static float screenWidth;
    private static float screenHeight;
    private static float worldOffsetX;
    private static float worldOffsetY;
    private static float topAlphaHeight;
    private static float bottomAlphaHeight;
    private static float secondLayerAlpha;
    private static float scalingX;
    private static float scalingY;
    private static float colorR;
    private static float colorG;
    private static float colorB;
    private static boolean drawDebugColors;
    private static float octaves;
    private static boolean highQuality;
    private static boolean enableEditing;
    private static float alphaCircleAlpha;
    private static float alphaCircleRad;
    private static int lastRow;
    private static ClimateManager climateManager;
    private static Texture noiseTexture;
    private static boolean renderOnlyOneRow;
    private static float baseAlpha;
    private static int renderEveryXRow;
    private static int renderXRowsFromCenter;
    private static boolean renderCurrentLayerOnly;
    private static float rightClickOffX;
    private static float rightClickOffY;
    private static float cameraOffscreenLeft;
    private static float cameraOffscreenTop;
    private static float cameraZoom;
    private static int minXOffset;
    private static int maxXOffset;
    private static int maxYOffset;
    private static boolean renderEndOnly;
    private static final SteppedUpdateFloat fogIntensity;
    private static final ImprovedFogDrawer[][] drawers;
    private static int keyPause;
    private static final float[] offsets;

    public static int getMinXOffset() {
        return minXOffset;
    }

    public static void setMinXOffset(int minXOffset) {
        ImprovedFog.minXOffset = minXOffset;
    }

    public static int getMaxXOffset() {
        return maxXOffset;
    }

    public static void setMaxXOffset(int maxXOffset) {
        ImprovedFog.maxXOffset = maxXOffset;
    }

    public static int getMaxYOffset() {
        return maxYOffset;
    }

    public static void setMaxYOffset(int maxYOffset) {
        ImprovedFog.maxYOffset = maxYOffset;
    }

    public static boolean isRenderEndOnly() {
        return renderEndOnly;
    }

    public static void setRenderEndOnly(boolean renderEndOnly) {
        ImprovedFog.renderEndOnly = renderEndOnly;
    }

    public static float getAlphaCircleAlpha() {
        return alphaCircleAlpha;
    }

    public static void setAlphaCircleAlpha(float alphaCircleAlpha) {
        ImprovedFog.alphaCircleAlpha = alphaCircleAlpha;
    }

    public static float getAlphaCircleRad() {
        return alphaCircleRad;
    }

    public static void setAlphaCircleRad(float alphaCircleRad) {
        ImprovedFog.alphaCircleRad = alphaCircleRad;
    }

    public static boolean isHighQuality() {
        return highQuality;
    }

    public static void setHighQuality(boolean highQuality) {
        ImprovedFog.highQuality = highQuality;
    }

    public static boolean isEnableEditing() {
        return enableEditing;
    }

    public static void setEnableEditing(boolean enableEditing) {
        ImprovedFog.enableEditing = enableEditing;
    }

    public static float getTopAlphaHeight() {
        return topAlphaHeight;
    }

    public static void setTopAlphaHeight(float topAlphaHeight) {
        ImprovedFog.topAlphaHeight = topAlphaHeight;
    }

    public static float getBottomAlphaHeight() {
        return bottomAlphaHeight;
    }

    public static void setBottomAlphaHeight(float bottomAlphaHeight) {
        ImprovedFog.bottomAlphaHeight = bottomAlphaHeight;
    }

    public static boolean isDrawDebugColors() {
        return drawDebugColors;
    }

    public static void setDrawDebugColors(boolean drawDebugColors) {
        ImprovedFog.drawDebugColors = drawDebugColors;
    }

    public static float getOctaves() {
        return octaves;
    }

    public static void setOctaves(float octaves) {
        ImprovedFog.octaves = octaves;
    }

    public static float getColorR() {
        return colorR;
    }

    public static void setColorR(float colorR) {
        ImprovedFog.colorR = colorR;
    }

    public static float getColorG() {
        return colorG;
    }

    public static void setColorG(float colorG) {
        ImprovedFog.colorG = colorG;
    }

    public static float getColorB() {
        return colorB;
    }

    public static void setColorB(float colorB) {
        ImprovedFog.colorB = colorB;
    }

    public static float getSecondLayerAlpha() {
        return secondLayerAlpha;
    }

    public static void setSecondLayerAlpha(float secondLayerAlpha) {
        ImprovedFog.secondLayerAlpha = secondLayerAlpha;
    }

    public static float getScalingX() {
        return scalingX;
    }

    public static void setScalingX(float scalingX) {
        ImprovedFog.scalingX = scalingX;
    }

    public static float getScalingY() {
        return scalingY;
    }

    public static void setScalingY(float scalingY) {
        ImprovedFog.scalingY = scalingY;
    }

    public static boolean isRenderOnlyOneRow() {
        return renderOnlyOneRow;
    }

    public static void setRenderOnlyOneRow(boolean renderOnlyOneRow) {
        ImprovedFog.renderOnlyOneRow = renderOnlyOneRow;
    }

    public static float getBaseAlpha() {
        return baseAlpha;
    }

    public static void setBaseAlpha(float baseAlpha) {
        ImprovedFog.baseAlpha = baseAlpha;
    }

    public static int getRenderEveryXRow() {
        return renderEveryXRow;
    }

    public static void setRenderEveryXRow(int renderEveryXRow) {
        ImprovedFog.renderEveryXRow = renderEveryXRow;
    }

    public static boolean isRenderCurrentLayerOnly() {
        return renderCurrentLayerOnly;
    }

    public static void setRenderCurrentLayerOnly(boolean renderCurrentLayerOnly) {
        ImprovedFog.renderCurrentLayerOnly = renderCurrentLayerOnly;
    }

    public static int getRenderXRowsFromCenter() {
        return renderXRowsFromCenter;
    }

    public static void setRenderXRowsFromCenter(int renderXRowsFromCenter) {
        ImprovedFog.renderXRowsFromCenter = renderXRowsFromCenter;
    }

    public static Texture getNoiseTexture() {
        return noiseTexture;
    }

    public static void update() {
        ImprovedFog.updateKeys();
        if (noiseTexture == null) {
            noiseTexture = Texture.getSharedTexture("media/textures/weather/fognew/fog_noise.png");
        }
        climateManager = ClimateManager.getInstance();
        if (!enableEditing) {
            highQuality = PerformanceSettings.fogQuality == 0;
            fogIntensity.setTarget(climateManager.getFogIntensity());
            fogIntensity.update(GameTime.getInstance().getMultiplier());
            baseAlpha = fogIntensity.value();
            if (highQuality) {
                renderEveryXRow = 1;
                topAlphaHeight = 0.38f;
                bottomAlphaHeight = 0.24f;
                octaves = 6.0f;
                secondLayerAlpha = 0.5f;
            } else {
                renderEveryXRow = 2;
                topAlphaHeight = 0.32f;
                bottomAlphaHeight = 0.32f;
                octaves = 3.0f;
                secondLayerAlpha = 1.0f;
            }
            colorR = ImprovedFog.climateManager.getColorNewFog().getExterior().r;
            colorG = ImprovedFog.climateManager.getColorNewFog().getExterior().g;
            colorB = ImprovedFog.climateManager.getColorNewFog().getExterior().b;
        }
        if (baseAlpha <= 0.0f) {
            scalingX = 0.0f;
            scalingY = 0.0f;
        } else {
            double rad = climateManager.getWindAngleRadians();
            rad -= 2.356194490192345;
            rad = Math.PI - rad;
            float x = (float)Math.cos(rad);
            float y = (float)Math.sin(rad);
            scalingX += x * climateManager.getWindIntensity() * GameTime.getInstance().getMultiplier();
            scalingY += y * climateManager.getWindIntensity() * GameTime.getInstance().getMultiplier();
        }
    }

    public static boolean startRender(int nPlayer, int z) {
        int rows;
        if (!DebugOptions.instance.weather.fog.getValue()) {
            drawingThisLayer = false;
            return false;
        }
        climateManager = ClimateManager.getInstance();
        if (z > 1 || baseAlpha <= 0.0f || PerformanceSettings.fogQuality == 2) {
            drawingThisLayer = false;
            return false;
        }
        drawingThisLayer = true;
        IsoPlayer player = IsoPlayer.players[nPlayer];
        if (renderCurrentLayerOnly && player.getZ() != (float)z) {
            drawingThisLayer = false;
            return false;
        }
        if (player.isInARoom() && z > 0) {
            drawingThisLayer = false;
            return false;
        }
        playerRow = PZMath.fastfloor(player.getX()) + PZMath.fastfloor(player.getY());
        zoom = Core.getInstance().getZoom(nPlayer);
        zLayer = z;
        playerIndex = nPlayer;
        PlayerCamera camera = IsoCamera.cameras[nPlayer];
        screenWidth = IsoCamera.getOffscreenWidth(nPlayer);
        screenHeight = IsoCamera.getOffscreenHeight(nPlayer);
        worldOffsetX = camera.getOffX() - (float)IsoCamera.getOffscreenLeft(playerIndex) * zoom;
        worldOffsetY = camera.getOffY() + (float)IsoCamera.getOffscreenTop(playerIndex) * zoom;
        rightClickOffX = camera.rightClickX + IsoUtils.XToScreen(camera.deferedX, camera.deferedY, 0.0f, 0);
        rightClickOffY = camera.rightClickY + IsoUtils.YToScreen(camera.deferedX, camera.deferedY, 0.0f, 0);
        cameraOffscreenLeft = IsoCamera.getOffscreenLeft(nPlayer);
        cameraOffscreenTop = IsoCamera.getOffscreenTop(nPlayer);
        cameraZoom = zoom;
        if (!enableEditing) {
            if (player.getVehicle() != null) {
                alphaCircleAlpha = 0.0f;
                alphaCircleRad = highQuality ? 2.0f : 2.6f;
            } else if (player.isInARoom()) {
                alphaCircleAlpha = 0.0f;
                alphaCircleRad = highQuality ? 1.25f : 1.5f;
            } else {
                alphaCircleAlpha = highQuality ? 0.1f : 0.16f;
                float f = alphaCircleRad = highQuality ? 2.5f : 3.0f;
                if (climateManager.getWeatherPeriod().isRunning() && (climateManager.getWeatherPeriod().isTropicalStorm() || climateManager.getWeatherPeriod().isThunderStorm())) {
                    alphaCircleRad *= 0.6f;
                }
            }
        }
        boolean x1 = false;
        boolean y1 = false;
        int x2 = 0 + IsoCamera.getOffscreenWidth(nPlayer);
        int y2 = 0 + IsoCamera.getOffscreenHeight(nPlayer);
        float topLeftX = IsoUtils.XToIso(0.0f, 0.0f, zLayer);
        float topLeftY = IsoUtils.YToIso(0.0f, 0.0f, zLayer);
        float bottomRightX = IsoUtils.XToIso(x2, y2, zLayer);
        float bottomRightY = IsoUtils.YToIso(x2, y2, zLayer);
        float bottomLeftY = IsoUtils.YToIso(0.0f, y2, zLayer);
        minY = PZMath.fastfloor(topLeftY);
        maxY = PZMath.fastfloor(bottomRightY);
        minX = PZMath.fastfloor(topLeftX);
        maxX = PZMath.fastfloor(bottomRightX);
        if (IsoPlayer.numPlayers > 1) {
            maxX = Math.max(maxX, IsoWorld.instance.currentCell.getMaxX()) + 20;
            maxY = Math.max(maxY, IsoWorld.instance.currentCell.getMaxY()) + 20;
        }
        int rowlen = rows = (maxX += maxXOffset) - (minX += minXOffset);
        if (minY != (maxY += maxYOffset)) {
            rowlen += PZMath.abs(minY - maxY);
        }
        rectangleIter.reset(rows, rowlen);
        lastRow = Integer.MAX_VALUE;
        ImprovedFog.fogRectangle.hasStarted = false;
        chunkMap = IsoWorld.instance.getCell().getChunkMap(nPlayer);
        return true;
    }

    public static void renderRowsBehind(IsoGridSquare squareMax) {
        if (!drawingThisLayer) {
            return;
        }
        int maxRow = -1;
        if (squareMax != null && (maxRow = squareMax.getX() + squareMax.getY()) < minX + minY) {
            return;
        }
        if (lastRow != Integer.MIN_VALUE && lastRow == maxRow) {
            return;
        }
        Vector2i v = rectangleMatrixPos;
        while (rectangleIter.next(v)) {
            if (v == null) continue;
            int x = v.x + minX;
            int y = v.y + minY;
            int curRow = x + y;
            if (curRow != lastRow) {
                if (!(lastRow == Integer.MIN_VALUE || renderEndOnly && squareMax != null)) {
                    ImprovedFog.endFogRectangle(ImprovedFog.lastIterPos.x, ImprovedFog.lastIterPos.y, zLayer);
                }
                lastRow = curRow;
            }
            IsoGridSquare square = chunkMap.getGridSquare(x, y, zLayer);
            boolean hasFog = true;
            if (square != null && (!square.isExteriorCache || square.isInARoom())) {
                hasFog = false;
            }
            if (hasFog) {
                if (!renderEndOnly || squareMax == null) {
                    ImprovedFog.startFogRectangle(x, y, zLayer);
                }
            } else if (!renderEndOnly || squareMax == null) {
                ImprovedFog.endFogRectangle(ImprovedFog.lastIterPos.x, ImprovedFog.lastIterPos.y, zLayer);
            }
            lastIterPos.set(x, y);
            if (maxRow == -1 || curRow != maxRow) continue;
            break;
        }
    }

    public static void endRender() {
        if (!drawingThisLayer) {
            return;
        }
        ImprovedFog.renderRowsBehind(null);
        if (ImprovedFog.fogRectangle.hasStarted) {
            ImprovedFog.endFogRectangle(ImprovedFog.lastIterPos.x, ImprovedFog.lastIterPos.y, zLayer);
        }
    }

    private static void startFogRectangle(int x, int y, int z) {
        if (!ImprovedFog.fogRectangle.hasStarted) {
            ImprovedFog.fogRectangle.hasStarted = true;
            ImprovedFog.fogRectangle.startX = x;
            ImprovedFog.fogRectangle.startY = y;
            ImprovedFog.fogRectangle.z = z;
        }
    }

    private static void endFogRectangle(int x, int y, int z) {
        if (ImprovedFog.fogRectangle.hasStarted) {
            ImprovedFog.fogRectangle.hasStarted = false;
            ImprovedFog.fogRectangle.endX = x;
            ImprovedFog.fogRectangle.endY = y;
            ImprovedFog.fogRectangle.z = z;
            ImprovedFog.renderFogSegment();
        }
    }

    private static void renderFogSegmentOld() {
        int row = ImprovedFog.fogRectangle.startX + ImprovedFog.fogRectangle.startY;
        int row2 = ImprovedFog.fogRectangle.endX + ImprovedFog.fogRectangle.endY;
        if (Core.debug && row != row2) {
            DebugLog.log("ROWS NOT EQUAL");
        }
        if (renderOnlyOneRow ? row != playerRow : row % renderEveryXRow != 0) {
            return;
        }
        if (Core.debug && renderXRowsFromCenter >= 1 && (row < playerRow - renderXRowsFromCenter || row > playerRow + renderXRowsFromCenter)) {
            return;
        }
        float alpha = baseAlpha;
        FogRectangle f = fogRectangle;
        float sx = IsoUtils.XToScreenExact(f.startX, f.startY, f.z, 0);
        float sy = IsoUtils.YToScreenExact(f.startX, f.startY, f.z, 0);
        float ex = IsoUtils.XToScreenExact(f.endX, f.endY, f.z, 0);
        float ey = IsoUtils.YToScreenExact(f.endX, f.endY, f.z, 0);
        sy -= 80.0f * (float)Core.tileScale;
        float height = 96.0f * (float)Core.tileScale;
        float segmentTileWidth = 6.0f;
        float widthTiles = ((ex += 32.0f * (float)Core.tileScale) - (sx -= 32.0f * (float)Core.tileScale)) / (64.0f * (float)Core.tileScale);
        float offset = (float)f.startX % 6.0f;
        float uvOffset = offset / 6.0f;
        float uvWidth = widthTiles / 6.0f;
        float u0 = uvOffset;
        float v0 = 0.0f;
        float u1 = uvWidth + uvOffset;
        float v1 = 1.0f;
        SpriteRenderer.instance.glEnable(3042);
        IndieGL.glBlendFunc(770, 771);
        if (FogShader.instance.StartShader()) {
            FogShader.instance.setScreenInfo(screenWidth, screenHeight, zoom, zLayer > 0 ? secondLayerAlpha : 1.0f);
            FogShader.instance.setTextureInfo(drawDebugColors ? 1.0f : 0.0f, octaves, alpha, Core.tileScale);
            FogShader.instance.setRectangleInfo((int)sx, (int)sy, (int)(ex - sx), (int)height);
            FogShader.instance.setWorldOffset(worldOffsetX, worldOffsetY, rightClickOffX, rightClickOffY);
            FogShader.instance.setScalingInfo(scalingX, scalingY, zLayer, highQuality ? 0.0f : 1.0f);
            FogShader.instance.setColorInfo(colorR, colorG, colorB, 1.0f);
            FogShader.instance.setParamInfo(topAlphaHeight, bottomAlphaHeight, alphaCircleAlpha, alphaCircleRad);
            FogShader.instance.setCameraInfo(cameraOffscreenLeft, cameraOffscreenTop, cameraZoom, offsets[Math.abs(row) % offsets.length]);
            if (PerformanceSettings.fboRenderChunk) {
                IndieGL.glDepthMask(false);
                IndieGL.enableDepthTest();
                IndieGL.glDepthFunc(513);
                float x = (float)ImprovedFog.fogRectangle.startX + (float)(ImprovedFog.fogRectangle.endX - ImprovedFog.fogRectangle.startX) / 2.0f;
                float y = (float)ImprovedFog.fogRectangle.startY + (float)(ImprovedFog.fogRectangle.endY - ImprovedFog.fogRectangle.startY) / 2.0f;
                int z = ImprovedFog.fogRectangle.z;
                float xm = PZMath.coordmodulof(x, 8) + 0.5f;
                float ym = PZMath.coordmodulof(y, 8) + 0.5f;
                float farDepthZ = IsoSprite.calculateDepth(xm - 1.0f, ym - 1.0f, z);
                float frontDepthZ = IsoSprite.calculateDepth(xm, ym, z);
                float farDepthZ2 = IsoSprite.calculateDepth(xm - 1.0f, ym - 1.0f, z + 1);
                float frontDepthZ2 = IsoSprite.calculateDepth(xm, ym, z + 1);
                float calcDepthZ = farDepthZ - frontDepthZ + frontDepthZ;
                float calcDepthZ2 = farDepthZ2 - frontDepthZ2 + frontDepthZ2;
                float calcDepth = calcDepthZ2 - calcDepthZ + calcDepthZ;
                FogShader.instance.setTargetDepth(calcDepth += IsoDepthHelper.getChunkDepthData((int)PZMath.fastfloor((float)(IsoPlayer.getInstance().getX() / 8.0f)), (int)PZMath.fastfloor((float)(IsoPlayer.getInstance().getY() / 8.0f)), (int)PZMath.fastfloor((float)(x / 8.0f)), (int)PZMath.fastfloor((float)(y / 8.0f)), (int)PZMath.fastfloor((float)((float)z))).depthStart);
            }
            SpriteRenderer.instance.render(noiseTexture, (int)sx, (int)sy, (int)(ex - sx), (int)height, 1.0f, 1.0f, 1.0f, alpha, u0, 0.0f, u1, 0.0f, u1, 1.0f, u0, 1.0f);
            IndieGL.EndShader();
        }
    }

    private static void renderFogSegment() {
        if (!PerformanceSettings.fboRenderChunk) {
            ImprovedFog.renderFogSegmentOld();
            return;
        }
        int row = ImprovedFog.fogRectangle.startX + ImprovedFog.fogRectangle.startY;
        int row2 = ImprovedFog.fogRectangle.endX + ImprovedFog.fogRectangle.endY;
        if (Core.debug && row != row2) {
            DebugLog.log("ROWS NOT EQUAL");
        }
        if (renderOnlyOneRow ? row != playerRow : row % renderEveryXRow != 0) {
            return;
        }
        if (Core.debug && renderXRowsFromCenter >= 1 && (row < playerRow - renderXRowsFromCenter || row > playerRow + renderXRowsFromCenter)) {
            return;
        }
        float alpha = baseAlpha;
        FogRectangle f = fogRectangle;
        float sx = IsoUtils.XToScreenExact(f.startX, f.startY, f.z, 0);
        float sy = IsoUtils.YToScreenExact(f.startX, f.startY, f.z, 0);
        float ex = IsoUtils.XToScreenExact(f.endX, f.endY, f.z, 0);
        float ey = IsoUtils.YToScreenExact(f.endX, f.endY, f.z, 0);
        sy -= 80.0f * (float)Core.tileScale;
        float height = 96.0f * (float)Core.tileScale;
        float segmentTileWidth = 6.0f;
        float widthTiles = ((ex += 32.0f * (float)Core.tileScale) - (sx -= 32.0f * (float)Core.tileScale)) / (64.0f * (float)Core.tileScale);
        float offset = (float)f.startX % 6.0f;
        float uvOffset = offset / 6.0f;
        float uvWidth = widthTiles / 6.0f;
        float u0 = uvOffset;
        float v0 = 0.0f;
        float u1 = uvWidth + uvOffset;
        float v1 = 1.0f;
        float depthBottom = 0.0f;
        float depthTop = 0.0f;
        if (PerformanceSettings.fboRenderChunk) {
            float calcDepth;
            int z = ImprovedFog.fogRectangle.z;
            float x = IsoUtils.XToIso(sx + (ex - sx) / 2.0f, sy + (ey - sy) / 2.0f, z);
            float y = IsoUtils.YToIso(sx + (ex - sx) / 2.0f, sy + (ey - sy) / 2.0f, z);
            float xm = x + 1.5f;
            float ym = y + 1.5f;
            float farDepthZ = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)(xm - 1.0f), (float)(ym - 1.0f), (float)((float)z)).depthStart;
            float frontDepthZ = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)xm, (float)ym, (float)((float)z)).depthStart;
            float farDepthZ2 = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)(xm - 1.0f), (float)(ym - 1.0f), (float)((float)(z + 1))).depthStart;
            float frontDepthZ2 = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)xm, (float)ym, (float)((float)(z + 1))).depthStart;
            float calcDepthZ = farDepthZ - frontDepthZ + frontDepthZ;
            float calcDepthZ2 = farDepthZ2 - frontDepthZ2 + frontDepthZ2;
            depthBottom = calcDepth = calcDepthZ2 - calcDepthZ + calcDepthZ;
            depthTop = depthBottom - (ey - sy) / 96.0f * 0.0028867084f * 0.6f;
        }
        ImprovedFogDrawer drawer = ImprovedFog.getDrawer();
        drawer.addRectangle(sx, sy, ex, sy + height, u0, 0.0f, u1, 1.0f, offsets[Math.abs(row) % offsets.length], depthBottom, depthTop, zLayer > 0 ? secondLayerAlpha : 1.0f, zLayer);
    }

    public static void DrawSubTextureRGBA(Texture tex, double subX, double subY, double subW, double subH, double x, double y, double w, double h, double r, double g, double b, double a) {
        if (tex == null || subW <= 0.0 || subH <= 0.0 || w <= 0.0 || h <= 0.0) {
            return;
        }
        double drawX = x;
        double drawY = y;
        drawX += (double)tex.offsetX;
        if ((drawY += (double)tex.offsetY) + h < 0.0 || drawY > 4096.0) {
            return;
        }
        float fsubX = PZMath.clamp((float)subX, 0.0f, (float)tex.getWidth());
        float fsubY = PZMath.clamp((float)subY, 0.0f, (float)tex.getHeight());
        float fsubW = PZMath.clamp((float)((double)fsubX + subW), 0.0f, (float)tex.getWidth()) - fsubX;
        float fsubH = PZMath.clamp((float)((double)fsubY + subH), 0.0f, (float)tex.getHeight()) - fsubY;
        float u0 = fsubX / (float)tex.getWidth();
        float v0 = fsubY / (float)tex.getHeight();
        float u1 = (fsubX + fsubW) / (float)tex.getWidth();
        float v1 = (fsubY + fsubH) / (float)tex.getHeight();
        float uSpan = tex.getXEnd() - tex.getXStart();
        float vSpan = tex.getYEnd() - tex.getYStart();
        u0 = tex.getXStart() + u0 * uSpan;
        u1 = tex.getXStart() + u1 * uSpan;
        v0 = tex.getYStart() + v0 * vSpan;
        v1 = tex.getYStart() + v1 * vSpan;
        SpriteRenderer.instance.render(tex, (float)drawX, (float)drawY, (float)w, (float)h, (float)r, (float)g, (float)b, (float)a, u0, v0, u1, v0, u1, v1, u0, v1);
    }

    public static void updateKeys() {
        if (!Core.debug) {
            return;
        }
        if (keyPause > 0) {
            --keyPause;
        }
        if (keyPause <= 0 && GameKeyboard.isKeyDown(72)) {
            DebugLog.log("Reloading fog shader...");
            keyPause = 30;
            FogShader.instance.reloadShader();
        }
    }

    public static ImprovedFogDrawer getDrawer() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        int stateIndex = SpriteRenderer.instance.getMainStateIndex();
        if (drawers[playerIndex][stateIndex] == null) {
            ImprovedFog.drawers[playerIndex][stateIndex] = new ImprovedFogDrawer();
        }
        return drawers[playerIndex][stateIndex];
    }

    public static void startFrame(ImprovedFogDrawer drawer) {
        drawer.screenInfo1 = screenWidth;
        drawer.screenInfo2 = screenHeight;
        drawer.screenInfo3 = zoom;
        drawer.screenInfo4 = Float.NaN;
        drawer.textureInfo1 = drawDebugColors ? 1.0f : 0.0f;
        drawer.textureInfo2 = octaves;
        drawer.textureInfo3 = baseAlpha;
        drawer.textureInfo4 = Core.tileScale;
        drawer.worldOffset1 = worldOffsetX;
        drawer.worldOffset2 = worldOffsetY;
        drawer.worldOffset3 = rightClickOffX;
        drawer.worldOffset4 = rightClickOffY;
        drawer.scalingInfo1 = scalingX;
        drawer.scalingInfo2 = scalingY;
        drawer.scalingInfo3 = Float.NaN;
        drawer.scalingInfo4 = highQuality ? 1.0f : 0.0f;
        drawer.colorInfo1 = colorR;
        drawer.colorInfo2 = colorG;
        drawer.colorInfo3 = colorB;
        drawer.colorInfo4 = 1.0f;
        drawer.paramInfo1 = topAlphaHeight;
        drawer.paramInfo2 = bottomAlphaHeight;
        drawer.paramInfo3 = alphaCircleAlpha;
        drawer.paramInfo4 = alphaCircleRad;
        drawer.cameraInfo1 = cameraOffscreenLeft;
        drawer.cameraInfo2 = cameraOffscreenTop;
        drawer.cameraInfo3 = cameraZoom;
        drawer.cameraInfo4 = Float.NaN;
        drawer.alpha = baseAlpha;
    }

    public static void init() {
        climateManager = ClimateManager.getInstance();
        fogIntensity.setTarget(climateManager.getFogIntensity());
        fogIntensity.overrideCurrentValue(climateManager.getFogIntensity());
        baseAlpha = fogIntensity.value();
    }

    static {
        lastIterPos = new Vector2i();
        fogRectangle = new FogRectangle();
        zoom = 1.0f;
        topAlphaHeight = 0.38f;
        bottomAlphaHeight = 0.24f;
        secondLayerAlpha = 0.5f;
        scalingX = 1.0f;
        scalingY = 1.0f;
        colorR = 1.0f;
        colorG = 1.0f;
        colorB = 1.0f;
        octaves = 6.0f;
        highQuality = true;
        alphaCircleAlpha = 0.3f;
        alphaCircleRad = 2.25f;
        lastRow = -1;
        renderEveryXRow = 1;
        minXOffset = -2;
        maxXOffset = 12;
        maxYOffset = -5;
        fogIntensity = new SteppedUpdateFloat(0.0f, 0.005f, 0.0f, 1.0f);
        drawers = new ImprovedFogDrawer[4][3];
        offsets = new float[]{0.3f, 0.8f, 0.0f, 0.6f, 0.3f, 0.1f, 0.5f, 0.9f, 0.2f, 0.0f, 0.7f, 0.1f, 0.4f, 0.2f, 0.5f, 0.3f, 0.8f, 0.4f, 0.9f, 0.5f, 0.8f, 0.4f, 0.7f, 0.2f, 0.0f, 0.6f, 0.1f, 0.6f, 0.9f, 0.7f};
    }

    private static class RectangleIterator {
        private int curX;
        private int curY;
        private int sX;
        private int sY;
        private int rowLen;
        private boolean altRow;
        private int curRow;
        private int rowIndex;
        private int maxRows;

        private RectangleIterator() {
        }

        public void reset(int rows, int rowlen) {
            this.sX = 0;
            this.sY = 0;
            this.curX = 0;
            this.curY = 0;
            this.curRow = 0;
            this.altRow = false;
            this.rowIndex = 0;
            this.rowLen = (int)PZMath.ceil((float)rowlen / 2.0f);
            this.maxRows = rows;
        }

        public boolean next(Vector2i vec) {
            if (this.rowLen <= 0 || this.maxRows <= 0 || this.curRow >= this.maxRows) {
                vec.set(0, 0);
                return false;
            }
            vec.set(this.curX, this.curY);
            ++this.rowIndex;
            if (this.rowIndex == this.rowLen) {
                this.rowLen = this.altRow ? this.rowLen - 1 : this.rowLen + 1;
                this.rowIndex = 0;
                this.sX = this.altRow ? this.sX + 1 : this.sX;
                this.sY = this.altRow ? this.sY : this.sY + 1;
                this.altRow = !this.altRow;
                this.curX = this.sX;
                this.curY = this.sY;
                ++this.curRow;
                return this.curRow != this.maxRows;
            }
            ++this.curX;
            --this.curY;
            return true;
        }
    }

    private static class FogRectangle {
        int startX;
        int startY;
        int endX;
        int endY;
        int z;
        boolean hasStarted;

        private FogRectangle() {
        }
    }
}

