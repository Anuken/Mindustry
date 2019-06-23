package io.anuke.mindustry;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import io.anuke.arc.Core;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.scene.ui.layout.Unit;
import io.anuke.arc.util.Strings;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.game.Saves.SaveSlot;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.ArcNetClient;
import io.anuke.mindustry.net.ArcNetServer;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.foundation.NSURL;
import org.robovm.apple.uikit.*;

import java.io.IOException;
import java.util.Collections;

import static io.anuke.mindustry.Vars.*;
import static org.robovm.apple.foundation.NSPathUtilities.getDocumentsDirectory;

public class IOSLauncher extends IOSApplication.Delegate{
    private boolean forced;

    @Override
    protected IOSApplication createApplication(){
        Net.setClientProvider(new ArcNetClient());
        Net.setServerProvider(new ArcNetServer());

        if(UIDevice.getCurrentDevice().getUserInterfaceIdiom() == UIUserInterfaceIdiom.Pad){
            Unit.dp.addition = 0.5f;
        }else{
            Unit.dp.addition = -0.5f;
        }

        Platform.instance = new Platform(){

            @Override
            public void shareFile(FileHandle file){
                FileHandle to = Core.files.absolute(getDocumentsDirectory()).child(file.name());
                file.copyTo(to);

                NSURL url = new NSURL(to.file());
                UIActivityViewController p = new UIActivityViewController(Collections.singletonList(url), null);
                p.getPopoverPresentationController().setSourceView(UIApplication.getSharedApplication().getKeyWindow().getRootViewController().getView());

                UIApplication.getSharedApplication().getKeyWindow().getRootViewController()
                .presentViewController(p, true, () -> io.anuke.arc.util.Log.info("Success! Presented {0}", to));
            }

            @Override
            public void beginForceLandscape(){
                forced = true;
            }

            @Override
            public void endForceLandscape(){
                forced = false;
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

        return b;
    }

    void openURL(NSURL url){

        Core.app.post(() -> {
            FileHandle file = Core.files.absolute(getDocumentsDirectory()).child(url.getLastPathComponent());
            Core.files.absolute(url.getPath()).copyTo(file);

            if(file.extension().equalsIgnoreCase(saveExtension)){ //open save

                if(SaveIO.isSaveValid(file)){
                    try{
                        SaveSlot slot = control.saves.importSave(file);
                        ui.load.runLoadSave(slot);
                    }catch(IOException e){
                        ui.showError(Core.bundle.format("save.import.fail", Strings.parseException(e, true)));
                    }
                }else{
                    ui.showError("save.import.invalid");
                }

            }else if(file.extension().equalsIgnoreCase(mapExtension)){ //open map
                Core.app.post(() -> {
                    if(!ui.editor.isShown()){
                        ui.editor.show();
                    }

                    ui.editor.beginEditMap(file);
                });
            }
        });
    }

    public static void main(String[] argv){
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(argv, null, IOSLauncher.class);
        pool.close();
    }
}