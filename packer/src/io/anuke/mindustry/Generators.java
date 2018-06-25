package io.anuke.mindustry;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.type.Upgrade;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.OreBlock;

public class Generators {

    public static void generate(ImageContext context){

        context.generate("block-icons", () -> {
            for(Block block : Block.all()){
                TextureRegion[] regions = block.getBlockIcon();

                if(regions.length == 0){
                    continue;
                }

                if(regions[0] == null){
                    context.err("Error in block \"{0}\": null region!", block.name);
                }

                Image image = context.get(regions[0]);

                for(TextureRegion region : regions){
                    image.draw(region);
                }

                image.save("block-icon-" + block.name);
            }
        });

        context.generate("mech-icons", () -> {
            for(Upgrade upgrade : Upgrade.all()){
                if(!(upgrade instanceof Mech)) continue;

                Mech mech = (Mech)upgrade;

                mech.load();
                mech.weapon.load();

                Image image = context.get(mech.region);

                if(!mech.flying){
                    image.draw(mech.baseRegion);
                    image.draw(mech.legRegion);
                    image.draw(mech.legRegion, true, false);
                    image.draw(mech.region);
                }

                image.draw(mech.weapon.equipRegion, false, false);
                image.draw(mech.weapon.equipRegion, true, false);


                image.save("mech-icon-" + mech.name);
            }
        });

        context.generate("ore-icons", () -> {
            for(Block block : Block.all()){
                if(!(block instanceof OreBlock)) continue;

                OreBlock ore = (OreBlock)block;
                Item item = ore.drops.item;
                Block base = ore.base;

                for (int i = 0; i < 3; i++) {
                    //get base image to draw on
                    Image image = context.get(base.name + (i+1));
                    Image shadow = context.get(item.name + (i+1));

                    for (int x = 0; x < image.width(); x++) {
                        for (int y = 1; y < image.height(); y++) {
                            Color color = shadow.getColor(x, y - 1);

                            //draw semi transparent background
                            if(color.a > 0.001f){
                                color.set(0, 0, 0, 0.3f);
                                image.draw(x, y, color);
                            }
                        }
                    }

                    image.draw(context.get(item.name + (i+1)));
                    image.save("ore-" + item.name + "-" + base.name + (i+1));
                }

            }
        });
    }

}
