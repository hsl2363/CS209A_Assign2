package cn.edu.sustech.cs209.chatting.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    private static List<Socket> usersockets = new ArrayList<>();
    private static List<String> usernames = new ArrayList<>();
    private static Map<String, Socket> somap = new HashMap<>();
    private static Map<String, ClientHandler> chmap = new HashMap<>();
    private static List<ClientHandler> handlers = new ArrayList<>();
    private static int groupcnt = 0;
    private static Map<Integer, List<String>> Groupmember = new HashMap<>();

    private static void UpdateUserList() {
        String S = "UserList;";
        for (ClientHandler h : handlers) {
            for (String n : usernames)
                S += n;
            h.out.println(S);
        }
    }

    private static void CloseBroadcast() {
        String S = "ServerClosed";
        for (ClientHandler h : handlers) {
            h.out.println(S);
        }
    }

    private static void DeleteUser(String username, Socket socket, ClientHandler ch) {
        usersockets.remove(socket);
        usernames.remove(username);
        handlers.remove(ch);
        UpdateUserList();
    }

    private static void SendMessage(String SendTo, String S) {
        ClientHandler to = chmap.get(SendTo);
        to.out.println(S);
    }

    private static void SendMessage(int SendTo, String S) {
        List<String> group = Groupmember.get(SendTo);
        for (String to : group) {
            ClientHandler h = chmap.get(to);
            h.out.println(S);
        }
    }

    private static void CreatePrivate(String A, String B) {
        ClientHandler b = chmap.get(B);
        b.out.println("AddPrivate;" + A);
    }

    private static void CreateGroup(List<String> users) {
        String S = "AddGroup;" + ++groupcnt;
        List<ClientHandler> ch = new ArrayList<>();
        for (String user : users) {
            S += ";" + user;
            ch.add(chmap.get(user));
        }
        for (ClientHandler h : ch)
            h.out.println(S);
    }

    public static void main(String[] args) throws IOException {

        System.out.println("Starting server");
        ServerSocket serversocket = new ServerSocket(2363);
        serversocket.setSoTimeout(0);
        System.out.println("Waiting...");

        String username;

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    CloseBroadcast();
                    for (Socket s : usersockets)
                        s.close();
                    serversocket.close();
                    System.out.println("Server Closed.");
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
        });

        while (true) {

            Socket socket = serversocket.accept();
            usersockets.add(socket);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            if ((username = in.readLine()) != null) {
                if (somap.containsKey(username)) {
                    out.println("NO");
                } else {
                    out.println("OK");
                    usernames.add(username);
                    break;
                }
            }
            in.close();
            somap.put(username, socket);
            System.out.printf("%s Connected.", username);
            UpdateUserList();

            ClientHandler T = new ClientHandler(username, socket);
            handlers.add(T);
            Thread t = new Thread(T);
            t.start();
            chmap.put(username, T);
        }

    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private String username;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(String username, Socket socket) throws IOException {
            this.clientSocket = socket;
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        }

        @Override
        public void run() {
            try {
                boolean quit = false;
                String input;
                String[] str;
                while ((input = in.readLine()) != null) {
                    str = input.split(";");
                    switch (str[0]) {
                        case "UserQuit":
                            quit = true;
                            break;
                        case "CreatePrivate":
                            CreatePrivate(str[1], str[2]);
                            break;
                        case "CreateGroup":
                            List<String> users = new ArrayList<>();
                            for (int i = 1; i < str.length; i++)
                                users.add(str[i]);
                            CreateGroup(users);
                            break;
                        case "SendMessageP":
                            SendMessage(str[2], input);
                            break;
                        case "SendMessageG":
                            SendMessage(Integer.valueOf(str[2]), input);
                            break;
                        default:
                            System.out.println("unrecognized");
                            break;
                    }
                    if (quit) {
                        DeleteUser(username, clientSocket, this);
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Error handling client: " + e);
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("Error closing client socket: " + e);
                }
            }
        }

    }

}
