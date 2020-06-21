package mindustry.content;

import mindustry.ctype.*;
import mindustry.game.Objectives.*;
import mindustry.type.*;

import static arc.struct.Seq.*;
import static mindustry.content.Planets.*;

public class SectorPresets implements ContentList{
    public static SectorPreset
    groundZero,
    craters, frozenForest, ruinousShores, stainedMountains, tarFields, fungalPass,
    saltFlats, overgrowth, impact0078, crags,
    desolateRift, nuclearComplex;

    @Override
    public void load(){

        groundZero = new SectorPreset("groundZero", starter, 15){{
            alwaysUnlocked = true;
            conditionWave = 5;
            launchPeriod = 5;
            rules = r -> {
                r.winWave = 30;
            };
        }};

        saltFlats = new SectorPreset("saltFlats", starter, 101){{
            conditionWave = 10;
            launchPeriod = 5;
            requirements = with(
            new SectorWave(groundZero, 60),
            //new Unlock(Blocks.daggerFactory),
            //new Unlock(Blocks.draugFactory),
            new Unlock(Blocks.door),
            new Unlock(Blocks.waterExtractor)
            );
        }};

        frozenForest = new SectorPreset("frozenForest", starter, 86){{
            conditionWave = 10;
            requirements = with(
            new SectorWave(groundZero, 10),
            new Unlock(Blocks.junction),
            new Unlock(Blocks.router)
            );
        }};

        craters = new SectorPreset("craters", starter, 18){{
            conditionWave = 10;
            requirements = with(
            new SectorWave(frozenForest, 10),
            new Unlock(Blocks.mender),
            new Unlock(Blocks.combustionGenerator)
            );
        }};

        ruinousShores = new SectorPreset("ruinousShores", starter, 19){{
            conditionWave = 20;
            launchPeriod = 20;
            requirements = with(
            new SectorWave(groundZero, 20),
            new SectorWave(craters, 15),
            new Unlock(Blocks.graphitePress),
            new Unlock(Blocks.combustionGenerator),
            new Unlock(Blocks.kiln),
            new Unlock(Blocks.mechanicalPump)
            );
        }};

        stainedMountains = new SectorPreset("stainedMountains", starter, 20){{
            conditionWave = 10;
            launchPeriod = 10;
            requirements = with(
            new SectorWave(frozenForest, 15),
            new Unlock(Blocks.pneumaticDrill),
            new Unlock(Blocks.powerNode),
            new Unlock(Blocks.turbineGenerator)
            );
        }};

        fungalPass = new SectorPreset("fungalPass", starter, 21){{
            requirements = with(
            new SectorWave(stainedMountains, 15),
            //new Unlock(Blocks.daggerFactory),
            //new Unlock(Blocks.crawlerFactory),
            new Unlock(Blocks.door),
            new Unlock(Blocks.siliconSmelter)
            );
        }};

        overgrowth = new SectorPreset("overgrowth", starter, 22){{
            conditionWave = 12;
            launchPeriod = 4;
            requirements = with(
            new SectorWave(craters, 40),
            new Launched(fungalPass),
            new Unlock(Blocks.cultivator),
            new Unlock(Blocks.sporePress)
            //new Unlock(Blocks.titanFactory),
            //new Unlock(Blocks.wraithFactory)
            );
        }};

        tarFields = new SectorPreset("tarFields", starter, 23){{
            conditionWave = 15;
            launchPeriod = 10;
            requirements = with(
            new SectorWave(ruinousShores, 20),
            new Unlock(Blocks.coalCentrifuge),
            new Unlock(Blocks.conduit),
            new Unlock(Blocks.wave)
            );
        }};

        desolateRift = new SectorPreset("desolateRift", starter, 123){{
            conditionWave = 3;
            launchPeriod = 2;
            requirements = with(
            new SectorWave(tarFields, 20),
            new Unlock(Blocks.thermalGenerator),
            new Unlock(Blocks.thoriumReactor)
            );
        }};


        nuclearComplex = new SectorPreset("nuclearComplex", starter, 130){{
            conditionWave = 30;
            launchPeriod = 15;
            requirements = with(
            new Launched(fungalPass),
            new Unlock(Blocks.thermalGenerator),
            new Unlock(Blocks.laserDrill)
            );
        }};

        /*
        crags = new Zone("crags", new MapGenerator("crags").dist(2f)){{
            loadout = Loadouts.basicFoundation;
            baseLaunchCost = ItemStack.with();
            startingItems = ItemStack.list(Items.copper, 2000, Items.lead, 2000, Items.graphite, 500, Items.titanium, 500, Items.silicon, 500);
            conditionWave = 3;
            launchPeriod = 2;
            requirements = with(stainedMountains, 40);
            blockRequirements = new Block[]{Blocks.thermalGenerator};
            resources = Array.with(Items.copper, Items.scrap, Items.lead, Items.coal, Items.sand};
        }};

        
        impact0078 = new SectorPreset("impact0078"){{
            loadout = Loadouts.basicNucleus;
            baseLaunchCost = ItemStack.list();
            startingItems = ItemStack.list(Items.copper, 2000, Items.lead, 2000, Items.graphite, 500, Items.titanium, 500, Items.silicon, 500);
            conditionWave = 3;
            launchPeriod = 2;
            //requirements = with(nuclearComplex, 40);
            //blockRequirements = new Block[]{Blocks.thermalGenerator};
            //resources = Array.with(Items.copper, Items.scrap, Items.lead, Items.coal, Items.titanium, Items.thorium};
        }};*/
    }
}
