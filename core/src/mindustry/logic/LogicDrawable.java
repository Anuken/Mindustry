package mindustry.logic;

import arc.struct.*;

public interface LogicDrawable{
    boolean drawable(LogicExecutor exec);
    void draw(LongSeq buffer);
}
