package io.anuke.mindustry.maps;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.content.blocks.*;
import io.anuke.mindustry.maps.generation.Generation;
import io.anuke.mindustry.maps.missions.*;

import static io.anuke.mindustry.Vars.*;

/**Just a class for returning the list of tutorial missions.*/
public class TutorialSector{

    public static Array<Mission> getMissions(){
        return Array.with(
            new ItemMission(Items.copper, 30).setMessage("$tutorial.begin"),
            new BlockMission(ProductionBlocks.mechanicalDrill).setMessage("$tutorial.drill"),
            new BlockMission(DistributionBlocks.conveyor).setMessage("$tutorial.conveyor"),
            new ItemMission(Items.copper, 50).setMessage("$tutorial.morecopper"),
            new BlockMission(TurretBlocks.duo).setMessage("$tutorial.turret"),
            new BlockMission(ProductionBlocks.mechanicalDrill).setMessage("$tutorial.drillturret"),
            new WaveMission(5).setMessage("$tutorial.waves"),
            new ExpandMission(1, 0),
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
            new ExpandMission(-1, 0),
            new BattleMission(){
                public void generate(Generation gen){}

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
        int x = sectorSize/2, y = sectorSize/2;
        world.setBlock(world.tile(x, y), StorageBlocks.core, waveTeam);
        world.setBlock(world.tile(x + 1, y + 2), TurretBlocks.duo, waveTeam);
        world.setBlock(world.tile(x + 1, y - 2), TurretBlocks.duo, waveTeam);
        world.setBlock(world.tile(x - 1, y + 2), UnitBlocks.daggerFactory, waveTeam);
        world.setBlock(world.tile(x - 1, y - 3), UnitBlocks.daggerFactory, waveTeam);

        //since placed() is not called here, add core manually
        state.teams.get(waveTeam).cores.add(world.tile(x, y));
    }
}
