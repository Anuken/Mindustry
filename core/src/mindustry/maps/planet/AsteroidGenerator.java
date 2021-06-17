package mindustry.maps.planet;

import arc.math.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.maps.generators.*;
import mindustry.type.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class AsteroidGenerator extends BlankPlanetGenerator{

    @Override
    public void generate(){
        int seed = state.rules.sector.planet.id;
        int sx = width/2, sy = height/2;

        pass((x, y) -> {
            floor = Blocks.space;

            if(Simplex.noise2d(seed, 5, 0.6f, 1f/ 100f, x, y) + Mathf.dst(x, y, sx, sy) / (float)width / 2f > 0.7f){
                floor = Blocks.stone;
            }
        });

        Schematics.placeLaunchLoadout(sx, sy);

        state.rules.environment = Env.space;
    }

    @Override
    public int getSectorSize(Sector sector){
        return 450;
    }
}
