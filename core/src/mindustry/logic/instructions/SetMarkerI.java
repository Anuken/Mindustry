package mindustry.logic.instructions;

import mindustry.logic.*;

import static mindustry.Vars.*;

public class SetMarkerI implements LogicInstruction{
    public LogicMarkerControl type = LogicMarkerControl.pos;
    public LogicVar id, p1, p2, p3;

    public SetMarkerI(LogicMarkerControl type, LogicVar id, LogicVar p1, LogicVar p2, LogicVar p3){
        this.type = type;
        this.id = id;
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
    }

    public SetMarkerI(){
    }

    @Override
    public void run(LogicExecutor exec){
        if(type == LogicMarkerControl.remove){
            state.markers.remove(id.numi());
        }else{
            var marker = state.markers.get(id.numi());
            if(marker == null) return;

            if(type == LogicMarkerControl.flushText){
                marker.setText(exec.textBuffer.toString(), p1.bool());
                exec.textBuffer.setLength(0);
            }else if(type == LogicMarkerControl.texture){
                if(p1.bool()){
                    marker.setTexture(exec.textBuffer.toString());
                    exec.textBuffer.setLength(0);
                }else{
                    marker.setTexture(p2.obj());
                }
            }else{
                marker.control(type, p1.numOrNan(), p2.numOrNan(), p3.numOrNan());
            }
        }
    }
}
