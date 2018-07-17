package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.ui.BorderImage;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.utils.Elements;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class LevelDialog extends FloatingDialog{

    public LevelDialog(){
        super("$text.level.select");
        addCloseButton();
        shown(this::setup);

        onResize(this::setup);
    }

    void setup(){
        content().clear();

        Table maps = new Table();
        maps.marginRight(14);
        ScrollPane pane = new ScrollPane(maps, "clear-black");
        pane.setFadeScrollBars(false);

        int maxwidth = (Gdx.graphics.getHeight() > Gdx.graphics.getHeight() ? 2 : 4);

        Table selmode = new Table();
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        selmode.add("$text.level.mode").padRight(15f);

        for(GameMode mode : GameMode.values()){
            TextButton[] b = {null};
            b[0] = Elements.newButton("$mode." + mode.name() + ".name", "toggle", () -> state.mode = mode);
            b[0].update(() -> b[0].setChecked(state.mode == mode));
            group.add(b[0]);
            selmode.add(b[0]).size(130f, 54f);
        }
        selmode.addButton("?", this::displayGameModeHelp).size(50f, 54f).padLeft(18f);

        content().add(selmode);
        content().row();

        Difficulty[] ds = Difficulty.values();

        float s = 50f;

        Table sdif = new Table();

        sdif.add("$setting.difficulty.name").padRight(15f);

        sdif.defaults().height(s + 4);
        sdif.addImageButton("icon-arrow-left", 10 * 3, () -> {
            state.difficulty = (ds[Mathf.mod(state.difficulty.ordinal() - 1, ds.length)]);
        }).width(s);

        sdif.addButton("", () -> {

        }).update(t -> {
            t.setText(state.difficulty.toString());
            t.setTouchable(Touchable.disabled);
        }).width(180f);

        sdif.addImageButton("icon-arrow-right", 10 * 3, () -> {
            state.difficulty = (ds[Mathf.mod(state.difficulty.ordinal() + 1, ds.length)]);
        }).width(s);

        content().add(sdif);
        content().row();

        float images = 146f;

        int i = 0;
        for(Map map : world.maps().all()){

            if(i % maxwidth == 0){
                maps.row();
            }

            ImageButton image = new ImageButton(new TextureRegion(map.texture), "clear");
            image.margin(5);
            image.getImageCell().size(images);
            image.top();
            image.row();
            image.add("[accent]" + Bundles.get("map." + map.name + ".name", map.name)).pad(3f).growX().wrap().get().setAlignment(Align.center, Align.center);
            image.row();
            image.label((() -> Bundles.format("text.level.highscore", Settings.getInt("hiscore" + map.name, 0)))).pad(3f);

            BorderImage border = new BorderImage(map.texture, 3f);
            image.replaceImage(border);

            image.clicked(() -> {
                hide();
                control.playMap(map);
            });

            maps.add(image).width(170).fillY().top().pad(4f);

            i++;
        }

        ImageButton genb = maps.addImageButton("icon-editor", "clear", 16 * 3, () -> {
            hide();
            //TODO

            /*
            ui.loadfrag.show();

            Timers.run(5f, () -> {
                Cursors.restoreCursor();
                threads.run(() -> {
                    world.loadSector(0, 0);
                    logic.play();
                    Gdx.app.postRunnable(ui.loadfrag::hide);
                });
            });*/
        }).width(170).fillY().pad(4f).get();

        genb.top();
        genb.margin(5);
        genb.clearChildren();
        genb.add(new BorderImage(Draw.region("icon-generated"), 3f)).size(images);
        genb.row();
        genb.add("$text.map.random").growX().wrap().pad(3f).get().setAlignment(Align.center, Align.center);
        genb.row();
        genb.add("<generated>").pad(3f);

        content().add(pane).uniformX();
    }

    private void displayGameModeHelp(){
        FloatingDialog d = new FloatingDialog(Bundles.get("mode.text.help.title"));
        d.setFillParent(false);
        Table table = new Table();
        table.defaults().pad(1f);
        ScrollPane pane = new ScrollPane(table, "clear");
        pane.setFadeScrollBars(false);
        table.row();
        for(GameMode mode : GameMode.values()){
            table.labelWrap("[accent]" + mode.toString() + ":[] [lightgray]" + mode.description()).width(400f);
            table.row();
        }

        d.content().add(pane);
        d.buttons().addButton("$text.ok", d::hide).size(110, 50).pad(10f);
        d.show();
    }

}
