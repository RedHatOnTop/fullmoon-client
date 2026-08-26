package dev.pinion.bridge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Discord auto-link — the launcher↔mod↔server chain.
 *
 * The launcher's Discord OAuth (identify scope) produces the player's Discord
 * user id. It drops that id into `<instance>/pinion/discord.json`; this class
 * picks it up, and when the server pushes a `link_prompt` (the player ran
 * /link and has a fresh code) the mod sends
 * `{type:"link_discord", code, discord_id}` over the bridge channel. The
 * server's answer (`link_result`) is shown as a chat line and the file's
 * one-shot marker is cleared.
 *
 * The file is written by the LAUNCHER, read by the MOD, and never contains
 * anything but the id — the code never touches disk (it arrives over the
 * channel and goes straight back over the channel).
 *
 * Shape: {"discord_id": "123456789012345678", "linked": false}
 * The launcher flips `linked` to true when it sees a successful link_result
 * echoed back through its own channel; the mod only ever reads discord_id.
 */
public final class DiscordLink {

    private static final Path FILE = Path.of("pinion", "discord.json");
    private static volatile String discordId;
    private static volatile boolean loaded;

    private DiscordLink() {
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            if (!Files.isReadable(FILE)) {
                return;
            }
            String text = Files.readString(FILE, StandardCharsets.UTF_8);
            var el = JsonParser.parseString(text);
            if (el.isJsonObject() && el.getAsJsonObject().has("discord_id")) {
                discordId = el.getAsJsonObject().get("discord_id").getAsString();
            }
        } catch (Exception ignored) {
            // no file, bad json — the player just links the manual way
        }
    }

    /** True when the launcher has given us a Discord id to link with. */
    public static boolean available() {
        load();
        return discordId != null && discordId.matches("\\d{1,20}");
    }

    /** Send the code + id to the server. No-op without either piece. */
    public static void sendLink(String code) {
        load();
        if (discordId == null || code == null || code.isEmpty()) {
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("type", "link_discord");
        json.addProperty("code", code.trim());
        json.addProperty("discord_id", discordId);
        FullmoonBridge.sendRaw(json);
    }

    /** The server's `link_result`, rendered as chat by the bridge handler. */
    public static void handleResult(com.google.gson.JsonObject json) {
        boolean ok = json.has("ok") && json.get("ok").getAsBoolean();
        String reason = json.has("reason") && json.get("reason").isJsonPrimitive()
                ? json.get("reason").getAsString() : "";
        String username = json.has("mc_username") && json.get("mc_username").isJsonPrimitive()
                ? json.get("mc_username").getAsString() : null;
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        String message = ok
                ? ("§a디스코드 연동 완료" + (username != null ? " — " + username : "")
                    + "§7 (가입 보너스 +2만원은 디스코드 /지갑에서 확인)")
                : switch (reason) {
                    case "invalid-format" -> "§c연동 실패: 코드 형식이 맞지 않습니다.";
                    case "not-found" -> "§c연동 실패: 그런 코드가 없습니다.";
                    case "expired" -> "§c연동 실패: 코드가 만료되었습니다. /link로 다시 발급하세요.";
                    case "discord-already-linked" -> "§c이 디스코드 계정은 이미 다른 마인크래프트 계정과 연동되어 있습니다.";
                    case "mc-already-linked" -> "§c이 마인크래프트 계정은 이미 다른 디스코드 계정과 연동되어 있습니다.";
                    case "not-your-code" -> "§c이 코드는 다른 플레이어에게 발급된 코드입니다.";
                    case "unavailable" -> "§c연동 서비스를 사용할 수 없습니다. /링크코드로 수동 연동해 주세요.";
                    default -> "§c연동에 실패했습니다. 잠시 후 다시 시도해 주세요.";
                };
        mc.player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(message));
    }
}
