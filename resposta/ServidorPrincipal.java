import java.io.*;
import java.net.*;
import java.util.*;

public class ServidorPrincipal {

  public static final int TCP_PORT = 6000;
  public static final int REPLY_PORT = 5001;
  public static final int TIMEOUT_MS = 10000; // aguarda 5s por respostas
  public static final String MULTICAST_GROUP = "230.0.0.1";
  public static final int MULTICAST_PORT = 4446;

  public static void main(String[] args) throws Exception {
    new ServidorPrincipal().start();
  }

  public void start() throws Exception {
    ServerSocket serverSocket = new ServerSocket(TCP_PORT);
    System.out.println("[PRINCIPAL] Aguardando clientes na porta: " + TCP_PORT);

    while (true) {
      Socket cliente = serverSocket.accept();
      new Thread(() -> clienteConectado(cliente)).start();
    }
  }

  private void clienteConectado(Socket cliente) {
    try {
      System.out.println("[PRINCIPAL] Cliente conectado: " + cliente.getInetAddress());
      BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
      PrintWriter out = new PrintWriter(cliente.getOutputStream(), true);

      String filename = in.readLine();
      System.out.println("[PRINCIPAL] Cliente pediu o arquivo: " + filename);

      // gera queryId e envia multicast indicando onde os servidores devem responder
      String queryId = UUID.randomUUID().toString();
      List<String> resultados = procurarArquivo(filename, queryId);

      // envia lista estruturada para o cliente: cada entrada "nome,ip,tcpPort,status"
      // separada por '|'
      String resposta = String.join("|", resultados);
      out.println(resposta);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private List<String> procurarArquivo(String fileName, String queryId) throws Exception {

    String msg = queryId + ";" + fileName + ";" + getLocalIP() + ";" + REPLY_PORT;

    InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
    try (DatagramSocket socket = new DatagramSocket()) {
      byte[] buf = msg.getBytes();
      DatagramPacket packet = new DatagramPacket(buf, buf.length, group, MULTICAST_PORT);
      socket.send(packet);
      System.out.println("[PRINCIPAL] Multicast enviado: " + msg);
    }

    List<String> respostas = new ArrayList<>();

    // abre socket para receber respostas UDP dos servidores de arquivo
    try (DatagramSocket replySocket = new DatagramSocket(REPLY_PORT)) {
      replySocket.setSoTimeout(TIMEOUT_MS);
      byte[] buffer = new byte[1024];

      long start = System.currentTimeMillis();
      while (true) {
        try {
          DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
          replySocket.receive(packet);
          String data = new String(packet.getData(), 0, packet.getLength());
          // esperado: queryId;serverName;serverIp;serverTcpPort;FOUND|NOTFOUND
          String[] parts = data.split(";");
          if (parts.length >= 5 && parts[0].equals(queryId)) {
            String serverName = parts[1];
            String serverIp = parts[2];
            String serverTcp = parts[3];
            String status = parts[4];
            String entry = serverName + "," + serverIp + "," + serverTcp + "," + status;
            respostas.add(entry);
            System.out.println("[PRINCIPAL] Resposta recebida: " + entry);
          } else {
            System.out.println("[PRINCIPAL] Resposta ignorada (A resposta tá toda atrapalhada): " + data);
          }
        } catch (SocketTimeoutException ste) {
          // timeout: verifica se já passou o tempo total
          if (System.currentTimeMillis() - start >= TIMEOUT_MS) {
            break;
          }
        }
      }
    }

    return respostas;
  }

  private String getLocalIP() throws Exception {
    return InetAddress.getLocalHost().getHostAddress();
  }
}