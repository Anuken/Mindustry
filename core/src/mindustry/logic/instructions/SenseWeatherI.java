package mindustry.logic.instructions;

import mindustry.logic.*;
import mindustry.type.*;

public class SenseWeatherI implements LogicInstruction{
    public LogicVar type, to;

    public SenseWeatherI(LogicVar type, LogicVar to){
        this.type = type;
        this.to = to;
    }

    @Override
    public void run(LogicExecutor exec){
        to.setbool(type.obj() instanceof Weather weather && weather.isActive());
    }
}
