import java.util.HashMap;

abstract class Instruction {

    private String name;
    private String opcode;
    public static final HashMap<String, String> OPCODE = new HashMap<>();

    static {
        OPCODE.put("and", "000000");
        OPCODE.put("or", "000000");
        OPCODE.put("add", "000000");
        OPCODE.put("addi", "001000");
        OPCODE.put("sll", "000000");
        OPCODE.put("sub", "000000");
        OPCODE.put("slt", "000000");
        OPCODE.put("beq", "000100");
        OPCODE.put("bne", "000101");
        OPCODE.put("lw", "100011");
        OPCODE.put("sw", "101011");
        OPCODE.put("j", "000010");
        OPCODE.put("jr", "000000");
        OPCODE.put("jal", "000011");
    }

    public Instruction(String name, String opcode) {
        this.name = name;
        this.opcode = opcode;
    }

    public static String getOpcode(String name) {
        return OPCODE.get(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOpcode() {
        return opcode;
    }

    public void setOpcode(String opcode) {
        this.opcode = opcode;
    }

    @Override
    public abstract String toString();
}
