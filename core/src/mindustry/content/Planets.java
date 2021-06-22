package mindustry.content;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.graphics.g3d.*;
import mindustry.graphics.g3d.PlanetGrid.*;
import mindustry.maps.planet.*;
import mindustry.type.*;

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
            meshLoader = () -> new HexMesh(this, 5);
            atmosphereColor = Color.valueOf("f07218");
            startSector = 10;
            atmosphereRadIn = 0.02f;
            atmosphereRadOut = 0.3f;
            tidalLock = true;
            orbitSpacing = 0.45f;
            totalRadius += 2.6f;
            lightSrcTo = 0.5f;
            lightDstFrom = 0.2f;
        }};

        float[] offsets = {40, 0, 0, 20};
        for(int i = 0; i < 4; i++){
            int fi = i;
            new Planet("gier-" + i, erekir, 0.12f){{
                hasAtmosphere = false;
                //for testing only!
                alwaysUnlocked = true;
                updateLighting = false;
                sectors.add(new Sector(this, Ptile.empty));
                camRadius = 0.68f;
                minZoom = 0.6f;
                drawOrbit = false;
                orbitOffset = offsets[fi];

                generator = new AsteroidGenerator();

                meshLoader = () -> {
                    Seq<GenericMesh> meshes = new Seq<>();
                    Color color = Blocks.ferricStoneWall.mapColor;
                    Rand rand = new Rand(2);

                    meshes.add(new NoiseMesh(this, 0, 2, color, radius, 2, 0.55f, 0.45f, 14f));
                    int am = rand.random(3, 7);

                    //TODO gier variants with different names and different resource distributions
                    for(int j = 0; j < am; j++){
                        meshes.add(new MatMesh(
                            new NoiseMesh(this, j + 1, 1, 0.022f + rand.random(0.039f), 2, 0.6f, 0.38f, 20f,
                                color, Blocks.carbonWall.mapColor, 3, 0.6f, 0.38f, 0.6f),
                            new Mat3D().setToTranslation(Tmp.v31.setToRandomDirection(rand).setLength(rand.random(0.44f, 1.4f)))));
                    }

                    return new MultiMesh(meshes.toArray(GenericMesh.class));
                };
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
