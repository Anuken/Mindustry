package mindustry.logic;

import arc.graphics.*;

public enum LCategory{
    blocks(Color.valueOf("d4816b")),
    control(Color.valueOf("6bb2b2")),
    operations(Color.valueOf("877bad")),
    io(Color.valueOf("a08a8a")),
    units(Color.valueOf("c7b59d"));

    public final Color color;

    LCategory(Color color){
        this.color = color;
    }
}
