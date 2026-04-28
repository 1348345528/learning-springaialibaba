package com.example.chat.model;

import java.io.Serializable;

/**
 * Represents a single chat message in the conversation memory.
 * This is a domain model used across the memory system.
 *
 * @param role     The role of the message sender (user, assistant, system)
 * @param content  The content of the message
 */
public record ChatMemoryMessage(
    String role,
    String content
) implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a user message.
     */
    public static ChatMemoryMessage user(String content) {
        return new ChatMemoryMessage("user", content);
    }

    /**
     * Creates an assistant message.
     */
    public static ChatMemoryMessage assistant(String content) {
        return new ChatMemoryMessage("assistant", content);
    }

    /**
     * Creates a system message.
     */
    public static ChatMemoryMessage system(String content) {
        return new ChatMemoryMessage("system", content);
    }
}
