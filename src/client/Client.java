package client;

import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Client extends Application implements Runnable {
  private Socket socket;
  private ClientController controller;
  private String name = "Guest";
  private ArrayList<String> history = new ArrayList<>();
  private int historyPointer = 0;
  private HashMap<String, MaterialDesignIcon> icons = new HashMap<>();
  private SecretKeySpec secretKey;
  private Cipher cipher;

  public static void main(String a[]) {
    launch(a);
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("client.fxml"));
    cipher = Cipher.getInstance("AES");
    String AES_PASS = "hello world 0000";
    secretKey = new SecretKeySpec(AES_PASS.getBytes(), "AES");
    Parent root = loader.load();
    controller = loader.getController();
    controller.init(this, name);
    primaryStage.setTitle("Client chat");
    primaryStage.setScene(new Scene(root, 1000, 750));
    primaryStage.setOnCloseRequest(t -> {
      Platform.exit();
      System.exit(0);
    });
    initIcons();
    try {
      socket = new Socket("localhost", 5555);
    } catch (IOException e) {
      System.out.println("Server not running, aborting");
      System.exit(0);
    }
    new Thread(this).start();
    primaryStage.show();
    System.out.println("Initialization successful! Enter will send message");
  }

  private void initIcons() {
    icons.put(":D", MaterialDesignIcon.EMOTICON);
    icons.put(":|", MaterialDesignIcon.EMOTICON_NEUTRAL);
    icons.put("B|", MaterialDesignIcon.EMOTICON_COOL);
    icons.put("3:)", MaterialDesignIcon.EMOTICON_DEVIL);
    icons.put(":)", MaterialDesignIcon.EMOTICON_HAPPY);
    icons.put(":(", MaterialDesignIcon.EMOTICON_SAD);
    icons.put(":p", MaterialDesignIcon.EMOTICON_TONGUE);
  }

  private byte[] readMessage() throws IOException {
    StringBuilder size = new StringBuilder();
    int c;
    while ((c = socket.getInputStream().read()) != '\n') size.append((char) c);
    byte[] buffer = new byte[Integer.parseInt(size.toString())];
    int check = socket.getInputStream().read(buffer);
    if (check != Integer.parseInt(size.toString())) throw new IOException("Incorrect checksum in read");
    return buffer;
  }

  @Override
  public void run() {
    try {
      while (true) {
        byte [] encrypted = readMessage();
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        String decrypted = new String(cipher.doFinal(encrypted));
        BufferedReader reader = new BufferedReader(new StringReader(decrypted));
        String command = reader.readLine();
        String name = readString(reader);
        String message = readString(reader);
        switch (command) {
          case "message":
            Platform.runLater(() -> processMessage(name, message));
            break;
          case "name":
            Platform.runLater(() -> processRename(name, message));
            break;
          case "file":
            Platform.runLater(() -> processFile(name, message));
            break;
          case "link":
            Platform.runLater(() -> {
              controller.messageArea.getChildren().add(new Text(name + ": "));
              Hyperlink hyperLink = new Hyperlink(message);
              hyperLink.setOnAction((event) -> HostServicesFactory.getInstance(this).showDocument(message));
              controller.messageArea.getChildren().add(hyperLink);
              controller.messageArea.getChildren().add(new Text("\n"));
            });
            break;
          default:
            System.err.println("Unknown command: " + command);
        }
      }
    } catch (IOException e) {
      System.out.println("Error reading client socket");
    } catch (Exception e){
      e.printStackTrace();
      System.out.println("Error decrypting");
    }
  }

  private void processFile(String name, String message) {
    try {
      controller.messageArea.getChildren().add(new Text(name + ": "));
      BufferedReader reader = new BufferedReader(new StringReader(message));
      String fileName = readString(reader);
      String content = readString(reader);
      reader.close();
      Hyperlink hyperLink = new Hyperlink(fileName);
      hyperLink.setOnAction((event) -> {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(fileName);
        File file = chooser.showSaveDialog(null);
        if (file == null) return;
        try (BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(file))) {
          byte[] byteContent = new byte[content.length()];
          for (int i = 0;i < byteContent.length;i++) {
            byteContent[i] = (byte) content.charAt(i);
          }
          writer.write(byteContent);
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
      controller.messageArea.getChildren().add(hyperLink);
      controller.messageArea.getChildren().add(new Text("\n"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String readString(BufferedReader reader) throws IOException {
    int length = Integer.parseInt(reader.readLine());
    char[] buffer = new char[length];
    if (reader.read(buffer) == -1) throw new IOException("Read -1");
    return String.valueOf(buffer);
  }

  private void processRename(String oldName, String newName) {
    controller.messageArea.getChildren().add(new Text(String.format("%s: '%s' has renamed himself to '%s'\n", newName, oldName, newName)));
  }

  private void processMessage(String name, String message) {
    controller.messageArea.getChildren().add(new Text(String.format("%s: ", name)));
    int last = 0, index = 0;
    boolean canBeSmiley = true;
    while (index < message.length()) {
      if (canBeSmiley) {
        for (Map.Entry<String, MaterialDesignIcon> entry : icons.entrySet()) {
          String substr = message.substring(index);
          String key = entry.getKey();
          if (substr.startsWith(key) && (
                  (substr.length() > key.length() && Character.isWhitespace(substr.charAt(key.length()))) ||
                          substr.length() == key.length()
          )) {
            controller.messageArea.getChildren().add(new Text(message.substring(last, index)));
            controller.messageArea.getChildren().add(new MaterialDesignIconView(entry.getValue()));
            index += key.length() - 1;
            last = index + 1;
            break;
          }
        }
      }
      canBeSmiley = Character.isWhitespace(message.charAt(index));
      index++;
    }
    controller.messageArea.getChildren().add(new Text(message.substring(last) + "\n"));
  }

  void sendMessage(String command, String message) {
    try {
      String toWrite = String.format("%s\n%d\n%s%d\n%s", command, name.length(), name, message.length(), message);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey);
      byte[]bts = cipher.doFinal(toWrite.getBytes());
      socket.getOutputStream().write(bts);
    } catch (Exception e) {
      System.out.println("Error sending message");
    }
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void addToHistory(String message) {
    history.add(message);
    historyPointer = history.size();
  }

  public void moveInHistory(int step) {
    historyPointer += step;
    historyPointer = Math.max(Math.min(historyPointer, history.size()), 0);
    if (historyPointer == history.size()) controller.messageField.clear();
    else controller.messageField.setText(history.get(historyPointer));
  }
}
