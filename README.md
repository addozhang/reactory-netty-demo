# Reactor Netty Proxy Simulation Demo

This project is for testing and simulating the "Connection prematurely closed BEFORE response" exception when using reactor-netty with an HTTP proxy. It provides a reproducible environment with a Spring Boot client and server, and a Squid proxy, to help investigate and debug connection issues that may occur when using proxies with reactor-netty.

## How to Generate and Use a Self-Signed Certificate for demo-server

This guide explains how to generate a self-signed SSL certificate for the demo-server module and configure Spring Boot to use it.

### 1. How the Certificates Are Generated

Configure OpenSSL to generate a root CA and a server certificate. You can run the following commands in your terminal (`demo-server` should be replaced with your actual server name):

```bash

```shell
# Generate a root CA
openssl genrsa -out demo-root-ca.key 2048
openssl req -x509 -new -nodes -key demo-root-ca.key -sha256 -days 3650 -out demo-root-ca.crt -subj "/CN=DemoRootCA"
# Generate a server key and CSR
openssl req -new -newkey rsa:2048 -nodes -keyout demo-server.key -out demo-server.csr -subj "/CN=demo-server"
# Sign the server certificate with the root CA
openssl x509 -req -in demo-server.csr -CA demo-root-ca.crt -CAkey demo-root-ca.key -CAcreateserial -out demo-server.crt -days 3650 -sha256
# Create a PKCS12 keystore for Spring Boot
openssl pkcs12 -export -in demo-server.crt -inkey demo-server.key -certfile demo-root-ca.crt -out demo-server-keystore.p12 -name demo-server -password pass:changeit
# Move the Keystore File
cp demo-server-keystore.p12 demo-server/src/main/resources
# Move the Root CA Certificate
cp demo-root-ca.crt demo-client/src/main/resources
```

### 2. Configure Spring Boot for SSL

Edit `demo-server/src/main/resources/application.properties` and add:

```
server.port=8080
server.ssl.enabled=true
server.ssl.key-store=classpath:demo-server-keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=demo-server
```

### 3. Run the Server

Start demo-server as usual. It will now serve HTTPS on port 8080.

### 4. Add Hosts File Entry

Change to your IP address.

```
192.168.31.13  demo-server
```

### 5. Trust the Certificate (Optional)

Your browser or HTTP client may warn about the self-signed certificate. You can choose to trust it for local development.

### 6. Trust the Root CA in Your Client

To securely connect to demo-server, configure your client to trust the root CA:

- For curl:

```shell
curl --cacert ./demo-root-ca.crt https://demo-server:8080/greeting
```

---

Now your demo-server uses a certificate signed by your root CA, and clients can trust it by using `demo-root-ca.crt`.

---

**Note:**
- For production, use certificates from a trusted CA.
- Update the password and alias as needed for your environment.

## How to Build and Run Containers

To build and run the Squid proxy, demo-server, and demo-client containers, follow these steps:

### 1. Build Docker Images

Run the following commands in your project root:

```sh
# Build Squid proxy image
docker build -t squid:latest -f Dockerfile.squid .

# Build demo-server image
docker build -t demo-server:latest -f Dockerfile.server .

# Build demo-client image
docker build -t demo-client:latest -f Dockerfile.client .
```

### 2. Start All Containers with Docker Compose

Use docker-compose to start all services:

```sh
docker-compose up
```

This will start the Squid proxy, demo-server, and demo-client containers as defined in `docker-compose.yaml`.

- demo-server will be available at https://demo-server:8080
- demo-client will be available at http://localhost:8081
- Squid proxy will be available at http://localhost:3128

To stop all containers, run:

```sh
docker-compose down
```
