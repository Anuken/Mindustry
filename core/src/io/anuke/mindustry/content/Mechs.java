package io.anuke.mindustry.content;

import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.entities.mechanic.MechanicFactory;

public class Mechs implements ContentList{
    public static Mech alpha, delta, tau, omega, dart, javelin, trident, glaive;

    public static Mech starter;

    private MechanicFactory mechanicFactory = new MechanicFactory();

    @Override
    public void load(){
        alpha = mechanicFactory.createMechanic("alpha-mech", false);
        delta = mechanicFactory.createMechanic("delta-mech", false);
        tau = mechanicFactory.createMechanic("tau-mech", false);
        omega = mechanicFactory.createMechanic("omega-mech", false);
        dart = mechanicFactory.createMechanic("dart-ship", true);
        javelin = mechanicFactory.createMechanic("javelin-ship", true);
        trident = mechanicFactory.createMechanic("trident-ship", true);
        glaive = mechanicFactory.createMechanic("glaive-ship", true);
        starter = dart;
    }
}
