package mindustry.logic.instructions;

import mindustry.logic.*;
import mindustry.type.*;

public class SenseWeatherI implements LInstruction{
    public LVar type, to;

    public SenseWeatherI(LVar type, LVar to){
        this.type = type;
        this.to = to;
    }

    @Override
    public void run(LExecutor exec){
        to.setbool(type.obj() instanceof Weather weather && weather.isActive());
    }
}
