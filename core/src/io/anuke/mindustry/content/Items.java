package io.anuke.mindustry.content;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.resource.Item;

public class Items {
    public static final Item

    stone = new Item("stone", Color.valueOf("777777")) {
        {
            material = false;
            hardness = 2;
        }
    },
    iron = new Item("iron", Color.valueOf("bc8271")){
        {
            hardness = 1;
        }
    },
    lead = new Item("lead", Color.valueOf("8e85a2")){
        {
            hardness = 1;
        }
    },
    coal = new Item("coal", Color.valueOf("272727")) {
        {
            explosiveness = 0.2f;
            flammability = 0.5f;
            fluxiness = 0.3f;
            material = false;
            hardness = 2;
        }
    },
    steel = new Item("steel", Color.valueOf("e2e2e2")),
    titanium = new Item("titanium", Color.valueOf("8da1e3")){
        {
            hardness = 3;
        }
    },
    thorium = new Item("thorium", Color.valueOf("bb80bd")) {
        {
            explosiveness = 0.1f;
            hardness = 4;
        }
    },
    silicon = new Item("silicon", Color.valueOf("53565c")),
    plastic = new Item("plastic", Color.valueOf("e9ead3")),
    densealloy = new Item("densealloy", Color.valueOf("b4d5c7")),
    biomatter = new Item("biomatter", Color.valueOf("648b55")) {
        {
            material = false;
            flammability = 0.4f;
            fluxiness = 0.2f;
        }
    },
    sand = new Item("sand", Color.valueOf("e3d39e")){
        {
            material = false;
            fluxiness = 0.5f;
        }
    };
}
