package dev.pinion.hud;

/** Clicks per second, counted from the actual button-down events rather than
 *  sampled per frame — a frame poll silently under-reports anyone who clicks
 *  faster than the refresh rate, and a HUD that lies is worse than no HUD.
 *
 *  Timestamps live in a fixed ring, so the counter never allocates in the
 *  input callback. */
public final class ClickCounter {
    private static final int CAP = 64;
    private static final long WINDOW_MS = 1000;

    private final long[] stamps = new long[CAP];
    private int head;

    public void hit() {
        stamps[head] = System.currentTimeMillis();
        head = (head + 1) % CAP;
    }

    public int perSecond() {
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        int n = 0;
        for (long t : stamps) {
            if (t > cutoff) {
                n++;
            }
        }
        return n;
    }
}
