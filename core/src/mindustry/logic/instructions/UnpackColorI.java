package mindustry.logic.instructions;

import arc.util.*;
import mindustry.logic.*;

public class UnpackColorI implements LInstruction{
    public LVar r, g, b, a, value;

    public UnpackColorI(LVar r, LVar g, LVar b, LVar a, LVar value){
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.value = value;
    }

    public UnpackColorI(){
    }

    @Override
    public void run(LExecutor exec){
        var color = Tmp.c1.fromDouble(value.num());
        r.setnum(color.r);
        g.setnum(color.g);
        b.setnum(color.b);
        a.setnum(color.a);
    }
}
