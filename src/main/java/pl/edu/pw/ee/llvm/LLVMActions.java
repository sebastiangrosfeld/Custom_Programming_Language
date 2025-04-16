package pl.edu.pw.ee.llvm;

import org.antlr.v4.runtime.ParserRuleContext;
import pl.edu.pw.ee.HolyJavaBaseListener;
import pl.edu.pw.ee.HolyJavaParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

class LLVMActions extends HolyJavaBaseListener {

    private static final Path OUTPUT_FILE_PATH = Path.of("output.ll");
    private static final int BUFFER_SIZE = 128;
    private final Map<String, Value> globalVariables = new HashMap<>();
    private final Map<String, Value> localVariables = new HashMap<>();
    private final Map<String, Function> functions = new HashMap<>();
    private final Map<String, Clazz> classes = new HashMap<>();
    private final Stack<Value> stack = new Stack<>();
    private final Stack<Array> arrayStack = new Stack<>();
    private final Stack<Matrix> matrixStack = new Stack<>();
    private final Stack<String> localLoopStack = new Stack<>();
    private final Stack<String> localIfStack = new Stack<>();
    private Function currentFunction;
    private Clazz currentClass;
    private boolean isGlobalContext = true;

    @Override
    public void exitProgramme(HolyJavaParser.ProgrammeContext context) {
        try {
            final var llvmCode = LLVMGenerator.generate();
            Files.writeString(OUTPUT_FILE_PATH, llvmCode);
        } catch (IOException ioException) {
            throw new IllegalStateException(ioException);
        }
    }

    @Override
    public void exitAssignmatrix(HolyJavaParser.AssignmatrixContext context) {
        final var ID = context.ID().getText();
        final var matrix = (Matrix) getVariable(ID, context);

        if (matrix == null) {
            error(context.getStart().getLine(), "unknown matrix " + ID);
        }

        final var value = stack.pop();
        final var columnIndex = stack.pop();
        final var rowIndex = stack.pop();

        if (rowIndex.type != PrimitiveType.INT && rowIndex.type != PrimitiveType.LONG) {
            error(context.getStart().getLine(), "matrix row index must be int or long");
        }

        if (columnIndex.type != PrimitiveType.INT && columnIndex.type != PrimitiveType.LONG) {
            error(context.getStart().getLine(), "matrix column index must be int or long");
        }

        if (rowIndex.type == PrimitiveType.INT) {
            LLVMGenerator.ext(rowIndex);
        }

        if (columnIndex.type == PrimitiveType.INT) {
            LLVMGenerator.ext(columnIndex);
        }

        if (value.type != matrix.type) {
            error(context.getStart().getLine(), "matrix type mismatch");
        }

        LLVMGenerator.assign_matrix_item(matrix, rowIndex.name(), columnIndex.name(), value);
    }

    @Override
    public void exitAssignarray(HolyJavaParser.AssignarrayContext context) {
        final var ID = context.ID().getText();
        final var array = (Array) getVariable(ID, context);

        if (array == null) {
            error(context.getStart().getLine(), "unknown array " + ID);
        }

        final var value = stack.pop();
        final var index = stack.pop();

        if (index.type != PrimitiveType.INT && index.type != PrimitiveType.LONG) {
            error(context.getStart().getLine(), "array index must be int or long");
        }

        if (index.type == PrimitiveType.INT) {
            final var subIndex = index.name().substring(0, index.name().length() - 1);
            final var subIndexValue = Integer.parseInt(subIndex);

            if (subIndexValue > (array.length - 1) || subIndexValue < 0) {
                error(context.getStart().getLine(), "array index out of range");
            }

            LLVMGenerator.ext(index);
        }

        final var indexValue = Integer.parseInt(index.name());

        if (indexValue > (array.length - 1) || indexValue < 0) {
            error(context.getStart().getLine(), "array index out of range");
        }

        if (value.type != array.type) {
            error(context.getStart().getLine(), "array type mismatch");
        }

        LLVMGenerator.assign_array_item(array, index.name(), value);
    }

    @Override
    public void exitAssign(HolyJavaParser.AssignContext context) {
        final var ID = context.ID().getText();
        final var variable = stack.pop();

        if (isVariableUndefined(ID)) {
            setVariable(ID, variable);
            LLVMGenerator.declare(ID, variable.type, isGlobalContext);
        }

        LLVMGenerator.assign(ID, isGlobalContext, variable);
    }

