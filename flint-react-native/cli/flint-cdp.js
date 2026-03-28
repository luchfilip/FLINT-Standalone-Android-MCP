#!/usr/bin/env node

const http = require('http');
const WebSocket = require('ws');

const args = process.argv.slice(2);

// Parse options
let port = 8081;
let delay = 500;
const flags = {};
const positional = [];

for (let i = 0; i < args.length; i++) {
  if (args[i] === '--port') { port = parseInt(args[++i], 10); }
  else if (args[i] === '--delay') { delay = parseInt(args[++i], 10); }
  else if (args[i] === '--params') { flags.params = args[++i]; }
  else { positional.push(args[i]); }
}

const command = positional[0];

if (!command) {
  console.error(`Usage: flint-cdp <command> [options]

Commands:
  read                          Read screen via readScreen()
  screen                        Get screen via getScreen()
  schema                        Get schema via getSchema()
  call <name> [--params '{...}']  Call a tool, then auto-read screen
  action <name> [listId] [itemIndex]  Invoke an action

Options:
  --port <port>    Metro port (default: 8081)
  --delay <ms>     Delay after callTool before reading (default: 500)`);
  process.exit(1);
}

function buildExpression() {
  switch (command) {
    case 'read':
      return 'globalThis.__flint__.readScreen()';
    case 'screen':
      return 'globalThis.__flint__.getScreen()';
    case 'schema':
      return 'globalThis.__flint__.getSchema()';
    case 'call': {
      const name = positional[1];
      if (!name) { console.error('Error: call requires a tool name'); process.exit(1); }
      const params = flags.params || '{}';
      return `globalThis.__flint__.callTool(${JSON.stringify(name)}, ${params})`;
    }
    case 'action': {
      const name = positional[1];
      if (!name) { console.error('Error: action requires an action name'); process.exit(1); }
      const listId = positional[2] ? JSON.stringify(positional[2]) : 'undefined';
      const itemIndex = positional[3] !== undefined ? positional[3] : 'undefined';
      return `globalThis.__flint__.invokeAction(${JSON.stringify(name)}, ${listId}, ${itemIndex})`;
    }
    default:
      console.error(`Unknown command: ${command}`);
      process.exit(1);
  }
}

function discover() {
  return new Promise((resolve, reject) => {
    http.get(`http://localhost:${port}/json`, (res) => {
      let data = '';
      res.on('data', (chunk) => data += chunk);
      res.on('end', () => {
        try {
          const targets = JSON.parse(data);
          if (!targets.length) return reject(new Error('No CDP targets found'));
          resolve(targets[0].webSocketDebuggerUrl);
        } catch (e) { reject(e); }
      });
    }).on('error', reject);
  });
}

function evaluate(ws, expression) {
  return new Promise((resolve, reject) => {
    const id = 1;
    ws.on('message', function handler(raw) {
      const msg = JSON.parse(raw);
      if (msg.id === id) {
        ws.off('message', handler);
        if (msg.error) return reject(new Error(msg.error.message));
        if (msg.result && msg.result.exceptionDetails) {
          return reject(new Error(msg.result.exceptionDetails.text || 'Evaluation error'));
        }
        resolve(msg.result.result.value);
      }
    });
    ws.send(JSON.stringify({
      id,
      method: 'Runtime.evaluate',
      params: { expression, returnByValue: true }
    }));
  });
}

async function main() {
  const wsUrl = await discover();
  const ws = new WebSocket(wsUrl);

  await new Promise((resolve, reject) => {
    ws.on('open', resolve);
    ws.on('error', reject);
  });

  let msgId = 0;

  function eval_(expression) {
    return new Promise((resolve, reject) => {
      const id = ++msgId;
      function handler(raw) {
        const msg = JSON.parse(raw);
        if (msg.id === id) {
          ws.off('message', handler);
          if (msg.error) return reject(new Error(msg.error.message));
          if (msg.result && msg.result.exceptionDetails) {
            return reject(new Error(msg.result.exceptionDetails.text || 'Evaluation error'));
          }
          resolve(msg.result.result.value);
        }
      }
      ws.on('message', handler);
      ws.send(JSON.stringify({
        id,
        method: 'Runtime.evaluate',
        params: { expression, returnByValue: true }
      }));
    });
  }

  const expr = buildExpression();
  const result = await eval_(expr);
  console.log(typeof result === 'string' ? result : JSON.stringify(result, null, 2));

  // For call command, wait then auto-read screen
  if (command === 'call') {
    await new Promise(r => setTimeout(r, delay));
    const screen = await eval_('globalThis.__flint__.readScreen()');
    console.log('\n--- Screen after call ---');
    console.log(typeof screen === 'string' ? screen : JSON.stringify(screen, null, 2));
  }

  ws.close();
}

main().catch((err) => {
  console.error('Error:', err.message);
  process.exit(1);
});
