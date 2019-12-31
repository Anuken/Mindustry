package mindustry.world.blocks.distribution;

import arc.*;
import arc.func.*;
import arc.struct.*;
import mindustry.type.*;
import mindustry.world.*;
import arc.graphics.g2d.*;
import mindustry.content.*;
import mindustry.entities.type.*;
import mindustry.game.EventType.*;
import mindustry.entities.type.base.*;

public class CompressedConveyor extends ArmoredConveyor{
    protected TextureRegion start;
    public TextureRegion end;

    protected static int cooldown = 4; // ticks it needs to wait with spawning when a ground unit has walked on it

    public CompressedConveyor(String name){
        super(name);
        compressable = true;
        entityType = CompressedConveyorEntity::new;
    }

    @Override
    public void load(){
        int i;
        for(i = 0; i < regions.length; i++){
            for(int j = 0; j < 4; j++){
                regions[i][j] = Core.atlas.find(name + "-" + i + "-" + 0);
            }
        }

        start = Core.atlas.find(name + "-5-0");
        end   = Core.atlas.find(name + "-6-0");
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        // draws the markings over either end of the track
        if(Track.start.check.get(tile) && Track.end.check.get(tile)) return;
        if(Track.start.check.get(tile))  Draw.rect(start, tile.drawx(), tile.drawy(), tile.rotation() * 90);
        if(Track.end.check.get(tile))      Draw.rect(end, tile.drawx(), tile.drawy(), tile.rotation() * 90);
    }

    @Override
    public void unitOn(Tile tile, Unit unit){ // resets the spawner cooldown, as well as adopting stray roomba's
        CompressedConveyorEntity entity = tile.ent();
        if(unit instanceof CraterUnit) entity.crater = (CraterUnit)unit;
        entity.reload = cooldown;
    }

    @Override
    public void update(Tile tile){ // tick away the cooldown
        CompressedConveyorEntity entity = tile.ent();
        if(entity.reload > 0) entity.reload--;
    }

    class CompressedConveyorEntity extends ConveyorEntity{
        public int reload = 0;
        public CraterUnit crater = null;
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){ // summon craters into existence to be loaded
        CompressedConveyorEntity entity = tile.ent();

        if(!Track.start.check.get(tile)) return false;
        if(entity.crater == null || entity.crater.dead || !entity.crater.loading() || entity.crater.on() != tile){
            if(entity.reload > 0) return false;
            entity.reload = cooldown;
            entity.crater = CraterUnit.on(tile);
        }

        return entity.crater.acceptItem(item);
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        ((CompressedConveyorEntity)tile.ent()).crater.handleItem(item);
    }

    @Override
    public int acceptStack(Item item, int amount, Tile tile, Unit source){
        return 0;
    }

    @Override
    public void handleStack(Item item, int amount, Tile tile, Unit source){
        //
    }

    @Override
    public boolean blendsArmored(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){ // only connect to compressable blocks
        return super.blendsArmored(tile, rotation, otherx, othery, otherrot, otherblock) && otherblock.compressable;
    }

    public enum Track{
        // tile is considered the end of the line
        end(tile -> {
            if(tile.front() == null) return true;
            if(tile.getTeam() != tile.front().getTeam()) return true; // comment out to trade
            return !tile.front().block().compressable;
        }),

        // tile is considered the start of the line
        start(tile -> {
            Tile[] inputs = new Tile[]{tile.back(), tile.left(), tile.right()};
            for(Tile input : inputs){
                if(input != null && input.getTeam() == tile.getTeam() && input.block().compressable && input.front() == tile) return false;
            }

            return true;
        });

        public final Boolf<Tile> check;

        Track(Boolf<Tile> check){
            this.check = check;
        }

        public static ObjectMap<Tile, CraterUnit> dibs = new ObjectMap<>();
    }
}
