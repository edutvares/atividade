import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TelaSimples extends JFrame {

  private JLabel label;
  private int contador = 0;

  public TelaSimples() {
    super("'Baixador' de arquivos");

    // Layout
    setLayout(new FlowLayout());

    // Texto inicial
    // label = new JLabel("Clique nos botões para ver ações!");
    // add(label);

    JTextField textfield1 = new JTextField();
    textfield1.setBounds(120, 10, 150, 20);
    add(textfield1);

    // Botão 1
    JButton botaoMensagem = new JButton("Mostrar Mensagem");
    botaoMensagem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JOptionPane.showMessageDialog(
            TelaSimples.this,
            "Ação realizada com sucesso!",
            "Mensagem",
            JOptionPane.INFORMATION_MESSAGE);
      }
    });
    add(botaoMensagem);

    // Botão 2
    JButton botaoContador = new JButton("Aumentar Contador");
    botaoContador.addActionListener(e -> {
      contador++;
      label.setText("Contador: " + contador);
    });
    add(botaoContador);

    // Configurações da janela
    setSize(350, 200);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null); // centraliza
    setVisible(true);
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(TelaSimples::new);
  }
}
