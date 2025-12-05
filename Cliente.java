import java.io.*;
import java.net.*;
import java.util.*;

public class Cliente {

    public static final int SERVIDOR_PRINCIPAL_PORT = 6000;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Uso: java Cliente <filename> <hostPrincipal>");
            return;
        }

        String filename = args[0];
        String principalHost = args[1];

        List<String> servidores = pedirAoServidorPrincipal(filename, principalHost);

        if (servidores.isEmpty()) {
            System.out.println("Nenhum servidor possui o arquivo.");
            return;
        }

        System.out.println("Servidores encontrados:");
        for (String s : servidores) System.out.println(" - " + s);

        // Formato: nome;ip;porta
        String[] parts = servidores.get(0).split(";");

        String ip = parts[2];
        int porta = Integer.parseInt(parts[3]);

        baixarArquivo(ip, porta, filename);
    }

    private static List<String> pedirAoServidorPrincipal(String filename, String host) throws Exception {
        Socket socket = new Socket(host, SERVIDOR_PRINCIPAL_PORT);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println(filename);

        String resposta = in.readLine();
        socket.close();

        if (resposta == null || resposta.isEmpty()) return new ArrayList<>();

        return Arrays.asList(resposta.split(";"));
    }

    private static void baixarArquivo(String ip, int port, String filename) throws Exception {
        Socket socket = new Socket(ip, port);

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        InputStream is = socket.getInputStream();

        out.println(filename);

        String header = in.readLine();
        if (header.equals("NOT_FOUND")) {
            System.out.println("Arquivo não encontrado no servidor.");
            return;
        }

        String[] parts = header.split(";");
        long size = Long.parseLong(parts[1]);

        FileOutputStream fos = new FileOutputStream(filename);

        long recebidos = 0;
        byte[] buffer = new byte[4096];
        int l;

        while (recebidos < size && (l = is.read(buffer)) != -1) {
            fos.write(buffer, 0, l);
            recebidos += l;
        }

        fos.close();
        socket.close();

        System.out.println("Download concluído: " + filename + " (" + recebidos + " bytes)");
    }
}
