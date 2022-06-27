package domaci;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

public class User extends JFrame {
    static int userCounter = 0;
    String name;
    String folder;
    String roomName;
    char num = 65;
    HashSet<String> roomList;

    public User(String name, String path, String roomName) {

        userCounter++;
        this.name = name;
        folder = path + name;
        File folderF = new File(folder);
        if (!folderF.exists()) {
            folderF.mkdirs();
        }
        this.roomName = roomName;
        roomList = new HashSet<>();
        roomList.add(this.roomName);

        this.setTitle(name);
        this.setSize(700, 600);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setContentPane(new Panel(this));
        this.setResizable(false);
        this.setVisible(true);
    }

    public void send(String message) {
        if (message == null) return;
        if (message.endsWith(".png") || message.endsWith(".jpg")) {
            String name = this.name;
            String type = "IMAGE";
            Message msg = new Message(name, type, message);
            sendImage(msg);
        } else {
            String name = this.name;
            String type = " TEXT";
            Message msg = new Message(name, type, message);
            sendText(msg);
        }
    }

    private void sendImage(Message msg) {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(roomName, "fanout");
            Panel panel = (Panel) this.getContentPane();

            BufferedImage image = ImageIO.read(new File(msg.data));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", bos);
            byte[] message = bos.toByteArray();

            channel.basicPublish(roomName, "", null, message);

            String s = "\n" + msg.name + " sends :" + msg.data;
            panel.console.append(s);

        } catch (Exception e) {
            Panel panel = (Panel) this.getContentPane();
            String s = "\n" + "File does not exist";
            panel.console.append(s);
        }
    }

    private void sendText(Message m) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(roomName, "fanout");
            String message = m.toString();
            channel.basicPublish(roomName, "", null, message.getBytes(StandardCharsets.UTF_8));
            String s = "\n" + m.name + " sends :" + m.data;
            Panel panel = (Panel) this.getContentPane();
            panel.console.append(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private class Message {
        String name;
        String type;
        String data;

        public Message(String name, String type, String data) {
            this.name = name;
            this.type = type;
            this.data = data;
        }

        @Override
        public String toString() {
            return name + "sep" + data + "sep" + type;
        }

    }
}

