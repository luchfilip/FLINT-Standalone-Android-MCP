import { FlintRegistry } from "../FlintRegistry";
import { generateSchema } from "../FlintSchema";

describe("generateSchema", () => {
  let registry: FlintRegistry;

  beforeEach(() => {
    registry = new FlintRegistry();
  });

  // 1. generates correct protocol and version
  it("generates correct protocol and version", () => {
    const schema = generateSchema(registry);
    expect(schema.protocol).toBe("flint");
    expect(schema.version).toBe("2.0");
    expect(schema.tools).toEqual([]);
  });

  // 2. generates tool with no params
  it("generates tool with no params", () => {
    registry.registerTools([
      {
        name: "refresh",
        description: "Refresh the screen",
        action: jest.fn(),
      },
    ]);

    const schema = generateSchema(registry);
    expect(schema.tools).toEqual([
      {
        name: "refresh",
        description: "Refresh the screen",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },
    ]);
  });

  // 3. generates tool with required params
  it("generates tool with required params", () => {
    registry.registerTools([
      {
        name: "search",
        description: "Search items",
        params: [
          {
            name: "query",
            type: "string",
            description: "Search query",
            required: true,
          },
          {
            name: "limit",
            type: "integer",
            description: "Max results",
          },
        ],
        action: jest.fn(),
      },
    ]);

    const schema = generateSchema(registry);
    expect(schema.tools).toHaveLength(1);
    const tool = schema.tools[0];
    expect(tool.name).toBe("search");
    expect(tool.inputSchema.properties).toEqual({
      query: { type: "string", description: "Search query" },
      limit: { type: "integer", description: "Max results" },
    });
    expect(tool.inputSchema.required).toEqual(["query", "limit"]);
  });

  // 4. generates tool with optional params (required: false)
  it("generates tool with optional params (required: false)", () => {
    registry.registerTools([
      {
        name: "filter",
        description: "Filter results",
        params: [
          {
            name: "category",
            type: "string",
            description: "Category name",
            required: true,
          },
          {
            name: "sortOrder",
            type: "string",
            description: "Sort direction",
            required: false,
          },
        ],
        action: jest.fn(),
      },
    ]);

    const schema = generateSchema(registry);
    const tool = schema.tools[0];
    expect(tool.inputSchema.required).toEqual(["category"]);
    expect(tool.inputSchema.properties).toEqual({
      category: { type: "string", description: "Category name" },
      sortOrder: { type: "string", description: "Sort direction" },
    });
  });

  // 5. generates multiple tools
  it("generates multiple tools", () => {
    registry.registerTools([
      {
        name: "open",
        description: "Open item",
        params: [
          { name: "id", type: "string", description: "Item ID" },
        ],
        action: jest.fn(),
      },
      {
        name: "close",
        description: "Close item",
        action: jest.fn(),
      },
    ]);

    const schema = generateSchema(registry);
    expect(schema.tools).toHaveLength(2);
    expect(schema.tools[0].name).toBe("open");
    expect(schema.tools[0].inputSchema.required).toEqual(["id"]);
    expect(schema.tools[1].name).toBe("close");
    expect(schema.tools[1].inputSchema.properties).toEqual({});
    expect(schema.tools[1].inputSchema.required).toEqual([]);
  });
});
