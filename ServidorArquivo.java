import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class ServidorArquivo {

    public static final String MULTICAST_GROUP = "230.0.0.1";
    public static final int MULTICAST_PORT = 4446;

    private final String baseDir;
    private final int tcpPort;
    private final String nomeServidor;

    public ServidorArquivo(String baseDir, String nomeServidor, int tcpPort) {
        this.baseDir = baseDir;
        this.nomeServidor = nomeServidor;
        this.tcpPort = tcpPort;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Uso: java ServidorArquivo <pasta> <nomeServidor> <tcpPort>");
            return;
        }
        new ServidorArquivo(args[0], args[1], Integer.parseInt(args[2])).start();
    }

    public void start() throws Exception {
        System.out.println("[FILESERVER " + nomeServidor + "] Servidor iniciado.");

        // Thread para multicast
        new Thread(this:: ouvirMulticast).start();

        // Thread para servir arquivos via TCP
        new Thread(this::servidorTCP).start();
    }

    private void ouvirMulticast() {
        try {
            MulticastSocket socket = new MulticastSocket(MULTICAST_PORT);
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            socket.joinGroup(group);

            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength());
                String[] parts = msg.split(";");

                String queryId = parts[0];
                String filename = parts[1];
                String replyIp = parts[2];
                int replyPort = Integer.parseInt(parts[3]);

                if (Files.exists(Paths.get(baseDir, filename))) {
                    sendReply(queryId, replyIp, replyPort);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendReply(String queryId, String replyIp, int replyPort) {
        try {
            String msg = queryId + ";" + nomeServidor + ";" + getLocalIP() + ";" + tcpPort;
            byte[] data = msg.getBytes();

            DatagramPacket packet =
                    new DatagramPacket(data, data.length, InetAddress.getByName(replyIp), replyPort);

            DatagramSocket s = new DatagramSocket();
            s.send(packet);
            s.close();

            System.out.println("[FILESERVER " + nomeServidor + "] Respondi que possuo o arquivo.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void servidorTCP() {
        try {
            ServerSocket serverSocket = new ServerSocket(tcpPort);
            System.out.println("[FILESERVER " + nomeServidor + "] TCP ativo na porta " + tcpPort);

            while (true) {
                Socket cliente = serverSocket.accept();
                new Thread(() -> handleDownload(cliente)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDownload(Socket cliente) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            OutputStream out = cliente.getOutputStream();

            String filename = in.readLine();
            File file = new File(baseDir, filename);

            if (!file.exists()) {
                out.write("NOT_FOUND\n".getBytes());
                cliente.close();
                return;
            }

            out.write(("OK;" + file.length() + "\n").getBytes());
            Files.copy(file.toPath(), out);

            cliente.close();
            System.out.println("[FILESERVER " + nomeServidor + "] Arquivo enviado: " + filename);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getLocalIP() throws Exception {
        return InetAddress.getLocalHost().getHostAddress();
    }
}
