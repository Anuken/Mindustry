package io.anuke.mindustry.game;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.resource.Weapon;

public class UpgradeInventory {
    private final Array<Weapon> weapons = new Array<>();

    public boolean hasWeapon(Weapon weapon){
        return weapons.contains(weapon, true);
    }

    public void addWeapon(Weapon weapon){
        weapons.add(weapon);
    }

    public Array<Weapon> getWeapons(){
        return weapons;
    }

    public void reset(){
        weapons.clear();
        weapons.add(Weapon.blaster);
    }
}
