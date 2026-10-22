package mindustry.ai.types;

import mindustry.entities.units.*;
import mindustry.gen.*;

public class NoAI implements UnitController{
    protected Unit unit;

    @Override
    public Unit unit(){
        return unit;
    }

    @Override
    public void unit(Unit unit){
        this.unit = unit;
    }

    @Override
    public boolean isValidController(){
        return true;
    }

    @Override
    public void updateUnit(){}

    @Override
    public void removed(Unit unit){}
}