package mindustry.editor;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.graphics.g2d.*;
import arc.scene.ui.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.mod.data.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import java.io.*;
import java.util.concurrent.*;
import java.util.zip.*;

import static mindustry.Vars.*;
import static mindustry.mod.DataImagePacker.*;

public class MapPatchImagesDialog extends BaseDialog{
    TextField searchField;
    @Nullable String searchString;
    Table inner;

    public MapPatchImagesDialog(){
        super("@editor.patches.images");

        cont.table(search -> {
            search.image(Icon.zoom);

            searchField = search.field("", t -> {
                searchString = t.length() > 0 ? t.toLowerCase() : null;
                rebuild();
            }).growX().get();
            searchField.setMessageText("@search");
        }).growX().row();

        cont.pane(t -> inner = t).grow();

        addCloseButton();

        buttons.button("@add", Icon.add, () -> {
            platform.showFileChooser(true, "png", result -> {
                try{
                    Pixmap pix = new Pixmap(result);
                    int width = pix.width;
                    int height = pix.height;
                    Pixmaps.bleed(pix);
                    byte[] bytes = PixmapIO.writePngBytes(pix);
                    pix.dispose();

                    if(width > DataPatcher.maxImageSize || height > DataPatcher.maxImageSize){
                        ui.showErrorMessage(Core.bundle.format("editor.patches.image.toolarge", width, height, DataPatcher.maxImageSize, DataPatcher.maxImageSize));
                        return;
                    }

                    var images = getImages();

                    //path and name are the same here; there's no path context.
                    String name = result.nameWithoutExtension();
                    String path = name;
                    var other = images.find(p -> (p.path.equalsIgnoreCase(path) || p.name.equalsIgnoreCase(name)));
                    if(other != null){
                        ui.showErrorMessage(Core.bundle.format("editor.patches.image.exists", other.name + " (" + other.path + ")"));
                        return;
                    }

                    byte[] hash = assetCache.add(bytes);

                    images.add(new ImageAsset(path, hash));
                    images.sort();
                    state.data.reloadImages(images);
                    rebuild();
                }catch(Exception e){
                    ui.showException(e);
                }
            });
        }).size(190f, 64f);

        buttons.button("@edit", Icon.menu, () -> {
            BaseDialog dialog = new BaseDialog("@waves.edit");
            dialog.cont.pane(p -> {
                p.margin(10f);
                p.table(Tex.button, t -> {
                    TextButtonStyle style = Styles.flatt;
                    t.defaults().size(280f, 60f).left();
                    t.button("@editor.patches.image.importzip", Icon.download, style, () -> platform.showFileChooser(true, "zip", file -> {

                        //Android doesn't allow accessing the file contents outside the callback; other platforms either copy it already, or don't have dumb permission issues
                        Fi targetFile;
                        if(OS.isAndroid){
                            targetFile = tmpDirectory.child(file.name());
                            file.copyTo(targetFile);
                        }else{
                            targetFile = file;
                        }

                        dialog.hide();
                        ui.loadAnd(() -> {
                            try{
                                class Result{
                                    int width, height;
                                    Fi path;
                                    byte[] hash; //null if width/height invalid
                                }

                                Seq<Future<?>> images = new Seq<>();
                                var errors = new CopyOnWriteArrayList<String>();

                                Fi zipped = new ZipFi(targetFile);
                                zipped.walk(ifile -> {
                                    if(ifile.extEquals("png")){
                                        images.add(mainExecutor.submit(() -> {
                                            try{
                                                Result res = new Result();
                                                res.path = ifile;
                                                byte[] bytes = ifile.readBytes();
                                                Pixmap pix = new Pixmap(bytes);
                                                res.width = pix.width;
                                                res.height = pix.height;

                                                if(res.width < DataPatcher.maxImageSize && res.height < DataPatcher.maxImageSize){
                                                    Pixmaps.bleed(pix);
                                                    bytes = PixmapIO.writePngBytes(pix);
                                                    res.hash = assetCache.add(bytes);
                                                }

                                                pix.dispose();

                                                return res;
                                            }catch(Throwable error){
                                                errors.add("[accent]" + ifile.path() + "[white]: " + Strings.getSimpleMessage(error));
                                                return null;
                                            }
                                        }));
                                    }
                                });

                                Threads.awaitAll(images);
                                zipped.delete(); //closes the zip file
                                int imported = 0;

                                for(var future : images){
                                    var image = (Result)future.get();
                                    if(image != null){
                                        if(image.width > DataPatcher.maxImageSize || image.height > DataPatcher.maxImageSize || image.hash == null){
                                            errors.add("[accent]" + image.path + "[white]: " + Core.bundle.format("editor.patches.image.toolarge", width, height, DataPatcher.maxImageSize, DataPatcher.maxImageSize));
                                            continue;
                                        }

                                        String path = image.path.pathWithoutExtension(), name = image.path.nameWithoutExtension();
                                        var other = getImages().find(op -> (op.path.equalsIgnoreCase(path) || op.name.equalsIgnoreCase(name)));
                                        if(other != null){
                                            errors.add("[accent]" + image.path + "[white]: " +Core.bundle.format("editor.patches.image.exists", other.name + " (" + other.path + ")").replace("\n", " "));
                                            continue;
                                        }
                                        getImages().add(new ImageAsset(path, image.hash));
                                        imported ++;
                                    }
                                }

                                getImages().sort();

                                state.data.reloadImages(getImages());
                                rebuild();

                                var idiag = new BaseDialog("@editor.patches.image.imports");
                                if(imported > 0) idiag.cont.add(Core.bundle.format("editor.patches.image.imported", imported)).row();
                                if(!errors.isEmpty()){
                                    idiag.cont.add("@editor.patches.image.error").padBottom(10f).row();
                                    idiag.cont.pane(ep -> {
                                        ep.add(Seq.with(errors).toString("\n", s -> "[gray]- []" + s)).labelAlign(Align.left, Align.left).grow();
                                    });
                                }else if(imported == 0){
                                    idiag.cont.add("@editor.patches.image.none");
                                }
                                idiag.buttons.button("@ok", Icon.ok, idiag::hide).size(200f, 64f);
                                idiag.show();
                            }catch(Throwable e){
                                ui.showException(e);
                            }
                        });
                    })).marginLeft(12f).row();
                    t.button("@editor.patches.image.exportzip", Icon.upload, style, () -> platform.showFileChooser(false, "zip", file -> {
                        dialog.hide();
                        try{
                            try(OutputStream fos = file.write(false, 4096); ZipOutputStream zos = new ZipOutputStream(fos)){
                                for(var image : getImages()){
                                    Fi cacheFile = image.getCacheFile();
                                    if(cacheFile == null) continue;

                                    zos.putNextEntry(new ZipEntry(image.path + ".png"));
                                    zos.write(cacheFile.readBytes());
                                    zos.closeEntry();
                                }
                            }
                        }catch(Throwable e){
                            ui.showException(e);
                        }
                    })).marginLeft(12f).row();
                    t.button("@editor.patches.image.clearall", Icon.trash, style, () -> {
                        dialog.hide();
                        ui.showConfirm("@editor.patches.image.clearall.confirm", () -> {
                            getImages().clear();
                            state.data.reloadImages(getImages());
                            rebuild();
                        });
                    }).marginLeft(12f).row();
                });
            });

            dialog.addCloseButton();
            dialog.show();
        });

        shown(() -> {
            searchString = null;
            searchField.setText("");
            rebuild();
        });

        makeButtonOverlay();
    }

