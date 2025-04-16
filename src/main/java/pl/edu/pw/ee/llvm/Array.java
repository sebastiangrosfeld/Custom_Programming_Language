package pl.edu.pw.ee.llvm;

import java.util.LinkedList;
import java.util.List;

public class Array extends Value {

    public List<Value> values = new LinkedList<>();

    public Array(String name, PrimitiveType type, int length, boolean isGlobal) {
        super(name, type, length, isGlobal);
    }

}
