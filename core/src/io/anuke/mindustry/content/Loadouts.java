package io.anuke.mindustry.content;

import io.anuke.mindustry.ctype.*;
import io.anuke.mindustry.game.*;

import java.io.*;

public class Loadouts implements ContentList{
    public static Schematic
    basicShard,
    advancedShard,
    basicFoundation,
    basicNucleus;

    @Override
    public void load(){
        try{
            basicShard = Schematics.readBase64("bXNjaAB4nD2K2wqAIBiD5ymibnoRn6YnEP1BwUMoBL19FuJ2sbFvUFgYZDaJsLeQrkinN9UJHImsNzlYE7WrIUastuSbnlKx2VJJt+8IQGGKdfO/8J5yrGJSMegLg+YUIA==");
            advancedShard = Schematics.readBase64("bXNjaAB4nD2LjQqAIAyET7OMIOhFfJqeYMxBgSkYCL199gu33fFtB4tOwUTaBCP5QpHFzwtl32DahBeKK1NwPq8hoOcUixwpY+CUxe3XIwBbB/pa6tadVCUP02hgHvp5vZq/0b7pBHPYFOQ=");
            basicFoundation = Schematics.readBase64("bXNjaAB4nD1OSQ6DMBBzFhVu8BG+0X8MQyoiJTNSukj8nlCi2Adbtg/GA4OBF8oB00rvyE/9ykafqOIw58A7SWRKy1ZiShhZ5RcOLZhYS1hefQ1gRIeptH9jq/qW2lvc1d2tgWsOfVX/tOwE86AYBA==");
            basicNucleus = Schematics.readBase64("bXNjaAB4nD2MUQqAIBBEJy0s6qOLdJXuYNtCgikYBd2+LNmdj308hkGHtkId7M4YFns4mk/yfB4a48602eDI+mlNznu0FMPFd0wYKCaewl8F0EOueqM+yKSLVfJrNKWnSw/FZGzEGXFG9sy/px4gEBW1");
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }
}
