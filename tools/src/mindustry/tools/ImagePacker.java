package mindustry.tools;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.geom.*;
import arc.mock.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Log.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.logic.*;
import mindustry.world.blocks.*;

import java.io.*;

public class ImagePacker{
    static ObjectMap<String, PackIndex> cache = new ObjectMap<>();

    public static void main(String[] args) throws Exception{
        Vars.headless = true;
        //makes PNG loading slightly faster
        ArcNativesLoader.load();

        Core.settings = new MockSettings();
        Log.logger = new NoopLogHandler();
        Vars.content = new ContentLoader();
        Vars.content.createBaseContent();
        Vars.content.init();
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
            public PixmapRegion getPixmap(AtlasRegion region){
                return new PixmapRegion(get(region.name));
            }

            @Override
            public boolean has(String s){
                return cache.containsKey(s);
            }
        };

        Draw.scl = 1f / Core.atlas.find("scale_marker").width;

        Time.mark();
        Vars.content.load();
        Generators.run();
        Log.info("&ly[Generator]&lc Total time to generate: &lg@&lcms", Time.elapsed());

        //write icons to icons.properties

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

        //now, write the IDs to logicids.dat

        //don't write to the file unless I'm packing, because logic IDs rarely change and I don't want merge conflicts from PRs
        if(!OS.username.equals("anuke")) return;

        //format: ([content type (byte)] [content count (short)] (repeat [name (string)])) until EOF
        Fi logicidfile = Fi.get("../../../assets/logicids.dat");

        Seq<UnlockableContent> lookupCont = new Seq<>();

        for(ContentType t : GlobalVars.lookableContent){
            lookupCont.addAll(Vars.content.<UnlockableContent>getBy(t).select(UnlockableContent::logicVisible));
        }

        ObjectIntMap<UnlockableContent>[] registered = new ObjectIntMap[ContentType.all.length];
        IntMap<UnlockableContent>[] idToContent = new IntMap[ContentType.all.length];

        for(int i = 0; i < ContentType.all.length; i++){
            registered[i] = new ObjectIntMap<>();
            idToContent[i] = new IntMap<>();
        }

        if(logicidfile.exists()){
            try(DataInputStream in = new DataInputStream(logicidfile.readByteStream())){
                for(ContentType ctype : GlobalVars.lookableContent){
                    short amount = in.readShort();
                    for(int i = 0; i < amount; i++){
                        String name = in.readUTF();
                        UnlockableContent fetched = Vars.content.getByName(ctype, name);
                        if(fetched != null){
                            registered[ctype.ordinal()].put(fetched, i);
                            idToContent[ctype.ordinal()].put(i, fetched);
                        }
                    }
                }
            }
        }

        //map stuff that hasn't been mapped yet
        for(UnlockableContent c : lookupCont){
            int ctype = c.getContentType().ordinal();
            if(!registered[ctype].containsKey(c)){
                int nextId = 0;
                //find next ID - this is O(N) but content counts are so low that I don't really care
                //checking the last ID doesn't work because there might be "holes"
                for(UnlockableContent other : lookupCont){
                    if(!idToContent[ctype].containsKey(other.id + 1)){
                        nextId = other.id + 1;
                        break;
                    }
                }

                idToContent[ctype].put(nextId, c);
                registered[ctype].put(c, nextId);
            }
        }

        //write the resulting IDs
        try(DataOutputStream out = new DataOutputStream(logicidfile.write(false, 2048))){
            for(ContentType t : GlobalVars.lookableContent){
                Seq<UnlockableContent> all = idToContent[t.ordinal()].values().toArray().sort(u -> registered[t.ordinal()].get(u));
                out.writeShort(all.size);
                for(UnlockableContent u : all){
                    out.writeUTF(u.name);
                }
            }
        }
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

    static void delete(String name){
        ((GenRegion)Core.atlas.find(name)).path.delete();
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
