package mindustry.input;

import arc.*;
import arc.KeyBinds.*;
import arc.input.InputDevice.*;
import arc.input.*;

public enum Binding implements KeyBind{
    move_x(new Axis(KeyCode.a, KeyCode.d), "general"),
    move_y(new Axis(KeyCode.s, KeyCode.w)),
    mouse_move(KeyCode.mouseForward),

    boost(KeyCode.shiftLeft),
    control(KeyCode.controlLeft),
    respawn(KeyCode.v),
    select(KeyCode.mouseLeft),
    deselect(KeyCode.mouseRight),
    break_block(KeyCode.mouseRight),

    pickupCargo(KeyCode.leftBracket),
    dropCargo(KeyCode.rightBracket),

    command(KeyCode.g),

    clear_building(KeyCode.q),
    pause_building(KeyCode.e),
    rotate(new Axis(KeyCode.scroll)),
    rotateplaced(KeyCode.r),
    diagonal_placement(KeyCode.controlLeft),
    pick(KeyCode.mouseMiddle),

    schematic_select(KeyCode.f),
    schematic_flip_x(KeyCode.z),
    schematic_flip_y(KeyCode.x),
    schematic_menu(KeyCode.t),

    category_prev(KeyCode.comma, "blocks"),
    category_next(KeyCode.period),

    block_select_left(KeyCode.left),
    block_select_right(KeyCode.right),
    block_select_up(KeyCode.up),
    block_select_down(KeyCode.down),
    block_select_01(KeyCode.num1),
    block_select_02(KeyCode.num2),
    block_select_03(KeyCode.num3),
    block_select_04(KeyCode.num4),
    block_select_05(KeyCode.num5),
    block_select_06(KeyCode.num6),
    block_select_07(KeyCode.num7),
    block_select_08(KeyCode.num8),
    block_select_09(KeyCode.num9),
    block_select_10(KeyCode.num0),

    zoom(new Axis(KeyCode.scroll), "view"),
    menu(Core.app.isAndroid() ? KeyCode.back : KeyCode.escape),
    fullscreen(KeyCode.f11),
    pause(KeyCode.space),
    minimap(KeyCode.m),
    toggle_menus(KeyCode.c),
    screenshot(KeyCode.p),
    toggle_power_lines(KeyCode.f5),
    toggle_block_status(KeyCode.f6),
    player_list(KeyCode.tab, "multiplayer"),
    chat(KeyCode.enter),
    chat_history_prev(KeyCode.up),
    chat_history_next(KeyCode.down),
    chat_scroll(new Axis(KeyCode.scroll)),
    console(KeyCode.f8),
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
