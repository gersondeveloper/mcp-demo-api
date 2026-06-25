# mcp-demo-api

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## API Documentation (Swagger UI)

The Swagger UI is available in all environments (dev and production) at:

- **Swagger UI:** <http://localhost:8080/q/swagger-ui>
- **OpenAPI spec (JSON/YAML):** <http://localhost:8080/q/openapi>

Endpoints documented:

| Resource | Base path |
|---|---|
| Users | `/api/users` |
| Products | `/api/products` |
| Orders | `/api/orders` |
| Order Items | `/api/orderItems` |

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/mcp-demo-api-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): Build RESTful web services and APIs using Jakarta REST (formerly JAX-RS)
- Hibernate ORM ([guide](https://quarkus.io/guides/hibernate-orm)): Object-relational mapping with JPA/Hibernate for relational database access
- JDBC Driver - PostgreSQL ([guide](https://quarkus.io/guides/datasource)): Connect to the PostgreSQL database via JDBC

## Provided Code

### Hibernate ORM

Create your first JPA entity

[Related guide section...](https://quarkus.io/guides/hibernate-orm)




### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

## MCP Server (Claude Desktop)

O MCP server expõe as operações da API para o Claude Desktop via linguagem natural, permitindo automação de cadastros e consultas sem navegar por telas.

### Pré-requisitos

- Node.js 18+

### Build

```shell script
cd mcp-server
npm install
npm run build
```

### Configuração no Claude Desktop

Editar o arquivo de configuração do Claude Desktop:

- **macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows:** `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "mcp-demo-api": {
      "command": "node",
      "args": ["/caminho/absoluto/para/mcp-demo-api/mcp-server/dist/index.js"]
    }
  }
}
```

Reiniciar o Claude Desktop após salvar para carregar o servidor.

### Tools disponíveis

| Tool | Descrição |
|------|-----------|
| `list_products` | Lista todos os produtos ativos |
| `create_product` | Cria um único produto |
| `create_products_batch` | Cria múltiplos produtos de uma vez (em paralelo) |
| `list_orders` | Lista todos os pedidos ativos |
| `create_order` | Cria um pedido com itens para um usuário |

### Rodando a API antes da demo

```shell script
docker-compose up -d        # sobe o PostgreSQL
./mvnw quarkus:dev          # sobe a API em localhost:8080
```
