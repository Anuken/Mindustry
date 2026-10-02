package arc.math;

public interface Scaled{
    /** 0 to 1. */
    float fin();

    /** 1 to 0 */
    default float fout(){
        return 1f - fin();
    }

    /** 1 to 0 */
    default float fout(Interp i){
        return i.apply(fout());
    }

    /** 1 to 0, ending at the specified margin */
    default float fout(float margin){
        float f = fin();
        if(f >= 1f - margin){
            return 1f - (f - (1f - margin)) / margin;
        }else{
            return 1f;
        }
    }

    /** 0 to 1 **/
    default float fin(Interp i){
        return i.apply(fin());
    }

    /** 0 to 1 */
    default float finpow(){
        return Interp.pow3Out.apply(fin());
    }
    
    /** 1 to 0 */
    default float foutpow(){
        return 1 - Interp.pow3Out.apply(fin());
    }
    
    /** 0 to 1 */
    default float finpowdown(){
        return Interp.pow3In.apply(fin());
    }
    
    /** 1 to 0 */
    default float foutpowdown(){
        return 1 - Interp.pow3In.apply(fin());
    }

    /** 0 to 1 to 0 */
    default float fslope(){
        return (0.5f - Math.abs(fin() - 0.5f)) * 2f;
    }

}
