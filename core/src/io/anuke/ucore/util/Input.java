package io.anuke.ucore.util;

import com.badlogic.gdx.controllers.PovDirection;

/**Enum for storing input codes of mouse, keyboard and controllers at once.*/
public enum Input {
    CONTROLLER_A(Type.controller, XboxBinds.A, "A"),
    CONTROLLER_B(Type.controller, XboxBinds.B, "B"),
    CONTROLLER_X(Type.controller, XboxBinds.X, "X"),
    CONTROLLER_Y(Type.controller, XboxBinds.Y, "Y"),
    CONTROLLER_GUIDE(Type.controller, XboxBinds.GUIDE, "Guide"),
    CONTROLLER_L_BUMPER(Type.controller, XboxBinds.L_BUMPER, "L Bumper"),
    CONTROLLER_R_BUMPER(Type.controller, XboxBinds.R_BUMPER, "R Bumper"),
    CONTROLLER_BACK(Type.controller, XboxBinds.BACK, "Back"),
    CONTROLLER_START(Type.controller, XboxBinds.START, "Start"),
    CONTROLLER_L_STICK(Type.controller, XboxBinds.L_STICK, "L Stick"),
    CONTROLLER_R_STICK(Type.controller, XboxBinds.R_STICK, "R Stick"),
    //pov?
    CONTROLLER_DPAD_UP(XboxBinds.DPAD_UP, PovDirection.north, "D-Pad Up"),
    CONTROLLER_DPAD_DOWN(XboxBinds.DPAD_DOWN, PovDirection.south, "D-Pad Down"),
    CONTROLLER_DPAD_LEFT(XboxBinds.DPAD_LEFT, PovDirection.west, "D-Pad Left"),
    CONTROLLER_DPAD_RIGHT(XboxBinds.DPAD_RIGHT, PovDirection.east, "D-Pad Right"),
    //controller axes
    CONTROLLER_L_TRIGGER(Type.controller, XboxBinds.L_TRIGGER, "L Trigger", true),
    CONTROLLER_R_TRIGGER(Type.controller, XboxBinds.R_TRIGGER, "R Trigger", true),
    CONTROLLER_L_STICK_VERTICAL_AXIS(Type.controller, XboxBinds.L_STICK_VERTICAL_AXIS, "L Stick Y Axis", true),
    CONTROLLER_L_STICK_HORIZONTAL_AXIS(Type.controller, XboxBinds.L_STICK_HORIZONTAL_AXIS, "L Stick X Axis", true),
    CONTROLLER_R_STICK_VERTICAL_AXIS(Type.controller, XboxBinds.R_STICK_VERTICAL_AXIS, "R Stick Y Axis", true),
    CONTROLLER_R_STICK_HORIZONTAL_AXIS(Type.controller, XboxBinds.R_STICK_HORIZONTAL_AXIS, "R Stick X Axis", true),
    //mouse
    MOUSE_LEFT(Type.mouse, 0, "Mouse Left"),
    MOUSE_RIGHT(Type.mouse, 1, "Mouse Right"),
    MOUSE_MIDDLE(Type.mouse, 2, "Mouse Middle"),
    MOUSE_BACK(Type.mouse, 3, "Mouse Back"),
    MOUSE_FORWARD(Type.mouse, 4, "Mouse Forward"),
    //scroll
    SCROLL(Type.scroll, 0, "Scrollwheel", true),
    //keyboard
    ANY_KEY(Type.key, -1, "Any Key"),
    NUM_0(7),
    NUM_1(8),
    NUM_2(9),
    NUM_3(10),
    NUM_4(11),
    NUM_5(12),
    NUM_6(13),
    NUM_7(14),
    NUM_8(15),
    NUM_9(16),
    A(29),
    ALT_LEFT(57),
    ALT_RIGHT(58),
    APOSTROPHE(75),
    AT(77),
    B(30),
    BACK(4),
    BACKSLASH(73),
    C(31),
    CALL(5),
    CAMERA(27),
    CLEAR(28),
    COMMA(55),
    D(32),
    DEL(67),
    BACKSPACE(67),
    FORWARD_DEL(112),
    DPAD_CENTER(23),
    DPAD_DOWN(20),
    DPAD_LEFT(21),
    DPAD_RIGHT(22),
    DPAD_UP(19),
    CENTER(23),
    DOWN(20),
    LEFT(21),
    RIGHT(22),
    UP(19),
    E(33),
    ENDCALL(6),
    ENTER(66),
    ENVELOPE(65),
    EQUALS(70),
    EXPLORER(64),
    F(34),
    FOCUS(80),
    G(35),
    GRAVE(68),
    H(36),
    HEADSETHOOK(79),
    HOME(3),
    I(37),
    J(38),
    K(39),
    L(40),
    LEFT_BRACKET(71),
    M(41),
    MEDIA_FAST_FORWARD(90),
    MEDIA_NEXT(87),
    MEDIA_PLAY_PAUSE(85),
    MEDIA_PREVIOUS(88),
    MEDIA_REWIND(89),
    MEDIA_STOP(86),
    MENU(82),
    MINUS(69),
    MUTE(91),
    N(42),
    NOTIFICATION(83),
    NUM(78),
    O(43),
    P(44),
    PERIOD(56),
    PLUS(81),
    POUND(18),
    POWER(26),
    Q(45),
    R(46),
    RIGHT_BRACKET(72),
    S(47),
    SEARCH(84),
    SEMICOLON(74),
    SHIFT_LEFT(59),
    SHIFT_RIGHT(60),
    SLASH(76),
    SOFT_LEFT(1),
    SOFT_RIGHT(2),
    SPACE(62),
    STAR(17),
    SYM(63),
    T(48),
    TAB(61),
    U(49),
    UNKNOWN(0),
    V(50),
    VOLUME_DOWN(25),
    VOLUME_UP(24),
    W(51),
    X(52),
    Y(53),
    Z(54),
    META_ALT_LEFT_ON(16),
    META_ALT_ON(2),
    META_ALT_RIGHT_ON(32),
    META_SHIFT_LEFT_ON(64),
    META_SHIFT_ON(1),
    META_SHIFT_RIGHT_ON(128),
    META_SYM_ON(4),
    CONTROL_LEFT(129),
    CONTROL_RIGHT(130),
    ESCAPE(131),
    END(132),
    INSERT(133),
    PAGE_UP(92),
    PAGE_DOWN(93),
    PICTSYMBOLS(94),
    SWITCH_CHARSET(95),
    BUTTON_CIRCLE(255),
    BUTTON_A(96),
    BUTTON_B(97),
    BUTTON_C(98),
    BUTTON_X(99),
    BUTTON_Y(100),
    BUTTON_Z(101),
    BUTTON_L1(102),
    BUTTON_R1(103),
    BUTTON_L2(104),
    BUTTON_R2(105),
    BUTTON_THUMBL(106),
    BUTTON_THUMBR(107),
    BUTTON_START(108),
    BUTTON_SELECT(109),
    BUTTON_MODE(110),

