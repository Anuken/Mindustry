package mindustry.ai.formations.patterns;

import arc.math.geom.*;
import mindustry.ai.formations.*;

public class ArrowFormation extends FormationPattern{
    //total triangular numbers
    private static final int totalTris = 30;
    //triangular number table
    private static final int[] triTable = new int[totalTris];

    //calculat triangular numbers
    static{
        int sum = 0;
        for(int i = 0; i < totalTris; i++){
            triTable[i] = sum;
            sum += (i + 1);
        }
    }

    @Override
    public Vec3 calculateSlotLocation(Vec3 out, int slot){
        //TODO
        return out;
    }
}
