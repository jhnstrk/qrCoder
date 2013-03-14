
public class GfPoly {
    static QrFiniteField m_gf256 = QrFiniteField.getInstance();

    // Stored in reverse order. i.e. 1 is first.
    byte[] m_coeffs;
    int    m_len;

    class DivResult {
        GfPoly quotient;
        GfPoly remainder;
    }

    GfPoly() {
        m_len = 0;
    }

    GfPoly(int len) {
        m_len = len;
        m_coeffs = new byte[len];
    }

    GfPoly(int len, int maxLen) {
        m_len = len;
        m_coeffs = new byte[maxLen];
    }

    // From reversed (high order first) coeefs.
    GfPoly(byte[] coeffsRev) {
        m_len = coeffsRev.length;
        m_coeffs = new byte[coeffsRev.length];
        for(int i=0;i<m_len;++i) {
            m_coeffs[i] = coeffsRev[m_len - 1 - i];
        }
    }

    int length() {
        return m_len;
    }

    void setCoeff(int i, byte val) {
        m_coeffs[i] = val;
    }

    byte getCoeff(int i) {
        return m_coeffs[i];
    }

    public void stripZeros() {
        while ( m_len > 0 && m_coeffs[m_len-1] == 0) {
            --m_len;
        }
    }

    public GfPoly clonePoly()
    {
        GfPoly ret = new GfPoly();
        ret.m_len = this.m_len;
        ret.m_coeffs = this.m_coeffs.clone();
        return ret;
    }

    public GfPoly add( GfPoly y)
    {
        GfPoly ret;
        if ( this.m_len >= y.m_len ) {
            ret = new GfPoly(this.m_len);
            for (int i=0; i<y.m_len; ++i){
                ret.m_coeffs[i] = m_gf256.add(this.m_coeffs[i], y.m_coeffs[i] );
            }
            for (int i=y.m_len; i<this.m_len; ++i){
                ret.m_coeffs[i] = this.m_coeffs[i];
            }
        } else {
            ret = new GfPoly(y.m_len);
            for (int i=0; i<this.m_len; ++i){
                ret.m_coeffs[i] = m_gf256.add(this.m_coeffs[i], y.m_coeffs[i]);
            }
            for (int i=this.m_len; i<y.m_len; ++i){
                ret.m_coeffs[i] = y.m_coeffs[i];
            }
        }
        return ret;
    }

    public GfPoly mul( GfPoly y)
    {
        if ( (m_len == 0 ) || (y.m_len == 0) ) {
            return new GfPoly();
        }

        GfPoly output = new GfPoly( m_len + y.m_len - 1);
        for (int i=0; i<m_len + y.m_len -1; ++i) {
            output.m_coeffs[i] = (byte)0;
        }
        for (int i=0; i<m_len; ++i) {
            for (int j=0; j<y.m_len; ++j) {
                byte prod = m_gf256.mul(m_coeffs[i], y.m_coeffs[j]);
                int pwr = i + j;
                output.m_coeffs[pwr]= m_gf256.add(output.getCoeff(pwr), prod);
            }
        }
        
        return output;
    }

    //! Get remainder. Highest order is first.
    public DivResult div( GfPoly poly)
    {
        int lenPoly = poly.length();

        if (m_len < lenPoly) {
            DivResult ret = new DivResult();
            ret.quotient = new GfPoly();
            ret.remainder = this;
            return ret;
        }

        GfPoly quotient  = new GfPoly(m_len - lenPoly + 1);

        byte[] work = new byte[lenPoly];
        for( int i=0; i<lenPoly; ++i){
            work[i] = m_coeffs[i + m_len - lenPoly];
        }

        for (int i=m_len - lenPoly; i>=0; --i) {
            work[0] = m_coeffs[i];
            // y1 x2 yy x yy ) x1 x5 xx x4 xx x3 xx x2 xx x xx 1
            byte v = m_gf256.div(work[lenPoly-1], poly.getCoeff(lenPoly-1) );
            quotient.setCoeff(i, v);
            for (int j=lenPoly-1;j>0;--j){
                byte tmpWork = m_gf256.mul(poly.getCoeff(j-1), v);
                tmpWork = m_gf256.sub(work[j-1], tmpWork);
                work[j] = tmpWork;
            }
        }

        GfPoly remainder = new GfPoly(lenPoly - 1);
        for ( int i =0; i<lenPoly-1; ++i) {
            remainder.setCoeff(i, work[i+1]);
        }
        DivResult ret = new DivResult();
        ret.remainder = remainder;
        ret.quotient = quotient;
        return ret;
    }

