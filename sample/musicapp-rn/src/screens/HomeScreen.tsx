import React from "react";
import { FlatList, StyleSheet, View } from "react-native";
import { NativeStackScreenProps } from "@react-navigation/native-stack";
import {
  useFlintScreen,
  useFlintTools,
  useFlintList,
  FlintItem,
  FlintText,
  FlintAction,
} from "flint-react-native";
import { playlists } from "../data/FakeData";
import { RootStackParamList } from "../navigation/types";

type Props = NativeStackScreenProps<RootStackParamList, "Home">;

export function HomeScreen({ navigation }: Props) {
  useFlintScreen("home", "Home");

  useFlintTools([
    {
      name: "search",
      description: "Search tracks by title or artist",
      params: [
        { name: "query", type: "string", description: "Search query", required: true },
      ],
      action: ({ query }) => {
        navigation.navigate("SearchResults", { query });
      },
    },
  ]);

  useFlintList("playlists", "Featured playlists");

  return (
    <View style={styles.container}>
      <FlintText flintKey="heading" style={styles.heading}>
        Music Library
      </FlintText>
      <FlatList
        data={playlists}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.list}
        renderItem={({ item, index }) => (
          <FlintItem list="playlists" index={index}>
            <View style={styles.card}>
              <View style={styles.cardContent}>
                <FlintText flintKey="name" style={styles.name}>
                  {item.name}
                </FlintText>
                <FlintText flintKey="description" style={styles.description}>
                  {item.description}
                </FlintText>
                <FlintText flintKey="trackCount" style={styles.meta}>
                  {`${item.trackCount} tracks`}
                </FlintText>
              </View>
              <FlintAction
                flintName="open"
                flintDescription="Open this playlist"
                onPress={() =>
                  navigation.navigate("PlaylistDetail", { playlistId: item.id })
                }
                style={styles.button}
              >
                <FlintText flintKey="buttonLabel" style={styles.buttonText}>
                  Open
                </FlintText>
              </FlintAction>
            </View>
          </FlintItem>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#121212" },
  heading: {
    fontSize: 28,
    fontWeight: "700",
    color: "#fff",
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 8,
  },
  list: { paddingHorizontal: 16, paddingBottom: 24 },
  card: {
    backgroundColor: "#1e1e1e",
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    flexDirection: "row",
    alignItems: "center",
  },
  cardContent: { flex: 1 },
  name: { fontSize: 18, fontWeight: "600", color: "#fff" },
  description: { fontSize: 14, color: "#aaa", marginTop: 4 },
  meta: { fontSize: 12, color: "#666", marginTop: 4 },
  button: {
    backgroundColor: "#1db954",
    borderRadius: 20,
    paddingHorizontal: 18,
    paddingVertical: 8,
  },
  buttonText: { color: "#fff", fontWeight: "600", fontSize: 14 },
});
