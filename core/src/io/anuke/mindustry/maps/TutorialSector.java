package io.anuke.mindustry.maps;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.blocks.CraftingBlocks;
import io.anuke.mindustry.content.blocks.ProductionBlocks;
import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.content.blocks.UnitBlocks;
import io.anuke.mindustry.maps.missions.BlockMission;
import io.anuke.mindustry.maps.missions.ItemMission;
import io.anuke.mindustry.maps.missions.Mission;
import io.anuke.mindustry.maps.missions.WaveMission;
import io.anuke.mindustry.world.Block;

import static io.anuke.mindustry.Vars.*;

/**Just a class for returning the list of tutorial missions.*/
public class TutorialSector{
    private static int droneIndex;

    public static Array<Mission> getMissions(){
/*
        Array<Mission> missions = Array.with(
            new ItemMission(Items.copper, 60).setMessage("$tutorial.begin"),

            new BlockMission(ProductionBlocks.mechanicalDrill).setMessage("$tutorial.drill"),

            new BlockMission(DistributionBlocks.conveyor).setShowComplete(false).setMessage("$tutorial.conveyor"),

            new ItemMission(Items.copper, 100).setMessage("$tutorial.morecopper"),

            new BlockMission(TurretBlocks.duo).setMessage("$tutorial.turret"),
            /
            //new BlockMission(ProductionBlocks.mechanicalDrill).setMessage("$tutorial.drillturret"),

            // Create a wave mission which spawns the core at 60, 60 rather than in the center of the map
            new WaveMission(2, 60, 60).setMessage("$tutorial.waves"),

            new ItemMission(Items.lead, 150).setMessage("$tutorial.lead"),
            new ItemMission(Items.copper, 250).setMessage("$tutorial.morecopper"),

            new BlockMission(CraftingBlocks.smelter).setMessage("$tutorial.smelter"),

            //drills for smelter
            new BlockMission(ProductionBlocks.mechanicalDrill),
            new BlockMission(ProductionBlocks.mechanicalDrill),
            new BlockMission(ProductionBlocks.mechanicalDrill),

            new ItemMission(Items.densealloy, 20).setMessage("$tutorial.densealloy"),

            new MarkerBlockMission(CraftingBlocks.siliconsmelter).setMessage("$tutorial.siliconsmelter"),

            //coal line
            new BlockMission(ProductionBlocks.mechanicalDrill).setMessage("$tutorial.silicondrill"),

            //sand line
            new BlockMission(ProductionBlocks.mechanicalDrill),
            new BlockMission(ProductionBlocks.mechanicalDrill),


            new BlockMission(PowerBlocks.combustionGenerator).setMessage("$tutorial.generator"),
            new BlockMission(ProductionBlocks.mechanicalDrill).setMessage("$tutorial.generatordrill"),
            new BlockMission(PowerBlocks.powerNode).setMessage("$tutorial.node"),
            //TODO fix positions
            new ConditionMission(Bundles.get("text.mission.linknode"), () -> world.tile(54, 52).entity != null && world.tile(54, 52).entity.power != null && world.tile(54, 52).entity.power.amount >= 0.01f)
                .setMessage("$tutorial.nodelink"),

            new ItemMission(Items.silicon, 70).setMessage("$tutorial.silicon"),

            new BlockMission(UnitBlocks.daggerFactory).setMessage("$tutorial.daggerfactory"),

            //power for dagger factory
            new BlockMission(PowerBlocks.powerNode),
            new BlockMission(PowerBlocks.powerNode),

            new UnitMission(UnitTypes.dagger).setMessage("$tutorial.dagger"),
            new ActionMission(TutorialSector::generateBase),
            new BattleMission(){
                public void generate(Generation gen){} //no
            }.setMessage("$tutorial.battle")
        );

        //find drone marker mission
        for(int i = 0; i < missions.size; i++){
            if(missions.get(i) instanceof MarkerBlockMission){
                droneIndex = i;
                break;
            }
        }*/

        return Array.with(
            //intentionally unlocalized
            new ItemMission(Items.copper, 50).setMessage("An updated tutorial will return next build.\nFor now, you'll have to deal with... this."),

            new BlockMission(ProductionBlocks.mechanicalDrill),

            new ItemMission(Items.copper, 100),
            new ItemMission(Items.lead, 50),

            new BlockMission(CraftingBlocks.smelter),
            new ItemMission(Items.densealloy, 10),
            new WaveMission(5)
        );
    }

    public static boolean supressDrone(){
        return world.getSector() != null && world.getSector().x == 0 && world.getSector().y == 0 && world.getSector().completedMissions < droneIndex;
    }

    private static void generateBase(){
        int x = sectorSize - 50, y = sectorSize - 50;
        world.setBlock(world.tile(x, y), StorageBlocks.core, waveTeam);
        world.setBlock(world.tile(x - 1, y + 2), UnitBlocks.daggerFactory, waveTeam);
        world.setBlock(world.tile(x - 1, y - 3), UnitBlocks.daggerFactory, waveTeam);

        //since placed() is not called here, add core manually
        state.teams.get(waveTeam).cores.add(world.tile(x, y));
    }

    private static class MarkerBlockMission extends BlockMission{
        public MarkerBlockMission(Block block){
            super(block);
        }
    }
}
