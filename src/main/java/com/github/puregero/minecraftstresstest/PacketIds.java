package com.github.puregero.minecraftstresstest;

public final class PacketIds {

    // Clientbound
    public final int PLAY_DISCONNECT;
    public final int PLAY_KEEP_ALIVE;
    public final int PLAY_SYNCHRONIZE_PLAYER_POSITION;
    public final int PLAY_RESOURCE_PACK;
    public final int PLAY_SET_HEALTH;
    public final int PLAY_PING;

    // Serverbound
    public final int PLAY_CONFIRM_TELEPORTATION;
    public final int PLAY_CLIENT_COMMAND;
    public final int PLAY_KEEP_ALIVE_RESPONSE;
    public final int PLAY_SET_PLAYER_POSITION_AND_ROTATION;
    public final int PLAY_PONG;
    public final int PLAY_RESOURCE_PACK_RESPONSE;

    public PacketIds(int protocolVersion) {
        if (protocolVersion >= 773) {
            PLAY_DISCONNECT = 0x20;
            PLAY_KEEP_ALIVE = 0x2B;
            PLAY_SYNCHRONIZE_PLAYER_POSITION = 0x46;
            PLAY_RESOURCE_PACK = 0x4F;
            PLAY_SET_HEALTH = 0x66;
            PLAY_PING = 0x3B;
        } else if (protocolVersion >= 770) {
            PLAY_DISCONNECT = 0x1C;
            PLAY_KEEP_ALIVE = 0x26;
            PLAY_SYNCHRONIZE_PLAYER_POSITION = 0x41;
            PLAY_RESOURCE_PACK = 0x4A;
            PLAY_SET_HEALTH = 0x61;
            PLAY_PING = 0x37;
        } else if (protocolVersion >= 768) {
            PLAY_DISCONNECT = 0x1D;
            PLAY_KEEP_ALIVE = 0x27;
            PLAY_SYNCHRONIZE_PLAYER_POSITION = 0x42;
            PLAY_RESOURCE_PACK = 0x4B;
            PLAY_SET_HEALTH = 0x62;
            PLAY_PING = 0x38;
        } else {
            PLAY_DISCONNECT = 0x1D;
            PLAY_KEEP_ALIVE = 0x26;
            PLAY_SYNCHRONIZE_PLAYER_POSITION = 0x40;
            PLAY_RESOURCE_PACK = 0x46;
            PLAY_SET_HEALTH = 0x5D;
            PLAY_PING = 0x35;
        }

        PLAY_CONFIRM_TELEPORTATION = 0x00;
        if (protocolVersion >= 771) {
            PLAY_CLIENT_COMMAND = 0x0B;
            PLAY_KEEP_ALIVE_RESPONSE = 0x1B;
            PLAY_SET_PLAYER_POSITION_AND_ROTATION = 0x1E;
            PLAY_PONG = 0x2C;
            PLAY_RESOURCE_PACK_RESPONSE = 0x30;
        } else if (protocolVersion >= 768) {
            PLAY_CLIENT_COMMAND = 0x0A;
            PLAY_KEEP_ALIVE_RESPONSE = 0x1A;
            PLAY_SET_PLAYER_POSITION_AND_ROTATION = 0x1D;
            PLAY_PONG = 0x2B;
            PLAY_RESOURCE_PACK_RESPONSE = 0x2F;
        } else {
            PLAY_CLIENT_COMMAND = 0x09;
            PLAY_KEEP_ALIVE_RESPONSE = 0x18;
            PLAY_SET_PLAYER_POSITION_AND_ROTATION = 0x1B;
            PLAY_PONG = 0x27;
            PLAY_RESOURCE_PACK_RESPONSE = 0x2B;
        }
    }

    public static final class Clientbound {
        private Clientbound() {}

        public static final class Login {
            private Login() {}
            public static final int
                    DISCONNECT = 0x00,
                    ENCRYPTION_REQUEST = 0x01,
                    LOGIN_SUCCESS = 0x02,
                    SET_COMPRESSION = 0x03;
        }

        public static final class Configuration {
            private Configuration() {}
            public static final int
                    DISCONNECT = 0x02,
                    FINISH_CONFIGURATION = 0x03,
                    KEEP_ALIVE = 0x04,
                    PING = 0x05,
                    KNOWN_PACKS = 0x0E;
        }
    }

    public static final class Serverbound {
        private Serverbound() {}

        public static final class Handshaking {
            private Handshaking() {}
            public static final int HANDSHAKE = 0x00;
        }

        public static final class Login {
            private Login() {}
            public static final int
                    LOGIN_START = 0x00,
                    LOGIN_ACKNOWLEDGED = 0x03;
        }

        public static final class Configuration {
            private Configuration() {}
            public static final int
                    CLIENT_INFORMATION = 0x00,
                    FINISH_CONFIGURATION = 0x03,
                    KEEP_ALIVE = 0x04,
                    PONG = 0x05,
                    KNOWN_PACKS = 0x07;
        }
    }
}
