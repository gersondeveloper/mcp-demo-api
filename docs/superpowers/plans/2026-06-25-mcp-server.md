# MCP Server Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Criar um MCP server em TypeScript (pasta `mcp-server/`) que expõe 5 tools para o Claude Desktop interagir com a API Quarkus local em `localhost:8080`.

**Architecture:** Servidor MCP com transporte stdio organizado em módulos de tools por domínio (products, orders). Um HTTP client compartilhado baseado em `fetch` nativo centraliza as chamadas à API. O Claude Desktop spawna o processo `node dist/index.js` e se comunica via stdin/stdout.

**Tech Stack:** TypeScript 5, Node 18+, `@modelcontextprotocol/sdk` ^1.x, `zod` ^3.x

## Global Constraints

- Node 18+ obrigatório (`fetch` nativo, sem dependências extras de HTTP)
- `"type": "module"` no `package.json` — projeto usa ES Modules
- Imports TypeScript devem usar extensão `.js` (requisito do Node ESM com `module: Node16`)
- URL base da API: `http://localhost:8080` (constante em `client.ts`)
- Pasta raiz do MCP: `mcp-server/` dentro do repositório `mcp-demo-api`
- Sem testes automatizados (escopo MVP)

---

## File Map

| Arquivo | Responsabilidade |
|---------|-----------------|
| `mcp-server/package.json` | Dependências e scripts de build |
| `mcp-server/tsconfig.json` | Configuração TypeScript com ESM |
| `mcp-server/src/client.ts` | `apiGet` e `apiPost` com `fetch` nativo |
| `mcp-server/src/tools/products.ts` | `registerProductTools`: list_products, create_product, create_products_batch |
| `mcp-server/src/tools/orders.ts` | `registerOrderTools`: list_orders, create_order |
| `mcp-server/src/index.ts` | Entry point: instancia `McpServer`, registra tools, conecta `StdioServerTransport` |
| `README.md` | Seção de setup do MCP server (instalação, config Claude Desktop, tools) |

---

### Task 1: Scaffold do projeto mcp-server

**Files:**
- Create: `mcp-server/package.json`
- Create: `mcp-server/tsconfig.json`

**Interfaces:**
- Produces: projeto TypeScript compilável com `npm run build`

- [ ] **Step 1: Criar estrutura de pastas**

```bash
mkdir -p mcp-server/src/tools
```

- [ ] **Step 2: Criar `mcp-server/package.json`**

```json
{
  "name": "mcp-demo-api-server",
  "version": "1.0.0",
  "type": "module",
  "main": "dist/index.js",
  "scripts": {
    "build": "tsc",
    "start": "node dist/index.js"
  },
  "dependencies": {
    "@modelcontextprotocol/sdk": "^1.12.1",
    "zod": "^3.24.0"
  },
  "devDependencies": {
    "@types/node": "^22.15.29",
    "typescript": "^5.8.3"
  }
}
```

- [ ] **Step 3: Criar `mcp-server/tsconfig.json`**

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "Node16",
    "moduleResolution": "Node16",
    "outDir": "./dist",
    "rootDir": "./src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist"]
}
```

- [ ] **Step 4: Instalar dependências**

```bash
cd mcp-server && npm install
```

Expected: `node_modules/` criado, `package-lock.json` gerado sem erros.

- [ ] **Step 5: Commit**

```bash
git add mcp-server/package.json mcp-server/tsconfig.json mcp-server/package-lock.json
git commit -m "feat: scaffold mcp-server TypeScript project"
```

---

### Task 2: HTTP Client

**Files:**
- Create: `mcp-server/src/client.ts`

**Interfaces:**
- Produces:
  - `apiGet<T>(path: string): Promise<T>` — GET para `http://localhost:8080{path}`
  - `apiPost<T>(path: string, body: unknown): Promise<T>` — POST com `Content-Type: application/json`

- [ ] **Step 1: Criar `mcp-server/src/client.ts`**

```typescript
const BASE_URL = 'http://localhost:8080';

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`);
  if (!response.ok) {
    throw new Error(`GET ${path} falhou: ${response.status} ${response.statusText}`);
  }
  return response.json() as Promise<T>;
}

export async function apiPost<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(`POST ${path} falhou: ${response.status} - ${text}`);
  }
  return response.json() as Promise<T>;
}
```

- [ ] **Step 2: Verificar compilação**

```bash
cd mcp-server && npm run build
```

Expected: pasta `dist/` criada com `client.js`. Nenhum erro TypeScript.

- [ ] **Step 3: Commit**

```bash
git add mcp-server/src/client.ts
git commit -m "feat: add HTTP client for Quarkus API"
```

---

### Task 3: Products Tools

**Files:**
- Create: `mcp-server/src/tools/products.ts`

**Interfaces:**
- Consumes: `apiGet<T>(path: string): Promise<T>` e `apiPost<T>(path: string, body: unknown): Promise<T>` de `../client.js`
- Consumes: `McpServer` de `@modelcontextprotocol/sdk/server/mcp.js`
- Produces: `registerProductTools(server: McpServer): void`

- [ ] **Step 1: Criar `mcp-server/src/tools/products.ts`**

```typescript
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';
import { apiGet, apiPost } from '../client.js';

