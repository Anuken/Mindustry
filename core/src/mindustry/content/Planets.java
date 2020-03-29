package mindustry.content;

import arc.graphics.*;
import mindustry.ctype.*;
import mindustry.graphics.g3d.*;
import mindustry.maps.planet.*;
import mindustry.type.*;

public class Planets implements ContentList{
    public static Planet
    sun,
    starter;

    @Override
    public void load(){
        sun = new Planet("sun", null, 0, 2){{
            bloom = true;
            //lightColor = Color.valueOf("f4ee8e");
            meshLoader = () -> new SunMesh(this, 3){{
                setColors(
                    1.1f,
                    Color.valueOf("ff7a38"),
                    Color.valueOf("ff9638"),
                    Color.valueOf("ffc64c"),
                    Color.valueOf("ffc64c"),
                    Color.valueOf("ffe371"),
                    Color.valueOf("f4ee8e")
                );

                scale = 1f;
                speed = 1000f;
                falloff = 0.3f;
                octaves = 4;
                spread = 1.2f;
                magnitude = 0f;
            }};
        }};

        starter = new Planet("TODO", sun, 3, 1){{
            generator = new TODOPlanetGenerator();
            meshLoader = () -> new HexMesh(this, 6);
            atmosphereColor = Color.valueOf("3c1b8f");
        }};
    }
}
