package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.Mechs;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.units.MechFactory;

public class UpgradeBlocks extends BlockList{
    public static Block deltaFactory, tauFactory, omegaFactory, dartFactory, javelinFactory, tridentFactory, halberdFactory;

    @Override
    public void load(){
        deltaFactory = new MechFactory("delta-mech-factory"){{
            mech = Mechs.delta;
            size = 2;
            powerCapacity = 70f;
        }};

        tauFactory = new MechFactory("tau-mech-factory"){{
            mech = Mechs.tau;
            size = 2;
            powerCapacity = 100f;
        }};

        omegaFactory = new MechFactory("omega-mech-factory"){{
            mech = Mechs.omega;
            size = 3;
            powerCapacity = 120f;
        }};

        dartFactory = new MechFactory("dart-ship-factory"){{
            mech = Mechs.dart;
            size = 2;
            powerCapacity = 50f;
        }};

        javelinFactory = new MechFactory("javelin-ship-factory"){{
            mech = Mechs.javelin;
            size = 2;
            powerCapacity = 80f;
        }};

        tridentFactory = new MechFactory("trident-ship-factory"){{
            mech = Mechs.trident;
            size = 2;
            powerCapacity = 100f;
        }};

        halberdFactory = new MechFactory("halberd-ship-factory"){{
            mech = Mechs.halberd;
            size = 3;
            powerCapacity = 120f;
        }};
    }
}
