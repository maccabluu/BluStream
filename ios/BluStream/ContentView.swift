import SwiftUI
import AVKit

struct ContentView: View {
    @State private var streamURL = ""
    @State private var player: AVPlayer?
    @State private var showingPlayer = false

    var body: some View {
        NavigationStack {
            ZStack {
                LinearGradient(colors: [.black, Color.blue.opacity(0.35)], startPoint: .top, endPoint: .bottom)
                    .ignoresSafeArea()

                VStack(spacing: 24) {
                    Spacer()
                    Image(systemName: "play.tv.fill")
                        .font(.system(size: 72))
                        .foregroundStyle(.blue)
                    Text("BluStream")
                        .font(.largeTitle.bold())
                        .foregroundStyle(.white)
                    Text("iOS Alpha")
                        .foregroundStyle(.secondary)

                    TextField("Direct video or HLS URL", text: $streamURL)
                        .textInputAutocapitalization(.never)
                        .keyboardType(.URL)
                        .padding()
                        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 14))

                    Button("Play Stream") {
                        guard let url = URL(string: streamURL), !streamURL.isEmpty else { return }
                        player = AVPlayer(url: url)
                        showingPlayer = true
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)

                    Spacer()
                }
                .padding(24)
            }
            .sheet(isPresented: $showingPlayer) {
                if let player {
                    VideoPlayer(player: player)
                        .ignoresSafeArea()
                        .onAppear { player.play() }
                        .onDisappear { player.pause() }
                }
            }
        }
        .preferredColorScheme(.dark)
    }
}
