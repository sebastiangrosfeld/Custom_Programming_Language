package pl.edu.pw.ee.llvm;

import java.util.LinkedList;
import java.util.List;

public class Function {

    public String name;
    public PrimitiveType returnType;
    public List<Parameter> parameters = new LinkedList<>();

    public Function(String name, PrimitiveType returnType) {
        this.name = name;
        this.returnType = returnType;
    }

}
