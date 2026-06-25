import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';
import { apiGet, apiPatch, apiPost, apiPut } from '../client.js';

const userShape = {
  username: z.string().describe('Nome do usuário'),
  address: z.string().describe('Endereço do usuário'),
};

export function registerUserTools(server: McpServer): void {
  server.tool(
    'list_users',
    'Lista todos os usuários ativos cadastrados na API',
    {},
    async () => {
      try {
        const users = await apiGet('/api/users');
        return { content: [{ type: 'text' as const, text: JSON.stringify(users, null, 2) }] };
      } catch (e) {
        return { content: [{ type: 'text' as const, text: `Erro ao listar usuários: ${(e as Error).message}` }] };
      }
    }
  );

  server.tool(
    'get_user',
    'Busca um usuário pelo ID',
    { id: z.number().int().positive().describe('ID do usuário') },
    async ({ id }) => {
      try {
        const user = await apiGet(`/api/users/${id}`);
        return { content: [{ type: 'text' as const, text: JSON.stringify(user, null, 2) }] };
      } catch (e) {
        return { content: [{ type: 'text' as const, text: `Erro ao buscar usuário: ${(e as Error).message}` }] };
      }
    }
  );

  server.tool(
    'create_user',
    'Cria um novo usuário na API',
    userShape,
    async (args) => {
      try {
        const user = await apiPost('/api/users', args);
        return { content: [{ type: 'text' as const, text: `Usuário criado com sucesso:\n${JSON.stringify(user, null, 2)}` }] };
      } catch (e) {
        return { content: [{ type: 'text' as const, text: `Erro ao criar usuário: ${(e as Error).message}` }] };
      }
    }
  );

  server.tool(
    'create_users_batch',
    'Cria múltiplos usuários numa única transação. Use quando precisar criar vários usuários de uma vez (ex.: 50) ao invés de chamar create_user repetidamente. Se qualquer um falhar, todo o batch é revertido.',
    {
      users: z
        .array(z.object(userShape))
        .min(1)
        .describe('Lista de usuários a criar'),
    },
    async ({ users }) => {
      const startedAt = Date.now();
      try {
        const created = await apiPost<unknown[]>('/api/users/batch', users);
        const elapsedMs = Date.now() - startedAt;
        const summary = { total: users.length, criados: created.length, tempoMs: elapsedMs };
        return {
          content: [
            {
              type: 'text' as const,
              text: `Batch concluído:\n${JSON.stringify(summary, null, 2)}\n\nUsuários criados:\n${JSON.stringify(created, null, 2)}`,
            },
          ],
        };
      } catch (e) {
        return { content: [{ type: 'text' as const, text: `Erro ao criar batch de usuários: ${(e as Error).message}` }] };
      }
    }
  );

  server.tool(
    'update_user',
    'Atualiza os dados de um usuário existente',
    { id: z.number().int().positive().describe('ID do usuário'), ...userShape },
    async ({ id, ...body }) => {
      try {
        const user = await apiPut(`/api/users/${id}`, body);
        return { content: [{ type: 'text' as const, text: `Usuário atualizado com sucesso:\n${JSON.stringify(user, null, 2)}` }] };
      } catch (e) {
        return { content: [{ type: 'text' as const, text: `Erro ao atualizar usuário: ${(e as Error).message}` }] };
      }
    }
  );

  server.tool(
    'activate_user',
    'Ativa um usuário desativado pelo ID',
    { id: z.number().int().positive().describe('ID do usuário') },
    async ({ id }) => {
      try {
        const user = await apiPatch(`/api/users/${id}/activate`);
        return { content: [{ type: 'text' as const, text: `Usuário ativado com sucesso:\n${JSON.stringify(user, null, 2)}` }] };
      } catch (e) {
        return { content: [{ type: 'text' as const, text: `Erro ao ativar usuário: ${(e as Error).message}` }] };
      }
    }
  );

  server.tool(
    'deactivate_user',
    'Desativa um usuário ativo pelo ID',
    { id: z.number().int().positive().describe('ID do usuário') },
    async ({ id }) => {
      try {
        const user = await apiPatch(`/api/users/${id}/deactivate`);
        return { content: [{ type: 'text' as const, text: `Usuário desativado com sucesso:\n${JSON.stringify(user, null, 2)}` }] };
      } catch (e) {
        return { content: [{ type: 'text' as const, text: `Erro ao desativar usuário: ${(e as Error).message}` }] };
      }
    }
  );
}
