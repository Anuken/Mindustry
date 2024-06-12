package mindustry.logic;

import arc.util.*;
import mindustry.game.*;
import mindustry.gen.*;

public class LVar{
    public final String name;
    public int id;

    public boolean isobj, constant;

    public Object objval;
    public double numval;

    //ms timestamp for when this was last synced; used in the sync instruction
    public long syncTime;

    public LVar(String name){
        this(name, -1);
    }

    public LVar(String name, int id){
        this(name, id, false);
    }

    public LVar(String name, int id, boolean constant){
        this.name = name;
        this.id = id;
        this.constant = constant;
    }

    public @Nullable Building building(){
        return isobj && objval instanceof Building building ? building : null;
    }

    public @Nullable Object obj(){
        return isobj ? objval : null;
    }

    public @Nullable Team team(){
        if(isobj){
            return objval instanceof Team t ? t : null;
        }else{
            int t = (int)numval;
            if(t < 0 || t >= Team.all.length) return null;
            return Team.all[t];
        }
    }

    public boolean bool(){
        return isobj ? objval != null : Math.abs(numval) >= 0.00001;
    }

    public double num(){
        return isobj ? objval != null ? 1 : 0 : invalid(numval) ? 0 : numval;
    }

    /** Get num value from variable, convert null to NaN to handle it differently in some instructions */
    public double numOrNan(){
        return isobj ? objval != null ? 1 : Double.NaN : invalid(numval) ? 0 : numval;
    }

    public float numf(){
        return isobj ? objval != null ? 1 : 0 : invalid(numval) ? 0 : (float)numval;
    }

    /** Get float value from variable, convert null to NaN to handle it differently in some instructions */
    public float numfOrNan(){
        return isobj ? objval != null ? 1 : Float.NaN : invalid(numval) ? 0 : (float)numval;
    }

    public int numi(){
        return (int)num();
    }

    public void setbool(boolean value){
        setnum(value ? 1 : 0);
    }

    public void setnum(double value){
        if(constant) return;
        if(invalid(value)){
            objval = null;
            isobj = true;
        }else{
            numval = value;
            objval = null;
            isobj = false;
        }
    }

    public void setobj(Object value){
        if(constant) return;
        objval = value;
        isobj = true;
    }

    public void setconst(Object value){
        objval = value;
        isobj = true;
    }

    public static boolean invalid(double d){
        return Double.isNaN(d) || Double.isInfinite(d);
    }

}
