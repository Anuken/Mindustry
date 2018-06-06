package io.anuke.mindustry.content.blocks;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.PowerBlock;
import io.anuke.mindustry.world.blocks.distribution.Sorter;
import io.anuke.mindustry.world.blocks.power.PowerDistributor;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.layout.Table;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DebugBlocks extends BlockList implements ContentList{
    public static Block powerVoid, powerInfinite, itemSource, liquidSource, itemVoid;

    @Override
    public void load() {
        powerVoid = new PowerBlock("powervoid") {
            {
                powerCapacity = Float.MAX_VALUE;
            }
        };

        powerInfinite = new PowerDistributor("powerinfinite") {
            {
                powerCapacity = 10000f;
                powerSpeed = 100f;
            }

            @Override
            public void update(Tile tile) {
                super.update(tile);
                tile.entity.power.amount = powerCapacity;
            }
        };

        itemSource = new Sorter("itemsource") {
            @Override
            public void update(Tile tile) {
                SorterEntity entity = tile.entity();
                entity.items.items[entity.sortItem.id] = 1;
                tryDump(tile, entity.sortItem);
            }

            @Override
            public boolean acceptItem(Item item, Tile tile, Tile source) {
                return false;
            }
        };

        liquidSource = new Block("liquidsource") {
            {
                update = true;
                solid = true;
                hasLiquids = true;
                liquidCapacity = 100f;
                configurable = true;
            }

            @Override
            public void update(Tile tile) {
                LiquidSourceEntity entity = tile.entity();

                tile.entity.liquids.amount = liquidCapacity;
                tile.entity.liquids.liquid = entity.source;
                tryDumpLiquid(tile);
            }

            @Override
            public void draw(Tile tile) {
                super.draw(tile);

                LiquidSourceEntity entity = tile.entity();

                Draw.color(entity.source.color);
                Draw.rect("blank", tile.worldx(), tile.worldy(), 4f, 4f);
                Draw.color();
            }

            @Override
            public void buildTable(Tile tile, Table table) {
                LiquidSourceEntity entity = tile.entity();

                Array<Liquid> items = Liquid.all();

                ButtonGroup<ImageButton> group = new ButtonGroup<>();
                Table cont = new Table();
                cont.margin(4);
                cont.marginBottom(5);

                cont.add().colspan(4).height(50f * (int) (items.size / 4f + 1f));
                cont.row();

                for (int i = 0; i < items.size; i++) {
                    if (i == 0) continue;
                    final int f = i;
                    ImageButton button = cont.addImageButton("white", "toggle", 24, () -> {
                        entity.source = items.get(f);
                    }).size(38, 42).padBottom(-5.1f).group(group).get();
                    button.getStyle().imageUpColor = items.get(i).color;
                    button.setChecked(entity.source.id == f);

                    if (i % 4 == 3) {
                        cont.row();
                    }
                }

                table.add(cont);
            }

            @Override
            public TileEntity getEntity() {
                return new LiquidSourceEntity();
            }

            class LiquidSourceEntity extends TileEntity {
                public Liquid source = Liquids.water;

                @Override
                public void write(DataOutputStream stream) throws IOException {
                    stream.writeByte(source.id);
                }

                @Override
                public void read(DataInputStream stream) throws IOException {
                    source = Liquid.getByID(stream.readByte());
                }
            }
        };

        itemVoid = new Block("itemvoid") {
            {
                update = solid = true;
            }

            @Override
            public void handleItem(Item item, Tile tile, Tile source) {
            }

            @Override
            public boolean acceptItem(Item item, Tile tile, Tile source) {
                return true;
            }
        };
    }
}
