package com.kraken.api.overlay.table;

import lombok.Builder;
import lombok.Data;

import java.awt.*;
import java.util.Collections;
import java.util.List;

@Data
@Builder
public class TableRow {
    Color rowColor;
    TableAlignment rowAlignment;
    @Builder.Default
    List<TableElement> elements = Collections.emptyList();
}
