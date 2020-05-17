package mindustry.ctype;

/** Do not rearrange, ever! */
public enum ContentType{
    item,
    block,
    mech_UNUSED,
    bullet,
    liquid,
    status,
    unit,
    weather,
    effect_UNUSED,
    sector,
    loadout_UNUSED,
    typeid_UNUSED,
    error,
    planet;

    public static final ContentType[] all = values();
}
