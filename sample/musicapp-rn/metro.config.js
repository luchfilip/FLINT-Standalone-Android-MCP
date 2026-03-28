const path = require('path');
const { getDefaultConfig, mergeConfig } = require('@react-native/metro-config');

const flintPackagePath = path.resolve(__dirname, '../../flint-react-native');

/**
 * Metro configuration
 * https://reactnative.dev/docs/metro
 *
 * @type {import('@react-native/metro-config').MetroConfig}
 */
const appNodeModules = path.resolve(__dirname, 'node_modules');

const config = {
  watchFolders: [flintPackagePath],
  resolver: {
    nodeModulesPaths: [appNodeModules],
    extraNodeModules: {
      react: path.resolve(appNodeModules, 'react'),
      'react-native': path.resolve(appNodeModules, 'react-native'),
      '@react-navigation/native': path.resolve(appNodeModules, '@react-navigation/native'),
    },
    blockList: [
      new RegExp(path.resolve(flintPackagePath, 'node_modules', 'react') + '/.*'),
      new RegExp(path.resolve(flintPackagePath, 'node_modules', 'react-native') + '/.*'),
    ],
  },
};

module.exports = mergeConfig(getDefaultConfig(__dirname), config);
