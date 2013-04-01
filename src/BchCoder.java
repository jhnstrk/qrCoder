import java.util.ArrayList;


public class BchCoder {
    final int codeWordLen = 23;
    final int dataLen = 11;
    final int errorCorrLen = codeWordLen - dataLen;
    final int dataMask = ((1 << dataLen) - 1 );
    final int codeWordEnd = (1 << codeWordLen);
    final int genPoly = ( 1 << 12 ) | ( 1 << 11 ) | ( 1 << 10 ) 
            | ( 1 << 9 ) | ( 1 << 8 ) | ( 1 << 5 ) | ( 1 << 2 ) | 1;

    // genPoly = 3 * 2787 ( 0000000000011 * 0101011100011 )
    final FiniteFieldInt m_gf;

    public BchCoder() {
        //final int generator32  = 0x25;   // x^5 + x^2 + 1. or 100101
        //m_gf = new FiniteFieldInt( generator32 );
        final int generator2pow11  = (1<<11) | (1<<2) | 1;
        //final int generator2pow12  = (1<<12) | (1<<7) | (1<<6) | (1<<5) | (1<<3) | (1<<1) | 1 ;
        m_gf = new FiniteFieldInt( generator2pow11 );
    }
    
    public int encode( int codeVal )
    {
        // Need dataLen + codeWordLen bits for this.
        int maskedCode = codeVal & dataMask;
        if (maskedCode != (int)codeVal) {
            System.err.println(this.getClass().getName() + ": Warning : input value out of range");
        }
        int codedVal = maskedCode << errorCorrLen;
        codedVal = Poly2.mod(codedVal , genPoly );  // Modulo the generator polynomial.
        return (codedVal | (maskedCode << errorCorrLen));
    }

    public int decode( int codeWord )
    {
        //TODO: Implement function
        // Implementation of the Berlekamp-Massey alg.
        // Calculate the syndromes, checking if they are all zero.
        FiniteFieldInt gf32 = m_gf;

        // non-primitive element that is the construction of this code. maybe.
        // int [] possBetaPows = {0, 89,  178, 267, 356, 534, 712, 801, 1068,    1157,    1424,    1602 };
        // int [] minPolys = { 3, 2787, 2787, 2787, 2787 };
        // 3     :                00011
        // 2787  : 00000000101011100011
        final int beta = gf32.pow(89);

        boolean allZeroSyndrome = true;
        final int N = 5;
        // s is the syndrome vector. s[0] is 0-order.
        GfPolyInt s = new GfPolyInt( N, gf32 );
        // The zero-order term is zero.
        for (int i=0; i<N; ++i) {
            int powVal = gf32.pow( beta, i );
            int s_i = this.evaluate(codeWord, powVal, m_gf );
            s.setCoeff(i, s_i);
            if (s.getCoeff(i) != 0) {
                allZeroSyndrome = false;
            }
        }

        if (allZeroSyndrome) {
            int ret = codeWord >> this.errorCorrLen;     // Drop terms of lowest order.
            return ret;
        }
 
        GfPolyInt C = new GfPolyInt(1, gf32);
        C.setCoeff(0, (1));
        GfPolyInt B = new GfPolyInt(1, gf32);
        B.setCoeff(0, (1));

        int L = 0;
        int b = 1;
        int m = 1;

        for (int n=0; n<N; ++n) {
            int d = s.getCoeff(n);
            for (int i=1; i<=L; ++i) {
                int tmp = gf32.mul(C.getCoeff(i), s.getCoeff(n-i));
                d = gf32.add(d, tmp);
            }

            if (d == 0) {
                ++m;
                continue;
            } else if (2*L <= n ){
                GfPolyInt Tmp = C.clonePoly();

                int d_over_b = gf32.div(d, b);
                B.shiftL(m);  // Times x^m
                B.mul( d_over_b);
                C = C.sub( B );

                B = Tmp;

                L = n + 1 - L;
                b = d;
                m = 1;
            } else {
                GfPolyInt tmp = B.clonePoly();
                int d_over_b = gf32.div(d, b);
                tmp.shiftL(m);  // Times x^m
                tmp.mul( d_over_b);
                C = C.sub( tmp );
                ++m;
            }
        }

        // C now contains the error location polynomial.
        // Evaluate c at each value of alpha to find the zeros.
        ArrayList<Integer> zeroBetaPwrs = new ArrayList<Integer>();
        for (int i=1; i<24; ++i) {
            int powVal = gf32.pow( beta, i );
            int v = C.evaluate(powVal);
            if ( v == 0) {
                zeroBetaPwrs.add( (Integer)i );
            }
        }

        if (zeroBetaPwrs.size() != L) {
            // Decoding error : number of error positions is not equal to number of errors.
            // Probably an un-correctable code.
            return -1;
        }

        int ePoly = 0;

        for ( int iBpwr : zeroBetaPwrs ) {
            // int x_recip = gf32.pow(beta, -iBpwr);
            // int pwrAlp = gf32.log(x_recip) ; //  / 89;
            // Because 2047 = 23 *89 we can do
            int pwr = 23 - iBpwr;

            if ( pwr < 24 ) {
                ePoly |= ( 1 << pwr );
            }
        }

        int ret = (codeWord ^ ePoly);
        ret >>= this.errorCorrLen;
        return ret;
    }

