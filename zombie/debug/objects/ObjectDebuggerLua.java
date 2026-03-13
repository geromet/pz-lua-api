/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.objects;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.UsedFromLua;
import zombie.debug.DebugLog;
import zombie.debug.objects.ObjectDebugger;

@UsedFromLua
public class ObjectDebuggerLua {
    private static final ConcurrentLinkedDeque<ArrayList<String>> array_list_pool = new ConcurrentLinkedDeque();

    public static ArrayList<String> AllocList() {
        ArrayList<String> list = array_list_pool.poll();
        if (list == null) {
            list = new ArrayList();
        }
        return list;
    }

    public static void ReleaseList(ArrayList<String> list) {
        list.clear();
        array_list_pool.offer(list);
    }

    public static void Log(Object o) {
        ObjectDebuggerLua.Log(o, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public static void Log(Object o, int inheritanceDepth) {
        ObjectDebuggerLua.Log(o, inheritanceDepth, Integer.MAX_VALUE);
    }

    public static void Log(Object o, int inheritanceDepth, int memberDepth) {
        ObjectDebugger.Log(DebugLog.Lua, o, inheritanceDepth, true, true, memberDepth);
    }

    public static void GetLines(Object o, ArrayList<String> list) {
        ObjectDebuggerLua.GetLines(o, list, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public static void GetLines(Object o, ArrayList<String> list, int inheritanceDepth) {
        ObjectDebuggerLua.GetLines(o, list, inheritanceDepth, Integer.MAX_VALUE);
    }

    public static void GetLines(Object o, ArrayList<String> list, int inheritanceDepth, int memberDepth) {
        ObjectDebugger.GetLines(o, list, inheritanceDepth, true, true, memberDepth);
    }
}

