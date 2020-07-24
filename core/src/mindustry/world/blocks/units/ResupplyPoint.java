package mindustry.world.blocks.units;

import arc.graphics.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

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
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, Pal.placing);
    }

    public class ResupplyPointEntity extends Building{

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
    public static boolean resupply(Building tile, float range, int ammoAmount, Color ammoColor){
        if(!state.rules.unitAmmo) return false;

        Unit unit = Units.closest(tile.team, tile.x, tile.y, range, u -> u.ammo() <= u.type().ammoCapacity - ammoAmount);
        if(unit != null){
            Fx.itemTransfer.at(tile.x, tile.y, ammoAmount / 2f, ammoColor, unit);
            unit.ammo(Math.min(unit.ammo() + ammoAmount, unit.type().ammoCapacity));
            return true;
        }

        return false;
    }
}
