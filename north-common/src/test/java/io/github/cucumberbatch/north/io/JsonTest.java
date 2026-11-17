package io.github.cucumberbatch.north.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonTest {

    @Test
    void jsonIsParsedAndWritten() {
        Map<String, Object> parsed = Json.parseObject("{\"type\":\"button\",\"id\":\"a\",\"pressed\":true}");
        Assertions.assertEquals("button", Json.getString(parsed, "type", null));
        Assertions.assertEquals("a", Json.getString(parsed, "id", null));
        Assertions.assertTrue(Json.getBoolean(parsed, "pressed", false));

        Assertions.assertEquals("{\"a\":1,\"b\":[true,null]}", Json.write(parseNested()));
    }

    private java.util.Map<String, Object> parseNested() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("a", 1);
        List<Object> list = new ArrayList<>();
        list.add(Boolean.TRUE);
        list.add(null);
        root.put("b", list);
        return root;
    }
}
