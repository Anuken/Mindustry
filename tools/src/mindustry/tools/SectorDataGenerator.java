package mindustry.tools;

import arc.*;
import arc.backend.headless.mock.*;
import arc.files.*;
import arc.mock.*;
import arc.struct.*;
import arc.struct.ObjectIntMap.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.net.Net;
import mindustry.type.*;
import mindustry.type.Sector.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

public class SectorDataGenerator{

    public static void main(String[] args){
        ArcNativesLoader.load();
        Core.files = new MockFiles();
        Core.app = new MockApplication();
        Core.settings = new MockSettings();
        Core.graphics = new MockGraphics();

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

            Seq<SectorData> list = planet.sectors.map(sector -> {
                SectorData data = new SectorData();

                ObjectIntMap<Block> floors = new ObjectIntMap<>();
                ObjectSet<Content> content = new ObjectSet<>();

                logic.reset();
                world.loadSector(sector);
                float waterFloors = 0, totalFloors = 0;
                state.rules.sector = sector;

                for(Tile tile : world.tiles){
                    if(world.getDarkness(tile.x, tile.y) >= 3){
                        continue;
                    }

                    Liquid liquid = tile.floor().liquidDrop;
                    if(tile.floor().itemDrop != null) content.add(tile.floor().itemDrop);
                    if(tile.overlay().itemDrop != null) content.add(tile.overlay().itemDrop);
                    if(liquid != null) content.add(liquid);

                    if(!tile.block().isStatic()){
                        totalFloors ++;
                        if(liquid == Liquids.water){
                            waterFloors += tile.floor().isDeep() ? 1f : 0.7f;
                        }
                        floors.increment(tile.floor());
                        if(tile.overlay() != Blocks.air){
                            floors.increment(tile.overlay());
                        }
                    }
                }

                CoreEntity entity = Team.sharded.core();
                int cx = entity.tileX(), cy = entity.tileY();

                int nearTiles = 0;
                int waterCheckRad = 5;

                //check for water presence
                for(int rx = -waterCheckRad; rx <= waterCheckRad; rx++){
                    for(int ry = -waterCheckRad; ry <= waterCheckRad; ry++){
                        Tile tile = world.tile(cx + rx, cy + ry);
                        if(tile == null || tile.floor().liquidDrop != null){
                            nearTiles ++;
                        }
                    }
                }

                if(waterFloors / totalFloors >= 0.6f){
                    Log.debug("Sector @ has @/@ water -> naval", sector.id, waterFloors, totalFloors);
                }

                //naval sector guaranteed
                if(nearTiles > 4){
                    Log.debug("Sector @ has @ water tiles at @ @ -> naval", sector.id, nearTiles, cx, cy);
                    waterFloors = totalFloors;
                }

                //sort counts in descending order
                Seq<Entry<Block>> entries = floors.entries().toArray();
                entries.sort(e -> -e.value);
                //remove all blocks occuring < 30 times - unimportant
                entries.removeAll(e -> e.value < 30);

                data.floors = new Block[entries.size];
                data.floorCounts = new int[entries.size];
                for(int i = 0; i < entries.size; i++){
                    data.floorCounts[i] = entries.get(i).value;
                    data.floors[i] = entries.get(i).key;
                }

                //TODO bad code
                boolean hasSnow = data.floors[0].name.contains("ice") || data.floors[0].name.contains("snow");
                boolean hasRain = !hasSnow && data.floors[0].name.contains("water");
                boolean hasDesert = !hasSnow && !hasRain && data.floors[0].name.contains("sand");
                boolean hasSpores = data.floors[0].name.contains("spore") || data.floors[0].name.contains("moss") || data.floors[0].name.contains("tainted");

                if(hasSnow){
                    data.attributes |= (1 << SectorAttribute.snowy.ordinal());
                }

                if(hasRain){
                    data.attributes |= (1 << SectorAttribute.rainy.ordinal());
                }

                if(hasDesert){
                    data.attributes |= (1 << SectorAttribute.desert.ordinal());
                }

                if(hasSpores){
                    data.attributes |= (1 << SectorAttribute.spores.ordinal());
                }

                data.resources = content.asArray().sort(Structs.comps(Structs.comparing(Content::getContentType), Structs.comparingInt(c -> c.id))).toArray(UnlockableContent.class);

                //50% water -> naval attribute
                if(waterFloors / totalFloors >= 0.6f){
                    data.attributes |= (1 << SectorAttribute.naval.ordinal());
                }

                if(count[0]++ % 10 == 0){
                    Log.info("&lyDone with sector &lm@/@", count[0], planet.sectors.size);
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
