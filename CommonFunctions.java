public class CommonFunctions {

    public static void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            MyBoot.logger.warning(e.toString());
        }
    }
}
