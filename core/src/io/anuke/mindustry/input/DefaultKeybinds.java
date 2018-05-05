package io.anuke.mindustry.input;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import io.anuke.ucore.core.Inputs.Axis;
import io.anuke.ucore.core.Inputs.DeviceType;
import io.anuke.ucore.core.KeyBinds;
import io.anuke.ucore.util.Input;

public class DefaultKeybinds {

    public static void load(){
        KeyBinds.defaults(
            "move_x", new Axis(Input.A, Input.D),
            "move_y", new Axis(Input.S, Input.W),
            "select", Input.MOUSE_LEFT,
            "break", Input.MOUSE_RIGHT,
            "shoot", Input.MOUSE_LEFT,
            "zoom_hold", Input.CONTROL_LEFT,
            "zoom", new Axis(Input.SCROLL),
            "menu", Gdx.app.getType() == ApplicationType.Android ? Input.BACK : Input.ESCAPE,
            "pause", Input.SPACE,
            "dash", Input.SHIFT_LEFT,
            "rotate_alt", new Axis(Input.R, Input.E),
            "rotate", new Axis(Input.SCROLL),
            "toggle_menus", Input.C,
            "block_info", Input.CONTROL_LEFT,
            "player_list", Input.TAB,
            "chat", Input.ENTER,
            "chat_history_prev", Input.UP,
            "chat_history_next", Input.DOWN,
            "chat_scroll", new Axis(Input.SCROLL),
            "console", Input.GRAVE,
            "weapon_1", Input.NUM_1,
            "weapon_2", Input.NUM_2,
            "weapon_3", Input.NUM_3,
            "weapon_4", Input.NUM_4,
            "weapon_5", Input.NUM_5,
            "weapon_6", Input.NUM_6
        );

        KeyBinds.defaults(
                DeviceType.controller,
            "move_x", new Axis(Input.CONTROLLER_L_STICK_HORIZONTAL_AXIS),
            "move_y", new Axis(Input.CONTROLLER_L_STICK_VERTICAL_AXIS),
            "cursor_x", new Axis(Input.CONTROLLER_R_STICK_HORIZONTAL_AXIS),
            "cursor_y", new Axis(Input.CONTROLLER_R_STICK_VERTICAL_AXIS),
            "select", Input.CONTROLLER_R_BUMPER,
            "break", Input.CONTROLLER_L_BUMPER,
            "shoot", Input.CONTROLLER_R_TRIGGER,
            "zoom_hold", Input.ANY_KEY,
            "zoom", new Axis(Input.CONTROLLER_DPAD_DOWN, Input.CONTROLLER_DPAD_UP),
            "menu", Input.CONTROLLER_X,
            "pause", Input.CONTROLLER_L_TRIGGER,
            "dash", Input.CONTROLLER_Y,
            "rotate_alt", new Axis(Input.CONTROLLER_DPAD_RIGHT, Input.CONTROLLER_DPAD_LEFT),
            "rotate", new Axis(Input.CONTROLLER_A, Input.CONTROLLER_B),
            "player_list", Input.CONTROLLER_START,
            "chat", Input.ENTER,
            "chat_history_prev", Input.UP,
            "chat_history_next", Input.DOWN,
            "chat_scroll", new Axis(Input.SCROLL),
            "console", Input.GRAVE,
            "weapon_1", Input.NUM_1,
            "weapon_2", Input.NUM_2,
            "weapon_3", Input.NUM_3,
            "weapon_4", Input.NUM_4,
            "weapon_5", Input.NUM_5,
            "weapon_6", Input.NUM_6
        );
    }
}
