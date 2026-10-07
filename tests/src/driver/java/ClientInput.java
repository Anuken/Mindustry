import arc.*;
import arc.backend.sdl.*;
import arc.input.*;
import org.lwjgl.sdl.*;
import org.lwjgl.system.*;

import java.util.function.*;

import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLMouse.*;

/**
 * Injects real SDL input events, so they travel the exact path physical input does:
 * SDL queue -> SdlInput -> InputEventQueue -> InputMultiplexer -> Scene / InputHandler.
 * <p>
 * Coordinates are screenshot pixels: origin top-left, same as {@link ClientHarness#capture()} and SDL.
 * (Arc itself uses a bottom-left origin internally; SdlInput does the flip, so we must not.)
 * <p>
 * SDL_PushEvent is thread safe, so these can be called from the driver/test thread. Each method waits a couple of
 * frames afterwards so the event has been consumed before returning.
 */
public final class ClientInput{
    private static int[] scancodes;

    private ClientInput(){}

    private static void push(Consumer<SDL_Event> fill){
        try(MemoryStack stack = MemoryStack.stackPush()){
            SDL_Event e = SDL_Event.calloc(stack);
            fill.accept(e);
            SDL_PushEvent(e);
        }
    }

    public static void move(float x, float y){
        push(e -> {
            e.type(SDL_EVENT_MOUSE_MOTION);
            e.motion().x(x).y(y);
        });
        ClientHarness.frames(2);
    }

    public static void mouseButton(float x, float y, int button, boolean down){
        push(e -> {
            e.type(down ? SDL_EVENT_MOUSE_BUTTON_DOWN : SDL_EVENT_MOUSE_BUTTON_UP);
            e.button().button((byte)button).down(down).clicks((byte)1).x(x).y(y);
        });
        ClientHarness.frames(2);
    }

    public static void click(float x, float y){
        click(x, y, SDL_BUTTON_LEFT);
    }

    /** Move, press, release. Most scene2d buttons fire on release, and hover state needs the preceding move. */
    public static void click(float x, float y, int button){
        move(x, y);
        mouseButton(x, y, button, true);
        mouseButton(x, y, button, false);
    }

    /** Press at one point, move in steps, release at another. */
    public static void drag(float x1, float y1, float x2, float y2, int steps){
        move(x1, y1);
        mouseButton(x1, y1, SDL_BUTTON_LEFT, true);
        for(int i = 1; i <= steps; i++){
            move(x1 + (x2 - x1) * i / steps, y1 + (y2 - y1) * i / steps);
        }
        mouseButton(x2, y2, SDL_BUTTON_LEFT, false);
    }

    /** Positive scrolls up (away from the user), like a physical wheel. */
    public static void scroll(float amount){
        push(e -> {
            e.type(SDL_EVENT_MOUSE_WHEEL);
            e.wheel().y(amount);
        });
        ClientHarness.frames(2);
    }

    /** Presses and releases a key. Does not produce text; use {@link #type} for that. */
    public static void key(KeyCode key){
        key(key, true);
        key(key, false);
    }

    public static void key(KeyCode key, boolean down){
        int scancode = scancode(key);
        push(e -> {
            e.type(down ? SDL_EVENT_KEY_DOWN : SDL_EVENT_KEY_UP);
            e.key().scancode(scancode).down(down).repeat(false);
        });
        ClientHarness.frames(2);
    }

    /**
     * Types text into whatever has keyboard focus (click a text field first). This skips SDL on purpose: SDL_EVENT_TEXT_INPUT
     * carries a native string pointer that is awkward to hand over from another thread. The characters reach the Scene exactly
     * as SdlInput would deliver them.
     */
    public static void type(String text){
        ClientHarness.run(() -> {
            for(char c : text.toCharArray()){
                Core.scene.keyTyped(c);
            }
        });
        ClientHarness.frames(2);
    }

    /** Reverse of SdlScanmap#getCode, same approach SdlInput#getKeyName uses. Lowest scancode wins (main keyboard before keypad). */
    private static int scancode(KeyCode key){
        if(scancodes == null){
            int[] map = new int[KeyCode.all.length];
            java.util.Arrays.fill(map, -1);
            for(int i = 1; i < 512; i++){
                int ordinal = SdlScanmap.getCode(i).ordinal();
                if(map[ordinal] == -1) map[ordinal] = i;
            }
            scancodes = map;
        }

        int code = scancodes[key.ordinal()];
        if(code == -1) throw new IllegalArgumentException("No keyboard scancode maps to " + key + " (mouse buttons and controller keys can't be pressed with `key`)");
        return code;
    }
}
