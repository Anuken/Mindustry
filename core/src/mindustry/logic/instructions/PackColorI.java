package mindustry.logic.instructions;

import arc.graphics.*;
import arc.math.*;
import mindustry.logic.*;

public class PackColorI implements LInstruction{
    public LVar result, r, g, b, a;

    public PackColorI(LVar result, LVar r, LVar g, LVar b, LVar a){
        this.result = result;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public PackColorI(){
    }

    @Override
    public void run(LExecutor exec){
        result.setnum(Color.toDoubleBits(Mathf.clamp(r.numf()), Mathf.clamp(g.numf()), Mathf.clamp(b.numf()), Mathf.clamp(a.numf())));
    }
}
