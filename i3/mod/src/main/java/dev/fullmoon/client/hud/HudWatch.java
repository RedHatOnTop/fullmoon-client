package dev.fullmoon.client.hud;

/**
 * When a HUD file on disk is worth re-reading. The launcher writes the same file the in-game
 * editor does, so the running client has to notice an edit it did not make — without paying for
 * a stat every frame, and without adopting its own writes back.
 *
 * <p>Holds no path and touches no disk: the caller stats the file and reports what it found.
 */
public final class HudWatch {
    /** A stat is cheap but not free; half a second is under human notice either way. */
    public static final long POLL_MS = 500L;

    private long nextPollAt;
    private long adopted;

    /** Whether enough time has passed that the caller should stat the file again. */
    public boolean due(long nowMs) {
        if (nowMs < nextPollAt) {
            return false;
        }
        nextPollAt = nowMs + POLL_MS;
        return true;
    }

    /**
     * Whether a stat's result is an edit the running HUD has not taken yet. A missing file
     * (reported as {@code 0}) is nothing to adopt, and forgets what was adopted so the file
     * coming back is read again whatever its timestamp says.
     */
    public boolean changed(long modifiedMs) {
        if (modifiedMs <= 0L) {
            adopted = 0L;
            return false;
        }
        if (modifiedMs == adopted) {
            return false;
        }
        adopted = modifiedMs;
        return true;
    }

    /** Records a write the client made itself, so the next poll does not read it back. */
    public void authored(long modifiedMs) {
        adopted = modifiedMs;
    }
}
