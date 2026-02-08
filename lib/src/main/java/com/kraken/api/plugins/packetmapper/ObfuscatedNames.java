package com.kraken.api.plugins.packetmapper;

/**
 * Contains obfuscated names for game client classes and fields.
 * These values change with each game update and must be updated accordingly.
 */
public class ObfuscatedNames {
    
    // Multipliers
    public static final String offsetMultiplier = "-962389735";
    public static final String indexMultiplier = "1342780201";
    public static final String addNodeGarbageValue = "2";
    public static final String mouseHandlerMillisMultiplier = "1507165324631712439";
    public static final String clientMillisMultiplier = "-3895020773792481799";
    
    // Packet-related class names
    public static final String clientPacketClassName = "mu";
    public static final String packetWriterClassName = "dh";
    public static final String packetBufferNodeClassName = "mw";
    public static final String classContainingGetPacketBufferNodeName = "hi";
    public static final String doActionClassName = "cp";
    
    // Field names
    public static final String getPacketBufferNodeGarbageValue = "-1779200100";
    public static final String packetWriterFieldName = "ca";
    public static final String isaacCipherFieldName = "af";
    public static final String packetBufferFieldName = "al";
    public static final String bufferOffsetField = "ai";
    public static final String bufferArrayField = "av";
    public static final String clientMillisField = "jb";
    
    // Method names
    public static final String addNodeMethodName = "ah";
    public static final String doActionMethodName = "kf";
    
    // MouseHandler
    public static final String MouseHandler_lastPressedTimeMillisClass = "bu";
    public static final String MouseHandler_lastPressedTimeMillisField = "ab";
    
    /**
     * Common buffer write method names (these are examples and need to be updated)
     */
    public static class BufferWriteMethods {
        // Byte writes
        public static final String WRITE_BYTE = "writeByte";
        public static final String WRITE_BYTE_ADD = "writeByteAdd";
        public static final String WRITE_BYTE_NEG = "writeByteNeg";
        public static final String WRITE_BYTE_SUB = "writeByteSub";
        
        // Short writes
        public static final String WRITE_SHORT = "writeShort";
        public static final String WRITE_SHORT_ADD = "writeShortAdd";
        public static final String WRITE_SHORT_LE = "writeShortLE";
        public static final String WRITE_SHORT_ADD_LE = "writeShortAddLE";
        
        // Int writes
        public static final String WRITE_INT = "writeInt";
        public static final String WRITE_INT_ME = "writeIntME";
        public static final String WRITE_INT_LE = "writeIntLE";
        
        // String writes
        public static final String WRITE_STRING_CP1252_NULL_TERMINATED = "writeStringCp1252NullTerminated";
        public static final String WRITE_STRING_CP1252_NULL_CIRCUMFIXED = "writeStringCp1252NullCircumfixed";
    }
}
