package io.anuke.mindustry.editor;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.function.Supplier;
import io.anuke.arc.graphics.Pixmap;
import io.anuke.arc.graphics.Pixmap.Format;
import io.anuke.arc.graphics.Texture;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Scaling;
import io.anuke.arc.util.async.AsyncExecutor;
import io.anuke.arc.util.async.AsyncResult;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.editor.generation.*;
import io.anuke.mindustry.editor.generation.GenerateFilter.GenerateInput;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.io.MapIO;
import io.anuke.mindustry.ui.BorderImage;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;

import static io.anuke.mindustry.Vars.mobile;

@SuppressWarnings("unchecked")
public class MapGenerateDialog extends FloatingDialog{
    private final MapEditor editor;
    private Pixmap pixmap;
    private Texture texture;
    private GenerateInput input = new GenerateInput();
    private Array<GenerateFilter> filters = new Array<>();
    private int scaling = mobile ? 3 : 1;
    private Supplier<GenerateFilter>[] filterTypes = new Supplier[]{NoiseFilter::new, ScatterFilter::new, TerrainFilter::new, DistortFilter::new, RiverNoiseFilter::new};
    private Table filterTable;

    private AsyncExecutor executor = new AsyncExecutor(1);
    private AsyncResult<Void> result;
    private boolean generating;
    private DummyTile returnTile = new DummyTile();

    private DummyTile[][] buffer1, buffer2;

    public MapGenerateDialog(MapEditor editor){
        super("$editor.generate");
        this.editor = editor;

        shown(this::setup);
        addCloseButton();
        buttons.addButton("$editor.apply", () -> {
            apply();
            hide();
        }).size(180f, 64f);
        buttons.addButton("$editor.randomize", () -> {
            for(GenerateFilter filter : filters){
                filter.randomize();
            }
            update();
        }).size(180f, 64f);
    }

    void setup(){
        filters.clear();

        if(pixmap != null){
            pixmap.dispose();
            texture.dispose();
            pixmap = null;
            texture = null;
        }

        pixmap = new Pixmap(editor.width() / scaling, editor.height() / scaling, Format.RGBA8888);
        texture = new Texture(pixmap);

        cont.clear();
        cont.table("flat", t -> {
            t.margin(8f);
            t.add(new BorderImage(texture)).grow().padRight(6).top().get().setScaling(Scaling.fit);
            t.table(right -> {
                right.pane(p -> filterTable = p).grow().get().setScrollingDisabled(true, false);
                right.row();
                right.addButton("$add", this::showAdd).fillX().height(50f);
            }).grow();
        }).grow();

        update();

        buffer1 = create();
        buffer2 = create();
    }

    DummyTile[][] create(){
        DummyTile[][] out = new DummyTile[editor.width() / scaling][editor.height() / scaling];

        for(int x = 0; x < out.length; x++){
            for(int y = 0; y < out[0].length; y++){
                out[x][y] = new DummyTile();
            }
        }
        return out;
    }

    void rebuildFilters(){
        filterTable.clearChildren();
        filterTable.top();

        for(GenerateFilter filter : filters){
            filterTable.table(t -> {
                t.add(filter.name()).padTop(5).color(Pal.accent).growX().left();

                t.row();

                t.table(b -> {
                    b.left();
                    b.defaults().size(50f);
                    b.addImageButton("icon-refresh", 14*2, () -> {
                        filter.randomize();
                        update();
                    });

                    b.addImageButton("icon-arrow-up", 10*2, () -> {
                        int idx = filters.indexOf(filter);
                        filters.swap(idx, Math.max(0, idx - 1));
                        rebuildFilters();
                        update();
                    });
                    b.addImageButton("icon-arrow-down", 10*2, () -> {
                        int idx = filters.indexOf(filter);
                        filters.swap(idx, Math.min(filters.size-1, idx + 1));
                        rebuildFilters();
                        update();
                    });
                    b.addImageButton("icon-trash", 14*2, () -> {
                        filters.remove(filter);
                        rebuildFilters();
                        update();
                    });
                }).growX();
            }).fillX();
            filterTable.row();
            filterTable.table("underline", f -> {
                f.left();
                for(FilterOption option : filter.options){
                    option.changed = this::update;

                    f.table(t -> {
                        t.left();
                        option.build(t);
                    }).growX().left();
                    f.row();
                }
            }).pad(3).padTop(0).width(280f);
            filterTable.row();
        }
    }

