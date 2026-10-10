import arc.struct.*;
import arc.util.*;
import org.junit.*;

import static org.junit.Assert.*;

public class StringsTest{

    @Test
    public void testSanitizeFilename(){
        assertEquals(Strings.sanitizeFilename("test"), "test");
        assertEquals(Strings.sanitizeFilename("test.txt"), "test.txt");
        assertEquals(Strings.sanitizeFilename("test/test"), "test_test");
        assertEquals(Strings.sanitizeFilename("CON"), "_CON");
        assertEquals(Strings.sanitizeFilename("CON.a/test"), "_CON.a_test");
        assertEquals(Strings.sanitizeFilename("."), "_");
        assertEquals(Strings.sanitizeFilename(".."), "__");
        assertEquals(Strings.sanitizeFilename("..txt"), "..txt");
    }

    @Test
    public void testFixed(){
        Object[] values = {
        3, 1.327f,  "1.327",
        2, 1.327f,  "1.32",
        1, 1.327f,  "1.3",
        0, 1.327f,  "1",
        3, 1.3005f, "1.3",
        2, 0.0001f, "0",
        4, 0.0001f, "0.0001",
        3, 0.0001f, "0",
        2, 2.435f, "2.43",
        2, 2.99f, "2.99",
        2, 2.99999f, "3",
        2, 0f, "0"
        };

        for(int i = 0; i < values.length; i += 3){
            assertEquals(values[i + 2], Strings.autoFixed((Float)values[i + 1], (Integer)values[i]));
        }
    }

    @Test
    public void testIntegerParse(){
        Seq.with("0", "+0", "-0", "235235", "99424", "1234", "1", "-24242", "170589", "-289157", "4246", "19284", "-672396", "-42412042040945", "1592835012852095", "9999999999999999999", "99999999999")
        .each(StringsTest::checkInteger);
    }

    @Test
    public void testInvalidFloat(){
        //parseFloat uses parseDouble and casts, so make sure the default value is returned correctly
        for(float f : new float[]{Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.MAX_VALUE, Float.MIN_VALUE}){
            assertEquals("For value: " + f, f, Strings.parseFloat("garbage", f), 0.001f);
        }
    }

    @Test
    public void testDoubleFloatParse(){
        Seq.with(
        "0", "0.0", "123.456", "123f", "145.6", "1e10", "-512515", "-535.646", "999.9344", "0.24324",
        ".325235", "3424324.", "+.31245", "-.51354", ".0", "-.0", "+.0", "0.000002", "200000.2000",
        "2000.00004", "-0.5", "1e10", "1e154", "1.5e3", "1.32e-6", "6e10",

        //decimal + exponent together
        "1.5e10", "-1.5e10", "1.5E10", "-1.5E-10", "3.14e2", "-3.14e-2", "2.5e0", "0.5e5", "-0.5e-5",

        //trailing dot combined with exponent / suffix
        "1.e5", "-1.e5", "5.e-3", "3.f", "-3.f", "10.F",

        //leading-dot combined with exponent
        ".5e3", "-.5e3", "+.5e-3",

        //plain signed integers/decimals
        "+123.456", "+5", "5", "-5", "1234567890.123456", "-1234567890.123456",

        //zero variants
        "0e0", "-0e0", "0.0e0", "0e10", "0.000e5",

        //small/negative exponents
        "1.32E-6", "9.999e-3", "-9.999e-3",

        //multi-digit exponents
        "1.234e12", "-1.234e12", "5.5e20", "-5.5e20",

        //ridiculous precision values
        "4.000000000000000000000000000000000000000005",
        "3.32798573289759823759832759382758392753275975239",
        "1.5e99999", "000123.4500",
        "7325897598275927589"
        ).each(StringsTest::checkFloat);
    }

    static void checkFloat(String value){
        assertEquals("For value: " + value, Double.parseDouble(value), Strings.parseDouble(value, Double.NaN), 0.00001);
        assertEquals("For value: " + value, Float.parseFloat(value), Strings.parseFloat(value, Float.NaN), 0.00001);
    }

    static void checkInteger(String value){
        long expectedLong = 67;

        try{
            expectedLong = Long.parseLong(value);
        }catch(Exception e){
            //out-of-range values should fail, so retain the placedholder 'wrong' value (essentially, checks if both failed)
        }

        assertEquals("Long parse: " + value, expectedLong, Strings.parseLong(value, 67));

        int expected = 67;
        try{
            expected = Integer.parseInt(value);
        }catch(Exception e){
            //ditto
        }

        assertEquals("Int parse: " + value, expected, Strings.parseInt(value, 67));
    }
}