    @Override
    public void enterMatrix(HolyJavaParser.MatrixContext context) {
        final var id = "mat" + (LLVMGenerator.mat - 1);
        final var matrix = new Matrix(id, PrimitiveType.UNKNOWN, 0, isGlobalContext);
        matrixStack.push(matrix);
        LLVMGenerator.mat++;
    }

    @Override
    public void exitMatrix(HolyJavaParser.MatrixContext context) {
        final var matrix = matrixStack.pop();
        LLVMGenerator.declare(matrix);

        for (var index = 0; index < matrix.length; index++) {
            final var row = matrix.rows.get(index);
            LLVMGenerator.assign_matrix_row(matrix, String.valueOf(index), row);
        }

        final var id = context.ID().getText();
        setVariable(id, matrix);
    }

    @Override
    public void exitArray(HolyJavaParser.ArrayContext context) {
        final var id = context.ID().getText();
        final var array = stack.pop();
        setVariable(id, array);
    }

    @Override
    public void exitPrint(HolyJavaParser.PrintContext context) {
        final var ID = context.ID().getText();

        if (isVariableUndefined(ID)) {
            error(context.getStart().getLine(), "unknown variable");
        }

        final var value = getVariable(ID, context);

        if (value.type == PrimitiveType.STRING) {
            final var loadedValue = LLVMGenerator.load(ID, value, isIdGlobal(ID, context));
            LLVMGenerator.printf(loadedValue);
            return;
        }

        LLVMGenerator.printf(value);
    }

    @Override
    public void exitRead(HolyJavaParser.ReadContext context) {
        final var ID = context.ID().getText();
        final var value = new Value(ID, PrimitiveType.STRING, BUFFER_SIZE - 1, isGlobalContext);
        setVariable(ID, value);
        LLVMGenerator.scanf(value);
    }

    @Override
    public void exitReturn(HolyJavaParser.ReturnContext context) {
        final var value = stack.pop();
        final var returnType = currentFunction.returnType;

        if (returnType == PrimitiveType.VOID) {
            error(context.getStart().getLine(), "void function cannot return a value");
        }

        if (value.type != returnType) {
            error(context.getStart().getLine(), "return type mismatch");
        }

        LLVMGenerator.ret(value);
    }

    @Override
    public void exitAssignfield(HolyJavaParser.AssignfieldContext context) {
        final var objectID = context.ID().getFirst().getText();
        final var fieldID = context.ID().getLast().getText();
        final var object = getVariable(objectID, context);

        final var clazz = classes.get(((CustomType) object.type).name);

        final var value = stack.pop();

        var field = clazz.getFieldOffsetNumber(fieldID);

        if (field == -1) {
            error(context.getStart().getLine(), "unknown field " + fieldID);
        }

        var fieldType = clazz.fields.get(fieldID).type;

        if (value.type != fieldType) {
            error(context.getStart().getLine(), "field type mismatch");
        }

        LLVMGenerator.assignField(clazz, field, objectID, value);
    }

    @Override
    public void exitNewobject(HolyJavaParser.NewobjectContext context) {
        final var id = context.ID().getFirst().getText();
        final var classId = context.ID().getLast().getText();
        final var clazz = classes.get(classId);

        if (clazz == null) {
            error(context.getStart().getLine(), "unknown class " + classId);
        }

        LLVMGenerator.newObject(id, clazz);
        setVariable(id, new Value(id, new CustomType(classId), 0, false));
    }

    @Override
    public void enterWhiledef(HolyJavaParser.WhiledefContext context) {
        var val = localLoopStack.pop();
        localLoopStack.push(val);
        var condition = stack.pop();
        if (condition.type != PrimitiveType.BOOLEAN) {
            error(context.getStart().getLine(), "Boolean type condition mismatch");
        }
        LLVMGenerator.write_loop_start_label();
        LLVMGenerator.evaluate_loop_condition(condition.name);

    }

