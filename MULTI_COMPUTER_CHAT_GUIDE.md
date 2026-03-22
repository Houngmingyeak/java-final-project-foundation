# Multi-Computer Chat Setup Guide

## Overview
This guide explains how to set up chat between multiple computers using your local network.

## Setup Instructions

### 1. Start the Chat Server
1. Run the ServerLauncher application:
   ```bash
   ./mvnw exec:java -Dexec.mainClass="ecocam.project_chat_console.ServerLauncher"
   ```

2. In the Server Launcher GUI:
   - Set Host to `0.0.0.0` (to listen on all network interfaces)
   - Set Port to `5000` (or your preferred port)
   - Click "Start Server"
   - Note the IP addresses displayed in the interface

### 2. Find Your Computer's IP Address
The server launcher will show your local IP addresses. Common ones look like:
- `192.168.x.x` (private network)
- `10.x.x.x` (private network)
- Your specific IP: `136.228.158.126` (as configured in database.properties)

### 3. Connect from Another Computer
On the second computer:

#### Option A: Use the Test Client
```bash
./mvnw exec:java -Dexec.mainClass="ecocam.project_chat_console.TestMultiComputerChat"
```

#### Option B: Use the GUI Application
1. Run the main application:
   ```bash
   ./mvnw javafx:run
   ```

2. In the login screen:
   - Set Server Host to the IP address of the computer running the server
   - Set Server Port to match what you configured on the server
   - Enter your username and password
   - Click "Sign In"

### 4. Configuration Files
The `database.properties` file contains:
```properties
# Database Configuration
db.url=jdbc:postgresql://saveun-2032-saveun2032.d.aivencloud.com:18245/defaultdb?sslmode=require
db.user=avnadmin
db.password=AVNS_No_5F43g81vthkGQMnb
server.host=136.228.158.126
server.port=5555
```

## Network Requirements
- Both computers must be on the same network
- Firewalls should allow the specified port (default 5000)
- No network address translation (NAT) between computers

## Testing
1. Start server on Computer A
2. Note the IP address of Computer A
3. On Computer B, connect using Computer A's IP address
4. Both computers should be able to chat in real-time

## Troubleshooting
- If connection fails, check firewall settings
- Ensure both computers can ping each other
- Verify the port is not blocked by network security
- Check that the server is actually running and listening

## Security Notes
- This setup is for local network use
- For internet use, additional security measures would be needed
- The database credentials are stored in plain text in the properties file