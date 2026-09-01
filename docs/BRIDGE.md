# `fullmoon:v1` bridge protocol

`fullmoon:v1` is the public plugin-message contract between the Paper
`fullmoon-bridge` plugin and the Fullmoon Fabric mod. It selects a presentation layer; it never
changes server authority. Treat every client message as forged. Permissions, cooldowns, prices,
inventory effects, and every other gameplay rule remain server-side.

## Channel and framing

- Channel: `fullmoon:v1`, using Bukkit Messenger and Fabric custom payloads.
- A registered channel is only a candidate. A player is supported only after a valid handshake.
- The wire payload is one UTF-8 JSON object prefixed by Minecraft's unsigned VarInt byte length.
  The framing matches `FriendlyByteBuf.writeByteArray`. The server also accepts bare JSON from older
  clients.
- Payloads must fit the plugin-message packet limit of 32,767 bytes.
- Every object has a `type`. Operational server messages may omit `proto`, which defaults to `1`.

## Handshake

```text
C -> S  {"type":"hello","proto":1,"client":"fullmoon","version":"3.0.0"}
S -> C  {"type":"welcome","proto":1,"waypoints":[...]}
```

The client waits five seconds for `welcome`, then disables bridge-only presentation for that login
session. The server never opens a native surface for a player that has only registered the channel
without completing the handshake. There is no renegotiation within a session.

A protocol mismatch disables bridge features for the session and leaves the vanilla fallback active.
Additive fields that old readers can safely ignore do not require a protocol bump.

## Messages

| Direction | Type | Purpose |
|---|---|---|
| S -> C | `welcome` | Complete the handshake and provide the full waypoint snapshot. |
| S -> C | `waypoint_sync` | Replace the full waypoint snapshot. |
| C -> S | `tp_request` | Request a registered waypoint by opaque ID. |
| S -> C | `tp_result` | Report an accepted or rejected teleport request. |
| S -> C | `screen_open` | Open a named native surface. Protocol 1 defines `warp`. |
| S -> C | `menu_open` | Open or replace a server-owned native menu snapshot. |
| C -> S | `menu_action` | Request one action advertised by the current menu snapshot. |
| C -> S | `menu_close` | Notify the server that the current native menu was dismissed. |

### Waypoints

```json
{
  "id": "palace_gate",
  "name": "Fullmoon Palace Gate",
  "icon": "moon",
  "x": 500,
  "y": 72,
  "z": -140,
  "world": "lobby",
  "group": "palace",
  "perm": "warp.palace"
}
```

`perm` is a server permission key. Hiding a waypoint in the client is a convenience, not an access
check. The server accepts only IDs from its registry; arbitrary coordinates are not part of the
protocol.

### Server-owned menus

`menu_open` carries a complete immutable snapshot. Slots preserve the vanilla 9-column geometry so
existing server menu layouts remain recognizable while the Fullmoon client renders them as its own
screen.

```json
{
  "type": "menu_open",
  "proto": 1,
  "id": "2ac6f3ea-8f45-4d72-bb9c-cbded41b57d1",
  "revision": 4,
  "title": "Casino",
  "rows": 6,
  "items": [
    {
      "slot": 19,
      "label": "Coin Table",
      "material": "minecraft:gold_nugget",
      "count": 1,
      "details": ["Choose heads or tails", "Server-verified result"],
      "actions": ["left", "shift_left"]
    }
  ]
}
```

The client sends only the opaque session ID, revision, selected slot, and an advertised click:

```text
C -> S  {"type":"menu_action","id":"...","revision":4,"slot":19,"click":"left"}
C -> S  {"type":"menu_close","id":"...","revision":4}
```

Rules:

1. `id` is an unpredictable server-generated session identifier.
2. `revision` is monotonic. The server rejects stale or replayed requests.
3. The server accepts only a slot and click combination advertised in that exact snapshot.
4. The server re-runs all domain validation before changing state. Menu data is not authorization.
5. A successful action produces a fresh snapshot or a `menu_close`; the client does not predict the
   result.
6. `rows` is `1..6`, slots are within `rows * 9`, and duplicate slots are invalid.
7. Supported clicks in protocol 1 are `left` and `shift_left`.

Casino, shops, selling, enhancement, potential, guild, mail, titles, raid selection, kit selection,
tutorial prompts, crafting, and lift selection use this native path. A synchronized player-to-player
item trade remains a real inventory container because it transfers item stacks rather than selecting
server menu commands.

## Vanilla fallback

- `/warp` lists and executes the same registered waypoint IDs through the same permission and
  cooldown path as `tp_request`.
- Every server-owned menu retains its ChestGUI inventory. The server opens it when the player has no
  completed handshake, the bridge is unavailable, or a menu snapshot exceeds the channel limit.
- Native and fallback surfaces expose the same actions. Client detection never changes the feature
  set.

## Server authority requirements

1. Validate waypoint permission, the shared 4,000 ms cooldown, world, and registry coordinates at
   execution time.
2. Use registration and handshake state only to choose a rendering surface.
3. Return `tp_result{ok:false}` for every denied teleport so the client does not infer outcomes.
4. Validate menu IDs, revisions, slots, click types, and the underlying domain operation.
5. Log handshake and rejected or completed gameplay requests for debugging and abuse analysis.

## Implementation notes

Bukkit silently discards a server payload sent before the player registers the channel. The server
therefore waits asynchronously for registration before replying to `hello`; it must not block the
main thread, because channel registration is processed there. The client may send `hello` from
`ClientPlayConnectionEvents.JOIN`; the server-side wait absorbs that race.
