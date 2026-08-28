package mindustry.ui.dialogs;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Http.*;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.mod.*;
import mindustry.ui.*;

import java.text.*;
import java.util.*;

import static mindustry.Vars.*;

public class ModBrowserDialog extends BaseDialog{
    protected Table browserTable;

    protected String searchtxt = "";
    protected @Nullable Seq<ModListing> modList;
    protected Seq<Cons<Seq<ModListing>>> fetchCallbacks = new Seq<>();
    protected boolean orderDate = true, fetchingMods;

    protected ObjectMap<String, TextureRegion> textureCache = new ObjectMap<>();

    public ModBrowserDialog(){
        super("@mods.browser");

        cont.table(table -> {
            table.left();
            table.image(Icon.zoom);
            table.field(searchtxt, res -> {
                searchtxt = res;
                rebuildBrowser();
            }).growX().get();
            table.button(Icon.list, Styles.emptyi, 32f, () -> {
                orderDate = !orderDate;
                rebuildBrowser();
            }).update(b -> b.getStyle().imageUp = (orderDate ? Icon.list : Icon.star)).size(40f).get()
            .addListener(new Tooltip(tip -> tip.label(() -> orderDate ? "@mods.browser.sortdate" : "@mods.browser.sortstars").left()));
        }).fillX().padBottom(4);

        cont.row();
        cont.pane(tablebrow -> {
            tablebrow.margin(10f).top();
            browserTable = tablebrow;
        }).scrollX(false);
        addCloseButton();
        makeButtonOverlay();

        onResize(this::rebuildBrowser);

        shown(this::rebuildBrowser);
    }

    public void getModList(Cons<Seq<ModListing>> listener){
        //mods already fetched, use that
        if(modList != null){
            listener.get(modList);
            return;
        }

        //queue more callbacks
        fetchCallbacks.add(listener);

        if(fetchingMods) return;

        getModListInternal(0);
    }

    protected void getModListInternal(int index){
        if(index >= modJsonURLs.length) return;

        //use a custom instance, since this is run in a new thread
        Json json = new Json();
        fetchingMods = true;

        Http.get(modJsonURLs[index], response -> {
            String strResult = response.getResultAsString();

            try{
                Seq<ModListing> mods = json.fromJson(Seq.class, ModListing.class, strResult);

                var d = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                Func<String, Date> parser = text -> {
                    try{
                        return d.parse(text);
                    }catch(Exception e){
                        return new Date();
                    }
                };

                mods.sortComparing(m -> parser.get(m.lastUpdated)).reverse();
                Core.app.post(() -> {
                    fetchingMods = false;
                    modList = mods;

                    fetchCallbacks.each(c -> c.get(mods));
                    fetchCallbacks.clear();
                });
            }catch(Exception e){ //failed to parse, should not happen
                Log.err(e);
                Core.app.post(() -> {
                    fetchCallbacks.clear();
                    fetchingMods = false;
                    ui.showException(e);
                });
            }
        }, error -> {
            if(index < modJsonURLs.length - 1){
                getModListInternal(index + 1);
            }else{
                Core.app.post(() -> { //failed to fetch (different kind of error)
                    fetchCallbacks.clear();
                    fetchingMods = false;
                    ui.mods.showModError(error);
                    hide();
                });
            }
        });
    }


    public void downloadDependencies(Seq<String> toImport, Cons<Seq<String>> imported){
        Seq<String> remaining = toImport.copy();
        getModList(listings -> {
            listings.each(l -> remaining.contains(l.internalName), l -> {
                remaining.remove(l.internalName);
                ui.mods.githubImportMod(l.repo, l.hasJava, true);
            });
            toImport.removeAll(remaining);
            imported.get(toImport);
            displayDependencyImportStatus(remaining, toImport);
        });
    }

