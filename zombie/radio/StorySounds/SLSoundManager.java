/*
 * Decompiled with CFR 0.152.
 */
package zombie.radio.StorySounds;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.input.GameKeyboard;
import zombie.iso.Vector2;
import zombie.radio.StorySounds.StoryEmitter;
import zombie.radio.StorySounds.StorySound;
import zombie.ui.TextManager;
import zombie.ui.UIFont;

@UsedFromLua
public final class SLSoundManager {
    public static boolean enabled;
    public static boolean debug;
    public static boolean luaDebug;
    public static StoryEmitter emitter;
    private static SLSoundManager instance;
    private final HashMap<Integer, Boolean> state = new HashMap();
    private final ArrayList<StorySound> storySounds = new ArrayList();
    private int nextTick;
    private final float borderCenterX = 10500.0f;
    private final float borderCenterY = 9000.0f;
    private final float borderRadiusMin = 12000.0f;
    private final float borderRadiusMax = 16000.0f;
    private final float borderScale = 1.0f;

    public static SLSoundManager getInstance() {
        if (instance == null) {
            instance = new SLSoundManager();
        }
        return instance;
    }

    private SLSoundManager() {
        this.state.put(12, false);
        this.state.put(13, false);
    }

    public boolean getDebug() {
        return debug;
    }

    public boolean getLuaDebug() {
        return luaDebug;
    }

    public ArrayList<StorySound> getStorySounds() {
        return this.storySounds;
    }

    public void print(String line) {
        if (debug) {
            System.out.println(line);
        }
    }

    public void init() {
        this.loadSounds();
    }

    public void loadSounds() {
        this.storySounds.clear();
        try {
            File f = ZomboidFileSystem.instance.getMediaFile("sound" + File.separator);
            if (f.exists() && f.isDirectory()) {
                File[] fileList = f.listFiles();
                for (int i = 0; i < fileList.length; ++i) {
                    String fileName;
                    if (!fileList[i].isFile() || (fileName = fileList[i].getName()).lastIndexOf(".") == -1 || fileName.lastIndexOf(".") == 0 || !fileName.substring(fileName.lastIndexOf(".") + 1).equals("ogg")) continue;
                    String sound = fileName.substring(0, fileName.lastIndexOf("."));
                    this.print("Adding sound: " + sound);
                    this.addStorySound(new StorySound(sound, 1.0f));
                }
            }
        }
        catch (Exception e) {
            System.out.print(e.getMessage());
        }
    }

    private void addStorySound(StorySound storySound) {
        this.storySounds.add(storySound);
    }

    public void updateKeys() {
        for (Map.Entry<Integer, Boolean> entry : this.state.entrySet()) {
            boolean isdown = GameKeyboard.isKeyDown(entry.getKey());
            if (isdown && entry.getValue() != isdown) {
                switch (entry.getKey()) {
                    case 12: {
                        break;
                    }
                    case 13: {
                        SLSoundManager.emitter.coordinate3d = !SLSoundManager.emitter.coordinate3d;
                        break;
                    }
                    case 53: {
                        break;
                    }
                }
            }
            entry.setValue(isdown);
        }
    }

    public void update(int storylineDay, int hour, int min) {
        this.updateKeys();
        emitter.tick();
    }

    public void thunderTest() {
        --this.nextTick;
        if (this.nextTick <= 0) {
            this.nextTick = Rand.Next(10, 180);
            float radius = Rand.Next(0.0f, 8000.0f);
            double angle = Math.random() * Math.PI * 2.0;
            float x = 10500.0f + (float)(Math.cos(angle) * (double)radius);
            float y = 9000.0f + (float)(Math.sin(angle) * (double)radius);
            if (Rand.Next(0, 100) < 60) {
                emitter.playSound("thunder", 1.0f, x, y, 0.0f, 100.0f, 8500.0f);
            } else {
                emitter.playSound("thundereffect", 1.0f, x, y, 0.0f, 100.0f, 8500.0f);
            }
        }
    }

    public void render() {
        this.renderDebug();
    }

    public void renderDebug() {
        if (debug) {
            String str = SLSoundManager.emitter.coordinate3d ? "3D coordinates, X-Z-Y" : "2D coordinates X-Y-Z";
            int w = TextManager.instance.MeasureStringX(UIFont.Large, str) / 2;
            int h = TextManager.instance.MeasureStringY(UIFont.Large, str);
            int midx = Core.getInstance().getScreenWidth() / 2;
            int midy = Core.getInstance().getScreenHeight() / 2;
            this.renderLine(UIFont.Large, str, midx - w, midy);
        }
    }

    private void renderLine(UIFont font, String line, int x, int y) {
        TextManager.instance.DrawString(font, x + 1, y + 1, line, 0.0, 0.0, 0.0, 1.0);
        TextManager.instance.DrawString(font, x - 1, y - 1, line, 0.0, 0.0, 0.0, 1.0);
        TextManager.instance.DrawString(font, x + 1, y - 1, line, 0.0, 0.0, 0.0, 1.0);
        TextManager.instance.DrawString(font, x - 1, y + 1, line, 0.0, 0.0, 0.0, 1.0);
        TextManager.instance.DrawString(font, x, y, line, 1.0, 1.0, 1.0, 1.0);
    }

    public Vector2 getRandomBorderPosition() {
        float radius = Rand.Next(12000.0f, 16000.0f);
        double angle = Math.random() * Math.PI * 2.0;
        float x = 10500.0f + (float)(Math.cos(angle) * (double)radius);
        float y = 9000.0f + (float)(Math.sin(angle) * (double)radius);
        return new Vector2(x, y);
    }

    public float getRandomBorderRange() {
        return Rand.Next(18000.0f, 24000.0f);
    }

    static {
        emitter = new StoryEmitter();
    }
}

