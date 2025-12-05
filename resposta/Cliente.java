import java.io.*;
import java.net.*;
import java.util.*;

public class Cliente {
  public static final int SERVIDOR_PRINCIPAL_PORT = 6000;
  public static final String MULTICAST_GROUP = "230.0.0.1";

  public static void main(String[] args) throws Exception {

    if (args.length < 2) {
      System.out.println("Uso: java Cliente <filename> <hostPrincipal>");
      return;
    }

    String filename = args[0];
    String principalHost = args[1];

    List<String> servidores = pedirAoServidorPrincipal(filename, principalHost);
  }

  private static List<String> pedirAoServidorPrincipal(String filename, String host) throws Exception {
    try (Socket socket = new Socket(host, SERVIDOR_PRINCIPAL_PORT)) {

      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

      out.println(filename);

      String resposta = in.readLine();
      System.out.println("Resposta do servidor principal: " + resposta);

      if (resposta == null || resposta.trim().isEmpty()) {
        System.out.println("Nenhum servidor respondeu que possui o arquivo.");
      } else {
        String[] entries = resposta.split("\\|");
        for (String e : entries) {
          // formato: nome,ip,tcpPort,status
          String[] p = e.split(",");
          if (p.length >= 4) {
            System.out.println("Servidor: " + p[0] + " - " + p[1] + ":" + p[2] + " - " + p[3]);
          } else {
            System.out.println("Entrada desconhecida: " + e);
          }
        }
      }
      socket.close();
    }

    return new ArrayList<>();
  }
}