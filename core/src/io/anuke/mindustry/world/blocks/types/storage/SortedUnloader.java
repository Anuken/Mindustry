package io.anuke.mindustry.world.blocks.types.storage;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.style.TextureRegionDrawable;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.layout.Table;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SortedUnloader extends Unloader {

    public SortedUnloader(String name){
        super(name);
    }

    @Override
    public void update(Tile tile){
        SortedUnloaderEntity entity = tile.entity();

        if(entity.items.totalItems() == 0 && entity.timer.get(timerUnload, speed)){
            tile.allNearby(other -> {
                if(other.block() instanceof StorageBlock && entity.items.totalItems() == 0 &&
                        ((StorageBlock)other.block()).hasItem(other, entity.sortItem)){
                    offloadNear(tile, ((StorageBlock)other.block()).removeItem(other, entity.sortItem));
                }
            });
        }

        if(entity.items.totalItems() > 0){
            tryDump(tile);
        }
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        SortedUnloaderEntity entity = tile.entity();

        Draw.color(entity.sortItem.color);
        Draw.rect("blank", tile.worldx(), tile.worldy(), 2f, 2f);
        Draw.color();
    }

    @Override
    public boolean isConfigurable(Tile tile){
        return true;
    }

    @Override
    public void configure(Tile tile, byte data) {
        SortedUnloaderEntity entity = tile.entity();
        if(entity != null){
            entity.sortItem = Item.getByID(data);
            entity.items.clear();
        }
    }

    @Override
    public void buildTable(Tile tile, Table table){
        SortedUnloaderEntity entity = tile.entity();

        Array<Item> items = Item.all();

        ButtonGroup<ImageButton> group = new ButtonGroup<>();
        Table cont = new Table();
        cont.margin(4);
        cont.marginBottom(5);

        cont.add().colspan(4).height(50f * (int)(items.size/4f + 1f));
        cont.row();

        for(int i = 0; i < items.size; i ++){

            final int f = i;
            ImageButton button = cont.addImageButton("white", "toggle", 24, () -> {
                setConfigure(tile, (byte)f);
            }).size(38, 42).padBottom(-5.1f).group(group).get();
            button.getStyle().imageUp = new TextureRegionDrawable(new TextureRegion(items.get(i).region));
            button.setChecked(entity.sortItem.id == f);

            if(i%4 == 3){
                cont.row();
            }
        }

        table.add(cont);
    }

    @Override
    public TileEntity getEntity(){
        return new SortedUnloaderEntity();
    }

    public static class SortedUnloaderEntity extends TileEntity{
        public Item sortItem = Items.iron;

        @Override
        public void write(DataOutputStream stream) throws IOException {
            stream.writeByte(sortItem.id);
        }

        @Override
        public void read(DataInputStream stream) throws IOException{
            sortItem = Item.all().get(stream.readByte());
        }
    }
}
