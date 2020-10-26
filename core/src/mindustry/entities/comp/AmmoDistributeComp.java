package mindustry.entities.comp;

import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.units.*;

@Component
abstract class AmmoDistributeComp implements Unitc{
    @Import float x, y;
    @Import UnitType type;
    @Import Team team;
    @Import float ammo;

    private transient float ammoCooldown;

    @Override
    public void update(){
        if(ammoCooldown > 0f) ammoCooldown -= Time.delta;

        if(ammo > 0 && ammoCooldown <= 0f && ResupplyPoint.resupply(team, x, y, type.ammoResupplyRange, Math.min(type.ammoResupplyAmount, ammo), type.ammoType.color, u -> u != self())){
            ammo -= Math.min(type.ammoResupplyAmount, ammo);
            ammoCooldown = 5f;
        }
    }
}