    @Override
    public void exitWhiledef(HolyJavaParser.WhiledefContext context) {
        String id = localLoopStack.pop();
        LLVMGenerator.load(id, new Value(String.valueOf(LLVMGenerator.register - 1), PrimitiveType.BOOLEAN), true);
        LLVMGenerator.evaluate_loop();
        LLVMGenerator.write_loop_end_label(); // End of the loop
    }

    @Override
    public void enterIfdef(HolyJavaParser.IfdefContext context) {
        var value = localIfStack.pop();
        localIfStack.push(value);
        var condition = stack.pop();

        if (condition.type != PrimitiveType.BOOLEAN) {
            error(context.getStart().getLine(), "Boolean type condition mismatch");
        }
        LLVMGenerator.write_if_start_label();
        LLVMGenerator.evaluate_if_condition(condition.name);
    }

    @Override
    public void exitIfdef(HolyJavaParser.IfdefContext context) {
        LLVMGenerator.jump_to_if_end();
        LLVMGenerator.write_if_end_label();
    }

    @Override
    public void enterElsedef(HolyJavaParser.ElsedefContext context) {
        var id = localIfStack.pop();
        LLVMGenerator.load(id, new Value(String.valueOf(LLVMGenerator.register - 1), PrimitiveType.BOOLEAN), true);
        LLVMGenerator.write_else_start();
        LLVMGenerator.evaluate_else();
    }

    @Override
    public void exitElsedef(HolyJavaParser.ElsedefContext context) {
        LLVMGenerator.jump_to_else_end();
        LLVMGenerator.write_else_end_label();
    }

    @Override
    public void exitIfcond(HolyJavaParser.IfcondContext context) {
        var id = context.getText();
        localIfStack.push(id);
    }

    @Override
    public void exitLoopcond(HolyJavaParser.LoopcondContext context) {
        var id = context.getText();
        localLoopStack.push(id);
    }

    @Override
    public void enterClassdef(HolyJavaParser.ClassdefContext context) {
        final var id = context.ID().getText();
        final var clazz = new Clazz(id);
        currentClass = clazz;
        classes.put(id, clazz);
    }

    @Override
    public void exitClassdef(HolyJavaParser.ClassdefContext context) {
        final var id = context.ID().getText();
        final var clazz = classes.get(id);
        LLVMGenerator.defineClass(clazz);
        LLVMGenerator.commit();
        currentClass = null;
    }

    @Override
    public void exitMethoddef(HolyJavaParser.MethoddefContext context) {
        currentClass.methods.add(currentFunction);
        currentFunction = null;
    }

    @Override
    public void exitFielddef(HolyJavaParser.FielddefContext context) {
        final var id = context.ID().getText();
        final var typeKeyword = context.type().getText();
        final var type = PrimitiveType.fromKeyword(typeKeyword);
        final var field = new Value(id, type);
        currentClass.fields.put(id, field);
    }

    @Override
    public void enterFundef(HolyJavaParser.FundefContext context) {
        isGlobalContext = false;
        LLVMGenerator.setMainContext(false);
    }

    @Override
    public void exitFundef(HolyJavaParser.FundefContext context) {
        LLVMGenerator.closeFunction(currentFunction);
        isGlobalContext = true;

        if (currentClass == null) {
            LLVMGenerator.commit();
        }

        LLVMGenerator.setMainContext(true);
        localVariables.clear();

        if (context.getParent() instanceof HolyJavaParser.MethoddefContext) {
            return;
        }

        currentFunction = null;
    }

    @Override
    public void enterFundefheader(HolyJavaParser.FundefheaderContext context) {
        final var id = context.ID().getText();
        final var returnTypeKeyword = context.type() == null ? context.VOID().getText() : context.type().getText();
        final var returnType = PrimitiveType.fromKeyword(returnTypeKeyword);
        final var function = currentClass == null ? new Function(id, returnType) : new Function(currentClass.name + "_" + id, returnType);
        functions.put(currentClass == null ? id : currentClass.name + "_" + id, function);
        currentFunction = function;

        if (currentClass != null) {
            final var thisParameter = new Parameter(Parameter.THIS_PARAM_NAME, new CustomType(currentClass.name));
            currentFunction.parameters.add(thisParameter);
            setVariable(Parameter.THIS_PARAM_NAME, thisParameter);
        }
    }

