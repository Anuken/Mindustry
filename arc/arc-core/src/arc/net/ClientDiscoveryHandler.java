

package arc.net;

import java.net.DatagramPacket;

public interface ClientDiscoveryHandler{
    /**
     * Implementations of this method should return a new {@link DatagramPacket}
     * that the {@link Client} will use to fill with the incoming packet data
     * sent by the {@link ServerDiscoveryHandler}.
     * @return a new {@link DatagramPacket}
     */
    DatagramPacket newDatagramPacket();

    /**
     * Called when the {@link Client} discovers a host.
     * @param datagramPacket the same {@link DatagramPacket} from
     * {@link #newDatagramPacket()}, after being filled with
     * the incoming packet data.
     */
    void discoveredHost(DatagramPacket datagramPacket);

    /**
     * Called right before the {@link Client#discoverHost(int, int)} or
     * {@link Client#discoverHosts(int, int)} method exits. This allows the
     * implementation to clean up any resources used.
     */
    void finish();

}
