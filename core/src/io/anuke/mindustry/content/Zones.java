package io.anuke.mindustry.content;

import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.maps.generators.BasicGenerator;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Zone;

public class Zones implements ContentList{
    public Zone wasteland;

    @Override
    public void load(){

        wasteland = new Zone("wasteland", new BasicGenerator(256, 256, Items.lead, Items.copper)){{
            deployCost = new ItemStack[]{new ItemStack(Items.copper, 2)};
        }};
    }
}
