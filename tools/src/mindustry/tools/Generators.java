package mindustry.tools;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.game.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;
import static mindustry.tools.ImagePacker.*;

public class Generators{
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
            float interpolated = (float)liquidFrame(rx, ry, 2);
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
        FilePackContext ctx = new FilePackContext();

        //scorches
        for(int bsize = 0; bsize < 10; bsize++){
            int size = bsize;
            Core.executor.execute(() -> {
                for(int i = 0; i < 3; i++){
                    Rand rand = new Rand();
                    ScorchGenerator gen = new ScorchGenerator();
                    double multiplier = 30;
                    double ss = size * multiplier / 20.0;

                    gen.seed = rand.random(100000);
                    gen.size += size*multiplier;
                    gen.scale = gen.size / 80f * 18f;
                    //gen.nscl -= size * 0.2f;
                    gen.octaves += ss/3.0;
                    gen.pers += ss/10.0/5.0;

                    gen.scale += rand.range(3f);
                    gen.scale -= ss*2f;
                    gen.nscl -= rand.random(1f);

                    Pixmap out = gen.generate();
                    Pixmap median = Pixmaps.median(out, 2, 0.75, new IntSeq());
                    Fi.get("../rubble/scorch-" + size + "-" + i + ".png").writePng(median);
                    out.dispose();
                    median.dispose();
                }
            });
        }

        //autotiles
        for(Block block : content.blocks().select(b -> (b.isFloor() && b.asFloor().autotile) || (b instanceof StaticWall && ((StaticWall)b).autotile))){
            int variants = block instanceof Floor f && f.autotileVariants > 1 ? f.autotileVariants : 1;
            for(int v = 0; v < variants; v++){
                Fi basePath = new Fi("../../../assets-raw/sprites_out/blocks/environment/" + block.name + "-autotile" + (variants <= 1 ? "" : "" + (v+1)) + ".png"), iconPath = basePath.parent().child(block.name + ".png");

                if(basePath.exists()){
                    int variant = v;
                    //theoretically this might not finish in time, but I doubt that will ever happen
                    mainExecutor.execute(() -> {
                        try{
                            ImageTileGenerator.generate(basePath, block.name + (variants <= 1 ? "" : "-" + (variant+1)), new Fi("../../../assets-raw/sprites_out/blocks/environment"));
                        }catch(Throwable e){
                            Log.err("Failed to autotile: " + block.name, e);
                        }finally{
                            //the raw autotile source image must never be included, it isn't useful
                            basePath.delete();
                        }
                    });

                    if(v == 0){
                        //save the bottom right region as the "main" sprite for previews
                        Pixmap out = new Pixmap(basePath);
                        Pixmap cropped = out.crop(32, 32, 32, 32);
                        boolean isFallback = !iconPath.exists();
                        if(isFallback){
                            iconPath.writePng(cropped);
                            //the static atlas cache predates this run, so packSprites() can't see this new file unless we seed it directly
                            ctx.seed(block.name, cropped);
                        }
                        out.dispose();
                        gens.put(block, cropped);
                    }
                }else{
                    Log.warn("Autotile block '@' not found: @", block.name, basePath.absolutePath());
                }
            }
        }

        //splashes
        {
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
        }

        //bubbles
        {
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
        }

        //gas frames
        {
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
        }

        //cliffs
        {
            int size = 64;
            int dark = new Color(0.5f, 0.5f, 0.6f, 1f).mul(0.98f).rgba();
            int mid = Color.lightGray.rgba();

            Pixmap[] images = new Pixmap[8];
            for(int i = 0; i < 8; i++){
                images[i] = new Pixmap(((GenRegion)Core.atlas.find("cliff" + i)).path);
            }

            for(int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++){
                int bi = i;
                Core.executor.execute(() -> {
                    Color color = new Color();
                    Pixmap result = new Pixmap(size, size);
                    byte[][] mask = new byte[size][size];

                    int val = (bi & 0xff) == 128 ? 128 : (byte)bi;
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
                });
            }
        }

        //cracks
        Core.executor.execute(() -> {
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

                    new Fi("../rubble/cracks-" + size + "-" + i + ".png").writePng(output);
                }
            }
        });

        //content sprites
        Pixmap colors = new Pixmap(content.blocks().size, 1);

        for(Seq<Content> arr : content.getContentMap()){
            for(var content: arr){
                if(content instanceof UnlockableContent u && u.packSprites){
                    try{
                        u.packSprites(ctx);

                        if(u instanceof Block block){
                            TextureRegion[] regions = block.getGeneratedIcons();

                            PixmapRegion image =
                            ctx.has("block-" + block.name + "-full") ? ctx.get("block-" + block.name + "-full") :
                            regions.length > 0 && regions[0].found() ? ctx.get(regions[0]) :
                            ctx.has(block.name + "1") ? ctx.get(block.name + "1") :
                            gens.containsKey(block) ? new PixmapRegion(gens.get(block)) :
                            null;

                            if(image == null){
                                Log.warn("No icon for '@', skipping block color.", block.name);
                                continue;
                            }

                            int width = image.width;
                            int height = image.height;

                            boolean hasEmpty = false;
                            Color average = new Color(), c = new Color();
                            float asum = 0f;
                            for(int x = 0; x < width; x++){
                                for(int y = 0; y < height; y++){
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
                        }
                    }catch(Throwable e){
                        Log.err("Failed to pack sprites for: " + u.name, e);
                    }
                }
            }
        }

        save(colors, "../../../assets/sprites/block_colors");

        //vanilla-only: random wreck debris
        content.units().each(type -> {
            if(!type.packSprites) return;

            String fullName = "unit-" + type.name + "-full";
            //fall back to the plain body region for the (currently unused) case of generateFullIcon == false
            if(!ctx.has(fullName) && !ctx.has(type.name)) return;

            Core.executor.execute(() -> {
                Pixmap image = ctx.get(ctx.has(fullName) ? fullName : type.name).crop();
                Rand rand = new Rand();
                rand.setSeed(type.name.hashCode());

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
                image.dispose();
            });
        });

        //team icons
        for(Team team : Team.all){
            if(has("team-" + team.name)){
                int rgba = team == Team.derelict ? Color.valueOf("b7b8c9").rgba() : team.color.rgba();
                Pixmap base = get("team-" + team.name);
                base.each((x, y) -> base.setRaw(x, y, Color.muli(base.getRaw(x, y), rgba)));

                delete("team-" + team.name);
                save(base.outline(Pal.gray, 3), "../ui/team-" + team.name);
            }
        }

        Threads.await(Core.executor);

        ctx.dispose();
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