package server;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class Server {
  public static void main(String args[]) {
    new Server();
  }

  private Selector acceptSelector;
  private SecretKeySpec secretKey;
  private Cipher cipher;

  private Server() {
    System.out.println("Server started...");
    try {
      cipher = Cipher.getInstance("AES");
      String AES_PASS = "hello world 0000";
      secretKey = new SecretKeySpec(AES_PASS.getBytes(), "AES");
      ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
      serverSocketChannel.configureBlocking(false);
      ServerSocket socket = serverSocketChannel.socket();
      InetSocketAddress address = new InetSocketAddress(5555);
      socket.bind(address);

      acceptSelector = Selector.open();
      serverSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);

      Iterator<SelectionKey> iter;
      SelectionKey key;
      while (serverSocketChannel.isOpen()) {
        acceptSelector.select();
        iter = acceptSelector.selectedKeys().iterator();
        while (iter.hasNext()) {
          key = iter.next();
          iter.remove();
          try {
            if (key.isAcceptable()) handleAccept(key);
            if (key.isReadable()) handleRead(key);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleAccept(SelectionKey key) throws IOException {
    SocketChannel sc = ((ServerSocketChannel) key.channel()).accept();
    String address = (new StringBuilder(sc.socket().getInetAddress().toString())).append(":").append(sc.socket().getPort()).toString();
    sc.configureBlocking(false);
    sc.register(acceptSelector, SelectionKey.OP_READ, address);
    System.out.println("Accepted connection from: " + address);
  }

  private void handleRead(SelectionKey key) throws Exception {
    SocketChannel ch = (SocketChannel) key.channel();
    ByteBuffer buf = ByteBuffer.allocate(128);
    // NOTE: using array list so that we are not relying on chars
    ArrayList<Byte> mess = new ArrayList<>();

    int read;
    while ((read = ch.read(buf)) > 0) {
      buf.flip();
      byte[] bytes = new byte[buf.limit()];
      buf.get(bytes);
      for (byte b: bytes) mess.add(b);
      buf.clear();
    }
    if (read < 0) {
      System.out.println(key.attachment() + " left the chat.\n");
      ch.close();
    }
    byte byteMess[] = new byte[mess.size()];
    for (int i = 0; i < mess.size(); i++) {
      byteMess[i] = mess.get(i);
    }
    cipher.init(Cipher.DECRYPT_MODE, this.secretKey);
    byte decrypted[] = cipher.doFinal(byteMess);
    broadcast(decrypted);
  }

  private void broadcast(byte[] msg) throws Exception {
    cipher.init(Cipher.ENCRYPT_MODE, this.secretKey);
    byte [] encrypted = cipher.doFinal(msg);
    ByteBuffer msgBuf = ByteBuffer.wrap(encrypted);
    for (SelectionKey key : acceptSelector.keys()) {
      if (key.isValid() && key.channel() instanceof SocketChannel) {
        SocketChannel sch = (SocketChannel) key.channel();
        ByteBuffer b = ByteBuffer.allocate(30).put(String.valueOf(encrypted.length).getBytes()).put("\n".getBytes());
        b.flip();
        sch.write(b);
        sch.write(msgBuf);
        msgBuf.rewind();
      }
    }
  }
}
