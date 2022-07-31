package cobalt.lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cobalt.lang.TokenType.*;

class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 0;
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else",  ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("func", FUNC);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var",   VAR);
        keywords.put("while", WHILE);
    }

    // Constructor
    // Set our source string to be the incoming character data from the
    // input script
    Scanner(String source) {
        this.source = source;
    }

    // Scan the input file for all available tokens, return a Token list with all
    // of our valid tokens
    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    // Check to see if we have reached the end of the script
    private boolean isAtEnd() {
        return current >= source.length();
    }

    // Parse the current token from the scanner to see if its a valid
    // lexeme. Report an error otherwise
    private void scanToken() {
        char c = advance();
        switch (c) {
            // Structural and Accessors
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case ';': addToken(SEMICOLON); break;

            // Operators 
            case '*': addToken(STAR); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '/': addToken(SLASH); break;
            case '?': addToken(NULLABLE); break;
    
            // Comments
            case '#': 
                // A comment goes until the end of the line
                while (peek() != '\n' && !isAtEnd()) advance();
                break;
    

            // Whitespace and new lines
            case ' ':
            case '\r':
            case '\t':
                //ignore whitespace characters
                break;
            case '\n':
                line++;
                break;

            case '"':
                string();
                break;

            default:
                if (isDigit(c)) {
                    // Check to see if our incoming value is part of a number
                    // Switch on base prefix (hex, binary, base10...)
                    switch (peek()) {
                        case 'x':
                            advance(); // Advance to disregard the '0x' prefix
                            hex();
                            break;
                        case 'b':
                            advance(); // Advance to disregard the '0b' prefix
                            binary();
                            break;
                        case 'o':
                            advance(); // Advance to disregard the '0o' prefix
                            octal();
                            break;
                        default:
                            number();
                            break;
                    }
                } else if (isAlpha(c)) {
                    // Check to see if our incoming value is part of
                    // a reserved word or identifier
                    identifier();
                } else {
                    Cobalt.error(line, "Unexpected character.");
                }
                break;
        }
    }


    // Determine if the char is a base 10 digit
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }


    // Determine if the character is a valid base 16 digit
    private boolean isHex(char c) {
        return (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }


    // Determine if the character is a valid base 16 digit
    private boolean isBinary(char c) {
        return (c >= '0' && c <= '1');
    }


    // Determine if the character is a valid base 8 digit
    private boolean isOctal(char c) {
        return (c >= '0' && c <= '7');
    }


    // Parse and tokenize the value as a base 10 number
    private void number() {
        while (isDigit(peek())) advance();

        // Look for a decimal place.
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the .
            advance();

            while (isDigit(peek())) advance();
        }
        
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }


    // Parse and tokenize the value as a hexadecimal number, store in base 10
    private void hex() {
        while (isDigit(peek()) || isHex(peek())) advance();
        
        addToken(NUMBER, Long.parseLong(source.substring(start + 2, current), 16));
    }


    // Parse and tokenize the value as a binary number, store in base 10
    private void binary() {
        while (isBinary(peek())) advance();
        
        addToken(NUMBER, Long.parseLong(source.substring(start + 2, current), 2));
    }


    // Parse and tokenize the value as an octal number, store in base 10
    private void octal() {
        while (isOctal(peek())) advance();
        
        addToken(NUMBER, Long.parseLong(source.substring(start + 2, current), 8));
    }


    // TODO: Lox spec supports multiline strings, we'll need to
    // probably remove that support since I don't intend Cobalt's
    // grammar to support that (maybe) :/

    // TODO: Escape sequences are not supported atm, for the
    // love of god please implement this functionality. Probably
    // should make an enum for the valid escape sequences, parse them
    // out like we do with operators, and inject the actual escape
    // sequence in the object thats returned to the interpreter

    // Process the input line if quotation marks are found
    // and we have a string literal
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            Cobalt.error(line, "Unterminated string.");
            return;
        }

        // Get closing quotes
        advance();

        // Trim the
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }


    // Determine if the infoming token is alphanumeric, and
    // add it to the Token list if it is valid
    private void identifier() {
        while (isAlphaNumeric(peek())) advance();
        
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = IDENTIFIER;
        addToken(type);
    }


    // Checkout the next character in our input, but dont consume it
    // This is mainly to process things like comments that take an entire line
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }


    // Checkout the next+1 character in our input, but dont consume it
    // This is mainly to process things like comments that take an entire line
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }


    // Check to see if the character passed is within
    // [a-z][A-Z]
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               c == '_';
    }


    // Check to see if the character passed is within
    // [a-z][A-Z][0-9]
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }


    // Return a boolean based on if a char is found at the current cursor,
    // then increment
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }


    // Advance the char pointer in the line scanner
    private char advance() {
        return source.charAt(current++);
    }


    // Add a token to the token List that does not have an object literal
    // associated with it.
    private void addToken(TokenType type) {
        addToken(type, null);
    }


    // Add a token to the token List that has an object associated with it
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}