package mindustry.maps.planet;

import arc.math.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.maps.generators.*;
import mindustry.type.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class AsteroidGenerator extends BlankPlanetGenerator{
    //TODO nonstatic
    public static int min = 20, max = 28, octaves = 2, foct = 3;
    public static float radMin = 12f, radMax = 60f, persistence = 0.4f, scale = 30f, mag = 0.46f, thresh = 1f;
    public static float fmag = 0.59f, fscl = 50f, fper = 0.6f;
    public static float iceChance = 0.05f, carbonChance = 0.1f;

    Rand rand;
    int seed;

    void asteroid(int ax, int ay, int radius){
        Floor floor = (
            rand.chance(iceChance) ? Blocks.ice :
            rand.chance(carbonChance) ? Blocks.graphiticStone :
            Blocks.ferricStone
        ).asFloor();

        for(int x = ax - radius; x <= ax + radius; x++){
            for(int y = ay - radius; y <= ay + radius; y++){
                if(tiles.in(x, y) &&  Mathf.dst(x, y, ax, ay) / radius + Simplex.noise2d(seed, octaves, persistence, 1f / scale, x, y) * mag < thresh){
                    tiles.getn(x, y).setFloor(floor);
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

        //spawn asteroids
        asteroid(sx, sy, rand.random(30, 50));

        int amount = rand.random(min, max);
        for(int i = 0; i < amount; i++){
            float radius = rand.random(radMin, radMax), ax = rand.random(radius, width - radius), ay = rand.random(radius, height - radius);

            asteroid((int)ax, (int)ay, (int)radius);
        }

        //tiny asteroids
        int smalls = rand.random(min, max) * 3;
        for(int i = 0; i < smalls; i++){
            float radius = rand.random(1, 8), ax = rand.random(radius, width - radius), ay = rand.random(radius, height - radius);

            asteroid((int)ax, (int)ay, (int)radius);
        }

        //random noise stone
        pass((x, y) -> {
            if(floor != Blocks.space){
                if(Ridged.noise2d(seed, x, y, foct, fper, 1f / fscl) - Ridged.noise2d(seed, x, y, 1, 1f, 5f)/2.7f > fmag){
                    floor = Blocks.stone;
                }
            }
        });

        //walls at insides
        pass((x, y) -> {
            if(floor == Blocks.space || Ridged.noise2d(seed + 1, x, y, 3, 0.5f, 1f / 60f) > 0.38f || Mathf.within(x, y, sx, sy, 20 + Ridged.noise2d(seed, x, y, 3, 0.5f, 1f / 30f) * 6f)) return;

            int radius = 6;
            for(int dx = x - radius; dx <= x + radius; dx++){
                for(int dy = y - radius; dy <= y + radius; dy++){
                    if(Mathf.within(dx, dy, x, y, radius + 0.0001f) && tiles.in(dx, dy) && tiles.getn(dx, dy).floor() == Blocks.space){
                        return;
                    }
                }
            }

            block = floor.asFloor().wall;

        });

        //random craters
        pass((x, y) -> {
            if(floor == Blocks.ferricStone && rand.chance(0.02)) floor = Blocks.ferricCraters;
            if(floor == Blocks.stone && rand.chance(0.02)) floor = Blocks.craters;
        });

        decoration(0.013f);

        //lead generates around stone walls
        oreAround(Blocks.oreLead, Blocks.stoneWall, 3, 70f, 0.6f);

        //copper only generates on ferric stone
        ore(Blocks.oreCopper, Blocks.ferricStone, 5f, 0.8f);

        wallOre(Blocks.carbonWall, Blocks.graphiticWall, 35f, 0.57f);

        //TODO
        //wallOre(Blocks.iceWall, Blocks.wallOreBeryl, 35f, 0.57f);

        //TODO:
        //- thorium - cores?
        //- copper maybe should not exist
        //- consider replacing certain ores with something else
        //- sand source - olivine/pyroxene
        //- beryllium in walls

        //titanium
        pass((x, y) -> {
            if(floor != Blocks.stone) return;
            int i = 4;

            if(Math.abs(0.5f - noise(x, y + i*999 - x*1.5f, 2, 0.65, (60 + i * 2))) > 0.26f * 1f){
                ore = Blocks.oreTitanium;
            }
        });

        Schematics.placeLaunchLoadout(sx, sy);

        state.rules.environment = Env.space;
    }

    @Override
    public int getSectorSize(Sector sector){
        return 500;
    }
}
