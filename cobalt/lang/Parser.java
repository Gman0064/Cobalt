package cobalt.lang;

import java.util.List;

import cobalt.lang.Expr.Variable;

import java.util.ArrayList;
import static cobalt.lang.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }


    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }


    private Expr expression() {
        return assignment();
    }


    private Stmt declaration() {
        try {
            if (match(VAR)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    
    private Stmt statement() {
        if (match(PRINT)) return printStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }


    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expected ';' after value.");
        return new Stmt.Print(value);
    }


    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expected variable name.");

        Expr initializer = null;
        boolean nullable = false;
        
        if (match(NULLABLE)) {
            nullable = true;
        }

        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expected ';' after variable declaration.");

        return new Stmt.Var(name, nullable, initializer);
    }


    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expected ';' after value.");
        return new Stmt.Expression(expr);
    }


    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expected '}' after block.");
        return statements;
    }


    private Expr assignment() {
        Expr expr = equality();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Variable var = (Expr.Variable) expr;
                Token name = var.name;
                if (!var.nullable && value == null) {
                    error(equals, "Invalid assignment of 'nil' on non-nullable type");
                } else {
                    return new Expr.Assign(name, value);
                }
            }
            error(equals, "Invalid assignment on non-variable type");
        }

        return expr;
    }

    // Parser definition for handling equalities
    //
    // equality     ->  comparison ( ( "!=" | "==" ) comparison )* ;
    //
    private Expr equality() {
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }


    // Parser definition for handling comparisons
    //
    // comparison   ->  term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
    //
    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }


    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }


    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    
    // Parser definition for handling unary statements
    //
    // unary   ->  ( "!" | "-" ) unary
    //             | primary ;
    //
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }


    // Parser definition for handling primary statements
    //
    // primary   ->  NUMBER | STRING | "true" | "false" | "nil"
    //             | "(" expression ")" ;
    //
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(IDENTIFIER)) {
            if (match(NULLABLE)) {
                return new Expr.Variable(previous(), true);
            } else {
                return new Expr.Variable(previous(), false);
            }
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expected expression.");
    }


    // Checks to see if the current token has any of the given types. If so,
    // it consumes the token and returns true. Otherwise, it returns false and leaves
    // the current token alone.
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }


    // Consume function
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }


    // Returns true if the current token is of the given type, false if otherwise
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }


    // Consume the current token and return it
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }


    // Check to see if we've run out of tokens to parse
    private boolean isAtEnd() {
        return peek().type == EOF;
    }


    // Return the current token we have yet to consume
    private Token peek() {
        return tokens.get(current);
    }


    // Return the previous token we just consumed
    private Token previous() {
        return tokens.get(current - 1);
    }


    // Throw a new parse error based on the incorrect token
    private ParseError error(Token token, String message) {
        Cobalt.error(token, message);
        return new ParseError();
    }


    // Once finding a parse error, discard the rest of the tokens in 
    // a statement in order to prevent cascading errors.
    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FOR:
                case FUNC:
                case IF:
                case PRINT:
                case RETURN:
                case VAR:
                case WHILE:
                return;
            }

            advance();
        }
    }
}


