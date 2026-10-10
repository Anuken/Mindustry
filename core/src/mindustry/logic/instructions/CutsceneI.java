package mindustry.logic.instructions;

import arc.math.*;
import mindustry.core.*;
import mindustry.logic.*;

import static mindustry.Vars.*;

public class CutsceneI implements LogicInstruction{
    public CutsceneAction action = CutsceneAction.stop;
    public LogicVar p1, p2, p3, p4;

    public CutsceneI(CutsceneAction action, LogicVar p1, LogicVar p2, LogicVar p3, LogicVar p4){
        this.action = action;
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        this.p4 = p4;
    }

    public CutsceneI(){
    }

    @Override
    public void run(LogicExecutor exec){
        if(headless) return;

        switch(action){
            case active -> p1.setbool(control.input.logicCutscene);
            case pan -> {
                control.input.logicCutscene = true;
                control.input.logicCamPan.set(World.unconv(p1.numf()), World.unconv(p2.numf()));
                control.input.logicCamSpeed = p3.numf();
                control.input.logicCutsceneInteract = p4.bool();
            }
            case zoom -> {
                control.input.logicCutscene = true;
                control.input.logicCutsceneZoom = Mathf.clamp(p1.numf());
                control.input.logicCutsceneInteract = p2.bool();
            }
            case stop -> {
                control.input.logicCutscene = false;
                control.input.logicCutsceneInteract = false;
            }
            case shake -> {
                renderer.shake(p1.numf(), p2.numf() * 60);
                control.input.logicCutsceneInteract = p3.bool();
            }
            case getHud -> p1.setbool(!control.input.logicHideHud);
            case setHud -> control.input.logicHideHud = !p1.bool();
        }
    }
}
