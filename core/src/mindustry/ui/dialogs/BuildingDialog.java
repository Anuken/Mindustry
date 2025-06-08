package mindustry.ui.dialogs;

import arc.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.input.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.type.Category;
import mindustry.ui.*;
import mindustry.ui.fragments.PlacementFragment;
import mindustry.world.Block;

import java.util.regex.*;

import static mindustry.Vars.*;

public class BuildingDialog extends BaseDialog{
    private Block firstBuilding;
    private String search = "";
    private TextField searchField;
    private Runnable rebuildPane = () -> {
    };
    private Pattern ignoreSymbols = Pattern.compile("[`~!@#$%^&*()\\-_=+{}|;:'\",<.>/?]");


    public BuildingDialog(){
        super("@buildings");
        Core.assets.load("sprites/schematic-background.png", Texture.class).loaded = t -> t.setWrap(TextureWrap.repeat);

        shouldPause = true;
        addCloseButton();
        makeButtonOverlay();
        shown(this::setup);
        onResize(this::setup);
    }

    void setup(){
        search = "";

        cont.top();
        cont.clear();

        cont.table(s -> {
            s.left();
            s.image(Icon.zoom);
            searchField = s.field(search, res -> {
                search = res;
                rebuildPane.run();
            }).growX().get();
            searchField.setMessageText("@building.search");
            searchField.clicked(KeyCode.mouseRight, () -> {
                if(!search.isEmpty()){
                    search = "";
                    searchField.clearText();
                    rebuildPane.run();
                }
            });
        }).fillX().padBottom(4);

        cont.row();
        cont.pane(t -> {
            t.top();
            t.update(() -> {
                if(Core.input.keyTap(Binding.chat) && Core.scene.getKeyboardFocus() == searchField && firstBuilding != null){
                    control.input.block = firstBuilding;
                    hide();
                }
            });

            rebuildPane = () -> {
                int cols = Math.max((int)(Core.graphics.getWidth() / Scl.scl(230)), 1);

                t.clear();
                int i = 0;
                String searchString = ignoreSymbols.matcher(search.toLowerCase()).replaceAll("");

                // get all player placeable blocks
                Seq<Block> placeableBlocks = new Seq<>();
                PlacementFragment f = new PlacementFragment();
                for(Category c : Category.all){
                    Seq<Block> blocks = f.getUnlockedByCategory(c);
                    placeableBlocks.add(blocks);
                }

                firstBuilding = null;
                for(Block block : placeableBlocks){
                    //make sure search fits
                    if(!search.isEmpty() && !ignoreSymbols.matcher(block.name.toLowerCase()).replaceAll("").contains(searchString))
                        continue;
                    if(firstBuilding == null) firstBuilding = block;

                    Button[] sel = {null};
                    sel[0] = t.button(b -> {
                        b.top();
                        b.margin(0f);
                        b.table(building -> {
                            Label label = building.add(block.name).style(Styles.outlineLabel).color(Color.white).top().growX().maxWidth(200f - 8f).get();
                            label.setEllipsis(true);
                            label.setAlignment(Align.center);
                        }).growX().height(50f);
                        b.row();
                        b.name = block.name;
                        b.stack(new Image(block.uiIcon).setScaling(Scaling.fit)).size(200f);
                    }, () -> {
                        if(sel[0].childrenPressed()) return;
                        control.input.block = block;
                        hide();
                    }).pad(4).style(Styles.flati).get();

                    sel[0].getStyle().up = Tex.pane;

                    if(++i % cols == 0){
                        t.row();
                    }
                }

                if(firstBuilding == null){
                    if(!searchString.isEmpty()){
                        t.add("@none.found");
                    }else{
                        t.add("@none").color(Color.lightGray);
                    }
                }
            };

            rebuildPane.run();
        }).grow().scrollX(false);
    }


    @Override
    public Dialog show(){
        super.show();

        if(Core.app.isDesktop() && searchField != null){
            Core.scene.setKeyboardFocus(searchField);
        }

        return this;
    }
}