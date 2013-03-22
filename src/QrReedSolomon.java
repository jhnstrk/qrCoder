import java.util.ArrayList;
//import java.util.Collections;


public class QrReedSolomon {


    public QrReedSolomon()
    {
    }


    //! Generate 
    public byte [] encode( byte[] input , GfPoly poly)
    {
        GfPoly iPoly= new GfPoly(input);
        iPoly.shiftL(poly.length() - 1);

        GfPoly res = iPoly.div(poly).remainder;

        return res.toArray(poly.length() - 1);
    }

    public byte[] reverse( byte[] x)
    {
        final byte[] ret = new byte[x.length];
        for (int i=0; i<x.length; ++i) {
            ret[i] = x[x.length -1 -i];
        }
        return ret;
    }


    byte[] decode( byte[] input, GfPoly poly )
    {
        // Implementation of the Berlekamp-Massey alg.
        // Calculate the syndromes, checking if they are all zero.
        FiniteField gf256 = QrFiniteField.getInstance256();
        
        // Convert input to polynomial form
        GfPoly iPoly = new GfPoly(input);
        boolean allZeroSyndrome = true;
        final int N = poly.length() - 1;
        // s is the syndrome vector. s[0] is 0-order.
        GfPoly s = new GfPoly( N );
        // The zero-order term is zero.
        for (int i=0; i<N; ++i) {
            s.setCoeff(i, iPoly.evaluate( gf256.pow(i) ) );
            if (s.getCoeff(i) != 0) {
                allZeroSyndrome = false;
            }
        }

        if (allZeroSyndrome) {
            iPoly.shiftR(N);     // Drop terms of lowest order.
            return iPoly.toArray(input.length - N);
        }

        GfPoly C = new GfPoly(1,poly.length());
        C.setCoeff(0, (byte)(1));
        GfPoly B = new GfPoly(1,poly.length());
        B.setCoeff(0, (byte)(1));

        int L = 0;
        byte b = 1;
        int m = 1;

        for (int n=0; n<N; ++n) {
            byte d = s.getCoeff(n);
            for (int i=1; i<=L; ++i) {
                byte tmp = gf256.mul(C.getCoeff(i), s.getCoeff(n-i));
                d = gf256.add(d, tmp);
            }
            if (d == 0) {
                ++m;
                continue;
            } else if (2*L <= N ){
                GfPoly Tmp = C.clonePoly();

                byte d_over_b = gf256.div(d, b);
                B.shiftL(m);  // Times x^m
                B.mul( d_over_b);
                C = C.sub( B );

                B = Tmp;

                L = n + 1 - L;
                b = d;
                m = 1;
            } else {
                GfPoly tmp = B.clonePoly();
                byte d_over_b = gf256.div(d, b);
                tmp.shiftL(m);  // Times x^m
                tmp.mul( d_over_b);
                C = C.sub( tmp );
                ++m;
            }
        }

        // C now contains the error location polynomial.
        // Evaluate c at each value of alpha to find the zeros.
        ArrayList<Byte> zeros = new ArrayList<Byte>();
        for (int i=0; i<256; ++i) {
            byte v = C.evaluate((byte)i);
            if ( v == 0) {
                zeros.add( (Byte)(byte)i );
            }
        }

        if (zeros.size() != L) {
            // Decoding error : number of error positions is not equal to number of errors.
            // Probably an un-correctable code.
            return null;
        }

        GfPoly omega = C.mul(s);
        // mod x^(2t)
        // omega.resize(N);  // TODO : check, maybe 2N, or N+1??

        GfPoly xPowN = new GfPoly(N+1);
        xPowN.setCoeff(N, (byte)1);
        omega = omega.div(xPowN).remainder;

        GfPoly lambdaDash = C.formalDerivative();

        GfPoly ePoly = new GfPoly(input.length);
        // Apply the Forney alg to find the error values.
        for ( byte x : zeros ) {
            byte x_recip = gf256.multiplicativeInverse(x);
            int pwr = gf256.log(x_recip);

            final byte omega_x = omega.evaluate(x);
            final byte lambaDash_x = lambdaDash.evaluate(x);
            byte tmp = gf256.mul(x_recip, omega_x);
            // Omit the minus here as add == sub.
            final byte val = gf256.div(tmp, lambaDash_x);

            if ( pwr < ePoly.length() ) {
                ePoly.setCoeff(pwr, val);
            }
        }

        GfPoly retPoly = iPoly.sub(ePoly);
        retPoly.shiftR(N);
        return retPoly.toArray(input.length - N);
    }

