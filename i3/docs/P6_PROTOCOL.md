# P6 — Versioned server channel

P6 connects the i3 Fabric client to the Fullmoon Paper bridge over the `fullmoon:v1` custom
payload channel. The wire contract is deliberately small: the channel establishes compatibility,
then carries measured server health and short operational notices. It does not replace vanilla
join or gameplay traffic.

## Wire envelope

The Fabric payload codec writes one Minecraft `byte[]`: a VarInt length followed by UTF-8 JSON.
`BridgeProtocol.decode` also accepts bare UTF-8 JSON so fixtures and captured payloads can be
replayed without a Minecraft buffer. Payloads larger than 32,767 bytes are rejected.

Every message has a `type` and a non-negative `proto`. Protocol version 1 defines four messages:

```json
{"type":"hello","proto":1,"client":"fullmoon","version":"3.0.0"}
{"type":"welcome","proto":1}
{"type":"hud_sync","proto":1,"revision":1,"tps":19.96,"tick_ms":8.36}
{"type":"notice","proto":1,"id":"maintenance","title":"Network notice","body":"Restart in five minutes.","severity":"warning","duration_ms":5000}
```

`hud_sync` accepts TPS from 0 through 20, tick duration from 0 through 1,000 milliseconds, and a
non-negative revision. Revisions must increase; duplicates and older samples are ignored. A sample
is live for five seconds. Once stale, the HUD prints `—` rather than retaining a value that is no
longer supported by the server.

A notice requires an identifier, title, body, one of `info`, `success`, `warning`, or `error`, and
a duration from 1,000 through 10,000 milliseconds. The client renders one active notice, replacing
it immutably when a newer notice arrives. Severity changes only the two-pixel rule; it does not add
an icon, animation, sound, or celebratory treatment.

## Handshake and compatibility

1. On join, the client enters `WAITING` and sends `hello` with its installed mod version.
2. A `welcome` at protocol 1 enters `ACTIVE`.
3. A lower server protocol enters `FALLBACK`; a higher server protocol enters `INCOMPATIBLE`.
4. No `welcome` within five seconds enters `FALLBACK`.
5. Disconnect clears the protocol, metrics, and notice state.

Only an active channel can apply HUD samples or notices, and their `proto` must match the accepted
server protocol. Unknown message types are ignored after decoding. Malformed, oversized, or
out-of-range input is rejected without changing the last valid state.

Fallback is silent and leaves vanilla play intact. Unsupported or absent metrics display `TPS —`;
the client never manufactures a nominal `20.0 TPS` value.

## Source boundaries

- `network/BridgeProtocol.java` owns framing, typed messages, and validation.
- `network/BridgeState.java` owns the immutable compatibility, freshness, and notice state machine.
- `network/FullmoonChannel.java` owns Fabric registration and client lifecycle events.
- `hud/ServerTickHud.java` reads live metrics.
- `hud/ServerNoticeOverlay.java` draws the current notice with the existing design tokens.

The protocol and state machine are covered by unit tests. The Fabric transport and rendering path
were verified against a local Paper server; the exact round-trip and fallback transcripts are in
`docs/evidence/`.
