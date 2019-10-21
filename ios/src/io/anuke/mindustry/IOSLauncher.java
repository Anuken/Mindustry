package io.anuke.mindustry;

import com.badlogic.gdx.backends.iosrobovm.*;
import io.anuke.arc.*;
import io.anuke.arc.files.*;
import io.anuke.arc.function.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.io.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.Saves.*;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.mod.*;
import io.anuke.mindustry.ui.*;
import org.robovm.apple.dispatch.*;
import org.robovm.apple.foundation.*;
import org.robovm.apple.uikit.*;
import org.robovm.objc.block.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import static io.anuke.mindustry.Vars.*;
import static org.robovm.apple.foundation.NSPathUtilities.getDocumentsDirectory;

public class IOSLauncher extends IOSApplication.Delegate{
    private boolean forced;

    @Override
    protected IOSApplication createApplication(){

        if(UIDevice.getCurrentDevice().getUserInterfaceIdiom() == UIUserInterfaceIdiom.Pad){
            Scl.setAddition(0.5f);
        }else{
            Scl.setAddition(-0.5f);
        }

        //IOSApplicationConfiguration config = new IOSApplicationConfiguration();
        return new IOSApplication(new ClientLauncher(){

            @Override
            public void showFileChooser(boolean open, String extension, Consumer<FileHandle> cons){
                UIDocumentBrowserViewController cont = new UIDocumentBrowserViewController((NSArray)null);

                NSArray<UIBarButtonItem> arr = new NSArray<>(new UIBarButtonItem(Core.bundle.get("cancel"), UIBarButtonItemStyle.Plain,
                    uiBarButtonItem -> cont.dismissViewController(true, () -> {})));

                cont.setAllowsDocumentCreation(!open);
                cont.setAdditionalLeadingNavigationBarButtonItems(arr);

                class ChooserDelegate extends NSObject implements UIDocumentBrowserViewControllerDelegate{
                    @Override
                    public void didPickDocumentURLs(UIDocumentBrowserViewController controller, NSArray<NSURL> documentURLs){

                    }

                    @Override
                    public void didPickDocumentsAtURLs(UIDocumentBrowserViewController controller, NSArray<NSURL> documentURLs){
                        if(documentURLs.size() < 1) return;

                        cont.dismissViewController(true, () -> {});
                        controller.importDocument(documentURLs.get(0), new NSURL(getDocumentsDirectory() + "/document"), UIDocumentBrowserImportMode.Copy, (url, error) -> cons.accept(Core.files.absolute(url.getPath())));
                    }

                    @Override
                    public void didRequestDocumentCreationWithHandler(UIDocumentBrowserViewController controller, VoidBlock2<NSURL, UIDocumentBrowserImportMode> importHandler){

                    }

                    @Override
                    public void didImportDocument(UIDocumentBrowserViewController controller, NSURL sourceURL, NSURL destinationURL){
                        cons.accept(Core.files.absolute(destinationURL.getAbsoluteString()));
                    }

                    @Override
                    public void failedToImportDocument(UIDocumentBrowserViewController controller, NSURL documentURL, NSError error){

                    }

                    @Override
                    public NSArray<UIActivity> applicationActivities(UIDocumentBrowserViewController controller, NSArray<NSURL> documentURLs){
                        return null;
                    }

                    @Override
                    public void willPresentActivityViewController(UIDocumentBrowserViewController controller, UIActivityViewController activityViewController){

                    }
                }

                cont.setDelegate(new ChooserDelegate());

               // DispatchQueue.getMainQueue().sync(() -> {
                UIApplication.getSharedApplication().getKeyWindow().getRootViewController().presentViewController(cont, true, () -> {});
               // });
            }

            @Override
            public void shareFile(FileHandle file){
                Log.info("Attempting to share file " + file);
                FileHandle to = Core.files.absolute(getDocumentsDirectory()).child(file.name());
                file.copyTo(to);

                NSURL url = new NSURL(to.file());
                UIActivityViewController p = new UIActivityViewController(Collections.singletonList(url), null);
                //p.getPopoverPresentationController().setSourceView(UIApplication.getSharedApplication().getKeyWindow().getRootViewController().getView());

                //DispatchQueue.getMainQueue().sync(() -> {
                UIApplication.getSharedApplication().getKeyWindow().getRootViewController()
                .presentViewController(p, true, () -> Log.info("Success! Presented {0}", to));
                //});
            }

            @Override
            public void beginForceLandscape(){
                forced = true;
                UINavigationController.attemptRotationToDeviceOrientation();
            }

            @Override
            public void endForceLandscape(){
                forced = false;
                UINavigationController.attemptRotationToDeviceOrientation();
            }
        }, new IOSApplicationConfiguration(){{
           errorHandler = ModCrashHandler::handle;
        }});
    }

    @Override
    public UIInterfaceOrientationMask getSupportedInterfaceOrientations(UIApplication application, UIWindow window){
        return forced ? UIInterfaceOrientationMask.Landscape : UIInterfaceOrientationMask.All;
    }


    @Override
    public boolean openURL(UIApplication app, NSURL url, UIApplicationOpenURLOptions options){
        System.out.println("Opened URL: " + url.getPath());
        openURL(url);
        return false;
    }

    @Override
    public boolean didFinishLaunching(UIApplication application, UIApplicationLaunchOptions options){
        boolean b = super.didFinishLaunching(application, options);

        if(options != null && options.has(UIApplicationLaunchOptions.Keys.URL())){
            System.out.println("Opened URL at launch: " + ((NSURL)options.get(UIApplicationLaunchOptions.Keys.URL())).getPath());
            openURL(((NSURL)options.get(UIApplicationLaunchOptions.Keys.URL())));
        }

        Events.on(ClientLoadEvent.class, e -> {
            Core.app.post(() -> Core.app.post(() -> {
                Core.scene.table(Styles.black9, t -> {
                    t.visible(() -> {
                        if(!forced) return false;
                        t.toFront();
                        UIInterfaceOrientation o = UIApplication.getSharedApplication().getStatusBarOrientation();
                        return forced && (o == UIInterfaceOrientation.Portrait || o == UIInterfaceOrientation.PortraitUpsideDown);
                    });
                    t.add("Please rotate the device to landscape orientation to use the editor.").wrap().grow();
                });
            }));
        });

        return b;
    }

    void openURL(NSURL url){

        Core.app.post(() -> Core.app.post(() -> {
            FileHandle file = Core.files.absolute(getDocumentsDirectory()).child(url.getLastPathComponent());
            Core.files.absolute(url.getPath()).copyTo(file);

            if(file.extension().equalsIgnoreCase(saveExtension)){ //open save

                if(SaveIO.isSaveValid(file)){
                    try{
                        SaveMeta meta = SaveIO.getMeta(new DataInputStream(new InflaterInputStream(file.read(Streams.DEFAULT_BUFFER_SIZE))));
                        if(meta.tags.containsKey("name")){
                            //is map
                            if(!ui.editor.isShown()){
                                ui.editor.show();
                            }

                            ui.editor.beginEditMap(file);
                        }else{
                            SaveSlot slot = control.saves.importSave(file);
                            ui.load.runLoadSave(slot);
                        }
                    }catch(IOException e){
                        ui.showException("$save.import.fail", e);
                    }
                }else{
                    ui.showErrorMessage("$save.import.invalid");
                }

            }
        }));
    }

    public static void main(String[] argv){
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(argv, null, IOSLauncher.class);
        pool.close();
    }
}
