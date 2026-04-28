import java.io.File; // Import the File class
import java.io.FileNotFoundException; // Import this class to handle errors
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner; // Import the Scanner class to read text files

public class lab3 {

    static HashMap<String, Integer> labelAddr = new HashMap<>(); // hash map to store labels + their addr
    static ArrayList<Instruction> instructionArray = new ArrayList<>();
    static int[] registers = new int[32]; // N.B. remember to always set $0 and $zero to 0!!
    static int[] dataMem = new int[8192];
    static int PC = 0;

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.out.println("Invalid number of arguments provided.");
            System.exit(1);
        }
        // open file specified in first command line arg
        parseFile(new File(args[0]));

        if (args.length > 1) {
            scriptMode(args[1]);
        } else {
            interactiveMode();
        }
    }

    public static void parseFile(File input) {
        // try with resources for auto-cleanup
        try (Scanner inputReader = new Scanner(input)) {
            int instructionIdx = 0; // initialize instruction counter

            // FIRST PASS - COMPUTE ADDRESSES OF LABELS
            while (inputReader.hasNextLine()) {
                String line = inputReader.nextLine();

                // ignore comments
                if (line.indexOf("#") != -1) {
                    line = line.substring(0, line.indexOf("#"));
                }

                // label handling
                if (line.indexOf(":") != -1) {
                    // label is all non-whitspace up until colon
                    String label = line.substring(0, line.indexOf(":"));
                    label = label.trim();

                    // associate the label with the following instruction
                    labelAddr.put(label, instructionIdx);

                    // remove the label from the line so that we can parse the rest of it
                    line = line.substring(line.indexOf(":") + 1);
                }

                // remove whitespace
                line = line.trim();
                if (line.isEmpty()) continue;

                // we now have removed labels and comments from the line
                instructionIdx += 1; // increment instruction counter
                String regex = "[,\\.\\s()$]+";
                String[] parsedLine = line.split(regex);
                String name = parsedLine[0];
                switch (name) {
                    // r-types with rs, rt, rd, shamt = 0
                    case "add":
                    case "sub":
                    case "and":
                    case "or":
                    case "slt":
                        instructionArray.add(
                            new Rtype(
                                name,
                                Instruction.getOpcode(name),
                                Register.getNumber(parsedLine[2]),
                                Register.getNumber(parsedLine[3]),
                                Register.getNumber(parsedLine[1]),
                                0
                            )
                        );
                        break;
                    // r-type with shamt != 0
                    case "sll":
                        instructionArray.add(
                            new Rtype(
                                name,
                                Instruction.getOpcode(name),
                                0,
                                Register.getNumber(parsedLine[2]),
                                Register.getNumber(parsedLine[1]),
                                Integer.parseInt(parsedLine[3])
                            )
                        );
                        break;
                    // jump to addr stored in register
                    case "jr":
                        instructionArray.add(
                            new Rtype(
                                name,
                                Instruction.getOpcode(name),
                                Register.getNumber(parsedLine[1]),
                                0,
                                0,
                                0
                            )
                        );
                        break;
                    // i-types with 3 "arguments"
                    case "addi":
                        instructionArray.add(
                            new Itype(
                                name,
                                Instruction.getOpcode(name),
                                Register.getNumber(parsedLine[2]),
                                Register.getNumber(parsedLine[1]),
                                Integer.parseInt(parsedLine[3])
                            )
                        );
                        break;
                    case "beq":
                    case "bne":
                        instructionArray.add(
                            new Itype(
                                name,
                                Instruction.getOpcode(name),
                                Register.getNumber(parsedLine[1]),
                                Register.getNumber(parsedLine[2]),
                                parsedLine[3]
                            )
                        );
                        break;
                    // i-type of form rt, imm(rs)
                    case "lw":
                    case "sw":
                        instructionArray.add(
                            new Itype(
                                name,
                                Instruction.getOpcode(name),
                                Register.getNumber(parsedLine[3]),
                                Register.getNumber(parsedLine[1]),
                                Integer.parseInt(parsedLine[2])
                            )
                        );
                        break;
                    // jump to label
                    case "j":
                    case "jal":
                        instructionArray.add(
                            new Jtype(
                                name,
                                Instruction.getOpcode(name),
                                parsedLine[1]
                            )
                        );
                        break;
                    default:
                        instructionArray.add(new Jtype(name, "invalid", -1) {});
                        break;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error opening specified file");
            e.printStackTrace();
            System.exit(1);
        }

        // SECOND PASS - REPLACE LABELS WITH ADDRESSES
        for (int i = 0; i < instructionArray.size(); i++) {
            Instruction inst = instructionArray.get(i);
            if (inst.getOpcode().equals("invalid")) {
                System.out.println("invalid instruction: " + inst.getName());
                System.exit(1);
            }
            // beq and bne: 16-bit immediate = labelAddr - (curr instruction idx + 1)
            if (inst.getName().equals("beq") || inst.getName().equals("bne")) {
                ((Itype) inst).setImmediate(
                    labelAddr.get(((Itype) inst).getLabel()) - (i + 1)
                );
                // j and jal: use the absolute address of the label
            } else if (
                inst.getName().equals("j") || inst.getName().equals("jal")
            ) {
                ((Jtype) inst).setAddress(
                    labelAddr.get(((Jtype) inst).getLabel())
                );
            }
            // print out the parsed instruction for debugging
            System.out.println(inst);
        }
    }

    public static void interactiveMode() {
        Scanner usrInput = new Scanner(System.in);
        while (true) {
            System.out.print("mips> ");
            String input = usrInput.next();
            if (input.charAt(0) == 'q') break;
            executeCmd(input);
        }

        usrInput.close();
    }

    public static void scriptMode(String file) {
        File myObj = new File(file);

        // try-with-resources: Scanner will be closed automatically
        try (Scanner scanner = new Scanner(myObj)) {
            while (scanner.hasNextLine()) {
                String cmd = scanner.nextLine();
                executeCmd(cmd);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Unable to open script file.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void executeCmd(String cmd) {
        switch (cmd.charAt(0)) {
            case 'h': // sh = show help
                System.out.println("d = dump register state");
                System.out.println(
                    "s = single step through the program (i.e. execute 1 instruction and stop)"
                );
                System.out.println(
                    "s num = step through num instructions of the program"
                );
                System.out.println("r = run until the program ends");
                System.out.println(
                    "m num1 num2 = display data memory from location num1 to num2"
                );
                System.out.println(
                    "c = clear all registers, memory, and the program counter to 0"
                );
                System.out.println("q = exit the program");
                break;
            case 'd': // dump register state
                System.out.println("\npc = " + PC);
                System.out.println(
                    "$0 = " +
                        registers[0] +
                        "\t$v0 = " +
                        registers[2] +
                        "\t$v1 = " +
                        registers[3] +
                        "\t$a0 = " +
                        registers[4]
                );
                System.out.println(
                    "$a1 = " +
                        registers[5] +
                        "\t$a2 = " +
                        registers[6] +
                        "\t$a3 = " +
                        registers[7] +
                        "\t$t0 = " +
                        registers[8]
                );
                System.out.println(
                    "$t1 = " +
                        registers[9] +
                        "\t$t2 = " +
                        registers[10] +
                        "\t$t3 = " +
                        registers[11] +
                        "\t$t4 = " +
                        registers[12]
                );
                System.out.println(
                    "$t5 = " +
                        registers[13] +
                        "\t$t6 = " +
                        registers[14] +
                        "\t$t7 = " +
                        registers[15] +
                        "\t$s0 = " +
                        registers[16]
                );
                System.out.println(
                    "$s1 = " +
                        registers[17] +
                        "\t$s2 = " +
                        registers[18] +
                        "\t$s3 = " +
                        registers[19] +
                        "\t$s4 = " +
                        registers[20]
                );
                System.out.println(
                    "$s5 = " +
                        registers[21] +
                        "\t$s6 = " +
                        registers[22] +
                        "\t$s7 = " +
                        registers[23] +
                        "\t$t8 = " +
                        registers[24]
                );
                System.out.println(
                    "$t9 = " +
                        registers[25] +
                        "\t$sp = " +
                        registers[29] +
                        "\t$ra = " +
                        registers[31]
                );

                break;
            case 's':
                // determine how many instructions to execute
                int n =
                    cmd.length() > 1
                        ? Integer.parseInt(cmd.substring(cmd.indexOf('s')))
                        : 1;
                // step through n number of instructions
                for (int i = 0; i < n; i++) {
                    if (PC >= instructionArray.size()) {
                        break;
                    }
                    executeInstruction();
                }
                System.out.println("\t" + n + " instruction(s) executed");
                break;
            case 'r': // run until the program ends
                while (PC < instructionArray.size()) {
                    executeInstruction();
                    PC++;
                }
                System.exit(0);
                break;
            case 'm': // m n1 n2 display dataMem from n1 to n2
                String regex = "[,\\.\\s()$]+";
                String[] parsedLine = cmd.split(regex);
                int n1 = Integer.parseInt(parsedLine[1]);
                int n2 = Integer.parseInt(parsedLine[2]);
                if (
                    n1 < 0 ||
                    n1 > dataMem.length ||
                    n2 < 0 ||
                    n2 > dataMem.length ||
                    n1 > n2
                ) {
                    System.out.println("Invalid memory range.");
                    break;
                }
                for (int i = n1; i <= n2; i++) {
                    System.out.println("[" + i + "] = " + dataMem[i]);
                }
                break;
            case 'c': // clear all registers, mem, reset PC
                Arrays.fill(registers, 0);
                Arrays.fill(dataMem, 0);
                PC = 0;
                System.out.println("\tSimulator reset");
                break;
            default:
                System.out.println("Error: Invalid command. 'h' for help menu");
        }
    }

    static void executeInstruction() {
        Instruction currInst = instructionArray.get(PC);

        switch (currInst.getName()) {
            case "add": {
                ((Rtype) currInst).setRd(
                    ((Rtype) currInst).getRs() + ((Rtype) currInst).getRt()
                );
                PC++;
                break;
            }
            case "sub": {
                ((Rtype) currInst).setRd(
                    ((Rtype) currInst).getRs() - ((Rtype) currInst).getRt()
                );
                PC++;
                break;
            }
            case "and": {
                ((Rtype) currInst).setRd(
                    ((Rtype) currInst).getRs() & ((Rtype) currInst).getRt()
                );
                PC++;
                break;
            }
            case "or": {
                ((Rtype) currInst).setRd(
                    ((Rtype) currInst).getRs() | ((Rtype) currInst).getRt()
                );
                PC++;
                break;
            }
            case "slt": {
                ((Rtype) currInst).setRd(
                    ((Rtype) currInst).getRs() < ((Rtype) currInst).getRt()
                        ? 1
                        : 0
                );
                PC++;
                break;
            }
            case "sll": {
                ((Rtype) currInst).setRd(
                    (((Rtype) currInst).getRt() <<
                        ((Rtype) currInst).getShamt())
                );
                PC++;
                break;
            }
            case "jr": {
                // register is found in rs field of the current instruction
                int dest = ((Rtype) currInst).getRs();
                if (dest >= registers.length) {
                    System.out.println(
                        "Error: jr instruction tried to access an invalid instruction index."
                    );
                    System.exit(1);
                }
                PC = registers[dest];
                break;
            }
            case "addi": {
                ((Itype) currInst).setRt(
                    ((Itype) currInst).getRt() +
                        ((Itype) currInst).getImmediate()
                );
                PC++;
                break;
            }
            case "beq": {
                // if rs == rt, PC = PC + 1 + branch address
                // else move onto the next instruction
                PC =
                    ((Itype) currInst).getRs() == ((Itype) currInst).getRt()
                        ? PC + 1 + ((Itype) currInst).getImmediate()
                        : PC + 1;
                break;
            }
            case "bne": {
                PC =
                    ((Itype) currInst).getRs() != ((Itype) currInst).getRt()
                        ? PC + 1 + ((Itype) currInst).getImmediate()
                        : PC + 1;
                break;
            }
            case "lw": {
                // reg(rt) = mem(rs + imm)
                registers[((Itype) currInst).getRt()] = dataMem[registers[((
                        (Itype) currInst
                    ).getRs())] +
                ((Itype) currInst).getImmediate()];
                PC++;
                break;
            }
            case "sw": {
                // mem(rs + imm) = reg(rt)
                dataMem[registers[(((Itype) currInst).getRs())] +
                ((Itype) currInst).getImmediate()] = registers[(
                    (Itype) currInst
                ).getRt()];
                PC++;
                break;
            }
            case "j": {
                int dest = ((Jtype) currInst).getAddress();
                if (dest >= instructionArray.size()) {
                    System.out.println(
                        "Error: j instruction tried to access an invalid instruction index."
                    );
                    System.exit(1);
                }
                PC = dest;
                break;
            }
            case "jal": {
                registers[Register.getNumber("ra")] = PC + 1;
                int dest = ((Jtype) currInst).getAddress();
                if (dest >= instructionArray.size()) {
                    System.out.println(
                        "Error: jal instruction tried to access an invalid instruction index."
                    );
                    System.exit(1);
                }
                PC = dest;
                break;
            }
            default: {
                System.out.println(
                    "Error: unsupported instruction " + currInst.getName()
                );
                System.exit(1);
            }
        }
    }
}
