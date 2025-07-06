package mindustry.logic;

public interface Readable{
    double read(LVar from);

    default Object readObject(LVar from){
        return Senseable.noSensed;
    }
}
