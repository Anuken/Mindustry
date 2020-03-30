package mindustry.world.blocks.power;

import arc.struct.*;
import mindustry.content.*;
import mindustry.entities.type.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class SolarGenerator extends PowerGenerator{

    public SolarGenerator(String name){
        super(name);
        // Remove the BlockFlag.producer flag to make this a lower priority target than other generators.
        flags = EnumSet.of(BlockFlag.multipart);
        entityType = GeneratorEntity::new;
    }

    @Override
    public void update(Tile tile){
        tile.<GeneratorEntity>ent().productionEfficiency = state.rules.solarPowerMultiplier < 0 ? (state.rules.lighting ? 1f - state.rules.ambientLight.a : 1f) : state.rules.solarPowerMultiplier;
    }

    @Override
    public void setStats(){
        super.setStats();
        // Solar Generators don't really have an efficiency (yet), so for them 100% = 1.0f
        stats.remove(generationType);
        stats.add(generationType, powerProduction * 60.0f, StatUnit.powerSecond);
    }

    @Override
    public void placed(Tile tile){
        super.placed(tile);

        multipart(tile, null);
    }

    @Override
    public void multipart(Tile tile, Player player){
        Block microblock = Blocks.repairPoint;
        if(tile.block == Blocks.largeSolarPanel){
            Tile corner = world.tile(tile.x - 1, tile.y - 1);

            if(corner.block != microblock){
                corner.setNet(microblock, tile.getTeam(), 0);
                corner.block.placed(corner);
            }else{
                if(player != null) Call.setTile(player, corner, microblock, tile.getTeam(), 0);
            }
        }
    }
}
