package ecocam.project_chat_console.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DiscoveryServer implements Runnable {
    private static final int DISCOVERY_PORT = 8888; // Port សម្រាប់ UDP Discovery
    private DatagramSocket socket;

    @Override
    public void run() {
        try {
            // បើក Socket ដើម្បីចាំស្តាប់ការសួររក IP ពីគ្រប់ម៉ាស៊ីន (0.0.0.0)
            socket = new DatagramSocket(DISCOVERY_PORT, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);
            System.out.println("UDP Discovery Server started on port " + DISCOVERY_PORT);

            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // ចាំទទួល Packet ពី Client

                String message = new String(packet.getData()).trim();

                // បើ Client សួរមកចំពាក្យសម្ងាត់ "DISCOVER_HYPERCHAT_SERVER"
                if ("DISCOVER_HYPERCHAT_SERVER".equals(message)) {
                    byte[] responseData = "HYPERCHAT_SERVER_HERE".getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(
                            responseData, responseData.length,
                            packet.getAddress(), packet.getPort()
                    );
                    socket.send(responsePacket); // ឆ្លើយតបទៅ Client វិញភ្លាមៗ
                }
            }
        } catch (IOException e) {
            System.err.println("Discovery Server Error: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) socket.close();
        }
    }
}