package mindustry.ui.dialogs;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Http.*;
import arc.util.io.*;
import arc.util.serialization.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.mod.*;
import mindustry.mod.Mods.*;
import mindustry.ui.*;

import java.util.*;

import static mindustry.Vars.*;

public class ModsDialog extends BaseDialog{
    public ModBrowserDialog browser;

    protected float modImportProgress;
    protected boolean cancelledImport;
    protected BaseDialog currentContent;

    protected float scroll = 0f;
    //only records mods that have a valid repo!
    protected ObjectMap<LoadedMod, ModListing> modToListing = new ObjectMap<>();
    protected ObjectSet<LoadedMod> withUpdates = new ObjectSet<>();
    protected @Nullable Element updaterElement;

    public ModsDialog(){
        super("@mods");
        addCloseButton();

        buttons.button("@mods.guide", Icon.link, () -> Core.app.openURI(modGuideURL)).size(210, 64f);

        if(!mobile){
            buttons.button("@mods.openfolder", Icon.link, () -> Core.app.openFolder(modDirectory.absolutePath()));
        }

        shown(() -> {
            setup();

            withUpdates.clear();
            refreshModUpdates();
        });
        onResize(this::setup);

        Events.on(ResizeEvent.class, event -> {
            if(currentContent != null){
                currentContent.hide();
                currentContent = null;
            }
        });

        hidden(() -> {
            if(mods.requiresReload()){
                mods.reload();
            }

            if(updaterElement != null){
                updaterElement.remove();
                updaterElement = null;
            }
        });

        browser = new ModBrowserDialog();
    }

    public void refreshModUpdates(){
        ObjectMap<String, LoadedMod> repoToMod = new ObjectMap<>();
        for(var mod : mods.getMods()){
            String repo = mod.getRepo();
            if(!mod.hasSteamID() && repo != null){
                repoToMod.put(repo, mod);
            }
        }

        browser.getModList(list -> {
            withUpdates.clear();
            for(var entry : list){
                var mod = repoToMod.get(entry.repo);
                if(mod != null){
                    modToListing.put(mod, entry);
                    if(Strings.checkNewerSemver(entry.version, mod.meta.version)) withUpdates.add(mod);
                }
            }

            if(withUpdates.size > 0){
                setup();
            }
        });
    }

    public boolean hasUpdate(LoadedMod mod){
        return withUpdates.contains(mod);
    }