    @Override
    public void exitFundefheader(HolyJavaParser.FundefheaderContext context) {
        LLVMGenerator.defineFunction(currentFunction);
    }

    @Override
    public void exitParamdef(HolyJavaParser.ParamdefContext context) {
        final var id = context.ID().getText();
        final var typeKeyword = context.type().getText();
        final var type = PrimitiveType.fromKeyword(typeKeyword);
        final var parameter = new Parameter(id, type);
        currentFunction.parameters.add(parameter);
        setVariable(id, parameter);
    }

    @Override
    public void exitMethodcall(HolyJavaParser.MethodcallContext context) {
        final var objectID = context.ID().getFirst().getText();
        final var methodID = context.ID().getLast().getText();
        final var object = getVariable(objectID, context);

        final var clazz = classes.get(((CustomType) object.type).name);
        final var method = functions.get(clazz.name + "_" + methodID);

        if (method == null) {
            error(context.getStart().getLine(), "unknown method " + methodID);
        }

        if (method.parameters.size() != context.expr0().size() + 1) {
            error(context.getStart().getLine(), "method parameter count mismatch");
        }

        final List<Value> arguments = new LinkedList<>();

        for (var i = 0; i < method.parameters.size(); i++) {
            final var parameter = method.parameters.get(i);

            if (parameter.name.equals(Parameter.THIS_PARAM_NAME)) {
                if (!object.type.equals(parameter.type)) {
                    error(context.getStart().getLine(), "method parameter type mismatch");
                }

                arguments.add(object);
                continue;
            }

            final var argument = stack.pop();

            if (parameter.type != argument.type) {
                error(context.getStart().getLine(), "method parameter type mismatch");
            }

            arguments.add(argument);
        }

        LLVMGenerator.callFunction(method, arguments);

        if (method.returnType != PrimitiveType.VOID) {
            final var result = new Value(String.valueOf(LLVMGenerator.register - 1), method.returnType);
            stack.push(result);
        }
    }

    @Override
    public void exitFuncall(HolyJavaParser.FuncallContext context) {
        final var ID = context.ID().getText();
        final var function = functions.get(ID);

        if (function == null) {
            error(context.getStart().getLine(), "unknown function " + ID);
        }

        if (function.parameters.size() != context.expr0().size()) {
            error(context.getStart().getLine(), "function parameter count mismatch");
        }

        final List<Value> arguments = new LinkedList<>();

        for (var i = 0; i < function.parameters.size(); i++) {
            final var parameter = function.parameters.get(i);
            final var argument = stack.pop();

            if (parameter.type != argument.type) {
                error(context.getStart().getLine(), "function parameter type mismatch");
            }

            arguments.add(argument);
        }

        LLVMGenerator.callFunction(function, arguments);

        if (function.returnType != PrimitiveType.VOID) {
            final var result = new Value(String.valueOf(LLVMGenerator.register - 1), function.returnType);
            stack.push(result);
        }
    }

    @Override
    public void exitMatrixitem(HolyJavaParser.MatrixitemContext context) {
        final var array = (Array) stack.pop();
        final var matrix = matrixStack.peek();
        matrix.rows.add(array);
        matrix.length++;

        if (matrix.type == PrimitiveType.UNKNOWN) {
            matrix.type = array.type;
            matrix.rowLength = array.length;
            return;
        }

        if (matrix.rowLength != array.length) {
            error(context.getStart().getLine(), "matrix row length mismatch");
        }

        if (matrix.type != array.type) {
            error(context.getStart().getLine(), "matrix type mismatch");
        }
    }

    @Override
    public void enterArraydef(HolyJavaParser.ArraydefContext context) {
        final var id = "arr" + (LLVMGenerator.arr - 1);
        final var array = new Array(id, PrimitiveType.UNKNOWN, 0, isGlobalContext);
        arrayStack.push(array);
        LLVMGenerator.arr++;
    }

    @Override
    public void exitArraydef(HolyJavaParser.ArraydefContext context) {
        final var array = arrayStack.pop();
        LLVMGenerator.declare(array);

        for (var index = 0; index < array.length; index++) {
            final var value = array.values.get(index);
            LLVMGenerator.assign_array_item(array, String.valueOf(index), value);
        }

        stack.push(array);
    }

