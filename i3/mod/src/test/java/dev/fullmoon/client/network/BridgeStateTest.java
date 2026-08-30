package dev.fullmoon.client.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

final class BridgeStateTest {
    @Test
    void exactWelcomeActivatesTheSession() {
        BridgeState state = BridgeState.apply(
            BridgeState.connected(1_000), new BridgeProtocol.Welcome(1), 1_100);

        assertEquals(BridgeState.Mode.ACTIVE, state.mode());
        assertEquals(1, state.serverProtocol());
    }

    @Test
    void oldAndNewServersFallBackWithoutSyntheticData() {
        BridgeState oldServer = BridgeState.apply(
            BridgeState.connected(1_000), new BridgeProtocol.Welcome(0), 1_100);
        BridgeState newServer = BridgeState.apply(
            BridgeState.connected(1_000), new BridgeProtocol.Welcome(2), 1_100);

        assertEquals(BridgeState.Mode.FALLBACK, oldServer.mode());
        assertEquals(BridgeState.Mode.INCOMPATIBLE, newServer.mode());
        assertTrue(oldServer.metrics().isEmpty());
        assertTrue(newServer.notice().isEmpty());
    }

    @Test
    void aSilentServerFallsBackAfterTheHandshakeBudget() {
        BridgeState waiting = BridgeState.connected(1_000);

        assertSame(waiting, BridgeState.tick(waiting, 5_999));
        assertEquals(BridgeState.Mode.FALLBACK, BridgeState.tick(waiting, 6_000).mode());
    }

    @Test
    void operationalPayloadsAreIgnoredUntilHandshakeCompletes() {
        BridgeState waiting = BridgeState.connected(1_000);
        BridgeProtocol.HudSync sync = new BridgeProtocol.HudSync(1, 1, 19.9, 8.2);
        BridgeProtocol.Notice notice = notice("first", 4_000);

        assertSame(waiting, BridgeState.apply(waiting, sync, 1_100));
        assertSame(waiting, BridgeState.apply(waiting, notice, 1_100));
    }

    @Test
    void metricsAdvanceMonotonicallyAndGoStale() {
        BridgeState active = active();
        BridgeState first = BridgeState.apply(
            active, new BridgeProtocol.HudSync(1, 4, 19.7, 14.1), 2_000);
        BridgeState older = BridgeState.apply(
            first, new BridgeProtocol.HudSync(1, 3, 20.0, 1.0), 2_100);
        BridgeState newer = BridgeState.apply(
            older, new BridgeProtocol.HudSync(1, 5, 19.9, 10.2), 2_200);

        assertSame(first, older);
        assertEquals(5, newer.metrics().orElseThrow().revision());
        assertTrue(BridgeState.liveMetrics(newer, 7_199).isPresent());
        assertTrue(BridgeState.liveMetrics(newer, 7_200).isEmpty());
    }

    @Test
    void mismatchedOperationalProtocolCannotChangeLiveState() {
        BridgeState active = active();

        assertSame(active, BridgeState.apply(
            active, new BridgeProtocol.HudSync(0, 1, 19.9, 8.2), 2_000));
        assertSame(active, BridgeState.apply(active, new BridgeProtocol.Unknown(2, "future"), 2_000));
        assertSame(active, BridgeState.apply(active, new BridgeProtocol.Hello(1, "fullmoon", "3.0.0"), 2_000));
        assertSame(active, BridgeState.tick(active, 20_000));
    }

    @Test
    void noticesReplaceByArrivalAndExpireExactly() {
        BridgeState active = active();
        BridgeState first = BridgeState.apply(active, notice("first", 4_000), 2_000);
        BridgeState second = BridgeState.apply(first, notice("second", 5_000), 2_500);

        assertEquals("second", BridgeState.liveNotice(second, 7_499).orElseThrow().id());
        assertTrue(BridgeState.liveNotice(second, 7_500).isEmpty());
    }

    @Test
    void disconnectReturnsAnEmptyImmutableState() {
        BridgeState state = BridgeState.apply(
            active(), new BridgeProtocol.HudSync(1, 1, 19.9, 8.2), 2_000);

        BridgeState disconnected = BridgeState.disconnected();

        assertEquals(BridgeState.Mode.DISCONNECTED, disconnected.mode());
        assertTrue(disconnected.metrics().isEmpty());
        assertTrue(disconnected.notice().isEmpty());
        assertFalse(state.equals(disconnected));
    }

    @Test
    void decodedPayloadsDriveTheReducerEndToEnd() {
        BridgeProtocol.Message welcome = BridgeProtocol.decode(
            "{\"type\":\"welcome\",\"proto\":1}".getBytes(StandardCharsets.UTF_8))
            .message().orElseThrow();
        BridgeProtocol.Message sync = BridgeProtocol.decode(
            ("{\"type\":\"hud_sync\",\"proto\":1,\"revision\":9,"
                + "\"tps\":19.95,\"tick_ms\":7.4}").getBytes(StandardCharsets.UTF_8))
            .message().orElseThrow();

        BridgeState state = BridgeState.apply(BridgeState.connected(1_000), welcome, 1_100);
        BridgeState synced = BridgeState.apply(state, sync, 1_200);

        assertEquals(BridgeState.Mode.ACTIVE, synced.mode());
        assertEquals(19.95, synced.metrics().orElseThrow().ticksPerSecond());
    }

    private static BridgeState active() {
        return BridgeState.apply(
            BridgeState.connected(1_000), new BridgeProtocol.Welcome(1), 1_100);
    }

    private static BridgeProtocol.Notice notice(String id, int durationMillis) {
        return new BridgeProtocol.Notice(
            1, id, "Maintenance", "Restart in five minutes",
            BridgeProtocol.Severity.WARNING, durationMillis);
    }
}
