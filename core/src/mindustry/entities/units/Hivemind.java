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
    private static ObjectMap<Tile, CraterUnit> dibstc = new ObjectMap<>();
    private static ObjectMap<CraterUnit, Tile> dibsct = new ObjectMap<>();

    public static void update(){
        if(Core.graphics.getFrameId() == lastFrameUpdated) return;
        lastFrameUpdated = Core.graphics.getFrameId();
        if(!timer.get(30)) return;

        dibstc.clear();
        dibsct.clear();
        ObjectSet<CraterUnit> craters = new ObjectSet<>();
        unitGroup.all().each(e -> e instanceof CraterUnit, crater -> craters.add((CraterUnit)crater));

        craters.each(crater -> {
            if(occupied(crater, crater.aspires())){
                crater.purpose = crater.on();
            }else{
                crater.purpose = crater.aspires();
            }
            dibstc.put(crater.purpose, crater);
            dibsct.put(crater, crater.purpose);
        });
    }

    private static boolean occupied(CraterUnit crater, Tile tile){
        final boolean[] result = {false};

        Units.allEntities(tile, unit -> {
            if(unit != crater && dibsct.get((CraterUnit)unit) != tile) result[0] = true;
        });

        if(dibstc.containsKey(tile) && dibstc.get(tile) != crater) result[0] = true;

        return result[0];
    }
}
