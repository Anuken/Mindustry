package io.anuke.mindustry.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.MapIO;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.maps.MapMeta;
import io.anuke.mindustry.maps.MapTileData;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;
import io.anuke.mindustry.world.Block;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.input.Input;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.scene.utils.UIUtils;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static io.anuke.mindustry.Vars.*;

public class MapEditorDialog extends Dialog implements Disposable{
    private MapEditor editor;
    private MapView view;
    private MapInfoDialog infoDialog;
    private MapLoadDialog loadDialog;
    private MapResizeDialog resizeDialog;
    private ScrollPane pane;
    private FloatingDialog menu;
    private boolean saved = false;
    private boolean shownWithMap = false;

    private ButtonGroup<ImageButton> blockgroup;

    public MapEditorDialog(){
        super("", "dialog");

        background("dark");

        editor = new MapEditor();
        view = new MapView(editor);

        infoDialog = new MapInfoDialog(editor);

        menu = new FloatingDialog("$text.menu");
        menu.addCloseButton();

        float isize = 16 * 2f;
        float swidth = 180f;

        menu.content().table(t -> {
            t.defaults().size(swidth, 60f).padBottom(5).padRight(5).padLeft(5);

            t.addImageTextButton("$text.editor.savemap", "icon-floppy-16", isize, this::save).size(swidth * 2f + 10, 60f).colspan(2);

            t.row();

            t.addImageTextButton("$text.editor.mapinfo", "icon-pencil", isize, () -> {
                infoDialog.show();
                menu.hide();
            });

            t.addImageTextButton("$text.editor.resize", "icon-resize", isize, () -> {
                resizeDialog.show();
                menu.hide();
            });

            t.row();

            t.addImageTextButton("$text.editor.import", "icon-load-map", isize, () ->
                    createDialog("$text.editor.import",
                            "$text.editor.importmap", "$text.editor.importmap.description", "icon-load-map", (Runnable) loadDialog::show,
                            "$text.editor.importfile", "$text.editor.importfile.description", "icon-file", (Runnable) () -> {
                                Platform.instance.showFileChooser("$text.loadimage", "Map Files", file -> {
                                    ui.loadGraphics(() -> {
                                        try{
                                            DataInputStream stream = new DataInputStream(file.read());

                                            MapMeta meta = MapIO.readMapMeta(stream);
                                            MapTileData data = MapIO.readTileData(stream, meta, false);

                                            editor.beginEdit(data, meta.tags, false);
                                            view.clearStack();
                                        }catch(Exception e){
                                            ui.showError(Bundles.format("text.editor.errorimageload", Strings.parseException(e, false)));
                                            Log.err(e);
                                        }
                                    });
                                }, true, mapExtension);
                            },
						"$text.editor.importimage", "$text.editor.importimage.description", "icon-file-image", (Runnable)() -> {
                            Platform.instance.showFileChooser("$text.loadimage", "Image Files", file -> {
                                ui.loadGraphics(() -> {
                                    try{
                                        MapTileData data = MapIO.readLegacyPixmap(new Pixmap(file));

                                        editor.beginEdit(data, editor.getTags(), false);
                                        view.clearStack();
                                    }catch (Exception e){
                                        ui.showError(Bundles.format("text.editor.errorimageload", Strings.parseException(e, false)));
                                        Log.err(e);
                                    }
                                });
                            }, true, "png");
						}));

            t.addImageTextButton("$text.editor.export", "icon-save-map", isize, () -> createDialog("$text.editor.export",
                    "$text.editor.exportfile", "$text.editor.exportfile.description", "icon-file", (Runnable) () -> {
                        Platform.instance.showFileChooser("$text.saveimage", "Map Files", file -> {
                            file = file.parent().child(file.nameWithoutExtension() + "." + mapExtension);
                            FileHandle result = file;
                            ui.loadGraphics(() -> {

                                try{
                                    if(!editor.getTags().containsKey("name")){
                                        editor.getTags().put("name", result.nameWithoutExtension());
                                    }
                                    MapIO.writeMap(result.write(false), editor.getTags(), editor.getMap());
                                }catch(Exception e){
                                    ui.showError(Bundles.format("text.editor.errorimagesave", Strings.parseException(e, false)));
                                    Log.err(e);
                                }
                            });
                        }, false, mapExtension);
                    }));

            t.row();

            t.row();
        });

        menu.content().row();

        menu.content().addImageTextButton("$text.quit", "icon-back", isize, () -> {
            tryExit();
            menu.hide();
        }).padTop(-5).size(swidth * 2f + 10, 60f);

        resizeDialog = new MapResizeDialog(editor, (x, y) -> {
            if(!(editor.getMap().width() == x && editor.getMap().height() == y)){
                ui.loadGraphics(() -> {
                    editor.resize(x, y);
                    view.clearStack();
                });
            }
        });

        loadDialog = new MapLoadDialog(map -> {

            ui.loadGraphics(() -> {
                try(DataInputStream stream = new DataInputStream(map.stream.get())){
                    MapMeta meta = MapIO.readMapMeta(stream);
                    MapTileData data = MapIO.readTileData(stream, meta, false);

                    editor.beginEdit(data, meta.tags, false);
                    view.clearStack();
                }catch(IOException e){
                    ui.showError(Bundles.format("text.editor.errormapload", Strings.parseException(e, false)));
                    Log.err(e);
                }
            });
        });

        setFillParent(true);

        clearChildren();
        margin(0);
        shown(this::build);

        update(() -> {
            if(Core.scene.getKeyboardFocus() instanceof Dialog && Core.scene.getKeyboardFocus() != this){
                return;
            }

            Vector2 v = pane.stageToLocalCoordinates(Graphics.mouse());

            if(v.x >= 0 && v.y >= 0 && v.x <= pane.getWidth() && v.y <= pane.getHeight()){
                Core.scene.setScrollFocus(pane);
            }else{
                Core.scene.setScrollFocus(null);
            }

            if(Core.scene != null && Core.scene.getKeyboardFocus() == this){
                doInput();
            }
        });

        shown(() -> {
            saved = true;
            Platform.instance.beginForceLandscape();
            view.clearStack();
            Core.scene.setScrollFocus(view);
            if(!shownWithMap){
                editor.beginEdit(new MapTileData(256, 256), new ObjectMap<>(), true);
            }
            shownWithMap = false;

            Timers.runTask(10f, Platform.instance::updateRPC);
        });

        hidden(() -> {
            Platform.instance.updateRPC();
            Platform.instance.endForceLandscape();
        });
    }

