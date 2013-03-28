
public class GfPolyInt {
    final FiniteFieldInt m_gf2n;

    // Stored in reverse order. i.e. 1 is first.
    int[] m_coeffs;
    int    m_len;

    class DivResult {
        GfPolyInt quotient;
        GfPolyInt remainder;
    }

    GfPolyInt(FiniteFieldInt gf) {
        m_gf2n = gf;
        m_len = 0;
    }

    GfPolyInt(int len, FiniteFieldInt gf) {
        m_len = len;
        m_gf2n = gf;
        m_coeffs = new int[len];
    }

    // From reversed (high order first) coefficients.
    GfPolyInt(int[] coeffsRev, FiniteFieldInt gf) {
        m_len = coeffsRev.length;
        m_gf2n = gf;
        m_coeffs = new int[coeffsRev.length];
        for(int i=0;i<m_len;++i) {
            m_coeffs[i] = coeffsRev[m_len - 1 - i];
        }
    }

    int length() {
        return m_len;
    }

    void setCoeff(int i, int val) {
        m_coeffs[i] = val;
    }

    int getCoeff(int i) {
        return m_coeffs[i];
    }

    public void stripZeros() {
        while ( m_len > 0 && m_coeffs[m_len-1] == 0) {
            --m_len;
        }
    }

    public GfPolyInt clonePoly()
    {
        GfPolyInt ret = new GfPolyInt(m_gf2n);
        ret.m_len = this.m_len;
        ret.m_coeffs = this.m_coeffs.clone();
        return ret;
    }

    public GfPolyInt add( GfPolyInt y)
    {
        GfPolyInt ret;
        if ( this.m_len >= y.m_len ) {
            ret = new GfPolyInt(m_len, m_gf2n);
            for (int i=0; i<y.m_len; ++i){
                ret.m_coeffs[i] = m_gf2n.add(this.m_coeffs[i], y.m_coeffs[i] );
            }
            for (int i=y.m_len; i<this.m_len; ++i){
                ret.m_coeffs[i] = this.m_coeffs[i];
            }
        } else {
            ret = new GfPolyInt(y.m_len, m_gf2n);
            for (int i=0; i<this.m_len; ++i){
                ret.m_coeffs[i] = m_gf2n.add(this.m_coeffs[i], y.m_coeffs[i]);
            }
            for (int i=this.m_len; i<y.m_len; ++i){
                ret.m_coeffs[i] = y.m_coeffs[i];
            }
        }
        return ret;
    }

    public GfPolyInt sub( GfPolyInt y)
    {
        return this.add(y);
    }

    public GfPolyInt mul( GfPolyInt y)
    {
        if ( (m_len == 0 ) || (y.m_len == 0) ) {
            return new GfPolyInt(m_gf2n);
        }

        GfPolyInt output = new GfPolyInt( m_len + y.m_len - 1, m_gf2n);
        for (int i=0; i<m_len + y.m_len -1; ++i) {
            output.m_coeffs[i] = (int)0;
        }
        for (int i=0; i<m_len; ++i) {
            for (int j=0; j<y.m_len; ++j) {
                int prod = m_gf2n.mul(m_coeffs[i], y.m_coeffs[j]);
                int pwr = i + j;
                output.m_coeffs[pwr]= m_gf2n.add(output.getCoeff(pwr), prod);
            }
        }
        
        return output;
    }

    //! Get remainder. Highest order is first.
    public DivResult div( GfPolyInt poly)
    {
        int lenPoly = poly.length();

        if (m_len < lenPoly) {
            DivResult ret = new DivResult();
            ret.quotient = new GfPolyInt(m_gf2n);
            ret.remainder = this;
            return ret;
        }

        GfPolyInt quotient  = new GfPolyInt(m_len - lenPoly + 1, m_gf2n);

        int[] work = new int[lenPoly];
        for( int i=0; i<lenPoly; ++i){
            work[i] = m_coeffs[i + m_len - lenPoly];
        }

        for (int i=m_len - lenPoly; i>=0; --i) {
            work[0] = m_coeffs[i];
            // y1 x2 yy x yy ) x1 x5 xx x4 xx x3 xx x2 xx x xx 1
            int v = m_gf2n.div(work[lenPoly-1], poly.getCoeff(lenPoly-1) );
            quotient.setCoeff(i, v);
            for (int j=lenPoly-1;j>0;--j){
                int tmpWork = m_gf2n.mul(poly.getCoeff(j-1), v);
                tmpWork = m_gf2n.sub(work[j-1], tmpWork);
                work[j] = tmpWork;
            }
        }

        GfPolyInt remainder = new GfPolyInt(lenPoly - 1, m_gf2n);
        for ( int i =0; i<lenPoly-1; ++i) {
            remainder.setCoeff(i, work[i+1]);
        }
        remainder.stripZeros();
        quotient.stripZeros();
        DivResult ret = new DivResult();
        ret.remainder = remainder;
        ret.quotient = quotient;
        return ret;
    }

