//import java.util.Vector;

// Perform math in the finite field given by

public class QrFiniteField {


    private static FiniteField instance256 = null;
    private static FiniteField instance32 = null;

    // The primitive polynomial for this field
    static final int generator256 = 0x011d; // x^8 + x^4 + x^3 + x^2 + 1. or 100011101
    static final int generator32  = 0x25;   // x^5 + x^2 + 1. or 100101

    public static FiniteField getInstance256() {
        if ( instance256 == null)
            instance256 = new FiniteField(generator256);
        return instance256;
    }

    public static FiniteField getInstance32() {
        if ( instance32 == null)
            instance32 = new FiniteField(generator32);
        return instance32;
    }
}
