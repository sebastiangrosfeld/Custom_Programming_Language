package pl.edu.pw.ee.llvm;

public class Constant extends Value {

    public Constant(String name, PrimitiveType type) {
        super(name, type, 0, false);
    }

    @Override
    public String name() {
        return name;
    }

}
