package mindustry.logic.instructions;

import arc.util.*;
import mindustry.core.*;
import mindustry.logic.*;
import mindustry.logic.LogicFx.*;

public class EffectI implements LInstruction{
    public EffectEntry type;
    public LVar x, y, rotation, color, data;

    public EffectI(EffectEntry type, LVar x, LVar y, LVar rotation, LVar color, LVar data){
        this.type = type;
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.color = color;
        this.data = data;
    }

    public EffectI(){
    }

    @Override
    public void run(LExecutor exec){
        if(type != null){
            double col = color.num();
            //limit size so people don't create lag with ridiculous numbers (some explosions scale with size)
            float rot = type.rotate ? rotation.numf() :
            Math.min(rotation.numf(), 1000f);

            type.effect.at(World.unconv(x.numf()), World.unconv(y.numf()), rot, Tmp.c1.fromDouble(col), data.obj());
        }
    }
}
