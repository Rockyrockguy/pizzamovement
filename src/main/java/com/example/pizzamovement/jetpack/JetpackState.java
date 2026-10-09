package com.example.pizzamovement.jetpack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-side per-player jetpack data. Touched only on the server thread. */
public final class JetpackState {
    public static class Entry {
        public long lastThrust = -1000L;   // game time of the last "engine on" packet from the client
        public boolean hasPos;
        public double lx, ly, lz;
        public int broken;                 // blocks broken, used to charge extra fuel

        public void remember(double x, double y, double z) {
            lx = x;
            ly = y;
            lz = z;
            hasPos = true;
        }
    }

    private static final Map<UUID, Entry> MAP = new HashMap<UUID, Entry>();

    private JetpackState() {}

    public static Entry entry(UUID id) {
        Entry e = MAP.get(id);
        if (e == null) {
            e = new Entry();
            MAP.put(id, e);
        }
        return e;
    }

    /** The client re-sends the packet every few ticks while Jump is held, so a short timeout is enough. */
    public static boolean isThrusting(Entry e, long now) {
        return now - e.lastThrust <= 8L;
    }

    public static void clear(UUID id) {
        MAP.remove(id);
    }
}
