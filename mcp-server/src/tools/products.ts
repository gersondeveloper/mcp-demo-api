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
