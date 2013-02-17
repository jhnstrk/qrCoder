
public class Poly2 {

    public static int add( int x , int y )
    {
        return x ^ y; // Addition and subtraction are XOR
    }

    public static int sub( int x , int y )
    {
        return x ^ y; // Addition and subtraction are XOR
    }

    public static int mul( int x , int y)
    {
        int ret = 0;
        for ( int i=0; i<32; ++i) {
            if ( (x & ( 1 << i)) != 0) {
                ret = ret ^ (y << i);
            }
        }
        return ret;
    }

    public static class DivResult {
        public int quotient;
        public int remainder;
        public DivResult( int q, int r) {
            quotient = q;
            remainder = r;
        }
    }
    
    public static DivResult divQr( int x, int y){

        int highBitY = -1;
        for ( int i = 31; i >= 0; --i) {
            if ( (y & (1 << i)) != 0 ) {
                highBitY =i;
                break;
            }
        }
        if ( highBitY == 0 ) {
            System.err.println("Poly2 : Division by 0");
            return new DivResult(0,0);
        }

        int quotient = 0;
        int remainder = x;
        for ( int i = 31; i>=highBitY; --i) {
            if ( (remainder & ( 1<<i)) != 0) {
                // Update quotient
                quotient = quotient | ( 1 << (i - highBitY));
                // Update t;
                remainder = remainder ^ ( y << (i - highBitY));
            }
        }
        return new DivResult(quotient, remainder);
    }

    public static int div( int x, int y){
        DivResult res = divQr(x,y);
        return res.quotient;
    }

    public static int mod( int x, int y){
        DivResult res = divQr(x,y);
        return res.remainder;
    }

    public static boolean test()
    {
        boolean pass = true;
        int a = 15;
        int b = 14;
        if ( mul(15,14) != 90 || (mul(14,15) != 90)) {
            System.out.println(" 14 * 15 failed");
        }
        
        System.out.println(" a + b = " + add(a,b));
        System.out.println(" a - b = " + sub(a,b));
        System.out.println(" a * b = " + mul(a,b));
        DivResult res = divQr(a,b);
        System.out.println(" a / b = " + res.quotient + " remainder " + res.remainder);
        
        int c = add(a,b);
        if ( b != sub(c,a) ) {
            System.err.println(" Failed b == (a+b) - a");
            pass = false;
        }

        int ab = mul(a,b);

        if ( b != div(ab,a) ) {
            System.err.println(" Failed b == (a*b)/a");
            pass = false;
        }

        if ( a != div(ab,b) ) {
            System.err.println(" Failed a == (a*b)/b");
            pass = false;
        }

        if ( 0 != mod(ab,a) ) {
            System.err.println(" Failed 0 == (a*b) mod a");
            pass = false;
        }
        if ( 0 != mod(ab,a) ) {
            System.err.println(" Failed 0 == (a*b) mod b");
            pass = false;
        }

        int testVal = BchCodec15_5.fromBinaryString("1000111110101111"); // 36783
        int testValNum = BchCodec15_5.fromBinaryString("1000001000000000000000"); //2129920

        int remainder = mod(testValNum, testVal);
        int expectRemainder =BchCodec15_5.fromBinaryString("100101000100010"); // 18798
        if ( remainder != expectRemainder) {
            System.err.println(" Failed 1000001000000000000000 / 1000111110101111 ==  ? rem 100101000100010");
            pass = false;
        }
        return pass;
    }
}
