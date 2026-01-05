package com.kraken.api.service.util.reflect.hooks.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MethodHook {
    private String methodName;
    private String className;
    private Integer garbageValue;
}