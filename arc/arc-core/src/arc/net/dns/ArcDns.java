package arc.net.dns;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.Streams.*;

import java.io.*;
import java.net.*;

public final class ArcDns{

    /** Default dns server port. */
    public static final int dnsResolverPort = 53;

    private static final Seq<InetSocketAddress> nameservers = new Seq<>(3);
    private static final Seq<NameserverProvider> nameserverProviders = Seq.with(
    new JndiContextNameserverProvider(),    // Simple JRE installation
    new ResolvConfNameserverProvider(),     // Unix/Linux
    new WellKnownNameserverProvider()       // Others
    );

    static{
        refreshNameservers();
    }

    public static Seq<NameserverProvider> getNameserverProviders(){
        return nameserverProviders;
    }

    /** Set a new ordered list of resolver config providers. */
    public static void setNameserverProviders(Seq<NameserverProvider> providers){
        nameserverProviders.clear();
        nameserverProviders.addAll(providers);
        refreshNameservers();
    }

    /** Returns all located servers */
    public static Seq<InetSocketAddress> getNameservers(){
        return nameservers;
    }

    public static void refreshNameservers(){
        nameservers.clear();

        if(Core.app != null){
            Core.app.getDnsServers(nameservers);
        }

        for(NameserverProvider provider : nameserverProviders){
            if(provider.isEnabled()){
                try{
                    nameservers.addAll(provider.getNameservers());
                    // Stop when a name server is found
                    if(!nameservers.isEmpty()) return;
                }catch(Exception e){
                    Log.warn("[DNS] Failed to initialize provider: @", e);
                }
            }
        }

        for(InetSocketAddress server : nameservers){
            Log.debug("[DNS] Added @ to nameservers", server);
        }

        // Add localhost as nameserver if no suitable nameserver provider found
        nameservers.add(new InetSocketAddress(InetAddress.getLoopbackAddress(), dnsResolverPort));
    }

    /**
     * Lookup the SRV record of a domain in the format {@code _service._protocol.name}
     * with the list of nameservers from {@link #getNameservers()}.
     */
    public static Seq<SRVRecord> getSrvRecords(String domain){
        for(InetSocketAddress nameserver : nameservers){
            try{
                return getSrvRecords(domain, nameserver);
            }catch(IOException ignored){
            }
        }
        return new Seq<>(1);
    }

    /**
     * Lookup the SRV record of a domain in the format {@code _service._protocol.name}.
     * The results are sorted by priority, then by weight.
     */
    public static Seq<SRVRecord> getSrvRecords(String domain, InetSocketAddress nameserver) throws IOException{
        Seq<SRVRecord> records = new Seq<>();

        try(DatagramSocket socket = new DatagramSocket()){
            socket.setSoTimeout(2000);

            short id = (short)new Rand().nextInt(Short.MAX_VALUE);
            byte[] response = new byte[512];

            try(ByteArrayOutputStream stream = new OptimizedByteArrayOutputStream(128);
                DataOutputStream out = new DataOutputStream(stream)
            ){
                out.writeShort(id);         // Id
                out.writeShort(0x0100);     // Flags (recursion enabled)
                out.writeShort(1);          // Questions
                out.writeShort(0);          // Answers
                out.writeShort(0);          // Authority
                out.writeShort(0);          // Additional

                // Domain
                for(String part : domain.split("\\.")){
                    out.writeByte(part.length());
                    out.write(part.getBytes(Strings.utf8));
                }
                out.writeByte(0);

                out.writeShort(33);         // Type (SRV)
                out.writeShort(1);          // Class (Internet)

                socket.send(new DatagramPacket(stream.toByteArray(), stream.size(), nameserver));
            }

            DatagramPacket packet = new DatagramPacket(response, response.length);
            socket.receive(packet);

            try(DataInputStream in = new DataInputStream(new ByteArrayInputStream(response))){
                short responseId = in.readShort();
                if(responseId != id) throw new IOException("Invalid response from dns server " + nameserver);

                in.readShort();
                in.readShort();
                int answers = in.readUnsignedShort();
                in.readShort();
                in.readShort();

                byte len;
                while((len = in.readByte()) != 0){
                    in.skipBytes(len);
                }

                in.readShort();
                in.readShort();

                for(int i = 0; i < answers; i++){
                    in.readShort();                         // OFFSET
                    in.readShort();                         // Type
                    in.readShort();                         // Class
                    long ttl = in.readInt() & 0xFFFFFFFFL;  // TTL
                    in.readShort();                         // Data length

                    int priority = in.readUnsignedShort();
                    int weight = in.readUnsignedShort();
                    int port = in.readUnsignedShort();

                    StringBuilder builder = new StringBuilder();
                    while((len = in.readByte()) != 0){
                        for(int j = 0; j < len; j++) builder.append((char)in.readByte());
                        builder.append('.');
                    }
                    builder.delete(builder.length() - 1, builder.length());

                    records.add(new SRVRecord(ttl, priority, weight, port, builder.toString()));
                }
            }
        }

        return records.sort();
    }
}
