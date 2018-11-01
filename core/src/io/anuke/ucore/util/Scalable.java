package io.anuke.ucore.util;

import com.badlogic.gdx.math.Interpolation;

public interface Scalable {
    /**0 to 1.*/
    float fin();

    /**1 to 0*/
    default float fout(){
        return 1f-fin();
    }

    /**1 to 0*/
    default float fout(Interpolation i){
        return i.apply(fout());
    }

    /**1 to 0, ending at the specified margin*/
    default float fout(float margin){
        float f = fin();
        if(f >= 1f-margin){
            return 1f-(f-(1f-margin))/margin;
        }else{
            return 1f;
        }
    }

    /**0 to 1**/
    default float fin(Interpolation i){
        return i.apply(fin());
    }

    /**0 to 1*/
    default float finpow(){
        return Interpolation.pow3Out.apply(fin());
    }

    /**0 to 1 to 0*/
    default float fslope(){
        return (0.5f-Math.abs(fin()-0.5f))*2f;
    }
}
