package io.anuke.mindustry;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.mindustry.world.blocks.OreBlock;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Hue;
import static io.anuke.mindustry.Vars.*;

public class Generators {

    public static void generate(ImageContext context){

        context.generate("block-icons", () -> {
            for(Block block : content.blocks()){
                TextureRegion[] regions = block.getBlockIcon();

                if(regions.length == 0){
                    continue;
                }

                if(block.turretIcon){
                    Color color = Color.ROYAL;

                    Image image = context.get(block.name);

                    Image read = context.create(image.width(), image.height());
                    read.draw(image);

                    for (int x = 0; x < image.width(); x++) {
                        for (int y = 0; y < image.height(); y++) {
                            if(read.isEmpty(x, y) &&
                                    (!read.isEmpty(x, y + 1) || !read.isEmpty(x, y - 1) || !read.isEmpty(x + 1, y) || !read.isEmpty(x - 1, y))){
                                image.draw(x, y, color);
                            }
                        }
                    }

                    Image base = context.get("block-" + block.size);
                    Image top = context.get("block-" + block.size + "-top");

                    for (int x = 0; x < base.width(); x++) {
                        for (int y = 0; y < base.height(); y++) {
                            Color result = top.getColor(x, y);
                            if(result.a > 0.01f){
                                Hue.mix(result, color, 0.45f, result);
                                base.draw(x, y, result);
                            }
                        }
                    }

                    base.draw(image);

                    base.save("block-icon-" + block.name);
                }else {

                    Image image = context.get(regions[0]);

                    for (TextureRegion region : regions) {
                        image.draw(region);
                    }

                    image.save("block-icon-" + block.name);
                }
            }
        });

        context.generate("mech-icons", () -> {
            for(Mech mech : content.<Mech>getBy(ContentType.mech)){

                mech.load();
                mech.weapon.load();

                Image image = context.get(mech.region);

                if(!mech.flying){
                    image.drawCenter(mech.baseRegion);
                    image.drawCenter(mech.legRegion);
                    image.drawCenter(mech.legRegion, true, false);
                    image.drawCenter(mech.region);
                }

                int off = (image.width() - mech.weapon.equipRegion.getRegionWidth())/2;

                image.draw(mech.weapon.equipRegion, -(int)mech.weaponOffsetX + off, (int)mech.weaponOffsetY + off, false, false);
                image.draw(mech.weapon.equipRegion, (int)mech.weaponOffsetX + off, (int)mech.weaponOffsetY + off, true, false);


                image.save("mech-icon-" + mech.name);
            }
        });

        context.generate("unit-icons", () -> {
            for(UnitType type : content.<UnitType>getBy(ContentType.unit)){

                type.load();
                type.weapon.load();

                Image image = context.get(type.region);

                if(!type.isFlying){
                    image.draw(type.baseRegion);
                    image.draw(type.legRegion);
                    image.draw(type.legRegion, true, false);
                    image.draw(type.region);

                    image.draw(type.weapon.equipRegion,
                            -(int)type.weaponOffsetX + (image.width() - type.weapon.equipRegion.getRegionWidth())/2,
                            (int)type.weaponOffsetY - (image.height() - type.weapon.equipRegion.getRegionHeight())/2 + 1,
                            false, false);
                    image.draw(type.weapon.equipRegion,
                            (int)type.weaponOffsetX + (image.width() - type.weapon.equipRegion.getRegionWidth())/2,
                            (int)type.weaponOffsetY - (image.height() - type.weapon.equipRegion.getRegionHeight())/2 + 1,
                            true, false);
                }

                image.save("unit-icon-" + type.name);
            }
        });

        context.generate("liquid-icons", () -> {
            for(Liquid liquid : content.liquids()){
                Image image = context.get("liquid-icon");
                for (int x = 0; x < image.width(); x++) {
                    for (int y = 0; y < image.height(); y++) {
                        Color color = image.getColor(x, y);
                        color.mul(liquid.color);
                        image.draw(x, y, color);
                    }
                }

                image.save("liquid-icon-" + liquid.name);
            }
        });

        context.generate("block-edges", () -> {
            for(Block block : content.blocks()){
                if(!(block instanceof Floor)) continue;
                Floor floor = (Floor)block;
                if(floor.getIcon().length > 0 && !Draw.hasRegion(floor.name + "-cliff-side")){
                    Image floori = context.get(floor.getIcon()[0]);
                    Color color = floori.getColor(0, 0).mul(1.3f, 1.3f, 1.3f, 1f);

                    String[] names = {"cliff-edge-2", "cliff-edge", "cliff-edge-1", "cliff-side"};
                    for(String str : names){
                        Image image = context.get("generic-" + str);

                        for(int x = 0; x < image.width(); x++){
                            for(int y = 0; y < image.height(); y++){
                                Color other = image.getColor(x, y);
                                if(other.a > 0){
                                    image.draw(x, y, color);
                                }
                            }
                        }

                        image.save(floor.name + "-" + str);
                    }
                }
            }
        });

        context.generate("ore-icons", () -> {
            for(Block block : content.blocks()){
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
