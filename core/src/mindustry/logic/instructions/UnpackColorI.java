package mindustry.logic.instructions;

import arc.util.*;
import mindustry.logic.*;

public class UnpackColorI implements LogicInstruction{
    public LogicVar r, g, b, a, value;

    public UnpackColorI(LogicVar r, LogicVar g, LogicVar b, LogicVar a, LogicVar value){
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.value = value;
    }

    public UnpackColorI(){
    }

    @Override
    public void run(LogicExecutor exec){
        var color = Tmp.c1.fromDouble(value.num());
        r.setnum(color.r);
        g.setnum(color.g);
        b.setnum(color.b);
        a.setnum(color.a);
    }
}
