const path = require("path");

module.exports = {
  preset: "ts-jest",
  testEnvironment: "jsdom",
  roots: ["<rootDir>/src"],
  transform: {
    "^.+\\.tsx?$": "ts-jest",
  },
  setupFilesAfterEnv: ["@testing-library/jest-dom"],
  moduleNameMapper: {
    "^react(/.*)?$": path.resolve(__dirname, "node_modules/react$1"),
    "^react-dom(/.*)?$": path.resolve(__dirname, "node_modules/react-dom$1"),
  },
};