    @Override
    protected void drawBackground(Batch batch, float parentAlpha, float x, float y){
        drawDefaultBackground(batch, parentAlpha, x, y);
    }

    private void save(){
        String name = editor.getTags().get("name", "");

        if(name.isEmpty()){
            ui.showError("$text.editor.save.noname");
        }else{
            Map map = world.maps.getByName(name);
            if(map != null && !map.custom){
                ui.showError("$text.editor.save.overwrite");
            }else{
                world.maps.saveMap(name, editor.getMap(), editor.getTags());
                ui.showInfoFade("$text.editor.saved");
            }
        }

        menu.hide();
        saved = true;
    }

    /**
     * Argument format:
     * 0) button name
     * 1) description
     * 2) icon name
     * 3) listener
     */
    private FloatingDialog createDialog(String title, Object... arguments){
        FloatingDialog dialog = new FloatingDialog(title);

        float h = 90f;

        dialog.content().defaults().size(360f, h).padBottom(5).padRight(5).padLeft(5);

        for(int i = 0; i < arguments.length; i += 4){
            String name = (String) arguments[i];
            String description = (String) arguments[i + 1];
            String iconname = (String) arguments[i + 2];
            Runnable listenable = (Runnable) arguments[i + 3];

            TextButton button = dialog.content().addButton(name, () -> {
                listenable.run();
                dialog.hide();
                menu.hide();
            }).left().get();

            button.clearChildren();
            button.table("button", t -> {
                t.addImage(iconname).size(16 * 3);
                t.update(() -> t.background(button.getClickListener().isOver() ? "button-over" : "button"));
            }).padLeft(-10).padBottom(-3).size(h);
            button.table(t -> {
                t.add(name).growX().wrap();
                t.row();
                t.add(description).color(Color.GRAY).growX().wrap();
            }).growX().padLeft(8);

            button.row();

            dialog.content().row();
        }

        dialog.addCloseButton();
        dialog.show();

        return dialog;
    }

    @Override
    public Dialog show(){
        return super.show(Core.scene, Actions.sequence(Actions.alpha(0f), Actions.scaleTo(1f, 1f), Actions.fadeIn(0.3f)));
    }

    @Override
    public void dispose(){
        editor.renderer().dispose();
    }

