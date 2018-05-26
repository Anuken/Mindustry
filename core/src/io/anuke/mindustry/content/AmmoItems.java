package io.anuke.mindustry.content;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemType;

public class AmmoItems {
    public static final Item

    leadBullet = new Item("lead-bullet", Color.valueOf("8e85a2")){{
        type = ItemType.ammo;
    }},

    armorPiercingBullet = new Item("armor-piercing-bullet", Color.valueOf("f9a3c7")){{
        type = ItemType.ammo;
    }},

    homingBullet = new Item("homing-bullet", Color.valueOf("6a6c72")){{
        type = ItemType.ammo;
    }},

    tracerBullet = new Item("tracer-bullet", Color.valueOf("ffe58b")){{
        type = ItemType.ammo;
    }},

    compositeFlak = new Item("composite-flak", Color.valueOf("e9ead3")){{
        type = ItemType.ammo;
    }},

    explosiveShell = new Item("explosive-shell", Color.valueOf("ff795e")){{
        type = ItemType.ammo;
    }},

    fragShell = new Item("frag-shell", Color.valueOf("e9ead3")){{
        type = ItemType.ammo;
    }},

    thoriumShell = new Item("thorium-shell", Color.valueOf("f9a3c7")){{
        type = ItemType.ammo;
    }},

    swarmMissile = new Item("swarm-missile", Color.valueOf("ff795e")){{
        type = ItemType.ammo;
    }},

    scytheMissile = new Item("scythe-missile", Color.valueOf("f9a3c7")){{
        type = ItemType.ammo;
    }},

    incendiaryMortarShell = new Item("incendiary-mortar-shell", Color.valueOf("ffe58b")){{
        type = ItemType.ammo;
    }},

    surgeMortarShell = new Item("surge-mortar-shell", Color.valueOf("bcddff")){{
        type = ItemType.ammo;
    }};
}
