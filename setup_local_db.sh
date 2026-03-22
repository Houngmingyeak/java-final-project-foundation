#!/bin/bash

# Setup local PostgreSQL database for chat console

echo "Setting up local PostgreSQL database..."

# Create database user
echo "Creating database user 'ppc'..."
createuser -s ppc 2>/dev/null || echo "User 'ppc' already exists"

# Create database
echo "Creating database 'chat_console'..."
createdb chat_console -O ppc 2>/dev/null || echo "Database 'chat_console' already exists"

# Set password for user
echo "Setting password for user 'ppc'..."
psql -d chat_console -c "ALTER USER ppc WITH PASSWORD 'localpassword';" 2>/dev/null

echo "Database setup complete!"
echo "Database: chat_console"
echo "User: ppc"
echo "Password: localpassword"
echo ""
echo "To test the connection, run:"
echo "  psql -U ppc -d chat_console -h localhost"