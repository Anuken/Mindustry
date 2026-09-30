package mindustry.logic.instructions;

import mindustry.logic.*;

import static mindustry.Vars.*;

public class SetMarkerI implements LInstruction{
    public LMarkerControl type = LMarkerControl.pos;
    public LVar id, p1, p2, p3;

    public SetMarkerI(LMarkerControl type, LVar id, LVar p1, LVar p2, LVar p3){
        this.type = type;
        this.id = id;
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
    }

    public SetMarkerI(){
    }

    @Override
    public void run(LExecutor exec){
        if(type == LMarkerControl.remove){
            state.markers.remove(id.numi());
        }else{
            var marker = state.markers.get(id.numi());
            if(marker == null) return;

            if(type == LMarkerControl.flushText){
                marker.setText(exec.textBuffer.toString(), p1.bool());
                exec.textBuffer.setLength(0);
            }else if(type == LMarkerControl.texture){
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
