
// Finite field, for up to 2^31.
public class FiniteFieldInt {
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

    int[] powersOfAlpha;
    int[] inverseTable;
    int[] logTable;

    public FiniteFieldInt(int gen)
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

    public int add( int x, int y){
        return x^y;
    }

    public int sub( int x, int y){
        return x^y;
    }

    public int mul( int x, int y){
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
        return ret;
    }

    // Return x/y
    // Division in the finite field is multiplication by the multiplicative inverse.
    // The multiplicative inverse is the number (y) which gives x * y = 1
    public int div( int x, int y)
    {
        int inv = this.multiplicativeInverse(y);
        return this.mul(x, inv);
    }

    int multiplicativeInverse( int x ) {
        return this.inverseTable[x & this.bitMask ];
    }

    void computeInverseTable()
    {
        this.inverseTable = new int[ this.highBit ];
        for (int i=0; i<this.highBit; ++i) {
            this.inverseTable[i] = this.computeMultiplicativeInverse((int)i);
        }
    }

    int computeMultiplicativeInverse( int x )
    {
        // The extended Euclidean algorithm for finite fields
        int ri1 = this.generator;
        int ri  = x;  // Unsigned.
        int ai1 = 0;
        int ai = 1;
        while (ri > 1) {
            int ai2 = ai1;
            ai1 = ai;
            int ri2 = ri1;
            ri1 = ri;
            Poly2.DivResult res = Poly2.divQr(ri2, ri1);
            ri = res.remainder;
            ai = this.sub( ai2, this.mul((int)res.quotient, ai1));
        }
        return (int)ai;
    }

    void computePowersOfAlpha()
    {
        this.powersOfAlpha = new int[this.highBit - 1];
        this.logTable = new int[this.highBit];
        this.logTable[0] = 0;
        this.powersOfAlpha[0] = (int)1;
        this.logTable[1] = 0;
        int x = 1;
        for (int i=1; i<this.highBit - 1; ++i) {
            x <<= 1;
            if ( (x & this.highBit) != 0 ) {
                x ^= this.generator;
            }
            this.powersOfAlpha[i] = (int)x;
            this.logTable[x] = (int)i;
        }
    }

    public int computePow( int power )
    {
        int ret = 1;
        int alpha2n = 2;
        int shiftedPower = power;
        while ( shiftedPower != 0) {
            if ( (shiftedPower & 1) != 0) {
                ret = this.mul(ret, alpha2n);
            }
            shiftedPower >>= 1;
            alpha2n = this.mul(alpha2n, alpha2n);
        }
        return ret;
    }

    //As pow, but allow out-of-range
    public int pow( int power )
    {
        if (power >= this.bitMask ) {
            power = power % this.bitMask;
        } else if ( power < 0 ) {
            power = (power % this.bitMask);
            if (power < 0) {
                power += this.bitMask;
            }
        }
        return this.powersOfAlpha[power];
    }
 
    //! Raise v to given power
    //  pow(0,0) gives 1
    public int pow(int v, int power )
    {
        int logv = this.log(v);
        logv *= power;
        return this.pow(logv);
    }

    public int log( int v )
    {
        return this.logTable[ v ];
        //     for (int i = 0; i<256; ++i) {
        //         if (this.pow(i) == v) {
        //             return i;
        //         }
        //     }
        //     return 0;
        //throw new FiniteFieldMathException("Log zero");
    }

    // Multiplication by repeated addition or subtraction.
    public int ordinaryMul( int n, int v) {

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
        FiniteFieldInt obj = new FiniteFieldInt(generator256);
        boolean allTestsPassed = true;

        if ( obj.generator != BinaryHelper.fromBinaryString("100011101") ) {
            System.err.println("Wrong generator: Got " + obj.generator +
                    " expected " + BinaryHelper.fromBinaryString("100011101") );
            allTestsPassed = false;
        }

        int aVal = 0x4F;
        int invAVal = obj.multiplicativeInverse(aVal);

        if ( invAVal != 147 ) {
            System.err.println("Inverse test gave wrong value");
            allTestsPassed = false;
        }

        allTestsPassed &= obj.testCommon();
        System.out.println(obj.getClass().getName() + ":test256() : Passed = " + allTestsPassed );
        return allTestsPassed;
    }

