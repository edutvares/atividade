import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ServidorArquivo {

  public static final String MULTICAST_GROUP = "230.0.0.1";
  public static final int MULTICAST_PORT = 4446;

  private final String baseDir;
  private final String nomeServidor;
  private final int tcpPort;

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
    // Inicia thread para servidor TCP
    new Thread(this::iniciarServidorTCP).start();

    // Inicia escuta multicast
    iniciarEscutaMulticast();
  }

  private void iniciarServidorTCP() {
    try {
      ServerSocket serverSocket = new ServerSocket(tcpPort);
      System.out.println("[ARQUIVO] Servidor TCP escutando na porta: " + tcpPort);

      while (true) {
        Socket cliente = serverSocket.accept();
        new Thread(() -> handleClienteDownload(cliente)).start();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleClienteDownload(Socket cliente) {
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
      OutputStream out = cliente.getOutputStream();

      String filename = in.readLine();
      System.out.println("[ARQUIVO] Cliente pediu: " + filename);

      File file = new File(baseDir, filename);

      if (file.exists() && file.isFile()) {
        // Envia arquivo
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        int bytesLidos;

        while ((bytesLidos = fis.read(buffer)) != -1) {
          out.write(buffer, 0, bytesLidos);
        }

        fis.close();
        out.flush();
        System.out.println("[ARQUIVO] Arquivo " + filename + " enviado com sucesso");
      } else {
        System.out.println("[ARQUIVO] Arquivo " + filename + " não encontrado");
      }

      cliente.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void iniciarEscutaMulticast() throws Exception {
    InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
    InetSocketAddress groupAddress = new InetSocketAddress(group, MULTICAST_PORT);

    NetworkInterface netIf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());

    try (MulticastSocket socket = new MulticastSocket(MULTICAST_PORT)) {
      socket.joinGroup(groupAddress, netIf);

      System.out.println("[ARQUIVO] Escutando grupo multicast...");

      byte[] buffer = new byte[1024];

      while (true) {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        String msg = new String(packet.getData(), 0, packet.getLength());
        String[] parts = msg.split(";");

        // Expecting: <queryId>;<filename>;<replyIp>;<replyPort>
        if (parts.length < 4) {
          System.out.println("[ARQUIVO] Mensagem multicast em formato inesperado: " + msg);
          continue;
        }

        String queryId = parts[0];
        String filename = parts[1];
        String replyIp = parts[2];
        int replyPort = Integer.parseInt(parts[3]);

        boolean exists = Files.exists(Paths.get(baseDir, filename));

        System.out.println("[ARQUIVO] " + (exists ? "Arquivo encontrado: " : "Arquivo nao encontrado: ") + filename
            + ". Enviando resposta para " + replyIp + ":" + replyPort + " (query=" + queryId + ")");

        String reply = queryId + ";" + nomeServidor + ";" + InetAddress.getLocalHost().getHostAddress() + ";"
            + tcpPort + ";" + (exists ? "FOUND" : "NOTFOUND");

        try (DatagramSocket ds = new DatagramSocket()) {
          byte[] rb = reply.getBytes();
          DatagramPacket rpacket = new DatagramPacket(rb, rb.length, InetAddress.getByName(replyIp), replyPort);
          ds.send(rpacket);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
