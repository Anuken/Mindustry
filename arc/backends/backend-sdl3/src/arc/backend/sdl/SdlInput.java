package arc.backend.sdl;

import arc.*;
import arc.input.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.struct.*;
import org.lwjgl.sdl.*;

import java.util.*;

public class SdlInput extends Input{
    private final InputEventQueue queue = new InputEventQueue();
    private int[] keycodeToScancode;
    private int mouseX, mouseY;
    private int deltaX, deltaY;
    private int mousePressed;
    private Seq<EditEvent> stringEditEvents = new Seq<>();

    //handle encoded input data
    void handleInput(SDL_Event event){
        int type = event.type();
        if(type == SDLEvents.SDL_EVENT_KEY_DOWN || type == SDLEvents.SDL_EVENT_KEY_UP){
            boolean down = type == SDLEvents.SDL_EVENT_KEY_DOWN;
            int keycode = event.key().scancode();

            KeyCode key = SdlScanmap.getCode(keycode);
            //only process non-repeats
            if(!event.key().repeat()){
                if(down){
                    queue.keyDown(key);
                }else{
                    queue.keyUp(key);
                }
            }

            //special keys
            if(down){
                if(key == KeyCode.backspace) queue.keyTyped((char)8);
                if(key == KeyCode.tab) queue.keyTyped('\t');
                if(key == KeyCode.enter) queue.keyTyped((char)13);
                if(key == KeyCode.forwardDel || key == KeyCode.del) queue.keyTyped((char)127);
            }

        }else if(type == SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN || type == SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP){
            boolean down = type == SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN;
            int keycode = event.button().button();
            int x = (int)event.button().x(), y = Core.graphics.getHeight() - (int)event.button().y();

            KeyCode key =
            keycode == SDLMouse.SDL_BUTTON_LEFT ? KeyCode.mouseLeft :
            keycode == SDLMouse.SDL_BUTTON_RIGHT ? KeyCode.mouseRight :
            keycode == SDLMouse.SDL_BUTTON_MIDDLE ? KeyCode.mouseMiddle :
            keycode == SDLMouse.SDL_BUTTON_X1 ? KeyCode.mouseBack :
            keycode == SDLMouse.SDL_BUTTON_X2 ? KeyCode.mouseForward : null;

            if(key != null){
                if(down){
                    mousePressed ++;
                    queue.touchDown(x, y, 0, key);
                }else{
                    mousePressed = Math.max(0, mousePressed - 1);
                    queue.touchUp(x, y, 0, key);
                }
            }

        }else if(type == SDLEvents.SDL_EVENT_MOUSE_MOTION){
            int x = (int)event.motion().x();
            int y = Core.graphics.getHeight() - (int)event.motion().y();

            deltaX = x - mouseX;
            deltaY = y - mouseY;
            mouseX = x;
            mouseY = y;

            if(mousePressed > 0){
                queue.touchDragged(mouseX, mouseY, 0);
            }else{
                queue.mouseMoved(mouseX, mouseY);
            }

        }else if(type == SDLEvents.SDL_EVENT_MOUSE_WHEEL){
            queue.scrolled(-fixSign(event.wheel().x()), -fixSign(event.wheel().y()));
        }else if(type == SDLEvents.SDL_EVENT_TEXT_INPUT){
            String text = event.text().textString();
            if(text != null){
                for(int i = 0; i < text.length(); i++){
                    queue.keyTyped(text.charAt(i));
                }
            }

        }else if(type == SDLEvents.SDL_EVENT_TEXT_EDITING){
            String editString = event.edit().textString();
            stringEditEvents.add(new EditEvent(){{
                start = event.edit().start();
                this.length = event.edit().length();
                this.text = editString;
            }});
        }
    }

    static float fixSign(float scroll){
        return Mathf.equal(scroll, 0f) ? 0f : Mathf.sign(scroll);
    }

