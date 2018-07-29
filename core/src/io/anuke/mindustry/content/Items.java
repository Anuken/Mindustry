package io.anuke.mindustry.content;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemType;

public class Items implements ContentList{
    public static Item stone, tungsten, lead, coal, carbide, titanium, thorium, silicon, plastanium, phasematter, surgealloy,
            biomatter, sand, blastCompound, pyratite;

    @Override
    public void load(){

        stone = new Item("stone", Color.valueOf("777777")){{
            hardness = 3;
        }};

        tungsten = new Item("tungsten", Color.valueOf("a0b0c8")){{
            type = ItemType.material;
            hardness = 1;
            cost = 0.75f;
        }};

        lead = new Item("lead", Color.valueOf("8e85a2")){{
            type = ItemType.material;
            hardness = 1;
            cost = 0.6f;
        }};

        coal = new Item("coal", Color.valueOf("272727")){{
            explosiveness = 0.2f;
            flammability = 0.5f;
            hardness = 2;
        }};

        carbide = new Item("carbide", Color.valueOf("e2e2e2")){{
            type = ItemType.material;
        }};

        titanium = new Item("titanium", Color.valueOf("8da1e3")){{
            type = ItemType.material;
            hardness = 3;
            cost = 1.1f;
        }};

        thorium = new Item("thorium", Color.valueOf("f9a3c7")){{
            type = ItemType.material;
            explosiveness = 0.1f;
            hardness = 4;
            radioactivity = 0.5f;
            cost = 1.2f;
        }};

        silicon = new Item("silicon", Color.valueOf("53565c")){{
            type = ItemType.material;
            cost = 0.9f;
        }};

        plastanium = new Item("plastanium", Color.valueOf("e9ead3")){{
            type = ItemType.material;
            flammability = 0.1f;
            explosiveness = 0.1f;
            cost = 1.5f;
        }};

        phasematter = new Item("phase-matter", Color.valueOf("f4ba6e")){{
            type = ItemType.material;
            cost = 1.5f;
        }};

        surgealloy = new Item("surge-alloy", Color.valueOf("b4d5c7")){
            {
                type = ItemType.material;
            }

            @Override
            public boolean isHidden(){
                return true;
            }
        };

        biomatter = new Item("biomatter", Color.valueOf("648b55")){{
            flammability = 0.4f;
            fluxiness = 0.2f;
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
    public Array<? extends Content> getAll(){
        return Item.all();
    }
}
