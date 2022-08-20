package mindustry.logic;

import arc.struct.*;

public enum LProperty{
    reactorHeat,
    storedPower,
    health,
    team,
    flag,
    playerUnit,
    x, y, rotation,
    shootX, shootY, shooting,
    link,
    payload;

    public static final LProperty[] all = values();
    public static final ObjectMap<String, LProperty> allMap = ObjectMap.of();

    static{
        for(LProperty property : all) allMap.put(property.name(), property);
    }

    public static LProperty forName(String key){
        return allMap.get(key);
    }

    public static boolean has(String key){
        return allMap.containsKey(key);
    }
}
