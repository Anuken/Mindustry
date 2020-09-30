package mindustry.input;

import arc.*;
import arc.KeyBinds.*;
import arc.input.InputDevice.*;
import arc.input.*;

public enum Binding implements KeyBind{
    moveX(new Axis(KeyCode.a, KeyCode.d), "general"),
    moveY(new Axis(KeyCode.s, KeyCode.w)),
    mouseMove(KeyCode.mouseBack),
    pan(KeyCode.mouseForward),

    boost(KeyCode.shiftLeft),
    control(KeyCode.controlLeft),
    respawn(KeyCode.v),
    select(KeyCode.mouseLeft),
    deselect(KeyCode.mouseRight),
    breakBlock(KeyCode.mouseRight),

    pickupCargo(KeyCode.leftBracket),
    dropCargo(KeyCode.rightBracket),

    command(KeyCode.g),

    clearBuilding(KeyCode.q),
    pauseBuilding(KeyCode.e),
    rotate(new Axis(KeyCode.scroll)),
    rotatePlaced(KeyCode.r),
    diagonalPlacement(KeyCode.controlLeft),
    pick(KeyCode.mouseMiddle),

    schematicSelect(KeyCode.f),
    schematicFlipX(KeyCode.z),
    schematicFlipY(KeyCode.x),
    schematicMenu(KeyCode.t),

    categoryPrevious(KeyCode.comma, "blocks"),
    categoryNext(KeyCode.period),

    blockSelectLeft(KeyCode.left),
    blockSelectRight(KeyCode.right),
    blockSelectUp(KeyCode.up),
    blockSelectDown(KeyCode.down),
    blockSelect01(KeyCode.num1),
    blockSelect02(KeyCode.num2),
    blockSelect03(KeyCode.num3),
    blockSelect04(KeyCode.num4),
    blockSelect05(KeyCode.num5),
    blockSelect06(KeyCode.num6),
    blockSelect07(KeyCode.num7),
    blockSelect08(KeyCode.num8),
    blockSelect019(KeyCode.num9),
    blockSelect10(KeyCode.num0),

    zoom(new Axis(KeyCode.scroll), "view"),
    menu(Core.app.isAndroid() ? KeyCode.back : KeyCode.escape),
    fullscreen(KeyCode.f11),
    pause(KeyCode.space),
    minimap(KeyCode.m),
    toggleMenus(KeyCode.c),
    screenshot(KeyCode.p),
    togglePowerLines(KeyCode.f5),
    toggleBlockStatus(KeyCode.f6),
    playerList(KeyCode.tab, "multiplayer"),
    chat(KeyCode.enter),
    chatHistoryPrevious(KeyCode.up),
    chatHistoryNext(KeyCode.down),
    chatScroll(new Axis(KeyCode.scroll)),
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
