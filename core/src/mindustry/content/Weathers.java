package mindustry.content;

import mindustry.ctype.*;
import mindustry.type.*;

public class Weathers implements ContentList{
    public static Weather
    rain,
    snow;

    @Override
    public void load(){
        snow = new Weather("snow"){

            @Override
            public void draw(){
                //TODO
            }
        };
    }
}
