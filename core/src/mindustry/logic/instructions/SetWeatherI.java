package mindustry.logic.instructions;

import arc.util.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;

public class SetWeatherI implements LogicInstruction{
    public LogicVar type, state;

    public SetWeatherI(LogicVar type, LogicVar state){
        this.type = type;
        this.state = state;
    }

    @Override
    public void run(LogicExecutor exec){
        if(type.obj() instanceof Weather weather){
            if(state.bool()){
                if(!weather.isActive()){ //Create is not already active
                    Tmp.v1.setToRandomDirection();
                    Call.createWeather(weather, 1f, WeatherState.fadeTime, Tmp.v1.x, Tmp.v1.y);
                }else{
                    weather.instance().life(WeatherState.fadeTime);
                }
            }else{
                if(weather.isActive() && weather.instance().life > WeatherState.fadeTime){
                    weather.instance().life(WeatherState.fadeTime);
                }
            }
        }
    }
}
