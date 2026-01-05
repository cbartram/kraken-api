package com.kraken.api.service.util.reflect.hooks.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldHook {
    private String fieldName;
    private String className;
}