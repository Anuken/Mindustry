package mindustry.ui.dialogs;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.scene.style.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.service.Achievement;
import mindustry.ui.*;

import java.io.*;
import java.text.*;
import java.util.*;

import static mindustry.Vars.*;

public class AchievementsDialog extends BaseDialog{
    private ObjectMap<String, TextureRegion> textureCache = new ObjectMap<>();
    private String searchtxt = "";
    private Table browserTable;


    public AchievementsDialog(){
        super("@achievements");

        cont.table(table -> {
            table.left();
            table.image(Icon.zoom);
            table.field(searchtxt, res -> {
                searchtxt = res;
                rebuildBrowser();
            }).growX().get();
        }).fillX().padBottom(4);

        cont.row();
        cont.pane(tablebrow -> {
            tablebrow.margin(10f).top();
            browserTable = tablebrow;
        }).scrollX(false);
        addCloseButton();

        shown(this::rebuildBrowser);
        onResize(this::rebuildBrowser);
    }

    private void rebuildBrowser(){
        browserTable.clear();

        int cols = (int)Math.max(Core.graphics.getWidth() / Scl.scl(480), 1);

        int i = 0;
        float s = 64f;

        for( Achievement ach : Achievement.all){
            String name = Core.bundle.get("achievement." + ach.name() + ".name");
            String desc = Core.bundle.get("achievement." + ach.name() + ".desc");
            if( !Strings.matches(searchtxt, name) && !Strings.matches(searchtxt, desc) ) continue;

            browserTable.button(con -> {
                con.setColor(ach.isAchieved() ? Pal.accent : Color.lightGray);
                con.margin(0f);
                con.left();

                con.add(new BorderImage(){
                    TextureRegion last;

                    {
                        border(ach.isAchieved() ? Pal.accent : Color.lightGray);
                        setDrawable(Tex.nomap);
                        pad = Scl.scl(4f);
                    }

                    @Override
                    public void draw(){
                        super.draw();

                        //TODO draw the sprite of the achievement

                        //textures are only requested when the rendering happens; this assists with culling
                        /*
                        if(!textureCache.containsKey(repo)){
                            textureCache.put(repo, last = Core.atlas.find("nomap"));
                            Http.get("https://raw.githubusercontent.com/Anuken/MindustryMods/master/icons/" + repo.replace("/", "_"), res -> {
                                Pixmap pix = new Pixmap(res.getResult());
                                Core.app.post(() -> {
                                    try{
                                        var tex = new Texture(pix);
                                        tex.setFilter(TextureFilter.linear);
                                        textureCache.put(repo, new TextureRegion(tex));
                                        pix.dispose();
                                    }catch(Exception e){
                                        Log.err(e);
                                    }
                                });
                            }, err -> {});
                        }

                        var next = textureCache.get(repo);
                        if(last != next){
                            last = next;
                            setDrawable(next);
                        }
                        */
                    }
                }).size(s).pad(4f * 2f);

                con.add("[accent]" + name +
                        "\n[lightgray]" + desc
                ).width(358f).wrap().grow().pad(4f, 2f, 4f, 6f).top().left().labelAlign(Align.topLeft);

            }, Styles.flatBordert,() -> {}).padRight(4f);

            if(++i % cols == 0) browserTable.row();
        }
    }
}