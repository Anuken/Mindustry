package arc.backend.sdl;

import arc.input.*;

public class SdlKeymap{

    public static KeyCode getCode(int code){
        switch(code){
            case 0: return KeyCode.unknown;
            case 8: return KeyCode.backspace;
            case 9: return KeyCode.tab;
            case 1073741982:
            case 13: return KeyCode.enter;
            case 27: return KeyCode.escape;
            case 32: return KeyCode.space;
            case 43: return KeyCode.plus;
            case 44: return KeyCode.comma;
            case 45: return KeyCode.minus;
            case 46: return KeyCode.period;
            case 47: return KeyCode.slash;
            case 48: return KeyCode.num0;
            case 49: return KeyCode.num1;
            case 50: return KeyCode.num2;
            case 51: return KeyCode.num3;
            case 52: return KeyCode.num4;
            case 53: return KeyCode.num5;
            case 54: return KeyCode.num6;
            case 55: return KeyCode.num7;
            case 56: return KeyCode.num8;
            case 57: return KeyCode.num9;
            case 58: return KeyCode.colon;
            case 59: return KeyCode.semicolon;
            case 61: return KeyCode.equals;
            case 64: return KeyCode.at;
            case 92: return KeyCode.backslash;
            case 96: return KeyCode.backtick;
            case 97: return KeyCode.a;
            case 98: return KeyCode.b;
            case 99: return KeyCode.c;
            case 100: return KeyCode.d;
            case 101: return KeyCode.e;
            case 102: return KeyCode.f;
            case 103: return KeyCode.g;
            case 104: return KeyCode.h;
            case 105: return KeyCode.i;
            case 106: return KeyCode.j;
            case 107: return KeyCode.k;
            case 108: return KeyCode.l;
            case 109: return KeyCode.m;
            case 110: return KeyCode.n;
            case 111: return KeyCode.o;
            case 112: return KeyCode.p;
            case 113: return KeyCode.q;
            case 114: return KeyCode.r;
            case 115: return KeyCode.s;
            case 116: return KeyCode.t;
            case 117: return KeyCode.u;
            case 118: return KeyCode.v;
            case 119: return KeyCode.w;
            case 120: return KeyCode.x;
            case 121: return KeyCode.y;
            case 122: return KeyCode.z;
            case 127: return KeyCode.forwardDel;
            case 1073742048: return KeyCode.controlLeft;
            case 1073742052: return KeyCode.controlRight;
            case 1073742049: return KeyCode.shiftLeft;
            case 1073742053: return KeyCode.shiftRight;
            case 1073741882: return KeyCode.f1;
            case 1073741883: return KeyCode.f2;
            case 1073741884: return KeyCode.f3;
            case 1073741885: return KeyCode.f4;
            case 1073741886: return KeyCode.f5;
            case 1073741887: return KeyCode.f6;
            case 1073741888: return KeyCode.f7;
            case 1073741889: return KeyCode.f8;
            case 1073741890: return KeyCode.f9;
            case 1073741891: return KeyCode.f10;
            case 1073741892: return KeyCode.f11;
            case 1073741893: return KeyCode.f12;
            case 1073741897: return KeyCode.insert;
            case 1073741898: return KeyCode.home;
            case 1073741901: return KeyCode.end;
            case 1073741903: return KeyCode.right;
            case 1073741904: return KeyCode.left;
            case 1073741905: return KeyCode.down;
            case 1073741906: return KeyCode.up;
            case 1073741926: return KeyCode.power;
            case 1073741942: return KeyCode.menu;
            case 1073741951: return KeyCode.mute;
            case 1073741980: return KeyCode.clear;
            case 1073742050: return KeyCode.altLeft;
            case 1073742054: return KeyCode.altRight;
        }
        return KeyCode.unknown;
    }
}
