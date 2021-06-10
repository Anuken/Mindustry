package mindustry.tools;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Log.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.world.blocks.*;

import java.io.*;

public class ImagePacker{
    static ObjectMap<String, PackIndex> cache = new ObjectMap<>();

    public static void main(String[] args) throws Exception{
        Vars.headless = true;
        //makes PNG loading slightly faster
        ArcNativesLoader.load();

        Log.logger = new NoopLogHandler();
        Vars.content = new ContentLoader();
        Vars.content.createBaseContent();
        Log.logger = new DefaultLogHandler();

        Fi.get("../../../assets-raw/sprites_out").walk(path -> {
            if(!path.extEquals("png")) return;

            cache.put(path.nameWithoutExtension(), new PackIndex(path));
        });

        Core.atlas = new TextureAtlas(){
            @Override
            public AtlasRegion find(String name){
                if(!cache.containsKey(name)){
                    GenRegion region = new GenRegion(name, null);
                    region.invalid = true;
                    return region;
                }

                PackIndex index = cache.get(name);
                if(index.pixmap == null){
                    index.pixmap = new Pixmap(index.file);
                    index.region = new GenRegion(name, index.file){{
                        width = index.pixmap.width;
                        height = index.pixmap.height;
                        u2 = v2 = 1f;
                        u = v = 0f;
                    }};
                }
                return index.region;
            }

            @Override
            public AtlasRegion find(String name, TextureRegion def){
                if(!cache.containsKey(name)){
                    return (AtlasRegion)def;
                }
                return find(name);
            }

            @Override
            public AtlasRegion find(String name, String def){
                if(!cache.containsKey(name)){
                    return find(def);
                }
                return find(name);
            }

            @Override
            public boolean has(String s){
                return cache.containsKey(s);
            }
        };

        Draw.scl = 1f / Core.atlas.find("scale_marker").width;

        Time.mark();
        Generators.run();
        Log.info("&ly[Generator]&lc Total time to generate: &lg@&lcms", Time.elapsed());
        //Log.info("&ly[Generator]&lc Total images created: &lg@", Image.total());

        //format:
        //character-ID=contentname:texture-name
        Fi iconfile = Fi.get("../../../assets/icons/icons.properties");
        OrderedMap<String, String> map = new OrderedMap<>();
        PropertiesUtils.load(map, iconfile.reader(256));

        ObjectMap<String, String> content2id = new ObjectMap<>();
        map.each((key, val) -> content2id.put(val.split("\\|")[0], key));

        Seq<UnlockableContent> cont = Seq.withArrays(Vars.content.blocks(), Vars.content.items(), Vars.content.liquids(), Vars.content.units(), Vars.content.statusEffects());
        cont.removeAll(u -> u instanceof ConstructBlock || u == Blocks.air);

        int minid = 0xF8FF;
        for(String key : map.keys()){
            minid = Math.min(Integer.parseInt(key) - 1, minid);
        }

        for(UnlockableContent c : cont){
            if(!content2id.containsKey(c.name)){
                map.put(minid + "", c.name + "|" + texname(c));
                minid --;
            }
        }

        Writer writer = iconfile.writer(false);
        for(String key : map.keys()){
            writer.write(key + "=" + map.get(key) + "\n");
        }

        writer.close();
    }

    static String texname(UnlockableContent c){
        return c.getContentType() + "-" + c.name + "-ui";
    }

    static void generate(String name, Runnable run){
        Time.mark();
        run.run();
        Log.info("&ly[Generator]&lc Time to generate &lm@&lc: &lg@&lcms", name, Time.elapsed());
    }

    static Pixmap get(String name){
        return get(Core.atlas.find(name));
    }

    static boolean has(String name){
        return Core.atlas.has(name);
    }

    static Pixmap get(TextureRegion region){
        validate(region);

        return cache.get(((AtlasRegion)region).name).pixmap.copy();
    }

    static void save(Pixmap pix, String path){
        Fi.get(path + ".png").writePng(pix);
    }

    static void drawCenter(Pixmap pix, Pixmap other){
        pix.draw(other, pix.width/2 - other.width/2, pix.height/2 - other.height/2, true);
    }

    static void saveScaled(Pixmap pix, String name, int size){
        Pixmap scaled = new Pixmap(size, size);
        //TODO bad linear scaling
        scaled.draw(pix, 0, 0, pix.width, pix.height, 0, 0, size, size, true, true);
        save(scaled, name);
    }

    static void drawScaledFit(Pixmap base, Pixmap image){
        Vec2 size = Scaling.fit.apply(image.width, image.height, base.width, base.height);
        int wx = (int)size.x, wy = (int)size.y;
        //TODO bad linear scaling
        base.draw(image, 0, 0, image.width, image.height, base.width/2 - wx/2, base.height/2 - wy/2, wx, wy, true, true);
    }

    static void replace(String name, Pixmap image){
        Fi.get(name + ".png").writePng(image);
        ((GenRegion)Core.atlas.find(name)).path.delete();
    }

    static void replace(TextureRegion region, Pixmap image){
        replace(((GenRegion)region).name, image);
    }

    static void err(String message, Object... args){
        throw new IllegalArgumentException(Strings.format(message, args));
    }

    static void validate(TextureRegion region){
        if(((GenRegion)region).invalid){
            ImagePacker.err("Region does not exist: @", ((GenRegion)region).name);
        }
    }

    static class GenRegion extends AtlasRegion{
        boolean invalid;
        Fi path;

        GenRegion(String name, Fi path){
            if(name == null) throw new IllegalArgumentException("name is null");
            this.name = name;
            this.path = path;
        }

        @Override
        public boolean found(){
            return !invalid;
        }
    }

    static class PackIndex{
        @Nullable AtlasRegion region;
        @Nullable Pixmap pixmap;
        Fi file;

        public PackIndex(Fi file){
            this.file = file;
        }
    }
}
