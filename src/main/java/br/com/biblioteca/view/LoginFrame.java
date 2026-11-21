package br.com.biblioteca.view;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.formdev.flatlaf.FlatClientProperties;

import br.com.biblioteca.exception.NegocioException;
import br.com.biblioteca.model.Funcionario;
import br.com.biblioteca.service.AutenticacaoService;

/** Tela de login (RN08). */
public class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final JTextField campoLogin = new JTextField(20);
    private final JPasswordField campoSenha = new JPasswordField(20);
    private final AutenticacaoService autenticacaoService = new AutenticacaoService();

    public LoginFrame() {
        super("Sistema de Biblioteca – Entrar");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(new PainelMarca(), BorderLayout.WEST);
        raiz.add(montarFormulario(), BorderLayout.CENTER);
        setContentPane(raiz);

        pack();
        setResizable(false);
        setLocationRelativeTo(null);   // centraliza na tela
    }

    private JPanel montarFormulario() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JLabel titulo = new JLabel("Entrar");
        titulo.putClientProperty(FlatClientProperties.STYLE_CLASS, "h1");

        JLabel subtitulo = new JLabel("Acesse com seu usuário de funcionário");
        subtitulo.putClientProperty(FlatClientProperties.STYLE_CLASS, "medium");

        campoLogin.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "seu login");
        campoSenha.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "sua senha");

        JButton botaoEntrar = new JButton("Entrar");
        botaoEntrar.addActionListener(e -> entrar());   // lambda: o que fazer no clique
        getRootPane().setDefaultButton(botaoEntrar);    // Enter aciona o botão

        JLabel dica = new JLabel("Primeiro acesso: admin / admin123");
        dica.putClientProperty(FlatClientProperties.STYLE_CLASS, "small");
        dica.setEnabled(false);   // texto acinzentado

        // GridBagLayout: cada componente numa "linha" da grade, ocupando toda a largura
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        c.gridy = 0; c.insets = new Insets(0, 0, 4, 0);   form.add(titulo, c);
        c.gridy = 1; c.insets = new Insets(0, 0, 28, 0);  form.add(subtitulo, c);
        c.gridy = 2; c.insets = new Insets(0, 0, 6, 0);   form.add(new JLabel("Usuário"), c);
        c.gridy = 3; c.insets = new Insets(0, 0, 16, 0);  form.add(campoLogin, c);
        c.gridy = 4; c.insets = new Insets(0, 0, 6, 0);   form.add(new JLabel("Senha"), c);
        c.gridy = 5; c.insets = new Insets(0, 0, 24, 0);  form.add(campoSenha, c);
        c.gridy = 6; c.insets = new Insets(0, 0, 16, 0);  form.add(botaoEntrar, c);
        c.gridy = 7; c.insets = new Insets(0, 0, 0, 0);   form.add(dica, c);

        return form;
    }

    private void entrar() {
        String login = campoLogin.getText();
        String senha = new String(campoSenha.getPassword());

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));   // o hash leva um instante
        try {
            Funcionario funcionario = autenticacaoService.autenticar(login, senha);
            new MainFrame(funcionario).setVisible(true);
            dispose();   // fecha a tela de login

        // >>> TRATAMENTO DE EXCEÇÕES NA UI
        } catch (NegocioException e) {
            // Regra violada (login inválido): aviso amarelo, o usuário tenta de novo
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Não foi possível entrar", JOptionPane.WARNING_MESSAGE);
            campoSenha.setText("");
            campoSenha.requestFocusInWindow();
        } catch (SQLException e) {
            // Falha técnica: erro vermelho
            JOptionPane.showMessageDialog(this,
                "Erro ao acessar o banco de dados:\n" + e.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        } finally {
            // finally: executa SEMPRE, com ou sem exceção
            setCursor(Cursor.getDefaultCursor());
        }
    }
}