package mindustry.content;

import arc.graphics.*;
import arc.math.geom.*;
import arc.util.noise.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.graphics.*;
import mindustry.graphics.g3d.*;
import mindustry.graphics.g3d.PlanetGrid.*;
import mindustry.maps.generators.*;
import mindustry.maps.planet.*;
import mindustry.type.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Planets implements ContentList{
    public static Planet
    sun,
    erekir,
    tantros,
    serpulo;

    @Override
    public void load(){
        sun = new Planet("sun", null, 2){{
            bloom = true;
            accessible = false;

            meshLoader = () -> new SunMesh(
                this, 4,
                5, 0.3, 1.7, 1.2, 1,
                1.1f,
                Color.valueOf("ff7a38"),
                Color.valueOf("ff9638"),
                Color.valueOf("ffc64c"),
                Color.valueOf("ffc64c"),
                Color.valueOf("ffe371"),
                Color.valueOf("f4ee8e")
            );
        }};

        erekir = new Planet("erekir", sun, 1, 2){{
            generator = new ErekirPlanetGenerator();
            meshLoader = () -> new HexMesh(this, 4);
            atmosphereColor = Color.valueOf("f07218");
            startSector = 10;
            atmosphereRadIn = 0.02f;
            atmosphereRadOut = 0.3f;
            tidalLock = true;
            orbitSpacing = 0.5f;
            totalRadius += 2.4f;
        }};

        for(int i = 0; i < 4; i++){
            new Planet("gier-" + i, erekir, 0.1f){{
                hasAtmosphere = false;
                //for testing only!
                alwaysUnlocked = true;
                updateLighting = false;
                sectors.add(new Sector(this, Ptile.empty));
                camRadius = 0.43f;
                orbitOffset = 0f;

                //new SectorPreset(name + "-sector", this, 0){{

                //}};

                generator = new BlankPlanetGenerator(){
                    @Override
                    public void generate(){
                        pass((x, y) -> {
                            floor = Blocks.space;
                        });

                        Schematics.placeLaunchLoadout(width/2, height/2);

                        state.rules.environment = Env.space;
                    }

                    @Override
                    public int getSectorSize(Sector sector){
                        return 300;
                    }
                };

                meshLoader = () -> new HexMesh(this, new HexMesher(){
                    Simplex sim = new Simplex(id);
                    @Override
                    public float getHeight(Vec3 position){
                        return (float)sim.octaveNoise3D(2, 0.55, 0.45f, 5f + position.x, 5f + position.y, 5f + position.z) * 14f;
                    }

                    @Override
                    public Color getColor(Vec3 position){
                        return Pal.gray;
                    }
                }, 2, Shaders.planet);
            }};
        }

        tantros = new Planet("tantros", sun, 1, 2){{
            generator = new TantrosPlanetGenerator();
            meshLoader = () -> new HexMesh(this, 4);
            atmosphereColor = Color.valueOf("3db899");
            startSector = 10;
            atmosphereRadIn = -0.01f;
            atmosphereRadOut = 0.3f;
        }};

        serpulo = new Planet("serpulo", sun, 1, 3){{
            generator = new SerpuloPlanetGenerator();
            meshLoader = () -> new HexMesh(this, 6);
            atmosphereColor = Color.valueOf("3c1b8f");
            atmosphereRadIn = 0.02f;
            atmosphereRadOut = 0.3f;
            startSector = 15;
            alwaysUnlocked = true;
        }};
    }
}
