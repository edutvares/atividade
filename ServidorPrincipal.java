import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ServidorPrincipal {

    public static final String MULTICAST_GROUP = "230.0.0.1";
    public static final int MULTICAST_PORT = 4446;
    public static final int REPLY_PORT = 5001;
    public static final int TCP_PORT = 6000;

    private static final int TIMEOUT = 10_000; // 10 segundos

    public Map<String, List<String>> respostas = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        new ServidorPrincipal().start();
    }

    public void start() throws Exception {
        System.out.println("[PRINCIPAL] Servidor principal iniciado.");

        // Thread para ouvir UDP com respostas dos servidores de arquivos
        new Thread(this::listenForReplies).start();

        // Inicia servidor TCP para clientes
        ServerSocket serverSocket = new ServerSocket(TCP_PORT);
        System.out.println("[PRINCIPAL] Aguardando clientes na porta: " + TCP_PORT);

        while (true) {
            Socket cliente = serverSocket.accept();
            new Thread(() -> handleClient(cliente)).start();
        }
    }

    private void handleClient(Socket cliente) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            PrintWriter out = new PrintWriter(cliente.getOutputStream(), true);

            String filename = in.readLine();
            System.out.println("[PRINCIPAL] Cliente pediu: " + filename);

            String queryId = UUID.randomUUID().toString();
            respostas.put(queryId, Collections.synchronizedList(new ArrayList<>()));

            // Envia multicast
            sendMulticast(queryId, filename);

            // Espera timeout
            Thread.sleep(TIMEOUT);

            // Responde ao cliente
            List<String> servidores = respostas.get(queryId);
            out.println(String.join(";", servidores));

            cliente.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMulticast(String queryId, String filename) {
        try {
            String msg = queryId + ";" + filename + ";" + getLocalIP() + ";" + REPLY_PORT;
            byte[] buffer = msg.getBytes();

            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, MULTICAST_PORT);

            MulticastSocket socket = new MulticastSocket();
            socket.send(packet);
            socket.close();

            System.out.println("[PRINCIPAL] Multicast enviado para arquivo: " + filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void listenForReplies() {
        try {
            DatagramSocket socket = new DatagramSocket(REPLY_PORT);
            byte[] buffer = new byte[1024];

            System.out.println("[PRINCIPAL] Aguardando respostas UDP...");

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String data = new String(packet.getData(), 0, packet.getLength());
                String[] parts = data.split(";");

                String queryId = parts[0];
                String serverAddress = parts[1];

                respostas.getOrDefault(queryId, new ArrayList<>()).add(serverAddress);

                System.out.println("[PRINCIPAL] Resposta registrada: " + serverAddress);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getLocalIP() throws Exception {
        return InetAddress.getLocalHost().getHostAddress();
    }
}
