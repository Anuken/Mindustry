package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.Mechs;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.units.MechPad;

public class UpgradeBlocks extends BlockList{
    public static Block alphaPad, deltaPad, tauPad, omegaPad, dartPad, javelinPad, tridentPad, glaivePad;

    @Override
    public void load(){

        alphaPad = new MechPad("alpha-mech-pad"){{
            mech = Mechs.alpha;
            size = 2;
            consumes.powerBuffered(50f);
        }};

        deltaPad = new MechPad("delta-mech-pad"){{
            mech = Mechs.delta;
            size = 2;
            consumes.powerBuffered(70f);
        }};

        tauPad = new MechPad("tau-mech-pad"){{
            mech = Mechs.tau;
            size = 2;
            consumes.powerBuffered(100f);
        }};

        omegaPad = new MechPad("omega-mech-pad"){{
            mech = Mechs.omega;
            size = 3;
            consumes.powerBuffered(120f);
        }};

        dartPad = new MechPad("dart-ship-pad"){{
            mech = Mechs.dart;
            size = 2;
            consumes.powerBuffered(50f);
            shadow = "shadow-rounded-2";
        }};

        javelinPad = new MechPad("javelin-ship-pad"){{
            mech = Mechs.javelin;
            size = 2;
            consumes.powerBuffered(80f);
            shadow = "shadow-rounded-2";
        }};

        tridentPad = new MechPad("trident-ship-pad"){{
            mech = Mechs.trident;
            size = 2;
            consumes.powerBuffered(100f);
            shadow = "shadow-rounded-2";
        }};

        glaivePad = new MechPad("glaive-ship-pad"){{
            mech = Mechs.glaive;
            size = 3;
            consumes.powerBuffered(120f);
            shadow = "shadow-round-3";
        }};
    }
}
