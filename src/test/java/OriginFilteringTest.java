import com.iambadatplaying.server.LocalServer;
import org.junit.Assert;
import org.junit.Test;

public class OriginFilteringTest {

    @Test
    public void testInternalOriginFiltering() {
        Assert.assertFalse("Allow self localhost", LocalServer.filterOrigin("http://localhost:" + LocalServer.APPLICATION_PORT + "/"));
    }
}
