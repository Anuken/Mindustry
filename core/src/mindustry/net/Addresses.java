package mindustry.net;

/** Utility class for parsing IPv4/IPv6. Taken from IPAddressUtil in the JDK. */
public class Addresses{

    /** @return the IPv4 or IPv6 address of the string, or null if the address is not valid. */
    public static byte[] getAddress(String src){
        byte[] ipv4 = getIpv4(src);
        if(ipv4 != null) return ipv4;
        return getIpv6(src);
    }

    /** @return the IPv4 address of the string, or null if the address is not valid. Exotic formats (such as decimal) are allowed. */
    public static byte[] getIpv4(String src){
        byte[] res = new byte[4];
        long tmpValue = 0L;
        int currByte = 0;
        boolean newOctet = true;
        int len = src.length();
        if(len != 0 && len <= 15){
            for(int i = 0; i < len; ++i){
                char c = src.charAt(i);
                if(c == '.'){
                    if(newOctet || tmpValue < 0L || tmpValue > 255L || currByte == 3){
                        return null;
                    }

                    res[currByte++] = (byte)((int)(tmpValue & 255L));
                    tmpValue = 0L;
                    newOctet = true;
                }else{
                    int digit = digit(c, 10);
                    if(digit < 0){
                        return null;
                    }

                    tmpValue *= 10L;
                    tmpValue += (long)digit;
                    newOctet = false;
                }
            }

            if(!newOctet && tmpValue >= 0L && tmpValue < 1L << (4 - currByte) * 8){
                switch(currByte){
                    case 0:
                        res[0] = (byte)((int)(tmpValue >> 24 & 255L));
                    case 1:
                        res[1] = (byte)((int)(tmpValue >> 16 & 255L));
                    case 2:
                        res[2] = (byte)((int)(tmpValue >> 8 & 255L));
                    case 3:
                        res[3] = (byte)((int)(tmpValue >> 0 & 255L));
                    default:
                        return res;
                }
            }else{
                return null;
            }
        }else{
            return null;
        }
    }

    /** @return the IPv6 address of the string, or null if the address is not valid. */
    public static byte[] getIpv6(String src){
        if(src.length() < 2){
            return null;
        }else{
            char[] srcb = src.toCharArray();
            byte[] dst = new byte[16];
            int srcb_length = srcb.length;
            int pc = src.indexOf(37);
            if(pc == srcb_length - 1){
                return null;
            }else{
                if(pc != -1){
                    srcb_length = pc;
                }

                int colonp = -1;
                int i = 0;
                int j = 0;
                if(srcb[i] == ':'){
                    ++i;
                    if(srcb[i] != ':'){
                        return null;
                    }
                }

                int curtok = i;
                boolean saw_xdigit = false;
                int val = 0;

                while(true){
                    int n;
                    while(i < srcb_length){
                        char ch = srcb[i++];
                        n = digit(ch, 16);
                        if(n != -1){
                            val <<= 4;
                            val |= n;
                            if(val > 65535){
                                return null;
                            }

                            saw_xdigit = true;
                        }else{
                            if(ch != ':'){
                                if(ch == '.' && j + 4 <= 16){
                                    String ia4 = src.substring(curtok, srcb_length);
                                    int dot_count = 0;

                                    for(int index = 0; (index = ia4.indexOf(46, index)) != -1; ++index){
                                        ++dot_count;
                                    }

                                    if(dot_count != 3){
                                        return null;
                                    }

                                    byte[] v4addr = getIpv4(ia4);
                                    if(v4addr == null){
                                        return null;
                                    }

                                    for(int k = 0; k < 4; ++k){
                                        dst[j++] = v4addr[k];
                                    }

                                    saw_xdigit = false;
                                    break;
                                }

                                return null;
                            }

                            curtok = i;
                            if(!saw_xdigit){
                                if(colonp != -1){
                                    return null;
                                }

                                colonp = j;
                            }else{
                                if(i == srcb_length){
                                    return null;
                                }

                                if(j + 2 > 16){
                                    return null;
                                }

                                dst[j++] = (byte)(val >> 8 & 255);
                                dst[j++] = (byte)(val & 255);
                                saw_xdigit = false;
                                val = 0;
                            }
                        }
                    }

                    if(saw_xdigit){
                        if(j + 2 > 16){
                            return null;
                        }

                        dst[j++] = (byte)(val >> 8 & 255);
                        dst[j++] = (byte)(val & 255);
                    }

                    if(colonp != -1){
                        n = j - colonp;
                        if(j == 16){
                            return null;
                        }

                        for(i = 1; i <= n; ++i){
                            dst[16 - i] = dst[colonp + n - i];
                            dst[colonp + n - i] = 0;
                        }

                        j = 16;
                    }

                    if(j != 16){
                        return null;
                    }

                    byte[] newdst = convertFromIPv4MappedAddress(dst);
                    if(newdst != null){
                        return newdst;
                    }

                    return dst;
                }
            }
        }
    }

    private static boolean isIPv4MappedAddress(byte[] addr){
        if(addr.length < 16){
            return false;
        }else{
            return addr[0] == 0 && addr[1] == 0 && addr[2] == 0 && addr[3] == 0 && addr[4] == 0 && addr[5] == 0 && addr[6] == 0 && addr[7] == 0 && addr[8] == 0 && addr[9] == 0 && addr[10] == -1 && addr[11] == -1;
        }
    }

    private static byte[] convertFromIPv4MappedAddress(byte[] addr){
        if(isIPv4MappedAddress(addr)){
            byte[] newAddr = new byte[4];
            System.arraycopy(addr, 12, newAddr, 0, 4);
            return newAddr;
        }else{
            return null;
        }
    }

    private static int digit(char ch, int radix){
        return parseAsciiDigit(ch, radix);
    }

    private static int parseAsciiDigit(char c, int radix){
        if(radix == 16){
            char c1 = Character.toLowerCase(c);
            return c1 >= 'a' && c1 <= 'f' ? c1 - 97 + 10 : parseAsciiDigit(c1, 10);
        }else{
            int val = c - 48;
            return val >= 0 && val < radix ? val : -1;
        }
    }

}