    void setup(){
        float h = 110f;
        float w = Math.min(Core.graphics.getWidth() / Scl.scl(1.05f) - Scl.scl(28f), 520f);

        cont.clear();
        cont.defaults().width(Math.min(Core.graphics.getWidth() / Scl.scl(1.05f), 556f)).pad(4);
        cont.add("@mod.reloadrequired").visible(mods::requiresReload).center().get().setAlignment(Align.center);
        cont.row();

        cont.table(buttons -> {
            buttons.left().defaults().growX().height(60f).uniformX();

            TextButtonStyle style = Styles.flatBordert;
            float margin = 12f;

            buttons.button("@mod.import", Icon.add, style, () -> {
                BaseDialog dialog = new BaseDialog("@mod.import");

                TextButtonStyle bstyle = Styles.flatt;

                dialog.cont.table(Tex.button, t -> {
                    t.defaults().size(300f, 70f);
                    t.margin(12f);

                    t.button("@mod.import.file", Icon.file, bstyle, () -> {
                        dialog.hide();

                        FileChooser.open("zip", "jar").submitMulti(files -> {
                            for(var file : files){
                                try{
                                    mods.importMod(file);
                                }catch(Exception e){
                                    ui.showException(e.getMessage() != null && e.getMessage().toLowerCase(Locale.ROOT).contains("writable dex") ? "@error.moddex" : "", e);
                                    Log.err(e);
                                }
                            }

                            setup();
                        });
                    }).margin(12f);

                    t.row();

                    t.button("@mod.import.github", Icon.github, bstyle, () -> {
                        dialog.hide();

                        ui.showTextInput("@mod.import.github", "", 64, Core.settings.getString("lastmod", ""), text -> {
                            //clean up the text in case somebody inputs a URL or adds random spaces
                            text = text.trim().replace(" ", "");
                            if(text.startsWith("https://github.com/")) text = text.substring("https://github.com/".length());

                            Core.settings.put("lastmod", text);
                            //there's no good way to know if it's a java mod here, so assume it's not
                            githubImportMod(text, false, null, true);
                        });
                    }).margin(12f);
                });
                dialog.addCloseButton();

                dialog.show();

            }).margin(margin);

            buttons.button("@mods.browser", Icon.menu, style, () -> browser.show()).margin(margin);
        }).width(w);

        cont.row();

        if(!mods.list().isEmpty()){
            boolean[] anyDisabled = {false};
            Table[] pane = {null};

            Cons<String> rebuild = query -> {
                var cont = pane[0];
                cont.clear();
                boolean any = false;
                for(LoadedMod mod : mods.list()){
                    if(Strings.matches(query, mod.meta.displayName)){
                        any = true;
                        if(!mod.enabled() && !anyDisabled[0] && mods.list().size > 0){
                            anyDisabled[0] = true;
                            cont.row();
                            cont.image().growX().height(4f).pad(6f).color(Pal.gray).row();
                        }

                        cont.button(t -> {
                            t.top().left();
                            t.margin(12f);

                            String stateDetails = getStateDetails(mod);
                            if(stateDetails != null){
                                t.addListener(new Tooltip(f -> f.background(Styles.black8).margin(4f).add(stateDetails).growX().width(400f).wrap()));
                            }

                            t.defaults().left().top();
                            t.table(title1 -> {
                                title1.left();

                                title1.add(new BorderImage(){{
                                    if(mod.iconTexture != null){
                                        setDrawable(new TextureRegion(mod.iconTexture));
                                    }else{
                                        setDrawable(Tex.nomap);
                                    }
                                    border(Pal.accent);
                                }}).size(h - 8f).padTop(-8f).padLeft(-8f).padRight(8f);

                                title1.table(text -> {
                                    boolean hideDisabled = !mod.isSupported() || mod.hasUnmetDependencies() || mod.hasContentErrors();
                                    String shortDesc = mod.meta.shortDescription();

                                    text.add("[accent]" + Strings.stripColors(mod.meta.displayName) + "\n" +
                                        (shortDesc.length() > 0 ? "[lightgray]" + shortDesc + "\n" : "")
                                        //so does anybody care about version?
                                        //+ "[gray]v" + Strings.stripColors(trimText(item.meta.version)) + "\n"
                                        + (mod.enabled() || hideDisabled ? "" : Core.bundle.get("mod.disabled") + ""))
                                    .wrap().top().width(300f).growX().left();

                                    text.row();

                                    String state = getStateText(mod);
                                    if(state != null){
                                        text.labelWrap(state).growX().row();
                                    }
                                }).top().growX();

                                title1.add().growX();
                            }).growX().growY().left();

                            t.table(right -> {
                                right.right();
                                right.button(mod.enabled() ? Icon.downOpen : Icon.upOpen, Styles.clearNonei, () -> {
                                    mods.setEnabled(mod, !mod.enabled());
                                    setup();
                                }).size(50f).disabled(!mod.isSupported());

                                right.button(mod.hasSteamID() ? Icon.link : Icon.trash, Styles.clearNonei, () -> {
                                    if(!mod.hasSteamID()){
                                        ui.showConfirm("@confirm", "@mod.remove.confirm", () -> {
                                            mods.removeMod(mod);
                                            withUpdates.remove(mod);
                                            setup();
                                        });
                                    }else{
                                        platform.viewListing(mod);
                                    }
                                }).size(50f);

                                if(steam && !mod.hasSteamID()){
                                    right.row();
                                    right.button(Icon.export, Styles.clearNonei, () -> {
                                        platform.publish(mod);
                                    }).size(50f);
                                }
                            }).growX().right().padRight(-8f).padTop(-8f);
                        }, Styles.grayt, () -> showMod(mod)).size(w, h).growX().pad(4f).padTop(8f).row();

                        if(hasUpdate(mod)){
                            cont.button(b -> {
                                b.margin(6f);
                                b.left();
                                b.image(Icon.download).color(Color.lightGray).size(iconMed).padRight(8f);
                                var list = modToListing.get(mod);
                                b.add(Core.bundle.format("mods.update.available", list == null ? "<unknown>" : list.version));
                            }, Styles.grayt, () -> {
                                githubImportMod(mod.getRepo(), mod.isJava(), null, false);
                            }).width(w).height(48f).padTop(-4f).row();
                        }
                    }
                }

                if(!any){
                    cont.add("@none.found").color(Color.lightGray).pad(4);
                }
            };

            if(!mobile || Core.graphics.isPortrait()){
                cont.table(search -> {
                    search.image(Icon.zoom).padRight(8f);
                    search.field("", rebuild).growX();
                }).fillX().padBottom(4);
            }

            cont.row();
            cont.pane(table1 -> {
                pane[0] = table1.margin(10f).top();
                rebuild.get("");
            }).scrollX(false).update(s -> scroll = s.getScrollY()).get().setScrollYForce(scroll);

            cont.row();

            if(withUpdates.size > 1){
                cont.button(b -> {
                    b.margin(6f);
                    b.image(Icon.download).size(iconMed).padRight(8f);
                    b.add("@mods.update.all");
                    b.image(Icon.download).size(iconMed).padLeft(8f);
                }, Styles.grayt, () -> {
                    var queue = withUpdates.toSeq();

                    int[] index = {0};

                    //TODO: this is an awful hack that forces mods to be downloaded in sequence
                    //handling callbacks in the functions is extremely tedious, so this is the cleanest method I could find
                    cancelledImport = false;
                    if(updaterElement != null) updaterElement.remove();
                    updaterElement = new Element();

                    updaterElement.update(() -> {
                        if(index[0] >= queue.size || cancelledImport){
                            updaterElement.remove();
                        }else if(!ui.loadfrag.shown()){ //loading not shown, queue next one
                            var next = queue.get(index[0] ++);

                            githubImportMod(next.getRepo(), next.isJava(), null, false);
                        }
                    });
                    addChild(updaterElement);
                }).padTop(8f).width(w).height(60f).padTop(12f).row();
            }
        }else{
            cont.table(Styles.black6, t -> t.add("@mods.none")).height(80f);
        }

        cont.row();
    }

