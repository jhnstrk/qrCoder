//import java.util.Vector;

//Perform math in the finite field given by

public class FiniteField {

    class FiniteFieldMathException extends Exception {

        FiniteFieldMathException( String what ) 
        {
            super(what);
        }
        /**        */
        private static final long serialVersionUID = 1L;
        
    }

    // The primitive polynomial for this field
    final int generator;
    final int bitMask; // Bits in the field;
    final int highBit; // Highest bit of generator only, also the size of the field.
    final int nextHighBit; // highBit >> 1
    final int numBits; // number of bits in the field

    byte[] powersOfAlpha;
    byte[] inverseTable;
    byte[] logTable;

    public FiniteField(int gen)
    {
        final int highBitPos = BinaryHelper.highBitPos(gen);
        this.generator = gen;
        this.numBits   = highBitPos;
        this.highBit   = (1 <<  highBitPos );
        this.nextHighBit = this.highBit >> 1;
        this.bitMask   = this.highBit - 1;

        this.computePowersOfAlpha();
        this.computeInverseTable();
    }

    public byte add( byte x, byte y){
        return (byte)((int)x^(int)y);
    }

    public byte sub( byte x, byte y){
        return (byte)((int)x^(int)y);
    }

    public byte mul( byte x, byte y){
        int ret = 0;
        int yShift = y & this.bitMask;
        for ( int i=0; i<this.numBits; ++i) {
            if ( (x & ( 1 << i)) != 0) {
                ret = ret ^ (yShift);
            }
            boolean carry = ((yShift & this.nextHighBit) != 0);
            yShift = (yShift << 1);
            if (carry) {
                yShift = yShift ^ this.generator;
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

    byte multiplicativeInverse( byte x ) {
        return this.inverseTable[(int)x & this.bitMask ];
    }

    void computeInverseTable()
    {
        this.inverseTable = new byte[ this.highBit ];
        for (int i=0; i<this.highBit; ++i) {
            this.inverseTable[i] = this.computeMultiplicativeInverse((byte)i);
        }
    }

    byte computeMultiplicativeInverse( byte x )
    {
        // The extended Euclidean algorithm for finite fields
        int ri1 = this.generator;
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
        this.powersOfAlpha = new byte[this.highBit - 1];
        this.logTable = new byte[this.highBit];
        this.logTable[0] = 0;
        this.powersOfAlpha[0] = (byte)1;
        this.logTable[1] = 0;
        int x = 1;
        for (int i=1; i<this.highBit - 1; ++i) {
            x <<= 1;
            if ( (x & this.highBit) != 0 ) {
                x ^= this.generator;
            }
            this.powersOfAlpha[i] = (byte)x;
            this.logTable[x] = (byte)i;
        }
    }

    public byte pow( byte power )
    {
        // No range checking. But power should be 1 <= x <= 255
        return this.powersOfAlpha[((int)power & 0xFF)];
    }

    public byte pow( int power )
    {
        // No range checking. But power should be 1 <= x <= 255
        return this.powersOfAlpha[power];
    }

    public int log( byte v )
    {
        return ((int)this.logTable[ (int)v & 0xFF ]) & 0xFF;
        //     for (int i = 0; i<256; ++i) {
        //         if (this.pow(i) == v) {
        //             return i;
        //         }
        //     }
        //     return 0;
        //throw new FiniteFieldMathException("Log zero");
    }

    // Multiplication by repeated addition or subtraction.
    public byte ordinaryMul( int n, byte v) {

        // Since addition is the same as subtraction, and both are XOR, 
        // applying addition twice returns 0. So the result is only non-zero if
        // n is not a multiple of 2.
        if ( (n & 1) != 0 ) {
            return v;
        }
        return 0;
    }

    public static boolean test()
    {
        return test256() && test32();
    }

    public static boolean test256()
    {
        final int generator256 = 0x011d; // x^8 + x^4 + x^3 + x^2 + 1. or 100011101
        FiniteField obj = new FiniteField(generator256);
        boolean allTestsPassed = true;

        if ( obj.generator != BinaryHelper.fromBinaryString("100011101") ) {
            System.err.println("Wrong generator: Got " + obj.generator +
                    " expected " + BinaryHelper.fromBinaryString("100011101") );
            allTestsPassed = false;
        }
        if ( obj.pow((byte)0) != 1 ) {
            System.err.println("Expected alpha^0 == 1. Got : " + obj.pow((byte)0) );
            allTestsPassed = false;
        }
        if ( obj.pow((byte)1) != 2 ) {
            System.err.println("Expected alpha^1 == 2. Got : " + obj.pow((byte)1) );
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
                    if (obj.log(bvi) != j) {
                        allTestsPassed = false;
                        System.err.println("log( a^j ) != j for " + j + " " + obj.log(bvi) + " " + bvi);
                    }
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
                int m2 = Poly2.mod(m1, obj.generator);
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

        System.out.println("QrFiniteField:test256() : Passed = " + allTestsPassed );
        return allTestsPassed;
    }

    public static boolean test32()
    {
        final int generator32  = 0x25;   // x^5 + x^2 + 1. or 100101
        final int SIZE=32;
        boolean allTestsPassed = true;
        FiniteField obj = new FiniteField(generator32);

        if ( obj.generator != BinaryHelper.fromBinaryString("100101") ) {
            System.err.println("Wrong generator: Got " + obj.generator +
                    " expected " + BinaryHelper.fromBinaryString("100101") );
            allTestsPassed = false;
        }
        if ( obj.pow((byte)0) != 1 ) {
            System.err.println("Expected alpha^0 == 1. Got : " + obj.pow((byte)0) );
            allTestsPassed = false;
        }
        if ( obj.pow((byte)1) != 2 ) {
            System.err.println("Expected alpha^1 == 2. Got : " + obj.pow((byte)1) );
            allTestsPassed = false;
        }

        if (obj.pow(25) != (byte)BinaryHelper.fromBinaryString("11001")) {
            System.err.println("Expected alpha^25 == 25. Got : " + obj.pow((byte)25) );
            allTestsPassed = false;
        }

        final byte aVal = SIZE / 2 + SIZE /4;
        byte invAVal = obj.multiplicativeInverse(aVal);

        if ( invAVal != (byte)17 ) {
            System.err.println("Inverse test gave wrong value");
            allTestsPassed = false;
        }

        for ( int i=0; i<SIZE; ++i) {
            byte bvi = (byte)i;
            boolean found = false;
            for ( int j=0; j<SIZE-1; ++j) {
                if ( obj.powersOfAlpha[j] == bvi) {
                    found = true;
                    if (obj.log(bvi) != j) {
                        allTestsPassed = false;
                        System.err.println("log( a^j ) != j for " + j + " " + obj.log(bvi) + " " + bvi);
                    }
                    break;
                }
            }
            if ( !found && (bvi != 0)) {
                allTestsPassed = false;
                System.err.println("Did not find value in field." + i);
            }
        }

        for ( int i=0; i<SIZE; ++i) {
            byte bvi = (byte)i;
            for ( int j=0; j<SIZE; ++j) {
                byte bvj = (byte)j;
                byte mul1 = obj.mul(bvi, bvj);
                byte mul2 = obj.mul(bvj, bvi);
                if ( mul1 != mul2 ) {
                    System.err.println("Multiplication not associative / distributive / commutative");
                    allTestsPassed = false;
                }
                int m1 = Poly2.mul(i, j);
                int m2 = Poly2.mod(m1, obj.generator);
                byte bm2 = (byte)m2;
                if ( bm2 != mul1 ) {
                    System.err.println("Multiplication does not match reference implementation");
                    allTestsPassed = false;
                }
            }
        }

        for ( int i=0; i<SIZE; ++i) {
            byte bv = (byte)i;
            byte mulVal = obj.mul(aVal, bv);
            byte addVal = obj.add(aVal, bv);
            byte subVal = obj.sub(aVal, bv);
            byte divVal = obj.div(mulVal, bv);
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

            if ( ( aVal != divVal )  && (bv != 0) ) {
                System.err.println("Multiply / Divide test failed ( A * B ) / A != B");
                allTestsPassed = false;
            }

            byte inv1 = obj.mul(bv, invVal);
            if ( ( inv1 != 1) && (bv != 0) ) {
                System.err.println("Inverse test failed");
                allTestsPassed = false;
            }
        }

        System.out.println("QrFiniteField:test32() : Passed = " + allTestsPassed );
        return allTestsPassed;
    }
}