    public void displayDependencyImportStatus(Seq<String> failed, Seq<String> success){
        new Dialog(""){{
            setFillParent(true);
            cont.margin(15);

            cont.add("@mod.dependencies.status").color(Pal.accent).center();
            cont.row();
            cont.image().width(300f).pad(2).height(4f).color(Pal.accent);
            cont.row();

            cont.pane(p -> {
                if(success.any()){
                    p.add("@mod.dependencies.success").color(Pal.accent).wrap().fillX().left().labelAlign(Align.left);
                    p.row();
                    p.table(t -> {
                        success.each(d -> {
                            t.add("[accent] > []" + d).wrap().growX().left().labelAlign(Align.left);
                            t.row();
                        });
                    }).growX().padBottom(8f).padLeft(8f);
                    p.row();
                }

                if(failed.any()){
                    p.add("@mod.dependencies.failure").color(Color.scarlet).wrap().fillX().left().labelAlign(Align.left);
                    p.row();
                    p.table(t -> {
                        failed.each(d -> {
                            t.add("[scarlet] > []" + d).wrap().growX().left().labelAlign(Align.left);
                            t.row();
                        });
                    }).growX().padBottom(8f).padLeft(8f);
                }
            }).fillX();
            cont.row();

            if(success.any()){
                cont.image().width(300f).pad(2).height(4f).color(Pal.accent);
                cont.row();
                cont.add("@mods.reloadexit").center();
                cont.row();

                hidden(() -> {
                    Log.info("Exiting to reload mods after dependency auto-import.");
                    Core.app.exit();
                });
            }

            cont.button("@ok", this::hide).size(300, 50);
            closeOnBack();
        }}.show();
    }

