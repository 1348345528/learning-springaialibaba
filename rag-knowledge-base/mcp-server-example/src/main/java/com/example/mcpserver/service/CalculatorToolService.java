package com.example.mcpserver.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class CalculatorToolService {

    @Tool(description = "Add two numbers")
    public double add(double a, double b) {
        return a + b;
    }

    @Tool(description = "Subtract two numbers")
    public double subtract(double a, double b) {
        return a - b;
    }

    @Tool(description = "Multiply two numbers")
    public double multiply(double a, double b) {
        return a * b;
    }

    @Tool(description = "Divide two numbers, throws if divisor is zero")
    public double divide(double a, double b) {
        if (b == 0) {
            throw new IllegalArgumentException("Cannot divide by zero");
        }
        return a / b;
    }
}
