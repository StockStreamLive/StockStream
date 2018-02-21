package data;

import org.junit.Test;

import static org.junit.Assert.*;

public class MarketEventTest {

    @Test
    public void testHashCode_diffObjects_expectDiff() {
        final MarketEvent marketEvent1 = new MarketEvent(MarketEvent.Status.OPEN, 1, 2);
        final MarketEvent marketEvent2 = new MarketEvent(MarketEvent.Status.OPEN, 1, 3);

        assertNotEquals(marketEvent1.hashCode(), marketEvent2.hashCode());
        assertFalse(marketEvent1.equals(marketEvent2));
    }

    @Test
    public void testHashCode_equalObjects_expectSame() {
        final MarketEvent marketEvent1 = new MarketEvent(MarketEvent.Status.CLOSE, 1, -5);
        final MarketEvent marketEvent2 = new MarketEvent(MarketEvent.Status.CLOSE, 1, -5);

        assertEquals(marketEvent1.hashCode(), marketEvent2.hashCode());
        assertTrue(marketEvent1.equals(marketEvent2));
    }

}
