package mindustry.ui;

/** Use content icon fields directly instead. This will be removed. */
@Deprecated
public enum Cicon{
    tiny, small, medium, large, xlarge, full;
    @Deprecated
    public final int size = 32;

    public static final Cicon[] all = values();
    public static final Cicon[] scaled = values();
}