    public static boolean test() 
    {
        boolean allTestsPassed = true;
        allTestsPassed &= PolyHolder.test();
        QrReedSolomon obj = new QrReedSolomon();
        String[] inStreamStr = {
                "00010000", "00100000", "00001100", "01010110",
                "01100001", "10000000", "11101100", "00010001",
                "11101100", "00010001", "11101100", "00010001",
                "11101100", "00010001", "11101100", "00010001" };
        String[] refCorr = { 
                "10100101", "00100100", "11010100", "11000001",
                "11101101", "00110110", "11000111", "10000111",
                "00101100", "01010101" };
        // This is for a version 1-M symbol.
        // Get the coeffs
        byte [] polyV = PolyHolder.getCoefficientsBytes(10);
        byte [] values = BinaryHelper.byteFromBinaryStringArray(inStreamStr);
        byte [] refEc = BinaryHelper.byteFromBinaryStringArray(refCorr);

        FiniteField gf256 = QrFiniteField.getInstance256();

        for (int i=0; i<polyV.length; ++i) {
            polyV[i] = gf256.pow(polyV[i]);
        }

        GfPoly poly = new GfPoly(polyV);
        {
            GfPoly valuesP = new GfPoly(values);
            GfPoly.DivResult dv = valuesP.div(poly);
            GfPoly tmp = dv.quotient.mul(poly);
            tmp = tmp.add( dv.remainder);
            allTestsPassed &= tmp.equals(valuesP);
        }
        byte [] enc = obj.encode( values, poly );
        if (!compareByteArrays(enc, refEc)) {
            System.err.println("Encoding failed");
            allTestsPassed = false;
        }

        byte [] fullEnc = catByteArrays( values, refEc);

        byte [] decVal = obj.decode(fullEnc, poly);
        if (!compareByteArrays(values, decVal)) {
            System.err.println("Decoding clean (no bits flipped) failed");
            allTestsPassed = false;
        }

        // Test corrupted bit stream.
        byte [] fullEncWithErrs = fullEnc.clone();
        // Flip some bits.
        int flipNum = 10;
        fullEncWithErrs[flipNum] = (byte)(fullEncWithErrs[flipNum] ^ 0x03);

        flipNum = 15;
        fullEncWithErrs[flipNum] = (byte)(fullEncWithErrs[flipNum] ^ 0x0c);

        flipNum = 23;
        fullEncWithErrs[flipNum] = (byte)(fullEncWithErrs[flipNum] ^ 0x0c);

        flipNum = 3;
        fullEncWithErrs[flipNum] = (byte)(fullEncWithErrs[flipNum] ^ 0x0c);

        decVal = obj.decode(fullEncWithErrs, poly);
        if (!compareByteArrays(values, decVal)) {
            System.err.println("Decoding corrupted stream (1) failed");
            allTestsPassed = false;
        }

        // Flip some bits.
        for ( flipNum = 0; flipNum < fullEnc.length; ++flipNum)
        {
            fullEncWithErrs = fullEnc.clone();
            fullEncWithErrs[flipNum] = (byte)(fullEncWithErrs[flipNum] ^ 0xba);
            decVal = obj.decode(fullEncWithErrs, poly);
            if (!compareByteArrays(values, decVal)) {
                System.err.println("Decoding corrupted stream (1) failed");
                allTestsPassed = false;
            }
        }

        if (allTestsPassed) {
            System.out.println("QrReedSolomon test passed");
        }
        return allTestsPassed;
    }


    // a goes into lower array values.
    public static byte[] catByteArrays( byte[] a, byte[] b) 
    {
        byte [] cat = new byte[ a.length + b.length];
        for ( int i=0; i<a.length; ++i){
            cat[i] = a[i];
        }
        for ( int i=0; i<b.length; ++i){
            cat[i+a.length] = b[i];
        }
        return cat;
    }

    public static boolean compareByteArrays( byte[] a, byte[] b) {
        if ( a==null && b==null)
            return true;
        if (a==null || b==null)
            return false;
        return java.util.Arrays.equals(a, b);
    }
}
