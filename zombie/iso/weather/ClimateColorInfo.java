/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.weather;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.Color;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.WeatherPeriod;

@UsedFromLua
public class ClimateColorInfo {
    private final Color interior = new Color(0, 0, 0, 1);
    private final Color exterior = new Color(0, 0, 0, 1);
    private static BufferedWriter writer;

    public ClimateColorInfo() {
    }

    public ClimateColorInfo(float r, float g, float b, float a) {
        this(r, g, b, a, r, g, b, a);
    }

    public ClimateColorInfo(float r, float g, float b, float a, float r2, float g2, float b2, float a2) {
        this.interior.r = r;
        this.interior.g = g;
        this.interior.b = b;
        this.interior.a = a;
        this.exterior.r = r2;
        this.exterior.g = g2;
        this.exterior.b = b2;
        this.exterior.a = a2;
    }

    public void setInterior(Color other) {
        this.interior.set(other);
    }

    public void setInterior(float r, float g, float b, float a) {
        this.interior.r = r;
        this.interior.g = g;
        this.interior.b = b;
        this.interior.a = a;
    }

    public Color getInterior() {
        return this.interior;
    }

    public void setExterior(Color other) {
        this.exterior.set(other);
    }

    public void setExterior(float r, float g, float b, float a) {
        this.exterior.r = r;
        this.exterior.g = g;
        this.exterior.b = b;
        this.exterior.a = a;
    }

    public Color getExterior() {
        return this.exterior;
    }

    public void setTo(ClimateColorInfo other) {
        this.interior.set(other.interior);
        this.exterior.set(other.exterior);
    }

    public ClimateColorInfo interp(ClimateColorInfo to, float t, ClimateColorInfo result) {
        this.interior.interp(to.interior, t, result.interior);
        this.exterior.interp(to.exterior, t, result.exterior);
        return result;
    }

    public void scale(float val) {
        this.interior.scale(val);
        this.exterior.scale(val);
    }

    public static ClimateColorInfo interp(ClimateColorInfo source2, ClimateColorInfo target, float t, ClimateColorInfo resultColorInfo) {
        return source2.interp(target, t, resultColorInfo);
    }

    public void write(ByteBufferWriter output) {
        output.putFloat(this.interior.r);
        output.putFloat(this.interior.g);
        output.putFloat(this.interior.b);
        output.putFloat(this.interior.a);
        output.putFloat(this.exterior.r);
        output.putFloat(this.exterior.g);
        output.putFloat(this.exterior.b);
        output.putFloat(this.exterior.a);
    }

    public void read(ByteBufferReader input) {
        this.interior.r = input.getFloat();
        this.interior.g = input.getFloat();
        this.interior.b = input.getFloat();
        this.interior.a = input.getFloat();
        this.exterior.r = input.getFloat();
        this.exterior.g = input.getFloat();
        this.exterior.b = input.getFloat();
        this.exterior.a = input.getFloat();
    }

    public void save(DataOutputStream output) throws IOException {
        output.writeFloat(this.interior.r);
        output.writeFloat(this.interior.g);
        output.writeFloat(this.interior.b);
        output.writeFloat(this.interior.a);
        output.writeFloat(this.exterior.r);
        output.writeFloat(this.exterior.g);
        output.writeFloat(this.exterior.b);
        output.writeFloat(this.exterior.a);
    }

    public void load(DataInputStream input, int worldVersion) throws IOException {
        this.interior.r = input.readFloat();
        this.interior.g = input.readFloat();
        this.interior.b = input.readFloat();
        this.interior.a = input.readFloat();
        this.exterior.r = input.readFloat();
        this.exterior.g = input.readFloat();
        this.exterior.b = input.readFloat();
        this.exterior.a = input.readFloat();
    }

