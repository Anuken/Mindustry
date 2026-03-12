package mindustry.maps.generators;

import arc.math.geom.*;
import arc.struct.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

public class FileMapGenerator implements WorldGenerator{
    public final Map map;
    public final SectorPreset preset;

    public FileMapGenerator(String mapName, SectorPreset preset){
        if(maps == null){
            this.map = null;
        }else{
            Seq<String> candidates = new Seq<>(4);

            //<planetname>/<mapname>.msav
            candidates.add(preset.planet.name + "/" + mapName);

            //<mapname>.msav (directly in maps folder)
            candidates.add(mapName);

            //for modded maps, try loading without the mod prefix
            if(preset.minfo.mod != null){
                String baseName = mapName.substring(1 + preset.minfo.mod.name.length());

                //<planetname>/<mapname>.msav
                candidates.add(preset.planet.name + "/" + baseName);

                //<mapname>.msav (directly in maps folder)
                candidates.add(baseName);
            }

            //find the first matching candidate to load
            String fileName = candidates.find(name -> Vars.tree.get("maps/" + name + "." + mapExtension).exists());

            this.map = maps.loadInternalMap(fileName == null ? candidates.first() : fileName);
        }

        this.preset = preset;
    }

    public FileMapGenerator(Map map, SectorPreset preset){
        this.map = map;
        this.preset = preset;
    }

    /** If you use this constructor, make sure to override generate()! */
    public FileMapGenerator(SectorPreset preset){
        this(emptyMap, preset);
    }

    @Override
    public void generate(Tiles tiles, WorldParams params){
        if(map == null) throw new RuntimeException("Generator has null map, cannot be used.");

        Sector sector = state.rules.sector;

        world.setGenerating(false);
        SaveIO.load(map.file, world.new FilterContext(map){
            @Override
            public Sector getSector(){
                return sector;
            }

            @Override
            public void end(){
                applyFilters();
                //no super.end(), don't call world load event twice
            }

            @Override
            public boolean isMap(){
                return true;
            }
        });
        world.setGenerating(true);

        //make sure sector is maintained - don't reset it after map load.
        if(sector != null){
            state.rules.sector = sector;
        }

        tiles = world.tiles;

        boolean anyCores = false;

        //TODO: unsure if indexer even works at this stage
        Block coreTypeToUse = state.rules.defaultTeam.cores().isEmpty() ? sector.planet.defaultCore : state.rules.defaultTeam.core().block;

        for(Tile tile : tiles){

            if(tile.overlay() == Blocks.spawn){
                int rad = 10;
                Geometry.circle(tile.x, tile.y, tiles.width, tiles.height, rad, (wx, wy) -> {
                    if(tile.overlay().itemDrop != null){
                        tile.clearOverlay();
                    }
                });
            }

            if(params.corePositionOverride != 0 && sector != null){
                if(tile.pos() == params.corePositionOverride){
                    if(sector.allowLaunchLoadout()){
                        Schematics.placeLaunchLoadout(tile.x, tile.y);
                    }else{
                        //if there's an override and no loadout schematic is allowed, try to place a fitting core instead.
                        tile.setBlock(coreTypeToUse, state.rules.defaultTeam, 0);
                    }
                    anyCores = true;

                    if(preset.addStartingItems || !preset.planet.allowLaunchLoadout){
                        tile.build.items.clear();
                        tile.build.items.add(state.rules.loadout);
                    }
                }else if(tile.build instanceof CoreBuild && tile.team() == state.rules.defaultTeam && tile.build.pos() != params.corePositionOverride){
                    //other cores placed must be cleared; they have been overridden
                    tile.remove();
                }
            }else if(tile.isCenter() && tile.block() instanceof CoreBlock && tile.team() == state.rules.defaultTeam && !anyCores){
                if(sector != null && sector.allowLaunchLoadout()){
                    Schematics.placeLaunchLoadout(tile.x, tile.y);
                }
                anyCores = true;

                if(preset.addStartingItems || !preset.planet.allowLaunchLoadout){
                    tile.build.items.clear();
                    tile.build.items.add(state.rules.loadout);
                }
            }
        }

        if(!anyCores){
            throw new IllegalArgumentException("All maps must have a core.");
        }

        state.map = map;
    }
}
