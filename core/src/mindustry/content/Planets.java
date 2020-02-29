package mindustry.content;

import mindustry.ctype.*;
import mindustry.maps.planet.*;
import mindustry.type.*;

public class Planets implements ContentList{
    public static Planet
    sun,
    starter;

    @Override
    public void load(){
        sun = new Planet("sun", null, 3, 1){{
            detail = 6;
            generator = new TestPlanetGenerator();
        }};

        starter = new Planet("TODO", sun, 3, 1){{
            detail = 6;
            generator = new TestPlanetGenerator();
        }};
    }
}
