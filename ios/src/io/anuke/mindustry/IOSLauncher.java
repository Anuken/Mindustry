package io.anuke.mindustry;

import com.badlogic.gdx.backends.iosrobovm.*;
import io.anuke.arc.*;
import io.anuke.arc.files.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.io.*;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.game.Saves.*;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.*;
import org.robovm.apple.foundation.*;
import org.robovm.apple.uikit.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import static io.anuke.mindustry.Vars.*;
import static org.robovm.apple.foundation.NSPathUtilities.getDocumentsDirectory;

public class IOSLauncher extends IOSApplication.Delegate{
    private boolean forced;

    @Override
    protected IOSApplication createApplication(){
        Net.setClientProvider(new ArcNetClient());
        Net.setServerProvider(new ArcNetServer());

        if(UIDevice.getCurrentDevice().getUserInterfaceIdiom() == UIUserInterfaceIdiom.Pad){
            UnitScl.dp.addition = 0.5f;
        }else{
            UnitScl.dp.addition = -0.5f;
        }

        Platform.instance = new Platform(){

            @Override
            public void shareFile(FileHandle file){
                Log.info("Attempting to share file " + file);
                FileHandle to = Core.files.absolute(getDocumentsDirectory()).child(file.name()/* + ".png"*/);
                file.copyTo(to);

                NSURL url = new NSURL(to.file());
                UIActivityViewController p = new UIActivityViewController(Collections.singletonList(url), null);
                p.getPopoverPresentationController().setSourceView(UIApplication.getSharedApplication().getKeyWindow().getRootViewController().getView());

                UIApplication.getSharedApplication().getKeyWindow().getRootViewController()
                .presentViewController(p, true, () -> io.anuke.arc.util.Log.info("Success! Presented {0}", to));
            }

            @Override
            public void beginForceLandscape(){
                Log.info("begin force landscape");
                forced = true;
                UINavigationController.attemptRotationToDeviceOrientation();
            }

            @Override
            public void endForceLandscape(){
                forced = false;
                UINavigationController.attemptRotationToDeviceOrientation();
            }
        };

        IOSApplicationConfiguration config = new IOSApplicationConfiguration();
        return new IOSApplication(new Mindustry(), config);
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

        Core.app.post(() -> Core.app.post(() -> {
            Core.scene.table("dialogDim", t -> {
                t.visible(() -> {
                    if(!forced) return false;
                    t.toFront();
                    UIInterfaceOrientation o = UIApplication.getSharedApplication().getStatusBarOrientation();
                    return forced && (o == UIInterfaceOrientation.Portrait || o == UIInterfaceOrientation.PortraitUpsideDown);
                });
                t.add("Please rotate the device to landscape orientation to use the editor.").wrap().grow();
            });
        }));

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
                        ui.showError(Core.bundle.format("save.import.fail", Strings.parseException(e, true)));
                    }
                }else{
                    ui.showError("save.import.invalid");
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