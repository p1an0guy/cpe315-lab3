public class Itype extends Instruction {

    private int rs;
    private int rt;
    private int immediate;
    private String label;

    public Itype(String name, String opcode, int rs, int rt, int immediate) {
        super(name, opcode);
        this.rs = rs;
        this.rt = rt;
        this.immediate = immediate;
        this.label = null;
    }

    public Itype(String name, String opcode, int rs, int rt, String label) {
        super(name, opcode);
        this.rs = rs;
        this.rt = rt;
        this.immediate = 0;
        this.label = label;
    }

    public int getRs() {
        return rs;
    }

    public void setRs(int rs) {
        this.rs = rs;
    }

    public int getRt() {
        return rt;
    }

    public void setRt(int rt) {
        this.rt = rt;
    }

    public int getImmediate() {
        return immediate;
    }

    public void setImmediate(int immediate) {
        this.immediate = immediate;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return getName() + " rs=" + rs + " rt=" + rt + " immediate=" + immediate;
    }
}
