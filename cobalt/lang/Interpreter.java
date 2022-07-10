//
//  Continue at Section 7.2.3
//  Page 100
//

package cobalt.lang;

import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch(RuntimeError error) {
            Cobalt.runtimeError(error);
        }
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                return -(double) right;
        }

        return null;
    }

    private boolean isTruthy(Object object) {
        if (object == null)
            return false;
        if (object instanceof Boolean)
            return (boolean) object;
        return true;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            case BANG_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return isEqual(left, right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return (double) left - (double) right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                } else if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                } else if (left instanceof String && right instanceof Double) {
                    return (String) left + (Double) right;
                } else if (left instanceof Double && right instanceof String) {
                    return (Double) left + (String) right;
                }
                throw new RuntimeError(expr.operator, "Operands must be of type number or string");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                checkNumberOperand(expr.operator, right);
                return (double) left / (double) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                boolean usingStrings = false;
                String val = "";
                double limit = 0;

                if (left instanceof String) {
                    usingStrings = true;
                    val = (String)left;
                    limit = (Double)right;
                } else if (right instanceof String) {
                    usingStrings = true;
                    val = (String)right;
                    limit = (Double)left;
                }

                if (usingStrings) {
                    String output = "";
                    for (int i = 0; i < limit; i++) {
                        output += val;
                    }
                    return output;
                } else {
                    return (double) left * (double) right;
                }
        }

        // Unreachable
        return null;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null)
            return true;
        if (a == null)
            return false;

        return a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) {
            if (operator.type == TokenType.SLASH && (double)operand == 0) {
                throw new RuntimeError(operator, "Division by zero is not allowed");
            }
            return;
        }
        throw new RuntimeError(operator, "Operand must be a number");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) {
            return;
        }
        if (left instanceof String && right instanceof Double) {
            return;
        }
        if (left instanceof Double && right instanceof String) {
            return;
        }
        
        throw new RuntimeError(operator, "Operands must be numbers");
    }
}
