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
import { tracks } from "../data/FakeData";
import { RootStackParamList } from "../navigation/types";

type Props = NativeStackScreenProps<RootStackParamList, "SearchResults">;

export function SearchResultsScreen({ route, navigation }: Props) {
  const { query } = route.params;
  useFlintScreen("search_results", "SearchResults");

  useFlintTools([
    {
      name: "go_back",
      description: "Go back to the previous screen",
      action: () => navigation.goBack(),
    },
  ]);

  useFlintList("results", "Search results");

  const lowerQuery = query.toLowerCase();
  const filtered = tracks.filter(
    (t) =>
      t.title.toLowerCase().includes(lowerQuery) ||
      t.artist.toLowerCase().includes(lowerQuery),
  );

  return (
    <View style={styles.container}>
      <FlintText flintKey="heading" style={styles.heading}>
        {`Results for "${query}"`}
      </FlintText>
      {filtered.length === 0 ? (
        <FlintText flintKey="empty" style={styles.empty}>
          No results found
        </FlintText>
      ) : (
        <FlatList
          data={filtered}
          keyExtractor={(item) => item.id}
          contentContainerStyle={styles.list}
          renderItem={({ item, index }) => (
            <FlintItem list="results" index={index}>
              <View style={styles.row}>
                <View style={styles.rowContent}>
                  <FlintText flintKey="title" style={styles.title}>
                    {item.title}
                  </FlintText>
                  <FlintText flintKey="artist" style={styles.artist}>
                    {item.artist}
                  </FlintText>
                  <FlintText flintKey="duration" style={styles.duration}>
                    {item.duration}
                  </FlintText>
                </View>
                <FlintAction
                  flintName="select"
                  flintDescription="View track details"
                  onPress={() =>
                    navigation.navigate("TrackDetail", { trackId: item.id })
                  }
                  style={styles.button}
                >
                  <FlintText flintKey="buttonLabel" style={styles.buttonText}>
                    View
                  </FlintText>
                </FlintAction>
              </View>
            </FlintItem>
          )}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#121212" },
  heading: {
    fontSize: 22,
    fontWeight: "700",
    color: "#fff",
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 8,
  },
  empty: { color: "#888", fontSize: 16, textAlign: "center", marginTop: 40 },
  list: { paddingHorizontal: 16, paddingBottom: 24 },
  row: {
    backgroundColor: "#1e1e1e",
    borderRadius: 10,
    padding: 14,
    marginBottom: 10,
    flexDirection: "row",
    alignItems: "center",
  },
  rowContent: { flex: 1 },
  title: { fontSize: 16, fontWeight: "600", color: "#fff" },
  artist: { fontSize: 14, color: "#aaa", marginTop: 2 },
  duration: { fontSize: 12, color: "#666", marginTop: 2 },
  button: {
    backgroundColor: "#1db954",
    borderRadius: 20,
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  buttonText: { color: "#fff", fontWeight: "600", fontSize: 14 },
});
