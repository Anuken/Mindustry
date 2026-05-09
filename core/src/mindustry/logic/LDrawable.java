package mindustry.logic;

import arc.struct.LongSeq;

public interface LDrawable {
    boolean drawable(LExecutor exec);
    void draw(LongSeq buffer);
}
