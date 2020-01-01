package mindustry.entities.units;

import arc.*;
import arc.util.*;
import arc.struct.*;
import mindustry.entities.type.base.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class Hivemind{
    private static long lastFrameUpdated = -1;
    private static Interval timer = new Interval(1);

    private static ObjectMap<Tile, CraterUnit> on = new ObjectMap<>();

    public static void update(){
        if(Core.graphics.getFrameId() == lastFrameUpdated) return;
        lastFrameUpdated = Core.graphics.getFrameId();
        if(!timer.get(30)) return;

        on.clear();

        ObjectSet<CraterUnit> craters = new ObjectSet<>();
        unitGroup.all().each(e -> e instanceof CraterUnit, crater -> craters.add((CraterUnit)crater));

        craters.each(crater -> {
            on.put(crater.on(), crater);
        });

        craters.each(i -> {
            craters.each(crater -> {
                if(!on.containsKey(crater.aspires())){
                    crater.purpose = crater.aspires();
                    on.put(crater.aspires(), crater);
                    on.remove(crater.on());
                    craters.remove(crater);
                };
            });
        });
    }
}