    public void beginEditMap(InputStream is){
        ui.loadGraphics(() -> {
            try{
                shownWithMap = true;
                DataInputStream stream = new DataInputStream(is);
                MapMeta meta = MapIO.readMapMeta(stream);
                editor.beginEdit(MapIO.readTileData(stream, meta, false), meta.tags, false);
                is.close();
                show();
            }catch(Exception e){
                Log.err(e);
                ui.showError(Bundles.format("text.editor.errorimageload", Strings.parseException(e, false)));
            }
        });
    }

    public MapView getView(){
        return view;
    }

    public void resetSaved(){
        saved = false;
    }

    public void updateSelectedBlock(){
        Block block = editor.getDrawBlock();
        for(int j = 0; j < content.blocks().size; j++){
            if(block.id == j && j < blockgroup.getButtons().size){
                blockgroup.getButtons().get(j).setChecked(true);
                break;
            }
        }
    }

    public boolean hasPane(){
        return Core.scene.getScrollFocus() == pane || Core.scene.getKeyboardFocus() != this;
    }

    public void build(){
        float amount = 10f, baseSize = 60f;

        float size = mobile ? (int) (Math.min(Gdx.graphics.getHeight(), Gdx.graphics.getWidth()) / amount / Unit.dp.scl(1f)) :
                Math.min(Gdx.graphics.getDisplayMode().height / amount, baseSize);

        clearChildren();
        table(cont -> {
            cont.left();

            cont.table(mid -> {
                mid.top();

                Table tools = new Table().top();

                ButtonGroup<ImageButton> group = new ButtonGroup<>();

                Consumer<EditorTool> addTool = tool -> {
                    ImageButton button = new ImageButton("icon-" + tool.name(), "clear-toggle");
                    button.clicked(() -> view.setTool(tool));
                    button.resizeImage(16 * 2f);
                    button.update(() -> button.setChecked(view.getTool() == tool));
                    group.add(button);
                    if(tool == EditorTool.pencil)
                        button.setChecked(true);

                    tools.add(button);
                };

                tools.defaults().size(size, size);

                tools.addImageButton("icon-menu-large", "clear", 16 * 2f, menu::show);

                ImageButton grid = tools.addImageButton("icon-grid", "clear-toggle", 16 * 2f, () -> view.setGrid(!view.isGrid())).get();

                addTool.accept(EditorTool.zoom);

                tools.row();

                ImageButton undo = tools.addImageButton("icon-undo", "clear", 16 * 2f, () -> view.undo()).get();
                ImageButton redo = tools.addImageButton("icon-redo", "clear", 16 * 2f, () -> view.redo()).get();

                addTool.accept(EditorTool.pick);

                tools.row();

                undo.setDisabled(() -> !view.getStack().canUndo());
                redo.setDisabled(() -> !view.getStack().canRedo());

                undo.update(() -> undo.getImage().setColor(undo.isDisabled() ? Color.GRAY : Color.WHITE));
                redo.update(() -> redo.getImage().setColor(redo.isDisabled() ? Color.GRAY : Color.WHITE));
                grid.update(() -> grid.setChecked(view.isGrid()));

                addTool.accept(EditorTool.line);
                addTool.accept(EditorTool.pencil);
                addTool.accept(EditorTool.eraser);

                tools.row();

                addTool.accept(EditorTool.fill);
                addTool.accept(EditorTool.elevation);

                ImageButton rotate = tools.addImageButton("icon-arrow-16", "clear", 16 * 2f, () -> editor.setDrawRotation((editor.getDrawRotation() + 1) % 4)).get();
                rotate.getImage().update(() -> {
                    rotate.getImage().setRotation(editor.getDrawRotation() * 90);
                    rotate.getImage().setOrigin(Align.center);
                });

                tools.row();

                tools.table("underline", t -> t.add("$text.editor.teams"))
                        .colspan(3).height(40).width(size * 3f).padBottom(3);

                tools.row();

                ButtonGroup<ImageButton> teamgroup = new ButtonGroup<>();

                int i = 0;

                for(Team team : Team.all){
                    ImageButton button = new ImageButton("white", "clear-toggle-partial");
                    button.margin(4f);
                    button.getImageCell().grow();
                    button.getStyle().imageUpColor = team.color;
                    button.clicked(() -> editor.setDrawTeam(team));
                    button.update(() -> button.setChecked(editor.getDrawTeam() == team));
                    teamgroup.add(button);
                    tools.add(button);

                    if(i++ % 3 == 2) tools.row();
                }

                mid.add(tools).top().padBottom(-6);

                mid.row();

                mid.table("underline", t -> {
                    Slider slider = new Slider(0, MapEditor.brushSizes.length - 1, 1, false);
                    slider.moved(f -> editor.setBrushSize(MapEditor.brushSizes[(int) (float) f]));

                    t.top();
                    t.add("$text.editor.brush");
                    t.row();
                    t.add(slider).width(size * 3f - 20).padTop(4f);
                }).padTop(5).growX().growY().top();

                mid.row();

                mid.table("underline", t -> t.add("$text.editor.elevation"))
                    .colspan(3).height(40).width(size * 3f);

                mid.row();

                mid.table("underline", t -> {
                    t.margin(0);
                    t.addImageButton("icon-arrow-left", "clear-partial", 16 * 2f, () -> editor.setDrawElevation(editor.getDrawElevation() - 1))
                    .disabled(b -> editor.getDrawElevation() <= -1).size(size);

                    t.label(() -> editor.getDrawElevation() == -1 ? "$text.editor.slope" : (editor.getDrawElevation() + ""))
                    .size(size).get().setAlignment(Align.center, Align.center);

                    t.addImageButton("icon-arrow-right", "clear-partial", 16 * 2f, () -> editor.setDrawElevation(editor.getDrawElevation() + 1))
                    .disabled(b -> editor.getDrawElevation() >= 63).size(size);
                }).colspan(3).height(size).width(size * 3f);

            }).margin(0).left().growY();


            cont.table(t -> t.add(view).grow()).grow();

            cont.table(this::addBlockSelection).right().growY();

        }).grow();
    }

