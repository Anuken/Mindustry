package mindustry.ctype;

/** Do not rearrange, ever! */
public enum ContentType{
    item,
    block,
    mech,
    bullet,
    liquid,
    status,
    unit,
    weather,
    effect_UNUSED,
    zone,
    loadout_UNUSED,
    typeid_UNUSED,
    error,
    planet;

    public static final ContentType[] all = values();
}
