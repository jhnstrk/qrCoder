// Perform math in the finite field given by

public class QrFiniteField {
    // The primitive polynomial for this field
    static int generator = 0x011d; // x^8 + x^4 + x^3 + x^2 + 1. or 100011101

    byte[] powersOfAlpha;

    public QrFiniteField()
    {
        this.computePowersOfAlpha();
    }
    public byte add( byte x, byte y){
        return (byte)((int)x^(int)y);
    }

    public byte sub( byte x, byte y){
        return (byte)((int)x^(int)y);
    }

    public byte mul( byte x, byte y){
        int ret = 0;
        int yShift = y & 0xFF;
        for ( int i=0; i<8; ++i) {
            if ( (x & ( 1 << i)) != 0) {
                ret = ret ^ (yShift);
            }
            boolean carry = ((yShift & 0x80) != 0);
            yShift = (yShift << 1);
            if (carry) {
                yShift = yShift ^ QrFiniteField.generator;
            }
        }
        return (byte)ret;
    }

    // Return x/y
    // Division in the finite field is multiplication by the multiplicative inverse.
    // The multiplicative inverse is the number (y) which gives x * y = 1
    public byte div( byte x, byte y)
    {
        byte inv = this.multiplicativeInverse(y);
        return this.mul(x, inv);
    }

    byte multiplicativeInverse( byte x )
    {
        // The extended Euclidean algorithm for finite fields
        int ri1 = QrFiniteField.generator;
        int ri  = (int)x & 0xFF;  // Unsigned.
        byte ai1 = 0;
        byte ai = 1;
        while (ri > 1) {
            byte ai2 = ai1;
            ai1 = ai;
            int ri2 = ri1;
            ri1 = ri;
            Poly2.DivResult res = Poly2.divQr(ri2, ri1);
            ri = res.remainder;
            ai = this.sub( ai2, this.mul((byte)res.quotient, ai1));
        }
        return (byte)ai;
    }

    void computePowersOfAlpha()
    {
        this.powersOfAlpha = new byte[255];
        this.powersOfAlpha[0] = (byte)1;
        int x = 1;
        for (int i=1; i<255; ++i) {
            x <<= 1;
            if ( (x & 0x100) != 0 ) {
                x ^= QrFiniteField.generator;
            }
            this.powersOfAlpha[i] = (byte)x;
        }
    }

    public byte getAlphaPower( byte power )
    {
        // No range checking. But power should be 1 <= x <= 255
        return this.powersOfAlpha[((int)power & 0xFF)];
    }

    public static boolean test()
    {
        QrFiniteField obj = new QrFiniteField();
        boolean allTestsPassed = true;

        if ( QrFiniteField.generator != BinaryHelper.fromBinaryString("100011101") ) {
            System.err.println("Wrong generator: Got " + QrFiniteField.generator +
                    " expected " + BinaryHelper.fromBinaryString("100011101") );
            allTestsPassed = false;
        }
        if ( obj.getAlphaPower((byte)0) != 1 ) {
            System.err.println("Expected alpha^0 == 1. Got : " + obj.getAlphaPower((byte)0) );
            allTestsPassed = false;
        }
        if ( obj.getAlphaPower((byte)1) != 2 ) {
            System.err.println("Expected alpha^1 == 2. Got : " + obj.getAlphaPower((byte)1) );
            allTestsPassed = false;
        }
        byte aVal = 0x4F;
        byte invAVal = obj.multiplicativeInverse(aVal);

        if ( invAVal != (byte)-109 ) {
            System.err.println("Inverse test gave wrong value");
            allTestsPassed = false;
        }

        for ( int i=0; i<256; ++i) {
            byte bvi = (byte)i;
            boolean found = false;
            for ( int j=0; j<255; ++j) {
                if ( obj.powersOfAlpha[j] == bvi) {
                    found = true;
                    break;
                }
            }
            if ( !found && (bvi != 0)) {
                allTestsPassed = false;
                System.err.println("Did not find value in field." + i);
            }
        }

        for ( int i=0; i<256; ++i) {
            byte bvi = (byte)i;
            for ( int j=0; j<256; ++j) {
                byte bvj = (byte)j;
                byte mul1 = obj.mul(bvi, bvj);
                byte mul2 = obj.mul(bvj, bvi);
                if ( mul1 != mul2 ) {
                    System.err.println("Multiplication not associative / distributive / commutative");
                    allTestsPassed = false;
                }
                int m1 = Poly2.mul(i, j);
                int m2 = Poly2.mod(m1, QrFiniteField.generator);
                byte bm2 = (byte)m2;
                if ( bm2 != mul1 ) {
                    System.err.println("Multiplication does not match reference implementation");
                    allTestsPassed = false;
                }
            }
        }

        for ( int i=0; i<256; ++i) {
            byte bv = (byte)i;
            // byte mulVal = obj.mul(aVal, bv);
            byte addVal = obj.add(aVal, bv);
            byte subVal = obj.sub(aVal, bv);
            // byte divVal = obj.div(aVal, bv);
            byte invVal = obj.multiplicativeInverse(bv);
            //System.out.println( "For " + i + ": "
            //        + " * " + BinaryHelper.asBinaryString(mulVal)
            //        + " + " + BinaryHelper.asBinaryString(addVal)
            //        + " - " + BinaryHelper.asBinaryString(subVal)
            //        + " / " + BinaryHelper.asBinaryString(divVal)
            //        + " ^-1 " + BinaryHelper.asBinaryString(invVal) );

            if ( addVal != subVal ) {
                System.err.println("Addition / subtraction test failed");
                allTestsPassed = false;
            }

            byte inv1 = obj.mul(bv, invVal);
            if ( ( inv1 != 1) && (bv != 0) ) {
                System.err.println("Inverse test failed");
                allTestsPassed = false;
            }
        }

        System.out.println("QrFiniteField:test() : " + allTestsPassed );
        return allTestsPassed;
    }
}
