package mindustry.editor;

import arc.*;
import arc.func.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.maps.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class MapLoadDialog extends BaseDialog{
    private @Nullable Map selected = null;

    public MapLoadDialog(Cons<Map> loader){
        super("@editor.loadmap");

        shown(this::rebuild);
        hidden(() -> selected = null);
        onResize(this::rebuild);

        buttons.defaults().size(210f, 64f);
        buttons.button("@cancel", Icon.cancel, this::hide);
        buttons.button("@load", Icon.ok, () -> {
            if(selected != null){
                loader.get(selected);
                hide();
            }
        }).disabled(b -> selected == null);
        addCloseListener();
        makeButtonOverlay();
    }

    public void rebuild(){
        cont.clear();
        ButtonGroup<Button> group = new ButtonGroup<>();

        int i = 0;
        int cols = Math.max((int)(Core.graphics.getWidth() / Scl.scl(300f)), 1);

        Table table = new Table();
        table.defaults().size(250f, 90f).pad(4f);
        table.margin(10f);

        ScrollPane pane = new ScrollPane(table);
        pane.setFadeScrollBars(false);
        pane.setScrollingDisabledX(true);

        for(Map map : maps.all()){
            table.button(b -> {
                b.add(new BorderImage(map.safeTexture(), 2f).setScaling(Scaling.fit)).padLeft(5f).size(16 * 4f);
                b.add(map.name()).wrap().grow().labelAlign(Align.center).padLeft(5f);
            }, Styles.squareTogglet, () -> selected = map).group(group).margin(8f).checked(b -> selected == map);

            if(++i % cols == 0) table.row();
        }

        group.uncheckAll();

        if(maps.all().isEmpty()){
            table.add("@maps.none").center();
        }else{
            cont.add("@editor.selectmap");
        }

        cont.row();
        cont.add(pane);
    }
}
