import java.util.Date;

public class Console {

    public static void display(final String str) {
        System.out.println(new Date().toString() + ": " + str);
    }

    public static void displayError(final String str) {
        System.err.println(new Date().toString() + ": " + str);
    }
}