    public static boolean test32()
    {
        boolean allTestsPassed = true;
        final int generator32  = 0x25;   // x^5 + x^2 + 1. or 100101
        final int SIZE=32;
        FiniteFieldInt obj = new FiniteFieldInt(generator32);
        if ( obj.generator != BinaryHelper.fromBinaryString("100101") ) {
            System.err.println("Wrong generator: Got " + obj.generator +
                    " expected " + BinaryHelper.fromBinaryString("100101") );
            allTestsPassed = false;
        }

        final int aVal = SIZE / 2 + SIZE /4;
        int invAVal = obj.multiplicativeInverse(aVal);

        if ( invAVal != (int)17 ) {
            System.err.println("Inverse test gave wrong value");
            allTestsPassed = false;
        }

        if (obj.pow(25) != (int)BinaryHelper.fromBinaryString("11001")) {
            System.err.println("Expected alpha^25 == 25. Got : " + obj.pow((int)25) );
            allTestsPassed = false;
        }

        allTestsPassed &= obj.testCommon();
        System.out.println(obj.getClass().getName() + ":test32() : Passed = " + allTestsPassed );
        return allTestsPassed;
    }

    public boolean testCommon()
    {
        final int SIZE=this.highBit;
        boolean allTestsPassed = true;

        final int aVal = (int)(SIZE / 2 + SIZE /4);

        if ( this.pow((int)0) != 1 ) {
            System.err.println("Expected alpha^0 == 1. Got : " + this.pow((int)0) );
            allTestsPassed = false;
        }
        if ( this.pow((int)1) != 2 ) {
            System.err.println("Expected alpha^1 == 2. Got : " + this.pow((int)1) );
            allTestsPassed = false;
        }

        if ( this.highBit > 2) {
            if (this.pow((int)2, 12345) != this.pow(12345) ){
                System.err.println("Unexpected alpha^12345");
                allTestsPassed = false;
            }
    
            if (this.mul((int)2, this.pow(SIZE-2)) != (int)1 ){
                System.err.println("Unexpected pow.");
                allTestsPassed = false;
            }
        }

        for ( int i=0; i<SIZE; ++i) {
            if ( this.computePow(i) != this.pow(i) ) {
                System.err.println("Pow computation failure.");
            }
        }
        {
            int bVal = this.add(aVal, (int)1);
            int loga = log(aVal);
            int logb = log(bVal);
            if ( this.mul(aVal, bVal) != this.pow(loga + logb)) {
                System.err.println("Multiplication by logs failed.");
                allTestsPassed = false;
            }
            if ( this.div(aVal, bVal) != this.pow(loga - logb)) {
                System.err.println("Division by logs failed");
                allTestsPassed = false;
            }
            if ( this.div(bVal, aVal) != this.pow(logb - loga)) {
                System.err.println("Division by logs failed");
                allTestsPassed = false;
            }
        }
        for ( int i=0; i<SIZE; ++i) {
            int bvi = (int)i;
            boolean found = false;
            for ( int j=0; j<SIZE-1; ++j) {
                if ( this.powersOfAlpha[j] == bvi) {
                    found = true;
                    if (this.log(bvi) != j) {
                        allTestsPassed = false;
                        System.err.println("log( a^j ) != j for " + j + " " + this.log(bvi) + " " + bvi);
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
            int bvi = (int)i;
            for ( int j=0; j<SIZE; ++j) {
                int bvj = (int)j;
                int mul1 = this.mul(bvi, bvj);
                int mul2 = this.mul(bvj, bvi);
                if ( mul1 != mul2 ) {
                    System.err.println("Multiplication not associative / distributive / commutative");
                    allTestsPassed = false;
                }
                int m1 = Poly2.mul(i, j);
                int m2 = Poly2.mod(m1, this.generator);
                int bm2 = (int)m2;
                if ( bm2 != mul1 ) {
                    System.err.println("Multiplication does not match reference implementation");
                    allTestsPassed = false;
                }
            }
        }

        for ( int i=0; i<SIZE; ++i) {
            int bv = (int)i;
            int mulVal = this.mul(aVal, bv);
            int addVal = this.add(aVal, bv);
            int subVal = this.sub(aVal, bv);
            int divVal = this.div(mulVal, bv);
            int invVal = this.multiplicativeInverse(bv);
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

            int inv1 = this.mul(bv, invVal);
            if ( ( inv1 != 1) && (bv != 0) ) {
                System.err.println("Inverse test failed");
                allTestsPassed = false;
            }
        }

        return allTestsPassed;
        
    }


}
