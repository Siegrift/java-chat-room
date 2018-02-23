package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;

public class SocketHandler implements Runnable {
  private Server server;
  private Socket socket;
  private int socketId;

  SocketHandler(Server server, Socket socket, int socketId) {
    this.server = server;
    this.socket = socket;
    this.socketId = socketId;
    new Thread(this).start();
  }

  @Override
  public void run() {
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      while (true) {
        String total = reader.readLine();
        if (total == null) break;
        char[] buffer = new char[Integer.parseInt(total)];
        reader.read(buffer);
        sendAll(new String(buffer));
      }
      server.removeSocket(socketId);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void sendAll(String message) throws IOException {
    for (Map.Entry<Integer, Socket> s : server.sockets.entrySet()) {
      s.getValue().getOutputStream().write(message.getBytes());
    }
  }
}
