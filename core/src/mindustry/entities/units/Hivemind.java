package mindustry.entities.units;

import arc.*;
import arc.util.*;
import arc.struct.*;
import mindustry.entities.*;
import mindustry.entities.type.base.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class Hivemind{
    private static long lastFrameUpdated = -1;
    private static Interval timer = new Interval(1);
    private static ObjectMap<Tile, CraterUnit> dibs = new ObjectMap<>();

    public static void update(){
        if(Core.graphics.getFrameId() == lastFrameUpdated) return;
        lastFrameUpdated = Core.graphics.getFrameId();
        if(!timer.get(30)) return;

        dibs.clear();
        ObjectSet<CraterUnit> craters = new ObjectSet<>();
        unitGroup.all().each(e -> e instanceof CraterUnit, crater -> craters.add((CraterUnit)crater));

        craters.each(crater -> {
            if(occupied(crater, crater.aspires())){
                crater.purpose = crater.on();
            }else{
                crater.purpose = crater.aspires();
            }
            dibs.put(crater.purpose, crater);
        });
    }

    private static boolean occupied(CraterUnit crater, Tile tile){
        final boolean[] result = {false};

        Units.allEntities(tile, unit -> {
            if(unit != crater) result[0] = true;
        });

        if(dibs.containsKey(tile) && dibs.get(tile) != crater) result[0] = true;

        return result[0];
    }
}