    @Override
    public void exitArrayitem(HolyJavaParser.ArrayitemContext context) {
        final var value = stack.pop();
        final var array = arrayStack.peek();
        array.values.add(value);
        array.length++;

        if (array.type == PrimitiveType.UNKNOWN) {
            array.type = value.type;
            return;
        }

        if (array.type != value.type) {
            error(context.getStart().getLine(), "array type mismatch");
        }
    }

    @Override
    public void exitOr(HolyJavaParser.OrContext context) {
        final var value1 = stack.pop();
        final var value2 = stack.pop();

        if (value1.type != PrimitiveType.BOOLEAN || value2.type != PrimitiveType.BOOLEAN) {
            error(context.getStart().getLine(), "OR type mismatch");
        }

        LLVMGenerator.or(value2, value1);
        stack.push(new Value(String.valueOf(LLVMGenerator.register - 1), PrimitiveType.BOOLEAN));
    }

    @Override
    public void exitLess(HolyJavaParser.LessContext context) {
        final var value1 = stack.pop();
        final var value2 = stack.pop();

        if (!value1.type.equals(value2.type)) {
            error(context.getStart().getLine(), "LESS type mismatch");
        }

        if (value1.type == PrimitiveType.STRING || value1.type == PrimitiveType.BOOLEAN) {
            error(context.getStart().getLine(), "LESS type invalid");
        }

        if (value1.type == PrimitiveType.INT || value1.type == PrimitiveType.LONG) {
            LLVMGenerator.less_i(value1, value2);
        }

        if (value1.type == PrimitiveType.DOUBLE || value1.type == PrimitiveType.FLOAT) {
            LLVMGenerator.less_f(value1, value2);
        }

        stack.push(new Value(String.valueOf(LLVMGenerator.register - 1), PrimitiveType.BOOLEAN));
    }

    @Override
    public void exitGreater(HolyJavaParser.GreaterContext context) {
        final var value1 = stack.pop();
        final var value2 = stack.pop();

        if (!value1.type.equals(value2.type)) {
            error(context.getStart().getLine(), "GRATER type mismatch");
        }

        if (value1.type == PrimitiveType.STRING || value1.type == PrimitiveType.BOOLEAN) {
            error(context.getStart().getLine(), "GREATER type invalid");
        }

        if (value1.type == PrimitiveType.INT || value1.type == PrimitiveType.LONG) {
            LLVMGenerator.greater_i(value1, value2);
        }

        if (value1.type == PrimitiveType.DOUBLE || value1.type == PrimitiveType.FLOAT) {
            LLVMGenerator.greater_f(value1, value2);
        }

        stack.push(new Value(String.valueOf(LLVMGenerator.register - 1), PrimitiveType.BOOLEAN));
    }

    @Override
    public void exitEqual(HolyJavaParser.EqualContext context) {
        final var value1 = stack.pop();
        final var value2 = stack.pop();

        if (!value1.type.equals(value2.type)) {
            error(context.getStart().getLine(), "EQUAL type mismatch");
        }

        if (value1.type == PrimitiveType.INT || value1.type == PrimitiveType.BOOLEAN || value1.type == PrimitiveType.LONG) {
            LLVMGenerator.equal_i(value1, value2);
        }

        if (value1.type == PrimitiveType.DOUBLE || value1.type == PrimitiveType.FLOAT) {
            LLVMGenerator.equal_f(value1, value2);
        }

        if (value1.type == PrimitiveType.STRING) {
            LLVMGenerator.equal_s(value1, value2);
        }

        stack.push(new Value(String.valueOf(LLVMGenerator.register - 1), PrimitiveType.BOOLEAN));
    }

    @Override
    public void exitNotequal(HolyJavaParser.NotequalContext context) {
        final var value1 = stack.pop();
        final var value2 = stack.pop();

        if (!value1.type.equals(value2.type)) {
            error(context.getStart().getLine(), "NOT_EQUAL type mismatch");
        }

        if (value1.type == PrimitiveType.INT || value1.type == PrimitiveType.BOOLEAN || value1.type == PrimitiveType.LONG) {
            LLVMGenerator.not_equal_i(value1, value2);
        }

        if (value1.type == PrimitiveType.DOUBLE || value1.type == PrimitiveType.FLOAT) {
            LLVMGenerator.not_equal_f(value1, value2);
        }

        if (value1.type == PrimitiveType.STRING) {
            LLVMGenerator.not_equal_s(value1, value2);
        }

        stack.push(new Value(String.valueOf(LLVMGenerator.register - 1), PrimitiveType.BOOLEAN));
    }

