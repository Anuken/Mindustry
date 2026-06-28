package mindustry.editor.data;

import arc.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.mod.*;
import mindustry.mod.data.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class MapEmojisView implements AssetView{
    ObjectSet<EmojiAsset> selected = new ObjectSet<>();

    @Override
    public void build(MapAssetsDialog diag, Table table){
        selected.clear();

        var all = state.data.getEmojis();
        int i = 0;
        int cols = Math.max(1, (int)(Core.graphics.getWidth() * 0.9f / Scl.scl(70f)));

        for(var emoji : all){
            if(diag.searchString != null && !emoji.name.toLowerCase().contains(diag.searchString)) continue;

            table.button(new TextureRegionDrawable(emoji.findRegion()), Styles.grayTogglei, 48f, () -> {
                if(selected.contains(emoji)){
                    selected.remove(emoji);
                }else{
                    selected.add(emoji);
                }
            }).size(62f).pad(4f).tooltip(emoji.name);

            if((++i) % cols == 0){
                table.row();
            }
        }
    }

    @Override
    public void buildButtons(MapAssetsDialog diag, Table buttons){
        buttons.button("@add", Icon.add, () -> {
            BaseDialog dialog = new BaseDialog("@add");
            dialog.setFillParent(false);

            AtlasRegion[] result = {Core.atlas.find("error")};

            dialog.cont.add(new BorderImage(Core.atlas.find("clear"), 4f)).update(i -> {
                i.setDrawable(result[0].found() ? result[0] : Core.atlas.find("nomap"));
            }).with(c -> c.drawAlpha = true).size(64f).pad(8f).row();

            var field = dialog.cont.field("", text -> result[0] = (AtlasRegion)Core.atlas.find(DataImagePacker.regionPrefix + text, text))
                .valid(t -> Core.atlas.find(DataImagePacker.regionPrefix + t, t).found() && !Fonts.hasIcon(t)).size(300f, 40f).get();

            dialog.cont.row();

            dialog.cont.label(() -> !result[0].found() ? "@asset.emoji.notfound" : Fonts.hasIcon(field.getText()) ? "@asset.emoji.exists" : "").pad(3f).color(Pal.remove);

            dialog.addCloseButton();
            dialog.buttons.button("@ok", Icon.ok, () -> {
                state.data.getEmojis().add(new EmojiAsset(field.getText()));
                state.data.reloadEmojis();
                diag.rebuild();
                dialog.hide();
            }).size(210f, 64f).disabled(b -> !field.isValid());

            dialog.show();
            Core.app.post(field::requestKeyboard);

        }).size(190f, 64f);

        buttons.button("@asset.emoji.delete", Icon.trash, () -> {
            for(var rem : selected){
                state.data.getEmojis().remove(rem);
            }
            state.data.reloadEmojis();
            selected.clear();
            diag.rebuild();
        }).size(190f, 64f).with(t -> {
            t.visible(() -> {
                t.setText(Core.bundle.format("asset.emoji.delete", selected.size));
                return selected.notEmpty();
            });
        });
    }
}
