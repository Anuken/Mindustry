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

    private static float fluid(boolean gas, double x, double y, float frame){
        int keyframes = gas ? 4 : 3;

        //interpolate between the current two keyframes
        int curFrame = (int)(frame * keyframes);
        int nextFrame = (curFrame + 1) % keyframes;
        float progress = (frame * keyframes) % 1f;

        if(gas){
            float min = 0.56f;
            float interpolated = Mathf.lerp((float)gasFrame(x, y, curFrame), (float)gasFrame(x, y, nextFrame), progress);
            return min + (1f - min) * interpolated;
        }else{ //liquids
            float min = 0.84f;
            double rx = (x + frame*32) % 32, ry = (y + frame*32) % 32;
            //rx = x; ry = y;
            //(float)liquidFrame(rx, ry, 0)
            float interpolated = (float)liquidFrame(rx, ry, 2);//Mathf.lerp((float)liquidFrame(rx, ry, curFrame), (float)liquidFrame(rx, ry, nextFrame), progress);
            //only two colors here
            return min + (interpolated >= 0.3f ? 1f - min : 0f);
        }
    }

    private static double gasFrame(double x, double y, int frame){
        int s = 31;
        //calculate random space offsets for the frame cutout
        double ox = Mathf.randomSeed(frame, 200_000), oy = Mathf.randomSeed(frame, 200_000);
        double scale = 21, second = 0.3;
        return (Simplex.rawTiled(x, y, ox, oy, s, s, scale) + Simplex.rawTiled(x, y, ox, oy, s, s, scale / 1.5) * second) / (1.0 + second);
    }

    private static double liquidFrame(double x, double y, int frame){
        int s = 31;
        //calculate random space offsets for the frame cutout
        double ox = Mathf.randomSeed(frame, 1), oy = Mathf.randomSeed(frame, 1);
        double scale = 26, second = 0.5;
        return (Simplex.rawTiled(x, y, ox, oy, s, s, scale) + Simplex.rawTiled(x, y, ox, oy, s, s, scale / 1.5) * second) / (1.0 + second);
    }

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

                for(int x = 0; x < pixmap.width; x++){
                    for(int y = 0; y < pixmap.height; y++){
                        float dst = Mathf.dst(x, y, size/2f, size/2f);
                        if(Math.abs(dst - radius) <= stroke){
                            pixmap.set(x, y, Color.white);
                        }
                    }
                }

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

        generate("gas-frames", () -> {
            int frames = Liquid.animationFrames;
            String[] stencils = {"fluid"};
            String[] types = {"liquid", "gas"};
            int typeIndex = 0;

            for(String type : types){
                boolean gas = typeIndex++ == 1;
                for(String region : stencils){
                    Pixmap base = get(region);

                    for(int i = 0; i < frames; i++){
                        float frame = i / (float)frames;

                        Pixmap copy = base.copy();
                        for(int x = 0; x < copy.width; x++){
                            for(int y = 0; y < copy.height; y++){
                                if(copy.getA(x, y) > 128){
                                    copy.setRaw(x, y, Color.rgba8888(1f, 1f, 1f, fluid(gas, x, y, frame)));
                                }
                            }
                        }
                        save(copy, region + "-" + type + "-" + i);
                    }
                }
            }
        });

        generate("cliffs", () -> {
            ExecutorService exec = Executors.newFixedThreadPool(OS.cores);
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

                    Fi fi = Fi.get("../blocks/environment/cliffmask" + (val & 0xff) + ".png");
                    fi.writePng(result);
                    fi.copyTo(Fi.get("../editor").child("editor-" + fi.name()));
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

                    Fi.get("../rubble/cracks-" + size + "-" + i + ".png").writePng(output);
                }
            }
        });

        generate("block-icons", () -> {
            Pixmap colors = new Pixmap(content.blocks().size, 1);

            for(Block block : content.blocks()){
                if(block.isAir() || block instanceof ConstructBlock || block instanceof OreBlock || block instanceof LegacyBlock) continue;

                Seq<TextureRegion> toOutline = new Seq<>();
                block.getRegionsToOutline(toOutline);

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
                                out.setRaw(x, y, index == -1 ? teamr.getRaw(x, y) : team.palettei[index]);
                            });
                            save(out, block.name + "-team-" + team.name);

                            if(team == Team.sharded){
                                shardTeamTop = out;
                            }
                        }
                    }
                }

                for(TextureRegion region : toOutline){
                    Pixmap pix = get(region).outline(block.outlineColor, block.outlineRadius);
                    save(pix, ((GenRegion)region).name + "-outline");
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

                        save(out, region.name);
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

                    if(block instanceof Floor && !((Floor)block).wallOre){
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
                    Pixmap container = new Pixmap(tint.width + 6, tint.height + 6);
                    container.draw(base, 3, 3, true);
                    base = container.outline(Pal.gray, 3);
                }

                saveScaled(base, item.name + "-icon-logic", logicIconSize);
                save(base, "../ui/" + item.getContentType().name() + "-" + item.name + "-ui");
            }
        });

        generate("team-icons", () -> {
            for(Team team : Team.all){
                if(has("team-" + team.name)){
                    int rgba = team == Team.derelict ? Color.valueOf("b7b8c9").rgba() : team.color.rgba();
                    Pixmap base = get("team-" + team.name);
                    base.each((x, y) -> base.setRaw(x, y, Color.muli(base.getRaw(x, y), rgba)));

                    delete("team-" + team.name);
                    save(base.outline(Pal.gray, 3), "../ui/team-" + team.name);
                }
            }
        });

        MultiPacker packer = new MultiPacker(){
            @Override
            public void add(PageType type, String name, PixmapRegion region, int[] splits, int[] pads){
                String prefix = type == PageType.main ? "" : "../" + type.name() + "/";
                Log.info("@ | @x@", prefix + name, region.width, region.height);
                //save(region.pixmap, prefix + name);
            }
        };

        //TODO !!!!! currently just an experiment

        if(false)
        generate("all-icons", () -> {
            for(Seq<Content> arr : content.getContentMap()){
                for(Content cont : arr){
                    if(cont instanceof UnlockableContent && !(cont instanceof Planet)){
                        UnlockableContent unlock = (UnlockableContent)cont;

                        if(unlock.generateIcons){
                            try{
                                unlock.createIcons(packer);
                            }catch(IllegalArgumentException e){
                                Log.err(e);
                                Log.err("Skip: @", unlock.name);
                            }
                        }
                    }
                }
            }
        });

        generate("unit-icons", () -> content.units().each(type -> {
            if(type.internal) return; //internal hidden units don't generate

            ObjectSet<String> outlined = new ObjectSet<>();

            try{
                Unit sample = type.constructor.get();

                Func<Pixmap, Pixmap> outline = i -> i.outline(type.outlineColor, 3);
                Cons<TextureRegion> outliner = t -> {
                    if(t != null && t.found()){
                        replace(t, outline.get(get(t)));
                    }
                };

                Seq<TextureRegion> toOutline = new Seq<>();
                type.getRegionsToOutline(toOutline);

                for(TextureRegion region : toOutline){
                    Pixmap pix = get(region).outline(type.outlineColor, type.outlineRadius);
                    save(pix, ((GenRegion)region).name + "-outline");
                }

                Seq<Weapon> weapons = type.weapons;
                weapons.each(Weapon::load);
                weapons.removeAll(w -> !w.region.found());

                for(Weapon weapon : weapons){
                    if(outlined.add(weapon.name) && has(weapon.name)){
                        //only non-top weapons need separate outline sprites (this is mostly just mechs)
                        if(!weapon.top || weapon.parts.contains(p -> p.under)){
                            save(outline.get(get(weapon.name)), weapon.name + "-outline");
                        }else{
                            //replace weapon with outlined version, no use keeping standard around
                            outliner.get(weapon.region);
                        }
                    }
                }

                //generate tank animation
                if(sample instanceof Tankc){
                    Pixmap pix = get(type.treadRegion);

                    for(int r = 0; r < type.treadRects.length; r++){
                        Rect treadRect = type.treadRects[r];
                        //slice is always 1 pixel wide
                        Pixmap slice = pix.crop((int)(treadRect.x + pix.width/2f), (int)(treadRect.y + pix.height/2f), 1, (int)treadRect.height);
                        int frames = type.treadFrames;
                        for(int i = 0; i < frames; i++){
                            int pullOffset = type.treadPullOffset;
                            Pixmap frame = new Pixmap(slice.width, slice.height);
                            for(int y = 0; y < slice.height; y++){
                                int idx = y + i;
                                if(idx >= slice.height){
                                    idx -= slice.height;
                                    idx += pullOffset;
                                    idx = Mathf.mod(idx, slice.height);
                                }

                                frame.setRaw(0, y, slice.getRaw(0, idx));
                            }
                            save(frame, type.name + "-treads" + r + "-" + i);
                        }
                    }
                }

                outliner.get(type.jointRegion);
                outliner.get(type.footRegion);
                outliner.get(type.legBaseRegion);
                outliner.get(type.baseJointRegion);
                if(sample instanceof Legsc) outliner.get(type.legRegion);
                if(sample instanceof Tankc) outliner.get(type.treadRegion);

                Pixmap image = type.segments > 0 ? get(type.segmentRegions[0]) : outline.get(get(type.previewRegion));

                Func<Weapon, Pixmap> weaponRegion = weapon -> Core.atlas.has(weapon.name + "-preview") ? get(weapon.name + "-preview") : get(weapon.region);
                Cons2<Weapon, Pixmap> drawWeapon = (weapon, pixmap) ->
                image.draw(weapon.flipSprite ? pixmap.flipX() : pixmap,
                (int)(weapon.x / Draw.scl + image.width / 2f - weapon.region.width / 2f),
                (int)(-weapon.y / Draw.scl + image.height / 2f - weapon.region.height / 2f),
                true
                );

                boolean anyUnder = false;

                //draw each extra segment on top before it is saved as outline
                if(sample instanceof Crawlc){
                    for(int i = 0; i < type.segments; i++){
                        //replace(type.segmentRegions[i], outline.get(get(type.segmentRegions[i])));
                        save(outline.get(get(type.segmentRegions[i])), type.name + "-segment-outline" + i);

                        if(i > 0){
                            drawCenter(image, get(type.segmentRegions[i]));
                        }
                    }
                    save(image, type.name);
                }

                //outline is currently never needed, although it could theoretically be necessary
                if(type.needsBodyOutline()){
                    save(image, type.name + "-outline");
                }else if(type.segments == 0){
                    replace(type.name, type.segments > 0 ? get(type.segmentRegions[0]) : outline.get(get(type.region)));
                }

                //draw weapons that are under the base
                for(Weapon weapon : weapons.select(w -> w.layerOffset < 0)){
                    drawWeapon.get(weapon, outline.get(weaponRegion.get(weapon)));
                    anyUnder = true;
                }

                //draw over the weapons under the image
                if(anyUnder){
                    image.draw(outline.get(get(type.previewRegion)), true);
                }

                //draw treads
                if(sample instanceof Tankc){
                    Pixmap treads = outline.get(get(type.treadRegion));
                    image.draw(treads, image.width / 2 - treads.width / 2, image.height / 2 - treads.height / 2, true);
                    image.draw(get(type.previewRegion), true);
                }

                //draw mech parts
                if(sample instanceof Mechc){
                    drawCenter(image, get(type.baseRegion));
                    drawCenter(image, get(type.legRegion));
                    drawCenter(image, get(type.legRegion).flipX());
                    image.draw(get(type.previewRegion), true);
                }

                //draw weapon outlines on base
                for(Weapon weapon : weapons){
                    //skip weapons under unit
                    if(weapon.layerOffset < 0) continue;

                    drawWeapon.get(weapon, outline.get(weaponRegion.get(weapon)));
                }

                //draw base region on top to mask weapons
                if(type.drawCell) image.draw(get(type.previewRegion), true);

                if(type.drawCell){
                    Pixmap baseCell = get(type.cellRegion);
                    Pixmap cell = baseCell.copy();

                    //replace with 0xffd37fff : 0xdca463ff for sharded colors?
                    cell.replace(in -> in == 0xffffffff ? 0xffa664ff : in == 0xdcc6c6ff || in == 0xdcc5c5ff ? 0xd06b53ff : 0);

                    image.draw(cell, image.width / 2 - cell.width / 2, image.height / 2 - cell.height / 2, true);
                }

                for(Weapon weapon : weapons){
                    //skip weapons under unit
                    if(weapon.layerOffset < 0) continue;

                    Pixmap reg = weaponRegion.get(weapon);
                    Pixmap wepReg = weapon.top ? outline.get(reg) : reg;

                    drawWeapon.get(weapon, wepReg);

                    if(weapon.cellRegion.found()){
                        Pixmap weaponCell = get(weapon.cellRegion);
                        weaponCell.replace(in -> in == 0xffffffff ? 0xffa664ff : in == 0xdcc6c6ff || in == 0xdcc5c5ff ? 0xd06b53ff : 0);
                        drawWeapon.get(weapon, weaponCell);
                    }
                }

                //TODO I can save a LOT of space by not creating a full icon.
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
                int shadowColor = Color.rgba8888(0, 0, 0, 0.3f);

                for(int i = 0; i < ore.variants; i++){
                    //get base image to draw on
                    Pixmap base = get(ore.variantRegions[i]);
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

                    replace(ore.variantRegions[i], image);

                    save(image, "../blocks/environment/" + ore.name + (i + 1));
                    save(image, "../editor/editor-" + ore.name + (i + 1));

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

    /** Generates a scorch pixmap based on parameters. Thread safe. */
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