    byte evaluate( byte value)
    {
        byte sum = 0;
        byte vTmp = 1;
        for (int i=0; i<this.m_len; ++i) {
            byte tmp = m_gf256.mul(this.m_coeffs[i], vTmp);
            sum = m_gf256.add(sum, tmp);
            vTmp = m_gf256.mul(vTmp, value);
        }
        return sum;
    }

    byte[] toArray()
    {
        byte[] ret = new byte[m_len];
        for (int i=0; i<m_len; ++i) {
            ret[m_len-1-i] = m_coeffs[i];
        }
        return ret;
    }

    byte[] toArray(int lenOut)
    {
        byte[] ret = new byte[lenOut];
        for (int i=0; i<m_len; ++i) {
            ret[lenOut-1-i] = m_coeffs[i];
        }
        for (int i=m_len; i<lenOut; ++i) {
            ret[lenOut-1-i] = 0;
        }
        return ret;
    }
    
    void resize( int newLen) {
        if ( newLen > m_len) {
            if ( newLen > this.m_coeffs.length) {
                byte [] newCoeffs = new byte[newLen];
                for ( int i=0; i<m_len; ++i) {
                    newCoeffs[i] = m_coeffs[i];
                }
                m_coeffs = newCoeffs;
            }
        } else {
            for ( int i=newLen; i<m_len; ++i) {
                m_coeffs[i] = 0;
            }
        }
        m_len = newLen;
    }

    void setAllToZero() 
    {
        for ( int i=0; i<m_coeffs.length; ++i) {
            m_coeffs[i] = 0;
        }
    }
    
    // Compare equality, allowing for additional zeros.
    public boolean equals( GfPoly other )
    {
        if ( this.m_len <= other.m_len ) {
            for ( int i=0; i<m_len; ++i) {
                if ( this.m_coeffs[i] != other.m_coeffs[i]){
                    return false;
                }
            }
            for ( int i=m_len; i<other.m_len; ++i )
            {
                if (other.m_coeffs[i] != 0) {
                    return false;
                }
            }
            return true;
        }
        return other.equals(this);
    }

    public void shiftL(int count)
    {
        if (count > 0) {
            this.resize( m_len + count);
            for ( int i=m_len -1; i >= count; --i) {
                this.m_coeffs[i] = this.m_coeffs[i - count];
            }
            for (int i=0; i<count; ++i) {
                this.m_coeffs[i] = 0;
            }
        } else if (count < 0) {
            // count is less than 0
            for (int i=0; i<m_len + count; ++i) {
                this.m_coeffs[i] = this.m_coeffs[i - count];
            }
            this.resize(m_len + count);
        }
    }

    public static boolean test() 
    {
        byte[] inXB = { 16, 8, 4, 2 , 2 };
        byte[] inYB = { 1, 2 , 2 };
//        byte[] inXB = { 16, 8 };
//        byte[] inYB = { 16 };
        GfPoly inX = new GfPoly(inXB);
        GfPoly inY = new GfPoly(inYB);
        DivResult res1 = inX.div( inY );
        GfPoly mulv1 = inY.mul( res1.quotient );
        boolean allTestsPassed = true;
        GfPoly recovered = mulv1.add( res1.remainder );
        if (!recovered.equals(inX)){
            allTestsPassed = false;
            System.err.println("multiply / divide test failed");
        }

        return allTestsPassed;
    }
}