    void showAdd(){
        FloatingDialog selection = new FloatingDialog("");
        selection.setFillParent(false);
        selection.cont.defaults().size(210f, 60f);
        for(Supplier<GenerateFilter> gen : filterTypes){
            GenerateFilter filter = gen.get();
            selection.cont.addButton(filter.name(), () -> {
                filters.add(filter);
                rebuildFilters();
                update();
                selection.hide();
            });
            selection.cont.row();
        }

        selection.addCloseButton();
        selection.show();
    }

    DummyTile dset(Tile tile){
        returnTile.set(tile);
        return returnTile;
    }

    void apply(){
        if(result != null){
            result.get();
        }

        //writeback buffer
        DummyTile[][] writeTiles = new DummyTile[editor.width()][editor.height()];

        for(int x = 0; x < editor.width(); x++){
            for(int y = 0; y < editor.height(); y++){
                writeTiles[x][y] = new DummyTile();
            }
        }

        for(GenerateFilter filter : filters){
            input.setFilter(filter, (x, y) -> dset(editor.tile(x, y)));
            //write to buffer
            for(int x = 0; x < editor.width(); x++){
                for(int y = 0; y < editor.height(); y++){
                    Tile tile = editor.tile(x, y);
                    input.begin(editor, x, y, tile.floor(), tile.block(), tile.ore());
                    filter.apply(input);
                    writeTiles[x][y].set(input.floor, input.block, input.ore, tile.getTeam(), tile.getRotation());
                }
            }

            editor.load(() -> {
                //read from buffer back into tiles
                for(int x = 0; x < editor.width(); x++){
                    for(int y = 0; y < editor.height(); y++){
                        Tile tile = editor.tile(x, y);
                        DummyTile write = writeTiles[x][y];

                        tile.setRotation((byte)write.rotation);
                        tile.setFloor((Floor)write.floor);
                        tile.setBlock(write.block);
                        tile.setTeam(write.team);
                        tile.setOre(write.ore);
                    }
                }
            });
        }

        //reset undo stack as generation... messes things up
        editor.load(editor::checkLinkedTiles);
        editor.renderer().updateAll();
        editor.clearOp();
    }

    void update(){

        if(generating){
            return;
        }

        Array<GenerateFilter> copy = new Array<>(filters);

        result = executor.submit(() -> {
            generating = true;

            if(!filters.isEmpty()){
                //write to buffer1 for reading
                for(int px = 0; px < pixmap.getWidth(); px++){
                    for(int py = 0; py < pixmap.getHeight(); py++){
                        buffer1[px][py].set(editor.tile(px * scaling, py * scaling));
                    }
                }
            }

            for(GenerateFilter filter : copy){
                input.setFilter(filter, (x, y) -> buffer1[x][y]);
                //read from buffer1 and write to buffer2
                for(int px = 0; px < pixmap.getWidth(); px++){
                    for(int py = 0; py < pixmap.getHeight(); py++){
                        int x = px*scaling, y = py*scaling;
                        DummyTile tile = buffer1[px][py];
                        input.begin(editor, x, y, tile.floor, tile.block, tile.ore);
                        filter.apply(input);
                        buffer2[px][py].set(input.floor, input.block, input.ore, tile.team, tile.rotation);
                    }
                }
                for(int px = 0; px < pixmap.getWidth(); px++){
                    for(int py = 0; py < pixmap.getHeight(); py++){
                        buffer1[px][py].set(buffer2[px][py]);
                    }
                }
            }

            for(int px = 0; px < pixmap.getWidth(); px++){
                for(int py = 0; py < pixmap.getHeight(); py++){
                    int color;
                    //get result from buffer1 if there's filters left, otherwise get from editor directly
                    if(filters.isEmpty()){
                        Tile tile = editor.tile(px * scaling, py * scaling);
                        color = MapIO.colorFor(tile.floor(), tile.block(), tile.ore(), Team.none);
                    }else{
                        DummyTile tile = buffer1[px][py];
                        color = MapIO.colorFor(tile.floor, tile.block, tile.ore, Team.none);
                    }
                    pixmap.drawPixel(px, pixmap.getHeight() - 1 - py, color);
                }
            }

            Core.app.post(() -> {
                texture.draw(pixmap, 0, 0);
                generating = false;
            });
            return null;
        });
    }

    public static class DummyTile{
        public Block block = Blocks.air, ore = Blocks.air, floor = Blocks.air;
        public Team team = Team.none;
        public int rotation;

        void set(Block floor, Block wall, Block ore, Team team, int rotation){
            this.floor = floor;
            this.block = wall;
            this.ore = ore;
            this.team = team;
            this.rotation = rotation;
        }

        void set(DummyTile other){
            set(other.floor, other.block, other.ore, other.team, other.rotation);
        }

        void set(Tile other){
            set(other.floor(), other.block(), other.ore(), other.getTeam(), other.getRotation());
        }

    }
}