    void rebuild(){
        inner.clearChildren();
        inner.top().left();

        float size = 200f;
        int cols = (int)Math.max(1, Core.graphics.getWidth() / Scl.scl(size + 12f));
        int i = 0;
        for(var image : getImages()){
            if(searchString != null && !image.path.toLowerCase().contains(searchString)) continue;
            TextureRegion region = Core.atlas.find(regionPrefix + image.name, "nomap");
            boolean found = Core.atlas.has(regionPrefix + image.name);
            @Nullable Fi cacheFile = image.getCacheFile();
            int iwidth = found ? region.width : 1, iheight = found ? region.height : 1;
            int ilength = cacheFile != null && cacheFile.exists() ? (int)cacheFile.length() : 0;

            inner.table(Styles.grayPanel, t -> {
                t.margin(5f);
                t.top();
                t.add(new BorderImage(region, 4f)).scaling(Scaling.fit).with(b -> b.drawAlpha = true).size(size - 10f).row();
                t.add((found ? "" : "[red]⚠[] ") + image.name).tooltip(regionPrefix + image.name + "\n[lightgray]" + image.path).ellipsis(true).left().width(size - 10f).growX().row();
                t.add(iwidth + "x" + iheight).tooltip(Strings.formatByteCount(ilength) + "[lightgray]b").color(Color.lightGray).left().growX().row();
                t.table(b -> {
                    b.left();
                    b.defaults().size((size - 10f) / 4f);
                    var istyle = Styles.emptyi;
                    b.button(Icon.pencil, istyle, () -> {
                        ui.showTextInput("@save.rename", "@editor.patches.path", image.path, res -> {
                            Fi fi = new Fi(res);
                            String name = fi.nameWithoutExtension();
                            String path = fi.pathWithoutExtension();
                            var other = getImages().find(p -> p != image && (p.path.equalsIgnoreCase(path) || p.name.equalsIgnoreCase(name)));
                            if(other != null){
                                ui.showErrorMessage(Core.bundle.format("editor.patches.image.exists", other.name + " (" + other.path + ")"));
                            }else{
                                //move and rename associated atlas region
                                if(region instanceof AtlasRegion at && at.name.equals(regionPrefix + image.name)){
                                    var map = Core.atlas.getRegionMap();
                                    map.remove(regionPrefix + image.name);
                                    map.put(regionPrefix + name, at);
                                    at.name = regionPrefix + name;
                                }

                                image.setPath(path);
                                image.name = name;
                                rebuild();
                            }
                        });
                    });
                    b.button(Icon.info, istyle, () -> {
                        BaseDialog d = new BaseDialog("@editor.patches.image.info");
                        float maxSize = Math.min(Core.graphics.getHeight(), Core.graphics.getHeight()) / Scl.scl(1f) * 0.8f - Scl.scl(64f + 60f);
                        d.cont.top();
                        d.cont.add(new BorderImage(region, 4f)).scaling(Scaling.fit).with(bi -> bi.drawAlpha = bi.forceNearest = true).size(maxSize).row();
                        d.cont.row().defaults().left();
                        boolean env = image.path.contains("blocks/environment/");
                        if(!found){
                            d.cont.add(Core.bundle.get("editor.patches.image.packfailed") + (env ? "\n" + Core.bundle.get("editor.patches.image.packfailed.env") : "")).row();
                        }
                        d.cont.add(Core.bundle.format("editor.patches.image.name", image.name)).row();
                        d.cont.add(Core.bundle.format("editor.patches.image.region.name", regionPrefix + image.name)).row();
                        d.cont.add(Core.bundle.format("editor.patches.image.path", image.name)).row();
                        d.cont.add(Core.bundle.format("editor.patches.image.env", env)).row();
                        d.cont.add(Core.bundle.format("editor.patches.image.size", iwidth, iheight)).row();
                        d.cont.add(Core.bundle.format("editor.patches.image.filesize", Strings.formatByteCount(ilength) + "[darkgray]b"));

                        d.addCloseButton();
                        d.show();
                    });
                    b.button(Icon.export, istyle, () -> {
                        platform.showFileChooser(false, "png", out -> {
                            try{
                                image.getCacheFileNoNull().copyTo(out);
                            }catch(Throwable e){
                                ui.showException(e);
                            }
                        });
                    });
                    b.button(Icon.trash, istyle, () -> {
                        ui.showConfirm("@editor.patches.image.delete.confirm", () -> {
                            Core.atlas.getRegionMap().remove(regionPrefix + image.name);
                            getImages().remove(image);
                            rebuild();
                        });
                    }).padTop(2f);
                }).growX();

            }).pad(4f).width(size).uniformY();
            if(++i % cols == 0){
                inner.row();
            }
        }

        if(inner.getChildren().isEmpty()){
            inner.center().add("@none.found").pad(10f);
        }
    }

    private Seq<ImageAsset> getImages(){
        return Vars.state.data.getImages();
    }
}
