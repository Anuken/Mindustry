package arc;

import arc.audio.Audio;
import arc.files.Fi;
import arc.util.ArcRuntimeException;

/**
 * Provides standard access to the filesystem, classpath, Android SD card, and Android assets directory.
 * @author mzechner
 * @author Nathan Sweet
 */
public interface Files{
    /**
     * Returns a handle representing a file or directory.
     * @param type Determines how the path is resolved.
     * @throws ArcRuntimeException if the type is classpath or internal and the file does not exist.
     * @see FileType
     */
    Fi get(String path, FileType type);

    /** Convenience method that returns a {@link FileType#classpath} file handle. */
    default Fi classpath(String path){
        return get(path, FileType.classpath);
    }

    /** Convenience method that returns a {@link FileType#internal} file handle. */
    default Fi internal(String path){
        return get(path, FileType.internal);
    }

    /** Convenience method that returns a {@link FileType#external} file handle. */
    default Fi external(String path){
        return get(path, FileType.external);
    }

    /** Convenience method that returns a {@link FileType#absolute} file handle. */
    default Fi absolute(String path){
        return get(path, FileType.absolute);
    }

    /** Convenience method that returns a {@link FileType#local} file handle. */
    default Fi local(String path){
        return get(path, FileType.local);
    }

    /** Convenience method that returns a cache file handle. */
    default Fi cache(String path){
        return get(getCachePath(), FileType.absolute).child(path);
    }

    /** @return absolute path to cache directory. */
    default String getCachePath(){
        return local("cache").absolutePath();
    }

    /** @return on IOS, the internal path to the assets directory. Not used on other platforms. */
    default String getInternalStoragePath(){
        return "";
    }

    /**
     * @return the external storage path directory. This is the SD card on Android and the home directory of the current user on
     * the desktop.
     */
    String getExternalStoragePath();

    /**
     * @return true if the external storage is ready for file IO. Eg, on Android, the SD card is not available when mounted for use
     * with a PC.
     */
    boolean isExternalStorageAvailable();

    /**
     * @return the local storage path directory. This is the private files directory on Android and the directory of the jar on the
     * desktop.
     */
    String getLocalStoragePath();

    /** @return true if the local storage is ready for file IO. */
    boolean isLocalStorageAvailable();

    /**
     * Indicates how to resolve a path to a file.
     * @author mzechner
     * @author Nathan Sweet
     */
    enum FileType{
        /**
         * Path relative to the root of the classpath. Classpath files are always readonly. Note that classpath files are not
         * compatible with some functionality on Android, such as {@link arc.audio.Audio#newSound(Fi)} and
         * {@link Audio#newMusic(Fi)}.
         */
        classpath,

        /**
         * Path relative to the asset directory on Android and to the application's root directory on the desktop. On the desktop,
         * if the file is not found, then the classpath is checked. This enables files to be found when using JWS or applets.
         * Internal files are always readonly.
         */
        internal,

        /** Path relative to the root of the SD card on Android and to the home directory of the current user on the desktop. */
        external,

        /**
         * Path that is a fully qualified, absolute filesystem path. To ensure portability across platforms use absolute files only
         * when absolutely (heh) necessary.
         */
        absolute,

        /** Path relative to the private files directory on Android and to the application's root directory on the desktop. */
        local
    }
}
