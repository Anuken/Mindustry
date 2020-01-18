package mindustry.content;

import mindustry.ctype.*;
import mindustry.maps.planet.*;
import mindustry.type.*;

public class Planets implements ContentList{
    //TODO make all names
    public static Planet starter;

    @Override
    public void load(){
        starter = new Planet("//TODO"){{
            detail = 6;
            generator = new TestPlanetGenerator();
        }};
    }
}
