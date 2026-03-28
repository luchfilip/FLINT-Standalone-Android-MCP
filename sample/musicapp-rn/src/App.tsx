import React from "react";
import { StatusBar } from "react-native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { FlintProvider, FlintNavigationContainer } from "flint-react-native";
import { RootStackParamList } from "./navigation/types";
import { HomeScreen } from "./screens/HomeScreen";
import { SearchResultsScreen } from "./screens/SearchResultsScreen";
import { PlaylistDetailScreen } from "./screens/PlaylistDetailScreen";
import { TrackDetailScreen } from "./screens/TrackDetailScreen";

const Stack = createNativeStackNavigator<RootStackParamList>();

export default function App() {
  return (
    <FlintProvider>
      <FlintNavigationContainer>
        <StatusBar barStyle="light-content" backgroundColor="#121212" />
        <Stack.Navigator
          screenOptions={{
            headerStyle: { backgroundColor: "#1a1a1a" },
            headerTintColor: "#fff",
            headerTitleStyle: { fontWeight: "600" },
            contentStyle: { backgroundColor: "#121212" },
          }}
        >
          <Stack.Screen
            name="Home"
            component={HomeScreen}
            options={{ title: "Flint Music" }}
          />
          <Stack.Screen
            name="SearchResults"
            component={SearchResultsScreen}
            options={{ title: "Search" }}
          />
          <Stack.Screen
            name="PlaylistDetail"
            component={PlaylistDetailScreen}
            options={{ title: "Playlist" }}
          />
          <Stack.Screen
            name="TrackDetail"
            component={TrackDetailScreen}
            options={{ title: "Now Playing" }}
          />
        </Stack.Navigator>
      </FlintNavigationContainer>
    </FlintProvider>
  );
}
