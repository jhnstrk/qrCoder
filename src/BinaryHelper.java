
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
        String str = new String();
        len = Math.min(len,32);
        for ( int i=0; i < len; ++i ) {
            if ( ( (num >> (len - 1 - i)) & 1 ) == 0 ) {
                str += "0";
            } else {
                str += "1";
            }
        }
        str += "(" + num +")";
        return str;
    }

    public static int fromBinaryString(String str)
    {
        int num = 0;
        for ( int i=0; i<str.length(); ++i)
        {
            if ( str.charAt(i) == '1' ) {
                num = (num << 1) | 1;
            } else if ( str.charAt(i) == '0'){
                num = (num << 1);
            } else if ( str.charAt(i) != ' '){
                return num;  // Stop at first non 0 / 1 /
            }
        }
        return num;
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
