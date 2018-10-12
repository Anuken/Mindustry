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
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.*;

/**Just a class for returning the list of tutorial missions.*/
public class TutorialSector{
    private static int droneIndex;

    public static Array<Mission> getMissions(){

        Array<Mission> missions = Array.with(
            new ItemMission(Items.copper, 60).setMessage("$tutorial.begin"),

            new BlockLocMission(ProductionBlocks.mechanicalDrill, 55, 62).setMessage("$tutorial.drill"),

            new BlockLocMission(DistributionBlocks.conveyor, 57, 62, 0).setShowComplete(false).setMessage("$tutorial.conveyor"),
            new BlockLocMission(DistributionBlocks.conveyor, 58, 62, 0).setShowComplete(false),
            new BlockLocMission(DistributionBlocks.conveyor, 59, 62, 0).setShowComplete(false),
            new BlockLocMission(DistributionBlocks.conveyor, 60, 62, 3).setShowComplete(false),

            new ItemMission(Items.copper, 100).setMessage("$tutorial.morecopper"),

            new BlockLocMission(TurretBlocks.duo, 56, 59).setMessage("$tutorial.turret"),
            new BlockLocMission(ProductionBlocks.mechanicalDrill, 55, 60).setMessage("$tutorial.drillturret"),

            new WaveMission(2).setMessage("$tutorial.waves"),

            new ActionMission(() ->
                Timers.runTask(30f, () -> {
                    Runnable r = () -> {
                        Array<Item> ores = Array.with(Items.copper, Items.coal, Items.lead);
                        GenResult res = new GenResult();
                        for(int x = 0; x < world.width(); x++){
                            for(int y = 0; y < world.height(); y++){
                                Tile tile = world.tile(x, y);
                                world.generator.generateTile(res, 0, 0, x, y, true, null, ores);
                                if(!tile.hasCliffs()){
                                    tile.setFloor((Floor) res.floor);
                                }
                            }
                        }
                        Events.fire(new WorldLoadEvent());
                    };

                    if(headless){
                        ui.loadLogic(r);
                    }else{
                        threads.run(r);
                    }
                })),

            new ItemMission(Items.lead, 150).setMessage("$tutorial.lead"),
            new ItemMission(Items.copper, 250).setMessage("$tutorial.morecopper"),

            new BlockLocMission(CraftingBlocks.smelter, 58, 69).setMessage("$tutorial.smelter"),

            //drills for smelter
            new BlockLocMission(ProductionBlocks.mechanicalDrill, 62, 86),
            new BlockLocMission(ProductionBlocks.mechanicalDrill, 58, 89),
            new BlockLocMission(ProductionBlocks.mechanicalDrill, 54, 68),

            //conveyors for smelter
            new LineBlockMission(DistributionBlocks.conveyor, 58, 88, 58, 70, 3),
            new LineBlockMission(DistributionBlocks.conveyor, 61, 86, 61, 70, 3),
            new LineBlockMission(DistributionBlocks.conveyor, 61, 69, 59, 69, 2),
            new LineBlockMission(DistributionBlocks.conveyor, 56, 69, 57, 69, 0),
            new LineBlockMission(DistributionBlocks.conveyor, 58, 68, 58, 63, 3),
            new BlockLocMission(DistributionBlocks.junction, 58, 62, 0),
            new BlockLocMission(DistributionBlocks.conveyor, 58, 61, 0),

            new ItemMission(Items.densealloy, 20).setMessage("$tutorial.densealloy"),

            new MarkerBlockMission(CraftingBlocks.siliconsmelter, 54, 52).setMessage("$tutorial.siliconsmelter"),

            //coal line
            new BlockLocMission(ProductionBlocks.mechanicalDrill, 47, 52).setMessage("$tutorial.silicondrill"),
            new LineBlockMission(DistributionBlocks.conveyor, 49, 52, 53, 52, 0),

            //sand line
            new BlockLocMission(ProductionBlocks.mechanicalDrill, 53, 49),
            new BlockLocMission(ProductionBlocks.mechanicalDrill, 56, 49),
            new LineBlockMission(DistributionBlocks.conveyor, 55, 50, 55, 51, 1),

            //silicon line
            new LineBlockMission(DistributionBlocks.conveyor, 56, 53, 59, 53, 0),
            new LineBlockMission(DistributionBlocks.conveyor, 60, 53, 60, 58, 1),

            new BlockLocMission(PowerBlocks.combustionGenerator, 49, 54).setMessage("$tutorial.generator"),
            new BlockLocMission(ProductionBlocks.mechanicalDrill, 47, 54).setMessage("$tutorial.generatordrill"),
            new BlockLocMission(PowerBlocks.powerNode, 52, 54).setMessage("$tutorial.node"),
            new ConditionMission(Bundles.get("text.mission.linknode"), () -> world.tile(54, 52).entity != null && world.tile(54, 52).entity.power != null && world.tile(54, 52).entity.power.amount >= 0.01f)
                .setMessage("$tutorial.nodelink"),

            new ItemMission(Items.silicon, 70).setMessage("$tutorial.silicon"),

            new BlockLocMission(UnitBlocks.daggerFactory, 64, 59).setMessage("$tutorial.daggerfactory"),

            //silicon lines for dagger factory
            new BlockLocMission(DistributionBlocks.router, 60, 57).setMessage("$tutorial.router"),
            new LineBlockMission(DistributionBlocks.conveyor, 61, 57, 63, 57, 0),
            new LineBlockMission(DistributionBlocks.conveyor, 64, 57, 64, 58, 1),

            //power for dagger factory
            new BlockLocMission(PowerBlocks.powerNode, 57, 54),
            new BlockLocMission(PowerBlocks.powerNode, 62, 54),

            new UnitMission(UnitTypes.dagger).setMessage("$tutorial.dagger"),
            new ExpandMission(1, 0){
                @Override
                public void onComplete(){
                    super.onComplete();
                    generateBase();
                }
            },
            new BattleMission(){
                public void generate(Generation gen){} //no
                public void onBegin(){} //also no
            }.setMessage("$tutorial.battle")
        );

        //find drone marker mission
        for(int i = 0; i < missions.size; i++){
            if(missions.get(i) instanceof MarkerBlockMission){
                droneIndex = i;
                break;
            }
        }

        return missions;
    }

    public static boolean supressDrone(){
        return world.getSector() != null && world.getSector().x == 0 && world.getSector().y == 0 && world.getSector().completedMissions < droneIndex;
    }

    private static void generateBase(){
        int x = sectorSize/2 + sectorSize, y = sectorSize/2;
        world.setBlock(world.tile(x, y), StorageBlocks.core, waveTeam);
        world.setBlock(world.tile(x - 1, y + 2), UnitBlocks.daggerFactory, waveTeam);
        world.setBlock(world.tile(x - 1, y - 3), UnitBlocks.daggerFactory, waveTeam);

        //since placed() is not called here, add core manually
        state.teams.get(waveTeam).cores.add(world.tile(x, y));
    }

    private static class MarkerBlockMission extends BlockLocMission{
        public MarkerBlockMission(Block block, int x, int y){
            super(block, x, y);
        }
    }
}
