package io.anuke.mindustry;

import com.badlogic.gdx.backends.iosrobovm.*;
import io.anuke.arc.*;
import io.anuke.arc.files.*;
import io.anuke.arc.func.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.io.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.Saves.*;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.mod.*;
import io.anuke.mindustry.ui.*;
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

        return new IOSApplication(new ClientLauncher(){

            @Override
            public void showFileChooser(boolean open, String extension, Cons<FileHandle> cons){
                UIDocumentBrowserViewController cont = new UIDocumentBrowserViewController((NSArray<NSString>)null);


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
                        NSFileCoordinator coord = new NSFileCoordinator(null);

                        try{
                            coord.coordinateReadingItem(documentURLs.get(0), NSFileCoordinatorReadingOptions.None, url -> {
                                if(url.startAccessingSecurityScopedResource()){
                                    try{
                                        controller.importDocument(url, new NSURL(getDocumentsDirectory() + "/document"), UIDocumentBrowserImportMode.Copy, (result, error) -> {
                                            if(error != null){
                                                ui.showErrorMessage("Import error.\n" + error.getLocalizedFailureReason() + "\n" + error.getLocalizedDescription());
                                            }else{
                                                try{
                                                    cons.get(Core.files.absolute(url.getPath()));
                                                }catch(Throwable t){
                                                    ui.showException(t);
                                                }
                                            }
                                        });
                                    }catch(Throwable t){
                                        ui.showException(t);
                                    }

                                    /*

                                    try{
                                        int[] tread = {0};

                                        cons.get(new FileHandle(url.getPath()){
                                            @Override
                                            public InputStream read(){
                                                NSInputStream stream = new NSInputStream(url);

                                                return new InputStream(){
                                                    byte[] tmp = {0};

                                                    @Override
                                                    public int read() throws IOException{
                                                        read(tmp);
                                                        return tmp[0];
                                                    }

                                                    @Override
                                                    public int read(byte[] bytes, int offset, int length){
                                                        int read = (int)stream.read(bytes, offset, length);
                                                        tread[0] += read;
                                                        return read;
                                                    }
                                                };
                                            }
                                        });
                                        Core.app.post(() -> Core.app.post(() -> Core.app.post(() -> ui.showInfo("Read " + tread[0]))));
                                        //cons.get(new FileHandle(url.getAbsoluteString()));
                                    }catch(Throwable t){
                                        ui.showException(t);

                                        try{
                                            cons.get(new FileHandle(url.getPath()));
                                        }catch(Throwable t2){
                                            ui.showException(t2);

                                            try{
                                                cons.get(new FileHandle(url.getAbsoluteURL().getPath()));
                                            }catch(Throwable t3){
                                                ui.showException(t3);
                                            }
                                        }
                                    }*/
                                }else{
                                    ui.showErrorMessage("Failed to access file.");
                                }

                                /*
                                try{
                                    int[] tread = {0};

                                    cons.get(new FileHandle(url.getPath()){
                                        @Override
                                        public InputStream read(){
                                            NSInputStream stream = new NSInputStream(url);

                                            return new InputStream(){
                                                byte[] tmp = {0};

                                                @Override
                                                public int read() throws IOException{
                                                    read(tmp);
                                                    return tmp[0];
                                                }

                                                @Override
                                                public int read(byte[] bytes, int offset, int length){
                                                    int read = (int)stream.read(bytes, offset, length);
                                                    tread[0] += read;
                                                    return read;
                                                }
                                            };
                                        }
                                    });
                                    Core.app.post(() -> Core.app.post(() -> Core.app.post(() -> ui.showInfo("Read " + tread[0]))));
                                }catch(Throwable t){
                                    ui.showException(t);
                                }*/
                            });
                        }catch(Throwable t){
                            ui.showException(t);
                        }
                        /*

                        try{
                            controller.importDocument(documentURLs.get(0), new NSURL(getDocumentsDirectory() + "/document"), UIDocumentBrowserImportMode.Copy, (url, error) -> {
                                if(error != null){
                                   ui.showErrorMessage("Import error.\n" + error.getLocalizedFailureReason() + "\n" + error.getLocalizedDescription());
                                }else{
                                    try{
                                        cons.get(Core.files.absolute(url.getPath()));
                                    }catch(Throwable t){
                                        ui.showException(t);
                                    }
                                }
                            });
                        }catch(Throwable t){
                            ui.showException(t);
                        }*/
                    }

                    @Override
                    public void didRequestDocumentCreationWithHandler(UIDocumentBrowserViewController controller, VoidBlock2<NSURL, UIDocumentBrowserImportMode> importHandler){

                    }

                    @Override
                    public void didImportDocument(UIDocumentBrowserViewController controller, NSURL sourceURL, NSURL destinationURL){
                        //cons.get(Core.files.absolute(destinationURL.getAbsoluteString()));
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

                UIApplication.getSharedApplication().getKeyWindow().getRootViewController().presentViewController(cont, true, () -> {});
            }

            @Override
            public void shareFile(FileHandle file){
                try{
                    Log.info("Attempting to share file " + file);
                    FileHandle to = Core.files.absolute(getDocumentsDirectory()).child(file.name());
                    file.copyTo(to);

                    NSURL url = new NSURL(to.file());
                    UIActivityViewController p = new UIActivityViewController(Collections.singletonList(url), null);
                    UIApplication.getSharedApplication().getKeyWindow().getRootViewController().presentViewController(p, true, () -> Log.info("Success! Presented {0}", to));
                }catch(Throwable t){
                    ui.showException(t);
                }
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
        openURL(url);
        return false;
    }

    @Override
    public boolean didFinishLaunching(UIApplication application, UIApplicationLaunchOptions options){
        boolean b = super.didFinishLaunching(application, options);

        if(options != null && options.has(UIApplicationLaunchOptions.Keys.URL())){
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
