package client;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ClientController {
  private static final String HELP = "---Commands---\n--help = show help\n--clear = clear text area\n--link [link] = create hyperlink\n" +
          "\":D\",\":|\",\"B|\",\"3:)\",\":)\",\":(\",\":p\" = smiley faces\n";
  public TextField nameField;
  public TextField messageField;
  public Button nameButton;
  public TextFlow messageArea;
  private Client client;

  public void changeButtonName() {
    client.sendMessage("name", nameField.getText());
    client.setName(nameField.getText());
    nameButton.setDisable(true);
  }


  void init(Client client, String name) {
    this.client = client;
    nameField.setText(name);
    nameField.textProperty().addListener((observable, oldValue, newValue) -> nameButton.setDisable(this.client.getName().equals(newValue)));
  }

  public void messageFieldKeyPressed(KeyEvent keyEvent) {
    if (keyEvent.getCode() == KeyCode.ENTER) {
      String message = messageField.getText();
      messageField.clear();
      client.addToHistory(message);
      if (message.startsWith("--link")) {
        client.sendMessage("link", message.substring(6).trim());
        return;
      }
      switch (message) {
        case "--help":
          messageArea.getChildren().add(new Text(HELP));
          break;
        case "--clear":
          messageArea.getChildren().clear();
          break;
        case "--link":
          client.sendMessage("link", message);
          break;
        default:
          client.sendMessage("message", message);
      }
    } else if (keyEvent.getCode() == KeyCode.UP) {
      client.moveInHistory(-1);
    } else if (keyEvent.getCode() == KeyCode.DOWN) {
      client.moveInHistory(1);
    }
  }

  public void chooseFile() {
    FileChooser chooser = new FileChooser();
    File file = chooser.showOpenDialog(null);
    if (file == null) return;
    byte[] bytes = new byte[16 * 1024];
    ArrayList<Byte> fileContent = new ArrayList<>();
    try {
      InputStream in = new FileInputStream(file);
      int count;
      while ((count = in.read(bytes)) > 0) {
        for (int i = 0; i < count; i++) {
          fileContent.add(bytes[i]);
        }
      }
    } catch (IOException e) {
      System.out.println("Error reading file!");
    }
    StringBuilder b = new StringBuilder();
    for (Byte byt : fileContent) {
      b.append((char) byt.byteValue());
    }
    String message = String.format("%d\n%s%d\n%s", file.getName().length(), file.getName(), b.toString().length(), b.toString());
    client.sendMessage("file", message);
  }
}