    private @Nullable String getStateText(LoadedMod item){
        if(item.isOutdated()){
            return "@mod.incompatiblemod";
        }else if(item.isBlacklisted()){
            return "@mod.blacklisted";
        }else if(!item.isSupported()){
            return "@mod.incompatiblegame";
        }else if(item.state == ModState.circularDependencies){
            return "@mod.circulardependencies";
        }else if(item.state == ModState.incompleteDependencies){
            return "@mod.incompletedependencies";
        }else if(item.hasUnmetDependencies()){
            return "@mod.unmetdependencies";
        }else if(item.hasContentErrors()){
            return "@mod.erroredcontent";
        }else if(item.meta.hidden){
            return "@mod.multiplayer.compatible";
        }
        return null;
    }

    private @Nullable String getStateDetails(LoadedMod item){
        if(item.isOutdated()){
            return "@mod.incompatiblemod.details";
        }else if(item.isBlacklisted()){
            return "@mod.blacklisted.details";
        }else if(!item.isSupported()){
            return Core.bundle.format("mod.requiresversion.details", item.meta.minGameVersion);
        }else if(item.state == ModState.circularDependencies){
            return "@mod.circulardependencies.details";
        }else if(item.state == ModState.incompleteDependencies){
            return Core.bundle.format("mod.incompletedependencies.details", item.missingDependencies.toString(", "));
        }else if(item.hasUnmetDependencies()){
            return Core.bundle.format("mod.missingdependencies.details", item.missingDependencies.toString(", "));
        }else if(item.hasContentErrors()){
            return "@mod.erroredcontent.details";
        }
        return null;
    }

