/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import se.krka.kahlua.vm.KahluaUtil;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.worldgen.WorldGenTiming;

@UsedFromLua
public class WorldGenUtils {
    @UsedFromLua
    public static final WorldGenUtils INSTANCE = new WorldGenUtils();
    private static final int OFFSET = 2;
    private ArrayList<String> files = new ArrayList();

    private WorldGenUtils() {
    }

    public String generateSeed() {
        Random rnd = new Random();
        Object res = "";
        for (int i = 0; i < 16; ++i) {
            int rndInt = rnd.nextInt(52);
            res = rndInt < 26 ? (String)res + (char)(rndInt + 65) : (String)res + (char)(rndInt + 97 - 26);
        }
        return res;
    }

    public void getFiles(String basePath) {
        this.files = new ArrayList();
        File mainFile = ZomboidFileSystem.instance.getMediaFile(basePath);
        File base = ZomboidFileSystem.instance.base.canonicalFile;
        try (Stream<Path> stream = Files.walk(Paths.get(mainFile.getPath(), new String[0]), new FileVisitOption[0]);){
            stream.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).filter(f -> f.toString().endsWith("lua")).forEach(f -> this.files.add(base.toPath().relativize((Path)f).toString().replaceAll("\\\\", "/")));
        }
        catch (IOException e) {
            DebugLog.General.printException(e, "", LogSeverity.Error);
        }
    }

    public int getFilesNum() {
        return this.files.size();
    }

    public String getFile(int i) {
        return this.files.get(i);
    }

    public String displayTable(String tableName) {
        StringBuilder buffer = new StringBuilder();
        Object table = LuaManager.env.rawget(tableName);
        this.displayElement(tableName, table, buffer, 0);
        return buffer.toString();
    }

    public String displayTable(KahluaTable table) {
        StringBuilder buffer = new StringBuilder();
        this.displayElement("", table, buffer, 0);
        return buffer.toString();
    }

    private void displayElement(String name, Object value, StringBuilder buffer, int offset) {
        String type = KahluaUtil.type(value);
        String space = " ".repeat(Math.max(0, offset));
        switch (type) {
            case "nil": 
            case "string": 
            case "number": 
            case "boolean": {
                buffer.append(String.format("%s%s = %s\n", space, name, value));
                break;
            }
            case "function": 
            case "coroutine": 
            case "userdata": {
                buffer.append(String.format("%s%s = %s\n", space, name, value));
                break;
            }
            case "table": {
                buffer.append(String.format("%s%s = {\n", space, name));
                KahluaTable table = (KahluaTable)value;
                KahluaTableIterator tableIter = table.iterator();
                while (tableIter.advance()) {
                    String k = tableIter.getKey().toString();
                    Object v = tableIter.getValue();
                    this.displayElement(k, v, buffer, offset + 2);
                }
                buffer.append(String.format("%s}\n", space));
                break;
            }
            default: {
                buffer.append(String.format("!!!! %s%s = %s\n", space, name, value));
            }
        }
    }

    /*
     * WARNING - void declaration
     */
    public boolean canPlace(List<String> placement, String floorName) {
        boolean toBePlaced = false;
        for (String string : placement) {
            void var5_8;
            String string2;
            String string3;
            String string4;
            String string5;
            boolean check2 = true;
            if (string.startsWith("!")) {
                check2 = false;
                String string6 = string.substring(1);
            }
            if (!floorName.matches(string5 = (string4 = (string3 = (string2 = "^" + (String)var5_8).replace(".", "\\.")).replace("*", ".*")).replace("?", ".?"))) continue;
            toBePlaced = check2;
        }
        return toBePlaced;
    }

    public @Nullable IsoObject doesFloorExit(IsoChunk chunk, int tileX, int tileY, int z) {
        IsoGridSquare square = chunk.getGridSquare(tileX, tileY, z);
        return square == null ? null : square.getFloor();
    }

    public @Nullable IsoObject doesFloorExit(IsoCell cell, int tileX, int tileY, int z) {
        IsoGridSquare square = cell.getGridSquare(tileX, tileY, z);
        return square == null ? null : square.getFloor();
    }

    public String methodName(StackTraceElement trace) {
        return String.format("%s.%s:%s", trace.getClassName(), trace.getMethodName(), trace.getLineNumber());
    }

    public String methodsCall(String header, int depth, String ... args2) {
        int i;
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        StringBuilder buffer = new StringBuilder();
        buffer.append(header);
        if (args2.length != 0) {
            buffer.append(" [");
            for (i = 0; i < args2.length; ++i) {
                buffer.append(args2[i]);
                buffer.append(", ");
            }
            buffer.append("]");
        }
        for (i = 0; i <= depth; ++i) {
            buffer.append(" <- " + this.methodName(trace[i + 2]));
        }
        return buffer.toString();
    }

    public void showTimers(String clazzStr) {
        try {
            Field[] fields;
            Class<?> clazz = Class.forName(clazzStr);
            for (Field field : fields = clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (!field.getType().equals(WorldGenTiming.class)) continue;
                WorldGenTiming timing = (WorldGenTiming)field.get(null);
                System.out.println(field.getName() + " = " + (float)timing.meanDuration() / 1000.0f / 1000.0f + " ms [" + timing.times() + "] [" + timing.minTime() + " " + timing.maxTime() + "]");
            }
        }
        catch (ClassNotFoundException e) {
            System.out.println("Class not found: " + clazzStr);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void showTimersTotal(String clazzStr) {
        try {
            Field[] fields;
            Class<?> clazz = Class.forName(clazzStr);
            for (Field field : fields = clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (!field.getType().equals(WorldGenTiming.class)) continue;
                WorldGenTiming timing = (WorldGenTiming)field.get(null);
                System.out.println(field.getName() + " = " + (float)timing.totalDuration() / 1000.0f / 1000.0f + " ms [" + timing.times() + "] [" + timing.minTime() + " " + timing.maxTime() + "]");
            }
        }
        catch (ClassNotFoundException e) {
            System.out.println("Class not found: " + clazzStr);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void resetTimers(String clazzStr) {
        try {
            Field[] fields;
            Class<?> clazz = Class.forName(clazzStr);
            for (Field field : fields = clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (!field.getType().equals(WorldGenTiming.class)) continue;
                ((WorldGenTiming)field.get(null)).reset();
            }
        }
        catch (ClassNotFoundException e) {
            System.out.println("Class not found: " + clazzStr);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void getTimerKept(String clazzStr, String fieldName) {
        try {
            Field[] fields;
            Class<?> clazz = Class.forName(clazzStr);
            for (Field field : fields = clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (!field.getType().equals(WorldGenTiming.class) || !field.getName().equals(fieldName)) continue;
                List<Long> kept = ((WorldGenTiming)field.get(null)).getKept();
                StringBuilder s = new StringBuilder();
                for (Long time : kept) {
                    s.append(String.format("%d%n", time));
                }
                System.out.println(s);
            }
        }
        catch (ClassNotFoundException e) {
            System.out.println("Class not found: " + clazzStr);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}

