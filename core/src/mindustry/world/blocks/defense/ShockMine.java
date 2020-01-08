package mindustry.world.blocks.defense;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import mindustry.entities.effect.Lightning;
import mindustry.entities.type.Unit;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.Tile;

public class ShockMine extends Block{
    public final int timerDamage = timers++;

    public float cooldown = 80f;
    public float tileDamage = 5f;
    public float damage = 13;
    public int length = 10;
    public int tendrils = 6;

    public ShockMine(String name){
        super(name);
        update = false;
        destructible = true;
        solid = false;
        targetable = false;
        layer = Layer.overlay;
        rebuildable = false;
    }

    @Override
    public void drawLayer(Tile tile){
        super.draw(tile);
        Draw.color(tile.getTeam().color);
        Draw.alpha(0.22f);
        Fill.rect(tile.drawx(), tile.drawy(), 2f, 2f);
        Draw.color();
    }

    @Override
    public void drawTeam(Tile tile){
        //no
    }

    @Override
    public void draw(Tile tile){
        //nope
    }

    @Override
    public void unitOn(Tile tile, Unit unit){
        if(unit.getTeam() != tile.getTeam() && tile.entity.timer.get(timerDamage, cooldown)){
            for(int i = 0; i < tendrils; i++){
                Lightning.create(tile.getTeam(), Pal.lancerLaser, damage, tile.drawx(), tile.drawy(), Mathf.random(360f), length);
            }
            tile.entity.damage(tileDamage);
        }
    }
}
