package com.example.mcpserver.service;

import com.example.mcpserver.model.User;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserToolService {

    @Tool(description = "Get a list of demo users with name and age")
    public List<User> getUserList() {
        return List.of(
                new User("小明", 18),
                new User("小红", 20),
                new User("张三", 25),
                new User("小白", 22)
        );
    }
}
