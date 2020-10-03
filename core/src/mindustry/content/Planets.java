package mindustry.content;

import arc.graphics.*;
import mindustry.ctype.*;
import mindustry.graphics.g3d.*;
import mindustry.maps.planet.*;
import mindustry.type.*;

public class Planets implements ContentList{
    public static Planet
    sun,
    serpulo;

    @Override
    public void load(){
        sun = new Planet("sun", null, 0, 2){{
            bloom = true;

            //lightColor = Color.valueOf("f4ee8e");

            meshLoader = () -> new SunMesh(
                this, 4,
                5, 0.3, 1.7, 1.2, 1,
                1.1f,
                Color.valueOf("ff7a38"),
                Color.valueOf("ff9638"),
                Color.valueOf("ffc64c"),
                Color.valueOf("ffc64c"),
                Color.valueOf("ffe371"),
                Color.valueOf("f4ee8e")
            );
        }};

        serpulo = new Planet("serpulo", sun, 3, 1){{
            generator = new SerpuloPlanetGenerator();
            meshLoader = () -> new HexMesh(this, 6);
            atmosphereColor = Color.valueOf("3c1b8f");
            startSector = 15;
        }};
    }
}
