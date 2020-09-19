package mindustry.tools;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.ctype.*;
import mindustry.game.*;
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
    //used for changing colors in the UI - testing only
    static final IntIntMap paletteMap = IntIntMap.with(
    //empty for now
    0x454545ff, 0x00000000,//0x32394bff,
    0x00000099, 0x00000000//0x000000ff
    );

    public static void generate(){
        ObjectMap<Block, Image> gens = new ObjectMap<>();

        if(!paletteMap.isEmpty()){
            ImagePacker.generate("uipalette", () -> {
                Fi.get("../ui").walk(fi -> {
                    if(!fi.extEquals("png")) return;

                    Pixmap pix = new Pixmap(fi);
                    pix.setBlending(Pixmap.Blending.sourceOver);
                    pix.each((x, y) -> {
                        int value = pix.getPixel(x, y);
                        pix.draw(x, y, paletteMap.get(value, value));
                    });

                    fi.writePNG(pix);
                });
            });
        }

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
                if(block.isAir() || block instanceof ConstructBlock || block instanceof OreBlock || block instanceof LegacyBlock) continue;

                block.load();

                TextureRegion[] regions = block.getGeneratedIcons();

                if(block instanceof Floor){
                    for(TextureRegion region : block.variantRegions()){
                        GenRegion gen = (GenRegion)region;
                        if(gen.path == null) continue;
                        gen.path.copyTo(Fi.get("../editor/editor-" + gen.path.name()));
                    }
                }

                Image shardTeamTop = null;

                if(block.teamRegion.found()){
                    Image teamr = ImagePacker.get(block.teamRegion);

                    for(Team team : Team.all){
                        if(team.hasPalette){
                            Image out = new Image(teamr.width, teamr.height);
                            teamr.each((x, y) -> {
                                int color = teamr.getColor(x, y).rgba8888();
                                int index = color == 0xffffffff ? 0 : color == 0xdcc6c6ff ? 1 : color == 0x9d7f7fff ? 2 : -1;
                                out.draw(x, y, index == -1 ? teamr.getColor(x, y) : team.palette[index]);
                            });
                            out.save(block.name + "-team-" + team.name);

                            if(team == Team.sharded){
                                shardTeamTop = out;
                            }
                        }
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
                        Image out = last = new Image(region.width, region.height);
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

                        //draw shard (default team top) on top of first sprite
                        if(i == 1 && shardTeamTop != null){
                            image.draw(shardTeamTop);
                        }
                    }

                    if(!(regions.length == 1 && regions[0] == Core.atlas.find(block.name) && shardTeamTop == null)){
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
                    float asum = 0f;
                    for(int x = 0; x < image.width; x++){
                        for(int y = 0; y < image.height; y++){
                            Color color = image.getColor(x, y);
                            average.r += color.r*color.a;
                            average.g += color.g*color.a;
                            average.b += color.b*color.a;
                            asum += color.a;
                            if(color.a < 0.9f){
                                hasEmpty = true;
                            }
                        }
                    }

                    average.mul(1f / asum);

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

            ObjectSet<String> outlined = new ObjectSet<>();

            try{
                type.load();
                type.init();

                Color outc = Pal.darkerMetal;
                //Func<Image, Image> outlineS = i -> i.shadow(0.8f, 9);
                Func<Image, Image> outline = i -> i.outline(4, outc);
                Cons<TextureRegion> outliner = t -> {
                    if(t != null && t.found()){
                        ImagePacker.replace(t, outline.get(ImagePacker.get(t)));
                    }
                };

                for(Weapon weapon : type.weapons){
                    if(outlined.add(weapon.name) && ImagePacker.has(weapon.name)){
                        outline.get(ImagePacker.get(weapon.name)).save(weapon.name + "-outline");

                        //old outline
                        //ImagePacker.get(weapon.name).outline(4, Pal.darkerMetal).save(weapon.name);
                    }
                }

                //baseRegion, legRegion, region, shadowRegion, cellRegion,
                //        occlusionRegion, jointRegion, footRegion, legBaseRegion, baseJointRegion, outlineRegion;

                outliner.get(type.jointRegion);
                outliner.get(type.footRegion);
                outliner.get(type.legBaseRegion);
                outliner.get(type.baseJointRegion);

                Image image = ImagePacker.get(type.region);

                outline.get(image).save(type.name + "-outline");
                //ImagePacker.replace(type.region, outline.get(image));

                if(type.constructor.get() instanceof Mechc){
                    image.drawCenter(type.baseRegion);
                    image.drawCenter(type.legRegion);
                    image.drawCenter(type.legRegion, true, false);
                    image.draw(type.region);
                }

                Image baseCell = ImagePacker.get(type.cellRegion);
                Image cell = new Image(type.cellRegion.width, type.cellRegion.height);
                cell.each((x, y) -> cell.draw(x, y, baseCell.getColor(x, y).mul(Color.valueOf("ffa665"))));

                image.draw(cell, image.width / 2 - cell.width / 2, image.height / 2 - cell.height / 2);

                for(Weapon weapon : type.weapons){
                    weapon.load();

                    image.draw(weapon.region,
                    (int)(weapon.x / Draw.scl + image.width / 2f - weapon.region.width / 2f),
                    (int)(-weapon.y / Draw.scl + image.height / 2f - weapon.region.height / 2f),
                    weapon.flipSprite, false);
                }

                image.save("unit-" + type.name + "-full");

                Rand rand = new Rand();
                rand.setSeed(type.name.hashCode());

                //generate random wrecks

                int splits = 3;
                float degrees = rand.random(360f);
                float offsetRange = Math.max(image.width, image.height) * 0.15f;
                Vec2 offset = new Vec2(1, 1).rotate(rand.random(360f)).setLength(rand.random(0, offsetRange)).add(image.width/2f, image.height/2f);

                Image[] wrecks = new Image[splits];
                for(int i = 0; i < wrecks.length; i++){
                    wrecks[i] = new Image(image.width, image.height);
                }

                RidgedPerlin r = new RidgedPerlin(1, 3);
                VoronoiNoise vn = new VoronoiNoise(type.id, true);

                image.each((x, y) -> {
                    //add darker cracks on top
                    boolean rValue = Math.max(r.getValue(x, y, 1f / (20f + image.width/8f)), 0) > 0.16f;
                    //cut out random chunks with voronoi
                    boolean vval = vn.noise(x, y, 1f / (14f + image.width/40f)) > 0.47;

                    float dst =  offset.dst(x, y);
                    //distort edges with random noise
                    float noise = (float)Noise.rawNoise(dst / (9f + image.width/70f)) * (60 + image.width/30f);
                    int section = (int)Mathf.clamp(Mathf.mod(offset.angleTo(x, y) + noise + degrees, 360f) / 360f * splits, 0, splits - 1);
                    if(!vval) wrecks[section].draw(x, y, image.getColor(x, y).mul(rValue ? 0.7f : 1f));
                });

                for(int i = 0; i < wrecks.length; i++){
                    wrecks[i].save(type.name + "-wreck" + i);
                }

                for(Cicon icon : Cicon.scaled){
                    Vec2 size = Scaling.fit.apply(image.width, image.height, icon.size, icon.size);
                    Image scaled = new Image((int)size.x, (int)size.y);

                    scaled.drawScaled(image);
                    scaled.save("../ui/unit-" + type.name + "-" + icon.name());
                }

            }catch(IllegalArgumentException e){
                Log.err("WARNING: Skipping unit @: @", type.name, e.getMessage());
            }

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
