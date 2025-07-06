package mindustry.logic;

public interface LWritable{
    boolean writable(LExecutor exec);
    void write(LVar at, double value);
    default void write(LVar at, Object value){}
}
