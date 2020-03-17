package mindustry.tools;

import arc.*;
import arc.files.*;
import arc.mock.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.net.Net;
import mindustry.type.*;
import mindustry.type.Sector.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class SectorDataGenerator{

    public static void main(String[] args){
        ArcNativesLoader.load();
        Core.files = new MockFiles();
        Core.app = new MockApplication();
        Core.settings = new MockSettings();

        headless = true;
        net = new Net(null);
        tree = new FileTree();
        Vars.init();
        content.createBaseContent();

        logic = new Logic();
        netServer = new NetServer();
        world = new World();

        content.init();

        for(Planet planet : content.<Planet>getBy(ContentType.planet)){
            int[] count = {0};
            if(planet.grid == null) continue;

            Fi fi = Fi.get("planets").child(planet.name + ".dat");

            Array<SectorData> list = planet.sectors.map(sector -> {
                SectorData data = new SectorData();

                ObjectIntMap<Block> floors = new ObjectIntMap<>();
                ObjectSet<Content> content = new ObjectSet<>();

                world.loadSector(sector);

                for(Tile tile : world.tiles){
                    Item item = tile.floor().itemDrop;
                    Liquid liquid = tile.floor().liquidDrop;
                    if(item != null) content.add(item);
                    if(liquid != null) content.add(liquid);

                    if(!tile.block().isStatic()){
                        floors.increment(tile.floor());
                        if(tile.overlay() != Blocks.air){
                            floors.increment(tile.overlay());
                        }
                    }
                }

                //sort counts in descending order
                Array<ObjectIntMap.Entry<Block>> entries = floors.entries().toArray();
                entries.sort(e -> -e.value);
                //remove all blocks occuring < 30 times - unimportant
                entries.removeAll(e -> e.value < 30);

                data.floors = new Block[entries.size];
                data.floorCounts = new int[entries.size];
                for(int i = 0; i < entries.size; i++){
                    data.floorCounts[i] = entries.get(i).value;
                    data.floors[i] = entries.get(i).key;
                }

                data.resources = content.asArray().sort(Structs.comps(Structs.comparing(Content::getContentType), Structs.comparingInt(c -> c.id))).toArray(UnlockableContent.class);

                if(count[0]++ % 10 == 0){
                    Log.info("&lyDone with sector &lm{0}/{1}", count[0], planet.sectors.size);
                }

                return data;
            });

            //write data
            try(Writes write = fi.writes()){
                write.s(list.size);
                list.each(s -> s.write(write));
            }
        }
    }
}
