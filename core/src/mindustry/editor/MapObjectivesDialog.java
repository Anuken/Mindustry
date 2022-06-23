package mindustry.editor;

import arc.*;
import arc.struct.*;
import mindustry.game.MapObjectives.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class MapObjectivesDialog extends BaseDialog{
    private Seq<MapObjective> objectives = new Seq<>();

    public MapObjectivesDialog(){
        super("@editor.objectives");

        buttons.defaults().size(180f, 64f).pad(2f);
        buttons.button("@back", Icon.left, this::hide);

        buttons.button("@edit", Icon.edit, () -> {
            BaseDialog dialog = new BaseDialog("@editor.export");
            dialog.cont.pane(p -> {
                p.margin(10f);
                p.table(Tex.button, in -> {
                    var style = Styles.flatt;

                    in.defaults().size(280f, 60f).left();

                    in.button("@waves.copy", Icon.copy, style, () -> {
                        dialog.hide();

                        Core.app.setClipboardText(JsonIO.write(objectives));
                    }).marginLeft(12f).row();
                    in.button("@waves.load", Icon.download, style, () -> {
                        dialog.hide();
                        try{
                            objectives.set(JsonIO.read(Seq.class, Core.app.getClipboardText()));

                            setup();
                        }catch(Throwable e){
                            ui.showException(e);
                        }
                    }).marginLeft(12f).disabled(b -> Core.app.getClipboardText() == null).row();
                    in.button("@clear", Icon.none, style, () -> {
                        dialog.hide();
                        objectives.clear();
                        setup();
                    }).marginLeft(12f).row();
                });
            });

            dialog.addCloseButton();
            dialog.show();
        });
    }

    public void show(Seq<MapObjective> objectives){
        super.show();

        this.objectives = objectives;
        setup();
    }

    void setup(){
        cont.clear();
        cont.add("This editor doesn't work yet. Come back later.");
    }
}