    protected void rebuildBrowser(){
        ObjectSet<String> installed = mods.list().map(m -> m.getRepo()).asSet();

        browserTable.clear();
        browserTable.add("@loading");

        int cols = (int)Math.max(Core.graphics.getWidth() / Scl.scl(480), 1);

        getModList(rlistings -> {
            browserTable.clear();
            int i = 0;

            var listings = rlistings;
            if(!orderDate){
                listings = rlistings.copy();
                listings.sortComparing(m1 -> -m1.stars);
            }

            for(ModListing mod : listings){
                if(((mod.hasJava || mod.hasScripts && !mod.iosCompatible) && Vars.ios) ||
                (!Strings.matches(searchtxt, mod.name) && !Strings.matches(searchtxt, mod.repo))
                ) continue;

                float s = 64f;

                browserTable.button(con -> {
                    con.margin(0f);
                    con.left();

                    String repo = mod.repo;
                    con.add(new BorderImage(){
                        TextureRegion last;

                        {
                            border(installed.contains(repo) ? Pal.accent : Color.lightGray);
                            setDrawable(Tex.nomap);
                            pad = Scl.scl(4f);
                        }

                        @Override
                        public void draw(){
                            super.draw();

                            //textures are only requested when the rendering happens; this assists with culling
                            if(!textureCache.containsKey(repo)){
                                textureCache.put(repo, last = Core.atlas.find("nomap"));

                                if(mod.hasIcon){
                                    Fi cacheFolder = Vars.mobile ? Core.files.cache("modIconCache"): dataDirectory.child("modIconCache");
                                    cacheFolder.mkdirs();
                                    Fi cacheFile = cacheFolder.child(Strings.sanitizeFilename(mod.repo + mod.lastUpdated) + ".png");

                                    if(!cacheFile.exists()){ //fetch from Github
                                        ConsT<HttpResponse, Exception> fetch = res -> {
                                            byte[] bytes = res.getResult();
                                            Pixmap pix = new Pixmap(bytes);
                                            cacheFile.writeBytes(bytes);
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
                                        };

                                        String repoName = repo.replace("/", "_");

                                        Http.get("https://raw.githubusercontent.com/Anuken/MindustryMods/master/icons/" + repoName)
                                        .error(err -> {
                                            //github ratelimited the client, try jsdelivr instead
                                            if(!(err instanceof HttpStatusException s && s.status == HttpStatus.NOT_FOUND)){
                                                Http.get("https://cdn.jsdelivr.net/gh/anuken/mindustrymods/icons/" + repoName)
                                                .error(err2 -> {}) //nothing I can do about it
                                                .timeout(15_000)
                                                .submit(fetch);
                                            }
                                        })
                                        .timeout(15_000)
                                        .submit(fetch);
                                    }else{ //load from cache
                                        mainExecutor.submit(() -> {
                                            try{
                                                Pixmap pix = new Pixmap(cacheFile);
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
                                            }catch(Exception e){
                                                Log.err(e);
                                                cacheFile.delete();
                                            }
                                        });
                                    }
                                }
                            }

                            var next = textureCache.get(repo);
                            if(last != next){
                                last = next;
                                setDrawable(next);
                            }
                        }
                    }).size(s).pad(4f * 2f);

                    String infoText =
                    "[accent]" + mod.name.replace("\n", "") +

                    (installed.contains(mod.repo) ? "\n[lightgray]" + Core.bundle.get("mod.installed") : "") +
                    "\n[lightgray]\uE809 " + mod.stars +

                    (!Version.isAtLeast(mod.minGameVersion) ? "\n" + Core.bundle.format("mod.requiresversion", mod.minGameVersion) :
                    ((mod.hasJava && Strings.parseDouble(mod.minGameVersion, 0) < minJavaModGameVersion && !mod.legacyCompatible) ? "\n" + Core.bundle.get("mod.incompatiblemod") : ""));

                    con.add(infoText).width(358f).wrap().grow().pad(4f, 2f, 4f, 6f).top().left().labelAlign(Align.topLeft);

                }, Styles.grayt, () -> {
                    var sel = new BaseDialog(mod.name);
                    sel.cont.pane(p -> p.add(mod.description + "\n\n[accent]" + Core.bundle.get("editor.author") + "[lightgray] " + mod.author)
                    .width(mobile ? 400f : 500f).wrap().pad(4f).labelAlign(Align.center, Align.left)).grow();
                    sel.buttons.defaults().size(150f, 54f).pad(2f);
                    sel.buttons.button("@back", Icon.left, () -> {
                        sel.clear();
                        sel.hide();
                    });

                    var found = mods.list().find(l -> mod.repo != null && mod.repo.equals(l.getRepo()));
                    sel.buttons.button(found == null ? "@mods.browser.add" : "@mods.browser.reinstall", Icon.download, () -> {
                        sel.hide();
                        ui.mods.githubImportMod(mod.repo, mod.hasJava, null, true);
                    });

                    if(Core.graphics.isPortrait()){
                        sel.buttons.row();
                    }

                    sel.buttons.button("@mods.github.open", Icon.link, () -> {
                        Core.app.openURI("https://github.com/" + mod.repo);
                    });

                    sel.buttons.button("@mods.browser.view-releases", Icon.zoom, () -> {
                        BaseDialog load = new BaseDialog("");
                        load.cont.add("[accent]" + Core.bundle.get("mods.browser.fetching"));
                        load.show();
                        Http.get(ghApi + "/repos/" + mod.repo + "/releases", res -> {
                            var json = Jval.read(res.getResultAsString());
                            JsonArray releases = json.asArray();

                            Core.app.post(() -> {
                                load.hide();

                                if(releases.size == 0){
                                    ui.showInfo("@mods.browser.noreleases");
                                }else{
                                    sel.hide();
                                    var downloads = new BaseDialog("@mods.browser.releases");
                                    downloads.cont.pane(p -> {
                                        for(int j = 0; j < releases.size; j++){
                                            var release = releases.get(j);

                                            int index = j;
                                            p.table(((TextureRegionDrawable)Tex.whiteui).tint(Pal.darkestGray), t -> {
                                                t.add("[accent]" + release.getString("name") + (index == 0 ? " " + Core.bundle.get("mods.browser.latest") : "")).top().left().growX().wrap().pad(5f);
                                                t.row();
                                                t.add((release.getString("published_at")).substring(0, 10).replaceAll("-", "/")).top().left().growX().wrap().pad(5f).color(Color.gray);
                                                t.row();
                                                t.table(b -> {
                                                    b.defaults().size(150f, 54f).pad(2f);
                                                    b.button("@mods.github.open-release", Icon.link, () -> Core.app.openURI(release.getString("html_url")));
                                                    b.button("@mods.browser.add", Icon.download, () -> {
                                                        String releaseUrl = release.getString("url");
                                                        ui.mods.githubImportMod(mod.repo, mod.hasJava, releaseUrl.substring(releaseUrl.lastIndexOf("/") + 1), true);
                                                    });
                                                }).right();
                                            }).margin(5f).growX().pad(5f);

                                            if(j < releases.size - 1) p.row();
                                        }
                                    }).width(500f).scrollX(false).fillY();
                                    downloads.buttons.button("@back", Icon.left, () -> {
                                        downloads.clear();
                                        downloads.hide();
                                        sel.show();
                                    }).size(150f, 54f).pad(2f);
                                    downloads.keyDown(KeyCode.escape, downloads::hide);
                                    downloads.keyDown(KeyCode.back, downloads::hide);
                                    downloads.hidden(sel::show);
                                    downloads.show();
                                }
                            });
                        }, t -> Core.app.post(() -> {
                            ui.mods.showModError(t);
                            load.hide();
                        }));
                    });
                    sel.keyDown(KeyCode.escape, sel::hide);
                    sel.keyDown(KeyCode.back, sel::hide);
                    sel.show();
                }).width(438f).pad(4).growX().left().height(s + 8*2f).fillY();

                if(++i % cols == 0) browserTable.row();
            }
        });
    }
}
