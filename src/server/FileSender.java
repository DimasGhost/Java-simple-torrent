package server;

import myutils.SocketHelper;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class FileSender extends Thread {
    private Socket socket;
    private File root;
    private int clientID;

    public FileSender(Socket socket, File root, int clientID) {
        this.socket = socket;
        this.root = root;
        this.clientID = clientID;
    }

    public void run() {
        try {
            System.out.println("Serving new client with ID " + clientID);
            SocketHelper helper = new SocketHelper(socket);

            ArrayList<File> files = new ArrayList<File>();
            for (File f : root.listFiles())
                if (f.isFile())
                    files.add(f);

            helper.out.writeInt(files.size());
            for (File f : files) {
                helper.writeString(f.getName());
                helper.out.writeLong(f.length());
            }

            String fname = helper.readString();
            System.out.println("Client " + clientID + " choose file " + fname);

            FileInputStream fin = null;
            for (File f : files)
                if (f.getName().equals(fname))
                    fin = new FileInputStream(f);

            int maxSize = 1024, size;
            byte[] bytes = new byte[maxSize];
            while ((size = fin.read(bytes, 0, maxSize)) > 0)
                helper.writeData(bytes, size);
            helper.out.writeInt(0);

            helper.in.readInt();

            System.out.println("Client " + clientID + " served");
            socket.close();
        } catch (Exception ex) {
            System.out.println("Connection with client " + clientID + " aborted");
        }
    }
}