    @Override
    public void exitXor(HolyJavaParser.XorContext context) {
        final var value1 = stack.pop();
        final var value2 = stack.pop();

        if (value1.type != PrimitiveType.BOOLEAN || value2.type != PrimitiveType.BOOLEAN) {
            error(context.getStart().getLine(), "XOR type mismatch");
        }

        LLVMGenerator.xor(value1, value2);
        stack.push(new Value(String.valueOf(LLVMGenerator.register - 1), PrimitiveType.BOOLEAN));
    }

    @Override
    public void exitAnd(HolyJavaParser.AndContext context) {
        final var value1 = stack.pop();
        final var value2 = stack.pop();

        if (value1.type != PrimitiveType.BOOLEAN || value2.type != PrimitiveType.BOOLEAN) {
            error(context.getStart().getLine(), "AND type mismatch");
        }

        LLVMGenerator.and(value2, value1);
        stack.push(new Value(String.valueOf(LLVMGenerator.register - 1), PrimitiveType.BOOLEAN));
    }

    @Override
    public void exitAdd(HolyJavaParser.AddContext context) {
        final var value1 = stack.pop();
        final var value2 = stack.pop();

        if (!value1.type.equals(value2.type)) {
            error(context.getStart().getLine(), "add type mismatch");
        }

        final var result = LLVMGenerator.add(value1, value2);
        stack.push(result);
    }

    @Override
    public void exitSub(HolyJavaParser.SubContext context) {
        final var value1 = stack.pop();
        final var value2 = stack.pop();

        if (!value1.type.equals(value2.type)) {
            error(context.getStart().getLine(), "sub type mismatch");
        }

        final var result = LLVMGenerator.sub(value1, value2);
        stack.push(result);
    }

    @Override
    public void exitDiv(HolyJavaParser.DivContext context) {
        final var value1 = stack.pop();
        final var value2 = stack.pop();

        if (!value1.type.equals(value2.type)) {
            error(context.getStart().getLine(), "div type mismatch");
        }

        final var result = LLVMGenerator.div(value1, value2);
        stack.push(result);
    }

    @Override
    public void exitMult(HolyJavaParser.MultContext context) {
        final var value1 = stack.pop();
        final var value2 = stack.pop();

        if (!value1.type.equals(value2.type)) {
            error(context.getStart().getLine(), "mult type mismatch");
        }

        final var result = LLVMGenerator.mult(value1, value2);
        stack.push(result);
    }

    @Override
    public void exitNeg(HolyJavaParser.NegContext context) {
        final var value = stack.pop();

        if (value.type != PrimitiveType.BOOLEAN) {
            error(context.getStart().getLine(), "NEG type mismatch");
        }

        LLVMGenerator.neg(value);
        stack.push(new Value(String.valueOf(LLVMGenerator.register - 1), PrimitiveType.BOOLEAN));
    }

    @Override
    public void exitTofloat(HolyJavaParser.TofloatContext context) {
        final var value = stack.pop();

        if (value.type == PrimitiveType.DOUBLE) {
            return;
        }

        if (!(value.type instanceof PrimitiveType)) {
            error(context.getStart().getLine(), "type mismatch");
        }

        switch (value.type) {
            case PrimitiveType.INT, PrimitiveType.LONG -> LLVMGenerator.sitofp(value, PrimitiveType.FLOAT);
            case PrimitiveType.FLOAT -> LLVMGenerator.trunc(value);
            default -> error(context.getStart().getLine(), "type mismatch");
        }

        stack.push(new Value(String.valueOf(LLVMGenerator.register - 1), PrimitiveType.FLOAT));
    }

