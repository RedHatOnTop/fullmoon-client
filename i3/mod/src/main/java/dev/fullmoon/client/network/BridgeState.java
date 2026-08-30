package dev.fullmoon.client.network;

import java.util.Objects;
import java.util.Optional;

public record BridgeState(
        Mode mode,
        int serverProtocol,
        long connectedAt,
        Optional<Metrics> metrics,
        Optional<ActiveNotice> notice) {
    public static final long HANDSHAKE_TIMEOUT_MILLIS = 5_000;
    public static final long METRICS_STALE_MILLIS = 5_000;

    public BridgeState {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(metrics, "metrics");
        Objects.requireNonNull(notice, "notice");
    }

    public enum Mode {
        DISCONNECTED,
        WAITING,
        ACTIVE,
        FALLBACK,
        INCOMPATIBLE
    }

    public record Metrics(
            long revision,
            double ticksPerSecond,
            double tickMilliseconds,
            long receivedAt) {}

    public record ActiveNotice(
            String id,
            String title,
            String body,
            BridgeProtocol.Severity severity,
            long receivedAt,
            long expiresAt) {
        public ActiveNotice {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(body, "body");
            Objects.requireNonNull(severity, "severity");
        }
    }

    public static BridgeState disconnected() {
        return empty(Mode.DISCONNECTED, 0, 0);
    }

    public static BridgeState connected(long now) {
        return empty(Mode.WAITING, 0, now);
    }

    public static BridgeState apply(BridgeState state, BridgeProtocol.Message message, long now) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(message, "message");

        if (message instanceof BridgeProtocol.Welcome welcome) {
            return welcome(state, welcome);
        }
        if (state.mode != Mode.ACTIVE || message.proto() != state.serverProtocol) {
            return state;
        }
        if (message instanceof BridgeProtocol.HudSync sync) {
            return metrics(state, sync, now);
        }
        if (message instanceof BridgeProtocol.Notice incoming) {
            return notice(state, incoming, now);
        }
        return state;
    }

    public static BridgeState tick(BridgeState state, long now) {
        Objects.requireNonNull(state, "state");
        if (state.mode == Mode.WAITING && now - state.connectedAt >= HANDSHAKE_TIMEOUT_MILLIS) {
            return empty(Mode.FALLBACK, 0, state.connectedAt);
        }
        return state;
    }

    public static Optional<Metrics> liveMetrics(BridgeState state, long now) {
        Objects.requireNonNull(state, "state");
        return state.metrics.filter(value -> now - value.receivedAt < METRICS_STALE_MILLIS);
    }

    public static Optional<ActiveNotice> liveNotice(BridgeState state, long now) {
        Objects.requireNonNull(state, "state");
        return state.notice.filter(value -> now < value.expiresAt);
    }

    private static BridgeState welcome(BridgeState state, BridgeProtocol.Welcome welcome) {
        if (welcome.proto() < BridgeProtocol.VERSION) {
            return empty(Mode.FALLBACK, welcome.proto(), state.connectedAt);
        }
        if (welcome.proto() > BridgeProtocol.VERSION) {
            return empty(Mode.INCOMPATIBLE, welcome.proto(), state.connectedAt);
        }
        return empty(Mode.ACTIVE, welcome.proto(), state.connectedAt);
    }

    private static BridgeState metrics(
            BridgeState state, BridgeProtocol.HudSync sync, long now) {
        Optional<Metrics> current = state.metrics;
        if (current.isPresent() && current.orElseThrow().revision() >= sync.revision()) {
            return state;
        }
        Metrics next = new Metrics(
            sync.revision(), sync.ticksPerSecond(), sync.tickMilliseconds(), now);
        return new BridgeState(
            state.mode, state.serverProtocol, state.connectedAt, Optional.of(next), state.notice);
    }

    private static BridgeState notice(
            BridgeState state, BridgeProtocol.Notice incoming, long now) {
        ActiveNotice next = new ActiveNotice(
            incoming.id(), incoming.title(), incoming.body(), incoming.severity(),
            now, now + incoming.durationMillis());
        return new BridgeState(
            state.mode, state.serverProtocol, state.connectedAt, state.metrics, Optional.of(next));
    }

    private static BridgeState empty(Mode mode, int serverProtocol, long connectedAt) {
        return new BridgeState(
            mode, serverProtocol, connectedAt, Optional.empty(), Optional.empty());
    }
}
