package pl.edu.pw.ee;

import pl.edu.pw.ee.llvm.LLVMCompiler;

public class Main {

    public static void main(String[] args) throws Exception {
        final var compiler = new LLVMCompiler();
        compiler.compile("test.hj");
    }

}

