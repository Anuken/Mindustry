package mindustry.editor;

import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class SectorGenerateDialog extends BaseDialog{
    Planet planet = Planets.erekir;
    int sector = 0, seed = 0;

    public SectorGenerateDialog(){
        super("@editor.sectorgenerate");
        setup();
    }

    void setup(){
        cont.clear();
        buttons.clear();

        addCloseButton();

        cont.defaults().left();

        cont.add("@editor.planet").padRight(10f);

        cont.button(planet.localizedName, () -> {
            BaseDialog dialog = new BaseDialog("");
            dialog.cont.pane(p -> {
                p.background(Tex.button).margin(10f);
                int i = 0;

                for(var plan : content.planets()){
                    if(plan.generator == null || plan.sectors.size == 0 || !plan.accessible) continue;

                    p.button(plan.localizedName, Styles.flatTogglet, () -> {
                        planet = plan;
                        sector = Math.min(sector, planet.sectors.size - 1);
                        seed = 0;
                        dialog.hide();
                    }).size(110f, 45f).checked(planet == plan);

                    if(++i % 4 == 0){
                        p.row();
                    }
                }
            });
            dialog.setFillParent(false);
            dialog.addCloseButton();
            dialog.show();
        }).size(200f, 40f).get().getLabel().setText(() -> planet.localizedName);

        cont.row();

        cont.add("@editor.sector").padRight(10f);

        cont.field(sector + "", text -> {
            sector = Strings.parseInt(text);
        }).width(200f).valid(text -> planet.sectors.size > Strings.parseInt(text, 99999) && Strings.parseInt(text, 9999) >= 0);

        cont.row();

        cont.add("@editor.seed").padRight(10f);

        cont.field(seed + "", text -> {
            seed = Strings.parseInt(text);
        }).width(200f).valid(Strings::canParseInt);

        cont.row();

        cont.label(() -> "[ " + planet.sectors.get(sector).getSize() + "x" + planet.sectors.get(sector).getSize() + " ]").color(Pal.accent).center().labelAlign(Align.center).padTop(5).colspan(2);

        buttons.button("@editor.apply", Icon.ok, () -> {
            ui.loadAnd(() -> {
                apply();
                hide();
            });
        });
    }

    void apply(){
        ui.loadAnd(() -> {
            editor.clearOp();
            editor.load(() -> {
                var sectorobj = planet.sectors.get(sector);

                //remove presets during generation: massive hack, but it works
                var preset = sectorobj.preset;
                sectorobj.preset = null;

                world.loadSector(sectorobj, seed, false);

                sectorobj.preset = preset;

                editor.updateRenderer();
                state.rules.sector = null;
                //clear extra filters
                editor.tags.put("genfilters", "{}");
            });
        });
    }
}
