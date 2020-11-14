package mindustry.editor;

import arc.KeyBinds.*;
import arc.input.*;
import arc.input.InputDevice.*;

public enum EditorBindings implements KeyBind{
    menu(KeyCode.escape),
    rotateLeft(KeyCode.r),
    rotateRight(KeyCode.e),

    // ctrl keys
    undo(KeyCode.z),
    redo(KeyCode.y),
    save(KeyCode.s),
    grid(KeyCode.g),
    clearDecorations(KeyCode.t);

    private final KeybindValue value;

    EditorBindings(KeybindValue value){
        this.value = value;
    }

    @Override
    public KeybindValue defaultValue(DeviceType deviceType){
        return value;
    }
}