    NUMPAD_0(144),
    NUMPAD_1(145),
    NUMPAD_2(146),
    NUMPAD_3(147),
    NUMPAD_4(148),
    NUMPAD_5(149),
    NUMPAD_6(150),
    NUMPAD_7(151),
    NUMPAD_8(152),
    NUMPAD_9(153),

    COLON(243),
    F1(244),
    F2(245),
    F3(246),
    F4(247),
    F5(248),
    F6(249),
    F7(250),
    F8(251),
    F9(252),
    F10(253),
    F11(254),
    F12(255),
    UNSET(Type.key, 0, "Unset");

    public final int code;
    public final Type type;
    public final String value;
    public final boolean axis;
    public final boolean pov;
    public final PovDirection direction;

    /**desktop keycode*/
    Input(int keycode){
        type = Type.key;
        value = com.badlogic.gdx.Input.Keys.toString(keycode);
        code = keycode;
        axis = false;
        pov = false;
        direction = null;
    }

    Input(Type type, int keycode, String value){
        this(type, keycode, value, false);
    }

    Input(Type type, int keycode, String value, boolean axis){
        this.code = keycode;
        this.type = type;
        this.value = value;
        this.axis = axis;
        pov = false;
        direction = null;
    }

    Input(int keycode, PovDirection direction, String value){
        this.code = keycode;
        this.type = Type.controller;
        this.value = value;
        this.axis = false;
        pov = true;
        this.direction = direction;
    }

    public static Input findByType(Type type, int code, boolean axis){
        for(Input i : values()){
            if(i.type == type && i.code == code && i.axis == axis){
                return i;
            }
        }
        return Input.UNKNOWN;
    }

    public static Input findPOV(PovDirection direction){
        for(Input i : values()){
            if(i.type == Type.controller && i.direction == direction && i.pov){
                return i;
            }
        }
        return Input.UNKNOWN;
    }

    @Override
    public String toString() {
        return value;
    }

    public enum Type{
        key, mouse, controller, scroll
    }
}
