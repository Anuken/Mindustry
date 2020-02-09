package mindustry.maps.filters;

import arc.math.*;
import arc.struct.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;

public class RandomItemFilter extends GenerateFilter{
    public Array<ItemStack> drops = new Array<>();
    public float chance = 0.3f;

    @Override
    public FilterOption[] options(){
        return new FilterOption[0];
    }

    @Override
    public void apply(Tiles tiles, GenerateInput in){
        for(Tile tile : tiles){
            if(tile.block() instanceof StorageBlock && !(tile.block() instanceof CoreBlock)){
                for(ItemStack stack : drops){
                    if(Mathf.chance(chance)){
                        tile.entity.items().add(stack.item, Math.min(Mathf.random(stack.amount), tile.block().itemCapacity));
                    }
                }
            }
        }
    }

    @Override
    public boolean isPost(){
        return true;
    }
}