    //note: start and length parameters seem useless, ignore those
    void handleFieldCandidate(EditEvent e){
        class ImeData{
            String lastSetText;
            String realText;
            int cursor;
        }

        String text = e.text;
        if(Core.scene != null && Core.scene.getKeyboardFocus() instanceof TextField){
            TextField field = (TextField)Core.scene.getKeyboardFocus();

            if(field.imeData instanceof ImeData){
                ImeData data = (ImeData)field.imeData;

                //text modified externally, which means this data is invalid, kill it
                if(data.lastSetText != field.getText()){
                    field.imeData = null;
                }else if(text.length() == 0){
                    //cancel or end composition
                    field.imeData = null;
                    field.setText(data.realText);
                    field.clearSelection();
                    field.setCursorPosition(data.cursor);
                }
            }

            //there seem to be stray IME events with zero length, ignore those?
            if(text.length() == 0){
                return;
            }

            //re-initialize when invalidated or just beginning
            if(field.imeData == null){
                field.imeData = new ImeData(){{
                    cursor = field.getCursorPosition();
                    realText = field.getText();
                }};
            }

            ImeData data = (ImeData)field.imeData;
            String targetText = data.realText;
            int insertPos = data.cursor;

            field.setText(targetText.substring(0, Math.min(insertPos, targetText.length())) + text + targetText.substring(Math.min(insertPos, targetText.length())));
            field.setSelection(insertPos, insertPos + text.length());
            //fixme: setCursor will clearSelection, but the IME cursor can inside selection.
            //  And TextField seems not to support that.
            //field.setCursorPosition(insertPos+e.start);

            data.lastSetText = field.getText();
        }
    }

    //called before main loop
    void update(){
        queue.setProcessor(inputMultiplexer);
        queue.drain();

        for(EditEvent e : stringEditEvents){
            handleFieldCandidate(e);
        }
        stringEditEvents.clear();
    }

    //called after main loop
    void postUpdate(){
        keyboard.postUpdate();
        deltaX = deltaY = 0;
    }

    @Override
    public String getKeyName(KeyCode code){
        if(keycodeToScancode == null){
            keycodeToScancode = new int[KeyCode.all.length];
            Arrays.fill(keycodeToScancode, -1);
            for(int i = 0; i < 256; i++){
                KeyCode keycode = SdlScanmap.getCode(i);
                if(keycodeToScancode[keycode.ordinal()] == -1){
                    keycodeToScancode[keycode.ordinal()] = i;
                }
            }
        }
        int value = keycodeToScancode[code.ordinal()];
        if(value != -1){
            return SDLKeyboard.SDL_GetScancodeName(value);
        }
        return super.getKeyName(code);
    }

    @Override
    public int mouseX(){
        return mouseX;
    }

    @Override
    public int mouseX(int pointer){
        return pointer == 0 ? mouseX : 0;
    }

    @Override
    public int deltaX(){
        return deltaX;
    }

    @Override
    public int deltaX(int pointer){
        return pointer == 0 ? deltaX : 0;
    }

    @Override
    public int mouseY(){
        return mouseY;
    }

    @Override
    public int mouseY(int pointer){
        return pointer == 0 ? mouseY : 0;
    }

    @Override
    public int deltaY(){
        return deltaY;
    }

    @Override
    public int deltaY(int pointer){
        return pointer == 0 ? deltaY : 0;
    }

    @Override
    public boolean isTouched(){
        return keyDown(KeyCode.mouseLeft) || keyDown(KeyCode.mouseRight);
    }

    @Override
    public boolean justTouched(){
        return keyTap(KeyCode.mouseLeft) || keyTap(KeyCode.mouseRight);
    }

    @Override
    public boolean isTouched(int pointer){
        return false;
    }

    @Override
    public long getCurrentEventTime(){
        return queue.getCurrentEventTime();
    }

    static class EditEvent{
        int start, length;
        String text;
    }
}
