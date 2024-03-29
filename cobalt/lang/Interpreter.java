package cobalt.lang;

import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private Environment environment = new Environment();

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

    @Override
    public Object visitVarExpr(Expr.Variable expr) {
        // TODO: Fix analogous variable issues
        return environment.get(expr.name);
        // Object var_value = environment.get(expr.name);
        // if (var_value != null) {
        //     return environment.get(expr.name);
        // } else {
        //     throw new RuntimeError(expr.name,
        //         "Reference to undefined variable: '" + expr.name + "'.");
        // }
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

    private void executeBlock(List<Stmt> statements,
                              Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
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
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        if (!stmt.nullable && value == null) {
            throw new RuntimeError(stmt.name, "Assignment of 'nil' to non-nullable type");
        } else {
            environment.define(stmt.name, value);
            return null;
        }
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
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
                } else if ((left == null) && right instanceof String) {
                    return "nil" + (String) right;
                } else if (left instanceof String && (right == null)) {
                    return (String) left + "nil";
                }
                throw new RuntimeError(expr.operator, "Invalid operands: '" + left + "' and '" + right + "'");
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
