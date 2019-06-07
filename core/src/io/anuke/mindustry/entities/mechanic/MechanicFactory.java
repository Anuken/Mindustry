package io.anuke.mindustry.entities.mechanic;

import io.anuke.mindustry.type.Mech;

public class MechanicFactory {
    public Mech createMechanic(String name, boolean flying){
        Mech mech = null;
        switch(name) {
            case "alpha-mech":
                mech = new alpha(name, flying);
            case "delta-mech":
                mech = new delta(name, flying);
            case "tau-mech":
                mech = new tau(name, flying);
            case "omega-mech":
                mech = new tau(name, flying);
            case "dart-ship":
                mech = new dart(name, flying);
            case "javeline-ship":
                mech = new javelin(name, flying);
            case "trident-ship":
                mech = new trident(name, flying);
            case "glaive-ship":
                mech = new glaive(name, flying);
        }
        return mech;
    }
}
