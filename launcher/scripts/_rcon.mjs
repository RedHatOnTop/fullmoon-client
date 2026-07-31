/* _rcon.mjs — talk to the local 26.1.2 test server.

   The server runs headless out of a scheduled task, so its console is not
   reachable: without this there is no way to put armour on the probe player or
   give it an effect, and the gear and potion HUD modules can only be
   photographed as the empty case. */
import net from "node:net";

const LOGIN = 3;
const COMMAND = 2;
const AUTH_FAIL = -1;

function packet(id, type, body) {
  const payload = Buffer.from(body, "utf8");
  const buf = Buffer.alloc(14 + payload.length);
  buf.writeInt32LE(10 + payload.length, 0);
  buf.writeInt32LE(id, 4);
  buf.writeInt32LE(type, 8);
  payload.copy(buf, 12);
  return buf;
}

/** Runs the commands in order and resolves with one reply per command. */
export function rcon(commands, opts = {}) {
  const { host = "127.0.0.1", port = 25575, password = "pinion" } = opts;
  return new Promise((resolve, reject) => {
    const sock = net.createConnection({ host, port });
    const replies = [];
    let queue = [...commands];
    let rest = Buffer.alloc(0);
    let authed = false;

    sock.setTimeout(15_000, () => reject(new Error("rcon timeout")));
    sock.on("error", reject);
    sock.on("connect", () => sock.write(packet(1, LOGIN, password)));

    const next = () => {
      if (queue.length === 0) {
        sock.end();
        resolve(replies);
        return;
      }
      sock.write(packet(replies.length + 2, COMMAND, queue.shift()));
    };

    sock.on("data", (chunk) => {
      rest = Buffer.concat([rest, chunk]);
      while (rest.length >= 4 && rest.length >= rest.readInt32LE(0) + 4) {
        const size = rest.readInt32LE(0);
        const id = rest.readInt32LE(4);
        const body = rest.subarray(12, 4 + size - 2).toString("utf8");
        rest = rest.subarray(4 + size);
        if (!authed) {
          if (id === AUTH_FAIL) {
            reject(new Error("rcon password rejected"));
            return;
          }
          authed = true;
          next();
        } else {
          replies.push(body);
          next();
        }
      }
    });
  });
}
