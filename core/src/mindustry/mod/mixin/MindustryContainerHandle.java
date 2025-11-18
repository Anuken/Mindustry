package mindustry.mod.mixin;

import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;

public class MindustryContainerHandle extends ContainerHandleURI{

    public MindustryContainerHandle() throws URISyntaxException{
        super(new URI("file:///mindustry"));
    }

    @Override
    public Collection<IContainerHandle> getNestedContainers(){
        return Collections.emptyList();
    }
}