    @Override
    public void exitToint(HolyJavaParser.TointContext context) {
        final var value = stack.pop();

        if (value.type == PrimitiveType.INT) {
            return;
        }

        if (!(value.type instanceof PrimitiveType)) {
            error(context.getStart().getLine(), "type mismatch");
        }

        switch (value.type) {
            case PrimitiveType.LONG -> LLVMGenerator.trunc(value);
            case PrimitiveType.FLOAT, PrimitiveType.DOUBLE -> LLVMGenerator.fptosi(value, PrimitiveType.INT);
            default -> error(context.getStart().getLine(), "type mismatch");
        }

        stack.push(new Value(String.valueOf(LLVMGenerator.register - 1), PrimitiveType.INT));
    }

    @Override
    public void exitTolong(HolyJavaParser.TolongContext context) {
        final var value = stack.pop();

        if (value.type == PrimitiveType.LONG) {
            return;
        }

        if (!(value.type instanceof PrimitiveType)) {
            error(context.getStart().getLine(), "type mismatch");
        }

        switch (value.type) {
            case PrimitiveType.INT -> LLVMGenerator.ext(value);
            case PrimitiveType.FLOAT, PrimitiveType.DOUBLE -> LLVMGenerator.fptosi(value, PrimitiveType.LONG);
            default -> error(context.getStart().getLine(), "type mismatch");
        }

        stack.push(new Value(String.valueOf(LLVMGenerator.register - 1), PrimitiveType.LONG));
    }

    @Override
    public void exitTodouble(HolyJavaParser.TodoubleContext context) {
        final var value = stack.pop();

        if (value.type == PrimitiveType.DOUBLE) {
            return;
        }

        if (!(value.type instanceof PrimitiveType)) {
            error(context.getStart().getLine(), "type mismatch");
        }

        switch (value.type) {
            case PrimitiveType.INT, PrimitiveType.LONG -> LLVMGenerator.sitofp(value, PrimitiveType.DOUBLE);
            case PrimitiveType.FLOAT -> LLVMGenerator.ext(value);
            default -> error(context.getStart().getLine(), "type mismatch");
        }

        stack.push(new Value(String.valueOf(LLVMGenerator.register - 1), PrimitiveType.DOUBLE));
    }

    @Override
    public void exitReadfield(HolyJavaParser.ReadfieldContext context) {
        final var objectID = context.ID().getFirst().getText();
        final var fieldID = context.ID().getLast().getText();
        final var object = getVariable(objectID, context);

        final var clazz = classes.get(((CustomType) object.type).name);

        final var field = clazz.getFieldOffsetNumber(fieldID);

        if (field == -1) {
            error(context.getStart().getLine(), "unknown field " + fieldID);
        }

        final var fieldType = clazz.fields.get(fieldID).type;

        LLVMGenerator.readField(clazz, field, fieldType, objectID);

        final var value = new Value(String.valueOf(LLVMGenerator.register - 1), fieldType);
        stack.push(value);
    }

    @Override
    public void exitMatrixvalue(HolyJavaParser.MatrixvalueContext context) {
        final var matrix = (Matrix) getVariable(context.ID().getText(), context);

        if (matrix == null) {
            error(context.getStart().getLine(), "unknown matrix " + context.ID().getText());
        }

        final var columnIndex = stack.pop();
        final var rowIndex = stack.pop();

        if (rowIndex.type != PrimitiveType.INT && rowIndex.type != PrimitiveType.LONG) {
            error(context.getStart().getLine(), "matrix row index must be int or long");
        }

        if (columnIndex.type != PrimitiveType.INT && columnIndex.type != PrimitiveType.LONG) {
            error(context.getStart().getLine(), "matrix column index must be int or long");
        }

        if (rowIndex.type == PrimitiveType.INT) {
            LLVMGenerator.ext(rowIndex);
        }

        if (columnIndex.type == PrimitiveType.INT) {
            LLVMGenerator.ext(columnIndex);
        }

        LLVMGenerator.load_matrix_value(matrix, rowIndex.name(), columnIndex.name());
        stack.push(new Value(String.valueOf(LLVMGenerator.register - 1), matrix.type));
    }

