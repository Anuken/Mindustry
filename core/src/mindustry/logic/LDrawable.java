package mindustry.logic;

import arc.struct.*;

public interface LDrawable {
    boolean drawable(LExecutor exec);
    void draw(LongSeq buffer);
}
