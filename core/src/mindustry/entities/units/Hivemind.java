package mindustry.entities.units;

import arc.*;
import arc.util.*;
import arc.struct.*;
import mindustry.world.*;
import mindustry.entities.type.base.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.distribution.PlastaniumConveyor.*;

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

        Array<CraterUnit> craters = new Array<>();
        unitGroup.all().each(e -> e instanceof CraterUnit, crater -> craters.add((CraterUnit)crater));

        craters.sort(Structs.comparingInt(crater -> sortPriority(crater.on())));

        craters.each(crater -> {
            on.put(crater.on(), crater);
        });

        craters.each(crater -> {
            if(crater == null || crater.aspires() == null) return;
            if(!on.containsKey(crater.aspires())){
                crater.purpose = crater.aspires();
                on.put(crater.aspires(), crater);
                on.remove(crater.on());
            }
        });
    }

    private static int sortPriority(Tile tile){
        if(tile == null) return 0;
        if(!(tile.block() instanceof PlastaniumConveyor)) return 0;
        PlastaniumConveyorEntity entity = tile.ent();
        return entity.tree;
    }
}
