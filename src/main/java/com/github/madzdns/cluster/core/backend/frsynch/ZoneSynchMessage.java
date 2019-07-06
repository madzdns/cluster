package com.github.madzdns.cluster.core.backend.frsynch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

import com.frfra.frsynch.codec.IMessage;

import com.github.madzdns.cluster.core.backend.MetaData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ZoneSynchMessage implements IMessage {
    public final static byte TYPE_BAD_KEY = 0;
    public final static byte TYPE_BAD_SEQ = 1;
    public final static byte TYPE_BAD_ID = 2;
    public final static byte TYPE_OK = 3;
    public final static byte TYPE_FULL_CHECK = 4;
    public final static byte TYPE_CHECK = 5;
    public final static byte TYPE_NOT_VALID_EDGE = 6;
    public final static byte TYPE_BOTH_STARTUP = 7;

    public final static byte COMMAND_TAKE_THis = 0;
    public final static byte COMMAND_GIVE_THis = 1;
    public final static byte COMMAND_DEL_THis = 2;
    public final static byte COMMAND_OK = 3;
    public final static byte COMMAND_RCPT_THis = 4;

    public final static byte SCHEDULED = 1;
    public final static byte NOT_SCHEDULED = 0;

    private byte[] content;

    private String zonename;

    private long lastModified;

    private byte command;

    private byte schedule;

    public short getContentLen() {

        return (short) (content == null ? 0 : content.length);
    }

    public byte[] getContent() {

        return content;
    }

    public String getZoneName() {

        return zonename;
    }

    public long getLastModified() {

        return lastModified;
    }

    public byte getCommand() {

        return command;
    }

    public byte getSchedule() {

        return schedule;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("Zone=")
                .append(zonename);

        return sb.toString();
    }


    public ZoneSynchMessage() {

    }

    public ZoneSynchMessage(final byte[] content, final String name,
                            final long modified, final byte command,
                            final short schedule) {
        this.content = content;
        this.zonename = name;
        this.lastModified = modified;
        this.command = command;
        this.schedule = schedule > MetaData.NOT_SYNCHED ? SCHEDULED : NOT_SCHEDULED;
    }

    @Override
    public byte[] serialize() {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            DataOutputStream out = new DataOutputStream(stream);
            out.writeUTF(getKey());
            out.writeLong(getVersion());
            if (content != null) {
                out.writeInt(content.length);
                out.write(content);
            } else {
                out.writeInt(0);
            }
            out.writeByte(getCommand());
            out.writeByte(getSchedule());
            return stream.toByteArray();
        } catch (Exception e) {
            log.error("", e);
            return null;
        }
    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> config) {

    }

    @Override
    public void deserialize(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            zonename = in.readUTF();
            lastModified = in.readLong();
            int len = in.readInt();
            content = null;
            if (len > 0) {
                content = new byte[len];
                in.read(content);
            }
            command = in.readByte();
            schedule = in.readByte();

        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    public String getKey() {

        return zonename;
    }

    @Override
    public long getVersion() {

        return lastModified;
    }
}