    public static boolean writeColorInfoConfig() {
        boolean success = false;
        try {
            String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String saveFile = "ClimateMain_" + stamp;
            String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + saveFile + ".lua";
            DebugLog.log("Attempting to save color config to: " + fileName);
            File f = new File(fileName);
            try (BufferedWriter w = new BufferedWriter(new FileWriter(f, false));){
                writer = w;
                ClimateManager clim = ClimateManager.getInstance();
                ClimateColorInfo.write("--[[");
                ClimateColorInfo.write("-- Generated file (" + saveFile + ")");
                ClimateColorInfo.write("-- Climate color configuration");
                ClimateColorInfo.write("-- File should be placed in: media/lua/server/Climate/ClimateMain.lua (remove date stamp)");
                ClimateColorInfo.write("--]]");
                w.newLine();
                ClimateColorInfo.write("ClimateMain = {};");
                ClimateColorInfo.write("ClimateMain.versionStamp = \"" + stamp + "\";");
                w.newLine();
                ClimateColorInfo.write("local WARM,NORMAL,CLOUDY = 0,1,2;");
                w.newLine();
                ClimateColorInfo.write("local SUMMER,FALL,WINTER,SPRING = 0,1,2,3;");
                w.newLine();
                ClimateColorInfo.write("function ClimateMain.onClimateManagerInit(_clim)");
                boolean tabs = true;
                ClimateColorInfo.write(1, "local c;");
                ClimateColorInfo.write(1, "c = _clim:getColNightNoMoon();");
                ClimateColorInfo.writeColor(1, clim.getColNightNoMoon());
                w.newLine();
                ClimateColorInfo.write(1, "c = _clim:getColNightMoon();");
                ClimateColorInfo.writeColor(1, clim.getColNightMoon());
                w.newLine();
                ClimateColorInfo.write(1, "c = _clim:getColFog();");
                ClimateColorInfo.writeColor(1, clim.getColFog());
                w.newLine();
                ClimateColorInfo.write(1, "c = _clim:getColFogLegacy();");
                ClimateColorInfo.writeColor(1, clim.getColFogLegacy());
                w.newLine();
                ClimateColorInfo.write(1, "c = _clim:getColFogNew();");
                ClimateColorInfo.writeColor(1, clim.getColFogNew());
                w.newLine();
                ClimateColorInfo.write(1, "c = _clim:getFogTintStorm();");
                ClimateColorInfo.writeColor(1, clim.getFogTintStorm());
                w.newLine();
                ClimateColorInfo.write(1, "c = _clim:getFogTintTropical();");
                ClimateColorInfo.writeColor(1, clim.getFogTintTropical());
                w.newLine();
                WeatherPeriod weather = clim.getWeatherPeriod();
                ClimateColorInfo.write(1, "local w = _clim:getWeatherPeriod();");
                w.newLine();
                ClimateColorInfo.write(1, "c = w:getCloudColorReddish();");
                ClimateColorInfo.writeColor(1, weather.getCloudColorReddish());
                w.newLine();
                ClimateColorInfo.write(1, "c = w:getCloudColorGreenish();");
                ClimateColorInfo.writeColor(1, weather.getCloudColorGreenish());
                w.newLine();
                ClimateColorInfo.write(1, "c = w:getCloudColorBlueish();");
                ClimateColorInfo.writeColor(1, weather.getCloudColorBlueish());
                w.newLine();
                ClimateColorInfo.write(1, "c = w:getCloudColorPurplish();");
                ClimateColorInfo.writeColor(1, weather.getCloudColorPurplish());
                w.newLine();
                ClimateColorInfo.write(1, "c = w:getCloudColorTropical();");
                ClimateColorInfo.writeColor(1, weather.getCloudColorTropical());
                w.newLine();
                ClimateColorInfo.write(1, "c = w:getCloudColorBlizzard();");
                ClimateColorInfo.writeColor(1, weather.getCloudColorBlizzard());
                w.newLine();
                String[] segTxt = new String[]{"Dawn", "Day", "Dusk"};
                String[] seasTxt = new String[]{"SUMMER", "FALL", "WINTER", "SPRING"};
                String[] tempTxt = new String[]{"WARM", "NORMAL", "CLOUDY"};
                for (int seg = 0; seg < 3; ++seg) {
                    ClimateColorInfo.write(1, "-- ###################### " + segTxt[seg] + " ######################");
                    for (int seas = 0; seas < 4; ++seas) {
                        for (int temp = 0; temp < 3; ++temp) {
                            if (temp != 0 && temp != 2 && (temp != 1 || seg != 2)) continue;
                            ClimateColorInfo cc = clim.getSeasonColor(seg, temp, seas);
                            ClimateColorInfo.writeSeasonColor(1, cc, segTxt[seg], seasTxt[seas], tempTxt[temp]);
                            w.newLine();
                        }
                    }
                }
                ClimateColorInfo.write("end");
                w.newLine();
                ClimateColorInfo.write("Events.OnClimateManagerInit.Add(ClimateMain.onClimateManagerInit);");
                writer = null;
                w.flush();
                w.close();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private static void writeSeasonColor(int tabs, ClimateColorInfo ci, String seg, String seas, String temp) throws IOException {
        Color c = ci.exterior;
        ClimateColorInfo.write(tabs, "_clim:setSeasonColor" + seg + "(" + temp + "," + seas + "," + c.r + "," + c.g + "," + c.b + "," + c.a + ",true);\t\t--exterior");
        c = ci.interior;
        ClimateColorInfo.write(tabs, "_clim:setSeasonColor" + seg + "(" + temp + "," + seas + "," + c.r + "," + c.g + "," + c.b + "," + c.a + ",false);\t\t--interior");
    }

    private static void writeColor(int tabs, ClimateColorInfo ci) throws IOException {
        Color c = ci.exterior;
        ClimateColorInfo.write(tabs, "c:setExterior(" + c.r + "," + c.g + "," + c.b + "," + c.a + ");");
        c = ci.interior;
        ClimateColorInfo.write(tabs, "c:setInterior(" + c.r + "," + c.g + "," + c.b + "," + c.a + ");");
    }

    private static void write(int tabsTimes, String s) throws IOException {
        String tabs = new String(new char[tabsTimes]).replace("\u0000", "\t");
        writer.write(tabs);
        writer.write(s);
        writer.newLine();
    }

    private static void write(String s) throws IOException {
        writer.write(s);
        writer.newLine();
    }
}

