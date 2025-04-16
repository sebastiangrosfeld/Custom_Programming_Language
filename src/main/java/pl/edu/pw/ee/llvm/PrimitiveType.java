package pl.edu.pw.ee.llvm;

public enum PrimitiveType implements Type {
    INT("i32", "strpi", "0", "icmp"),
    LONG("i64", "strpi", "0", "icmp"),
    FLOAT("float", "strpd", "0.0", "fcmp"),
    DOUBLE("double", "strpd", "0.0", "fcmp"),
    STRING("i8*", "strps", "null", ""),
    BOOLEAN("i1", "strpb", "0", "icmp"),
    VOID("void", "", "", ""),
    UNKNOWN("", "", "", "");

    private final String llvmType;
    private final String llvmPrintPattern;
    private final String llvmZeroValue;
    private final String llvmComparator;

    PrimitiveType(String llvmType, String llvmPrintPattern, String llvmZeroValue, String llvmComparator) {
        this.llvmType = llvmType;
        this.llvmPrintPattern = llvmPrintPattern;
        this.llvmZeroValue = llvmZeroValue;
        this.llvmComparator = llvmComparator;
    }

    public static PrimitiveType fromKeyword(String keyword) {
        for (final var type : PrimitiveType.values()) {
            if (type.name().equalsIgnoreCase(keyword)) {
                return type;
            }
        }

        return UNKNOWN;
    }

    @Override
    public String llvmType() {
        return llvmType;
    }

    @Override
    public String llvmComparator() {
        return llvmComparator;
    }

    @Override
    public String llvmPrintPattern() {
        return llvmPrintPattern;
    }

    @Override
    public String llvmZeroValue() {
        return llvmZeroValue;
    }
}