    int evaluate( int value)
    {
        int sum = 0;
        int vTmp = 1;
        for (int i=0; i<this.m_len; ++i) {
            int tmp = m_gf2n.mul(this.m_coeffs[i], vTmp);
            sum = m_gf2n.add(sum, tmp);
            vTmp = m_gf2n.mul(vTmp, value);
        }
        return sum;
    }

    void mul( int x)
    {
        for (int i=0; i<this.m_len; ++i) {
            m_coeffs[i] = m_gf2n.mul(m_coeffs[i], x);
        }
    }

    int[] toArray()
    {
        int[] ret = new int[m_len];
        for (int i=0; i<m_len; ++i) {
            ret[m_len-1-i] = m_coeffs[i];
        }
        return ret;
    }

    int[] toArray(int lenOut)
    {
        int[] ret = new int[lenOut];
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
                int [] newCoeffs = new int[newLen];
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
    public boolean equals( GfPolyInt other )
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

    public void shiftR(int count) {
        shiftL(-count);
    }

    GfPolyInt formalDerivative()
    {
        // Return the derivative of the poly.
        if (this.m_len <= 1){
            return new GfPolyInt(m_gf2n);
        }

        final int N = this.m_len - 1;
        final GfPolyInt ret = new GfPolyInt( N , m_gf2n);
        for (int i=0; i<N; ++i){
            int om = m_gf2n.ordinaryMul(i+1,    this.getCoeff(i+1));
            ret.setCoeff(i, om );
        }
        return ret;
    }

    public String toString()
    {
        String ret = new String();
        for (int i=m_len-1; i>=0; --i) {
            ret += this.m_coeffs[i];
            if ( i > 0)
                ret += " ";
        }
        return ret;
    }

    public static boolean test() 
    {
        FiniteField gfbyte = QrFiniteField.getInstance256();
        FiniteFieldInt gf = new  FiniteFieldInt(gfbyte.generator);

        int[] inXB = { 16, 8, 4, 2 , 2 };
        int[] inYB = { 1, 2 , 2 };
//        int[] inXB = { 16, 8 };
//        int[] inYB = { 16 };
        GfPolyInt inX = new GfPolyInt(inXB, gf);
        GfPolyInt inY = new GfPolyInt(inYB, gf);
        DivResult res1 = inX.div( inY );
        GfPolyInt mulv1 = inY.mul( res1.quotient );
        boolean allTestsPassed = true;
        GfPolyInt recovered = mulv1.add( res1.remainder );
        if (!recovered.equals(inX)){
            allTestsPassed = false;
            System.err.println("multiply / divide test failed");
        }

        int [] p1 = { 1,1 };
        int [] p2 = { 1,4 };
        int [] p3 = { 1,16 };
        GfPolyInt pp1 = new GfPolyInt(p1, gf);
        GfPolyInt pp2 = new GfPolyInt(p2, gf);
        GfPolyInt pp3 = new GfPolyInt(p3, gf);
        
        GfPolyInt prod123 = pp1.mul(pp2).mul(pp3);
        
        if (prod123.evaluate(p1[1]) != 0) {
            allTestsPassed = false;
        }
        if (prod123.evaluate(p2[1]) != 0) {
            allTestsPassed = false;
        }
        if (prod123.evaluate(p3[1]) != 0) {
            allTestsPassed = false;
        }
        if (! (prod123.div(pp2.mul(pp3)).quotient.equals(pp1)) ) {
            allTestsPassed = false;
        }
        return allTestsPassed;
    }

}
