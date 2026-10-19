package arc.backend.robovm;

import arc.*;
import org.robovm.apple.uikit.*;

public class IOSSceneDelegate extends UIWindowSceneDelegateAdapter{
    private UIWindow uiWindow;

    @Override
    public void setWindow(UIWindow window){
        this.uiWindow = window;
    }

    @Override
    public UIWindow getWindow(){
        return uiWindow;
    }

    @Override
    public void willConnect(UIScene scene, UISceneSession session, UISceneConnectionOptions connectionOptions){
        if(scene instanceof UIWindowScene){
            IOSApplication app = (IOSApplication)Core.app;
            app.handleSceneConnection((UIWindowScene)scene);
            IOSApplication.Delegate userLauncher = (IOSApplication.Delegate)UIApplication.getSharedApplication().getDelegate();
            if(userLauncher != null) userLauncher.willConnect(scene, session, connectionOptions);
        }
    }

    @Override
    public void sceneWillResignActive(UIScene scene){
        IOSApplication app = (IOSApplication)Core.app;
        IOSApplication.Delegate userLauncher = (IOSApplication.Delegate)UIApplication.getSharedApplication().getDelegate();
        if(userLauncher != null) userLauncher.sceneWillResignActive(scene);
        app.willResignActive(scene);
    }

    @Override
    public void sceneWillEnterForeground(UIScene scene){
        IOSApplication app = (IOSApplication)Core.app;
        app.willEnterForeground(scene);
        IOSApplication.Delegate userLauncher = (IOSApplication.Delegate)UIApplication.getSharedApplication().getDelegate();
        if(userLauncher != null) userLauncher.sceneWillEnterForeground(scene);
    }

    @Override
    public void sceneDidBecomeActive(UIScene scene){
        IOSApplication app = (IOSApplication)Core.app;
        app.didBecomeActive(scene);
        IOSApplication.Delegate userLauncher = (IOSApplication.Delegate)UIApplication.getSharedApplication().getDelegate();
        if(userLauncher != null) userLauncher.sceneDidBecomeActive(scene);
    }

    @Override
    public void sceneDidDisconnect(UIScene scene){
        IOSApplication.Delegate userLauncher = (IOSApplication.Delegate)UIApplication.getSharedApplication().getDelegate();
        if(userLauncher != null){
            userLauncher.sceneDidDisconnect(scene);
            // we call willTerminate manually since we kill the process below
            userLauncher.willTerminate(UIApplication.getSharedApplication());
        }
        // OS can disconnect and reconnect scenes to free resources; Arc doesn't support graphics recreation, so just exit
        System.exit(0);
    }

    @Override
    public void sceneDidEnterBackground(UIScene scene){
        IOSApplication.Delegate userLauncher = (IOSApplication.Delegate)UIApplication.getSharedApplication().getDelegate();
        if(userLauncher != null) userLauncher.sceneDidEnterBackground(scene);
    }
}