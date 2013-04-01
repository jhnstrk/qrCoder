

public class Main {

    /**
     * @param args
     */
    public static void main(String[] args) {

        if (!FiniteFieldInt.test()) {
            System.err.println("FiniteField test failed");
        }

        if (!FiniteField.test()) {
            System.err.println("FiniteField test failed");
        }
        if (!BchCoder.test()) {
            System.err.println("BchCodec15_5 test failed");
        }

        if (!BchCodec15_5.test()) {
            System.err.println("BchCodec15_5 test failed");
        }

        if (!PolyHolder.test()) {
            System.err.println("PolyHolder test failed");
        }
        if (!GfPoly.test()) {
            System.err.println("GfPoly test failed");
        }
        if (!QrReedSolomon.test()) {
            System.err.println("QrReedSolomon test failed");
        }

        if (!Poly2.test()) {
            System.err.println("Poly2 test failed");
        }

    }

}
