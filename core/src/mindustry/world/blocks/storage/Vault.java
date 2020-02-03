package mindustry.world.blocks.storage;

import arc.*;
import mindustry.content.*;
import mindustry.entities.type.*;
import mindustry.world.*;

public class Vault extends StorageBlock{

    public Vault(String name){
        super(name);
        solid = true;
        update = true;
        destructible = true;
    }

    @Override
    public void update(Tile tile){
        super.update(tile);

        if(tile.block == Blocks.container && tile.entity.items.has(Items.surgealloy, 300) && tile.entity.items.total() == 300){
            Core.app.post(() -> {
                BaseUnit spawn = UnitTypes.eradicator.create(tile.getTeam());
                spawn.set(tile.drawx(), tile.drawy());
                tile.entity.kill();
                spawn.add();
            });
        }

        if(tile.block == Blocks.vault && tile.entity.items.has(Items.plastanium, 1000) && tile.entity.items.total() == 1000){
            Core.app.post(() -> {
                BaseUnit spawn = UnitTypes.reaper.create(tile.getTeam());
                spawn.set(tile.drawx(), tile.drawy());
                tile.entity.kill();
                spawn.add();
            });
        }
    }
}
