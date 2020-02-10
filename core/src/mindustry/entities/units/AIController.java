package mindustry.entities.units;

import arc.math.*;
import arc.util.*;
import mindustry.gen.*;

public class AIController implements UnitController{
    protected Unitc unit;

    float rot = Mathf.random(360f);

    @Override
    public void unit(Unitc unit){
        this.unit = unit;
    }

    @Override
    public Unitc unit(){
        return unit;
    }

    @Override
    public void update(){
        rot += Mathf.range(3f) * Time.delta();

        unit.moveAt(Tmp.v1.trns(rot, unit.type().speed));
        if(!unit.vel().isZero()){
            unit.lookAt(unit.vel().angle());
        }
    }
}
