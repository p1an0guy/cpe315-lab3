import java.util.HashMap;

public class Register {

    private static final HashMap<String, Integer> REGISTERS = new HashMap<>();

    static {
        REGISTERS.put("zero", 0);
        REGISTERS.put("0", 0);
        REGISTERS.put("v0", 2);
        REGISTERS.put("v1", 3);
        REGISTERS.put("a0", 4);
        REGISTERS.put("a1", 5);
        REGISTERS.put("a2", 6);
        REGISTERS.put("a3", 7);
        REGISTERS.put("t0", 8);
        REGISTERS.put("t1", 9);
        REGISTERS.put("t2", 10);
        REGISTERS.put("t3", 11);
        REGISTERS.put("t4", 12);
        REGISTERS.put("t5", 13);
        REGISTERS.put("t6", 14);
        REGISTERS.put("t7", 15);
        REGISTERS.put("s0", 16);
        REGISTERS.put("s1", 17);
        REGISTERS.put("s2", 18);
        REGISTERS.put("s3", 19);
        REGISTERS.put("s4", 20);
        REGISTERS.put("s5", 21);
        REGISTERS.put("s6", 22);
        REGISTERS.put("s7", 23);
        REGISTERS.put("t8", 24);
        REGISTERS.put("t9", 25);
        REGISTERS.put("sp", 29);
        REGISTERS.put("ra", 31);
    }

    public static String getBinary(String name) {
        return String.format("%5s", Integer.toBinaryString(getNumber(name))).replace(
            ' ',
            '0'
        );
    }

    public static int getNumber(String name) {
        if (name == null) {
            return -1;
        }
        return REGISTERS.get(name.startsWith("$") ? name.substring(1) : name);
    }
}
