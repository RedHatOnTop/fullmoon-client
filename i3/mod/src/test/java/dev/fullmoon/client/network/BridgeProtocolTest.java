package dev.fullmoon.client.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;

final class BridgeProtocolTest {
    @Test
    void helloCarriesTheCurrentProtocolAndValidatedVersion() {
        byte[] encoded = BridgeProtocol.hello("3.0.0");
        BridgeProtocol.DecodeResult decoded = BridgeProtocol.decode(encoded);

        BridgeProtocol.Hello hello = assertInstanceOf(
            BridgeProtocol.Hello.class, decoded.message().orElseThrow());
        assertEquals(BridgeProtocol.VERSION, hello.proto());
        assertEquals("fullmoon", hello.client());
        assertEquals("3.0.0", hello.version());
    }

    @Test
    void helloRejectsBlankOrOversizedVersions() {
        assertEquals("client version is required", BridgeProtocol.helloError(" "));
        assertEquals("client version is required", BridgeProtocol.helloError(null));
        assertEquals("client version is too long", BridgeProtocol.helloError("x".repeat(33)));
        assertEquals("client version is required",
            assertThrows(IllegalArgumentException.class, () -> BridgeProtocol.hello(" ")).getMessage());
    }

    @Test
    void decoderAcceptsBareAndMinecraftFramedJson() {
        byte[] bare = json("""
            {"type":"welcome","proto":1}
            """);
        byte[] framed = frame(bare);

        BridgeProtocol.Welcome first = assertInstanceOf(
            BridgeProtocol.Welcome.class, BridgeProtocol.decode(bare).message().orElseThrow());
        BridgeProtocol.Welcome second = assertInstanceOf(
            BridgeProtocol.Welcome.class, BridgeProtocol.decode(framed).message().orElseThrow());
        assertEquals(first, second);
    }

    @Test
    void decoderRejectsMalformedAndOversizedPayloads() {
        BridgeProtocol.DecodeResult absent = BridgeProtocol.decode(null);
        BridgeProtocol.DecodeResult malformed = BridgeProtocol.decode(json("not-json"));
        BridgeProtocol.DecodeResult oversized = BridgeProtocol.decode(
            new byte[BridgeProtocol.MAX_PAYLOAD_BYTES + 1]);

        assertEquals("payload is required", absent.error().orElseThrow());
        assertEquals("payload is not a JSON object", malformed.error().orElseThrow());
        assertEquals("payload exceeds 32767 bytes", oversized.error().orElseThrow());
        assertTrue(malformed.message().isEmpty());
        assertTrue(oversized.message().isEmpty());
    }

    @Test
    void decoderRequiresAnObjectType() {
        assertEquals("payload is not a JSON object", decodeError("[]"));
        assertEquals("payload type is required", decodeError("{}"));
        assertEquals("payload type is required", decodeError("{\"type\":null}"));
    }

    @Test
    void handshakeMessagesValidateEveryRequiredField() {
        assertEquals("hello proto must be a non-negative integer", decodeError("""
            {"type":"hello","client":"fullmoon","version":"3.0.0"}
            """));
        assertEquals("hello proto must be a non-negative integer", decodeError("""
            {"type":"hello","proto":-1,"client":"fullmoon","version":"3.0.0"}
            """));
        assertEquals("hello client is required", decodeError("""
            {"type":"hello","proto":1,"version":"3.0.0"}
            """));
        assertEquals("client version is too long", decodeError(String.format("""
            {"type":"hello","proto":1,"client":"fullmoon","version":"%s"}
            """, "x".repeat(33))));
        assertEquals("welcome proto must be a non-negative integer",
            decodeError("{\"type\":\"welcome\"}"));
        assertEquals("welcome proto must be a non-negative integer",
            decodeError("{\"type\":\"welcome\",\"proto\":-1}"));
    }

