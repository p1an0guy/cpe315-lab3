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
    static int PC;

    public static void main(String[] args) {
        // open file specified in first command line arg
        parseFile(new File(args[0]));

        if (args.length > 1) {
            scriptMode(args[1]); // TODO: implement static void scriptMode
        } else {
            interactiveMode(); // TODO: implement static void interactiveMode();
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
                                Register.getBinary(parsedLine[2]),
                                Register.getBinary(parsedLine[3]),
                                Register.getBinary(parsedLine[1]),
                                "00000",
                                Rtype.getFunct(name)
                            )
                        );
                        break;
                    // r-type with shamt != 0
                    case "sll":
                        instructionArray.add(
                            new Rtype(
                                name,
                                Instruction.getOpcode(name),
                                "00000",
                                Register.getBinary(parsedLine[2]),
                                Register.getBinary(parsedLine[1]),
                                // shamt needs to be a 5-bit signed immediate
                                String.format(
                                    "%5s",
                                    Integer.toBinaryString(
                                        (Integer.parseInt(parsedLine[3])) & 0x1F
                                    )
                                ).replace(' ', '0'),
                                Rtype.getFunct(name)
                            )
                        );
                        break;
                    // jump to addr stored in register
                    case "jr":
                        instructionArray.add(
                            new Rtype(
                                name,
                                Instruction.getOpcode(name),
                                Register.getBinary(parsedLine[1]),
                                "00000",
                                "00000",
                                "00000",
                                Rtype.getFunct(name)
                            )
                        );
                        break;
                    // i-types with 3 "arguments"
                    case "addi":
                        instructionArray.add(
                            new Itype(
                                name,
                                Instruction.getOpcode(name),
                                Register.getBinary(parsedLine[2]),
                                Register.getBinary(parsedLine[1]),
                                String.format(
                                    "%16s",
                                    Integer.toBinaryString(
                                        (Integer.parseInt(parsedLine[3])) &
                                            0xFFFF
                                    )
                                ).replace(' ', '0')
                            )
                        );
                        break;
                    case "beq":
                    case "bne":
                        instructionArray.add(
                            new Itype(
                                name,
                                Instruction.getOpcode(name),
                                Register.getBinary(parsedLine[1]),
                                Register.getBinary(parsedLine[2]),
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
                                Register.getBinary(parsedLine[3]),
                                Register.getBinary(parsedLine[1]),
                                String.format(
                                    "%16s",
                                    Integer.toBinaryString(
                                        (Integer.parseInt(parsedLine[2])) &
                                            0xFFFF
                                    )
                                ).replace(' ', '0')
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
                        instructionArray.add(
                            new Jtype(name, "invalid", "invalid") {}
                        );
                        break;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error opening specified file");
            e.printStackTrace();
            System.exit(1);
        }

        // SECOND PASS - REPLACE LABELS WITH ADDRESSES
        for (Instruction inst : instructionArray) {
            if (inst.getOpcode().equals("invalid")) {
                System.out.println("invalid instruction: " + inst.getName());
                System.exit(1);
            }
            // beq and bne: 16-bit immediate = labelAddr - (curr instruction idx + 1)
            if (inst.getName().equals("beq") || inst.getName().equals("bne")) {
                ((Itype) inst).setImmediate(
                    // convert to 2's complement binary, sign extend to 16 bits
                    String.format(
                        "%16s",
                        Integer.toBinaryString(
                            (labelAddr.get(((Itype) inst).getImmediate()) -
                                    (instructionArray.indexOf(inst) + 1)) &
                                0xFFFF
                        )
                    ).replace(' ', '0')
                );
                // j and jal: use the absolute address of the label
            } else if (
                inst.getName().equals("j") || inst.getName().equals("jal")
            ) {
                ((Jtype) inst).setAddress(
                    // convert to 2's complement binary, sign extend to 26 bits
                    String.format(
                        "%26s",
                        Integer.toBinaryString(
                            (labelAddr.get(((Jtype) inst).getAddress()) &
                                0x3FFFFFF)
                        )
                    ).replace(' ', '0')
                );
            }
            // print out the binary for the instruction
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
                // $0 = 0          $v0 = 0         $v1 = 0         $a0 = 0
                // $a1 = 0         $a2 = 0         $a3 = 0         $t0 = 0
                // $t1 = 0         $t2 = 0         $t3 = 0         $t4 = 0
                // $t5 = 0         $t6 = 0         $t7 = 0         $s0 = 0
                // $s1 = 0         $s2 = 0         $s3 = 0         $s4 = 0
                // $s5 = 0         $s6 = 0         $s7 = 0         $t8 = 0
                // $t9 = 0         $sp = 0         $ra = 0
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
            case 's': // single step through program
                // see if there are more args
                break;
            case 'r': // run until the program ends
                break;
            case 'm': // m n1 n2 display dataMem from n1 to n2
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
}
