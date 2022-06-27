package domaci;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

public class Panel extends JPanel {

    JTextArea console;
    JTextArea rooms;
    JButton send;
    JButton open;
    JButton changeRoom;
    JButton exit;
    JButton create;


    public Panel(User p) {
        this.setLayout(null);

        console = new JTextArea("Console :");
        console.setBounds(200, 15, 450, 430);
        console.setBackground(Color.CYAN);
        this.add(console);

        rooms = new JTextArea("Rooms:");
        rooms.setBounds(50, 15, 100, 430);
        rooms.setBackground(Color.CYAN);
        String s = "\n" + p.roomName;
        rooms.append(s);
        this.add(rooms);

        send = new JButton("Send");
        send.setBounds(10, 475, 150, 30);
        send.setActionCommand("send");
        send.addActionListener(new ButtonListener(p));
        this.add(send);

        open = new JButton("Delete");
        open.setBounds(180, 475, 150, 30);
        open.setActionCommand("delete");
        open.addActionListener(new ButtonListener(p));
        this.add(open);

        changeRoom = new JButton("Change Room");
        changeRoom.setBounds(350, 475, 150, 30);
        changeRoom.setActionCommand("change");
        changeRoom.addActionListener(new ButtonListener(p));
        this.add(changeRoom);

        exit = new JButton("Exit");
        exit.setBounds(520, 475, 150, 30);
        exit.setActionCommand("exit");
        exit.addActionListener(new ButtonListener(p));
        this.add(exit);

        create = new JButton("Create new room");
        create.setBounds(250, 520, 200, 30);
        create.setActionCommand("create");
        create.addActionListener(new ButtonListener(p));
        this.add(create);

        acceptMessages(p);
    }

    private void refreshRooms(User user) {
        rooms.setText("Rooms :");
        for (String room : user.roomList) {
            String s = "\n" + room;
            rooms.append(s);
        }
    }

    private void acceptMessages(User user) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(user.roomName, "fanout");
            String QUEUE_NAME = channel.queueDeclare().getQueue();
            channel.queueBind(QUEUE_NAME, user.roomName, "");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                byte[] array = delivery.getBody();
                String[] split = message.split("sep");
                if (split.length == 3) {
                    if (!split[0].equals(user.name) && user.roomList.contains(user.roomName)) {
                        String s = "\n" + user.name + " received a message from " + split[0] + " : " + split[1] + " " + split[2];
                        console.append(s);
                    }
                } else {
                    ByteArrayInputStream bis = new ByteArrayInputStream(array);
                    BufferedImage image = ImageIO.read(bis);
                    String pathname = user.folder + "\\" + user.num + ".jpg";
                    File f = new File(pathname);
                    user.num++;
                    if (!f.exists()) {
                        f.createNewFile();
                    }
                    ImageIO.write(image, "jpg", f);
                    String s = "\n" + user.name + " received " + pathname + " IMAGE";
                    console.append(s);
                }
            };
            channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ButtonListener implements ActionListener {

        User user;

        public ButtonListener(User u) {
            this.user = u;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("send")) {
                String message = JOptionPane.showInputDialog("Message:");
                user.send(message);
            } else if (e.getActionCommand().equals("delete")) {
                String room = JOptionPane.showInputDialog("Enter room for deletion");
                if (room != null) {
                    user.roomList.remove(room);
                    refreshRooms(user);
                }
            } else if (e.getActionCommand().equals("change")) {
                String room = JOptionPane.showInputDialog("Enter new room");
                if (room != null) {
                    user.roomName = room;
                    user.roomList.add(room);
                    refreshRooms(user);
                }
                acceptMessages(user);
            } else if (e.getActionCommand().equals("exit")) {
                user.dispose();
                User.userCounter--;
                if (User.userCounter == 0) System.exit(0);
            } else if (e.getActionCommand().equals("create")) {
                String name = JOptionPane.showInputDialog("Enter process name");
                String path = JOptionPane.showInputDialog("Enter folder path");
                String room = JOptionPane.showInputDialog("Enter room");
                if (name == null) return;
                while (name.equals("")) {
                    name = JOptionPane.showInputDialog("Enter process name");
                }
                if (path == null || path.equals("")) {
                    int len = user.name.length();
                    path = user.folder.substring(0, user.folder.length() - len);
                }
                if (room == null || room.equals("")) {
                    room = user.roomName;
                }

                User uNew = new User(name, path, room);
            }
        }
    }
}
