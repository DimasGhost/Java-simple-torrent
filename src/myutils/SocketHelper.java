package myutils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketHelper {
    public DataInputStream in;
    public DataOutputStream out;

    public SocketHelper(Socket socket) throws IOException {
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    public void writeData(byte[] bytes, int len) throws IOException {
        out.writeInt(len);
        out.write(bytes, 0, len);
    }

    public byte[] readData() throws IOException {
        int len = in.readInt();
        if (len == 0)
            return null;
        byte[] bytes = new byte[len];
        in.readNBytes(bytes, 0, len);
        return bytes;
    }

    public void writeString(String s) throws IOException {
        byte[] bytes = s.getBytes();
        writeData(bytes, bytes.length);
    }

    public String readString() throws IOException {
        return new String(readData());
    }
}
