package io.anuke.mindustry;

import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Block.Icon;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.mindustry.world.blocks.OreBlock;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.tilesize;

public class Generators {

    public static void generate(){

        ImagePacker.generate("block-icons", () -> {
            for(Block block : content.blocks()){
                TextureRegion[] regions = block.getGeneratedIcons();

                if(regions.length == 0){
                    continue;
                }

                try{
                    Image image = ImagePacker.get(regions[0]);

                    for(TextureRegion region : regions){
                        image.draw(region);
                    }

                    if(regions.length > 1){
                        image.save(block.name + "-icon-full");
                    }

                    for(Icon icon : Icon.values()){
                        if(icon.size == 0) continue;
                        Image scaled = new Image(icon.size, icon.size);
                        scaled.drawScaled(image);
                        scaled.save(block.name + "-icon-" + icon.name());
                    }
                }catch(IllegalArgumentException e){
                    Log.info("Skipping &ly'{0}'", block.name);
                }catch(NullPointerException e){
                    Log.err("Block &ly'{0}'&lr has an null region!");
                }
            }
        });

        ImagePacker.generate("mech-icons", () -> {
            for(Mech mech : content.<Mech>getBy(ContentType.mech)){

                mech.load();
                mech.weapon.load();

                Image image = ImagePacker.get(mech.region);

                if(!mech.flying){
                    image.drawCenter(mech.baseRegion);
                    image.drawCenter(mech.legRegion);
                    image.drawCenter(mech.legRegion, true, false);
                    image.drawCenter(mech.region);
                }

                int off = (image.width() - mech.weapon.equipRegion.getWidth())/2;

                image.draw(mech.weapon.equipRegion, -(int)mech.weaponOffsetX + off, (int)mech.weaponOffsetY + off, false, false);
                image.draw(mech.weapon.equipRegion, (int)mech.weaponOffsetX + off, (int)mech.weaponOffsetY + off, true, false);


                image.save("mech-icon-" + mech.name);
            }
        });

        ImagePacker.generate("unit-icons", () -> {
            for(UnitType type : content.<UnitType>getBy(ContentType.unit)){

                type.load();
                type.weapon.load();

                Image image = ImagePacker.get(type.region);

                if(!type.isFlying){
                    image.draw(type.baseRegion);
                    image.draw(type.legRegion);
                    image.draw(type.legRegion, true, false);
                    image.draw(type.region);

                    image.draw(type.weapon.equipRegion,
                            -(int)type.weaponOffsetX + (image.width() - type.weapon.equipRegion.getWidth())/2,
                            (int)type.weaponOffsetY - (image.height() - type.weapon.equipRegion.getHeight())/2 + 1,
                            false, false);
                    image.draw(type.weapon.equipRegion,
                            (int)type.weaponOffsetX + (image.width() - type.weapon.equipRegion.getWidth())/2,
                            (int)type.weaponOffsetY - (image.height() - type.weapon.equipRegion.getHeight())/2 + 1,
                            true, false);
                }

                image.save("unit-icon-" + type.name);
            }
        });

        ImagePacker.generate("ore-icons", () -> {
            for(Block block : content.blocks()){
                if(!(block instanceof OreBlock)) continue;

                OreBlock ore = (OreBlock)block;
                Item item = ore.itemDrop;
                Block base = ore.base;

                for (int i = 0; i < 3; i++) {
                    //get base image to draw on
                    Image image = ImagePacker.get(base.name + (i+1));
                    Image shadow = ImagePacker.get(item.name + (i+1));

                    int offset = image.width()/tilesize;

                    for (int x = 0; x < image.width(); x++) {
                        for (int y = offset; y < image.height(); y++) {
                            Color color = shadow.getColor(x, y - offset);

                            //draw semi transparent background
                            if(color.a > 0.001f){
                                color.set(0, 0, 0, 0.3f);
                                image.draw(x, y, color);
                            }
                        }
                    }

                    image.draw(ImagePacker.get(item.name + (i+1)));
                    image.save("ore-" + item.name + "-" + base.name + (i+1));

                    //save icons
                    image.save(block.name + "-icon-full");
                    for(Icon icon : Icon.values()){
                        if(icon.size == 0) continue;
                        Image scaled = new Image(icon.size, icon.size);
                        scaled.drawScaled(image);
                        scaled.save(block.name + "-icon-" + icon.name());
                    }
                }
            }
        });

        ImagePacker.generate("edges", () -> {
            for(Block block : content.blocks()){
                if(!(block instanceof Floor)) continue;

                Floor floor = (Floor)block;

                if(ImagePacker.has(floor.name + "-edge")){
                    continue;
                }

                try{
                    Image image = ImagePacker.get(floor.generateIcons()[0]);
                    Image edge = ImagePacker.get("edge-stencil-" + floor.edgeStyle);
                    Image result = new Image(edge.width(), edge.height());

                    for(int x = 0; x < edge.width(); x++){
                        for(int y = 0; y < edge.height(); y++){
                            result.draw(x, y, edge.getColor(x, y).mul(image.getColor(x % image.width(), y % image.height())));
                        }
                    }

                    result.save(floor.name + "-edge");

                }catch(Exception ignored){}
            }
        });
    }

}
