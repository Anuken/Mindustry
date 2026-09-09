import mindustry.logic.*;
import mindustry.logic.LExecutor.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;

public class LogicTests{

    @BeforeAll
    static void init(){
        ApplicationTests.launchApplication();
    }

    /** Assembles and loads a small mlog program, mirroring the LogicBlock usage pattern. */
    static LExecutor load(String code){
        LExecutor exec = new LExecutor();
        exec.privileged = true;
        exec.load(LAssembler.assemble(code, true));
        return exec;
    }

    /** Runs a single `set result <value>` line and returns the decoded value assigned to `from`. */
    static Object setFromValue(String code){
        LExecutor exec = load(code);
        assertTrue(exec.instructions.length > 0, "expected at least one instruction to be parsed from: " + code);
        assertTrue(exec.instructions[0] instanceof SetI, "expected a set instruction from: " + code);
        return ((SetI)exec.instructions[0]).from.objval;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("stringEscapeCases")
    void parsesStringEscapes(String name, String code, String expected){
        assertEquals(expected, setFromValue(code));
    }

    static Stream<Arguments> stringEscapeCases(){
        return Stream.of(
        Arguments.of(
        "plain string has no escapes",
        "set result \"asdf\"",
        "asdf"
        ),
        Arguments.of(
        "\\n decodes to a real newline character",
        "set result \"line1\\nline2\"",
        "line1\nline2"
        ),
        Arguments.of(
        "\\\" decodes to a literal quote without ending the string",
        "set result \"the entity said \\\"hi\\\"\"",
        "the entity said \"hi\""
        ),
        Arguments.of(
        "\\\\ decodes to a single literal backslash",
        "set result \"a\\\\b\"",
        "a\\b"
        ),
        Arguments.of(
        "an escaped backslash immediately followed by a literal n must NOT collapse into a newline",
        "set result \"a\\\\nb\"",
        "a\\nb"
        ),
        Arguments.of(
        "unrecognized escape sequences pass through as a literal backslash + character",
        "set result \"tab\\ttab\"",
        "tab\\ttab"
        ),
        Arguments.of(
        "\\n, \\\" and \\\\ combined in a single string",
        "set result \"start\\nmid\\\"quoted\\\"\\\\end\"",
        "start\nmid\"quoted\"\\end"
        ),
        Arguments.of(
        "empty quoted string",
        "set result \"\"",
        ""
        ),
        Arguments.of(
        "escaped backslash sitting right before the closing quote",
        "set result \"end\\\\\"",
        "end\\"
        ),
        Arguments.of(
        "\\u0041 decodes to the corresponding unicode character",
        "set result \"\\u0041\"",
        "A"
        ),
        Arguments.of(
        "\\uf8ff decodes to the corresponding unicode character",
        "set result \"\\uf8ff\\uf8ff\"",
        ""
        ),
        Arguments.of(
        "\\u escapes combined with other escapes in one string",
        "set result \"\\u0041\\n\\u0042\\\\end\"",
        "A\nB\\end"
        )
        );
    }

    @Test
    void plainNumberIsNotTreatedAsAString(){
        LExecutor exec = load("set result 5");
        SetI set = (SetI)exec.instructions[0];
        assertFalse(set.from.isobj, "a bare number should not be stored as an object value");
        assertEquals(5.0, set.from.numval);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sanitizeCases")
    void sanitizesInput(String name, String input, String expected){
        assertEquals(expected, LStatement.sanitize(input));
    }

    static Stream<Arguments> sanitizeCases(){
        return Stream.of(
        Arguments.of("a bare single quote character is invalid", "\"", "invalid"),
        Arguments.of("a bare single semicolon is invalid", ";", "invalid"),
        Arguments.of("a bare single space is invalid", " ", "invalid"),
        Arguments.of("a single ordinary character passes through unchanged", "a", "a"),
        Arguments.of("empty input stays empty", "", ""),
        Arguments.of("a plain already-quoted value is untouched", "\"hello\"", "\"hello\""),
        Arguments.of("a unescaped quote at the end gets doubled", "\"hello\\\"", "\"hello\\\\\""),
        Arguments.of(
        "a bare, unescaped quote embedded inside a quoted value gets escaped",
        "\"a\"b\"",
        "\"a\\\"b\""
        ),
        Arguments.of(
        "a literal backslash inside a quoted value gets escaped so it can't form an accidental escape",
        "\"C:\\Users\"",
        "\"C:\\\\Users\""
        ),
        Arguments.of(
        "a real embedded newline inside a quoted value is turned into the \\n escape sequence",
        "\"line1\nline2\"",
        "\"line1\\nline2\""
        ),
        Arguments.of(
        "bare quote + literal backslash + real newline combined inside one quoted value",
        "\"a\\b\"c\nd\"",
        "\"a\\\\b\\\"c\\nd\""
        ),
        Arguments.of(
        "unquoted values get semicolons/spaces/quotes replaced instead of escaped",
        "hello world;test\"quote",
        "hello_worldstest'quote"
        ),
        Arguments.of("a raw newline in an unquoted value gets neutralized like a space", "a\nb\nc", "a_b_c"),
        Arguments.of("a raw tab in an unquoted value gets neutralized like a space", "a\tb", "a_b"),
        Arguments.of("a raw '#' in an unquoted value gets neutralized so it can't start a comment", "a#b", "a_b"),
        Arguments.of("a lone newline character is invalid on its own, same as a lone space", "\n", "invalid"),
        Arguments.of("a lone tab character is invalid on its own, same as a lone space", "\t", "invalid"),
        Arguments.of("a lone '#' character is invalid on its own, same as a lone space", "#", "invalid"),
        Arguments.of(
        "a well-formed \\uXXXX escape inside a quoted value passes through untouched",
        "\"\\u0041\"",
        "\"\\u0041\""
        ),
        Arguments.of(
        "\\uXXXX combined with other escapes inside a quoted value",
        "\"\\u0041\\n\\u0042\\\\end\"",
        "\"\\u0041\\n\\u0042\\\\end\""
        ),
        Arguments.of(
        "a malformed \\u escape with too few digits still escapes the backslash instead of throwing",
        "\"\\u12\"",
        "\"\\\\u12\""
        ),
        Arguments.of(
        "a malformed \\u escape with non-hex digits still escapes the backslash instead of throwing",
        "\"\\u12zz\"",
        "\"\\\\u12zz\""
        ),
        Arguments.of(
        "a \\u escape truncated by the closing quote still escapes the backslash instead of throwing",
        "\"a\\u123\"",
        "\"a\\\\u123\""
        )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sanitizeRoundTripCases")
    void sanitizedQuotedValuesRoundTripThroughTheParser(String name, String userInput, String expectedDecoded){
        String sanitized = LStatement.sanitize(userInput);
        //sanity check: sanitize should have kept this as a quoted string value
        assertTrue(sanitized.length() >= 2 && sanitized.charAt(0) == '"' && sanitized.charAt(sanitized.length() - 1) == '"', "expected a quoted value, got: " + sanitized);

        Object decoded = setFromValue("set result " + sanitized);
        assertEquals(expectedDecoded, decoded);

        //make sure read/write roundtrips it
        var statements = LAssembler.read("set result " + sanitized + "\n", true);
        assertEquals("set result " + sanitized + "\n", LAssembler.write(statements));
    }

    static Stream<Arguments> sanitizeRoundTripCases(){
        return Stream.of(
        Arguments.of("plain quoted value", "\"hello\"", "hello"),
        Arguments.of("bare embedded quote round-trips to a literal quote", "\"a\"b\"", "a\"b"),
        Arguments.of("literal backslash round-trips to a single backslash", "\"C:\\Users\"", "C:\\Users"),
        Arguments.of("real embedded newline round-trips to a real newline", "\"line1\nline2\"", "line1\nline2"),
        Arguments.of("newline typed as escape turns into real backslash", "\"a\\nb\\nc\"", "a\nb\nc"),

        //check intentional escapes
        Arguments.of(
        "bare quote + backslash + newline combined round-trip correctly together",
        "\"a\\b\"c\nd\"",
        "a\\b\"c\nd"
        ),
        Arguments.of(
        "typing \\n by hand in a single-line field decodes to a real newline, not literal backslash+n",
        "\"a\\nb\\nc\"",
        "a\nb\nc"
        ),
        Arguments.of(
        "typing \\\" by hand decodes to a literal embedded quote, not literal backslash+quote",
        "\"say \\\"hi\\\"\"",
        "say \"hi\""
        ),
        //this case is a little unintuitive (two backslashes = one backslash, but so is one with nothing after it?)
        //I don't have any better ideas for handling this
        Arguments.of(
        "typing \\\\ by hand (two backslashes) decodes to a single literal backslash",
        "\"a\\\\b\"",
        "a\\b"
        ),
        Arguments.of(
        "a lone backslash NOT forming a recognized escape still round-trips as itself, unaffected by the fix",
        "\"C:\\Users\"",
        "C:\\Users"
        ),
        Arguments.of(
        "a trailing lone backslash right before the closing quote still round-trips as itself",
        "\"end\\\"",
        "end\\"
        )
        );
    }

    //unterminated / malformed string literals: these must fail loudly, never silently misparse

    @ParameterizedTest(name = "{0}")
    @MethodSource("unterminatedStringCases")
    void unterminatedStringsThrow(String name, String code){
        assertThrows(RuntimeException.class, () -> load(code), "expected a parse error from: " + code);
    }

    static Stream<Arguments> unterminatedStringCases(){
        return Stream.of(
        Arguments.of(
        "a trailing escaped quote is content, not a terminator, so the string is left unterminated",
        "set result \"asd\\\""
        ),
        Arguments.of(
        "a dangling backslash at the absolute end of input has nothing to escape",
        "set result \"asd\\"
        ),
        Arguments.of(
        "an escaped-quote-then-semicolon does not let the semicolon act as a statement separator inside the string",
        "set result \"asd\\\"; set bar 5"
        ),
        Arguments.of(
        "a string missing its closing quote entirely",
        "set result \"asd"
        ),
        Arguments.of(
        "a string that never closes before the line ends, with trailing content after it",
        "set result \"asd\nset bar 5"
        ),
        Arguments.of(
        "a \\u escape with non-hex digits is rejected",
        "set result \"\\u12zz\""
        ),
        Arguments.of(
        "a \\u escape with non-hex digits is rejected",
        "set result \"\\u1h23\""
        ),
        Arguments.of(
        "a \\u escape with non-hex digits is rejected",
        "set result \"\\uhh23\""
        ),
        Arguments.of(
        "a \\u escape truncated by the closing quote is rejected",
        "set result \"\\u12\""
        )
        );
    }

    @Test
    void varWithProperlyQuotedEmptyString(){
        LAssembler asm = new LAssembler();
        LVar v = asm.var("\"\"");
        assertTrue(v.isobj);
        assertEquals("", v.objval);
    }

    @Test
    void quoteInVariableNameThrows(){
        //quotes mid-variable should not be allowed
        assertThrows(RuntimeException.class, () -> load("set result abc\"def"));
    }

    @Test
    void quoteAtEndOfVariableNameThrows(){
        //ditto
        assertThrows(RuntimeException.class, () -> load("set result abcdef\""));
    }

    @Test
    void crlfAfterUnquotedTokenDoesNotCorruptTheToken(){
        LExecutor exec = load("set a bar\r\nset b bar\n");
        SetI first = (SetI)exec.instructions[0];
        SetI second = (SetI)exec.instructions[1];
        assertSame(first.from, second.from, "a CRLF-terminated reference to 'bar' must resolve to the same variable as an LF-terminated one");
    }

    @Test
    void crlfAfterQuotedStringParsesCleanly(){
        assertEquals("bar", setFromValue("set result \"bar\"\r\n"));
    }

    @Test
    void loneCarriageReturnActsAsALineEnding(){
        //old-style Mac ('\r'-only) line endings are normalized the same way as CRLF.
        LExecutor exec = load("set result 1\rset result2 2\r");
        assertEquals(2, exec.instructions.length, "expected two separate statements, split on the lone '\\r'");
    }

    @Test
    void crlfLabelsResolveToTheSameJumpLocationAsLfLabels(){
        assertDoesNotThrow(() -> load("loop:\r\njump loop always\r\n"));
    }
}