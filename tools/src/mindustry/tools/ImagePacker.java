package mindustry.tools;

import arc.*;
import arc.files.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Log.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;

public class ImagePacker{
    static ObjectMap<String, TextureRegion> regionCache = new ObjectMap<>();
    static ObjectMap<String, BufferedImage> imageCache = new ObjectMap<>();

    public static void main(String[] args) throws Exception{
        Vars.headless = true;
        ArcNativesLoader.load();

        Log.setLogger(new NoopLogHandler());
        Vars.content = new ContentLoader();
        Vars.content.createBaseContent();
        Log.setLogger(new DefaultLogHandler());

        Fi.get("../../../assets-raw/sprites_out").walk(path -> {
            if(!path.extEquals("png")) return;

            String fname = path.nameWithoutExtension();

            try{
                BufferedImage image = ImageIO.read(path.file());

                if(image == null) throw new IOException("image " + path.absolutePath() + " is null for terrible reasons");
                GenRegion region = new GenRegion(fname, path){

                    @Override
                    public int getX(){
                        return 0;
                    }

                    @Override
                    public int getY(){
                        return 0;
                    }

                    @Override
                    public int getWidth(){
                        return image.getWidth();
                    }

                    @Override
                    public int getHeight(){
                        return image.getHeight();
                    }
                };

                regionCache.put(fname, region);
                imageCache.put(fname, image);
            }catch(IOException e){
                throw new RuntimeException(e);
            }
        });

        Core.atlas = new TextureAtlas(){
            @Override
            public AtlasRegion find(String name){
                if(!regionCache.containsKey(name)){
                    GenRegion region = new GenRegion(name, null);
                    region.invalid = true;
                    return region;
                }
                return (AtlasRegion)regionCache.get(name);
            }

            @Override
            public AtlasRegion find(String name, TextureRegion def){
                if(!regionCache.containsKey(name)){
                    return (AtlasRegion)def;
                }
                return (AtlasRegion)regionCache.get(name);
            }

            @Override
            public AtlasRegion find(String name, String def){
                if(!regionCache.containsKey(name)){
                    return (AtlasRegion)regionCache.get(def);
                }
                return (AtlasRegion)regionCache.get(name);
            }

            @Override
            public boolean has(String s){
                return regionCache.containsKey(s);
            }
        };

        Draw.scl = 1f / Core.atlas.find("scale_marker").getWidth();

        Time.mark();
        Generators.generate();
        Log.info("&ly[Generator]&lc Total time to generate: &lg@&lcms", Time.elapsed());
        Log.info("&ly[Generator]&lc Total images created: &lg@", Image.total());
        Image.dispose();

        //format:
        //character-ID=contentname:texture-name
        Fi iconfile = Fi.get("../../../assets/icons/icons.properties");
        OrderedMap<String, String> map = new OrderedMap<>();
        PropertiesUtils.load(map, iconfile.reader(256));

        ObjectMap<String, String> content2id = new ObjectMap<>();
        map.each((key, val) -> content2id.put(val.split("\\|")[0], key));

        Seq<UnlockableContent> cont = Seq.withArrays(Vars.content.blocks(), Vars.content.items(), Vars.content.liquids(), Vars.content.units());
        cont.removeAll(u -> u instanceof BuildBlock || u == Blocks.air);

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
        if(c instanceof Block) return "block-" + c.name + "-medium";
        if(c instanceof UnitType) return "unit-" + c.name + "-medium";
        return c.getContentType() + "-" + c.name + "-icon";
    }

    static void generate(String name, Runnable run){
        Time.mark();
        run.run();
        Log.info("&ly[Generator]&lc Time to generate &lm@&lc: &lg@&lcms", name, Time.elapsed());
    }

    static BufferedImage buf(TextureRegion region){
        return imageCache.get(((AtlasRegion)region).name);
    }

    static Image create(int width, int height){
        return new Image(width, height);
    }

    static Image get(String name){
        return get(Core.atlas.find(name));
    }

    static boolean has(String name){
        return Core.atlas.has(name);
    }

    static Image get(TextureRegion region){
        GenRegion.validate(region);

        return new Image(imageCache.get(((AtlasRegion)region).name));
    }

    static void err(String message, Object... args){
        throw new IllegalArgumentException(Strings.format(message, args));
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

        static void validate(TextureRegion region){
            if(((GenRegion)region).invalid){
                ImagePacker.err("Region does not exist: @", ((GenRegion)region).name);
            }
        }
    }
}
