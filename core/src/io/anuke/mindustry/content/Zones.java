package io.anuke.mindustry.content;

import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Zone;

public class Zones implements ContentList{
    public Zone wasteland;

    @Override
    public void load(){

        wasteland = new Zone("wasteland"){{
            deployCost = new ItemStack[]{new ItemStack(Items.copper, 2)};
        }};
    }
}
