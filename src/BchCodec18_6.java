
public class BchCodec18_6 {
    final int codeWordLen = 18;
    final int dataLen = 6;
    final int errorCorrLen = codeWordLen - dataLen;
    final int dataMask = ((1 << dataLen) - 1 );
    final int codeWordEnd = (1 << codeWordLen);
    final int genPoly = ( 1 << 12 ) | ( 1 << 11 ) | ( 1 << 10 ) 
            | ( 1 << 9 ) | ( 1 << 8 ) | ( 1 << 5 ) | ( 1 << 2 ) | 1;

    final FiniteField m_gf32;

    public BchCodec18_6() {
        final int generator32  = 0x25;   // x^5 + x^2 + 1. or 100101
        m_gf32 = new FiniteField( generator32 );
    }
    
    public int encode( int codeVal )
    {
        // Need dataLen + codeWordLen bits for this.
        int maskedCode = codeVal & dataMask;
        if (maskedCode != (int)codeVal) {
            System.err.println("BchCodec18_6: Warning : input value out of range");
        }
        int codedVal = maskedCode << errorCorrLen;
        codedVal = Poly2.mod(codedVal , genPoly );  // Modulo the generator polynomial.
        return (codedVal | (maskedCode << errorCorrLen));
    }

    public int decode( int codeWord )
    {
        //TODO: Implement function
        return 0;
    }

    public byte evaluate(int poly, byte value)
    {
        byte ret = 0;
        int polyS = poly & (0x7FFFFFFF);
        byte vPower = 1;
        while ( polyS != 0 ) {
            int coeff = polyS & 1;
            ret += coeff * vPower;
            vPower = m_gf32.mul( vPower, value );
            polyS >>= 1;
        }
        return ret;
    }

    public static boolean test()
    {
        boolean allTestsPassed = true;

        BchCodec18_6 obj = new BchCodec18_6();

        final int expectedCodeWord = BinaryHelper.fromBinaryString("000111110010010100");
        final int vIn = 7;

        int testCoded = obj.encode(vIn);
        if ( testCoded != expectedCodeWord) {
            allTestsPassed = false;
            System.err.println("BchCodec18_6 encoding failed");
        }

        if ( allTestsPassed ){
            System.out.println("BchCodec18_6 test passed");
        }
        return allTestsPassed;
    }
}