    @Test
    void decoderRejectsAnInvalidFrameInsteadOfReadingPastIt() {
        byte[] body = json("{\"type\":\"welcome\",\"proto\":1}");
        byte[] framed = frame(body);
        framed[0] = (byte) (framed[0] + 1);

        BridgeProtocol.DecodeResult result = BridgeProtocol.decode(framed);

        assertTrue(result.message().isEmpty());
        assertEquals("payload is not a JSON object", result.error().orElseThrow());
    }

    @Test
    void hudSyncRequiresFiniteBoundedMetricsAndRevision() {
        BridgeProtocol.HudSync sync = assertInstanceOf(
            BridgeProtocol.HudSync.class,
            BridgeProtocol.decode(json("""
                {"type":"hud_sync","proto":1,"revision":7,"tps":19.8,"tick_ms":12.4}
                """)).message().orElseThrow());

        assertEquals(7, sync.revision());
        assertEquals(19.8, sync.ticksPerSecond());
        assertEquals(12.4, sync.tickMilliseconds());

        assertEquals("hud_sync tps must be between 0 and 20",
            BridgeProtocol.decode(json("""
                {"type":"hud_sync","proto":1,"revision":7,"tps":20.1,"tick_ms":12.4}
                """)).error().orElseThrow());
        assertEquals("hud_sync tick_ms must be between 0 and 1000",
            BridgeProtocol.decode(json("""
                {"type":"hud_sync","proto":1,"revision":7,"tps":19.8,"tick_ms":-1}
                """)).error().orElseThrow());
        assertEquals("hud_sync revision must be non-negative",
            BridgeProtocol.decode(json("""
                {"type":"hud_sync","proto":1,"revision":-1,"tps":19.8,"tick_ms":12.4}
                """)).error().orElseThrow());
        assertEquals("hud_sync proto must be a non-negative integer", decodeError("""
            {"type":"hud_sync","revision":7,"tps":19.8,"tick_ms":12.4}
            """));
        assertEquals("hud_sync revision must be non-negative", decodeError("""
            {"type":"hud_sync","proto":1,"revision":1.5,"tps":19.8,"tick_ms":12.4}
            """));
        assertEquals("hud_sync tps must be between 0 and 20", decodeError("""
            {"type":"hud_sync","proto":1,"revision":7,"tps":"NaN","tick_ms":12.4}
            """));
        assertEquals("hud_sync tick_ms must be between 0 and 1000", decodeError("""
            {"type":"hud_sync","proto":1,"revision":7,"tps":19.8,"tick_ms":"bad"}
            """));
    }

