package mindustry.logic;

public interface LReadable{
    boolean readable(LExecutor exec);
    double read(LVar from);
    default Object readObject(LVar from){
        return Senseable.noSensed;
    }
}
