package arc.input;

import arc.*;
import arc.input.KeyBind.*;

/** Enum for storing input codes of mouse, keyboard and controllers. */
public enum KeyCode implements KeybindValue{
    //controller general
    controllerA(KeyType.controller, "A"),
    controllerB(KeyType.controller, "B"),
    controllerX(KeyType.controller, "X"),
    controllerY(KeyType.controller, "Y"),
    controllerGuide(KeyType.controller, "Guide"),
    controllerLBumper(KeyType.controller, "L Bumper"),
    controllerRBumper(KeyType.controller, "R Bumper"),
    controllerBack(KeyType.controller, "Back"),
    controllerStart(KeyType.controller, "Start"),
    controllerLStick(KeyType.controller, "L Stick"),
    controllerRStick(KeyType.controller, "R Stick"),
    //pov
    controllerdPadUp(KeyType.controller, "D-Pad Up"),
    controllerdPadDown(KeyType.controller, "D-Pad Down"),
    controllerdPadLeft(KeyType.controller, "D-Pad Left"),
    controllerdPadRight(KeyType.controller, "D-Pad Right"),
    //controller axes
    controllerLTrigger(KeyType.controller, "L Trigger", true),
    controllerRTrigger(KeyType.controller, "R Trigger", true),
    controllerLStickYAxis(KeyType.controller, "L Stick Y Axis", true),
    controllerLStickXAxis(KeyType.controller, "L Stick X Axis", true),
    controllerRStickYAxis(KeyType.controller, "R Stick Y Axis", true),
    controllerRStickXAxis(KeyType.controller, "R Stick X Axis", true),
    //mouse
    mouseLeft(KeyType.mouse, "Mouse Left"),
    mouseRight(KeyType.mouse, "Mouse Right"),
    mouseMiddle(KeyType.mouse, "Mouse Middle"),
    mouseBack(KeyType.mouse, "Mouse Back"),
    mouseForward(KeyType.mouse, "Mouse Forward"),
    //scroll
    scroll(KeyType.scroll, "Scrollwheel", true),
    //keyboard
    anyKey(KeyType.key, "Any Key"),
    num0(KeyType.key, "0"),
    num1(KeyType.key, "1"),
    num2(KeyType.key, "2"),
    num3(KeyType.key, "3"),
    num4(KeyType.key, "4"),
    num5(KeyType.key, "5"),
    num6(KeyType.key, "6"),
    num7(KeyType.key, "7"),
    num8(KeyType.key, "8"),
    num9(KeyType.key, "9"),
    a(KeyType.key, "A"),
    altLeft(KeyType.key, "L-Alt"),
    altRight(KeyType.key, "R-Alt"),
    apostrophe(KeyType.key, "'"),
    at(KeyType.key, "@"),
    b(KeyType.key, "B"),
    back(KeyType.key, "Back"),
    backslash(KeyType.key, "\\"),
    c(KeyType.key, "C"),
    call(KeyType.key, "Call"),
    camera(KeyType.key, "Camera"),
    clear(KeyType.key, "Clear"),
    comma(KeyType.key, ","),
    d(KeyType.key, "D"),
    del(KeyType.key, "Delete"),
    backspace(KeyType.key, "Delete"),
    forwardDel(KeyType.key, "Forward Delete"),
    dpadCenter(KeyType.key, "Center"),
    dpadDown(KeyType.key, "Down"),
    dpadLeft(KeyType.key, "Left"),
    dpadRight(KeyType.key, "Right"),
    dpadUp(KeyType.key, "Up"),
    center(KeyType.key, "Center"),
    down(KeyType.key, "Down"),
    left(KeyType.key, "Left"),
    right(KeyType.key, "Right"),
    up(KeyType.key, "Up"),
    e(KeyType.key, "E"),
    endcall(KeyType.key, "End Call"),
    enter(KeyType.key, "Enter"),
    envelope(KeyType.key, "Envelope"),
    equals(KeyType.key, "="),
    explorer(KeyType.key, "Explorer"),
    f(KeyType.key, "F"),
    focus(KeyType.key, "Focus"),
    g(KeyType.key, "G"),
    backtick(KeyType.key, "`"),
    h(KeyType.key, "H"),
    headsetHook(KeyType.key, "Headset Hook"),
    home(KeyType.key, "Home"),
    i(KeyType.key, "I"),
    j(KeyType.key, "J"),
    k(KeyType.key, "K"),
    l(KeyType.key, "L"),
    leftBracket(KeyType.key, "["),
    m(KeyType.key, "M"),
    mediaFastForward(KeyType.key, "Fast Forward"),
    mediaNext(KeyType.key, "Next Media"),
    mediaPlayPause(KeyType.key, "Play/Pause"),
    mediaPrevious(KeyType.key, "Prev Media"),
    mediaRewind(KeyType.key, "Rewind"),
    mediaStop(KeyType.key, "Stop Media"),
    menu(KeyType.key, "Menu"),
    minus(KeyType.key, "-"),
    mute(KeyType.key, "Mute"),
    n(KeyType.key, "N"),
    notification(KeyType.key, "Notification"),
    num(KeyType.key, "Num"),
    o(KeyType.key, "O"),
    p(KeyType.key, "P"),
    period(KeyType.key, "."),
    plus(KeyType.key, "Plus"),
    pound(KeyType.key, "#"),
    power(KeyType.key, "Power"),
    q(KeyType.key, "Q"),
    r(KeyType.key, "R"),
    rightBracket(KeyType.key, "]"),
    s(KeyType.key, "S"),
    search(KeyType.key, "Search"),
    semicolon(KeyType.key, ";"),
    shiftLeft(KeyType.key, "L-Shift"),
    shiftRight(KeyType.key, "R-Shift"),
    slash(KeyType.key, "/"),
    softLeft(KeyType.key, "Soft Left"),
    softRight(KeyType.key, "Soft Right"),
    space(KeyType.key, "Space"),
    star(KeyType.key, "*"),
    sym(KeyType.key, "SYM"),
    t(KeyType.key, "T"),
    tab(KeyType.key, "Tab"),
    u(KeyType.key, "U"),
    unknown(KeyType.key, "Unknown"),
    v(KeyType.key, "V"),
    volumeDown(KeyType.key, "Volume Down"),
    volumeUp(KeyType.key, "Volume Up"),
    w(KeyType.key, "W"),
    x(KeyType.key, "X"),
    y(KeyType.key, "Y"),
    z(KeyType.key, "Z"),
    metaAltLeftOn(KeyType.key, "9"),
    metaAltOn(KeyType.key, "Soft Right"),
    metaAltRightOn(KeyType.key, "D"),
    metaShiftLeftOn(KeyType.key, "Explorer"),
    metaShiftOn(KeyType.key, "Soft Left"),
    metaShiftRightOn(KeyType.key, "null"),
    metaSymOn(KeyType.key, "Back"),
    controlLeft(KeyType.key, "L-Ctrl"),
    controlRight(KeyType.key, "R-Ctrl"),
    escape(KeyType.key, "Escape"),
    end(KeyType.key, "End"),
    insert(KeyType.key, "Insert"),
    pageUp(KeyType.key, "Page Up"),
    pageDown(KeyType.key, "Page Down"),
    pictSymbols(KeyType.key, "PICTSYMBOLS"),
    switchCharset(KeyType.key, "switchCharset"),
    buttonCircle(KeyType.key, "F12"),
    buttonA(KeyType.key, "A Button"),
    buttonB(KeyType.key, "B Button"),
    buttonC(KeyType.key, "C Button"),
    buttonX(KeyType.key, "X Button"),
    buttonY(KeyType.key, "Y Button"),
    buttonZ(KeyType.key, "Z Button"),
    buttonL1(KeyType.key, "L1 Button"),
    buttonR1(KeyType.key, "R1 Button"),
    buttonL2(KeyType.key, "L2 Button"),
    buttonR2(KeyType.key, "R2 Button"),
    buttonThumbL(KeyType.key, "Left Thumb"),
    buttonThumbR(KeyType.key, "Right Thumb"),
    buttonStart(KeyType.key, "Start"),
    buttonSelect(KeyType.key, "Select"),
    buttonMode(KeyType.key, "Button Mode"),
    numpad0(KeyType.key, "Numpad 0"),
    numpad1(KeyType.key, "Numpad 1"),
    numpad2(KeyType.key, "Numpad 2"),
    numpad3(KeyType.key, "Numpad 3"),
    numpad4(KeyType.key, "Numpad 4"),
    numpad5(KeyType.key, "Numpad 5"),
    numpad6(KeyType.key, "Numpad 6"),
    numpad7(KeyType.key, "Numpad 7"),
    numpad8(KeyType.key, "Numpad 8"),
    numpad9(KeyType.key, "Numpad 9"),
    colon(KeyType.key, ":"),
    f1(KeyType.key, "F1"),
    f2(KeyType.key, "F2"),
    f3(KeyType.key, "F3"),
    f4(KeyType.key, "F4"),
    f5(KeyType.key, "F5"),
    f6(KeyType.key, "F6"),
    f7(KeyType.key, "F7"),
    f8(KeyType.key, "F8"),
    f9(KeyType.key, "F9"),
    f10(KeyType.key, "F10"),
    f11(KeyType.key, "F11"),
    f12(KeyType.key, "F12"),
    unset(KeyType.key, "Unset"),
    application(KeyType.key, "Application"),
    asterisk(KeyType.key, "*"),
    capsLock(KeyType.key, "Caps Lock"),
    pause(KeyType.key, "Pause"),
    printScreen(KeyType.key, "Print Screen"),
    scrollLock(KeyType.key, "Scroll Lock");

    public static final KeyCode[] all = values();
    public static final KeyCode[] numbers = {num0, num1, num2, num3, num4, num5, num6, num7, num8, num9};
    public final KeyType type;
    public final String value;
    public final boolean axis;

    private String cachedName;

    KeyCode(KeyType type, String value){
        this(type, value, false);
    }

    KeyCode(KeyType type, String value, boolean axis){
        this.type = type;
        this.value = value;
        this.axis = axis;
    }

    public static KeyCode byOrdinal(int id){
        if(id < 0 || id >= all.length) throw new ArrayIndexOutOfBoundsException("Invalid key code: " + id);
        return all[id];
    }

    /** @return a human-friendly name based on keyboard layout. */
    public String getName(){
        if(cachedName == null){
            cachedName = Core.input == null ? value : Core.input.getKeyName(this);
            if(cachedName == null || cachedName.isEmpty()) cachedName = value;
        }
        return cachedName;
    }

    @Override
    public String toString(){
        return getName();
    }

    public enum KeyType{
        key, mouse, controller, scroll
    }
}
