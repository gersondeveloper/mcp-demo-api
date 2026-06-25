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
