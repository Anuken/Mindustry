package mindustry.logic.instructions;

import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class ApplyEffectI implements LogicInstruction{
    public boolean clear;
    public LogicVar effect, unit, duration;

    public ApplyEffectI(boolean clear, LogicVar effect, LogicVar unit, LogicVar duration){
        this.clear = clear;
        this.effect = effect;
        this.unit = unit;
        this.duration = duration;
    }

    public ApplyEffectI(){
    }

    @Override
    public void run(LogicExecutor exec){
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
