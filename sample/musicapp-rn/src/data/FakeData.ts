export type Playlist = {
  id: string;
  name: string;
  description: string;
  trackCount: number;
};

export type Track = {
  id: string;
  title: string;
  artist: string;
  duration: string;
  album: string;
};

export const playlists: Playlist[] = [
  { id: "1", name: "Jazz Essentials", description: "Best jazz tracks", trackCount: 12 },
  { id: "2", name: "Rock Classics", description: "Classic rock hits", trackCount: 8 },
  { id: "3", name: "Lo-fi Beats", description: "Chill study music", trackCount: 15 },
];

export const tracks: Track[] = [
  { id: "1", title: "Blue Train", artist: "John Coltrane", duration: "10:42", album: "Blue Train" },
  { id: "2", title: "Kind of Blue", artist: "Miles Davis", duration: "9:22", album: "Kind of Blue" },
  { id: "3", title: "Take Five", artist: "Dave Brubeck", duration: "5:24", album: "Time Out" },
  { id: "4", title: "Bohemian Rhapsody", artist: "Queen", duration: "5:55", album: "A Night at the Opera" },
  { id: "5", title: "Stairway to Heaven", artist: "Led Zeppelin", duration: "8:02", album: "Led Zeppelin IV" },
];
