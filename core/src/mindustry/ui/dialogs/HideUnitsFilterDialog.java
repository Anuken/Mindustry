package mindustry.ui.dialogs;

import arc.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.type.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class HideUnitsFilterDialog extends BaseDialog{
    private static final ObjectSet<String> excluded = new ObjectSet<>();

    public static void load(){
        excluded.clear();
        for(String name : Core.settings.getString("hideunits.excluded", "").split(",")){
            if(!name.isEmpty()){
                excluded.add(name);
            }
        }
    }

    public static boolean affects(UnitType type){
        return !excluded.contains(type.name);
    }

    public static void affects(UnitType type, boolean value){
        if(value ? excluded.remove(type.name) : excluded.add(type.name)){
            StringBuilder sb = new StringBuilder();
            for(String name : excluded){
                if(!sb.isEmpty()) sb.append(',');
                sb.append(name);
            }
            Core.settings.put("hideunits.excluded", sb.toString());
        }
    }

    public HideUnitsFilterDialog(){
        super("@hideunits.title");
        load();
        addCloseButton();
        shown(this::rebuild);
        onResize(this::rebuild);
    }

    void rebuild(){
        cont.clear();

        Seq<UnitType> types = Vars.content.units().select(type -> !type.isHidden());

        cont.add("[lightgray]" + Core.bundle.get("hideunits.info")).wrap().width(460f).center().padBottom(20f).row();

        cont.table(buttons -> {
            buttons.defaults().size(150f, 48f).pad(4f);
            buttons.button("@addall", () -> {
                for(UnitType type : types) affects(type, true);
            });
            buttons.button("@clear", () -> {
                for(UnitType type : types) affects(type, false);
            });
        }).padBottom(20f).row();

        cont.pane(grid -> {
            int i = 0, cols = 8;
            for(UnitType type : types){
                ImageButton button = grid.button(new TextureRegionDrawable(type.uiIcon), Styles.clearTogglei, 45, () ->
                    affects(type, !affects(type))).size(45 + 16).pad(2f).tooltip(type.localizedName).get();

                button.getImage().setScaling(Scaling.fit);
                button.update(() -> button.setChecked(affects(type)));

                if(++i % cols == 0) grid.row();
            }
        }).top();
    }
}
