import SwiftUI
import AVKit

private enum BluTab: String, CaseIterable, Identifiable {
    case home = "Home"
    case movies = "Movies"
    case shows = "Shows"
    case genres = "Genres"
    case search = "Search"
    case myStuff = "My Stuff"
    case addons = "Add-ons"
    case settings = "Settings"

    var id: String { rawValue }

    var icon: String {
        switch self {
        case .home: return "house.fill"
        case .movies: return "film.fill"
        case .shows: return "tv.fill"
        case .genres: return "square.grid.2x2.fill"
        case .search: return "magnifyingglass"
        case .myStuff: return "bookmark.fill"
        case .addons: return "puzzlepiece.extension.fill"
        case .settings: return "gearshape.fill"
        }
    }
}

struct ContentView: View {
    @State private var selectedTab: BluTab = .home
    @State private var streamURL = ""
    @State private var streamTitle = "BluStream"
    @State private var player: AVPlayer?
    @State private var showingPlayer = false
    @State private var searchText = ""
    @AppStorage("blustream.profileName") private var profileName = "Macca"

    var body: some View {
        NavigationStack {
            ZStack {
                LinearGradient(
                    colors: [Color.black, Color(red: 0.01, green: 0.08, blue: 0.14)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea()

                VStack(spacing: 0) {
                    header
                    Divider().overlay(Color.white.opacity(0.08))
                    content
                    Divider().overlay(Color.white.opacity(0.08))
                    bottomBar
                }
            }
            .sheet(isPresented: $showingPlayer) {
                if let player {
                    NavigationStack {
                        VideoPlayer(player: player)
                            .ignoresSafeArea()
                            .background(Color.black)
                            .navigationTitle(streamTitle)
                            .navigationBarTitleDisplayMode(.inline)
                            .onAppear { player.play() }
                            .onDisappear { player.pause() }
                    }
                }
            }
        }
        .preferredColorScheme(.dark)
    }

    private var header: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 0) {
                    Text("BLU")
                        .foregroundStyle(Color.blue)
                        .fontWeight(.heavy)
                    Text("STREAM")
                        .foregroundStyle(.white)
                        .fontWeight(.light)
                }
                .font(.title2)

                Text("iOS ALPHA 0.6")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 2) {
                Text(profileName)
                    .font(.subheadline.weight(.semibold))
                Text("iPhone")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            ZStack {
                Circle().fill(Color.blue.opacity(0.25))
                Image(systemName: "person.fill")
                    .foregroundStyle(Color.blue)
            }
            .frame(width: 38, height: 38)
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 12)
    }

    @ViewBuilder
    private var content: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                switch selectedTab {
                case .home:
                    homeView
                case .movies:
                    sectionPlaceholder(title: "Movies", message: "Your movie catalogue will appear here.", icon: "film.stack.fill")
                case .shows:
                    sectionPlaceholder(title: "TV Shows", message: "Your TV catalogue will appear here.", icon: "tv.fill")
                case .genres:
                    genresView
                case .search:
                    searchView
                case .myStuff:
                    sectionPlaceholder(title: "My Stuff", message: "Saved titles and recent items will appear here.", icon: "bookmark.fill")
                case .addons:
                    addonsView
                case .settings:
                    settingsView
                }
            }
            .padding(18)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var homeView: some View {
        VStack(alignment: .leading, spacing: 18) {
            VStack(alignment: .leading, spacing: 8) {
                Text("Welcome back, \(profileName)")
                    .font(.title.bold())
                Text("BluStream for iPhone is now ready for direct HLS and video testing.")
                    .foregroundStyle(.secondary)
            }

            directPlayerCard

            VStack(alignment: .leading, spacing: 12) {
                Text("Browse")
                    .font(.title3.bold())

                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    browseButton("Movies", icon: "film.fill", tab: .movies)
                    browseButton("TV Shows", icon: "tv.fill", tab: .shows)
                    browseButton("Genres", icon: "square.grid.2x2.fill", tab: .genres)
                    browseButton("My Stuff", icon: "bookmark.fill", tab: .myStuff)
                }
            }
        }
    }

    private var directPlayerCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Direct Stream Test", systemImage: "play.rectangle.fill")
                .font(.headline)

            TextField("Name", text: $streamTitle)
                .textFieldStyle(.roundedBorder)

            TextField("Direct video or HLS URL", text: $streamURL)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .keyboardType(.URL)
                .textFieldStyle(.roundedBorder)

            Button {
                playDirectStream()
            } label: {
                Label("Play on iPhone", systemImage: "play.fill")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .disabled(streamURL.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        }
        .padding(16)
        .background(Color.white.opacity(0.06), in: RoundedRectangle(cornerRadius: 18))
    }

    private var genresView: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Genres")
                .font(.title.bold())

            let genres = ["Action", "Adventure", "Animation", "Comedy", "Crime", "Documentary", "Drama", "Family", "Fantasy", "History", "Horror", "Music", "Mystery", "Romance", "Science Fiction", "Thriller", "War", "Western"]

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                ForEach(genres, id: \.self) { genre in
                    Button {
                        selectedTab = .search
                        searchText = genre
                    } label: {
                        HStack {
                            Image(systemName: "play.square.stack.fill")
                            Text(genre)
                                .lineLimit(1)
                            Spacer()
                        }
                        .padding(12)
                        .frame(maxWidth: .infinity)
                        .background(Color.white.opacity(0.06), in: RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var searchView: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Search")
                .font(.title.bold())

            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(.secondary)
                TextField("Search BluStream", text: $searchText)
                    .textInputAutocapitalization(.never)
            }
            .padding(12)
            .background(Color.white.opacity(0.06), in: RoundedRectangle(cornerRadius: 14))

            if searchText.isEmpty {
                Text("Enter a title or genre.")
                    .foregroundStyle(.secondary)
            } else {
                Text("Search ready for: \(searchText)")
                    .foregroundStyle(.secondary)
            }
        }
    }

    private var addonsView: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Add-ons")
                .font(.title.bold())

            Text("Use direct URLs you have permission to access. HLS streams ending in .m3u8 and normal video URLs work through Apple AVPlayer.")
                .foregroundStyle(.secondary)

            directPlayerCard
        }
    }

    private var settingsView: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Settings")
                .font(.title.bold())

            VStack(alignment: .leading, spacing: 8) {
                Text("Profile name")
                    .font(.headline)
                TextField("Profile name", text: $profileName)
                    .textFieldStyle(.roundedBorder)
            }

            VStack(alignment: .leading, spacing: 6) {
                Text("BluStream iOS")
                    .font(.headline)
                Text("Alpha 0.6")
                    .foregroundStyle(.secondary)
                Text("Built with SwiftUI and AVKit for iPhone and iPad.")
                    .foregroundStyle(.secondary)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white.opacity(0.06), in: RoundedRectangle(cornerRadius: 18))
        }
    }

    private func sectionPlaceholder(title: String, message: String, icon: String) -> some View {
        VStack(alignment: .leading, spacing: 18) {
            Text(title)
                .font(.title.bold())

            VStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.system(size: 52))
                    .foregroundStyle(Color.blue)
                Text(message)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 42)
            .background(Color.white.opacity(0.06), in: RoundedRectangle(cornerRadius: 18))
        }
    }

    private func browseButton(_ title: String, icon: String, tab: BluTab) -> some View {
        Button {
            selectedTab = tab
        } label: {
            VStack(spacing: 10) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundStyle(Color.blue)
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.white)
            }
            .frame(maxWidth: .infinity, minHeight: 96)
            .background(Color.white.opacity(0.06), in: RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
    }

    private var bottomBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(BluTab.allCases) { tab in
                    Button {
                        selectedTab = tab
                    } label: {
                        VStack(spacing: 4) {
                            Image(systemName: tab.icon)
                                .font(.system(size: 16, weight: .semibold))
                            Text(tab.rawValue)
                                .font(.caption2)
                        }
                        .foregroundStyle(selectedTab == tab ? Color.blue : Color.secondary)
                        .frame(minWidth: 62)
                        .padding(.vertical, 8)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 8)
        }
        .background(Color.black.opacity(0.92))
    }

    private func playDirectStream() {
        let trimmed = streamURL.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let url = URL(string: trimmed), !trimmed.isEmpty else { return }
        player = AVPlayer(url: url)
        showingPlayer = true
    }
}
