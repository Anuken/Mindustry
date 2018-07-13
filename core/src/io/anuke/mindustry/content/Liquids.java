package io.anuke.mindustry.content;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.type.Liquid;

public class Liquids implements ContentList{
    public static Liquid water, lava, oil, cryofluid;

    @Override
    public void load(){

        water = new Liquid("water", Color.valueOf("486acd")){
            {
                heatCapacity = 0.4f;
                tier = 0;
                effect = StatusEffects.wet;
            }
        };

        lava = new Liquid("lava", Color.valueOf("e37341")){
            {
                temperature = 0.8f;
                viscosity = 0.8f;
                tier = 2;
                effect = StatusEffects.melting;
            }
        };

        oil = new Liquid("oil", Color.valueOf("313131")){
            {
                viscosity = 0.7f;
                flammability = 0.6f;
                explosiveness = 0.6f;
                tier = 1;
                effect = StatusEffects.tarred;
            }
        };

        cryofluid = new Liquid("cryofluid", Color.SKY){
            {
                heatCapacity = 0.9f;
                temperature = 0.25f;
                tier = 1;
                effect = StatusEffects.freezing;
            }
        };
    }

    @Override
    public Array<? extends Content> getAll(){
        return Liquid.all();
    }
}
