package io.anuke.mindustry.game;

import java.util.*;

/** Defines sizes of a content's preview icon. */
public enum Cicon{
    /** Full size. */
    full(0),
    tiny(8 * 2),
    small(8 * 3),
    medium(8 * 4),
    large(8 * 5),
    xlarge(8 * 6);

    public static final Cicon[] all = values();
    public static final Cicon[] scaled = Arrays.copyOfRange(all, 1, all.length);

    public final int size;

    Cicon(int size){
        this.size = size;
    }
}
