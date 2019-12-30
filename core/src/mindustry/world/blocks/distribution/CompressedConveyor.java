package mindustry.world.blocks.distribution;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.content.*;
import mindustry.entities.type.*;
import mindustry.entities.type.base.*;
import mindustry.game.EventType.*;
import mindustry.type.*;
import mindustry.world.*;

public class CompressedConveyor extends ArmoredConveyor{
    protected TextureRegion start;
    public TextureRegion end;

    protected static int cooldown = 10;

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

        if(start(tile) && end(tile)) return;
        if(start(tile)) Draw.rect(start, tile.drawx(), tile.drawy(), tile.rotation() * 90);
        if(  end(tile)) Draw.rect(  end, tile.drawx(), tile.drawy(), tile.rotation() * 90);
    }

    protected boolean start(Tile tile){
        Tile[] inputs = new Tile[]{tile.back(), tile.left(), tile.right()};
        for(Tile input : inputs){
            if(input != null && input.getTeam() == tile.getTeam() && input.block().compressable && input.front() == tile) return false;
        }

        return true;
    }

    public boolean end(Tile tile){
        Tile next = tile.front();
        if(next == null) return true;
        if(next.getTeam() != tile.getTeam()) return true;
        return !next.block().compressable;
    }

    @Override
    public void unitOn(Tile tile, Unit unit){
        CompressedConveyorEntity entity = tile.ent();
        entity.reload = cooldown;
        if(unit instanceof CraterUnit) entity.crater = (CraterUnit)unit;
    }

    @Override
    public void update(Tile tile){
        CompressedConveyorEntity entity = tile.ent();
        if(entity.reload > 0) entity.reload--;
    }

    class CompressedConveyorEntity extends ConveyorEntity{
        public int reload = 0;
        public CraterUnit crater = null;
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        CompressedConveyorEntity entity = tile.ent();

        if(!start(tile)) return false;
        if(entity.crater == null || entity.crater.dead || !entity.crater.loading() || entity.crater.on() != tile){
            if(entity.reload > 0) return false;
            entity.reload = cooldown;
            entity.crater = (CraterUnit)UnitTypes.crater.create(tile.getTeam());
            entity.crater.set(tile.drawx(), tile.drawy());
            entity.crater.rotation = tile.rotation() * 90;
            entity.crater.add();
            Events.fire(new UnitCreateEvent(entity.crater));
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
    public boolean blendsArmored(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return super.blendsArmored(tile, rotation, otherx, othery, otherrot, otherblock) && otherblock.compressable;
    }
}
