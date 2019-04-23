package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.scene.ui.Image;
import io.anuke.arc.scene.ui.Tooltip;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Mech;

import static io.anuke.mindustry.Vars.*;

public class SandboxDialog extends FloatingDialog{
    private Table table;

    public SandboxDialog(){
        super("$sandbox.options");
        setFillParent(true);
        shown(this::setup);
        onResize(this::setup);
        addCloseButton();
    }

    private void setup(){
        cont.clear();
        cont.pane(m->table=m);
        table.top();
        table.margin(10f);
        table.addButton("$sandbox.rules", ()->ui.custom.dialog.show()).width(300f).center();
        table.row();
        table.add("$sandbox.mech").growX().left().color(Pal.accent).padTop(50f);
        table.row();
        table.addImage("white").growX().pad(5).padLeft(0).padRight(0).height(3).color(Pal.accent);
        table.row();
        table.table(t->{
            for(Mech m : content.<Mech>getBy(ContentType.mech)){
                Image image = new Image(m.getContentIcon());
                image.clicked(()->{
                    player.mech = m;
                    this.hide();
                });
                image.addListener(new Tooltip<>(new Table("button"){{
                    add(m.localizedName);
                }}));
                t.add(image);
            }
        });
    }
}
