module.exports = {
  transform: {
    '^.+\\.tsx?$': 'ts-jest',
  },
  testMatch: ['**/src/**/__tests__/**/*.test.ts', '**/src/**/__tests__/**/*.test.tsx'],
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json'],
  testEnvironment: 'jsdom',
  moduleNameMapper: {
    '^react-native$': '<rootDir>/src/__mocks__/react-native.tsx',
    '^@react-navigation/native$': '<rootDir>/src/__mocks__/@react-navigation/native.tsx',
    '^expo-router$': '<rootDir>/src/__mocks__/expo-router.ts',
  },
};