    public int evaluate(int poly, int value, FiniteFieldInt gf)
    {
        int ret = 0;
        int polyS = poly & (0x7FFFFFFF);
        int vPower = 1;
        while ( polyS != 0 ) {
            int coeff = polyS & 1;
            if ( coeff != 0 ) {
                ret = gf.add( ret, vPower );
            }
            vPower = gf.mul( vPower, value );
            polyS >>= 1;
        }
        return ret;
    }

    public static boolean test()
    {
        boolean allTestsPassed = true;

        BchCoder obj = new BchCoder();
        
        GfPolyInt x_b = new GfPolyInt(2, obj.m_gf);
        x_b.setCoeff(0, 1);
        x_b.setCoeff(1, 1);
        GfPolyInt prod = x_b.clonePoly();
        int beta = obj.m_gf.pow(89);
        int betaP = beta;
        for (int i=1; i<5; ++i){
            x_b.setCoeff(0, betaP);
            prod = prod.mul(x_b);
            betaP = obj.m_gf.mul(betaP, beta);
        }
        System.out.println( prod.toString() );

        final int expectedCodeWord = BinaryHelper.fromBinaryString("000111110010010100");
        final int vIn = 7;

        int testCoded = obj.encode(vIn);
        if ( testCoded != expectedCodeWord) {
            allTestsPassed = false;
            System.err.println(obj.getClass().getName() + " encoding 7 failed");
        }
        testCoded = obj.encode(31);
        if ( testCoded != BinaryHelper.fromBinaryString("011111001001010000") ) {
            allTestsPassed = false;
            System.err.println(obj.getClass().getName() + " encoding 31 failed");
        }

        for (int i=0; i<6; ++i) {
            int vTest = 1 << i;
            int vTestCodeword = obj.encode( vTest);
            System.out.println( BinaryHelper.asBinaryString(vTestCodeword, 24) );
        }
        
        obj.factorizePoly2(obj.genPoly);
        if (Poly2.mul(3, 2787) != obj.genPoly){
            System.err.println("Factors of generator not right");
        }

        int testDecoded = obj.decode(expectedCodeWord);
        if (testDecoded != vIn) {
            allTestsPassed = false;
            System.err.println(obj.getClass().getName() + " decoding failed");
        }

        int corrupted;

        corrupted = expectedCodeWord ^ (1 << 17);
        testDecoded = obj.decode(corrupted);
        if (testDecoded != vIn) {
            allTestsPassed = false;
            System.err.println(obj.getClass().getName() + ": decoding 1 error failed");
        }

        for (int i=0; i<obj.codeWordLen; ++i) {
            corrupted = expectedCodeWord ^ (1<<i);
            testDecoded = obj.decode(corrupted);
            if (testDecoded != vIn) {
                allTestsPassed = false;
                System.err.println(obj.getClass().getName() + ": decoding 1 error at " + i + " failed");
            }
        }

        corrupted = expectedCodeWord ^ ( (1<<12) | (1<<10) | (1<<0) );
        testDecoded = obj.decode(corrupted);
        if (testDecoded != vIn) {
            allTestsPassed = false;
            System.err.println(obj.getClass().getName() + ": decoding 3 errors failed");
        }

        if ( allTestsPassed ){
            System.out.println(obj.getClass().getName() + " test passed");
        }
        return allTestsPassed;
    }

