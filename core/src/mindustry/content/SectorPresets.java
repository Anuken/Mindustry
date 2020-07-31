package mindustry.content;

import mindustry.ctype.*;
import mindustry.type.*;

import static mindustry.content.Planets.*;

public class SectorPresets implements ContentList{
    public static SectorPreset
    groundZero,
    craters, frozenForest, ruinousShores, stainedMountains, tarFields, fungalPass,
    saltFlats, overgrowth, impact0078, crags,
    desolateRift, nuclearComplex;

    @Override
    public void load(){

        groundZero = new SectorPreset("groundZero", serpulo, 15){{
            alwaysUnlocked = true;
            conditionWave = 5;
            launchPeriod = 5;
            rules = r -> {
                r.winWave = 10;
            };
        }};

        saltFlats = new SectorPreset("saltFlats", serpulo, 101){{
            conditionWave = 10;
            launchPeriod = 5;
        }};

        frozenForest = new SectorPreset("frozenForest", serpulo, 86){{
            conditionWave = 10;
        }};

        craters = new SectorPreset("craters", serpulo, 18){{
            conditionWave = 10;
        }};

        ruinousShores = new SectorPreset("ruinousShores", serpulo, 19){{
            conditionWave = 20;
            launchPeriod = 20;
        }};

        stainedMountains = new SectorPreset("stainedMountains", serpulo, 20){{
            conditionWave = 10;
            launchPeriod = 10;
        }};

        fungalPass = new SectorPreset("fungalPass", serpulo, 21){{

        }};

        overgrowth = new SectorPreset("overgrowth", serpulo, 22){{
            conditionWave = 12;
            launchPeriod = 4;
        }};

        tarFields = new SectorPreset("tarFields", serpulo, 23){{
            conditionWave = 15;
            launchPeriod = 10;
        }};

        desolateRift = new SectorPreset("desolateRift", serpulo, 123){{
            conditionWave = 3;
            launchPeriod = 2;
        }};


        nuclearComplex = new SectorPreset("nuclearComplex", serpulo, 130){{
            conditionWave = 30;
            launchPeriod = 15;
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
