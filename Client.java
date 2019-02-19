package ru.guap.client;

import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.awt.image.*;
import java.awt.event.*;

public class Client {
    // ПЕРЕМЕННЫЕ СЕТИ
    boolean isConnected = false;
    String serverHost = null;
    int serverPort;
    Socket clientSocket;
    BufferedReader readSocket;
    BufferedWriter writeSocket;

    // ПЕРЕМЕННЫЕ ГРАФИКИ
    JFrame frame;
    JToolBar toolbar; // кнопки
    JPanel menu; // меню
    JLabel existLabel; // доска существует
    JLabel notFoundLabel; // доска не неайдена
    BoardPanel boardPanel; // отображение доски
    BufferedImage board = null; // доска
    Graphics2D graphics;
    Color mainColor;
    int size = 10; // размер кисти

    class BoardPanel extends JPanel implements Serializable {
        private static final long serialVersionUID = -109728024865681281L;

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(board, 0, 0, this);
        }
    }

    /*************************************
     * КЛАСС СЧИТЫВАНИЕ ДАННЫХ ОТ СЕРВЕРА
     *************************************/
    class NetDraw extends Thread {
        String message;
        String[] splitMessage;

        public NetDraw() {
            this.start();
        }

        public void run() {
            try {
                try {
                    while (true) {
                        message = readSocket.readLine();
                        splitMessage = message.split(" ", 2);
                        if (splitMessage[0].equals("CREATE")) {
                            /*****************
                             * СОЗДАНИЕ ДОСКИ
                             *****************/
                            if (splitMessage[1].equals("OK")) {
                                board = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
                                graphics = board.createGraphics();
                                graphics.setColor(Color.white);
                                graphics.fillRect(0, 0, 800, 600);
                                isConnected = true;
                                frame.remove(menu);
                                frame.add(boardPanel);
                                frame.repaint();
                            } else if (splitMessage[1].equals("EXISTS")) {
                                menu.add(existLabel);
                                frame.repaint();
                            }
                        } else if (splitMessage[0].equals("CONNECT")) {
                            /**********************
                             * ПОДКЛЮЧЕНИЕ К ДОСКЕ
                             **********************/
                            if (splitMessage[1].equals("OK")) {
                                int[] rgbArray = new int[480000];
                                for (int i = 0; i < rgbArray.length; i++) {
                                    message = readSocket.readLine();
                                    rgbArray[i] = Integer.parseInt(message);
                                }
                                board = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
                                board.setRGB(0, 0, 800, 600, rgbArray, 0, 800);
                                graphics = board.createGraphics();
                                isConnected = true;
                                frame.remove(menu);
                                frame.add(boardPanel);
                                frame.repaint();
                            } else if (splitMessage[1].equals("NOT FOUND")) {
                                menu.add(notFoundLabel);
                                frame.repaint();
                            }
                        } else {
                            /*********************
                             * РИСОВАНИЕ НА ДОСКЕ
                             *********************/
                            splitMessage = message.split(" ", 4);
                            int color = Integer.parseInt(splitMessage[0]);
                            int coordX = Integer.parseInt(splitMessage[1]);
                            int coordY = Integer.parseInt(splitMessage[2]);
                            int size = Integer.parseInt(splitMessage[3]);

                            graphics.setColor(new Color(color));
                            graphics.fillOval(coordX, coordY, size, size);
                            boardPanel.repaint();
                        }
                    }
                } catch (IOException err) {
                    System.out.println(err.toString());
                    readSocket.close();
                    writeSocket.close();
                }
            } catch (IOException err) {
                System.out.println(err.toString());
            }
        }
    }

    public Client(String serverHost, int serverPort) {
        // СЕТЬ
        try {
            try {
                this.serverHost = serverHost;
                this.serverPort = serverPort;
                clientSocket = new Socket(serverHost, serverPort);
                readSocket = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                writeSocket = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                new NetDraw();
            } catch (IOException err) {
                System.out.println(err.toString());
                readSocket.close();
                writeSocket.close();
            }
        } catch (IOException err) {
            System.out.println(err.toString());
        }

        // ГРАФИКА
        frame = new JFrame("MultiPaint");
        frame.setSize(840, 600); // размер окна
        frame.setResizable(false); // нельзя менять размер окна
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // закрытие программы
        frame.setLayout(null);
        frame.setVisible(true);

        // ******************
        // ПАНЕЛЬ РИСОВАНИЯ
        // ******************
        boardPanel = new BoardPanel();
        boardPanel.setBounds(40, 0, 800, 600);
        boardPanel.setOpaque(true);
        mainColor = Color.white; // Color нынешний цвет

        // *************
        // ПАНЕЛЬ МЕНЮ
        // *************
        menu = new JPanel();
        menu.setBounds(40, 0, 800, 600);
        menu.setBackground(mainColor);
        menu.setLayout(null);
        frame.add(menu);

        // ДОСКА С ТАКИМ ИМЕНЕМ УЖЕ СУЩЕСТВУЕТ
        ImageIcon exist = new ImageIcon(this.getClass().getClassLoader().getResource("exist.png"));
        existLabel = new JLabel(exist);
        existLabel.setBounds(20, 85, 200, 30);

        // ДОСКА С ТАКИМ ИМЕНЕМ НЕ НАЙДЕНА
        ImageIcon notFound = new ImageIcon(this.getClass().getClassLoader().getResource("notFound.png"));
        notFoundLabel = new JLabel(notFound);
        notFoundLabel.setBounds(20, 85, 200, 30);

        // ПРИГЛАШЕНИЕ
        ImageIcon invite = new ImageIcon(this.getClass().getClassLoader().getResource("invite.png"));
        JLabel inviteLabel = new JLabel(invite);
        inviteLabel.setBounds(20, 5, 200, 30);
        menu.add(inviteLabel);

        // ТЕКСТОВОЕ ПОЛЕ
        JTextField textField = new JTextField();
        textField.setBounds(20, 45, 200, 30);
        textField.setText("default");
        menu.add(textField);

        // СОЗДАТЬ ДОСКУ
        JButton createBoard = new JButton(
                new ImageIcon(this.getClass().getClassLoader().getResource("createBoard.png")));
        createBoard.setBounds(225, 40, 210, 40); // размещение
        createBoard.setBorderPainted(false); // не рисовать рамку
        createBoard.setBackground(Color.lightGray); // цвет фона (убирает градиент при наведении)
        createBoard.setOpaque(false); // прозрачность
        createBoard.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String nameBoard = textField.getText();
                // НЕТ ИМЕНИ ДОСКИ
                if (nameBoard.equals("")) {
                    frame.repaint();
                    return;
                }

                // УДАЛЕНИЕ ПРЕДУПРЕЖДЕНИЙ
                if (menu.isAncestorOf(existLabel)) {
                    menu.remove(existLabel);
                    frame.repaint();
                }
                if (menu.isAncestorOf(notFoundLabel)) {
                    menu.remove(notFoundLabel);
                    frame.repaint();
                }

                try {
                    try {
                        writeSocket.write("CREATE " + nameBoard + "\n");
                        writeSocket.flush();
                    } catch (IOException err) {
                        System.out.println(err.toString());
                        readSocket.close();
                        writeSocket.close();
                    }
                } catch (IOException err) {
                    System.out.println(err.toString());
                }
            }
        });
        menu.add(createBoard);

        // ПОДКЛЮЧИТЬСЯ К ДОСКЕ
        JButton joinBoard = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("joinBoard.png")));
        joinBoard.setBounds(435, 40, 210, 40); // размещение
        joinBoard.setBorderPainted(false); // не рисовать рамку
        joinBoard.setBackground(Color.lightGray); // цвет фона (убирает градиент при наведении)
        joinBoard.setOpaque(false); // прозрачность
        joinBoard.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String nameBoard = textField.getText();
                // НЕТ ИМЕНИ ДОСКИ
                if (nameBoard.equals("")) {
                    System.out.println();
                    frame.repaint();
                    return;
                }

                // УДАЛЕНИЕ ПРЕДУПРЕЖДЕНИЙ
                if (menu.isAncestorOf(existLabel)) {
                    menu.remove(existLabel);
                    frame.repaint();
                }
                if (menu.isAncestorOf(notFoundLabel)) {
                    menu.remove(notFoundLabel);
                    frame.repaint();
                }

                // ПОДКЛЮЧЕНИЕ
                try {
                    try {
                        writeSocket.write("CONNECT " + nameBoard + "\n");
                        writeSocket.flush();
                    } catch (IOException err) {
                        System.out.println(err.toString());
                        readSocket.close();
                        writeSocket.close();
                    }
                } catch (IOException err) {
                    System.out.println(err.toString());
                }
            }
        });
        menu.add(joinBoard);

        // ************************
        // ПАНЕЛЬ С ИНСТРУМЕНТАМИ
        // ************************
        JToolBar toolbar = new JToolBar("Toolbar", JToolBar.VERTICAL);
        toolbar.setBounds(0, 0, 40, 600); // размещение
        toolbar.setLayout(null); // элементы размещаем сами
        toolbar.setFloatable(false); // нельзя перетаскивать
        toolbar.setBorderPainted(false); // без рамок
        toolbar.setBackground(mainColor); // устанавливаем цвет панели
        frame.add(toolbar);

        // МЕНЮ
        JButton menuButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("menu.png")));
        menuButton.setBounds(0, 0, 40, 40); // размещение
        menuButton.setBorderPainted(false); // не рисовать рамку
        menuButton.setBackground(Color.lightGray); // цвет фона (убирает градиент при наведении)
        menuButton.setOpaque(false); // прозрачность
        menuButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (isConnected) {
                    if (frame.isAncestorOf(menu)) {
                        frame.remove(menu);
                        frame.add(boardPanel);
                        frame.repaint();
                    } else {
                        frame.remove(boardPanel);
                        frame.add(menu);
                        frame.repaint();
                    }
                }
            }
        });
        toolbar.add(menuButton);

        // РАЗМЕР 10
        JButton size4Button = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("size10.png")));
        size4Button.setBounds(0, 40, 40, 40);
        size4Button.setBorderPainted(false);
        size4Button.setBackground(Color.lightGray);
        size4Button.setOpaque(false);
        size4Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                size = 10;
            }
        });
        toolbar.add(size4Button);

        // РАЗМЕР 20
        JButton size10Button = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("size20.png")));
        size10Button.setBounds(0, 80, 40, 40);
        size10Button.setBorderPainted(false);
        size10Button.setBackground(Color.lightGray);
        size10Button.setOpaque(false);
        size10Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                size = 20;
            }
        });
        toolbar.add(size10Button);

        // РАЗМЕР 40
        JButton size20Button = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("size40.png")));
        size20Button.setBounds(0, 120, 40, 40);
        size20Button.setBorderPainted(false);
        size20Button.setBackground(Color.lightGray);
        size20Button.setOpaque(false);
        size20Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                size = 40;
            }
        });
        toolbar.add(size20Button);

        // РАЗМЕР 80
        JButton size30Button = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("size80.png")));
        size30Button.setBounds(0, 160, 40, 40);
        size30Button.setBorderPainted(false);
        size30Button.setBackground(Color.lightGray);
        size30Button.setOpaque(false);
        size30Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                size = 80;
            }
        });
        toolbar.add(size30Button);

        // ЦВЕТ БЕЛЫЙ
        JButton whiteButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("white.png")));
        whiteButton.setBounds(0, 240, 40, 40);
        whiteButton.setBorderPainted(false);
        whiteButton.setBackground(Color.lightGray);
        whiteButton.setOpaque(false);
        whiteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainColor = Color.white;
                toolbar.setBackground(mainColor);
                menu.setBackground(mainColor);
            }
        });
        toolbar.add(whiteButton);

        // ЦВЕТ ЧЕРНЫЙ
        JButton blackButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("black.png")));
        blackButton.setBounds(0, 280, 40, 40);
        blackButton.setBorderPainted(false);
        blackButton.setBackground(Color.lightGray);
        blackButton.setOpaque(false);
        blackButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainColor = Color.black;
                toolbar.setBackground(mainColor);
                menu.setBackground(mainColor);
            }
        });
        toolbar.add(blackButton);

        // ЦВЕТ КРАСНЫЙ
        JButton redButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("red.png")));
        redButton.setBounds(0, 320, 40, 40);
        redButton.setBorderPainted(false);
        redButton.setBackground(Color.lightGray);
        redButton.setOpaque(false);
        redButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainColor = Color.red;
                toolbar.setBackground(mainColor);
                menu.setBackground(mainColor);
            }
        });
        toolbar.add(redButton);

        // ЦВЕТ ОРАНЖЕВЫЙ
        JButton orangeButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("orange.png")));
        orangeButton.setBounds(0, 360, 40, 40);
        orangeButton.setBorderPainted(false);
        orangeButton.setBackground(Color.lightGray);
        orangeButton.setOpaque(false);
        orangeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainColor = Color.orange;
                toolbar.setBackground(mainColor);
                menu.setBackground(mainColor);
            }
        });
        toolbar.add(orangeButton);

        // ЦВЕТ ЖЕЛТЫЙ
        JButton yellowButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("yellow.png")));
        yellowButton.setBounds(0, 400, 40, 40);
        yellowButton.setBorderPainted(false);
        yellowButton.setBackground(Color.lightGray);
        yellowButton.setOpaque(false);
        yellowButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainColor = Color.yellow;
                toolbar.setBackground(mainColor);
                menu.setBackground(mainColor);
            }
        });
        toolbar.add(yellowButton);

        // ЦВЕТ ЗЕЛЕНЫЙ
        JButton greenButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("green.png")));
        greenButton.setBounds(0, 440, 40, 40);
        greenButton.setBorderPainted(false);
        greenButton.setBackground(Color.lightGray);
        greenButton.setOpaque(false);
        greenButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainColor = Color.green;
                toolbar.setBackground(mainColor);
                menu.setBackground(mainColor);
            }
        });
        toolbar.add(greenButton);

        // ЦВЕТ ГОЛУБОЙ
        JButton cyanButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("cyan.png")));
        cyanButton.setBounds(0, 480, 40, 40);
        cyanButton.setBorderPainted(false);
        cyanButton.setBackground(Color.lightGray);
        cyanButton.setOpaque(false);
        cyanButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainColor = Color.cyan;
                toolbar.setBackground(mainColor);
                menu.setBackground(mainColor);
            }
        });
        toolbar.add(cyanButton);

        // ЦВЕТ СИНИЙ
        JButton blueButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("blue.png")));
        blueButton.setBounds(0, 520, 40, 40);
        blueButton.setBorderPainted(false);
        blueButton.setBackground(Color.lightGray);
        blueButton.setOpaque(false);
        blueButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainColor = Color.blue;
                toolbar.setBackground(mainColor);
                menu.setBackground(mainColor);
            }
        });
        toolbar.add(blueButton);

        // ЦВЕТ ФИОЛЕТОВЫЙ
        JButton magentaButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("magenta.png")));
        magentaButton.setBounds(0, 560, 40, 40);
        magentaButton.setBorderPainted(false);
        magentaButton.setBackground(Color.lightGray);
        magentaButton.setOpaque(false);
        magentaButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                mainColor = Color.magenta;
                toolbar.setBackground(mainColor);
                menu.setBackground(mainColor);
            }
        });
        toolbar.add(magentaButton);

        // ***********
        // СЛУШАТЕЛИ
        // ***********
        boardPanel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                try {
                    try {
                        String message = mainColor.getRGB() + " " + (e.getX() - size / 2) + " " + (e.getY() - size / 2)
                                + " " + size;
                        writeSocket.write(message + "\n");
                        writeSocket.flush();
                    } catch (IOException err) {
                        System.out.println(err.toString());
                        readSocket.close();
                        writeSocket.close();
                    }
                } catch (IOException err) {
                    System.out.println(err.toString());
                }

            }
        });

        boardPanel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                try {
                    try {
                        String message = mainColor.getRGB() + " " + (e.getX() - size / 2) + " " + (e.getY() - size / 2)
                                + " " + size;
                        writeSocket.write(message + "\n");
                        writeSocket.flush();
                    } catch (IOException err) {
                        System.out.println(err.toString());
                        readSocket.close();
                        writeSocket.close();
                    }
                } catch (IOException err) {
                    System.out.println(err.toString());
                }
            }
        });
    }
}