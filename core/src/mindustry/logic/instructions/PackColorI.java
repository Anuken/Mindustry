package mindustry.logic.instructions;

import arc.graphics.*;
import arc.math.*;
import mindustry.logic.*;

public class PackColorI implements LogicInstruction{
    public LogicVar result, r, g, b, a;

    public PackColorI(LogicVar result, LogicVar r, LogicVar g, LogicVar b, LogicVar a){
        this.result = result;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public PackColorI(){
    }

    @Override
    public void run(LogicExecutor exec){
        result.setnum(Color.toDoubleBits(Mathf.clamp(r.numf()), Mathf.clamp(g.numf()), Mathf.clamp(b.numf()), Mathf.clamp(a.numf())));
    }
}
