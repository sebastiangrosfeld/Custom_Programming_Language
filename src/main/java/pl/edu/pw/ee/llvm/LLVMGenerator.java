package pl.edu.pw.ee.llvm;

import java.util.List;

class LLVMGenerator {

    private static final StringBuilder HEADER_TEXT = new StringBuilder();
    private static final StringBuilder MAIN_TEXT = new StringBuilder();
    private static final StringBuilder BUFFER_TEXT = new StringBuilder();
    static int register = 1;
    static int str = 1;
    static int arr = 1;
    static int mat = 1;
    static int loops = 0;
    static int cur_loops = 0;
    static int loop_index = 0;
    static int ifs = 0;
    static int cur_ifs = 0;
    static int if_index = 0;
    private static StringBuilder CURRENT_TEXT = MAIN_TEXT;

    static void printf(Value value) {
        if (value.type == PrimitiveType.BOOLEAN) {
            printf_bool();
            return;
        }

        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @")
                .append(value.type.llvmPrintPattern())
                .append(", i32 0, i32 0), ")
                .append(value.type.llvmType())
                .append(" ")
                .append(value.name())
                .append(")\n");
        register++;
    }

    private static void printf_bool() {
        // Porównanie, czy wartość boola to 1 (true) czy 0 (false)
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = icmp eq i1 %")
                .append(register - 1)
                .append(", 1\n");
        register++;

        // Konwersja wyniku porównania na string ("true" lub "false")
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = select i1 %")
                .append(register - 1)
                .append(", i8* @truetext, i8* @falsetext\n");
        register++;

        // Wywołanie printf z formatem %s
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([6 x i8], [6 x i8]* @strpb, i32 0, i32 0), i8* %")
                .append(register - 1)
                .append(")\n");
        register++;
    }

    static void declare(String id, Type type, boolean isGlobalContext) {
        final var text = isGlobalContext ? HEADER_TEXT : CURRENT_TEXT;
        text.append(isGlobalContext ? "@" : "%")
                .append(id)
                .append(" = ")
                .append(isGlobalContext ? "global" : "alloca")
                .append(" ")
                .append(type.llvmType())
                .append(isGlobalContext ? " " + type.llvmZeroValue() : "")
                .append("\n");
    }

    static void declare(Array array) {
        final var text = array.isGlobal ? HEADER_TEXT : CURRENT_TEXT;
        text.append(array.name())
                .append(" = ")
                .append(array.isGlobal ? "global" : "alloca")
                .append(" [")
                .append(array.length)
                .append(" x ")
                .append(array.type.llvmType())
                .append("]")
                .append(array.isGlobal ? " zeroinitializer" : "")
                .append("\n");
    }

    static void declare(Matrix matrix) {
        final var text = matrix.isGlobal ? HEADER_TEXT : CURRENT_TEXT;
        text.append(matrix.name())
                .append(" = ")
                .append(matrix.isGlobal ? "global" : "alloca")
                .append(" [")
                .append(matrix.rows.size())
                .append(" x [")
                .append(matrix.rowLength)
                .append(" x ")
                .append(matrix.type.llvmType())
                .append("]*]")
                .append(matrix.isGlobal ? " zeroinitializer" : "")
                .append("\n");
    }

    static void assign_array_item(Array array, String index, Value value) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = getelementptr inbounds [")
                .append(array.length)
                .append(" x ")
                .append(value.type.llvmType())
                .append("], [")
                .append(array.length)
                .append(" x ")
                .append(value.type.llvmType())
                .append("]* ")
                .append(array.name())
                .append(", i64 0, i64 ")
                .append(index)
                .append("\n");
        CURRENT_TEXT.append("store ")
                .append(value.type.llvmType())
                .append(" ")
                .append(value.name())
                .append(", ")
                .append(value.type.llvmType())
                .append("* %")
                .append(register)
                .append("\n");
        register++;
    }

