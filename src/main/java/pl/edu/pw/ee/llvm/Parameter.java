package pl.edu.pw.ee.llvm;

public class Parameter extends Value {

    public static final String THIS_PARAM_NAME = "this";

    public Parameter(String name, Type type) {
        super(name, type);
        isGlobal = false;
    }

}
