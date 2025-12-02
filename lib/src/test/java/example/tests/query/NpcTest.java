package example.tests.query;

import com.kraken.api.Context;
import example.tests.BaseApiTest;

public class NpcTest extends BaseApiTest {
    @Override
    protected boolean runTest(Context ctx) throws Exception {
        return true;
    }

    @Override
    protected String getTestName() {
        return "NPC";
    }
}
