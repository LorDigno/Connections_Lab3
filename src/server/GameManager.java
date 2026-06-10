package server;

import server.puzzles.RealPuzzle;
import server.puzzles.UserPuzzle;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

//gestisce effettivamente il puzzle
class GameManager {
    private volatile RealPuzzle current;
    private ConcurrentHashMap<String, UserPuzzle> participants;
    private ScheduledExecutorService timer;

    // Al timeout → chiama endGame()
    // endGame() → calcola classifica → passa a UDPNotifier → carica prossima
}