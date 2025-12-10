import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;

public class Tela extends JFrame {

  private JTextField nomeArquivo;
  private JButton botao;
  private JPanel painelDownloads;
  private String nomeArquivoAtual;
  private JProgressBar barraProgresso;
  private javax.swing.Timer timerProgresso;

  public Tela() {
    super("'Baixador' de arquivos");

    // Layout principal: vertical (BoxLayout)
    setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

    // Painel de busca
    JPanel painelBusca = new JPanel(new FlowLayout());
    nomeArquivo = new JTextField(20);
    botao = new JButton("Procurar Arquivo");
    painelBusca.add(nomeArquivo);
    painelBusca.add(botao);
    add(painelBusca);

    // Painel de downloads (vazio inicialmente)
    painelDownloads = new JPanel(new FlowLayout(FlowLayout.LEFT));
    painelDownloads.setBorder(BorderFactory.createTitledBorder("Servidores com arquivo:"));
    add(painelDownloads);

    // Barra de progresso (inicialmente invisível)
    barraProgresso = new JProgressBar(0, 100);
    barraProgresso.setStringPainted(true);
    barraProgresso.setVisible(false);
    add(barraProgresso);

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(500, 250);
    setLocationRelativeTo(null);

    // // Texto inicial
    // // label = new JLabel("Clique nos botões para ver ações!");
    // // add(label);

    // JTextField textfield1 = new JTextField();
    // textfield1.setBounds(120, 10, 150, 20);
    // add(textfield1);

    // // Botão 1
    // JButton botaoMensagem = new JButton("Mostrar Mensagem");
    // botaoMensagem.addActionListener(new ActionListener() {
    // @Override
    // public void actionPerformed(ActionEvent e) {
    // JOptionPane.showMessageDialog(
    // TelaSimples.this,
    // "Ação realizada com sucesso!",
    // "Mensagem",
    // JOptionPane.INFORMATION_MESSAGE);
    // }
    // });
    // add(botaoMensagem);

    // // Botão 2
    // JButton botaoContador = new JButton("Aumentar Contador");
    // botaoContador.addActionListener(e -> {
    // contador++;
    // label.setText("Contador: " + contador);
    // });
    // add(botaoContador);

    // // Configurações da janela
    // setSize(350, 200);
    // setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    // setLocationRelativeTo(null); // centraliza
    // setVisible(true);
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(Tela::new);
  }

  public void mostrar() {
    setVisible(true);
  }

  public void setAcaoBotao(ActionListener listener) {
    botao.addActionListener(listener);
  }

  public String getNomeArquivo() {
    return nomeArquivo.getText();
  }

  public void iniciarBarraProgresso() {
    barraProgresso.setValue(0);
    barraProgresso.setVisible(true);
    painelDownloads.removeAll();
    painelDownloads.revalidate();
    painelDownloads.repaint();

    // Timer que incrementa a barra a cada 100ms (total 10s = 100 incrementos)
    timerProgresso = new javax.swing.Timer(100, e -> {
      int valor = barraProgresso.getValue();
      if (valor < 100) {
        barraProgresso.setValue(valor + 1);
      }
    });
    timerProgresso.start();
  }

  public void pararBarraProgresso() {
    if (timerProgresso != null) {
      timerProgresso.stop();
    }
    barraProgresso.setVisible(false);
  }

  public void prepararBotoesDownload(String resposta) {
    // Para a barra de progresso
    pararBarraProgresso();

    // Armazena nome do arquivo atual para usar nos downloads
    nomeArquivoAtual = resposta == null ? null : getNomeArquivo();

    // Limpar botões anteriores
    painelDownloads.removeAll();

    if (resposta == null || resposta.trim().isEmpty()) {
      JLabel label = new JLabel("Nenhum servidor disponível.");
      painelDownloads.add(label);
    } else {
      // Parse resposta: "nome,ip,tcpPort,status|nome,ip,tcpPort,status|..."
      String[] entries = resposta.split("\\|");

      for (String entry : entries) {
        String[] parts = entry.split(",");
        if (parts.length >= 4) {
          String serverName = parts[0];
          String serverIp = parts[1];
          String serverTcpPorta = parts[2];
          String status = parts[3];

          JButton botaoDownload = new JButton(serverName);

          // Habilita apenas servidores com FOUND
          if ("FOUND".equals(status)) {
            botaoDownload.setEnabled(true);
            botaoDownload.setToolTipText(serverIp + ":" + serverTcpPorta);
            // Adiciona ActionListener para fazer download
            String finalServerIp = serverIp;
            int finalServerTcp = Integer.parseInt(serverTcpPorta);
            botaoDownload.addActionListener(e -> {
              Cliente.baixarArquivo(nomeArquivoAtual, finalServerIp, finalServerTcp);
            });
          } else {
            botaoDownload.setEnabled(false);
            botaoDownload.setToolTipText(serverIp + ":" + serverTcpPorta + " (arquivo não encontrado)");
          }

          painelDownloads.add(botaoDownload);
        }
      }
    }

    // Atualizar layout
    painelDownloads.revalidate();
    painelDownloads.repaint();
  }
}
