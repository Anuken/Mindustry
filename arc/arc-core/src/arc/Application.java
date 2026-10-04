package arc;

import arc.graphics.*;
import arc.struct.*;
import arc.util.*;

import java.net.*;

public interface Application extends Disposable{

    /** Returns a list of all the application listeners used. */
    Seq<ApplicationListener> getListeners();

    /** Adds a new application listener. */
    default void addListener(ApplicationListener listener){
        synchronized(getListeners()){
            getListeners().add(listener);
        }
    }

    /** Removes an application listener. */
    default void removeListener(ApplicationListener listener){
        post(() -> {
            synchronized(getListeners()){
                getListeners().remove(listener);
            }
        });
    }

    /** Call this before update() in each backend. */
    default void defaultUpdate(){
        Core.settings.autosave();
        Time.updateGlobal();
    }

    /** @return what {@link ApplicationType} this application has, e.g. Android or Desktop */
    ApplicationType getType();

    default boolean isDesktop(){
        return getType() == ApplicationType.desktop;
    }

    default boolean isHeadless(){
        return getType() == ApplicationType.headless;
    }

    default boolean isAndroid(){
        return getType() == ApplicationType.android;
    }

    default boolean isIOS(){
        return getType() == ApplicationType.iOS;
    }

    default boolean isMobile(){
        return isAndroid() || isIOS();
    }

    default boolean isWeb(){
        return getType() == ApplicationType.web;
    }

    /** @return the Android API level on Android, the major OS version on iOS (5, 6, 7, ..), or 0 on the desktop. */
    default int getVersion(){
        return 0;
    }

    /** @return the Java heap memory use in bytes. */
    default long getJavaHeap(){
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    /** @return the Native heap memory use in bytes. Only valid on Android. */
    default long getNativeHeap(){
        return 0;
    }

    /** @return the main graphics thread, upon which all ApplicationListener methods are called. May return null if not initialized. */
    default @Nullable Thread getMainThread(){
        return null;
    }

    /**
     * @return whether the currently executing thread is the main thread.
     * If the main thread is not initialized, returns true by default. This is used for error checking purposes.
     * */
    default boolean isOnMainThread(){
        Thread thread = getMainThread();
        return thread == null || Thread.currentThread() == thread;
    }

    @Nullable String getClipboardText();

    void setClipboardText(String text);

    default void setClipboardImage(Pixmap pixmap){
        //no-op
    }

    /** Open a folder in the system's file browser.
     * @return whether this operation was successful. */
    default boolean openFolder(String file){
        return false;
    }

    /**
     * Launches the default browser to display a URI. If the default browser is not able to handle the specified URI, the
     * application registered for handling URIs of the specified type is invoked. The application is determined from the protocol
     * and path of the URI. A best effort is made to open the given URI; however, since external applications are involved, no guarantee
     * can be made as to whether the URI was actually opened. If it is known that the URI was not opened, false will be returned;
     * otherwise, true will be returned.
     * @param URI the URI to be opened.
     * @return false if it is known the uri was not opened, true otherwise.
     */
    default boolean openURI(String URI){
        return false;
    }

    default void getDnsServers(Seq<InetSocketAddress> out){}

    /** Posts a runnable on the main loop thread.*/
    void post(Runnable runnable);

    /**
     * Schedule an exit from the application. On android, this will cause a call to pause() and dispose() some time in the future,
     * it will not immediately finish your application.
     * On iOS this should be avoided in production as it breaks Apples guidelines.
     */
    void exit();

    /** Disposes of core resources. */
    @Override
    default void dispose(){
        //flush any changes to settings upon dispose
        if(Core.settings != null){
            Core.settings.autosave();
        }

        if(Core.audio != null){
            Core.audio.dispose();
        }
    }

    /** Enumeration of possible {@link Application} types */
    enum ApplicationType{
        android, desktop, headless, web, iOS
    }
}
