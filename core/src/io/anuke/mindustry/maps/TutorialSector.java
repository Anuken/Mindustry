package io.anuke.mindustry.maps;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.content.blocks.*;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.maps.generation.Generation;
import io.anuke.mindustry.maps.generation.WorldGenerator.GenResult;
import io.anuke.mindustry.maps.missions.*;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.ucore.core.Events;

import static io.anuke.mindustry.Vars.*;

/**Just a class for returning the list of tutorial missions.*/
public class TutorialSector{

    public static Array<Mission> getMissions(){
        //int x = sectorSize/2, y = sectorSize/2;

        return Array.with(
        new ItemMission(Items.copper, 30).setMessage("$tutorial.begin"),

        new BlockLocMission(ProductionBlocks.mechanicalDrill, 55, 62).setMessage("$tutorial.drill"),

        new BlockLocMission(DistributionBlocks.conveyor, 57, 62, 0).setShowComplete(false).setMessage("$tutorial.conveyor"),
        new BlockLocMission(DistributionBlocks.conveyor, 58, 62, 0).setShowComplete(false),
        new BlockLocMission(DistributionBlocks.conveyor, 59, 62, 0).setShowComplete(false),
        new BlockLocMission(DistributionBlocks.conveyor, 60, 62, 3).setShowComplete(false),

        new ItemMission(Items.copper, 50).setMessage("$tutorial.morecopper"),

        new BlockLocMission(TurretBlocks.duo, 56, 59).setMessage("$tutorial.turret"),
        new BlockLocMission(ProductionBlocks.mechanicalDrill, 55, 60).setMessage("$tutorial.drillturret"),

        new WaveMission(5).setMessage("$tutorial.waves"),

        new ActionMission(() -> {
            Array<Item> ores = Array.with(Items.copper, Items.coal, Items.lead);
            GenResult res = new GenResult();
            for(int x = 0; x < world.width(); x++){
                for(int y = 0; y < world.height(); y++){
                    Tile tile = world.tile(x, y);
                    world.generator().generateTile(res, 0, 0, x, y, true, null, ores);
                    if(!tile.hasCliffs()){
                        tile.setFloor((Floor) res.floor);
                    }
                }
            }
            Events.fire(new WorldLoadEvent());
        }),

        new ItemMission(Items.lead, 30).setMessage("$tutorial.lead"),
        new ItemMission(Items.copper, 150).setMessage("$tutorial.morecopper"),

        new BlockMission(CraftingBlocks.smelter).setMessage("$tutorial.smelter"),
        new ItemMission(Items.densealloy, 30).setMessage("$tutorial.densealloy"),
        new BlockMission(CraftingBlocks.siliconsmelter).setMessage("$tutorial.siliconsmelter"),
        new BlockMission(PowerBlocks.combustionGenerator).setMessage("$tutorial.generator"),
        new BlockMission(PowerBlocks.powerNode).setMessage("$tutorial.node"),

        new ItemMission(Items.silicon, 30).setMessage("$tutorial.silicon"),

        new BlockMission(UnitBlocks.daggerFactory).setMessage("$tutorial.daggerfactory"),
        new UnitMission(UnitTypes.dagger).setMessage("$tutorial.dagger"),
        new ExpandMission(1, 0),
        new BattleMission(){
            public void generate(Generation gen){
            }

            @Override
            public boolean isComplete(){
                return false;
            }

            public void onBegin(){
                super.onBegin();
                generateBase();
            }
        }.setMessage("$tutorial.battle")
        );
    }

    public static boolean supressDrone(){
        return world.getSector() != null && world.getSector().x == 0 && world.getSector().y == 0;
    }

    private static void generateBase(){
        int x = sectorSize/2, y = sectorSize + sectorSize/2;
        world.setBlock(world.tile(x, y), StorageBlocks.core, waveTeam);
       // world.setBlock(world.tile(x + 1, y + 2), TurretBlocks.duo, waveTeam);
        //world.setBlock(world.tile(x + 1, y - 2), TurretBlocks.duo, waveTeam);
        world.setBlock(world.tile(x - 1, y + 2), UnitBlocks.daggerFactory, waveTeam);
        world.setBlock(world.tile(x - 1, y - 3), UnitBlocks.daggerFactory, waveTeam);

        //fill turret ammo
        //world.tile(x + 1, y + 2).block().handleStack(Items.copper, 1, world.tile(x + 1, y + 2), null);
        //world.tile(x + 1, y - 2).block().handleStack(Items.copper, 1, world.tile(x + 1, y - 2), null);

        //since placed() is not called here, add core manually
        state.teams.get(waveTeam).cores.add(world.tile(x, y));
    }
}
