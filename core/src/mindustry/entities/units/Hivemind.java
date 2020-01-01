package mindustry.entities.units;

import arc.*;
import arc.util.*;
import arc.struct.*;
import mindustry.entities.type.base.*;

import static mindustry.Vars.*;

public class Hivemind{
    private static long lastFrameUpdated = -1;
    private static Interval timer = new Interval(1);

    public static void update(){
        if(Core.graphics.getFrameId() == lastFrameUpdated) return;
        lastFrameUpdated = Core.graphics.getFrameId();
        if(!timer.get(30)) return;

        ObjectSet<CraterUnit> craters = new ObjectSet<>();
        unitGroup.all().each(e -> e instanceof CraterUnit, crater -> craters.add((CraterUnit)crater));

        craters.each(crater -> {
            crater.purpose = crater.aspires();
        });
    }
}
