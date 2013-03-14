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
        byte[] ret = new byte[x.length];
        for (int i=0; i<x.length; ++i) {
            ret[i] = x[x.length -1 -i];
        }
        return ret;
    }


    byte[] decode( byte[] input, GfPoly poly )
    {
        // Implementation of the Berlekamp-Massey alg.
        // Calculate the syndromes, checking if they are all zero.
        QrFiniteField gf256 = QrFiniteField.getInstance();
        
        // Convert input to polynomial form
        GfPoly iPoly = new GfPoly(input);
        boolean allZeroSyndrome = true;
        int N = poly.length() - 1;
        byte[]s = new byte[ N ];
        for (int i=0; i<N; ++i) {
            s[i] = iPoly.evaluate( gf256.getAlphaPower(i) );
            if (s[i] != 0) {
                allZeroSyndrome = false;
            }
        }

        if (allZeroSyndrome) {
            iPoly.shiftR(N);     // Drop terms of lowest order.
            return iPoly.toArray(input.length - N);  // Keep only high order.
        }

        GfPoly C = new GfPoly(1,poly.length());
        C.setCoeff(0, (byte)(1));
        GfPoly B = new GfPoly(1,poly.length());
        B.setCoeff(0, (byte)(1));

        int L = 0;
        byte b = 1;
        int m = 1;

        for (int n=0; n<N; ++n) {
            byte d = s[n];
            for (int j=0; j<n; ++j) {
                byte tmp = gf256.mul(C.getCoeff(j), s[n-j]);
                d = gf256.add(d, tmp);
            }
            if (d == 0) {
                ++m;
                continue;
            } else {
                byte d_over_b = gf256.div(d, b);
                int endC = B.length() + m;
                C.resize(endC);

                boolean reset = (2*L <= N); 
                if  (reset) {
                    B = C.clonePoly();
                }

                for (int i=endC-1; i>=m ; --i) {
                    byte tmp = gf256.mul(B.getCoeff(i-m), d_over_b);
                    tmp = gf256.sub(C.getCoeff(i), tmp);
                    C.setCoeff(i, tmp);
                }

                if  (reset) {
                    L = n + 1 - L;
                    b = d;
                    m = 1;
                } else {
                    ++m;
                }
            }
        }
        
        // C now contains the error location polynomial.
        // TODO : Evaluate c at each value of alpha to find the zeros.
        ArrayList<Byte> zeros = new ArrayList<Byte>();
        for (int i=0; i<256; ++i) {
            byte v = C.evaluate((byte)i);
            if ( v == 0) {
                zeros.add( v );
            }
        }

        // Apply the Forney alg to find the error values.
        return new byte[0];
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

        QrFiniteField gf256 = QrFiniteField.getInstance();

        for (int i=0; i<polyV.length; ++i) {
            polyV[i] = gf256.getAlphaPower(polyV[i]);
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

        for (int i=0; i<enc.length; ++i) {
            //System.out.println(BinaryHelper.asBinaryString(enc[i]));
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
