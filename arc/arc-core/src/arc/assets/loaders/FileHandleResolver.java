package arc.assets.loaders;

import arc.assets.AssetManager;
import arc.files.Fi;

/**
 * Interface for classes the can map a file name to a {@link Fi}. Used to allow the {@link AssetManager} to load resources
 * from anywhere or implement caching strategies.
 * @author mzechner
 */
public interface FileHandleResolver{
    Fi resolve(String fileName);
}
