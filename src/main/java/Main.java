import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class Main {
  public static void main(String[] args) {
    ServerSocket serverSocket = null;
    Socket clientSocket = null;
    try {
      String directoryString = null;
      if (args.length > 1 && "--directory".equals(args[0])) {
        directoryString = args[1];
      }
      // Connect
      serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);
      while (true) {
        clientSocket = serverSocket.accept();
        System.out.println("accepted new connection");
        RequestHandler handler = new RequestHandler(clientSocket.getInputStream(),
            clientSocket.getOutputStream(), directoryString);
        handler.start();
      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}

class RequestHandler extends Thread {
  private InputStream inputStream;
  private OutputStream outputStream;
  private String fileDir;

  RequestHandler(InputStream inputStream, OutputStream outputStream,
      String fileDir) {
    this.inputStream = inputStream;
    this.outputStream = outputStream;
    this.fileDir = fileDir == null ? "" : fileDir + File.separator;
  }

  public void run() {
    try {
      // Read
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
      String requestLine = bufferedReader.readLine();
      Map<String, String> requestHeaders = new HashMap<String, String>();
      String header = null;
      while ((header = bufferedReader.readLine()) != null && !header.isEmpty()) {
        String[] keyVal = header.split(":", 2);
        if (keyVal.length == 2) {
          requestHeaders.put(keyVal[0].toLowerCase(), keyVal[1].trim());
        }
      }
      // Read body
      StringBuffer bodyBuffer = new StringBuffer();
      while (bufferedReader.ready()) {
        bodyBuffer.append((char) bufferedReader.read());
      }
      String body = bodyBuffer.toString();
      // Process
      String[] requestLinePieces = requestLine.split(" ", 3);
      String httpMethod = requestLinePieces[0];
      String requestTarget = requestLinePieces[1];
      String httpVersion = requestLinePieces[2];

      // Determine if client accepts gzip encoding
      boolean acceptGzip = false;
      if (requestHeaders.containsKey("accept-encoding")) {
        String[] encodings = requestHeaders.get("accept-encoding").split(",");
        for (String encoding : encodings) {
          if (encoding.trim().equalsIgnoreCase("gzip")) {
            acceptGzip = true;
            break;
          }
        }
      }

      // Write
      if ("POST".equals(httpMethod)) {
        if (requestTarget.startsWith("/files/")) {
          File file = new File(fileDir + requestTarget.substring(7));
          if (file.createNewFile()) {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(body);
            fileWriter.close();
          }
          outputStream.write("HTTP/1.1 201 Created\r\n\r\n".getBytes());
        } else {
          outputStream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
        }
        outputStream.flush();
        outputStream.close();
        return;
      }
      if (requestTarget.equals("/")) {
        outputStream.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
      } else if (requestTarget.startsWith("/echo/")) {
        String echoString = requestTarget.substring(6);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        if (acceptGzip) {
          GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
          gzipOutputStream.write(echoString.getBytes());
          gzipOutputStream.close();
        } else {
          byteArrayOutputStream.write(echoString.getBytes());
        }
        byte[] responseBytes = byteArrayOutputStream.toByteArray();
        String outputString = "HTTP/1.1 200 OK\r\n"
            + "Content-Type: text/plain\r\n"
            + "Content-Length: " + responseBytes.length + "\r\n";
        if (acceptGzip) {
          outputString += "Content-Encoding: gzip\r\n";
        }
        outputString += "\r\n";
        outputStream.write(outputString.getBytes());
        outputStream.write(responseBytes);
      } else if (requestTarget.equals("/user-agent")) {
        String outputString = "HTTP/1.1 200 OK\r\n"
            + "Content-Type: text/plain\r\n"
            + "Content-Length: " + requestHeaders.get("user-agent").length() +
            "\r\n"
            + "\r\n" + requestHeaders.get("user-agent");
        outputStream.write(outputString.getBytes());

      } else if (requestTarget.startsWith("/files/")) {
        String fileName = requestTarget.substring(7);
        FileReader fileReader;
        try {
          fileReader = new FileReader(fileDir + fileName);
        } catch (FileNotFoundException e) {
          outputStream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
          outputStream.flush();
          outputStream.close();
          return;
        }
        BufferedReader bufferedFileReader = new BufferedReader(fileReader);
        StringBuffer stringBuffer = new StringBuffer();
        String line;
        while ((line = bufferedFileReader.readLine()) != null) {
          stringBuffer.append(line);
        }
        bufferedFileReader.close();
        fileReader.close();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        if (acceptGzip) {
          GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
          gzipOutputStream.write(stringBuffer.toString().getBytes());
          gzipOutputStream.close();
        } else {
          byteArrayOutputStream.write(stringBuffer.toString().getBytes());
        }
        byte[] responseBytes = byteArrayOutputStream.toByteArray();
        String outputString = "HTTP/1.1 200 OK\r\n"
            + "Content-Type: application/octet-stream\r\n"
            + "Content-Length: " + responseBytes.length + "\r\n";
        if (acceptGzip) {
          outputString += "Content-Encoding: gzip\r\n";
        }
        outputString += "\r\n";
        outputStream.write(outputString.getBytes());
        outputStream.write(responseBytes);
      } else {
        outputStream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
      }
      outputStream.flush();
      outputStream.close();
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}
