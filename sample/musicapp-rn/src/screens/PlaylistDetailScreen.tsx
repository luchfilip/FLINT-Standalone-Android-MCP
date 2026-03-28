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
import { playlists, tracks } from "../data/FakeData";
import { RootStackParamList } from "../navigation/types";

type Props = NativeStackScreenProps<RootStackParamList, "PlaylistDetail">;

export function PlaylistDetailScreen({ route, navigation }: Props) {
  const { playlistId } = route.params;
  const playlist = playlists.find((p) => p.id === playlistId);
  useFlintScreen("playlist_detail", "PlaylistDetail");

  useFlintTools([
    {
      name: "go_back",
      description: "Go back to the previous screen",
      action: () => navigation.goBack(),
    },
  ]);

  useFlintList("tracks", "Playlist tracks");

  if (!playlist) {
    return (
      <View style={styles.container}>
        <FlintText flintKey="error" style={styles.error}>
          Playlist not found
        </FlintText>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <FlintText flintKey="name" style={styles.name}>
          {playlist.name}
        </FlintText>
        <FlintText flintKey="description" style={styles.description}>
          {playlist.description}
        </FlintText>
        <FlintText flintKey="trackCount" style={styles.meta}>
          {`${playlist.trackCount} tracks`}
        </FlintText>
      </View>
      <FlatList
        data={tracks}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.list}
        renderItem={({ item, index }) => (
          <FlintItem list="tracks" index={index}>
            <View style={styles.row}>
              <View style={styles.rowContent}>
                <FlintText flintKey="title" style={styles.title}>
                  {item.title}
                </FlintText>
                <FlintText flintKey="artist" style={styles.artist}>
                  {item.artist}
                </FlintText>
              </View>
              <FlintAction
                flintName="play"
                flintDescription="Play this track"
                onPress={() =>
                  navigation.navigate("TrackDetail", { trackId: item.id })
                }
                style={styles.button}
              >
                <FlintText flintKey="buttonLabel" style={styles.buttonText}>
                  Play
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
  header: { paddingHorizontal: 20, paddingTop: 16, paddingBottom: 12 },
  name: { fontSize: 26, fontWeight: "700", color: "#fff" },
  description: { fontSize: 15, color: "#aaa", marginTop: 4 },
  meta: { fontSize: 13, color: "#666", marginTop: 4 },
  error: { color: "#f44", fontSize: 16, textAlign: "center", marginTop: 40 },
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
  button: {
    backgroundColor: "#1db954",
    borderRadius: 20,
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  buttonText: { color: "#fff", fontWeight: "600", fontSize: 14 },
});
