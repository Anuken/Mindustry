package arc.backend.sdl;

import arc.input.*;

public class SdlScanmap{

    public static KeyCode getCode(int code){
        switch(code){
            case 0: return KeyCode.unknown;
            case 42: return KeyCode.backspace;
            case 43: return KeyCode.tab;
            case 88:
            case 40: return KeyCode.enter;
            case 41: return KeyCode.escape;
            case 44: return KeyCode.space;
            case 87: return KeyCode.plus;
            case 54:
            case 133: return KeyCode.comma;
            case 45:
            case 86: return KeyCode.minus;
            case 55:
            case 99: return KeyCode.period;
            case 56:
            case 84: return KeyCode.slash;
            case 85: return KeyCode.asterisk;
            case 39:
            case 98: return KeyCode.num0;
            case 30:
            case 89: return KeyCode.num1;
            case 31:
            case 90: return KeyCode.num2;
            case 32:
            case 91: return KeyCode.num3;
            case 33:
            case 92: return KeyCode.num4;
            case 34:
            case 93: return KeyCode.num5;
            case 35:
            case 94: return KeyCode.num6;
            case 36:
            case 95: return KeyCode.num7;
            case 37:
            case 96: return KeyCode.num8;
            case 38:
            case 97: return KeyCode.num9;
            case 203: return KeyCode.colon;
            case 51: return KeyCode.semicolon;
            case 52: return KeyCode.apostrophe;
            case 46: return KeyCode.equals;
            case 206: return KeyCode.at;
            case 47: return KeyCode.leftBracket;
            case 48: return KeyCode.rightBracket;
            case 100:
            case 49: return KeyCode.backslash;
            case 53: return KeyCode.backtick;
            case 4: return KeyCode.a;
            case 5: return KeyCode.b;
            case 6: return KeyCode.c;
            case 7: return KeyCode.d;
            case 8: return KeyCode.e;
            case 9: return KeyCode.f;
            case 10: return KeyCode.g;
            case 11: return KeyCode.h;
            case 12: return KeyCode.i;
            case 13: return KeyCode.j;
            case 14: return KeyCode.k;
            case 15: return KeyCode.l;
            case 16: return KeyCode.m;
            case 17: return KeyCode.n;
            case 18: return KeyCode.o;
            case 19: return KeyCode.p;
            case 20: return KeyCode.q;
            case 21: return KeyCode.r;
            case 22: return KeyCode.s;
            case 23: return KeyCode.t;
            case 24: return KeyCode.u;
            case 25: return KeyCode.v;
            case 26: return KeyCode.w;
            case 27: return KeyCode.x;
            case 28: return KeyCode.y;
            case 29: return KeyCode.z;
            case 231:
            case 227: return KeyCode.sym;
            case 224: return KeyCode.controlLeft;
            case 228: return KeyCode.controlRight;
            case 225: return KeyCode.shiftLeft;
            case 229: return KeyCode.shiftRight;
            case 57: return KeyCode.capsLock;
            case 58: return KeyCode.f1;
            case 59: return KeyCode.f2;
            case 60: return KeyCode.f3;
            case 61: return KeyCode.f4;
            case 62: return KeyCode.f5;
            case 63: return KeyCode.f6;
            case 64: return KeyCode.f7;
            case 65: return KeyCode.f8;
            case 66: return KeyCode.f9;
            case 67: return KeyCode.f10;
            case 68: return KeyCode.f11;
            case 69: return KeyCode.f12;
            case 70: return KeyCode.printScreen;
            case 71: return KeyCode.scrollLock;
            case 72: return KeyCode.pause;
            case 73: return KeyCode.insert;
            case 76: return KeyCode.del;
            case 74: return KeyCode.home;
            case 77: return KeyCode.end;
            case 75: return KeyCode.pageUp;
            case 78: return KeyCode.pageDown;
            case 79: return KeyCode.right;
            case 80: return KeyCode.left;
            case 81: return KeyCode.down;
            case 82: return KeyCode.up;
            case 101: return KeyCode.application;
            case 102: return KeyCode.power;
            case 118: return KeyCode.menu;
            case 127: return KeyCode.mute;
            case 156: return KeyCode.clear;
            case 226: return KeyCode.altLeft;
            case 230: return KeyCode.altRight;
        }
        return KeyCode.unknown;
    }
}
