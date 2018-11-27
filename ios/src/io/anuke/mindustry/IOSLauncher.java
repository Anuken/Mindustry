package io.anuke.mindustry;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import io.anuke.kryonet.KryoClient;
import io.anuke.kryonet.KryoServer;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.game.Saves.SaveSlot;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Strings;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.foundation.NSURL;
import org.robovm.apple.uikit.*;

import java.io.IOException;
import java.util.Collections;

import static io.anuke.mindustry.Vars.*;
import static org.robovm.apple.foundation.NSPathUtilities.getDocumentsDirectory;

public class IOSLauncher extends IOSApplication.Delegate {
    private boolean forced;

    @Override
    protected IOSApplication createApplication() {
        Net.setClientProvider(new KryoClient());
        Net.setServerProvider(new KryoServer());

        if(UIDevice.getCurrentDevice().getUserInterfaceIdiom() == UIUserInterfaceIdiom.Pad){
            Unit.dp.addition = 0.5f;
        }

        Platform.instance = new Platform() {

            @Override
            public void shareFile(FileHandle file){
                FileHandle to = Gdx.files.absolute(getDocumentsDirectory()).child(file.name());
                file.copyTo(to);

                NSURL url = new NSURL(to.file());
                UIActivityViewController p = new UIActivityViewController(Collections.singletonList(url), null);
                p.getPopoverPresentationController().setSourceView(UIApplication.getSharedApplication().getKeyWindow().getRootViewController().getView());

                UIApplication.getSharedApplication().getKeyWindow().getRootViewController()
                .presentViewController(p, true, () -> io.anuke.ucore.util.Log.info("Success! Presented {0}", to));
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
    public boolean openURL(UIApplication app, NSURL url, UIApplicationOpenURLOptions options) {
        System.out.println("Opened URL: " + url.getPath());
        openURL(url);
        return false;
    }

    @Override
    public boolean didFinishLaunching(UIApplication application, UIApplicationLaunchOptions options) {
        boolean b = super.didFinishLaunching(application, options);

        if(options != null && options.has(UIApplicationLaunchOptions.Keys.URL())){
            System.out.println("Opened URL at launch: " + ((NSURL)options.get(UIApplicationLaunchOptions.Keys.URL())).getPath());
            openURL(((NSURL)options.get(UIApplicationLaunchOptions.Keys.URL())));
        }

        return b;
    }

    void openURL(NSURL url){

        Gdx.app.postRunnable(() -> {
            FileHandle file = Gdx.files.absolute(getDocumentsDirectory()).child(url.getLastPathComponent());
            Gdx.files.absolute(url.getPath()).copyTo(file);

            if(file.extension().equalsIgnoreCase(saveExtension)){ //open save

                if(SaveIO.isSaveValid(file)){
                    try{
                        SaveSlot slot = control.saves.importSave(file);
                        ui.load.runLoadSave(slot);
                    }catch (IOException e){
                        ui.showError(Bundles.format("text.save.import.fail", Strings.parseException(e, false)));
                    }
                }else{
                    ui.showError("$text.save.import.invalid");
                }

            }else if(file.extension().equalsIgnoreCase(mapExtension)){ //open map
                Gdx.app.postRunnable(() -> {
                    if (!ui.editor.isShown()) {
                        ui.editor.show();
                    }

                    ui.editor.beginEditMap(file.read());
                });
            }
        });
    }

    public static void main(String[] argv) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(argv, null, IOSLauncher.class);
        pool.close();
    }
}