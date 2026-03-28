import React from "react";
import { StyleSheet, View } from "react-native";
import { NativeStackScreenProps } from "@react-navigation/native-stack";
import {
  useFlintScreen,
  useFlintTools,
  FlintText,
} from "flint-react-native";
import { tracks } from "../data/FakeData";
import { RootStackParamList } from "../navigation/types";

type Props = NativeStackScreenProps<RootStackParamList, "TrackDetail">;

export function TrackDetailScreen({ route, navigation }: Props) {
  const { trackId } = route.params;
  const track = tracks.find((t) => t.id === trackId);
  useFlintScreen("track_detail", "TrackDetail");

  useFlintTools([
    {
      name: "go_back",
      description: "Go back to the previous screen",
      action: () => navigation.goBack(),
    },
  ]);

  if (!track) {
    return (
      <View style={styles.container}>
        <FlintText flintKey="error" style={styles.error}>
          Track not found
        </FlintText>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.artwork} />
      <View style={styles.details}>
        <FlintText flintKey="title" style={styles.title}>
          {track.title}
        </FlintText>
        <FlintText flintKey="artist" style={styles.artist}>
          {track.artist}
        </FlintText>
        <FlintText flintKey="album" style={styles.album}>
          {track.album}
        </FlintText>
        <FlintText flintKey="duration" style={styles.duration}>
          {track.duration}
        </FlintText>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#121212", alignItems: "center" },
  artwork: {
    width: 240,
    height: 240,
    backgroundColor: "#2a2a2a",
    borderRadius: 16,
    marginTop: 40,
  },
  details: { alignItems: "center", marginTop: 32, paddingHorizontal: 20 },
  title: { fontSize: 26, fontWeight: "700", color: "#fff", textAlign: "center" },
  artist: { fontSize: 18, color: "#1db954", marginTop: 8 },
  album: { fontSize: 15, color: "#aaa", marginTop: 8 },
  duration: { fontSize: 14, color: "#666", marginTop: 8 },
  error: { color: "#f44", fontSize: 16, textAlign: "center", marginTop: 40 },
});
