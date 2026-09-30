package mindustry.logic.instructions;

import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class ApplyEffectI implements LInstruction{
    public boolean clear;
    public LVar effect, unit, duration;

    public ApplyEffectI(boolean clear, LVar effect, LVar unit, LVar duration){
        this.clear = clear;
        this.effect = effect;
        this.unit = unit;
        this.duration = duration;
    }

    public ApplyEffectI(){
    }

    @Override
    public void run(LExecutor exec){
        if(net.client()) return;

        if(unit.obj() instanceof Unit unit && effect.obj() instanceof StatusEffect effect){
            if(clear){
                unit.unapply(effect);
            }else{
                unit.apply(effect, duration.numf() * 60f);
            }
        }
    }
}
