package arc.backend.robovm;

import arc.input.*;
import org.robovm.apple.uikit.*;

public class IOSKeymap{

    static KeyCode getKeyCode(UIKey key){
        UIKeyboardHIDUsage keyCode;
        try{
            keyCode = key.getKeyCode();
        }catch (IllegalArgumentException e){
            return KeyCode.unknown;
        }

        switch (keyCode) {
            case KeyboardA: return KeyCode.a;
            case KeyboardB: return KeyCode.b;
            case KeyboardC: return KeyCode.c;
            case KeyboardD: return KeyCode.d;
            case KeyboardE: return KeyCode.e;
            case KeyboardF: return KeyCode.f;
            case KeyboardG: return KeyCode.g;
            case KeyboardH: return KeyCode.h;
            case KeyboardI: return KeyCode.i;
            case KeyboardJ: return KeyCode.j;
            case KeyboardK: return KeyCode.k;
            case KeyboardL: return KeyCode.l;
            case KeyboardM: return KeyCode.m;
            case KeyboardN: return KeyCode.n;
            case KeyboardO: return KeyCode.o;
            case KeyboardP: return KeyCode.p;
            case KeyboardQ: return KeyCode.q;
            case KeyboardR: return KeyCode.r;
            case KeyboardS: return KeyCode.s;
            case KeyboardT: return KeyCode.t;
            case KeyboardU: return KeyCode.u;
            case KeyboardV: return KeyCode.v;
            case KeyboardW: return KeyCode.w;
            case KeyboardX: return KeyCode.x;
            case KeyboardY: return KeyCode.y;
            case KeyboardZ: return KeyCode.z;
            case Keyboard1: return KeyCode.num1;
            case Keyboard2: return KeyCode.num2;
            case Keyboard3: return KeyCode.num3;
            case Keyboard4: return KeyCode.num4;
            case Keyboard5: return KeyCode.num5;
            case Keyboard6: return KeyCode.num6;
            case Keyboard7: return KeyCode.num7;
            case Keyboard8: return KeyCode.num8;
            case Keyboard9: return KeyCode.num9;
            case Keyboard0: return KeyCode.num0;
            case KeyboardReturnOrEnter: return KeyCode.enter;
            case KeyboardEscape: return KeyCode.escape;
            case KeyboardDeleteOrBackspace: return KeyCode.backspace;
            case KeyboardTab: return KeyCode.tab;
            case KeyboardSpacebar: return KeyCode.space;
            case KeyboardHyphen: return KeyCode.minus;
            case KeyboardEqualSign: return KeyCode.equals;
            case KeyboardOpenBracket: return KeyCode.leftBracket;
            case KeyboardCloseBracket: return KeyCode.rightBracket;
            case KeyboardBackslash: return KeyCode.backspace;
            case KeyboardNonUSPound: return KeyCode.pound;
            case KeyboardSemicolon: return KeyCode.semicolon;
            case KeyboardQuote: return KeyCode.apostrophe;
            case KeyboardGraveAccentAndTilde: return KeyCode.backtick;
            case KeyboardComma: return KeyCode.comma;
            case KeyboardPeriod: return KeyCode.period;
            case KeyboardSlash: return KeyCode.slash;
            case KeyboardF1: return KeyCode.f1;
            case KeyboardF2: return KeyCode.f2;
            case KeyboardF3: return KeyCode.f3;
            case KeyboardF4: return KeyCode.f4;
            case KeyboardF5: return KeyCode.f5;
            case KeyboardF6: return KeyCode.f6;
            case KeyboardF7: return KeyCode.f7;
            case KeyboardF8: return KeyCode.f8;
            case KeyboardF9: return KeyCode.f9;
            case KeyboardF10: return KeyCode.f10;
            case KeyboardF11: return KeyCode.f11;
            case KeyboardF12: return KeyCode.f12;
            case KeyboardPause: return KeyCode.pause;
            case KeyboardInsert: return KeyCode.insert;
            case KeyboardHome: return KeyCode.home;
            case KeyboardPageUp: return KeyCode.pageUp;
            case KeyboardDeleteForward: return KeyCode.forwardDel;
            case KeyboardEnd: return KeyCode.end;
            case KeyboardPageDown: return KeyCode.pageDown;
            case KeyboardRightArrow: return KeyCode.right;
            case KeyboardLeftArrow: return KeyCode.left;
            case KeyboardDownArrow: return KeyCode.down;
            case KeyboardUpArrow: return KeyCode.up;
            case Keypad1: return KeyCode.num1;
            case Keypad2: return KeyCode.num2;
            case Keypad3: return KeyCode.num3;
            case Keypad4: return KeyCode.num4;
            case Keypad5: return KeyCode.num5;
            case Keypad6: return KeyCode.num6;
            case Keypad7: return KeyCode.num7;
            case Keypad8: return KeyCode.num8;
            case Keypad9: return KeyCode.num9;
            case Keypad0: return KeyCode.num0;
            case KeyboardNonUSBackslash: return KeyCode.backslash;
            case KeyboardApplication: return KeyCode.menu;
            case KeyboardPower: return KeyCode.power;
            case KeypadEqualSign:
            case KeypadEqualSignAS400: return KeyCode.equals;
            case KeyboardHelp: return KeyCode.f1;
            case KeyboardMenu: return KeyCode.menu;
            case KeyboardSelect: return KeyCode.buttonSelect;
            case KeyboardStop: return KeyCode.mediaStop;
            case KeyboardFind: return KeyCode.search;
            case KeyboardMute: return KeyCode.mute;
            case KeyboardVolumeUp: return KeyCode.volumeUp;
            case KeyboardVolumeDown: return KeyCode.volumeDown;
            case KeypadComma: return KeyCode.comma;
            case KeyboardAlternateErase: return KeyCode.del;
            case KeyboardCancel: return KeyCode.escape;
            case KeyboardClear: return KeyCode.clear;
            case KeyboardReturn: return KeyCode.enter;
            case KeyboardLeftControl: return KeyCode.controlLeft;
            case KeyboardLeftShift: return KeyCode.shiftLeft;
            case KeyboardLeftAlt: return KeyCode.altLeft;
            case KeyboardRightControl: return KeyCode.controlRight;
            case KeyboardRightShift: return KeyCode.shiftRight;
            case KeyboardRightAlt: return KeyCode.altRight;
            case KeyboardCapsLock: return KeyCode.capsLock;
            case KeyboardPrintScreen: return KeyCode.printScreen;
            case KeyboardScrollLock: return KeyCode.scrollLock;
            default: return KeyCode.unknown;
        }
    }
}
