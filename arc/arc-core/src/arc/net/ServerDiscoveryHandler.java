

package arc.net;

import arc.net.FrameworkMessage.DiscoverHost;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public interface ServerDiscoveryHandler{
    /**
     * Called when the {@link Server} receives a {@link DiscoverHost} packet.
     * @throws IOException from sending a response.
     */
    void onDiscoverReceived(InetAddress address, ReponseHandler handler) throws IOException;

    interface ReponseHandler{
        void respond(ByteBuffer buffer) throws IOException;
    }
}
