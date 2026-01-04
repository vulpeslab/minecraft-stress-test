package com.github.puregero.minecraftstresstest;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


public class Bot extends ChannelInboundHandlerAdapter {
    // Minecraft version to protocol version mapping
    // https://minecraft.wiki/w/Protocol_version
    private static final Map<String, Integer> VERSION_TO_PROTOCOL = Map.ofEntries(
            Map.entry("1.20.6", 766),
            Map.entry("1.21", 767),
            Map.entry("1.21.1", 767),
            Map.entry("1.21.2", 768),
            Map.entry("1.21.3", 768),
            Map.entry("1.21.4", 769),
            Map.entry("1.21.5", 770),
            Map.entry("1.21.6", 771),
            Map.entry("1.21.7", 772),
            Map.entry("1.21.8", 772),
            Map.entry("1.21.9", 773),
            Map.entry("1.21.10", 773),
            Map.entry("1.21.11", 774)
    );

    public static final String DEFAULT_VERSION = "1.21";
    private static final int PROTOCOL_VERSION = resolveProtocolVersion(System.getProperty("bot.version", DEFAULT_VERSION));
    private static final PacketIds PACKETS = new PacketIds(PROTOCOL_VERSION);

    private static int resolveProtocolVersion(String versionOrProtocol) {
        // First check if it's a known version string
        if (VERSION_TO_PROTOCOL.containsKey(versionOrProtocol)) {
            int protocol = VERSION_TO_PROTOCOL.get(versionOrProtocol);
            System.out.println("Using Minecraft " + versionOrProtocol + " (protocol " + protocol + ")");
            return protocol;
        }
        // Otherwise try to parse as protocol number
        try {
            int protocol = Integer.parseInt(versionOrProtocol);
            System.out.println("Using protocol version " + protocol);
            return protocol;
        } catch (NumberFormatException e) {
            System.err.println("Unknown version '" + versionOrProtocol + "'. Supported versions: " + VERSION_TO_PROTOCOL.keySet());
            System.err.println("Falling back to " + DEFAULT_VERSION);
            return VERSION_TO_PROTOCOL.get(DEFAULT_VERSION);
        }
    }
    private static final double CENTER_X = Double.parseDouble(System.getProperty("bot.x", "0"));
    private static final double CENTER_Z = Double.parseDouble(System.getProperty("bot.z", "0"));
    private static final boolean LOGS = Boolean.parseBoolean(System.getProperty("bot.logs", "true"));
    private static final boolean Y_AXIS = Boolean.parseBoolean(System.getProperty("bot.yaxis", "true"));
    private static final int VIEW_DISTANCE = Integer.parseInt(System.getProperty("bot.viewdistance", "2"));
    private static final int RESOURCE_PACK_RESPONSE = Integer.parseInt(System.getProperty("bot.resource.pack.response", "3"));

    private static final Executor ONE_TICK_DELAY = CompletableFuture.delayedExecutor(50, TimeUnit.MILLISECONDS);

    public static final String DEFAULT_SPEED = "0.1";
    public static double SPEED = Double.parseDouble(System.getProperty("bot.speed", DEFAULT_SPEED));
    public static final String DEFAULT_RADIUS = "1000";
    public static double RADIUS = Double.parseDouble(System.getProperty("bot.radius", DEFAULT_RADIUS));

    public SocketChannel channel;
    private String username;
    private final String address;
    private final int port;
    private UUID uuid;
    private boolean loginState = true;
    private boolean configState = false;
    private boolean playState = false;

    private double x = 0;
    private double y = 0;
    private double z = 0;
    private float yaw = (float) (Math.random() * 360);

    private boolean goUp = false;
    private boolean goDown = false;
    private boolean isSpawned = false;
    private boolean isDead = false;