    private void showMod(LoadedMod mod){
        BaseDialog dialog = new BaseDialog(mod.meta.displayName);

        dialog.addCloseButton();

        if(!mobile){
            dialog.buttons.button("@mods.openfolder", Icon.link, () -> Core.app.openFolder(mod.file.absolutePath()));
        }

        if(mod.getRepo() != null){
            boolean showImport = !mod.hasSteamID();
            dialog.buttons.button("@mods.github.open", Icon.link, () -> Core.app.openURI("https://github.com/" + mod.getRepo()));
            if(mobile && showImport) dialog.buttons.row();
            if(showImport) dialog.buttons.button("@mods.browser.reinstall", Icon.download, () -> githubImportMod(mod.getRepo(), mod.isJava(), null, false));
        }

        dialog.cont.pane(desc -> {
            desc.center();
            desc.defaults().padTop(10).left();

            desc.add("@editor.name").padRight(10).color(Color.gray).padTop(0);
            desc.row();
            desc.add(mod.meta.displayName).growX().wrap().padTop(2);
            desc.row();
            if(mod.meta.author != null){
                desc.add("@editor.author").padRight(10).color(Color.gray);
                desc.row();
                desc.add(mod.meta.author).growX().wrap().padTop(2);
                desc.row();
            }
            if(mod.meta.version != null){
                desc.add("@mod.version").padRight(10).color(Color.gray).top();
                desc.row();
                desc.add(mod.meta.version).growX().wrap().padTop(2);
                desc.row();
            }
            if(mod.meta.description != null){
                desc.add("@editor.description").padRight(10).color(Color.gray).top();
                desc.row();
                desc.add(mod.meta.description).growX().wrap().padTop(2);
                desc.row();
            }

            String state = getStateDetails(mod);

            if(state != null){
                desc.add("@mod.disabled").padTop(13f).padBottom(-6f).row();
                desc.add(state).growX().wrap().row();
            }

        }).width(400f);

        Seq<UnlockableContent> all = Seq.with(content.getContentMap()).<Content>flatten().select(c -> c.minfo.mod == mod && c instanceof UnlockableContent u && !u.isHidden()).as();
        if(all.any()){
            dialog.cont.row();
            dialog.cont.button("@mods.viewcontent", Icon.book, () -> {
                BaseDialog d = new BaseDialog(mod.meta.displayName);
                d.cont.pane(cs -> {
                    int i = 0;
                    for(UnlockableContent c : all){
                        cs.button(new TextureRegionDrawable(c.uiIcon), Styles.flati, iconMed, () -> {
                            ui.content.show(c);
                        }).size(50f).with(im -> {
                            var click = im.getClickListener();
                            im.update(() -> im.getImage().color.lerp(!click.isOver() ? Color.lightGray : Color.white, 0.4f * Time.delta));

                        }).tooltip(c.localizedName);

                        if(++i % (int)Math.min(Core.graphics.getWidth() / Scl.scl(110), 14) == 0) cs.row();
                    }
                }).grow();
                d.addCloseButton();
                d.show();
                currentContent = d;
            }).size(300, 50).pad(4);
        }

        dialog.show();
    }

    protected void handleMod(String repo, HttpResponse result, boolean forceEnable){
        try{
            Fi file = tmpDirectory.child(repo.replace("/", "") + ".zip");
            long len = result.getContentLength();
            Floatc cons = len <= 0 ? f -> {} : p -> modImportProgress = p;

            try(var stream = file.write(false)){
                Streams.copyProgress(result.getResultAsStream(), stream, len, 4096, p -> {
                    if(cancelledImport) throw new RuntimeException("cancelled");
                    cons.get(p);
                });
            }

            if(cancelledImport) return;

            var mod = mods.importMod(file, forceEnable);
            mod.setRepo(repo);
            file.delete();
            Core.app.post(() -> {
                var same = withUpdates.toSeq().find(l -> Structs.eq(l.getRepo(), repo));
                if(same != null) withUpdates.remove(same);

                try{
                    setup();
                    ui.loadfrag.hide();
                }catch(Throwable e){
                    ui.showException(e);
                }
            });
        }catch(Throwable e){
            if(cancelledImport) return;
            showModError(e);
        }
    }

    protected void importFail(Throwable t){
        Core.app.post(() -> showModError(t));
    }

