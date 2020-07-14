package mindustry.world.blocks.storage;

import arc.*;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars.*;
import mindustry.content.Fx;
import mindustry.entities.Effects;
import mindustry.entities.type.TileEntity;
import mindustry.type.Item;
import mindustry.type.ItemType;
import mindustry.world.Tile;
import mindustry.world.meta.BlockStat;
import mindustry.world.meta.StatUnit;


public class ItemTeleport extends Vault{
    
    public float tranSpeed;
    public static final Entity core = Vars.state.teams.closestCore(tile.drawx(), tile.drawy(), tile.getTeam());

    public ItemTeleport(String name){
        super(name);
        solid = true;
        update = true;
        destructible = true;
    };
    
    public boolean acceptItem(Item item, Tile tile, Tile source){
        
        return item.type == ItemType.material && !tile.entity.items.has(item, itemCapacity);
        
    };
    public void setStats(){
        
        super.setStats();

        stats.add(BlockStat.itemsMoved, (int)(60 / tranSpeed), StatUnit.itemsSecond);
        
    };
    public static getItem(Entity core,Tile other,Tile tile){
        
        Item item1 = tile.entity.items.first();
        if(item1 != null){
            
        if(core.items.has(item1, other.block().itemCapacity)){
            
            return tile.entity.items.take();
            
        };
        else{
            
            return tile.entity.items.first();
            
        };
        
        };
        else{
            return null;
        };
        
    };
    public void update(Tile tile){
        
        Tile other = Vars.world.tileWorld(core.getX(), core.getY());
        Item item = getItem(core, other, tile);
        
        if(item != null && core != null && tile.entity.power.status >= 1 && !core.items.has(item, other.block().itemCapacity) && tile.entity.timer.get(tranSpeed / Time.delta())){
            
            if(Mathf.chance(tile.entity.healthf())){
            
            core.items.add(item, 1);
            };
            tile.entity.items.remove(item, 1);
            if(Mathf.chance(0.2)){
                Effects.effect(Fx.transSmoke, tile);
            };
            
        };
        
    };
    public void onDestroyed(Tile tile){
                
        if(tile.entity.power.status >= 1){
            
        Effects.effect(Fx.transExplode, tile);
        Damage.damage(null, tile.drawx(), tile.drawy(), 30, 99999999, true); 
        Effects.effect(Fx.bigShockwave, tile);
        Sounds.explosion.at(tile.worldx(), tile.worldy(), 1);
        
        };
        else{
            
            super.onDestroyed(Tile tile);
        };
    };
    public void drawSelect(Tile tile){
                
        super.drawSelect(Tile tile);
        
        Drawf.circles(core.getX(), core.getY(), other.block().size*8);
        Drawf.circles(tile.drawx(), tile.drawy(), this.size*8);
        Drawf.arrow(tile.drawx(), tile.drawy(), core.getX(), core.getY(), this.size*8, 3);
        
    };
}