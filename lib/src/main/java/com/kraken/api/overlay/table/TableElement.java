package com.kraken.api.overlay.table;

import lombok.Builder;
import lombok.Data;

import java.awt.*;

@Data
@Builder
public class TableElement {
    TableAlignment alignment;
    Color color;
    String content;
}
