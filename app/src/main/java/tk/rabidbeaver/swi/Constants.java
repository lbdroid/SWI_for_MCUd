package tk.rabidbeaver.swi;

public class Constants {
    interface ACTIONTYPES {
        int NULL = 0;
        int BROADCAST_INTENT = 1;
        int ACTIVITY_INTENT = 2;
        int KEYCODE = 3;
    }

    interface MCUDCOMMANDS {
        byte start_detect[] = {(byte)0xaa, 0x55, 0x02, 0x01, 0x01, 0x02};
        byte stop_detect[] = {(byte)0xaa, 0x55, 0x02, 0x01, 0x00, 0x03};
        byte clear[] = {(byte)0xaa, 0x55, 0x01, 0x02, 0x03};
        byte save[] = {(byte)0xaa, 0x55, 0x01, 0x03, 0x02};
    }
}
