package pl.edu.pw.ee.llvm;

import java.util.LinkedList;
import java.util.List;

public class Matrix extends Value {

    public List<Array> rows = new LinkedList<>();
    public int rowLength = 0;

    public Matrix(String name, PrimitiveType type, int length, boolean isGlobal) {
        super(name, type, length, isGlobal);
    }

}
