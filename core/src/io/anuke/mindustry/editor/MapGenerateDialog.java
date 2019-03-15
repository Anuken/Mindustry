package io.anuke.mindustry.editor;

import io.anuke.arc.collection.Array;
import io.anuke.arc.graphics.Pixmap;
import io.anuke.arc.graphics.Pixmap.Format;
import io.anuke.arc.graphics.Texture;
import io.anuke.arc.scene.Element;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.editor.generation.FilterOption;
import io.anuke.mindustry.editor.generation.GenerateFilter;
import io.anuke.mindustry.editor.generation.GenerateFilter.GenerateInput;
import io.anuke.mindustry.editor.generation.NoiseFilter;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.MapIO;
import io.anuke.mindustry.ui.BorderImage;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;
import io.anuke.mindustry.world.DummyTile;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;

public class MapGenerateDialog extends FloatingDialog{
    private final MapEditor editor;
    private Pixmap pixmap;
    private Texture texture;
    private GenerateInput input = new GenerateInput();
    private Array<GenerateFilter> filters = new Array<>();

    public MapGenerateDialog(MapEditor editor){
        super("editor.generate");
        this.editor = editor;

        shown(this::setup);
        addCloseButton();
        buttons.addButton("$editor.apply", () -> {
            apply();
            hide();
        }).size(180f, 64f);
    }

    void setup(){
        if(pixmap != null){
            pixmap.dispose();
            texture.dispose();
            pixmap = null;
            texture = null;
        }

        pixmap = new Pixmap(editor.width(), editor.height(), Format.RGBA8888);
        texture = new Texture(pixmap);

        cont.clear();
        cont.table("flat", t -> {
            t.margin(8f);
            t.add(new BorderImage(texture)).size(500f).padRight(6);
            t.table(right -> {
                Table[] fs = {null};
                Runnable rebuild = () -> {
                    Table p = fs[0];
                    p.clearChildren();

                    for(GenerateFilter filter : filters){
                        p.table("button", f -> {
                            f.left();
                            for(FilterOption option : filter.options){
                                option.build(f);
                                f.row();
                            }
                            for(Element e : f.getChildren()){
                                e.changed(this::update);
                            }
                        }).pad(3).width(280f);
                        p.row();
                    }
                };

                right.pane(p -> {
                    p.top();
                    fs[0] = p;
                }).fill().width(300f).growY().get().setScrollingDisabled(true, false);
                right.row();
                right.addButton("$add", () -> {
                    filters.add(new NoiseFilter());
                    rebuild.run();
                }).fillX().height(50f);
            }).grow();

            t.row();
            t.addButton("$editor.update", () -> {
                input.randomize();
                update();
            }).size(300f, 66f).colspan(2).pad(6);
        });

        update();
    }

    void apply(){
        Tile[][] writeTiles = new Tile[editor.width()][editor.height()];

        for(int x = 0; x < editor.width(); x++){
            for(int y = 0; y < editor.height(); y++){
                writeTiles[x][y] = new DummyTile(x, y);
            }
        }

        for(GenerateFilter filter : filters){
            for(int x = 0; x < editor.width(); x++){
                for(int y = 0; y < editor.height(); y++){
                    Tile tile = editor.tile(x, y);
                    Tile write = writeTiles[x][y];
                    input.begin(editor, x, y, tile.floor(), tile.block(), tile.ore());
                    filter.apply(input);
                    write.setRotation(tile.getRotation());
                    write.setOre(input.ore);
                    write.setFloor((Floor)input.floor);
                    write.setBlock(input.block);
                    write.setTeam(tile.getTeam());
                }
            }

            editor.load(() -> {
                for(int x = 0; x < editor.width(); x++){
                    for(int y = 0; y < editor.height(); y++){
                        Tile tile = editor.tile(x, y);
                        Tile write = writeTiles[x][y];

                        tile.setRotation(write.getRotation());
                        tile.setOre(write.ore());
                        tile.setFloor(write.floor());
                        tile.setBlock(write.block());
                        tile.setTeam(write.getTeam());
                    }
                }
            });
        }

        editor.renderer().updateAll();
        editor.clearOp();
    }

    void update(){

        boolean modified = false;
        for(GenerateFilter filter : filters){
            modified = true;
            for(int x = 0; x < editor.width(); x++){
                for(int y = 0; y < editor.height(); y++){
                    Tile tile = editor.tile(x, y);
                    input.begin(editor, x, y, tile.floor(), tile.block(), tile.ore());
                    filter.apply(input);
                    pixmap.drawPixel(x, pixmap.getHeight() - 1 - y, MapIO.colorFor(input.floor, input.block, input.ore, Team.none));
                }
            }
        }

        if(!modified){
            for(int x = 0; x < editor.width(); x++){
                for(int y = 0; y < editor.height(); y++){
                    Tile tile = editor.tile(x, y);
                    pixmap.drawPixel(x, pixmap.getHeight() - 1 - y, MapIO.colorFor(tile.floor(), tile.block(), tile.ore(), Team.none));
                }
            }
        }

        texture.draw(pixmap, 0, 0);
    }
}
