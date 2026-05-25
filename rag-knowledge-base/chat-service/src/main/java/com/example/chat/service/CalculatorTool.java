package com.example.chat.service;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 数学计算工具 — 让 Agent 能执行精确的数学表达式求值。
 * 使用手写递归下降解析器，无外部依赖，JDK 17+ 兼容。
 */
public class CalculatorTool implements java.util.function.Function<CalculatorTool.Request, CalculatorTool.Response> {

    @Override
    public Response apply(Request request) {
        String expr = request.expression();
        if (expr == null || expr.isBlank()) {
            return new Response(null, "表达式不能为空");
        }

        if (!expr.matches("[0-9+\\-*/().%\\s]+")) {
            return new Response(null, "表达式包含非法字符，仅支持数字和 + - * / ( ) . %");
        }

        try {
            double d = new ExprEvaluator(expr).eval();
            String display = (d == Math.floor(d) && !Double.isInfinite(d))
                    ? String.valueOf((long) d)
                    : String.valueOf(d);
            return new Response(display, null);
        } catch (Exception e) {
            return new Response(null, "数学表达式计算失败: " + e.getMessage());
        }
    }

    /** 简单递归下降算术表达式求值器 */
    private static class ExprEvaluator {
        private final String input;
        private int pos;

        ExprEvaluator(String input) {
            this.input = input;
            this.pos = 0;
        }

        double eval() {
            double result = expr();
            skipSpaces();
            if (pos < input.length()) {
                throw new IllegalArgumentException("表达式末尾有意外字符: '" + input.charAt(pos) + "'");
            }
            return result;
        }

        // expr  = term (('+'|'-') term)*
        private double expr() {
            double left = term();
            skipSpaces();
            while (pos < input.length()) {
                char op = input.charAt(pos);
                if (op == '+' || op == '-') {
                    pos++;
                    double right = term();
                    left = (op == '+') ? left + right : left - right;
                } else {
                    break;
                }
                skipSpaces();
            }
            return left;
        }

        // term = factor (('*'|'/'|'%') factor)*
        private double term() {
            double left = factor();
            skipSpaces();
            while (pos < input.length()) {
                char op = input.charAt(pos);
                if (op == '*' || op == '/' || op == '%') {
                    pos++;
                    double right = factor();
                    if (op == '*') left = left * right;
                    else if (op == '%') left = left % right;
                    else {
                        if (right == 0) throw new ArithmeticException("除以零");
                        left = left / right;
                    }
                } else {
                    break;
                }
                skipSpaces();
            }
            return left;
        }

        // factor = '(' expr ')' | number
        private double factor() {
            skipSpaces();
            if (pos >= input.length()) {
                throw new IllegalArgumentException("表达式不完整");
            }
            char c = input.charAt(pos);
            if (c == '(') {
                pos++;
                double result = expr();
                skipSpaces();
                if (pos >= input.length() || input.charAt(pos) != ')') {
                    throw new IllegalArgumentException("缺少右括号");
                }
                pos++;
                return result;
            }
            return number();
        }

        private double number() {
            skipSpaces();
            int start = pos;
            while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
                pos++;
            }
            if (start == pos) {
                throw new IllegalArgumentException("期望数字，但找到: '" + input.charAt(pos) + "'");
            }
            return Double.parseDouble(input.substring(start, pos));
        }

        private void skipSpaces() {
            while (pos < input.length() && input.charAt(pos) == ' ') {
                pos++;
            }
        }
    }

    public record Request(String expression) {}

    public record Response(String result, String error) {}
}
