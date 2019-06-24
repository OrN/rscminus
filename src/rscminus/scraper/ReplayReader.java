/**
 * rscminus
 *
 * This file is part of rscminus.
 *
 * rscminus is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * rscminus is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with rscminus. If not,
 * see <http://www.gnu.org/licenses/>.
 *
 * Authors: see <https://github.com/OrN/rscminus>
 */

package rscminus.scraper;

import rscminus.common.ISAACCipher;
import rscminus.common.MathUtil;
import rscminus.common.Sleep;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class ReplayReader {
    private byte[] m_data;
    private Queue<Integer> m_timestamps = new LinkedList<Integer>();
    LinkedList<Integer> m_disconnectOffsets = new LinkedList<Integer>();

    private static final byte[] m_outputDisconnectPattern = {
            0x00, 0x01, 0x00, 0x00, 0x00, (byte)0xEB
    };

    // Reader state
    private boolean m_loggedIn;
    private boolean m_forceQuit;
    private boolean m_outgoing;
    private int m_position;
    private LinkedList<ReplayKeyPair> m_keys;
    private int m_keyIndex;
    private ISAACCipher isaac = new ISAACCipher();

    public static final int TIMESTAMP_EOF = -1;

    public int getDataPosition() {
        return m_position;
    }

    public int getDataSize() {
        return m_data.length;
    }

    public void open(File f, ReplayVersion replayVersion, LinkedList<ReplayKeyPair> keys, boolean outgoing) throws IOException {
        // Allocate space for data without replay headers
        m_data = new byte[calculateSize(f)];
        m_outgoing = outgoing;

        // Read replay data
        DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(f))));
        int timestamp = 0;
        int lastTimestamp = timestamp;
        int offset = 0;
        int lastOffset = offset;
        LinkedHashMap<Integer,Integer> timestamps = new LinkedHashMap<Integer,Integer>();
        while ((timestamp = in.readInt()) != TIMESTAMP_EOF) {
            timestamps.put(offset, timestamp);

            // Handle v0 disconnect
            if (replayVersion.version == 0 && (timestamp - lastTimestamp) > 400)
                m_disconnectOffsets.add(offset);

            lastOffset = offset;
            int length = in.readInt();
            if (length > 0) {
                in.read(m_data, offset, length);
                offset += length;
            }
            if (length == -1 && replayVersion.version != 0) {
                m_disconnectOffsets.add(offset);
            }
            lastTimestamp = timestamp;
        }

        in.close();

        m_loggedIn = false;
        m_position = 0;
        m_keys = keys;
        m_keyIndex = -1;

        // Map timestamps for faster import
        Iterator<Map.Entry<Integer, Integer>> iterator = timestamps.entrySet().iterator();
        Map.Entry<Integer, Integer> entry = iterator.next();
        int timestampOffset = 0;
        while (!isEOF()) {
            // Handle disconnect
            if (m_disconnectOffsets.contains(m_position)) {
                m_loggedIn = false;
            }

            if (m_loggedIn || m_outgoing) {
                int length = readPacketLength();
                skip(length);
                timestampOffset = m_position - 1;
            } else {
                timestampOffset = m_position;
                skip(1);
                m_loggedIn = true;
            }

            while (timestampOffset >= entry.getKey()) {
                timestamp = entry.getValue();
                if (!iterator.hasNext()) {
                    break;
                }
                entry = iterator.next();
            }

            // Add timestamp to FIFO
            m_timestamps.add(timestamp);
        }
        m_position = 0;
        m_loggedIn = false;

        // Build disconnect map for out.bin because we didn't handle it
        // We detect the login information packet
        if (outgoing) {
            while (!isEOF()) {
                if (binarySearch(m_outputDisconnectPattern)) {
                    m_disconnectOffsets.add(m_position);
                }
                skip(1);
            }
            m_position = 0;
        }
    }

    private boolean binarySearch(byte[] pattern) {
        for (int i = 0; i < pattern.length; i++) {
            int offset = m_position + i;
            if (offset >= m_data.length || m_data[offset] != pattern[i])
                return false;
        }
        return true;
    }

    private boolean verifyLogin() {
        boolean success = true;
        int originalPosition = m_position;
        ReplayPacket packet;
        packet = readPacket(true);
        if (packet == null || packet.opcode != 51)
            success = false;
        packet = readPacket(true);
        if (packet == null || packet.opcode != 131)
            success = false;
        packet = readPacket(true);
        if (packet == null || packet.opcode != 240)
            success = false;
        packet = readPacket(true);
        if (packet == null || packet.opcode != 25)
            success = false;
        packet = readPacket(true);
        if (packet == null || packet.opcode != 5)
            success = false;
        packet = readPacket(true);
        if (packet == null || packet.opcode != 111)
            success = false;
        packet = readPacket(true);
        if (packet == null || packet.opcode != 182)
            success = false;
        m_position = originalPosition;
        return success;
    }

    public ReplayPacket readPacket(boolean peek) {
        if (isEOF() || m_forceQuit)
            return null;

        int packetTimestamp;
        if (peek)
            packetTimestamp = m_timestamps.peek();
        else
            packetTimestamp = m_timestamps.poll();

        // Check for disconnect for outgoing (workaround)
        if (m_outgoing) {
            int oldPosition = m_position;
            readPacketLength();
            if (m_disconnectOffsets.contains(m_position))
                m_loggedIn = false;
            m_position = oldPosition;
        } else {
            // Handle disconnect
            if (m_disconnectOffsets.contains(m_position)) {
                m_loggedIn = false;
            }
        }

        try {
            ReplayPacket replayPacket = new ReplayPacket();
            if (!m_loggedIn) {
                if (m_outgoing) {
                    int length = readPacketLength();
                    if (length > 1) {
                        int dataLength = length - 1;
                        replayPacket.data = new byte[dataLength];
                        if (length < 160) {
                            replayPacket.data[dataLength - 1] = readByte();
                            replayPacket.opcode = readUnsignedByte();
                            if (dataLength > 1)
                                read(replayPacket.data, 0, dataLength - 1);
                        } else {
                            replayPacket.opcode = readUnsignedByte();
                            read(replayPacket.data, 0, dataLength);
                        }
                    } else {
                        replayPacket.data = null;
                        replayPacket.opcode = readUnsignedByte();
                    }
                    replayPacket.timestamp = packetTimestamp;

                    if (replayPacket.opcode != 0) {
                        System.out.println("ERROR: Invalid outgoing login packet: " + replayPacket.opcode);
                        return null;
                    }

                    // Set isaac keys
                    isaac.reset();
                    isaac.setKeys(m_keys.get(++m_keyIndex).keys);

                    replayPacket.opcode = ReplayEditor.VIRTUAL_OPCODE_CONNECT;

                    m_loggedIn = true;
                } else {
                    // Handle login response
                    int loginResponse = readUnsignedByte();
                    if ((loginResponse & 64) != 0) {
                        // Set isaac keys
                        isaac.reset();
                        isaac.setKeys(m_keys.get(++m_keyIndex).keys);
                        m_loggedIn = true;
                    } else {
                        m_forceQuit = true;
                    }

                    // Create virtual connect packet
                    replayPacket.opcode = ReplayEditor.VIRTUAL_OPCODE_CONNECT;
                    replayPacket.data = new byte[1];
                    replayPacket.data[0] = (byte) loginResponse;

                    // Set timestamp
                    replayPacket.timestamp = packetTimestamp;

                    boolean success = verifyLogin();
                    if (!success) {
                        System.out.println("ERROR: Invalid incoming login packet: " + replayPacket.opcode);
                        return null;
                    }
                }
            } else {
                int length = readPacketLength();
                if (length > 1) {
                    int dataLength = length - 1;
                    long start = System.currentTimeMillis();
                    replayPacket.data = new byte[dataLength];
                    if (length < 160) {
                        replayPacket.data[dataLength - 1] = readByte();
                        replayPacket.opcode = readUnsignedByte();
                        if (dataLength > 1)
                            read(replayPacket.data, 0, dataLength - 1);
                    } else {
                        replayPacket.opcode = readUnsignedByte();
                        read(replayPacket.data, 0, dataLength);
                    }
                } else {
                    replayPacket.data = null;
                    replayPacket.opcode = readUnsignedByte();
                }
                replayPacket.opcode = (replayPacket.opcode - isaac.getNextValue()) & 0xFF;
                replayPacket.timestamp = packetTimestamp;
            }
            return replayPacket;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isEOF() {
        return (m_position >= m_data.length);
    }

    private void read(byte[] data, int offset, int length) {
        int maxLength = Math.min(m_data.length - m_position, length);
        if (maxLength != length)
            System.out.println("WARNING: Copy is out of bounds");
        System.arraycopy(m_data, m_position, data, offset, length);
        m_position += length;
    }

    private void skip(int size) {
        m_position += size;
    }

    private byte readByte() {
        return m_data[m_position++];
    }

    private int readUnsignedByte() {
        return readByte() & 0xFF;
    }

    public int readPacketLength() {
        int length = readUnsignedByte();
        if (length >= 160)
            length = 256 * length - (40960 - readUnsignedByte());
        return length;
    }

    private int calculateSize(File f) throws IOException {
        DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(f))));
        int timestamp = 0;
        int size = 0;
        while ((timestamp = in.readInt()) != TIMESTAMP_EOF) {
            int length = in.readInt();
            if (length > 0) {
                size += length;
                in.skipBytes(length);
            }
        }
        in.close();
        return size;
    }
}
