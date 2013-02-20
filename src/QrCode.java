import java.util.Vector;

public class QrCode {
    final int m_size = 29;

    // char-to-number encoding.
    final String alphaTable5 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:";

    byte m_codeHolder[];

    final int modulus = BinaryHelper.fromBinaryString("100011101");

    class BitHolder {
        public int m_bitPos[];
        public BitHolder() {
            m_bitPos = new int[8];
        }
        BitHolder( int b0, int b1, int b2, int b3, int b4, int b5, int b6, int b7)
        {
            m_bitPos = new int[8];
            m_bitPos[0] = b0; m_bitPos[1] = b1; m_bitPos[2] = b2;
            m_bitPos[3] = b3; m_bitPos[4] = b4; m_bitPos[5] = b5;
            m_bitPos[6] = b6; m_bitPos[7] = b7;
        }
    }


    Vector<BitHolder> m_bitLookupD;
    Vector<BitHolder> m_bitLookupE;

    public QrCode() {
        m_codeHolder = new byte[m_size*m_size];
        m_bitLookupD = new Vector<BitHolder>(); // 1 to 55
        m_bitLookupE = new Vector<BitHolder>(); // 1 to 15
        fillBitLookups();
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

    int indexFromRC( int row, int col)
    {
        return row*m_size + col;
    }

    //TODO: Create bit holder constructors for the common types.
    BitHolder upGroup(int tlRow, int tlCol)
    {
        return new BitHolder (
                indexFromRC(tlRow,tlCol),
                indexFromRC(tlRow,tlCol+1),
                indexFromRC(tlRow+1,tlCol),
                indexFromRC(tlRow+1,tlCol+1),
                indexFromRC(tlRow+2,tlCol),
                indexFromRC(tlRow+2,tlCol+1),
                indexFromRC(tlRow+3,tlCol),
                indexFromRC(tlRow+3,tlCol+1)
                );
    }

    BitHolder downGroup(int tlRow, int tlCol)
    {
        return new BitHolder (
                indexFromRC(tlRow+3,tlCol),
                indexFromRC(tlRow+3,tlCol+1),
                indexFromRC(tlRow+2,tlCol),
                indexFromRC(tlRow+2,tlCol+1),
                indexFromRC(tlRow+1,tlCol),
                indexFromRC(tlRow+1,tlCol+1),
                indexFromRC(tlRow,tlCol),
                indexFromRC(tlRow,tlCol+1)
                );
    }

    BitHolder upToDownGroup(int tlRow, int tlCol)
    {
        return new BitHolder (
                indexFromRC(tlRow+1,tlCol),
                indexFromRC(tlRow+1,tlCol+1),
                indexFromRC(tlRow,tlCol),
                indexFromRC(tlRow,tlCol+1),
                indexFromRC(tlRow,tlCol+2),
                indexFromRC(tlRow,tlCol+3),
                indexFromRC(tlRow+1,tlCol+2),
                indexFromRC(tlRow+1,tlCol+3)
                );
    }

    BitHolder downToUpGroup(int tlRow, int tlCol)
    {
        return new BitHolder (
                indexFromRC(tlRow,tlCol),
                indexFromRC(tlRow,tlCol+1),
                indexFromRC(tlRow+1,tlCol),
                indexFromRC(tlRow+1,tlCol+1),
                indexFromRC(tlRow+1,tlCol+2),
                indexFromRC(tlRow+1,tlCol+3),
                indexFromRC(tlRow,tlCol+2),
                indexFromRC(tlRow,tlCol+3)
                );
    }


    void fillBitLookups()
    {
        // TODO : Fill this.
        this.m_bitLookupD.add(this.upGroup(25, 27));
    }
}
