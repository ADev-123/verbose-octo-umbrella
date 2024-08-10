import java.io.*;
import java.net.*;
import java.util.Scanner;

public class FileTransferServer {

    private static final int DEFAULT_PORT = 5000;
    private ServerSocket serverSocket;
    private String sharedFolderPath;
    private String password;

    public FileTransferServer() {
        this.sharedFolderPath = "";
        this.password = "";
    }

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            // Get shared folder path and password from the user
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter the path to the folder you want to share: ");
            sharedFolderPath = scanner.nextLine();
            System.out.print("Enter a password for accessing the shared folder: ");
            password = scanner.nextLine();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected from " + clientSocket.getInetAddress());

                // Handle client connection in a separate thread
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port " + DEFAULT_PORT);
            }
        }
        FileTransferServer server = new FileTransferServer();
        server.start(port);
    }

    class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                // Authentication
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

                System.out.println("Waiting for client to enter password...");
                String clientPassword = (String) in.readObject();
                if (!clientPassword.equals(password)) {
                    System.out.println("Incorrect password. Disconnecting client.");
                    out.writeObject("Incorrect password.");
                    clientSocket.close();
                    return;
                }

                out.writeObject("Authentication successful.");
                // File transfer
                DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());

                // Receive file name
                String fileName = dis.readUTF();
                System.out.println("Receiving file: " + fileName);

                // Create file output stream
                File outputFile = new File(sharedFolderPath, fileName);
                FileOutputStream fos = new FileOutputStream(outputFile);

                // Receive file size
                long fileSize = dis.readLong();

                // Receive and write file data
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;
                while (totalBytesRead < fileSize && (bytesRead = dis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }

                fos.close();
                System.out.println("File received successfully.");

                // Send confirmation to client
                dos.writeUTF("File received successfully.");

                clientSocket.close();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error handling client connection: " + e.getMessage());
            }
        }
    }
}
