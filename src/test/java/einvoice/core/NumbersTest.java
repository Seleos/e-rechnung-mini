package einvoice.core;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Tests reading and printing numbers in German notation. */
class NumbersTest {
    @Test
    void parsesGermanAndPlainNotation() {
        assertEquals(new BigDecimal("6.75"), Numbers.parse("6,75"));
        assertEquals(new BigDecimal("1234.56"), Numbers.parse("1.234,56"));
        assertEquals(new BigDecimal("6.75"), Numbers.parse("6.75"));
        assertEquals(new BigDecimal("70"), Numbers.parse(" 70 "));
        assertEquals(new BigDecimal("1234.56"), Numbers.parse("1,234.56"));
        assertEquals(new BigDecimal("1.5"), Numbers.parse("1,5"));
        assertEquals(new BigDecimal("70.00"), Numbers.parse("70,00 €"));
    }

    @Test
    void returnsNullForTextThatIsNotANumber() {
        assertNull(Numbers.parse(null));
        assertNull(Numbers.parse(""));
        assertNull(Numbers.parse("   "));
        assertNull(Numbers.parse("abc"));
        assertNull(Numbers.parse("1,2,3"));
    }

    @Test
    void formatsAmountsWithTwoDecimalsAndGrouping() {
        assertEquals("1.234,56", Numbers.amount(new BigDecimal("1234.56")));
        assertEquals("70,00", Numbers.amount(new BigDecimal("70")));
        assertEquals("0,13", Numbers.amount(new BigDecimal("0.125")));
        assertEquals("", Numbers.amount(null));
        assertEquals("562,28\u00A0€", Numbers.money(new BigDecimal("562.28"), "EUR"));
        assertEquals("562,28\u00A0USD", Numbers.money(new BigDecimal("562.28"), "USD"));
    }

    @Test
    void formatsQuantitiesWithoutNeedlessDecimals() {
        assertEquals("6,75", Numbers.quantity(new BigDecimal("6.75")));
        assertEquals("3", Numbers.quantity(new BigDecimal("3.00")));
        assertEquals("", Numbers.quantity(null));
    }
}
