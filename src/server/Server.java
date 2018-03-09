package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Server {
  public static void main(String args[]) {
    new Server();
  }

  HashMap<Integer, Socket> sockets = new HashMap<>();

  private Server() {
    int socketId = 0;
    System.out.println("Server started...");
    try (ServerSocket serverSocket = new ServerSocket(5555)) {
      //noinspection InfiniteLoopStatement
      while (true) {
        Socket socket = serverSocket.accept();
        sockets.put(socketId, socket);
        new SocketHandler(this, socket, socketId);
        System.out.println("Processed client socket: " + socketId);
        socketId++;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void removeSocket(int socketId) {
    sockets.remove(socketId);
    System.out.println("Removed client socket: " + socketId);
  }
}
