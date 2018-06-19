package io.anuke.mindustry;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.type.Upgrade;
import io.anuke.mindustry.world.Block;

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
    }

}
