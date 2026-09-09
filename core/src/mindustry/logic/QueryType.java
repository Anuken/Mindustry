package mindustry.logic;

public enum QueryType{
    unit, building, bullet;

    //TODO: bullets are bugged due to stale references w/ pooling and thus not query-able.
    public static final QueryType[] queryable = {unit, building};
}