    static void assign_matrix_row(Matrix matrix, String index, Array value) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = getelementptr inbounds [")
                .append(matrix.rows.size())
                .append(" x [")
                .append(matrix.rowLength)
                .append(" x ")
                .append(matrix.type.llvmType())
                .append("]*], [")
                .append(matrix.rows.size())
                .append(" x [")
                .append(matrix.rowLength)
                .append(" x ")
                .append(matrix.type.llvmType())
                .append("]*]* ")
                .append(matrix.name())
                .append(", i64 0, i64 ")
                .append(index)
                .append("\n");
        CURRENT_TEXT.append("store [")
                .append(matrix.rowLength)
                .append(" x ")
                .append(matrix.type.llvmType())
                .append("]* ")
                .append(value.name())
                .append(", [")
                .append(matrix.rowLength)
                .append(" x ")
                .append(matrix.type.llvmType())
                .append("]** %")
                .append(register)
                .append("\n");
        register++;
    }

    static void assign_matrix_item(Matrix matrix, String rowIndex, String columnIndex, Value value) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = getelementptr inbounds [")
                .append(matrix.rows.size())
                .append(" x [")
                .append(matrix.rowLength)
                .append(" x ")
                .append(value.type.llvmType())
                .append("]*], [")
                .append(matrix.rows.size())
                .append(" x [")
                .append(matrix.rowLength)
                .append(" x ")
                .append(value.type.llvmType())
                .append("]*]* ")
                .append(matrix.name())
                .append(", i64 0, i64 ")
                .append(rowIndex)
                .append("\n");
        register++;
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = load [")
                .append(matrix.rowLength)
                .append(" x ")
                .append(value.type.llvmType())
                .append("]*, [")
                .append(matrix.rowLength)
                .append(" x ")
                .append(value.type.llvmType())
                .append("]** %")
                .append(register - 1)
                .append("\n");
        register++;
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = getelementptr inbounds [")
                .append(matrix.rowLength)
                .append(" x ")
                .append(value.type.llvmType())
                .append("], [")
                .append(matrix.rowLength)
                .append(" x ")
                .append(value.type.llvmType())
                .append("]* %")
                .append(register - 1)
                .append(", i64 0, i64 ")
                .append(columnIndex)
                .append("\n");
        CURRENT_TEXT.append("store ")
                .append(value.type.llvmType())
                .append(" ")
                .append(value.name())
                .append(", ")
                .append(value.type.llvmType())
                .append("* %")
                .append(register)
                .append("\n");
        register++;
    }

    static void assign(String id, boolean isGlobalContext, Value value) {
        CURRENT_TEXT.append("store ")
                .append(value.type.llvmType())
                .append(" ")
                .append(value.name())
                .append(", ")
                .append(value.type.llvmType())
                .append("* ")
                .append(isGlobalContext ? "@" : "%")
                .append(id)
                .append("\n");
    }

    static Value load(String id, Value value, boolean isGlobalContext) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = load ")
                .append(value.type.llvmType())
                .append(", ")
                .append(value.type.llvmType())
                .append("* ")
                .append(isGlobalContext ? "@" : "%")
                .append(id)
                .append("\n");
        register++;
        final var newValue = value.withName(String.valueOf(register - 1));
        newValue.isGlobal = false;
        return newValue;
    }

    static void load_array_value(Array array, String index) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = getelementptr inbounds [")
                .append(array.length)
                .append(" x ")
                .append(array.type.llvmType())
                .append("], [")
                .append(array.length)
                .append(" x ")
                .append(array.type.llvmType())
                .append("]* ")
                .append(array.name())
                .append(", i64 0, i64 ")
                .append(index)
                .append("\n");
        register++;
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = load ")
                .append(array.type.llvmType())
                .append(", ")
                .append(array.type.llvmType())
                .append("* %")
                .append(register - 1)
                .append("\n");
        register++;
    }

    static void load_matrix_value(Matrix matrix, String rowIndex, String columnIndex) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = getelementptr inbounds [")
                .append(matrix.rows.size())
                .append(" x [")
                .append(matrix.rowLength)
                .append(" x ")
                .append(matrix.type.llvmType())
                .append("]*], [")
                .append(matrix.rows.size())
                .append(" x [")
                .append(matrix.rowLength)
                .append(" x ")
                .append(matrix.type.llvmType())
                .append("]*]* ")
                .append(matrix.name())
                .append(", i64 0, i64 ")
                .append(rowIndex)
                .append("\n");
        register++;
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = load [")
                .append(matrix.rowLength)
                .append(" x ")
                .append(matrix.type.llvmType())
                .append("]*, [")
                .append(matrix.rowLength)
                .append(" x ")
                .append(matrix.type.llvmType())
                .append("]** %")
                .append(register - 1)
                .append("\n");
        register++;
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = getelementptr inbounds [")
                .append(matrix.rowLength)
                .append(" x ")
                .append(matrix.type.llvmType())
                .append("], [")
                .append(matrix.rowLength)
                .append(" x ")
                .append(matrix.type.llvmType())
                .append("]* %")
                .append(register - 1)
                .append(", i64 0, i64 ")
                .append(columnIndex)
                .append("\n");
        register++;
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = load ")
                .append(matrix.type.llvmType())
                .append(", ")
                .append(matrix.type.llvmType())
                .append("* %")
                .append(register - 1)
                .append("\n");
        register++;
    }

    static void jump_to_else_end() {
        final var endLabel = "else_end_" + cur_ifs;
        CURRENT_TEXT.append("br label %")
                .append(endLabel)
                .append("\n");
    }

    static void write_else_end_label() {
        final var endLabel = "else_end_" + cur_ifs;
        CURRENT_TEXT.append(endLabel)
                .append(": \n");
    }

    static void write_else_start() {
        var number = cur_ifs;
        final var startLabel = "else_start_" + number;
        CURRENT_TEXT.append("br label %")
                .append(startLabel)
                .append("\n");
        CURRENT_TEXT.append(startLabel)
                .append(": \n");
    }

    static void evaluate_else() {
        final var body_label = "else_body_" + cur_ifs;
        final var end_label = "else_end_" + cur_ifs;
        CURRENT_TEXT.append("br i1 %")
                .append(register - 1)
                .append(", label %")
                .append(end_label)
                .append(", label %")
                .append(body_label)
                .append("\n");
        CURRENT_TEXT.append(body_label)
                .append(": \n");
    }

    static void write_if_start_label() {
        ifs++;
        cur_ifs++;
        final var startLabel = "if_start_" + cur_ifs;
        CURRENT_TEXT.append("br label %")
                .append(startLabel)
                .append("\n");
        CURRENT_TEXT.append(startLabel)
                .append(": \n");
    }

    static void evaluate_if_condition(String id) {
        final var body_label = "if_body_" + cur_ifs;
        final var end_label = "if_end_" + cur_ifs;
        CURRENT_TEXT.append("br i1 %")
                .append(id)
                .append(", label %")
                .append(body_label)
                .append(", label %")
                .append(end_label)
                .append("\n");
        CURRENT_TEXT.append(body_label)
                .append(": \n");
    }

    static void jump_to_if_end() {
        final var endLabel = "if_end_" + cur_ifs;
        CURRENT_TEXT.append("br label %")
                .append(endLabel)
                .append("\n");
    }

    static void write_if_end_label() {
        final var endLabel = "if_end_" + cur_ifs;
        CURRENT_TEXT.append(endLabel)
                .append(": \n");
        cur_ifs--;
        if (cur_ifs == if_index) {
            if_index = ifs;
            cur_ifs = ifs;
        }
        register++;
    }

    static void write_loop_start_label() {
        loops++;
        cur_loops++;
        final var startLabel = "loop_start_" + cur_loops;
        CURRENT_TEXT.append("br label %")
                .append(startLabel)
                .append("\n");
        CURRENT_TEXT.append(startLabel)
                .append(": \n");
    }

    static void evaluate_loop() {
        final var body_label = "loop_body_" + cur_loops;
        final var end_label = "loop_end_" + cur_loops;
        CURRENT_TEXT.append("br i1 %")
                .append(register - 1)
                .append(", label %")
                .append(body_label)
                .append(", label %")
                .append(end_label)
                .append("\n");
    }

    static void evaluate_loop_condition(String id) {
        final var body_label = "loop_body_" + cur_loops;
        final var end_label = "loop_end_" + cur_loops;
        CURRENT_TEXT.append("br i1 %")
                .append(id)
                .append(", label %")
                .append(body_label)
                .append(", label %")
                .append(end_label)
                .append("\n");
        CURRENT_TEXT.append(body_label)
                .append(": \n");
    }

    static void write_loop_end_label() {
        final var endLabel = "loop_end_" + cur_loops;
        CURRENT_TEXT.append(endLabel)
                .append(": \n");
        cur_loops--;
        if (cur_loops == loop_index) {
            loop_index = loops;
            cur_loops = loops;
        }
        register++;
    }

    static void and(Value value1, Value value2) {
        final var labelTrue = "and_true_" + register;
        final var labelNotTrue = "and_not_true_" + register;
        final var labelEnd = "and_end_" + register;
        final var result = "%" + register;
        final var trueVal = "%true_" + register;
        final var falseVal = "%false_" + register;
        register++;

        // Jeśli value1 jest fałszywe, skaczemy od razu do końca
        CURRENT_TEXT.append("br i1 ")
                .append(value1.name())
                .append(", label %")
                .append(labelTrue)
                .append(", label %")
                .append(labelNotTrue)
                .append("\n");

        // Blok jeśli value1 == true sprawdzamy value2
        CURRENT_TEXT.append(labelTrue)
                .append(":\n");
        CURRENT_TEXT.append(trueVal)
                .append(" = and i1 ")
                .append(value1.name())
                .append(", ")
                .append(value2.name())
                .append("\n");
        CURRENT_TEXT.append("br label %")
                .append(labelEnd)
                .append("\n");

        // jesli nie to zwracamy zero
        CURRENT_TEXT.append(labelNotTrue)
                .append(":\n");
        CURRENT_TEXT.append(falseVal)
                .append(" = and i1 0, 0\n");
        CURRENT_TEXT.append("br label %")
                .append(labelEnd)
                .append("\n");

        CURRENT_TEXT.append(labelEnd)
                .append(":\n")
                .append(result)
                .append(" = phi i1 [ ").append(trueVal).append(", %").append(labelTrue)
                .append(" ], [ ").append(falseVal).append(", %").append(labelNotTrue).append(" ]\n");
    }

    static void or(Value value1, Value value2) {
        final var labelTrue = "and_true_" + register;
        final var labelNotTrue = "and_not_true_" + register;
        final var labelEnd = "and_end_" + register;
        final var result = "%" + register;
        final var trueVal = "%true_" + register;
        final var falseVal = "%false_" + register;
        register++;

        // Jeśli value1 jest prawdziwe, skaczemy od labelTrue
        CURRENT_TEXT.append("br i1 ")
                .append(value1.name())
                .append(", label %")
                .append(labelTrue)
                .append(", label %")
                .append(labelNotTrue)
                .append("\n");

        // Blok jeśli value1 == true zwracamy od razu prawda
        CURRENT_TEXT.append(labelTrue)
                .append(":\n");
        CURRENT_TEXT.append(trueVal)
                .append(" = or i1 1, 1")
                .append("\n")
                .append("br label %")
                .append(labelEnd)
                .append("\n");

        // Blok jesli value1 != true obliczamy or
        CURRENT_TEXT.append(labelNotTrue)
                .append(":\n");
        CURRENT_TEXT.append(falseVal)
                .append(" = or i1 ").append(value1.name()).append(", ").append(value2.name())
                .append("\n")
                .append("br label %")
                .append(labelEnd)
                .append("\n");

        // Blok końcowy
        CURRENT_TEXT.append(labelEnd)
                .append(":\n")
                .append(result)
                .append(" = phi i1 [ ").append(trueVal).append(", %").append(labelTrue)
                .append(" ], [ ").append(falseVal).append(", %").append(labelNotTrue).append(" ]\n");
    }

    static void xor(Value value1, Value value2) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = xor i1 ")
                .append(value1.name())
                .append(", ")
                .append(value2.name())
                .append("\n");
        register++;
    }

    static void neg(Value value) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = xor i1 ")
                .append(value.name())
                .append(", 1\n");  // NOT = XOR z 1
        register++;
    }

    static void equal_i(Value value1, Value value2) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = ")
                .append(value1.type.llvmComparator())
                .append(" eq ")
                .append(value2.type.llvmType())
                .append(" ")
                .append(value1.name())
                .append(", ")
                .append(value2.name())
                .append("\n");
        register++;
    }