    public void showModError(Throwable error){
        ui.loadfrag.hide();

        if(error instanceof NoSuchMethodError || Strings.getCauses(error).contains(t -> t.getMessage() != null && (t.getMessage().contains("trust anchor") || t.getMessage().contains("SSL") || t.getMessage().contains("protocol")))){
            ui.showErrorMessage("@feature.unsupported");
        }else if(error instanceof HttpStatusException st){
            ui.showErrorMessage(Core.bundle.format("connectfail", Strings.capitalize(st.status.toString().toLowerCase())));
        }else if(error.getMessage() != null && error.getMessage().toLowerCase(Locale.ROOT).contains("writable dex")){
            ui.showException("@error.moddex", error);
        }else{
            ui.showException(error);
        }
    }

    public void githubImportMod(String repo, boolean isJava, boolean forceEnable){
        githubImportMod(repo, isJava, null, forceEnable);
    }

    public void githubImportMod(String repo, boolean isJava, @Nullable String release, boolean forceEnable){
        modImportProgress = 0f;
        cancelledImport = false;
        ui.loadfrag.show(Core.bundle.format("mods.downloading", repo));
        ui.loadfrag.setProgress(() -> modImportProgress);
        ui.loadfrag.setButton(() -> {
            ui.loadfrag.hide();
            cancelledImport = true;
        });

        if(isJava){
            githubImportJavaMod(repo, release, forceEnable);
        }else{
            Http.get(ghApi + "/repos/" + repo, res -> {
                if(cancelledImport) return;
                var json = Jval.read(res.getResultAsString());
                String mainBranch = json.getString("default_branch");
                String language = json.getString("language", "<none>");

                //this is a crude heuristic for class mods; only required for direct github import
                //TODO make a more reliable way to distinguish java mod repos
                if(language.equals("Java") || language.equals("Kotlin") || language.equals("Groovy") || language.equals("Scala")){
                    githubImportJavaMod(repo, release, forceEnable);
                }else{
                    githubImportBranch(mainBranch, repo, release, forceEnable);
                }
            }, this::importFail);
        }
    }

    public void githubImportJavaMod(String repo, @Nullable String release, boolean forceEnable){
        //grab latest release
        Http.get(ghApi + "/repos/" + repo + "/releases/" + (release == null ? "latest" : release), res -> {
            if(cancelledImport) return;
            var json = Jval.read(res.getResultAsString());
            var assets = json.get("assets").asArray();

            //prioritize dexed jar, as that's what Sonnicon's mod template outputs
            var dexedAsset = assets.find(j -> j.getString("name").startsWith("dexed") && j.getString("name").endsWith(".jar"));
            var asset = dexedAsset == null ? assets.find(j -> j.getString("name").endsWith(".jar")) : dexedAsset;

            if(asset != null){
                //grab actual file
                var url = asset.getString("browser_download_url");

                Http.get(url, result -> {
                    if(cancelledImport) return;
                    handleMod(repo, result, forceEnable);
                }, this::importFail);
            }else{
                throw new ArcRuntimeException("No JAR file found in releases. Make sure you have a valid jar file in the mod's latest Github Release.");
            }
        }, this::importFail);
    }

    public void githubImportBranch(String branch, String repo, @Nullable String release, boolean forceEnable){
        if(release != null) {
            Http.get(ghApi + "/repos/" + repo + "/releases/" + release, res -> {
                if(cancelledImport) return;
                String zipUrl = Jval.read(res.getResultAsString()).getString("zipball_url");
                Http.get(zipUrl, loc -> {
                    if(cancelledImport) return;
                    if(loc.getHeader("Location") != null){
                        Http.get(loc.getHeader("Location"), result -> {
                            if(cancelledImport) return;
                            handleMod(repo, result, forceEnable);
                        }, this::importFail);
                    }else{
                        handleMod(repo, loc, forceEnable);
                    }
                }, this::importFail);
            });
        }else{
            Http.get(ghApi + "/repos/" + repo + "/zipball/" + branch, loc -> {
                if(cancelledImport) return;
                if(loc.getHeader("Location") != null){
                    Http.get(loc.getHeader("Location"), result -> {
                        if(cancelledImport) return;
                        handleMod(repo, result, forceEnable);
                    }, this::importFail);
                }else{
                    handleMod(repo, loc, forceEnable);
                }
            }, this::importFail);
        }
    }
}
