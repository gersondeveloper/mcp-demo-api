# MCP Server — Design Spec

**Data:** 2026-06-25
**Status:** Aprovado

## Contexto

O cliente possui um SaaS onde usuários navegam por múltiplas telas para realizar cadastros e consultas. O objetivo deste MVP é demonstrar, via Claude Desktop, que é possível automatizar essas operações em linguagem natural — cadastrar vários produtos de uma vez e consultar dados como relatórios, eliminando a necessidade de navegar por telas.

## Escopo

MCP server em TypeScript que expõe tools para o Claude Desktop interagir com a API Quarkus (`mcp-demo-api`) rodando em `localhost:8080`.

Fora do escopo: autenticação, testes automatizados, deploy remoto.

## Estrutura

```
mcp-demo-api/
├── src/main/java/...        ← API Quarkus (existente)
├── mcp-server/
│   ├── src/
│   │   ├── index.ts         ← entry point, registra as tools no MCP server
│   │   ├── client.ts        ← HTTP client (fetch nativo) para a API
│   │   └── tools/
│   │       ├── products.ts  ← list_products, create_product, create_products_batch
│   │       └── orders.ts    ← list_orders, create_order
│   ├── package.json
│   └── tsconfig.json
├── docker-compose.yml
└── CLAUDE.md
```

## Dependências

- `@modelcontextprotocol/sdk` — SDK oficial Anthropic para TypeScript
- `typescript` + `@types/node` — build e tipagem
- Node 18+ (fetch nativo, sem dependências extras de HTTP)

## Tools Expostas

| Tool | Descrição | Parâmetros |
|------|-----------|------------|
| `list_products` | Lista todos os produtos ativos | — |
| `create_product` | Cria um produto | `name: string`, `description: string`, `price: number`, `stockQuantity: number` |
| `create_products_batch` | Cria múltiplos produtos em paralelo | `products: Array<{name, description, price, stockQuantity}>` |
| `list_orders` | Lista todos os pedidos ativos | — |
| `create_order` | Cria um pedido com itens | `userId: number`, `items: Array<{productId: number, quantity: number}>` |

## Transporte

stdio — o Claude Desktop spawna o processo `node dist/index.js` e se comunica via stdin/stdout. Não há porta de rede envolvida.

## Fluxo de Automação (cenário principal da demo)

```
Usuário → Claude Desktop
"Cadastre 3 produtos: Notebook R$3000 est.10, Mouse R$150 est.50, Teclado R$300 est.30"
         ↓
Claude invoca create_products_batch([...])
         ↓
MCP Server → Promise.all([POST /api/products, POST /api/products, POST /api/products])
         ↓
Claude responde com resumo dos produtos criados
```

## Fluxo de Consulta (cenário de report)

```
Usuário → Claude Desktop
"Quais produtos estão cadastrados?"
         ↓
Claude invoca list_products()
         ↓
MCP Server → GET /api/products
         ↓
Claude formata e apresenta como tabela/resumo em linguagem natural
```

## Error Handling

As tools capturam erros HTTP e retornam mensagens de texto descritivas (ex: `"Erro ao criar produto: dados inválidos"`). O Claude interpreta e comunica ao usuário de forma natural. Exceções de rede (API fora do ar) são capturadas e retornadas como mensagem de erro legível.

## Configuração no Claude Desktop

Arquivo:
- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
- Windows: `%APPDATA%\Claude\claude_desktop_config.json`

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

Após alterar o arquivo, reiniciar o Claude Desktop para carregar o MCP server.

## Decisões de Design

- **Batch em paralelo:** `create_products_batch` usa `Promise.all` — cria todos os produtos simultaneamente, mais rápido e impactante na demo.
- **Sem auth por ora:** A API local não tem autenticação. Se necessário no futuro, adicionar `Authorization` header no `client.ts`.
- **URL como constante:** `http://localhost:8080` definida em `client.ts`. Pode virar variável de ambiente futuramente sem mudança de arquitetura.
- **fetch nativo:** Node 18+ tem `fetch` built-in — zero dependências extras de HTTP.
