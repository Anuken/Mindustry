package mindustry.editor;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.game.*;
import mindustry.game.MapObjectives.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class MapEditorDialog extends Dialog implements Disposable{
    private MapView view;
    private MapInfoDialog infoDialog;
    private MapLoadDialog loadDialog;
    private MapResizeDialog resizeDialog;
    private MapGenerateDialog generateDialog;
    private SectorGenerateDialog sectorGenDialog;
    private MapPlayDialog playtestDialog;
    private ScrollPane pane;
    private BaseDialog menu;
    private Table blockSelection;
    private Rules lastSavedRules;
    private boolean saved = false; //currently never read
    private boolean shownWithMap = false;
    private Seq<Block> blocksOut = new Seq<>();

    public MapEditorDialog(){
        super("");

        background(Styles.black);

        view = new MapView();
        infoDialog = new MapInfoDialog();
        generateDialog = new MapGenerateDialog(true);
        sectorGenDialog = new SectorGenerateDialog();
        playtestDialog = new MapPlayDialog();

        menu = new BaseDialog("@menu");
        menu.addCloseButton();

        float swidth = 180f;

        menu.cont.table(t -> {
            t.defaults().size(swidth, 60f).padBottom(5).padRight(5).padLeft(5);

            t.button("@editor.savemap", Icon.save, this::save);

            t.button("@editor.mapinfo", Icon.pencil, () -> {
                infoDialog.show();
                menu.hide();
            });

            t.row();

            t.button("@editor.generate", Icon.terrain, () -> {
                generateDialog.show(generateDialog::applyToEditor);
                menu.hide();
            });

            t.button("@editor.resize", Icon.resize, () -> {
                resizeDialog.show();
                menu.hide();
            });

            t.row();

            t.button("@editor.import", Icon.download, () -> createDialog("@editor.import",
                "@editor.importmap", "@editor.importmap.description", Icon.download, (Runnable)loadDialog::show,
                "@editor.importfile", "@editor.importfile.description", Icon.file, (Runnable)() ->
                platform.showFileChooser(true, mapExtension, file -> ui.loadAnd(() -> {
                    maps.tryCatchMapError(() -> {
                        if(MapIO.isImage(file)){
                            ui.showInfo("@editor.errorimage");
                        }else{
                            editor.beginEdit(MapIO.createMap(file, true));
                        }
                    });
                })),

                "@editor.importimage", "@editor.importimage.description", Icon.fileImage, (Runnable)() ->
                platform.showFileChooser(true, "png", file ->
                ui.loadAnd(() -> {
                    try{
                        Pixmap pixmap = new Pixmap(file);
                        editor.beginEdit(pixmap);
                        pixmap.dispose();
                    }catch(Exception e){
                        ui.showException("@editor.errorload", e);
                        Log.err(e);
                    }
                })))
            );

            t.button("@editor.export", Icon.upload, () -> createDialog("@editor.export",
            "@editor.exportfile", "@editor.exportfile.description", Icon.file,
                (Runnable)() -> platform.export(editor.tags.get("name", "unknown"), mapExtension, file -> MapIO.writeMap(file, editor.createMap(file))),
            "@editor.exportimage", "@editor.exportimage.description", Icon.fileImage,
                (Runnable)() -> platform.export(editor.tags.get("name", "unknown"), "png", file -> {
                    Pixmap out = MapIO.writeImage(editor.tiles());
                    file.writePng(out);
                    out.dispose();
                })));

            t.row();

            t.button("@editor.ingame", Icon.right, this::editInGame);

            t.button("@editor.playtest", Icon.play, this::playtest);
        });

        menu.cont.row();

        if(steam){
            menu.cont.button("@editor.publish.workshop", Icon.link, () -> {
                Map builtin = maps.all().find(m -> m.name().equals(editor.tags.get("name", "").trim()));

                if(editor.tags.containsKey("steamid") && builtin != null && !builtin.custom){
                    platform.viewListingID(editor.tags.get("steamid"));
                    return;
                }

                Map map = save();

                if(editor.tags.containsKey("steamid") && map != null){
                    platform.viewListing(map);
                    return;
                }

                if(map == null) return;

                if(map.tags.get("description", "").length() < 4){
                    ui.showErrorMessage("@editor.nodescription");
                    return;
                }

                if(!Structs.contains(Gamemode.all, g -> g.valid(map))){
                    ui.showErrorMessage("@map.nospawn");
                    return;
                }

                platform.publish(map);
            }).padTop(-3).size(swidth * 2f + 10, 60f).update(b ->
                b.setText(editor.tags.containsKey("steamid") ?
                    editor.tags.get("author", "").equals(steamPlayerName) ? "@workshop.listing" : "@view.workshop" :
                "@editor.publish.workshop"));

            menu.cont.row();
        }

        //wip feature
        if(experimental){
            menu.cont.button("@editor.sectorgenerate", Icon.terrain, () -> {
                menu.hide();
                sectorGenDialog.show();
            }).padTop(!steam ? -3 : 1).size(swidth * 2f + 10, 60f);
            menu.cont.row();
        }

        menu.cont.row();

        menu.cont.button("@quit", Icon.exit, () -> {
            tryExit();
            menu.hide();
        }).padTop(!steam && !experimental ? -3 : 1).size(swidth * 2f + 10, 60f);

        resizeDialog = new MapResizeDialog((width, height, shiftX, shiftY) -> {
            if(!(editor.width() == width && editor.height() == height && shiftX == 0 && shiftY == 0)){
                ui.loadAnd(() -> {
                    editor.resize(width, height, shiftX, shiftY);
                });
            }
        });

        loadDialog = new MapLoadDialog(map -> ui.loadAnd(() -> {
            try{
                editor.beginEdit(map);
            }catch(Exception e){
                ui.showException("@editor.errorload", e);
                Log.err(e);
            }
        }));

        setFillParent(true);

        clearChildren();
        margin(0);

        update(() -> {
            if(Core.scene.getKeyboardFocus() instanceof Dialog && Core.scene.getKeyboardFocus() != this){
                return;
            }

            if(Core.scene != null && Core.scene.getKeyboardFocus() == this){
                doInput();
            }
        });

        shown(() -> {

            saved = true;
            if(!Core.settings.getBool("landscape")) platform.beginForceLandscape();
            editor.clearOp();
            Core.scene.setScrollFocus(view);
            if(!shownWithMap){
                //clear units, rules and other unnecessary stuff
                logic.reset();
                state.rules = new Rules();
                editor.beginEdit(200, 200);
            }
            shownWithMap = false;

            Time.runTask(10f, platform::updateRPC);
        });

        hidden(() -> {
            editor.clearOp();
            platform.updateRPC();
            if(!Core.settings.getBool("landscape")) platform.endForceLandscape();
        });

        shown(this::build);
    }

    public void resumeEditing(){
        state.set(State.menu);
        shownWithMap = true;
        show();
        state.rules = (lastSavedRules == null ? new Rules() : lastSavedRules);
        lastSavedRules = null;
        saved = false;
        editor.renderer.updateAll();
    }

    private void editInGame(){
        menu.hide();
        ui.loadAnd(() -> {
            lastSavedRules = state.rules;
            hide();
            //only reset the player; logic.reset() will clear entities, which we do not want
            state.teams = new Teams();
            player.reset();
            state.rules = Gamemode.editor.apply(lastSavedRules.copy());
            state.rules.limitMapArea = false;
            state.rules.sector = null;
            state.rules.fog = false;
            state.map = new Map(StringMap.of(
                "name", "Editor Playtesting",
                "width", editor.width(),
                "height", editor.height()
            ));
            world.endMapLoad();
            player.set(world.width() * tilesize/2f, world.height() * tilesize/2f);
            player.clearUnit();

            for(var unit : Groups.unit){
                if(unit.spawnedByCore){
                    unit.remove();
                }
            }

            Groups.build.clear();
            Groups.weather.clear();
            logic.play();

            if(player.team().core() == null){
                player.set(world.width() * tilesize/2f, world.height() * tilesize/2f);
                var unit = (state.rules.hasEnv(Env.scorching) ? UnitTypes.evoke : UnitTypes.alpha).spawn(player.team(), player.x, player.y);
                unit.spawnedByCore = true;
                player.unit(unit);
            }

            player.checkSpawn();
        });
    }

    public void resumeAfterPlaytest(Map map){
        beginEditMap(map.file);
    }

    private void playtest(){
        menu.hide();
        Map map = save();

        if(map != null){
            //skip dialog, play immediately when shift clicked
            if(Core.input.shift()){
                hide();
                //auto pick best fit
                control.playMap(map, map.applyRules(
                    Gamemode.survival.valid(map) ? Gamemode.survival :
                    Gamemode.attack.valid(map) ? Gamemode.attack :
                    Gamemode.sandbox), true
                );
            }else{
                playtestDialog.playListener = this::hide;
                playtestDialog.show(map, true);
            }
        }
    }

    public @Nullable Map save(){
        boolean isEditor = state.rules.editor;
        state.rules.editor = false;
        state.rules.objectiveFlags.clear();
        state.rules.objectives.each(MapObjective::reset);
        String name = editor.tags.get("name", "").trim();
        editor.tags.put("rules", JsonIO.write(state.rules));
        editor.tags.remove("width");
        editor.tags.remove("height");

        player.clearUnit();

        //remove player unit
        Unit unit = Groups.unit.find(u -> u.spawnedByCore);
        if(unit != null){
            unit.remove();
        }

        Map returned = null;

        if(name.isEmpty()){
            infoDialog.show();
            Core.app.post(() -> ui.showErrorMessage("@editor.save.noname"));
        }else{
            Map map = maps.all().find(m -> m.name().equals(name));
            if(map != null && !map.custom && !map.workshop){
                handleSaveBuiltin(map);
            }else{
                boolean workshop = false;
                //try to preserve Steam ID
                if(map != null && map.tags.containsKey("steamid")){
                    editor.tags.put("steamid", map.tags.get("steamid"));
                    workshop = true;
                }
                returned = maps.saveMap(editor.tags);
                if(workshop){
                    returned.workshop = workshop;
                }
                ui.showInfoFade("@editor.saved");
            }
        }

        menu.hide();
        saved = true;
        state.rules.editor = isEditor;
        return returned;
    }

    /** Called when a built-in map save is attempted.*/
    protected void handleSaveBuiltin(Map map){
        ui.showErrorMessage("@editor.save.overwrite");
    }

    /**
     * Argument format:
     * 0) button name
     * 1) description
     * 2) icon name
     * 3) listener
     */
    private void createDialog(String title, Object... arguments){
        BaseDialog dialog = new BaseDialog(title);

        float h = 90f;

        dialog.cont.defaults().size(360f, h).padBottom(5).padRight(5).padLeft(5);

        for(int i = 0; i < arguments.length; i += 4){
            String name = (String)arguments[i];
            String description = (String)arguments[i + 1];
            Drawable iconname = (Drawable)arguments[i + 2];
            Runnable listenable = (Runnable)arguments[i + 3];

            TextButton button = dialog.cont.button(name, () -> {
                listenable.run();
                dialog.hide();
                menu.hide();
            }).left().margin(0).get();

            button.clearChildren();
            button.image(iconname).padLeft(10);
            button.table(t -> {
                t.add(name).growX().wrap();
                t.row();
                t.add(description).color(Color.gray).growX().wrap();
            }).growX().pad(10f).padLeft(5);

            button.row();

            dialog.cont.row();
        }

        dialog.addCloseButton();
        dialog.show();
    }

    @Override
    public Dialog show(){
        return super.show(Core.scene, Actions.sequence(Actions.alpha(1f)));
    }

    @Override
    public void hide(){
        super.hide(Actions.sequence(Actions.alpha(0f)));
    }

    @Override
    public void dispose(){
        editor.renderer.dispose();
    }

    public void beginEditMap(Fi file){
        ui.loadAnd(() -> {
            try{
                shownWithMap = true;
                editor.beginEdit(MapIO.createMap(file, true));
                show();
            }catch(Exception e){
                Log.err(e);
                ui.showException("@editor.errorload", e);
            }
        });
    }

    public MapView getView(){
        return view;
    }

    public MapGenerateDialog getGenerateDialog(){
        return generateDialog;
    }

    public void resetSaved(){
        saved = false;
    }

    public boolean hasPane(){
        return Core.scene.getScrollFocus() == pane || Core.scene.getKeyboardFocus() != this;
    }

    public void build(){
        float size = mobile ? 50f : 58f;

        clearChildren();
        table(cont -> {
            cont.left();

            cont.table(mid -> {
                mid.top();

                Table tools = new Table().top();

                ButtonGroup<ImageButton> group = new ButtonGroup<>();
                Table[] lastTable = {null};

                Cons<EditorTool> addTool = tool -> {

                    ImageButton button = new ImageButton(ui.getIcon(tool.name()), Styles.squareTogglei);
                    button.clicked(() -> {
                        view.setTool(tool);
                        if(lastTable[0] != null){
                            lastTable[0].remove();
                        }
                    });
                    button.update(() -> button.setChecked(view.getTool() == tool));
                    group.add(button);

                    if(tool.altModes.length > 0){
                        button.clicked(l -> {
                            if(!mobile){
                                //desktop: rightclick
                                l.setButton(KeyCode.mouseRight);
                            }
                        }, e -> {
                            //need to double tap
                            if(mobile && e.getTapCount() < 2){
                                return;
                            }

                            if(lastTable[0] != null){
                                lastTable[0].remove();
                            }

                            Table table = new Table(Styles.black9);
                            table.defaults().size(300f, 70f);

                            for(int i = 0; i < tool.altModes.length; i++){
                                int mode = i;
                                String name = tool.altModes[i];

                                table.button(b -> {
                                    b.left();
                                    b.marginLeft(6);
                                    b.setStyle(Styles.flatTogglet);
                                    b.add(Core.bundle.get("toolmode." + name)).left();
                                    b.row();
                                    b.add(Core.bundle.get("toolmode." + name + ".description")).color(Color.lightGray).left();
                                }, () -> {
                                    tool.mode = (tool.mode == mode ? -1 : mode);
                                    table.remove();
                                }).update(b -> b.setChecked(tool.mode == mode));
                                table.row();
                            }

                            table.update(() -> {
                                Vec2 v = button.localToStageCoordinates(Tmp.v1.setZero());
                                table.setPosition(v.x, v.y, Align.topLeft);
                                if(!isShown()){
                                    table.remove();
                                    lastTable[0] = null;
                                }
                            });

                            table.pack();
                            table.act(Core.graphics.getDeltaTime());

                            addChild(table);
                            lastTable[0] = table;
                        });
                    }


                    Label mode = new Label("");
                    mode.setColor(Pal.remove);
                    mode.update(() -> mode.setText(tool.mode == -1 ? "" : "M" + (tool.mode + 1) + " "));
                    mode.setAlignment(Align.bottomRight, Align.bottomRight);
                    mode.touchable = Touchable.disabled;

                    tools.stack(button, mode);
                };

                tools.defaults().size(size, size);

                tools.button(Icon.menu, Styles.flati, menu::show);

                ImageButton grid = tools.button(Icon.grid, Styles.squareTogglei, () -> view.setGrid(!view.isGrid())).get();

                addTool.get(EditorTool.zoom);

                tools.row();

                ImageButton undo = tools.button(Icon.undo, Styles.flati, editor::undo).get();
                ImageButton redo = tools.button(Icon.redo, Styles.flati, editor::redo).get();

                addTool.get(EditorTool.pick);

                tools.row();

                undo.setDisabled(() -> !editor.canUndo());
                redo.setDisabled(() -> !editor.canRedo());

                undo.update(() -> undo.getImage().setColor(undo.isDisabled() ? Color.gray : Color.white));
                redo.update(() -> redo.getImage().setColor(redo.isDisabled() ? Color.gray : Color.white));
                grid.update(() -> grid.setChecked(view.isGrid()));

                addTool.get(EditorTool.line);
                addTool.get(EditorTool.pencil);
                addTool.get(EditorTool.eraser);

                tools.row();

                addTool.get(EditorTool.fill);
                addTool.get(EditorTool.spray);

                ImageButton rotate = tools.button(Icon.right, Styles.flati, () -> editor.rotation = (editor.rotation + 1) % 4).get();
                rotate.getImage().update(() -> {
                    rotate.getImage().setRotation(editor.rotation * 90);
                    rotate.getImage().setOrigin(Align.center);
                });

                tools.row();

                tools.table(Tex.underline, t -> t.add("@editor.teams"))
                .colspan(3).height(40).width(size * 3f + 3f).padBottom(3);

                tools.row();

                ButtonGroup<ImageButton> teamgroup = new ButtonGroup<>();

                int i = 0;

                for(Team team : Team.baseTeams){
                    ImageButton button = new ImageButton(Tex.whiteui, Styles.clearNoneTogglei);
                    button.margin(4f);
                    button.getImageCell().grow();
                    button.getStyle().imageUpColor = team.color;
                    button.clicked(() -> editor.drawTeam = team);
                    button.update(() -> button.setChecked(editor.drawTeam == team));
                    teamgroup.add(button);
                    tools.add(button);

                    if(i++ % 3 == 2) tools.row();
                }

                mid.add(tools).top().padBottom(-6);

                mid.row();

                mid.table(Tex.underline, t -> {
                    Slider slider = new Slider(0, MapEditor.brushSizes.length - 1, 1, false);
                    slider.moved(f -> editor.brushSize = MapEditor.brushSizes[(int)f]);
                    for(int j = 0; j < MapEditor.brushSizes.length; j++){
                        if(MapEditor.brushSizes[j] == editor.brushSize){
                            slider.setValue(j);
                        }
                    }

                    var label = new Label("@editor.brush");
                    label.setAlignment(Align.center);
                    label.touchable = Touchable.disabled;

                    t.top().stack(slider, label).width(size * 3f - 20).padTop(4f);
                    t.row();
                }).padTop(5).growX().top();

                mid.row();

                if(!mobile){
                    mid.table(t -> {
                        t.button("@editor.center", Icon.move, Styles.flatt, view::center).growX().margin(9f);
                    }).growX().top();
                }

                mid.row();

                mid.table(t -> {
                    t.button("@editor.cliffs", Icon.terrain, Styles.flatt, editor::addCliffs).growX().margin(9f);
                }).growX().top();
            }).margin(0).left().growY();


            cont.table(t -> t.add(view).grow()).grow();

            cont.table(this::addBlockSelection).right().growY();

        }).grow();
    }

    private void doInput(){

        if(Core.input.ctrl()){
            //alt mode select
            for(int i = 0; i < view.getTool().altModes.length; i++){
                if(i + 1 < KeyCode.numbers.length && Core.input.keyTap(KeyCode.numbers[i + 1])){
                    view.getTool().mode = i;
                }
            }
        }else{
            for(EditorTool tool : EditorTool.all){
                if(Core.input.keyTap(tool.key)){
                    view.setTool(tool);
                    break;
                }
            }
        }

        if(Core.input.keyTap(KeyCode.escape)){
            if(!menu.isShown()){
                menu.show();
            }
        }

        if(Core.input.keyTap(KeyCode.r)){
            editor.rotation = Mathf.mod(editor.rotation + 1, 4);
        }

        if(Core.input.keyTap(KeyCode.e)){
            editor.rotation = Mathf.mod(editor.rotation - 1, 4);
        }

        //ctrl keys (undo, redo, save)
        if(Core.input.ctrl()){
            if(Core.input.keyTap(KeyCode.z)){
                editor.undo();
            }

            //more undocumented features, fantastic
            if(Core.input.keyTap(KeyCode.t)){

                //clears all 'decoration' from the map
                for(int x = 0; x < editor.width(); x++){
                    for(int y = 0; y < editor.height(); y++){
                        Tile tile = editor.tile(x, y);
                        if(tile.block().breakable && tile.block() instanceof Prop){
                            tile.setBlock(Blocks.air);
                            editor.renderer.updatePoint(x, y);
                        }

                        if(tile.overlay() != Blocks.air && tile.overlay() != Blocks.spawn){
                            tile.setOverlay(Blocks.air);
                            editor.renderer.updatePoint(x, y);
                        }
                    }
                }

                editor.flushOp();
            }

            if(Core.input.keyTap(KeyCode.y)){
                editor.redo();
            }

            if(Core.input.keyTap(KeyCode.s)){
                save();
            }

            if(Core.input.keyTap(KeyCode.g)){
                view.setGrid(!view.isGrid());
            }
        }
    }

    private void tryExit(){
        ui.showConfirm("@confirm", "@editor.unsaved", this::hide);
    }

    private void addBlockSelection(Table cont){
        blockSelection = new Table();
        pane = new ScrollPane(blockSelection);
        pane.setFadeScrollBars(false);
        pane.setOverscroll(true, false);
        pane.exited(() -> {
            if(pane.hasScroll()){
                Core.scene.setScrollFocus(view);
            }
        });

        cont.table(search -> {
            search.image(Icon.zoom).padRight(8);
            search.field("", this::rebuildBlockSelection).growX()
            .name("editor/search").maxTextLength(maxNameLength).get().setMessageText("@players.search");
        }).growX().pad(-2).padLeft(6f);
        cont.row();
        cont.table(Tex.underline, extra -> extra.labelWrap(() -> editor.drawBlock.localizedName).width(200f).center()).growX();
        cont.row();
        cont.add(pane).expandY().top().left();

        rebuildBlockSelection("");
    }

    private void rebuildBlockSelection(String searchText){
        blockSelection.clear();

        blocksOut.clear();
        blocksOut.addAll(Vars.content.blocks());
        blocksOut.sort((b1, b2) -> {
            int core = -Boolean.compare(b1 instanceof CoreBlock, b2 instanceof CoreBlock);
            if(core != 0) return core;
            int synth = Boolean.compare(b1.synthetic(), b2.synthetic());
            if(synth != 0) return synth;
            int ore = Boolean.compare(b1 instanceof OverlayFloor, b2 instanceof OverlayFloor);
            if(ore != 0) return ore;
            return Integer.compare(b1.id, b2.id);
        });

        int i = 0;

        for(Block block : blocksOut){
            TextureRegion region = block.uiIcon;

            if(!Core.atlas.isFound(region) || !block.inEditor
                    || block.buildVisibility == BuildVisibility.debugOnly
                    || (!searchText.isEmpty() && !block.localizedName.toLowerCase().contains(searchText.toLowerCase()))
            ) continue;

            ImageButton button = new ImageButton(Tex.whiteui, Styles.squareTogglei);
            button.getStyle().imageUp = new TextureRegionDrawable(region);
            button.clicked(() -> editor.drawBlock = block);
            button.resizeImage(8 * 4f);
            button.update(() -> button.setChecked(editor.drawBlock == block));
            blockSelection.add(button).size(50f).tooltip(block.localizedName);

            if(i == 0) editor.drawBlock = block;

            if(++i % 4 == 0){
                blockSelection.row();
            }
        }

        if(i == 0){
            blockSelection.add("@none.found").color(Color.lightGray).padLeft(54f).padTop(10f);
        }
    }
}
