package mindustry.entities.units;

import mindustry.gen.*;

public class AIController implements UnitController{
    protected Unitc unit;

    @Override
    public void unit(Unitc unit){
        this.unit = unit;
    }

    @Override
    public Unitc unit(){
        return unit;
    }


}
