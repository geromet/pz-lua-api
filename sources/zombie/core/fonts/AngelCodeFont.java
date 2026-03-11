/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.fonts;

import gnu.trove.list.array.TShortArrayList;
import gnu.trove.map.hash.TShortObjectHashMap;
import gnu.trove.procedure.TShortObjectProcedure;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import org.lwjgl.opengl.GL11;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.asset.Asset;
import zombie.asset.AssetStateObserver;
import zombie.core.Color;
import zombie.core.SpriteRenderer;
import zombie.core.fonts.Font;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureID;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;

@UsedFromLua
public final class AngelCodeFont
implements Font,
AssetStateObserver {
    private static final int DISPLAY_LIST_CACHE_SIZE = 200;
    private static final int MAX_CHAR = 255;
    private int baseDisplayListId = -1;
    public CharDef[] chars;
    private boolean displayListCaching;
    private DisplayList eldestDisplayList;
    private int eldestDisplayListId;
    private final LinkedHashMap<String, DisplayList> displayLists = new LinkedHashMap<String, DisplayList>(this, 200, 1.0f, true){
        final /* synthetic */ AngelCodeFont this$0;
        {
            AngelCodeFont angelCodeFont = this$0;
            Objects.requireNonNull(angelCodeFont);
            this.this$0 = angelCodeFont;
            super(arg0, arg1, arg2);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            this.this$0.eldestDisplayList = (DisplayList)eldest.getValue();
            this.this$0.eldestDisplayListId = this.this$0.eldestDisplayList.id;
            return false;
        }
    };
    private Texture fontImage;
    private int lineHeight;
    private final HashMap<Short, Texture> pages = new HashMap();
    private File fntFile;
    private boolean sdf;
    public static int xoff;
    public static int yoff;
    public static Color curCol;
    public static float curR;
    public static float curG;
    public static float curB;
    public static float curA;
    private static float scale;
    private static char[] data;

    public AngelCodeFont(String fntFile, Texture image) throws FileNotFoundException {
        int index;
        this.fontImage = image;
        Object path = fntFile;
        FileInputStream is = new FileInputStream(new File((String)path));
        if (((String)path).startsWith("/")) {
            path = ((String)path).substring(1);
        }
        while ((index = ((String)path).indexOf("\\")) != -1) {
            path = ((String)path).substring(0, index) + "/" + ((String)path).substring(index + 1);
        }
        this.parseFnt(is);
    }

    public AngelCodeFont(String fntFile, String imgFile) throws FileNotFoundException {
        int index;
        Object path;
        if (!StringUtils.isNullOrWhitespace(imgFile)) {
            boolean flags = false;
            this.fontImage = Texture.getSharedTexture(imgFile, 0);
            if (this.fontImage != null && !this.fontImage.isReady()) {
                this.fontImage.getObserverCb().add(this);
            }
        }
        if (((String)(path = fntFile)).startsWith("/")) {
            path = ((String)path).substring(1);
        }
        while ((index = ((String)path).indexOf("\\")) != -1) {
            path = ((String)path).substring(0, index) + "/" + ((String)path).substring(index + 1);
        }
        this.fntFile = new File(ZomboidFileSystem.instance.getString((String)path));
        FileInputStream is = new FileInputStream(ZomboidFileSystem.instance.getString((String)path));
        this.parseFnt(is);
    }

    @Override
    public void drawString(float x, float y, String text) {
        this.drawString(x, y, text, Color.white);
    }

    @Override
    public void drawString(float x, float y, String text, Color col) {
        this.drawString(x, y, text, col, 0, text.length() - 1);
    }

    public void drawString(float x, float y, String text, float r, float g, float b, float a) {
        this.drawString(x, y, text, r, g, b, a, 0, text.length() - 1);
    }

    public void drawString(float x, float y, float scale, String text, float r, float g, float b, float a) {
        this.drawString(x, y, scale, text, r, g, b, a, 0, text.length() - 1);
    }

    @Override
    public void drawString(float x, float y, String text, Color col, int startIndex, int endIndex) {
        xoff = (int)x;
        yoff = (int)y;
        curR = col.r;
        curG = col.g;
        curB = col.b;
        curA = col.a;
        scale = 0.0f;
        Texture.lr = col.r;
        Texture.lg = col.g;
        Texture.lb = col.b;
        Texture.la = col.a;
        if (this.displayListCaching && startIndex == 0 && endIndex == text.length() - 1) {
            DisplayList displayList = this.displayLists.get(text);
            if (displayList != null) {
                GL11.glCallList(displayList.id);
            } else {
                displayList = new DisplayList();
                displayList.text = text;
                int displayListCount = this.displayLists.size();
                if (displayListCount < 200) {
                    displayList.id = this.baseDisplayListId + displayListCount;
                } else {
                    displayList.id = this.eldestDisplayListId;
                    this.displayLists.remove(this.eldestDisplayList.text);
                }
                this.displayLists.put(text, displayList);
                GL11.glNewList(displayList.id, 4865);
                this.render(text, startIndex, endIndex);
                GL11.glEndList();
            }
        } else {
            this.render(text, startIndex, endIndex);
        }
    }

    public void drawString(float x, float y, String text, float r, float g, float b, float a, int startIndex, int endIndex) {
        this.drawString(x, y, 0.0f, text, r, g, b, a, startIndex, endIndex);
    }

    public void drawString(float x, float y, float scale, String text, float r, float g, float b, float a, int startIndex, int endIndex) {
        xoff = (int)x;
        yoff = (int)y;
        curR = r;
        curG = g;
        curB = b;
        curA = a;
        AngelCodeFont.scale = scale;
        Texture.lr = r;
        Texture.lg = g;
        Texture.lb = b;
        Texture.la = a;
        if (this.displayListCaching && startIndex == 0 && endIndex == text.length() - 1) {
            DisplayList displayList = this.displayLists.get(text);
            if (displayList != null) {
                GL11.glCallList(displayList.id);
            } else {
                displayList = new DisplayList();
                displayList.text = text;
                int displayListCount = this.displayLists.size();
                if (displayListCount < 200) {
                    displayList.id = this.baseDisplayListId + displayListCount;
                } else {
                    displayList.id = this.eldestDisplayListId;
                    this.displayLists.remove(this.eldestDisplayList.text);
                }
                this.displayLists.put(text, displayList);
                GL11.glNewList(displayList.id, 4865);
                this.render(text, startIndex, endIndex);
                GL11.glEndList();
            }
        } else {
            this.render(text, startIndex, endIndex);
        }
    }

    @Override
    public int getHeight(String text) {
        return this.getHeight(text, false, false);
    }

    public int getHeight(String text, boolean returnActualHeight, boolean returnOffset) {
        DisplayList displayList = null;
        if (this.displayListCaching && (displayList = this.displayLists.get(text)) != null && displayList.height != null) {
            return displayList.height.intValue();
        }
        int lines = 1;
        int maxHeight = 0;
        int minOffset = 1000000;
        for (int i = 0; i < text.length(); ++i) {
            CharDef charDef;
            char id = text.charAt(i);
            if (id == '\n') {
                ++lines;
                maxHeight = 0;
                continue;
            }
            if (id == ' ' || id >= this.chars.length || (charDef = this.chars[id]) == null) continue;
            maxHeight = Math.max(charDef.height + charDef.yoffset, maxHeight);
            minOffset = Math.min(charDef.yoffset, minOffset);
        }
        if (returnActualHeight) {
            return maxHeight - minOffset;
        }
        if (returnOffset) {
            return minOffset;
        }
        maxHeight = lines * this.getLineHeight();
        if (displayList != null) {
            displayList.height = (short)maxHeight;
        }
        return maxHeight;
    }

    @Override
    public int getLineHeight() {
        return this.lineHeight;
    }

    @Override
    public int getWidth(String text) {
        return this.getWidth(text, 0, text.length() - 1, false);
    }

    @Override
    public int getWidth(String text, boolean xAdvance) {
        return this.getWidth(text, 0, text.length() - 1, xAdvance);
    }

    @Override
    public int getWidth(String text, int start, int end) {
        return this.getWidth(text, start, end, false);
    }

    @Override
    public int getWidth(String text, int start, int end, boolean xadvance) {
        DisplayList displayList = null;
        if (this.displayListCaching && start == 0 && end == text.length() - 1 && (displayList = this.displayLists.get(text)) != null && displayList.width != null) {
            return displayList.width.intValue();
        }
        int numChars = end - start + 1;
        int maxWidth = 0;
        int width = 0;
        CharDef lastCharDef = null;
        for (int i = 0; i < numChars; ++i) {
            CharDef charDef;
            char id = text.charAt(start + i);
            if (id == '\n') {
                width = 0;
                continue;
            }
            if (id >= this.chars.length || (charDef = this.chars[id]) == null) continue;
            if (lastCharDef != null) {
                width += lastCharDef.getKerning(id);
            }
            lastCharDef = charDef;
            width = xadvance || i < numChars - 1 ? (width += charDef.xadvance) : (width += charDef.width);
            maxWidth = Math.max(maxWidth, width);
        }
        if (displayList != null) {
            displayList.width = (short)maxWidth;
        }
        return maxWidth;
    }

    public int getYOffset(String text) {
        DisplayList displayList = null;
        if (this.displayListCaching && (displayList = this.displayLists.get(text)) != null && displayList.yOffset != null) {
            return displayList.yOffset.intValue();
        }
        int stopIndex = text.indexOf(10);
        if (stopIndex == -1) {
            stopIndex = text.length();
        }
        int minYOffset = 10000;
        for (int i = 0; i < stopIndex; ++i) {
            char id = text.charAt(i);
            CharDef charDef = this.chars[id];
            if (charDef == null) continue;
            minYOffset = Math.min(charDef.yoffset, minYOffset);
        }
        if (displayList != null) {
            displayList.yOffset = (short)minYOffset;
        }
        return minYOffset;
    }

    private CharDef parseChar(String line) {
        CharDef def = new CharDef(this);
        StringTokenizer tokens = new StringTokenizer(line, " =");
        tokens.nextToken();
        tokens.nextToken();
        def.id = Integer.parseInt(tokens.nextToken());
        if (def.id < 0) {
            return null;
        }
        if (def.id > 255) {
            // empty if block
        }
        tokens.nextToken();
        def.x = Short.parseShort(tokens.nextToken());
        tokens.nextToken();
        def.y = Short.parseShort(tokens.nextToken());
        tokens.nextToken();
        def.width = Short.parseShort(tokens.nextToken());
        tokens.nextToken();
        def.height = Short.parseShort(tokens.nextToken());
        tokens.nextToken();
        def.xoffset = Short.parseShort(tokens.nextToken());
        tokens.nextToken();
        def.yoffset = Short.parseShort(tokens.nextToken());
        tokens.nextToken();
        def.xadvance = Short.parseShort(tokens.nextToken());
        tokens.nextToken();
        def.page = Short.parseShort(tokens.nextToken());
        Texture fontImage1 = this.fontImage;
        if (this.pages.containsKey(def.page)) {
            fontImage1 = this.pages.get(def.page);
        }
        if (fontImage1 != null && fontImage1.isReady()) {
            def.init();
        }
        if (def.id != 32) {
            this.lineHeight = Math.max(def.height + def.yoffset, this.lineHeight);
        }
        return def;
    }

    private void parseFnt(InputStream fntFile) {
        if (this.displayListCaching) {
            this.baseDisplayListId = GL11.glGenLists(200);
            if (this.baseDisplayListId == 0) {
                this.displayListCaching = false;
            }
        }
        try {
            CharDef def;
            BufferedReader in = new BufferedReader(new InputStreamReader(fntFile));
            String info = in.readLine();
            String common = in.readLine();
            TShortObjectHashMap<TShortArrayList> kerning = new TShortObjectHashMap<TShortArrayList>(64);
            ArrayList<CharDef> charDefs = new ArrayList<CharDef>(255);
            int maxChar = 0;
            boolean done = false;
            while (!done) {
                StringTokenizer tokens;
                String line = in.readLine();
                if (line == null) {
                    done = true;
                    continue;
                }
                if (line.startsWith("page")) {
                    tokens = new StringTokenizer(line, " =");
                    tokens.nextToken();
                    tokens.nextToken();
                    short id = Short.parseShort(tokens.nextToken());
                    tokens.nextToken();
                    Object file = tokens.nextToken().replace("\"", "");
                    file = this.fntFile.getParent() + File.separatorChar + (String)file;
                    file = ((String)file).replace("\\", "/");
                    int flags = 0;
                    Texture tex = Texture.getSharedTexture((String)file, flags |= TextureID.useCompression ? 4 : 0);
                    if (tex == null) {
                        DebugLog.DetailedInfo.trace("AngelCodeFont failed to load page " + id + " texture " + (String)file);
                    } else {
                        this.pages.put(id, tex);
                        if (!tex.isReady()) {
                            tex.getObserverCb().add(this);
                        }
                    }
                }
                if (!line.startsWith("chars c") && line.startsWith("char") && (def = this.parseChar(line)) != null) {
                    maxChar = Math.max(maxChar, def.id);
                    charDefs.add(def);
                }
                if (line.startsWith("kernings c") || !line.startsWith("kerning")) continue;
                tokens = new StringTokenizer(line, " =");
                tokens.nextToken();
                tokens.nextToken();
                short first = Short.parseShort(tokens.nextToken());
                tokens.nextToken();
                int second = Integer.parseInt(tokens.nextToken());
                tokens.nextToken();
                int offset = Integer.parseInt(tokens.nextToken());
                TShortArrayList values2 = (TShortArrayList)kerning.get(first);
                if (values2 == null) {
                    values2 = new TShortArrayList();
                    kerning.put(first, values2);
                }
                values2.add((short)second);
                values2.add((short)offset);
            }
            this.chars = new CharDef[maxChar + 1];
            Iterator iterator2 = charDefs.iterator();
            while (iterator2.hasNext()) {
                this.chars[def.id] = def = (CharDef)iterator2.next();
            }
            kerning.forEachEntry(new TShortObjectProcedure<TShortArrayList>(this){
                final /* synthetic */ AngelCodeFont this$0;
                {
                    AngelCodeFont angelCodeFont = this$0;
                    Objects.requireNonNull(angelCodeFont);
                    this.this$0 = angelCodeFont;
                }

                @Override
                public boolean execute(short key, TShortArrayList value) {
                    CharDef charDef = this.this$0.chars[key];
                    charDef.kerningSecond = new short[value.size() / 2];
                    charDef.kerningAmount = new short[value.size() / 2];
                    int n = 0;
                    for (int i = 0; i < value.size(); i += 2) {
                        charDef.kerningSecond[n] = value.get(i);
                        charDef.kerningAmount[n] = value.get(i + 1);
                        ++n;
                    }
                    short[] sortedSecond = Arrays.copyOf(charDef.kerningSecond, charDef.kerningSecond.length);
                    short[] copyAmount = Arrays.copyOf(charDef.kerningAmount, charDef.kerningAmount.length);
                    Arrays.sort(sortedSecond);
                    block1: for (int i = 0; i < sortedSecond.length; ++i) {
                        for (int j = 0; j < charDef.kerningSecond.length; ++j) {
                            if (charDef.kerningSecond[j] != sortedSecond[i]) continue;
                            charDef.kerningAmount[i] = copyAmount[j];
                            continue block1;
                        }
                    }
                    charDef.kerningSecond = sortedSecond;
                    return true;
                }
            });
            in.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void render(String text, int start, int end) {
        int numChars = ++end - start;
        float x = 0.0f;
        float y = 0.0f;
        CharDef lastCharDef = null;
        if (data.length < numChars) {
            data = new char[(numChars + 128 - 1) / 128 * 128];
        }
        text.getChars(start, end, data, 0);
        for (int i = 0; i < numChars; ++i) {
            CharDef charDef;
            int id = data[i];
            if (id == 10) {
                x = 0.0f;
                y += (float)this.getLineHeight();
                continue;
            }
            if (id >= this.chars.length) {
                id = 63;
            }
            if ((charDef = this.chars[id]) == null) continue;
            if (lastCharDef != null) {
                x = scale > 0.0f ? (x += (float)lastCharDef.getKerning(id) * scale) : (x += (float)lastCharDef.getKerning(id));
            }
            lastCharDef = charDef;
            charDef.draw(x, y);
            if (scale > 0.0f) {
                x += (float)charDef.xadvance * scale;
                continue;
            }
            x += (float)charDef.xadvance;
        }
    }

    @Override
    public void onStateChanged(Asset.State oldState, Asset.State newState, Asset asset) {
        if (asset != this.fontImage && !this.pages.containsValue(asset)) {
            return;
        }
        if (newState != Asset.State.READY) {
            return;
        }
        for (CharDef charDef : this.chars) {
            if (charDef == null || charDef.image != null) continue;
            Texture fontImage1 = this.fontImage;
            if (this.pages.containsKey(charDef.page)) {
                fontImage1 = this.pages.get(charDef.page);
            }
            if (asset != fontImage1) continue;
            charDef.init();
        }
    }

    public boolean isEmpty() {
        if (this.fontImage != null && this.fontImage.isEmpty()) {
            return true;
        }
        for (Texture tex : this.pages.values()) {
            if (!tex.isEmpty()) continue;
            return true;
        }
        return false;
    }

    public boolean isSdf() {
        return this.sdf;
    }

    public void setSdf(boolean b) {
        this.sdf = b;
    }

    public void destroy() {
        for (CharDef charDef : this.chars) {
            if (charDef == null) continue;
            charDef.destroy();
        }
        Arrays.fill(this.chars, null);
        this.pages.clear();
    }

    static {
        data = new char[256];
    }

    private static class DisplayList {
        Short height;
        int id;
        String text;
        Short width;
        Short yOffset;

        private DisplayList() {
        }
    }

    public class CharDef {
        public short dlIndex;
        public short height;
        public int id;
        public Texture image;
        public short[] kerningSecond;
        public short[] kerningAmount;
        public short width;
        public short x;
        public short xadvance;
        public short xoffset;
        public short y;
        public short yoffset;
        public short page;
        final /* synthetic */ AngelCodeFont this$0;

        public CharDef(AngelCodeFont this$0) {
            AngelCodeFont angelCodeFont = this$0;
            Objects.requireNonNull(angelCodeFont);
            this.this$0 = angelCodeFont;
        }

        public void draw(float x, float y) {
            Texture tex = this.image;
            if (scale > 0.0f) {
                SpriteRenderer.instance.states.getPopulatingActiveState().render(tex, x + (float)this.xoffset * scale + (float)xoff, y + (float)this.yoffset * scale + (float)yoff, (float)this.width * scale, (float)this.height * scale, curR, curG, curB, curA, null);
            } else {
                SpriteRenderer.instance.renderi(tex, (int)(x + (float)this.xoffset + (float)xoff), (int)(y + (float)this.yoffset + (float)yoff), this.width, this.height, curR, curG, curB, curA, null);
            }
        }

        public int getKerning(int otherCodePoint) {
            if (this.kerningSecond == null) {
                return 0;
            }
            int low = 0;
            int high = this.kerningSecond.length - 1;
            while (low <= high) {
                int midIndex = low + high >>> 1;
                if (this.kerningSecond[midIndex] < otherCodePoint) {
                    low = midIndex + 1;
                    continue;
                }
                if (this.kerningSecond[midIndex] > otherCodePoint) {
                    high = midIndex - 1;
                    continue;
                }
                return this.kerningAmount[midIndex];
            }
            return 0;
        }

        public void init() {
            Texture fontImage = this.this$0.fontImage;
            if (this.this$0.pages.containsKey(this.page)) {
                fontImage = this.this$0.pages.get(this.page);
            }
            this.image = new CharDefTexture(fontImage.getTextureId(), fontImage.getName() + "_" + this.x + "_" + this.y);
            this.image.setRegion(this.x + (int)(fontImage.xStart * (float)fontImage.getWidthHW()), this.y + (int)(fontImage.yStart * (float)fontImage.getHeightHW()), this.width, this.height);
        }

        public void destroy() {
            if (this.image != null && this.image.getTextureId() != null) {
                ((CharDefTexture)this.image).releaseCharDef();
                this.image = null;
            }
        }

        public String toString() {
            return "[CharDef id=" + this.id + " x=" + this.x + " y=" + this.y + "]";
        }
    }

    public static final class CharDefTexture
    extends Texture {
        public CharDefTexture(TextureID data, String name) {
            super(data, name);
        }

        public void releaseCharDef() {
            this.removeDependency(this.dataid);
        }
    }
}

