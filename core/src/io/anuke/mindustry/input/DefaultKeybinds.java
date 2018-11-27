package io.anuke.mindustry.input;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import io.anuke.ucore.core.Inputs.Axis;
import io.anuke.ucore.core.Inputs.DeviceType;
import io.anuke.ucore.core.KeyBinds;
import io.anuke.ucore.core.KeyBinds.Category;
import io.anuke.ucore.input.Input;

public class DefaultKeybinds{

    public static void load(){
        String[] sections = {"player_1"};

        for(String section : sections){

            KeyBinds.defaultSection(section, DeviceType.keyboard,
                new Category("general"),
                "move_x", new Axis(Input.A, Input.D),
                "move_y", new Axis(Input.S, Input.W),
                "select", Input.MOUSE_LEFT,
                "deselect", Input.MOUSE_RIGHT,
                "break", Input.MOUSE_RIGHT,
                "rotate", new Axis(Input.SCROLL),
                "dash", Input.SHIFT_LEFT,
                "drop_unit", Input.SHIFT_LEFT,
                new Category("view"),
                "zoom_hold", Input.CONTROL_LEFT,
                "zoom", new Axis(Input.SCROLL),
                "zoom_minimap", new Axis(Input.MINUS, Input.PLUS),
                "menu", Gdx.app.getType() == ApplicationType.Android ? Input.BACK : Input.ESCAPE,
                "pause", Input.SPACE,
                "toggle_menus", Input.C,
                "screenshot", Input.P,
                new Category("multiplayer"),
                "player_list", Input.TAB,
                "chat", Input.ENTER,
                "chat_history_prev", Input.UP,
                "chat_history_next", Input.DOWN,
                "chat_scroll", new Axis(Input.SCROLL)
            );

            KeyBinds.defaultSection(section, DeviceType.controller,
                new Category("general"),
                "move_x", new Axis(Input.CONTROLLER_L_STICK_HORIZONTAL_AXIS),
                "move_y", new Axis(Input.CONTROLLER_L_STICK_VERTICAL_AXIS),
                "cursor_x", new Axis(Input.CONTROLLER_R_STICK_HORIZONTAL_AXIS),
                "cursor_y", new Axis(Input.CONTROLLER_R_STICK_VERTICAL_AXIS),
                //"select", Input.CONTROLLER_R_BUMPER,
                //"break", Input.CONTROLLER_L_BUMPER,
                //"shoot", Input.CONTROLLER_R_TRIGGER,
                "dash", Input.CONTROLLER_Y,
                "rotate_alt", new Axis(Input.CONTROLLER_DPAD_RIGHT, Input.CONTROLLER_DPAD_LEFT),
                "rotate", new Axis(Input.CONTROLLER_A, Input.CONTROLLER_B),
                new Category("view"),
                "zoom_hold", Input.ANY_KEY,
                "zoom", new Axis(Input.CONTROLLER_DPAD_DOWN, Input.CONTROLLER_DPAD_UP),
                "menu", Input.CONTROLLER_X,
                "pause", Input.CONTROLLER_L_TRIGGER,
                new Category("multiplayer"),
                "player_list", Input.CONTROLLER_START
            );

        }

        KeyBinds.setSectionAlias("default", "player_1");
    }
}
