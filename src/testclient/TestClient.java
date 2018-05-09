package testclient;

import myutils.SocketHelper;

import java.io.FileOutputStream;
import java.net.Socket;

public class TestClient {
    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("127.0.0.1", 9999);
        SocketHelper helper = new SocketHelper(socket);

        int files = helper.in.readInt();
        for (int i = 0; i < files; ++i)
            System.out.println((i + 1) + ": " + helper.readString() + " (" + helper.in.readLong() / 1e6f + "MB)");

        String fname = System.console().readLine();
        helper.writeString(fname);

        FileOutputStream fout = new FileOutputStream(fname);
        byte[] bytes;
        while ((bytes = helper.readData()) != null)
            fout.write(bytes, 0, bytes.length);

        helper.out.writeInt(0);

        fout.close();
    }
}
