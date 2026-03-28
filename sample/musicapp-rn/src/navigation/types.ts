export type RootStackParamList = {
  Home: undefined;
  SearchResults: { query: string };
  PlaylistDetail: { playlistId: string };
  TrackDetail: { trackId: string };
};
