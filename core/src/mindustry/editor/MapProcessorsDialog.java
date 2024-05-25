package mindustry.editor;

import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.logic.LogicBlock.*;

import static mindustry.Vars.*;

public class MapProcessorsDialog extends BaseDialog{
    private IconSelectDialog iconSelect = new IconSelectDialog();
    private TextField search;
    private Seq<Building> processors = new Seq<>();
    private Table list;

    public MapProcessorsDialog(){
        super("@editor.worldprocessors");

        shown(this::setup);

        addCloseButton();
        buttons.button("@add", Icon.add, () -> {
            boolean foundAny = false;

            outer:
            for(int y = 0; y < Vars.world.height(); y++){
                for(int x = 0; x < Vars.world.width(); x++){
                    Tile tile = Vars.world.rawTile(x, y);
                    if(!tile.synthetic()){
                        foundAny = true;
                        tile.setNet(Blocks.worldProcessor, Team.sharded, 0);
                        if(ui.editor.isShown()){
                            Vars.editor.renderer.updatePoint(x, y);
                        }
                        break outer;
                    }
                }
            }

            if(!foundAny){
                ui.showErrorMessage("@editor.worldprocessors.nospace");
            }else{
                setup();
            }
        }).size(210f, 64f);

        cont.top();
        getCell(cont).grow();

        cont.table(s -> {
            s.image(Icon.zoom).padRight(8);
            search = s.field(null, text -> rebuild()).growX().get();
            search.setMessageText("@players.search");
        }).width(440f).fillX().padBottom(4).row();

        cont.pane(t -> {
            list = t;
        });
    }

    private void rebuild(){
        list.clearChildren();

        if(processors.isEmpty()){
            list.add("@editor.worldprocessors.none");
        }else{
            Table t = list;
            var text = search.getText().toLowerCase();

            t.defaults().pad(4f);
            float h = 50f;
            for(var build : processors){
                if(build instanceof LogicBuild log && (text.isEmpty() || (log.tag != null && log.tag.toLowerCase().contains(text)))){

                    t.button(log.iconTag == 0 ? Styles.none : new TextureRegionDrawable(Fonts.getLargeIcon(Fonts.unicodeToName(log.iconTag))), Styles.graySquarei, iconMed, () -> {
                        iconSelect.show(ic -> {
                            log.iconTag = (char)ic;
                            rebuild();
                        });
                    }).size(h);

                    t.button((log.tag == null ? "<no name>\n" : "[accent]" + log.tag + "\n") + "[lightgray][[" + log.tile.x + ", " + log.tile.y + "]", Styles.grayt, () -> {
                        //TODO: bug: if you edit name inside of the edit dialog, it won't show up in the list properly
                        log.showEditDialog(true);
                    }).size(Vars.mobile ? 390f : 450f, h).margin(10f).with(b -> {
                        b.getLabel().setAlignment(Align.left, Align.left);
                    });

                    t.button(Icon.pencil, Styles.graySquarei, Vars.iconMed, () -> {
                        ui.showTextInput("", "@editor.name", LogicBlock.maxNameLength, log.tag == null ? "" : log.tag, tag -> {
                            //bypass configuration and set it directly in case privileged checks mess things up
                            log.tag = tag;
                            setup();
                        });
                    }).size(h);

                    if(Vars.state.isGame() && state.isEditor()){
                        t.button(Icon.eyeSmall, Styles.graySquarei, Vars.iconMed, () -> {
                            hide();
                            control.input.config.showConfig(build);
                            control.input.panCamera(Tmp.v1.set(build));
                        }).size(h);
                    }

                    t.button(Icon.trash, Styles.graySquarei, iconMed, () -> {
                        ui.showConfirm("@editor.worldprocessors.delete.confirm",  () -> {
                            boolean surrounded = true;
                            for(int i = 0; i < 4; i++){
                                Tile other = build.tile.nearby(i);
                                if(other != null && !(other.block().privileged || other.block().isStatic())){
                                    surrounded = false;
                                    break;
                                }
                            }
                            if(surrounded){
                                build.tile.setNet(build.tile.floor().wall instanceof StaticWall ? build.tile.floor().wall : Blocks.stoneWall);
                            }else{
                                build.tile.setNet(Blocks.air);
                            }
                            processors.remove(build);
                            rebuild();
                        });
                    }).size(h);

                    t.row();
                }
            }
        }
    }

    private void setup(){

        processors.clear();

        //scan the entire world for processor (Groups.build can be empty, indexer is probably inaccurate)
        Vars.world.tiles.eachTile(t -> {
            if(t.isCenter() && t.block() == Blocks.worldProcessor){
                processors.add(t.build);
            }
        });

        rebuild();
    }
}
