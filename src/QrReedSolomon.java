
public class QrReedSolomon {

    QrFiniteField m_gf256;

    public QrReedSolomon()
    {
        m_gf256 = new QrFiniteField();
    }

    class DivResult {
        byte [] quotient;
        byte [] remainder;
    }

    //! Generate 
    public byte [] encode( byte[] input , byte[] poly)
    {
        byte [] tmp = new byte[input.length + poly.length - 1];
        for ( int i=0; i< input.length; ++i) {
            tmp[i] = input[i];
        }
        for (int i = input.length; i<(input.length + poly.length -1); ++i){
            tmp[i] = 0;
        }
        return this.div(tmp, poly).remainder;
    }

    public byte[] add( byte[] x , byte[] y)
    {
        final int lenX = x.length;
        final int lenY = y.length;
        byte[] ret;
        if ( lenX > lenY ) {
            ret = new byte[lenX];
            final int off = lenX-lenY;
            for (int i=0; i<off; ++i){
                ret[i] = x[i];
            }
            for (int i=off; i<lenX; ++i){
                ret[i] = this.m_gf256.add(x[i], y[i-off]);
            }
        } else {
            ret = new byte[lenX];
            final int off = lenY-lenX;
            for (int i=0; i<off; ++i){
                ret[i] = y[i];
            }
            for (int i=off; i<lenY; ++i){
                ret[i] = this.m_gf256.add(x[i-off], y[i]);
            }
        }
        return ret;
    }

    public byte[] reverse( byte[] x)
    {
        byte[] ret = new byte[x.length];
        for (int i=0; i<x.length; ++i) {
            ret[i] = x[x.length -1 -i];
        }
        return ret;
    }

    public byte[] stripLeadingZeros( byte[] x)
    {
        int num0 = 0;
        while ( num0 < x.length && x[num0] == 0){
            ++num0;
        }
        if (num0 == 0)
            return x;
        byte[] ret = new byte[x.length - num0];
        for ( int i=0; i<x.length-num0; ++i){
            ret[i] = x[i+num0];
        }
        return ret;
    }

    public byte[] mul( byte[] x , byte[] y)
    {
        int lenX = x.length;
        int lenY = y.length;
        byte [] output = new byte[ lenX + lenY - 1];
        for (int i=0; i<lenX + lenY -1; ++i) {
            output[i] = 0;
        }
        for (int i=0; i<lenX; ++i) {
            for (int j=0; j<lenY; ++j) {
                byte prod = this.m_gf256.mul(x[i], y[j]);
                int pwr = i + j;
                output[pwr] = this.m_gf256.add(output[pwr], prod);
            }
        }
        
        return output;
    }

    //! Get remainder. Highest order is first.
    public DivResult div( byte[] input , byte[] poly)
    {
        int lenInput = input.length;
        int lenPoly = poly.length;
        //TODO : Perform the polynomial division.
        if (lenInput < lenPoly) {
            DivResult ret = new DivResult();
            ret.quotient = new byte[0];
            ret.remainder = input;
            return ret;
        }

        byte[] quotient  = new byte[lenInput - lenPoly + 1];

        byte[] work = new byte[lenPoly];
        for( int i=0; i<lenPoly; ++i){
            work[i] = input[i];
        }

        for (int i=0; i<(lenInput - lenPoly + 1); ++i) {
            work[lenPoly-1] = input[i+lenPoly-1];
            // y1 x2 yy x yy ) x1 x5 xx x4 xx x3 xx x2 xx x xx xx
            byte v = m_gf256.div(work[0], poly[0]);
            quotient[i] = v;
            for (int j=0;j<lenPoly-1;++j){
                byte tmpWork = m_gf256.mul(poly[j+1], v);
                tmpWork = m_gf256.sub(work[j+1], tmpWork);
                work[j] = tmpWork;
            }
        }

        byte [] remainder = new byte[lenPoly - 1];
        for ( int i =0; i<lenPoly-1; ++i) {
            remainder[i] = work[i];
        }
        DivResult ret = new DivResult();
        ret.remainder = remainder;
        ret.quotient = quotient;
        return ret;
    }

    public static boolean test() 
    {
        boolean allTestsPassed = testMulDiv();
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

        for (int i=0; i<polyV.length; ++i) {
            polyV[i] = obj.m_gf256.getAlphaPower(polyV[i]);
        }

        {
            DivResult dv = obj.div(values, polyV);
            byte[] tmp = obj.mul(dv.quotient, polyV);
            tmp = obj.add(tmp, dv.remainder);
            allTestsPassed &= compareByteArrays(tmp, values);
        }
        byte [] enc = obj.encode( values, polyV );
        allTestsPassed &= compareByteArrays(enc, refEc);
        for (int i=0; i<enc.length; ++i) {
            System.out.println(BinaryHelper.asBinaryString(enc[i]));
        }
        return allTestsPassed;
    }
    
    public static boolean testMulDiv() 
    {
        QrReedSolomon obj = new QrReedSolomon();
        byte[] inX = { 16, 8, 4, 2 , 2 };
        byte[] inY = { 1, 2 , 2 };
        DivResult res1 = obj.div( inX, inY );
        byte [] mulv1 = obj.mul(inY, res1.quotient);
        boolean allTestsPassed = true;
        byte [] recovered =obj.add(mulv1, res1.remainder);
        if (!compareByteArrays(recovered, inX)){
            allTestsPassed = false;
            System.err.println("multiply / divide test failed");
        }

        // Generate a ecc polynomial.
        int numEcCodeWords = 10;
        byte[] firstDeg = { 1 , 1 };
        byte[] prod = {1, 1};
        for (int i=0; i<numEcCodeWords-1; ++i) {
            firstDeg[1] = obj.m_gf256.mul(firstDeg[1], (byte)2);
            prod = obj.mul(prod, firstDeg);
        }
        byte [] polyV = PolyHolder.getCoefficientsBytes(numEcCodeWords);
        for (int i=0; i<polyV.length; ++i) {
            polyV[i] = obj.m_gf256.getAlphaPower(polyV[i]);
        }

        if ( !compareByteArrays(polyV, prod) ) {
            System.err.println("Failed to generate polynomial by multiplication");
            allTestsPassed = false;
        }
        return allTestsPassed;
    }
    
    public static boolean compareByteArrays( byte[] a, byte[] b) {
        if ( a==null && b==null)
            return true;
        if (a==null || b==null)
            return false;
        if (a.length != b.length)
            return false;
        for (int i=0; i< a.length; ++i) {
            if (a[i] != b[i])
                return false;
        }
        return true;
    }
}
