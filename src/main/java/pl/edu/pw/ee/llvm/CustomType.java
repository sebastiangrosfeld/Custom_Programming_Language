package pl.edu.pw.ee.llvm;

import java.util.Objects;

public class CustomType implements Type {

    public final String name;

    public CustomType(String name) {
        this.name = name;
    }

    @Override
    public String llvmType() {
        return "%" + name;
    }

    @Override
    public String llvmComparator() {
        return "";
    }

    @Override
    public String llvmPrintPattern() {
        return "";
    }

    @Override
    public String llvmZeroValue() {
        return "null";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CustomType that)) {
            return false;
        }

        return Objects.equals(name, that.name);
    }

}
