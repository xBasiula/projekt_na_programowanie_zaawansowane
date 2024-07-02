package org.example;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

class Kon {
    String id;

    Kon(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Kon{" + "id='" + id + '\'' + '}';
    }
}

class Lama {
    String id;

    Lama(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Lama{" + "id='" + id + '\'' + '}';
    }
}

class Alpaka {
    String id;

    Alpaka(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Alpaka{" + "id='" + id + '\'' + '}';
    }
}

public class Server {
    private static final int MAX_CLIENTS = 5;
    private static final Map<String, List<Object>> data = new ConcurrentHashMap<>();
    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final Map<Integer, String> history = new ConcurrentHashMap<>();
    private static int clientCounter = 0;

    public static void main(String[] args) throws IOException {
        data.put("Kon", Arrays.asList(new Kon("kon_1"), new Kon("kon_2"), new Kon("kon_3"), new Kon("kon_4")));
        data.put("Lama", Arrays.asList(new Lama("lama_1"), new Lama("lama_2"), new Lama("lama_3"), new Lama("lama_4")));
        data.put("Alpaka", Arrays.asList(new Alpaka("alpaka_1"), new Alpaka("alpaka_2"), new Alpaka("alpaka_3"), new Alpaka("alpaka_4")));

        ServerSocket serverSocket = new ServerSocket(12345);
        System.out.println("Server started");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            if (clients.size() >= MAX_CLIENTS) {
                new PrintWriter(clientSocket.getOutputStream(), true).println("REFUSED");
                clientSocket.close();
                continue;
            }
            clientCounter++;
            ClientHandler clientHandler = new ClientHandler(clientSocket, clientCounter);
            clients.add(clientHandler);
            new Thread(clientHandler).start();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final int clientId;
        private boolean running = true;

        ClientHandler(Socket socket, int id) {
            this.clientSocket = socket;
            this.clientId = id;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                out.println("OK");
                out.println(clientId);
                System.out.println("Client " + clientId + " connected.");
                history.put(clientId, "Connected");

                while (running) {
                    String request = in.readLine();
                    if (request == null) {
                        break;
                    }
                    handleRequest(request, out);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                clients.remove(this);
            }
        }

        private void handleRequest(String request, PrintWriter out) {
            try {
                Thread.sleep(new Random().nextInt(1000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            String[] parts = request.split("_");
            if (parts.length != 2 || !parts[0].equals("get")) {
                out.println("ERROR");
                return;
            }
            String className = parts[1];
            List<Object> objects = data.get(className);
            if (objects == null) {
                out.println("ERROR");
                return;
            }
            out.println(serializeObjects(objects));
            history.put(clientId, "Requested " + className);
            System.out.println("Client " + clientId + " requested " + className + ": " + objects);
        }

        private String serializeObjects(List<?> objects) {
            StringBuilder sb = new StringBuilder();
            for (Object obj : objects) {
                sb.append(obj.toString()).append(";");
            }
            return sb.toString();
        }
    }
}
