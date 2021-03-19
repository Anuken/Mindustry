package mindustry.world.blocks.units;

import arc.func.*;
import arc.graphics.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.AmmoTypes.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ResupplyPoint extends Block{
    public final int timerResupply = timers++;

    public int ammoAmount = 10;
    public float resupplyRate = 5f;
    public float range = 60f;
    public Color ammoColor = Items.copper.color;

    public ResupplyPoint(String name){
        super(name);
        solid = update = true;
        hasItems = true;
        flags = EnumSet.of(BlockFlag.resupply);
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, Pal.placing);
    }

    public class ResupplyPointBuild extends Building{

        @Override
        public void drawSelect(){
            Drawf.dashCircle(x, y, range, team.color);
        }

        @Override
        public void updateTile(){
            if(consValid() && timer(timerResupply, resupplyRate / timeScale) && resupply(this, range, ammoAmount, ammoColor)){
                consume();
            }
        }
    }

    /** Tries to resupply nearby units.
     * @return whether resupplying was successful. If unit ammo is disabled, always returns false. */
    public static boolean resupply(Building tile, float range, float ammoAmount, Color ammoColor){
        return resupply(tile.team, tile.x, tile.y, range, ammoAmount, ammoColor, u -> true);
    }

    /** Tries to resupply nearby units.
     * @return whether resupplying was successful. If unit ammo is disabled, always returns false. */
    public static boolean resupply(Team team, float x, float y, float range, float ammoAmount, Color ammoColor, Boolf<Unit> valid){
        if(!state.rules.unitAmmo) return false;

        Unit unit = Units.closest(team, x, y, range, u -> u.type.ammoType instanceof ItemAmmoType && u.ammo <= u.type.ammoCapacity - ammoAmount && valid.get(u));
        if(unit != null){
            Fx.itemTransfer.at(x, y, ammoAmount / 2f, ammoColor, unit);
            unit.ammo = Math.min(unit.ammo + ammoAmount, unit.type.ammoCapacity);
            return true;
        }

        return false;
    }
}
