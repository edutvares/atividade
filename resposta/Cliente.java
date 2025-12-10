import java.io.*;
import java.net.*;
import javax.swing.*;

public class Cliente {
  public static final int SERVIDOR_PRINCIPAL_PORT = 6000;
  public static final String MULTICAST_GROUP = "230.0.0.1";

  public static void main(String[] args) throws Exception {

    // espera pelo menos 2 argumentos: <filename> <host> para iniciar no modo
    // terminal
    if (args.length >= 2) {
      String filename = args[0];
      String principalHost = args[1];

      new Thread(() -> {
        try {
          pedirAoServidorPrincipal(filename, principalHost, null);
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }).start();

    } else {
      SwingUtilities.invokeLater(() -> {
        Tela tela = new Tela();

        tela.setAcaoBotao(e -> {
          System.out.println("Procurando arquivo: " + tela.getNomeArquivo());

          String filename = tela.getNomeArquivo();

          String principalHost;
          if (args.length >= 1) {
            principalHost = args[0];
          } else {
            principalHost = "localhost";
          }

          tela.iniciarBarraProgresso();

          new Thread(() -> {
            try {
              pedirAoServidorPrincipal(filename, principalHost, tela);
            } catch (Exception ex) {
              ex.printStackTrace();
            }
          }).start();

        });

        tela.mostrar();
      });
    }

  }

  private static void pedirAoServidorPrincipal(String filename, String host, Tela tela) throws Exception {
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

      // Se tela está disponível (uso via GUI), atualiza os botões de download
      if (tela != null) {
        SwingUtilities.invokeLater(() -> {
          tela.prepararBotoesDownload(resposta);
        });
      }

      socket.close();
    }
  }

  // Método para fazer download real do arquivo
  public static void baixarArquivo(String filename, String serverIp, int serverPort) {
    new Thread(() -> {
      try {
        // Abre diálogo para escolher local de salvamento
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Escolha o local para salvar o arquivo");
        fileChooser.setSelectedFile(new File(filename));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int resultado = fileChooser.showSaveDialog(null);

        if (resultado != JFileChooser.APPROVE_OPTION) {
          System.out.println("[CLIENTE] Download cancelado");
          return;
        }

        File arquivoLocal = fileChooser.getSelectedFile();

        // Verifica se arquivo já existe
        if (arquivoLocal.exists()) {
          int resposta = JOptionPane.showConfirmDialog(null,
              "Arquivo já existe. Deseja sobrescrever?",
              "Arquivo Existente",
              JOptionPane.YES_NO_OPTION);
          if (resposta != JOptionPane.YES_OPTION) {
            System.out.println("[CLIENTE] Download cancelado - arquivo não sobrescrito");
            return;
          }
        }

        // Conecta ao servidor e faz download
        Socket socket = new Socket(serverIp, serverPort);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedInputStream in = new BufferedInputStream(socket.getInputStream());

        // Envia requisição do arquivo
        out.println(filename);

        // Recebe arquivo do servidor
        FileOutputStream fos = new FileOutputStream(arquivoLocal);
        byte[] buffer = new byte[4096];
        int bytesLidos;
        while ((bytesLidos = in.read(buffer)) != -1) {
          fos.write(buffer, 0, bytesLidos);
        }

        fos.close();
        in.close();
        socket.close();

        System.out
            .println("[CLIENTE] Arquivo " + filename + " baixado com sucesso em: " + arquivoLocal.getAbsolutePath());
        JOptionPane.showMessageDialog(null, "Arquivo baixado com sucesso!\n" + arquivoLocal.getAbsolutePath(),
            "Sucesso", JOptionPane.INFORMATION_MESSAGE);
      } catch (Exception ex) {
        System.err.println("[CLIENTE] Erro ao baixar arquivo: " + ex.getMessage());
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, "Erro ao baixar arquivo: " + ex.getMessage(), "Erro",
            JOptionPane.ERROR_MESSAGE);
      }
    }).start();
  }
}