package mindustry.logic;

import mindustry.ctype.*;

public interface Senseable{
    Object noSensed = new Object();

    double sense(LAccess sensor);

    default double sense(Content content){
        return 0;
    }

    default Object senseObject(LAccess sensor){
        return noSensed;
    }
}
