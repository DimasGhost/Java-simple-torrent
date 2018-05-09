package server;

import java.io.*;
import java.net.ServerSocket;

public class Server {

    public static void main(String[] args) {
        String dir = ".", port = "9999";
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-d") && i + 1 < args.length)
                dir = args[i + 1];
            if (args[i].equals("-p") && i + 1 < args.length)
                port = args[i + 1];
        }

        int portVal;
        try {
            portVal = Integer.parseInt(port);
        } catch (Exception ex) {
            System.out.println("Port should be a valid integer.");
            return;
        }

        Server server;
        try {
            server = new Server(dir, portVal);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return;
        }
        System.out.println("Server is up at port " + port + " at directory \"" + dir + "\"");

        try {
            server.listen();
        } catch (Exception ex) {
            System.out.println("Oops, something went wrong...");
        }
    }

    private File root;
    private ServerSocket listener;
    int clientsCnt = 0;

    public Server(String dir, int port) throws Exception {
        root = new File(dir);
        if (!root.exists() || !root.isDirectory())
            throw new Exception("\"" + dir + "\" is not a valid name of existing directory.");
        if (port < 0 || port > 65535)
            throw new Exception("Port should be in range [0; 65535]");
        try {
            listener = new ServerSocket(port);
        } catch (Exception ex) {
            throw new Exception("Can't run server at port " + port);
        }
    }

    public void listen() throws IOException {
        try {
            while (!listener.isClosed()) {
                new FileSender(listener.accept(), root, ++clientsCnt).start();
            }
        } finally {
            listener.close();
        }
    }
}