    @Test
    void noticeValidatesIdentityCopySeverityAndDuration() {
        BridgeProtocol.Notice notice = assertInstanceOf(
            BridgeProtocol.Notice.class,
            BridgeProtocol.decode(json("""
                {"type":"notice","proto":1,"id":"restart","title":"Server restart",
                 "body":"Lobby restarts in five minutes.","severity":"warning","duration_ms":6000}
                """)).message().orElseThrow());

        assertEquals("restart", notice.id());
        assertEquals("Server restart", notice.title());
        assertEquals("Lobby restarts in five minutes.", notice.body());
        assertEquals(BridgeProtocol.Severity.WARNING, notice.severity());
        assertEquals(6000, notice.durationMillis());

        assertEquals("notice title is required", decodeError("""
            {"type":"notice","proto":1,"id":"restart","title":" ","body":"Body",
             "severity":"info","duration_ms":5000}
            """));
        assertEquals("notice severity is invalid", decodeError("""
            {"type":"notice","proto":1,"id":"restart","title":"Title","body":"Body",
             "severity":"purple","duration_ms":5000}
            """));
        assertEquals("notice duration_ms must be between 1000 and 10000", decodeError("""
            {"type":"notice","proto":1,"id":"restart","title":"Title","body":"Body",
             "severity":"info","duration_ms":30000}
            """));
        assertEquals("notice proto must be a non-negative integer", decodeError("""
            {"type":"notice","id":"restart","title":"Title","body":"Body",
             "severity":"info","duration_ms":5000}
            """));
        assertEquals("notice id is required", decodeError("""
            {"type":"notice","proto":1,"title":"Title","body":"Body",
             "severity":"info","duration_ms":5000}
            """));
        assertEquals("notice id is too long", decodeError(String.format("""
            {"type":"notice","proto":1,"id":"%s","title":"Title","body":"Body",
             "severity":"info","duration_ms":5000}
            """, "x".repeat(65))));
        assertEquals("notice title is too long", decodeError(String.format("""
            {"type":"notice","proto":1,"id":"id","title":"%s","body":"Body",
             "severity":"info","duration_ms":5000}
            """, "x".repeat(65))));
        assertEquals("notice body is required", decodeError("""
            {"type":"notice","proto":1,"id":"id","title":"Title",
             "severity":"info","duration_ms":5000}
            """));
        assertEquals("notice body is too long", decodeError(String.format("""
            {"type":"notice","proto":1,"id":"id","title":"Title","body":"%s",
             "severity":"info","duration_ms":5000}
            """, "x".repeat(161))));
        assertEquals("notice duration_ms must be between 1000 and 10000", decodeError("""
            {"type":"notice","proto":1,"id":"id","title":"Title","body":"Body",
             "severity":"success","duration_ms":2147483648}
            """));
    }

    @Test
    void unknownTypesRemainHarmlessAndObservable() {
        BridgeProtocol.Unknown unknown = assertInstanceOf(
            BridgeProtocol.Unknown.class,
            BridgeProtocol.decode(json("{\"type\":\"future_surface\",\"proto\":1}"))
                .message().orElseThrow());

        assertEquals("future_surface", unknown.type());
        assertEquals(1, unknown.proto());

        BridgeProtocol.Unknown unversioned = assertInstanceOf(
            BridgeProtocol.Unknown.class,
            BridgeProtocol.decode(json("{\"type\":\"future_surface\"}"))
                .message().orElseThrow());
        assertEquals(0, unversioned.proto());
    }

    @Test
    void framingUsesTheFullMinecraftVarintRange() {
        byte[] body = new byte[300];
        byte[] framed = frame(body);

        assertArrayEquals(new byte[] { (byte) 0xAC, 0x02 }, new byte[] { framed[0], framed[1] });
        assertEquals(302, framed.length);
        assertFalse(framed[0] == 44);
    }

    @Test
    void anUnterminatedVarintIsRejected() {
        byte[] invalid = {
            (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80
        };

        assertEquals("payload is not a JSON object",
            BridgeProtocol.decode(invalid).error().orElseThrow());
    }

    @Test
    void decodeResultCannotContainBothOrNeitherOutcome() {
        assertThrows(IllegalArgumentException.class, () -> new BridgeProtocol.DecodeResult(
            Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new BridgeProtocol.DecodeResult(
            Optional.of(new BridgeProtocol.Welcome(1)), Optional.of("error")));
        assertThrows(NullPointerException.class, () -> new BridgeProtocol.DecodeResult(
            null, Optional.of("error")));
        assertThrows(NullPointerException.class, () -> new BridgeProtocol.DecodeResult(
            Optional.empty(), null));
    }

    private static String decodeError(String text) {
        return BridgeProtocol.decode(json(text)).error().orElseThrow();
    }

    private static byte[] json(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] frame(byte[] body) {
        byte[] prefix = new byte[5];
        int value = body.length;
        int size = 0;
        while ((value & ~0x7F) != 0) {
            prefix[size++] = (byte) ((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        prefix[size++] = (byte) value;

        byte[] framed = new byte[size + body.length];
        System.arraycopy(prefix, 0, framed, 0, size);
        System.arraycopy(body, 0, framed, size, body.length);
        return framed;
    }
}
