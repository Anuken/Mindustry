package io.anuke.mindustry.content;

import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.game.Rules;
import io.anuke.mindustry.maps.generators.MapGenerator;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Zone;

public class Zones implements ContentList{
    public Zone groundZero;

    @Override
    public void load(){

        groundZero = new Zone("groundZero", new MapGenerator("groundZero")){{
            deployCost = ItemStack.with(Items.copper, 100);
            startingItems = ItemStack.with(Items.copper, 50);
            alwaysUnlocked = true;
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 60;
            }};
        }};
    }
}
