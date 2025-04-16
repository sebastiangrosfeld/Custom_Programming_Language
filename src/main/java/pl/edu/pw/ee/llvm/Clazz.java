package pl.edu.pw.ee.llvm;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Clazz {

    public String name;
    public Map<String, Value> fields = new LinkedHashMap<>();
    public List<Function> methods = new LinkedList<>();

    public Clazz(String name) {
        this.name = name;
    }

    public int getFieldOffsetNumber(String fieldName) {
        var offset = 0;

        for (final var entry : fields.entrySet()) {
            if (entry.getKey().equals(fieldName)) {
                return offset;
            }

            offset++;
        }

        return -1;
    }

}
