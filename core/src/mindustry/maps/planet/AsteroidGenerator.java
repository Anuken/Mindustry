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
    public static int min = 15, max = 25, octaves = 2, foct = 3;

    public static float radMin = 5f, radMax = 50f, persistence = 0.4f, scale = 30f, mag = 0.46f, thresh = 1f;

    public static float fmag = 0.6f, fscl = 50f, fper = 0.6f;

    Rand rand;
    int seed;

    void asteroid(int ax, int ay, int radius){

        for(int x = ax - radius; x <= ax + radius; x++){
            for(int y = ay - radius; y <= ay + radius; y++){
                if(tiles.in(x, y) &&  Mathf.dst(x, y, ax, ay) / (radius) + Simplex.noise2d(seed, octaves, persistence, 1f / scale, x, y) * mag < thresh){
                    tiles.getn(x, y).setFloor(Blocks.stone.asFloor());
                }
            }
        }
    }

    @Override
    public void generate(){
        seed = state.rules.sector.planet.id;
        int sx = width/2, sy = height/2;
        rand = new Rand(seed);

        pass((x, y) -> {
            floor = Blocks.space;
        });

        asteroid(sx, sy, rand.random(30, 50));

        int amount = rand.random(min, max);
        for(int i = 0; i < amount; i++){
            float radius = rand.random(radMin, radMax), ax = rand.random(radius, width - radius), ay = rand.random(radius, height - radius);

            asteroid((int)ax, (int)ay, (int)radius);
        }

        //tiny asteroids.
        int smalls = rand.random(min, max * 2);
        for(int i = 0; i < smalls; i++){
            float radius = rand.random(1, 8), ax = rand.random(radius, width - radius), ay = rand.random(radius, height - radius);

            asteroid((int)ax, (int)ay, (int)radius);
        }

        pass((x, y) -> {
            if(floor != Blocks.space){
                if(Ridged.noise2d(seed, x, y, foct, fper, 1f / fscl) > fmag){
                    floor = Blocks.graphiticStone;
                }
            }
        });

        pass((x, y) -> {
            if(floor == Blocks.space || Ridged.noise2d(seed + 1, x, y, 3, 0.5f, 1f / 70f) > 0.5f) return;

            int radius = 5;
            for(int dx = x - radius; dx <= x + radius; dx++){
                for(int dy = y - radius; dy <= y + radius; dy++){
                    if(Mathf.within(dx, dy, x, y, radius + 0.0001f) && tiles.in(dx, dy) && tiles.getn(dx, dy).floor() == Blocks.space){
                        return;
                    }
                }
            }

            block = floor.asFloor().wall;

        });

        Schematics.placeLaunchLoadout(sx, sy);

        state.rules.environment = Env.space;
    }

    @Override
    public int getSectorSize(Sector sector){
        return 450;
    }
}
