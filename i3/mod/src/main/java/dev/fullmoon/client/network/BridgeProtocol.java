package dev.fullmoon.client.network;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class BridgeProtocol {
    public static final int VERSION = 1;
    public static final int MAX_PAYLOAD_BYTES = 32_767;

    private static final int MAX_CLIENT_VERSION_LENGTH = 32;
    private static final int MAX_NOTICE_ID_LENGTH = 64;
    private static final int MAX_NOTICE_TITLE_LENGTH = 64;
    private static final int MAX_NOTICE_BODY_LENGTH = 160;
    private static final int MIN_NOTICE_DURATION_MILLIS = 1_000;
    private static final int MAX_NOTICE_DURATION_MILLIS = 10_000;

    private BridgeProtocol() {}

    public sealed interface Message permits Hello, Welcome, HudSync, Notice, Unknown {
        int proto();
    }

    public record Hello(int proto, String client, String version) implements Message {}

    public record Welcome(int proto) implements Message {}

    public record HudSync(
            int proto,
            long revision,
            double ticksPerSecond,
            double tickMilliseconds) implements Message {}

    public record Notice(
            int proto,
            String id,
            String title,
            String body,
            Severity severity,
            int durationMillis) implements Message {}

    public record Unknown(int proto, String type) implements Message {}

    public enum Severity {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    public record DecodeResult(Optional<Message> message, Optional<String> error) {
        public DecodeResult {
            Objects.requireNonNull(message, "message");
            Objects.requireNonNull(error, "error");
            if (message.isPresent() == error.isPresent()) {
                throw new IllegalArgumentException("decode result must contain either a message or an error");
            }
        }

        private static DecodeResult success(Message message) {
            return new DecodeResult(Optional.of(message), Optional.empty());
        }

        private static DecodeResult failure(String error) {
            return new DecodeResult(Optional.empty(), Optional.of(error));
        }
    }

    public static byte[] hello(String clientVersion) {
        String error = helloError(clientVersion);
        if (!error.isEmpty()) {
            throw new IllegalArgumentException(error);
        }

        JsonObject json = new JsonObject();
        json.addProperty("type", "hello");
        json.addProperty("proto", VERSION);
        json.addProperty("client", "fullmoon");
        json.addProperty("version", clientVersion);
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    static String helloError(String clientVersion) {
        if (clientVersion == null || clientVersion.isBlank()) {
            return "client version is required";
        }
        if (clientVersion.length() > MAX_CLIENT_VERSION_LENGTH) {
            return "client version is too long";
        }
        return "";
    }

    public static DecodeResult decode(byte[] payload) {
        if (payload == null) {
            return DecodeResult.failure("payload is required");
        }
        if (payload.length > MAX_PAYLOAD_BYTES) {
            return DecodeResult.failure("payload exceeds 32767 bytes");
        }

        JsonObject json = parseObject(payload);
        if (json == null) {
            return DecodeResult.failure("payload is not a JSON object");
        }

        String type = string(json, "type");
        if (type.isEmpty()) {
            return DecodeResult.failure("payload type is required");
        }

        return switch (type) {
            case "hello" -> decodeHello(json);
            case "welcome" -> decodeWelcome(json);
            case "hud_sync" -> decodeHudSync(json);
            case "notice" -> decodeNotice(json);
            default -> decodeUnknown(json, type);
        };
    }

    private static DecodeResult decodeHello(JsonObject json) {
        Integer proto = integer(json, "proto");
        if (proto == null || proto < 0) {
            return DecodeResult.failure("hello proto must be a non-negative integer");
        }
        String client = string(json, "client");
        String version = string(json, "version");
        if (client.isEmpty()) {
            return DecodeResult.failure("hello client is required");
        }
        String versionError = helloError(version);
        if (!versionError.isEmpty()) {
            return DecodeResult.failure(versionError);
        }
        return DecodeResult.success(new Hello(proto, client, version));
    }

    private static DecodeResult decodeWelcome(JsonObject json) {
        Integer proto = integer(json, "proto");
        if (proto == null || proto < 0) {
            return DecodeResult.failure("welcome proto must be a non-negative integer");
        }
        return DecodeResult.success(new Welcome(proto));
    }

    private static DecodeResult decodeHudSync(JsonObject json) {
        Integer proto = integer(json, "proto");
        Long revision = longInteger(json, "revision");
        Double tps = finiteNumber(json, "tps");
        Double tickMillis = finiteNumber(json, "tick_ms");

        if (proto == null || proto < 0) {
            return DecodeResult.failure("hud_sync proto must be a non-negative integer");
        }
        if (revision == null || revision < 0) {
            return DecodeResult.failure("hud_sync revision must be non-negative");
        }
        if (tps == null || tps < 0 || tps > 20) {
            return DecodeResult.failure("hud_sync tps must be between 0 and 20");
        }
        if (tickMillis == null || tickMillis < 0 || tickMillis > 1_000) {
            return DecodeResult.failure("hud_sync tick_ms must be between 0 and 1000");
        }
        return DecodeResult.success(new HudSync(proto, revision, tps, tickMillis));
    }

    private static DecodeResult decodeNotice(JsonObject json) {
        Integer proto = integer(json, "proto");
        Integer duration = integer(json, "duration_ms");
        String id = string(json, "id");
        String title = string(json, "title");
        String body = string(json, "body");
        String severityName = string(json, "severity");

        if (proto == null || proto < 0) {
            return DecodeResult.failure("notice proto must be a non-negative integer");
        }
        String textError = noticeTextError(id, title, body);
        if (!textError.isEmpty()) {
            return DecodeResult.failure(textError);
        }

        Severity severity;
        try {
            severity = Severity.valueOf(severityName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            return DecodeResult.failure("notice severity is invalid");
        }

        if (duration == null
                || duration < MIN_NOTICE_DURATION_MILLIS
                || duration > MAX_NOTICE_DURATION_MILLIS) {
            return DecodeResult.failure("notice duration_ms must be between 1000 and 10000");
        }
        return DecodeResult.success(new Notice(proto, id, title, body, severity, duration));
    }

    private static String noticeTextError(String id, String title, String body) {
        if (id.isEmpty()) {
            return "notice id is required";
        }
        if (id.length() > MAX_NOTICE_ID_LENGTH) {
            return "notice id is too long";
        }
        if (title.isEmpty()) {
            return "notice title is required";
        }
        if (title.length() > MAX_NOTICE_TITLE_LENGTH) {
            return "notice title is too long";
        }
        if (body.isEmpty()) {
            return "notice body is required";
        }
        if (body.length() > MAX_NOTICE_BODY_LENGTH) {
            return "notice body is too long";
        }
        return "";
    }

    private static DecodeResult decodeUnknown(JsonObject json, String type) {
        Integer proto = integer(json, "proto");
        return DecodeResult.success(new Unknown(proto == null ? 0 : proto, type));
    }

    private static JsonObject parseObject(byte[] payload) {
        JsonObject direct = parseJsonObject(payload, 0, payload.length);
        if (direct != null) {
            return direct;
        }

        Frame frame = readFrame(payload);
        if (frame == null) {
            return null;
        }
        return parseJsonObject(payload, frame.offset(), frame.length());
    }

    private static JsonObject parseJsonObject(byte[] payload, int offset, int length) {
        try {
            JsonElement parsed = JsonParser.parseString(
                new String(payload, offset, length, StandardCharsets.UTF_8));
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (RuntimeException error) {
            return null;
        }
    }

    private static Frame readFrame(byte[] payload) {
        int value = 0;
        int shift = 0;
        for (int index = 0; index < Math.min(5, payload.length); index++) {
            int current = payload[index] & 0xFF;
            value |= (current & 0x7F) << shift;
            if ((current & 0x80) == 0) {
                int offset = index + 1;
                return value == payload.length - offset ? new Frame(offset, value) : null;
            }
            shift += 7;
        }
        return null;
    }

    private static Integer integer(JsonObject json, String key) {
        Long value = longInteger(json, key);
        if (value == null || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            return null;
        }
        return value.intValue();
    }

    private static Long longInteger(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            return null;
        }
        try {
            double value = json.get(key).getAsDouble();
            if (!Double.isFinite(value) || value != Math.rint(value)) {
                return null;
            }
            return json.get(key).getAsLong();
        } catch (RuntimeException error) {
            return null;
        }
    }

    private static Double finiteNumber(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            return null;
        }
        try {
            double value = json.get(key).getAsDouble();
            return Double.isFinite(value) ? value : null;
        } catch (RuntimeException error) {
            return null;
        }
    }

    private static String string(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            return "";
        }
        try {
            return json.get(key).getAsString().trim();
        } catch (RuntimeException error) {
            return "";
        }
    }

    private record Frame(int offset, int length) {}
}
