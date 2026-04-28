public class Jtype extends Instruction {

    private int address;
    private String label;

    public Jtype(String name, String opcode, int address) {
        super(name, opcode);
        this.address = address;
        this.label = null;
    }

    public Jtype(String name, String opcode, String label) {
        super(name, opcode);
        this.address = 0;
        this.label = label;
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return getName() + " address=" + address;
    }
}
