import java.util.Random;

public class MyRandom {
    private static final Random random = new Random();

    public static int randomFloor(final int numFloors) {
        final int rand = randomInt(0, numFloors * 2);
        return rand >= numFloors ? 0 : rand;
    }

    public static int randomFloorDifferentThan(final int value, final int numFloors) {
        if (value == 0 && numFloors <= 1)
            throw new IllegalArgumentException(value + " is the only option for a floor in the interval [" + value + "," + (numFloors - 1) + "]");

        int rand;
        do {
            rand = randomFloor(numFloors);
        } while (rand == value);
        return rand;
    }

    public static int randomInt(final int min, final int max) {
        return min + random.nextInt(max - min + 1);
    }
}
