

public class Main {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        
        Poly2.test();
        BchCodec15_5.test();

        BchCodec15_5 coder = new BchCodec15_5();

        byte testVal = 17;
        short codeTest = coder.encode(testVal);
        byte decodeTest = coder.decode(codeTest);
        
        System.out.println("Value " + testVal + " encoded: " + codeTest + " decoded:" + decodeTest);
    
    }

}
