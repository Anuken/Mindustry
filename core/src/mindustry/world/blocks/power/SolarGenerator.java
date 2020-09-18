package mindustry.world.blocks.power;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import mindustry.graphics.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class SolarGenerator extends PowerGenerator{

    public SolarGenerator(String name){
        super(name);
        //remove the BlockFlag.producer flag to make this a lower priority target than other generators.
        flags = EnumSet.of();
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(generationType);
        stats.add(generationType, powerProduction * 60.0f, StatUnit.powerSecond);
    }

    @Override
    public TextureRegion[] icons(){
        return teamRegion.found() ? new TextureRegion[]{region, teamRegion} : super.icons();
    }

    public class SolarGeneratorBuild extends GeneratorBuild{
        @Override
        public void updateTile(){
            productionEfficiency = enabled ?
                Mathf.maxZero(Attribute.light.env() +
                    (state.rules.lighting ?
                        1f - state.rules.ambientLight.a :
                        1f
                    )) : 0f;
        }

        @Override
        public void draw(){
            super.draw();

            Draw.color(team.color);
            if(teamRegion != null && teamRegion != Core.atlas.find("error")) Draw.rect(teamRegion, x, y);
            Draw.color();
        }
    }
}
