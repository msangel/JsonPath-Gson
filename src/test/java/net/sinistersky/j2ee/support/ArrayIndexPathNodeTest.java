package net.sinistersky.j2ee.support;


import java.util.List;

import com.google.gson.JsonElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ArrayIndexPathNodeTest {

    @Test
    public void test_indexes() {
        String data = "[1,2,3,4,5,6,7,8,9,10]";
        Object[][] cases = {
                {"$[0]", 1},
                {"$[9]", 10},
                {"$[10]", null},
                {"$[-1]", 10},
                {"$[-9]", 2},
                {"$[-10]", 1},
                {"$[-11]", null},
        };

        Parser parser = new Parser();
        for (Object[] aCase : cases) {
            List<JsonElement> res = parser.parseExpression("" + aCase[0]).exec(data);
            if (res.isEmpty()) {
                assertNull(aCase[1]);
            } else {
                assertEquals(Integer.valueOf("" + aCase[1]).intValue(), res.get(0).getAsInt());
            }
        }


    }

}
