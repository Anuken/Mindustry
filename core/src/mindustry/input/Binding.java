package mindustry.input;

import arc.Application.ApplicationType;
import arc.Core;
import arc.KeyBinds.*;
import arc.input.InputDevice.DeviceType;
import arc.input.KeyCode;

public enum Binding implements KeyBind{
    move_x(new Axis(KeyCode.A, KeyCode.D), "general"),
    move_y(new Axis(KeyCode.S, KeyCode.W)),
    mouse_move(KeyCode.MOUSE_BACK),
    dash(KeyCode.SHIFT_LEFT),
    select(KeyCode.MOUSE_LEFT),
    deselect(KeyCode.MOUSE_RIGHT),
    break_block(KeyCode.MOUSE_RIGHT),
    clear_building(KeyCode.Q),
    pause_building(KeyCode.E),
    rotate(new Axis(KeyCode.SCROLL)),
    rotateplaced(KeyCode.R),
    diagonal_placement(KeyCode.CONTROL_LEFT),
    pick(KeyCode.MOUSE_MIDDLE),
    schematic_select(KeyCode.F),
    schematic_flip_x(KeyCode.Z),
    schematic_flip_y(KeyCode.X),
    schematic_menu(KeyCode.T),
    category_prev(KeyCode.COMMA),
    category_next(KeyCode.PERIOD),
    block_select_left(KeyCode.LEFT),
    block_select_right(KeyCode.RIGHT),
    block_select_up(KeyCode.UP),
    block_select_down(KeyCode.DOWN),
    block_select_01(KeyCode.NUM_1),
    block_select_02(KeyCode.NUM_2),
    block_select_03(KeyCode.NUM_3),
    block_select_04(KeyCode.NUM_4),
    block_select_05(KeyCode.NUM_5),
    block_select_06(KeyCode.NUM_6),
    block_select_07(KeyCode.NUM_7),
    block_select_08(KeyCode.NUM_8),
    block_select_09(KeyCode.NUM_9),
    block_select_10(KeyCode.NUM_0),
    zoom(new Axis(KeyCode.SCROLL), "view"),
    menu(Core.app.getType() == ApplicationType.Android ? KeyCode.BACK : KeyCode.ESCAPE),
    fullscreen(KeyCode.F11),
    pause(KeyCode.SPACE),
    minimap(KeyCode.M),
    toggle_menus(KeyCode.C),
    screenshot(KeyCode.P),
    toggle_power_lines(KeyCode.F7),
    player_list(KeyCode.TAB, "multiplayer"),
    chat(KeyCode.ENTER),
    chat_history_prev(KeyCode.UP),
    chat_history_next(KeyCode.DOWN),
    chat_scroll(new Axis(KeyCode.SCROLL)),
    console(KeyCode.F8),
    ;

    private final KeybindValue defaultValue;
    private final String category;

    Binding(KeybindValue defaultValue, String category){
        this.defaultValue = defaultValue;
        this.category = category;
    }

    Binding(KeybindValue defaultValue){
        this(defaultValue, null);
    }

    @Override
    public KeybindValue defaultValue(DeviceType type){
        return defaultValue;
    }

    @Override
    public String category(){
        return category;
    }
}
