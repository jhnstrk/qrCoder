import java.util.Vector;

public class QrCode {
    final int m_size = 29;

    byte m_codeHolder[];

    class BitHolder {
        public int m_bitPos[];
        public BitHolder() {
            m_bitPos = new int[8];
        }
        BitHolder( int b0, int b1, int b2, int b3, int b4, int b5, int b6, int b7)
        {
            m_bitPos = new int[8];
            m_bitPos[0] = b0;
            //TODO:   REpeat for others.
        }
    }

    //TODO: Create bit holder constructors for the common types.
    
    Vector<BitHolder> m_bitLookupD;
    Vector<BitHolder> m_bitLookupE;

    public QrCode() {
        m_codeHolder = new byte[m_size*m_size];
    }

    byte getBit( int row, int col){
        if ( (row >= m_size ) || (row < 0) || 
             (col >= m_size ) || (col < 0) ) {

            System.err.printf("Out of bounds read at %i, %j", row, col);
            return 0;
        }
        return m_codeHolder[row*m_size + col];
    }

    void setBit( int row, int col, byte value){
        if ( (row >= m_size ) || (row < 0) || 
             (col >= m_size ) || (col < 0) ) {

            System.err.printf("Out of bounds read at %i, %j", row, col);
            return;
        }
        m_codeHolder[row*m_size + col] = value;
    }

    void buildBitLookups()
    {
    }

    }
}
