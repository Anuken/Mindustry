package mindustry.content;

import mindustry.ctype.*;
import mindustry.game.*;

public class Loadouts implements ContentList{
    public static Schematic
    basicShard,
    basicFoundation,
    basicNucleus,
    basicBastion,
    basicAegis;

    @Override
    public void load(){
        basicShard = Schematics.readBase64("bXNjaAB4nD2K2wqAIBiD5ymibnoRn6YnEP1BwUMoBL19FuJ2sbFvUFgYZDaJsLeQrkinN9UJHImsNzlYE7WrIUastuSbnlKx2VJJt+8IQGGKdfO/8J5yrGJSMegLg+YUIA==");
        basicFoundation = Schematics.readBase64("bXNjaAB4nD1OSQ6DMBBzFhVu8BG+0X8MQyoiJTNSukj8nlCi2Adbtg/GA4OBF8oB00rvyE/9ykafqOIw58A7SWRKy1ZiShhZ5RcOLZhYS1hefQ1gRIeptH9jq/qW2lvc1d2tgWsOfVX/tOwE86AYBA==");
        basicNucleus = Schematics.readBase64("bXNjaAB4nD2MUQqAIBBEJy0s6qOLdJXuYNtCgikYBd2+LNmdj308hkGHtkId7M4YFns4mk/yfB4a48602eDI+mlNznu0FMPFd0wYKCaewl8F0EOueqM+yKSLVfJrNKWnSw/FZGzEGXFG9sy/px4gEBW1");
        basicBastion = Schematics.readBase64("bXNjaAF4nGNgZmBmZmDJS8xNZWBNzMsEUtwpqcXJRZkFJZn5eQyClfmlCin5Cnn5JQqpFZnFJVwMbDmJSak5xQxM0bGMDDzJ+UWpukmJxWDVDAyMIAQkACJxFp8=");
        basicAegis = Schematics.readBase64("bXNjaAF4nGNgYWBhZmDJS8xNZeDPyc/PVkhKzcjMS1GozC9l4E5JLU4uyiwoyczPY2BgYMtJTErNKWZgio5lZOBKzi9K1U1MTc8sBkoxghCQAAA85RNL");
    }
}