    private void doInput(){
        //tool select
        for(int i = 0; i < EditorTool.values().length; i++){
            if(Inputs.keyTap(Input.valueOf("NUM_" + (i + 1)))){
                view.setTool(EditorTool.values()[i]);
                break;
            }
        }

        if(Inputs.keyTap(Input.R)){
            editor.setDrawRotation((editor.getDrawRotation() + 1) % 4);
        }

        if(Inputs.keyTap(Input.E)){
            editor.setDrawRotation(Mathf.mod((editor.getDrawRotation() + 1), 4));
        }

        //ctrl keys (undo, redo, save)
        if(UIUtils.ctrl()){
            if(Inputs.keyTap(Input.Z)){
                view.undo();
            }

            if(Inputs.keyTap(Input.Y)){
                view.redo();
            }

            if(Inputs.keyTap(Input.S)){
                save();
            }

            if(Inputs.keyTap(Input.G)){
                view.setGrid(!view.isGrid());
            }
        }
    }

    private void tryExit(){
        if(!saved){
            ui.showConfirm("$text.confirm", "$text.editor.unsaved", this::hide);
        }else{
            hide();
        }
    }

    private void addBlockSelection(Table table){
        Table content = new Table();
        pane = new ScrollPane(content);
        pane.setFadeScrollBars(false);
        pane.setOverscroll(true, false);
        ButtonGroup<ImageButton> group = new ButtonGroup<>();
        blockgroup = group;

        int i = 0;

        for(Block block : Vars.content.blocks()){
            TextureRegion[] regions = block.getCompactIcon();
            if((block.synthetic() && (Recipe.getByResult(block) == null || !control.unlocks.isUnlocked(Recipe.getByResult(block))))
                    && block != StorageBlocks.core){
                continue;
            }

            if(Recipe.getByResult(block) != null && !Recipe.getByResult(block).visibility.shown()){
                continue;
            }

            if(regions.length == 0 || regions[0] == Draw.region("jjfgj")) continue;

            Stack stack = new Stack();

            for(TextureRegion region : regions){
                stack.add(new Image(region));
            }

            ImageButton button = new ImageButton("white", "clear-toggle");
            button.clicked(() -> editor.setDrawBlock(block));
            button.resizeImage(8 * 4f);
            button.getImageCell().setActor(stack);
            button.addChild(stack);
            button.getImage().remove();
            button.update(() -> button.setChecked(editor.getDrawBlock() == block));
            group.add(button);
            content.add(button).size(60f);

            if(i++ % 3 == 2){
                content.row();
            }
        }

        group.getButtons().get(2).setChecked(true);

        table.table("underline", extra -> extra.labelWrap(() -> editor.getDrawBlock().formalName).width(220f).center()).growX();
        table.row();
        table.add(pane).growY().fillX();
    }
}
