

// Need to decode BCH codes using the 15,5 coding
public class BchCodec15_5 {
    final int dataLen = 5;
    final byte dataMask = ((1 << dataLen) - 1 );
    final int codeWordLen = 15;
    final int codeWordEnd = (1 << codeWordLen);
    final short genPoly = ( 1 << 10 ) | ( 1 << 8 ) | ( 1 << 5 ) | ( 1 << 4 ) | ( 1 << 2 ) | ( 1 << 1 ) | 1;
    private byte lut[];

    public BchCodec15_5()
    {
        System.out.println( "Generator : " + asBinaryString(genPoly) );

        lut = new byte[codeWordEnd];
        for ( int i = 0; i < codeWordEnd; ++i) {
            lut[i] = -1;
        }

        byte end = ( 1 << dataLen );
        for ( byte codeVal = 0; codeVal < end; ++codeVal) {
            short codeWord = this.encode( codeVal );
            lut[codeWord] = codeVal;

            System.out.println( asBinaryString(codeVal) + " -> " + asBinaryString(codeWord) );

            // Add bit errors
            for ( int i = 0; i<codeWordLen; ++i){
                int errCodeWordI = (1 << i); 
                for ( int j = i; j<codeWordLen; ++j) {
                    int bitCount1 = (i == j) ? 1 : 2;
                    int errCodeWordJ = errCodeWordI | (1 << j); 
                    for (int k = j; k<codeWordLen; ++k) {
                        int bitCount = bitCount1 + ( (j == k) ? 0 : 1 ); 
                        int errCodeWordK = errCodeWordJ | (1 << k);
                        int codeWithErrors = codeWord ^ errCodeWordK; // XOR
                        
                        byte newVal = (byte)(codeVal | ( bitCount << dataLen ));
                        if ( lut[codeWithErrors] != -1 && ( lut[codeWithErrors] != newVal) ) {
                            System.err.println("Collision : "
                                    + asBinaryString(codeWord)
                                    + " ^ " + asBinaryString(errCodeWordK,16)
                                    + " = " + asBinaryString(codeWithErrors,16)
                                    );
                        }
                        lut[codeWithErrors] = newVal;
                    }
                }
            } // bit error loop
        }  // data value loop
        {
            // sanity checking
            int numInValid = 0;
            int numError[] = new int[4];
            numError[0] = 0;
            numError[1] = 0;
            numError[2] = 0;
            numError[3] = 0;
            for ( int i = 0; i < codeWordEnd; ++i){
                byte value =lut[i];
                if ( value < 0) {
                    ++numInValid;
                } else {
                    int errCount = ( value >> dataLen ) & 3;
                    ++(numError[errCount]);
                }
            }
            System.out.println("Invalid : " + numInValid);
            System.out.println("0 Errs : " + numError[0]);
            System.out.println("1 Errs : " + numError[1]);
            System.out.println("2 Errs : " + numError[2]);
            System.out.println("3 Errs : " + numError[3]);
        }
    }

    public byte decode(short codeWord) 
    {
        if ( (codeWord < 0) || (codeWord >= lut.length ) ) {
            System.err.println("Bad codeword " + codeWord);
            return -1;
        }

        byte lutVal = lut[codeWord];
        if ( lutVal < 0 ) {
            return -1;      // Not a valid codeword /  too many errors
        }
        byte retVal = (byte)(lutVal & dataMask);
        //int errCount = ( lutVal >> dataLen ) & 3;
        return retVal;
    }

    public short encode( byte codeVal )
    {
        // Need 16 bits for this.
        int maskedCode = codeVal & dataMask;
        int codedVal = maskedCode << 10; // * x^10
        codedVal = Poly2.mod(codedVal , genPoly );  // Modulo the generator polynomial.
        return (short)(codedVal | (maskedCode << 10));
    }

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
        return asBinaryString( (int)num, 8);
    }

    public static String asBinaryString( short num )
    {
        return asBinaryString( (int)num, 16);
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

    public static boolean test()
    {
        //BchCodec15_5 coder = new BchCodec15_5();
        int testVal = 123;
        String testStr = BchCodec15_5.asBinaryString(testVal, 17);
        int testVal2   = BchCodec15_5.fromBinaryString(testStr);
        if ( testVal != testVal2 ) {
            return false;
        }

        if ( BchCodec15_5.nonZeroBitCount(1) != 1)
            return false;

        if ( BchCodec15_5.nonZeroBitCount((1<<4) | (1<<2) ) != 2)
            return false;

        if ( BchCodec15_5.highBitPos((1<<4)) != 4)
            return false;

        testVal = BchCodec15_5.fromBinaryString("1000111110101111");
        int testValNum = BchCodec15_5.fromBinaryString("1000001000000000000000");

        int remainder = Poly2.mod( testValNum ,testVal );
        int expectRemainder =BchCodec15_5.fromBinaryString("100101000100010");
        
        if ( remainder !=- expectRemainder) {
            return false;
        }

        BchCodec15_5 coder = new BchCodec15_5();

        short codeTest = coder.encode( (byte)testVal );
        byte decodeTest = coder.decode(codeTest);

        if ( decodeTest != testVal ) {
            System.err.println(" Encode / Decode " + testVal +" failed. Got " + decodeTest);
            return false;
        }
        decodeTest = coder.decode((short)(codeTest ^ ( 1 << 5)));
        if ( decodeTest != testVal ) {
            System.err.println(" Encode / Decode 1 bit err " + testVal +" failed. Got " + decodeTest);
            return false;
        }

        decodeTest = coder.decode((short)(codeTest ^ ( 1 << 5) ^ (1 << 10) ^( 1 << 7)));
        if ( decodeTest != testVal ) {
            System.err.println(" Encode / Decode 3 bit errs " + testVal +" failed. Got " + decodeTest);
            return false;
        }

        decodeTest = coder.decode( (short)(codeTest ^ ( 1 + 2 + 4 + 16)) );
        if ( decodeTest != -1 ) {
            System.err.println(" Encode / Decode 4 bit errs " + testVal +" failed. Got " + decodeTest);
            return false;
        }

        return true;
    }
}
