package mindustry.world.blocks.power;

import arc.*;
import arc.struct.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.graphics.*;
import mindustry.world.meta.*;

import static mindustry.Vars.state;

public class SolarGenerator extends PowerGenerator{

    public SolarGenerator(String name){
        super(name);
        // Remove the BlockFlag.producer flag to make this a lower priority target than other generators.
        flags = EnumSet.of();
        entityType = GeneratorEntity::new;
    }

    @Override
    public void update(Tile tile){
        tile.<GeneratorEntity>ent().productionEfficiency = state.rules.lighting ? 1f - state.rules.ambientLight.a : 1f;
    }

    @Override
    public void setStats(){
        super.setStats();
        // Solar Generators don't really have an efficiency (yet), so for them 100% = 1.0f
        stats.remove(generationType);
        stats.add(generationType, powerProduction * 60.0f, StatUnit.powerSecond);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        drawPlaceText(Core.bundle.formatFloat("bar.efficiency", (state.rules.lighting ? 1f - state.rules.ambientLight.a : 1f) * 100, 0), x, y, valid);
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("efficiency", entity -> new Bar(
        () -> Core.bundle.formatFloat("bar.efficiency", ((GeneratorEntity)entity).productionEfficiency * 100f, 0),
        () -> Pal.ammo,
        () -> ((GeneratorEntity)entity).productionEfficiency));
    }
}
