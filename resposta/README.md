# Tá meio atrapalhado mas vai dar certo

## Como rodar

- Compila tudo com javac

```
javac *.java
```

- Inicia o servidor principal

`java ServidorPrincipal`

- Inicia os servidores de arquivo

`java ServidorArquivo <Caminho da pasta com arquivos> <nome do servidor> <Essa ia ser a porta para baixar os arquivos>`
Exemplo: `java ServidorArquivo pasta2 SERV2 4556`

Agora é só solicitar o arquivo no cliente

`javac ./Cliente.java && java Cliente arquivo2.txt 192.168.1.7`

Ele faz o broadcast e aguarda os 10 segundos. Quem responder ele cria uma lista com os resultados. (Isso ia virar uma interface bonitinha depois)
