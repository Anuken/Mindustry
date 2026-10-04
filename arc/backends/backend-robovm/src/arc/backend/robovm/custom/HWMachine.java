package arc.backend.robovm.custom;

import org.robovm.rt.bro.Bro;
import org.robovm.rt.bro.Struct;
import org.robovm.rt.bro.annotation.Bridge;
import org.robovm.rt.bro.annotation.Library;
import org.robovm.rt.bro.annotation.MachineSizedSInt;
import org.robovm.rt.bro.ptr.BytePtr;
import org.robovm.rt.bro.ptr.MachineSizedUIntPtr;
import org.robovm.rt.bro.ptr.VoidPtr;

@Library("c")
public class HWMachine{
    static{
        Bro.bind();
    }

    @Bridge
    public static native int sysctlbyname(BytePtr name, VoidPtr oldp,
                                          MachineSizedUIntPtr oldlenp, VoidPtr newp,
                                          @MachineSizedSInt long newlen);

    public static String getMachineString(){
        BytePtr name = BytePtr.toBytePtrAsciiZ("hw.machine");
        MachineSizedUIntPtr size = new MachineSizedUIntPtr();
        sysctlbyname(name, null, size, null, 0);
        BytePtr machine = Struct.allocate(BytePtr.class, (int)size.get());
        sysctlbyname(name, machine.as(VoidPtr.class), size, null, 0);
        return machine.toStringAsciiZ();
    }

}
