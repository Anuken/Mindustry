package io.anuke.mindustry.content;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemType;

public class Items implements ContentList{
    public static Item stone, copper, lead, densealloy, coal, titanium, thorium, silicon, plastanium, phasefabric, surgealloy,
            biomatter, sand, blastCompound, pyratite;

    @Override
    public void load(){
        copper = new Item("copper", Color.valueOf("d99d73")){{
            type = ItemType.material;
            hardness = 1;
            cost = 0.6f;
            genOre = true;
            alwaysUnlocked = true;
        }};

        lead = new Item("lead", Color.valueOf("8c7fa9")){{
            type = ItemType.material;
            hardness = 1;
            cost = 0.9f;
            genOre = true;
        }};

        densealloy = new Item("dense-alloy", Color.valueOf("b2c6d2")){{
            type = ItemType.material;
            cost = 1.2f;
        }};

        coal = new Item("coal", Color.valueOf("272727")){{
            explosiveness = 0.2f;
            flammability = 0.5f;
            hardness = 2;
            genOre = true;
        }};

        titanium = new Item("titanium", Color.valueOf("8da1e3")){{
            type = ItemType.material;
            hardness = 3;
            cost = 1.1f;
            genOre = true;
        }};

        thorium = new Item("thorium", Color.valueOf("f9a3c7")){{
            type = ItemType.material;
            explosiveness = 0.1f;
            hardness = 4;
            radioactivity = 0.5f;
            cost = 1.4f;
            genOre = true;
        }};

        stone = new Item("stone", Color.valueOf("777777")){{
            hardness = 3;
        }};

        silicon = new Item("silicon", Color.valueOf("53565c")){{
            type = ItemType.material;
            cost = 0.9f;
        }};

        plastanium = new Item("plastanium", Color.valueOf("cbd97f")){{
            type = ItemType.material;
            flammability = 0.1f;
            explosiveness = 0.1f;
            cost = 1.6f;
        }};

        phasefabric = new Item("phase-fabric", Color.valueOf("f4ba6e")){{
            type = ItemType.material;
            cost = 1.5f;
            fluxiness = 0.9f;
            radioactivity = 0.3f;
        }};

        surgealloy = new Item("surge-alloy", Color.valueOf("f3e979")){{
            type = ItemType.material;
        }};

        biomatter = new Item("biomatter", Color.valueOf("648b55")){{
            flammability = 0.55f;
            fluxiness = 0.3f;
        }};

        sand = new Item("sand", Color.valueOf("e3d39e")){{
            fluxiness = 0.5f;
        }};

        blastCompound = new Item("blast-compound", Color.valueOf("ff795e")){{
            flammability = 0.2f;
            explosiveness = 0.6f;
        }};

        pyratite = new Item("pyratite", Color.valueOf("ffaa5f")){{
            flammability = 0.7f;
            explosiveness = 0.2f;
        }};
    }

    @Override
    public ContentType type(){
        return ContentType.item;
    }
}
