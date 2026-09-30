package mindustry.logic.instructions;

import mindustry.game.objectives.*;
import mindustry.logic.*;

import static mindustry.Vars.*;

public class MakeMarkerI implements LInstruction{
    //TODO arbitrary number
    public static final int maxMarkers = 20000;

    public String type = "shape";
    public LVar id, x, y, replace;

    public MakeMarkerI(String type, LVar id, LVar x, LVar y, LVar replace){
        this.type = type;
        this.id = id;
        this.x = x;
        this.y = y;
        this.replace = replace;
    }

    public MakeMarkerI(){
    }

    @Override
    public void run(LExecutor exec){
        var cons = MapObjectives.markerNameToType.get(type);

        if(cons != null && state.markers.size() < maxMarkers){
            int mid = id.numi();
            if(replace.bool() || !state.markers.has(mid)){
                var marker = cons.get();
                marker.control(LMarkerControl.pos, x.num(), y.num(), 0);
                state.markers.add(mid, marker);
            }
        }
    }
}
