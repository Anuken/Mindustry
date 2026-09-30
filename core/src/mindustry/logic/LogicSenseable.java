package mindustry.logic;

public interface LogicSenseable{
    Object noSensed = new Object();

    double sense(LogicProp sensor);

    default double sense(Object object){
        return 0;
    }

    default Object senseObject(LogicProp sensor){
        return noSensed;
    }

    default Object senseObject(double value){
        return noSensed;
    }
}
