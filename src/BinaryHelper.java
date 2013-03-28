import java.util.Arrays;


public class BinaryHelper {
    public static int nonZeroBitCount( int num )
    {
        int count = 0;
        for ( int i=0; i < 32; ++i ) {
            count +=  ((num >> i ) & 1);
        }
        return count;
    }

    public static String asBinaryString( byte num )
    {
        return asBinaryString( num & 0xFF, 8);
    }

    public static String asBinaryString( short num )
    {
        return asBinaryString( num & 0xFFFF, 16);
    }

    public static String asBinaryString( int num, int len )
    {
        String str = Integer.toString(num, 2);
        if (str.length() < len) {
            char [] buff = new char[len - str.length()];
            Arrays.fill(buff, '0');
            str = (new String(buff)) + str;
        } else if ( str.length() > len) {
            str = str.substring(str.length() - len, str.length());
        }
        return str;
    }

    public static int fromBinaryString(String str)
    {
        return Integer.parseInt(str,2);
    }

    public static byte[] byteFromBinaryStringArray(String[] str)
    {
        if (str == null)
            return null;
        byte[] ret = new byte[str.length];
        for (int i=0; i< str.length; ++i)
        {
            ret[i] = (byte)fromBinaryString(str[i]);
        }
        return ret;
    }

    public static int highBitPos( int val )
    {
        if (val == 0 ) {
            return -1;
        }
        int num = 31;
        while ( ((val >>> num) & 1) == 0 ) {
            --num;
        }
        return num;
    }

}
