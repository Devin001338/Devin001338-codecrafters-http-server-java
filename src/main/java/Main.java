import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  public static void main(String[] args) {
    System.out.println("Logs from your program will appear here!");

    // Set the base directory for file lookups
    String baseDir = "/tmp/data/codecrafters.io/http-server-tester/";

    try (ServerSocket serverSocket = new ServerSocket(4221)) {
      serverSocket.setReuseAddress(true);

      while (true) {
        Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
        System.out.println("Accepted new connection");

        // Handle each connection in a new thread
        new Thread(new ClientHandler(clientSocket, baseDir)).start();
      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}

class ClientHandler implements Runnable {
  private Socket clientSocket;
  private String baseDir;

  public ClientHandler(Socket clientSocket, String baseDir) {
    this.clientSocket = clientSocket;
    this.baseDir = baseDir;
  }

  @Override
  public void run() {
    try {
      InputStream input = clientSocket.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(input));
      String line = reader.readLine();
      System.out.println("Request Line: " + line);

      if (line != null) {
        String[] HttpRequest = line.split(" ");
        String path = HttpRequest[1];
        System.out.println("Requested Path: " + path);

        // Read headers
        String userAgent = "";
        while (!(line = reader.readLine()).isEmpty()) {
          if (line.startsWith("User-Agent:")) {
            userAgent = line.substring("User-Agent:".length()).trim();
            break;
          }
        }
        System.out.println("User-Agent: " + userAgent);

        OutputStream output = clientSocket.getOutputStream();

        if ("/user-agent".equals(path)) {
          String responseBody = userAgent;
          String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                                   "Content-Type: text/plain\r\n" +
                                   "Content-Length: " + responseBody.length() + "\r\n" +
                                   "\r\n";
          output.write(responseHeaders.getBytes());
          output.write(responseBody.getBytes());
        } else if ("/".equals(path) || "/index.html".equals(path)) {
          String responseBody = "Welcome to the homepage!";
          String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                                   "Content-Type: text/plain\r\n" +
                                   "Content-Length: " + responseBody.length() + "\r\n" +
                                   "\r\n";
          output.write(responseHeaders.getBytes());
          output.write(responseBody.getBytes());
        } else if (path.startsWith("/echo/")) {
          String echoStr = path.substring(6); // Extract the string after "/echo/"
          String responseBody = echoStr;
          String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                                   "Content-Type: text/plain\r\n" +
                                   "Content-Length: " + responseBody.length() + "\r\n" +
                                   "\r\n";
          output.write(responseHeaders.getBytes());
          output.write(responseBody.getBytes());
        } else if (path.startsWith("/files/")) {
          String filename = baseDir + path.substring(7); // Extract the filename after "/files/"
          File file = new File(filename);

          if (file.exists() && !file.isDirectory()) {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] fileContent = new byte[(int) file.length()];
            fileInputStream.read(fileContent);

            String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                                     "Content-Type: application/octet-stream\r\n" +
                                     "Content-Length: " + fileContent.length + "\r\n" +
                                     "\r\n";
            output.write(responseHeaders.getBytes());
            output.write(fileContent);
            
            fileInputStream.close();
          } else {
            String responseHeaders = "HTTP/1.1 404 Not Found\r\n\r\n";
            output.write(responseHeaders.getBytes());
          }
        } else {
          String responseHeaders = "HTTP/1.1 404 Not Found\r\n\r\n";
          output.write(responseHeaders.getBytes());
        }

        output.close();
      }

      // Close the streams and socket
      reader.close();
      clientSocket.close();
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}