    public Bot(String username, String address, int port) {
        this.username = username;
        this.address = address;
        this.port = port;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        sendPacket(ctx, PacketIds.Serverbound.Handshaking.HANDSHAKE, buffer -> {
            buffer.writeVarInt(PROTOCOL_VERSION);
            buffer.writeUtf(address);
            buffer.writeShort(port);
            buffer.writeVarInt(2);
        });

        sendPacket(ctx, PacketIds.Serverbound.Login.LOGIN_START, buffer -> {
            buffer.writeUtf(username);
            buffer.writeUUID(UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)));
        });
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        if (uuid != null) {
            System.out.println(username + " has disconnected from " + address + ":" + port);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            FriendlyByteBuf byteBuf = new FriendlyByteBuf((ByteBuf) msg);
            if (loginState) {
                channelReadLogin(ctx, byteBuf);

            } else if (configState) {
                channelReadConfig(ctx, byteBuf);

            } else if (playState) {
                channelReadPlay(ctx, byteBuf);
            }
        } finally {
            ((ByteBuf) msg).release();
        }
    }


    private void channelReadLogin(ChannelHandlerContext ctx, FriendlyByteBuf byteBuf) {
        int packetId = byteBuf.readVarInt();

        if (packetId == PacketIds.Clientbound.Login.DISCONNECT) {
            System.out.println(username + " was disconnected during login due to " + byteBuf.readUtf());
            ctx.close();

        } else if (packetId == PacketIds.Clientbound.Login.ENCRYPTION_REQUEST) {
            System.out.println("Server requesting for ENCRYPTION_REQUEST, so it is on ONLINEMODE, disconnecting");
            ctx.close();

        } else if (packetId == PacketIds.Clientbound.Login.LOGIN_SUCCESS) {

            if (PROTOCOL_VERSION >= 764) {
                sendPacket(ctx, PacketIds.Serverbound.Login.LOGIN_ACKNOWLEDGED, buffer -> {
                });
            }

            loggedIn(ctx, byteBuf);

        } else if (packetId == PacketIds.Clientbound.Login.SET_COMPRESSION) {
            byteBuf.readVarInt();
            ctx.pipeline().addAfter("packetDecoder", "compressionDecoder", new CompressionDecoder());
            ctx.pipeline().addAfter("packetEncoder", "compressionEncoder", new CompressionEncoder());
        } else {
            throw new RuntimeException("Unknown login packet id of " + packetId);
        }
    }


    private void loggedIn(ChannelHandlerContext ctx, FriendlyByteBuf byteBuf) {
        UUID uuid = byteBuf.readUUID();
        String username = byteBuf.readUtf();
        int numberElements = byteBuf.readVarInt(); //number of elements after this position
        boolean isSigned = false;

        if (numberElements > 0) {
            try {
                byteBuf.readUtf(); //name
                byteBuf.readUtf(); //value
                isSigned = byteBuf.readBoolean(); //issigned
            } catch (Exception e) {
            }
        }

        this.uuid = uuid;
        this.username = username;

        if (isSigned) {
            System.out.println(username + " (" + uuid + ") has logged in on an ONLINEMODE server, stopping");
            ctx.close();
            return;
        } else
            System.out.println(username + " (" + uuid + ") has logged in");

        loginState = false;
        configState = true;

        CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS).execute(() -> {
            if (configState) {
                sendPacket(ctx, PacketIds.Serverbound.Configuration.CLIENT_INFORMATION, buffer -> {
                    buffer.writeUtf("en_GB");
                    buffer.writeByte(VIEW_DISTANCE);
                    buffer.writeVarInt(0);
                    buffer.writeBoolean(true);
                    buffer.writeByte(0);
                    buffer.writeVarInt(0);
                    buffer.writeBoolean(false);
                    buffer.writeBoolean(true);
                    if (PROTOCOL_VERSION >= 773) {
                        buffer.writeVarInt(0);
                    }
                });

                sendPacket(ctx, PacketIds.Serverbound.Configuration.KNOWN_PACKS, buffer -> {
                    buffer.writeVarInt(0);
                });
            }

            CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS).execute(() -> tick(ctx));
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }


    private void tick(ChannelHandlerContext ctx) {

        if (!ctx.channel().isActive()) return;

        ONE_TICK_DELAY.execute(() -> tick(ctx));

        if (!isSpawned) return; // Don't tick until we've spawned in

        if (!Y_AXIS && (goUp || goDown)) {
            goDown = goUp = false;
            if (Math.random() < 0.1) yaw = (float) (Math.random() * 360);
        }

        if (goUp) {
            y += 0.1;
            goUp = Math.random() < 0.98;
        } else if (goDown) {
            y -= 0.1;
            goDown = Math.random() < 0.98;
        } else {
            if (Math.max(Math.abs(x - CENTER_X), Math.abs(z - CENTER_Z)) > RADIUS) {
                double tx = Math.random() * RADIUS * 2 - RADIUS + CENTER_X;
                double tz = Math.random() * RADIUS * 2 - RADIUS + CENTER_Z;

                yaw = (float) Math.toDegrees(Math.atan2(x - tx, tz - z));
            }

            x += SPEED * -Math.sin(Math.toRadians(yaw));
            z += SPEED * Math.cos(Math.toRadians(yaw));
        }

        if (Y_AXIS) {
            y -= SPEED / 10;
        }

        sendPacket(ctx, PACKETS.PLAY_SET_PLAYER_POSITION_AND_ROTATION, buffer -> {
            buffer.writeDouble(x);
            buffer.writeDouble(y);
            buffer.writeDouble(z);
            buffer.writeFloat(yaw);
            buffer.writeFloat(0);
            buffer.writeBoolean(true);
        });
    }


    private void channelReadConfig(ChannelHandlerContext ctx, FriendlyByteBuf byteBuf) {
        int packetId = byteBuf.readVarInt();

        if (packetId == PacketIds.Clientbound.Configuration.DISCONNECT) {
            System.out.println(username + " (" + uuid + ") (config) was kicked due to " + byteBuf.readUtf());
            ctx.close();

        } else if (packetId == PacketIds.Clientbound.Configuration.FINISH_CONFIGURATION) {

            sendPacket(ctx, PacketIds.Serverbound.Configuration.FINISH_CONFIGURATION, buffer -> {
            });

            configState = false;
            playState = true;

        } else if (packetId == PacketIds.Clientbound.Configuration.KEEP_ALIVE) {
            long id = byteBuf.readLong();
            sendPacket(ctx, PacketIds.Serverbound.Configuration.KEEP_ALIVE, buffer -> buffer.writeLong(id));

        } else if (packetId == PacketIds.Clientbound.Configuration.PING) {
            int id = byteBuf.readInt();
            sendPacket(ctx, PacketIds.Serverbound.Configuration.PONG, buffer -> buffer.writeInt(id));

        } else if (packetId == PacketIds.Clientbound.Configuration.KNOWN_PACKS) {
            int packCount = byteBuf.readVarInt();
            java.util.List<String[]> packs = new java.util.ArrayList<>();
            for (int i = 0; i < packCount; i++) {
                packs.add(new String[]{byteBuf.readUtf(), byteBuf.readUtf(), byteBuf.readUtf()});
            }
            sendPacket(ctx, PacketIds.Serverbound.Configuration.KNOWN_PACKS, buffer -> {
                buffer.writeVarInt(packs.size());
                for (String[] pack : packs) {
                    buffer.writeUtf(pack[0]);
                    buffer.writeUtf(pack[1]);
                    buffer.writeUtf(pack[2]);
                }
            });
        }
    }


    private void channelReadPlay(ChannelHandlerContext ctx, FriendlyByteBuf byteBuf) {
        int packetId = byteBuf.readVarInt();

        if (LOGS) {
            System.out.println(username + " play packet: 0x" + Integer.toHexString(packetId) + " (" + byteBuf.readableBytes() + " bytes)");
        }

        if (packetId == PACKETS.PLAY_DISCONNECT) {
            System.out.println(username + " (" + uuid + ") was kicked due to " + byteBuf.readUtf());
            ctx.close();
            loginState = true;
            playState = false;

        } else if (packetId == PACKETS.PLAY_KEEP_ALIVE) {
            if (byteBuf.readableBytes() >= 8) {
                long id = byteBuf.readLong();
                sendPacket(ctx, PACKETS.PLAY_KEEP_ALIVE_RESPONSE, buffer -> buffer.writeLong(id));
            }

        } else if (packetId == PACKETS.PLAY_PING) {
            if (byteBuf.readableBytes() >= 4) {
                int id = byteBuf.readInt();
                sendPacket(ctx, PACKETS.PLAY_PONG, buffer -> buffer.writeInt(id));
            }

        } else if (packetId == PACKETS.PLAY_SYNCHRONIZE_PLAYER_POSITION) {
            int minBytes = PROTOCOL_VERSION >= 768 ? 61 : 33;
            if (byteBuf.readableBytes() < minBytes) {
                return;
            }

            int id;
            double px, py, pz;
            float pyaw;
            int flags;

            if (PROTOCOL_VERSION >= 768) {
                id = byteBuf.readVarInt();
                px = byteBuf.readDouble();
                py = byteBuf.readDouble();
                pz = byteBuf.readDouble();
                byteBuf.readDouble();
                byteBuf.readDouble();
                byteBuf.readDouble();
                pyaw = byteBuf.readFloat();
                byteBuf.readFloat();
                flags = byteBuf.readInt();
            } else {
                px = byteBuf.readDouble();
                py = byteBuf.readDouble();
                pz = byteBuf.readDouble();
                pyaw = byteBuf.readFloat();
                byteBuf.readFloat();
                flags = byteBuf.readByte();
                id = byteBuf.readVarInt();
            }

            x = (flags & 0x01) == 0x01 ? x + px : px;
            y = (flags & 0x02) == 0x02 ? y + py : py;
            z = (flags & 0x04) == 0x04 ? z + pz : pz;
            yaw = (flags & 0x08) == 0x08 ? yaw + pyaw : pyaw;

            if (LOGS) {
                System.out.println("Teleporting " + username + " to " + x + "," + y + "," + z);
            }

            if (goDown) {
                goDown = false;
            } else if (!goUp) {
                goUp = true;
            } else {
                goUp = false;
                goDown = Math.random() < 0.5;
                if (!goDown) yaw = (float) (Math.random() * 360);
            }

            sendPacket(ctx, PACKETS.PLAY_CONFIRM_TELEPORTATION, buffer -> buffer.writeVarInt(id));

            if (!isDead) {
                isSpawned = true;
            }

        } else if (packetId == PACKETS.PLAY_RESOURCE_PACK) {
            if (byteBuf.readableBytes() >= 16) {
                UUID packUuid = byteBuf.readUUID();
                String url = byteBuf.readUtf();
                byteBuf.readUtf();
                byteBuf.readBoolean();
                if (byteBuf.readBoolean()) byteBuf.readUtf();
                if (LOGS) {
                    System.out.println("Resource pack: " + url);
                }

                sendPacket(ctx, PACKETS.PLAY_RESOURCE_PACK_RESPONSE, buffer -> {
                    buffer.writeUUID(packUuid);
                    buffer.writeVarInt(RESOURCE_PACK_RESPONSE);
                });
            }

        } else if (packetId == PACKETS.PLAY_SET_HEALTH) {
            if (byteBuf.readableBytes() >= 4) {
                float health = byteBuf.readFloat();

                if (health <= 0) {
                    if (!isDead) {
                        isDead = true;
                        isSpawned = false;
                        if (LOGS) {
                            System.out.println(username + " died, attempting to respawn...");
                        }

                        CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS).execute(() -> {
                            sendPacket(ctx, PACKETS.PLAY_CLIENT_COMMAND, buffer -> buffer.writeVarInt(0));
                        });
                    }
                } else if (isDead) {
                    isDead = false;
                    isSpawned = true;
                    if (LOGS) {
                        System.out.println(username + " has respawned with health " + health);
                    }
                }
            }
        }
    }


    public void close() {
        channel.close();
    }


    public void sendPacket(ChannelHandlerContext ctx, int packetId, Consumer<FriendlyByteBuf> applyToBuffer) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(ctx.alloc().buffer());
        buffer.writeVarInt(packetId);
        applyToBuffer.accept(buffer);
        ctx.writeAndFlush(buffer);
    }
}