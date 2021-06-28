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
import arc.util.async.*;
import arc.util.noise.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.legacy.*;
import mindustry.world.meta.*;

import java.util.concurrent.*;

import static mindustry.Vars.*;
import static mindustry.tools.ImagePacker.*;

public class Generators{
    static final int logicIconSize = (int)iconMed, maxUiIcon = 128;

    public static void run(){
        ObjectMap<Block, Pixmap> gens = new ObjectMap<>();

        generate("splashes", () -> {

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
                        pixmap.set(x, y, Color.white);
                    }
                });

                Fi.get("splash-" + i + ".png").writePng(pixmap);

                pixmap.dispose();
            }
        });

        generate("bubbles", () -> {

            int frames = 16;
            int size = 40;
            for(int i = 0; i < frames; i++){
                float fin = (float)i / (frames);
                float fout = 1f - fin;
                float stroke = 3.5f * fout;
                float radius = (size/2f) * fin;
                float shinelen = radius / 2.5f, shinerad = stroke*1.5f + 0.3f;
                float shinex = size/2f + shinelen / Mathf.sqrt2, shiney = size/2f - shinelen / Mathf.sqrt2;

                Pixmap pixmap = new Pixmap(size, size);

                pixmap.each((x, y) -> {
                    float dst = Mathf.dst(x, y, size/2f, size/2f);
                    if(Math.abs(dst - radius) <= stroke || Mathf.within(x, y, shinex, shiney, shinerad)){
                        pixmap.set(x, y, Color.white);
                    }
                });

                Fi.get("bubble-" + i + ".png").writePng(pixmap);

                pixmap.dispose();
            }
        });

        generate("cliffs", () -> {
            ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            int size = 64;
            int dark = new Color(0.5f, 0.5f, 0.6f, 1f).mul(0.98f).rgba();
            int mid = Color.lightGray.rgba();

            Pixmap[] images = new Pixmap[8];
            for(int i = 0; i < 8; i++){
                images[i] = new Pixmap(((GenRegion)Core.atlas.find("cliff" + i)).path);
            }

            for(int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++){
                int bi = i;
                exec.execute(() -> {
                    Color color = new Color();
                    Pixmap result = new Pixmap(size, size);
                    byte[][] mask = new byte[size][size];

                    byte val = (byte)bi;
                    //check each bit/direction
                    for(int j = 0; j < 8; j++){
                        if((val & (1 << j)) != 0){
                            if(j % 2 == 1 && (((val & (1 << (j + 1))) != 0) != ((val & (1 << (j - 1))) != 0))){
                                continue;
                            }

                            Pixmap image = images[j];
                            image.each((x, y) -> {
                                color.set(image.getRaw(x, y));
                                if(color.a > 0.1){
                                    //white -> bit 1 -> top
                                    //black -> bit 2 -> bottom
                                    mask[x][y] |= (color.r > 0.5f ? 1 : 2);
                                }
                            });
                        }
                    }

                    result.each((x, y) -> {
                        byte m = mask[x][y];
                        if(m != 0){
                            //mid
                            if(m == 3){
                                //find nearest non-mid color
                                byte best = 0;
                                float bestDst = 0;
                                boolean found = false;
                                //expand search range until found
                                for(int rad = 9; rad < 64; rad += 7){
                                    for(int cx = Math.max(x - rad, 0); cx <= Math.min(x + rad, size - 1); cx++){
                                        for(int cy = Math.max(y - rad, 0); cy <= Math.min(y + rad, size - 1); cy++){
                                            byte nval = mask[cx][cy];
                                            if(nval == 1 || nval == 2){
                                                float dst2 = Mathf.dst2(cx, cy, x, y);
                                                if(dst2 <= rad * rad && (!found || dst2 < bestDst)){
                                                    best = nval;
                                                    bestDst = dst2;
                                                    found = true;
                                                }
                                            }
                                        }
                                    }
                                }

                                if(found){
                                    m = best;
                                }
                            }

                            result.setRaw(x, y, m == 1 ? Color.whiteRgba : m == 2 ? dark : mid);
                        }
                    });

                    Fi.get("../blocks/environment/cliffmask" + (val & 0xff) + ".png").writePng(result);
                });
            }

            Threads.await(exec);
        });

        generate("cracks", () -> {
            for(int size = 1; size <= BlockRenderer.maxCrackSize; size++){
                int dim = size * 32;
                int steps = BlockRenderer.crackRegions;
                for(int i = 0; i < steps; i++){
                    float fract = i / (float)steps;

                    Pixmap image = new Pixmap(dim, dim);
                    for(int x = 0; x < dim; x++){
                        for(int y = 0; y < dim; y++){
                            float dst = Mathf.dst((float)x/dim, (float)y/dim, 0.5f, 0.5f) * 2f;
                            if(dst < 1.2f && Ridged.noise2d(1, x, y, 3, 1f / 40f) - dst*(1f-fract) > 0.16f){
                                image.setRaw(x, y, Color.whiteRgba);
                            }
                        }
                    }

                    Pixmap output = new Pixmap(image.width, image.height);
                    int rad = 3;

                    //median filter
                    for(int x = 0; x < output.width; x++){
                        for(int y = 0; y < output.height; y++){
                            int whites = 0, clears = 0;
                            for(int cx = -rad; cx < rad; cx++){
                                for(int cy = -rad; cy < rad; cy++){
                                    int wx = Mathf.clamp(cx + x, 0, output.width - 1), wy = Mathf.clamp(cy + y, 0, output.height - 1);
                                    int color = image.getRaw(wx, wy);
                                    if((color & 0xff) > 127){
                                        whites ++;
                                    }else{
                                        clears ++;
                                    }
                                }
                            }
                            output.setRaw(x, y, whites >= clears ? Color.whiteRgba : Color.clearRgba);
                        }
                    }

                    Fi.get("cracks-" + size + "-" + i + ".png").writePng(output);
                }
            }
        });

        generate("block-icons", () -> {
            Pixmap colors = new Pixmap(content.blocks().size, 1);

            for(Block block : content.blocks()){
                if(block.isAir() || block instanceof ConstructBlock || block instanceof OreBlock || block instanceof LegacyBlock) continue;

                block.load();
                block.loadIcon();

                TextureRegion[] regions = block.getGeneratedIcons();

                if(block.variants > 0 || block instanceof Floor){
                    for(TextureRegion region : block.variantRegions()){
                        GenRegion gen = (GenRegion)region;
                        if(gen.path == null) continue;
                        gen.path.copyTo(Fi.get("../editor/editor-" + gen.path.name()));
                    }
                }

                for(TextureRegion region : block.makeIconRegions()){
                    GenRegion gen = (GenRegion)region;
                    save(get(region).outline(block.outlineColor, block.outlineRadius), gen.name + "-outline");
                }

                Pixmap shardTeamTop = null;

                if(block.teamRegion.found()){
                    Pixmap teamr = get(block.teamRegion);

                    for(Team team : Team.all){
                        if(team.hasPalette){
                            Pixmap out = new Pixmap(teamr.width, teamr.height);
                            teamr.each((x, y) -> {
                                int color = teamr.getRaw(x, y);
                                int index = color == 0xffffffff ? 0 : color == 0xdcc6c6ff ? 1 : color == 0x9d7f7fff ? 2 : -1;
                                out.setRaw(x, y, index == -1 ? teamr.getRaw(x, y) : team.palette[index].rgba());
                            });
                            save(out, block.name + "-team-" + team.name);

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
                    Pixmap last = null;
                    if(block.outlineIcon){
                        GenRegion region = (GenRegion)regions[block.outlinedIcon >= 0 ? block.outlinedIcon : regions.length -1];
                        Pixmap base = get(region);
                        Pixmap out = last = base.outline(block.outlineColor, block.outlineRadius);

                        //do not run for legacy ones
                        if(block.outlinedIcon >= 0){
                            //prevents the regions above from being ignored/invisible/etc
                            for(int i = block.outlinedIcon + 1; i < regions.length; i++){
                                out.draw(get(regions[i]), true);
                            }
                        }

                        region.path.delete();

                        save(out, block.name);
                    }

                    if(!regions[0].found()){
                        continue;
                    }

                    Pixmap image = get(regions[0]);

                    int i = 0;
                    for(TextureRegion region : regions){
                        i++;
                        if(i != regions.length || last == null){
                            image.draw(get(region), true);
                        }else{
                            image.draw(last, true);
                        }

                        //draw shard (default team top) on top of first sprite
                        if(region == block.teamRegions[Team.sharded.id] && shardTeamTop != null){
                            image.draw(shardTeamTop, true);
                        }
                    }

                    if(!(regions.length == 1 && regions[0] == Core.atlas.find(block.name) && shardTeamTop == null)){
                        save(image, "block-" + block.name + "-full");
                    }

                    save(image, "../editor/" + block.name + "-icon-editor");

                    if(block.buildVisibility != BuildVisibility.hidden){
                        saveScaled(image, block.name + "-icon-logic", logicIconSize);
                    }
                    saveScaled(image, "../ui/block-" + block.name + "-ui", Math.min(image.width, maxUiIcon));

                    boolean hasEmpty = false;
                    Color average = new Color(), c = new Color();
                    float asum = 0f;
                    for(int x = 0; x < image.width; x++){
                        for(int y = 0; y < image.height; y++){
                            Color color = c.set(image.get(x, y));
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
                        average.mul(0.77f);
                    }else{
                        average.mul(1.1f);
                    }
                    //encode square sprite in alpha channel
                    average.a = hasEmpty ? 0.1f : 1f;
                    colors.setRaw(block.id, 0, average.rgba());
                }catch(NullPointerException e){
                    Log.err("Block &ly'@'&lr has an null region!", block);
                }
            }

            save(colors, "../../../assets/sprites/block_colors");
        });

        generate("shallows", () -> {
            content.blocks().<ShallowLiquid>each(b -> b instanceof ShallowLiquid, floor -> {
                Pixmap overlay = get(floor.liquidBase.region);
                int index = 0;
                for(TextureRegion region : floor.floorBase.variantRegions()){
                    Pixmap res = get(region).copy();
                    for(int x = 0; x < res.width; x++){
                        for(int y = 0; y < res.height; y++){
                            res.set(x, y, Pixmap.blend((overlay.getRaw(x, y) & 0xffffff00) | (int)(floor.liquidOpacity * 255), res.getRaw(x, y)));
                        }
                    }

                    String name = floor.name + "" + (++index);
                    save(res, "../blocks/environment/" + name);
                    save(res, "../editor/editor-" + name);

                    gens.put(floor, res);
                }
            });
        });

        generate("item-icons", () -> {
            for(UnlockableContent item : Seq.<UnlockableContent>withArrays(content.items(), content.liquids(), content.statusEffects())){
                if(item instanceof StatusEffect && !has(item.getContentType().name() + "-" + item.name)){
                    continue;
                }

                Pixmap base = get(item.getContentType().name() + "-" + item.name);
                //tint status effect icon color
                if(item instanceof StatusEffect){
                    StatusEffect stat = (StatusEffect)item;
                    Pixmap tint = base;
                    base.each((x, y) -> tint.setRaw(x, y, Color.muli(tint.getRaw(x, y), stat.color.rgba())));

                    //outline the image
                    Pixmap container = new Pixmap(38, 38);
                    container.draw(base, 3, 3, true);
                    base = container.outline(Pal.gray, 3);
                }

                saveScaled(base, item.name + "-icon-logic", logicIconSize);
                save(base, "../ui/" + item.getContentType().name() + "-" + item.name + "-ui");
            }
        });

        //TODO broken, freezes
        generate("unit-icons", () -> content.units().each(type -> {
            if(type.isHidden()) return; //hidden units don't generate

            ObjectSet<String> outlined = new ObjectSet<>();

            try{
                type.load();
                type.loadIcon();
                type.init();

                Func<Pixmap, Pixmap> outline = i -> i.outline(Pal.darkerMetal, 3);
                Cons<TextureRegion> outliner = t -> {
                    if(t != null && t.found()){
                        replace(t, outline.get(get(t)));
                    }
                };

                for(Weapon weapon : type.weapons){
                    if(outlined.add(weapon.name) && has(weapon.name)){
                        save(outline.get(get(weapon.name)), weapon.name + "-outline");
                    }
                }

                outliner.get(type.jointRegion);
                outliner.get(type.footRegion);
                outliner.get(type.legBaseRegion);
                outliner.get(type.baseJointRegion);
                if(type.constructor.get() instanceof Legsc) outliner.get(type.legRegion);

                Pixmap image = outline.get(get(type.region));

                save(image, type.name + "-outline");

                //draw mech parts
                if(type.constructor.get() instanceof Mechc){
                    drawCenter(image, get(type.baseRegion));
                    drawCenter(image, get(type.legRegion));
                    drawCenter(image, get(type.legRegion).flipX());
                    image.draw(get(type.region), true);
                }

                //draw outlines
                for(Weapon weapon : type.weapons){
                    weapon.load();

                    image.draw(weapon.flipSprite ? outline.get(get(weapon.region)).flipX() : outline.get(get(weapon.region)),
                    (int)(weapon.x / Draw.scl + image.width / 2f - weapon.region.width / 2f),
                    (int)(-weapon.y / Draw.scl + image.height / 2f - weapon.region.height / 2f),
                    true
                    );
                }

                //draw base region on top to mask weapons
                image.draw(get(type.region), true);
                int baseColor = Color.valueOf("ffa665").rgba();

                Pixmap baseCell = get(type.cellRegion);
                Pixmap cell = new Pixmap(type.cellRegion.width, type.cellRegion.height);
                cell.each((x, y) -> cell.set(x, y, Color.muli(baseCell.getRaw(x, y), baseColor)));

                image.draw(cell, image.width / 2 - cell.width / 2, image.height / 2 - cell.height / 2, true);

                for(Weapon weapon : type.weapons){
                    weapon.load();

                    Pixmap wepReg = weapon.top ? outline.get(get(weapon.region)) : get(weapon.region);
                    if(weapon.flipSprite){
                        wepReg = wepReg.flipX();
                    }

                    image.draw(wepReg,
                    (int)(weapon.x / Draw.scl + image.width / 2f - weapon.region.width / 2f),
                    (int)(-weapon.y / Draw.scl + image.height / 2f - weapon.region.height / 2f),
                    true);
                }

                save(image, "unit-" + type.name + "-full");

                Rand rand = new Rand();
                rand.setSeed(type.name.hashCode());

                //generate random wrecks

                int splits = 3;
                float degrees = rand.random(360f);
                float offsetRange = Math.max(image.width, image.height) * 0.15f;
                Vec2 offset = new Vec2(1, 1).rotate(rand.random(360f)).setLength(rand.random(0, offsetRange)).add(image.width/2f, image.height/2f);

                Pixmap[] wrecks = new Pixmap[splits];
                for(int i = 0; i < wrecks.length; i++){
                    wrecks[i] = new Pixmap(image.width, image.height);
                }

                VoronoiNoise vn = new VoronoiNoise(type.id, true);

                image.each((x, y) -> {
                    //add darker cracks on top
                    boolean rValue = Math.max(Ridged.noise2d(1, x, y, 3, 1f / (20f + image.width/8f)), 0) > 0.16f;
                    //cut out random chunks with voronoi
                    boolean vval = vn.noise(x, y, 1f / (14f + image.width/40f)) > 0.47;

                    float dst =  offset.dst(x, y);
                    //distort edges with random noise
                    float noise = (float)Noise.rawNoise(dst / (9f + image.width/70f)) * (60 + image.width/30f);
                    int section = (int)Mathf.clamp(Mathf.mod(offset.angleTo(x, y) + noise + degrees, 360f) / 360f * splits, 0, splits - 1);
                    if(!vval) wrecks[section].setRaw(x, y, Color.muli(image.getRaw(x, y), rValue ? 0.7f : 1f));
                });

                for(int i = 0; i < wrecks.length; i++){
                    save(wrecks[i], "../rubble/" + type.name + "-wreck" + i);
                }

                int maxd = Math.min(Math.max(image.width, image.height), maxUiIcon);
                Pixmap fit = new Pixmap(maxd, maxd);
                drawScaledFit(fit, image);

                saveScaled(fit, type.name + "-icon-logic", logicIconSize);
                save(fit, "../ui/unit-" + type.name + "-ui");
            }catch(IllegalArgumentException e){
                Log.err("WARNING: Skipping unit @: @", type.name, e.getMessage());
            }

        }));

        generate("ore-icons", () -> {
            content.blocks().<OreBlock>each(b -> b instanceof OreBlock, ore -> {
                String prefix = ore instanceof WallOreBlock ? "wall-ore-" : "ore-";
                Item item = ore.itemDrop;
                int shadowColor = Color.rgba8888(0, 0, 0, 0.3f);

                for(int i = 0; i < ore.variants; i++){
                    //get base image to draw on
                    Pixmap base = get((ore instanceof WallOreBlock ? "wall-" : "") + item.name + (i + 1));
                    Pixmap image = base.copy();

                    int offset = image.width / tilesize - 1;

                    for(int x = 0; x < image.width; x++){
                        for(int y = offset; y < image.height; y++){
                            //draw semi transparent background
                            if(base.getA(x, y - offset) != 0){
                                image.setRaw(x, y, Pixmap.blend(shadowColor, base.getRaw(x, y)));
                            }
                        }
                    }

                    image.draw(base, true);
                    save(image, "../blocks/environment/" + prefix + item.name + (i + 1));
                    save(image, "../editor/editor-" + prefix + item.name + (i + 1));

                    save(image, "block-" + ore.name + "-full");
                    save(image, "../ui/block-" + ore.name + "-ui");
                }
            });
        });

        generate("edges", () -> {
            content.blocks().<Floor>each(b -> b instanceof Floor && !(b instanceof OverlayFloor), floor -> {

                if(has(floor.name + "-edge") || floor.blendGroup != floor){
                    return;
                }

                try{
                    Pixmap image = gens.get(floor, get(floor.getGeneratedIcons()[0]));
                    Pixmap edge = get("edge-stencil");
                    Pixmap result = new Pixmap(edge.width, edge.height);

                    for(int x = 0; x < edge.width; x++){
                        for(int y = 0; y < edge.height; y++){
                            result.set(x, y, Color.muli(edge.getRaw(x, y), image.get(x % image.width, y % image.height)));
                        }
                    }

                    save(result, "../blocks/environment/" + floor.name + "-edge");

                }catch(Exception ignored){}
            });
        });

        generate("scorches", () -> {
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
                    Fi.get("../rubble/scorch-" + size + "-" + i + ".png").writePng(median);
                    out.dispose();
                    median.dispose();
                }
            }
        });
    }

    /** Generates a scorch pixmap based on parameters. Thread safe, unless multiple scorch generators are running in parallel. */
    public static class ScorchGenerator{
        public int size = 80, seed = 0, color = Color.whiteRgba;
        public double scale = 18, pow = 2, octaves = 4, pers = 0.4, add = 2, nscl = 4.5f;

        public Pixmap generate(){
            Pixmap pix = new Pixmap(size, size);

            pix.each((x, y) -> {
                double dst = Mathf.dst(x, y, size/2, size/2) / (size / 2f);
                double scaled = Math.abs(dst - 0.5f) * 5f + add;
                scaled -= noise(Angles.angle(x, y, size/2, size/2))*nscl;
                if(scaled < 1.5f) pix.setRaw(x, y, color);
            });

            return pix;
        }

        private double noise(float angle){
            return Math.pow(Simplex.noise2d(seed, octaves, pers, 1 / scale, Angles.trnsx(angle, size/2f) + size/2f, Angles.trnsy(angle, size/2f) + size/2f), pow);
        }
    }

}
