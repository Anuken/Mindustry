package io.anuke.mindustry.maps;

import com.badlogic.gdx.graphics.Texture;

public class Sector{
    /**Position on the map, can be positive or negative.*/
    public short x, y;
    /**Whether this sector has already been captured. TODO statistics?*/
    public boolean unlocked;
    /**Display texture. Needs to be disposed.*/
    public transient Texture texture;
}
