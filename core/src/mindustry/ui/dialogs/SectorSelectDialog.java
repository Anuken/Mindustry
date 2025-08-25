package mindustry.ui.dialogs;

import arc.*;
import arc.func.*;
import arc.input.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;

import java.util.*;

//internal use only!
public class SectorSelectDialog extends BaseDialog{
    Table sectors = new Table();
    Planet planet = Planets.serpulo;
    Cons<SectorPreset> cons = s -> {};
    TextField search;

    public SectorSelectDialog(){
        super("@content.sector.name");

        cont.top();
        cont.table(s -> {
            s.image(Icon.zoom);
            search = s.field("", ignored -> {
                rebuild();
            }).width(300f).get();
            search.keyDown(KeyCode.enter, () -> {
                String text = search.getText().toLowerCase(Locale.ROOT);
                var found = Vars.content.sectors().find(sec -> matches(sec, text));
                if(found != null){
                    cons.get(found);
                    hide();
                }
            });
        });
        cont.row();

        cont.pane(sectors).grow().top();
        sectors.top();

        addCloseButton();

        shown(() -> {
            search.clearText();
            search.requestKeyboard();
            Core.app.post(() -> search.requestKeyboard());
            rebuild();
        });
    }

    public void show(Planet planet, Cons<SectorPreset> cons){
        this.planet = planet;
        this.cons = cons;

        show();
    }

    void rebuild(){
        sectors.clear();

        String text = search.getText().toLowerCase(Locale.ROOT);

        for(var sector : Vars.content.sectors()){
            if(matches(sector, text)){
                sectors.button(sector.localizedName, new TextureRegionDrawable(sector.uiIcon), Styles.grayt, 32f, () -> {
                    cons.get(sector);
                    hide();
                }).size(400f, 50f).margin(4f).pad(3f);
                sectors.row();
            }
        }
    }

    boolean matches(SectorPreset sector, String text){
        return sector.planet == planet && (text.isEmpty() || sector.name.toLowerCase(Locale.ROOT).contains(text) || sector.localizedName.toLowerCase(Locale.ROOT).contains(text));
    }
}
