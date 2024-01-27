package mindustry.net;

import arc.func.*;
import arc.math.*;
import arc.net.dns.*;
import arc.struct.*;
import arc.util.*;

import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.*;

public class Dns{
    private static final int aRecord = 1, aaaaRecord = 28, srvRecord = 33;
    private static IntMap<ObjectMap<String, Seq<?>>> cache = new IntMap<>(); //TODO remove this cache?
    private static ConcurrentHashMap<String, InetAddress> domainToIp = new ConcurrentHashMap<>();

    static <T> void resolve(int type, String domain, Func<ByteBuffer, T> reader, Cons<Seq<T>> result, Cons<Exception> error){
        ObjectMap<String, Seq<?>> map;

        synchronized(cache){
            map = cache.get(type, ObjectMap::new);

            //TODO timeout
            if(map.containsKey(domain)){
                result.get((Seq<T>)map.get(domain));
                return;
            }
        }

        send(ArcDns.getNameservers(), 0, type, domain, reader, records -> {
            synchronized(cache){
                //cache the records
                map.put(domain, records);
            }

            result.get(records);
        }, error);
    }

    /*
       TODO: SRV record support

       resolve(srvRecord, domain, bytes -> {
            int priority = bytes.getShort() & 0xFFFF;
            int weight =  bytes.getShort() & 0xFFFF;
            int port =  bytes.getShort() & 0xFFFF;

            int len;
            StringBuilder builder = new StringBuilder();
            while((len = bytes.get()) != 0){
                for(int j = 0; j < len; j++) builder.append((char)bytes.get());
                builder.append('.');
            }
            builder.delete(builder.length() - 1, builder.length());

            return "SRV Record: " + builder + " " + priority + " " + weight + " port=" +port;
        }, records -> {
            if(records.size > 0){
                Log.info("@ has SRV records: @", domain, records);
            }
        }, e -> {});
     */

    //TODO no SRV record support
    static void resolveAddress(String domain, Cons<InetAddress> result, Cons<Exception> error){

        //since parsing the address may be slow, check the cache first.
        var cachedIp = domainToIp.get(domain);
        if(cachedIp != null){
            try{
                result.get(cachedIp);
            }catch(Exception e){
                error.get(e);
            }
            return;
        }

        //attempt to resolve ipv4 or ipv6 address
        byte[] rawAddress = Addresses.getAddress(domain);
        if(rawAddress != null){
            try{
                var address = InetAddress.getByAddress(domain, rawAddress);
                domainToIp.put(domain, address);
                result.get(address);
            }catch(Exception e){
                error.get(e);
            }
            return;
        }

        resolve(aRecord, domain, bytes -> {
            byte[] address = new byte[4];
            bytes.get(address);
            return address;
        }, addresses -> {
            try{
                if(addresses.size > 0){
                    result.get(InetAddress.getByAddress(addresses.get(0)));
                }else{
                    //there are no records found - try AAAA instead.
                    resolve(aaaaRecord, domain, bytes -> {
                        byte[] address = new byte[16];
                        bytes.get(address);
                        return address;
                    }, addresses2 -> {

                        try{
                            if(addresses2.size > 0){
                                result.get(InetAddress.getByAddress(addresses2.get(0)));
                            }else{
                                //there are no records found
                                error.get(new UnresolvedAddressException());
                            }
                        }catch(UnknownHostException unknown){
                            error.get(unknown);
                        }
                    }, error);
                }
            }catch(UnknownHostException unknown){
                error.get(unknown);
            }
        }, error);
    }

    static <T> void send(Seq<InetSocketAddress> addresses, int index, int type, String domain, Func<ByteBuffer, T> reader, Cons<Seq<T>> recordResult, Cons<Exception> error){
        short id = (short)new Rand().nextInt(Short.MAX_VALUE);

        ByteBuffer buffer = ByteBuffer.allocate(512);

        buffer.putShort(id);             // Id
        buffer.putShort((short) 0x0100); // Flags (recursion enabled)
        buffer.putShort((short) 1);      // Questions
        buffer.putShort((short) 0);      // Answers
        buffer.putShort((short) 0);      // Authority
        buffer.putShort((short) 0);      // Additional

        // Domain
        for(String part : domain.split("\\.")){
            buffer.put((byte) part.length());
            buffer.put(part.getBytes(Strings.utf8));
        }
        buffer.put((byte) 0);

        buffer.putShort((short) type);   // Type
        buffer.putShort((short) 1);      // Class (Internet)

        buffer.flip();

        AsyncUdp.send(addresses.get(index), 2000, 512, buffer, result -> {
            short responseId = result.getShort();
            if(responseId != id) {
                throw new ArcRuntimeException("Invalid response ID");
            }

            result.getShort();
            result.getShort();
            int answers = result.getShort() & 0xFFFF;
            result.getShort();
            result.getShort();

            byte len;
            while((len = result.get()) != 0) {
                result.position(result.position() + len);
            }

            result.getShort();
            result.getShort();

            var records = new Seq<T>(answers);

            for(int i = 0; i < answers; i++) {
                result.getShort();                           // OFFSET
                int answerType = result.getShort() & 0xFFFF; // Type
                result.getShort();                           // Class
                result.getInt();                             // TTL
                int length = result.getShort() & 0xFFFF;     // Data length

                // Optionally CNAME results will be returned with the A results, skip those
                if(answerType != type){
                    result.position(result.position() + length);
                    continue;
                }

                int position = result.position();

                records.add(reader.get(result));

                result.position(position + length);
            }

            recordResult.get(records);
        }, e -> {
            if(index >= addresses.size - 1){
                error.get(e);
            }else{
                send(addresses, index + 1, type, domain, reader, recordResult, error);
            }
        });
    }
}
