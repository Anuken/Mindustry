package mindustry.tools;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.tools.ImagePacker.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.legacy.*;

import static mindustry.Vars.*;

public class Generators{

    public static void generate(){
        ObjectMap<Block, Image> gens = new ObjectMap<>();

        ImagePacker.generate("splashes", () -> {
            ArcNativesLoader.load();

            int frames = 12;
            int size = 32;
            for(int i = 0; i < frames; i++){
                float fin = (float)i / (frames);
                float fout = 1f - fin;
                float stroke = 3.5f * fout;
                float radius = (size/2f) * fin;

                Pixmap pixmap = new Pixmap(size, size);

                pixmap.each((x, y) -> {
                    float dst = Mathf.dst(x, y, size/2f, size/2f);
                    if(Math.abs(dst - radius) <= stroke){
                        pixmap.draw(x, y, Color.white);
                    }
                });

                Fi.get("splash-" + i + ".png").writePNG(pixmap);

                pixmap.dispose();
            }
        });

        ImagePacker.generate("cracks", () -> {
            RidgedPerlin r = new RidgedPerlin(1, 3);
            for(int size = 1; size <= Block.maxCrackSize; size++){
                int dim = size * 32;
                int steps = Block.crackRegions;
                for(int i = 0; i < steps; i++){
                    float fract = i / (float)steps;

                    Image image = new Image(dim, dim);
                    for(int x = 0; x < dim; x++){
                        for(int y = 0; y < dim; y++){
                            float dst = Mathf.dst((float)x/dim, (float)y/dim, 0.5f, 0.5f) * 2f;
                            if(dst < 1.2f && r.getValue(x, y, 1f / 40f) - dst*(1f-fract) > 0.16f){
                                image.draw(x, y, Color.white);
                            }
                        }
                    }

                    Image output = new Image(image.width, image.height);
                    int rad = 3;

                    //median filter
                    for(int x = 0; x < output.width; x++){
                        for(int y = 0; y < output.height; y++){
                            int whites = 0, clears = 0;
                            for(int cx = -rad; cx < rad; cx++){
                                for(int cy = -rad; cy < rad; cy++){
                                    int wx = Mathf.clamp(cx + x, 0, output.width - 1), wy = Mathf.clamp(cy + y, 0, output.height - 1);
                                    Color color = image.getColor(wx, wy);
                                    if(color.a > 0.5f){
                                        whites ++;
                                    }else{
                                        clears ++;
                                    }
                                }
                            }
                            output.draw(x, y, whites >= clears ? Color.white : Color.clear);
                        }
                    }

                    output.save("cracks-" + size + "-" + i);
                }
            }
        });

        ImagePacker.generate("block-icons", () -> {
            Image colors = new Image(content.blocks().size, 1);

            for(Block block : content.blocks()){
                if(block.isAir() || block instanceof BuildBlock || block instanceof OreBlock || block instanceof LegacyBlock) continue;

                block.load();

                TextureRegion[] regions = block.getGeneratedIcons();

                if(block instanceof Floor){
                    for(TextureRegion region : block.variantRegions()){
                        GenRegion gen = (GenRegion)region;
                        if(gen.path == null) continue;
                        gen.path.copyTo(Fi.get("../editor/editor-" + gen.path.name()));
                    }
                }

                if(regions.length == 0){
                    continue;
                }

                try{
                    Image last = null;
                    if(block.outlineIcon){
                        int radius = 4;
                        GenRegion region = (GenRegion)regions[regions.length - 1];
                        Image base = ImagePacker.get(region);
                        Image out = last = new Image(region.getWidth(), region.getHeight());
                        for(int x = 0; x < out.width; x++){
                            for(int y = 0; y < out.height; y++){

                                Color color = base.getColor(x, y);
                                out.draw(x, y, color);
                                if(color.a < 1f){
                                    boolean found = false;
                                    outer:
                                    for(int rx = -radius; rx <= radius; rx++){
                                        for(int ry = -radius; ry <= radius; ry++){
                                            if(Mathf.dst(rx, ry) <= radius && base.getColor(rx + x, ry + y).a > 0.01f){
                                                found = true;
                                                break outer;
                                            }
                                        }
                                    }
                                    if(found){
                                        out.draw(x, y, block.outlineColor);
                                    }
                                }
                            }
                        }

                        region.path.delete();

                        out.save(block.name);
                    }

                    Image image = ImagePacker.get(regions[0]);

                    int i = 0;
                    for(TextureRegion region : regions){
                        i++;
                        if(i != regions.length || last == null){
                            image.draw(region);
                        }else{
                            image.draw(last);
                        }
                    }

                    if(!(regions.length == 1 && regions[0] == Core.atlas.find(block.name))){
                        image.save("block-" + block.name + "-full");
                    }

                    image.save("../editor/" + block.name + "-icon-editor");

                    for(Cicon icon : Cicon.scaled){
                        Image scaled = new Image(icon.size, icon.size);
                        scaled.drawScaled(image);
                        scaled.save("../ui/block-" + block.name + "-" + icon.name());
                    }

                    boolean hasEmpty = false;
                    Color average = new Color();
                    for(int x = 0; x < image.width; x++){
                        for(int y = 0; y < image.height; y++){
                            Color color = image.getColor(x, y);
                            average.r += color.r;
                            average.g += color.g;
                            average.b += color.b;
                            if(color.a < 0.9f){
                                hasEmpty = true;
                            }
                        }
                    }
                    average.mul(1f / (image.width * image.height));
                    if(block instanceof Floor){
                        average.mul(0.8f);
                    }else{
                        average.mul(1.1f);
                    }
                    //encode square sprite in alpha channel
                    average.a = hasEmpty ? 0.1f : 1f;
                    colors.draw(block.id, 0, average);
                }catch(NullPointerException e){
                    Log.err("Block &ly'@'&lr has an null region!", block);
                }
            }

            colors.save("../../../assets/sprites/block_colors");
        });

        ImagePacker.generate("shallows", () -> {
            content.blocks().<ShallowLiquid>each(b -> b instanceof ShallowLiquid, floor -> {
                Image overlay = ImagePacker.get(floor.liquidBase.region);
                int index = 0;
                for(TextureRegion region : floor.floorBase.variantRegions()){
                    Image res = new Image(32, 32);
                    res.draw(ImagePacker.get(region));
                    for(int x = 0; x < res.width; x++){
                        for(int y = 0; y < res.height; y++){
                            Color color = overlay.getColor(x, y).a(floor.liquidOpacity);
                            res.draw(x, y, color);
                        }
                    }

                    String name = floor.name + "" + (++index);
                    res.save("../blocks/environment/" + name);
                    res.save("../editor/editor-" + name);

                    gens.put(floor, res);
                }
            });
        });

        ImagePacker.generate("item-icons", () -> {
            for(UnlockableContent item : Seq.<UnlockableContent>withArrays(content.items(), content.liquids())){
                Image base = ImagePacker.get(item.getContentType().name() + "-" + item.name);
                for(Cicon icon : Cicon.scaled){
                    //if(icon.size == base.width) continue;
                    Image image = new Image(icon.size, icon.size);
                    image.drawScaled(base);
                    image.save(item.getContentType().name() + "-" + item.name + "-" + icon.name(), false);

                    if(icon == Cicon.medium){
                        image.save("../ui/" + item.getContentType() + "-" + item.name + "-icon");
                    }
                }
            }
        });

        ImagePacker.generate("unit-icons", () -> content.units().each(type -> {
            if(type.isHidden()) return; //hidden units don't generate

            type.load();

            Image image = ImagePacker.get(type.parts > 0 ? type.partRegions[0] : type.region);
            for(int i = 1; i < type.parts; i++){
                image.draw(ImagePacker.get(type.partRegions[i]));
            }
            if(type.parts > 0){
                image.save(type.name);
            }

            if(type.constructor.get() instanceof Mechc){
                image.drawCenter(type.baseRegion);
                image.drawCenter(type.legRegion);
                image.drawCenter(type.legRegion, true, false);
                image.draw(type.region);
            }

            Image baseCell = ImagePacker.get(type.parts > 0 ? type.partCellRegions[0] : type.cellRegion);
            for(int i = 1; i < type.parts; i++){
                baseCell.draw(ImagePacker.get(type.partCellRegions[i]));
            }

            if(type.parts > 0){
                image.save(type.name + "-cell");
            }

            Image cell = new Image(type.cellRegion.getWidth(), type.cellRegion.getHeight());
            cell.each((x, y) -> cell.draw(x, y, baseCell.getColor(x, y).mul(Color.valueOf("ffa665"))));

            image.draw(cell, image.width / 2 - cell.width / 2, image.height / 2 - cell.height / 2);

            for(Weapon weapon : type.weapons){
                weapon.load();

                for(int i : (weapon.mirror ? Mathf.signs : Mathf.one)){
                    i *= Mathf.sign(weapon.flipped);

                    image.draw(weapon.region,
                    (int)(i * weapon.x / Draw.scl + image.width / 2 - weapon.region.getWidth() / 2),
                    (int)(-weapon.y / Draw.scl + image.height / 2f - weapon.region.getHeight() / 2f),
                    i > 0, false);
                }
            }

            image.save("unit-" + type.name + "-full");
        }));

        ImagePacker.generate("ore-icons", () -> {
            content.blocks().<OreBlock>each(b -> b instanceof OreBlock, ore -> {
                Item item = ore.itemDrop;

                for(int i = 0; i < 3; i++){
                    //get base image to draw on
                    Image image = new Image(32, 32);
                    Image shadow = ImagePacker.get(item.name + (i + 1));

                    int offset = image.width / tilesize - 1;

                    for(int x = 0; x < image.width; x++){
                        for(int y = offset; y < image.height; y++){
                            Color color = shadow.getColor(x, y - offset);

                            //draw semi transparent background
                            if(color.a > 0.001f){
                                color.set(0, 0, 0, 0.3f);
                                image.draw(x, y, color);
                            }
                        }
                    }

                    image.draw(ImagePacker.get(item.name + (i + 1)));
                    image.save("../blocks/environment/ore-" + item.name + (i + 1));
                    image.save("../editor/editor-ore-" + item.name + (i + 1));

                    image.save("block-" + ore.name + "-full");
                    for(Cicon icon : Cicon.scaled){
                        Image scaled = new Image(icon.size, icon.size);
                        scaled.drawScaled(image);
                        scaled.save("../ui/block-" + ore.name + "-" + icon.name());
                    }
                }
            });
        });

        ImagePacker.generate("edges", () -> {
            content.blocks().<Floor>each(b -> b instanceof Floor && !(b instanceof OverlayFloor), floor -> {

                if(ImagePacker.has(floor.name + "-edge") || floor.blendGroup != floor){
                    return;
                }

                try{
                    Image image = gens.get(floor, ImagePacker.get(floor.getGeneratedIcons()[0]));
                    Image edge = ImagePacker.get("edge-stencil");
                    Image result = new Image(edge.width, edge.height);

                    for(int x = 0; x < edge.width; x++){
                        for(int y = 0; y < edge.height; y++){
                            result.draw(x, y, edge.getColor(x, y).mul(image.getColor(x % image.width, y % image.height)));
                        }
                    }

                    result.save("../blocks/environment/" + floor.name + "-edge");

                }catch(Exception ignored){}
            });
        });

        ImagePacker.generate("scorches", () -> {
            for(int size = 0; size < 10; size++){
                for(int i = 0; i < 3; i++){
                    ScorchGenerator gen = new ScorchGenerator();
                    double multiplier = 30;
                    double ss = size * multiplier / 20.0;

                    gen.seed = Mathf.random(100000);
                    gen.size += size*multiplier;
                    gen.scale = gen.size / 80f * 18f;
                    //gen.nscl -= size * 0.2f;
                    gen.octaves += ss/3.0;
                    gen.pers += ss/10.0/5.0;

                    gen.scale += Mathf.range(3f);
                    gen.scale -= ss*2f;
                    gen.nscl -= Mathf.random(1f);

                    Pixmap out = gen.generate();
                    Pixmap median = Pixmaps.median(out, 2, 0.75);
                    Fi.get("../rubble/scorch-" + size + "-" + i + ".png").writePNG(median);
                    out.dispose();
                    median.dispose();
                }
            }
        });
    }

}