const productShape = {
  name: z.string().describe('Nome do produto'),
  description: z.string().describe('Descrição do produto'),
  price: z.number().positive().describe('Preço unitário'),
  stockQuantity: z.number().int().nonnegative().describe('Quantidade em estoque'),
};

export function registerProductTools(server: McpServer): void {
  server.tool(
    'list_products',
    'Lista todos os produtos ativos cadastrados na API',
    {},
    async () => {
      try {
        const products = await apiGet('/api/products');
        return { content: [{ type: 'text' as const, text: JSON.stringify(products, null, 2) }] };
      } catch (e) {
        return { content: [{ type: 'text' as const, text: `Erro ao listar produtos: ${(e as Error).message}` }] };
      }
    }
  );

  server.tool(
    'create_product',
    'Cria um único produto na API',
    productShape,
    async (args) => {
      try {
        const product = await apiPost('/api/products', args);
        return { content: [{ type: 'text' as const, text: `Produto criado com sucesso:\n${JSON.stringify(product, null, 2)}` }] };
      } catch (e) {
        return { content: [{ type: 'text' as const, text: `Erro ao criar produto: ${(e as Error).message}` }] };
      }
    }
  );

  server.tool(
    'create_products_batch',
    'Cria múltiplos produtos de uma só vez em paralelo. Use quando o usuário pedir para cadastrar mais de um produto ao mesmo tempo.',
    { products: z.array(z.object(productShape)).min(1).describe('Lista de produtos a criar') },
    async ({ products }) => {
      try {
        const results = await Promise.all(products.map(p => apiPost('/api/products', p)));
        return {
          content: [{
            type: 'text' as const,
            text: `${results.length} produto(s) criado(s) com sucesso:\n${JSON.stringify(results, null, 2)}`,
          }],
        };
      } catch (e) {
        return { content: [{ type: 'text' as const, text: `Erro ao criar produtos: ${(e as Error).message}` }] };
      }
    }
  );
}
```

- [ ] **Step 2: Verificar compilação**

```bash
cd mcp-server && npm run build
```

Expected: `dist/tools/products.js` gerado sem erros TypeScript.

- [ ] **Step 3: Commit**

```bash
git add mcp-server/src/tools/products.ts
git commit -m "feat: add product MCP tools (list, create, batch)"
```

---

### Task 4: Orders Tools

**Files:**
- Create: `mcp-server/src/tools/orders.ts`

**Interfaces:**
- Consumes: `apiGet<T>(path: string): Promise<T>` e `apiPost<T>(path: string, body: unknown): Promise<T>` de `../client.js`
- Consumes: `McpServer` de `@modelcontextprotocol/sdk/server/mcp.js`
- Produces: `registerOrderTools(server: McpServer): void`

- [ ] **Step 1: Criar `mcp-server/src/tools/orders.ts`**

```typescript
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';
import { apiGet, apiPost } from '../client.js';

export function registerOrderTools(server: McpServer): void {
  server.tool(
    'list_orders',
    'Lista todos os pedidos ativos cadastrados na API',
    {},
    async () => {
      try {
        const orders = await apiGet('/api/orders');
        return { content: [{ type: 'text' as const, text: JSON.stringify(orders, null, 2) }] };
      } catch (e) {
        return { content: [{ type: 'text' as const, text: `Erro ao listar pedidos: ${(e as Error).message}` }] };
      }
    }
  );

  server.tool(
    'create_order',
    'Cria um novo pedido para um usuário com um ou mais itens',
    {
      userId: z.number().int().positive().describe('ID do usuário que está fazendo o pedido'),
      items: z.array(z.object({
        productId: z.number().int().positive().describe('ID do produto'),
        quantity: z.number().int().positive().describe('Quantidade do produto'),
      })).min(1).describe('Lista de itens do pedido'),
    },
    async ({ userId, items }) => {
      try {
        const order = await apiPost('/api/orders', { userId, items });
        return { content: [{ type: 'text' as const, text: `Pedido criado com sucesso:\n${JSON.stringify(order, null, 2)}` }] };
      } catch (e) {
        return { content: [{ type: 'text' as const, text: `Erro ao criar pedido: ${(e as Error).message}` }] };
      }
    }
  );
}
```

- [ ] **Step 2: Verificar compilação**

```bash
cd mcp-server && npm run build
```

Expected: `dist/tools/orders.js` gerado sem erros TypeScript.

- [ ] **Step 3: Commit**

```bash
git add mcp-server/src/tools/orders.ts
git commit -m "feat: add order MCP tools (list, create)"
```

---

### Task 5: Entry Point e Smoke Test

**Files:**
- Create: `mcp-server/src/index.ts`

**Interfaces:**
- Consumes: `registerProductTools(server: McpServer): void` de `./tools/products.js`
- Consumes: `registerOrderTools(server: McpServer): void` de `./tools/orders.js`
- Consumes: `McpServer` de `@modelcontextprotocol/sdk/server/mcp.js`
- Consumes: `StdioServerTransport` de `@modelcontextprotocol/sdk/server/stdio.js`
- Produces: processo Node.js executável que o Claude Desktop conecta via stdio

- [ ] **Step 1: Criar `mcp-server/src/index.ts`**

```typescript
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { registerProductTools } from './tools/products.js';
import { registerOrderTools } from './tools/orders.js';

