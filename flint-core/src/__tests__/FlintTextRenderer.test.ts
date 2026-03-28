import { renderSnapshot } from "../FlintTextRenderer";
import { FlintScreenSnapshot } from "../types";

describe("FlintTextRenderer", () => {
  // 1. renders screen name
  it("renders screen name", () => {
    const snapshot: FlintScreenSnapshot = {
      screen: "home",
      content: { elements: [] },
      overlays: [],
    };
    expect(renderSnapshot(snapshot, [])).toBe("screen: home");
  });

  // 2. renders content elements
  it("renders content elements", () => {
    const snapshot: FlintScreenSnapshot = {
      screen: "detail",
      content: {
        elements: [
          { type: "content", key: "title", value: "Hello World" },
          { type: "content", key: "subtitle", value: "A greeting" },
        ],
      },
      overlays: [],
    };
    const result = renderSnapshot(snapshot, []);
    expect(result).toBe(
      "screen: detail\ntitle: Hello World\nsubtitle: A greeting"
    );
  });

  // 3. renders list with items
  it("renders list with items", () => {
    const snapshot: FlintScreenSnapshot = {
      screen: "browse",
      content: {
        elements: [
          {
            type: "list",
            id: "tracks",
            description: "Track list",
            items: [
              { index: 0, content: { name: "Song A" }, actions: [] },
              { index: 1, content: { name: "Song B" }, actions: [] },
            ],
          },
        ],
      },
      overlays: [],
    };
    const result = renderSnapshot(snapshot, []);
    expect(result).toBe(
      "screen: browse\ntracks:\n  [0] name: Song A\n  [1] name: Song B"
    );
  });

  // 4. item content joined with pipe separator
  it("item content joined with pipe separator", () => {
    const snapshot: FlintScreenSnapshot = {
      screen: "browse",
      content: {
        elements: [
          {
            type: "list",
            id: "tracks",
            description: "Track list",
            items: [
              {
                index: 0,
                content: { name: "Song A", artist: "Artist 1" },
                actions: [],
              },
            ],
          },
        ],
      },
      overlays: [],
    };
    const result = renderSnapshot(snapshot, []);
    expect(result).toBe(
      "screen: browse\ntracks:\n  [0] name: Song A | artist: Artist 1"
    );
  });

  // 5. renders overlays with content and actions
  it("renders overlays with content and actions", () => {
    const snapshot: FlintScreenSnapshot = {
      screen: "home",
      content: { elements: [] },
      overlays: [
        {
          id: "dialog",
          description: "Confirm dialog",
          content: { message: "Are you sure?" },
          actions: [
            { name: "confirm", description: "Confirm action" },
            { name: "cancel", description: "Cancel action" },
          ],
        },
      ],
    };
    const result = renderSnapshot(snapshot, []);
    expect(result).toBe(
      "screen: home\noverlay(dialog):\n  message: Are you sure?\n  actions: confirm, cancel"
    );
  });

  // 6. renders tool names
  it("renders tool names", () => {
    const snapshot: FlintScreenSnapshot = {
      screen: "home",
      content: { elements: [] },
      overlays: [],
    };
    const result = renderSnapshot(snapshot, ["navigate", "refresh", "search"]);
    expect(result).toBe("screen: home\ntools: navigate, refresh, search");
  });

  // 7. handles empty snapshot (just screen name)
  it("handles empty snapshot (just screen name)", () => {
    const snapshot: FlintScreenSnapshot = {
      screen: "empty",
      content: { elements: [] },
      overlays: [],
    };
    expect(renderSnapshot(snapshot, [])).toBe("screen: empty");
  });

  // 8. handles snapshot with no tools
  it("handles snapshot with no tools", () => {
    const snapshot: FlintScreenSnapshot = {
      screen: "settings",
      content: {
        elements: [{ type: "content", key: "theme", value: "dark" }],
      },
      overlays: [],
    };
    const result = renderSnapshot(snapshot, []);
    expect(result).toBe("screen: settings\ntheme: dark");
  });

  // 9. GOLDEN TEST: home screen output matches Compose SDK exactly
  it("GOLDEN: home screen matches Compose SDK output", () => {
    const snapshot: FlintScreenSnapshot = {
      screen: "home",
      content: {
        elements: [
          {
            type: "list",
            id: "playlists",
            description: "User playlists",
            items: [
              {
                index: 0,
                content: { name: "Chill Vibes", description: "Relaxing tunes" },
                actions: [
                  { name: "play", description: "Play playlist" },
                  { name: "delete", description: "Delete playlist" },
                ],
              },
              {
                index: 1,
                content: { name: "Workout", description: "High energy" },
                actions: [
                  { name: "play", description: "Play playlist" },
                  { name: "delete", description: "Delete playlist" },
                ],
              },
            ],
          },
        ],
      },
      overlays: [],
    };

    const expected = [
      "screen: home",
      "playlists:",
      "  [0] name: Chill Vibes | description: Relaxing tunes | actions: play, delete",
      "  [1] name: Workout | description: High energy | actions: play, delete",
      "tools: navigate, refresh",
    ].join("\n");

    expect(renderSnapshot(snapshot, ["navigate", "refresh"])).toBe(expected);
  });

  // 10. GOLDEN TEST: search results screen with content element + list
  it("GOLDEN: search results screen with content and list", () => {
    const snapshot: FlintScreenSnapshot = {
      screen: "search_results",
      content: {
        elements: [
          { type: "content", key: "query", value: "jazz" },
          {
            type: "list",
            id: "results",
            description: "Search results",
            items: [
              {
                index: 0,
                content: { title: "Jazz Classics", artist: "Various" },
                actions: [{ name: "play", description: "Play track" }],
              },
              {
                index: 1,
                content: { title: "Smooth Jazz", artist: "Kenny G" },
                actions: [{ name: "play", description: "Play track" }],
              },
            ],
          },
        ],
      },
      overlays: [],
    };

    const expected = [
      "screen: search_results",
      "query: jazz",
      "results:",
      "  [0] title: Jazz Classics | artist: Various | actions: play",
      "  [1] title: Smooth Jazz | artist: Kenny G | actions: play",
      "tools: search, play",
    ].join("\n");

    expect(renderSnapshot(snapshot, ["search", "play"])).toBe(expected);
  });
});
