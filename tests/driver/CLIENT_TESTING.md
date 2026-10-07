# Driving the graphical client (for testing)

The client runs on a **private virtual display** (Xvfb + software GL). It never opens a window on the host desktop.
Add `-PclientTest.hostDisplay` to the gradle command only if a human wants to watch.

## Start / stop

    tests/driver/client-driver.sh start      # blocks until READY (30-120 s)
    tests/driver/client-driver.sh cmd 'ui'   # same as: curl -s localhost:8765 -d 'ui'
    tests/driver/client-driver.sh stop

Or directly: `gradle tests:clientDriver --console=plain`.

Commands are strictly sequential. Every command returns plain text; failures return `ERROR: ...`.

## Loop: look, act, look

1. `shot name` writes `tests/build/client_test_output/name.png` and prints the path. Open the image to see the screen.
2. `ui` prints visible scene2d elements with centers in **screenshot pixels**. `*` marks interactive ones. Prefer this over guessing coordinates from the image.
3. Act with `clicktext`, `click`, `key`, `type`, ...
4. `shot` again, or `ui`, and check the result. Run `errors` regularly: the game logs many failures instead of throwing.

## Commands

| Command | Effect |
|---|---|
| `shot [name]` | save screenshot, print path |
| `ui` | dump visible UI tree (names, text, centers, DISABLED) |
| `find <text>` | elements whose text or name contains `<text>` |
| `state` | game state, map, tick, open dialog |
| `errors` | error-level log lines since the last call (and clears them) |
| `size` | framebuffer size |
| `getblock x y` | print tile information at tile coordinates, or `null` if outside the world |
| `placeblock x y <block-name> [rotation]` | queue the named block for the living player unit to build (rotation defaults to 0) |
| `click x y [right]`, `move x y`, `drag x1 y1 x2 y2`, `scroll dy` | mouse, in screenshot pixels (top-left origin) |
| `clicktext <text>` | click the button/label containing `<text>`; fails with a reason if absent or covered |
| `key <KeyCode> [down\|up]` | e.g. `key escape`, `key w down`. Names are `arc.input.KeyCode` constants |
| `type <text>` | type into the focused text field (click it first) |
| `frames [n]` | let n frames render (default 1) |
| `freeze` / `thaw` | stop / resume game time. UI animations still run, so dialogs still open and close |
| `menu` | close dialogs, return to main menu |
| `map <internal name>` | play an internal map, e.g. `map serpulo/groundZero` |
| `connect [host] [port]` | join a server the way the join dialog does (default localhost, 6567) |
| `disconnect` | leave the server and return to the menu |
| `net` | connection state: client/active, player count, own name and id |
| `chat` | chat messages received so far, newest first, one per line |
| `say <text>` | send a chat message to the server |
| `js <code>` | run JavaScript on the render thread with the whole game in scope |
| `quit` | shut down (also stops Xvfb) |

The game keeps running between commands. `freeze` before inspecting anything that moves.

## Campaign and anything without a command: use `js`

    js Vars.control.playSector(Vars.state.rules.sector)           // example shape only, check the API
    js Vars.ui.planet.show()
    js Vars.ui.research.show()
    js Vars.state.rules.defaultTeam.core().items.add(Vars.content.item("copper"), 500)

Anything you work out interactively is a good candidate for a test (below).

## Turning a session into a test

Every command above is a thin wrapper over `ClientHarness` / `ClientInput` / `UiDump`, so a recorded session maps 1:1:

```java
public class ClientMenuFlowTests extends ClientTestBase{
    @Test
    void settingsOpensFromMainMenu(){
        ClientHarness.call(() -> UiDump.findClickable("Settings"));          // fails with a useful message if absent
        UiDump.Hit h = ClientHarness.call(() -> UiDump.findClickable("Settings"));
        ClientInput.click(h.cx(), h.cy());
        ClientHarness.waitUntil("settings dialog", () -> ui.settings.isShown(), 300);
        assertFrameNotBlank("settings_open");
    }
}
```

Rules: assert on game/UI **state**, not on pixels, wherever possible. Use screenshots as evidence and for
`assertFrameNotBlank`, not for pixel-exact comparison (software GL output is stable on one machine, not across Mesa versions).
`ClientTestBase` already fails any test during which the client logged an error.
Run with `gradle tests:clientTest --tests ClientMenuFlowTests`.

## Notes

* The `js` command executes arbitrary code in the game. The server binds to 127.0.0.1 only; keep it that way.
* HiDPI scaling is not handled. Under Xvfb the scale is 1, so pixel coordinates are exact.
* `type` bypasses SDL text events and delivers characters straight to the Scene.
