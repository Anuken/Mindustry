package io.anuke.mindustry.content.blocks;

import com.badlogic.gdx.utils.Array;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.PowerBlock;
import io.anuke.mindustry.world.blocks.distribution.Sorter;
import io.anuke.mindustry.world.blocks.power.PowerNode;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.layout.Table;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import static io.anuke.mindustry.Vars.*;

public class DebugBlocks extends BlockList implements ContentList{
    public static Block powerVoid, powerInfinite, itemSource, liquidSource, itemVoid;

    @Remote(targets = Loc.both, called = Loc.both, forward = true)
    public static void setLiquidSourceLiquid(Player player, Tile tile, Liquid liquid){
        LiquidSourceEntity entity = tile.entity();
        entity.source = liquid;
    }

    @Override
    public void load(){
        powerVoid = new PowerBlock("powervoid"){
            {
                consumes.powerDirect(Float.MAX_VALUE);
                shadow = "shadow-round-1";
            }

            @Override
            public void setBars(){
                super.setBars();
                bars.remove(BarType.power);
            }

            @Override
            public void init(){
                super.init();
                stats.remove(BlockStat.powerUse);
            }
        };

        powerInfinite = new PowerNode("powerinfinite"){
            {
                maxNodes = 100;
                outputsPower = true;
                consumesPower = false;
                shadow = "shadow-round-1";
            }

            @Override
            public float getPowerProduction(Tile tile){
                return 10000f;
            }
        };

        itemSource = new Sorter("itemsource"){
            {
                hasItems = true;
            }

            @Override
            public boolean outputsItems(){
                return true;
            }

            @Override
            public void setBars(){
                super.setBars();
                bars.remove(BarType.inventory);
            }

            @Override
            public void update(Tile tile){
                SorterEntity entity = tile.entity();
                entity.items.set(entity.sortItem, 1);
                tryDump(tile, entity.sortItem);
            }

            @Override
            public boolean acceptItem(Item item, Tile tile, Tile source){
                return false;
            }
        };

        liquidSource = new Block("liquidsource"){
            {
                update = true;
                solid = true;
                hasLiquids = true;
                liquidCapacity = 100f;
                configurable = true;
                outputsLiquid = true;
            }

            @Override
            public void update(Tile tile){
                LiquidSourceEntity entity = tile.entity();

                tile.entity.liquids.add(entity.source, liquidCapacity);
                tryDumpLiquid(tile, entity.source);
            }

            @Override
            public void draw(Tile tile){
                super.draw(tile);

                LiquidSourceEntity entity = tile.entity();

                Draw.color(entity.source.color);
                Draw.rect("blank", tile.worldx(), tile.worldy(), 4f, 4f);
                Draw.color();
            }

            @Override
            public void buildTable(Tile tile, Table table){
                LiquidSourceEntity entity = tile.entity();

                Array<Liquid> items = content.liquids();

                ButtonGroup<ImageButton> group = new ButtonGroup<>();
                Table cont = new Table();

                for(int i = 0; i < items.size; i++){
                    if(!control.unlocks.isUnlocked(items.get(i))) continue;

                    final int f = i;
                    ImageButton button = cont.addImageButton("liquid-icon-" + items.get(i).name, "toggle", 24,
                            () -> Call.setLiquidSourceLiquid(null, tile, items.get(f))).size(38, 42).padBottom(-5.1f).group(group).get();
                    button.setChecked(entity.source.id == f);

                    if(i % 4 == 3){
                        cont.row();
                    }
                }

                table.add(cont);
            }

            @Override
            public TileEntity newEntity(){
                return new LiquidSourceEntity();
            }
        };

        itemVoid = new Block("itemvoid"){
            {
                update = solid = true;
            }

            @Override
            public void handleItem(Item item, Tile tile, Tile source){
            }

            @Override
            public boolean acceptItem(Item item, Tile tile, Tile source){
                return true;
            }
        };
    }

    class LiquidSourceEntity extends TileEntity{
        public Liquid source = Liquids.water;

        @Override
        public void writeConfig(DataOutput stream) throws IOException{
            stream.writeByte(source.id);
        }

        @Override
        public void readConfig(DataInput stream) throws IOException{
            source = content.liquid(stream.readByte());
        }
    }
}
