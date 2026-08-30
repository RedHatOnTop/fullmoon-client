package dev.fullmoon.client.network;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import dev.fullmoon.client.FullmoonClient;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FullmoonChannel {
    private static final Logger LOG = LoggerFactory.getLogger("Fullmoon/Channel");
    private static final CustomPacketPayload.Type<Envelope> TYPE =
        new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(FullmoonClient.NAMESPACE, "v1"));
    private static final StreamCodec<FriendlyByteBuf, Envelope> CODEC = StreamCodec.of(
        (buffer, envelope) -> buffer.writeByteArray(envelope.data()),
        buffer -> new Envelope(buffer.readByteArray(BridgeProtocol.MAX_PAYLOAD_BYTES)));
    private static final AtomicReference<BridgeState> STATE =
        new AtomicReference<>(BridgeState.disconnected());

    private FullmoonChannel() {}

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);

        ClientPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) ->
            context.client().execute(() -> receive(payload.data())));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
            client.execute(FullmoonChannel::connect));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            STATE.set(BridgeState.disconnected()));
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    public static Optional<BridgeState.Metrics> metrics(long now) {
        return BridgeState.liveMetrics(STATE.get(), now);
    }

    public static Optional<BridgeState.ActiveNotice> notice(long now) {
        return BridgeState.liveNotice(STATE.get(), now);
    }

    public static BridgeState state() {
        return STATE.get();
    }

    private static void connect() {
        STATE.set(BridgeState.connected(System.currentTimeMillis()));
        try {
            ClientPlayNetworking.send(new Envelope(BridgeProtocol.hello(clientVersion())));
            LOG.info("Sent fullmoon:v1 hello (proto {})", BridgeProtocol.VERSION);
        } catch (RuntimeException error) {
            LOG.error("Failed to send fullmoon:v1 hello", error);
        }
    }

    private static void receive(byte[] payload) {
        BridgeProtocol.DecodeResult decoded = BridgeProtocol.decode(payload);
        if (decoded.error().isPresent()) {
            LOG.warn("Rejected fullmoon:v1 payload: {}", decoded.error().orElseThrow());
            return;
        }

        BridgeProtocol.Message message = decoded.message().orElseThrow();
        BridgeState before = STATE.get();
        BridgeState after = BridgeState.apply(before, message, System.currentTimeMillis());
        STATE.set(after);

        if (message instanceof BridgeProtocol.Welcome welcome) {
            LOG.info("Received fullmoon:v1 welcome (server proto {}, mode {})",
                welcome.proto(), after.mode());
        } else if (message instanceof BridgeProtocol.HudSync sync && after != before) {
            if (before.metrics().isEmpty()) {
                LOG.info("Received fullmoon:v1 HUD revision {} ({} TPS, {} ms)",
                    sync.revision(), sync.ticksPerSecond(), sync.tickMilliseconds());
            } else {
                LOG.debug("Applied fullmoon:v1 HUD revision {}", sync.revision());
            }
        } else if (message instanceof BridgeProtocol.Notice notice && after != before) {
            LOG.info("Received fullmoon:v1 notice {}", notice.id());
        } else if (message instanceof BridgeProtocol.Unknown unknown) {
            LOG.debug("Ignored fullmoon:v1 payload type {}", unknown.type());
        }
    }

    private static String clientVersion() {
        return FabricLoader.getInstance().getModContainer(FullmoonClient.NAMESPACE)
            .orElseThrow(() -> new IllegalStateException("Fullmoon mod container is missing"))
            .getMetadata()
            .getVersion()
            .getFriendlyString();
    }

    private static void tick() {
        BridgeState before = STATE.get();
        BridgeState after = BridgeState.tick(before, System.currentTimeMillis());
        if (after != before && STATE.compareAndSet(before, after)
                && after.mode() == BridgeState.Mode.FALLBACK) {
            LOG.info("No fullmoon:v1 welcome within 5 seconds; using vanilla fallback");
        }
    }

    public record Envelope(byte[] data) implements CustomPacketPayload {
        public Envelope {
            data = data.clone();
        }

        @Override
        public byte[] data() {
            return data.clone();
        }

        @Override
        public Type<Envelope> type() {
            return TYPE;
        }
    }
}