    @Override
    public void exitArrayvalue(HolyJavaParser.ArrayvalueContext context) {
        final var array = (Array) getVariable(context.ID().getText(), context);

        if (array == null) {
            error(context.getStart().getLine(), "unknown array " + context.ID().getText());
        }

        final var index = stack.pop();

        if (index.type != PrimitiveType.INT && index.type != PrimitiveType.LONG) {
            error(context.getStart().getLine(), "array index must be int or long");
        }

        if (index.type == PrimitiveType.INT) {
            final var subIndex = index.name().substring(0, index.name().length() - 1);
            final var subIndexValue = Integer.parseInt(subIndex);

            if (subIndexValue > (array.length - 1) || subIndexValue < 0) {
                error(context.getStart().getLine(), "array index out of range");
            }

            LLVMGenerator.ext(index);
        }

        final var indexValue = Integer.parseInt(index.name());

        if (indexValue > (array.length - 1) || indexValue < 0) {
            error(context.getStart().getLine(), "array index out of range");
        }

        LLVMGenerator.load_array_value(array, index.name());
        stack.push(new Value(String.valueOf(LLVMGenerator.register - 1), array.type));
    }

    @Override
    public void exitId(HolyJavaParser.IdContext context) {
        final var ID = context.ID().getText();

        if (isVariableUndefined(ID)) {
            error(context.getStart().getLine(), "unknown variable " + ID);
        }

        final var value = getVariable(ID, context);

        if (value instanceof Parameter) {
            stack.push(value);
            return;
        }

        final var result = LLVMGenerator.load(ID, value, isIdGlobal(ID, context));
        stack.push(result);
    }

    @Override
    public void exitFloat(HolyJavaParser.FloatContext context) {
        final var text = context.FLOAT().getText();
        final var id = text.substring(0, text.length() - 1);
        stack.push(new Constant(id, PrimitiveType.FLOAT));
    }

    @Override
    public void exitInt(HolyJavaParser.IntContext context) {
        final var text = context.INT().getText();
        final var id = text.substring(0, text.length() - 1);
        stack.push(new Constant(id, PrimitiveType.INT));
    }

    @Override
    public void exitLong(HolyJavaParser.LongContext context) {
        stack.push(new Constant(context.LONG().getText(), PrimitiveType.LONG));
    }

    @Override
    public void exitDouble(HolyJavaParser.DoubleContext context) {
        stack.push(new Constant(context.DOUBLE().getText(), PrimitiveType.DOUBLE));
    }

    @Override
    public void exitString(HolyJavaParser.StringContext context) {
        final var tmp = context.STRING().getText();
        final var content = tmp.substring(1, tmp.length() - 1);
        LLVMGenerator.constant_string(content);
        final var id = "str" + (LLVMGenerator.str - 1);
        stack.push(new Value(id, PrimitiveType.STRING, content.length(), isGlobalContext));
    }

    @Override
    public void exitBool(HolyJavaParser.BoolContext context) {
        if (Objects.equals(context.BOOL().getText(), "true")) {
            stack.push(new Constant("1", PrimitiveType.BOOLEAN));
        } else {
            stack.push(new Constant("0", PrimitiveType.BOOLEAN));
        }
    }

    private Value getVariable(String id, ParserRuleContext context) {
        if (isGlobalContext) {
            final var variable = globalVariables.get(id);

            if (variable != null) {
                return variable;
            }
        }

        final var variable = localVariables.get(id);

        if (variable == null) {
            final var globalVariable = globalVariables.get(id);

            if (globalVariable != null) {
                return globalVariable;
            }

            error(context.getStart().getLine(), "unknown variable " + id);
        }

        return variable;
    }

    private void setVariable(String id, Value value) {
        if (isGlobalContext) {
            globalVariables.put(id, value);
        } else {
            localVariables.put(id, value);
        }
    }

    private boolean isVariableUndefined(String id) {
        if (isGlobalContext) {
            return !globalVariables.containsKey(id);
        }

        return !localVariables.containsKey(id) && !globalVariables.containsKey(id);
    }

    private boolean isIdGlobal(String id, ParserRuleContext context) {
        if (globalVariables.containsKey(id) && isGlobalContext) {
            return true;
        }

        if (localVariables.containsKey(id)) {
            return false;
        }

        if (globalVariables.containsKey(id)) {
            return true;
        }

        error(context.getStart().getLine(), "unknown variable " + id);
        return false;
    }

    private void error(int line, String message) {
        final var errorMessage = "Error, line " + line + ", " + message;
        throw new IllegalStateException(errorMessage);
    }

}
