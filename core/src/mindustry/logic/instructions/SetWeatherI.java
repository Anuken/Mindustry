package mindustry.logic.instructions;

import arc.util.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;

public class SetWeatherI implements LInstruction{
    public LVar type, state;

    public SetWeatherI(LVar type, LVar state){
        this.type = type;
        this.state = state;
    }

    @Override
    public void run(LExecutor exec){
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
