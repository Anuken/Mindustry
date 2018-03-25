package io.anuke.mindustry.content;

import io.anuke.mindustry.resource.Item;

public class Items {
    public static final Item

    stone = new Item("stone") {
        {
            material = false;
        }
    },
    iron = new Item("iron"),
    lead = new Item("lead"),
    coal = new Item("coal") {
        {
            explosiveness = 0.2f;
            flammability = 0.5f;
            fluxiness = 0.5f;
            material = false;
        }
    },
    steel = new Item("steel"),
    titanium = new Item("titanium"),
    thorium = new Item("thorium") {
        {
            explosiveness = 0.1f;
        }
    },
    silicon = new Item("silicon"),
    plastic = new Item("plastic"),
    densealloy = new Item("densealloy"),
    biomatter = new Item("biomatter") {
        {
            material = false;
            flammability = 0.4f;
        }
    };
}
