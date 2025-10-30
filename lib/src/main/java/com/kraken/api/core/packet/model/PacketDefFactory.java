package com.kraken.api.core.packet.model;

import com.google.inject.Singleton;
import com.kraken.api.core.packet.ObfuscatedNames;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class PacketDefFactory {

    private final Map<String, PacketDef> cache = new ConcurrentHashMap<>();

    // OPOBJ packets (1-5)
    public PacketDef getOpObj1() {
        return cache.computeIfAbsent("OPOBJ1", k -> new PacketDef(
                ObfuscatedNames.OPOBJ1_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPOBJ1_WRITE1, ObfuscatedNames.OPOBJ1_WRITE2,
                        ObfuscatedNames.OPOBJ1_WRITE3, ObfuscatedNames.OPOBJ1_WRITE4},
                ObfuscatedNames.OPOBJ1_WRITES,
                PacketType.OPOBJ
        ));
    }

    public PacketDef getOpObj2() {
        return cache.computeIfAbsent("OPOBJ2", k -> new PacketDef(
                ObfuscatedNames.OPOBJ2_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPOBJ2_WRITE1, ObfuscatedNames.OPOBJ2_WRITE2,
                        ObfuscatedNames.OPOBJ2_WRITE3, ObfuscatedNames.OPOBJ2_WRITE4},
                ObfuscatedNames.OPOBJ2_WRITES,
                PacketType.OPOBJ
        ));
    }

    public PacketDef getOpObj3() {
        return cache.computeIfAbsent("OPOBJ3", k -> new PacketDef(
                ObfuscatedNames.OPOBJ3_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPOBJ3_WRITE1, ObfuscatedNames.OPOBJ3_WRITE2,
                        ObfuscatedNames.OPOBJ3_WRITE3, ObfuscatedNames.OPOBJ3_WRITE4},
                ObfuscatedNames.OPOBJ3_WRITES,
                PacketType.OPOBJ
        ));
    }

    public PacketDef getOpObj4() {
        return cache.computeIfAbsent("OPOBJ4", k -> new PacketDef(
                ObfuscatedNames.OPOBJ4_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPOBJ4_WRITE1, ObfuscatedNames.OPOBJ4_WRITE2,
                        ObfuscatedNames.OPOBJ4_WRITE3, ObfuscatedNames.OPOBJ4_WRITE4},
                ObfuscatedNames.OPOBJ4_WRITES,
                PacketType.OPOBJ
        ));
    }

    public PacketDef getOpObj5() {
        return cache.computeIfAbsent("OPOBJ5", k -> new PacketDef(
                ObfuscatedNames.OPOBJ5_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPOBJ5_WRITE1, ObfuscatedNames.OPOBJ5_WRITE2,
                        ObfuscatedNames.OPOBJ5_WRITE3, ObfuscatedNames.OPOBJ5_WRITE4},
                ObfuscatedNames.OPOBJ5_WRITES,
                PacketType.OPOBJ
        ));
    }

    // OPLOC packets (1-5)
    public PacketDef getOpLoc1() {
        return cache.computeIfAbsent("OPLOC1", k -> new PacketDef(
                ObfuscatedNames.OPLOC1_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPLOC1_WRITE1, ObfuscatedNames.OPLOC1_WRITE2,
                        ObfuscatedNames.OPLOC1_WRITE3, ObfuscatedNames.OPLOC1_WRITE4},
                ObfuscatedNames.OPLOC1_WRITES,
                PacketType.OPLOC
        ));
    }

    public PacketDef getOpLoc2() {
        return cache.computeIfAbsent("OPLOC2", k -> new PacketDef(
                ObfuscatedNames.OPLOC2_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPLOC2_WRITE1, ObfuscatedNames.OPLOC2_WRITE2,
                        ObfuscatedNames.OPLOC2_WRITE3, ObfuscatedNames.OPLOC2_WRITE4},
                ObfuscatedNames.OPLOC2_WRITES,
                PacketType.OPLOC
        ));
    }

    public PacketDef getOpLoc3() {
        return cache.computeIfAbsent("OPLOC3", k -> new PacketDef(
                ObfuscatedNames.OPLOC3_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPLOC3_WRITE1, ObfuscatedNames.OPLOC3_WRITE2,
                        ObfuscatedNames.OPLOC3_WRITE3, ObfuscatedNames.OPLOC3_WRITE4},
                ObfuscatedNames.OPLOC3_WRITES,
                PacketType.OPLOC
        ));
    }

    public PacketDef getOpLoc4() {
        return cache.computeIfAbsent("OPLOC4", k -> new PacketDef(
                ObfuscatedNames.OPLOC4_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPLOC4_WRITE1, ObfuscatedNames.OPLOC4_WRITE2,
                        ObfuscatedNames.OPLOC4_WRITE3, ObfuscatedNames.OPLOC4_WRITE4},
                ObfuscatedNames.OPLOC4_WRITES,
                PacketType.OPLOC
        ));
    }

    public PacketDef getOpLoc5() {
        return cache.computeIfAbsent("OPLOC5", k -> new PacketDef(
                ObfuscatedNames.OPLOC5_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPLOC5_WRITE1, ObfuscatedNames.OPLOC5_WRITE2,
                        ObfuscatedNames.OPLOC5_WRITE3, ObfuscatedNames.OPLOC5_WRITE4},
                ObfuscatedNames.OPLOC5_WRITES,
                PacketType.OPLOC
        ));
    }

    // OPNPC packets (1-5)
    public PacketDef getOpNpc1() {
        return cache.computeIfAbsent("OPNPC1", k -> new PacketDef(
                ObfuscatedNames.OPNPC1_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPNPC1_WRITE1, ObfuscatedNames.OPNPC1_WRITE2},
                ObfuscatedNames.OPNPC1_WRITES,
                PacketType.OPNPC
        ));
    }

    public PacketDef getOpNpc2() {
        return cache.computeIfAbsent("OPNPC2", k -> new PacketDef(
                ObfuscatedNames.OPNPC2_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPNPC2_WRITE1, ObfuscatedNames.OPNPC2_WRITE2},
                ObfuscatedNames.OPNPC2_WRITES,
                PacketType.OPNPC
        ));
    }

    public PacketDef getOpNpc3() {
        return cache.computeIfAbsent("OPNPC3", k -> new PacketDef(
                ObfuscatedNames.OPNPC3_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPNPC3_WRITE1, ObfuscatedNames.OPNPC3_WRITE2},
                ObfuscatedNames.OPNPC3_WRITES,
                PacketType.OPNPC
        ));
    }

    public PacketDef getOpNpc4() {
        return cache.computeIfAbsent("OPNPC4", k -> new PacketDef(
                ObfuscatedNames.OPNPC4_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPNPC4_WRITE1, ObfuscatedNames.OPNPC4_WRITE2},
                ObfuscatedNames.OPNPC4_WRITES,
                PacketType.OPNPC
        ));
    }

    public PacketDef getOpNpc5() {
        return cache.computeIfAbsent("OPNPC5", k -> new PacketDef(
                ObfuscatedNames.OPNPC5_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPNPC5_WRITE1, ObfuscatedNames.OPNPC5_WRITE2},
                ObfuscatedNames.OPNPC5_WRITES,
                PacketType.OPNPC
        ));
    }

    // OPPLAYER packets (1-8)
    public PacketDef getOpPlayer1() {
        return cache.computeIfAbsent("OPPLAYER1", k -> new PacketDef(
                ObfuscatedNames.OPPLAYER1_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPPLAYER1_WRITE1, ObfuscatedNames.OPPLAYER1_WRITE2},
                ObfuscatedNames.OPPLAYER1_WRITES,
                PacketType.OPPLAYER
        ));
    }

    public PacketDef getOpPlayer2() {
        return cache.computeIfAbsent("OPPLAYER2", k -> new PacketDef(
                ObfuscatedNames.OPPLAYER2_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPPLAYER2_WRITE1, ObfuscatedNames.OPPLAYER2_WRITE2},
                ObfuscatedNames.OPPLAYER2_WRITES,
                PacketType.OPPLAYER
        ));
    }

    public PacketDef getOpPlayer3() {
        return cache.computeIfAbsent("OPPLAYER3", k -> new PacketDef(
                ObfuscatedNames.OPPLAYER3_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPPLAYER3_WRITE1, ObfuscatedNames.OPPLAYER3_WRITE2},
                ObfuscatedNames.OPPLAYER3_WRITES,
                PacketType.OPPLAYER
        ));
    }

    public PacketDef getOpPlayer4() {
        return cache.computeIfAbsent("OPPLAYER4", k -> new PacketDef(
                ObfuscatedNames.OPPLAYER4_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPPLAYER4_WRITE1, ObfuscatedNames.OPPLAYER4_WRITE2},
                ObfuscatedNames.OPPLAYER4_WRITES,
                PacketType.OPPLAYER
        ));
    }

    public PacketDef getOpPlayer5() {
        return cache.computeIfAbsent("OPPLAYER5", k -> new PacketDef(
                ObfuscatedNames.OPPLAYER5_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPPLAYER5_WRITE1, ObfuscatedNames.OPPLAYER5_WRITE2},
                ObfuscatedNames.OPPLAYER5_WRITES,
                PacketType.OPPLAYER
        ));
    }

    public PacketDef getOpPlayer6() {
        return cache.computeIfAbsent("OPPLAYER6", k -> new PacketDef(
                ObfuscatedNames.OPPLAYER6_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPPLAYER6_WRITE1, ObfuscatedNames.OPPLAYER6_WRITE2},
                ObfuscatedNames.OPPLAYER6_WRITES,
                PacketType.OPPLAYER
        ));
    }

    public PacketDef getOpPlayer7() {
        return cache.computeIfAbsent("OPPLAYER7", k -> new PacketDef(
                ObfuscatedNames.OPPLAYER7_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPPLAYER7_WRITE1, ObfuscatedNames.OPPLAYER7_WRITE2},
                ObfuscatedNames.OPPLAYER7_WRITES,
                PacketType.OPPLAYER
        ));
    }

    public PacketDef getOpPlayer8() {
        return cache.computeIfAbsent("OPPLAYER8", k -> new PacketDef(
                ObfuscatedNames.OPPLAYER8_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPPLAYER8_WRITE1, ObfuscatedNames.OPPLAYER8_WRITE2},
                ObfuscatedNames.OPPLAYER8_WRITES,
                PacketType.OPPLAYER
        ));
    }

    // Special operation packets with items
    public PacketDef getOpLocT() {
        return cache.computeIfAbsent("OPLOCT", k -> new PacketDef(
                ObfuscatedNames.OPLOCT_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPLOCT_WRITE1, ObfuscatedNames.OPLOCT_WRITE2,
                        ObfuscatedNames.OPLOCT_WRITE3, ObfuscatedNames.OPLOCT_WRITE4,
                        ObfuscatedNames.OPLOCT_WRITE5, ObfuscatedNames.OPLOCT_WRITE6,
                        ObfuscatedNames.OPLOCT_WRITE7},
                ObfuscatedNames.OPLOCT_WRITES,
                PacketType.OPLOCT
        ));
    }

    public PacketDef getOpNpcT() {
        return cache.computeIfAbsent("OPNPCT", k -> new PacketDef(
                ObfuscatedNames.OPNPCT_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPNPCT_WRITE1, ObfuscatedNames.OPNPCT_WRITE2,
                        ObfuscatedNames.OPNPCT_WRITE3, ObfuscatedNames.OPNPCT_WRITE4,
                        ObfuscatedNames.OPNPCT_WRITE5},
                ObfuscatedNames.OPNPCT_WRITES,
                PacketType.OPNPCT
        ));
    }

    public PacketDef getOpPlayerT() {
        return cache.computeIfAbsent("OPPLAYERT", k -> new PacketDef(
                ObfuscatedNames.OPPLAYERT_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPPLAYERT_WRITE1, ObfuscatedNames.OPPLAYERT_WRITE2,
                        ObfuscatedNames.OPPLAYERT_WRITE3, ObfuscatedNames.OPPLAYERT_WRITE4,
                        ObfuscatedNames.OPPLAYERT_WRITE5},
                ObfuscatedNames.OPPLAYERT_WRITES,
                PacketType.OPPLAYERT
        ));
    }

    public PacketDef getOpObjT() {
        return cache.computeIfAbsent("OPOBJT", k -> new PacketDef(
                ObfuscatedNames.OPOBJT_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPOBJT_WRITE1, ObfuscatedNames.OPOBJT_WRITE2,
                        ObfuscatedNames.OPOBJT_WRITE3, ObfuscatedNames.OPOBJT_WRITE4,
                        ObfuscatedNames.OPOBJT_WRITE5, ObfuscatedNames.OPOBJT_WRITE6,
                        ObfuscatedNames.OPOBJT_WRITE7},
                ObfuscatedNames.OPOBJT_WRITES,
                PacketType.OPOBJT
        ));
    }

    // Interface/Widget packets
    public PacketDef getIfButtonT() {
        return cache.computeIfAbsent("IF_BUTTONT", k -> new PacketDef(
                ObfuscatedNames.IF_BUTTONT_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.IF_BUTTONT_WRITE1, ObfuscatedNames.IF_BUTTONT_WRITE2,
                        ObfuscatedNames.IF_BUTTONT_WRITE3, ObfuscatedNames.IF_BUTTONT_WRITE4,
                        ObfuscatedNames.IF_BUTTONT_WRITE5, ObfuscatedNames.IF_BUTTONT_WRITE6},
                ObfuscatedNames.IF_BUTTONT_WRITES,
                PacketType.IF_BUTTONT
        ));
    }

    public PacketDef getIfButtonX() {
        return cache.computeIfAbsent("IF_BUTTONX", k -> new PacketDef(
                ObfuscatedNames.IF_BUTTONX_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.IF_BUTTONX_WRITE1, ObfuscatedNames.IF_BUTTONX_WRITE2,
                        ObfuscatedNames.IF_BUTTONX_WRITE3, ObfuscatedNames.IF_BUTTONX_WRITE4},
                ObfuscatedNames.IF_BUTTONX_WRITES,
                PacketType.IF_BUTTONX
        ));
    }

    public PacketDef getIfSubOp() {
        return cache.computeIfAbsent("IF_SUBOP", k -> new PacketDef(
                ObfuscatedNames.IF_SUBOP_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.IF_SUBOP_WRITE1, ObfuscatedNames.IF_SUBOP_WRITE2,
                        ObfuscatedNames.IF_SUBOP_WRITE3, ObfuscatedNames.IF_SUBOP_WRITE4,
                        ObfuscatedNames.IF_SUBOP_WRITE5},
                ObfuscatedNames.IF_SUBOP_WRITES,
                PacketType.IF_SUBOP
        ));
    }

    public PacketDef getOpHeldd() {
        return cache.computeIfAbsent("OPHELDD", k -> new PacketDef(
                ObfuscatedNames.OPHELDD_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.OPHELDD_WRITE1, ObfuscatedNames.OPHELDD_WRITE2,
                        ObfuscatedNames.OPHELDD_WRITE3, ObfuscatedNames.OPHELDD_WRITE4,
                        ObfuscatedNames.OPHELDD_WRITE5, ObfuscatedNames.OPHELDD_WRITE6},
                ObfuscatedNames.OPHELDD_WRITES,
                PacketType.OPHELDD
        ));
    }

    // Resume packets for dialogs
    public PacketDef getResumePausebutton() {
        return cache.computeIfAbsent("RESUME_PAUSEBUTTON", k -> new PacketDef(
                ObfuscatedNames.RESUME_PAUSEBUTTON_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.RESUME_PAUSEBUTTON_WRITE1,
                        ObfuscatedNames.RESUME_PAUSEBUTTON_WRITE2},
                ObfuscatedNames.RESUME_PAUSEBUTTON_WRITES,
                PacketType.RESUME_PAUSEBUTTON
        ));
    }

    public PacketDef getResumeCountDialog() {
        return cache.computeIfAbsent("RESUME_COUNTDIALOG", k -> new PacketDef(
                ObfuscatedNames.RESUME_COUNTDIALOG_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.RESUME_COUNTDIALOG_WRITE1},
                ObfuscatedNames.RESUME_COUNTDIALOG_WRITES,
                PacketType.RESUME_COUNTDIALOG
        ));
    }

    public PacketDef getResumeObjDialog() {
        return cache.computeIfAbsent("RESUME_OBJDIALOG", k -> new PacketDef(
                ObfuscatedNames.RESUME_OBJDIALOG_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.RESUME_OBJDIALOG_WRITE1},
                ObfuscatedNames.RESUME_OBJDIALOG_WRITES,
                PacketType.RESUME_OBJDIALOG
        ));
    }

    public PacketDef getResumeNameDialog() {
        return cache.computeIfAbsent("RESUME_NAMEDIALOG", k -> new PacketDef(
                ObfuscatedNames.RESUME_NAMEDIALOG_OBFUSCATED_NAME,
                new String[]{ObfuscatedNames.RESUME_NAMEDIALOG_WRITE1,
                        ObfuscatedNames.RESUME_NAMEDIALOG_WRITE2},
                ObfuscatedNames.RESUME_NAMEDIALOG_WRITES,
                PacketType.RESUME_NAMEDIALOG
        ));
    }

    public PacketDef getResumeStringDialog() {
        return cache.computeIfAbsent("RESUME_STRINGDIALOG", k -> new PacketDef(
                ObfuscatedNames.RESUME_STRINGDIALOG_OBFUSCATED_NAME,
                new String[]{ObfuscatedNames.RESUME_STRINGDIALOG_WRITE1,
                        ObfuscatedNames.RESUME_STRINGDIALOG_WRITE2},
                ObfuscatedNames.RESUME_STRINGDIALOG_WRITES,
                PacketType.RESUME_STRINGDIALOG
        ));
    }

    // Movement and event packets
    public PacketDef getMoveGameClick() {
        return cache.computeIfAbsent("MOVE_GAMECLICK", k -> new PacketDef(
                ObfuscatedNames.MOVE_GAMECLICK_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.MOVE_GAMECLICK_WRITE1, ObfuscatedNames.MOVE_GAMECLICK_WRITE2,
                        ObfuscatedNames.MOVE_GAMECLICK_WRITE3, ObfuscatedNames.MOVE_GAMECLICK_WRITE4},
                ObfuscatedNames.MOVE_GAMECLICK_WRITES,
                PacketType.MOVE_GAMECLICK
        ));
    }

    public PacketDef getEventMouseClick() {
        return cache.computeIfAbsent("EVENT_MOUSE_CLICK", k -> new PacketDef(
                ObfuscatedNames.EVENT_MOUSE_CLICK_OBFUSCATEDNAME,
                new String[]{ObfuscatedNames.EVENT_MOUSE_CLICK_WRITE1, ObfuscatedNames.EVENT_MOUSE_CLICK_WRITE2,
                        ObfuscatedNames.EVENT_MOUSE_CLICK_WRITE3, ObfuscatedNames.EVENT_MOUSE_CLICK_WRITE4},
                ObfuscatedNames.EVENT_MOUSE_CLICK_WRITES,
                PacketType.EVENT_MOUSE_CLICK
        ));
    }

    // Optional: Helper method to get packet by action number
    public PacketDef getOpObj(int action) {
        switch (action) {
            case 1: return getOpObj1();
            case 2: return getOpObj2();
            case 3: return getOpObj3();
            case 4: return getOpObj4();
            case 5: return getOpObj5();
            default: throw new IllegalArgumentException("Invalid OPOBJ action: " + action);
        }
    }

    public PacketDef getOpLoc(int action) {
        switch (action) {
            case 1: return getOpLoc1();
            case 2: return getOpLoc2();
            case 3: return getOpLoc3();
            case 4: return getOpLoc4();
            case 5: return getOpLoc5();
            default: throw new IllegalArgumentException("Invalid OPLOC action: " + action);
        }
    }

    public PacketDef getOpNpc(int action) {
        switch (action) {
            case 1: return getOpNpc1();
            case 2: return getOpNpc2();
            case 3: return getOpNpc3();
            case 4: return getOpNpc4();
            case 5: return getOpNpc5();
            default: throw new IllegalArgumentException("Invalid OPNPC action: " + action);
        }
    }

    public PacketDef getOpPlayer(int action) {
        switch (action) {
            case 1: return getOpPlayer1();
            case 2: return getOpPlayer2();
            case 3: return getOpPlayer3();
            case 4: return getOpPlayer4();
            case 5: return getOpPlayer5();
            case 6: return getOpPlayer6();
            case 7: return getOpPlayer7();
            case 8: return getOpPlayer8();
            default: throw new IllegalArgumentException("Invalid OPPLAYER action: " + action);
        }
    }

    // Clear cache if needed (useful for testing or when obfuscation changes)
    public void clearCache() {
        cache.clear();
    }
}