    void factorizePoly2 ( int poly)
    {
        int fac = poly;
        for (int i=2; i<=fac; ++i) {
            Poly2.DivResult testV = Poly2.divQr( fac, i );  // Modulo the generator polynomial.
            if ( testV.remainder == 0 )  {
                System.out.println("Generator has factor " + BinaryHelper.asBinaryString(i, 13) + " = " + i);
                fac = testV.quotient;
                i = 2;
            }
        }

        int hb = BinaryHelper.highBitPos(poly);
        GfPolyInt fg = new GfPolyInt(hb + 1, m_gf);
        for (int i=0; i<=hb; ++i){
            fg.setCoeff(i, (poly >> i) & 1);
        }

        GfPolyInt denom = new GfPolyInt(2, m_gf);
        denom.setCoeff(1, 1);
        for (int i=0; i<m_gf.bitMask; ++i) {
            denom.setCoeff(0, m_gf.pow(i));
            GfPolyInt.DivResult testV = fg.div( denom );
            if ( testV.remainder.length() == 0 )  {
                System.out.println("Generator has factor " + denom.toString() + " (a^" + i + ")");
                fg = testV.quotient;
                --i;
            }
        }
        
        // Minimal polynomials
        //FiniteFieldInt gf = new FiniteFieldInt(Integer.parseInt("75",8));
        //FiniteFieldInt gf = new FiniteFieldInt(Integer.parseInt("100101",2));
        FiniteFieldInt gf = m_gf;
        
        int[] minPolys = new int[5];
        int prod = 1;
        final int c = 0; // First power
        final int primPwr = 89;
        for (int j=0; j<minPolys.length; ++j) {
            int betaPw = gf.pow(j*primPwr + c);
            for (int i=1; i<(gf.highBit << 1);++i) {
                if ( this.evaluate(i, betaPw, gf) == 0 ) {
                    System.out.println("Minimal poly " + j + " = " + i + "  : " + BinaryHelper.asBinaryString(i, 20));
                    minPolys[j] = i;
                    boolean isAlreadyThere = false;
                    for (int k=0;k<j;++k) {
                        if (minPolys[k] == i){
                            isAlreadyThere = true;
                            break;
                        }
                    }
                    if (!isAlreadyThere) {
                        //System.out.println("Accumulating poly");
                        prod = Poly2.mul(prod, i);
                    }
                    break;
                }
            }
        }
        System.out.println("Product of minimal polys " + prod + " = " + BinaryHelper.asBinaryString(prod, 31));

        for (int i=0; i<minPolys.length; ++i) {
            System.out.println("gen(a^" + (i+c) + ") = " + this.evaluate(prod, gf.pow(i*primPwr+c), gf) );
        }
    }
    
    void printZeros()
    {
        int [] generators = new int[23];
        generators[0] = 0x1;
        generators[1] = 0x2;
        generators[2] = 0x7;
        generators[3] = Integer.parseInt("1011",2);
        generators[4] = Integer.parseInt("23",8);
        generators[5] = Integer.parseInt("75",8);
        generators[6] = Integer.parseInt("147",8);
        generators[7] = Integer.parseInt("325",8);
        generators[8] = Integer.parseInt("453",8);
        generators[9] = Integer.parseInt("1461",8);
        generators[10] = Integer.parseInt("3771",8);
        generators[11] = Integer.parseInt("4005",8);
        generators[12] = Integer.parseInt("15341",8);
        generators[13] = Integer.parseInt("20033",8);

        for (int i=1; i<14; ++i) {
            System.out.println("Testing " + i);
            FiniteFieldInt ff = new FiniteFieldInt(generators[i]);
            if (!ff.testCommon()){
                System.err.println("Test failed " + i);
            }
            for ( int j=0; j<ff.highBit-1; ++j) {
                int v = this.evaluate(this.genPoly, ff.pow(j), ff);
                if ( v == 0 ) {
                    System.out.println("Zero at " + j);
                }
            }
        }
        //final int generator32  = 0x25;   // x^5 + x^2 + 1. or 100101
        //m_gf = new FiniteFieldInt( generator32 );
        //final int generator2pow11  = (1<<11) | (1<<2) | 1;
        //final int generator2pow12  = (1<<12) | (1<<7) | (1<<6) | (1<<5) | (1<<3) | (1<<1) | 1 ;

    }
}
