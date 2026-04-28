public class Rtype extends Instruction {

    private int rs;
    private int rt;
    private int rd;
    private int shamt;

    public Rtype(
        String name,
        String opcode,
        int rs,
        int rt,
        int rd,
        int shamt
    ) {
        super(name, opcode);
        this.rs = rs;
        this.rt = rt;
        this.rd = rd;
        this.shamt = shamt;
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

    public int getRd() {
        return rd;
    }

    public void setRd(int rd) {
        this.rd = rd;
    }

    public int getShamt() {
        return shamt;
    }

    public void setShamt(int shamt) {
        this.shamt = shamt;
    }

    @Override
    public String toString() {
        return getName() + " rs=" + rs + " rt=" + rt + " rd=" + rd + " shamt=" + shamt;
    }
}
