package com.kraken.api.plugins.packetmapper;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a mapped packet with its structure and write operations
 */
@Data
public class PacketMapping {
    private String packetName;
    private String obfuscatedName;
    private String actionType;
    private BufferAnalysis bufferAnalysis;
    private List<ParameterMapping> parameters = new ArrayList<>();
    
    public PacketMapping(String packetName, String actionType, BufferAnalysis analysis) {
        this.packetName = packetName;
        this.obfuscatedName = packetName; // The field name is already obfuscated
        this.actionType = actionType;
        this.bufferAnalysis = analysis;
        
        // Try to deduce parameters from the action type and buffer analysis
        deduceParameters();
    }
    
    /**
     * Attempts to deduce parameter mappings based on action type and buffer data
     */
    private void deduceParameters() {
        if (actionType == null) {
            return;
        }
        
        List<WriteOperation> operations = bufferAnalysis.getOperations();
        
        // Based on known packet types, map parameters
        switch (actionType) {
            case "MOVE_GAMECLICK":
                if (operations.size() >= 3) {
                    addParameter("worldPointX", operations.get(0));
                    addParameter("worldPointY", operations.get(1));
                    addParameter("ctrlDown", operations.get(2));
                }
                break;
                
            case "OPOBJ1":
            case "OPOBJ2":
            case "OPOBJ3":
            case "OPOBJ4":
            case "OPOBJ5":
                if (operations.size() >= 4) {
                    addParameter("objectId", operations.get(0));
                    addParameter("worldPointX", operations.get(1));
                    addParameter("worldPointY", operations.get(2));
                    addParameter("ctrlDown", operations.get(3));
                }
                break;
                
            case "OPLOC1":
            case "OPLOC2":
            case "OPLOC3":
            case "OPLOC4":
            case "OPLOC5":
                if (operations.size() >= 4) {
                    addParameter("objectId", operations.get(0));
                    addParameter("worldPointX", operations.get(1));
                    addParameter("worldPointY", operations.get(2));
                    addParameter("ctrlDown", operations.get(3));
                }
                break;
                
            case "OPNPC1":
            case "OPNPC2":
            case "OPNPC3":
            case "OPNPC4":
            case "OPNPC5":
                if (operations.size() >= 2) {
                    addParameter("npcIndex", operations.get(0));
                    addParameter("ctrlDown", operations.get(1));
                }
                break;
                
            case "IF_BUTTON1":
            case "IF_BUTTON2":
            case "IF_BUTTON3":
            case "IF_BUTTON4":
            case "IF_BUTTON5":
                if (operations.size() >= 3) {
                    addParameter("widgetId", operations.get(0));
                    addParameter("slot", operations.get(1));
                    addParameter("itemId", operations.get(2));
                }
                break;
                
            case "IF_BUTTONT":
                if (operations.size() >= 6) {
                    addParameter("sourceWidgetId", operations.get(0));
                    addParameter("sourceSlot", operations.get(1));
                    addParameter("sourceItemId", operations.get(2));
                    addParameter("destinationWidgetId", operations.get(3));
                    addParameter("destinationSlot", operations.get(4));
                    addParameter("destinationItemId", operations.get(5));
                }
                break;
                
            default:
                // For unknown types, create generic parameters
                for (int i = 0; i < operations.size(); i++) {
                    addParameter("param" + i, operations.get(i));
                }
                break;
        }
    }
    
    /**
     * Adds a parameter mapping
     */
    private void addParameter(String paramName, WriteOperation operation) {
        ParameterMapping param = new ParameterMapping();
        param.setParameterName(paramName);
        param.setWriteOperation(operation);
        parameters.add(param);
    }
    
    /**
     * Generates Java constant definitions for this packet mapping
     */
    public String toJavaConstants() {
        StringBuilder sb = new StringBuilder();
        String upperName = packetName.toUpperCase();
        
        // Add comment with action type
        if (actionType != null) {
            sb.append("// ").append(actionType).append("\n");
        }
        
        // Obfuscated packet name
        sb.append("public static final String ").append(upperName)
          .append("_OBFUSCATEDNAME = \"").append(obfuscatedName).append("\";\n");
        
        // Parameter mappings
        for (int i = 0; i < parameters.size(); i++) {
            ParameterMapping param = parameters.get(i);
            int paramNum = i + 1;
            
            // Parameter name
            sb.append("public static final String ").append(upperName)
              .append("_WRITE").append(paramNum)
              .append(" = \"").append(param.getParameterName()).append("\";\n");
            
            // Write method name (this would need to be extracted from actual method calls)
            sb.append("public static final String ").append(upperName)
              .append("_METHOD_NAME").append(paramNum)
              .append(" = \"").append(param.getWriteOperation().getMethodName() != null ? 
                     param.getWriteOperation().getMethodName() : "unknown").append("\";\n");
        }
        
        // Write transformations array
        sb.append("public static final String[][] ").append(upperName)
          .append("_WRITES = new String[][]{\n");
        
        for (int i = 0; i < parameters.size(); i++) {
            ParameterMapping param = parameters.get(i);
            WriteOperation op = param.getWriteOperation();
            
            sb.append("        {");
            
            // Add transformation strings
            List<String> transformations = op.getTransformations();
            for (int j = 0; j < transformations.size(); j++) {
                sb.append("\"").append(transformations.get(j)).append("\"");
                if (j < transformations.size() - 1) {
                    sb.append(", ");
                }
            }
            
            sb.append("}");
            
            if (i < parameters.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        
        sb.append("};\n");
        
        return sb.toString();
    }
    
    /**
     * Returns a human-readable summary of the mapping
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PacketMapping{");
        sb.append("name=").append(packetName);
        sb.append(", action=").append(actionType);
        sb.append(", params=").append(parameters.size());
        sb.append(", bytes=").append(bufferAnalysis.getTotalBytes());
        sb.append("}");
        return sb.toString();
    }
}
