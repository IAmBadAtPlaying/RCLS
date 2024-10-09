import com.iambadatplaying.Starter;
import org.junit.Assert;
import org.junit.Test;


public class BasicReleaseTest {
    @Test
    public void ensureIsDevIsFalse() {
        Assert.assertFalse(Starter.isDev);
    }
}
