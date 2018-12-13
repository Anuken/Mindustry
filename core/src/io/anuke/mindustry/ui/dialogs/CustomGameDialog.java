package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.ui.BorderImage;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class CustomGameDialog extends FloatingDialog{

    public CustomGameDialog(){
        super("$text.customgame");
        addCloseButton();
        shown(this::setup);

        onResize(this::setup);
    }

    void setup(){
        content().clear();

        Table maps = new Table();
        maps.marginRight(14);
        ScrollPane pane = new ScrollPane(maps);
        pane.setFadeScrollBars(false);

        int maxwidth = (Gdx.graphics.getHeight() > Gdx.graphics.getHeight() ? 2 : 4);

        Table selmode = new Table();
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        selmode.add("$text.level.mode").padRight(15f);
        int i = 0;

        Table modes = new Table();
        modes.marginBottom(5);

        for(GameMode mode : GameMode.values()){
            if(mode.hidden) continue;

            modes.addButton("$mode." + mode.name() + ".name", "toggle", () -> state.mode = mode)
                .update(b -> b.setChecked(state.mode == mode)).group(group).size(140f, 54f);
            if(i++ % 2 == 1) modes.row();
        }
        selmode.add(modes);
        selmode.addButton("?", this::displayGameModeHelp).width(50f).fillY().padLeft(18f);

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

        sdif.addButton("", () -> {})
        .update(t -> {
            t.setText(state.difficulty.toString());
            t.setTouchable(Touchable.disabled);
        }).width(180f);

        sdif.addImageButton("icon-arrow-right", 10 * 3, () -> {
            state.difficulty = (ds[Mathf.mod(state.difficulty.ordinal() + 1, ds.length)]);
        }).width(s);

        content().add(sdif);
        content().row();

        float images = 146f;

        i = 0;
        maps.defaults().width(170).fillY().top().pad(4f);
        for(Map map : world.maps.all()){

            if(i % maxwidth == 0){
                maps.row();
            }

            ImageButton image = new ImageButton(new TextureRegion(map.texture), "clear");
            image.margin(5);
            image.getImageCell().size(images);
            image.top();
            image.row();
            image.add("[accent]" + map.getDisplayName()).pad(3f).growX().wrap().get().setAlignment(Align.center, Align.center);
            image.row();
            image.label((() -> Bundles.format("text.level.highscore", Settings.getInt("hiscore" + map.name, 0)))).pad(3f);

            BorderImage border = new BorderImage(map.texture, 3f);
            border.setScaling(Scaling.fit);
            image.replaceImage(border);

            image.clicked(() -> {
                hide();
                control.playMap(map);
            });

            maps.add(image);

            i++;
        }

        ImageButton gen = maps.addImageButton("icon-editor", "clear", 16*4, () -> {
            hide();
            world.generator.playRandomMap();
        }).growY().get();
        gen.row();
        gen.add("$text.map.random");

        if(world.maps.all().size == 0){
            maps.add("$text.maps.none").pad(50);
        }

        content().add(pane).uniformX();
    }

    private void displayGameModeHelp(){
        FloatingDialog d = new FloatingDialog(Bundles.get("mode.text.help.title"));
        d.setFillParent(false);
        Table table = new Table();
        table.defaults().pad(1f);
        ScrollPane pane = new ScrollPane(table);
        pane.setFadeScrollBars(false);
        table.row();
        for(GameMode mode : GameMode.values()){
            if(mode.hidden) continue;
            table.labelWrap("[accent]" + mode.toString() + ":[] [lightgray]" + mode.description()).width(400f);
            table.row();
        }

        d.content().add(pane);
        d.buttons().addButton("$text.ok", d::hide).size(110, 50).pad(10f);
        d.show();
    }

}
