package mindustry.logic.instructions;

import mindustry.game.objectives.*;
import mindustry.logic.*;

import static mindustry.Vars.*;

public class MakeMarkerI implements LogicInstruction{
    //TODO arbitrary number
    public static final int maxMarkers = 20000;

    public String type = "shape";
    public LogicVar id, x, y, replace;

    public MakeMarkerI(String type, LogicVar id, LogicVar x, LogicVar y, LogicVar replace){
        this.type = type;
        this.id = id;
        this.x = x;
        this.y = y;
        this.replace = replace;
    }

    public MakeMarkerI(){
    }

    @Override
    public void run(LogicExecutor exec){
        var cons = MapObjectives.markerNameToType.get(type);

        if(cons != null && state.markers.size() < maxMarkers){
            int mid = id.numi();
            if(replace.bool() || !state.markers.has(mid)){
                var marker = cons.get();
                marker.control(LogicMarkerControl.pos, x.num(), y.num(), 0);
                state.markers.add(mid, marker);
            }
        }
    }
}
