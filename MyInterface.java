public class MyInterface {
    private int backtrackCounter;

    public MyInterface() {
        this.backtrackCounter = 0;
    }

    public void displayProgress(String str) {
        displayAux(str);
        backtrackCounter = str.length();
    }

    public void display(String str) {
        displayAux(str);
        backtrackCounter = 0;
    }

    private void displayAux(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < backtrackCounter; i++)
            sb.append('\b');
        sb.append(str);
        System.out.print(sb.toString());
    }
}
