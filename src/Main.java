

public class Main {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        if (!PolyHolder.test()) {
            System.err.println("PolyHolder test failed");
        }
        if (!GfPoly.test()) {
            System.err.println("GfPoly test failed");
        }
        if (!QrReedSolomon.test()) {
            System.err.println("QrReedSolomon test failed");
        }
        if (!QrFiniteField.test()) {
            System.err.println("QrFiniteField test failed");
        }
        if (!Poly2.test()) {
            System.err.println("Poly2 test failed");
        }
        if (!BchCodec15_5.test()) {
            System.err.println("BchCodec15_5 test failed");
        }

    }

}
