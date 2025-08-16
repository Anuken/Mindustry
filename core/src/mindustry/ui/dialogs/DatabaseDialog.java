package mindustry.ui.dialogs;

import arc.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class DatabaseDialog extends BaseDialog{
    private TextField search;
    private Table all = new Table();

    private @Nullable Seq<UnlockableContent> allTabs;
    //sun means "all content"
    private UnlockableContent tab = Planets.sun;

    public DatabaseDialog(){
        super("@database");

        shouldPause = true;
        addCloseButton();
        shown(() -> {
            checkTabList();
            if(state.isCampaign() && allTabs.contains(state.getPlanet())){
                tab = state.getPlanet();
            }else if(state.isGame() && state.rules.planet != null && allTabs.contains(state.rules.planet)){
                tab = state.rules.planet;
            }

            rebuild();
        });
        onResize(this::rebuild);

        all.margin(20).marginTop(0f).marginRight(30f);

        cont.top();
        cont.table(s -> {
            s.image(Icon.zoom).padRight(8);
            search = s.field(null, text -> rebuild()).growX().get();
            search.setMessageText("@players.search");
        }).fillX().padBottom(4).row();

        cont.pane(all).scrollX(false);
    }

    void checkTabList(){
        if(allTabs == null){
            Seq<Content>[] allContent = Vars.content.getContentMap();
            ObjectSet<UnlockableContent> all = new ObjectSet<>();
            for(var contents : allContent){
                for(var content : contents){
                    if(content instanceof UnlockableContent u){
                        all.addAll(u.databaseTabs);
                    }
                }
            }
            allTabs = all.toSeq().sort();
            allTabs.insert(0, Planets.sun);
        }
    }

    void rebuild(){
        checkTabList();

        all.clear();
        var text = search.getText().toLowerCase();

        Seq<Content>[] allContent = Vars.content.getContentMap();

        all.table(t -> {
            int i = 0;
            for(var content : allTabs){
                t.button(content == Planets.sun ? Icon.eyeSmall : content instanceof Planet p ? Icon.icons.get(p.icon, Icon.commandRally) : new TextureRegionDrawable(content.uiIcon), Styles.clearNoneTogglei, iconMed, () -> {
                    tab = content;
                    rebuild();
                }).size(50f).checked(b -> tab == content).tooltip(content == Planets.sun ? "@all" : content.localizedName).with(but -> {
                    but.getStyle().imageUpColor = content instanceof Planet p ? p.iconColor : Color.white.cpy();
                });

                if(++i % 10 == 0) t.row();
            }
        }).row();

        for(int j = 0; j < allContent.length; j++){
            ContentType type = ContentType.all[j];

            Seq<UnlockableContent> array = allContent[j]
                .select(c -> c instanceof UnlockableContent u && !u.isHidden() && !u.hideDatabase && (tab == Planets.sun || u.allDatabaseTabs || u.databaseTabs.contains(tab)) &&
                    (text.isEmpty() || u.localizedName.toLowerCase().contains(text))).as();

            if(array.size == 0) continue;

            //sorting only makes sense when in-game; otherwise, banned blocks can't exist
            if(state.isGame()){
                array.sort(Structs.comps(Structs.comparingBool(UnlockableContent::isBanned), Structs.comparingInt(u -> u.id)));
            }

            all.add("@content." + type.name() + ".name").growX().left().color(Pal.accent);
            all.row();
            all.image().growX().pad(5).padLeft(0).padRight(0).height(3).color(Pal.accent);
            all.row();
            all.table(list -> {
                list.left();

                int cols = (int)Mathf.clamp((Core.graphics.getWidth() - Scl.scl(30)) / Scl.scl(32 + 12), 1, 22);
                int count = 0;

                for(var unlock : array){
                    Image image = unlocked(unlock) ? new Image(new TextureRegionDrawable(unlock.uiIcon), mobile ? Color.white : Color.lightGray).setScaling(Scaling.fit) : new Image(Icon.lock, Pal.gray);

                    //banned cross
                    if(state.isGame() && unlock.isBanned()){
                        list.stack(image, new Image(Icon.cancel){{
                            setColor(Color.scarlet);
                            touchable = Touchable.disabled;
                        }}).size(8 * 4).pad(3);
                    }else{
                        list.add(image).size(8 * 4).pad(3);
                    }

                    ClickListener listener = new ClickListener();
                    image.addListener(listener);
                    if(!mobile && unlocked(unlock)){
                        image.addListener(new HandCursorListener());
                        image.update(() -> image.color.lerp(!listener.isOver() ? Color.lightGray : Color.white, Mathf.clamp(0.4f * Time.delta)));
                    }

                    if(unlocked(unlock)){
                        image.clicked(() -> {
                            if(Core.input.keyDown(KeyCode.shiftLeft) && Fonts.getUnicode(unlock.name) != 0){
                                Core.app.setClipboardText((char)Fonts.getUnicode(unlock.name) + "");
                                ui.showInfoFade("@copied");
                            }else{
                                ui.content.show(unlock);
                            }
                        });
                        image.addListener(new Tooltip(t -> t.background(Tex.button).add(unlock.localizedName + (settings.getBool("console") ? "\n[gray]" + unlock.name : ""))));
                    }

                    if((++count) % cols == 0){
                        list.row();
                    }
                }
            }).growX().left().padBottom(10);
            all.row();
        }

        if(all.getChildren().isEmpty()){
            all.add("@none.found");
        }
    }

    boolean unlocked(UnlockableContent content){
        return (!Vars.state.isCampaign() && !Vars.state.isMenu()) || content.unlocked();
    }
}