const server = new McpServer({
  name: 'mcp-demo-api',
  version: '1.0.0',
});

registerProductTools(server);
registerOrderTools(server);

const transport = new StdioServerTransport();
await server.connect(transport);
```

- [ ] **Step 2: Build final**

```bash
cd mcp-server && npm run build
```

Expected: `dist/index.js` gerado sem erros TypeScript.

- [ ] **Step 3: Smoke test — verificar que o processo responde ao protocolo MCP**

```bash
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}' | node mcp-server/dist/index.js
```

Expected: resposta JSON no stdout contendo `"protocolVersion"` e `"serverInfo"`. Nenhum stack trace.

- [ ] **Step 4: Commit**

```bash
git add mcp-server/src/index.ts
git commit -m "feat: add MCP server entry point with stdio transport"
```

---

### Task 6: Configuração no Claude Desktop e README

**Files:**
- Modify: `README.md`
- External: `claude_desktop_config.json` (fora do repositório)

**Interfaces:**
- Consumes: `mcp-server/dist/index.js` (gerado no Task 5)

- [ ] **Step 1: Build de produção limpo**

```bash
cd mcp-server && npm run build
```

Confirmar que `mcp-server/dist/index.js` existe.

- [ ] **Step 2: Obter o caminho absoluto do projeto**

```bash
pwd
```

Anotar o output (ex: `/home/gerson/workspace/mcp-demo-api`). Será usado no config do Claude Desktop.

- [ ] **Step 3: Editar o config do Claude Desktop**

Localizar e editar o arquivo:
- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
- Windows: `%APPDATA%\Claude\claude_desktop_config.json`

Conteúdo (substituir o path pelo valor obtido no Step 2):

```json
{
  "mcpServers": {
    "mcp-demo-api": {
      "command": "node",
      "args": ["/home/gerson/workspace/mcp-demo-api/mcp-server/dist/index.js"]
    }
  }
}
```

Se o arquivo já tiver outros `mcpServers`, adicionar apenas a chave `"mcp-demo-api"` dentro do objeto existente.

- [ ] **Step 4: Reiniciar o Claude Desktop**

Fechar completamente e reabrir. No ícone de ferramentas (hammer), confirmar que `mcp-demo-api` aparece com as 5 tools:
- `list_products`
- `create_product`
- `create_products_batch`
- `list_orders`
- `create_order`

- [ ] **Step 5: Teste de smoke no Claude Desktop**

Primeiro, garantir que a API está rodando:
```bash
docker-compose up -d
./mvnw quarkus:dev
```

Testar no Claude Desktop:

**Teste 1 — Consulta (report):**
> "Liste os produtos cadastrados"

Expected: Claude invoca `list_products` e apresenta os produtos formatados.

**Teste 2 — Automação em lote:**
> "Cadastre 3 produtos: Notebook por R$3000 com 10 em estoque, Mouse por R$150 com 50 em estoque, Teclado por R$300 com 30 em estoque"

Expected: Claude invoca `create_products_batch` com array de 3 itens e confirma a criação dos 3 produtos.

- [ ] **Step 6: Atualizar README.md**

Adicionar ao final do `README.md` a seção abaixo:

```markdown
## MCP Server (Claude Desktop)

O MCP server expõe as operações da API para o Claude Desktop via linguagem natural.

### Pré-requisitos

- Node.js 18+

### Build

```bash
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

Reiniciar o Claude Desktop após salvar.

### Tools disponíveis

| Tool | Descrição |
|------|-----------|
| `list_products` | Lista produtos ativos |
| `create_product` | Cria um produto |
| `create_products_batch` | Cria múltiplos produtos de uma vez |
| `list_orders` | Lista pedidos ativos |
| `create_order` | Cria um pedido com itens |

### Rodando a API antes da demo

```bash
docker-compose up -d        # sobe o PostgreSQL
./mvnw quarkus:dev          # sobe a API em localhost:8080
```
```

- [ ] **Step 7: Commit final**

```bash
git add README.md
git commit -m "docs: update README with MCP server setup and Claude Desktop config"
```
