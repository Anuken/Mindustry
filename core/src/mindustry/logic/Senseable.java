package mindustry.logic;

public interface Senseable{
    Object noSensed = new Object();

    double sense(LAccess sensor);

    default double sense(Object object){
        return 0;
    }

    default Object senseObject(LAccess sensor){
        return noSensed;
    }

    default Object senseObject(double value){
        return noSensed;
    }
}