    static void equal_f(Value value1, Value value2) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = ")
                .append(value1.type.llvmComparator())
                .append(" oeq ")
                .append(value2.type.llvmType())
                .append(" ")
                .append(value1.name())
                .append(", ")
                .append(value2.name())
                .append("\n");
        register++;
    }

    static void equal_s(Value value1, Value value2) {
        final var result = "%result_" + register;
        CURRENT_TEXT.append(result)
                .append(" = call i32 @strcmp(")
                .append(value1.type.llvmType())
                .append(" ")
                .append(value1.name())
                .append(", ")
                .append(value2.type.llvmType())
                .append(" ")
                .append(value2.name())
                .append(")\n");

        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = icmp eq i32 ")
                .append(result)
                .append(", 0\n");
        register++;
    }

    static void not_equal_i(Value value1, Value value2) {
        equal_i(value1, value2);
        CURRENT_TEXT.append("%")
                .append(register)
                .append("xor i1 %")
                .append(register - 1)
                .append(", 1\n");
        register++;
    }

    static void not_equal_f(Value value1, Value value2) {
        equal_f(value1, value2);
        CURRENT_TEXT.append("%")
                .append(register)
                .append("xor i1 %")
                .append(register - 1)
                .append(", 1\n");
        register++;
    }

    static void not_equal_s(Value value1, Value value2) {
        equal_s(value1, value2);
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = xor i1 %")
                .append(register - 1)
                .append(", 1\n");
        register++;
    }

    static void less_i(Value value1, Value value2) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = ")
                .append(value1.type.llvmComparator())
                .append(" ult ")
                .append(value2.type.llvmType())
                .append(" ")
                .append(value2.name())
                .append(", ")
                .append(value1.name())
                .append("\n");
        register++;
    }

    static void less_f(Value value1, Value value2) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = ")
                .append(value1.type.llvmComparator())
                .append(" olt ")
                .append(value2.type.llvmType())
                .append(" ")
                .append(value2.name())
                .append(", ")
                .append(value1.name())
                .append("\n");
        register++;
    }

    static void greater_i(Value value1, Value value2) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = ")
                .append(value1.type.llvmComparator())
                .append(" ugt ")
                .append(value2.type.llvmType())
                .append(" ")
                .append(value2.name())
                .append(", ")
                .append(value1.name())
                .append("\n");
        register++;
    }

    static void greater_f(Value value1, Value value2) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = ")
                .append(value1.type.llvmComparator())
                .append(" ogt ")
                .append(value2.type.llvmType())
                .append(" ")
                .append(value2.name())
                .append(", ")
                .append(value1.name())
                .append("\n");
        register++;
    }

    static Value add(Value value1, Value value2) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = ")
                .append(isFloatingPoint(value1) ? "f" : "")
                .append("add ")
                .append(value1.type.llvmType())
                .append(" ")
                .append(value1.name())
                .append(", ")
                .append(value2.name())
                .append("\n");
        register++;
        return value1.withName(String.valueOf(register - 1));
    }

    static Value sub(Value value1, Value value2) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = ")
                .append(isFloatingPoint(value1) ? "f" : "")
                .append("sub ")
                .append(value1.type.llvmType())
                .append(" ")
                .append(value2.name())
                .append(", ")
                .append(value1.name())
                .append("\n");
        register++;
        return value1.withName(String.valueOf(register - 1));
    }

    static Value mult(Value value1, Value value2) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = ")
                .append(isFloatingPoint(value1) ? "f" : "")
                .append("mul ")
                .append(value1.type.llvmType())
                .append(" ")
                .append(value1.name())
                .append(", ")
                .append(value2.name())
                .append("\n");
        register++;
        return value1.withName(String.valueOf(register - 1));
    }

    static Value div(Value value1, Value value2) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = ")
                .append(isFloatingPoint(value1) ? "f" : "s")
                .append("div ")
                .append(value1.type.llvmType())
                .append(" ")
                .append(value2.name())
                .append(", ")
                .append(value1.name())
                .append("\n");
        register++;
        return value1.withName(String.valueOf(register - 1));
    }

    private static boolean isFloatingPoint(Value value) {
        return value.type == PrimitiveType.FLOAT || value.type == PrimitiveType.DOUBLE;
    }

    static void ext(Value value) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = ")
                .append(isFloatingPoint(value) ? "fpext" : "sext")
                .append(" ")
                .append(value.type.llvmType())
                .append(" ")
                .append(value.name())
                .append(" to ")
                .append(isFloatingPoint(value) ? "double" : "i64")
                .append("\n");
        register++;
    }

    static void trunc(Value value) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = ")
                .append(isFloatingPoint(value) ? "fptrunc" : "trunc")
                .append(" ")
                .append(value.type.llvmType())
                .append(" ")
                .append(value.name())
                .append(" to ")
                .append(isFloatingPoint(value) ? "float" : "i32")
                .append("\n");
        register++;
    }

    static void sitofp(Value value, Type targetType) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = sitofp ")
                .append(value.type.llvmType())
                .append(" ")
                .append(value.name())
                .append(" to ")
                .append(targetType.llvmType())
                .append("\n");
        register++;
    }

    static void fptosi(Value value, Type targetType) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = fptosi ")
                .append(value.type.llvmType())
                .append(" ")
                .append(value.name())
                .append(" to ")
                .append(targetType.llvmType())
                .append("\n");
        register++;
    }

    static void allocate_string(String id, int length) {
        CURRENT_TEXT.append("%")
                .append(id)
                .append(" = alloca [")
                .append(length + 1)
                .append(" x i8]\n");
    }

    static void constant_string(String content) {
        final var length = content.length() + 1;
        HEADER_TEXT.append("@str")
                .append(str)
                .append(" = constant [")
                .append(length)
                .append(" x i8] c\"")
                .append(content)
                .append("\\00\"\n");
        final var id = "str" + str;
        allocate_string(id, (length - 1));
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = bitcast [")
                .append(length)
                .append(" x i8]* %")
                .append(id)
                .append(" to i8*\n");
        CURRENT_TEXT.append("call void @llvm.memcpy.p0i8.p0i8.i64(i8* align 1 %")
                .append(register)
                .append(", i8* align 1 getelementptr inbounds ([")
                .append(length)
                .append(" x i8], [")
                .append(length)
                .append(" x i8]* @")
                .append(id)
                .append(", i32 0, i32 0), i64 ")
                .append(length)
                .append(", i1 false)\n");
        register++;
        str++;
    }

    static void scanf(Value value) {
        allocate_string("str" + str, value.length);
        declare(value.name, value.type, value.isGlobal);
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = getelementptr inbounds [")
                .append(value.length + 1)
                .append(" x i8], [")
                .append(value.length + 1)
                .append(" x i8]* %str")
                .append(str)
                .append(", i64 0, i64 0\n");
        register++;
        CURRENT_TEXT.append("store i8* %")
                .append(register - 1)
                .append(", i8** ")
                .append(value.name())
                .append("\n");
        str++;
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([5 x i8], [5 x i8]* @strs, i32 0, i32 0), i8* %")
                .append(register - 1)
                .append(")\n");
        register++;
    }

    static void ret(Value value) {
        CURRENT_TEXT.append("ret ")
                .append(value.type.llvmType())
                .append(" ")
                .append(value.name())
                .append("\n");
    }

    static void defineFunction(Function function) {
        CURRENT_TEXT.append("define ")
                .append(function.returnType.llvmType())
                .append(" @")
                .append(function.name)
                .append("(");

        for (var i = 0; i < function.parameters.size(); i++) {
            final var param = function.parameters.get(i);

            CURRENT_TEXT.append(param.type.llvmType())
                    .append(param.name.equals(Parameter.THIS_PARAM_NAME) ? "*" : "")
                    .append(" %")
                    .append(param.name);

            if (i != function.parameters.size() - 1) {
                CURRENT_TEXT.append(", ");
            }
        }

        CURRENT_TEXT.append(") {\n");
    }

    static void closeFunction(Function function) {
        if (function.returnType == PrimitiveType.VOID) {
            CURRENT_TEXT.append("ret void\n");
        }

        CURRENT_TEXT.append("}\n");
    }

    static void callFunction(Function function, List<Value> args) {
        if (function.returnType != PrimitiveType.VOID) {
            CURRENT_TEXT.append("%")
                    .append(register)
                    .append(" = ");
        }

        CURRENT_TEXT.append("call ")
                .append(function.returnType.llvmType())
                .append(" @")
                .append(function.name)
                .append("(");

        for (var i = 0; i < args.size(); i++) {
            final var arg = args.get(i);
            final var isCustomType = arg.type instanceof CustomType;

            CURRENT_TEXT.append(arg.type.llvmType())
                    .append(isCustomType ? "*" : "")
                    .append(" ")
                    .append(arg.name());

            if (i != args.size() - 1) {
                CURRENT_TEXT.append(", ");
            }
        }

        CURRENT_TEXT.append(")\n");
        register++;
    }

    static void commit() {
        HEADER_TEXT.append(BUFFER_TEXT);
        BUFFER_TEXT.setLength(0);
    }

    static void setMainContext(boolean isMainContext) {
        if (isMainContext) {
            CURRENT_TEXT = MAIN_TEXT;
        } else {
            CURRENT_TEXT = BUFFER_TEXT;
        }
    }

    public static void defineClass(Clazz clazz) {
        HEADER_TEXT.append("%")
                .append(clazz.name)
                .append(" = type {\n");

        var fieldsCount = 0;

        for (final var field : clazz.fields.entrySet()) {
            HEADER_TEXT.append(field.getValue().type.llvmType())
                    .append(fieldsCount == clazz.fields.size() - 1 ? "\n" : ",\n");
            fieldsCount++;
        }

        HEADER_TEXT.append("}\n");
    }

    public static void newObject(String id, Clazz clazz) {
        CURRENT_TEXT.append("%")
                .append(id)
                .append(" = alloca %")
                .append(clazz.name)
                .append("\n");
        register++;
    }

    public static void assignField(Clazz clazz, int field, String objectId, Value value) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = getelementptr inbounds %")
                .append(clazz.name)
                .append(", %")
                .append(clazz.name)
                .append("* %")
                .append(objectId)
                .append(", i32 0, i32 ")
                .append(field)
                .append("\n");
        register++;
        CURRENT_TEXT.append("store ")
                .append(value.type.llvmType())
                .append(" ")
                .append(value.name())
                .append(", ")
                .append(value.type.llvmType())
                .append("* %")
                .append(register - 1)
                .append("\n");
    }

    public static void readField(Clazz clazz, int field, Type fieldType, String objectId) {
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = getelementptr inbounds %")
                .append(clazz.name)
                .append(", %")
                .append(clazz.name)
                .append("* %")
                .append(objectId)
                .append(", i32 0, i32 ")
                .append(field)
                .append("\n");
        register++;
        CURRENT_TEXT.append("%")
                .append(register)
                .append(" = load ")
                .append(fieldType.llvmType())
                .append(", ")
                .append(fieldType.llvmType())
                .append("* %")
                .append(register - 1)
                .append("\n");
        register++;
    }

    static String generate() {
        return "declare i32 @printf(i8*, ...)\n" +
                "declare i32 @sprintf(i8*, i8*, ...)\n" +
                "declare i8* @strcpy(i8*, i8*)\n" +
                "declare i8* @strcat(i8*, i8*)\n" +
                "declare i32 @scanf(i8*, ...)\n" +
                "declare i32 @strcmp(i8*, i8*)\n" +
                "declare void @llvm.memcpy.p0i8.p0i8.i64(i8* noalias nocapture writeonly, i8* noalias nocapture readonly, i64, i1 immarg)\n" +
                "@strps = constant [4 x i8] c\"%s\\0A\\00\"\n" +
                "@strpi = constant [4 x i8] c\"%d\\0A\\00\"\n" +
                "@strpd = constant [4 x i8] c\"%f\\0A\\00\"\n" +
                "@strs = constant [5 x i8] c\"%10s\\00\"\n" +
                "@strspi = constant [3 x i8] c\"%d\\00\"\n" +
                "@strpb = constant [4 x i8] c\"%s\\0A\\00\"\n" +
                "@truetext = constant [5 x i8] c\"true\\00\"\n" +
                "@falsetext = constant [6 x i8] c\"false\\00\"\n" +
                HEADER_TEXT +
                "define i32 @main() nounwind{\n" +
                MAIN_TEXT +
                "ret i32 0 }\n";
    }

}
