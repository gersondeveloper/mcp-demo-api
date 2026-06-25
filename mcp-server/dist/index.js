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
