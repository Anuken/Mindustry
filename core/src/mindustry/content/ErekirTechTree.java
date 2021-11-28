package mindustry.content;

import static mindustry.content.Blocks.*;
import static mindustry.content.TechTree.*;

public class ErekirTechTree{

    public static void load(){
        rootErekir = node(coreBastion, () -> {
            node(duct, () -> {
                node(ductRouter, () -> {
                    node(ductBridge, () -> {
                        node(surgeConveyor, () -> {
                            node(surgeRouter);
                        });
                    });

                    node(overflowDuct, () -> {

                    });

                    node(reinforcedContainer, () -> {
                        node(reinforcedVault, () -> {

                        });
                    });
                });
            });
        });
    }
}
