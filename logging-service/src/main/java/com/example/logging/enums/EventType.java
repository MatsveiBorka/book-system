package com.example.logging.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventType {
    CREATE("Create"),
    UPDATE("Update"),
    DELETE("Delete");

    private final String name;
}
