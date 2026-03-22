package ecocam.project_chat_console;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class ShowLocalIP {
    public static void main(String[] args) {
        System.out.println("=== Local IP Address Information ===\n");
        
        try {
            // Get localhost information
            InetAddress localhost = InetAddress.getLocalHost();
            System.out.println("Hostname: " + localhost.getHostName());
            System.out.println("Localhost IP: " + localhost.getHostAddress());
            System.out.println();
            
            // Get all IP addresses
            System.out.println("All Network Interface IPs:");
            System.out.println("---------------------------");
            
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                
                // Skip loopback and down interfaces
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                System.out.println("Interface: " + networkInterface.getName() + 
                                 " (" + networkInterface.getDisplayName() + ")");
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address.getHostAddress().contains(".")) {
                        System.out.println("  IPv4: " + address.getHostAddress());
                    } else if (!address.isLoopbackAddress() && address.getHostAddress().contains(":")) {
                        System.out.println("  IPv6: " + address.getHostAddress());
                    }
                }
                System.out.println();
            }
            
            // Show commonly used addresses for chat
            System.out.println("=== Recommended for Chat Server ===");
            showRecommendedAddresses();
            
        } catch (UnknownHostException e) {
            System.err.println("Unable to determine localhost: " + e.getMessage());
        } catch (SocketException e) {
            System.err.println("Network error: " + e.getMessage());
        }
    }
    
    private static void showRecommendedAddresses() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            System.out.println("Use these addresses for your chat server:");
            
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address.getHostAddress().contains(".")) {
                        // Check if it's a private network address
                        String ip = address.getHostAddress();
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            System.out.println("  " + ip + " (" + networkInterface.getName() + ")");
                        }
                    }
                }
            }
            System.out.println("\nFor local testing, you can also use:");
            System.out.println("  localhost or 127.0.0.1");
            
        } catch (SocketException e) {
            System.err.println("Error getting network interfaces: " + e.getMessage());
        }
    }
}